import test from 'node:test';
import assert from 'node:assert/strict';
import { comparisonRows } from '../../web/f_flares/PayrollCutoff.js';

const statutory = (sss, ph, pag, tax) => ({
  sss: { employeeCents: sss }, philHealth: { employeeCents: ph },
  pagIbig: { employeeCents: pag }, withholdingTaxCents: tax
});
const input = {
  statutory: statutory(150000, 75000, 20000, 100750),
  comparison: { statutory: statutory(150000, 75000, 20000, 100750),
    grossCents: 3000000, voluntaryCents: 0, deductionsCents: 345750,
    motorphDeductionsCents: 345750, netCents: 2654250, motorphNetCents: 2654250 }
};

test('identical rate calculations show no discrepancy', () => {
  const rows = comparisonRows(input);
  assert.equal(rows.length, 8);
  assert.deepEqual(rows.at(-1), ['Take-home pay', '₱26,542.50', '₱26,542.50', '₱0.00']);
});

test('different configured rates show signed reference less MotorPH differences', () => {
  const result = comparisonRows({ ...input, comparison: { ...input.comparison,
    statutory: statutory(160000, 75000, 20000, 100750), deductionsCents: 355750, netCents: 2644250 } });
  assert.deepEqual(result[1], ['SSS', '₱1,500.00', '₱1,600.00', '₱100.00']);
  assert.deepEqual(result.at(-1), ['Take-home pay', '₱26,542.50', '₱26,442.50', '-₱100.00']);
});

test('blocked amounts are unavailable rather than a fabricated negative net', () => {
  const rows = comparisonRows({ ...input, comparison: { ...input.comparison, netCents: null, motorphNetCents: null } });
  assert.deepEqual(rows.at(-1), ['Take-home pay', 'Unavailable', 'Unavailable', 'Unavailable']);
  assert.deepEqual(comparisonRows({ statutory: null, comparison: null }), []);
});

test('unsafe monetary amounts are rejected', () => {
  assert.throws(() => comparisonRows({ ...input, comparison: { ...input.comparison, netCents: Number.MAX_SAFE_INTEGER + 1 } }), /Unexpected/);
});
