const FIELDS = new Set(['employeeId', 'employeeName', 'scheduleId', 'rawRecordCount', 'firstRecordDate', 'lastRecordDate']);
const NUMERIC_FIELDS = new Set(['scheduleId', 'rawRecordCount']);

export function sortAttendanceCoverage(items, { field = 'employeeId', direction = 'asc' } = {}) {
  if (!Array.isArray(items)) return [];
  const key = FIELDS.has(field) ? field : 'employeeId';
  const multiplier = direction === 'desc' ? -1 : 1;
  return [...items].sort((left, right) => {
    const leftValue = left?.[key];
    const rightValue = right?.[key];
    let primary;
    if (NUMERIC_FIELDS.has(key)) {
      const a = Number.isFinite(Number(leftValue)) ? Number(leftValue) : Number.NEGATIVE_INFINITY;
      const b = Number.isFinite(Number(rightValue)) ? Number(rightValue) : Number.NEGATIVE_INFINITY;
      primary = a === b ? 0 : a < b ? -1 : 1;
    } else {
      primary = String(leftValue ?? '').localeCompare(String(rightValue ?? ''), undefined,
        { numeric: true, sensitivity: 'base' });
    }
    if (primary) return primary * multiplier;
    return String(left?.employeeId ?? '').localeCompare(String(right?.employeeId ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
  });
}
