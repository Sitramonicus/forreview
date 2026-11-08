# Gap analysis: the design document vs this build

Read this before you open the app expecting the document. It is the honest scoreboard.

Source: **MO-IT108 2nd Milestone — System Design Document** (use case view, RBAC matrix, and the 17
modules in §2.2.2), plus **BRS** §3 and **MO-IT107** database design.

---

## The scoreboard

Legend: **Built** = works and is tested · **Partial** = exists but not what the document describes ·
**Missing** = nothing there.

| # | Module (design doc §2.2.2) | State | What exists | What is missing |
|---|---|---|---|---|
| 1 | Employee Login — EID + password + **2FA**, throttling, lockout, session kill | **Partial** | Firebase email/password sign-in; every request carries the token | **No 2FA**, no EID-based login, no failed-attempt throttle, no lockout, no "terminate old sessions" |
| 2 | Clock In/Out — off-site/on-site, **QR**, day type, grace period, tardy tag | **Missing** | Reading stored timecard rows only | All of it |
| 3 | View Attendance — present/absent/tardy counts, OT/undertime hours | **Partial** | Raw row counts and dates | **No classification**: no present/tardy/absent, no OT/undertime hours, no schedule comparison |
| 4 | Leave Calendar — bi-monthly team calendar, application status | **Partial** | A month grid; submit and cancel a request | **No approval**, no accrual balance, no team scope, no bi-monthly view |
| 5 | Salary Tracker — precomputed snapshot + "Estimate as of" | **Missing** | — | The whole snapshot cache |
| 6 | Tickets — 8 types, routed by type, queue position | **Partial** | One free-text ticket; create, list, cancel | Types, routing, queue, conversation, notes, audit |
| 7 | Team roster — availability, day type per member | **Partial** | Read-only list of all 34 | Today's availability, day type, team-only scope |
| 8 | View/Edit Schedules — single/multi-day/weekly patterns | **Missing** | Fixed schedule IDs 101–104 | All of it |
| 9 | Change Employee Timesheet — approve/edit attendance | **Missing** | — | All of it |
| 10 | Onboarding — add hire, assign ID, queue | **Partial** | Create an employee record | Onboard queue, clearance steps, asset setup |
| 11 | Offboarding — departure, clearance, mark inactive | **Missing** | — | All of it |
| 12 | View/Update/Add Member Info — incl. **mid-cycle rate change** | **Partial** | Add/edit record and pay | No effective-dated rate from the UI, no audit of who changed what |
| 13 | Handle Tickets (HR / Payroll / Supervisor) | **Missing** | — | Any queue-and-handle view |
| 14 | Apply Payroll Changes — individual/team/global adjustment forms | **Missing** | — | All of it |
| 15 | Review Changes — audit trail, approve/reject | **Missing** | — | All of it |
| 16 | Payroll Report — consolidated summary | **Partial** | One saved run at a time | Multi-employee run, department/branch totals |
| 17 | Distribute Payslips — PDF + email + retry | **Missing** | Payslip CSV export | PDF, email, retry, delivery status |

**Tally: 0 modules complete to the document's definition. 8 partial. 9 missing.**

That is the honest answer to "we have nothing". It is not nothing — the payroll engine, the
permission matrix, and the data layer are real — but against a 17-module design document, most of
the product is not built.

---

## What is genuinely solid

| Layer | Evidence |
|---|---|
| Payslip arithmetic | Reproduces MotorPH's own payslip to the centavo; 70 golden checks |
| Statutory tables | 2025 and 2026 buckets, SSS bracket banding, PhilHealth, Pag-IBIG, BIR |
| Permissions | The 12×5 RBAC matrix from the design doc, transcribed and checked cell by cell (95 checks) |
| Payroll determinism | Saved runs are immutable and reprint from stored figures |
| Effective-dated rates | `SalaryHistory` pays the rate in force at period end; a raise no longer rewrites old payslips |
| Payroll periods | One dated cycle for everyone, validated against monthly/semi-monthly cadence |
| The data | All 34 people from your CSV, including the reporting lines from `Immediate Supervisor` |

---

## The three things I got wrong

1. **I said "any Firebase account, no account types."** Your design document defines five roles and
   a 12-entity matrix. Those are now coded and tested. I had read the file and failed to apply it.
2. **I let each payslip set its own dates.** Your point was exactly right: a payroll period is one
   set of dates for everybody. There is now a `PayrollPeriod` with a cadence check, and a run that
   does not match its period is refused.
3. **There was no salary history.** Without it, editing a rate silently changed every past
   calculation. There is now a dated rate table, and the March record you predicted is derivable.

---

## What "done" would actually take

Not a rewrite. The engine and the data layer are usable. What is missing is the workflow skin:

| Phase | Work | Why it is the order |
|---|---|---|
| 1 | Payroll period UI + multi-employee run + per-department report | Turns the engine into a payroll *cycle*, which is the core of the whole document |
| 2 | Tickets with type, queue, thread, audit | Every other workflow rides on this |
| 3 | Approvals that write records (leave, timesheet, rate) | Makes tickets consequential |
| 4 | Clock in/out + attendance classification | Needs periods and schedules to mean anything |
| 5 | Schedules, onboarding/offboarding queues | Needs roles and audit already in place |
| 6 | 2FA, throttling, session control | Hardening, once the flows exist |
| 7 | Payslip PDF + email distribution | Last, because it depends on the run being right |

Each phase is independently shippable. None of them require undoing the payroll engine.
