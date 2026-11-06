function hasQuery(record, fields, query) {
  return fields.some(field => {
    const value = record && record[field];
    return value !== null && value !== undefined && String(value).toLowerCase().includes(query);
  });
}

/** Searches only the explicitly enumerated synthetic directory, raw, and stored workflow fields. */
export function searchSandboxRecords(query, { employees = [], attendanceByEmployee = {}, leaveRequests = [], tickets = [] } = {}) {
  const needle = String(query || '').trim().toLowerCase();
  if (!needle) return { employees: [], attendance: [], leaveRequests: [], tickets: [] };
  const employeeItems = Array.isArray(employees) ? employees : [];
  const leaveItems = Array.isArray(leaveRequests) ? leaveRequests : [];
  const ticketItems = Array.isArray(tickets) ? tickets : [];
  const attendanceGroups = attendanceByEmployee && typeof attendanceByEmployee === 'object' && !Array.isArray(attendanceByEmployee)
    ? attendanceByEmployee : {};
  const attendance = Object.entries(attendanceGroups).flatMap(([employeeId, rows]) =>
    (Array.isArray(rows) ? rows : []).map(record => ({ ...record, employeeId }))
      .filter(record => hasQuery(record,
        ['employeeId', 'attendanceId', 'scheduleId', 'date', 'timeIn', 'timeOut', 'dayType'], needle)));

  return {
    employees: employeeItems.filter(record => hasQuery(record,
      ['id', 'name', 'department', 'role', 'location', 'status'], needle)),
    attendance,
    leaveRequests: leaveItems.filter(record => hasQuery(record,
      ['id', 'employeeId', 'employeeName', 'leaveType', 'startDate', 'endDate', 'status', 'note'], needle)),
    tickets: ticketItems.filter(record => hasQuery(record,
      ['id', 'employeeId', 'employeeName', 'ticketType', 'queue', 'summary', 'status'], needle))
  };
}
