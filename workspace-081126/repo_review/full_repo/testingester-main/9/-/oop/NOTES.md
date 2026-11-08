# MS2 Backend — Waves 1–4 (Employee hierarchy + contribution calculators + PayrollCalculator)

> **Built:** 2026-10-26 (Mon) waves 1–3 · **Wave 4 (`PayrollCalculator`):** 2026-10-28 (Wed) — backend only,
> **no GUI touched** in that step (the GUI rewiring came later, commit `942aa9d`)
> **Design source:** `MS1 Method Dictionary + CRC Cards` — every class and method
> name below exists in the approved design. No invented identifiers.
> **Verified:** `javac` 21.0.12, `-Xlint:all`, **0 warnings, 0 errors** · console harness **91/91 PASS**
> (sections A–I) · CP2 parity: **25/25 documented deviations** (the OOP values follow the
> official matrices; section H is the proof, `parity-report.txt` is the audit trail).

## What is implemented (Waves 1–4 of the 5-wave build order)

| Class | Type | S1 methods implemented |
|---|---|---|
| `Person` | base class | `getFullName`, `getAge`, `updateAddress`, `updatePhoneNumber`, `toString` + validated personal setters |
| `Employee` | **abstract**, extends `Person` | 19-param constructor, `getId/setId`, gov-ID getters (read-only), `getStatus/setStatus`, `getPosition/setPosition`, `getImmediateSupervisor/set…`, `getBasicSalary/set…`, `getClothingAllowance/set…`, `getGrossSemiMonthlyRate/set…`, `getHourlyRate/set…` + 4 abstract benefit methods |
| `RegularEmployee` | subclass | `getRiceSubsidy` (1500), `getPhoneAllowance` (2000), `getAnnualLeaveCredits` (12), `isEligibleForPromotion` (yearsOfService ≥ 2) |
| `ProbationaryEmployee` | subclass | same 4 overrides with 1000 / 500 / 6 / false |
| `GovernmentContributionCalculator` | **abstract** | `calculate(double)` (abstract), `getContributionName` |
| `SSSCalculator` | subclass | `calculate` — 45 brackets, cap 1,125.00 |
| `PhilHealthCalculator` | subclass | `calculate` — employee share: 50% of the 3% premium, floor 150.00, cap 900.00 |
| `PagIbigCalculator` | subclass | `calculate` — employee rate 1% / 2%, cap 100.00 |
| `WithholdingTaxCalculator` | subclass | `calculate` — 6 BIR brackets, up to 35% |
| `PayrollRecord` | **immutable** value object | 19-param constructor + 19 getters, **no setters** |
| `AttendanceLog` | helper | `recordClockIn`, `recordClockOut`, `getDailyHours`, `getWorkedDays` |
| `PayrollCalculator` | **service / orchestrator** | `calculatePayroll(Employee, AttendanceLog)` + the period overload, `private calculateGross`, `private calculateNetPay` — one polymorphic loop over `List<GovernmentContributionCalculator>`, tax applied in a second phase |
| `ConsoleBackendTest` | test harness (not production) | 91 assertions, prints PASS/FAIL, exit code 1 on failure |

**Not built yet (next waves):** `Credentials`, `LeaveRequest`, `LeaveManagement`, `HRManager`.
The GUI rewiring is complete (commit `942aa9d`): `MainMenu` and `Dashboard` call
`PayrollCalculator` and no longer contain any `calculate*` method. The payroll path
(attendance → gross → deductions → net) is complete in the backend.

## Documented adjustments (why the code differs from the sheet)

1. **`Employee` keeps the exact 19-param constructor from the Method Dictionary**, but
   `riceSubsidy` / `phoneAllowance` values are validated and then *superseded* by the
   polymorphic overrides introduced in the Week 3 abstraction design. Keeping the signature
   preserved compatibility with how `data.csv` is read; the override is the source of truth.
2. **`Person` owns the 5 personal fields.** `Employee` inherits `getFirstName`, `setFirstName`,
   `getFullName`, etc. instead of re-declaring them (the Method Dictionary lists `getFullName` on
   both rows — inheritance makes that one implementation, not two).
3. **`AttendanceLog` is filled from the real timecard** (commit `773d8a8`). `AttendanceRepository`
   reads `src/motorphemployee/attendance.csv` (5,168 rows, 34 employees, 2025-06-03 → 2025-12-31)
   and the period picker only offers months that have records. CP2's hardcoded `workingDays[]`
   (`MainMenu.java:1066`) and the in-memory 08:00–16:00 bridge are gone; because the API was already
   CSV-ready, no caller changed.
4. **Constructors assign validated values directly** instead of calling their own setters, so a
   subclass never observes a half-constructed object (`-Xlint:this-escape` clean).
5. **`private calculateGross` takes the period bounds explicitly** (`AttendanceLog, int employeeId,
   double hourlyRate, String periodStart, String periodEnd`) instead of the 2-argument Method
   Dictionary row. The formula is exactly the sheet's (`workedDays * hourlyRate * 8`); the extra
   arguments only replace hidden state, because a private helper cannot know the payroll period
   otherwise. The public surface (`calculatePayroll`) matches the sheet one-to-one.
6. **`absences` is 0** in every `PayrollRecord`. The timecard records clock-in and clock-out only, so
   there is no absence or leave-day source to derive it from, and inventing day counts would put
   fabricated numbers on a payslip. `totalLeaves` comes from the employee's own `getAnnualLeaveCredits()`.
7. **The GUI builds the subclass that matches the status column** (2026-10-30). `MainMenu` and
   `Dashboard` used to construct `RegularEmployee` for every row, so the polymorphic benefit methods
   returned permanent-staff values (1,500 / 2,000 / 12) for the 11 probationary employees. Both
   screens now branch on `status`. The data file carries no probation start date, evaluation score or
   months served, so a probationary record is built with neutral placeholders: **6 months** (the
   standard probation period under Labor Code Art. 281), **`"N/A"`** for the start date (the same
   convention the data file already uses for an unknown supervisor) and **0** for a score that has not
   been evaluated yet. None of those three values reaches the payslip; the payslip still prints the
   allowance columns exactly as before, so displayed values and gross pay are unchanged.

## Official matrices over the old program (2026-10-27)

The 4 statutory calculators now follow the **official government matrices**, not CP2. The matrix
verification (`matrix-verification.md`) proved CP2 is wrong in 4 places; those 4 fixes are
Issues 2 to 5 below. Consequence: `parity-report.txt` is **no longer an all-MATCH file** — every
mismatch row in it is one of the documented deviations, and the proof that the new numbers are
correct lives in **section H** of `ConsoleBackendTest` (expected values copied from the matrix
cells, e.g. PhilHealth SAMPLE `B10` = 375, WHT SAMPLE `B20` = 513.40, SSS 3,250 = 157.50).

Impact on the 34-employee sample (basic salary used as the proxy for gross): PhilHealth
**34/34** employees (PHP 18,676.80/month over-deducted by CP2), WHT 18/34 (PHP 0.20 each),
SSS 4/34 (CP2 *under*-deducted PHP 22.50 each), Pag-IBIG 0/34 (all salaries are above the
point where the PHP 100 cap flattens the difference).

## `PayrollCalculator` orchestrator (2026-10-28)

The payroll path is now one method: `calculatePayroll(employee, attendance, periodStart, periodEnd)`
returns an immutable `PayrollRecord`. Nothing in this class asks which calculator or which employee type
it is holding — `grep -c instanceof PayrollCalculator.java` = **0**.

| Phase | What runs | Type it works with (S1 design) |
|---|---|---|
| gross | `workedDays * hourlyRate * 8` | `AttendanceLog` + `Employee` |
| **phase 1** | SSS → PhilHealth → Pag-IBIG in **one loop** | `List<GovernmentContributionCalculator>` (abstract) |
| **phase 2** | BIR withholding tax on `gross - phase 1` | `GovernmentContributionCalculator` (abstract) |
| result | `new PayrollRecord(...)`, fully validated | immutable value object |

Verified run (`console-test-output.txt`, section I, 20 June 2024 working days, hourly rate 205.36):

| Field | Value | Where it comes from |
|---|---|---|
| gross income | **32,857.60** | 20 days × 205.36 × 8 |
| hours worked | **160.00** | 20 days × 8 |
| SSS | **1,125.00** | official cap |
| PhilHealth | **492.864** | (300 + (32,857.60 − 10,000) × 3%) / 2 |
| Pag-IBIG | **100.00** | official employee cap |
| withholding tax | **2,061.35** | 20% of (31,139.736 − 20,833) |
| **net pay** | **29,078.39** | gross − 3,779.21 deductions |

Section I also proves: the result is immutable (no setters), two employee types price differently through
one `Employee` parameter, the calculator list is injectable (SSS-only run changes only the SSS amount),
Issue 1 is refused, and the no-dates overload prices the most recent semi-monthly period.

## Known issues found by the console run

| ID | Issue | Evidence | Status |
|---|---|---|---|
| **Issue 1** | At gross pay **0.00** the statutory minimums still apply (SSS 135 + PhilHealth 150 = 285), so net pay would be **-285.00** (a negative payslip). The calculators refuse a negative taxable income instead. The old program charged the full PhilHealth premium (300.00), which is why its recorded negative result is -435.00. | `cp2-parity-output.txt` line `NET\|0.00\|435.000000\|-435.000000` (CP2, before the PhilHealth fix) | ✅ **RESOLVED** (2026-10-28) — `PayrollCalculator.calculatePayroll` rejects a non-positive gross **before** any deduction runs; section I asserts the refusal. A second guard rejects the case where statutory deductions would consume the whole gross, so a negative net can never reach `PayrollRecord`. |
| **Issue 2** | PhilHealth deducted **100% of the premium**; the official employee share is **50%** (`Home_Philhealth_Matrix.xlsx` note `A6`, SAMPLE `B10` = 375 on 25,000). Every payslip was over-deducted by 2x. | `matrix-verification.md` §2 | ✅ **RESOLVED** — `PhilHealthCalculator` returns `premium / 2`; section H asserts 150 / 375 / 900 |
| **Issue 3** | Pag-IBIG deducted **employee + employer share** (3%/4%); the official employee share is **1%/2%**, cap 100 (`B2:B3` vs `D2:D3`, note `A5`). | `matrix-verification.md` §3 | ✅ **RESOLVED** — `PagIbigCalculator` uses the employee rate only; salary < 1,000 stays 0.00 and UNVERIFIED (no matrix row) |
| **Issue 4** | Withholding tax bracket 1 used the excess base **20,832** instead of **20,833** (`B3`, SAMPLE `B20` = 513.40) → +PHP 0.20 over-withheld in [20,833, 33,333]. | `matrix-verification.md` §4 | ✅ **RESOLVED** — `WithholdingTaxCalculator` uses 20,833 (and clamps the 20,832–20,833 gap so tax can never go negative) |
| **Issue 5** | SSS brackets were **upper-inclusive**; the official schedule is **lower-inclusive** ("3,250 - 3,749.99"), so a salary exactly on a boundary was under-deducted by PHP 22.50. | `matrix-verification.md` §1 | ✅ **RESOLVED** — `SSSCalculator` treats bracket *i* as `[lower, lower + 499.99)`; section H asserts 3,250 → 157.50 |

## GUI subclass fix (2026-10-30)

The payslip never showed the wrong benefit values, because it prints the allowance columns from the
data file, not the computed record — so a smoke test could not catch this. The defect was in the
object the screens built: `new RegularEmployee(...)` for every employee, probationary or not.

| What | Evidence |
|---|---|
| Before | one `new motorphemployee.oop.RegularEmployee(` per screen, zero `ProbationaryEmployee` |
| After | both screens branch on the `status` column; one `ProbationaryEmployee` construction each |
| Construction check | all 34 rows: 23 `RegularEmployee` + 11 `ProbationaryEmployee`, 0 failures; probationary rows report 1,000 / 500 / 6 and not promotable |
| GUI re-run | employee 21 (Probationary) completed **View Salary** with no error dialog, 30 Oct 2026 |
| No regression | gross parity **408/408** comparisons, 0 mismatches, worst delta **0.0** |

## Leave workflow fix (2026-10-31)

Two defects in the leave screens, both found by the group's tester after the Week 9 smoke test and
reported in the MS2 Known Issues List (rows 18 and 19).

| What | Evidence |
|---|---|
| Approving a request wrote `leaves.csv` and moved the leave credits immediately, but the request stayed in `pendingleaves.csv` until Save was pressed, so the same leave could be approved again and counted twice | before: `LeaveApproval.approvedActionPerformed` only did `model.removeRow(...)`; `saveActionPerformed` was the only writer of `pendingleaves.csv` |
| Once the approve call site was fixed (commit `683c9f0`), rejected leaves were still written with the reason and the approval swapped, because the reject call passed `(reason, approval)` into `writeToFile(..., approval, reason)` | `git show 683c9f0` touches one call site: a rejected row still lands as `Reason=No`, `Approved?=<reason>` |
| Fix | `LeaveService` is now the only writer on the approve and reject path. One `decide(index, approved)` call removes the request, appends the processed row in header order and moves the credits, and restores all three files if a write fails. `LeaveApproval` keeps the table view only, the manual Save button is gone, and `LeaveManagement` reads `leaves.csv` through the same service |
| Column order | `LeaveService.LEAVE_COLUMNS` is the single definition of `Employee ID, Leave Type, Days, Reason, Approved?`; approve and reject both go through it, so the two call sites cannot disagree again |
| Evidence | `oop/leave-fix-test-output.txt` — 44 assertions, 0 failures: approve, reject, double-processing refused, stale row refused, shifted (ragged) `data.csv` refused instead of being rewritten, unknown employee, zero-credit employee, comma inside a reason, CRLF preserved, rollback after a failed write |
| No regression | `ConsoleBackendTest` still reports **91 passed, 0 failed**; `data.csv` is rewritten with its 24 columns and its original CRLF line endings |
| GUI click-through | **verified 2026-10-31 21:15** (HR login, VS Code F5 → LandingPage): Leave Management → Pending Leaves processed one approve and one reject; both requests left `pendingleaves.csv` on that same click with no Save, the reject row landed as `Reason=Sick, Approved?=No`, and the credits moved `12,0,0 → 11,1,0` (approved) / `11,1,0 → 11,1,1` (rejected). Raw run: `oop/gui-retest-output.txt` |

## Real timecard instead of invented attendance (2026-10-31)

Known issue 16 in the MS2 Known Issues List: the payroll screens recorded a fixed 08:00–16:00 day
on a 2024 calendar for every "worked day" of a hardcoded month table, so a payslip was never tied to
an actual timecard.

| What | Evidence |
|---|---|
| Source | the group's `Copy of MotorPH_Employee Data.xlsx`, tab **Attendance Record** (`Employee #, Last Name, First Name, Date, Log In, Log Out`) |
| Import | `src/motorphemployee/attendance.csv` — **5,168 rows, 34 employees, 2025-06-03 → 2025-12-31**, one row per employee per working day, 0 duplicate employee/day pairs, 0 missing clock-in or clock-out |
| Employee numbers | the sheet uses 10001–10034 while `data.csv` uses 1–34 for the same 34 people (names identical, offset 10000), so the export is keyed to the ids `data.csv` already uses |
| Reader | `AttendanceRepository` turns rows into an `AttendanceLog`; the period picker offers only months that have rows, and worked days come from `AttendanceLog.getWorkedDays` — the same value `PayrollCalculator` prices |
| Screens | the duplicated 08:00–16:00 loop and the `workingDays[]` table are gone from `Dashboard` and `MainMenu`; both read the repository instead |
| Evidence | `oop/attendance-test-output.txt` — 33 assertions, 0 failures |
| No regression | `ConsoleBackendTest` 91/91, leave evidence 44/44 |
| Numbers | for this dataset the timecard has the same working days as the old table (Jun 20, Jul 23, Aug 22, Sep 21, Oct 23, Nov 21, Dec 22), so payslip amounts for those months do not move — but they are now derived from the timecard, and a month with no rows is not offered at all |
| Not yet verified (2026-10-31) | GUI click-through of View Salary / Calculate Salary (compiled and console-tested, not clicked in this run — the 21:15 GUI retest covered the leave screens only, not these two) |

## Leave credit rule (matches the original program)

Approving a leave deducts exactly **one** credit, no matter how many days the request covers:
a 4-day leave moves the column from 11 to 10, a 2-day leave from 12 to 11. That is what the
baseline program does, so the refactor kept it on purpose.

```java
// baseline 655d43d, LeaveApproval.updateLeaveCredits(...)
if (isApproved) {
    if (leaveCredits > 0) {
        data[21] = String.valueOf(leaveCredits - 1); // Deduct 1 leave credit
        data[22] = String.valueOf(approvedLeaves + 1);
```

`days` is a parameter of that method but it is never used for the deduction; it is only recorded
in `leaves.csv`. Leave credits also never reach the payslip: gross pay is
`workedDays * hourlyRate * 8`, and `workedDays` comes from the timecard, never from approved
leaves. Keeping the behaviour was a deliberate decision (2026-10-31): matching the original
program is worth more for the TA comparison than a rule nobody asked for. A days-based
deduction (`credits - days`) would be a rule change and needs its own note here first.

Verified on the running app (Eclipse, 2026-10-31): a 4-day Emergency Leave was submitted from
the employee Dashboard, appeared in Pending Leaves, was approved, landed in `leaves.csv` as
`1,Emergency Leave,4,for testing only,Yes`, and moved employee 1 from `11,1,0` to `10,2,0`.

**Open defect in the submit path (ARCH/KI-13):** `LeaveForm.submitActionPerformed` appends its
own row with `new FileWriter(path, true)` and `row + "\n"`, so the row it writes ends in LF
while the rest of the file is CRLF, and it bypasses `LeaveService` entirely. A file watcher
caught the bytes (`... 6F 6E 6C 79 0A`) on two separate runs. Saving still works with the file
missing, because `FileWriter` creates it, and `Files.readAllLines` accepts both endings - the
defect is cosmetic for now, but it is the proof that a second writer still exists.

## Popup windows are modal dialogs (KI-22, 2026-10-31)

Known issue 22 in the MS2 Known Issues List: the popup screens did not take focus, could be opened
many times from the same button, and closing one from the taskbar closed the whole application.

| What | Evidence |
|---|---|
| Cause | every popup was a `JFrame` built with `setDefaultCloseOperation(EXIT_ON_CLOSE)`, so a close request on a child called `System.exit()` on the JVM that also hosts the main window; none was modal, and each parent did `new <Screen>(...)` on every click |
| Why it looked IDE-specific | it is not. The probe never makes a window visible and the JVM still exits, because `JFrame.processWindowEvent` performs the exit — so Eclipse, VS Code and a plain terminal behave the same. In a VS Code debug session the dead JVM only reads as "debug session ended" |
| Fix | `LeaveForm`, `ChangeCredentials`, `Form`, `LeaveManagement` and `LeaveApproval` now extend `JDialog`, are constructed modal with the opening window as owner, and use `DISPOSE_ON_CLOSE`. Duplicates need no singleton field: a modal child blocks the parent, so the button that opens it cannot be clicked twice |
| Navigation | Leave Management and Pending Leaves replace each other, so each hands the owner up one level and disposes itself before opening the modal child — a modal child blocks inside `setVisible()`, which would otherwise leave the old screen open behind it |
| Form metadata | the five `.form` files were switched to `JDialogFormInfo` with `defaultCloseOperation` 2, so the NetBeans designer matches the classes |
| Evidence | `oop/ki22-modal-popups-output.txt` — before: `close = 3`, one `WINDOW_CLOSING` dispatch kills the JVM; after: five screens `JDialog / modal=true / close = 2`, all five closed, `STILL ALIVE`, exit 0 |
| Left alone | `EXIT_ON_CLOSE` stays only on the top-level windows: `LoadingScreen`, `LandingPage`, `Login`, `LoginHR`, `Dashboard`, `MainMenu` |
| No regression | `ConsoleBackendTest` 91/91, `LeaveServiceTest` 44/44, `AttendanceRepositoryTest` 33/33, `javac -Xlint:all` 0 errors |
| GUI verification | focus and duplicates confirmed by the tester who filed the issue (Ethan, Eclipse, after pulling main @ a29fd2b): "The window focus is now stolen by the popups, so hindi na possible yung duplicates." The third symptom needed the real close path, because Cancel only calls `dispose()`: a `WM_CLOSE` posted to the popup's HWND (the same message the taskbar's "Close window" sends) closed the form and left the app running (`APP_ALIVE=YES`, `POPUP_GONE=True`). Details in `oop/ki22-modal-popups-output.txt` |

## Add Employee form fits the screen and always has an exit (KI-20, 2026-11-01)

Known issue 20 in the MS2 Known Issues List: the Add Employee form's buttons are cropped at the
bottom, the window is not resizable, and there is no visible exit.

| What | Evidence |
|---|---|
| Cause | `Form.initComponents()` ended with `setSize(1012, 734)` on a `setUndecorated(true)` / `setResizable(false)` `JDialog`, while the content's own preferred size is 1034 x 714. Nothing tied the window to the screen it opens on |
| Why it is screen-dependent | `setLocationRelativeTo(null)` centres the dialog on the **screen**, so the height that has to fit is the display, not the content. On a 1366 x 768 laptop at 125% scaling the logical screen is 1093 x 614: the dialog opens at y = -60 and the button row (y 656-682 inside the dialog) lands at screen y 596-622 — 48 px below the 574 px work area. At 100% on the same laptop the row sits at 673-699 and is visible, which is why the defect looked machine-specific |
| No exit | the dialog is undecorated, so there is no title bar and no X; `Cancel` is the only exit and it lives in the cropped row, leaving the taskbar as the only way out |
| Fix | `fitToScreen()` runs after `initComponents()`: size the dialog from the content's preferred size instead of the magic number, clamp to `GraphicsEnvironment.getMaximumWindowBounds()`, wrap the content in a `JScrollPane` when it still does not fit, and bind Escape to `dispose()`. `Cancel`, `DISPOSE_ON_CLOSE` and modality are unchanged |
| Evidence | `oop/ki20-screen-fit-output.txt` — 22 assertions, 0 failures: old arithmetic leaves 48 px of the button row below the work area at 1366x768 @125% and 55 px at 1024x600; the fixed form opens 1034x714 on the 1920x1080 dev screen with both buttons inside the work area; an 800x500 window turns scrollable and `Cancel` is reachable after scrolling; Escape disposes the form and the JVM survives. The probe is committed as `test/motorphemployee/Ki20ScreenFitProbe.java` — in Eclipse, right-click it and Run As → Java Application |
| Left alone | the other screens keep their fixed sizes: only `Form` was reported, and `ChangeCredentials` / `LeaveForm` are small dialogs that fit everywhere |
| Eclipse | builds with Eclipse's own compiler, not just `javac`: ECJ 3.46.100 on all three source roots gives 107 class files, 0 errors and 39 warnings, every one of them a pre-existing unused local. Running the probe from the ECJ output also gives 22 of 22. `test/` is already a source folder in `.classpath`, so no Eclipse project file had to change |
| Not yet verified | a click-through on hardware that actually needs the scroll pane — the probe forces the window size instead of running on a 1366x768 @125% machine |
| No regression | `ConsoleBackendTest` 91/91, `javac -Xlint:all` 0 errors |

## How to build and run (verified commands)

```bash
# compile the backend
javac -encoding UTF-8 -Xlint:all -d <outdir> \
      C:/Users/Paul/Documents/MO-IT110-MotorPH-OOP-Team3/oop/src/motorphemployee/oop/*.java

# run the console test (exit code 0 = all pass)
java -cp <outdir> motorphemployee.oop.ConsoleBackendTest
```

```bash
# compile the GUI + backend + the leave evidence test
javac -encoding UTF-8 -d <outdir> $(find src oop/src -name '*.java') test/LeaveServiceTest.java

# run the leave evidence (exit code 0 = all pass)
java -cp <outdir> LeaveServiceTest
```

```bash
# compile the GUI + backend + the attendance evidence test
javac -encoding UTF-8 -d <outdir> $(find src oop/src -name '*.java') test/AttendanceRepositoryTest.java

# run the attendance evidence (exit code 0 = all pass)
java -cp <outdir> AttendanceRepositoryTest
```

Evidence files in this folder: `console-test-output.txt` (the actual run),
`parity-report.txt` (CP2 vs OOP, per input), `cp2-parity-output.txt` (frozen CP2 formulas),
`leave-fix-test-output.txt` (the leave workflow run), `attendance-test-output.txt` (the timecard run),
`ki22-modal-popups-output.txt` (the popup close probe), `ki20-screen-fit-output.txt` (the Add Employee screen-fit probe).
