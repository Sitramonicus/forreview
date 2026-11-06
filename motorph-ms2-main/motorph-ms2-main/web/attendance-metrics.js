/** Requirement 3: labels for the attendance metrics the Java API returns with GET /api/attendance (nothing is calculated here). */
export function minutesText(minutes) {
  const total = Number(minutes) || 0;
  const hours = Math.floor(total / 60);
  const rest = total % 60;
  if (!hours) return `${rest} min`;
  return rest ? `${hours} h ${rest} min` : `${hours} h`;
}

export function metricsRows(metrics) {
  if (!metrics || !metrics.from) return [];
  const credits = (metrics.leaveCredits || []).map(item => `${item.leaveType} ${item.remaining} left`).join(' · ');
  return [
    ['DAYS PRESENT', String(metrics.presentDays)],
    ['DAYS ABSENT', String(metrics.absentDays)],
    ['DAYS TARDY', metrics.tardyDays ? `${metrics.tardyDays} (${minutesText(metrics.tardyMinutes)} late)` : '0'],
    ['OVERTIME (APPROVED)', minutesText(metrics.overtimeMinutes) + (metrics.unapprovedExtraMinutes ? ` · ${minutesText(metrics.unapprovedExtraMinutes)} extra not approved` : '')],
    ['UNDERTIME', minutesText(metrics.undertimeMinutes)],
    ['LEAVE TAKEN', `${metrics.leaveDays} day(s)${metrics.holidayDays ? ` · ${metrics.holidayDays} holiday(s)` : ''}`],
    [`LEAVE CREDITS ${metrics.creditYear}`, credits || '—']
  ];
}
