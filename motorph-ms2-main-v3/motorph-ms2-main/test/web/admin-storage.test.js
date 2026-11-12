import test from 'node:test';
import assert from 'node:assert/strict';
import { storageSummary, verificationSummary } from '../../web/f_flares/AdminStorage.js';

test('storage summary describes actual configured durability and in-process counts', () => {
  assert.equal(storageSummary({ backend: 'firestore', durable: true, writeReady: true, storeCount: 30,
    employeeRecords: 34, attendanceRecords: 7421, activePayslips: 0 }),
  'Firestore · durable store configured · writes enabled · 30 state stores · 34 employees · 7421 attendance rows · 0 active payslips');
  assert.match(storageSummary({ backend: 'memory', durable: false, writeReady: true, storeCount: 30,
    employeeRecords: 34, attendanceRecords: 0, activePayslips: 0 }), /not durable/);
});

test('round-trip summary requires read, matching state, committed write, and matching read-back', () => {
  const result = { backend: 'firestore', readBeforeWrite: true, stateMatchedBeforeWrite: true,
    writeCommitted: true, readBackMatched: true, verifiedAt: '2026-11-02T12:00' };
  assert.match(verificationSummary(result), /^PASS · firestore read matched/);
  assert.throws(() => verificationSummary({ ...result, readBackMatched: false }), /all verification checks/);
});
