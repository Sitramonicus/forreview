import test from 'node:test';
import assert from 'node:assert/strict';
import { filterAndSortEmployeeRecordSummary } from '../../web/employee-record-summary-view.js';

const rows = [
  { employeeId: 'DEMO-002', employeeName: 'Mara Rivera', department: 'Finance', hasScheduleMapping: true, rawRecordCount: 2, leaveRequestCount: 0, ticketCount: 1 },
  { employeeId: 'DEMO-001', employeeName: 'Avery Sample', department: 'Operations', hasScheduleMapping: true, rawRecordCount: 10, leaveRequestCount: 2, ticketCount: 0 },
  { employeeId: 'DEMO-NEW', employeeName: 'Synthetic Person', department: 'Demo', hasScheduleMapping: false, rawRecordCount: 0, leaveRequestCount: 1, ticketCount: 0 }
];

test('filters by mapping and searches only employee display fields', () => {
  assert.deepEqual(filterAndSortEmployeeRecordSummary(rows, { mapping: 'unmapped' }).map(row => row.employeeId), ['DEMO-NEW']);
  assert.deepEqual(filterAndSortEmployeeRecordSummary(rows, { query: 'finance' }).map(row => row.employeeId), ['DEMO-002']);
});

test('sorts numeric counts and text fields without mutating the source array', () => {
  assert.deepEqual(filterAndSortEmployeeRecordSummary(rows, { sortBy: 'rawRecordCount', direction: 'desc' }).map(row => row.rawRecordCount), [10, 2, 0]);
  assert.deepEqual(filterAndSortEmployeeRecordSummary(rows, { sortBy: 'ticketCount' }).map(row => row.employeeId), ['DEMO-001', 'DEMO-NEW', 'DEMO-002']);
  assert.deepEqual(rows.map(row => row.employeeId), ['DEMO-002', 'DEMO-001', 'DEMO-NEW']);
});

test('safely falls back for malformed input or unsupported options', () => {
  assert.deepEqual(filterAndSortEmployeeRecordSummary(null), []);
  assert.deepEqual(filterAndSortEmployeeRecordSummary(rows, { mapping: 'absent', sortBy: 'hours', direction: 'up' }).map(row => row.employeeName), ['Avery Sample', 'Mara Rivera', 'Synthetic Person']);
});
