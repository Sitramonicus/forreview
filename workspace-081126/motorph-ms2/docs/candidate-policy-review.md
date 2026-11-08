# Candidate policy review from prior submissions and current references

**Status: research only; no calculator is active.** The temporary 2026 browser preview once described in this document has been removed under the current “no temporary features” instruction. No rule table or payroll estimate is shipped by the app. Peer implementations and statutory references below remain candidate research, not MotorPH policy. No official MotorPH payroll rulebook or active grading rubric was found in the supplied set. See `docs/build-status-no-local-no-temp.md` for current behavior.

## 1. What the inspected MO-IT110 peer implementation says

The supplied peer review copy at `/home/user/repo_review/full_repo/testingester-main/6/-/` contains an explicit, testable candidate model:

- `src/motorph/model/AttendanceRecord.java`: first cutoff is days 1–15; second cutoff is day 16 through actual month end. Login from 08:00 through 08:10 is treated as 08:00; logout after 17:00 is capped at 17:00. It calculates elapsed time directly from those adjusted times and does **not** subtract a meal interval.
- `src/motorph/service/PayrollProcessor.java`: the first cutoff has no deductions and net equals cutoff gross. On the second cutoff, it computes monthly contributions from the combined two-cutoff gross, computes withholding tax after those contributions, and subtracts the whole monthly deduction amount from the second-cutoff gross (floored at zero). Allowances are shown on the second-cutoff slip but excluded from net in that implementation.
- `src/motorph/model/Employee.java`: gross is hourly rate times the calculated minutes. The course-project fixtures also supply semi-monthly and hourly rates.
- The peer has substantial tests and CP2 compatibility comparisons, but its own behavior is still an implementation choice, not an authoritative MotorPH payroll rule.

**Important defect/risk found in that candidate:** an 08:00–17:00 record yields nine elapsed hours because the code caps at 17:00 but deducts no lunch. DOLE's source says normal work hours do not exceed eight hours a day and requires at least a 60-minute meal period off work; a pair of in/out times without a meal record cannot distinguish the unpaid break. The peer rule must not be copied as an hours-worked or overtime algorithm without resolving that mismatch.

The supplied peer review also records 473 of 476 comparison rows and 14 of 14 cutoff summaries matching the CP2 output; three row differences were ₱0.01 due to floating/half-centavo behavior. That is compatibility evidence, not legal-compliance evidence. See `/home/user/repo_review/submission-comprehensive-report.md` and `/home/user/repo_review/submission-feature-pseudocode.md` for the review context. No peer employee or government-ID data was copied into this app or document.

A public MO-IT110 repository surfaced the same 1–15 / 16–month-end pattern and second-cutoff deduction convention, but explicitly says its payroll tables/thresholds are copied from CP2 rather than independently verified. Treat it as corroboration of a *course-project convention*, not a company policy: https://github.com/fdtan/MO-IT110_OOP-MS2_TanFD.

## 2. Current official statutory references located

These sources can support statutory-component research, but they do not establish MotorPH's attendance import, pay-period, allowance, leave, or role policy:

| Component | Source finding | Boundary before implementation |
| --- | --- | --- |
| SSS | The SSS site publishes its contribution schedule effective Jan. 1, 2025: 15% total on MSC up to ₱35,000, split 5% employee / 10% employer; EC is employer-only. The schedule also distinguishes the MPF portion above ₱20,000. | Use the circular/table and verify every bracket and rounding boundary. Do not assume a new 2026 table without a newer official circular. |
| PhilHealth | The 2026 advisory states the 5% contribution rate effective Jan. 2025, with ₱10,000 floor and ₱100,000 ceiling. The 2025 employer advisory says use monthly basic salary and excludes overtime, allowances, 13th-month pay, bonuses and specified absence/undertime reductions from that base. | Confirm the formal-sector share split and exact payroll-period deduction method against the applicable circular. |
| Pag-IBIG | Circular 460 raises the Maximum Fund Salary to ₱10,000 effective February 2024; the employee rate is 1% at fund salary ≤₱1,500 and 2% above, while the employer rate is 2%. | Confirm applicable employee category and effective date; don't extrapolate to voluntary/other member types. |
| BIR withholding | Annex E to RR 11-2018 publishes daily, weekly, semi-monthly and monthly tables effective Jan. 1, 2023 onward. | The chosen tax base, payroll-period method, taxable allowances/benefits, year-end adjustment and rounding need explicit tests. A monthly-only example is not a semi-monthly payroll specification. |
| Hours/premiums | DOLE's official materials specify an eight-hour normal workday, a minimum 60-minute meal period off work, and statutory overtime/night/rest-day/holiday provisions. | The attendance schema lacks meal punches, a defined `dayType` enum, and an authoritative schedule/holiday assignment. Those facts must be supplied or explicitly selected as demo assumptions. |

## 3. A net-pay reference found online — useful, but not a drop-in payroll engine

`ph-payroll` is the strongest technical reference located for statutory components: its README says it uses exact decimal arithmetic, effective-dated rules, source circulars, and fails closed outside covered periods; it documents support from **2026-01** because the 2026 PhilHealth rule is the most recent component. Its scope is monthly basic pay plus statutory contributions/tax, not attendance-hour computation, MotorPH's cutoff handling, leave treatment, or a payslip workflow. It is MIT-licensed and asks users to verify results before relying on it: https://github.com/earnieacts/ph-payroll.

### Documented synthetic example from that reference

For monthly basic pay of ₱25,000, the library documents employee deductions of SSS ₱1,250, PhilHealth ₱625, Pag-IBIG ₱200, withholding tax ₱313.75, and net ₱22,611.25. A manual annualized arithmetic check gives taxable monthly income ₱22,925; annualized ₱275,100; 15% of the ₱25,100 annual amount above ₱250,000 is ₱3,765, or ₱313.75 per month. This is a reference example only—not this app's result or an approved MotorPH payslip.

Two caveats need reconciliation before adoption:

1. The BIR Annex E monthly table uses rounded peso breakpoints (₱20,833 and 15% above that), which yields ₱313.80 for ₱22,925 when read directly. The library's annualized method gives ₱313.75. The ₱0.05 difference needs a verified BIR-period/rounding interpretation and an independent approved example.
2. In the library source, `employerCost` is the sum of employer contributions (₱3,355 in its ₱25,000 example), not basic salary plus those contributions. Do not display that field as total employer payroll cost without changing/renaming it.

A second 2026 calculator repository also advertises complete deductions and net pay, but the repository surfaced with a single commit and no comparable validation evidence. It is a secondary cross-check, not a preferred authority: https://github.com/tenzerodev/rdcruz-ph-payroll.

## 4. Workflow/role patterns in peer submissions

The supplied peer corpus shows patterns, not a consistent permission contract: employee requests, an admin/manager review screen, approve/reject outcomes, leave-credit changes, and separate payroll/admin roles appear in different projects. Folder 9's `LeaveService.java` performs approve/reject and updates request files plus leave-credit counters transactionally, but the service itself has no authorization check. Other projects use different role labels and access patterns. These examples are not sufficient to invent secure Firebase roles or reviewer boundaries.

## 5. Safe recommendation / next decision

1. Keep production-style payroll, attendance-hour reports, approvals and reviewer controls disabled.
2. If the project needs a classroom demonstration before official rules are confirmed, expose the folder-6 conventions only as a **clearly labelled, synthetic candidate profile**, after the user confirms the meal-break, cutoff, deduction timing, allowance, and net-floor choices. Preserve the official statutory calculator as a separately sourced component and reconcile BIR withholding first.
3. Mark any inferred role/workflow matrix as a proposal based on the peer corpus; do not publish it as live authorization until the user approves the exact employee, HR/approver, payroll, and admin boundaries.
4. Keep the external payroll library as a comparison source, not a dependency or oracle; reproduce independent golden values and cite official circulars before expanding beyond the disposable statutory preview into attendance-based or persisted payroll.

## Sources inspected

- Supplied peer-review copy: `/home/user/repo_review/full_repo/testingester-main/6/-/src/motorph/model/AttendanceRecord.java`, `Employee.java`, and `service/PayrollProcessor.java`.
- Supplied source analysis: `/home/user/repo_review/submission-comprehensive-report.md`, `/home/user/repo_review/submission-feature-pseudocode.md`.
- SSS contribution schedule: https://www.sss.gov.ph/pay-contribution/
- PhilHealth CY2025 employer advisory: https://www.philhealth.gov.ph/advisories/2025/PA2025-0002.pdf
- PhilHealth 2026 advisory: https://www.philhealth.gov.ph/advisories/2026/PA2026-0042.pdf
- Pag-IBIG Circular 460: https://www.pagibigfund.gov.ph/documents/Circular%20No.%20460%20-%20Guidelines%20on%20the%20Pag-IBIG%20Fund's%20Implementation%20of%20Increase%20in%20the%20MFS%20Effective%20February%202024.pdf
- BIR Annex E: https://bir-cdn.bir.gov.ph/local/pdf/Annex%20E%20RR%2011-2018.pdf
- DOLE Book III: https://blr.dole.gov.ph/2014/12/11/book-iii-conditions-of-employment/
- DOLE Workers' Statutory Monetary Benefits Handbook (2024): https://bwc.dole.gov.ph/wp-content/uploads/2024/10/Workers-Statutory-Monetary-Benefits-Handbook-2024-Edition.pdf
- `ph-payroll` README and specification: https://github.com/earnieacts/ph-payroll, https://github.com/earnieacts/ph-payroll/blob/main/SPEC.md

## 6. Re-review of the user's database design and later calculator work

The user's ctxt.io document is the **MO-IT107 database design/data dictionary**, not a database file or a set of calculation rules. It does, however, lay out a useful storage model that the earlier review had not connected to a working calculation pipeline:

- `Rates` and `Tax Table` are intended as changeable reference tables; the design mentions a rate `implementationYear`, although this field is missing from the linked Data Dictionary and the design's own key constraints disagree.
- `Government Deductibles` separates monthly employer contribution, employee contribution, and withholding tax.
- `Contributions` stores SSS, PhilHealth, and Pag-IBIG amounts separately for `Employee` and `Employer`.
- `Payroll`, `Payslip`, `Earnings`, `Deductibles`, `Other Transactions`, `Government Tally`, and `Summary` cover the intended input/output and reporting areas. Payslip includes a start/end period and gross income.
- The related DFD describes the order of operations: timesheet validation → wage/adjustments → government deductibles → basic payroll computation → payslip → monthly summary. The System Admin has a rate-update flow.

That design is valuable for the **mechanics before calculation**, but it contains no actual rates, effective date ranges, formula, rounding policy, or worked expected values. In particular, the tax table is not explicit about whether each row is monthly, semi-monthly, or another withholding period, and the monthly government-deduction key shown in the design has no year component. The user-provided full synthesis likewise warns that the PDFs do not state statutory values or arithmetic. The design should be versioned and reconciled before it is used as a persistent production schema.

A different older SQLite file exists in peer folder 1 (`/home/user/repo_review/full_repo/testingester-main/1/-/myMotorPhdb.db`). I inspected its table definitions only—not any records—and found `Users`, `Allowance`, `Salary_Deduction`, `EmployeeAudit`, and `Employee_info`, not the normalized Rates/Tax Table/Government Deductibles design in the ctxt link. It is not a source of verified statutory rates.

### A later prior project did tackle the calculation pipeline

The supplied folder-9 review copy, `/home/user/repo_review/full_repo/testingester-main/9/-/oop/`, contains an actual `PayrollCalculator` orchestrator and separate SSS, PhilHealth, Pag-IBIG, and withholding-tax calculators. Its pipeline is:

1. derive gross as worked days × hourly rate × 8;
2. calculate employee/employer statutory items;
3. calculate withholding after employee statutory contributions;
4. calculate net and build an immutable payroll record.

Its NOTES and console tests report this pipeline exercised. But the formulas are hard-coded as Java `double` constants and the source XLSX matrices cited by its verification note were on a personal Windows path, not among the supplied files. The source values are **not a current 2026 ruleset**:

- SSS caps the employee share at ₱1,125, which predates the 2025 schedule (2026 uses the current schedule effective January 2025: 5% employee / 10% employer on MSC up to ₱35,000, plus employer-only EC).
- PhilHealth uses 3%, a ₱10,000 floor/₱60,000 ceiling and a 50/50 split; the current direct-contributor advisory for formal-sector MBS gives 5%, ₱10,000 floor/₱100,000 ceiling, with allowances and overtime excluded from the MBS base.
- Pag-IBIG caps at ₱100, reflecting the former ₱5,000 MFS; Circular 460 moved the MFS ceiling to ₱10,000 effective February 2024, allowing up to ₱200 per side.
- Its first taxable monthly BIR band uses 20%; Annex E uses 15% from ₱20,833 to ₱33,332. The prior project also does not make a semi-monthly tax table part of its calculator API.

A separate folder-7 `ContributionCalculator.java` also computes semi-monthly values, but doubles the pay-period input to a monthly proxy and reuses old CP2 schedules: SSS max ₱1,125, PhilHealth 1.5% employee amount with ₱900 cap, Pag-IBIG cap ₱100, and a 20% first taxable band. Its payroll code labels overtime pricing as temporary pending confirmation. Folder 3 is even simpler (flat 4.5% SSS, 2% PhilHealth, ₱100 Pag-IBIG, no tax schedule). These show earlier mechanical exploration, but none supply a current validated 2026 profile.

The older folder-6 implementation is even more explicitly a CP2/course-project compatibility profile, not a 2026 statutory table. Neither implementation was copied into the current app.

## 7. Withdrawn temporary 2026 preview — historical research only

A temporary synthetic-input monthly preview was previously added during an earlier iteration. It was explicitly non-persistent, but the current user instruction prohibits temporary features, so the preview UI, calculator, rules module, CSV export, styling, and preview-only tests have been removed. None of the numbers in this historical section should be shown or relied upon as a current app result. The official-source links above remain useful research inputs for a future approved, persisted payroll engine; they are not enough to resolve MotorPH's pay-period, eligibility, taxable-base, deduction-allocation, calculation, authorization, or audit rules.

The working application now presents a fail-closed payroll configuration gate and calculates no payroll amount. A durable payroll feature must be implemented only after its policy inputs and independently verified examples are approved. See `docs/build-status-no-local-no-temp.md`.
