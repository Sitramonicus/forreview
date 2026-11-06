const FIELDS = new Set(['date', 'attendanceId', 'scheduleId', 'timeIn', 'timeOut', 'dayType']);
const NUMERIC_FIELDS = new Set(['attendanceId', 'scheduleId']);

export function sortAttendanceRecords(items, { field = 'date', direction = 'asc' } = {}) {
  if (!Array.isArray(items)) return [];
  const key = FIELDS.has(field) ? field : 'date';
  const multiplier = direction === 'desc' ? -1 : 1;
  return [...items].sort((left, right) => {
    let primary;
    if (NUMERIC_FIELDS.has(key)) {
      const a = Number(left?.[key] ?? 0);
      const b = Number(right?.[key] ?? 0);
      primary = a === b ? 0 : a < b ? -1 : 1;
    } else {
      primary = String(left?.[key] ?? '').localeCompare(String(right?.[key] ?? ''), undefined,
        { numeric: true, sensitivity: 'base' });
    }
    if (primary) return primary * multiplier;
    return Number(left?.attendanceId ?? 0) - Number(right?.attendanceId ?? 0);
  });
}
