import test from 'node:test';
import assert from 'node:assert/strict';
import { accountFrom, canCancel, canFinalize, canPickFor, canReview, defaultView, viewsFor } from '../../web/roles.js';

// Shaped like GET /api/demo/accounts.
const accounts = [
  { employeeId: 'DEMO-001', name: 'Mara Rivera', role: 'HR', roleLabel: 'HR', supervisorId: 'DEMO-003', team: ['DEMO-001', 'DEMO-003', 'DEMO-002', 'DEMO-004'] },
  { employeeId: 'DEMO-002', name: 'Eli Santos', role: 'PAYROLL', roleLabel: 'Payroll Staff', supervisorId: 'DEMO-003', team: ['DEMO-002', 'DEMO-003', 'DEMO-001', 'DEMO-004'] },
  { employeeId: 'DEMO-003', name: 'Noa Reyes', role: 'SUPERVISOR', roleLabel: 'Supervisor', supervisorId: '', team: ['DEMO-003', 'DEMO-001', 'DEMO-002', 'DEMO-004'] },
  { employeeId: 'DEMO-004', name: 'Jae Cruz', role: 'EMPLOYEE', roleLabel: 'Employee', supervisorId: 'DEMO-003', team: ['DEMO-004', 'DEMO-003', 'DEMO-001', 'DEMO-002'] },
  { employeeId: 'DEMO-005', name: 'Lia Torres', role: 'ADMIN', roleLabel: 'System Admin', supervisorId: '', team: ['DEMO-005'] }
];
const as = id => accountFrom(accounts, id);

test('each role gets its own menu', () => {
  assert.deepEqual(viewsFor(as('DEMO-004')), ['overview', 'attendance', 'requests', 'team', 'payroll']);
  assert.ok(!viewsFor(as('DEMO-003')).includes('employees'));
  assert.ok(viewsFor(as('DEMO-002')).includes('reports') && !viewsFor(as('DEMO-002')).includes('hr'));
  assert.ok(viewsFor(as('DEMO-001')).includes('hr'));
  assert.equal(viewsFor(as('DEMO-005')).length, 9);
  assert.ok(viewsFor(as('DEMO-005')).includes('config') && !viewsFor(as('DEMO-001')).includes('config'), 'only the admin gets System config');
  assert.ok(!viewsFor(null).includes('config'), 'Firebase mode has no System config');
  assert.equal(defaultView(as('DEMO-004')), 'overview');
  assert.equal(defaultView(as('DEMO-001')), 'overview', 'every role lands on the branch page');
  assert.equal(defaultView(null), 'employees');
});

test('pickers offer only the people the role may act for', () => {
  assert.ok(canPickFor(as('DEMO-004'), 'leave', 'DEMO-004') && !canPickFor(as('DEMO-004'), 'leave', 'DEMO-001'));
  assert.ok(canPickFor(as('DEMO-001'), 'leave', 'DEMO-004'), 'HR files leave for anyone');
  assert.ok(canPickFor(as('DEMO-003'), 'payroll', 'DEMO-004') && !canPickFor(as('DEMO-003'), 'payroll', 'DEMO-005'));
  assert.ok(canPickFor(as('DEMO-003'), 'attendance-edit', 'DEMO-004') && !canPickFor(as('DEMO-003'), 'attendance-edit', 'DEMO-003'));
  assert.ok(!canPickFor(as('DEMO-001'), 'attendance-edit', 'DEMO-004'), 'HR only reads attendance');
  assert.ok(canPickFor(null, 'payroll', 'DEMO-005'), 'Firebase mode has no roles');
});

test('reviews follow the matrix and never allow your own request', () => {
  assert.equal(canReview(as('DEMO-003'), 'leave', 'DEMO-004'), true);
  assert.equal(canReview(as('DEMO-001'), 'leave', 'DEMO-004'), true);
  assert.equal(canReview(as('DEMO-001'), 'overtime', 'DEMO-004'), false);
  assert.equal(canReview(as('DEMO-002'), 'leave', 'DEMO-004'), false);
  assert.equal(canReview(as('DEMO-003'), 'leave', 'DEMO-003'), false);
  assert.equal(canReview(as('DEMO-005'), 'overtime', 'DEMO-004'), true);
  assert.equal(canCancel(as('DEMO-004'), 'leave', 'DEMO-001'), false);
  assert.equal(canCancel(as('DEMO-001'), 'leave', 'DEMO-004'), true);
  assert.equal(canFinalize(as('DEMO-002'), 'DEMO-004'), true);
  assert.equal(canFinalize(as('DEMO-002'), 'DEMO-002'), false);
  assert.equal(canFinalize(as('DEMO-001'), 'DEMO-004'), false);
});

test('accountFrom adds direct reports and rejects unknown IDs', () => {
  assert.deepEqual(as('DEMO-003').reports, ['DEMO-001', 'DEMO-002', 'DEMO-004']);
  assert.equal(accountFrom(accounts, 'DEMO-999'), null);
});

test('supervisors pick only their direct reports for earnings', () => {
  const noa = { employeeId: 'DEMO-003', role: 'SUPERVISOR', team: ['DEMO-001', 'DEMO-002', 'DEMO-004'], reports: ['DEMO-001', 'DEMO-002', 'DEMO-004'] };
  assert.equal(canPickFor(noa, 'processing', 'DEMO-004'), true);
  assert.equal(canPickFor(noa, 'processing', 'DEMO-003'), false);
  assert.equal(canPickFor(noa, 'processing', 'DEMO-005'), false);
});

test('tickets go to their handler, never to the person who filed them', async () => {
  const { canHandleTicket } = await import('../../web/roles.js');
  const noa = { employeeId: 'DEMO-003', role: 'SUPERVISOR', reports: ['DEMO-004'] };
  const eli = { employeeId: 'DEMO-002', role: 'PAYROLL', reports: [] };
  const lia = { employeeId: 'DEMO-005', role: 'ADMIN', reports: [] };
  assert.equal(canHandleTicket(noa, { employeeId: 'DEMO-004', queue: 'Team Lead' }), true);
  assert.equal(canHandleTicket(noa, { employeeId: 'DEMO-005', queue: 'Team Lead' }), false, 'only for direct reports');
  assert.equal(canHandleTicket(eli, { employeeId: 'DEMO-004', queue: 'Payroll Staff' }), true);
  assert.equal(canHandleTicket(eli, { employeeId: 'DEMO-002', queue: 'Payroll Staff' }), false, 'not your own');
  assert.equal(canHandleTicket(eli, { employeeId: 'DEMO-004', queue: 'HR' }), false);
  assert.equal(canHandleTicket(lia, { employeeId: 'DEMO-004', queue: 'IT Admin' }), true);
});
