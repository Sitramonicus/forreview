import test from 'node:test';
import assert from 'node:assert/strict';
import { birRows, rateSummary } from '../../web/admin-config.js';

test('rates summary reads like the built-in 2026 set', () => {
  const version = { sss: { employeePercent: '5', employerPercent: '10', minMsc: '5000.00', maxMsc: '35000.00' },
    philHealth: { premiumPercent: '5', floor: '10000.00', ceiling: '100000.00' },
    pagIbig: { lowBand: '1500.00', lowPercent: '1', highPercent: '2', cap: '10000.00' },
    bir: [{ ratePercent: '0' }, { ratePercent: '15' }, { ratePercent: '35' }] };
  assert.equal(rateSummary(version), 'SSS 5% / 10% (MSC ₱5000.00–₱35000.00) · PhilHealth 5% (₱10000.00–₱100000.00) · '
    + 'Pag-IBIG 1% ≤ ₱1500.00, else 2% (cap ₱10000.00) · 3 tax brackets, top 35%');
});

test('blank tax rows are dropped, partial rows are kept for the server to reject', () => {
  assert.deepEqual(birRows([{ over: '0', baseTax: '0', ratePercent: '0' }, { over: '', baseTax: '', ratePercent: '' }, { over: '5', baseTax: '', ratePercent: '' }]),
    [{ over: '0', baseTax: '0', ratePercent: '0' }, { over: '5', baseTax: '', ratePercent: '' }]);
});
