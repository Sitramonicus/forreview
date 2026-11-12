const rn_key = 'motorph-rail';

export function initRailNav(root = document) {
  const rn_shell = root.getElementById('app-shell'), rn_sidebar = root.getElementById('sidebar'),
    rn_toggle = root.getElementById('rail-toggle'), rn_head = root.getElementById('head-nav');
  if (!rn_shell || !rn_sidebar || !rn_toggle) return { collapse() {}, expand() {}, toggle() {} };

  const isCollapsed = () => rn_shell.classList.contains('rail');

  function apply(collapsed, remember = true) {
    rn_shell.classList.toggle('rail', collapsed);
    rn_shell.classList.remove('peek');
    rn_toggle.setAttribute('aria-expanded', String(!collapsed));
    rn_toggle.setAttribute('aria-label', collapsed ? 'Expand navigation' : 'Collapse navigation');
    if (remember) {
      try { localStorage.setItem(rn_key, collapsed ? '1' : '0'); } catch (ignored) { /* private mode */ }
    }
  }

  const peek = on => rn_shell.classList.toggle('peek', on && isCollapsed());
  rn_sidebar.addEventListener('mouseenter', () => peek(true));
  rn_sidebar.addEventListener('mouseleave', () => peek(false));
  rn_sidebar.addEventListener('focusin', () => peek(true));
  rn_sidebar.addEventListener('focusout', event => { if (!rn_sidebar.contains(event.relatedTarget)) peek(false); });
  rn_toggle.addEventListener('click', () => apply(!isCollapsed()));

  rn_sidebar.querySelectorAll('.nav-item').forEach(item => {
    item.addEventListener('click', () => {
      const icon = item.querySelector('.nav-ico');
      icon.classList.add('pulse');
      setTimeout(() => icon.classList.remove('pulse'), 500);
    });
    if (!rn_head) return;
    const twin = document.createElement('button'), label = document.createElement('span');
    twin.type = 'button';
    twin.append(item.querySelector('.nav-ico').cloneNode(true), label);
    twin.addEventListener('click', () => item.click());
    const sync = () => {
      const text = item.querySelector('span:not(.nav-index):not(.nav-mark)').textContent;
      if (label.textContent !== text) label.textContent = text;
      twin.setAttribute('aria-label', text);
      twin.hidden = item.classList.contains('role-hidden');
      twin.classList.toggle('active', item.classList.contains('active'));
    };
    new MutationObserver(sync).observe(item, { attributes: true, attributeFilter: ['class'], childList: true, subtree: true, characterData: true });
    sync();
    rn_head.append(twin);
  });

  const wide = window.matchMedia('(min-width: 1000px)');
  function honourWidth() {
    let stored = null;
    try { stored = localStorage.getItem(rn_key); } catch (ignored) { /* private mode */ }
    apply(stored === '1' || (!wide.matches && stored !== '0'), false);
  }
  wide.addEventListener('change', honourWidth);
  honourWidth();

  return { collapse: () => apply(true), expand: () => apply(false), toggle: () => apply(!isCollapsed()) };
}
