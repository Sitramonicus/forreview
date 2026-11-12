
export function buildWorkspaceExport({ mode = 'local-imported', employees = [], leaveRequests = [], tickets = [], attendanceByEmployee = {} } = {}, exportedAt = new Date()) {
  if (!Array.isArray(employees) || !Array.isArray(leaveRequests) || !Array.isArray(tickets)) {
    throw new TypeError('Workspace export collections must be arrays.');
  }
  if (!attendanceByEmployee || typeof attendanceByEmployee !== 'object' || Array.isArray(attendanceByEmployee)) {
    throw new TypeError('Attendance export data must be grouped by employee ID.');
  }
  if (!(exportedAt instanceof Date) || Number.isNaN(exportedAt.getTime())) {
    throw new TypeError('A valid export timestamp is required.');
  }
  const records = [];
  for (const [employeeId, items] of Object.entries(attendanceByEmployee)) {
    if (!Array.isArray(items)) throw new TypeError(`Attendance rows for ${employeeId} must be an array.`);
    items.forEach(item => {
      if (!item || typeof item !== 'object' || Array.isArray(item)) {
        throw new TypeError(`Attendance row for ${employeeId} must be an object.`);
      }
      records.push({ ...item, employeeId });
    });
  }
  return {
    formatVersion: 1,
    exportedAt: exportedAt.toISOString(),
    storageMode: String(mode),
    importedOnly: true,
    hoursCalculated: false,
    payrollIncluded: false,
    employees: employees.slice(),
    attendance: records,
    leaveRequests: leaveRequests.slice(),
    tickets: tickets.slice()
  };
}
