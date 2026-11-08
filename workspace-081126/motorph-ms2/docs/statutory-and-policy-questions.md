# Statutory and policy questions — answers and evidence

**Date:** 2026-11-05 · **Status:** answers are recommendations for MotorPH to confirm; nothing here is
implemented without that confirmation except where noted.

Eight questions came up in cross-review. Each one is answered in three parts: **what the code actually
does today**, **the answer**, and **what it would change**.

Four of the eight premises do not match this codebase. That is stated first, because answering a
question about behaviour the system does not have is how wrong policy gets written into a pay run.

---

## Q1. Placeholder hourly rates ₱150 / ₱160 / ₱200

**Premise does not apply.** There is no flat hourly-rate table anywhere in this system, and there is no
source for those three numbers in any supplied file.

**What the code does:** hourly rate is *derived*, never stored as a constant —
`hourlyRate = monthlyBasic ÷ 168` (`PayrollService.HOURS_PER_MONTH`), and `dailyRate = monthlyBasic ÷ 20`.
That divisor is not a guess: it reproduces every row of MotorPH's own employee file, which carries both
`Basic Salary` and `Hourly Rate`:

| Employee | Basic salary | Hourly rate in file | Basic ÷ 168 |
|---|---|---|---|
| 10001 Garcia, Manuel III | 90,000 | 535.71 | 535.71 ✔ |
| 10002 Lim, Antonio | 60,000 | 357.14 | 357.14 ✔ |
| 10005 … | 52,670 | 313.51 | 313.51 ✔ |

The lowest hourly rate in MotorPH's own file is ₱313.51. A ₱150/hour rate belongs to a salary of about
₱25,200/month; ₱200/hour to about ₱33,600.

**Answer: do not introduce flat hourly rates.** They are not needed, and they break the golden case:
at ₱150/h, 152 paid hours would pay 22,800 instead of the 26,750 MotorPH's payslip shows — a ₱3,950
error on one employee. If a demo needs low-salary employees, lower the *monthly basic* on the employee
record and let the divisor produce the rate; that keeps one rule for everyone.

**Impact:** none — no change required. If the other implementation has such a table, it is a defect.

---

## Q2. Leave credits — Vacation 5, Medical 5, Personal 3

**Premise does not apply.** No leave credits or balances exist in this system. The Requests view stores
a request and a status; `app.js` says so explicitly: *"no days or balances are calculated."*

**Answer: acceptable only as clearly-labelled demo policy, and it must not touch payroll.**

Three reasons to be careful:

1. **The numbers are company policy, not law.** The only statutory floor is the Service Incentive Leave
   of 5 days for employees with at least one year of service (Labor Code Art. 95). "Vacation 5 /
   Medical 5 / Personal 3" is an invented but reasonable demo policy — the danger is presenting it as
   a statutory entitlement.
2. **A balance is a running ledger, not a constant.** Once a credit exists, someone has to decide
   accrual (monthly? annually? prorated?), carry-over, conversion to cash, and what happens on
   resignation. None of that is decided.
3. **Credits are not pay.** A balance only affects money through the *leave pay* rule (Q6), which is
   still undecided.

**Recommendation:** keep credits out of the payroll chain. If the UI needs them for realism, store them
on the employee record with an explicit `source: "demo-policy"` marker and show them as informational
only. Do not let a balance silently reduce or increase a payslip.

---

## Q3. 13th-month assumption in tax (₱90,000)

**Premise does not apply.** This engine computes no 13th month at all — there is no line for it and no
tax treatment of it. `docs/payroll-ruleset-and-golden-case.md` lists "13th month" under not-done.

**The ₱90,000 figure itself is correct** — under RA 10963 (TRAIN), the first ₱90,000 of 13th-month pay
*and other benefits combined* is exempt; the excess is taxable. Two corrections to the framing:

- **It is a threshold on the benefit, not on basic salary.** An employee on ₱30,000 basic receives a
  ₱30,000 13th month and pays no tax on it; the exemption is exhausted at a basic salary of roughly
  ₱90,000 because the 13th month is then itself ₱90,000. So "only matters above ₱90,000 basic" is
  approximately true by coincidence, not by rule.
- **It is annual and cumulative, not per cutoff.** You cannot decide the taxable part of a 13th month
  inside a semi-monthly run without knowing the year's other bonuses. It belongs to an annual
  adjustment, computed once, with the employee's year-to-date benefit total.

**Recommendation:** treat 13th month as a separate annual run, not a payslip line: compute
`min(total benefits for the year, 90,000)` as exempt and tax the excess at the employee's marginal
band. That is a deliberate follow-on feature, not a tweak to `PayrollService`.

---

## Q4. Pag-IBIG at exactly ₱1,500 — 2% (site) or 1% (circular)?

**This one the code already answers, and it follows the circular.** `PayrollRuleset.pagibigEmployee`:

```
fund ≤ 1,500 → 1%      # "at or below", inclusive
fund > 1,500 → 2%
```

and `PayrollGoldenTest` pins both edges: **"Pag-IBIG 1% at 1,500" = 15.00**, "Pag-IBIG 2% at 1,501" = 30.02.

**Answer: keep 1% at exactly ₱1,500.** HDMF Circular 460 (2024, still in force for 2025–2026) puts the
employee at 1% for a Monthly Fund Salary *at or below* ₱1,500 and 2% above it. Current 2026 contribution
tables state the same boundary. If sweldongpinoy shows 2% at exactly ₱1,500, that is the outlier and
worth reporting to them — the money is ₱15, but the rule should be right because the same comparison
pattern (`≤` vs `<`) is reused by every other clamp in the ruleset.

**Impact:** none — already correct and tested. No change.

---

## Q5. Overtime — what counts, and at what rate?

Two separate questions, and the first one matters more than the rate.

**What counts.** Overtime is work **beyond 8 hours in a workday** (Labor Code Art. 87), *not* "any time
after 17:00". A 10:00–19:00 shift with an hour's meal performs 8 hours and earns no overtime; a
06:00–15:00 shift earns none either. The test is *productive hours in the day*, not the clock.

**Rates** (DOLE matrix — the premium applies on top of the day's own rate):

| Day type | First 8 hours | Overtime hours |
|---|---|---|
| Ordinary working day | 100% | **125%** (+25%) |
| Rest day / special non-working day | 130% | **169%** (+30% of 130%) |
| Special day that is also a rest day | 150% | **195%** |
| Regular holiday | 200% | **260%** |
| Regular holiday that is also a rest day | 260% | **338%** |

Night differential (+10%, Art. 86) applies to hours between **22:00 and 06:00** and stacks on top of
whichever rate already applies — it is 10% of the *applicable* rate, not of plain basic.

**What the code does today:** overtime hours are an operator-entered field multiplied by
`hourlyRate × rate%` with the rate defaulting to 125%. That part is correct for an ordinary day. Two
gaps:

1. **Only one multiplier exists.** Rest-day and holiday premia (169/195/260/338) are not modelled even
   though every attendance row already carries a `dayType` field.
2. **Nothing prevents double pay.** `daysWorked` and `overtimeHours` are independent inputs, so an
   operator can enter 10 days *and* 20 OT hours for the same period without the system noticing that
   the OT is already inside the day count.

**Recommendation:** keep 125% as the ordinary-day default; add per-`dayType` multipliers before holiday
premiums are claimed to work; and add a guard tying OT hours to the days they belong to. Until then the
UI should say the app supports ordinary-day overtime only.

---

## Q6. Should approved leave count as paid hours in payroll?

**Neither "paid hours" nor "not counted" — approved paid leave is a paid *day*, not worked hours.**

- It **does** contribute to basic pay (a paid day off is a paid day).
- It **does not** contribute to hours worked, so it must never generate overtime or night differential.
- Unpaid leave contributes to neither.
- It belongs on its own payslip line ("Leave pay" or a separate day count), not silently merged into
  `daysWorked`, or the payslip can no longer explain itself.

**But it cannot be implemented yet**, for a reason worth stating plainly: leave *approval* does not
exist in this system. Requests can be submitted and cancelled; there is no approver, so "approved
leave" is not a state the data can be in. Payroll does not read the leave collection at all.

**Recommendation:** decide the rule now, implement after an approval workflow exists and after the leave
policy decisions D-18…D-21 are closed. Do not wire unapproved leave into payroll to save time — that is
how people get paid for leave nobody granted.

---

## Q7. Should logging in before 08:00 earn extra time?

**Premise is backwards — it already does, and that is the problem.**

Verified by running this codebase's own `WorkedTime`:

| Punch pattern | Paid hours |
|---|---|
| In 07:00, break 12:00–13:00, out 17:00 | **9.00** |
| In 08:00, break 12:00–13:00, out 17:00 | 8.00 |
| In 07:00, no break recorded, out 17:00 | **10.00** |

So an employee who badges in an hour early is paid an extra straight-time hour today, with no approval
and no overtime premium — and morning overtime is paid at 100% while evening overtime entered manually
is paid at 125%. That is inconsistent whatever policy you choose.

**Answer: early arrival should not automatically earn pay.** Philippine practice treats work before the
scheduled start as compensable only when it is required or authorised; purely voluntary early arrival is
not. The defensible rule:

```
paid hours for a day = min(worked hours, scheduled hours)
extra hours = max(0, worked − scheduled)   → paid only if approved, then at the OT rate
```

**Impact — this is a real fix, not a policy nicety.** `TimesheetService` currently sums raw worked
hours. Changing it to cap at the scheduled day (and to return separately the unapproved overage) would
change reported hours for any row with an early punch or a missing meal punch. The supplied December
timecard has all four punches on every working row, so the golden case (19 days / 152.00 hours) is
unaffected — but the 9-hour "no break recorded" case would no longer inflate paid hours, which is the
correct outcome and is currently documented as a data requirement instead.

**Recommendation:** implement the cap, and show unapproved overage as a separate number. Requires
MotorPH to confirm the scheduled-hours source (currently each schedule row carries its own times).

---

## Q8. Should an employee be allowed to approve their own leave?

**No — and the current system avoids the question by not having approval at all.** Requests can be
submitted and cancelled; no approver exists, self or otherwise.

**Answer: never, and enforce it on the server.** Segregation of duties: the approver must not be the
requester. Three implementation rules if approval is built:

1. Compare **identity**, not role — an approver who is also the requester is blocked even if they hold
   the approver role.
2. Enforce it in the Java service, not the UI; the UI is not a security boundary.
3. Back it with the Firestore rule, so a direct REST call with a valid token still fails.

One honest caveat for this project's shape: today every collection lives under `users/{uid}`, so the
only approver available *is* the owner. That means approval cannot be built meaningfully before the
role model (D-32) is decided — which is exactly why it is not built.

**Recommendation:** block self-approval structurally, and keep approval out of the product until there
is a role model that can express "someone else".

---

## Summary

| # | Question | Verdict | Action |
|---|---|---|---|
| 1 | ₱150/₱160/₱200 hourly rates | Premise absent — rates are derived (`÷168`) | No change; never add flat rates |
| 2 | Leave credits 5/5/3 | Demo policy only, must not touch pay | Keep out of payroll; label as demo |
| 3 | 13th-month ₱90,000 | Correct figure, wrong framing; no 13th month exists | Separate annual feature, not a tweak |
| 4 | Pag-IBIG at exactly ₱1,500 | **Code follows the circular (1%)** | No change; already tested |
| 5 | Overtime basis and rate | Beyond 8 productive hours, not after 17:00; 125% ordinary day | Add day-type multipliers + double-pay guard |
| 6 | Paid leave as paid hours | Paid *day*, not hours | Blocked on approval workflow |
| 7 | Early login | **Already earns extra straight time — should not** | Real fix: cap at scheduled hours |
| 8 | Self-approval | Never | Blocked on role model (D-32) |

Only **#7** is a defect in the current build. #4 is already right. #1, #2, #3, #6 and #8 are decisions
or features that do not exist yet; #5 is a gap in premium-pay coverage.

**Sources:** RA 10963 (TRAIN) ₱90,000 benefit ceiling; Labor Code Arts. 86, 87, 95, 100; HDMF Circular
460 (MFS ₱10,000, employee 1% ≤ ₱1,500 else 2%); DOLE premium-pay matrix; MotorPH's supplied employee
file (basic ↔ hourly rates); this repository's `PayrollRuleset`, `PayrollService`, `WorkedTime` and
`PayrollGoldenTest`.
