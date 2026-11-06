/**
 * Requirements 6 and 7: contact info and today's availability for the team roster.
 * shortcut: the directory has no contact fields yet, so each person gets a synthetic work email
 * (@motorph.example, a reserved test domain) and a desk extension; add real contact fields with HR onboarding if needed.
 */
export function contactFor(employee) {
  const handle = String(employee.name || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z\s]/g, '').trim().split(/\s+/).filter(Boolean).join('.');
  const digits = String(employee.id || '').replace(/[^0-9A-F]/gi, '').slice(-3).toUpperCase();
  return { email: `${handle || 'employee'}@motorph.example`, extension: `local ${digits || '000'}` };
}

/** today is YYYY-MM-DD. Approved leave covering today wins; then the stored directory status. */
export function availabilityFor(employee, leaveRequests, today) {
  if (employee.status === 'Separated') return 'Separated';
  const leave = (leaveRequests || []).find(request => request.employeeId === employee.id && request.status === 'Approved'
    && request.startDate <= today && today <= request.endDate);
  if (leave) return `On leave today (${leave.leaveType}, until ${leave.endDate})`;
  if (employee.status === 'On leave') return 'On leave';
  const upcoming = (leaveRequests || []).filter(request => request.employeeId === employee.id && request.status === 'Approved' && request.startDate > today)
    .sort((a, b) => a.startDate.localeCompare(b.startDate))[0];
  return upcoming ? `Available · leave from ${upcoming.startDate}` : 'Available';
}

/** The leave calendar shows whose leave it is; pending or turned-down requests say so. */
export function leaveEventText(request) {
  const first = String(request.employeeName || request.employeeId || '').split(/\s+/)[0];
  return request.status === 'Approved' ? `${first} · ${request.leaveType}` : `${first} · ${request.leaveType} (${String(request.status).toLowerCase()})`;
}
