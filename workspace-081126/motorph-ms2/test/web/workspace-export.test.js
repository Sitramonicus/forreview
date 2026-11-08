import test from 'node:test';
import assert from 'node:assert/strict';
import { buildWorkspaceExport } from '../../web/workspace-export.js';

const date = new Date('2026-11-04T02:00:00.000Z');

test('builds a synthetic sandbox bundle with employee-grouped raw rows and stored workflows', () => {
  const employees = [{ id: '10001', name: 'Manuel III Garcia' }];
  const leaveRequests = [{ id: 'LV-1', employeeId: '10001', status: 'Pending review' }];
  const tickets = [{ id: 'TK-1', employeeId: '10001', status: 'Open' }];
  const bundle = buildWorkspaceExport({
    mode: 'firebase-user-sandbox',
    employees,
    leaveRequests,
    tickets,
    attendanceByEmployee: {
      '10001': [{ attendanceId: 5001, employeeId: 'UNTRUSTED', date: '2026-10-01', timeIn: '08:01', timeOut: '17:00', dayType: 'DEMO' }],
      'DEMO-NEW': []
    }
  }, date);

  assert.equal(bundle.formatVersion, 1);
  assert.equal(bundle.exportedAt, '2026-11-04T02:00:00.000Z');
  assert.equal(bundle.storageMode, 'firebase-user-sandbox');
  assert.equal(bundle.syntheticOnly, true);
  assert.equal(bundle.hoursCalculated, false);
  assert.equal(bundle.payrollIncluded, false);
  assert.deepEqual(bundle.employees, employees);
  assert.deepEqual(bundle.attendance, [{ employeeId: '10001', attendanceId: 5001, date: '2026-10-01', timeIn: '08:01', timeOut: '17:00', dayType: 'DEMO' }]);
  assert.deepEqual(bundle.leaveRequests, leaveRequests);
  assert.deepEqual(bundle.tickets, tickets);
  assert.ok(!JSON.stringify(bundle).includes('netPay'));
});

test('returns empty arrays for missing data without inventing rows', () => {
  const bundle = buildWorkspaceExport({}, date);
  assert.deepEqual(bundle.employees, []);
  assert.deepEqual(bundle.attendance, []);
  assert.deepEqual(bundle.leaveRequests, []);
  assert.deepEqual(bundle.tickets, []);
  assert.equal(bundle.syntheticOnly, true);
});

test('rejects malformed collections, attendance groups, and timestamps', () => {
  assert.throws(() => buildWorkspaceExport({ employees: {} }, date), /collections must be arrays/);
  assert.throws(() => buildWorkspaceExport({ attendanceByEmployee: [] }, date), /grouped by employee ID/);
  assert.throws(() => buildWorkspaceExport({ attendanceByEmployee: { '10001': {} } }, date), /must be an array/);
  assert.throws(() => buildWorkspaceExport({ attendanceByEmployee: { '10001': [null] } }, date), /must be an object/);
  assert.throws(() => buildWorkspaceExport({}, '2026-11-04'), /valid export timestamp/);
});
