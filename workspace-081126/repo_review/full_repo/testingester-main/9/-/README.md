# MotorPH Payroll System: OOP Refactor

MO-IT110 Object-Oriented Programming. Term 1, SY 2026-2027. Section S2101, Team 3.

| Team member |
|---|
| John Paul Pore |
| Jan Fermin Valencia |
| Ethan Caleb Racimo |
| Myles Tumbaga |

## What this repo is

We refactored the MotorPH Payroll System. The old program was procedural Java that read and wrote CSV files. It came from MO-IT103, Computer Programming 2. The new one is object oriented and follows the Milestone 1 design we submitted.

The four principles are in the code: encapsulation, abstraction, inheritance, polymorphism.

The repo holds the source, the worksheet revisions, the test output and the documentation. If you change one, check the others still tell the same story.

## Where we started

`src/motorphemployee/` is the CP2 package. We refactored it in place so the old screens still open and run. What changed:

- `MainMenu` and `Dashboard` call the OOP backend now. Neither one computes payroll anymore.
- The popup screens are modal dialogs. Closing one does not close the app.
- Two service classes were added, `LeaveService` and `AttendanceRepository`.
- The payroll screens read the real timecard, `attendance.csv`.

These files were left alone: `data.csv`, `leaves.csv`, `pendingleaves.csv`, `nbproject/`, `build.xml`. If you need the code before any of our edits, it is in commit `655d43d`. The old README is [README.CP2.md](README.CP2.md).

The baseline came from MO-IT103, Computer Programming 2, A1101, Group 4. Members: John Paul P., Eliakim Set, El Chad Chavez, Emersson Aporado, Nesty Loy.

- Original repo: <https://github.com/eliakimset/MO-IT103-A1101-CP2-Group-4>
- Our copy: <https://github.com/J0eychnpulpey/MO-IT103-A1101-CP2-Group-4>

What was wrong with it, based on our Milestone 1 design:

| Problem | Where |
|---|---|
| God classes that hold the UI and the payroll logic | `MainMenu.java` (~1.6k LOC), `Dashboard.java` (~1.4k LOC) |
| The same four `calculate*()` methods, copied twice | `MainMenu.java` lines 1279, 1378, 1396, 1418 and `Dashboard.java` lines 1195, 1294, 1312, 1334 |
| Fields are private, but the setters take any value. `setBasicSalary(-5000)` passes | `Employee.java`, 19 fields |
| Passwords stored in plain text | `HRLogin.csv`, `data.csv` |
| Data lives in CSV files, each module loads and saves its own | `data.csv`, `leaves.csv`, `pendingleaves.csv`, `HRLogin.csv` |

## Where we are

| Deliverable | Status |
|---|---|
| MS1, OOP design package | Done. 16 unique classes, 35 CRC rows and 97 method rows, built across W2 to W5. |
| MS2, OOP implementation | Code complete for the backend. 12 of the 16 design classes have a class file, the other four are design-only. The numbers are below. |
| TA, documentation | Waiting on external QA and the final documentation. |

MS2 in numbers:

- 13 files in `oop/src/motorphemployee/oop/`, 12 classes plus the console harness.
- `javac -Xlint:all` on `oop/src` and `test`: 0 warnings, 0 errors.
- Console suite: 91 of 91 checks pass.
- GUI rewired to `PayrollCalculator`, and 8 duplicated `calculate*` methods removed.
- Smoke test checklist: 5 of 5, all Yes.
- Gross pay against the CP2 baseline: 408 of 408 employee-months, 0 differences.

## Class design by week

W2 is encapsulation. The classes are `Employee`, `PayrollCalculator`, `AttendanceLog`, `LeaveManagement`, `Credentials` and `HRManager`.

W3 is abstraction. `Employee` becomes the abstract base, with `RegularEmployee` and `ProbationaryEmployee` under it. `GovernmentContributionCalculator` is abstract too, and `SSSCalculator`, `PhilHealthCalculator`, `PagIbigCalculator` and `WithholdingTaxCalculator` extend it. `PayrollCalculator` runs them.

W4 is inheritance. `Person` to `Employee` to `RegularEmployee` or `ProbationaryEmployee`. The calculators follow the same shape.

W5 is polymorphism. The code holds parent type references (`Employee`, `GovernmentContributionCalculator`) and the overridden version runs. One loop covers all four calculators.

Rules we followed:

1. The GUI only presents. Payroll math lives in the backend.
2. Every CP2 payroll feature still has to work.
3. Test the backend from the console before touching the GUI.

## Running it

[SETUP.md](SETUP.md) has the steps for VS Code, Eclipse and the terminal. Eclipse needs nothing extra, `.project` and `.classpath` are committed. VS Code needs the Extension Pack for Java.

There are two source roots. `src` is the CP2 GUI and `oop/src` is the new backend.

- Main entry point: `motorphemployee.LoadingScreen`, then `LandingPage`, then `Login`
- Console test harness: `motorphemployee.oop.ConsoleBackendTest`
- Sample login: employee number 1 to 34, username `admin`, password `password`

Those credentials are the CP2 demo data, all fictional employee records for the course. The MS1 design defines `Credentials.authenticate` and `changePassword` with hashed storage. We did not migrate that in this refactor. It is an open item in the MS2 Known Issues list, so the sample data stays as it is.

## What is in the backend

| File | What it shows |
|---|---|
| `oop/src/motorphemployee/oop/` | 12 backend classes plus the `ConsoleBackendTest` harness |
| `oop/console-test-output.txt` | Console suite, 91 of 91 checks pass |
| `oop/cp2-parity-output.txt` | The CP2 baseline output the refactor is compared against |
| `oop/parity-report.txt` | CP2 and OOP deductions side by side on 25 salary inputs. Mismatches here are expected, see the deviation note below. |
| `oop/matrix-verification.md` | The four statutory deductions checked against the official government matrices |
| `oop/NOTES.md` | Scope, documented deviations, open items and the dated fix logs |
| `oop/attendance-test-output.txt` | Timecard import, 33 assertions, 0 failures over 5,168 rows and 34 employees |
| `oop/leave-fix-test-output.txt` | Leave approve and reject, 44 assertions, 0 failures |
| `oop/gui-retest-output.txt` | Click through of approve and reject on the running app |
| `oop/ki20-screen-fit-output.txt` | Add Employee screen fit, 22 assertions, 0 failures |
| `oop/ki22-modal-popups-output.txt` | Popup modality and close behaviour, before and after the fix |

Two service classes sit in `src/motorphemployee/`:

- `LeaveService` is the only writer for leave approve and reject. The request leaves `pendingleaves.csv`, one row goes into `leaves.csv` in header order, and the leave credits move. If any step fails, all three files are restored.
- `AttendanceRepository` reads the real timecard, `attendance.csv`, into `AttendanceLog`. The period picker only offers months that have records.

The four statutory deductions follow the official government matrices. Where that differs from the CP2 formula, the difference is on purpose. It is written up in `oop/NOTES.md` and in the MS2 Known Issues list. Gross pay itself does not move, 408 of 408 employee-months, 0 differences.

## Still open

- Four classes from the MS1 design have no class file yet: `Credentials`, `LeaveRequest`, `LeaveManagement` and `HRManager`. Section A of the MS2 worksheet marks them as design-only, and they belong to the TA wave.
- Employee records are still read with `line.split(",")` in `MainMenu` and `Dashboard`. A comma inside a field shifts every column after it. The fix is a quote-aware parser, same wave. Logged in the Known Issues list.
- Passwords are stored in plain text in `data.csv` and `HRLogin.csv`. The MS1 design defines hashed storage, and we did not migrate it in this refactor. Logged as Unresolved.
- `LeaveForm` still appends to `pendingleaves.csv` itself on submit instead of going through `LeaveService`. Logged in `oop/NOTES.md`.
- The HR dashboard label "Gross Semi-Monthly Rate" overlaps the field under it. Display only, no effect on the numbers. Logged as Unresolved.
- TA: external QA in W11 and the final documentation in W12.

## Repo rules

- `main` carries the refactor chain for this milestone. Each step is built and tested before it is pushed.
- Commit messages: a short imperative subject, then what changed.
- Never commit `build/`, `bin/`, `dist/`, `*.class`, `nbproject/private/`, `.vscode/`.
- Committed on purpose: `.project`, `.classpath`, `.settings/`, so every member can import the project in one step.
- Keep the docs consistent. The app, the Milestone worksheet, the test evidence and this repo have to describe the same work.

The baseline code belongs to the original MO-IT103 Group 4 authors. We reuse it as the required source for MO-IT110. Their README is kept as [README.CP2.md](README.CP2.md).
