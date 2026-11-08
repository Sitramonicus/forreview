import test from 'node:test';
import assert from 'node:assert/strict';
import { filterWorkflowItems } from '../../web/workflow-filter.js';

const items = [
  { id: 'LV-101', employeeId: '10001', employeeName: 'Avery Sample', leaveType: 'Vacation', startDate: '2026-11-12', status: 'Pending review' },
  { id: 'LV-102', employeeId: '10002', employeeName: 'Morgan Example', leaveType: 'Medical', startDate: '2026-11-15', status: 'Cancelled' },
  { id: 'TK-201', employeeId: '10001', employeeName: 'Avery Sample', ticketType: 'Technical support', status: 'Open' }
];

test('filters case-insensitively across explicitly selected fields', () => {
  const results = filterWorkflowItems(items, { query: 'aVeRy', searchFields: ['employeeName', 'id'] });
  assert.deepEqual(results.map(item => item.id), ['LV-101', 'TK-201']);
});

test('combines text and exact status filters', () => {
  const results = filterWorkflowItems(items, { query: '10001', status: 'Open', searchFields: ['employeeId', 'employeeName'] });
  assert.deepEqual(results.map(item => item.id), ['TK-201']);
});

test('filters exact leave or ticket type only through the explicitly selected type field', () => {
  assert.deepEqual(filterWorkflowItems(items, { type: 'Vacation', typeField: 'leaveType' }).map(item => item.id), ['LV-101']);
  assert.deepEqual(filterWorkflowItems(items, { type: 'Technical support', typeField: 'ticketType' }).map(item => item.id), ['TK-201']);
  assert.deepEqual(filterWorkflowItems(items, { type: 'Vacation' }), []);
});

test('ignores text in fields not configured for the list', () => {
  const results = filterWorkflowItems(items, { query: '2026-11-12', searchFields: ['id', 'employeeName'] });
  assert.deepEqual(results, []);
});

test('date-range filters include overlapping request dates and both boundaries', () => {
  const requests = [
    { id: 'before', startDate: '2026-10-01', endDate: '2026-10-09', status: 'Pending review' },
    { id: 'touch-from', startDate: '2026-10-10', endDate: '2026-10-12', status: 'Pending review' },
    { id: 'inside', startDate: '2026-10-11', endDate: '2026-10-11', status: 'Cancelled' },
    { id: 'touch-to', startDate: '2026-10-15', endDate: '2026-10-17', status: 'Pending review' },
    { id: 'after', startDate: '2026-10-18', endDate: '2026-10-20', status: 'Pending review' }
  ];
  const result = filterWorkflowItems(requests, { dateRange: { from: '2026-10-10', to: '2026-10-15' } });
  assert.deepEqual(result.map(item => item.id), ['touch-from', 'inside', 'touch-to']);
  assert.deepEqual(filterWorkflowItems(requests, { dateRange: { from: '2026-10-16' } }).map(item => item.id), ['touch-to', 'after']);
  assert.deepEqual(filterWorkflowItems(requests, { dateRange: { from: '2026-10-15', to: '2026-10-10' } }), []);
});

test('trims filter inputs and safely handles malformed item collections', () => {
  assert.deepEqual(filterWorkflowItems(items, { query: ' LV-101 ', searchFields: ['id'] }).map(item => item.id), ['LV-101']);
  assert.deepEqual(filterWorkflowItems(null), []);
});
