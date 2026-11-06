/** Local-demo overtime requests: submit, then a different employee approves or rejects. Java enforces every rule. */

/** Table rows for overtime requests; the reviewer can act only on other people's pending requests. */
export function overtimeRows(requests, reviewerId, mayReview = () => true) {
  if (!Array.isArray(requests)) throw new TypeError('Unexpected overtime response.');
  return requests.map(request => ({
    id: request.id,
    cells: [request.id, `${request.employeeId} — ${request.employeeName}`, request.date,
      `${(request.minutes / 60).toFixed(2)} h`, request.reviewerId ? `${request.status} by ${request.reviewerId}` : request.status],
    canReview: Boolean(reviewerId) && request.status === 'Pending review' && request.employeeId !== reviewerId && mayReview(request.employeeId)
  }));
}

/** mayReview(ownerId) says whether the signed-in account may decide that person's overtime. */
export function initOvertime(apiJson, mayReview = () => true) {
  const form = document.getElementById('overtime-form');
  const status = document.getElementById('overtime-status');
  const rows = document.getElementById('overtime-rows');
  const reviewer = document.getElementById('overtime-reviewer');
  let requests = [];

  function show(message, isError = false) {
    status.textContent = message;
    status.classList.toggle('payroll-preview-error', isError);
  }

  function render() {
    rows.replaceChildren();
    const view = overtimeRows(requests, reviewer.value, mayReview);
    if (!view.length) {
      const row = document.createElement('tr');
      const cell = document.createElement('td');
      cell.colSpan = 6;
      cell.className = 'table-message';
      cell.textContent = 'No overtime requests yet.';
      row.append(cell);
      rows.append(row);
      return;
    }
    view.forEach(item => {
      const row = document.createElement('tr');
      item.cells.forEach(text => {
        const cell = document.createElement('td');
        cell.textContent = text;
        row.append(cell);
      });
      const action = document.createElement('td');
      if (item.canReview) {
        ['approve', 'reject'].forEach(decision => {
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'button button-quiet';
          button.textContent = decision === 'approve' ? 'Approve' : 'Reject';
          button.dataset.overtimeId = item.id;
          button.dataset.decision = decision;
          button.setAttribute('aria-label', `${button.textContent} overtime ${item.id}`);
          action.append(button);
        });
      } else {
        action.textContent = '—';
      }
      row.append(action);
      rows.append(row);
    });
  }

  async function refresh() {
    const payload = await apiJson('/api/workflows');
    requests = payload.overtimeRequests || [];
    render();
  }

  form.addEventListener('submit', async event => {
    event.preventDefault();
    try {
      const minutes = Math.round(Number(document.getElementById('overtime-hours').value) * 60);
      const payload = await apiJson('/api/workflows/overtime', {
        employeeId: document.getElementById('overtime-employee').value,
        date: document.getElementById('overtime-date').value,
        minutes,
        note: document.getElementById('overtime-note').value
      });
      show(`Submitted ${payload.item.id} for review. It is paid only after someone else approves it.`);
      await refresh();
    } catch (error) {
      show(error instanceof Error ? error.message : 'Could not submit overtime.', true);
    }
  });

  rows.addEventListener('click', async event => {
    const button = event.target.closest('button[data-overtime-id]');
    if (!button) return;
    button.disabled = true;
    try {
      const payload = await apiJson(`/api/workflows/overtime/${encodeURIComponent(button.dataset.overtimeId)}/${button.dataset.decision}`,
        { reviewerId: reviewer.value });
      show(`${payload.item.id} ${payload.item.status.toLowerCase()} by ${payload.item.reviewerId}. Recalculate cutoffs to see it in pay.`);
      await refresh();
    } catch (error) {
      show(error instanceof Error ? error.message : 'Could not review overtime.', true);
      button.disabled = false;
    }
  });

  reviewer.addEventListener('change', render);
  return { refresh: () => refresh().catch(error => show(error instanceof Error ? error.message : 'Could not load overtime.', true)) };
}
