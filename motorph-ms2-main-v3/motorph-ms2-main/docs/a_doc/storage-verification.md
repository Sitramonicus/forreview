# Storage verification

The System Admin **System configuration → Database** panel distinguishes the configured backend from a verified round trip. Counts on the panel come from the running process and are not, by themselves, proof that the remote store contains the same state.

## Verify a running instance

1. Sign in as System Admin and open **System configuration → Database**.
2. Confirm the configured backend is `firestore` for a cloud deployment. If it says `file` or `memory`, the app is not using Firestore for application state.
3. Choose **Verify database read / write / read** and confirm. The admin-only, CSRF-protected endpoint performs the check while application mutations are serialized:
   - reloads the configured store;
   - compares that saved snapshot with the state this server loaded;
   - commits the unchanged snapshot using the same store implementation as normal saves;
   - reloads it and checks an exact state match;
   - then the normal request wrapper persists its audit entry before responding.
4. A green `PASS` reports the backend, state-store count and aggregate employee/attendance/payslip counts. It never returns employee names, government identifiers, payroll amounts, or the full database snapshot.

The verification write updates the Firestore snapshot metadata and uses its compare-and-set update-time precondition. It does not modify payroll or employee business data. A normal audit-log entry for the check is saved. On a mismatch or an ambiguous read/write result, the app refuses to overwrite the saved state and pauses subsequent writes; inspect/export the database and restart from the chosen verified source. Do not repeat the check blindly after a timeout.

## What this proves—and what it does not

A passing Firestore check proves that, **at that time and for that server identity**, the running app can read its saved state, match it to loaded state, commit an unchanged snapshot via the app's actual Firestore state-store code, and read it back. The E2E suite runs this check both with a local file and with the official Firestore emulator.

It does not prove that the live `motorph-jd` project is the intended production project, that its records are correct, that the service-account IAM is least-privileged, that another instance is not running, that a backup can be restored, or that production load/availability targets are met. Repeat against the actual deployment identity and project after each deployment. The app remains single-instance; the state snapshot design is not a normalized multi-instance database.

`GET /api/health` only reports the app's configured persistence mode and process health. It is not a Firestore connectivity test. `GET /api/admin/storage` is admin-only and reports process-memory counts; only the explicit POST round trip verifies the configured state store.

## Safe inspection without starting the app

`FirestoreSync.java diff` performs read-only collection loading and reports local/remote counts. It is useful before migration or restoration, but a count match is not a content-integrity proof. Never use `upload UPLOAD` or `download REPLACE` for a health check. Back up first and stop the app before any destructive copy.
