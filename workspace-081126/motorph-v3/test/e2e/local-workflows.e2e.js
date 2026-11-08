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
    // Node refuses to spawn .cmd files (npm.cmd) without a shell on Windows; args here are fixed, not user input.
    shell: process.platform === 'win32' && command.endsWith('.cmd'),
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
    // taskkill /T also stops children of the npm.cmd shell (Vite), which child.kill would leave running.
    if (process.platform === 'win32') execFileSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
    else process.kill(-child.pid, 'SIGTERM');
  } catch { /* already stopped */ }
  await Promise.race([
    new Promise(resolve => child.once('exit', resolve)),
    new Promise(resolve => setTimeout(resolve, 2500))
  ]);
  if (child.exitCode === null) {
    try {
      if (process.platform === 'win32') execFileSync('taskkill', ['/pid', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
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
    FIREBASE_PROJECT_ID: '',
    // Each run starts from the built-in demo data and never touches data/local-demo.json.
    LOCAL_DATA_FILE: ''
  };
  const api = start('java', ['--add-modules', 'jdk.httpserver', '-cp', `${gsonJar}${path.delimiter}${javaClasses}`, 'ph.motorph.web.AppServer'], apiEnv, 'Java API');
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
  // Local demo: choose an account; each role gets its own workspace. Most of this flow runs as the System Admin.
  // The shared demo password (SETUP.md), then the demo 2FA code shown on screen (there is no phone).
  const demoPassword = 'MotorPH-demo-2026';
  async function enterPassword(password = demoPassword) {
    await page.waitForSelector('#password-form:not([hidden])', { timeout: timeoutMs });
    await page.locator('#signin-password').fill(password);
    await page.locator('#password-form button[type="submit"]').click();
  }
  async function enterTwoFactorCode() {
    await page.waitForSelector('#twofa-form:not([hidden])', { timeout: timeoutMs });
    const code = (await page.locator('#twofa-demo').innerText()).match(/\b(\d{6})\b/)[1];
    await page.locator('#twofa-code').fill(code);
    await page.locator('#twofa-form button[type="submit"]').click();
  }
  async function signInAs(name) {
    if (await page.locator('#switch-account-button').isVisible()) await page.locator('#switch-account-button').click();
    await page.getByRole('button', { name: new RegExp(`^Sign in as ${name} `) }).click();
    await enterPassword();
    await enterTwoFactorCode();
    await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
    await page.waitForFunction(expected => document.getElementById('operator-label').textContent.startsWith(expected), name, { timeout: timeoutMs });
  }
  // The desk pages show one section tab at a time; these steps use "Show all sections" (tabs are checked separately).
  await page.addInitScript(() => localStorage.setItem('motorph-all-sections', 'true'));
  await page.goto(baseUrl);
  await page.waitForSelector('#account-gate:not([hidden])', { timeout: timeoutMs });
  assert.equal(await page.locator('.account-option').count(), 5);
  // 2FA: a wrong code is refused with the tries left; the right one signs in.
  await page.getByRole('button', { name: /^Sign in as Lia Torres / }).click();
  await enterPassword('wrong-demo-password');
  await page.waitForFunction(() => document.getElementById('account-error').textContent.includes('4 tries left before the account locks'), { timeout: timeoutMs });
  await enterPassword();
  await page.waitForSelector('#twofa-form:not([hidden])', { timeout: timeoutMs });
  assert.match(await page.locator('#twofa-sent').innerText(), /phone number ending in 05/);
  const liaCode = (await page.locator('#twofa-demo').innerText()).match(/\b(\d{6})\b/)[1];
  await page.locator('#twofa-code').fill(liaCode === '000000' ? '111111' : '000000');
  await page.locator('#twofa-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('account-error').textContent.includes('2 tries left'), { timeout: timeoutMs });
  await page.locator('#twofa-code').fill(liaCode);
  await page.locator('#twofa-form button[type="submit"]').click();
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await page.waitForFunction(() => document.getElementById('operator-label').textContent.startsWith('Lia Torres'), { timeout: timeoutMs });
  console.log('PASS: password then demo 2FA: a wrong password and a wrong code are refused with the tries left; the right ones sign in.');
  // Every role lands on Overview (the branch page); the directory steps below start from Employees.
  assert.equal(await page.locator('#overview-view').isVisible(), true, 'sign-in lands on the branch page');
  await page.locator('[data-view="employees"]').click();
  await page.waitForFunction(() => document.querySelectorAll('#employee-rows tr').length === 5, { timeout: timeoutMs });
  assert.equal((await page.locator('#workflow-storage-mode').textContent()).trim(), 'Local API memory');
  assert.equal((await page.locator('#overview-workflow-count').textContent()).trim(), '00');
  await page.locator('#employee-sort').selectOption('id');
  await page.locator('#employee-order').selectOption('desc');
  const employeeIds = await page.locator('#employee-rows .person-id').allTextContents();
  assert.deepEqual(employeeIds, ['DEMO-005', 'DEMO-004', 'DEMO-003', 'DEMO-002', 'DEMO-001']);
  const [employeeSortedDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#employee-export-button').click()
  ]);
  const employeeSortedCsv = readFileSync(await employeeSortedDownload.path(), 'utf8');
  assert.ok(employeeSortedCsv.indexOf('DEMO-003') < employeeSortedCsv.indexOf('DEMO-002'));
  assert.ok(employeeSortedCsv.indexOf('DEMO-002') < employeeSortedCsv.indexOf('DEMO-001'));
  console.log('PASS: System Admin workspace loads all five synthetic records and employee search/export follows selected sort order.');

  const payrollApiRequests = [];
  page.on('request', request => { if (request.url().includes('/api/payroll')) payrollApiRequests.push(request.url()); });
  await page.locator('[data-view="payroll"]').click();
  assert.match(await page.locator('#payroll-view').innerText(), /Temporary ruleset — preview only/);
  await page.locator('#payroll-preview-form button[type="submit"]').click();
  await page.waitForFunction(() => !document.getElementById('payroll-preview-results').hidden, { timeout: timeoutMs });
  assert.equal((await page.locator('#payroll-result-net').textContent()).trim(), '₱22,611.25');
  assert.equal((await page.locator('#payroll-result-employee-total').textContent()).trim(), '₱2,388.75');
  assert.equal((await page.locator('#payroll-result-employer-total').textContent()).trim(), '₱3,355.00');
  assert.match(await page.locator('#payroll-result-lines').innerText(), /₱313.75/);
  const [payrollPreviewDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#payroll-preview-export').click()
  ]);
  assert.equal(payrollPreviewDownload.suggestedFilename(), 'motorph-2026-monthly-preview-not-payslip.csv');
  const payrollPreviewCsv = readFileSync(await payrollPreviewDownload.path(), 'utf8');
  assert.match(payrollPreviewCsv, /Net cash pay estimate/);
  assert.match(payrollPreviewCsv, /₱22,611\.25/);
  assert.match(await page.locator('#payroll-preview-export-status').textContent(), /not a payslip/);
  assert.deepEqual(payrollApiRequests.map(url => new URL(url).pathname), ['/api/payroll/statutory'],
    'payroll preview is calculated by the Java statutory API only');
  await page.locator('#payroll-preview-gross').fill('100');
  await page.locator('#payroll-preview-form button[type="submit"]').click();
  await page.waitForFunction(() => /deductions exceed gross cash pay/.test(document.getElementById('payroll-preview-status').textContent), { timeout: timeoutMs });
  assert.equal(await page.locator('#payroll-preview-results').isHidden(), true);
  assert.equal(await page.locator('#payroll-preview-export').isDisabled(), true);
  await page.locator('#payroll-preview-gross').fill('25000.00');
  console.log('PASS: 2026 statutory preview is calculated by the Java API (sweldongpinoy.com values) and blocks negative net.');

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
  // Attendance summary (requirement 3): 3 rows on Mara's 11 workdays from 1 to 15 Oct, none late.
  await page.waitForFunction(() => document.getElementById('attendance-metrics').textContent.includes('DAYS PRESENT3'), { timeout: timeoutMs });
  assert.match(await page.locator('#attendance-metrics').innerText(), /DAYS ABSENT\s+8[\s\S]*DAYS TARDY\s+0/);
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
  assert.equal((await page.locator('#directory-report-total').textContent()).trim(), '06');
  assert.equal((await page.locator('#directory-report-active').textContent()).trim(), '05');
  assert.equal((await page.locator('#directory-report-onleave').textContent()).trim(), '01');
  assert.match(await page.locator('#directory-department-counts').innerText(), /QA Demo: 1/);
  const [directorySummaryDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#directory-summary-export').click()
  ]);
  assert.equal(directorySummaryDownload.suggestedFilename(), 'motorph-directory-label-summary.csv');
  const directorySummaryCsv = readFileSync(await directorySummaryDownload.path(), 'utf8');
  assert.match(directorySummaryCsv, /"Status label","Active","5"/);
  assert.match(directorySummaryCsv, /"Department label","QA Demo","1"/);
  await page.waitForFunction(() => document.querySelectorAll('#attendance-report-rows tr').length === 6, { timeout: timeoutMs });
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
  assert.equal(await page.locator('#team-rows tr').count(), 6);
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
  assert.equal(await page.locator('#attendance-report-rows tr').count(), 6);
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
  assert.match(await page.locator('#leave-calendar-grid').innerText(), /Personal \(cancelled\)/);
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
  assert.match(await page.locator('#ticket-rows tr', { hasText: ticketId }).innerText(), /IT Admin · #1 in queue/, 'the ticket shows its place in the queue');
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
  await page.waitForFunction(() => document.querySelectorAll('#employee-rows tr').length === 6, { timeout: timeoutMs });
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
  assert.equal(workspaceBundle.employees.length, 6);
  assert.equal(workspaceBundle.attendance.length, 9);
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

  // Temporary leave credits and local-demo approval. DEMO-002 has one pending 1-weekday Vacation request (2026-11-20).
  const balanceRow = type => page.locator('#leave-balance tbody tr').filter({ hasText: type });
  await page.locator('#leave-filter-search').fill('');
  await page.locator('#leave-filter-status').selectOption('');
  await page.locator('#leave-filter-type').selectOption('');
  await page.locator('#leave-employee').selectOption('DEMO-002');
  await page.locator('#leave-start').fill('2026-11-23');
  await page.locator('#leave-start').dispatchEvent('change');
  await page.waitForFunction(() => /Vacation\s+5\s+0\s+1\s+5/.test(document.getElementById('leave-balance').innerText), { timeout: timeoutMs });
  await page.locator('#leave-type').selectOption('Vacation');
  await page.locator('#leave-end').fill('2026-11-27');
  await page.locator('#leave-note').fill('');
  await page.locator('#leave-request-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('leave-form-status').textContent.includes('Not enough Vacation credits'), { timeout: timeoutMs });
  const pendingRow = page.locator('#leave-request-rows tr').filter({ hasText: secondLeaveId });
  await signInAs('Eli Santos');
  await page.locator('[data-view="requests"]').click();
  await pendingRow.waitFor({ timeout: timeoutMs });
  assert.equal(await pendingRow.getByRole('button', { name: /^Approve/ }).count(), 0, 'nobody can approve their own request');
  await signInAs('Noa Reyes');
  await page.locator('[data-view="requests"]').click();
  await pendingRow.getByRole('button', { name: new RegExp(`Approve ${secondLeaveId}`) }).click();
  await page.waitForFunction(id => {
    const row = [...document.querySelectorAll('#leave-request-rows tr')].find(item => item.textContent.includes(id));
    return row && row.textContent.includes('Approved');
  }, secondLeaveId, { timeout: timeoutMs });
  await pendingRow.locator('details summary').click();
  assert.match(await pendingRow.innerText(), /APPROVED BY\s+DEMO-003/);
  await signInAs('Lia Torres');
  await page.locator('[data-view="requests"]').click();
  await page.locator('#leave-employee').selectOption('DEMO-002');
  await page.locator('#leave-start').fill('2026-11-23');
  await page.locator('#leave-start').dispatchEvent('change');
  await page.waitForFunction(() => /Vacation\s+5\s+1\s+0\s+4/.test(document.getElementById('leave-balance').innerText), { timeout: timeoutMs });
  assert.match(await balanceRow('Personal').innerText(), /3\s+0\s+0\s+3/);
  console.log('PASS: leave credits block over-limit requests; the employee cannot approve their own leave, the supervisor approves it as the signed-in account, and credits are used.');

  // Attendance-based cutoff payroll and payslip for DEMO-002, October 2026 (hand-worked: rate 60,000 / 168 = 357.14; 2026-10-03 is a Saturday, so +30% rest-day premium 857.14; sweldongpinoy.com deductions).
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-cutoff-employee').selectOption('DEMO-002');
  await page.locator('#payroll-cutoff-month').fill('2026-10');
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.waitForFunction(() => document.querySelectorAll('#payroll-cutoff-rows tr').length === 2, { timeout: timeoutMs });
  const cutoffText = await page.locator('#payroll-cutoff-rows').innerText();
  assert.match(cutoffText, /1–15[\s\S]*16\.00 \(\+0\.08 unapproved, unpaid\)[\s\S]*₱6,571\.38[\s\S]*₱456\.78[\s\S]*₱7,864\.60/);
  assert.match(cutoffText, /16–end[\s\S]*8\.00[\s\S]*₱2,857\.12[\s\S]*₱456\.79[\s\S]*₱4,150\.33/);
  await page.getByRole('button', { name: 'View payslip for DEMO-002, 16–end 2026-10' }).click();
  await page.waitForFunction(() => !document.getElementById('payroll-payslip').hidden, { timeout: timeoutMs });
  const payslipText = await page.locator('#payroll-payslip').innerText();
  assert.match(payslipText, /SYNTHETIC DEMO — NOT A REAL PAYSLIP/);
  assert.match(payslipText, /PS-DEMO-002-2026-10-2/);
  assert.match(payslipText, /Eli Santos/);
  assert.match(payslipText, /SSS \(employee share\)\s+−₱237\.50/);
  assert.match(payslipText, /Pag-IBIG \(employee share\)\s+−₱94\.29/);
  assert.match(payslipText, /Rice subsidy \(non-taxable\)\s+\+₱750\.00[\s\S]*NET PAY\s+₱4,150\.33/);
  console.log('PASS: cutoff payroll derives gross from attendance, splits Java deductions half per cutoff, and shows a printable synthetic payslip.');

  // Overtime: DEMO-002 worked 2 min past 8 h on 2026-10-01. Approving 0.25 h pays those 2 min at 125%:
  // 2.5 / 60 x 357.14 = 14.88; gross 6,586.26; month 9,443.38 -> Pag-IBIG 188.87 (94.43 on cutoff 1); net 6,129.33 + allowances 1,750 = 7,879.33.
  assert.equal(await page.locator('#overtime-panel').isHidden(), false);
  await page.locator('#overtime-employee').selectOption('DEMO-002');
  await page.locator('#overtime-date').fill('2026-10-01');
  await page.locator('#overtime-hours').fill('0.25');
  await page.locator('#overtime-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('overtime-status').textContent.startsWith('Submitted OT-'), { timeout: timeoutMs });
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  await page.waitForFunction(() => document.getElementById('overtime-rows').textContent.includes('Pending review'), { timeout: timeoutMs });
  assert.equal(await page.locator('#overtime-rows button').count(), 0, 'nobody can approve their own overtime');
  await signInAs('Mara Rivera');
  await page.locator('[data-view="payroll"]').click();
  await page.waitForFunction(() => document.getElementById('overtime-rows').textContent.includes('Pending review'), { timeout: timeoutMs });
  assert.equal(await page.locator('#overtime-rows button').count(), 0, 'HR cannot approve overtime');
  await signInAs('Noa Reyes');
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#overtime-rows button', { hasText: 'Approve' }).click();
  await page.waitForFunction(() => document.getElementById('overtime-rows').textContent.includes('Approved by DEMO-003'), { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-cutoff-employee').selectOption('DEMO-002');
  await page.locator('#payroll-cutoff-month').fill('2026-10');
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('payroll-cutoff-rows').textContent.includes('OT'), { timeout: timeoutMs });
  assert.match(await page.locator('#payroll-cutoff-rows').innerText(), /16\.00 \+ 0\.03 OT \(\+0\.05 unapproved, unpaid\)[\s\S]*₱6,586\.26[\s\S]*₱456\.93[\s\S]*₱7,879\.33/);
  await page.getByRole('button', { name: 'View payslip for DEMO-002, 1–15 2026-10' }).click();
  await page.waitForFunction(() => !document.getElementById('payroll-payslip').hidden, { timeout: timeoutMs });
  const overtimeSlip = await page.locator('#payroll-payslip').innerText();
  assert.match(overtimeSlip, /Rest-day \/ holiday premium\s+₱857\.14/);
  assert.match(overtimeSlip, /Overtime \(approved\)\s+₱14\.88/);
  console.log('PASS: overtime is paid only after another employee approves it, with rest-day premium and overtime on the payslip.');

  // Saved payroll history: finalize the open payslip, then reopen the frozen copy from the history table.
  await page.locator('#payslip-finalize').click();
  await page.waitForFunction(() => document.getElementById('payroll-history-rows').textContent.includes('PS-DEMO-002-2026-10-1'), { timeout: timeoutMs });
  assert.equal(await page.locator('#payslip-finalize').isHidden(), true, 'a saved payslip cannot be finalized again');
  assert.match(await page.locator('#payslip-footnote').innerText(), /^Finalized .*will not change/);
  assert.match(await page.locator('#payroll-history-rows').innerText(), /PS-DEMO-002-2026-10-1[\s\S]*2026-10 · 1–15[\s\S]*₱6,586\.26[\s\S]*₱7,879\.33/);
  await page.getByRole('button', { name: 'View payslip for DEMO-002, 16–end 2026-10' }).click();
  await page.waitForFunction(() => document.getElementById('payslip-fields').textContent.includes('PS-DEMO-002-2026-10-2'), { timeout: timeoutMs });
  assert.equal(await page.locator('#payslip-finalize').isHidden(), false);
  await page.getByRole('button', { name: 'Open saved payslip PS-DEMO-002-2026-10-1' }).click();
  await page.waitForFunction(() => document.getElementById('payslip-fields').textContent.includes('PS-DEMO-002-2026-10-1'), { timeout: timeoutMs });
  assert.match(await page.locator('#payroll-payslip').innerText(), /FINALIZED — SAVED SNAPSHOT[\s\S]*NET PAY\s+₱7,879\.33/);
  console.log('PASS: a payslip is finalized once into saved payroll history and reopens unchanged.');

  // Gross to net (hand-worked): Jae, October 1-15: 16 h x 148.81 = 2,380.96 earned + 1,000 bonus = 3,380.96 gross;
  // SSS 125, PhilHealth 125, Pag-IBIG 23.81 (2% of 2,380.96 / 2), tax 0; loan 300; allowances 1,250 -> net 4,057.15.
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  assert.equal(await page.locator('#adj-employee option[value="DEMO-002"]').count(), 0, 'payroll staff cannot pick themselves');
  await page.locator('#adj-employee').selectOption('DEMO-004');
  await page.waitForFunction(() => document.getElementById('adj-allowances').textContent.includes('Paid by Check'), { timeout: timeoutMs });
  await page.locator('#adj-earning-month').fill('2026-10');
  await page.locator('#adj-earning-amount').fill('1000');
  await page.locator('#adj-earning-description').fill('Spot award');
  await page.locator('#adj-earning-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('adj-status').textContent.startsWith('Added bonus'), { timeout: timeoutMs });
  await page.locator('#adj-deduction-amount').fill('300');
  await page.locator('#adj-deduction-from').fill('2026-10');
  await page.locator('#adj-deduction-to').fill('2026-10');
  await page.locator('#adj-deduction-form button[type="submit"]').click();
  await page.waitForFunction(() => !document.getElementById('adj-status').textContent.startsWith('Added bonus'), { timeout: timeoutMs });
  assert.match(await page.locator('#adj-status').innerText(), /^Added company loan/);
  await page.waitForFunction(() => document.querySelectorAll('#adj-rows button[data-adjustment-id]').length === 2, { timeout: timeoutMs });
  await page.locator('#payroll-cutoff-employee').selectOption('DEMO-004');
  await page.locator('#payroll-cutoff-month').fill('2026-10');
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('payroll-cutoff-rows').textContent.includes('4,057.15'), { timeout: timeoutMs });
  await page.getByRole('button', { name: 'View payslip for DEMO-004, 1–15 2026-10' }).click();
  await page.waitForFunction(() => document.getElementById('payslip-fields').textContent.includes('PS-DEMO-004-2026-10-1'), { timeout: timeoutMs });
  const g2nSlip = await page.locator('#payroll-payslip').innerText();
  assert.match(g2nSlip, /Bonus — Spot award \(taxable\)\s+₱1,000\.00[\s\S]*Gross pay\s+₱3,380\.96/);
  assert.match(g2nSlip, /Company loan\s+−₱300\.00[\s\S]*Total deductions\s+−₱573\.81/);
  assert.match(g2nSlip, /Phone allowance \(non-taxable\)\s+\+₱250\.00[\s\S]*NET PAY\s+₱4,057\.15/);
  await page.locator('#payslip-finalize').click();
  await page.waitForFunction(() => document.getElementById('payroll-history-rows').textContent.includes('PS-DEMO-004-2026-10-1'), { timeout: timeoutMs });
  await page.locator('#disbursement-month').fill('2026-10');
  await page.locator('#disbursement-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('disbursement-status').textContent.startsWith('Released'), { timeout: timeoutMs });
  assert.match(await page.locator('#disbursement-rows').innerText(), /Jae Cruz ₱4,057\.15 \(Check\)/);
  // Payslip email (simulated): the release emails Jae's payslip; an outage makes a resend fail, and the retry sends it.
  assert.match(await page.locator('#disbursement-status').innerText(), /(\d+) payslip\(s\).*Emailed \1\./, 'every released payslip is emailed');
  await page.waitForFunction(() => document.getElementById('outbox-rows').textContent.includes('jae.cruz@motorph.example'), { timeout: timeoutMs });
  await page.locator('#outbox-outage').check();
  await page.waitForFunction(() => document.getElementById('outbox-status').textContent.startsWith('Simulated mail outage on'), { timeout: timeoutMs });
  await page.locator('#outbox-rows tr', { hasText: 'jae.cruz@motorph.example' }).getByRole('button', { name: /^Resend EM-/ }).click();
  await page.waitForFunction(() => document.getElementById('outbox-status').textContent.includes('failed again'), { timeout: timeoutMs });
  await page.locator('#outbox-outage').uncheck();
  await page.waitForFunction(() => document.getElementById('outbox-status').textContent.startsWith('Mail server back'), { timeout: timeoutMs });
  await page.locator('#outbox-retry').click();
  await page.waitForFunction(() => document.getElementById('outbox-status').textContent === 'Retried failed emails: 1 sent now.', { timeout: timeoutMs });
  assert.match(await page.locator('#outbox-rows tr', { hasText: 'jae.cruz@motorph.example' }).innerText(), /Sent \(simulated\)\s+3 · last/);
  // The email's PDF downloads, the payroll team got the collated report, and the cycle is Distributed.
  const [pdfDownload] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#outbox-rows tr', { hasText: 'jae.cruz@motorph.example' }).getByRole('button', { name: /^Download PS-.*\.pdf$/ }).click()
  ]);
  assert.match(pdfDownload.suggestedFilename(), /^PS-DEMO-004-2026-10-1\.pdf$/);
  assert.match(await page.locator('#outbox-rows').innerText(), /payroll@motorph\.example/);
  await page.locator('#disbursement-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('disbursement-rows').textContent.includes('· Distributed'), { timeout: timeoutMs });
  await page.locator('#disbursement-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('disbursement-status').textContent.includes('No finalized payslips are waiting'), { timeout: timeoutMs });
  await signInAs('Mara Rivera');
  await page.locator('[data-view="payroll"]').click();
  assert.equal(await page.locator('#adj-earning-form').isVisible(), false, 'HR only views payroll items');
  await page.locator('#remittance-month').fill('2026-10');
  await page.locator('#remittance-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('remittance-rows').textContent.includes('Pag-IBIG'), { timeout: timeoutMs });
  assert.match(await page.locator('#remittance-rows').innerText(), /SSS[\s\S]*PhilHealth[\s\S]*BIR \(withholding tax\)/);
  console.log('PASS: payroll staff run gross to net (bonus, loan, allowances), finalize, release a disbursement batch; HR reads the remittance report.');

  // Supervisor tools: Noa sets Jae's schedule, corrects a timesheet and files a merit increase; HR approves it.
  await signInAs('Noa Reyes');
  await page.locator('[data-view="team"]').click();
  // Choosing a person loads their current schedule into the form; wait for it before typing.
  await Promise.all([page.waitForResponse(response => response.url().endsWith('/api/schedules')), page.locator('#schedule-employee').selectOption('DEMO-004')]);
  await page.locator('#schedule-start').fill('10:00');
  await page.locator('#schedule-day-MON').uncheck();
  await page.locator('#schedule-day-SAT').check();
  await page.locator('#schedule-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('schedule-status').textContent.startsWith('Saved: 10:00–19:00, Tue–Sat'), { timeout: timeoutMs });
  assert.match(await page.locator('#schedule-rows').innerText(), /DEMO-004 — Jae Cruz\s+10:00–19:00\s+14:00 \(1 h\)\s+Tue–Sat\s+DEMO-003/);
  await page.locator('[data-view="attendance"]').click();
  await page.locator('#correction-employee').selectOption('DEMO-004');
  await page.waitForFunction(() => document.querySelector('#correction-row option[value="5302"]'), { timeout: timeoutMs });
  await page.locator('#correction-row').selectOption('5302');
  await page.locator('#correction-in').fill('10:05');
  await page.locator('#correction-out').fill('19:00');
  await page.locator('#correction-reason').fill('Badge reader fault');
  await page.locator('#correction-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('correction-status').textContent.startsWith('Corrected 2026-10-06'), { timeout: timeoutMs });
  assert.match(await page.locator('#correction-rows').innerText(), /DEMO-004 · 2026-10-06\s+08:06–17:00\s+10:05–19:00\s+Badge reader fault\s+DEMO-003/);
  await page.locator('[data-view="requests"]').click();
  await page.locator('#paychange-employee').selectOption('DEMO-004');
  await page.locator('#paychange-type').selectOption('MERIT');
  assert.equal(await page.locator('#paychange-position').isVisible(), false, 'merit increases need no new position');
  await page.locator('#paychange-basic').fill('27000');
  await page.locator('#paychange-effective').fill('2026-12');
  await page.locator('#paychange-note').fill('Annual review');
  await page.locator('#paychange-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('paychange-status').textContent.includes('waiting for HR'), { timeout: timeoutMs });
  assert.equal(await page.locator('#paychange-rows button').count(), 0, 'the requester cannot approve');
  await signInAs('Mara Rivera');
  await page.locator('[data-view="requests"]').click();
  await page.locator('#paychange-rows button', { hasText: 'Approve' }).click();
  await page.waitForFunction(() => document.getElementById('paychange-rows').textContent.includes('Monthly basic ₱27000.00 from 2026-12'), { timeout: timeoutMs });
  assert.equal(await page.locator('#paychange-form').isVisible(), false, 'HR decides; supervisors file');
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-cutoff-employee').selectOption('DEMO-004');
  await page.locator('#payroll-cutoff-month').fill('2026-12');
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('payroll-cutoff-status').textContent.includes('₱160.71/hour'), { timeout: timeoutMs });
  console.log('PASS: supervisor sets a team schedule, corrects a timesheet with a reason, files a merit increase; HR approves and payroll uses the new rate.');

  // Role workspaces: each account gets its own menu and only the tools its role allows.
  const menu = () => page.locator('.nav-item:not(.role-hidden)').allInnerTexts();
  await signInAs('Jae Cruz');
  assert.deepEqual(await menu(), ['Overview', 'My attendance', 'My requests', 'Team', 'My pay']);
  // My day: clock in, clock out, and the salary tracker for the current cutoff (server time).
  await page.locator('[data-view="overview"]').click();
  await page.waitForFunction(() => document.getElementById('clock-state').textContent === 'Clocked out', { timeout: timeoutMs });
  // On a rest day, holiday or leave day (it depends on today's real date) the clock asks first; say yes.
  const acceptDayType = dialog => dialog.accept();
  page.on('dialog', acceptDayType);
  await page.locator('#clock-button').click();
  await page.waitForFunction(() => document.getElementById('clock-state').textContent === 'Clocked in', { timeout: timeoutMs });
  page.off('dialog', acceptDayType);
  assert.match(await page.locator('#clock-detail').innerText(), /· (Present|Tardy)\.$/, 'clock-in tags the day Present or Tardy');
  assert.equal(await page.locator('#clock-button').innerText(), 'Clock out');
  // QR onsite check-in: clocked in from the app = offsite with a QR code; the (simulated) branch scan makes it onsite.
  assert.match(await page.locator('#clock-location').innerText(), /^Offsite/);
  assert.match(await page.locator('#onsite-qr-code').innerText(), /^MPH-[A-Z2-9]{6}$/);
  assert.equal(await page.locator('#onsite-qr-image svg rect').count() > 100, true, 'the QR picture is drawn');
  await page.locator('#onsite-scan').click();
  await page.waitForFunction(() => document.getElementById('clock-location').textContent.startsWith('Onsite'), { timeout: timeoutMs });
  assert.equal(await page.locator('#onsite-qr').isVisible(), false, 'no QR once onsite');
  // Clocking out asks first (and again before the shift ends): saying no keeps Jae clocked in.
  const questions = [];
  const answer = accept => async dialog => { questions.push(dialog.message()); await (accept ? dialog.accept() : dialog.dismiss()); };
  const decline = answer(false);
  page.on('dialog', decline);
  await page.locator('#clock-button').click();
  await page.waitForFunction(() => document.getElementById('clock-status').textContent === 'Still clocked in.', { timeout: timeoutMs });
  page.off('dialog', decline);
  assert.match(questions[0], /^Clock out now\?/);
  const agree = answer(true);
  page.on('dialog', agree);
  await page.locator('#clock-button').click();
  await page.waitForFunction(() => document.getElementById('clock-state').textContent === 'Done for today', { timeout: timeoutMs });
  page.off('dialog', agree);
  assert.match(await page.locator('#clock-detail').innerText(), /· (Present|Tardy) · (Regular|Overtime|Undertime) hours\.$/, 'clock-out tags the worked hours');
  // Notifications: Noa changed Jae's schedule earlier, so the bell has news; opening it marks it read.
  assert.match(await page.locator('#notification-bell').innerText(), /🔔 \d+/);
  await page.locator('#notification-bell').click();
  await page.waitForFunction(() => document.getElementById('notification-list').textContent.includes('Your schedule changed'), { timeout: timeoutMs });
  await page.waitForFunction(() => document.getElementById('notification-bell').textContent === '🔔', { timeout: timeoutMs });
  await page.keyboard.press('Escape');
  assert.equal(await page.locator('#notification-panel').isVisible(), false);
  // Team roster "Today": Jae worked today, so he shows Present or Tardy with his times.
  await page.locator('[data-view="team"]').click();
  await page.waitForFunction(() => /(Present|Tardy) · \d\d:\d\d–\d\d:\d\d/.test(document.querySelector('#team-rows').textContent), { timeout: timeoutMs });
  await page.locator('[data-view="overview"]').click();
  assert.equal(await page.locator('#clock-button').isVisible(), false, 'one attendance row per day');
  await page.waitForFunction(() => document.getElementById('tracker-figures').textContent.includes('GROSS SO FAR'), { timeout: timeoutMs });
  assert.match(await page.locator('#tracker-period').innerText(), /^(1–15|16–end) \d{4}-\d{2} /);
  // The overview has one card per page the role can open (same names as the menu); a card opens its page.
  assert.deepEqual(await page.locator('.tab-card:not(.role-hidden) .tab-card-title').allInnerTexts(), ['My attendance', 'My requests', 'Team', 'My pay']);
  await page.locator('.tab-card[data-open-view="payroll"]').click();
  assert.equal(await page.locator('#payroll-view').isVisible(), true, 'the My pay card opens My pay');
  await page.locator('[data-view="overview"]').click();
  await page.locator('[data-view="payroll"]').click();
  assert.equal(await page.locator('#payroll-preview-form').isVisible(), false, 'employees do not get the statutory calculator');
  assert.deepEqual(await page.locator('#payroll-cutoff-employee option').allInnerTexts(), ['Choose synthetic employee', 'DEMO-004 — Jae Cruz']);
  await page.locator('[data-view="team"]').click();
  await page.waitForFunction(() => document.querySelectorAll('#team-rows tr').length === 4, { timeout: timeoutMs });
  assert.equal(await page.locator('#team-rows tr').filter({ hasText: 'DEMO-005' }).count(), 0, 'employees see only their team');
  await signInAs('Eli Santos');
  assert.ok((await menu()).includes('Payroll processing') && !(await menu()).includes('HR desk'));
  await page.locator('[data-view="payroll"]').click();
  assert.equal(await page.locator('#payroll-preview-form').isVisible(), true, 'payroll staff get the statutory calculator');
  await signInAs('Mara Rivera');
  assert.ok((await menu()).includes('HR desk'));
  await page.locator('[data-view="attendance"]').click();
  assert.equal(await page.locator('#attendance-entry-panel').isVisible(), false, 'HR only reads attendance');
  await signInAs('Noa Reyes');
  await page.locator('[data-view="attendance"]').click();
  assert.equal(await page.locator('#attendance-entry-panel').isVisible(), true, 'supervisors edit team timesheets');
  assert.deepEqual(await page.locator('#attendance-entry-employee option').evaluateAll(options => options.map(option => option.value)), ['DEMO-001', 'DEMO-002', 'DEMO-004']);
  await signInAs('Lia Torres');
  assert.equal((await menu()).length, 9);
  console.log('PASS: each demo account gets its own role workspace (Employee, Payroll Staff, HR, Supervisor, System Admin).');

  // HR: onboard a new hire, tick the checklist, answer an HR ticket, offboard; the account closes after the last day.
  await signInAs('Jae Cruz');
  await page.locator('[data-view="requests"]').click();
  await page.locator('#ticket-type').selectOption('HR inquiry');
  await page.locator('#ticket-summary').fill('How many Personal leave days do I have left?');
  await page.locator('#ticket-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('ticket-rows').textContent.includes('How many Personal'), { timeout: timeoutMs });
  await signInAs('Mara Rivera');
  await page.locator('[data-view="requests"]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('#resolve-ticket option')].some(option => option.textContent.includes('How many Personal')), { timeout: timeoutMs });
  await page.locator('#resolve-ticket').selectOption({ index: 1 });
  await page.locator('#resolve-reply').fill('You have 2 Personal days left.');
  await page.locator('#resolve-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('resolve-status').textContent.startsWith('Resolved'), { timeout: timeoutMs });
  await page.locator('[data-view="hr"]').click();
  await page.locator('#catalog-name').fill('Logistics');
  await page.locator('#catalog-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('catalog-departments').textContent.includes('Logistics'), { timeout: timeoutMs });
  await page.locator('#onboard-first').fill('Ana');
  await page.locator('#onboard-surname').fill('Lim');
  await page.locator('#onboard-department').selectOption('Logistics');
  await page.locator('#onboard-position').selectOption('Operations Associate');
  await page.locator('#onboard-location').fill('Pasig');
  await page.locator('#onboard-start').fill('2026-10-01');
  await page.locator('#onboard-supervisor').selectOption('DEMO-003');
  await page.locator('#onboard-basic').fill('21000');
  await page.locator('#onboard-pay-method').selectOption('Check');
  await page.locator('#onboard-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('onboard-status').textContent.startsWith('Onboarded Ana Lim'), { timeout: timeoutMs });
  await page.waitForFunction(() => document.getElementById('employment-rows').textContent.includes('1 of 4 done'), { timeout: timeoutMs });
  await page.getByRole('button', { name: 'Mark "Contract signed" done for Ana Lim' }).click();
  await page.waitForFunction(() => document.getElementById('employment-rows').textContent.includes('2 of 4 done'), { timeout: timeoutMs });
  await signInAs('Ana Lim');
  assert.deepEqual(await menu(), ['Overview', 'My attendance', 'My requests', 'Team', 'My pay']);
  await signInAs('Jae Cruz');
  await page.locator('[data-view="requests"]').click();
  const answered = page.locator('#ticket-rows tr').filter({ hasText: 'How many Personal' });
  await answered.locator('details summary').click();
  assert.match(await answered.innerText(), /REPLY\s+You have 2 Personal days left\.[\s\S]*RESOLVED BY\s+DEMO-001/);
  await signInAs('Mara Rivera');
  await page.locator('[data-view="hr"]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('#offboard-employee option')].some(option => option.textContent.includes('Ana Lim')), { timeout: timeoutMs });
  await page.locator('#offboard-employee').selectOption({ label: [...await page.locator('#offboard-employee option').allInnerTexts()].find(text => text.includes('Ana Lim')) });
  await page.locator('#offboard-last-day').fill('2026-10-31');
  await page.locator('#offboard-reason').fill('Resigned');
  await page.locator('#offboard-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('offboard-status').textContent.includes('Final payslip: 2026-10 cutoff 16–end'), { timeout: timeoutMs });
  await page.locator('#switch-account-button').click();
  await page.waitForSelector('#account-gate:not([hidden])', { timeout: timeoutMs });
  assert.equal(await page.getByRole('button', { name: /^Sign in as Ana Lim / }).count(), 0, 'the account closes after the last day');
  await page.getByRole('button', { name: /^Sign in as Lia Torres / }).click();
  await enterPassword();
  await enterTwoFactorCode();
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  console.log('PASS: HR answers an HR ticket, adds a department, onboards a new hire with a checklist, and offboards them (account closed, final payslip noted).');

  // Government details (fake numbers): the admin edits; HR sees masked numbers and needs approval for full view;
  // payroll staff propose a change that waits for the admin; every full view is logged.
  const jaeGovRow = () => page.locator('#gov-rows tr[data-employee-id="DEMO-004"]');
  await page.locator('[data-view="employees"]').click();
  await page.locator('#gov-employee').selectOption('DEMO-004');
  await page.locator('#gov-sss').fill('12-3456789-0');
  await page.locator('#gov-tin').fill('123-456-789');
  assert.equal(await page.locator('#gov-save').innerText(), 'Save now');
  await page.locator('#gov-change-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('gov-status').textContent.startsWith('Saved DEMO-004'), { timeout: timeoutMs });
  await signInAs('Mara Rivera');
  await page.locator('[data-view="employees"]').click();
  await page.waitForFunction(() => (document.querySelector('#gov-rows tr[data-employee-id="DEMO-004"]') || {}).textContent?.includes('••-••••789-0'), { timeout: timeoutMs });
  await jaeGovRow().getByRole('button', { name: /Show full/ }).click();
  await page.waitForFunction(() => document.getElementById('gov-status').textContent.includes('approved access request'), { timeout: timeoutMs });
  await page.locator('#gov-access-employee').selectOption('DEMO-004');
  await page.locator('#gov-access-reason').fill('Remittance check');
  await page.locator('#gov-access-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('gov-status').textContent.startsWith('Asked for full view'), { timeout: timeoutMs });
  await signInAs('Eli Santos');
  await page.locator('[data-view="employees"]').click();
  await page.locator('#gov-employee').selectOption('DEMO-004');
  await page.locator('#gov-pagIbig').fill('1234-5678-9012');
  await page.locator('#gov-reason').fill('New MID card');
  await page.locator('#gov-change-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('gov-status').textContent.includes('for approval'), { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="employees"]').click();
  await page.waitForFunction(() => document.querySelectorAll('#gov-request-rows button').length === 4, { timeout: timeoutMs });
  for (let i = 0; i < 2; i++) {
    await page.locator('#gov-request-rows button', { hasText: 'Approve' }).first().click();
    await page.waitForFunction(expected => document.querySelectorAll('#gov-request-rows button').length === expected, 2 - 2 * i, { timeout: timeoutMs });
  }
  await jaeGovRow().getByRole('button', { name: /Show full/ }).click();
  await page.waitForFunction(() => document.querySelector('#gov-rows tr[data-employee-id="DEMO-004"]').textContent.includes('1234-5678-9012'), { timeout: timeoutMs });
  await signInAs('Mara Rivera');
  await page.locator('[data-view="employees"]').click();
  await page.waitForFunction(() => document.querySelector('#gov-rows tr[data-employee-id="DEMO-004"]'), { timeout: timeoutMs });
  await jaeGovRow().getByRole('button', { name: /Show full/ }).click();
  await page.waitForFunction(() => document.querySelector('#gov-rows tr[data-employee-id="DEMO-004"]').textContent.includes('12-3456789-0'), { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="employees"]').click();
  await page.waitForFunction(() => document.getElementById('gov-audit-rows').textContent.includes('VIEWED_FULL'), { timeout: timeoutMs });
  assert.match(await page.locator('#gov-audit-rows').innerText(), /DEMO-001\s+DEMO-004\s+VIEWED_FULL/);
  await signInAs('Jae Cruz');
  assert.equal(await page.locator('#government-panel').isVisible(), false, 'employees have no government-details access');
  await signInAs('Lia Torres');
  console.log('PASS: government details: admin edits directly, HR sees masked numbers and full view only after admin approval, payroll proposes a change the admin approves, full views are audited.');

  // System configuration (admin): a rates version from December, and a temporary role change.
  await page.locator('[data-view="config"]').click();
  await page.waitForFunction(() => document.getElementById('rate-sss-employeePercent').value === '5', { timeout: timeoutMs });
  await page.locator('#rates-month').fill('2026-12');
  await page.locator('#rates-note').fill('SSS 5.5% test');
  await page.locator('#rate-sss-employeePercent').fill('5.5');
  await page.locator('#rates-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('rates-status').textContent.startsWith('Saved PH-2026-admin-v1'), { timeout: timeoutMs });
  assert.match(await page.locator('#rates-rows').innerText(), /PH-2026-admin-v1\s+2026-12\s+SSS 5\.5% \/ 10%/);
  await page.locator('#role-employee').selectOption('DEMO-004');
  await page.locator('#role-role').selectOption('SUPERVISOR');
  await page.locator('#role-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('role-status').textContent.includes('Employee / DEMO-003 → Supervisor / DEMO-003'), { timeout: timeoutMs });
  await signInAs('Jae Cruz');
  assert.ok((await menu()).includes('Requests & approvals'), 'the new role shows at the next sign-in');
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-preview-period').fill('2026-12');
  await page.locator('#payroll-preview-gross').fill('25000.00');
  await page.locator('#payroll-preview-form button[type="submit"]').click();
  // December uses the admin version: SSS 5.5% of 25,000 = 1,375; tax 295; net 22,505.
  await page.waitForFunction(() => document.getElementById('payroll-preview-results').textContent.includes('22,505.00'), { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="config"]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('#role-employee option')].length > 0, { timeout: timeoutMs });
  await page.locator('#role-employee').selectOption('DEMO-004');
  await page.locator('#role-role').selectOption('EMPLOYEE');
  await page.locator('#role-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('role-status').textContent.includes('→ Employee / DEMO-003'), { timeout: timeoutMs });
  assert.match(await page.locator('#role-log-rows').innerText(), /DEMO-005\s+DEMO-004\s+Supervisor \/ DEMO-003\s+Employee \/ DEMO-003/);
  console.log('PASS: system configuration: the admin adds a December rates version payroll uses, and changes a role that applies at the next sign-in.');

  // Payslip corrections. Void: Eli finalizes Noa's October 1-15 payslip, asks to void it, Lia approves.
  // Adjust: Jae's released October 1-15 payslip predates his Tue-Sat schedule; Monday 5 Oct is now a rest day, so the
  // recalculation adds the 30% premium: 480 min x 30% = 144 min x 148.81 / 60 = 357.14 underpaid.
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-cutoff-employee').selectOption('DEMO-003');
  await page.locator('#payroll-cutoff-month').fill('2026-10');
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.getByRole('button', { name: 'View payslip for DEMO-003, 1–15 2026-10' }).click();
  await page.waitForFunction(() => document.getElementById('payslip-fields').textContent.includes('PS-DEMO-003-2026-10-1'), { timeout: timeoutMs });
  await page.locator('#payslip-finalize').click();
  await page.waitForFunction(() => document.getElementById('payroll-history-rows').textContent.includes('PS-DEMO-003-2026-10-1'), { timeout: timeoutMs });
  await page.locator('#payroll-cutoff-form button[type="submit"]').click();
  await page.reload();
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await page.locator('[data-view="payroll"]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('#correction-payslip option')].some(option => option.value === 'PS-DEMO-003-2026-10-1'), { timeout: timeoutMs });
  await page.locator('#correction-payslip').selectOption('PS-DEMO-003-2026-10-1');
  await page.locator('#correction-reason-text').fill('Wrong hours entered');
  await page.locator('#payslip-correction-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('payslip-correction-status').textContent.includes('void PS-DEMO-003-2026-10-1'), { timeout: timeoutMs });
  await page.locator('#correction-payslip').selectOption('PS-DEMO-004-2026-10-1');
  await page.locator('#correction-reason-text').fill('Rest-day premium missing');
  await page.locator('#correction-target-month').fill('2026-12');
  await page.locator('#payslip-correction-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('payslip-correction-status').textContent.includes('was released'), { timeout: timeoutMs });
  assert.equal(await page.locator('#payslip-correction-rows button').count(), 0, 'payroll staff do not approve their own requests');
  await signInAs('Lia Torres');
  await page.locator('[data-view="payroll"]').click();
  await page.waitForFunction(() => document.querySelectorAll('#payslip-correction-rows button').length === 4, { timeout: timeoutMs });
  await page.getByRole('button', { name: /^Approve PCR-/ }).first().click();
  await page.waitForFunction(() => document.querySelectorAll('#payslip-correction-rows button').length === 2, { timeout: timeoutMs });
  await page.getByRole('button', { name: /^Approve PCR-/ }).first().click();
  await page.waitForFunction(() => document.querySelectorAll('#payslip-correction-rows button').length === 0, { timeout: timeoutMs });
  const correctionText = await page.locator('#payslip-correction-rows').innerText();
  assert.match(correctionText, /Voided as PS-DEMO-003-2026-10-1-void1/);
  assert.match(correctionText, /Underpaid by ₱357\.14 gross: added as a taxable Adjustment on 2026-12 cutoff 1/);
  await page.waitForFunction(() => document.getElementById('payroll-history-rows').textContent.includes('PS-DEMO-003-2026-10-1-void1 · VOID'), { timeout: timeoutMs });
  console.log('PASS: payslip corrections: an unreleased payslip is voided and a released one is recalculated (underpaid 357.14 added later), both approved by the System Admin.');

  // Requirements 6-10 quick wins: roster contact and availability, HR statistics, a team incentive, the payroll summary.
  await signInAs('Mara Rivera');
  await page.locator('[data-view="team"]').click();
  await page.waitForFunction(() => document.getElementById('team-rows').textContent.includes('jae.cruz@motorph.example'), { timeout: timeoutMs });
  assert.match(await page.locator('#team-rows tr', { hasText: 'DEMO-004' }).innerText(), /local 004\s+(Available|On leave)/);
  await page.locator('[data-view="hr"]').click();
  await page.waitForFunction(() => document.getElementById('hr-stats').textContent.includes('BY BRANCH'), { timeout: timeoutMs });
  assert.match(await page.locator('#hr-stats').innerText(), /HEADCOUNT\s+\d+ current[\s\S]*BY BRANCH\s+.*Pasig/);
  await signInAs('Eli Santos');
  await page.locator('[data-view="payroll"]').click();
  await page.waitForFunction(() => [...document.querySelectorAll('#adj-target option')].some(option => option.value === 'TEAM:DEMO-003'), { timeout: timeoutMs });
  await page.locator('#adj-target').selectOption('TEAM:DEMO-003');
  await page.locator('#adj-earning-type').selectOption('Incentive');
  await page.locator('#adj-earning-month').fill('2026-12');
  await page.locator('#adj-earning-cutoff').selectOption('2');
  await page.locator('#adj-earning-amount').fill('250');
  await page.locator('#adj-earning-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('adj-status').textContent.startsWith('Added to'), { timeout: timeoutMs });
  assert.match(await page.locator('#adj-status').innerText(), /in Team of Noa Reyes\. Skipped: DEMO-002 \(you cannot change your own pay\)/);
  await page.locator('#summary-month').fill('2026-10');
  await page.locator('#summary-by').selectOption('branch');
  await page.locator('#summary-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('summary-rows').textContent.includes('TOTAL'), { timeout: timeoutMs });
  const [summaryCsv] = await Promise.all([page.waitForEvent('download'), page.locator('#summary-csv').click()]);
  assert.equal(summaryCsv.suggestedFilename(), 'motorph-payroll-summary-2026-10-branch.csv');
  const [summaryPdf] = await Promise.all([page.waitForEvent('download'), page.locator('#summary-pdf').click()]);
  assert.equal(summaryPdf.suggestedFilename(), 'motorph-payroll-summary-2026-10-branch.pdf');
  assert.match(await page.locator('#summary-rows').innerText(), /Pasig/);
  await signInAs('Lia Torres');
  console.log('PASS: roster contact and availability, HR employee statistics, a team incentive (filer skipped), and the payroll summary by branch.');

  // Branch page, own password, and the System Admin's security, payroll policy, audit and backup panels.
  await signInAs('Jae Cruz');
  await page.locator('[data-view="overview"]').click();
  await page.waitForFunction(() => document.getElementById('branch-title').textContent === 'Pasig branch', { timeout: timeoutMs });
  assert.match(await page.locator('#operator-label').innerText(), /Pasig branch/);
  assert.equal(await page.locator('#account-password-label').innerText(), 'USING THE DEMO PASSWORD');
  await page.locator('#password-current').fill(demoPassword);
  await page.locator('#password-next').fill('jae-e2e-password-9');
  await page.locator('#change-password-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('change-password-status').textContent.startsWith('Password changed'), { timeout: timeoutMs });
  await page.locator('#switch-account-button').click();
  await page.getByRole('button', { name: /^Sign in as Jae Cruz / }).click();
  await enterPassword();
  await page.waitForFunction(() => document.getElementById('account-error').textContent.startsWith('Wrong password'), { timeout: timeoutMs });
  await enterPassword('jae-e2e-password-9');
  await enterTwoFactorCode();
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="config"]').click();
  await page.waitForFunction(() => document.getElementById('security-rows').textContent.includes('Own password since'), { timeout: timeoutMs });
  await page.locator('#security-rows tr', { hasText: 'DEMO-004' }).getByRole('button', { name: 'Reset password DEMO-004' }).click();
  await page.waitForFunction(() => document.getElementById('security-status').textContent.includes('back on the demo password'), { timeout: timeoutMs });
  assert.match(await page.locator('#policy-status').innerText(), /mentor’s rules/);
  await page.locator('#audit-actor').fill('DEMO-004');
  await page.locator('#audit-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('audit-rows').textContent.includes('/api/auth/password'), { timeout: timeoutMs });
  assert.match(await page.locator('#audit-rows').innerText(), /POST \/api\/admin\/security\/DEMO-004\/reset-password/);
  assert.match(await page.locator('#backup-rows').innerText(), /memory only/, 'the browser test runs without a data file');
  console.log('PASS: branch page, changing your own password, and the admin resets it, keeps the mentor policy, reads the audit log and sees backups.');
  await page.locator('[data-view="requests"]').click();

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.locator('#leave-export-button').click()
  ]);
  assert.match(download.suggestedFilename(), /\.csv$/);
  // Personal details with a photo (HR desk -> People), then a System Admin delete of a record with no history.
  await signInAs('Mara Rivera');
  await page.locator('[data-view="hr"]').click();
  await page.locator('#personal-employee').selectOption('DEMO-004');
  await page.waitForFunction(() => document.getElementById('personal-first').value === 'Jae', { timeout: timeoutMs });
  assert.equal(await page.locator('#personal-surname').inputValue(), 'Cruz', 'older records split the name at the last space');
  await page.locator('#personal-gender').selectOption('Male');
  await page.locator('#personal-email').fill('jae.personal@example.com');
  await page.locator('#personal-street').fill('Sample Street');
  await page.locator('#personal-city').fill('Pasig');
  await page.locator('#personal-region').fill('NCR');
  await page.locator('#personal-postal').fill('1600');
  await page.locator('#personal-contact').fill('09170000004');
  await page.locator('#personal-birthdate').fill('1998-05-14');
  await page.locator('#personal-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('personal-status').textContent.startsWith('Saved'), { timeout: timeoutMs });
  const onePixelPng = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYPj/HwADAQH/AoHa5wAAAABJRU5ErkJggg==', 'base64');
  await page.locator('#personal-photo-file').setInputFiles({ name: 'jae.png', mimeType: 'image/png', buffer: onePixelPng });
  await page.locator('#personal-photo-upload').click();
  await page.waitForFunction(() => document.getElementById('personal-photo-status').textContent === 'Photo saved.' && !document.getElementById('personal-photo').hidden, { timeout: timeoutMs });
  await signInAs('Lia Torres');
  await page.locator('[data-view="hr"]').click();
  const accept = dialog => dialog.accept(dialog.type() === 'prompt' ? 'DELETE' : undefined);
  page.on('dialog', accept);
  await page.locator('#hr-rows tr', { hasText: 'DEMO-004' }).getByRole('button', { name: /^Delete the record of/ }).click();
  await page.waitForFunction(() => document.getElementById('employee-form-status').textContent.includes('offboard them instead'), { timeout: timeoutMs });
  // A record created by mistake (no attendance, pay or requests) can be deleted.
  await page.locator('#employee-edit-name').fill('Created By Mistake');
  await page.locator('#employee-edit-department').fill('Operations');
  await page.locator('#employee-edit-role').fill('Operations Associate');
  await page.locator('#employee-edit-location').fill('Pasig');
  await page.locator('#employee-save-button').click();
  await page.waitForFunction(() => document.getElementById('hr-rows').textContent.includes('Created By Mistake'), { timeout: timeoutMs });
  await page.locator('#hr-rows tr', { hasText: 'Created By Mistake' }).getByRole('button', { name: /^Delete the record of/ }).click();
  await page.waitForFunction(() => document.getElementById('employee-form-status').textContent.startsWith('Deleted Created By Mistake'), { timeout: timeoutMs });
  assert.equal(await page.locator('#hr-rows tr', { hasText: 'Created By Mistake' }).count(), 0, 'the deleted record is gone');
  page.off('dialog', accept);
  console.log('PASS: personal details (names, gender, email, address parts, contact) with a photo; the System Admin deletes only a record with no history.');

  // Section tabs: one section at a time, only the tabs the role can use, long explanations folded away.
  await page.locator('[data-view="payroll"]').click();
  await page.locator('#payroll-view').getByRole('button', { name: 'Show one section at a time' }).click();
  assert.deepEqual(await page.locator('#payroll-view [role="tab"]:not([hidden]) span:first-child').allInnerTexts(),
    ['Payslips', 'Earnings & deductions', 'Adjustment forms', 'Release & emails', 'Corrections', 'Overtime', 'Reports']);
  await page.locator('#payroll-view').getByRole('tab', { name: /^Payslips/ }).click();
  assert.equal(await page.locator('#outbox-panel').isVisible(), false, 'other tabs are hidden');
  await page.locator('#payroll-view').getByRole('tab', { name: /^Release & emails/ }).click();
  assert.equal(await page.locator('#outbox-panel').isVisible(), true, 'the chosen tab shows');
  assert.equal(await page.locator('#payroll-cutoff-form').isVisible(), false);
  assert.equal(await page.locator('#disbursement-form').isVisible(), true, 'a tab can show part of a larger panel');
  await page.keyboard.press('ArrowRight');
  assert.equal(await page.locator('#payroll-view [role="tab"][aria-selected="true"] span:first-child').innerText(), 'Corrections', 'arrow keys move between tabs');
  assert.ok(await page.locator('#payroll-view details.intro-more').count() > 0, 'long explanations sit behind "How this works"');
  await page.locator('[data-view="config"]').click();
  assert.equal(await page.locator('#config-view [role="tab"]:not([hidden])').count(), 6, 'the System Admin sees all six config tabs');
  await page.locator('#config-view').getByRole('button', { name: 'Show all sections' }).click();
  console.log('PASS: section tabs: one section at a time, role-filtered tabs, keyboard arrows, folded explanations, and "Show all sections".');

  // Reset demo data (System Admin): back to the built-in accounts; the page reloads at the account chooser.
  await page.locator('[data-view="config"]').click();
  await page.locator('#reset-confirm').fill('reset');
  await page.locator('#reset-form button[type="submit"]').click();
  await page.waitForFunction(() => document.getElementById('reset-status').textContent.includes('Type RESET'), { timeout: timeoutMs });
  await page.locator('#reset-confirm').fill('RESET');
  await page.locator('#reset-form button[type="submit"]').click();
  await page.waitForSelector('#account-gate:not([hidden])', { timeout: timeoutMs });
  assert.equal(await page.locator('.account-option').count(), 5, 'reset leaves the five built-in accounts');
  await signInAs('Lia Torres');
  await page.waitForFunction(() => document.querySelectorAll('#employee-rows tr').length === 5, { timeout: timeoutMs });
  await page.locator('[data-view="requests"]').click();
  await page.waitForFunction(() => document.getElementById('leave-request-rows').textContent.includes('No session requests yet'), { timeout: timeoutMs });
  console.log('PASS: the System Admin resets the demo data to the built-in starting point.');
  await page.setViewportSize({ width: 390, height: 844 });
  const widths = await page.evaluate(() => ({ viewport: innerWidth, document: document.documentElement.scrollWidth,
    // Names what overflows (outside scrollable table wrappers) so a failure is easy to fix.
    wide: [...document.querySelectorAll('body *')].filter(el => el.getBoundingClientRect().right > innerWidth + 1
      && el.getBoundingClientRect().width > 0 && !el.parentElement?.closest('.table-wrap, .calendar-table-wrap'))
      .slice(0, 5).map(el => `${el.tagName}.${el.className}#${el.id}`) }));
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
