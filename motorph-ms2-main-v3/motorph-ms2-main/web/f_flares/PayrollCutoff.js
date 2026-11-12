import { formatPesoCents, parsePesoCents, statutoryFromApi } from './PayrollPreview2026.js';

const hours = minutes => (minutes / 60).toFixed(2);

function hoursText(paidMinutes, overageMinutes = 0, overtimeMinutes = 0) {
  let text = hours(paidMinutes);
  if (overtimeMinutes > 0) text += ` + ${hours(overtimeMinutes)} OT`;
  if (overageMinutes > 0) text += ` (+${hours(overageMinutes)} unapproved, unpaid)`;
  return text;
}

const EARNING_LINES = [
  ['basicCents', 'Basic pay (hours worked)'],
  ['premiumCents', 'Rest-day / holiday premium'],
  ['overtimeCents', 'Overtime (approved)'],
  ['nightDiffCents', 'Night differential (22:00–06:00)'],
  ['holidayPayCents', 'Regular holiday pay (not worked)'],
  ['leavePayCents', 'Paid leave']
];

export function buildCutoffPayroll(payload) {
  if (!payload || !Array.isArray(payload.cutoffs) || payload.cutoffs.length !== 2) {
    throw new TypeError('Unexpected payroll response.');
  }
  const cutoffs = payload.cutoffs.map(cutoff => {
    const money = statutoryFromApi({ deductionsCents: cutoff.deductionsCents,
      ...(cutoff.netCents === null ? {} : { netCents: cutoff.netCents }) });
    return {
      label: cutoff.label === '1-15' ? '1–15' : '16–end',
      from: cutoff.from,
      to: cutoff.to,
      daysWithRecords: cutoff.daysWithRecords,
      hours: hoursText(cutoff.paidMinutes, cutoff.unpaidOverageMinutes, cutoff.overtimeMinutes),
      grossCents: parsePesoCents(cutoff.grossPay, 'Cutoff gross')
        + BigInt(cutoff.bonusCents || 0) + BigInt(cutoff.allowanceCents || 0),
      deductionsCents: money.deductionsCents,
      netCents: cutoff.blocked ? null : money.netCents,
      blockedReason: cutoff.blocked
        ? `Deductions ${formatPesoCents(money.deductionsCents)} exceed this cutoff's gross; net not shown rather than negative.`
        : ''
    };
  });
  return {
    month: payload.month,
    hourlyRate: payload.hourlyRate,
    monthlyBasic: payload.monthlyBasic,
    monthlyGrossCents: parsePesoCents(payload.monthlyGross, 'Monthly gross'),
    statutory: payload.statutory ? statutoryFromApi(payload.statutory) : null,
    cutoffs
  };
}

export function payslipRows(payslip) {
  if (!payslip || !payslip.employee || !payslip.deductions) throw new TypeError('Unexpected payslip response.');
  const money = statutoryFromApi({ grossPayCents: payslip.grossPayCents, netPayCents: payslip.netPayCents, ...payslip.deductions });
  const paidHours = hoursText(payslip.paidMinutes, payslip.unpaidOverageMinutes, payslip.overtimeMinutes);
  const days = payslip.daysWithRecords;
  const extras = [];
  if (payslip.leaveDays) extras.push(`${payslip.leaveDays} paid leave day${payslip.leaveDays === 1 ? '' : 's'}`);
  if (payslip.holidayDays) extras.push(`${payslip.holidayDays} unworked regular holiday${payslip.holidayDays === 1 ? '' : 's'}`);
  const earnings = payslip.earnings || { basicCents: payslip.grossPayCents };
  const earningLines = EARNING_LINES
    .filter(([key], index) => index === 0 || Number(earnings[key] || 0) !== 0)
    .map(([key, label]) => [label, formatPesoCents(BigInt(earnings[key] || 0))]);
  const named = item => (item.description ? `${item.type} — ${item.description}` : item.type);
  const bonusLines = (payslip.bonuses || []).map(item => [`${named(item)} (taxable)`, formatPesoCents(BigInt(item.amountCents))]);
  const voluntaryLines = (payslip.deductions.voluntary || []).map(item => [
    item.shortfallCents ? `${named(item)} (${formatPesoCents(BigInt(item.shortfallCents))} not taken: not enough pay)` : named(item),
    `−${formatPesoCents(BigInt(item.amountCents))}`]);
  const allowance = payslip.allowances || {};
  const allowanceLines = [['riceCents', 'Rice subsidy'], ['phoneCents', 'Phone allowance'], ['clothingCents', 'Clothing allowance']]
    .filter(([key]) => Number(allowance[key] || 0) !== 0)
    .map(([key, label]) => [`${label} (non-taxable)`, `+${formatPesoCents(BigInt(allowance[key]))}`]);
  return {
    details: [
      ['PAYSLIP NO.', payslip.payslipId],
      ['EMPLOYEE', `${payslip.employee.name} · ${payslip.employee.id}`],
      ['DEPARTMENT / ROLE', `${payslip.employee.department} · ${payslip.employee.role}`],
      ['PAY PERIOD', `${payslip.period.from} – ${payslip.period.to}`],
      ['PAID HOURS', `${paidHours} h over ${days} attendance day${days === 1 ? '' : 's'} at ₱${payslip.hourlyRate}/h${extras.length ? `; ${extras.join(', ')}` : ''}`]
    ],
    lines: [
      ...earningLines,
      ...bonusLines,
      ['Gross pay', formatPesoCents(money.grossPayCents)],
      ['SSS (employee share)', `−${formatPesoCents(money.sssCents)}`],
      ['PhilHealth (employee share)', `−${formatPesoCents(money.philHealthCents)}`],
      ['Pag-IBIG (employee share)', `−${formatPesoCents(money.pagIbigCents)}`],
      ['Withholding tax', `−${formatPesoCents(money.withholdingTaxCents)}`],
      ...voluntaryLines,
      ['Total deductions', `−${formatPesoCents(money.totalCents)}`],
      ...allowanceLines,
      ['NET PAY', formatPesoCents(money.netPayCents)]
    ],
    footnote: payslip.voided
      ? `VOIDED ${payslip.voidedAt} by ${payslip.voidedBy}: ${payslip.voidReason}. Kept for the record; it is not the payslip in force.`
      : payslip.saved
      ? `Finalized ${payslip.finalizedAt} · ${payslip.rulesetId} · ${payslip.profileVersion}. Saved snapshot: it will not change if attendance, leave or overtime change later. Imported.`
      : `Generated ${payslip.generatedAt} · ${payslip.rulesetId} · ${payslip.profileVersion}. Imported; temporary rules; not saved.`
  };
}

export function savedPayslipRows(items) {
  if (!Array.isArray(items)) throw new TypeError('Unexpected payslip history response.');
  return items.map(item => ({
    id: item.payslipId,
    cells: [item.voided ? `${item.payslipId} · VOID` : (item.version > 1 ? `${item.payslipId} · v${item.version}` : item.payslipId),
      `${item.employeeId} — ${item.employeeName}`, `${item.month} · ${item.cutoff === '1-15' ? '1–15' : '16–end'}`,
      formatPesoCents(BigInt(item.grossPayCents)), formatPesoCents(BigInt(item.netPayCents)), item.finalizedAt]
  }));
}

let finalizeAllowed = () => true;

function renderPayslip(payslip) {
  const view = payslipRows(payslip);
  const fields = document.getElementById('payslip-fields');
  const lines = document.getElementById('payslip-lines');
  fields.replaceChildren();
  view.details.forEach(([label, value]) => {
    const term = document.createElement('dt');
    const description = document.createElement('dd');
    term.textContent = label;
    description.textContent = value;
    fields.append(term, description);
  });
  lines.replaceChildren();
  view.lines.forEach(([label, value], index) => {
    const row = document.createElement('tr');
    if (index === view.lines.length - 1) row.className = 'payslip-net';
    const head = document.createElement('th');
    head.scope = 'row';
    head.textContent = label;
    const cell = document.createElement('td');
    cell.textContent = value;
    row.append(head, cell);
    lines.append(row);
  });
  document.getElementById('payslip-footnote').textContent = view.footnote;
  const finalize = document.getElementById('payslip-finalize');
  finalize.hidden = payslip.saved || document.getElementById('payroll-history').hidden || !finalizeAllowed(payslip.employee.id);
  finalize.dataset.employeeId = payslip.employee.id;
  finalize.dataset.month = payslip.month;
  finalize.dataset.cutoff = payslip.period.label === '1-15' ? '1' : '2';
  document.getElementById('payslip-flag-saved').hidden = !payslip.saved;
  const section = document.getElementById('payroll-payslip');
  section.hidden = false;
  section.focus();
}

export function comparisonRows(payload) {
  if (!payload?.comparison || !payload.statutory) return [];
  const { netCents, motorphNetCents, ...referenceValues } = payload.comparison;
  const referenceMoney = statutoryFromApi(referenceValues);
  const checked = value => {
    if (value == null) return null;
    if (!Number.isSafeInteger(value)) throw new TypeError('Unexpected comparison amount.');
    return BigInt(value);
  };
  const ref = { ...referenceMoney, netCents: checked(netCents), motorphNetCents: checked(motorphNetCents) };
  const actual = statutoryFromApi(payload.statutory);
  const values = [
    ['Earnings including allowances', ref.grossCents, ref.grossCents],
    ['SSS', actual.sss.employeeCents, ref.statutory.sss.employeeCents],
    ['PhilHealth', actual.philHealth.employeeCents, ref.statutory.philHealth.employeeCents],
    ['Pag-IBIG', actual.pagIbig.employeeCents, ref.statutory.pagIbig.employeeCents],
    ['Withholding tax', actual.withholdingTaxCents, ref.statutory.withholdingTaxCents],
    ['Other recorded deductions', ref.voluntaryCents, ref.voluntaryCents],
    ['Total deductions', ref.motorphDeductionsCents, ref.deductionsCents],
    ['Take-home pay', ref.motorphNetCents, ref.netCents]
  ];
  return values.map(([label, motorph, reference]) => [label,
    motorph == null ? 'Unavailable' : formatPesoCents(motorph),
    reference == null ? 'Unavailable' : formatPesoCents(reference),
    motorph == null || reference == null ? 'Unavailable' : formatPesoCents(reference - motorph)]);
}

export function initCutoffPayroll(apiJson, { canFinalize = () => true } = {}) {
  const getJson = url => apiJson(url);
  finalizeAllowed = canFinalize;
  const form = document.getElementById('payroll-cutoff-form');
  if (!form) return { refreshHistory() {} };
  const status = document.getElementById('payroll-cutoff-status');
  const rows = document.getElementById('payroll-cutoff-rows');
  const payslipSection = document.getElementById('payroll-payslip');
  const historyRows = document.getElementById('payroll-history-rows');
  const historyStatus = document.getElementById('payroll-history-status');
  const modes = document.querySelectorAll('[data-pay-mode]');
  const comparison = document.getElementById('pay-comparison');
  const comparisonStatus = document.getElementById('pay-comparison-status');
  const comparisonTable = document.getElementById('pay-comparison-rows');
  const motorphTable = document.getElementById('motorph-cutoffs');
  const modeNote = document.getElementById('pay-mode-note');
  modes.forEach(button => button.addEventListener('click', () => {
    const reference = button.dataset.payMode === 'reference';
    modes.forEach(other => other.setAttribute('aria-pressed', String(other === button)));
    comparison.hidden = !reference;
    motorphTable.hidden = reference;
    if (reference) payslipSection.hidden = true;
    modeNote.textContent = reference
      ? 'Reference estimate and differences. No payroll record is changed.'
      : 'MotorPH uses attendance and the configured payroll rules.';
  }));
  function clearComparison() {
    comparisonTable.replaceChildren();
    comparisonStatus.textContent = 'Calculate a month to compare.';
  }
  form.addEventListener('change', () => {
    clearComparison();
    rows.replaceChildren();
    payslipSection.hidden = true;
    status.textContent = 'Selection changed. Calculate again to see this month.';
  });

  document.getElementById('payslip-print').addEventListener('click', () => window.print());

  async function refreshHistory() {
    try {
      const view = savedPayslipRows((await getJson('/api/payroll/payslips')).payslips);
      historyRows.replaceChildren();
      view.forEach(item => {
        const row = document.createElement('tr');
        item.cells.forEach(text => {
          const cell = document.createElement('td');
          cell.textContent = text;
          row.append(cell);
        });
        const action = document.createElement('td');
        const open = document.createElement('button');
        open.type = 'button';
        open.className = 'button button-quiet';
        open.textContent = 'Open';
        open.dataset.savedId = item.id;
        open.setAttribute('aria-label', `Open saved payslip ${item.id}`);
        action.append(open);
        row.append(action);
        historyRows.append(row);
      });
      historyStatus.textContent = view.length
        ? `${view.length} finalized payslip${view.length === 1 ? '' : 's'} (local memory; cleared when the server restarts).`
        : 'No finalized payslips yet. Open a payslip and choose Finalize payslip.';
    } catch (error) {
      historyStatus.textContent = error instanceof Error ? error.message : 'Could not load payroll history.';
    }
  }

  document.getElementById('payslip-finalize').addEventListener('click', async event => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
      const { employeeId, month, cutoff } = button.dataset;
      const payload = await apiJson('/api/payroll/payslips', { employeeId, month, cutoff });
      renderPayslip(payload.payslip);
      historyStatus.textContent = `Finalized ${payload.payslip.payslipId}.`;
      await refreshHistory();
    } catch (error) {
      historyStatus.textContent = error instanceof Error ? error.message : 'Could not finalize the payslip.';
    } finally {
      button.disabled = false;
    }
  });

  historyRows.addEventListener('click', async event => {
    const button = event.target.closest('button[data-saved-id]');
    if (!button) return;
    try {
      renderPayslip((await getJson(`/api/payroll/payslips/${encodeURIComponent(button.dataset.savedId)}`)).payslip);
    } catch (error) {
      historyStatus.textContent = error instanceof Error ? error.message : 'Could not open the saved payslip.';
    }
  });

  rows.addEventListener('click', async event => {
    const button = event.target.closest('button[data-cutoff]');
    if (!button) return;
    button.disabled = true;
    try {
      const { employeeId, month, cutoff } = button.dataset;
      const payload = await getJson(`/api/payroll/payslip?employeeId=${encodeURIComponent(employeeId)}&month=${encodeURIComponent(month)}&cutoff=${cutoff}`);
      renderPayslip(payload.payslip);
    } catch (error) {
      status.textContent = error instanceof Error ? error.message : 'Could not load the payslip.';
      status.classList.add('payroll-preview-error');
    } finally {
      button.disabled = false;
    }
  });
  form.addEventListener('submit', async event => {
    event.preventDefault();
    status.classList.remove('payroll-preview-error');
    rows.replaceChildren();
    clearComparison();
    payslipSection.hidden = true;
    const employeeId = document.getElementById('payroll-cutoff-employee').value;
    const month = document.getElementById('payroll-cutoff-month').value;
    try {
      if (!employeeId) throw new Error('Choose an employee.');
      const payload = await getJson(`/api/payroll/cutoffs?employeeId=${encodeURIComponent(employeeId)}&month=${encodeURIComponent(month)}`);
      const result = buildCutoffPayroll(payload);
      comparisonTable.replaceChildren();
      const comparisons = comparisonRows(payload);
      comparisons.forEach(cells => {
        const row = document.createElement('tr');
        cells.forEach(text => {
          const cell = document.createElement('td');
          cell.textContent = text;
          row.append(cell);
        });
        comparisonTable.append(row);
      });
      comparisonStatus.textContent = comparisons.length
        ? `${employeeId} · ${result.month}. Positive differences mean the reference is higher. Unavailable indicates deductions exceed a cutoff or the monthly estimate.`
        : 'The reference is unavailable for these inputs.';
      result.cutoffs.forEach((cutoff, index) => {
        const row = document.createElement('tr');
        [`${cutoff.label} (${cutoff.from} – ${cutoff.to})`, String(cutoff.daysWithRecords), cutoff.hours,
          formatPesoCents(cutoff.grossCents), formatPesoCents(cutoff.deductionsCents),
          cutoff.netCents === null ? cutoff.blockedReason : formatPesoCents(cutoff.netCents)]
          .forEach(text => {
            const cell = document.createElement('td');
            cell.textContent = text;
            row.append(cell);
          });
        const action = document.createElement('td');
        if (cutoff.netCents !== null && cutoff.grossCents > 0n) {
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'button button-quiet';
          button.textContent = 'Payslip';
          button.dataset.employeeId = employeeId;
          button.dataset.month = result.month;
          button.dataset.cutoff = String(index + 1);
          button.setAttribute('aria-label', `View payslip for ${employeeId}, ${cutoff.label} ${result.month}`);
          action.append(button);
        } else {
          action.textContent = '—';
        }
        row.append(action);
        rows.append(row);
      });
      const deductions = result.statutory
        ? `Month's statutory deductions: SSS ${formatPesoCents(result.statutory.sss.employeeCents)}, PhilHealth ${formatPesoCents(result.statutory.philHealth.employeeCents)}, Pag-IBIG ${formatPesoCents(result.statutory.pagIbig.employeeCents)}, tax ${formatPesoCents(result.statutory.withholdingTaxCents)}.`
        : 'No statutory calculation is available for this month.';
      status.textContent = `${employeeId} · ${result.month} · rate ₱${result.hourlyRate}/hour · monthly attendance earnings and bonuses ${formatPesoCents(result.monthlyGrossCents)} (before allowances). ${deductions} Not saved. Choose Payslip on a cutoff to view or print it.`;
    } catch (error) {
      status.textContent = error instanceof Error ? error.message : 'Could not calculate cutoff payroll.';
      status.classList.add('payroll-preview-error');
    }
  });
  return { refreshHistory };
}
