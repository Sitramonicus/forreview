/** System Admin configuration: roles and reporting lines; statutory rates and tax table versions. */

export const ROLE_OPTIONS = [['EMPLOYEE', 'Employee'], ['SUPERVISOR', 'Supervisor'], ['PAYROLL', 'Payroll Staff'], ['HR', 'HR'], ['ADMIN', 'System Admin']];

export function rateSummary(version) {
  return `SSS ${version.sss.employeePercent}% / ${version.sss.employerPercent}% (MSC ₱${version.sss.minMsc}–₱${version.sss.maxMsc}) · `
    + `PhilHealth ${version.philHealth.premiumPercent}% (₱${version.philHealth.floor}–₱${version.philHealth.ceiling}) · `
    + `Pag-IBIG ${version.pagIbig.lowPercent}% ≤ ₱${version.pagIbig.lowBand}, else ${version.pagIbig.highPercent}% (cap ₱${version.pagIbig.cap}) · `
    + `${version.bir.length} tax brackets, top ${version.bir[version.bir.length - 1].ratePercent}%`;
}

/** Tax rows from the form; rows left completely blank are dropped. */
export function birRows(rows) {
  return rows.filter(row => row.over !== '' || row.baseTax !== '' || row.ratePercent !== '');
}

const RATE_FIELDS = [
  ['sss', 'employeePercent'], ['sss', 'employerPercent'], ['sss', 'minMsc'], ['sss', 'maxMsc'],
  ['philHealth', 'premiumPercent'], ['philHealth', 'floor'], ['philHealth', 'ceiling'],
  ['pagIbig', 'lowBand'], ['pagIbig', 'lowPercent'], ['pagIbig', 'highPercent'], ['pagIbig', 'employerPercent'], ['pagIbig', 'cap']
];

function fill(select, values) {
  const current = select.value;
  select.replaceChildren();
  values.forEach(([value, label]) => {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    select.append(option);
  });
  if (values.some(([value]) => value === current)) select.value = current;
}

function table(tbody, rows) {
  tbody.replaceChildren();
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

export function initAdminConfig(apiJson, getAccount) {
  if (!document.getElementById('config-view')) return { refresh() {} };
  const value = id => document.getElementById(id).value.trim();
  let accounts = [];

  async function refreshRoles() {
    const payload = await apiJson('/api/admin/roles');
    accounts = payload.accounts;
    const me = getAccount();
    fill(document.getElementById('role-employee'), accounts.filter(item => !me || item.employeeId !== me.employeeId)
      .map(item => [item.employeeId, `${item.employeeId} — ${item.name}`]));
    fill(document.getElementById('role-supervisor'), [['', 'No supervisor'], ...accounts.filter(item => item.role === 'SUPERVISOR')
      .map(item => [item.employeeId, `${item.employeeId} — ${item.name}`])]);
    syncRoleForm();
    table(document.getElementById('role-rows'), accounts.map(item => [`${item.employeeId} — ${item.name}`, item.position, item.roleLabel,
      item.supervisorId || '—', item.status]));
    table(document.getElementById('role-log-rows'), payload.changes.map(change => [change.at, change.by, change.employeeId, change.before, change.after]));
  }

  function syncRoleForm() {
    const chosen = accounts.find(item => item.employeeId === document.getElementById('role-employee').value);
    if (!chosen) return;
    document.getElementById('role-role').value = chosen.role;
    document.getElementById('role-supervisor').value = chosen.supervisorId;
  }

  async function refreshRates() {
    const payload = await apiJson('/api/rate-proposals');
    const me = getAccount();
    document.getElementById('rates-submit').textContent = me && me.role === 'PAYROLL' ? 'Propose for approval' : 'Save as new version';
    table(document.getElementById('proposal-rows'), payload.proposals.map(item => {
      let result = item.status === 'Pending review' ? 'Pending review' : `${item.status}: ${item.outcome}`;
      if (me && me.role === 'ADMIN' && item.status === 'Pending review' && item.proposedBy !== me.employeeId) {
        result = document.createElement('span');
        result.className = 'workflow-row-actions';
        ['approve', 'reject'].forEach(decision => {
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'button button-quiet workflow-cancel';
          button.textContent = decision === 'approve' ? 'Approve' : 'Reject';
          button.dataset.proposalId = item.id;
          button.dataset.decision = decision;
          button.setAttribute('aria-label', `${button.textContent} ${item.id}`);
          result.append(button);
        });
      }
      return [item.id, item.effectiveMonth, item.note || '—', item.proposedBy, result];
    }));
    table(document.getElementById('rates-rows'), payload.versions.map(version => [version.id + (version.id === payload.inForce ? ' (in force now)' : ''),
      version.effectiveMonth, rateSummary(version), version.note, version.createdBy || 'Built in']));
    // Prefill the form with the version in force so the admin edits from the current numbers.
    const current = payload.versions.find(version => version.id === payload.inForce) || payload.versions[0];
    RATE_FIELDS.forEach(([group, key]) => { document.getElementById(`rate-${group}-${key}`).value = current[group][key]; });
    for (let i = 0; i < 8; i++) {
      const row = current.bir[i] || { over: '', baseTax: '', ratePercent: '' };
      document.getElementById(`bir-${i}-over`).value = row.over;
      document.getElementById(`bir-${i}-baseTax`).value = row.baseTax;
      document.getElementById(`bir-${i}-ratePercent`).value = row.ratePercent;
    }
  }

  async function refresh() {
    const account = getAccount();
    if (!account || !['ADMIN', 'PAYROLL'].includes(account.role)) return;
    try {
      await Promise.all([account.role === 'ADMIN' ? refreshRoles() : null, refreshRates()]);
    } catch (error) {
      say(document.getElementById('role-status'), error.message, true);
    }
  }

  document.getElementById('role-employee').addEventListener('change', syncRoleForm);
  document.getElementById('role-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('role-status');
    try {
      const { change } = await apiJson(`/api/admin/roles/${encodeURIComponent(value('role-employee'))}`,
        { role: value('role-role'), supervisorId: value('role-supervisor') });
      say(status, `${change.employeeId}: ${change.before} → ${change.after}. It applies at their next sign-in.`);
      await refreshRoles();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('rates-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('rates-status');
    try {
      const body = { effectiveMonth: value('rates-month'), note: value('rates-note'), sss: {}, philHealth: {}, pagIbig: {} };
      RATE_FIELDS.forEach(([group, key]) => { body[group][key] = value(`rate-${group}-${key}`); });
      body.bir = birRows([...Array(8).keys()].map(i => ({ over: value(`bir-${i}-over`), baseTax: value(`bir-${i}-baseTax`), ratePercent: value(`bir-${i}-ratePercent`) })));
      if ((getAccount() || {}).role === 'PAYROLL') {
        const { item } = await apiJson('/api/rate-proposals', body);
        say(status, `Filed ${item.id} for ${item.effectiveMonth}; it waits for the System Admin's approval.`);
      } else {
        const { version } = await apiJson('/api/admin/rates', body);
        say(status, `Saved ${version.id}, effective ${version.effectiveMonth}. Payslips from that month use it; finalized payslips keep theirs.`);
      }
      await refreshRates();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('proposal-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-proposal-id]');
    if (!button) return;
    const status = document.getElementById('rates-status');
    try {
      const { item } = await apiJson(`/api/rate-proposals/${encodeURIComponent(button.dataset.proposalId)}/${button.dataset.decision}`, {});
      say(status, `${item.id} ${item.status.toLowerCase()}. ${item.outcome}`);
      await refreshRates();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('reset-form').addEventListener('submit', async event => {
    event.preventDefault();
    const status = document.getElementById('reset-status');
    try {
      await apiJson('/api/admin/reset', { confirm: value('reset-confirm') });
      say(status, 'Demo data reset. Reloading…');
      sessionStorage.clear();
      window.location.reload();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  async function refreshPersistence() {
    const health = await apiJson('/api/health');
    document.getElementById('persistence-label').textContent = health.persistence === 'file' ? 'SAVED TO FILE' : 'MEMORY ONLY';
  }

  const refreshAll = async () => {
    await refresh();
    const account = getAccount();
    if (account && account.role === 'ADMIN') await refreshPersistence().catch(() => {});
  };

  return { refresh: refreshAll };
}
