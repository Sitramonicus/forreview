# Build status — persistent-only target

> **Payroll status update (2026-11-05):** payroll now calculates against an effective-dated
> ruleset and reproduces the supplied MotorPH payslip to the centavo. See
> `docs/payroll-ruleset-and-golden-case.md`. The statements below about a removed *temporary
> preview* remain historically accurate; what replaced it is a durable, saved-run feature, not a
> browser-only estimate.

**This document supersedes earlier statements that a process-memory product mode or a temporary payroll preview is an acceptable deliverable.** The user's current requirement is “No locals, no temps.” No peer implementation or earlier policy candidate changes that requirement.

## Product behavior now enforced

- `AppServer.main` requires `FIREBASE_PROJECT_ID`. The only no-Firebase entry-point exception is `MOTORPH_TEST_MODE=true` while binding to a loopback host; the production run scripts do not set it. Direct constructor-based local services remain test fixtures used by the isolated test harness, not a supported deployment mode.
- `web/app.js` does not open the workspace if Firebase web configuration is missing. It shows a setup error instead. The unauthenticated local workspace is available only in the test build flag `VITE_E2E_TEST_MODE=true` used by the test runner.
- Removed the browser-only 2026 payroll preview that existed on 2026-11-04. On 2026-11-05 it was replaced by a server-side, effective-dated payroll service grounded in MotorPH's supplied payslip and timecard: `PayrollService`, `PayrollRuleset`, `TimesheetService`, saved `PayrollRun` snapshots in Firestore, and payslip rendering in the Payroll view. The removed artefact was a synthetic-input estimate; the replacement stores its inputs, ruleset version and assumptions with every run.
- The test-only synthetic fixture remains intentionally process-memory-backed so the UI/API contract can be exercised without using real staff data or writing test records to a live Firebase project. This is test infrastructure, not a deployable local-data feature.

## Test totals (2026-11-06, after the RBAC and payroll-cycle work)

| Suite | Result |
|---|---|
| `./test.sh` (Java: domain, Firestore adapters, RBAC, payroll, identity, integration) | **614 checks PASS** |
| `npm run test:web` | **62 / 62 PASS** |
| `npm run test:e2e` (real browser) | **17 PASS** |

Breakdown of the Java gate: 17 employee-domain, 23 attendance-domain, 32 workflow-domain, 19 Firestore
REST adapter, 14 Firestore workflow-store, 7 employee-schedule mapping, **245 RBAC policy**, **34
payroll period and salary history**, **45 identity and seat**, **177 AppServer integration**, plus the
production-refuses-local check (1). Those add up to 614, which is what `test.sh` reports.

## Verified after these changes

- **The roster is now MotorPH's own file (2026-11-06).** The four invented `DEMO-` employees that
  shipped in the fixture are gone. `tools/generate_fixtures.py` reads
  `uploads/Copy of MotorPH_Employee Data - Employee Details.csv` and writes all **34 rows**
  (10001–10034) with the supplied names, positions, basic pay and three allowances into
  `InMemoryEmployeeStore.java`. Department is derived from position (the CSV has no department
  column); status is "Active" for all 34 because the CSV's `Status` column is employment type.
  12 test classes, 13 web test files and the e2e flow were re-pointed from `DEMO-00x` to the real
  numbers. New records now receive the next free five-digit number (10035 onward) instead of a
  `DEMO-<hex>` ID; `docs/firestore.rules` requires `^[0-9]{5}$`.
- **Employee pay is now editable (2026-11-06).** The HR editor previously exposed no pay fields, so
  "where do I change a rate?" had no answer. Monthly basic plus rice/phone/clothing allowances are
  now inputs on **HR → Employee records → Edit**, are sent with create/update, and are what payroll
  reads. Omitted pay fields keep their stored values.
- **Salary history exists (2026-11-06).** A rate is a dated row, not a field on the employee record,
  so a raise no longer rewrites a payslip that has already been paid. `SalaryHistory` is seeded with
  one opening rate per employee from the employee file (effective 2025-01-01), Payroll Staff record
  further rows via `POST /api/payroll/salary-history`, and a run that declares a cycle must reconcile
  with the rate in force at that period's end or be refused. Effective dates must be the first of a
  month, because a cycle cannot be paid at two rates.
- **Payroll periods exist (2026-11-06).** `PayrollPeriod` defines one set of dates for everybody,
  validated against its cadence: monthly runs 1st→last day; semi-monthly is 1st–15th or 16th→last
  day. A typed "30" for February, a ten-day range, or a range spanning two months is refused. A run
  that names a period must carry that period's dates, and a closed period accepts no further runs.
- **Identity is verified, not asserted (2026-11-06).** The server used to treat *any* non-empty
  `Authorization` header as a signed-in account and take the employee number from a client header, so
  two crafted headers made anyone the System Admin. Tokens are now verified against Google's
  `accounts:lookup`, `FIREBASE_API_KEY` is required at startup, the person comes from a seat registry
  the account is actually linked to, and the seat comes from that person's real roles. See
  `docs/identity-seats-and-testing-roles.md`.
- **One account can exercise every role (2026-11-06).** A person holds a *set* of seats
  (10006 is HR Manager and also supervises somebody) and an account can be linked to *several*
  people, so testing five roles needs one login, not five accounts. `POST /api/me/links` creates those
  links and is HR-only; nobody may edit their own.
- **Five account types enforce access (2026-11-06).** `Policy` holds the design document's 12×5
  matrix, `AccessControl` enforces it on every route, and the actor's role comes from their employee
  number — never from a header the browser chooses. Directory listings are filtered, so a response
  cannot contain a row the caller may not read. `RbacPolicyTest` checks the matrix cell by cell.
- **SSS salary credit corrected (2026-11-06).** The engine rounded the credit **up** to the next
  ₱500; SSS publishes ₱500-wide **brackets** (5,250.00–5,749.99 → 5,500.00). Round-up overstates
  the credit in the lower half of every bracket (₱10,001 → ₱10,500 instead of ₱10,000; ₱25,249 →
  ₱25,500 instead of ₱25,000). Now banded to the bracket. **No MotorPH employee was affected** —
  all 34 salaries sit above their bracket midpoint — so the golden payslip is unchanged.
  `PayrollGoldenTest` 65 → 70. The claim in `docs/sweldongpinoy-crosscheck.md` §4 that ceil and
  bracket banding were "verified identical" was wrong and has been withdrawn.
- **The reference comparison now refuses non-like-for-like cases (2026-11-06).** Their calculator
  only prices a whole month; its semi-monthly branch doubles the basic and allowances it is sent.
  The route used to pass the monthly basic for a semi-monthly cutoff and report the resulting
  difference as if it were an arithmetic error. It now returns `available:false` with a reason for
  any semi-monthly request and for any month where days worked is below 20. Verified live: Monthly /
  20 days / ₱53,500 matches on all six lines at ₱49,461.67; the other two cases are declined with an
  explanation. UI labels the three states `MATCHES` / `NOT COMPARABLE` / `UNAVAILABLE`.
- **Source-file conflict surfaced, not reconciled (2026-11-06).** Employee 10015's phone allowance is
  ₱2,000 on MotorPH's sample payslip and ₱1,000 in the employee master file. Payroll follows the
  employee file, so that person's statutory lines still match the payslip exactly while benefits and
  take-home differ by ₱1,000 (₱21,711.67 vs ₱22,711.67). The Payroll screen now prints this under
  the form for that employee, `/api/payroll/rules` exposes it as `dataConflicts`, and the golden case
  still reproduces ₱22,711.67 from the payslip's own figures.
- **Withholding-table correction (2026-11-06):** the 25/30/35% bands used `8,541.80 / 33,541.80 /
  183,541.80` as their base instead of the annual amounts over 12 (`102,500/12 = 8,541.67`,
  `402,500/12 = 33,541.67`, `2,202,500/12 = 183,541.67`). A 13-centavo error on every payslip above
  ~₱90,000 basic. Found by comparing against sweldongpinoy.com, fixed, and pinned by three new tests
  (`PayrollGoldenTest` 61 → 65). The ₱53,500 golden case was never affected.
- **Reference comparison added:** `POST /api/payroll/compare` plus a "compare with sweldongpinoy.com"
  checkbox. Optional, server-side, read-only; a failure can never affect a payslip. Verified live:
  all six statutory lines match.
- `npm run test:web` — passed, 62 browser/domain tests.
- `./test.sh` — passed 339 Java checks, plus a production-entry-point assertion that startup fails without Firebase configuration. `AppServerIntegrationTest` now covers the payroll routes over HTTP (125 checks total): ruleset reads and uncovered-year refusal, timesheet reduction to 19 days and 152.00 hours, preview matching every golden payslip line, save/preview/history/replay, immutability, unknown-field and bad-period rejection, the Firebase session gate, and confirmation that every payroll Firestore call stays inside the token's user sandbox.
- `npm run test:e2e` — passed the isolated fixture flow and the browser fail-closed check when Firebase configuration is absent; the payroll check now asserts the reproduced payslip, the saved run, and the reprintable run-history row (total earnings, deductions, net, employer cost).
- `npm run build` — passed.
- `npm audit --audit-level=high` — 0 vulnerabilities.
- Live sandbox preview is running on port `8090`; `GET /api/health` returns `firebase-user-sandbox`. This verifies the Firebase-configured server starts, not browser sign-in, cloud reads, or the un-published schedule-mapping rules.
- No Firebase Console action or Firestore write was performed in this slice. Live Firebase integration is not newly re-verified here. The Firestore rules file now includes the owner-scoped schedule-mapping collection; the project owner must publish that rules update before seeded attendance can be linked in Firebase. The rule text was not run through the Firestore emulator or published in this slice.

These are verification results for the code paths exercised, not proof that the full product is complete or deployed.

## Current persistent vertical slices

- Firebase Auth sign-in and caller-token-forwarded Firestore REST access with user-owned paths and reviewed rules.
- Limited synthetic directory read/create/update.
- Raw attendance Firestore reads/reports use owner-scoped `demoEmployeeSchedules` mapping documents; there is no attendance write route/form. The opt-in synthetic seed persists the pre-existing fixture links as data. Schedule assignment editing remains disabled.
- Synthetic leave/ticket create, list, and owner-side cancel; no reviewers, approvals, balances, or notification semantics.
- Search/filter/sort/export over existing sandbox data.

The seeded records are fictional examples and are not copied from the process-memory fixture. The cloud schema is still a per-user development sandbox, not a company tenant or validated production employee database.

## Function-level inventory

`docs/function-pseudocode-and-handoff-synthesis.md` catalogues every named Java method and every browser function with observable pseudocode, tags each one as live / read-only / validation / fixture / gap, states what is missing per area, and lists the policy and formula decisions (D-01 … D-53) that must be supplied before schedule, timekeeping, leave-approval, or payroll work can proceed. It also records the synthesis of the received external handoff package and explicitly rejects its local-JSON persistence and active payroll preview as current behaviour.

## Next implementation slices

1. **Schedule administration and raw attendance capture:** establish an approved schedule model and assignment lifecycle, then a durable attendance writer with authorization/rules tests. The former process-memory attendance writer has been removed; `POST /api/attendance` returns `405`. Current immutable seed mappings are not an editable schedule-management feature. The mappings are copied from the earlier synthetic fixture only; they are not business schedule definitions. `dayType=DEMO` is sample data, not a classification policy.
2. **Identity, organization scope, and roles:** define tenant membership, employee-user mapping, and role-to-action boundaries before adding supervisor or HR review access. Existing owner-only Firebase rules do not support this.
3. **Attendance/timesheet calculation:** keep raw timestamps separate from calculated time until break, shift, timezone, overnight, grace/rounding, correction, holiday, leave, premium, and golden-case rules are approved.
4. **Durable payroll:** after approved examples and statutory period/basis/rounding choices, implement a versioned calculator, immutable input/output snapshots, auditability, saved runs and payslip authorization. Until then, there is no payroll preview or calculation route.
5. **Operations and deployment:** emulator/rules coverage, environment and origin configuration, indexes, backup/retention, error reporting, recovery, accessibility/security review, and a deployment smoke test.

No unresolved rule should be filled from a peer submission without user approval. Keep all test records synthetic and isolated; do not use user or employee production data during development.
