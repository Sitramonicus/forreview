import test from 'node:test';
import assert from 'node:assert/strict';
import { formatPesoCents, parsePesoCents, statutoryFromApi } from '../../web/payroll-preview-2026.js';
import { STATUTORY_25000_API_RESULT } from './statutory-25000.fixture.js';

// The statutory calculation is tested in Java (StatutoryCalculator2026Test, incl. 25 sweldongpinoy.com values).

test('parses and formats centavos exactly without floating-point arithmetic', () => {
  assert.equal(parsePesoCents('1,234.56'), 123456n);
  assert.equal(parsePesoCents('0'), 0n);
  assert.equal(formatPesoCents(123456789n), '₱1,234,567.89');
  assert.throws(() => parsePesoCents('-1'), /non-negative/);
  assert.throws(() => parsePesoCents('1.001'), /two decimal places/);
});

test('converts the Java API centavo fields to BigInt, including nested ones', () => {
  const result = statutoryFromApi(STATUTORY_25000_API_RESULT);
  assert.equal(result.netPayCents, 2261125n);
  assert.equal(result.sss.employeeCents, 125000n);
  assert.equal(result.rulesetId, 'PH-2026-sweldongpinoy-v4');
  assert.equal(formatPesoCents(result.netPayCents), '₱22,611.25');
});

test('rejects non-integer amounts from the API', () => {
  assert.throws(() => statutoryFromApi({ netPayCents: 1.5 }), /Unexpected payroll amount/);
  assert.throws(() => statutoryFromApi({ netPayCents: '100' }), /Unexpected payroll amount/);
});
