import test from 'node:test';
import assert from 'node:assert/strict';
import { firstSentence, parseTabs } from '../../web/page-tabs.js';

test('a page lists its tabs as key:Label pairs', () => {
  assert.deepEqual(parseTabs('payslips:Payslips|items:Earnings & deductions| release : Release & emails '),
    [{ key: 'payslips', label: 'Payslips' }, { key: 'items', label: 'Earnings & deductions' }, { key: 'release', label: 'Release & emails' }]);
  assert.deepEqual(parseTabs(''), []);
});

test('the one-line version of an explanation is its first sentence', () => {
  assert.equal(firstSentence('Gross to net: earned pay + bonuses. Payroll Staff enter items for others.'), 'Gross to net: earned pay + bonuses.');
  assert.equal(firstSentence('No full stop here'), 'No full stop here');
  assert.equal(firstSentence('  Spaces   collapse.  Then more. '), 'Spaces collapse.');
});
