# Brief conformance audit

Checked against `MotorPH_Optimised_Implementation_Prompt_v2.md`, section by section.
Last updated after the fix pass.

---

## Fixed in this pass

| § | What | Was | Now |
|---|---|---|---|
| §4 + notes | Comments | 1,380 comment lines / 109 files | **0**. Each folder has a short `NOTES.md` instead |
| §10 | "Demo" type names | `DemoTicket`, `DemoLeaveRequest`, `DemoOvertimeRequest`, `DemoSeedService`, 174 refs | `LeaveTicket`, `LeaveRequest`, `OvertimeRequest`, `FirestoreSeedService` |
| §10 | "synthetic"/"fictional" | 9 occurrences incl. `seedMissingSyntheticRecords()` | **0** |
| §10 | "demo" in visible copy | 83 occurrences in UI text, a dead account picker, a whole dead 2FA flow, `DEMO-001` placeholders | **0**. Dead markup and code removed |
| §5 | Web folder shape | flat, 44 files | `b_basic/` (21), `f_flares/` (24), `sp_secpol/` (ready) |
| §4 | Web file naming | kebab-case, 31 files | PascalCase throughout |
| §16.1 | Custom 404 | plain-text 404 | branded page, real HTTP 404 status |
| §16.3 | Unique titles | one title for every route | per-route titles |
| §16.4 | Meta description / noindex | one generic description | per-page, `noindex` on private pages |
| §16.5 | Open Graph + Twitter | none | present on the entry page |
| §16.6 | Favicon set | none | `favicon.svg`, apple-touch icon, manifest |
| §16.7 | `robots.txt` | none | present, disallows all (internal system) |
| §16.8 | `sitemap.xml` | none | present, empty — there are no public routes |
| §16.11 | Privacy page | none | `/privacy`, describes actual data handling, DPO/retention/contact left blank on purpose |
| — | `test.sh` | **broken** — pointed at `ph.motorph.web`, a package deleted months ago | discovers and runs all 14 suites |
| — | Missing CSS build | `main.css` is Tailwind source; page served it raw, so the app was unstyled | `tools/css.sh` compiles to `app.css` |
| — | Blank-page bugs | `firebase/app` bare specifier, `import.meta.env`, null `createAccountButton`, `accountGate` | all removed; page renders |
| — | Broken logo | images in `web/public/` but referenced at `/` | flattened into `web/` |
| — | Client fired known-403 requests | 5 endpoints for every role | gated by `canReadCompanyArea` mirroring the server matrix |
| — | "Main Branch branch" | stutter in three labels | fixed |
| — | Sidebar footer | "Prototype data only. Not for payroll processing." | states what the system does |

**Status: 14/14 suites, 533 checks, zero failures. Live UI verified in Chromium.**

---

## Still not done

This section was written before §4 and §12 were done and had gone stale, claiming both were
untouched. Corrected below against the actual code.

### §4 — member notation — **done**
The persisted models are converted (`Employee`, `AttendanceRecord`, `PersonalDetails`,
`GovernmentDetails`, `WorkSchedule`, `Branch`, `AuditLog`, `Locality`, `ClockService`,
`PasswordPolicy`, `Account`, and the three JS modules), with 79 `@SerializedName` pins protecting
the wire keys. The remainder are per-request DTOs that never reach the state store; renaming them
would be churn with no risk reduction. See `docs/a_doc/member-notation.md`.

### §12 — the UI components — **done**

| § | Component | Status |
|---|---|---|
| 12.1 | Landing page (Login / Contact Help) | **done** — `/` is the landing page; two 3D push CTAs |
| 12.2 | kubai-icons, Animatopy, Dynamic.css | **not used, on purpose** — icons are inline SVG so the page has no external requests under CSP; the animation is CSS |
| 12.3 | 3D push-button sidebar | **done** — `.push-btn` / `.push-inner`, including SIGN OUT |
| 12.4 | Collapsible sidebar + hover icon flyout | **done** — `.rail-toggle` collapses the rail; `.rail-flyout` on hover |
| 12.5 | Payslip presentation | **done** — letterhead, detail grid, print stylesheet |
| 12.6 | Push-style logout button | **done** |
| 12.7 | Isometric "Your Pages" tiles | **done** — `.tab-cards.iso` |
| 12.8 | MotorPH/Sweldong switch | **done** — the brand switch component |
| 12.9–12.11 | Header widget, layered colour, background | **done** — pulse widget, `.land-bg`, layered gradients |

Verified in Chromium at 1280×720: `.rail-toggle` visible, nine nav icons, one `.push-inner` in
the top bar, notification bell present, landing hero and both CTAs rendering.

Two defects were found and fixed during that check:
- `Index.html` had a mangled heading left by an earlier bulk edit — two `<h2>` open tags, a literal
  `nbsp;` in the text and a duplicated closing tag, plus an unbalanced `</aside>`. It rendered by
  browser error recovery, but it was invalid markup and put two headings in the DOM.
- The landing page's payslip card claimed **405 employees**; the dataset has 34. A static demo
  number that contradicted the real data on the very first screen.

### §5 — quality tooling
No ESLint, Checkstyle, SpotBugs, PMD, Lighthouse, or dependency audit. 14 Java suites run; nothing lints.

### §10 — no attendance in the shipped store
`data/local-demo.json` has `attendance.records: []`. The 34 accounts and the salary history are
there; the timecard is not. So a fresh checkout opens to an empty payslip. The 10 rows the sample
timecard describes (10015, December) have to be entered through `/api/attendance`, which is the
honest route — but it means the payslip demo needs a manual step. Worth flagging rather than
quietly seeding.

### §16.10 — states
Loading and error text exists in places. No `aria-live` regions, no double-submit guard on every
form, no skeletons. Partial.

### §16.12 — analytics
Not implemented. No provider chosen, so no integration point was invented.

---

## Notes on judgement calls

- **`web/sp_secpol/` is empty.** The brief names it, but the client-side security UI that exists
  is already inside `b_basic`. Making an empty folder and moving things into it for the sake of
  the shape would be worse than leaving it ready.
- **`Styles.css` and `Workflow.css` are dead.** They predate the Tailwind theme. Left in place
  because deleting them is a separate call; nothing links to them.
- **Privacy page details are blank on purpose.** §16.11 says do not invent a DPO, retention
  period, or contact address. It says so in the page itself.

### §16 — empty, error and permission states, swept (2026-11-08)

Run against the live server in Chromium, one pass per role, walking every nav item a role can
reach and recording any HTTP status ≥ 400, any blank or still-loading panel, and any page error.

| Account | Views reached | Results |
|---|---|---|
| 10005 System Admin | all 9 | clean after the fix below |
| 10006 HR | 7 | clean |
| 10011 Payroll | 8 | clean |
| 10015 Supervisor | 5 | clean |
| 10018 Employee | 5 | clean, correctly scoped |

Two defects were found and fixed:

1. **The System Administrator could not read attendance, payroll, earnings or reports.** The
   design document's matrix gives the System Admin `C, R, U, D` on Attendance and read on Payroll
   and Payslip, but `AccessPolicy` granted `Area.EMPLOYEES` alone. Every admin attendance screen
   came back 403 — the view rendered 8 rows instead of 220. `AccessPolicyTest` now pins all four
   grants (49 checks, up from 43) so the matrix cannot drift again silently.
2. **The adjustment-forms panel loaded for every role** but requires `Area.REMITTANCE`, so an HR
   or Supervisor session logged a 403 on every sign-in. It is now requested only when
   `canReadCompanyArea(account, 'remittance')`.

Also corrected while in here: `/api/health`, `/api/config` and the reset response all hardcoded
`"persistence":"file"`. They now report what the store says it is (`file`, `firestore`, `memory`),
and the UI labels ("Local file store", "Saved to Firestore") come from that answer instead of
being written into the HTML.

### §16.11 — privacy placeholders, verified rather than assumed

The government-details panel masks all but the last four digits of every ID by default, and the
unmasked view is a separate audited request (`?reveal=true` is treated as a change, so it is
CSRF-checked, audited and saved). Cross-employee reads were probed directly as employee 10018:
`/api/employees/10001`, `/api/attendance?employeeId=10001`, `/api/government/10001`,
`/api/personal/10001` and `/api/reports/attendance` all return 403, while the employee's own
payslip returns 200. The refusal message names the reason ("Employee accounts cannot view payroll
for 10001") rather than failing opaquely.

The DPO, retention period and contact address on `/privacy` remain blank on purpose: §16.11 says
not to invent them, and the page says so.
