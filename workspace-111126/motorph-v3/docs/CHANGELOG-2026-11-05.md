> Historical record: parity claims below reflect the implementation notes at that time. Raw external responses were not retained; current provenance and corrected limits are in [data-provenance.md](a_doc/data-provenance.md) and [current-status.md](a_doc/current-status.md).

# Change log — 2026-11-05

Everything added or changed in this session, in the order it was done. All data is synthetic (DEMO employees only).

## Quick start (local demo mode, no Firebase)

- Run from Git Bash in `motorph-ms2`: `./run.sh` (Firebase mode, port 8080) or use the `motorph-local` preview config (local demo, port 8081).
- Local demo mode is needed to see leave approval (Firebase mode has no reviewer role).
- Tests: `./test.sh` (Java), `npm run test:web` (browser logic), `npm run test:e2e` (full browser flow), `npm run build`.

## 1. Setup on this Windows laptop

- Installed npm packages (`npm install`; 0 vulnerabilities).
- `run.sh`, `run-api.sh`, `test.sh`: Java classpath separator now switches to `;` on Windows (Git Bash) and stays `:` elsewhere.
- `test/e2e/local-workflows.e2e.js`:
  - classpath uses `path.delimiter`
  - spawns `npm.cmd` through a shell on Windows (newer Node refuses otherwise)
  - stops test servers with `taskkill /T` so Vite isn't left running
- Installed Playwright's Chromium (`npx playwright-core install chromium`) so `npm run test:e2e` runs.
- Added `G:\z\JD\.claude\launch.json` with two app previews: `motorph` (8080, Firebase) and `motorph-local` (8081, local demo).

## 2. Leave approve / reject (local demo only)

- New route `POST /api/workflows/leave/{id}/approve` and `/reject`, body `{"reviewerId":"DEMO-002"}`.
- Records `reviewerId` and `reviewedAt` (the course Leave table's `approverID` / `dateApproved`).
- Rules enforced:
  - only a **Pending review** request can be decided (otherwise `409`)
  - the reviewer must exist in the directory
  - **a reviewer cannot approve or reject their own request** (my addition; not in the course docs)
  - Firebase mode returns `403` (the published Firestore rules have no reviewer role)
- Browser: "Reviewing as (local demo supervisor)" picker, Approve/Reject buttons on pending rows, Approved/Rejected in the status filter, reviewer and time shown in row Details.
- Files: `DemoLeaveRequest.java`, `WorkflowStore.java`, `InMemoryWorkflowStore.java`, `FirestoreWorkflowStore.java` (blocked on purpose), `WorkflowService.java`, `AppServer.java`, `web/app.js`, `web/index.html`.

## 3. Temporary leave credits

- **Placeholder numbers (user-chosen, not an approved policy)**, in `LeavePolicy.java`:
  - Vacation 5 days/year (same as the Labor Code's 5-day Service Incentive Leave minimum)
  - Medical 5 days/year
  - Personal 3 days/year
  - Unpaid: not limited
- Days counted Monday–Friday; no holiday calendar.
- A request counts in the calendar year it starts.
- Pending requests hold credits; approved requests use them; cancelling frees them.
- Submitting more than (remaining − pending) is refused; approving more than remaining is refused.
- New route `GET /api/workflows/leave/balances?employeeId=…&year=…`.
- Browser: "Temporary leave credits" table in the leave form (yearly / used / pending / remaining).
- Files: `LeavePolicy.java`, `LeaveBalance.java`, `WorkflowService.java`, `AppServer.java`, `web/app.js`, `web/index.html`, `web/workflow.css`.

## 4. Attendance-based cutoff payroll

> Superseded in part by section 10: rates are now monthly basic ÷ 168, and paid time is min(worked, 8 h) instead of the 17:00 cap.

- Rules chosen by the user (course convention), in `PayrollProfile.java`:
  - cutoffs **1–15** and **16–month end**
  - log-in up to **08:10 counts as 08:00**; log-in before 08:00 also counts from 08:00 (my assumption)
  - log-out capped at **17:00**
  - **12:00–13:00 lunch is unpaid** (only the overlapping part is removed)
- **Placeholder hourly rates**: DEMO-001 ₱150, DEMO-002 ₱160, DEMO-003 ₱200. Employees without a rate are refused.
- Gross = paid minutes × rate ÷ 60, rounded half-up per cutoff.
- New route `GET /api/payroll/cutoffs?employeeId=…&month=…`.
- Browser: "Cutoff payroll estimate" panel on the Payroll page.
- Files: `PayrollProfile.java`, `CutoffPay.java`, `CutoffPayrollService.java`, `MonthPayroll.java`, `web/payroll-cutoff.js`.

## 5. Calculations matched to sweldongpinoy.com

- The mentor named **https://www.sweldongpinoy.com/** as the reference.
- Our results were compared with the site's 2026 Private Employee results for **25 salaries (₱1,000–₱700,000)**; all match to the centavo.
- To match, three rules changed:
  - **Tax** is annualised: 12 × monthly taxable, using the RR 11-2018 annual table, ÷ 12 (was the monthly table). The site also added a 13th month above ₱90,000 basic; **removed in section 11** per the mentor
  - **PhilHealth** employee share is rounded from salary × 2.5% (was 5% rounded, then halved)
  - **Pag-IBIG** was 2% from exactly ₱1,500 to match the site; **changed back to 1% in section 10** per the mentor
- SSS already matched.
- **Deductions are split half per cutoff**, like the site's semi-monthly view (user's choice). Each line (SSS, PhilHealth, Pag-IBIG, tax) is halved; an odd centavo goes to the 2nd cutoff so the month adds up exactly.
- A cutoff whose share of deductions is bigger than its gross is shown as **blocked**, never as a negative net.

## 6. All payroll math moved to Java

- `StatutoryCalculator2026.java` + `StatutoryBreakdown.java`: SSS, PhilHealth, Pag-IBIG and tax in whole centavos (inputs capped at ₱10,000,000).
- New route `POST /api/payroll/statutory` serves the manual "Monthly contribution preview" form.
- `MonthPayroll.java` combines gross, deductions and the per-cutoff split for both the cutoff screen and payslips.
- The browser only formats results:
  - `web/payroll-preview-2026.js` now has only peso parsing/formatting
  - `web/payroll-rules-2026.js` was deleted (its rules are in Java)
- The preview shows "Calculating…" while waiting for Java.

## 7. Payslips (view and print)

- New route `GET /api/payroll/payslip?employeeId=…&month=…&cutoff=1|2`.
- Shows:
  - payslip number (`PS-DEMO-001-2026-10-1`)
  - employee name, department, role
  - pay period, paid hours, rate
  - gross, SSS, PhilHealth, Pag-IBIG, tax, total deductions, **net pay**
- Clearly labelled **"SYNTHETIC DEMO — NOT A REAL PAYSLIP"**; **nothing is saved**.
- No payslip for a cutoff with no paid attendance or a blocked net (`409`).
- Browser: "Payslip" button on each payable cutoff row, payslip card, **Print payslip** button (prints only the payslip).
- Files: `AppServer.java`, `MonthPayroll.java`, `web/payroll-cutoff.js`, `web/index.html`, `web/payroll-preview.css`.

## 8. Tests added or updated

- Java (`./test.sh`, **387 checks**, was 181):
  - `StatutoryCalculator2026Test` — the 25 sweldongpinoy.com values plus SSS/PhilHealth/Pag-IBIG/tax boundaries
  - `CutoffPayrollServiceTest` — hours rules, cutoff gross, per-line split (hand-worked values)
  - `WorkflowServiceTest` — approve/reject and leave-credit rules
  - `AppServerIntegrationTest` — every new route, Firebase-mode 403, payslip 409/400
- Browser logic (`npm run test:web`, **62 tests**): formatting, cutoff display, payslip display, preview CSV.
- End-to-end (`npm run test:e2e`, **17 sections, all pass on Windows**); new sections cover:
  - leave credits blocking an over-limit request
  - self-approval hidden; approval by another reviewer uses a credit
  - cutoff payroll for DEMO-002 (₱2,271.60 and ₱991.60 nets) and its payslip
- `npm run build` passes; `npm audit` reports 0 vulnerabilities.

## 9. Docs updated

- `README.md`, `docs/api-contract.md` (all new routes), `docs/acceptance-baseline.md`, `docs/peer-parity-roadmap.md`, `docs/source-grounding-and-open-rules.md`, `docs/local-only-inventory-and-full-build-pseudocode.md`, `docs/candidate-policy-review.md`.

## 10. Mentor's answers applied

The mentor answered the eight questions. What changed in the code:

- **Hourly rate = monthly basic ÷ 168** (matches MotorPH's employee file: ₱90,000 → ₱535.71, ₱60,000 → ₱357.14).
  - The flat ₱150 / ₱160 / ₱200 rates are gone.
  - Placeholder monthly basics: DEMO-001 ₱52,670 (₱313.51/h, the file's lowest rate), DEMO-002 ₱60,000 (₱357.14/h), DEMO-003 ₱90,000 (₱535.71/h).
  - The API now also returns `monthlyBasic`.
- **Paid time = min(worked, 8 hours) per day**; no more "cap at 17:00".
  - A 10:00–19:00 shift with the meal hour pays 8 h, no overtime.
  - An early log-in (e.g. 07:00–17:00 = 9 h worked) pays 8 h; the extra 1 h is shown as **"unapproved, unpaid"** (`unpaidOverageMinutes`) and is never added to pay.
  - Grace (08:00–08:10 counts as 08:00) and the unpaid 12:00–13:00 meal hour stay.
- **Pag-IBIG at exactly ₱1,500 is now 1%** (HDMF Circular 460: "at or below ₱1,500").
  - This is the one place we now differ from sweldongpinoy.com, on purpose.
  - Ruleset renamed `PH-2026-sweldongpinoy-v3`.
- Profile renamed `TEMPORARY-2026-mentor-v1`.
- Already matched the mentor's answers, so no change was needed:
  - leave credits stay a labelled demo policy and are not used in payroll
  - self-approval is blocked on the server by employee ID
- Tests updated with new hand-worked values:
  - DEMO-001 October: 24 h × ₱313.51 = ₱7,524.24; net ₱7,136.50
  - DEMO-002 end-to-end: nets ₱5,291.03 and ₱2,433.90
  - the mentor's 07:00–17:00 and 10:00–19:00 examples
- Results: Java **391** checks, browser logic **63**, end-to-end **17/17**, build OK.

## Still open after the mentor's answers

- Overtime and paid leave: built in section 12.
- **Self-approval:** the mentor says it must be blocked by identity. The local demo picks the reviewer from a drop-down, which is not a real login. Firebase mode still blocks approval until the Firestore rules support a reviewer.
- **Real monthly basics:** the DEMO basics above are placeholders.

## 11. 13th month removed from tax

- The mentor said the 13th-month ₱90,000 exemption is annual and cumulative, so it can't be decided inside a semi-monthly run. You chose to drop it.
- Tax is now just 12 × monthly taxable → annual table → ÷ 12. Nothing else is added.
- Only basics **above ₱90,000** change; at ₱90,000 and below, results still match sweldongpinoy.com.
- Hand-worked new values (the site's higher value in brackets):
  - ₱95,000 → tax ₱14,543.75 (site ₱14,647.92)
  - ₱100,000 → tax ₱15,762.50 (site ₱15,970.83): 12 × 95,550 = 1,146,600 → 102,500 + 25% × 346,600 = 189,150 ÷ 12
  - ₱120,000 → ₱20,762.50 (site ₱21,387.50)
  - ₱200,000 → ₱42,206.67 (site ₱44,956.67)
  - ₱700,000 → ₱193,650.83 (site ₱211,442.50)
- No DEMO employee is affected (the highest basic is ₱90,000, and cutoff gross comes from attendance).
- Ruleset renamed `PH-2026-sweldongpinoy-v4`.
- Results: Java **391**, browser logic **63**, end-to-end **17/17**, build OK.

## 12. Paid leave, overtime, holidays and night differential

Your choices: build paid leave + overtime first; Vacation, Medical and Personal are paid (8 h × hourly rate per day), Unpaid is not; use the official 2026 holiday list.

- **Overtime requests (local demo only)**
  - New "Overtime requests" panel on the Payroll page: employee, date worked, hours, note, and a "Reviewing as" picker.
  - Another employee approves or rejects. **Nobody can approve their own request** (checked on the server, like leave).
  - One open request per employee per date; 1 minute to 12 hours.
  - Routes: `POST /api/workflows/overtime`, `POST /api/workflows/overtime/{id}/approve|reject`; listed in `GET /api/workflows`.
  - Firebase mode returns `403` (no Firestore rules for it yet).
- **How a worked day is now paid** (minute by minute, meal hour skipped, 08:00–08:10 grace kept):
  - first 8 worked hours = regular
  - later minutes = overtime, up to the approved minutes
  - anything beyond that = "unapproved, unpaid" (still shown)
- **Rates by day type** (OT rates from the mentor; first-8-hour rates are the DOLE ones those OT rates are built on):

  | Day | First 8 h | Overtime |
  |---|---|---|
  | Ordinary day | 100% | 125% |
  | Rest day (Sat/Sun) or special non-working day | 130% | 169% |
  | Special day on a rest day | 150% | 195% |
  | Regular holiday | 200% | 260% |
  | Regular holiday on a rest day | 260% | 338% |

- **Night differential:** +10% of that minute's rate for 22:00–06:00, stacked on premiums and overtime.
- **Unworked regular holiday** (weekday): paid 8 hours. Unworked special days: not paid ("no work, no pay").
- **Paid leave:** each approved Vacation/Medical/Personal **weekday** that is not a holiday and has no attendance pays 8 hours. It never counts as hours worked, so it can't create overtime.
- **2026 holidays** in `HolidayCalendar2026.java`:
  - Regular: Jan 1, Mar 20 (Eid'l Fitr, Proc. 1189), Apr 2, Apr 3, Apr 9, May 1, May 27 (Eid'l Adha, Proc. 1264), Jun 12, Aug 31, Nov 30, Dec 25, Dec 30
  - Special non-working: Feb 17, Apr 4, Aug 21, Nov 1, Nov 2, Dec 8, Dec 24, Dec 31
  - Feb 25 (EDSA) is a special **working** day, so it is ordinary.
  - Source: Proclamation No. 1006 (Sept 3, 2025), as reported by GMA News, and the two Eid proclamations.
- **Payslip and cutoff table** now show each earnings line (basic, premium, overtime, night differential, holiday pay, paid leave) and approved OT hours.
- **Changed numbers:** DEMO-002's seed row on 2026-10-03 is a **Saturday**, so it now earns the 30% rest-day premium (+₱857.14). Its October nets in the browser test are now ₱6,114.60 and ₱2,400.33.
- **Hand-worked December test** (DEMO-001, ₱313.51/h): special-day work, Christmas with 2 h approved OT, unworked Rizal Day, 3 Vacation days, 1 Unpaid day, and 1.5 h of night OT. Cutoff grosses ₱13,939.43 and ₱9,154.49.
- Files: `HolidayCalendar2026.java`, `DemoOvertimeRequest.java`, `CutoffPay.java`, `CutoffPayrollService.java`, `PayrollProfile.java` (`splitDay`), `LeavePolicy.java` (`isPaid`), `WorkflowStore/Service`, `InMemoryWorkflowStore`, `FirestoreWorkflowStore`, `AppServer.java`, `web/overtime.js`, `web/payroll-cutoff.js`, `web/app.js`, `web/index.html`.
- Results: Java **428** checks, browser logic **64**, end-to-end **18/18**, build OK.

### Assumptions to confirm with the mentor

- Rest days are **Saturday and Sunday** for everyone (schedules don't store rest days yet).
- An unworked regular holiday is paid **without** checking that the employee worked or was on paid leave the day before (the Labor Code condition).
- A holiday inside a leave request still uses a leave credit (credits count every Mon–Fri). It is paid as holiday pay if it's a regular holiday, and not paid if it's a special day.
- SSS, PhilHealth, Pag-IBIG and tax still use the **month's whole gross** (including overtime and premiums) as every base. PhilHealth normally uses basic salary only.
- Local holidays and the Eid holidays of other years are not loaded; months outside 2026 are refused.

## 13. Saved payroll history

- **Finalize payslip** button on the payslip card (local demo):
  - saves a frozen copy of that cutoff's payslip, marked "FINALIZED — SAVED SNAPSHOT" with the time it was finalized
  - the server builds the payslip itself; the browser cannot send amounts
  - each cutoff can be finalized **once** (`409` after that); cutoffs with no pay or a blocked net can't be finalized
- **Saved payroll history** table under the payslip: payslip number, employee, period, gross, net, finalized time, and an **Open** button. Opening one shows the saved copy, which can be printed.
- A saved payslip **does not change** if attendance, leave or overtime change later. A test adds attendance after finalizing and checks that the live payslip changes but the saved one doesn't.
- Routes: `POST /api/payroll/payslips`, `GET /api/payroll/payslips`, `GET /api/payroll/payslips/{id}`.
- Limits:
  - kept in **server memory**, like the rest of the local demo, so restarting the server clears it
  - Firebase mode returns `403` (it needs Firestore rules for a payroll collection)
  - there is no "unfinalize" or correction flow
- Files: `SavedPayslipStore.java`, `AppServer.java` (`buildPayslip`, `handleSavedPayslips`), `web/payroll-cutoff.js`, `web/app.js`, `web/index.html`, `web/payroll-preview.css`.
- Results: Java **438** checks, browser logic **66**, end-to-end **19/19** (run twice), build OK.

## 14. UI redesign with Tailwind CSS

- **Tailwind CSS 4.3.4** is now part of the Vite build (`@tailwindcss/vite` in `vite.config.js`). `npm run build` and the preview compile it automatically; no separate step.
- The four old stylesheets (`styles.css`, `workflow.css`, `payroll-preview.css`, `employee-records.css`) are replaced by one file, **`web/main.css`**.
  - The old files were moved to `archive/css-before-tailwind/` (not deleted; not used by the build) in case you want them back.
  - The page keeps its class names; each is now built from Tailwind utilities with `@apply`. That way the JavaScript, which creates table rows and buttons with those names, didn't need changes.
- **Look, based on the MotorPH letterhead image:**
  - **Letterhead header** on every page: wheel-and-flame logo, bold italic navy **MotorPH**, and the company address, phone and email.
  - The logo was a placeholder drawing; replaced with your MotorPH logo in section 15.
  - Page titles are **bold uppercase** (e.g. "EMPLOYEE REGISTER"), like "MONTHLY PAYROLL SUMMARY REPORT".
  - **Table headers are dark slate with white text**; rows are striped.
  - Navy sidebar with a flame-coloured stripe; the active page gets an orange marker.
  - Flame-orange section labels and accents; white rounded cards on a light grey page.
  - **Montserrat** font, close to the one in the image. It's bundled with the app, so it works offline.
- **Payslip** is now printed on the MotorPH letterhead (logo, address, "EMPLOYEE PAYSLIP"); the net pay row is highlighted.
- Mobile: the sidebar becomes a compact menu, and the letterhead shrinks to logo and name. Nothing scrolls sideways at 375–390 px.
- Fixed old text on the Payroll page that still said overtime and leave pay weren't included.
- `THIRD_PARTY_NOTICES.md` lists Tailwind (MIT) and Montserrat (SIL OFL 1.1), and their license texts are in `third_party/licenses/`. The system.css title-bar style is no longer used.
- Checked in the browser at desktop and mobile width. Results: Java **438**, browser logic **66**, end-to-end **19/19**, build OK, `npm audit` 0 vulnerabilities.

## 15. UI redesign, round 2 (your feedback)

You said the first version had rounded corners and coloured top/left edges, looked AI-generated and not modern, and later that the all-white version was plain and confusing. Changes:

- **No rounded corners anywhere**: Tailwind's corner sizes are set to 0 for the whole app; buttons, inputs, cards, badges and avatars are square.
- **No coloured top or left edges** on cards and notices, no shadows, no gradient stripes, no "pill" badges.
- Removed decorative clutter: the small numbered labels ("DIRECTORY — 01"), the dashes, the circled "i"/"!" icons and the page numbers in the menu.
- **Dark blue (navy) used across the app**:
  - navy sidebar; the current page is shown as a white block
  - navy top bar with the breadcrumb, status and sign-out
  - navy summary strip for the key numbers
  - navy table headers
  - light navy band behind each section title, on a cool blue-grey page
- **Your MotorPH logo** (the third image) is used in the sidebar, on the sign-in page and on the payslip letterhead. It sits on a square white tile on navy because the image has a white background. File: `web/public/motorph-logo.png`.
- **Sidebar logo without the white box:** `web/public/motorph-logo-transparent.png` is your logo with only the outer white background removed (made with Pillow from the original). The payslip still uses the original image.
- Fonts: **Inter** for all text (a modern UI font), **Montserrat** kept for the bold italic "MotorPH" wordmark.
- The big address letterhead was removed from the top of every page; the payslip keeps it (logo, name, address, "EMPLOYEE PAYSLIP").
- Phone layout fixes found by the 390 px check: the panel toolbars wrap, form columns can't grow past the screen, and the leave-credits table wraps or scrolls inside its box.
- The mobile check in `test/e2e/local-workflows.e2e.js` now names the elements that overflow when it fails.
- Results: Java **438**, browser logic **66**, end-to-end **19/19** (run twice), build OK.

## 16. Role-based workspaces (phase 1)

Built from your RBAC roles, permissions matrix and Venn diagram. You chose: roles and layouts first, 2 extra demo people, local demo only.

- **Demo accounts** (all fictional). The app now opens at **"Choose a demo account"**:

  | Account | Role | Notes |
  |---|---|---|
  | Mara Rivera (DEMO-001) | HR | reports to Noa |
  | Eli Santos (DEMO-002) | Payroll Staff | reports to Noa |
  | Noa Reyes (DEMO-003) | Supervisor | leads the team |
  | **Jae Cruz (DEMO-004)** | Employee | **new**; reports to Noa; ₱25,000 placeholder basic, 2 October attendance rows |
  | **Lia Torres (DEMO-005)** | System Admin | **new**; ₱75,000 placeholder basic |

  - **Switch account** (top bar) goes back to the chooser.
  - There are no passwords, because this is a demo, not a real login.
- **Each role has its own menu:**
  - **Employee:** Overview, My attendance, My requests, Team, My pay
  - **Supervisor:** Overview, Team attendance, Requests & approvals, Team, Pay & payslips
  - **Payroll Staff:** Overview, Employees, Attendance, Requests, Team, Reports, Payroll processing
  - **HR:** Overview, Employees, Attendance, Requests & approvals, Team, HR desk, Reports, Payroll
  - **System Admin:** everything
- **Tools shown per role:**
  - statutory calculator: Payroll Staff and Admin
  - timesheet entry: Supervisor (direct reports only) and Admin
  - Finalize payslip: Payroll Staff and Admin, never their own
  - build tracker and "still outside" panel: Admin only
  - pickers list only the people you may act for (e.g. an Employee's leave, overtime and pay pickers show only themselves)
- **The server enforces it** (`AccessPolicy.java`). It's not just hidden in the browser:
  - Reads are filtered by **Self / Team / All** as in the matrix. Employee and Supervisor see their team in the directory and team leave ("view team members", "check team leave").
  - **Approvals use the signed-in account** (mentor: "by identity, not role"). Leave is approved by the employee's supervisor, HR or Admin; overtime by the supervisor or Admin. **Nobody approves their own request.** The old "Reviewing as" dropdowns are hidden.
  - You file leave, overtime and tickets for yourself; HR may file leave and tickets for anyone; Admin may file anything.
  - Employee records are created and edited by HR and Admin only.
- **Tests:** new `AccessPolicyTest` (29 checks), 26 role checks in the API test, `test/web/roles.test.js`, and a new end-to-end section that signs in as each account. The existing approval steps now really switch accounts: Eli can't approve his own leave or overtime, Mara (HR) can't approve overtime, and Noa approves both.
- **Changed expectations:** 5 people instead of 3 (directory, reports and export counts). Payroll Staff can no longer approve leave, and HR can no longer approve overtime, so the tests' reviewers changed to the supervisor.
- **Not built yet (phase 2):** ~~clock in/out, salary tracker~~ (done in section 17), ~~the new ticket types (promotion, merit increase, bonus, disciplinary/unpaid leave), team schedules~~ (done in section 19), ~~gross-to-net with allowances and voluntary deductions, disbursement and remittance~~ (done in section 18), ~~onboarding/offboarding~~ (done in section 20), ~~government details with admin approval~~ (done in section 21), and ~~system configuration~~ (roles and rates done in section 22; payroll policy and security settings not built). Also roles in Firebase mode.
- Results: Java **494**, browser logic **70**, end-to-end **20/20** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 17. Clock in/out and salary tracker (phase 2, part 1)

- **My day** panel at the top of Overview for every demo account.
  - Employees and Supervisors land on it; for them it replaces the build-summary numbers.
- **Time clock:**
  - **Clock in** and **Clock out** use the **server's time** (Asia/Manila, to the minute), not the browser's, so the time can't be edited on the device.
  - Clock out writes one normal attendance row, which then goes through the usual pay rules (08:10 grace, meal hour, 8 h cap, approved overtime).
  - One row per person per day: after clocking out, it says "Done for today".
  - You can't clock in twice or clock out without clocking in.
  - Shortcut: a shift past midnight is closed at 23:59 of the day it started; overnight shifts need a rule.
  - The open shift is kept in server memory, like the rest of the local demo.
- **Salary tracker** for the cutoff you're in (1–15 or 16–end):
  - shows paid hours so far (plus approved overtime, paid leave and holidays), gross so far, estimated deductions, estimated net, and month gross so far
  - all amounts come from the Java payroll engine
  - it counts **only days up to today**. While building it I found that future approved leave and holidays (e.g. Bonifacio Day on 30 Nov) were being counted as "so far". New `through=` option on `/api/payroll/cutoffs` fixes that.
  - an open shift counts once you clock out
- Routes: `GET`/`POST /api/attendance/clock` (signed-in account only; Firebase mode `403`).
- Files: `ClockService.java`, `AppServer.java` (`handleClock`, `through`), `CutoffPayrollService.java`, `web/my-day.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Tests: `ClockServiceTest` (9 checks with a controllable clock, including the 08:04 grace and past-midnight cases), 10 more API checks, a `through=` payroll check, `test/web/my-day.test.js`, and an end-to-end step where Jae clocks in and out and sees the tracker.
- Results: Java **514**, browser logic **73**, end-to-end **20/20** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 18. Payroll gross-to-net, disbursement and remittance (phase 2, part 2)

Your choices: allowances are non-taxable and paid half per cutoff; bonuses are taxable; build everything (earnings, voluntary deductions, disbursement, remittance).

- **How a cutoff is paid now:**
  - **Earned pay** (attendance, premiums, approved overtime, night differential, holiday and leave pay) **+ taxable bonuses = gross**
  - **− statutory**, half of the month's each cutoff:
    - SSS, PhilHealth and Pag-IBIG are on earned pay only (bonuses don't change them)
    - withholding tax is on earned pay + bonuses
  - **− voluntary deductions** (company loan, insurance, benefit contribution), taken in the order added, never more than what's left. If there isn't enough pay, the rest shows as "not taken".
  - **+ allowances** (rice, phone, clothing): non-taxable and outside every contribution base, half of the monthly amount each cutoff
  - **= net pay**
- **Placeholder monthly allowances** (shaped like the MotorPH employee file):

  | Person | Rice | Phone | Clothing | Paid by |
  |---|---|---|---|---|
  | Mara | ₱1,500 | ₱1,000 | ₱1,000 | Direct deposit |
  | Eli | ₱1,500 | ₱1,000 | ₱1,000 | Direct deposit |
  | Noa | ₱1,500 | ₱2,000 | ₱1,000 | Direct deposit |
  | Jae | ₱1,500 | ₱500 | ₱500 | Check |
  | Lia | ₱1,500 | ₱2,000 | ₱1,000 | Direct deposit |

- **Payroll processing panel** (Payroll page; Payroll Staff and Admin can edit, HR can only view):
  - **Earnings & deductions:** add a taxable bonus, incentive or adjustment to one cutoff, or a voluntary deduction per cutoff over a month range; remove items. Payroll Staff can't pick themselves.
  - **Disbursement:** release a cutoff's finalized payslips as one batch (direct deposit or check). Each payslip can be released once. Nothing is sent to a bank.
  - **Remittance report:** for a month, the withheld employee shares plus the employer shares (SSS incl. EC, PhilHealth, Pag-IBIG) and the withholding tax to pay each agency, from finalized payslips only.
- **Payslip** now lists bonuses (taxable), voluntary deductions (with any "not taken" amount), and allowances (non-taxable) before net pay.
- **Permissions** (from your matrix): earnings/deductions are edited by Payroll Staff and Admin, never for their own pay; HR reads; the Supervisor reads the team; Employees see them only on their own payslip. Disbursement is Payroll Staff/Admin. Remittance is Admin/HR/Payroll.
- **Changed numbers:** every payable cutoff now includes allowances in net pay. For example, DEMO-001's October 1–15 net is ₱7,136.50 + ₱1,750 = ₱8,886.50, and DEMO-002's end-to-end nets each rose by ₱1,750.
- **Hand-worked checks:**
  - Jae, October 1–15: ₱2,380.96 earned + ₱1,000 bonus − SSS ₱125 − PhilHealth ₱125 − Pag-IBIG ₱23.81 − ₱300 loan + ₱1,250 allowances = **₱4,057.15** (unit, API and browser)
  - a ₱20,000 bonus on ₱7,524.24 earned raises tax to ₱887.31 a month while SSS stays on earned pay
  - a ₱2,000 loan + ₱500 insurance on ₱2,107.15 of room takes ₱2,000 + ₱107.15 (₱392.85 short)
  - remittance: SSS ₱125 employee + ₱255 employer
  - While testing, my own hand calculation for Jae's November missed Bonifacio Day (30 Nov) holiday pay; the engine was right and the test now says why.
- **Bug found and fixed while checking in the browser:** the items list was sending each item as escaped text, so the table stayed empty. The API test now checks the JSON shape.
- **Assumptions to confirm with the mentor:**
  - allowances are paid only in a cutoff with earned pay
  - a bonus's extra tax is split half per cutoff like every deduction
  - bonuses don't count for SSS, PhilHealth or Pag-IBIG
  - a month with bonuses but no earned pay withholds nothing
- Files: `PayrollAdjustments.java`, `DisbursementStore.java`, `MonthPayroll.java`, `CutoffPayrollService.java`, `PayrollProfile.java`, `AccessPolicy.java`, `AppServer.java`, `web/payroll-processing.js`, `web/payroll-cutoff.js`, `web/roles.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Results: Java **541**, browser logic **76**, end-to-end **21/21** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 19. Supervisor tools (phase 2, part 3)

Your choices: schedules have a shift start and workdays; HR approves promotions, merit increases and disciplinary leave, while Payroll Staff approve bonuses; supervisors can correct existing timesheets with a reason.

- **Team schedules** (Team page):
  - The supervisor (or Admin) sets each direct report's **shift start** (05:00–15:00) and **workdays** (1–6 days).
  - Pay follows the schedule:
    - the 10-minute grace counts from the shift start
    - the unpaid meal hour starts 4 hours in (10:00 shift → 14:00–15:00)
    - days off are **rest days** (rest-day premium; no leave or holiday pay on them)
  - Everyone sees the team's schedules.
  - Shortcut: there's no schedule history, so a change applies to every date not yet finalized. Finalized payslips keep theirs.
- **Timesheet corrections** (Attendance page; Supervisor for direct reports, or Admin):
  - pick a person and one of their attendance rows, enter the new times and a **required reason**
  - the log keeps the original and corrected times, who edited it and when
  - a row can only be corrected under its own employee
- **Pay-change requests** (Requests page):
  - The supervisor files **Promotion** (new position + new basic from a month), **Merit increase** (new basic from a month), **Bonus** (amount + cutoff) or **Disciplinary / unpaid leave** (dates + reason) for a direct report.
  - **HR** approves promotions, merit increases and disciplinary leave; **Payroll Staff** approve bonuses; the Admin can approve any.
  - Nobody decides a request about themselves or one they filed. For example, HR can't approve HR's own promotion; the Admin does.
  - **Approving applies it:**
    - the salary changes from the effective month (`SalaryHistory`)
    - a promotion also changes the position title right away (shortcut: titles aren't effective-dated)
    - a bonus becomes a taxable payroll earning
    - disciplinary leave becomes approved Unpaid leave
  - Employees don't see these requests (matrix: Earnings/Deductibles "–" for Employee).
- **Hand-worked checks:**
  - Jae on a 10:00, Tue–Sat schedule. Mon 5 Oct becomes a rest day: 08:00–17:00 with the 14:00 meal hour = 8 h at 130%. Tue 6 Oct 08:06–17:00 = 7.9 h (no grace before 10:00). Basic ₱2,366.08 + premium ₱357.14.
  - Correcting Tue 6 Oct to 10:05–19:00 makes it a full 8 h day. With Wed 7 Oct, his October 1–15 earned pay is ₱3,928.58.
  - Merit increase to ₱27,000 from December: rate ₱160.71 (27,000 / 168) in December, ₱148.81 in November.
- Routes: `GET /api/schedules`, `PUT /api/schedules/{id}`, `GET`/`POST /api/attendance/corrections`, `GET`/`POST /api/pay-changes`, `POST /api/pay-changes/{id}/approve|reject`.
- Files: `WorkSchedule.java`, `SalaryHistory.java`, `PayChangeRequests.java`, `PayrollProfile.java` (`splitDay` with a schedule), `HolidayCalendar2026.java` (rest day per person), `CutoffPayrollService.java`, `AttendanceService.java`, `MutableAttendanceStore`/`InMemoryAttendanceStore` (`correctTimes`), `AccessPolicy.java`, `AppServer.java`, `web/supervisor-tools.js`, `web/roles.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Tests: 7 schedule/salary/correction payroll checks, 6 policy checks, 28 API checks, `test/web/supervisor-tools.test.js`, and an end-to-end section (Noa sets the schedule, corrects the timesheet and files a merit increase; Mara approves; Eli sees ₱160.71/h in December).
- Noted while testing: choosing a team member loads their current schedule into the form, so anything typed within a split second of choosing is replaced. The test now waits for the load.
- **Still to confirm with the mentor:**
  - the meal hour 4 hours into the shift
  - no overnight shifts
  - leave credits still count Monday–Friday even for someone on a Tue–Sat schedule
- Results: Java **582**, browser logic **79**, end-to-end **22/22** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 20. HR onboarding and offboarding (phase 2, part 4)

Your choices: full onboarding with a checklist; separation with final pay and clearance (no cash for unused leave); plus HR answering HR tickets and maintaining departments and positions.

- **Onboard a new hire** (HR desk; HR or Admin):
  - enter the name, department and position (from the HR lists), location, start date, supervisor, monthly basic, rice/phone/clothing allowances and pay method
  - one step does all of this:
    - creates the employee record
    - puts them on the supervisor's team
    - gives them a schedule ID so attendance can be recorded
    - sets their salary from the start month (rate = basic ÷ 168)
    - gives them an **Employee demo account** in the chooser
  - **Checklist:** Contract signed, Government IDs submitted, Payroll set up (ticked automatically), Orientation done. HR ticks items off, and each records who did it and when.
- **Offboard** (HR or Admin, never yourself):
  - set the **last day** and **reason**; the status becomes **Separated**
  - pay counts only from the start date through the last day (no attendance, holiday or leave pay outside it)
  - the **account closes** the day after the last day: it leaves the chooser and the server refuses it
  - the panel names the **final payslip** (the cutoff containing the last day); Payroll Staff finalize and release it as usual
  - **Clearance checklist:** Equipment returned, Clearance signed, Final pay released, Certificate of employment issued
  - unused leave is **not** converted to cash; that needs the mentor's rule
- **HR tickets:**
  - HR answers open tickets in the **HR queue** (HR inquiry) with a reply; the Admin can answer any
  - the employee sees the reply in the ticket's Details
  - nobody answers their own ticket
  - the status filter now has **Resolved**
- **Departments & positions:**
  - HR adds and renames entries in the lists the onboarding form uses
  - renaming updates every employee record that uses it
  - the older HR desk editor still accepts free text, so existing tests and data keep working
- **Hand-worked check:**
  - Ana Lim starts 1 Oct at ₱21,000 (₱125.00/h). She works 20 and 30 Oct (16 h = ₱2,000.00), and her last day is 31 Oct.
  - Final payslip (Oct 16–31): ₱2,000.00 − SSS ₱125 − PhilHealth ₱125 − Pag-IBIG ₱20 + allowances ₱1,250 = **₱2,980.00**, by check.
  - November pays nothing; she doesn't get Bonifacio Day holiday pay after leaving.
- **Shortcuts to know:**
  - the reporting line (who supervises whom) is kept process-wide in `AccessPolicy` so new hires can join a team
  - the last cutoff's allowances are paid in full (half the month), not prorated
- Routes: `/api/hr/catalog` (+ `/rename`), `/api/hr/onboard`, `/api/hr/employment` (+ `/{id}/offboard`, `/{id}/tasks`), `/api/workflows/tickets/{id}/resolve`.
- Files: `HrRecords.java`, `AccessPolicy.java` (supervisor assignment, HR and ticket rules), `InMemoryEmployeeScheduleStore.java` (`assign`), `DemoTicket.java`, `WorkflowStore`/`InMemoryWorkflowStore`/`FirestoreWorkflowStore`/`WorkflowService` (`resolveTicket`), `EmployeeService.java` (Separated status), `CutoffPayrollService.java` (employment window, new-hire allowances), `AppServer.java`, `web/hr-tools.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Tests: 32 more API checks (onboarding, team and attendance, own pay, checklist, offboarding, closed account, final payslip, no pay after leaving, tickets, lists), `test/web/hr-tools.test.js`, and an end-to-end section (Jae asks HR, Mara answers, adds Logistics, onboards Ana, ticks the contract, Ana signs in, Mara offboards her and the account disappears).
- Results: Java **614**, browser logic **82**, end-to-end **23/23** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 21. Government details (phase 2, part 5)

Your choice: both changes and full view need the System Admin's approval.

- **What's stored:** each employee's **SSS**, **PhilHealth**, **Pag-IBIG MID** and **TIN**, with format checks (10 / 12 / 12 / 9-or-12 digits, shown with dashes). The demo starts empty, and the panel warns **never to enter real numbers**.
- **Who sees what** (your matrix and diagram):
  - **System Admin:** full access, edits directly, approves requests.
  - **HR and Payroll Staff:** see numbers **masked to the last 4 digits** (`••-••••789-0`).
    - They can **propose a change**; nothing changes until the System Admin approves.
    - They can **ask for full view** of one person with a reason. Once approved, full view lasts **until the end of that day**, for that requester and that employee only.
  - **Supervisors and Employees:** no access (the server refuses).
  - Nobody files a request about themselves, and the System Admin doesn't approve requests about their own record.
- **Audit log** (System Admin): every direct edit, request, decision and **full view**, with who, whom and when.
- **Panel:** "Government details" on the Employees page (HR, Payroll Staff, System Admin): masked table with **Show full**, a "Change numbers" form ("Save now" for the admin, "Send for approval" for HR/Payroll), "Ask for full view", the requests table with Approve/Reject for the admin, and the audit log.
- Routes: `GET /api/government`, `GET /api/government/{id}?reveal=true`, `PUT /api/government/{id}`, `POST /api/government/{id}/change-requests`, `POST /api/government/{id}/access-requests`, `GET /api/government/requests`, `POST /api/government/requests/{id}/approve|reject`, `GET /api/government/audit`.
- Files: `GovernmentDetails.java`, `AccessPolicy.java`, `AppServer.java`, `web/government.js`, `web/roles.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Tests: `GovernmentDetailsTest` (18 checks: formats, masking, approval, same-day per-person grants, audit), 21 API checks, `test/web/government.test.js`, and an end-to-end section:
  - Lia enters Jae's numbers
  - Mara sees them masked, is refused full view, asks, and sees them after approval
  - Eli proposes a new Pag-IBIG MID, which Lia approves
  - the audit shows Mara's full view
  - Jae can't see the panel
- Shortcut: numbers, requests and the audit log are in server memory like the rest of the local demo. Payslips don't show government numbers yet.
- Results: Java **653**, browser logic **84**, end-to-end **24/24** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 22. System configuration (phase 2, part 6)

Your choice: user roles and teams, plus tax tables and rates. The payroll policy and security settings were left out.

- **New menu item "System config"**, for the System Admin only (Firebase mode doesn't show it).
- **User roles & teams:**
  - the admin changes anyone's **role** (Employee, Supervisor, Payroll Staff, HR, System Admin) and **supervisor**; it applies at their next sign-in
  - **Guard rails:**
    - nobody changes their own role
    - at least one System Admin always stays
    - a supervisor with a team keeps the role until the team is reassigned
    - the supervisor must hold the Supervisor role
    - no reporting loops
  - every change is logged (who, whom, before → after, when)
- **Tax tables & rates** (matrix: Tax Table/Rates, Admin only):
  - **versions** by effective month; payroll and the contribution preview use the **latest version on or before each month**
  - the **built-in version** is the sweldongpinoy.com-matched 2026 set (Pag-IBIG 1% at exactly ₱1,500)
  - the admin saves a new version starting from the numbers in force, with these fields:
    - SSS employee/employer %, min/max MSC
    - PhilHealth premium %, floor, ceiling
    - Pag-IBIG low band, low/high/employer %, cap
    - the BIR annual table (up to 8 rows: over, base tax, rate on the excess)
  - earlier months and **finalized payslips keep their numbers**; payslips name the version (`rulesetId`)
  - fixed (not editable): SSS MSC steps of ₱500 and the EC amounts
  - checks: rates 0–50% (tax 0–100%), PhilHealth must split evenly, MSC range in ₱500 steps, tax rows ascending from ₱0, one version per month
- **Refactor:** `StatutoryCalculator2026` now reads its numbers from a `StatutoryRates` version. All 142 calculator checks pass, including the 25 sweldongpinoy.com values, so the built-in results are unchanged.
- **Hand-worked check:** a December version with SSS at 5.5% on ₱25,000:
  - SSS 5.5% of the ₱25,000 MSC = ₱1,375
  - taxable ₱22,800 × 12 = ₱273,600; 15% of ₱23,600 = ₱3,540 ÷ 12 = ₱295 tax
  - net **₱22,505.00**
  - November keeps the built-in ₱22,611.25
- **Shortcut:** roles and reporting lines are process-wide (`AccessPolicy`), so they reset when the server restarts, like the rest of the local demo.
- Routes: `GET /api/admin/roles`, `POST /api/admin/roles/{id}`, `GET`/`POST /api/admin/rates`.
- Files: `StatutoryRates.java`, `StatutoryCalculator2026.java`, `MonthPayroll.java`, `CutoffPayrollService.java`, `AccessPolicy.java` (`changeRole`, `changeSupervisor`), `AppServer.java`, `web/admin-config.js`, `web/roles.js`, `web/app.js`, `web/index.html`, `web/main.css`.
- Tests: 3 more calculator checks (versions), 17 API checks (validation, versions by month, the ₱22,505 case, role guard rails, last admin, loops, log), `test/web/admin-config.test.js`, and an end-to-end section (Lia saves the December version and Eli's preview shows ₱22,505.00; Lia makes Jae a Supervisor, Jae's menu changes, then Lia changes him back).
- Results: Java **673**, browser logic **86**, end-to-end **25/25** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 23. Permanent storage for the local demo

Your choice: one JSON file.

- **Where:** `data/local-demo.json` inside `motorph-ms2` (change it with the `LOCAL_DATA_FILE` setting; an empty value keeps the old memory-only behaviour).
- **What's saved:** everything the local demo holds:
  - employees and attendance
  - leave, tickets and overtime
  - saved and released payslips, bonuses and deductions
  - schedules and salary changes
  - pay-change requests and timesheet corrections
  - HR records and checklists, departments and positions
  - government numbers, requests and audit
  - rate versions, roles and teams, and open clock-in shifts
- **When:**
  - loaded when the server starts
  - written after **every successful change** (and after a logged full view of government numbers)
  - writes go to a temporary file that then replaces the old one, so a crash can't leave half a file
  - changes are applied one at a time while saving, and stopping the server waits for a save in progress
- **If the file is damaged:** it's **moved aside** (`local-demo.json.unreadable-…`), never overwritten, and the demo starts from its built-in data.
- **Reset:** System config → **Demo data** → type `RESET` → **Reset demo data**. This restores the five built-in accounts, sample attendance, default roles and rates, and removes everything else. It can't be undone. Deleting the file while the server is stopped does the same.
- Tests and the browser test run **in memory** and never touch your file.
- **Known gap:** the server answers a change a moment before it writes the file, so if the computer loses power in that moment, the last change can be lost.
- The file holds only synthetic data, but it does include any fake government numbers typed into the demo. Treat it like the demo itself and don't put real data in.
- Checked for real in the preview: I filed a leave request, restarted the server, and it was still there. Then I reset, so your preview starts clean.
- Files: `LocalStateFile.java` (file format, safe writes, set-aside), `AppServer.java` (export/import of every store, save after changes, reset, health), `ClockService.java` and `AccessPolicy.java` (export/import), `web/admin-config.js`, `web/index.html`, `web/main.css`, `test/e2e/local-workflows.e2e.js` (memory only + reset step).
- Tests: `LocalStateFileTest` (26 checks: nothing written until a change; leave, hire, role, government numbers, schedule, finalized payslip, open shift and audit all survive a restart; reset needs `RESET` and the System Admin and is itself saved; an unreadable file is set aside), and an end-to-end reset step.
- Results: Java **699**, browser logic **86**, end-to-end **26/26** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 24. Payslip corrections

Your choices: void-or-adjust, with the System Admin approving.

- **Not yet released → void:**
  - Payroll Staff request it with a reason, and the System Admin approves
  - the payslip is kept, marked **VOID** (`PS-…-void1`), and the cutoff can be finalized again as **v2**
  - if the payslip gets released before approval, the void is refused, so the money already sent is never cancelled
- **Already released → adjust:**
  - the money has gone out, so on approval the cutoff is **recalculated** from current data and the **gross difference** goes on a later cutoff chosen in the request
  - **underpaid** → a taxable **Adjustment** earning
  - **overpaid** → an **Overpayment recovery** deduction for that one cutoff (never pushes net below zero, like other deductions)
  - the released payslip itself stays unchanged
- **Rules:**
  - Payroll Staff file corrections; the System Admin approves or rejects
  - nobody corrects or approves their own payslip
  - one pending correction per payslip
  - the target cutoff must be later and not already finalized
  - everything shows in the corrections table with its result
- **Panel:** Payroll processing → **Payslip corrections**. The history table marks **VOID** and **v2**, and opening a voided payslip shows who voided it and why.
- **Hand-worked checks:**
  - Jae's released Nov 1–15 payslip paid 8 h on 3 Nov. Noa corrects that row to 08:00–12:00 (4 h), so he was overpaid by 4 h × ₱148.81 = **₱595.24**. December 1–15 now carries a ₱595.24 recovery, and December 16–31 doesn't.
  - In the browser test, Jae's released Oct 1–15 payslip predates his Tue–Sat schedule. Monday 5 Oct is now a rest day, so he was underpaid by the 30% premium: 144 min × ₱148.81 ÷ 60 = **₱357.14**, added to December 1–15.
- **Shortcut to confirm with the mentor:** the difference is taken on **gross** pay and recovered or paid through the later cutoff's normal tax and contributions; there's no separate recomputation of the old month's withholding.
- Files: `PayslipCorrections.java`, `SavedPayslipStore.java` (voided archive, versions), `PayrollAdjustments.java` (one-cutoff recovery), `MonthPayroll.java`, `AccessPolicy.java`, `AppServer.java` (routes, history, data file), `web/payroll-processing.js`, `web/payroll-cutoff.js`, `web/app.js`, `web/index.html`.
- Older data files (without the new sections) still load.
- Results: Java **718**, browser logic **87**, end-to-end **27/27** (run twice), build OK, `npm audit` 0 vulnerabilities.

## 25. Roles in Firebase mode

Your choice: act as a person inside your own sandbox (not a shared company login).

- **After the Firebase sign-in**, a chooser lists the fake people in your own sandbox (Mara, Noa, Jae…), each with their role. Picking one opens that role's workspace, with the same menus, labels and pickers as the local demo.
- **The server checks the role**, the same way as locally:
  - the browser sends `X-Demo-User`, and the server checks that the person is in *your* sandbox
  - the role comes from the server's own table (`AccessPolicy.java`), never from the browser
  - so an Employee sees only their own attendance and can't file leave for someone else, while HR sees everyone
- **New, empty sandbox:** the chooser offers **Add synthetic demo people** (writes the fake people and attendance) or **Open the empty sandbox** (no writes, no role, like before).
- **Switch account** reopens the chooser without a page reload, because a reload would sign you out of Firebase (in-memory sign-in). The chooser also has **Sign out**.
- **No Firestore rules change.** Every Firebase user still owns all of their own sandbox data, so a role here only shapes what this app's server allows. It is not data security between people.
- **Still local-demo only in Firebase mode** (they need new Firestore rules): leave approval, overtime, schedules, timesheet corrections, pay changes, HR tools, government details, payroll processing, saved history and corrections, clock in/out, and System config.
- Files: `AppServer.java` (`actorFor`, `handleDemoAccounts`), `web/app.js` (account chooser for both modes), `web/index.html`, `web/roles.js` (comment), `test/e2e/firebase-auth-lifecycle.e2e.js` (opens the empty sandbox after sign-up).
- Tests: 9 more Java checks with the fake Firestore server:
  - the account list needs the sign-in and reads only your sandbox
  - acting as someone not in your sandbox gets `401`
  - Employee vs HR attendance scope
  - refused leave writes nothing
- Results: Java **727**, browser logic **87**, local end-to-end **27/27** (run twice), build OK.
- **Not run:** the live Firebase browser test (`npm run test:firebase-auth`). It creates and deletes a throwaway account on your real Firebase project, so you need to run it yourself.

## 26. Requirement quick wins (from the MO-IT108 Requirements Analysis)

Compared the app with requirements 1–10 in the course documents (`uploads/`), then built the quick wins.

- **Req. 2: clock-out pop-ups.**
  - Clocking out always asks first ("Clock out now?").
  - Before the shift ends it asks again, warning that it counts as undertime.
  - Saying no keeps you clocked in. The server now sends `shiftEnd` with the clock status.
- **Req. 3: attendance summary.**
  - The attendance page shows, for the chosen range: days present, absent and tardy, approved overtime, undertime, leave taken, and the remaining leave credits.
  - With no range, it covers the earliest to the latest record.
  - It uses the same schedule, 10-minute grace and 8-hour split as payroll (`AttendanceMetrics.java`).
  - **Absent** = a scheduled workday up to today with no attendance row, not a holiday and not on approved leave.
  - **Tardy** = time in after the shift start + 10 minutes.
  - **Undertime** = short of 8 paid hours on a workday.
- **Req. 5: queue position.** Open tickets show "#N in queue": their place in that queue, oldest first, counting everyone's tickets. Cancelled or resolved tickets drop out, and the rest move up.
- **Req. 6/7: team roster.**
  - New **Contact** column: a synthetic work email at `@motorph.example` (a reserved test domain) and a desk extension.
  - New **Today** column: on leave today, an upcoming leave, or available, following approved leave.
  - The leave calendar markers now name the person ("Jae · Vacation") and label pending or rejected requests.
- **Req. 8: HR employee statistics.** At the top of HR operations, for HR and the System Admin:
  - headcount and status
  - counts by department and by branch
  - new hires and separations this year
  - onboarding and offboarding queues
  - open HR tickets
- **Req. 9: group incentives and deductions.**
  - In Payroll processing, **Apply to** can be everyone, a team, a branch or a department.
  - Each person gets an ordinary item, which can be removed on its own.
  - Skipped (with the reason shown): the person filing it, anyone without a pay profile, separated staff, and already finalized payslips.
- **Req. 10: payroll summary report.**
  - Gross, deductions, allowances and net per department or per branch for a month, with a total.
  - Only finalized payslips count, and voided ones are left out.
  - New payslips now record the branch.
- **Fixed along the way:** ticket queue order compares times, not text. ISO times with fractions of different lengths sort wrongly as text, and this made a test fail randomly.
- **Outdated text updated:**
  - the Overview build tracker (it said payroll was "gated")
  - the HR page heading and notice ("not an HR authorization role")
  - the "still outside" list
  - a note on `docs/peer-parity-roadmap.md` that it's an old snapshot
- **Hand-worked checks:**
  - Mara, 1–15 Oct (Mon–Fri): rows on 1, 2 and 15 Oct out of 11 workdays, so 3 present and 8 absent. None was after 08:10, so 0 tardy. Every row is 8 h or more after the meal hour (07:58–16:59 is 8 h 1 min), so 0 undertime and 2 extra minutes.
  - Unit week (Mon 2 Nov, "today" Thu 5 Nov): Monday 08:15–17:00 is 15 min tardy and 15 min undertime. Tuesday 08:00–19:00 with 60 approved minutes is 60 overtime + 60 not approved. Wednesday is absent, Thursday is on leave, and Friday hasn't happened yet.
  - Summary: Jae's Nov 1–15 payslip has deductions of 2,190.48 + 1,250.00 − 2,866.67 = **₱573.81** (statutory 273.81 + loan 300.00), under Operations / Pasig.
- Files:
  - Java: `AttendanceMetrics.java` (new), `AppServer.java`
  - web: `web/attendance-metrics.js` (new), `web/team-roster.js` (new), `web/my-day.js`, `web/hr-tools.js`, `web/payroll-processing.js`, `web/app.js`, `web/index.html`, `web/main.css`
  - tests: `AttendanceMetricsTest.java` (new), plus new checks in `AppServerIntegrationTest`, the web tests and the browser test
- Results: Java **753**, browser logic **96**, end-to-end **28/28** (three runs), build OK.
- **Shortcuts to know:**
  - Contact details are made from the name, because the directory has no contact fields yet.
  - Firebase mode counts attendance with the default Mon–Fri 08:00 schedule.
  - The roster's "Today" uses the browser's date.

## 27. Simulated 2FA, QR onsite check-in and payslip emails

Your choice: simulated versions of the three heavy requirements (they would need a phone/SMS service, a branch scanner and a mail server).

- **2FA (Req. 1, security):**
  - After you choose an account, the server "texts" a **6-digit code** to the phone ending in the employee's last ID digits. The demo shows the code on screen, labelled as demo-only.
  - The right code within 5 minutes signs you in. A wrong code says how many tries are left; after 3 wrong codes you choose the account again.
  - The server now checks a **session token** on every request, so sending just the account name no longer works (`401`).
  - Reloading the page keeps you signed in while the session is valid. **Switch account** signs out.
  - Firebase mode keeps using the Firebase sign-in.
- **QR onsite check-in (Req. 2):**
  - Clocking in from the app counts as **Offsite** and shows a **QR code** (`MPH-XXXXXX`, valid for 10 minutes).
  - **Simulate scan at branch** stands in for the branch kiosk: the shift becomes **Onsite** at the scan time, and the day stays marked onsite after clock-out.
  - An expired code offers **Show a new QR code**.
  - The picture only looks like a QR code; it can't be scanned (no QR library).
- **Payslip emails (Req. 10):**
  - Releasing pay emails each payslip to the employee's work address (net pay, pay method, and a reminder to file a payroll inquiry if something is wrong).
  - The new **Payslip emails** panel on the Payroll page shows them. Employees see their own; Payroll Staff can **Simulate mail outage**, **Resend** one email, or **Retry failed emails**.
  - Every attempt is counted. Nothing really leaves the computer.
- **Fixed along the way:** the ticket queue now keeps filing order when two tickets get the same time (the Windows clock can tick coarsely), which made a test fail now and then.
- Files:
  - Java: `TwoFactor.java` (new), `PayslipOutbox.java` (new), `ClockService.java`, `AppServer.java`
  - web: `web/payslip-outbox.js` (new), `web/my-day.js`, `web/payroll-processing.js`, `web/app.js`, `web/index.html`, `web/main.css`
  - tests: Java test helpers now sign in with 2FA, plus new 2FA, QR and outbox checks; web tests; the browser test types the code, scans the QR and retries a failed email
- Results: Java **784**, browser logic **99**, end-to-end **29/29** (five runs), build OK, `npm audit` 0 vulnerabilities.
- **Shortcuts to know:**
  - The 2FA code is shown instead of sent.
  - Sessions are in memory, so a server restart signs everyone out.
  - The QR picture isn't scannable, and the scan is a button.
  - Email addresses are made from names.

## 28. Passwords, security and payroll policy settings, audit log, backups, branch pages

The rest of the course requirements that don't need Firestore rules.

- **Passwords (Req. 1):**
  - Signing in is now account, then **password**, then the 2FA code.
  - Every demo account starts with the shared demo password (written in SETUP.md). Change it in **Overview → My account**; changing it signs out your other sign-ins.
  - Passwords are stored only as **salted PBKDF2 hashes** in the data file.
  - Too many wrong passwords **lock** the account (5 tries, 15 minutes by default).
- **Security settings (System Admin, System config):**
  - minimum password length, wrong passwords before the lock, lockout minutes, 2FA code minutes, and session hours (a session now ends after it)
  - per account: password status, **Unlock**, and **Reset password** (back to the demo password, which also signs that account out)
- **Payroll policy (System Admin):**
  - grace minutes, hours per month (the hourly-rate divisor), paid hours a day, and night differential %
  - **defaults = the mentor's rules** (10 / 168 / 8 / 10), with a **Back to the mentor's rules** button
  - it applies to pay not yet finalized, and each payslip now records the policy it was made with
  - overtime, rest-day and holiday rates follow the Labor Code and are not editable
- **Audit log (System Admin):**
  - every change request and sign-in attempt: who, when, what, target and result
  - **never the request contents**, so no passwords or government numbers
  - filter by account, path, or refused only
  - the newest 1,000 entries are kept; older ones are **archived** to `data/archive` (the requirements' "archival of older logs")
  - it survives a demo reset and a backup restore
- **Backups (System Admin):**
  - automatic copies in `data/backups` (at most every 30 minutes of changes), plus **Back up now**; the newest 20 are kept
  - **Restore** (type RESTORE) keeps a safety copy first
  - if `data/local-demo.json` is ever damaged, the server starts from the **newest readable backup** instead of the demo data
  - saving also retries for a moment when Windows briefly holds the file (antivirus, an editor)
- **Branch pages (Req. 1):**
  - after sign-in, the top bar shows your branch and Overview has **Your branch**: people, active, on leave today, clocked in now, onsite today (counts only, no names)
  - Payroll Staff, HR and the System Admin see every branch
- **Hand-worked checks:**
  - With 160 hours a month, Jae's rate is 25,000 ÷ 160 = **₱156.25** (148.81 with 168).
  - With no grace period, Mara's 08:01 and 08:03 rows count as tardy: 2 days, 1 + 3 = **4 minutes**.
- Files:
  - Java: `AccountSecurity.java`, `AuditLog.java`, `PayrollPolicy.java` (new); `LocalStateFile.java` (backups, archive, retry), `TwoFactor.java` (code and session times), `PayrollProfile.java`, `WorkSchedule.java`, `SalaryHistory.java`, `CutoffPayrollService.java`, `AttendanceMetrics.java` (read the policy), `AppServer.java`
  - web: `web/admin-security.js`, `web/branches.js` (new), `web/app.js`, `web/index.html`, `web/main.css`
  - tests: `AuditLogTest.java` (new), a new security/audit/backup/policy/branch block in `AppServerIntegrationTest` (including recovery from a damaged file), web tests, and the browser test (password step, password change, admin panels)
- Results: Java **837**, browser logic **102**, end-to-end **30/30** (three runs), build OK, `npm audit` 0 vulnerabilities.
- **Shortcuts to know:**
  - The demo password is shared and written in SETUP.md (fine for fake accounts only).
  - The payroll policy has one version, not effective months.
  - Branch pages are a panel, not a separate site per branch.

## 29. Attendance tags, day-type prompt, roster Present/Absent, notifications

From the System Design document's scenarios (Clock In/Out module, Team Management and the "notifies the employee" steps).

- **Attendance tags:**
  - Clocking in tags the day **Present** or **Tardy** (time in against the shift start + grace).
  - Clocking out adds the worked-hours tag: **Regular**, **Overtime** (approved overtime worked) or **Undertime** (short of the paid hours on a workday). Both show on the time clock.
  - The tags use the same rules as payroll and the attendance summary.
- **Day-type prompt:** clocking in on a **rest day, a holiday or a day of approved leave** asks first ("Today is a Rest day for you. Clock in anyway?"). Saying no doesn't clock you in.
- **Team roster "Today":**
  - **Present / Tardy** with the times (onsite or offsite while clocked in), **On leave**, the rest day or holiday, **Absent** once the grace period passes with no clock-in, or **Not in yet**
  - an upcoming approved leave is still mentioned
  - it covers the people your role can see
- **Notifications:**
  - a 🔔 bell in the top bar with the unread count; opening it lists them and marks them read
  - the person affected is told about schedule changes, timesheet corrections, leave, overtime and pay-change decisions, resolved tickets, and released pay
  - nobody is notified about their own action
  - kept in the data file, newest 100 per person
- **Not changed:** the QR scan doesn't recompute the tag. The tag follows the clock-in time, because that's the time payroll pays from. The document says "recomputes"; confirm with the mentor if the onsite scan should decide tardiness instead.
- Files:
  - Java: `Notifications.java` (new), `AppServer.java` (clock tags, `dayTypeOf`, `/api/team/today`, notifications at each event)
  - web: `web/notifications.js` (new), `web/my-day.js`, `web/app.js`, `web/index.html`, `web/main.css`
  - tests: new integration checks (leave, overtime, schedule and pay notifications; no self-notification; mark read; clock tags; roster today), web tests, and the browser test (tags, bell, roster)
- Results: Java **847**, browser logic **105**, end-to-end **30/30** (three runs), build OK, `npm audit` 0 vulnerabilities.
- **Shortcut to know:** the clock tests can't pick the date or time (they use the real clock), so the Present/Tardy check compares against the actual clock-in time, and the day-type prompt is accepted if it appears.

## 30. The remaining System Design items

The rest of the System Design document's scenarios and permissions table.

- **Sign-in (Login module):**
  - A new sign-in **ends that account's older sessions**.
  - **Progressive throttling:** after the 2nd wrong password in a row, the next try must wait 1 second, then 2, 4, 8… A refused early try doesn't count. Five wrong passwords still lock the account.
- **Branch dashboard:** every role now lands on **Overview** (the branch page) after signing in.
- **Salary tracker:** says **"Estimate as of [date time]"**.
- **Supervisors edit team earnings** (permissions table: Earnings/Deductibles R,U for the team): a supervisor can add or remove earnings and voluntary deductions for **their direct reports**, never their own. Group items, release and reports stay with Payroll Staff.
- **Address and birthdate** (the table's Address entity, and the onboarding scenario):
  - HR and the System Admin record them, in the new **Address & birthdate** panel or during onboarding
  - everyone else sees only their own, in Overview → My account, and the employee is notified of changes
  - synthetic values only; the birthdate must make the person 15–100 years old
- **Single-day and multi-day schedules:**
  - a supervisor gives a direct report a **different shift start or a day off** on chosen dates (up to 31 days), on top of the weekly pattern; the newest change wins
  - pay, attendance counts, tags and the roster all use the schedule for each date
  - the employee is notified
- **Rate changes wait for approval** (Payroll Supervision, "Apply Global Adjustment"):
  - Payroll Staff now have a **Tax tables & rates** page and **propose** a version, which waits as **Pending**
  - the System Admin **approves** (it becomes a rates version) or rejects it; nobody approves their own
  - the admin can still save a version directly
- **Allowance corrections** (the rice-subsidy dispute scenario): **Rice subsidy / Phone allowance / Clothing allowance adjustment** for one cutoff are **non-taxable**. They're paid on top of net, outside gross, contributions and tax, and listed under allowances on the payslip.
- **Distribution:**
  - each payslip email carries a **PDF payslip**, downloadable from Payslip emails
  - a release also sends a **collated report** to the payroll team
  - the batch shows **Distributed** once every email is sent (or how many failed)
- **Hand-worked checks:**
  - **Day changes:** Jae's Tue–Sat week of 12–16 Oct has no rows, so Tue–Fri count as **4 absent**. A day off on Wed 14 Oct makes it **3**. An extra Monday shift makes it **4**, and removing that change goes back to **3**.
  - **Rice subsidy:** a ₱200 correction on Eli's 1–15 Oct cutoff raises net by exactly **₱200.00**, with gross and tax unchanged.
  - **Throttling:** 1st wrong password, no wait. 2nd, wait 1 s. Trying at once is refused (429). 3rd, "2 tries left", wait 2 s. 5th, locked.
- Files:
  - Java: `PersonalDetails.java`, `RateProposals.java`, `PayslipPdf.java` (new), `AccountSecurity.java`, `AccessPolicy.java`, `WorkSchedule.java` (day changes), `CutoffPayrollService.java`, `AttendanceMetrics.java`, `MonthPayroll.java`, `PayrollAdjustments.java`, `PayslipOutbox.java`, `AppServer.java`
  - web: `web/admin-config.js`, `web/supervisor-tools.js`, `web/hr-tools.js`, `web/payroll-processing.js`, `web/payslip-outbox.js`, `web/my-day.js`, `web/roles.js`, `web/app.js`, `web/index.html`
  - tests: Java integration checks for each item (including the PDF's cross-reference offsets), web tests, and the browser test (landing page, PDF download, Distributed)
- Results: Java **893**, browser logic **110**, end-to-end **30/30** (three runs), build OK, `npm audit` 0 vulnerabilities.
- **Shortcuts to know:**
  - The PDF is plain text, one page, with "PHP" instead of ₱ (the standard PDF fonts have no peso sign).
  - Throttling waits reset when the server restarts; lockouts don't.
  - The design's "multi-window 2FA codes" (several valid at once) is still a single code.

## 31. Overview page and sign-in animation

- **Overview → "Your pages":** one card for each page your role can open, using the same names and numbers as the menu (e.g. an Employee sees My attendance, My requests, Team, My pay). Each card says what's there, shows a live count where there is one, and opens its page on click. It replaces the old summary cards, which still said payroll was "On hold". The workstream status (build tracker) stays below for now.
- **Sign-in animation:** after a sign-in (not on reload), a navy screen shows the MotorPH logo easing in, a flame-colored line sweeping across, "MOTORPH PAYROLL OFFICE" and "Welcome, [name]", then fades into the workspace (about 1.7 seconds). It never blocks clicks, and it's skipped for people who turn on reduced motion.
- **Logo quality:** the only logo available is the 308 × 235 image sent in the chat. It was rebuilt at 4× (`archive/logo-before-hd/rebuild-logo-4x.py`): only the white connected to the outside is removed (the white wheel and chrome stay), each edge pixel's real colour is taken from inside the logo so there's no white halo, then it's enlarged with a crisp outline and light sharpening and saved 720 px wide (249 KB). The sign-in card, the sidebar and the animation (back to 240 px) all use it. The old 248 px file is kept in `archive/logo-before-hd/`. A true vector (SVG) would still be sharper.
- Results: browser logic **110**, end-to-end **30/30**, build OK.

## 32. Section tabs on the desk pages, and a text-only sign-in animation

- **Section tabs:**
  - Payroll, HR desk, Team, Requests, Attendance, Employees, Reports and System config show **one section at a time** under a tab bar, instead of every section stacked down the page.
  - Payroll's tabs are Payslips · Earnings & deductions · Release & emails · Corrections · Overtime · Reports.
  - You only see the tabs your role can use (a page with a single section has no tab bar).
  - Each page remembers its last tab, and the bar stays at the top while you scroll.
  - Arrow keys, Home and End move between tabs.
- **"Needs action" badges:** a tab shows a count when it holds Approve buttons or failed payslip emails.
- **Less text:** long explanations and the page notices show their first sentence, with the rest behind **How this works**.
- **Show all sections:** a link on the tab bar shows every section at once (for searching the page or printing); click it again to go back to one at a time. The browser test turns it on for its existing steps, and a separate check tests the tabs.
- **How it's built:** one shared module, `web/page-tabs.js`. Each page lists its tabs (`data-tabs`) and each section is tagged with its tab (`data-tab`), so nothing about how a page works changed.
- **Sign-in animation:** the logo is gone; it now shows a large **MotorPH** wordmark in the brand font, the flame line, **PAYROLL OFFICE** and "Welcome, [name]".
- Results: browser logic **112**, end-to-end **31/31**, build OK.

## 33. The remaining document requirements (not the mentor's decisions)

After re-reading every course document, the requirements that don't need your mentor:

- **Scale (Requirements Analysis: "at least 100 employee records"; "payroll... a few seconds"):** a new `ScaleTest` onboards **120** synthetic employees through the API, gives each 5 worked days, then runs payroll for all of them, finalizes all 120 payslips, releases pay (120 emails), and makes the summary and a 3-page PDF report. Measured here: directory 2 ms, slowest payroll 13 ms, all 120 payrolls about 115 ms, finalizing about 95 ms, release 48 ms, summary 4 ms. Every gross matched the hand-worked 5 × 8 h × (21,000 ÷ 168 = 125.00) = **₱5,000.00**.
- **Tickets (System Design: routed by type; Open / In progress / Resolved / Cancelled):**
  - New types: **Reschedule request** (to the team lead) and **General inquiry** (to HR).
  - **Each queue now has its handler:** the employee's supervisor (Team Lead queue), Payroll Staff (payroll inquiries), HR, and the System Admin (technical support, and can step in anywhere). Before, only HR could handle tickets, and Payroll Staff couldn't even see the payroll queue.
  - The handler can **Start** a ticket (**In progress**, and the employee is notified) and **Resolve** it with a reply. An in-progress ticket can no longer be cancelled.
- **Adjustment forms (Payroll Supervision):**
  - **Add / Subtract / Replace** the rice, phone or clothing allowance, add or subtract a taxable earning, or add a deduction, for one cutoff.
  - They apply to one employee, a team, a branch, a department or everyone.
  - A form waits as **Pending** until another Payroll Staff member or the System Admin approves it. Approval applies it and keeps each person's gross, allowances, deductions and net **before and after**. Finalized payslips and the people involved are skipped.
  - This is a new **Adjustment forms** tab on Payroll. (The design's "replace withholding tax rate" example is the rate-change approval from section 30.)
- **Payroll report files (Payroll Distribution):** **Download CSV** and **Download PDF** next to the payroll summary: one line per finalized payslip, subtotals per department or branch, and a total.
- **Change history (database design "Changelog"):** every change to an employee record, field by field, with before, after, who and through what. This covers directory edits, personal details, offboarding, approved promotions and merit increases, and role changes. It's shown in HR desk → People; employees can read their own through the API.
- **Address in parts and a contact number (database design Address / Employee):** building, street, city, region, a 4-digit postal code and a mobile number replace the single address box, in HR desk → People and in onboarding. Older one-line addresses still display.
- **Other transactions (database design):** **Reimbursement** and **Cash advance** are paid with the payslip without tax, and **Cash advance repayment** is a deduction type.
- **Half-month leave calendar (Leave Tracker "bimonthly calendar view"):** the team leave calendar can show the whole month, 1–15 or 16–end.
- **HR statistics charts (HR dashboard "stats charts"):** bar charts of people by department and by branch. They use one colour, square ends and the number beside each bar; the text figures above stay as the table view.
- **Hand-worked checks:**
  - An adjustment form on Jae's 1–15 Dec cutoff replaces rice with ₱1,000 (+₱250) and subtracts ₱100 from phone, so allowances rise **₱150.00**. Its ₱50 deduction finds no room, because the earlier ₱595.24 overpayment recovery already uses all of his gross (deductions never push net below zero). So net rises **₱150.00** and gross is unchanged.
  - A ₱300 reimbursement for Eli raises net by exactly **₱300.00**, with gross and tax unchanged.
- **Left for the mentor (not built):** two Payroll/HR approvers for government numbers; leave earned over time and leave monetisation.
- Results: Java **940** (including the new scale test), browser logic **118**, end-to-end **31/31**, build OK, `npm audit` 0 vulnerabilities.

## 34. Employee record extras (compared with kmagana121/MO-IT110-MS2-Draft)

That repository is an early NetBeans/SQLite desktop draft: a login, a logout-only menu and an employee form. Our app already did everything else it does; these five came from its employee form:

- **Photo:** HR or the System Admin uploads a PNG or JPEG (up to 150 KB; the file's own bytes are checked, not its name) in HR desk → People. It shows there and in the employee's Overview → My account. Anyone who may see the employee in the directory can view it.
- **First name and surname** are separate fields in Personal details and onboarding. The directory name follows them ("First Surname"), and older records are split at the last space.
- **Gender:** Female, Male or Prefer not to say.
- **Personal email** (optional), separate from the synthetic work address.
- **Delete (System Admin only), with safeguards:**
  - you type DELETE to confirm
  - it's refused when the person has attendance, payslips, pay items, requests or tickets, or a team; offboard them instead
  - so pay records are never left without their employee, and nobody deletes their own record
  - the change history keeps a "Record deleted" note
  - (Your friend's System Design permissions table gives the System Admin delete on employees.)
- Every change (names, gender, email, photo) goes to the employee change history.
- **Not copied from that draft:** its new hires' passwords are their date of birth, some database commands are built by joining text (an SQL injection risk), and its database path is fixed to one computer.
- Results: Java **958**, browser logic **119**, end-to-end **32/32**, build OK, `npm audit` 0 vulnerabilities.

## Still not built

- **Firebase mode** for the role features that write shared or reviewed data (approvals, overtime, schedules, corrections, HR tools, payroll processing, saved history, the outbox, security and audit). Each needs Firestore rules that you publish. A real shared company login in Firebase would need a full rules rewrite.
- **Real external services** instead of the simulations: SMS for 2FA, a QR library or branch scanner, a mail server.
- **From the documents, still open:** "multi-window" 2FA codes and IP-based throttling (design details for real network clients), and, from the database design, leave earned over time and leave monetisation (they need the mentor's leave rules).
- **To confirm with the mentor:** government numbers need approval from two Payroll or HR staff in the requirements, but the app uses one System Admin approval (your earlier choice); the gross-difference shortcut for payslip corrections; allowances paid only in a cutoff with earned pay.
