# `md/` — the project's documents in one place

Everything a person needs to understand, rebuild or defend this project, gathered out of the three working folders
and the uploads directory. The working folders themselves can be trimmed; **nothing in this folder should be.**

```
md/
├── ALL-GITHUB-LINKS.md      every GitHub (and CodePen / statutory) source, and what each one was for
├── README.md                this index
├── markdown/                the Markdown documents — briefs, designs, reports, progress
│   └── claude-build-docs/   26 documents from the earlier build snapshot
├── csv/                     the 9 data files — the real MotorPH employee/attendance/payslip data
└── documents/               the 13 source documents — the course PDFs, diagrams, rules, hand-off text
```

## `markdown/` — what each document is

**The requirements and the design.** These are the authority for the build; where code and these disagree, these win.

| File | What it is |
|---|---|
| `MotorPH_Optimised_Implementation_Prompt_v2.md` | **The brief.** Every requirement the rebuild is measured against |
| `Copy of MotorPH-Business Requirement Specification.pdf` | (in `documents/`) the client's own requirements |
| `MO-IT108 - 2nd Milestone_ System Design Document…pdf` | (in `documents/`) the RBAC matrix, role descriptions, scenarios |
| `MO-IT107 - 2nd Milestone_ Database Design…pdf` | (in `documents/`) the data model |
| `MOTORPH_AGENT_HANDOFF.md` | The prior agent's hand-off: what was built, what was left |
| `MotorPH_Payroll_System_Full_Synthesis.md` | Synthesis of all the source material |
| `MotorPH_OOP_Feature_Inventory_and_Pseudocode.md` | Feature inventory with pseudocode |
| `Philippine_Payroll_API_Calculation.md` | The statutory arithmetic, sourced |

**This rebuild's own documents** — written while doing the work.

| File | What it is |
|---|---|
| `build-comparison.md` | **Claude's build vs. the current workspace**, and which parts of each survive |
| `branch-assignment.md` | How the 34 employees were placed into branches (the centroid rule) |
| `rbac-matrix-conformance.md` | The design document's permissions matrix, transcribed, and the three places the code had drifted from it |
| `session-progress.md` | The state of the golden payslip work: what closed, what opened |
| `brief-conformance-audit.md` | **Brief vs. build, section by section** — what is done, what is not, and the judgement calls |
| `merge-audit.md` | **Claude's base vs. ours** — class-by-class and module-by-module, with the five bugs the comparison caught |
| `data-provenance.md` | **Which data is real and which I generated**, the full assumption list, and what was deliberately not invented |
| `README.md` / `SETUP.md` | How the application is built and run |

**The peer review.** The comparison against the other submissions.

| File | What it is |
|---|---|
| `submission-comprehensive-report.md` | The full review of the peer corpus |
| `submission-folder-analysis.md` | Folder-by-folder breakdown |
| `submission-feature-pseudocode.md` | Peer feature pseudocode |
| `submission-test-results.md` | What the peers' tests actually proved |
| `submission-retest-addendum.md` | Re-test notes |
| `motorph-build-plan.md` | The plan derived from the review |
| `ms2-firebase-task-breakdown.md` | Task breakdown for the Firebase milestone |
| `repo-review-README.md` | The review corpus index |

**Baseline and policy.**

| File | What it is |
|---|---|
| `acceptance-baseline.md` | The acceptance criteria |
| `api-contract.md` | The HTTP API contract |
| `candidate-policy-review.md` | Open policy questions and their sources |
| `source-grounding-and-open-rules.md` | Which rules are sourced and which are still open |
| `peer-parity-roadmap.md` | Feature parity against the peers |
| `legacy-mo-it108-traceability.md` | Traceability to the MO-IT108 milestone |
| `local-demo-inventory.md`, `local-only-inventory-and-full-build-pseudocode.md` | What runs locally, with pseudocode |
| `firebase-setup-guide.md`, `firestore-rules-review.md` | Firestore setup and the rules review |
| `CHANGELOG-2026-11-05.md` | The prior build's changelog, item by item |

## `csv/` — the data

| File | What it is |
|---|---|
| `Copy of MotorPH_Employee Data - Employee Details.csv` | **The real employee file** — 34 people, IDs 10001–10034. The source of every salary, allowance and reporting line |
| `EmployeeDetails.csv` | The same file as it ships inside the application (`src/main/resources/data/`) |
| `Copy of MotorPH Payslip - MotorPH Payslip.csv` | **The golden payslip** — 10015, 2026-01, take-home 22,711.67 |
| `SamplePayslip.csv` | The shipped copy of the same |
| `Copy of MotorPH Sample Timecard - MotorPH Payslip.csv` | The sample attendance |
| `SampleTimecard.csv` | The shipped copy |
| `Copy of MotorPH Inventory Data - March 2023 Inventory Data.csv` | Inventory data (not used by payroll) |
| `MotorPH_Products_List_2025.csv` | Product list (not used by payroll) |
| `MotorPH_Sales Data-3rd Quarter-Year 2025.csv` | Sales data (not used by payroll) |

The three non-payroll files are kept because they are part of the supplied MotorPH data set, not because the
payroll engine reads them.

## `documents/` — the source documents

| File | What it is |
|---|---|
| `MO-IT108 - 2nd Milestone_ System Design Document…pdf` | **The design authority.** RBAC permissions matrix (p. 11), role descriptions (pp. 21–22), scenarios |
| `MO-IT108 - 1st Milestone_ Requirements Analysis Document…pdf` | Requirements analysis |
| `MO-IT108 - Terminal Assessment_ Business Technology Proposal…pdf` | The proposal |
| `MO-IT107 - 2nd Milestone_ Database Design…pdf` | The database design |
| `MO-IT107 Homework_ Data Dictionary…pdf` | The data dictionary |
| `MO-IT107 Homework_ Data Flow Diagram…pdf` | The data-flow diagram |
| `Copy of MotorPH-Business Requirement Specification.pdf` | The client's business requirements |
| `The Code.pdf` | Reference material |
| `Use Case Diagram (1).png` | The use-case diagram |
| `image-1.png` | A supplied screenshot |
| `firestore.rules` | The Firestore security rules |
| `file-list.txt` | The peer corpus file listing |
| `message-2.txt` | A supplied message |

---

## How this folder relates to the working folders

| Working folder | What it is | Status |
|---|---|---|
| `motorph-v3/` | The live build — Java engine, `sp_secpol` auth, `web/`, tests, `lib/` | **Active.** Everything in it is current |
| `motorph-ms2/` | The earlier snapshot of Claude's build | **Removed** — superseded by `motorph-v3`. Its 26 unique documents are in `markdown/claude-build-docs/` and its 4 unique stylesheets are back in `motorph-v3/web/` |
| `repo_review/` | The peer submission corpus and the review reports | Reports copied here; the raw corpus is on GitHub |
| `uploads/` | The original uploads | **Removed** — all 22 files were verified byte-identical here before deletion |

The single source of truth is **`motorph-v3/`**. If a file in this folder and a file in `motorph-v3/docs/` disagree,
`motorph-v3/docs/` is the newer one — but both were copied from the same state, so they should not.
