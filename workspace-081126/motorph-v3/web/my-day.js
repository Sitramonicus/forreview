import { buildCutoffPayroll } from './payroll-cutoff.js';
import { formatPesoCents } from './payroll-preview-2026.js';

/** Time-clock wording for GET/POST /api/attendance/clock. button is null once today's row is recorded. */
export function clockText(status) {
  const tag = status.attendanceTag ? ` · ${status.attendanceTag}` : '';
  if (status.clockedIn) return { state: 'Clocked in', detail: `Since ${status.since} today (${status.today})${tag}.`, button: 'Clock out' };
  if (status.todayRecord) {
    const hours = status.workHourTag ? ` · ${status.workHourTag} hours` : '';
    return { state: 'Done for today', detail: `In ${status.todayRecord.timeIn} · out ${status.todayRecord.timeOut} (${status.today})${tag}${hours}.`, button: null };
  }
  return { state: 'Clocked out', detail: `Not clocked in yet today (${status.today}).`, button: 'Clock in' };
}

/** The pop-up before clocking in on a rest day, holiday or leave day (design doc: "prompts if it's a rest day/holiday/leave"). */
export function clockInQuestion(status) {
  if (!status.dayType || status.dayType === 'Workday') return null;
  return `Today is a ${status.dayType.replace(/^On approved leave/, 'day on approved leave')} for you. Clock in anyway?`;
}

/** Offsite/onsite line under the time clock (requirement 2). */
export function locationText(status) {
  if (status.location === 'Onsite') return `Onsite · QR scanned at the branch at ${status.onsiteSince}`;
  if (status.clockedIn) return 'Offsite · clocked in from the app. Scan the QR code at the branch to check in onsite.';
  return status.todayRecord ? 'Offsite (no branch scan today)' : '';
}

/**
 * A 21x21 QR-style picture for the code: the three corner squares of a QR code plus a pattern from the code's letters.
 * shortcut: it only looks like a QR code (no error correction, not scannable); use a QR library for real kiosks.
 */
export function qrPattern(code) {
  const size = 21;
  let seed = 0;
  for (const letter of String(code)) seed = (seed * 31 + letter.charCodeAt(0)) >>> 0;
  const finder = (r, c) => {
    for (const [top, left] of [[0, 0], [0, size - 7], [size - 7, 0]]) {
      const y = r - top, x = c - left;
      if (y >= 0 && y < 7 && x >= 0 && x < 7) return y === 0 || y === 6 || x === 0 || x === 6 || (y >= 2 && y <= 4 && x >= 2 && x <= 4) ? 1 : 0;
      if (y >= -1 && y <= 7 && x >= -1 && x <= 7) return 0;
    }
    return null;
  };
  return Array.from({ length: size }, (_, r) => Array.from({ length: size }, (_, c) => {
    const fixed = finder(r, c);
    if (fixed !== null) return fixed;
    seed = (seed * 1103515245 + 12345) >>> 0;
    return (seed >>> 16) & 1;
  }));
}

function qrSvg(code) {
  const grid = qrPattern(code);
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '-1 -1 23 23');
  svg.setAttribute('role', 'img');
  svg.setAttribute('aria-label', `Demo QR picture for ${code} (not scannable)`);
  const background = document.createElementNS(svg.namespaceURI, 'rect');
  background.setAttribute('x', '-1');
  background.setAttribute('y', '-1');
  background.setAttribute('width', '23');
  background.setAttribute('height', '23');
  background.setAttribute('fill', '#fff');
  svg.append(background);
  grid.forEach((row, r) => row.forEach((on, c) => {
    if (!on) return;
    const cell = document.createElementNS(svg.namespaceURI, 'rect');
    cell.setAttribute('x', String(c));
    cell.setAttribute('y', String(r));
    cell.setAttribute('width', '1');
    cell.setAttribute('height', '1');
    cell.setAttribute('fill', '#0b1736');
    svg.append(cell);
  }));
  return svg;
}

/** The pop-up questions before clocking out at `now` (HH:mm): always confirm, and warn again before the shift ends. */
export function clockOutQuestions(clock) {
  const questions = ['Clock out now? Today’s attendance will be recorded and can only be changed by your supervisor.'];
  if (clock.now && clock.shiftEnd && clock.now.slice(0, 5) < clock.shiftEnd) {
    questions.push(`Your shift ends at ${clock.shiftEnd}. Clocking out now counts as undertime. Clock out early anyway?`);
  }
  return questions;
}

/** Salary tracker for the cutoff containing `today`, from the Java cutoff payroll (nothing is calculated here). */
export function trackerSummary(payload, today, asOf = '') {
  const result = buildCutoffPayroll(payload);
  const index = Number(today.slice(8, 10)) <= 15 ? 0 : 1;
  const cutoff = result.cutoffs[index];
  const extras = [];
  if (payload.cutoffs[index].leaveDays) extras.push(`${payload.cutoffs[index].leaveDays} paid leave day(s)`);
  if (payload.cutoffs[index].holidayDays) extras.push(`${payload.cutoffs[index].holidayDays} paid holiday(s)`);
  return {
    period: `${cutoff.label} ${result.month} (${cutoff.from} – ${cutoff.to})`,
    figures: [
      ['PAID HOURS SO FAR', `${cutoff.hours} h over ${cutoff.daysWithRecords} day(s)${extras.length ? ` + ${extras.join(', ')}` : ''}`],
      ['GROSS SO FAR', formatPesoCents(cutoff.grossCents)],
      ['EST. DEDUCTIONS', `−${formatPesoCents(cutoff.deductionsCents)}`],
      ['EST. NET THIS CUTOFF', cutoff.netCents === null ? 'Not enough pay yet to cover this cutoff’s deductions' : formatPesoCents(cutoff.netCents)],
      ['MONTH GROSS SO FAR', formatPesoCents(result.monthlyGrossCents)]
    ],
    asOf: asOf ? `Estimate as of ${today} ${asOf.slice(0, 5)}` : '',
    note: `${asOf ? `Estimate as of ${today} ${asOf.slice(0, 5)}. ` : ''}Rate ₱${result.hourlyRate}/h. Estimate from attendance, approved overtime, paid leave and holidays up to today; deductions are half of the month's so far and change as the month goes on. Payroll Staff finalize the actual payslip.`
  };
}

export function initMyDay(apiJson) {
  const panel = document.getElementById('my-day-panel');
  const button = document.getElementById('clock-button');
  const status = document.getElementById('clock-status');
  let action = 'in';
  let current = null;

  function showError(target, error, fallback) {
    target.textContent = error instanceof Error ? error.message : fallback;
  }

  async function refreshTracker(clock) {
    const figures = document.getElementById('tracker-figures');
    try {
      const payload = await apiJson(`/api/payroll/cutoffs?employeeId=${encodeURIComponent(clock.employeeId)}&month=${clock.today.slice(0, 7)}&through=${clock.today}`);
      const view = trackerSummary(payload, clock.today, clock.now || '');
      document.getElementById('tracker-period').textContent = view.period;
      figures.replaceChildren();
      view.figures.forEach(([label, value]) => {
        const term = document.createElement('dt');
        term.textContent = label;
        const description = document.createElement('dd');
        description.textContent = value;
        figures.append(term, description);
      });
      document.getElementById('tracker-note').textContent = view.note;
    } catch (error) {
      figures.replaceChildren();
      showError(document.getElementById('tracker-note'), error, 'Could not load the salary tracker.');
    }
  }

  function render(clock) {
    const view = clockText(clock);
    document.getElementById('clock-state').textContent = view.state;
    document.getElementById('clock-detail').textContent = view.detail;
    button.hidden = !view.button;
    if (view.button) button.textContent = view.button;
    action = clock.clockedIn ? 'out' : 'in';
    current = clock;
    document.getElementById('clock-location').textContent = locationText(clock);
    const qr = document.getElementById('onsite-qr');
    qr.hidden = !clock.qrCode;
    document.getElementById('onsite-new-qr').hidden = !(clock.clockedIn && clock.location !== 'Onsite' && !clock.qrCode);
    if (clock.qrCode) {
      document.getElementById('onsite-qr-image').replaceChildren(qrSvg(clock.qrCode));
      document.getElementById('onsite-qr-code').textContent = clock.qrCode;
      document.getElementById('onsite-qr-expiry').textContent = `Valid until ${clock.qrExpiresAt}`;
    }
  }

  async function sendClock(body, done) {
    try {
      const clock = await apiJson('/api/attendance/clock', body);
      render(clock);
      status.textContent = done(clock);
    } catch (error) {
      showError(status, error, 'Could not update the time clock.');
      // An expired code: show the current state, which offers a new QR code.
      apiJson('/api/attendance/clock').then(render).catch(() => {});
    }
  }

  document.getElementById('onsite-scan').addEventListener('click', () =>
    sendClock({ action: 'onsite', code: current ? current.qrCode : '' }, clock => `Checked in onsite at ${clock.onsiteSince}.`));
  document.getElementById('onsite-new-qr').addEventListener('click', () =>
    sendClock({ action: 'qr' }, () => 'New QR code shown; it is valid for 10 minutes.'));

  async function refresh() {
    panel.hidden = false;
    try {
      const clock = await apiJson('/api/attendance/clock');
      render(clock);
      await refreshTracker(clock);
    } catch (error) {
      showError(status, error, 'Could not load the time clock.');
    }
  }

  button.addEventListener('click', async () => {
    if (action === 'in') {
      const latest = await apiJson('/api/attendance/clock').catch(() => current);
      const question = clockInQuestion(latest || {});
      if (question && !window.confirm(question)) {
        status.textContent = 'Not clocked in.';
        return;
      }
    }
    if (action === 'out') {
      // Ask with the current time, not the time the page loaded.
      const latest = await apiJson('/api/attendance/clock').catch(() => current);
      if (!clockOutQuestions(latest || {}).every(question => window.confirm(question))) {
        status.textContent = 'Still clocked in.';
        return;
      }
    }
    button.disabled = true;
    try {
      const clock = await apiJson('/api/attendance/clock', { action });
      render(clock);
      status.textContent = clock.record ? `Clocked out at ${clock.record.timeOut}. Today's attendance is recorded.` : `Clocked in at ${clock.since}.`;
      await refreshTracker(clock);
    } catch (error) {
      showError(status, error, 'Could not update the time clock.');
    } finally {
      button.disabled = false;
    }
  });

  return { refresh };
}
