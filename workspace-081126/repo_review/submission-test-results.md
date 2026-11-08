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
