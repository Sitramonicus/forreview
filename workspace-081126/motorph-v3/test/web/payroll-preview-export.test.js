import test from 'node:test';
import assert from 'node:assert/strict';
import { statutoryFromApi } from '../../web/payroll-preview-2026.js';
import { buildPayrollPreviewCsv, payrollPreviewCsvRows } from '../../web/payroll-preview-export.js';
import { STATUTORY_25000_API_RESULT } from './statutory-25000.fixture.js';

test('exports only the completed synthetic calculation inputs and clearly labelled estimates', () => {
  const result = statutoryFromApi(STATUTORY_25000_API_RESULT);
  const rows = payrollPreviewCsvRows(result);
  const netRow = rows.find(row => row.item === 'Net cash pay estimate');
  const sssInput = rows.find(row => row.item === 'SSS compensation base');
  assert.equal(netRow.amount, '₱22,611.25');
  assert.match(netRow.basisNote, /Not a payslip/);
  assert.equal(sssInput.amount, '₱25,000.00');
  assert.match(sssInput.basisNote, /MSC/);
  assert.doesNotMatch(JSON.stringify(rows), /employee id|government id/i);
  const csv = buildPayrollPreviewCsv(result);
  assert.match(csv, /"Net cash pay estimate","₱22,611.25"/);
  assert.match(csv, /"Temporary statutory reference; not a payroll run"/);
});

test('refuses to export when no completed calculation is supplied', () => {
  assert.throws(() => payrollPreviewCsvRows(null), /completed synthetic preview/);
});
