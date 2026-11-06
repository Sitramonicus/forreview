export function directoryLabelSummaryRows(employees) {
  if (!Array.isArray(employees)) return [];
  const counts = new Map();
  employees.forEach(employee => {
    if (!employee || typeof employee !== 'object') return;
    [['Status label', employee.status], ['Department label', employee.department]].forEach(([category, label]) => {
      if (typeof label !== 'string' || !label.trim()) return;
      const key = `${category}\u0000${label}`;
      counts.set(key, (counts.get(key) || 0) + 1);
    });
  });
  return [...counts.entries()]
    .map(([key, count]) => {
      const [category, label] = key.split('\u0000');
      return { category, label, employeeRecords: count };
    })
    .sort((a, b) => a.category.localeCompare(b.category) || a.label.localeCompare(b.label));
}
