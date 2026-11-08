import assert from 'node:assert/strict';
import { spawn, execFileSync } from 'node:child_process';
import { mkdtempSync, readFileSync, readdirSync, rmSync } from 'node:fs';
import net from 'node:net';
import path from 'node:path';
import { tmpdir } from 'node:os';
import { fileURLToPath } from 'node:url';
import { chromium } from 'playwright-core';

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const sourceDir = path.join(projectRoot, 'src/main/java/ph/motorph/web');
const gsonJar = path.join(projectRoot, 'lib/gson-2.13.2.jar');
const timeoutMs = 30_000;
const apiKeyValues = readEnvFile(path.join(projectRoot, '.env.local'));
const config = {
  apiKey: apiKeyValues.VITE_FIREBASE_API_KEY || '',
  authDomain: apiKeyValues.VITE_FIREBASE_AUTH_DOMAIN || '',
  projectId: apiKeyValues.VITE_FIREBASE_PROJECT_ID || '',
  appId: apiKeyValues.VITE_FIREBASE_APP_ID || '',
  storageBucket: apiKeyValues.VITE_FIREBASE_STORAGE_BUCKET || '',
  messagingSenderId: apiKeyValues.VITE_FIREBASE_MESSAGING_SENDER_ID || ''
};
const javaClasses = mkdtempSync(path.join(tmpdir(), 'motorph-auth-smoke-classes-'));
const children = [];
let browser;
let createdAccount = false;
let idToken = '';
let testEmail = '';
let testPassword = '';

function readEnvFile(file) {
  try {
    return Object.fromEntries(readFileSync(file, 'utf8').split(/\r?\n/)
      .map(line => line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/))
      .filter(Boolean)
      .map(([, key, value]) => [key, value.replace(/^['"]|['"]$/g, '')]));
  } catch {
    return {};
  }
}

function freePort() {
  return new Promise((resolve, reject) => {
    const server = net.createServer();
    server.once('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const port = server.address().port;
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
  children.push(child);
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

async function waitUntil(predicate, label) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    if (predicate()) return;
    await new Promise(resolve => setTimeout(resolve, 50));
  }
  throw new Error(`Timed out waiting for ${label}.`);
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

async function deleteTestAccount() {
  if (!createdAccount) return;
  let cleanupToken = idToken;
  if (!cleanupToken && testEmail && testPassword) {
    const signInResponse = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(config.apiKey)}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: testEmail, password: testPassword, returnSecureToken: true })
    });
    if (signInResponse.ok) cleanupToken = (await signInResponse.json()).idToken || '';
  }
  if (!cleanupToken) throw new Error(`Disposable Firebase account ${testEmail} was created, but no cleanup token could be obtained.`);
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:delete?key=${encodeURIComponent(config.apiKey)}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ idToken: cleanupToken })
  });
  if (!response.ok) {
    const detail = await response.text().catch(() => '');
    throw new Error(`Could not delete disposable Firebase account ${testEmail} (HTTP ${response.status}): ${detail}`);
  }
  createdAccount = false;
  idToken = '';
  console.log('PASS: disposable Firebase Auth account deleted; no Firestore writes were made.');
}

async function main() {
  assert.ok(config.apiKey && config.projectId && config.appId, 'Firebase Web config is missing from .env.local');
  const apiPort = await freePort();
  const webPort = await freePort();
  const sources = readdirSync(sourceDir).filter(file => file.endsWith('.java')).map(file => path.join(sourceDir, file));
  execFileSync('javac', ['-encoding', 'UTF-8', '--add-modules', 'jdk.httpserver', '-cp', gsonJar, '-d', javaClasses, ...sources], {
    cwd: projectRoot,
    stdio: 'inherit'
  });

  const apiEnv = {
    ...process.env,
    PORT: String(apiPort),
    HOST: '127.0.0.1',
    WEB_ROOT: path.join(projectRoot, 'web'),
    FIREBASE_PROJECT_ID: config.projectId
  };
  const api = start('java', ['--add-modules', 'jdk.httpserver', '-cp', `${gsonJar}:${javaClasses}`, 'ph.motorph.web.AppServer'], apiEnv, 'Java Firebase API');
  await waitForServer(`http://127.0.0.1:${apiPort}/api/health`, api, 'Java Firebase API');

  const webEnv = {
    ...process.env,
    API_PROXY_TARGET: `http://127.0.0.1:${apiPort}`,
    VITE_FIREBASE_API_KEY: config.apiKey,
    VITE_FIREBASE_AUTH_DOMAIN: config.authDomain,
    VITE_FIREBASE_PROJECT_ID: config.projectId,
    VITE_FIREBASE_APP_ID: config.appId,
    VITE_FIREBASE_STORAGE_BUCKET: config.storageBucket,
    VITE_FIREBASE_MESSAGING_SENDER_ID: config.messagingSenderId
  };
  const npm = process.platform === 'win32' ? 'npm.cmd' : 'npm';
  const vite = start(npm, ['run', 'dev', '--', '--host', '127.0.0.1', '--port', String(webPort)], webEnv, 'Vite Firebase Auth test');
  const baseUrl = `http://127.0.0.1:${webPort}`;
  const health = await (await waitForServer(`${baseUrl}/api/health`, vite, 'Vite/API proxy')).json();
  assert.equal(health.mode, 'firebase-user-sandbox');

  browser = await chromium.launch({ headless: true, args: ['--no-sandbox', '--disable-dev-shm-usage'] });
  const page = await browser.newPage({ viewport: { width: 1365, height: 900 } });
  const apiResponses = {};
  page.on('response', async response => {
    const pathname = new URL(response.url()).pathname;
    if (pathname === '/api/employees' || pathname === '/api/workflows' || pathname === '/api/reports/attendance') {
      try { apiResponses[pathname] = { status: response.status(), payload: await response.json() }; } catch { /* keep status */ }
    }
  });
  await page.goto(baseUrl);
  await page.waitForSelector('#auth-gate:not([hidden])', { timeout: timeoutMs });
  const unique = `${Date.now()}-${Math.random().toString(16).slice(2, 10)}`;
  testEmail = `motorph-auth-smoke-${unique}@example.com`;
  testPassword = `Demo-${unique}-A9!`;
  await page.locator('#auth-email').fill(testEmail);
  await page.locator('#auth-password').fill(testPassword);
  const [signUpResponse] = await Promise.all([
    page.waitForResponse(response => response.url().includes('/accounts:signUp'), { timeout: timeoutMs }),
    page.locator('#create-account-button').click()
  ]);
  const signUpPayload = await signUpResponse.json();
  if (signUpResponse.status() === 200) {
    createdAccount = true;
    idToken = signUpPayload.idToken || '';
  }
  assert.equal(signUpResponse.status(), 200, `Firebase account creation failed: ${JSON.stringify(signUpPayload.error || {})}`);
  await page.waitForSelector('#app-shell:not([hidden])', { timeout: timeoutMs });
  await page.waitForFunction(() => document.getElementById('workflow-storage-mode').textContent.trim() === 'User-scoped Firestore', { timeout: timeoutMs });
  await waitUntil(() => Boolean(apiResponses['/api/employees'] && apiResponses['/api/workflows']), 'signed-in Firestore reads');
  assert.ok(idToken, 'Firebase sign-up response should provide a token for cleanup');
  assert.ok(apiResponses['/api/employees'], 'signed-in employee read should complete');
  assert.equal(apiResponses['/api/employees'].status, 200, 'signed-in employee read should be authorized');
  assert.ok(apiResponses['/api/workflows'], 'signed-in workflow read should complete');
  assert.equal(apiResponses['/api/workflows'].status, 200, 'signed-in workflow read should be authorized');
  assert.deepEqual(apiResponses['/api/workflows'].payload.leaveRequests, [], 'new account should start with no leave records');
  assert.deepEqual(apiResponses['/api/workflows'].payload.tickets, [], 'new account should start with no ticket records');
  await page.locator('[data-view="reports"]').click();
  await waitUntil(() => Boolean(apiResponses['/api/reports/attendance']), 'user-scoped raw attendance report');
  assert.equal(apiResponses['/api/reports/attendance'].status, 200, 'raw attendance report should be authorized');
  assert.equal(apiResponses['/api/reports/attendance'].payload.hoursCalculated, false);
  assert.deepEqual(apiResponses['/api/reports/attendance'].payload.items, [], 'new account should have no synthetic employee report rows');
  console.log('PASS: disposable account signed in and completed empty user-scoped reads and report.');

  await page.locator('#sign-out-button').click();
  await page.waitForSelector('#auth-gate:not([hidden])', { timeout: timeoutMs });
  assert.equal(await page.locator('#app-shell').isVisible(), false);
  console.log('PASS: Firebase sign-out returned the browser to the sign-in gate.');
}

let failure;
try {
  await main();
} catch (error) {
  failure = error;
} finally {
  if (browser) await browser.close().catch(() => {});
  for (const child of children.reverse()) await stop(child);
  try {
    await deleteTestAccount();
  } catch (cleanupError) {
    failure = failure ? new AggregateError([failure, cleanupError], 'Auth lifecycle test failed and disposable-account cleanup also failed.') : cleanupError;
  }
  rmSync(javaClasses, { recursive: true, force: true });
}
if (failure) throw failure;
