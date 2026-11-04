# Folder 13 — Payroll Engine (regulation-driven payroll framework)

**Upstream:** [Payroll-Engine/PayrollEngine](https://github.com/Payroll-Engine/PayrollEngine)  
**Snapshot:** `main`, `2ba455f0c9f4c08da3c6a65ed3f06b573b75c87a`; GitHub metadata checked 2026-11-04 (136 stars, 34 forks; repository license MIT).  
**Local source:** `repo_review/full_repo/testingester-main/13/-/PayrollEngine/`  
**Evidence boundary:** top-level repository README, example/test folder layout, schemas, release notes, and docs were reviewed. The main repository is a distribution/example/test/configuration hub; backend, WebApp, Payroll Console, SDKs, and related services are separate repositories. Pseudocode describes the platform model advertised and exercised by examples/tests, not every internal implementation detail of those other repositories.

## Major-feature map

| Major | Feature | Minor features and sub-functions |
|---|---|---|
| 1 | Tenant/provider integration and deployment | Docker stack; backend and web app services; database initialization/configuration; REST/OpenAPI surface; API key/OAuth modes; .NET Client SDK; provider tenancy. |
| 2 | Regulation authoring, layering and sharing | Base/country/industry/company layers; versioned regulations; inherited and overridden objects; shared regulations; cases, wage types, collectors, lookups, reports; actions and sandboxed C# scripts. |
| 3 | Time-aware payroll input (“cases”) | Case/case-field schemas; dated event values; validity intervals; employee/company/national/global scopes; input validation and action-based field behavior; change history. |
| 4 | Calendars, payroll periods and payrun orchestration | Calendar type/period generation; divisions; payrun jobs; employee scopes; process/forecast/preview modes; multiple runs; asynchronous job queue in newer releases. |
| 5 | Calculation engine, wage types and collectors | Select effective case inputs; execute regulation scripts; compute wage types/collectors; culture/time-aware values; trace calculation; produce employee/payrun outputs. |
| 6 | Retroactive changes, cancellation and forecast | Recalculate past valid periods after backdated changes; correction/cancel/undo; forecast future values; preserve prior run history. |
| 7 | Reports, consolidated results and exports | Payslip/report definitions; query completed/forecast results; consolidate across periods/divisions; export result data; report templates/document integration. |
| 8 | Automated regulation testing | Cases as input; payrun processing; result/report expectations; command-line test suite; run in CI; edge tests for calendars, versions, retro pay, time values and divisions. |
| 9 | Exchange/import/export and automation tooling | JSON/YAML exchange schemas; import regulations/cases/examples; console commands; REST endpoints; client SDK services; automation via Docker/CLI. |

## Feature pseudocode


## High-level pseudocode — each major feature

### Major 1 — Deployment and access

```text
PROCEDURE MAJOR_1_DEPLOYMENT_AND_ACCESS
    load deployment configuration and start backend dependencies
    authenticate caller and resolve tenant context
    expose REST/WebApp/SDK interface
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_1.1  // Start configured deployment
        CALL MINOR_1.2  // Authenticate an API request
        CALL MINOR_1.3  // Use .NET client
END PROCEDURE
```

### Major 2 — Regulation layers

```text
PROCEDURE MAJOR_2_REGULATION_LAYERS
    select accessible regulation versions and ordered layers
    merge and validate component definitions
    publish/share valid configuration
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_2.1  // Compose effective regulation
        CALL MINOR_2.2  // Maintain regulation components
        CALL MINOR_2.3  // Execute regulator actions/scripts
END PROCEDURE
```

### Major 3 — Time-aware case data

```text
PROCEDURE MAJOR_3_TIME_AWARE_CASE_DATA
    validate case input and validity interval
    append time-stamped values without erasing history
    resolve values for the requested temporal perspective
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_3.1  // Validate and record case input
        CALL MINOR_3.2  // Resolve value for a payroll date
        CALL MINOR_3.3  // Correct or cancel case input
END PROCEDURE
```

### Major 4 — Calendar and payrun lifecycle

```text
PROCEDURE MAJOR_4_CALENDAR_AND_PAYRUN_LIFECYCLE
    resolve calendar period
    create payrun job and employee scopes
    preview or process employees
    persist status and progress
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_4.1  // Resolve payroll period
        CALL MINOR_4.2  // Create and process payrun job
        CALL MINOR_4.3  // Preview without side effects
        CALL MINOR_4.4  // Queue and worker (newer release path)
END PROCEDURE
```

### Major 5 — Wage calculations/results

```text
PROCEDURE MAJOR_5_WAGE_CALCULATIONS_RESULTS
    load effective case data and regulation
    calculate wage types and collectors
    persist employee/payrun results and trace
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_5.1  // Calculate wage types
        CALL MINOR_5.2  // Calculate collectors
        CALL MINOR_5.3  // Build employee/payrun result
END PROCEDURE
```

### Major 6 — Retro, cancellation, forecasts

```text
PROCEDURE MAJOR_6_RETRO_CANCELLATION_FORECASTS
    identify impacted historical or future periods
    recalculate retro or forecast outputs
    record correction/forecast state without overwriting history
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_6.1  // Retrospective recalculation
        CALL MINOR_6.2  // Forecast
        CALL MINOR_6.3  // Cancel/undo a run
END PROCEDURE
```

### Major 7 — Reports and exports

```text
PROCEDURE MAJOR_7_REPORTS_AND_EXPORTS
    authorize report and select result filters
    query/consolidate results by exact status and period
    render/export report
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_7.1  // Build regulation-defined report
        CALL MINOR_7.2  // Consolidate results
        CALL MINOR_7.3  // Export data/document
END PROCEDURE
```

### Major 8 — Test-driven regulation development

```text
PROCEDURE MAJOR_8_TEST_DRIVEN_REGULATION_DEVELOPMENT
    load test regulation, cases and expected outputs
    execute payrun/preview/report test
    compare results and report failures
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_8.1  // Define test fixture
        CALL MINOR_8.2  // Execute test suite
END PROCEDURE
```

### Major 9 — Exchange and automation

```text
PROCEDURE MAJOR_9_EXCHANGE_AND_AUTOMATION
    validate JSON/YAML exchange data
    import/export in dependency order or execute console/API operation
    return structured results and diagnostics
    dispatch each requested minor operation to the matching procedure:
        CALL MINOR_9.1  // Import/export exchange files
        CALL MINOR_9.2  // Payroll Console/API automation
END PROCEDURE
```

### Major 1 — Deployment and access

**Minor 1.1 — Start configured deployment** — sub-functions: select container stack; configure DB password/provider/ports; initialize database; start backend and WebApp; access API and UI.

```text
read deployment configuration and secrets from environment
start configured database
start Payroll Engine backend connected to database
apply initial database setup when first run
start WebApp and optional Console/MCP services
expose configured UI/API ports
```

**Minor 1.2 — Authenticate an API request** — sub-functions: validate configured auth mode; API key or OAuth token; apply tenant context; authorize resource access.

```text
read authentication mode
IF mode = API key THEN validate request key
ELSE IF mode = OAuth THEN validate token authority, audience, expiry and claims
ELSE apply explicitly configured no-auth mode only where allowed
resolve tenant/user context
reject request when credentials/context do not authorize it
```

**Minor 1.3 — Use .NET client** — sub-functions: configure HTTP base address/credentials; call client service; query or mutate engine resources; map API results.

```text
construct HTTP client with backend URL and auth
construct typed Payroll Engine service
call service method with typed request
return typed result or propagate API error
```

### Major 2 — Regulation layers

**Minor 2.1 — Compose effective regulation** — sub-functions: resolve base plus country, industry and company layers; select versions valid at evaluation time; override lower-layer definitions with higher-layer definitions; share accessible regulations between tenants.

```text
layers ← regulations assigned to payroll, ordered base → country → industry → company
FOR each layer
    choose version effective for requested evaluation date
    merge cases, fields, wage types, collectors, lookups, actions and reports
    when same logical object is overridden, retain highest applicable layer's version
validate references and access rights
return effective regulation graph
```

**Minor 2.2 — Maintain regulation components** — sub-functions: define case input structures; wage formulas; collectors; lookups/tables; reports; actions; version and share them.

```text
validate component schema and dependencies
save component under selected regulation/version
validate regulation graph for unresolved links or invalid scripts
publish/share a valid regulation version to authorized consumer tenant
```

**Minor 2.3 — Execute regulator actions/scripts** — sub-functions: control case-field visibility/requiredness/options; execute permitted no-code action or low-code script; use sandboxed runtime; write only scoped values.

```text
create isolated script context with regulation namespace and case context
expose approved client scripting actions and inputs
execute action/script within sandbox limits
collect validation messages and permitted target updates
reject invalid/unauthorized script effects
```

### Major 3 — Time-aware case data

**Minor 3.1 — Validate and record case input** — sub-functions: resolve employee/company/national/global case; validate fields/action rules; attach effective start/end; timestamp change; preserve history.

```text
resolve effective regulation and case definition
validate submitted fields and cross-field actions
validate valid-from/valid-to interval
append a timestamped case value with its validity interval
retain earlier values/history rather than overwrite it invisibly
```

**Minor 3.2 — Resolve value for a payroll date** — sub-functions: select value valid at requested date; support historical, current-knowledge and forecast views; return source and interval context.

```text
find case-value versions for the requested scope/field
filter by validity interval appropriate to requested temporal perspective
choose applicable value according to engine time semantics
return value plus source/time metadata
```

**Minor 3.3 — Correct or cancel case input** — sub-functions: append corrected event; cancel prior event; preserve audit/history; identify impacted payroll dates.

```text
record correction/cancellation as a new time-aware change
preserve original record and audit history
compute affected evaluation windows
queue or mark affected past payruns for recalculation
```

### Major 4 — Calendar and payrun lifecycle

**Minor 4.1 — Resolve payroll period** — sub-functions: generate daily/weekly/biweekly/semi-monthly/monthly/quarterly/etc. boundaries based on configured calendar; account for date progression; select date window.

```text
validate calendar type and anchor date
calculate next period start/end using calendar-specific step
ensure next start strictly advances
return period boundaries for requested index/date
```

**Minor 4.2 — Create and process payrun job** — sub-functions: select payroll/division/period/employees; select normal/forecast mode; create job; process employees; capture job status/progress/results.

```text
validate payrun request, division, calendar period and regulation access
create persisted job with requested run mode and employee scope
for each selected employee:
    create isolated employee calculation context
    execute payroll using effective cases and regulation
    persist wage-type/collector results and processing status
mark job complete or aborted with diagnostics
```

**Minor 4.3 — Preview without side effects** — sub-functions: calculate sample/selected employee; return draft result; avoid finalizing/persisting a normal payrun outcome.

```text
load employee inputs and effective regulation into temporary context
calculate preview result
return preview values and warnings
leave permanent payroll job/result state unchanged
```

**Minor 4.4 — Queue and worker (newer release path)** — sub-functions: persist job before enqueue; bounded queue/backpressure; background processing; progress persistence; completion/abort webhook.

```text
validate request
persist job record before enqueue
IF bounded queue full THEN return overload response without losing saved job context
worker dequeues job and processes employees
persist progress in batches
on completion/abort persist terminal state and notify configured webhook
```

### Major 5 — Wage calculations/results

**Minor 5.1 — Calculate wage types** — sub-functions: evaluate regulation formulas for each employee and applicable period; read case values and lookups; respect culture/date/time context; record trace.

```text
for each wage type in dependency order
    resolve required case fields and lookup values for evaluation context
    execute formula/action in isolated context
    validate result type and date scope
    save result and optional wage trace
```

**Minor 5.2 — Calculate collectors** — sub-functions: aggregate wage-type results by configured collector, period, and scope; support custom collector result; make values addressable to later calculations/reports.

```text
for each collector in regulation order
    select contributing wage results in its configured scope/window
    aggregate using configured collector behavior
    store collector result with result date
```

**Minor 5.3 — Build employee/payrun result** — sub-functions: combine wages and collectors; determine totals using the regulation's definitions; produce payslip/result records; preserve culture/currency formatting metadata.

```text
for employee and run period
    execute wage types and collectors
    build result object from configured output mappings
    persist result under employee/payrun job/period context
```

### Major 6 — Retro, cancellation, forecasts

**Minor 6.1 — Retrospective recalculation** — sub-functions: detect backdated value changes; identify affected completed periods; recalculate affected payroll using effective historical inputs; produce delta/correction outputs.

```text
affectedPeriods ← completed periods overlapping changed validity window
for each period:
    recompute with revised effective case data and same regulation/version policy
    compare recomputed outputs with stored result
    record retro difference and correction result
```

**Minor 6.2 — Forecast** — sub-functions: evaluate future-valid case data at forecast date; label forecast output distinctly from completed/payable payroll.

```text
select future evaluation date and applicable forecast case values
run calculation in forecast mode
persist/return forecast result with forecast status, not completed status
```

**Minor 6.3 — Cancel/undo a run** — sub-functions: select eligible run; cancel or reverse job/results using supported lifecycle; retain audit trail.

```text
load target payrun and verify cancellable state
create cancellation/reversal operation
mark superseded output as no longer current, retaining history
recalculate dependent consolidated outputs as required
```

### Major 7 — Reports and exports

**Minor 7.1 — Build regulation-defined report** — sub-functions: choose report; select employee/payrun/time filters; query wage/collector/result values; render report.

```text
validate report authorization and parameters
resolve report definition in effective regulation
query scoped results and source values
format and return report rows/document
```

**Minor 7.2 — Consolidate results** — sub-functions: aggregate per period/division/payroll; filter run status exactly; step periods correctly; stop on invalid/nonadvancing calendar.

```text
for each requested payroll period and division
    query results matching exact requested job status
    append normalized rows to consolidated result
    advance calendar to next period
    if period did not advance THEN stop with warning
return consolidated results
```

**Minor 7.3 — Export data/document** — sub-functions: exchange JSON/YAML; report data export; PDF/Excel document generation in companion document component; honor record/download limits.

```text
validate output type and authorization
serialize exchange-compatible data or render configured report document
apply metadata, typing, filters and size limits
return downloadable output
```

### Major 8 — Test-driven regulation development

**Minor 8.1 — Define test fixture** — sub-functions: regulation, case inputs, run parameters, expected wage/collector/report outputs.

```text
load fixture regulation and case input
load expected output and evaluation window
validate fixture schema before processing
```

**Minor 8.2 — Execute test suite** — sub-functions: import fixture; run payroll; compare actual to expected; collect failures; continue/stop according to runner setting; use CLI or CI.

```text
FOR each test fixture
    provision/import fixture data
    execute requested payrun or preview
    compare actual outputs to expected values and reports
    record pass/failure with difference details
    continue or fail-fast according to command option
return aggregate test status and diagnostics
```

### Major 9 — Exchange and automation

**Minor 9.1 — Import/export exchange files** — sub-functions: validate JSON/YAML schema; import regulation/cases/jobs/reports; resolve dependencies; dry run where supported; export normalized content.

```text
read selected exchange file(s)
validate against published schema
resolve ordered dependencies and target tenant context
IF dry-run THEN report planned changes only
ELSE import in dependency order and report per-object outcomes
```

**Minor 9.2 — Payroll Console/API automation** — sub-functions: execute import/test/payrun/report commands; match file masks/recursive options; consume REST/SDK; return machine-readable result/exit status.

```text
parse command and options
validate API connection/authentication and input paths
execute corresponding API operation
write structured result/log and exit status
```

## Source caveats / confidence

- This is a configurable payroll application **framework**, not an out-of-the-box Philippine MotorPH policy. Regulation authors must supply/approve country/company rules and test values.
- Main repository contains examples, tests, schemas, setup and docs; major runtime components live in separate repositories listed in its README. This inventory does not assert all those sibling source repositories were added to local folder submissions.
- Current release notes document a PostgreSQL provider, exact job-status filtering, period-step fixes, OAuth, signed responses in selected integrations, scripting and worker enhancements; these are version-specific additions, not timeless assumptions.
- Do not treat a green example suite as statutory validation: correctness depends on the regulation input and expected outputs chosen by the implementer.
- README license is MIT for this repository. Check licenses of each separate component/dependency before reusing code; the general license table does not imply all third-party payroll data or regulatory content is license-free.
- No Docker stack, service, build, or tests were run for this static inventory.
