# Static analysis and linting

Brief §5 asks for ESLint on the JavaScript, Checkstyle + SpotBugs + PMD on the Java, and Lighthouse
for accessibility/performance/best-practices/SEO. It also says not to add tools just to inflate the
list, to make the compatible ones runnable through the normal scripts, and to record anything that
could not be configured.

Run them with `sh tools/lint.sh`. They are deliberately **not** part of `./test.sh`: two of them want a
download, one wants a running server, and a failing Lighthouse score should never make the unit tests
look broken.

## What runs

| Tool | Scope | Result |
|---|---|---|
| ESLint 9 | `web/**/*.js`, `test/web/**/*.js` | clean, 0 findings |
| Checkstyle 10.12.4 | `src/main/java` | clean, 0 findings |
| PMD 7.0.0 (`errorprone`, `bestpractices`) | `src/main/java` | clean on correctness rules |
| SpotBugs 4.8.3 (`-effort:max -low`) | compiled `target/classes` | clean on correctness detectors |
| Lighthouse | landing page, live server | accessibility **100**, best-practices **100** |

All four run on the project's Java 11 toolchain.

## What each config deliberately does not enforce

A stock Checkstyle config produced 1,277 errors on this codebase. Almost all of them were rules that
contradict instructions the project was built to:

| Stock rule | Findings | Why it is off |
|---|---|---|
| `NeedBraces` | 690 | single-statement `if`/`for` on one line is the house style |
| `LeftCurly` | 300 | one-line methods put `{` and the body on the same line |
| `OneStatementPerLine` | 114 | same |
| `MultipleVariableDeclarations` | 89 | §4 *requires* grouping same-type declarations on one line |
| `ParameterName` | 38 | `ab_name` / `ab_dtName` members are the whole point of §4 |
| `VariableDeclarationUsageDistance` | 15 | declarations are grouped at the top of a method by type |

What is left is the set that catches actual mistakes: star imports, unused imports, empty blocks,
`String` compared with `==`, `equals` without `hashCode`, missing switch default, fall-through,
assignments inside conditions, and a 200-character line cap. `ConstantName` allows both `ALL_CAPS` and
the §4 `ab_name` form, because `PasswordPolicy.pw_sInitial` is final *and* correctly named.

## What the tools actually found

Turning them on was not a formality — they caught real defects:

- **18 unused imports** across 14 files, removed.
- **24 case-conversion sites** without a `Locale`. Under a Turkish locale `toLowerCase()` maps `I` to
  `ı`, which would have broken role matching and the `/api/...` path router. All now use
  `Locale.ROOT`.
- **A 32-bit hash bug** in `InMemoryEmployeeScheduleStore.stableId`: `Math.abs(Integer.MIN_VALUE)` is
  still negative. Replaced with `Math.floorMod`.
- **Three dead private methods** and **one dead local** (`basic` in `CutoffPayrollService.cutoff`,
  `session` in `AppServer.audit`).
- **Two theoretical NPEs** guarded (`contentTypeFor`, `LocalStateFile.save`/`listBackups`).
- **A dead sign-in splash**: `<div id="signin-splash">` was permanently `hidden` with no code left to
  unhide it, plus five orphaned `@keyframes`. Removed.
- **Inline `style=""` attributes** in `Landing.html`/`Index.html` were being blocked by the app's own
  CSP (`style-src 'self'`), so the payslip bars rendered at zero width. Moved to classes in
  `main.css`.
- **Contrast failures** on the landing art card: `text-zinc-400` on white measured 2.62:1, below the
  4.5:1 minimum. Moved to `text-zinc-600`.

## SpotBugs false positives, and why they are filtered

`tools/lint/spotbugs-exclude.xml` documents each exclusion inline. In short:

- **`URF_UNREAD_FIELD`** — every store class is written and read by Gson through reflection. SpotBugs
  only sees the Java reads, so all persisted fields look dead. This is the same fact that made the
  `@SerializedName` work necessary.
- **`SIC_INNER_SHOULD_BE_STATIC_ANON`** — the lambdas handed to `HttpServer` contexts are single-use
  by design.
- **`EI_EXPOSE_REP` / `MS_EXPOSE_REP`** — services are handed the store they own for the process
  lifetime. There is one roster per server.
- **`VA_FORMAT_STRING_USES_NEWLINE`** — `PayslipPdf` writes a binary PDF xref table, where `%n` would
  emit `\r\n` on Windows and corrupt the file. `\n` is required.

## Lighthouse

Both audits ran against the live server via `tools/lint.sh`. Landing page: **accessibility 100,
best-practices 100, nothing scoring below 1.0**. Getting there required fixing the CSP-blocked inline
styles and the contrast failures listed above — the first run was 94 / 93.

## Not run

- **Dependency audit** (`npm audit`, OWASP dependency-check) — mentioned in §5 but not wired in; the
  two Java dependencies are pinned and hand-checked, and `npm audit` needs the registry.
- **HTML/CSS validators** — the markup is generated and validated by the browser during the Lighthouse
  run; a separate `htmlhint` pass would duplicate that.
