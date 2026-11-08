# Where this build actually stands

You asked, in effect: *the design document defines five account types and a full payroll lifecycle,
and you gave me "any Firebase account" and one payslip at a time.* That was correct, and this document
is the honest answer to "so what exists now".

---

## 1. The parts I had wrong, and what replaced them

### "Any Firebase account" → five roles, enforced

I had read MO-IT108 and shipped an app where signing in was the whole authorization story. The
document's §2.2.1 is a **12-entity × 5-role matrix**; it is now the thing that decides every request.

- `Policy.java` — the matrix, cell by cell, as the single source of truth.
- `AccessControl.java` — enforces it, including "self" and "my team".
- `RoleAssignment.java` — maps your 34 employee numbers to a role (seed below).
- `RbacPolicyTest` — **229 checks**, including a cell-by-cell comparison against a matrix typed
  independently from the document.

The role is **never** taken from anything the browser sends. The browser says *who it is*
(employee number); the server decides *what that person may do*.

**"My team" is not a table anyone maintains.** It is the `Immediate Supervisor` column of MotorPH's
own employee file. The reporting lines resolve 100%: the CEO (10001) has no supervisor, everyone else
does. 10015 Fredrick Romualdez has 10016 and 10017 reporting to him; that is why his directory shows
exactly three people.

### One payslip at a time → company-wide periods

`PayrollPeriod` defines a cutoff once, for everyone, and validates it against its cadence:

| Cadence | Must be |
|---|---|
| `MONTHLY` | 1st → last day of one month |
| `SEMI_MONTHLY` | 1st → 15th, **or** 16th → last day |

A typed `30` for February is refused. A ten-day range is refused. A range spanning two months is
refused. A run that names a period must carry that period's dates and cadence — which is the specific
bug you were pointing at: two employees could be paid for two different ranges in one run. Once a
period is `CLOSED`, no further runs are accepted for it, for anybody.

### No salary history → dated rates that cannot rewrite the past

Editing an employee's salary used to change every payslip that was recomputed. Now:

- A rate is a **row with a date**, not a field on the person.
- Every employee starts with one opening rate taken from the employee file (effective 2025-01-01).
- Payroll Staff submit further rows; the effective date must be the **first of a month**, because a
  cycle cannot be paid at two rates.
- A run that declares a cycle must reconcile with the **rate in force at that period's end**. Paying
  January at August's rate is refused, and the refusal names both figures.
- Two rates for the same person and date are refused.

Demonstrated end to end: a raise to ₱60,000 effective 2026-07-01 leaves January paying ₱53,500 and
August paying ₱60,000. `PayrollPeriodAndHistoryTest` (34 checks) and
`AppServerIntegrationTest.localModeKeepsAnEarlierPayslipAtItsOwnRate` pin it.

---

## 2. Who may do what (the matrix, in words)

| | System Admin | HR | Payroll Staff | Supervisor | Employee |
|---|---|---|---|---|---|
| Employee record | full | create / read / update | read | read self | read self |
| Attendance | full | read | read | **read + update own team** | read self |
| Payslip | full | read | **create / read / update** | read own team | read self |
| Payroll runs | full | read | **create / read** | own team only | own only |
| Tax tables | **full** | — | — | — | — |
| Government details | full | approval required | approval required | — | — |

`/api/me` returns this matrix as the server resolved it for the caller, so the UI never has to guess.

**Refusals are real HTTP statuses with a reason**, e.g.
`403 "That employee is not one of your direct reports."` A directory listing is **filtered**, not
blocked, so a reply can never contain a row the caller may not read. An unlinked account's listing is
empty **and** reports `total: 0` — it does not learn how many people MotorPH employs.

### Demonstration role seed

Based on the real positions in your CSV, and flagged as a demonstration in `/api/me`
(`roleSeedIsDemonstration: true`) because it is not a personnel policy:

| Role | Employee numbers |
|---|---|
| System Admin | 10005 (IT Operations and Systems) |
| HR | 10006 (HR Manager), 10007, 10008, 10009 |
| Payroll Staff | 10010 (Accounting Head), 10011 (Payroll Manager), 10012 (Payroll Team Leader), 10013 (Payroll Rank and File) |
| Supervisor | 10001, 10002, 10003, 10004, 10015, 10016 |
| Employee | everyone else |

---

## 3. The 10015 phone allowance: still unresolved, now visible everywhere

MotorPH's own files disagree. Payslip `15-2026-01-15` pays **2,000.00** phone allowance; the employee
master file says **1,000.00**. I have not smoothed this over:

- The **payroll screen states the conflict in words**, next to the figures.
- `/api/payroll/rules` returns it under `dataConflicts`.
- The **golden test still reproduces MotorPH's payslip to the centavo from the payslip's own numbers**
  (net 22,711.67).
- The **salary history follows the employee master file** (1,000.00), because that is the record HR
  maintains and the one onboarding and rate-change workflows write to. Consequently a *cycle* run that
  submits 2,000.00 for 10015 is **refused** with both figures named, rather than silently computed.

Which number is right is MotorPH's decision, not something to bury in a lookup table. The question is
still on the table: **1,000 or 2,000?**

---

## 4. Ticketing: research done, design written, implementation not started

You told me to study frappe/helpdesk, opensupports and osTicket first. That is in
`docs/ticketing-design.md`, including what all three agree on and what does not transfer.

The short version: all three separate **type**, **queue**, **conversation** and **audit trail**, and
all three are *customer support* tools — which is the part that does not transfer, because a payroll
ticket is a request to change a payroll record that must be approved and then applied.

What exists today: create / list / cancel one free-text ticket with a `queue` label nothing enforces.
That is a filing cabinet with one drawer. It is described as such in the design doc, not as a ticket
system.

---

## 5. The honest scoreboard against the design document's 17 modules

**0 complete, 8 partial, 9 missing.** Full table in `docs/gap-analysis-vs-design-document.md`.

The eight partials have real substance behind them — the payroll arithmetic, the statutory tables,
the permissions and the data layer are not stubs — but none of them yet meet the document's own
definition of the module.

The nine missing ones are, in the order I would build them: payroll-cycle UI and multi-employee runs,
tickets, approvals that write records, clock in/out with attendance classification, schedules,
onboarding/offboarding queues, 2FA, and payslip distribution.

---

## 6. What is verified right now

| Suite | Result |
|---|---|
| `./test.sh` | **547 checks PASS** |
| `npm run test:web` | **62 / 62 PASS** |
| `npm run test:e2e` (real browser) | **17 PASS** |

The e2e run includes a 390px mobile check, which caught a real regression: my identity banner and the
acting-as picker pushed the layout 233px wider than a phone viewport. Fixed.

---

## 7. Looking at it

The dev server is running in **fixture mode** (isolated process memory, no Firebase project needed).
There is an **ACTING AS** picker in the top bar: choose an employee number and everything re-loads as
that person, because every permission decision is made server-side from that number.

Worth trying, in this order:

1. **10015 Fredrick Romualdez** (Supervisor) → Employees shows **three** people: himself and his two
   reports. The banner says why.
2. **10006 Andrea Mae Villanueva** (HR) → Employees shows **34**.
3. **10011 Anthony Salcedo** (Payroll Staff) → Payroll shows the compute form and the run history.
4. **10018 Allison San Jose** (Employee) → her own record only; no payslip for anyone else.

`GET /api/me` for any of them returns the resolved matrix, so you can check the code's answer against
the document's table yourself.
