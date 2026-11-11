import test from 'node:test';
import assert from 'node:assert/strict';
import { metricsRows, minutesText } from '../../web/f_flares/AttenMetrics.js';

test('minutes read as hours and minutes', () => {
  assert.equal(minutesText(15), '15 min');
  assert.equal(minutesText(120), '2 h');
  assert.equal(minutesText(75), '1 h 15 min');
});

test('attendance metrics rows follow the Java response', () => {
  const rows = metricsRows({ from: '2026-11-02', to: '2026-11-08', presentDays: 2, absentDays: 1, tardyDays: 1, tardyMinutes: 15,
    overtimeMinutes: 60, unapprovedExtraMinutes: 60, undertimeMinutes: 15, leaveDays: 1, holidayDays: 0, creditYear: 2026,
    leaveCredits: [{ leaveType: 'Vacation', used: 1, remaining: 4 }] });
  assert.deepEqual(rows.map(row => row[1]), ['2', '1', '1 (15 min late)', '1 h · 1 h extra not approved', '15 min', '1 day(s)', 'Vacation 4 left']);
  assert.deepEqual(metricsRows({ from: '' }), [], 'no records and no range: nothing to show');
});
