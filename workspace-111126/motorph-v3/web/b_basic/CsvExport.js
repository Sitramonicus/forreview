function validateColumns(cs_aCols) {
  if (!Array.isArray(cs_aCols) || cs_aCols.length === 0) throw new TypeError('CSV must include at least one column.');
  cs_aCols.forEach((cs_oCol, cs_iIdx) => {
    if (!cs_oCol || typeof cs_oCol.key !== 'string' || !cs_oCol.key.trim()
      || typeof cs_oCol.label !== 'string' || !cs_oCol.label.trim()) {
      throw new TypeError(`CSV column ${cs_iIdx + 1} must include a key and label.`);
    }
  });
}

function csvCell(cs_vValue) {
  const cs_sText = String(cs_vValue ?? '');
  // a leading = + - or @ makes a spreadsheet treat the cell as a formula
  const cs_sSafe = /^[\s]*[=+\-@]/u.test(cs_sText) ? `'${cs_sText}` : cs_sText;
  return `"${cs_sSafe.replaceAll('"', '""')}"`;
}

export function buildCsv(cs_aCols, cs_aRows) {
  validateColumns(cs_aCols);
  if (!Array.isArray(cs_aRows)) throw new TypeError('CSV rows must be an array.');
  cs_aRows.forEach((cs_oRow, cs_iIdx) => {
    if (!cs_oRow || typeof cs_oRow !== 'object' || Array.isArray(cs_oRow)) {
      throw new TypeError(`CSV row ${cs_iIdx + 1} must be an object.`);
    }
  });
  const cs_sHead = cs_aCols.map(cs_oCol => csvCell(cs_oCol.label)).join(',');
  const cs_sBody = cs_aRows.map(cs_oRow => cs_aCols.map(cs_oCol => csvCell(cs_oRow[cs_oCol.key])).join(','));
  return [cs_sHead, ...cs_sBody].join('\r\n');
}
