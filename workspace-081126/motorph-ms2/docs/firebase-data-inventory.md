# What Firebase actually holds

**Generated from the mapping code** (`FirestoreEmployeeStore`, `FirestoreAttendanceStore`,
`FirestoreEmployeeScheduleStore`, `FirestoreWorkflowStore`, `FirestorePayrollStore`, `DemoSeedService`),
not from memory. Every path is relative to one signed-in user.

```
users/{uid}/                       ← uid comes from the verified Firebase ID token, never from a request
```

Nothing exists outside a user's own tree. There is no shared/global collection, no admin collection,
and no cross-user read path anywhere in the code.

---

## 1. The six collections

| Collection | Documents | Written by | Read rule |
|---|---|---|---|
| `demoEmployees` | **34** (MotorPH's employee file) + any you create | Create, seed, edit | owner only |
| `demoEmployeeSchedules` | 4 (employee → schedule id) | Seed only | owner only |
| `demoAttendance` | 28 rows | **Seed only — no capture path exists** | owner only |
| `demoLeaveRequests` | user-created | Submit, cancel | owner only |
| `demoTickets` | user-created | Submit, cancel | owner only |
| `demoPayrollRuns` | user-created | Save run | owner only, **create + read; update and delete denied** |

---

## 2. Field-by-field

### `demoEmployees/{employeeId}` — document ID is the employee ID (`10015`)

| Field | Type | Example | Notes |
|---|---|---|---|
| `id` | string | `10015` | duplicated inside the doc for convenience |
| `name` | string | `Fredrick Romualdez` | ≤ 100 chars, required |
| `department` | string | `Accounting` | required |
| `role` | string | `Account Manager` | required |
| `location` | string | `22A/52 Lubowitz Meadows, Pililla 4895 Zambales` | required; the full address as supplied |
| `status` | string | `Active` | **only** `Active` or `On leave` |
| `monthlyBasic` | string | `"53500.00"` | **optional**; absent = no salary on file |
| `riceSubsidy` | string | `"1500.00"` | optional, written with the other three |
| `phoneAllowance` | string | `"1000.00"` | optional; MotorPH's payslip shows 2,000 for this person, their employee file shows 1,000 — see `docs/where-your-csv-data-goes.md` §4 |
| `clothingAllowance` | string | `"1000.00"` | optional |

Money is a **decimal string**, never a Firestore number: Firestore has no decimal type, and floats
would lose centavos. `monthlyBasic` absent is meaningful (no pay profile) and is different from
`"0.00"` — that is why the payroll picker filters on it.

### `demoEmployeeSchedules/{employeeId}` — document ID is the employee ID

| Field | Type | Example |
|---|---|---|
| `employeeId` | string | `10015` |
| `scheduleId` | integer | `104` |

This is the only link between a person and their timecard rows. No mapping → attendance returns an
empty list and the coverage report marks them **unmapped** (not an error).

### `demoAttendance/{attendanceId}` — document ID is the numeric attendance ID

| Field | Type | Example |
|---|---|---|
| `attendanceId` | integer | `10423` |
| `scheduleId` | integer | `104` |
| `date` | string | `2023-12-01` |
| `timeIn` | string | `08:00` |
| `breakOut` | string | `12:00` | **optional** — absent when no meal punch |
| `breakIn` | string | `13:00` | **optional** |
| `timeOut` | string | `17:00` |
| `hoursWorked` | string | `"8.00"` | stored, not recomputed on read |
| `dayType` | string | `DEMO` | free text; the DOLE premium work would use this |

### `demoLeaveRequests/{LV-…}` — server-generated ID

| Field | Type | Example |
|---|---|---|
| `id` | string | `LV-4C1D9A0B77E2` |
| `employeeId` | string | `10001` |
| `employeeName` | string | denormalised for display |
| `leaveType` | string | `Vacation` / `Medical` / `Personal` / `Unpaid` |
| `startDate` | string | `2026-11-10` |
| `endDate` | string | `2026-11-11` |
| `note` | string | ≤ 240 chars, may be empty |
| `status` | string | `Pending review` → `Cancelled` |
| `submittedAt` | **timestamp** | Firestore `timestampValue` |
| `cancelledAt` | **timestamp** | only written on cancel |

Cancel is a `PATCH` with an update mask limited to `status` and `cancelledAt`, so a cancel can never
rewrite the note or the submission time.

### `demoTickets/{TK-…}`

| Field | Type | Example |
|---|---|---|
| `id`, `employeeId`, `employeeName` | string | |
| `ticketType` | string | `Overtime request`, `Leave request`, `Timesheet correction`, `Payroll inquiry`, `HR inquiry`, `Technical support` |
| `queue` | string | derived server-side: `Team Lead`, `Payroll Staff`, `HR`, `IT Admin` |
| `summary` | string | ≤ 240 chars, required |
| `status` | string | `Open` → `Cancelled` |
| `submittedAt`, `cancelledAt` | timestamp | |

### `demoPayrollRuns/{PR-YYYYMMDD-XXXXXXXX}` — immutable

| Field | Type | Example |
|---|---|---|
| `payPeriod` | string | `SEMI_MONTHLY` |
| `periodStart` / `periodEnd` | string | `2026-01-02` / `2026-01-15` |
| `rulesetId` | string | `PH-STATUTORY-2026` |
| `taxBasis` | string | `BASIC_ONLY` |
| `statutoryBasis` | string | `FULL_MONTHLY_EACH_PERIOD` |
| `createdAt` | string | ISO instant |
| `payslips` | **array** of maps | 33 fields each (see below) |

Each element of `payslips` stores every computed line: identity (4), period (3), `rulesetId`,
salary (4), premiums (5), benefits (6), deductions (5), employer (5), `taxableIncome`, `takeHomePay`.

**Why the whole payslip is stored:** a reprint must never recompute. If 2027's tables change,
last year's payslip still prints what was paid. `FirestorePayrollStore.fromDocument` refuses a run with
no `payslips` rather than returning a blank one.

---

## 3. Security rules (`docs/firestore.rules`)

| Rule | Effect |
|---|---|
| every path under `users/{uid}` | read/write requires `request.auth.uid == uid` |
| `demoPayrollRuns` | `create` and `read` allowed; `update` and `delete` **false** |
| `validDemoPayrollRun()` | validates required fields on write, so a malformed run is rejected at the rule layer |
| employee pay fields | optional, must be numeric strings when present |
| `demoAttendance` | create-only via seed shape; no update path |

Java never holds a service account. It forwards the caller's ID token to Firestore REST, so **the rules
are the real authorisation boundary** — bypassing the Java API with `curl` still hits the same rules.

---

## 4. What Firebase does **not** hold (today)

No users/roles collection (identity is Firebase Auth only), no company/organisation record, no
approvals, no schedules as first-class documents (only the employee→id mapping), no leave balances, no
audit log, no payslip PDFs/binaries, no clock-in events, and no per-employee YTD totals.

Most of those are prerequisites for the role model in `docs/role-model-and-access-matrix.md` — that
document says exactly which collections would have to be added and what each rule would become.
