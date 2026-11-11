# Comprehensive review of submissions 1–9 and provisional MotorPH system plan

**Prepared:** 3 November 2026 (Asia/Shanghai)  
**Scope:** nine numbered repository submissions, initial and follow-up runtime checks, and three supplied MO-IT107 design documents (database design, data dictionary, and data-flow diagram).  
**Purpose:** consolidate the findings in one report and develop a traceable, explicitly provisional system plan from the supplied PDFs. This is a review and planning artifact—not a claim that any numbered repository is the user's authored/approved project, and not a substitute for missing payroll or attendance requirements.

## Executive summary

- All nine repositories were statically reviewed and tested only in isolated working copies where execution could write files or data. Original repository files and original fixtures were left untouched. The uploaded inventory CSV was treated only as an intentionally wrong-schema compatibility input, never as payroll data.
- Retests verified representative startup/navigation for most GUI projects and test suites for folders 6 and 9. Key results: F6 **167/167**; F9 **91/91 payroll/backend + 33/33 attendance + 44/44 leave = 168/168** when timezone-aligned; F3's simple compatible synthetic row yielded **17,500.0**, while its bundled quoted-comma CSV remains misparsed (**34/34 gross values differ** from correctly parsed salary-column values); F2/F5 reached the admin dashboard and opened six navigation areas on a disposable MariaDB-compatible test database. The detailed per-folder record is appended below.
- Folders **2 and 5** are near-identical executable code packaged with different README/course labels. Do not count them as independent implementations or infer authorship from labels.
- The PDFs describe a coherent high-level scope: employee and government-ID management; attendance and leave; payroll, contributions, payslips and monthly summaries; role-based access with PII approval; audit and archival. The DFD names five actors and decomposes the work into employee management, payroll processing and archival.
- The supplied design artifacts are not fully consistent with each other, including within the database-design PDF itself. Its page-5 schema image color-flags `Government` for additional security and shows several links/columns that disagree with nearby prose definitions: `Address.regionID`, `Supervisor.departmentID`, `Schedule.restDayID`, and `Leave.scheduleID`. The database-design document defines 30 tables; the data dictionary lists 29 and omits a separate `Rest Day` table. Employee department/government IDs also differ between the data dictionary and the design diagram/prose. These must be reconciled before schema implementation.
- A useful implementation plan can be proposed, but the documents do **not** supply the actual payroll/attendance rule set, detailed role permissions, approved repository-to-document mapping, or the two referenced business-process/requirements documents. Those gaps remain explicitly **unknown**. The system plan below labels recommendations as proposals rather than verified requirements.

## Workspace failure and test-run cleanup

The workspace tool twice reported **“Failed to initialize E2B workspace”** before a later call succeeded. This indicates a temporary workspace-initialization failure at the tool/platform boundary; the exact infrastructure cause is unknown. There is no evidence that the PDFs, filenames, repository contents, or a user action caused it, so this report does not assign a more specific cause.

The test-run copies were not deleted earlier because their outcomes and test-only changes had not yet been consolidated into a durable report. Deleting them before that point would have removed useful reproduction material and risked losing which adaptations produced which results. This report now incorporates the initial analysis, tests, retest addendum, test-copy adaptations, and feature pseudocode. After checking that all nine folders, key outcomes, and test-only changes were represented, I deleted `/home/user/repo_review/test_runs/` (the `current` and `compat_retest` copies, including the disposable database/schema). The directory is confirmed absent; `/home/user/repo_review/full_repo/testingester-main/` remains present. Original repository files and original fixtures, uploaded PDFs, and the report were not deleted or modified by that cleanup.

## Evidence labels and review boundaries

- **Document fact:** wording/schema/flows present in one of the supplied PDFs. It is not automatically a confirmed operational requirement.
- **Repository observation:** source/build/data structure present in one of the nine repositories.
- **Test observation:** behavior directly exercised in an isolated copy under the stated test conditions.
- **Proposed plan:** a recommended way to reconcile/implement the documented intent; it is not an approved business rule.
- **Unknown:** information not supplied or not safely inferable. No missing rule is filled by guesswork.

The MO-IT107 database-design PDF was text-extracted and reviewed by section; its relationship tables and column definitions were read, and its page-5 schema/relationship image was rendered and visually checked. The data-dictionary PDF was text-extracted and its table/column definitions and relationship lists reviewed; its embedded page images were checked as well (they reproduce table/definition content rather than add a separate process diagram). The DFD text layer was sparse, so its diagram pages were rendered and visually reviewed (context, level 1, level 2 employee management, level 2 payroll processing, and level 2 archival). This supports describing the diagrams, but does not validate their upstream business references: the DFD cites a Business Requirement Specification and “MotorPH Existing Business Processes - Payroll,” neither of which was supplied here. The user has already stated they do not have those materials.

## Findings from the three supplied MO-IT107 PDFs

### 1. Database-design overview — documented intent

The database-design document describes five broad capability areas:

1. Employee management, including employment status, department/position, salary structure and personal information.
2. Payroll computation using attendance, adjustments, overtime, tax and government contributions; semi-monthly payslips and a monthly summary are named.
3. Attendance/leave tracking, including time-in/time-out, leave requests, holidays/rest days and leave benefits.
4. Government contributions and compliance, including employer/employee contributions and tax/contribution reporting.
5. Role-based access control for system administrators, HR, payroll staff and employees; audit traceability; and protected PII.

The overview states that PII access requires approval from **two authorized personnel**, with only corresponding contribution values normally visible/accessed. Treat this as explicit design intent, not as verified repository behavior. The precise approval sequence, approver roles, emergency access, and field-level rules are not specified.

### 2. Database-design table inventory (30 tables)

The database-design document's sections 3.1–3.30 define:

- **Employee/organization:** `Employee`, `Address`, `Department`, `Position`, `Supervisor`, `Schedule`, `Rest Day`.
- **Attendance/leave/audit:** `Leave Balance`, `Changelog`, `Attendance`, `Leave`, `Leave Accrual History`.
- **Government records:** `Government`, `Government Deductibles`, `Contributions`.
- **Payroll:** `Payroll`, `Payslip`, `Leave Monetisation`, `Earnings`, `Deductibles`, `Other Transactions`, `Government Tally`, `Summary`.
- **Reference/configuration:** `Regions`, `Cities`, `Holiday`, `Months`, `Rates`, `Tax Table`, `Leave Type`.

**Visual schema-diagram note (database-design PDF, page 5):** the page contains a large entity-relationship diagram connecting the 30 table boxes with key links. Its legend color-codes access patterns: orange for update/append/read, purple for update/read, blue for append/read, and red for “additional security required.” The `Government` box is red, visually flagging those identifiers for extra protection. This is a meaningful security annotation, but the diagram does not define a concrete permission policy or encryption/key-handling mechanism. The image also provides relationship/column information that differs from some of the nearby prose table definitions; the specific conflicts are itemized below. The data-dictionary PDF's embedded page images mostly reproduce the table-definition content, rather than introducing another system-flow diagram.

The dictionary's table overview names the same broad areas but has no separate `Rest Day` table definition. The `Schedule` table already contains `restDay` as a `SET`, while the database-design PDF also defines a Boolean-per-weekday `Rest Day` table with no foreign key and no listed relationship. Whether to keep a single schedule field, use a separate rest-day relation, or support multiple weekly rest days is unresolved.

Important documented design relationships include employee-to-address/position/supervisor; position-to-department; supervisor-to-employee; schedule-to-employee; attendance-to-schedule; leave balance and leave/accrual/monetisation links; government-to-employee; payroll-to-employee/earnings/deductibles/summary/month; payslip-to-payroll; and government tally-to-summary. The report's appended source material preserves the original relationship tables in full.

### 3. Data dictionary and database-design discrepancies requiring a decision

These are cross-document mismatches, not automatic proof that either document is correct:

| Area | Database-design PDF | Data-dictionary PDF | Implication before implementation |
|---|---|---|---|
| Employee → Department | The detailed `Employee` definition and page-5 schema image show `positionID` but not `departmentID`; department is reached via `Position.departmentID`. | `Employee` lists `departmentID` as a foreign key and its relationship list also links to Department. | Choose one source of department affiliation or explicitly define why both direct and position-derived department values exist; avoid contradictory duplicates. |
| Employee ↔ Government | `Government.employeeID` links a government record to an employee; neither the detailed `Employee` definition nor the page-5 schema image lists `governmentID` on Employee. | Employee also lists `governmentID`; the dictionary separately describes Government → Employee. | Define one canonical ownership/FK direction and enforce one-to-one uniqueness if that is intended. Avoid a circular or duplicate relationship. |
| Address → Region | The detailed Address table lists `cityID` only and `Cities` links to `Regions`; the page-5 schema image additionally shows `regionID` on Address. | Address lists both `cityID` and `regionID`, and its relationship list links to both Cities and Regions. | Decide whether region is derived through city or stored redundantly; validate consistency if duplicated. The schema diagram agrees with the dictionary here, but not with the detailed table definition. |
| Supervisor → Department | The detailed Supervisor definition lists `supervisorID` and `employeeID`; the page-5 schema image additionally shows `departmentID`. | Supervisor lists `departmentID` and the dictionary relates Supervisor to Department. | Clarify whether department is derived from the supervisor's employee/position or stored separately. The schema diagram agrees with the dictionary here, but not with the detailed table definition. |
| Rest day | Detailed Schedule definition has `restDay SET` and a separate `Rest Day` table with no FK; the page-5 schema image instead shows `Schedule.restDayID` as an FK to `Rest Day`. | No separate Rest Day table entry; Schedule has `restDay SET`. | Resolve the conflicting representations and decide whether schedules allow one or several rest days. The schema image includes a relationship the detailed table definition and dictionary do not. |
| Leave → Schedule | The detailed Leave definition lists `balanceID` and `leaveType` but not `scheduleID`; the page-5 schema image shows `scheduleID` as a Leave FK. | Leave lists `scheduleID` and the relationship list includes a schedule association. | Decide whether schedule is derived through `Leave Balance` or stored directly; avoid mismatched schema/code. The schema image agrees with the dictionary here, but not with the detailed table definition. |
| Contribution period | `Government Deductibles` is keyed by `(governmentID, monthID)`, while `Contributions` is keyed by `(governmentID, contributor)` and its relationship to deductibles is only described via `governmentID`. | Similar government/contribution entities are described; the relationship details do not resolve the period key issue. | Confirm whether contributions vary by month/pay period; if so, represent period in the key/relationship to avoid ambiguous historical records. |
| Access-control persistence | DFD has “Configuration & User Management,” role actors and PII approval; the 30-table design does not define user/account/role/permission/approval-request tables. | No corresponding account/permission/PII-approval table definition. | Specify whether external identity/RBAC will be used or add an approved access-control model. Do not infer credentials from employee data. |
| Archival | DFD shows cold storage, annual processing, `Compile (1YO) Records` for logs and `Compile (2YO) Records` for payslips, followed by removal of relevant records. | The dictionary does not define cold-storage/retention structures. | Preserve the literal `1YO`/`2YO` labels; their exact meaning and retention authority are undefined. Confirm retention, backup, restoration, legal holds and deletion authority before implementing. |
| Government Deductibles key | Field-by-field listing marks `governmentID` as PK/FK and `monthID` as FK, without clearly restating the full composite key at field level. | Detailed constraints explicitly define composite PK `(governmentID, monthID)`. | The schema diagram marks both as key/FK fields. Use the composite key only if the design owner confirms that interpretation. |
| Government Tally key | `summaryID` is marked Primary Key (assumed); `contributor` is not clearly marked as key in its field list. | Explicit composite PK `(summaryID, contributor)`. | Diagram marks both as key fields, with `summaryID` also FK. Clarify and document the intended composite key. |
| Rates key/version | Data Dictionary lists five rate fields, but not `implementationYear`. | Rates definition includes `implementationYear`, while the constraints section says no PK, FK, or unique constraint. | Schema image shows `implementationYear` as PK. Confirm whether it versions the rates and whether it is the key; the current artifacts disagree. |
| Holiday uniqueness | `holidayName` and `holidayDate` are each shown as individually unique. | Detailed design specifies a combined unique rule `(holidayName, holidayDate)`. | Diagram does not visibly resolve the uniqueness rule. Decide whether either field alone or only the pair must be unique. |
| Payroll month link | `monthID` appears in Payroll, but its field constraint does not clearly identify it as an FK. | Payroll FK list omits `monthID`, while the relationship matrix states Payroll → Months via `monthID`. | ERD shows `monthID`, but its key marker is not clear. Make the FK and referenced key explicit. |
| Leave Monetisation → Leave Type | `leaveType` is `NOT NULL`, but the field list does not clearly mark it as an FK. | Table-level FK summary lists `balanceID` only; the relationship matrix nevertheless shows Leave Monetisation → Leave Type. | ERD depicts the leave-type link. Align the column constraints and relationship matrix. |
| Relationship-matrix omissions | `Address.regionID` and `Cities.regionID` are listed as FKs in the dictionary. | Relationship matrix does not separately list Address → Regions or Cities → Regions; the design includes the relevant entities/fields elsewhere. | ERD visually connects the geographic entities. Ensure every intended FK is recorded consistently. |
| Contributions relationship | `Contributions.governmentID` is shown as a composite key/FK to Government. | Relationship matrix describes Contributions → Government Deductibles via `governmentID`, which may not uniquely identify a month-specific deductible record. | ERD shows contribution/govt-deductible connections; reconcile the referenced table and period key. |
| DFD annotations | Not applicable. | No prose definition for `opt*`, `1YO`, or `2YO`. | DFD uses these exact labels. Preserve them literally until their meaning is confirmed; do not convert them into production behavior by assumption. |

Other modeling choices to settle: the PDFs name `Months` as an enum/reference table but do not define a clear pay-period entity for semi-monthly periods. The database-design table definition and schema image show a `Rates.implementationYear` field, but the data dictionary omits it; the design gives no PK/FK/unique constraint for `Rates`, and `Tax Table` has no effective-date/version field. Thus the existence and meaning of an implementation year should be reconciled, and the documents still do not establish versioned tax brackets or authoritative rate values. The exact enum values and official rule-table values are not established in the supplied material. Government identifiers are highly sensitive; an `encryptedRef` field is mentioned, but key management, access logging and encryption lifecycle are not specified.

### 4. DFD flow, as drawn

**Context diagram (PDF page 2):** external actors are Employee, Supervisor, Human Resources, Payroll Staff and System Admin, with “MotorPH Payroll System” as the process. The diagram shows employee time-in/out and viewing employee information/payslips; supervisor team attendance, leave/monetisation and benefit/deduction handling plus team views; HR employee-detail/PII updates and access approvals; payroll staff verification and payroll/payslip/benefit-deduction updates; and system-admin configuration/user management, broad employee-information viewing, PII approval and archival handling. The arrows are a high-level flow map, not a complete permission matrix.

**Level 1 (PDF page 3):** process 1.0 Employee Management uses an Employee Repository and Logs; process 2.0 Payroll Processing uses a Payroll store; process 3.0 Archival transfers old logs/payroll toward Cold Storage. Employee, supervisor, HR, payroll staff and admin interact with the processes. The diagram labels retrieval/append/modify and backup/archive flows.

**Level 2 employee management (PDF page 4):** 1.1 Employee Information Handling; 1.2 Schedule and Time Tracking; 1.3 Government Info Access. Data stores include employee address, job profile, PII, changelog, attendance and leave. PII view/update and employee changes are shown with approval flows. Employee, supervisor, HR, payroll staff and system admin are shown interacting.

**Level 2 payroll processing (PDF page 5):** 2.1 Timesheet Validation; 2.2 Leave Monetisation Process; 2.3 Wage and Adjustments; 2.4 Government Deductibles; 2.5 Basic Payroll Computation; 2.6 Payslip Generation & Distribution; 2.7 Monthly Summary Compilation. Data stores include attendance/leave/leave-monetisation logs, wage, benefits & deductibles, tax table/rates, payslip and monthly summary report. The diagram shows supervisory wage/adjustment input and approved leave monetisation, payroll-staff verification, and employee/HR/team viewing paths.

**Level 2 archival (PDF page 6):** process 3.1 Appending and process 3.2 Deleting; system admin triggers annual archiving. The image labels one-year records for attendance, leave, changelogs and leave-monetisation logs, two-year payslips, cold storage, and removal of relevant records after an archival trigger/finish. This sequence must not be implemented as destructive production behavior until retention, authorization, restore testing and applicable requirements are confirmed.

### 5. Integrated synthesis and diagram-to-schema crosswalk

Taken together, the PDFs describe a lifecycle rather than three independent artifacts: establish an employee and their authorized job/schedule information; collect and validate attendance and approved leave; prepare and verify payroll using wage/adjustment and statutory-rule inputs; issue employee/team payslips and monthly aggregates; then preserve records under an archival process. The database-design PDF supplies the intended data structure and capability statements, the dictionary clarifies fields/types/constraints, and the DFD pictures the actors, transformations, stores and approval/archival flows.

#### What the DFD images add beyond the extracted text

The diagrams were visually inspected as rendered pages. Their arrows and data-store labels provide workflow details that are largely absent from the text layer:

| DFD image | Visual content and how it fits the other PDFs |
|---|---|
| **Context diagram, page 2** | Shows five external actors around one MotorPH Payroll System process. Employee sends attendance/time-in/time-out and receives employee information/payslips. Supervisor sends team-attendance adjustments, leave/monetisation handling and team wage/benefit-deduction inputs; team information/payslip views flow back. HR updates employee details and participates in approved PII access/update flows. Payroll Staff verifies employee/payroll details and updates payroll, payslip and benefit/deduction information. System Admin handles configuration/user management and archival, with employee-information and PII-approval flows shown. The diagram is a context map; it does not define each role's precise field-level permission. |
| **Level 1, page 3** | Decomposes the system into **1.0 Employee Management**, **2.0 Payroll Processing**, and **3.0 Archival**. The stores are **D1 Employee Repository**, **D2 Logs**, **D3 Payroll**, and **D4 Cold Storage**. D1/D2 feed employee-management and schedule/leave work; payroll processing consumes and updates payroll information; archival moves older records toward cold storage. This gives the main process boundary and store map for the database/table groups. |
| **Employee-management level 2, page 4** | Splits process 1.0 into **1.1 Employee Information Handling**, **1.2 Schedule and Time Tracking**, and **1.3 Government Info Access**. Visible stores include **D1.1 Employee Address**, **D1.2 Job Profile**, **D1.3 Personally Identifiable Information**, **D2.1 Attendance**, **D2.2 Leave**, and **D2.3 Changelog**. Employee time-in/out feeds schedule/time tracking; supervisors adjust attendance and handle leave requests; HR updates employee information; payroll staff verifies employee details; approved PII access is shown as a separate path. This maps naturally to Employee/Address/Position/Schedule, Attendance/Leave/Leave Balance, Government, and Changelog data, but the schema documents do not define all of the access-request records implied by these flows. |
| **Payroll level 2, page 5** | Splits process 2.0 into **2.1 Timesheet Validation**, **2.2 Leave Monetisation Process**, **2.3 Wage and Adjustments**, **2.4 Government Deductibles**, **2.5 Basic Payroll Computation**, **2.6 Payslip Generation & Distribution**, and **2.7 Monthly Summary Compilation**. It labels **D2.1 Attendance Logs**, **D2.2 Leave Logs**, **D2.4 Leave Monetisation Logs**, **D3.1 Wage**, **D3.2 Benefits & Deductibles**, **D3.3 Tax Table & Rates**, **D3.4 Payslip**, and **D3.5 Monthly Summary Report**. Visually, timesheet validation derives work hours from attendance and paid-leave detail from leave logs; approved monetisation is appended and added to benefits; supervisor/admin inputs can update wages/adjustments/rates; government deductions use the rate store; basic computation combines time activity, wage values and adjustments; payroll staff validates; payslips are distributed/viewed; and monthly totals are compiled. The image shows process order and named stores, not calculation formulas or approved amounts. |
| **Archival level 2, page 6** | Shows **3.1 Appending** records to **D4 Cold Storage** and **3.2 Deleting** relevant source records after the archival process finishes. An annual admin prompt is drawn. Attendance, leave, changelog and leave-monetisation logs are marked for **“1Y0/1YO”** compilation and payslips for **“2Y0/2YO”** compilation. This appears to mean one-year-old and two-year-old records, but the notation is visually small/ambiguous. The image depicts a copy-then-delete lifecycle; it does not provide a retention authority, archive format, restore procedure or legal-hold exception. Treat the ages and deletion arrows as unverified design annotations until confirmed. |

#### Crosswalk from DFD stores to designed tables

- **D1 Employee Repository / D1.1 Address / D1.2 Job Profile:** likely maps to `Employee`, `Address`, `Department`, `Position`, `Supervisor`, and `Schedule`. The exact direct-vs-derived employee/department relationship must be resolved.
- **D1.3 Personally Identifiable Information:** likely maps to sensitive employee fields and `Government` identifiers (including `encryptedRef` in the design). The PDFs do not define an access-request/dual-approval table or key-management model.
- **D2 Logs:** likely maps to `Attendance`, `Leave`, `Leave Balance`, `Leave Accrual History`, `Changelog`, and `Leave Monetisation`. The DFD's D2.1–D2.4 labels do not map one-to-one to all of these database tables; for example, a separate leave-monetisation log is drawn but the database design defines a `Leave Monetisation` table without an explicit archive store.
- **D3 Payroll:** likely maps to `Payroll`, `Payslip`, `Earnings`, `Deductibles`, `Other Transactions`, `Government`, `Government Deductibles`, `Contributions`, `Government Tally`, and `Summary`, with `Rates`, `Tax Table`, `Months`, `Holiday`, and `Leave Type` as reference/configuration data. The DFD's `Monthly Summary Report` is a named store/output; the data design has `Summary` but does not specify a report-file store or output format.
- **D4 Cold Storage:** present in the DFD but not modeled as a database table, archive schema, storage technology, manifest, or restore mechanism in the other PDFs.

#### Integrated process and control interpretation

The intended direction is: **authorized employee/job setup → time and leave capture → validation → payroll inputs and approved adjustments → rule-based calculation → payroll-staff verification → payslip and monthly summary → controlled archival**. PII approval and audit logging cut across that entire lifecycle. This synthesis is stronger than any one PDF alone, but it remains a documented design proposal: no numbered repository has been shown to implement this full DFD/database model, and no authoritative rule source or acceptance oracle was provided.

Important limits visible when the documents are read together:

- The database overview explicitly states **two-authorized-person PII approval**, while the DFD labels PII access/update approvals but does not draw a clear two-person sequence or define the approval record. Preserve the stricter written intent in a provisional design; do not claim the diagram proves the full mechanism.
- The overview names **semi-monthly payslips** and **monthly summaries**; the tables have pay-period start/end dates on `Payslip` and `monthID` in payroll-related tables, but no clear pay-period entity/calendar or approved cycle boundaries.
- The design includes `Rates.implementationYear`, but that field is omitted from the data dictionary and its constraints/semantics are not explained; `Tax Table` has no validity-period/version field. Actual statutory values are also absent. Historical payroll therefore cannot yet be independently reproduced from the documented schema alone.
- DFD archival arrows describe deletion after copying, but database/data-dictionary documents do not specify cold-storage structures or validated retention policy. Do not implement destructive deletion from the diagram alone.
- The DFD names Configuration & User Management, but the 30-table schema does not include a defined account/role/permission/approval model. Identity/access control remains an architecture requirement to specify, not an existing schema feature.

## Provisional system plan aligned to the supplied documents

Everything below is a **proposed plan**, not an assertion that the missing requirements have been supplied or approved. It uses the PDFs as a starting point and deliberately leaves rule values configurable/unknown.

### A. Architecture and boundaries

1. **Presentation layer:** separate employee, supervisor, HR, payroll-staff and system-admin views. UI screens collect requests and show only permitted results; they do not contain payroll formulas or direct persistence logic.
2. **Application services:** `EmployeeService`, `AttendanceService`, `LeaveService`, `PIIAccessApprovalService`, `PayrollRunService`, `PayslipService`, `MonthlySummaryService`, `AuditService`, and (only after approval) `ArchiveService`. Each service enforces authorization and state transitions.
3. **Domain/calculation layer:** explicit models for employee/job/address, schedule/attendance, leave balance/request/accrual/monetisation, payroll period, earnings/deductions, contribution/tax calculation and immutable payroll results. Monetary calculations use fixed-precision decimal arithmetic and one centrally tested rule engine.
4. **Persistence layer:** repositories/DAOs isolate the chosen database and enforce transactions, keys, constraints and migrations. Do not couple SQL to Swing screens or calculate pay in UI event handlers.
5. **Identity and authorization:** use an approved identity provider or a separately specified account/role/permission model. Enforce least privilege and employee/team scope. Build the documented two-authorized-person PII approval as an explicit auditable workflow unless the course owner clarifies otherwise; no bypass is assumed.
6. **Configuration/rule versioning:** store effective-dated versions of tax and contribution tables, leave policies, pay periods and attendance rules after authoritative values are supplied. Retain the exact rule version used for each finalized payroll for reproducibility.
7. **Audit and security:** append-only audit events for sensitive reads/changes/approvals, access review, encrypted storage/transit as applicable, protected secrets, backups and tested restore. Limit PII in exports/logs; do not treat a generic “admin” role as automatically entitled to government IDs.

### B. Proposed core data flow

1. **Employee/job setup:** authorized HR creates or updates employee, job/position/department, address and schedule through services. Validate required fields and effective dates. Record old/new field values in a controlled changelog. Resolve the Employee/Department/Government/Address/Supervisor discrepancies above before finalizing tables.
2. **Attendance capture and correction:** employee submits time-in/time-out; system associates events with the employee's effective schedule and attendance date. Supervisor adjustments require a reason and audit entry. Reconcile duplicates, missing punches, overnight shifts, holidays, rest days and timezone boundaries under the approved policy; do not invent those rules now.
3. **Leave request and balance:** capture leave type, dates/hours, reason, request timestamp and state. Supervisor (or the confirmed approver) approves/rejects; only approved leave changes payroll inputs. Update balances and accrual history transactionally. A leave monetisation request follows a separately approved path and creates a traceable monetary adjustment only after authorization.
4. **Payroll run:** select a confirmed pay period and eligible employees; validate attendance and approved leave; assemble wage and authorized adjustments; calculate gross earnings, statutory/other deductions, employer contributions and net pay from versioned rule tables; show validation exceptions; permit payroll-staff review/verification; finalize atomically and make the run idempotent.
5. **Payslip and monthly summary:** store one immutable payroll result per employee/pay period (subject to confirmed period/uniqueness rules), distribute a privacy-filtered payslip, and aggregate the monthly totals and government tallies. Re-running a finalized period should require a documented correction/reversal path, not silently overwrite history.
6. **Archival:** only after a formally confirmed retention policy, copy and verify records, record archive manifest/checksum and operator/approval, test restore, then delete only the authorized eligible source records. The DFD's 1-year/2-year labels are currently unverified policy annotations.

### C. Provisional role/permission intent

The PDFs establish actor names and a general tiered-access concept; the matrix below is a **least-privilege proposal**, not a final permission specification:

| Actor | Provisional permitted scope | Guardrails / unresolved detail |
|---|---|---|
| Employee | Own profile fields explicitly approved for self-view, own attendance/leave status and own payslips. | Own-data correction rights and access to government identifiers are not defined. |
| Supervisor | Team attendance correction/verification, leave decisions, approved team wage/adjustment inputs, team payslip/employee views only where required. | Team membership source, permitted salary/PII fields and separation-of-duties rules are unknown. |
| HR | Employee record maintenance, schedule/leave/attendance support, PII approval/request workflow. | Whether HR can see full government identifiers or only approve access is unclear; preserve dual approval stated in database overview. |
| Payroll Staff | Validate payroll inputs, run/review payroll, manage payslips, permitted contribution values and monthly summaries. | Full government identifiers should not be exposed merely because payroll needs contribution amounts; exact view permissions unknown. |
| System Admin | Account/configuration operations, system health and approved archive administration. | Admin access to employee details/PII is broad in the DFD but must be reconciled with least privilege and the two-person approval statement. |

### D. Proposed release phases and acceptance gates

- **Phase 0 — requirements reconciliation:** obtain authoritative payroll/attendance/leave rules, role matrix, pay-period definition, tax/contribution versions, data sources and retention approval; reconcile the schema mismatches. Do not start production calculations or destructive archival without this gate.
- **Phase 1 — data/domain foundation:** canonical schema, migrations, referential integrity, identity integration, authorization policy, audit events, test fixtures and privacy threat review.
- **Phase 2 — employee/schedule/attendance and leave:** implement normal capture, correction, approval and balance paths; cover duplicates, invalid times, missing punches, date/time boundaries and unauthorized actions using approved expected results.
- **Phase 3 — payroll and reporting:** implement versioned rule engine, payroll validation, controlled finalization, payslips, contribution totals and monthly summary. Independently verify sample calculations against authoritative expected values before acceptance.
- **Phase 4 — archive and operations:** implement only with approved retention and deletion authority; verify backup, restore, archive integrity, access audit and recovery before enabling purge.

**Cross-cutting acceptance criteria proposed:** no unapproved access to PII; dual approval where the database-design statement applies; auditable create/update/approve/finalize actions; atomic and repeatable payroll runs; correct effective-date/rule version reproduction; no silent acceptance of incompatible input schemas; clear errors rather than empty “success”; tested backup/restore; and no original fixtures altered in test workflows.

## What the nine repositories can—and cannot—contribute to this plan

The nine submissions are comparison material, not verified mappings to the MO-IT107 documents. Their provenance, assignment fit, authorship and reuse permission are not established for the current user. The most relevant technical patterns are: F4's CSV/repository/service separation; F6's OOP payroll refactor and 167-check suite; F9's GUI-to-backend split and strongest documented MS1→MS2 trace; F1/F2/F5's database-backed screens; and F3's small procedural demonstrations. F7/F8 illustrate CSV-driven GUI/backend flows. None should be treated as a definitive MotorPH business-rule source. F2/F5 are near-duplicates. The full repository-by-repository analysis and feature pseudocode are appended below.

## Explicit unresolved inputs (recorded as unknown, not requested again)

- Approved gross-pay, overtime, undertime, holiday/rest-day, paid-leave, deduction, contribution, tax and rounding rules; authoritative values and effective dates.
- Attendance device/import source, schedules, time zone, grace periods, duplicate/missing punches, overnight shifts and correction approval.
- Leave types, accrual frequency/rates/caps, carry-over, negative balance, approver, monetisation and payroll treatment.
- Pay-cycle calendar and semi-monthly period boundaries, proration, eligibility, final-pay treatment, corrections/reversals and rerun behavior.
- Exact employee/supervisor/HR/payroll/admin permissions, team scoping, PII field visibility, dual-approval roles and emergency access.
- Confirmed archive retention, cold-storage format, legal holds, restore/deletion authorization and applicable compliance requirements.
- Which of the nine repositories (if any) is the user's approved baseline and whether any source is authorized for adaptation.
- The two business-reference documents cited by the DFD; they were not supplied and the user does not have them.

These are blockers to claiming policy correctness, not questions repeated to the user. The current plan remains useful as a provisional architecture and gap register.

---

# Consolidated source reports and detailed evidence

The following sections preserve the previously prepared repository analysis, initial test record, retest addendum, and feature pseudocode so the details remain available in this single report. Their original standalone files remain in the review folder as well.

## Appendix A — Initial static submission analysis

# `testingester` numbered submissions — structure, behavior, and course links

**Repository reviewed:** `/home/user/repo_review/full_repo/testingester-main/` (export of `Sitramonicus/testingester`). The numbered projects are nested beneath each `N/-/` directory; folder 8 has one more nested copy of its project root. This document records the initial static pass: source layouts, READMEs/setup notes, implementation paths, data-file headers, build metadata, and available test/evidence files. Behavioral testing was subsequently performed from isolated copies; see `/home/user/repo_review/submission-test-results.md` for observed runtime evidence and environment blockers. The test totals cited here as README/log claims were not rerun as part of the static pass. No employee records or passwords are reproduced.

## At a glance

| Folder | What the files identify | Main structure / data store | Course connection and confidence |
|---|---|---|---|
| **1** | Swing employee/payroll manager; no README or reliable course/team label | One NetBeans package; SQLite database; PDF libraries | Conceptually GUI + JDBC + database CRUD. Course/milestone unknown. |
| **2** | README says MO-IT110 OOP, H2101 Group 3 | Swing forms + nominal `Classes` domain layer; local MySQL | README label is explicit; this code is virtually the same implementation as folder 5. Not proof of the user's authorship. |
| **3** | README says COMPROG1 A1101 Terminal Assessment / Phase 1; names an author | Separate console programs; CSV input in one program | CP1/procedural foundations, not a personal CP2 app. The repo does not establish that it is the user's submission. |
| **4** | CP2 Group 4 is indicated by `sources.txt` paths; no README | Maven/Java Swing with controller, service, model, repository and view packages; CSV | Strong conceptual example of GUI/data/service separation and CSV-backed payroll. No explicit MO-IT110 milestone claim. |
| **5** | README says MO-IT103 H1101 Group 1 / Computer Programming 2 | Same Swing/MySQL project as folder 2 | CP2-labeled snapshot; source code is almost identical to folder 2. |
| **6** | README explicitly says MO-IT110 OOP MS2 refactor | Smaller Swing app; separated model/deduction/service/UI; CSV | Explicit MS2 example claiming to implement an MS1-approved design. Authorship/permission still needs confirmation before graded reuse. |
| **7** | No README or reliable course/team label | Swing app, shared static CSV data, attendance CSV; employee CSV is not included | OOP and GUI concepts can be studied, but exact assignment link is uncertain and it is not self-contained. |
| **8** | Nested directory identifies `MO-IT103-Group18`; no README | Console and Swing front ends; backend classes; local CSVs plus an online console data source | CP2-style project with broader payroll/attendance/employee features. Not a direct MO-IT110 milestone artifact. |
| **9** | README explicitly says MO-IT110 OOP Team 3; also identifies its CP2 baseline as another Group 4 project | CP2 GUI in `src/`; OOP backend in `oop/src/`; tests, evidence, notes and QA workbook | Most explicit MS1-to-MS2 mapping in this repo. Still confirm authorship/permission before treating as the user's work. |

## Folder 1 — SQLite/NetBeans Swing payroll manager

**Location:** `1/-/src/EmployeePayrollMph/`. There are 12 Java source files, many NetBeans `.form` files, images and bundled JARs. `build/classes/` also contains compiled `.class` files and form copies; those are build output, not a second source implementation. The project metadata names `EmployeePayrollMph.EmployeePayrollMph` as its entry point.

**How it works:** the entry frame presents a login form. Its handler queries the SQLite `Users` table using a prepared statement, stores the selected user's ID/name in static fields on `Emp`, records a login event in `EmployeeAudit`, then opens `MainMenu`. The menu opens employee management, allowances, salary update, deductions, payment records, search and audit. The included SQLite database schema has `Employee_info`, `Users`, `Allowance`, `Salary_Deduction`, and `EmployeeAudit` tables.

The `Employee` frame performs employee add/update/delete against `Employee_info`, including an image BLOB. `Allowance` reads an employee, calculates an allowance total, and inserts a row into `Allowance`. `SalaryDeductions` inserts a deduction record. `EmployeeSalary` looks up the employee plus stored allowances/deductions to prepare a payslip; the app bundles iText, and the menu has PDF report export handlers. This is therefore more of a database-backed admin CRUD app than a clean payroll-calculation library.

**Caveats:** the SQLite connector uses a hard-coded Windows path under one developer's `C:\Users\...` directory even though database files are included in the project. Several screens are mainly generated Swing code; `EmployeeAttendance` and `EmployeeLeave` exist, but their presence alone does not establish a complete persisted attendance/leave workflow. The login's role check uses Java string identity (`==`) and contains an empty `if (count == 1);` statement, so the code should not be treated as a reliable authentication example without checking/fixing that flow.

**Course connection:** the code is useful for seeing Swing event handlers, JDBC/SQLite, CRUD and report generation. There is no source evidence tying it to CP1, CP2, MS1 or MS2, so I would not label it as your coursework baseline.

## Folders 2 and 5 — one Swing/MySQL app under two different labels

**Locations:** `2/-/` and `5/-/`. Each has 49 Java files, many NetBeans `.form` layouts and image assets, a Maven file, and two third-party time-picker JARs. The active entry point is `main.main`, which opens `Jframes.LogInNew`; `Archived/` contains four older screens.

**How it works:** the login screen queries the local MySQL `user_accounts` table and routes Admin/IT users to `AdminHomeDashboard`, other users to `HomeDashboard`. The employee dashboard links to profile, salary, attendance, leave requests and overtime requests. The admin side links to employee records, salary/attendance, accounts, leave approvals and overtime approvals. The screens issue SQL directly through `Classes.db`, which connects to a local MySQL database named `motorph`. SQL references tables named `employees`, `user_accounts`, `attendance_records`, `salary`, `leave_requests`, and `overtime_requests`.

There is a `Classes/` package with `Employee`, `Salary`, `Attendance`, `Deduction`, `Payroll`, `UserAuthentication`, `Admin`, `HR`, `Manager`, request and task classes. These communicate the intended domain model, but multiple methods are empty placeholders (for example, model-level payroll/CRUD methods). The working behavior is mostly in Swing-frame event handlers. Salary computation logic appears in both `CreateSalary` and `UpdateSalaryAdmin`, which each query attendance and calculate salary/deductions. This is an example of a GUI-heavy project with some OOP-shaped classes, not a fully separated model/service architecture.

**Important duplication finding:** folders 2 and 5 are not two materially different codebases. A recursive comparison found only four differences: `.DS_Store`, `README.md`, and author-comment lines in `Classes/Employee.java` and `main/main.java`. The executable Java logic, forms, assets and build file otherwise match. Folder 5's README labels it MO-IT103 H1101 Group 1 / CP2; folder 2's README labels it MO-IT110 H2101 Group 3 / OOP. That makes them different submission *packagings/labels*, but essentially the same program. The folder 2 label does not by itself demonstrate that this is your approved MS1 design or your authored work.

**Caveats:** the MySQL schema/setup is not included as a self-contained database export; running requires a local `motorph` database and compatible account setup. Some domain-layer methods are stubs, while substantial behavior is coupled to the UI. The credentials are database-backed, but the source uses direct password comparison; this is not a security design to copy.

**Course connection:** folder 5 is explicitly CP2. Folder 2 is labelled MO-IT110 OOP, but the repository does not include an MS1 design package connecting these classes to an approved design. Treat it as a CP2-style app/OOP comparison unless the mentor confirms more.

## Folder 3 — small CP1/terminal-assessment console exercises

**Location:** `3/-/`. The README identifies a COMPROG1 A1101 Terminal Assessment submission and describes a Phase 1 console payroll system. There are seven Java files, employee CSV/TXT samples, and NetBeans metadata. Six Java files are in package `motorph`; `MotorPH_ReadFromFile.java` is in the default package, so it is a separate runnable entry point.

**How it works:** the files are small, independent demonstrations rather than one integrated app. They show fixed-value examples for hours worked, gross pay and semi-monthly salary; a separate class calculates example deductions; the file-reader program opens `employee_data.csv`, reads salary from a fixed column, applies its own sample deductions, and prints a result. `ComputeGrossPay` has a reusable static multiplication method, while the other examples largely hard-code the employee/time/pay inputs in `main`.

**Caveats:** the README describes a longer planned progression, but the source is still a set of small console programs, not a GUI, database-backed system or connected payroll architecture. The deduction rates differ between files and are explicitly example-style flat rates; they are not a safe source for official payroll rules. The folder's named author is not evidence that the current user authored it.

**Course connection:** this is the clearest CP1/procedural foundation in the numbered set: variables, arithmetic, conditions, methods and basic file reading. It is not evidence of a personal CP2 app. Given the user's CP1-to-OOP path, this may be useful as a prior-learning comparison, but do not describe it as their submission without confirmation.

## Folder 4 — layered CP2 Swing/CSV payroll app

**Location:** `4/-/`. It has 50 Java files: 42 under `src/main/java/com/motorph/` and 8 under `src/test/java/com/motorph/`, plus CSV inputs, images, Maven metadata and VS Code settings. `sources.txt` contains Windows paths naming a `CP2_GROUP-4` project. There is no README explaining the feature set or assignment milestones.

**Structure and flow:** `Main.main()` starts the Swing login. On successful login, `initializeApplication()` constructs a `DataRepository`, loads employees and attendance, then wires `EmployeeService`, `PayrollService`, `ReportService` to their controllers and gives those controllers to `MainFrame`. The view is divided into dashboard, employee, payroll, reports and leave panels. The packages are `controller`, `service`, `model`, `repository`, `dao`, `security`, `leave`, `util` and `view` (including dialogs/renderers).

- `DataRepository` reads employee and attendance CSVs, parses dates/times and builds `Employee`/`AttendanceRecord` objects.
- `EmployeeService` searches and manages employee records and writes employee changes back to CSV; attendance lookup is exposed by date/range methods.
- `PayrollService` selects an employee and period, builds a `PaySlip`, and can generate payroll for all employees. `PayrollProcessor` supplies gross-pay, SSS-table, PhilHealth, Pag-IBIG and withholding-tax calculations. `ReportService` builds payslip and weekly/monthly summaries.
- Controllers validate request parameters and delegate to services; Swing panels call controllers.
- The CSV files contain employee details, attendance records and user credentials. The bundled sample files have 35 employee data rows and 5,168 attendance rows (excluding headers); the runtime probe loaded 35 employee rows.

**Caveats:** the repo mixes a reasonably separated architecture with helper/demo code (`EmployeeDAO`, CSV tests and test utilities) that is not obviously part of the main wiring. The leave request classes/panel exist, but the startup wiring shown in `Main` does not construct a `LeaveRequestService` or pass it into the frame. Several test classes are standalone/manual `main` programs, and some use hard-coded Windows paths; the presence of JUnit in the Maven file does not mean the available checks are all JUnit tests. Payroll formulas still need validation against the current approved specification, not merely against code in another folder.

**Course connection:** useful as a CP2 example of Swing + CSV and of separating controllers/services/models/repositories. The course identity is inferred from the source path, not stated in an official README; there is no explicit MS1/MS2 linkage.

## Folder 6 — explicit MO-IT110 MS2 refactor example

**Location:** `6/-/`. Its README names MO-IT110 OOP Milestone 2 and says the project refactors a CP2 Java Swing app into an approved MS1 design. It has 22 production Java files under `src/motorph/`, 10 plain-Java tests under `test/motorph/`, CSV data, CP2 reference outputs, NetBeans metadata and an Ant build file. The declared main class is `motorph.MotorPH`.

**Structure and flow:** `MotorPH` opens `LoginFrame`. UI classes are under `ui/`; the payroll domain is separated into `model/`, `deduction/`, `service/` and `user/`.

1. `CSVDataHandler` loads employees and attendance from CSV and creates model objects.
2. `Employee` is an abstract class; `RegularEmployee` and `ProbationaryEmployee` supply subtype behavior such as allowances.
3. `AttendanceRecord` calculates hours/cutoff membership.
4. `PayrollProcessor` accepts a list of `Deduction` objects (SSS, PhilHealth, Pag-IBIG and tax), totals cutoff hours, calculates gross/deductions/net, and creates an immutable `Payslip`.
5. UI frames display employee/payroll results and save payroll CSV output.
6. `User`, `EmployeeUser` and `PayrollStaff` demonstrate authentication/role permissions for the employee-lookup vs staff screens.

The README says the implementation preserves CP2's cutoff behavior and describes how it rounds money, applies deductions and lists allowances. It also cautions that the salary tables/rules were carried from CP2 rather than independently verified against a current MotorPH/government specification. The README reports 167 checks and 0 failures, with CP2 comparison differences on 3 of 476 rows; those are project-reported results, not runs performed in this review.

**Course connection:** direct MS2 relevance by its own README; the code is a concrete example of implementing an approved design, using encapsulation, inheritance, polymorphism and a view/backend split. The CP2 source commit and authorship/permission should be confirmed before adopting any code or presenting it as the user's work.

## Folder 7 — Swing/CSV payroll app with unclear submission identity

**Location:** `7/-/src/motorphpayrollsystem/`. There are 35 Java files, NetBeans forms/assets, and bundled OpenCSV, Commons Lang and AbsoluteLayout libraries. There is no README, no course/team label, and no CSV data file in the folder. The project main class is `MotorPHPayrollSystem`.

**Structure and flow:** the main class tries to create `attendance.csv`, loads employee data through `EmployeeData`, and opens `loginUI`. Login looks up an employee row in a CSV and compares a password against column 20. On success it opens `DashboardUI`. `EmployeeData` keeps the CSV rows in a static shared list and offers add/update/delete/profile methods that rewrite the CSV. The UI includes employee dashboard/profile, attendance clock-in/clock-out and history, employee records, admin employee add/update/delete, payroll screens and a bulk-payroll/export screen. `Attendance` and `CSVUtil` provide CSV persistence; `Payroll`, `ContributionCalculator`, `PayrollCalculator` and `PayrollProcessor` contain pay logic; `FullTimeEmployee`/`PartTimeEmployee` and `EmployeeMapper` provide model objects.

**What is only partial or unclear:** `MotorPH_Employee Data.csv`, required by `EmployeeData`, is not included. Attendance data is created as an empty CSV when absent. `LeaveRequest` and `OvertimeRequest` are domain classes, but a search shows no leave-request workflow in the screens; the overtime request model is stored on the processor but not used by its main processing method. The payroll processor's comments describe overtime as a temporary/unfinished path. So the classes indicate intended scope, not proof every listed capability is operational.

**Course connection:** its class hierarchy and UI/backend concepts could be compared to OOP course topics, but no source evidence ties it to a particular MO-IT110 module, MS1 design or user's own submission. It is not self-contained enough to treat as a runnable reference without its expected employee CSV.

## Folder 8 — MO-IT103 Group 18 console + Swing project

**Location:** `8/-/MO-IT103-Group18-master/MO-IT103-Group18-master/`. There are 36 Java files under `Backend/`, `Console/`, `GUI/` and one top-level labels file, plus NetBeans/Ant metadata, JARs and three local CSV datasets (employee details, attendance and credentials). No README is included.

**Two front ends use the data differently:**

- `Console.TACP2` is a terminal workflow: login with employee number/last name; view profile, daily attendance, weekly hours and payroll. It downloads published Google Sheets CSV over HTTP, so it depends on internet access and the sheet schema.
- The Swing UI starts from `GUI.LogIn` and proceeds to `GUI.MainMenu`. It uses the bundled local CSVs and has profile, attendance, payroll, employee table and employee add/update/delete screens.

The `Backend/` package contains employee/attendance records, CSV loading, payroll processors (regular, final-pay and 13th-month variants), statutory deduction calculators, regular/holiday overtime classes, plus leave and payslip-dispute domain classes. The regular payroll processor uses attendance totals and a `Payslip`; deduction types are separate classes. The GUI exposes employee dispute submission and a payroll-side review/resolve/reject frame.

**Caveats:** leave records shown by the GUI are explicitly hard-coded sample rows because there is no leave data file. Payslip disputes live in a singleton in-memory list, not persistent storage, so the records disappear on process exit. The payroll-side dispute frame itself notes role authorization is not implemented. The console path fetches external sheets; the GUI path reads local data—do not confuse them as one identical data flow. This is a CP2-labeled comparison project, not an MS1-approved design by evidence in this folder.

## Folder 9 — documented MO-IT110 Team 3 CP2-to-OOP project

**Location:** `9/-/`. This is the most documented folder: README, old `README.CP2.md`, `SETUP.md`, `oop/NOTES.md`, parity/matrix reports, console/test outputs, a QA workbook, and code. It has 15 Java files in the original GUI `src/`, 13 in `oop/src/` (12 backend classes plus `ConsoleBackendTest`), and 3 test/probe files. The README identifies the OOP work as MO-IT110 OOP Team 3, and identifies the CP2 baseline as MO-IT103 Group 4. Those are source-repository statements, not confirmation of the current user's authorship or reuse permission.

**Two connected source roots:**

- `src/motorphemployee/` is the earlier CP2 Swing app (login, landing screen, dashboard, employee forms, leave screens and related UI). Its data remains in CSV.
- `oop/src/motorphemployee/oop/` is the OOP payroll backend. `Person` → `Employee` → `RegularEmployee`/`ProbationaryEmployee` is the data hierarchy; `GovernmentContributionCalculator` has concrete contribution/tax calculators; `AttendanceLog` models attendance; `PayrollCalculator` coordinates employee + attendance + calculators and produces `PayrollRecord`.
- `ConsoleBackendTest` provides a console test harness before GUI integration. The GUI's `MainMenu`/`Dashboard` were rewired to call the OOP backend rather than compute payroll themselves.
- `AttendanceRepository` loads the real timecard CSV and supplies worked-day data to the payroll flow. `LeaveService` processes approval/rejection and writes the related CSV files; `LeaveForm` still has a separate direct append path noted as an open issue.

**How the feature sequence works:** employee/attendance CSV → `AttendanceRepository`/`AttendanceLog` → `PayrollCalculator` → contribution calculators → immutable `PayrollRecord` → GUI display. The README and notes describe preserving the CP2 UI while replacing duplicated calculations with one backend. They also document changes to modal popups, attendance sourcing and the add-employee form; these are later implementation refinements beyond the core OOP concept demonstration.

**The explicit module link:** the README maps W2 to encapsulation, W3 to abstraction, W4 to inheritance and W5 to polymorphism, and describes MS1 as 16 unique classes, 35 CRC rows and 97 method rows. It says MS2 implements 12 of the 16 design classes; `Credentials`, `LeaveRequest`, `LeaveManagement` and `HRManager` remain design-only. It states the GUI should present only, payroll math should live in the backend, and backend console tests should precede GUI integration. The repo includes official-matrix verification and CP2-parity evidence; the README says some deduction results intentionally differ from CP2 because the OOP version follows the official matrices, while gross pay matches the CP2 baseline. Do not transfer those tax/contribution assumptions to your assignment without checking its current approved rules.

The README reports 91/91 backend checks and several other dated test/GUI evidence logs. I have not rerun them. The QA workbook in this folder is named for a CP2 Group 4 QA submission; it should not be mistaken for proof that the OOP source was authored by the current user.

**Course connection:** the strongest visible MS1→MS2 teaching example in the repository because its own README and code comments map the OOP concepts and design artifacts into implementation. It is useful for studying the mapping, not automatically reusable submission material.

## How these projects relate to the course modules

There are two levels of evidence, and they should not be conflated:

1. **Explicit labels in the files:** folder 3 says COMPROG1/Terminal Assessment; folders 4, 5 and 8 are CP2-labeled or path-identified; folders 6 and 9 explicitly describe MO-IT110 MS2/OOP work; folder 2 is labelled MO-IT110 OOP but lacks an included MS1 package tying its classes to an approved design.
2. **Conceptual fit:** CSV and database folders illustrate data/file I/O; Swing folders illustrate GUI event handling; model/service/backend folders illustrate OOP decomposition; folders 6 and 9 demonstrate inheritance/abstraction/polymorphism; tests and comparison reports illustrate verification.

From the MO-IT110 Modules 02–09 handout previously reviewed, the project focus is the MotorPH payroll application and its employee/attendance data; the handout does **not** prescribe one particular MotorPH spreadsheet tab as the project base. The numbered folders themselves generally do not record a reliable one-to-one mapping from a particular feature to a specific module number. For that reason this report maps folders to the explicit milestones/concepts above rather than inventing claims like “folder 4 is Module 6.” The clearest official milestone relationship is folder 9's stated MS1 design → MS2 implementation, with folder 6 as a separate MS2 example.

| Course concept / deliverable | Where it is visible | How to read the connection |
|---|---|---|
| Procedural foundations, arithmetic, conditions and basic file reading | Folder 3 | Small independent console exercises; useful prior-learning material, not a CP2 baseline. |
| GUI screens, event handlers, employee records and data persistence | Folders 1, 2/5, 4, 7, 8 and 9's original `src/` | Different implementations and storage choices; presence does not imply OOP design compliance. |
| CSV/database input and output | SQLite in 1; MySQL in 2/5; CSV in 3/4/6/7/8/9 | Shows data-source variety. The course brief/module instructions still take precedence over old project choices. |
| Separation of UI from payroll logic | Most clearly 4, 6 and 9; partially 7/8 | Compare real call paths, not package names alone. Folders 6/9 explicitly describe a backend/UI split. |
| Encapsulation, abstraction, inheritance, polymorphism | Models in 2, 4, 6, 7, 8, 9; explicitly week-mapped in 9 | For the user's MS1, the approved design artifacts and rubric—not an old implementation's class list—are authoritative. |
| MS1 OOP Design Package (CRC cards, method dictionary, architecture overview) | Folder 9's README describes its own package; folder 6 refers to an approved design | These are other teams' records. Do not copy/adapt as your own without mentor confirmation and permission. |
| MS2 implementation and tests | Folders 6 and 9 explicitly; folder 9 has the clearest audit trail | Useful to understand how design becomes code; check current assignment rules and authorship first. |
| Terminal Assessment / final QA and documentation | Folder 3's README calls itself a Terminal Assessment; folder 9 reports TA work still open | These labels differ in course/phase and should not be treated as the same assignment. |

## Cross-folder conclusions and safe study order

- **Folders 2 and 5 are the same practical implementation** despite their different README labels; study one, not both, unless comparing the packaging/attribution differences.
- **Folders 4, 5 and 8 are alternative CP2-style systems**, not evidence of the user's personal CP2 app. Their persistence choices differ (CSV vs MySQL vs CSV/online sheet), and their features are not interchangeable.
- **Folder 9 is the best source here for tracing explicit MS1 concepts into MS2 code**, while folder 6 is a smaller, separate implementation example of the same design-to-implementation idea. Both need authorship/permission confirmation before graded use.
- **Folder 3 is a separate CP1-style reference**, not proof that the user has a CP2 app.
- **Folders 1 and 7 have unclear provenance/course stage.** Use them only as generic technical comparisons until a mentor or repository author confirms their context.

Suggested reading order for understanding—not copying—is: **3** (small console building blocks) → **one CP2 project** (4, 5 or 8; folder 4 is the clearest package-separated example) → **6** (smaller MS2 separation) → **9** (most explicit MS1-to-MS2 mapping and evidence). For your own MS1, start from the official rubric and your actual approved project requirements; use another folder only as a comparison after confirming permission and fit.

## Review boundary

This is a static repository explanation, not a correctness audit or a claim that every advertised feature runs. I read source and metadata but did not execute the projects. Several folders have external setup/data dependencies; exact runtime behavior and payroll-rule correctness require separate, course-specific validation. The repository remains unchanged; this report is the only new deliverable.

## Appendix B — Initial behavioral test results

# Behavioral test results — numbered submissions 1–9

**Purpose:** companion to [`submission-folder-analysis.md`](submission-folder-analysis.md). This document records the initial test pass. The previously blocked/incompatible cases were then re-tested under prepared runtime conditions; see [`submission-retest-addendum.md`](submission-retest-addendum.md) for the current results and the isolated-copy adaptations. No original repository source was edited. All runs that could write data used isolated copies; the uploaded inventory CSV was copied and kept unchanged. No employee names, credentials, or raw payroll rows are reproduced here.

## Executive summary

- **Best verified MS2 evidence:** folders **6 and 9**. Folder 6's bundled backend suite passed **167/167** checks. Folder 9 passed **91/91** payroll/backend checks, **33/33** attendance checks, and **44/44** leave checks (**168 checks total**) when run with `Asia/Shanghai`, matching the user's local timezone. A first folder 9 run under the sandbox's UTC default was **90/91** because the test compared `LocalDate.now()` in UTC with code using `Asia/Manila`; setting the JVM timezone to Asia/Shanghai or Manila made it pass.
- **Important real input defect:** folder 3's bundled employee CSV run completed, but **all 34 reported gross-pay values differed** from the salary column at index 17 when that CSV was parsed correctly. Its reader uses plain `String.split(",")`; every row has a quoted address containing a comma, shifting the assumed field positions. The selected inventory CSV also crashes this reader with `ArrayIndexOutOfBoundsException`.
- **Inventory CSV compatibility is poor by design**: the selected file has 49 records and five inventory fields, not employee/payroll/attendance fields. Folder 4 and 6 returned empty datasets for incompatible roles; folder 7 padded its 49 rows then failed during employee mapping; folder 8's production loader hit a `NullPointerException`; folder 9's attendance repository rejected the header with an `IOException`. Folders 1, 2, and 5 have no CSV input path to test.
- **Environment blockers:** OpenJDK 11 only, Maven unavailable, no GUI display, and no local MySQL server on port 3306. This prevented full integrated GUI/database testing for several folders. These are distinguished below from defects observed in executable code.

## Test conditions and interpretation

- The uploaded inventory CSV is used **only as the user's requested cross-app compatibility/adversarial input**, not as payroll data. A payroll application not accepting it is not by itself a payroll defect; silent empty data, unchecked exceptions, or misleading success are recorded as observed error-handling behavior.
- “Passed” means the named executable/test returned success or its explicit checks passed. It does not establish that payroll formulas conform to a current official MotorPH specification; no external approved formula oracle was provided.
- “Observed” is based on a run. “Source-backed inference” is a conclusion from inspected code that could not be executed. “Blocked” means unavailable under the current runtime/GUI/database setup.

## Results matrix

| Folder | Executed bundled behavior | Inventory CSV result | Run status |
|---|---|---|---|
| 1 | 12 Java sources compiled when a matching `AbsoluteLayout` JAR from another submission was supplied on the temporary classpath. SQLite DB was inspected read-only; table row counts were 3 allowances, 7 audit rows, 12 employee rows, 9 salary-deduction rows, and 8 users. | No CSV interface. | GUI start hit `HeadlessException`; database connection/event flows not end-to-end tested. The dependency supplied for compilation is not bundled in folder 1. |
| 2 | No application test run. Maven is absent, the project declares Java 21 while only Java 11 is installed, and no MySQL server listens on localhost:3306. | No CSV interface. | Blocked: GUI, required runtime/dependencies, and DB unavailable. |
| 3 | Four calculation/demo entry points and the bundled employee-file reader exited successfully. Reader processed 34 employee markers. Comparison found 34/34 gross outputs differ from properly parsed salary field 17. | `ArrayIndexOutOfBoundsException`. | Runnable terminal code, but bundled CSV parsing is observably misaligned. |
| 4 | A data/payroll subset compiled against Commons CSV 1.9.0. `DataRepository` returned 35 employee rows and 5,168 attendance rows from bundled fixtures. | Inventory as employee file: 0 employees. Inventory as attendance file: 0 attendance rows. | Full app not built: project targets Java 17, but Java 11 lacks the `Stream.toList()` API used by `PayrollService`; Maven and other dependencies are unavailable. |
| 5 | No application test run; same environment constraints as folder 2. Source review shows it is near-duplicate of folder 2. | No CSV interface. | Blocked: Java 21/Maven/MySQL/GUI unavailable. |
| 6 | `motorph.AllTests`: **167 passed, 0 failed**, exit 0. | As employee input: 0 employees; as attendance input: 0 groups/rows. No exception. | Backend compiles and runs. Incompatible input is silently skipped, which can look like a successful empty payroll. |
| 7 | `PayrollBackendTest`: four basic checks passed (regular pay, gross pay, net pay, overall backend checks). | Loader read 49 records and padded each data row to 21 fields; `EmployeeMapper.fromCSV` then threw `NumberFormatException` on inventory text used as an employee number. | Backend runnable; no bundled employee CSV and GUI not exercised. |
| 8 | Bundled loader main exited 0 and reported 34 employees. A direct probe resolved an employee object and 152 attendance rows for the first loaded ID. The dispute demo exited 0, but it is a demonstration flow, not an assertion-based test suite. | Inventory as employee input: 49 raw IDs but no employee object; production loader main then threw `NullPointerException`. As attendance input: 0 matching attendance rows. | Backend runnable. GUI not exercised; local/online input split remains a significant integration boundary. |
| 9 | `ConsoleBackendTest`: **91/91** under `Asia/Shanghai` or `Asia/Manila`; `AttendanceRepositoryTest`: **33/33**; `LeaveServiceTest`: **44/44**. | The attendance repository rejected the inventory header with an explicit `IOException` for an invalid employee number. | Backend/services compile and pass when timezone is aligned. GUI/24-column employee-table loading not exercised because no display. |

## Folder-by-folder observations

### Folder 1 — SQLite Swing CRUD

**Observed:** The sources compile with a matching NetBeans `AbsoluteLayout` library supplied only on the test classpath. The bundled SQLite file is populated. The GUI entry point cannot be meaningfully exercised in this headless environment; startup reaches a Swing `HeadlessException`.

**Data flow:** SQLite is the persistent store; UI actions issue database work and populate UI fields/tables. The read-only table counts confirm the database is not an empty placeholder. The source's hard-coded Windows database path is a portability risk, but its behavior against the supplied database path was not tested end-to-end here.

**Limit:** No CSV interface. The test does not verify CRUD integrity, authentication, or payroll calculations through the UI.

### Folder 2 — MySQL Swing application

**Blocked:** Its Maven metadata targets Java 21; this environment has Java 11 and no Maven. No MySQL listener exists on localhost:3306, and Swing cannot be displayed. There are no bundled CSV fixtures to substitute. Do not interpret the lack of runtime results as proof that its SQL flows fail.

**Source-backed inference:** UI event handlers issue SQL through the DB helper; model classes include incomplete/stub methods and salary calculations are duplicated in GUI code. Folder 5 is nearly the same implementation, so the two should not be treated as independent design evidence without confirming their provenance.

### Folder 3 — procedural console calculations and CSV reader

**Observed:** `CalculateHoursWorked`, `ComputeGrossPay`, `ComputeSemiMonthlySalary`, and `MotorPH_ApplyDeductions` all exited 0 on their built-in examples. The bundled `MotorPH_ReadFromFile` processed 34 rows without an exception.

**Observed defect:** The reader's naive comma split is not quote-aware. Every bundled row contains a comma in the quoted address cell. Comparing its 34 printed gross-pay outputs with the correctly parsed CSV salary column at index 17 produced **0 matching values out of 34**. Thus an exit code of zero is not evidence of correct field selection or payroll output.

**Inventory test:** Replacing the expected input with the five-column inventory file caused `ArrayIndexOutOfBoundsException`. This is a schema assumption/validation failure, not a payroll formula result.

**Data flow:** Each run reads CSV lines into arrays and calculates/prints values in process memory; no durable payroll result store was exercised.

### Folder 4 — layered Swing/CSV CP2 application

**Observed:** The main `DataRepository`/model/processor subset compiled with the project's Commons CSV 1.9.0 dependency and loaded **35 employee rows** and **5,168 attendance rows** from bundled data. An isolated probe substituting the inventory file returned 0 employees when used as employee data and 0 attendance rows when used as attendance data.

**Blocked:** The project targets Java 17. The available Java 11 cannot compile `PayrollService`, which uses `Stream.toList()` (introduced after Java 11). Maven is absent; the full source/test build was not completed. Therefore repository loading was exercised, but service/controller/GUI integration was not.

**Data flow:** CSV rows are parsed into `Employee` and `AttendanceRecord` collections by `DataRepository`; payroll services operate on those objects and other services write CSV. The inventory probe suggests incompatible header/column names result in empty lists rather than a clear schema error. This risks a blank/zero payroll being mistaken for valid input.

### Folder 5 — second MySQL Swing implementation

**Blocked:** Same Java 21/Maven/GUI/MySQL constraints as folder 2. No CSV fixtures or CSV input path. No behavioral outcome is claimed.

**Comparison:** Static review found it nearly identical to folder 2 despite different README wording. Because both use local MySQL and have no attached test data, mentor confirmation is needed before treating either as the authoritative or reusable project.

### Folder 6 — explicit MS2 OOP refactor

**Observed:** The full source compiled under Java 11. `motorph.AllTests` exited 0 with **167 passed, 0 failed**. This is the strongest single bundled suite result among the folders in this run.

**Inventory test:** `CSVDataHandler` accepted the file operation but returned **0 employees** when inventory was supplied as employee data, and **0 attendance groups/rows** when supplied as attendance data. No exception or useful error was raised in the probe. The parser's documented shape is 19 columns for employees and 6 for attendance; the uploaded file has five different fields.

**Data flow:** CSV data is loaded into employee/attendance objects and collections; payroll computations and role checks operate in memory; result persistence writes output CSV. The tests validate much more than a simple “main exits” check, but the inventory result exposes a silent-empty-data hazard.

### Folder 7 — Swing/CSV payroll backend

**Observed:** The four reported backend checks all passed. The required employee CSV is absent from the submission, so normal login and UI flows were not exercised.

**Inventory test:** `EmployeeData` loaded all 49 records and padded them to 21 fields. This lenient width-padding masks the schema mismatch at the file-loading stage. The next step, `EmployeeMapper.fromCSV`, attempted to interpret inventory text as an employee number and threw `NumberFormatException`.

**Data flow:** `EmployeeData.records` is a static shared in-memory list. UI/backend operations modify rows and rewrite CSV. The observed load-then-map failure shows that padding to a target width is not sufficient field validation.

### Folder 8 — mixed console/GUI implementation

**Observed:** The bundled data loader reports 34 employees. A direct backend probe resolved a first employee object and found 152 attendance rows for that ID. The included `TestPayslipDispute` class ran its create → review → resolve demonstration and exited 0, but contains no assertions and should not be counted as an automated test suite.

**Inventory test:** The raw loader returned 49 IDs from the inventory's first column but could not create an employee object. The production `PayrollDataLoader` main assumes a particular employee (10001) exists and dereferences the missing object, producing `NullPointerException`. Substituting inventory for attendance while retaining employee data returned zero matching attendance rows.

**Data flow:** Local CSVs populate objects/maps for the GUI/backend, while some console workflows use online data. That split means a local fixture passing does not prove console and GUI workflows share the same source or logic. The inventory scenario demonstrates an unchecked missing-record assumption.

### Folder 9 — MO-IT110 OOP payroll/attendance/leave project

**Observed:** The non-GUI backend and safe services/tests compile under Java 11. With the JVM timezone set to `Asia/Shanghai` (the user's timezone), payroll/backend checks passed **91/91**, attendance checks **33/33**, and leave checks **44/44**. The full test set totaled 168 passing checks.

**Timezone-sensitive test:** With the sandbox's default UTC timezone, the payroll suite initially returned **90 passed, 1 failed**. The failing check was “the default period ends today”: the test uses `LocalDate.now()` in the JVM default timezone while production calculation uses `Asia/Manila`. At the test time UTC was still November 2 while Manila/Shanghai had reached November 3. Aligning the JVM to Asia/Shanghai or Asia/Manila made all 91 checks pass. This is an environment-sensitive test, not a payroll-calculation failure under the user's local date.

**Inventory test:** The actual `AttendanceRepository` rejected the inventory file at line 1 with `IOException` because its first field was not a valid employee number. This is clearer than silently returning empty records, though it is not a generic CSV import failure—the file is the wrong schema. The folder's separate Swing employee-table path expects a much wider legacy data file; that GUI path was not run headlessly.

**Data flow:** Attendance rows are read into repository records; payroll results are calculated by OOP services; leave operations read/write their CSV fixtures. The suite coverage spans payroll, attendance, and leave rather than only isolated arithmetic.

## Comparative judgment (scope-aware)

| Question | Best evidence in this run | Caveat |
|---|---|---|
| Which MS2 candidate has the most convincing executable test evidence? | **Folder 9**, narrowly: 168 passing checks across payroll, attendance, and leave; folder 6 is close at 167/167. | Counts are not a common standardized metric. Folder 9 needs timezone alignment, and neither test total proves formula compliance with an external specification. |
| Which is easiest to run locally from the checked-in backend fixtures? | **Folder 6** and **folder 9** both run well in this environment; folder 6's suite passes without timezone adjustment. | Neither Swing GUI was end-to-end tested; folder 6 silently accepts incompatible schemas as empty data. |
| Which rejects malformed attendance input most explicitly? | **Folder 9** (`IOException` with line context). | This reports an invalid row but does not make the inventory CSV an attendance file. |
| Which has a concrete observed CSV correctness defect? | **Folder 3**: all 34 gross outputs disagree with the correctly parsed salary field because comma-splitting shifts columns. | This applies to the bundled CSV reader, not the separate arithmetic demo methods. |
| Which appears most cleanly layered from source plus runtime evidence? | **Folders 4, 6, and 9** show repository/service/model separation; 6 and 9 have executable tests. | Folder 4 full integration was blocked by Java 17 requirement; folder 9's desktop wiring was not run. |
| Which are least substantiated at runtime? | **Folders 2 and 5** (environment/DB unavailable), then 1 (GUI/DB flow unavailable) and 4 (partial core only). | Lack of a runnable environment is not evidence of a defective implementation. |

**Provisional MS2 takeaway:** Folder 9 is the strongest *feature-coverage* candidate in this test pass because its checked suite spans payroll, attendance, and leave, and it has explicit repository/service boundaries. Folder 6 is nearly tied, simpler to run here, and has a clean 167/167 suite, but its bad-input probe silently produced empty collections. This is not a definitive “best project” verdict: test totals and architecture do not establish correct authorship, assignment fit, or permission to reuse code. Confirm with the mentor which repository is the intended MS2 reference and what reuse is allowed before adopting anything.

## Remaining untested items

1. Launch Swing screens on a desktop-capable system and verify actual form wiring, authentication, exports, dialogs, and navigation.
2. Run folders 2 and 5 under their declared Java 21/Maven setup with a disposable, correctly seeded MySQL database.
3. Run folder 4 under Java 17+ with all declared dependencies, then test controller/service validations and CSV writes in an isolated directory.
4. Verify payroll formulas against the current official rubric/specification, not by copying expected output from another submission.
5. For any future CSV interchange test, use a payroll/attendance CSV fixture for functional correctness; keep the inventory CSV only as a deliberate wrong-schema robustness test.

## Appendix C — Retest addendum and isolated-copy change log

# Retest addendum — supported inputs and prepared runtime

This addendum updates the first-pass findings in [`submission-test-results.md`](submission-test-results.md). It addresses the user's request to retest unvalidated cases using each app's expected interface, and to make temporary adaptations where needed.

## Safety and test setup

- Fresh copies of folders 1–9 are under `/home/user/repo_review/test_runs/compat_retest/`. The source export at `/home/user/repo_review/full_repo/testingester-main/` and the uploaded inventory file were not modified.
- A Java 21 JDK, Maven, and Xvfb were provisioned for the retest. A disposable MariaDB 11.8.6 server and synthetic schema/data were used for folders 2 and 5; that server has been stopped. This is a **MySQL-protocol compatibility test**, not proof against a native MySQL server/version.
- Temporary source/config edits were confined to the fresh test copies and were limited to connection paths/settings or test-harness compatibility. Test fixture rows contain synthetic QA values. Product behavior findings below distinguish these setup changes from application-source defects.
- The inventory CSV remains a deliberate wrong-schema probe. For functional checks, I used bundled fixtures or synthetic data matching each program's interface.

## Retest results

| Folder | Supported-interface retest | Outcome |
|---|---|---|
| **1** | Compiled all 12 Java sources with Java 21 and the matching NetBeans `AbsoluteLayout` dependency. In the isolated copy, redirected the SQLite URL to its copied database and added one synthetic account; ran under Xvfb. | Login reached `MainMenu` with no uncaught exception when `src` was included on the classpath for assets. SQLite login/audit path was exercised. The project still is not self-contained with respect to its layout dependency and relies on a hard-coded Windows path unless configured. |
| **2** | `mvn test` completed successfully under Java 21; there are no Surefire test reports. Repointed only the copied DB helper to an isolated MariaDB schema and seeded synthetic rows. Automated a valid Admin login and clicked the six dashboard areas. | Login reached `AdminHomeDashboard`; **Employees Data**, **Salary**, **Attendance**, **Leave Request**, **Overtime Request**, and **User Accounts** screens opened. The five data-list screens showed the synthetic row; attendance opened its two calendar tables. This establishes connection/navigation/basic query behavior only—not create/update/delete or formula correctness. |
| **3** | Replaced only the copied `employee_data.csv` with one 19-column synthetic row matching the reader's field positions and without a comma inside the address. | Reader exited 0 and returned the expected gross semi-monthly field value (**17,500.0**) for that row. This demonstrates the intended simple schema works. The previous bundled-file discrepancy remains: quoted commas in all 34 bundled addresses shift the naive `split(",")` columns, so 34/34 gross values still disagree with the correctly parsed salary field. |
| **4** | Under Java 21/Maven, adapted two **test-only** source files in the copy: typed empty attendance lists as `List<AttendanceRecord>` and used reflection in a test helper to call a private CSV-save method. Copied bundled images to the lowercase classpath location the app requests. Used a one-user synthetic credentials fixture and the bundled employee/attendance data. | Main and tests compile; `mvn test` exits 0 but reports **0 automated tests**. Six manual CSV/service test mains passed, including a CSV write using the app's `done` terminator. GUI login reached `MainFrame`. The original source's mixed-case resource directory (`Resources/Images`) does not match its lowercase `/images/...` lookup on this case-sensitive runtime; only the test classpath was adapted. The inventory file still produces empty incompatible datasets. |
| **5** | Same approach as folder 2, in its own copied project and same disposable database. | Maven build/test exits 0 with no test suite. Synthetic login reached `AdminHomeDashboard`; the same six navigation screens opened with matching seeded data. The near-duplicate implementation should not be counted as independent evidence. |
| **6** | Recompiled all production and test sources with Java 21, reran `motorph.AllTests`, and started the GUI under Xvfb. | **167 passed, 0 failed**; GUI `LoginFrame` opened. The wrong-schema inventory outcome remains 0 employees/0 attendance without a helpful error. |
| **7** | Compiled all 35 Java sources with Java 21. First supplied a compatible 19-column employee file from folder 6 and verified mapping; then used a synthetic header-plus-data-row fixture with the expected password field to exercise GUI login. | The 19-column file mapped **34/34 rows** after the loader padded rows to 21 fields. The expected-format synthetic login reached `DashboardUI`. The earlier inventory result remains: padding accepts the five-field rows, but employee mapping later throws `NumberFormatException`. |
| **8** | Compiled all 36 Java sources with Java 21. Used the bundled employee-details CSV and derived a valid employee-number/last-name login pair internally (no values printed). | GUI login reached `MainMenu`; bundled loader found 34 employees. The original inventory substitution still leaves no matching employee and the production loader's unguarded missing-employee dereference still throws `NullPointerException`. |
| **9** | Compiled all 28 production/OOP sources and the two service test classes with Java 21. Reran suites with the JVM timezone set to `Asia/Shanghai`; launched the main table screen with bundled data. | Payroll/backend **91/91**, attendance **33/33**, leave **44/44**; `MainMenu` opened with **34 table rows**. The single UTC-vs-Manila date-sensitive check still requires timezone alignment. Inventory remains explicitly rejected by the attendance repository as an invalid employee-number row. |

## What changed—and what did not

**Temporary test-copy adaptations:**

- Folder 1: SQLite URL redirected to the copied database; a synthetic login row was added to the copied database.
- Folders 2 and 5: local database URL/user settings changed in the copied `Classes/db.java`; synthetic schema/data was created in the disposable MariaDB database. The dashboard needed a `profilePicturePath` column, which was added to that test schema after the first login exposed the missing column.
- Folder 4: only two test-helper Java files were adjusted to compile against the production API; image resources were placed in the requested classpath location; the copied credentials CSV was replaced with synthetic QA credentials.
- Folders 3 and 7: only copied CSV fixtures were adapted to match their stated layouts. No payroll calculation/parser source was fixed.
- Other folders: no application source edits were needed for these retests.

**Not changed:** original repository files, original fixtures, and the uploaded inventory CSV. The test adaptations are not proposed product fixes and should not be copied into any candidate submission without separate review.

## Updated interpretation

The earlier setup blockers for Java version, Maven, and headless Swing were removable: supported JDK 21/Maven builds and GUI smoke flows now work for the previously blocked projects. Folder 2 and 5 additionally require a database; their navigation works against the disposable MariaDB schema, but native MySQL-specific differences and actual CRUD/payroll writes remain unverified. Folder 4's manual tests and GUI can run once its test harness/resource layout is accommodated, but Maven itself still runs **zero automated assertions**.

The inventory outcomes remain useful robustness evidence, not valid payroll baselines. On correct-format data, folders 1, 2, 4, 5, 7, 8, and 9 now reach representative application screens; folder 6's suite remains clean; folder 3 works on a simple compatible row but misreads its own bundled CSV because of quoted commas. No failure was hidden by changing production code.

## Remaining focused checks

1. For folders 2 and 5, repeat against the intended MySQL version and a schema confirmed by the mentor; exercise salary creation/update and leave/overtime writes, then verify persistence and rollback/error cases.
2. For folder 4, add real automated assertions only if authorized; validate payroll against the approved rubric. Fixing its resource-path case mismatch would require a separate code-change decision.
3. For folders 1, 6–9, use controlled GUI actions to verify representative writes/exports, not just login/navigation. Keep them on isolated copies.
4. Preserve the folder 3 quoted-comma finding and test boundary rows (empty/zero/negative rates and truncated records) separately; do not “fix” the parser in the analysis copy before measuring the original behavior.
5. Before adopting code from any numbered repository, confirm authorship, assignment fit, and reuse permission with the mentor.

## Appendix D — Feature-by-feature pseudocode for folders 1–9

# How the nine submissions work — feature pseudocode

This document turns the current repository/retest consensus into plain-language pseudocode. It explains what the checked-in code appears to do; it is **not** a proposed design, a code fix, or proof that every path works. The full tests and caveats are in [`submission-test-results.md`](submission-test-results.md) and [`submission-retest-addendum.md`](submission-retest-addendum.md).

## Current consensus and labels

- The numbered folders are repository candidates, **not confirmed as the user's own submissions**. Do not claim authorship or reuse permission without mentor confirmation.
- Folders **2 and 5 are effectively the same program** with different README/submission labels; their core pseudocode is shared below.
- Folders **6 and 9** are the strongest explicit MO-IT110 MS2 examples: 6 describes a smaller OOP refactor; 9 documents the most explicit CP2/MS1-to-MS2 mapping.
- The uploaded inventory CSV is a five-field inventory file, **not payroll or attendance data**. Its mismatched-schema outcomes are robustness observations, not proof that a payroll feature should accept inventory rows.
- `[RUN]` means a supported path was exercised in the retest; `[SOURCE]` means the flow is visible in code but not fully exercised; `[PARTIAL]` means present but unwired/incomplete or known to have limits.

Pseudocode uses generic operations such as `readCSV`, `queryDB`, and `showScreen`; it omits Java/Swing syntax and exact tax rates. Exact payroll formulas must be checked against the course's approved rules.

---

## Folder 1 — SQLite Swing employee/payroll manager

### 1. Start and login `[RUN, with isolated DB path]`

```text
START app
OPEN SQLite connection
SHOW login form

ON Login:
    read username, password, selected division
    query Users for a row matching all three values
    if a row is found:
        store its ID and username in shared Emp fields
        count matching rows

    if selected division is "Admin":
        if count == 1:
            do nothing                 // actual source has an empty if-body
        show success dialog
        open MainMenu
        insert an EmployeeAudit event
    else:
        show login-failure dialog
```

**Important:** this mirrors the source, including the empty `if (count == 1);` and a string-identity role comparison. Do not read it as secure or reliable authentication logic. The retest used a synthetic user and a redirected SQLite path in a copy.

### 2. Employee directory `[SOURCE]`

```text
ON employee screen open:
    query Employee_info
    populate the table/form

ON Add:
    validate/read form fields and optional image
    INSERT employee fields into Employee_info
    refresh table

ON Select row:
    copy row values into the form

ON Update:
    UPDATE Employee_info for selected ID
    refresh table

ON Delete:
    confirm deletion
    DELETE Employee_info row for selected ID
    refresh table
```

### 3. Allowance entry `[SOURCE]`

```text
read employee ID and allowance inputs
look up employee/rate information
calculate allowance total from the entered allowance components
INSERT allowance details and total into Allowance
show saved result
```

### 4. Salary deductions `[SOURCE]`

```text
read employee ID, reason, and deduction amount
INSERT a Salary_Deduction row
refresh or display the deduction record
```

### 5. Payslip and PDF/report screens `[SOURCE]`

```text
read selected employee ID
query Employee_info, Allowance, and Salary_Deduction records
combine stored pay, allowance, and deduction values for display
render payslip
if PDF export requested:
    format payslip/report with bundled PDF library
    save output file
```

### 6. Attendance and leave screens `[PARTIAL]`

```text
open attendance or leave screen from menu
show its form/table controls if present
```

The presence of these screens does not establish a complete persisted attendance/leave workflow; the review did not find an equivalent complete data path for them.

---

## Folders 2 and 5 — same Swing/MySQL program

The same pseudocode applies to **folder 2** (README-labelled MO-IT110 OOP) and **folder 5** (README-labelled MO-IT103 CP2). The labels differ; the executable implementation is nearly identical. The temporary retest used MariaDB in MySQL-compatible mode and synthetic rows; it verified login/navigation and basic reads, not full writes or formula correctness.

### 1. Login and role routing `[RUN]`

```text
OPEN LogInNew
ON Login:
    read user ID and password
    use prepared query against user_accounts
    if matching row exists:
        read userType
        if userType is Admin or IT:
            open AdminHomeDashboard
        else:
            open HomeDashboard
        hide login screen
    else:
        show invalid-login dialog
```

### 2. Employee dashboard `[RUN for navigation; deeper actions SOURCE]`

```text
show employee home screen
when Profile chosen: query/display that user's profile
when Salary chosen: query/display stored salary/payslip rows
when Attendance chosen: query/display that employee's attendance
when Leave Request chosen: open leave request form/history
when Overtime Request chosen: open overtime request form/history
```

### 3. Admin employee/account management `[RUN for opening screens]`

```text
query employees or user_accounts
populate admin table
on add/update/delete action:
    collect selected/form values
    execute corresponding SQL statement
    refresh table or show result
```

### 4. Admin attendance and salary `[RUN for opening screens; calculations SOURCE]`

```text
select employee and payroll month/period
query attendance_records for matching employee/date range
sum worked time and late time using screen-level logic
query employee salary/allowance values
calculate gross pay and deductions in the UI handler
if creating salary:
    INSERT salary result
if updating salary:
    UPDATE salary result
show/refresh salary table
```

The salary logic is duplicated between the create/update screens rather than being centralized in a tested service.

### 5. Leave and overtime requests/approvals `[RUN for opening screens; deeper actions SOURCE]`

```text
Employee request flow:
    read request type, dates/times, reason
    INSERT request in leave_requests or overtime_requests

Admin review flow:
    query pending requests
    select a request and decision
    UPDATE its status/approval fields
    refresh request list
```

`Classes/` contains domain-shaped Employee/Salary/Attendance/Payroll/role classes, but some methods are stubs; most active behavior is in Swing handlers. Maven builds passed in the retest, but there are no Surefire tests in these projects.

---

## Folder 3 — independent CP1 terminal exercises

These are separate small programs, not one combined payroll application.

### 1. Worked-hours demo `[RUN]`

```text
set the demo clock-in/clock-out values
apply the program's time-difference/lunch logic
print worked hours
```

### 2. Gross-pay and semi-monthly-pay demos `[RUN]`

```text
set demo hours and hourly/salary values
gross = demo pay rate × demo hours
apply the program's semi-monthly-period calculation
print the result
```

### 3. Deduction demo `[RUN]`

```text
set demo gross pay
calculate the listed sample deductions using that file's fixed rates
net pay = gross pay - total sample deductions
print each result
```

Different files use different example rates; these are not a verified official formula set.

### 4. CSV payroll reader `[RUN; known defect on bundled CSV]`

```text
open employee_data.csv
skip/read header as implemented
for each row:
    cells = split line on every comma       // not quote-aware
    salary = cells[17]
    parse salary and calculate/print payroll summary
```

A simple 19-column row without an embedded comma produced the expected salary field in retest. The bundled file's quoted addresses contain commas, so this algorithm shifts the fields and yields incorrect gross values for all 34 bundled rows.

---

## Folder 4 — layered Swing/CSV payroll application

### 1. Login and app wiring `[RUN with synthetic credentials]`

```text
start Login frame
load credentials from userCredentials.csv
on valid login:
    create DataRepository
    load employees and attendance into model objects
    create EmployeeService, PayrollService, ReportService
    create controllers around those services
    open MainFrame and pass controllers to views
```

### 2. Employee search and maintenance `[SOURCE; manual service tests passed]`

```text
read employee rows through DataRepository
search by employee/date/criteria in EmployeeService
on add/update request:
    validate fields
    modify in-memory employee collection
    serialize collection back to employee CSV
refresh view
```

### 3. Attendance lookup `[SOURCE]`

```text
parse attendance CSV rows into AttendanceRecord objects
for requested employee/date or date range:
    filter records
    return matching attendance to controller/view
```

### 4. Payroll for one employee or all employees `[SOURCE]`

```text
select employee and pay-period start/end
retrieve matching attendance
calculate gross pay from employee rate and worked time
calculate statutory deductions (SSS table, PhilHealth, Pag-IBIG, tax)
net = gross + applicable allowances - deductions
create PaySlip
if batch requested:
    repeat for each employee
return payslips to view
```

### 5. Payslip and period reports `[SOURCE]`

```text
choose employee/period or weekly/monthly summary
ask PayrollService/ReportService for calculated data
format one payslip or aggregate report
show it in report/payslip panel
```

### 6. Leave screen `[PARTIAL]`

```text
open leave-related panel if selected
```

Leave-related classes/panel exist, but startup wiring did not connect a LeaveRequestService to the frame in the reviewed source. The retest reached MainFrame, but did not establish a complete leave submission/approval path. Maven reported zero automated tests; six manual helper programs passed after test-only compatibility adjustments.

---

## Folder 5 — MySQL application

See **Folders 2 and 5** above. Folder 5 uses the same feature pseudocode and active Java behavior as folder 2; it is repeated here as a separate submission identity only because its README/course label differs. Its copied app connected to the disposable DB and opened the same six admin areas. Do not count its runtime result as independent implementation evidence.

---

## Folder 6 — explicit MS2 OOP payroll refactor

### 1. Startup, CSV loading, and login `[RUN: 167/167 tests; LoginFrame opened]`

```text
start MotorPH
CSVDataHandler loads employee CSV into Employee objects
CSVDataHandler loads attendance CSV into AttendanceRecord objects
show LoginFrame
on successful login:
    determine user/role
    show permitted employee or staff screens
```

### 2. Employee subtype behavior `[SOURCE]`

```text
Employee is the shared abstract/base type
if employee is RegularEmployee:
    use regular-employee pay/allowance behavior
else if employee is ProbationaryEmployee:
    use probationary-employee behavior
return behavior through the shared Employee interface
```

### 3. Attendance and pay-period aggregation `[RUN through suite]`

```text
for each attendance record belonging to employee and cutoff:
    determine if record belongs to cutoff
    accumulate worked hours/days and applicable time values
return cutoff totals
```

### 4. Payroll and deduction composition `[RUN through 167-check suite]`

```text
receive employee, attendance list, cutoff, and list of Deduction calculators
hours = aggregate matching attendance
gross = calculate employee/cutoff pay, including subtype allowance behavior
for each deduction calculator (SSS, PhilHealth, Pag-IBIG, tax):
    deductionAmount = calculator.calculate(gross, employee/context)
net = gross - sum(deductionAmount)
create immutable Payslip with period, gross, deductions, net
```

### 5. Role permissions and result persistence `[SOURCE]`

```text
if user is EmployeeUser:
    allow self/profile and permitted own-payroll view
if user is PayrollStaff:
    allow staff payroll view/save operations
when save requested:
    serialize calculated payroll results to payroll-results CSV
```

The retest confirmed the suite still passes 167 checks under Java 21. Its calculations should still be checked against the currently approved rubric; the test pass is internal consistency evidence, not a compliance ruling.

---

## Folder 7 — Swing/CSV payroll system

### 1. Startup and employee CSV loading `[RUN with expected-layout fixture]`

```text
if attendance.csv is absent/empty:
    create it with expected attendance header
load MotorPH_Employee Data.csv with OpenCSV
for each employee row shorter than 21 fields:
    pad missing fields with empty strings
store rows in static EmployeeData.records
open loginUI
```

### 2. Login `[RUN: compatible synthetic row reached DashboardUI]`

```text
read employee number and password
for each data row after the header:
    if row[0] matches employee number
       and row[19] (20th field) matches password:
        open DashboardUI for that employee
        stop
show login error if no row matches
```

The inventory row gets padded but does not become a valid employee record; mapping later tries to parse its text as an employee number and fails.

### 3. Employee profile and directory maintenance `[SOURCE]`

```text
select employee row
for profile: display the row's employee fields
for add/update/delete:
    validate/modify EmployeeData.records
    rewrite employee CSV from the shared list
    notify screens to refresh
```

### 4. Attendance clock-in/out/history `[SOURCE]`

```text
on clock-in:
    capture employee and current date/time
    append attendance event to attendance CSV
on clock-out:
    find open attendance event for employee/day
    add clock-out time
for history:
    read CSV rows and filter by employee/date
    display matching events
```

### 5. Payroll and bulk export `[SOURCE; backend smoke checks passed]`

```text
map CSV row to FullTimeEmployee/PartTimeEmployee
read hours/attendance for selected pay period
calculate regular/gross pay
calculate applicable contributions/deductions
net = gross - deductions + applicable allowances
for bulk payroll:
    repeat for each employee
    display/export payroll rows
```

The UI/backend has payroll screens and export code, but the project has no bundled employee-data file; leave/overtime workflow classes are not fully wired to screens.

---

## Folder 8 — local Swing app plus online console app

### 1. GUI login `[RUN with bundled employee-details schema]`

```text
show employee-number and last-name fields
read local employee-details CSV
skip header
for each row:
    if field 0 equals entered employee number
       and field 1 equals entered last name:
        open GUI MainMenu for that employee
        stop
otherwise show invalid-login message
```

The GUI authentication path checks employee number/last name in the employee-details file; it does not use the separate small credentials CSV for this login path.

### 2. Employee selection and profile `[SOURCE]`

```text
load employee IDs into selector
when ID changes:
    read matching employee-details row
    display profile fields
```

### 3. Attendance views `[RUN for loader; GUI display path SOURCE]`

```text
load attendance CSV records into memory
filter records by employee ID and requested date/period
calculate/display daily attendance or period hours
```

The local loader found 34 employees and resolved attendance for bundled records in the retest.

### 4. Payroll variants `[SOURCE; backend loader exercised]`

```text
select employee and payroll period/type
load employee + matching attendance
if regular pay:
    use RegularPayrollProcessor
else if final pay:
    use FinalPayPayrollProcessor
else if 13th-month pay:
    use ThirteenthMonthPayrollProcessor
calculate gross and applicable overtime/pay components
calculate SSS, PhilHealth, Pag-IBIG, and tax deductions
create/display Payslip
```

### 5. Employee table and employee maintenance `[SOURCE]`

```text
load employee details into table
on add/update/delete:
    edit local CSV-backed employee data
    refresh table/profile views
```

### 6. Payslip disputes `[SOURCE; demo only]`

```text
create dispute with employee, period, type, and reason
set status to Submitted
payroll staff may mark Under Review
then resolve with notes or reject with reason
store dispute in singleton in-memory list
```

The list is not durable; process exit loses disputes. The included dispute `main` is a demonstration, not an assertion test.

### 7. Console workflow `[SOURCE; different data source]`

```text
read employee number and last name
fetch published Google Sheets CSV over HTTP
validate login against fetched data
show console menu:
    profile / daily attendance / weekly hours / payroll
fetch/parse the needed sheet rows for each selection
calculate and print requested result
```

This online console flow is separate from the local-CSV GUI flow. Leave rows in the GUI are sample/hard-coded rather than a fully persisted leave workflow.

---

## Folder 9 — CP2 GUI connected to an OOP payroll backend

### 1. HR and employee login `[SOURCE]`

```text
HR login:
    read username/password
    skip HR-login CSV header
    find matching pair
    on match open HR MainMenu

Employee login:
    read employee number, username, password
    scan employee data CSV
    compare employee number at field 0,
           username at field 19,
           password at field 20
    on match open employee Dashboard
```

The employee validator checks for at least three fields before accessing fields 19 and 20, so a short-but-not-empty row can still fail with an index error.

### 2. Employee table and CSV maintenance `[RUN: MainMenu loaded 34 rows]`

```text
read data.csv header and rows
split rows into fields
populate JTable; hide configured internal columns
on row click:
    copy selected fields into edit controls
on Update:
    validate ID is unique
    replace selected row's fields
    rewrite table data to CSV
on Delete:
    confirm
    remove selected row in table
    rewrite CSV
```

### 3. Attendance repository and period list `[RUN: attendance tests passed]`

```text
read attendance.csv line by line
skip recognized header and blank rows
require employee ID, date, clock-in, clock-out
parse ID/date/time; reject invalid rows with line context
store AttendanceRecord objects
for employee:
    collect YearMonth values present in records
    expose those actual months as payroll periods
```

### 4. OOP payroll calculation `[RUN: 91 payroll checks passed]`

```text
choose employee and actual attendance period
create RegularEmployee or ProbationaryEmployee model
build AttendanceLog from the selected employee's timecard records
PayrollCalculator aggregates worked days/hours
select the applicable GovernmentContributionCalculator implementations
calculate SSS/PhilHealth/Pag-IBIG/tax amounts
produce PayrollRecord with gross, deductions, and net
return record to GUI for display
```

### 5. Leave request decision `[RUN: 44 leave checks passed]`

```text
load pending leave rows, processed leave rows, and employee data
when approver chooses request + approve/reject:
    re-read files and verify selected pending row still matches
    find employee and validate leave-credit rules
    prepare updated employee leave credits/counters
    remove request from pending file
    append processed decision row
    update employee data file
    if any write fails:
        restore all three files from saved snapshots
```

The main LeaveService implements the transactional/rollback path; a separate leave-form append path was noted as an open integration issue.

### 6. GUI boundary `[RUN for main table view]`

```text
GUI loads/displays employee and attendance/payroll data
GUI sends selected employee/period to OOP backend
GUI displays returned PayrollRecord
GUI should not reimplement payroll arithmetic in event handlers
```

The OOP test harness passed under the user's timezone and the direct table screen loaded 34 rows. Not all desktop forms or write/export paths were exercised.

---

## Quick comparison of the flows

- **Mostly database/UI-driven:** folder 1 (SQLite); folders 2 and 5 (MySQL) calculate much of salary in Swing handlers.
- **Small procedural examples:** folder 3 reads fixed/example values and prints arithmetic.
- **CSV plus layered services:** folder 4.
- **OOP payroll pipeline:** folder 6; folder 9 extends this with explicit repositories, service tests, and a documented CP2-to-MS2 bridge.
- **CSV-heavy GUI:** folder 7.
- **Two disconnected input channels:** folder 8 (local GUI CSVs vs online console sheets).

The pseudocode is a map for understanding and QA—not evidence that any repository is the user's own work or a safe source to submit. Confirm authorship, course fit, current payroll rules, and reuse permission with the mentor before adopting code.


---

End of consolidated report.
