import test from 'node:test';
import assert from 'node:assert/strict';
import { formatAmount, payslipSections, remittanceRows, payslipCsvRows, payslipCsvColumns, basisSummary, runSummary, totalEarnings } from '../../web/payroll.js';

const golden = {
  employeeId: '15', employeeName: 'Romualdez, Fredrick', position: 'Account Manager', department: 'Accounting',
  payPeriod: 'SEMI_MONTHLY', periodStart: '2026-01-02', periodEnd: '2026-01-15', rulesetId: 'PH-STATUTORY-2026',
  monthlyBasic: '53500.00', dailyRate: '2675.00', daysWorked: '10.00', hourlyRate: '318.45',
  overtimeHours: '0', overtimePay: '0.00', nightDifferentialHours: '0', nightDifferentialPay: '0.00',
  grossIncome: '26750.00', riceSubsidy: '1500.00', phoneAllowance: '2000.00', clothingAllowance: '1000.00',
  taxableAllowance: '0.00', nonTaxableAllowance: '4500.00', totalBenefits: '4500.00',
  sssEmployee: '1750.00', philHealthEmployee: '1337.50', pagibigEmployee: '200.00',
  withholdingTax: '5250.83', totalDeductions: '8538.33',
  sssEmployer: '3500.00', sssEc: '30.00', philHealthEmployer: '1337.50', pagibigEmployer: '200.00',
  employerCost: '36317.50', taxableIncome: '50212.50', takeHomePay: '22711.67'
};

test('formatAmount renders two decimals with thousands separators', () => {
  assert.equal(formatAmount('26750'), '26,750.00');
  assert.equal(formatAmount(1337.5), '1,337.50');
  assert.equal(formatAmount('not-a-number'), '0.00');
});

test('payslip sections follow the supplied MotorPH layout', () => {
  const sections = payslipSections(golden);
  assert.deepEqual(sections.map(s => s.title), [
    'EARNINGS', 'BENEFITS', 'DEDUCTIONS', 'SUMMARY', 'EMPLOYER COST (not deducted from pay)'
  ]);
  const deductions = sections.find(s => s.title === 'DEDUCTIONS').rows;
  assert.equal(deductions.find(r => r.label === 'Social Security System').value, '1750.00');
  assert.equal(deductions.find(r => r.label === 'TOTAL DEDUCTIONS').value, '8538.33');
  const summary = sections.find(s => s.title === 'SUMMARY').rows;
  assert.equal(summary.find(r => r.label === 'TAKE HOME PAY').value, '22711.67');
  assert.equal(summary.find(r => r.label === 'TAKE HOME PAY').total, true);
});

test('payslipSections rejects missing input', () => {
  assert.throws(() => payslipSections(null), TypeError);
  assert.throws(() => payslipSections('x'), TypeError);
});

test('remittanceRows adds employee and employer shares', () => {
  const rows = remittanceRows(golden);
  assert.deepEqual(rows, [
    ['Social Security System', '5250.00'],
    ["Employees' Compensation", '30.00'],
    ['PhilHealth', '2675.00'],
    ['Pag-IBIG', '400.00'],
    ['BIR withholding', '5250.83']
  ]);
});

test('payslip CSV keeps all amounts and no thousands separators', () => {
  const rows = payslipCsvRows([golden]);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].takeHomePay, '22711.67');
  assert.equal(rows[0].sssEmployee, '1750.00');
  assert.equal(rows[0].monthlyBasic, '53500.00');
  assert.equal(rows[0].rulesetId, 'PH-STATUTORY-2026');
  assert.ok(payslipCsvColumns().length >= 30);
});

test('payslipCsvRows rejects a non-array', () => {
  assert.throws(() => payslipCsvRows('x'), TypeError);
});

test('basisSummary names the assumptions in force', () => {
  assert.match(basisSummary({ taxBasis: 'BASIC_ONLY', statutoryBasis: 'FULL_MONTHLY_EACH_PERIOD' }), /less statutory/);
  assert.match(basisSummary({ taxBasis: 'BASIC_PLUS_TAXABLE_ALLOWANCE' }), /includes allowance/);
  assert.match(basisSummary({ statutoryBasis: 'SPLIT_EVENLY' }), /split across cutoffs/);
});

test('runSummary keeps gross, benefits, and earnings distinct', () => {
  const summary = runSummary({
    id: 'PR-20260102-77BC9231', payPeriod: 'SEMI_MONTHLY', periodStart: '2026-01-02', periodEnd: '2026-01-15',
    rulesetId: 'PH-STATUTORY-2026', employeeCount: 1,
    totalGross: '26750.00', totalBenefits: '4500.00', totalEarnings: '31250.00',
    totalDeductions: '8538.33', totalNet: '22711.67', totalEmployerCost: '36317.50'
  });
  assert.equal(summary.gross, '26,750.00');
  assert.equal(summary.benefits, '4,500.00');
  assert.equal(summary.earnings, '31,250.00');
  assert.equal(summary.net, '22,711.67');
  assert.equal(summary.period, '2026-01-02 — 2026-01-15 (semi-monthly)');
});

test('runSummary falls back to gross plus benefits for runs saved before the split', () => {
  const legacy = runSummary({ id: 'PR-1', totalGross: '26750.00', totalBenefits: '4500.00', totalNet: '22711.67' });
  assert.equal(legacy.earnings, '31,250.00');
  assert.equal(legacy.employees, '0');
});

test('totalEarnings reconciles gross income plus benefits as a raw decimal', () => {
  assert.equal(totalEarnings(golden), '31250.00');
  assert.equal(totalEarnings({ grossIncome: '26750.00', totalBenefits: '4500.00' }), '31250.00');
  assert.equal(totalEarnings({}), '0.00');
  // The payslip row must stay raw so the view formats it exactly once.
  assert.doesNotMatch(totalEarnings(golden), /,/);
});

test('payslip summary shows total earnings between benefits and deductions', () => {
  const summary = payslipSections(golden).find(section => section.title === 'SUMMARY');
  const labels = summary.rows.map(row => row.label);
  assert.deepEqual(labels, ['Gross Income', 'Benefits', 'TOTAL EARNINGS', 'Deductions', 'TAKE HOME PAY']);
  // Rows carry raw decimal strings; the view formats them with formatAmount.
  assert.equal(formatAmount(summary.rows[2].value), '31,250.00');
  assert.equal(formatAmount(summary.rows[4].value), '22,711.67');
  assert.equal(summary.rows[2].strong, true);
});
