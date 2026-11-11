import test from 'node:test';
import assert from 'node:assert/strict';
import { sortWorkflowItems } from '../../web/f_flares/WorkflowSort.js';

const requests = [
  { id: 'LV-03', employeeName: 'Mina', status: 'Cancelled', submittedAt: '2026-11-03T12:00:00Z', leaveType: 'Personal', startDate: '2026-11-10' },
  { id: 'LV-01', employeeName: 'Alex', status: 'Pending review', submittedAt: '2026-11-01T12:00:00Z', leaveType: 'Vacation', startDate: '2026-11-08' },
  { id: 'LV-02', employeeName: 'Bea', status: 'Cancelled', submittedAt: '2026-11-02T12:00:00Z', leaveType: 'Medical', startDate: '2026-11-09' }
];

test('sorts owner-sandbox workflow rows by selected field and preserves newest-first default', () => {
  assert.deepEqual(sortWorkflowItems(requests).map(row => row.id), ['LV-03', 'LV-02', 'LV-01']);
  assert.deepEqual(sortWorkflowItems(requests, { field: 'startDate', direction: 'asc' }).map(row => row.id), ['LV-01', 'LV-02', 'LV-03']);
  assert.deepEqual(sortWorkflowItems(requests, { field: 'employeeName', direction: 'desc' }).map(row => row.employeeName), ['Mina', 'Bea', 'Alex']);
});

test('does not mutate records and safely handles malformed collections or keys', () => {
  const input = [...requests];
  sortWorkflowItems(input, { field: 'unknown' });
  assert.deepEqual(input, requests);
  assert.deepEqual(sortWorkflowItems(null), []);
});
