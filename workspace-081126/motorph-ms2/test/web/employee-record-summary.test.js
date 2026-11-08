import test from 'node:test';
import assert from 'node:assert/strict';
import { employeeRecordSummary } from '../../web/employee-record-summary.js';

const employees = [
  { id: '10001', name: 'Manuel III Garcia', department: 'People Operations' },
  { id: 'DEMO-NEW', name: 'Synthetic Person', department: 'Demo' }
];

test('joins raw attendance, leave, and ticket counts only by existing employee ID', () => {
  const rows = employeeRecordSummary(employees, [
    { employeeId: '10001', hasScheduleMapping: true, rawRecordCount: 2, firstRecordDate: '2026-10-01', lastRecordDate: '2026-10-02' },
    { employeeId: 'DEMO-NEW', hasScheduleMapping: false, rawRecordCount: 0, firstRecordDate: '', lastRecordDate: '' }
  ], [
    { employeeId: '10001' }, { employeeId: '10001' }
  ], [
    { employeeId: 'DEMO-NEW' }
  ]);
  assert.deepEqual(rows, [
    { employeeId: '10001', employeeName: 'Manuel III Garcia', department: 'People Operations', hasScheduleMapping: true, rawRecordCount: 2, firstRecordDate: '2026-10-01', lastRecordDate: '2026-10-02', leaveRequestCount: 2, ticketCount: 0 },
    { employeeId: 'DEMO-NEW', employeeName: 'Synthetic Person', department: 'Demo', hasScheduleMapping: false, rawRecordCount: 0, firstRecordDate: '', lastRecordDate: '', leaveRequestCount: 0, ticketCount: 1 }
  ]);
  assert.ok(!('attendanceStatus' in rows[1]) && !('hours' in rows[0]) && !('pay' in rows[0]));
});

test('includes employees with no linked rows and safely handles malformed optional collections', () => {
  assert.equal(employeeRecordSummary(employees, null, {}, null)[0].rawRecordCount, 0);
  assert.equal(employeeRecordSummary(employees, null, {}, null)[1].leaveRequestCount, 0);
  assert.deepEqual(employeeRecordSummary(null), []);
});
