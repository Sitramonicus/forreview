import { formatPesoCents } from './PayrollPreview2026.js';

const DAYS = ['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'];
const DAY_NAMES = { MON: 'Mon', TUE: 'Tue', WED: 'Wed', THU: 'Thu', FRI: 'Fri', SAT: 'Sat', SUN: 'Sun' };

export function workdaysText(workdays) {
  const order = DAYS.filter(day => workdays.includes(day));
  const indexes = order.map(day => DAYS.indexOf(day));
  const consecutive = indexes.every((value, i) => i === 0 || value === indexes[i - 1] + 1);
  if (order.length > 2 && consecutive) return `${DAY_NAMES[order[0]]}–${DAY_NAMES[order[order.length - 1]]}`;
  return order.map(day => DAY_NAMES[day]).join(', ');
}

export function scheduleRows(schedules, names = {}) {
  return schedules.map(item => [`${item.employeeId} — ${names[item.employeeId] || ''}`.trim(), `${item.shiftStart}–${item.shiftEnd}`,
    `${item.mealStart} (1 h)`, workdaysText(item.workdays), item.updatedBy ? `${item.updatedBy} · ${item.updatedAt}` : 'Default']);
}

export function payChangeSummary(request) {
  switch (request.type) {
    case 'PROMOTION': return `Promotion to ${request.newPosition}; basic ₱${request.newMonthlyBasic} from ${request.effectiveMonth}`;
    case 'MERIT': return `Merit increase; basic ₱${request.newMonthlyBasic} from ${request.effectiveMonth}`;
    case 'BONUS': return `Bonus ${formatPesoCents(BigInt(request.bonusCents))} on ${request.bonusMonth} cutoff ${request.bonusCutoff === 1 ? '1–15' : '16–end'}`;
    default: return `Disciplinary unpaid leave ${request.startDate} to ${request.endDate}`;
  }
}

export function canReviewPayChange(account, request) {
  if (!account || request.status !== 'Pending review') return false;
  if (account.employeeId === request.employeeId || account.employeeId === request.requestedBy) return false;
  if (account.role === 'ADMIN') return true;
  return request.type === 'BONUS' ? account.role === 'PAYROLL' : account.role === 'HR';
}

function table(tbody, rows, columns, empty) {
  tbody.replaceChildren();
  if (!rows.length) {
    const row = document.createElement('tr');
    const cell = document.createElement('td');
    cell.colSpan = columns;
    cell.className = 'table-message';
    cell.textContent = empty;
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

function say(target, message, isError = false) {
  target.textContent = message;
  target.classList.toggle('payroll-preview-error', isError);
}

export function dayChangeRows(schedules, names = {}) {
  return schedules.flatMap(item => (item.dayChanges || []).map(change => ({
    id: change.id,
    employeeId: change.employeeId,
    cells: [`${change.employeeId} — ${names[change.employeeId] || ''}`.trim(), change.from === change.to ? change.from : `${change.from} to ${change.to}`,
      change.shiftStart ? `Shift from ${change.shiftStart}` : 'Day off', change.reason]
  })));
}

export function initSupervisorTools(apiJson, { getAccount, getNames }) {
  if (!document.getElementById('schedule-form')) return { refresh() {} };
  const value = id => document.getElementById(id).value;

  async function refreshSchedules() {
    try {
      const { schedules } = await apiJson('/api/schedules');
      table(document.getElementById('schedule-rows'), scheduleRows(schedules, getNames()), 5, 'No schedules visible.');
      const account = getAccount();
      const canChange = account && ['SUPERVISOR', 'ADMIN'].includes(account.role);
      table(document.getElementById('daychange-rows'), dayChangeRows(schedules, getNames()).map(item => {
        if (!canChange || item.employeeId === account.employeeId) return [...item.cells, '—'];
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'button button-quiet workflow-cancel';
        button.textContent = 'Remove';
        button.dataset.changeId = item.id;
        button.setAttribute('aria-label', `Remove ${item.id}`);
        return [...item.cells, button];
      }), 5, 'No single-day or multi-day changes.');
      const chosen = schedules.find(item => item.employeeId === value('schedule-employee'));
      if (chosen) {
        document.getElementById('schedule-start').value = chosen.shiftStart;
        DAYS.forEach(day => { document.getElementById(`schedule-day-${day}`).checked = chosen.workdays.includes(day); });
      }
    } catch (error) {
      say(document.getElementById('schedule-status'), error.message, true);
    }
  }

  async function refreshCorrections() {
    const employeeId = value('correction-employee');
    const rowsSelect = document.getElementById('correction-row');
    rowsSelect.replaceChildren();
    try {
      if (employeeId) {
        const payload = await apiJson(`/api/attendance?employeeId=${encodeURIComponent(employeeId)}`);
        (payload.records || payload.items || []).forEach(record => {
          const option = document.createElement('option');
          option.value = String(record.attendanceId);
          option.textContent = `${record.date} · ${record.timeIn}–${record.timeOut} (#${record.attendanceId})`;
          rowsSelect.append(option);
        });
      }
      const { corrections } = await apiJson('/api/attendance/corrections');
      table(document.getElementById('correction-rows'), corrections.map(item => [`${item.employeeId} · ${item.date}`,
        `${item.originalTimeIn}–${item.originalTimeOut}`, `${item.timeIn}–${item.timeOut}`, item.reason, `${item.editedBy} · ${item.editedAt}`]), 5, 'No timesheet corrections yet.');
    } catch (error) {
      say(document.getElementById('correction-status'), error.message, true);
    }
  }

  async function refreshPayChanges() {
    try {
      const account = getAccount();
      const { requests } = await apiJson('/api/pay-changes');
      table(document.getElementById('paychange-rows'), requests.map(request => {
        let action = request.outcome || '—';
        if (canReviewPayChange(account, request)) {
          action = document.createElement('span');
          action.className = 'workflow-row-actions';
          ['approve', 'reject'].forEach(decision => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'button button-quiet workflow-cancel';
            button.textContent = decision === 'approve' ? 'Approve' : 'Reject';
            button.dataset.paychangeId = request.id;
            button.dataset.decision = decision;
            button.setAttribute('aria-label', `${button.textContent} ${request.id}`);
            action.append(button);
          });
        }
        return [request.id, `${request.employeeId} — ${request.employeeName}`, payChangeSummary(request), request.note || '—',
          `${request.status}${request.reviewerId ? ` by ${request.reviewerId}` : ` (${request.approverRole === 'PAYROLL' ? 'Payroll Staff' : 'HR'})`}`, action];
      }), 6, 'No pay-change requests.');
    } catch (error) {
      say(document.getElementById('paychange-status'), error.message, true);
    }
  }

  document.getElementById('schedule-employee').addEventListener('change', refreshSchedules);
  document.getElementById('schedule-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('schedule-status');
    try {
      const workdays = DAYS.filter(day => document.getElementById(`schedule-day-${day}`).checked);
      const response = await fetchPut(apiJson, `/api/schedules/${encodeURIComponent(value('schedule-employee'))}`, { shiftStart: value('schedule-start'), workdays });
      say(status, `Saved: ${response.schedule.shiftStart}–${response.schedule.shiftEnd}, ${workdaysText(response.schedule.workdays)}. Pay uses it from now on (finalized payslips keep theirs).`);
      await refreshSchedules();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('correction-employee').addEventListener('change', refreshCorrections);
  document.getElementById('correction-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('correction-status');
    try {
      const { correction } = await apiJson('/api/attendance/corrections', { employeeId: value('correction-employee'),
        attendanceId: Number(value('correction-row')), timeIn: value('correction-in'), timeOut: value('correction-out'), reason: value('correction-reason') });
      say(status, `Corrected ${correction.date}: ${correction.originalTimeIn}–${correction.originalTimeOut} → ${correction.timeIn}–${correction.timeOut}.`);
      await refreshCorrections();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  const typeSelect = document.getElementById('paychange-type');
  const showFields = () => document.querySelectorAll('[data-paychange-for]').forEach(field => {
    field.hidden = !field.dataset.paychangeFor.split(' ').includes(typeSelect.value);
  });
  typeSelect.addEventListener('change', showFields);
  showFields();
  document.getElementById('paychange-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('paychange-status');
    try {
      const body = { employeeId: value('paychange-employee'), type: typeSelect.value, note: value('paychange-note') };
      if (['PROMOTION', 'MERIT'].includes(body.type)) Object.assign(body, { newMonthlyBasic: value('paychange-basic'), effectiveMonth: value('paychange-effective') });
      if (body.type === 'PROMOTION') body.newPosition = value('paychange-position');
      if (body.type === 'BONUS') Object.assign(body, { amount: value('paychange-amount'), month: value('paychange-month'), cutoff: value('paychange-cutoff') });
      if (body.type === 'DISCIPLINARY') Object.assign(body, { startDate: value('paychange-start'), endDate: value('paychange-end') });
      const { item } = await apiJson('/api/pay-changes', body);
      say(status, `Filed ${item.id}; waiting for ${item.approverRole === 'PAYROLL' ? 'Payroll Staff' : 'HR'}.`);
      await refreshPayChanges();
    } catch (error) {
      say(status, error.message, true);
    }
  });
  document.getElementById('paychange-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-paychange-id]');
    if (!button) return;
    const status = document.getElementById('paychange-status');
    try {
      const { item } = await apiJson(`/api/pay-changes/${encodeURIComponent(button.dataset.paychangeId)}/${button.dataset.decision}`, {});
      say(status, `${item.id} ${item.status.toLowerCase()}. ${item.outcome}`);
      await refreshPayChanges();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('daychange-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('daychange-status');
    try {
      const off = document.getElementById('daychange-off').checked;
      const { item } = await apiJson(`/api/schedules/${encodeURIComponent(value('daychange-employee'))}/days`, {
        from: value('daychange-from'), to: value('daychange-to'), ...(off ? { dayOff: true } : { shiftStart: value('daychange-start') }), reason: value('daychange-reason') });
      say(target, `Added ${item.id}: ${item.shiftStart ? `shift from ${item.shiftStart}` : 'day off'} on ${item.from === item.to ? item.from : `${item.from} to ${item.to}`}. The employee was notified.`);
      await refreshSchedules();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  document.getElementById('daychange-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-change-id]');
    if (!button) return;
    const target = document.getElementById('daychange-status');
    try {
      await apiJson(`/api/schedules/days/${encodeURIComponent(button.dataset.changeId)}/remove`, {});
      say(target, `Removed ${button.dataset.changeId}; the weekly pattern applies again on those dates.`);
      await refreshSchedules();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  return { refresh: () => Promise.all([refreshSchedules(), refreshCorrections(), refreshPayChanges()]) };
}

async function fetchPut(apiJson, url, body) {
  return apiJson(url, body, 'PUT');
}
