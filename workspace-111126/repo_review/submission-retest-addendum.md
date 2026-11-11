# Retest addendum — supported inputs and prepared runtime

This addendum updates the first-pass findings in [`submission-test-results.md`](submission-test-results.md). It addresses the user's request to retest unvalidated cases using each app's expected interface, and to make temporary adaptations where needed.

## Safety and test setup

- Fresh copies of folders 1–9 are under `/home/user/repo_review/test_runs/compat_retest/`. The source export at `/home/user/repo_review/full_repo/testingester-main/` and the uploaded inventory file were not modified.
- A Java 21 JDK, Maven, and Xvfb were provisioned for the retest. A disposable MariaDB 11.8.6 server and synthetic schema/data were used for folders 2 and 5; that server has been stopped. This is a **MySQL-protocol compatibility test**, not proof against a native MySQL server/version.
- Temporary source/config edits were confined to the fresh test copies and were limited to connection paths/settings or test-harness compatibility. Test fixture rows contain synthetic QA values. Product behavior findings below distinguish these setup changes from application-source defects.
- The inventory CSV remains a deliberate wrong-schema probe. For functional checks, I used bundled fixtures or synthetic data matching each program's interface.

## Retest results

| Folder | Supported-interface retest | Outcome |
|---|---|---|
| **1** | Compiled all 12 Java sources with Java 21 and the matching NetBeans `AbsoluteLayout` dependency. In the isolated copy, redirected the SQLite URL to its copied database and added one synthetic account; ran under Xvfb. | Login reached `MainMenu` with no uncaught exception when `src` was included on the classpath for assets. SQLite login/audit path was exercised. The project still is not self-contained with respect to its layout dependency and relies on a hard-coded Windows path unless configured. |
| **2** | `mvn test` completed successfully under Java 21; there are no Surefire test reports. Repointed only the copied DB helper to an isolated MariaDB schema and seeded synthetic rows. Automated a valid Admin login and clicked the six dashboard areas. | Login reached `AdminHomeDashboard`; **Employees Data**, **Salary**, **Attendance**, **Leave Request**, **Overtime Request**, and **User Accounts** screens opened. The five data-list screens showed the synthetic row; attendance opened its two calendar tables. This establishes connection/navigation/basic query behavior only—not create/update/delete or formula correctness. |
| **3** | Replaced only the copied `employee_data.csv` with one 19-column synthetic row matching the reader's field positions and without a comma inside the address. | Reader exited 0 and returned the expected gross semi-monthly field value (**17,500.0**) for that row. This demonstrates the intended simple schema works. The previous bundled-file discrepancy remains: quoted commas in all 34 bundled addresses shift the naive `split(",")` columns, so 34/34 gross values still disagree with the correctly parsed salary field. |
| **4** | Under Java 21/Maven, adapted two **test-only** source files in the copy: typed empty attendance lists as `List<AttendanceRecord>` and used reflection in a test helper to call a private CSV-save method. Copied bundled images to the lowercase classpath location the app requests. Used a one-user synthetic credentials fixture and the bundled employee/attendance data. | Main and tests compile; `mvn test` exits 0 but reports **0 automated tests**. Six manual CSV/service test mains passed, including a CSV write using the app's `done` terminator. GUI login reached `MainFrame`. The original source's mixed-case resource directory (`Resources/Images`) does not match its lowercase `/images/...` lookup on this case-sensitive runtime; only the test classpath was adapted. The inventory file still produces empty incompatible datasets. |
| **5** | Same approach as folder 2, in its own copied project and same disposable database. | Maven build/test exits 0 with no test suite. Synthetic login reached `AdminHomeDashboard`; the same six navigation screens opened with matching seeded data. The near-duplicate implementation should not be counted as independent evidence. |
| **6** | Recompiled all production and test sources with Java 21, reran `motorph.AllTests`, and started the GUI under Xvfb. | **167 passed, 0 failed**; GUI `LoginFrame` opened. The wrong-schema inventory outcome remains 0 employees/0 attendance without a helpful error. |
| **7** | Compiled all 35 Java sources with Java 21. First supplied a compatible 19-column employee file from folder 6 and verified mapping; then used a synthetic header-plus-data-row fixture with the expected password field to exercise GUI login. | The 19-column file mapped **34/34 rows** after the loader padded rows to 21 fields. The expected-format synthetic login reached `DashboardUI`. The earlier inventory result remains: padding accepts the five-field rows, but employee mapping later throws `NumberFormatException`. |
| **8** | Compiled all 36 Java sources with Java 21. Used the bundled employee-details CSV and derived a valid employee-number/last-name login pair internally (no values printed). | GUI login reached `MainMenu`; bundled loader found 34 employees. The original inventory substitution still leaves no matching employee and the production loader's unguarded missing-employee dereference still throws `NullPointerException`. |
| **9** | Compiled all 28 production/OOP sources and the two service test classes with Java 21. Reran suites with the JVM timezone set to `Asia/Shanghai`; launched the main table screen with bundled data. | Payroll/backend **91/91**, attendance **33/33**, leave **44/44**; `MainMenu` opened with **34 table rows**. The single UTC-vs-Manila date-sensitive check still requires timezone alignment. Inventory remains explicitly rejected by the attendance repository as an invalid employee-number row. |

## What changed—and what did not

**Temporary test-copy adaptations:**

- Folder 1: SQLite URL redirected to the copied database; a synthetic login row was added to the copied database.
- Folders 2 and 5: local database URL/user settings changed in the copied `Classes/db.java`; synthetic schema/data was created in the disposable MariaDB database. The dashboard needed a `profilePicturePath` column, which was added to that test schema after the first login exposed the missing column.
- Folder 4: only two test-helper Java files were adjusted to compile against the production API; image resources were placed in the requested classpath location; the copied credentials CSV was replaced with synthetic QA credentials.
- Folders 3 and 7: only copied CSV fixtures were adapted to match their stated layouts. No payroll calculation/parser source was fixed.
- Other folders: no application source edits were needed for these retests.

**Not changed:** original repository files, original fixtures, and the uploaded inventory CSV. The test adaptations are not proposed product fixes and should not be copied into any candidate submission without separate review.

## Updated interpretation

The earlier setup blockers for Java version, Maven, and headless Swing were removable: supported JDK 21/Maven builds and GUI smoke flows now work for the previously blocked projects. Folder 2 and 5 additionally require a database; their navigation works against the disposable MariaDB schema, but native MySQL-specific differences and actual CRUD/payroll writes remain unverified. Folder 4's manual tests and GUI can run once its test harness/resource layout is accommodated, but Maven itself still runs **zero automated assertions**.

The inventory outcomes remain useful robustness evidence, not valid payroll baselines. On correct-format data, folders 1, 2, 4, 5, 7, 8, and 9 now reach representative application screens; folder 6's suite remains clean; folder 3 works on a simple compatible row but misreads its own bundled CSV because of quoted commas. No failure was hidden by changing production code.

## Remaining focused checks

1. For folders 2 and 5, repeat against the intended MySQL version and a schema confirmed by the mentor; exercise salary creation/update and leave/overtime writes, then verify persistence and rollback/error cases.
2. For folder 4, add real automated assertions only if authorized; validate payroll against the approved rubric. Fixing its resource-path case mismatch would require a separate code-change decision.
3. For folders 1, 6–9, use controlled GUI actions to verify representative writes/exports, not just login/navigation. Keep them on isolated copies.
4. Preserve the folder 3 quoted-comma finding and test boundary rows (empty/zero/negative rates and truncated records) separately; do not “fix” the parser in the analysis copy before measuring the original behavior.
5. Before adopting code from any numbered repository, confirm authorship, assignment fit, and reuse permission with the mentor.
