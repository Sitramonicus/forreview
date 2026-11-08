# Folders 1–9 — major-feature orchestration pseudocode

This file supplies one top-level routine per major feature for the original submissions. The more detailed minor-function pseudocode remains in [`../submission-feature-pseudocode.md`](../submission-feature-pseudocode.md), and folder 9's complete example is the user attachment at `/home/user/uploads/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md`. Here, `CALL FOLDER_N_DETAIL` means follow the corresponding minor workflow named after the routine; it does not assert that the feature is fully wired or tested.

## Folder 1 — SQLite Swing manager

```text
PROCEDURE F1_AUTHENTICATION_AND_AUDIT
    open SQLite login form
    validate submitted credentials against Users
    if accepted, set current-user context and navigate to menu
    record login audit event
    preserve the source's known flawed division/role branch behavior
END
```

```text
PROCEDURE F1_EMPLOYEE_RECORDS(action, employeeData)
    connect to SQLite
    IF action = list THEN load Employee_info rows
    IF action = create THEN validate fields/image; insert employee; refresh list
    IF action = update THEN write selected employee fields; refresh list
    IF action = delete THEN confirm; delete selected row; refresh list
END
```

```text
PROCEDURE F1_ALLOWANCE_ENTRY(employeeId, allowanceInputs)
    load employee/rate context
    calculate the screen's allowance total
    insert allowance detail and total
    display saved outcome
END
```

```text
PROCEDURE F1_SALARY_DEDUCTION(employeeId, reason, amount)
    collect deduction inputs
    insert Salary_Deduction record
    refresh or show saved deduction
END
```

```text
PROCEDURE F1_PAYSLIP_AND_PDF(employeeId, optionalPayrollId)
    load employee, allowance and deduction data
    compose the payslip/report view
    IF PDF requested THEN render selected report template and stream/save PDF
END
```

```text
PROCEDURE F1_ATTENDANCE_AND_LEAVE_SCREENS(action)
    open requested attendance or leave screen
    render available form/table controls
    do not claim a persisted end-to-end workflow unless a backing data path is verified
END
```

## Folders 2 and 5 — one Swing/MySQL program

```text
PROCEDURE F2_F5_LOGIN_AND_ROLE_ROUTING(credentials)
    query configured MySQL account table
    IF valid THEN set authenticated user
        IF user is admin/IT THEN open admin dashboard
        ELSE open employee dashboard
    ELSE show login failure
END
```

```text
PROCEDURE F2_F5_EMPLOYEE_SELF_SERVICE(user)
    load linked employee data
    show profile and salary/attendance summaries
    allow navigation to leave and overtime request screens
    keep actions within currently wired UI behavior
END
```

```text
PROCEDURE F2_F5_ADMIN_EMPLOYEE_AND_ACCOUNT_MAINTENANCE(action, data)
    require admin dashboard path
    dispatch employee/account list, create, edit, or delete operation
    validate form fields as implemented by selected Swing handler
    issue the handler's SQL against MySQL
    refresh affected table/dashboard
END
```

```text
PROCEDURE F2_F5_ATTENDANCE_AND_SALARY(action, employee, period)
    load selected employee and attendance/salary rows
    IF salary create/update THEN execute UI-side calculation branch
    persist changed salary/attendance values
    refresh payroll or employee display
END
```

```text
PROCEDURE F2_F5_LEAVE_AND_OVERTIME_REVIEW(requestId, decision)
    load selected request
    show request details in admin review screen
    apply the decision through the implemented screen handler
    update request row and refresh list
END
```

```text
PROCEDURE F2_F5_DATABASE_PERSISTENCE(queryOrChange)
    open local motorph MySQL connection
    bind values to the handler's SQL statement
    execute and handle result/error
    close or return connection according to shared Classes.db behavior
END
```

## Folder 3 — standalone console exercises

```text
PROCEDURE F3_WORKED_HOURS_EXAMPLE
    read fixed/example time inputs
    compute duration using that example's time logic
    print duration
END
```

```text
PROCEDURE F3_GROSS_AND_SEMIMONTHLY_EXAMPLES
    read fixed/example hours, rate and pay inputs
    calculate example gross/semi-monthly values
    print values
END
```

```text
PROCEDURE F3_DEDUCTION_EXAMPLE
    read example pay amount
    apply the constants in the selected demo file
    print deductions/result
END
```

```text
PROCEDURE F3_CSV_PAYROLL_READER(path)
    open expected employee_data.csv
    read salary from the expected fixed column
    apply that program's example deductions
    print payroll result OR report file/schema failure
END
```

## Folder 4 — layered Swing/CSV CP2 application

```text
PROCEDURE F4_STARTUP_AND_AUTHENTICATION
    show login screen
    authenticate against configured local credentials
    create repository and load employee/attendance data
    wire services/controllers into the Swing frame
    open main application
END
```

```text
PROCEDURE F4_EMPLOYEE_MAINTENANCE(action, fields)
    controller validates requested employee operation
    delegate list/search/create/update/delete to EmployeeService
    persist employee changes through CSV repository
    refresh affected table/profile view
END
```

```text
PROCEDURE F4_ATTENDANCE_LOOKUP(employeeId, dateRange)
    query attendance repository for employee and requested date/range
    parse and return matching records
    render attendance result
END
```

```text
PROCEDURE F4_PAYROLL(employeeSelection, period)
    validate employee/period inputs
    IF one employee selected THEN compute that employee's payslip
    ELSE iterate eligible employees and compute each payslip
    pass attendance and deduction components to PayrollService
    return results to UI
END
```

```text
PROCEDURE F4_REPORTS(reportType, period)
    select payslip or period-summary report path
    aggregate service outputs for requested week/month
    format and display/export the report
END
```

```text
PROCEDURE F4_LEAVE_SCREEN(action)
    render leave panel and controls
    attempt only source-wired leave actions
    report unsupported/unwired workflow rather than implying approval persistence
END
```

## Folder 6 — MO-IT110 OOP refactor example

```text
PROCEDURE F6_STARTUP_CSV_AND_LOGIN
    load employees and attendance from CSV
    validate user credentials/role
    open login-appropriate UI
END
```

```text
PROCEDURE F6_EMPLOYEE_TYPE_BEHAVIOR(employeeRow)
    validate and map employee fields
    IF regular THEN construct RegularEmployee
    ELSE IF probationary THEN construct ProbationaryEmployee
    use subtype method for its compensation/allowance behavior
END
```

```text
PROCEDURE F6_ATTENDANCE_AND_CUTOFF(employee, dateRange)
    load employee time records
    construct AttendanceRecord objects
    classify each record into configured cutoff
    aggregate accepted time and return cutoff totals
END
```

```text
PROCEDURE F6_PAYROLL_AND_DEDUCTIONS(employee, attendance, cutoff)
    calculate gross from selected employee type and attendance totals
    invoke configured deduction objects
    combine gross and deductions using current processor flow
    build immutable Payslip
END
```

```text
PROCEDURE F6_ROLE_PERMISSION(user, requestedScreen)
    authenticate user object
    inspect its configured type/permission method
    allow employee lookup or payroll staff access only if method permits
    otherwise deny navigation/action
END
```

```text
PROCEDURE F6_PAYROLL_OUTPUT(payslip, requestedOutput)
    render payslip in UI
    IF save/export requested THEN serialize to the configured payroll output
    report I/O error without changing the calculated result
END
```

```text
PROCEDURE F6_BACKEND_VERIFICATION(testSuite)
    load synthetic/test fixtures
    execute employee, attendance, deduction and payroll cases
    compare actual results to approved project fixtures/CP2 compatibility outputs
    report each pass/failure and aggregate result
END
```

## Folder 7 — Swing/CSV payroll system

```text
PROCEDURE F7_STARTUP_AND_CSV_LOAD
    create attendance CSV if absent
    load employee CSV using expected schema
    populate shared employee-row collection
    open login UI
END
```

```text
PROCEDURE F7_LOGIN_AND_PROFILE(credentials)
    find employee row and compare stored credential field
    IF match THEN open dashboard and show selected profile
    ELSE show login failure
END
```

```text
PROCEDURE F7_EMPLOYEE_DIRECTORY(action, employeeData)
    read current shared employee collection
    dispatch add/update/delete/profile operation
    rewrite employee CSV after mutation
    refresh screen
END
```

```text
PROCEDURE F7_ATTENDANCE(action, employee, timestamp)
    for clock-in/out validate selected employee and event input
    append event to attendance CSV
    for history query load/filter employee events
    render attendance table
END
```

```text
PROCEDURE F7_PAYROLL_AND_BULK_EXPORT(selection, period)
    load employee/time inputs
    run implemented payroll processor and contribution components
    collect per-employee result
    display or export bulk payroll output
    do not treat unfinished overtime request logic as a calculation input
END
```

```text
PROCEDURE F7_LEAVE_AND_OVERTIME_REQUESTS(request)
    expose domain model only where no screen/service flow exists
    report incomplete workflow; do not create approval or payroll side effects
END
```

## Folder 8 — local GUI and remote-sheet console

```text
PROCEDURE F8_GUI_LOGIN(credentials)
    query bundled local credential data
    if match then open GUI employee/admin workflow
    else show error
END
```

```text
PROCEDURE F8_EMPLOYEE_PROFILE_AND_DIRECTORY(action)
    use selected employee from local GUI data
    show profile or dispatch employee-row maintenance
    persist local edit if supported
END
```

```text
PROCEDURE F8_ATTENDANCE_VIEW(employee, period)
    load local attendance rows
    filter by selected employee and period
    compute/display views as implemented
END
```

```text
PROCEDURE F8_PAYROLL_VARIANT(employee, variant)
    load employee and attendance
    select regular, final-pay or 13th-month processor
    calculate configured earning/deduction output
    build/render payslip
END
```

```text
PROCEDURE F8_EMPLOYEE_ADMINISTRATION(action, employeeData)
    read employee table
    validate fields and run add/update/delete action
    save changes to local CSV
    refresh employee list
END
```

```text
PROCEDURE F8_PAYSLIP_DISPUTE(employee, dispute)
    validate and append dispute to process-memory singleton list
    payroll reviewer selects dispute
    resolve or reject and update in-memory status
    note that role authorization and durable persistence are not established
END
```

```text
PROCEDURE F8_CONSOLE_WORKFLOW(sheetUrl, credentials)
    download configured Google Sheets CSV over HTTP
    authenticate by employee number and last name
    display profile, attendance, weekly hours and payroll
    keep this remote-sheet flow separate from local GUI CSV data
END
```

## Folder 9

Folder 9's full major/minor/sub-function inventory and pseudocode is in the user-supplied example file. It explicitly distinguishes GUI behavior, OOP backend, design-only concepts, inherited features, known issues and pseudocode. Use that file rather than synthesizing another abbreviated duplicate.
