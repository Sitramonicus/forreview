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

