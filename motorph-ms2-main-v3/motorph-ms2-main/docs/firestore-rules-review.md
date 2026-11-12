# Firestore rules review — development sandbox

**Review status:** The user published this updated ruleset. Live API smoke tests using disposable Firebase accounts verified owner-scoped synthetic employee create/update, leave and ticket create/cancel/read-back, and cross-user denial (`403`). Both disposable Auth accounts and test employee documents were deleted; one cancelled leave record (`LV-3502C01F0E60`) and one cancelled ticket (`TK-F05DD2B0D6CC`) remain under their deleted test UIDs because workflow deletes are intentionally denied. The Firebase Rules Emulator/Console Playground was not used. The assistant made no Firebase Console changes.

Reviewed file: `docs/firestore.rules`

## Verdict

The candidate rules are appropriately narrow for a disposable synthetic-data sandbox, with explicit limitations. They are **not a production MotorPH employee/payroll authorization policy** and should not be used for real employee, attendance, or payroll data.

## Published rules permit

- An authenticated account can read only its own `users/{uid}/demoEmployees`, `demoAttendance`, `demoLeaveRequests`, and `demoTickets` collections.
- Employee and attendance writes retain the earlier exact allowlists. Employee documents have no salary/government identifiers; attendance remains raw reference-shaped demo data.
- Leave/ticket create operations require exact fields, valid synthetic employee IDs/names present in the same user's demo employee collection, bounded text, and supported types/statuses. Ticket queue labels are allowlisted.
- Leave requests can only move from `Pending review` to `Cancelled`; tickets can only move from `Open` to `Cancelled`. Only `status` and `cancelledAt` may change. Workflow deletes and reviewer/approval operations are denied.
- Existing employee/attendance demo documents remain owner-accessible and deletable; workflow records have no delete permission.
- All other Firestore paths—including root employee collections and all payroll paths—fall through to deny.
- The Java backend forwards the Firebase ID token. It uses the browser-supplied UID only to form a document path; Firestore Rules compare that path UID with `request.auth.uid`, so the header alone grants no access.

The overlapping catch-all `allow read, write: if false` is intentional: specific matching allows grant only the two scoped paths; the false rule does not cancel them. Firestore evaluates every request against the active ruleset. See [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started).

## Risks and limitations

1. **Self-service accounts:** the app offers development-account creation. Every account is limited to its own isolated sandbox, but there is no company membership, HR role, supervisor role, or admin workflow.
2. **No organization-wide employee access:** per-user data is deliberately not shared between users. This is safe for testing, but not the eventual multi-user payroll design.
3. **No payroll data:** the strict field allowlists intentionally reject payroll/rate/pay fields. No payroll collection has an allow rule.
4. **Domain constraints not fully expressible here:** the rules check date/time string formats, while Java parses dates/times. They do not enforce `timeIn <= timeOut` or uniqueness of `(scheduleId, date)` across separate documents. The current seed fixture is unique and the Java model validates time order; a future attendance-write feature needs an explicit, transaction-safe uniqueness design.
5. **No employee identity binding:** accounts are scoped to their own sandbox, but there is no account-to-employee mapping. The user can select any synthetic employee in that same sandbox when creating a request. No reviewer can read or approve it; no workflow audit log or recovery mechanism exists.
6. **Project administrators:** project owners and privileged Google Cloud IAM identities may access data through administrative channels; client Security Rules are not a replacement for IAM controls.
7. **No App Check / production hardening:** not necessary for this isolated classroom prototype, but production deployment would require a broader security review, abuse controls, hosting review, monitoring, and an authorization design.

The rules use `String.matches()` for date/time shape checks, supported by the [Rules String reference](https://firebase.google.com/docs/reference/rules/rules.String). The patterns are format checks, not full calendar validation; Java performs strict parsing on read.

## Post-publication live validation

The following were tested using disposable Firebase Auth accounts and synthetic records through the running Java API / Firestore REST. Results reflect live calls after the user published the rules; the Console Rules Playground and Emulator were not separately used.

| Case | Observed result |
| --- | --- |
| Authenticated user seeds own demo collections | `200`; 3 employees + 6 attendance documents created |
| Repeat seed for same user | `200`; 0 employees + 0 attendance documents added (idempotent) |
| List/search/detail and selected employee attendance read | `200`; expected synthetic records returned |
| Inclusive one-day attendance range | `200`; exactly one raw record; `hoursCalculated:false` |
| Empty date range | `200`; zero records |
| Unknown employee | `404` |
| Reversed date range | `400` |
| Unauthenticated app request | `401` |
| Spoofed UID / cross-user sandbox path | `403` |
| Payroll collection path | `403` |
| Extra salary field on synthetic employee | `403` |
| Earlier test cleanup | PASS; earlier temporary Auth accounts and employee/attendance documents were removed |

### Additional tests against the published rules

| Case | Observed result |
| --- | --- |
| Fresh disposable UID reads workflow lists | `200`; empty leave/ticket lists; `user-scoped-firestore` |
| Synthetic employee create / update | `201` / `200`; server-generated ID and allowlisted fields persisted |
| New employee raw attendance read | `200`; zero records because no schedule mapping was created |
| Leave create / cancel / read-back | `201` / `200` / `200`; persisted record is `Cancelled` |
| Ticket create / cancel / read-back | `201` / `200` / `200`; derived queue `IT Admin`; persisted record is `Cancelled` |
| Forged UID using a valid test token | `403` |
| Test cleanup | PASS; both disposable Auth accounts and employee documents deleted. One cancelled leave (`LV-3502C01F0E60`) and one cancelled ticket (`TK-F05DD2B0D6CC`) remain under the deleted test UIDs; workflow delete is denied by design. |

Workflow delete denial itself and Console Rules Playground/Emulator behavior were not live-tested. Fake-Firestore/API tests cover workflow path forwarding and transition field masks. These results verify the live owner-scoped path smoke tests only; they do not make these rules a production HR/payroll policy. Official rules semantics and testing guidance: [Get started with Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started) and [field-level rules](https://firebase.google.com/docs/firestore/security/rules-fields).
