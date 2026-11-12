
const ALL_VIEWS = ['overview', 'employees', 'attendance', 'requests', 'team', 'hr', 'reports', 'payroll', 'config'];

export const ROLE_WORKSPACE_VIEWS = Object.freeze({
  EMPLOYEE: 'workspace-employee',
  SUPERVISOR: 'workspace-supervisor',
  PAYROLL: 'workspace-payroll',
  HR: 'workspace-hr',
  ADMIN: 'workspace-admin'
});

const EXECUTIVES = ['CEO', 'CFO', 'COO'];
const homeRole = role => (EXECUTIVES.includes(role) ? 'SUPERVISOR' : role);

const ROLE_HOME_VIEWS = new Set(Object.values(ROLE_WORKSPACE_VIEWS));
const UNSIGNED_VIEWS = ALL_VIEWS.filter(view => view !== 'config');

export const ROLE_VIEWS = {
  EMPLOYEE: ['overview', 'attendance', 'requests', 'team', 'payroll'],
  SUPERVISOR: ['overview', 'attendance', 'requests', 'team', 'payroll'],
  PAYROLL: ['overview', 'employees', 'attendance', 'requests', 'team', 'reports', 'payroll', 'config'],
  HR: ['overview', 'employees', 'attendance', 'requests', 'team', 'hr', 'reports', 'payroll'],
  CEO: ['overview', 'employees', 'attendance', 'requests', 'team', 'reports', 'payroll'],
  CFO: ['overview', 'employees', 'attendance', 'requests', 'team', 'reports', 'payroll'],
  COO: ['overview', 'employees', 'attendance', 'requests', 'team', 'reports', 'payroll'],
  ADMIN: ALL_VIEWS
};

const NAV_LABELS = {
  EMPLOYEE: { overview: 'My workspace', attendance: 'My attendance', requests: 'My requests', payroll: 'My pay' },
  SUPERVISOR: { overview: 'Team workspace', attendance: 'Team attendance', requests: 'Requests & approvals', payroll: 'Pay & payslips' },
  PAYROLL: { overview: 'Payroll office', payroll: 'Payroll processing', config: 'Tax tables & rates' },
  HR: { overview: 'People operations', requests: 'Requests & approvals' },
  CEO: { overview: 'Executive desk', team: 'Organisation', payroll: 'Payroll & costs' },
  CFO: { overview: 'Finance desk', team: 'Organisation', payroll: 'Payroll & remittances' },
  COO: { overview: 'Operations desk', team: 'Organisation', attendance: 'Attendance & overtime' },
  ADMIN: { overview: 'System controls' }
};

export function viewsFor(account) {
  return account ? ROLE_VIEWS[account.role] || ROLE_VIEWS.EMPLOYEE : UNSIGNED_VIEWS;
}

export function isRoleWorkspace(view) {
  return ROLE_HOME_VIEWS.has(view);
}

export function navViewFor(view) {
  return isRoleWorkspace(view) ? 'overview' : view;
}

export function defaultView(account) {
  return account ? ROLE_WORKSPACE_VIEWS[homeRole(account.role)] || ROLE_WORKSPACE_VIEWS.EMPLOYEE : 'employees';
}

const inTeam = (account, id) => account.employeeId === id || (account.team || []).includes(id);
const isReport = (account, id) => (account.reports || []).includes(id);

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
    const shown = !account || roles.includes(account.role) || (EXECUTIVES.includes(account.role) && roles.includes('EMPLOYEE'));
    element.classList.toggle('role-hidden', !shown);
  });
}

export function accountFrom(accounts, employeeId) {
  const match = accounts.find(item => item.employeeId === employeeId);
  if (!match) return null;
  // the server gives the CMO the supervisor grants, so the browser treats the account the same way
  const role = match.role === 'CMO' ? 'SUPERVISOR' : match.role;
  return { ...match, role, reports: accounts.filter(item => item.supervisorId === employeeId).map(item => item.employeeId) };
}

const COMPANY_AREA_READERS = {
  employees: ['HR', 'PAYROLL', 'CEO', 'CFO', 'COO', 'ADMIN'],
  attendance: ['HR', 'PAYROLL', 'COO'],
  payroll: ['HR', 'PAYROLL', 'CEO', 'CFO'],
  statutory: ['PAYROLL', 'CEO', 'CFO'],
  remittance: ['PAYROLL', 'CFO'],
  reports: ['HR', 'PAYROLL', 'CEO', 'CFO', 'COO']
};

export function canReadCompanyArea(account, area) {
  if (!account) return false;
  const readers = COMPANY_AREA_READERS[area];
  return Array.isArray(readers) ? readers.includes(account.role) : false;
}

export function isHr(account) {
  return Boolean(account) && account.role === 'HR';
}
