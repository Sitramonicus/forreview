# MotorPH MS2 — persistent-workspace foundation

An original browser application with a Java 11 HTTP API, Vite frontend, Firebase Authentication, and user-scoped Firestore storage. The current dataset is **synthetic only**. This repository is an in-progress foundation, not a complete payroll or HR product.

## Runtime policy

- The normal Java entry point refuses to start without `FIREBASE_PROJECT_ID`; it no longer silently serves process-memory data.
- The browser refuses to open the workspace when Firebase web configuration is missing. There is no production local-preview fallback.
- The test suite has an explicitly isolated synthetic fixture mode bound to loopback. It is not enabled by `run.sh` or `run-api.sh`, and it is used only for automated tests.
- No temporary payroll calculator, payroll preview, or downloadable estimated payslip is included. Payroll calculations are disabled until the unresolved rules and authorization boundaries are approved.
- Use only fictional data. Do not enter real employee personal, payroll, attendance, or government-identification information.

## Run the Firebase-backed app

Requires JDK 11+, Node.js 20.19+ (or a current supported Node release), and npm. Install the browser Firebase web-app configuration in ignored `.env.local` using the documented variables in `.env.example`; expose `FIREBASE_PROJECT_ID` to the Java process. `run.sh` can derive the project ID from `.env.local` when the web API key is non-placeholder. The Web API key is a public client identifier, not a server credential; never add a service-account key or Admin SDK credentials.

```sh
npm install
./run.sh
```

Open <http://localhost:8080>. For Vite hot reload, use two terminals:

```sh
# terminal 1: Firebase-backed Java API
./run-api.sh

# terminal 2: browser frontend
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` to `http://127.0.0.1:8080` by default; set `API_PROXY_TARGET` for a different backend. Configure Firebase Authentication's Email/Password provider and authorized domains, and publish/review `docs/firestore.rules` for the intended development sandbox before using cloud writes; the current rules file includes owner-scoped `demoEmployeeSchedules` read/create access for fictional seed mappings. Firebase rules remain the authorization boundary for Firestore REST calls forwarded with the caller's ID token.

## Checks

```sh
npm run test:web
./test.sh
npm run test:e2e
npm run build
npm audit --audit-level=high
```

The automated end-to-end suite starts a loopback-only Java/Vite synthetic fixture and cleans it up. It verifies UI and API behavior, not Firebase deployment, live rules, or payroll compliance. The optional `npm run test:firebase-auth` lifecycle check uses a disposable test account and makes no Firestore writes; treat it as an external smoke test and avoid concurrent runs. It requires Firebase configuration and network access.

## Current capabilities

- Firebase email/password account sign-in, account creation, password reset, and sign-out. Browser auth uses in-memory persistence.
- User-scoped synthetic employee directory search/detail and limited create/edit fields. This is not an organizational HR role or a production employee lifecycle workflow.
- Raw attendance read and coverage reporting from the user's Firestore sandbox. Employee-to-schedule links are read from owner-scoped Firestore documents created by the opt-in synthetic seed. No attendance write route/form exists; the app does not calculate hours or classify absences.
- Payroll that reproduces MotorPH's supplied payslip to the centavo from an effective-dated statutory ruleset, with the whole chain (ruleset read → stored timecard → preview → saved immutable run → reprint) covered by HTTP integration checks. Days worked is still an operator-confirmed input; see [`docs/payroll-ruleset-and-golden-case.md`](docs/payroll-ruleset-and-golden-case.md) for what is and is not implemented.
- Owner-scoped synthetic leave-request and ticket create/list/cancel flows, backed by Firestore. There are no reviewer, approval, rejection, resolution, balance, notification, or payroll-treatment workflows.
- Search, filters, sorting, accessible detail views, raw-data reports, and CSV/JSON export for supported synthetic sandbox records.
- Opt-in “Add missing synthetic fixtures” action for fictional employee, mapping, and raw-attendance sample documents. The mapping values preserve the earlier synthetic fixture only; the seed does not copy process-memory state to Firebase and does not define production schedules.

## Deliberately disabled / incomplete

The following are not implemented as working product features: persistent schedule assignment and attendance capture/correction; verified organization-level roles or employee-user mapping; manager/team scope; leave approval and balances; ticket review/resolution; overtime/night-differential hour derivation; leave pay, holiday and rest-day premiums, and 13th month; multi-employee payroll runs in the UI; payroll approval workflow; production operations/backup/retention. Payroll itself is implemented and produces figures: an effective-dated ruleset computes a payslip that matches MotorPH's supplied payslip to the centavo, and each saved run is an immutable snapshot that can be reprinted. Days worked is still an operator-confirmed input rather than an automatic timecard derivation, and the app makes no certification or regulatory-compliance claim.

The existing Firebase path structure (`users/{uid}/demoEmployees`, `demoAttendance`, `demoEmployeeSchedules`, `demoLeaveRequests`, `demoTickets`, and `demoPayrollRuns`) is a development sandbox. Owner-only rules are not a substitute for a company role model. Peer submissions and official tables are research/candidate references, not MotorPH policy. See [`docs/build-status-no-local-no-temp.md`](docs/build-status-no-local-no-temp.md) for the live implementation status, verified tests, and next vertical slices; payroll now calculates against an effective-dated ruleset and reproduces the supplied MotorPH payslip to the centavo, with the full route set covered by HTTP integration checks (`./test.sh`, 318 checks) and the saved-run history kept as immutable, reprintable snapshots — see [`docs/payroll-ruleset-and-golden-case.md`](docs/payroll-ruleset-and-golden-case.md). Payroll policy questions raised in cross-review (hourly rates, leave credits, 13th month, Pag-IBIG
boundary, overtime, paid leave, early login, self-approval) are answered with evidence in
[`docs/statutory-and-policy-questions.md`](docs/statutory-and-policy-questions.md). **Start with [`docs/START-HERE.md`](docs/START-HERE.md)** — a 10-section plain-language guide (screens, one worked payslip, what's missing). For full function-by-function detail see [`docs/full-pseudocode-inventory.md`](docs/full-pseudocode-inventory.md) for a complete, current pseudocode walkthrough of every feature, function and subfunction — start there to understand what the app does. See also [`docs/function-pseudocode-and-handoff-synthesis.md`](docs/function-pseudocode-and-handoff-synthesis.md) for the earlier handoff synthesis (predates payroll) and the open policy/formula decisions, [`docs/local-only-inventory-and-full-build-pseudocode.md`](docs/local-only-inventory-and-full-build-pseudocode.md) and the linked peer feature catalogue for design references, not evidence of completion.

## Dependencies and supporting references

The Java API uses the vendored Gson JAR; licensing is recorded in `THIRD_PARTY_NOTICES.md` and `third_party/licenses/`. `docs/source-grounding-and-open-rules.md`, `docs/candidate-policy-review.md`, `docs/acceptance-baseline.md`, and `docs/peer-parity-roadmap.md` describe source evidence, unresolved decisions, and the implementation roadmap. Prior-submission code is reference material only and should not be represented as this project's authorship.
