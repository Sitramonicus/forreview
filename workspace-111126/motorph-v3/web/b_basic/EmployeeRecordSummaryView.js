const SORT_KEYS = new Set(['employeeName', 'rawRecordCount', 'firstRecordDate', 'lastRecordDate', 'leaveRequestCount', 'ticketCount']);

export function filterAndSortEmployeeRecordSummary(rows, { query = '', mapping = 'all', sortBy = 'employeeName', direction = 'asc' } = {}) {
  if (!Array.isArray(rows)) return [];
  const needle = String(query || '').trim().toLowerCase();
  const selectedMapping = ['all', 'mapped', 'unmapped'].includes(mapping) ? mapping : 'all';
  const field = SORT_KEYS.has(sortBy) ? sortBy : 'employeeName';
  const sign = direction === 'desc' ? -1 : 1;
  return rows.filter(row => {
    if (!row || typeof row !== 'object') return false;
    if (selectedMapping === 'mapped' && row.hasScheduleMapping !== true) return false;
    if (selectedMapping === 'unmapped' && row.hasScheduleMapping !== false) return false;
    if (!needle) return true;
    return [row.employeeId, row.employeeName, row.department]
      .some(value => value !== null && value !== undefined && String(value).toLowerCase().includes(needle));
  }).slice().sort((a, b) => {
    const left = a[field];
    const right = b[field];
    let result;
    if (typeof left === 'number' && typeof right === 'number') result = left - right;
    else result = String(left || '').localeCompare(String(right || ''), undefined, { sensitivity: 'base', numeric: true });
    return result === 0 ? String(a.employeeId).localeCompare(String(b.employeeId), undefined, { numeric: true }) : sign * result;
  });
}
