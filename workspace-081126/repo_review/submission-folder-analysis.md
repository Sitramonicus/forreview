# `testingester` numbered submissions — structure, behavior, and course links

**Repository reviewed:** `/home/user/repo_review/full_repo/testingester-main/` (export of `Sitramonicus/testingester`). The numbered projects are nested beneath each `N/-/` directory; folder 8 has one more nested copy of its project root. This document records the initial static pass: source layouts, READMEs/setup notes, implementation paths, data-file headers, build metadata, and available test/evidence files. Behavioral testing was subsequently performed from isolated copies; see `/home/user/repo_review/submission-test-results.md` for observed runtime evidence and environment blockers. The test totals cited here as README/log claims were not rerun as part of the static pass. No employee records or passwords are reproduced.

> **Corpus extension:** Five user-selected public repositories have since been added as folders 10–14 under `full_repo/testingester-main/`. See [`submission-feature-catalog/README.md`](submission-feature-catalog/README.md) for snapshot commits, licenses/stars as captured, and detailed inventories/pseudocode. The original analysis below describes folders 1–9 only.

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
