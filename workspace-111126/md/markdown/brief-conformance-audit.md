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

### §4 — member notation
Zero `x_name`-style fields in ~15,000 lines. The brief asks for `ab_name` / `ab_dtName`.
Converting every identifier in a working, tested codebase is a large mechanical rewrite; it has
not been attempted.

### §12 — the UI components
Nothing from the CodePen references is in the build:

| § | Component | Status |
|---|---|---|
| 12.1 | Landing page (Login / Contact Help) | **missing** — `/` is the sign-in card. `/help` exists as a page, not as a landing CTA |
| 12.2 | kubai-icons, Animatopy, Dynamic.css | not used |
| 12.3 | 3D push-button sidebar | nav is flat text |
| 12.4 | Collapsible sidebar + hover icon flyout | missing |
| 12.5 | Payslip presentation | logo, details and Print present; referenced layout not used |
| 12.6 | Push-style logout button | plain "Switch account" |
| 12.7 | Isometric "Your Pages" tiles | flat cards |
| 12.8 | MotorPH/Sweldong switch | text links, not the switch component |
| 12.9–12.11 | Header widget, layered colour, background | missing |

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
