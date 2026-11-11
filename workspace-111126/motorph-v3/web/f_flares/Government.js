

export const GOV_FIELDS = [['sss', 'SSS'], ['philHealth', 'PhilHealth'], ['pagIbig', 'Pag-IBIG MID'], ['tin', 'TIN']];

export function governmentRow(record) {
  return [`${record.employeeId} — ${record.employeeName}`, ...GOV_FIELDS.map(([key]) => record.fields[key] || 'Not on file')];
}

export function requestSummary(request) {
  if (request.kind === 'ACCESS') return `Full view${request.accessDate ? ` for ${request.accessDate}` : ''}`;
  return GOV_FIELDS.filter(([key]) => request.proposed[key]).map(([key, label]) => `${label} → ${request.proposed[key]}`).join('; ');
}

function cells(row, values) {
  values.forEach(value => {
    const cell = document.createElement('td');
    if (value instanceof Node) cell.append(value);
    else cell.textContent = value;
    row.append(cell);
  });
}

function button(label, data, aria) {
  const element = document.createElement('button');
  element.type = 'button';
  element.className = 'button button-quiet workflow-cancel';
  element.textContent = label;
  Object.assign(element.dataset, data);
  element.setAttribute('aria-label', aria);
  return element;
}

function say(target, message, isError = false) {
  target.textContent = message;
  target.classList.toggle('payroll-preview-error', isError);
}

export function initGovernment(apiJson, getAccount) {
  const panel = document.getElementById('government-panel');
  if (!panel) return { refresh() {} };
  const status = document.getElementById('gov-status');
  const value = id => document.getElementById(id).value.trim();
  const isAdmin = () => getAccount() && getAccount().role === 'ADMIN';

  async function refresh() {
    const account = getAccount();
    
    
    if (!account || !['HR', 'PAYROLL', 'CFO'].includes(account.role)) return;
    document.getElementById('gov-save').textContent = isAdmin() ? 'Save now' : 'Send for approval';
    try {
      const { records } = await apiJson('/api/government');
      const tbody = document.getElementById('gov-rows');
      tbody.replaceChildren();
      records.forEach(record => {
        const row = document.createElement('tr');
        row.dataset.employeeId = record.employeeId;
        cells(row, [...governmentRow(record), button('Show full', { reveal: record.employeeId }, `Show full numbers for ${record.employeeName}`)]);
        tbody.append(row);
      });
      const { requests } = await apiJson('/api/government/requests');
      const requestRows = document.getElementById('gov-request-rows');
      requestRows.replaceChildren();
      requests.forEach(request => {
        const row = document.createElement('tr');
        let action = request.reviewerId ? `${request.status} by ${request.reviewerId}` : request.status;
        if (isAdmin() && request.status === 'Pending review' && request.employeeId !== account.employeeId) {
          action = document.createElement('span');
          action.className = 'workflow-row-actions';
          action.append(button('Approve', { govRequest: request.id, decision: 'approve' }, `Approve ${request.id}`),
            button('Reject', { govRequest: request.id, decision: 'reject' }, `Reject ${request.id}`));
        }
        cells(row, [request.id, `${request.employeeId} — ${request.employeeName}`, requestSummary(request), request.reason || '—', request.requestedBy, action]);
        requestRows.append(row);
      });
      if (isAdmin()) {
        const { audit } = await apiJson('/api/government/audit');
        const auditRows = document.getElementById('gov-audit-rows');
        auditRows.replaceChildren();
        audit.slice(0, 30).forEach(entry => {
          const row = document.createElement('tr');
          cells(row, [entry.at, entry.actorId, entry.employeeId, entry.action, entry.detail]);
          auditRows.append(row);
        });
      }
    } catch (error) {
      say(status, error.message, true);
    }
  }

  panel.addEventListener('click', async event => {
    const reveal = event.target.closest('button[data-reveal]');
    const decide = event.target.closest('button[data-gov-request]');
    try {
      if (reveal) {
        const { record } = await apiJson(`/api/government/${encodeURIComponent(reveal.dataset.reveal)}?reveal=true`);
        const row = reveal.closest('tr');
        row.querySelectorAll('td').forEach((cell, index) => {
          if (index >= 1 && index <= 4) cell.textContent = record.fields[GOV_FIELDS[index - 1][0]] || 'Not on file';
        });
        say(status, `Showing full numbers for ${record.employeeName}. This view was logged.`);
      }
      if (decide) {
        const { item } = await apiJson(`/api/government/requests/${encodeURIComponent(decide.dataset.govRequest)}/${decide.dataset.decision}`, {});
        say(status, `${item.id} ${item.status.toLowerCase()}.`);
        await refresh();
      }
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('gov-change-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const employeeId = value('gov-employee');
      const body = {};
      GOV_FIELDS.forEach(([key]) => { if (value(`gov-${key}`)) body[key] = value(`gov-${key}`); });
      if (isAdmin()) {
        await apiJson(`/api/government/${encodeURIComponent(employeeId)}`, body, 'PUT');
        say(status, `Saved ${employeeId}'s numbers. The edit was logged.`);
      } else {
        const { item } = await apiJson(`/api/government/${encodeURIComponent(employeeId)}/change-requests`, { ...body, reason: value('gov-reason') });
        say(status, `Sent ${item.id} to the System Admin for approval.`);
      }
      event.target.reset();
      await refresh();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('gov-access-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const { item } = await apiJson(`/api/government/${encodeURIComponent(value('gov-access-employee'))}/access-requests`, { reason: value('gov-access-reason') });
      say(status, `Asked for full view (${item.id}); the System Admin approves it for today only.`);
      await refresh();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  return { refresh };
}
