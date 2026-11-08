import test from 'node:test';
import assert from 'node:assert/strict';

const moduleOrError = await import('../../web/workflow-demo.js').catch(error => ({ importError: error }));
const employees = [
  { id: '10001', name: 'Manuel III Garcia' },
  { id: '10002', name: 'Antonio Lim' }
];

function api() {
  assert.ok(!moduleOrError.importError, 'workflow demo module should load');
  for (const name of ['createWorkflowState', 'submitLeaveRequest', 'cancelLeaveRequest', 'submitTicket', 'cancelTicket']) {
    assert.equal(typeof moduleOrError[name], 'function', `${name} should be exported`);
  }
  return moduleOrError;
}

test('creates a pending synthetic leave request without mutating prior state', () => {
  const { createWorkflowState, submitLeaveRequest } = api();
  const initial = createWorkflowState();
  const result = submitLeaveRequest(initial, {
    employeeId: '10001', leaveType: 'Vacation', startDate: '2026-11-10', endDate: '2026-11-11', note: 'Fictional request'
  }, employees, '2026-11-03');

  assert.equal(initial.leaveRequests.length, 0);
  assert.equal(result.state.leaveRequests.length, 1);
  assert.equal(result.request.employeeName, 'Manuel III Garcia');
  assert.equal(result.request.status, 'Pending review');
  assert.equal(result.request.startDate, '2026-11-10');
});

test('rejects reversed or impossible leave dates', () => {
  const { createWorkflowState, submitLeaveRequest } = api();
  const initial = createWorkflowState();
  assert.throws(() => submitLeaveRequest(initial, {
    employeeId: '10001', leaveType: 'Vacation', startDate: '2026-11-12', endDate: '2026-11-11', note: ''
  }, employees), /start date/i);
  assert.throws(() => submitLeaveRequest(initial, {
    employeeId: '10001', leaveType: 'Vacation', startDate: '2026-02-30', endDate: '2026-03-01', note: ''
  }, employees), /valid date/i);
});

test('cancels only the requesting employee’s pending leave request', () => {
  const { createWorkflowState, submitLeaveRequest, cancelLeaveRequest } = api();
  const initial = createWorkflowState();
  const { state, request } = submitLeaveRequest(initial, {
    employeeId: '10001', leaveType: 'Medical', startDate: '2026-11-12', endDate: '2026-11-12', note: ''
  }, employees, '2026-11-03');

  assert.throws(() => cancelLeaveRequest(state, request.id, '10002'), /own request/i);
  const cancelled = cancelLeaveRequest(state, request.id, '10001');
  assert.equal(state.leaveRequests[0].status, 'Pending review');
  assert.equal(cancelled.leaveRequests[0].status, 'Cancelled');
  assert.throws(() => cancelLeaveRequest(cancelled, request.id, '10001'), /pending/i);
});

test('rejects unknown employees, leave types, and overlong notes', () => {
  const { createWorkflowState, submitLeaveRequest } = api();
  const state = createWorkflowState();
  const common = { leaveType: 'Vacation', startDate: '2026-11-10', endDate: '2026-11-10', note: '' };
  assert.throws(() => submitLeaveRequest(state, { ...common, employeeId: 'REAL-EMPLOYEE' }, employees), /synthetic demo directory/i);
  assert.throws(() => submitLeaveRequest(state, { ...common, employeeId: '10001', leaveType: 'Unapproved type' }, employees), /leave type/i);
  assert.throws(() => submitLeaveRequest(state, { ...common, employeeId: '10001', note: 'x'.repeat(241) }, employees), /240 characters/i);
});

test('routes each synthetic ticket type to its documented queue label', () => {
  const { createWorkflowState, submitTicket } = api();
  const expected = {
    'Overtime request': 'Team Lead',
    'Leave request': 'Team Lead',
    'Timesheet correction': 'Team Lead',
    'Payroll inquiry': 'Payroll Staff',
    'HR inquiry': 'HR',
    'Technical support': 'IT Admin'
  };
  Object.entries(expected).forEach(([ticketType, queue]) => {
    const result = submitTicket(createWorkflowState(), {
      employeeId: '10002', ticketType, summary: 'Review a fictional record'
    }, employees, '2026-11-03');
    assert.equal(result.ticket.employeeName, 'Antonio Lim');
    assert.equal(result.ticket.queue, queue);
    assert.equal(result.ticket.status, 'Open');
  });
});

test('rejects invalid ticket types or empty messages', () => {
  const { createWorkflowState, submitTicket } = api();
  const state = createWorkflowState();
  assert.throws(() => submitTicket(state, {
    employeeId: '10001', ticketType: 'Payroll payout', summary: 'Do a calculation'
  }, employees), /ticket type/i);
  assert.throws(() => submitTicket(state, {
    employeeId: '10001', ticketType: 'HR inquiry', summary: '   '
  }, employees), /short message/i);
  assert.throws(() => submitTicket(state, {
    employeeId: '10001', ticketType: 'HR inquiry', summary: 'x'.repeat(241)
  }, employees), /240 characters/i);
});

test('prevents another employee cancelling a ticket and permits own open-ticket cancellation', () => {
  const { createWorkflowState, submitTicket, cancelTicket } = api();
  const initial = createWorkflowState();
  const { state, ticket } = submitTicket(initial, {
    employeeId: '10001', ticketType: 'HR inquiry', summary: 'Synthetic test'
  }, employees, '2026-11-03');
  assert.throws(() => cancelTicket(state, ticket.id, '10002'), /own ticket/i);
  const cancelled = cancelTicket(state, ticket.id, '10001');
  assert.equal(state.tickets[0].status, 'Open');
  assert.equal(cancelled.tickets[0].status, 'Cancelled');
  assert.throws(() => cancelTicket(cancelled, ticket.id, '10001'), /open ticket/i);
});
