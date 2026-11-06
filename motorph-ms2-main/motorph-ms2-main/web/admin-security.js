/**
 * System Admin: security settings (password rules, lockout, 2FA code life, session length) with per-account unlock
 * and password reset, payroll policy settings, the audit log, and backups with restore. Java enforces every rule.
 */
export function auditRows(entries) {
  return entries.map(entry => [entry.at.replace('T', ' '), entry.actor, `${entry.method} ${entry.path}`, entry.target || '—', String(entry.status)]);
}

export function accountSecurityText(account) {
  if (account.lockedUntil) return `Locked until ${account.lockedUntil.slice(11, 16)}`;
  const parts = [account.defaultPassword ? 'Demo password' : `Own password since ${account.passwordChangedAt.slice(0, 10)}`];
  if (account.failedAttempts) parts.push(`${account.failedAttempts} wrong recently`);
  return parts.join(' · ');
}

function fillRows(tbody, rows, columns, emptyText) {
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

function actionButton(label, data) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'button button-quiet workflow-cancel';
  button.textContent = label;
  Object.assign(button.dataset, data);
  button.setAttribute('aria-label', `${label} ${data.employeeId || data.backup || ''}`.trim());
  return button;
}

export function initAdminSecurity(apiJson, getAccount) {
  if (!document.getElementById('security-form')) return { refresh() {} };
  const field = id => document.getElementById(id);
  const say = (id, message, isError = false) => {
    field(id).textContent = message;
    field(id).classList.toggle('payroll-preview-error', isError);
  };
  const isAdmin = () => (getAccount() || {}).role === 'ADMIN';

  async function refreshSecurity() {
    const { settings, accounts } = await apiJson('/api/admin/security');
    ['minPasswordLength', 'maxFailedAttempts', 'lockoutMinutes', 'codeMinutes', 'sessionHours'].forEach(key => { field(`security-${key}`).value = settings[key]; });
    const me = (getAccount() || {}).employeeId;
    fillRows(field('security-rows'), accounts.map(account => {
      const actions = document.createElement('span');
      actions.className = 'workflow-row-actions';
      if (account.lockedUntil || account.failedAttempts) actions.append(actionButton('Unlock', { employeeId: account.employeeId, action: 'unlock' }));
      if (!account.defaultPassword && account.employeeId !== me) actions.append(actionButton('Reset password', { employeeId: account.employeeId, action: 'reset-password' }));
      return [`${account.employeeId} — ${account.name}`, accountSecurityText(account), actions.childElementCount ? actions : '—'];
    }), 3, 'No accounts.');
  }

  async function refreshPolicy() {
    const { policy, mentorDefaults } = await apiJson('/api/admin/payroll-policy');
    ['graceMinutes', 'hoursPerMonth', 'dailyHours', 'nightDifferentialPercent'].forEach(key => { field(`policy-${key}`).value = policy[key]; });
    say('policy-status', mentorDefaults ? 'These are the mentor’s rules (10-minute grace, ÷168, 8 paid hours, 10% night differential).'
      : `Changed by ${policy.updatedBy} at ${policy.updatedAt.replace('T', ' ')}; this is no longer the mentor's rules.`);
  }

  async function refreshAudit() {
    const params = new URLSearchParams({ limit: '200' });
    if (field('audit-actor').value.trim()) params.set('actor', field('audit-actor').value.trim());
    if (field('audit-path').value.trim()) params.set('path', field('audit-path').value.trim());
    if (field('audit-failures').checked) params.set('failures', 'true');
    const payload = await apiJson(`/api/admin/audit?${params}`);
    fillRows(field('audit-rows'), auditRows(payload.entries), 5, 'No matching audit entries.');
    say('audit-status', `Showing ${payload.entries.length} of ${payload.kept} kept entries${payload.archives.length ? `; ${payload.archives.length} older batch(es) archived in data/archive` : ''}.`);
  }

  async function refreshBackups() {
    const payload = await apiJson('/api/admin/backups');
    const select = field('restore-name');
    select.replaceChildren(...payload.backups.map(name => new Option(name, name)));
    fillRows(field('backup-rows'), payload.backups.map(name => [name]), 1,
      payload.persistence === 'file' ? 'No backups yet.' : 'This server keeps data in memory only, so there are no backups.');
  }

  async function refresh() {
    if (!isAdmin()) return;
    try {
      await Promise.all([refreshSecurity(), refreshPolicy(), refreshAudit(), refreshBackups()]);
    } catch (error) {
      say('security-status', error.message, true);
    }
  }

  field('security-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const body = {};
      ['minPasswordLength', 'maxFailedAttempts', 'lockoutMinutes', 'codeMinutes', 'sessionHours'].forEach(key => { body[key] = field(`security-${key}`).value; });
      await apiJson('/api/admin/security', body);
      say('security-status', 'Security settings saved. They apply to the next sign-in.');
      await Promise.all([refreshSecurity(), refreshAudit()]);
    } catch (error) {
      say('security-status', error.message, true);
    }
  });

  field('security-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-employee-id]');
    if (!button) return;
    try {
      await apiJson(`/api/admin/security/${encodeURIComponent(button.dataset.employeeId)}/${button.dataset.action}`, {});
      say('security-status', button.dataset.action === 'unlock' ? `${button.dataset.employeeId} unlocked.`
        : `${button.dataset.employeeId} is back on the demo password and signed out everywhere.`);
      await Promise.all([refreshSecurity(), refreshAudit()]);
    } catch (error) {
      say('security-status', error.message, true);
    }
  });

  field('policy-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const body = {};
      ['graceMinutes', 'hoursPerMonth', 'dailyHours', 'nightDifferentialPercent'].forEach(key => { body[key] = field(`policy-${key}`).value; });
      await apiJson('/api/admin/payroll-policy', body);
      await Promise.all([refreshPolicy(), refreshAudit()]);
    } catch (error) {
      say('policy-status', error.message, true);
    }
  });

  field('policy-defaults').addEventListener('click', () => {
    [['graceMinutes', 10], ['hoursPerMonth', 168], ['dailyHours', 8], ['nightDifferentialPercent', 10]].forEach(([key, value]) => { field(`policy-${key}`).value = value; });
    field('policy-form').requestSubmit();
  });

  field('audit-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      await refreshAudit();
    } catch (error) {
      say('audit-status', error.message, true);
    }
  });

  field('backup-now').addEventListener('click', async () => {
    try {
      const { backup } = await apiJson('/api/admin/backups', {});
      say('backup-status', `Backed up as ${backup}.`);
      await refreshBackups();
    } catch (error) {
      say('backup-status', error.message, true);
    }
  });

  field('restore-form').addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const { restored, safetyBackup } = await apiJson('/api/admin/backups/restore', { name: field('restore-name').value, confirm: field('restore-confirm').value.trim() });
      say('backup-status', `Restored ${restored}. The data from just before is kept as ${safetyBackup}. Reloading…`);
      window.setTimeout(() => window.location.reload(), 900);
    } catch (error) {
      say('backup-status', error.message, true);
    }
  });

  return { refresh };
}
