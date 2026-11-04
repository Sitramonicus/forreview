# MotorPH Payroll Build Plan

**Status:** Planning baseline; no implementation started.  
**Prepared:** 3 November 2026.  
**Working assumption:** this is a course-oriented Java/OOP payroll build, not a production payroll service. If the target is a distinct MO-IT107 database deliverable or a production system, use the relevant scope gate rather than applying the whole plan unchanged.  
**Plan sources:** `/home/user/repo_review/submission-comprehensive-report.md`; `/home/user/uploads/MotorPH_Payroll_System_Full_Synthesis.md`; the nine-submission source/test reports and retest evidence preserved in the comprehensive report.  
**Plan style:** course-first, risk-first, small vertical slices, observable checks, explicit stop conditions. Relevant planning/scope/test guidance was selected from `skills_compilation.md`; unrelated skills were not applied.

## 1. Recommendation in one minute

Build **the smallest course-compliant MotorPH payroll flow around the user's approved design and real assignment scope**. Do not select or copy a numbered repository as the user's code until its provenance, permission, and course fit are confirmed. If no usable personal baseline exists, create a small Java project that follows the actual course instructions and uses the nine repositories only as study references.

Use the MO-IT107 PDFs as **system context and a traceability source**, not as proof that every listed table, role, archive behavior, or payroll rule belongs in the current assignment. If the target is MO-IT110, keep MO-IT107 scope separate unless the current instructions explicitly combine them. The PDF set is broad and internally inconsistent; the submission reports show projects that cover subsets of it, not one implementation that satisfies it all.

**Build order:** scope and rule gate → baseline/design → one verified employee-to-payroll vertical slice → only required payroll extensions → GUI/report integration → submission QA. Keep database/RBAC/archival work behind explicit scope gates.

## 2. Goal, success conditions, and non-goals

### Goal

Produce a clear, testable, primarily object-oriented MotorPH payroll submission that implements the approved assignment/design, separates payroll calculations from presentation, and can show evidence for every implemented rule.

### Success means

1. The chosen repository and project scope are approved for this assignment; no other team's work is misrepresented as the user's.
2. Every implemented payroll/attendance/leave rule traces to an authoritative course requirement, approved design, or user-confirmed rule.
3. A representative end-to-end flow reads a supported employee/attendance input, computes a payroll result, and displays or exports it through the required interface.
4. Focused automated tests verify calculation and data behavior; GUI-only expectations have a short manual checklist.
5. The existing build and required tests pass in the course-approved environment, and no original fixtures or source files were changed during testing.
6. The project stops at the assignment's required scope—no speculative production architecture.

### Explicit non-goals unless the rubric says otherwise

- Implementing all 30 tables from the database-design PDF simply because they are pictured.
- Building a web app, REST API, microservices, event bus, cloud deployment, CI pipeline, ORM, or multiple database providers.
- Adding two-person PII approval screens, full RBAC, legal retention/deletion, or cold-storage automation before the assignment requires them and the policy is resolved.
- Treating inventory CSV as payroll input.
- Inventing salary formulas, tax rates, leave rules, timekeeping rules, or acceptance values.
- Replacing the user's own design with another repository's class structure without authorization.

## 3. What the submission review says to reuse—and what not to reuse

| Submission evidence | Use in this plan | Limits / caution |
|---|---|---|
| **Folder 9** | Best reference for tracing an OOP backend into a GUI; the report records payroll/backend **91/91**, attendance **33/33**, leave **44/44**, plus a GUI smoke check. The source describes the GUI calling a payroll backend and immutable payroll result. | Its own README maps it to another group's CP2/OOP work. It is not established as the user's approved design. The 168 checks establish those tests passed under the tested timezone; they do not prove formulas match this assignment. |
| **Folder 6** | Strong reference for a compact backend/test loop; `motorph.AllTests` passed **167/167**. | A passing suite is not an official payroll-rule oracle. Do not transplant its rates/formulas without a governing source. |
| **Folder 4** | Good example of separating view, controller, service, model, and repository responsibilities. | Six manual test mains passed in the retest, but Maven found **zero automated tests**. It also needed test-only harness/resource adaptations. Treat package separation as a pattern, not proof of quality. |
| **Folders 2 and 5** | A reference for Swing screens and database-backed navigation if the course specifically requires that style. | They are near-duplicate executable code under different labels. Disposable MariaDB-compatible login/navigation worked; real CRUD/payroll writes and native MySQL behavior remain unverified. Do not count them as independent designs. |
| **Folder 3** | Useful as a small CP1 arithmetic/procedural example only. | Naive comma splitting misreads its own quoted-comma employee CSV: **34/34** bundled gross outputs differ from the correctly parsed salary-column values. Its files also use example calculations, not authoritative policy. Do not use it as formula or parser baseline. |
| **Folders 1, 7, and 8** | Compare specific Swing/CSV/SQLite or console patterns if needed. | Course/provenance fit is uncertain or workflows/data are partial. F7 padded an incompatible file then failed mapping; F8's wrong-schema input reached a null dereference. Not a primary baseline. |

**Baseline decision rule:** preferred source is the user's own approved MS1 design / existing project, if one exists and is authorized. If not, ask the course mentor which design to implement before adapting other teams' work. The report does not establish a one-to-one mapping between any numbered folder and the supplied MO-IT107 PDFs.

## 4. Evidence from the PDFs that shapes the feature map

The synthesis reconstructs the system as a lifecycle:

1. Maintain employee, job, address, schedule, and protected government information.
2. Record/adjust attendance and manage leave/balances.
3. Validate time and leave inputs; add wages and approved adjustments.
4. Calculate payroll and government deductions; payroll staff verifies.
5. Generate payslips and monthly summary.
6. Archive older log/payslip records, then remove relevant source records.

The DFD image details matter to the plan:

- **Context and Level 1 (PDF pp. 2–3):** Employee, Supervisor, HR, Payroll Staff, and System Admin are separate actors; main processes are Employee Management, Payroll Processing, and Archival; D1–D4 are Employee Repository, Logs, Payroll, and Cold Storage.
- **Employee-management Level 2 (p. 4):** 1.1 Employee Information Handling, 1.2 Schedule and Time Tracking, and 1.3 Government Info Access; stores include address, job profile, PII, attendance, leave, and changelog. PII paths are marked “On Approval.”
- **Payroll Level 2 (p. 5):** 2.1 Timesheet Validation → 2.2 Leave Monetisation Process / 2.3 Wage and Adjustments / 2.4 Government Deductibles → 2.5 Basic Payroll Computation → 2.6 Payslip Generation & Distribution → 2.7 Monthly Summary Compilation. It names D2.1/D2.2/D2.4 logs and D3.1–D3.5 wage, benefits/deductibles, tax/rates, payslip, and summary stores.
- **Archival Level 2 (p. 6):** annual admin prompt, append to Cold Storage, then delete relevant records. Keep `1YO`, `2YO`, and `opt*` literal: the PDFs do not define them or establish retention policy.
- **Database ERD (Database Design p. 5):** its legend assigns read/append/update patterns and marks `Government` “Additional Security Required.” The ERD conflicts with prose/dictionary on several columns and keys; do not generate a schema directly from the image without reconciliation.

The database overview says semi-monthly payslips, monthly summaries, role-based access, auditability, and two-authorized-person PII approval. Those are **documented design intent**, not verified behavior in the nine repositories. The DFD cites two business references that are not included in the source set.

## 5. Scope gates: keep the course build lean

### Gate A — Assignment scope

Before code changes, write a one-page scope/traceability sheet from the current assignment instructions and the user's approved design. Mark each feature:

- **Required now** — explicitly in rubric/approved design.
- **Reference only** — present in MO-IT107 source PDFs but not yet required by the current milestone.
- **Blocked** — required, but its rule, permission, input, or expected result is missing.
- **Out of scope** — not required for this submission.

Do not treat the whole PDF system as one mandatory feature set. In particular, separate the MO-IT107 database/DFD context from MO-IT110 MS1/MS2 requirements unless the current course instructions explicitly combine them.

### Gate B — Repository and authorship

- If there is a personal, approved MS1 project: implement that design, not the most feature-rich example.
- If there is no personal baseline: do not silently adopt F9/F6/F4 or another team's code. Use them for comparison; get mentor permission/fit confirmation before reuse.
- If adaptation is approved, work only in an isolated copy until the changes are accepted. Preserve original source and fixtures.

### Gate C — Business-rule readiness

Create a short rule register. Every value gets a source and a test case before it enters code.

| Rule area | Required evidence before implementation |
|---|---|
| Pay period and eligibility | Start/end boundaries, semi-monthly/monthly meaning, eligible employee criteria. |
| Base salary and proration | Approved conversion/period formula and worked sample. |
| Attendance | Schedule, time zone, missed/duplicate punches, overnight shifts, grace periods, manual correction. |
| Overtime / holiday / rest day / undertime | Exact triggers, rate multipliers, interaction and rounding rules. |
| Leave | Types, balances, accrual timing, approval, paid leave treatment, monetisation rules. |
| Government deductions/tax | Authoritative rate/bracket table, effective date, employee/employer split and rounding. |
| Net pay / adjustments | Included/excluded earnings and deductions, transaction types and ordering. |
| Output | Required payslip fields, monthly report contents, file/UI format. |

The supplied PDFs name many of these concepts but do not supply the actual formulas/rates. The user has already said the missing rules/reference materials are unavailable; this plan does not ask for them again. Mark unresolved items **BLOCKED** and do not write guessed calculations. If a rule is not needed for the milestone, explicitly defer it.

## 6. Lean architecture target

Use the existing project's conventions if there is an approved baseline. If starting fresh, keep one Java application and only these responsibilities:

1. **Model:** only entities required by the approved design, such as `Employee`, `AttendanceRecord`, `PayPeriod`, `PayrollResult`; use inheritance only if the design/rubric needs real employee subtypes.
2. **Input boundary:** one loader/repository matching the actual fixture/interface (CSV or existing database). Validate expected headers/columns and fail with a useful error; do not return an apparently successful empty payroll for incompatible input.
3. **Calculation:** one clear payroll calculator/service that accepts domain data plus approved rules and returns a result. Keep arithmetic out of Swing event handlers. Use decimal money types as the existing course/project conventions require.
4. **Presentation:** keep the existing GUI if required; call the backend and display its result. Do not rebuild screens that already satisfy the assignment.
5. **Tests:** keep the project's existing test mechanism. Add a new test framework only if the assignment/build already requires it.

A minimal call path should stay readable:

```text
Required UI/console → payroll service → calculator
                           ↑              ↓
                 employee + attendance   payroll result
                  input/repository       → required UI/output
```

Avoid a generic framework, configuration engine, multiple backends, an ORM, and separate services for every table. Add a boundary only when it isolates a real responsibility or is required by the approved design. `PayrollCalculator`, `AttendanceRepository`, `PayrollRecord`, and the F9 model/calculator split are useful naming/shape references, not mandatory class names.

### If a relational database is explicitly required

1. Reconcile the schema conflicts first; do not create tables from a mixed ERD/dictionary/prose version.
2. Implement only the tables needed for the accepted use cases unless the rubric explicitly requires all 30.
3. Decide whether an external identity/RBAC system or account tables are required; the provided schema has no complete account/permission/approval model.
4. Add a named pay-period representation only if the approved requirements need it; semi-monthly slips/monthly summaries are described but period rules are not.
5. Do not implement cold-storage deletion until a retention and recovery policy is authoritative.

## 7. Dependency-ordered build plan

Each phase has a checkpoint. A blocked policy-dependent phase must not be bypassed with invented defaults.

### Phase 0 — Freeze scope and protect the work

**Tasks**

0.1 Read the current course rubric, approved MS1 package, and existing project instructions. Record the milestone and required deliverables.  
0.2 Choose the personal/approved baseline or mark the baseline decision blocked; record any reuse permission.  
0.3 Create the scope/traceability sheet and label each MO-IT107 feature required, reference-only, blocked, or out of scope.  
0.4 Create an isolated working copy/branch before any source or fixture edits; preserve the originals.

**Checkpoint:** scope, baseline, and permissions are explicit; original files remain untouched. No payroll formula code starts before Gate C is satisfied.

### Phase 1 — Establish a reproducible baseline

**Tasks**

1.1 Run the chosen project's existing build and test commands before editing; save results.  
1.2 Record the required JDK and dependencies from the actual project/rubric. Do not infer JDK 21 solely because retests used it.  
1.3 Identify the real input schema and one synthetic/approved test row; keep the inventory CSV separate.  
1.4 Write a short test map: current passing checks, gaps, and GUI manual steps.

**Checkpoint:** the starting project builds or has a documented pre-existing blocker; one compatible input is known; no source behavior has been changed.

### Phase 2 — Specify rules and reconcile only needed data

**Tasks**

2.1 Populate the rule register from authoritative course/user material. For each rule, record source, example input, expected output, rounding, and owning test.  
2.2 If a required rule remains missing, produce a precise unresolved item and leave the dependent feature blocked.  
2.3 Map required concepts to the approved design's classes/fields and existing code; do not add all tables or classes from the PDFs by default.  
2.4 If database work is in scope, resolve only the schema conflicts touched by the required features and document the chosen source/decision.

**Checkpoint:** each planned feature has acceptance criteria and a test oracle; there are no silently guessed rates, formulas, or FK decisions.

### Phase 3 — Deliver the first vertical payroll slice

**Task sequence**

3.1 Add/confirm one focused test for a supported employee + one pay period + known attendance, with expected result taken from the approved rule register.  
3.2 Make the test fail for the missing/incorrect behavior before changing calculation code, where practical.  
3.3 Implement the smallest calculation path in the existing backend/design boundary; do not place formulas in the GUI.  
3.4 Connect the chosen input source to that path and return one structured payroll result.  
3.5 Display that result through the existing required interface (or the smallest console harness if the UI integration is a later approved step).  
3.6 Run the focused test, existing suite, build, and one manual flow.

**Acceptance:** compatible input reaches the calculator, expected output matches an independent approved example, unsupported schema produces a clear failure, and no unrelated screens/modules change.

### Phase 4 — Add only required payroll components

Add in this order, one verified slice at a time:

4.1 Base salary/pay-period calculation and required proration.  
4.2 Attendance-derived regular hours and paid-leave hours.  
4.3 Required overtime/holiday/rest-day/undertime components.  
4.4 Benefits, adjustments, and other transactions required by the rubric.  
4.5 Statutory deductions and tax using approved effective-date tables.  
4.6 Net-pay composition and payslip fields.

For every slice: add a focused expected-value test, implement the minimum calculation, rerun regression tests and build, then verify one sample through the app. Do not combine unrelated formula categories into one large rewrite.

**Acceptance:** each included component traces to a source rule and has an independent expected result; totals reconcile to the approved example; no extra formula is included merely because a data field exists.

### Phase 5 — Attendance and leave, only if required

5.1 Validate the required input format and employee/schedule keys.  
5.2 Implement only required attendance cases (normal day, boundary dates, missing/duplicate/overnight entries as defined by the rules).  
5.3 Implement required leave request/status/balance behavior and only approved accrual/monetisation rules.  
5.4 Connect approved attendance/leave output to the payroll slice and test the join end-to-end.

**Acceptance:** schedule/date uniqueness and time checks behave according to approved rules; leave changes payroll only in the approved status; data changes are not silently discarded.

If these workflows are not part of the current milestone, defer this entire phase and keep attendance/leave as required input fixtures.

### Phase 6 — Integrate the GUI and required outputs

6.1 Keep GUI event handlers thin: collect input, call service/calculator, show result/error.  
6.2 Add only the screens or fields required by the rubric; preserve existing working screens.  
6.3 Generate the required payslip/report using existing project libraries/templates if present.  
6.4 Add a small manual QA checklist for login, one payroll flow, result display/export, and relevant error state.

**Acceptance:** GUI shows the same values as backend tests; no formula is duplicated in the UI; invalid inputs produce an understandable message; no unrequired redesign.

### Phase 7 — Optional MO-IT107 database/security/reporting track

Enter this phase only if current instructions explicitly require it.

7.1 Reconcile only schema decisions needed by the required flows: Employee department/government links; Address/Region; Supervisor department; Schedule/Rest Day; Leave schedule link; Government Deductibles/Tally composite keys; Rates `implementationYear` key; Holiday uniqueness; Payroll `monthID` FK; Leave Monetisation `leaveType` FK; Contributions' referenced table/period key; and relationship-matrix omissions for city/region/rest-day links.  
7.2 Map DFD stores to tables; do not assume a DFD store equals one table.  
7.3 Implement minimal persistence for approved flows and test referential integrity/transactions.  
7.4 Implement the approved actor permissions. If the two-person PII approval statement is required, model the request and two approvals explicitly; do not expose government IDs to all admins by default.  
7.5 Build monthly summary only to the content/format required.  
7.6 Add archival last, after retention and restore requirements are approved; test archive verification and restore before any purge.

**Acceptance:** schema matches one approved design version; permission checks are tested; no production data or permanent deletion is involved in test runs.

### Phase 8 — Submission-ready QA and evidence

8.1 Run the project's full existing suite and build after the final code change.  
8.2 Run boundary cases only where expected values are sourced.  
8.3 Verify one compatible fixture and one deliberately incompatible schema; errors must be explicit, not a false-success empty result.  
8.4 Manually follow required GUI paths and verify displayed values against the test oracle.  
8.5 Prepare the rubric's required documentation, class/design mapping, test evidence, and demo steps.  
8.6 Confirm source/fixtures are clean and only authorized files changed.

**Definition of done:** all in-scope criteria pass; build/tests have fresh evidence; design-to-code mapping is visible; unresolved rules are labeled; no scope-gated feature has slipped in; original project material is unchanged.

## 8. Test strategy and what existing test results do/do not prove

### Use the nine submissions as evidence, not as the oracle

- F6's 167 checks and F9's 168 checks are useful regression/reference evidence, but not a substitute for current approved formulas.
- F9's date-sensitive test needed `Asia/Shanghai` or `Asia/Manila`; use a declared project timezone for test runs and test date boundaries. Do not assume the user locale is a payroll rule.
- F4's Maven exit code 0 had **zero Surefire tests**; its manual mains are not an automated suite.
- F2/F5 tested navigation on disposable MariaDB protocol compatibility; native MySQL and persistence-write paths are unverified.
- F3's working synthetic 19-column row does not fix its production CSV parser; quoted commas in bundled rows cause incorrect column selection.

### Minimum proof for each implemented feature

1. **Unit:** one normal case and relevant boundary/error cases against an independent expected value.
2. **Input boundary:** required columns/fields, missing or malformed rows, quoted CSV values if CSV is in scope, and an explicit incompatible-schema error.
3. **Service/integration:** employee + period + attendance/leave → payroll result through public interfaces.
4. **UI/manual:** one end-to-end click path, correct value display, and one invalid-input/error state.
5. **Regression:** existing suite + build after each coherent slice.

Use synthetic test data. Do not print personal employee identifiers or government data into logs/reports.

## 9. Key risks and mitigations

| Risk | Impact | Response |
|---|---|---|
| Payroll/attendance rules are missing or contradictory | High: tests cannot establish correctness. | Keep formulas blocked until an authoritative rule and expected example exist; do not invent defaults. |
| Wrong or unauthorized repository chosen | High: wasted work and authorship/course-integrity risk. | Prefer user's approved design; treat numbered projects as references; confirm permission before adapting. |
| PDF schema conflicts | High if database is required; unnecessary if not. | Reconcile only relevant tables at Phase 2/7; keep a decision table and do not mix diagram/prose silently. |
| Feature creep from 30-table/DFD scope | High: oversized coursework. | Label requirements vs reference-only; implement the smallest rubric-complete vertical slice; keep RBAC/archive optional. |
| Fragile fixture parsing | Medium/high: silent wrong payroll. | Validate schema, fail loudly, use project-native fixture format; F3 is a known counterexample. |
| Test totals mistaken for policy proof | High. | Use a separate approved calculation oracle; old tests prove only their own assertions. |
| PII/credentials leak into QA | High. | Synthetic data only, avoid copying secrets, restrict test artifacts, keep logs redacted. |
| Date/time test instability | Medium. | Declare timezone and fixed clock/test dates where supported; record timezone in test evidence. |

## 10. First working session checklist

Do these in order; stop at a blocked item rather than guessing:

- [ ] Locate the current milestone/rubric and the user's approved design (if any).
- [ ] Mark each PDF capability required, reference-only, blocked, or out of scope.
- [ ] Choose the authorized baseline; do not copy a numbered repository by default.
- [ ] Run its current build/tests in an isolated copy and record the exact result.
- [ ] Fill the rule register for the first payroll use case; if unavailable, keep calculation work blocked.
- [ ] Implement only the first tested vertical slice.

**Stop condition:** once the required payroll flow passes its sourced acceptance tests and the required GUI/output works, stop. Do not continue into full RBAC, database normalization, archival automation, or production hardening unless the assignment explicitly asks for it.

## 11. Source-to-plan traceability

| Plan choice | Evidence used |
|---|---|
| Prefer approved personal MS1/baseline; do not assume a personal CP2 codebase | Submission folder analysis: provenance/course labels are uncertain; F9 reports another team's CP2 baseline. Active MO-IT110 boundary in `skills.md`. |
| Use F9/F6 as comparison patterns, not formula authority | Retest addendum: F9 91+33+44; F6 167/167; all test totals are repository-specific. |
| Avoid treating F2/F5 as independent options | Static comparison: executable logic virtually identical. |
| Reject F3 as parser/calculation oracle | Bundled CSV quoted-comma misalignment: 34/34 values disagree under correct parsing. |
| Keep an OOP backend boundary and thin GUI | Folder 4 layering; folder 6 refactor; folder 9 OOP backend/UI separation and test harness. |
| Use full system features only by scope gate | MO-IT107 synthesis: five actors, 30-table DB-design version, seven payroll sub-processes, PII approval intent, archive annotations; several source conflicts and missing formulas/rates. |
| Do not implement archive purge without approval | DFD page 6 shows append-to-cold-storage then delete, but synthesis says retention labels/restore details are unspecified. |
| Keep the plan incremental and testable | Relevant planning, lean-build, simplicity, spec-driven, and incremental-implementation guidance from `skills_compilation.md`, filtered through `/home/user/skills.md`. |

---

## Decision summary

**Recommended path:** use the user's own approved design if available; implement one tested payroll path first; use F9/F6/F4 as references only; defer the MO-IT107 database/RBAC/archive scope unless the current rubric requires it; block formula work whenever the necessary rule is not authoritative.

**Next action:** choose the course-approved baseline and mark the first required payroll use case in the scope sheet. Then run that baseline's existing tests before editing.
