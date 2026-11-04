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

