import test from 'node:test';
import assert from 'node:assert/strict';
import { buildCutoffPayroll, payslipRows, savedPayslipRows } from '../../web/f_flares/PayrollCutoff.js';
import { overtimeRows } from '../../web/f_flares/Overtime.js';

// Shaped like GET /api/payroll/cutoffs for DEMO-001, October 2026 (Java computes every amount).
const payload = {
  employeeId: 'DEMO-001',
  month: '2026-10',
  hourlyRate: '150.00',
  monthlyGross: '3597.50',
  cutoffs: [
    { label: '1-15', from: '2026-10-01', to: '2026-10-15', daysWithRecords: 3, paidMinutes: 1439, unpaidOverageMinutes: 0, grossPay: '3597.50',
      deductionsCents: 28597, netCents: 331153, blocked: false },
    { label: '16-end', from: '2026-10-16', to: '2026-10-31', daysWithRecords: 0, paidMinutes: 0, grossPay: '0.00',
      deductionsCents: 28598, netCents: null, blocked: true }
  ],
  statutory: { totalEmployeeDeductionsCents: 57195, sss: { employeeCents: 25000 } }
};

test('shows Java cutoff amounts without recalculating them', () => {
  const result = buildCutoffPayroll(payload);
  const [first] = result.cutoffs;
  assert.equal(first.label, '1–15');
  assert.equal(first.hours, '23.98');
  assert.equal(first.grossCents, 359750n);
  assert.equal(first.deductionsCents, 28597n);
  assert.equal(first.netCents, 331153n);
  assert.equal(result.monthlyGrossCents, 359750n);
  assert.equal(result.statutory.totalEmployeeDeductionsCents, 57195n);
});

test('unapproved overage is shown next to paid hours, not added to them', () => {
  const withOverage = { ...payload, cutoffs: [{ ...payload.cutoffs[0], paidMinutes: 1440, unpaidOverageMinutes: 90 }, payload.cutoffs[1]] };
  assert.equal(buildCutoffPayroll(withOverage).cutoffs[0].hours, '24.00 (+1.50 unapproved, unpaid)');
});

test('a blocked cutoff shows a reason instead of a negative net', () => {
  const second = buildCutoffPayroll(payload).cutoffs[1];
  assert.equal(second.netCents, null);
  assert.match(second.blockedReason, /₱285\.98 exceed/);
});

test('a month with no paid attendance has no statutory breakdown', () => {
  const result = buildCutoffPayroll({ ...payload, monthlyGross: '0.00', statutory: null });
  assert.equal(result.statutory, null);
});

test('rejects a malformed response', () => {
  assert.throws(() => buildCutoffPayroll({ cutoffs: [] }), /Unexpected payroll response/);
});

// Shaped like GET /api/payroll/payslip for DEMO-001, October 2026, cutoff 1 (amounts from Java).
const payslip = {
  payslipId: 'PS-DEMO-001-2026-10-1',
  employee: { id: 'DEMO-001', name: 'Mara Rivera', department: 'People Operations', role: 'HR Coordinator' },
  month: '2026-10',
  period: { label: '1-15', from: '2026-10-01', to: '2026-10-15' },
  daysWithRecords: 3,
  paidMinutes: 1439,
  overtimeMinutes: 90,
  leaveDays: 1,
  holidayDays: 0,
  hourlyRate: '150.00',
  earnings: { basicCents: 300000, premiumCents: 0, overtimeCents: 28125, nightDiffCents: 0, holidayPayCents: 0, leavePayCents: 31625 },
  grossPayCents: 359750,
  deductions: { sssCents: 12500, philHealthCents: 12500, pagIbigCents: 3597, withholdingTaxCents: 0, totalCents: 28597 },
  netPayCents: 331153,
  rulesetId: 'PH-2026-MOTORPH-ESTIMATE-v5',
  profileVersion: 'TEMPORARY-2026-mentor-v2',
  generatedAt: '2026-11-05T12:00:00Z',
  temporary: true,
  saved: false
};

test('payslip shows employee, period, each deduction line and net from the API', () => {
  const view = payslipRows(payslip);
  assert.deepEqual(view.details[0], ['PAYSLIP NO.', 'PS-DEMO-001-2026-10-1']);
  assert.match(view.details[4][1], /23\.98 \+ 1\.50 OT h over 3 attendance days at ₱150\.00\/h; 1 paid leave day/);
  assert.deepEqual(view.lines.map(line => line[0]).slice(0, 3), ['Basic pay (hours worked)', 'Overtime (approved)', 'Paid leave']);
  assert.deepEqual(view.lines.map(line => line[1]),
    ['₱3,000.00', '₱281.25', '₱316.25', '₱3,597.50', '−₱125.00', '−₱125.00', '−₱35.97', '−₱0.00', '−₱285.97', '₱3,311.53']);
  assert.match(view.footnote, /not saved/);
});

test('overtime rows let a reviewer act only on other people\'s pending requests', () => {
  const requests = [
    { id: 'OT-1', employeeId: 'DEMO-001', employeeName: 'Mara Rivera', date: '2026-12-01', minutes: 90, status: 'Pending review', reviewerId: '' },
    { id: 'OT-2', employeeId: 'DEMO-002', employeeName: 'Eli Santos', date: '2026-12-02', minutes: 30, status: 'Pending review', reviewerId: '' },
    { id: 'OT-3', employeeId: 'DEMO-003', employeeName: 'Noa Reyes', date: '2026-12-03', minutes: 60, status: 'Approved', reviewerId: 'DEMO-002' }
  ];
  const view = overtimeRows(requests, 'DEMO-002');
  assert.deepEqual(view.map(row => row.canReview), [true, false, false]);
  assert.equal(view[0].cells[3], '1.50 h');
  assert.equal(view[2].cells[4], 'Approved by DEMO-002');
  assert.deepEqual(overtimeRows(requests, '').map(row => row.canReview), [false, false, false]);
});

test('a finalized payslip says it is a saved snapshot', () => {
  const view = payslipRows({ ...payslip, saved: true, finalizedAt: '2026-11-05T14:00:00Z' });
  assert.match(view.footnote, /^Finalized 2026-11-05T14:00:00Z .*will not change/);
});

test('saved history rows show period and amounts from the API', () => {
  const [row] = savedPayslipRows([{ payslipId: 'PS-DEMO-001-2026-10-1', employeeId: 'DEMO-001', employeeName: 'Mara Rivera',
    month: '2026-10', cutoff: '1-15', grossPayCents: 752424, netPayCents: 713650, finalizedAt: '2026-11-05T14:00:00Z' }]);
  assert.deepEqual(row.cells, ['PS-DEMO-001-2026-10-1', 'DEMO-001 — Mara Rivera', '2026-10 · 1–15', '₱7,524.24', '₱7,136.50', '2026-11-05T14:00:00Z']);
  assert.throws(() => savedPayslipRows(null), /Unexpected payslip history/);
  const [voided, second] = savedPayslipRows([{ payslipId: 'PS-DEMO-003-2026-10-1-void1', voided: true, employeeId: 'DEMO-003', employeeName: 'Noa Reyes',
    month: '2026-10', cutoff: '1-15', grossPayCents: 1, netPayCents: 1, finalizedAt: 'x' },
    { payslipId: 'PS-DEMO-003-2026-10-1', version: 2, employeeId: 'DEMO-003', employeeName: 'Noa Reyes', month: '2026-10', cutoff: '1-15', grossPayCents: 1, netPayCents: 1, finalizedAt: 'y' }]);
  assert.equal(voided.cells[0], 'PS-DEMO-003-2026-10-1-void1 · VOID');
  assert.equal(second.cells[0], 'PS-DEMO-003-2026-10-1 · v2');
  assert.match(payslipRows({ ...payslip, saved: true, voided: true, voidedAt: '2026-11-06T02:00:00Z', voidedBy: 'DEMO-005', voidReason: 'Wrong hours' }).footnote,
    /^VOIDED 2026-11-06T02:00:00Z by DEMO-005: Wrong hours/);
});

test('payslip lists bonuses, allowances and voluntary deductions from the API', () => {
  const view = payslipRows({ ...payslip,
    bonuses: [{ type: 'Bonus', description: 'Spot award', amountCents: 100000 }], bonusCents: 100000,
    allowances: { riceCents: 75000, phoneCents: 25000, clothingCents: 25000, totalCents: 125000 },
    deductions: { ...payslip.deductions, voluntary: [{ type: 'Company loan', description: 'Laptop loan', amountCents: 30000, shortfallCents: 0 }],
      voluntaryCents: 30000 } });
  const labels = view.lines.map(line => line[0]);
  assert.ok(labels.includes('Bonus — Spot award (taxable)'));
  assert.ok(labels.includes('Company loan — Laptop loan'));
  assert.ok(labels.includes('Rice subsidy (non-taxable)'));
  assert.equal(view.lines[view.lines.length - 1][0], 'NET PAY');
});

test('payslip rejects a malformed response', () => {
  assert.throws(() => payslipRows({}), /Unexpected payslip response/);
});
