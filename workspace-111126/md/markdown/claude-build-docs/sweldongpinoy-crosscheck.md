# Sweldong Pinoy cross-check — what it proved, and what it can't

**Date:** 2026-11-06 · **Method:** live calls to `POST https://www.sweldongpinoy.com/` from this
sandbox, compared against our engine and against official tables.

---

## 1. There is no public API — but there is a working internal endpoint

The site's form posts `application/x-www-form-urlencoded` to its own homepage and gets JSON back:

```
POST https://www.sweldongpinoy.com/
  employment_type=Private Employee
  pay_period=Monthly|Semi-Monthly|Weekly|Daily
  payroll_period_daily_days=20
  basic_pay=53,500.00
  taxable_allowance=0.00
  nontaxable_allowance=0.00
  night_differential_hours= / night_differential_rate=
  overtime_hours= / overtime_rate=
  work_schedule=5
→ {"result":{...},"status":"OK","altresult":{...}}     # both objects identical in every test
```

It needs only the `JSESSIONID` the homepage hands out; no key, no CSRF token. `altresult` never once
differed from `result` across ~90 calls.

**This is undocumented and unsupported.** No rate limits, no versioning, no terms for automated use, and
it can change or block crawlers without notice. It is a **reference oracle for our tests**, never a
runtime dependency (see D-4 below).

---

## 2. What the comparison found

We posted MotorPH's **34 real employee pay profiles** through both systems.

| Result | Detail |
|---|---|
| SSS, PhilHealth, Pag-IBIG | **34 / 34 exact match**, every employee (after the bracket fix above) |
| Withholding tax | **33 / 34 exact**; the one difference was our bug (#3 below) |
| Net pay | **34 / 34 exact** after the fix |

### The site also reproduced MotorPH's own payslip

For 53,500 basic: SSS 1,750.00, PhilHealth 1,337.50, Pag-IBIG 200.00, **tax 5,250.83**,
net 44,961.67. The last figure is the employee's *monthly* net; MotorPH's payslip shows 22,711.67
because it deducts the full month's contributions inside one semi-monthly cutoff. The tax agreement to
the centavo — including the `.33` floor — independently confirms the annual÷12 basis.

---

## 3. A real bug the site exposed in our engine

Our monthly withholding table used `8,541.80 / 33,541.80 / 183,541.80` as the base for the 25/30/35%
bands. The correct values are the annual BIR amounts over 12:

```
102,500 / 12 = 8,541.67    402,500 / 12 = 33,541.67    2,202,500 / 12 = 183,541.67
```

The 13-centavo error appeared on every payslip earning above ₱66,666.67 taxable (₱90,000 roughly).
At 90,000 basic we produced **13,325.13**; the site produced **13,325.00**. The site was right.

**Fixed**, with three new tests pinning the bracket bases to the annual amounts. `PayrollGoldenTest`
went 61 → 65 checks and still passes; the 53,500 golden case was never affected.

---

## 4. Where we deliberately differ from the site

| Area | Site | Us | Why ours is right |
|---|---|---|---|
| **SSS brackets** | Range table (each bracket is a ₱500-wide range ending in .99) | **Was** ⌈basic÷500⌉; **now** nearest ₱500 bracket, the same banding the site uses | The earlier "identical" claim was **wrong** and has been withdrawn: round-up and bracket banding agree only when a salary sits in the upper half of its bracket. They diverge below the midpoint — ₱10,001 must credit ₱10,000, but ceil gives ₱10,500 (₱25 overpaid), and ₱25,249 must credit ₱25,000, but ceil gives ₱25,500. The engine now bands to the bracket like SSS publishes. Guarded by five boundary checks in `PayrollGoldenTest`. |
| **Pag-IBIG at exactly ₱1,500** | **2%** (₱30.00) | **1%** (₱15.00) | HDMF Circular 460: employee pays 1% *at or below* ₱1,500. The site applies "at least ₱1,500". Ours matches the circular — and MotorPH's employee file has nobody at that boundary, so real payslips are unaffected. |
| **`payroll_period_daily_days`** | **Ignored** (20/21/22/26 all produce identical output) | Day count drives pay | The parameter is cosmetic on their side: monthly pay is flat regardless of days worked. We take days worked because a payslip that pays a full month for 3 days worked is not a payslip. |
| **`work_schedule` (5/6/4)** | **Ignored** | Not modelled | Same finding — no effect on any output. |
| **Daily pay period** | Multiplies basic × `days` | Supported via `daysWorked` | Semantics differ: theirs is "daily rate × days" in one step; ours is "monthly basic ÷ 20 × days". |
| **Semi-monthly** | Doubles basic (treats input as the cutoff amount) | Deducts full-month statutory per cutoff or splits | Two defensible readings; the supplied MotorPH payslip settles ours. |
| **Tax 95k–100k** | 13-centavo variant | Annual÷12 exact | Our fix above. |
| **Tax above ~100k** | Diverges sharply (₱2,750 higher at 200k) | Annex E cumulative | The site's curve applies a ~27% marginal rate directly after ₱66,666, which does not match any published band. It appears to add the lower brackets' tax on top. **Unresolved on their side** — worth one more probe before treating their high-income output as authoritative. |
| **Year selector** | UI only; `year`/`tax_year`/`yy` params ignored | `forYear` refuses uncovered years | Ours fails closed rather than silently using the wrong table. |
| **Self-employed / minimum wage / kasambahay** | Fall through to the private-employee formula | Not implemented | Their branches are cosmetic too. |

---

## 5. The API question (answering it directly)

**Should the app call their endpoint instead of calculating?** No — and the reason is not politics:

1. **It can be down, blocked, or changed.** It is one unversioned URL with no contract.
2. **It has visible errors** (§4: ₱1,500 boundary, high-income tax, ignored parameters).
3. **Payroll needs an audit trail.** Every figure must be reproducible years later; a third party's
   live answer is not reproducible.
4. **Latency and privacy.** You would be posting salary data for every payslip to a third party.

**The right architecture, which is what we already have:**

```
Browser  →  our API (/api/payroll/*)  →  PayrollService + PayrollRuleset   ← the source of truth
                     ↑
        optional "compare with sweldongpinoy" checkbox  →  their endpoint, side by side
```

The checkbox (§6) is a **verification aid**, never the calculation. If the network is down, the app
still pays everyone correctly — which is the whole point of not outsourcing the maths.

**"Wouldn't local be better?"** Local *calculation* is exactly what we do; the maths runs on the
server the app already owns, with no external call in the path. What we must never do is calculate in
the browser: the client would then own money logic, and any user could edit it.

---

## 6. Reproducing this

```sh
# 1. start the API with fixtures
MOTORPH_TEST_MODE=true HOST=127.0.0.1 PORT=8183 java --add-modules jdk.httpserver \
  -cp lib/gson-2.13.2.jar:target/classes ph.motorph.web.AppServer &

# 2. run the differential test (needs network)
python3 tools/sweldongpinoy-crosscheck.py
```

Expected output today: every statutory line matches on all 34 employees; the only remaining
differences are the `gross` line, which is a definitional difference — the site adds non-taxable
allowances into `gross_pay`, we report payroll gross and allowances separately (§ `totalGross` vs
`totalEarnings` in `docs/api-contract.md`).

This script is **not** part of `./test.sh`: the build must never depend on a third-party website being
reachable.
