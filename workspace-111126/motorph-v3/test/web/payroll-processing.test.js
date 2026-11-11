import test from 'node:test';
import assert from 'node:assert/strict';
import { adjustmentRows, allowanceText, batchRows, correctionSummary, remittanceRows } from '../../web/f_flares/PayrollProcessing.js';

// Shaped like GET /api/payroll/adjustments, /disbursements and /remittance (amounts from Java).
const items = {
  earnings: [{ id: 'ER-1', employeeId: 'DEMO-004', month: '2026-11', cutoff: 1, type: 'Bonus', description: 'Spot award', amountCents: 100000 }],
  deductions: [{ id: 'VD-1', employeeId: 'DEMO-004', type: 'Company loan', description: '', amountPerCutoffCents: 30000, fromMonth: '2026-11', toMonth: '' },
    { id: 'VD-2', employeeId: 'DEMO-001', type: 'Insurance', description: '', amountPerCutoffCents: 5000, fromMonth: '2026-11', toMonth: '2026-12' }],
  allowances: [{ employeeId: 'DEMO-004', riceCents: 150000, phoneCents: 50000, clothingCents: 50000, payMethod: 'Check' }]
};

test('payroll items list one employee with signed amounts', () => {
  const rows = adjustmentRows(items, 'DEMO-004');
  assert.deepEqual(rows.map(row => row.id), ['ER-1', 'VD-1']);
  assert.deepEqual(rows[0].cells, ['Earning (taxable)', 'Bonus', 'Spot award', '2026-11 · 1–15', '+₱1,000.00']);
  assert.deepEqual(rows[1].cells.slice(3), ['2026-11 → until removed · every cutoff', '−₱300.00']);
  assert.match(allowanceText(items, 'DEMO-004'), /rice ₱1,500\.00 · phone ₱500\.00 · clothing ₱500\.00\. Paid by Check/);
  assert.throws(() => adjustmentRows({}, 'DEMO-004'), /Unexpected payroll items/);
});

test('disbursement batches and remittance rows', () => {
  const [batch] = batchRows([{ id: 'DB-2026-11-1-1', month: '2026-11', cutoff: 1, releasedBy: 'DEMO-002', releasedAt: '2026-11-06T01:00:00Z', totalCents: 286667,
    lines: [{ employeeName: 'Jae Cruz', netCents: 286667, method: 'Check' }] }]);
  assert.deepEqual(batch, ['DB-2026-11-1-1', '2026-11 · 1–15', 'DEMO-002 · 2026-11-06T01:00:00Z', 'Jae Cruz ₱2,866.67 (Check)', '₱2,866.67']);
  const rows = remittanceRows({ agencies: [{ agency: 'SSS', employeeCents: 12500, employerCents: 25500, totalCents: 38000 },
    { agency: 'BIR (withholding tax)', employeeCents: 0, employerCents: 0, totalCents: 0 }] });
  assert.deepEqual(rows, [['SSS', '₱125.00', '₱255.00', '₱380.00'], ['BIR (withholding tax)', '₱0.00', '—', '₱0.00']]);
});

test('correction summaries say void or where the difference goes', () => {
  assert.equal(correctionSummary({ payslipId: 'PS-DEMO-003-2026-10-1', kind: 'VOID' }), 'PS-DEMO-003-2026-10-1: Void (not yet released)');
  assert.equal(correctionSummary({ payslipId: 'PS-DEMO-004-2026-11-1', kind: 'ADJUST', targetMonth: '2026-12', targetCutoff: 1 }),
    'PS-DEMO-004-2026-11-1: Adjust on 2026-12 · 1–15');
});

test('group choices cover everyone, each team, branch and department', async () => {
  const { groupOptions, groupResultText } = await import('../../web/f_flares/PayrollProcessing.js');
  const employees = [
    { id: 'DEMO-001', location: 'Makati', department: 'People Operations', status: 'Active' },
    { id: 'DEMO-004', location: 'Pasig', department: 'Operations', status: 'Active' },
    { id: 'DEMO-009', location: 'Cebu', department: 'Sales', status: 'Separated' }
  ];
  const accounts = [{ employeeId: 'DEMO-001', name: 'Mara Rivera', supervisorId: 'DEMO-003' }, { employeeId: 'DEMO-003', name: 'Noa Reyes', supervisorId: '' }];
  assert.deepEqual(groupOptions(employees, accounts).map(option => option[1]),
    ['Everyone', 'Team of Noa Reyes', 'Branch: Makati', 'Branch: Pasig', 'Department: Operations', 'Department: People Operations']);
  assert.equal(groupResultText({ added: [{}, {}], skipped: [{ employeeId: 'DEMO-002', reason: 'you cannot change your own pay' }] }, 'Team of Noa Reyes'),
    'Added to 2 employee(s) in Team of Noa Reyes. Skipped: DEMO-002 (you cannot change your own pay). Each is a separate item you can remove.');
});

test('payroll summary rows end with the total', async () => {
  const { summaryRows } = await import('../../web/f_flares/PayrollProcessing.js');
  const item = { employees: 1, payslips: 1, grossCents: 219048, deductionsCents: 57381, allowancesCents: 125000, netCents: 286667 };
  assert.deepEqual(summaryRows({ groups: [{ name: 'Operations', ...item }], total: item }),
    [['Operations', '1', '1', '₱2,190.48', '−₱573.81', '+₱1,250.00', '₱2,866.67'], ['TOTAL', '1', '1', '₱2,190.48', '−₱573.81', '+₱1,250.00', '₱2,866.67']]);
  assert.deepEqual(summaryRows({ groups: [], total: item }), []);
});

test('outbox rows show delivery and attempts', async () => {
  const { outboxRows } = await import('../../web/f_flares/PayslipOutbox.js');
  const rows = outboxRows([
    { id: 'EM-0002', to: 'jae.cruz@motorph.example', payslipId: 'PS-DEMO-004-2026-11-1', subject: 'Your MotorPH payslip for 2026-11 1–15', status: 'Failed',
      reason: 'Mail server unreachable (simulated outage)', attempts: 2, lastAttemptAt: '2026-11-06T10:15' },
    { id: 'EM-0001', to: 'mara.rivera@motorph.example', payslipId: 'PS-DEMO-001-2026-11-1', subject: 's', status: 'Sent', reason: '', attempts: 1, lastAttemptAt: '2026-11-06T10:00' }
  ]);
  assert.equal(rows[0].cells[3], 'Failed: Mail server unreachable (simulated outage)');
  assert.equal(rows[0].cells[4], '2 · last 2026-11-06 10:15');
  assert.equal(rows[1].cells[3], 'Sent (simulated)');
});

test('allowance corrections are labelled non-taxable', () => {
  const rows = adjustmentRows({ earnings: [{ id: 'ER-1', employeeId: 'DEMO-002', type: 'Rice subsidy adjustment', description: 'Dispute', month: '2026-10', cutoff: 1, amountCents: 20000 }],
    deductions: [] }, 'DEMO-002');
  assert.equal(rows[0].cells[0], 'Non-taxable');
  assert.equal(rows[0].cells[4], '+₱200.00');
});

test('a released batch shows whether it is distributed', () => {
  const rows = batchRows([{ id: 'DB-2026-11-1-1', status: 'Distributed', month: '2026-11', cutoff: 1, releasedBy: 'DEMO-002', releasedAt: 't', totalCents: 286667,
    lines: [{ employeeName: 'Jae Cruz', netCents: 286667, method: 'Check' }] }]);
  assert.equal(rows[0][0], 'DB-2026-11-1-1 · Distributed');
});
