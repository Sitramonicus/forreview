# Comparison: Claude's build vs. the current workspace

Recorded before any implementation work, as required. Sources inspected:

- **Claude's build (the base):** `github.com/Sitramonicus/forreview` → `motorph-ms2-main/motorph-ms2-main`.
  76 Java files, 9,519 lines of main source in one package (`ph.motorph.web`), 41 browser modules,
  `test:web` + Playwright e2e, Firestore REST adapter, Firebase Auth sign-in.
- **The current workspace:** `motorph-ms2` on the same repository, plus the working copy this
  session began with.

## What the workspace copy actually is

The `motorph-ms2` folder on `main` is **not** a second implementation to merge. `diff -rq` against the
working copy shows 110 differences and they are all additive: newer docs (`identity-seats-and-testing-roles.md`,
`build-status-no-local-no-temp.md`), the seat-registry work (`RoleAssignment`, `SeatRegistry`,
`FirebaseTokenVerifier`), and the hardened `/api/payroll/rules`. Claude's tree has none of it because it
predates it. So the merge question is narrow: **take Claude's build, and port the later corrections into it.**

## Keep from Claude

| Area | Why it wins |
|---|---|
| Shell and density | 9 nav views with tab sub-navigation, the flat square Tailwind theme, navy `#12233f` surfaces, the flame accent `#ea580c`, and the real MotorPH logo (308×235 plus a transparent 720×455). The workspace build had fewer screens and a coarser look. |
| Breadth of workflow | Leave, overtime, tickets, pay changes, rate proposals, payslip corrections, disbursement, remittance, adjustment forms, saved-payslip outbox, onboard/offboard, audit trail. The workspace build had a fraction of this. |
| Attendance and payroll | `CutoffPayrollService`, `AttendanceMetrics`, clock service, schedules, holiday calendar, cut-off pipeline, payslip PDF, outbox with retry. |
| Persistence layer | `FirestoreRestClient` + the four Firestore stores + `LocalStateFile` (with backups and audit archive). The workspace build had none of the local durability. |
| Front-end module split | `page-tabs`, `my-day`, `supervisor-tools`, `hr-tools`, `payroll-processing`, per-page sort/filter/summary modules. |

## Keep from the current workspace (ported into the base)

| Correction | Why it must survive |
|---|---|
| **Identity is verified, not asserted** | Claude's `AccessPolicy.Actor` is built from the `X-Demo-User` header alone when no Firebase project is set — anyone can name anyone and be that person's role. This is the same forgery hole fixed in the workspace build. Replaced by Employee ID + password + server sessions. |
| **Seats, not one-role-per-account** | The workspace build established that a person holds a *set* of roles (10006 is HR Manager and also supervises someone) and that one login must be able to exercise several roles. Claude's model is one role per account. |
| **`/api/payroll/rules` must not be public** | Claude's `/api/payroll/preview` and statutory routes were open. The workspace build gated rates behind a session and withheld the bracket tables from non-admins. Re-applied. |
| **Real employee file, no invented people** | Claude seeds `DEMO-001…005` (Mara Rivera, Eli Santos, …). Those are replaced by the 34 rows of `Copy of MotorPH_Employee Data - Employee Details.csv`. |
| **Durability discipline** | "No locals, no temps": the workspace build refused to start in a memory-only production mode. Claude's `data/local-demo.json` is a real store, which is fine, but nothing may be seeded as a fiction. |
| **Golden payslip** | The sample payslip must reproduce to the centavo (P22,711.67 net). Claude's statutory calculator is present but its golden case was never asserted against the sample. Re-asserted. |

## Replace or reconcile

| Item | Decision |
|---|---|
| Firebase Auth sign-in, `X-Firebase-UID`, token verifier | **Removed.** Replaced by Employee ID + password, Argon2id, HttpOnly session cookie. Firestore is retained as the data store where configured; authentication is fully separated from it. |
| `TwoFactor` with on-screen demo code | **Removed from sign-in.** A 2FA code displayed on the same screen as the password field adds steps without adding security. Sessions, not 2FA prompts, now carry the risk. The `SessionStore` keeps its own absolute/idle expiry instead. |
| `AccountSecurity` PBKDF2, 20,000 rounds | **Replaced** by Argon2id (BouncyCastle) with a startup-supplied pepper. The old class also held `DEFAULT_PASSWORD` in source, which stays, but only as the documented first-login credential that must be changed. |
| `AccessPolicy` role map keyed on `DEMO-00x` | **Replaced** by the CSV-derived org chart: roles from position, scope from the reporting tree. |
| Branch = free-text `location` string | **Replaced** by named branches with a leaf-team centroid rule (see `branch-assignment.md`). |
| `DemoSeedService`, `DemoLeaveRequest`, `DemoOvertimeRequest`, `DemoTicket` | **Renamed** (`SeedRecords`, `LeaveRequest`, `OvertimeRequest`, `SupportTicket`) and the "demo" wording removed everywhere in product surface. |
| "Synthetic"/"fictional"/"demo" copy in UI, docs, tests | **Removed.** |
| Flat single package `ph.motorph.web` | **Split** into `o_oop`, `c_calc`, `t_tkt`, `l_log`, `sp_secpol`, `svc` as the brief requires. |
| `test.sh` listing 16 `java` invocations | **Replaced** by a script that compiles every package and runs a registered suite list. |

## Order of authority used for the rest of this work

1. The brief (`MotorPH_Optimised_Implementation_Prompt_v2.md`).
2. Corrected behaviour from the workspace build, where it does not conflict.
3. Claude's build as the base.
4. Supplied CSVs, timecard and payslip.
5. Reasonable assumptions, recorded in `assumptions-and-data-notes.md`.
