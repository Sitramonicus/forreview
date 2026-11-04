import test from 'node:test';
import assert from 'node:assert/strict';
import { directoryLabelSummaryRows } from '../../web/directory-summary.js';

test('counts stored status and department labels independently and in stable order', () => {
  const result = directoryLabelSummaryRows([
    { status: 'Active', department: 'Operations' },
    { status: 'On leave', department: 'Operations' },
    { status: 'Active', department: 'Finance' }
  ]);
  assert.deepEqual(result, [
    { category: 'Department label', label: 'Finance', employeeRecords: 1 },
    { category: 'Department label', label: 'Operations', employeeRecords: 2 },
    { category: 'Status label', label: 'Active', employeeRecords: 2 },
    { category: 'Status label', label: 'On leave', employeeRecords: 1 }
  ]);
});

test('returns an empty summary for missing collections and skips malformed labels', () => {
  assert.deepEqual(directoryLabelSummaryRows(null), []);
  assert.deepEqual(directoryLabelSummaryRows([null, {}, { status: '', department: 7 }]), []);
});
