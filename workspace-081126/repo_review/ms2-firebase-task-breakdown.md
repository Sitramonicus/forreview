# MotorPH MS2 Program — Major Tasks and Subtasks

**Goal:** Build an original, browser-accessible MotorPH payroll web app with an HTML/CSS interface and Java OOP backend, using Firebase for required persistence. It should load the selected employee and attendance data, calculate payroll in Java, display a payslip/result, and save it when required by MS2. A hosted deployment means end users do not install a desktop app; the Java backend and data services still need to be hosted.

**Starting point:** You confirmed there is no MS1 project and authorized selective reuse of useful code from other teams, while preferring a purpose-built original rather than submitting one team's project. Build a **new Java web app**: HTML/CSS in the browser, Java for the backend and payroll logic. Borrow patterns/components only when they fit; keep source attribution/provenance clear and follow course reuse/disclosure rules.

**Platform decision:** Use a browser UI (HTML/CSS, with JavaScript only where needed for browser interactions) backed by Java. This matches your preference for no desktop install and future browser access while keeping Java central to the course work. Once hosted, users access it by URL; hosting and secure backend setup are still required.

## Current implementation checkpoint

- Project folder: `/home/user/motorph-ms2/`.
- Running browser preview: Java 11 server on `0.0.0.0:8080`, serving the Vite-built frontend from `dist/`; optional Vite hot reload proxies to the Java API.
- Employee directory and attendance use Java service/store boundaries. Local mode uses in-memory fixtures; configured Firebase mode uses Firestore REST with a Firebase Auth ID token forwarded from the browser. No Admin SDK or service-account key is used.
- Firebase project `motorph-jd` / web app `motorphdev` is configured. Email/Password Auth and live Firestore are verified using disposable synthetic accounts. The user published the candidate per-user development rules. Authenticated app seeding created 3 synthetic employees and 6 attendance records; repeat seeding created 0 duplicates. Employee list/search/detail and raw inclusive attendance queries passed. Temporary records and Auth accounts were deleted.
- `.env.local` contains the user-supplied Firebase Web API key and is listed in `.gitignore`; the user does not need to edit it. This workspace is not a Git repository. The published rules restrict the app to each user's synthetic employee/attendance paths; payroll paths and extra payroll fields remain denied.
- The app includes Firebase sign-in/account creation, synthetic seeding, employee search and keyboard-accessible detail, raw attendance queries with inclusive user-editable dates (defaulting to the selected employee's earliest/latest available record dates per the legacy MO-IT108 requirement), and a gated Payroll view. It does not calculate work hours, deductions, or pay. The provided source synthesis gives no complete attendance-hour algorithm or exact semi-monthly cut-off calendar, and the user requires calculations to remain disabled.
- Automated suite: 13 employee-domain + 16 attendance-domain + 15 Firestore REST adapter + 23 HTTP integration checks = **67 passing checks**. Live API checks also confirmed repeat-seed idempotency; employee list/search/detail; date boundary and empty-range behavior; 404 unknown employee; 400 reversed range; 401 unauthenticated; 403 UID spoof, payroll collection, and extra salary field. Headless Chromium passed real Firebase sign-in, employee list/search/detail, date-filtered raw attendance, and payroll gate with no console/page errors; responsive layout passed at 320–1024 px. Temporary test users/data were cleaned up.
- Localhost browser sign-in is verified. The exact Arena preview-origin auth is **not yet verified**: the E2B proxy returned 403 when contacted from this sandbox because its traffic-access-token header is only injected for the user's preview session. If Firebase reports `auth/unauthorized-domain` in that preview, add the exact current preview host (`8080-is3s38ic4lxn7e4e77g8b.e2b.app`) under Authentication → Settings → Authorized domains. The user must make that Console change if required; no rules or Firebase Console changes were made by the assistant.
- Earlier MO-IT108 Requirements Analysis/System Design PDFs were reviewed as legacy context. They clarify that the attendance viewer defaults to the employee's earliest/latest available records and describe a much broader workflow; they still do not supply a complete hour/payroll algorithm. That safe default is implemented. `The Code.pdf` is unrelated C++ inventory code and was not reused. The crosswalk and rough parity estimate are in `/home/user/motorph-ms2/docs/legacy-mo-it108-traceability.md`; the staged completion plan is `/home/user/motorph-ms2/docs/peer-parity-roadmap.md`.
- The server is a development prototype, not production hardened. Responsive QA found and fixed hidden-button and narrow/tablet layout overflow; the production bundle was rebuilt and rechecked.

## Checklist — current state

| # | Major task | Status | Evidence / blocker |
|---|---|---|---|
| 1 | New app foundation and Firebase proof | **Complete** | Live Firebase Auth and user-scoped Firestore read/write passed through the Java API; 3 employees + 6 attendance seeded, idempotency verified, and cleanup passed. Unauthenticated/cross-user/payroll/extra-salary cases were denied. |
| 2 | Employee sign-in and selection | **Complete on localhost; preview-origin check pending** | Real Firebase browser sign-in, employee list/search, and keyboard detail selection passed at `localhost:8080`. The Arena preview host may need adding to Firebase Authorized Domains if it reports `auth/unauthorized-domain`. |
| 3 | Attendance and pay-period inputs | **Input/query path complete; derived hours gated** | Selected employee + user-chosen inclusive dates return raw attendance records; empty and reversed ranges behave correctly. No default pay-period calendar or hour/payroll calculation was invented. |
| 4 | OOP payroll calculation engine | **Blocked by source/rule gate** | Do not implement guessed formulas; the supplied sources lack complete arithmetic/rates, attendance-hour rules, and an expected-value example. |
| 5 | Payroll run, payslip, and result storage | **Not started / gated** | Depends on approved calculation rules and required MS2 output behavior. |
| 6 | Browser workflow | **Complete for Tasks 1–3 scope; payroll intentionally gated** | Firebase sign-in, employee selection/detail, raw attendance filtering, and safe mobile/tablet layout passed in headless Chromium. No full payroll run-to-payslip flow is enabled. |
| 7 | Optional MO-IT107 features | **Not started** | Leave, benefits, role workflows, summaries, and archival remain outside confirmed MS2 scope. |
| 8 | Test, secure, and prepare evidence | **Verified for Tasks 1–3; submission hardening remains** | 67 Java checks, production build, live Firestore authorization checks, browser E2E, and 320–1024 px responsive checks pass. Preview-origin authorization and production/deployment hardening remain external to this workspace. |

## Program flow we will build

**Sign in (only if required) → select employee and pay period → load attendance → calculate payroll in the OOP backend → display payslip/result → save the result to Firebase.**

This is the first complete version. Leave the full MO-IT107 scope—every table, all five actors, annual archive/deletion, and dual-approval PII workflows—out unless the MS2 rubric explicitly requires it. Keep the browser UI visually distinctive but restrained and professional.

## Web UI direction and reuse rules

- Use a browser UI so users can access the deployed app by URL without installing a desktop program. This still requires a hosted Java backend and Firebase services; it is not just a standalone HTML file.
- Visual target: a **professional payroll/office interface with a restrained non-modern GUI character**—not a stock glossy AI dashboard and not a game UI. Use clear hierarchy, practical tables/forms, readable type, calm colors, and subtle period-appropriate details. Avoid glassmorphism, shader/glow effects, gratuitous gradients, arcade pixels, game icons, and Poolsuite/NES branding or artwork.
- Treat the links you shared as inspiration and possible component sources, not a mandate to combine every framework. NES.css and PSone.css are too game-coded as whole visual systems for this payroll app; Poolsuite can inform mood/color/layout only, not copied brand assets. Consider system.css / classic.css for restrained control motifs, Carbon for enterprise interaction/accessibility patterns, and the retro-design-system as a catalogue to selectively reference—not necessarily import wholesale.
- Before reusing code, CSS, fonts, icons, or images, inspect that exact asset's license and dependencies. Reuse only what the license permits, retain required notices, and document the source in an attribution/third-party notices file. Prefer one small compatible base (or original CSS informed by references) over mixing several competing design systems.
- The first prototype now adapts only the fine-rule title-bar motif from system.css (MIT), recolored and scaled for the employee detail dialog; its notice is recorded in the project. No external fonts or assets are included.
- Keep the interface responsive, keyboard-usable, and accessible; retro character must not impair payroll data legibility or form usability.

## Design approach: borrow patterns, not whole projects

- **F9:** follow the progression of testing the OOP backend before connecting the web UI; use as a reference for Employee/Attendance/PayrollCalculator/PayrollRecord responsibilities.
- **F6:** take the compact model/service/calculator separation and focused test discipline.
- **F4:** borrow the clean boundary between web UI, controller/service, model, and data access—only as much as our actual feature path needs.
- **Do not use F3 as a parser/calculation base:** its naive CSV split misreads quoted commas in its bundled employee file.
- **Do not treat F2 and F5 as two implementations:** they are near duplicates; real database writes/payroll behavior were not verified.
- No single reference dictates the final class list. The approved MS2 rubric is the authority; the project should have original wiring, naming where appropriate, and tested behavior.

## Major Task 1 — Create the new app foundation and prove Firebase access

1. Confirm the MS2 rubric and required outputs; set up a fresh isolated project rather than cloning an entire other team's repo.
2. Choose the course-compatible Java version/build setup; the prior tests used Java 21, but that does not prove MS2 requires Java 21.
3. Create a development-only Firebase project and use synthetic data.
4. Use **Firestore** as the provisional database; only switch to Realtime Database if the rubric/data flow makes a simple tree a better fit.
5. Test the chosen web-to-Firebase path with Firebase Authentication and Firestore Security Rules. Firestore REST accepts Firebase ID tokens and applies rules to those requests [1]. Never put an Admin/service-account key in browser code or a distributable client; if any privileged credential is used server-side, keep it only in protected server configuration.
6. Enable only the required authentication method; verify one synthetic write/read and an unauthorized request being denied.
7. Add a tiny configuration boundary for project ID/API key; do not commit passwords, refresh tokens, or privileged credentials.

**Done when:** the browser app and Java backend can authenticate a test user and read/write one synthetic record through the intended rules. If the MS2 rubric or deployment environment disallows the approach, revise before building on it.

## Major Task 2 — Employee sign-in and employee selection

1. Use the existing Firebase Email/Password sign-in flow as required for this prototype; do not use a real staff account.
2. Define the employee fields from the MS2 rubric and agreed data map—not every field from the 29/30-table PDFs.
3. Store/read synthetic employee documents in `users/{uid}/demoEmployees`; Firestore Rules scope each account to its own demo collection.
4. Show employee search/selection using the available synthetic identifier.
5. Display the available synthetic profile/job fields; employee create/update is not required by the current plan.
6. Handle wrong ID, missing employee, permission denial, and connection failure clearly.

**Done when:** the signed-in test user can select a synthetic employee and see the expected demo fields. This passed in the browser on localhost; the current Arena preview host's Firebase authorized-domain setting remains to be confirmed if sign-in reports `auth/unauthorized-domain`.

## Major Task 3 — Attendance and pay-period inputs

1. Define the attendance input fields and source using the rubric/approved rules. The supplied source synthesis supports attendance ID, schedule ID, date, time in/out, day type, unique `(scheduleID, date)`, and `timeIn <= timeOut`; it does not provide a complete hour-derivation algorithm or exact semi-monthly cut-off calendar.
2. Store/read synthetic attendance in the app's user-scoped `users/{uid}/demoAttendance` collection, linked to the selected synthetic employee. Keep the approved real input source unresolved rather than importing live data.
3. Let the user select an employee and filter by an inclusive date range. Per the earlier MO-IT108 Requirements Analysis, default the range to the selected employee's earliest/latest available attendance dates; allow the user to narrow it. This is not a default payroll period. The raw-record query path is implemented and live-tested.
4. Validate request shape, date parsing, reversed ranges, missing employee, and record schema at the available boundaries. This is a read/query prototype, not an attendance-entry or supervisor-adjustment workflow.
5. **Keep hour totals disabled** until an authoritative attendance algorithm and worked expected example are supplied. Do not infer paid hours from raw time-in/time-out or treat elapsed spans as payroll hours.
6. Add clock-in/out or supervisor-adjustment web forms only if explicitly required and a source/authorization model is approved.

**Current completion:** employee + inclusive date input and raw synthetic attendance retrieval are complete. The separate derived-hour acceptance cannot be completed safely from the supplied sources and remains gated; payroll calculations remain disabled. Wrong-schema data must fail clearly rather than produce a misleading payroll result.

## Major Task 4 — OOP payroll calculation engine

1. Define the smallest useful classes, aligned with the rubric: likely `Employee`, `AttendanceRecord`/`AttendanceSummary`, `PayPeriod`, and `PayrollResult`.
2. Add `PayrollCalculator` or the equivalent responsibility from the approved class design; keep calculations out of browser/UI code and Firebase access code.
3. Write focused tests for the first supported calculation using an authoritative expected-value example.
4. Implement required earnings and deductions one rule at a time.
5. Add only the employee subclasses/interfaces that provide real behavior or are required by the approved design; do not add an inheritance hierarchy just to mirror another project.
6. Return one structured payroll result for the Java backend, browser UI, and persistence layer.

**Rule gate:** the PDFs identify overtime, undertime, holidays, contributions, tax, and leave but do not supply all formulas or current values. Do not guess these. A missing required rule blocks that component; unrelated working components can continue.

**Why reference engines are not copied as payroll authority:** Folder 6's `PayrollProcessor` implements the CP2 cutoff/deduction behavior and uses `BigDecimal`; its README explicitly says the rules were taken from CP2, not independently verified. Its tests mostly establish parity with that CP2 baseline (473/476 pay rows and 14/14 summaries). Folder 9's OOP `PayrollCalculator` has a clear worked-days × hourly-rate × 8 gross-pay path and 91 payroll/backend checks, but its accompanying `oop/matrix-verification.md` records differences from the matrices it compared against: SSS exact-bracket boundaries, a full PhilHealth premium deducted instead of the employee half, employer Pag-IBIG share deducted from the employee, and a ₱0.20 withholding-tax bracket-base discrepancy. These projects are useful architecture and test-pattern references, not proof of the rules this MS2 app must implement. The payroll UI stays gated both because the user directed that calculations remain disabled and because the authoritative MS2 formulas, effective values, rounding rules, and worked expected result are not yet approved.

**Done when:** calculation tests pass without Firebase or the web app running.

## Major Task 5 — Payroll run, payslip, and Firebase result storage

1. Connect employee lookup and attendance loading to the calculator.
2. Let the user run payroll for one selected employee and pay period.
3. Display the result and only the payslip fields required by MS2.
4. Save a result only if persistence is required; provisional location: `payrollRuns/{periodId}/payslips/{employeeId}`.
5. Read the saved result back and compare it to the calculator output.
6. Define the simplest required duplicate/re-run behavior; do not overwrite a finalized run silently.
7. Add a monthly summary only if MS2 requires it.

**Done when:** one synthetic case completes the flow and displayed/saved values match the independent calculator test.

## Major Task 6 — Build and connect the browser workflow

1. Serve a browser-based interface with HTML/CSS and only the JavaScript needed for navigation, form behavior, and requests.
2. Connect sign-in (if required), employee selection, attendance period, payroll run, and payslip view to Java backend services.
3. Keep payroll rules and authoritative calculations in Java, not duplicated in browser scripts.
4. Show clear loading, validation, permission, network, and missing-data states; keep the layout usable on common desktop and mobile widths.
5. Make the server accept the deployed preview/production host securely; do not make browser code call localhost.

**Done when:** a user can complete the required workflow in a browser and gets the same result as the backend tests.

## Major Task 7 — Optional features from the broader MotorPH design

Add these only if the current MS2 rubric explicitly requires them and the missing rules are available:

1. **Leave:** requests, approvals, balances/accrual, monetisation.
2. **Benefits and adjustments:** only approved earnings, deductions, and other transactions.
3. **Government contributions/tax:** approved rates/brackets, effective dates, formulas, and test examples.
4. **Monthly summary:** required totals and output format.
5. **Role-based access/PII:** the database overview says two-person approval, but exact roles/workflow are undefined; implement only if MS2 confirms it.
6. **Archival:** DFD labels `1YO`/`2YO` and shows copy-then-delete without a defined retention/recovery policy; keep deletion out of the first build unless approved.

## Major Task 8 — Test, secure, and prepare MS2 evidence

1. Unit-test payroll calculations independently from Firebase.
2. Test Firebase sign-in, allowed read/write, denied access, missing records, malformed data, and network failure using synthetic data.
3. Manually test the complete browser flow and one invalid/error path.
4. Compare values to the approved rule source, not just another repository's test output.
5. Capture the required design-to-code mapping, test outputs, and demo steps without exposing credentials or real employee data.
6. Check that no service-account file, password, or refresh token is committed.
7. Verify the project builds and all required tests pass.

**Done when:** required MS2 behaviors pass, evidence is ready, and no optional feature is half-built.

## Build order

1. New project + safe Firebase test-record proof.
2. Employee sign-in/selection and profile display (sign-in only if required).
3. Attendance/pay-period loading and validation.
4. Payroll engine and calculation tests.
5. Payroll run + payslip display/save.
6. Required browser-to-Java integration and error states.
7. Optional features only if required.
8. Final QA and submission evidence.

## Firebase in plain language

- **Firebase project:** the cloud workspace for the app's data and sign-in services.
- **Firestore:** a document database (collections contain documents); it is not relational, so we should model only the MS2 records.
- **Firebase Authentication:** identifies a signed-in user.
- **Firestore Security Rules:** restrict what authenticated client requests can read/write. Firestore REST requests with Firebase ID tokens are checked against these rules [1].

## Why the other submissions inform this build

- **F9:** strongest reference for OOP backend-to-interface progression; retest had 91 payroll/backend + 33 attendance + 44 leave checks. It is another team's project, not a formula authority.
- **F6:** useful test/calculator separation pattern; 167/167 checks do not prove our rules.
- **F4:** useful package-boundary pattern; its Maven run discovered zero automated tests.
- **F2/F5:** near duplicates; database navigation worked in a disposable test, but payroll writes remain unverified.
- **F3:** known quoted-comma parser defect; not a good base for data loading or expected payroll values.

## Source

1. [Firestore REST API authentication](https://firebase.google.com/docs/firestore/use-rest-api) — Firebase ID tokens authenticate requests as the user and Firestore Security Rules authorize them; service-account/OAuth requests use IAM instead.
