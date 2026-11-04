# Matrix verification (official vs CP2 vs OOP)

> Scope: 4 deduction formulas (SSS · PhilHealth · Pag-IBIG · Withholding Tax) checked against the 4 official xlsx matrices in `C:\Users\Paul\Documents\MotorPH Files\`.
> Source of "CP2" values: verbatim copy of the 4 static methods from `src/motorphemployee/Dashboard.java:1195–1356` (run under JDK 21). Source of "OOP" values: `oop/src/motorphemployee/oop/*.java` compiled with `javac -Xlint:all` (0 warnings), run through the same harness. CP2 and OOP agree on **all 66 inputs tested** (`run.txt`), so the OOP columns are identical to CP2 by construction.
> Official values are computed from the matrix rules and the matrix cell refs cited below; nothing guessed. Verdict tolerance: |Δ| ≤ 0.005.
> No repo file was modified except this one. Harness + raw output live in the scratch dir (`cache\scratch\q4\`), not in the repo.

## Summary

| Matrix | CP2 | OOP | Verdict | Mismatch count (16 base inputs) | Mismatch count (boundary points) |
|---|---|---|---|---|---|
| SSS | `Dashboard.java:1195` / `MainMenu.java:1279` | `SSSCalculator` | 🟠 DEVIATION at exact bracket boundaries only | **0 / 16** | **44 / 44** lower bounds (3,250 … 24,750) |
| PhilHealth | `Dashboard.java:1294` / `MainMenu.java:1378` | `PhilHealthCalculator` | 🟠 DEVIATION — share, not formula | **16 / 16** | 4 / 4 (boundary set) |
| Pag-IBIG | `Dashboard.java:1312` / `MainMenu.java:1396` | `PagIbigCalculator` | 🟠 DEVIATION — employer share included | **3 / 16** (+2 UNVERIFIED: 0, 500 not in matrix) | 5 / 6 (2,500–4,750 band) |
| Withholding Tax | `Dashboard.java:1334` / `MainMenu.java:1418` | `WithholdingTaxCalculator` | 🟠 DEVIATION — bracket 1 off by ₱0.20 | **3 / 16** | 3 / 4 |

## 1. SSS

**(a) Official table** — `Home_SSS_Matrix.xlsx`, sheet `SSS Contribution Schedule`.
- `D2` = 135.00 for "Below 3,250".
- `A3:C46` = 44 rows, lower bound in `A`, upper in `C` (display only), contribution in `D`. Rows 3..46 step 500 from 3,250 (`D3`=157.50) to `A46`=24,750 (`D46`=1,125.00, "Over").
- Matrix ranges are written as `3,250 – 3,749.99` etc., i.e. **lower-bound inclusive**.
- Merged cells A1:C1 and A2:C2 are headers only; the data rows are not merged, so parsing was unambiguous.

**(b) CP2 code** — `Dashboard.java:1195–1292`: `if (totalSalary <= 3250) … else if (totalSalary <= 3750) …` (line 1198 onward), i.e. **upper-bound inclusive** `<=` chain, 45 branches, last branch `else → 1125` (line ~1290). Duplicate `MainMenu.java:1279` identical (diffed).

**(c) OOP code** — `SSSCalculator.java`: 44 bracket lower bounds `3250 + 500·i`, amounts `135 + 22.5·i`, returns `AMOUNTS[i]` when `salary <= LOWER_BOUNDS[i]`, else cap `1125`. Same upper-inclusive behavior as CP2 (proven by identical output on 66 inputs).

**(d) Verdict per test input** (16 base inputs, plus the boundary points that matter):

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 0.00 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 500.00 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 1,000.00 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 1,500.00 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 2,500.00 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 5,000.00 | 225.00 | 225.00 | 225.00 | +0.00 | ✅ MATCH |
| 10,000.00 | 450.00 | 450.00 | 450.00 | +0.00 | ✅ MATCH |
| 15,000.00 | 675.00 | 675.00 | 675.00 | +0.00 | ✅ MATCH |
| 20,000.00 | 900.00 | 900.00 | 900.00 | +0.00 | ✅ MATCH |
| 25,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 29,750.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 30,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 40,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 50,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 90,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |
| 200,000.00 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |

Boundary points (exact lower bounds — the matrix puts these in the *upper* bracket, CP2/OOP in the *lower* one):

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 3,249.99 | 135.00 | 135.00 | 135.00 | +0.00 | ✅ MATCH |
| 3,250.00 | 157.50 | 135.00 | 135.00 | -22.50 | 🟠 DEVIATION |
| 3,749.99 | 157.50 | 157.50 | 157.50 | +0.00 | ✅ MATCH |
| 3,750.00 | 180.00 | 157.50 | 157.50 | -22.50 | 🟠 DEVIATION |
| 24,750.00 | 1,125.00 | 1,102.50 | 1,102.50 | -22.50 | 🟠 DEVIATION |
| 24,750.01 | 1,125.00 | 1,125.00 | 1,125.00 | +0.00 | ✅ MATCH |

**(e) Mismatch count** — **0 / 16** on the base inputs; **44 / 44** exact bracket lower bounds mismatch (each by −22.50, the step). Between boundaries (any value strictly inside a bracket, e.g. 3,749.99 or 24,749.99) CP2 = official. The matrix leaves a theoretical gap (e.g. 3,749.995) that neither implementation resolves — not a practical issue for 2-decimal peso amounts.

---

## 2. PhilHealth

**(a) Official table** — `Home_Philhealth_Matrix.xlsx`, sheet `Philhealth`.
- `A2:C2`: salary ≤ 10,000 → premium rate 3%, monthly premium **300** (floor).
- `A3:C3`: 10,000.01 – 59,999.99 → 3%, premium "300 up to 1,800" (i.e. 3% of salary, between those bounds).
- `A4:C4`: 60,000 → 1,800 (ceiling).
- `A6` NOTE: *"Monthly premium contribution payments are equally shared between the employee and employer."* → **employee share = 50% of premium.**
- `A7:B10` SAMPLE: basic 25,000 → 3% = 750 → **Employee Share (50%) = 375** (`B10`).
- Official employee deduction used below: `0.5 × clamp(0.03 × salary, 300, 1800)`.

**(b) CP2 code** — `Dashboard.java:1294–1310`: `premiumRate = 0.03`; `≤10,000 → 300`; `≤59,999.99 → min(300 + (salary−10,000)·0.03, 1800)`; `else 1800`. **Returns `monthlyPremium` = the full 100% premium**, with the comment "Employee and employer share equally" **but no ÷2 is applied** (line ~1305). The caller (`Dashboard.java:1013–1023`) subtracts this full amount from the employee's pay.

**(c) OOP code** — `PhilHealthCalculator.java`: `PREMIUM_RATE 0.03`, `MIN 300`, `MAX 1800`, same piecewise logic, returns the full premium. Identical to CP2 (javadoc says "carried over 1:1 from CP2").

**Formula check:** the CP2/OOP formula `300 + 0.03·(s−10,000)` = `0.03·s` for s ≥ 10,000, so the *premium* formula **matches the matrix** (verified at 15,000 → 450, 20,000 → 600, 25,000 → 750 ✓ official 750).

**(d) Verdict per test input:**

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 0.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 500.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 1,000.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 1,500.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 2,500.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 5,000.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 10,000.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 15,000.00 | 225.00 | 450.00 | 450.00 | +225.00 | 🟠 DEVIATION |
| 20,000.00 | 300.00 | 600.00 | 600.00 | +300.00 | 🟠 DEVIATION |
| 25,000.00 | 375.00 | 750.00 | 750.00 | +375.00 | 🟠 DEVIATION |
| 29,750.00 | 446.25 | 892.50 | 892.50 | +446.25 | 🟠 DEVIATION |
| 30,000.00 | 450.00 | 900.00 | 900.00 | +450.00 | 🟠 DEVIATION |
| 40,000.00 | 600.00 | 1,200.00 | 1,200.00 | +600.00 | 🟠 DEVIATION |
| 50,000.00 | 750.00 | 1,500.00 | 1,500.00 | +750.00 | 🟠 DEVIATION |
| 90,000.00 | 900.00 | 1,800.00 | 1,800.00 | +900.00 | 🟠 DEVIATION |
| 200,000.00 | 900.00 | 1,800.00 | 1,800.00 | +900.00 | 🟠 DEVIATION |

Boundary points:

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 3,250.00 | 150.00 | 300.00 | 300.00 | +150.00 | 🟠 DEVIATION |
| 20,832.00 | 312.48 | 624.96 | 624.96 | +312.48 | 🟠 DEVIATION |
| 24,750.00 | 371.25 | 742.50 | 742.50 | +371.25 | 🟠 DEVIATION |
| 33,333.00 | 500.00 | 999.99 | 999.99 | +500.00 | 🟠 DEVIATION |

**(e) Mismatch count — 16 / 16 base inputs.** Every input with a nonzero premium deviates by exactly **50% of the premium** (CP2 deducts 2× the official employee share). Root cause is one missing `× 0.5`, not the formula.

---

## 3. Pag-IBIG

**(a) Official table** — `Home_PagIBIG_Matrix.xlsx`, sheet `Pag-ibig Contribution`.
- `A2:D2` "At least 1,000 to 1,500": employee 1% (`B2`), employer 2% (`C2`), total 3% (`D2`).
- `A3:D3` "Over 1,500": employee 2% (`B3`), employer 2% (`C3`), total 4% (`D3`).
- `A5` NOTE: *"The maximum contribution amount is 100."*
- **No row for salary < 1,000** (matrix silent) → those inputs are ❓ UNVERIFIED, not assumed.
- Official employee deduction used below: `min(employee_rate × salary, 100)`.

**(b) CP2 code** — `Dashboard.java:1312–1332`: `1000 ≤ s ≤ 1500 → employee 0.01 + employer 0.02`; `s > 1500 → employee 0.02 + employer 0.02`; `min(s × (employee+employer), 100)` → **returns the TOTAL (employee + employer) 3%/4% capped at 100**. Caller at `Dashboard.java:1014` subtracts this from the employee's pay → employer share is being deducted from the employee.

**(c) OOP code** — `PagIbigCalculator.java`: same tiers, `totalRate = employeeRate + employerRate`, `min(salary × totalRate, 100)`. Identical to CP2.

**(d) Verdict per test input:**

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 0.00 | — | 0.00 | 0.00 | — | ❓ UNVERIFIED |
| 500.00 | — | 0.00 | 0.00 | — | ❓ UNVERIFIED |
| 1,000.00 | 10.00 | 30.00 | 30.00 | +20.00 | 🟠 DEVIATION |
| 1,500.00 | 15.00 | 45.00 | 45.00 | +30.00 | 🟠 DEVIATION |
| 2,500.00 | 50.00 | 100.00 | 100.00 | +50.00 | 🟠 DEVIATION |
| 5,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 10,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 15,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 20,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 25,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 29,750.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 30,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 40,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 50,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 90,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |
| 200,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |

Boundary points (the band where CP2's 4% cap reaches 100 at 2,500 but the official 2% employee share does not reach 100 until 5,000):

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 2,500.00 | 50.00 | 100.00 | 100.00 | +50.00 | 🟠 DEVIATION |
| 3,250.00 | 65.00 | 100.00 | 100.00 | +35.00 | 🟠 DEVIATION |
| 3,750.00 | 75.00 | 100.00 | 100.00 | +25.00 | 🟠 DEVIATION |
| 4,250.00 | 85.00 | 100.00 | 100.00 | +15.00 | 🟠 DEVIATION |
| 4,750.00 | 95.00 | 100.00 | 100.00 | +5.00 | 🟠 DEVIATION |
| 5,000.00 | 100.00 | 100.00 | 100.00 | +0.00 | ✅ MATCH |

**(e) Mismatch count — 3 / 16** base inputs (1,000; 1,500; 2,500 — all where 4%/3% is below the 100 cap but 1%/2% is not). **2 UNVERIFIED** (0, 500: matrix has no row below 1,000). For 5,000 and up both sides hit the 100 cap and match, which is why the base count looks small; the deviation is real for every salary 1,000–4,999.99 (5 boundary points listed above).

---

## 4. Withholding Tax

**(a) Official table** — `Home_WithholdingTax_Matrix.xlsx`, sheet `Sheet1`.
- `A2:B2` ≤ 20,832 → 0.
- `A3:B3` 20,833 to < 33,333 → **20% in excess of 20,833**.
- `A4:B4` 33,333 to < 66,667 → 2,500 + 25% in excess of 33,333.
- `A5:B5` 66,667 to < 166,667 → 10,833 + 30% in excess of 66,667.
- `A6:B6` 166,667 to < 666,667 → 40,833.33 + 32% in excess of 166,667.
- `A7:B7` ≥ 666,667 → 200,833.33 + 35% in excess of 666,667.
- `A9` NOTE: *"Withholding tax is calculated after applying deductions."* — CP2 does this (`Dashboard.java:1016–1023`).
- SAMPLE `A10:B21`: taxable 23,400 → `(23,400 − 20,833) × 20% = 513.40` (`B20`). Confirms the **20,833** excess base.

**(b) CP2 code** — `Dashboard.java:1334–1352`: `≤20,832 → 0`; `≤33,333 → (s − 20,832) × 0.20`; `≤66,667 → 2,500 + (s−33,333)·0.25`; `≤166,667 → 10,833 + (s−66,667)·0.30`; `≤666,667 → 40,833.33 + (s−166,667)·0.32`; else `200,833.33 + (s−666,667)·0.35`.

**(c) OOP code** — `WithholdingTaxCalculator.java`: `UPPER_BOUNDS (20832, 33333, 66667, 166667, 666667)`, `LOWER_BOUNDS (0, 20832, 33333, 66667, 166667)`; bracket 1 uses base 20,832, not 20,833. Same behavior as CP2.

**Deviation:** bracket 1 uses the excess base **20,832** where the matrix (and its SAMPLE) uses **20,833**. Effect: CP2/OOP are **+₱0.20** higher than official for every taxable income in [20,833, 33,333]. Brackets 2–6 match exactly (verified at 40,000 → 4,166.75; 50,000 → 6,666.75; 90,000 → 17,832.90; 200,000 → 51,499.89 — all equal the matrix formula).

**(d) Verdict per test input** (taxable income = the function input; the matrix's pipeline subtracts SSS+PhilHealth+Pag-IBIG first, which CP2 also does — not re-tested here):

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 0.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 500.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 1,000.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 1,500.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 2,500.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 5,000.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 10,000.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 15,000.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 20,000.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 25,000.00 | 833.40 | 833.60 | 833.60 | +0.20 | 🟠 DEVIATION |
| 29,750.00 | 1,783.40 | 1,783.60 | 1,783.60 | +0.20 | 🟠 DEVIATION |
| 30,000.00 | 1,833.40 | 1,833.60 | 1,833.60 | +0.20 | 🟠 DEVIATION |
| 40,000.00 | 4,166.75 | 4,166.75 | 4,166.75 | +0.00 | ✅ MATCH |
| 50,000.00 | 6,666.75 | 6,666.75 | 6,666.75 | +0.00 | ✅ MATCH |
| 90,000.00 | 17,832.90 | 17,832.90 | 17,832.90 | +0.00 | ✅ MATCH |
| 200,000.00 | 51,499.89 | 51,499.89 | 51,499.89 | +0.00 | ✅ MATCH |

Boundary points:

| Input (PHP) | Official (matrix) | CP2 | OOP | Δ CP2−Official | Verdict |
|---:|---:|---:|---:|---:|---|
| 20,832.00 | 0.00 | 0.00 | 0.00 | +0.00 | ✅ MATCH |
| 20,833.00 | 0.00 | 0.20 | 0.20 | +0.20 | 🟠 DEVIATION |
| 24,750.00 | 783.40 | 783.60 | 783.60 | +0.20 | 🟠 DEVIATION |
| 33,333.00 | 2,500.00 | 2,500.20 | 2,500.20 | +0.20 | 🟠 DEVIATION |

**(e) Mismatch count — 3 / 16** base inputs (25,000; 29,750; 30,000 — all bracket 1 with a non-zero tax). Boundary points at 20,833 / 24,750 / 33,333 show the same +0.20 offset.

---

## Summary of the 4 open questions

**1. SSS table match?** **YES for the bracket amounts; NO at exact boundaries.** Official `D3:D46` = CP2/OOP at every non-boundary value (16/16 base inputs). At the 44 exact lower bounds (3,250; 3,750; …; 24,750) CP2 uses the lower bracket (`<=`), the matrix uses the upper one (`A3 = 3,250 … 'to 3,749.99'`). Evidence: §1(d)/(e).

**2. PhilHealth formula?** **Formula YES, employee share NO.** Premium `max(300, 3%·s)` capped at 1,800 matches the matrix premium exactly. But the matrix says the premium is *equally shared* (`A6`) and the SAMPLE `B10` = 375 = 50% of 750. CP2 deducts 100% (750). This is the cause of Issue 2 — the hypothesis "300 + 3% of excess" was mathematically the same formula, so the real gap is the 50%.

**3. Pag-IBIG employer share?** **YES — CP2 includes the employer share in the employee deduction.** CP2 adds 2% employer to the 1%/2% employee rate (total 3%/4%) and deducts the whole thing from the employee. Official employee share is 1% / 2%. Evidence: `Home_PagIBIG_Matrix.xlsx` `B2:B3` vs `D2:D3`; `Dashboard.java:1014`. **Note: this is the opposite of the Issue 3 hypothesis** — CP2 is *higher* than official, not lower.

**4. Tax brackets?** **NO, by ₱0.20 in one bracket.** Brackets 2–6 match. Bracket 1 uses 20,832 instead of 20,833 as the excess base (matrix `A3` and SAMPLE `B20`/`B21`). Evidence: §4 "Deviation" and the 513.40 sample.

---

## Recommended fixes

| # | Deviation | File / method | Exact fix (from the matrix) |
|---|---|---|---|
| 1 | PhilHealth employee share = 100% instead of 50% | `oop/src/motorphemployee/oop/PhilHealthCalculator.java` → `calculate()`; CP2: `Dashboard.java:1294` & `MainMenu.java:1378` | Return `0.5 * min(max(0.03 * s, 300), 1800)` (employee share). Keep the premium formula. Update the `calculate()` javadoc. Re-run the 25/25 parity check **after** deciding whether CP2 is also changed. |
| 2 | Pag-IBIG deducts employer share | `PagIbigCalculator.java` → `calculate()`; CP2: `Dashboard.java:1312` & `MainMenu.java:1396` | Use employee rate only: `min(s × (s<=1500 ? 0.01 : 0.02), 100)`. Add a note that s < 1,000 is not in the matrix (decide 0 vs. error). |
| 3 | WHT bracket 1 excess base 20,832 vs 20,833 | `WithholdingTaxCalculator.java` → `calculate()`, `LOWER_BOUNDS[1]`; CP2: `Dashboard.java:1334` & `MainMenu.java:1418` | Change bracket-1 to `(income − 20,833) × 0.20` and the `UPPER_BOUNDS[0]` boundary to `20,832` (keep ≤ 20,832 → 0). |
| 4 | SSS exact-boundary side (≤ vs ≥) | `SSSCalculator.java` → `calculate()`; CP2 `Dashboard.java:1195` & `MainMenu.java:1279` | Change to lower-bound inclusive (`salary >= LOWER_BOUNDS[i]` going down) **only if** the official matrix is the authority — decide with Paul. Impact: ₱22.50 at each of 44 exact bracket values; no impact on any other value. |

**Decision for Paul (do not apply without you):** whether CP2 should be corrected too (CP2 is the current behavior and the 25/25 parity report is based on it). Right now the OOP matches CP2, so fixing OOP alone would break parity.

## Test inputs used (reproducible)

- Base set (required by task): 0, 500, 1,000, 1,500, 2,500, 5,000, 10,000, 15,000, 20,000, 25,000, 29,750, 30,000, 40,000, 50,000, 90,000, 200,000.
- Boundary additions (to expose the findings): SSS 3,249.99 / 3,250 / 3,749.99 / 3,750 / 24,750 / 24,750.01 and all 44 SSS lower bounds; PhilHealth 3,250 / 20,832 / 24,750 / 33,333; Pag-IBIG 2,500 / 3,250 / 3,750 / 4,250 / 4,750 / 5,000; WHT 20,832 / 20,833 / 24,750 / 33,333.
- Harness: `cache\scratch\q4\Q4Harness.java` + `CP2.java` (verbatim `Dashboard.java:1195–1356`); raw output `cache\scratch\q4\run.txt` (66 inputs; CP2 == OOP on all 66).

## Limits / UNVERIFIED

- ❓ Pag-IBIG for salary < 1,000: matrix has no row. CP2/OOP return 0; not verified against any official value.
- ❓ Pag-IBIG cap: matrix says "maximum contribution amount is 100" without saying whether it caps the employee share or the total. Employee-share interpretation used for the official column; if the cap applies to the total, the base-table verdict stays the same (both reach 100 above 5,000).
- ❓ SSS values between bracket bounds at fractional centavos (e.g. 3,749.995) are not defined by the matrix.
- The WHT test used the **function input** as taxable income. The full pipeline (SSS+PH+PI subtracted first) was not re-tested here; CP2/OOP do this in the caller at `Dashboard.java:1016–1023`.
