/**
 * End-to-end check against the real server build.
 *
 * The previous e2e compiled an app that no longer exists (`src/main/java/ph/motorph/web`) and
 * drove a Firebase-authenticated browser. This one starts the shipped server on a spare port with
 * its own throwaway data file, signs in through the UI with an employee number and a 2FA code, and
 * asserts the things that have actually broken during development:
 *
 *   - the 2FA step is required and a wrong code grants nothing
 *   - every field a real role can reach loads without a 4xx
 *   - the System Administrator can read attendance (the matrix drift)
 *   - an employee cannot read another employee's record
 *   - a write survives a server restart (persistence is real, not per-request)
 *
 * Run: node test/e2e/local-workflows.e2e.js
 * Needs: target/classes built (sh tools/build.sh), playwright-core + a Chromium binary.
 */
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { copyFileSync, existsSync, mkdtempSync, rmSync } from 'node:fs';
import net from 'node:net';
import { tmpdir } from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const classes = path.join(projectRoot, 'target/classes');
const gsonJar = path.join(projectRoot, 'lib/gson-2.13.2.jar');
const bouncyJar = path.join(projectRoot, 'lib/bcprov-jdk18on-1.78.1.jar');
const chromiumPath = process.env.CHROMIUM_PATH
  || '/home/user/.cache/ms-playwright/chromium-1248/chrome-linux64/chrome';
const password = 'MotorPH-demo-2026';
const timeoutMs = 30_000;

let scratch;
let browser;
let child = null;
let failure = null;
let newEmployeeId;
let onboardedId;
let backupName;

function freePort() {
  return new Promise((resolve, reject) => {
    const probe = net.createServer();
    probe.once('error', reject);
    probe.listen(0, '127.0.0.1', () => {
      const { port } = probe.address();
      probe.close(() => resolve(port));
    });
  });
}

async function startServer(port) {
  child = spawn('java', [
    '-Xmx256m', '-XX:TieredStopAtLevel=1',
    '--add-modules', 'jdk.httpserver',
    '-cp', [gsonJar, bouncyJar, path.join(projectRoot, 'lib/google/*'), classes].join(path.delimiter),
    'ph.motorph.svc.AppServer'
  ], {
    cwd: projectRoot,
    env: {
      ...process.env,
      PORT: String(port),
      HOST: '127.0.0.1',
      WEB_ROOT: process.env.MOTORPH_E2E_WEB_ROOT || path.join(projectRoot, 'web'),
      MOTORPH_PEPPER: 'e2e-pepper-at-least-32-characters-long',
      LOCAL_DATA_FILE: path.join(scratch, 'state.json'),
      MOTORPH_SEED_FILE: path.join(scratch, 'state.json'),
      FIREBASE_PROJECT_ID: process.env.MOTORPH_E2E_FIRESTORE_PROJECT || '',
      MOTORPH_DEFER_SAVES: 'false'
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  let serverLog = '';
  const capture = chunk => { serverLog = (serverLog + chunk.toString()).slice(-8000); };
  child.stdout.on('data', capture);
  child.stderr.on('data', capture);
  // A fresh data file is seeded and saved in full before the port opens, and provisioning the
  // 34 demo accounts costs one Argon2id hash each (about 9-10 s on two cores). Allow a generous
  // window so a loaded machine does not turn a slow start into a test failure.
  for (let i = 0; i < 450; i++) {
    if (child.exitCode !== null || child.signalCode !== null) {
      throw new Error(`server exited before startup (code ${child.exitCode}, signal ${child.signalCode}):\n${serverLog}`);
    }
    try {
      const response = await fetch(`http://127.0.0.1:${port}/api/health`);
      if (response.ok) return;
      if (i % 25 === 0) console.log(`  waiting for the server (${response.status})...`);
    } catch {
      if (i % 25 === 0) console.log('  waiting for the server...');
    }
    await new Promise(r => setTimeout(r, 200));
  }
  throw new Error(`server did not start within 90 seconds:\n${serverLog}`);
}

async function stopServer() {
  if (!child) return;
  const done = new Promise(resolve => child.once('exit', resolve));
  child.kill('SIGTERM');
  await Promise.race([done, new Promise(r => setTimeout(r, 5000))]);
  if (child.exitCode === null) child.kill('SIGKILL');
  child = null;
}

/** Sign in through the UI and return the page, already on the workspace. */
async function signIn(page, employeeId, { expectWrongCodeFirst = false } = {}) {
  await page.goto(`http://127.0.0.1:${page.__port}/signin`, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(800);
  await page.fill('#auth-employee', employeeId);
  await page.fill('#auth-password', password);
  await page.click('#sign-in-button');
  await page.waitForFunction(() => !document.getElementById('code-form').hidden, null, { timeout: timeoutMs })
    .catch(async error => { throw new Error(`sign-in ${employeeId} failed: ${await page.textContent('#auth-error')} (${error.message})`); });

  if (expectWrongCodeFirst) {
    await page.fill('#code-input', '000000');
    await page.click('#code-verify-button');
    await page.waitForFunction(
      () => !document.getElementById('auth-error').hidden,
      null,
      { timeout: timeoutMs }
    ).catch(() => {});
    const error = (await page.textContent('#auth-error')) || '';
    assert.match(error, /wrong code|tries/i, `a wrong 2FA code is refused (got: ${error})`);
    assert.equal(await page.isVisible('.nav-item'), false, 'a wrong code still shows no workspace');
  }

  const preview = await page.textContent('#code-preview');
  const code = (preview.match(/\d{6}/) || [])[0];
  assert.ok(code, 'the demo deployment shows the code under the input');
  await page.fill('#code-input', code);
  await page.click('#code-verify-button');
  await page.waitForFunction(() => !document.getElementById('app-shell').hidden, null, { timeout: timeoutMs })
    .catch(async error => { throw new Error(`verification ${employeeId} failed: ${await page.textContent('#auth-error')} (${error.message})`); });
  assert.equal(await page.locator('#first-password-form').count(), 0, 'first sign-in does not require a password change');
  const workspaceStatus = await page.evaluate(async () => (await fetch('/api/employees')).status);
  assert.equal(workspaceStatus, 200, 'the default password opens the workspace after the code step');
  await page.waitForSelector('.nav-item:visible', { timeout: timeoutMs })
    .catch(() => {});
  await page.waitForTimeout(1800);
  assert.ok(await page.isVisible('.nav-item'), `employee ${employeeId} reaches the workspace`);
}

async function main() {
  if (!existsSync(classes)) throw new Error('target/classes missing; run sh tools/build.sh first');
  scratch = mkdtempSync(path.join(tmpdir(), 'motorph-e2e-'));
  // Start from the real dataset so counts in this test mean something.
  copyFileSync(path.join(projectRoot, 'data/local-demo.json'), path.join(scratch, 'state.json'));

  const port = await freePort();
  await startServer(port);

  for (const endpoint of ['/api/admin/roles', '/api/employees', '/api/attendance?employeeId=10015&from=2026-01-01&to=2026-01-31']) {
    const anonymous = await fetch(`http://127.0.0.1:${port}${endpoint}`);
    assert.equal(anonymous.status, 401, `${endpoint} rejects anonymous access`);
    const forged = await fetch(`http://127.0.0.1:${port}${endpoint}`, { headers: { 'X-Demo-User': '10005' } });
    assert.equal(forged.status, 401, `${endpoint} rejects a forged identity header`);
  }
  console.log('PASS: protected APIs reject anonymous and forged-header requests');

  browser = await chromium.launch({
    executablePath: chromiumPath,
    args: ['--no-sandbox', '--disable-dev-shm-usage']
  });

  // ---- 1. 2FA is a real gate -------------------------------------------------
  console.log('1/5 two-factor gate');
  {
    const page = await browser.newPage();
    page.__port = port;
    await signIn(page, '10006', { expectWrongCodeFirst: true });
    console.log('PASS: sign-in needs the second factor; a wrong code opens nothing');
    await page.close();
  }

  // ---- 2. every reachable view loads without a 4xx ---------------------------
  console.log('2/5 view sweep');
  {
    const page = await browser.newPage();
    page.__port = port;
    const bad = [];
    const pageErrors = [];
    const externalCompareRequests = [];
    page.on('pageerror', e => pageErrors.push(e.message));
    await signIn(page, '10005');   // System Admin reaches the most views
    assert.equal(await page.isVisible('#workspace-admin-view'), true, 'System Admin lands on the dedicated system-control workspace');
    assert.equal(await page.isVisible('#workspace-hr-view'), false, 'HR workspace is not mixed into the System Admin home');
    assert.equal(await page.locator('#workspace-admin-view h1').innerText(), 'System control desk');
    await page.waitForFunction(() => document.querySelector('#admin-home-api-state')?.textContent.trim() === 'Online', null, { timeout: timeoutMs });
    assert.ok(Number(await page.locator('#admin-home-active-count').innerText()) > 0, 'admin home reads the live enabled-account count');
    assert.match(await page.locator('#admin-home-storage-status').innerText(), /durable|not durable/, 'admin home reports configured storage readiness');
    await page.click('#workspace-admin-view [data-workspace-tab="roles"]');
    assert.equal(await page.isVisible('#config-view'), true, 'admin shortcut opens System configuration');
    assert.equal(await page.isVisible('#config-view [data-tab="roles"]'), true, 'admin shortcut opens the roles-and-teams section');
    await page.click('.nav-item[data-view="overview"]');
    assert.equal(await page.isVisible('#workspace-admin-view'), true, 'the workspace navigation returns to the System control desk');
    const workspaceCases = [
      ['10018', 'employee', 'My workspace'],
      ['10015', 'supervisor', 'Team workspace'],
      ['10011', 'payroll', 'Payroll operations'],
      ['10006', 'hr', 'People operations']
    ];
    for (const [employeeId, role, heading] of workspaceCases) {
      const rolePage = await browser.newPage();
      rolePage.__port = port;
      await signIn(rolePage, employeeId);
      assert.equal(await rolePage.isVisible(`#workspace-${role}-view`), true, `${role} lands on its own workspace`);
      assert.equal(await rolePage.locator(`#workspace-${role}-view h1`).innerText(), heading, `${role} gets a role-specific workspace heading`);
      assert.equal(await rolePage.isVisible('#workspace-admin-view'), false, `${role} cannot see the System control desk`);
      if (role === 'supervisor') {
        await rolePage.waitForFunction(() => Number(document.querySelector('#supervisor-home-team-total')?.textContent) > 0, null, { timeout: timeoutMs });
        assert.ok(await rolePage.locator('#supervisor-home-team-rows tr').count() > 0, 'supervisor home shows the scoped team roster');
      }
      if (role === 'payroll') {
        assert.equal(await rolePage.locator('.payroll-workflow-steps li').count(), 4, 'payroll home shows the four actual processing steps');
        assert.equal(await rolePage.isVisible('#workspace-payroll-view [data-workspace-tab="release"]'), true, 'payroll home links to release controls');
      }
      if (role === 'hr') {
        await rolePage.waitForFunction(() => Number(document.querySelector('#hr-home-employee-count')?.textContent) > 0, null, { timeout: timeoutMs });
        const workforce = await rolePage.evaluate(() => ({
          total: Number(document.querySelector('#hr-home-employee-count').textContent),
          regular: Number(document.querySelector('#hr-home-regular-count').textContent),
          probationary: Number(document.querySelector('#hr-home-probationary-count').textContent)
        }));
        assert.ok(workforce.regular + workforce.probationary > 0 && workforce.regular + workforce.probationary <= workforce.total,
          'HR home summaries reflect the loaded employee-file statuses');
      }
      await rolePage.click('#sign-out-button');
      await rolePage.waitForURL(`http://127.0.0.1:${port}/`);
      await rolePage.close();
    }
    page.on('request', request => {
      if (request.url().includes('/api/admin/sweldong/compare')
          || request.url().includes('/api/admin/sweldong/decision')
          || request.url().includes('sweldongpinoy.com')) externalCompareRequests.push(request.url());
    });
    const storage = await page.evaluate(async () => {
      const stateResponse = await fetch('/api/admin/storage');
      const state = await stateResponse.json();
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const checkResponse = await fetch('/api/admin/storage/verify', { method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, body: JSON.stringify({ confirm: 'VERIFY' }) });
      const check = await checkResponse.json();
      const after = await (await fetch('/api/admin/storage')).json();
      return { stateStatus: stateResponse.status, state, checkStatus: checkResponse.status, check, after };
    });
    const expectedBackend = process.env.MOTORPH_E2E_FIRESTORE_PROJECT ? 'firestore' : 'file';
    assert.equal(storage.stateStatus, 200, 'admin reads storage diagnostics');
    assert.equal(storage.state.backend, expectedBackend, 'diagnostics report the configured backend');
    assert.equal(storage.state.employeeRecords, 34, 'loaded employee count is reported');
    assert.equal(storage.state.attendanceRecords, 7421, 'loaded attendance count is reported');
    assert.equal(storage.checkStatus, 200, `storage read/write/read succeeds: ${JSON.stringify(storage.check)}`);
    assert.equal(storage.check.stateMatchedBeforeWrite, true, 'live saved snapshot matches running memory before the probe');
    assert.equal(storage.check.writeCommitted, true, 'the configured store committed the unchanged snapshot');
    assert.equal(storage.check.readBackMatched, true, 'the store returns the same snapshot after commit');
    assert.equal(storage.after.lastVerification.readBackMatched, true, 'successful check is recorded for this server process');
    page.on('response', r => {
      const expectedInactiveSignIn = r.status() === 401 && r.url().endsWith('/api/auth/signin');
      if (r.status() >= 400 && !r.url().includes('/api/auth/session') && !expectedInactiveSignIn) {
        bad.push(`${r.status()} ${r.request().method()} ${r.url().replace(`http://127.0.0.1:${port}`, '')}`);
      }
    });
    const views = ['overview', 'employees', 'attendance', 'requests', 'team', 'hr', 'reports', 'payroll', 'config'];
    for (const view of views) {
      const item = await page.$(`.nav-item[data-view="${view}"]`);
      if (!item || !(await item.isVisible())) continue;
      await item.click();
      await page.waitForTimeout(1100);
      const text = await page.evaluate(() => {
        const shown = [...document.querySelectorAll('.page-view, section.view')].filter(v => v.offsetParent !== null);
        return (shown[0] || document.body).innerText.trim();
      });
      assert.ok(text.length > 200, `the ${view} view renders content`);
    }
    await page.click('.nav-item[data-view="payroll"]');
    const externalTab = page.locator('#payroll-view [data-tab-key="sweldong"]');
    assert.equal(await externalTab.isVisible(), true, 'System Admin can open the optional comparison tab');
    await externalTab.click();
    assert.equal(await page.isVisible('#sweldong-reference-panel'), true, 'comparison panel is restricted to its admin payroll tab');
    assert.equal(await page.locator('#sweldong-consent').isChecked(), false, 'external transfer consent starts unchecked');
    assert.equal(await page.locator('#sweldong-submit').isDisabled(), true, 'comparison submit starts disabled');
    assert.equal(await page.isVisible('#sweldong-results'), false, 'no comparison result is automatically selected or fetched');
    assert.match(await page.locator('#sweldong-status').innerText(), /No external request has been made/);
    await page.waitForTimeout(300);
    assert.deepEqual(externalCompareRequests, [], 'opening the admin panel sends no request to the external site or compare API');
    assert.deepEqual(bad, [], 'no view issues a request the server refuses');
    assert.deepEqual(pageErrors, [], 'no uncaught page errors while walking the app');
    console.log('PASS: all nine admin views render with no refused requests');
    await page.close();
  }

  // ---- 3. the System Administrator can read attendance ----------------------
  console.log('3/5 admin attendance access');
  {
    const page = await browser.newPage();
    page.__port = port;
    await signIn(page, '10005');
    const rows = await page.evaluate(async () => {
      const response = await fetch('/api/attendance?employeeId=10001', { headers: { Accept: 'application/json' } });
      return { status: response.status, body: await response.json().catch(() => ({})) };
    });
    assert.equal(rows.status, 200, 'the admin reads any attendance record (design matrix C,R,U,D)');
    assert.ok(Array.isArray(rows.body.items) && rows.body.items.length > 0, 'the admin attendance read returns rows');
    const security = await page.evaluate(async () => {
      const state = await (await fetch('/api/admin/security')).json();
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const refused = await fetch('/api/admin/security', { method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, body: JSON.stringify({
          minPasswordLength: 8, maxFailedAttempts: 3, lockoutMinutes: 5, codeMinutes: 1, sessionHours: 24 }) });
      return { state, status: refused.status };
    });
    assert.equal(security.state.settings.minPasswordLength, 12, 'security panel reports the real password limit');
    assert.equal(security.state.settings.maxFailedAttempts, 5, 'security panel reports the real lockout threshold');
    assert.equal(security.state.settings.editable, false, 'unsupported settings are clearly read-only');
    assert.equal(security.status, 409, 'server rejects ineffective security-setting changes');
    assert.equal(security.state.accounts.find(row => row.employeeId === '10005').defaultPassword, true, 'security panel reports the shared default password');

    await page.click('.nav-item[data-view="config"]');
    await page.click('#config-view [data-tab-key="security"]');
    await page.waitForFunction(() => document.querySelectorAll('#security-rows tr').length >= 34, null, { timeout: timeoutMs });
    const accessRow = page.locator('#security-rows tr').filter({ hasText: '10032' });
    await accessRow.getByRole('button', { name: 'Deactivate 10032' }).click();
    await page.waitForFunction(() => {
      const row = [...document.querySelectorAll('#security-rows tr')].find(item => item.innerText.includes('10032'));
      return row?.innerText.includes('Deactivated') && row.querySelector('button[data-action="activate"]');
    }, null, { timeout: timeoutMs });
    const inactiveSignIn = await page.evaluate(async () => {
      const response = await fetch('/api/auth/signin', { method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ employeeId: '10032', password: 'MotorPH-demo-2026' }) });
      return { status: response.status, body: await response.json() };
    });
    assert.equal(inactiveSignIn.status, 401, 'a deactivated account cannot sign in');
    assert.match(inactiveSignIn.body.error, /deactivated/i);
    await accessRow.getByRole('button', { name: 'Activate 10032' }).click();
    await page.waitForFunction(() => {
      const row = [...document.querySelectorAll('#security-rows tr')].find(item => item.innerText.includes('10032'));
      return row?.innerText.includes('Active') && row.querySelector('button[data-action="deactivate"]');
    }, null, { timeout: timeoutMs });
    const attacker = await browser.newPage();
    await attacker.goto(`http://127.0.0.1:${port}/signin`);
    await attacker.evaluate(async () => {
      for (let n = 0; n < 5; n++) await fetch('/api/auth/signin', { method: 'POST',
        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ employeeId: '10032', password: 'incorrect' }) });
    });
    await attacker.close();
    const locked = await page.evaluate(async () => (await (await fetch('/api/admin/security')).json()).accounts.find(row => row.employeeId === '10032'));
    assert.ok(locked.lockedUntil, 'security panel reflects a real lockout');
    const unlockStatus = await page.evaluate(async () => {
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      return (await fetch('/api/admin/security/10032/unlock', { method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, body: '{}' })).status;
    });
    assert.equal(unlockStatus, 200, 'admin can unlock the actual credential');
    const unlocked = await page.evaluate(async () => (await (await fetch('/api/admin/security')).json()).accounts.find(row => row.employeeId === '10032'));
    assert.equal(unlocked.lockedUntil, '', 'unlock removes the real lock');
    const backup = await page.evaluate(async () => {
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const saved = await fetch('/api/admin/backups', { method: 'POST', headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf }, body: '{}' });
      const payload = await saved.json();
      const listed = await (await fetch('/api/admin/backups')).json();
      return { status: saved.status, name: payload.backup, listed: listed.backups || [] };
    });
    assert.equal(backup.status, 201, 'an administrator can create a backup');
    assert.ok(backup.listed.includes(backup.name), 'the created backup is listed');
    backupName = backup.name;
    const identity = await page.evaluate(async () => {
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const headers = { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf };
      const role = await fetch('/api/admin/roles/10032', { method: 'POST', headers,
        body: JSON.stringify({ role: 'SUPERVISOR', supervisorId: '10015' }) });
      const created = await fetch('/api/employees', { method: 'POST', headers,
        body: JSON.stringify({ name: 'E2E New Employee', department: 'Operations', role: 'Staff', location: 'Quezon City', status: 'Active' }) });
      return { roleStatus: role.status, roleBody: await role.text(), createStatus: created.status, created: await created.json() };
    });
    assert.equal(identity.roleStatus, 200, `role change accepted: ${identity.roleBody}`);
    assert.equal(identity.createStatus, 201, 'new employee created');
    newEmployeeId = identity.created.item.id || identity.created.item.employeeId;
    assert.match(newEmployeeId || '', /^\d{5}$/, 'new employee receives a valid account number');

    console.log(`PASS: System Admin reads attendance (${rows.body.items.length} rows) and creates a backup`);
    await page.click('#sign-out-button');
    await page.waitForURL(`http://127.0.0.1:${port}/`);
    assert.equal(await page.evaluate(async () => (await fetch('/api/auth/session')).status), 401, 'logout invalidates the server session');
    await page.close();
    const hr = await browser.newPage();
    hr.__port = port;
    await signIn(hr, '10006');
    const onboarding = await hr.evaluate(async () => {
      const catalog = await (await fetch('/api/hr/catalog')).json();
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const response = await fetch('/api/hr/onboard', { method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf },
        body: JSON.stringify({ name: 'Onboarded Test Employee', department: catalog.departments[0],
          position: catalog.positions[0], location: 'Quezon City', startDate: '2026-01-01',
          supervisorId: '10015', monthlyBasic: '20000', payMethod: 'Direct deposit' }) });
      return { status: response.status, body: await response.json() };
    });
    assert.equal(onboarding.status, 201, `HR onboarding succeeds: ${JSON.stringify(onboarding.body)}`);
    onboardedId = onboarding.body.employee.id;

    await hr.close();
  }

  // ---- 4. an employee cannot read another employee --------------------------
  console.log('4/5 cross-employee refusals');
  {
    const page = await browser.newPage();
    page.__port = port;
    await signIn(page, '10018');
    assert.equal(await page.isVisible('#workspace-employee-view'), true, 'employee sees the employee self-service workspace');
    assert.equal(await page.isVisible('#workspace-admin-view'), false, 'employee does not see the System control desk');
    const probe = await page.evaluate(async () => {
      const call = async url => (await fetch(url, { headers: { Accept: 'application/json' } })).status;
      return {
        otherRecord: await call('/api/employees/10001'),
        otherAttendance: await call('/api/attendance?employeeId=10001'),
        otherGovernment: await call('/api/government/10001'),
        ownPayslip: await call('/api/payroll/payslip?employeeId=10018&month=2026-01&cutoff=1'),
        otherComparison: await call('/api/payroll/cutoffs?employeeId=10001&month=2026-01'),
        adminStorage: await call('/api/admin/storage')
      };
    });
    assert.equal(probe.otherRecord, 403, "an employee cannot open another employee's record");
    assert.equal(probe.otherAttendance, 403, "an employee cannot read another employee's attendance");
    assert.equal(probe.otherGovernment, 403, "an employee cannot read another employee's government details");
    assert.equal(probe.ownPayslip, 200, 'an employee can open their own payslip');
    assert.equal(probe.otherComparison, 403, 'an employee cannot compare another employee’s pay');
    assert.equal(probe.adminStorage, 403, 'storage diagnostics are admin-only');
    const payrollBefore = await page.evaluate(async () => (await (await fetch('/api/payroll/cutoffs?employeeId=10018&month=2026-01')).json()));
    await page.click('.nav-item[data-view=payroll]');
    assert.equal(await page.locator('#payroll-view [data-tab-key="sweldong"]').isVisible(), false,
      'the optional external comparison tab is unavailable to employees');
    assert.equal(await page.isVisible('#sweldong-reference-panel'), false,
      'employees cannot open the admin-only comparison panel');
    await page.selectOption('#payroll-cutoff-employee', '10018');
    await page.fill('#payroll-cutoff-month', '2026-01');
    await page.click('#payroll-cutoff-form button[type=submit]');
    await page.waitForFunction(() => document.querySelectorAll('#pay-comparison-rows tr').length === 8, null, { timeout: timeoutMs });
    await page.click('[data-pay-mode=reference]');
    assert.equal(await page.isVisible('#pay-comparison'), true, 'employee can open the reference calculation');
    assert.equal(await page.isVisible('#motorph-cutoffs'), false, 'reference mode replaces the cutoff table');
    assert.match(await page.locator('#pay-comparison-rows').innerText(), /Take-home pay/, 'reference shows a net comparison');
    await page.click('[data-pay-mode=motorph]');
    assert.equal(await page.isVisible('#motorph-cutoffs'), true, 'employee can switch back to MotorPH');
    const payrollAfter = await page.evaluate(async () => (await (await fetch('/api/payroll/cutoffs?employeeId=10018&month=2026-01')).json()));
    assert.deepEqual(payrollAfter, payrollBefore, 'comparison does not change payroll values');
    console.log('PASS: employee self-service works and every cross-employee probe is refused');
    await page.close();
  }

  // ---- 5. a write survives a restart ----------------------------------------
  console.log('5/5 restart persistence');
  {
    const page = await browser.newPage();
    page.__port = port;
    await signIn(page, '10011');   // Payroll files the adjustment
    const written = await page.evaluate(async () => {
      const csrf = (document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1];
      const response = await fetch('/api/payroll/adjustments/earnings', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': decodeURIComponent(csrf || '') },
        body: JSON.stringify({
          employeeId: '10015', month: '2026-02', cutoff: '1',
          type: 'Bonus', description: 'e2e restart proof', amount: '10.00'
        })
      });
      return response.status;
    });
    assert.equal(written, 201, 'the payroll adjustment is accepted');

    await page.close();
    await stopServer();
    await startServer(port);

    const check = await browser.newPage();
    check.__port = port;
    await signIn(check, '10011');
    const found = await check.evaluate(async () => {
      const response = await fetch('/api/payroll/adjustments?month=2026-02', { headers: { Accept: 'application/json' } });
      const body = await response.json();
      return (body.earnings || []).some(item => item.description === 'e2e restart proof');
    });
    assert.ok(found, 'the adjustment is still there after a server restart');
    console.log('PASS: payroll data persists and the shared default password signs in after restart');
    await check.close();
    const admin = await browser.newPage();
    admin.__port = port;
    await signIn(admin, '10005');
    const account = await admin.evaluate(async () => {
      const data = await (await fetch('/api/admin/roles')).json();
      return data.accounts.find(row => row.employeeId === '10032');
    });
    assert.equal(account.role, 'SUPERVISOR', 'role change survives roster update and restart');
    assert.equal(account.supervisorId, '10015', 'supervisor change survives roster update and restart');
    await admin.close();
    const hire = await browser.newPage();
    hire.__port = port;
    await signIn(hire, newEmployeeId);
    assert.equal(await hire.evaluate(async () => (await fetch('/api/auth/session')).status), 200, 'new employee can sign in after restart');
    await hire.close();
    const hrHire = await browser.newPage();
    hrHire.__port = port;
    await signIn(hrHire, onboardedId);
    await hrHire.close();
    console.log('PASS: role and supervisor persist, and created and HR-onboarded accounts can sign in');
    const restore = await browser.newPage();
    restore.__port = port;
    console.log('  signing in for restore');
    await signIn(restore, '10005');
    const restored = await restore.evaluate(async name => {
      const csrf = decodeURIComponent((document.cookie.match(/motorph_csrf=([^;]+)/) || [])[1] || '');
      const response = await fetch('/api/admin/backups/restore', { method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-CSRF-Token': csrf },
        body: JSON.stringify({ name, confirm: 'RESTORE' }) });
      return { status: response.status, body: await response.text(), session: (await fetch('/api/auth/session')).status };
    }, backupName);
    assert.equal(restored.status, 200, `backup restore succeeds: ${restored.body}`);
    assert.equal(restored.session, 401, 'restore revokes existing sessions');
    console.log('  signing in after restore');
    await signIn(restore, '10005');
    const restoredAccounts = await restore.evaluate(async () => (await (await fetch('/api/admin/roles')).json()).accounts);
    assert.ok(!restoredAccounts.some(row => row.employeeId === newEmployeeId), 'restore loads the earlier roster');
    assert.notEqual(restoredAccounts.find(row => row.employeeId === '10032').role, 'SUPERVISOR', 'restore loads the earlier role');
    await restore.close();
    console.log('PASS: restoring backup restores identity state and revokes sessions');
  }

  console.log('\nlocal-workflows.e2e: 5 checks passed');
}

try {
  await main();
} catch (error) {
  failure = error;
  console.error('FAIL:', error.message);
} finally {
  try { await browser?.close(); } catch { /* already closed */ }
  try { await stopServer(); } catch { /* already stopped */ }
  if (scratch) rmSync(scratch, { recursive: true, force: true });
}
if (failure) process.exit(1);
