# The five questions, answered

You asked five things. This is the straight answer to each, with the exact file and screen.

**First: why were there demo people?**
Because the app was built before your files were wired in, and the four people I invented were
leftovers from that stage. That was wrong of me to leave sitting in front of you.
**They are gone.** The roster is now your 34 rows, 10001–10034, read straight from your CSV.

Run `python3 tools/generate_fixtures.py` to rebuild it from the file at any time.

---

## 1. What account types do we have?

**None. There is only one kind of account, and it sees only its own data.**

| Thing | Reality |
|---|---|
| Who can sign in | Any Firebase account (email + password) |
| What an account can see | Only records stored under its own user ID |
| Roles (employee / supervisor / HR / payroll / admin) | **Not stored anywhere.** No role field, no `members` collection, no permission claims |
| Can an HR user see all 34 people? | **No.** Every signed-in user sees the same 34-person roster copy inside **their own** sandbox |
| Where this is enforced | `docs/firestore.rules` — every path is `users/{uid}/…` |

So today: **one account type, one person's view.** The five-role model you need is written up as a
proposal in `docs/role-model-and-access-matrix.md`, but nothing in it is built.

---

## 2. Where are the payroll period dates set, and how are they converted?

### Where they are set

**Screen:** Payroll → the **New payslip** panel.

| Field | On screen | Question it answers |
|---|---|---|
| Pay period | `PAY PERIOD` dropdown | Monthly or Semi-monthly |
| Start | `PERIOD START` | First day being paid |
| End | `PERIOD END` | Last day being paid |
| Days worked | `DAYS WORKED` | How many days are paid |

Nothing is stored. The dates are read from those three boxes each time you press
**Compute payslip**. There is **no calendar of 1–15 / 16–end-of-month cutoffs** — you type the dates.

### How they become a calculation

```
PERIOD START  →  which year's tax and contribution tables apply
                 (PayrollRuleset.forYear).  A 2027 start date refuses to run,
                 because no 2027 table has been supplied.

PAY PERIOD    →  which tax table column is used (monthly ÷12, semi-monthly ÷24)

DAYS WORKED   →  gross = (monthly basic ÷ 20) × days worked
```

### How they are converted for sweldongpinoy

Only when you tick **Compare with sweldongpinoy.com**.

| Ours | Theirs |
|---|---|
| Pay period Monthly, days = 20 | `pay_period=Monthly`, basic = monthly salary |
| Pay period Semi-monthly | **Not compared.** Their semi-monthly mode doubles what it is given, so it prices a whole month. Comparing it with one of our cutoffs would show a fake difference |
| Days worked < 20 in a month | **Not compared.** Same reason — we pay part of a month, they always pay a full one |

When a comparison *is* like for like, the panel shows both sides line by line. With ₱53,500 they
agree on all six lines and net ₱49,461.67 each.

**Files:** `web/app.js` (`payrollFormInput`), `AppServer.java` (`periodYear`, `comparableToReference`,
`referenceBasic`), `SweldongPinoyClient.java` (`referencePeriod`).

---

## 3. If an employee's rates change, where can that be changed?

**Screen:** **HR → Employee records →** find the person **→ Edit**.

The form now has these boxes, and payroll reads them directly:

| Box | Feeds |
|---|---|
| Monthly basic pay | Daily rate, gross, all contributions, tax |
| Rice subsidy | Benefits |
| Phone allowance | Benefits |
| Clothing allowance | Benefits |

**Two things to know, and both are honest limitations:**

1. **There is no salary history.** Change the number and every *past* calculation that is
   recomputed uses the new number. Only **saved payroll runs** keep their old figures — they were
   frozen when you saved them, with the ruleset stamped on them.
2. **There is no approval trail.** Anyone signed in can edit, because there are no roles yet (§1).

The same limitation is printed on the HR screen itself.

**Files:** `web/index.html` (the pay inputs), `web/app.js` (`editSyntheticEmployee`), `Employee.java`
(`withPay`).

---

## 4. Where do we handle time in/out, leaves, holiday pay?

**Short answer: none of those are calculated. Anywhere.** Here is exactly what each one does today.

| Topic | What exists | What does NOT exist |
|---|---|---|
| **Time in / out** | The Attendance screen lists stored timecard rows read from your December 2023 file. The Payroll screen can count them into *days worked* via **Use stored timesheet days** | No clock-in/out screen. No way to add a punch. No lateness, undertime, or overtime derived from punches |
| **Days worked** | You type it, or copy it from the stored timecard | It is never inferred silently |
| **Overtime hours** | You type the hours; paid at 125% of the hourly rate | Not derived from punches. No holiday, rest-day, or night-shift multipliers beyond night differential |
| **Night differential** | You type the hours; 10% premium | Not derived from punches |
| **Leaves** | Requests screen: submit, list, cancel. Status is only Pending / Cancelled | **No leave pay, no leave deduction, no balance, no approval.** A leave never touches a payslip |
| **Holidays** | Your timecard's "Holiday" remarks are stored and displayed | **Not read by payroll.** No regular-holiday or special-holiday premium exists in the ruleset |
| **13th month** | Nothing | Separate annual feature, not built |

The engine only knows: `daily rate`, `hourly rate`, `125% overtime`, `10% night differential`, and
the four statutory contributions plus withholding tax.

**Files:** `InMemoryAttendanceStore.java` (the stored rows), `TimesheetService.java` (day counting),
`PayrollRuleset.java` (the only premiums that exist), `WorkflowService.java` (leave and tickets).

---

## 5. Where is each of these in the code?

| You want to see | Open |
|---|---|
| The 34 people | `src/main/java/ph/motorph/web/InMemoryEmployeeStore.java` (generated) |
| The generator | `tools/generate_fixtures.py` |
| The pay maths | `PayrollService.java` |
| The tax and contribution tables | `PayrollRuleset.java` |
| The stored timecard rows | `InMemoryAttendanceStore.java` |
| One payslip, end to end | `docs/START-HERE.md` §4 |
| Field-by-field mapping of your CSV | `docs/where-your-csv-data-goes.md` |
