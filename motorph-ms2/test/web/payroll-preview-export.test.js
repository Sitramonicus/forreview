import test from 'node:test';
import assert from 'node:assert/strict';
import { calculatePayrollPreview2026 } from '../../web/payroll-preview-2026.js';
import { buildPayrollPreviewCsv, payrollPreviewCsvRows } from '../../web/payroll-preview-export.js';

const example = {
  period: '2026-01',
  grossCashPay: '25000.00',
  sssCompensation: '25000.00',
  philHealthBasicSalary: '25000.00',
  pagIbigFundSalary: '25000.00',
  taxableCompensation: '25000.00'
};

test('exports only the completed synthetic calculation inputs and clearly labelled estimates', () => {
  const result = calculatePayrollPreview2026(example);
  const rows = payrollPreviewCsvRows(result);
  const netRow = rows.find(row => row.item === 'Net cash pay estimate');
  const sssInput = rows.find(row => row.item === 'SSS compensation base');
  assert.equal(netRow.amount, '₱22,611.20');
  assert.match(netRow.basisNote, /Not a payslip/);
  assert.equal(sssInput.amount, '₱25,000.00');
  assert.match(sssInput.basisNote, /MSC/);
  assert.doesNotMatch(JSON.stringify(rows), /employee id|government id/i);
  const csv = buildPayrollPreviewCsv(result);
  assert.match(csv, /"Net cash pay estimate","₱22,611\.20"/);
  assert.match(csv, /"Temporary statutory reference; not a payroll run"/);
});

test('refuses to export when no completed calculation is supplied', () => {
  assert.throws(() => payrollPreviewCsvRows(null), /completed synthetic preview/);
});
