# Source-grounded requirements and open-rule register

This note grounds the current prototype in the materials already supplied in this workspace. It does not treat the MO-IT107 design set as an automatic replacement for any MO-IT110 milestone requirement.

## Project decisions already made

- Start a fresh project; there is no personal MS1 implementation to extend.
- Use Java for the backend and payroll logic, with an HTML/CSS browser interface instead of Swing.
- Use Firebase as a development data service. Project `motorph-jd` and web app `motorphdev` exist; Email/Password Auth is enabled and the user reports publishing the narrowly scoped demo rules in `docs/firestore.rules`. Live synthetic API tests verified allowed own-user employee/attendance operations and denied cross-user/payroll/extra-salary operations. Exact Firestore location and Firebase authorization from the current Arena preview origin remain unconfirmed.
- Use the other teams' submissions selectively as pattern references, not as a full project to submit.
- Earlier MO-IT108 MotorPH Requirements Analysis and System Design documents are supplemental legacy context, not the current MO-IT110 MS2 rubric. They clarify attendance-view scope and proposed role/data fields, but not a complete hour or payroll algorithm. See `docs/legacy-mo-it108-traceability.md`.
- `The Code.pdf` is a C++ inventory-management exercise, not a payroll module; no code from it is reused.
- Keep synthetic data only; never use the uploaded inventory CSV as payroll data.
- Do not invent payroll formulas, current statutory rates, or expected payroll outputs.
- The UI should be professional, restrained, and non-game-like, using permitted/credited code selectively.

## Firebase development boundary

The browser uses Firebase Authentication only to establish a user identity. The Java backend forwards the resulting ID token to Firestore REST, so Firestore Security Rules authorize access; it does not use an Admin SDK or service-account credential. The published development rules scope `users/{uid}/demoEmployees` and `users/{uid}/demoAttendance` to synthetic data, separate from any eventual production employee/payroll schema. Live tests verified the required own-user paths and tested deny cases. The latest published-rules smoke used disposable Auth accounts and employee documents, all deleted after testing; one cancelled synthetic leave record (`LV-3502C01F0E60`) and one cancelled ticket (`TK-F05DD2B0D6CC`) remain under their deleted test UIDs because workflow deletes are denied. This sandbox is not a production role/HR access design.

## Supplied source set

- `uploads/MO-IT107 - 2nd Milestone_ Database Design for the MotorPH Payroll System H2101 TORRES, J.D..pdf`
- `uploads/MO-IT107 Homework_ Data Dictionary for MotorPH Payroll System Database H2101 Torres, J.D..pdf`
- `uploads/MO-IT107 Homework_ Data Flow Diagram of MotorPH Payroll System H2101 Torres, J.D..pdf`
- `uploads/MotorPH_Payroll_System_Full_Synthesis.md` — detailed text reconstruction of those three PDFs.
- The user's earlier MO-IT108 Requirements Analysis, System Design, and Terminal Assessment proposal PDFs are summarized in `docs/legacy-mo-it108-traceability.md`; treat them as older-course context, not a substitute for the active MO-IT110 rubric.
- `uploads/The Code.pdf` — inspected and classified as unrelated C++ inventory-management code; it was not used as a payroll source.
- MO-IT110 modules/requirements were reviewed earlier and summarized in `repo_review/submission-comprehensive-report.md`; the readable project-level conclusion is that the modules teach the Java/OOP design-to-implementation progression, but do not provide a complete payroll formula oracle.
- `repo_review/submission-comprehensive-report.md` — repository tests, module links, scope cautions, and limitations for the nine reference submissions.

## Facts used in the current slice

### Employee directory

The MO-IT107 Data Dictionary describes Employee identity/status and links to Address, Position, Department, Government, and Supervisor. The exact direct-vs-derived relationships differ between that dictionary, Database Design prose, and the ERD. Therefore the current UI uses only a small display projection (ID, name, department, role, location, status) with fully synthetic records. It does not claim to implement the full Employee table or decide the disputed relationships. Government identifiers and other PII are intentionally absent.

### Raw attendance model and local-only demo capture

The supplied Data Dictionary lists these Attendance fields:

- `attendanceID` — integer primary key;
- `scheduleID` — foreign key;
- `date` — date, unique in combination with `scheduleID`;
- `timeIn` and `timeOut` — time values, with the source check `timeIn <= timeOut`;
- `dayType` — enum, with members not specified.

The prototype maps these to `AttendanceRecord` and validates positive synthetic identifiers, required values, `timeIn <= timeOut`, and unique `(scheduleID, date)` entries. A minimal, fixed employee-to-schedule mapping allows records to be viewed. The screen provides inclusive date filtering and, following the older MO-IT108 Requirements Analysis, defaults to the selected employee's earliest/latest available record dates. This is a view convenience, not a payroll-period calendar. `dayType` is shown and set as `DEMO`, not as an official enum value. No work hours, overtime, paid leave, undertime, attendance classification, or pay values are derived.

The user explicitly authorized a narrow create-only feature in the unauthenticated local demo: a raw synthetic date and two times can be added only for a mapped demo employee. The server supplies the existing schedule mapping and `DEMO` day type; the process-memory store enforces the unique schedule/date key atomically. New/unmapped employees are rejected; no mapping is assigned. The form is hidden in Firebase mode, and the API rejects raw-attendance creation there; Firebase attendance remains read-only. Local records are shared by callers of the local server process and disappear when it stops. No correction/delete, time-clock verification, schedule assignment, hours, or payroll operation is included.

The database-design PDF also describes attendance as tied to Schedule and the DFD routes attendance through Schedule and Time Tracking, then Timesheet Validation. The exact source/import mechanism and MS2 workflow are not asserted by this limited raw-entry feature.

### Payroll and payslip fields

The source set names payroll components and storage fields, including:

- `Payroll`: employee, wage/rebate/summary IDs, month, PTO maximum, hourly/daily/monthly rates, net pay;
- `Payslip`: payroll ID, period start/end, regular/overtime/holiday-overtime/PTO/undertime hours, worked days/holidays/rest days, gross income;
- `Earnings`: overtime/PTO/holiday/rest-day pay, subsidies/stipends, other incentives, total benefits;
- `Deductibles`, `Other Transactions`, `Government Deductibles`, `Contributions`, `Government Tally`, and `Summary` for deduction and contribution amounts/totals.

The DFD labels Timesheet Validation, Leave Monetisation, Wage and Adjustments, Government Deductibles, Basic Payroll Computation, Payslip Generation & Distribution, and Monthly Summary Compilation. These descriptions establish the concept names and broad flow, not their exact computation.

## Unresolved source conflicts kept open

The PDFs disagree on several schema details. The prototype has not chosen a definitive production schema for these:

- `Schedule.restDay SET` versus `Schedule.restDayID` plus a separate Rest Day table;
- direct Employee `departmentID` / `governmentID` versus deriving department via Position and linking Government via `Government.employeeID`;
- Address region stored directly versus derived through City;
- presence of `Supervisor.departmentID`;
- presence of `Leave.scheduleID`;
- key/version fields for Rates and Government Tally;
- some Holiday uniqueness and relationship-matrix cardinalities.

See Sections 16 and 17 of the full synthesis for the complete discrepancies.

## Payroll-rule status

The source documents do **not** supply the arithmetic formula or complete business inputs for gross pay, salary conversion, overtime, holiday/rest-day rates, undertime, paid leave, leave monetisation, cutoff deduction allocation, or an end-to-end payslip. They also do not provide an authoritative MotorPH worked payslip example. A temporary monthly preview from an earlier iteration has been removed under the current no-temporary-features instruction. No government statutory calculation is active; this does not fill the attendance-to-gross or pay-period gaps. The nine GitHub submissions' formulas/tests are comparison evidence only, not MotorPH policy authority.

## Scope boundary

MO-IT107's DFD/database design is broad (five actors; employee management, payroll processing, and archival; PII approval intent; many database tables). The current project begins as a narrow web-based Java/OOP slice. Do not add every actor, all tables, two-person PII workflow, monthly reports, or archive deletion unless the active MO-IT110 milestone explicitly requires it. Keep such items traceable to the source and clearly labelled reference-only until scope is confirmed.

## Additional candidate-policy research (2026-11-03)

At the user's invitation, candidate assumptions from the supplied peer repositories and current official references were reviewed. The folder-6 course-project cutoff/hour rules and folder-9 contribution-calculator pipeline are documented in `docs/candidate-policy-review.md`, along with the user's MO-IT107 data dictionary and the outdated formulas in that prior calculator. A temporary 2026 browser preview mentioned in earlier research was removed on 2026-11-04. No calculator files or active payroll estimate remain. Earlier peer/statutory analysis is research only and the Payroll workspace now fails closed with configuration gates.
