import test from 'node:test';
import assert from 'node:assert/strict';
import { governmentRow, requestSummary } from '../../web/government.js';

test('government rows show masked numbers or "Not on file"', () => {
  assert.deepEqual(governmentRow({ employeeId: 'DEMO-004', employeeName: 'Jae Cruz', fields: { sss: '••-••••789-0', philHealth: '', pagIbig: '', tin: '•••-••6-789' } }),
    ['DEMO-004 — Jae Cruz', '••-••••789-0', 'Not on file', 'Not on file', '•••-••6-789']);
});

test('request summaries say what is asked', () => {
  assert.equal(requestSummary({ kind: 'ACCESS', accessDate: '' }), 'Full view');
  assert.equal(requestSummary({ kind: 'ACCESS', accessDate: '2026-11-06' }), 'Full view for 2026-11-06');
  assert.equal(requestSummary({ kind: 'CHANGE', proposed: { pagIbig: '••••-••••-9012', tin: '•••-••4-321' } }), 'Pag-IBIG MID → ••••-••••-9012; TIN → •••-••4-321');
});
