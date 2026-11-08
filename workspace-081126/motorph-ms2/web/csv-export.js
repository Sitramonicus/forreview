function validateColumns(columns) {
  if (!Array.isArray(columns) || columns.length === 0) throw new TypeError('CSV must include at least one column.');
  columns.forEach((column, index) => {
    if (!column || typeof column.key !== 'string' || !column.key.trim()
      || typeof column.label !== 'string' || !column.label.trim()) {
      throw new TypeError(`CSV column ${index + 1} must include a key and label.`);
    }
  });
}

function csvCell(value) {
  const text = String(value ?? '');
  // Prefix spreadsheet formula-like cells with an apostrophe before quoting.
  // This includes formula markers preceded by whitespace, tabs, or line breaks.
  const safeText = /^[\s]*[=+\-@]/u.test(text) ? `'${text}` : text;
  return `"${safeText.replaceAll('"', '""')}"`;
}

export function buildCsv(columns, rows) {
  validateColumns(columns);
  if (!Array.isArray(rows)) throw new TypeError('CSV rows must be an array.');
  rows.forEach((row, index) => {
    if (!row || typeof row !== 'object' || Array.isArray(row)) {
      throw new TypeError(`CSV row ${index + 1} must be an object.`);
    }
  });
  const header = columns.map(column => csvCell(column.label)).join(',');
  const body = rows.map(row => columns.map(column => csvCell(row[column.key])).join(','));
  return [header, ...body].join('\r\n');
}
