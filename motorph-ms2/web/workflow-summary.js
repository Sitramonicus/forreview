const LABEL_GROUPS = [
  ['Leave request status', 'leaveRequests', 'status'],
  ['Leave request type', 'leaveRequests', 'leaveType'],
  ['Ticket status', 'tickets', 'status'],
  ['Ticket type', 'tickets', 'ticketType'],
  ['Ticket queue label', 'tickets', 'queue']
];

export function workflowSummaryRows(leaveRequests, tickets) {
  const collections = {
    leaveRequests: Array.isArray(leaveRequests) ? leaveRequests : [],
    tickets: Array.isArray(tickets) ? tickets : []
  };
  const rows = [];
  LABEL_GROUPS.forEach(([category, collectionName, field]) => {
    const counts = new Map();
    collections[collectionName].forEach(item => {
      const label = item && typeof item[field] === 'string' ? item[field].trim() : '';
      if (label) counts.set(label, (counts.get(label) || 0) + 1);
    });
    [...counts.entries()].forEach(([label, recordCount]) => rows.push({ category, label, recordCount }));
  });
  return rows.sort((a, b) => a.category.localeCompare(b.category) || a.label.localeCompare(b.label));
}
