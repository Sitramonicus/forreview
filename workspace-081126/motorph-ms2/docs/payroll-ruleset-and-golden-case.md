# Payroll ruleset, golden case, and what the supplied data settled

**Status date:** 2026-11-05. **Supersedes** the "payroll gated, no calculation" statements in
`docs/build-status-no-local-no-temp.md` and the research-only status in
`docs/candidate-policy-review.md`. Payroll now calculates, saves runs, and is verified against a
document MotorPH produced.

## 1. What closed the gap

The supplied documents contain a **MotorPH payslip with every line item**, and a **timecard with
break punches**. That payslip is a controlling example: any ruleset that does not reproduce it is
wrong, regardless of what a summary table says. The engine in `PayrollService` + `PayrollRuleset`
reproduces it to the centavo, and `PayrollGoldenTest` fails the build if that ever stops being true.

**Source documents**

| Document | What it settled |
|---|---|
| `Copy of MotorPH Payslip - MotorPH Payslip.csv` | The full arithmetic chain: gross, benefits, four deductions, totals, take-home |
| `Copy of MotorPH Sample Timecard - MotorPH Payslip.csv` | Break out / break in punches, the 8-hour day, weekend and holiday rows at 0 hrs |
| `Copy of MotorPH_Employee Data - Employee Details.csv` | Monthly basic, the three allowances, and the 20-day / 168-hour divisors |
| sweldongpinoy.com (rates: 2026) | The rate *structure*: basic pay + taxable/non-taxable allowance + night differential + overtime → statutory → withholding |

## 2. The golden case

Payslip `15-2026-01-15`, employee 15, period 2026-01-02 → 2026-01-15, monthly basic ₱53,500,
10 days worked, benefits ₱4,500.

| Line | MotorPH | Engine |
|---|---:|---:|
| Daily rate (53,500 ÷ 20) | 2,675.00 | 2,675.00 |
| Gross income (× 10 days) | 26,750.00 | 26,750.00 |
| Rice / phone / clothing | 1,500 / 2,000 / 1,000 | same |
| Benefits total | 4,500.00 | 4,500.00 |
| SSS employee | 1,750.00 | 1,750.00 |
| PhilHealth | 1,337.50 | 1,337.50 |
| Pag-IBIG | 200.00 | 200.00 |
| Taxable base | *not printed* | 50,212.50 |
| Withholding tax | 5,250.83 | 5,250.83 |
| Total deductions | 8,538.33 | 8,538.33 |
| **Take home pay** | **22,711.67** | **22,711.67** |

Two MotorPH conventions are now **implemented as evidenced**, not guessed:

1. **Statutory amounts are monthly and are deducted in full inside a semi-monthly cutoff.** The
   payslip deducts a full month's SSS (₱1,750 = 5% of the ₱35,000 ceiling) from one cutoff. This is
   the default (`FULL_MONTHLY_EACH_PERIOD`); a `SPLIT_EVENLY` mode exists but does not reproduce
   the payslip.
2. **Withholding is computed on monthly basic less employee statutory shares**, using the monthly
   Annex E table. This is `BASIC_ONLY`, the default. `BASIC_PLUS_TAXABLE_ALLOWANCE` exists for
   comparison and changes the answer.

## 3. Rate decisions that are now evidenced rather than assumed

**Bracket floors are the annual thresholds divided by 12, not the whole-peso rendering.** This is
the one finding that could not have been guessed:

```
50,212.50 taxable
  floor 33,333.00 (whole peso)  -> 1,875 + (50,212.50 - 33,333.00) × 20% = 5,250.90   ✗
  floor 33,333.33 (400,000/12)  -> 1,875 + (50,212.50 - 33,333.33) × 20% = 5,250.83   ✓
```

The same applies to the semi-monthly table, which uses the annual thresholds over 24 and is **not**
the monthly table halved. Both tables are in `PayrollRuleset`.

**Divisors are `monthly ÷ 20` and `monthly ÷ 168`.** Every row of the supplied salary table satisfies
these to the centavo (₱24,000 → ₱1,200 daily and ₱142.86 hourly; ₱41,850 → ₱2,092.50 / ₱249.11).

**SSS MSC rounds up to the next ₱500 step and is then clamped** to ₱5,000–₱35,000. At ₱53,500 both
readings give ₱1,750, so the payslip cannot settle rounding; the round-up rule is the standard SSS
method and is flagged as such in the ruleset.

**Timekeeping uses punches, not a fixed lunch.** The supplied timecard reports 08:00–12:00 /
13:00–17:00 as **8 hrs**, i.e. out − in − meal. `WorkedTime` implements exactly that, and
`TimesheetServiceTest` asserts the supplied row shape. Consequence worth stating plainly: a row with
**no** break punches yields 9 hours, so missing meal punches inflate paid hours. That is a data
requirement, not a rule the app can invent.

## 4. What is implemented

- `Money`, `PayrollRuleset` (2025 and 2026, effective-dated, fail-closed outside them),
  `PayrollService`, `Payslip`, `PayrollRun`, `PayrollStore` + `InMemory`/`Firestore` adapters,
  `TimesheetService`, `WorkedTime`, break-aware `AttendanceRecord`.
- Routes: `GET /api/payroll/rules`, `GET /api/payroll/timesheet`, `POST /api/payroll/preview`,
  `POST /api/payroll/runs`, `GET /api/payroll/runs`, `GET /api/payroll/runs/{id}`.
- Employee records carry monthly basic, rice subsidy, phone allowance and clothing allowance; the
  payroll picker only offers employees that have a pay profile.
- UI: payslip in MotorPH's layout, remittance summary (both shares), statutory rate table, run
  history with saved snapshots, payslip CSV export, "use stored timesheet days".
- Saved runs are **immutable snapshots**: inputs, ruleset version, both calculation assumptions, and
  every computed line are stored, so a reprint never recalculates.
- The whole chain is pinned **at the HTTP layer** by `AppServerIntegrationTest`: ruleset read and
  uncovered-year refusal, timesheet reduction (`19 days / 152.00 hours` from the supplied December
  timecard), every golden payslip line from `POST /api/payroll/preview`, save → history → replay by
  run ID, `405` on delete, `400` on unknown fields, over-long days, reversed or uncovered periods,
  `415` on a non-JSON body, `401` without a Firebase session, and — with a stub Firestore — all
  payroll calls confined to `users/{uid}/demoPayrollRuns` with the ID token forwarded.

## 5. What is still not done

| Gap | Why it is still open |
|---|---|
| Attendance → days worked → payslip, automatically | The UI has "use stored timesheet days", but days worked is still an operator-confirmed field. Absence, tardiness and undertime have no approved rule. |
| Overtime and night-differential **hours** from punches | Rates are parameterised (125% / 10%); the qualifying hours are not derived. Overtime approval is not modelled. |
| Leave pay, holiday/rest-day premiums, 13th month, other earnings | No rule supplied. |
| A **multi-employee** run | API and store support many payslips per run; the UI computes one employee at a time. |
| Approval, roles, audit, payslip access control | Owner-only Firestore paths; no role model. |
| Ruleset for years before 2025 | `forYear` throws outside 2025–2026 rather than borrowing an adjacent table. |

## 5a. Two reporting details the UI now reconciles

- A payroll "gross" in the API excludes allowances, so a run reports `totalGross`, `totalBenefits` and
  `totalEarnings` (= gross + benefits) rather than one ambiguous number. The payslip SUMMARY block
  prints Gross Income → Benefits → **TOTAL EARNINGS** → Deductions → Take Home Pay, so the printed
  figures add up and match the run history row exactly.
- `totalEarnings` in `web/payroll.js` returns a raw decimal string, not display text; the view formats
  every payslip row exactly once. A web test pins this, because returning preformatted text silently
  rendered ₱0.00 in the summary.

## 6. What the remaining questions now are (short list)

> Cross-review questions on hourly rates, leave credits, 13th month, the Pag-IBIG ₱1,500 boundary,
> overtime, paid leave, early login and self-approval are answered with evidence in
> [`docs/statutory-and-policy-questions.md`](statutory-and-policy-questions.md). One of those answers
> (early login) identifies a real defect in `TimesheetService`.

1. **Cutoff convention:** confirm `FULL_MONTHLY_EACH_PERIOD` is company policy, or supply the split
   rule. This is the largest remaining per-payslip difference.
2. **Withholding base:** confirm `BASIC_ONLY` (as the payslip shows) or include taxable allowances.
3. **MSC rounding:** round up to the bracket, or use the bracket containing the salary? Both give
   ₱1,750 at the ceiling, so only a mid-range payslip can settle it.
4. **Overtime and night-differential hours:** how are they qualified and approved?
5. **Absence/tardiness/undertime:** which day is "no work" versus "no record"?
6. **2027 tables:** the effective-dating mechanism works; next year's rates must be added when
   published.

## 7. Non-negotiable test

`PayrollGoldenTest` reproduces the supplied payslip (14 amounts), the bracket boundaries, the MSC
steps, the semi-monthly table, the split and allowance variants, the rejections (uncovered year,
negative pay, too many days, negative take-home, bad dates, unknown period), and the remittance
totals. If a future rule change breaks the golden case, the test fails the build on purpose.
