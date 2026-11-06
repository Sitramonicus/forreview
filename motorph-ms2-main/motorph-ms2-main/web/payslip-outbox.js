/**
 * Requirement 10: payslip emails (simulated outbox). Releasing pay emails each payslip to a synthetic work address;
 * Payroll Staff can simulate a mail outage, resend one email or retry every failed one. Java enforces every rule.
 */
export function outboxRows(messages) {
  return messages.map(message => ({
    id: message.id,
    status: message.status,
    cells: [message.id, message.to, `${message.payslipId}\n${message.subject}`,
      message.status === 'Failed' ? `Failed: ${message.reason}` : 'Sent (simulated)', `${message.attempts} · last ${message.lastAttemptAt.replace('T', ' ')}`]
  }));
}

/** download(url, filename) fetches with the signed-in headers and saves the file (the payslip PDF). */
export function initOutbox(apiJson, getAccount, download = async () => {}) {
  const tbody = document.getElementById('outbox-rows');
  if (!tbody) return { refresh() {} };
  const status = document.getElementById('outbox-status');
  const outage = document.getElementById('outbox-outage');
  let messagesById = new Map();
  const canSend = () => ['PAYROLL', 'ADMIN'].includes((getAccount() || {}).role);
  const say = (message, isError = false) => {
    status.textContent = message;
    status.classList.toggle('payroll-preview-error', isError);
  };

  async function refresh() {
    try {
      const payload = await apiJson('/api/payroll/outbox');
      outage.checked = payload.outage;
      tbody.replaceChildren();
      const rows = outboxRows(payload.messages);
      messagesById = new Map(payload.messages.map(message => [message.id, message]));
      if (!rows.length) {
        const row = document.createElement('tr');
        const cell = document.createElement('td');
        cell.colSpan = 6;
        cell.className = 'table-message';
        cell.textContent = 'No payslip emails yet. They are sent when Payroll Staff release pay.';
        row.append(cell);
        tbody.append(row);
        return;
      }
      rows.forEach(item => {
        const row = document.createElement('tr');
        item.cells.forEach(value => {
          const cell = document.createElement('td');
          cell.textContent = value;
          cell.style.whiteSpace = 'pre-line';
          row.append(cell);
        });
        const action = document.createElement('td');
        const message = messagesById.get(item.id);
        if (message && message.attachment) {
          const pdf = document.createElement('button');
          pdf.type = 'button';
          pdf.className = 'button button-quiet workflow-cancel';
          pdf.textContent = 'PDF';
          pdf.dataset.pdf = message.payslipId;
          pdf.setAttribute('aria-label', `Download ${message.attachment}`);
          action.append(pdf, ' ');
        }
        if (canSend()) {
          const button = document.createElement('button');
          button.type = 'button';
          button.className = 'button button-quiet workflow-cancel';
          button.textContent = 'Resend';
          button.dataset.emailId = item.id;
          button.setAttribute('aria-label', `Resend ${item.id}`);
          action.append(button);
        } else if (!action.childNodes.length) {
          action.textContent = '—';
        }
        row.append(action);
        tbody.append(row);
      });
    } catch (error) {
      say(error.message, true);
    }
  }

  tbody.addEventListener('click', async event => {
    const pdf = event.target.closest('button[data-pdf]');
    if (pdf) {
      try {
        await download(`/api/payroll/payslips/${encodeURIComponent(pdf.dataset.pdf)}/pdf`, `${pdf.dataset.pdf}.pdf`);
      } catch (error) {
        say(error.message, true);
      }
      return;
    }
    const button = event.target.closest('button[data-email-id]');
    if (!button) return;
    try {
      const { item } = await apiJson(`/api/payroll/outbox/${encodeURIComponent(button.dataset.emailId)}/resend`, {});
      say(item.status === 'Sent' ? `${item.id} resent to ${item.to}.` : `${item.id} failed again: ${item.reason}.`, item.status !== 'Sent');
      await refresh();
    } catch (error) {
      say(error.message, true);
    }
  });

  document.getElementById('outbox-retry').addEventListener('click', async () => {
    try {
      const { nowSent } = await apiJson('/api/payroll/outbox/retry-failed', {});
      say(`Retried failed emails: ${nowSent} sent now.`);
      await refresh();
    } catch (error) {
      say(error.message, true);
    }
  });

  outage.addEventListener('change', async () => {
    try {
      const payload = await apiJson('/api/payroll/outbox/outage', { on: outage.checked });
      say(payload.outage ? 'Simulated mail outage on: new and resent emails fail until you turn it off.' : 'Mail server back (simulated). Retry the failed emails.');
    } catch (error) {
      outage.checked = !outage.checked;
      say(error.message, true);
    }
  });

  return { refresh };
}
