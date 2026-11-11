import test from 'node:test';
import assert from 'node:assert/strict';
import { comparisonHeading, comparisonRows, formatReferenceCents } from '../../web/f_flares/SweldongComparison.js';

const report = {
  validated: true,
  comparisonStatus: 'MISMATCH',
  motorph: { rulesetId: 'PH-2026-MOTORPH-ESTIMATE-v5' },
  sweldong: { validated: true },
  deltas: [
    ['grossPay', 1100000, 1100000], ['sss', 50000, 50100], ['philHealth', 25000, 25000],
    ['pagIbig', 20000, 20000], ['withholdingTax', 0, 0], ['totalDeductions', 95000, 95100], ['netPay', 1005000, 1004900]
  ].map(([component, motorphCents, sweldongCents]) => ({ component, motorphCents, sweldongCents,
    deltaCents: sweldongCents - motorphCents, matches: sweldongCents === motorphCents }))
};

test('formats positive and negative cents without floating-point rounding', () => {
  assert.equal(formatReferenceCents(1005000), '₱10,050.00');
  assert.equal(formatReferenceCents(-101), '−₱1.01');
});

test('renders component comparison rows with explicit deltas', () => {
  const rows = comparisonRows(report);
  assert.equal(rows.length, 7);
  assert.equal(rows[1].label, 'SSS / GSIS employee share');
  assert.equal(rows[1].delta, 100);
  assert.equal(rows[1].matches, false);
  assert.equal(rows[6].delta, -100);
});

test('mismatch heading warns against automatically preferring either calculation', () => {
  assert.match(comparisonHeading(report), /neither calculation is automatically preferred/);
});

test('local-unavailable fallback does not invent local values', () => {
  const noLocal = { ...report, comparisonStatus: 'LOCAL_UNAVAILABLE', motorph: null, deltas: [] };
  assert.equal(comparisonRows(noLocal).every(row => row.motorph === null && row.sweldong === null), true);
  assert.match(comparisonHeading(noLocal), /MotorPH estimate unavailable/);
});

test('rejects malformed or internally inconsistent comparison reports', () => {
  assert.throws(() => comparisonRows({}), /validated results/);
  assert.throws(() => comparisonRows({ ...report, deltas: report.deltas.map(item => ({ ...item, deltaCents: 1 })) }), /invalid/);
});
