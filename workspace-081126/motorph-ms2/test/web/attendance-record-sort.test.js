import test from 'node:test';
import assert from 'node:assert/strict';
import { sortAttendanceRecords } from '../../web/attendance-record-sort.js';

const records = [
  { attendanceId: 10, scheduleId: 101, date: '2026-10-03', timeIn: '08:03', timeOut: '17:00', dayType: 'DEMO' },
  { attendanceId: 2, scheduleId: 101, date: '2026-10-01', timeIn: '07:58', timeOut: '17:01', dayType: 'DEMO' },
  { attendanceId: 1, scheduleId: 102, date: '2026-10-01', timeIn: '08:01', timeOut: '16:59', dayType: 'DEMO' }
];

test('sorts raw attendance by supported fields and direction without deriving values', () => {
  assert.deepEqual(sortAttendanceRecords(records).map(row => row.attendanceId), [1, 2, 10]);
  assert.deepEqual(sortAttendanceRecords(records, { field: 'date', direction: 'desc' }).map(row => row.attendanceId), [10, 1, 2]);
  assert.deepEqual(sortAttendanceRecords(records, { field: 'attendanceId' }).map(row => row.attendanceId), [1, 2, 10]);
  assert.deepEqual(sortAttendanceRecords(records, { field: 'timeIn', direction: 'desc' }).map(row => row.timeIn), ['08:03', '08:01', '07:58']);
});

test('returns a sorted copy and safely handles missing input or unsupported keys', () => {
  const input = [...records];
  sortAttendanceRecords(input, { field: 'paidHours' });
  assert.deepEqual(input, records);
  assert.deepEqual(sortAttendanceRecords(undefined), []);
});
