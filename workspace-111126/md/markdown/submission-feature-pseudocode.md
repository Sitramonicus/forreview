# How the nine submissions work — feature pseudocode

This document turns the current repository/retest consensus into plain-language pseudocode. It explains what the checked-in code appears to do; it is **not** a proposed design, a code fix, or proof that every path works. The full tests and caveats are in [`submission-test-results.md`](submission-test-results.md) and [`submission-retest-addendum.md`](submission-retest-addendum.md).

> **Extension:** The user later added five external repositories as folders 10–14. Their feature-by-feature inventories and major/minor pseudocode are in [`submission-feature-catalog/README.md`](submission-feature-catalog/README.md) and the five linked per-folder documents. This original file remains the pseudocode record for folders 1–9.

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
