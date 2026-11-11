# Local demo inventory

This note separates unauthenticated process-memory behavior from Firebase-backed owner-sandbox behavior. It is not an acceptance rubric or a production-readiness claim.

## Local process-memory mode

- Selected when the API runs without a Firebase project ID. The browser shows `LOCAL PREVIEW`; there is no user sign-in boundary. Directory/workflow/raw records are shared by callers of that local API process and reset when the process stops.
- Synthetic employee create/update is limited to the allowed display fields. IDs are server-generated. New employees receive no schedule mapping.
- Raw attendance queries are read-only as a view; the explicitly authorized **create-only** demo form is the exception. It accepts an existing mapped synthetic employee, date, time-in, and time-out. The server supplies the existing schedule ID and `DEMO` label; the store enforces unique schedule/date and `timeIn <= timeOut`. No correction, delete, schedule assignment, attendance classification, hours, or pay is added.
- Leave/ticket workflows are synthetic, process-memory records with their existing submit/cancel limits; no reviewer, approval, resolution, or delivery action is present.
- The temporary statutory calculator takes fictional browser inputs and is not persisted or connected to attendance, employees, or a payroll API.
- The read-only raw attendance calendar (including its two-date inclusive filter), employee linked-record detail and searchable/mapped-filterable/sortable stored count summary with visible/all CSV, cross-module search, workflow type filters, visible/all CSV exports, and JSON workspace export use the same local demo service. The JSON export includes only the current synthetic records and marks `hoursCalculated:false`, `payrollIncluded:false`; search/export issue reads only.

## Firebase user-sandbox mode

- The app requires Firebase Email/Password sign-in when client configuration is complete; API reads/writes use the caller's user-scoped paths and existing published rules.
- Employee display fields and owner-scoped workflow routes retain their previously accepted limits. The employee-linked record view, raw attendance calendar, cross-module search, and JSON bundle use user-scoped reads.
- **Firebase attendance stays read-only.** `POST /api/attendance` is rejected with `403` before a storage service is accessed. No Firebase attendance write has been introduced.
- No user roles, employee-account mapping, supervisor/team boundaries, approvals, schedule assignment, attendance calculations, payroll inputs, or persisted pay/payslip workflow are introduced by this slice.

## Local acceptance evidence

- Java domain/service checks cover mapping, fixed day type, unique schedule/date, time order, and unmapped employee rejection.
- Java API integration checks cover the local `201` response metadata, duplicate `409`, invalid `400`, fixed server mapping/classification, client-supplied schedule rejection, and Firebase-mode `403`.
- Browser E2E covers successful local capture, duplicate/mapping/time-order rejection, raw calendar display, employee-linked rows, type-filtered visible exports, cross-module search, JSON export contents, and read-only HTTP methods for bundle/search.
- Latest fully run checks and their counts are recorded in `README.md` and `docs/peer-parity-roadmap.md`.
