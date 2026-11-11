# Folders 1–9 — normalized major/minor feature map

This supplements the earlier analysis/pseudocode for the original nine folders so that all fourteen folders can be browsed in one consistent major → minor → sub-functions shape. It does **not** replace or duplicate the detailed minor-workflow pseudocode already in [`../submission-feature-pseudocode.md`](../submission-feature-pseudocode.md). Separate major-feature orchestration pseudocode for folders 1–8 is in [`folders-01-to-09-major-pseudocode.md`](folders-01-to-09-major-pseudocode.md). For each minor row, use the named folder section in the existing pseudocode file and its `[RUN]`, `[SOURCE]`, or `[PARTIAL]` label. The folder-9 user attachment is the full example for that repository.

**Scope labels:** `Implemented/observed` means source presents the workflow; `Partial` means UI/model exists but a complete flow was not found; `Run` refers only to prior isolated test evidence recorded in existing review reports and does not mean every branch was tested. Folders 2 and 5 are one executable project under different labels.

## Folder 1 — SQLite / NetBeans Swing manager

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Startup and login/audit | Open login; query SQLite user/division; populate shared user context; route to menu; append audit event. Caveat: source has a role/string comparison flaw and empty conditional. | `submission-feature-pseudocode.md`, Folder 1: “Start and login” |
| 2. Employee records | Load table; create employee and optional image; select row; update; delete; refresh. | Folder 1: “Employee directory” |
| 3. Allowances | Select employee; enter allowance components; calculate displayed total; insert allowance record. | Folder 1: “Allowance entry” |
| 4. Deductions | Enter employee, deduction reason and amount; write salary-deduction row; view/refresh. | Folder 1: “Salary deductions” |
| 5. Payroll/payslip | Read employee, allowance and deduction records; prepare salary/payslip view; expose report/PDF output. | Folder 1: “Payslip and PDF/report screens” |
| 6. Attendance and leave UI | Open screens/forms and render available rows; complete persisted workflow was not established. | Folder 1: “Attendance and leave screens” `[PARTIAL]` |
| 7. Deleted-record lifecycle | Employee screens and models expose remove operations; exact persistent recovery workflow is not established uniformly. | Folder 1 employee-directory pseudocode and source notes |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_1_PAYROLL_ADMIN_FLOW
    authenticate through login screen and set current-user context
    load the selected employee's database-linked records
    when requested, maintain employee, allowance, or deduction rows
    when payroll view is requested, combine stored employee/pay inputs
    render payslip/report and stream PDF if chosen
    treat attendance/leave screens as incomplete until persistence is separately verified
END PROCEDURE
```

## Folders 2 and 5 — same Swing/MySQL application

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Authentication and role routing | Read account credentials; query MySQL; direct Admin/IT vs employee to corresponding home screen. | `submission-feature-pseudocode.md`, Folders 2 and 5: “Login and role routing” |
| 2. Employee self-service | Navigate profile, salary, attendance, leave and overtime-request screens; display account/employee data. | Same file: “Employee dashboard” |
| 3. Admin employee/account maintenance | Browse employees/accounts; open create/update/delete/admin screens; UI event handlers issue SQL. | Same file: “Admin employee/account management” |
| 4. Attendance and salary administration | Read/edit attendance/salary data; UI-side payroll/deduction arithmetic in salary create/update screens; present payroll outputs. | Same file: “Admin attendance and salary” |
| 5. Leave/overtime request decisions | Browse employee requests and approval screens; update request status through handlers. Model/workflow completeness varies. | Same file: “Leave and overtime requests/approvals” |
| 6. Database-backed persistence | Direct SQL against local `motorph` MySQL tables; requires external database/schema. | Folder 2/5 setup and observed read-path notes in `submission-folder-analysis.md` |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDERS_2_AND_5_MAIN_FLOW
    authenticate user against MySQL account table
    route account to employee or admin dashboard
    IF employee action THEN show self-service profile/pay/attendance/request views
    IF admin action THEN maintain employee/accounts and payroll/attendance records
    route leave or overtime request to its review screen
    persist changes through UI-issued SQL against configured MySQL schema
END PROCEDURE
```

## Folder 3 — independent CP1 console exercises

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Worked-hours example | Use fixed/example attendance values; subtract times; print duration. | `submission-feature-pseudocode.md`, Folder 3: “Worked-hours demo” |
| 2. Gross and semi-monthly pay examples | Multiply supplied hours/rate; calculate example gross/semi-monthly values; print results. | Same file: “Gross-pay and semi-monthly-pay demos” |
| 3. Deductions example | Apply the program's example deduction percentages/amounts to sample pay; display output. | Same file: “Deduction demo” |
| 4. CSV payroll reader | Open expected employee CSV; read salary at a fixed field/column; apply the file's example deductions; print result/error. | Same file: “CSV payroll reader” `[RUN; input-layout caveat]` |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_3_CONSOLE_EXERCISES
    choose a standalone example program
    initialize its hard-coded/example inputs OR read the expected CSV layout
    calculate only that example's hours/pay/deduction result
    print result or report input/file failure
END PROCEDURE
```

## Folder 4 — layered Swing/CSV CP2 app

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Startup and authentication | Start GUI; authenticate synthetic/local credentials; wire repositories, services and controllers; load data. | `submission-feature-pseudocode.md`, Folder 4: “Login and app wiring” |
| 2. Employee administration/search | List/paginate/search; add/edit/delete or maintain records; update employee CSV; show profile. | Same file: “Employee search and maintenance” |
| 3. Attendance access | Load CSV attendance; find by employee/date/range; present attendance data. | Same file: “Attendance lookup” |
| 4. Payroll | Choose employee and period; calculate a single employee or generate for all; compose configured deduction components. | Same file: “Payroll for one employee or all employees” |
| 5. Reports | Prepare payslip; create weekly/monthly summaries; render/export reports. | Same file: “Payslip and period reports” |
| 6. Leave screen | UI exists; startup wiring to a leave service is incomplete/unconfirmed. | Same file: “Leave screen” `[PARTIAL]` |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_4_LAYERED_PAYROLL_FLOW
    authenticate and load CSV data through repository
    dispatch UI action through controller to employee/payroll/report service
    for payroll, select employee and date range then calculate result
    render payslip or summary using report service
    persist employee changes through CSV repository
    expose leave UI only as partial until its service path is wired
END PROCEDURE
```

## Folder 6 — MO-IT110 MS2 OOP refactor example

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Startup, CSV data and login | Load employee/attendance CSV; construct user; validate login; open role-appropriate UI. | `submission-feature-pseudocode.md`, Folder 6: “Startup, CSV loading, and login” |
| 2. Employee types | Build base employee plus regular/probationary subtype; expose type-specific compensation/allowance behavior. | Same file: “Employee subtype behavior” |
| 3. Attendance/cutoffs | Parse timecards; normalize according to code's rules; aggregate work time and first/second cutoff. | Same file: “Attendance and pay-period aggregation” |
| 4. Payroll/deductions | Select deduction implementations; compute gross, contributions/tax, net and immutable payslip. | Same file: “Payroll and deduction composition” |
| 5. Roles/permissions | Employee vs payroll staff identity; gate employee lookup/payroll screens based on current user type. | Same file: “Role permissions and result persistence” |
| 6. Persistence/output | Read CSV and save payroll output, including displayed payslip/report. | Same file's payroll/persistence flow and `submission-folder-analysis.md` Folder 6 notes |
| 7. Verification | Run plain-Java tests against example and boundary behavior; compare against course-project outputs. | `submission-test-results.md` / `submission-retest-addendum.md`, Folder 6 |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_6_OOP_PAYROLL_FLOW
    load CSV employee/attendance rows and authenticate user
    construct the employee subtype and attendance records
    aggregate the selected cutoff's attendance
    call PayrollProcessor with selected deduction components
    return immutable payslip to authorized UI and persist output if requested
    verify components with the folder's tests/CP2 comparison evidence
END PROCEDURE
```

## Folder 7 — Swing/CSV payroll app with missing employee source data

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. Startup/data loading | Create attendance file if absent; load expected employee CSV into shared list; open login. | `submission-feature-pseudocode.md`, Folder 7: “Startup and employee CSV loading” |
| 2. Login/profile/directory | Match credentials in employee row; show profile; add/update/delete employee rows; rewrite CSV. | Same file: “Login” and “Employee profile and directory maintenance” |
| 3. Attendance | Record clock-in/out; append/read attendance CSV; display employee history. | Same file: “Attendance clock-in/out/history” |
| 4. Payroll and bulk output | Invoke payroll calculation pipeline; calculate contribution/tax components; prepare bulk employee pay; export payroll output. Overtime path is described as unfinished. | Same file: “Payroll and bulk export” |
| 5. Leave/overtime requests | Domain classes indicate request types, but a complete leave-request workflow was not found; overtime request is not integrated into the main processor. | Same file Folder 7 note and `[PARTIAL]` boundaries |
| 6. File persistence | Shared static employee rows and CSV helper write employee/attendance/payroll files. | Folder 7 code/source notes in `submission-folder-analysis.md` |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_7_EMPLOYEE_AND_PAYROLL_FLOW
    load expected employee CSV and attendance data
    authenticate employee and show dashboard
    dispatch profile/directory action to shared employee-row operations
    append or list attendance events when requested
    invoke payroll processors for selected/bulk pay output
    export supported report while keeping leave/overtime request paths marked partial
END PROCEDURE
```

## Folder 8 — local Swing app plus separate online console app

| Major feature | Minor feature → sub-functions | Pseudocode reference |
|---|---|---|
| 1. GUI login and data source | Authenticate against bundled local credentials/CSV; initialize GUI workflow. | `submission-feature-pseudocode.md`, Folder 8: “GUI login” |
| 2. Employee selection/profile | Search/select employee; present profile details from local data. | Same file: “Employee selection and profile” |
| 3. Attendance | Read timecards; show employee attendance and weekly/monthly time views. | Same file: “Attendance views” |
| 4. Payroll variants | Run regular, final-pay and 13th-month processors; compute configured deductions/premiums; create payslip. | Same file: “Payroll variants” |
| 5. Employee administration | List/add/edit/delete employee rows in local CSV-backed UI. | Same file: “Employee table and employee maintenance” |
| 6. Payslip disputes | Submit employee dispute; in-memory dispute list; review/resolve/reject in payroll frame. | Same file: “Payslip disputes” `[SOURCE; in-memory and role gap]` |
| 7. Console workflow | Login by employee number/last name; download sheet CSV over HTTP; show profile, attendance, weekly hours and payroll. Separate from GUI data channel. | Same file: “Console workflow” |

### Major-feature orchestration pseudocode

```text
PROCEDURE FOLDER_8_WORKFLOW
    IF GUI mode THEN load bundled CSVs and authenticate from local records
    ELSE IF console mode THEN fetch configured public-sheet CSV and authenticate separately
    route employee to profile, attendance, and payroll view
    choose regular, final-pay, or 13th-month processor as requested
    create/render payslip and accept dispute in the demo workflow if selected
    keep the GUI and HTTP-sheet data channels distinct
END PROCEDURE
```

## Folder 9 — supplied feature-inventory example

The uploaded file [`MotorPH_OOP_Feature_Inventory_and_Pseudocode.md`](../../uploads/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md) is the reference example. It already separates implemented GUI, inherited behavior, new OOP backend and design-only classes; lists major and minor features; identifies sub-functions; and gives pseudocode per minor workflow while preserving known defects/limitations. Existing repository-specific source analysis and behavior pseudocode remain linked from the [master index](README.md).

## Source and policy caveats

- The earlier folders are examples/candidates, not verified work authored by the user. Confirm mentor approval and licensing before graded reuse.
- Folders 6 and 9 contain payroll formulas from prior course implementations. Candidate policy review records known stale rates, attendance/lunch gaps and conflicting conventions. They are not authoritative company or statutory rules.
- For exact observed behavior, use the code paths under `full_repo/testingester-main/N/-/` alongside these crosswalks; do not infer a complete feature solely from a class, form or README bullet.
