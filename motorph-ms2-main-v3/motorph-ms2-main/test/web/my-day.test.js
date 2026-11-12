import test from 'node:test';
import assert from 'node:assert/strict';
import { clockOutQuestions, clockText, locationText, qrPattern, trackerSummary } from '../../web/b_basic/MyDay.js';

test('time clock wording follows the server status', () => {
  assert.deepEqual(clockText({ clockedIn: false, since: '', today: '2026-11-09', todayRecord: null }),
    { state: 'Clocked out', detail: 'Not clocked in yet today (2026-11-09).', button: 'Clock in' });
  assert.equal(clockText({ clockedIn: true, since: '08:04', today: '2026-11-09', todayRecord: null }).button, 'Clock out');
  const done = clockText({ clockedIn: false, since: '', today: '2026-11-09', todayRecord: { timeIn: '08:04', timeOut: '17:02' } });
  assert.equal(done.button, null);
  assert.match(done.detail, /In 08:04 · out 17:02/);
});

// Shaped like GET /api/payroll/cutoffs for DEMO-004 (148.81/h), November 2026, after one clocked day on the 9th.
const payload = {
  employeeId: 'DEMO-004', month: '2026-11', hourlyRate: '148.81', monthlyGross: '1190.48',
  cutoffs: [
    { label: '1-15', from: '2026-11-01', to: '2026-11-15', daysWithRecords: 1, paidMinutes: 480, unpaidOverageMinutes: 0, overtimeMinutes: 0,
      leaveDays: 0, holidayDays: 0, grossPay: '1190.48', deductionsCents: 26190, netCents: 92858, blocked: false },
    { label: '16-end', from: '2026-11-16', to: '2026-11-30', daysWithRecords: 0, paidMinutes: 0, unpaidOverageMinutes: 0, overtimeMinutes: 0,
      leaveDays: 0, holidayDays: 1, grossPay: '1190.48', deductionsCents: 26191, netCents: 92857, blocked: false }
  ],
  statutory: null
};

test('tracker picks the cutoff containing today and shows Java amounts', () => {
  const first = trackerSummary(payload, '2026-11-09');
  assert.equal(first.period, '1–15 2026-11 (2026-11-01 – 2026-11-15)');
  assert.deepEqual(first.figures.map(row => row[1]),
    ['8.00 h over 1 day(s)', '₱1,190.48', '−₱261.90', '₱928.58', '₱1,190.48']);
  const second = trackerSummary(payload, '2026-11-20');
  assert.match(second.figures[0][1], /1 paid holiday/);
});

test('tracker explains a cutoff whose pay does not yet cover deductions', () => {
  const blocked = { ...payload, cutoffs: [{ ...payload.cutoffs[0], netCents: null, blocked: true }, payload.cutoffs[1]] };
  assert.match(trackerSummary(blocked, '2026-11-02').figures[3][1], /Not enough pay yet/);
});

test('clocking out always asks, and asks again before the shift ends', () => {
  assert.equal(clockOutQuestions({ now: '17:05:12', shiftEnd: '17:00' }).length, 1);
  const early = clockOutQuestions({ now: '15:30:00', shiftEnd: '17:00' });
  assert.equal(early.length, 2);
  assert.match(early[1], /shift ends at 17:00/);
});

test('offsite until the branch QR scan, then onsite', () => {
  assert.match(locationText({ clockedIn: true, location: 'Offsite' }), /^Offsite · clocked in from the app/);
  assert.equal(locationText({ clockedIn: true, location: 'Onsite', onsiteSince: '08:14' }), 'Onsite · QR scanned at the branch at 08:14');
  assert.equal(locationText({ clockedIn: false, location: 'Offsite', todayRecord: null }), '');
});

test('the QR picture has the three corner squares and depends on the code', () => {
  const grid = qrPattern('MPH-ABC234');
  assert.equal(grid.length, 21);
  for (const [r, c] of [[0, 0], [0, 20], [20, 0], [3, 3]]) assert.equal(grid[r][c], 1, `corner square cell ${r},${c}`);
  assert.equal(grid[7][7], 0, 'quiet line around the corner square');
  assert.notDeepEqual(qrPattern('MPH-XYZ789'), grid);
  assert.deepEqual(qrPattern('MPH-ABC234'), grid, 'same code, same picture');
});

test('clock tags and the day-type question', async () => {
  const { clockInQuestion } = await import('../../web/b_basic/MyDay.js');
  assert.match(clockText({ clockedIn: true, since: '08:15', today: '2026-11-09', todayRecord: null, attendanceTag: 'Tardy' }).detail, /· Tardy\.$/);
  assert.match(clockText({ clockedIn: false, today: '2026-11-09', todayRecord: { timeIn: '08:02', timeOut: '15:00' }, attendanceTag: 'Present', workHourTag: 'Undertime' }).detail,
    /· Present · Undertime hours\.$/);
  assert.equal(clockInQuestion({ dayType: 'Workday' }), null);
  assert.equal(clockInQuestion({ dayType: 'Rest day' }), 'Today is a Rest day for you. Clock in anyway?');
  assert.equal(clockInQuestion({ dayType: 'On approved leave (Vacation)' }), 'Today is a day on approved leave (Vacation) for you. Clock in anyway?');
});

test('the salary tracker says when the estimate was made', () => {
  const view = trackerSummary(payload, '2026-11-09', '10:15:00');
  assert.equal(view.asOf, 'Estimate as of 2026-11-09 10:15');
  assert.match(view.note, /^Estimate as of 2026-11-09 10:15\. Rate/);
});
