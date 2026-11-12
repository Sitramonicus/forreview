# c_calc

Payroll maths. Everything that turns attendance into pesos lives here.

- `CutoffPayrollService` — one cutoff's gross. Day types: ordinary, rest day, special
  non-working, regular holiday. A *worked* holiday pays `workedUnits × (multiplier − 1)` on top of
  base, not the full multiplier. I got this wrong once and every holiday came out double.
- `MonthPayroll` — the two cutoffs of a month. `grossForMonth` hands back a `List<CutoffPay>`,
  so `.get(i)`. There is no `getCutoffs()`.
- `StatutoryRates` / `StatutoryCalculator2026` — SSS, PhilHealth, Pag-IBIG, BIR. Rates carry an
  effective month; a month uses the newest version on or before it. Don't backdate a 2026 rate
  onto 2023 and call it correct.
- Hourly is `monthlyBasic ÷ 20 ÷ 8` (daily ÷ 8). The ÷168 thing is the old engine, it's dead.
- `SalaryHistory.monthlyBasic(...)` returns `Optional`. `.get()` it or the compiler will not save you.

Wages are cents in `long` everywhere. No `double`, no `float`, no `BigDecimal` in the hot path.

## member notation

`ClockService` converted: `qr_mShifts`, `qr_mOnsite`, `qr_mCodes`, `qr_mExpires`, every public method's
parameters. `BookSlipCorrections` (was `SampleSlipCorrections`) — "Sample" was placeholder language.
Both call sites follow: `AppServer` and `GoldenPayslipTest`.

`ClockService` is the one class in here that holds mutable session-ish state per employee, so it is
touched by the QrTest and the browser flow at once. Run `./test.sh`, not just the Java suites.

cut too deep -- 2026-11-08
tried to delete two unused private methods (wholeDays, partOf) with a brace-counter that also ate the
blank line above them, so it swallowed the next method's javadoc too. money() vanished and the build
said "cannot find symbol" three times.
restored money(hourlyRate, hourUnits) = rate * hours, same shape as paidDays. golden payslip still
22,711.67 so it is right.
lesson: the rfind('\n\n') start-of-method trick is not safe when methods have no javadoc between them.
count braces from the signature and leave the preceding whitespace alone.
