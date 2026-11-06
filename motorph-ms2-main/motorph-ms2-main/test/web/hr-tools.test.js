import test from 'node:test';
import assert from 'node:assert/strict';
import { checklistProgress, employeeStats, employmentSummary, resolvableTickets } from '../../web/hr-tools.js';

test('checklist progress lists what is left', () => {
  const progress = checklistProgress([{ task: 'Contract signed', done: true }, { task: 'Payroll set up', done: true },
    { task: 'Government IDs submitted', done: false }, { task: 'Orientation done', done: false }]);
  assert.equal(progress.text, '2 of 4 done');
  assert.deepEqual(progress.left, ['Government IDs submitted', 'Orientation done']);
});

test('employment summary shows start, last day and the final payslip', () => {
  assert.equal(employmentSummary({ startDate: '2026-10-01', lastDay: '2026-10-31', separationReason: 'Resigned', finalMonth: '2026-10', finalCutoff: 2 }),
    'Started 2026-10-01 · last day 2026-10-31 (Resigned) · final payslip: 2026-10 cutoff 16–end');
  assert.equal(employmentSummary({ startDate: '', lastDay: '' }), 'Seeded record');
});

test('HR answers only open HR-queue tickets that are not their own', () => {
  const tickets = [
    { id: 'TK-1', status: 'Open', queue: 'HR', employeeId: 'DEMO-004' },
    { id: 'TK-2', status: 'Open', queue: 'Team Lead', employeeId: 'DEMO-004' },
    { id: 'TK-3', status: 'Resolved', queue: 'HR', employeeId: 'DEMO-004' },
    { id: 'TK-4', status: 'Open', queue: 'HR', employeeId: 'DEMO-001' }
  ];
  assert.deepEqual(resolvableTickets(tickets, { employeeId: 'DEMO-001', role: 'HR' }).map(t => t.id), ['TK-1']);
  assert.deepEqual(resolvableTickets(tickets, { employeeId: 'DEMO-005', role: 'ADMIN' }).map(t => t.id), ['TK-1', 'TK-2', 'TK-4']);
  assert.deepEqual(resolvableTickets(tickets, { employeeId: 'DEMO-002', role: 'PAYROLL' }), []);
});

test('employee statistics count the directory, HR records and the HR queue', () => {
  const employees = [
    { id: 'A', status: 'Active', department: 'Operations', location: 'Pasig' },
    { id: 'B', status: 'Active', department: 'Operations', location: 'Makati' },
    { id: 'C', status: 'On leave', department: 'Finance', location: 'Pasig' },
    { id: 'D', status: 'Separated', department: 'Finance', location: 'Pasig' }
  ];
  const records = [
    { employeeId: 'B', startDate: '2026-03-01', onboarding: [{ task: 'Laptop', done: false }], offboarding: [] },
    { employeeId: 'D', startDate: '2025-01-06', lastDay: '2026-10-30', onboarding: [{ task: 'Laptop', done: true }], offboarding: [{ task: 'ID returned', done: false }] }
  ];
  const tickets = [{ status: 'Open', queue: 'HR' }, { status: 'Resolved', queue: 'HR' }, { status: 'Open', queue: 'Payroll Staff' }];
  const stats = Object.fromEntries(employeeStats(employees, records, tickets, 2026));
  assert.equal(stats.HEADCOUNT, '3 current (4 on record)');
  assert.equal(stats['BY STATUS'], 'Active 2 · On leave 1 · Separated 1');
  assert.equal(stats['BY DEPARTMENT'], 'Operations 2 · Finance 1', 'separated people are not counted in departments');
  assert.equal(stats['BY BRANCH'], 'Pasig 2 · Makati 1');
  assert.equal(stats['NEW HIRES 2026'], '1');
  assert.equal(stats['SEPARATIONS 2026'], '1');
  assert.equal(stats['ONBOARDING QUEUE'], '1');
  assert.equal(stats['OFFBOARDING QUEUE'], '1');
  assert.equal(stats['OPEN HR TICKETS'], '1');
});

test('change history rows show before and after', async () => {
  const { changelogRows } = await import('../../web/hr-tools.js');
  assert.deepEqual(changelogRows([{ at: '2026-11-06T10:15', field: 'Branch', before: 'Pasig', after: 'Makati', by: 'DEMO-001', via: 'Directory edit' },
    { at: '2026-11-06T10:16', field: 'Address', before: '', after: 'Unit 4', by: 'DEMO-001', via: 'Address & birthdate' }]),
    [['2026-11-06 10:15', 'Branch', 'Pasig', 'Makati', 'DEMO-001', 'Directory edit'], ['2026-11-06 10:16', 'Address', '—', 'Unit 4', 'DEMO-001', 'Address & birthdate']]);
});

test('personal fields read the address parts and contact number', async () => {
  const { personalFields } = await import('../../web/hr-tools.js');
  const values = { 'p-building': 'Unit 4', 'p-street': 'Sample St', 'p-city': 'Pasig', 'p-region': 'NCR', 'p-postal': '1600', 'p-contact': '09170000004' };
  assert.deepEqual(personalFields('p', id => values[id]),
    { building: 'Unit 4', street: 'Sample St', city: 'Pasig', region: 'NCR', postalCode: '1600', contactNumber: '09170000004' });
});

test('bar chart data: current people per department, largest first', async () => {
  const { statBars } = await import('../../web/hr-tools.js');
  const people = [{ department: 'Operations', status: 'Active' }, { department: 'Finance', status: 'Active' }, { department: 'Operations', status: 'On leave' },
    { department: 'Finance', status: 'Separated' }];
  assert.deepEqual(statBars(people, 'department'), [['Operations', 2], ['Finance', 1]]);
});

test('name fields read first name, surname, gender and email', async () => {
  const { nameFields } = await import('../../web/hr-tools.js');
  const values = { 'p-first': 'Jae', 'p-surname': 'Cruz', 'p-gender': 'Male', 'p-email': 'jae.personal@example.com' };
  assert.deepEqual(nameFields('p', id => values[id]), { firstName: 'Jae', surname: 'Cruz', gender: 'Male', email: 'jae.personal@example.com' });
});
