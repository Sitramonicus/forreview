/**
 * Adjustment forms (design doc, Payroll Supervision): Add / Subtract / Replace pay components for one cutoff, for one
 * employee or a group, Pending until another Payroll Staff member or the System Admin approves. Java does the math.
 */
import { formatPesoCents } from './payroll-preview-2026.js';

const peso = cents => formatPesoCents(BigInt(cents));
export const COMPONENT_OPERATIONS = {
  'Rice subsidy': ['Add', 'Subtract', 'Replace'],
  'Phone allowance': ['Add', 'Subtract', 'Replace'],
  'Clothing allowance': ['Add', 'Subtract', 'Replace'],
  'Taxable earning': ['Add', 'Subtract'],
  Deduction: ['Add']
};

export function scopeLabel(scope, names = {}) {
  const [kind, value] = String(scope).split(/:(.*)/s);
  if (kind === 'ALL') return 'Everyone';
  if (kind === 'EMPLOYEE') return `${value} — ${names[value] || ''}`.trim();
  if (kind === 'TEAM') return `Team of ${names[value] || value}`;
  return `${kind === 'BRANCH' ? 'Branch' : 'Department'}: ${value}`;
}

export function lineText(line) {
  const amount = peso(line.amountCents);
  if (line.operation === 'Replace') return `${line.component}: set to ${amount}`;
  return `${line.component}: ${line.operation === 'Add' ? '+' : '−'}${amount}`;
}

export function resultText(form) {
  if (form.status === 'Pending review') return 'Pending review';
  if (form.status === 'Rejected') return `Rejected by ${form.decidedBy}`;
  const changed = form.results.filter(item => !item.skipped)
    .map(item => `${item.employeeId}: net ${peso(item.netBefore)} → ${peso(item.netAfter)}`);
  const skipped = form.results.filter(item => item.skipped).map(item => `${item.employeeId} skipped (${item.skipped})`);
  return [`Approved by ${form.decidedBy}`, ...changed, ...skipped].join('\n');
}

export function initAdjustmentForms(apiJson, { getAccount, getNames, getScopes }) {
  const formEl = document.getElementById('adjform-form');
  if (!formEl) return { refresh() {} };
  const status = document.getElementById('adjform-status');
  const tbody = document.getElementById('adjform-rows');
  const say = (message, isError = false) => {
    status.textContent = message;
    status.classList.toggle('payroll-preview-error', isError);
  };

  function syncOperations(row) {
    const component = row.querySelector('.adjform-component').value;
    const select = row.querySelector('.adjform-operation');
    const current = select.value;
    select.replaceChildren(...(COMPONENT_OPERATIONS[component] || []).map(op => new Option(op, op)));
    if ([...select.options].some(option => option.value === current)) select.value = current;
  }
  formEl.querySelectorAll('.adjform-line').forEach(row => {
    row.querySelector('.adjform-component').addEventListener('change', () => syncOperations(row));
    syncOperations(row);
  });

  async function refresh() {
    const account = getAccount();
    if (!account || !['PAYROLL', 'ADMIN', 'HR'].includes(account.role)) return;
    const scope = document.getElementById('adjform-scope');
    const keep = scope.value;
    scope.replaceChildren(...getScopes().map(([value, label]) => new Option(label, value)));
    if ([...scope.options].some(option => option.value === keep)) scope.value = keep;
    try {
      const { forms } = await apiJson('/api/payroll/adjustment-forms');
      tbody.replaceChildren();
      if (!forms.length) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.className = 'table-message';
        cell.textContent = 'No adjustment forms yet.';
        row.append(cell);
        tbody.append(row);
      }
      forms.forEach(form => {
        const row = document.createElement('tr');
        [form.id, scopeLabel(form.scope, getNames()), `${form.month} · ${form.cutoff === 1 ? '1–15' : '16–end'}`,
          form.lines.map(lineText).join('\n'), form.reason].forEach(value => {
          const cell = document.createElement('td');
          cell.textContent = value;
          cell.style.whiteSpace = 'pre-line';
          row.append(cell);
        });
        const last = document.createElement('td');
        last.style.whiteSpace = 'pre-line';
        if (form.status === 'Pending review' && ['PAYROLL', 'ADMIN'].includes(account.role) && form.submittedBy !== account.employeeId) {
          const actions = document.createElement('span');
          actions.className = 'workflow-row-actions';
          ['approve', 'reject'].forEach(decision => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'button button-quiet workflow-cancel';
            button.textContent = decision === 'approve' ? 'Approve' : 'Reject';
            button.dataset.formId = form.id;
            button.dataset.decision = decision;
            button.setAttribute('aria-label', `${button.textContent} ${form.id}`);
            actions.append(button);
          });
          last.append(actions);
        } else {
          last.textContent = resultText(form);
        }
        row.append(last);
        tbody.append(row);
      });
    } catch (error) {
      say(error.message, true);
    }
  }

  formEl.addEventListener('submit', async event => {
    event.preventDefault();
    const lines = [...formEl.querySelectorAll('.adjform-line')].map(row => ({
      component: row.querySelector('.adjform-component').value,
      operation: row.querySelector('.adjform-operation').value,
      amount: row.querySelector('.adjform-amount').value.trim()
    })).filter(line => line.component && line.amount !== '');
    try {
      const { item } = await apiJson('/api/payroll/adjustment-forms', {
        scope: document.getElementById('adjform-scope').value, month: document.getElementById('adjform-month').value,
        cutoff: document.getElementById('adjform-cutoff').value, reason: document.getElementById('adjform-reason').value, lines });
      say(`Filed ${item.id}; it waits for approval by another Payroll Staff member or the System Admin.`);
      await refresh();
    } catch (error) {
      say(error.message, true);
    }
  });

  tbody.addEventListener('click', async event => {
    const button = event.target.closest('button[data-form-id]');
    if (!button) return;
    try {
      const { item } = await apiJson(`/api/payroll/adjustment-forms/${encodeURIComponent(button.dataset.formId)}/${button.dataset.decision}`, {});
      say(`${item.id} ${item.status.toLowerCase()}. ${item.outcome}`);
      await refresh();
    } catch (error) {
      say(error.message, true);
    }
  });

  return { refresh };
}
