
import { isHr } from '../b_basic/Roles.js';

export function checklistProgress(tasks) {
  const done = tasks.filter(task => task.done).length;
  const left = tasks.filter(task => !task.done).map(task => task.task);
  return { text: `${done} of ${tasks.length} done`, left };
}

export function employmentSummary(record) {
  const parts = [record.startDate ? `Started ${record.startDate}` : 'Seeded record'];
  if (record.lastDay) parts.push(`last day ${record.lastDay} (${record.separationReason})`);
  if (record.finalMonth) parts.push(`final payslip: ${record.finalMonth} cutoff ${record.finalCutoff === 1 ? '1–15' : '16–end'}`);
  return parts.join(' · ');
}

export function employeeStats(employees, records, tickets, year) {
  const count = (items, key) => {
    const counts = new Map();
    items.forEach(item => counts.set(item[key], (counts.get(item[key]) || 0) + 1));
    return [...counts.entries()].sort((a, b) => b[1] - a[1] || String(a[0]).localeCompare(String(b[0]))).map(([name, n]) => `${name} ${n}`).join(' · ') || '—';
  };
  const current = employees.filter(employee => employee.status !== 'Separated');
  const inProgress = list => list.length && list.some(task => !task.done);
  return [
    ['HEADCOUNT', `${current.length} current (${employees.length} on record)`],
    ['BY STATUS', count(employees, 'status')],
    ['BY DEPARTMENT', count(current, 'department')],
    ['BY BRANCH', count(current, 'location')],
    [`NEW HIRES ${year}`, String(records.filter(record => String(record.startDate || '').startsWith(String(year))).length)],
    [`SEPARATIONS ${year}`, String(records.filter(record => String(record.lastDay || '').startsWith(String(year))).length)],
    ['ONBOARDING QUEUE', String(records.filter(record => inProgress(record.onboarding || [])).length)],
    ['OFFBOARDING QUEUE', String(records.filter(record => inProgress(record.offboarding || [])).length)],
    ['OPEN HR TICKETS', String(tickets.filter(ticket => ticket.status === 'Open' && ticket.queue === 'HR').length)]
  ];
}

const PERSONAL_PARTS = [['building', 'building'], ['street', 'street'], ['city', 'city'], ['region', 'region'], ['postalCode', 'postal'], ['contactNumber', 'contact']];

export function personalFields(prefix, read = id => document.getElementById(id).value.trim()) {
  return Object.fromEntries(PERSONAL_PARTS.map(([key, suffix]) => [key, read(`${prefix}-${suffix}`)]));
}

export function nameFields(prefix, read = id => document.getElementById(id).value.trim()) {
  return { firstName: read(`${prefix}-first`), surname: read(`${prefix}-surname`), gender: read(`${prefix}-gender`), email: read(`${prefix}-email`) };
}

export function changelogRows(entries) {
  return entries.map(entry => [entry.at.replace('T', ' '), entry.field, entry.before || '—', entry.after || '—', entry.by, entry.via]);
}

export function statBars(employees, key) {
  const counts = new Map();
  employees.filter(employee => employee.status !== 'Separated').forEach(employee => counts.set(employee[key], (counts.get(employee[key]) || 0) + 1));
  return [...counts.entries()].sort((a, b) => b[1] - a[1] || String(a[0]).localeCompare(String(b[0])));
}

function renderBars(container, title, bars) {
  const block = document.createElement('figure');
  block.className = 'stat-chart';
  const caption = document.createElement('figcaption');
  caption.textContent = title;
  const list = document.createElement('ul');
  const max = Math.max(1, ...bars.map(([, n]) => n));
  bars.forEach(([name, n]) => {
    const item = document.createElement('li');
    item.title = `${name}: ${n} ${n === 1 ? 'person' : 'people'}`;
    const label = document.createElement('span');
    label.className = 'stat-chart-label';
    label.textContent = name;
    const track = document.createElement('span');
    track.className = 'stat-chart-track';
    const fill = document.createElement('span');
    fill.className = 'stat-chart-fill';
    fill.style.width = `${(n / max) * 100}%`;
    track.append(fill);
    const value = document.createElement('span');
    value.className = 'stat-chart-value';
    value.textContent = String(n);
    item.append(label, track, value);
    list.append(item);
  });
  block.append(caption, list);
  container.append(block);
}

export function resolvableTickets(tickets, account) {
  if (!account) return [];
  return tickets.filter(ticket => ['Open', 'In progress'].includes(ticket.status) && ticket.employeeId !== account.employeeId
    && (account.role === 'ADMIN' || (account.role === 'HR' && ticket.queue === 'HR')));
}

function fill(select, values, placeholder) {
  const current = select.value;
  select.replaceChildren();
  if (placeholder) {
    const option = document.createElement('option');
    option.value = '';
    option.textContent = placeholder;
    select.append(option);
  }
  values.forEach(([value, label]) => {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = label;
    select.append(option);
  });
  if (values.some(([value]) => value === current)) select.value = current;
}

function say(target, message, isError = false) {
  target.textContent = message;
  target.classList.toggle('payroll-preview-error', isError);
}

export function initHrTools(apiJson, hooks) {
  const onboardForm = document.getElementById('onboard-form');
  if (!onboardForm) return { refresh() {}, refreshTickets() {} };
  const value = id => document.getElementById(id).value.trim();
  const status = document.getElementById('onboard-status');

  async function refreshCatalog() {
    const catalog = await apiJson('/api/hr/catalog');
    fill(document.getElementById('onboard-department'), catalog.departments.map(name => [name, name]));
    fill(document.getElementById('onboard-position'), catalog.positions.map(name => [name, name]));
    document.getElementById('catalog-departments').textContent = catalog.departments.join(' · ');
    document.getElementById('catalog-positions').textContent = catalog.positions.join(' · ');
    const kind = document.getElementById('rename-kind').value;
    fill(document.getElementById('rename-from'), (kind === 'department' ? catalog.departments : catalog.positions).map(name => [name, name]));
  }

  let lastRecords = [];

  function renderStats() {
    const list = document.getElementById('hr-stats');
    list.replaceChildren();
    employeeStats(hooks.getEmployees(), lastRecords, hooks.getTickets(), new Date().getFullYear()).forEach(([label, value]) => {
      const term = document.createElement('dt');
      term.textContent = label;
      const description = document.createElement('dd');
      description.textContent = value;
      list.append(term, description);
    });
    const charts = document.getElementById('hr-stats-charts');
    charts.replaceChildren();
    renderBars(charts, 'People by department', statBars(hooks.getEmployees(), 'department'));
    renderBars(charts, 'People by branch', statBars(hooks.getEmployees(), 'location'));
  }

  async function refreshRecords() {
    
    if (!isHr(hooks.getAccount())) return;
    const tbody = document.getElementById('employment-rows');
    const { records } = await apiJson('/api/hr/employment');
    lastRecords = records;
    renderStats();
    tbody.replaceChildren();
    if (!records.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 4;
      cell.className = 'table-message';
      cell.textContent = 'No onboarding or offboarding yet.';
      row.append(cell);
      tbody.append(row);
    }
    records.forEach(record => {
      const row = document.createElement('tr');
      const person = document.createElement('td');
      person.textContent = `${record.employeeId} — ${record.employeeName}${record.separated ? ' (account closed)' : ''}`;
      const summary = document.createElement('td');
      summary.textContent = employmentSummary(record);
      row.append(person, summary);
      [record.onboarding, record.offboarding].forEach(tasks => {
        const cell = document.createElement('td');
        if (!tasks.length) {
          cell.textContent = '—';
        } else {
          const progress = checklistProgress(tasks);
          const label = document.createElement('span');
          label.textContent = progress.text;
          cell.append(label);
          progress.left.forEach(task => {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'button button-quiet workflow-cancel';
            button.textContent = task;
            button.dataset.employeeId = record.employeeId;
            button.dataset.task = task;
            button.setAttribute('aria-label', `Mark "${task}" done for ${record.employeeName}`);
            cell.append(' ', button);
          });
        }
        row.append(cell);
      });
      tbody.append(row);
    });
  }

  function refreshPickers() {
    const accounts = hooks.getAccounts();
    fill(document.getElementById('onboard-supervisor'), accounts.filter(item => item.role === 'SUPERVISOR').map(item => [item.employeeId, `${item.employeeId} — ${item.name}`]));
    const me = hooks.getAccount();
    fill(document.getElementById('offboard-employee'), hooks.getEmployees()
      .filter(employee => employee.status !== 'Separated' && (!me || employee.id !== me.employeeId))
      .map(employee => [employee.id, `${employee.id} — ${employee.name}`]), 'Choose employee');
  }

  function refreshTickets() {
    renderStats();
    const tickets = resolvableTickets(hooks.getTickets(), hooks.getAccount());
    fill(document.getElementById('resolve-ticket'), tickets.map(ticket => [ticket.id, `${ticket.id} · ${ticket.employeeName}: ${ticket.summary}`]),
      tickets.length ? 'Choose ticket' : 'No open HR tickets');
  }

  async function loadPersonal() {
    const id = document.getElementById('personal-employee').value;
    if (!id) return;
    const record = await apiJson(`/api/personal/${encodeURIComponent(id)}`);
    const { entries } = await apiJson(`/api/changelog?employeeId=${encodeURIComponent(id)}`);
    const tbody = document.getElementById('changelog-rows');
    tbody.replaceChildren();
    const rows = changelogRows(entries);
    if (!rows.length) rows.push(['No changes recorded yet.', '', '', '', '', '']);
    rows.forEach(values => {
      const row = document.createElement('tr');
      values.forEach(value => {
        const cell = document.createElement('td');
        cell.textContent = value;
        row.append(cell);
      });
      tbody.append(row);
    });
    document.getElementById('personal-birthdate').value = record.birthdate;
    document.getElementById('personal-first').value = record.firstName;
    document.getElementById('personal-surname').value = record.surname;
    document.getElementById('personal-gender').value = record.gender;
    document.getElementById('personal-email').value = record.email;
    await showPhoto(document.getElementById('personal-photo'), id, record.hasPhoto);
    PERSONAL_PARTS.forEach(([key, suffix]) => { document.getElementById(`personal-${suffix}`).value = record[key] || ''; });
    say(document.getElementById('personal-status'), record.updatedAt ? `Last changed by ${record.updatedBy} on ${record.updatedAt.slice(0, 10)}.` : 'Not recorded yet.');
  }

  async function showPhoto(img, id, has) {
    if (img.src.startsWith('blob:')) URL.revokeObjectURL(img.src);
    img.hidden = true;
    if (!has) return;
    const blob = await hooks.fetchBlob(`/api/personal/${encodeURIComponent(id)}/photo`);
    img.src = URL.createObjectURL(blob);
    img.hidden = false;
  }

  document.getElementById('personal-photo-upload').addEventListener('click', async () => {
    const target = document.getElementById('personal-photo-status');
    const file = document.getElementById('personal-photo-file').files[0];
    if (!file) return say(target, 'Choose a PNG or JPEG first.', true);
    if (file.size > 150 * 1024) return say(target, 'The photo must be at most 150 KB.', true);
    try {
      const photo = await new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(new Error('The photo could not be read.'));
        reader.readAsDataURL(file);
      });
      await apiJson(`/api/personal/${encodeURIComponent(value('personal-employee'))}/photo`, { photo }, 'PUT');
      say(target, 'Photo saved.');
      await loadPersonal();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  document.getElementById('personal-photo-remove').addEventListener('click', async () => {
    const target = document.getElementById('personal-photo-status');
    try {
      await apiJson(`/api/personal/${encodeURIComponent(value('personal-employee'))}/photo/remove`, {});
      say(target, 'Photo removed.');
      await loadPersonal();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  document.getElementById('personal-employee').addEventListener('change', () => loadPersonal().catch(error => say(document.getElementById('personal-status'), error.message, true)));
  document.getElementById('personal-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('personal-status');
    try {
      await apiJson(`/api/personal/${encodeURIComponent(value('personal-employee'))}`, { ...personalFields('personal'), ...nameFields('personal'), birthdate: value('personal-birthdate') }, 'PUT');
      say(target, 'Saved. The employee was notified.');
    } catch (error) {
      say(target, error.message, true);
    }
  });

  async function refresh() {
    try {
      refreshPickers();
      fill(document.getElementById('personal-employee'), hooks.getEmployees().filter(employee => employee.status !== 'Separated')
        .map(employee => [employee.id, `${employee.id} — ${employee.name}`]), 'Choose employee');
      await Promise.all([refreshCatalog(), refreshRecords()]);
    } catch (error) {
      say(status, error.message, true);
    }
  }

  onboardForm.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const { employee } = await apiJson('/api/hr/onboard', { name: `${value('onboard-first')} ${value('onboard-surname')}`.trim(), department: value('onboard-department'),
        position: value('onboard-position'), location: value('onboard-location'), startDate: value('onboard-start'),
        supervisorId: value('onboard-supervisor'), monthlyBasic: value('onboard-basic'), riceAllowance: value('onboard-rice'),
        phoneAllowance: value('onboard-phone'), clothingAllowance: value('onboard-clothing'), payMethod: value('onboard-pay-method'),
        birthdate: value('onboard-birthdate'), ...personalFields('onboard'), ...nameFields('onboard') });
      say(status, `Onboarded ${employee.name} as ${employee.id}. They can now sign in as an Employee; finish the checklist below.`);
      onboardForm.reset();
      await hooks.reloadDirectory();
      await refresh();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('offboard-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('offboard-status');
    try {
      const { record } = await apiJson(`/api/hr/employment/${encodeURIComponent(value('offboard-employee'))}/offboard`,
        { lastDay: value('offboard-last-day'), reason: value('offboard-reason') });
      say(target, `${record.employeeName}: last day ${record.lastDay}. Pay stops after it; the account closes the day after. Final payslip: ${record.finalMonth} cutoff ${record.finalCutoff === 1 ? '1–15' : '16–end'} (Payroll finalizes and releases it).`);
      await hooks.reloadDirectory();
      await refresh();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  document.getElementById('employment-rows').addEventListener('click', async event => {
    const button = event.target.closest('button[data-task]');
    if (!button) return;
    try {
      await apiJson(`/api/hr/employment/${encodeURIComponent(button.dataset.employeeId)}/tasks`, { task: button.dataset.task });
      await refreshRecords();
    } catch (error) {
      say(status, error.message, true);
    }
  });

  document.getElementById('catalog-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('catalog-status');
    try {
      await apiJson('/api/hr/catalog', { kind: value('catalog-kind'), name: value('catalog-name') });
      say(target, `Added ${value('catalog-kind')} "${value('catalog-name')}".`);
      await refreshCatalog();
    } catch (error) {
      say(target, error.message, true);
    }
  });
  document.getElementById('rename-kind').addEventListener('change', () => refreshCatalog().catch(() => {}));
  document.getElementById('rename-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('catalog-status');
    try {
      const result = await apiJson('/api/hr/catalog/rename', { kind: value('rename-kind'), from: value('rename-from'), to: value('rename-to') });
      say(target, `Renamed to "${result.renamed}"; ${result.employeesUpdated} employee record(s) updated.`);
      await refreshCatalog();
      await hooks.reloadDirectory();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  document.getElementById('resolve-form').addEventListener('submit', async event => {
    event.preventDefault();
    const target = document.getElementById('resolve-status');
    try {
      const { item } = await apiJson(`/api/workflows/tickets/${encodeURIComponent(value('resolve-ticket'))}/resolve`, { reply: value('resolve-reply') });
      say(target, `Resolved ${item.id}; ${item.employeeName} sees your reply.`);
      document.getElementById('resolve-reply').value = '';
      await hooks.reloadWorkflows();
    } catch (error) {
      say(target, error.message, true);
    }
  });

  return { refresh, refreshTickets };
}
