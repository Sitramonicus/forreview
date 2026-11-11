# Prototype API contract

All routes are same-origin JSON endpoints. The production entry point requires `FIREBASE_PROJECT_ID`; employee, attendance, and workflow routes require a signed-in Firebase user and use only that user's Firestore sandbox. The Java backend forwards the Firebase ID token to Firestore REST; Firestore Security Rules authorize requests. The loopback-only process-memory server is an automated test fixture, not a supported runtime. `POST /api/attendance` is not implemented and returns `405`; attendance is read-only until a persistent schedule/identity design and security rules are implemented. The project owner previously published `docs/firestore.rules`; live smoke tests verified leave and ticket create/cancel persistence and cross-user denial. The assistant has not changed Firebase Console settings. Future rule-file changes still require owner publication.

In Firebase mode, browser requests include:

```text
Authorization: Bearer <Firebase ID token>
X-Firebase-UID: <signed-in Firebase UID>
```

The UID header is not trusted for authorization: Firestore Rules compare it to the UID in the verified ID token. Do not log or persist ID tokens.

## Health

`GET /api/health`

Firebase-backed production mode returns `{"status":"ok","mode":"firebase-user-sandbox"}`. The loopback test fixture reports `local-synthetic-demo`; it is not a production mode.

## Employee directory

`GET /api/employees`

`GET /api/employees?q=finance`

```json
{
  "items": [
    {
      "id": "10002",
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

`POST /api/employees` creates a record in the caller's own sandbox; `PUT /api/employees/{employeeId}`
updates it. Both accept `name`, `department`, `role`, `location`, `status` (`Active` or `On leave`)
and, optionally, the pay inputs payroll reads: `monthlyBasic`, `riceSubsidy`, `phoneAllowance`,
`clothingAllowance`. Omitted pay fields keep their stored values, so an edit cannot zero a salary by
accident. Unknown fields — client-supplied IDs, government identifiers, computed amounts — are
rejected. **Employee numbers are issued by the server**: the next free five-digit number after the
highest in the directory (10035, 10036, …), matching MotorPH's own numbering. IDs must match
`[0-9]{5}`. Responses are `201` and `200` with `{"item": {...}}`. Firebase writes use only the
caller's `users/{uid}/demoEmployees` path and existing owner rules; they grant no HR, supervisor, or
payroll authority. The isolated test fixture has no user authentication and stores changes in
process memory; the production entry point refuses to start in that mode. Delete, termination,
schedule-assignment editing, effective-dated salary history, and audit-history routes are not
implemented. A new employee has no schedule mapping; mappings are read from persistent
owner-scoped `demoEmployeeSchedules` documents. Note the consequence of editing pay: there is no
salary history, so a changed rate applies to every later recomputation, while saved payroll runs
keep the figures they were saved with.

## Raw attendance

`GET /api/attendance?employeeId=10001`

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

`POST /api/attendance` returns `405 Method not allowed`. No attendance writer is implemented in any runtime. The former process-memory attendance route/form and mutable-store interface were removed. A future write path requires approved capture semantics, durable schedule/employee assignments, atomic uniqueness, and authorization tests.

Attendance attribution now reads `users/{uid}/demoEmployeeSchedules/{employeeId}`. The opt-in synthetic seed creates only the pre-existing fictional fixture links; these opaque IDs are not shift definitions or approved schedules. Firestore rules permit owner reads and validated creates, and deny mapping update/delete. Publish the updated `docs/firestore.rules` before using this seed/mapping path in Firebase. Existing sandbox records without a persisted mapping are left unlinked; the API does not fall back to a hard-coded mapping.

### Raw attendance coverage report

`GET /api/reports/attendance`

Optional inclusive ISO date filters: `from=YYYY-MM-DD` and/or `to=YYYY-MM-DD`. Blank filters include all available records; this is not a pay-period boundary. The response includes one row for each employee in the caller's synthetic directory, using the caller-scoped persisted `demoEmployeeSchedules` mapping. New employees without a mapping are reported as `hasScheduleMapping:false`, `scheduleId:null`, with zero attributed rows; this does **not** mean absent or unworked.

```json
{
  "items": [
    {
      "employeeId": "10001",
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
- `POST /api/workflows/leave/{requestId}/cancel` and `POST /api/workflows/tickets/{ticketId}/cancel` allow only the pending/open-to-cancelled transition. Delete, review, approve, reject, and resolve routes do not exist.
- Firebase records use `users/{uid}/demoLeaveRequests/{requestId}` and `users/{uid}/demoTickets/{ticketId}`. The candidate Firestore rules allow owner-only read/create/cancel; they deny deletes and all reviewer actions. Records are limited to the user's synthetic employee directory and contain no payroll calculations.
- The isolated test fixture returns `storage:"local-process-memory"` only inside automated tests. The production runtime requires Firebase and never selects this store.

Possible errors: `400` invalid request fields or dates; `401` missing/expired sign-in; `403` Firestore rules deny the operation (publish the updated `docs/firestore.rules`); `404` employee/request/ticket not found in the caller's sandbox; `409` invalid workflow transition.

## Opt-in demo seed

`POST /api/demo/seed`

Requires Firebase Authentication and Firestore mode. Creates missing fictional documents only in:

- `users/{uid}/demoEmployees/{employeeId}`
- `users/{uid}/demoEmployeeSchedules/{employeeId}`
- `users/{uid}/demoAttendance/{attendanceId}`

The response includes `employeesCreated`, `scheduleMappingsCreated`, and `attendanceCreated`. The endpoint preflights the mapping collection permission before writing, does not overwrite existing records, and marks seeded attendance with `dayType: "DEMO"`. It cannot create payroll documents. Firestore Rules are in `docs/firestore.rules`.

## Identity and access control

Every request is resolved to an **actor** before the route runs, in three steps:

1. **Account** — a Firebase ID token, verified against Google's `accounts:lookup` (signature, expiry,
   project). An unverifiable token is `401`; a verification outage is `503`, never a bypass.
2. **Person** — which linked employee to act as, from `X-MotorPH-Employee`. The account must actually
   be linked to that person, else `403`.
3. **Seat** — which role of that person to act under, from `X-MotorPH-Seat`. The person must actually
   hold that seat, else `403`.

The **role is never taken from the request.** One account may hold several people and one person may
hold several seats, which is why a single login can exercise every part of the matrix. An account HR
has not linked is `APPLICANT`, which holds no permissions at all.

`FIREBASE_API_KEY` is required in Firebase mode; without it the process refuses to start, because an
unverified token is the same as no authentication.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/me` | Who the caller is: employee number, role, position, supervisor, direct reports, the resolved 12×5 permission matrix, and the `seats` and `people` this session may switch between |
| POST | `/api/me/links` | HR links an account to a person: `{"accountId","employeeNumber"}`. Calling it again with a second number is how one login covers two jobs. HR/System Admin only; nobody may edit their own links |
| GET | `/api/fixtures/actors` | **Fixture mode only.** The seeded roster, so a local screen can offer a choice of employee number. Returns 404 whenever Firebase is configured |

Refusals are real HTTP statuses, not empty results:

- `401` — no valid session.
- `403` — signed in, but the matrix does not allow this operation on this record. The body says which
  rule refused, e.g. `"That employee is not one of your direct reports."`
- Unlinked accounts (`APPLICANT`) hold no permissions at all: their directory listing is empty **and**
  reports `total: 0`, so a caller who may see nobody does not learn how large the roster is.

Directory listings are **filtered, not blocked**, so a response can never contain a row the caller may
not read: an employee sees only themselves, a supervisor sees themselves plus their direct reports
(from the `Immediate Supervisor` column of the supplied file), HR and Payroll see the whole roster.

## Payroll periods

A payroll period is one set of dates for everyone. A run that names a period must carry that period's
dates and cadence, and a closed period accepts no further runs.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/payroll/periods` | Open and closed cycles |
| POST | `/api/payroll/periods` | Open a cycle: `{"cadence":"MONTHLY"\|"SEMI_MONTHLY","periodStart","periodEnd"}` → `201`, `409` if it already exists |
| GET | `/api/payroll/periods/{id}` | One cycle |
| POST | `/api/payroll/periods/{id}/close` | Close it; later runs are refused with `409` |

Cadence is validated, not trusted: `MONTHLY` must run from the 1st to the last day of one month;
`SEMI_MONTHLY` must be the 1st–15th or the 16th–last day. A typed `30` for February, a ten-day range,
or a range spanning two months is refused with `400` and a reason.

## Salary history

Rates are dated rows, not a field on the employee record, so a raise cannot rewrite a payslip that has
already been paid.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/payroll/salary-history?employeeId=` | Dated rates. Payroll and HR see everyone; anybody else sees only their own |
| POST | `/api/payroll/salary-history` | Record a rate. Requires `PAYROLL` update, so HR and employees are refused (`403`) |

```json
{
  "employeeId": "10015",
  "effectiveFrom": "2026-07-01",
  "monthlyBasic": "60000",
  "riceSubsidy": "1500",
  "phoneAllowance": "1000",
  "clothingAllowance": "1000",
  "reason": "Promotion to Senior Account Manager"
}
```

- `effectiveFrom` must be the **first day of a month**: pay is computed per cycle, and a cycle cannot
  be paid at two rates. A mid-month date is refused with `400` and that explanation.
- Two rates for the same person and date are refused (`400`).
- Opening rates are seeded from the supplied employee file, effective 2025-01-01, one row per person.
- A run for a **declared cycle** must reconcile with the dated rate: submitting an amount that
  disagrees with the rate in force is refused (`400`) and names both figures. A bare
  `/api/payroll/preview` that names no cycle stays a calculator and uses the figures it is given.

## Scope and security

- No production employee or payroll records; the data is the supplied MotorPH files.
- No service account, Admin SDK, or privileged credentials.
- Firestore Rules are in `docs/firestore.rules`.
- The role seed for the 34 employees is a **demonstration assignment** based on their real positions,
  flagged as such in `/api/me`; it is not a personnel policy.

## Payroll

Payroll is enabled and effective-dated. Every response that carries money returns decimal strings;
the client never does arithmetic on them.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/payroll/rules?year=2026` | Ruleset id, effective window, rates, MSC bounds, both Annex E tables |
| GET | `/api/payroll/timesheet?employeeId=&from=&to=` | Stored timecard summary: `daysWorked`, `totalHours`, per-day rows |
| POST | `/api/payroll/preview` | Compute one payslip; `saved:false` |
| POST | `/api/payroll/runs` | Compute and persist an immutable run snapshot; `201` |
| GET | `/api/payroll/runs` | Run history with totals (no payslip bodies) |
| GET | `/api/payroll/runs/{runId}` | One saved run including every payslip line |

Both run endpoints are access-controlled: Payroll sees every run, everybody else sees only runs that
contain a payslip they may read, and a caller with no payslip access at all is refused with `403`.

`/api/payroll/preview` and `/api/payroll/runs` accept:

```json
{
  "employeeId": "10015",
  "employeeName": "Fredrick Romualdez",
  "position": "Account Manager",
  "department": "Accounting",
  "payPeriod": "SEMI_MONTHLY",
  "periodStart": "2026-01-02",
  "periodEnd": "2026-01-15",
  "monthlyBasic": "53500",
  "daysWorked": "10",
  "overtimeHours": "0",
  "overtimeRatePercent": "125",
  "nightDifferentialHours": "0",
  "nightDifferentialRatePercent": "10",
  "riceSubsidy": "1500",
  "phoneAllowance": "2000",
  "clothingAllowance": "1000",
  "taxableAllowance": "0",
  "otherDeductions": "0",
  "taxBasis": "BASIC_ONLY",
  "statutoryBasis": "FULL_MONTHLY_EACH_PERIOD"
}
```

Unknown fields are rejected (`400`). Rejections also cover: negative pay or a negative take-home,
days worked above 20 (monthly) or 10 (semi-monthly), reversed or malformed periods, an unknown pay
period, and a `periodStart` year without a loaded ruleset. The two assumption fields are echoed back
on every saved run and are the only two MotorPH conventions the supplied payslip could not settle by
itself; see `docs/payroll-ruleset-and-golden-case.md`.

Employee create/update now also accepts optional `monthlyBasic`, `riceSubsidy`, `phoneAllowance`,
`clothingAllowance` (decimal strings). Omitting them leaves stored values untouched.

Every money field is a decimal string rounded to centavos; hours and day counts are decimal
strings; `employeeCount` and `count` are integers.

`GET /api/payroll/rules` returns `id` (`PH-STATUTORY-2026`), `year`, `effectiveFrom`,
`effectiveTo`, the SSS employee/employer rates, MSC floor/ceiling/step, EC amounts and threshold,
MPF threshold, PhilHealth rate/floor/ceiling, Pag-IBIG rate/floor/cap, the divisor set, and both
Annex E withholding tables. Only 2025 and 2026 are loaded; any other year is a `400`, never a guess.

`GET /api/payroll/timesheet` returns `employeeId`, `from`, `to`, `daysWorked`, `totalHours`, and a
`days` array of `{date, dayType, hours}`. It reads stored attendance and never writes it. Both
`from` and `to` are required.

`POST /api/payroll/preview` returns `{payslip, saved:false, ruleset}`. `POST /api/payroll/runs`
returns `201 {run, saved:true}`. The `run` object is the summary:

```json
{
  "id": "PR-20260102-77BC9231",
  "payPeriod": "SEMI_MONTHLY",
  "periodStart": "2026-01-02",
  "periodEnd": "2026-01-15",
  "rulesetId": "PH-STATUTORY-2026",
  "taxBasis": "BASIC_ONLY",
  "statutoryBasis": "FULL_MONTHLY_EACH_PERIOD",
  "createdAt": "2026-11-05T10:16:01.660166Z",
  "employeeCount": 1,
  "totalGross": "26750.00",
  "totalBenefits": "4500.00",
  "totalEarnings": "31250.00",
  "totalDeductions": "8538.33",
  "totalNet": "22711.67",
  "totalEmployerCost": "36317.50",
  "payslips": []
}
```

`totalGross` is payroll gross only, `totalBenefits` is the allowance total, and `totalEarnings`
is their sum — the figure a payslip prints as total earnings. `payslips` is present only on
`POST /api/payroll/runs` and `GET /api/payroll/runs/{id}`; the history list stays a summary.
A payslip carries `employeeId`, `employeeName`, `position`, `department`, `payPeriod`,
`periodStart`, `periodEnd`, `rulesetId`, `monthlyBasic`, `dailyRate`, `daysWorked`, `hourlyRate`,
`overtimeHours`, `overtimePay`, `nightDifferentialHours`, `nightDifferentialPay`, `grossIncome`,
`riceSubsidy`, `phoneAllowance`, `clothingAllowance`, `taxableAllowance`, `nonTaxableAllowance`,
`totalBenefits`, `sssEmployee`, `philHealthEmployee`, `pagibigEmployee`, `withholdingTax`,
`totalDeductions`, `sssEmployer`, `sssEc`, `philHealthEmployer`, `pagibigEmployer`,
`employerCost`, `taxableIncome`, and `takeHomePay`.

Saved runs are immutable: `DELETE /api/payroll/runs/{id}` is `405`, and the Firestore rules allow
only create and read on `demoPayrollRuns`. A run stores its ruleset id and both assumption fields,
so a reprint never depends on today's tables.

### `POST /api/payroll/compare` — optional reference check

Computes the payslip exactly as `/preview` does, then **additionally** asks sweldongpinoy.com for its
own figures. Returns both side by side.

```json
{
  "reference": "sweldongpinoy.com",
  "referenceNote": "Undocumented third-party calculator, used for comparison only. Never used to compute or save a payslip.",
  "ours":   { "grossPay":"58000.00","sss":"1750.00","philHealth":"1337.50","pagibig":"200.00",
              "withholdingTax":"5250.83","totalDeductions":"8538.33","netPay":"49461.67" },
  "available": true,
  "theirs": { "…same shape…" },
  "differences": [ {"field":"sss","ours":"1750.00","theirs":"1750.00","delta":"0.00"} ],
  "matches": true
}
```

When the reference is unreachable the route still answers `200` with:

```json
{ "ours": {…}, "available": false, "reason": "Reference calculator timed out.",
  "theirs": null, "differences": [] }
```

The same shape is returned when the two figures would not be **like for like**, because their
calculator only ever prices one whole month:

| Request | `available` | `reason` |
|---|---|---|
| Monthly, days worked = 20 | `true` | — two full-month figures compared |
| Monthly, days worked < 20 | `false` | "Our figure pays 10 day(s) of a 20-day month; the reference always pays a full month…" |
| Semi-monthly, any days | `false` | "sweldongpinoy.com only prices a whole month. Set the pay period to Monthly…" |

This is a deliberate refusal to print a delta that measures the *basis* rather than the arithmetic.
Their semi-monthly branch doubles the basic pay and allowances it is given, so a semi-monthly request
sent as a "cutoff" comes back as a whole month. The UI labels the three cases differently:
`MATCHES` / `N DIFFERENCE(S)`, `NOT COMPARABLE`, and `UNAVAILABLE`.

Rules that make this safe:

- It never changes the payslip. `ours` is produced by our ruleset before the reference is contacted.
- A timeout, an HTTP error or a malformed answer is reported, never thrown — payroll cannot break because
  a third-party site is down.
- It runs **server-side** (`SweldongPinoyClient`), so salary data is never posted from the browser, and
  CORS/the session handshake stay out of the client.
- `./test.sh` pins `MOTORPH_SWP_ENDPOINT` to a closed port, so the build never touches the internet.
