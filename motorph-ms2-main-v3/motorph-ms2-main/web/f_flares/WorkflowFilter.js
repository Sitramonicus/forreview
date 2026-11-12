export function filterWorkflowItems(wf_aItems, { query = '', status = '', type = '', typeField = '', searchFields = [], dateRange = null } = {}) {
  if (!Array.isArray(wf_aItems)) return [];
  const wf_sNeedle = String(query || '').trim().toLowerCase();
  const wf_sStatus = String(status || '').trim();
  const wf_sType = String(type || '').trim();
  const wf_sTypeField = String(typeField || '').trim();
  const wf_aFields = Array.isArray(searchFields) ? searchFields : [];
  const wf_sFrom = dateRange ? String(dateRange.from || '') : '';
  const wf_sTo = dateRange ? String(dateRange.to || '') : '';
  const wf_sStartField = dateRange && dateRange.startField || 'startDate';
  const wf_sEndField = dateRange && dateRange.endField || 'endDate';
  if (wf_sFrom && wf_sTo && wf_sFrom > wf_sTo) return [];
  return wf_aItems.filter(wf_oItem => {
    if (!wf_oItem || typeof wf_oItem !== 'object') return false;
    if (wf_sStatus && wf_oItem.status !== wf_sStatus) return false;
    if (wf_sType && (!wf_sTypeField || wf_oItem[wf_sTypeField] !== wf_sType)) return false;
    if (wf_sFrom && (typeof wf_oItem[wf_sEndField] !== 'string' || wf_oItem[wf_sEndField] < wf_sFrom)) return false;
    if (wf_sTo && (typeof wf_oItem[wf_sStartField] !== 'string' || wf_oItem[wf_sStartField] > wf_sTo)) return false;
    if (!wf_sNeedle) return true;
    return wf_aFields.some(wf_sField => {
      const wf_vValue = wf_oItem[wf_sField];
      return wf_vValue !== null && wf_vValue !== undefined && String(wf_vValue).toLowerCase().includes(wf_sNeedle);
    });
  });
}
