export function filterAttendanceCoverage(items, { query = '', mapping = 'all' } = {}) {
  if (!Array.isArray(items)) return [];
  const normalizedQuery = String(query).trim().toLocaleLowerCase();
  return items.filter(item => {
    if (!item || typeof item !== 'object') return false;
    if (typeof item.employeeId !== 'string' && typeof item.employeeName !== 'string') return false;
    const hasMapping = item.hasScheduleMapping === true;
    if (mapping === 'mapped' && !hasMapping) return false;
    if (mapping === 'unmapped' && hasMapping) return false;
    if (!['all', 'mapped', 'unmapped'].includes(mapping)) return false;
    if (!normalizedQuery) return true;
    const searchable = [item.employeeId, item.employeeName,
      hasMapping ? `schedule ${item.scheduleId}` : 'no mapping']
      .filter(value => value !== undefined && value !== null)
      .join(' ')
      .toLocaleLowerCase();
    return searchable.includes(normalizedQuery);
  });
}
