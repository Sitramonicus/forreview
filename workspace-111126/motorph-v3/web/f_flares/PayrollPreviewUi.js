import { formatPesoCents, statutoryFromApi } from './PayrollPreview2026.js';
import { buildPayrollPreviewCsv } from './PayrollPreviewExport.js';

function byId(id) {
  return document.getElementById(id);
}

function value(id) {
  return byId(id).value.trim();
}

function setMoney(id, cents) {
  byId(id).textContent = formatPesoCents(cents);
}

function addBreakdownRow(body, label, employeeCents, employerCents, note) {
  const row = document.createElement('tr');
  const cells = [label, employeeCents === null ? '—' : formatPesoCents(employeeCents),
    employerCents === null ? '—' : formatPesoCents(employerCents), note];
  cells.forEach((text, index) => {
    const cell = document.createElement(index === 0 ? 'th' : 'td');
    if (index === 0) cell.scope = 'row';
    cell.textContent = text;
    row.append(cell);
  });
  body.append(row);
}

export function initPayrollPreview(postJson) {
  const form = byId('payroll-preview-form');
  if (!form) return;
  const status = byId('payroll-preview-status');
  const results = byId('payroll-preview-results');
  const exportButton = byId('payroll-preview-export');
  let lastResult = null;

  exportButton.addEventListener('click', () => {
    if (!lastResult) return;
    const blob = new Blob([buildPayrollPreviewCsv(lastResult)], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'motorph-2026-monthly-preview-not-payslip.csv';
    link.hidden = true;
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    byId('payroll-preview-export-status').textContent = 'Downloaded a temporary imported preview CSV; it is not a payslip or payment record.';
  });

  form.addEventListener('submit', async event => {
    event.preventDefault();
    status.classList.remove('payroll-preview-error');
    results.hidden = true;
    lastResult = null;
    exportButton.disabled = true;
    byId('payroll-preview-export-status').textContent = '';
    status.textContent = 'Calculating…';
    const input = {
      period: value('payroll-preview-period'),
      grossCashPay: value('payroll-preview-gross'),
      sssCompensation: value('payroll-preview-sss'),
      philHealthBasicSalary: value('payroll-preview-ph'),
      pagIbigFundSalary: value('payroll-preview-pagibig'),
      taxableCompensation: value('payroll-preview-taxable')
    };

    try {
      const result = statutoryFromApi((await postJson('/api/payroll/statutory', input)).result);
      lastResult = result;
      exportButton.disabled = false;
      byId('payroll-results-ruleset').textContent = `${result.rulesetId} · ${result.period}`;
      setMoney('payroll-result-net', result.netPayCents);
      setMoney('payroll-result-employee-total', result.totalEmployeeDeductionsCents);
      setMoney('payroll-result-employer-total', result.totalEmployerContributionsCents);
      setMoney('payroll-result-employer-cost', result.employerCashCostCents);

      const lines = byId('payroll-result-lines');
      lines.replaceChildren();
      addBreakdownRow(lines, 'SSS Social Security + MPF', result.sss.employeeCents,
        result.sss.employerRegularCents, `MSC ${formatPesoCents(result.sss.mscCents)}; 5% employee / 10% employer`);
      addBreakdownRow(lines, 'SSS Employees’ Compensation', 0n, result.sss.ecCents,
        'Employer-only; included in employer contributions total');
      addBreakdownRow(lines, 'PhilHealth premium', result.philHealth.employeeCents,
        result.philHealth.employerCents,
        `5% of MBS base ${formatPesoCents(result.philHealth.premiumBaseCents)}, split equally`);
      addBreakdownRow(lines, 'Pag-IBIG monthly savings', result.pagIbig.employeeCents,
        result.pagIbig.employerCents,
        `Fund Salary base ${formatPesoCents(result.pagIbig.baseCents)} after ₱10,000 cap`);
      addBreakdownRow(lines, 'BIR withholding tax', result.withholdingTaxCents, null,
        `Monthly taxable base ${formatPesoCents(result.taxableAfterEmployeeContributionsCents)} after employee shares`);
      results.hidden = false;
      status.textContent = 'Calculated by the Java payroll service from the entered imported bases. Nothing was saved.';
    } catch (error) {
      lastResult = null;
      exportButton.disabled = true;
      status.textContent = error instanceof Error ? error.message : 'Could not calculate preview.';
      status.classList.add('payroll-preview-error');
    }
  });
}
