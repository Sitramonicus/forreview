import test from 'node:test';
import assert from 'node:assert/strict';
import { bellLabel, notificationLines } from '../../web/b_basic/Notifications.js';

test('the bell says how many are unread', () => {
  assert.equal(bellLabel(0), 'Notifications');
  assert.equal(bellLabel(3), 'Notifications (3 unread)');
});

test('notification lines show when, what kind and the text', () => {
  assert.deepEqual(notificationLines([{ id: 'NT-00001', at: '2026-11-06T10:15', kind: 'Leave', text: 'Your Vacation leave was approved.', read: false }]),
    [{ id: 'NT-00001', unread: true, text: '2026-11-06 10:15 · Leave: Your Vacation leave was approved.' }]);
});
