import test from 'node:test';
import assert from 'node:assert/strict';
import { accountSecurityText, auditRows } from '../../web/f_flares/AdminSecurity.js';
import { branchRows } from '../../web/b_basic/Branches.js';

test('audit rows read who, what and the result', () => {
  assert.deepEqual(auditRows([{ at: '2026-11-06T10:15', actor: 'no account', method: 'POST', path: '/api/auth/2fa/start', target: 'DEMO-004', status: 401 }]),
    [['2026-11-06 10:15', 'no account', 'POST /api/auth/2fa/start', 'DEMO-004', '401']]);
});

test('account password status', () => {
  assert.equal(accountSecurityText({ lockedUntil: '2026-11-06T10:30', defaultPassword: true, failedAttempts: 0 }), 'Locked until 10:30');
  assert.equal(accountSecurityText({ lockedUntil: '', defaultPassword: true, failedAttempts: 2 }), 'Shared default password · 2 wrong recently');
  assert.equal(accountSecurityText({ lockedUntil: '', defaultPassword: false, passwordChangedAt: '2026-11-06T09:00', failedAttempts: 0 }), 'Own password since 2026-11-06');
});

test('branch rows mark your own branch', () => {
  assert.deepEqual(branchRows({ mine: 'Pasig', branches: [{ name: 'Makati', headcount: 2, active: 2, onLeaveToday: 0, clockedInNow: 1, onsiteToday: 0 },
    { name: 'Pasig', headcount: 1, active: 1, onLeaveToday: 0, clockedInNow: 0, onsiteToday: 1 }] }),
    [['Makati', '2', '2', '0', '1', '0'], ['Pasig (yours)', '1', '1', '0', '0', '1']]);
});
