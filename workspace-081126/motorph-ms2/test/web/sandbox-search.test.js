import test from 'node:test';
import assert from 'node:assert/strict';
import { searchSandboxRecords } from '../../web/sandbox-search.js';

const data = {
  employees: [{ id: '10001', name: 'Manuel III Garcia', department: 'Finance', role: 'Analyst', status: 'Active' }],
  attendanceByEmployee: {
    '10001': [{ attendanceId: 5001, scheduleId: 101, date: '2026-10-01', timeIn: '08:01', timeOut: '17:00', dayType: 'DEMO' }]
  },
  leaveRequests: [{ id: 'LV-1', employeeId: '10001', employeeName: 'Manuel III Garcia', leaveType: 'Personal', startDate: '2026-11-10', endDate: '2026-11-10', status: 'Pending review', note: 'Sample note' }],
  tickets: [{ id: 'TK-1', employeeId: '10002', employeeName: 'Jonas Demo', ticketType: 'Technical support', queue: 'IT Admin', summary: 'Sample issue', status: 'Open' }]
};

test('searches case-insensitively across selected employee and linked record fields', () => {
  const results = searchSandboxRecords('10001', data);
  assert.equal(results.employees.length, 1);
  assert.equal(results.attendance.length, 1);
  assert.equal(results.leaveRequests.length, 1);
  assert.equal(results.tickets.length, 0);
});

test('finds raw times and stored request/ticket labels without calculating or interpreting them', () => {
  assert.equal(searchSandboxRecords('08:01', data).attendance.length, 1);
  assert.equal(searchSandboxRecords('Pending review', data).leaveRequests.length, 1);
  assert.equal(searchSandboxRecords('IT Admin', data).tickets.length, 1);
});

test('returns no results for a blank query and safely handles malformed collections', () => {
  assert.deepEqual(searchSandboxRecords('   ', data), { employees: [], attendance: [], leaveRequests: [], tickets: [] });
  assert.deepEqual(searchSandboxRecords('x', { employees: null, attendanceByEmployee: [], leaveRequests: {}, tickets: null }),
    { employees: [], attendance: [], leaveRequests: [], tickets: [] });
});
