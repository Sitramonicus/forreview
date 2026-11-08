/**
 * Local-demo role layouts. Mirrors AccessPolicy.java so the browser only offers what the server will allow;
 * the server still enforces every rule (this file only hides and filters).
 * account = { employeeId, name, role, roleLabel, team: [ids], reports: [ids] } from GET /api/demo/accounts.
 */
const ALL_VIEWS = ['overview', 'employees', 'attendance', 'requests', 'team', 'hr', 'reports', 'payroll', 'config'];
// No chosen account (legacy calls) sees every view but System Admin configuration, which is local-demo only.
const FIREBASE_VIEWS = ALL_VIEWS.filter(view => view !== 'config');

export const ROLE_VIEWS = {
  EMPLOYEE: ['overview', 'attendance', 'requests', 'team', 'payroll'],
  SUPERVISOR: ['overview', 'attendance', 'requests', 'team', 'payroll'],
  PAYROLL: ['overview', 'employees', 'attendance', 'requests', 'team', 'reports', 'payroll', 'config'],
  HR: ['overview', 'employees', 'attendance', 'requests', 'team', 'hr', 'reports', 'payroll'],
  ADMIN: ALL_VIEWS
};

const NAV_LABELS = {
  EMPLOYEE: { attendance: 'My attendance', requests: 'My requests', payroll: 'My pay' },
  SUPERVISOR: { attendance: 'Team attendance', requests: 'Requests & approvals', payroll: 'Pay & payslips' },
  PAYROLL: { payroll: 'Payroll processing', config: 'Tax tables & rates' },
  HR: { requests: 'Requests & approvals' }
};

export function viewsFor(account) {
  return account ? ROLE_VIEWS[account.role] || ROLE_VIEWS.EMPLOYEE : FIREBASE_VIEWS;
}

/** Design doc: "redirect to the branch dashboard": every role lands on Overview, which shows the branch. Firebase mode keeps the directory. */
export function defaultView(account) {
  return account ? 'overview' : 'employees';
}

const inTeam = (account, id) => account.employeeId === id || (account.team || []).includes(id);
const isReport = (account, id) => (account.reports || []).includes(id);

/** Which people a picker may offer, by purpose (same scopes as the server). */
export function canPickFor(account, purpose, employeeId) {
  if (!account) return true;
  const { role } = account;
  if (role === 'ADMIN') return true;
  switch (purpose) {
    case 'leave':
    case 'ticket': return role === 'HR' || account.employeeId === employeeId;
    case 'overtime': return account.employeeId === employeeId;
    case 'payroll': return role === 'PAYROLL' || role === 'HR' || (role === 'SUPERVISOR' ? inTeam(account, employeeId) : account.employeeId === employeeId);
    case 'attendance-view': return role === 'PAYROLL' || role === 'HR' || (role === 'SUPERVISOR' ? inTeam(account, employeeId) : account.employeeId === employeeId);
    case 'attendance-edit': return role === 'SUPERVISOR' && isReport(account, employeeId);
    case 'schedule':
    case 'paychange': return role === 'SUPERVISOR' && isReport(account, employeeId);
    case 'government': return (role === 'HR' || role === 'PAYROLL') && account.employeeId !== employeeId;
    case 'processing': return role === 'HR' || (role === 'PAYROLL' && account.employeeId !== employeeId) || (role === 'SUPERVISOR' && isReport(account, employeeId));
    default: return false;
  }
}

/** Nobody decides their own request; leave by supervisor/HR/admin, overtime by supervisor/admin. */
export function canReview(account, area, ownerId) {
  if (!account || account.employeeId === ownerId) return false;
  if (account.role === 'ADMIN') return true;
  if (account.role === 'SUPERVISOR' && isReport(account, ownerId)) return true;
  return area === 'leave' && account.role === 'HR';
}

export function canCancel(account, area, ownerId) {
  if (!account) return true;
  return account.employeeId === ownerId || account.role === 'ADMIN' || (account.role === 'HR' && area !== 'overtime');
}

/** Same routing as AccessPolicy.canHandleTicket: Team Lead = the owner's supervisor, Payroll Staff, HR, IT Admin = System Admin. */
export function canHandleTicket(account, ticket) {
  if (!account || account.employeeId === ticket.employeeId) return false;
  if (account.role === 'ADMIN') return true;
  if (ticket.queue === 'Team Lead') return account.role === 'SUPERVISOR' && isReport(account, ticket.employeeId);
  if (ticket.queue === 'Payroll Staff') return account.role === 'PAYROLL';
  if (ticket.queue === 'HR') return account.role === 'HR';
  return false;
}

export function canFinalize(account, ownerId) {
  return !account || ((account.role === 'ADMIN' || account.role === 'PAYROLL') && account.employeeId !== ownerId);
}

/** Hides menu items and [data-roles] sections the account's role does not use, and renames menu items. */
export function applyRoleLayout(root, account) {
  const views = viewsFor(account);
  root.querySelectorAll('.nav-item').forEach(item => {
    item.classList.toggle('role-hidden', !views.includes(item.dataset.view));
    const label = item.querySelector('span:not(.nav-index):not(.nav-mark)');
    if (!label) return;
    if (!label.dataset.defaultLabel) label.dataset.defaultLabel = label.textContent;
    label.textContent = (account && NAV_LABELS[account.role] && NAV_LABELS[account.role][item.dataset.view]) || label.dataset.defaultLabel;
  });
  root.querySelectorAll('.tab-card').forEach(card => {
    card.classList.toggle('role-hidden', !views.includes(card.dataset.openView));
    const title = card.querySelector('.tab-card-title');
    title.textContent = (account && NAV_LABELS[account.role] && NAV_LABELS[account.role][card.dataset.openView]) || title.dataset.defaultLabel;
  });
  root.querySelectorAll('[data-roles]').forEach(element => {
    const roles = element.dataset.roles.split(/\s+/);
    element.classList.toggle('role-hidden', Boolean(account) && !roles.includes(account.role));
  });
}

/** Builds the signed-in account from the accounts list (adds direct reports). */
export function accountFrom(accounts, employeeId) {
  const match = accounts.find(item => item.employeeId === employeeId);
  if (!match) return null;
  return { ...match, reports: accounts.filter(item => item.supervisorId === employeeId).map(item => item.employeeId) };
}
