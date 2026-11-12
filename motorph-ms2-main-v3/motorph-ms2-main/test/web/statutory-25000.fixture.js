// The Java /api/payroll/statutory "result" for ₱25,000 in every base (2026-01), as returned by AppServer.
// Historical local regression fixture; it is not evidence of external-site parity.
export const STATUTORY_25000_API_RESULT = {
  rulesetId: 'PH-2026-MOTORPH-ESTIMATE-v5',
  period: '2026-01',
  grossCashPayCents: 2500000,
  sssCompensationCents: 2500000,
  philHealthBasicSalaryCents: 2500000,
  pagIbigFundSalaryCents: 2500000,
  sss: { mscCents: 2500000, employeeCents: 125000, employerRegularCents: 250000, ecCents: 3000, employerTotalCents: 253000 },
  philHealth: { premiumBaseCents: 2500000, totalPremiumCents: 125000, employeeCents: 62500, employerCents: 62500 },
  pagIbig: { baseCents: 1000000, employeeCents: 20000, employerCents: 20000 },
  taxableCompensationCents: 2500000,
  taxableAfterEmployeeContributionsCents: 2292500,
  withholdingTaxCents: 31375,
  totalEmployeeStatutoryCents: 207500,
  totalEmployeeDeductionsCents: 238875,
  netPayCents: 2261125,
  totalEmployerContributionsCents: 335500,
  employerCashCostCents: 2835500
};
