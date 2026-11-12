const wf_sFields = new Set([
  'id', 'employeeName', 'status', 'submittedAt', 'leaveType', 'startDate', 'endDate', 'ticketType', 'queue'
]);

export function sortWorkflowItems(wf_aItems, { field = 'submittedAt', direction = 'desc' } = {}) {
  if (!Array.isArray(wf_aItems)) return [];
  const wf_sKey = wf_sFields.has(field) ? field : 'submittedAt';
  const wf_iSign = direction === 'asc' ? 1 : -1;
  return [...wf_aItems].sort((wf_oLeft, wf_oRight) => {
    const wf_iPrimary = String(wf_oLeft?.[wf_sKey] ?? '').localeCompare(String(wf_oRight?.[wf_sKey] ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
    if (wf_iPrimary) return wf_iPrimary * wf_iSign;
    return String(wf_oLeft?.id ?? '').localeCompare(String(wf_oRight?.id ?? ''), undefined,
      { numeric: true, sensitivity: 'base' });
  });
}
