import { buildCsv } from '../b_basic/CsvExport.js';
import { formatPesoCents } from './PayrollPreview2026.js';

export const PAYROLL_PREVIEW_CSV_COLUMNS = [
  { key: 'section', label: 'Section' },
  { key: 'item', label: 'Item' },
  { key: 'amount', label: 'Input amount or estimate' },
  { key: 'employeeShare', label: 'Employee share' },
  { key: 'employerShare', label: 'Employer share' },
  { key: 'basisNote', label: 'Basis or note' }
];

export function payrollPreviewCsvRows(result) {
  if (!result || typeof result !== 'object' || !result.sss || !result.philHealth || !result.pagIbig) {
    throw new TypeError('A completed imported preview result is required.');
  }
  const peso = cents => formatPesoCents(cents);
  return [
    { section: 'Preview metadata', item: 'Ruleset', amount: result.rulesetId, basisNote: 'Temporary statutory reference; not a payroll run' },
    { section: 'Preview metadata', item: 'Period', amount: result.period, basisNote: 'Monthly 2026 profile only' },
    { section: 'Input basis', item: 'Gross cash pay', amount: peso(result.grossCashPayCents), basisNote: 'Imported user-entered amount' },
    { section: 'Input basis', item: 'SSS compensation base', amount: peso(result.sssCompensationCents), basisNote: `Applied MSC ${peso(result.sss.mscCents)}` },
    { section: 'Input basis', item: 'PhilHealth monthly basic salary', amount: peso(result.philHealthBasicSalaryCents), basisNote: `Applied premium base ${peso(result.philHealth.premiumBaseCents)} after floor/cap` },
    { section: 'Input basis', item: 'Pag-IBIG Fund Salary', amount: peso(result.pagIbigFundSalaryCents), basisNote: `Applied contribution base ${peso(result.pagIbig.baseCents)} after ₱10,000 cap` },
    { section: 'Input basis', item: 'Taxable compensation before employee shares', amount: peso(result.taxableCompensationCents), basisNote: 'Imported user-entered amount' },
    { section: 'Contribution', item: 'SSS Social Security + MPF', employeeShare: peso(result.sss.employeeCents), employerShare: peso(result.sss.employerRegularCents), basisNote: `MSC ${peso(result.sss.mscCents)}` },
    { section: 'Contribution', item: 'SSS Employees’ Compensation', employeeShare: peso(0n), employerShare: peso(result.sss.ecCents), basisNote: 'Employer-only' },
    { section: 'Contribution', item: 'PhilHealth', employeeShare: peso(result.philHealth.employeeCents), employerShare: peso(result.philHealth.employerCents), basisNote: `Premium ${peso(result.philHealth.totalPremiumCents)}` },
    { section: 'Contribution', item: 'Pag-IBIG', employeeShare: peso(result.pagIbig.employeeCents), employerShare: peso(result.pagIbig.employerCents), basisNote: `Fund Salary ${peso(result.pagIbig.baseCents)}` },
    { section: 'Withholding', item: 'BIR withholding estimate (annualised)', employeeShare: peso(result.withholdingTaxCents), basisNote: `Taxable base after employee shares ${peso(result.taxableAfterEmployeeContributionsCents)}` },
    { section: 'Estimate', item: 'Total employee deductions', employeeShare: peso(result.totalEmployeeDeductionsCents), basisNote: 'Statutory shares plus withholding estimate' },
    { section: 'Estimate', item: 'Total employer contributions', employerShare: peso(result.totalEmployerContributionsCents), basisNote: 'Does not include employer salary cost' },
    { section: 'Estimate', item: 'Net cash pay estimate', amount: peso(result.netPayCents), basisNote: 'Not a payslip or payment instruction' },
    { section: 'Estimate', item: 'Gross pay plus employer contributions', amount: peso(result.employerCashCostCents), basisNote: 'Imported comparison only' }
  ];
}

export function buildPayrollPreviewCsv(result) {
  return buildCsv(PAYROLL_PREVIEW_CSV_COLUMNS, payrollPreviewCsvRows(result));
}
