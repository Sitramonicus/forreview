# Submission feature inventory and pseudocode — index

**Prepared:** 2026-11-04  
**Purpose:** feature inventory by major feature → minor feature → sub-functions, followed by source-grounded pseudocode for each listed major and minor feature. This set supplements the existing static review of numbered submissions 1–9 with the five repositories requested by the user (folders 10–14).

## Scope and evidence discipline

- **Folders 1–9:** see [`folders-01-to-09-normalized-map.md`](folders-01-to-09-normalized-map.md) for the normalized major/minor/sub-function crosswalk, and [`folders-01-to-09-major-pseudocode.md`](folders-01-to-09-major-pseudocode.md) for major-feature routines. Minor workflows are detailed in the existing `submission-feature-pseudocode.md`. Folder 2 and folder 5 are near-identical programs; do not count them as two distinct implementations. Folder 9 has the detailed uploaded example at `/home/user/uploads/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md` (4,702 lines as read in this session) and its copied project source in `full_repo/testingester-main/9/-/`.
- **Folders 10–14:** requested public repositories were downloaded as source snapshots under `full_repo/testingester-main/{10..14}/-/`. Each has an upstream snapshot note, preserving URL, branch, commit, and metadata. Detailed feature catalog and pseudocode are in the links below.
- Pseudocode documents **observed/repository-described behavior**, not a target design and not evidence of runtime correctness. Where a feature is only advertised by README or implied by a type/screen, that boundary is stated. No payroll/legal rule is imported as authoritative from a peer repository.
- Repo star totals are point-in-time GitHub API metadata checked 2026-11-04. The five URLs were user-selected. This document does not independently certify that each repo was top-ranked in GitHub topic search at some earlier query time.
- The repositories are third-party study/reference material, not evidence of the user's authorship or submission. Verify license and mentor/assignment permissions before adapting code into graded work. Never copy real employee data or secrets.

## Folder index

| Folder | Repository / project | Added or existing | Feature/pseudocode reference | Source |
|---:|---|---|---|---|
| 1 | SQLite/NetBeans Swing payroll manager | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md`; `../submission-test-results.md` | `full_repo/testingester-main/1/-/` |
| 2 | Swing/MySQL payroll system, OOP-labelled README | Existing corpus | Same folder-2/5 implementation section in `../submission-feature-pseudocode.md` | `full_repo/testingester-main/2/-/` |
| 3 | COMPROG1 console exercises | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md` | `full_repo/testingester-main/3/-/` |
| 4 | Layered Swing/CSV payroll app | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md` | `full_repo/testingester-main/4/-/` |
| 5 | Same Swing/MySQL app under CP2 label | Existing corpus | Same folder-2/5 implementation section in `../submission-feature-pseudocode.md` | `full_repo/testingester-main/5/-/` |
| 6 | MO-IT110 MS2 payroll OOP refactor | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md`; `../candidate-policy-review.md` | `full_repo/testingester-main/6/-/` |
| 7 | Swing/CSV payroll app, unclear provenance | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md` | `full_repo/testingester-main/7/-/` |
| 8 | MO-IT103 Group 18 console + Swing system | Existing corpus | `../submission-folder-analysis.md`; `../submission-feature-pseudocode.md` | `full_repo/testingester-main/8/-/` |
| 9 | MO-IT110 Team 3 CP2-to-OOP project; user supplied feature-inventory example | Existing corpus + attachment | User example: `/home/user/uploads/MotorPH_OOP_Feature_Inventory_and_Pseudocode.md`; original source notes: `../submission-folder-analysis.md`, `../submission-feature-pseudocode.md` | `full_repo/testingester-main/9/-/` |
| 10 | Employee Payroll with Laravel Framework | Newly added | [Folder 10 full inventory + pseudocode](folder-10-laravel-payroll.md) | `full_repo/testingester-main/10/-/Employee-Payroll-with-laravel-framework/` |
| 11 | Foodya Restaurant POS | Newly added | [Folder 11 full inventory + pseudocode](folder-11-foodya-pos.md) | `full_repo/testingester-main/11/-/Foodya-Restaurant/` |
| 12 | `manuog1994/payroll-manager` Firebase PDF portal | Newly added | [Folder 12 full inventory + pseudocode](folder-12-payroll-manager-firebase.md) | `full_repo/testingester-main/12/-/payroll-manager/` |
| 13 | Payroll Engine framework | Newly added | [Folder 13 full inventory + pseudocode](folder-13-payroll-engine.md) | `full_repo/testingester-main/13/-/PayrollEngine/` |
| 14 | Frappe HRMS / Frappe HR | Newly added | [Folder 14 full inventory + pseudocode](folder-14-frappe-hrms.md) | `full_repo/testingester-main/14/-/hrms/` |

## Requested-repository snapshot details

| Folder | Upstream repository | Default branch snapshot | Stars / forks on 2026-11-04 | License in upstream snapshot |
|---:|---|---|---:|---|
| 10 | `wasilolly/Employee-Payroll-with-laravel-framework` | `master` · `b9d3d39882da4ad197a54afb30d9e9119be2ddbf` | 41 / 33 | No GitHub license detected |
| 11 | `Shahzaib-Awann/Foodya-Restaurant` | `main` · `698ac13942aad709aa31659d0f61107dff6204ef` | 12 / 3 | No GitHub license detected |
| 12 | `manuog1994/payroll-manager` | `main` · `d3f2b180390262794e301ef5be3dfd8ecf548a82` | 4 / 1 | No GitHub license detected |
| 13 | `Payroll-Engine/PayrollEngine` | `main` · `2ba455f0c9f4c08da3c6a65ed3f06b573b75c87a` | 136 / 34 | MIT |
| 14 | `frappe/hrms` | `develop` · `159548fa676d51e0bc7ce85c6b6516d3d7b7316a` | 9,017 / 2,866 | GPL-3.0 |

Each added folder has `UPSTREAM-SNAPSHOT.md`; source archives were extracted without `.git` history or dependencies. Upstream URLs and license files remain in the source snapshot where present.

## Coverage and quality audit

For folders 10–14, the catalog has a separate orchestration pseudocode for every major feature and a detailed pseudocode block for every listed minor feature:

| Folder | Major features | Minor features | Major + minor pseudocode blocks |
|---:|---:|---:|---:|
| 10 | 7 | 20 | 27 |
| 11 | 10 | 29 | 39 |
| 12 | 5 | 15 | 20 |
| 13 | 9 | 26 | 35 |
| 14 | 14 | 51 | 65 |
| **Total** | **45** | **141** | **186** |

The count was mechanically checked against the Markdown headings and fenced pseudocode blocks; fences are balanced in all five files. A simple source scan found no private-key block or common cloud/token signatures in folders 10–14. That scan is not a full security review. The original folders 1–9 remain documented in their existing reports; the user-supplied folder-9 example is explicitly indexed above.

## Skills compilation quality check used

This inventory applies the relevant working guidance: source before claims; distinguish advertised, source-visible, and verified behavior; preserve uncertainty; provide concrete sub-functions; avoid inventing business rules; and make the verification boundary explicit. The Skills Index lookups covered source-grounded technical documentation and verification/review. This is a static research/documentation task: no project was built, no dependencies installed, and no runtime/test success is claimed.
