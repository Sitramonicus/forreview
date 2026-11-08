/** Branch page rows (GET /api/branches): counts only, no names. */
export function branchRows(payload) {
  if (!payload || !Array.isArray(payload.branches)) throw new TypeError('Unexpected branch response.');
  return payload.branches.map(branch => [branch.name === payload.mine ? `${branch.name} (yours)` : branch.name, String(branch.headcount), String(branch.active),
    String(branch.onLeaveToday), String(branch.clockedInNow), String(branch.onsiteToday)]);
}
