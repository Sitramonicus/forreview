# Prototype API contract

All routes are same-origin JSON endpoints. In local mode, employee/attendance fixtures are synthetic and workflow records live only in the Java process memory (cleared on server restart). When `FIREBASE_PROJECT_ID` is configured, employee, attendance, and workflow routes require a signed-in Firebase user and use only that user's Firestore sandbox. The Java backend forwards the Firebase ID token to Firestore REST; Firestore Security Rules authorize every request. The project owner has published `docs/firestore.rules`; live smoke tests verified leave and ticket create/cancel persistence and cross-user denial. The assistant has not changed Firebase Console settings. Future rule-file changes still require owner publication.

In Firebase mode, browser requests include:

```text
Authorization: Bearer <Firebase ID token>
X-Firebase-UID: <signed-in Firebase UID>
```

The UID header is not trusted for authorization: Firestore Rules compare it to the UID in the verified ID token. Do not log or persist ID tokens.

## Health

`GET /api/health`

Local mode:

```json
{"status":"ok","mode":"local-synthetic-demo"}
```

Firebase mode reports `firebase-user-sandbox`.

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
- `POST /api/workflows/leave/{requestId}/cancel` and `POST /api/workflows/tickets/{ticketId}/cancel` allow only the pending/open-to-cancelled transition. Delete, review, approve, reject, and resolve routes do not exist.
- Firebase records use `users/{uid}/demoLeaveRequests/{requestId}` and `users/{uid}/demoTickets/{ticketId}`. The candidate Firestore rules allow owner-only read/create/cancel; they deny deletes and all reviewer actions. Records are limited to the user's synthetic employee directory and contain no payroll calculations.
- Local mode returns `storage:"local-process-memory"`; these records survive page reloads while the Java process is running, but are lost when it stops.

Possible errors: `400` invalid request fields or dates; `401` missing/expired sign-in; `403` Firestore rules deny the operation (publish the updated `docs/firestore.rules`); `404` employee/request/ticket not found in the caller's sandbox; `409` invalid workflow transition.

## Opt-in demo seed

`POST /api/demo/seed`

Requires Firebase Authentication and Firestore mode. Creates missing fictional documents only in:

- `users/{uid}/demoEmployees/{employeeId}`
- `users/{uid}/demoAttendance/{attendanceId}`

The endpoint does not overwrite existing records and marks attendance with `dayType: "DEMO"`. It cannot create payroll documents. Firestore Rules are in `docs/firestore.rules`.

## Scope and security

- No production employee or payroll records.
- No service account, Admin SDK, or privileged credentials.
- Current user-owned sandbox rules are development-only, not an organizational role model.
- Payroll formulas, pay-period logic, and calculations remain blocked until specified by supplied course materials.
