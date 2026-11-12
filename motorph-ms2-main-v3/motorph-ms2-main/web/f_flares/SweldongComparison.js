const ROWS = [
  ['grossPay', 'Gross pay'],
  ['sss', 'SSS / GSIS employee share'],
  ['philHealth', 'PhilHealth employee share'],
  ['pagIbig', 'Pag-IBIG employee share'],
  ['withholdingTax', 'Withholding tax'],
  ['totalDeductions', 'Total deductions'],
  ['netPay', 'Net pay']
];

export function formatReferenceCents(value) {
  let cents;
  try { cents = BigInt(value); } catch { throw new TypeError('Comparison amounts must be integer centavos.'); }
  const negative = cents < 0n;
  if (negative) cents = -cents;
  const pesos = (cents / 100n).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
  const fraction = String(cents % 100n).padStart(2, '0');
  return `${negative ? '−' : ''}₱${pesos}.${fraction}`;
}

export function comparisonRows(report) {
  if (!report || report.validated !== true || !report.sweldong?.validated
      || !Array.isArray(report.deltas)) throw new TypeError('Comparison report is missing validated results.');
  if (report.comparisonStatus !== 'LOCAL_UNAVAILABLE' && !report.motorph) {
    throw new TypeError('Comparison report is missing the MotorPH result.');
  }
  const indexed = new Map(report.deltas.map(item => [item.component, item]));
  return ROWS.map(([key, label]) => {
    const item = indexed.get(key);
    if (!item) {
      if (report.comparisonStatus === 'LOCAL_UNAVAILABLE') return { key, label, motorph: null, sweldong: null, delta: null, matches: false };
      throw new TypeError(`Comparison report is missing ${key}.`);
    }
    if (!Number.isSafeInteger(item.motorphCents) || !Number.isSafeInteger(item.sweldongCents)
        || !Number.isSafeInteger(item.deltaCents) || item.deltaCents !== item.sweldongCents - item.motorphCents) {
      throw new TypeError(`Comparison report has an invalid ${key} delta.`);
    }
    return { key, label, motorph: item.motorphCents, sweldong: item.sweldongCents,
      delta: item.deltaCents, matches: item.matches === true };
  });
}

export function comparisonHeading(report) {
  if (!report || !report.sweldong || !report.motorph && report.comparisonStatus !== 'LOCAL_UNAVAILABLE') {
    throw new TypeError('Comparison report has no source metadata.');
  }
  if (report.comparisonStatus === 'MATCH') return 'MATCH — all listed employee-side amounts are equal.';
  if (report.comparisonStatus === 'MISMATCH') return 'MISMATCH — review every delta; neither calculation is automatically preferred.';
  return 'MotorPH estimate unavailable — validated Sweldong result is available only as an explicit reference fallback.';
}

const field = id => document.getElementById(id);

function renderRows(tableBody, report) {
  const rows = comparisonRows(report);
  tableBody.replaceChildren();
  rows.forEach(row => {
    const tr = document.createElement('tr');
    [row.label, row.motorph === null ? 'Unavailable' : formatReferenceCents(row.motorph),
      row.sweldong === null ? 'Unavailable' : formatReferenceCents(row.sweldong),
      row.delta === null ? '—' : formatReferenceCents(row.delta),
      row.delta === null ? 'Not compared' : row.matches ? 'MATCH' : 'DIFFERS']
      .forEach(value => {
        const td = document.createElement('td');
        td.textContent = value;
        tr.append(td);
      });
    tableBody.append(tr);
  });
}

export function initSweldongComparison(apiJson, getAccount) {
  const form = field('sweldong-comparison-form');
  if (!form) return { refresh() {} };
  const consent = field('sweldong-consent');
  const submit = field('sweldong-submit');
  const status = field('sweldong-status');
  const resultPanel = field('sweldong-results');
  const decisionStatus = field('sweldong-selection-status');
  let report = null;
  let decision = null;

  const say = (element, text, error = false) => {
    element.textContent = text;
    element.classList.toggle('payroll-preview-error', error);
  };
  const render = () => {
    if (!report) return;
    const result = report.sweldong;
    const local = report.motorph;
    say(field('sweldong-result-heading'), comparisonHeading(report), report.comparisonStatus === 'MISMATCH');
    field('sweldong-comparison-id').textContent = `Comparison ${report.comparisonId || 'unavailable'} · ${report.checkedAt || ''}`;
    field('sweldong-motorph-source').textContent = local
      ? `${local.source} · ${local.rulesetId} · rates ${local.ratesVersion}`
      : `MotorPH unavailable · ${report.motorphUnavailableReason || 'reason not returned'}`;
    field('sweldong-provider-source').textContent = `${result.source} · ${result.providerVersion} · HTTP ${result.httpStatus}`;
    field('sweldong-audit-scope').textContent = report.auditScope || 'The report is not stored by the server; export it to retain the normalized input and response.';
    renderRows(field('sweldong-comparison-rows'), report);
    field('sweldong-raw-response').textContent = result.rawResponseText;
    field('sweldong-select-reference').disabled = !report.fallback?.eligible;
    field('sweldong-results').hidden = false;
    decisionStatus.textContent = decision?.selectionReason
      || 'MotorPH remains selected by default. No Sweldong value is used unless an admin explicitly selects it after review.';
  };

  const refresh = () => {
    if ((getAccount() || {}).role !== 'ADMIN') {
      submit.disabled = true;
      return;
    }
    submit.disabled = !consent.checked;
  };

  consent.addEventListener('change', refresh);
  form.addEventListener('submit', async event => {
    event.preventDefault();
    if ((getAccount() || {}).role !== 'ADMIN') {
      say(status, 'System Admin access is required.', true);
      return;
    }
    if (!consent.checked) {
      say(status, 'Confirm the external data transfer before continuing.', true);
      return;
    }
    submit.disabled = true;
    say(status, 'Sending the supported manual inputs to Sweldong Pinoy and validating its response.');
    resultPanel.hidden = true;
    decision = null;
    try {
      report = await apiJson('/api/admin/sweldong/compare', {
        confirm: 'SEND_TO_SWELDONG',
        period: field('sweldong-period').value,
        employmentType: 'Private Employee',
        payPeriod: 'Monthly',
        dailyDays: 20,
        basicPay: field('sweldong-basic').value,
        taxableAllowance: field('sweldong-taxable-allowance').value,
        nontaxableAllowance: field('sweldong-nontaxable-allowance').value,
        nightDifferentialHours: '', nightDifferentialRate: '', overtimeHours: '', overtimeRate: '', workSchedule: 5
      });
      say(status, 'Response validated. Review the source versions, assumptions and every component before choosing a reference.');
      render();
    } catch (error) {
      report = null;
      say(status, error.message, true);
    } finally {
      refresh();
    }
  });

  async function selectSource(source) {
    if (!report?.comparisonId) return;
    if (source === 'SWELDONG' && !report.fallback?.eligible) return;
    const buttons = [field('sweldong-keep-motorph'), field('sweldong-select-reference')];
    buttons.forEach(button => { button.disabled = true; });
    say(decisionStatus, 'Recording the explicit comparison-source choice in the admin audit log.');
    try {
      decision = await apiJson('/api/admin/sweldong/decision', {
        comparisonId: report.comparisonId,
        selectedSource: source
      });
      report.selectedSource = decision.selectedSource;
      report.selectionReason = decision.selectionReason;
      report.selectionAt = new Date().toISOString();
      decisionStatus.textContent = `${decision.selectedSource} selected for this reference preview only. ${decision.selectionReason}`;
    } catch (error) {
      say(decisionStatus, error.message, true);
    } finally {
      buttons[0].disabled = false;
      buttons[1].disabled = !report?.fallback?.eligible;
    }
  }

  field('sweldong-keep-motorph').addEventListener('click', () => selectSource('MOTORPH'));
  field('sweldong-select-reference').addEventListener('click', () => selectSource('SWELDONG'));
  field('sweldong-export').addEventListener('click', () => {
    if (!report) return;
    const evidence = { ...report, retainedAs: 'downloaded comparison evidence only; not a saved payroll record',
      selectedSource: decision?.selectedSource || 'MOTORPH',
      selectionReason: decision?.selectionReason || 'MotorPH default retained; no explicit external fallback was selected.' };
    const blob = new Blob([`${JSON.stringify(evidence, null, 2)}\n`], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `motorph-sweldong-comparison-${report.checkedAt?.replace(/[^0-9TZ-]/g, '-') || 'evidence'}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  });

  return { refresh };
}
