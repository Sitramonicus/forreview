import test from 'node:test';
import assert from 'node:assert/strict';

const moduleOrError = await import('../../web/b_basic/CsvExport.js').catch(error => ({ importError: error }));
function api() {
  assert.ok(!moduleOrError.importError, 'CSV export module should load');
  assert.equal(typeof moduleOrError.buildCsv, 'function', 'buildCsv should be exported');
  return moduleOrError.buildCsv;
}

test('builds RFC-style quoted CSV rows with CRLF separators', () => {
  const buildCsv = api();
  const csv = buildCsv([
    { key: 'id', label: 'Employee ID' },
    { key: 'name', label: 'Name' }
  ], [{ id: 'DEMO-001', name: 'Mara Rivera' }]);
  assert.equal(csv, '"Employee ID","Name"\r\n"DEMO-001","Mara Rivera"');
});

test('quotes commas, double-quotes, and line breaks safely', () => {
  const buildCsv = api();
  const csv = buildCsv([{ key: 'note', label: 'Note' }], [{ note: 'Clock, corrected "demo"\nnext line' }]);
  assert.equal(csv, '"Note"\r\n"Clock, corrected ""demo""\nnext line"');
});

test('neutralizes spreadsheet formula prefixes after leading whitespace', () => {
  const buildCsv = api();
  const rows = ['=1+1', '+SUM(A1:A2)', '-2+3', '@NOW()', '  =HYPERLINK("x")']
    .map(value => ({ value }));
  const csv = buildCsv([{ key: 'value', label: 'Value' }], rows);
  assert.equal(csv.split('\r\n').slice(1).join('\r\n'), [
    '"\'=1+1"', '"\'+SUM(A1:A2)"', '"\'-2+3"', '"\'@NOW()"', '"\'  =HYPERLINK(""x"")"'
  ].join('\r\n'));
});

test('renders missing and null values as empty cells', () => {
  const buildCsv = api();
  assert.equal(buildCsv([{ key: 'a', label: 'A' }, { key: 'b', label: 'B' }], [{ a: null }, {}]),
    '"A","B"\r\n"",""\r\n"",""');
});

test('rejects malformed column and row inputs', () => {
  const buildCsv = api();
  assert.throws(() => buildCsv([], []), /at least one column/i);
  assert.throws(() => buildCsv([{ key: '', label: 'Name' }], []), /key and label/i);
  assert.throws(() => buildCsv([{ key: 'id', label: 'ID' }], {}), /rows must be an array/i);
});
