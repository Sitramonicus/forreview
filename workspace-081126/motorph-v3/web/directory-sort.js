const SORTABLE_FIELDS = new Set(['id', 'name', 'department', 'role', 'location', 'status']);

export function sortDirectoryEntries(items, { field = 'name', direction = 'asc' } = {}) {
  if (!Array.isArray(items)) return [];
  const key = SORTABLE_FIELDS.has(field) ? field : 'name';
  const multiplier = direction === 'desc' ? -1 : 1;
  return [...items].sort((left, right) => {
    const leftValue = String(left?.[key] ?? '');
    const rightValue = String(right?.[key] ?? '');
    const primary = leftValue.localeCompare(rightValue, undefined, { numeric: true, sensitivity: 'base' });
    if (primary) return primary * multiplier;
    return String(left?.id ?? '').localeCompare(String(right?.id ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
  });
}
