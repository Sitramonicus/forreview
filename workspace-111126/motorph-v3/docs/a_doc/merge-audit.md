# Claude's base vs ours — what merged, what changed, what's left

Compared `motorph-ms2-main` (Claude's build) against `motorph-v3` (ours), file by file.

---

## Verdict

**Nothing was lost.** Every class and every web module in Claude's build is present in ours, and
the payroll engine, the access matrix and the workflow rules are all still there. What changed is
architecture: Claude had one flat package and one flat `web/`; we split them by responsibility and
replaced the identity system.

| | Claude | Ours |
|---|---|---|
| Main classes | 61, all in `ph.motorph.web` | 80 across 6 packages |
| Test suites | 15 (some legacy, now archived) | 15 (14 Java + 119 web tests) |
| Web modules | 37 JS, flat | 37 JS, in `b_basic` / `f_flares` |
| Auth | Firebase + 2FA + account picker | employee number + password, then a 6-digit code |

---

## Java: class-by-class

Every one of Claude's 61 classes exists in ours. The only four names that differ are renames forced
by the brief's ban on the word "demo":

| Claude | Ours | Why |
|---|---|---|
| `DemoTicket` | `LeaveTicket` | §10 |
| `DemoLeaveRequest` | `LeaveRequest` | §10 |
| `DemoOvertimeRequest` | `OvertimeRequest` | §10 |
| `DemoSeedService` | `FirestoreSeedService` | §10 |

**23 new classes**, none of them replacing lost behaviour — they're all auth, org-chart or payroll
output that the brief required and Claude didn't have:

`AccessMatrix`, `Account`, `AuthService`, `Branch`, `CredentialStore`, `CsvLine`, `EmpRecords`,
`EmployeeImport`, `FirestoreSeedService`, `LeaveRequest`, `LeaveTicket`, `Locality`, `OrgChart`,
`OvertimeRequest`, `PasswordHasher`, `PasswordPolicy`, `Payslip`, `PayslipWriter`, `RolePolicy`,
`SampleSlipCorrections`, `SessionCookie`, `SessionRecord`, `SessionStore`

### Method-level check

Of the 57 classes that exist in both builds, **53 have every method Claude wrote, still present**.

The 4 that differ, and why each is correct:

| Class | Missing | Reason |
|---|---|---|
| `AppServer` | `handleDemoAccounts`, `handleSeedDemo`, `sandbox` | Routes renamed (`/api/demo/accounts` → `/api/accounts`, `/api/demo/seed` → `/api/firestore/seed`); `sandbox` was Firebase-only |
| `InMemoryEmployeeStore` | `addFixture` | It existed *only* to seed DEMO-001…DEMO-005. Deleting the fake people deletes the method. |
| `Employee` | `requireText` | Moved to `CsvLine`; the validation still runs on every field |
| `InMemoryEmployeeScheduleStore` | (constructor variant) | Signature changed; behaviour identical |

### Behaviour never weakened

- **`AccessPolicy`**: a near-total rewrite (367 diff lines), but it went *up* in strictness. Claude
  had 5 roles and 11 areas. We have **9 roles** (added CEO, CFO, COO, CMO) and the same 11 areas,
  and every guard method — `canRead`, `requireRead`, `requireArea`, `requirePayrollProcessing`,
  `requireHrManagement`, `requireGovernmentRead` — is still there and still enforced.
- **`CutoffPayrollService`**: rewritten for the worked-holiday bug (§ the fix is recorded in
  `session-progress.md`). Verified against DOLE for all four day types.

---

## Web: module-by-module

**All 37 of Claude's JS files are present.** Line counts differ by ±10 in most files, and that
difference is removed comments plus the label sweep — no module was dropped.

The 10 functions that no longer exist in `app.js` are all **deliberately retired**, each tied to the
Firebase identity system the brief told us to remove:

| Removed | Was |
|---|---|
| `startAccountChooser`, `showAccountList` | The account picker, replaced by the employee-number form |
| `askPassword`, `startTwoFactor`, `savedSessionFor` | Client-side 2FA, rebuilt server-side (see below) |
| `createDevAccount`, `sendPasswordReset` | Firebase dev-account helpers |
| `seedFromChooser`, `seedSyntheticDemoData` | Seeded fake people into a sandbox |
| `editSyntheticEmployee` | → renamed `editImportedEmployee`, still wired to the Edit button |

---

## Bugs the merge check caught — all now fixed

Comparing against Claude's tree is what surfaced these. None were caught by the 533 Java checks.

1. **`showSignIn()` set `.hidden` on a deleted element** — `TypeError: Cannot set properties of
   null`, thrown on every page load. Every session restore hit it.
2. **42 web unit tests silently broken** — they imported from the old flat `web/` paths after the
   folder move. They had been passing before the move and failing after, with nobody running them.
   Fixed; **all 119 now pass** and `test.sh` runs them.
3. **The e2e suite drives a product that no longer exists** — see below.
4. **`"dayType": "DEMO"` on every attendance row** in the API. Now `"Imported"`.
5. **UI claimed statutory deductions are split across cutoffs.** The engine uses `FULL_FIRST` —
   the whole month's SSS/PhilHealth/Pag-IBIG/tax comes out of the 1–15 cutoff. Two pieces of help
   text said "half is deducted on each cutoff". Corrected to match the engine.

---

## Still open after the merge

### The e2e suite (1,200 lines) cannot pass
`test/e2e/local-workflows.e2e.js` signs in through the account picker and asserts the directory holds
exactly `DEMO-001`…`DEMO-005`. Every one of those is gone by design. It is a test of Claude's product,
not ours. It is **left in the tree, not deleted**, because rewriting 1,200 lines of browser assertions
is a separate job from this merge — but it will fail if run. Say the word and it gets rewritten against
the employee-number flow.

Note that the 2FA half of it is now reachable again: the sign-in is two steps, so a rewritten e2e would
type an employee number, read the code, and submit it, exactly as the old one did.

### Docs Claude carried that we did not
`docs/` in Claude's build has 12 files; ours has 12 different ones. Nothing was lost — ours are the
newer set. Worth knowing the old `api-contract.md` and `acceptance-baseline.md` describe the
Firebase routes.

### `vite.config.js` + `package.json` are vestigial
Both still point at Vite, Tailwind-from-npm and Firebase. We serve `web/` straight from disk and
compile CSS with `tools/css.sh`. Harmless, but they describe a build that no longer exists.

---

## Two-factor: removed, then put back

An earlier pass removed the 2FA step because §7 of the brief describes employee number + password and
nothing else. That was the wrong call and it has been reversed: the two-step flow is back, this time on
the server.

What is different from Claude's version:

- His ran in the browser. `twoFactor` here is a Java challenge store (`sp_secpol/TwoFactor.java`).
  `AuthService.signIn()` returns a challenge id — never a session — and only `verifyCode()` calls
  `sessions.issue()`. A password on its own opens nothing.
- The codes are `SecureRandom` six digits with a five-minute window, three attempts, single use.
  His were generated client-side, which is no second factor at all.
- `POST /api/auth/signin` → `{challengeId, employeeId, role, sentTo, expiresInMinutes, code}`;
  `POST /api/auth/2fa/verify` → session cookie + CSRF.
- `code` is in the response and drawn under the input because there is no SMS gateway on this
  deployment. That is the one deliberate deviation from a production system and the seam an SMS or
  TOTP sender replaces.

`AuthTest` covers the restore at 87 checks: a wrong code grants nothing, three wrong codes kill the
challenge, an expired code is refused, and the step-one id is not a session token.

## Firestore became the store, not a mirror (2026-11-08)

The brief says to keep an existing persistence service if it can stay useful, and to separate
authentication from persistence. The previous arrangement failed both halves: Firebase was
configured as an *authentication* provider, so with a project id set the server served employee
and attendance data from per-user collections, demanded a Firebase ID token on every read, and
refused payroll, clock-in, HR and notifications outright. Nothing was ever written to Firestore
by the app; the local file was the only store.

That is now one design instead of two:

- `StateStore` is an interface. `LocalStateFile` and `FirestoreStateStore` both implement it, and
  `AppServer.chooseStateStore()` picks one at startup. Nothing else in the server knows which.
- Every write goes through `dataFile.save(exportState())`, and `/api/health`, `/api/config` and the
  reset endpoint report `file`, `firestore` or `memory` from the store itself, so the UI can no
  longer hardcode "Local file store".
- The 48 `firebaseProjectId.isEmpty()` sites are gone. 25 of them were guards that disabled a
  feature whenever a project id was set; the rest were ternaries that swapped in stub data.
  `firebaseProjectId` now only selects the backend, and sign-in is employee number + password
  throughout.
- Attendance alone is 942,839 bytes and Firestore caps a document at 1 MiB, so a store is sliced
  at 700,000 characters (`motorphStateChunks/{store}_0000`) and rejoined on load. Slices are cut
  on characters, not bytes. `savedAt` is the only field that changes in a round trip.
- Writes are upserts. The REST client used to POST only, which real Firestore answers with
  `ALREADY_EXISTS`; the second save of a live app would have been lost. Stale chunks are deleted
  when a store shrinks, so the console shows exactly what is live.
- An empty backend is seeded from `data/local-demo.json`, not from the built-in roster stub. The
  stub carries employees but no attendance, so a fresh Firestore would otherwise have been
  created with 34 people and zero rows and then saved that emptiness back.

Verified end to end against a stand-in Firestore that implements the REST subset: upload 34
employees and 7,421 rows, restart, `Loaded saved data from Firestore`, 20 January rows for one
employee served from the cloud, a payroll adjustment written and still there after another
restart. `FirestoreStateStoreTest` (15 checks) and `FirestoreSeedServiceTest` (12 checks) cover
the same paths without a network.

To point it at the real `motorph-jd` project, see `docs/firebase-setup-guide.md`: it needs
`FIREBASE_PROJECT_ID`, `MOTORPH_FIRESTORE_TOKEN` and `MOTORPH_FIRESTORE_UID`. The token is a
Google OAuth access token, not a service-account key, and it expires hourly. Without those three
the server prints a warning and uses the local file, which is why the preview still starts.

## The e2e suite was dead, and is now real (2026-11-08)

`test/e2e/local-workflows.e2e.js` compiled `src/main/java/ph/motorph/web` — a directory that no
longer exists — and drove a Firebase-authenticated browser that has also been removed. It had not
run in a long time and could not have passed. `test/e2e/firebase-auth-lifecycle.e2e.js` was worse:
it read a `.env.local` that is not in the repository.

Both are replaced by one suite that starts the shipped build on a spare port with its own
throwaway data file, signs in through the UI, and asserts the things that have actually broken:

1. **The second factor is a gate.** A wrong code is refused, says so, and opens no workspace.
2. **Every view a role can reach loads.** All nine admin views render and issue no 4xx.
3. **The System Admin can read attendance.** This is the matrix drift fixed above; the check would
   have caught it, and it returns 213 rows where it used to return 403.
4. **An employee cannot read another employee.** `/api/employees/10001`,
   `/api/attendance?employeeId=10001` and `/api/government/10001` are all 403 for account 10018,
   while their own payslip is 200.
5. **A write survives a restart.** A payroll adjustment is filed, the server is stopped and
   started again, and the adjustment is still there.

It runs from `./test.sh` and skips cleanly when `playwright-core` or Chromium is absent, or when
`MOTORPH_SKIP_E2E=1`. That makes `./test.sh` 19 suites: 17 Java, 1 web unit, 1 browser.

One environment note for whoever runs it next: this sandbox has 1.9 GB of RAM, and Chromium plus a
default-heap JVM will deadlock. The suite starts its server with `-Xmx256m
-XX:TieredStopAtLevel=1`; without those flags `page.goto` times out against a server that is
plainly answering `curl`.

## First contact with the real project (`motorph-jd`, 2026-11-08)

The whole store was pushed to the live project and read back. Three things only the real API
could have taught us, all now fixed:

1. **`motorphState/chunks/{id}` is not a legal document path.** Firestore requires paths to
   alternate collection and document, so three segments after `documents/` is rejected with
   *"Document name lacks / at index 80"*. Chunks moved to their own top-level collection,
   `motorphStateChunks`. The test fake now enforces the even-segment rule — it had been accepting
   the invalid path, which is why 15 checks passed against a layout production refuses.
2. **`tools/FirestoreSync.java` still called `new FirestoreSeedService(remote, local)`** after the
   service's parameters were reordered to `(local, remote)`. Upload therefore read the *remote*
   side, found it empty, and reported "There is no local state to upload yet." Silent because the
   argument types are identical.
3. The fake's chunk assertions matched the wrong prefix, so the layout change was invisible to
   them.

Result against the live project: `motorphState` (`_meta`, `_index`) plus 31 documents in
`motorphStateChunks`, attendance split 700,000 + 153,787 characters. Read back through the API, the
state is **byte-identical to `data/local-demo.json`** — 30 stores, 34 employees, 7,421 rows. A live
server signed in, served attendance, wrote a payroll adjustment, was restarted, and the adjustment
was still there. The probe row was then deleted through the API and confirmed gone.

Note for whoever runs `./test.sh` next: this sandbox has 1.9 GB of RAM. Running the browser e2e
while a Firestore-connected server is also up starves Chromium and `page.goto` times out against a
server that answers `curl` fine. Stop the extra server first.
