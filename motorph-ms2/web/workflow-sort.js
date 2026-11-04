const SORTABLE_FIELDS = new Set([
  'id', 'employeeName', 'status', 'submittedAt', 'leaveType', 'startDate', 'endDate', 'ticketType', 'queue'
]);

export function sortWorkflowItems(items, { field = 'submittedAt', direction = 'desc' } = {}) {
  if (!Array.isArray(items)) return [];
  const key = SORTABLE_FIELDS.has(field) ? field : 'submittedAt';
  const multiplier = direction === 'asc' ? 1 : -1;
  return [...items].sort((left, right) => {
    const primary = String(left?.[key] ?? '').localeCompare(String(right?.[key] ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
    if (primary) return primary * multiplier;
    return String(left?.id ?? '').localeCompare(String(right?.id ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
  });
}
