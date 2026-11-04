# MotorPH MS2 — browser payroll foundation

An original browser-based prototype with an HTML/CSS interface and Java 11 backend. Employee and attendance data are synthetic. It includes a disposable, client-only 2026 statutory preview with editable/versioned rules; attendance-derived payroll, persisted runs, and payslips remain gated.

## Run locally

Requires JDK 11+, Node.js 20.19+ (or a current supported Node release), and npm.

```sh
npm install
./run.sh
```

Then open <http://localhost:8080>. `run.sh` builds the Vite frontend and serves it from the Java server. Set `PORT` to choose a different HTTP port.

For development with Vite hot reload, use two terminals:

```sh
# terminal 1: Java backend
./run-api.sh

# terminal 2: frontend
npm run dev
```

Open <http://localhost:5173>. Vite proxies `/api` calls to the Java service at `http://127.0.0.1:8080` by default; set `API_PROXY_TARGET` to use a different backend. Set `HOST=127.0.0.1` for the Java backend when Vite is proxying it locally.

Run Java domain checks:

```sh
./test.sh
```

Run browser end-to-end checks (the suite starts isolated local API/Vite processes, uses only synthetic in-memory records, and cleans them up on exit):

```sh
npx playwright-core install chromium
# Linux, if system libraries are missing:
sudo npx playwright-core install-deps chromium
npm run test:e2e
```

Optional live Firebase Auth lifecycle smoke test:

```sh
npm run test:firebase-auth
```

This uses `.env.local`, creates one uniquely named disposable Auth account, verifies sign-in, empty user-scoped reads, and sign-out on localhost, makes **no Firestore writes**, and deletes the Auth account in cleanup. It needs network access and should not be run concurrently with another live Auth test.

The app uses a small vendored Gson JAR for the Java Firestore REST adapter; its license is recorded in `THIRD_PARTY_NOTICES.md` and `third_party/licenses/`.

## Firebase development sandbox

- `.env.local` contains the Firebase **Web** app config used by the browser SDK; `.gitignore` lists this file. The user-supplied Web API key for `motorph-jd` is installed assistant-side. The user does not need to edit `.env.local`.
- The Web API key is a public client identifier, not a server credential. Do not add a service-account key or Admin SDK credentials.
- Authentication uses Email/Password and in-memory browser persistence. Live disposable-account creation/sign-in, empty user-scoped reads, and sign-out now pass on localhost; the test Auth account was deleted. Do not use real staff accounts or data.
- Firestore requests are made by the Java backend through the REST API, forwarding the signed-in user's Firebase ID token. Firestore Security Rules authorize those requests; no privileged server identity bypasses the rules.
- The app uses `users/{uid}/demoEmployees`, `demoAttendance`, `demoLeaveRequests`, and `demoTickets`. Workflow documents are owner-scoped with no reviewer or delete permission in the candidate rules. The opt-in seed writes fictional employee/attendance records only.
- The user published the updated `docs/firestore.rules`. Live Firebase smoke testing with disposable accounts verified owner-scoped employee create/update, leave and ticket create/cancel/read-back, and a cross-user denial (`403`). Both disposable Auth accounts and employee documents were deleted; one cancelled synthetic leave and one cancelled synthetic ticket remain under their deleted test UIDs because workflow deletes are intentionally denied (`LV-3502C01F0E60`, `TK-F05DD2B0D6CC`). No real employee data or credentials were used. These are **development-only** rules, not production role/HR policy.
- If Firebase is not configured locally, the app uses the in-memory synthetic fixture. No Firebase writes are made in that mode.
- `measurementId` / Firebase Analytics is not used.
- Browser auth is verified on `localhost`. The current Arena preview host is `8080-is3s38ic4lxn7e4e77g8b.e2b.app`; its authorized-domain status could not be tested from the sandbox because Arena's traffic-access token is injected only in the user preview session. If the preview reports `auth/unauthorized-domain`, add that exact host under Firebase Console → Authentication → Settings → Authorized domains.

See `docs/firebase-setup-guide.md` for the console checkpoint and `docs/firestore.rules` for the development rules.

## What works

- Overview with live sandbox counts, searchable employee directory/detail, employee directory sort controls, HR directory search/status filters/sorting, synthetic directory/workflow label-count summaries, searchable/mapped-filterable/sortable employee-linked record counts with all/visible CSV, sortable raw attendance register with a raw-only month calendar and searchable/filterable/sortable coverage report, cross-module sandbox search/export, Requests & tickets, searchable/sortable read-only Team roster with read-only detail view, and a temporary 2026 monthly statutory preview. Employee details open from a native keyboard-operable button without overriding table semantics; result counts and form feedback use polite status regions.
- Java employee/attendance services plus a workflow service. Local preview workflow and raw attendance-entry records are held in Java process memory; Firebase mode uses the signed-in user’s Firestore subcollections. Raw attendance remains read-only in Firebase mode.
- Leave requests and tickets validate inputs, derive employee names from the caller’s own synthetic directory, show documented queue labels, and support owner-sandbox cancellation. A monthly leave-request calendar displays pending/cancelled request dates only and can draft form bounds from two selected dates; it does not imply approval, submit a request, calculate days, or show balances. Search/status/type and inclusive requested-date-overlap filters, plus selectable table sorting, narrow the leave history display only; they do not change the calendar markers. Separate CSV controls export visible or all records. Row details reveal the submitted note/message and UTC timestamps without adding a reviewer workflow. Both local and Firestore workflow histories default to newest first; optional table sorting does not change storage order or all-record exports. Ticket lists also filter by stored type; workflow tables offer visible-only or all-record CSV export. No reviewer/approval/resolve/delete routes exist. The user has published the rules; live tests confirmed leave and ticket create/cancel persistence, plus cross-user denial.
- Read-only Team roster plus a synthetic directory editor. In Firebase mode, signed-in users can create/edit only name, department, role label, location, and demo status in their own sandbox. Local preview has no user authentication and shares one process-memory store for that API process. IDs are server-generated; delete, termination, schedule, salary, and government-ID fields are not exposed. New employees receive no schedule mapping, so attendance queries remain empty and raw capture rejects them rather than assigning one. A local-only attendance form can create a raw DEMO row for existing mapped synthetic employees; it enforces unique schedule/date and time order. Firebase attendance stays read-only; no correction, hours, or payroll calculation is included. This is not an HR role or a production employee-admin workflow.
- Employee detail now includes a read-only employee-specific ledger of raw attendance times and stored leave/ticket labels. The attendance register adds a month calendar showing only loaded raw row dates/times; selecting two dates applies an inclusive raw-record filter. Blank dates do not mean absence or unworked.
- CSV export for currently visible synthetic employee results, filtered HR directory and Team roster rows, loaded raw attendance rows, visible attendance coverage rows, synthetic directory, workflow label-count, and employee-linked count summaries, and either visible or all owner-sandbox workflow records. A read-only workspace JSON bundle and cross-module search use only owner-scoped GETs and include no payroll preview inputs, credentials, calculated hours, or pay. CSV cells are quoted and spreadsheet-formula prefixes are neutralized.
- Temporary monthly 2026 statutory preview: explicit separate fictional bases, SSS MSC/employee/employer/EC, PhilHealth employee/employer shares, Pag-IBIG employee/employer shares, BIR monthly withholding, net estimate and employer contribution cost. Logic is isolated in versioned rules/calculator modules; it is client-side only, is not retained, and can be exported as a clearly labelled preview CSV that is not a payslip.
- Server-side employee search/detail, HR table search/status filters, a user-scoped attendance-coverage report (raw row counts and first/last record dates by configured mapping), synthetic directory label counts by status/department plus workflow label counts by status/type/queue, both with CSV export, and dynamic overview counts. Unmapped employees are labeled as unmapped, not absent. The attendance date fields default to that employee's earliest/latest available records, following the earlier MO-IT108 Requirements Analysis; the user can narrow the range. This is not a payroll-period calendar. The attendance API and reports do not derive hours or pay; the separate statutory preview uses only manually entered synthetic monthly bases.
- Firebase email/password sign-in, development-account creation, and password-reset email UI; reset responses do not disclose whether an email has an account. The app stays behind the sign-in gate when Firebase config is complete.
- `/api/health`, employee list/search/detail plus owner-sandbox create/update, raw attendance query and coverage report, a local-process-memory-only raw attendance create route, owner-scoped workflow list/create/cancel routes, and opt-in demo seed endpoint.
- Verification: `npm run test:web` passes 59 workflow/calendar/filter/sort/summary/CSV/search/workspace-export/payroll-preview tests; `./test.sh` passes 181 Java checks; `npm run test:e2e` passes the local browser flow for employee create/edit, employee sorting/export, HR/Team filters and sorting with filtered CSV, empty unscheduled attendance, raw directory-label summary/CSV, reversed-date rejection, leave/ticket submit/cancel and detail disclosures, calendar date drafting, inclusive leave-range and type filters, visible/all workflow exports, raw attendance summary/sorting/export/calendar, employee-linked record detail, mapped-filtered/sorted count summary and visible/all CSV, read-only workspace JSON export and cross-module search, local-only raw row creation with duplicate/mapping/time-order rejection, API response metadata and Firebase-mode write rejection, report search/filter/sort/drill-through and CSV, persisted workflow-label count summary and CSV, temporary 2026 statutory preview CSV (not a payslip), no payroll API call, and negative-net blocking, simulated workflow/report authorization and service failures with recovery, reload persistence, and mobile width; `npm run build` passes; `npm audit` reports 0 vulnerabilities. An isolated live Firebase auth-lifecycle browser check passed disposable account creation/sign-in, user-scoped empty Firestore reads, and sign-out on localhost; the account was deleted and no Firestore writes were made. The exact Arena preview-origin sign-in remains unverified.

## Not implemented / important limits

- No production hosting, HR authorization roles, admin controls, audit trail, automated backup/restore, or production security review. The JSON download is a portable snapshot, not a restore path. The built-in JDK HTTP server is a learning prototype, not a hardened public service.
- No live employee, attendance, or payroll information. Do not use this prototype for payroll processing.
- A temporary 2026 monthly statutory preview is available with explicit synthetic inputs; its formulas/rates are isolated in `web/payroll-rules-2026.js` and `web/payroll-preview-2026.js`. There is still no work-hour derivation, attendance-to-gross formula, semi-monthly tax/split handling, saved payroll run, or payslip. The supplied material does not define the full attendance mechanics or exact cutoff/deduction allocation, so those are not inferred.
- Leave/ticket records are persistent only in the signed-in user’s sandbox after the updated rules are published; the current local preview uses process memory. There is no supervisor/reviewer role, real queue delivery, approval, notification, audit trail, team assignment, HR lifecycle editing, attendance import, or approved source-system integration.

## Source and attribution

`docs/source-grounding-and-open-rules.md` records supplied-source facts, scope boundaries, and unresolved ambiguities. `docs/acceptance-baseline.md` captures the current source-grounded behavior and gates as a draft, not an approved rubric. `docs/candidate-policy-review.md` reviews the user's database design and later calculator, peer assumptions, official statutory sources, and a net-pay reference; it distinguishes the enabled temporary monthly statutory preview from still-gated attendance/pay-period mechanics. `docs/legacy-mo-it108-traceability.md` maps the user's earlier MotorPH requirements/design documents to the current prototype and explains the feature-parity estimate. `docs/peer-parity-roadmap.md` lists the remaining phases and acceptance gates to reach broad peer-level feature parity. `docs/local-demo-inventory.md` separates process-memory local behavior from Firebase-backed owner-sandbox behavior. `docs/local-only-inventory-and-full-build-pseudocode.md` lists local-only paths, current behavior pseudocode, required non-local changes, and the full target build plan. The restrained title stripe adapts a small MIT-licensed `system.css` motif; exact attribution and licenses are listed in `THIRD_PARTY_NOTICES.md`.
