# MotorPH MS2 — Implementation Brief

## 1. Objective

Continue the MotorPH employee and payroll application using Claude's implementation as the primary starting point:

**Primary base repository:** https://github.com/Sitramonicus/forreview/tree/main/motorph-ms2-main/motorph-ms2-main

The existing project in the current workspace contains changes and corrections that were not included in the instructions given to Claude. Compare both implementations first, then use Claude's version as the main base while retaining relevant improvements from the current workspace. Do not blindly replace the current project or discard later corrections.

The expected result is a functioning, populated MotorPH employee and payroll application, with the requested authentication, employee hierarchy, access controls, branch assignments, attendance, payroll, and UI changes implemented in the project itself. This is an implementation task, not merely an audit or a proposed plan.

Work through the requirements in this document without stopping after the comparison. Make reasonable, clearly documented assumptions where the supplied material does not settle a detail. Do not ask me to repeat information already present here or in the project files.

## 2. Required initial inspection

Before changing code:

1. Inspect the current workspace, its project structure, build configuration, application entry points, persistence layer, tests, and existing documentation.
2. Retrieve and inspect the primary base repository above. If it cannot be accessed, state the exact blocker and continue with the files available locally. Do not claim to have compared code that you could not access.
3. Locate and read `skills_compilation.md`. Search it for the skills relevant to this task, including Java/backend development, frontend development, application architecture, authentication and session security, database design, accessibility, CSS, testing, linting, and performance. Read and apply the applicable skill instructions before implementing their respective areas. Do not indiscriminately apply unrelated skills.
4. Compare the current implementation with Claude's implementation. Identify changes in behaviour, data structures, calculations, security, UI, dependencies, and documentation. Pay particular attention to later fixes and dated systems that were already changed in the current implementation.
5. Produce a concise comparison record with three classifications:
   - **Keep from Claude:** the preferred implementation or UI to retain.
   - **Keep from current project:** a later correction or useful feature missing from Claude's copy.
   - **Replace or reconcile:** outdated, conflicting, duplicated, or incomplete behaviour.
6. Use the following order of authority when sources disagree:
   1. Explicit requirements in this brief.
   2. Corrected behaviour already present in the current project, where it does not conflict with this brief.
   3. Claude's implementation as the primary base.
   4. Existing project documentation and supplied CSV/timecard/payslip materials.
   5. External references and reasonable implementation assumptions.

Do not use the comparison as an excuse to stop. After recording the differences, implement the agreed result.

## 3. General implementation rules

- Preserve Claude's existing UI, colour scheme, branding, and MotorPH logo except where this brief explicitly requests a change.
- Preserve working behaviour unless it conflicts with an explicit requirement or is demonstrably defective.
- Prefer a small number of clear folders. Do not create a folder for every class or component.
- Keep the existing stack where it is suitable. Do not rewrite the entire application or introduce a new framework merely for preference.
- Replace Firebase Authentication with Employee ID and password authentication. Do not remove Firestore or another existing persistence service automatically if it can remain useful as the application's data store. Separate authentication from data persistence, then retain or replace the persistence layer based on what the inspected code supports.
- Do not leave non-functional buttons, placeholder pages, fake success messages, or controls that only look interactive.
- Do not hard-code secrets, password peppers, session tokens, or private configuration in source control.
- Keep calculations in testable backend/service modules rather than embedding business rules in UI event handlers.
- Avoid needless abstractions, excessive dependencies, and gratuitous rewrites.
- Use transactions or equivalent safeguards for changes that must remain consistent, particularly payroll runs, password changes, and approval actions.
- Make the application usable at desktop, tablet, and mobile widths. Maintain keyboard access, visible focus, sensible contrast, and reduced-motion support.
- Do not claim a test, benchmark, integration, or comparison succeeded unless you actually ran it and observed the result.

## 4. Naming and code-style preferences

Follow these conventions where they fit the language and existing code. Do not sacrifice valid syntax, standard tooling, or readability to force a notation into a place where it does not belong.

### Variable and member naming

- When several variables share the same data type and their names are short, group declarations on one line, separated by commas. Split them when the line becomes difficult to scan, wraps excessively, or contains more than roughly four variables.
- For files with only a few variables and functions, prefer lowercase member notation such as `ab_name`. The prefix letters identify the source package/folder group and variable group: for example, `a` can identify `a_doc`, and `p` can identify a PhilHealth-related group.
- For a small number of names containing more than two words, use camelCase after the member prefix.
- For larger files with many declarations, combine the member notation with Hungarian-style type prefixes, for example `ab_dtName`, where `dt` identifies the data type and `Name` is the variable name.
- Apply the notation consistently within a file. Do not rename every existing identifier solely to impose a new style if doing so would create needless churn.

### File naming

- Prefer PascalCase for file names.
- Abbreviations are selective rather than mechanically shortened. `Emp`, `Hday`, `Gov`, `Sched`, `Svc`, `Mem`, `Req`, and `Deet` are acceptable where clear. Avoid unclear abbreviations such as `Atten`, `Pht`, `WF`, `WoFo`, `Cdr`, and `PS` for payslip. Do not abbreviate Firestore as `FS`, or mutable as `Mut`.
- Prefer an understandable full word over an abbreviation when uncertain. Do not invent a new abbreviation merely to shorten a filename.
- Keep names consistent within a feature area.

## 5. Source and web folder organisation

Use the following organisation as the preferred structure. Add a folder only when its contents do not reasonably belong in an existing one. Keep the number of folders low.

### Application source

Under the main source directory, use:

- `a_doc` — project notes, technical documentation, decision records, and implementation references.
- `o_oop` — object-oriented models, entities, and core application objects.
- `t_tkt` — ticket, support-request, and related workflow code.
- `l_log` — logging and audit-related code.
- `c_calc` — payroll, statutory, and other calculation code.

Add another source folder only when a group has a clear responsibility that does not fit those five. Keep package declarations, imports, build files, and test paths valid after any relocation.

### Web application

Under `web/`, prefer:

- `b_basic` — primary GUI, pages, tabs, profiles, and their local components.
- `f_flares` — motion, animation, decorative effects, and dynamic visual elements.
- `sp_secpol` — security and policy-related UI, notices, and client-side security controls.

Organise tabs and profiles inside `b_basic` only where that separation improves navigation. Do not create deeply nested folders without a practical reason.

### Tests and quality tools

Create a conventional, easy-to-run test and quality structure. Retain the tests already used by Claude and the current project, then add the relevant checks below where compatible with the stack:

- Unit tests for Java services, models, authentication, permissions, calculations, and data transformations.
- Integration tests for persistence, API routes, sessions, payroll processing, and role restrictions.
- Frontend tests for components, forms, navigation, and state changes.
- End-to-end browser tests for login, logout, password change, role access, employee workflows, attendance, payroll, and payslips.
- JavaScript linting with ESLint, plus formatting checks where suitable.
- Java static analysis and style checks, including Checkstyle, SpotBugs, and PMD where compatible with the project's Java version and build system.
- CSS/HTML checks, accessibility checks, and responsive-layout checks.
- Lighthouse checks for accessibility, performance, best practices, and SEO where applicable to the application.
- Dependency and security auditing.

Do not add tools just to inflate the tool list. Configure the tools that are actually compatible, make them runnable through the project's normal scripts, and document any tool that could not be configured. Separate mandatory tests from optional audits so a network-dependent Lighthouse run does not make the ordinary unit-test command unusable.

## 6. Employee records and account provisioning

Use **`Copy of MotorPH_Employee Data - Employee Details.csv`** as the canonical employee source for all employee accounts and employee details.

- Import every employee represented in the CSV. Do not silently omit employees because their role or supervisor is inconvenient to map.
- Use the Employee ID as the login identifier. Do not require a Firebase account, email address, or external identity provider to sign in.
- Preserve employee details accurately. Normalise formatting only where needed for reliable storage and display; do not alter source facts without documenting the transformation.
- Handle duplicate, blank, or malformed Employee IDs explicitly. Do not create ambiguous accounts.
- Keep employee identity, reporting relationships, branch, role, and payroll data in fields that can be validated independently.
- Do not expose residential addresses, personal contact details, or other unnecessary personal information in general employee views.
- Apply access checks on the server/API and data-query layer. Hiding a button or tab is not an access-control mechanism.

### Initial passwords

Set the initial password for each imported employee account to the exact case-sensitive value:

`MotorPH-demo-2026`

This is an initial provisioning credential for testing the password-change flow. It must never be stored as plaintext in the database, logs, client code, or committed configuration.

Implement password storage with:

- A modern password-hashing algorithm, preferably Argon2id with an appropriate work factor for the deployment environment.
- A cryptographically random, unique salt for every account. Two employees who start with the same password must have different stored password hashes.
- A server-side pepper supplied through a protected environment variable or secret manager. Do not derive it from an Employee ID, email, name, birthday, location, timestamp, or other predictable/user-specific value. Never store the pepper beside the password hashes.
- A documented pepper-missing/start-up failure policy. Do not silently fall back to plaintext or a hard-coded pepper.
- Constant-time hash comparison through the selected password library.
- Rate limiting and sensible failed-login handling. Error messages must not reveal whether an Employee ID exists.

Set a `must_change_password` (or equivalent) flag for initially provisioned accounts and require the employee to choose a new password after the first successful login. Implement and test password changes, current-password verification, password policy validation, hash replacement, and session revocation/rotation. Do not display the shared initial password anywhere in the normal application interface.

## 7. Authentication and session management

Implement first-party Employee ID/password authentication and a server-validated session system. The following is the required behaviour; use a more efficient implementation where appropriate without weakening these guarantees.

### Session token

- Generate at least 32 cryptographically random bytes for each session token.
- Encode the token as an opaque URL-safe random string. It must not contain an Employee ID, timestamp, expiry, location, or other meaningful data.
- Send it to the browser in a cookie configured with `HttpOnly`, `SameSite=Lax` or stricter where the application permits, and `Secure` in HTTPS deployments. Set a narrow, appropriate `Path` and avoid broad domain scope.
- Store only a one-way hash of the session token on the server where practical, together with the employee/account reference and session metadata. Never store or log the raw token.
- Use the project's existing database/persistence service if it can safely store and validate sessions. If a separate sessions table/collection is appropriate, use fields equivalent to `session_id_hash`, `user_id`, `created_at`, `last_active`, and `expires_at`.

### Expiry rules

- Idle timeout: 15 minutes without authenticated activity.
- Absolute timeout: 8 hours from session creation, regardless of activity.
- Check both limits on every protected server request. Expired sessions must be rejected and invalidated.
- Refresh `last_active` from authenticated activity. Avoid a background loop that checks every second; use request-driven/lazy validation. Throttle writes if necessary, provided the 15-minute idle limit remains accurate enough for the application.
- The frontend may show an inactivity warning or redirect after local inactivity, but this is a user-experience aid only. Server-side expiry is authoritative.
- Logout must invalidate the server-side session and clear the cookie.
- Rotate the session after login and after password or privilege changes. Password changes should invalidate other active sessions for the account, or provide an explicit, secure session-management policy.
- Add CSRF protection suitable for cookie-based authentication, validate request origins where appropriate, and prevent session fixation.
- Do not rely on client-side role values, local storage, or hidden UI controls to validate sessions or permissions.

A daily cleanup task may remove expired session records, but correctness must not depend on that cleanup. Expiry must be enforced during request validation.

## 8. Reporting hierarchy, teams, and permissions

Use the immediate-supervisor information in the employee CSV to establish reporting relationships.

- Each employee belongs to the team led by their listed immediate supervisor, subject to the actual hierarchy represented in the data.
- A supervisor's roster includes their direct reports and all descendants in the reporting tree. For example, if Mata's team and De Leon's team report to Romualdez, Romualdez's roster must include both teams and their employees, including any nested reports.
- Do not treat every employee with a managerial-sounding job title as a supervisor unless the CSV or an explicit role mapping supports it.
- Detect missing supervisors, self-reporting, circular reporting chains, duplicate relationships, and employees who cannot be placed in the hierarchy. Do not silently invent reporting lines. Record exceptions and use a safe restricted default until resolved.
- A supervisor may see only their own team and descendant teams. They must not gain access to unrelated teams merely because they share a branch or department.
- Where a superior has subordinate supervisors, the superior's roster view must include those supervisors' teams recursively, not just immediate reports.

### Role and access model

Implement role-based permissions together with employee/team scope. A role determines the type of action available; the reporting tree determines which employee records the user may access. Treat this as a starting mapping and reconcile titles against the actual CSV and existing application roles.

| Role | Intended access |
|---|---|
| CEO | Read-only, company-wide oversight. May view workforce and high-level payroll cost information, including organisation-wide compensation visibility as required by the brief. No payroll editing or processing by default. |
| CFO | Broad financial visibility: payroll costs, statutory liabilities/remittances, compensation, and financial reports. Approval permissions may be enabled only where the application has an explicit approval workflow. Viewing payroll does not automatically grant authority to release funds or alter payroll. |
| COO | Operational visibility: department/team staffing, overtime, holiday work, attendance patterns, and operational labour-cost reports. Do not grant unrestricted access to all individual salary records unless the source data or an explicit permission requires it. |
| CMO | Treat as a supervisor-level account, limited to the CMO's own reporting chain. The CSV indicates a small team of three; verify the actual reporting rows and do not grant general C-level financial access. |
| Team supervisor | View and perform only the permitted actions for direct reports and descendant teams, such as leave or attendance approvals where those workflows exist. |
| HR staff | Employee administration and HR workflows within the permissions assigned to the HR role. Sensitive compensation access must be separately permissioned. |
| Payroll staff | Payroll preparation, calculation, review, and payslip operations as explicitly permitted. Separate preparation from final approval where feasible. |
| Employee | View their own profile, attendance, leave/requests, pay information, and payslips; submit permitted requests; change their password. |
| System administrator | Technical account and configuration administration. Administrative access must be explicit and audited; do not assume that a technical administrator automatically needs unrestricted salary visibility. |

Use least privilege. Keep approval, processing, viewing, and configuration permissions distinct. Record sensitive actions in an audit log with actor, action, target, time, and relevant outcome, while excluding passwords, raw session tokens, and unnecessary personal data.

## 9. Branch assignment

Use the branch list and branch names already present in Claude's implementation as the set of available branches. Do not invent extra branches unless the existing data makes that unavoidable and the reason is recorded.

Apply these rules:

1. For leaf teams — teams whose supervisor has no subordinate supervisors/teams beneath them, such as the Mata and De Leon teams in the example — estimate the team's geographic centre using the employee addresses in the CSV.
2. Use the available coordinates or a permitted geocoding method to derive a geographic centroid for the employees in each leaf team. Assign that team to the nearest existing branch using the branch's known location.
3. If an address cannot be geocoded reliably, use the most precise shared locality supported by the CSV. Do not fabricate coordinates or claim a precise midpoint where the source only supports a municipality/city-level estimate.
4. Do not upload the employee CSV or full residential addresses to an external geocoding service without explicit authorisation. Prefer existing coordinates, an offline/local source, or a locality-level approximation. Use addresses only for this assignment calculation, and do not expose them in the application UI or reports.
5. Supervisors who have subordinate supervisors/teams beneath them, and all C-level employees, must be assigned to **Main Branch**.
6. Employees in a leaf team inherit that leaf team's assigned branch unless the CSV contains a clear, documented exception.
7. Preserve the reporting hierarchy independently of branch assignment. Branch membership must not be used as a substitute for supervisor-based access control.

Document the resulting team-to-branch assignments and the method used. Where source addresses are insufficient, mark the assignment as approximate in engineering documentation rather than pretending it is exact.

## 10. Attendance and payroll population

Use both of the following supplied files:

- `Copy of MotorPH_Employee Data - Employee Details.csv` — employee identity, employment details, salary/rate information, and other employee-specific inputs.
- `Copy of MotorPH Sample Timecard - MotorPH Payslip.csv` — timecard, attendance, and payslip examples to guide the shape and treatment of payroll inputs and outputs.

Populate attendance and payroll records for every employee represented in the employee CSV.

### Attendance

- Use the sample timecard as the pattern for attendance fields and the handling of ordinary days, lateness, undertime, overtime, rest days, special days, regular holidays, night differential, and other supported cases.
- Respect each employee's applicable schedule, employment status, and payroll classification. Do not give every employee identical attendance.
- Create coherent attendance records across enough completed payroll periods to make employee histories, reports, and charts useful. Prefer at least the latest 12 completed monthly periods where the project supports monthly payroll. Do not invent a completed payroll for an unfinished period.
- Make attendance and payroll agree. Overtime, absences, lateness, leaves, holiday work, and other paid/unpaid time must reconcile with the corresponding payroll calculations.
- Avoid impossible or contradictory records, such as overlapping shifts, duplicate clock events, overtime outside a plausible shift, or paid leave that is also deducted as an absence.
- Where the employee CSV or sample timecard does not establish a value, choose a plausible assumption, document it in the data-generation notes, and keep it consistent across all affected records.

### Payroll

- Generate a plausible payroll history for every employee using the employee CSV and timecard/payslip material.
- Retain employee-specific salary rates, employment categories, allowances, and other applicable inputs where those are actually present in the sources.
- Calculate the gross pay, earnings, deductions, statutory contributions, withholding tax, and net pay through the application's calculation services. Do not randomly assign final payroll totals.
- Ensure every payslip item reconciles to its source attendance and payroll inputs. Totals must add correctly and round consistently to centavos.
- Implement or retain separate, testable calculations for ordinary pay, overtime, rest-day work, special non-working holidays, regular holidays, night differential, leave treatment, statutory deductions, and tax, to the extent supported by the source data and applicable rules.
- Verify Philippine statutory rates, thresholds, and payroll rules against current authoritative sources applicable to the payroll period. Keep effective dates and source references in the calculation documentation. Do not silently apply a 2026 rate to a different period if the rate changed.
- Do not present a provisional assumption as a statutory fact. Where a rule or employee classification is unavailable, document the assumption and make the result traceable.
- Support payroll preparation, review, approval (if applicable), processing, and payslip generation as separate states. Prevent duplicate processing of the same employee/pay period.

### Data provenance and presentation

The user-facing application must not display terms such as **fictional**, **version**, **demo**, or **synthetic data** as labels, badges, page names, or explanatory copy. Remove those terms from visible product wording and obsolete UI elements.

However, attendance and payroll records generated to populate the application must remain truthfully identified as seeded/test records in internal documentation and test fixtures. Do not claim that generated records are actual company attendance or historical payroll. Keep data provenance out of normal employee-facing screens, but preserve it for maintainers, auditors, and testing. Do not delete provenance notes merely to make the interface look production-ready.

## 11. My Pay: MotorPH calculation and Sweldong Pinoy comparison

Add a switch in the **My Pay** tab, available to all employees, between:

- the application's MotorPH payroll calculation; and
- a Sweldong Pinoy-based calculation/reference.

Before implementing the second option, inspect the current Sweldong Pinoy calculator and establish what inputs, assumptions, rates, and outputs it uses. Use a public, legitimate source; do not depend on an undocumented private endpoint or brittle page scraping. Where practical, implement the comparison as a separate calculation module using documented rules rather than embedding an external website in the application.

Both views must use the same employee and pay-period inputs. Clearly identify which calculation is being displayed, explain any differing assumptions, and show discrepancies without silently changing the actual payroll record. The switch is for employee comparison and understanding; it must not alter a processed payslip or the official payroll calculation. Add tests for representative employees and calculation edge cases.

## 12. UI: preserve the existing product identity

Keep Claude's existing GUI, overall colour scheme, and MotorPH logo. The linked examples below are visual/interaction references, not permission to copy their branding or replace the MotorPH identity. Adapt their colours to the existing MotorPH palette and their dimensions to the actual layout.

### 12.1 Landing page

Use this CodePen as the landing-page layout and interaction reference:

https://codepen.io/shshaw/pen/aZXzzE

- Replace the current **Explore** and **Discover** actions with **Login** and **Contact Help**.
- **Login** opens the Employee ID/password login page.
- **Contact Help** opens the IT contact/help page for account access and login problems.
- Replace the example images with imagery suitable for MotorPH and the employee/payroll application.
- Change the highlight/accent colour to match the existing MotorPH colour scheme.
- Keep the page responsive and make both actions functional.

### 12.2 Icons and animation resources

Use the following as component/resource references:

- Icons: https://github.com/kubai087/kubai-icons
- Animations: https://sarthology.github.io/Animatopy/
- CSS effects/animations: https://ustymukhman.github.io/Dynamic.css/public/

Use icons consistently in navigation, sidebar previews, header controls, and relevant actions. Do not use arbitrary Unicode characters as substitutes for the provided icon set. Check licensing and package compatibility before adding assets. Avoid loading every animation library or effect if only a few components need it.

### 12.3 Sidebar navigation buttons

Use the following CodePen as the appearance and interaction reference for sidebar buttons:

https://codepen.io/ (the supplied 3D-button HTML/CSS/JS sample is reproduced in Appendix A)

Adapt the 3D card treatment to MotorPH colours. Use the supplied Start, Favorite, Confirm, and Settings sample only as a styling reference; use the application's actual navigation labels and actions. Scope all selectors to the sidebar component. Do not apply the sample's global `body`, `svg`, or button rules to the entire application.

### 12.4 Collapsible and previewable sidebar

The sidebar must support these states:

- **Expanded:** MotorPH logo, icons, and normal text labels are visible.
- **Collapsed/idle:** retain the MotorPH logo and show compact icon-only navigation.
- **Hover/focus preview:** temporarily reveal the full navigation labels and normal button appearance while the pointer or keyboard focus is within the sidebar. Restore the collapsed state on pointer exit or focus leaving the component.
- **Collapsed header treatment:** when the sidebar is collapsed and not being previewed, use the compact header-style treatment from https://codepen.io/ainalem/pen/pXrVmQ, adapted to the Kubai icons and MotorPH styling.

Keep the behaviour usable on touch devices, where hover is unavailable. Provide a dedicated expand/collapse control, accessible names/tooltips for icon-only controls, keyboard operation, visible focus, and reduced-motion behaviour. The MotorPH logo must remain visible in the collapsed state.

### 12.5 Payslip presentation

Use this as the payslip layout/generation reference:

https://codepen.io/vajkri/pen/PrBQxg

Keep the existing MotorPH logo in the payslip, retain all existing employee/pay-period/payroll details, and preserve the **Print Payslip** button. Adapt the reference to the current payslip data model; do not replace real employee-specific information with placeholder fields. Ensure printed output is legible, correctly paginated, and excludes navigation, unrelated controls, and decorative background effects.

### 12.6 Logout button

Use the supplied tactile/press-button sample in Appendix B as the visual reference for the logout control. Adapt the finish and colours to MotorPH. Scope the styles to the logout component; do not copy the sample's global `body` and `button` rules. The control must still perform the actual server-side logout and session invalidation.

### 12.7 Overview — “Your Pages” section

Use the isometric button reference below for the **Your Pages** section on the Overview tab:

https://codepen.io/ (the supplied HTML/CSS sample is reproduced in Appendix C)

Replace the example labels with the actual destinations available to the logged-in user. Each tile must navigate to its intended page. Retain responsive sizing, keyboard access, readable labels, and a usable reduced-motion alternative. Do not use the reference's full-screen positioning or global background styles inside the Overview tab.

### 12.8 My Pay switch control

Use this as the visual reference for switching between the MotorPH calculation and Sweldong Pinoy comparison:

https://codepen.io/ksenia-k/pen/poYZpyQ

The switch must be accessible by keyboard and screen reader, clearly indicate the selected calculation, and preserve the same employee/pay-period context when toggled.

### 12.9 Small header component

Use a very small, still-legible version of this reference in the application header:

https://codepen.io/ericadu/pen/GRZJjjE

It must remain subordinate to the MotorPH logo and primary navigation, must not crowd the header, and must not obscure controls at narrow widths.

### 12.10 Layered colour effect

Use this reference:

https://codepen.io/supah/pen/mMMdxG

Retain the reference's end colour, but change its start colour to a colour from the MotorPH palette. Make the topmost layer transparent and progressively lower the opacity of the nearby layers so the effect blends into the page background rather than appearing as an opaque slab. Keep text and controls legible above it.

### 12.11 Background effect

Use a restrained, less noticeable adaptation of:

https://codepen.io/mubangadv/pen/oNaBGLW

It should function as a subtle background detail, not compete with page content, consume excessive GPU/CPU resources, or reduce contrast. Provide a static/reduced-motion fallback and avoid unnecessary animation on low-powered devices.

## 13. Performance ideas from the Lean 4 server example

Use the supplied YouTube Short as inspiration, not as a requirement to reproduce its implementation:

https://www.youtube.com/shorts/Cr69N5pG258

The supplied description says the developer used Lean 4 proofs to justify direct access to SQLite's B-tree layout, reported a feed-load reduction from 16.7 μs to 5.4 μs, generated optimised C, and used `io_uring`. It also reports results of more than 280,000 concurrent users and 26,000 requests per second on a small server.

Evaluate which ideas make sense for this application's actual Java/backend, database, hosting, and workload. In particular:

- Measure current response times, query patterns, database access, and bottlenecks before optimising.
- Apply suitable indexes, query improvements, pagination, caching, reduced payloads, and efficient I/O where measurements justify them.
- Preserve transaction correctness, payroll accuracy, and access-control checks while optimising.
- Do not bypass a database engine's normal query layer or depend on undocumented storage internals unless the data layout and invariants can be proven and the maintenance cost is justified.
- Do not introduce Lean 4, generated C, SQLite B-tree shortcuts, or `io_uring` merely because the video used them. Adopt those only if they fit the actual stack and provide a measured benefit.
- Do not repeat the video's benchmark figures as if this application achieved them. Benchmark this application on the actual target environment and report the test conditions and results.

## 14. Security, privacy, and reliability requirements

In addition to authentication and role-based access above:

- Validate input on the server, including Employee IDs, date ranges, payroll values, and workflow transitions.
- Use parameterised queries or the equivalent safe data-access mechanism.
- Protect against injection, cross-site scripting, CSRF, insecure direct-object references, broken access control, and session fixation.
- Apply least-privilege access to employee records, attendance, compensation, reports, and payslips.
- Never expose one employee's payslip or personal data to another employee through guessed IDs or modified request parameters.
- Use secure password-reset/change flows and audit security-sensitive events.
- Avoid logging sensitive payroll fields, passwords, raw session tokens, or full residential addresses.
- Keep secrets in environment variables or an appropriate secret store, with example placeholders only in sample configuration.
- Use safe error handling. Do not expose stack traces or internal credentials to ordinary users.
- Use database constraints and validation to prevent duplicate payroll records and invalid employee relationships.
- Ensure payroll recalculation and processing are repeatable and do not create duplicate deductions or duplicate payment records.

## 15. Required implementation and verification sequence

Complete the work in this order, adjusting only where the repository's dependencies require it:

1. Inspect both project versions, read the applicable skills, and record the source comparison.
2. Reconcile project structure, build configuration, and retained features.
3. Import employee records and build/validate the reporting hierarchy and branch assignments.
4. Implement Employee ID/password authentication, secure password storage, password-change flow, and server-side sessions.
5. Implement and test role permissions, manager team scope, and audit logging.
6. Populate coherent attendance and payroll histories for every employee.
7. Verify or implement payroll/statutory calculations, payslip generation, and the Sweldong Pinoy comparison.
8. Apply the requested UI changes while preserving the existing MotorPH identity.
9. Add and configure automated tests, static analysis, accessibility checks, and relevant performance checks.
10. Run the full test/build/lint/security suite, fix failures caused by the implementation, and verify the application from a fresh start.

### Minimum acceptance checks

The work is not complete until the following have been checked:

**Employee data and hierarchy**
- Every CSV employee has exactly one valid account and a unique Employee ID.
- Reporting relationships are valid, acyclic, and consistent with the CSV.
- A supervisor can see all and only the permitted employees in their recursive team roster.
- The CMO receives supervisor-level access to the CMO reporting chain, not broad executive access.
- CEO, CFO, COO, HR, Payroll, supervisor, employee, and administrator permissions match the configured policy.
- Branch assignments follow the leaf-team midpoint rule, with parent supervisors and C-level staff assigned to Main Branch.

**Authentication and sessions**
- Every account can initially authenticate with the supplied initial password.
- Stored password hashes differ across accounts because of unique salts.
- No plaintext password or pepper appears in stored records, frontend assets, logs, or committed configuration.
- First-login password change works and rejects an incorrect current password.
- Login creates a random opaque session; logout invalidates it.
- Idle expiry and absolute expiry are enforced on the server.
- Expired, revoked, or tampered sessions cannot access protected routes.
- Cross-employee data access is rejected by the server, not merely hidden in the GUI.

**Attendance and payroll**
- Every employee has coherent attendance and payroll records for the seeded periods.
- Each payroll total reconciles to its earnings, deductions, statutory contributions, and attendance inputs.
- Payslip values match the payroll record and print correctly.
- Reprocessing a payroll period cannot duplicate a payment or deduction.
- Statutory calculations have tests for thresholds, caps, rounding, effective dates, and boundary cases.
- The MotorPH/Sweldong Pinoy switch changes the displayed calculation only, not the official payroll record.

**UI and accessibility**
- Landing-page Login and Contact Help actions work.
- Sidebar expand, collapse, hover/focus preview, and touch behaviour work.
- Sidebar navigation, Overview tiles, logout, payslip print, header effect, and My Pay switch work.
- Layouts remain usable at narrow mobile, tablet, and desktop widths.
- Keyboard navigation, focus visibility, contrast, and reduced-motion settings are checked.
- Print output excludes navigation and decorative effects while retaining the MotorPH payslip branding and details.

**Engineering quality**
- The project builds from a clean checkout/extraction using documented steps.
- Unit, integration, and end-to-end tests run and their results are reported accurately.
- ESLint and applicable Java checks (Checkstyle, SpotBugs, PMD) run successfully, or an explicit reason is documented for any unavailable check.
- HTML/CSS, accessibility, dependency, and security checks are run where configured.
- No known high-severity dependency or security finding is silently ignored.
- Performance claims are backed by measurements from this application.

## 16. Deliverables

Provide:

1. The implemented project, with all changed source files and configuration.
2. The source comparison record explaining what was retained from Claude, what was retained from the current project, and what was replaced or reconciled.
3. A concise implementation summary organised by feature area.
4. The final folder tree.
5. Instructions to install dependencies, configure secrets, initialise/import the employee data, run the application, and run all tests.
6. A list of environment variables and safe example values/placeholders.
7. Test/build/lint/security/a11y results, including failures and checks that could not be run.
8. Documentation of payroll assumptions, statutory sources/effective dates, generated attendance/payroll provenance, role permissions, branch assignments, and session policy.
9. A list of remaining limitations or decisions that could not be resolved from the supplied materials.

Do not report a percentage of completion unless it is calculated against a defined feature inventory. Do not claim the application is production-ready merely because it builds. Be explicit about anything that remains unverified.

---

# Appendices: supplied UI component examples

The following code is included as a reference for the requested effects. It is not a mandate to copy global CSS into the application. Scope styles to the relevant component, adapt the colours to MotorPH, and preserve the existing app-wide styles.

## Appendix A — 3D sidebar button sample

### HTML

```html
<main class="wrap">
  <button class="btn-3d btn-1">
    <div class="inner">
      <svg viewBox="0 0 24 24"><path d="M8 5v14l11-7z" /></svg>
      <div class="label">Start</div>
    </div>
  </button>
  <button class="btn-3d btn-2">
    <div class="inner">
      <svg viewBox="0 0 24 24"><path d="M12 17.27 18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z" /></svg>
      <div class="label">Favorite</div>
    </div>
  </button>
  <button class="btn-3d btn-3">
    <div class="inner">
      <svg viewBox="0 0 24 24"><path d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z" /></svg>
      <div class="label">Confirm</div>
    </div>
  </button>
  <button class="btn-3d btn-4">
    <div class="inner">
      <svg viewBox="0 0 24 24"><path d="M19.14 12.94c.04-.31.06-.63.06-.94s-.02-.63-.06-.94l2.03-1.58a.5.5 0 0 0 .12-.64l-1.92-3.32a.5.5 0 0 0-.6-.22l-2.39.96a7.025 7.025 0 0 0-1.62-.94l-.36-2.54A.5.5 0 0 0 14 2h-4a.5.5 0 0 0-.5.42l-.36 2.54c-.59.23-1.13.54-1.62.94l-2.39-.96a.5.5 0 0 0-.6.22L2.61 8.84a.5.5 0 0 0 .12.64l2.03 1.58c-.04.31-.06.63-.06.94s.02.63.06.94l-2.03 1.58a.5.5 0 0 0-.12.64l1.92 3.32c.14.24.43.34.7.22l2.39-.96c.49.4 1.03.71 1.62.94l.36 2.54a.5.5 0 0 0 .5.42h4a.5.5 0 0 0 .5-.42l.36-2.54c.59-.23 1.13-.54 1.62-.94l2.39.96c.27.11.56.02.7-.22l1.92-3.32a.5.5 0 0 0-.12-.64l-2.03-1.58zM12 15.5A3.5 3.5 0 1 1 15.5 12 3.5 3.5 0 0 1 12 15.5z" /></svg>
      <div class="label">Settings</div>
    </div>
  </button>
</main>
```

### CSS

```css
:root {
  --bg: #0f1724;
  --accent1: #06b6d4;
  --accent2: #7c3aed;
  --accent3: #f59e0b;
  --accent4: #10b981;
  --light: #fff;
  font-family: Inter, system-ui, sans-serif;
}

.wrap {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 24px;
}

.btn-3d {
  background: none;
  border: none;
  perspective: 1000px;
  cursor: pointer;
  position: relative;
}

.btn-3d .inner {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 14px;
  min-height: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  transform-style: preserve-3d;
  transition: transform 0.4s cubic-bezier(0.18, 0.9, 0.32, 1), box-shadow 0.3s;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
}

.btn-3d:hover .inner {
  transform: rotateX(12deg) rotateY(-10deg) translateY(-8px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.6);
}

.btn-3d:active .inner {
  transform: translateY(2px) scale(0.97);
}

.btn-3d .label {
  font-weight: 600;
  font-size: 16px;
  color: var(--light);
}

.btn-3d svg {
  width: 50px;
  height: 50px;
  fill: var(--light);
  transition: transform 0.6s ease, fill 0.3s ease, filter 0.4s ease;
  filter: drop-shadow(0 0 0 transparent);
}

.btn-3d.btn-1 .inner { background: linear-gradient(145deg, #022c43, #053f5e); }
.btn-3d.btn-2 .inner { background: linear-gradient(145deg, #24104f, #3a0ca3); }
.btn-3d.btn-3 .inner { background: linear-gradient(145deg, #5a3d00, #f59e0b); }
.btn-3d.btn-4 .inner { background: linear-gradient(145deg, #004d40, #10b981); }

.btn-3d.btn-1:hover svg { transform: scale(1.3) rotate(10deg); fill: #06b6d4; filter: drop-shadow(0 0 10px #06b6d4); }
.btn-3d.btn-2:hover svg { transform: scale(1.3) rotate(10deg); fill: #7c3aed; filter: drop-shadow(0 0 10px #7c3aed); }
.btn-3d.btn-3:hover svg { transform: scale(1.3) rotate(10deg); fill: #f59e0b; filter: drop-shadow(0 0 10px #f59e0b); }
.btn-3d.btn-4:hover svg { transform: scale(1.3) rotate(10deg); fill: #10b981; filter: drop-shadow(0 0 10px #10b981); }

.btn-3d .pulse { animation: motorph-pulse 0.5s ease, motorph-glow 0.5s ease; }

@keyframes motorph-pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.25); }
  100% { transform: scale(1); }
}

@keyframes motorph-glow {
  0% { filter: drop-shadow(0 0 0 transparent); }
  50% { filter: drop-shadow(0 0 14px currentColor); }
  100% { filter: drop-shadow(0 0 0 transparent); }
}

@media (prefers-reduced-motion: reduce) {
  .btn-3d .inner, .btn-3d svg { transition: none; }
  .btn-3d .pulse { animation: none; }
}
```

### JavaScript

```javascript
document.querySelectorAll(".btn-3d").forEach((btn) => {
  btn.addEventListener("click", () => {
    const svg = btn.querySelector("svg");
    if (!svg) return;
    svg.classList.add("pulse");
    window.setTimeout(() => svg.classList.remove("pulse"), 500);
  });
});
```

## Appendix B — Logout button sample

### HTML

```html
<button class="motorph-logout" type="button">
  <span class="button-outer">
    <span class="button-inner"><span>Log out</span></span>
  </span>
</button>
```

### CSS

```css
.motorph-logout {
  --logout-bg: rgba(0, 0, 0, 0.75);
  all: unset;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  position: relative;
  border-radius: 999vw;
  background-color: var(--logout-bg);
  box-shadow:
    -0.15em -0.15em 0.15em -0.075em rgba(5, 5, 5, 0.25),
    0.0375em 0.0375em 0.0675em 0 rgba(5, 5, 5, 0.1);
}

.motorph-logout::after {
  content: "";
  position: absolute;
  z-index: 0;
  width: calc(100% + 0.3em);
  height: calc(100% + 0.3em);
  top: -0.15em;
  left: -0.15em;
  border-radius: inherit;
  background: linear-gradient(-135deg, rgba(5, 5, 5, 0.5), transparent 20%, transparent 100%);
  filter: blur(0.0125em);
  opacity: 0.25;
  mix-blend-mode: multiply;
}

.motorph-logout .button-outer {
  position: relative;
  z-index: 1;
  border-radius: inherit;
  transition: box-shadow 300ms ease;
  box-shadow:
    0 0.05em 0.05em -0.01em rgba(5, 5, 5, 1),
    0 0.01em 0.01em -0.01em rgba(5, 5, 5, 0.5),
    0.15em 0.3em 0.1em -0.01em rgba(5, 5, 5, 0.25);
}

.motorph-logout:hover .button-outer {
  box-shadow: 0 0 0 0 rgba(5, 5, 5, 1), 0 0 0 0 rgba(5, 5, 5, 0.5), 0 0 0 0 rgba(5, 5, 5, 0.25);
}

.motorph-logout .button-inner {
  position: relative;
  z-index: 1;
  border-radius: inherit;
  padding: 0.65em 1.05em;
  background-image: linear-gradient(135deg, #e6e6e6, #b4b4b4);
  transition: box-shadow 300ms ease, clip-path 250ms ease, background-image 250ms ease, transform 250ms ease;
  overflow: clip;
  clip-path: inset(0 round 999vw);
  box-shadow:
    0 0 0 0 inset rgba(5, 5, 5, 0.1),
    -0.05em -0.05em 0.05em 0 inset rgba(5, 5, 5, 0.25),
    0 0 0 0 inset rgba(5, 5, 5, 0.1),
    0 0 0.05em 0.2em inset rgba(255, 255, 255, 0.25),
    0.025em 0.05em 0.1em 0 inset rgba(255, 255, 255, 1),
    0.12em 0.12em 0.12em inset rgba(255, 255, 255, 0.25),
    -0.075em -0.25em 0.25em 0.1em inset rgba(5, 5, 5, 0.25);
}

.motorph-logout:hover .button-inner {
  clip-path: inset(2px round 999vw);
  box-shadow:
    0.1em 0.15em 0.05em 0 inset rgba(5, 5, 5, 0.75),
    -0.025em -0.03em 0.05em 0.025em inset rgba(5, 5, 5, 0.5),
    0.25em 0.25em 0.2em 0 inset rgba(5, 5, 5, 0.5),
    0 0 0.05em 0.5em inset rgba(255, 255, 255, 0.15),
    0 0 0 0 inset rgba(255, 255, 255, 1),
    0.12em 0.12em 0.12em inset rgba(255, 255, 255, 0.25),
    -0.075em -0.12em 0.2em 0.1em inset rgba(5, 5, 5, 0.25);
}

.motorph-logout .button-inner > span {
  position: relative;
  z-index: 4;
  font-family: Inter, system-ui, sans-serif;
  letter-spacing: -0.05em;
  font-weight: 600;
  color: transparent;
  background-image: linear-gradient(135deg, #191919, #4b4b4b);
  -webkit-background-clip: text;
  background-clip: text;
  transition: transform 250ms ease;
  display: block;
  user-select: none;
}

.motorph-logout:hover .button-inner > span { transform: scale(0.975); }
.motorph-logout:active .button-inner { transform: scale(0.975); }
.motorph-logout:focus-visible { outline: 3px solid var(--motorph-accent, #b21f2d); outline-offset: 4px; }

@media (prefers-reduced-motion: reduce) {
  .motorph-logout .button-outer,
  .motorph-logout .button-inner,
  .motorph-logout .button-inner > span { transition: none; }
}
```

## Appendix C — Overview “Your Pages” isometric tile sample

### HTML

```html
<main class="motorph-page-tiles">
  <section class="motorph-page-tiles__iso">
    <div class="motorph-page-tiles__container">
      <button class="motorph-page-tile" type="button">
        <span class="motorph-page-tile__content">My Attendance</span>
        <span class="motorph-page-tile__side" aria-hidden="true"></span>
        <span class="motorph-page-tile__front" aria-hidden="true"></span>
      </button>
      <button class="motorph-page-tile" type="button">
        <span class="motorph-page-tile__content">My Pay</span>
        <span class="motorph-page-tile__side" aria-hidden="true"></span>
        <span class="motorph-page-tile__front" aria-hidden="true"></span>
      </button>
    </div>
  </section>
</main>
```

### CSS

```css
.motorph-page-tiles {
  --tile-text: #eee;
  --tile-base: #222;
  --tile-side: #161616;
  --tile-front: #262626;
  --tile-size: 1em;
  color: var(--tile-text);
  display: grid;
  place-content: center;
  width: 100%;
}

.motorph-page-tiles__iso {
  --iso: scaleX(0.866025) rotateZ(30deg) skewX(150deg);
  max-width: 45ch;
  transform: var(--iso);
}

.motorph-page-tiles__container {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5em;
}

.motorph-page-tile {
  --magic: 0.866025;
  --shift: translate(calc(-1 * var(--tile-size)), calc(-1 * var(--tile-size) * var(--magic)));
  position: relative;
  font: inherit;
  color: inherit;
  background-color: var(--tile-base);
  border: 0;
  padding: 0;
  cursor: pointer;
  transform: var(--shift);
  transition: transform 400ms ease;
}

.motorph-page-tile__content {
  display: block;
  background-color: var(--tile-base);
  padding: 1em;
}

.motorph-page-tile__side,
.motorph-page-tile__front {
  position: absolute;
  transition: transform 400ms ease;
  z-index: 1;
}

.motorph-page-tile__front {
  inset: 0 calc(var(--tile-size) * -2) 0 100%;
  background-color: var(--tile-front);
  transform-origin: center left;
  transform: skewY(41.25deg) scaleX(0.5);
}

.motorph-page-tile__side {
  inset: 100% 0 calc(-2 * var(--tile-size) * var(--magic)) 0;
  background-color: var(--tile-side);
  transform-origin: top center;
  transform: skewX(49deg) scaleY(0.5);
}

.motorph-page-tile:hover {
  --shift: translate(calc(-2 * var(--tile-size)), calc(-2 * var(--tile-size) * var(--magic)));
}

.motorph-page-tile:active { --shift: translate(0, 0); }
.motorph-page-tile:hover .motorph-page-tile__front { transform: skewY(41.25deg) scaleX(1); }
.motorph-page-tile:hover .motorph-page-tile__side { transform: skewX(49deg) scaleY(1); }
.motorph-page-tile:active .motorph-page-tile__front { transform: skewY(41.25deg) scaleX(0); }
.motorph-page-tile:active .motorph-page-tile__side { transform: skewX(49deg) scaleY(0); }
.motorph-page-tile:focus-visible { outline: 3px solid var(--motorph-accent, #b21f2d); outline-offset: 6px; }

@media (max-width: 640px) {
  .motorph-page-tiles__container { gap: 1em; }
  .motorph-page-tile { font-size: 0.85em; }
}

@media (prefers-reduced-motion: reduce) {
  .motorph-page-tile,
  .motorph-page-tile__side,
  .motorph-page-tile__front { transition: none; }
}
```

---

## 16. Production-readiness requirements: the 12 missing items

Treat the following as required implementation work, not a post-launch wish list. Inspect the existing routes and deployment setup first, then implement each item in the conventions of the current stack. Do not create decorative placeholders that merely satisfy a checklist.

### 16.1 Custom 404 page

- Implement a branded not-found page that matches the MotorPH visual identity.
- Include a clear explanation and working navigation back to the landing page or the user's permitted dashboard.
- Unknown routes must return the correct HTTP 404 status where server routing permits; do not render a 404 design with HTTP 200 (soft 404).
- Do not disclose whether a protected route or resource exists to unauthorised users. Apply the application's normal authentication and authorisation rules.

### 16.2 Above-the-fold call to action

- The landing page must present its primary CTA within the initial viewport on common desktop and mobile dimensions, without requiring scrolling.
- The primary action is **Login**; **Contact Help** remains the secondary action.
- The CTA must be visually prominent, have a clear action label, and navigate to the real login/help routes.
- Keep the MotorPH identity and the existing landing-page reference requirements in Section 12.1. Do not let decorative effects push the CTA below the fold or obscure it.

### 16.3 Unique meta title per page

- Give every route a unique, accurate HTML document title. Do not reuse one generic title across every route.
- Use a consistent title format, such as `Page Name | MotorPH`, while keeping titles concise and descriptive.
- Do not expose private employee, payroll, or account details in document titles.

### 16.4 Unique meta description per page

- Give every public page an accurate meta description that describes the actual page and does not promise features that are absent.
- Do not reuse one generic description across every route. Keep descriptions concise and useful in search/social previews.
- Set canonical URLs only when a stable public canonical URL exists. Do not invent a production domain.
- For authenticated employee, HR, payroll, and administrative pages, use appropriate `noindex` directives. Do not expose private employee, payroll, or account content to search engines.

### 16.5 Open Graph and social sharing image

- Create a branded Open Graph image for public-facing pages, with the correct dimensions and legible composition at social-preview size.
- Configure `og:title`, `og:description`, `og:type`, `og:url`, and `og:image`, plus suitable Twitter/X card metadata.
- Use absolute image URLs based on the actual configured public origin. Do not use localhost URLs in production metadata.
- Do not include employee names, salary figures, payslip data, or other private information in the image or metadata.

### 16.6 Favicon set

- Produce a coherent favicon set derived from the existing MotorPH logo/mark, including the standard favicon, an appropriate SVG or PNG variant, Apple touch icon, and web-app manifest icons where supported.
- Add the correct `<link>` references and verify that every asset path resolves after build and deployment.
- Preserve the source logo; do not redraw or distort the brand mark without need.

### 16.7 `robots.txt`

- Add a valid `robots.txt` at the public site root, using the actual production origin only when known.
- Use it as crawler guidance, not as access control. Server-side authentication and authorisation must protect private routes regardless of crawler directives.
- If the application is an internal-only deployment with no public search presence, document that and configure indexing conservatively.

### 16.8 `sitemap.xml`

- Add a valid `sitemap.xml` at the public site root, using the actual production origin only when known.
- Include only intended public, indexable routes. Exclude login-protected pages, employee records, payroll, payslips, internal APIs, and any route containing private or user-specific data.

### 16.9 Alt text for every meaningful image

- Every meaningful `<img>`, image component, and informative image used in the interface must have concise, context-appropriate alternative text.
- Decorative images must use empty alt text (`alt=""`) or the equivalent accessibility mechanism so screen readers do not announce irrelevant decoration.
- Do not put filenames, generic phrases such as "image", or redundant descriptions in alt text.
- Background images that convey information must have an accessible text equivalent in the page content.
- Include an automated check or test that detects missing alt attributes, then manually review whether the text is useful.

### 16.10 Loading, empty, error, and success states

- Implement visible loading feedback for route transitions and asynchronous operations, including login, employee data, attendance, payroll, reports, payslip generation, and form submissions.
- Prevent duplicate submissions while an operation is pending, unless the operation is explicitly safe to repeat.
- Provide meaningful empty, error, success, and permission-denied states where applicable. Preserve entered form data after recoverable errors.
- Use skeletons only where they improve perceived loading; avoid layout shifts and indefinite spinners.
- Respect reduced-motion preferences and ensure status changes are announced accessibly, for example with suitable live regions.

### 16.11 Privacy policy page

- Add a public Privacy Policy page linked from the landing page and other appropriate public entry points.
- Describe the information the application actually collects and processes, the purposes, access roles, retention/deletion approach, security controls, analytics use, and a contact route for privacy questions or requests.
- Align the policy with the application's real behaviour and the applicable Philippine Data Privacy Act and NPC requirements. Verify legal references against current authoritative sources.
- Do not invent a Data Protection Officer, company address, retention period, lawful basis, third-party processor, or contact email. Use clearly marked configuration placeholders for details that the owner must supply, and document them for completion before public deployment.
- Do not claim that the application is compliant merely because a policy page exists. The implementation and actual processing practices must match the published policy.

### 16.12 Analytics

- Install privacy-conscious analytics only after identifying a suitable provider compatible with the deployment and the application's privacy obligations. Prefer a minimal, cookieless or consent-respecting configuration where practical.
- Keep analytics disabled on sensitive authenticated pages by default unless there is a documented need and a lawful, disclosed basis. Never send Employee IDs, names, email addresses, salaries, payslip values, attendance details, request contents, passwords, session identifiers, or other personal/sensitive data as analytics properties, URLs, event labels, or free-text fields.
- Track only necessary aggregate product events, such as public landing-page visits, CTA clicks, and coarse navigation events, with data minimisation and retention settings.
- Add the required disclosure to the Privacy Policy and implement consent controls if required by the chosen provider, jurisdiction, or tracking technology.
- Keep provider keys and configuration in environment variables. Do not commit secrets. Add an environment flag so analytics can be disabled in local/test environments.
- Verify that the analytics script loads correctly in the intended deployment and that no private application data is transmitted. If no provider credentials or deployment origin are available, implement the integration point and configuration documentation without fabricating successful live tracking.

### 16.13 Production-readiness acceptance checklist

The work is complete only when all applicable items below pass:

- [ ] Unknown route displays the custom 404 page and returns HTTP 404.
- [ ] Login CTA and Contact Help are visible without scrolling at 320px-wide mobile and common desktop viewport sizes.
- [ ] Every route has a unique title and an appropriate meta description.
- [ ] Public-page Open Graph preview metadata points to a valid, deployed image.
- [ ] Favicon and touch-icon assets resolve in the built application.
- [ ] `robots.txt` is valid and does not act as a substitute for access control.
- [ ] `sitemap.xml` contains only intended public routes, with no private routes or personal data.
- [ ] Every image has reviewed alt text or is explicitly marked decorative.
- [ ] Loading, empty, error, success, and permission states work for relevant async flows.
- [ ] Privacy Policy matches actual data handling and contains no invented organisational details.
- [ ] Analytics records only approved, non-sensitive events and can be disabled outside production.
- [ ] Lighthouse/SEO and accessibility checks are run where the deployment environment permits; report actual results and unresolved warnings.

## 17. Skills, slash commands, and external design references

Use `skills_compilation.md` as an active skill library, not merely a document to skim. Before implementation, inspect its skill index and relevant skill bodies, identify the commands actually supported by the current agent, and invoke the relevant skills at the point of work. A skill name is not automatically a slash command: verify its frontmatter, invocation instructions, and whether the current agent supports slash-command invocation. If a command is unavailable, follow the skill's instructions manually where possible and record the limitation. Do not invent command names or claim a skill ran when it did not.

### 17.1 Skills already in `skills_compilation.md` to apply

Use the following as the core workflow for this project. These names are present in the supplied compilation; invoke them through the current agent's supported mechanism rather than assuming every harness uses identical slash syntax.

| Skill / command | When to use it in this task |
|---|---|
| `using-agent-skills` | Start here to select skills by task phase and avoid loading unrelated material. |
| `spec-driven-development` | Reconcile the implementation brief into testable capabilities and acceptance criteria before large changes. |
| `planning-and-task-breakdown` | Break the work into dependency-ordered tasks after the scope is clear. |
| `incremental-implementation` | Implement in small, testable slices while preserving the working application. |
| `frontend-ui-engineering` | Landing page, responsive layout, accessibility, loading/error states, metadata, and visual polish. |
| `api-and-interface-design` | Authentication, sessions, API contracts, permission boundaries, and backend/frontend interfaces. |
| `security-and-hardening` | Password handling, sessions, CSRF, access control, privacy, analytics data minimisation, and sensitive records. |
| `source-driven-development` | Verify framework APIs, statutory rules, analytics configuration, and third-party integration details against authoritative documentation. |
| `test-driven-development` | Add or update unit, integration, and regression tests for each behaviour. |
| `browser-testing-with-devtools` | Verify actual page behaviour, responsive states, accessibility, console/network errors, and navigation. |
| `observability-and-instrumentation` | Add appropriate operational logging and the limited analytics/instrumentation described above, without logging personal or sensitive payroll data. |
| `performance-optimization` | Check asset loading, page performance, animation cost, and Core Web Vitals. |
| `code-review-and-quality` | Review the completed changes for correctness, security, maintainability, and regressions. |
| `verification-before-completion` | Run the relevant checks and report only outcomes actually observed. |
| `shipping-and-launch` | Complete deployment-readiness checks, environment configuration, and launch documentation. |

Relevant commands explicitly present in the compilation include `/caveman-review`, `/caveman-optimize`, `/caveman-setup`, `/caveman-help`, `/caveman-discover`, `/caveman-evidence-review`, `/caveman-learn`, `/caveman-manage`, `/caveman-stats`, `/caveman-commit`, `/caveman-compress`, `/investigate-first`, `/lean-build`, `/migration`, `/safe-refactor`, `/surgical-patch`, and `/verify-and-stop`. These belong to the Cavecrew/Caveman skill family; inspect their definitions before use and do not invoke them as generic substitutes for the engineering skills above.

For this implementation, prioritise `/investigate-first` before repository changes, `/lean-build` for controlled implementation, `/caveman-review` for a quality review, and `/verify-and-stop` once the acceptance checks have passed. Use `/caveman-optimize` only when a measured performance or code-quality issue warrants it. Do not run every command mechanically.

### 17.2 External skills and repositories: selected recommendations

Do not install every listed anti-slop skill. Several overlap heavily, and stacking their conflicting prose rules can make the agent's writing less natural. Inspect the repository, README, license, supported harnesses, and skill instructions before installing anything. Add only the skills that materially improve this project, and record the exact source and version/commit in the compilation.

**Recommended for this project:**

1. **Impeccable — `pbakaus/impeccable`**. Strongly recommended for the landing page and all UI work. It supplies a design-oriented workflow and commands for critiquing, shaping, and polishing frontend interfaces. Install the current `impeccable` skill (not the deprecated `frontend-design` alias):
   ```bash
   npx skills@latest add https://github.com/pbakaus/impeccable --skill impeccable
   ```
   Use `/impeccable` as the entry command, then consult its current command list for focused operations such as critique, polish, and design-system work. Do not guess subcommand names; use only those documented by the installed version.

2. **Google Elephant-Goldfish Model — `google/elephant-goldfish-model`**. Recommended for reviewing the large implementation brief and the design/architecture decisions before implementation. Its Elephant role drafts specifications from project context; fresh-context Goldfish reviewers challenge missing assumptions and risks. It is a plugin/workflow, not a generic UI skill. Install only if the current agent supports it:
   ```bash
   # Claude Code
   claude plugin marketplace add google/elephant-goldfish-model
   claude plugin install egm@egm

   # Codex CLI
   codex plugin marketplace add google/elephant-goldfish-model
   codex plugin add egm@egm
   ```
   Relevant commands include `/egm:egm-brainstorm`, `/egm:egm-draft`, `/egm:egm-resolve`, `/egm:egm-review`, and `/egm:egm-bootstrap` in Claude Code; verify exact command names for the installed release and harness. Use the design-review flow on the capability/specification documents, not as a substitute for implementation tests.

3. **Matt Pocock Skills — `mattpocock/skills`**. Recommended selectively for engineering workflow and better requirements discovery. Install the repository's engineering skills and run its setup command once per repository:
   ```bash
   npx skills@latest add mattpocock/skills
   ```
   ```text
   /setup-matt-pocock-skills
   ```
   Start with `/ask-matt` to select an appropriate workflow and `/grill-me` or `/grill-with-docs` when requirements or domain assumptions need sharper definition. Use `/to-spec` and `/to-tickets` only if the repository's workflow benefits from maintaining those artefacts. Do not introduce a second issue tracker or duplicate specification system if the project already has one.

4. **Warp `skill-doctor` — `warpdotdev/common-skills`**. Optional, useful after implementation to assess whether the agent's installed skills are actually improving its work. It analyses local agent sessions and skills; it is not a code-quality scanner. Install with:
   ```bash
   npx skills@latest add https://github.com/warpdotdev/common-skills --skill skill-doctor
   ```
   Invoke `/skill-doctor` only if the current harness exposes it and the user has authorised analysis of the relevant local conversation history. Follow its privacy and harness-support requirements; never upload transcripts.

**Choose at most one primary anti-slop writing skill:**

- Preferred: **Slopkit / Slopbeth** (`ehmo/slopkit`). It distinguishes editing shipped prose from shaping the agent's own status messages, and emphasises preserving meaning, source accuracy, and the author's voice. Install both skills using:
  ```bash
  npx github:ehmo/slopkit install
  ```
  Use `/slopbeth` for README, policy, help text, and other prose artefacts, and `/slopgent` for the agent's own progress and completion reports, if the installed harness exposes these invocations. Treat its scripts as diagnostic aids, not objective measures of writing quality.
- Alternative: **Soundshuman** (`aashaexo/soundshuman`), particularly for repo-wide prose audits and voice preservation:
  ```bash
  npx skills@latest add https://github.com/aashaexo/soundshuman --skill humanize
  ```
  Invoke `/humanize` when editing documentation or product copy, if supported by the harness.
- Alternative: **Anti-AI-Slop Writing** (`jalaalrd/anti-ai-slop-writing`):
  ```bash
  npx skills@latest add https://github.com/jalaalrd/anti-ai-slop-writing --skill anti-ai-slop-writing
  ```
  Invoke `/anti-ai-slop-writing` for copy editing, if supported. Do not combine its rigid sentence/punctuation constraints with another anti-slop skill unless their instructions have been reconciled.

The other supplied anti-slop candidates — `elithrar/dotfiles` (`anti-slop`), `stephenturner/skills` (`deslop`), `aboudjem/humanizer-skill` (`humanizer`), `cursor/plugins` (`unslop`), `petergyang/no-ai-slop` (`no-ai-slop`), and `hardikpandya/stop-slop` — are alternatives, not a bundle to install. Inspect them only if the preferred option proves unsuitable. Do not install duplicate humanizers merely to increase the number of skills.

**Not a first-choice dependency:** `Leocanela279/how-tf-can-ai-have-taste`. Inspect it for useful design judgement or critique techniques if Impeccable leaves a specific gap, but do not add a second overlapping design system without a concrete benefit. Its repository is a reference, not a required runtime dependency.

### 17.3 Extend `skills_compilation.md`

Add the selected external skills to `skills_compilation.md` so the project has one searchable skill reference. Do this only after inspecting the actual upstream source and license. For each addition:

- Preserve the original `SKILL.md` content and any required companion references/scripts/assets; do not paste only a README summary and label it a complete skill.
- Add a source header with repository URL, exact skill path, retrieval date, commit SHA or release/version, and license.
- Retain upstream attribution and copyright notices. Do not silently alter the upstream skill text.
- Include a short local index entry describing the skill's purpose, supported agent/harnesses, invocation command (if any), and when this project should use it.
- Avoid duplicate skills. For anti-slop skills, add only the selected primary option unless a second one has a clearly separate purpose.
- If a repository is a plugin rather than a standalone skill, document its installation and supported commands separately; do not misrepresent it as a `SKILL.md` file.
- If a skill requires external scripts, dependencies, network access, telemetry, or access to conversation history, inspect those requirements and document the implications before use.
- Keep the compilation's existing source delimiters and index conventions intact. Rebuild or update any generated index/count/hash metadata if the file format provides for it.

### 17.4 Subframe design inspiration — required for landing-page redesign

Use Subframe's `search_inspiration` tool before designing the landing page. Search for real website references relevant to an employee/payroll platform, corporate service portal, workforce management product, and polished login/landing experience. Review the returned references, identify the useful layout, typography, colour, interaction, and CTA patterns, then design a MotorPH page that fits the existing brand rather than copying another site's identity.

The exact instruction to run is:

```text
Use the `search_inspiration` tool from Subframe to find inspiration, then design a landing page for my project.
```

If Subframe is not connected, check whether the current agent supports its MCP integration and report the required setup rather than pretending the tool was used. For Claude Code, Subframe documents this connection command:

```bash
claude mcp add --transport http subframe https://mcp.subframe.com/mcp
```

Then authenticate using the agent's MCP interface (for Claude Code, `/mcp`). If the current harness cannot connect to Subframe, use the supplied visual references and the Impeccable skill as the fallback, clearly recording that Subframe inspiration search was unavailable. Do not fabricate inspiration results.

### 17.5 Design execution and review

- Use Subframe inspiration to inform the landing page's visual direction, then use Impeccable to critique and refine the actual implementation.
- Preserve the existing MotorPH logo, palette, and identity. Inspiration is not permission to clone another brand or copy its exact page.
- Avoid generic template output, gratuitous gradients, oversized rounded cards, arbitrary decorative animation, and stock dashboard layouts. Every visual decision should serve the product's audience and task.
- Capture before/after screenshots at desktop and mobile sizes where browser tooling permits. Review the rendered page, not just the source code.
- Use keyboard navigation, screen-reader semantics, contrast checks, reduced-motion settings, and the loading/error/empty states from Section 14 as part of the final visual review.

**Final instruction:** Implement the application, not just the appearance. Every visible control must perform its intended action, every protected operation must be authorised by the server, and every displayed payroll value must be traceable to its inputs and calculation rules.
