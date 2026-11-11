import test from 'node:test';
import assert from 'node:assert/strict';
import { buildAttendanceCalendar } from '../../web/f_flares/AttenCalendar.js';

test('builds a six-week Monday-first raw-record calendar with a month title', () => {
  const calendar = buildAttendanceCalendar(new Date(2026, 9, 1), []);
  assert.equal(calendar.title, 'October 2026');
  assert.deepEqual(calendar.weekdays, ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']);
  assert.equal(calendar.weeks.length, 6);
  assert.ok(calendar.weeks.flat().every(cell => Array.isArray(cell.records)));
  assert.equal(calendar.weeks.flat().filter(cell => cell.inMonth).length, 31);
});

test('places raw entries only on their recorded dates and retains raw fields', () => {
  const record = { attendanceId: 5001, date: '2026-10-01', timeIn: '08:01', timeOut: '17:00', dayType: 'DEMO' };
  const calendar = buildAttendanceCalendar(new Date(2026, 9, 1), [record]);
  const cells = calendar.weeks.flat();
  assert.deepEqual(cells.find(cell => cell.date === '2026-10-01').records, [record]);
  assert.deepEqual(cells.find(cell => cell.date === '2026-10-02').records, []);
  assert.equal(cells.find(cell => cell.date === '2026-10-01').records[0].timeIn, '08:01');
});

test('ignores malformed and out-of-month records without treating blank dates as absence', () => {
  const calendar = buildAttendanceCalendar(new Date(2026, 9, 1), [
    { date: '2026-10-01', timeIn: '08:00', timeOut: '17:00' },
    { date: '2026-11-01', timeIn: '08:00', timeOut: '17:00' },
    { date: '2026-10-32', timeIn: '08:00', timeOut: '17:00' },
    { date: 'not-a-date' }, null
  ]);
  const cells = calendar.weeks.flat().filter(cell => cell.inMonth);
  assert.equal(cells.reduce((count, cell) => count + cell.records.length, 0), 1);
  assert.ok(cells.filter(cell => cell.date !== '2026-10-01').every(cell => cell.records.length === 0));
});

test('rejects invalid month or non-array attendance input', () => {
  assert.throws(() => buildAttendanceCalendar('2026-10-01', []), /valid month date/);
  assert.throws(() => buildAttendanceCalendar(new Date(2026, 9, 1), {}), /must be an array/);
});
