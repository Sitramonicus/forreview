export function storageSummary(payload) {
  const counts = ['storeCount', 'employeeRecords', 'attendanceRecords', 'activePayslips'];
  if (!payload || !['firestore', 'file', 'memory'].includes(payload.backend)
      || counts.some(key => !Number.isInteger(payload[key]) || payload[key] < 0)) {
    throw new TypeError('Unexpected storage status response.');
  }
  const location = payload.backend === 'firestore' ? 'Firestore' : payload.backend === 'file' ? 'local file' : 'memory only';
  const saved = payload.durable ? 'durable store configured' : 'not durable';
  const readiness = payload.writeReady ? 'writes enabled' : 'writes paused';
  return `${location} · ${saved} · ${readiness} · ${payload.storeCount} state stores · ${payload.employeeRecords} employees · ${payload.attendanceRecords} attendance rows · ${payload.activePayslips} active payslips`;
}

export function verificationSummary(result) {
  if (!result || typeof result.backend !== 'string' || result.readBeforeWrite !== true
      || result.stateMatchedBeforeWrite !== true || result.writeCommitted !== true || result.readBackMatched !== true) {
    throw new TypeError('Storage round-trip did not return all verification checks.');
  }
  const at = result.verifiedAt || 'time unavailable';
  return `PASS · ${result.backend} read matched the running state, an unchanged snapshot was committed, and read-back matched at ${at}. No employee or payroll values were changed.`;
}

export function initAdminStorage(apiJson, getAccount) {
  const status = document.getElementById('storage-status');
  if (!status) return { refresh() {} };
  const button = document.getElementById('storage-verify');
  const verification = document.getElementById('storage-verification');
  const backendLabel = document.getElementById('storage-backend-label');
  const say = (element, message, isError = false) => {
    element.textContent = message;
    element.classList.toggle('payroll-preview-error', isError);
  };

  async function refresh() {
    if ((getAccount() || {}).role !== 'ADMIN') {
      button.disabled = true;
      return;
    }
    try {
      const payload = await apiJson('/api/admin/storage');
      say(status, storageSummary(payload), !payload.writeReady);
      backendLabel.textContent = payload.backend === 'firestore' ? 'FIRESTORE' : payload.backend === 'file' ? 'LOCAL FILE' : 'MEMORY ONLY';
      backendLabel.classList.toggle('payroll-preview-error', !payload.durable);
      button.disabled = !payload.durable || !payload.writeReady;
      if (payload.lastVerification) say(verification, verificationSummary(payload.lastVerification));
      else say(verification, 'No successful round-trip verification has been recorded in this server process.');
    } catch (error) {
      button.disabled = true;
      backendLabel.textContent = 'UNAVAILABLE';
      backendLabel.classList.add('workstream-status-warning');
      say(status, error.message, true);
    }
  }

  button.addEventListener('click', async () => {
    button.disabled = true;
    say(verification, 'Checking the configured store. The unchanged state will be written and read back.');
    try {
      const result = await apiJson('/api/admin/storage/verify', { confirm: 'VERIFY' });
      say(verification, verificationSummary(result));
      await refresh();
    } catch (error) {
      say(verification, error.message, true);
      await refresh();
    }
  });

  return { refresh };
}
