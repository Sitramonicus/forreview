# RBAC conformance: the design document's permissions matrix

The brief requires the five account types and the access matrix to be **enforced, not decorative**. The matrix is
the one on page 11 of `MO-IT108 - 2nd Milestone_ System Design Document of MotorPH Payroll System`.

## The matrix, transcribed

C = Create, R = Read, U = Update, D = Delete, A = Approval, Req = Request.

| Table / entity | System Admin | HR | Payroll | Supervisor | Employee |
|---|---|---|---|---|---|
| Employee | C, R, U, D | C, R, U | R | R (Self) | R (Self) |
| Address | C, R, U, D | C, R, U | R (Self) | R (Self) | R (Self) |
| Department | C, R, U, D | C, R, U | – | – | – |
| Position | C, R, U, D | C, R, U | – | – | – |
| Leave | C, R, U, D | C, R, U | R (Self) | R, U (Team) | R (Self) |
| **Attendance** | **C, R, U, D** | **R** | **R** | **R, U (Team)** | **R (Self)** |
| Payroll | C, R, U, D | R | C, R, U | R (Self) | R (Self) |
| Payslip | C, R, U, D | R | C, R, U | R (Team) | R (Self) |
| Earnings / Deductibles | C, R, U, D | R | C, R, U | R, U (Team) | – |
| Government Deductibles | C, R, U, D | R | R | – | – |
| Government Details | C, R, U, D | A | A | – | – |
| Tax Table / Rates | C, R, U, D | – | – | – | – |

## Where the code had drifted, and the correction

Working through this table turned up three places where `AccessPolicy` was more generous than the document. All
three were corrected in the code, not worked around in the test:

1. **Attendance was writable by HR.** The matrix gives HR `R` on Attendance only — the supervisor holds
   `R, U (Team)` and the System Administrator holds `C, R, U, D`. `requireAttendanceEdit` now admits only those
   two. A timesheet correction is a team-lead act; HR reads the attendance but does not rewrite it.
2. **Schedules were editable by HR.** Schedules are not a row in the matrix, so the role descriptions decide it: page
   21 gives the Supervisor "edit single/multi/weekly schedules", while page 22 gives HR onboarding, offboarding,
   ticket handling and employee records. `requireScheduleEdit` follows the supervisor.
3. **Team scope is recursive.** The matrix's "(Team)" is resolved through `OrgChart`, so a supervisor reaches every
   descendant, not just direct reports. Reads use the recursive scope; *writes* to attendance, schedules and
   approvals stay on the direct line, because the matrix's `U` is the supervisor's own team and a manager should not
   silently rewrite a team leader's decisions one level down.

## What the matrix implies for the CMO

The CMO is not a C-level financial viewer in this system. The document's five account types are System Admin, HR,
Payroll, Supervisor and Employee; the CMO is a **department head with a three-person team**, so it is modelled at
supervisor level with `TEAM` scope: it reaches employee, attendance, overtime, ticket, payslip and earnings data for
its own chain (10032, 10033, 10034) and nothing company-wide. It holds no `REPORTS`, `STATUTORY` or `REMITTANCE`
visibility. This is exactly what the brief asks for, and `AccessPolicyTest` asserts it.
