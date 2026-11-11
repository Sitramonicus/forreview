> Historical audit; current status is in [current-status.md](current-status.md).

> Historical audit: several findings below have since been fixed. For current status and remaining risks, see [current-status.md](current-status.md).

# MotorPH requirements re-check

Checked against `MotorPH_Optimised_Implementation_Prompt_v2.md` and the original prompt supplied in the conversation. This is an evidence-based review, not proof that every screen and calculation has been exhaustively tested. Date used for completed-period checks: 2026-11-02.

## Bottom line

**Not all requirements are met. Do not expose this build to the internet or use it for real employee or payroll data.**

The last full test run reported 20 suites passed, including 119 web tests and 5 browser checks. That result is real, but it does not prove complete security or requirements coverage: the independent checks below found serious gaps.

## Must fix first

1. **Unauthenticated impersonation and administrator access.** `AppServer.java:3703–3719` accepts `X-Demo-User` as an employee identity without a valid session; no header falls back to an ADMIN actor marked non-explicit. Some permission checks only test the role, not whether the actor is authenticated. On an isolated local server, requests with no cookie returned HTTP 200 from `/api/admin/roles` and `/api/employees`; a forged `X-Demo-User: 10005` returned 200 for another employee's attendance. No sensitive response bodies were saved. Remove the trust in caller-supplied identities, reject missing/invalid sessions on all protected endpoints, review every guard, and add negative API tests for unauthenticated, expired, revoked, and spoofed requests. The role matrix tests alone do not cover this trust boundary.
2. **Sign-in behavior — current instruction.** All accounts use the shared initial password specified for this build. After the code step, first sign-in opens the workspace directly; password changes are optional. IT can activate or deactivate accounts from System configuration → Account access & passwords. This replaces the earlier recommendation to force a password change at first sign-in.
3. **Firestore writes are not durable or atomic.** The current background writer acknowledges requests before Firestore saves complete. A crash, expired token or network failure can lose acknowledged updates. A full snapshot is spread over multiple documents without an atomic commit, so interrupted writes can leave a mixed snapshot. Full-state writes by more than one server instance can overwrite each other. See `docs/firebase-setup-guide.md`. Credentials that refresh automatically will not solve these persistence issues. Design transactions or another crash-safe publication protocol and surface persistence failures to operators and clients.
4. **Live-preview shutdown and data verification.** The Firestore launcher was reported stopped previously, but the actual Java child remained on port 8081 bound to `0.0.0.0`. During this re-check, its HTTP listener was stopped and its stuck Java process was forcibly terminated. No exposure or compromise is proven; it was nevertheless potentially reachable through the preview. Because termination may interrupt a multi-document save, the live Firestore snapshot has **not** been revalidated since shutdown. Before restarting or writing, make a read-only backup/export and verify the current remote state, then review access logs if available. Revoke all tokens pasted into the conversation.
5. **Incorrectly completed data periods.** `data/local-demo.json` contains 7,421 attendance records dated 2026-01-01 through 2026-11-07. Against 2026-11-02, November is incomplete and 130 rows are dated in the future. There are only 10 completed monthly periods in this dataset, not the requested preferred 12; no saved payslip snapshots exist. `docs/a_doc/data-provenance.md` describes a different 10,358-row, 2025–2026 dataset and is stale. Do not label generated attendance or calculated payslips as actual company history. Reconcile the seed dataset, period policy and notes without fabricating completed payroll.
6. **The My Pay comparison is not implemented as requested.** `Index.html:771–777` places the switch in a payroll preview restricted to PAYROLL/ADMIN, not all employees' My Pay. `App.js:2343–2352` changes a label only; it explicitly says nothing is recalculated. It does not provide two calculations on the same employee/period inputs or show differences. UI styling is not completion of this feature.
7. **Dependency vulnerabilities.** `npm audit --json` in this check reported 30 findings: 10 high, 20 moderate, 0 critical. High findings include the direct `lighthouse` development dependency and transitive packages. These numbers are not a finding that all vulnerabilities are reachable in the shipped app; review the advisories and affected environments, fix or record risk before release. No Java dependency security audit was run in this re-check.

## V2 brief: section-by-section

Legend: **Present** = an implementation/evidence exists, not blanket acceptance. **Partial** = some requirements exist but gaps remain. **Not established** = insufficient evidence in this check. **Missing** = a required capability is absent.

| Section | Status | Evidence / remaining work |
|---|---|---|
| 1–2: base, inspection, comparison | Partial | `build-comparison.md` and `merge-audit.md` record a comparison. Their claims that header identity forgery was eliminated conflict with current code and reproduction. The historic remote comparison was not independently rerun in this check. |
| 3: working implementation and safeguards | Partial | Java/web app exists, but unauthenticated access, non-atomic writes and ineffective comparison control violate key requirements. End-to-end usability of every control is not established. |
| 4: notation, file names, style | Partial | `member-notation.md` records model conversions and serialization pins. Several files still contain conventional names and many formal comments (including recent changes). The original preference for almost no comments is not fulfilled consistently. |
| 5: folders, tests and tooling | Partial | Source and web are divided by responsibility and there are unit and browser suites, ESLint and optional Java lint tooling. Browser coverage is five checks, not every workflow in the brief. Current dependency audit has unresolved high findings; independent HTML/CSS validation, full accessibility, all-role end-to-end, and Java dependency scanning not established. `tools/lint.sh` filters only selected PMD/SpotBugs findings and its pipelines can misreport tool errors as clean. |
| 6: employee accounts and passwords | Partial / unsafe | 34 employee records and Argon2id with random salts exist. The shared initial password and fallback pepper are hard-coded; password changes are not persisted; first-change enforcement at server boundaries is not established. Every employee's initial authentication was not individually retested. |
| 7: sessions | Partial / unsafe | Random session tokens, HttpOnly cookies, 15-minute idle and 8-hour absolute limits, CSRF and logout logic exist. Header impersonation and role-only checks bypass the session security boundary; therefore the acceptance criteria are not met. Privilege-change rotation and all protected routes were not independently verified. |
| 8: hierarchy and roles | Partial / unsafe | CSV-based hierarchy, recursive team scope and role tests exist. The external identity bypass defeats enforcement. Each executive's intended data scope and all exceptional reporting chains were not comprehensively retested here. |
| 9: branches | Partial | `branch-assignment.md` documents offline locality approximations and the Main Branch policy. The notes contain intermediate team rows that can conflict with their final counts; each CSV row was not independently recalculated during this re-check. |
| 10: attendance, payroll and provenance | Partial | Dataset contains 34 employees and 7,421 generated attendance rows; one golden payslip test passes. Dataset contains future dates and lacks 12 completed months as of 2026-11-02; persisted payroll snapshots are empty. Statutory coverage for all historical periods, holidays, classifications, and all employee/period reconciliation remains unproven. Provenance notes report stale numbers. |
| 11: Sweldong comparison | Missing | Switch only changes explanatory text and is on a payroll-only panel; it does not calculate or compare both methods for every employee. |
| 12.1–12.11: UI | Partial | Landing page, logo, rail, pay layout, print styling, tactile controls and effects exist. Actual Kubai/Animatopy/Dynamic.css resources were not used (`brief-conformance-audit.md`); adaptations are not necessarily equivalent to every requested interaction. Full hover/focus preview, collapsed header, touch, 320px CTA, print, accessibility and all controls were not rerun in this check. The pay switch is nonfunctional as a comparison. |
| 13: performance | Partial | A live Firestore sign-in/2FA timing was measured earlier; no target-environment load, throughput, query-pattern or concurrent-user benchmark is established. No Lean/io_uring performance figures are claimed. |
| 14: security, privacy, reliability | Not met | See impersonation, passwords, Firestore durability and dependency findings. Other classes of attack and all sensitive endpoints have not been comprehensively audited. |
| 15: sequence and acceptance | Not met | Many tests exist but key security, payroll-data, comparison, tooling, accessibility and reliability acceptance checks remain unmet or unverified. |
| 16: deliverables | Partial | Source, setup and multiple audit/decision documents exist; some are stale or contradict the code. Complete reliable checks and update provenance, role/security, limitations and deployment notes. |
| 16.1–16.13: production-readiness additions | Partial | Branded 404, landing CTA, meta tags, SVG favicon, privacy page, robots and empty sitemap exist. Open Graph image uses a relative URL (`Landing.html:12`), with no verified deployed preview; title/description uniqueness across every route, 320px CTA, image alt review and all async states are not fully verified. Privacy details require the owner's real information and legal review. Analytics integration is absent, including the requested disabled integration point. Prior Lighthouse numbers are historical, not a current full deployment audit. |
| 17: skills, external references and visual review | Partial / not established | `skills_compilation.md`, `skills.md` and an index exist and were consulted in this re-check. Prior invocation, installation, upstream provenance and before/after visual-review evidence are not fully established. Subframe connector lookup returned unsupported in this environment; no inspiration search was performed during this re-check. Do not claim it was. |

## Original prompt: additional cross-check

- **Use Claude's build as base, preserve later corrections:** comparison docs exist, but the prior report's claim that forged-header identity was removed is incorrect.
- **All CSV accounts, password, session, hierarchy and branches:** implemented in part; critical session bypass and non-persistent passwords mean the requirement is not fulfilled. Team-based branch positions are approximate, documented rather than precise geocoding.
- **Populate attendance/payroll:** generated attendance exists; the shipped file does not have the 12 completed months preferred by v2 or saved payroll history, and contains future records as of the date used here.
- **Remove visible fictional/version/demo/synthetic wording:** not completely done. `Index.html:691–702` and `:910` visibly use “version”; `:943` refers to `local-demo.json`. Internal provenance must remain truthful.
- **GUI references:** many visual adaptations exist; the named icon/animation libraries are not used, collapsed sidebar/reference fidelity is not fully established, and the all-employee MotorPH/Sweldong comparison is missing. `ui-components.md` treats relabelling as a completed switch; that is not the requirement.
- **Code conventions and notes:** folder split and some notation work exist; comment/docstring and consistent style preferences are not fully met. Notes exist but several are outdated or contradictory.
- **Lean example:** measured improvements are appropriate; reproducing Lean 4 or the video's throughput was expressly optional. No such claim is made.

## What was actually checked

- Read both supplied prompts, relevant current source, setup, comparison, provenance and component notes.
- Ran isolated HTTP authorization checks without storing response bodies; observed 200s described above.
- Inspected the actual local state file and counted dated attendance and saved-payslip snapshots.
- Ran `npm run lint:web`: passed. Ran `npm audit --json`: 10 high, 20 moderate at the time of the check.
- Last full suite (prior turn, after the writer change): 20 suites passed; 119 web tests and 5 browser checks included. Not rerun for this documentation-only review.
- Last live Firestore browser check (prior turn): sign-in completed and both labels said Firestore. This is not proof of security, crash safety or current remote integrity after the forced shutdown.
- Did not run a full penetration test, legal/statutory review, all-device accessibility review, or independent recalculation of every employee/period. Claims of absence or completion outside the checks above are not made.

## Recommended order

1. Keep the app offline; revoke exposed tokens and check the remote snapshot read-only.
2. Fix the session/identity bypass and enforce authorization centrally, with negative integration tests.
3. Fix persistent credentials, first-password-change enforcement, secret management and the demo 2FA limitation.
4. Make persistence atomic/durable and observable; then implement automatic refresh with Google Application Default Credentials and least-privilege IAM.
5. Correct the seeded period/provenance mismatch and implement the real employee-facing comparison.
6. Address dependency findings, expand acceptance tests and accessibility checks, and refresh contradictory documentation.
