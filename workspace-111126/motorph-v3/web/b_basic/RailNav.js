const RAIL_KEY = 'motorph-rail';

/**
 * Collapsible navigation rail.
 *
 * Collapsed, only the logo and the icons stay. Hovering an icon pops a small panel with that page's
 * name, because an icon on its own is a guess. The collapsed state is remembered so a reload does not
 * undo the choice.
 */
export function initRailNav(root = document) {
  const shell = root.getElementById('app-shell');
  const sidebar = root.getElementById('sidebar');
  const toggle = root.getElementById('rail-toggle');
  if (!shell || !sidebar || !toggle) return { collapse() {}, expand() {}, toggle() {} };

  const flyout = document.createElement('div');
  flyout.className = 'rail-flyout';
  flyout.setAttribute('role', 'tooltip');
  document.body.append(flyout);

  let hideTimer = null;

  function isCollapsed() {
    return shell.classList.contains('rail');
  }

  function apply(collapsed, remember = true) {
    shell.classList.toggle('rail', collapsed);
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.setAttribute('aria-label', collapsed ? 'Expand navigation' : 'Collapse navigation');
    if (!collapsed) hideFlyout();
    if (remember) {
      try { localStorage.setItem(RAIL_KEY, collapsed ? '1' : '0'); } catch (ignored) { /* private mode */ }
    }
  }

  function hideFlyout() {
    flyout.classList.remove('open');
  }

  function showFlyout(button) {
    if (!isCollapsed()) return;
    const label = button.dataset.label || button.textContent.trim();
    const icon = button.querySelector('.nav-ico');
    flyout.replaceChildren();
    const copy = document.createElement('span');
    copy.textContent = label;
    if (icon) flyout.append(icon.cloneNode(true));
    flyout.append(copy);
    const box = button.getBoundingClientRect();
    flyout.style.left = `${box.right + 12}px`;
    flyout.style.top = `${box.top - 4}px`;
    flyout.style.minWidth = `${Math.max(150, label.length * 9 + 46)}px`;
    flyout.classList.add('open');
  }

  toggle.addEventListener('click', () => apply(!isCollapsed()));

  sidebar.querySelectorAll('.nav-item').forEach(button => {
    button.addEventListener('mouseenter', () => {
      clearTimeout(hideTimer);
      showFlyout(button);
    });
    button.addEventListener('mouseleave', () => {
      hideTimer = setTimeout(hideFlyout, 120);
    });
    button.addEventListener('focus', () => showFlyout(button));
    button.addEventListener('blur', hideFlyout);
    button.addEventListener('click', () => {
      if (isCollapsed()) apply(true, false);
      hideFlyout();
    });
  });

  sidebar.addEventListener('mouseleave', () => { hideTimer = setTimeout(hideFlyout, 160); });

  // A narrow window has no room for a 232px rail, so it starts collapsed there.
  const wide = window.matchMedia('(min-width: 1000px)');
  function honourWidth() {
    let stored = null;
    try { stored = localStorage.getItem(RAIL_KEY); } catch (ignored) { /* private mode */ }
    apply(stored === '1' || (!wide.matches && stored !== '0'), false);
  }
  wide.addEventListener('change', honourWidth);
  honourWidth();

  return {
    collapse: () => apply(true),
    expand: () => apply(false),
    toggle: () => apply(!isCollapsed())
  };
}
