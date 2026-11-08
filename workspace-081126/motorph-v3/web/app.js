import { initializeApp } from 'firebase/app';
import { createUserWithEmailAndPassword, getAuth, inMemoryPersistence, onAuthStateChanged, setPersistence, signInWithEmailAndPassword, signOut, sendPasswordResetEmail } from 'firebase/auth';
import { createWorkflowState, submitLeaveRequest, submitTicket } from './workflow-demo.js';
import { buildCsv } from './csv-export.js';
import { buildLeaveCalendar } from './leave-calendar.js';
import { buildAttendanceCalendar } from './attendance-calendar.js';
import { filterWorkflowItems } from './workflow-filter.js';
import { directoryLabelSummaryRows } from './directory-summary.js';
import { filterAttendanceCoverage } from './attendance-coverage-filter.js';
import { sortDirectoryEntries } from './directory-sort.js';
import { sortAttendanceCoverage } from './attendance-coverage-sort.js';
import { sortAttendanceRecords } from './attendance-record-sort.js';
import { sortWorkflowItems } from './workflow-sort.js';
import { workflowSummaryRows } from './workflow-summary.js';
import { buildWorkspaceExport } from './workspace-export.js';
import { searchSandboxRecords } from './sandbox-search.js';
import { employeeRecordSummary } from './employee-record-summary.js';
import { filterAndSortEmployeeRecordSummary } from './employee-record-summary-view.js';
import { initPayrollPreview } from './payroll-preview-ui.js';
import { initCutoffPayroll } from './payroll-cutoff.js';
import { initOvertime } from './overtime.js';
import { initMyDay } from './my-day.js';
import { initPayrollProcessing } from './payroll-processing.js';
import { initSupervisorTools } from './supervisor-tools.js';
import { initHrTools } from './hr-tools.js';
import { initGovernment } from './government.js';
import { initAdminConfig } from './admin-config.js';
import { metricsRows } from './attendance-metrics.js';
import { availabilityFor, contactFor, leaveEventText } from './team-roster.js';
import { initOutbox } from './payslip-outbox.js';
import { initAdminSecurity } from './admin-security.js';
import { branchRows } from './branches.js';
import { initNotifications } from './notifications.js';
import { initPageTabs } from './page-tabs.js';
import { initAdjustmentForms } from './adjustment-forms.js';
import { groupOptions } from './payroll-processing.js';

const todayKey = () => new Date().toLocaleDateString('en-CA');
import { accountFrom, applyRoleLayout, canCancel, canFinalize, canHandleTicket, canPickFor, canReview, defaultView } from './roles.js';

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY || '',
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID || '',
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || '',
  appId: import.meta.env.VITE_FIREBASE_APP_ID || ''
};
const firebaseConfigured = Boolean(firebaseConfig.apiKey && firebaseConfig.projectId && firebaseConfig.appId
  && !firebaseConfig.apiKey.toLowerCase().includes('replace') && !firebaseConfig.apiKey.toLowerCase().includes('paste_'));
const firebaseApp = firebaseConfigured ? initializeApp(firebaseConfig) : null;
const auth = firebaseApp ? getAuth(firebaseApp) : null;
const ACCOUNT_KEY = 'motorph-demo-account';
const SESSION_KEY = 'motorph-demo-session';
const state = { account: null, accounts: [], employees: [], allEmployees: [], totalEmployees: 0, rawAttendance: [], attendanceReport: [], reportRequestNumber: 0, selected: null, searchTimer: null, requestNumber: 0, attendanceRequestNumber: 0, user: null, workflow: createWorkflowState(), visibleLeaveRequests: [], visibleTickets: [], employeeRecordSummary: [], visibleEmployeeRecordSummary: [] };
let leaveCalendarMonth = new Date();
let leaveDateSelectionStart = '';
let leaveDateSelectionEnd = '';
let attendanceCalendarMonth = null;
let attendanceCalendarEmployee = '';
let attendanceCalendarSelectionStart = '';
let attendanceCalendarSelectionEnd = '';
async function payrollJson(url, body, method = 'POST') {
  const options = body === undefined
    ? { headers: await apiHeaders() }
    : { method, headers: await apiHeaders({ 'Content-Type': 'application/json' }), body: JSON.stringify(body) };
  const response = await fetch(url, options);
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
  return payload;
}
initPayrollPreview(payrollJson);
const cutoffPayroll = initCutoffPayroll(payrollJson, { canFinalize: ownerId => canFinalize(state.account, ownerId) });
const myDay = initMyDay(payrollJson);
const government = initGovernment(payrollJson, () => state.account);
const adminConfig = initAdminConfig(payrollJson, () => state.account);
const hrTools = initHrTools(payrollJson, { fetchBlob: async url => { const response = await fetch(url, { headers: await apiHeaders({ Accept: 'image/*' }) }); if (!response.ok) throw new Error('Photo not found.'); return response.blob(); },
  getAccount: () => state.account, getAccounts: () => state.accounts,
  getEmployees: () => state.allEmployees, getTickets: () => state.workflow.tickets,
  reloadDirectory: () => loadEmployees(), reloadWorkflows: () => loadWorkflows() });
const supervisorTools = initSupervisorTools(payrollJson, { getAccount: () => state.account,
  getNames: () => Object.fromEntries(state.allEmployees.map(employee => [employee.id, employee.name])) });
const payrollProcessing = initPayrollProcessing(payrollJson, () => !state.account || ['PAYROLL', 'ADMIN', 'SUPERVISOR'].includes(state.account.role),
  () => state.account, () => Promise.all([cutoffPayroll.refreshHistory(), outbox.refresh()]), () => ({ employees: state.allEmployees, accounts: state.accounts }));
/** Downloads a file from the API with the signed-in headers (payslip PDFs, payroll report files). */
async function downloadWithSession(url, filename) {
  const response = await fetch(url, { headers: await apiHeaders({ Accept: '*/*' }) });
  if (!response.ok) throw new Error((await response.json().catch(() => ({}))).error || `Download failed (${response.status})`);
  const link = document.createElement('a');
  link.href = URL.createObjectURL(await response.blob());
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(link.href), 1000);
}
const outbox = initOutbox(payrollJson, () => state.account, downloadWithSession);
['csv', 'pdf'].forEach(format => document.getElementById(`summary-${format}`).addEventListener('click', async () => {
  const month = document.getElementById('summary-month').value;
  const by = document.getElementById('summary-by').value;
  try {
    await downloadWithSession(`/api/payroll/summary?month=${encodeURIComponent(month)}&by=${encodeURIComponent(by)}&format=${format}`,
      `motorph-payroll-summary-${month}-${by}.${format}`);
  } catch (error) {
    document.getElementById('summary-status').textContent = error.message;
  }
}));
const adminSecurity = initAdminSecurity(payrollJson, () => state.account);
const notificationsUi = initNotifications(payrollJson, () => state.account);
const adjustmentForms = initAdjustmentForms(payrollJson, {
  getAccount: () => state.account,
  getNames: () => Object.fromEntries(state.allEmployees.map(employee => [employee.id, employee.name])),
  getScopes: () => [...state.allEmployees.filter(employee => employee.status !== 'Separated').map(employee => [`EMPLOYEE:${employee.id}`, `${employee.id} — ${employee.name}`]),
    ...groupOptions(state.allEmployees, state.accounts)]
});
const pageTabs = initPageTabs(document);
const overtime = initOvertime(payrollJson, ownerId => !state.account || canReview(state.account, 'overtime', ownerId));

const authGate = document.getElementById('auth-gate');
const appShell = document.getElementById('app-shell');
const authError = document.getElementById('auth-error');
const authEmail = document.getElementById('auth-email');
const authPassword = document.getElementById('auth-password');
const signInButton = document.getElementById('sign-in-button');
const createAccountButton = document.getElementById('create-account-button');

function setAuthError(message) {
  authError.textContent = message;
  authError.hidden = !message;
}

function friendlyAuthError(error) {
  const code = error && error.code || '';
  if (code === 'auth/operation-not-allowed') return 'Email/Password sign-in is disabled. Enable it in Firebase Console → Authentication → Sign-in method.';
  if (code === 'auth/unauthorized-domain') return 'This preview domain is not authorized. Add its host under Firebase Console → Authentication → Settings → Authorized domains, or test on localhost.';
  if (code === 'auth/invalid-api-key' || code === 'auth/api-key-not-valid.-please-pass-a-valid-api-key.') return 'The Firebase Web API key in .env.local is missing or invalid.';
  if (code === 'auth/email-already-in-use') return 'That email already has a Firebase account. Use Sign in instead.';
  if (code === 'auth/weak-password') return 'Use a password with at least six characters.';
  if (code === 'auth/invalid-email') return 'Enter a valid email address.';
  if (code === 'auth/too-many-requests') return 'Too many attempts. Wait a few minutes and try again.';
  if (code === 'auth/invalid-credential' || code === 'auth/wrong-password' || code === 'auth/user-not-found') return 'Email or password is incorrect.';
  return error && error.message ? error.message : 'Firebase sign-in could not be completed.';
}

async function signIn() {
  setAuthError('');
  document.getElementById('auth-reset-status').textContent = '';
  try {
    await signInWithEmailAndPassword(auth, authEmail.value.trim(), authPassword.value);
  } catch (error) {
    setAuthError(friendlyAuthError(error));
  }
}

async function createDevAccount() {
  setAuthError('');
  document.getElementById('auth-reset-status').textContent = '';
  try {
    await createUserWithEmailAndPassword(auth, authEmail.value.trim(), authPassword.value);
  } catch (error) {
    setAuthError(friendlyAuthError(error));
  }
}

async function sendPasswordReset() {
  setAuthError('');
  const status = document.getElementById('auth-reset-status');
  const button = document.getElementById('password-reset-button');
  status.textContent = '';
  if (!authEmail.value.trim() || !authEmail.checkValidity()) {
    authEmail.reportValidity();
    return;
  }
  button.disabled = true;
  status.textContent = '';
  try {
    await sendPasswordResetEmail(auth, authEmail.value.trim());
    status.textContent = 'If an account exists for that email, Firebase will send a reset link.';
  } catch (error) {
    if (error && ['auth/user-not-found', 'auth/user-disabled'].includes(error.code)) {
      status.textContent = 'If an account exists for that email, Firebase will send a reset link.';
    } else {
      setAuthError(friendlyAuthError(error));
    }
  } finally {
    button.disabled = false;
  }
}

async function apiHeaders(extra = {}) {
  const headers = { Accept: 'application/json', ...extra };
  if (state.account) headers['X-Demo-User'] = state.account.employeeId;
  if (state.session) headers['X-Demo-Session'] = state.session;
  if (state.user) {
    const token = await state.user.getIdToken();
    headers.Authorization = `Bearer ${token}`;
    headers['X-Firebase-UID'] = state.user.uid;
  }
  return headers;
}

function resetEmployeeEditor() {
  const form = document.getElementById('employee-form');
  form.reset();
  document.getElementById('employee-edit-id').value = '';
  document.getElementById('employee-editor-title').textContent = 'Create synthetic record';
  document.getElementById('employee-save-button').textContent = 'Create synthetic record';
  document.getElementById('employee-cancel-edit-button').hidden = true;
  setWorkflowStatus('employee-form-status', '');
}

function resetSessionData() {
  state.requestNumber += 1;
  state.attendanceRequestNumber += 1;
  state.reportRequestNumber += 1;
  window.clearTimeout(state.searchTimer);
  state.searchTimer = null;
  state.employees = [];
  state.allEmployees = [];
  state.totalEmployees = 0;
  state.rawAttendance = [];
  state.attendanceReport = [];
  document.getElementById('attendance-report-from').value = '';
  document.getElementById('attendance-report-to').value = '';
  document.getElementById('attendance-report-search').value = '';
  document.getElementById('attendance-report-mapping').value = 'all';
  document.getElementById('attendance-report-sort').value = 'employeeId';
  document.getElementById('attendance-report-order').value = 'asc';
  document.getElementById('attendance-report-status').hidden = true;
  document.getElementById('attendance-report-status').textContent = '';
  document.getElementById('attendance-report-count').textContent = 'No report loaded';
  document.getElementById('attendance-report-export').disabled = true;
  document.getElementById('directory-summary-export').disabled = true;
  document.getElementById('directory-summary-export-status').textContent = '';
  document.getElementById('workflow-summary-export').disabled = true;
  document.getElementById('workflow-summary-export-status').textContent = '';
  renderRawAttendanceSummary([]);
  state.selected = null;
  state.workflow = createWorkflowState();
  state.visibleLeaveRequests = [];
  state.visibleTickets = [];
  state.employeeRecordSummary = [];
  state.visibleEmployeeRecordSummary = [];
  leaveCalendarMonth = new Date();
  leaveDateSelectionStart = '';
  leaveDateSelectionEnd = '';
  document.getElementById('leave-calendar-selection-status').textContent = 'Choose the first date to draft a request range.';
  search.value = '';
  document.getElementById('employee-sort').value = 'name';
  document.getElementById('employee-order').value = 'asc';
  attendanceFrom.value = '';
  attendanceTo.value = '';
  document.getElementById('attendance-sort').value = 'date';
  document.getElementById('attendance-order').value = 'asc';
  document.getElementById('attendance-entry-form').reset();
  document.getElementById('attendance-entry-status').textContent = '';
  attendanceCalendarMonth = null;
  attendanceCalendarEmployee = '';
  attendanceCalendarSelectionStart = '';
  attendanceCalendarSelectionEnd = '';
  document.getElementById('attendance-calendar-selection-status').textContent = 'Choose a start and end date to filter raw rows.';
  document.getElementById('leave-request-form').reset();
  document.getElementById('ticket-form').reset();
  document.getElementById('leave-filter-search').value = '';
  document.getElementById('leave-filter-status').value = '';
  document.getElementById('leave-filter-type').value = '';
  document.getElementById('leave-filter-from').value = '';
  document.getElementById('leave-filter-to').value = '';
  document.getElementById('leave-sort').value = 'submittedAt';
  document.getElementById('leave-sort-order').value = 'desc';
  document.getElementById('ticket-filter-search').value = '';
  document.getElementById('ticket-filter-status').value = '';
  document.getElementById('ticket-filter-type').value = '';
  document.getElementById('ticket-sort').value = 'submittedAt';
  document.getElementById('ticket-sort-order').value = 'desc';
  document.getElementById('employee-record-summary-search').value = '';
  document.getElementById('employee-record-summary-mapping').value = 'all';
  document.getElementById('employee-record-summary-sort').value = 'employeeName';
  document.getElementById('employee-record-summary-order').value = 'asc';
  document.getElementById('hr-directory-search').value = '';
  document.getElementById('hr-directory-status').value = '';
  document.getElementById('hr-directory-sort').value = 'name';
  document.getElementById('hr-directory-order').value = 'asc';
  document.getElementById('team-directory-search').value = '';
  document.getElementById('team-directory-status').value = '';
  document.getElementById('team-directory-sort').value = 'name';
  document.getElementById('team-directory-order').value = 'asc';
  document.getElementById('team-export-button').disabled = true;
  document.getElementById('team-export-status').textContent = '';
  document.getElementById('hr-directory-export').disabled = true;
  document.getElementById('hr-directory-export-status').textContent = '';
  resetEmployeeEditor();
  seedDemoButton.hidden = true;
  populateAttendanceEmployees();
  populateWorkflowEmployees();
  renderEmployees();
  renderWorkflow();
  renderDirectoryViews();
  showAttendanceMessage('Choose an employee to load synthetic records.');
  attendanceResultCount.textContent = 'No records loaded';
  document.getElementById('attendance-export-button').disabled = true;
  document.getElementById('employee-export-status').textContent = '';
  document.getElementById('attendance-export-status').textContent = '';
  document.getElementById('leave-export-status').textContent = '';
  document.getElementById('ticket-export-status').textContent = '';
  setWorkflowStatus('leave-form-status', '');
  setWorkflowStatus('ticket-form-status', '');
}

function showWorkspace(user) {
  state.user = user;
  resetSessionData();
  authGate.hidden = true;
  appShell.hidden = false;
  document.getElementById('sign-out-button').hidden = !user;
  document.getElementById('attendance-entry-panel').hidden = Boolean(user);
  document.getElementById('operator-label').textContent = user ? 'USER SANDBOX' : 'LOCAL PREVIEW';
  document.getElementById('employee-storage-mode').textContent = user ? 'User-scoped Firestore' : 'Shared local process memory';
  document.getElementById('operator-avatar').textContent = user ? (user.email || 'D').slice(0, 1).toUpperCase() : 'D';
  loadEmployees();
  loadWorkflows();
}

function showSignIn(message = '') {
  state.user = null;
  state.account = null;
  sessionStorage.removeItem(ACCOUNT_KEY);
  resetSessionData();
  appShell.hidden = true;
  document.getElementById('account-gate').hidden = true;
  authGate.hidden = false;
  setAuthError(message);
}

const rows = document.getElementById('employee-rows');
const search = document.getElementById('employee-search');
const count = document.getElementById('employee-count');
const resultCount = document.getElementById('result-count');
const seedDemoButton = document.getElementById('seed-demo-button');
const dialog = document.getElementById('employee-dialog');
const dialogTitle = document.getElementById('dialog-title');
const dialogContent = document.getElementById('dialog-content');
const attendanceRows = document.getElementById('attendance-rows');
const attendanceEmployee = document.getElementById('attendance-employee');
const attendanceFrom = document.getElementById('attendance-from');
const attendanceTo = document.getElementById('attendance-to');
const attendanceResultCount = document.getElementById('attendance-result-count');

function initials(name) {
  return name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
}

function makeCell(content) {
  const cell = document.createElement('td');
  if (content instanceof Node) cell.append(content);
  else cell.textContent = content;
  return cell;
}

function downloadCsvFile(filename, columns, records) {
  const csv = buildCsv(columns, records);
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.hidden = true;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function downloadJsonFile(filename, data) {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  link.hidden = true;
  document.body.append(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

async function fetchSandboxSnapshot() {
  const headers = await apiHeaders();
  const [healthResponse, employeesResponse, workflowsResponse] = await Promise.all([
    fetch('/api/health', { headers }),
    fetch('/api/employees', { headers }),
    fetch('/api/workflows', { headers })
  ]);
  const [health, employeePayload, workflowPayload] = await Promise.all([
    healthResponse.json(), employeesResponse.json(), workflowsResponse.json()
  ]);
  for (const [response, payload, label] of [
    [healthResponse, health, 'Health'],
    [employeesResponse, employeePayload, 'Employee'],
    [workflowsResponse, workflowPayload, 'Workflow']
  ]) {
    if (!response.ok) throw new Error(payload.error || `${label} request failed (${response.status})`);
  }
  if (!Array.isArray(employeePayload.items)) throw new Error('Unexpected employee response.');
  const attendancePairs = await Promise.all(employeePayload.items.map(async employee => {
    const response = await fetch(`/api/attendance?employeeId=${encodeURIComponent(employee.id)}`, { headers });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Attendance request failed (${response.status})`);
    return [employee.id, Array.isArray(payload.items) ? payload.items : []];
  }));
  return {
    mode: health.mode || 'unknown',
    employees: employeePayload.items,
    leaveRequests: Array.isArray(workflowPayload.leaveRequests) ? workflowPayload.leaveRequests : [],
    tickets: Array.isArray(workflowPayload.tickets) ? workflowPayload.tickets : [],
    attendanceByEmployee: Object.fromEntries(attendancePairs)
  };
}

function renderSandboxSearchResults(results, employees) {
  const container = document.getElementById('sandbox-search-results');
  container.replaceChildren();
  const names = new Map((Array.isArray(employees) ? employees : []).map(employee => [employee.id, employee.name || employee.id]));
  const groups = [
    { title: 'Employees', items: results.employees, format: item => `${item.name} · ${item.id} · ${item.department} · stored status: ${item.status}` },
    { title: 'Raw attendance', items: results.attendance, format: item => `${item.date} · ${names.get(item.employeeId) || item.employeeId} (${item.employeeId}) · ${item.timeIn}–${item.timeOut} · ${item.dayType}` },
    { title: 'Leave requests', items: results.leaveRequests, format: item => `${item.id} · ${item.employeeName} · ${item.leaveType} · ${item.startDate}–${item.endDate} · stored status: ${item.status}` },
    { title: 'Tickets', items: results.tickets, format: item => `${item.id} · ${item.employeeName} · ${item.ticketType} · ${item.queue} · stored status: ${item.status}` }
  ];
  const total = groups.reduce((count, group) => count + group.items.length, 0);
  if (!total) {
    const empty = document.createElement('p');
    empty.textContent = 'No matching synthetic records. A blank result does not indicate attendance status or eligibility.';
    container.append(empty);
    return total;
  }
  groups.forEach(group => {
    if (!group.items.length) return;
    const section = document.createElement('section');
    section.className = 'sandbox-search-result-group';
    const heading = document.createElement('h3');
    heading.textContent = `${group.title} (${group.items.length})`;
    const list = document.createElement('ul');
    group.items.forEach(item => {
      const row = document.createElement('li');
      row.textContent = group.format(item);
      list.append(row);
    });
    section.append(heading, list);
    container.append(section);
  });
  return total;
}

function showTableMessage(message) {
  rows.replaceChildren();
  const row = document.createElement('tr');
  const cell = document.createElement('td');
  cell.colSpan = 6;
  cell.className = 'table-message';
  cell.textContent = message;
  row.append(cell);
  rows.append(row);
}

function renderEmployees() {
  rows.replaceChildren();
  const visibleEmployees = sortDirectoryEntries(state.employees, {
    field: document.getElementById('employee-sort').value,
    direction: document.getElementById('employee-order').value
  });
  if (visibleEmployees.length === 0) {
    showTableMessage(search.value.trim() ? 'No employee records match that search.' : 'No demo records are available.');
  } else {
    visibleEmployees.forEach(employee => {
      const row = document.createElement('tr');
      row.className = 'employee-row';
      // Preserve convenient pointer activation while keeping native table semantics.
      row.addEventListener('click', () => openEmployee(employee));

      const person = document.createElement('div');
      person.className = 'person-cell';
      const avatar = document.createElement('span');
      avatar.className = 'person-initials';
      avatar.setAttribute('aria-hidden', 'true');
      avatar.textContent = initials(employee.name);
      const nameBlock = document.createElement('span');
      const name = document.createElement('span');
      name.className = 'person-name';
      name.textContent = employee.name;
      const id = document.createElement('span');
      id.className = 'person-id';
      id.textContent = employee.id;
      nameBlock.append(name, id);
      person.append(avatar, nameBlock);
      row.append(makeCell(person));
      row.append(makeCell(employee.department));
      row.append(makeCell(employee.role));
      row.append(makeCell(employee.location));

      const status = document.createElement('span');
      status.className = `status-pill${employee.status.toLowerCase() === 'active' ? '' : ' leave'}`;
      status.textContent = employee.status;
      row.append(makeCell(status));
      const openButton = document.createElement('button');
      openButton.type = 'button';
      openButton.className = 'row-open-button';
      openButton.setAttribute('aria-label', `Open ${employee.name}, ${employee.id}`);
      openButton.addEventListener('click', event => {
        event.stopPropagation();
        openEmployee(employee);
      });
      const arrow = document.createElement('span');
      arrow.className = 'row-arrow';
      arrow.setAttribute('aria-hidden', 'true');
      arrow.textContent = '›';
      openButton.append(arrow);
      row.append(makeCell(openButton));
      rows.append(row);
    });
  }

  count.textContent = String(state.totalEmployees).padStart(2, '0');
  const overviewCount = document.getElementById('overview-employee-count');
  if (overviewCount) overviewCount.textContent = String(state.totalEmployees).padStart(2, '0');
  resultCount.textContent = `${state.employees.length} ${state.employees.length === 1 ? 'record' : 'records'} shown`;
  document.getElementById('employee-export-button').disabled = state.employees.length === 0;
}

function openEmployee(employee) {
  state.selected = employee;
  dialogTitle.textContent = employee.name;
  dialogContent.replaceChildren();
  const fields = [
    ['EMPLOYEE ID', employee.id],
    ['EMPLOYMENT STATUS', employee.status],
    ['DEPARTMENT', employee.department],
    ['ROLE', employee.role],
    ['LOCATION', employee.location],
    ['DATA SOURCE', 'Synthetic demo record']
  ];
  const grid = document.createElement('div');
  grid.className = 'detail-grid';
  fields.forEach(([labelText, valueText]) => {
    const item = document.createElement('div');
    item.className = 'detail-item';
    const label = document.createElement('span');
    label.textContent = labelText;
    const value = document.createElement('strong');
    value.textContent = valueText;
    item.append(label, value);
    grid.append(item);
  });
  const records = document.createElement('section');
  records.className = 'employee-linked-records';
  const recordsHeading = document.createElement('h3');
  recordsHeading.textContent = 'Linked sandbox records';
  const recordsNote = document.createElement('p');
  recordsNote.textContent = 'Raw attendance times and stored request/ticket labels only. No attendance or payroll calculations are shown.';
  const recordsStatus = document.createElement('p');
  recordsStatus.className = 'employee-linked-records-status';
  recordsStatus.setAttribute('role', 'status');
  recordsStatus.setAttribute('aria-live', 'polite');
  recordsStatus.textContent = 'Loading linked records…';
  records.append(recordsHeading, recordsNote, recordsStatus);
  dialogContent.append(grid, records);
  dialog.showModal();
  loadEmployeeSandboxRecords(employee.id, records);
}

async function loadEmployeeSandboxRecords(employeeId, container) {
  try {
    const headers = await apiHeaders();
    const [attendanceResponse, workflowResponse] = await Promise.all([
      fetch(`/api/attendance?employeeId=${encodeURIComponent(employeeId)}`, { headers }),
      fetch('/api/workflows', { headers })
    ]);
    const [attendancePayload, workflowPayload] = await Promise.all([
      attendanceResponse.json(), workflowResponse.json()
    ]);
    if (!attendanceResponse.ok) throw new Error(attendancePayload.error || `Attendance request failed (${attendanceResponse.status})`);
    if (!workflowResponse.ok) throw new Error(workflowPayload.error || `Workflow request failed (${workflowResponse.status})`);
    if (state.selected?.id !== employeeId || !container.isConnected) return;
    const attendance = Array.isArray(attendancePayload.items) ? attendancePayload.items : [];
    const leave = Array.isArray(workflowPayload.leaveRequests)
      ? workflowPayload.leaveRequests.filter(item => item.employeeId === employeeId) : [];
    const tickets = Array.isArray(workflowPayload.tickets)
      ? workflowPayload.tickets.filter(item => item.employeeId === employeeId) : [];
    const appendList = (title, rows, format) => {
      const group = document.createElement('div');
      group.className = 'employee-linked-record-group';
      const heading = document.createElement('h4');
      heading.textContent = `${title} (${rows.length})`;
      group.append(heading);
      if (!rows.length) {
        const empty = document.createElement('p');
        empty.textContent = 'No stored rows for this employee in this sandbox.';
        group.append(empty);
      } else {
        const list = document.createElement('ul');
        rows.forEach(item => {
          const row = document.createElement('li');
          row.textContent = format(item);
          list.append(row);
        });
        group.append(list);
      }
      container.append(group);
    };
    container.querySelector('.employee-linked-records-status')?.remove();
    appendList('Raw attendance', attendance, item => `${item.date} · ${item.timeIn}–${item.timeOut} · ${item.dayType}`);
    appendList('Leave requests', leave, item => `${item.leaveType} · ${item.startDate}–${item.endDate} · stored status: ${item.status}`);
    appendList('Tickets', tickets, item => `${item.ticketType} · ${item.queue} · stored status: ${item.status}`);
  } catch (error) {
    if (!container.isConnected || state.selected?.id !== employeeId) return;
    const status = container.querySelector('.employee-linked-records-status');
    if (status) status.textContent = error.message || 'Could not load linked records.';
  }
}

function editSyntheticEmployee(employee) {
  document.getElementById('employee-edit-id').value = employee.id;
  document.getElementById('employee-edit-name').value = employee.name;
  document.getElementById('employee-edit-department').value = employee.department;
  document.getElementById('employee-edit-role').value = employee.role;
  document.getElementById('employee-edit-location').value = employee.location;
  document.getElementById('employee-edit-status').value = employee.status;
  document.getElementById('employee-editor-title').textContent = `Edit ${employee.id}`;
  document.getElementById('employee-save-button').textContent = 'Save synthetic record';
  document.getElementById('employee-cancel-edit-button').hidden = false;
  document.getElementById('employee-form').scrollIntoView({ behavior: 'smooth', block: 'center' });
  document.getElementById('employee-edit-name').focus();
}

function populateAttendanceEmployees() {
  [[attendanceEmployee, 'attendance-view'], [document.getElementById('attendance-entry-employee'), 'attendance-edit']].forEach(([select, purpose]) => {
    if (!select) return;
    const current = select.value;
    select.replaceChildren();
    state.allEmployees.filter(employee => canPickFor(state.account, purpose, employee.id)).forEach(employee => {
      const option = document.createElement('option');
      option.value = employee.id;
      option.textContent = `${employee.id} — ${employee.name}`;
      select.append(option);
    });
    if (state.allEmployees.some(employee => employee.id === current)) select.value = current;
  });
}

function populateWorkflowEmployees() {
  document.getElementById('leave-reviewer-group').hidden = firebaseConfigured;
  document.getElementById('overtime-panel').hidden = firebaseConfigured;
  document.getElementById('payroll-processing-panel').hidden = firebaseConfigured;
  document.querySelector('.nav-item[data-view="config"]').hidden = firebaseConfigured;
  ['schedule-panel', 'correction-panel', 'paychange-panel', 'hr-stats-panel', 'personal-panel', 'outbox-panel', 'onboard-panel', 'catalog-panel', 'resolve-form', 'government-panel'].forEach(id => { document.getElementById(id).hidden = firebaseConfigured; });
  if (!firebaseConfigured && state.account && ['HR', 'ADMIN'].includes(state.account.role)) hrTools.refresh();
  if (!firebaseConfigured) government.refresh();
  if (!firebaseConfigured) adminConfig.refresh();
  if (!firebaseConfigured) adminSecurity.refresh();
  if (!firebaseConfigured) supervisorTools.refresh();
  if (!firebaseConfigured) payrollProcessing.refreshTargets();
  if (!firebaseConfigured) adjustmentForms.refresh();
  document.getElementById('payroll-history').hidden = firebaseConfigured;
  if (!firebaseConfigured) {
    overtime.refresh();
    cutoffPayroll.refreshHistory();
  }
  const purposes = { 'leave-employee': 'leave', 'ticket-employee': 'ticket', 'payroll-cutoff-employee': 'payroll', 'overtime-employee': 'overtime', 'adj-employee': 'processing',
    'schedule-employee': 'schedule', 'daychange-employee': 'schedule', 'paychange-employee': 'paychange', 'correction-employee': 'attendance-edit',
    'gov-employee': 'government', 'gov-access-employee': 'government' };
  if (state.account) {
    // Reviews are recorded as the signed-in account, so the reviewer pickers only hold yourself.
    document.getElementById('leave-reviewer-group').hidden = true;
    document.getElementById('overtime-reviewer').closest('label').hidden = true;
  }
  ['leave-employee', 'ticket-employee', 'leave-reviewer', 'payroll-cutoff-employee', 'overtime-employee', 'overtime-reviewer', 'adj-employee',
    'schedule-employee', 'daychange-employee', 'paychange-employee', 'correction-employee', 'gov-employee', 'gov-access-employee'].forEach(id => {
    const select = document.getElementById(id);
    const current = select.value;
    select.replaceChildren();
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = state.allEmployees.length ? 'Choose synthetic employee' : 'Load directory first';
    select.append(placeholder);
    const allowed = employee => !state.account
      || (purposes[id] ? canPickFor(state.account, purposes[id], employee.id) : employee.id === state.account.employeeId);
    const choices = state.allEmployees.filter(allowed);
    select.dataset.only = state.account && choices.length === 1 ? choices[0].id : '';
    choices.forEach(employee => {
      const option = document.createElement('option');
      option.value = employee.id;
      option.textContent = `${employee.id} — ${employee.name}`;
      select.append(option);
    });
    if (state.allEmployees.some(employee => employee.id === current)) select.value = current;
    if (select.dataset.only) select.value = select.dataset.only;
  });
}

function showWorkflowTableMessage(tbody, columnCount, message) {
  tbody.replaceChildren();
  const row = document.createElement('tr');
  const cell = document.createElement('td');
  cell.colSpan = columnCount;
  cell.className = 'table-message';
  cell.textContent = message;
  row.append(cell);
  tbody.append(row);
}

function makeWorkflowStatus(status) {
  const badge = document.createElement('span');
  const quiet = status === 'Cancelled' || status === 'Rejected';
  badge.className = `status-pill${quiet ? ' workflow-cancelled' : (status.toLowerCase() === 'active' || status === 'Approved' ? '' : ' leave')}`;
  badge.textContent = status;
  return badge;
}

function makeWorkflowDetails(item, kind) {
  const details = document.createElement('details');
  details.className = 'workflow-detail';
  const summary = document.createElement('summary');
  summary.textContent = 'Details';
  summary.setAttribute('aria-label', `View details for ${item.id}`);
  details.append(summary);
  const list = document.createElement('dl');
  list.className = 'workflow-detail-fields';
  const fields = kind === 'leave'
    ? [['NOTE', item.note || 'No note provided'], ['SUBMITTED (UTC)', item.submittedAt || 'Not recorded']]
    : [['MESSAGE', item.summary || 'No message'], ['QUEUE LABEL', item.queue || 'Not recorded'], ['SUBMITTED (UTC)', item.submittedAt || 'Not recorded']];
  if (item.cancelledAt) fields.push(['CANCELLED (UTC)', item.cancelledAt]);
  if (item.resolvedAt) fields.push(['REPLY', item.reply], ['RESOLVED BY', item.resolvedBy], ['RESOLVED (UTC)', item.resolvedAt]);
  if (item.reviewedAt) fields.push([`${(item.status || 'REVIEWED').toUpperCase()} BY`, item.reviewerId || 'Not recorded'], ['REVIEWED (UTC)', item.reviewedAt]);
  fields.forEach(([label, value]) => {
    const term = document.createElement('dt');
    const description = document.createElement('dd');
    term.textContent = label;
    description.textContent = value;
    list.append(term, description);
  });
  details.append(list);
  return details;
}

function makeCancelButton(action, item, employeeId, label) {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'button button-quiet workflow-cancel';
  button.dataset.action = action;
  button.dataset.id = item.id;
  button.dataset.employeeId = employeeId;
  button.textContent = label;
  button.setAttribute('aria-label', `${label} ${item.id} for ${item.employeeName}`);
  return button;
}

function selectLeaveCalendarDate(date) {
  const startInput = document.getElementById('leave-start');
  const endInput = document.getElementById('leave-end');
  const status = document.getElementById('leave-calendar-selection-status');
  if (!leaveDateSelectionStart || leaveDateSelectionEnd) {
    leaveDateSelectionStart = date;
    leaveDateSelectionEnd = '';
    startInput.value = date;
    endInput.value = '';
    status.textContent = `Draft start date ${date} selected; choose an end date. Nothing has been submitted.`;
  } else if (date < leaveDateSelectionStart) {
    leaveDateSelectionStart = date;
    leaveDateSelectionEnd = '';
    startInput.value = date;
    endInput.value = '';
    status.textContent = `Start date reset to ${date}; choose an end date. Nothing has been submitted.`;
  } else {
    leaveDateSelectionEnd = date;
    endInput.value = date;
    status.textContent = `Draft range ${leaveDateSelectionStart} through ${date} selected. Review the form and submit explicitly; no days or balances are calculated.`;
  }
  renderLeaveCalendar();
}

function renderLeaveCalendar() {
  const tbody = document.getElementById('leave-calendar-grid');
  if (!tbody) return;
  const calendar = buildLeaveCalendar(leaveCalendarMonth, state.workflow.leaveRequests);
  // Design doc "bimonthly calendar view": one cutoff half at a time, or the whole month.
  const span = document.getElementById('leave-calendar-span').value;
  const inSpan = day => day.inMonth && (span === 'month' || (span === 'first' ? day.day <= 15 : day.day >= 16));
  document.getElementById('leave-calendar-month').textContent = span === 'month' ? calendar.title : `${calendar.title} · ${span === 'first' ? '1–15' : '16–end'}`;
  tbody.replaceChildren();
  calendar.weeks.filter(week => week.some(inSpan)).forEach(week => {
    const row = document.createElement('tr');
    week.forEach(day => {
      const cell = document.createElement('td');
      cell.className = `calendar-day${inSpan(day) ? '' : ' outside-month'}${day.isToday ? ' today' : ''}`;
      if (!inSpan(day)) {
        cell.setAttribute('aria-hidden', 'true');
        row.append(cell);
        return;
      }
      cell.setAttribute('aria-label', `${day.date}${day.requests.length ? `, ${day.requests.length} request date${day.requests.length === 1 ? '' : 's'}` : ''}`);
      const dateNumber = document.createElement('button');
      dateNumber.type = 'button';
      dateNumber.className = `calendar-date-select${day.date === leaveDateSelectionStart || day.date === leaveDateSelectionEnd ? ' selected' : ''}${day.isToday ? ' today' : ''}`;
      dateNumber.dataset.date = day.date;
      dateNumber.textContent = String(day.day);
      dateNumber.setAttribute('aria-label', `Choose ${day.date} as a request date${day.requests.length ? `; ${day.requests.length} submitted request marker${day.requests.length === 1 ? '' : 's'}` : ''}`);
      dateNumber.addEventListener('click', () => selectLeaveCalendarDate(day.date));
      cell.append(dateNumber);
      if (day.requests.length) {
        const list = document.createElement('ul');
        list.className = 'calendar-events';
        day.requests.forEach(request => {
          const event = document.createElement('li');
          event.className = `calendar-event${request.status === 'Cancelled' ? ' cancelled' : ''}`;
          event.textContent = leaveEventText(request);
          event.title = `${request.employeeName} · ${request.employeeId} · ${request.leaveType} · ${request.status}`;
          list.append(event);
        });
        cell.append(list);
      }
      row.append(cell);
    });
    tbody.append(row);
  });
}

let leaveBalanceRequest = 0;
async function renderLeaveBalances() {
  const target = document.getElementById('leave-balance');
  const employeeId = document.getElementById('leave-employee').value;
  if (!target) return;
  if (!employeeId) {
    target.textContent = 'Choose an employee to see temporary credits.';
    return;
  }
  const start = document.getElementById('leave-start').value;
  const year = /^\d{4}-/.test(start) ? start.slice(0, 4) : String(new Date().getFullYear());
  const requestNumber = ++leaveBalanceRequest;
  try {
    const response = await fetch(`/api/workflows/leave/balances?employeeId=${encodeURIComponent(employeeId)}&year=${year}`,
      { headers: await apiHeaders() });
    const payload = await response.json();
    if (requestNumber !== leaveBalanceRequest) return; // a newer refresh already started
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    const table = document.createElement('table');
    const caption = document.createElement('caption');
    caption.textContent = `${employeeId} · ${payload.year} · weekdays`;
    const head = document.createElement('tr');
    ['TYPE', 'YEARLY', 'USED (APPROVED)', 'PENDING', 'REMAINING'].forEach(label => {
      const th = document.createElement('th');
      th.scope = 'col';
      th.textContent = label;
      head.append(th);
    });
    const thead = document.createElement('thead');
    thead.append(head);
    const tbody = document.createElement('tbody');
    payload.balances.forEach(balance => {
      const row = document.createElement('tr');
      [balance.leaveType, balance.allowance, balance.used, balance.pending, balance.remaining]
        .forEach(value => row.append(makeCell(String(value))));
      tbody.append(row);
    });
    table.append(caption, thead, tbody);
    target.replaceChildren(table);
  } catch (error) {
    if (requestNumber === leaveBalanceRequest) target.textContent = error.message || 'Could not load temporary credits.';
  }
}

function renderWorkflow() {
  if (!document.getElementById('leave-request-rows')) return;
  renderLeaveBalances();
  renderDirectoryViews(); // the roster's "Today" column follows approved leave
  const leaveRequests = state.workflow.leaveRequests;
  const filteredLeaves = sortWorkflowItems(filterWorkflowItems(leaveRequests, {
    query: document.getElementById('leave-filter-search').value,
    status: document.getElementById('leave-filter-status').value,
    type: document.getElementById('leave-filter-type').value,
    typeField: 'leaveType',
    searchFields: ['id', 'employeeId', 'employeeName', 'leaveType', 'startDate', 'endDate'],
    dateRange: {
      from: document.getElementById('leave-filter-from').value,
      to: document.getElementById('leave-filter-to').value,
      startField: 'startDate',
      endField: 'endDate'
    }
  }), {
    field: document.getElementById('leave-sort').value,
    direction: document.getElementById('leave-sort-order').value
  });
  state.visibleLeaveRequests = filteredLeaves;
  const leaveRows = document.getElementById('leave-request-rows');
  leaveRows.replaceChildren();
  if (!leaveRequests.length) {
    showWorkflowTableMessage(leaveRows, 6, 'No session requests yet.');
  } else if (!filteredLeaves.length) {
    showWorkflowTableMessage(leaveRows, 6, 'No requests match these filters.');
  } else {
    filteredLeaves.forEach(request => {
      const row = document.createElement('tr');
      [request.id, `${request.employeeName} · ${request.employeeId}`, request.leaveType,
        `${request.startDate} — ${request.endDate}`].forEach(value => row.append(makeCell(value)));
      row.append(makeCell(makeWorkflowStatus(request.status)));
      const actions = document.createElement('div');
      actions.className = 'workflow-row-actions';
      actions.append(makeWorkflowDetails(request, 'leave'));
      if (request.status === 'Pending review') {
        if (canCancel(state.account, 'leave', request.employeeId)) actions.append(makeCancelButton('cancel-leave', request, request.employeeId, 'Cancel'));
        const reviewerId = state.account ? state.account.employeeId : document.getElementById('leave-reviewer').value;
        const mayReview = state.account ? canReview(state.account, 'leave', request.employeeId) : reviewerId !== request.employeeId;
        if (!firebaseConfigured && reviewerId && mayReview) {
          actions.append(makeCancelButton('approve-leave', request, reviewerId, 'Approve'),
            makeCancelButton('reject-leave', request, reviewerId, 'Reject'));
        }
      }
      row.append(makeCell(actions));
      leaveRows.append(row);
    });
  }
  document.getElementById('leave-request-count').textContent = `Showing ${filteredLeaves.length} of ${leaveRequests.length} ${leaveRequests.length === 1 ? 'request' : 'requests'} in this sandbox`;

  const tickets = state.workflow.tickets;
  const filteredTickets = sortWorkflowItems(filterWorkflowItems(tickets, {
    query: document.getElementById('ticket-filter-search').value,
    status: document.getElementById('ticket-filter-status').value,
    type: document.getElementById('ticket-filter-type').value,
    typeField: 'ticketType',
    searchFields: ['id', 'employeeId', 'employeeName', 'ticketType', 'queue', 'summary']
  }), {
    field: document.getElementById('ticket-sort').value,
    direction: document.getElementById('ticket-sort-order').value
  });
  state.visibleTickets = filteredTickets;
  const ticketRows = document.getElementById('ticket-rows');
  ticketRows.replaceChildren();
  if (!tickets.length) {
    showWorkflowTableMessage(ticketRows, 6, 'No session tickets yet.');
  } else if (!filteredTickets.length) {
    showWorkflowTableMessage(ticketRows, 6, 'No tickets match these filters.');
  } else {
    filteredTickets.forEach(ticket => {
      const row = document.createElement('tr');
      [ticket.id, `${ticket.employeeName} · ${ticket.employeeId}`, ticket.ticketType, ticket.queuePosition ? `${ticket.queue} · #${ticket.queuePosition} in queue` : ticket.queue]
        .forEach(value => row.append(makeCell(value)));
      row.append(makeCell(makeWorkflowStatus(ticket.status)));
      const actions = document.createElement('div');
      actions.className = 'workflow-row-actions';
      actions.append(makeWorkflowDetails(ticket, 'ticket'));
      if (ticket.status === 'Open' && canCancel(state.account, 'tickets', ticket.employeeId)) actions.append(makeCancelButton('cancel-ticket', ticket, ticket.employeeId, 'Cancel'));
      // The ticket's handler (design doc: routed by type) takes it, then replies and resolves it. Local demo only.
      if (!firebaseConfigured && canHandleTicket(state.account, ticket) && ['Open', 'In progress'].includes(ticket.status)) {
        if (ticket.status === 'Open') actions.append(makeCancelButton('start-ticket', ticket, ticket.employeeId, 'Start'));
        actions.append(makeCancelButton('resolve-ticket', ticket, ticket.employeeId, 'Resolve'));
      }
      row.append(makeCell(actions));
      ticketRows.append(row);
    });
  }
  document.getElementById('ticket-count').textContent = `Showing ${filteredTickets.length} of ${tickets.length} ${tickets.length === 1 ? 'ticket' : 'tickets'} in this sandbox`;
  document.getElementById('leave-export-button').disabled = leaveRequests.length === 0;
  document.getElementById('leave-visible-export-button').disabled = filteredLeaves.length === 0;
  document.getElementById('ticket-export-button').disabled = tickets.length === 0;
  document.getElementById('ticket-visible-export-button').disabled = filteredTickets.length === 0;
  const overviewWorkflowCount = document.getElementById('overview-workflow-count');
  if (overviewWorkflowCount) overviewWorkflowCount.textContent = String(leaveRequests.length + tickets.length).padStart(2, '0');
  renderWorkflowSummary();
  renderEmployeeRecordSummary();
  renderLeaveCalendar();
}

function renderWorkflowSummary() {
  const summaryRows = workflowSummaryRows(state.workflow.leaveRequests, state.workflow.tickets);
  const body = document.getElementById('workflow-summary-rows');
  body.replaceChildren();
  if (!summaryRows.length) {
    showWorkflowTableMessage(body, 3, 'No synthetic workflow records are available for this summary.');
  } else {
    summaryRows.forEach(summary => {
      const row = document.createElement('tr');
      [summary.category, summary.label, String(summary.recordCount)].forEach(value => row.append(makeCell(value)));
      body.append(row);
    });
  }
  const recordTotal = state.workflow.leaveRequests.length + state.workflow.tickets.length;
  document.getElementById('workflow-summary-count').textContent = `${summaryRows.length} stored-label rows · ${recordTotal} workflow records`;
  document.getElementById('workflow-summary-export').disabled = summaryRows.length === 0;
}

function renderEmployeeRecordSummary() {
  const summary = employeeRecordSummary(state.allEmployees, state.attendanceReport,
    state.workflow.leaveRequests, state.workflow.tickets);
  state.employeeRecordSummary = summary;
  const visible = filterAndSortEmployeeRecordSummary(summary, {
    query: document.getElementById('employee-record-summary-search').value,
    mapping: document.getElementById('employee-record-summary-mapping').value,
    sortBy: document.getElementById('employee-record-summary-sort').value,
    direction: document.getElementById('employee-record-summary-order').value
  });
  state.visibleEmployeeRecordSummary = visible;
  const body = document.getElementById('employee-record-summary-rows');
  if (!body) return;
  body.replaceChildren();
  if (!summary.length) {
    showWorkflowTableMessage(body, 7, 'No synthetic employees are available for this summary.');
  } else if (!visible.length) {
    showWorkflowTableMessage(body, 7, 'No employee rows match these filters.');
  } else {
    visible.forEach(item => {
      const row = document.createElement('tr');
      [
        `${item.employeeName} · ${item.employeeId}`,
        item.hasScheduleMapping ? 'Mapped' : 'No mapping',
        String(item.rawRecordCount),
        item.firstRecordDate || '—',
        item.lastRecordDate || '—',
        String(item.leaveRequestCount),
        String(item.ticketCount)
      ].forEach(value => row.append(makeCell(value)));
      body.append(row);
    });
  }
  document.getElementById('employee-record-summary-count').textContent = `Showing ${visible.length} of ${summary.length} employee rows · stored counts only`;
  document.getElementById('employee-record-summary-export').disabled = summary.length === 0;
  document.getElementById('employee-record-summary-visible-export').disabled = visible.length === 0;
}

function setWorkflowStorageMode(value) {
  document.getElementById('workflow-storage-mode').textContent = value;
}

async function loadWorkflows() {
  const status = document.getElementById('workflow-load-status');
  status.hidden = true;
  status.textContent = '';
  setWorkflowStorageMode(state.user ? 'User-scoped Firestore' : 'Local API memory');
  try {
    const response = await fetch('/api/workflows', { headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    if (!payload || !Array.isArray(payload.leaveRequests) || !Array.isArray(payload.tickets)) {
      throw new Error('Unexpected workflow response.');
    }
    state.workflow = {
      ...createWorkflowState(),
      leaveRequests: payload.leaveRequests,
      tickets: payload.tickets
    };
    setWorkflowStorageMode(payload.storage === 'user-scoped-firestore' ? 'User-scoped Firestore' : 'Local API memory');
    renderWorkflow();
    hrTools.refreshTickets();
  } catch (error) {
    state.workflow = createWorkflowState();
    renderWorkflow();
    setWorkflowStorageMode(state.user ? 'Rules access required' : 'Local API unavailable');
    status.textContent = error.message || 'Could not load workflow records.';
    status.hidden = false;
  }
}

/** Team roster "Today": Present / Tardy / On leave / Absent from the server (local demo), else from approved leave. */
function todayStatus(employee) {
  const leave = availabilityFor(employee, state.workflow.leaveRequests, todayKey());
  const live = (state.teamToday || {})[employee.id];
  if (!live) return leave;
  return leave.startsWith('Available · leave from') ? `${live} · ${leave.slice('Available · '.length)}` : live;
}

async function loadTeamToday() {
  if (firebaseConfigured || !state.account) return;
  try {
    const { items } = await payrollJson('/api/team/today');
    state.teamToday = Object.fromEntries(items.map(item => [item.employeeId, item.status]));
    renderDirectoryViews();
  } catch {
    state.teamToday = {};
  }
}

function renderDirectoryViews() {
  const teamRows = document.getElementById('team-rows');
  const hrRows = document.getElementById('hr-rows');
  if (!teamRows || !hrRows) return;
  const employees = state.allEmployees;
  document.getElementById('directory-summary-export').disabled = employees.length === 0;
  const filteredTeam = sortDirectoryEntries(filterWorkflowItems(employees, {
    query: document.getElementById('team-directory-search').value,
    status: document.getElementById('team-directory-status').value,
    searchFields: ['id', 'name', 'department', 'role', 'location', 'status']
  }), {
    field: document.getElementById('team-directory-sort').value,
    direction: document.getElementById('team-directory-order').value
  });
  const filteredEmployees = sortDirectoryEntries(filterWorkflowItems(employees, {
    query: document.getElementById('hr-directory-search').value,
    status: document.getElementById('hr-directory-status').value,
    searchFields: ['id', 'name', 'department', 'role', 'location', 'status']
  }), {
    field: document.getElementById('hr-directory-sort').value,
    direction: document.getElementById('hr-directory-order').value
  });
  teamRows.replaceChildren();
  hrRows.replaceChildren();
  document.getElementById('directory-report-total').textContent = String(employees.length).padStart(2, '0');
  document.getElementById('directory-report-active').textContent = String(employees.filter(employee => employee.status === 'Active').length).padStart(2, '0');
  document.getElementById('directory-report-onleave').textContent = String(employees.filter(employee => employee.status === 'On leave').length).padStart(2, '0');
  const departmentCounts = new Map();
  employees.forEach(employee => departmentCounts.set(employee.department, (departmentCounts.get(employee.department) || 0) + 1));
  const departmentList = document.getElementById('directory-department-counts');
  departmentList.replaceChildren();
  if (!departmentCounts.size) {
    const item = document.createElement('li');
    item.textContent = 'No directory labels loaded.';
    departmentList.append(item);
  } else {
    [...departmentCounts.entries()].sort((a, b) => a[0].localeCompare(b[0])).forEach(([department, count]) => {
      const item = document.createElement('li');
      item.textContent = `${department}: ${count}`;
      departmentList.append(item);
    });
  }
  if (!employees.length) {
    showWorkflowTableMessage(teamRows, 7, 'No synthetic directory records are available.');
    showWorkflowTableMessage(hrRows, 6, 'No synthetic directory records are available.');
  } else {
    if (!filteredTeam.length) {
      showWorkflowTableMessage(teamRows, 7, 'No records match these filters.');
    } else {
      filteredTeam.forEach(employee => {
        const teamRow = document.createElement('tr');
        const teamPerson = document.createElement('button');
        teamPerson.type = 'button';
        teamPerson.className = 'button button-quiet';
        teamPerson.textContent = `${employee.name} · ${employee.id}`;
        teamPerson.setAttribute('aria-label', `View synthetic record for ${employee.name}`);
        teamPerson.addEventListener('click', () => openEmployee(employee));
        const contact = contactFor(employee);
        const contactCell = document.createElement('span');
        const mail = document.createElement('a');
        mail.href = `mailto:${contact.email}`;
        mail.textContent = contact.email;
        contactCell.append(mail, document.createElement('br'), contact.extension);
        [teamPerson, employee.department, employee.role, employee.location, makeWorkflowStatus(employee.status), contactCell,
          todayStatus(employee)].forEach(value => teamRow.append(makeCell(value)));
        teamRows.append(teamRow);
      });
    }
    if (!filteredEmployees.length) {
      showWorkflowTableMessage(hrRows, 6, 'No records match these filters.');
    } else {
      filteredEmployees.forEach(employee => {
        const hrRow = document.createElement('tr');
        const hrPerson = document.createElement('strong');
        hrPerson.textContent = `${employee.name} · ${employee.id}`;
        [hrPerson, employee.department, employee.role, employee.location, makeWorkflowStatus(employee.status)]
          .forEach(value => hrRow.append(makeCell(value)));
        const actions = document.createElement('div');
        actions.className = 'directory-actions';
        const detailButton = document.createElement('button');
        detailButton.type = 'button';
        detailButton.className = 'button button-quiet workflow-cancel';
        detailButton.textContent = 'View';
        detailButton.setAttribute('aria-label', `View synthetic record for ${employee.name}`);
        detailButton.addEventListener('click', () => openEmployee(employee));
        const editButton = document.createElement('button');
        editButton.type = 'button';
        editButton.className = 'button button-quiet workflow-cancel';
        editButton.textContent = 'Edit';
        editButton.setAttribute('aria-label', `Edit synthetic directory record for ${employee.name}`);
        editButton.addEventListener('click', () => editSyntheticEmployee(employee));
        actions.append(detailButton, editButton);
        if (!firebaseConfigured && state.account && state.account.role === 'ADMIN' && employee.id !== state.account.employeeId) {
          const deleteButton = document.createElement('button');
          deleteButton.type = 'button';
          deleteButton.className = 'button button-quiet workflow-cancel';
          deleteButton.textContent = 'Delete';
          deleteButton.setAttribute('aria-label', `Delete the record of ${employee.name}`);
          deleteButton.addEventListener('click', () => deleteEmployeeRecord(employee));
          actions.append(deleteButton);
        }
        hrRow.append(makeCell(actions));
        hrRows.append(hrRow);
      });
    }
  }
  const total = employees.length;
  document.getElementById('team-count').textContent = `Showing ${filteredTeam.length} of ${total} ${total === 1 ? 'record' : 'records'}`;
  document.getElementById('team-export-button').disabled = filteredTeam.length === 0;
  document.getElementById('hr-directory-export').disabled = filteredEmployees.length === 0;
  document.getElementById('hr-count').textContent = `Showing ${filteredEmployees.length} of ${total} ${total === 1 ? 'record' : 'records'}`;
  renderEmployeeRecordSummary();
}

/** System Admin delete: only records with no history (the server refuses the rest and says to offboard instead). */
async function deleteEmployeeRecord(employee) {
  const confirmText = window.prompt(`Permanently delete the record of ${employee.name} (${employee.id})? This only works for a record with no attendance, pay or requests. Type DELETE to confirm.`, '');
  if (confirmText === null) return;
  const status = document.getElementById('employee-form-status');
  try {
    await payrollJson(`/api/employees/${encodeURIComponent(employee.id)}/delete`, { confirm: confirmText.trim() });
    status.textContent = `Deleted ${employee.name} (${employee.id}). The change history keeps a note of it.`;
    await loadEmployees();
  } catch (error) {
    status.textContent = error.message;
  }
}

function setWorkflowStatus(id, message, error = false) {
  const element = document.getElementById(id);
  element.textContent = message;
  element.classList.toggle('workflow-error', error);
  element.classList.toggle('workflow-success', Boolean(message) && !error);
}

function showAttendanceMessage(message) {
  attendanceRows.replaceChildren();
  const row = document.createElement('tr');
  const cell = document.createElement('td');
  cell.colSpan = 6;
  cell.className = 'table-message';
  cell.textContent = message;
  row.append(cell);
  attendanceRows.append(row);
}

function applyDefaultAttendanceRange(records) {
  if (!records.length || attendanceFrom.value || attendanceTo.value) return;
  const dates = records.map(record => String(record.date))
    .filter(date => /^\d{4}-\d{2}-\d{2}$/.test(date))
    .sort();
  if (!dates.length) return;
  // The legacy MO-IT108 requirement says the attendance view defaults to its full available date span.
  // This is not a pay-period calendar and does not derive hours or payroll values.
  attendanceFrom.value = dates[0];
  attendanceTo.value = dates[dates.length - 1];
}

function renderAttendanceMetrics(metrics) {
  const list = document.getElementById('attendance-metrics');
  list.replaceChildren();
  const rows = metricsRows(metrics);
  document.getElementById('attendance-metrics-range').textContent = rows.length ? `${metrics.from} – ${metrics.to}` : 'No attendance in this range';
  rows.forEach(([label, value]) => {
    const term = document.createElement('dt');
    term.textContent = label;
    const description = document.createElement('dd');
    description.textContent = value;
    list.append(term, description);
  });
}

function renderRawAttendanceSummary(records) {
  const dates = records.map(record => String(record.date)).filter(date => /^\d{4}-\d{2}-\d{2}$/.test(date)).sort();
  document.getElementById('attendance-summary-count').textContent = String(records.length).padStart(2, '0');
  document.getElementById('attendance-summary-first').textContent = dates[0] || '—';
  document.getElementById('attendance-summary-last').textContent = dates[dates.length - 1] || '—';
  const overviewCount = document.getElementById('overview-attendance-count');
  if (overviewCount) overviewCount.textContent = String(records.length).padStart(2, '0');
}

function clearAttendanceCalendarSelection() {
  attendanceCalendarSelectionStart = '';
  attendanceCalendarSelectionEnd = '';
  document.getElementById('attendance-calendar-selection-status').textContent = 'Choose a start and end date to filter raw rows.';
  renderAttendanceCalendar(state.rawAttendance);
}

function selectAttendanceCalendarDate(date) {
  const status = document.getElementById('attendance-calendar-selection-status');
  if (!attendanceCalendarSelectionStart || attendanceCalendarSelectionEnd) {
    attendanceCalendarSelectionStart = date;
    attendanceCalendarSelectionEnd = '';
    status.textContent = `Start date ${date} selected. Choose an end date; no attendance status or hours are inferred.`;
    renderAttendanceCalendar(state.rawAttendance);
    return;
  }
  const from = date < attendanceCalendarSelectionStart ? date : attendanceCalendarSelectionStart;
  const to = date < attendanceCalendarSelectionStart ? attendanceCalendarSelectionStart : date;
  attendanceCalendarSelectionStart = from;
  attendanceCalendarSelectionEnd = to;
  attendanceFrom.value = from;
  attendanceTo.value = to;
  status.textContent = `Applied inclusive raw-record range ${from} through ${to}; no days or hours were calculated.`;
  loadAttendance();
}

function renderAttendanceCalendar(records) {
  const employeeId = attendanceEmployee.value;
  if (attendanceCalendarEmployee !== employeeId) {
    attendanceCalendarEmployee = employeeId;
    const earliest = records.map(record => record.date).filter(value => /^\d{4}-\d{2}-\d{2}$/.test(value || '')).sort()[0];
    attendanceCalendarMonth = earliest
      ? new Date(Number(earliest.slice(0, 4)), Number(earliest.slice(5, 7)) - 1, 1)
      : new Date();
  }
  if (!attendanceCalendarMonth) attendanceCalendarMonth = new Date();
  const calendar = buildAttendanceCalendar(attendanceCalendarMonth, records);
  document.getElementById('attendance-calendar-month').textContent = calendar.title;
  const grid = document.getElementById('attendance-calendar-grid');
  grid.replaceChildren();
  calendar.weeks.forEach(week => {
    const row = document.createElement('tr');
    week.forEach(cell => {
      const dayCell = document.createElement('td');
      if (!cell.inMonth) dayCell.classList.add('outside-month');
      if (cell.date) dayCell.dataset.date = cell.date;
      dayCell.setAttribute('aria-label', cell.inMonth
        ? `${cell.date}, ${cell.records.length} raw ${cell.records.length === 1 ? 'record' : 'records'}`
        : 'Outside current month');
      if (cell.inMonth) {
        const day = document.createElement('button');
        day.type = 'button';
        day.className = `calendar-date-select${cell.date === attendanceCalendarSelectionStart || cell.date === attendanceCalendarSelectionEnd ? ' selected' : ''}`;
        day.dataset.date = cell.date;
        day.textContent = String(cell.day);
        day.setAttribute('aria-label', `Select ${cell.date} for raw attendance date range`);
        day.addEventListener('click', () => selectAttendanceCalendarDate(cell.date));
        dayCell.append(day);
        if (cell.records.length) {
          const list = document.createElement('ul');
          list.className = 'calendar-events';
          cell.records.forEach(record => {
            const event = document.createElement('li');
            event.className = 'calendar-event';
            event.textContent = `${record.timeIn}–${record.timeOut}`;
            event.title = `Raw row ${record.attendanceId}: ${record.timeIn} to ${record.timeOut}, ${record.dayType}`;
            event.setAttribute('aria-label', `Raw record ${record.attendanceId}, ${record.timeIn} to ${record.timeOut}, ${record.dayType}`);
            list.append(event);
          });
          dayCell.append(list);
        }
      }
      row.append(dayCell);
    });
    grid.append(row);
  });
}

function renderAttendance(records) {
  state.rawAttendance = sortAttendanceRecords(records, {
    field: document.getElementById('attendance-sort').value,
    direction: document.getElementById('attendance-order').value
  });
  const orderedRecords = state.rawAttendance;
  document.getElementById('attendance-export-button').disabled = orderedRecords.length === 0;
  applyDefaultAttendanceRange(orderedRecords);
  renderRawAttendanceSummary(orderedRecords);
  attendanceRows.replaceChildren();
  if (orderedRecords.length === 0) {
    showAttendanceMessage('No attendance entries match this employee and date range.');
  } else {
    orderedRecords.forEach(record => {
      const row = document.createElement('tr');
      [record.date, String(record.attendanceId), String(record.scheduleId), record.timeIn, record.timeOut, record.dayType]
        .forEach(value => row.append(makeCell(value)));
      attendanceRows.append(row);
    });
  }
  attendanceResultCount.textContent = `${orderedRecords.length} raw ${orderedRecords.length === 1 ? 'entry' : 'entries'} loaded`;
  renderAttendanceCalendar(orderedRecords);
}

async function loadAttendance() {
  const employeeId = attendanceEmployee.value;
  if (!employeeId) {
    state.rawAttendance = [];
    renderRawAttendanceSummary([]);
    renderAttendanceCalendar([]);
    document.getElementById('attendance-export-button').disabled = true;
    showAttendanceMessage('Load the employee directory first.');
    attendanceResultCount.textContent = 'No records loaded';
    return;
  }
  const requestNumber = ++state.attendanceRequestNumber;
  state.rawAttendance = [];
  renderRawAttendanceSummary([]);
  document.getElementById('attendance-export-button').disabled = true;
  showAttendanceMessage('Loading raw attendance records…');
  attendanceResultCount.textContent = 'Loading';
  const params = new URLSearchParams({ employeeId });
  const from = attendanceFrom.value;
  const to = attendanceTo.value;
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  try {
    const response = await fetch(`/api/attendance?${params.toString()}`, { headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    if (requestNumber !== state.attendanceRequestNumber) return;
    renderAttendance(Array.isArray(payload.items) ? payload.items : []);
    renderAttendanceMetrics(payload.metrics);
  } catch (error) {
    renderAttendanceMetrics(null);
    if (requestNumber !== state.attendanceRequestNumber) return;
    state.rawAttendance = [];
    renderRawAttendanceSummary([]);
    renderAttendanceCalendar([]);
    document.getElementById('attendance-export-button').disabled = true;
    showAttendanceMessage(error.message || 'Could not load attendance records.');
    attendanceResultCount.textContent = 'Could not load records';
  }
}

function renderAttendanceReport(items) {
  state.attendanceReport = items;
  applyAttendanceReportFilters();
  renderEmployeeRecordSummary();
}

function applyAttendanceReportFilters() {
  const items = sortAttendanceCoverage(filterAttendanceCoverage(state.attendanceReport, {
    query: document.getElementById('attendance-report-search').value,
    mapping: document.getElementById('attendance-report-mapping').value
  }), {
    field: document.getElementById('attendance-report-sort').value,
    direction: document.getElementById('attendance-report-order').value
  });
  const rows = document.getElementById('attendance-report-rows');
  rows.replaceChildren();
  if (!items.length) {
    const message = state.attendanceReport.length
      ? 'No employees match these report filters.'
      : 'No synthetic employees are available for this report.';
    showWorkflowTableMessage(rows, 6, message);
  } else {
    items.forEach(item => {
      const row = document.createElement('tr');
      row.append(makeCell(`${item.employeeName} · ${item.employeeId}`));
      row.append(makeCell(item.hasScheduleMapping ? `Schedule ${item.scheduleId}` : 'No mapping'));
      row.append(makeCell(String(item.rawRecordCount)));
      row.append(makeCell(item.firstRecordDate || '—'));
      row.append(makeCell(item.lastRecordDate || '—'));
      if (item.hasScheduleMapping) {
        const openButton = document.createElement('button');
        openButton.type = 'button';
        openButton.className = 'button button-quiet workflow-report-open';
        openButton.dataset.employeeId = item.employeeId;
        openButton.textContent = 'View rows';
        openButton.setAttribute('aria-label', `View raw entries for ${item.employeeId}`);
        row.append(makeCell(openButton));
      } else {
        row.append(makeCell('—'));
      }
      rows.append(row);
    });
  }
  const totalRawRows = items.reduce((total, item) => total + (Number.isInteger(item.rawRecordCount) ? item.rawRecordCount : 0), 0);
  const countLabel = `${items.length} of ${state.attendanceReport.length} employee ${state.attendanceReport.length === 1 ? 'row' : 'rows'} visible · ${totalRawRows} raw ${totalRawRows === 1 ? 'record' : 'records'} in range`;
  document.getElementById('attendance-report-count').textContent = countLabel;
  document.getElementById('attendance-report-export').disabled = items.length === 0;
}

async function loadAttendanceReport() {
  const requestNumber = ++state.reportRequestNumber;
  const status = document.getElementById('attendance-report-status');
  status.hidden = true;
  status.textContent = '';
  document.getElementById('attendance-report-count').textContent = 'Loading report';
  document.getElementById('attendance-report-export').disabled = true;
  showWorkflowTableMessage(document.getElementById('attendance-report-rows'), 6, 'Loading raw attendance summary…');
  const params = new URLSearchParams();
  const from = document.getElementById('attendance-report-from').value;
  const to = document.getElementById('attendance-report-to').value;
  if (from) params.set('from', from);
  if (to) params.set('to', to);
  const query = params.toString();
  try {
    const response = await fetch(`/api/reports/attendance${query ? `?${query}` : ''}`, { headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    if (!payload || !Array.isArray(payload.items) || payload.hoursCalculated !== false) {
      throw new Error('Unexpected attendance report response.');
    }
    if (requestNumber !== state.reportRequestNumber) return;
    renderAttendanceReport(payload.items);
    status.textContent = 'Loaded raw record counts and date coverage only; no hours or payroll values were calculated.';
    status.hidden = false;
  } catch (error) {
    if (requestNumber !== state.reportRequestNumber) return;
    state.attendanceReport = [];
    showWorkflowTableMessage(document.getElementById('attendance-report-rows'), 6, error.message || 'Could not load the raw attendance report.');
    document.getElementById('attendance-report-count').textContent = 'Could not load report';
    document.getElementById('attendance-report-export').disabled = true;
    status.textContent = error.message || 'Could not load the raw attendance report.';
    status.hidden = false;
  }
}

async function loadEmployees() {
  const requestNumber = ++state.requestNumber;
  const query = search.value.trim();
  showTableMessage('Loading employee records…');
  resultCount.textContent = 'Loading';
  try {
    const url = query ? `/api/employees?q=${encodeURIComponent(query)}` : '/api/employees';
    const response = await fetch(url, { headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    if (!payload || !Array.isArray(payload.items)) throw new Error('Unexpected employee response');
    if (requestNumber !== state.requestNumber) return;
    state.employees = payload.items;
    state.totalEmployees = Number.isInteger(payload.total) ? payload.total : payload.items.length;
    if (!query) {
      state.allEmployees = payload.items;
      populateAttendanceEmployees();
      populateWorkflowEmployees();
      renderDirectoryViews();
    }
    seedDemoButton.hidden = !(state.user && state.totalEmployees === 0 && !query);
    renderEmployees();
  } catch (error) {
    if (requestNumber !== state.requestNumber) return;
    state.employees = [];
    state.allEmployees = [];
    seedDemoButton.hidden = true;
    populateAttendanceEmployees();
    populateWorkflowEmployees();
    renderDirectoryViews();
    renderEmployees();
    showTableMessage(error.message || 'Could not reach the Java service. Check that the prototype server is running, then refresh.');
    resultCount.textContent = 'Could not load records';
  }
}

async function seedSyntheticDemoData() {
  seedDemoButton.disabled = true;
  seedDemoButton.textContent = 'Writing synthetic records…';
  try {
    const response = await fetch('/api/demo/seed', { method: 'POST', headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    resultCount.textContent = `${payload.employeesCreated} employees and ${payload.attendanceCreated} attendance entries added`;
    await loadEmployees();
  } catch (error) {
    showTableMessage(error.message || 'Could not write the synthetic demo records.');
    resultCount.textContent = 'Demo setup failed';
  } finally {
    seedDemoButton.disabled = false;
    seedDemoButton.textContent = 'Add synthetic demo data';
  }
}

function applyWorkflowItem(collection, item, storage) {
  const current = state.workflow[collection];
  const index = current.findIndex(existing => existing.id === item.id);
  const next = [...current];
  if (index < 0) next.unshift(item);
  else next[index] = item;
  state.workflow = { ...state.workflow, [collection]: next };
  setWorkflowStorageMode(storage === 'user-scoped-firestore' ? 'User-scoped Firestore' : 'Local API memory');
  renderWorkflow();
}

async function sendWorkflowRequest(url, body) {
  const headers = await apiHeaders(body === undefined ? {} : { 'Content-Type': 'application/json' });
  const options = { method: 'POST', headers };
  if (body !== undefined) options.body = JSON.stringify(body);
  const response = await fetch(url, options);
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
  if (!payload || !payload.item) throw new Error('Unexpected workflow response.');
  return payload;
}

document.getElementById('employee-form').addEventListener('submit', async event => {
  event.preventDefault();
  const id = document.getElementById('employee-edit-id').value;
  const body = {
    name: document.getElementById('employee-edit-name').value,
    department: document.getElementById('employee-edit-department').value,
    role: document.getElementById('employee-edit-role').value,
    location: document.getElementById('employee-edit-location').value,
    status: document.getElementById('employee-edit-status').value
  };
  const button = document.getElementById('employee-save-button');
  button.disabled = true;
  try {
    const headers = await apiHeaders({ 'Content-Type': 'application/json' });
    const response = await fetch(id ? `/api/employees/${encodeURIComponent(id)}` : '/api/employees', {
      method: id ? 'PUT' : 'POST', headers, body: JSON.stringify(body)
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    const employeeId = payload.item && payload.item.id;
    resetEmployeeEditor();
    search.value = '';
    await loadEmployees();
    setWorkflowStatus('employee-form-status', `${id ? 'Updated' : 'Created'} ${employeeId || 'synthetic record'} in ${state.user ? 'your user-scoped Firestore sandbox' : 'shared local API-process memory'}. No schedule mapping was added.`);
  } catch (error) {
    setWorkflowStatus('employee-form-status', error.message || 'Could not save this synthetic record.', true);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('employee-cancel-edit-button').addEventListener('click', resetEmployeeEditor);

document.getElementById('leave-request-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const employeeId = document.getElementById('leave-employee').value;
  const input = {
    employeeId,
    leaveType: document.getElementById('leave-type').value,
    startDate: document.getElementById('leave-start').value,
    endDate: document.getElementById('leave-end').value,
    note: document.getElementById('leave-note').value
  };
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    submitLeaveRequest(createWorkflowState(), input, state.allEmployees);
    const payload = await sendWorkflowRequest('/api/workflows/leave', input);
    const [year, month] = input.startDate.split('-').map(Number);
    leaveCalendarMonth = new Date(year, month - 1, 1);
    leaveDateSelectionStart = '';
    leaveDateSelectionEnd = '';
    document.getElementById('leave-calendar-selection-status').textContent = 'Request submitted; choose dates to draft another range.';
    applyWorkflowItem('leaveRequests', payload.item, payload.storage);
    form.reset();
    document.getElementById('leave-employee').value = employeeId;
    const storage = payload.storage === 'user-scoped-firestore' ? 'saved to your user sandbox' : 'saved in local server memory';
    setWorkflowStatus('leave-form-status', `${payload.item.id} ${storage}. No reviewer was notified.`);
  } catch (error) {
    setWorkflowStatus('leave-form-status', error.message || 'Could not create this demo request.', true);
  } finally {
    button.disabled = false;
  }
});

document.getElementById('ticket-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const employeeId = document.getElementById('ticket-employee').value;
  const input = {
    employeeId,
    ticketType: document.getElementById('ticket-type').value,
    summary: document.getElementById('ticket-summary').value
  };
  const button = form.querySelector('button[type="submit"]');
  button.disabled = true;
  try {
    submitTicket(createWorkflowState(), input, state.allEmployees);
    const payload = await sendWorkflowRequest('/api/workflows/tickets', input);
    applyWorkflowItem('tickets', payload.item, payload.storage);
    form.reset();
    document.getElementById('ticket-employee').value = employeeId;
    const storage = payload.storage === 'user-scoped-firestore' ? 'saved to your user sandbox' : 'saved in local server memory';
    setWorkflowStatus('ticket-form-status', `${payload.item.id} ${storage}; queue label ${payload.item.queue}, not delivered.`);
  } catch (error) {
    setWorkflowStatus('ticket-form-status', error.message || 'Could not create this demo ticket.', true);
  } finally {
    button.disabled = false;
  }
});

async function handleTicketWork(button) {
  const status = document.getElementById('ticket-form-status');
  const action = button.dataset.action === 'start-ticket' ? 'start' : 'resolve';
  const body = {};
  if (action === 'resolve') {
    const reply = window.prompt('Reply to the employee (they see it with the ticket):', '');
    if (reply === null) return;
    body.reply = reply;
  }
  button.disabled = true;
  try {
    const { item } = await payrollJson(`/api/workflows/tickets/${encodeURIComponent(button.dataset.id)}/${action}`, body);
    applyWorkflowItem('tickets', item, 'local-process-memory');
    renderWorkflow();
    setWorkflowStatus('ticket-form-status', `${item.id} is ${item.status.toLowerCase()}. The employee was notified.`);
  } catch (error) {
    status.textContent = error.message;
    button.disabled = false;
  }
}

async function handleWorkflowCancel(event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  if (button.dataset.action === 'start-ticket' || button.dataset.action === 'resolve-ticket') {
    await handleTicketWork(button);
    return;
  }
  const decision = { 'approve-leave': 'approve', 'reject-leave': 'reject' }[button.dataset.action];
  const isLeave = button.dataset.action === 'cancel-leave' || Boolean(decision);
  const formStatus = isLeave ? 'leave-form-status' : 'ticket-form-status';
  const collection = isLeave ? 'leaveRequests' : 'tickets';
  button.disabled = true;
  if (decision) {
    try {
      const payload = await sendWorkflowRequest(`/api/workflows/leave/${encodeURIComponent(button.dataset.id)}/${decision}`,
        { reviewerId: button.dataset.employeeId });
      applyWorkflowItem(collection, payload.item, payload.storage);
      setWorkflowStatus(formStatus, `${button.dataset.id} ${payload.item.status.toLowerCase()} by ${payload.item.reviewerId} (local demo). No balance changed; no one was notified.`);
    } catch (error) {
      setWorkflowStatus(formStatus, error.message || 'Could not review this demo leave request.', true);
      button.disabled = false;
    }
    return;
  }
  try {
    const payload = await sendWorkflowRequest(`/api/workflows/${isLeave ? 'leave' : 'tickets'}/${encodeURIComponent(button.dataset.id)}/cancel`);
    applyWorkflowItem(collection, payload.item, payload.storage);
    setWorkflowStatus(formStatus, `${button.dataset.id} cancelled in this user sandbox. No reviewer was notified.`);
  } catch (error) {
    setWorkflowStatus(formStatus, error.message || 'Could not cancel this demo item.', true);
    button.disabled = false;
  }
}

document.getElementById('leave-request-rows').addEventListener('click', handleWorkflowCancel);
document.getElementById('ticket-rows').addEventListener('click', handleWorkflowCancel);
['leave-filter-search', 'ticket-filter-search'].forEach(id => document.getElementById(id).addEventListener('input', renderWorkflow));
['leave-reviewer', 'leave-filter-status', 'leave-filter-type', 'ticket-filter-status', 'ticket-filter-type', 'leave-sort', 'leave-sort-order', 'ticket-sort', 'ticket-sort-order']
  .forEach(id => document.getElementById(id).addEventListener('change', renderWorkflow));
['leave-filter-from', 'leave-filter-to'].forEach(id => document.getElementById(id).addEventListener('input', renderWorkflow));
['leave-employee', 'leave-start'].forEach(id => document.getElementById(id).addEventListener('change', renderLeaveBalances));
document.getElementById('employee-record-summary-search').addEventListener('input', renderEmployeeRecordSummary);
['employee-record-summary-mapping', 'employee-record-summary-sort', 'employee-record-summary-order']
  .forEach(id => document.getElementById(id).addEventListener('change', renderEmployeeRecordSummary));
document.getElementById('hr-directory-search').addEventListener('input', renderDirectoryViews);
document.getElementById('hr-directory-status').addEventListener('change', renderDirectoryViews);
document.getElementById('hr-directory-sort').addEventListener('change', renderDirectoryViews);
document.getElementById('hr-directory-order').addEventListener('change', renderDirectoryViews);
document.getElementById('team-directory-search').addEventListener('input', renderDirectoryViews);
document.getElementById('team-directory-status').addEventListener('change', renderDirectoryViews);
document.getElementById('team-directory-sort').addEventListener('change', renderDirectoryViews);
document.getElementById('team-directory-order').addEventListener('change', renderDirectoryViews);
['leave-start', 'leave-end'].forEach(id => document.getElementById(id).addEventListener('input', () => {
  leaveDateSelectionStart = '';
  leaveDateSelectionEnd = '';
  document.getElementById('leave-calendar-selection-status').textContent = 'Form dates changed directly; calendar markers still show submitted requests only.';
  renderLeaveCalendar();
}));
document.getElementById('leave-calendar-prev').addEventListener('click', () => {
  leaveCalendarMonth = new Date(leaveCalendarMonth.getFullYear(), leaveCalendarMonth.getMonth() - 1, 1);
  renderLeaveCalendar();
});
document.getElementById('leave-calendar-next').addEventListener('click', () => {
  leaveCalendarMonth = new Date(leaveCalendarMonth.getFullYear(), leaveCalendarMonth.getMonth() + 1, 1);
  renderLeaveCalendar();
});
document.querySelectorAll('[data-open-requests]').forEach(button => button.addEventListener('click', () => switchView('requests')));

seedDemoButton.addEventListener('click', seedSyntheticDemoData);

search.addEventListener('input', () => {
  window.clearTimeout(state.searchTimer);
  state.searchTimer = window.setTimeout(loadEmployees, 180);
});
document.getElementById('employee-sort').addEventListener('change', renderEmployees);
document.getElementById('employee-order').addEventListener('change', renderEmployees);
document.getElementById('refresh-button').addEventListener('click', loadEmployees);
document.getElementById('employee-export-button').addEventListener('click', () => {
  try {
    downloadCsvFile('motorph-synthetic-employees.csv', [
      { key: 'id', label: 'Employee ID' },
      { key: 'name', label: 'Name' },
      { key: 'department', label: 'Department' },
      { key: 'role', label: 'Role' },
      { key: 'location', label: 'Location' },
      { key: 'status', label: 'Status' }
    ], sortDirectoryEntries(state.employees, {
      field: document.getElementById('employee-sort').value,
      direction: document.getElementById('employee-order').value
    }));
    document.getElementById('employee-export-status').textContent = `Exported ${state.employees.length} currently visible synthetic employee records in the selected sort order.`;
  } catch (error) {
    document.getElementById('employee-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('team-export-button').addEventListener('click', () => {
  try {
    const employees = sortDirectoryEntries(filterWorkflowItems(state.allEmployees, {
      query: document.getElementById('team-directory-search').value,
      status: document.getElementById('team-directory-status').value,
      searchFields: ['id', 'name', 'department', 'role', 'location', 'status']
    }), {
      field: document.getElementById('team-directory-sort').value,
      direction: document.getElementById('team-directory-order').value
    });
    downloadCsvFile('motorph-synthetic-roster.csv', [
      { key: 'id', label: 'Employee ID' },
      { key: 'name', label: 'Name' },
      { key: 'department', label: 'Department' },
      { key: 'role', label: 'Role' },
      { key: 'location', label: 'Location' },
      { key: 'status', label: 'Stored Demo Status' },
      { key: 'email', label: 'Work Email (synthetic)' },
      { key: 'extension', label: 'Extension (synthetic)' },
      { key: 'availability', label: 'Today' }
    ], employees.map(employee => ({ ...employee, ...contactFor(employee), availability: availabilityFor(employee, state.workflow.leaveRequests, todayKey()) })));
    document.getElementById('team-export-status').textContent = `Exported ${employees.length} currently filtered synthetic directory rows.`;
  } catch (error) {
    document.getElementById('team-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('hr-directory-export').addEventListener('click', () => {
  try {
    const employees = sortDirectoryEntries(filterWorkflowItems(state.allEmployees, {
      query: document.getElementById('hr-directory-search').value,
      status: document.getElementById('hr-directory-status').value,
      searchFields: ['id', 'name', 'department', 'role', 'location', 'status']
    }), {
      field: document.getElementById('hr-directory-sort').value,
      direction: document.getElementById('hr-directory-order').value
    });
    downloadCsvFile('motorph-hr-directory.csv', [
      { key: 'id', label: 'Employee ID' },
      { key: 'name', label: 'Name' },
      { key: 'department', label: 'Department' },
      { key: 'role', label: 'Role' },
      { key: 'location', label: 'Location' },
      { key: 'status', label: 'Stored Demo Status' }
    ], employees);
    document.getElementById('hr-directory-export-status').textContent = `Exported ${employees.length} currently filtered synthetic directory rows.`;
  } catch (error) {
    document.getElementById('hr-directory-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('employee-record-summary-export').addEventListener('click', () => {
  try {
    const rows = employeeRecordSummary(state.allEmployees, state.attendanceReport,
      state.workflow.leaveRequests, state.workflow.tickets).map(item => ({
      employeeId: item.employeeId,
      employeeName: item.employeeName,
      department: item.department,
      scheduleMapping: item.hasScheduleMapping ? 'Mapped' : 'No mapping',
      rawRecordCount: item.rawRecordCount,
      firstRecordDate: item.firstRecordDate,
      lastRecordDate: item.lastRecordDate,
      leaveRequestCount: item.leaveRequestCount,
      ticketCount: item.ticketCount
    }));
    downloadCsvFile('motorph-employee-record-counts.csv', [
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'department', label: 'Department' },
      { key: 'scheduleMapping', label: 'Schedule Mapping' },
      { key: 'rawRecordCount', label: 'Raw Attendance Rows' },
      { key: 'firstRecordDate', label: 'First Raw Date' },
      { key: 'lastRecordDate', label: 'Last Raw Date' },
      { key: 'leaveRequestCount', label: 'Stored Leave Request Count' },
      { key: 'ticketCount', label: 'Stored Ticket Count' }
    ], rows);
    document.getElementById('employee-record-summary-export-status').textContent = `Exported stored counts for ${rows.length} synthetic employees; no classifications were calculated.`;
  } catch (error) {
    document.getElementById('employee-record-summary-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});

document.getElementById('employee-record-summary-visible-export').addEventListener('click', () => {
  try {
    const rows = state.visibleEmployeeRecordSummary.map(item => ({
      employeeId: item.employeeId,
      employeeName: item.employeeName,
      department: item.department,
      scheduleMapping: item.hasScheduleMapping ? 'Mapped' : 'No mapping',
      rawRecordCount: item.rawRecordCount,
      firstRecordDate: item.firstRecordDate,
      lastRecordDate: item.lastRecordDate,
      leaveRequestCount: item.leaveRequestCount,
      ticketCount: item.ticketCount
    }));
    downloadCsvFile('motorph-employee-record-counts-visible.csv', [
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'department', label: 'Department' },
      { key: 'scheduleMapping', label: 'Schedule Mapping' },
      { key: 'rawRecordCount', label: 'Raw Attendance Rows' },
      { key: 'firstRecordDate', label: 'First Raw Date' },
      { key: 'lastRecordDate', label: 'Last Raw Date' },
      { key: 'leaveRequestCount', label: 'Stored Leave Request Count' },
      { key: 'ticketCount', label: 'Stored Ticket Count' }
    ], rows);
    document.getElementById('employee-record-summary-export-status').textContent = `Exported ${rows.length} currently visible employee rows.`;
  } catch (error) {
    document.getElementById('employee-record-summary-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});

document.getElementById('directory-summary-export').addEventListener('click', () => {
  try {
    const rows = directoryLabelSummaryRows(state.allEmployees);
    downloadCsvFile('motorph-directory-label-summary.csv', [
      { key: 'category', label: 'Count Category' },
      { key: 'label', label: 'Stored Synthetic Label' },
      { key: 'employeeRecords', label: 'Employee Records' }
    ], rows);
    document.getElementById('directory-summary-export-status').textContent = `Exported ${rows.length} synthetic directory label-count rows.`;
  } catch (error) {
    document.getElementById('directory-summary-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('workflow-summary-export').addEventListener('click', () => {
  try {
    const rows = workflowSummaryRows(state.workflow.leaveRequests, state.workflow.tickets);
    downloadCsvFile('motorph-workflow-label-summary.csv', [
      { key: 'category', label: 'Workflow Label Category' },
      { key: 'label', label: 'Stored Demo Label' },
      { key: 'recordCount', label: 'Record Count' }
    ], rows);
    document.getElementById('workflow-summary-export-status').textContent = `Exported ${rows.length} synthetic workflow label-count rows.`;
  } catch (error) {
    document.getElementById('workflow-summary-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('attendance-export-button').addEventListener('click', () => {
  try {
    const employeeId = attendanceEmployee.value.replace(/[^A-Za-z0-9._-]/g, '_');
    downloadCsvFile(`motorph-raw-attendance-${employeeId || 'synthetic'}.csv`, [
      { key: 'date', label: 'Date' },
      { key: 'attendanceId', label: 'Attendance ID' },
      { key: 'scheduleId', label: 'Schedule ID' },
      { key: 'timeIn', label: 'Time In' },
      { key: 'timeOut', label: 'Time Out' },
      { key: 'dayType', label: 'Day Type' }
    ], state.rawAttendance);
    document.getElementById('attendance-export-status').textContent = `Exported ${state.rawAttendance.length} raw synthetic attendance records. No hours or payroll values were derived.`;
  } catch (error) {
    document.getElementById('attendance-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('attendance-report-export').addEventListener('click', () => {
  try {
    const visibleItems = sortAttendanceCoverage(filterAttendanceCoverage(state.attendanceReport, {
      query: document.getElementById('attendance-report-search').value,
      mapping: document.getElementById('attendance-report-mapping').value
    }), {
      field: document.getElementById('attendance-report-sort').value,
      direction: document.getElementById('attendance-report-order').value
    });
    const rows = visibleItems.map(item => ({
      employeeId: item.employeeId,
      employeeName: item.employeeName,
      scheduleMapping: item.hasScheduleMapping ? `Schedule ${item.scheduleId}` : 'No mapping',
      rawRecordCount: item.rawRecordCount,
      firstRecordDate: item.firstRecordDate,
      lastRecordDate: item.lastRecordDate
    }));
    downloadCsvFile('motorph-raw-attendance-coverage.csv', [
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'scheduleMapping', label: 'Schedule Mapping' },
      { key: 'rawRecordCount', label: 'Raw Record Count' },
      { key: 'firstRecordDate', label: 'First Record Date' },
      { key: 'lastRecordDate', label: 'Last Record Date' }
    ], rows);
    document.getElementById('attendance-report-export-status').textContent = `Exported coverage for ${rows.length} currently visible synthetic employees; no hours were derived.`;
  } catch (error) {
    document.getElementById('attendance-report-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('leave-export-button').addEventListener('click', () => {
  try {
    downloadCsvFile('motorph-demo-leave-requests.csv', [
      { key: 'id', label: 'Request ID' },
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'leaveType', label: 'Leave Type' },
      { key: 'startDate', label: 'Start Date' },
      { key: 'endDate', label: 'End Date' },
      { key: 'status', label: 'Status' },
      { key: 'note', label: 'Note' },
      { key: 'submittedAt', label: 'Submitted At' }
    ], state.workflow.leaveRequests);
    document.getElementById('leave-export-status').textContent = `Exported ${state.workflow.leaveRequests.length} synthetic leave requests.`;
  } catch (error) {
    document.getElementById('leave-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('leave-visible-export-button').addEventListener('click', () => {
  try {
    downloadCsvFile('motorph-demo-leave-requests-visible.csv', [
      { key: 'id', label: 'Request ID' },
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'leaveType', label: 'Leave Type' },
      { key: 'startDate', label: 'Start Date' },
      { key: 'endDate', label: 'End Date' },
      { key: 'status', label: 'Status' },
      { key: 'note', label: 'Note' },
      { key: 'submittedAt', label: 'Submitted At' }
    ], state.visibleLeaveRequests);
    document.getElementById('leave-export-status').textContent = `Exported ${state.visibleLeaveRequests.length} visible synthetic leave requests.`;
  } catch (error) {
    document.getElementById('leave-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});
document.getElementById('ticket-export-button').addEventListener('click', () => {
  try {
    downloadCsvFile('motorph-demo-tickets.csv', [
      { key: 'id', label: 'Ticket ID' },
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'ticketType', label: 'Ticket Type' },
      { key: 'queue', label: 'Queue Label' },
      { key: 'summary', label: 'Summary' },
      { key: 'status', label: 'Status' },
      { key: 'submittedAt', label: 'Submitted At' }
    ], state.workflow.tickets);
    document.getElementById('ticket-export-status').textContent = `Exported ${state.workflow.tickets.length} synthetic tickets.`;
  } catch (error) {
    document.getElementById('ticket-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});

document.getElementById('ticket-visible-export-button').addEventListener('click', () => {
  try {
    downloadCsvFile('motorph-demo-tickets-visible.csv', [
      { key: 'id', label: 'Ticket ID' },
      { key: 'employeeId', label: 'Employee ID' },
      { key: 'employeeName', label: 'Employee Name' },
      { key: 'ticketType', label: 'Ticket Type' },
      { key: 'queue', label: 'Queue Label' },
      { key: 'summary', label: 'Summary' },
      { key: 'status', label: 'Status' },
      { key: 'submittedAt', label: 'Submitted At' }
    ], state.visibleTickets);
    document.getElementById('ticket-export-status').textContent = `Exported ${state.visibleTickets.length} visible synthetic tickets.`;
  } catch (error) {
    document.getElementById('ticket-export-status').textContent = `Export failed: ${error.message || 'unknown error'}`;
  }
});

document.getElementById('sandbox-export-button').addEventListener('click', async event => {
  const button = event.currentTarget;
  const status = document.getElementById('sandbox-export-status');
  button.disabled = true;
  status.hidden = false;
  status.textContent = 'Reading the current sandbox; no records will be changed…';
  try {
    const snapshot = await fetchSandboxSnapshot();
    const bundle = buildWorkspaceExport(snapshot);
    downloadJsonFile(`motorph-demo-workspace-${bundle.exportedAt.slice(0, 10)}.json`, bundle);
    status.textContent = `Downloaded ${bundle.employees.length} synthetic employee records, ${bundle.attendance.length} raw attendance rows, ${bundle.leaveRequests.length} leave requests, and ${bundle.tickets.length} tickets. No calculations were added.`;
  } catch (error) {
    status.textContent = `Export failed: ${error.message || 'unknown error'}`;
  } finally {
    button.disabled = false;
  }
});

document.getElementById('sandbox-search-form').addEventListener('submit', async event => {
  event.preventDefault();
  const query = document.getElementById('sandbox-search-query').value.trim();
  const status = document.getElementById('sandbox-search-status');
  const button = event.currentTarget.querySelector('button[type="submit"]');
  status.hidden = false;
  if (!query) {
    status.textContent = 'Enter a search term; blank searches do not load or classify records.';
    document.getElementById('sandbox-search-results').replaceChildren();
    return;
  }
  button.disabled = true;
  status.textContent = 'Searching the current sandbox…';
  try {
    const snapshot = await fetchSandboxSnapshot();
    const matches = searchSandboxRecords(query, snapshot);
    const count = renderSandboxSearchResults(matches, snapshot.employees);
    status.textContent = `${count} matching stored records across the current sandbox. No attendance or payroll values were derived.`;
  } catch (error) {
    status.textContent = `Search failed: ${error.message || 'unknown error'}`;
    document.getElementById('sandbox-search-results').replaceChildren();
  } finally {
    button.disabled = false;
  }
});

attendanceFrom.addEventListener('input', clearAttendanceCalendarSelection);
attendanceTo.addEventListener('input', clearAttendanceCalendarSelection);
document.getElementById('attendance-filter').addEventListener('submit', event => {
  event.preventDefault();
  loadAttendance();
});
document.getElementById('attendance-entry-form').addEventListener('submit', async event => {
  event.preventDefault();
  const employeeId = document.getElementById('attendance-entry-employee').value;
  const attendanceDate = document.getElementById('attendance-entry-date').value;
  const button = document.getElementById('attendance-entry-submit');
  setWorkflowStatus('attendance-entry-status', 'Adding a local synthetic raw row…');
  button.disabled = true;
  try {
    const response = await fetch('/api/attendance', {
      method: 'POST',
      headers: await apiHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify({
        employeeId,
        date: attendanceDate,
        timeIn: document.getElementById('attendance-entry-time-in').value,
        timeOut: document.getElementById('attendance-entry-time-out').value
      })
    });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    setWorkflowStatus('attendance-entry-status', `Created synthetic attendance ${payload.item.attendanceId} with fixed DEMO label. No hours or pay were calculated.`);
    attendanceEmployee.value = employeeId;
    attendanceFrom.value = '';
    attendanceTo.value = '';
    attendanceCalendarEmployee = employeeId;
    attendanceCalendarMonth = new Date(Number(attendanceDate.slice(0, 4)), Number(attendanceDate.slice(5, 7)) - 1, 1);
    attendanceCalendarSelectionStart = '';
    attendanceCalendarSelectionEnd = '';
    document.getElementById('attendance-calendar-selection-status').textContent = 'Choose a start and end date to filter raw rows.';
    loadAttendance();
  } catch (error) {
    setWorkflowStatus('attendance-entry-status', error.message || 'Could not add the raw attendance row.', true);
  } finally {
    button.disabled = false;
  }
});
document.getElementById('attendance-sort').addEventListener('change', () => renderAttendance(state.rawAttendance));
document.getElementById('attendance-order').addEventListener('change', () => renderAttendance(state.rawAttendance));
document.getElementById('attendance-calendar-prev').addEventListener('click', () => {
  attendanceCalendarMonth = new Date(attendanceCalendarMonth.getFullYear(), attendanceCalendarMonth.getMonth() - 1, 1);
  renderAttendanceCalendar(state.rawAttendance);
});
document.getElementById('attendance-calendar-next').addEventListener('click', () => {
  attendanceCalendarMonth = new Date(attendanceCalendarMonth.getFullYear(), attendanceCalendarMonth.getMonth() + 1, 1);
  renderAttendanceCalendar(state.rawAttendance);
});
document.getElementById('attendance-report-filter').addEventListener('submit', event => {
  event.preventDefault();
  loadAttendanceReport();
});
document.getElementById('attendance-report-search').addEventListener('input', applyAttendanceReportFilters);
document.getElementById('attendance-report-mapping').addEventListener('change', applyAttendanceReportFilters);
document.getElementById('attendance-report-sort').addEventListener('change', applyAttendanceReportFilters);
document.getElementById('attendance-report-order').addEventListener('change', applyAttendanceReportFilters);
document.getElementById('attendance-report-rows').addEventListener('click', event => {
  const button = event.target.closest('button[data-employee-id]');
  if (!button) return;
  attendanceEmployee.value = button.dataset.employeeId;
  attendanceFrom.value = document.getElementById('attendance-report-from').value;
  attendanceTo.value = document.getElementById('attendance-report-to').value;
  switchView('attendance');
});
attendanceEmployee.addEventListener('change', () => {
  attendanceFrom.value = '';
  attendanceTo.value = '';
  attendanceCalendarSelectionStart = '';
  attendanceCalendarSelectionEnd = '';
  document.getElementById('attendance-calendar-selection-status').textContent = 'Choose a start and end date to filter raw rows.';
  loadAttendance();
});
document.getElementById('close-dialog').addEventListener('click', () => dialog.close());
document.getElementById('dialog-done').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => {
  if (event.target === dialog) dialog.close();
});
document.addEventListener('keydown', event => {
  if (event.key === '/' && !['INPUT', 'TEXTAREA'].includes(document.activeElement.tagName) && !dialog.open) {
    event.preventDefault();
    search.focus();
  }
});

function switchView(view) {
  const labels = { overview: 'Overview', employees: 'Employees', attendance: 'Attendance', requests: 'Requests & tickets', team: 'Team roster', hr: 'HR operations', reports: 'Reports', payroll: 'Payroll', config: 'System configuration' };
  if (!labels[view]) return;
  document.querySelectorAll('.page-view').forEach(page => {
    page.hidden = page.id !== `${view}-view`;
  });
  document.querySelectorAll('.nav-item').forEach(nav => {
    const active = nav.dataset.view === view;
    nav.classList.toggle('active', active);
    if (active) nav.setAttribute('aria-current', 'page');
    else nav.removeAttribute('aria-current');
  });
  const navLabel = document.querySelector(`.nav-item[data-view="${view}"] span[data-default-label]`);
  document.getElementById('crumb-current').textContent = navLabel ? navLabel.textContent : labels[view];
  document.title = `${labels[view]} — MotorPH Payroll Office`;
  if (view === 'attendance' && attendanceEmployee.value) loadAttendance();
  if (view === 'reports') loadAttendanceReport();
  if (view === 'team') loadTeamToday();
  pageTabs.refresh();
  notificationsUi.refresh();
}

document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', () => switchView(item.dataset.view));
});
document.getElementById('leave-calendar-span').addEventListener('change', renderLeaveCalendar);
document.querySelectorAll('.tab-card').forEach(card => {
  card.addEventListener('click', () => switchView(card.dataset.openView));
});

/** A short navy splash with the logo after an explicit sign-in (not on reload). It never blocks clicks; CSS skips motion if reduced. */
function playSignInSplash(account) {
  const splash = document.getElementById('signin-splash');
  document.getElementById('signin-splash-welcome').textContent = account ? `Welcome, ${account.name.split(' ')[0]}` : 'Welcome';
  splash.hidden = true;
  void splash.offsetWidth; // restart the CSS animation when it plays again
  splash.hidden = false;
  window.setTimeout(() => { splash.hidden = true; }, 1900);
}

const accountGate = document.getElementById('account-gate');

async function refreshBranchAndAccount(account) {
  document.getElementById('branch-panel').hidden = false;
  document.getElementById('account-panel').hidden = false;
  document.getElementById('account-password-label').textContent = state.defaultPassword ? 'USING THE DEMO PASSWORD' : '';
  payrollJson(`/api/personal/${encodeURIComponent(account.employeeId)}`).then(record => {
    document.getElementById('account-personal').textContent = record.address
      ? `${record.firstName} ${record.surname} · ${record.gender || 'gender not recorded'} · Birthdate ${record.birthdate} · Contact ${record.contactNumber || '—'}`
        + `${record.email ? ` · ${record.email}` : ''} · Address: ${record.address} (HR keeps these up to date).`
      : 'HR has not recorded your address, contact number and birthdate yet.';
    const photo = document.getElementById('account-photo');
    photo.hidden = true;
    if (record.hasPhoto) {
      apiHeaders({ Accept: 'image/*' }).then(headers => fetch(`/api/personal/${encodeURIComponent(account.employeeId)}/photo`, { headers }))
        .then(response => (response.ok ? response.blob() : null)).then(blob => { if (blob) { photo.src = URL.createObjectURL(blob); photo.hidden = false; } });
    }
  }).catch(() => {});
  if (account.branch) document.getElementById('operator-label').textContent = `${account.name} · ${account.roleLabel} · ${account.branch} branch`;
  try {
    const payload = await payrollJson('/api/branches');
    document.getElementById('branch-title').textContent = payload.branches.length > 1 ? 'Branches' : `${payload.mine} branch`;
    document.getElementById('branch-date').textContent = `TODAY · ${payload.today}`;
    const tbody = document.getElementById('branch-rows');
    tbody.replaceChildren();
    branchRows(payload).forEach(values => {
      const row = document.createElement('tr');
      values.forEach(value => row.append(makeCell(value)));
      tbody.append(row);
    });
  } catch (error) {
    document.getElementById('branch-date').textContent = error.message;
  }
}

document.getElementById('change-password-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const status = document.getElementById('change-password-status');
  try {
    await postAuth('/api/auth/password', { current: document.getElementById('password-current').value, next: document.getElementById('password-next').value }, state.session);
    state.defaultPassword = false;
    document.getElementById('account-password-label').textContent = '';
    form.reset();
    status.textContent = 'Password changed. Other sign-ins of this account were signed out.';
  } catch (error) {
    status.textContent = error.message;
  }
});

function enterAs(account) {
  state.account = account;
  sessionStorage.setItem(ACCOUNT_KEY, account.employeeId);
  accountGate.hidden = true;
  applyRoleLayout(document, account);
  showWorkspace(state.user);
  document.getElementById('operator-label').textContent = `${account.name} · ${account.roleLabel}`;
  document.getElementById('operator-avatar').textContent = account.name.split(' ').map(part => part[0]).join('').slice(0, 2);
  document.getElementById('switch-account-button').hidden = false;
  myDay.refresh();
  if (!firebaseConfigured) outbox.refresh();
  if (!firebaseConfigured) refreshBranchAndAccount(account);
  if (!firebaseConfigured) notificationsUi.refresh();
  if (['PAYROLL', 'HR', 'ADMIN', 'SUPERVISOR'].includes(account.role)) payrollProcessing.refresh();
  switchView(defaultView(account));
}

/** Firebase mode: the signed-in user picks which fake person in their own sandbox to act as. */
async function seedFromChooser(button) {
  button.disabled = true;
  button.textContent = 'Adding synthetic people…';
  try {
    const response = await fetch('/api/demo/seed', { method: 'POST', headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
    await startAccountChooser();
  } catch (error) {
    const message = document.getElementById('account-error');
    message.textContent = error instanceof Error ? error.message : 'Could not add the synthetic people.';
    message.hidden = false;
    button.disabled = false;
    button.textContent = 'Add synthetic demo people';
  }
}

/** Local demo 2FA: the server "texts" a code (shown here, since there is no phone); the right code gives a session. */
function askPassword(account) {
  const form = document.getElementById('password-form');
  document.getElementById('account-error').hidden = true;
  document.getElementById('account-list').hidden = true;
  form.hidden = false;
  form.dataset.employeeId = account.employeeId;
  document.getElementById('password-who').textContent = `${account.name} (${account.roleLabel})`;
  document.getElementById('signin-password').value = '';
  document.getElementById('signin-password').focus();
}

document.getElementById('password-back').addEventListener('click', () => showAccountList());
document.getElementById('password-form').addEventListener('submit', event => {
  event.preventDefault();
  startTwoFactor(accountFrom(state.accounts, event.currentTarget.dataset.employeeId), document.getElementById('signin-password').value);
});

async function startTwoFactor(account, password) {
  const form = document.getElementById('twofa-form');
  const message = document.getElementById('account-error');
  message.hidden = true;
  try {
    const challenge = await postAuth('/api/auth/2fa/start', { employeeId: account.employeeId, password });
    document.getElementById('account-list').hidden = true;
    document.getElementById('password-form').hidden = true;
    form.hidden = false;
    form.dataset.challengeId = challenge.challengeId;
    form.dataset.employeeId = account.employeeId;
    document.getElementById('twofa-sent').textContent = `${account.name}: we sent a 6-digit code to ${challenge.sentTo}. It expires at ${challenge.expiresAt}.`;
    document.getElementById('twofa-demo').textContent = `Demo only (there is no phone): your code is ${challenge.demoCode}.`;
    document.getElementById('twofa-code').value = '';
    document.getElementById('twofa-code').focus();
  } catch (error) {
    message.textContent = error.message;
    message.hidden = false;
  }
}

async function postAuth(url, body, session = '') {
  const headers = { Accept: 'application/json', 'Content-Type': 'application/json' };
  if (session) headers['X-Demo-Session'] = session;
  const response = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) });
  const payload = await response.json();
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status})`);
  return payload;
}

function showAccountList() {
  document.getElementById('twofa-form').hidden = true;
  document.getElementById('password-form').hidden = true;
  document.getElementById('account-list').hidden = false;
}

document.getElementById('twofa-back').addEventListener('click', showAccountList);
document.getElementById('twofa-form').addEventListener('submit', async event => {
  event.preventDefault();
  const form = event.currentTarget;
  const message = document.getElementById('account-error');
  try {
    const { session, defaultPassword } = await postAuth('/api/auth/2fa/verify', { challengeId: form.dataset.challengeId, code: document.getElementById('twofa-code').value });
    state.session = session;
    state.defaultPassword = defaultPassword;
    sessionStorage.setItem(SESSION_KEY, session);
    showAccountList();
    const signedIn = accountFrom(state.accounts, form.dataset.employeeId);
    playSignInSplash(signedIn);
    enterAs(signedIn);
  } catch (error) {
    message.textContent = error.message;
    message.hidden = false;
    if (!/tries left/.test(error.message)) showAccountList();
  }
});

async function savedSessionFor(employeeId) {
  const session = sessionStorage.getItem(SESSION_KEY) || '';
  if (!session) return '';
  const response = await fetch('/api/auth/session', { headers: { Accept: 'application/json', 'X-Demo-Session': session } });
  const payload = await response.json().catch(() => ({}));
  return response.ok && payload.employeeId === employeeId ? session : '';
}

async function startAccountChooser() {
  const list = document.getElementById('account-list');
  state.account = null;
  document.getElementById('account-error').hidden = true;
  if (firebaseConfigured) {
    document.getElementById('account-sign-out').hidden = false;
    document.getElementById('account-intro').textContent = 'Your private sandbox: choose which fake person to act as. Each opens the workspace for that role, and the server checks every permission. Only you can see this data.';
  }
  try {
    const response = await fetch('/api/demo/accounts', { headers: await apiHeaders() });
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || 'Could not load demo accounts.');
    state.accounts = payload.accounts;
    const saved = accountFrom(payload.accounts, sessionStorage.getItem(ACCOUNT_KEY) || '');
    // Local demo: a reload keeps you signed in only while the 2FA session is still valid (a server restart ends it).
    const session = saved && !firebaseConfigured ? await savedSessionFor(saved.employeeId) : '';
    if (saved && (firebaseConfigured || session)) {
      state.session = session;
      enterAs(saved);
      return;
    }
    sessionStorage.removeItem(SESSION_KEY);
    list.replaceChildren();
    payload.accounts.forEach(item => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'account-option';
      button.setAttribute('aria-label', `Sign in as ${item.name} (${item.roleLabel})`);
      const name = document.createElement('strong');
      name.textContent = item.name;
      const detail = document.createElement('span');
      detail.textContent = `${item.roleLabel} · ${item.position}`;
      button.append(name, detail);
      // Firebase mode already signed in with Firebase; the local demo asks for the 2FA code first.
      button.addEventListener('click', () => {
        if (!firebaseConfigured) return askPassword(item);
        playSignInSplash(item);
        enterAs(accountFrom(payload.accounts, item.employeeId));
      });
      list.append(button);
    });
    if (!payload.accounts.length) {
      const seed = document.createElement('button');
      seed.type = 'button';
      seed.className = 'button button-dark';
      seed.textContent = 'Add synthetic demo people';
      seed.addEventListener('click', () => seedFromChooser(seed));
      const open = document.createElement('button');
      open.type = 'button';
      open.className = 'button button-quiet';
      open.textContent = 'Open the empty sandbox';
      open.addEventListener('click', () => {
        accountGate.hidden = true;
        applyRoleLayout(document, null);
        showWorkspace(state.user);
      });
      const note = document.createElement('p');
      note.className = 'auth-intro';
      note.textContent = 'Your sandbox has no people yet.';
      list.append(note, seed, open);
    }
    appShell.hidden = true;
    authGate.hidden = true;
    accountGate.hidden = false;
  } catch (error) {
    const message = document.getElementById('account-error');
    message.textContent = error instanceof Error ? error.message : 'Could not load demo accounts.';
    message.hidden = false;
    accountGate.hidden = false;
  }
}

document.getElementById('switch-account-button').addEventListener('click', async () => {
  sessionStorage.removeItem(ACCOUNT_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  if (state.session) await postAuth('/api/auth/signout', {}, state.session).catch(() => {});
  state.session = '';
  // A reload would end the Firebase session (in-memory persistence), so Firebase mode reopens the chooser instead.
  if (firebaseConfigured) startAccountChooser();
  else window.location.reload();
});

if (!firebaseConfigured) {
  startAccountChooser();
} else {
  document.getElementById('auth-form').addEventListener('submit', event => {
    event.preventDefault();
    signIn();
  });
  createAccountButton.addEventListener('click', createDevAccount);
  document.getElementById('password-reset-button').addEventListener('click', sendPasswordReset);
  ['sign-out-button', 'account-sign-out'].forEach(id => document.getElementById(id).addEventListener('click', async () => {
    try { await signOut(auth); } catch (error) { console.error('Firebase sign-out failed', error); }
  }));
  setPersistence(auth, inMemoryPersistence)
    .then(() => onAuthStateChanged(auth, user => {
      if (user) {
        state.user = user;
        startAccountChooser();
      } else showSignIn();
    }))
    .catch(error => showSignIn(friendlyAuthError(error)));
}
