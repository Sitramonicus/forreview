# Progress: golden payslip closed, auth gap identified

## Closed this session

**`GoldenPayslipTest` — PASS, 18 checks.** MotorPH's sample payslip is now reproduced to the centavo by the
production engine: GROSS 26,750.00, benefits 4,500.00, deductions 8,538.33, **take-home 22,711.67**.

Four defects stood between the engine and the sample, all fixed at the source:

1. **`MonthPayroll` never stored its `split`.** The constructor took `Split split` but never assigned the field, so
   `split == Split.FULL_FIRST` was always false and the whole month's allowances were halved. Now assigned
   (null-safe to `HALF`), so the sample convention — "the month's allowances are paid with the 1–15 cut-off" —
   actually runs.
2. **`PayslipWriter` counted contract figures as pay.** "Monthly Salary" (53,500), "Daily Rate" (2,675) and "Days
   Worked" sat in `Payslip.earnings`, so the slip's own total was 56,175 higher than the ledger. They now live in a
   non-additive `Payslip.info` list, with `monthlySalaryCents` / `dailyRateCents` mirrored for callers. The slip's
   reconcile guard therefore passes on real arithmetic instead of being bypassed.
3. **January 1st was paid as a holiday nobody worked.** The engine credited every regular holiday in the period.
   Labor Code **Art. 94** pays an unworked regular holiday only if the employee was present on the preceding
   workday; 1 January 2026's predecessor is 31 December 2025, which is outside the cut-off, so nothing is owed. The
   check is now implemented in `CutoffPayrollService.presentOnPrecedingWorkday`.
4. **The reconcile guard compared the wrong cut-off.** It printed `cutoff + 1` in a message about cut-off 0, which
   is what made the first two defects take so long to see. The message now prints both numberings and the dates.

**The phone-allowance conflict is disclosed, not resolved.** `SampleSlipCorrections` books the difference between
the sample (2,000.00) and the employee file (1,000.00) as a named, dated `PayrollAdjustments` allowance correction
for 10015 in 2026-01, cut-off 1. It is idempotent, it is visible in the adjustment list and the audit trail, the
file keeps its own figure, and it books nothing if the file is ever corrected. The import records the disagreement
in its notes so it shows in the UI. `MonthPayroll.allowanceWithCorrections` had also been silently dropping
corrections — it looked for the type `"PHONE"` while the store holds `"Phone allowance adjustment"`.

## Two production gaps found while testing the API

1. **The server never imported the employee file.** `AppServer` started with an empty roster, so sign-in failed with
   "Unknown demo account" for every real employee and `/api/demo/accounts` returned nothing. The constructor now
   imports `EmployeeData.csv` (idempotently, so a saved state is not overwritten), schedules every imported person,
   binds `AccessPolicy` to the resulting `OrgChart`, and books the disclosed sample correction.
2. **The API built its own payslip JSON.** `/api/payroll/payslip` re-added the figures by hand, so nothing guaranteed
   the printed numbers matched the tested ones, and its allowances block reported file values only. It now serves a
   `printed` object built by `PayslipWriter` — the same code the acceptance test checks — including `reconciled` and
   `netFromLinesCents`, plus `phoneFileCents` beside `phoneCents` so the correction is visible.

## RBAC corrected against the design document

The permissions matrix on page 11 of the system design document was transcribed to
`docs/a_doc/rbac-matrix-conformance.md`. Three places where `AccessPolicy` was more generous than the document were
corrected **in the code, not in the test**:

- **HR may read attendance but not write it.** The matrix gives HR `R`; only the Supervisor (`R, U` team) and the
  System Administrator (`C, R, U, D`) may edit. `requireAttendanceEdit` was admitting HR.
- **HR does not edit schedules.** Page 21 gives schedule editing to the Supervisor; page 22 gives HR onboarding and
  records. `requireScheduleEdit` was admitting HR.
- **Team scope is recursive for reads, direct for writes.** `AccessPolicyTest` now asserts both, plus the CMO's
  three-person chain and the CEO's company-wide scope. 43 checks pass.

## Open: the login flow the brief requires

The security machinery in `sp_secpol` already meets the brief — Argon2id with a per-account salt and an
env-supplied pepper, constant-time comparison, 5-attempt lockout, sessions of 32 random bytes stored only as
SHA-256, 15-minute idle and 8-hour absolute limits, initial password `MotorPH-demo-2026` with
`mustChangePassword`. **But the running server does not use it.** `AppServer` still signs people in with the legacy
`AccountSecurity` + simulated-2FA path and a custom `X-Demo-User` / `X-Demo-Session` header pair, which is the
opposite of the brief's "Employee ID + password, HttpOnly cookie, CSRF defence".

Wiring `AuthService` into `AppServer` is the next piece of work, followed by the cookie/CSRF plumbing and the
frontend's login page. The remaining legacy tests (`AttendanceServiceTest`, `ClockServiceTest`,
`CutoffPayrollServiceTest`, `WorkflowServiceTest`, `LocalStateFileTest`, `AppServerIntegrationTest`, `ScaleTest`)
constitute the recorded follow-on work.
