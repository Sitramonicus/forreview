/**
 * Payroll presentation helpers. All money arithmetic happens on the server; these functions
 * only format amounts, build the payslip layout, and assemble CSV columns.
 */

const AMOUNT_FIELDS = Object.freeze([
  ['monthlyBasic', 'Monthly Salary'],
  ['dailyRate', 'Daily Rate'],
  ['hourlyRate', 'Hourly Rate'],
  ['overtimePay', 'Overtime'],
  ['nightDifferentialPay', 'Night Differential'],
  ['grossIncome', 'GROSS INCOME'],
  ['riceSubsidy', 'Rice Subsidy'],
  ['phoneAllowance', 'Phone Allowance'],
  ['clothingAllowance', 'Clothing Allowance'],
  ['taxableAllowance', 'Taxable Allowance'],
  ['totalBenefits', 'TOTAL BENEFITS'],
  ['sssEmployee', 'Social Security System'],
  ['philHealthEmployee', 'PhilHealth'],
  ['pagibigEmployee', 'Pag-IBIG'],
  ['withholdingTax', 'Withholding Tax'],
  ['totalDeductions', 'TOTAL DEDUCTIONS'],
  ['takeHomePay', 'TAKE HOME PAY'],
  ['sssEmployer', 'SSS employer share'],
  ['sssEc', "Employer EC"],
  ['philHealthEmployer', 'PhilHealth employer share'],
  ['pagibigEmployer', 'Pag-IBIG employer share'],
  ['employerCost', 'EMPLOYER COST']
]);

/** Money text for display: "26,750.00". Never used for arithmetic. */
export function formatAmount(value) {
  const number = Number(value);
  if (!Number.isFinite(number)) return '0.00';
  return number.toLocaleString('en-PH', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

/** Sections of the payslip in the order MotorPH prints them. */
export function payslipSections(slip) {
  if (!slip || typeof slip !== 'object') throw new TypeError('A payslip object is required.');
  return [
    {
      title: 'EARNINGS',
      rows: [
        { label: 'Monthly Salary', value: slip.monthlyBasic },
        { label: 'Daily Rate', value: slip.dailyRate },
        { label: `Days Worked (${Number(slip.daysWorked)})`, value: null },
        { label: 'Overtime', value: slip.overtimePay },
        { label: 'Night Differential', value: slip.nightDifferentialPay },
        { label: 'GROSS INCOME', value: slip.grossIncome, strong: true }
      ]
    },
    {
      title: 'BENEFITS',
      rows: [
        { label: 'Rice Subsidy', value: slip.riceSubsidy },
        { label: 'Phone Allowance', value: slip.phoneAllowance },
        { label: 'Clothing Allowance', value: slip.clothingAllowance },
        { label: 'Taxable Allowance', value: slip.taxableAllowance },
        { label: 'TOTAL', value: slip.totalBenefits, strong: true }
      ]
    },
    {
      title: 'DEDUCTIONS',
      rows: [
        { label: 'Social Security System', value: slip.sssEmployee },
        { label: 'PhilHealth', value: slip.philHealthEmployee },
        { label: 'Pag-IBIG', value: slip.pagibigEmployee },
        { label: 'Withholding Tax', value: slip.withholdingTax },
        { label: 'TOTAL DEDUCTIONS', value: slip.totalDeductions, strong: true }
      ]
    },
    {
      title: 'SUMMARY',
      rows: [
        { label: 'Gross Income', value: slip.grossIncome },
        { label: 'Benefits', value: slip.totalBenefits },
        { label: 'TOTAL EARNINGS', value: totalEarnings(slip), strong: true },
        { label: 'Deductions', value: slip.totalDeductions },
        { label: 'TAKE HOME PAY', value: slip.takeHomePay, total: true }
      ]
    },
    {
      title: 'EMPLOYER COST (not deducted from pay)',
      rows: [
        { label: 'SSS employer share', value: slip.sssEmployer },
        { label: "Employer EC", value: slip.sssEc },
        { label: 'PhilHealth employer share', value: slip.philHealthEmployer },
        { label: 'Pag-IBIG employer share', value: slip.pagibigEmployer },
        { label: 'EMPLOYER COST', value: slip.employerCost, strong: true }
      ]
    }
  ];
}

/** Remittance rows: employee plus employer share per agency. */
export function remittanceRows(slip) {
  if (!slip || typeof slip !== 'object') throw new TypeError('A payslip object is required.');
  const sum = (a, b) => (Number(a || 0) + Number(b || 0)).toFixed(2);
  return [
    ['Social Security System', sum(slip.sssEmployee, slip.sssEmployer)],
    ["Employees' Compensation", sum(slip.sssEc, 0)],
    ['PhilHealth', sum(slip.philHealthEmployee, slip.philHealthEmployer)],
    ['Pag-IBIG', sum(slip.pagibigEmployee, slip.pagibigEmployer)],
    ['BIR withholding', sum(slip.withholdingTax, 0)]
  ];
}

/** Flat CSV row for one payslip, using the same labels as the printed layout. */
export function payslipCsvColumns() {
  return [
    { key: 'employeeId', label: 'Employee ID' },
    { key: 'employeeName', label: 'Employee Name' },
    { key: 'position', label: 'Position' },
    { key: 'department', label: 'Department' },
    { key: 'payPeriod', label: 'Pay Period' },
    { key: 'periodStart', label: 'Period Start' },
    { key: 'periodEnd', label: 'Period End' },
    { key: 'rulesetId', label: 'Ruleset' },
    ...AMOUNT_FIELDS.map(([key, label]) => ({ key, label })),
    { key: 'daysWorked', label: 'Days Worked' },
    { key: 'overtimeHours', label: 'Overtime Hours' },
    { key: 'nightDifferentialHours', label: 'Night Differential Hours' }
  ];
}

/** One CSV row per payslip, amounts formatted to two decimals. */
export function payslipCsvRows(payslips) {
  if (!Array.isArray(payslips)) throw new TypeError('Payslips must be an array.');
  return payslips.map(slip => {
    const row = {
      employeeId: slip.employeeId,
      employeeName: slip.employeeName,
      position: slip.position,
      department: slip.department,
      payPeriod: slip.payPeriod,
      periodStart: slip.periodStart,
      periodEnd: slip.periodEnd,
      rulesetId: slip.rulesetId,
      daysWorked: slip.daysWorked,
      overtimeHours: slip.overtimeHours,
      nightDifferentialHours: slip.nightDifferentialHours
    };
    for (const [key] of AMOUNT_FIELDS) row[key] = formatAmount(slip[key]).replace(/,/g, '');
    return row;
  });
}

/** Human label for the calculation assumptions currently selected. */
export function basisSummary({ taxBasis, statutoryBasis } = {}) {
  const tax = taxBasis === 'BASIC_PLUS_TAXABLE_ALLOWANCE'
    ? 'taxable base includes allowance' : 'taxable base is monthly basic less statutory';
  const statutory = statutoryBasis === 'SPLIT_EVENLY'
    ? 'statutory amounts split across cutoffs' : 'full monthly statutory each cutoff';
  return `${tax} · ${statutory}`;
}

/**
 * Run-history figures. The API sends gross, benefits, earnings, deductions, net, and employer
 * cost; the client never re-derives money, but an older saved run may predate totalEarnings, so
 * fall back to gross + benefits rather than printing a blank cell.
 */
export function runSummary(run = {}) {
  const gross = formatAmount(run.totalGross);
  const benefits = formatAmount(run.totalBenefits);
  const earnings = formatAmount(run.totalEarnings
    ?? (Number(run.totalGross || 0) + Number(run.totalBenefits || 0)));
  return {
    id: run.id || '',
    period: `${run.periodStart || ''} — ${run.periodEnd || ''} (${run.payPeriod === 'MONTHLY' ? 'monthly' : 'semi-monthly'})`,
    rulesetId: run.rulesetId || '',
    employees: String(run.employeeCount ?? 0),
    gross, benefits, earnings,
    deductions: formatAmount(run.totalDeductions),
    net: formatAmount(run.totalNet),
    employerCost: formatAmount(run.totalEmployerCost)
  };
}

/**
 * Gross income plus the allowance total: the figure the summary must reconcile to.
 * Returns a raw decimal string like every other payslip row; the view formats it.
 */
export function totalEarnings(slip = {}) {
  const sum = Number(slip.grossIncome || 0) + Number(slip.totalBenefits || 0);
  return (Number.isFinite(sum) ? sum : 0).toFixed(2);
}
