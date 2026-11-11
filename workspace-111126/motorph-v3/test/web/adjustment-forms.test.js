import test from 'node:test';
import assert from 'node:assert/strict';
import { COMPONENT_OPERATIONS, lineText, resultText, scopeLabel } from '../../web/f_flares/AdjustmentForms.js';

test('components allow only their operations', () => {
  assert.deepEqual(COMPONENT_OPERATIONS['Rice subsidy'], ['Add', 'Subtract', 'Replace']);
  assert.deepEqual(COMPONENT_OPERATIONS.Deduction, ['Add']);
});

test('form lines, scopes and results read plainly', () => {
  assert.equal(lineText({ component: 'Rice subsidy', operation: 'Replace', amountCents: 100000 }), 'Rice subsidy: set to ₱1,000.00');
  assert.equal(lineText({ component: 'Phone allowance', operation: 'Subtract', amountCents: 10000 }), 'Phone allowance: −₱100.00');
  assert.equal(scopeLabel('EMPLOYEE:DEMO-004', { 'DEMO-004': 'Jae Cruz' }), 'DEMO-004 — Jae Cruz');
  assert.equal(scopeLabel('TEAM:DEMO-003', { 'DEMO-003': 'Noa Reyes' }), 'Team of Noa Reyes');
  assert.equal(resultText({ status: 'Approved', decidedBy: 'DEMO-005', results: [
    { employeeId: 'DEMO-004', skipped: '', netBefore: 125000, netAfter: 140000 },
    { employeeId: 'DEMO-002', skipped: 'their own pay', netBefore: 0, netAfter: 0 }] }),
    'Approved by DEMO-005\nDEMO-004: net ₱1,250.00 → ₱1,400.00\nDEMO-002 skipped (their own pay)');
});
