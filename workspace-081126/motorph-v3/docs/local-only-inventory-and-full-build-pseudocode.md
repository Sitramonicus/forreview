# Local-only inventory, current behavior, and full-build pseudocode

**Status date:** 2026-11-04  
**Purpose:** identify every behavior that is genuinely local-only, explain its contents and implementation paths, state what would be required to make it durable/non-local, and provide pseudocode for the current build and the remaining full-system work. This is an engineering inventory—not a claim that the app is a complete or production-ready payroll system.

## 1. Current build status

The current project is a working synthetic-data prototype with local process-memory mode and Firebase user-sandbox mode. It has a limited but verified set of employee, raw-attendance, leave/ticket, reporting, and statutory-preview features.

Latest recorded verification: **59 web tests, 181 Java checks, Playwright E2E, production build, and dependency audit with 0 vulnerabilities**. The exact Arena preview-origin Firebase sign-in and production deployment are not verified.

The project’s rough estimate is about **72% broad feature parity**, or about **66% when local-only raw attendance creation and the browser-only statutory preview are excluded**. The user’s earlier 75% non-local target was not met before this documentation request; this estimate is not a grade or code-coverage score. The latest instruction now requests a full non-local build and permits adapting prior submissions as candidate logic. No Firebase Rules, Cloud Functions, or live Firebase data were changed or deployed in response. See `docs/peer-parity-roadmap.md` for the denominator and remaining feature gates.

## 2. What is genuinely local-only

“Local-only” here means that behavior either uses the unauthenticated Java process-memory store or is performed only in the browser without a durable server record. Some screens are available in both modes and therefore are **not** local-only even though their filtering/rendering runs in JavaScript.

| Local-only item | What it does | Contents / implementation | What must change for a non-local or durable version |
| --- | --- | --- | --- |
| Local preview identity/storage mode | Runs without sign-in when Firebase is not configured. Changes are shared by callers of that API process and disappear when it stops. | `AppServer` selects local stores when no Firebase project is configured. `InMemoryEmployeeStore`, `InMemoryWorkflowStore`, and `InMemoryAttendanceStore` hold synthetic records in process memory. The UI labels this `LOCAL PREVIEW` / `Local API memory`. | For multi-user durability, require authenticated Firebase identity, use only `users/{uid}/...` paths, enforce rules on the server, and define a deliberate migration/import process. Do not silently copy shared local rows into a user account; there is no employee-account identity mapping today. |
| Local raw-attendance create | Creates a raw row only in local in-memory demo mode. It does not edit or delete existing rows. | `MutableAttendanceStore.java` exposes create. `InMemoryAttendanceStore.java` synchronizes insertion and checks unique `(scheduleID,date)`. `AttendanceService.createForEmployee` verifies an existing employee and existing schedule mapping, passes the server-selected schedule ID, and fixes `dayType` to `DEMO`. `AppServer.handleAttendanceCreate` accepts exactly employee ID/date/time-in/time-out, returns `201` plus `storage=local-process-memory`, and returns `403` in Firebase mode. UI lives in `web/index.html` and `web/app.js`. | Add a durable, authenticated store implementation; ensure the employee and schedule mapping are owned by the signed-in UID; enforce unique schedule/date atomically under concurrent requests; allocate durable IDs; add security-rule or trusted-server tests; and test deny cases. Firestore rules currently do not make this write path safe/transactional. The current route intentionally rejects Firebase mode. A client-supplied `scheduleID` or `dayType` must remain rejected. **No Firebase attendance write is present in the current build.** |
| Temporary statutory preview | Computes a disposable monthly estimate from values manually entered in the browser. It is not a saved payroll run. | `StatutoryCalculator2026.java` calculates in integer centavos behind `POST /api/payroll/statutory`; `web/payroll-preview-ui.js` renders input/result and preview CSV. Nothing is stored. The E2E verifies only the statutory API is called. | Available in both modes and read-only. Making it a saved payroll requires approved rules, authorization, audit, and persistence: immutable run inputs/results and golden expected-value tests. |
| Local-only E2E environment | Proves the local browser flow against a disposable local Java API and test data. | `test/e2e/local-workflows.e2e.js` launches a local API and Vite server with Firebase config blank. It tests UI and API behavior but does not establish that the Arena preview origin or a live Firebase deployment works. | Keep these fast tests, and add Firebase Emulator/API contract tests for signed-in reads/writes and rule denials. A live preview-origin smoke still needs the exact authorized domain and a disposable account. Do not use undeletable workflow records for repeated smoke tests. |
| Downloaded workspace JSON | A client-created file download, not cloud backup/restore. | `web/workspace-export.js` creates a synthetic-only snapshot with employee records, raw attendance rows, leave requests and tickets; it marks `hoursCalculated:false` and `payrollIncluded:false`. `fetchSandboxSnapshot()` obtains data through GET APIs. | No change is needed for reading the current user’s Firebase sandbox; the bundle already uses scoped GETs when signed in. To restore/import the file into Firebase, add a separately designed import path, duplicate-ID handling, schema versioning, per-collection validation, user ownership checks, and preview/confirm/rollback behavior. Current export is not a restore function or automated backup. |

### Local-only feature file contents

- `src/main/java/ph/motorph/web/MutableAttendanceStore.java` — the writable interface limited to creating a raw `AttendanceRecord` from schedule ID, date, times, and fixed day type.
- `src/main/java/ph/motorph/web/InMemoryAttendanceStore.java` — synthetic attendance fixtures and synchronized in-memory create/duplicate check; store lifetime is the Java process.
- `src/main/java/ph/motorph/web/AttendanceService.java` — employee existence check, existing mapping lookup, mutable-store check, fixed `DEMO` assignment; it does not create mappings or calculate hours.
- `src/main/java/ph/motorph/web/AppServer.java` — POST route whitelist, date/time parsing, local-mode guard, JSON response and status codes. Firebase mode is rejected before store access.
- `web/index.html` — the local-only attendance form, explanatory boundary text, raw-record calendar, report/export/search UI.
- `web/app.js` — posts local raw input, loads scoped reads, renders the calendar and reports, cross-module search, JSON export, and employee-linked record details. `showWorkspace(user)` hides the create form for signed-in Firebase users.
- `StatutoryCalculator2026.java` (Java calculation, moved from the browser on 2026-11-05), `web/payroll-preview-2026.js`, `web/payroll-preview-ui.js` — browser display;only temporary statutory preview profile/calculator/UI; no persisted run.
- `test/java/ph/motorph/web/AttendanceServiceTest.java` and `AppServerIntegrationTest.java` — domain/API checks for mapping, fixed DEMO label, uniqueness, time order, schedule injection rejection, and Firebase POST rejection.
- `test/e2e/local-workflows.e2e.js` — local browser coverage including create success/rejections, calendars, exports, search, JSON bundle, and no-page-error/mobile checks.
- `docs/local-demo-inventory.md` — shorter local-vs-Firebase summary; this file is the more complete implementation inventory and pseudocode.

## 3. Capabilities that are not local-only

These currently use existing owner-scoped GET APIs and work in local preview and Firebase user-sandbox mode, subject to the user having access to the signed-in sandbox:

- Employee list/search/detail, allowlisted synthetic directory create/update, and HR/Team views.
- Raw attendance GET by employee and optional inclusive date range; Firebase attendance is read-only.
- Owner-scoped leave/ticket GET/create/cancel routes with existing field validation and published user-sandbox rules.
- Attendance coverage report, raw attendance calendar/date selection, employee-linked record detail/count summaries, cross-module search, CSV exports, and the read-only workspace JSON download.
- The local attendance *create* form is not shared; it is hidden in Firebase mode and its API returns `403` there.

No client-side report in this list infers absent/present/tardy status, hours, overtime, leave balances, approval, eligibility, or pay. Stored status/type/queue labels are displayed as labels only.

## 4. Current system pseudocode — what works today

### 4.1 Startup and mode selection

```text
read frontend Firebase configuration
if Firebase configuration is complete:
    initialize Firebase Auth
    show sign-in gate until Firebase reports a signed-in user
    for each API call, send caller ID token and UID headers
else:
    show LOCAL PREVIEW without authentication

Java API startup:
    if Firebase project is absent:
        use shared process-memory employee/workflow/attendance stores
    else:
        require valid-looking Firebase session headers for user-scoped reads/writes
        build Firestore services rooted at users/{uid}/...
        keep attendance store read-only
```

The Firestore Rules, rather than a UI label, are the security boundary. There is no organizational role or employee-account mapping in this build.

### 4.2 Synthetic employee directory

```text
GET /api/employees [optional search]
    select local store OR current user's Firestore employee collection
    return allowed synthetic fields and total count

POST /api/employees
    reject unknown fields and client-supplied ID
    validate name, department, role label, location, demo status
    generate synthetic ID on server
    save to local process store OR owner-scoped Firestore collection

PUT /api/employees/{id}
    reject unknown fields
    validate allowlisted directory fields
    update local process store OR owner-scoped Firestore document

UI
    search/sort/filter/list/detail
    HR/Team views display the same synthetic directory; no HR role is inferred
```

### 4.3 Raw attendance query, local create, and reports

```text
GET /api/attendance?employeeId=...&from=...&to=...
    require an existing employee
    find only the existing configured schedule mapping
    read rows from local store OR caller's Firestore collection
    apply inclusive optional dates; reject reversed range
    return raw attendanceID/scheduleID/date/timeIn/timeOut/dayType
    return hoursCalculated=false

POST /api/attendance (local mode only)
    if Firebase project configured: return 403 before reading the body/store
    reject all fields except employeeId/date/timeIn/timeOut
    parse ISO date and strict 24-hour HH:mm times
    find employee and pre-existing schedule mapping; never assign one
    construct AttendanceRecord with server-fixed dayType=DEMO
    synchronized store insertion rejects duplicate (scheduleID,date)
    model validation rejects timeIn > timeOut
    return 201; no hours/classification/pay output

GET /api/reports/attendance
    enumerate caller's synthetic employees and configured mappings
    count stored raw rows/date bounds for optional inclusive date range
    unmapped => hasScheduleMapping=false, count=0; do not call it absence

Browser
    raw table/calendar shows only loaded record dates/times
    two calendar clicks apply an inclusive selected range
    employee-linked summary joins raw row counts and stored workflow counts by ID
    no date without a row is classified
```

### 4.4 Leave and ticket demonstrations

```text
GET /api/workflows
    read only current sandbox's leaveRequests and tickets

POST /api/workflows/leave
    accept validated synthetic employee ID, leave type, ordered dates, optional bounded note
    derive employee name from current sandbox directory
    assign only the existing demonstration status
    save locally OR under current UID Firestore path

POST /api/workflows/tickets
    accept validated synthetic employee ID, documented ticket type, bounded summary
    derive employee name and documented queue label
    assign only existing demonstration status
    save locally OR under current UID Firestore path

POST /api/workflows/{leave|ticket}/{id}/cancel
    allow only the existing owner-side pending/open cancellation rule
    reject repeat/ineligible cancellation
    save locally OR under current UID Firestore path

UI
    search/status/type/date-overlap filters, selectable sorting, visible/all CSV
    detail view shows stored note/message and timestamps
    calendar displays submitted request dates only
    no reviewer notification, approve/reject, balance change, ticket resolution or payroll treatment
```

### 4.5 Reporting, search, and portable export

```text
attendance calendar = render raw rows by actual stored date; blank is not absence
employee detail = GET that employee's raw attendance + sandbox workflows; filter by exact employee ID
employee record report = join existing employee IDs to raw coverage + leave/ticket counts
cross-module search = GET employee/workflow/raw attendance data; search only listed stored fields
CSV export = selected visible/all stored rows; neutralize spreadsheet formula prefixes
JSON export = GET snapshot for current mode/user; mark syntheticOnly, hoursCalculated=false, payrollIncluded=false
```

### 4.6 Temporary statutory preview

```text
user enters synthetic monthly cash pay and explicit bases in browser
validate finite/nonnegative inputs and supported preview period
apply current versioned temporary rules in exact centavos
show employee deductions, employer contributions, monthly withholding, net estimate
block negative net; allow labelled preview CSV
make no API request; write nothing to local storage or Firestore
```

## 5. Full-build pseudocode — target behavior after decisions and implementation

The following is an implementation outline, not a claim that these functions already exist. `BLOCKED` means the product needs an approved rule, security boundary, or acceptance example before those lines can safely become code. The user’s latest message authorizes reusing prior submissions as **candidate source material**; it does not resolve conflicts between those submissions or turn their formulas into MotorPH policy automatically.

### 5.1 Authenticated storage promotion

```text
on every write:
    verify Firebase ID token server-side OR use authenticated Firebase SDK + rules
    derive uid from verified token, never trust a client-supplied employee/user relation
    load only users/{uid}/... records

attendance create (non-local capability requested, but not yet implemented or deployed):
    verify body allowlist
    verify employee exists in uid scope
    resolve schedule mapping from approved uid-owned schedule source
    begin transaction
        reserve unique key (uid, scheduleID, date)
        allocate durable attendance ID
        create raw row with allowed dayType from approved enum
    commit transaction or return conflict
    emit audit event only after approved schema/retention policy exists

never copy unauthenticated shared local process records into a Firebase user account automatically
```

**Technical requirement:** Firestore Security Rules cannot provide the same general transaction-safe composite uniqueness guarantee by a simple client check. A durable design should use a trusted backend/Cloud Function or a transaction with a deterministic uniqueness document/key, plus a reviewed rule set and emulator tests. No Firebase Console/rule publication is included in this code inventory.

### 5.2 Attendance and timesheet rules engine

```text
load approved schedule, timezone, pay period, holiday/day-type data
load raw punches within selected period
validate missing/duplicate/reversed/overnight punches
apply explicit grace, meal-break, rounding, overtime, rest-day, holiday, leave and correction rules
produce a trace for each input row and each calculated output
compare trace and totals to approved golden cases
```

**BLOCKED inputs:** source/import mechanism; schedule assignment and effective dates; time zone; meal/break handling; overnight shifts; missing and duplicate punches; grace and rounding; attendance/day-type enum; holidays/rest days; leave interaction; correction rights and audit history; pay-period boundaries. Folder-6 code normalizes 08:00–08:10 and caps at 17:00 but deducts no meal period, so a nominal 08:00–17:00 shift can become nine elapsed hours. Do not use it as a validated hour engine without resolving that defect. Folder-9’s eight-hours-per-workday pipeline is a different candidate and also needs an approved worked example.

### 5.3 Leave/ticket review and team/HR capabilities

```text
load approved role-to-action matrix and employee-to-user/team mapping
show request/ticket queue only to explicitly permitted roles
for each review action:
    verify caller role and record scope on the server
    validate allowed state transition and required reason
    write before/after event to approved audit structure
    update leave balance only by approved accrual/credit rules
    send notification only through configured service with retry/status handling
```

**BLOCKED inputs:** employee/HR/supervisor/payroll/admin role definitions; self/team/branch scope; manager assignment; review/cancel/reject semantics; leave type eligibility/accrual/carryover; balance timing; ticket routing/resolution; notification requirements; audit retention. Folder-9’s leave service demonstrates approve/reject and credit-counter updates but lacks its own authorization checks; other peer submissions use different role models. Do not copy its state transitions into live Firebase access without an approved matrix.

### 5.4 Payroll engine and payslip

```text
load versioned payroll profile and approved pay period
load effective-dated employee rate snapshot and permitted synthetic inputs
load approved attendance summary and eligible leave/holiday adjustments
calculate gross earnings by explicit component
calculate each statutory employee/employer item with effective date and basis
calculate tax for explicitly chosen period and approved taxable bases
apply cutoff/deduction allocation and rounding exactly as the approved profile states
validate invariants and golden examples; fail closed outside supported periods
persist immutable payroll run + input snapshot + ruleset version + trace
review/approve/reject using approved role matrix
render payslip/report from saved result; authorize read/download per employee/role
```

**BLOCKED inputs:** payroll input staging was previously deferred; salary-to-hourly conversion; cutoff dates; meal/hour rules; paid leave, holidays, overtime/undertime; allowance taxability; tax-period method; deduction allocation between cutoffs; annual adjustment; rounding; approval and correction/rerun rules; payslip privacy/distribution. Folder-9’s calculator structure can inform separation of components, but its old statutory rates are explicitly outdated in `docs/candidate-policy-review.md`. Folder-6’s cutoff rules are a distinct compatibility candidate and have the meal-break risk above. Current 2026 preview is not a substitute for this engine.

### 5.5 Release/operations

```text
freeze acceptance matrix and supported profile version
run domain + API + Firestore emulator allow/deny + browser + accessibility + mobile tests
exercise failure, retry, idempotency and duplicate-concurrency paths
verify exact preview origin and production configuration without sharing credentials
publish only reviewed rules/functions; capture deployment version and rollback path
monitor errors; document retention, backup/restore, incident process and support owner
```

These controls are not present in the local prototype and are required before describing a cloud deployment as production-ready.

## 6. Works / does not work / planned

### Works now

- Firebase email/password sign-in and per-user sandbox reads/writes for the existing employee allowlist and leave/ticket create/cancel paths; live localhost auth smoke passed. Exact Arena preview-origin sign-in remains unverified.
- Local synthetic employee/workflow/attendance process-memory stores; local raw attendance create for existing mappings only.
- Firebase raw attendance is GET-only; its POST returns `403`.
- Raw attendance list/calendar/date filters, inclusive two-date calendar selection, coverage report, employee-linked counts/details, CSV filters/exports, cross-module search, and read-only JSON export.
- Browser-only temporary 2026 statutory estimate using explicit fictional bases; no persistence or payroll API.
- Current regression evidence listed above.

### Does not work / is intentionally absent

- No live data, durable local storage, local-to-Firebase sync, JSON restore, Cloud Function, Firebase attendance write, or production upload.
- No schedule assignment, real clock-in/out, QR/site verification, attendance classification, hour/overtime/holiday/undertime calculation, or correction/delete workflow.
- No employee-to-account mapping, HR/supervisor/payroll role, team membership, approval/rejection, leave balance/accrual, ticket resolution/delivery, notification, or audit system.
- No payroll input staging, attendance-derived gross, cutoff allocation, saved payroll run, payslip, payroll report, payroll approval/distribution, or production tax guarantee.
- No deployment/monitoring/backup/restore or verified production/Arena Auth domain.

### Planned order

1. **Freeze requirements**: identify active rubric; choose required/optional/excluded features; approve explicit acceptance examples.
2. **Choose candidate profiles**: compare folder-6 and folder-9 behaviors against supplied official sources; record exact deviations; adopt one labeled classroom candidate only after choosing break, cutoff, tax, role, and review rules.
3. **Implement owner-scoped persistence**: Firestore transaction/Cloud Function attendance design, rules and emulator tests; no manual console changes without user approval.
4. **Implement/verify attendance engine**: schedule model, raw input source, meal/missing/overnight/correction cases, traceable totals and approved golden tests.
5. **Implement workflow authorization**: roles/employee binding, leave balances/decisions, ticket assignment/resolution, immutable audit and notifications only as accepted.
6. **Implement full payroll**: versioned rules, salary snapshot, inputs, gross-to-net, exact decimals, persisted run/payslip, approvals and reports from accepted examples.
7. **Release closure**: cloud/browser tests, accessibility, security/failure tests, origin verification, rollback/backup/ops evidence.

“Full finished build” is only complete after every selected acceptance item passes. Code from prior submissions can be adapted as a candidate, but no peer implementation is independently authoritative, and the previously reviewed folders contain conflicting workflow rules and outdated statutory formulas.

## 7. Key source and project documents

- `docs/api-contract.md` — observable API request/response contract.
- `docs/source-grounding-and-open-rules.md` — facts and unresolved source constraints.
- `docs/acceptance-baseline.md` — current testable behavior, draft status.
- `docs/candidate-policy-review.md` — peer-candidate formulas, statutory comparisons, known defects and conflicts.
- `docs/peer-parity-roadmap.md` — rough estimate and ordered gates.
- `docs/local-demo-inventory.md` — short local/Firebase mode summary.
- `docs/legacy-mo-it108-traceability.md` — legacy requirement mapping and differences.
- `README.md` — setup, current features, constraints and verification commands.
