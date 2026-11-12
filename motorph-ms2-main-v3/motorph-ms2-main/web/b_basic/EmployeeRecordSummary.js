
export function employeeRecordSummary(employees, attendanceSummaries = [], leaveRequests = [], tickets = []) {
  if (!Array.isArray(employees)) return [];
  const attendanceByEmployee = new Map((Array.isArray(attendanceSummaries) ? attendanceSummaries : [])
    .filter(item => item && typeof item.employeeId === 'string')
    .map(item => [item.employeeId, item]));
  const countByEmployee = records => {
    const counts = new Map();
    (Array.isArray(records) ? records : []).forEach(item => {
      if (item && typeof item.employeeId === 'string') counts.set(item.employeeId, (counts.get(item.employeeId) || 0) + 1);
    });
    return counts;
  };
  const leaveCounts = countByEmployee(leaveRequests);
  const ticketCounts = countByEmployee(tickets);
  return employees.filter(employee => employee && typeof employee.id === 'string').map(employee => {
    const attendance = attendanceByEmployee.get(employee.id);
    return {
      employeeId: employee.id,
      employeeName: String(employee.name || employee.id),
      department: String(employee.department || ''),
      hasScheduleMapping: Boolean(attendance && attendance.hasScheduleMapping),
      rawRecordCount: attendance && Number.isInteger(attendance.rawRecordCount) ? attendance.rawRecordCount : 0,
      firstRecordDate: attendance && typeof attendance.firstRecordDate === 'string' ? attendance.firstRecordDate : '',
      lastRecordDate: attendance && typeof attendance.lastRecordDate === 'string' ? attendance.lastRecordDate : '',
      leaveRequestCount: leaveCounts.get(employee.id) || 0,
      ticketCount: ticketCounts.get(employee.id) || 0
    };
  });
}
