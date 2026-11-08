import test from 'node:test';
import assert from 'node:assert/strict';
import { filterAttendanceCoverage } from '../../web/attendance-coverage-filter.js';

const rows = [
  { employeeId: '10001', employeeName: 'Demo One', scheduleId: 101, hasScheduleMapping: true },
  { employeeId: '10002', employeeName: 'Demo Two', scheduleId: 102, hasScheduleMapping: true },
  { employeeId: 'DEMO-004', employeeName: 'Unmapped Sample', scheduleId: null, hasScheduleMapping: false }
];

test('filters attendance coverage by mapped status and case-insensitive employee search', () => {
  assert.deepEqual(filterAttendanceCoverage(rows, { mapping: 'mapped' }).map(row => row.employeeId), ['10001', '10002']);
  assert.deepEqual(filterAttendanceCoverage(rows, { mapping: 'unmapped' }).map(row => row.employeeId), ['DEMO-004']);
  assert.deepEqual(filterAttendanceCoverage(rows, { query: 'demo two' }).map(row => row.employeeId), ['10002']);
  assert.deepEqual(filterAttendanceCoverage(rows, { query: 'schedule 101' }).map(row => row.employeeId), ['10001']);
});

test('safely handles empty and malformed inputs and rejects unknown mapping filters', () => {
  assert.deepEqual(filterAttendanceCoverage(null), []);
  assert.deepEqual(filterAttendanceCoverage([null, {}]), []);
  assert.deepEqual(filterAttendanceCoverage(rows, { mapping: 'unknown' }), []);
});
