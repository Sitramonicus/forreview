# Prototype API contract

> Historical contract snapshot: many paragraphs below predate the current server-session and persistent-store implementation and should not be read as a complete current contract. Current storage and external-reference behavior is documented in [storage verification](a_doc/storage-verification.md), [Sweldong comparison](a_doc/sweldong-reference-probe.md), and [current status](a_doc/current-status.md). Verify each endpoint against the running build before relying on older sections.

All routes are same-origin JSON endpoints. In local mode, employee/attendance fixtures are synthetic and workflow records live only in the Java process memory (cleared on server restart). When `FIREBASE_PROJECT_ID` is configured, employee, attendance, and workflow routes require a signed-in Firebase user and use only that user's Firestore sandbox. The Java backend forwards the Firebase ID token to Firestore REST; Firestore Security Rules authorize every request. The project owner has published `docs/firestore.rules`; live smoke tests verified leave and ticket create/cancel persistence and cross-user denial. The assistant has not changed Firebase Console settings. Future rule-file changes still require owner publication.

In Firebase mode, browser requests include:

```text
Authorization: Bearer <Firebase ID token>
X-Firebase-UID: <signed-in Firebase UID>
```

The UID header is not trusted for authorization: Firestore Rules compare it to the UID in the verified ID token. Do not log or persist ID tokens.

## Health

`GET /api/health` returns `status`, `mode` (`local` or `firebase`) and the configured `persistence` (`file`, `memory` or `firestore`). This is a process/configuration check only; it does not contact Firestore or verify a read/write.

System Admin can use `GET /api/admin/storage` for in-process aggregate counts and `POST /api/admin/storage/verify` with `{"confirm":"VERIFY"}` for a serialized read/compare/write/read-back check against the configured state store. This endpoint is not available to employees. See [storage verification](a_doc/storage-verification.md).

## Employee directory

`GET /api/employees`

`GET /api/employees?q=finance`

```json
{
  "items": [
    {
      "id": "DEMO-002",
      "name": "Eli Santos",
      "department": "Finance",
      "role": "Payroll Associate",
      "location": "Taguig",
      "status": "Active"
    }
  ],
  "total": 3,
  "matched": 1
}
```

Search is case-insensitive across displayed fields. `total` is the full user-scoped demo directory size; `matched` is the returned count.

`GET /api/employees/{employeeId}` returns `{"item": {...}}` or `404 {"error":"Employee not found"}`.

`POST /api/employees` creates an owner-sandbox fictional record; `PUT /api/employees/{employeeId}` updates its allowed directory fields. Both accept only `name`, `department`, `role`, `location`, and `status` (`Active` or `On leave`). Unknown fields—including client-supplied IDs, payroll values, and government identifiers—are rejected. The server generates IDs in the `DEMO-<12 uppercase hex characters>` form. Responses are `201` and `200`, respectively, with `{"item": {...}}`. Firebase writes use only the caller's `users/{uid}/demoEmployees` path and existing owner rules; they do not grant HR/supervisor roles. Local mode has no user authentication and stores changes in a single process-memory demo store shared by callers of that API process. Delete, termination, schedule assignment, and audit-history routes are not implemented. A new record has no schedule mapping, so attendance returns an empty list until a separately approved schedule mapping is configured.

## Raw attendance

`GET /api/attendance?employeeId=DEMO-001`

Optional ISO date filters: `from=YYYY-MM-DD` and/or `to=YYYY-MM-DD`. If both are supplied, `from` must be on or before `to`; filtering is inclusive. `employeeId` is required.

```json
{
  "items": [
    {
      "attendanceId": 5001,
      "scheduleId": 101,
      "date": "2026-10-01",
      "timeIn": "08:01",
      "timeOut": "17:00",
      "dayType": "DEMO"
    }
  ],
  "count": 1,
  "hoursCalculated": false
}
```

Possible errors: `400` for missing employee ID, invalid date syntax, or reversed range; `404` for an unknown employee; `401` for missing/expired Firebase sign-in; `403` when Firestore Rules deny the request. Raw times only; no hours or pay are derived.

`POST /api/attendance` is a **local-demo-only create operation**. Firebase attendance remains read-only; Firebase-mode POSTs return `403` before accessing a store. The local endpoint accepts exactly the following fields (no client-selected schedule ID or `dayType`):

```json
{
  "employeeId": "DEMO-001",
  "date": "2026-10-20",
  "timeIn": "08:00",
  "timeOut": "17:00"
}
```

The employee must already exist and have a configured synthetic schedule mapping. The server uses that mapping, sets `dayType` to `DEMO`, and stores the raw row in shared process memory. The store atomically enforces unique `(scheduleID, date)`; `timeIn <= timeOut` is validated. Success is `201` with `{"item":{...},"storage":"local-process-memory","syntheticOnly":true,"hoursCalculated":false}`. Errors: `400` for unknown fields, missing/malformed date or time, or reversed time order; `404` for an unknown employee; `409` for an unmapped employee or duplicate schedule/date; `403` in Firebase mode. No hours, classifications, or pay are derived. The local store is ephemeral and this is not a Firebase write path.

### Raw attendance coverage report

`GET /api/reports/attendance`

Optional inclusive ISO date filters: `from=YYYY-MM-DD` and/or `to=YYYY-MM-DD`. Blank filters include all available records; this is not a pay-period boundary. The response includes one row for each employee in the caller's synthetic directory, using the current configured employee-to-schedule mapping. New employees without a mapping are reported as `hasScheduleMapping:false`, `scheduleId:null`, with zero attributed rows; this does **not** mean absent or unworked.

```json
{
  "items": [
    {
      "employeeId": "DEMO-001",
      "employeeName": "Mara Rivera",
      "scheduleId": 101,
      "hasScheduleMapping": true,
      "rawRecordCount": 3,
      "firstRecordDate": "2026-10-01",
      "lastRecordDate": "2026-10-15"
    }
  ],
  "count": 1,
  "from": null,
  "to": null,
  "hoursCalculated": false
}
```

Counts and date bounds summarize stored raw rows only. No hours, worked-day classification, absence, or pay values are calculated. The endpoint is read-only and uses the same user-scoped employee and attendance paths as the raw attendance query. Possible errors: `400` invalid date syntax or reversed range; `401` missing/expired sign-in; `403` when Firestore Rules deny a read.

## Leave requests and tickets

`GET /api/workflows` returns only the current user's lists:

```json
{"leaveRequests":[],"tickets":[],"storage":"user-scoped-firestore"}
```

- `POST /api/workflows/leave` accepts `employeeId`, `leaveType`, `startDate`, `endDate`, and optional `note`. The server derives the employee name from the caller's own synthetic directory, validates supported leave types and ISO calendar dates, and creates a `Pending review` record.
- `POST /api/workflows/tickets` accepts `employeeId`, `ticketType`, and `summary`. The server derives the employee name and queue label and creates an `Open` record. No ticket is delivered or assigned to a real handler.
- `POST /api/workflows/leave/{requestId}/cancel` and `POST /api/workflows/tickets/{ticketId}/cancel` allow only the pending/open-to-cancelled transition.
- `POST /api/workflows/leave/{requestId}/approve` and `/reject` with body `{"reviewerId":"DEMO-002"}` move a pending request to `Approved`/`Rejected` and record `reviewerId` and `reviewedAt` (the course Leave table's `approverID`/`dateApproved`). The reviewer must exist in the directory and cannot decide their own request; an already-decided or cancelled request returns `409`. **Local mode only:** Firebase mode returns `403` because the published rules have no reviewer role. Approving a limited type is refused (`409`) if it would exceed remaining credits. Delete and resolve routes do not exist.
- `POST /api/workflows/overtime` with body `{"employeeId":"DEMO-002","date":"2026-10-01","minutes":90,"note":""}` creates a `Pending review` overtime request (`OT-…`). `minutes` must be a whole number from 1 to 720; only one pending or approved request per employee per date (`409`). `POST /api/workflows/overtime/{id}/approve` and `/reject` with `{"reviewerId":"DEMO-001"}` decide it; the reviewer cannot be the requester (`400`); decided requests return `409`. `GET /api/workflows` also returns `overtimeRequests`. **Local mode only:** Firebase mode returns `403` and lists none.
- `POST /api/payroll/statutory` with string pesos `period` (2026-MM), `grossCashPay`, `sssCompensation`, `philHealthBasicSalary`, `pagIbigFundSalary`, `taxableCompensation` returns `result` with integer-centavo fields (`sss`, `philHealth`, `pagIbig`, `withholdingTaxCents`, totals, `netPayCents`, employer cost) and `saved: false`. `400` for bad input, a non-2026 month, or deductions above gross. Calculation: `StatutoryCalculator2026.java`, matched to sweldongpinoy.com.
- `GET /api/payroll/payslip?employeeId=DEMO-001&month=2026-10&cutoff=1` returns `payslip` for one cutoff (`1` = 1–15, `2` = 16–end): `payslipId` (`PS-<employee>-<month>-<cutoff>`), `employee` (id, name, department, role), `period`, `daysWithRecords`, `paidMinutes`, `unpaidOverageMinutes`, `overtimeMinutes`, `nightDiffMinutes`, `holidayDays`, `leaveDays`, `earnings` (as in cutoffs), `hourlyRate`, `grossPayCents`, `deductions` (`sssCents`, `philHealthCents`, `pagIbigCents`, `withholdingTaxCents`, `totalCents`; each line is half the month, odd centavo on cutoff 2), `netPayCents`, `rulesetId`, `profileVersion`, `generatedAt`, `temporary: true`, `saved: false`. `409` when the cutoff has no paid attendance or its deductions exceed its gross; `400` for a bad cutoff/month or no temporary rate. Nothing is stored.
- `POST /api/payroll/payslips` with body `{"employeeId":"DEMO-001","month":"2026-10","cutoff":"1"}` builds the payslip from live data (same as `GET /api/payroll/payslip`) and saves it with `saved: true` and `finalizedAt`; returns `201` and `payslip`. No amounts are accepted from the client. `409` if that payslip ID is already finalized, or the cutoff has no paid attendance or a blocked net; `400` for bad input. `GET /api/payroll/payslips` lists `payslips` (newest first: `payslipId`, `employeeId`, `employeeName`, `month`, `cutoff`, `grossPayCents`, `netPayCents`, `finalizedAt`). `GET /api/payroll/payslips/{payslipId}` returns the saved snapshot unchanged (`404` if unknown). **Local mode only** (process memory, cleared on restart); Firebase mode returns `403`.
- `GET /api/payroll/cutoffs?employeeId=DEMO-001&month=2026-10` returns `cutoffs` (`label` `1-15`/`16-end`, `from`, `to`, `daysWithRecords`, `paidMinutes`, `unpaidOverageMinutes` (worked time beyond 8 h a day with no approved overtime; shown, not paid), `overtimeMinutes` (approved and paid), `nightDiffMinutes`, `holidayDays` (unworked regular holidays paid 8 h), `leaveDays` (approved paid leave weekdays paid 8 h), `earnings` (`basicCents`, `premiumCents`, `overtimeCents`, `nightDiffCents`, `holidayPayCents`, `leavePayCents`), `grossPay` as a decimal string = the sum of `earnings`), `monthlyGross`, `hourlyRate` (monthly basic ÷ 168), `monthlyBasic`, `profileVersion`, `temporary: true`, `deductionsApplied: true`, `deductionSplit: "half-per-cutoff"`, and `statutory` (same shape as above, or `null` when gross is 0). Each cutoff also has `deductionsCents`, `netCents` (`null` when `blocked`), and `blocked`. Rules and placeholder monthly basics are in `PayrollProfile.java`. `400` for a bad month or an employee without a monthly basic; `404` for an unknown employee. Read-only in both modes.
- `GET /api/workflows/leave/balances?employeeId=DEMO-001&year=2026` returns `balances` (`leaveType`, `allowance`, `used` = approved, `pending`, `remaining` = allowance − used), plus `unit: "weekdays"`, `policyVersion`, and `temporary: true`. `year` defaults to the current year (2000–2100). Credits are **temporary placeholders** from `LeavePolicy.java`. Creating a Vacation/Medical/Personal request returns `400` when its weekdays exceed remaining minus pending credits.
- Firebase records use `users/{uid}/demoLeaveRequests/{requestId}` and `users/{uid}/demoTickets/{ticketId}`. The candidate Firestore rules allow owner-only read/create/cancel; they deny deletes and all reviewer actions. Records are limited to the user's synthetic employee directory and contain no payroll calculations.
- Local mode returns `storage:"local-process-memory"`; these records survive page reloads while the Java process is running, but are lost when it stops.

Possible errors: `400` invalid request fields or dates; `401` missing/expired sign-in; `403` Firestore rules deny the operation (publish the updated `docs/firestore.rules`); `404` employee/request/ticket not found in the caller's sandbox; `409` invalid workflow transition.

## Opt-in demo seed

`POST /api/demo/seed`

Requires Firebase Authentication and Firestore mode. Creates missing fictional documents only in:

- `users/{uid}/demoEmployees/{employeeId}`
- `users/{uid}/demoAttendance/{attendanceId}`

The endpoint does not overwrite existing records and marks attendance with `dayType: "DEMO"`. It cannot create payroll documents. Firestore Rules are in `docs/firestore.rules`.

## Roles (local demo)

- `GET /api/demo/accounts` lists the demo accounts: `employeeId`, `name`, `position`, `role` (`EMPLOYEE`, `SUPERVISOR`, `PAYROLL`, `HR`, `ADMIN`), `roleLabel`, `supervisorId`, `team`, plus `policyVersion`. In Firebase mode it needs the Firebase sign-in and lists the people in the caller's own sandbox (`401` without it).
- The browser sends **`X-Demo-User: <employeeId>`** on every request after an account is chosen. The server takes the role from its own table (`AccessPolicy.java`), never from the client. An unknown account returns `401`; a forbidden action returns `403` with the reason. In Firebase mode the person must be in the caller's own sandbox (`401` "That person is not in your sandbox."); routes that are local-demo only stay `403` whatever the role.
- Lists are filtered to what the role may see (employees, attendance, leave, overtime, tickets, saved payslips). Single-record reads outside the scope return `403`.
- Approvals are recorded as the signed-in account: `reviewerId` may be omitted, and if sent it must equal the signed-in account (`403` otherwise). Nobody can approve their own request.
- Calls **without** the header (scripts, older tests) are treated as an unrestricted System Admin, and a named `reviewerId` must still be allowed to review by its role. This is a local-demo shortcut, not authentication.
- Firebase mode has no roles yet; it keeps its owner-scoped sandbox behaviour.

## Clock in/out and salary tracker (local demo)

- `GET /api/attendance/clock` returns the signed-in account's time clock: `employeeId`, `today`, `now` (server time, Asia/Manila, to the minute), `clockedIn`, `since`, and `todayRecord` (today's attendance row or `null`).
- `POST /api/attendance/clock` with `{"action":"in"}` opens a shift; `{"action":"out"}` closes it and writes one raw attendance row (returned as `record`). Responds `201` with the same status fields. `409` when already clocked in, not clocked in, or today's row already exists (one per day); `400` without a signed-in demo account or for another action; `403` in Firebase mode. A shift past midnight closes at 23:59 of its start day.
- The salary tracker uses `GET /api/payroll/cutoffs?...&through=YYYY-MM-DD`: `through` counts only days up to that date (attendance, paid leave, unworked holidays); cutoffs keep their real dates. Bad dates are `400`.

## Payroll processing (local demo)

- `GET /api/payroll/adjustments[?employeeId=]` lists `earnings` (one-off taxable: `type` Bonus/Incentive/Adjustment, `month`, `cutoff`, `amountCents`), `deductions` (voluntary: `type` Company loan/Insurance/Benefit contribution/Other voluntary, `amountPerCutoffCents`, `fromMonth`, `toMonth` or empty), and `allowances` (monthly `riceCents`, `phoneCents`, `clothingCents`, `payMethod`). Filtered by the role's Earnings scope (Admin/HR/Payroll all, Supervisor team, Employee none).
- `POST /api/payroll/adjustments/earnings` `{employeeId, month (2026-MM), cutoff "1"|"2", type, description?, amount}` and `POST /api/payroll/adjustments/deductions` `{employeeId, type, description?, amountPerCutoff, fromMonth, toMonth?}` return `201` and `item`. `POST /api/payroll/adjustments/{id}/remove`. Payroll Staff or Admin only, never for their own pay (`403`); an earning for an already-finalized payslip is `409`.
- Cutoffs and payslips now carry `bonusCents`, `allowanceCents`/`allowances`, `statutoryCents`, `voluntaryCents`/`voluntary` (each with `amountCents` taken and `shortfallCents`), `earnedPayCents`, `employerContributions` and `payMethod`. `grossPayCents` = earned + bonuses; `totalCents` = statutory + voluntary; `netPayCents` = gross − deductions + allowances.
- `POST /api/payroll/disbursements` `{month, cutoff}` releases every finalized payslip of that cutoff not yet released as one batch (`201`, `batch` with `lines`: payslip, employee, `method` Direct deposit/Check, `netCents`; `totalCents`). Nothing left to release is `409`. `GET` lists batches (Admin/Payroll/HR). Saved payslips show `released`.
- `GET /api/payroll/remittance?month=2026-MM` (Admin/HR/Payroll) sums finalized payslips per agency: `agencies` (SSS, PhilHealth, Pag-IBIG with employee + employer shares; BIR withholding tax) and per-`employees` rows.

## Supervisor tools (local demo)

- `GET /api/schedules` lists work schedules for people the role may see: `employeeId`, `shiftStart`, `shiftEnd` (+9 h), `mealStart` (+4 h), `workdays` (`MON`…`SUN`), `updatedBy`, `updatedAt`. `PUT /api/schedules/{employeeId}` `{shiftStart "HH:mm" 05:00–15:00, workdays [1–6 days]}` by the person's supervisor or the admin. Pay uses it: grace from the shift start, meal hour 4 h in, non-workdays are rest days.
- `POST /api/attendance/corrections` `{employeeId, attendanceId, timeIn, timeOut, reason}` corrects a direct report's row (supervisor or admin; reason required). Returns `201` with the original and corrected times, `editedBy` and `editedAt`. `404` when the row is not that employee's. `GET /api/attendance/corrections[?employeeId=]` lists the log (Attendance read scope).
- `POST /api/pay-changes` `{employeeId, type, …}` filed by the supervisor (or admin), never for yourself. Types: `PROMOTION` (`newPosition`, `newMonthlyBasic`, `effectiveMonth`), `MERIT` (`newMonthlyBasic`, `effectiveMonth`), `BONUS` (`amount`, `month`, `cutoff`), `DISCIPLINARY` (`startDate`, `endDate`, `note` required). `POST /api/pay-changes/{id}/approve|reject`: HR decides promotion/merit/disciplinary, Payroll Staff decide bonuses, the admin any; never the employee or the requester. Approval applies it: new position and/or salary from the effective month, a taxable bonus earning (409 if that payslip is finalized), or approved Unpaid leave. `GET /api/pay-changes` (Admin/HR/Payroll all, Supervisor team, Employee none). Cutoff `monthlyBasic` and `hourlyRate` follow the salary in force for the month.

## HR (local demo)

- `GET /api/hr/catalog` returns the `departments` and `positions` lists (everyone). `POST /api/hr/catalog` `{kind: department|position, name}` adds one (`409` if it exists); `POST /api/hr/catalog/rename` `{kind, from, to}` renames it and updates every employee using it (`employeesUpdated`). HR or admin.
- `POST /api/hr/onboard` `{name, department, position, location, startDate (2026), supervisorId (a Supervisor), monthlyBasic, riceAllowance, phoneAllowance, clothingAllowance, payMethod Direct deposit|Check}` creates the employee (status Active), puts them on the supervisor's team, gives them a schedule ID for attendance, sets the salary from the start month, and returns `employee` and `record` with the onboarding checklist ("Payroll set up" already done). The new person appears in `/api/demo/accounts` as an Employee. HR or admin.
- `POST /api/hr/employment/{id}/offboard` `{lastDay (2026), reason}` sets status Separated and adds the clearance checklist; returns `finalMonth`/`finalCutoff` (the cutoff with the last day). Pay is counted only from the start date through the last day (attendance, holiday and leave pay). After the last day the account is closed: `/api/demo/accounts` omits it and its `X-Demo-User` gets `401`. Nobody offboards themselves; `409` if already offboarded.
- `POST /api/hr/employment/{id}/tasks` `{task}` ticks a checklist item (`409` if done, `400` unknown). `GET /api/hr/employment` lists records with both checklists. HR or admin.
- `POST /api/workflows/tickets/{id}/resolve` `{reply (2-400 chars)}`: HR resolves tickets in the HR queue, the admin any; never your own; `409` unless Open. Tickets carry `reply`, `resolvedBy`, `resolvedAt`.

## Government details (local demo; fake numbers only)

- Fields: `sss` (10 digits, `00-0000000-0`), `philHealth` (12, `00-000000000-0`), `pagIbig` (MID, 12, `0000-0000-0000`), `tin` (9 or 12, `000-000-000[-000]`). Dashes and spaces are optional on input; blank clears.
- Access (matrix: System Admin full; HR and Payroll "A"; Supervisor and Employee none, `403`): `GET /api/government` lists every employee with **masked** numbers (last 4 digits). `GET /api/government/{id}?reveal=true` returns full numbers to the System Admin, or to HR/Payroll with an approved access request for that person for today; each full view is logged (`403` otherwise).
- `PUT /api/government/{id}` `{sss?, philHealth?, pagIbig?, tin?}`: System Admin edits immediately (logged).
- `POST /api/government/{id}/change-requests` `{fields…, reason?}` and `POST /api/government/{id}/access-requests` `{reason}` (required): HR or Payroll, never about themselves. `GET /api/government/requests` (admin all; others their own; proposed numbers masked except for the admin). `POST /api/government/requests/{id}/approve|reject`: System Admin only, not for their own record. Approving a change applies it; approving access allows full view until the end of that day (server date).
- `GET /api/government/audit`: System Admin; who, whom, action (`EDITED`, `CHANGE_REQUESTED/APPROVED/REJECTED`, `ACCESS_REQUESTED/APPROVED/REJECTED`, `VIEWED_FULL`), detail, time.

## System configuration (System Admin only, `403` otherwise)

- `GET /api/admin/storage` reports configured backend (`file`, `memory`, `firestore`), durability/write readiness, in-process store and aggregate counts, and this process's last successful verification. Counts alone are not a remote read check.
- `POST /api/admin/storage/verify` `{confirm: "VERIFY"}` performs a serialized configured-store read/compare, unchanged-snapshot write, and read-back. A missing/mismatched snapshot or ambiguous read/write result pauses writes; pre-write mismatch is not overwritten. Failure audit is visible only in the current process if the store cannot safely be written. See [storage verification](a_doc/storage-verification.md).
- `POST /api/admin/sweldong/compare` `{confirm: "SEND_TO_SWELDONG", period, employmentType, payPeriod, dailyDays, basicPay, taxableAllowance, nontaxableAllowance, nightDifferentialHours, nightDifferentialRate, overtimeHours, overtimeRate, workSchedule}` sends one explicitly approved, supported manual input to the observed homepage form. Current scope is 2026 Private Employee / Monthly / 20-day / 5-day schedule and zero ND/OT only. It returns a validated raw/normalized comparison report; it never saves payroll. This undocumented external call requires admin session and CSRF, runs outside the state lock, and stores only a normal audit event (not inputs/results).
- `POST /api/admin/sweldong/decision` `{comparisonId, selectedSource: MOTORPH|SWELDONG}` records an explicit admin choice for an in-process validated comparison. Sweldong selection is allowed only when the result is validated/unambiguous and the input is within scope. `appliesToPayroll` is always false; comparison IDs expire on restart.
- `GET /api/admin/roles` lists every person's `role`, `roleLabel`, `supervisorId` and `status`, plus `changes` (newest first: `at`, `by`, `employeeId`, `before`, `after`). `POST /api/admin/roles/{id}` `{role?, supervisorId?}` (`""` clears the supervisor). Guard rails: never your own role (`403`); at least one System Admin stays (`409`); a supervisor with direct reports keeps the role until the team is reassigned (`409`); the supervisor must hold the Supervisor role and reporting lines can't loop (`400`). A change applies at the person's next request/sign-in.
- `GET /api/admin/rates[?month=]` lists rates `versions` (the built-in `PH-2026-MOTORPH-ESTIMATE-v5` first) and `inForce` for the month (default: this month). `POST /api/admin/rates` `{effectiveMonth (2026), note, sss {employeePercent, employerPercent, minMsc, maxMsc}, philHealth {premiumPercent, floor, ceiling}, pagIbig {lowBand, lowPercent, highPercent, employerPercent, cap}, bir [{over, baseTax, ratePercent}…]}` adds `PH-2026-admin-vN`. Percents up to two decimals; pesos as text. Validation: rates 0–50% (tax 0–100%), even PhilHealth split, SSS MSC range in ₱500 steps, tax rows ascending from ₱0; one version per effective month (`409`). Payroll and the statutory preview use the latest version on or before each month, and `rulesetId` names it.

## Payslip corrections (local demo)

- `POST /api/payroll/corrections` `{payslipId, reason (3-200), targetMonth?, targetCutoff?}`: Payroll Staff only, never for their own payslip. The kind follows the payslip: `VOID` when not yet released, `ADJUST` when released (then the target cutoff is required and must be later). One pending correction per payslip (`409`).
- `POST /api/payroll/corrections/{id}/approve|reject`: System Admin only, not for their own payslip; `409` once decided.
  - Approving a **VOID** moves the snapshot to `<payslipId>-void<n>` (with `voided`, `voidedAt`, `voidedBy`, `voidReason`) and frees the cutoff; finalizing again records `version` n+1. A payslip released after the request can no longer be voided (`409`).
  - Approving an **ADJUST** recalculates the cutoff from current data and compares gross with the released snapshot (`differenceCents`). Underpaid adds a taxable `Adjustment` earning on the target cutoff; overpaid adds an `Overpayment recovery` deduction for that one cutoff (`onlyCutoff`). The released payslip stays as it was. `409` if the target cutoff is already finalized.
- `GET /api/payroll/corrections` (Admin/HR/Payroll). Saved payslip lists include voided snapshots (`voided: true`) and each `version`.

## Requirement quick wins (2026-11-06)

- `GET /api/attendance` also returns `metrics` for the range (no range = earliest to latest record): `presentDays`, `absentDays` (scheduled workdays up to today with no row, not a holiday, not on approved leave), `tardyDays`/`tardyMinutes` (time in after the shift start + 10-minute grace), `overtimeMinutes` (approved), `unapprovedExtraMinutes`, `undertimeMinutes` (short of 8 paid hours on workdays), `leaveDays`, `holidayDays`, and `leaveCredits` for `creditYear`. Same schedule and day split as payroll; Firebase mode uses the default Mon-Fri 08:00 schedule.
- `GET/POST /api/attendance/clock` also returns `shiftEnd` (HH:mm); the browser asks before clocking out and again before the shift ends.
- Tickets in `GET /api/workflows` (and a new ticket's `item`) carry `queuePosition` while Open: their place in that queue, oldest first, counting everyone's tickets.
- `POST /api/payroll/adjustments/group` `{group: ALL | TEAM:<supervisorId> | BRANCH:<location> | DEPARTMENT:<name>, kind: earning | deduction, ...the earning or deduction fields}`: Payroll Staff / System Admin. Adds one ordinary item per person and returns `added` and `skipped` (`{employeeId, reason}`: the filer themselves, no pay profile, separated, or finalized payslip). `400` for an empty or unknown group; `409` if nobody could get it.
- `GET /api/payroll/summary?month=2026-MM&by=department|branch` (Payroll/HR/Admin): `groups` (`name`, `employees`, `payslips`, `grossCents`, `deductionsCents`, `allowancesCents`, `netCents`) and `total`, from finalized payslips (voided ones excluded). New payslips record `employee.location`; older ones fall back to the current branch.

## Simulated 2FA, QR check-in and payslip emails (local demo)

- **2FA:** `POST /api/auth/2fa/start` `{employeeId}` returns `challengeId`, `sentTo`, `expiresAt` and `demoCode` (shown because there is no phone). `POST /api/auth/2fa/verify` `{challengeId, code}` returns `session`; a wrong code is `401` with the tries left, and after 3 wrong codes or 5 minutes the request ends. Every request as a demo account must send **`X-Demo-Session`** with `X-Demo-User`, or it gets `401`. `GET /api/auth/session` checks a session; `POST /api/auth/signout` `{}` ends it. Sessions are in memory, so a server restart signs everyone out. Firebase mode uses the Firebase sign-in (`404` here).
- **QR onsite check-in:** `GET/POST /api/attendance/clock` also return `location` (`Offsite`/`Onsite`), `onsiteSince`, `qrCode` and `qrExpiresAt`. Clocking in shows a QR code valid for 10 minutes. `{action: "onsite", code}` is the branch scan (`400` wrong code, `409` expired/already onsite), and `{action: "qr"}` shows a new code. Onsite scans are kept in the data file (`onsiteCheckIns`).
- **Payslip emails:** `POST /api/payroll/disbursements` also returns `emailsSent`/`emailsFailed`; each released payslip is emailed to the synthetic work address (`firstname.lastname@motorph.example`). `GET /api/payroll/outbox` lists `messages` (employees their own, supervisors their team, Payroll/HR/Admin all) and `outage`. Payroll Staff / System Admin: `POST /api/payroll/outbox/outage` `{on}` simulates a mail outage, `POST /api/payroll/outbox/{EM-0001}/resend` `{}`, and `POST /api/payroll/outbox/retry-failed` `{}` (returns `nowSent`). Nothing is really sent.

## Security, payroll policy, audit log, backups and branches (local demo)

- **Sign-in and passwords:** `POST /api/auth/signin` `{employeeId, password}` begins sign-in; `POST /api/auth/2fa/verify` `{challengeId, code}` opens the workspace and returns `defaultPassword`. All accounts start with the shared password listed in SETUP.md; first sign-in does not require changing it. `POST /api/auth/password` `{current, next}` changes your own password voluntarily and signs out other sessions.
- **Account access (System Administrator / IT):** `GET /api/admin/security` lists each account's `enabled`, `defaultPassword`, lock status and fixed server limits. `POST /api/admin/security/{id}/activate|deactivate` controls sign-in; deactivation revokes that account's sessions and pending code challenges. At least one active System Administrator is kept. `POST .../{id}/unlock` and `/{id}/reset-password` unlock or restore the shared initial password; reset signs out the target account. `POST /api/admin/security` refuses unsupported server-setting changes.
- **Payroll policy (System Admin):** `GET/POST /api/admin/payroll-policy` `{graceMinutes 0-30, hoursPerMonth 160-208, dailyHours 4-10, nightDifferentialPercent 10-30}`; `mentorDefaults` says whether it is 10 / 168 / 8 / 10. Applies to unfinalized pay; each payslip records `payrollPolicy`.
- **Audit log (System Admin):** `GET /api/admin/audit?actor=&path=&failures=true&limit=` returns `entries` (`at`, `actor`, `method`, `path`, `target`, `status`; newest first, never request bodies), `kept` and `archives`. Every change request and sign-in attempt is recorded; past 1,000 entries the oldest 500 go to `data/archive/audit-*.json`.
- **Backups (System Admin):** `GET /api/admin/backups`; `POST /api/admin/backups` `{}` backs up now; `POST /api/admin/backups/restore` `{name, confirm: "RESTORE"}` restores (after a `before-restore` safety backup). Automatic backups at most every 30 minutes of changes; the newest 20 are kept; a damaged data file is replaced by the newest readable backup at start.
- **Branches:** `GET /api/branches` returns `mine` (the signed-in person's location), `today` and `branches` (`name`, `headcount`, `active`, `onLeaveToday`, `clockedInNow`, `onsiteToday`; counts only). Employees and supervisors get their own branch; Payroll/HR/Admin get every branch. `/api/demo/accounts` items include `branch`.

## Attendance tags, roster today, notifications (local demo)

- `GET/POST /api/attendance/clock` also return `dayType` (`Workday`, `Rest day`, a holiday kind such as `Regular holiday`, or `On approved leave (Type)`), `attendanceTag` (`Present`/`Tardy` from the time in against the shift start + grace; empty before clock-in) and, once today's row exists, `workHourTag` (`Regular`, `Overtime` = approved overtime worked, `Undertime` = short of the paid hours on a workday).
- `GET /api/team/today` lists `{employeeId, status}` for everyone whose directory record you may read: `Present`/`Tardy` with the times (and onsite/offsite while clocked in), `On leave`, the day type on a rest day or holiday, `Absent` once the grace period has passed with no clock-in, or `Not in yet`.
- `GET /api/notifications` (your own, newest first, with `unread`) and `POST /api/notifications/read` `{}`. Created for the person affected, never for your own action: schedule changes, timesheet corrections, leave, overtime and pay-change decisions, resolved tickets, and released pay.

## Remaining System Design items (local demo)

- **Sign-in:** a successful 2FA verify ends that account's older sessions. After the 2nd wrong password in a row the next try must wait 1 s, then 2 s, 4 s ... (`429` with the wait; a refused try does not count); 5 wrong passwords still lock the account.
- **Landing page:** every role lands on Overview (the branch page) after sign-in.
- **Supervisors' team earnings:** `POST /api/payroll/adjustments/earnings`, `/deductions` and `/{id}/remove` also accept the employee's supervisor for their direct reports (never their own pay). Group items stay with Payroll Staff / System Admin.
- **Address and birthdate:** `GET /api/personal/{id}` (HR/Admin anyone; others only themselves) and `PUT /api/personal/{id}` `{address 5-200 chars, birthdate making them 15-100}` (HR/Admin). `POST /api/hr/onboard` also accepts `birthdate` and `address`.
- **Single-day / multi-day schedules:** `POST /api/schedules/{id}/days` `{from, to?, shiftStart | dayOff: true, reason}` (supervisor of a direct report or System Admin; up to 31 days in 2026; the newest change wins); `POST /api/schedules/days/{SC-0001}/remove` `{}`. `GET /api/schedules` items include `dayChanges`. Pay, attendance counts, tags and the roster use the schedule for each date.
- **Rate changes for approval:** `POST /api/rate-proposals` (Payroll Staff; same body as `/api/admin/rates`) files `RP-0001` as `Pending review`; `POST /api/rate-proposals/{id}/approve|reject` (System Admin, not the proposer) turns it into a rates version or rejects it. `GET /api/rate-proposals` lists proposals, `versions` and `inForce` (Payroll Staff and System Admin).
- **Allowance corrections:** earning types `Rice subsidy adjustment`, `Phone allowance adjustment`, `Clothing allowance adjustment` are non-taxable: paid on top of net, outside gross, contributions and tax, listed in the payslip's `allowances.corrections`.
- **Distribution:** `GET /api/payroll/payslips/{id}/pdf` returns the finalized payslip as a one-page PDF (same access as the payslip). Each payslip email has `attachment` (`PS-….pdf`); a release also sends a collated report to `payroll@motorph.example` (visible to Payroll/HR/Admin). Disbursement batches carry `status`: `Distributed` once every payslip email is sent, else `Released, N email(s) failed`.

## Remaining document requirements (local demo)

- **Tickets:** new types `Reschedule request` (Team Lead) and `General inquiry` (HR). Each queue has its handler: Team Lead = the owner's supervisor, Payroll Staff, HR, IT Admin = the System Admin (who can step in anywhere); handlers see their queue's tickets in `GET /api/workflows`. `POST /api/workflows/tickets/{id}/start` `{}` marks a ticket `In progress` (`resolvedBy` = the handler); `/resolve` works from Open or In progress; an in-progress ticket can no longer be cancelled (`409`). Firestore rules (`docs/firestore.rules`) list the new types; publish them for Firebase mode.
- **Adjustment forms:** `POST /api/payroll/adjustment-forms` `{scope: EMPLOYEE:<id> | TEAM:<supervisorId> | BRANCH:<name> | DEPARTMENT:<name> | ALL, month, cutoff, reason, lines: [{component, operation, amount}]}` (Payroll Staff / System Admin) -> `Pending review`. Components: `Rice subsidy`, `Phone allowance`, `Clothing allowance` (Add / Subtract / Replace), `Taxable earning` (Add / Subtract), `Deduction` (Add). `POST .../{AF-0001}/approve|reject` by another Payroll Staff member or the System Admin (not the submitter) applies it and stores `results` (per employee: gross, allowances, deductions and net before and after, or `skipped` with the reason). `GET` lists them (Payroll/HR/Admin).
- **Payroll report files:** `GET /api/payroll/summary?month=&by=&format=csv|pdf` downloads the summary: one line per finalized payslip, subtotals per group and a total (CSV with a UTF-8 byte-order mark; formula-looking cells are neutralised; PDF on as many pages as needed).
- **Change history:** `GET /api/changelog?employeeId=` (HR / System Admin anyone, others only themselves): `entries` with `at`, `employeeId`, `field`, `before`, `after`, `by`, `via` for directory edits, personal details, offboarding, approved promotions and merit increases, and role/supervisor changes.
- **Personal details:** `PUT /api/personal/{id}` now takes `{building?, street, city, region, postalCode (4 digits), contactNumber (09XXXXXXXXX or +639XXXXXXXXX), birthdate}`; `GET` returns those plus the one-line `address`. Onboarding accepts the same fields.
- **Other transactions:** non-taxable earning types `Reimbursement` and `Cash advance` (paid on top of net, outside gross and tax) and the deduction type `Cash advance repayment`.

## Employee record extras (local demo)

- `PUT /api/personal/{id}` also takes `firstName`, `surname` (required; letters, spaces, `. ' -`, up to 60 each), `gender` (`Female`, `Male`, `Prefer not to say`) and an optional personal `email`. Saving the names updates the directory name ("First Surname"). `GET` returns them (older records: the directory name split at the last space) and `hasPhoto`. Onboarding accepts the same fields.
- **Photo:** `PUT /api/personal/{id}/photo` `{photo: data URL}` (HR / System Admin; PNG or JPEG checked by its bytes, at most 150 KB), `GET /api/personal/{id}/photo` returns the image to anyone who may read that employee's directory record, `POST /api/personal/{id}/photo/remove` `{}`.
- **Delete:** `POST /api/employees/{id}/delete` `{confirm: "DELETE"}` (System Admin, not your own record). Refused with `409` when the employee has attendance, finalized payslips, pay items, leave/overtime/tickets, or people reporting to them; offboard them instead. The change history keeps a "Record deleted" entry.

## Local data file

- The local demo keeps its state in `data/local-demo.json` (override with `LOCAL_DATA_FILE`; empty = memory only). It is loaded at start-up and written after every successful change (non-GET request, or a logged full view of government numbers), through a temporary file that replaces the old one. Changes are applied one at a time while saving; stopping the server waits for a save in progress. An unreadable file is moved to `local-demo.json.unreadable-<time>` and the demo starts from the built-in data. Firebase mode never uses the file.
- `GET /api/health` reports `persistence`: `file` or `memory`.
- `POST /api/admin/reset` `{"confirm":"RESET"}` (System Admin) restores the built-in demo data (five accounts, sample attendance, default roles and rates) and saves it.

## Scope and security

- No production employee or payroll records.
- No service account, Admin SDK, or privileged credentials.
- Current user-owned sandbox rules are development-only, not an organizational role model.
- Payroll formulas, pay-period logic, and calculations remain blocked until specified by supplied course materials.
