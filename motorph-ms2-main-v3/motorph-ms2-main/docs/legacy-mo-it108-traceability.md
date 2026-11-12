# Legacy MotorPH coursework → current MS2 traceability

**Purpose:** use the user's earlier MO-IT108 MotorPH documents to clarify the current prototype without silently expanding its approved scope. These are earlier-course sources (A.Y. 2024–2025), not the active MO-IT110 MS2 rubric. They can inform design and requirements mapping, but do not override the current boundary: only an isolated synthetic 2026 monthly statutory preview is enabled; attendance-based or persisted payroll remains disabled.

## Sources inspected

- `/home/user/uploads/MO-IT108 - 1st Milestone_ Requirements Analysis Document of MotorPH Payroll System H1103 Torres, A. (1).pdf` — a requirements document with ten functional-requirement groups and non-functional requirements.
- `/home/user/uploads/MO-IT108 - 2nd Milestone_ System Design Document of MotorPH Payroll System H1103 Torres, A. (1).pdf` — module descriptions, scenarios, data concepts, and an RBAC matrix.
- `/home/user/uploads/MO-IT108 - Terminal Assessment_ Business Technology Proposal for MotorPH Payroll System H1103 Torres, A..pdf` — a broad technology proposal. Several major sections still contain template prompts/placeholders, so treat it as vision/context rather than an approved detailed specification.
- `/home/user/uploads/The Code.pdf` — C++ inventory-management iterations using stock fields such as ID, brand, age, date, and status. Despite a MotorPH inventory CSV filename, it is not a payroll system or a reusable MotorPH payroll module. No code from it was integrated.

## What the legacy documents clarify

### Attendance view

The MO-IT108 Requirements Analysis says employees can view attendance metrics over a date range, defaulting from the earliest to latest available record. The System Design expands this into daily logs and counts for present, absent, tardy, overtime, undertime, and leave usage. It also names concepts such as `ClockInStatus`, `ShiftStartTime`, `ShiftEndTime`, `GracePeriod`, `AttendanceTag`, `TotalWorked`, `WorkHourTag`, `AbsentCount`, `PresentCount`, `TardyCount`, `OvertimeHours`, `UndertimeHours`, `LeaveTaken`, and `LeaveBalance`.

This clarifies the intended *shape* of the attendance screen, but not a complete calculation policy. The documents do not provide actual grace-period values, the absence threshold, break treatment, rounding, overnight-shift handling, missing-punch corrections, timezone policy, overtime triggers, or a worked expected example. One field is described as `TotalWorked` based on clock-in/out, but that alone does not settle those rules. Therefore the app now defaults the raw-record filter to the selected synthetic employee's earliest and latest available dates; the user may narrow the inclusive range. This is a data-range convenience, **not** a payroll period, and no hour/category totals are derived.

### Payroll snapshot and pay cycle

The legacy System Design lists proposed snapshot fields such as `PayrollCycleStart`, `PayrollCycleEnd`, `HourlyWage`, `RegHours`, `OTHours`, `PTOHours`, holiday hours/shifts, incentives, tax, other deductions, gross/net pay, and a snapshot timestamp. The Requirements Analysis also names wage components, incentives, statutory/other deductions, payroll reports, and payslip distribution.

These are useful field names and workflow hints, not complete formulas or full current policy. The documents do not give exact semi-monthly cut-off dates, salary-to-hourly conversion, attendance rules, benefits treatment, rounding policy, or a complete expected payslip. A separate synthetic 2026 statutory preview now covers only monthly SSS/PhilHealth/Pag-IBIG and BIR withholding; employee-linked salary snapshots, attendance calculations, saved payroll, and payslips remain disabled.

### Identity and roles

The legacy documents describe Employee, Supervisor/Lead, HR, Payroll Staff, and System Admin roles. Their RBAC matrix distinguishes self/team access, HR/Payroll actions, and approvals for government details; the requirements also call for 2FA and branch-aware access. The current prototype has Firebase Email/Password sign-in and an isolated per-user synthetic sandbox only. It does **not** implement 2FA, branch routing, team membership, HR/Payroll roles, production PII handling, or the legacy RBAC matrix. No real employee information is permitted.

## Ten legacy functional-requirement groups vs current prototype

| Legacy requirement | Current state | Notes |
|---|---|---|
| 1. Employee login | **Partial** | Firebase Email/Password sign-in is live; no employee-ID login, 2FA, branch routing, or production session/RBAC policy. |
| 2. Clock in/out | **Not implemented** | No live clocking, QR verification, or attendance-write workflow. |
| 3. Employee attendance | **Partial** | Raw synthetic entries, selected employee, inclusive date filters, and earliest–latest default. No absent/present/tardy/overtime/undertime/leave summaries. |
| 4. Salary tracker | **Intentionally disabled** | The source's snapshot fields do not define a full calculation or authoritative values. |
| 5. Tickets | **Partial, owner-scoped create/cancel** | Fictional ticket submission/cancel and queue labels for overtime, leave, timesheet, payroll, HR, and technical support; nothing is delivered or resolved. Published user-scoped Firestore rules were live-smoke-tested for ticket create/cancel. |
| 6. Leave tracker/calendar | **Partial, owner-scoped create/cancel** | Validated fictional request submit/cancel plus a display-only calendar of submitted/cancelled date ranges; no balance/accrual, approval, payroll treatment, or approved leave calendar. Published user-scoped Firestore rules were live-smoke-tested for leave create/cancel. |
| 7. Team management | **Partial read-only roster** | Shows synthetic directory records; no team membership, schedules, supervisor scope, or approval workflow. |
| 8. HR handling | **Partial synthetic directory maintenance** | Signed-in Firebase users can create/update allowlisted fictional directory fields in their own sandbox; local mode is unauthenticated process memory. No HR role, schedule assignment, delete, onboarding/offboarding, or sensitive PII. |
| 9. Payroll supervision | **Preview-only partial** | A separate synthetic monthly statutory calculator exists; no employee-linked payroll, adjustments, approvals, payslips, or reports. |
| 10. Payroll distribution | **Not implemented** | No payslip generation, email delivery, retry handling, or distribution status. |

## Percent estimate: two different denominators

There is no single valid percentage “against the submitted folders”: those projects have different feature sets, tech stacks, and course scopes, and several are near-duplicates. The ranges below are **feature-completeness estimates, not a grade or quality score**.

- **Agreed current Tasks 1–3 scope: about 85–90%.** Task 1's live Firebase proof is complete. Task 2's browser sign-in and employee selection are verified on localhost, with the exact Arena preview-origin authorization still unverified. Task 3's employee/date inputs and raw-record path are complete; derived attendance-hour totals remain gated because their algorithm and test oracle are missing. The separate monthly statutory preview does not enable attendance-based payroll.
- **Full legacy MO-IT108 functional breadth: roughly 25–35% represented, but less operationally complete.** The app has a real sign-in/data foundation, limited owner-scoped synthetic directory editing, local-only create-only raw attendance for mapped synthetic employees (Firebase remains read-only), and owner-scoped leave/ticket create/cancel routes (published user-scoped Firestore rules live-smoke-tested for leave/ticket create/cancel). These limited workflows do not include reviewers or approvals. Clocking, leave balances/approvals/approved calendars, delivered and resolved tickets, actual team/supervisor management, HR lifecycle operations, full payroll calculations (beyond the temporary statutory preview), payslips, reports, RBAC/2FA, and production operations remain unimplemented.
- **Compared with the more complete numbered submissions (especially folders 6 and 9): roughly 25–35% of end-to-end feature breadth.** This prototype is stronger in live Firebase Auth/Firestore integration, browser accessibility/responsiveness, and current test evidence; those reference projects include end-to-end payroll engines/results and broader workflows that this app deliberately does not yet have; the temporary monthly statutory calculator is not an attendance-to-payslip engine. Compared with the smallest procedural/demo submissions, the comparison can reverse because this project already has a live web/data foundation.

The largest gap to “parity” is not another Firebase feature: it is the **unbuilt payroll/business workflow** (validated attendance summaries, approved pay rules, payroll results/payslips, and applicable roles). Do not close that gap by importing unverified formulas from another submission.
