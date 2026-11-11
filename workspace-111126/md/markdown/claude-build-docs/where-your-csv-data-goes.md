# Where your CSV data goes

Short answer: **the 34 people in your employee file are the app's people.** Nothing is invented.
This page says exactly which file, which screen, and which field.

---

## 1. The four files you gave me

| File | What is in it | Where it is used |
|---|---|---|
| `Copy of MotorPH_Employee Data - Employee Details.csv` | 34 employees, 19 columns each | **Every screen.** This is the roster. |
| `Copy of MotorPH Sample Timecard - MotorPH Payslip.csv` | 1 timecard, employee 10015, December 2023 | Attendance screen + timesheet day counter |
| `Copy of MotorPH Payslip - MotorPH Payslip.csv` | 1 payslip, employee 10015, 2–15 Jan 2026 | The golden test (the pay figures are reproduced exactly) |
| `Copy of MotorPH Inventory Data - March 2023 Inventory Data.csv` | 48 stock items | **Not used.** It is not payroll data. |

Files live in `/home/user/uploads/` in this workspace.

---

## 2. How the employee file becomes the app's data

It is not typed in by hand. A script reads the CSV and writes the Java fixture:

```
uploads/Copy of MotorPH_Employee Data - Employee Details.csv
        ↓  python3 tools/generate_fixtures.py
src/main/java/ph/motorph/web/InMemoryEmployeeStore.java
        ↓  the API serves it
/api/employees  →  screens
```

Run `python3 tools/generate_fixtures.py` again after the CSV changes. It fails loudly on a new
position rather than guessing a department.

### What is taken straight from your file

| CSV column | App field | Notes |
|---|---|---|
| `Employee #` | employee ID | 10001–10034, exactly as supplied |
| `First Name` + `Last Name` | name | |
| `Position` | role | |
| `Basic Salary` | monthly basic pay | |
| `Rice Subsidy` | rice subsidy | |
| `Phone Allowance` | phone allowance | |
| `Clothing Allowance` | clothing allowance | |
| `Address` | location | |

### What is derived, because the CSV has no such column

| App field | How it is derived | Honest label |
|---|---|---|
| Department | from the position (18 positions → 10 departments) | **Assumption.** Your file has no department column. |
| Employment status | every row is `Active` | Your `Status` column is Regular/Probationary — the *employment type*, not whether someone is active. |

The 18 positions are listed one by one in `tools/generate_fixtures.py`. If your file gains a new
position, the generator stops and asks rather than guessing.

---

## 3. Which screen shows what

| Screen (left nav) | Shows |
|---|---|
| **Employees** | The 34 people, searchable by ID, name, department, role, location |
| **My Team** | The same roster, read-only |
| **HR → Employee records** | The same roster, with **View** and **Edit** |
| **HR → Create employee record** | Adds a person; the server issues the next number (10035, 10036, …) |
| **Payroll** | Pick a person, enter a period, compute the payslip |
| **Attendance** | Pick a person, see their stored timecard rows |
| **Reports** | Attendance coverage for all 34; directory counts |

---

## 4. The one place your two files disagree

Employee **10015 (Fredrick Romualdez)**:

| Field | Your payslip CSV says | Your employee file says |
|---|---|---|
| Phone Allowance | ₱2,000.00 | ₱1,000.00 |

Everything else agrees. The payroll reads the **employee file**, so:

- SSS, PhilHealth, Pag-IBIG, and withholding tax **match your payslip to the centavo** (₱1,750.00 / ₱1,337.50 / ₱200.00 / ₱5,250.83).
- Benefits come to ₱3,500.00 instead of ₱4,500.00, so take-home is **₱21,711.67** instead of ₱22,711.67.

This is **disclosed on the Payroll screen** under the form whenever 10015 is selected. It is a
question for MotorPH, not something to quietly reconcile:

> Which is right for 10015 — ₱1,000 phone allowance, or ₱2,000?

The golden test still proves the engine reproduces **₱22,711.67** when given the payslip's own
figures. So the calculation is right; only the source value differs.

---

## 5. What is still not from your files

| Thing | Why | Where it shows |
|---|---|---|
| Schedule assignments | Your employee file has no schedule column | Attendance coverage shows "No mapping" for 30 of 34 people |
| October 2026 attendance rows | Invented sample rows for the attendance screens | Only for 10001, 10002, 10003 (small), and 10015 (your December 2023 timecard) |
| Leave and ticket records | No such file was supplied | Empty until someone creates one |
| Holiday pay, leave deductions, clock-in | Not calculated anywhere yet | Stated on screen where relevant |
