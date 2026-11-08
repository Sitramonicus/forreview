import test from 'node:test';
import assert from 'node:assert/strict';
import { sortAttendanceCoverage } from '../../web/attendance-coverage-sort.js';

const reports = [
  { employeeId: 'DEMO-10', employeeName: 'Mina', scheduleId: 110, rawRecordCount: 2, firstRecordDate: '2026-10-02', lastRecordDate: '2026-10-03' },
  { employeeId: 'DEMO-2', employeeName: 'Alex', scheduleId: 102, rawRecordCount: 8, firstRecordDate: '2026-10-01', lastRecordDate: '2026-10-09' },
  { employeeId: 'DEMO-1', employeeName: 'Unmapped', scheduleId: null, rawRecordCount: 0, firstRecordDate: '', lastRecordDate: '' }
];

test('sorts coverage rows by employee, numeric coverage, and date fields', () => {
  assert.deepEqual(sortAttendanceCoverage(reports, { field: 'employeeId' }).map(row => row.employeeId), ['DEMO-1', 'DEMO-2', 'DEMO-10']);
  assert.deepEqual(sortAttendanceCoverage(reports, { field: 'rawRecordCount', direction: 'desc' }).map(row => row.rawRecordCount), [8, 2, 0]);
  assert.deepEqual(sortAttendanceCoverage(reports, { field: 'firstRecordDate' }).map(row => row.employeeId), ['DEMO-1', 'DEMO-2', 'DEMO-10']);
  assert.deepEqual(sortAttendanceCoverage(reports, { field: 'scheduleId', direction: 'desc' }).map(row => row.scheduleId), [110, 102, null]);
});

test('keeps source array untouched and returns an empty result for malformed collection input', () => {
  const input = [...reports];
  sortAttendanceCoverage(input, { field: 'not-a-field' });
  assert.deepEqual(input, reports);
  assert.deepEqual(sortAttendanceCoverage(null), []);
});
