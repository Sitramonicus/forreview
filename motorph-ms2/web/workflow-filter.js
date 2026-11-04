export function filterWorkflowItems(items, { query = '', status = '', type = '', typeField = '', searchFields = [], dateRange = null } = {}) {
  if (!Array.isArray(items)) return [];
  const needle = String(query || '').trim().toLowerCase();
  const selectedStatus = String(status || '').trim();
  const selectedType = String(type || '').trim();
  const categoryField = String(typeField || '').trim();
  const fields = Array.isArray(searchFields) ? searchFields : [];
  const from = dateRange ? String(dateRange.from || '') : '';
  const to = dateRange ? String(dateRange.to || '') : '';
  const startField = dateRange && dateRange.startField || 'startDate';
  const endField = dateRange && dateRange.endField || 'endDate';
  if (from && to && from > to) return [];
  return items.filter(item => {
    if (!item || typeof item !== 'object') return false;
    if (selectedStatus && item.status !== selectedStatus) return false;
    if (selectedType && (!categoryField || item[categoryField] !== selectedType)) return false;
    if (from && (typeof item[endField] !== 'string' || item[endField] < from)) return false;
    if (to && (typeof item[startField] !== 'string' || item[startField] > to)) return false;
    if (!needle) return true;
    return fields.some(field => {
      const value = item[field];
      return value !== null && value !== undefined && String(value).toLowerCase().includes(needle);
    });
  });
}
