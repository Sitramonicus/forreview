import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import net from 'node:net';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const sourceDir = path.join(projectRoot, 'src/main/java/ph/motorph/web');
const gsonJar = path.join(projectRoot, 'lib/gson-2.13.2.jar');
const timeoutMs = 15_000;
const javaClasses = mkdtempSync(path.join(tmpdir(), 'motorph-e2e-classes-'));
const serverChildren = [];
let browser;

function freePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const { port } = server.address();
      server.close(error => error ? reject(error) : resolve(port));
    });
  });
}

function start(command, args, env, label) {
  const child = spawn(command, args, {
    cwd: projectRoot,
    env,
    detached: process.platform !== 'win32',
    stdio: ['ignore', 'pipe', 'pipe']
  });
  child.log = '';
  child.on('error', error => { child.log += `${label}: ${error.message}\n`; });
  for (const stream of [child.stdout, child.stderr]) {
    stream.setEncoding('utf8');
    stream.on('data', chunk => { child.log = (child.log + chunk).slice(-8000); });
  }
  serverChildren.push(child);
  return child;
}

async function waitForServer(url, child, label) {
  const end = Date.now() + timeoutMs;
  let lastError = 'no response';
  while (Date.now() < end) {
    if (child.exitCode !== null) throw new Error(`${label} exited early.\n${child.log}`);
    try {
      const response = await fetch(url);
      if (response.ok) return response;
      lastError = `HTTP ${response.status}`;
    } catch (error) {
      lastError = error.message;
    }
    await new Promise(resolve => setTimeout(resolve, 150));
  }
  throw new Error(`${label} did not become ready (${lastError}).\n${child.log}`);
}

async function stop(child) {
  if (!child || child.exitCode !== null) return;
  try {
    if (process.platform === 'win32') child.kill('SIGTERM');
    else process.kill(-child.pid, 'SIGTERM');
  } catch { /* already stopped */ }
  await Promise.race([
    new Promise(resolve => child.once('exit', resolve)),
    new Promise(resolve => setTimeout(resolve, 2500))
  ]);
  if (child.exitCode === null) {
    try {
      if (process.platform === 'win32') child.kill('SIGKILL');
      else process.kill(-child.pid, 'SIGKILL');
    } catch { /* already stopped */ }
  }
}

async function main() {
  const apiPort = await freePort();
  const webPort = await freePort();
  const sources = readdirSync(sourceDir).filter(file => file.endsWith('.java')).map(file => path.join(sourceDir, file));
  execFileSync('javac', ['--add-modules', 'jdk.httpserver', '-cp', gsonJar, '-d', javaClasses, ...sources], {
    cwd: projectRoot,
    stdio: 'inherit'
  });

  const apiEnv = {
    ...process.env,
    PORT: String(apiPort),
    HOST: '127.0.0.1',
    WEB_ROOT: path.join(projectRoot, 'web'),
    FIREBASE_PROJECT_ID: ''
  };
  const api = start('java', ['--add-modules', 'jdk.httpserver', '-cp', `${gsonJar}:${javaClasses}`, 'ph.motorph.web.AppServer'], apiEnv, 'Java API');
  await waitForServer(`http://127.0.0.1:${apiPort}/api/health`, api, 'Java API');

  const webEnv = {
    ...process.env,
    API_PROXY_TARGET: `http://127.0.0.1:${apiPort}`,
    VITE_FIREBASE_API_KEY: '',
    VITE_FIREBASE_AUTH_DOMAIN: '',
    VITE_FIREBASE_PROJECT_ID: '',
    VITE_FIREBASE_APP_ID: '',
    VITE_FIREBASE_STORAGE_BUCKET: '',
    VITE_FIREBASE_MESSAGING_SENDER_ID: ''
  };
  const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm';
  const vite = start(npm, ['run', 'dev', '--', '--host', '127.0.0.1', '--port', String(webPort)], webEnv, 'Vite');
  const baseUrl = `http://127.0.0.1:${webPort}`;
  const healthResponse = await waitForServer(`${baseUrl}/api/health`, vite, 'Vite/API proxy');
  const health = await healthResponse.json();
  assert.equal(health.mode, 'local-synthetic-demo', 'E2E must use isolated local process memory, not Firebase');

  try {
    browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
  } catch (error) {
    throw new Error(`Chromium could not launch. Install the Playwright browser and its system dependencies.\n${error.message}`);
  }
  const page = await browser.newPage({ viewport: { width: 1365, height: 900 } });
  const pageErrors = [];
  const sandboxSnapshotMethods = [];
  let captureSandboxReads = false;
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('request', request => {
    if (captureSandboxReads && request.url().includes('/api/')) sandboxSnapshotMethods.push(request.method());
  });
  await page.goto(baseUrl);
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await page.waitForFunction(() => document.querySelectorAll('#employee-rows tr').length === 3, { timeout: timeoutMs });
  assert.equal((await page.locator('#workflow-storage-mode').textContent()).trim(), 'Local API memory');
  assert.equal((await page.locator('#overview-workflow-count').textContent()).trim(), '00');
  await page.locator('#employee-sort').selectOption('id');
  await page.locator('#employee-order').selectOption('desc');
  const employeeIds = await page.locator('#employee-rows .person-id').allTextContents();
  assert.deepEqual(employeeIds, ['DEMO-003', 'DEMO-002', 'DEMO-001']);
  const [employeeSortedDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#employee-export-button').click()
  ]);
  const employeeSortedCsv = readFileSync(await employeeSortedDownload.path(), 'utf8');
  assert.ok(employeeSortedCsv.indexOf('DEMO-003') < employeeSortedCsv.indexOf('DEMO-002'));
  assert.ok(employeeSortedCsv.indexOf('DEMO-002') < employeeSortedCsv.indexOf('DEMO-001'));
  console.log('PASS: local workspace loads three synthetic records and employee search/export follows selected sort order.');

  const payrollApiRequests = [];
  page.on('request', request => { if (request.url().includes('/api/payroll')) payrollApiRequests.push(request.url()); });
  await page.locator('[data-view="payroll"]').click();
  assert.match(await page.locator('#payroll-view').innerText(), /Temporary ruleset — preview only/);
  await page.locator('#payroll-preview-form button[type="submit"]').click();
  await page.waitForFunction(() => !document.getElementById('payroll-preview-results').hidden, { timeout: timeoutMs });
  assert.equal((await page.locator('#payroll-result-net').textContent()).trim(), '₱22,611.20');
  assert.equal((await page.locator('#payroll-result-employee-total').textContent()).trim(), '₱2,388.80');
  assert.equal((await page.locator('#payroll-result-employer-total').textContent()).trim(), '₱3,355.00');
  assert.match(await page.locator('#payroll-result-lines').innerText(), /₱313.80/);
  const [payrollPreviewDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#payroll-preview-export').click()
  ]);
  assert.equal(payrollPreviewDownload.suggestedFilename(), 'motorph-2026-monthly-preview-not-payslip.csv');
  const payrollPreviewCsv = readFileSync(await payrollPreviewDownload.path(), 'utf8');
  assert.match(payrollPreviewCsv, /Net cash pay estimate/);
  assert.match(payrollPreviewCsv, /₱22,611\.20/);
  assert.match(await page.locator('#payroll-preview-export-status').textContent(), /not a payslip/);
  assert.equal(payrollApiRequests.length, 0, 'temporary payroll preview must not call payroll APIs or persist input');
  await page.locator('#payroll-preview-gross').fill('100');
  await page.locator('#payroll-preview-form button[type="submit"]').click();
  assert.match(await page.locator('#payroll-preview-status').textContent(), /deductions exceed gross cash pay/);
  assert.equal(await page.locator('#payroll-preview-results').isHidden(), true);
  assert.equal(await page.locator('#payroll-preview-export').isDisabled(), true);
  await page.locator('#payroll-preview-gross').fill('25000.00');
  console.log('PASS: disposable 2026 statutory preview calculates synthetic sample locally, blocks negative net, and makes no payroll API request.');

  let simulatedFailure = { status: 403, error: 'Synthetic rules denial.' };
  const failureHandler = route => route.fulfill({
    status: simulatedFailure.status,
    contentType: 'application/json',
    body: JSON.stringify({ error: simulatedFailure.error })
  });
  await page.route('**/api/workflows', failureHandler);
  await page.reload();
  await page.waitForFunction(() => !document.getElementById('workflow-load-status').hidden, { timeout: timeoutMs });
  assert.match(await page.locator('#workflow-load-status').textContent(), /Synthetic rules denial/);
  simulatedFailure = { status: 503, error: 'Synthetic service outage.' };
  await page.reload();
  await page.waitForFunction(() => document.getElementById('workflow-load-status').textContent.includes('Synthetic service outage'), { timeout: timeoutMs });
  await page.unroute('**/api/workflows', failureHandler);
  await page.reload();
  await page.waitForFunction(() => (document.getElementById('workflow-storage-mode').textContent || '').trim() === 'Local API memory', { timeout: timeoutMs });
  console.log('PASS: workflow authorization/service errors are visible and recover after retry.');

  await page.locator('[data-view="hr"]').click();
  await page.locator('#employee-edit-name').fill('Synthetic Browser E2E');
  await page.locator('#employee-edit-department').fill('QA Demo');
  await page.locator('#employee-edit-role').fill('Browser Tester');
  await page.locator('#employee-edit-location').fill('Taguig');
  await page.locator('#employee-edit-status').selectOption('Active');
  await page.locator('#employee-save-button').click();
  await page.waitForFunction(() => document.getElementById('employee-form-status').textContent.startsWith('Created DEMO-'), { timeout: timeoutMs });
  const createMessage = await page.locator('#employee-form-status').textContent();
  const employeeId = createMessage.match(/DEMO-[A-F0-9]{12}/)?.[0];
  assert.ok(employeeId, 'create response should show a server-generated ID');
  assert.match(createMessage, /shared local API-process memory/);
  await page.locator('#hr-directory-search').fill(employeeId);
  await page.locator('#hr-directory-status').selectOption('Active');
  assert.equal(await page.locator('#hr-rows tr').filter({ hasText: employeeId }).count(), 1);
  const [hrDirectoryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#hr-directory-export').click()
  ]);
  assert.equal(hrDirectoryDownload.suggestedFilename(), 'motorph-hr-directory.csv');
  const hrDirectoryCsv = readFileSync(await hrDirectoryDownload.path(), 'utf8');
  assert.match(hrDirectoryCsv, new RegExp(employeeId));
  assert.doesNotMatch(hrDirectoryCsv, /DEMO-001/);
  assert.match(await page.locator('#hr-directory-export-status').textContent(), /currently filtered synthetic directory rows/);
  await page.locator('#hr-directory-search').fill('no-such-person');
  assert.match(await page.locator('#hr-rows').innerText(), /No records match these filters/);
  await page.locator('#hr-directory-search').fill('');
  await page.locator('#hr-directory-status').selectOption('');
  await page.locator('#hr-directory-sort').selectOption('department');
  await page.locator('#hr-directory-order').selectOption('desc');
  const hrDepartments = await page.locator('#hr-rows tr td:nth-child(2)').allTextContents();
  assert.deepEqual(hrDepartments, [...hrDepartments].sort((a, b) => b.localeCompare(a, undefined, { sensitivity: 'base' })));

  await page.locator('#hr-rows').getByRole('button', { name: 'Edit synthetic directory record for Synthetic Browser E2E' }).click();
  await page.locator('#employee-edit-name').fill('Synthetic Browser E2E Updated');
  await page.locator('#employee-edit-location').fill('Makati');
  await page.locator('#employee-save-button').click();
  await page.waitForFunction(() => document.getElementById('employee-form-status').textContent.startsWith('Updated DEMO-'), { timeout: timeoutMs });
  assert.ok((await page.locator('#hr-rows').innerText()).includes('Synthetic Browser E2E Updated'));
  console.log('PASS: synthetic employee create/edit round-trip.');

  await page.locator('[data-view="attendance"]').click();
  await page.locator('#attendance-employee').selectOption('DEMO-001');
  await page.waitForFunction(() => document.querySelectorAll('#attendance-rows tr').length === 3, { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-summary-count').textContent()).trim(), '03');
  assert.equal((await page.locator('#overview-attendance-count').textContent()).trim(), '03');
  assert.equal((await page.locator('#attendance-summary-first').textContent()).trim(), '2026-10-01');
  assert.equal((await page.locator('#attendance-summary-last').textContent()).trim(), '2026-10-15');
  assert.equal((await page.locator('#attendance-calendar-month').textContent()).trim(), 'October 2026');
  assert.equal(await page.locator('#attendance-calendar-grid .calendar-event').count(), 3);
  assert.match(await page.locator('#attendance-calendar-grid td[data-date="2026-10-01"]').innerText(), /08:01–17:00/);
  await page.locator('#attendance-calendar-next').click();
  assert.equal((await page.locator('#attendance-calendar-month').textContent()).trim(), 'November 2026');
  assert.equal(await page.locator('#attendance-calendar-grid .calendar-event').count(), 0);
  await page.locator('#attendance-calendar-prev').click();
  assert.equal((await page.locator('#attendance-calendar-month').textContent()).trim(), 'October 2026');
  await page.locator('#attendance-order').selectOption('desc');
  assert.equal((await page.locator('#attendance-rows tr').first().locator('td').first().textContent()).trim(), '2026-10-15');
  const [attendanceSortedDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#attendance-export-button').click()
  ]);
  const attendanceSortedCsv = readFileSync(await attendanceSortedDownload.path(), 'utf8');
  assert.ok(attendanceSortedCsv.indexOf('2026-10-15') < attendanceSortedCsv.indexOf('2026-10-02'));
  await page.locator('#attendance-order').selectOption('asc');
  await page.locator('#attendance-from').fill('2026-10-02');
  await page.locator('#attendance-to').fill('2026-10-02');
  await page.locator('#attendance-filter button[type="submit"]').click();
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '01', { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-summary-first').textContent()).trim(), '2026-10-02');
  assert.equal((await page.locator('#attendance-summary-last').textContent()).trim(), '2026-10-02');
  await page.locator('#attendance-from').fill('');
  await page.locator('#attendance-to').fill('');
  await page.locator('#attendance-filter button[type="submit"]').click();
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '03', { timeout: timeoutMs });
  await page.locator('#attendance-calendar-grid td[data-date="2026-10-02"] button').click();
  await page.locator('#attendance-calendar-grid td[data-date="2026-10-15"] button').click();
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '02', { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-from').inputValue()), '2026-10-02');
  assert.equal((await page.locator('#attendance-to').inputValue()), '2026-10-15');
  assert.match(await page.locator('#attendance-calendar-selection-status').textContent(), /inclusive raw-record range/);
  await page.locator('#attendance-from').fill('');
  await page.locator('#attendance-to').fill('');
  await page.locator('#attendance-filter button[type="submit"]').click();
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '03', { timeout: timeoutMs });
  await page.locator('#attendance-employee').selectOption(employeeId);
  await page.waitForFunction(() => document.getElementById('attendance-rows').textContent.includes('No attendance entries match'), { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-summary-count').textContent()).trim(), '00');
  assert.equal((await page.locator('#attendance-summary-first').textContent()).trim(), '—');
  console.log('PASS: raw attendance summary, inclusive filters, and calendar-selected range; no hours are derived or fabricated.');

  await page.locator('[data-view="reports"]').click();
  assert.equal((await page.locator('#directory-report-total').textContent()).trim(), '04');
  assert.equal((await page.locator('#directory-report-active').textContent()).trim(), '03');
  assert.equal((await page.locator('#directory-report-onleave').textContent()).trim(), '01');
  assert.match(await page.locator('#directory-department-counts').innerText(), /QA Demo: 1/);
  const [directorySummaryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#directory-summary-export').click()
  ]);
  assert.equal(directorySummaryDownload.suggestedFilename(), 'motorph-directory-label-summary.csv');
  const directorySummaryCsv = readFileSync(await directorySummaryDownload.path(), 'utf8');
  assert.match(directorySummaryCsv, /"Status label","Active","3"/);
  assert.match(directorySummaryCsv, /"Department label","QA Demo","1"/);
  await page.waitForFunction(() => document.querySelectorAll('#attendance-report-rows tr').length === 4, { timeout: timeoutMs });
  const mappedReportRow = page.locator('#attendance-report-rows tr').filter({ hasText: 'DEMO-001' });
  assert.match(await mappedReportRow.innerText(), /Schedule 101/);
  assert.equal((await mappedReportRow.locator('td').nth(2).textContent()).trim(), '3');
  assert.equal((await mappedReportRow.locator('td').nth(3).textContent()).trim(), '2026-10-01');
  const unmappedReportRow = page.locator('#attendance-report-rows tr').filter({ hasText: employeeId });
  assert.match(await unmappedReportRow.innerText(), /No mapping/);
  assert.equal((await unmappedReportRow.locator('td').nth(2).textContent()).trim(), '0');
  await page.locator('#attendance-report-search').fill('DEMO-001');
  assert.equal(await page.locator('#attendance-report-rows tr').count(), 1);
  assert.match(await page.locator('#attendance-report-rows').innerText(), /DEMO-001/);
  await page.locator('#attendance-report-mapping').selectOption('unmapped');
  assert.match(await page.locator('#attendance-report-rows').innerText(), /No employees match/);
  await page.locator('#attendance-report-search').fill('');
  assert.equal(await page.locator('#attendance-report-rows tr').count(), 1);
  assert.match(await page.locator('#attendance-report-rows').innerText(), new RegExp(employeeId));
  const [filteredReportDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#attendance-report-export').click()
  ]);
  assert.match(readFileSync(await filteredReportDownload.path(), 'utf8'), new RegExp(employeeId));
  assert.doesNotMatch(readFileSync(await filteredReportDownload.path(), 'utf8'), /DEMO-001/);
  await page.locator('#attendance-report-mapping').selectOption('all');
  await page.locator('#attendance-report-sort').selectOption('rawRecordCount');
  await page.locator('#attendance-report-order').selectOption('desc');
  const coverageCounts = (await page.locator('#attendance-report-rows tr td:nth-child(3)').allTextContents()).map(Number);
  assert.deepEqual(coverageCounts, [...coverageCounts].sort((a, b) => b - a));
  await page.locator('#attendance-report-from').fill('2026-10-02');
  await page.locator('#attendance-report-to').fill('2026-10-02');
  await page.locator('#attendance-report-filter button[type="submit"]').click();
  await page.waitForFunction(() => document.querySelector('#attendance-report-rows tr')?.textContent.includes('2026-10-02'), { timeout: timeoutMs });
  assert.equal((await mappedReportRow.locator('td').nth(2).textContent()).trim(), '1');
  assert.equal((await unmappedReportRow.locator('td').nth(2).textContent()).trim(), '0');
  const [reportDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#attendance-report-export').click()
  ]);
  assert.equal(reportDownload.suggestedFilename(), 'motorph-raw-attendance-coverage.csv');
  assert.match(await page.locator('#attendance-report-status').textContent(), /no hours or payroll values/);
  await mappedReportRow.getByRole('button', { name: 'View raw entries for DEMO-001' }).click();
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '01', { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-employee').inputValue()), 'DEMO-001');
  assert.equal((await page.locator('#attendance-rows tr').first().locator('td').first().textContent()).trim(), '2026-10-02');
  console.log('PASS: owner-scoped raw attendance coverage report, unmapped status, search/filter/sort/export, and drill-through to raw rows.');

  await page.locator('[data-view="team"]').click();
  assert.equal(await page.locator('#team-rows tr').count(), 4);
  await page.locator('#team-directory-search').fill('DEMO-001');
  await page.locator('#team-directory-status').selectOption('Active');
  assert.equal(await page.locator('#team-rows tr').filter({ hasText: 'DEMO-001' }).count(), 1);
  await page.locator('#team-directory-search').fill('no-such-person');
  assert.match(await page.locator('#team-rows').innerText(), /No records match these filters/);
  await page.locator('#team-directory-search').fill('');
  await page.locator('#team-directory-status').selectOption('');
  await page.locator('#team-directory-sort').selectOption('department');
  await page.locator('#team-directory-order').selectOption('desc');
  const teamDepartments = await page.locator('#team-rows tr td:nth-child(2)').allTextContents();
  assert.deepEqual(teamDepartments, [...teamDepartments].sort((a, b) => b.localeCompare(a, undefined, { sensitivity: 'base' })));
  const maraRow = page.locator('#team-rows tr').filter({ hasText: 'DEMO-001' });
  await maraRow.getByRole('button', { name: 'View synthetic record for Mara Rivera' }).click();
  assert.equal(await page.locator('#employee-dialog').evaluate(dialog => dialog.open), true);
  assert.match(await page.locator('#dialog-content').innerText(), /People Operations/);
  await page.locator('#dialog-done').click();
  const [rosterDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#team-export-button').click()
  ]);
  assert.equal(rosterDownload.suggestedFilename(), 'motorph-synthetic-roster.csv');
  console.log('PASS: read-only synthetic roster search/status filters, sorting, and filtered CSV export.');

  let reportFailure = { status: 403, error: 'Synthetic report access denial.' };
  const reportFailureHandler = route => route.fulfill({
    status: reportFailure.status,
    contentType: 'application/json',
    body: JSON.stringify({ error: reportFailure.error })
  });
  await page.route('**/api/reports/attendance*', reportFailureHandler);
  await page.locator('[data-view="reports"]').click();
  await page.waitForFunction(() => document.getElementById('attendance-report-status').textContent.includes('Synthetic report access denial.'), { timeout: timeoutMs });
  reportFailure = { status: 503, error: 'Synthetic report service outage.' };
  await page.locator('#attendance-report-filter button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('attendance-report-status').textContent.includes('Synthetic report service outage.'), { timeout: timeoutMs });
  await page.unroute('**/api/reports/attendance*', reportFailureHandler);
  await page.locator('#attendance-report-filter button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('attendance-report-status').textContent.includes('no hours or payroll values were calculated'), { timeout: timeoutMs });
  assert.equal(await page.locator('#attendance-report-rows tr').count(), 4);
  console.log('PASS: report authorization/service errors are visible and recover on retry.');

  await page.locator('[data-view="attendance"]').click();
  await page.locator('#attendance-entry-employee').selectOption('DEMO-002');
  await page.locator('#attendance-entry-date').fill('2026-10-20');
  await page.locator('#attendance-entry-time-in').fill('08:02');
  await page.locator('#attendance-entry-time-out').fill('17:00');
  await page.locator('#attendance-entry-submit').click();
  await page.waitForFunction(() => document.getElementById('attendance-entry-status').textContent.startsWith('Created synthetic attendance'), { timeout: timeoutMs });
  await page.waitForFunction(() => (document.getElementById('attendance-summary-count').textContent || '').trim() === '03', { timeout: timeoutMs });
  assert.match(await page.locator('#attendance-rows').innerText(), /2026-10-20/);
  assert.match(await page.locator('#attendance-rows').innerText(), /DEMO/);
  await page.locator('#attendance-entry-submit').click();
  await page.waitForFunction(() => document.getElementById('attendance-entry-status').textContent.includes('already exists'), { timeout: timeoutMs });
  assert.equal((await page.locator('#attendance-summary-count').textContent()).trim(), '03');
  await page.locator('#attendance-entry-date').fill('2026-10-21');
  await page.locator('#attendance-entry-time-in').fill('18:00');
  await page.locator('#attendance-entry-time-out').fill('17:00');
  await page.locator('#attendance-entry-submit').click();
  await page.waitForFunction(() => document.getElementById('attendance-entry-status').textContent.includes('timeIn must be less than or equal'), { timeout: timeoutMs });
  await page.locator('#attendance-entry-employee').selectOption(employeeId);
  await page.locator('#attendance-entry-date').fill('2026-10-22');
  await page.locator('#attendance-entry-time-in').fill('08:00');
  await page.locator('#attendance-entry-time-out').fill('17:00');
  await page.locator('#attendance-entry-submit').click();
  await page.waitForFunction(() => document.getElementById('attendance-entry-status').textContent.includes('No configured synthetic schedule mapping'), { timeout: timeoutMs });
  console.log('PASS: local-only raw attendance entry enforces the existing mapping, unique schedule/date, and time order without deriving hours.');

  await page.locator('[data-view="requests"]').click();
  await page.locator('#leave-calendar-grid button[data-date="2026-11-10"]').click();
  await page.locator('#leave-calendar-grid button[data-date="2026-11-12"]').click();
  assert.equal(await page.locator('#leave-start').inputValue(), '2026-11-10');
  assert.equal(await page.locator('#leave-end').inputValue(), '2026-11-12');
  assert.match(await page.locator('#leave-calendar-selection-status').textContent(), /no days or balances are calculated/);
  await page.locator('#leave-employee').selectOption(employeeId);
  await page.locator('#leave-type').selectOption('Personal');
  await page.locator('#leave-start').fill('2026-11-12');
  await page.locator('#leave-end').fill('2026-11-10');
  await page.locator('#leave-request-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('leave-form-status').classList.contains('workflow-error'), { timeout: timeoutMs });
  assert.ok((await page.locator('#leave-request-rows').innerText()).includes('No session requests yet.'));
  console.log('PASS: reversed leave dates show an error and create no record.');
  await page.locator('#leave-start').fill('2026-11-10');
  await page.locator('#leave-end').fill('2026-11-10');
  await page.locator('#leave-note').fill('Synthetic browser E2E only.');
  await page.locator('#leave-request-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('leave-form-status').textContent.startsWith('LV-'), { timeout: timeoutMs });
  const leaveMessage = await page.locator('#leave-form-status').textContent();
  const leaveId = leaveMessage.match(/LV-[A-F0-9]{12}/)?.[0];
  assert.ok(leaveId, 'leave submit should return a synthetic request ID');
  await page.locator('#leave-employee').selectOption('DEMO-002');
  await page.locator('#leave-type').selectOption('Vacation');
  await page.locator('#leave-start').fill('2026-11-20');
  await page.locator('#leave-end').fill('2026-11-20');
  await page.locator('#leave-note').fill('Second synthetic request for filtered-export coverage.');
  await page.locator('#leave-request-form button[type="submit"]').click();
  await page.waitForFunction(id => {
    const status = document.getElementById('leave-form-status').textContent;
    return status.startsWith('LV-') && !status.includes(id);
  }, leaveId, { timeout: timeoutMs });
  const secondLeaveId = (await page.locator('#leave-form-status').textContent()).match(/LV-[A-F0-9]{12}/)?.[0];
  assert.ok(secondLeaveId && secondLeaveId !== leaveId, 'second leave request should have a distinct ID');
  assert.equal((await page.locator('#overview-workflow-count').textContent()).trim(), '02');
  await page.locator('#leave-filter-search').fill('');
  await page.locator('#leave-filter-status').selectOption('');
  await page.locator('#leave-filter-type').selectOption('Personal');
  assert.equal(await page.locator('#leave-request-rows tr').count(), 1);
  const [visibleLeaveDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#leave-visible-export-button').click()
  ]);
  const visibleLeaveCsv = readFileSync(await visibleLeaveDownload.path(), 'utf8');
  assert.ok(visibleLeaveCsv.includes(leaveId) && !visibleLeaveCsv.includes(secondLeaveId), 'visible leave CSV follows exact type filter');
  await page.locator('#leave-filter-search').fill(leaveId);
  await page.locator('#leave-filter-status').selectOption('Pending review');
  await page.locator('#leave-filter-type').selectOption('');
  assert.equal(await page.locator('#leave-request-rows tr').filter({ hasText: leaveId }).count(), 1);
  await page.locator('#leave-sort').selectOption('startDate');
  await page.locator('#leave-sort-order').selectOption('asc');
  assert.equal(await page.locator('#leave-request-rows tr').filter({ hasText: leaveId }).count(), 1);
  await page.locator('#leave-sort').selectOption('submittedAt');
  await page.locator('#leave-sort-order').selectOption('desc');
  await page.locator('#leave-filter-search').fill('no-such-request');
  assert.match(await page.locator('#leave-request-rows').innerText(), /No requests match these filters/);
  assert.ok(await page.locator('#leave-calendar-grid .calendar-event').count() > 0, 'list filters must not hide calendar requests');
  await page.locator('#leave-filter-search').fill(leaveId);
  const leaveRow = page.locator('#leave-request-rows tr').filter({ hasText: leaveId });
  await leaveRow.locator('details summary').click();
  assert.match(await leaveRow.innerText(), /Synthetic browser E2E only/);
  assert.match(await leaveRow.innerText(), /SUBMITTED \(UTC\)/);
  await leaveRow.getByRole('button', { name: new RegExp(`Cancel ${leaveId}`) }).click();
  await page.locator('#leave-filter-status').selectOption('Cancelled');
  await page.waitForFunction(id => {
    const row = [...document.querySelectorAll('#leave-request-rows tr')].find(item => item.textContent.includes(id));
    return row && row.textContent.includes('Cancelled');
  }, leaveId, { timeout: timeoutMs });
  await leaveRow.locator('details summary').click();
  assert.match(await leaveRow.innerText(), /CANCELLED \(UTC\)/);
  await page.locator('#leave-filter-search').fill('');
  await page.locator('#leave-filter-status').selectOption('');
  await page.locator('#leave-filter-from').fill('2026-11-11');
  await page.locator('#leave-filter-to').fill('2026-11-12');
  assert.match(await page.locator('#leave-request-rows').innerText(), /No requests match these filters/);
  await page.locator('#leave-filter-from').fill('2026-11-10');
  await page.locator('#leave-filter-to').fill('2026-11-10');
  assert.equal(await page.locator('#leave-request-rows tr').filter({ hasText: leaveId }).count(), 1);
  await page.locator('#leave-filter-from').fill('2026-11-12');
  await page.locator('#leave-filter-to').fill('2026-11-10');
  assert.match(await page.locator('#leave-request-rows').innerText(), /No requests match these filters/);
  await page.locator('#leave-filter-from').fill('');
  await page.locator('#leave-filter-to').fill('');
  assert.match(await page.locator('#leave-calendar-grid').innerText(), /Personal · Cancelled/);
  await page.locator('#leave-calendar-prev').click();
  assert.equal((await page.locator('#leave-calendar-month').textContent()).trim(), 'October 2026');
  assert.equal(await page.locator('#leave-calendar-grid .calendar-event').count(), 0);
  await page.locator('#leave-calendar-next').click();
  assert.equal((await page.locator('#leave-calendar-month').textContent()).trim(), 'November 2026');
  assert.ok(await page.locator('#leave-calendar-grid .calendar-event').count() > 0);
  console.log('PASS: leave submit/cancel and request-date calendar navigation/status.');

  await page.locator('#ticket-employee').selectOption(employeeId);
  await page.locator('#ticket-type').selectOption('Technical support');
  await page.locator('#ticket-summary').fill('Synthetic browser ticket E2E.');
  await page.locator('#ticket-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('ticket-form-status').textContent.startsWith('TK-'), { timeout: timeoutMs });
  const ticketMessage = await page.locator('#ticket-form-status').textContent();
  const ticketId = ticketMessage.match(/TK-[A-F0-9]{12}/)?.[0];
  assert.ok(ticketId, 'ticket submit should return a synthetic ticket ID');
  assert.equal((await page.locator('#overview-workflow-count').textContent()).trim(), '03');
  await page.locator('#ticket-filter-search').fill(ticketId);
  await page.locator('#ticket-filter-status').selectOption('Open');
  await page.locator('#ticket-filter-type').selectOption('Technical support');
  const ticketRow = page.locator('#ticket-rows tr').filter({ hasText: ticketId });
  assert.equal(await ticketRow.count(), 1);
  assert.match(await ticketRow.innerText(), /IT Admin/);
  await page.locator('#ticket-filter-type').selectOption('Payroll inquiry');
  assert.match(await page.locator('#ticket-rows').innerText(), /No tickets match these filters/);
  assert.equal(await page.locator('#ticket-visible-export-button').isDisabled(), true);
  await page.locator('#ticket-filter-type').selectOption('Technical support');
  const [visibleTicketDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#ticket-visible-export-button').click()
  ]);
  assert.match(readFileSync(await visibleTicketDownload.path(), 'utf8'), new RegExp(ticketId));
  await page.locator('#ticket-sort').selectOption('queue');
  await page.locator('#ticket-sort-order').selectOption('asc');
  assert.equal(await ticketRow.count(), 1);
  await page.locator('#ticket-sort').selectOption('submittedAt');
  await page.locator('#ticket-sort-order').selectOption('desc');
  await ticketRow.locator('details summary').click();
  assert.match(await ticketRow.innerText(), /Synthetic browser ticket E2E/);
  await page.locator('#ticket-filter-search').fill('no-such-ticket');
  assert.match(await page.locator('#ticket-rows').innerText(), /No tickets match these filters/);
  await page.locator('#ticket-filter-search').fill(ticketId);
  await ticketRow.getByRole('button', { name: new RegExp(`Cancel ${ticketId}`) }).click();
  await page.locator('#ticket-filter-status').selectOption('Cancelled');
  await page.waitForFunction(id => {
    const row = [...document.querySelectorAll('#ticket-rows tr')].find(item => item.textContent.includes(id));
    return row && row.textContent.includes('Cancelled');
  }, ticketId, { timeout: timeoutMs });
  console.log('PASS: ticket submit/queue/cancel flow.');

  await page.locator('[data-view="team"]').click();
  await page.locator('#team-rows tr').filter({ hasText: 'DEMO-002' }).getByRole('button').click();
  await page.waitForFunction(() => document.querySelectorAll('.employee-linked-record-group').length === 3, { timeout: timeoutMs });
  assert.match(await page.locator('#dialog-content').innerText(), /raw attendance \(3\)/i);
  assert.match(await page.locator('#dialog-content').innerText(), /2026-10-20 · 08:02–17:00 · DEMO/);
  assert.match(await page.locator('#dialog-content').innerText(), /leave requests \(1\)/i);
  assert.match(await page.locator('#dialog-content').innerText(), /Vacation · 2026-11-20–2026-11-20 · stored status: Pending review/);
  assert.match(await page.locator('#dialog-content').innerText(), /tickets \(0\)/i);
  await page.locator('#dialog-done').click();
  console.log('PASS: employee detail combines only that employee’s raw rows and stored request/ticket labels.');

  await page.reload();
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await page.waitForFunction(() => document.querySelectorAll('#employee-rows tr').length === 4, { timeout: timeoutMs });
  await page.locator('[data-view="requests"]').click();
  await page.waitForFunction(id => [...document.querySelectorAll('#leave-request-rows tr')]
    .some(row => row.textContent.includes(id) && row.textContent.includes('Cancelled')), leaveId, { timeout: timeoutMs });
  await page.waitForFunction(id => [...document.querySelectorAll('#ticket-rows tr')]
    .some(row => row.textContent.includes(id) && row.textContent.includes('Cancelled')), ticketId, { timeout: timeoutMs });
  await page.locator('[data-view="reports"]').click();
  captureSandboxReads = true;
  const [workspaceDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#sandbox-export-button').click()
  ]);
  assert.match(workspaceDownload.suggestedFilename(), /^motorph-demo-workspace-\d{4}-\d{2}-\d{2}\.json$/);
  const workspaceBundle = JSON.parse(readFileSync(await workspaceDownload.path(), 'utf8'));
  assert.equal(workspaceBundle.storageMode, 'local-synthetic-demo');
  assert.equal(workspaceBundle.syntheticOnly, true);
  assert.equal(workspaceBundle.hoursCalculated, false);
  assert.equal(workspaceBundle.payrollIncluded, false);
  assert.equal(workspaceBundle.employees.length, 4);
  assert.equal(workspaceBundle.attendance.length, 7);
  assert.equal(workspaceBundle.leaveRequests.length, 2);
  assert.equal(workspaceBundle.tickets.length, 1);
  await page.waitForFunction(() => document.getElementById('sandbox-export-status').textContent.includes('No calculations were added.'), { timeout: timeoutMs });
  await page.locator('#sandbox-search-query').fill('DEMO-002');
  await page.locator('#sandbox-search-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('sandbox-search-status').textContent.includes('matching stored records'), { timeout: timeoutMs });
  assert.match(await page.locator('#sandbox-search-status').textContent(), /5 matching stored records/);
  assert.match(await page.locator('#sandbox-search-results').innerText(), /2026-10-20/);
  assert.match(await page.locator('#sandbox-search-results').innerText(), /Vacation · 2026-11-20/);
  await page.locator('#sandbox-search-query').fill('08:02');
  await page.locator('#sandbox-search-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('sandbox-search-status').textContent.includes('1 matching stored record'), { timeout: timeoutMs });
  assert.match(await page.locator('#sandbox-search-results').innerText(), /2026-10-20/);
  captureSandboxReads = false;
  assert.ok(sandboxSnapshotMethods.length > 0);
  assert.ok(sandboxSnapshotMethods.every(method => method === 'GET'), `sandbox search/export made non-read request(s): ${sandboxSnapshotMethods.join(', ')}`);
  await page.waitForFunction(() => {
    const row = [...document.querySelectorAll('#employee-record-summary-rows tr')].find(item => item.textContent.includes('DEMO-002'));
    return row && row.cells[2]?.textContent.trim() === '3';
  }, { timeout: timeoutMs });
  const employeeRecordRow = page.locator('#employee-record-summary-rows tr').filter({ hasText: 'DEMO-002' });
  assert.equal((await employeeRecordRow.locator('td').nth(2).textContent()).trim(), '3');
  assert.equal((await employeeRecordRow.locator('td').nth(5).textContent()).trim(), '1');
  assert.equal((await employeeRecordRow.locator('td').nth(6).textContent()).trim(), '0');
  const [employeeSummaryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#employee-record-summary-export').click()
  ]);
  assert.equal(employeeSummaryDownload.suggestedFilename(), 'motorph-employee-record-counts.csv');
  const employeeSummaryCsv = readFileSync(await employeeSummaryDownload.path(), 'utf8');
  assert.match(employeeSummaryCsv, /"Employee ID","Employee Name","Department"/);
  assert.match(employeeSummaryCsv, /DEMO-002.*3.*1.*0/);
  await page.locator('#employee-record-summary-mapping').selectOption('unmapped');
  assert.equal(await page.locator('#employee-record-summary-rows tr').count(), 1);
  assert.match(await page.locator('#employee-record-summary-rows').innerText(), new RegExp(employeeId));
  const [visibleEmployeeSummaryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#employee-record-summary-visible-export').click()
  ]);
  const visibleEmployeeSummaryCsv = readFileSync(await visibleEmployeeSummaryDownload.path(), 'utf8');
  assert.ok(visibleEmployeeSummaryCsv.includes(employeeId) && !visibleEmployeeSummaryCsv.includes('DEMO-002'));
  await page.locator('#employee-record-summary-mapping').selectOption('all');
  await page.waitForFunction(() => document.getElementById('workflow-summary-count').textContent.includes('3 workflow records'), { timeout: timeoutMs });
  assert.equal(await page.locator('#workflow-summary-rows tr').count(), 7);
  assert.match(await page.locator('#workflow-summary-rows').innerText(), /Personal/);
  assert.match(await page.locator('#workflow-summary-rows').innerText(), /Technical support/);
  const [workflowSummaryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#workflow-summary-export').click()
  ]);
  assert.equal(workflowSummaryDownload.suggestedFilename(), 'motorph-workflow-label-summary.csv');
  assert.match(readFileSync(await workflowSummaryDownload.path(), 'utf8'), /"Ticket queue label","IT Admin","1"/);
  console.log('PASS: employee/workflow persistence and synthetic workflow-label summary/export after reload.');
  await page.locator('[data-view="requests"]').click();

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#leave-export-button').click()
  ]);
  assert.match(download.suggestedFilename(), /\.csv$/);
  await page.setViewportSize({ width: 390, height: 844 });
  const widths = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth }));
  assert.ok(widths.document <= widths.viewport, `mobile layout overflows: ${JSON.stringify(widths)}`);
  assert.deepEqual(pageErrors, [], `browser errors: ${pageErrors.join('; ')}`);
  console.log(`PASS: CSV download (${download.suggestedFilename()}), mobile width (${widths.document}px), and no page errors.`);
}

try {
  await main();
} finally {
  if (browser) await browser.close().catch(() => {});
  for (const child of serverChildren.reverse()) await stop(child);
  rmSync(javaClasses, { recursive: true, force: true });
}
