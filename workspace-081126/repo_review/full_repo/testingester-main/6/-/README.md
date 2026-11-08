# MotorPH Payroll System: OOP Refactor

MO-IT110 Object-Oriented Programming, Milestone 2 (S2101, Tan, F.D.)

This repository refactors the CP2 MotorPH Java Swing payroll app (commit `2eedf04`)
into the object-oriented design approved in Milestone 1. All payroll logic now lives
in backend classes; the Swing frames only collect input and display results.

## How to run

1. Open this folder in NetBeans (File > Open Project). JDK 25.
2. Run the project (main class `motorph.MotorPH`).
3. Log in:

| Username | Password | Opens |
|---|---|---|
| `payroll_staff` | `motorph123` | Dashboard: employees, payroll (one or all employees), summary, save |
| `employee` | `motorph123` | Employee lookup (number, name, birthday only) |

CP2's password `12345` was replaced because the MS1 design requires at least 8 characters.

## Structure

```
src/motorph/
  MotorPH.java                 entry point (opens LoginFrame)
  model/      Employee (abstract), RegularEmployee, ProbationaryEmployee,
              AttendanceRecord, Payslip
  deduction/  Deduction (abstract), SSSDeduction, PhilHealthDeduction,
              PagIbigDeduction, WithholdingTaxDeduction
  user/       User (abstract), EmployeeUser, PayrollStaff
  service/    PayrollProcessor, CSVDataHandler
  ui/         LoginFrame, DashboardFrame, EmployeeFrame, EmployeeLookupFrame,
              SalaryComputationFrame, UIStyleHelper (view layer only)
data/         employees.csv, attendance.csv, payroll_results.csv
test/         plain-Java tests (no JUnit) and CP2 reference results
```

## Payroll flow

```
CSVDataHandler.loadEmployees() / loadAttendance()
      -> Employee (Regular/Probationary) + AttendanceRecord objects
      -> PayrollProcessor (constructed with the Deduction objects)
           hours -> gross -> deductions -> net
      -> Payslip (immutable)
      -> GUI table / summary / CSVDataHandler.savePayrollResults()
```

## Payroll rules

Salary tables, contribution thresholds, tax brackets, and cutoff handling are taken
from the CP2 implementation, not separately verified against a current MotorPH or
government specification.

- **Hours:** a login from 8:00 to 8:10 counts as 8:00; a logout after 17:00 counts as 17:00; never negative.
- **Gross pay:** hours worked x hourly rate, per cutoff.
- **1st cutoff (days 1-15):** no deductions; net pay = gross pay.
- **2nd cutoff (day 16 to month end):** the month's deductions are taken once, on the
  combined gross of both cutoffs. Order: SSS, PhilHealth, Pag-IBIG on monthly gross;
  then withholding tax on taxable income (monthly gross minus those three).
  Net pay = 2nd cutoff gross minus the month's deductions, never below 0.
- **Allowances:** `computeAllowances()` is overridden per subclass (Regular: rice +
  phone + clothing; Probationary: 0). They are listed once per month on the
  2nd-cutoff payslip and are **not** added to net pay, matching CP2.
  Probationary = 0 is a project assumption: employees.csv lists allowance amounts for
  probationary staff, but CP2 never paid allowances to anyone, so the rule changes no totals.

## Money and rounding

Money uses `BigDecimal`. Attendance is recorded to the minute, so gross pay is
computed as hourly rate x minutes / 60 with the division done last, which keeps
half-centavo amounts exact (535.71 x 4,490 min / 60 = 40,088.965). Amounts keep full
precision through the calculation and are rounded to 2 decimals with
`RoundingMode.HALF_UP` only when displayed or saved, the same point where CP2 rounded.

## Tests

Right-click `test/motorph/AllTests.java` > Run File (runs from the project folder).
Each test class can also be run on its own.

| Test | Checks |
|---|---|
| `EmployeeTest` | setter validation, `computeGrossPay`, overridden methods through `Employee` references |
| `AttendanceRecordTest` | grace period, logout cap, cutoff labels incl. leap years |
| `CSVDataHandlerTest` | 34 employees (23 Regular, 11 Probationary), 5,168 attendance records, save round trip on a temp copy |
| `DeductionTest` | each deduction subclass at its bracket edges; tax on taxable income |
| `PayrollProcessorTest` | cutoff rule, deduction totals, immutability, summary, saving results |
| `TwoCutoffExample` | itemized June 2024 payslips for one Regular (10002) and one Probationary (10021) employee |
| `Cp2ComparisonTest` | all 34 employees x 14 cutoffs against the original CP2 output |
| `UserTest` | login and the full action-to-role matrix |

Current result: 167 checks, 0 failures. `Cp2ComparisonTest`: 473 of 476 rows and
14 of 14 cutoff summaries identical to CP2. The other 3 rows differ by 0.01 because
their exact gross pay ends in a half centavo (for example 10,379.575) and CP2's
`double` arithmetic stored it just below the half and rounded down; `BigDecimal`
rounds it up correctly.
