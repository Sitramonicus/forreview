# CP2 reference results

These files are the output of the **unmodified CP2 code** (commit `2eedf04`,
`SalaryComputationModule.computeAllEmployees` and `computeFullPayrollSummary`)
for all 34 employees in all 14 cutoffs in `data/attendance.csv`
(June 1-15, 2024 to December 16-31, 2024).

| File | Contents |
|---|---|
| `cp2_payroll_results.csv` | 476 rows (34 employees x 14 cutoffs), same columns and `%.2f` format as CP2's Save Results, plus a Cutoff column |
| `cp2_payroll_summary.csv` | 14 rows, CP2's Generate Summary values per cutoff |

`Cp2ComparisonTest` runs the refactored OOP backend on the same data and
compares every value against these files.
