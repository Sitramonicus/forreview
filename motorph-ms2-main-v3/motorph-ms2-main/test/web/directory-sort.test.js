import test from 'node:test';
import assert from 'node:assert/strict';
import { sortDirectoryEntries } from '../../web/b_basic/DirectorySort.js';

const employees = [
  { id: 'DEMO-10', name: 'Mina', department: 'Operations', role: 'Lead', location: 'Taguig', status: 'Active' },
  { id: 'DEMO-2', name: 'Alex', department: 'Finance', role: 'Analyst', location: 'Makati', status: 'On leave' },
  { id: 'DEMO-1', name: 'Alex', department: 'HR', role: 'Partner', location: 'Taguig', status: 'Active' }
];

test('sorts employee directory rows by a selected field with numeric-aware text order', () => {
  assert.deepEqual(sortDirectoryEntries(employees, { field: 'id' }).map(row => row.id), ['DEMO-1', 'DEMO-2', 'DEMO-10']);
  assert.deepEqual(sortDirectoryEntries(employees, { field: 'name' }).map(row => row.id), ['DEMO-1', 'DEMO-2', 'DEMO-10']);
  assert.deepEqual(sortDirectoryEntries(employees, { field: 'department', direction: 'desc' }).map(row => row.department), ['Operations', 'HR', 'Finance']);
  assert.deepEqual(sortDirectoryEntries(employees, { field: 'status' }).map(row => row.status), ['Active', 'Active', 'On leave']);
});

test('does not mutate the source list and safely falls back for invalid options', () => {
  const input = [...employees];
  sortDirectoryEntries(input, { field: 'salary', direction: 'sideways' });
  assert.deepEqual(input, employees);
  assert.deepEqual(sortDirectoryEntries(null), []);
});
