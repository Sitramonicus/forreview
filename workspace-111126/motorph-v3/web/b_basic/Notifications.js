
const BELL_PATH = 'M12 22a2 2 0 002-2h-4a2 2 0 002 2zm6-6V11a6 6 0 00-5-5.91V4a1 1 0 10-2 0v1.09A6 6 0 006 11v5l-2 2v1h16v-1l-2-2z';

function paintBell(bell, unread) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('width', '16');
  svg.setAttribute('height', '16');
  svg.setAttribute('aria-hidden', 'true');
  svg.setAttribute('fill', 'currentColor');
  const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  path.setAttribute('d', BELL_PATH);
  svg.append(path);
  bell.replaceChildren(svg);
  if (unread) {
    const count = document.createElement('span');
    count.className = 'bell-count';
    count.textContent = String(unread);
    bell.append(count);
  }
}

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
      paintBell(bell, unread);
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
    
    await apiJson('/api/notifications/read', {}).catch(() => {});
    paintBell(bell, 0);
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
