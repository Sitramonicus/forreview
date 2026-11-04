import test from 'node:test';
import assert from 'node:assert/strict';
import { calculatePayrollPreview2026, formatPesoCents, parsePesoCents } from '../../web/payroll-preview-2026.js';
import { PAYROLL_RULES_2026 } from '../../web/payroll-rules-2026.js';

const sample = {
  period: '2026-11',
  grossCashPay: '25000.00',
  sssCompensation: '25000.00',
  philHealthBasicSalary: '25000.00',
  pagIbigFundSalary: '25000.00',
  taxableCompensation: '25000.00'
};

test('parses and formats centavos exactly without floating-point arithmetic', () => {
  assert.equal(parsePesoCents('1,234.56'), 123456n);
  assert.equal(parsePesoCents('0'), 0n);
  assert.equal(formatPesoCents(123456789n), '₱1,234,567.89');
  assert.throws(() => parsePesoCents('-1'), /non-negative/);
  assert.throws(() => parsePesoCents('1.001'), /two decimal places/);
});

test('2026 synthetic ₱25,000 monthly example computes separate employee and employer shares', () => {
  const result = calculatePayrollPreview2026(sample);
  assert.equal(result.rulesetId, 'PH-2026-monthly-preview-v1');
  assert.equal(result.sss.mscCents, 2_500_000n);
  assert.equal(result.sss.employeeCents, 125_000n);
  assert.equal(result.sss.employerRegularCents, 250_000n);
  assert.equal(result.sss.ecCents, 3_000n);
  assert.equal(result.sss.employerTotalCents, 253_000n);
  assert.equal(result.philHealth.totalPremiumCents, 125_000n);
  assert.equal(result.philHealth.employeeCents, 62_500n);
  assert.equal(result.philHealth.employerCents, 62_500n);
  assert.equal(result.pagIbig.baseCents, 1_000_000n);
  assert.equal(result.pagIbig.employeeCents, 20_000n);
  assert.equal(result.pagIbig.employerCents, 20_000n);
  assert.equal(result.taxableAfterEmployeeContributionsCents, 2_292_500n);
  assert.equal(result.withholdingTaxCents, 31_380n);
  assert.equal(result.totalEmployeeDeductionsCents, 238_880n);
  assert.equal(result.netPayCents, 2_261_120n);
  assert.equal(result.totalEmployerContributionsCents, 335_500n);
  assert.equal(result.employerCashCostCents, 2_835_500n);
  assert.equal(formatPesoCents(result.netPayCents), '₱22,611.20');
});

test('SSS 2025 schedule salary-credit boundaries and maximum are respected', () => {
  const previewForSss = base => calculatePayrollPreview2026({ ...sample, sssCompensation: base });
  assert.equal(previewForSss('5249.99').sss.mscCents, 500_000n);
  assert.equal(previewForSss('5250.00').sss.mscCents, 550_000n);
  assert.equal(previewForSss('34749.99').sss.mscCents, 3_450_000n);
  assert.equal(previewForSss('34750.00').sss.mscCents, 3_500_000n);
  assert.equal(previewForSss('90000.00').sss.mscCents, 3_500_000n);
  assert.equal(previewForSss('15000.00').sss.ecCents, 3_000n);
  assert.equal(previewForSss('14500.00').sss.ecCents, 1_000n);
});

test('PhilHealth floor/cap and Pag-IBIG tiers/cap are independently applied', () => {
  const floor = calculatePayrollPreview2026({ ...sample, philHealthBasicSalary: '5000.00', pagIbigFundSalary: '1500.00' });
  assert.equal(floor.philHealth.premiumBaseCents, 1_000_000n);
  assert.equal(floor.philHealth.employeeCents, 25_000n);
  assert.equal(floor.pagIbig.employeeCents, 1_500n);
  assert.equal(floor.pagIbig.employerCents, 3_000n);
  const oddCentPremium = calculatePayrollPreview2026({ ...sample, philHealthBasicSalary: '10000.10' });
  assert.equal(oddCentPremium.philHealth.totalPremiumCents, 50_001n);
  assert.equal(oddCentPremium.philHealth.employeeCents + oddCentPremium.philHealth.employerCents, 50_001n);

  const cap = calculatePayrollPreview2026({ ...sample, philHealthBasicSalary: '150000.00', pagIbigFundSalary: '150000.00', grossCashPay: '200000.00' });
  assert.equal(cap.philHealth.premiumBaseCents, 10_000_000n);
  assert.equal(cap.philHealth.employeeCents, 250_000n);
  assert.equal(cap.pagIbig.baseCents, 1_000_000n);
  assert.equal(cap.pagIbig.employeeCents, 20_000n);
});

test('monthly BIR table uses 15% first taxable band, then the printed base-tax bands', () => {
  const contributions = calculatePayrollPreview2026(sample).totalEmployeeStatutoryCents;
  const calculateForTaxableBase = taxable => calculatePayrollPreview2026({
    ...sample,
    grossCashPay: '1000000.00',
    taxableCompensation: formatPesoCents(BigInt(taxable) + contributions).replace('₱', '')
  }).withholdingTaxCents;

  assert.equal(calculateForTaxableBase(2_083_300n), 0n);
  assert.equal(calculateForTaxableBase(2_292_500n), 31_380n);
  assert.equal(calculateForTaxableBase(3_333_300n), 187_500n);
  assert.equal(calculateForTaxableBase(6_666_600n), 854_160n);
  assert.equal(calculateForTaxableBase(6_666_700n), 854_180n);
});

test('rules can be swapped in tests and unsupported periods or negative nets are blocked', () => {
  const changedRules = {
    ...PAYROLL_RULES_2026,
    id: 'test-alternative',
    sss: { ...PAYROLL_RULES_2026.sss, employeeRateBasisPoints: 600 }
  };
  assert.equal(calculatePayrollPreview2026(sample, changedRules).sss.employeeCents, 150_000n);
  assert.throws(() => calculatePayrollPreview2026({ ...sample, period: '2025-11' }), /2026 monthly periods only/);
  assert.throws(() => calculatePayrollPreview2026({ ...sample, grossCashPay: '0' }), /greater than zero/);
  assert.throws(() => calculatePayrollPreview2026({ ...sample, grossCashPay: '100' }), /exceed gross cash pay/);
});
