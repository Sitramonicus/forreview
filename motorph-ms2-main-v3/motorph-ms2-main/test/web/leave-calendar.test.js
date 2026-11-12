import test from 'node:test';
import assert from 'node:assert/strict';
import { buildLeaveCalendar } from '../../web/f_flares/LeaveCalendar.js';

const request = (overrides = {}) => ({
  id: 'LV-DEMO',
  employeeId: 'DEMO-001',
  employeeName: 'Synthetic Person',
  leaveType: 'Personal',
  startDate: '2026-11-10',
  endDate: '2026-11-11',
  status: 'Pending review',
  ...overrides
});

test('builds a six-week Monday-first calendar with a display-only month title', () => {
  const calendar = buildLeaveCalendar(new Date(2026, 10, 3), [], new Date(2026, 10, 3));
  assert.equal(calendar.title, 'November 2026');
  assert.deepEqual(calendar.weekdays, ['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday']);
  assert.equal(calendar.weeks.length, 6);
  assert.equal(calendar.weeks.flat().length, 42);
  assert.equal(calendar.weeks.flat().filter(day => day.inMonth).length, 30);
  assert.equal(calendar.weeks.flat().filter(day => day.isToday).length, 1);
});

test('shows each date in an inclusive request range, but only inside the displayed month', () => {
  const calendar = buildLeaveCalendar(new Date(2026, 10, 1), [request({ startDate: '2026-10-31', endDate: '2026-11-02' })]);
  const marked = calendar.weeks.flat().filter(day => day.requests.length).map(day => day.date);
  assert.deepEqual(marked, ['2026-11-01', '2026-11-02']);
});

test('retains cancelled request status without implying approved time off', () => {
  const calendar = buildLeaveCalendar(new Date(2026, 10, 1), [request({ status: 'Cancelled', startDate: '2026-11-10', endDate: '2026-11-10' })]);
  const day = calendar.weeks.flat().find(item => item.date === '2026-11-10');
  assert.equal(day.requests.length, 1);
  assert.equal(day.requests[0].status, 'Cancelled');
});

test('ignores malformed or reversed ranges and rejects an invalid month argument', () => {
  const calendar = buildLeaveCalendar(new Date(2026, 10, 1), [
    request({ startDate: '2026-02-30', endDate: '2026-03-01' }),
    request({ startDate: '2026-11-15', endDate: '2026-11-10' })
  ]);
  assert.equal(calendar.weeks.flat().every(day => day.requests.length === 0), true);
  assert.throws(() => buildLeaveCalendar('November 2026'), /valid month date/i);
});
