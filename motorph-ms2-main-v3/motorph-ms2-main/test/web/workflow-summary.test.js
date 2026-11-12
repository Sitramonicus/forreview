import test from 'node:test';
import assert from 'node:assert/strict';
import { workflowSummaryRows } from '../../web/f_flares/WorkflowSummary.js';

test('summarizes stored workflow status and type/queue labels without interpreting approval or balances', () => {
  const rows = workflowSummaryRows([
    { status: 'Cancelled', leaveType: 'Personal' },
    { status: 'Pending review', leaveType: 'Personal' }
  ], [
    { status: 'Open', ticketType: 'Technical support', queue: 'IT Admin' },
    { status: 'Cancelled', ticketType: 'Technical support', queue: 'IT Admin' },
    { status: 'Open', ticketType: 'Payroll inquiry', queue: 'Payroll Staff' }
  ]);
  assert.deepEqual(rows, [
    { category: 'Leave request status', label: 'Cancelled', recordCount: 1 },
    { category: 'Leave request status', label: 'Pending review', recordCount: 1 },
    { category: 'Leave request type', label: 'Personal', recordCount: 2 },
    { category: 'Ticket queue label', label: 'IT Admin', recordCount: 2 },
    { category: 'Ticket queue label', label: 'Payroll Staff', recordCount: 1 },
    { category: 'Ticket status', label: 'Cancelled', recordCount: 1 },
    { category: 'Ticket status', label: 'Open', recordCount: 2 },
    { category: 'Ticket type', label: 'Payroll inquiry', recordCount: 1 },
    { category: 'Ticket type', label: 'Technical support', recordCount: 2 }
  ]);
});

test('handles empty and malformed workflow collections without producing fake labels', () => {
  assert.deepEqual(workflowSummaryRows(null, undefined), []);
  assert.deepEqual(workflowSummaryRows([null, {}, { status: ' ' }], [null, { queue: 2 }]), []);
});
