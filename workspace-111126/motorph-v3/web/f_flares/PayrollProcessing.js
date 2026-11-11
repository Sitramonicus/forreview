import { formatPesoCents } from './PayrollPreview2026.js';

const peso = cents => formatPesoCents(BigInt(cents));
const cutoffLabel = cutoff => (Number(cutoff) === 1 ? '1–15' : '16–end');

import { canReadCompanyArea } from '../b_basic/Roles.js';
export const ALLOWANCE_TYPES = ['Rice subsidy adjustment', 'Phone allowance adjustment', 'Clothing allowance adjustment', 'Reimbursement', 'Cash advance'];

export function adjustmentRows(payload, employeeId) {
  if (!payload || !Array.isArray(payload.earnings) || !Array.isArray(payload.deductions)) throw new TypeError('Unexpected payroll items response.');
  const earnings = payload.earnings.filter(item => item.employeeId === employeeId).map(item => ({
    id: item.id,
    cells: [ALLOWANCE_TYPES.includes(item.type) ? 'Non-taxable' : 'Earning (taxable)', item.type, item.description || '—', `${item.month} · ${cutoffLabel(item.cutoff)}`, `+${peso(item.amountCents)}`]
  }));
  const deductions = payload.deductions.filter(item => item.employeeId === employeeId).map(item => ({
    id: item.id,
    cells: ['Voluntary deduction', item.type, item.description || '—', `${item.fromMonth} → ${item.toMonth || 'until removed'} · every cutoff`, `−${peso(item.amountPerCutoffCents)}`]
  }));
  return [...earnings, ...deductions];
}

export function allowanceText(payload, employeeId) {
  const item = (payload.allowances || []).find(entry => entry.employeeId === employeeId);
  if (!item) return '';
  return `Monthly allowances (non-taxable, half each cutoff): rice ${peso(item.riceCents)} · phone ${peso(item.phoneCents)} · clothing ${peso(item.clothingCents)}. Paid by ${item.payMethod}.`;
}

export function batchRows(batches) {
  if (!Array.isArray(batches)) throw new TypeError('Unexpected disbursement response.');
  return batches.map(batch => [batch.status ? `${batch.id} · ${batch.status}` : batch.id, `${batch.month} · ${cutoffLabel(batch.cutoff)}`, `${batch.releasedBy} · ${batch.releasedAt}`,
    batch.lines.map(line => `${line.employeeName} ${peso(line.netCents)} (${line.method})`).join('; '), peso(batch.totalCents)]);
}

export function remittanceRows(payload) {
  if (!payload || !Array.isArray(payload.agencies)) throw new TypeError('Unexpected remittance response.');
  return payload.agencies.map(item => [item.agency, peso(item.employeeCents), item.agency.startsWith('BIR') ? '—' : peso(item.employerCents), peso(item.totalCents)]);
}

export function summaryRows(payload) {
  if (!payload || !Array.isArray(payload.groups)) throw new TypeError('Unexpected payroll summary response.');
  const row = (name, item) => [name, String(item.employees), String(item.payslips), peso(item.grossCents), `−${peso(item.deductionsCents)}`,
    `+${peso(item.allowancesCents)}`, peso(item.netCents)];
  if (!payload.groups.length) return [];
  return [...payload.groups.map(item => row(item.name, item)), row('TOTAL', payload.total)];
}

function fillTable(tbody, rows, columns, emptyText) {
  tbody.replaceChildren();
  if (!rows.length) {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = columns;
    cell.className = 'table-message';
    cell.textContent = emptyText;
    row.append(cell);
    tbody.append(row);
    return;
  }
  rows.forEach(values => {
    const row = document.createElement('tr');
    values.forEach(value => {
      const cell = document.createElement('td');
      if (value instanceof Node) cell.append(value);
      else cell.textContent = value;
      row.append(cell);
    });
    tbody.append(row);
  });
}

export function groupOptions(employees, accounts) {
  const names = new Map((accounts || []).map(item => [item.employeeId, item.name]));
  const supervisors = [...new Set((accounts || []).map(item => item.supervisorId).filter(Boolean))].sort();
  const distinct = key => [...new Set((employees || []).filter(item => item.status !== 'Separated').map(item => item[key]).filter(Boolean))].sort();
  return [['ALL', 'Everyone'],
    ...supervisors.map(id => [`TEAM:${id}`, `Team of ${names.get(id) || id}`]),
    ...distinct('location').map(name => [`BRANCH:${name}`, `Branch: ${name}`]),
    ...distinct('department').map(name => [`DEPARTMENT:${name}`, `Department: ${name}`])];
}

export function groupResultText(payload, label) {
  const skipped = payload.skipped.map(item => `${item.employeeId} (${item.reason})`).join(', ');
  return `Added to ${payload.added.length} employee(s) in ${label}.${skipped ? ` Skipped: ${skipped}.` : ''} Each is a separate item you can remove.`;
}

export function correctionSummary(request) {
  const what = request.kind === 'VOID' ? 'Void (not yet released)' : `Adjust on ${request.targetMonth} · ${cutoffLabel(request.targetCutoff)}`;
  return `${request.payslipId}: ${what}`;
}

export function initPayrollProcessing(apiJson, canEdit = () => true, getAccount = () => null, onPayslipsChanged = () => {},
  getDirectory = () => ({ employees: [], accounts: [] })) {
  const employee = document.getElementById('adj-employee');
  if (!employee) return { refresh() {}, refreshTargets() {} };
  const target = document.getElementById('adj-target');

  function refreshTargets() {
    const current = target.value;
    const { employees, accounts } = getDirectory();
    target.replaceChildren(new Option('This employee only', ''), ...groupOptions(employees, accounts).map(([value, label]) => new Option(label, value)));
    if ([...target.options].some(option => option.value === current)) target.value = current;
  }

  
  async function postGroup(kind, fields) {
    if (!target.value) return null;
    const label = target.selectedOptions[0].textContent;
    const payload = await apiJson('/api/payroll/adjustments/group', { group: target.value, kind, ...fields });
    say(status, groupResultText(payload, label));
    await refreshItems();
    return payload;
  }
  const status = document.getElementById('adj-status');
  const say = (target, message, isError = false) => {
    target.textContent = message;
    target.classList.toggle('payroll-preview-error', isError);
  };
  const fail = (target, error) => say(target, error instanceof Error ? error.message : 'Request failed.', true);

  async function refreshItems() {
    const employeeId = employee.value;
    const tbody = document.getElementById('adj-rows');
    if (!employeeId) {
      fillTable(tbody, [], 6, 'Choose an employee to see their earnings and deductions.');
      document.getElementById('adj-allowances').textContent = '';
      return;
    }
    try {
      const payload = await apiJson(`/api/payroll/adjustments?employeeId=${encodeURIComponent(employeeId)}`);
      document.getElementById('adj-allowances').textContent = allowanceText(payload, employeeId);
      fillTable(tbody, adjustmentRows(payload, employeeId).map(item => {
        let action = '—';
        if (canEdit()) {
          action = document.createElement('button');
          action.type = 'button';
          action.className = 'button button-quiet workflow-cancel';
          action.textContent = 'Remove';
          action.dataset.adjustmentId = item.id;
          action.setAttribute('aria-label', `Remove ${item.id}`);
        }
        return [...item.cells, action];
      }), 6, 'No earnings or voluntary deductions for this employee.');
    } catch (error) {
      fail(status, error);
    }
  }

  async function refreshBatches() {
    
    
    const account = getAccount();
    if (!canReadCompanyArea(account, 'payroll')) return;
    try {
      fillTable(document.getElementById('disbursement-rows'), batchRows((await apiJson('/api/payroll/disbursements')).batches), 5, 'No pay released yet.');
    } catch (error) {
      fail(document.getElementById('disbursement-status'), error);
    }
  }

  async function refreshCorrections() {
    const account = getAccount();
    
    if (!account || !['PAYROLL', 'HR'].includes(account.role)) return;
    try {
      const { payslips } = await apiJson('/api/payroll/payslips');
      const choices = payslips.filter(item => !item.voided && item.employeeId !== account.employeeId);
      const select = document.getElementById('correction-payslip');
      select.replaceChildren();
      choices.forEach(item => {
        const option = document.createElement('option');
        option.value = item.payslipId;
        option.textContent = `${item.payslipId}${item.released ? ' (released)' : ''}`;
        select.append(option);
      });
      const { corrections } = await apiJson('/api/payroll/corrections');
      fillTable(document.getElementById('payslip-correction-rows'), corrections.map(request => {
        let action = request.outcome || request.status;
        if (account.role === 'ADMIN' && request.status === 'Pending review' && request.employeeId !== account.employeeId) {
          action = document.createElement('span');
          action.className = 'workflow-row-actions';
          ['approve', 'reject'].forEach(decision => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'button button-quiet workflow-cancel';
            button.textContent = decision === 'approve' ? 'Approve' : 'Reject';
            button.dataset.correctionId = request.id;
            button.dataset.decision = decision;
            button.setAttribute('aria-label', `${button.textContent} ${request.id}`);
            action.append(button);
          });
        }
        return [request.id, `${request.employeeId} — ${request.employeeName}`, correctionSummary(request), request.reason, request.requestedBy, action];
      }), 6, 'No payslip corrections.');
    } catch (error) {
      fail(document.getElementById('payslip-correction-status'), error);
    }
  }

  document.getElementById('payslip-correction-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('payslip-correction-status');
    try {
      const { item } = await apiJson('/api/payroll/corrections', { payslipId: document.getElementById('correction-payslip').value,
        reason: document.getElementById('correction-reason-text').value, targetMonth: document.getElementById('correction-target-month').value,
        targetCutoff: document.getElementById('correction-target-cutoff').value });
      say(target, item.kind === 'VOID'
        ? `Filed ${item.id}: void ${item.payslipId} (not yet released). Waiting for the System Admin.`
        : `Filed ${item.id}: ${item.payslipId} was released, so the difference goes on ${item.targetMonth} ${cutoffLabel(item.targetCutoff)}. Waiting for the System Admin.`);
      await refreshCorrections();
    } catch (error) {
      fail(target, error);
    }
  });

  document.getElementById('payslip-correction-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-correction-id]');
    if (!button) return;
    const target = document.getElementById('payslip-correction-status');
    try {
      const { item } = await apiJson(`/api/payroll/corrections/${encodeURIComponent(button.dataset.correctionId)}/${button.dataset.decision}`, {});
      say(target, `${item.id} ${item.status.toLowerCase()}. ${item.outcome}`);
      await Promise.all([refreshCorrections(), refreshItems(), onPayslipsChanged()]);
    } catch (error) {
      fail(target, error);
    }
  });

  employee.addEventListener('change', refreshItems);

  document.getElementById('adj-earning-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const value = id => document.getElementById(id).value;
      const fields = { month: value('adj-earning-month'), cutoff: value('adj-earning-cutoff'), type: value('adj-earning-type'),
        description: value('adj-earning-description'), amount: value('adj-earning-amount') };
      if (await postGroup('earning', fields)) return;
      const payload = await apiJson('/api/payroll/adjustments/earnings', { employeeId: employee.value, ...fields });
      say(status, `Added ${payload.item.type.toLowerCase()} ${peso(payload.item.amountCents)} to ${payload.item.month} ${cutoffLabel(payload.item.cutoff)}. Recalculate cutoffs to see it.`);
      await refreshItems();
    } catch (error) {
      fail(status, error);
    }
  });

  document.getElementById('adj-deduction-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const value = id => document.getElementById(id).value;
      const fields = { type: value('adj-deduction-type'), description: value('adj-deduction-description'), amountPerCutoff: value('adj-deduction-amount'),
        fromMonth: value('adj-deduction-from'), toMonth: value('adj-deduction-to') };
      if (await postGroup('deduction', fields)) return;
      const payload = await apiJson('/api/payroll/adjustments/deductions', { employeeId: employee.value, ...fields });
      say(status, `Added ${payload.item.type.toLowerCase()} of ${peso(payload.item.amountPerCutoffCents)} per cutoff from ${payload.item.fromMonth}.`);
      await refreshItems();
    } catch (error) {
      fail(status, error);
    }
  });

  document.getElementById('adj-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-adjustment-id]');
    if (!button) return;
    try {
      await apiJson(`/api/payroll/adjustments/${encodeURIComponent(button.dataset.adjustmentId)}/remove`, {});
      say(status, `Removed ${button.dataset.adjustmentId}. Finalized payslips keep what they had.`);
      await refreshItems();
    } catch (error) {
      fail(status, error);
    }
  });

  document.getElementById('disbursement-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('disbursement-status');
    try {
      const released = await apiJson('/api/payroll/disbursements', {
        month: document.getElementById('disbursement-month').value, cutoff: document.getElementById('disbursement-cutoff').value });
      const { batch, emailsSent, emailsFailed } = released;
      say(target, `Released ${batch.id}: ${batch.lines.length} payslip(s), ${peso(batch.totalCents)} net. Emailed ${emailsSent}${emailsFailed ? `, ${emailsFailed} failed (see Payslip emails)` : ''}.`);
      await Promise.all([refreshBatches(), onPayslipsChanged()]);
    } catch (error) {
      fail(target, error);
    }
  });

  document.getElementById('remittance-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('remittance-status');
    try {
      const month = document.getElementById('remittance-month').value;
      const payload = await apiJson(`/api/payroll/remittance?month=${encodeURIComponent(month)}`);
      fillTable(document.getElementById('remittance-rows'), remittanceRows(payload), 4, 'Nothing to remit.');
      say(target, `${month}: from ${payload.employees.reduce((sum, row) => sum + row.payslipCount, 0)} finalized payslip(s) for ${payload.employees.length} employee(s). Unfinalized cutoffs are not included.`);
    } catch (error) {
      fail(target, error);
    }
  });

  document.getElementById('summary-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('summary-status');
    try {
      const month = document.getElementById('summary-month').value;
      const by = document.getElementById('summary-by').value;
      const payload = await apiJson(`/api/payroll/summary?month=${encodeURIComponent(month)}&by=${encodeURIComponent(by)}`);
      fillTable(document.getElementById('summary-rows'), summaryRows(payload), 7, 'No finalized payslips this month.');
      say(target, `${month} by ${by}: ${payload.total.payslips} finalized payslip(s) for ${payload.total.employees} employee(s).`);
    } catch (error) {
      fail(target, error);
    }
  });

  return { refreshTargets, refresh: () => { refreshTargets(); return Promise.all([refreshItems(), refreshBatches(), refreshCorrections()]); } };
}
