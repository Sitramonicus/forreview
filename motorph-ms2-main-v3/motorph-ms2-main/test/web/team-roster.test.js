import test from 'node:test';
import assert from 'node:assert/strict';
import { availabilityFor, contactFor, leaveEventText } from '../../web/b_basic/TeamRoster.js';

test('synthetic contact info uses the reserved example domain', () => {
  assert.deepEqual(contactFor({ id: 'DEMO-004', name: 'Jae Cruz' }), { email: 'jae.cruz@motorph.example', extension: 'local 004' });
  assert.equal(contactFor({ id: 'DEMO-0A1B2C3D4E5F', name: 'Ana Peña-Reyes' }).email, 'ana.penareyes@motorph.example');
});

test('availability follows approved leave first, then the stored status', () => {
  const leaves = [
    { employeeId: 'DEMO-004', status: 'Approved', leaveType: 'Vacation', startDate: '2026-11-05', endDate: '2026-11-06' },
    { employeeId: 'DEMO-003', status: 'Pending review', leaveType: 'Medical', startDate: '2026-11-06', endDate: '2026-11-06' },
    { employeeId: 'DEMO-003', status: 'Approved', leaveType: 'Personal', startDate: '2026-11-20', endDate: '2026-11-20' }
  ];
  assert.equal(availabilityFor({ id: 'DEMO-004', status: 'Active' }, leaves, '2026-11-06'), 'On leave today (Vacation, until 2026-11-06)');
  assert.equal(availabilityFor({ id: 'DEMO-003', status: 'Active' }, leaves, '2026-11-06'), 'Available · leave from 2026-11-20', 'pending leave does not count');
  assert.equal(availabilityFor({ id: 'DEMO-002', status: 'On leave' }, leaves, '2026-11-06'), 'On leave');
  assert.equal(availabilityFor({ id: 'DEMO-009', status: 'Separated' }, leaves, '2026-11-06'), 'Separated');
});

test('calendar markers name the person', () => {
  assert.equal(leaveEventText({ employeeName: 'Jae Cruz', leaveType: 'Vacation', status: 'Approved' }), 'Jae · Vacation');
  assert.equal(leaveEventText({ employeeName: 'Noa Reyes', leaveType: 'Medical', status: 'Pending review' }), 'Noa · Medical (pending review)');
});
