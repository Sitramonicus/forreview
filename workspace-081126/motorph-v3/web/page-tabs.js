/**
 * Section tabs for the desk pages. A page lists its tabs in data-tabs="key:Label|key:Label"; elements inside carry
 * data-tab="key" (or several keys) and only the active tab's elements show. Containers marked data-tab-group show
 * when any of their tabbed children does. Tabs with nothing your role can see are hidden. The last tab per page is
 * remembered, and "Show all sections" shows everything (for searching, printing, or the browser test).
 * Badges count work waiting in a tab: Approve buttons and failed payslip emails.
 */
const TAB_KEY = 'motorph-tab-';
const ALL_KEY = 'motorph-all-sections';

export function parseTabs(spec) {
  return String(spec || '').split('|').map(part => part.trim()).filter(Boolean).map(part => {
    const [key, ...label] = part.split(':');
    return { key: key.trim(), label: label.join(':').trim() || key.trim() };
  });
}

/** First sentence of a long explanation, for the one-line version (the rest goes behind "How this works"). */
export function firstSentence(text) {
  const clean = String(text || '').replace(/\s+/g, ' ').trim();
  const match = clean.match(/^(.+?[.!?])(\s|$)/);
  return match ? match[1] : clean;
}

/** Count of waiting work in a set of elements: Approve buttons and failed email rows. */
export function badgeCount(elements) {
  let count = 0;
  elements.forEach(element => {
    count += element.querySelectorAll('button[aria-label^="Approve "]').length;
    count += [...element.querySelectorAll('td')].filter(cell => cell.textContent.startsWith('Failed:')).length;
  });
  return count;
}

const available = element => !element.hidden && !element.classList.contains('role-hidden');

function shortenIntros(page) {
  page.querySelectorAll('p.payroll-preview-intro, p.processing-note:not([id]):not([role]), .notice > p').forEach(paragraph => {
    if (paragraph.dataset.shortened || paragraph.textContent.trim().length < 140) return;
    paragraph.dataset.shortened = 'true';
    const short = document.createElement('p');
    short.className = `${paragraph.className} intro-short`;
    short.textContent = firstSentence(paragraph.textContent);
    const more = document.createElement('details');
    more.className = 'intro-more';
    const summary = document.createElement('summary');
    summary.textContent = 'How this works';
    paragraph.replaceWith(short);
    more.append(summary, paragraph);
    short.after(more);
  });
}

export function initPageTabs(root = document) {
  const pages = [...root.querySelectorAll('.page-view[data-tabs]')];
  const state = new Map();

  function showAll() { return window.localStorage.getItem(ALL_KEY) === 'true'; }

  function apply(page) {
    const entry = state.get(page);
    const tabbed = [...page.querySelectorAll('[data-tab]')];
    const keysOf = element => element.dataset.tab.split(/\s+/);
    const usable = entry.tabs.filter(tab => tabbed.some(element => keysOf(element).includes(tab.key) && available(element)
      && (!element.parentElement.closest('[data-tab-group]') || available(element.parentElement.closest('[data-tab-group]')))));
    let active = entry.active;
    if (!usable.some(tab => tab.key === active)) active = usable.length ? usable[0].key : '';
    entry.active = active;
    const all = showAll();
    entry.observer.disconnect();
    tabbed.forEach(element => element.classList.toggle('tab-off', !all && !keysOf(element).includes(active)));
    page.querySelectorAll('[data-tab-group]').forEach(group => {
      const anyOn = [...group.querySelectorAll('[data-tab]')].some(element => !element.classList.contains('tab-off') && available(element));
      group.classList.toggle('tab-off', !all && !anyOn);
    });
    entry.bar.hidden = usable.length < 2 && !all;
    entry.buttons.forEach((button, key) => {
      const tab = entry.tabs.find(item => item.key === key);
      button.hidden = !usable.includes(tab);
      const selected = !all && key === active;
      button.setAttribute('aria-selected', String(selected));
      button.tabIndex = selected || (all && key === usable[0]?.key) ? 0 : -1;
      const count = badgeCount(tabbed.filter(element => keysOf(element).includes(key) && available(element)));
      const badge = button.querySelector('.tab-badge');
      badge.textContent = count ? String(count) : '';
      badge.hidden = !count;
      button.setAttribute('aria-label', count ? `${tab.label} (${count} waiting)` : tab.label);
    });
    entry.allToggle.textContent = all ? 'Show one section at a time' : 'Show all sections';
    entry.allToggle.setAttribute('aria-pressed', String(all));
    entry.observer.observe(page, { subtree: true, childList: true, attributes: true, attributeFilter: ['hidden', 'class'] });
  }

  function select(page, key, focus = false) {
    const entry = state.get(page);
    entry.active = key;
    window.sessionStorage.setItem(TAB_KEY + page.id, key);
    if (showAll()) window.localStorage.setItem(ALL_KEY, 'false');
    pages.forEach(apply);
    if (focus) entry.buttons.get(key).focus();
  }

  pages.forEach(page => {
    shortenIntros(page);
    const tabs = parseTabs(page.dataset.tabs);
    const bar = document.createElement('div');
    bar.className = 'page-tabs';
    const list = document.createElement('div');
    list.className = 'page-tab-list';
    list.setAttribute('role', 'tablist');
    list.setAttribute('aria-label', 'Sections on this page');
    const buttons = new Map();
    tabs.forEach(tab => {
      const button = document.createElement('button');
      button.type = 'button';
      button.className = 'page-tab';
      button.setAttribute('role', 'tab');
      button.dataset.tabKey = tab.key;
      const label = document.createElement('span');
      label.textContent = tab.label;
      const badge = document.createElement('span');
      badge.className = 'tab-badge';
      badge.hidden = true;
      button.append(label, badge);
      button.addEventListener('click', () => select(page, tab.key));
      button.addEventListener('keydown', event => {
        if (!['ArrowRight', 'ArrowLeft', 'Home', 'End'].includes(event.key)) return;
        event.preventDefault();
        const visible = [...buttons.values()].filter(item => !item.hidden);
        const index = visible.indexOf(button);
        const next = event.key === 'Home' ? visible[0] : event.key === 'End' ? visible[visible.length - 1]
          : visible[(index + (event.key === 'ArrowRight' ? 1 : -1) + visible.length) % visible.length];
        select(page, next.dataset.tabKey, true);
      });
      buttons.set(tab.key, button);
      list.append(button);
    });
    const allToggle = document.createElement('button');
    allToggle.type = 'button';
    allToggle.className = 'page-tabs-all';
    allToggle.addEventListener('click', () => {
      window.localStorage.setItem(ALL_KEY, String(!showAll()));
      pages.forEach(apply);
    });
    bar.append(list, allToggle);
    const anchor = page.querySelector(':scope > .notice') || page.querySelector(':scope > .page-heading');
    if (anchor) anchor.after(bar);
    else page.prepend(bar);
    let pending = false;
    const observer = new MutationObserver(() => {
      if (pending) return;
      pending = true;
      window.requestAnimationFrame(() => {
        pending = false;
        apply(page);
      });
    });
    state.set(page, { tabs, bar, buttons, allToggle, observer, active: window.sessionStorage.getItem(TAB_KEY + page.id) || tabs[0]?.key || '' });
  });
  pages.forEach(apply);

  return {
    refresh: () => pages.forEach(apply),
    /** Open the tab holding an element (e.g. after following a link to it). */
    reveal(element) {
      const page = element.closest('.page-view[data-tabs]');
      const holder = element.closest('[data-tab]');
      if (page && holder) select(page, holder.dataset.tab.split(/\s+/)[0]);
    }
  };
}
