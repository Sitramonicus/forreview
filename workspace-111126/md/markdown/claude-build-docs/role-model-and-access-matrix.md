# Role model and access matrix — gap analysis

**Date:** 2026-11-06 · **Input:** your five role definitions, the RBAC matrix in `message (2).txt`,
and the permission diagram (5 nesting scopes + 8 permission bubbles).

**Status: implemented.** The five roles and the 12×5 matrix below are in `Policy.java` (the single
source of truth), enforced by `AccessControl.java` on every route, and checked against an
independently typed copy of the document matrix by `RbacPolicyTest` (229 checks). Direct reporting
lines come from the `Immediate Supervisor` column of the supplied employee file, so "my team" is a
fact about MotorPH's data rather than a table someone has to maintain.

Two notes on what implementation changed:

- A sixth, **non-role** state `APPLICANT` exists for a signed-in account HR has not linked to an
  employee number. Every matrix cell for it is `NONE`. Without it a new account could not sign in to
  ask for access, and the alternative — defaulting to `EMPLOYEE` — would hand payroll data to anyone
  with an email address.
- The Employee cell for a Supervisor is `R (Self)`, but a supervisor legitimately needs the name and
  placement of their direct reports. That access is granted through the team-scoped entities
  (Attendance, Payslip) rather than by widening the Employee cell, so the matrix still matches the
  document.

---

## 1. Your spec, restated precisely

| Role | What you asked for |
|---|---|
| **Employee** | Clock in/out, view own attendance, own salary tracker, submit tickets (Overtime, Leave, Timesheet, Payroll dispute, Tech support), view team members, check team leave |
| **Supervisor** | Everything Employees do **plus** manage team schedules, approve OT/leave, handle timesheet edits, and raise promotion / merit / bonus / disciplinary / unpaid-leave requests that change earnings |
| **Payroll** | Gross-to-net (earnings + OT + bonuses + allowances), statutory and voluntary deductions, disburse, remit, generate payslips |
| **HR** | Everything Supervisors do plus onboard/offboard, employee records, HR tickets |
| **System Admin** | Configuration, roles, security policies, full database access; immediate access to and approval of government details |

The diagram adds one rule your text implies but doesn't state: **Employee ⊂ Supervisor ⊂ (Payroll ∪ HR)**,
with System Admin enclosing everything. That is a sensible shape, and it matches the RBAC table.

---

## 2. The hard blocker: "team" has no definition in the data model

**Everything a Supervisor does depends on a team existing.** Today it does not:

- `demoEmployees` has no `supervisorId`, no `teamId`, no `reportsTo`.
- The Team screen is explicitly labelled *"read-only roster… no reporting lines or team scopes inferred."*
- Firestore paths are `users/{uid}/…`, so **there is no way to read another user's documents** under the
  current rules — not even legitimately.

Consequence: `R (Team) Payslip`, `U (Team) Attendance`, `approve team OT/leave` cannot be implemented
without (a) storing reporting lines and (b) redesigning the path structure away from per-user silos.
That second change is the big one — it moves every collection from `users/{uid}/demoX` to a
company-scoped `companies/{cid}/x` with a role claim in the token.

**Until that lands, every "team" feature is a mock.** That is why approvals do not exist: with a
per-user data silo the only possible approver is the owner, and self-approval is exactly what you
correctly want to forbid.

---

## 3. Role by role: what exists, what's missing

### System Admin

| Capability | Status |
|---|---|
| Immediate access to government details | **Cannot be granted** — no government-details collection exists |
| Full database access | **Cannot be granted** — rules are owner-scoped; "full access" needs an admin claim |
| Role management | **Missing** — roles are not stored anywhere; Firebase Auth knows *who* you are, not *what* you are |

Also worth flagging as a policy question: "immediate access to government details" needs a definition —
SSS/PhilHealth/TIN/Pag-IBIG numbers are sensitive personal data under the Data Privacy Act. Who may read
them, and is every read audited? The current system deliberately stores **none** of these (see
`docs/firebase-data-inventory.md` §4), so nothing is exposed today.

### HR

| Capability | Status |
|---|---|
| Onboard / offboard | **Partial** — create and edit exist; **no delete/deactivate**, because lifecycle policy is undecided. "Offboard" also has payroll meaning (final pay, clearance) that has no rule yet |
| Employee records | **Exists** (create/edit six directory fields + four pay fields) |
| HR tickets | **Partial** — tickets exist with an `HR inquiry` queue label, but nothing routes, assigns or resolves them |
| Everything a Supervisor does | Blocked by §2 |

### Payroll

| Capability | Status |
|---|---|
| Gross-to-net | **Done and verified** — reproduces MotorPH's sample to the centavo and now matches sweldongpinoy on all 34 employees' statutory lines |
| Statutory deductions | **Done** (SSS, PhilHealth, Pag-IBIG, BIR) |
| Voluntary deductions | **Missing** — loans, insurance, benefit contributions have no model; `otherDeductions` is a single opaque amount |
| Bonuses / merit increases | **Missing** — no earning type other than basic, OT, night diff, allowances |
| Disbursement | **Missing** — no payout file, no bank/check data, no "paid" state on a run |
| Remittance | **Partial** — totals are computed and displayed; no filing/remittance record, no proof of payment |
| Payslips | **Done** — printable, exportable, immutable once saved |

### Supervisor

| Capability | Status |
|---|---|
| All employee functions | Blocked by §2 |
| Manage team schedules | **Missing** — schedules are not first-class documents, only an employee→scheduleId integer mapping |
| Approve OT / leave | **Missing** — no approval state at all; leave is `Pending review` → `Cancelled` only |
| Timesheet edits | **Missing** — attendance is read-only by design (there is no write route) |
| Promotion / merit / bonus / disciplinary requests that alter earnings | **Missing** — and this one needs care: an approval that changes pay must leave a trail reaching the payroll run |

### Employee

| Capability | Status |
|---|---|
| **Clock in/out** | **Missing — the single biggest gap.** No capture route; `POST /api/attendance` returns 405; attendance arrives only via the seed |
| View own attendance | **Done** (read-only, raw rows) |
| Salary tracker | **Partial** — payslips exist per saved run; no YTD totals, no 13th-month tracker, no "how much have I been paid this year" |
| Submit tickets (5 types) | **Partial** — 6 types exist with queues; a payroll *dispute* has no link to a payslip and no resolution state |
| View team members | Blocked by §2 |
| Check team leave | Blocked by §2 |

---

## 4. The three things that must be built first, in this order

1. **Tenancy and roles.** Move from `users/{uid}` silos to `companies/{companyId}` with a `role` and
   `companyId` in a custom claim or a `members/{uid}` document. Without this, no team, approval or admin
   feature is expressible.
2. **Reporting lines.** `supervisorId` on the employee record (or a `teams` collection). Cheap, but it is
   the dependency for half the Supervisor column.
3. **Approvals as a first-class object.** One `approvals` collection with `subjectType`
   (`LEAVE`, `OVERTIME`, `TIMESHEET_EDIT`, `EARNING_CHANGE`), `requestedBy`, `decidedBy`, `decision`,
   `decidedAt`. Leave, OT and timesheet edits all become rows in it instead of bespoke fields — and
   "cannot approve your own" becomes one rule: `decidedBy != requestedBy`, enforced in the rules file.

Only after those three does clock-in/out make sense — because a clock-in event needs to know *whose
schedule* it is validating against, and a timesheet edit needs an approver.

---

## 5. Recommended sequencing (with what each unlocks)

| Step | Work | Unlocks |
|---|---|---|
| 1 | `companies/{cid}` path migration + `members` collection + role claim | Roles exist at all |
| 2 | `supervisorId` + a real Team screen | "View team members", "check team leave" |
| 3 | `attendanceEvents` collection + `POST /api/attendance/clock` | **Clock in/out**, then timesheet edits with an approver |
| 4 | `approvals` collection + decision endpoints | Leave approval, OT approval, no-self-approval |
| 5 | Payroll reads approved OT/leave | Approved leave and OT actually change pay |
| 6 | Voluntary deductions + earning types | Payroll Staff's real job |
| 7 | Payout file + remittance records | Disbursement and remittance |

Steps 1–2 are a refactor of storage, not of payroll maths — the engine does not change at all.

---

## 6. One design warning

Your spec has Supervisor "handle timesheet edits" and Payroll "process payroll". If both can edit the
inputs a payslip is computed from, you need to decide **which one wins** and keep an audit trail of
every change. Otherwise two people can produce two different payslips for the same period and neither
is provably the one that was paid.

The current design sidesteps this by making attendance read-only and payroll runs immutable. Keep that
property when you add editing: **never mutate an attendance row in place — append a correction with an
approver and a timestamp**, and let the run snapshot record which corrections it included.
