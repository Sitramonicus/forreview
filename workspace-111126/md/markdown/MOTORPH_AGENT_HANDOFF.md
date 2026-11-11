# MotorPH Payroll Workspace — Complete Agent Handoff

**Prepared:** 4 November 2026  
**Companion archive:** `MotorPH_MS2_Local_Product.zip`  
**Purpose:** give a receiving coding agent the project purpose, history, boundaries, current runnable product, all current code, source syntheses, decisions, and test evidence in one Markdown document.

> **Provenance statement:** the runnable application in the companion archive is a locally reconstructed product based on the supplied LMArena conversation export, user-provided MO-IT107 submissions, prior feature synthesis, and project requirements. It is **not** a byte-for-byte clone, checkout, or verified export of `https://github.com/Sitramonicus/forreview`. The original workspace repository was not directly available as a source checkout in this execution. Do not claim otherwise.

## 0. Immediate instructions to the receiving agent

1. Read this entire handoff before changing code. The raw trace, skills compilation, PDFs, and previous syntheses are retained under `reference-materials/` in the ZIP.
2. Treat this package's source as the current runnable baseline. Preserve its Java 11 + HTML/CSS/JavaScript architecture unless the user explicitly directs a change.
3. The user has explicitly overridden an earlier overly cautious pause on payroll calculation. Do not disable or freeze the payroll module merely because earlier trace entries said to wait. The current package includes a replaceable **monthly 2026 statutory estimate** from explicit synthetic inputs.
4. Do not represent that estimate as a full payroll run, attendance-derived salary, semi-monthly payroll, payslip, or production payroll. Its current boundaries are specific and recorded below.
5. Make reversible assumptions, label them, and proceed. Do not turn source uncertainty into a blocker when a modest, clearly disclosed implementation is possible.
6. Do not import whole reference projects or their frameworks. Use them for feature comparison and independently implement the features appropriate to this project.
7. Never introduce real employee data, government identifiers, bank data, private credentials, API secrets, or Firebase service-account keys into the local demo or archive.
8. Run `./test.sh` (Windows: `test.bat`) after each meaningful change. Report browser E2E limitations honestly; do not claim a browser test passed unless it was actually run.

## 1. Product purpose and user intent

The product is a **MotorPH Payroll and Employee Management workspace** for coursework and local demonstration. It consolidates employee directory functions, raw attendance records, leave requests, help-desk tickets, team/HR views, reporting, and payroll-related calculations in one browser application.

The project context comes from MO-IT107 Information Management (database dictionary, logical database design, and data-flow diagrams), MO-IT110 Object-Oriented Programming work, the user's LMArena agent conversation, the OOP feature inventory, and selective comparison with peer repositories.

The user wants tangible software rather than endless requirement-gating. The instruction that must govern future work is: **make useful progress, do not invent facts, and distinguish confirmed source requirements from implementation assumptions.** The user corrected the previous assistant for carrying an old restriction forward after it had been overridden. That correction is current and intentional.

## 2. Source chronology, authority, and provenance

### 2.1 Primary supplied materials

| Source | Role in this project | Included path in ZIP |
|---|---|---|
| LMArena full trace export | Chronological discussion, implementation notes, source extracts, feature reviews, tests, and changing project state | `reference-materials/lmarena_fully_sanitized_agent_run_1793783074106.txt` |
| `skills_compilation.md` | Coding/review/process skill collection; relevant practices distilled in `AGENTS.md` | `reference-materials/skills_compilation.md` |
| MO-IT107 Data Dictionary PDF | Tables, columns, types, lengths, precision, constraints, and descriptions | `reference-materials/MO-IT107 Homework_ Data Dictionary for MotorPH Payroll  System Database H2101 Torres, J.D..pdf` |
| MO-IT107 Database Design PDF | Overview, ERD, table descriptions, keys, relationships, discrepancies | `reference-materials/MO-IT107 - 2nd Milestone_ Database Design for the MotorPH Payroll System  H2101 TORRES, J.D..pdf` |
| MO-IT107 DFD PDF | Context, Level 1, Level 2, payroll, employee management, and archive flows | `reference-materials/MO-IT107 Homework_ Data Flow Diagram of MotorPH Payroll System H2101 Torres, J.D..pdf` |
| Full PDF synthesis | Expanded transcription and explanation of the database and DFD diagrams | `reference-materials/MotorPH_Payroll_System_Full_Synthesis.md` |
| OOP feature inventory | Feature/subfeature and pseudocode inventory for the MO-IT110 OOP team source | `reference-materials/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md` |

The LMArena export warns that **24 conversation elements remained collapsed** during extraction. These include edited-file groups and a comprehensive report. Their contents may be missing. Do not infer their exact edits, code, or decisions. The full raw export is retained in the ZIP for search and chronology review.

### 2.2 GitHub repositories supplied by the user

- Workspace repo: <https://github.com/Sitramonicus/forreview>
- Support folders: <https://github.com/Sitramonicus/testingester>
- Additional comparisons:
  - <https://github.com/wasilolly/Employee-Payroll-with-laravel-framework>
  - <https://github.com/Shahzaib-Awann/Foodya-Restaurant>
  - <https://github.com/manuog1994/payroll-manager>
  - <https://github.com/Payroll-Engine/PayrollEngine>
  - <https://github.com/frappe/hrms>

The LMArena trace contains extracted repository reviews, selected files, comparative feature catalogues, and some upstream source snapshots. Some repositories had no recognised licence metadata in the captured snapshot. Feature concepts may inform this product; do not assume code reuse is licensed. Review licences and course rules before copying source.

### 2.3 Source priority

When details conflict, use this order:

1. The user's latest explicit instruction.
2. The current application source and tests in the ZIP.
3. The latest chronological LMArena trace material that can actually be read.
4. The user's own MO-IT107/110 materials and the prior syntheses.
5. Peer repositories as comparative evidence, not as binding specifications.
6. External official sources for current statutory details, clearly separated from the course PDFs.

### 2.4 Critical override

Earlier assistant-maintained notes said payroll calculations should remain disabled pending complete formula sources. The user later explicitly said they did not want to keep waiting and that the earlier mentality was blocking product delivery. The current project therefore includes a monthly 2026 statutory preview. This is a replaceable, explicitly provisional implementation, not permission to fabricate missing attendance/pay-cycle mechanics.

## 3. Current product snapshot

### 3.1 Technology

| Layer | Current local product |
|---|---|
| Browser | HTML, CSS, vanilla JavaScript |
| Server | Java 11-compatible JDK HTTP server (`com.sun.net.httpserver.HttpServer`) |
| Persistence | Local JSON file (`data/demo-data.json`) |
| Runtime dependencies | JDK only for the app; Node.js and Python are used by the test script |
| UI approach | Single-page navigation with independent module panels, forms, filters, tables, dialogs, and CSV output |
| Default bind | `127.0.0.1:8080` |

The original LMArena workspace was described as a Java backend and browser UI with Firebase Email/Password Authentication plus user-scoped Firestore REST storage. That mode is **not in this reconstructed package**. The local build is intentionally self-contained and uses JSON. Do not say Firebase is connected in this ZIP.

### 3.2 Current modules

| Module | Current implementation |
|---|---|
| Overview | Live counts for employee records, raw attendance, pending leave requests, and open tickets |
| Employees | Search, status filter, sorting, employee detail, synthetic create/edit, CSV export |
| Attendance | Raw records, employee selection, inclusive date filtering, default earliest/latest range, sorting, summaries, local-only raw record creation, CSV |
| Requests | Leave submit/cancel, request history, filters, date-overlap filter, calendar that drafts dates, details, ticket submit/cancel, CSV |
| Team | Read-only employee roster with search, status filter, sort, detail, and export |
| HR Desk | Synthetic employee create/edit, independent filters/sort; no complete HR lifecycle or RBAC |
| Reports | Employee label counts, raw attendance coverage, mapping filters, search, sort, date filtering, CSV, drill-through |
| Payroll | Monthly 2026 statutory estimate from five separately entered synthetic bases; employee/employer shares, BIR monthly withholding, net estimate, employer cost, preview-only CSV |

### 3.3 Synthetic records

The seed dataset contains fictional entries only:

- `DEMO-001` — Mara Rivera, People Operations / HR Coordinator, Makati, schedule 101.
- `DEMO-002` — Eli Santos, Finance / Payroll Associate, Taguig, schedule 102.
- `DEMO-003` — Noa Reyes, Operations / Operations Lead, Quezon City, schedule 103.

The data file is `data/demo-data.json`. `POST /api/demo/reset` restores the initial seed. Do not put real employee or payroll information in this file.

### 3.4 Scope status summary

**Working:** local Java service, browser shell, employee CRUD for allowlisted synthetic fields, raw attendance list/filter/create with validation, synthetic leave/ticket submission and cancellation, read-only Team view, HR directory edit view, directory and attendance reports, CSV exports, and a monthly statutory preview.

**Partial / not yet end-to-end:** employee lifecycle, shift/schedule assignment, attendance import and time calculations, absence/tardy/overtime classification, reviewer routing, leave approval/credits, ticket resolution, team membership/supervisor hierarchy, role permissions, audit history, payroll run, payslip, bank payout, remittance, production deployment.

The historical broad peer-feature-breadth estimate in the LMArena trace was approximately **61–62%** at the latest clearly identified 4 November 2026 snapshots. It was a subjective feature inventory estimate for a different Firebase-backed workspace state, not test coverage, a grade, production readiness, or a score for this reconstruction. Do not label this local ZIP “100% peer parity.”

## 4. Local setup and operation

### 4.1 Windows

1. Install JDK 11 or later.
2. Double-click `run.bat`.
3. Open <http://127.0.0.1:8080>.
4. Stop by closing the command window.

To run the test suite, double-click `test.bat` or run it from PowerShell.

### 4.2 Linux/macOS

```sh
chmod +x run.sh test.sh
./run.sh
```

Open <http://127.0.0.1:8080>. Test with:

```sh
./test.sh
```

### 4.3 Environment overrides

```sh
PORT=8182 MOTORPH_DATA_FILE=data/demo-data.json ./run.sh
```

PowerShell:

```powershell
$env:PORT = '8182'
$env:MOTORPH_DATA_FILE = 'data/demo-data.json'
.un.bat
```

The app binds to loopback by default. Do not expose it to a public network without a security review.

## 5. Application architecture and source map

```text
motorph-ms2-product/
├── AGENTS.md
├── README.md
├── MOTORPH_AGENT_HANDOFF.md
├── run.sh
├── run.bat
├── test.sh
├── test.bat
├── .gitignore
├── data/
│   └── demo-data.json
├── src/
│   ├── main/java/ph/motorph/ms2/
│   │   ├── Main.java
│   │   ├── ApiServer.java
│   │   ├── DemoStore.java
│   │   ├── Json.java
│   │   └── PayrollCalculator2026.java
│   └── test/java/ph/motorph/ms2/
│       └── SmokeTest.java
├── web/
│   ├── index.html
│   ├── styles.css
│   └── app.js
├── test/
│   └── check_html_bindings.py
├── docs/
│   ├── PROJECT_HANDOFF.md
│   ├── FEATURE_INVENTORY.md
│   ├── API_CONTRACT.md
│   ├── PAYROLL_PROFILE_2026.md
│   ├── SOURCE_GROUNDING.md
│   ├── TESTS_AND_VERIFICATION.md
│   └── REFERENCE_REPOSITORIES.md
└── reference-materials/
    ├── three MO-IT107 source PDFs
    ├── full database/DFD synthesis
    ├── OOP feature inventory/pseudocode
    ├── full LMArena trace export
    └── full skills compilation
```

### 5.1 Backend classes

- `Main.java`: reads port/data-file configuration, creates the store, starts the HTTP service.
- `ApiServer.java`: registers endpoints, parses requests, validates payloads, routes API calls, and serves static browser assets.
- `DemoStore.java`: owns seed records and local JSON persistence for employees, attendance, leave requests, and tickets.
- `Json.java`: local JSON parser/serializer; no third-party runtime library is required.
- `PayrollCalculator2026.java`: isolated `BigDecimal` calculation profile for the provisional monthly statutory estimate.
- `SmokeTest.java`: direct Java tests for serialization, payroll arithmetic, validation, and endpoint behaviour.

### 5.2 Frontend files

- `web/index.html`: screen structure, navigation, tables, forms, dialogs, and payroll preview input/output areas.
- `web/styles.css`: visual style and responsive layout.
- `web/app.js`: fetches API data, controls UI state, filters/sorts rows, renders dialogs/details, validates form interactions, creates CSV downloads, and runs the client-side payroll preview presentation.

The frontend should render user-controlled text via safe DOM text APIs. CSV exports must quote values and neutralise spreadsheet formula-leading values. Do not introduce unsafe interpolated `innerHTML` for arbitrary user values.

## 6. Data model and persistence

### 6.1 Employee

Core fields:

- `employeeId`
- `name`
- `department`
- `role`
- `location`
- `status`
- `scheduleId` (may be null)
- creation/update timestamps where present

The create/edit form does not accept salary, national government identifiers, bank details, password, or schedule ID. A newly created employee has `scheduleId: null` until a separate schedule assignment feature is designed.

### 6.2 Attendance

Core fields:

- `attendanceId`
- `scheduleId`
- `date`
- `timeIn`
- `timeOut`
- `dayType`
- source/timestamp fields when present

Validation requires an existing employee and a known schedule mapping, valid date/time values, time-in not later than time-out, and uniqueness of `(scheduleId, date)`. Overnight shifts are not supported. Reads remain raw: they do not return worked hours or payable hours.

### 6.3 Leave requests

Core fields:

- `requestId`
- `employeeId`
- `employeeName`
- `leaveType`
- `startDate`
- `endDate`
- `note`
- `status`
- `createdAt`, `updatedAt`, and cancellation timestamp when present

New requests are pending. The owner can cancel a pending request. There is no reviewer approval, leave credit debit, accrual, monetisation, or notification workflow.

### 6.4 Tickets

Core fields:

- `ticketId`
- `employeeId`
- `employeeName`
- `ticketType`
- `summary`
- `queueLabel`
- `status`
- creation/update/cancel timestamps

Queue labels are demonstrative routing labels only. They are not actual assignment, delivery, notification, or reviewer authority.

### 6.5 Persistence behaviour

The local JSON store writes to `data/demo-data.json` by default. The store seeds a fictional initial dataset when the configured path does not exist. Updates use a temporary write followed by replacement where supported. The reset route is destructive to local demo changes and restores the seed.

## 7. HTTP API contract

Base URL: `http://127.0.0.1:8080` by default.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Mode, storage, payroll profile, and production-payroll marker |
| GET | `/api/employees` | Employee list; supports query/status/department filters |
| GET | `/api/employees/{employeeId}` | Employee detail |
| POST | `/api/employees` | Create synthetic employee |
| PATCH/PUT | `/api/employees/{employeeId}` | Update allowlisted employee fields |
| GET | `/api/attendance?employeeId=&from=&to=` | Read raw attendance; optional employee filter and inclusive dates |
| POST | `/api/attendance` | Create local synthetic raw attendance entry |
| GET | `/api/reports/attendance?from=&to=` | Raw coverage report per employee |
| GET | `/api/workflows` | Leave request and ticket history |
| POST | `/api/leave-requests` | Submit pending leave request |
| POST | `/api/leave-requests/{id}/cancel` | Cancel own pending request in the local demo |
| POST | `/api/tickets` | Submit synthetic ticket |
| POST | `/api/tickets/{id}/cancel` | Cancel open synthetic ticket |
| GET | `/api/team` | Read-only roster |
| GET | `/api/hr/employees` | HR directory listing |
| GET | `/api/payroll/rules` | Provisional monthly statutory profile metadata |
| POST | `/api/payroll/preview` | Calculate non-persisted monthly preview |
| POST | `/api/demo/reset` | Restore demo seed |

All API routes and example JSON request/response bodies are documented in `docs/API_CONTRACT.md`, which is embedded in this handoff and present separately in the ZIP.

## 8. Payroll calculation profile

**Profile ID:** `PH-STATUTORY-2026-MONTHLY-PROVISIONAL`  
**Period:** monthly  
**Currency:** PHP  
**Persistence:** none  
**Role:** educational/demo estimate from explicit fictional inputs.

Five amounts are required independently:

- `cashPay`
- `sssCompensation`
- `philHealthMbs`
- `pagibigFundSalary`
- `taxableCompensation`

This separation avoids silently assuming that agency compensation bases are identical.

### 8.1 SSS

- Regular SS rate: 15% total, split 5% employee and 10% employer.
- Applied MSC: minimum ₱5,000, maximum ₱35,000, ₱500 increments according to the January 2025 schedule.
- MSC above ₱20,000 is the MPF/MySSS Pension Booster component.
- Employer EC: ₱10 at MSC ₱14,500 and below, ₱30 from MSC ₱15,000 and above.

### 8.2 PhilHealth

- 5% rate.
- MBS floor ₱10,000, ceiling ₱100,000.
- Equal employee/employer split.
- The basis is separately supplied rather than inferred from cash pay.

### 8.3 Pag-IBIG

- Maximum Fund Salary ₱10,000.
- Employee share 1% at Fund Salary of ₱1,500 and below, otherwise 2%.
- Employer share 2%.

### 8.4 BIR withholding

The calculator uses the monthly Annex E table:

| Monthly taxable compensation | Tax |
|---|---|
| ₱20,833 and below | ₱0 |
| ₱20,834–₱33,332 | 15% of excess over ₱20,833 |
| ₱33,333–₱66,666 | ₱1,875 + 20% of excess over ₱33,333 |
| ₱66,667–₱166,666 | ₱8,541.80 + 25% of excess over ₱66,667 |
| ₱166,667–₱666,666 | ₱33,541.80 + 30% of excess over ₱166,667 |
| ₱666,667 and above | ₱183,541.80 + 35% of excess over ₱666,667 |

The implementation compares centavo values against the next whole-peso bracket lower bound (for example, `< ₱33,333`) so ₱33,332.50 remains in the lower band.

The current preview subtracts employee SSS, PhilHealth, and Pag-IBIG shares from the explicit taxable compensation before applying monthly withholding. This is a documented implementation assumption. Annual reconciliation, all taxable/exempt benefit classifications, and full withholding adjustment rules are outside this module.

### 8.5 Rounding and sample

- Java `BigDecimal` for all monetary arithmetic.
- Two-decimal `HALF_UP` rounding.
- Negative bases are rejected; negative resulting net is rejected.
- Net estimate = cash pay − employee SSS − employee PhilHealth − employee Pag-IBIG − BIR monthly withholding.
- Employer cost estimate = cash pay + employer SSS + employer EC + employer PhilHealth + employer Pag-IBIG.

For the illustrative case where all five input bases equal ₱25,000:

| Result | Amount |
|---|---:|
| SSS employee | ₱1,250.00 |
| SSS employer SS/MPF | ₱2,500.00 |
| SSS employer EC | ₱30.00 |
| PhilHealth employee | ₱625.00 |
| PhilHealth employer | ₱625.00 |
| Pag-IBIG employee | ₱200.00 |
| Pag-IBIG employer | ₱200.00 |
| Employee statutory shares before tax | ₱2,075.00 |
| Taxable base after employee statutory shares | ₱22,925.00 |
| BIR monthly withholding | ₱313.80 |
| Estimated net | **₱22,611.20** |
| Employer statutory cost | ₱3,355.00 |
| Employer cost estimate | **₱28,355.00** |

### 8.6 Excluded from payroll

No attendance-derived wage, salary conversion, meal-break/time-hour derivation, tardiness, undertime, overtime, night differential, holiday/rest-day premiums, leave pay/accrual/monetisation, bonuses, commissions, loans, advances, other deductions, semi-monthly cutoff splitting, deduction allocation by cutoff, saved payroll run, posting, bank payout, remittance, annual tax reconciliation, or payslip is implemented.

The preview is active. The full payroll lifecycle is unfinished.

### 8.7 Statutory source links

The current profile document records these sources as checked 4 November 2026:

1. SSS Pay Contributions: <https://www.sss.gov.ph/pay-contribution/>.
2. SSS Circular No. 2024-006 / contribution schedule effective January 2025: <https://www.sss.gov.ph/wp-content/uploads/2024/12/2025-SSS-Contribution-Table-rev.pdf>.
3. PhilHealth Advisory No. 2025-0002: <https://www.philhealth.gov.ph/advisories/2025/PA2025-0002.pdf>.
4. Philippine Information Agency report, 14 May 2026, PhilHealth 5% premium rate for 2026: <https://pia.gov.ph/news/philhealth-sets-5-premium-contribution-rate-for-2026/>.
5. Pag-IBIG Fund Circular No. 460: <https://www.pagibigfund.gov.ph/documents/Circular%20No.%20460%20-%20Guidelines%20on%20the%20Pag-IBIG%20Fund's%20Implementation%20of%20Increase%20in%20the%20MFS%20Effective%20February%202024.pdf>.
6. BIR Annex E revised withholding tax table: <https://bir-cdn.bir.gov.ph/local/pdf/Annex%20E%20RR%2011-2018.pdf>.
7. BIR Revenue Regulations No. 11-2018: <https://bir-cdn.bir.gov.ph/local/pdf/RR%20No.%2011-2018.pdf>.

For changes to legal values, verify the current official publication date and effective date again. Do not rely on an old peer project's constants.

## 9. Course database and process design context

The MO-IT107 Data Dictionary groups its database tables into five areas:

- Employee Details: Employee, Address, Department, Position, Supervisor, Schedule, Leave Balance.
- Logging: Changelog, Attendance, Leave, Leave Accrual History.
- Government Details: Government, Government Deductibles, Contributions.
- Payroll: Payroll, Payslip, Leave Monetisation, Earnings, Deductibles, Other Transactions, Government Tally, Summary.
- Static-Update Only: Regions, Cities, Holiday, Months, Rates, Tax Table, Leave Type.

The MO-IT107 database design also has a separate `Rest Day` entity. Preserve the discrepancy between that ERD and the dictionary's `Schedule.restDay` SET field; do not silently reconcile it.

### 9.1 DFD major processes

- Context level: Employee, Supervisor, Human Resources, Payroll Staff, and System Admin interact with the MotorPH Payroll System.
- Level 1: Employee Management, Payroll Processing, and Archival interact with employee repository, logs, payroll, and cold storage.
- Employee management Level 2: Employee Information Handling, Schedule and Time Tracking, Government Info Access; stores include Employee Address, Job Profile, Personally Identifiable Information, Attendance, Leave, and Changelog.
- Payroll Level 2: Timesheet Validation, Leave Monetisation Process, Wage and Adjustments, Government Deductibles, Basic Payroll Computation, Payslip Generation & Distribution, Monthly Summary Compilation; stores include Attendance Logs, Leave Logs, Leave Monetisation Logs, Wage, Benefits & Deductibles, Tax Table & Rates, Payslip, and Monthly Summary Report.
- Archival Level 2: Annual record appending, cold storage, and deletion/removal of selected records.

The original DFD names two supporting source documents not included in the three supplied PDFs: the Business Requirement Specification Document for Payroll System and MotorPH Existing Business Processes - Payroll. Their contents are not represented as reviewed.

### 9.2 Known database-source discrepancies

- Schedule rest days: SET field in dictionary vs separate `Rest Day` table and `restDayID` in ERD.
- Employee department/government references differ between dictionary, table detail, and diagram representations.
- Supervisor fields and Employee supervisor reference are not consistently represented.
- Rates/Tax Table have lookup descriptions but no fully specified effective dates, full formulas, rounding, tax period, or worked examples.
- Monthly Government Deductibles key omits year component in the drawing/design context.

The local demo intentionally implements a smaller operational data subset; it is not a full implementation of every database entity.

## 10. Feature implementation inventory

| Area | Implemented | Partial / not implemented |
|---|---|---|
| Overview/navigation | Local navigation and live counts | No admin dashboard/production telemetry |
| Employee directory | Search, filters, sorting, details, synthetic create/edit, CSV | No schedule assignment, government IDs, salary records, delete, audit, employee lifecycle |
| Attendance | Raw records, inclusive date filter, summary, sort, local record creation, duplicate prevention, export | No import, live clock, work hours, break policy, late/OT/absence classification, correction approval |
| Leave | Submit/cancel pending, filters, date-range overlap, calendar draft, details, export | No balances, accrual, approval/rejection, reviewer roles, payroll treatment, notifications |
| Tickets | Submit/cancel open, queue label, history/details/filters/export | No real assignment/delivery, resolve/reopen, SLA, notification |
| Team | Read-only roster and views | No team membership, supervisor tree, schedule/team assignment, supervisor scope |
| HR Desk | Basic synthetic employee create/edit, filters/sort | No onboarding/offboarding/clearance, HR roles, audit, full job/position management |
| Reports | Directory labels/status/department, raw attendance coverage report/drill-through/export | No payroll register, payslip, government remittance, annual report, retention archive |
| Payroll | Monthly statutory preview with separate bases and preview CSV | No attendance-to-gross, overtime, pay cycle, cutoff allocation, persisted run, payslip, payout/remittance |
| Data/access | Local JSON storage, server validation, loopback binding | No Firebase connection in this ZIP, production RBAC, audit trail, backups, security review |

See `docs/FEATURE_INVENTORY.md` for the detailed item-by-item status matrix.

## 11. Test and verification evidence

### 11.1 Test script

`./test.sh` / `test.bat` performs:

1. Compile Java source and tests with `javac --release 11`.
2. Run `ph.motorph.ms2.SmokeTest`.
3. Run `node --check web/app.js`.
4. Run `python test/check_html_bindings.py`.

Latest verified output from this package:

```text
PASS: 36 MotorPH checks
PASS: browser JavaScript syntax check
PASS: 122 unique HTML IDs; 101 literal JavaScript bindings resolve.
```

The 36 Java checks cover JSON parsing/serialisation, decimal behaviour, payroll calculation and statutory shares, SSS band boundaries, BIR whole-peso and fractional-centavo boundaries, negative input/net validation, health/employee/attendance/workflow/report/payroll routes, attendance duplicate prevention, cancellation, and error handling.

### 11.2 Local HTTP smoke

A separate process on port 8183, using a temporary JSON file, passed:

- `GET /api/health` → HTTP 200; `LOCAL_SYNTHETIC`, `local-json`, `livePayroll: false`.
- `GET /api/employees` → HTTP 200; 3 synthetic employees.
- `GET /api/attendance?employeeId=DEMO-001` → HTTP 200; 3 raw rows, `hoursCalculated: false`, `payrollIncluded: false`.
- `GET /` → HTTP 200 HTML.
- `GET /app.js` → HTTP 200 JavaScript.

The temporary process and its temporary data file were stopped/removed after the checks.

### 11.3 Browser end-to-end limitation

A Playwright/Chromium attempt was blocked by the execution environment (`net::ERR_BLOCKED_BY_ADMINISTRATOR` when navigating to the local browser app). Therefore this reconstruction does **not** claim a full browser end-to-end test passed in this session. The LMArena trace does record historical E2E passes in the original workspace environment; those are historical evidence, not a rerun against this package.

### 11.4 Interpretation

Passing the local tests confirms only the exercised paths. It does not prove legal payroll compliance, production security, complete workflow correctness, Firebase integration, browser accessibility, or peer parity.

## 12. Open work and next development order

Continue product work; do not revert to a frozen state. The following order minimises dependency clashes.

1. **Schedule and attendance model:** add schedule assignment, shift definitions, rest-day/holiday calendar, attendance import/source, and explicit policy for breaks/overnight shifts. Keep unmapped attendance distinct from absence.
2. **Work-hour calculations:** derive only after schedule and attendance policies exist; add correction flow and test cases for missing punches, overlaps, overnight spans, breaks, holidays, and rest days.
3. **Leave lifecycle:** leave balances, accrual rules, request approval/rejection, reviewer identity/authority, credit deductions, cancellation/reversal, and calendar statuses. Keep the current request demo as baseline.
4. **Ticket workflow:** real queue assignment, authorised review, response/resolution/reopen, event history, and user-visible notifications/status.
5. **HR/team lifecycle:** department/position/supervisor references, onboarding/offboarding, schedule assignments, team membership, authority boundaries, and audit history.
6. **Payroll run:** connect validated work-time inputs to salary bases, premiums, leave pay, other earnings/deductions, pay periods, deduction allocation, annual tax treatment, result records, approvals, payslip, and remittance reports. Retain the statutory profile as separately versioned calculation code.
7. **Security and persistence:** if Firebase is restored, use Firebase Auth ID tokens and user-scoped access; define roles and record-level authorisation; test allow/deny paths; never add admin credentials to the browser.
8. **Final product QA:** full browser E2E, 320px to desktop responsive checks, keyboard/accessibility review, malformed-input and error-recovery tests, persistence/reload checks, export checks, and dependency/licence review.

Do not use the historical peer-parity percentage as an automatic target for arbitrary feature additions. Maintain a traceability matrix from each proposed feature to a course source, peer evidence, or clearly marked product inference. A 100% feature claim requires a defined comparison set and observable acceptance criteria.

## 13. Process and coding preferences distilled from `skills_compilation.md`

The compilation is preserved in full in the ZIP. Relevant practices for this project:

- Inspect the current source and relevant source materials before asking the user to repeat information.
- Think before coding; surface true uncertainty and assumptions.
- Simplicity first; implement only useful scope.
- Make surgical edits; do not refactor unrelated code.
- Define observable acceptance criteria and verify them.
- Work in coherent bounded steps; state exact failures rather than vague blockers.
- Keep the UI accessible and text clear; the user prefers focused, actionable presentation.
- Do not force brainstorming/approval gates for routine implementation once the user has authorised reasonable best-effort decisions.
- Do not treat every skill in the compilation as governing policy; it includes large catalogues and unrelated platform-specific material. Apply only relevant general coding, testing, and review guidance.

## 14. Reference inputs inside the companion ZIP

The ZIP retains the full raw LMArena text export and full `skills_compilation.md`, plus all three original PDFs, prior full PDF synthesis, and full OOP feature/pseudocode inventory. The two large raw inputs are not duplicated in the body of this Markdown because they are many megabytes and already exist as exact files beside it in the ZIP. Their filenames and paths above are the source-of-truth handoff pointers. All current product source code and project documentation are embedded below.

---

# Appendix A — Current project documentation (full text)


## Document: `README.md`

# MotorPH Payroll Workspace — Local Reconstruction

A runnable browser-based MotorPH employee/payroll workspace reconstructed from the supplied LMArena agent trace, the user's MO-IT107 database/DFD submissions, and the project handoff context.

> **Important provenance:** this folder is a new local reconstruction, not a byte-for-byte clone or Git checkout of `https://github.com/Sitramonicus/forreview`. The GitHub workspace source itself was not available in this execution environment. The LMArena export captures substantial source snippets, API behaviour, tests, feature notes, and historical workspace state, which were used to reproduce the application locally. Do not describe this package as the exact original repository.

## Run locally

### Windows

1. Install JDK 11 or later.
2. Double-click `run.bat`.
3. Open <http://127.0.0.1:8080>.

To stop the server, close the command window.

### macOS / Linux

```sh
chmod +x run.sh test.sh
./run.sh
```

Open <http://127.0.0.1:8080>.

The port and local data file can be changed with environment variables:

```sh
PORT=8182 MOTORPH_DATA_FILE=data/demo-data.json ./run.sh
```

On Windows PowerShell:

```powershell
$env:PORT = '8182'
$env:MOTORPH_DATA_FILE = 'data/demo-data.json'
.\run.bat
```

The server binds to `127.0.0.1` by default. This is deliberate: the reconstructed local build should not be exposed to a network without a security review.

## Test

Windows:

```bat
test.bat
```

macOS / Linux:

```sh
./test.sh
```

The test command compiles the Java sources with `--release 11`, runs 36 Java JSON/payroll/API smoke checks, checks JavaScript syntax, and verifies 122 unique HTML IDs against 101 literal JavaScript element references.

## Included modules

- **Overview:** employee, raw attendance, pending leave, and open-ticket counts.
- **Employees:** searchable directory, status filter, sorting, detail view, synthetic create/edit, and CSV export.
- **Attendance:** raw time-in/time-out records, employee selection, inclusive date range, default earliest/latest record range, sorting, summary counts/date bounds, and local-only synthetic record creation.
- **Requests:** leave request submission/cancellation, inclusive date-overlap filtering, calendar date drafting, and ticket submission/cancellation. No approval or review role is simulated.
- **Team:** read-only roster with search, status filter, sorting, and CSV export.
- **HR Desk:** basic synthetic employee create/edit and directory filtering. It is not a full employee lifecycle module.
- **Reports:** employee status/department label counts and a raw-attendance coverage report with mapping filters, search, sort, date filtering, CSV export, and drill-through.
- **Payroll:** a version-labelled 2026 monthly statutory estimate from five explicit monetary bases. The calculation is real and testable, but it is **not** a payroll run, payslip, attendance-derived wage calculation, or production payroll service.

## Data and privacy

- All seeded records are fictional.
- Data is saved in `data/demo-data.json`, which the Java service creates on first launch if absent.
- Do not enter real staff records, real government identifiers, bank details, passwords, or real salary data.
- `Reset synthetic data` restores the three fictional employee records and their sample attendance rows.
- No Firebase credentials, service-account keys, or real employee data are included.
- This package runs in **local JSON mode**. The LMArena trace describes a separate Firebase-authenticated, user-scoped Firestore mode in the original workspace; that integration has not been reproduced in this local reconstruction.

## Payroll limitations

The payroll screen applies a monthly statutory estimate profile to explicitly entered bases:

- cash pay;
- SSS compensation;
- PhilHealth monthly basic salary;
- Pag-IBIG Fund Salary;
- taxable compensation before employee statutory deductions.

These bases are kept separate. The app does not assume that every agency uses the same compensation definition. It calculates employee and employer shares separately, applies monthly BIR withholding, and reports estimated net cash after the listed employee deductions.

The app does **not** calculate work hours, tardiness, undertime, overtime, holiday premiums, rest-day premiums, leave pay, salary conversion, semi-monthly cut-offs, deduction allocation between cut-offs, annual tax adjustments, or stored payroll runs. It does not produce a payslip. The rate profile and source links are described in `docs/PAYROLL_PROFILE_2026.md`.

## Project documents

- `AGENTS.md` — instructions for a future coding agent.
- `docs/PROJECT_HANDOFF.md` — product purpose, history, architecture, decisions, current scope, and unfinished work.
- `docs/FEATURE_INVENTORY.md` — module-by-module implementation status.
- `docs/API_CONTRACT.md` — current local HTTP endpoints and data shapes.
- `docs/PAYROLL_PROFILE_2026.md` — formula notes, sources, assumptions, and a worked example.
- `docs/SOURCE_GROUNDING.md` — source order, course PDF coverage, and provenance boundaries.
- `docs/TESTS_AND_VERIFICATION.md` — verification that has actually been run in this package.
- `docs/REFERENCE_REPOSITORIES.md` — how the comparison projects informed feature selection, without wholesale code copying.
- `reference-materials/` — supplied PDFs, earlier synthesis, feature pseudocode inventory, LMArena trace export, and compiled skills.
- `MOTORPH_AGENT_HANDOFF.md` in this folder is the consolidated source-and-code Markdown intended for another agent.

## Current development status

This is a useful local demo and a reconstruction aid, not 100% peer parity and not production-ready. The current feature gaps are listed in `docs/FEATURE_INVENTORY.md`. The workspace trace's latest historical peer-parity estimate was approximately 61% on 4 November 2026; that was a subjective feature-breadth estimate for a different, Firebase-backed workspace snapshot, not a measurement of this reconstruction.


## Document: `AGENTS.md`

# Instructions for the next agent

## Primary objective

Continue the MotorPH Payroll Workspace from the existing source. Preserve its local Java + browser architecture unless the user explicitly changes it. The goal is a useful, testable coursework product with clear provenance, not a production HRIS rebuild.

## Read before editing

1. `docs/PROJECT_HANDOFF.md`
2. `docs/FEATURE_INVENTORY.md`
3. `docs/API_CONTRACT.md`
4. `docs/SOURCE_GROUNDING.md`
5. `docs/PAYROLL_PROFILE_2026.md`
6. `reference-materials/` for the original user materials, trace, and skill compilation.

The LMArena agent export is chronological and has 24 collapsed elements whose content was not captured. Do not treat the missing elements as evidence of decisions.

## User's current instruction

The user explicitly corrected an earlier overly cautious response. Do not reapply an older instruction to freeze all payroll calculation or wait for perfect source completeness. Make reasonable, reversible decisions, disclose assumptions, and deliver working increments. Do not silently represent assumptions as source facts.

The user wants actual files and a runnable ZIP plus a full Markdown handoff. Avoid asking for context already present in the handoff or source material.

## Engineering rules

- Keep the UI browser-based and the backend Java unless there is an explicit change request.
- Target Java 11 language/runtime compatibility (`javac --release 11`).
- Keep dependencies small. There is no Maven/Gradle/npm requirement in the local reconstruction.
- Preserve the local JSON demo mode and synthetic data. Never add private credentials or real personnel data to the repository or handoff.
- Make surgical changes. Do not rebuild adjacent modules unnecessarily.
- Avoid speculative features, over-engineering, and advanced patterns that do not contribute to the course scope.
- If an outside workflow convention is inferred from a peer repository, label it as a candidate/demo rule in docs and code comments.
- Do not treat absence of an attendance row as an absence from work.
- Do not infer supervisor, reviewer, HR, or payroll authority from a job title or queue label.
- Do not silently merge the user's MO-IT107 data dictionary and ERD where their entity/column designs disagree. Keep the discrepancy register explicit.
- Payroll formulas must stay isolated and version-labelled. Do not use the temporary monthly calculator as a reason to claim full payroll, cut-off allocation, or payslip processing is complete.
- For monetary arithmetic, prefer `BigDecimal` and explicit rounding. Do not replace with binary floating-point arithmetic.
- Keep text insertion safe (`textContent`, not interpolated `innerHTML` for user-entered values).
- CSV must quote fields and neutralise formula-leading values before export.

## Verification before completion

Run:

```sh
./test.sh
```

On Windows:

```bat
test.bat
```

For the running app, check `/api/health`, `/api/employees`, `/api/attendance`, `/api/workflows`, `/api/reports/attendance`, and `/api/payroll/preview` with synthetic data. If browser E2E is unavailable, state exactly why and do not claim it passed.

## Product boundaries

The original LMArena workspace is described as Firebase Auth + user-scoped Firestore REST. This reconstructed local package intentionally uses a local JSON file. Do not claim that the Firebase integration is present here. Do not add service-account/Admin credentials to the client or package. If Firebase is restored, use the caller's ID token and explicit per-user rules; test deny paths as well as allow paths.


## Document: `docs/PROJECT_HANDOFF.md`

# MotorPH Payroll Workspace — Project Handoff

**Prepared:** 4 November 2026  
**Package:** local Java 11+ backend + HTML/CSS/JavaScript frontend  
**Operating mode:** synthetic local demo data  
**Provenance:** reconstructed from supplied project context and source extracts; not a byte-for-byte copy of the private GitHub workspace.

## 1. Product purpose

The project is a MotorPH employee and payroll workspace. Its purpose is to bring employee directory operations, attendance records, leave and ticket submissions, reporting, and payroll-related calculations into one browser application.

The course context spans the user's MO-IT107 information-management work (data dictionary, database design, and DFD) and the later MO-IT110 OOP/software-development work. The product is informed by the user's course schema, the OOP team implementation inventory, the LMArena agent's work history, and selected feature patterns from peer projects.

The user has emphasised that the application should become a tangible, testable product. The user overrode an earlier overly cautious instruction that had been used to keep every payroll calculation disabled. The current approach is to implement a clearly labelled monthly statutory estimate, expose assumptions, and keep unimplemented payroll mechanics visible rather than silently withholding the whole module.

## 2. Provenance and source hierarchy

The current local project is a reconstruction, not a direct clone:

- The workspace URL is `https://github.com/Sitramonicus/forreview`.
- The LMArena trace describes a workspace folder named `motorph-ms2`, with browser UI, Java backend, Firebase Auth, and user-scoped Firestore access.
- The raw GitHub workspace source could not be pulled into this execution environment. The reconstruction therefore uses detailed conversation trace evidence, captured API routes, source snippets, test output, and user-provided course materials.
- The LMArena export warns that 24 elements were collapsed during extraction, including edit groups and a comprehensive report. Their missing contents are not assumed.

Source precedence for later work:

1. The user's latest explicit instructions.
2. The actual current project files and tests in this package.
3. The latest chronological project state and direct source excerpts in `reference-materials/lmarena_fully_sanitized_agent_run_1793783074106.txt`.
4. The user's MO-IT107/110 course submissions and earlier syntheses.
5. Peer repositories, used for comparative feature ideas only.
6. External official references for current statutory values, clearly separated from coursework source content.

A prior assistant response quoted an older state in which payroll calculations were blocked. That is superseded by the user's later instruction. Do not resurrect it.

## 3. Technology and architecture

### Frontend

- `web/index.html`: one-page application shell, navigation, module views, forms, tables, dialogs, and payroll preview.
- `web/styles.css`: responsive office-software visual style, with system font fallbacks and no required runtime design framework.
- `web/app.js`: browser state, API calls, filtering, sorting, calendar, dialogs, table rendering, CSV export, and payroll-result presentation.

### Backend

- Java 11-compatible source using the JDK `com.sun.net.httpserver.HttpServer`.
- No framework is required.
- `ApiServer` routes HTTP requests and serves static frontend assets.
- `DemoStore` manages synthetic employee, attendance, leave, and ticket records.
- `Json` provides JSON parsing and serialisation without a third-party runtime library.
- `PayrollCalculator2026` performs the isolated monthly statutory estimate using `BigDecimal`.

### Persistence

The local package writes to `data/demo-data.json` by default. The data store creates an initial synthetic dataset when the file does not exist, and writes updates through a temporary file followed by atomic replacement where supported.

The original LMArena workspace was described as a Firebase-authenticated version:

- Email/password Firebase Authentication.
- Java service calling the Firestore REST API with the signed-in user's ID token.
- User-scoped paths such as `users/{uid}/demoEmployees`, `demoAttendance`, `demoLeaveRequests`, and `demoTickets`.
- Firestore Security Rules rather than a privileged server credential.

That Firebase integration is **not present in this ZIP**. The current package is local JSON mode, with no credential or private key included. The handoff must not claim otherwise.

## 4. Current modules

### Overview

Shows live local counts for employees, raw attendance rows, pending leave requests, and open tickets, and provides quick navigation to major modules.

### Employees

Supports:

- synthetic employee list;
- search across ID, name, department, role, and location;
- status filter;
- sort field and direction;
- employee detail dialog;
- create and edit for name, department, role, location, and status;
- CSV export of the currently filtered directory.

The backend generates IDs. Newly created employees do not receive a schedule mapping automatically.

### Attendance

Supports:

- employee selection;
- raw time-in/time-out records;
- inclusive date range;
- initial date range based on the selected employee's earliest and latest stored rows;
- sorting by date, time in, or time out;
- raw row count and first/last visible date summary;
- CSV export;
- local-only creation for an existing employee with a preconfigured demo schedule mapping.

The server rejects duplicate schedule/date entries, invalid dates, reversed time ranges, and unmapped employees. Overnight shifts are not supported. The interface does not calculate paid hours, lateness, undertime, overtime, absence, or payroll.

### Requests

Leave request:

- choose a synthetic employee and leave type;
- enter a start date, end date, and optional note;
- server validation rejects an invalid/reversed date range and past start dates;
- new requests receive `Pending review` state;
- requesting user can cancel a pending request;
- table search, status filter, inclusive date-overlap filters, newest/oldest sorting, record details, and CSV export;
- monthly calendar shows submitted date spans, including cancelled records, and can draft a start/end range into the form.

The calendar is not an approval calendar. The local app does not calculate working days, leave eligibility, accrued balances, or leave-pay effects.

Ticket:

- choose a synthetic employee and issue type;
- submit a message up to 240 characters;
- the server derives a simple queue label from ticket type;
- submitter can cancel their own open local ticket;
- search, status filter, history sorting, record details, and CSV export.

Queue labels do not represent a delivered ticket, assigned reviewer, notification, resolution, or real access control.

### Team

Read-only roster with search, status filtering, sorting, detail view through the employee dialog, and filtered CSV export. It does not invent memberships, supervisor assignments, or authority.

### HR Desk

Basic synthetic directory create/edit and filtering. It does not implement onboarding/offboarding, employee deletion, salary/government identifiers, audit trails, or HR permissions.

### Reports

- employee status totals;
- department label counts;
- CSV export of the grouped directory summary;
- raw attendance coverage per employee;
- inclusive date filtering;
- mapped/unmapped/all filtering;
- search across employee name, ID, and schedule;
- sorting by employee, raw row count, first entry, and last entry;
- visible result CSV export;
- drill-through to raw attendance rows.

A missing record is not treated as proof of absence. The report does not derive time, hours, classifications, or pay.

### Payroll preview

The product includes a monthly Philippine statutory estimate based on five independently provided bases:

1. cash pay;
2. SSS compensation;
3. PhilHealth monthly basic salary;
4. Pag-IBIG Fund Salary;
5. taxable compensation before employee statutory contributions.

The profile computes employee and employer shares, BIR monthly withholding, estimated net pay, employer statutory cost, and estimated employer cost. It can export a labelled preview CSV, not a payslip.

It does not calculate payroll from attendance, choose the exact semi-monthly calendar, allocate deductions between cut-offs, calculate overtime or holiday rates, use leave balances, save payroll runs, or generate payslips. Details and sources are in `PAYROLL_PROFILE_2026.md`.

## 5. Data model in the local package

The local demo JSON currently stores four operational collections:

- `employees`: `employeeId`, `name`, `department`, `role`, `location`, `status`, `scheduleId`, and optional timestamps.
- `attendance`: `attendanceId`, `scheduleId`, `date`, `timeIn`, `timeOut`, `dayType`, and optional source/timestamp.
- `leaveRequests`: `requestId`, `employeeId`, `employeeName`, `leaveType`, `startDate`, `endDate`, `note`, `status`, and timestamps.
- `tickets`: `ticketId`, `employeeId`, `employeeName`, `ticketType`, `summary`, `queueLabel`, `status`, and timestamps.

The three sample employees have fixed schedule mappings 101–103. New employee records have `scheduleId = null`.

This operational demo schema is intentionally smaller than the user's MO-IT107 design. The full 28-table course dictionary, ERD image, and DFD images are retained in `reference-materials/`. The local code does not claim to implement every table in those course PDFs.

## 6. Relationship to the supplied MO-IT107 documents

The user supplied:

- Data Dictionary for MotorPH Payroll System Database, 28 pages.
- Database Design for the MotorPH Payroll System, 25 pages.
- Data Flow Diagram of MotorPH Payroll System, 6 pages.

Their complete prior synthesis is retained in `reference-materials/MotorPH_Payroll_System_Full_Synthesis.md`.

The course design covers employee details, address, department, position, supervisor, schedule, rest day, leave balance, changelog, attendance, leave, leave accrual history, government identifiers and contributions, payroll, payslip, leave monetisation, earnings, deductibles, other transactions, government tally, summary, regions, cities, holiday, months, rates, tax table, and leave type.

The data dictionary and ERD disagree in several places, notably `Rest Day` versus the `Schedule.restDay` SET field, and the presence/absence of columns or foreign-key links. These differences are recorded in the full synthesis, not silently resolved here.

The DFD's broad order is employee information handling and schedule/time tracking, then payroll processing with timesheet validation, wage and adjustments, leave monetisation, government deductibles, basic payroll computation, payslip generation/distribution, monthly summary compilation, and archival/deletion. This local package currently implements only parts of that workflow.

## 7. How the peer projects informed the feature set

The supplied list was treated as a comparative set:

- Laravel employee payroll repository: conventional employee, department, role, payroll CRUD, and exports.
- Foodya: role/page permission and salary-history UI ideas; its restaurant domain is irrelevant.
- Payroll Manager: Firebase-backed profile and data patterns; the absence of a GitHub licence in the captured snapshot precludes wholesale code reuse.
- PayrollEngine: versioned, testable payroll-rule concepts; its .NET/Docker architecture is not copied.
- Frappe HRMS: broad employee lifecycle, leave, attendance, expenses, recruitment, performance, payroll, and tax/benefit scope; its Frappe framework is not copied.
- `testingester` folders: peer/course submission comparisons, with more complete MO-IT110 examples particularly useful for leave, attendance, payroll, and reporting feature inventories.

The local code was authored as a reconstruction, not pasted from the unlicensed/unclear-license support repositories. `docs/REFERENCE_REPOSITORIES.md` records this boundary.

## 8. Historical workspace status and completion estimates

The LMArena trace contains multiple snapshots. The later 4 November 2026 workspace snapshot reported roughly 61% broad peer-feature breadth, with 43 web unit tests, 167 Java checks, local Playwright E2E, a production build, and a clean npm audit. The denominator was a subjective peer comparison and did not represent grade, code coverage, or production readiness.

That snapshot listed as completed: directory and workflows, sorting/filtering, raw attendance coverage reporting, CSV safety/export, calendar draft, and a separate monthly statutory preview. It still lacked persistent reviewer-authorised workflows, actual team/supervisor assignments, HR lifecycle, schedule policy, attendance-hour derivation, full pay-run mechanics, payslips, and production operations.

The 61% number describes the original workspace snapshot, not an independently measured completion score for this local reconstruction.

## 9. Pending work, ordered by dependency

1. **Schedule model and assignment:** define schedule times, required daily hours, weekly rest days, and assignment/history. Keep unmapped employees explicit.
2. **Attendance semantics:** define break deduction, rounding, overnight shifts, missing punches, late/undertime/overtime treatment, rest days, holidays, and corrections. Then derive hours and classifications.
3. **Leave balances and review:** define entitlement/accrual, unit of measure, request validation, reviewer scope, approve/reject/cancel rules, balance reservation/deduction/reversal, and calendar semantics.
4. **Ticket handling:** define reviewer/queue roles, assignment, status progression, resolution, reopen, notifications, and audit history.
5. **Team/supervisor and HR lifecycle:** define membership, reporting lines, authorised actions, onboarding, update/deactivation, exit handling, and audit trail.
6. **Full payroll run:** choose a pay calendar, salary conversion, attendance/leave/holiday earnings, deduction timing, tax/benefit treatment, statutory basis, rounding, and independent expected values. Then persist a run and generate payslips.
7. **Reports and payroll exports:** build period, employee, department, employer-cost, statutory remittance, and payslip reports once run semantics exist.
8. **Firebase restoration:** port the local service contract to the Firebase-authenticated model, with user scope, server-side authorization, deny-path tests, and no service-account client exposure.
9. **Release quality:** browser E2E, responsive/accessibility checks, failure paths, backups, audit, monitoring, deployment, and a security review if the scope moves beyond a classroom demo.

## 10. What the next agent should do first

- Run `./test.sh` (or `test.bat`) before changing code.
- Start `./run.sh` / `run.bat` and inspect the live screens.
- Read `API_CONTRACT.md` and compare it with the existing routes.
- Pick the smallest next end-to-end slice from the pending list, add tests, then implement both API and UI.
- Update this handoff and feature inventory after each completed slice. Never use a historical estimate as a substitute for observable acceptance criteria.


## Document: `docs/FEATURE_INVENTORY.md`

# Feature Inventory and Implementation Status

Status labels describe this local reconstruction, not the inaccessible original GitHub repository.

- **Implemented:** code and observable UI/API path exist in this package.
- **Partial:** useful part exists, but the peer-level workflow is incomplete.
- **Not implemented:** no working feature path exists.
- **Candidate:** an assumption or convention that needs an explicit product decision before being treated as authoritative.

## 1. Overview

| Feature | Status | Details |
|---|---|---|
| Application launch and local health | Implemented | Java HTTP service serves the frontend and exposes `/api/health`. |
| Navigation | Implemented | Overview, Employees, Attendance, Requests, Team, HR Desk, Reports, Payroll. |
| Live local summary | Implemented | Employee count, raw attendance count, pending leave count, open ticket count. |
| Development-mode notice | Implemented | Screens state that all records are synthetic and local. |
| Firebase login/session | Not implemented in ZIP | Described in original LMArena workspace, absent from local reconstruction. |

## 2. Employee directory

| Feature | Status | Details |
|---|---|---|
| Employee list | Implemented | Server-backed local JSON collection. |
| Search | Implemented | Employee ID, name, department, role, location. |
| Status filter | Implemented | Active, On leave, Inactive. |
| Sort | Implemented | Name, department, role, status; ascending/descending. |
| Detail dialog | Implemented | Shows fields and schedule mapping/timestamps, when present. |
| Create employee | Implemented | Server-generated demo ID. |
| Edit employee | Implemented | Allowlisted name, department, role, location, status. |
| Schedule assignment | Not implemented | New employees have no schedule mapping. |
| Government identifiers / salary / bank details | Not implemented | Deliberately excluded from the synthetic directory UI. |
| Deactivate / terminate workflow | Partial | Status can be changed to Inactive, but no clearance, final pay, or lifecycle workflow exists. |
| Delete / audit history | Not implemented | No delete endpoint or audit trail. |

## 3. Attendance and timekeeping

| Feature | Status | Details |
|---|---|---|
| Raw attendance list | Implemented | Date, employee, schedule ID, time in/out, day type. |
| Employee selection | Implemented | Existing synthetic employees. |
| Inclusive date filtering | Implemented | Optional From/To, invalid reversed dates rejected. |
| Default date coverage | Implemented | Defaults to the selected employee's earliest/latest rows. |
| Sorting | Implemented | Date, time in, time out. |
| Coverage summary | Implemented | Raw row count, first visible date, last visible date. |
| Raw CSV export | Implemented | Exports visible rows. |
| Local synthetic attendance creation | Implemented | Existing employee with schedule mapping only. |
| Duplicate employee/date prevention | Implemented | Server checks schedule ID + date. |
| Time-in/time-out validation | Implemented | Valid time strings, time-in not after time-out; no overnight shift. |
| Attendance source import | Not implemented | No CSV import or external time-clock integration. |
| Clock in / clock out | Not implemented | No live capture workflow. |
| Correction request / approval | Not implemented | No authorised correction route. |
| Break deduction / hours worked | Not implemented | No derived duration. |
| Late / undertime / overtime | Not implemented | Work policy is not settled. |
| Absence classification | Not implemented | Missing rows are not treated as absences. |
| Holiday / rest-day rates | Not implemented | Day label is stored only. |

## 4. Leave requests

| Feature | Status | Details |
|---|---|---|
| Submit leave request | Implemented | Type, employee, start/end dates, optional note. |
| Date/type validation | Implemented | Valid date, end not before start, start not in past, allowed type list. |
| Pending state | Implemented | New record receives `Pending review`. |
| Cancel pending request | Implemented | Cancel only while pending. |
| Search/status filter | Implemented | Search across ID, employee, type, dates, note. |
| Date overlap filter | Implemented | Includes a request when its interval overlaps the selected filter interval. |
| History sort | Implemented | Newest/oldest by creation timestamp. |
| Record details | Implemented | Note and timestamps exposed in disclosure panel. |
| Request-date calendar | Implemented | Monthly calendar shows submitted date spans. |
| Calendar-to-form draft | Implemented | Two selected dates draft a range, but do not submit. |
| Approve / reject | Not implemented | No reviewer identity, permissions, or decision routes. |
| Leave balances / accrual | Not implemented | No calculation or persistence for entitlement. |
| Leave pay / monetisation | Not implemented | Not part of local demo. |
| Notifications / reviewer inbox | Not implemented | Queue state is local only. |

## 5. Tickets and disputes

| Feature | Status | Details |
|---|---|---|
| Create ticket | Implemented | Synthetic employee, type, message. |
| Queue label | Implemented | Simple label mapping based on type. |
| Cancel own open ticket | Implemented | Only Open tickets can be cancelled. |
| Search/status/history sort | Implemented | Client-side table controls. |
| Record detail | Implemented | Message and timestamps. |
| CSV export | Implemented | All local ticket records. |
| Actual queue delivery / assignment | Not implemented | Labels do not deliver to a reviewer. |
| Resolve / reject / reopen | Not implemented | No ticket review state machine. |
| Notifications / SLA | Not implemented | Not modelled. |

## 6. Team and HR Desk

| Feature | Status | Details |
|---|---|---|
| Read-only team roster | Implemented | Reuses the employee directory. |
| Team search/status/sort | Implemented | Independent filter state. |
| Team detail | Implemented | Opens employee detail dialog. |
| Team CSV export | Implemented | Exports filtered roster. |
| Team membership | Not implemented | Department/role/location do not imply membership. |
| Supervisor mapping | Not implemented | No reporting tree or authority is inferred. |
| HR employee create/edit | Implemented | Basic synthetic fields only. |
| HR filters/sort | Implemented | Independent state from Employees and Team. |
| Onboarding/offboarding | Not implemented | No lifecycle forms or state machine. |
| Position/department master data | Not implemented | Stored as basic labels, not normalised lookup tables. |
| HR role permissions / audit | Not implemented | Local demo has no organisational RBAC. |

## 7. Reports and exports

| Feature | Status | Details |
|---|---|---|
| Employee status totals | Implemented | Local directory counts. |
| Department label counts | Implemented | Local directory counts. |
| Directory summary export | Implemented | Grouped count CSV. |
| Raw attendance coverage | Implemented | Row counts and first/last dates per employee. |
| Mapping filter | Implemented | All, mapped, unmapped. |
| Search and sorting | Implemented | Employee name/ID/schedule and selected sort. |
| Date range | Implemented | Inclusive. |
| Drill-through | Implemented | Opens Attendance for a mapped employee and selected dates. |
| CSV safety | Implemented | Quoting, CRLF, UTF-8 BOM, formula-prefix neutralisation. |
| Payroll register / payslip / statutory remittance reports | Not implemented | Requires a real payroll run and pay-period model. |
| Archival / deletion | Not implemented | No retention policy or admin archive path. |

## 8. Payroll

| Feature | Status | Details |
|---|---|---|
| Monthly statutory preview | Implemented | Manual synthetic inputs; version-labelled provisional profile. |
| SSS MSC calculation | Implemented | Salary credit band mapping and regular/MPF components. |
| Employee/employer SSS shares | Implemented | Separated. |
| Employer EC | Implemented | Employer-only amount based on applied MSC band threshold. |
| PhilHealth MBS floor/cap and 5% split | Implemented | Explicit input, floor and ceiling. |
| Pag-IBIG MFS cap and employee/employer shares | Implemented | Explicit input, cap, salary threshold. |
| Monthly BIR withholding | Implemented | Annex E monthly table. |
| Negative base / negative estimated net rejection | Implemented | Validation in calculator. |
| Preview result CSV | Implemented | Clearly labelled preview, not a payslip. |
| Payroll run persistence | Not implemented | No payroll-run entity/API. |
| Attendance-derived gross pay | Not implemented | No work-hour derivation or wage policy. |
| Semi-monthly split/cutoff allocation | Not implemented | No exact cutoff calendar. |
| Overtime / holiday / rest-day / leave earnings | Not implemented | Requires approved inputs and rules. |
| Other deductions / bonuses / loans / advances | Not implemented | No transaction workflow. |
| Payslip generation/distribution | Not implemented | No saved pay result. |
| Employer cost report / remittance report | Not implemented | Preview reports only in one local screen. |

## 9. Persistence, security, and operations

| Feature | Status | Details |
|---|---|---|
| Local JSON persistence | Implemented | File-based demo storage. |
| Initial synthetic seed | Implemented | Three fictional employees and raw attendance rows. |
| Reset demo data | Implemented | Restores seed. |
| Server validation | Implemented | Required values, lengths, types, date/time constraints. |
| Firebase Auth / Firestore | Not implemented in ZIP | Described in original workspace trace, not in this reconstruction. |
| Role-based access control | Not implemented | No user roles. |
| Audit log | Not implemented | No append-only change history. |
| Backups / monitoring / hosting | Not implemented | Local classroom prototype only. |
| Cross-site / hostile-network security review | Not implemented | The service binds to localhost by default. |


## Document: `docs/API_CONTRACT.md`

# Local API Contract

Base URL: `http://127.0.0.1:8080` by default. All routes are served by `src/main/java/ph/motorph/ms2/ApiServer.java`.

All request/response payloads use JSON unless a route serves static files. Validation failures generally return HTTP 400 with `{ "error": "..." }`; not-found entity lookups return HTTP 404.

## Health

`GET /api/health`

Example response:

```json
{
  "ok": true,
  "mode": "LOCAL_SYNTHETIC",
  "payrollProfile": "PH-STATUTORY-2026-MONTHLY-PROVISIONAL",
  "storage": "local-json",
  "livePayroll": false
}
```

## Employees

- `GET /api/employees` — list all records.
- `GET /api/employees?q=<text>&status=<status>&department=<name>` — filtered list.
- `GET /api/employees/{employeeId}` — single record.
- `POST /api/employees` — create a synthetic employee.
- `PATCH /api/employees/{employeeId}` or `PUT` — update allowlisted fields.

Create body:

```json
{
  "name": "Synthetic Employee",
  "department": "Finance",
  "role": "Payroll Associate",
  "location": "Taguig",
  "status": "Active"
}
```

The server generates the employee ID and initial `scheduleId` is `null`. Allowed status values: `Active`, `On leave`, `Inactive`.

The local API intentionally does not accept government IDs, bank details, salary, password, or schedule ID from the browser employee form.

## Attendance

- `GET /api/attendance?employeeId=DEMO-001&from=YYYY-MM-DD&to=YYYY-MM-DD`
- `POST /api/attendance`

The employee selector is optional for the GET route; omitting it returns raw attendance across all currently mapped employees.

Create body:

```json
{
  "employeeId": "DEMO-001",
  "date": "2026-11-05",
  "timeIn": "08:30",
  "timeOut": "17:00",
  "dayType": "Regular"
}
```

Accepted `dayType` values: `Regular`, `Rest day`, `Holiday`, `Special non-working holiday`, `Special working day`.

Rules:

- employee must exist;
- employee must have a schedule mapping;
- date and times must be valid;
- time-in must not be later than time-out;
- duplicate `(scheduleId, date)` rows are rejected;
- overnight shifts are not accepted;
- the server responds with `hoursCalculated: false` and `payrollIncluded: false` for reads.

## Attendance coverage report

`GET /api/reports/attendance?from=YYYY-MM-DD&to=YYYY-MM-DD`

Returns one row per employee with:

- employee ID and name;
- whether a schedule mapping exists;
- schedule ID where available;
- raw record count;
- first and last raw attendance date in the selected inclusive range.

The response includes `hoursCalculated: false` and `payrollIncluded: false`. An unmapped or empty row is not an absence classification.

## Workflows

- `GET /api/workflows` — returns `{ leaveRequests: [...], tickets: [...] }`.
- `POST /api/leave-requests` — create a pending synthetic request.
- `POST /api/leave-requests/{requestId}/cancel` — cancel only a pending request.
- `POST /api/tickets` — create a synthetic open ticket.
- `POST /api/tickets/{ticketId}/cancel` — cancel only an open ticket.

Leave body:

```json
{
  "employeeId": "DEMO-001",
  "leaveType": "Vacation",
  "startDate": "2026-11-10",
  "endDate": "2026-11-11",
  "note": "Fictional sample request"
}
```

Accepted leave type values: `Vacation`, `Sick`, `Emergency`, `Personal`, `Other`. The end date must be on or after the start date; the start date must be today or later; note length is at most 240 characters.

Ticket body:

```json
{
  "employeeId": "DEMO-002",
  "ticketType": "Payroll question",
  "summary": "Fictional sample question"
}
```

Accepted ticket types: `Timesheet correction`, `Payroll question`, `Employee record`, `Leave concern`, `Other`. Message length is at most 240 characters. Queue labels are simple demonstration labels, not reviewer routing.

## Read-only Team and HR views

- `GET /api/team?q=<text>&status=<status>` — filtered employee records; response marks `readOnly: true`.
- `GET /api/hr/employees?q=<text>&status=<status>` — employee list for HR desk.

The GUI uses the ordinary employee create/update endpoints for the synthetic HR directory fields. There is no role permission enforcement in this local mode.

## Payroll

- `GET /api/payroll/rules` — current provisional monthly profile metadata.
- `POST /api/payroll/preview` — calculate a monthly statutory preview.

Body:

```json
{
  "cashPay": "25000.00",
  "sssCompensation": "25000.00",
  "philHealthMbs": "25000.00",
  "pagibigFundSalary": "25000.00",
  "taxableCompensation": "25000.00"
}
```

Response includes `profileId`, period, input bases, contribution components, employee deductions, employer contributions, net estimate, employer cost, `isPersisted: false`, and `isProductionPayroll: false`.

## Demo reset

`POST /api/demo/reset`

Restores the synthetic seed file. This is destructive to local demo changes; the UI asks for confirmation.


## Document: `docs/PAYROLL_PROFILE_2026.md`

# Philippine Monthly Statutory Estimate — 2026 Provisional Profile

**Profile ID:** `PH-STATUTORY-2026-MONTHLY-PROVISIONAL`  
**Period:** Monthly  
**Currency:** PHP  
**Persistence:** None  
**Purpose:** Coursework/demo calculation and a replaceable base for later payroll work. It is not a legally certified payroll engine or a completed payroll run.

## 1. Five independent inputs

The calculator requires separate amounts for:

| Input | Meaning |
|---|---|
| `cashPay` | Cash amount available before employee deductions. Used to produce the estimated net amount and employer-cost total. |
| `sssCompensation` | Monthly SSS compensation basis used to select the Monthly Salary Credit (MSC). |
| `philHealthMbs` | PhilHealth monthly basic salary (MBS) basis. |
| `pagibigFundSalary` | Pag-IBIG Fund Salary subject to the maximum fund salary. |
| `taxableCompensation` | Monthly taxable compensation before employee statutory shares are deducted in this preview. |

These are deliberately separate. The application does not assume that every statutory agency defines compensation identically. The user enters fictional values directly; the calculator does not infer them from employee data, attendance, or salary records.

## 2. SSS

The profile follows the SSS business employer/employee schedule effective 1 January 2025, which remains the latest schedule surfaced by the official SSS pages on 4 November 2026.

- Regular SS contribution rate: 15% of the applicable MSC, split 5% employee and 10% employer.
- Total MSC is capped at ₱35,000.
- MSC above ₱20,000 and up to ₱35,000 is attributed to the Mandatory Provident Fund (MPF / MySSS Pension Booster), with employee/employer shares still 5%/10% on that component.
- Employer-only Employees' Compensation (EC): ₱10 for MSC at ₱14,500 and below; ₱30 for MSC at ₱15,000 and above.
- The schedule has ₱5,000 minimum MSC and uses ₱500 steps. The first compensation bracket below ₱5,250 maps to ₱5,000 MSC; ₱5,250 begins the next bracket at ₱5,500 MSC.

Implementation:

1. Map monthly SSS compensation to an MSC using the schedule's ₱500 steps, with minimum ₱5,000 and maximum ₱35,000.
2. Split the MSC into regular SS MSC up to ₱20,000 and MPF MSC above ₱20,000.
3. Calculate 5% employee and 10% employer shares on the regular and MPF components.
4. Add employer-only EC from the applicable MSC threshold.

The code returns the total employee share, total employer SS/MPF share, employer EC, total MSC, and regular/MPF components separately.

## 3. PhilHealth

For 2026, PhilHealth's 5% premium rate remains in effect. The May 2026 Philippine Information Agency report records PhilHealth's confirmation that the rate remained 5%, with a ₱10,000 income floor and ₱100,000 income ceiling. The 2025 official PhilHealth advisory sets the MBS definition and says the employed-member premium is equally shared by the employer and employee.

- Rate: 5% of applicable MBS.
- MBS floor: ₱10,000.
- MBS ceiling: ₱100,000.
- Employee share: 50% of total premium.
- Employer share: 50% of total premium.
- MBS is the employee's fixed basic rate; the 2025 advisory excludes sales commission, overtime, allowances, 13th-month pay, bonuses, other gratuity payments, and reductions arising from undertime, tardiness, unpaid leave, absences, or similar causes.

Implementation:

1. Clamp the explicit PhilHealth MBS input to ₱10,000–₱100,000.
2. Multiply by 5%.
3. Split the premium equally between employee and employer.

## 4. Pag-IBIG

The profile uses Pag-IBIG Fund Circular No. 460, effective February 2024:

- Maximum Fund Salary (MFS): ₱10,000 per month, increased from ₱5,000.
- Employee share: 1% for Fund Salary of ₱1,500 and below; 2% over ₱1,500.
- Employer share: 2%.

Implementation:

1. Clamp the explicit Pag-IBIG Fund Salary input to ₱0–₱10,000.
2. Select the employee rate: 1% at or below ₱1,500, otherwise 2%.
3. Calculate the employer share at 2%.

The app does not infer which allowances should be included in an employee's Fund Salary. That base must be supplied explicitly.

## 5. BIR monthly withholding

The profile uses the monthly table in Annex E of Revenue Regulations No. 11-2018, effective 1 January 2023 onwards.

| Monthly taxable compensation | Monthly withholding |
|---|---|
| ₱20,833 and below | ₱0 |
| ₱20,833–₱33,332 | 15% of excess over ₱20,833 |
| ₱33,333–₱66,666 | ₱1,875 + 20% of excess over ₱33,333 |
| ₱66,667–₱166,666 | ₱8,541.80 + 25% of excess over ₱66,667 |
| ₱166,667–₱666,666 | ₱33,541.80 + 30% of excess over ₱166,667 |
| ₱666,667 and above | ₱183,541.80 + 35% of excess over ₱666,667 |

Implementation order in the current preview:

1. Sum the employee SSS, PhilHealth, and Pag-IBIG shares.
2. Subtract that total from the separately entered taxable compensation.
3. Reject a result below zero rather than creating a negative taxable base.
4. Apply the monthly BIR Annex E table to the resulting amount.

This is a provisional calculator assumption about the tax base ordering for this preview. The full annual reconciliation, taxable/exempt classification of every benefit, and withholding adjustments are outside its scope.

## 6. Rounding and net estimate

- Inputs are parsed as `BigDecimal`.
- Monetary amounts are rounded to two decimal places using `HALF_UP`.
- Percentages are applied to the selected base and rounded to cents at the contribution component level.
- PhilHealth is split equally at two decimal places.
- Net estimate = cash pay − employee SSS − employee PhilHealth − employee Pag-IBIG − monthly BIR withholding.
- Employer statutory cost = employer SSS (regular + MPF) + employer EC + employer PhilHealth + employer Pag-IBIG.
- Employer cost estimate = cash pay + employer statutory cost.
- A negative estimated net is rejected rather than emitted as negative take-home pay.

## 7. Worked example

All five explicit input bases are set to ₱25,000.00.

| Item | Amount |
|---|---:|
| SSS monthly salary credit | ₱25,000.00 |
| SSS regular SS MSC component | ₱20,000.00 |
| SSS MPF MSC component | ₱5,000.00 |
| SSS employee share | ₱1,250.00 |
| SSS employer share (SS + MPF) | ₱2,500.00 |
| SSS employer EC | ₱30.00 |
| PhilHealth total premium | ₱1,250.00 |
| PhilHealth employee share | ₱625.00 |
| PhilHealth employer share | ₱625.00 |
| Pag-IBIG Fund Salary applied | ₱10,000.00 |
| Pag-IBIG employee share | ₱200.00 |
| Pag-IBIG employer share | ₱200.00 |
| Employee statutory shares before tax | ₱2,075.00 |
| Taxable base after employee statutory shares | ₱22,925.00 |
| BIR monthly withholding | ₱313.80 |
| Estimated net pay | **₱22,611.20** |
| Total employer statutory cost | ₱3,355.00 |
| Estimated employer cost | **₱28,355.00** |

The example demonstrates the calculator arithmetic, not a full actual employee payroll. The bases are deliberately entered independently at the same value for illustration.

## 8. Not included

- Attendance-based wages, salary conversion, daily/monthly/hourly rate reconciliation.
- Meal-break treatment, time worked, late minutes, undertime, overtime, night differential.
- Regular holiday, special non-working day, special working day, and rest-day premiums.
- Leave eligibility, paid leave, unpaid leave, leave accrual, leave monetisation.
- Bonuses, commissions, reimbursements, allowances, loans, advances, and other payroll deductions.
- Semi-monthly or other cut-off splitting, per-cutoff contribution/tax allocation, annual tax adjustment, and year-end reconciliation.
- Persisted payroll run, approval, posting, bank disbursement, BIR remittance, government remittance, or payslip generation.

Do not describe this monthly preview as a payslip or as a production-ready payroll engine.

## 9. Sources

1. SSS, *Pay Contributions* — official employee/employer contribution information and current salary-credit schedule: <https://www.sss.gov.ph/pay-contribution/>.
2. SSS Circular No. 2024-006, *Schedule of SSS Contributions Effective January 2025*: <https://www.sss.gov.ph/wp-content/uploads/2024/12/2025-SSS-Contribution-Table-rev.pdf>.
3. Philippine Health Insurance Corporation Advisory No. 2025-0002, *Premium Contribution for All Direct Contributors for CY 2025*: <https://www.philhealth.gov.ph/advisories/2025/PA2025-0002.pdf>.
4. Philippine Information Agency, *PhilHealth sets 5% premium contribution rate for 2026*, 14 May 2026: <https://pia.gov.ph/news/philhealth-sets-5-premium-contribution-rate-for-2026/>.
5. Pag-IBIG Fund Circular No. 460, *Guidelines on the Implementation of Increase in the Maximum Fund Salary Effective February 2024*. The copy included in the LMArena research trace is available at <https://www.pagibigfund.gov.ph/documents/Circular%20No.%20460%20-%20Guidelines%20on%20the%20Pag-IBIG%20Fund's%20Implementation%20of%20Increase%20in%20the%20MFS%20Effective%20February%202024.pdf>.
6. Bureau of Internal Revenue, *Annex E — Revised Withholding Tax Table, Effective 1 January 2023 and onwards*: <https://bir-cdn.bir.gov.ph/local/pdf/Annex%20E%20RR%2011-2018.pdf>.
7. BIR Revenue Regulations No. 11-2018: <https://bir-cdn.bir.gov.ph/local/pdf/RR%20No.%2011-2018.pdf>.

These are public-source references used to populate the replaceable provisional profile. Always recheck effective dates and agency guidance before using the calculator for real compensation.


## Document: `docs/SOURCE_GROUNDING.md`

# Source Grounding and Provenance

## A. User-supplied primary materials

| Source | What it establishes | Where retained |
|---|---|---|
| LMArena complete trace export | Prior user instructions, project objectives, feature scope, architecture, API route descriptions, historical tests, statutory research, source excerpts, and workspace status | `reference-materials/lmarena_fully_sanitized_agent_run_1793783074106.txt` |
| `skills_compilation.md` | Coding workflow skills, source-first behaviour, verification, surgical changes, and the user's preferred agent operating rules | `reference-materials/skills_compilation.md` |
| MO-IT107 Data Dictionary PDF (28 pages) | Table catalogue, column descriptions, data types, precision, constraints, and table purposes | `reference-materials/MO-IT107 Homework_ Data Dictionary for MotorPH Payroll System Database H2101 Torres, J.D..pdf` |
| MO-IT107 Database Design PDF (25 pages) | Database overview, visual ERD, table-level column/constraint text, entity relationships | `reference-materials/MO-IT107 - 2nd Milestone_ Database Design for the MotorPH Payroll System H2101 TORRES, J.D..pdf` |
| MO-IT107 DFD PDF (6 pages) | Context-level, Level 1, Level 2 payroll, and archival DFDs | `reference-materials/MO-IT107 Homework_ Data Flow Diagram of MotorPH Payroll System H2101 Torres, J.D..pdf` |
| Earlier full synthesis | Expanded cross-PDF source transcription including diagram explanations and discrepancies | `reference-materials/MotorPH_Payroll_System_Full_Synthesis.md` |
| OOP feature inventory/pseudocode | Prior code-derived feature inventory for the MO-IT110 OOP team project | `reference-materials/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md` |

## B. GitHub workspace and support repositories

The user supplied these sources:

- Workspace: <https://github.com/Sitramonicus/forreview>
- Peer submissions: <https://github.com/Sitramonicus/testingester>
- Supporting comparisons:
  - <https://github.com/wasilolly/Employee-Payroll-with-laravel-framework>
  - <https://github.com/Shahzaib-Awann/Foodya-Restaurant>
  - <https://github.com/manuog1994/payroll-manager>
  - <https://github.com/Payroll-Engine/PayrollEngine>
  - <https://github.com/frappe/hrms>

The exact live contents of the workspace repository were not fetched into this execution. The local code is therefore a reconstruction based on the LMArena trace, not a checkout of the private repository. The raw source URLs are included for future direct comparison.

The LMArena trace captured upstream support snapshots and warns that certain repositories had no recognized licence metadata. This local reconstruction uses feature concepts and independently authored code; it does not copy entire upstream implementations. Review the upstream licence and coursework terms before reusing source code from a comparison project.

## C. LMArena chronology and overrides

The trace contains several progress snapshots, so estimates and states differ. The latest identifiable 4 November 2026 snapshot described approximately 61% broad peer-feature breadth, with a working temporary monthly 2026 statutory preview. Earlier assistant-produced roadmap entries claimed payroll remained disabled. The user later overrode that hold and asked the agent not to let excessive caution prevent producing a product.

This package acts on the newer instruction. It implements a monthly statutory estimate, but does not pretend that the full attendance-to-payslip payroll lifecycle is done.

The trace extraction warns that 24 conversation elements remained collapsed. Their contents may be absent; no decision or file content is invented to fill those gaps.

## D. Course PDF visual content

The PDFs contain diagrams and tables that cannot be fully understood from extracted text alone. The earlier synthesis records diagram-page details. Key visual elements:

- Database Design, page 5: ERD with entity boxes, PK/FK annotations, connecting lines, and legend. It includes a separate `Rest Day` entity and a schedule relation distinct from the simplified data dictionary's SET field.
- DFD, page 2: context diagram with Employee, Supervisor, Human Resources, Payroll Staff, System Admin, and central MotorPH Payroll System process.
- DFD, page 3: Level 1 Employee Management, Payroll Processing, Archival, employee repository, logs, payroll, and cold storage.
- DFD, page 4: Employee Information Handling, Schedule and Time Tracking, Government Info Access, and data stores for address, job profile, personally identifiable information, attendance, leave, and changelog.
- DFD, page 5: Level 2 Payroll Processing, including Timesheet Validation, Leave Monetisation, Wage and Adjustments, Government Deductibles, Basic Payroll Computation, Payslip Generation & Distribution, Monthly Summary Compilation, and their data stores.
- DFD, page 6: archival and deletion processes, annual compilation, cold storage, logs, and payslip records.

## E. Explicit discrepancy policy

Do not silently reconcile these source differences:

- `Schedule.restDay` appears as `SET` in the dictionary, while the ERD has a separate `Rest Day` table and `restDayID` relation.
- The Employee table's department and government references appear in the dictionary but are missing/represented differently in the later database-design table text.
- Supervisor entity attributes and Employee supervisor references are not consistently shown across the diagrams and table descriptions.
- Tax rates and contribution values in the design are described as reference tables, but the submitted design does not fully specify effective dating, year/month keys, tax period, formulas, or rounding.
- The DFD references Business Requirement Specification and Existing Business Processes documents that were not part of the three uploaded PDFs.

The local demo schema is intentionally a small operational subset, not a claim that all course-design entities are implemented.


## Document: `docs/TESTS_AND_VERIFICATION.md`

# Tests and Verification Record

## Checks run in this package

The current `test.sh` / `test.bat` performs:

1. Compile Java main and test source with `javac --release 11`.
2. Run `ph.motorph.ms2.SmokeTest`.
3. Run `node --check web/app.js`.
4. Run `python test/check_html_bindings.py` to confirm there are no duplicate HTML IDs and that literal JavaScript element references resolve.

Latest observed output:

```text
PASS: 36 MotorPH checks
PASS: browser JavaScript syntax check
PASS: 122 unique HTML IDs; 101 literal JavaScript bindings resolve.
```

The 31 Java checks cover JSON round-tripping, `BigDecimal` behaviour, the ₱25,000 monthly statutory example, SSS MSC boundaries, BIR whole-peso and fractional-centavo bracket boundaries, negative input/net rejection, health endpoint, employee list/create, raw attendance read/create, duplicate attendance rejection, leave submit/list/cancel, ticket submit/cancel, attendance coverage, and payroll preview endpoint validation.

## Local HTTP smoke

The service was started with `PORT=8183` and returned:

- `GET /api/health` → HTTP 200, `LOCAL_SYNTHETIC` mode, local JSON storage, non-persisted payroll marker.
- `GET /` → HTTP 200 HTML.
- `GET /app.js` → HTTP 200 JavaScript.
- `GET /api/employees` → HTTP 200 synthetic roster.
- `GET /api/attendance?employeeId=DEMO-001` → HTTP 200 raw records.

This service was run with the packaged code. The example port was chosen because port 8080 was already occupied by another service in this execution environment. The default for a user's own machine remains 8080. The temporary smoke-test process used a separate temporary JSON file and was stopped after the checks.

## Browser end-to-end limitation

A Playwright attempt to open the local browser app was blocked by the environment with `net::ERR_BLOCKED_BY_ADMINISTRATOR` when Chromium navigated to `127.0.0.1:8182`. A standalone Chromium screenshot attempt also did not complete in the allotted command window. Therefore this package does **not** claim that a full browser end-to-end test passed in this session.

The LMArena trace reports earlier E2E passes in the original agent environment. Those are historical source evidence, not a test rerun against this reconstructed ZIP.

## Re-run locally

Windows:

```bat
test.bat
run.bat
```

Linux/macOS:

```sh
./test.sh
./run.sh
```

For an independent smoke test once the app is running:

```sh
curl http://127.0.0.1:8080/api/health
curl http://127.0.0.1:8080/api/employees
curl "http://127.0.0.1:8080/api/attendance?employeeId=DEMO-001"
```

## Test interpretation

Passing these tests proves the tested local paths behave as expected. It does not prove production security, full workflow correctness, legal payroll compliance, Firebase integration, browser accessibility, or peer-feature parity.


## Document: `docs/REFERENCE_REPOSITORIES.md`

# Reference Repository Comparison Notes

The repositories are comparison material, not a mandate to reproduce their architecture. The user asked that their folders support the project; the LMArena trace contains feature reviews and captured source snapshots for several. This reconstruction uses relevant behaviour patterns and independently authored Java/browser code. It does not copy whole codebases.

## Main comparison repositories

### 1. Laravel Employee Payroll System

<https://github.com/wasilolly/Employee-Payroll-with-laravel-framework>

Useful comparison areas recorded in the LMArena review:

- employee, department, and role administration;
- payroll record handling;
- reports and PDF output;
- conventional authenticated office workflows.

Not copied:

- Laravel/PHP project architecture;
- its database or employee records;
- assumptions that its payroll formulas, table design, or age of implementation match MotorPH requirements.

The captured snapshot did not have recognized GitHub licence metadata. Do not transplant source code without separately verifying licence and coursework requirements.

### 2. Foodya Restaurant

<https://github.com/Shahzaib-Awann/Foodya-Restaurant>

Useful comparison areas:

- role/page navigation distinctions;
- employee or staff access patterns;
- financial history and status-oriented lists;
- a dashboard layout with separate functional workspaces.

Not copied:

- menu, ordering, kitchen, delivery, and restaurant-specific logic;
- Next.js/MySQL architecture;
- assumptions that restaurant cashier roles map directly to HR/payroll permissions.

The captured snapshot did not have recognized GitHub licence metadata. Treat its source as reference only until rights are confirmed.

### 3. Payroll Manager

<https://github.com/manuog1994/payroll-manager>

Useful comparison areas:

- Firebase-backed application concepts;
- profile and basic payroll record structures.

Not copied:

- code whose ownership/licence was not established;
- its simplified data model as a complete MotorPH payroll design.

The LMArena snapshot records no recognized repository licence metadata. Seek permission before copying source.

### 4. PayrollEngine

<https://github.com/Payroll-Engine/PayrollEngine>

Useful comparison areas:

- payroll rules treated as configurable, versioned policy;
- explicit pay-run stages and calculation results;
- regression tests and worked examples;
- separation of payroll calculation logic from UI code.

Not copied:

- .NET/ASP.NET/Docker runtime architecture;
- the full multi-repository production system;
- country-specific policy configuration unrelated to the Philippine course project.

The captured upstream repository was identified as MIT licensed in the LMArena trace. The current reconstruction still does not copy its code.

### 5. Frappe HRMS

<https://github.com/frappe/hrms>

Useful comparison areas:

- employee lifecycle and directory structure;
- leave management and balance concepts;
- shift/attendance concepts;
- salary structure and payroll processing;
- tax/benefit, expenses, recruitment, performance, and reports as possible future scope references.

Not copied:

- Frappe Framework or ERPNext runtime;
- Python modules and assets;
- the full ERP scope, deployment stack, or assumptions around its database.

The captured repository was GPL-3.0 in the LMArena export. This local reconstruction does not copy its code.

## `testingester` peer folders

<https://github.com/Sitramonicus/testingester>

The LMArena trace describes nine peer-submission folders. Later reviews weighted the more complete MO-IT110-style examples in folders 6 and 9 more heavily for payroll, attendance, and leave workflow comparisons, and also considered earlier OOP/course project designs.

The current local product does not include all peer folder contents. The LMArena trace and the user's prior OOP feature inventory remain the available comparative record in this package.

## How to use the comparisons

A future agent should:

1. extract the required feature idea, not the source project architecture;
2. trace the feature to the MotorPH course schema or a user-approved feature target;
3. identify its data inputs, authority, validation, statuses, and side effects;
4. implement it in the current Java/browser architecture;
5. add tests for the normal and failure paths;
6. mark any inferred policy as a proposal, not an established MotorPH rule.

The purpose of reference projects is to expose missing functional areas and implementation patterns, not to inflate scope with unrelated modules.


---

# Appendix B — Complete current source code and runtime files

The following sections reproduce the exact text files shipped in the ZIP, as they exist at handoff time. The source files in the archive remain the runnable copy; this appendix is an agent-readable text mirror.


### `src/main/java/ph/motorph/ms2/Main.java`

```java
package ph.motorph.ms2;

import java.nio.file.Path;

public final class Main {
    private Main() {}
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String host = System.getenv().getOrDefault("HOST", "127.0.0.1");
        Path dataFile = Path.of(System.getenv().getOrDefault("MOTORPH_DATA_FILE", "data/demo-data.json"));
        Path webRoot = Path.of(System.getenv().getOrDefault("MOTORPH_WEB_ROOT", "web"));
        DemoStore store = new DemoStore(dataFile);
        ApiServer app = new ApiServer(store, webRoot);
        app.start(host, port);
        System.out.println("MotorPH local workspace running at http://" + host + ":" + port);
        System.out.println("Mode: synthetic local data; storage: " + dataFile.toAbsolutePath());
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }
}
```


### `src/main/java/ph/motorph/ms2/ApiServer.java`

```java
package ph.motorph.ms2;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ApiServer {
    private final DemoStore store;
    private final PayrollCalculator2026 payrollCalculator = new PayrollCalculator2026();
    private final Path webRoot;
    private HttpServer server;
    private ExecutorService executor;

    ApiServer(DemoStore store, Path webRoot) {
        this.store = store;
        this.webRoot = webRoot.toAbsolutePath().normalize();
    }

    void start(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/", this::handleApi);
        server.createContext("/", this::handleStatic);
        executor = Executors.newCachedThreadPool();
        server.setExecutor(executor);
        server.start();
    }

    int port() { return server == null ? -1 : server.getAddress().getPort(); }

    void stop() { if (server != null) server.stop(0); if (executor != null) executor.shutdownNow(); }

    private void handleApi(HttpExchange exchange) throws IOException {
        try {
            cors(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { send(exchange, 204, ""); return; }
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
            Object response;
            int status = 200;

            if ("GET".equals(method) && "/api/health".equals(path)) {
                response = mapOf("ok", true, "mode", "LOCAL_SYNTHETIC", "payrollProfile", PayrollCalculator2026.PROFILE_ID,
                        "storage", "local-json", "livePayroll", false);
            } else if ("GET".equals(method) && "/api/employees".equals(path)) {
                List<Map<String, Object>> employees = filterEmployees(store.employees(), query);
                response = mapOf("items", employees, "total", store.employees().size(), "matched", employees.size());
            } else if ("POST".equals(method) && "/api/employees".equals(path)) {
                Map<String, Object> values = employeeInput(body(exchange), true);
                response = mapOf("item", store.createEmployee(values)); status = 201;
            } else if (("PUT".equals(method) || "PATCH".equals(method)) && path.startsWith("/api/employees/")) {
                String id = segment(path, 3);
                Map<String, Object> values = employeeInput(body(exchange), false);
                Map<String, Object> item = store.updateEmployee(id, values);
                if (item == null) { response = error("Employee not found."); status = 404; }
                else response = mapOf("item", item);
            } else if ("GET".equals(method) && path.startsWith("/api/employees/")) {
                String id = segment(path, 3);
                Map<String, Object> item = store.employee(id);
                if (item == null) { response = error("Employee not found."); status = 404; }
                else response = mapOf("item", item);
            } else if ("GET".equals(method) && "/api/attendance".equals(path)) {
                response = attendanceQuery(query);
            } else if ("POST".equals(method) && "/api/attendance".equals(path)) {
                response = mapOf("item", createAttendance(body(exchange))); status = 201;
            } else if ("GET".equals(method) && "/api/reports/attendance".equals(path)) {
                response = attendanceReport(query);
            } else if ("GET".equals(method) && "/api/workflows".equals(path)) {
                response = mapOf("leaveRequests", store.leaveRequests(), "tickets", store.tickets());
            } else if ("POST".equals(method) && "/api/leave-requests".equals(path)) {
                response = mapOf("item", createLeave(body(exchange))); status = 201;
            } else if ("POST".equals(method) && path.startsWith("/api/leave-requests/") && path.endsWith("/cancel")) {
                Map<String, Object> item = store.cancelLeave(segment(path, 3));
                if (item == null) { response = error("Leave request not found."); status = 404; }
                else response = mapOf("item", item);
            } else if ("POST".equals(method) && "/api/tickets".equals(path)) {
                response = mapOf("item", createTicket(body(exchange))); status = 201;
            } else if ("POST".equals(method) && path.startsWith("/api/tickets/") && path.endsWith("/cancel")) {
                Map<String, Object> item = store.cancelTicket(segment(path, 3));
                if (item == null) { response = error("Ticket not found."); status = 404; }
                else response = mapOf("item", item);
            } else if ("GET".equals(method) && "/api/team".equals(path)) {
                response = mapOf("items", filterEmployees(store.employees(), query), "readOnly", true);
            } else if ("GET".equals(method) && "/api/hr/employees".equals(path)) {
                response = mapOf("items", filterEmployees(store.employees(), query));
            } else if ("GET".equals(method) && "/api/payroll/rules".equals(path)) {
                response = mapOf("profileId", PayrollCalculator2026.PROFILE_ID, "period", "MONTHLY", "currency", "PHP",
                        "description", "Monthly statutory estimate. Separate compensation bases are required.", "persisted", false);
            } else if ("POST".equals(method) && "/api/payroll/preview".equals(path)) {
                response = payrollCalculator.calculate(body(exchange));
            } else if ("POST".equals(method) && "/api/demo/reset".equals(path)) {
                store.reset(); response = mapOf("ok", true, "message", "Synthetic demo data has been reset.");
            } else {
                response = error("API route not found."); status = 404;
            }
            sendJson(exchange, status, response);
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, error(ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            sendJson(exchange, 500, error("The server could not complete the request."));
        }
    }

    private List<Map<String, Object>> filterEmployees(List<Map<String, Object>> source, Map<String, String> query) {
        String search = query.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
        String department = query.getOrDefault("department", "").trim();
        String status = query.getOrDefault("status", "").trim();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> employee : source) {
            if (!search.isEmpty()) {
                String searchable = (DemoStore.str(employee.get("employeeId")) + " " + DemoStore.str(employee.get("name")) + " " +
                        DemoStore.str(employee.get("department")) + " " + DemoStore.str(employee.get("role")) + " " +
                        DemoStore.str(employee.get("location"))).toLowerCase(Locale.ROOT);
                if (!searchable.contains(search)) continue;
            }
            if (!department.isEmpty() && !department.equals(DemoStore.str(employee.get("department")))) continue;
            if (!status.isEmpty() && !status.equals(DemoStore.str(employee.get("status")))) continue;
            result.add(new LinkedHashMap<>(employee));
        }
        result.sort(Comparator.comparing(row -> DemoStore.str(row.get("employeeId"))));
        return result;
    }

    private Map<String, Object> attendanceQuery(Map<String, String> query) {
        String employeeId = query.getOrDefault("employeeId", "").trim();
        String from = dateParam(query.get("from"), "from");
        String to = dateParam(query.get("to"), "to");
        if (!from.isEmpty() && !to.isEmpty() && from.compareTo(to) > 0) throw new IllegalArgumentException("The start date must not be after the end date.");
        Map<Integer, String> scheduleToEmployee = scheduleMap();
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : store.attendance()) {
            int scheduleId = DemoStore.integer(row.get("scheduleId"), -1);
            String owner = scheduleToEmployee.get(scheduleId);
            if (owner == null || (!employeeId.isEmpty() && !employeeId.equals(owner))) continue;
            String date = DemoStore.str(row.get("date"));
            if (!from.isEmpty() && date.compareTo(from) < 0) continue;
            if (!to.isEmpty() && date.compareTo(to) > 0) continue;
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("employeeId", owner);
            items.add(copy);
        }
        items.sort(Comparator.comparing(row -> DemoStore.str(row.get("date"))));
        return mapOf("items", items, "total", items.size(), "hoursCalculated", false, "payrollIncluded", false);
    }

    private Map<String, Object> createAttendance(Map<String, Object> input) throws IOException {
        String employeeId = required(input, "employeeId");
        Map<String, Object> employee = store.employee(employeeId);
        if (employee == null) throw new IllegalArgumentException("Choose an existing synthetic employee.");
        int scheduleId = DemoStore.integer(employee.get("scheduleId"), -1);
        if (scheduleId < 0) throw new IllegalArgumentException("This employee has no schedule mapping. Assigning one is outside this local demo workflow.");
        String date = validDate(required(input, "date"), "date");
        String timeIn = required(input, "timeIn");
        String timeOut = required(input, "timeOut");
        validTime(timeIn, "timeIn"); validTime(timeOut, "timeOut");
        if (timeIn.compareTo(timeOut) > 0) throw new IllegalArgumentException("Time-in must not be later than time-out. Overnight shifts are not supported by this demo.");
        String dayType = optional(input, "dayType", "Regular");
        if (!List.of("Regular", "Rest day", "Holiday", "Special non-working holiday", "Special working day").contains(dayType)) throw new IllegalArgumentException("Choose a listed day classification.");
        for (Map<String, Object> row : store.attendance()) {
            if (scheduleId == DemoStore.integer(row.get("scheduleId"), -2) && date.equals(DemoStore.str(row.get("date")))) {
                throw new IllegalArgumentException("An attendance record already exists for this employee and date.");
            }
        }
        return store.createAttendance(mapOf("scheduleId", scheduleId, "date", date, "timeIn", timeIn, "timeOut", timeOut, "dayType", dayType,
                "source", "LOCAL_SYNTHETIC_FORM"));
    }

    private Map<String, Object> attendanceReport(Map<String, String> query) {
        String from = dateParam(query.get("from"), "from");
        String to = dateParam(query.get("to"), "to");
        if (!from.isEmpty() && !to.isEmpty() && from.compareTo(to) > 0) throw new IllegalArgumentException("The start date must not be after the end date.");
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Integer, String> scheduleToEmployee = scheduleMap();
        List<Map<String, Object>> all = store.attendance();
        for (Map<String, Object> employee : store.employees()) {
            int scheduleId = DemoStore.integer(employee.get("scheduleId"), -1);
            List<String> dates = new ArrayList<>();
            int count = 0;
            for (Map<String, Object> row : all) {
                if (scheduleId < 0 || scheduleId != DemoStore.integer(row.get("scheduleId"), -2)) continue;
                String date = DemoStore.str(row.get("date"));
                if (!from.isEmpty() && date.compareTo(from) < 0) continue;
                if (!to.isEmpty() && date.compareTo(to) > 0) continue;
                count++; dates.add(date);
            }
            dates.sort(String::compareTo);
            result.add(mapOf("employeeId", employee.get("employeeId"), "employeeName", employee.get("name"),
                    "hasScheduleMapping", scheduleId >= 0 && scheduleToEmployee.containsKey(scheduleId),
                    "scheduleId", scheduleId < 0 ? null : scheduleId, "rawRecordCount", count,
                    "firstRecordDate", dates.isEmpty() ? null : dates.get(0), "lastRecordDate", dates.isEmpty() ? null : dates.get(dates.size() - 1)));
        }
        return mapOf("items", result, "hoursCalculated", false, "payrollIncluded", false,
                "from", from.isEmpty() ? null : from, "to", to.isEmpty() ? null : to);
    }

    private Map<String, Object> createLeave(Map<String, Object> input) throws IOException {
        String employeeId = required(input, "employeeId");
        Map<String, Object> employee = store.employee(employeeId);
        if (employee == null) throw new IllegalArgumentException("Choose an existing synthetic employee.");
        String type = required(input, "leaveType");
        if (!List.of("Vacation", "Sick", "Emergency", "Personal", "Other").contains(type)) throw new IllegalArgumentException("Choose a listed leave type.");
        String start = validDate(required(input, "startDate"), "startDate");
        String end = validDate(required(input, "endDate"), "endDate");
        if (start.compareTo(end) > 0) throw new IllegalArgumentException("The leave end date must be on or after its start date.");
        if (start.compareTo(LocalDate.now().toString()) < 0) throw new IllegalArgumentException("Demo leave requests must start today or later.");
        String note = optional(input, "note", "");
        if (note.length() > 240) throw new IllegalArgumentException("The note must be 240 characters or fewer.");
        return store.createLeave(mapOf("employeeId", employeeId, "employeeName", employee.get("name"), "leaveType", type,
                "startDate", start, "endDate", end, "note", note));
    }

    private Map<String, Object> createTicket(Map<String, Object> input) throws IOException {
        String employeeId = required(input, "employeeId");
        Map<String, Object> employee = store.employee(employeeId);
        if (employee == null) throw new IllegalArgumentException("Choose an existing synthetic employee.");
        String type = required(input, "ticketType");
        if (!List.of("Timesheet correction", "Payroll question", "Employee record", "Leave concern", "Other").contains(type)) throw new IllegalArgumentException("Choose a listed ticket type.");
        String summary = required(input, "summary");
        if (summary.length() > 240) throw new IllegalArgumentException("The message must be 240 characters or fewer.");
        String queue = type.equals("Timesheet correction") ? "Team Lead" : (type.equals("Payroll question") ? "Payroll" : "HR Desk");
        return store.createTicket(mapOf("employeeId", employeeId, "employeeName", employee.get("name"), "ticketType", type,
                "summary", summary, "queueLabel", queue));
    }

    private Map<String, Object> employeeInput(Map<String, Object> input, boolean create) {
        Map<String, Object> values = new LinkedHashMap<>();
        String name = create ? required(input, "name") : optional(input, "name", null);
        if (name != null) {
            name = name.trim().replaceAll("\\s+", " ");
            if (name.length() < 2 || name.length() > 100) throw new IllegalArgumentException("Name must contain 2 to 100 characters.");
            values.put("name", name);
        }
        putOptional(values, input, "department", 80);
        putOptional(values, input, "role", 80);
        putOptional(values, input, "location", 80);
        String status = optional(input, "status", null);
        if (status != null) {
            if (!List.of("Active", "On leave", "Inactive").contains(status)) throw new IllegalArgumentException("Status must be Active, On leave, or Inactive.");
            values.put("status", status);
        } else if (create) values.put("status", "Active");
        if (create) {
            for (String field : List.of("department", "role", "location")) if (!values.containsKey(field)) throw new IllegalArgumentException("Missing required field: " + field);
        }
        if (values.isEmpty()) throw new IllegalArgumentException("Provide at least one editable employee field.");
        return values;
    }

    private void putOptional(Map<String, Object> values, Map<String, Object> input, String key, int max) {
        if (!input.containsKey(key)) return;
        String value = String.valueOf(input.get(key) == null ? "" : input.get(key)).trim();
        if (value.isEmpty() || value.length() > max) throw new IllegalArgumentException(key + " must contain 1 to " + max + " characters.");
        values.put(key, value);
    }

    private Map<Integer, String> scheduleMap() {
        Map<Integer, String> result = new LinkedHashMap<>();
        for (Map<String, Object> employee : store.employees()) {
            int scheduleId = DemoStore.integer(employee.get("scheduleId"), -1);
            if (scheduleId >= 0) result.put(scheduleId, DemoStore.str(employee.get("employeeId")));
        }
        return result;
    }

    private void handleStatic(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()) && !"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Method not allowed."); return;
        }
        String path = exchange.getRequestURI().getPath();
        if (path == null || path.equals("/")) path = "/index.html";
        Path requested = webRoot.resolve(path.substring(1)).normalize();
        if (!requested.startsWith(webRoot) || !Files.exists(requested) || Files.isDirectory(requested)) {
            send(exchange, 404, "Not found."); return;
        }
        String contentType = contentType(requested);
        byte[] bytes = Files.readAllBytes(requested);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, "HEAD".equalsIgnoreCase(exchange.getRequestMethod()) ? -1 : bytes.length);
        if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private Map<String, Object> body(HttpExchange exchange) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length > 64 * 1024) throw new IllegalArgumentException("Request body is too large.");
        if (bytes.length == 0) return new LinkedHashMap<>();
        try { return Json.object(new String(bytes, StandardCharsets.UTF_8)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Request body must be valid JSON."); }
    }

    private static String required(Map<String, Object> input, String key) {
        String value = optional(input, key, null);
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing required field: " + key);
        return value.trim();
    }

    private static String optional(Map<String, Object> input, String key, String fallback) {
        Object value = input.get(key);
        return value == null ? fallback : String.valueOf(value).trim();
    }

    private static String validDate(String value, String field) {
        try { LocalDate.parse(value); return value; }
        catch (DateTimeParseException ex) { throw new IllegalArgumentException(field + " must use YYYY-MM-DD."); }
    }

    private static String dateParam(String value, String field) {
        if (value == null || value.isBlank()) return "";
        return validDate(value, field);
    }

    private static void validTime(String value, String field) {
        if (!value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) throw new IllegalArgumentException(field + " must use 24-hour HH:MM format.");
    }

    private static String segment(String path, int index) {
        String[] parts = path.split("/");
        if (parts.length <= index) return "";
        return decode(parts[index]);
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return result;
        for (String item : raw.split("&")) {
            String[] pair = item.split("=", 2);
            result.put(decode(pair[0]), pair.length > 1 ? decode(pair[1]) : "");
        }
        return result;
    }

    private static String decode(String value) {
        try { return URLDecoder.decode(value, StandardCharsets.UTF_8.name()); }
        catch (Exception ex) { return value; }
    }

    private static void cors(HttpExchange exchange) {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Access-Control-Allow-Origin", "*");
        headers.set("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type,Authorization");
    }

    private static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = Json.stringify(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void send(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String contentType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "text/javascript; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static Map<String, Object> error(String message) { return mapOf("error", message == null ? "Request failed." : message); }
    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }
}
```


### `src/main/java/ph/motorph/ms2/DemoStore.java`

```java
package ph.motorph.ms2;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

final class DemoStore {
    private final Path dataFile;
    private Map<String, Object> data;
    private final AtomicInteger employeeSequence = new AtomicInteger(4);
    private final AtomicInteger attendanceSequence = new AtomicInteger(1);
    private final AtomicInteger leaveSequence = new AtomicInteger(1);
    private final AtomicInteger ticketSequence = new AtomicInteger(1);

    DemoStore(Path dataFile) throws IOException {
        this.dataFile = dataFile;
        Path parent = dataFile.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        if (Files.exists(dataFile)) {
            try { data = Json.object(Files.readString(dataFile, StandardCharsets.UTF_8)); }
            catch (Exception ex) { throw new IOException("Cannot load local demo data at " + dataFile + ": " + ex.getMessage(), ex); }
        } else {
            data = seed();
            save();
        }
        initialiseSequences();
    }

    synchronized Map<String, Object> snapshot() { return deepCopyMap(data); }
    synchronized List<Map<String, Object>> employees() { return maps(data.get("employees")); }
    synchronized List<Map<String, Object>> attendance() { return maps(data.get("attendance")); }
    synchronized List<Map<String, Object>> leaveRequests() { return maps(data.get("leaveRequests")); }
    synchronized List<Map<String, Object>> tickets() { return maps(data.get("tickets")); }

    synchronized Map<String, Object> employee(String id) {
        for (Map<String, Object> employee : employees()) if (id.equals(str(employee.get("employeeId")))) return employee;
        return null;
    }

    synchronized Map<String, Object> createEmployee(Map<String, Object> values) throws IOException {
        Map<String, Object> employee = new LinkedHashMap<>();
        employee.put("employeeId", String.format("DEMO-%03d", employeeSequence.getAndIncrement()));
        employee.putAll(values);
        employee.put("scheduleId", null);
        employee.put("createdAt", java.time.Instant.now().toString());
        rawList("employees").add(employee);
        save();
        return employee;
    }

    synchronized Map<String, Object> updateEmployee(String id, Map<String, Object> values) throws IOException {
        Map<String, Object> employee = employee(id);
        if (employee == null) return null;
        employee.putAll(values);
        employee.put("updatedAt", java.time.Instant.now().toString());
        save();
        return employee;
    }

    synchronized Map<String, Object> createAttendance(Map<String, Object> values) throws IOException {
        Map<String, Object> record = new LinkedHashMap<>(values);
        record.put("attendanceId", nextAttendanceId());
        record.put("createdAt", java.time.Instant.now().toString());
        rawList("attendance").add(record);
        save();
        return record;
    }

    synchronized Map<String, Object> createLeave(Map<String, Object> values) throws IOException {
        Map<String, Object> record = new LinkedHashMap<>(values);
        record.put("requestId", String.format("LV-%04d", leaveSequence.getAndIncrement()));
        record.put("status", "Pending review");
        record.put("createdAt", java.time.Instant.now().toString());
        rawList("leaveRequests").add(record);
        save();
        return record;
    }

    synchronized Map<String, Object> cancelLeave(String id) throws IOException {
        for (Map<String, Object> record : leaveRequests()) {
            if (id.equals(str(record.get("requestId")))) {
                if (!"Pending review".equals(str(record.get("status")))) throw new IllegalArgumentException("Only pending requests can be cancelled.");
                record.put("status", "Cancelled");
                record.put("updatedAt", java.time.Instant.now().toString());
                save();
                return record;
            }
        }
        return null;
    }

    synchronized Map<String, Object> createTicket(Map<String, Object> values) throws IOException {
        Map<String, Object> record = new LinkedHashMap<>(values);
        record.put("ticketId", String.format("TK-%04d", ticketSequence.getAndIncrement()));
        record.put("status", "Open");
        record.put("createdAt", java.time.Instant.now().toString());
        rawList("tickets").add(record);
        save();
        return record;
    }

    synchronized Map<String, Object> cancelTicket(String id) throws IOException {
        for (Map<String, Object> record : tickets()) {
            if (id.equals(str(record.get("ticketId")))) {
                if (!"Open".equals(str(record.get("status")))) throw new IllegalArgumentException("Only open tickets can be cancelled.");
                record.put("status", "Cancelled");
                record.put("updatedAt", java.time.Instant.now().toString());
                save();
                return record;
            }
        }
        return null;
    }

    synchronized void reset() throws IOException {
        data = seed();
        employeeSequence.set(4);
        attendanceSequence.set(1);
        leaveSequence.set(1);
        ticketSequence.set(1);
        save();
    }

    private void initialiseSequences() {
        employeeSequence.set(Math.max(4, employees().size() + 1));
        int maxAttendance = 0;
        for (Map<String, Object> row : attendance()) maxAttendance = Math.max(maxAttendance, integer(row.get("attendanceId"), 0));
        attendanceSequence.set(maxAttendance + 1);
        leaveSequence.set(leaveRequests().size() + 1);
        ticketSequence.set(tickets().size() + 1);
    }

    private int nextAttendanceId() { return attendanceSequence.getAndIncrement(); }

    @SuppressWarnings("unchecked")
    private List<Object> rawList(String key) {
        Object value = data.get(key);
        if (!(value instanceof List)) {
            List<Object> created = new ArrayList<>();
            data.put(key, created);
            return created;
        }
        return (List<Object>) value;
    }

    private void save() throws IOException {
        String json = Json.stringify(data);
        Path temp = dataFile.resolveSibling(dataFile.getFileName().toString() + ".tmp");
        Files.writeString(temp, json, StandardCharsets.UTF_8);
        try { Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (IOException ex) { Files.move(temp, dataFile, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static Map<String, Object> seed() {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> employees = new ArrayList<>();
        employees.add(employee("DEMO-001", "Mara Rivera", "People Operations", "HR Coordinator", "Makati", "Active", 101));
        employees.add(employee("DEMO-002", "Eli Santos", "Finance", "Payroll Associate", "Taguig", "Active", 102));
        employees.add(employee("DEMO-003", "Noa Reyes", "Operations", "Operations Lead", "Quezon City", "On leave", 103));
        root.put("employees", employees);

        List<Map<String, Object>> attendance = new ArrayList<>();
        attendance.add(attendance(1, 101, "2026-10-01", "08:26", "17:03", "Regular"));
        attendance.add(attendance(2, 101, "2026-10-02", "08:31", "17:00", "Regular"));
        attendance.add(attendance(3, 101, "2026-11-02", "08:24", "17:02", "Regular"));
        attendance.add(attendance(4, 102, "2026-10-01", "08:17", "17:10", "Regular"));
        attendance.add(attendance(5, 102, "2026-10-02", "08:35", "17:00", "Regular"));
        attendance.add(attendance(6, 103, "2026-10-01", "08:28", "17:01", "Regular"));
        root.put("attendance", attendance);
        root.put("leaveRequests", new ArrayList<Map<String, Object>>());
        root.put("tickets", new ArrayList<Map<String, Object>>());
        root.put("meta", mapOf("mode", "LOCAL_SYNTHETIC", "seedVersion", 1, "createdAt", java.time.Instant.now().toString()));
        return root;
    }

    private static Map<String, Object> employee(String id, String name, String department, String role, String location, String status, Integer scheduleId) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("employeeId", id);
        row.put("name", name);
        row.put("department", department);
        row.put("role", role);
        row.put("location", location);
        row.put("status", status);
        row.put("scheduleId", scheduleId);
        return row;
    }

    private static Map<String, Object> attendance(int id, int scheduleId, String date, String timeIn, String timeOut, String dayType) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("attendanceId", id);
        row.put("scheduleId", scheduleId);
        row.put("date", date);
        row.put("timeIn", timeIn);
        row.put("timeOut", timeOut);
        row.put("dayType", dayType);
        return row;
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List) for (Object item : (List<?>) value) if (item instanceof Map) result.add((Map<String, Object>) item);
        return result;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> deepCopyMap(Map<String, Object> source) {
        return (Map<String, Object>) Json.parse(Json.stringify(source));
    }

    static String str(Object value) { return value == null ? "" : String.valueOf(value); }
    static int integer(Object value, int fallback) {
        try { return new BigDecimal(String.valueOf(value)).intValueExact(); }
        catch (Exception ex) { return fallback; }
    }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }
}
```


### `src/main/java/ph/motorph/ms2/Json.java`

```java
package ph.motorph.ms2;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static Object parse(String input) {
        Parser parser = new Parser(input == null ? "" : input);
        Object value = parser.value();
        parser.space();
        if (!parser.end()) throw new IllegalArgumentException("Unexpected content after JSON value.");
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> object(String input) {
        Object parsed = parse(input);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("A JSON object is required.");
        return (Map<String, Object>) parsed;
    }

    static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        write(value, out);
        return out.toString();
    }

    private static void write(Object value, StringBuilder out) {
        if (value == null) { out.append("null"); return; }
        if (value instanceof String) { quote((String) value, out); return; }
        if (value instanceof Number || value instanceof Boolean) { out.append(value); return; }
        if (value instanceof Map) {
            out.append('{');
            boolean first = true;
            for (Object entryObject : ((Map<?, ?>) value).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entryObject;
                if (!first) out.append(',');
                first = false;
                quote(String.valueOf(entry.getKey()), out);
                out.append(':');
                write(entry.getValue(), out);
            }
            out.append('}');
            return;
        }
        if (value instanceof Iterable) {
            out.append('[');
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) out.append(',');
                first = false;
                write(item, out);
            }
            out.append(']');
            return;
        }
        quote(String.valueOf(value), out);
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        out.append('"');
    }

    private static final class Parser {
        private final String source;
        private int index;
        Parser(String source) { this.source = source; }
        boolean end() { return index >= source.length(); }
        void space() { while (!end() && Character.isWhitespace(source.charAt(index))) index++; }

        Object value() {
            space();
            if (end()) throw error("Expected a JSON value.");
            char c = source.charAt(index);
            if (c == '{') return objectValue();
            if (c == '[') return arrayValue();
            if (c == '"') return stringValue();
            if (c == 't') return literal("true", Boolean.TRUE);
            if (c == 'f') return literal("false", Boolean.FALSE);
            if (c == 'n') return literal("null", null);
            if (c == '-' || Character.isDigit(c)) return numberValue();
            throw error("Unexpected character.");
        }

        private Map<String, Object> objectValue() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            space();
            if (!end() && source.charAt(index) == '}') { index++; return result; }
            while (true) {
                space();
                if (end() || source.charAt(index) != '"') throw error("Expected an object key.");
                String key = stringValue();
                space();
                if (end() || source.charAt(index++) != ':') throw error("Expected ':' after an object key.");
                result.put(key, value());
                space();
                if (end()) throw error("Unterminated object.");
                char c = source.charAt(index++);
                if (c == '}') return result;
                if (c != ',') throw error("Expected ',' or '}' in object.");
            }
        }

        private List<Object> arrayValue() {
            index++;
            List<Object> result = new ArrayList<>();
            space();
            if (!end() && source.charAt(index) == ']') { index++; return result; }
            while (true) {
                result.add(value());
                space();
                if (end()) throw error("Unterminated array.");
                char c = source.charAt(index++);
                if (c == ']') return result;
                if (c != ',') throw error("Expected ',' or ']' in array.");
            }
        }

        private String stringValue() {
            index++;
            StringBuilder result = new StringBuilder();
            while (!end()) {
                char c = source.charAt(index++);
                if (c == '"') return result.toString();
                if (c == '\\') {
                    if (end()) throw error("Unterminated escape.");
                    char escaped = source.charAt(index++);
                    switch (escaped) {
                        case '"': result.append('"'); break;
                        case '\\': result.append('\\'); break;
                        case '/': result.append('/'); break;
                        case 'b': result.append('\b'); break;
                        case 'f': result.append('\f'); break;
                        case 'n': result.append('\n'); break;
                        case 'r': result.append('\r'); break;
                        case 't': result.append('\t'); break;
                        case 'u':
                            if (index + 4 > source.length()) throw error("Invalid Unicode escape.");
                            try { result.append((char) Integer.parseInt(source.substring(index, index + 4), 16)); }
                            catch (NumberFormatException ex) { throw error("Invalid Unicode escape."); }
                            index += 4;
                            break;
                        default: throw error("Invalid escape character.");
                    }
                } else {
                    if (c < 0x20) throw error("Control character in string.");
                    result.append(c);
                }
            }
            throw error("Unterminated string.");
        }

        private Object numberValue() {
            int start = index;
            if (source.charAt(index) == '-') index++;
            while (!end() && Character.isDigit(source.charAt(index))) index++;
            if (!end() && source.charAt(index) == '.') {
                index++;
                while (!end() && Character.isDigit(source.charAt(index))) index++;
            }
            if (!end() && (source.charAt(index) == 'e' || source.charAt(index) == 'E')) {
                index++;
                if (!end() && (source.charAt(index) == '+' || source.charAt(index) == '-')) index++;
                while (!end() && Character.isDigit(source.charAt(index))) index++;
            }
            try { return new BigDecimal(source.substring(start, index)); }
            catch (NumberFormatException ex) { throw error("Invalid number."); }
        }

        private Object literal(String expected, Object result) {
            if (!source.startsWith(expected, index)) throw error("Invalid literal.");
            index += expected.length();
            return result;
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " (character " + index + ")");
        }
    }
}
```


### `src/main/java/ph/motorph/ms2/PayrollCalculator2026.java`

```java
package ph.motorph.ms2;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** Monthly Philippine statutory estimate profile, labelled provisional for project use. */
public final class PayrollCalculator2026 {
    public static final String PROFILE_ID = "PH-STATUTORY-2026-MONTHLY-PROVISIONAL";
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal ONE = new BigDecimal("1.00");

    public Map<String, Object> calculate(Map<String, Object> input) {
        BigDecimal cashPay = money(input, "cashPay");
        BigDecimal sssBase = money(input, "sssCompensation");
        BigDecimal philHealthBase = money(input, "philHealthMbs");
        BigDecimal pagibigBase = money(input, "pagibigFundSalary");
        BigDecimal taxableBaseBeforeContributions = money(input, "taxableCompensation");

        BigDecimal sssMsc = mapSssMsc(sssBase);
        BigDecimal sssRegularMsc = sssMsc.min(new BigDecimal("20000.00"));
        BigDecimal sssMpfMsc = sssMsc.subtract(sssRegularMsc).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sssEmployeeRegularShare = percent(sssRegularMsc, "0.05");
        BigDecimal sssEmployeeMpfShare = percent(sssMpfMsc, "0.05");
        BigDecimal sssEmployerRegularShare = percent(sssRegularMsc, "0.10");
        BigDecimal sssEmployerMpfShare = percent(sssMpfMsc, "0.10");
        BigDecimal sssEmployee = sssEmployeeRegularShare.add(sssEmployeeMpfShare).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sssEmployer = sssEmployerRegularShare.add(sssEmployerMpfShare).setScale(2, RoundingMode.HALF_UP);
        BigDecimal sssEc = sssMsc.compareTo(new BigDecimal("15000")) < 0
                ? new BigDecimal("10.00") : new BigDecimal("30.00");

        BigDecimal philHealthMbsApplied = clamp(philHealthBase, new BigDecimal("10000"), new BigDecimal("100000"));
        BigDecimal philHealthTotal = percent(philHealthMbsApplied, "0.05");
        BigDecimal philHealthEmployee = half(philHealthTotal);
        BigDecimal philHealthEmployer = philHealthTotal.subtract(philHealthEmployee).setScale(2, RoundingMode.HALF_UP);

        BigDecimal pagibigSalaryApplied = clamp(pagibigBase, ZERO, new BigDecimal("10000"));
        BigDecimal pagibigEmployeeRate = pagibigSalaryApplied.compareTo(new BigDecimal("1500")) <= 0
                ? new BigDecimal("0.01") : new BigDecimal("0.02");
        BigDecimal pagibigEmployee = percent(pagibigSalaryApplied, pagibigEmployeeRate);
        BigDecimal pagibigEmployer = percent(pagibigSalaryApplied, "0.02");

        BigDecimal employeeStatutory = sssEmployee.add(philHealthEmployee).add(pagibigEmployee).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAfterContributions = taxableBaseBeforeContributions.subtract(employeeStatutory).max(ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal withholdingTax = birMonthlyTax(taxableAfterContributions);
        BigDecimal totalEmployeeDeductions = employeeStatutory.add(withholdingTax).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netEstimate = cashPay.subtract(totalEmployeeDeductions).setScale(2, RoundingMode.HALF_UP);
        if (netEstimate.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("The entered employee deductions exceed cash pay. Review the five input bases.");
        }

        BigDecimal employerStatutory = sssEmployer.add(sssEc).add(philHealthEmployer).add(pagibigEmployer).setScale(2, RoundingMode.HALF_UP);
        BigDecimal employerCost = cashPay.add(employerStatutory).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profileId", PROFILE_ID);
        result.put("period", "MONTHLY");
        result.put("currency", "PHP");
        result.put("inputs", mapOf(
                "cashPay", cashPay,
                "sssCompensation", sssBase,
                "philHealthMbs", philHealthBase,
                "pagibigFundSalary", pagibigBase,
                "taxableCompensation", taxableBaseBeforeContributions));
        result.put("contributions", mapOf(
                "sssMsc", sssMsc,
                "sssRegularMsc", sssRegularMsc,
                "sssMpfMsc", sssMpfMsc,
                "sssEmployeeRegularShare", sssEmployeeRegularShare,
                "sssEmployeeMpfShare", sssEmployeeMpfShare,
                "sssEmployee", sssEmployee,
                "sssEmployerRegularShare", sssEmployerRegularShare,
                "sssEmployerMpfShare", sssEmployerMpfShare,
                "sssEmployer", sssEmployer,
                "sssEmployerEc", sssEc,
                "philHealthMbsApplied", philHealthMbsApplied,
                "philHealthTotal", philHealthTotal,
                "philHealthEmployee", philHealthEmployee,
                "philHealthEmployer", philHealthEmployer,
                "pagibigFundSalaryApplied", pagibigSalaryApplied,
                "pagibigEmployeeRate", pagibigEmployeeRate,
                "pagibigEmployee", pagibigEmployee,
                "pagibigEmployer", pagibigEmployer));
        result.put("taxableCompensationAfterEmployeeContributions", taxableAfterContributions);
        result.put("withholdingTax", withholdingTax);
        result.put("employeeStatutoryContributions", employeeStatutory);
        result.put("totalEmployeeDeductions", totalEmployeeDeductions);
        result.put("netPayEstimate", netEstimate);
        result.put("employerStatutoryCost", employerStatutory);
        result.put("employerCostEstimate", employerCost);
        result.put("isPersisted", false);
        result.put("isProductionPayroll", false);
        result.put("notes", new String[] {
                "Monthly statutory preview only. This does not create a payroll run or payslip.",
                "Cash pay, SSS compensation, PhilHealth MBS, Pag-IBIG Fund Salary, and taxable compensation are separate inputs.",
                "No attendance, cutoff allocation, overtime, holiday premium, leave, bonus, reimbursement, or other deduction is inferred.",
                "SSS, PhilHealth, and Pag-IBIG employer shares are employer cost, not employee deductions."
        });
        return result;
    }

    static BigDecimal mapSssMsc(BigDecimal compensation) {
        BigDecimal minMsc = new BigDecimal("5000");
        BigDecimal maxMsc = new BigDecimal("35000");
        if (compensation.compareTo(new BigDecimal("5250")) < 0) return minMsc;
        BigDecimal steps = compensation.subtract(new BigDecimal("4750"))
                .divide(new BigDecimal("500"), 0, RoundingMode.FLOOR);
        return minMsc.add(steps.multiply(new BigDecimal("500"))).min(maxMsc).max(minMsc).setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal birMonthlyTax(BigDecimal taxable) {
        BigDecimal amount;
        if (taxable.compareTo(new BigDecimal("20833")) <= 0) {
            amount = ZERO;
        } else if (taxable.compareTo(new BigDecimal("33333")) < 0) {
            amount = taxable.subtract(new BigDecimal("20833")).multiply(new BigDecimal("0.15"));
        } else if (taxable.compareTo(new BigDecimal("66667")) < 0) {
            amount = new BigDecimal("1875.00").add(taxable.subtract(new BigDecimal("33333")).multiply(new BigDecimal("0.20")));
        } else if (taxable.compareTo(new BigDecimal("166667")) < 0) {
            amount = new BigDecimal("8541.80").add(taxable.subtract(new BigDecimal("66667")).multiply(new BigDecimal("0.25")));
        } else if (taxable.compareTo(new BigDecimal("666667")) < 0) {
            amount = new BigDecimal("33541.80").add(taxable.subtract(new BigDecimal("166667")).multiply(new BigDecimal("0.30")));
        } else {
            amount = new BigDecimal("183541.80").add(taxable.subtract(new BigDecimal("666667")).multiply(new BigDecimal("0.35")));
        }
        return amount.max(ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(Map<String, Object> input, String key) {
        Object value = input.get(key);
        if (value == null || String.valueOf(value).trim().isEmpty()) throw new IllegalArgumentException("Missing required amount: " + key);
        BigDecimal parsed;
        try { parsed = new BigDecimal(String.valueOf(value).replace(",", "").trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException(key + " must be a valid number."); }
        if (parsed.signum() < 0) throw new IllegalArgumentException(key + " cannot be negative.");
        if (parsed.scale() > 2) parsed = parsed.setScale(2, RoundingMode.HALF_UP);
        return parsed.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal percent(BigDecimal base, String rate) { return percent(base, new BigDecimal(rate)); }
    private static BigDecimal percent(BigDecimal base, BigDecimal rate) { return base.multiply(rate).setScale(2, RoundingMode.HALF_UP); }
    private static BigDecimal half(BigDecimal amount) { return amount.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP); }
    private static BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) { return value.max(min).min(max).setScale(2, RoundingMode.HALF_UP); }

    private static Map<String, Object> mapOf(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) map.put(String.valueOf(pairs[i]), pairs[i + 1]);
        return map;
    }
}
```


### `src/test/java/ph/motorph/ms2/SmokeTest.java`

```java
package ph.motorph.ms2;

import java.math.BigDecimal;
import java.net.URI;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SmokeTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        testJsonRoundTrip();
        testPayrollRules();
        testPayrollBoundaries();
        testHttpWorkflows();
        System.out.println("PASS: " + checks + " MotorPH checks");
    }

    private static void testJsonRoundTrip() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", "Mara \"M\" Rivera"); value.put("amount", new BigDecimal("1234.50")); value.put("active", true);
        Object parsed = Json.parse(Json.stringify(value));
        check(parsed instanceof Map, "JSON object round trip");
        check("Mara \"M\" Rivera".equals(((Map<?, ?>) parsed).get("name")), "JSON escaping");
        check(new BigDecimal("1234.50").compareTo((BigDecimal) ((Map<?, ?>) parsed).get("amount")) == 0, "JSON decimal");
    }

    private static void testPayrollRules() {
        PayrollCalculator2026 calculator = new PayrollCalculator2026();
        Map<String, Object> input = payrollInput("25000", "25000", "25000", "25000", "25000");
        Map<String, Object> result = calculator.calculate(input);
        check(amount(result.get("netPayEstimate"), "22611.20"), "PHP 25,000 monthly net estimate");
        check(amount(result.get("withholdingTax"), "313.80"), "monthly BIR withholding");
        check(amount(result.get("employeeStatutoryContributions"), "2075.00"), "employee statutory shares");
        check(amount(result.get("employerStatutoryCost"), "3355.00"), "employer statutory cost");
        check(Boolean.FALSE.equals(result.get("isPersisted")), "preview is not persisted");
        check("MONTHLY".equals(result.get("period")), "monthly profile");
    }

    private static void testPayrollBoundaries() {
        check(amount(PayrollCalculator2026.mapSssMsc(new BigDecimal("5249.99")), "5000.00"), "SSS first MSC band");
        check(amount(PayrollCalculator2026.mapSssMsc(new BigDecimal("5250.00")), "5500.00"), "SSS next MSC band");
        check(amount(PayrollCalculator2026.mapSssMsc(new BigDecimal("100000.00")), "35000.00"), "SSS maximum MSC cap");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("20833")), "0.00"), "BIR zero-tax lower boundary");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("25000")), "625.05"), "BIR 15 percent monthly bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("50000")), "5208.40"), "BIR 20 percent monthly bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("33332.50")), "1874.93"), "BIR fractional amount remains in 15 percent bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("66666.50")), "8541.70"), "BIR fractional amount remains in 20 percent bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("100000")), "16875.05"), "BIR 25 percent monthly bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("200000")), "43541.70"), "BIR 30 percent monthly bracket");
        check(amount(PayrollCalculator2026.birMonthlyTax(new BigDecimal("700000")), "195208.35"), "BIR 35 percent monthly bracket");
        boolean rejected = false;
        try { new PayrollCalculator2026().calculate(payrollInput("1", "25000", "25000", "25000", "25000")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "negative net estimate rejected");
        rejected = false;
        try { new PayrollCalculator2026().calculate(payrollInput("1", "-1", "25000", "25000", "25000")); }
        catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "negative statutory basis rejected");
    }

    private static void testHttpWorkflows() throws Exception {
        Path temp = Files.createTempDirectory("motorph-ms2-test-");
        DemoStore store = new DemoStore(temp.resolve("demo-data.json"));
        ApiServer server = new ApiServer(store, Path.of("web"));
        server.start("127.0.0.1", 0);
        try {
            HttpClient client = HttpClient.newHttpClient();
            String base = "http://127.0.0.1:" + server.port();
            HttpResponse<String> health = request(client, "GET", base + "/api/health", null);
            check(health.statusCode() == 200 && health.body().contains("LOCAL_SYNTHETIC"), "health endpoint");
            HttpResponse<String> employeeList = request(client, "GET", base + "/api/employees", null);
            check(employeeList.statusCode() == 200 && employeeList.body().contains("DEMO-001"), "employee list endpoint");
            HttpResponse<String> employeeCreate = request(client, "POST", base + "/api/employees", "{\"name\":\"Test Employee\",\"department\":\"QA\",\"role\":\"Tester\",\"location\":\"Makati\",\"status\":\"Active\"}");
            check(employeeCreate.statusCode() == 201 && employeeCreate.body().contains("DEMO-004"), "employee create endpoint");
            HttpResponse<String> attendance = request(client, "GET", base + "/api/attendance?employeeId=DEMO-001", null);
            check(attendance.statusCode() == 200 && attendance.body().contains("hoursCalculated\":false"), "raw attendance endpoint");
            String today = LocalDate.now().plusDays(1).toString();
            String attendanceJson = "{\"employeeId\":\"DEMO-001\",\"date\":\"" + today + "\",\"timeIn\":\"08:30\",\"timeOut\":\"17:00\",\"dayType\":\"Regular\"}";
            HttpResponse<String> attendanceCreate = request(client, "POST", base + "/api/attendance", attendanceJson);
            check(attendanceCreate.statusCode() == 201, "attendance create endpoint");
            HttpResponse<String> duplicate = request(client, "POST", base + "/api/attendance", attendanceJson);
            check(duplicate.statusCode() == 400, "duplicate attendance blocked");
            String leaveJson = "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"" + today + "\",\"endDate\":\"" + LocalDate.parse(today).plusDays(1) + "\",\"note\":\"Test\"}";
            HttpResponse<String> leave = request(client, "POST", base + "/api/leave-requests", leaveJson);
            check(leave.statusCode() == 201 && leave.body().contains("Pending review"), "leave submission");
            HttpResponse<String> workflows = request(client, "GET", base + "/api/workflows", null);
            check(workflows.statusCode() == 200 && workflows.body().contains("LV-0001"), "workflow listing");
            HttpResponse<String> cancelLeave = request(client, "POST", base + "/api/leave-requests/LV-0001/cancel", "{}");
            check(cancelLeave.statusCode() == 200 && cancelLeave.body().contains("Cancelled"), "leave cancellation");
            HttpResponse<String> ticket = request(client, "POST", base + "/api/tickets", "{\"employeeId\":\"DEMO-002\",\"ticketType\":\"Payroll question\",\"summary\":\"Test ticket\"}");
            check(ticket.statusCode() == 201 && ticket.body().contains("Payroll"), "ticket submission and queue label");
            HttpResponse<String> cancelTicket = request(client, "POST", base + "/api/tickets/TK-0001/cancel", "{}");
            check(cancelTicket.statusCode() == 200 && cancelTicket.body().contains("Cancelled"), "ticket cancellation");
            HttpResponse<String> report = request(client, "GET", base + "/api/reports/attendance", null);
            check(report.statusCode() == 200 && report.body().contains("rawRecordCount"), "attendance coverage report");
            HttpResponse<String> payroll = request(client, "POST", base + "/api/payroll/preview", Json.stringify(payrollInput("25000", "25000", "25000", "25000", "25000")));
            check(payroll.statusCode() == 200 && payroll.body().contains("22611.20"), "payroll preview endpoint");
            HttpResponse<String> payrollBad = request(client, "POST", base + "/api/payroll/preview", "{\"cashPay\":-5}");
            check(payrollBad.statusCode() == 400, "payroll invalid input validation");
        } finally {
            server.stop();
        }
    }

    private static HttpResponse<String> request(HttpClient client, String method, String uri, String body) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri));
        if (body == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, Object> payrollInput(String cash, String sss, String phil, String pagibig, String taxable) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("cashPay", cash); input.put("sssCompensation", sss); input.put("philHealthMbs", phil);
        input.put("pagibigFundSalary", pagibig); input.put("taxableCompensation", taxable);
        return input;
    }

    private static boolean amount(Object actual, String expected) {
        return new BigDecimal(String.valueOf(actual)).compareTo(new BigDecimal(expected)) == 0;
    }
    private static void check(boolean passed, String label) {
        if (!passed) throw new AssertionError("FAIL: " + label);
        checks++;
        System.out.println("PASS: " + label);
    }
}
```


### `web/index.html`

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <meta name="theme-color" content="#f5f6f3">
  <title>MotorPH · Payroll Workspace</title>
  <link rel="stylesheet" href="/styles.css">
  <script src="/app.js" defer></script>
</head>
<body>
  <div class="app-shell">
    <aside class="sidebar" aria-label="Main navigation">
      <a class="brand" href="#overview" aria-label="MotorPH home">
        <span class="brand-mark">M</span>
        <span><strong>MotorPH</strong><small>PAYROLL OFFICE</small></span>
      </a>
      <div class="workspace-label">WORKSPACE</div>
      <nav class="nav-list">
        <button class="nav-item active" data-view="overview"><span class="nav-icon">▦</span>Overview</button>
        <button class="nav-item" data-view="employees"><span class="nav-icon">♙</span>Employees</button>
        <button class="nav-item" data-view="attendance"><span class="nav-icon">◷</span>Attendance</button>
        <button class="nav-item" data-view="requests"><span class="nav-icon">▤</span>Requests</button>
        <button class="nav-item" data-view="team"><span class="nav-icon">♧</span>Team</button>
        <button class="nav-item" data-view="hr"><span class="nav-icon">▣</span>HR Desk</button>
        <button class="nav-item" data-view="reports"><span class="nav-icon">▥</span>Reports</button>
        <button class="nav-item" data-view="payroll"><span class="nav-icon">₱</span>Payroll</button>
      </nav>
      <div class="sidebar-bottom">
        <div class="mode-card"><span class="mode-dot"></span><div><strong>LOCAL DEMO</strong><small>Synthetic records only</small></div></div>
        <button id="reset-demo" class="reset-button" type="button">Reset synthetic data</button>
        <div class="sidebar-version">MOTORPH WORKSPACE · LOCAL BUILD</div>
      </div>
    </aside>

    <main class="main-area">
      <header class="topbar">
        <div class="breadcrumb"><span>MotorPH</span><span class="crumb-slash">/</span><strong id="breadcrumb-current">Overview</strong></div>
        <div class="topbar-right"><span class="local-pill"><i></i> Synthetic workspace</span><span class="user-avatar">JD</span></div>
      </header>

      <div id="global-message" class="global-message" role="status" aria-live="polite" hidden></div>

      <section class="page-view active" id="overview-view">
        <div class="page-heading"><div><p class="eyebrow">WORKSPACE / SUMMARY</p><h1>Operations overview</h1><p class="page-intro">A current view of the synthetic employee and workflow records.</p></div><div class="heading-meta"><span class="meta-label">ENVIRONMENT</span><strong>Local demonstration</strong><small>Changes are saved to your local data file.</small></div></div>
        <div class="notice"><span class="notice-icon">i</span><p><strong>Development data only.</strong> The included employees, attendance entries, requests, and payroll inputs are fictional. Do not enter real employee or payroll information.</p></div>
        <div class="summary-grid">
          <article class="summary-card"><span class="summary-label">EMPLOYEE RECORDS</span><strong id="overview-employees">—</strong><small>Directory entries</small></article>
          <article class="summary-card"><span class="summary-label">RAW ATTENDANCE</span><strong id="overview-attendance">—</strong><small>Stored time records</small></article>
          <article class="summary-card"><span class="summary-label">PENDING LEAVE</span><strong id="overview-leave">—</strong><small>Awaiting review</small></article>
          <article class="summary-card"><span class="summary-label">OPEN TICKETS</span><strong id="overview-tickets">—</strong><small>Unresolved demo tickets</small></article>
        </div>
        <div class="panel-grid">
          <section class="panel"><div class="panel-heading"><div><p class="eyebrow">WORKSPACE</p><h2>Application areas</h2></div><span class="subtle-tag">8 MODULES</span></div><div class="module-list">
            <button class="module-row" data-go="employees"><span class="module-index">01</span><span><strong>Employee directory</strong><small>Search, create, inspect, and edit synthetic employee records.</small></span><b>→</b></button>
            <button class="module-row" data-go="attendance"><span class="module-index">02</span><span><strong>Attendance register</strong><small>Review raw clock records and add local demonstration entries.</small></span><b>→</b></button>
            <button class="module-row" data-go="requests"><span class="module-index">03</span><span><strong>Requests and tickets</strong><small>Submit, inspect, filter, export, and cancel demo requests.</small></span><b>→</b></button>
            <button class="module-row" data-go="payroll"><span class="module-index">04</span><span><strong>Payroll preview</strong><small>Estimate monthly statutory contributions from explicit inputs.</small></span><b>→</b></button>
          </div></section>
          <section class="panel"><div class="panel-heading"><div><p class="eyebrow">CURRENT BUILD</p><h2>System status</h2></div><span class="status-badge">RUNNING</span></div><div class="status-list">
            <div><span>Java API</span><strong class="status-ok">Connected</strong></div><div><span>Local data store</span><strong class="status-ok">Available</strong></div><div><span>Firebase account</span><strong class="status-muted">Not connected</strong></div><div><span>Payroll run persistence</span><strong class="status-muted">Not enabled</strong></div><div><span>Attendance hour derivation</span><strong class="status-muted">Not enabled</strong></div>
          </div><p class="panel-footnote">The local build is intended for feature testing. It is not a production payroll service.</p></section>
        </div>
      </section>

      <section class="page-view" id="employees-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">PEOPLE / DIRECTORY</p><h1>Employees</h1><p class="page-intro">Search and manage the fictional employee directory.</p></div><button class="button button-primary" id="add-employee-button">＋ Add employee</button></div>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">EMPLOYEE REGISTER</p><h2>Directory records</h2></div><button class="button button-quiet" id="employee-export">Export CSV</button></div>
          <div class="filters"><label class="filter-search"><span>SEARCH</span><input id="employee-search" type="search" placeholder="Name, ID, department, role, or location"></label><label><span>STATUS</span><select id="employee-status"><option value="">All statuses</option><option>Active</option><option>On leave</option><option>Inactive</option></select></label><label><span>SORT BY</span><select id="employee-sort"><option value="name">Name</option><option value="department">Department</option><option value="role">Position</option><option value="status">Status</option></select></label><label><span>ORDER</span><select id="employee-order"><option value="asc">A–Z / ascending</option><option value="desc">Z–A / descending</option></select></label></div>
          <div class="table-wrap"><table><thead><tr><th>EMPLOYEE</th><th>DEPARTMENT</th><th>POSITION</th><th>LOCATION</th><th>STATUS</th><th></th></tr></thead><tbody id="employee-rows"><tr><td colspan="6" class="table-message">Loading…</td></tr></tbody></table></div><div class="panel-footer"><span id="employee-count">Loading records</span><span>Local synthetic register</span></div>
        </section>
      </section>

      <section class="page-view" id="attendance-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">TIME / RECORDS</p><h1>Attendance</h1><p class="page-intro">Inspect recorded time-in and time-out values. This register does not calculate paid hours.</p></div><button class="button button-primary" id="add-attendance-toggle">＋ Add local entry</button></div>
        <section class="panel form-panel" id="attendance-form-panel" hidden><div class="panel-heading"><div><p class="eyebrow">SYNTHETIC INPUT</p><h2>Add attendance record</h2></div><button class="text-button" id="close-attendance-form" type="button">Close ×</button></div>
          <form id="attendance-form" class="form-grid"><label><span>EMPLOYEE</span><select id="attendance-employee" required></select></label><label><span>DATE</span><input id="attendance-date" type="date" required></label><label><span>TIME IN</span><input id="attendance-time-in" type="time" required value="08:30"></label><label><span>TIME OUT</span><input id="attendance-time-out" type="time" required value="17:00"></label><label><span>DAY CLASSIFICATION</span><select id="attendance-day-type"><option>Regular</option><option>Rest day</option><option>Holiday</option><option>Special non-working holiday</option><option>Special working day</option></select></label><div class="form-actions"><button class="button button-primary" type="submit">Save local record</button></div><p class="form-note">Only employees with an existing schedule mapping can receive a local demo record. Duplicate employee/date entries are blocked. Overnight shifts are not supported.</p></form>
        </section>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">RAW TIME RECORDS</p><h2>Attendance register</h2></div><button class="button button-quiet" id="attendance-export">Export CSV</button></div>
          <div class="filters"><label><span>EMPLOYEE</span><select id="attendance-filter-employee"></select></label><label><span>FROM</span><input id="attendance-from" type="date"></label><label><span>TO</span><input id="attendance-to" type="date"></label><label><span>SORT BY</span><select id="attendance-sort"><option value="date">Date</option><option value="timeIn">Time in</option><option value="timeOut">Time out</option></select></label><label><span>ORDER</span><select id="attendance-order"><option value="asc">Ascending</option><option value="desc">Descending</option></select></label><button class="button button-secondary" id="attendance-apply">Apply filters</button></div>
          <div class="attendance-summary"><div><span>RAW ROWS</span><strong id="attendance-summary-count">—</strong></div><div><span>FIRST DATE</span><strong id="attendance-summary-first">—</strong></div><div><span>LAST DATE</span><strong id="attendance-summary-last">—</strong></div></div>
          <div class="notice notice-small"><span class="notice-icon">i</span><p>Rows show the values stored in the record. No late minutes, work hours, undertime, overtime, or payroll amounts are inferred.</p></div>
          <div class="table-wrap"><table><thead><tr><th>DATE</th><th>EMPLOYEE</th><th>SCHEDULE</th><th>TIME IN</th><th>TIME OUT</th><th>DAY TYPE</th></tr></thead><tbody id="attendance-rows"><tr><td colspan="6" class="table-message">Choose an employee to load records.</td></tr></tbody></table></div><div class="panel-footer"><span id="attendance-count">No records loaded</span><span>Raw records / no hour derivation</span></div>
        </section>
      </section>

      <section class="page-view" id="requests-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">EMPLOYEE SERVICES / SUBMISSIONS</p><h1>Requests</h1><p class="page-intro">Submit local leave requests and help-desk tickets, then track or cancel them.</p></div><span class="subtle-tag">LOCAL WORKFLOW DEMO</span></div>
        <div class="two-column">
          <section class="panel"><div class="panel-heading"><div><p class="eyebrow">ABSENCE MANAGEMENT</p><h2>Leave request</h2></div></div>
            <form id="leave-form" class="stack-form"><label><span>EMPLOYEE</span><select id="leave-employee" required></select></label><label><span>LEAVE TYPE</span><select id="leave-type"><option>Vacation</option><option>Sick</option><option>Emergency</option><option>Personal</option><option>Other</option></select></label><div class="inline-fields"><label><span>START DATE</span><input id="leave-start" type="date" required></label><label><span>END DATE</span><input id="leave-end" type="date" required></label></div><label><span>NOTE <small>240 CHARACTERS MAX</small></span><textarea id="leave-note" rows="2" maxlength="240" placeholder="Optional synthetic note"></textarea></label><button class="button button-primary" type="submit">Submit demo request</button><p class="form-note">Submissions remain pending. This local demo has no reviewer, approval, leave-credit deduction, or notification workflow.</p></form>
          </section>
          <section class="panel calendar-panel"><div class="panel-heading"><div><p class="eyebrow">REQUEST DATES</p><h2>Leave calendar</h2></div><div class="calendar-controls"><button class="icon-button calendar-nav" id="calendar-prev" type="button" aria-label="Previous month">‹</button><strong id="calendar-month">—</strong><button class="icon-button calendar-nav" id="calendar-next" type="button" aria-label="Next month">›</button></div></div><div id="leave-calendar" class="calendar-grid" aria-label="Leave request calendar"></div><p class="form-note">Markers show submitted request dates only, including cancelled requests. Selecting two dates drafts the leave form range; it does not submit or imply approval.</p></section>
          <section class="panel"><div class="panel-heading"><div><p class="eyebrow">HELP DESK</p><h2>Ticket or dispute</h2></div></div>
            <form id="ticket-form" class="stack-form"><label><span>EMPLOYEE</span><select id="ticket-employee" required></select></label><label><span>TICKET TYPE</span><select id="ticket-type"><option>Timesheet correction</option><option>Payroll question</option><option>Employee record</option><option>Leave concern</option><option>Other</option></select></label><label><span>MESSAGE <small>240 CHARACTERS MAX</small></span><textarea id="ticket-summary" rows="3" maxlength="240" required placeholder="Describe a fictional demo issue"></textarea></label><button class="button button-primary" type="submit">Create demo ticket</button><p class="form-note">Queue labels are routing hints only. No reviewer receives this ticket in the local demo.</p></form>
          </section>
        </div>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">REQUEST HISTORY</p><h2>Leave requests</h2></div><button class="button button-quiet" id="leave-export">Export CSV</button></div><div class="filters"><label class="filter-search"><span>SEARCH</span><input id="leave-search" type="search" placeholder="ID, employee, type, or date"></label><label><span>STATUS</span><select id="leave-status"><option value="">Any status</option><option>Pending review</option><option>Cancelled</option></select></label><label><span>STARTING ON / AFTER</span><input id="leave-filter-from" type="date"></label><label><span>ENDING ON / BEFORE</span><input id="leave-filter-to" type="date"></label><label><span>SORT</span><select id="leave-sort"><option value="newest">Newest first</option><option value="oldest">Oldest first</option></select></label></div><div class="table-wrap"><table><thead><tr><th>REQUEST</th><th>EMPLOYEE</th><th>TYPE</th><th>DATES</th><th>STATUS</th><th></th></tr></thead><tbody id="leave-rows"><tr><td colspan="6" class="table-message">Loading…</td></tr></tbody></table></div></section>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">SUPPORT QUEUE</p><h2>Tickets</h2></div><button class="button button-quiet" id="ticket-export">Export CSV</button></div><div class="filters"><label class="filter-search"><span>SEARCH</span><input id="ticket-search" type="search" placeholder="ID, employee, type, or queue"></label><label><span>STATUS</span><select id="ticket-status"><option value="">Any status</option><option>Open</option><option>Cancelled</option></select></label><label><span>SORT</span><select id="ticket-sort"><option value="newest">Newest first</option><option value="oldest">Oldest first</option></select></label></div><div class="table-wrap"><table><thead><tr><th>TICKET</th><th>EMPLOYEE</th><th>TYPE</th><th>QUEUE LABEL</th><th>STATUS</th><th></th></tr></thead><tbody id="ticket-rows"><tr><td colspan="6" class="table-message">Loading…</td></tr></tbody></table></div></section>
      </section>

      <section class="page-view" id="team-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">ORGANISATION / PEOPLE</p><h1>Team</h1><p class="page-intro">A read-only roster view of the synthetic directory.</p></div><span class="subtle-tag">READ ONLY</span></div>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">TEAM DIRECTORY</p><h2>People roster</h2></div><button class="button button-quiet" id="team-export">Export CSV</button></div><div class="filters"><label class="filter-search"><span>SEARCH</span><input id="team-search" type="search" placeholder="ID, name, department, role, location"></label><label><span>STATUS</span><select id="team-status"><option value="">Any status</option><option>Active</option><option>On leave</option><option>Inactive</option></select></label><label><span>SORT BY</span><select id="team-sort"><option value="name">Name</option><option value="department">Department</option><option value="role">Position</option><option value="status">Status</option></select></label><label><span>ORDER</span><select id="team-order"><option value="asc">A–Z / ascending</option><option value="desc">Z–A / descending</option></select></label></div><div class="table-wrap"><table><thead><tr><th>EMPLOYEE</th><th>DEPARTMENT</th><th>POSITION</th><th>LOCATION</th><th>STATUS</th></tr></thead><tbody id="team-rows"></tbody></table></div><p class="panel-footnote">This view does not infer team membership, reporting lines, supervisor authority, or approval rights.</p></section>
      </section>

      <section class="page-view" id="hr-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">PEOPLE OPERATIONS / RECORDS</p><h1>HR Desk</h1><p class="page-intro">Maintain basic fictional employee directory fields.</p></div><button class="button button-primary" id="hr-add-employee">＋ Add employee</button></div>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">HR REGISTER</p><h2>Employee records</h2></div><span class="subtle-tag">NO DELETE</span></div><div class="filters"><label class="filter-search"><span>SEARCH</span><input id="hr-search" type="search" placeholder="ID, name, department, role, location"></label><label><span>STATUS</span><select id="hr-status"><option value="">Any status</option><option>Active</option><option>On leave</option><option>Inactive</option></select></label><label><span>SORT BY</span><select id="hr-sort"><option value="name">Name</option><option value="department">Department</option><option value="role">Position</option><option value="status">Status</option></select></label><label><span>ORDER</span><select id="hr-order"><option value="asc">A–Z / ascending</option><option value="desc">Z–A / descending</option></select></label></div><div class="table-wrap"><table><thead><tr><th>EMPLOYEE</th><th>DEPARTMENT</th><th>POSITION</th><th>LOCATION</th><th>STATUS</th><th></th></tr></thead><tbody id="hr-rows"></tbody></table></div><p class="panel-footnote">This local demo has no onboarding, offboarding, salary editing, audit trail, or role permission management.</p></section>
      </section>

      <section class="page-view" id="reports-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">ANALYTICS / EXPORTS</p><h1>Reports</h1><p class="page-intro">Raw attendance coverage by synthetic employee and selected date range.</p></div><span class="subtle-tag">RAW RECORDS ONLY</span></div>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">DIRECTORY SUMMARY</p><h2>Employee labels</h2></div><button class="button button-quiet" id="directory-summary-export">Export CSV</button></div><div id="directory-status-counts" class="directory-metrics"></div><div id="directory-department-counts" class="directory-breakdown"></div></section>
        <section class="panel"><div class="panel-heading"><div><p class="eyebrow">ATTENDANCE COVERAGE</p><h2>Record availability</h2></div><button class="button button-quiet" id="report-export">Export visible CSV</button></div><div class="filters"><label><span>FROM</span><input id="report-from" type="date"></label><label><span>TO</span><input id="report-to" type="date"></label><label class="filter-search"><span>SEARCH EMPLOYEE / SCHEDULE</span><input id="report-search" type="search" placeholder="Name, ID, schedule"></label><label><span>MAPPING</span><select id="report-mapping"><option value="all">All employees</option><option value="mapped">Mapped</option><option value="unmapped">Unmapped</option></select></label><label><span>SORT BY</span><select id="report-sort"><option value="employeeName">Employee</option><option value="rawRecordCount">Raw rows</option><option value="firstRecordDate">First entry</option><option value="lastRecordDate">Last entry</option></select></label><label><span>ORDER</span><select id="report-order"><option value="asc">Ascending</option><option value="desc">Descending</option></select></label><button class="button button-secondary" id="report-apply">Refresh report</button></div><div class="notice notice-small"><span class="notice-icon">i</span><p>A missing raw row is not proof of absence. This report counts stored entries and date coverage only; it does not calculate hours or payroll.</p></div><div class="table-wrap"><table><thead><tr><th>EMPLOYEE</th><th>SCHEDULE MAPPING</th><th>RAW ROWS</th><th>FIRST ENTRY</th><th>LAST ENTRY</th><th></th></tr></thead><tbody id="report-rows"></tbody></table></div><div class="panel-footer"><span id="report-count">Loading report</span><span>No hours calculated</span></div></section>
      </section>

      <section class="page-view" id="payroll-view" hidden>
        <div class="page-heading"><div><p class="eyebrow">PAY / CALCULATION</p><h1>Payroll preview</h1><p class="page-intro">Calculate a monthly statutory estimate using explicit synthetic monetary bases.</p></div><span class="subtle-tag">2026 MONTHLY PROFILE</span></div>
        <div class="notice notice-warning"><span class="notice-icon">!</span><p><strong>Estimate only, not a payroll run.</strong> This preview uses manually supplied bases. It does not read attendance, generate payslips, allocate semi-monthly cut-offs, or save results.</p></div>
        <div class="payroll-layout">
          <section class="panel"><div class="panel-heading"><div><p class="eyebrow">CALCULATOR INPUTS</p><h2>Monthly compensation bases</h2></div><button class="text-button" id="payroll-sample">Fill ₱25,000 sample</button></div>
            <form id="payroll-form" class="stack-form">
              <label><span>CASH PAY <small>AMOUNT AVAILABLE BEFORE EMPLOYEE DEDUCTIONS</small></span><input id="pay-cash" inputmode="decimal" type="number" min="0" step="0.01" value="25000" required></label>
              <label><span>SSS COMPENSATION <small>MONTHLY SSS BRACKET BASIS</small></span><input id="pay-sss" inputmode="decimal" type="number" min="0" step="0.01" value="25000" required></label>
              <label><span>PHILHEALTH MBS <small>MONTHLY BASIC SALARY, NOT ALL CASH PAY</small></span><input id="pay-philhealth" inputmode="decimal" type="number" min="0" step="0.01" value="25000" required></label>
              <label><span>PAG-IBIG FUND SALARY <small>SUBJECT TO ₱10,000 MAXIMUM FUND SALARY</small></span><input id="pay-pagibig" inputmode="decimal" type="number" min="0" step="0.01" value="25000" required></label>
              <label><span>TAXABLE COMPENSATION <small>MONTHLY TAXABLE BASE BEFORE EMPLOYEE STATUTORY SHARES</small></span><input id="pay-taxable" inputmode="decimal" type="number" min="0" step="0.01" value="25000" required></label>
              <button class="button button-primary button-wide" type="submit">Calculate monthly estimate</button>
            </form>
            <p class="form-note">The five bases are intentionally separate. The application does not assume that SSS compensation, PhilHealth MBS, Pag-IBIG Fund Salary, taxable compensation, and cash pay are interchangeable.</p>
          </section>
          <section class="panel result-panel" id="payroll-result-panel"><div class="panel-heading"><div><p class="eyebrow">CALCULATION OUTPUT</p><h2>Estimate breakdown</h2></div><button class="button button-quiet" id="payroll-export" disabled>Export preview CSV</button></div><div id="payroll-result" class="result-empty"><div class="empty-mark">₱</div><strong>No calculation yet</strong><p>Submit the five monthly input bases to see the employee deductions, employer contributions, and estimated net pay.</p></div></section>
        </div>
        <section class="panel sources-panel"><div class="panel-heading"><div><p class="eyebrow">RULE PROFILE</p><h2>Applied assumptions and references</h2></div><span class="subtle-tag">VERSIONED PROFILE</span></div><div class="rule-notes"><p><strong>SSS:</strong> 5% employee and 10% employer contribution on the applicable monthly salary credit, capped at ₱35,000 MSC, plus employer-only EC.</p><p><strong>PhilHealth:</strong> 5% of monthly basic salary, subject to ₱10,000 floor and ₱100,000 ceiling, split equally between employee and employer.</p><p><strong>Pag-IBIG:</strong> 1% employee share at Fund Salary up to ₱1,500, otherwise 2%, with 2% employer share and ₱10,000 maximum Fund Salary.</p><p><strong>BIR:</strong> Monthly withholding table from Annex E of RR 11-2018, effective 1 January 2023 onwards.</p><p><strong>Rounding:</strong> Monetary values are rounded half-up to two decimal places. Taxable compensation is reduced by the employee statutory shares before applying monthly withholding.</p></div><div class="source-links"><a href="https://www.sss.gov.ph/pay-contribution/" target="_blank" rel="noreferrer">SSS contribution schedule ↗</a><a href="https://www.philhealth.gov.ph/advisories/2025/PA2025-0002.pdf" target="_blank" rel="noreferrer">PhilHealth CY2025 advisory ↗</a><a href="https://www.pagibigfund.gov.ph/documents/Circular%20No.%20460%20-%20Guidelines%20on%20the%20Pag-IBIG%20Fund's%20Implementation%20of%20Increase%20in%20the%20MFS%20Effective%20February%202024.pdf" target="_blank" rel="noreferrer">Pag-IBIG Circular 460 ↗</a><a href="https://bir-cdn.bir.gov.ph/local/pdf/Annex%20E%20RR%2011-2018.pdf" target="_blank" rel="noreferrer">BIR Annex E ↗</a></div></section>
      </section>

      <footer class="app-footer"><span>MotorPH Payroll Workspace</span><span>Local synthetic build · No real employee records</span></footer>
    </main>
  </div>

  <dialog id="employee-dialog" class="dialog">
    <form id="employee-form" method="dialog">
      <div class="dialog-heading"><div><p class="eyebrow">DIRECTORY RECORD</p><h2 id="employee-dialog-title">Add employee</h2></div><button class="icon-button" type="button" id="employee-dialog-close" aria-label="Close dialog">×</button></div>
      <input type="hidden" id="employee-id">
      <div class="stack-form"><label><span>FULL NAME</span><input id="employee-name" maxlength="100" required></label><label><span>DEPARTMENT</span><input id="employee-department" maxlength="80" required></label><label><span>POSITION</span><input id="employee-role" maxlength="80" required></label><label><span>LOCATION</span><input id="employee-location" maxlength="80" required></label><label><span>STATUS</span><select id="employee-status-edit"><option>Active</option><option>On leave</option><option>Inactive</option></select></label></div>
      <div class="dialog-actions"><button class="button button-secondary" type="button" id="employee-dialog-cancel">Cancel</button><button class="button button-primary" type="submit">Save employee</button></div>
    </form>
  </dialog>
  <dialog id="employee-detail-dialog" class="dialog"><div class="dialog-heading"><div><p class="eyebrow">EMPLOYEE DETAILS</p><h2 id="employee-detail-title">Employee</h2></div><button class="icon-button" type="button" id="employee-detail-close" aria-label="Close details">×</button></div><div id="employee-detail-content" class="detail-grid"></div><div class="dialog-actions"><button class="button button-secondary" type="button" id="employee-detail-dismiss">Close</button></div></dialog>
  <div id="toast" class="toast" role="status" aria-live="polite" hidden></div>
</body>
</html>
```


### `web/styles.css`

```css
:root{--bg:#f4f5f1;--panel:#fff;--ink:#202821;--muted:#737d73;--line:#e2e6df;--line-dark:#cbd2c9;--green:#285647;--green-2:#e7f0eb;--amber:#8e6428;--amber-bg:#faf1df;--red:#a3463e;--shadow:0 10px 30px rgba(29,49,39,.055);--mono:'DM Mono',monospace;--sans:'DM Sans',Arial,sans-serif}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:14px/1.5 var(--sans)}button,input,select,textarea{font:inherit}button{cursor:pointer}button:disabled{opacity:.5;cursor:not-allowed}a{color:inherit}.app-shell{min-height:100vh;display:grid;grid-template-columns:238px minmax(0,1fr)}.sidebar{background:#202d28;color:#eef4ee;display:flex;flex-direction:column;min-height:100vh;padding:25px 16px 18px;position:sticky;top:0;height:100vh}.brand{display:flex;align-items:center;gap:11px;text-decoration:none;padding:0 10px 31px}.brand-mark{height:34px;width:34px;border:1px solid #81958a;display:grid;place-items:center;font-weight:700;font-size:17px;color:#fff}.brand strong{display:block;letter-spacing:.04em;font-size:15px}.brand small{display:block;color:#aab8af;font:9px var(--mono);letter-spacing:.13em;margin-top:2px}.workspace-label,.eyebrow,.meta-label,.summary-label,.field-label,.sidebar-version{font:10px var(--mono);letter-spacing:.11em}.workspace-label{color:#85998d;padding:0 12px 10px}.nav-list{display:grid;gap:4px}.nav-item{width:100%;border:0;text-align:left;color:#becbc2;background:transparent;padding:11px 12px;border-radius:5px;display:flex;gap:12px;align-items:center;font-size:13px}.nav-item:hover{background:#2b3a33;color:#fff}.nav-item.active{background:#344b40;color:#fff;box-shadow:inset 3px 0 #a9c1ae}.nav-icon{width:18px;text-align:center;font-size:16px;color:#a9bdb0}.sidebar-bottom{margin-top:auto}.mode-card{border:1px solid #40544a;background:#293a32;padding:11px 10px;display:flex;align-items:flex-start;gap:9px;border-radius:5px}.mode-dot{width:7px;height:7px;border-radius:50%;background:#8bc39c;margin-top:5px;box-shadow:0 0 0 3px rgba(139,195,156,.13)}.mode-card strong{display:block;font:10px var(--mono);letter-spacing:.08em}.mode-card small{display:block;color:#aab8af;font-size:11px;margin-top:3px}.reset-button{border:0;color:#aab8af;background:transparent;font-size:11px;padding:12px 0 15px}.reset-button:hover{color:#fff;text-decoration:underline}.sidebar-version{font-size:8px;color:#75877c;padding:10px 3px 0;border-top:1px solid #3b4b42}.main-area{min-width:0;padding:0 38px 20px;max-width:1700px;width:100%;margin:0 auto}.topbar{height:66px;display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid var(--line);margin-bottom:30px}.breadcrumb{display:flex;gap:9px;color:var(--muted);font-size:12px}.breadcrumb strong{color:var(--ink);font-weight:600}.crumb-slash{color:#bdc4bc}.topbar-right{display:flex;align-items:center;gap:15px}.local-pill{display:flex;align-items:center;gap:7px;border:1px solid var(--line-dark);background:#fff;padding:5px 9px;border-radius:99px;color:#536258;font-size:10px}.local-pill i{width:6px;height:6px;background:#56916c;border-radius:50%}.user-avatar{height:28px;width:28px;border-radius:50%;background:#e3ebe5;color:var(--green);display:grid;place-items:center;font:10px var(--mono)}.page-view{display:none}.page-view.active{display:block}.page-heading{display:flex;align-items:flex-end;justify-content:space-between;gap:20px;margin:0 0 22px}.eyebrow{color:#78857b;margin:0 0 6px}.page-heading h1{font-size:29px;line-height:1.2;letter-spacing:-.04em;margin:0;font-weight:600}.page-intro{margin:7px 0 0;color:var(--muted);font-size:13px}.heading-meta{min-width:210px;padding:10px 13px;border-left:2px solid #c4d2c8;background:#edf1eb}.heading-meta .meta-label{display:block;color:#7a857b;font-size:9px}.heading-meta strong{display:block;font-size:12px;margin-top:3px}.heading-meta small{display:block;color:var(--muted);font-size:10px;margin-top:2px}.button{border:1px solid transparent;border-radius:4px;padding:9px 13px;font-size:11px;font-weight:600;white-space:nowrap}.button-primary{color:#fff;background:var(--green);border-color:var(--green)}.button-primary:hover{background:#1e4437}.button-secondary{color:#34483c;background:#edf1eb;border-color:#d9e0d7}.button-secondary:hover{background:#e3eae2}.button-quiet{color:#53675b;background:#fff;border-color:var(--line-dark)}.button-quiet:hover{border-color:#829487}.button-wide{width:100%;padding:11px}.text-button{border:0;background:transparent;color:var(--green);font-size:11px;font-weight:600}.text-button:hover{text-decoration:underline}.notice{display:flex;align-items:flex-start;gap:10px;padding:13px 14px;border:1px solid #dfe7e0;background:#edf3ee;border-radius:4px;margin-bottom:19px;color:#46594b}.notice p{margin:0;font-size:11px;line-height:1.6}.notice strong{color:#2c4939}.notice-icon{height:18px;width:18px;flex:0 0 18px;border-radius:50%;display:grid;place-items:center;background:#d7e6da;color:#315b43;font:bold 11px Georgia}.notice-warning{background:var(--amber-bg);border-color:#eee0c5;color:#74572e}.notice-warning .notice-icon{background:#f0dfbd;color:#7b531c}.notice-warning strong{color:#664514}.notice-small{margin:0 0 14px;padding:9px 11px}.notice-small p{font-size:10px}.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:13px;margin-bottom:20px}.summary-card{background:var(--panel);border:1px solid var(--line);padding:17px 18px;box-shadow:var(--shadow);min-height:110px}.summary-label{display:block;color:#78837a;font-size:9px}.summary-card strong{display:block;font-size:27px;line-height:1.2;letter-spacing:-.04em;margin-top:10px}.summary-card small{color:var(--muted);font-size:10px}.panel-grid{display:grid;grid-template-columns:1.15fr .85fr;gap:15px}.panel{background:var(--panel);border:1px solid var(--line);box-shadow:var(--shadow);padding:18px 19px;margin-bottom:16px;min-width:0}.panel-heading{display:flex;justify-content:space-between;align-items:center;gap:12px;margin-bottom:15px}.panel-heading .eyebrow{font-size:9px;margin-bottom:3px}.panel-heading h2{font-size:16px;letter-spacing:-.02em;margin:0;font-weight:600}.subtle-tag,.status-badge{font:9px var(--mono);letter-spacing:.06em;color:#637468;background:#f0f3ef;border:1px solid #e1e7e0;padding:5px 7px;border-radius:3px;white-space:nowrap}.status-badge{color:#3f7554;background:#edf5ee;border-color:#dceade}.module-list{display:grid}.module-row{border:0;border-top:1px solid #edf0eb;background:#fff;display:grid;grid-template-columns:34px 1fr 15px;gap:10px;align-items:center;text-align:left;padding:12px 2px;color:var(--ink)}.module-row:hover strong{color:var(--green)}.module-index{font:10px var(--mono);color:#91a095}.module-row strong{display:block;font-size:12px}.module-row small{display:block;color:var(--muted);font-size:10px;margin-top:2px}.module-row b{color:#94a096}.status-list{display:grid;gap:0}.status-list div{display:flex;justify-content:space-between;align-items:center;padding:10px 0;border-top:1px solid #edf0eb;font-size:11px}.status-list div span{color:var(--muted)}.status-ok{font:10px var(--mono);color:#3c8055}.status-muted{font:10px var(--mono);color:#929a92}.panel-footnote{font-size:10px;color:#7c867d;margin:13px 0 0;line-height:1.55}.filters{display:flex;align-items:flex-end;gap:11px;flex-wrap:wrap;margin:0 0 14px}.filters label,.stack-form label,.form-grid label{display:grid;gap:5px;min-width:140px;flex:1}.filters label span,.stack-form label span,.form-grid label span{font:9px var(--mono);letter-spacing:.08em;color:#6f7a70}.filters .filter-search{flex:2;min-width:220px}.filters input,.filters select,.stack-form input,.stack-form select,.stack-form textarea,.form-grid input,.form-grid select,.dialog input,.dialog select{width:100%;border:1px solid var(--line-dark);background:#fff;border-radius:3px;padding:9px 10px;color:var(--ink);font-size:12px;min-height:36px;outline:none}.stack-form textarea{resize:vertical}.filters input:focus,.filters select:focus,.stack-form input:focus,.stack-form select:focus,.stack-form textarea:focus,.form-grid input:focus,.form-grid select:focus,.dialog input:focus,.dialog select:focus{border-color:#789889;box-shadow:0 0 0 2px rgba(40,86,71,.1)}.table-wrap{width:100%;overflow:auto;border-top:1px solid var(--line)}table{border-collapse:collapse;width:100%;font-size:11px;white-space:nowrap}th{text-align:left;color:#7a857b;background:#f8f9f6;font:9px var(--mono);letter-spacing:.07em;padding:11px 10px;border-bottom:1px solid var(--line)}td{padding:11px 10px;border-bottom:1px solid #edf0ec;color:#4b574d;vertical-align:middle}tbody tr:hover{background:#fafcf9}.table-message{text-align:center;color:#89938a;padding:25px}.row-title{color:#25362c;font-weight:600}.row-sub{display:block;color:#8b948c;font:9px var(--mono);margin-top:2px}.table-action{border:1px solid var(--line-dark);background:#fff;border-radius:3px;padding:5px 8px;color:#435a4a;font-size:10px}.table-action:hover{border-color:#809688}.pill{display:inline-flex;border-radius:99px;padding:3px 7px;font:9px var(--mono);background:#edf3ee;color:#496e55}.pill.warning{background:#fbf1df;color:#8b642d}.pill.muted{background:#f0f1ef;color:#777f78}.panel-footer{display:flex;justify-content:space-between;color:#7c877e;font:9px var(--mono);padding-top:11px}.form-panel{background:#fbfcfa}.form-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}.form-actions{display:flex;align-items:end}.form-note{grid-column:1/-1;color:#858f86;font-size:10px;line-height:1.5;margin:2px 0}.text-button{padding:5px}.two-column{display:grid;grid-template-columns:1fr 1fr;gap:15px}.stack-form{display:grid;gap:12px}.inline-fields{display:grid;grid-template-columns:1fr 1fr;gap:10px}.stack-form label small{font:8px var(--mono);color:#929b92;margin-left:5px}.stack-form .form-note{margin:0}.payroll-layout{display:grid;grid-template-columns:minmax(280px,.85fr) minmax(380px,1.15fr);gap:15px;align-items:start}.result-panel{min-height:460px}.result-empty{min-height:330px;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;color:var(--muted);padding:24px}.empty-mark{width:47px;height:47px;display:grid;place-items:center;border:1px solid #dce5dc;color:var(--green);background:#f0f5f0;font-size:22px;border-radius:50%;margin-bottom:12px}.result-empty strong{color:#526156;font-size:13px}.result-empty p{max-width:270px;font-size:11px;line-height:1.6}.result-hero{padding:14px;background:#eef4ef;border:1px solid #dce8de;margin-bottom:13px}.result-hero span{display:block;font:9px var(--mono);letter-spacing:.08em;color:#6b7a6e}.result-hero strong{display:block;font-size:30px;letter-spacing:-.05em;color:#275844;margin-top:4px}.result-hero small{color:#6e7f72;font-size:10px}.result-table{width:100%;border-collapse:collapse;white-space:normal}.result-table th{background:#f7f9f6}.result-table td:last-child,.result-table th:last-child{text-align:right;font-variant-numeric:tabular-nums}.result-section{font:9px var(--mono);letter-spacing:.1em;color:#7c897e;padding:11px 0 5px}.result-total td{font-weight:700;color:#293b30;border-top:1px solid var(--line-dark)}.rule-notes{display:grid;grid-template-columns:1fr 1fr;gap:7px 20px}.rule-notes p{margin:0;color:#69756b;font-size:10px;line-height:1.6}.rule-notes strong{color:#344b3b}.sources-panel{margin-top:15px}.source-links{display:flex;gap:8px 18px;flex-wrap:wrap;border-top:1px solid var(--line);padding-top:12px;margin-top:12px}.source-links a{font-size:10px;color:#426b55;text-decoration:none}.source-links a:hover{text-decoration:underline}.app-footer{border-top:1px solid var(--line);margin-top:30px;padding:15px 0;color:#909990;font:9px var(--mono);display:flex;justify-content:space-between}.dialog{width:min(470px,calc(100% - 24px));border:1px solid var(--line-dark);padding:20px;background:#fff;box-shadow:0 20px 70px rgba(20,40,30,.2)}.dialog::backdrop{background:rgba(25,39,31,.4)}.dialog-heading{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:18px}.dialog-heading h2{margin:0;font-size:18px}.icon-button{border:0;background:transparent;font-size:23px;color:#738076}.dialog-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:18px}.global-message{padding:10px 13px;border:1px solid #e8d6b4;background:#fff8e9;color:#705526;font-size:11px;margin-bottom:16px}.toast{position:fixed;right:22px;bottom:20px;max-width:360px;padding:12px 15px;background:#243d31;color:#fff;box-shadow:var(--shadow);border-radius:4px;font-size:11px;z-index:10}.toast.error{background:#843f39}.hidden,[hidden]{display:none!important}
@media(max-width:1100px){.app-shell{grid-template-columns:205px minmax(0,1fr)}.main-area{padding:0 24px 20px}.summary-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.panel-grid,.payroll-layout{grid-template-columns:1fr 1fr}}
@media(max-width:800px){.app-shell{display:block}.sidebar{position:relative;height:auto;min-height:0;padding:12px 14px}.brand{padding:0 5px 12px}.workspace-label,.sidebar-bottom{display:none}.nav-list{display:flex;overflow-x:auto;gap:4px;padding-bottom:2px}.nav-item{width:auto;white-space:nowrap;padding:8px 10px}.main-area{padding:0 15px 18px}.topbar{margin-bottom:22px;height:54px}.page-heading{align-items:flex-start;flex-direction:column}.heading-meta{display:none}.panel-grid,.two-column,.payroll-layout{grid-template-columns:1fr}.form-grid{grid-template-columns:1fr 1fr}.form-actions{align-items:center}.app-footer{gap:10px;flex-wrap:wrap}}
@media(max-width:520px){.page-heading h1{font-size:25px}.summary-grid{gap:8px}.summary-card{padding:13px;min-height:96px}.summary-card strong{font-size:23px}.panel{padding:14px 12px}.filters{display:grid;grid-template-columns:1fr 1fr;align-items:end}.filters .filter-search{grid-column:1/-1;min-width:0}.filters button{width:100%}.form-grid{grid-template-columns:1fr}.form-grid .form-note{grid-column:auto}.inline-fields{grid-template-columns:1fr 1fr}.local-pill{font-size:0;padding:7px}.local-pill i{margin:0}.breadcrumb{font-size:11px}.rule-notes{grid-template-columns:1fr}.app-footer{font-size:8px}.table-wrap{margin:0 -2px}}
.directory-metrics{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px}.directory-metric{border:1px solid var(--line);background:#fafbf9;padding:12px}.directory-metric span{display:block;color:#78847a;font:9px var(--mono);letter-spacing:.08em}.directory-metric strong{display:block;font-size:22px;margin-top:5px}.directory-breakdown{display:flex;flex-wrap:wrap;gap:7px;margin-top:12px}.directory-breakdown span{padding:5px 8px;border:1px solid var(--line);background:#fbfcfa;color:#5b6a5e;font-size:10px}.calendar-controls{display:flex;align-items:center;gap:9px}.calendar-controls strong{font:10px var(--mono);min-width:100px;text-align:center}.calendar-nav{border:1px solid var(--line);width:27px;height:27px;display:grid;place-items:center;padding:0;font-size:18px;background:#fff}.calendar-grid{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));gap:4px}.calendar-weekday{text-align:center;color:#8a948b;font:8px var(--mono);padding:4px 0}.calendar-day{border:1px solid #edf0eb;background:#fff;min-height:40px;padding:4px;display:flex;flex-direction:column;align-items:center;justify-content:flex-start;color:#4b594e;font-size:10px}.calendar-day:hover{border-color:#91a899;background:#f5f8f5}.calendar-day.muted{color:#b4bcb5;background:#fafbf9}.calendar-day.today{border-color:#6d9a7b}.calendar-day.selected{background:#e9f1eb;border-color:#5f8a6c;color:#285647}.calendar-day.has-request:after{content:"";display:block;width:5px;height:5px;border-radius:50%;background:#c28b42;margin-top:3px}.calendar-day.cancelled:after{background:#a2aaa2}.calendar-legend{display:flex;gap:12px;color:#879187;font-size:9px;margin-top:9px}.calendar-legend span{display:flex;gap:5px;align-items:center}.calendar-legend i{display:block;width:6px;height:6px;border-radius:50%;background:#c28b42}.calendar-legend .cancelled-dot{background:#a2aaa2}.row-details{white-space:normal;min-width:140px;max-width:260px}.row-details summary{cursor:pointer;color:#48614f;font-size:10px}.row-details p{font-size:10px;color:#69766c;margin:6px 0;max-width:260px;white-space:normal;overflow-wrap:anywhere}.sort-controls{display:flex;gap:8px;flex-wrap:wrap}.attendance-summary{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;margin-bottom:13px}.attendance-summary div{padding:10px;border:1px solid var(--line);background:#fafbf9}.attendance-summary span{display:block;color:#839086;font:8px var(--mono);letter-spacing:.08em}.attendance-summary strong{display:block;font-size:13px;margin-top:3px}@media(max-width:520px){.directory-metrics{grid-template-columns:1fr 1fr}.calendar-day{min-height:33px}.attendance-summary{grid-template-columns:1fr}}
.detail-grid{display:grid;grid-template-columns:1fr 1fr;gap:9px}.detail-item{padding:9px;background:#f7f9f6;border:1px solid var(--line)}.detail-item span{display:block;color:#819087;font:8px var(--mono);letter-spacing:.1em}.detail-item strong{display:block;color:#35483a;font-size:12px;margin-top:4px;overflow-wrap:anywhere}

/* Local reconstruction additions */
.row-link{border:0;background:transparent;padding:0;text-align:left;cursor:pointer;font:inherit;font-weight:600;color:#285647}.row-link:hover{text-decoration:underline}.calendar-day.outside-month{color:#b4bcb5;background:#fafbf9}.calendar-day.selected-date{background:#e9f1eb;border-color:#5f8a6c;color:#285647}.calendar-marker{display:block;color:#b17c35;font-size:14px;line-height:10px;margin-top:2px}.calendar-day:focus-visible,.row-link:focus-visible{outline:2px solid #628b72;outline-offset:2px}.detail-grid>div{padding:9px;background:#f7f9f6;border:1px solid var(--line);min-width:0}.detail-grid>div span{display:block;color:#819087;font:8px var(--mono);letter-spacing:.1em}.detail-grid>div strong{display:block;color:#35483a;font-size:12px;margin-top:4px;overflow-wrap:anywhere}.result-ready{min-height:0}.result-table td{white-space:normal}.row-details{white-space:normal;min-width:140px;max-width:260px}.row-details summary{cursor:pointer;color:#48614f;font-size:10px}.row-details p{font-size:10px;color:#69766c;margin:6px 0;max-width:260px;white-space:normal;overflow-wrap:anywhere}
```


### `web/app.js`

```javascript
'use strict';

const state = {
  employees: [],
  allAttendance: [],
  attendance: [],
  leaveRequests: [],
  tickets: [],
  attendanceReport: [],
  visibleEmployees: [],
  visibleTeam: [],
  visibleHr: [],
  visibleLeave: [],
  visibleTickets: [],
  visibleReport: [],
  payrollResult: null,
  currentView: 'overview',
  attendanceEmployeeId: '',
  calendarMonth: new Date(new Date().getFullYear(), new Date().getMonth(), 1),
  calendarFirstSelection: null
};

const $ = id => document.getElementById(id);
const has = id => Boolean($(id));
const text = value => String(value ?? '');
const money = value => new Intl.NumberFormat('en-PH', { style: 'currency', currency: 'PHP' }).format(Number(value || 0));
const localDate = date => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const parseLocalDate = value => {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(text(value));
  if (!match) return null;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return date.getFullYear() === Number(match[1]) && date.getMonth() === Number(match[2]) - 1 && date.getDate() === Number(match[3]) ? date : null;
};

async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) };
  if (options.body && !headers['Content-Type']) headers['Content-Type'] = 'application/json';
  const response = await fetch(path, { ...options, headers });
  const raw = await response.text();
  let payload = {};
  try { payload = raw ? JSON.parse(raw) : {}; }
  catch { payload = { error: raw || 'Unexpected server response.' }; }
  if (!response.ok) throw new Error(payload.error || `Request failed (${response.status}).`);
  return payload;
}

function toast(message, error = false) {
  const node = $('toast');
  if (!node) return;
  node.textContent = message;
  node.classList.toggle('error', error);
  node.hidden = false;
  window.clearTimeout(toast.timer);
  toast.timer = window.setTimeout(() => { node.hidden = true; }, 3400);
}

function globalMessage(message) {
  const node = $('global-message');
  if (!node) return;
  node.textContent = message;
  node.hidden = !message;
}

function setView(name) {
  state.currentView = name;
  document.querySelectorAll('.page-view').forEach(view => {
    const active = view.id === `${name}-view`;
    view.classList.toggle('active', active);
    view.hidden = !active;
  });
  document.querySelectorAll('.nav-item').forEach(button => button.classList.toggle('active', button.dataset.view === name));
  if (has('breadcrumb-current')) {
    $('breadcrumb-current').textContent = ({ overview: 'Overview', employees: 'Employees', attendance: 'Attendance', requests: 'Requests', team: 'Team', hr: 'HR Desk', reports: 'Reports', payroll: 'Payroll' })[name] || name;
  }
  if (name === 'attendance') loadAttendance();
  if (name === 'reports') loadAttendanceReport();
  if (name === 'requests') { renderCalendar(); renderLeaveRequests(); renderTickets(); }
}

document.querySelectorAll('[data-view]').forEach(button => button.addEventListener('click', () => setView(button.dataset.view)));
document.querySelectorAll('[data-go]').forEach(button => button.addEventListener('click', () => setView(button.dataset.go)));

function cell(value, className = '') {
  const td = document.createElement('td');
  td.textContent = text(value);
  if (className) td.className = className;
  return td;
}

function pill(value) {
  const span = document.createElement('span');
  span.className = 'pill' + (value === 'Cancelled' || value === 'Inactive' ? ' muted' : value === 'On leave' || value === 'Pending review' || value === 'Open' ? ' warning' : '');
  span.textContent = text(value);
  return span;
}

function button(label, action, id, className = 'table-action') {
  const node = document.createElement('button');
  node.type = 'button'; node.className = className; node.textContent = label;
  node.dataset.action = action; node.dataset.id = id;
  return node;
}

function tableMessage(target, columns, message) {
  target.replaceChildren();
  const row = document.createElement('tr');
  const td = cell(message, 'table-message'); td.colSpan = columns;
  row.append(td); target.append(row);
}

function searchable(item, fields, query) {
  if (!query) return true;
  return fields.map(field => text(item[field])).join(' ').toLocaleLowerCase().includes(query);
}

function sortItems(items, field, direction = 'asc') {
  const multiplier = direction === 'desc' ? -1 : 1;
  return [...items].sort((a, b) => {
    const left = a[field]; const right = b[field];
    if (left == null && right == null) return 0;
    if (left == null) return 1 * multiplier;
    if (right == null) return -1 * multiplier;
    if (typeof left === 'number' && typeof right === 'number') return (left - right) * multiplier;
    return new Intl.Collator('en', { numeric: true, sensitivity: 'base' }).compare(text(left), text(right)) * multiplier;
  });
}

function sortHistory(items, order) {
  return [...items].sort((a, b) => {
    const left = text(a.createdAt || a.dateRequested || a.date || '');
    const right = text(b.createdAt || b.dateRequested || b.date || '');
    return order === 'oldest' ? left.localeCompare(right) : right.localeCompare(left);
  });
}

function csvCell(value) {
  let content = text(value);
  if (/^[\s\u0000-\u001f]*[=+@-]/.test(content)) content = `'${content}`;
  return `"${content.replace(/"/g, '""')}"`;
}

function exportCsv(filename, rows, columns = null) {
  if (!rows.length) { toast('There are no rows to export.', true); return; }
  const keys = columns || Object.keys(rows[0]);
  const csv = [keys.map(csvCell).join(','), ...rows.map(row => keys.map(key => csvCell(row[key])).join(','))].join('\r\n');
  const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a'); link.href = url; link.download = filename;
  document.body.append(link); link.click(); link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function employeeSummaryRows(items) {
  return items.map(({ employeeId, name, department, role, location, status, scheduleId }) => ({ employeeId, name, department, role, location, status, scheduleId: scheduleId ?? '' }));
}

function renderEmployeeRows(target, items, mode) {
  target.replaceChildren();
  if (!items.length) { tableMessage(target, mode === 'team' ? 5 : 6, 'No employee records match the current filters.'); return; }
  items.forEach(item => {
    const row = document.createElement('tr');
    const identity = document.createElement('td');
    const name = document.createElement('button'); name.type = 'button'; name.className = 'row-title row-link';
    name.textContent = item.name; name.dataset.action = 'view-employee'; name.dataset.id = item.employeeId;
    const id = document.createElement('span'); id.className = 'row-sub'; id.textContent = item.employeeId;
    identity.append(name, id); row.append(identity);
    row.append(cell(item.department), cell(item.role), cell(item.location));
    const status = document.createElement('td'); status.append(pill(item.status)); row.append(status);
    if (mode !== 'team') {
      const actions = document.createElement('td');
      if (mode === 'hr') actions.append(button('Edit', 'edit-employee', item.employeeId));
      else actions.append(button('View', 'view-employee', item.employeeId));
      row.append(actions);
    }
    target.append(row);
  });
}

function filteredDirectory(prefix) {
  const query = $(`${prefix}-search`).value.trim().toLocaleLowerCase();
  const status = $(`${prefix}-status`).value;
  const sort = $(`${prefix}-sort`).value;
  const order = $(`${prefix}-order`).value;
  return sortItems(state.employees.filter(item => searchable(item, ['employeeId', 'name', 'department', 'role', 'location'], query) && (!status || item.status === status)), sort, order);
}

function renderEmployees() {
  state.visibleEmployees = filteredDirectory('employee');
  renderEmployeeRows($('employee-rows'), state.visibleEmployees, 'employee');
  $('employee-count').textContent = `${state.visibleEmployees.length} of ${state.employees.length} employee records`;
  $('employee-export').disabled = state.visibleEmployees.length === 0;
}

function renderTeamAndHr() {
  state.visibleTeam = filteredDirectory('team');
  state.visibleHr = filteredDirectory('hr');
  renderEmployeeRows($('team-rows'), state.visibleTeam, 'team');
  renderEmployeeRows($('hr-rows'), state.visibleHr, 'hr');
  $('team-export').disabled = state.visibleTeam.length === 0;
}

function populateEmployeeSelects() {
  ['attendance-employee', 'attendance-filter-employee', 'leave-employee', 'ticket-employee'].forEach(id => {
    const select = $(id); if (!select) return;
    const prior = select.value;
    select.replaceChildren();
    const placeholder = document.createElement('option'); placeholder.value = ''; placeholder.textContent = 'Choose synthetic employee'; select.append(placeholder);
    state.employees.forEach(employee => {
      const option = document.createElement('option'); option.value = employee.employeeId;
      option.textContent = `${employee.name} · ${employee.employeeId}`; select.append(option);
    });
    if ([...select.options].some(option => option.value === prior)) select.value = prior;
  });
}

function renderDirectorySummary() {
  const statusNode = $('directory-status-counts');
  const departmentNode = $('directory-department-counts');
  if (!statusNode || !departmentNode) return;
  statusNode.replaceChildren(); departmentNode.replaceChildren();
  const statuses = new Map(); const departments = new Map();
  state.employees.forEach(item => {
    statuses.set(item.status, (statuses.get(item.status) || 0) + 1);
    departments.set(item.department, (departments.get(item.department) || 0) + 1);
  });
  [['TOTAL', state.employees.length], ...[...statuses.entries()].sort((a,b) => a[0].localeCompare(b[0]))].forEach(([label, count]) => {
    const card = document.createElement('div'); card.className = 'directory-metric';
    const value = document.createElement('strong'); value.textContent = String(count);
    const caption = document.createElement('span'); caption.textContent = label;
    card.append(value, caption); statusNode.append(card);
  });
  [...departments.entries()].sort((a,b) => a[0].localeCompare(b[0])).forEach(([name, count]) => {
    const item = document.createElement('span'); item.className = 'directory-breakdown-item';
    item.textContent = `${name}: ${count}`; departmentNode.append(item);
  });
  $('directory-summary-export').disabled = state.employees.length === 0;
}

function openEmployeeDetail(employee) {
  if (!employee) return;
  $('employee-detail-title').textContent = employee.name;
  const target = $('employee-detail-content'); target.replaceChildren();
  const fields = [
    ['Employee ID', employee.employeeId], ['Name', employee.name], ['Department', employee.department],
    ['Position', employee.role], ['Location', employee.location], ['Status', employee.status],
    ['Schedule mapping', employee.scheduleId == null ? 'Not assigned' : `Schedule ${employee.scheduleId}`],
    ['Created', employee.createdAt || 'Not recorded'], ['Updated', employee.updatedAt || 'No update recorded']
  ];
  fields.forEach(([label, value]) => {
    const block = document.createElement('div');
    const key = document.createElement('span'); key.textContent = label;
    const val = document.createElement('strong'); val.textContent = text(value);
    block.append(key, val); target.append(block);
  });
  $('employee-detail-dialog').showModal();
}

function openEmployeeDialog(employee = null) {
  $('employee-form').dataset.mode = employee ? 'edit' : 'create';
  $('employee-id').value = employee?.employeeId || '';
  $('employee-dialog-title').textContent = employee ? `Edit ${employee.employeeId}` : 'Add employee';
  $('employee-name').value = employee?.name || '';
  $('employee-department').value = employee?.department || '';
  $('employee-role').value = employee?.role || '';
  $('employee-location').value = employee?.location || '';
  $('employee-status-edit').value = employee?.status || 'Active';
  $('employee-dialog').showModal();
}

function onDirectoryAction(event) {
  const target = event.target.closest('button[data-action]'); if (!target) return;
  const employee = state.employees.find(item => item.employeeId === target.dataset.id);
  if (!employee) return;
  if (target.dataset.action === 'edit-employee') openEmployeeDialog(employee);
  if (target.dataset.action === 'view-employee') openEmployeeDetail(employee);
}

$('add-employee-button').addEventListener('click', () => openEmployeeDialog());
$('hr-add-employee').addEventListener('click', () => openEmployeeDialog());
$('employee-dialog-close').addEventListener('click', () => $('employee-dialog').close());
$('employee-dialog-cancel').addEventListener('click', () => $('employee-dialog').close());
$('employee-detail-close').addEventListener('click', () => $('employee-detail-dialog').close());
$('employee-detail-dismiss').addEventListener('click', () => $('employee-detail-dialog').close());
$('employee-rows').addEventListener('click', onDirectoryAction);
$('team-rows').addEventListener('click', onDirectoryAction);
$('hr-rows').addEventListener('click', onDirectoryAction);

$('employee-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = {
    name: $('employee-name').value.trim(), department: $('employee-department').value.trim(),
    role: $('employee-role').value.trim(), location: $('employee-location').value.trim(),
    status: $('employee-status-edit').value
  };
  const id = $('employee-id').value;
  try {
    await api(id ? `/api/employees/${encodeURIComponent(id)}` : '/api/employees', {
      method: id ? 'PATCH' : 'POST', body: JSON.stringify(payload)
    });
    $('employee-dialog').close(); await loadEmployees();
    toast(id ? 'Employee record updated.' : 'Synthetic employee created.');
  } catch (error) { toast(error.message, true); }
});

['employee', 'team', 'hr'].forEach(prefix => {
  $(`${prefix}-search`).addEventListener('input', prefix === 'employee' ? renderEmployees : renderTeamAndHr);
  $(`${prefix}-status`).addEventListener('change', prefix === 'employee' ? renderEmployees : renderTeamAndHr);
  $(`${prefix}-sort`).addEventListener('change', prefix === 'employee' ? renderEmployees : renderTeamAndHr);
  $(`${prefix}-order`).addEventListener('change', prefix === 'employee' ? renderEmployees : renderTeamAndHr);
});

async function loadEmployees() {
  try {
    const payload = await api('/api/employees');
    state.employees = Array.isArray(payload.items) ? payload.items : [];
    populateEmployeeSelects();
    renderEmployees(); renderTeamAndHr(); renderDirectorySummary(); updateOverview();
  } catch (error) { globalMessage(`Employee directory could not be loaded: ${error.message}`); }
}

function attendanceForSelectedRange() {
  const from = $('attendance-from').value; const to = $('attendance-to').value;
  if (from && to && from > to) throw new Error('The start date must not be after the end date.');
  return state.allAttendance.filter(item => item.employeeId === state.attendanceEmployeeId &&
    (!from || item.date >= from) && (!to || item.date <= to));
}

async function loadAttendance() {
  const employeeId = $('attendance-filter-employee').value;
  state.attendanceEmployeeId = employeeId;
  if (!employeeId) {
    state.attendance = []; renderAttendance([]);
    $('attendance-count').textContent = 'Choose an employee to load records';
    return;
  }
  $('attendance-count').textContent = 'Loading raw records…';
  try {
    const payload = await api(`/api/attendance?employeeId=${encodeURIComponent(employeeId)}`);
    const records = Array.isArray(payload.items) ? payload.items : [];
    if (payload.hoursCalculated !== false) throw new Error('Unexpected attendance response.');
    if (!records.length) {
      $('attendance-from').value = ''; $('attendance-to').value = '';
    } else if (! $('attendance-from').value && ! $('attendance-to').value) {
      const dates = records.map(item => item.date).filter(Boolean).sort();
      $('attendance-from').value = dates[0] || '';
      $('attendance-to').value = dates[dates.length - 1] || '';
    }
    // Cache the response as the selected employee's raw records. The server attaches employeeId via schedule mapping.
    state.selectedAttendance = records;
    renderAttendance(attendanceForSelectedRange());
  } catch (error) {
    state.selectedAttendance = [];
    renderAttendance([]); $('attendance-count').textContent = 'Could not load records';
    toast(error.message, true);
  }
}

function renderAttendance(items = null) {
  const records = items || [];
  state.attendance = records;
  const sort = $('attendance-sort').value; const order = $('attendance-order').value;
  const sorted = sortItems(records, sort, order);
  const target = $('attendance-rows'); target.replaceChildren();
  if (!sorted.length) tableMessage(target, 6, 'No raw attendance entries match the selected employee and dates.');
  sorted.forEach(record => {
    const employee = state.employees.find(item => item.employeeId === record.employeeId);
    const row = document.createElement('tr');
    row.append(cell(record.date), cell(employee?.name || record.employeeId || 'Unmapped'), cell(record.scheduleId), cell(record.timeIn), cell(record.timeOut), cell(record.dayType));
    target.append(row);
  });
  const dates = sorted.map(item => item.date).filter(Boolean).sort();
  $('attendance-summary-count').textContent = String(sorted.length).padStart(2, '0');
  $('attendance-summary-first').textContent = dates[0] || '—';
  $('attendance-summary-last').textContent = dates[dates.length - 1] || '—';
  $('attendance-count').textContent = `${sorted.length} raw ${sorted.length === 1 ? 'record' : 'records'} loaded · no hours calculated`;
  $('attendance-export').disabled = sorted.length === 0;
  state.visibleAttendance = sorted;
}

$('attendance-filter-employee').addEventListener('change', () => {
  $('attendance-from').value = ''; $('attendance-to').value = '';
  loadAttendance();
});
$('attendance-apply').addEventListener('click', () => {
  try { renderAttendance(attendanceForSelectedRange()); }
  catch (error) { toast(error.message, true); }
});
$('attendance-sort').addEventListener('change', () => { try { renderAttendance(attendanceForSelectedRange()); } catch {} });
$('attendance-order').addEventListener('change', () => { try { renderAttendance(attendanceForSelectedRange()); } catch {} });
$('add-attendance-toggle').addEventListener('click', () => {
  $('attendance-form-panel').hidden = false;
  if (state.attendanceEmployeeId) $('attendance-employee').value = state.attendanceEmployeeId;
  $('attendance-form-panel').scrollIntoView({ behavior: 'smooth', block: 'nearest' });
});
$('close-attendance-form').addEventListener('click', () => { $('attendance-form-panel').hidden = true; });
$('attendance-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = {
    employeeId: $('attendance-employee').value, date: $('attendance-date').value,
    timeIn: $('attendance-time-in').value, timeOut: $('attendance-time-out').value, dayType: $('attendance-day-type').value
  };
  try {
    await api('/api/attendance', { method: 'POST', body: JSON.stringify(payload) });
    $('attendance-form-panel').hidden = true;
    $('attendance-filter-employee').value = payload.employeeId;
    $('attendance-from').value = ''; $('attendance-to').value = '';
    await Promise.all([loadAttendance(), loadAllAttendance(), loadAttendanceReport()]);
    toast('Local synthetic attendance entry saved.');
  } catch (error) { toast(error.message, true); }
});

async function loadAllAttendance() {
  try {
    const payload = await api('/api/attendance');
    state.allAttendance = Array.isArray(payload.items) ? payload.items : [];
    updateOverview();
  } catch (error) { state.allAttendance = []; toast(`Attendance summary unavailable: ${error.message}`, true); }
}

async function loadWorkflows() {
  try {
    const payload = await api('/api/workflows');
    state.leaveRequests = Array.isArray(payload.leaveRequests) ? payload.leaveRequests : [];
    state.tickets = Array.isArray(payload.tickets) ? payload.tickets : [];
    renderLeaveRequests(); renderTickets(); renderCalendar(); updateOverview();
  } catch (error) { globalMessage(`Workflow records could not be loaded: ${error.message}`); }
}

$('leave-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = { employeeId: $('leave-employee').value, leaveType: $('leave-type').value,
    startDate: $('leave-start').value, endDate: $('leave-end').value, note: $('leave-note').value.trim() };
  try {
    await api('/api/leave-requests', { method: 'POST', body: JSON.stringify(payload) });
    $('leave-form').reset();
    $('leave-start').value = localDate(new Date()); $('leave-end').value = localDate(new Date());
    await loadWorkflows(); toast('Leave request submitted as pending review.');
  } catch (error) { toast(error.message, true); }
});

$('ticket-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = { employeeId: $('ticket-employee').value, ticketType: $('ticket-type').value, summary: $('ticket-summary').value.trim() };
  try {
    await api('/api/tickets', { method: 'POST', body: JSON.stringify(payload) });
    $('ticket-form').reset(); await loadWorkflows(); toast('Demo ticket created.');
  } catch (error) { toast(error.message, true); }
});

function appendDetails(cellNode, summary, paragraphs) {
  const details = document.createElement('details'); details.className = 'row-details';
  const label = document.createElement('summary'); label.textContent = summary; details.append(label);
  paragraphs.filter(item => item && item[1] !== undefined && item[1] !== null && item[1] !== '').forEach(([key, value]) => {
    const p = document.createElement('p'); const strong = document.createElement('strong'); strong.textContent = `${key}: `;
    p.append(strong, document.createTextNode(text(value))); details.append(p);
  });
  cellNode.append(details);
}

function renderLeaveRequests() {
  const query = $('leave-search').value.trim().toLocaleLowerCase();
  const status = $('leave-status').value; const from = $('leave-filter-from').value; const to = $('leave-filter-to').value;
  let items = state.leaveRequests.filter(item => searchable(item, ['requestId', 'employeeName', 'employeeId', 'leaveType', 'startDate', 'endDate', 'note'], query) &&
    (!status || item.status === status) && (!from || item.endDate >= from) && (!to || item.startDate <= to));
  items = sortHistory(items, $('leave-sort').value);
  state.visibleLeave = items;
  const target = $('leave-rows'); target.replaceChildren();
  if (!items.length) tableMessage(target, 6, 'No leave requests match the current filters.');
  items.forEach(item => {
    const row = document.createElement('tr');
    const requestCell = cell(item.requestId); appendDetails(requestCell, 'Details', [['Employee ID', item.employeeId], ['Note', item.note], ['Submitted', item.createdAt], ['Updated', item.updatedAt]]); row.append(requestCell);
    row.append(cell(item.employeeName), cell(item.leaveType), cell(`${item.startDate} – ${item.endDate}`));
    const statusCell = document.createElement('td'); statusCell.append(pill(item.status)); row.append(statusCell);
    const action = document.createElement('td');
    if (item.status === 'Pending review') action.append(button('Cancel', 'cancel-leave', item.requestId)); else action.textContent = '—';
    row.append(action); target.append(row);
  });
  $('leave-export').disabled = state.leaveRequests.length === 0;
}

function renderTickets() {
  const query = $('ticket-search').value.trim().toLocaleLowerCase(); const status = $('ticket-status').value;
  const items = sortHistory(state.tickets.filter(item => searchable(item, ['ticketId', 'employeeName', 'employeeId', 'ticketType', 'queueLabel', 'summary'], query) && (!status || item.status === status)), $('ticket-sort').value);
  state.visibleTickets = items;
  const target = $('ticket-rows'); target.replaceChildren();
  if (!items.length) tableMessage(target, 6, 'No tickets match the current filters.');
  items.forEach(item => {
    const row = document.createElement('tr');
    const idCell = cell(item.ticketId); appendDetails(idCell, 'Details', [['Employee ID', item.employeeId], ['Message', item.summary], ['Submitted', item.createdAt], ['Updated', item.updatedAt]]); row.append(idCell);
    row.append(cell(item.employeeName), cell(item.ticketType), cell(item.queueLabel));
    const statusCell = document.createElement('td'); statusCell.append(pill(item.status)); row.append(statusCell);
    const action = document.createElement('td'); if (item.status === 'Open') action.append(button('Cancel', 'cancel-ticket', item.ticketId)); else action.textContent = '—'; row.append(action);
    target.append(row);
  });
  $('ticket-export').disabled = state.tickets.length === 0;
}

async function workflowAction(event, type) {
  const target = event.target.closest('button[data-action]'); if (!target) return;
  const collection = type === 'leave' ? 'leave-requests' : 'tickets';
  try {
    await api(`/api/${collection}/${encodeURIComponent(target.dataset.id)}/cancel`, { method: 'POST', body: '{}' });
    await loadWorkflows(); toast(`${type === 'leave' ? 'Leave request' : 'Ticket'} cancelled.`);
  } catch (error) { toast(error.message, true); }
}
$('leave-rows').addEventListener('click', event => workflowAction(event, 'leave'));
$('ticket-rows').addEventListener('click', event => workflowAction(event, 'ticket'));
['leave-search', 'leave-filter-from', 'leave-filter-to', 'leave-sort'].forEach(id => $(id).addEventListener(id === 'leave-search' ? 'input' : 'change', renderLeaveRequests));
$('leave-status').addEventListener('change', renderLeaveRequests);
$('ticket-search').addEventListener('input', renderTickets); $('ticket-status').addEventListener('change', renderTickets); $('ticket-sort').addEventListener('change', renderTickets);

function renderCalendar() {
  const root = $('leave-calendar'); if (!root) return;
  const month = state.calendarMonth;
  $('calendar-month').textContent = month.toLocaleDateString('en-PH', { month: 'long', year: 'numeric' });
  root.replaceChildren();
  ['M', 'T', 'W', 'T', 'F', 'S', 'S'].forEach(day => {
    const header = document.createElement('span'); header.className = 'calendar-weekday'; header.textContent = day; root.append(header);
  });
  const first = new Date(month.getFullYear(), month.getMonth(), 1);
  const mondayOffset = (first.getDay() + 6) % 7;
  const start = new Date(first.getFullYear(), first.getMonth(), 1 - mondayOffset);
  for (let index = 0; index < 42; index++) {
    const date = new Date(start.getFullYear(), start.getMonth(), start.getDate() + index);
    const iso = localDate(date);
    const dayButton = document.createElement('button'); dayButton.type = 'button'; dayButton.className = 'calendar-day';
    if (date.getMonth() !== month.getMonth()) dayButton.classList.add('outside-month', 'muted');
    dayButton.dataset.date = iso; dayButton.setAttribute('aria-label', date.toLocaleDateString('en-PH', { dateStyle: 'full' }));
    const dayNumber = document.createElement('span'); dayNumber.textContent = String(date.getDate()); dayButton.append(dayNumber);
    const matching = state.leaveRequests.filter(item => item.startDate <= iso && item.endDate >= iso);
    if (matching.length) {
      const marker = document.createElement('span'); marker.className = 'calendar-marker';
      marker.textContent = matching.some(item => item.status === 'Pending review') ? '•' : '○';
      dayButton.append(marker);
      dayButton.title = matching.map(item => `${item.requestId} · ${item.status}`).join('\n');
    }
    if (state.calendarFirstSelection === iso || $('leave-start').value === iso || $('leave-end').value === iso) dayButton.classList.add('selected-date', 'selected');
    dayButton.addEventListener('click', () => selectCalendarDate(iso));
    root.append(dayButton);
  }
}

function selectCalendarDate(iso) {
  if (!state.calendarFirstSelection) {
    state.calendarFirstSelection = iso;
    $('leave-start').value = iso; $('leave-end').value = iso;
    toast(`Start date drafted: ${iso}. Select an end date.`);
  } else {
    const first = state.calendarFirstSelection;
    $('leave-start').value = first <= iso ? first : iso;
    $('leave-end').value = first <= iso ? iso : first;
    state.calendarFirstSelection = null;
    toast(`Leave date range drafted: ${$('leave-start').value} to ${$('leave-end').value}.`);
  }
  renderCalendar();
}
$('calendar-prev').addEventListener('click', () => { state.calendarMonth = new Date(state.calendarMonth.getFullYear(), state.calendarMonth.getMonth() - 1, 1); renderCalendar(); });
$('calendar-next').addEventListener('click', () => { state.calendarMonth = new Date(state.calendarMonth.getFullYear(), state.calendarMonth.getMonth() + 1, 1); renderCalendar(); });

async function loadAttendanceReport() {
  const params = new URLSearchParams();
  if ($('report-from').value) params.set('from', $('report-from').value);
  if ($('report-to').value) params.set('to', $('report-to').value);
  try {
    const payload = await api(`/api/reports/attendance${params.toString() ? `?${params}` : ''}`);
    state.attendanceReport = Array.isArray(payload.items) ? payload.items : [];
    renderAttendanceReport();
  } catch (error) { tableMessage($('report-rows'), 6, error.message); $('report-count').textContent = 'Could not load report'; toast(error.message, true); }
}

function renderAttendanceReport() {
  const query = $('report-search').value.trim().toLocaleLowerCase();
  const mapping = $('report-mapping').value;
  let items = state.attendanceReport.filter(item => searchable(item, ['employeeName', 'employeeId', 'scheduleId'], query) &&
    (mapping === 'all' || (mapping === 'mapped' ? item.hasScheduleMapping : !item.hasScheduleMapping)));
  items = sortItems(items, $('report-sort').value, $('report-order').value);
  state.visibleReport = items;
  const target = $('report-rows'); target.replaceChildren();
  if (!items.length) tableMessage(target, 6, 'No employees match the report filters.');
  items.forEach(item => {
    const row = document.createElement('tr');
    row.append(cell(`${item.employeeName} · ${item.employeeId}`), cell(item.hasScheduleMapping ? `Schedule ${item.scheduleId}` : 'No mapping'), cell(item.rawRecordCount), cell(item.firstRecordDate || '—'), cell(item.lastRecordDate || '—'));
    const action = document.createElement('td');
    if (item.hasScheduleMapping) action.append(button('View rows', 'open-attendance', item.employeeId)); else action.textContent = '—';
    row.append(action); target.append(row);
  });
  const total = items.reduce((sum, item) => sum + Number(item.rawRecordCount || 0), 0);
  $('report-count').textContent = `${items.length} employee ${items.length === 1 ? 'row' : 'rows'} · ${total} raw records in selected range`;
  $('report-export').disabled = items.length === 0;
}

$('report-apply').addEventListener('click', loadAttendanceReport);
['report-search', 'report-mapping', 'report-sort', 'report-order'].forEach(id => $(id).addEventListener(id === 'report-search' ? 'input' : 'change', renderAttendanceReport));
$('report-rows').addEventListener('click', event => {
  const target = event.target.closest('button[data-action="open-attendance"]'); if (!target) return;
  setView('attendance'); $('attendance-filter-employee').value = target.dataset.id;
  $('attendance-from').value = $('report-from').value; $('attendance-to').value = $('report-to').value;
  loadAttendance();
});

function updateOverview() {
  $('overview-employees').textContent = String(state.employees.length);
  $('overview-attendance').textContent = String(state.allAttendance.length);
  $('overview-leave').textContent = String(state.leaveRequests.filter(item => item.status === 'Pending review').length);
  $('overview-tickets').textContent = String(state.tickets.filter(item => item.status === 'Open').length);
}

$('employee-export').addEventListener('click', () => exportCsv('motorph-demo-employees-filtered.csv', employeeSummaryRows(state.visibleEmployees)));
$('team-export').addEventListener('click', () => exportCsv('motorph-demo-team-roster-filtered.csv', employeeSummaryRows(state.visibleTeam)));
$('directory-summary-export').addEventListener('click', () => {
  const rows = [];
  const statusCounts = new Map(); const departmentCounts = new Map();
  state.employees.forEach(item => { statusCounts.set(item.status, (statusCounts.get(item.status) || 0) + 1); departmentCounts.set(item.department, (departmentCounts.get(item.department) || 0) + 1); });
  rows.push({ category: 'Total employees', label: 'All', count: state.employees.length });
  [...statusCounts.entries()].forEach(([label, count]) => rows.push({ category: 'Status', label, count }));
  [...departmentCounts.entries()].forEach(([label, count]) => rows.push({ category: 'Department', label, count }));
  exportCsv('motorph-demo-directory-summary.csv', rows);
});
$('attendance-export').addEventListener('click', () => exportCsv('motorph-demo-attendance-raw.csv', state.visibleAttendance.map(({ attendanceId, employeeId, scheduleId, date, timeIn, timeOut, dayType, source }) => ({ attendanceId, employeeId, scheduleId, date, timeIn, timeOut, dayType, source }))));
$('leave-export').addEventListener('click', () => exportCsv('motorph-demo-leave-requests.csv', state.leaveRequests));
$('ticket-export').addEventListener('click', () => exportCsv('motorph-demo-tickets.csv', state.tickets));
$('report-export').addEventListener('click', () => exportCsv('motorph-demo-attendance-coverage-filtered.csv', state.visibleReport));

$('payroll-sample').addEventListener('click', () => {
  $('pay-cash').value = '25000'; $('pay-sss').value = '25000'; $('pay-philhealth').value = '25000'; $('pay-pagibig').value = '25000'; $('pay-taxable').value = '25000';
});
$('payroll-form').addEventListener('submit', async event => {
  event.preventDefault();
  const payload = { cashPay: $('pay-cash').value, sssCompensation: $('pay-sss').value, philHealthMbs: $('pay-philhealth').value,
    pagibigFundSalary: $('pay-pagibig').value, taxableCompensation: $('pay-taxable').value };
  try {
    state.payrollResult = await api('/api/payroll/preview', { method: 'POST', body: JSON.stringify(payload) });
    renderPayrollResult(state.payrollResult);
    $('payroll-export').disabled = false;
    toast('Monthly statutory estimate calculated. No payroll record was saved.');
  } catch (error) { toast(error.message, true); }
});

function resultRow(label, value, className = '') {
  const row = document.createElement('tr'); if (className) row.className = className;
  const name = document.createElement('td'); name.textContent = label;
  const amount = document.createElement('td'); amount.textContent = money(value);
  row.append(name, amount); return row;
}
function resultSection(table, label) {
  const row = document.createElement('tr'); const td = document.createElement('td'); td.colSpan = 2; td.className = 'result-section'; td.textContent = label;
  row.append(td); table.append(row);
}
function renderPayrollResult(result) {
  const target = $('payroll-result'); target.replaceChildren(); target.className = 'result-ready';
  const hero = document.createElement('div'); hero.className = 'result-hero';
  const label = document.createElement('span'); label.textContent = 'ESTIMATED NET PAY · MONTHLY';
  const amount = document.createElement('strong'); amount.textContent = money(result.netPayEstimate);
  const caption = document.createElement('small'); caption.textContent = result.profileId;
  hero.append(label, amount, caption); target.append(hero);
  const table = document.createElement('table'); table.className = 'result-table';
  const tbody = document.createElement('tbody'); table.append(tbody);
  resultSection(tbody, 'EMPLOYEE STATUTORY DEDUCTIONS');
  tbody.append(resultRow('SSS employee share', result.contributions.sssEmployee));
  tbody.append(resultRow('PhilHealth employee share', result.contributions.philHealthEmployee));
  tbody.append(resultRow('Pag-IBIG employee share', result.contributions.pagibigEmployee));
  tbody.append(resultRow('Withholding tax', result.withholdingTax));
  tbody.append(resultRow('Total employee deductions', result.totalEmployeeDeductions, 'result-total'));
  resultSection(tbody, 'EMPLOYER CONTRIBUTIONS · NOT DEDUCTED FROM NET PAY');
  tbody.append(resultRow('SSS employer SS + MPF share', result.contributions.sssEmployer));
  tbody.append(resultRow('SSS employer EC', result.contributions.sssEmployerEc));
  tbody.append(resultRow('PhilHealth employer share', result.contributions.philHealthEmployer));
  tbody.append(resultRow('Pag-IBIG employer share', result.contributions.pagibigEmployer));
  tbody.append(resultRow('Total employer statutory cost', result.employerStatutoryCost, 'result-total'));
  resultSection(tbody, 'BASES AND TOTALS');
  tbody.append(resultRow('Applied SSS monthly salary credit', result.contributions.sssMsc));
  tbody.append(resultRow('Regular SSS MSC component', result.contributions.sssRegularMsc));
  tbody.append(resultRow('SSS MPF MSC component', result.contributions.sssMpfMsc));
  tbody.append(resultRow('Applied PhilHealth MBS', result.contributions.philHealthMbsApplied));
  tbody.append(resultRow('Applied Pag-IBIG Fund Salary', result.contributions.pagibigFundSalaryApplied));
  tbody.append(resultRow('Taxable base after employee contributions', result.taxableCompensationAfterEmployeeContributions));
  tbody.append(resultRow('Cash pay', result.inputs.cashPay));
  tbody.append(resultRow('Employer cost estimate', result.employerCostEstimate, 'result-total'));
  target.append(table);
  const notes = document.createElement('p'); notes.className = 'form-note';
  notes.textContent = 'Not persisted. This estimate excludes attendance-derived earnings, overtime, holidays, leave, bonuses, reimbursements, other deductions, and any semi-monthly allocation. It is a coursework estimate, not payroll advice.';
  target.append(notes);
}

$('payroll-export').addEventListener('click', () => {
  if (!state.payrollResult) return;
  const result = state.payrollResult; const rows = [];
  const push = (section, label, amount) => rows.push({ profileId: result.profileId, period: result.period, section, item: label, amount: Number(amount || 0).toFixed(2), persisted: false, isPayslip: false });
  push('Employee deductions', 'SSS employee', result.contributions.sssEmployee);
  push('Employee deductions', 'PhilHealth employee', result.contributions.philHealthEmployee);
  push('Employee deductions', 'Pag-IBIG employee', result.contributions.pagibigEmployee);
  push('Employee deductions', 'Monthly withholding tax', result.withholdingTax);
  push('Employee deductions', 'Total employee deductions', result.totalEmployeeDeductions);
  push('Employer contributions', 'SSS employer SS and MPF', result.contributions.sssEmployer);
  push('Employer contributions', 'SSS employer EC', result.contributions.sssEmployerEc);
  push('Employer contributions', 'PhilHealth employer', result.contributions.philHealthEmployer);
  push('Employer contributions', 'Pag-IBIG employer', result.contributions.pagibigEmployer);
  push('Employer contributions', 'Total employer statutory cost', result.employerStatutoryCost);
  push('Result', 'Estimated net pay', result.netPayEstimate);
  push('Result', 'Estimated employer cost', result.employerCostEstimate);
  exportCsv('motorph-monthly-statutory-preview-not-a-payslip.csv', rows);
});

$('reset-demo').addEventListener('click', async () => {
  if (!window.confirm('Reset local synthetic employees, attendance, leave requests, and tickets to the supplied demo seed?')) return;
  try {
    await api('/api/demo/reset', { method: 'POST', body: '{}' });
    state.payrollResult = null;
    $('payroll-export').disabled = true;
    $('payroll-result').className = 'result-empty';
    $('payroll-result').textContent = 'Demo data reset. Run a new calculation to see an estimate.';
    await refreshAll(); toast('Synthetic demo data reset.');
  } catch (error) { toast(error.message, true); }
});

async function refreshAll() {
  await loadEmployees();
  await Promise.all([loadAllAttendance(), loadWorkflows(), loadAttendanceReport()]);
  updateOverview();
}

async function boot() {
  try {
    const health = await api('/api/health');
    if (health.mode !== 'LOCAL_SYNTHETIC') throw new Error('Unexpected application mode.');
    await refreshAll();
    const today = localDate(new Date());
    $('attendance-date').value = today;
    $('leave-start').value = today; $('leave-end').value = today;
    const firstEmployee = state.employees[0]?.employeeId || '';
    if (firstEmployee) {
      $('attendance-filter-employee').value = firstEmployee;
      await loadAttendance();
    }
  } catch (error) { globalMessage(`The local API is unavailable: ${error.message}. Start the app using run.sh or run.bat.`); }
}

boot();
```


### `test/check_html_bindings.py`

```python
from html.parser import HTMLParser
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]

class IdParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
        self.duplicates = set()
    def handle_starttag(self, tag, attrs):
        values = dict(attrs)
        element_id = values.get('id')
        if element_id:
            if element_id in self.ids:
                self.duplicates.add(element_id)
            self.ids.add(element_id)

html = (ROOT / 'web' / 'index.html').read_text(encoding='utf-8')
js = (ROOT / 'web' / 'app.js').read_text(encoding='utf-8')
parser = IdParser()
parser.feed(html)

literal_refs = set(re.findall(r"\$\('([a-zA-Z][a-zA-Z0-9_-]*)'\)", js))
missing = sorted(literal_refs - parser.ids)
if parser.duplicates:
    raise SystemExit('Duplicate HTML IDs: ' + ', '.join(sorted(parser.duplicates)))
if missing:
    raise SystemExit('JavaScript references missing HTML IDs: ' + ', '.join(missing))

required = {
    'employee-sort', 'team-sort', 'hr-sort', 'attendance-filter-employee',
    'attendance-summary-count', 'leave-filter-from', 'leave-filter-to',
    'calendar-prev', 'calendar-next', 'report-mapping', 'report-search',
    'employee-detail-dialog', 'payroll-export'
}
missing_required = sorted(required - parser.ids)
if missing_required:
    raise SystemExit('Expected product controls missing: ' + ', '.join(missing_required))

print(f'PASS: {len(parser.ids)} unique HTML IDs; {len(literal_refs)} literal JavaScript bindings resolve.')
```


### `run.sh`

```sh
#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java -name '*.java' -print > build/main-sources.txt
javac --release 11 -encoding UTF-8 -d build/classes @build/main-sources.txt
exec java -cp build/classes ph.motorph.ms2.Main
```


### `test.sh`

```sh
#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' -print > build/test-sources.txt
javac --release 11 -encoding UTF-8 -d build/test-classes @build/test-sources.txt
java -ea -cp build/test-classes ph.motorph.ms2.SmokeTest
node --check web/app.js
printf '%s\n' 'PASS: browser JavaScript syntax check'
python test/check_html_bindings.py
```


### `run.bat`

```bat
@echo off
setlocal
cd /d "%~dp0"
if not exist build\classes mkdir build\classes
dir /s /b src\main\java\*.java > build\main-sources.txt
javac --release 11 -encoding UTF-8 -d build\classes @build\main-sources.txt
if errorlevel 1 exit /b 1
java -cp build\classes ph.motorph.ms2.Main
```


### `test.bat`

```bat
@echo off
setlocal
cd /d "%~dp0"
if not exist build\test-classes mkdir build\test-classes
dir /s /b src\main\java\*.java src\test\java\*.java > build\test-sources.txt
javac --release 11 -encoding UTF-8 -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1
java -ea -cp build\test-classes ph.motorph.ms2.SmokeTest
if errorlevel 1 exit /b 1
node --check web\app.js
if errorlevel 1 exit /b 1
echo PASS: browser JavaScript syntax check
python test\check_html_bindings.py
if errorlevel 1 exit /b 1
```


### `.gitignore`

```gitignore
build/classes/
build/test-classes/
build/main-sources.txt
build/test-sources.txt
data/*.tmp
*.log
.DS_Store
Thumbs.db
```


### `data/demo-data.json`

```json
{"employees":[{"employeeId":"DEMO-001","name":"Mara Rivera","department":"People Operations","role":"HR Coordinator","location":"Makati","status":"Active","scheduleId":101},{"employeeId":"DEMO-002","name":"Eli Santos","department":"Finance","role":"Payroll Associate","location":"Taguig","status":"Active","scheduleId":102},{"employeeId":"DEMO-003","name":"Noa Reyes","department":"Operations","role":"Operations Lead","location":"Quezon City","status":"On leave","scheduleId":103}],"attendance":[{"attendanceId":1,"scheduleId":101,"date":"2026-10-01","timeIn":"08:26","timeOut":"17:03","dayType":"Regular"},{"attendanceId":2,"scheduleId":101,"date":"2026-10-02","timeIn":"08:31","timeOut":"17:00","dayType":"Regular"},{"attendanceId":3,"scheduleId":101,"date":"2026-11-02","timeIn":"08:24","timeOut":"17:02","dayType":"Regular"},{"attendanceId":4,"scheduleId":102,"date":"2026-10-01","timeIn":"08:17","timeOut":"17:10","dayType":"Regular"},{"attendanceId":5,"scheduleId":102,"date":"2026-10-02","timeIn":"08:35","timeOut":"17:00","dayType":"Regular"},{"attendanceId":6,"scheduleId":103,"date":"2026-10-01","timeIn":"08:28","timeOut":"17:01","dayType":"Regular"}],"leaveRequests":[],"tickets":[],"meta":{"mode":"LOCAL_SYNTHETIC","seedVersion":1,"createdAt":"2026-11-04T09:59:51.675673465Z"}}
```


---

# Appendix C — Full earlier source syntheses

These were created in earlier steps from the supplied user materials and are included in the archive separately as well. Their text is reproduced here so a receiving agent can search the complete detailed inventory without opening the earlier files.


## Full source document: `reference-materials/MotorPH_Payroll_System_Full_Synthesis.md`

# MotorPH Payroll System — Complete Source Synthesis

**Purpose:** A comprehensive, text-first reconstruction of the three supplied MO-IT107 documents, prepared so that an agent which cannot inspect the original PDFs or their embedded diagrams can still work from the same substantive information.

**System:** MotorPH Payroll System  
**Author shown in the submissions:** Jether Daniel A. Torres  
**Programme shown in the worksheets:** BSIT – Software Development  
**Academic year shown:** A.Y. 2024–2025  
**Course:** MO-IT107, Information Management  

> **Source fidelity rule:** This synthesis reports what the three supplied documents say. It does not silently repair schema differences, infer missing database constraints, invent payroll formulas, or replace the submitted design with a different design. Where the documents disagree, both versions are recorded and the disagreement is explained. Interpretations that connect two parts of the documents are identified as interpretations rather than direct source statements.

---

## Table of contents

1. [What this reference contains](#1-what-this-reference-contains)
2. [Source documents and their roles](#2-source-documents-and-their-roles)
3. [System overview and functional scope](#3-system-overview-and-functional-scope)
4. [Visual reconstruction of the database schema diagram](#4-visual-reconstruction-of-the-database-schema-diagram)
5. [Visual reconstruction of the data-flow diagrams](#5-visual-reconstruction-of-the-data-flow-diagrams)
6. [Database table inventory and functional grouping](#6-database-table-inventory-and-functional-grouping)
7. [Complete data dictionary: Employee Details](#7-complete-data-dictionary-employee-details)
8. [Complete data dictionary: Logging](#8-complete-data-dictionary-logging)
9. [Complete data dictionary: Government Details](#9-complete-data-dictionary-government-details)
10. [Complete data dictionary: Payroll](#10-complete-data-dictionary-payroll)
11. [Complete data dictionary: Static-Update-Only tables](#11-complete-data-dictionary-static-update-only-tables)
12. [Additional table in the database-design submission: Rest Day](#12-additional-table-in-the-database-design-submission-rest-day)
13. [Relationship catalogue](#13-relationship-catalogue)
14. [End-to-end functional traces](#14-end-to-end-functional-traces)
15. [Constraints, access controls, retention, and operational rules](#15-constraints-access-controls-retention-and-operational-rules)
16. [Cross-document discrepancies and unresolved design questions](#16-cross-document-discrepancies-and-unresolved-design-questions)
17. [What the documents do not specify](#17-what-the-documents-do-not-specify)
18. [Page-by-page source map](#18-page-by-page-source-map)
19. [Working instructions for a downstream agent](#19-working-instructions-for-a-downstream-agent)
20. [Data Dictionary glossary and acronym definitions](#20-data-dictionary-glossary-and-acronym-definitions)

---

## 1. What this reference contains

This file synthesises three documents supplied together:

1. **Data Dictionary for MotorPH Payroll System Database** — the Week 7, Worksheet No. 4 submission for logical schema design. It provides the table inventory, groups the tables by function, describes the columns, assigns data types and lengths or precision, and records field-level constraints.
2. **Database Design for the MotorPH Payroll System** — the second-milestone database-design submission. It provides the system overview, the visual schema diagram, detailed table definitions and table-level constraints, a relationship catalogue with cardinalities, and a short references section.
3. **Data Flow Diagram of MotorPH Payroll System** — the Week 11, Worksheet No. 6 submission. It provides a context-level DFD, a Level 1 DFD, an employee-management Level 2 decomposition, a payroll-processing Level 2 decomposition, and an archival Level 2 decomposition.

The synthesis is intentionally more than a summary. It converts information that is carried by tables, diagram labels, connector arrows, colour keys, and page layouts into readable text. It retains field names, spellings, type declarations, identifiers, stated cardinalities, process names, data-store names, and flow labels. The intention is that a receiving agent can answer detailed questions about the documents without having to reopen the PDFs.

### 1.1 Source hierarchy used in this synthesis

The documents overlap, but they do not constitute one perfectly consistent specification. The following handling rules are applied:

- The **Data Dictionary** is treated as the main source for the column-by-column catalogue, including data types, lengths, precision, descriptions, and field-level constraints.
- The **Database Design** submission is treated as the main source for table-level constraints, the relationship matrix, the extra `Rest Day` table, and the ERD/schema-diagram presentation.
- The **DFD** submission is treated as the main source for system actors, processes, data stores, directional data flows, payroll sub-processes, and archival stages.
- The **ERD image** is transcribed as a separate visual source. Where the image displays a key or column not stated in the corresponding prose table, that difference is retained.
- The **relationship matrix** is transcribed as written. It is not silently expanded to include every foreign key that appears elsewhere in the documents.
- The **DFD labels** are kept close to their original wording. Abbreviations such as `1YO`, `2YO`, and `opt*` are not expanded because the DFD does not define them.

### 1.2 Conventions used below

- `PK` means primary key, `FK` means foreign key, and `Unique` means a stated uniqueness rule.
- `NOT NULL`, `NULL allowed`, `DEFAULT(CURRENT_TIMESTAMP)`, `Min(0)`, `Max(31)`, and similar expressions are reported in the same sense as the source.
- `Decimal(19,4)` means precision 19 and scale 4 where the source provides those values. `Decimal(4,2)` means precision 4 and scale 2.
- `Varchar(n)` is used as a compact rendering of the source's `Varchar` plus `Size(n)` notation.
- When the source gives only a broad type such as `Enum`, `SET`, `DateTime`, `Varbinary`, or `Integer`, this synthesis does not invent an underlying SQL dialect or a more specific implementation type.
- `No constraint stated` means the relevant source table shows a dash or says `None`, or does not identify a constraint for that field. It does not establish that a real implementation should have no constraint.
- The source documents use both `Pag-ibig` and `Pag-IBIG` in different places. This synthesis generally uses `Pag-ibig` when reproducing field labels and `Pag-IBIG` when discussing the government programme in prose.
- `Government PII` is the DFD's wording for government-related personally identifiable information, which includes government identifiers held in the `Government` table.

---

## 2. Source documents and their roles

### 2.1 Data Dictionary for MotorPH Payroll System Database

**Document identity:**

- Course: MO-IT107, Information Management.
- Week: 7.
- Topic: Logical Schema Design.
- Worksheet: No. 4, Data Dictionary for MotorPH Payroll System Database.
- Business Analyst: Jether Daniel A. Torres.
- Programme and year level: BSIT – Software Development; A.Y. 2024–2025.
- Main document title: *Data Dictionary for MotorPH Payroll System Database*.

The document explains that a data dictionary describes the significance, organisation, and context of data items by describing fields in a dataset. Its instructions say to access the team’s Milestone 1 output, use that information to fill the table requirements, replace the template's red text, and remove the instruction note before submission.

Its substantive contents are:

1. **Table Definitions** — a list of database tables grouped as Employee Details, Logging, Government Details, Payroll, and Static-Update Only.
2. **Column Definitions** — a field catalogue containing column names, descriptions, data types, lengths or precision, and constraints.

The Data Dictionary describes 29 named tables. It does not include a separate `Rest Day` table, although it does describe a `restDay` field in `Schedule` as a `SET`.

### 2.2 Database Design for the MotorPH Payroll System

**Document identity:**

- Title: *Database Design for the MotorPH Payroll System*.
- Prepared and presented by: Torres, Jether Daniel A.
- Programme: Bachelor of Science in Information Technology.
- Term and year: 2nd Term – A.Y. 2024–2025.

The system overview describes a centralised payroll database intended to automate, organise, and streamline payroll operations. The submission presents five functional areas:

1. Employee Management.
2. Payroll Processing.
3. Attendance and Leave Tracking.
4. Government Contributions and Compliance.
5. Security and Access Control.

It then shows a schema diagram, lists table definitions and constraints, presents a relationship matrix, and cites an Entity-Relationship Diagram, a Data Dictionary, and a Schema Diagram in its references section.

The Database Design submission describes 30 tables in its table catalogue because it includes a separate `Rest Day` table. It also shows some fields in its ERD that are absent from its own prose table listings, and some fields that appear in the Data Dictionary but not in the prose table listings.

### 2.3 Data Flow Diagram of MotorPH Payroll System

**Document identity:**

- Course: MO-IT107, Information Management.
- Week: 11.
- Topic: Data Flow Diagrams.
- Worksheet: No. 6, Data Flow Diagram of MotorPH Payroll System.
- Business Analyst: Jether Daniel A. Torres.
- Programme and year level: BSIT – Software Development; A.Y. 2024–2025.

The worksheet defines a data-flow diagram as a tool for understanding movement of data inside a system or process. The instructions direct the student to consult two business documents:

1. *Business Requirement Specification Document for Payroll System*.
2. *MotorPH Existing Business Processes – Payroll*.

The DFD is required to use **Gene & Sarson notation** and to be decomposed at least to **Level 2**. The submitted DFD contains a context-level diagram, a Level 1 diagram, an employee-management Level 2 view, a payroll-processing Level 2 view, and an archival Level 2 view.

The references table names the same two business documents as the sources for the DFD. Their full text is not included in the three uploaded PDFs, so their requirements and process details cannot be independently reconstructed from this source set.

---

## 3. System overview and functional scope

### 3.1 Overall purpose

The Database Design submission describes the MotorPH Payroll System database as a centralised, secure, and scalable repository for most payroll-related data. Its stated intended effects are:

- automate and organise payroll operations;
- reduce manual effort in payroll handling;
- support faster retrieval and processing of payroll-related data;
- enable timely salary computation;
- support tax management;
- support legally required government contributions and compliance;
- improve data derivation and reduce unnecessary effort; and
- accommodate future organisational growth through mechanisms for data expansion.

The system is presented as a repository that connects employee, attendance, leave, compensation, deductions, government contribution, payroll, and summary information. The Employee record is described as the primary reference point for most modules, with nearly every module eventually tied to its unique identifier.

### 3.2 Employee Management

The Employee Management function captures, updates, and maintains employee records. The Database Design overview says these include:

- employment status;
- department affiliation;
- salary structure; and
- personal information.

It is described as the primary reference for other modules, with most other modules connected to the employee's unique identifier.

The DFD divides employee-side operations into three Level 2 processes:

- `1.1 Employee Information Handling`;
- `1.2 Schedule and Time Tracking`; and
- `1.3 Government Info Access`.

The associated stores are employee address, job profile, personally identifiable information, attendance, leave, and changelog records.

### 3.3 Payroll Processing

Payroll Processing is described as automated computation that accounts for:

- attendance records;
- salary adjustments;
- overtime hours;
- tax deductions; and
- government-mandated contributions, specifically SSS, PhilHealth, Pag-IBIG, and withholding tax.

The overview says the system provides a **semi-monthly payslip for each employee** and a **monthly summary for each payroll processing cycle**. The schema has a `Payslip` table containing the pay-period start and end dates, while the DFD has a payslip-generation process and a monthly-summary compilation process.

The DFD decomposes payroll into seven processes:

- `2.1 Timesheet Validation`;
- `2.2 Leave Monetisation Process`;
- `2.3 Wage and Adjustments`;
- `2.4 Government Deductibles`;
- `2.5 Basic Payroll Computation`;
- `2.6 Payslip Generation & Distribution`; and
- `2.7 Monthly Summary Compilation`.

### 3.4 Attendance and Leave Tracking

The overview says the system integrates with an employee attendance logging system to fetch clock-in and clock-out data automatically. It also records and catalogues leave applications.

The purpose of this function is to enable payroll calculations to:

- account for approved absences;
- account for active work presence during holidays and rest days; and
- determine applicable leave benefits.

The schema stores attendance using `Attendance`, linked to `Schedule`. Leave requests are stored in `Leave`, linked to `Leave Balance` and leave type. Leave accruals are recorded in `Leave Accrual History`, and leave monetisation is represented in `Leave Monetisation`.

### 3.5 Government Contributions and Compliance

The overview says the system calculates and maintains records of statutory government-related organisational contributions, including employer and employee deductions for government agencies. It is also said to generate tax reports and government contribution summaries to assist regulatory compliance.

The government-related data model contains:

- `Government`, holding government identifiers for an employee;
- `Government Deductibles`, holding employer contribution, employee contribution, and withholding-tax amounts by government record and month;
- `Contributions`, holding specific PhilHealth, Pag-ibig, and SSS contribution values by contributor;
- `Government Tally`, holding overall contribution totals by contributor; and
- `Summary`, which contains combined contribution and tax totals.

The source documents do not provide the actual statutory rates or bracket values. They define tables intended to hold rates and tax brackets.

### 3.6 Security and Access Control

The Database Design overview describes role-based access control (RBAC) with hierarchical or tiered access for:

- System administrators;
- Human Resources personnel;
- Payroll staff; and
- employees.

The stated security objectives are to restrict modifications to authorised personnel, reduce unauthorised changes, and make the account responsible for a modification traceable through audit logs.

The document further states that access to an employee's personally identifiable information is barred unless two authorised personnel approve it. It says that only the corresponding contribution values are normally visible and accessible. The DFD reflects this with `View PII (On Approval)`, `Update PII (On Approval)`, and `PII Access Approval` flows, and a distinct `1.3 Government Info Access` process.

The ERD/schema diagram uses a red highlight for the `Government` entity, and its legend labels red as **Additional Security Required**. This visual emphasis is consistent with the prose statement that government identifiers require special access control.

---

## 4. Visual reconstruction of the database schema diagram

**Source location:** Database Design PDF, page 5, heading `2. Schema Diagram`.

The image is a dense entity-and-relationship schema. It contains the named tables below, their principal fields, PK/FK markings, and connector lines. The diagram is not merely decorative: it shows fields and foreign-key links that are not fully represented in the prose table definitions.

### 4.1 Diagram legend

The diagram includes a colour key with four meanings:

| Colour shown in diagram | Legend meaning |
|---|---|
| Pale orange / yellow | Update, Append, Read |
| Pale purple / lavender | Update, Read |
| Pale cyan / teal | Append, Read |
| Red | Additional Security Required |

The `Government` entity is the red-highlighted entity in the diagram. The source does not provide a separate written access-control matrix mapping each colour to each user role, so the legend should be understood as the diagram's own operation/security key rather than as a complete permissions specification.

### 4.2 Tables visible in the schema diagram

The diagram visibly contains the following entities and key columns. This inventory is a transcription of the diagram, not a replacement for the table-by-table field catalogue later in this file.

#### Employee and organisational structure

- **Employee** — `employeeID` (PK), `addressID` (FK), `positionID` (FK), `supervisorID` (FK), `firstName`, `lastName`, `contactNumber`, `birthdate`, `employmentStatus`.
- **Address** — `addressID` (PK), `cityID` (FK), `regionID` (FK), `building`, `street`, `postalCode`.
- **Department** — `departmentID` (PK), `departmentName`.
- **Position** — `positionID` (PK), `departmentID` (FK), `positionName`.
- **Supervisor** — `supervisorID` (PK), `employeeID` (FK), `departmentID` (FK).
- **Schedule** — `scheduleID` (PK), `employeeID` (FK), `restDayID` (FK), `startTime`, `requiredDailyHours`.
- **Rest Day** — `restDayID` (PK), plus Boolean fields `sunday`, `monday`, `tuesday`, `wednesday`, `thursday`, `friday`, and `saturday`.

#### Leave and attendance

- **Leave Balance** — `balanceID` (PK), `leaveType` (FK), `scheduleID` (FK), `currentBalance`, `lastUpdated`.
- **Leave Type** — `leaveType` (PK), `mandatory`, `defaultAccrualRate`, `accrualFrequency`, `maxAccrual`.
- **Leave** — `leaveID` (PK), `balanceID` (FK), `leaveType` (FK), `scheduleID` (FK), `dateStart`, `hourDuration`, `leaveReason`, `leaveStatus`, `requestLeaveCredit`, `remainingLeaveCredit`, `dateRequested`, `dateApproved`, `approverID`.
- **Leave Accrual History** — `accrualID` (PK), `balanceID` (FK), `leaveType` (FK), `accrualDate`, `creditAccrued`, `oldBalance`, `newBalance`.
- **Leave Monetisation** — `monetisationID` (PK), `leaveType` (FK as represented in the ERD), `balanceID` (FK), `creditsMonetised`, `monetaryValuePer`, `totalConverted`, `dateMonetised`, `approverID`.
- **Attendance** — `attendanceID` (PK), `scheduleID` (FK), `date`, `timeIn`, `timeOut`, `dayType`.
- **Changelog** — `changelogNo` (PK), `employeeID` (FK), `createdAt`, `appenderID`, `updatedAt`, `editorID`, `fieldCode`, `dataType`, `oldValue`, `newValue`.
- **Holiday** — `holidayID` (PK), `holidayName`, `holidayDate`.

#### Government identifiers and deductions

- **Government** — `governmentID` (PK), `employeeID` (FK), `TIN`, `sssNo`, `philHealthNo`, `pagIbigNo`, `encryptedRef`. The entire entity is highlighted red in the schema image, matching the `Additional Security Required` key.
- **Government Deductibles** — `monthID` and `governmentID` are both shown with PK/FK markings; fields also include `employerContribution`, `employeeContribution`, and `withholdingTax`.
- **Contributions** — `governmentID` and `contributor` are shown as composite PK fields; `governmentID` is also an FK. The amounts are `philHealthContribution`, `pagIbigContribution`, and `sssContribution`.
- **Government Tally** — `summaryID` and `contributor` are shown as key fields, with `summaryID` also marked as an FK; amounts are `overallPagIbig`, `overallPhilHealth`, and `overallSSS`.

#### Payroll and reporting

- **Payroll** — `payrollID` (PK), `employeeID` (FK), `rebateID` (FK), `wageID` (FK), `summaryID` (FK), `monthID`, `ptoMax`, `hourlyRate`, `dailyRate`, `monthlyRate`, `netPay`.
- **Payslip** — `payslipID` (PK), `payrollID` (FK), `periodStartDate`, `periodEndDate`, `regularHours`, `overtimeHours`, `holidayOvertimeHours`, `ptoHours`, `undertimeHours`, `daysWorked`, `holidaysWorked`, `restDaysWorked`, `grossIncome`.
- **Earnings** — `wageID` (PK), `overtimePay`, `ptoPay`, `holidayPay`, `restDayPay`, `riceSubsidy`, `phoneStipend`, `clothesStipend`, `otherIncentTotal`, `totalBenefits`.
- **Deductibles** — `rebateID` (PK), `governmentID` (FK), `otherDeductTotal`, `totalDeductibles`.
- **Other Transactions** — `transactionID` (PK), `payrollID` (FK), `transactionType`, `transactionAmount`, `description`.
- **Summary** — `summaryID` (PK), `monthID` (FK), `overallGrossIncome`, `overallBenefits`, `employeeDisbursementTotal`, `employerDisbursementTotal`, `overallPITotal`, `overallPHTotal`, `overallSSSTotal`, `overallTaxTotal`, `governmentDisbursementTotal`.

#### Static/reference tables

- **Regions** — `regionID` (PK), `regionName`.
- **Cities** — `cityID` (PK), `regionID` (FK), `cityName`.
- **Months** — `monthID` (PK), `monthlyWorkdays`.
- **Rates** — `implementationYear` (shown as PK in the diagram), `piEmployerRate`, `piEmployeeRate`, `piMaxContribution`, `phRate`, `sssRate`.
- **Tax Table** — `taxBracket` (PK), `minCompensation`, `maxCompensation`, `bracketWidth`, `taxRate`.

### 4.3 What the ERD visually communicates

The diagram's connector network places `Employee` near the right side and connects it to `Address`, `Position`, and `Supervisor`. `Position` is connected to `Department`, while `Supervisor` links back to `Employee`. `Schedule` links to `Employee` and to the separate `Rest Day` table. `Attendance` links to `Schedule`.

The leave cluster places `Leave`, `Leave Balance`, `Leave Type`, `Leave Accrual History`, and `Leave Monetisation` near one another. Their connectors depict the leave-request, leave-balance, leave-type, accrual-history, and monetisation associations.

The payroll cluster links `Payroll` with `Earnings`, `Deductibles`, and `Summary`. `Payslip` is connected to `Payroll`. `Other Transactions` is connected to `Payroll`. `Government Deductibles` and `Contributions` connect government records and contributor-specific amounts, while `Government Tally` is attached to monthly `Summary` totals.

The reference-data portion places `Address` with `Cities` and `Regions`, `Cities` with `Regions`, and the tax/rate/month/holiday/leave-type entities around the calculation and reference tables.

### 4.4 Important visual-only schema details

The image makes several details especially clear:

1. `Schedule` contains `restDayID` rather than the `restDay` `SET` column described in the Data Dictionary and prose table list.
2. A standalone `Rest Day` table exists in the ERD, with seven weekday Boolean fields.
3. `Employee` in the ERD does not visibly contain `departmentID` or `governmentID`, although these are in the Data Dictionary's Employee field list.
4. `Address` in the ERD visibly contains `regionID`, although the Database Design prose definition of Address lists only `addressID`, `cityID`, `building`, `street`, and `postalCode`.
5. `Supervisor` in the ERD visibly contains `departmentID`, although the Database Design prose definition of Supervisor lists only `supervisorID` and `employeeID`.
6. `Rates` visibly marks `implementationYear` as the primary key, although the Database Design's table constraint section says the Rates table has no primary key, and the Data Dictionary does not list `implementationYear` at all.
7. `Government Deductibles` visibly shows both `monthID` and `governmentID` as key fields, consistent with a composite key.
8. `Contributions` visibly shows `(governmentID, contributor)` as a composite key.
9. `Government Tally` visibly shows `summaryID` and `contributor` as key fields, although the Data Dictionary calls `summaryID` its primary key “(assumed)”.

The exact consequences of these discrepancies are not resolved by the three documents. See [Section 16](#16-cross-document-discrepancies-and-unresolved-design-questions).

---

## 5. Visual reconstruction of the data-flow diagrams

**Source locations:** Data Flow Diagram PDF, pages 2–6. The actual diagrams are image-based; the following sections convert their labels, arrows, and visual groupings into text.

### 5.1 DFD notation and reading conventions

The worksheet expressly calls for **Gene & Sarson notation**. The diagrams use:

- rounded or rectangular cyan/teal shapes for external entities such as Employee, Supervisor, Human Resources, Payroll Staff, and System Admin;
- yellow/orange-topped process shapes for system processes, numbered `0.0`, `1.0`, `2.0`, `3.0`, and the decomposed Level 2 process numbers;
- lavender/purple data-store shapes, generally labelled with a `D` identifier and a store name; and
- directed connectors, with text labels describing the data or action moving along each connector.

The diagrams are process-and-data-flow views. They do not show every physical database column, and a data store in a DFD is not necessarily a one-to-one representation of a single relational table. The DFD uses larger logical stores such as `Employee Repository`, `Logs`, and `Payroll`, then subdivides them into named components in the Level 2 diagrams.

### 5.2 External entities

Five external actors appear across the DFDs:

| Actor | Role represented in the diagrams |
|---|---|
| **Employee** | Supplies attendance and time-in/time-out information; views employee information and payslips; views personal payslip information in the payroll Level 2 view. |
| **Supervisor** | Views team-member information and team payslips; adjusts team attendance; handles leave requests; handles or initiates leave monetisation; submits team-member wage and adjustment information; views team benefits/deductions. |
| **Human Resources** | Updates employee details; views employee and payroll details; participates in PII access approval and PII viewing/updating on approval; views relevant employee, attendance, and leave information. |
| **Payroll Staff** | Views and verifies employee and payroll details; views employee government PII on approval; updates payroll, payslip, benefits, and deductibles; validates and verifies payroll details and monthly reports. |
| **System Admin** | Manages configurations and users; views all employee information; participates in PII access approval; handles backup and archival; initiates the annual archival prompt; updates optional/system-controlled values in the detailed payroll diagram. |

The actors are distinct. The diagrams do not collapse HR, Payroll Staff, Supervisor, and System Admin into one generic administrator role.

### 5.3 Data stores in the DFD

| Identifier | Store name | Diagram placement / contents represented |
|---|---|---|
| `D1` | Employee Repository | Level 1 store for employee information. |
| `D1.1` | Employee Address | Level 2 subdivision for employee address data. |
| `D1.2` | Job Profile | Level 2 subdivision for employee job-profile data. |
| `D1.3` | Personally Identifiable Information | Level 2 subdivision for PII, shown beside Government Info Access. |
| `D2` | Logs | Level 1 store for operational logs, expanded into attendance, leave, changelog, and leave-monetisation logs. |
| `D2.1` | Attendance / Attendance Logs | Attendance and time activity used for work-hour derivation and timesheet validation. |
| `D2.2` | Leave / Leave Logs | Leave records used to derive paid-leave details and support leave management. |
| `D2.3` | Changelog / Changelogs | Employee-information change history. |
| `D2.4` | Leave Monetisation Logs | History of leave credits converted to monetary value. |
| `D3` | Payroll | Level 1 store containing wage, payroll, payslip, and benefits/deductibles information. |
| `D3.1` | Wage | Basic remunerative values and wage values. |
| `D3.2` | Benefits & Deductibles | Benefit and deduction values, including adjustments and computed deductions. |
| `D3.3` | Tax Table & Rates | Tax table and rates used to compute contributions. |
| `D3.4` | Payslip | Generated payslip records. |
| `D3.5` | Monthly Summary Report | Generated monthly summary report. |
| `D4` | Cold Storage | Destination for old logs and payroll records held for later compression. |

The Level 1 diagram names `D1 Employee Repository`, `D2 Logs`, `D3 Payroll`, and `D4 Cold Storage`. The Level 2 diagrams split the first three into more granular stores. These identifiers should be preserved when discussing the submitted diagrams.

### 5.4 Context-level DFD (`LV0: Context Diagram`)

**Source image:** DFD PDF, page 2. The context diagram represents the entire `MotorPH Payroll System` as one process, numbered `0.0`, surrounded by the five external entities.

#### 5.4.1 Employee flows

- **Employee → MotorPH Payroll System:** `Attendance and Time In/Out`.
- **MotorPH Payroll System → Employee:** `View Employee Information` (the information is supplied by the system in response to the employee's view/access activity).
- **MotorPH Payroll System → Employee:** `View Payslip`.

The diagram therefore presents the employee as both an input source for attendance/time data and a recipient of employee information and payslip output.

#### 5.4.2 Supervisor flows

- **MotorPH Payroll System → Supervisor:** `View Team Payslips`.
- **Supervisor → MotorPH Payroll System:** `Adjust Team Attendance`.
- **Supervisor → MotorPH Payroll System:** `Handle Leave Requests and Monetisation`.
- **Supervisor → MotorPH Payroll System:** `Handling Team Member Benefits / Deductions`.
- **MotorPH Payroll System → Supervisor:** `View Team Members' Employee Info`.

The diagram distinguishes the supervisor's ability to view team information from the supervisor's submission or handling of team attendance, leave, monetisation, and benefits/deductions matters.

#### 5.4.3 Human Resources flows

- **Human Resources → MotorPH Payroll System:** `Update Employee Details`.
- **Human Resources → MotorPH Payroll System:** `Update PII (On Approval)`.
- **MotorPH Payroll System → Human Resources:** `View PII (On Approval)`.
- **MotorPH Payroll System → Human Resources:** `View Employee and Payroll Details`.
- **Human Resources → MotorPH Payroll System:** `PII Access Approval`.

The approval condition is part of the flow labels. The diagram does not present PII viewing or updating as unrestricted operations.

#### 5.4.4 Payroll Staff flows

- **MotorPH Payroll System → Payroll Staff:** `View and Verify Employee and Payroll Details`.
- **MotorPH Payroll System → Payroll Staff:** `View Employees' Government PII (On Approval)`.
- **Payroll Staff → MotorPH Payroll System:** `Update Payroll, Payslip, and Benefits/Deductibles`.

The government PII view has an explicit approval condition. Payroll Staff also have a separate update flow for payroll-related records.

#### 5.4.5 System Admin flows

- **System Admin → MotorPH Payroll System:** `Configuration & User Management`.
- **MotorPH Payroll System → System Admin:** `View All Employee Information`.
- **System Admin ↔ MotorPH Payroll System:** `Archival Handling` (the diagram presents the archival interaction between the system and the administrator).
- **System Admin → MotorPH Payroll System:** `PII Access Approval`.

The context diagram shows the system administrator as responsible for configuration and user management, broad employee-information viewing, archival handling, and an approval role in the PII access path.

### 5.5 Level 1 DFD: major processes and data stores

**Source image:** DFD PDF, page 3. The Level 1 DFD decomposes the single context process into three major processes:

- `1.0 Employee Management`;
- `2.0 Payroll Processing`; and
- `3.0 Archival`.

The major data stores are `D1 Employee Repository`, `D2 Logs`, `D3 Payroll`, and `D4 Cold Storage`.

#### 5.5.1 Process 1.0 — Employee Management

`1.0 Employee Management` operates on the `D1 Employee Repository` and the `D2 Logs` store.

**Employee Repository (`D1`) flows:**

- `Append/Modify Employee Info` — information is written to or modified in the employee repository.
- `Retrieve Employee Information` — employee information is retrieved from the repository for the process.

**Employee Management and Logs (`D2`) flows shown in the diagram:**

- `View Leave and Attendance Details`.
- `View Employee Changelog`.
- `Append/Modify Leave and Attendance`.
- `Append Employee Info Changes`.

**Employee Management process labels and access-related flows:**

- `PII Access Approval`.
- `Configurations & User Management`.
- `View All Employee Information`.
- `Update Employee Details`.
- `View Employee Info`.
- `View PII (On Approval)`.
- `Update PII (On Approval)`.

The Level 1 diagram connects Employee Management with Employee, Supervisor, Human Resources, Payroll Staff, and System Admin. The employee and supervisor interactions include attendance/time-in/time-out, employee-information viewing, team-member employee-information viewing, attendance adjustment, and leave-request handling. HR performs employee-detail updates and participates in approved PII operations. Payroll Staff view and verify employee information and may view government PII on approval. System Admin handles configuration/user management, broad employee-information viewing, and PII-related approval.

#### 5.5.2 Process 2.0 — Payroll Processing

`2.0 Payroll Processing` handles payroll-related calculations and updates. It connects with `D3 Payroll` and with relevant records in `D2 Logs`.

**Payroll store (`D3`) flows:**

- `Update Wage, Payroll, Payslip, and Benefits/Deductibles` — updated payroll information is written to the payroll store.
- `Retrieve Wage, Payroll, Payslip, and Benefits/Deductibles` — payroll information is retrieved for processing or viewing.

**Log-related flows:**

- `Derive work hours` — work-hour information is derived from attendance logs.
- `Append/Modify Leave Monetisation History` — leave-monetisation history is recorded or modified.
- `Retrieve (2YO) Payroll Details` — the Level 1 diagram shows this label in the archival/log retrieval path associated with payroll processing.

**Actor-facing flows:**

- Supervisor: `Handle Leave Monetisation`, `View Team Payslips`, and `Handling Team Member Wage and Adjustments`.
- Payroll Staff: `View and Verify Payroll Details` and `Update Payroll, Payslip, and Benefits/Deductibles`.
- Human Resources: `View Payroll Details`.
- System Admin: `Configurations & User Management` and the system-level archival/configuration interactions.

The diagram's label `2YO` is reproduced exactly. The document does not define whether it means “two years old”, “two-year-old”, or another retention code, so no expansion is imposed here.

#### 5.5.3 Process 3.0 — Archival

`3.0 Archival` is shown as a separate process connected to `D2 Logs`, `D3 Payroll`, `D4 Cold Storage`, and System Admin.

Its visible labels include:

- `Retrieve (1YO) Attendance, Update, and Leave Monetisation Logs` — information is retrieved from the logs for archival handling.
- `Store Old Logs and Payroll for later compression` — archival output is stored in `D4 Cold Storage`.
- `Backup and Archival` — the administrator participates in the archival process.

The diagram contains a `1YO` label in the log retrieval path and a `2YO` payroll-related label in the process-2.0 path. The more detailed archival Level 2 diagram later makes the record groups explicit: it compiles `1YO` records from the operational logs and `2YO` records from Payslip.

### 5.6 Level 2 DFD: Employee Management (`1.0` decomposition)

**Source image:** DFD PDF, page 4. This diagram expands Employee Management into three processes and six data stores.

#### 5.6.1 Process 1.1 — Employee Information Handling

This process deals with employee address, job-profile, employee-information, and changelog activity.

**Employee-information stores:**

- `D1.1 Employee Address`.
  - `Append/Modify Employee Address` — process 1.1 writes or changes employee address information.
  - `Retrieve Employee Address` — process 1.1 retrieves address information.
- `D1.2 Job Profile`.
  - `Append/Modify Employee Job Profile` — process 1.1 writes or changes job-profile information.
  - `Retrieve Employee Job Profile` — process 1.1 retrieves job-profile information.
- `D2.3 Changelog`.
  - `Append on Employee Info Update` — employee-information updates are logged.
  - `View` — the changelog is available for viewing.

**Actor-facing labels:**

- Payroll Staff: `View and Verify Employee Details`.
- Payroll Staff: `View Employees' Government PII (On Approval)`.
- Supervisor: `View Team Members' Employee Info`.
- Employee: `View Employee Info`.
- Human Resources: `Update Employee Details`, `View Employee Info`, and the employee-information update path.
- System Admin: `Configurations & User Management` and `View All Employee Information`.

The diagram shows HR updating employee information, with an employee-information update being appended to the changelog. It also presents employee-information viewing separately for an employee, a supervisor, payroll staff, HR, and the system administrator.

#### 5.6.2 Process 1.2 — Schedule and Time Tracking

This process handles attendance and leave-related schedule activity.

**Inputs from external actors:**

- Employee → Process 1.2: `Attendance and Time In/Out`.
- Supervisor → Process 1.2: `Handle Leave Requests`.
- Supervisor → Process 1.2: `Adjust Team Attendance`.

**Data-store operations:**

- `D2.1 Attendance` — `View` and `Append/Modify` flows connect the process with the Attendance store.
- `D2.2 Leave` — `View` and `Append/Modify` flows connect the process with the Leave store.

The diagram positions attendance and leave as the operational records that are read and updated by Schedule and Time Tracking. The Level 1 diagram also shows work-hour derivation from logs as an input to payroll processing.

#### 5.6.3 Process 1.3 — Government Info Access

This process is dedicated to government information and PII access.

**Data store:**

- `D1.3 Personally Identifiable Information`.
  - `View` — the process reads PII from the store.
  - `Append/Modify` — the process writes or changes PII in the store.

**Access-control labels around the process:**

- `Access Approval`.
- `Update PII (On Approval)`.
- `View PII (On Approval)`.
- `Record Modification`.
- A second `Access Approval` flow is also drawn in the access path.

The process connects with Human Resources and System Admin in the approved-access area. The precise internal authorisation sequence is not defined in prose, but the visual labels clearly require approval for PII viewing and updating, and show a record-modification path. This is consistent with the database overview's statement that PII requires approval from two authorised personnel.

### 5.7 Level 2 DFD: Payroll Processing (`2.0` decomposition)

**Source image:** DFD PDF, page 5. This is the most detailed payroll-processing diagram. It decomposes process `2.0 Payroll Processing` into seven processes and five payroll stores, while also using attendance, leave, and leave-monetisation logs.

#### 5.7.1 Process 2.1 — Timesheet Validation

**Input stores:**

- `D2.1 Attendance Logs`.
- `D2.2 Leave Logs`.

**Flow labels:**

- `Derive work hours and other attendance data` — attendance logs supply the work-hour and other attendance information.
- `Derive details for paid leaves` — leave logs supply paid-leave details.
- `Get Time Activity` — timesheet validation passes time-activity information into basic payroll computation.

Process 2.1 is the point where attendance and paid-leave information is consolidated into time activity for the payroll calculation. The diagram does not state an algorithm for validating time entries or handling missing/overlapping entries.

#### 5.7.2 Process 2.2 — Leave Monetisation Process

**Input:** Supervisor → Process 2.2: `Process Approved Leave Monetisation`.

**Outputs:**

- `Append` — writes the monetisation event to `D2.4 Leave Monetisation Logs`.
- `Add to Benefits` — sends the monetary result into Process 2.3 Wage and Adjustments.

The process therefore depicts monetisation as a supervisor-handled, approved event, logged separately, with the resulting value added to benefits. The DFD does not state the conversion formula or define the leave categories eligible for monetisation.

#### 5.7.3 Process 2.3 — Wage and Adjustments

**Inputs and outputs:**

- Supervisor → Process 2.3: `Send Team Member Wage and Adjustments`.
- Process 2.2 → Process 2.3: `Add to Benefits`.
- Process 2.3 → `D3.1 Wage`: `Update Wage (opt*)`.
- Process 2.3 → `D3.2 Benefits & Deductibles`: `Append Adjustments`.
- System Admin → Process 2.3: `Update Values (opt*)`.

The diagram shows wage information and adjustments feeding payroll calculation. It explicitly distinguishes a wage-store update from appending adjustments to the Benefits & Deductibles store. The `opt*` marker is part of the submitted diagram, but no legend or note defines it.

#### 5.7.4 Process 2.4 — Government Deductibles

**Inputs and outputs:**

- System Admin → Process 2.4: `Update Values (opt*)`.
- System Admin / rate maintenance path → `D3.3 Tax Table & Rates`: `Update Rates (opt*)`.
- `D3.3 Tax Table & Rates` → Process 2.4: `Utilise Rates to Compute Contributions`.
- Process 2.4 → `D3.2 Benefits & Deductibles`: `Append to Deductibles`.
- Process 2.4 → Process 2.7: `Collate`.

Process 2.4 uses the tax table and rate store to compute contributions and appends the resulting amounts to deductibles. It also contributes information to monthly summary compilation. The DFD does not provide specific formulas, statutory rate values, thresholds, rounding rules, or calculation dates.

#### 5.7.5 Process 2.5 — Basic Payroll Computation

**Inputs:**

- Process 2.1 → Process 2.5: `Get Time Activity`.
- `D3.1 Wage` → Process 2.5: `Get Basic Remunerative Values`.
- `D3.2 Benefits & Deductibles` → Process 2.5: `Apply Adjustments`.

**Outputs:**

- Process 2.5 → Process 2.6: `Get Net Pay and Constituents`.
- Process 2.5 ↔ Payroll Staff: `View, Validate, and Verify Payroll Details`.

This is the central calculation process. It combines time activity, basic remunerative values, and adjustments. The output is described as net pay and its constituents. The process is also subject to payroll-staff review, validation, and verification.

The term `Basic Payroll Computation` is the process name. The source does not give a mathematical formula for gross pay, deductions, net pay, overtime, holiday pay, undertime, or any other component.

#### 5.7.6 Process 2.6 — Payslip Generation & Distribution

**Inputs:** Process 2.5 → Process 2.6: `Get Net Pay and Constituents`.

**Output store:**

- Process 2.6 → `D3.4 Payslip`: `Append`.

**User-facing flows:**

- Employee: `View Personal` — employee views their personal payslip information.
- Supervisor: `View Team` — supervisor views team payslip information.
- Human Resources: `View` — HR views payslip/report information.

The process creates payslip records from the net-pay calculation and makes relevant payslip information available to the employee, supervisor, and HR. The Level 1 diagram also describes a `View Payslip` flow at the context level.

#### 5.7.7 Process 2.7 — Monthly Summary Compilation

**Inputs:**

- Process 2.4 → Process 2.7: `Collate`.
- `D3.4 Payslip` → Process 2.7: `Collate`.

**Outputs:**

- Process 2.7 → `D3.5 Monthly Summary Report`: `Generate`.
- Payroll Staff ↔ Process 2.7: `View, Validate, and Verify Monthly Report`.

Monthly Summary Compilation collates payroll/payslip and government-deduction/contribution information, produces a Monthly Summary Report, and allows Payroll Staff to view, validate, and verify the report.

### 5.8 Level 2 DFD: Archival (`3.0` decomposition)

**Source image:** DFD PDF, page 6. This diagram decomposes the Level 1 Archival process into two processes:

- `3.1 Appending`;
- `3.2 Deleting`.

It shows four operational log stores, one payslip store, and cold storage:

- `D2.1 Attendance Logs`;
- `D2.2 Leave Logs`;
- `D2.3 Changelogs`;
- `D2.4 Leave Monetisation Logs`;
- `D3.4 Payslip`; and
- `D4 Cold Storage`.

#### 5.8.1 Process 3.1 — Appending

- System Admin → Process 3.1: `Trigger on Annual Prompt`.
- The process receives `Compile (1YO) Records` from the group of log stores: Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs.
- The process receives `Compile (2YO) Records` from `D3.4 Payslip`.
- Process 3.1 → `D4 Cold Storage`: `Store Old Logs and Payroll for later compression`.

The diagram thus depicts a scheduled archival event initiated by a system administrator, compilation of one group of records marked `1YO`, compilation of payslip records marked `2YO`, and transfer of old logs/payroll to cold storage for later compression.

#### 5.8.2 Process 3.2 — Deleting

- Process 3.2 is labelled `Deleting`.
- Its outgoing data-removal action is labelled `Remove Relevant Records`.
- The process is connected to the operational log stores `D2.1` through `D2.4` and to `D3.4 Payslip`.
- The diagram contains a `Trigger on Finish` connector between the archival stages.

The intended ordering shown is archival compilation/storage followed by deletion of the corresponding relevant source records after the preceding stage finishes. The diagram does not define the verification checks, recovery procedure, failure behaviour, legal retention basis, or exact calendar calculation behind the `1YO` and `2YO` labels.

### 5.9 End-to-end DFD interpretation

Across all DFD levels, the main operational chain is:

1. Employee and Supervisor interactions provide attendance, time, leave, and team information.
2. Employee Management maintains the employee repository, job profile, address, PII, attendance, leave, and changelog information.
3. Payroll Processing obtains validated time activity, leave details, wage information, adjustments, and statutory rates.
4. Basic Payroll Computation derives net pay and its constituents.
5. Payslip Generation & Distribution records and exposes payslip information.
6. Monthly Summary Compilation collates the period's payslip and government-related amounts into a monthly report.
7. Archival compiles older logs and payslip records, stores them in cold storage, and then removes relevant records from the operational stores.

This is a synthesis of the process and data-store structure shown in the diagrams. The source does not specify the exact timing or execution schedule of each payroll run, except that the overview describes semi-monthly payslips and monthly summaries, and the archival diagram identifies an annual prompt.

---

## 6. Database table inventory and functional grouping

The Data Dictionary arranges its 29 tables into five functional groups. The Database Design submission includes all of these, with the additional `Rest Day` table. The grouping below reproduces the Data Dictionary's organisation.

### 6.1 Group A — Employee Details

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Employee` | Preserves detailed records of all employees, incorporating unique identifiers, departmental affiliations, positional data, government-linked information, and supervisory relationships. |
| `Address` | Maintains discrete records of employees' physical addresses, incorporating city and regional identifiers, building details, street information, and postal codes. |
| `Department` | Delineates operational units, providing unique identifiers and names that define each departmental segment. |
| `Position` | Registers individual job roles by associating a unique position identifier with corresponding departmental information. |
| `Supervisor` | Documents supervisory assignments by linking supervisor identifiers with employee records and departmental associations. |
| `Schedule` | Records employee scheduling data, including start times, required daily working hours, and assigned weekly rest days, to support workforce management and payroll performance calculations. |
| `Leave Balance` | Tracks employee leave entitlements, maintaining balances per leave type and timestamps for the latest updates to support accrual management. |

### 6.2 Group B — Logging

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Changelog` | Archives modifications to employee records, including timestamps, field identifiers, previous values, and updated values for audit purposes. |
| `Attendance` | Registers attendance information, documenting clock-in and clock-out times, attendance dates, and day classification. |
| `Leave` | Records leave requests, including leave types, durations, justifications, approval statuses, and scheduled start dates. |
| `Leave Accrual History` | Records leave-accrual progression over time, including accrual dates, credited amounts, and balance transitions. |

### 6.3 Group C — Government Details

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Government` | Consolidates government-related identifiers for each employee. |
| `Government Deductibles` | Itemises monthly government-related deductions, listing employer and employee contributions alongside withholding taxes. |
| `Contributions` | Records individual government contributions, detailing employer and employee shares for PhilHealth, Pag-ibig, and SSS. |

### 6.4 Group D — Payroll

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Payroll` | Compiles payroll information by linking employee identifiers with wage, rebate, and summary records, while incorporating monthly identifiers and detailed salary rates. |
| `Payslip` | Generates individual payslip records within the pay period, including hour metrics such as overtime and undertime, and gross and net income figures. |
| `Leave Monetisation` | Documents the conversion of accrued leave into monetary value. |
| `Earnings` | Aggregates additional remuneration components such as overtime, paid time off, holiday pay, and stipends. |
| `Deductibles` | Details payroll deductions by listing rebate identifiers and other deduction totals, and aggregates these values to yield overall deductibles per payroll cycle. |
| `Other Transactions` | Records miscellaneous payroll-related financial transactions such as bonuses, advances, reimbursements, and additional deductions. |
| `Government Tally` | Consolidates government contribution totals over each payroll period, providing Pag-ibig, PhilHealth, and SSS totals according to contributor. |
| `Summary` | Organises a monthly financial summary of payroll disbursements, benefits, tax withholdings, and government disbursements. |

### 6.5 Group E — Static-Update Only

| Table | Purpose stated in the Data Dictionary |
|---|---|
| `Regions` | Maintains static region records using a unique identifier and region name. |
| `Cities` | Lists cities with unique identifiers and links them to corresponding regions. |
| `Holiday` | Enumerates public and recognised organisational holidays using unique identifiers, names, and dates. |
| `Months` | Defines monthly periods using month identifiers and a record of the number of workdays. |
| `Rates` | Specifies statutory contribution rates, including employer and employee Pag-ibig rates, maximum contributions, PhilHealth rates, and SSS rates. |
| `Tax Table` | Outlines tax brackets through income ranges, bracket widths, and associated tax rates. |
| `Leave Type` | Classifies leave categories, indicating whether each type is mandatory and specifying default accrual rates, accrual frequencies, and maximum accrual limits. |

**Grouping note:** The Data Dictionary table inventory labels the final category `E. Static-Update Only`, while the heading above its column definitions labels the same category `F. Static-Update Only`. The table group has seven listed entities in either case.

---

## 7. Complete data dictionary: Employee Details

This section reproduces every field specified in the Data Dictionary's Employee Details group. Field descriptions are retained in meaning, with line wraps repaired for readability. The final column reproduces the source's stated constraints. Table-level constraint details from the Database Design submission are added after each table where useful.

### 7.1 `Employee`

**Purpose:** Employee identity, employment status, organisational placement, address, government record, and supervisory linkage.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `employeeID` | `SmallInt`, size 5 | Unique employee identifier. | Primary Key; Unique. |
| `addressID` | `SmallInt`, size 5 | Linked address identifier. | Foreign Key. |
| `departmentID` | `TinyInt` | Identifier for the department. | Foreign Key. |
| `positionID` | `TinyInt` | Identifier for the position. | Foreign Key. |
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Foreign Key. |
| `supervisorID` | `SmallInt`, size 5 | Employee's immediate supervisor identifier. | Foreign Key. |
| `firstName` | `Varchar(255)` | Employee's given name. | No additional constraint stated. |
| `lastName` | `Varchar(255)` | Employee's surname. | No additional constraint stated. |
| `contactNumber` | `Varchar(15)` | Employee's phone number. | No additional constraint stated. |
| `birthdate` | `Date` | Employee's date of birth. | `CHECK (NOT NULL)` and `CHECK (birthdate < CURRENT_DATE)`. |
| `employmentStatus` | `Enum` | Current employment status. | No additional constraint stated. |

**Database Design prose version:** The table definition lists `employeeID`, `addressID`, `positionID`, `supervisorID`, `firstName`, `lastName`, `contactNumber`, `birthdate`, and `employmentStatus`. It does not list `departmentID` or `governmentID` among its columns. Its constraint section lists `employeeID` as PK; `addressID`, `positionID`, and `supervisorID` as FKs; `employeeID` as unique; and requires `birthdate` to be non-null and earlier than the current date.

**Database Design relationship-matrix version:** The Employee-to-Address, Employee-to-Position, and Employee-to-Supervisor relationships are specified, while the relationship matrix also refers to Government-to-Employee and Position-to-Department. The matrix does not show Employee's `departmentID` or `governmentID` as separate foreign-key entries in the Employee relationship rows, but the Data Dictionary does.

**Interpretation:** `Employee` is the core reference record. The source documents do not settle whether department membership is stored directly in `Employee`, derived through `Position`, or represented in both places. They also do not settle whether the `Government` relationship is held as `Employee.governmentID`, `Government.employeeID`, or both.

### 7.2 `Address`

**Purpose:** Employee address information, including city and region identifiers and the physical address components.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `addressID` | `SmallInt`, size 5 | Unique address record identifier. | Primary Key; Unique. |
| `cityID` | `SmallInt`, size 3 | Linked city identifier. | Foreign Key. |
| `regionID` | `TinyInt`, size 1 | Linked region identifier. | Foreign Key. |
| `building` | `Varchar(100)` | Building name or number. | No additional constraint stated. |
| `street` | `Varchar(150)` | Street name. | No additional constraint stated. |
| `postalCode` | `Varchar(5)` | Postal or ZIP code. | No additional constraint stated. |

**Database Design prose version:** The Address table lists `addressID`, `cityID`, `building`, `street`, and `postalCode`; it omits `regionID`. It lists `addressID` as PK and `cityID` as FK, with no check constraint.

**ERD image version:** The diagram contains both `cityID` and `regionID` as foreign keys in Address. It also shows `Cities.regionID` linking Cities to Regions. The relationship matrix specifies Address → Cities as N:1, but does not separately list Address → Regions.

### 7.3 `Department`

**Purpose:** Identifies an organisational department.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `departmentID` | `TinyInt` | Unique department identifier. | Primary Key; Unique. |
| `departmentName` | `Varchar(100)` | Name of the department. | No additional constraint stated. |

**Database Design prose version:** Identical two-column inventory. `departmentID` is the PK and unique identifier. There is no FK and no check constraint. The relationship matrix connects `Position.departmentID` to `Department.departmentID`, cardinality N:1.

### 7.4 `Position`

**Purpose:** Identifies an employee's position and the department to which the position belongs.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `positionID` | `TinyInt` | Unique position identifier. | Primary Key; Unique. |
| `departmentID` | `TinyInt` | Linked department identifier. | Foreign Key. |
| `positionName` | `Varchar(100)` | Name of the position. | No additional constraint stated. |

**Database Design prose version:** Identical field inventory. `positionID` is PK; `departmentID` is FK; `positionID` uniquely identifies each position; no check constraint is stated. The relationship matrix gives Position → Department a cardinality of N:1.

### 7.5 `Supervisor`

**Purpose:** Records a supervisor identity, the employee record associated with that supervisor, and departmental association.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `supervisorID` | `SmallInt`, size 5 | Unique supervisor record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `departmentID` | `TinyInt` | Linked department identifier. | Foreign Key. |

**Database Design prose version:** The table definition lists only `supervisorID` and `employeeID`. It gives `supervisorID` as PK, `employeeID` as FK, and says the supervisor identifier uniquely identifies each supervisor record. It does not list `departmentID` in that prose column table.

**ERD image version:** The Supervisor entity contains `supervisorID` (PK), `employeeID` (FK), and `departmentID` (FK). The relationship matrix says Supervisor → Employee is 1:1 and describes supervisors as employees themselves. The Data Dictionary's purpose description also mentions departmental associations.

### 7.6 `Schedule`

**Purpose:** Employee work schedule, including start time, required daily work hours, and weekly rest day.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `scheduleID` | `SmallInt`, size 5 | Unique schedule identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key; Unique. |
| `startTime` | `Time` | Scheduled work start time. | `NOT NULL`. |
| `requiredDailyHours` | `TinyInt`, size 2 | Number of work hours required per day. | `Min(1)`, `Max(24)`, `NOT NULL`. |
| `restDay` | `SET` | Assigned weekly rest day. | `NOT NULL`. |

**Database Design prose version:** It lists the same five columns and says `scheduleID` is PK, `employeeID` is FK, and a schedule uniquely represents each unique `employeeID` record. Its check-constraint wording says `startTime` and `restDay` must be supplied (`NOT NULL`) and `requiredDailyHours` must be between 1 and 24.

**ERD image version:** The diagram instead shows `restDayID` (FK) in Schedule and a separate `Rest Day` entity, with `restDayID` as its PK and seven Boolean weekday columns. This conflicts with the `restDay SET` field in the prose and Data Dictionary. See the detailed discrepancy note in Section 16.

### 7.7 `Leave Balance`

**Purpose:** Maintains the available balance of a particular leave type for an employee, using the employee's schedule as the employee association.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `balanceID` | `Integer` | Unique leave balance identifier. | Primary Key; Unique. |
| `leaveType` | `Enum` | Linked leave type identifier. | Foreign Key. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier for the employee. | Foreign Key. |
| `currentBalance` | `Decimal(4,2)` | Balance of the leave type under the employee. | `Min(0)`. |
| `lastUpdated` | `DateTime` | Update timestamp. | No additional constraint stated. |

**Database Design additions:**

- PK: `balanceID`.
- FKs: `leaveType` and `scheduleID`.
- Unique: the combination `(leaveType, scheduleID)` must be unique, so the same leave type is not represented more than once for the same schedule.
- Check: `currentBalance` must be greater than or equal to zero.

The Database Design description says `balanceID` uniquely identifies each leave type balance for each `scheduleID`. The diagram represents Leave Balance as connected to Leave Type and Schedule.

---

## 8. Complete data dictionary: Logging

The Logging group records employee changes, attendance, leave requests, and accrual history. These records supply information used by Schedule and Time Tracking, employee audit review, leave management, and payroll calculations.

### 8.1 `Changelog`

**Purpose:** Audit history of changes to employee-linked information.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `changelogNo` | `Integer` | Unique changelog entry identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `createdAt` | `DateTime` | Record creation timestamp. | `NOT NULL`; default `CURRENT_TIMESTAMP`. |
| `appenderID` | `SmallInt`, size 5 | Identifier for the entry creator. | No additional constraint stated. |
| `updatedAt` | `DateTime` | Timestamp of the last update. | `NOT NULL`; default `CURRENT_TIMESTAMP`. |
| `editorID` | `SmallInt`, size 5 | Identifier for the entry editor. | No additional constraint stated. |
| `fieldCode` | `Varchar(10)` | Shortened identifier for a table and column, e.g. `P_NP` for `Payroll.NetPay`. | No additional constraint stated. |
| `dataType` | `Enum` | Defines the type of the old and new values. | No additional constraint stated. |
| `oldValue` | `Varchar(255)` | Previous value before the change, stored as text. | No additional constraint stated. |
| `newValue` | `Varchar(255)` | Updated value after the change, stored as text. | No additional constraint stated. |

**Database Design additions:** `changelogNo` is PK; `employeeID` is FK; `changelogNo` uniquely identifies each change logged for employee-tied information. The check-constraint prose says `createdAt` and `updatedAt` must be non-null and have `CURRENT_TIMESTAMP` assigned if empty. The Data Dictionary represents the same timestamp rule as field-level `NOT NULL` plus default `CURRENT_TIMESTAMP`.

The DFD has a `D2.3 Changelog` store. Employee-information updates are appended to the changelog, and the Level 1/Level 2 diagrams show viewing and appending changelog information. The archival DFD includes Changelogs in the `Compile (1YO) Records` group.

### 8.2 `Attendance`

**Purpose:** Attendance time records tied to an employee's schedule.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `attendanceID` | `Integer` | Unique attendance record identifier. | Primary Key; Unique. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier. | Foreign Key. |
| `date` | `Date` | Attendance date. | Unique in combination with `scheduleID`. |
| `timeIn` | `Time` | Time when the employee clocked in. | Check: `timeIn <= timeOut`. |
| `timeOut` | `Time` | Time when the employee clocked out. | Check: `timeIn <= timeOut`. |
| `dayType` | `Enum` | Type of day. | No additional constraint stated. |

**Database Design additions:** `attendanceID` is PK; `scheduleID` is FK; `(scheduleID, date)` must be unique; `timeIn` must be less than or equal to `timeOut`.

**DFD role:** Attendance is stored as `D2.1 Attendance` in the employee-management Level 2 view and `D2.1 Attendance Logs` in the payroll and archival views. It receives employee attendance and time-in/time-out data. The payroll Level 2 diagram derives work hours and other attendance data from these logs for Timesheet Validation. The archival diagram compiles Attendance Logs as part of `1YO` records.

### 8.3 `Leave`

**Purpose:** Employee leave requests, including the leave type, duration, justification, status, credits, request timestamp, approval timestamp, and approving supervisor identifier.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `leaveID` | `Integer` | Unique leave record identifier. | Primary Key; Unique. |
| `balanceID` | `Integer` | Linked leave balance record identifier. | Foreign Key. |
| `leaveType` | `Enum` | Category of leave. | Foreign Key. |
| `scheduleID` | `SmallInt`, size 5 | Linked schedule identifier. | Foreign Key. |
| `dateStart` | `Date` | Leave start date. | `NOT NULL`; check `dateStart >= CURRENT_DATE`. |
| `hourDuration` | `SmallInt` | Duration of leave in hours. | `Min(0)`. |
| `leaveReason` | `Varchar(150)` | Justification for the leave request. | No additional constraint stated. |
| `leaveStatus` | `Enum` | Approval status. | No additional constraint stated. |
| `requestLeaveCredit` | `Decimal(4,2)` | Credits that will be consumed. | No additional constraint stated. |
| `remainingLeaveCredit` | `Decimal(4,2)` | Credits remaining after use. | No additional constraint stated. |
| `dateRequested` | `DateTime` | Timestamp of leave-request submission. | Default `CURRENT_TIMESTAMP`. |
| `dateApproved` | `DateTime` | Timestamp that remains null until the request is approved. | `NULL` allowed. |
| `approverID` | `SmallInt`, size 5 | Tracks the Supervisor ID who approved the leave. | No additional constraint stated. |

**Database Design version:** It lists all the above fields except `scheduleID`. It lists `leaveID` as PK and `balanceID` plus `leaveType` as FKs. Its check says `dateStart` must be non-null and on or after the current date. The Data Dictionary includes a `scheduleID` FK which is absent from the Database Design prose column list and table-level FK summary.

**DFD role:** `D2.2 Leave` is used by Schedule and Time Tracking, and is read to derive paid-leave details for Timesheet Validation. Supervisors handle leave requests. The archival DFD includes Leave Logs in the `1YO` compilation group.

### 8.4 `Leave Accrual History`

**Purpose:** Historical record of leave-credit accrual and the balance before and after each accrual.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `accrualID` | `Integer` | Unique leave accrual record identifier. | Primary Key; Unique. |
| `balanceID` | `Integer` | Linked leave balance record identifier. | Foreign Key. |
| `leaveType` | `Enum` | Category of leave for accrual purposes. | `NOT NULL`. |
| `accrualDate` | `Date` | Date when the leave accrual is recorded and applied. | `NOT NULL`; check `accrualDate <= CURRENT_DATE`. |
| `creditAccrued` | `Decimal(4,2)` | Number of leave credits accrued until the accrual date. | `Min(0)`. |
| `oldBalance` | `Decimal(4,2)` | Leave balance prior to the accrual. | `Min(0)`. |
| `newBalance` | `Decimal(4,2)` | Leave balance after the accrual. | `Min(0)`. |

**Database Design additions:** `accrualID` is PK; `balanceID` is FK; the unique identifier distinguishes each accrual instance; `accrualDate` must be present and no later than the current date; `creditAccrued`, `oldBalance`, and `newBalance` must each be non-negative.

**Relationship notes:** The relationship matrix lists Leave Accrual History → Leave Balance as N:1 and Leave Accrual History → Leave Type as N:1. The `balanceID` FK is listed in the table's constraint section, while the `leaveType` association is named in the matrix and the ERD. The Data Dictionary makes `leaveType` non-null but does not explicitly mark it as an FK in the field constraint column.

---

## 9. Complete data dictionary: Government Details

### 9.1 `Government`

**Purpose:** Holds each employee's government-related identifiers and an encrypted reference field.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Unique government record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `TIN` | `Varchar(15)` | Taxpayer Identification Number. | No additional constraint stated. |
| `sssNo` | `Varchar(12)` | Social Security System number. | No additional constraint stated. |
| `philHealthNo` | `Varchar(12)` | PhilHealth identifier. | No additional constraint stated. |
| `pagIbigNo` | `Varchar(12)` | Pag-ibig fund identifier. | No additional constraint stated. |
| `encryptedRef` | `Varbinary(Max)` | Encrypted reference data. | No additional constraint stated. |

**Database Design additions:** `governmentID` is PK; `employeeID` is FK; `governmentID` uniquely identifies each government record; no check constraint is stated. The relationship matrix specifies Government → Employee as 1:1. The schema diagram colours Government red, corresponding to `Additional Security Required` in the legend.

**Security relevance:** The overview says employee PII should be inaccessible unless two authorised personnel approve it, and that only corresponding contribution values are normally visible and accessible. The DFD has a separate Government Info Access process and PII data store, with access approval, on-approval viewing, on-approval updating, and record-modification flows. The source does not state the encryption algorithm, key-management procedure, masking format, or exact fields that qualify for ordinary versus approval-controlled display.

### 9.2 `Government Deductibles`

**Purpose:** Holds monthly employer-side and employee-side contribution amounts and withholding tax for a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Primary Key; Foreign Key. |
| `monthID` | `Enum` | Linked month identifier. | Foreign Key. |
| `employerContribution` | `Decimal(19,4)` | Employer contribution amount. | `Min(0)`. |
| `employeeContribution` | `Decimal(19,4)` | Employee contribution amount. | `Min(0)`. |
| `withholdingTax` | `Decimal(19,4)` | Amount of tax withheld. | `Min(0)`. |

**Database Design key definition:** The Database Design explicitly states the composite primary key `(governmentID, monthID)`. Both columns are treated as foreign keys in its constraint table, referencing Government and Months respectively. Its unique-constraint description repeats that `(governmentID, monthID)` uniquely identifies deductibles under each government record. Contribution amounts must be greater than or equal to zero.

**Relationship matrix:** Government Deductibles → Months is N:1; Government Deductibles → Government is 1:1. The ERD marks both `monthID` and `governmentID` as key fields, and both are marked as foreign keys.

**DFD role:** This information is computed in process `2.4 Government Deductibles`, using the tax table and rates, then appended to `D3.2 Benefits & Deductibles` and collated into the monthly summary process.

### 9.3 `Contributions`

**Purpose:** Records PhilHealth, Pag-ibig, and SSS amounts for a named contributor associated with a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Composite Primary Key; Foreign Key. |
| `contributor` | `Enum` | Identifier for the contributor. | Composite Primary Key. |
| `philHealthContribution` | `Decimal(19,4)` | PhilHealth contribution amount. | `Min(0)`. |
| `pagIbigContribution` | `Decimal(19,4)` | Pag-ibig contribution amount. | `Min(0)`. |
| `sssContribution` | `Decimal(19,4)` | SSS contribution amount. | `Min(0)`. |

**Database Design constraints:** Composite PK `(governmentID, contributor)`; `governmentID` is an FK; the composite pair uniquely identifies a government contribution per employee and employer; all contribution amounts must be greater than or equal to zero.

**Relationship matrix:** Contributions → Government Deductibles is shown as N:1, linked by `governmentID`, with the purpose that this is the reference location where computed contributions are collated. The wording does not explain how the `contributor` values are enumerated or whether employee and employer are the only allowed enum members, though the surrounding descriptions refer to both employee and employer shares.

---

## 10. Complete data dictionary: Payroll

The Payroll group contains the main payroll record, payslip, leave monetisation, earnings, deductions, miscellaneous financial transactions, government tallies, and monthly summary records.

### 10.1 `Payroll`

**Purpose:** Main employee payroll record linking an employee, wage calculation, deduction/rebate calculation, summary, payroll month, salary rates, PTO maximum, and net pay.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `payrollID` | `Integer` | Unique payroll record identifier. | Primary Key; Unique. |
| `employeeID` | `SmallInt`, size 5 | Linked employee identifier. | Foreign Key. |
| `rebateID` | `Integer` | Linked rebate record identifier. | Foreign Key. |
| `wageID` | `Integer` | Linked wage record identifier. | Foreign Key. |
| `summaryID` | `Integer` | Linked summary record identifier. | Foreign Key. |
| `monthID` | `Enum` | Identifier for the payroll month. | No additional constraint stated in the Data Dictionary. |
| `ptoMax` | `Integer` | Maximum allowed paid time off (PTO). | `Min(0)`. |
| `hourlyRate` | `Decimal(19,4)` | Pay rate per hour. | `Min(0)`; `NOT NULL`. |
| `dailyRate` | `Decimal(19,4)` | Fixed daily pay rate. | `Min(0)`; `NOT NULL`. |
| `monthlyRate` | `Decimal(19,4)` | Monthly salary rate. | `Min(0)`. |
| `netPay` | `Decimal(19,4)` | Final pay after deductions. | `Min(0)`; `NOT NULL`. |

**Database Design constraints:** PK `payrollID`; FKs `employeeID`, `rebateID`, `wageID`, and `summaryID`; `payrollID` is described as uniquely identifying each payroll per employee per period; `hourlyRate`, `dailyRate`, and `netPay` must be present (`NOT NULL`); `ptoMax` must be greater than or equal to zero.

**Relationship matrix:**

- Payroll → Employee: N:1.
- Payroll → Deductibles: 1:1 through `rebateID`.
- Payroll → Earnings: 1:1 through `wageID`.
- Payroll → Summary: N:1 through `summaryID`.
- Payroll → Months: N:1 through `monthID`.

The table's FK summary does not list `monthID` as an FK, although the relationship matrix explicitly connects Payroll to Months through that field. This discrepancy is retained in Section 16.

### 10.2 `Payslip`

**Purpose:** Stores the pay-period record and the employee's hours, days, gross income, and payslip identifier.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `payslipID` | `Integer` | Unique payslip identifier. | Primary Key; Unique. |
| `payrollID` | `Integer` | Linked payroll record identifier. | Foreign Key. |
| `periodStartDate` | `Date` | Start date of the pay period. | `NOT NULL`. |
| `periodEndDate` | `Date` | End date of the pay period. | Unique in combination with `payrollID`. |
| `regularHours` | `SmallInt` | Standard work hours. | `Min(0)`; `NOT NULL`. |
| `overtimeHours` | `SmallInt` | Overtime hours worked. | `Min(0)`. |
| `holidayOvertimeHours` | `SmallInt` | Overtime during holidays. | `Min(0)`. |
| `ptoHours` | `SmallInt` | Total paid time off hours taken. | `Min(0)`. |
| `undertimeHours` | `SmallInt` | Hours below expected work time. | `Min(0)`. |
| `daysWorked` | `TinyInt` | Total days worked. | `Min(0)`. |
| `holidaysWorked` | `TinyInt` | Number of holidays worked. | `Min(0)`. |
| `restDaysWorked` | `TinyInt` | Number of rest days worked. | `Min(0)`. |
| `grossIncome` | `Decimal(19,4)` | Total earnings before deductions. | `Min(0)`. |

**Database Design constraints:** PK `payslipID`; FK `payrollID`; `(payrollID, periodEndDate)` must be unique; hours and days worked must be greater than or equal to zero. The design text also says the payslip identifier uniquely identifies each payslip given to an employee per payroll.

**Relationship matrix:** Payslip → Payroll is 1:1 through `Payslip.payrollID`. The DFD's process `2.6 Payslip Generation & Distribution` appends generated payslip records to `D3.4 Payslip`, and gives Employee, Supervisor, and HR viewing flows. The Database Design overview says a semi-monthly payslip is provided for each employee.

### 10.3 `Leave Monetisation`

**Purpose:** Records accrued leave credits converted into monetary value, including the balance source, conversion amount, assigned monetary value, total, date, and approver.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `monetisationID` | `Integer` | Unique leave monetisation record identifier. | Primary Key; Unique. |
| `leaveType` | `Enum` | Type of leave being monetised. | `NOT NULL`. |
| `balanceID` | `Integer` | Linked leave balance record identifier for the monetisation. | Foreign Key. |
| `creditsMonetised` | `Decimal(4,2)` | Number of leave credits converted to monetary value. | `Min(0)`. |
| `monetaryValuePer` | `Decimal(19,4)` | Monetary value assigned per leave credit. | `Min(0)`. |
| `totalConverted` | `Decimal(19,4)` | Aggregate monetary value resulting from conversion. | `Min(0)`. |
| `dateMonetised` | `Date` | Date when leave monetisation was effected. | No additional constraint stated. |
| `approverID` | `SmallInt`, size 5 | Identifier for the approver responsible for validating the monetisation. | No additional constraint stated. |

**Database Design constraints:** PK `monetisationID`; FK `balanceID`; leave type must be provided (`NOT NULL`); `creditsMonetised`, `monetaryValuePer`, and `totalConverted` must be non-negative. The relationship matrix also lists Leave Monetisation → Leave Type as N:1, even though the table-level FK summary lists only `balanceID`.

**DFD role:** Supervisor → Process 2.2 is labelled `Process Approved Leave Monetisation`; Process 2.2 appends to `D2.4 Leave Monetisation Logs` and sends `Add to Benefits` to Process 2.3 Wage and Adjustments. The DFD thus presents a leave-monetisation event as an approved operation that is logged and then incorporated into employee benefits.

### 10.4 `Earnings`

**Purpose:** Holds the wage-side benefit and remuneration components.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `wageID` | `Integer` | Unique wage record identifier. | Primary Key; Unique. |
| `overtimePay` | `Decimal(19,4)` | Payment for overtime hours. | `Min(0)`. |
| `ptoPay` | `Decimal(19,4)` | Paid time off compensation. | `Min(0)`. |
| `holidayPay` | `Decimal(19,4)` | Payment for holiday work. | `Min(0)`. |
| `restDayPay` | `Decimal(19,4)` | Payment for work on rest days. | `Min(0)`. |
| `riceSubsidy` | `Decimal(19,4)` | Subsidy provided for rice. | `Min(0)`. |
| `phoneStipend` | `Decimal(19,4)` | Stipend for phone expenses. | `Min(0)`. |
| `clothesStipend` | `Decimal(19,4)` | Stipend for clothing expenses. | `Min(0)`. |
| `otherIncentTotal` | `Decimal(19,4)` | Sum of other incentives. | `Min(0)`. |
| `totalBenefits` | `Decimal(19,4)` | Total benefit amount. | `Min(0)`. |

**Database Design constraints:** PK `wageID`; no FK is listed on Earnings; `wageID` uniquely identifies benefit calculations per payroll; all monetary amounts must be greater than or equal to zero. Payroll refers to Earnings through `Payroll.wageID`, and the relationship matrix says Payroll → Earnings is 1:1.

### 10.5 `Deductibles`

**Purpose:** Stores the other-deduction total and aggregate deductible amount, associated with a government record.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `rebateID` | `Integer` | Unique rebate record identifier. | Primary Key; Unique. |
| `governmentID` | `SmallInt`, size 5 | Linked government record identifier. | Foreign Key. |
| `otherDeductTotal` | `Decimal(19,4)` | Sum of other deductions. | `Min(0)`. |
| `totalDeductibles` | `Decimal(19,4)` | Overall deductions sum. | `Min(0)`. |

**Database Design constraints:** PK `rebateID`; FK `governmentID`; `rebateID` uniquely identifies deductible calculations per payroll, including government deductibles; deduction amounts must be non-negative.

**Relationship matrix:** Payroll → Deductibles is 1:1 through `Payroll.rebateID`. Deductibles → Government Deductibles is also shown as 1:1 through `Deductibles.governmentID`, with the purpose that government deductibles are needed to compute total deductibles.

### 10.6 `Other Transactions`

**Purpose:** Stores miscellaneous payroll-related financial transactions such as bonuses, advances, reimbursements, and additional deductions.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `transactionID` | `Integer` | Unique transaction record identifier. | Primary Key; Unique. |
| `payrollID` | `Integer` | Payroll identifier for the transaction. | Foreign Key. |
| `transactionType` | `Enum` | Type of transaction. | No additional constraint stated. |
| `transactionAmount` | `Decimal(19,4)` | Amount of the transaction. | `Min(0)`. |
| `description` | `Varchar(150)` | Description of the transaction. | No additional constraint stated. |

**Database Design constraints:** PK `transactionID`; FK `payrollID`; the identifier uniquely distinguishes miscellaneous calculations per payroll; `transactionAmount` must be non-negative. The relationship matrix gives Other Transactions → Payroll a cardinality of N:1.

### 10.7 `Government Tally`

**Purpose:** Stores the overall Pag-ibig, PhilHealth, and SSS totals for each contributor within a monthly summary.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `summaryID` | `Integer` | Linked summary record identifier. | Primary Key is marked as “assumed” in the Data Dictionary. |
| `contributor` | `Enum` | Identifier for the contributor. | No additional constraint stated in the Data Dictionary. |
| `overallPagIbig` | `Decimal(19,4)` | Overall Pag-ibig contribution between this month's payrolls. | `Min(0)`. |
| `overallPhilHealth` | `Decimal(19,4)` | Overall PhilHealth contribution between this month's payrolls. | `Min(0)`. |
| `overallSSS` | `Decimal(19,4)` | Overall SSS contribution between this month's payrolls. | `Min(0)`. |

**Database Design constraints and ERD:** Composite PK `(summaryID, contributor)`; `summaryID` is an FK; the pair uniquely identifies overall totals for each government contribution per employee and employer; all overall contribution amounts must be non-negative. The ERD visually marks both `summaryID` and `contributor` as key fields, with `summaryID` also marked as an FK.

**Relationship matrix:** Government Tally → Summary is N:1 through `Government Tally.summaryID`. The stated purpose is to sum government-related disbursements within a summary.

**Important conflict:** The Data Dictionary labels `summaryID` as an assumed primary key, while the Database Design prose and ERD show a composite key including `contributor`. The composite-key version is clearly represented in the latter two sources, but this synthesis does not overwrite the Data Dictionary's recorded assumption.

### 10.8 `Summary`

**Purpose:** Stores monthly aggregated payroll, benefit, employee/employer disbursement, government contribution, tax, and government-disbursement totals.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `summaryID` | `Integer` | Unique summary record identifier. | Primary Key; Unique. |
| `monthID` | `Enum` | Linked month identifier. | No additional constraint stated in the Data Dictionary. |
| `overallGrossIncome` | `Decimal(19,4)` | Total gross income summed. | `Min(0)`. |
| `overallBenefits` | `Decimal(19,4)` | Total benefits summed. | `Min(0)`. |
| `employeeDisbursementTotal` | `Decimal(19,4)` | Total disbursements to employees. | `Min(0)`. |
| `employerDisbursementTotal` | `Decimal(19,4)` | Total amount disbursed by the employer. | `Min(0)`. |
| `overallPITotal` | `Decimal(19,4)` | Combined Pag-ibig total. | `Min(0)`. |
| `overallPHTotal` | `Decimal(19,4)` | Combined PhilHealth total. | `Min(0)`. |
| `overallSSSTotal` | `Decimal(19,4)` | Combined SSS total. | `Min(0)`. |
| `overallTaxTotal` | `Decimal(19,4)` | Total tax withheld summed. | `Min(0)`. |
| `governmentDisbursementTotal` | `Decimal(19,4)` | Total government disbursement summed. | `Min(0)`. |

**Database Design constraints:** PK `summaryID`; FK `monthID`; `summaryID` uniquely identifies monthly enumerative reports; all monetary totals must be non-negative.

**Relationship matrix:** Payroll → Summary is N:1; Government Tally → Summary is N:1. The monthly report process collates information and generates the `D3.5 Monthly Summary Report` store. The Database Design overview calls for a monthly summary for each payroll processing cycle.

---

## 11. Complete data dictionary: Static-Update-Only tables

These entities hold reference values, calendar data, contribution-rate data, tax-bracket data, and leave-type definitions. The Data Dictionary calls this group `E. Static-Update Only`; its column-definition subsection labels it `F. Static-Update Only`.

### 11.1 `Regions`

| Column | Data type / size | Description | Constraint |
|---|---|---|---|
| `regionID` | `TinyInt`, size 1 | Unique region identifier. | Primary Key; Unique. |
| `regionName` | `Varchar(100)` | Name of the region. | No additional constraint stated. |

**Database Design:** Same two columns. `regionID` is PK, no FK, unique by identifier, and no check constraint is stated.

### 11.2 `Cities`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `cityID` | `SmallInt`, size 3 | Unique city identifier. | Primary Key; Unique. |
| `regionID` | `TinyInt` | Linked region identifier. | Foreign Key. |
| `cityName` | `Varchar(100)` | Name of the city. | Unique. |

**Database Design:** Same three columns; PK `cityID`; FK `regionID`; `cityName` must have only one instance between records; no check constraint. Relationship matrix does not list a separate `Cities → Regions` row, but the `regionID` foreign-key column is present in the table and ERD.

### 11.3 `Holiday`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `holidayID` | `TinyInt` | Unique holiday record identifier. | Primary Key; Unique. |
| `holidayName` | `Varchar(255)` | Name of the holiday. | Unique. |
| `holidayDate` | `Date` | Date of the holiday. | Unique. |

**Database Design version:** PK `holidayID`; no FK; `holidayID` uniquely identifies each listed holiday; `(holidayName, holidayDate)` must be unique as a combination; no check constraint.

**Important difference:** The Data Dictionary marks `holidayName` and `holidayDate` individually as unique fields. The Database Design instead specifies a composite unique combination of name and date. These are not equivalent rules: independent uniqueness prevents two holidays from sharing a name or date, while pair uniqueness prevents only a duplicate name/date pair.

### 11.4 `Months`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `monthID` | `Enum` | Unique month identifier. | The field is described as an enum; no additional field-level constraint stated. |
| `monthlyWorkdays` | `Integer`, size 2 | Number of workdays in the month. | `Min(1)`, `Max(31)`. |

**Database Design constraints:** PK `monthID`; no FK; no separate unique constraint because `monthID` is already an Enum; `monthlyWorkdays` must be between 1 and 31.

### 11.5 `Rates`

**Purpose:** Holds statutory rate values for Pag-ibig, PhilHealth, and SSS.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `piEmployerRate` | `Decimal(5,3)` | Global employer Pag-ibig rate. | `Min(0)`; `NOT NULL`. |
| `piEmployeeRate` | `Decimal(5,3)` | Global employee Pag-ibig rate. | `Min(0)`; `NOT NULL`. |
| `piMaxContribution` | `Decimal(5,3)` | Maximum Pag-ibig contribution. | `Min(0)`; `NOT NULL`. |
| `phRate` | `Decimal(5,3)` | Global PhilHealth rate. | `Min(0)`; `NOT NULL`. |
| `sssRate` | `Decimal(5,3)` | Global SSS rate. | `Min(0)`; `NOT NULL`. |

**Database Design additional field:** The Database Design prose table and ERD include `implementationYear`, data type `SmallInt` (shown as size 4 in the diagram), described as the year the rates were implemented. The Data Dictionary omits this field.

**Database Design constraint conflict:** Its prose table says PK `None`, FK `None`, Unique `None`; the check-constraint text says all fields must be provided (`NOT NULL`) and rates must be greater than or equal to zero. The ERD, however, visibly marks `implementationYear` as `PK`. The documents do not resolve whether implementation year is intended to identify each rate set.

**DFD role:** `D3.3 Tax Table & Rates` provides the rates used by Process `2.4 Government Deductibles` to compute contributions. The System Admin has an `Update Rates (opt*)` flow to the store.

### 11.6 `Tax Table`

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `taxBracket` | `Integer` | Identifier for the tax bracket. | `Min(1)`; Primary Key. |
| `minCompensation` | `Decimal(19,4)` | Minimum income for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |
| `maxCompensation` | `Decimal(19,4)` | Maximum income for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |
| `bracketWidth` | `Decimal(19,4)` | Income range width for the bracket. | `Min(0)`. |
| `taxRate` | `Decimal(19,4)` | Tax rate for the bracket. | `Min(0)`; `NOT NULL` in the Database Design constraints. |

**Database Design constraints:** PK `taxBracket`; no FK; `taxBracket` uniquely identifies the withholding-tax criteria and consequent requirement. The check-constraint wording says `taxBracket` must “at least have and be greater than 1” (the source wording is unclear), that `minCompensation`, `maxCompensation`, and `taxRate` must be present, and that monetary values must be non-negative.

**Unresolved wording:** The Data Dictionary's `Min(1)` indicates a minimum of 1. The Database Design's prose has a malformed phrase that could be read as “at least have and be greater than 1”. It is unsafe to silently convert this to `>= 1` or `> 1` without an implementation decision.

### 11.7 `Leave Type`

**Purpose:** Defines the leave categories and their accrual rules.

| Column | Data type / size | Description | Data Dictionary constraint |
|---|---|---|---|
| `leaveType` | `Enum` | Classification of leave. | `NOT NULL`. |
| `mandatory` | `Boolean` | Indicates whether the leave type is mandatory. | No additional constraint stated. |
| `defaultAccrualRate` | `Decimal(4,2)` | Standard rate at which leave accrues for this type. | `Min(0)`. |
| `accrualFrequency` | `Enum` | Frequency of leave accrual. | No additional constraint stated. |
| `maxAccrual` | `Decimal(4,2)` | Maximum allowable accrual for the leave type. | `Min(0)`. |

**Database Design constraints:** PK `leaveType`; no FK; no separate unique constraint because `leaveType` is already an Enum; `defaultAccrualRate` and `maxAccrual` must be greater than or equal to zero.

---

## 12. Additional table in the database-design submission: `Rest Day`

**Source:** Database Design PDF, Section 3.7, and schema diagram on page 5. This table is not included in the Data Dictionary's table inventory or column-definition section.

| Column | Data type / size | Description | Database Design constraint |
|---|---|---|---|
| `restDayID` | `SmallInt(5)` | Unique identifier for the rest-day record. | Primary Key; Unique. |
| `sunday` | `Boolean` | Indicates whether Sunday is designated as a rest day. | No check constraint stated. |
| `monday` | `Boolean` | Indicates whether Monday is designated as a rest day. | No check constraint stated. |
| `tuesday` | `Boolean` | Indicates whether Tuesday is designated as a rest day. | No check constraint stated. |
| `wednesday` | `Boolean` | Indicates whether Wednesday is designated as a rest day. | No check constraint stated. |
| `thursday` | `Boolean` | Indicates whether Thursday is designated as a rest day. | No check constraint stated. |
| `friday` | `Boolean` | Indicates whether Friday is designated as a rest day. | No check constraint stated. |
| `saturday` | `Boolean` | Indicates whether Saturday is designated as a rest day. | No check constraint stated. |

**Constraints:** PK `restDayID`; no FK; the identifier uniquely identifies each record in the Rest Day table; no check constraint is stated.

**Schema role:** The ERD shows `Schedule.restDayID` as an FK to `Rest Day.restDayID`. This makes a schedule point to a row that can flag one or more weekdays as rest days. That is structurally different from the Data Dictionary's single `Schedule.restDay` field of type `SET`. The Database Design prose for Schedule also lists `restDay` rather than `restDayID`, so this remains an internal inconsistency in the database-design submission itself.

---

## 13. Relationship catalogue

This section reproduces the relationship matrix in the Database Design submission. The source calls out the relationship, cardinality, foreign-key column, and purpose. The cardinalities below are not recalculated from the column types; they are the cardinalities stated in the document.

### 13.1 Employee, address, position, and supervision relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Employee → Address | 1:1 | `Employee.addressID` | Each employee is assigned an address. |
| Employee → Position | N:1 | `Employee.positionID` | Every employee holds a specific position. |
| Employee → Supervisor | N:1 | `Employee.supervisorID` | References the supervisor of the employee. |
| Address → Cities | N:1 | `Address.cityID` | Each address entry is located within a specific city. |
| Position → Department | N:1 | `Position.departmentID` | Positions are assigned within departments. |
| Supervisor → Employee | 1:1 | `Supervisor.employeeID` | Supervisors are employees themselves, connecting both tables directly. |
| Schedule → Employee | 1:1 | `Schedule.employeeID` | Each employee is assigned a schedule. |
| Changelog → Employee | N:1 | `Changelog.employeeID` | Tracks changes made to employee information. |

The Employee → Supervisor and Supervisor → Employee entries describe two directions of the supervisor/employee association. The relationship matrix gives N:1 for an Employee's supervisor reference and 1:1 for the Supervisor record's link to its Employee record.

### 13.2 Leave and attendance relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Leave Balance → Leave Type | N:1 | `LeaveBalance.leaveType` | Indicates the leave type applicable for an employee leave balance. |
| Leave Balance → Schedule | N:1 | `LeaveBalance.scheduleID` | Leave balances are determined based on an employee's schedule. |
| Attendance → Schedule | N:1 | `Attendance.scheduleID` | Attendance records are linked to the schedule. |
| Leave → Leave Type | N:1 | `Leave.leaveType` | Indicates the leave type used for the request. |
| Leave → Leave Balance | N:1 | `Leave.balanceID` | Leave requests are validated against available leave balances. |
| Leave Accrual History → Leave Balance | N:1 | `LeaveAccrualHistory.balanceID` | Accrual records adjust the leave balance over time. |
| Leave Accrual History → Leave Type | N:1 | `LeaveAccrualHistory.leaveType` | Indicates the leave type that has been accrued. |
| Leave Monetisation → Leave Type | N:1 | `LeaveMonetisation.leaveType` | Indicates the leave type requested for conversion. |
| Leave Monetisation → Leave Balance | N:1 | `LeaveMonetisation.balanceID` | Converts leave credits into monetary value based on the leave balance. |

The Data Dictionary's `Leave` field list additionally shows `scheduleID` as a foreign key, but the relationship matrix does not include a separate Leave → Schedule row. The relationship matrix also lists Leave Monetisation → Leave Type, while the table-level constraint summary lists only `balanceID` as the foreign key. These differences are not resolved by the source.

### 13.3 Government and payroll relationships

| Relationship | Cardinality stated | Foreign-key field | Purpose stated |
|---|---:|---|---|
| Government → Employee | 1:1 | `Government.employeeID` | Links each government record with an employee. |
| Payroll → Employee | N:1 | `Payroll.employeeID` | Links payroll records to employees. |
| Payroll → Deductibles | 1:1 | `Payroll.rebateID` | Deduction computations of payroll. |
| Payroll → Earnings | 1:1 | `Payroll.wageID` | Benefit computations of payroll. |
| Payroll → Summary | N:1 | `Payroll.summaryID` | Collates payroll data into a monthly summary record. |
| Payroll → Months | N:1 | `Payroll.monthID` | References payroll month and its corresponding working days. |
| Government Deductibles → Months | N:1 | `GovernmentDeductibles.monthID` | References the month these deductibles are for. |
| Government Deductibles → Government | 1:1 | `GovernmentDeductibles.governmentID` | Associates deductibles with a government record. |
| Contributions → Government Deductibles | N:1 | `Contributions.governmentID` | References the location where computed contribution values are collated. |
| Deductibles → Government Deductibles | 1:1 | `Deductibles.governmentID` | Government deductibles are needed to compute total deductibles. |
| Payslip → Payroll | 1:1 | `Payslip.payrollID` | Each payslip is uniquely tied to a payroll record. |
| Other Transactions → Payroll | N:1 | `OtherTransactions.payrollID` | Connects miscellaneous transactions to payroll records. |
| Government Tally → Summary | N:1 | `GovernmentTally.summaryID` | Sums government-related disbursements within a summary. |

### 13.4 Relationship diagram in compact textual form

The matrix can be read in the following direction, where the arrow points from the source entity named in the matrix to the referenced entity:

```text
Employee --1:1--> Address
Employee --N:1--> Position --N:1--> Department
Employee --N:1--> Supervisor --1:1--> Employee
Schedule --1:1--> Employee
Address --N:1--> Cities
Changelog --N:1--> Employee

Leave Balance --N:1--> Leave Type
Leave Balance --N:1--> Schedule
Attendance --N:1--> Schedule
Leave --N:1--> Leave Type
Leave --N:1--> Leave Balance
Leave Accrual History --N:1--> Leave Balance
Leave Accrual History --N:1--> Leave Type
Leave Monetisation --N:1--> Leave Type
Leave Monetisation --N:1--> Leave Balance

Government --1:1--> Employee
Payroll --N:1--> Employee
Payroll --1:1--> Deductibles
Payroll --1:1--> Earnings
Payroll --N:1--> Summary
Payroll --N:1--> Months
Government Deductibles --N:1--> Months
Government Deductibles --1:1--> Government
Contributions --N:1--> Government Deductibles
Deductibles --1:1--> Government Deductibles
Payslip --1:1--> Payroll
Other Transactions --N:1--> Payroll
Government Tally --N:1--> Summary
```

This compact representation is only a readable restatement of the prose matrix. It does not add relationships to `Rest Day`, `Regions`, or any other table that is not explicitly included in that matrix.

### 13.5 Relationships present in fields or diagrams but absent from the matrix

Several links are visible elsewhere in the source set but are not listed in the relationship matrix:

- `Address.regionID` → `Regions.regionID` appears in the Data Dictionary and ERD.
- `Cities.regionID` → `Regions.regionID` appears in the Cities field definition and ERD.
- `Schedule.restDayID` → `Rest Day.restDayID` appears in the ERD; no Rest Day relationship is stated in the matrix.
- `Employee.governmentID` → `Government.governmentID` appears in the Data Dictionary, but the Database Design instead lists `Government.employeeID` → `Employee.employeeID` as the 1:1 relationship.
- `Employee.departmentID` → `Department.departmentID` appears in the Data Dictionary, but the Database Design's prose Employee table omits the field, and the relationship matrix instead links Position → Department.
- `Supervisor.departmentID` → `Department.departmentID` appears in the Data Dictionary and ERD, but is not separately identified in the relationship matrix.
- `Leave.scheduleID` → `Schedule.scheduleID` appears in the Data Dictionary, but not as a separate relationship-matrix row.
- `Leave Monetisation.leaveType` → `Leave Type.leaveType` appears in the relationship matrix and ERD, although the table-level FK summary only lists `balanceID`.

### 13.6 Foreign-key direction and cardinality cautions

The written matrix uses labels such as `Employee → Address` and `Schedule → Employee`. The FK column shown in each row belongs to the first entity named in that relationship, so the arrow is a convenient way of reproducing the matrix's wording. Do not infer that the arrow means the source entity “owns” or creates the referenced entity.

The matrix also does not always provide enough detail to distinguish optional from mandatory participation. For example, `1:1` alone does not tell the receiving agent whether a record must exist before another record can be created, whether the foreign key may be null, or whether deletion cascades. Those implementation details are not in the uploaded documents.

---

## 14. End-to-end functional traces

This section combines the schema, field definitions, relationship matrix, overview text, and DFD. The traces are helpful for understanding how records are intended to be used together. They are not substitutes for the original labels or a complete transaction specification.

### 14.1 Employee record establishment and maintenance

1. An employee is represented by a unique `employeeID` in `Employee`.
2. The Employee record links to an `Address` record through `addressID` in the Data Dictionary and Database Design relationship matrix.
3. Position and department information are represented through `Position` and `Department`. The Data Dictionary places `departmentID` directly in Employee; the Database Design prose omits it and the relationship matrix connects Position to Department.
4. A supervisor relationship is recorded through `supervisorID` in Employee and an associated Supervisor record. The relationship matrix says a Supervisor is an Employee and gives Supervisor → Employee a 1:1 cardinality.
5. A `Government` record stores TIN, SSS, PhilHealth, and Pag-ibig identifiers, along with an encrypted reference field. The relationship matrix gives Government → Employee a 1:1 association.
6. The employee's schedule is recorded in `Schedule`, linked to Employee. A schedule records start time, required daily hours, and rest-day information. The representation of rest days differs between the Data Dictionary (`SET`) and the ERD (`restDayID` pointing to Rest Day).
7. Employee details are maintained through Employee Management. The DFD shows employee address and job-profile retrieval/update flows, plus employee-information changes appended to Changelog.
8. If government PII is viewed or changed, the DFD routes it through Government Info Access and labels the operation as approval-controlled.

### 14.2 Attendance capture and timesheet validation

1. Employee attendance and time-in/time-out data enter the system.
2. The Employee Management Level 2 diagram routes the data through `1.2 Schedule and Time Tracking`.
3. The process views and appends/modifies `D2.1 Attendance` and `D2.2 Leave`.
4. The relational schema stores attendance rows in `Attendance`, each linked to a `Schedule` record using `scheduleID`.
5. The Attendance design requires `(scheduleID, date)` to be unique and requires `timeIn <= timeOut`.
6. In the payroll Level 2 diagram, `2.1 Timesheet Validation` reads Attendance Logs and derives work hours and other attendance data.
7. The same process reads Leave Logs and derives details for paid leaves.
8. The process sends `Get Time Activity` to `2.5 Basic Payroll Computation`.

The source documents do not describe a complete attendance validation algorithm. They do not state how to handle an overnight shift where `timeIn` occurs on one date and `timeOut` on the next, missing clock events, multiple clock-ins, or manual corrections beyond the fact that supervisors may adjust team attendance and the store can be appended/modified.

### 14.3 Leave request, balance, and accrual trace

1. Leave categories are defined in `Leave Type`, with a leave-type identifier, mandatory flag, default accrual rate, accrual frequency, and maximum accrual.
2. `Leave Balance` stores the current balance for a leave type and schedule.
3. A leave request is recorded in `Leave`, linking the leave balance and leave type. The Data Dictionary also includes `scheduleID` as a foreign key.
4. The leave record includes the request start date, duration in hours, reason, status, credits to consume, remaining credits, request time, approval time, and approver identifier.
5. The `dateStart` constraint requires a non-null date on or after the current date.
6. The request may have an approval timestamp that remains null until approval; the Data Dictionary explicitly allows `dateApproved` to be null.
7. Supervisors handle leave requests in the Employee Management DFD. Leave and attendance stores are appendable/modifiable and viewable.
8. `Leave Accrual History` stores the accrual date, credit accrued, old balance, and new balance. The accrual date must not be later than the current date, and all credit/balance figures must be non-negative.
9. The leave balance is associated with the employee schedule and leave category, and the Database Design adds a unique combination of `(leaveType, scheduleID)`.

The source does not enumerate the actual leave categories, define enum members, identify the statutory accrual rules for each category, or specify the order of leave approval and credit reservation. It stores the fields required for those operations without providing a full business-rule specification.

### 14.4 Leave monetisation trace

1. A supervisor handles the approved leave-monetisation request.
2. Process `2.2 Leave Monetisation Process` records the event by appending to `D2.4 Leave Monetisation Logs`.
3. The process sends `Add to Benefits` to `2.3 Wage and Adjustments`.
4. The relational table `Leave Monetisation` stores leave type, balance ID, credits monetised, monetary value per credit, total converted, date monetised, and approver ID.
5. Wage and Adjustments updates wage information (`D3.1 Wage`) and appends adjustments to `D3.2 Benefits & Deductibles`.
6. The DFD shows the leave-monetisation log as part of the later `1YO` archival group.

The data fields support recording a conversion, but the PDFs do not provide the formula for `totalConverted`, the source of `monetaryValuePer`, eligible leave types, maximum monetisable credits, or the treatment of partial credits. No formula should be invented.

### 14.5 Wage, benefit, and adjustment trace

1. The Employee and organisational records establish which employee receives the payroll calculation.
2. Process `2.3 Wage and Adjustments` receives `Send Team Member Wage and Adjustments` from Supervisor.
3. It also receives monetised leave value from Process `2.2` using the `Add to Benefits` flow.
4. It updates `D3.1 Wage` through the flow labelled `Update Wage (opt*)`.
5. It appends adjustments to `D3.2 Benefits & Deductibles`.
6. The `Earnings` table stores overtime pay, PTO pay, holiday pay, rest-day pay, rice subsidy, phone stipend, clothes stipend, other incentives, and total benefits.
7. The `Payroll` record links to the wage record through `wageID`.

The source uses both `Wage` and `Earnings` as naming conventions: `D3.1 Wage` is the DFD store name, while the relational table is called `Earnings` and holds `wageID`. The relationship matrix states Payroll → Earnings is 1:1.

### 14.6 Government deduction and contribution trace

1. The employee's Government record holds the identifiers used for government-related payroll processing.
2. `D3.3 Tax Table & Rates` stores the tax bracket and contribution-rate reference information.
3. System Admin has an `Update Rates (opt*)` flow in the Payroll Level 2 DFD.
4. Process `2.4 Government Deductibles` utilises rates to compute contributions.
5. It appends computed information to `D3.2 Benefits & Deductibles` through `Append to Deductibles`.
6. The relational `Government Deductibles` table holds employer contribution, employee contribution, and withholding tax by government record and month.
7. The `Contributions` table stores specific PhilHealth, Pag-ibig, and SSS amounts with a contributor identifier.
8. `Deductibles` stores the government link and total deductions.
9. `Government Tally` holds overall contribution totals by contributor and is linked to `Summary`.
10. The monthly summary compilation process collates payroll and contribution information and generates a Monthly Summary Report.

The table names and relationships distinguish individual government identifiers, monthly deductible amounts, contributor-specific contribution values, and monthly tally totals. The documents do not give the exact reconciliation formula between Government Deductibles, Contributions, Deductibles, Government Tally, and Summary, so the precise arithmetic relationship must not be assumed.

### 14.7 Basic payroll computation and payslip generation

1. Process `2.5 Basic Payroll Computation` receives Time Activity from Timesheet Validation.
2. It retrieves basic remunerative values from `D3.1 Wage`.
3. It applies adjustments from `D3.2 Benefits & Deductibles`.
4. It computes `Net Pay and Constituents` and sends them to `2.6 Payslip Generation & Distribution`.
5. Payroll Staff can `View, Validate, and Verify Payroll Details` at the computation stage.
6. Process `2.6` appends the payslip to `D3.4 Payslip`.
7. Employee can view personal payslip information, Supervisor can view team information, and HR can view payslip/report information.
8. The `Payslip` relational table records pay-period start and end dates, standard hours, overtime hours, holiday overtime hours, PTO hours, undertime, days worked, holidays worked, rest days worked, and gross income.
9. `Payroll.netPay` records final pay after deductions; the Database Design says `hourlyRate`, `dailyRate`, and `netPay` must be non-null.

The database overview describes semi-monthly payslips. The source does not specify whether the system runs twice per month on fixed calendar dates, on a particular cut-off schedule, or through any other schedule. It also does not provide the gross-pay or net-pay formula.

### 14.8 Monthly summary and reporting trace

1. Process `2.7 Monthly Summary Compilation` receives information to collate from Government Deductibles and the Payslip store.
2. It collates payroll/payslip and government-related totals.
3. It generates `D3.5 Monthly Summary Report`.
4. Payroll Staff views, validates, and verifies the monthly report.
5. The `Summary` table holds total gross income, total benefits, employee disbursements, employer disbursements, combined Pag-ibig, PhilHealth, and SSS totals, total tax withheld, and total government disbursement.
6. `Government Tally` stores overall government contribution totals by contributor and references Summary.

The database overview describes one monthly summary per payroll processing cycle. The data model has `monthID` but the provided fields do not clearly define a year-plus-month key for Summary. The Rates table contains an `implementationYear` in the Database Design source, but no equivalent year field is defined for Summary in the Data Dictionary.

### 14.9 Archival and deletion trace

1. System Admin triggers Process `3.1 Appending` using an `Annual Prompt`.
2. The process compiles `1YO` records from Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs.
3. The process compiles `2YO` records from Payslip.
4. It stores old logs and payroll in `D4 Cold Storage` for later compression.
5. A `Trigger on Finish` connector links the archival stages.
6. Process `3.2 Deleting` removes relevant records from the operational stores.

This creates a visible distinction between operational records and older data transferred to cold storage. It is important not to turn the `1YO` and `2YO` labels into a more specific policy than the source supplies. The diagram does not define a retention schedule in a formal policy sense, does not say whether the labels mean “records older than one/two years”, and does not describe legal holds, backup validation, restoration testing, or the effect of a failed archive operation.

---

## 15. Constraints, access controls, retention, and operational rules

This section consolidates the rules that are scattered across field tables, table-level constraints, overview prose, and diagram labels.

### 15.1 Identifier and key rules

- `employeeID` is the unique Employee identifier and PK.
- `addressID`, `departmentID`, `positionID`, `supervisorID`, `scheduleID`, `balanceID`, and the other entity-specific IDs are used to identify their corresponding records as described in their table definitions.
- `Government Deductibles` uses `(governmentID, monthID)` as the composite PK in the Database Design submission and ERD.
- `Contributions` uses `(governmentID, contributor)` as the composite PK.
- `Government Tally` uses `(summaryID, contributor)` as the composite PK in the Database Design submission and ERD, while the Data Dictionary marks `summaryID` as PK “(assumed)”.
- `Leave Balance` has a unique combination of `(leaveType, scheduleID)` in the Database Design submission.
- `Attendance` has a unique combination of `(scheduleID, date)`.
- `Payslip` has a unique combination of `(payrollID, periodEndDate)`.
- `Holiday` uses a combined `(holidayName, holidayDate)` uniqueness rule in the Database Design prose, whereas the Data Dictionary marks both columns individually unique.
- `City.cityName` is unique in both the Data Dictionary and Database Design prose.
- The Database Design submission says a Payroll record is unique per employee per period, but its prose constraint does not expressly state the fields constituting that composite uniqueness rule.

### 15.2 Date and time rules

The source gives the following date/time checks:

- Employee `birthdate` must not be null and must be earlier than `CURRENT_DATE`.
- Schedule `startTime` must be present.
- `requiredDailyHours` must be between 1 and 24, inclusive, and must be present.
- Schedule rest-day information must be present; whether it is a `SET` or a `restDayID` is inconsistent between sources.
- Attendance `timeIn` must be less than or equal to `timeOut`.
- Leave `dateStart` must be present and greater than or equal to `CURRENT_DATE`.
- Leave `dateRequested` defaults to `CURRENT_TIMESTAMP` in the Data Dictionary.
- Leave `dateApproved` is allowed to be null until approval.
- Changelog `createdAt` and `updatedAt` must be present and default to `CURRENT_TIMESTAMP`.
- Leave Accrual History `accrualDate` must be present and less than or equal to `CURRENT_DATE`.
- Payslip `periodStartDate` must be present. The source also defines a unique `(payrollID, periodEndDate)` pair, but does not state an ordering check requiring `periodEndDate >= periodStartDate`.
- `dateMonetised` is stored as a Date, but no temporal check is stated for it.
- Holiday dates are stored as Date values, without a separate check constraint.

### 15.3 Non-negative numeric rules

The source defines non-negative checks for several financial and balance values. The principal groups are:

- Leave Balance: `currentBalance >= 0`.
- Leave: `hourDuration >= 0`.
- Leave Accrual History: `creditAccrued`, `oldBalance`, and `newBalance` are non-negative.
- Government Deductibles: contribution/deduction monetary amounts are non-negative.
- Contributions: PhilHealth, Pag-ibig, and SSS amounts are non-negative.
- Payroll: `ptoMax >= 0`; hourly rate, daily rate, monthly rate, and net pay have non-negative field constraints in the Data Dictionary, while the Database Design's explicit table-level check calls out `ptoMax >= 0` and the non-null requirements for hourly rate, daily rate, and net pay.
- Payslip: hours and days worked are non-negative; gross income has a `Min(0)` field constraint in the Data Dictionary.
- Leave Monetisation: credits, per-credit value, and total converted amount are non-negative.
- Earnings: all monetary amounts are non-negative.
- Deductibles: deduction amounts are non-negative.
- Other Transactions: transaction amount is non-negative.
- Government Tally: all contribution totals are non-negative.
- Summary: all monetary totals are non-negative.
- Months: `monthlyWorkdays` must be between 1 and 31.
- Rates: rates must be non-negative and, according to the Database Design prose, all fields must be supplied.
- Tax Table: monetary values must be non-negative; minimum compensation, maximum compensation, and tax rate must be non-null according to the Database Design constraints.
- Leave Type: `defaultAccrualRate` and `maxAccrual` are non-negative.

### 15.4 Auditability and change history

The Database Design overview says modifications should be restricted to authorised personnel and traceable to the account responsible. The `Changelog` table supports this by storing:

- the affected employee (`employeeID`);
- creation and last-update timestamps (`createdAt`, `updatedAt`);
- the account/identifier associated with the append (`appenderID`);
- the account/identifier associated with the edit (`editorID`);
- a shortened table-and-column identifier (`fieldCode`);
- a data type (`dataType`); and
- previous and updated values (`oldValue`, `newValue`).

The example `P_NP` means `Payroll.NetPay`. The DFD adds the operational use: employee-information updates are appended to Changelog, the log can be viewed, and Changelogs are later included in archival compilation.

### 15.5 Personally identifiable information access

The security design contains several distinct statements:

1. Access is role-based, with separate roles for System Admin, HR, Payroll Staff, and Employee.
2. Modifications are limited to authorised personnel.
3. Audit logs should make modifications traceable to the responsible account.
4. Access to an employee's personally identifiable information requires approval from two authorised personnel.
5. Only the corresponding contribution values are normally visible and accessible.
6. The `Government` entity is highlighted with the schema legend's `Additional Security Required` colour.
7. The DFD has a dedicated `1.3 Government Info Access` process and a `D1.3 Personally Identifiable Information` store.
8. PII viewing and updating are explicitly labelled `On Approval`.
9. `PII Access Approval` appears in the context and Level 1 diagrams; the Level 2 employee-management diagram shows approval and record-modification paths.

The documents do not specify the exact identity-verification method, the two approver roles, approval evidence format, session controls, field masking, encryption algorithm, access-request expiry, or revocation policy. Do not invent these details.

### 15.6 Data retention and archival handling

The archival Level 2 diagram contains these exact labels:

- `Trigger on Annual Prompt`;
- `Compile (1YO) Records`;
- `Compile (2YO) Records`;
- `Store Old Logs and Payroll for later compression`;
- `Trigger on Finish`; and
- `Remove Relevant Records`.

It places Attendance Logs, Leave Logs, Changelogs, and Leave Monetisation Logs in the group associated with `1YO` records. It places Payslip in the group associated with `2YO` records. System Admin triggers the annual prompt; Process 3.1 appends/archives the records into Cold Storage; Process 3.2 deletes relevant records from the operational stores.

These are diagrammed retention/archival labels, not a complete records-retention policy. The source does not define:

- what the abbreviation `YO` stands for;
- the exact date calculation for the `1YO` or `2YO` thresholds;
- whether the threshold is based on creation date, last update, pay-period date, or another date;
- whether all records or only selected records are archived;
- whether Cold Storage has a separate backup or replication layer;
- how the archive is validated before deletion;
- how archived records are retrieved or restored;
- whether legal holds or disputes suspend deletion; or
- whether the word “later” in “later compression” has a defined schedule.

### 15.7 Payroll period and report cadence

The Database Design overview states that each employee receives a **semi-monthly payslip** and that each payroll processing cycle has a **monthly summary**. The database stores period dates in Payslip and a month identifier in Payroll and Summary. However, it does not define the precise cut-off calendar, payroll processing dates, or a year-month identifier for every monthly report.

---

## 16. Cross-document discrepancies and unresolved design questions

The documents are complementary but not fully harmonised. This section is critical for any agent using the synthesis to answer design questions, revise the schema, generate SQL, or evaluate whether the diagrams match the table definitions.

| No. | Topic | Data Dictionary | Database Design prose / relationship matrix | ERD or DFD image | Why it matters |
|---:|---|---|---|---|---|
| 1 | Total table count | 29 named tables. | 30 named tables because `Rest Day` is included. | ERD visibly includes `Rest Day`. | Do not report one count without explaining which source inventory is being counted. |
| 2 | Rest-day structure | `Schedule.restDay` is a `SET`, `NOT NULL`. No `Rest Day` table is listed. | Schedule prose also lists `restDay` as `SET`, but a separate Rest Day table is included. | ERD uses `Schedule.restDayID` as FK to a `Rest Day` table containing seven weekday Booleans. | A relational implementation cannot use both representations as though they were identical without a deliberate design decision. |
| 3 | Employee columns | Includes `departmentID` and `governmentID` as FKs. | Employee prose table omits both. The relationship matrix separately lists Position → Department and Government → Employee. | Employee ERD box omits `departmentID` and `governmentID`. | Department and government references may be direct Employee FKs, derived from Position/Government, or an older version of the design. The PDFs do not resolve this. |
| 4 | Address columns | Includes `regionID` as FK. | Address prose table omits `regionID`; only `cityID` appears in the table's FK constraint summary. | Address ERD box includes `regionID` as FK; Cities also has `regionID`. | The design must decide whether region is stored directly on Address or derived through Cities. |
| 5 | Supervisor columns | Includes `departmentID` as FK. | Supervisor prose table lists only `supervisorID` and `employeeID`. | Supervisor ERD box includes `departmentID` as FK. | A schema built from only the prose table would omit a field visibly present in both the Data Dictionary and ERD. |
| 6 | Schedule fields | Includes `restDay` as `SET`. | Schedule prose also includes `restDay` as `SET`. | ERD instead includes `restDayID` as FK. | Same conflict as No. 2; the prose and ERD disagree inside the Database Design submission itself. |
| 7 | Leave columns | Includes `scheduleID` as FK. | Leave prose table and table-level FK list omit `scheduleID`. | ERD Leave entity includes `scheduleID` as FK. | The Data Dictionary and ERD agree, while the prose table omits the field. |
| 8 | Employee-government link | Employee contains `governmentID` as FK. | Government contains `employeeID` as FK; relationship matrix says Government → Employee is 1:1. | ERD Government contains `employeeID`; Employee does not visibly show `governmentID`. | Both directions could be represented in a physical schema, but doing so may duplicate the same association. The design does not specify the intended FK location. |
| 9 | Employee-department link | Employee contains `departmentID` FK. | Employee prose omits it; Position contains `departmentID`, and Position → Department is N:1. | Employee does not show departmentID; Position does. | The department may be obtained through Position, but the Data Dictionary also declares a direct Employee FK. |
| 10 | Government Deductibles key | Field table marks `governmentID` as PK and FK; `monthID` as FK. | Composite PK is `(governmentID, monthID)`. | Both `governmentID` and `monthID` are visibly marked as PK/FK. | Use the composite key if reproducing the Database Design/ERD version, while retaining that the Data Dictionary's field-level table is less explicit. |
| 11 | Government Tally key | `summaryID` is marked `Primary Key (assumed)`; `contributor` has no key declaration in its Data Dictionary field table. | Composite PK `(summaryID, contributor)`. | Both `summaryID` and `contributor` are marked as key fields; `summaryID` also FK. | Composite key is the explicit Database Design/ERD definition; “assumed” in the Data Dictionary signals uncertainty. |
| 12 | Rates table key and year | Lists five rate fields and no `implementationYear`. | Lists `implementationYear` as a field, but says the table has no PK, FK, or unique constraint. | `implementationYear` is visibly marked PK in the ERD. | The identifier for rate versions is not settled. The ERD and prose contradict each other, and the Data Dictionary lacks the year column. |
| 13 | Holiday uniqueness | `holidayName` unique and `holidayDate` unique individually. | Composite unique rule `(holidayName, holidayDate)`. | ERD does not visibly annotate the unique constraints. | These are materially different integrity rules. |
| 14 | Payroll month key | `monthID` is listed in Payroll but not marked FK. | Payroll table-level FK list omits `monthID`, but relationship matrix states Payroll → Months through `monthID`. | ERD shows monthID in Payroll, but the key marker is not as clear as its explicit FK fields. | The prose relationship says the month link exists even though the FK constraint list is incomplete. |
| 15 | Leave Monetisation leave-type link | `leaveType` is `NOT NULL`, but its field constraint does not clearly label it FK. | Table-level FK summary lists only `balanceID`. | Relationship matrix and ERD show Leave Monetisation → Leave Type. | The association is present conceptually and visually but missing from the table-level FK list. |
| 16 | Contributions relationship | `governmentID` is composite PK/FK. | Relationship matrix says Contributions → Government Deductibles N:1 via `governmentID`. | ERD visibly shows Contributions related to Government Deductibles and Government-side data. | The textual relationship target may not match the table FK declaration, which describes `governmentID` as a government record identifier. |
| 17 | `Address` to `Regions` | Address has `regionID` FK. | Relationship matrix lists Address → Cities, not Address → Regions. | ERD shows Address.regionID and Cities.regionID, with both tied into the region/city area. | The matrix is incomplete relative to the listed fields and ERD. |
| 18 | `Cities` to `Regions` | Cities has `regionID` FK. | No separate Cities → Regions row appears in the relationship matrix. | ERD visibly connects Cities and Regions. | A foreign-key field exists without an explicit relationship-matrix row. |
| 19 | `Schedule` to `Rest Day` | No such table or relationship. | A Rest Day table exists, but no explicit Rest Day relationship row is present. | ERD shows Schedule.restDayID. | The ERD has a foreign-key path that the written relationship matrix omits. |
| 20 | Static group label | Table inventory calls the group E. | Column-definition subsection calls it F. | No corresponding numbering in ERD. | Editorial inconsistency, not a data-model difference. |
| 21 | Database Design table-of-contents numbering | Not applicable. | TOC says Section 2 is Tables, 3 Relationships, 4 References, while body uses 2 Schema Diagram, 3 Tables, 4 Relationships, 5 References. | Schema diagram is on page 5. | Page/section references should follow actual body headings, not the inconsistent TOC. |
| 22 | `opt*` in payroll DFD | Not applicable. | No prose explanation. | DFD labels `Update Wage (opt*)`, `Update Values (opt*)`, and `Update Rates (opt*)`. | Marker must be retained literally until its meaning is known. |
| 23 | `1YO` / `2YO` in archival DFD | Not specified in the Data Dictionary. | No prose explanation. | Archival DFD uses `Compile (1YO) Records` and `Compile (2YO) Records`. | These labels should not be expanded into a formal retention period without confirmation. |
| 24 | DFD external business-source materials | The data dictionary does not include them. | The database design does not include their text. | DFD reference list names the Business Requirement Specification and Existing Business Processes – Payroll. | The three supplied PDFs do not contain the full source business requirements that informed the DFD. |

### 16.1 Highest-impact reconciliation questions

For actual implementation or formal correction, the following questions have the largest effect on table structure and referential integrity:

1. **Rest Day:** Should `Schedule` store a `SET` of weekdays, or should it store `restDayID` referencing a separate `Rest Day` record with seven Boolean flags?
2. **Employee–Government relationship:** Should `Government.employeeID` be the sole FK for the 1:1 association, or should `Employee.governmentID` also exist?
3. **Department placement:** Is department assignment stored directly in Employee, derived exclusively through Position, or duplicated in both places?
4. **Address–Region relationship:** Is `Address.regionID` a stored FK, or should region be derived through `Address.cityID` → `Cities.regionID`?
5. **Supervisor department:** Is `Supervisor.departmentID` required in the physical table?
6. **Leave scheduling:** Should `Leave.scheduleID` be present as in the Data Dictionary and ERD?
7. **Rates versioning:** Is `implementationYear` the primary key for Rates, and if so, why does the table constraint section say no PK?
8. **Composite keys:** Should `Government Deductibles` and `Government Tally` use the composite keys shown in the Database Design/ERD?
9. **Holiday uniqueness:** Should the uniqueness rule apply separately to name and date, or only to the combined name/date pair?
10. **Missing FK declarations:** Should `Payroll.monthID`, `LeaveMonetisation.leaveType`, and the region/rest-day links be added to the written FK list and relationship matrix?

A receiving agent should treat these as open specification issues, not as permission to choose whichever implementation seems most convenient.

---

## 17. What the documents do not specify

This section separates the system's documented elements from details that are absent in the three PDFs. It is especially important for preventing a downstream agent from filling gaps with generic payroll-system assumptions.

### 17.1 No full SQL implementation specification

The documents provide logical column definitions and constraints, but do not identify:

- the target database management system or SQL dialect;
- complete `CREATE TABLE` statements;
- exact enum members for employment status, leave type, leave status, day type, transaction type, contributor, month, accrual frequency, or changelog data type;
- cascade rules for update or delete;
- nullability for every column not explicitly constrained;
- indexes beyond keys and unique rules that are stated;
- generated/identity/auto-increment behaviour for IDs;
- default values other than those explicitly stated for certain timestamps; or
- physical storage, partitioning, replication, or backup configuration.

### 17.2 No payroll formulas

The documents identify components and process names, but do not provide complete formulas for:

- gross income;
- net pay;
- hourly, daily, or monthly salary conversion;
- ordinary overtime;
- holiday overtime;
- holiday pay;
- rest-day pay;
- paid time off compensation;
- undertime;
- benefits totals;
- deduction totals;
- withholding tax;
- SSS, PhilHealth, or Pag-ibig contributions;
- employer versus employee contribution shares;
- government disbursement totals; or
- employee/employer disbursement totals.

The schema provides fields where amounts are stored. The DFD identifies which processes use those amounts. Neither gives the arithmetic rules.

### 17.3 No actual rate schedules or tax brackets

The `Rates` and `Tax Table` tables define the places where rates and brackets are held. The PDFs do not provide actual statutory percentages, amounts, income thresholds, bracket widths, tax formulas, effective dates, or the date on which rate changes take effect.

### 17.4 No complete business rules for leave

The data model provides leave categories, accrual frequency, accrual rate, maximum accrual, leave balances, requests, approvals, and monetisation. It does not list the actual leave categories or define:

- how leave is earned within a period;
- when accrual is posted;
- whether accrual is prorated;
- how leaves are approved or rejected;
- how requests that exceed the balance are handled;
- whether a request reserves credits before approval;
- how overlapping leave requests are handled;
- which leave types can be monetised;
- how monetisation is valued; or
- how an approved leave request affects attendance data and payroll.

### 17.5 No complete security implementation

The overview identifies RBAC and a two-person approval requirement for PII, but does not provide a complete role-permission matrix, specific permission names, authentication factors, encryption method, key custody, logging retention, masking rules, access review cycle, or PII access-request workflow.

### 17.6 No complete archival policy

The DFD shows the labels `1YO`, `2YO`, an annual prompt, cold storage, later compression, and subsequent deletion. It does not define the precise age calculation, file format, compression method, archive integrity check, restore process, backup schedule, legal exceptions, or audit evidence.

### 17.7 No contents of the referenced business requirements

The DFD worksheet says it was based on the Business Requirement Specification Document for Payroll System and MotorPH Existing Business Processes – Payroll. The DFD PDF's references table lists the two documents by name, but the supplied source set does not include either complete document. The DFD may therefore reflect rules that are not fully explained in the three files. This synthesis cannot recover requirements that are only contained in those absent references.

---

## 18. Page-by-page source map

The following map allows a downstream agent to locate a fact in the originals if access is later restored.

### 18.1 Data Dictionary PDF — 28 pages

| PDF page | Main content |
|---:|---|
| 1 | MO-IT107 Week 7, Logical Schema Design, Worksheet 4 metadata and explanation of a data dictionary. |
| 2 | Instructions and start of the Data Dictionary table inventory; Employee Details group begins. |
| 3 | Table inventory continuation: Employee Details and Logging group. |
| 4 | Table inventory continuation: Government Details and Payroll group. |
| 5 | Table inventory continuation: Payroll and Static-Update Only group; start of column definitions. |
| 6 | Employee table fields. |
| 7 | Employee continuation, Address, and Department fields. |
| 8 | Position, Supervisor, and Schedule fields. |
| 9 | Schedule continuation, Leave Balance, and Logging heading. |
| 10 | Changelog fields. |
| 11 | Attendance and Leave fields. |
| 12 | Leave continuation and Leave Accrual History fields. |
| 13 | Government fields and Government Deductibles fields. |
| 14 | Government Deductibles continuation and Contributions fields. |
| 15 | Contributions continuation and Payroll fields. |
| 16 | Payroll continuation, Payslip fields, and start of Leave Monetisation. |
| 17 | Payslip continuation, Leave Monetisation continuation, and Earnings fields. |
| 18 | Earnings continuation, Deductibles, Other Transactions, and Government Tally fields. |
| 19 | Government Tally continuation and Summary fields. |
| 20 | Summary continuation and start of Static-Update-Only fields; Regions and Cities. |
| 21 | Holiday and Months fields. |
| 22 | Rates fields. |
| 23 | Tax Table and Leave Type fields. |
| 24 | Relationships matrix: Employee, Address, Position, Supervisor, Schedule, and Changelog relationships. |
| 25 | Relationships matrix continuation: Leave Balance, Attendance, Leave, Leave Accrual History, Leave Monetisation, Government, and Payroll links. |
| 26 | Relationships matrix continuation: payroll, government deductibles, contributions, deductions, payslips, other transactions, Government Tally; start of Definition of Terms. |
| 27 | Definition of Terms: TIN, SSS, PhilHealth, PagIbig, PTO, PI, fieldCode, Varchar, Varbinary, Enum, TinyInt. |
| 28 | Definition of Terms continuation: SmallInt, Precision, Scale, accrual, monetisation, bracketWidth, contributor. |

### 18.2 Database Design PDF — 25 pages

| PDF page | Main content |
|---:|---|
| 1 | Title page, author, programme, and term. |
| 2 | Table of contents. |
| 3 | Database overview and the first four system functions. |
| 4 | Security and Access Control continuation. |
| 5 | Full-page Schema Diagram. |
| 6 | Employee and Address tables and constraints. |
| 7 | Department and Position tables and constraints. |
| 8 | Supervisor and Schedule tables and constraints. |
| 9 | Rest Day and Leave Balance tables and constraints. |
| 10 | Changelog and Attendance tables and constraints. |
| 11 | Leave fields and constraints. |
| 12 | Leave Accrual History fields and constraints. |
| 13 | Government and Government Deductibles fields and constraints. |
| 14 | Contributions and the start of Payroll. |
| 15 | Payroll continuation and Payslip. |
| 16 | Leave Monetisation. |
| 17 | Earnings and Deductibles. |
| 18 | Other Transactions and Government Tally. |
| 19 | Summary continuation. |
| 20 | Summary continuation, Regions, and Cities. |
| 21 | Holiday, Months, and Rates. |
| 22 | Tax Table and Leave Type. |
| 23 | Relationship section heading and the first page of the relationship matrix. |
| 24 | Continuation of the relationship matrix. |
| 25 | References: Entity-Relationship Diagram, Data Dictionary, and Schema Diagram. |

### 18.3 DFD PDF — 6 pages

| PDF page | Main content |
|---:|---|
| 1 | MO-IT107 Week 11, Data Flow Diagrams, Worksheet 6 metadata, instructions, Gene & Sarson notation requirement, and Level 2 requirement. |
| 2 | Context-level (`LV0`) diagram showing Employee, Supervisor, HR, Payroll Staff, System Admin, and process `0.0 MotorPH Payroll System`. |
| 3 | Level 1 diagram showing `1.0 Employee Management`, `2.0 Payroll Processing`, `3.0 Archival`, and stores `D1` through `D4`. |
| 4 | Employee Management Level 2 diagram: processes `1.1`, `1.2`, `1.3`; stores `D1.1`–`D1.3` and `D2.1`–`D2.3`. |
| 5 | Payroll Processing Level 2 diagram: processes `2.1`–`2.7` and stores `D2.1`, `D2.2`, `D2.4`, `D3.1`–`D3.5`. |
| 6 | Archival Level 2 diagram: processes `3.1 Appending` and `3.2 Deleting`, stores `D2.1`–`D2.4`, `D3.4`, and `D4 Cold Storage`; references table. |

---

## 19. Working instructions for a downstream agent

The following instructions are included so the synthesis can be used as the receiving agent's primary context file.

### 19.1 Treat this synthesis as a source digest, not as an updated design

Use this file to understand what the three uploaded PDFs contain. Do not interpret it as confirmation that all table definitions are mutually consistent or implementation-ready. It is a comprehensive digest of the submitted material, including its disagreements.

### 19.2 Preserve source labels

When discussing the design, retain the original names unless the user explicitly asks for renaming:

- `Employee Management`, `Payroll Processing`, `Archival`;
- `Employee Information Handling`, `Schedule and Time Tracking`, `Government Info Access`;
- `Timesheet Validation`, `Leave Monetisation Process`, `Wage and Adjustments`, `Government Deductibles`, `Basic Payroll Computation`, `Payslip Generation & Distribution`, `Monthly Summary Compilation`;
- `Appending`, `Deleting`;
- `Employee Repository`, `Logs`, `Payroll`, `Cold Storage`; and
- all `D1`–`D4` and `D1.1`–`D3.5` identifiers.

Do not rename `Earnings` to `Wage` or `Government Tally` to `Contribution Summary` merely because an alternative label may appear more intuitive. `Wage` is the DFD store name, while `Earnings` is the relational table name.

### 19.3 Do not invent unspecified values or formulas

No exact payroll formula, statutory percentage, tax bracket, enum member, encryption method, or archival age rule is included. When asked about these, state that the supplied documents do not specify them. Ask for the referenced Business Requirement Specification or Existing Business Processes document if its contents are necessary to answer.

### 19.4 Keep document variants separate when answering schema questions

When a user asks “what does the Data Dictionary say?”, answer from the Data Dictionary version. When asked “what does the ERD show?”, answer from the visual ERD transcription. When asked “what does the Database Design table definition say?”, answer from that prose definition. If the question asks for the combined design, disclose the relevant conflict and show both variants.

### 19.5 Do not silently resolve the key discrepancies

In particular, do not silently decide:

- `Schedule.restDay SET` versus `Schedule.restDayID` plus `Rest Day`;
- Employee direct `departmentID` and `governmentID` versus links through Position and Government;
- Address direct `regionID` versus region derived through City;
- `Supervisor.departmentID` presence or absence;
- `Leave.scheduleID` presence or absence;
- `Rates.implementationYear` as a field and primary key versus no primary key; or
- the composite key for `Government Tally` versus the Data Dictionary's assumed `summaryID` key.

If a task requires producing SQL or a corrected ERD, identify these as decisions that must be made. Do not claim that the PDFs have already chosen one unambiguously.

### 19.6 Distinguish schema structure from process flows

The database schema describes where records and values are stored and how tables are related. The DFD describes how actors and processes move or update those records. A DFD store such as `D2 Logs` can encompass several relational tables, and a process such as `2.5 Basic Payroll Computation` can consume several stores. Do not assume a DFD data store is exactly one database table unless a source statement confirms it.

### 19.7 Distinguish stated security intent from implemented security detail

The documents state RBAC, authorisation, auditability, and two-authorised-person PII approval. They do not give a complete access-control matrix or implementation mechanism. Do not infer which exact roles can perform every operation unless the diagrams name the flow or the user supplies the missing policy.

### 19.8 Preserve the archival labels literally

The DFD uses `1YO`, `2YO`, `Annual Prompt`, `later compression`, and `Remove Relevant Records`. Do not expand `YO`, derive an exact number of days, or claim that the archive/deletion process is fully specified.

### 19.9 Suggested source-grounded response pattern

For a detailed question, answer in this order:

1. state the source document and page or diagram level;
2. quote or reproduce the relevant table, field, process, flow, or cardinality;
3. compare another document only if it addresses the same element;
4. identify any mismatch plainly; and
5. distinguish source fact from interpretation or unresolved implementation choice.

### 19.10 Source documents

The synthesis is based only on these three supplied PDFs:

1. `MO-IT107 Homework_ Data Dictionary for MotorPH Payroll System Database H2101 Torres, J.D..pdf` — 28 pages.
2. `MO-IT107 - 2nd Milestone_ Database Design for the MotorPH Payroll System H2101 TORRES, J.D..pdf` — 25 pages.
3. `MO-IT107 Homework_ Data Flow Diagram of MotorPH Payroll System H2101 Torres, J.D..pdf` — 6 pages.

The Data Flow Diagram also names two external references, but those full documents were not among the three supplied PDFs:

- `Business Requirement Specification Document for Payroll System`.
- `MotorPH Existing Business Processes - Payroll`.

---

## Final scope statement

This synthesis covers the three supplied PDFs' system overview, all table inventories, all column definitions visible in the Data Dictionary, the Database Design table definitions and table-level constraints, the schema diagram's visual content, the stated relationships and cardinalities, all visible DFD actors/processes/data stores/flows, and the archival sequence. It also records the important omissions and contradictions that a downstream agent would otherwise be unable to see from a text-only excerpt.

It does **not** claim to contain the full contents of the two external business-requirement documents named in the DFD references, nor does it add payroll formulas, statutory rate values, SQL implementation decisions, or business rules absent from the supplied files.

### 13.7 Data Dictionary relationship matrix (separate source version)

**Source location:** Data Dictionary PDF, pages 24–26. This is a second relationship matrix, separate from the Database Design relationship matrix reproduced in Sections 13.1–13.3. It contains a broader set of declared FK links and differs on certain cardinalities.

The Data Dictionary matrix has these columns: `Foreign Key Table`, `Foreign Key Column`, `Referenced Table`, `Referenced Primary Key`, and `Description of Relationship`.

| Foreign-key table | FK column | Referenced table / PK | Cardinality stated in Data Dictionary | Description stated |
|---|---|---|---:|---|
| Employee | `addressID` | Address.`addressID` | 1:1 | Each employee is assigned an address. |
| Employee | `departmentID` | Department.`departmentID` | N:1 | Employees are categorised by their department. |
| Employee | `positionID` | Position.`positionID` | N:1 | The employee holds a specific position. |
| Employee | `governmentID` | Government.`governmentID` | 1:1 | Government records are uniquely linked to each employee. |
| Employee | `supervisorID` | Supervisor.`supervisorID` | N:1 | The system references the supervisor of the employee. |
| Address | `cityID` | Cities.`cityID` | 1:1 | Each address entry is located within a specific city. |
| Address | `regionID` | Regions.`regionID` | 1:1 | Addresses are also linked to a region. |
| Position | `departmentID` | Department.`departmentID` | N:1 | Positions are assigned within departments. |
| Supervisor | `departmentID` | Department.`departmentID` | N:1 | Departments have at least one supervisor. |
| Supervisor | `employeeID` | Employee.`employeeID` | 1:1 | Supervisors are employees themselves, connecting both tables directly. |
| Schedule | `employeeID` | Employee.`employeeID` | 1:1 | Each employee is assigned a schedule. |
| Changelog | `employeeID` | Employee.`employeeID` | N:1 | Tracks changes made by the employee. |
| Leave Balance | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type applicable for an employee leave balance. |
| Leave Balance | `scheduleID` | Schedule.`scheduleID` | N:1 | Leave balances are determined based on an employee's schedule. |
| Attendance | `scheduleID` | Schedule.`scheduleID` | N:1 | Attendance records are linked to the schedule. |
| Leave | `scheduleID` | Schedule.`scheduleID` | N:1 | Leave requester identifier. |
| Leave | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type used for the request. |
| Leave | `balanceID` | Leave Balance.`balanceID` | N:1 | Leave requests are validated against available leave balances. |
| Leave Accrual History | `balanceID` | Leave Balance.`balanceID` | N:1 | Accrual records adjust the leave balance over time. |
| Leave Accrual History | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type that has been accrued. |
| Leave Monetisation | `leaveType` | Leave Type.`leaveType` | N:1 | Indicates the leave type requested for conversion. |
| Leave Monetisation | `balanceID` | Leave Balance.`balanceID` | N:1 | Converts leave credits into monetary value based on the leave balance. |
| Government | `employeeID` | Employee.`employeeID` | 1:1 | Links each government record with an employee. |
| Payroll | `employeeID` | Employee.`employeeID` | N:1 | Links payroll records to employees. |
| Payroll | `rebateID` | Deductibles.`rebateID` | 1:1 | Deduction computations of payroll. |
| Payroll | `wageID` | Earnings.`wageID` | 1:1 | Benefit computations of payroll. |
| Payroll | `summaryID` | Summary.`summaryID` | N:1 | Collates payroll data into a monthly summary record. |
| Payroll | `monthID` | Months.`monthID` | 1:1 | References payroll month and its corresponding working days. |
| Government Deductibles | `monthID` | Months.`monthID` | N:1 | References the month these deductibles are for. |
| Government Deductibles | `governmentID` | Government.`governmentID` | 1:1 | Associates deductibles with a government record. |
| Contributions | `governmentID` | Government Deductibles.`governmentID` | N:1 | Reference location where computed contribution will be collated. |
| Deductibles | `governmentID` | Government Deductibles.`governmentID` | 1:1 | Needs government deductibles to compute total deductibles. |
| Payslip | `payrollID` | Payroll.`payrollID` | 1:1 | Each payslip is uniquely tied to a payroll record. |
| Other Transactions | `payrollID` | Payroll.`payrollID` | N:1 | Connects miscellaneous transactions to payroll records. |
| Government Tally | `summaryID` | Summary.`summaryID` | N:1 | Aggregates government-related disbursements within a summary. |

**Important Data Dictionary-specific readings:**

- Employee has direct links to Address, Department, Position, Government, and Supervisor.
- Address has separate links to Cities and Regions.
- Supervisor has a Department link as well as its Employee link.
- Leave has a Schedule link, a Leave Type link, and a Leave Balance link.
- Payroll → Months is recorded as **1:1** in this matrix.
- Contributions is shown as referencing Government Deductibles by `governmentID` in this matrix, despite the field definition describing `Contributions.governmentID` as a linked government record identifier and the schema diagram showing Government-side links.

### 13.8 Comparing the two relationship matrices

The Data Dictionary matrix and Database Design matrix should be treated as two distinct source versions:

| Relationship topic | Data Dictionary matrix (PDF pp. 24–26) | Database Design matrix (PDF pp. 24–25) |
|---|---|---|
| Employee → Department | Explicit Employee.`departmentID` → Department, N:1. | Not listed as a direct Employee relationship; Position → Department is listed. |
| Employee → Government | Explicit Employee.`governmentID` → Government, 1:1. | Government → Employee is listed using Government.`employeeID`, 1:1. |
| Address → Regions | Explicit Address.`regionID` → Regions, 1:1. | Not listed. |
| Supervisor → Department | Explicit Supervisor.`departmentID` → Department, N:1; purpose says each department has at least one supervisor. | Not listed as a separate relationship. |
| Leave → Schedule | Explicit Leave.`scheduleID` → Schedule, N:1; described as Leave requester identifier. | Not listed. |
| Payroll → Months | N/A in the Database Design field table, but the relationship matrix itself lists N:1. | N:1. |
| Contributions → Government Deductibles | Listed as N:1. | Also listed as N:1. |
| Address → Cities | 1:1. | N:1. |

The Address → Cities and Payroll → Months cardinalities differ between the two matrices. Do not treat these as harmless wording variations: the stated cardinalities have direct implications for the intended relationship model.

The Data Dictionary matrix also includes the relationship `Address.regionID` → Regions and `Supervisor.departmentID` → Department. Those links are not in the Database Design matrix, even though the ERD shows the relevant columns. The database-design relationship matrix is therefore shorter than the set of links represented across the other source materials.

---

## 20. Data Dictionary glossary and acronym definitions

**Source location:** Data Dictionary PDF, pages 26–28, Section 3, `Definition of Terms`.

The Data Dictionary explicitly includes a glossary, which is part of the source content and should be retained by any agent using this synthesis.

| Term | Definition given in the Data Dictionary |
|---|---|
| `TIN` | Taxpayer Identification Number — a unique number assigned to individuals or entities for tax purposes. |
| `SSS` | Social Security System Number — an identifier used to track social security contributions and benefits. |
| `PhilHealth` | Identifier for the Philippine Health Insurance Corporation, used to manage health-insurance records and contributions. |
| `PagIbig` | Identifier for the Pag-IBIG Fund (Home Development Mutual Fund), used for housing savings and related benefits. |
| `PTO` | Paid Time Off — the policy allowing employees to take leave while still receiving their regular pay. |
| `PI` | The Data Dictionary says, “No, not Archimedes' constant.” It is the abbreviation used in rate fields (`piEmployerRate`, `piEmployeeRate`, `piMaxContribution`) and refers to Pag-IBIG-related contribution rates. |
| `fieldCode` | A shortened identifier used in logging to represent specific table and column names concisely, e.g. `P_NP` for `Payroll.NetPay`. |
| `Varchar` | Variable Character — a data type for storing text strings of varying lengths. |
| `Varbinary` | Variable Binary — a data type for storing binary data that does not have a fixed length. |
| `Enum` | Enumeration — a data type that defines a set of predefined values. |
| `TinyInt` | Tiny Integer — a data type representing a small-range integer, with values that typically do not reach beyond 127. |
| `SmallInt` | Small Integer — a numeric data type used to store integers within a limited range, with values that typically do not reach beyond 32,767. |
| `Precision` | The total number of digits allowed in a decimal number, both to the left and right of the decimal point. |
| `Scale` | The number of digits allowed to the right of the decimal point in a decimal number. |
| `accrual` | The process by which leave credits are accumulated over time, typically based on a defined rate or frequency. |
| `monetisation` | The conversion of leave credits into monetary value, allowing employees to receive compensation for unused leave. |
| `bracketWidth` | Represents the income range covered by a tax bracket, defining the span between the minimum and maximum compensation for that bracket. |
| `contributor` | Identifier for the party, such as an employee or employer, contributing to a benefit or deduction in payroll and related calculations. |

**Glossary note:** The glossary describes `TinyInt` and `SmallInt` using typical upper-range descriptions, but the PDFs do not specify the actual database engine. The wording should be retained as the document's explanation, not treated as a complete SQL-dialect declaration.

---


## Full source document: `reference-materials/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md`

# MotorPH Payroll System — Complete Feature Inventory and Pseudocode

**Repository:** [J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3)  
**Branch inspected:** `main`  
**Course context:** MO-IT110 Object-Oriented Programming, Term 1, SY 2026–2027, Section S2101, Team 3  
**Purpose of this document:** A source-oriented inventory of the application's major features, minor features, their constituent sub-functions, and detailed pseudocode for the workflows represented in the repository.

---

## Reading this document

This document separates three related but different things:

1. **The application as a whole**, including the inherited Swing desktop interface and its CSV-backed functions.
2. **The newly implemented OOP backend**, which owns employee validation, employee types, attendance records, statutory deduction calculations, and payroll calculation.
3. **The intended OOP design**, including several planned classes that do not yet exist in the backend implementation.

The repository has two source roots:

| Source root | Responsibility | Typical contents |
|---|---|---|
| `src/motorphemployee/` | Existing Java Swing application, retained and modified from the CP2 project | Loading screen, landing page, login screens, HR main menu, employee dashboard, forms, leave screens, CSV services |
| `oop/src/motorphemployee/oop/` | New OOP backend | Person and employee classes, employee subclasses, contribution calculators, payroll calculator, payroll result, attendance log, console test harness |

The repository's README says that `MainMenu` and `Dashboard` now call the OOP `PayrollCalculator`, that the payroll calculation methods were removed from those two large GUI classes, and that the application reads actual rows from `attendance.csv`. It also documents 12 implemented OOP classes out of 16 classes in the approved OOP design. The remaining four design classes are `Credentials`, `LeaveRequest`, `LeaveManagement`, and `HRManager`. This does **not** mean that the Swing leave screens are absent: `LeaveManagement` and `LeaveApproval` GUI classes exist in the original application, while the corresponding OOP domain/service design classes are a separate matter.

### Status labels used below

- **Implemented in GUI:** The operation exists in the Swing application.
- **Implemented in OOP backend:** The operation is represented by a class or service in the new backend.
- **Inherited CP2:** The feature comes from the earlier procedural application and remains part of the refactored product.
- **Partial / limited:** A screen or code path exists, but behaviour is constrained or incomplete.
- **Design-only:** Present in the approved class design or described as intended, but not implemented as a backend class in this repository version.
- **Known issue:** The repository documents a defect, security gap, or limitation relevant to the function.

### Pseudocode conventions

The pseudocode describes the behaviour visible in the repository, not a proposed replacement implementation. `READ`, `WRITE`, `SHOW`, `RETURN`, `THROW`, and `CALL` are descriptive operations. A `TRY / CATCH` block represents the application's error-handling path. A process marked **known issue** deliberately preserves the current behaviour rather than silently correcting it.

Where the code uses a GUI dialog, pseudocode describes the user's action and the corresponding event handler. It does not attempt to reproduce Swing layout code line by line. Where a backend method is more detailed than the screen-level action, the backend operation receives its own pseudocode.

---

# PART I — FEATURE INVENTORY

## 1. Major feature map

The application is best understood as the following twelve major functional areas. Some are visible to employees or HR personnel; others are supporting services required for the user-facing workflows.

| No. | Major feature | Principal users / callers | Main source files or classes |
|---:|---|---|---|
| 1 | Application startup and navigation | All users | `LoadingScreen`, `LandingPage`, `Login`, `LoginHR` |
| 2 | Authentication and session transitions | Employees, HR | `Login`, `LoginHR`, `Dashboard`, `MainMenu` |
| 3 | HR employee-record administration | HR personnel | `MainMenu`, `Form` |
| 4 | Employee self-service dashboard | Employees | `Dashboard`, `ChangeCredentials` |
| 5 | Attendance and timecard intake | Payroll screens, payroll backend | `AttendanceRepository`, `AttendanceLog` |
| 6 | Employee identity and employment classification | Employee construction, payroll | `Person`, `Employee`, `RegularEmployee`, `ProbationaryEmployee` |
| 7 | Payroll computation and result presentation | HR, employees | `PayrollCalculator`, `PayrollRecord`, `MainMenu`, `Dashboard` |
| 8 | Statutory contribution and tax calculation | Payroll backend | `GovernmentContributionCalculator`, `SSSCalculator`, `PhilHealthCalculator`, `PagIbigCalculator`, `WithholdingTaxCalculator` |
| 9 | Leave application | Employees | `LeaveForm` |
| 10 | Leave review, approval, rejection, and recording | HR personnel | `LeaveManagement`, `LeaveApproval`, `LeaveService` |
| 11 | Account credential maintenance | Employees | `ChangeCredentials` |
| 12 | File persistence, validation, and quality assurance | Application and maintainers | CSV files, `LeaveService`, `AttendanceRepository`, `ConsoleBackendTest`, test/evidence outputs |

The OOP employee classes and statutory calculators are also cross-cutting components. They appear as independent major areas here because their own methods and rules are substantial enough to merit separate inventories and pseudocode.

## 2. Feature status at a glance

| Component | Present behaviour | Status / limitation |
|---|---|---|
| Employee login | Checks employee number, username, and password against `data.csv` | Implemented in GUI; plaintext password comparison |
| HR login | Checks username and password against `HRLogin.csv` | Implemented in GUI; plaintext password comparison; invalid-login handler closes the login window |
| Employee dashboard | Loads the employee record and shows personal, employment, government, salary, and leave information | Implemented in GUI |
| HR main menu | Lists employee records and exposes add, update, delete, save, refresh, view, and payroll controls | Implemented in GUI |
| Add employee | Validates required and numeric fields, checks duplicate employee number, appends a 24-column row | Implemented in GUI; defaults are inserted for username, password, and leave counters |
| Update employee | Copies selected form values into the selected table row | Implemented in GUI; the change remains in memory until Save |
| Delete employee | Confirms and removes a selected row from the table model | Implemented in GUI; the change remains in memory until Save |
| CSV save | Rewrites `data.csv` from the table model | Implemented; comma/quote escaping is not fully handled |
| Employee payroll view | Reads available attendance months, calculates the selected month's payroll, and displays the breakdown | Implemented through OOP backend |
| HR payroll calculation | Selects an employee and a month with attendance, calculates, and displays the breakdown | Implemented through OOP backend |
| Attendance import | Reads and validates `attendance.csv`, then maps records by employee and month | Implemented in `AttendanceRepository` |
| Clock-in / clock-out model | Stores date/time entries and validates chronological order | Implemented in `AttendanceLog`; imported timecards are the payroll source used by the GUI |
| SSS, PhilHealth, Pag-IBIG, and withholding tax | Each calculator implements a separate calculation rule | Implemented in OOP backend |
| Leave submission | Appends a pending leave row | Implemented in GUI; the form writes directly, bypassing the service-level transaction |
| Leave approval / rejection | Removes a pending request, records its decision, and updates employee counters | Implemented by `LeaveService`, used by the approval GUI |
| Leave credit deduction | Decreases the employee's leave-credit counter by one for an approved request | Implemented current rule; it is one credit per request, not one credit per day |
| Change employee credentials | Rewrites username and password columns in `data.csv` | Implemented in GUI; plaintext storage, no password hashing |
| OOP `Credentials` class | Intended authentication and password-change abstraction | Design-only |
| OOP `LeaveRequest` class | Intended leave-request entity | Design-only |
| OOP `LeaveManagement` service/domain class | Intended OOP leave-management abstraction | Design-only; do not confuse with existing Swing dialog `LeaveManagement.java` |
| OOP `HRManager` class | Intended HR actor/domain class | Design-only |

---

# 3. Major Feature 1 — Application Startup and Navigation

## 3.1 Purpose

The startup sequence opens the application, presents the role-selection screen, and routes the user to the appropriate login screen. The documented entry sequence is:

`LoadingScreen` → `LandingPage` → either `Login` or `LoginHR`.

The landing page provides three principal actions: employee access, HR access, and application exit. Login screens provide a return route to the landing page. Successful authentication opens the corresponding dashboard; logging out returns the user to the landing page or login entry point, depending on the screen's handler.

## 3.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 1.1 Loading screen | Display startup screen; prepare the entry transition; open landing page |
| 1.2 Landing page | Display role options; open employee login; open HR login; exit application |
| 1.3 Employee access routing | Instantiate employee login; show login screen; preserve application process |
| 1.4 HR access routing | Instantiate HR login; show HR login screen; preserve application process |
| 1.5 Back navigation | Dispose the current login window; recreate or reveal landing page |
| 1.6 Logout navigation | Ask for confirmation; return to the login/landing route; close current dashboard |
| 1.7 Application exit | Invoke exit action from landing page or relevant screen |
| 1.8 Clock display | Render current date and time; refresh the clock at one-second intervals |
| 1.9 Dialog lifecycle | Open child forms as modal dialogs; return control to the owner when the dialog closes |

## 3.3 Pseudocode — Major Feature 1: Start and route the application

```text
PROCEDURE START_APPLICATION
    CREATE LoadingScreen
    DISPLAY LoadingScreen

    WHEN startup transition is complete:
        CLOSE LoadingScreen
        CREATE LandingPage
        DISPLAY LandingPage
END PROCEDURE
```

### 3.3.1 Minor feature 1.1 — Loading screen

```text
PROCEDURE SHOW_LOADING_SCREEN
    INITIALISE loading window
    DISPLAY loading window

    PERFORM the screen's startup transition

    WHEN the transition completes:
        CLOSE loading window
        CALL OPEN_LANDING_PAGE
END PROCEDURE
```

The source establishes the entry sequence, but the detailed animation, delay, or loading work should not be expanded beyond what the actual `LoadingScreen` class implements. The pseudocode therefore records the transition without inventing a data-loading routine.

### 3.3.2 Minor feature 1.2 — Landing page

```text
PROCEDURE OPEN_LANDING_PAGE
    CREATE LandingPage
    DISPLAY LandingPage

    WAIT FOR user action

    IF user selects Employees THEN
        CALL OPEN_EMPLOYEE_LOGIN
    ELSE IF user selects HR THEN
        CALL OPEN_HR_LOGIN
    ELSE IF user selects Exit THEN
        CALL EXIT_APPLICATION
    END IF
END PROCEDURE
```

### 3.3.3 Minor feature 1.3 — Open employee login

```text
PROCEDURE OPEN_EMPLOYEE_LOGIN
    CREATE Login window
    DISPLAY Login window
END PROCEDURE
```

### 3.3.4 Minor feature 1.4 — Open HR login

```text
PROCEDURE OPEN_HR_LOGIN
    CREATE LoginHR window
    DISPLAY LoginHR window
END PROCEDURE
```

### 3.3.5 Minor feature 1.5 — Back navigation

```text
PROCEDURE RETURN_TO_LANDING(currentLoginWindow)
    CLOSE currentLoginWindow
    CREATE LandingPage
    DISPLAY LandingPage
END PROCEDURE
```

### 3.3.6 Minor feature 1.6 — Logout

```text
PROCEDURE LOGOUT(currentDashboard)
    SHOW confirmation dialog: "Are you sure you want to log out?"

    IF user confirms THEN
        CLOSE currentDashboard
        CREATE Login window OR return to the landing entry route
        DISPLAY the next login-entry screen
    ELSE
        KEEP currentDashboard open
    END IF
END PROCEDURE
```

The employee and HR screens use their own navigation handlers. Do not assume that every logout handler shares one central session manager, because no such backend session manager is documented.

### 3.3.7 Minor feature 1.7 — Exit application

```text
PROCEDURE EXIT_APPLICATION
    TERMINATE the Java application process
END PROCEDURE
```

### 3.3.8 Minor feature 1.8 — Live date and time

```text
PROCEDURE START_CLOCK(dateLabel, timeLabel)
    SET clock timer interval TO 1 second

    EVERY 1 second:
        currentDate ← system date formatted as yyyy-MM-dd
        currentTime ← system time formatted as HH:mm:ss
        SET dateLabel TEXT TO currentDate
        SET timeLabel TEXT TO currentTime
END PROCEDURE
```

### 3.3.9 Minor feature 1.9 — Modal dialog lifecycle

```text
PROCEDURE OPEN_MODAL_DIALOG(ownerWindow, dialog)
    SET dialog owner TO ownerWindow
    SET dialog modality TO APPLICATION_MODAL
    SET dialog close operation TO DISPOSE_ON_CLOSE
    DISPLAY dialog

    WAIT until dialog closes
    RETURN control to ownerWindow
END PROCEDURE
```

The repository records modal behaviour for popup forms so that closing a child window does not terminate the main application. This is a UI lifecycle property, not an authentication or data-processing feature.

---

# 4. Major Feature 2 — Authentication and Session Transitions

## 4.1 Purpose

The application has separate login paths for ordinary employees and HR personnel. Employee authentication uses the employee number, username, and password stored in the employee data CSV. HR authentication uses the username and password in `HRLogin.csv`.

The implementation compares the supplied password directly against the stored plaintext value. The repository specifically records that credential hashing from the original OOP design was not migrated. The login screens therefore provide access checking, but not secure password storage.

## 4.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 2.1 Employee login form | Collect employee number; collect username; collect password; show/hide password; reject empty input |
| 2.2 Employee credential lookup | Read employee CSV; locate matching employee number; compare username; compare password |
| 2.3 Employee login result | Open employee dashboard on success; show error on failure; return to landing page |
| 2.4 HR login form | Collect HR username; collect HR password; show/hide password; reject empty input |
| 2.5 HR credential lookup | Read HR credential CSV; compare username and password |
| 2.6 HR login result | Open HR main menu on success; show error on failure |
| 2.7 Password visibility | Toggle password field between concealed and visible presentation |
| 2.8 Logout | Confirm logout; dispose current dashboard; return to login entry |
| 2.9 Credential storage limitation | Store and compare plaintext passwords; no password hashing migration |
| 2.10 HR failed-login window behaviour | Current handler disposes the HR login window after its decision path, including the failed path |

## 4.3 Pseudocode — Major Feature 2: Authenticate a user

```text
PROCEDURE AUTHENTICATE_USER(role)
    IF role = EMPLOYEE THEN
        CALL EMPLOYEE_LOGIN
    ELSE IF role = HR THEN
        CALL HR_LOGIN
    ELSE
        SHOW unsupported role error
    END IF
END PROCEDURE
```

### 4.3.1 Minor feature 2.1 — Collect employee login inputs

```text
PROCEDURE COLLECT_EMPLOYEE_LOGIN_INPUTS
    employeeNumber ← READ employee-number field
    username ← READ username field
    password ← READ password field

    IF employeeNumber is empty OR username is empty OR password is empty THEN
        SHOW validation error
        RETURN INVALID_INPUT
    END IF

    RETURN employeeNumber, username, password
END PROCEDURE
```

### 4.3.2 Minor feature 2.2 — Employee credential lookup

```text
FUNCTION VERIFY_EMPLOYEE_CREDENTIALS(employeeNumber, username, password)
    OPEN data.csv FOR reading

    FOR EACH row IN data.csv:
        PARSE row into columns

        IF row is the matching employee record THEN
            IF column[0] = employeeNumber
               AND column[19] = username
               AND column[20] = password THEN
                CLOSE data.csv
                RETURN TRUE
            END IF
        END IF
    END FOR

    CLOSE data.csv
    RETURN FALSE
END FUNCTION
```

**Current implementation details:** employee number is read from column `0`, username from column `19`, and password from column `20`. The implementation uses comma splitting rather than a fully quote-aware CSV parser. A malformed or comma-containing field can therefore misalign columns. The login code's row-length guard is also weaker than the number of columns subsequently accessed, so malformed short records can cause an indexing error rather than a clean credential rejection.

### 4.3.3 Minor feature 2.3 — Employee login result

```text
PROCEDURE EMPLOYEE_LOGIN
    result ← CALL COLLECT_EMPLOYEE_LOGIN_INPUTS

    IF result = INVALID_INPUT THEN
        RETURN
    END IF

    employeeNumber, username, password ← result
    isValid ← CALL VERIFY_EMPLOYEE_CREDENTIALS(
                  employeeNumber,
                  username,
                  password
              )

    IF isValid = TRUE THEN
        CLOSE employee login window
        CREATE Dashboard(employeeNumber)
        DISPLAY employee dashboard
    ELSE
        SHOW invalid employee login message
        KEEP login form available for another attempt
    END IF
END PROCEDURE
```

### 4.3.4 Minor feature 2.4 — Collect HR login inputs

```text
PROCEDURE COLLECT_HR_LOGIN_INPUTS
    username ← READ HR username field
    password ← READ HR password field

    IF username is empty OR password is empty THEN
        SHOW validation error
        RETURN INVALID_INPUT
    END IF

    RETURN username, password
END PROCEDURE
```

### 4.3.5 Minor feature 2.5 — HR credential lookup

```text
FUNCTION VERIFY_HR_CREDENTIALS(username, password)
    OPEN HRLogin.csv FOR reading

    SKIP header row if present

    FOR EACH credentialRow IN file:
        storedUsername ← credentialRow[0]
        storedPassword ← credentialRow[1]

        IF username = storedUsername AND password = storedPassword THEN
            CLOSE file
            RETURN TRUE
        END IF
    END FOR

    CLOSE file
    RETURN FALSE
END FUNCTION
```

### 4.3.6 Minor feature 2.6 — HR login result

```text
PROCEDURE HR_LOGIN
    result ← CALL COLLECT_HR_LOGIN_INPUTS

    IF result = INVALID_INPUT THEN
        RETURN
    END IF

    username, password ← result
    isValid ← CALL VERIFY_HR_CREDENTIALS(username, password)

    IF isValid THEN
        CLOSE HR login window
        CREATE MainMenu
        DISPLAY HR main menu
    ELSE
        SHOW invalid HR credentials message
    END IF

    NOTE: the current HR login event handler disposes the login window
          after the branch, including after a failed login.
END PROCEDURE
```

The final note reflects the current handler's unconditional disposal behaviour. It is a code-level defect, not a desired login rule.

### 4.3.7 Minor feature 2.7 — Show or hide password

```text
PROCEDURE TOGGLE_PASSWORD_VISIBILITY(passwordField, visibilityControl)
    IF visibilityControl is selected THEN
        SET passwordField echo character TO visible
    ELSE
        SET passwordField echo character TO concealed
    END IF
END PROCEDURE
```

### 4.3.8 Minor feature 2.8 — Logout from employee or HR dashboard

```text
PROCEDURE CONFIRM_AND_LOGOUT(currentWindow)
    answer ← SHOW confirmation dialog

    IF answer = CONFIRM THEN
        CLOSE currentWindow
        CREATE login-entry screen
        DISPLAY login-entry screen
    ELSE
        RETURN to currentWindow without changing its state
    END IF
END PROCEDURE
```

### 4.3.9 Minor feature 2.9 — Existing plaintext credential handling

```text
PROCEDURE CURRENT_CREDENTIAL_COMPARISON(suppliedPassword, storedPassword)
    COMPARE suppliedPassword directly with storedPassword
    RETURN comparison result

    SECURITY STATUS:
        password is not hashed by the current implementation
        no salt or password-hash verification is performed here
END PROCEDURE
```

This block documents existing behaviour; it is not a recommended password-storage method.

---

# 5. Major Feature 3 — HR Employee-Record Administration

## 5.1 Purpose

The HR main menu is the central employee-record administration screen. It reads employee rows from `data.csv`, displays a table, and provides controls for adding, viewing, updating, deleting, saving, refreshing, clearing, and calculating the payroll of an employee.

The employee table includes personal information, government identifiers, employment status, position, supervisor, salary fields, allowances, login fields, and leave counters. Several columns are hidden in the table presentation, but remain part of the stored record.

## 5.2 Employee record shape used by the GUI

The Add Employee form appends a 24-value row in this order:

| Index | Field | Meaning |
|---:|---|---|
| 0 | Employee number / ID | Employee's unique number |
| 1 | Last name | Employee surname |
| 2 | First name | Employee given name |
| 3 | Birthday | Birth date text |
| 4 | Address | Address text |
| 5 | Phone number | Contact number |
| 6 | SSS number | SSS identifier |
| 7 | PhilHealth number | PhilHealth identifier |
| 8 | TIN | Tax identifier |
| 9 | Pag-IBIG number | Pag-IBIG identifier |
| 10 | Status | Employment classification |
| 11 | Position | Job position |
| 12 | Supervisor | Supervisor identifier or value |
| 13 | Basic salary | Base salary field |
| 14 | Rice subsidy | Rice allowance field |
| 15 | Phone allowance | Phone allowance field |
| 16 | Clothing allowance | Clothing allowance field |
| 17 | Gross semi-monthly rate | Semi-monthly gross-rate field |
| 18 | Hourly rate | Hourly salary rate |
| 19 | Username | Employee login username |
| 20 | Password | Employee login password |
| 21 | Leave credits | Current leave-credit counter |
| 22 | Approved leaves | Approved leave-request counter |
| 23 | Rejected leaves | Rejected leave-request counter |

The exact data file is the source of truth for the index mapping. This table describes the 24 values written by the form, not a formally enforced database schema.

## 5.3 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 3.1 Load employee records | Locate `data.csv`; read header; read rows; populate table model; report read errors |
| 3.2 Display employee table | Show available employee rows; hide columns 3–5 and 10–23 in the configured table view; retain full model data |
| 3.3 Select employee | Detect selected row; require a valid selection; map row values to form fields |
| 3.4 View employee information | Retrieve selected record; format personal and employment data; show detail dialog |
| 3.5 Add employee form | Open modal form; collect personal, government, employment, and salary fields |
| 3.6 Validate new employee | Check required fields; parse numeric values; check duplicate employee ID |
| 3.7 Create new employee row | Map inputs to 24 columns; insert default credentials and leave counters; append row |
| 3.8 Update employee details | Require one selected row; parse numeric fields; reject duplicate ID; update table model |
| 3.9 Delete employee | Require one selected row; ask for confirmation; remove selected table row |
| 3.10 Save employee records | Read table model; write header and all rows to `data.csv`; report outcome |
| 3.11 Refresh employee list | Clear current table rows; reload file from disk |
| 3.12 Clear form | Empty employee-detail input fields |
| 3.13 Open leave administration | Instantiate the leave-management dialog; show it modally |
| 3.14 Open Add Employee | Instantiate the employee form; show it modally |
| 3.15 HR clock | Refresh the date and time labels once per second |
| 3.16 Delete/Update persistence boundary | Keep table-model edits pending until Save; recognise that Refresh can discard unsaved changes |

## 5.4 Pseudocode — Major Feature 3: HR employee administration

```text
PROCEDURE HR_EMPLOYEE_ADMINISTRATION
    CALL LOAD_EMPLOYEE_RECORDS
    DISPLAY employee table

    WHILE HR main menu is open:
        WAIT FOR HR action

        CASE action OF
            ADD:        CALL ADD_EMPLOYEE
            SELECT:     CALL POPULATE_SELECTED_EMPLOYEE
            VIEW:       CALL VIEW_EMPLOYEE_INFORMATION
            UPDATE:     CALL UPDATE_EMPLOYEE
            DELETE:     CALL DELETE_EMPLOYEE
            SAVE:       CALL SAVE_EMPLOYEE_RECORDS
            REFRESH:    CALL REFRESH_EMPLOYEE_RECORDS
            CLEAR:      CALL CLEAR_EMPLOYEE_FORM
            LEAVE:      CALL OPEN_LEAVE_MANAGEMENT
            PAYROLL:    CALL HR_CALCULATE_PAYROLL
            LOGOUT:     CALL CONFIRM_AND_LOGOUT
            EXIT:       CALL EXIT_APPLICATION
        END CASE
    END WHILE
END PROCEDURE
```

### 5.4.1 Minor feature 3.1 — Load employee records

```text
PROCEDURE LOAD_EMPLOYEE_RECORDS
    filePath ← employee data CSV path

    TRY
        OPEN filePath FOR reading
        READ first line as table header
        SET table model columns TO parsed header values

        WHILE another line exists:
            line ← READ next line
            values ← SPLIT line using comma delimiter
            ADD values as a row in employee table model
        END WHILE

        CLOSE file
        APPLY configured hidden-column presentation
    CATCH file or parsing error:
        SHOW employee-data loading error
    END TRY
END PROCEDURE
```

**Known limitation:** the current split operation is not a quote-aware CSV parser. A comma inside a quoted address or other text value can produce an incorrect column count.

### 5.4.2 Minor feature 3.2 — Display employee table

```text
PROCEDURE DISPLAY_EMPLOYEE_TABLE(tableModel)
    ATTACH tableModel to employee table
    HIDE configured columns from visual display
    KEEP the underlying column values in the table model
    DISPLAY employee table
END PROCEDURE
```

Hiding a column changes presentation, not the stored row. Some hidden columns contain government identifiers, salary values, and account information.

### 5.4.3 Minor feature 3.3 — Select employee and populate fields

```text
PROCEDURE POPULATE_SELECTED_EMPLOYEE
    selectedRow ← GET selected table row

    IF selectedRow does not exist THEN
        RETURN without changing form
    END IF

    FOR EACH mapped employee field:
        fieldValue ← GET value from selectedRow at mapped column index
        SET corresponding text field TO fieldValue
    END FOR
END PROCEDURE
```

### 5.4.4 Minor feature 3.4 — View employee information

```text
PROCEDURE VIEW_EMPLOYEE_INFORMATION
    selectedRow ← GET selected table row

    IF selectedRow does not exist THEN
        SHOW "Select an employee first" message
        RETURN
    END IF

    employeeData ← READ all displayed and required columns from selectedRow
    formattedDetails ← FORMAT employeeData as labelled information
    SHOW formattedDetails in an information dialog
END PROCEDURE
```

### 5.4.5 Minor feature 3.5 — Open Add Employee form

```text
PROCEDURE OPEN_ADD_EMPLOYEE_FORM
    CREATE Form with MainMenu as owner
    SET form to modal
    DISPLAY form
END PROCEDURE
```

### 5.4.6 Minor feature 3.6 — Validate new employee input

```text
FUNCTION VALIDATE_NEW_EMPLOYEE_INPUT(formValues)
    FOR EACH required field IN formValues:
        IF field is empty THEN
            SHOW missing-field error
            RETURN INVALID
        END IF
    END FOR

    TRY
        employeeID ← PARSE employee number as integer
        basicSalary ← PARSE basic salary as number
        riceSubsidy ← PARSE rice subsidy as number
        phoneAllowance ← PARSE phone allowance as number
        clothingAllowance ← PARSE clothing allowance as number
        grossSemiMonthlyRate ← PARSE gross semi-monthly rate as number
        hourlyRate ← PARSE hourly rate as number
    CATCH numeric conversion error:
        SHOW numeric-field error
        RETURN INVALID
    END TRY

    IF employeeID already exists in data.csv THEN
        SHOW duplicate employee-number error
        RETURN INVALID
    END IF

    RETURN VALID
END FUNCTION
```

The form's checks are not equivalent to the OOP `Employee` constructor's full validation. The Add Employee form does not invoke the OOP employee object as its validation authority.

### 5.4.7 Minor feature 3.7 — Create and append employee row

```text
PROCEDURE ADD_EMPLOYEE
    formValues ← READ all Add Employee form fields

    IF VALIDATE_NEW_EMPLOYEE_INPUT(formValues) = INVALID THEN
        KEEP form open
        RETURN
    END IF

    employeeRow ← ARRAY OF 24 values in the established column order

    employeeRow[19] ← default username "admin"
    employeeRow[20] ← default password "password"
    employeeRow[21] ← default leave credits 12
    employeeRow[22] ← approved leave count 0
    employeeRow[23] ← rejected leave count 0

    TRY
        OPEN data.csv FOR appending
        WRITE employeeRow
        CLOSE file
        SHOW employee-added confirmation
        CLOSE Add Employee form
    CATCH file error:
        SHOW add-employee failure
        KEEP form available
    END TRY
END PROCEDURE
```

**Security and account note:** the form inserts the same default username and password values for a new employee. Passwords remain plaintext in the data file. This is existing course-project behaviour, not a secure provisioning method.

### 5.4.8 Minor feature 3.8 — Update employee details

```text
PROCEDURE UPDATE_EMPLOYEE
    selectedRow ← GET selected row

    IF selectedRow does not exist THEN
        SHOW selection error
        RETURN
    END IF

    values ← READ employee-detail form fields

    TRY
        PARSE all numeric fields required by the form
    CATCH conversion error:
        SHOW invalid numeric input
        RETURN
    END TRY

    IF values.employeeID matches another row's employeeID THEN
        SHOW duplicate employee-number error
        RETURN
    END IF

    FOR EACH mapped field:
        SET selectedRow[fieldIndex] TO corresponding form value
    END FOR

    SHOW update confirmation
    NOTE: the modified table row is not written to disk until SAVE.
END PROCEDURE
```

### 5.4.9 Minor feature 3.9 — Delete employee

```text
PROCEDURE DELETE_EMPLOYEE
    selectedRow ← GET selected row

    IF selectedRow does not exist THEN
        SHOW selection error
        RETURN
    END IF

    answer ← SHOW deletion confirmation dialog

    IF answer = CONFIRM THEN
        REMOVE selectedRow from table model
        SHOW deletion confirmation
        NOTE: disk file remains unchanged until SAVE.
    ELSE
        KEEP row unchanged
    END IF
END PROCEDURE
```

### 5.4.10 Minor feature 3.10 — Save employee records

```text
PROCEDURE SAVE_EMPLOYEE_RECORDS
    OPEN data.csv FOR replacement

    WRITE table model header row

    FOR EACH row IN employee table model:
        values ← READ each column in row order
        WRITE values as one comma-separated line
    END FOR

    CLOSE file
    SHOW save confirmation

    IF an I/O error occurs THEN
        SHOW save failure
    END IF
END PROCEDURE
```

**Known limitation:** the current writer concatenates values as comma-separated text. It does not provide full CSV quoting and escaping for arbitrary commas, quotation marks, or line breaks inside fields.

### 5.4.11 Minor feature 3.11 — Refresh employee list

```text
PROCEDURE REFRESH_EMPLOYEE_RECORDS
    REMOVE all current rows from table model
    CALL LOAD_EMPLOYEE_RECORDS
END PROCEDURE
```

Because the refresh operation reloads from disk, changes that were made in the table but not saved can be discarded.

### 5.4.12 Minor feature 3.12 — Clear employee form

```text
PROCEDURE CLEAR_EMPLOYEE_FORM
    FOR EACH employee-detail input field:
        SET field value TO empty string
    END FOR
END PROCEDURE
```

### 5.4.13 Minor feature 3.13 — Open leave administration

```text
PROCEDURE OPEN_LEAVE_MANAGEMENT
    CREATE LeaveManagement dialog with MainMenu as owner
    SET dialog to modal
    DISPLAY dialog
END PROCEDURE
```

### 5.4.14 Minor feature 3.14 — Add Employee window sizing

```text
PROCEDURE FIT_ADD_EMPLOYEE_FORM_TO_SCREEN
    screenBounds ← GET usable display bounds
    preferredSize ← GET form's preferred dimensions
    finalWidth ← MIN(preferredSize.width, screenBounds.width)
    finalHeight ← MIN(preferredSize.height, screenBounds.height)

    SET form dimensions TO finalWidth and finalHeight

    IF content exceeds available height THEN
        PLACE form content inside scroll pane
    END IF

    REGISTER Escape key to close the form
END PROCEDURE
```

### 5.4.15 Minor feature 3.16 — Save-boundary handling

```text
PROCEDURE HANDLE_TABLE_EDIT(action)
    IF action is UPDATE OR DELETE THEN
        CHANGE table model only
        MARK the table model as potentially modified
    END IF

    IF action is SAVE THEN
        CALL SAVE_EMPLOYEE_RECORDS
    ELSE IF action is REFRESH THEN
        CALL REFRESH_EMPLOYEE_RECORDS
        NOTE: the current handler reloads from disk and discards unsaved table-model changes;
              it does not preserve those changes for later recovery.
    END IF
END PROCEDURE
```

This pseudocode makes explicit the distinction between editing the UI table and persisting the CSV file. The current interface should not be described as auto-saving every update or deletion.

---

# 6. Major Feature 4 — Employee Self-Service Dashboard

## 6.1 Purpose

The employee dashboard retrieves the logged-in employee's row from `data.csv` and presents the employee's own profile, employment data, government identifiers, salary information, and leave counters. It provides a salary-view action, a leave-application action, and a credential-change action. The profile-edit control does not directly edit the profile; it displays a message telling the employee to contact HR.

## 6.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 4.1 Identify logged-in employee | Receive employee number from successful login; locate matching row |
| 4.2 Load employee profile | Read personal, contact, employment, position, supervisor, and salary columns |
| 4.3 Display employee information | Populate profile labels and fields; display government identifiers as configured |
| 4.4 Display leave counters | Read leave credits, approved count, and rejected count from columns 21–23 |
| 4.5 View salary | Open attendance-based period list; select a month; invoke payroll backend; show salary result |
| 4.6 Apply for leave | Open `LeaveForm` with the current employee number |
| 4.7 Edit information request | Display contact-HR message; no direct employee profile mutation |
| 4.8 Change credentials | Open `ChangeCredentials` dialog for logged-in employee |
| 4.9 Employee logout | Confirm and return to the login entry route |
| 4.10 Employee clock | Display date and time at one-second intervals |

## 6.3 Pseudocode — Major Feature 4: Load and operate the employee dashboard

```text
PROCEDURE OPEN_EMPLOYEE_DASHBOARD(employeeNumber)
    CREATE Dashboard(employeeNumber)
    CALL LOAD_EMPLOYEE_DASHBOARD_DATA(employeeNumber)
    CALL START_CLOCK
    DISPLAY dashboard

    WHILE dashboard remains open:
        WAIT FOR employee action

        CASE action OF
            VIEW_SALARY:          CALL EMPLOYEE_VIEW_SALARY(employeeNumber)
            APPLY_FOR_LEAVE:      CALL OPEN_LEAVE_FORM(employeeNumber)
            EDIT_INFORMATION:     CALL DISPLAY_CONTACT_HR_MESSAGE
            CHANGE_CREDENTIALS:   CALL OPEN_CHANGE_CREDENTIALS(employeeNumber)
            LOGOUT:               CALL CONFIRM_AND_LOGOUT(dashboard)
            EXIT:                 CALL EXIT_APPLICATION
        END CASE
    END WHILE
END PROCEDURE
```

### 6.3.1 Minor feature 4.1 — Locate the logged-in employee

```text
FUNCTION FIND_EMPLOYEE_RECORD(employeeNumber)
    OPEN data.csv FOR reading

    FOR EACH employeeRow IN file:
        PARSE employeeRow

        IF employeeRow[0] = employeeNumber THEN
            CLOSE file
            RETURN employeeRow
        END IF
    END FOR

    CLOSE file
    RETURN NOT_FOUND
END FUNCTION
```

### 6.3.2 Minor feature 4.2 — Load dashboard profile data

```text
PROCEDURE LOAD_EMPLOYEE_DASHBOARD_DATA(employeeNumber)
    employeeRow ← CALL FIND_EMPLOYEE_RECORD(employeeNumber)

    IF employeeRow = NOT_FOUND THEN
        SHOW employee-record-not-found error
        RETURN
    END IF

    MAP employeeRow[1] TO last-name display
    MAP employeeRow[2] TO first-name display
    MAP employeeRow[3] TO birthday display
    MAP employeeRow[4] TO address display
    MAP employeeRow[5] TO phone display
    MAP employeeRow[6] TO SSS display
    MAP employeeRow[7] TO PhilHealth display
    MAP employeeRow[8] TO TIN display
    MAP employeeRow[9] TO Pag-IBIG display
    MAP employeeRow[10] TO employment-status display
    MAP employeeRow[11] TO position display
    MAP employeeRow[12] TO supervisor display
    MAP employeeRow[13..18] TO salary and allowance displays
    MAP employeeRow[21] TO leave-credit display
    MAP employeeRow[22] TO approved-leave display
    MAP employeeRow[23] TO rejected-leave display
END PROCEDURE
```

### 6.3.3 Minor feature 4.3 — Display employee profile

```text
PROCEDURE DISPLAY_EMPLOYEE_PROFILE(employeeData)
    SET personal information labels from employeeData
    SET employment information labels from employeeData
    SET salary and allowance labels from employeeData
    SET government identifier labels from employeeData
    REFRESH dashboard display
END PROCEDURE
```

The fields shown are based on the stored employee record. A display field does not imply that the employee can modify that field.

### 6.3.4 Minor feature 4.4 — Display leave counters

```text
PROCEDURE DISPLAY_EMPLOYEE_LEAVE_COUNTERS(employeeData)
    leaveCredits ← PARSE employeeData[21]
    approvedLeaves ← PARSE employeeData[22]
    rejectedLeaves ← PARSE employeeData[23]

    DISPLAY leaveCredits
    DISPLAY approvedLeaves
    DISPLAY rejectedLeaves
END PROCEDURE
```

### 6.3.5 Minor feature 4.5 — View employee salary

```text
PROCEDURE EMPLOYEE_VIEW_SALARY(employeeNumber)
    repository ← NEW AttendanceRepository
    availableMonths ← repository.monthsWithRecords(employeeNumber)

    IF availableMonths is empty THEN
        SHOW no-attendance-records message
        RETURN
    END IF

    selectedMonth ← PROMPT employee to select a month from availableMonths

    IF employee cancels THEN
        RETURN
    END IF

    periodStart ← first date of selectedMonth
    periodEnd ← last date of selectedMonth
    attendance ← repository.loadFor(employeeNumber, selectedMonth)
    workedDays ← attendance.countWorkedDays(periodStart, periodEnd)

    employeeRow ← CALL FIND_EMPLOYEE_RECORD(employeeNumber)
    employeeObject ← CALL BUILD_EMPLOYEE_OBJECT_FROM_ROW(employeeRow)

    calculator ← NEW PayrollCalculator with active statutory calculators
    payroll ← calculator.calculatePayroll(employeeObject, attendance,
                                          periodStart, periodEnd)

    CALL DISPLAY_PAYROLL_RESULT(payroll, employeeRow)
END PROCEDURE
```

The employee dashboard uses the actual months available for that employee in `attendance.csv`. It does not create a fabricated month when no matching timecard data exists.

### 6.3.6 Minor feature 4.6 — Open the leave form

```text
PROCEDURE OPEN_LEAVE_FORM(employeeNumber)
    CREATE LeaveForm(employeeNumber)
    SET LeaveForm to modal
    DISPLAY LeaveForm
END PROCEDURE
```

### 6.3.7 Minor feature 4.7 — Employee profile-edit restriction

```text
PROCEDURE DISPLAY_CONTACT_HR_MESSAGE
    SHOW message:
        "Please contact your HR to edit your information."
    DO NOT modify the employee record
END PROCEDURE
```

This is a restriction/message, not an implemented profile-editing feature.

### 6.3.8 Minor feature 4.8 — Open credential-change dialog

```text
PROCEDURE OPEN_CHANGE_CREDENTIALS(employeeNumber)
    CREATE ChangeCredentials(employeeNumber)
    SET dialog to modal
    DISPLAY dialog
END PROCEDURE
```

### 6.3.9 Minor feature 4.9 — Employee dashboard logout

```text
PROCEDURE EMPLOYEE_LOGOUT(dashboard)
    answer ← SHOW logout confirmation

    IF answer = CONFIRM THEN
        CLOSE dashboard
        CREATE Login entry screen
        DISPLAY Login entry screen
    ELSE
        KEEP dashboard open
    END IF
END PROCEDURE
```

---

# 7. Major Feature 5 — Attendance and Timecard Handling

## 7.1 Purpose

The refactored payroll screens read time entries from `attendance.csv`, rather than relying on the older fixed workday assumptions. `AttendanceRepository` reads the file, validates its rows, caches the parsed records, filters them by employee and month, and creates an `AttendanceLog` for payroll calculation.

The repository README and test output record 5,168 attendance rows for 34 employees, covering 3 June 2025 through 31 December 2025. The imported file's employee identifiers were remapped from 10001–10034 to 1–34 to match the identifiers in `data.csv`. Months are offered only when the selected employee has timecard records.

## 7.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 5.1 Locate attendance file | Resolve `attendance.csv` path; confirm that the file exists |
| 5.2 Load attendance file | Open file; read header; parse each row; cache parsed records |
| 5.3 Validate attendance row | Verify four required columns; parse employee number; validate ISO date; validate clock-in and clock-out time |
| 5.4 Handle malformed attendance | Identify line number; raise an explanatory I/O or parsing error; prevent invalid row from being silently accepted |
| 5.5 List months for employee | Filter by employee number; extract YearMonth values; sort chronologically |
| 5.6 Check employee-month availability | Determine whether at least one matching timecard row exists |
| 5.7 Retrieve a month's records | Filter records by employee number and selected month |
| 5.8 Build attendance log | Insert each date's clock-in and clock-out into `AttendanceLog` |
| 5.9 Validate clock-in | Validate employee ID; reject future clock-in; parse time; reject duplicate clock-in |
| 5.10 Validate clock-out | Require prior clock-in; reject duplicate clock-out; reject clock-out at/before clock-in; reject duration over 24 hours |
| 5.11 Calculate daily hours | Return zero for missing/incomplete record; otherwise calculate elapsed time |
| 5.12 Count worked days | Count complete clock-in/clock-out pairs in the inclusive selected period |
| 5.13 Format month and period | Produce month label; derive first and last dates of a month |

## 7.3 Pseudocode — Major Feature 5: Retrieve and prepare attendance for payroll

```text
PROCEDURE PREPARE_ATTENDANCE_FOR_PAYROLL(employeeNumber, selectedMonth)
    repository ← NEW AttendanceRepository
    availableMonths ← repository.monthsWithRecords(employeeNumber)

    IF selectedMonth is not in availableMonths THEN
        THROW no attendance for selected employee and month
    END IF

    log ← repository.loadFor(employeeNumber, selectedMonth)
    periodStart ← repository.periodStartOf(selectedMonth)
    periodEnd ← repository.periodEndOf(selectedMonth)
    workedDays ← log.countWorkedDays(periodStart, periodEnd)

    RETURN log, periodStart, periodEnd, workedDays
END PROCEDURE
```

### 7.3.1 Minor feature 5.1 — Locate the timecard file

```text
FUNCTION LOCATE_ATTENDANCE_FILE
    path ← "src/motorphemployee/attendance.csv"

    IF file does not exist at path THEN
        THROW file-not-found error
    END IF

    RETURN path
END FUNCTION
```

### 7.3.2 Minor feature 5.2 — Load and cache attendance data

```text
PROCEDURE LOAD_ATTENDANCE_RECORDS
    IF attendance records are already cached THEN
        RETURN cached records
    END IF

    path ← CALL LOCATE_ATTENDANCE_FILE
    OPEN path FOR reading
    READ first line

    IF first line is a header THEN
        SKIP it
    ELSE
        PROCESS first line as a data row
    END IF

    WHILE another line exists:
        lineNumber ← current file line number
        line ← READ next line
        record ← CALL PARSE_ATTENDANCE_ROW(line, lineNumber)
        ADD record to attendance cache
    END WHILE

    CLOSE file
    RETURN attendance cache
END PROCEDURE
```

### 7.3.3 Minor feature 5.3 — Parse and validate one row

```text
FUNCTION PARSE_ATTENDANCE_ROW(line, lineNumber)
    columns ← SPLIT line by comma

    IF number of columns is not exactly 4 THEN
        THROW error identifying lineNumber and expected column count
    END IF

    employeeID ← PARSE columns[0] as integer
    attendanceDate ← PARSE columns[1] using yyyy-MM-dd
    timeIn ← PARSE columns[2] using HH:mm:ss
    timeOut ← PARSE columns[3] using HH:mm:ss

    IF any conversion fails THEN
        THROW error identifying lineNumber and invalid value
    END IF

    RETURN attendance row(employeeID, attendanceDate, timeIn, timeOut)
END FUNCTION
```

### 7.3.4 Minor feature 5.4 — Handle invalid attendance file

```text
PROCEDURE HANDLE_ATTENDANCE_IMPORT_ERROR(error)
    STOP processing the invalid import
    PRESERVE the diagnostic message and offending line number when available
    PROPAGATE an IOException or equivalent repository error to the caller
    LET the payroll screen display a useful failure message
END PROCEDURE
```

### 7.3.5 Minor feature 5.5 — List available months for one employee

```text
FUNCTION MONTHS_WITH_RECORDS(employeeNumber)
    records ← CALL LOAD_ATTENDANCE_RECORDS
    monthSet ← EMPTY SET

    FOR EACH record IN records:
        IF record.employeeID = employeeNumber THEN
            ADD YEAR_MONTH(record.attendanceDate) TO monthSet
        END IF
    END FOR

    months ← CONVERT monthSet TO LIST
    SORT months in ascending chronological order
    RETURN months
END FUNCTION
```

### 7.3.6 Minor feature 5.6 — Check whether an employee has attendance

```text
FUNCTION HAS_RECORDS(employeeNumber)
    records ← CALL LOAD_ATTENDANCE_RECORDS

    FOR EACH record IN records:
        IF record.employeeID = employeeNumber THEN
            RETURN TRUE
        END IF
    END FOR

    RETURN FALSE
END FUNCTION
```

### 7.3.7 Minor feature 5.7 — Retrieve records for a selected month

```text
FUNCTION RECORDS_FOR(employeeNumber, selectedMonth)
    records ← CALL LOAD_ATTENDANCE_RECORDS
    result ← EMPTY LIST

    FOR EACH record IN records:
        IF record.employeeID = employeeNumber
           AND YEAR_MONTH(record.attendanceDate) = selectedMonth THEN
            APPEND record TO result
        END IF
    END FOR

    RETURN result
END FUNCTION
```

### 7.3.8 Minor feature 5.8 — Populate an AttendanceLog

```text
FUNCTION LOAD_ATTENDANCE_LOG(employeeNumber, selectedMonth)
    rows ← CALL RECORDS_FOR(employeeNumber, selectedMonth)
    log ← NEW AttendanceLog

    FOR EACH row IN rows:
        log.recordClockIn(employeeNumber, row.date, row.timeIn)
        log.recordClockOut(employeeNumber, row.date, row.timeOut)
    END FOR

    RETURN log
END FUNCTION
```

### 7.3.9 Minor feature 5.9 — Record clock-in

```text
PROCEDURE RECORD_CLOCK_IN(employeeID, date, time)
    IF employeeID <= 0 THEN
        THROW invalid employee identifier
    END IF

    IF date is in the future THEN
        THROW future clock-in is not allowed
    END IF

    parsedTime ← PARSE time in HH:mm or HH:mm:ss format

    IF employee already has a clock-in for date THEN
        THROW duplicate clock-in
    END IF

    CREATE daily entry if needed
    SET daily entry clockIn TO parsedTime
END PROCEDURE
```

### 7.3.10 Minor feature 5.10 — Record clock-out

```text
PROCEDURE RECORD_CLOCK_OUT(employeeID, date, time)
    entry ← FIND daily entry for employeeID and date

    IF entry does not exist OR entry.clockIn does not exist THEN
        THROW clock-out requires a prior clock-in
    END IF

    IF entry.clockOut already exists THEN
        THROW duplicate clock-out
    END IF

    parsedTime ← PARSE time in HH:mm or HH:mm:ss format

    IF parsedTime <= entry.clockIn THEN
        THROW clock-out must be later than clock-in
    END IF

    duration ← parsedTime - entry.clockIn

    IF duration > 24 hours THEN
        THROW invalid daily duration
    END IF

    SET entry.clockOut TO parsedTime
END PROCEDURE
```

### 7.3.11 Minor feature 5.11 — Calculate hours for a date

```text
FUNCTION DAILY_HOURS(employeeID, date)
    entry ← FIND daily entry for employeeID and date

    IF entry does not exist THEN
        RETURN 0
    END IF

    IF entry.clockIn is missing OR entry.clockOut is missing THEN
        RETURN 0
    END IF

    RETURN elapsed time between entry.clockIn and entry.clockOut
END FUNCTION
```

### 7.3.12 Minor feature 5.12 — Count worked days in a period

```text
FUNCTION COUNT_WORKED_DAYS(periodStart, periodEnd)
    count ← 0

    FOR EACH daily entry IN attendance log:
        IF periodStart <= daily entry.date <= periodEnd
           AND daily entry.clockIn exists
           AND daily entry.clockOut exists THEN
            count ← count + 1
        END IF
    END FOR

    RETURN count
END FUNCTION
```

The payroll calculation counts complete attendance pairs as worked days. It does not calculate gross pay from the exact duration of each timecard row; the payroll formula uses eight hours per counted worked day.

### 7.3.13 Minor feature 5.13 — Build a month period

```text
FUNCTION PERIOD_START_OF(month)
    RETURN first calendar date of month
END FUNCTION

FUNCTION PERIOD_END_OF(month)
    RETURN final calendar date of month
END FUNCTION

FUNCTION LABEL_OF(month)
    RETURN formatted month label used by the selection dialog
END FUNCTION
```

---

# 8. Major Feature 6 — Employee Identity, Validation, and Classification

## 8.1 Purpose

The OOP backend moves person and employee data out of procedural screen code and into domain classes. `Person` contains general identity and contact information. Abstract `Employee` extends `Person` with employee identifiers, government identifiers, employment classification, salary values, and common validation. `RegularEmployee` and `ProbationaryEmployee` provide different allowances, leave-credit entitlements, and promotion eligibility through overridden methods.

This feature supplies the payroll calculator with a typed employee object. It is also the principal example of inheritance and polymorphism in the repository.

## 8.2 Class structure

```text
Person
  └── Employee (abstract)
        ├── RegularEmployee
        └── ProbationaryEmployee

GovernmentContributionCalculator (abstract)
  ├── SSSCalculator
  ├── PhilHealthCalculator
  ├── PagIbigCalculator
  └── WithholdingTaxCalculator

PayrollCalculator
  ├── receives Employee
  ├── receives AttendanceLog
  └── receives a list of GovernmentContributionCalculator objects
```

## 8.3 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 6.1 Person creation | Validate names, address, phone, and birthday; store identity fields |
| 6.2 Birthday validation | Accept supported date formats; reject malformed or future dates |
| 6.3 Person accessors | Retrieve and update first name, surname, birthday, address, and phone |
| 6.4 Person derived values | Produce full name; calculate age; format person description |
| 6.5 Employee creation | Validate positive ID; government identifier fields; employment fields; salary and allowance values |
| 6.6 Employee common data | Store status, position, supervisor, base salary, gross semi-monthly rate, hourly rate, and clothing allowance |
| 6.7 Employee government identifiers | Store SSS, PhilHealth, TIN, and Pag-IBIG values as protected/final identifiers in the OOP object |
| 6.8 Employee abstract contract | Require subclasses to provide rice subsidy, phone allowance, annual leave credits, and promotion eligibility |
| 6.9 Regular employee rules | Provide regular allowances; 12 annual leave credits; promotion eligibility based on service |
| 6.10 Probationary employee rules | Provide probationary allowances; 6 annual leave credits; no promotion eligibility |
| 6.11 Setter validation | Reject invalid salary, rate, identifier, and allowance values through validated setters |
| 6.12 GUI-to-object mapping | Select subclass from stored employment status; instantiate the matching OOP employee object |

## 8.4 Pseudocode — Major Feature 6: Construct the correct employee object

```text
FUNCTION BUILD_EMPLOYEE_OBJECT_FROM_ROW(employeeRow)
    commonFields ← EXTRACT person and employee data from employeeRow

    IF employeeRow.status indicates Regular THEN
        employee ← NEW RegularEmployee(commonFields, regular service data)
    ELSE IF employeeRow.status indicates Probationary THEN
        employee ← NEW ProbationaryEmployee(commonFields, probation data)
    ELSE
        THROW unsupported employment status
    END IF

    RETURN employee
END FUNCTION
```

The GUI supplies placeholders for probationary fields when the CSV does not contain the relevant tenure or evaluation data: six probation months, start date `N/A`, and score `0`. Those placeholders are not evidence that the underlying employee records contain those values.

### 8.4.1 Minor feature 6.1 — Construct a Person

```text
CONSTRUCTOR Person(firstName, lastName, birthday, address, phoneNumber)
    VALIDATE firstName is not blank
    VALIDATE lastName is not blank
    VALIDATE address is not blank
    VALIDATE phoneNumber is not blank

    PARSE birthday using yyyy-MM-dd OR M/d/yyyy
    IF birthday cannot be parsed THEN
        THROW invalid birthday
    END IF

    IF birthday is later than current date THEN
        THROW future birthday is not allowed
    END IF

    STORE firstName
    STORE lastName
    STORE birthday
    STORE address
    STORE phoneNumber
END CONSTRUCTOR
```

### 8.4.2 Minor feature 6.2 — Validate phone number

```text
FUNCTION IS_VALID_PHONE(phoneNumber)
    IF phoneNumber is blank THEN
        RETURN FALSE
    END IF

    IF phoneNumber does not match [digits, +, parentheses, hyphens, spaces]
       OR length is less than 7
       OR length is greater than 20 THEN
        RETURN FALSE
    END IF

    RETURN TRUE
END FUNCTION
```

### 8.4.3 Minor feature 6.3 — Update Person details

```text
PROCEDURE UPDATE_ADDRESS(newAddress)
    IF newAddress is blank THEN
        THROW invalid address
    END IF
    SET address TO newAddress
END PROCEDURE

PROCEDURE UPDATE_PHONE(newPhone)
    IF IS_VALID_PHONE(newPhone) = FALSE THEN
        THROW invalid phone number
    END IF
    SET phoneNumber TO newPhone
END PROCEDURE
```

### 8.4.4 Minor feature 6.4 — Derived Person values

```text
FUNCTION GET_FULL_NAME
    RETURN lastName + ", " + firstName
END FUNCTION

FUNCTION GET_AGE
    RETURN number of completed years between birthday and current date
END FUNCTION

FUNCTION TO_STRING
    RETURN GET_FULL_NAME + " (" + formatted birthday + ")"
END FUNCTION
```

### 8.4.5 Minor feature 6.5 — Construct an Employee

```text
CONSTRUCTOR Employee(commonPersonData, employeeData)
    CALL Person constructor with commonPersonData

    IF employeeData.id <= 0 THEN
        THROW invalid employee ID
    END IF

    VALIDATE SSS number is present and has minimum accepted length
    VALIDATE PhilHealth number is present and has minimum accepted length
    VALIDATE TIN is present and has minimum accepted length
    VALIDATE Pag-IBIG number is present and has minimum accepted length

    VALIDATE status is not blank
    VALIDATE position is not blank
    VALIDATE supervisor is not blank

    VALIDATE basicSalary > 0
    VALIDATE grossSemiMonthlyRate > 0
    VALIDATE hourlyRate > 0
    VALIDATE clothingAllowance >= 0

    VALIDATE riceSubsidyInput >= 0
    VALIDATE phoneAllowanceInput >= 0

    STORE common employee values
    STORE government identifiers as read-only values

    NOTE: riceSubsidyInput and phoneAllowanceInput are validated,
          but the subclass supplies the effective allowance values.
END CONSTRUCTOR
```

### 8.4.6 Minor feature 6.6 — Employee salary and employment setters

```text
PROCEDURE SET_BASIC_SALARY(value)
    IF value <= 0 THEN
        THROW invalid basic salary
    END IF
    STORE value
END PROCEDURE

PROCEDURE SET_GROSS_SEMI_MONTHLY_RATE(value)
    IF value <= 0 THEN
        THROW invalid gross semi-monthly rate
    END IF
    STORE value
END PROCEDURE

PROCEDURE SET_HOURLY_RATE(value)
    IF value <= 0 THEN
        THROW invalid hourly rate
    END IF
    STORE value
END PROCEDURE

PROCEDURE SET_CLOTHING_ALLOWANCE(value)
    IF value < 0 THEN
        THROW negative allowance is not allowed
    END IF
    STORE value
END PROCEDURE
```

### 8.4.7 Minor feature 6.7 — Abstract Employee contract

```text
ABSTRACT METHOD GET_RICE_SUBSIDY
ABSTRACT METHOD GET_PHONE_ALLOWANCE
ABSTRACT METHOD GET_ANNUAL_LEAVE_CREDITS
ABSTRACT METHOD IS_ELIGIBLE_FOR_PROMOTION
```

The abstract methods force concrete employee classes to specify the behaviour for their employment type.

### 8.4.8 Minor feature 6.8 — Regular employee rules

```text
CONSTRUCTOR RegularEmployee(commonData, yearsOfService)
    CALL Employee constructor

    IF yearsOfService < 0 OR yearsOfService > 60 THEN
        THROW invalid years of service
    END IF

    STORE yearsOfService
END CONSTRUCTOR

FUNCTION GET_RICE_SUBSIDY
    RETURN 1500
END FUNCTION

FUNCTION GET_PHONE_ALLOWANCE
    RETURN 2000
END FUNCTION

FUNCTION GET_ANNUAL_LEAVE_CREDITS
    RETURN 12
END FUNCTION

FUNCTION IS_ELIGIBLE_FOR_PROMOTION
    RETURN yearsOfService >= 2
END FUNCTION
```

### 8.4.9 Minor feature 6.9 — Probationary employee rules

```text
CONSTRUCTOR ProbationaryEmployee(commonData, probationMonths,
                                 startDate, evaluationScore)
    CALL Employee constructor

    IF probationMonths < 0 OR probationMonths > 12 THEN
        THROW invalid probation duration
    END IF

    IF startDate is blank THEN
        THROW missing probation start date
    END IF

    IF evaluationScore < 0 OR evaluationScore > 100 THEN
        THROW invalid evaluation score
    END IF

    STORE probationMonths, startDate, evaluationScore
END CONSTRUCTOR

FUNCTION GET_RICE_SUBSIDY
    RETURN 1000
END FUNCTION

FUNCTION GET_PHONE_ALLOWANCE
    RETURN 500
END FUNCTION

FUNCTION GET_ANNUAL_LEAVE_CREDITS
    RETURN 6
END FUNCTION

FUNCTION IS_ELIGIBLE_FOR_PROMOTION
    RETURN FALSE
END FUNCTION
```

### 8.4.10 Minor feature 6.10 — Polymorphic allowance and leave retrieval

```text
PROCEDURE READ_EMPLOYEE_BENEFITS(employee)
    rice ← employee.GET_RICE_SUBSIDY()
    phone ← employee.GET_PHONE_ALLOWANCE()
    leaveCredits ← employee.GET_ANNUAL_LEAVE_CREDITS()
    promotionEligible ← employee.IS_ELIGIBLE_FOR_PROMOTION()

    RETURN rice, phone, leaveCredits, promotionEligible
END PROCEDURE
```

The caller works with an `Employee` reference. The regular or probationary implementation executes according to the runtime employee object.

---

# 9. Major Feature 7 — Payroll Computation and Payslip Result

## 9.1 Purpose

`PayrollCalculator` centralises the payroll calculation that previously appeared as duplicated methods in `MainMenu` and `Dashboard`. Both GUI entry points now pass an OOP employee object and attendance information to the backend, receive a `PayrollRecord`, and display the result.

The calculation flow is:

1. Validate the employee, attendance log, and requested pay period.
2. Count complete attendance days within the pay period.
3. Calculate gross pay using the employee's hourly rate and the fixed eight-hour day assumption.
4. Calculate active SSS, PhilHealth, and Pag-IBIG deductions.
5. Subtract those contributions from gross pay to obtain the taxable amount used by the withholding-tax calculator.
6. Calculate withholding tax.
7. Add all deductions, derive net pay, and validate the result.
8. Construct an immutable `PayrollRecord` and return it to the GUI.

The GUI's month selection uses the full selected calendar month. The backend also has an overload that derives a recent semi-monthly period, described in the code notes as a 15-day period. These are separate entry paths, not interchangeable date rules.

## 9.2 Payroll calculation assumptions and current limitations

| Item | Current behaviour in the repository |
|---|---|
| Attendance unit | A worked day is counted when both clock-in and clock-out exist for that date |
| Paid hours per worked day | Fixed at 8 hours in the gross-pay calculation |
| Gross pay | `workedDays × hourlyRate × 8` |
| Exact timecard duration | `AttendanceLog` can calculate daily duration, but `PayrollCalculator` uses the worked-day count and the fixed eight-hour assumption for gross pay |
| Zero gross | Rejected; payroll calculation does not accept a non-positive gross amount |
| Statutory deductions | SSS, PhilHealth, and Pag-IBIG are applied through active calculator objects |
| Withholding-tax base | Gross pay less the three statutory contributions |
| Net pay | Gross pay less total statutory deductions and withholding tax |
| Absences | The `PayrollRecord` currently receives an absence value of zero; the calculation does not derive absences from missing attendance dates |
| Leave credits | The employee class's annual leave-credit entitlement is recorded as a leave-related value; approved leave days do not currently alter the gross-pay formula |
| Rice and phone allowance | The subclass exposes these values, but the current gross-pay formula does not add them to the worked-day gross result |
| Clothing allowance | Stored in employee/payroll data; the current worked-day gross formula does not add it to gross pay |
| Employee status | GUI creates `RegularEmployee` or `ProbationaryEmployee` based on the employee row's status |
| Missing attendance month | The GUI offers only months with records; absence of records is shown as an error rather than silently generating payroll |
| Payroll result | `PayrollRecord` is immutable after construction |

These are descriptions of the present source, not recommendations about how a production payroll system should calculate compensation.

## 9.3 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 7.1 Select payroll employee | Read selected employee row; retrieve employee ID; obtain employee details |
| 7.2 Select payroll period | List months with actual attendance; display month picker; derive period start and end |
| 7.3 Build typed employee | Read employee status; instantiate the correct subclass; supply available or documented placeholder data |
| 7.4 Load timecard data | Request employee/month attendance from `AttendanceRepository`; create `AttendanceLog` |
| 7.5 Count worked days | Count dates with a complete clock-in/clock-out pair within the requested period |
| 7.6 Validate calculation request | Require employee and attendance; validate period start/end; ensure period order |
| 7.7 Calculate gross pay | Multiply worked days, hourly rate, and eight hours |
| 7.8 Validate gross pay | Reject gross pay at or below zero |
| 7.9 Calculate statutory deductions | Invoke active SSS, PhilHealth, and Pag-IBIG calculator objects |
| 7.10 Validate deduction total | Reject statutory contributions that equal or exceed gross pay |
| 7.11 Calculate taxable pay | Subtract statutory contributions from gross pay |
| 7.12 Calculate withholding tax | Apply the monthly tax table to the taxable base passed by the payroll calculator |
| 7.13 Calculate total deductions | Sum SSS, PhilHealth, Pag-IBIG, and withholding tax |
| 7.14 Calculate net pay | Subtract total deductions from gross pay |
| 7.15 Assemble payroll result | Populate employee, period, salary, benefit, hours, deduction, leave, and net-pay fields |
| 7.16 Validate payroll result | Require positive employee ID; valid dates; non-negative values; net-pay arithmetic within tolerance |
| 7.17 Return payroll result | Return immutable `PayrollRecord` to GUI caller |
| 7.18 Display HR payroll result | Show employee details, workdays, gross, deductions, and net pay |
| 7.19 Display employee salary result | Show the logged-in employee's salary breakdown in a dialog |
| 7.20 Default semi-monthly period | Calculate using the backend overload that selects the recent 15-day period |

## 9.4 Pseudocode — Major Feature 7: Calculate payroll

```text
FUNCTION CALCULATE_PAYROLL(employee, attendanceLog, periodStart, periodEnd)
    CALL VALIDATE_PAYROLL_REQUEST(employee, attendanceLog,
                                  periodStart, periodEnd)

    workedDays ← attendanceLog.COUNT_WORKED_DAYS(periodStart, periodEnd)
    hoursWorked ← workedDays × 8

    grossPay ← workedDays × employee.hourlyRate × 8

    IF grossPay <= 0 THEN
        THROW payroll gross amount must be positive
    END IF

    sssAmount ← 0
    philHealthAmount ← 0
    pagIbigAmount ← 0

    FOR EACH calculator IN statutory calculators:
        IF calculator is active AND calculator is not withholding-tax calculator THEN
            amount ← calculator.CALCULATE(grossPay)

            IF calculator is SSSCalculator THEN
                sssAmount ← amount
            ELSE IF calculator is PhilHealthCalculator THEN
                philHealthAmount ← amount
            ELSE IF calculator is PagIbigCalculator THEN
                pagIbigAmount ← amount
            END IF
        END IF
    END FOR

    statutoryTotal ← sssAmount + philHealthAmount + pagIbigAmount

    IF statutoryTotal >= grossPay THEN
        THROW statutory deductions leave no positive taxable amount
    END IF

    taxablePay ← grossPay - statutoryTotal
    withholdingTax ← WITHHOLDING_TAX_CALCULATOR.CALCULATE(taxablePay)

    totalDeductions ← statutoryTotal + withholdingTax
    netPay ← grossPay - totalDeductions

    payrollRecord ← BUILD_IMMUTABLE_PAYROLL_RECORD(
        employee,
        periodStart,
        periodEnd,
        workedDays,
        hoursWorked,
        grossPay,
        sssAmount,
        philHealthAmount,
        pagIbigAmount,
        withholdingTax,
        totalDeductions,
        netPay
    )

    RETURN payrollRecord
END FUNCTION
```

### 9.4.1 Minor feature 7.1 — Select an employee for HR payroll

```text
PROCEDURE SELECT_EMPLOYEE_FOR_PAYROLL
    selectedRow ← GET selected row in HR employee table

    IF selectedRow does not exist THEN
        SHOW select-an-employee message
        RETURN NO_EMPLOYEE
    END IF

    employeeNumber ← selectedRow[0]
    employeeData ← selectedRow
    RETURN employeeNumber, employeeData
END PROCEDURE
```

### 9.4.2 Minor feature 7.2 — Select a month with attendance

```text
PROCEDURE SELECT_PAYROLL_MONTH(employeeNumber)
    repository ← NEW AttendanceRepository
    months ← repository.monthsWithRecords(employeeNumber)

    IF months is empty THEN
        SHOW no attendance records for this employee
        RETURN NO_MONTH
    END IF

    selectedMonth ← SHOW month-selection dialog using months

    IF user cancels THEN
        RETURN CANCELLED
    END IF

    RETURN selectedMonth
END PROCEDURE
```

### 9.4.3 Minor feature 7.3 — Build the correct employee subclass

```text
FUNCTION BUILD_EMPLOYEE_FOR_PAYROLL(employeeRow)
    EXTRACT first name, last name, birthday, address, phone,
            employee number, government numbers, status, position,
            supervisor, salary, rates, and clothing allowance

    IF status indicates Regular THEN
        yearsOfService ← value available to the GUI
        IF no tenure value exists in the source row THEN
            yearsOfService ← 0
        END IF
        RETURN NEW RegularEmployee(common values, yearsOfService)

    ELSE IF status indicates Probationary THEN
        probationMonths ← 6
        probationStartDate ← "N/A"
        evaluationScore ← 0
        RETURN NEW ProbationaryEmployee(common values,
                                        probationMonths,
                                        probationStartDate,
                                        evaluationScore)
    ELSE
        THROW unsupported employment status
    END IF
END FUNCTION
```

The current CSV does not contain full probationary tenure and evaluation fields. The values shown above are the GUI's placeholders, not inferred historical employment facts.

### 9.4.4 Minor feature 7.4 — Load employee attendance

```text
FUNCTION LOAD_EMPLOYEE_ATTENDANCE(employeeNumber, selectedMonth)
    repository ← NEW AttendanceRepository
    log ← repository.loadFor(employeeNumber, selectedMonth)

    IF log contains no complete attendance entries THEN
        RETURN log with zero worked days
    END IF

    RETURN log
END FUNCTION
```

### 9.4.5 Minor feature 7.5 — Count attendance within the payroll period

```text
FUNCTION GET_WORKED_DAYS(attendanceLog, periodStart, periodEnd)
    workedDays ← 0

    FOR EACH entry IN attendanceLog:
        IF entry.date >= periodStart AND entry.date <= periodEnd
           AND entry.clockIn exists
           AND entry.clockOut exists THEN
            workedDays ← workedDays + 1
        END IF
    END FOR

    RETURN workedDays
END FUNCTION
```

### 9.4.6 Minor feature 7.6 — Validate the payroll request

```text
PROCEDURE VALIDATE_PAYROLL_REQUEST(employee, attendanceLog,
                                  periodStart, periodEnd)
    IF employee is NULL THEN
        THROW missing employee
    END IF

    IF attendanceLog is NULL THEN
        THROW missing attendance log
    END IF

    IF periodStart is NULL OR periodEnd is NULL THEN
        THROW missing pay-period date
    END IF

    IF periodEnd < periodStart THEN
        THROW invalid pay-period order
    END IF
END PROCEDURE
```

### 9.4.7 Minor feature 7.7 — Calculate gross pay

```text
FUNCTION CALCULATE_GROSS_PAY(workedDays, hourlyRate)
    IF workedDays < 0 OR hourlyRate < 0 THEN
        THROW negative payroll input
    END IF

    hoursPerWorkedDay ← 8
    grossPay ← workedDays × hourlyRate × hoursPerWorkedDay
    RETURN grossPay
END FUNCTION
```

This exact formula is important. The repository's payroll parity notes establish that the refactor's gross-pay result matched the CP2 baseline for 408 employee-month combinations. The timecard duration is not substituted into this formula; a complete workday is treated as eight paid hours.

### 9.4.8 Minor feature 7.8 — Reject zero or negative gross

```text
PROCEDURE VALIDATE_GROSS_PAY(grossPay)
    IF grossPay <= 0 THEN
        THROW invalid gross pay
    END IF
END PROCEDURE
```

### 9.4.9 Minor feature 7.9 — Calculate statutory deductions as a polymorphic loop

```text
FUNCTION CALCULATE_ACTIVE_STATUTORY_DEDUCTIONS(grossPay, calculators)
    amounts ← EMPTY MAP

    FOR EACH calculator IN calculators:
        IF calculator is active AND calculator is not WithholdingTaxCalculator THEN
            amount ← calculator.calculate(grossPay)
            amounts[calculator.code] ← amount
        END IF
    END FOR

    RETURN amounts
END FUNCTION
```

### 9.4.10 Minor feature 7.10 — Validate statutory deductions

```text
PROCEDURE VALIDATE_STATUTORY_TOTAL(grossPay, sss, philHealth, pagIbig)
    total ← sss + philHealth + pagIbig

    IF total >= grossPay THEN
        THROW statutory deductions must be less than gross pay
    END IF

    RETURN total
END PROCEDURE
```

### 9.4.11 Minor feature 7.11 — Derive taxable pay

```text
FUNCTION CALCULATE_TAXABLE_PAY(grossPay, statutoryTotal)
    taxablePay ← grossPay - statutoryTotal

    IF taxablePay < 0 THEN
        THROW taxable pay cannot be negative
    END IF

    RETURN taxablePay
END FUNCTION
```

### 9.4.12 Minor feature 7.12 — Calculate withholding tax

```text
FUNCTION CALCULATE_WITHHOLDING_TAX(taxablePay)
    RETURN WithholdingTaxCalculator.calculate(taxablePay)
END FUNCTION
```

The detailed tax bracket pseudocode appears in Major Feature 8, because the calculator is a separate backend feature.

### 9.4.13 Minor feature 7.13 — Sum deductions

```text
FUNCTION SUM_TOTAL_DEDUCTIONS(sss, philHealth, pagIbig, withholdingTax)
    RETURN sss + philHealth + pagIbig + withholdingTax
END FUNCTION
```

### 9.4.14 Minor feature 7.14 — Calculate net pay

```text
FUNCTION CALCULATE_NET_PAY(grossPay, totalDeductions)
    netPay ← grossPay - totalDeductions

    IF netPay < 0 THEN
        THROW net pay cannot be negative
    END IF

    RETURN netPay
END FUNCTION
```

### 9.4.15 Minor feature 7.15 — Assemble PayrollRecord

```text
FUNCTION BUILD_IMMUTABLE_PAYROLL_RECORD(employee, dates, metrics, amounts)
    record ← NEW PayrollRecord with:
        employee ID
        issue date/time
        period start and end
        salary values
        allowance values
        gross pay
        semi-monthly pay value
        net pay
        SSS, PhilHealth, Pag-IBIG, and withholding-tax amounts
        total deduction amount
        leave-credit value
        worked hours
        hourly rate
        absence count = 0

    CALL VALIDATE_PAYROLL_RECORD(record)
    RETURN record
END FUNCTION
```

### 9.4.16 Minor feature 7.16 — Validate PayrollRecord

```text
PROCEDURE VALIDATE_PAYROLL_RECORD(record)
    IF record.employeeID <= 0 THEN
        THROW invalid employee ID
    END IF

    IF record.periodStart is NULL OR record.periodEnd is NULL THEN
        THROW missing pay-period date
    END IF

    IF record.periodEnd < record.periodStart THEN
        THROW invalid date range
    END IF

    FOR EACH numeric amount in record:
        IF amount < 0 OR amount is not a valid numeric value THEN
            THROW invalid payroll amount
        END IF
    END FOR

    expectedNet ← record.grossPay - record.totalDeductions

    IF ABS(expectedNet - record.netPay) > 0.01 THEN
        THROW payroll arithmetic does not balance
    END IF
END PROCEDURE
```

The class uses a small arithmetic tolerance when validating that net pay equals gross pay minus deductions.

### 9.4.17 Minor feature 7.17 — Return immutable payroll result

```text
FUNCTION RETURN_PAYROLL_RESULT(record)
    DO NOT expose setters that mutate the constructed record
    RETURN record
END FUNCTION
```

### 9.4.18 Minor feature 7.18 — Display HR payroll breakdown

```text
PROCEDURE DISPLAY_HR_PAYROLL_RESULT(employeeData, record)
    DISPLAY employee number and employee name
    DISPLAY employment status and selected pay period
    DISPLAY worked days
    DISPLAY gross salary
    DISPLAY SSS deduction
    DISPLAY PhilHealth deduction
    DISPLAY Pag-IBIG deduction
    DISPLAY withholding tax
    DISPLAY total deductions
    DISPLAY net pay
    SHOW result in payroll information dialog
END PROCEDURE
```

### 9.4.19 Minor feature 7.19 — Display employee salary breakdown

```text
PROCEDURE DISPLAY_EMPLOYEE_PAYROLL_RESULT(record)
    DISPLAY logged-in employee profile information
    DISPLAY period and worked-day count
    DISPLAY gross pay and deduction breakdown
    DISPLAY final net pay
    SHOW result in employee salary dialog
END PROCEDURE
```

### 9.4.20 Minor feature 7.20 — Backend default semi-monthly overload

```text
FUNCTION CALCULATE_PAYROLL_FOR_RECENT_SEMI_MONTH(employee, attendanceLog)
    currentDate ← current date in Asia/Manila
    periodEnd ← currentDate
    periodStart ← date 14 days before periodEnd

    RETURN CALCULATE_PAYROLL(employee, attendanceLog,
                             periodStart, periodEnd)
END FUNCTION
```

This describes the documented recent 15-day default. The GUI path explicitly supplies the calendar month's start and end dates instead.

---

# 10. Major Feature 8 — Government Contribution and Withholding-Tax Calculators

## 10.1 Purpose

The OOP backend defines an abstract `GovernmentContributionCalculator` and four concrete calculators:

- `SSSCalculator`
- `PhilHealthCalculator`
- `PagIbigCalculator`
- `WithholdingTaxCalculator`

Each calculator provides a common `calculate` operation, a name/code, and an active-state flag. Payroll holds the parent calculator type and invokes each concrete implementation polymorphically. The three employee-side statutory contributions are included before withholding tax; withholding tax is calculated against the gross amount after those contributions are deducted.

The repository includes a matrix-verification document. The figures below describe the implemented rules recorded in the source and the project's verification notes; they should not be read as a claim that the same rates remain legally current outside the code's stated project assumptions.

## 10.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 8.1 Abstract calculator construction | Validate calculator name and code; set active state; expose shared calculation contract |
| 8.2 Salary input validation | Reject negative salary; reject NaN; preserve calculator-specific bounds |
| 8.3 Calculator activation | Read active state; enable or disable a calculator |
| 8.4 SSS contribution | Apply below-table amount; find salary bracket; calculate bracket amount; apply maximum |
| 8.5 PhilHealth contribution | Calculate 3% premium; enforce minimum and maximum; return employee's half |
| 8.6 Pag-IBIG contribution | Apply 1% or 2% employee rate by salary band; cap at 100; handle salary below the matrix floor |
| 8.7 Withholding-tax contribution | Select one of six monthly tax bands; apply base tax and marginal rate; handle tax-free floor and bracket boundaries |
| 8.8 Polymorphic invocation | Iterate over calculator objects; skip inactive calculators; associate result with calculator code |
| 8.9 Calculation verification | Compare outputs with expected matrix cases; test boundary values and invalid inputs |

## 10.3 Common calculator contract

```text
ABSTRACT CLASS GovernmentContributionCalculator
    FIELD name
    FIELD code
    FIELD active = TRUE

    CONSTRUCTOR(name, code)
        IF name is blank OR code is blank THEN
            THROW invalid calculator identity
        END IF
        STORE name and code
        SET active TO TRUE
    END CONSTRUCTOR

    ABSTRACT FUNCTION calculate(salary)

    FUNCTION isActive
        RETURN active
    END FUNCTION

    PROCEDURE setActive(value)
        SET active TO value
    END PROCEDURE
END CLASS
```

### 10.3.1 Minor feature 8.1 — Construct a contribution calculator

```text
PROCEDURE CONSTRUCT_CALCULATOR(name, code)
    IF name is blank THEN
        THROW missing calculator name
    END IF

    IF code is blank THEN
        THROW missing calculator code
    END IF

    SET name
    SET code
    SET active = TRUE
END PROCEDURE
```

### 10.3.2 Minor feature 8.2 — Validate salary input

```text
PROCEDURE REQUIRE_VALID_SALARY(salary)
    IF salary is NaN OR salary < 0 THEN
        THROW salary must be a valid non-negative amount
    END IF
END PROCEDURE
```

The shared guard rejects negative values and NaN. Individual calculators then apply their own bracket rules. The pseudocode does not add an unverified finite-number guard that is not documented as part of the shared method.

### 10.3.3 Minor feature 8.3 — Enable or disable a calculator

```text
PROCEDURE SET_CALCULATOR_ACTIVE(calculator, isActive)
    calculator.active ← isActive
END PROCEDURE

FUNCTION IS_CALCULATOR_ACTIVE(calculator)
    RETURN calculator.active
END FUNCTION
```

## 10.4 SSS calculator

### 10.4.1 Implemented calculation details

- The implementation contains a below-table contribution amount of **₱135**.
- The matrix logic contains **43 salary bands** beginning at ₱3,250, with ₱500 increments in the bracket progression.
- The contribution progression begins at **₱157.50** and increases by **₱22.50** per band.
- The maximum employee-side SSS amount represented by the calculator is **₱1,125**, reached at the top band of ₱24,750 and above.
- The lower bound of a bracket is inclusive. A salary exactly on a bracket boundary belongs to the new bracket, not the previous one.
- The source reports a bracket count of 45 when the below-table and terminal cases are included in the total count reported by the method.

### 10.4.2 Minor feature 8.4 — SSS band calculation

```text
FUNCTION CALCULATE_SSS(salary)
    CALL REQUIRE_VALID_SALARY(salary)

    IF salary is below the first contribution-table band THEN
        RETURN 135.00
    END IF

    bracketLower ← 3250
    contribution ← 157.50

    WHILE bracketLower < 24750:
        bracketUpperExclusive ← bracketLower + 500

        IF salary >= bracketLower AND salary < bracketUpperExclusive THEN
            RETURN contribution
        END IF

        bracketLower ← bracketLower + 500
        contribution ← contribution + 22.50
    END WHILE

    RETURN 1125.00
END FUNCTION
```

The implementation stores the band data directly rather than necessarily using this exact loop. This pseudocode expresses the bracket progression and inclusive lower-bound rule.

## 10.5 PhilHealth calculator

### 10.5.1 Implemented calculation details

- Total premium rate: **3%** of salary used as the calculator's salary base.
- Minimum total premium: **₱300** when the salary base is at or below ₱10,000.
- Maximum total premium: **₱1,800** when the salary base exceeds ₱59,999.99.
- Employee share: **one half of the total premium**.
- The implementation therefore returns an employee contribution from ₱150 to ₱900.
- Example recorded in the project notes: a salary base of ₱25,000 produces a total premium of ₱750 and an employee share of ₱375.

### 10.5.2 Minor feature 8.5 — PhilHealth calculation

```text
FUNCTION CALCULATE_PHILHEALTH(salary)
    CALL REQUIRE_VALID_SALARY(salary)

    totalPremium ← salary × 0.03

    IF totalPremium < 300 THEN
        totalPremium ← 300
    END IF

    IF totalPremium > 1800 THEN
        totalPremium ← 1800
    END IF

    employeeShare ← totalPremium / 2
    RETURN employeeShare
END FUNCTION
```

## 10.6 Pag-IBIG calculator

### 10.6.1 Implemented calculation details

- The calculator uses an employee-side rate of **1%** for the ₱1,000–₱1,500 salary band, inclusive.
- It uses **2%** for salary above ₱1,500.
- The employee contribution is capped at **₱100**.
- A salary below ₱1,000 produces no contribution in the implementation because it falls below the defined matrix floor.
- The calculator does not add an employer share to the employee deduction result.

### 10.6.2 Minor feature 8.6 — Pag-IBIG calculation

```text
FUNCTION CALCULATE_PAGIBIG(salary)
    CALL REQUIRE_VALID_SALARY(salary)

    IF salary < 1000 THEN
        RETURN 0
    ELSE IF salary <= 1500 THEN
        rate ← 0.01
    ELSE
        rate ← 0.02
    END IF

    contribution ← salary × rate

    IF contribution > 100 THEN
        contribution ← 100
    END IF

    RETURN contribution
END FUNCTION
```

## 10.7 Withholding-tax calculator

### 10.7.1 Implemented bracket representation

The code defines six monthly tax bands. The following table restates the thresholds and marginal-rate data represented by the calculator:

| Band | Monthly taxable income range | Base tax | Marginal rate |
|---:|---:|---:|---:|
| 1 | ₱0–₱20,832 | ₱0 | 0% |
| 2 | Above ₱20,832 to ₱33,333 | ₱0 | 20% on excess over ₱20,833 |
| 3 | Above ₱33,333 to ₱66,667 | ₱2,500 | 25% on excess over ₱33,333 |
| 4 | Above ₱66,667 to ₱166,667 | ₱10,833 | 30% on excess over ₱66,667 |
| 5 | Above ₱166,667 to ₱666,667 | ₱40,833.33 | 32% on excess over ₱166,667 |
| 6 | Above ₱666,667 | ₱200,833.33 | 35% on excess over ₱666,667 |

The project notes identify a boundary correction around ₱20,832/₱20,833. The implementation uses ₱20,832 as the tax-free upper bound and ₱20,833 as the next bracket's lower bound, avoiding an erroneous use of ₱20,832 as the excess base.

### 10.7.2 Minor feature 8.7 — Withholding-tax calculation

```text
FUNCTION CALCULATE_WITHHOLDING_TAX(taxableMonthlyIncome)
    CALL REQUIRE_VALID_SALARY(taxableMonthlyIncome)

    IF taxableMonthlyIncome <= 20832 THEN
        RETURN 0
    ELSE IF taxableMonthlyIncome <= 33333 THEN
        baseTax ← 0
        lowerBound ← 20833
        marginalRate ← 0.20
    ELSE IF taxableMonthlyIncome <= 66667 THEN
        baseTax ← 2500
        lowerBound ← 33333
        marginalRate ← 0.25
    ELSE IF taxableMonthlyIncome <= 166667 THEN
        baseTax ← 10833
        lowerBound ← 66667
        marginalRate ← 0.30
    ELSE IF taxableMonthlyIncome <= 666667 THEN
        baseTax ← 40833.33
        lowerBound ← 166667
        marginalRate ← 0.32
    ELSE
        baseTax ← 200833.33
        lowerBound ← 666667
        marginalRate ← 0.35
    END IF

    excess ← MAX(0, taxableMonthlyIncome - lowerBound)
    tax ← baseTax + (excess × marginalRate)
    RETURN tax
END FUNCTION
```

### 10.7.3 Minor feature 8.8 — Invoke all active calculators

```text
FUNCTION RUN_ACTIVE_CALCULATORS(salary, calculatorList)
    resultMap ← EMPTY MAP

    FOR EACH calculator IN calculatorList:
        IF calculator.isActive() THEN
            result ← calculator.calculate(salary)
            resultMap[calculator.code] ← result
        END IF
    END FOR

    RETURN resultMap
END FUNCTION
```

In the payroll path, withholding tax is treated separately because it uses the salary remaining after the employee-side SSS, PhilHealth, and Pag-IBIG amounts have been deducted.

### 10.7.4 Minor feature 8.9 — Calculator verification

```text
PROCEDURE VERIFY_CALCULATOR(calculator, testCases)
    FOR EACH testCase IN testCases:
        actual ← calculator.calculate(testCase.input)

        IF ROUND(actual, 2) != ROUND(testCase.expected, 2) THEN
            RECORD failure with input, expected, and actual values
        ELSE
            RECORD success
        END IF
    END FOR

    REPORT number of passed and failed cases
END PROCEDURE
```

The repository includes matrix-verification and console-test outputs. The documented test total for the backend suite is 91 passing checks.

---

# 11. Major Feature 9 — Employee Leave Application

## 11.1 Purpose

An employee opens `LeaveForm` from the dashboard and submits a request using a leave category, a number of days, and a reason. The form receives the employee number from the logged-in dashboard and appends the request to `pendingleaves.csv`.

The leave categories displayed by the form are:

- Maternity / Paternity Leave
- Sick Leave
- Vacation Leave
- Emergency Leave

The form currently parses the number of days as an integer. The GUI does not fully validate the positive-day rule or require a non-empty reason before appending. The later approval service validates that the request contains at least one day. The form also writes directly to the pending CSV instead of delegating the initial submission to `LeaveService`, which the repository lists as an open issue.

## 11.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 9.1 Open leave form | Receive employee number; create modal `LeaveForm`; display form |
| 9.2 Select leave type | Present available categories; capture chosen category |
| 9.3 Enter leave duration | Read days field; parse integer; display numeric error if parsing fails |
| 9.4 Enter leave reason | Read reason text; retain the text for the pending request |
| 9.5 Build request row | Combine employee number, type, days, and reason |
| 9.6 Append pending request | Open `pendingleaves.csv` in append mode; add request row; close file |
| 9.7 Confirm submission | Show success message; close form |
| 9.8 Cancel request form | Close form without writing the request |
| 9.9 Approval-time validation | Require positive days; verify employee exists; enforce available leave-credit rule |
| 9.10 Submission-path limitation | Direct CSV append bypasses the transaction service; form-side checks are weaker than approval-side checks |

## 11.3 Pseudocode — Major Feature 9: Submit a leave request

```text
PROCEDURE SUBMIT_LEAVE_APPLICATION(employeeNumber)
    leaveType ← READ selected leave type
    daysInput ← READ days field
    reason ← READ reason field

    TRY
        days ← PARSE daysInput as integer
    CATCH numeric conversion error:
        SHOW "Enter a valid number of days" error
        RETURN
    END TRY

    requestRow ← employeeNumber, leaveType, days, reason

    TRY
        OPEN pendingleaves.csv in append mode
        APPEND requestRow
        CLOSE file

        SHOW leave-submission success message
        CLOSE leave form
    CATCH I/O error:
        SHOW leave-submission failure
        KEEP form available
    END TRY
END PROCEDURE
```

### 11.3.1 Minor feature 9.1 — Open the leave form

```text
PROCEDURE OPEN_LEAVE_APPLICATION(employeeNumber)
    CREATE LeaveForm(employeeNumber)
    SET the employee number as the request owner
    SET form modality to application-modal
    DISPLAY form
END PROCEDURE
```

### 11.3.2 Minor feature 9.2 — Select leave type

```text
PROCEDURE SELECT_LEAVE_TYPE
    DISPLAY the available leave categories:
        Maternity / Paternity Leave
        Sick Leave
        Vacation Leave
        Emergency Leave

    selectedType ← READ selected category
    RETURN selectedType
END PROCEDURE
```

### 11.3.3 Minor feature 9.3 — Parse leave duration

```text
FUNCTION PARSE_LEAVE_DAYS(daysInput)
    TRY
        days ← PARSE daysInput as integer
    CATCH parse error:
        SHOW invalid-number message
        RETURN INVALID
    END TRY

    RETURN days
END FUNCTION
```

The current GUI parses an integer but does not enforce `days >= 1` at submission time. That positive-value check occurs in the approval service. Therefore, `0` or a negative integer may be appended as a pending row and rejected later during approval/rejection processing.

### 11.3.4 Minor feature 9.4 — Capture the reason

```text
FUNCTION READ_LEAVE_REASON
    reason ← READ reason text field
    RETURN reason
END FUNCTION
```

The current form does not require a non-empty reason before appending. Reasons containing commas can also complicate the naïve comma-separated row representation; the service has a helper that joins remaining columns when reconstructing a reason.

### 11.3.5 Minor feature 9.5 — Build pending request row

```text
FUNCTION BUILD_PENDING_LEAVE_ROW(employeeNumber, leaveType, days, reason)
    RETURN [employeeNumber, leaveType, days, reason]
END FUNCTION
```

### 11.3.6 Minor feature 9.6 — Append pending request

```text
PROCEDURE APPEND_PENDING_LEAVE(requestRow)
    OPEN pendingleaves.csv FOR append
    WRITE requestRow as a comma-separated line
    CLOSE file
END PROCEDURE
```

### 11.3.7 Minor feature 9.7 — Confirm submission

```text
PROCEDURE CONFIRM_LEAVE_SUBMISSION
    SHOW successful submission message
    CLOSE LeaveForm
END PROCEDURE
```

### 11.3.8 Minor feature 9.8 — Cancel submission

```text
PROCEDURE CANCEL_LEAVE_SUBMISSION
    DO NOT append a request
    CLOSE LeaveForm
END PROCEDURE
```

### 11.3.9 Minor feature 9.9 — Validate request when it is processed

```text
PROCEDURE VALIDATE_LEAVE_REQUEST_FOR_DECISION(request)
    days ← PARSE request.days as integer

    IF days < 1 THEN
        THROW leave request must contain at least one day
    END IF

    employee ← FIND employee record using request.employeeID

    IF employee does not exist THEN
        THROW employee record not found
    END IF

    IF request is being approved AND employee.leaveCredits <= 0 THEN
        THROW employee has no leave credits available
    END IF

    RETURN employee, days
END PROCEDURE
```

---

# 12. Major Feature 10 — Leave Administration, Approval, Rejection, and Processing

## 12.1 Purpose

The leave-administration interface separates processed requests from pending requests. The `LeaveManagement` dialog displays the processed leave ledger and provides access to `LeaveApproval`, which displays pending requests. HR selects one request and approves or rejects it.

`LeaveService` performs the decision. It validates the selected row against the current pending file, verifies the employee, validates the requested number of days, updates the appropriate employee counters, writes the processed decision, removes the pending request, and attempts to restore the original file contents if any part of the transaction fails.

The service is the intended single writer for approval/rejection decisions. The employee-facing `LeaveForm` still writes new pending requests directly, however, so the request-creation path is not yet fully centralised.

## 12.2 Leave file and counter fields

| File / field | Purpose |
|---|---|
| `pendingleaves.csv` | Pending requests awaiting a decision |
| `leaves.csv` | Processed leave records, including the decision |
| `data.csv` column 21 | Employee leave-credit counter |
| `data.csv` column 22 | Approved-leave request counter |
| `data.csv` column 23 | Rejected-leave request counter |
| Pending request fields | Employee ID, leave type, days, and reason |
| Processed request fields | Employee ID, leave type, days, reason, and approval result (`Yes` / `No`) |

### Important current leave-credit rule

For every approved request, the service subtracts **one** from the leave-credit counter, regardless of the number of requested days. Rejection leaves the credit counter unchanged. The approved-request counter increases by one on approval; the rejected-request counter increases by one on rejection.

The number of days is retained in the leave files, but the current rule does not subtract one credit per day. Approved leave is also not deducted from attendance hours or used to add paid hours in the current payroll calculation. This is a documented implementation rule and limitation.

## 12.3 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 10.1 Display processed leave ledger | Load processed rows; reconstruct reason; populate processed table |
| 10.2 Open pending requests | Instantiate `LeaveApproval`; display pending queue |
| 10.3 Load pending queue | Read pending file; map employee, type, days, and reason to table columns |
| 10.4 Select a request | Require selected row; capture expected row values |
| 10.5 Approve request | Invoke `LeaveService.decide` with approval flag `TRUE` |
| 10.6 Reject request | Invoke `LeaveService.decide` with approval flag `FALSE` |
| 10.7 Guard against stale selection | Verify index still exists; compare the current row with the row shown to HR |
| 10.8 Validate request | Parse days; require at least one; find employee; verify file row shape |
| 10.9 Update approval counters | Decrease leave credits by one; increase approved count by one |
| 10.10 Update rejection counters | Keep leave credits; increase rejected count by one |
| 10.11 Build processed record | Append decision status to employee, category, days, and reason |
| 10.12 Remove pending record | Remove exactly the selected matching row |
| 10.13 Write files | Write pending file; append processed file; update employee data file |
| 10.14 Preserve file consistency | Snapshot original file bytes; use atomic file replacement where implemented; restore snapshots after failure |
| 10.15 Handle request errors | Display stale-row, invalid-request, missing-employee, no-credit, or I/O error |
| 10.16 Refresh approval queue | Reload pending rows after successful decision; clear selection |
| 10.17 Navigate between leave screens | Switch from processed ledger to pending queue; return; close dialog |
| 10.18 Preserve reason text | Rejoin trailing split columns into reason when displaying rows containing commas |

## 12.4 Pseudocode — Major Feature 10: Process a leave decision

```text
PROCEDURE PROCESS_LEAVE_DECISION(selectedIndex, expectedRequest, approved)
    pendingRows ← READ pendingleaves.csv

    IF selectedIndex is outside pendingRows THEN
        THROW request is no longer available
    END IF

    currentRequest ← pendingRows[selectedIndex]

    IF currentRequest does not match expectedRequest THEN
        THROW request changed after HR selected it
    END IF

    employee, days ← VALIDATE_LEAVE_REQUEST_FOR_DECISION(currentRequest)

    IF approved = TRUE THEN
        IF employee.leaveCredits <= 0 THEN
            THROW no leave credits remain
        END IF
        newLeaveCredits ← employee.leaveCredits - 1
        newApprovedCount ← employee.approvedLeaves + 1
        newRejectedCount ← employee.rejectedLeaves
        decisionText ← "Yes"
    ELSE
        newLeaveCredits ← employee.leaveCredits
        newApprovedCount ← employee.approvedLeaves
        newRejectedCount ← employee.rejectedLeaves + 1
        decisionText ← "No"
    END IF

    processedRow ← BUILD_PROCESSED_ROW(currentRequest, decisionText)

    CALL EXECUTE_LEAVE_TRANSACTION(
        selectedIndex,
        currentRequest,
        processedRow,
        employee,
        newLeaveCredits,
        newApprovedCount,
        newRejectedCount
    )

    RETURN SUCCESS
END PROCEDURE
```

### 12.4.1 Minor feature 10.1 — Display processed leave ledger

```text
PROCEDURE DISPLAY_PROCESSED_LEAVES
    processedRows ← LeaveService.loadProcessed()
    tableRows ← EMPTY LIST

    FOR EACH row IN processedRows:
        displayedReason ← RECONSTRUCT_REASON_FROM_ROW(row)
        displayedRow ← [employeeID, leaveType, days,
                        displayedReason, approvalStatus]
        APPEND displayedRow TO tableRows
    END FOR

    POPULATE processed-leave table with tableRows
END PROCEDURE
```

### 12.4.2 Minor feature 10.2 — Open pending leave approval

```text
PROCEDURE OPEN_PENDING_LEAVE_APPROVAL(ownerWindow)
    CREATE LeaveApproval with ownerWindow
    SET dialog to modal
    DISPLAY dialog
END PROCEDURE
```

### 12.4.3 Minor feature 10.3 — Load pending requests

```text
PROCEDURE DISPLAY_PENDING_LEAVES
    pendingRows ← LeaveService.loadPending()
    tableRows ← EMPTY LIST

    FOR EACH pendingRow IN pendingRows:
        displayedRow ← MAP pendingRow to:
            Employee ID
            Leave Type
            Days
            Reason
        APPEND displayedRow TO tableRows
    END FOR

    REPLACE pending table contents with tableRows
END PROCEDURE
```

### 12.4.4 Minor feature 10.4 — Capture the selected request

```text
FUNCTION GET_SELECTED_PENDING_REQUEST(table)
    selectedIndex ← table.selectedRow

    IF selectedIndex does not exist THEN
        SHOW select-a-request message
        RETURN NO_SELECTION
    END IF

    expectedRequest ← COPY values shown in selected row
    RETURN selectedIndex, expectedRequest
END FUNCTION
```

The GUI passes the expected row values to the service. This allows the service to detect whether another operation has changed or removed the row since HR selected it.

### 12.4.5 Minor feature 10.5 — Approve request

```text
PROCEDURE APPROVE_SELECTED_LEAVE
    selection ← GET_SELECTED_PENDING_REQUEST(table)

    IF selection = NO_SELECTION THEN
        RETURN
    END IF

    index, expectedRequest ← selection

    TRY
        LeaveService.decide(index, expectedRequest, TRUE)
        SHOW approval success message
        CALL DISPLAY_PENDING_LEAVES
        CLEAR table selection
    CATCH business-rule, stale-state, or I/O error:
        SHOW the specific failure message
    END TRY
END PROCEDURE
```

### 12.4.6 Minor feature 10.6 — Reject request

```text
PROCEDURE REJECT_SELECTED_LEAVE
    selection ← GET_SELECTED_PENDING_REQUEST(table)

    IF selection = NO_SELECTION THEN
        RETURN
    END IF

    index, expectedRequest ← selection

    TRY
        LeaveService.decide(index, expectedRequest, FALSE)
        SHOW rejection success message
        CALL DISPLAY_PENDING_LEAVES
        CLEAR table selection
    CATCH business-rule, stale-state, or I/O error:
        SHOW the specific failure message
    END TRY
END PROCEDURE
```

### 12.4.7 Minor feature 10.7 — Protect against a stale selection

```text
PROCEDURE VERIFY_SELECTED_REQUEST_IS_CURRENT(index, expectedRequest, currentRows)
    IF index < 0 OR index >= LENGTH(currentRows) THEN
        THROW selected request no longer exists
    END IF

    actualRequest ← currentRows[index]

    IF actualRequest does not equal expectedRequest THEN
        THROW pending request changed; reload the queue
    END IF

    RETURN actualRequest
END PROCEDURE
```

### 12.4.8 Minor feature 10.8 — Validate request and employee record

```text
FUNCTION VALIDATE_REQUEST_AND_FIND_EMPLOYEE(request, employeeRows)
    days ← PARSE request.days as integer

    IF days < 1 THEN
        THROW requested days must be at least one
    END IF

    employee ← FIND row where employee ID equals request.employeeID

    IF employee does not exist THEN
        THROW employee record not found
    END IF

    CALL ENSURE_UNIFORM_EMPLOYEE_COLUMNS(employeeRows)

    leaveCredits ← PARSE employee[21]
    approvedCount ← PARSE employee[22]
    rejectedCount ← PARSE employee[23]

    RETURN employee, days, leaveCredits, approvedCount, rejectedCount
END FUNCTION
```

### 12.4.9 Minor feature 10.9 — Update counters for an approved request

```text
PROCEDURE APPLY_APPROVAL_COUNTERS(employee, days)
    IF employee.leaveCredits <= 0 THEN
        THROW no leave credits available
    END IF

    employee.leaveCredits ← employee.leaveCredits - 1
    employee.approvedLeaves ← employee.approvedLeaves + 1
    RETURN employee
END PROCEDURE
```

The parameter `days` is retained in the request and processed ledger, but the current implementation's credit rule subtracts one request credit, not `days` credits.

### 12.4.10 Minor feature 10.10 — Update counters for a rejected request

```text
PROCEDURE APPLY_REJECTION_COUNTERS(employee)
    employee.leaveCredits ← employee.leaveCredits
    employee.approvedLeaves ← employee.approvedLeaves
    employee.rejectedLeaves ← employee.rejectedLeaves + 1
    RETURN employee
END PROCEDURE
```

### 12.4.11 Minor feature 10.11 — Build processed leave row

```text
FUNCTION BUILD_PROCESSED_ROW(request, approved)
    IF approved THEN
        decision ← "Yes"
    ELSE
        decision ← "No"
    END IF

    RETURN [request.employeeID,
            request.leaveType,
            request.days,
            request.reason,
            decision]
END FUNCTION
```

### 12.4.12 Minor feature 10.12 — Remove exactly one pending row

```text
FUNCTION REMOVE_PENDING_REQUEST(pendingRows, index, expectedRequest)
    CALL VERIFY_SELECTED_REQUEST_IS_CURRENT(index,
                                             expectedRequest,
                                             pendingRows)
    REMOVE pendingRows[index]
    RETURN pendingRows
END FUNCTION
```

### 12.4.13 Minor feature 10.13 — Execute the multi-file transaction

```text
PROCEDURE EXECUTE_LEAVE_TRANSACTION(index, request, processedRow,
                                   employee, newCredits,
                                   newApproved, newRejected)
    originalPendingBytes ← READ ALL BYTES from pendingleaves.csv
    originalProcessedBytes ← READ ALL BYTES from leaves.csv
    originalEmployeeBytes ← READ ALL BYTES from data.csv

    TRY
        pendingRows ← PARSE originalPendingBytes
        processedRows ← PARSE originalProcessedBytes
        employeeRows ← PARSE originalEmployeeBytes

        pendingRows ← REMOVE_PENDING_REQUEST(pendingRows, index, request)
        UPDATE employee row counters using newCredits, newApproved, newRejected
        APPEND processedRow to processedRows

        WRITE pendingRows using atomic replacement where supported
        WRITE processedRows using the preserved file format
        WRITE employeeRows using atomic replacement where supported

    CATCH I/O or runtime failure:
        RESTORE pendingleaves.csv from originalPendingBytes
        RESTORE leaves.csv from originalProcessedBytes
        RESTORE data.csv from originalEmployeeBytes
        RAISE original failure
    END TRY
END PROCEDURE
```

The service takes byte snapshots of the three files and restores them if the write sequence fails. Its atomic replacement helper writes to a temporary file and moves the replacement into place where supported. This is application-level rollback across CSV files, not a database transaction managed by a database server.

### 12.4.14 Minor feature 10.14 — Preserve the original files on failure

```text
PROCEDURE RESTORE_LEAVE_FILES(pendingBytes, processedBytes, employeeBytes)
    WRITE pendingBytes back to pendingleaves.csv
    WRITE processedBytes back to leaves.csv
    WRITE employeeBytes back to data.csv
END PROCEDURE
```

### 12.4.15 Minor feature 10.15 — Handle leave-processing errors

```text
PROCEDURE HANDLE_LEAVE_DECISION_ERROR(error)
    IF error means no row selected THEN
        SHOW selection instruction
    ELSE IF error means stale or removed request THEN
        SHOW reload-required message
    ELSE IF error means invalid number of days THEN
        SHOW positive-days error
    ELSE IF error means employee not found THEN
        SHOW missing-employee error
    ELSE IF error means no leave credits THEN
        SHOW no-credit error
    ELSE IF error is an I/O failure THEN
        SHOW file update failure and state that rollback was attempted
    ELSE
        SHOW general leave-processing error
    END IF
END PROCEDURE
```

### 12.4.16 Minor feature 10.16 — Refresh the pending queue

```text
PROCEDURE REFRESH_PENDING_QUEUE
    CLEAR pending table rows
    CALL DISPLAY_PENDING_LEAVES
    CLEAR selected row
END PROCEDURE
```

### 12.4.17 Minor feature 10.17 — Navigate between processed and pending leave screens

```text
PROCEDURE OPEN_PENDING_FROM_PROCESSED_SCREEN
    CLOSE or hide processed leave dialog as required by the owner flow
    CREATE LeaveApproval dialog
    DISPLAY pending request queue
END PROCEDURE

PROCEDURE RETURN_TO_PROCESSED_SCREEN
    CLOSE LeaveApproval dialog
    REFRESH processed leave ledger
    DISPLAY LeaveManagement dialog
END PROCEDURE
```

### 12.4.18 Minor feature 10.18 — Reconstruct a reason containing commas

```text
FUNCTION RECONSTRUCT_REASON_FROM_ROW(row)
    IF row has the expected number of columns THEN
        RETURN reason column
    ELSE
        reason ← JOIN every column between reason start and approval status
                  using comma
        RETURN reason
    END IF
END FUNCTION
```

This compensates for some reason text being split into multiple cells by the simple comma parser. It does not make the underlying CSV format fully quote-aware.

---

# 13. Major Feature 11 — Employee Credential Maintenance

## 13.1 Purpose

The employee dashboard provides a credential-change dialog. The employee can enter a new username and password, show or conceal the password field, apply the changes, or cancel. The event handler trims the entered text, rejects empty values, finds the employee row, updates columns 19 and 20, and writes the employee data file.

The current implementation does not ask for the old password, does not check username uniqueness, does not enforce a password-strength rule, and stores the new password in plaintext. The README explicitly says that the OOP design's intended hashed credential storage was not migrated.

## 13.2 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 11.1 Open credential dialog | Receive employee number; create modal dialog; display form |
| 11.2 Enter new username | Read username field; trim whitespace |
| 11.3 Enter new password | Read password field; trim whitespace |
| 11.4 Show/hide password | Toggle password-field echo character |
| 11.5 Validate new values | Require both values to be non-empty |
| 11.6 Locate employee row | Read `data.csv`; match employee number |
| 11.7 Update credentials | Replace username at index 19; replace password at index 20 |
| 11.8 Persist credentials | Rewrite employee CSV with updated row |
| 11.9 Report result | Show success or I/O error; clear fields after success |
| 11.10 Cancel update | Close dialog without writing |
| 11.11 Security limitations | Plaintext storage; no old-password verification; no uniqueness check; no strength validation; CSV parsing limitations |

## 13.3 Pseudocode — Major Feature 11: Change employee credentials

```text
PROCEDURE CHANGE_EMPLOYEE_CREDENTIALS(employeeNumber)
    newUsername ← TRIM(READ username field)
    newPassword ← TRIM(READ password field)

    IF newUsername is empty OR newPassword is empty THEN
        SHOW required-fields error
        RETURN
    END IF

    rows ← READ all rows from data.csv
    employeeRow ← FIND row where column[0] = employeeNumber

    IF employeeRow does not exist THEN
        SHOW employee-record-not-found error
        RETURN
    END IF

    employeeRow[19] ← newUsername
    employeeRow[20] ← newPassword

    TRY
        REWRITE data.csv with updated rows
        SHOW credentials-updated confirmation
        CLEAR username and password fields
    CATCH I/O error:
        SHOW credential-update failure
    END TRY
END PROCEDURE
```

### 13.3.1 Minor feature 11.1 — Open credential dialog

```text
PROCEDURE OPEN_CREDENTIAL_DIALOG(employeeNumber)
    CREATE ChangeCredentials(employeeNumber)
    SET dialog modality to application-modal
    DISPLAY dialog
END PROCEDURE
```

### 13.3.2 Minor feature 11.2 — Read and trim username

```text
FUNCTION READ_NEW_USERNAME
    username ← TRIM(username text field)
    RETURN username
END FUNCTION
```

### 13.3.3 Minor feature 11.3 — Read and trim password

```text
FUNCTION READ_NEW_PASSWORD
    password ← TRIM(password field)
    RETURN password
END FUNCTION
```

### 13.3.4 Minor feature 11.4 — Toggle password visibility

```text
PROCEDURE TOGGLE_NEW_PASSWORD_VISIBILITY
    IF Show Password control is selected THEN
        DISPLAY password characters
    ELSE
        CONCEAL password characters
    END IF
END PROCEDURE
```

### 13.3.5 Minor feature 11.5 — Validate credential values

```text
FUNCTION VALIDATE_NEW_CREDENTIALS(username, password)
    IF username is empty THEN
        SHOW username-required error
        RETURN FALSE
    END IF

    IF password is empty THEN
        SHOW password-required error
        RETURN FALSE
    END IF

    RETURN TRUE
END FUNCTION
```

No further rule should be attributed to this function. In particular, the current code does not implement a password-strength policy or a uniqueness test for the username.

### 13.3.6 Minor feature 11.6 — Locate employee by ID

```text
FUNCTION FIND_EMPLOYEE_ROW_IN_DATA_FILE(employeeNumber, rows)
    FOR EACH row IN rows:
        IF row[0] = employeeNumber THEN
            RETURN row
        END IF
    END FOR

    RETURN NOT_FOUND
END FUNCTION
```

### 13.3.7 Minor feature 11.7 — Replace username and password

```text
PROCEDURE APPLY_CREDENTIAL_CHANGES(employeeRow, username, password)
    employeeRow[19] ← username
    employeeRow[20] ← password
END PROCEDURE
```

### 13.3.8 Minor feature 11.8 — Rewrite employee file

```text
PROCEDURE SAVE_CREDENTIAL_CHANGES(allRows)
    OPEN data.csv for replacement

    FOR EACH row IN allRows:
        WRITE row as comma-separated values
    END FOR

    CLOSE file
END PROCEDURE
```

### 13.3.9 Minor feature 11.9 — Report credential update result

```text
PROCEDURE REPORT_CREDENTIAL_UPDATE(success)
    IF success THEN
        SHOW success message
        CLEAR username field
        CLEAR password field
    ELSE
        SHOW file-write or update error
    END IF
END PROCEDURE
```

### 13.3.10 Minor feature 11.10 — Cancel credential update

```text
PROCEDURE CANCEL_CREDENTIAL_UPDATE
    DO NOT modify data.csv
    CLOSE credential dialog
END PROCEDURE
```

### 13.3.11 Minor feature 11.11 — Record credential security limitation

```text
PROCEDURE CURRENT_PASSWORD_STORAGE_BEHAVIOUR
    STORE password as ordinary text in data.csv
    COMPARE entered password directly to stored text at login
    DO NOT perform password hashing in this code path
    DO NOT verify the old password before replacing it
END PROCEDURE
```

---

# 14. Major Feature 12 — CSV Persistence, File Services, Validation, and Quality Assurance

## 14.1 Purpose

The application stores its business data in CSV files rather than a relational database. The most important files are employee data, processed leave, pending leave, HR credentials, and attendance. `AttendanceRepository` provides a dedicated read and filtering service for timecards. `LeaveService` centralises approval/rejection writes and attempts to keep three CSV files consistent if a decision fails.

The backend also has a console test harness and supporting test outputs. These are development and quality-assurance functions, not end-user screens, but they are part of the repository's actual feature set and evidence of backend behaviour.

## 14.2 Files and their purposes

| File | Main content | Main readers/writers |
|---|---|---|
| `data.csv` | Employee profile, government numbers, employment status, salary/allowances, username/password, and leave counters | `MainMenu`, `Dashboard`, `Login`, `Form`, `ChangeCredentials`, `LeaveService` |
| `pendingleaves.csv` | Leave applications awaiting HR decisions | `LeaveForm`, `LeaveService`, `LeaveApproval` |
| `leaves.csv` | Processed leave requests and approval/rejection result | `LeaveService`, `LeaveManagement` |
| `HRLogin.csv` | HR usernames and passwords | `LoginHR` |
| `attendance.csv` | Employee-number/date/clock-in/clock-out rows | `AttendanceRepository` |
| `oop/console-test-output.txt` | Console backend test output | Backend QA/evidence |
| `oop/cp2-parity-output.txt` | Earlier CP2 calculation outputs used for parity comparison | Payroll QA/evidence |
| `oop/parity-report.txt` | Side-by-side payroll/deduction comparisons for salary inputs | Payroll QA/evidence |
| `oop/matrix-verification.md` | Contribution calculator verification notes | Statutory-calculator QA/evidence |
| `oop/attendance-test-output.txt` | Attendance import and validation test output | Attendance QA/evidence |
| `oop/leave-fix-test-output.txt` | Leave approval/rejection test output | Leave QA/evidence |
| `oop/gui-retest-output.txt` | GUI approval/rejection retest evidence | GUI QA/evidence |
| `oop/ki20-screen-fit-output.txt` | Add Employee screen fit test output | UI QA/evidence |

## 14.3 Minor features and sub-functions

| Minor feature | Sub-functions |
|---|---|
| 12.1 Employee CSV loading | Resolve path; open file; read header; parse rows; add rows to table |
| 12.2 Employee CSV saving | Open file in replacement mode; write header; write each table row; close file |
| 12.3 Employee CSV append | Append a newly created employee record |
| 12.4 Pending leave append | Append a new pending request |
| 12.5 Attendance CSV parsing | Header handling; four-column validation; integer/date/time parsing; diagnostic line numbers |
| 12.6 Attendance record cache | Load once; reuse records; filter cached data |
| 12.7 Leave file reads | Load pending and processed rows; map values for UI |
| 12.8 Leave multi-file snapshot | Capture original bytes of pending, processed, and employee files |
| 12.9 Leave atomic write | Write temporary file; replace destination where supported |
| 12.10 Leave rollback | Restore all captured file contents after failure |
| 12.11 CSV row consistency check | Ensure employee rows have uniform column counts before updating counters |
| 12.12 Numeric parsing and validation | Convert IDs, salaries, allowances, days, rates, and counters; catch invalid values |
| 12.13 Error reporting | Show user-facing errors; propagate repository/service failures to callers |
| 12.14 Console test harness | Run backend cases; count pass/fail; report results |
| 12.15 Attendance test suite | Verify imported row count, employee count, filtering, date handling, and parsing |
| 12.16 Leave test suite | Verify approval, rejection, counter changes, row movement, and failure rollback |
| 12.17 Payroll parity comparison | Compare gross pay with CP2 results across employee-month test cases |
| 12.18 Compiler verification | Compile with `javac -Xlint:all`; capture warnings and errors |
| 12.19 GUI smoke testing | Confirm the listed screen and workflow checks |

## 14.4 Pseudocode — Major Feature 12: Persist and verify system data

```text
PROCEDURE APPLICATION_DATA_OPERATIONS
    WHEN employee records are needed:
        CALL LOAD_EMPLOYEE_RECORDS

    WHEN HR saves employee table changes:
        CALL SAVE_EMPLOYEE_RECORDS

    WHEN a new employee is created:
        CALL APPEND_EMPLOYEE_RECORD

    WHEN attendance is needed:
        CALL LOAD_ATTENDANCE_RECORDS

    WHEN an employee submits leave:
        CALL APPEND_PENDING_LEAVE

    WHEN HR decides a leave request:
        CALL EXECUTE_LEAVE_TRANSACTION

    WHEN backend verification is requested:
        CALL RUN_CONSOLE_BACKEND_TESTS
        CALL RUN_ATTENDANCE_TESTS
        CALL RUN_LEAVE_TESTS
        CALL RUN_PAYROLL_PARITY_CHECKS
        CALL RUN_COMPILER_CHECK
END PROCEDURE
```

### 14.4.1 Minor feature 12.1 — Read employee CSV

```text
FUNCTION READ_EMPLOYEE_CSV
    OPEN data.csv FOR reading
    header ← READ first row
    rows ← EMPTY LIST

    WHILE file has another row:
        line ← READ row
        fields ← SPLIT line by comma
        APPEND fields TO rows
    END WHILE

    CLOSE file
    RETURN header, rows
END FUNCTION
```

### 14.4.2 Minor feature 12.2 — Write employee CSV

```text
PROCEDURE WRITE_EMPLOYEE_CSV(header, rows)
    OPEN data.csv FOR replacement
    WRITE header

    FOR EACH row IN rows:
        WRITE row in the existing column order
    END FOR

    CLOSE data.csv
END PROCEDURE
```

### 14.4.3 Minor feature 12.3 — Append employee CSV row

```text
PROCEDURE APPEND_EMPLOYEE_RECORD(employeeRow)
    OPEN data.csv FOR append
    WRITE employeeRow as one record
    CLOSE file
END PROCEDURE
```

### 14.4.4 Minor feature 12.4 — Append pending leave request

```text
PROCEDURE WRITE_PENDING_LEAVE(requestRow)
    OPEN pendingleaves.csv FOR append
    WRITE requestRow
    CLOSE file
END PROCEDURE
```

### 14.4.5 Minor feature 12.11 — Validate CSV row width

```text
PROCEDURE ENSURE_UNIFORM_EMPLOYEE_COLUMNS(employeeRows)
    expectedColumnCount ← number of columns in the valid employee record

    FOR EACH row IN employeeRows:
        IF LENGTH(row) != expectedColumnCount THEN
            THROW inconsistent employee row width
        END IF
    END FOR
END PROCEDURE
```

The leave service uses a consistency check before rewriting the employee file. The legacy GUI's general employee CSV parser does not provide the same full row-shape guarantee at every read site.

### 14.4.6 Minor feature 12.12 — Parse numeric values

```text
FUNCTION PARSE_REQUIRED_NUMBER(textValue, fieldName)
    TRY
        value ← PARSE textValue as number
    CATCH conversion error:
        THROW invalid numeric value for fieldName
    END TRY

    RETURN value
END FUNCTION
```

### 14.4.7 Minor feature 12.13 — Report read/write failure

```text
PROCEDURE HANDLE_FILE_FAILURE(operationName, error)
    LOG or retain diagnostic details available to the caller
    SHOW a concise user-facing message naming operationName
    DO NOT report success
    PROPAGATE failure when caller must stop the workflow
END PROCEDURE
```

### 14.4.8 Minor feature 12.8 — Snapshot related leave files

```text
FUNCTION SNAPSHOT_LEAVE_FILES
    pendingSnapshot ← READ ALL BYTES from pendingleaves.csv
    processedSnapshot ← READ ALL BYTES from leaves.csv
    employeeSnapshot ← READ ALL BYTES from data.csv

    RETURN pendingSnapshot, processedSnapshot, employeeSnapshot
END FUNCTION
```

### 14.4.9 Minor feature 12.9 — Write a replacement file atomically

```text
PROCEDURE ATOMIC_REPLACE(destination, newContents)
    temporaryFile ← CREATE temporary file beside destination
    WRITE newContents to temporaryFile
    CLOSE temporaryFile
    MOVE temporaryFile over destination using atomic move where supported
END PROCEDURE
```

### 14.4.10 Minor feature 12.10 — Restore data on failure

```text
PROCEDURE ROLLBACK_FILE_CHANGES(snapshots)
    WRITE snapshots.pending TO pendingleaves.csv
    WRITE snapshots.processed TO leaves.csv
    WRITE snapshots.employee TO data.csv
END PROCEDURE
```

### 14.4.11 Minor feature 12.14 — Run backend console tests

```text
PROCEDURE RUN_CONSOLE_BACKEND_TESTS
    INITIALISE passedCount ← 0
    INITIALISE failedCount ← 0

    FOR EACH backend test case:
        RUN test case

        IF assertion passes THEN
            passedCount ← passedCount + 1
        ELSE
            failedCount ← failedCount + 1
            RECORD test name and failure
        END IF
    END FOR

    PRINT passedCount and failedCount
    RETURN test result
END PROCEDURE
```

### 14.4.12 Minor feature 12.17 — Verify payroll parity

```text
PROCEDURE VERIFY_GROSS_PAY_PARITY(cp2Results, oopResults)
    matched ← 0
    mismatched ← 0

    FOR EACH employee-month pair in comparison set:
        oldGross ← cp2Results[pair]
        newGross ← oopResults[pair]

        IF ROUND(oldGross, 2) = ROUND(newGross, 2) THEN
            matched ← matched + 1
        ELSE
            mismatched ← mismatched + 1
            RECORD employee-month and both values
        END IF
    END FOR

    REPORT matched and mismatched
END PROCEDURE
```

### 14.4.13 Minor feature 12.18 — Compile the backend

```text
PROCEDURE VERIFY_COMPILATION
    RUN Java compiler against oop/src and test source roots
    ENABLE all lint warnings

    IF compiler reports errors THEN
        RECORD compilation failure
    ELSE
        RECORD successful compilation
    END IF

    REPORT warning and error counts
END PROCEDURE
```

### 14.4.14 Minor feature 12.19 — Run GUI smoke tests

```text
PROCEDURE RUN_GUI_SMOKE_TESTS(checklist)
    FOR EACH check IN checklist:
        EXECUTE the documented user interaction
        OBSERVE expected screen or result
        RECORD Yes if observed, otherwise No
    END FOR

    REPORT completed checks and failures
END PROCEDURE
```

---

# PART II — CROSS-FEATURE WORKFLOWS

The preceding sections give each major feature and its minor operations their own pseudocode. The following combined workflows show how the features connect during normal application use. These are useful when tracing a complete operation across multiple classes.

## 15. End-to-end workflow A — Employee logs in and views salary

```text
PROCEDURE EMPLOYEE_LOGIN_TO_PAYROLL
    CALL START_APPLICATION
    CALL OPEN_LANDING_PAGE
    CALL OPEN_EMPLOYEE_LOGIN

    credentials ← COLLECT_EMPLOYEE_LOGIN_INPUTS
    IF credentials are invalid THEN
        RETURN to login screen
    END IF

    IF VERIFY_EMPLOYEE_CREDENTIALS(credentials) = FALSE THEN
        SHOW invalid credentials
        RETURN to login screen
    END IF

    employeeNumber ← credentials.employeeNumber
    CALL OPEN_EMPLOYEE_DASHBOARD(employeeNumber)

    employeeRecord ← FIND_EMPLOYEE_RECORD(employeeNumber)
    IF employeeRecord is not found THEN
        SHOW employee record error
        STOP salary flow
    END IF

    CALL DISPLAY_EMPLOYEE_PROFILE(employeeRecord)
    CALL DISPLAY_EMPLOYEE_LEAVE_COUNTERS(employeeRecord)

    WHEN employee selects View Salary:
        months ← AttendanceRepository.monthsWithRecords(employeeNumber)

        IF months is empty THEN
            SHOW no attendance available
            RETURN to dashboard
        END IF

        selectedMonth ← employee selects one available month
        attendance ← AttendanceRepository.loadFor(employeeNumber, selectedMonth)
        employee ← BUILD_EMPLOYEE_OBJECT_FROM_ROW(employeeRecord)

        payroll ← PayrollCalculator.calculatePayroll(
                      employee,
                      attendance,
                      first day of selectedMonth,
                      last day of selectedMonth
                  )

        DISPLAY_EMPLOYEE_PAYROLL_RESULT(payroll)
END PROCEDURE
```

## 16. End-to-end workflow B — HR adds an employee, then saves the record

```text
PROCEDURE HR_ADD_EMPLOYEE_WORKFLOW
    CALL HR_LOGIN

    IF authentication fails THEN
        DO NOT open MainMenu
        RETURN
    END IF

    CALL LOAD_EMPLOYEE_RECORDS
    DISPLAY employee table

    HR selects Add Employee
    CALL OPEN_ADD_EMPLOYEE_FORM

    formValues ← READ form fields

    IF VALIDATE_NEW_EMPLOYEE_INPUT(formValues) = INVALID THEN
        SHOW validation feedback
        KEEP form open
        RETURN
    END IF

    employeeRow ← BUILD 24-column employee row
    APPEND employeeRow TO data.csv

    CLOSE form
    CALL REFRESH_EMPLOYEE_RECORDS
    DISPLAY updated employee table
END PROCEDURE
```

**Persistence distinction:** Add Employee appends to the CSV directly. Update and Delete modify the table model and require the separate Save action to persist. Do not conflate those three write patterns.

## 17. End-to-end workflow C — HR updates and deletes an employee

```text
PROCEDURE HR_UPDATE_DELETE_WORKFLOW
    CALL LOAD_EMPLOYEE_RECORDS

    HR selects employee row
    CALL POPULATE_SELECTED_EMPLOYEE

    IF HR selects Update THEN
        CALL UPDATE_EMPLOYEE
        NOTE table model is changed, not yet persisted
    ELSE IF HR selects Delete THEN
        CALL DELETE_EMPLOYEE
        NOTE table model is changed, not yet persisted
    END IF

    IF HR selects Save THEN
        CALL SAVE_EMPLOYEE_RECORDS
    ELSE IF HR selects Refresh THEN
        RELOAD data.csv
        DISCARD unsaved table-model changes
    END IF
END PROCEDURE
```

## 18. End-to-end workflow D — HR calculates payroll

```text
PROCEDURE HR_PAYROLL_WORKFLOW
    selectedEmployee ← SELECT_EMPLOYEE_FOR_PAYROLL

    IF selectedEmployee is absent THEN
        RETURN
    END IF

    months ← AttendanceRepository.monthsWithRecords(selectedEmployee.id)

    IF months is empty THEN
        SHOW attendance-not-found message
        RETURN
    END IF

    month ← PROMPT HR to select month
    IF month is cancelled THEN
        RETURN
    END IF

    periodStart ← first date of month
    periodEnd ← last date of month
    log ← AttendanceRepository.loadFor(selectedEmployee.id, month)
    employee ← BUILD_EMPLOYEE_OBJECT_FROM_ROW(selectedEmployee.row)

    payroll ← PayrollCalculator.calculatePayroll(
                  employee, log, periodStart, periodEnd
              )

    DISPLAY_HR_PAYROLL_RESULT(employee, payroll)
END PROCEDURE
```

## 19. End-to-end workflow E — Employee submits leave and HR decides it

```text
PROCEDURE LEAVE_REQUEST_LIFECYCLE
    employeeNumber ← current logged-in employee
    CALL OPEN_LEAVE_APPLICATION(employeeNumber)

    request ← employee enters category, days, and reason
    CALL SUBMIT_LEAVE_APPLICATION(employeeNumber)

    request is appended to pendingleaves.csv

    HR opens LeaveManagement
    CALL DISPLAY_PROCESSED_LEAVES

    HR opens pending queue
    CALL DISPLAY_PENDING_LEAVES

    HR selects a pending request

    IF HR selects Approve THEN
        CALL PROCESS_LEAVE_DECISION(index, request, TRUE)
    ELSE IF HR selects Reject THEN
        CALL PROCESS_LEAVE_DECISION(index, request, FALSE)
    END IF

    IF decision succeeds THEN
        pending request is removed
        processed decision is added to leaves.csv
        employee leave counters are updated in data.csv
        pending queue is refreshed
    ELSE
        original file snapshots are restored when the transaction fails
        error is reported to HR
    END IF
END PROCEDURE
```

## 20. End-to-end workflow F — Payroll calculation internal call sequence

```text
PROCEDURE PAYROLL_INTERNAL_SEQUENCE(employee, attendance, startDate, endDate)
    VALIDATE employee and period
    workedDays ← COUNT complete attendance pairs in period
    workedHours ← workedDays × 8
    gross ← workedDays × employee.hourlyRate × 8
    REQUIRE gross > 0

    sss ← SSSCalculator.calculate(gross)
    philHealth ← PhilHealthCalculator.calculate(gross)
    pagIbig ← PagIbigCalculator.calculate(gross)

    statutoryTotal ← sss + philHealth + pagIbig
    REQUIRE statutoryTotal < gross

    taxableBase ← gross - statutoryTotal
    withholdingTax ← WithholdingTaxCalculator.calculate(taxableBase)

    deductions ← statutoryTotal + withholdingTax
    net ← gross - deductions

    CREATE PayrollRecord with all supported result fields
    VALIDATE net = gross - deductions within 0.01
    RETURN immutable record
END PROCEDURE
```

---

# PART III — COMPLETE SUB-FUNCTION REGISTER

The following register consolidates the sub-functions distributed throughout the feature sections. It is intended as a checklist for an agent that needs to trace or discuss the application without reopening every source file.

## 21. Startup and navigation register

1. Initialise loading screen.
2. Display loading screen.
3. End loading transition.
4. Open landing page.
5. Display employee access option.
6. Display HR access option.
7. Open employee login.
8. Open HR login.
9. Return from employee login to landing page.
10. Return from HR login to landing page.
11. Confirm logout.
12. Close employee dashboard.
13. Close HR main menu.
14. Reopen login entry route.
15. Exit application.
16. Create and refresh the date label.
17. Create and refresh the time label.
18. Open modal child dialog.
19. Return control to dialog owner.
20. Dispose child dialog without terminating parent application.

## 22. Authentication register

1. Read employee number.
2. Read employee username.
3. Read employee password.
4. Validate employee login fields are not blank.
5. Open `data.csv`.
6. Read employee rows.
7. Match employee number at column 0.
8. Match employee username at column 19.
9. Match employee password at column 20.
10. Open `Dashboard` on successful employee login.
11. Display employee login error on failure.
12. Read HR username.
13. Read HR password.
14. Validate HR login fields are not blank.
15. Open `HRLogin.csv`.
16. Compare HR username and password.
17. Open `MainMenu` on successful HR login.
18. Display HR login error on failure.
19. Toggle employee password visibility.
20. Toggle HR password visibility.
21. Toggle new-password visibility in the credential dialog.
22. Close current screen on logout confirmation.
23. Preserve current screen if logout is cancelled.
24. Handle current unconditional HR login-window disposal after failed authentication.
25. Recognise plaintext comparison and storage.

## 23. HR employee-administration register

1. Resolve employee CSV path.
2. Open employee CSV.
3. Read table header.
4. Parse employee rows.
5. Populate employee table model.
6. Hide configured columns from the displayed table.
7. Select employee row.
8. Map selected row to employee-detail fields.
9. View employee information in a dialog.
10. Open Add Employee form.
11. Read Add Employee fields.
12. Reject missing required fields.
13. Parse employee ID.
14. Parse basic salary.
15. Parse rice subsidy.
16. Parse phone allowance.
17. Parse clothing allowance.
18. Parse gross semi-monthly rate.
19. Parse hourly rate.
20. Check employee ID uniqueness.
21. Build 24-column employee record.
22. Add default username.
23. Add default password.
24. Add default leave credits.
25. Initialise approved-leave count.
26. Initialise rejected-leave count.
27. Append employee record.
28. Display Add Employee result.
29. Update selected employee row.
30. Validate numeric update fields.
31. Check updated employee ID uniqueness.
32. Delete selected row after confirmation.
33. Save table header and rows.
34. Refresh employee rows from disk.
35. Clear employee detail fields.
36. Open LeaveManagement dialog.
37. Fit Add Employee form to usable display bounds.
38. Add scrolling when form content exceeds available space.
39. Bind Escape to form closure.
40. Keep Update/Delete changes in memory until Save.
41. Recognise Refresh as a discard of unsaved model changes.

## 24. Employee dashboard register

1. Receive employee number from login.
2. Locate matching employee row.
3. Read employee name.
4. Read birthday.
5. Read address.
6. Read phone number.
7. Read SSS number.
8. Read PhilHealth number.
9. Read TIN.
10. Read Pag-IBIG number.
11. Read employment status.
12. Read position.
13. Read supervisor.
14. Read salary and allowance values.
15. Read leave credits.
16. Read approved leave count.
17. Read rejected leave count.
18. Display profile.
19. Display leave counters.
20. Open salary view.
21. Open LeaveForm.
22. Display contact-HR message for profile edits.
23. Open ChangeCredentials.
24. Confirm employee logout.
25. Refresh date and time.

## 25. Attendance register

1. Locate `attendance.csv`.
2. Verify file exists.
3. Read attendance header.
4. Read attendance line.
5. Split line into four fields.
6. Validate four-column row length.
7. Parse employee number.
8. Parse ISO date.
9. Parse clock-in time.
10. Parse clock-out time.
11. Report invalid line with line number.
12. Cache parsed attendance rows.
13. List months by employee.
14. Sort month list chronologically.
15. Check whether employee has attendance.
16. Filter rows for employee and month.
17. Derive month start date.
18. Derive month end date.
19. Label selected month.
20. Create AttendanceLog.
21. Record clock-in.
22. Reject invalid employee ID.
23. Reject future clock-in.
24. Parse supported time format.
25. Reject duplicate clock-in.
26. Record clock-out.
27. Require existing clock-in.
28. Reject duplicate clock-out.
29. Require clock-out after clock-in.
30. Reject duration above 24 hours.
31. Calculate daily duration.
32. Return zero for incomplete attendance.
33. Count complete worked days within an inclusive period.

## 26. Employee OOP model register

1. Construct Person.
2. Validate first name.
3. Validate last name.
4. Validate address.
5. Parse birthday.
6. Reject future birthday.
7. Validate phone number.
8. Get full name.
9. Calculate age.
10. Update address.
11. Update phone number.
12. Construct Employee.
13. Validate positive employee ID.
14. Validate government identifiers.
15. Validate status.
16. Validate position.
17. Validate supervisor.
18. Validate basic salary.
19. Validate gross semi-monthly rate.
20. Validate hourly rate.
21. Validate clothing allowance.
22. Validate supplied rice subsidy and phone allowance.
23. Get subclass rice subsidy.
24. Get subclass phone allowance.
25. Get annual leave-credit entitlement.
26. Check promotion eligibility.
27. Construct RegularEmployee.
28. Validate years of service.
29. Construct ProbationaryEmployee.
30. Validate probation duration.
31. Validate probation start date.
32. Validate evaluation score.
33. Dispatch overridden methods polymorphically.

## 27. Payroll and statutory-calculator register

1. Construct PayrollCalculator.
2. Require calculator dependencies.
3. Validate employee argument.
4. Validate attendance log argument.
5. Validate period dates.
6. Reject reversed period.
7. Count worked days.
8. Calculate paid-hour count using eight hours per worked day.
9. Calculate gross pay.
10. Reject zero or negative gross.
11. Iterate active contribution calculators.
12. Skip inactive calculators.
13. Calculate SSS.
14. Calculate PhilHealth.
15. Calculate Pag-IBIG.
16. Sum statutory contributions.
17. Reject statutory total at or above gross.
18. Calculate taxable amount.
19. Calculate withholding tax.
20. Sum total deductions.
21. Calculate net pay.
22. Build immutable PayrollRecord.
23. Validate record employee ID.
24. Validate record period.
25. Validate non-negative amounts.
26. Validate net-pay arithmetic within tolerance.
27. Return record.
28. Display HR payroll result.
29. Display employee salary result.
30. Use recent 15-day backend overload when called without explicit dates.
31. Apply SSS below-table value.
32. Select SSS band.
33. Apply SSS maximum.
34. Apply inclusive SSS lower bound.
35. Calculate PhilHealth premium.
36. Apply PhilHealth minimum.
37. Apply PhilHealth maximum.
38. Return employee half of PhilHealth premium.
39. Select Pag-IBIG employee rate.
40. Apply Pag-IBIG cap.
41. Return zero below the implemented matrix floor.
42. Select withholding-tax band.
43. Apply withholding-tax base tax.
44. Apply marginal withholding-tax rate.
45. Handle tax-free threshold and ₱20,833 lower-bound correction.
46. Enable/disable a calculator.
47. Validate salary input.
48. Verify calculator outputs against test cases.

## 28. Leave application and administration register

1. Open LeaveForm for logged-in employee.
2. Capture leave type.
3. Capture days.
4. Parse integer days.
5. Capture reason.
6. Build pending leave row.
7. Append pending row.
8. Display submission confirmation.
9. Cancel form without writing.
10. Load processed leave ledger.
11. Reconstruct reason text.
12. Open pending leave screen.
13. Load pending records.
14. Populate pending table.
15. Require selected request.
16. Capture expected selected row.
17. Validate selected index.
18. Compare live row with expected row.
19. Parse days during decision processing.
20. Reject days below one.
21. Find employee by employee number.
22. Validate employee row width.
23. Read leave credits.
24. Read approved counter.
25. Read rejected counter.
26. Require leave credit for approval.
27. Decrease leave credit by one on approval.
28. Increase approved counter by one.
29. Preserve credits on rejection.
30. Increase rejected counter by one.
31. Build processed row with Yes/No decision.
32. Snapshot `pendingleaves.csv` bytes.
33. Snapshot `leaves.csv` bytes.
34. Snapshot `data.csv` bytes.
35. Remove pending row.
36. Append processed row.
37. Update employee counters.
38. Write files.
39. Use temporary-file replacement where supported.
40. Roll back all three files on failure.
41. Refresh pending table.
42. Clear selected row after successful decision.
43. Display stale-row error.
44. Display missing-employee error.
45. Display no-credit error.
46. Display file-write error.
47. Return from pending to processed view.
48. Close leave dialog.
49. Preserve the current one-credit-per-approved-request rule.
50. Preserve the fact that approved leave does not alter payroll attendance hours or gross pay.

## 29. Credential-maintenance register

1. Open ChangeCredentials dialog.
2. Read new username.
3. Trim username whitespace.
4. Read new password.
5. Trim password whitespace.
6. Toggle password visibility.
7. Reject empty username.
8. Reject empty password.
9. Read employee data rows.
10. Locate employee row.
11. Replace username at column 19.
12. Replace password at column 20.
13. Rewrite `data.csv`.
14. Display success message.
15. Clear input fields after success.
16. Display file error on failure.
17. Cancel without writing.
18. Recognise that no current-password check exists.
19. Recognise that no uniqueness check exists.
20. Recognise that no password-strength check exists.
21. Recognise that passwords remain plaintext.

## 30. Data and QA register

1. Read CSV header.
2. Parse CSV rows.
3. Write CSV header.
4. Write CSV rows.
5. Append CSV row.
6. Validate expected row width.
7. Parse numeric fields.
8. Report read/write errors.
9. Cache attendance data.
10. Snapshot leave files.
11. Replace file through temporary file.
12. Restore snapshot after failure.
13. Run console backend tests.
14. Record pass/fail results.
15. Run attendance assertions.
16. Run leave approval/rejection assertions.
17. Run GUI retest.
18. Run Add Employee screen-fit checks.
19. Compare payroll gross to CP2 output.
20. Compile with lint enabled.
21. Run GUI smoke checklist.

---

# PART IV — KNOWN ISSUES, LIMITATIONS, AND NON-FEATURES

## 31. Known implementation issues

These are not additional features. They are important qualifications that should accompany any description of the system.

### 31.1 Plaintext passwords

- Employee passwords are stored in `data.csv`.
- HR passwords are stored in `HRLogin.csv`.
- Login compares plaintext input directly with stored text.
- The approved OOP design describes hashed credential storage, but the repository says that migration was not completed.
- The Add Employee form inserts the default username `admin` and password `password`.
- The credential-change dialog updates plaintext values rather than creating password hashes.

### 31.2 Legacy CSV parsing

- Several legacy GUI classes use `split(",")` to read CSV rows.
- Simple splitting does not honour quoted fields containing commas.
- The generic `MainMenu` writer writes comma-separated text without full quote escaping.
- Addresses and leave reasons can contain commas, so column positions can become unreliable unless special handling is applied.
- The leave service has a helper that joins trailing reason cells, but this does not replace a standards-compliant CSV parser across the whole application.

### 31.3 Employee record editing and persistence

- Add Employee appends the new record directly to `data.csv`.
- Update Employee changes the table model first; HR must press Save to write it to disk.
- Delete Employee removes the row from the table model first; HR must press Save to write it to disk.
- Refresh reloads the disk copy and can discard unsaved edits.
- The Add Employee form checks required fields and numeric conversion, but it does not run the full OOP `Employee` constructor validation.

### 31.4 Employee credential management

- The Change Credentials dialog does not request the old password.
- It does not check whether another employee already uses the new username.
- It does not enforce password length, complexity, reuse, or breach rules.
- It stores the new password in plaintext.

### 31.5 HR login window

The current HR login handler disposes the login window after processing the action, including the unsuccessful branch. This means an invalid attempt can close the HR login screen rather than leaving it open for another attempt.

### 31.6 Leave form and leave service are not the same entry path

- The employee leave form writes directly to `pendingleaves.csv`.
- This bypasses `LeaveService` for request creation.
- The form parses an integer but does not enforce positive days before append.
- The approval service enforces `days >= 1` later.
- The form does not require a non-empty reason.
- The repository records a line-ending inconsistency caused by the direct append path.

### 31.7 Leave-credit accounting

- One approved request subtracts one leave credit, regardless of the number of days requested.
- One approved request increments the approved-request counter by one.
- One rejected request increments the rejected-request counter by one and preserves leave credits.
- Requested days remain recorded in the leave files.
- Approved leave does not currently alter attendance-derived work hours or the gross-pay formula.

### 31.8 Payroll calculation limits

- Gross pay uses `workedDays × hourlyRate × 8`.
- A complete clock-in/clock-out pair counts as a worked day.
- The exact elapsed hours of a record are not used to prorate this gross-pay calculation.
- The payroll result currently records absences as zero.
- The current gross-pay calculation does not add all subclass benefits or the stored allowance values into gross pay.
- Annual leave-credit entitlement is recorded as a value, but that alone does not mean approved leave has been incorporated into payroll arithmetic.

### 31.9 Missing OOP design classes

The README identifies four unimplemented classes from the original design:

1. `Credentials`
2. `LeaveRequest`
3. `LeaveManagement` (OOP design/domain/service class)
4. `HRManager`

The Swing classes `LeaveManagement.java` and `LeaveApproval.java` still exist. Do not state that the application has no leave-management user interface; the missing item is the corresponding OOP design class.

### 31.10 Other documented UI issue

The repository's known-issues notes record overlap in an HR dashboard gross semi-monthly-rate label. This is a presentation defect, not a payroll calculation feature.

---

# PART V — IMPLEMENTATION EVIDENCE AND SOURCE INDEX

## 32. Repository-reported verification evidence

The README and supporting output files report the following results for the inspected repository version:

| Verification item | Reported result | What it supports |
|---|---:|---|
| OOP backend implementation | 12 of 16 designed classes implemented | Scope of the backend refactor |
| Backend source/test files | 13 files, including console harness | Backend code footprint |
| Java compilation with `-Xlint:all` | 0 warnings, 0 errors | Compilation status recorded by the project |
| Console backend test suite | 91 / 91 checks pass | Backend unit/console checks |
| GUI smoke checklist | 5 / 5 checks marked Yes | The documented GUI smoke-test set |
| Gross-pay comparison | 408 / 408 employee-month comparisons match CP2 | Gross-pay parity on the comparison set |
| Attendance test output | 33 assertions, 0 failures | Attendance import/filter/date parsing checks |
| Attendance dataset | 5,168 rows, 34 employees | Size of the imported timecard dataset |
| Leave fix test output | 44 assertions, 0 failures | Leave approve/reject and transactional behaviour |
| Add Employee screen-fit output | 22 assertions, 0 failures | Form fit and display checks |

These are repository-reported test outputs. This synthesis has inspected the documentation and source code; it has not independently executed the Java application or rerun those tests.

## 33. Primary source files to inspect

The links below point to files on the repository's `main` branch. If the repository changes later, the live source may differ from the version assessed here.

### 33.1 Swing application

| File | What it supports |
|---|---|
| [`LoadingScreen.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LoadingScreen.java) | Application entry sequence |
| [`LandingPage.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LandingPage.java) | Employee/HR role selection and exit |
| [`Login.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Login.java) | Employee login, field validation, CSV credential lookup |
| [`LoginHR.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LoginHR.java) | HR login and HR CSV lookup |
| [`MainMenu.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/MainMenu.java) | HR employee table, employee operations, payroll entry point |
| [`Dashboard.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Dashboard.java) | Employee self-service screen and salary view |
| [`Form.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/Form.java) | Add Employee form and row creation |
| [`ChangeCredentials.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/ChangeCredentials.java) | Employee username/password update |
| [`LeaveForm.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveForm.java) | Employee leave submission form |
| [`LeaveManagement.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveManagement.java) | Processed leave ledger and navigation |
| [`LeaveApproval.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveApproval.java) | Pending leave queue and approve/reject actions |
| [`AttendanceRepository.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/AttendanceRepository.java) | Timecard import, parsing, filtering, and period derivation |
| [`LeaveService.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/src/motorphemployee/LeaveService.java) | Leave decision logic, counters, multi-file writes, rollback |

### 33.2 OOP backend

| File | What it supports |
|---|---|
| [`Person.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/Person.java) | Identity and contact data with validation |
| [`Employee.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/Employee.java) | Abstract employee data model and common validation |
| [`RegularEmployee.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/RegularEmployee.java) | Regular employee allowances, leave credits, and promotion rule |
| [`ProbationaryEmployee.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/ProbationaryEmployee.java) | Probationary employee allowances, leave credits, and promotion rule |
| [`AttendanceLog.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/AttendanceLog.java) | Clock-in/out validation, daily hours, worked-day count |
| [`GovernmentContributionCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/GovernmentContributionCalculator.java) | Abstract calculator contract and shared validation |
| [`SSSCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/SSSCalculator.java) | SSS band calculation |
| [`PhilHealthCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PhilHealthCalculator.java) | PhilHealth premium and employee share |
| [`PagIbigCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PagIbigCalculator.java) | Pag-IBIG employee deduction |
| [`WithholdingTaxCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/WithholdingTaxCalculator.java) | Monthly withholding-tax bands |
| [`PayrollCalculator.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PayrollCalculator.java) | Main payroll workflow |
| [`PayrollRecord.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/PayrollRecord.java) | Immutable payroll result and result validation |
| [`ConsoleBackendTest.java`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/src/motorphemployee/oop/ConsoleBackendTest.java) | Backend console test harness |
| [`NOTES.md`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/oop/NOTES.md) | Implementation deviations, known issues, and test/fix notes |
| [`README.md`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/README.md) | Project scope, source roots, implementation status, and setup |
| [`README.CP2.md`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/README.CP2.md) | Earlier baseline functionality and screens |
| [`SETUP.md`](https://github.com/J0eychnpulpey/MO-IT110-MotorPH-OOP-Team3/blob/main/SETUP.md) | Execution and IDE setup |

## 34. Evidence references

The repository's README identifies supporting files for the following evidence:

- `oop/console-test-output.txt`
- `oop/cp2-parity-output.txt`
- `oop/parity-report.txt`
- `oop/matrix-verification.md`
- `oop/attendance-test-output.txt`
- `oop/leave-fix-test-output.txt`
- `oop/gui-retest-output.txt`
- `oop/ki20-screen-fit-output.txt`

These files should be inspected directly if the task changes from feature inventory to a formal test-evidence report. This document reports the summary values published by the README and names the supporting files; it does not invent additional test cases or claim an independent rerun.

---

# PART VI — AGENT HANDOFF NOTES

## 35. Rules for describing this project accurately

An agent receiving this document should follow these constraints when answering questions or producing project documentation:

1. Treat `src/motorphemployee/` as the Swing GUI and legacy data-management layer, and `oop/src/motorphemployee/oop/` as the refactored backend.
2. Do not say that the whole application is an OOP backend. It is a refactored application that retains the Swing screens and CSV persistence while moving selected logic into OOP classes.
3. Do not attribute payroll formula ownership to `MainMenu` or `Dashboard`. The README says that they call `PayrollCalculator`, and that the duplicated calculation methods were removed.
4. Do not state that every designed OOP class is implemented. The repository reports 12 of 16 implemented, with `Credentials`, `LeaveRequest`, OOP `LeaveManagement`, and `HRManager` design-only.
5. Do not confuse the missing OOP `LeaveManagement` design class with the existing Swing `LeaveManagement.java` dialog.
6. Do not describe password handling as secure or hashed. The code stores plaintext passwords.
7. Do not claim that the employee can edit profile data from the dashboard. The current button directs the employee to contact HR.
8. Do not claim that leave credits are reduced by the number of requested days. The current service decrements one credit for one approved request.
9. Do not claim that approved leave changes payroll hours or gross pay. That link is not implemented in the current payroll formula.
10. Do not claim that gross pay is calculated from elapsed attendance hours. It uses complete attendance days multiplied by hourly rate and eight hours.
11. Do not claim that the absence value is derived from attendance. The current payroll result records zero absences.
12. Do not claim that all salary allowances are included in gross pay. The current gross formula is worked days multiplied by hourly rate multiplied by eight.
13. Distinguish Add Employee's direct append from Update/Delete's table-model edits that wait for Save.
14. Distinguish the employee's pending leave submission from HR's approval service. The form still writes directly to the pending CSV.
15. When discussing tests, identify them as repository-reported output unless someone independently reruns them.
16. When describing government rules, distinguish implemented project matrices from current legal requirements. The code's stored values are a software implementation and may require a separate current-law check.
17. Do not infer database tables, audit logs, role permissions, or security controls that are not present in this repository's code.
18. Do not claim that CSV persistence is transactional throughout the application. The leave decision service provides rollback attempts for its three related files; the rest of the legacy CSV operations are not all handled through the same service.
19. Keep known defects visible instead of silently repairing their descriptions in a project summary.
20. Use the repository's own terms for classes, fields, and files, and state clearly whenever a description is an inference rather than a directly implemented method.

## 36. Final feature-completeness interpretation

The MotorPH application provides the core course-project workflows expected of a payroll desktop system: separate employee and HR entry points, employee-record administration, employee profile viewing, attendance-based salary calculation, government deduction calculation, employee leave submission, HR leave decisions, and CSV-backed persistence.

The OOP refactor provides a distinct backend for employee types, validation, attendance records, statutory calculators, and payroll results. Its most important architectural change is that the GUI no longer owns duplicated payroll formula implementations. The backend also supplies automated console checks and documented parity comparisons.

The implementation is not a complete enterprise payroll platform. Authentication remains plaintext, data storage remains CSV-based, the employee profile cannot be edited by the employee, the leave-submission screen bypasses the leave service, approved leave is not reflected in payroll hours, and the current payroll formula applies a fixed eight-hour value per complete attendance day. Four designed OOP classes remain unimplemented. Those qualifications are part of the feature inventory, not optional footnotes.

---

**End of feature inventory and pseudocode.**

## 37. Additional pseudocode for cross-cutting minor operations

This section supplies explicit calls for minor functions whose detailed implementation is shared with another major feature. The duplication is intentional: an agent can find a direct pseudocode block for each user-facing feature without assuming that a shared helper is a separate service.

### 37.1 HR main-menu clock

```text
PROCEDURE UPDATE_HR_MAIN_MENU_CLOCK
    CALL START_CLOCK(MainMenu.dateLabel, MainMenu.timeLabel)
END PROCEDURE
```

### 37.2 Employee dashboard clock

```text
PROCEDURE UPDATE_EMPLOYEE_DASHBOARD_CLOCK
    CALL START_CLOCK(Dashboard.dateLabel, Dashboard.timeLabel)
END PROCEDURE
```

### 37.3 Attendance parser used by data persistence

```text
PROCEDURE VALIDATE_ATTENDANCE_CSV_ROW(line, lineNumber)
    CALL PARSE_ATTENDANCE_ROW(line, lineNumber)
    IF parsing fails THEN
        CALL HANDLE_ATTENDANCE_IMPORT_ERROR(error)
    END IF
END PROCEDURE
```

The complete field-by-field parser is in section 7.3.3; this is the persistence-layer invocation of that operation.

### 37.4 Attendance cache retrieval

```text
FUNCTION GET_CACHED_ATTENDANCE_RECORDS
    IF cache is not initialised THEN
        cache ← READ_AND_PARSE attendance.csv
    END IF
    RETURN cache
END FUNCTION
```

### 37.5 Read pending leave file

```text
FUNCTION READ_PENDING_LEAVE_FILE
    OPEN pendingleaves.csv FOR reading
    rows ← READ each pending request row
    CLOSE file
    RETURN rows
END FUNCTION
```

### 37.6 Read processed leave file

```text
FUNCTION READ_PROCESSED_LEAVE_FILE
    OPEN leaves.csv FOR reading
    rows ← READ each processed request row
    CLOSE file
    RETURN rows
END FUNCTION
```

### 37.7 Map a pending leave row for the user interface

```text
FUNCTION MAP_PENDING_ROW_FOR_GUI(row)
    employeeID ← row[0]
    leaveType ← row[1]
    days ← row[2]
    reason ← RECONSTRUCT_REASON_FROM_ROW(row)
    RETURN [employeeID, leaveType, days, reason]
END FUNCTION
```

### 37.8 Map a processed leave row for the user interface

```text
FUNCTION MAP_PROCESSED_ROW_FOR_GUI(row)
    employeeID ← row[0]
    leaveType ← row[1]
    days ← row[2]
    reason ← RECONSTRUCT_REASON_FROM_ROW(row)
    approval ← final decision column
    RETURN [employeeID, leaveType, days, reason, approval]
END FUNCTION
```

### 37.9 Console test harness exit behaviour

```text
PROCEDURE FINISH_CONSOLE_TEST_RUN(passedCount, failedCount)
    PRINT passedCount + " passed"
    PRINT failedCount + " failed"

    IF failedCount > 0 THEN
        EXIT program with failure code 1
    ELSE
        EXIT program with success code 0
    END IF
END PROCEDURE
```

### 37.10 Attendance test suite

```text
PROCEDURE RUN_ATTENDANCE_TEST_SUITE
    repository ← NEW AttendanceRepository
    records ← repository.records()

    ASSERT record count equals expected imported dataset count
    ASSERT distinct employee count equals expected employee count
    ASSERT earliest date equals expected first date
    ASSERT latest date equals expected last date
    ASSERT employee-month lookup returns only months with rows
    ASSERT month results are sorted chronologically
    ASSERT filtered records belong to requested employee and month
    ASSERT malformed dates are rejected
    ASSERT malformed times are rejected
    ASSERT malformed row widths are rejected
    ASSERT incomplete attendance does not count as a worked day

    REPORT assertion totals
END PROCEDURE
```

The repository records 33 attendance assertions with no failures over its 5,168-row, 34-employee dataset.

### 37.11 Leave transaction test suite

```text
PROCEDURE RUN_LEAVE_TRANSACTION_TEST_SUITE
    CREATE isolated copies of pending, processed, and employee data files

    TEST approval:
        submit valid request
        approve request
        ASSERT request removed from pending
        ASSERT processed row has approval = Yes
        ASSERT leave credits decrease by exactly one
        ASSERT approved counter increases by one

    TEST rejection:
        submit valid request
        reject request
        ASSERT request removed from pending
        ASSERT processed row has approval = No
        ASSERT leave credits remain unchanged
        ASSERT rejected counter increases by one

    TEST double processing:
        attempt to decide the same request twice
        ASSERT second decision is refused

    TEST stale row:
        modify or remove selected request after selection
        ASSERT decision is refused

    TEST malformed employee data:
        create shifted/ragged employee row
        ASSERT service refuses to rewrite it

    TEST missing employee:
        submit request for unknown employee
        ASSERT decision fails

    TEST no remaining credits:
        approve request for employee with zero credits
        ASSERT approval fails

    TEST comma in reason:
        process reason containing commas
        ASSERT reason is reconstructed correctly

    TEST line-ending preservation:
        prepare CRLF files
        process request
        ASSERT expected line endings remain

    TEST write failure:
        force a file-write failure
        ASSERT all three original files are restored

    REPORT assertion totals
END PROCEDURE
```

The README's associated test evidence reports 44 assertions and no failures.

### 37.12 Add Employee window-fit test suite

```text
PROCEDURE RUN_ADD_EMPLOYEE_SCREEN_FIT_TESTS
    OPEN Add Employee form on available display configuration
    MEASURE form bounds
    ASSERT form does not extend beyond usable screen bounds

    IF content height exceeds visible area THEN
        ASSERT scroll pane is present
    END IF

    PRESS Escape
    ASSERT form closes without closing parent window

    REPEAT for the documented display-size cases
    REPORT assertion totals
END PROCEDURE
```

The repository identifies 22 passing assertions for this screen-fit check.

### 37.13 GUI approval/rejection retest

```text
PROCEDURE RUN_GUI_LEAVE_RETEST
    START application
    LOG IN as HR
    OPEN Leave Management
    OPEN Pending Leaves

    SELECT a test request
    APPROVE it
    ASSERT it leaves pending list immediately
    ASSERT it appears in processed list with Yes
    ASSERT leave counters change once

    SELECT another test request
    REJECT it
    ASSERT it leaves pending list immediately
    ASSERT it appears in processed list with No
    ASSERT leave credits do not decrease on rejection
    ASSERT rejected counter increases once

    CLOSE dialogs
    REPORT observed outcomes
END PROCEDURE
```

The README and dated notes identify a successful GUI click-through of one approval and one rejection on 31 October 2026.

### 37.14 Payroll calculator edge-case tests

```text
PROCEDURE RUN_PAYROLL_EDGE_CASE_TESTS
    TEST valid employee and complete attendance:
        ASSERT gross = workedDays × hourlyRate × 8
        ASSERT net = gross - total deductions

    TEST null employee:
        ASSERT calculation is rejected

    TEST null attendance:
        ASSERT calculation is rejected

    TEST missing period date:
        ASSERT calculation is rejected

    TEST reversed date range:
        ASSERT calculation is rejected

    TEST no worked days:
        ASSERT zero gross is rejected

    TEST statutory deductions equal gross:
        ASSERT no negative payslip is produced

    TEST inactive contribution calculator:
        ASSERT inactive calculator is skipped

    TEST injected calculator list:
        ASSERT only configured active calculators contribute

    TEST PayrollRecord immutability:
        ASSERT no mutation setters are exposed

    TEST payroll arithmetic:
        ASSERT net equals gross minus deductions within 0.01

    REPORT assertion totals
END PROCEDURE
```

### 37.15 Contribution boundary tests

```text
PROCEDURE RUN_CONTRIBUTION_BOUNDARY_TESTS
    FOR EACH SSS lower-bound value:
        ASSERT the exact lower bound enters the new bracket
        ASSERT value immediately below it remains in the prior bracket

    ASSERT salary below ₱3,250 returns the below-table SSS amount
    ASSERT salary at and above the top SSS band returns the maximum

    ASSERT PhilHealth minimum premium is applied at the lower salary range
    ASSERT PhilHealth premium increases by the configured 3% above ₱10,000
    ASSERT PhilHealth premium does not exceed ₱1,800
    ASSERT returned PhilHealth amount is one half of premium

    ASSERT Pag-IBIG rate is 1% from ₱1,000 through ₱1,500
    ASSERT Pag-IBIG rate is 2% above ₱1,500
    ASSERT Pag-IBIG amount does not exceed ₱100
    ASSERT salary below ₱1,000 returns zero in this implementation

    ASSERT withholding-tax tax-free ceiling returns zero
    ASSERT each taxable threshold selects the correct band
    ASSERT ₱20,832.01–₱20,832.99 does not produce negative tax
    ASSERT top bracket uses 35% over ₱666,667

    REPORT boundary-test results
END PROCEDURE
```

### 37.16 Gross-pay parity test

```text
PROCEDURE RUN_GROSS_PAY_PARITY_TEST
    baseline ← READ CP2 baseline results
    refactored ← READ OOP payroll results

    FOR EACH employee-month comparison:
        compare baseline gross to OOP gross
        count exact cent-level matches
        record every mismatch
    END FOR

    REPORT matches, mismatches, and largest difference
END PROCEDURE
```

The README reports 408 of 408 gross-pay comparisons matching. This result concerns gross pay; the statutory deductions deliberately differ from the original CP2 implementation where the source notes identify errors in the older contribution formulas.

### 37.17 Compiler check

```text
PROCEDURE RUN_COMPILER_CHECK
    COMPILE backend source and test source
    ENABLE -Xlint:all

    IF compilation fails THEN
        PRINT compiler errors
        RETURN FAIL
    END IF

    PRINT warning count
    PRINT error count
    RETURN PASS only if both counts are zero
END PROCEDURE
```

### 37.18 GUI smoke checklist

```text
PROCEDURE RUN_DOCUMENTED_GUI_SMOKE_CHECKLIST
    FOR EACH of the five documented smoke checks:
        launch or navigate to the relevant screen
        perform the documented action
        observe the expected screen or result
        record Yes or No
    END FOR

    REPORT all five check results
END PROCEDURE
```

The README reports five of five checks marked Yes. The names and exact click sequence of every checklist row should be read from the repository's test evidence before treating this summary as a substitute for the underlying checklist.

---

## 38. Important payroll-period caveat for agent review

The backend's no-date overload sets a recent 15-day period, while the statutory calculators contain monthly contribution/tax thresholds. The explicit-period overload passes the computed period gross amount directly into those calculators, and the source does not show an explicit annualisation or monthly-equivalent conversion for the recent-15-day overload.

The GUI salary screens use a selected calendar month, so their ordinary user-facing path has a month-sized period. An agent discussing the backend's no-date overload should identify the monthly-threshold/15-day-period relationship as an item requiring validation, rather than assume that both overloads produce the same contribution basis. The repository says the no-date overload uses the same computation and changes only the period; that is a source statement, not proof that monthly statutory thresholds are correct for every period length.

---


---

# Appendix D — File and provenance cautions

1. The ZIP also contains three binary PDFs and two very large text source files (the LMArena trace and skills compilation). They are intentionally retained as files rather than expanded inline in this Markdown.
2. The project PDFs include images/diagrams. The full synthesis above the code appendices describes the ERD and each DFD level; consult the PDFs inside the ZIP for the original visual geometry and labels.
3. The LMArena text export has collapsed gaps. Do not pretend the missing 24 items were reviewed.
4. The original Firebase version and this local JSON reconstruction are different operating modes. Firebase source integration is a separate future port, not something this ZIP claims to provide.
5. The monthly statutory estimate is a useful live feature, but not the complete payroll lifecycle. Preserve the distinction in UI labels, result exports, future docs, and demonstrations.
6. Synthetic records only. Do not use this package for actual staff records or salary processing.

**End of agent handoff.**
