
export function contactFor(employee) {
  const handle = String(employee.name || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
    .replace(/[^a-z\s]/g, '').trim().split(/\s+/).filter(Boolean).join('.');
  const digits = String(employee.id || '').replace(/[^0-9A-F]/gi, '').slice(-3).toUpperCase();
  return { email: `${handle || 'employee'}@motorph.example`, extension: `local ${digits || '000'}` };
}

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

export function leaveEventText(request) {
  const first = String(request.employeeName || request.employeeId || '').split(/\s+/)[0];
  return request.status === 'Approved' ? `${first} · ${request.leaveType}` : `${first} · ${request.leaveType} (${String(request.status).toLowerCase()})`;
}
