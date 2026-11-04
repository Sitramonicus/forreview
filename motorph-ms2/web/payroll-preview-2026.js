import { PAYROLL_RULES_2026 } from './payroll-rules-2026.js';

const CENTS_PER_PESO = 100n;
const BASIS_POINT_DENOMINATOR = 10000n;

export function parsePesoCents(value, fieldName = 'amount') {
  const text = String(value ?? '').trim().replace(/,/g, '');
  if (!/^\d+(?:\.\d{1,2})?$/.test(text)) {
    throw new TypeError(`${fieldName} must be a non-negative peso amount with up to two decimal places.`);
  }
  const [wholeText, fractionText = ''] = text.split('.');
  return BigInt(wholeText) * CENTS_PER_PESO + BigInt(fractionText.padEnd(2, '0'));
}

export function formatPesoCents(cents) {
  const value = BigInt(cents);
  const sign = value < 0n ? '-' : '';
  const absolute = value < 0n ? -value : value;
  const pesos = (absolute / CENTS_PER_PESO).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const centavos = (absolute % CENTS_PER_PESO).toString().padStart(2, '0');
  return `${sign}₱${pesos}.${centavos}`;
}

function toCentsFromRule(pesos, label) {
  return parsePesoCents(String(pesos), label);
}

function roundPositiveRatio(numerator, denominator) {
  return (numerator + denominator / 2n) / denominator;
}

function applyBasisPoints(cents, basisPoints) {
  return roundPositiveRatio(cents * BigInt(basisPoints), BASIS_POINT_DENOMINATOR);
}

function sssMonthlySalaryCredit(compensationCents, rules) {
  const minimum = toCentsFromRule(rules.minimumMscPesos, 'SSS minimum MSC');
  const maximum = toCentsFromRule(rules.maximumMscPesos, 'SSS maximum MSC');
  const firstHigherRange = toCentsFromRule(rules.firstHigherRangePesos, 'SSS first higher range');
  const compensationStep = toCentsFromRule(rules.rangeStepPesos, 'SSS compensation range step');
  const mscStep = toCentsFromRule(rules.mscStepPesos, 'SSS MSC step');

  if (compensationCents < firstHigherRange) return minimum;
  const increments = (compensationCents - firstHigherRange) / compensationStep + 1n;
  const msc = minimum + increments * mscStep;
  return msc > maximum ? maximum : msc;
}

function computeSss(compensationCents, rules) {
  const mscCents = sssMonthlySalaryCredit(compensationCents, rules);
  const employeeCents = applyBasisPoints(mscCents, rules.employeeRateBasisPoints);
  const employerRegularCents = applyBasisPoints(mscCents, rules.employerRateBasisPoints);
  const ecThreshold = toCentsFromRule(rules.ecThresholdMscPesos, 'SSS EC threshold');
  const ecCents = toCentsFromRule(
    mscCents < ecThreshold ? rules.ecBelowThresholdPesos : rules.ecAtOrAboveThresholdPesos,
    'SSS Employees’ Compensation'
  );
  return {
    mscCents,
    employeeCents,
    employerRegularCents,
    ecCents,
    employerTotalCents: employerRegularCents + ecCents
  };
}

function computePhilHealth(monthlyBasicSalaryCents, rules) {
  const floor = toCentsFromRule(rules.baseFloorPesos, 'PhilHealth contribution floor');
  const ceiling = toCentsFromRule(rules.baseCeilingPesos, 'PhilHealth contribution ceiling');
  const premiumBaseCents = monthlyBasicSalaryCents < floor
    ? floor
    : monthlyBasicSalaryCents > ceiling ? ceiling : monthlyBasicSalaryCents;
  const premiumCents = applyBasisPoints(premiumBaseCents, rules.premiumRateBasisPoints);
  const employeeShareBasisPoints = BigInt(rules.employeeShareBasisPoints);
  const employerShareBasisPoints = BigInt(rules.employerShareBasisPoints);
  if (employeeShareBasisPoints + employerShareBasisPoints !== BASIS_POINT_DENOMINATOR) {
    throw new RangeError('PhilHealth employee and employer shares must total 100%.');
  }
  const employeeCents = applyBasisPoints(premiumCents, rules.employeeShareBasisPoints);
  return {
    premiumBaseCents,
    totalPremiumCents: premiumCents,
    employeeCents,
    employerCents: premiumCents - employeeCents
  };
}

function computePagIbig(fundSalaryCents, rules) {
  const ceiling = toCentsFromRule(rules.fundSalaryCeilingPesos, 'Pag-IBIG MFS ceiling');
  const lowBandCeiling = toCentsFromRule(rules.employeeLowBandCeilingPesos, 'Pag-IBIG employee-rate threshold');
  const baseCents = fundSalaryCents > ceiling ? ceiling : fundSalaryCents;
  const employeeRate = fundSalaryCents <= lowBandCeiling
    ? rules.employeeLowRateBasisPoints
    : rules.employeeHighRateBasisPoints;
  return {
    baseCents,
    employeeCents: applyBasisPoints(baseCents, employeeRate),
    employerCents: applyBasisPoints(baseCents, rules.employerRateBasisPoints)
  };
}

function computeMonthlyBirWithholding(taxableCents, rules) {
  const firstThresholdCents = toCentsFromRule(rules.firstTaxablePesoThreshold, 'BIR first threshold');
  if (taxableCents <= firstThresholdCents) return 0n;
  const brackets = rules.brackets;
  for (const bracket of brackets) {
    const floorCents = toCentsFromRule(bracket.floorPesos, 'BIR bracket floor');
    const belowCents = bracket.belowPesos === null
      ? null
      : toCentsFromRule(bracket.belowPesos, 'BIR bracket upper bound');
    if (belowCents === null || taxableCents < belowCents) {
      const baseTaxCents = toCentsFromRule(bracket.baseTaxPesos, 'BIR base tax');
      const excessCents = taxableCents > floorCents ? taxableCents - floorCents : 0n;
      return baseTaxCents + applyBasisPoints(excessCents, bracket.rateBasisPoints);
    }
  }
  return 0n;
}

/**
 * Computes a disposable, synthetic-only 2026 MONTHLY statutory preview.
 * Inputs are kept distinct because SSS compensation, PhilHealth monthly basic
 * salary, Pag-IBIG Fund Salary, tax compensation, and cash pay need not be the
 * same amount. Nothing is persisted or sent to the API.
 */
export function calculatePayrollPreview2026(input, rules = PAYROLL_RULES_2026) {
  if (!input || typeof input !== 'object') throw new TypeError('Enter synthetic monthly pay bases.');
  if (rules.effectiveYear !== 2026 || !/^2026-(0[1-9]|1[0-2])$/.test(String(input.period || ''))) {
    throw new RangeError('This temporary ruleset covers 2026 monthly periods only.');
  }

  const grossCashPayCents = parsePesoCents(input.grossCashPay, 'Gross cash pay');
  const sssCompensationCents = parsePesoCents(input.sssCompensation, 'SSS compensation base');
  const philHealthBasicSalaryCents = parsePesoCents(input.philHealthBasicSalary, 'PhilHealth monthly basic salary');
  const pagIbigFundSalaryCents = parsePesoCents(input.pagIbigFundSalary, 'Pag-IBIG Fund Salary');
  const taxableCompensationCents = parsePesoCents(input.taxableCompensation, 'Taxable compensation before employee contributions');

  if (grossCashPayCents <= 0n) throw new RangeError('Gross cash pay must be greater than zero for this preview.');
  if (sssCompensationCents <= 0n || philHealthBasicSalaryCents <= 0n || pagIbigFundSalaryCents <= 0n) {
    throw new RangeError('Enter a positive contribution base for SSS, PhilHealth, and Pag-IBIG.');
  }

  const sss = computeSss(sssCompensationCents, rules.sss);
  const philHealth = computePhilHealth(philHealthBasicSalaryCents, rules.philHealth);
  const pagIbig = computePagIbig(pagIbigFundSalaryCents, rules.pagIbig);
  const totalEmployeeStatutoryCents = sss.employeeCents + philHealth.employeeCents + pagIbig.employeeCents;
  const taxableAfterEmployeeContributionsCents = taxableCompensationCents > totalEmployeeStatutoryCents
    ? taxableCompensationCents - totalEmployeeStatutoryCents
    : 0n;
  const withholdingTaxCents = computeMonthlyBirWithholding(taxableAfterEmployeeContributionsCents, rules.birMonthly);
  const totalEmployeeDeductionsCents = totalEmployeeStatutoryCents + withholdingTaxCents;
  if (totalEmployeeDeductionsCents > grossCashPayCents) {
    throw new RangeError('Employee deductions exceed gross cash pay; preview blocked rather than showing negative net pay.');
  }

  const totalEmployerContributionsCents = sss.employerTotalCents + philHealth.employerCents + pagIbig.employerCents;
  return Object.freeze({
    rulesetId: rules.id,
    period: input.period,
    grossCashPayCents,
    sssCompensationCents,
    philHealthBasicSalaryCents,
    pagIbigFundSalaryCents,
    sss: Object.freeze(sss),
    philHealth: Object.freeze(philHealth),
    pagIbig: Object.freeze(pagIbig),
    taxableCompensationCents,
    taxableAfterEmployeeContributionsCents,
    withholdingTaxCents,
    totalEmployeeStatutoryCents,
    totalEmployeeDeductionsCents,
    netPayCents: grossCashPayCents - totalEmployeeDeductionsCents,
    totalEmployerContributionsCents,
    employerCashCostCents: grossCashPayCents + totalEmployerContributionsCents
  });
}
