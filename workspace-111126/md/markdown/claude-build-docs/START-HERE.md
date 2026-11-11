# MotorPH — the short guide

You don't need the 1,500-line version to understand this app.
**Read this file. Skim section 5. Look things up in the big file only when you want.**

---

## 1. What this app is, in 5 lines

1. It's a payroll app for MotorPH: people, timecards, leave, and payslips.
2. You sign in as **one of five kinds of account** — System Admin, HR, Payroll Staff, Supervisor, or
   Employee — and the server decides what you may see from your employee number. A supervisor sees
   their team, HR sees everyone, an employee sees themselves.
3. It calculates a payslip **exactly like MotorPH's own sample payslip** — same numbers to the centavo.
4. It saves each payroll run permanently. You can reprint it later.
5. The **34 people are the real rows from MotorPH's employee file**. Only the sample timecards,
   leave and ticket records are invented, and each is labelled where it appears.

---

## 2. The 3 ideas that explain everything else

**Idea 1 — The browser never does maths.**
Every number on screen came from the server. The browser only prints it.
So if a number is wrong, the bug is on the server. Always.

**Idea 2 — The server decides, from who you are.**
Two layers, and they do different jobs.

*Firebase's rules* guarantee you can only reach your own user's documents — your `uid` comes from your
token, so you cannot ask for someone else's storage even by editing the request.

*The role matrix* (`Policy.java`) decides what your employee number may see **within** that storage:
a supervisor gets their direct reports, HR gets the roster, an employee gets themselves. The server
resolves your role from your employee number — the browser never sends a role, and it never sends a
permission. Directory replies are filtered server-side, so a response cannot contain a row you may not
read, and a refusal comes back as a real `403` with a reason.

**Idea 3 — Money is never a float.**
Decimals are stored as text (`"26750.00"`) and calculated exactly. Rounding happens in exactly one
place: `Money.round()` — 2 decimals, round half up. That's how the centavos match.

---

## 3. The app has 8 screens

Left nav, top to bottom. Only the first and last matter much:

| Screen | What it does | Notes |
|---|---|---|
| **Overview** | Dashboard numbers | Counts only |
| **Employees** | List, search, open, create, edit | Start here to see who exists |
| **Attendance** | Raw timecard rows for one person | Read-only |
| **Requests** | Leave + tickets | Submit and cancel only |
| **Team** | Read-only roster | No reporting lines |
| **HR desk** | Create/edit employee fields | Same as Employees, different layout |
| **Reports** | Who has how many attendance rows | Coverage, not pay |
| **Payroll** | **The main event** | See §5 |

---

## 4. One payslip, start to finish

This is the whole app in one path.

```
You pick an employee            →  salary comes from their record (not typed in)
        ↓
You enter or fetch days worked  →  "Use stored timesheet days" reads their timecard
        ↓
Server calculates               →  daily rate, gross, SSS, PhilHealth, Pag-IBIG, tax
        ↓
You see the payslip             →  nothing saved yet
        ↓
You click "Save as payroll run" →  frozen forever, with the rules version stamped on it
        ↓
Later you click "Open"          →  it prints the STORED numbers, it does not recalculate
```

**Why "frozen" matters:** tax tables change. If 2027's tables change, last year's payslip must still
print what was actually paid. That's why saved runs can't be edited or deleted.

---

## 5. The payroll numbers (the part worth knowing)

**Inputs:** monthly salary, days worked, allowances, overtime hours, night-diff hours.

**The 6 formulas:**

```
daily rate    = monthly salary ÷ 20
hourly rate   = monthly salary ÷ 168
gross         = daily rate × days worked   (+ overtime + night diff)
SSS           = 5% of salary, capped between ₱5,000 and ₱35,000
PhilHealth    = 5% of salary, split in half
Pag-IBIG      = 2% of salary, capped at ₱10,000
tax           = BIR table, applied to (salary − the three contributions above)
take home     = gross + allowances − (SSS + PhilHealth + Pag-IBIG + tax)
```

**Worked example** (this is MotorPH's real sample — the test that must always pass):

```
Salary ₱53,500, 10 days, semi-monthly
  gross        26,750.00
  SSS           1,750.00
  PhilHealth    1,337.50
  Pag-IBIG        200.00
  tax           5,250.83
  ─────────────────────
  take home    22,711.67   ← must be exactly this
```

If a change makes this number move, the change is wrong. A test enforces it.

---

## 6. Where things are (when you want to look)

```
web/index.html      all 8 screens
web/app.js          all the clicking and loading
web/payroll.js      how the payslip is laid out

src/main/java/ph/motorph/web/
  AppServer.java        the routes (what URL does what)
  PayrollService.java   ★ the maths
  PayrollRuleset.java   ★ the tax/contribution tables
  TimesheetService.java how timecards become days + hours
  Firestore*.java       saving and loading
```

★ = if you read two files, read these.

---

## 6b. What the sweldongpinoy test found (2026-11-06)

We ran MotorPH's 34 real employees through both systems:

- **SSS, PhilHealth, Pag-IBIG: identical on all 34**, to the centavo.
- **Tax: they caught a bug in ours.** Three bracket bases were 13 centavos too high
  (8,541.80 instead of 102,500÷12 = 8,541.67). Anyone above ~₱90,000 basic was overcharged 13c.
  **Fixed**, with tests.
- **Two places where we are right and they are wrong:** Pag-IBIG at exactly ₱1,500 (they use 2%,
  the circular says 1%), and their tax curve above ~₱100,000 doesn't match any published band.
- **Our SSS rounding was wrong and is now fixed.** We rounded the salary credit *up* to the next
  ₱500; SSS publishes ₱500-wide *brackets* (5,250.00–5,749.99 → 5,500.00). The two agree only in
  the upper half of each bracket, so ₱10,001 was credited ₱10,500 instead of ₱10,000. Now banded.
  **No MotorPH employee was affected** — all 34 salaries sit above their bracket midpoint.
- **Their `days` and `schedule` settings do nothing** — output is identical for 15, 20, 22 days.
- **There is no public API**, just an undocumented form post. We added an optional
  "compare with sweldongpinoy.com" checkbox that calls it **through our server**, side by side.
  If their site is down, the payslip is unaffected.

## 7. The 8 questions from the review, in one line each

| Question | Answer |
|---|---|
| ₱150/₱160/₱200 hourly rates? | Not used. Rate = salary ÷ 168. Don't add flat rates. |
| Leave credits 5/5/3? | Fine as demo policy. Keep out of payroll. |
| 13th month + ₱90,000 tax? | No 13th month exists. It's a separate annual feature. |
| Pag-IBIG at exactly ₱1,500? | **Already follows the law (1%).** Nothing to do. |
| Overtime — what counts? | Past **8 hours**, not past 17:00. 125% on normal days. |
| Paid leave in payroll? | It's a paid **day**, not paid hours. Blocked — no approvals yet. |
| Log in before 08:00 = extra pay? | **It does today, and it shouldn't.** This is a real bug. |
| Approve your own leave? | Never. |

---

## 8. What's missing (don't go looking for it)

- No attendance clock-in/out screen (records come from the CSV seed only)
- No automatic "days worked" — you confirm the number
- No overtime/night-diff **hours** from punches (you type the hours)
- No leave pay, no holidays, no 13th month
- No approvals, no roles, no managers
- No multi-employee run in the UI

The app **says** these are missing instead of pretending. That's deliberate. If a screen implies a
feature works when it doesn't, that's a bug worth reporting.

---

## 9. Three sentences that are safe to say out loud

1. "Payroll reproduces MotorPH's sample payslip to the centavo, with tests at every layer."
2. "Every saved run is permanent and reprints from stored numbers."
3. "The roster is MotorPH's own employee file; sample timecards and workflows are labelled, and gaps are labelled too."

---

## 10. When you need more

| You want | Open |
|---|---|
| **Where your CSV data goes, field by field** | `docs/where-your-csv-data-goes.md` |
| **The five questions: accounts, periods, rate changes, time/leave/holiday** | `docs/answers-five-questions.md` |
| Every screen and button, in order | this file, §3–5 |
| Exactly what one function does | `docs/full-pseudocode-inventory.md` §9 (search the function name) |
| The URL/JSON shapes | `docs/api-contract.md` |
| Why the tax numbers are what they are | `docs/payroll-ruleset-and-golden-case.md` |
| The 8 questions, with evidence | `docs/statutory-and-policy-questions.md` |
| Why our numbers match sweldongpinoy (and where they don't) | `docs/sweldongpinoy-crosscheck.md` |
| Divisions (÷20, ÷168), 2026 tables, API vs local | `docs/divisors-2026-tables-and-api-architecture.md` |
| What Firebase stores, field by field | `docs/firebase-data-inventory.md` |
| **Where the build stands now** (start here if you are checking my work) | `docs/where-this-build-stands.md` |
| **The 17 modules vs this build** — 0 complete, 8 partial, 9 missing | `docs/gap-analysis-vs-design-document.md` |
| **Ticketing** — what frappe/opensupports/osTicket do, and the design for ours | `docs/ticketing-design.md` |
| Role model: the five account types and the 12x5 matrix | `docs/role-model-and-access-matrix.md` |
| What changed and when | `docs/build-status-no-local-no-temp.md` |

**Tip:** the big file has a table of contents at the top. Search it (Ctrl-F) for a word you saw on
screen — a button label, an error message, a field name — and it will land you in the right section.
