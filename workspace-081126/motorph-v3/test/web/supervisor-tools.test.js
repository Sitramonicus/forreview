import test from 'node:test';
import assert from 'node:assert/strict';
import { canReviewPayChange, payChangeSummary, scheduleRows, workdaysText } from '../../web/supervisor-tools.js';

test('workdays read as a range when consecutive', () => {
  assert.equal(workdaysText(['TUE', 'WED', 'THU', 'FRI', 'SAT']), 'Tue–Sat');
  assert.equal(workdaysText(['MON', 'WED', 'FRI']), 'Mon, Wed, Fri');
  assert.equal(workdaysText(['SAT', 'SUN']), 'Sat, Sun');
});

test('schedule rows show shift, meal hour and who set it', () => {
  const [row] = scheduleRows([{ employeeId: 'DEMO-004', shiftStart: '10:00', shiftEnd: '19:00', mealStart: '14:00',
    workdays: ['TUE', 'WED', 'THU', 'FRI', 'SAT'], updatedBy: 'DEMO-003', updatedAt: '2026-11-06T01:00:00Z' }], { 'DEMO-004': 'Jae Cruz' });
  assert.deepEqual(row, ['DEMO-004 — Jae Cruz', '10:00–19:00', '14:00 (1 h)', 'Tue–Sat', 'DEMO-003 · 2026-11-06T01:00:00Z']);
});

test('pay-change summaries and who may decide them', () => {
  const bonus = { id: 'PC-1', type: 'BONUS', employeeId: 'DEMO-004', requestedBy: 'DEMO-003', status: 'Pending review', bonusCents: 50000, bonusMonth: '2026-12', bonusCutoff: 1 };
  const merit = { ...bonus, type: 'MERIT', newMonthlyBasic: '27000.00', effectiveMonth: '2026-12' };
  assert.equal(payChangeSummary(bonus), 'Bonus ₱500.00 on 2026-12 cutoff 1–15');
  assert.equal(payChangeSummary(merit), 'Merit increase; basic ₱27000.00 from 2026-12');
  const as = (employeeId, role) => ({ employeeId, role });
  assert.equal(canReviewPayChange(as('DEMO-002', 'PAYROLL'), bonus), true);
  assert.equal(canReviewPayChange(as('DEMO-001', 'HR'), bonus), false);
  assert.equal(canReviewPayChange(as('DEMO-001', 'HR'), merit), true);
  assert.equal(canReviewPayChange(as('DEMO-003', 'SUPERVISOR'), merit), false, 'the requester never decides');
  assert.equal(canReviewPayChange(as('DEMO-004', 'ADMIN'), merit), false, 'nor the employee');
  assert.equal(canReviewPayChange(as('DEMO-005', 'ADMIN'), { ...merit, status: 'Approved' }), false);
});

test('day-change rows read the dates and the change', async () => {
  const { dayChangeRows } = await import('../../web/supervisor-tools.js');
  const rows = dayChangeRows([{ employeeId: 'DEMO-004', dayChanges: [
    { id: 'SC-0001', employeeId: 'DEMO-004', from: '2026-10-14', to: '2026-10-14', shiftStart: '', reason: 'Swapped day' },
    { id: 'SC-0002', employeeId: 'DEMO-004', from: '2026-12-01', to: '2026-12-03', shiftStart: '07:00', reason: 'Inventory' }] }], { 'DEMO-004': 'Jae Cruz' });
  assert.deepEqual(rows.map(row => row.cells), [['DEMO-004 — Jae Cruz', '2026-10-14', 'Day off', 'Swapped day'],
    ['DEMO-004 — Jae Cruz', '2026-12-01 to 2026-12-03', 'Shift from 07:00', 'Inventory']]);
});
