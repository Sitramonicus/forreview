# Roadmap to full peer-level MotorPH feature parity

**How to read “100%”:** the nine submitted folders do not share one feature set, so there is no single objective peer-completion percentage. This roadmap uses the richest relevant comparison: the payroll/attendance/leave workflows in the stronger MO-IT110-style submissions (especially folders 6 and 9), plus the user's earlier MO-IT108 Requirements Analysis and System Design. It is a feature-completion target, not a grade.

**Current user constraints remain in force:** synthetic data only; no local-only or temporary product features. The earlier temporary 2026 statutory preview has been removed. Do not infer or connect attendance, pay-period allocation, API/Firestore, or payroll rules from peer code until the applicable rules are approved. No Firebase Console changes by the assistant. The legacy Terminal Assessment proposal contains template placeholders and is not treated as a detailed specification. `The Code.pdf` is unrelated C++ inventory work and is excluded.

## Practical progression and ownership

This is the shortest safe path from the current prototype to a complete **approved-scope course project**. It does not mean production payroll readiness or guessed business rules.

| Stage | Owner | Work and exit condition |
| --- | --- | --- |
| 1. Verification closure — in progress | Assistant | Chromium dependencies are installed. `npm run test:e2e` passes the local employee/attendance/workflow/reload/export/mobile flow, including reversed-date rejection, table filters, and simulated workflow authorization/service failures. The opt-in `npm run test:firebase-auth` smoke test passed disposable account creation/sign-in, user-scoped empty reads, and sign-out on localhost; its Auth account was deleted and it made no Firestore writes. Do not create more undeletable Firestore workflow test records. Remaining checks: preview-origin sign-in and actual Firebase/API failure states. User action only if the browser reports `auth/unauthorized-domain`: authorize that exact preview host. |
| 2. Freeze approved scope — draft in workspace | Assistant drafts; user approves unresolved choices | `docs/acceptance-baseline.md` maps current observable behavior and source/policy gates. It is not an approved rubric interpretation. No standalone active MO-IT110 grading rubric was identified among the supplied workspace files; required / optional / excluded labels still need the active rubric or user acceptance. Exit when the target peer-parity set is explicit. |
| 3. Implement policy-backed workflows | Assistant drafts candidate rules from supplied peers; user accepts/revises | The user has invited disclosed inference from submitted projects. `docs/candidate-policy-review.md` now records candidate leave approval/credit and role patterns, but the projects disagree and some omit server-side authorization. Implement a clearly labelled course-demo candidate only after documenting the selected role matrix and rules; no inferred role is published to Firebase until the boundaries are accepted. |
| 4. Payroll — calculation disabled | Assistant implements only after policy decisions and verified examples | No preview or calculator is shipped. The Payroll view reports blocking gates. Requires approved rates/bases/effective dates, period and rounding method, earnings/deductions, attendance rules, role matrix, durable run snapshots, correction/rerun behavior, and payslip access. |
| 5. Release and evidence | Assistant prepares; user accepts | Run Java/web tests, Firebase Rules Emulator or equivalent deny/allow tests, browser/accessibility/responsive checks, and failure-path checks. Produce traceability and demo steps. Exit when every selected requirement has evidence and the deployment is clearly labeled as a classroom prototype, not production. |

**What the user needs to do:** the updated Firestore rules now require publication before the demo seed can persist schedule-mapping documents; this slice did not change Firebase Console state. No credentials should be shared. The user has said there is no active rubric and has explicitly prohibited local-only and temporary product features. The read-only reporting additions are complete under the current boundaries. Reaching a higher broad-parity percentage requires a rubric or explicit policy authorization for attendance capture/calculation, leave/ticket review states, role boundaries, and payroll mechanics; do not infer those. Payroll input staging remains deferred.

## Progress snapshot (2026-11-04)

**Historical parity estimates below are stale and are not current completion evidence.** The present target is a persistent, non-temporary product; use the current status and verified test results in `docs/build-status-no-local-no-temp.md`. The new attendance calendar, employee-linked record views/counts, cross-module search, visible workflow exports, and synthetic sandbox bundle are read-only reporting surfaces, not the missing policy-backed workflows. The remaining gap is dominated by attendance calculations/capture permissions, leave/ticket review semantics, roles, and a full payroll/payslip path; these are outside the current authorization or underspecified. Payroll input staging remains deferred and excluded from both estimates.

- [x] Synthetic, user-scoped employee and workflow foundation; sortable read-only raw attendance; the earlier 2026 temporary preview has been removed; payroll calculation is disabled.
- [x] Browser-level leave/ticket workflows, status/cancel paths, table search/status filters and optional sorting (newest-first default), inclusive leave-request date-overlap filtering, newest-first history in both stores, all-record CSV export, responsive check, and a request-date calendar that drafts form dates without implying approval or submission.
- [x] Read-only raw attendance coverage report with inclusive date filtering, per-employee row counts/date bounds, explicit unmapped status, employee/name/ID/schedule search, mapped/unmapped/all filters, field/direction sorting, CSV export of visible rows, and no hours or absence classifications.
- [x] Removed the former process-memory-only attendance create path, UI, mutable interface, and API handler. `POST /api/attendance` now returns `405`; cloud capture awaits durable schedule/identity design.
- [x] Cross-mode read-only additions: raw attendance month calendar and two-date filter; employee-linked raw/workflow detail and searchable/mapped-filterable/sortable count-summary CSV; exact leave/ticket type filters with visible/all CSV; owner-sandbox JSON portability bundle; and cross-module search. These use only existing scoped GET APIs and stored values.
- [x] Overview counts now reflect loaded directory, selected raw attendance, and sandbox workflow records. Leave/ticket detail disclosures expose submitted content and UTC event timestamps without adding new workflow permissions.
- [x] HR directory table has client-side text/status filters, selectable field/direction sorting, and filtered CSV export; read-only Team roster adds independent search/status filters, selectable sorting, and filtered CSV export; Reports includes directory status/department and workflow status/type/queue label-count summaries, CSV export, and attendance-report drill-through to raw rows.
- [x] Draft `docs/acceptance-baseline.md` records current pass/fail behavior and gates from supplied sources; it remains provisional because no active rubric was found.
- [x] `docs/candidate-policy-review.md` compares the user's MO-IT107 schema, folder-6 and folder-9 calculations, official statutory sources, and a current net-pay library; outdated prior formulas and the temporary profile are disclosed.
- [x] Removed the former temporary 2026 preview modules and UI. Payroll now displays a policy gate and calculates nothing.
- [x] Automated local E2E error/recovery checks for simulated authorization denial and service outage across workflow and attendance-report views.
- [x] Verify Firebase Auth sign-out on localhost with an opt-in disposable-account test; the account was deleted and no Firestore writes were made.
- [ ] Verify authentication on the exact Arena preview origin and live Firebase/API deny/failure paths.
- [ ] Freeze the course-project acceptance scope against the supplied rubric and identify source-backed versus policy-gated features.
- [ ] Payroll input staging (deferred by user for a later phase; excluded from the current parity percentage).
- [ ] Implement the remaining durable policy-backed workflows and release evidence. No temporary calculator is to be reintroduced.

## Already done — current foundation

- Firebase Email/Password Auth and user-scoped Firestore access were live-tested with synthetic accounts. The sign-in page now wires Firebase password-reset email with non-enumerating confirmation text; reset delivery has not been live-tested.
- Employee list, search, and detail selection work; data remains fictional and scoped to the signed-in sandbox.
- Raw attendance records are selectable by employee and inclusive date range; default range spans that employee's earliest/latest available records, following the older Requirements Analysis. The register also displays a month calendar of loaded raw rows with two-date inclusive filter selection, never an absence classification. A separate read-only coverage report summarizes raw row counts and date bounds by configured employee-to-schedule mapping; unmapped is not reported as absent. Firebase attendance remains read-only.
- Payroll calculations are disabled. Attendance views are read-only and do not imply payroll periods.
- Leave request/ticket API routes now validate input, derive employee names from the user's synthetic directory, create records, show status, and allow only cancellation. A display-only personal request calendar shows pending/cancelled date ranges, not approved leave; two date clicks can draft form bounds without submitting. Client-side search/status filters narrow each workflow table, and the leave table additionally filters by inclusive overlap with a requested-date range; native detail disclosures show note/message and submission/cancellation timestamps; local and Firestore lists order newest first; exports remain the full owner-sandbox dataset. Firebase paths are owner-scoped and have no reviewer/approval/delete access; after publication, live smoke testing verified employee create/update, leave and ticket create/cancel/read-back, and cross-user denial. One cancelled leave and one cancelled ticket remain under deleted disposable UIDs because workflow deletion is denied. Local preview stores these records in Java process memory and loses them when the process restarts.
- Team roster is read-only with search/status filters, field/direction sorting, selected-record detail view, and filtered CSV export; the HR directory table also has client-side search/status filters, field/direction sorting, and filtered CSV export. Reports summarizes only stored synthetic directory status/department and workflow status/type/queue labels, with label-count CSV exports; workflow counts do not imply approvals or resolutions. The directory editor creates/updates only fictional name, department, role, location, and demo status. Raw attendance attribution reads persistent owner-scoped seed mappings; mappings are not editable. Firebase mode is user-scoped; the old unauthenticated process-memory store is now test-fixture-only; production requires Firebase. Employee detail now shows a read-only linked-record summary, and Reports provides owner-scoped cross-module search and a read-only JSON portability bundle. No team membership, HR authorization, delete, schedules, or lifecycle workflow exists.
- Verification: 51 Node.js workflow/calendar/filter/sort/summary/CSV-safety/search/workspace-export tests, 182 Java checks, loopback synthetic-fixture Playwright E2E, production build, and `npm audit` (0 vulnerabilities) pass. Local E2E covers employee create/edit and sort-ordered export, HR and Team roster filters/sorting/CSV, directory label counts, unscheduled attendance, raw attendance sort/export/calendar and two-date range selection, plus verification that attendance has no create form or write route, employee-linked raw/workflow detail and searchable/mapped-filtered count-summary CSV, workflow type filtering and visible/all CSV export, read-only sandbox JSON bundle and cross-module search (GET-only), coverage-report search/filter/sorting/export/drill-through/error recovery, workflow-label summary/export, leave/ticket submit/cancel, detail disclosures, and optional table sort controls, inclusive leave date-range filters, calendar draft-date selection/navigation, payroll configuration gate and absence of temporary calculator UI, simulated workflow authorization/service errors and recovery, reload persistence, CSV download, and mobile width. Earlier Firebase Auth smoke is historical. The latest slice did not re-run live Firebase; the new schedule-mapping rule has not been published or emulator-verified.

## Ordered tasks to reach the broader target

### 1. Freeze a traceable acceptance baseline

- Map the active MO-IT110 rubric to the legacy MO-IT108 requirement groups and only the peer features we actually intend to match.
- Mark every requirement as required, optional, or excluded; record source, owner, and acceptance example.
- Reconcile conflicting legacy sources (for example, old role/2FA workflows vs the current Firebase sandbox).
- Preserve the current boundary: synthetic records only; no local-only or temporary product features; payroll remains disabled until persistent rules are approved; no attendance-derived pay output or persisted payroll until those mechanics and rules are approved.

**Done when:** every target feature has a source and observable pass/fail acceptance criterion; proposals and template text are not presented as approved policy.

### 2. Complete the employee and organization foundation

- Define the approved synthetic employee, department/position/branch, schedule, and supervisor models.
- **Partially implemented:** owner-sandbox create/update/search for fictional directory fields only. Add audit/history, approved role limits, schedules, branch/position models, and lifecycle actions only when source requirements and authorization rules are supplied.
- Keep government identifiers and real personal data out of the demo sandbox.
- Decide how users map to employees and teams before moving beyond per-user isolated demo data.

**Done when:** synthetic employee lifecycle operations are validated, auditable, and limited by the approved role policy.

### 3. Finish attendance capture and summaries

- **Implemented within the user's explicit limit:** local in-memory raw time-in/time-out creation for existing mapped synthetic employees, fixed `DEMO` day type, unique schedule/date and time-order validation. Firebase attendance stays read-only; no schedule assignment, correction, or delete path was added.
- Add attendance correction flows only if required; distinguish remote/offsite from verified onsite clocking if matching the legacy design.
- Implement QR-based onsite verification only if it is an acceptance requirement; define expiry, replay protection, and device flow.
- Add schedule/day-type, grace-period, present/absent/tardy, leave, overtime, and undertime summaries only after each rule is specified.
- Define missing/duplicate punches, breaks, overnight shifts, time zone, rounding, corrections, and date boundaries.
- Keep user-selected dates inclusive and the earliest/latest default; never substitute an assumed payroll cutoff.

**Gate:** attendance classifications and hour totals require written rules and an approved worked example. Until then, keep raw entries only.

**Done when:** approved test cases cover normal, late, absent, leave, holiday/rest day, missing punch, duplicate, correction, and boundary scenarios.

### 4. Add leave management

- Define synthetic leave types, balances/accrual, eligibility, request/cancel states, and approval roles.
- Build employee requests, supervisor decisions, balance history, and employee/team leave calendars.
- Make approvals auditable and ensure only approved leave affects any later payroll input.

**Gate:** leave accrual, carry-over, monetization, and payroll treatment need an authoritative policy.

**Done when:** request, approve/reject/cancel, balance update, calendar, and audit tests pass.

### 5. Add ticketing and dispute workflows

- Define ticket types, fields, routing, queue position, statuses, cancellation rules, and resolution history.
- Implement overtime, leave, timesheet, payroll-dispute, HR/general inquiry, and technical-support forms only for in-scope roles.
- Notify requestors and handlers, and retain an audit trail.

**Done when:** each included ticket type has tested routing, authorization, state changes, and user-visible status.

### 6. Add team and supervisor workflows

- Define employee-to-team/branch membership and the supervisor's permitted scope.
- Build rosters, availability, schedule viewing/editing, pending-request review, and timesheet correction workflow.
- Record reasons and before/after values for every correction; notify affected employees.

**Done when:** supervisor actions are limited to assigned teams and are fully auditable.

### 7. Add HR onboarding/offboarding and employee administration

- Add synthetic onboarding, profile/branch/position changes, deactivation, clearance queues, and search/update screens if required.
- Define effective dates and the effect of mid-cycle changes without guessing payroll behavior.
- Ensure inactive accounts lose access according to an approved policy.

**Done when:** employee lifecycle changes are validated, role-limited, and tested against audit/history requirements.

### 8. Implement persistent payroll after policy approval — gated

- No payroll calculator or estimate is currently shipped. The previous temporary browser preview was removed.
- Before connecting it to attendance or employee records, decide salary conversion and pay-cycle boundaries, attendance/lunch/overtime/PTO/holiday/rest-day/undertime treatment, earnings and taxable/non-taxable benefits, and how monthly deductions/tax are allocated across paydays.
- Obtain independent worked expected-value cases, including edge cases; reconcile conflicting prior-project matrices against current government sources.
- When those inputs are approved, implement a versioned Java domain service using exact decimal arithmetic, durable input/output snapshots, ruleset trace, and golden tests.
- Test each statutory rule independently, then test gross-to-net, employer cost, cutoff allocation, and repeatability.

**Hard gate:** the user prohibits temporary features. No payroll estimate, attendance-derived pay, semi-monthly split, saved payroll, or payslip output is active until the missing mechanics and examples are approved and implemented persistently.

**Done when:** independent golden cases match each component and a repeat run retains a traceable ruleset version.

### 9. Add payroll supervision, adjustments, and approval

- Define rate/incentive/deduction adjustment scope (individual/team/branch/global), approvers, separation of duties, and effective dates.
- Build review/approve/reject flows with immutable audit events and controlled payroll reruns/corrections.
- Generate payroll summaries only from approved, versioned inputs.

**Gate:** depends on approved payroll formulas, values, roles, and correction policy.

### 10. Add payslips, reports, and distribution

- Define the required payslip fields and privacy rules.
- Generate the required output format, store a stable result reference, and verify read-back.
- Add payroll reports/monthly summaries only if required.
- Add email delivery and retry/status tracking only if required and a safe delivery service is configured.

**Gate:** no payslip values until the calculation engine is approved.

### 11. Replace demo isolation with approved identity, RBAC, and PII controls

- Define account-to-employee mapping, roles, team/branch scopes, session policy, account recovery, and 2FA requirements.
- Implement least-privilege authorization in backend and Firebase Rules; do not rely on UI hiding.
- If the older two-person PII approval requirement is adopted, implement explicit requests, two distinct authorized approvals, audit, and emergency policy.
- Add access logging, abuse limits, and tests for own/team/branch/unauthorized access.

**Done when:** an authorization matrix is implemented and every allow/deny path is covered by live or rules-emulator tests.

### 12. Production hosting and operational safety

- Replace the learning JDK server with an appropriately hardened deployment architecture; configure a stable domain, HTTPS, allowed origins, protected configuration, and environment-specific Firebase settings.
- Add monitoring, structured error handling, rate/abuse controls, backup/restore, and data-retention procedures.
- Load-test the approved scale target; the legacy document mentions 100 concurrent employee records, but the active target must be confirmed before treating that as a release criterion.
- Complete a security/privacy review before any real employee or payroll data is used.

**Done when:** deployment, recovery, performance, and security acceptance tests pass outside the development sandbox.

### 13. Final regression and submission evidence

- Run domain/unit, API integration, Firestore rules, browser E2E, accessibility/keyboard, mobile/tablet, and failure-path tests.
- Compare outputs to approved source examples, not just another team's implementation.
- Prepare requirement-to-code traceability, architecture/design artifacts, test results, and demo steps without exposing secrets or real data.

**Done when:** every required acceptance item is evidenced and no unapproved feature or calculation is enabled.

## Completion rule

Do not mark “100%” until all tasks selected in Step 1 have passed their acceptance criteria. Tasks 3–10 are broader than current Tasks 1–3; task 8 and all downstream payroll outputs remain blocked by missing rules and the user's current no-calculation constraint. The current prototype is a strong foundation, not yet a complete payroll system.
