const LEAVE_TYPES = Object.freeze(['Vacation', 'Medical', 'Personal', 'Unpaid']);

// Routes reflect the queues described in the legacy project materials. These are
// labels for a browser-only demo, not real authorization or delivery routing.
const TICKET_QUEUES = Object.freeze({
  'Overtime request': 'Team Lead',
  'Leave request': 'Team Lead',
  'Timesheet correction': 'Team Lead',
  'Payroll inquiry': 'Payroll Staff',
  'HR inquiry': 'HR',
  'Technical support': 'IT Admin'
});

const MAX_NOTE_LENGTH = 240;

export function createWorkflowState() {
  return {
    leaveRequests: [],
    tickets: [],
    nextLeaveNumber: 1,
    nextTicketNumber: 1
  };
}

function requireState(state) {
  if (!state || !Array.isArray(state.leaveRequests) || !Array.isArray(state.tickets)) {
    throw new TypeError('Workflow state is invalid.');
  }
}

function findEmployee(employeeId, employees) {
  const employee = Array.isArray(employees) && employees.find(item => item && item.id === employeeId);
  if (!employee) throw new Error('Choose an employee from the synthetic demo directory.');
  return employee;
}

function isValidDate(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const date = new Date(`${value}T00:00:00.000Z`);
  return !Number.isNaN(date.valueOf()) && date.toISOString().slice(0, 10) === value;
}

function cleanOptionalText(value, field) {
  const text = String(value ?? '').trim();
  if (text.length > MAX_NOTE_LENGTH) throw new Error(`${field} must be ${MAX_NOTE_LENGTH} characters or fewer.`);
  return text;
}

function dateStamp(value) {
  const date = value instanceof Date ? value : new Date(`${value}T00:00:00.000Z`);
  if (Number.isNaN(date.valueOf())) throw new Error('Submission date is invalid.');
  return date.toISOString().slice(0, 10);
}

export function submitLeaveRequest(state, input, employees, submittedAt = new Date()) {
  requireState(state);
  if (!input || !LEAVE_TYPES.includes(input.leaveType)) throw new Error('Choose a valid leave type.');
  const employee = findEmployee(input.employeeId, employees);
  if (!isValidDate(input.startDate) || !isValidDate(input.endDate)) throw new Error('Enter a valid date for both leave dates.');
  if (input.startDate > input.endDate) throw new Error('The start date must be on or before the end date.');
  const note = cleanOptionalText(input.note, 'Note');
  const request = {
    id: `LV-${String(state.nextLeaveNumber).padStart(4, '0')}`,
    employeeId: employee.id,
    employeeName: String(employee.name || employee.id),
    leaveType: input.leaveType,
    startDate: input.startDate,
    endDate: input.endDate,
    note,
    status: 'Pending review',
    submittedAt: dateStamp(submittedAt)
  };
  return {
    state: { ...state, leaveRequests: [...state.leaveRequests, request], nextLeaveNumber: state.nextLeaveNumber + 1 },
    request
  };
}

export function cancelLeaveRequest(state, requestId, employeeId) {
  requireState(state);
  const request = state.leaveRequests.find(item => item.id === requestId);
  if (!request) throw new Error('Leave request was not found.');
  if (request.employeeId !== employeeId) throw new Error('You can only cancel your own request.');
  if (request.status !== 'Pending review') throw new Error('Only a pending leave request can be cancelled.');
  const leaveRequests = state.leaveRequests.map(item => item.id === requestId ? { ...item, status: 'Cancelled' } : item);
  return { ...state, leaveRequests };
}

export function submitTicket(state, input, employees, submittedAt = new Date()) {
  requireState(state);
  const ticketType = input && input.ticketType;
  if (!Object.hasOwn(TICKET_QUEUES, ticketType)) throw new Error('Choose a valid ticket type.');
  const employee = findEmployee(input.employeeId, employees);
  const summary = cleanOptionalText(input.summary, 'Message');
  if (!summary) throw new Error('Add a short message before submitting the ticket.');
  const ticket = {
    id: `TK-${String(state.nextTicketNumber).padStart(4, '0')}`,
    employeeId: employee.id,
    employeeName: String(employee.name || employee.id),
    ticketType,
    queue: TICKET_QUEUES[ticketType],
    summary,
    status: 'Open',
    submittedAt: dateStamp(submittedAt)
  };
  return {
    state: { ...state, tickets: [...state.tickets, ticket], nextTicketNumber: state.nextTicketNumber + 1 },
    ticket
  };
}

export function cancelTicket(state, ticketId, employeeId) {
  requireState(state);
  const ticket = state.tickets.find(item => item.id === ticketId);
  if (!ticket) throw new Error('Ticket was not found.');
  if (ticket.employeeId !== employeeId) throw new Error('You can only cancel your own ticket.');
  if (ticket.status !== 'Open') throw new Error('Only an open ticket can be cancelled.');
  const tickets = state.tickets.map(item => item.id === ticketId ? { ...item, status: 'Cancelled' } : item);
  return { ...state, tickets };
}

export { LEAVE_TYPES, MAX_NOTE_LENGTH, TICKET_QUEUES };
