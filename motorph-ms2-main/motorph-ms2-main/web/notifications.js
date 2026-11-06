/** In-app notifications (GET /api/notifications): a bell in the top bar with the unread count and a list. */
export function bellLabel(unread) {
  return unread ? `Notifications (${unread} unread)` : 'Notifications';
}

export function notificationLines(items) {
  return items.map(item => ({ id: item.id, unread: !item.read, text: `${item.at.replace('T', ' ')} · ${item.kind}: ${item.text}` }));
}

export function initNotifications(apiJson, getAccount) {
  const bell = document.getElementById('notification-bell');
  const panel = document.getElementById('notification-panel');
  if (!bell || !panel) return { refresh() {} };
  const list = document.getElementById('notification-list');

  async function refresh() {
    if (!getAccount()) return;
    bell.hidden = false;
    try {
      const { items, unread } = await apiJson('/api/notifications');
      bell.textContent = unread ? `🔔 ${unread}` : '🔔';
      bell.setAttribute('aria-label', bellLabel(unread));
      list.replaceChildren();
      const lines = notificationLines(items);
      if (!lines.length) {
        const empty = document.createElement('li');
        empty.textContent = 'No notifications yet.';
        list.append(empty);
      }
      lines.forEach(line => {
        const item = document.createElement('li');
        item.textContent = line.text;
        if (line.unread) item.classList.add('unread');
        list.append(item);
      });
    } catch {
      bell.hidden = true;
    }
  }

  bell.addEventListener('click', async () => {
    panel.hidden = !panel.hidden;
    bell.setAttribute('aria-expanded', String(!panel.hidden));
    if (panel.hidden) return;
    await refresh();
    // Opening the list marks everything read; the highlight stays until the next refresh.
    await apiJson('/api/notifications/read', {}).catch(() => {});
    bell.textContent = '🔔';
    bell.setAttribute('aria-label', bellLabel(0));
  });

  document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !panel.hidden) {
      panel.hidden = true;
      bell.setAttribute('aria-expanded', 'false');
      bell.focus();
    }
  });

  return { refresh };
}
