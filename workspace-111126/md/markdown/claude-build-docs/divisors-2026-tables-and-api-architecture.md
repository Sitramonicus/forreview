# The divisor questions, the 2026 tables, and the API architecture

**Date:** 2026-11-06 · Answers to "what if the month isn't 20 days?", "168 is a magic number",
"double-check the 2026 tables", and "should this be an API at all?".

---

## 1. "What if the work month isn't 20 work days?" — the divisor

**It already can be.** MotorPH's own file has a `Gross Semi-monthly Rate` column, and the daily rate is
defined as `monthly ÷ 2 ÷ semi-monthly gross ÷ …` — formally:

```
daily rate = (monthly basic × 12) ÷ (work days per year)
```

which, for MotorPH's stated 5-day week (261 working days a year, 313 in a 6-day week), gives 20 days a
month = 240 days a year. The `20` is not a guess: it is `240 ÷ 12`, and it reproduces every row of the
employee file (₱24,000 → ₱1,200.00; ₱41,850 → ₱2,092.50; ₱53,500 → ₱2,675.00).

**What we should add is not a different constant but a per-employee `workDaysPerMonth`** — 20 for the
5-day staff, 26 for a 6-day staff, 22 for a 5.5-day staff — defaulting to 20 so nothing changes for the
34 existing employees. Then:

```
daily rate  = monthly basic ÷ workDaysPerMonth
hourly rate = daily rate ÷ 8
```

That single change replaces the `÷20` and `÷168` pair and makes the divisor explicit and defensible.
It is a **ruleset/employee-field change, not a maths change**: the golden case uses 20, so it keeps
passing unchanged.

**One caveat that matters more than the divisor:** whatever constant you pick, a *monthly* employee is
paid the same monthly basic whether the month has 18, 20 or 22 working days. The divisor only exists to
price a *partial* month (days worked, absences, OT hours). Do not let it decide full-month pay.

---

## 2. "168 is a magic number" — it is, unless you show the derivation

```
168 = 20 days × 8.4 hours   ← the real definition
    = 240 work days × 8.4 hours ÷ 12
```

8.4 hours (8h + a 1-hour unpaid meal, prorated to a 5-day week) is the standard Philippine
"8.4-hour working day" used by DOLE. The cleaner presentation:

```
hourly rate = daily rate ÷ 8          ← 8 paid hours per day
daily rate  = monthly ÷ 20
    ⇒ hourly = monthly ÷ 160
```

**But `÷160 ≠ ÷168`:** ₱53,500 ÷ 160 = ₱334.38, while MotorPH's file says ₱318.45 (= ÷168). The 8.4
hour divisor *includes* the meal hour and therefore pays less per hour. That is a real policy choice,
not a rounding detail:

| Divisor | Hourly at ₱53,500 | One OT hour at 125% |
|---|---|---|
| ÷160 (8 h/day) | 334.38 | 417.97 |
| ÷168 (8.4 h/day, MotorPH) | 318.45 | 398.07 |

**Recommendation:** keep ÷168 because it is what MotorPH's own file and the supplied payslip use, but
**stop calling it magic**: expose it as `hoursPerMonth = workDaysPerMonth × 8.4` in the ruleset, so a
6-day-week employee automatically gets `÷218.4`. Anyone reading the code then sees where the number
comes from and can change it once, in one place.

---

## 3. "Double-check the 2026 tables"

All four checked against official schedules **and** against sweldongpinoy's live endpoint
(34 real employees, every statutory line matching to the centavo).

| Item | Ours | 2026 official | Verdict |
|---|---|---|---|
| **SSS total rate** | 20% (5% EE + 10% ER + 5%…) | 15% of MSC, 5% employee / 10% employer, plus EC | ✔ Our split is right: `sssEmployee` 5%, `sssEmployer` 10% |
| **SSS MSC** | ₱5,000 – ₱35,000, ₱500 steps | Same; unchanged since Jan 2025 | ✔ |
| **SSS bracket rounding** | ⌈basic / 500⌉ | Range table `1.00–5,249.99 → 5,000` | ✔ **Equal in every case** — brackets are 500 wide with a 250 offset, so ceil-to-500 reproduces the table exactly. Verified at 5,001 / 5,250 / 10,001 / 25,249 / 25,250 / 25,750. |
| **SSS EC** | ₱10 below MSC 15,000, ₱30 at/above | Same | ✔ |
| **MPF** | Not computed separately; MSC caps at 35,000 | MPF covers the portion of MSC above ₱20,000, at 5% total | ⚠ **Simplification, not an error.** The 15% total already includes MPF; splitting it out matters only if MotorPH must file MPF separately. Flagged for confirmation. |
| **PhilHealth** | 5% of basic, floor ₱10,000, ceiling ₱100,000, split 50/50 | Same, no 2026 increase | ✔ |
| **Pag-IBIG** | 2% of min(basic, 10,000); **1% at exactly ₱1,500** | 1% *at or below* ₱1,500 | ✔ ours and the circular; **the site says 2% and is wrong** |
| **BIR monthly** | floors 20,833.33 / 33,333.33 / 66,666.67 / 166,666.67 / 666,666.67 | annual ÷ 12 | ✔ |
| **BIR bases** | **8,541.67 / 33,541.67 / 183,541.67** | 102,500 / 402,500 / 2,202,500 ÷ 12 | ✔ **fixed today** — they were `…41.80` |
| **BIR semi-monthly** | its own table, not the monthly one halved | its own table | ✔ confirmed by the payslip |

**The one open question left:** whether MotorPH wants the MPF portion of SSS reported separately. That
is a filing decision, not a maths error, and it changes no take-home pay.

---

## 4. The API questions

### 4.1 "Is there a way to API it?"

**It already is an API.** Every figure comes from JSON endpoints — `/api/payroll/preview`,
`/api/payroll/runs`, `/api/payroll/timesheet`, `/api/payroll/rules`. The browser is just one client.
`curl` works today:

```sh
curl -s -X POST http://127.0.0.1:8183/api/payroll/preview \
  -H 'Content-Type: application/json' \
  -d '{"employeeId":"10015","payPeriod":"SEMI_MONTHLY","periodStart":"2026-01-02",
       "periodEnd":"2026-01-15","monthlyBasic":"53500","daysWorked":10,
       "riceSubsidy":"1500","phoneAllowance":"2000","clothingAllowance":"1000"}'
```

So nothing needs to be built to "make it an API" — it needs **packaging**: an OpenAPI description, a
token-scoped API key for machine clients, and documented versioning. That is a small, safe addition.

**What must not happen:** moving the maths into JavaScript. Right now the browser never computes money;
that is a security property, not a stylistic one.

### 4.2 "What if the API is down — wouldn't local be better?"

This is really three separate questions, with three different answers:

| Scenario | Answer |
|---|---|
| **The Firebase/network is down** | The API cannot read or write data. A local calculate-only mode would still produce correct payslips from numbers already on screen — but it could not *save* a run. Worth having as an explicitly-labelled offline preview, **not** as a second source of truth. |
| **sweldongpinoy is down** | Nothing breaks. Their endpoint is only a comparison checkbox, never in the calculation path. |
| **"Local" meaning "calculate in the browser"** | **No.** The client would then own money logic, and anyone could edit it with dev-tools. A payslip is a financial record; its arithmetic must run where it can be audited. |

The architecture we already have is the right shape:

```
Client (browser or curl)  →  OUR API  →  PayrollService + PayrollRuleset   ← single source of truth
                                  ↑
        optional: compare-with-sweldongpinoy checkbox → their endpoint (read-only, side by side)
```

**The optimal path:** keep the Java service as the only calculator, add the optional comparison checkbox
(§5), and if offline resilience is required later, cache the *last computed payslip* client-side and
label it "cached — not recalculated". Never a second calculator.

### 4.3 The sweldongpinoy checkbox you asked for

Feasible, with one important design point: **the browser must not call their site directly.** Reasons:
CORS, the `JSESSIONID` handshake, and — decisively — you would be sending salary data from the user's
machine to a third party where you cannot log it.

The correct shape is a **server-side proxy on our own API**:

```
POST /api/payroll/compare-sweldongpinoy   { the same body as /preview }
  → our API posts the form to sweldongpinoy with a short timeout
  → returns { ours: {...}, theirs: {...}, differences: [...] }
  → if their site is slow or down: { theirs: null, reason: "reference unavailable" }
```

The UI shows a checkbox "compare with sweldongpinoy.com", and when ticked displays a two-column
comparison with differences highlighted. **The payslip itself never uses their numbers.** If the
checkbox fails, the payslip is unaffected — that is the whole point.

**Recommendation:** build this as a *diagnostic* feature, clearly labelled, and keep it off by default.

---

## 5. What I would do, in order

1. **Keep the maths where it is.** It now matches MotorPH's payslip *and* sweldongpinoy on all 34
   employees. That is the strongest position available and it is already achieved.
2. **Make the divisors explicit** — `workDaysPerMonth` (default 20) and `hoursPerMonth = days × 8.4`.
   No behaviour change for today's data; a clear derivation for tomorrow's.
3. **Add the compare endpoint** as a server-side proxy with a hard timeout and a visible "reference only"
   label.
4. **Do not** build a second calculator in the browser, and do not make any payslip depend on a
   third-party call.
5. **Then** the role model — because that, not the arithmetic, is what stands between this and a real
   app. See `docs/role-model-and-access-matrix.md`.
