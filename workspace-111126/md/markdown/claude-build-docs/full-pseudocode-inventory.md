# MotorPH Payroll Office — full pseudocode inventory

> **Looking for the short version?** Read [`START-HERE.md`](START-HERE.md) — 10 screens, one worked
> payslip, no function-by-function detail. Come back to this file when you need to look up a specific
> function.

Every feature, every function, every subfunction, written as pseudocode you can read without
opening the source. **Generated from the current code on 2026-11-05**, not from an earlier design.

- **36 Java classes** in `src/main/java/ph/motorph/web/`: **187 methods with a body**, plus 11
  value-object/interface classes whose members are one-line accessors (listed in Appendix D.2)
- **18 browser modules, 106 named functions** in `web/` (66 of them in `app.js`)
- **10 Java test classes + 2 browser suites** proving the behaviour described below

If a behaviour is not written here, it is not in the product. Section 12 lists what is deliberately
absent, so you can tell "not built" apart from "built but you didn't find it".

How to read this document:

| Marker | Meaning |
|---|---|
| `→` | returns |
| `fail(status, msg)` | throws an error the HTTP layer turns into that status code |
| **REAL** | reads or writes durable Firestore data under the signed-in user |
| **FIXTURE** | process-memory synthetic data, only in isolated test mode |
| **PURE** | no I/O, no side effects, unit-testable |

---

## Table of contents

1. [The system in one page](#1-the-system-in-one-page)
2. [Startup, configuration and the three run modes](#2-startup-configuration-and-the-three-run-modes)
3. [The HTTP layer — every route, in order](#3-the-http-layer)
4. [Storage: interfaces and their two implementations](#4-storage)
5. [Money, time and rules primitives](#5-primitives)
6. [Employees](#6-employees)
7. [Attendance and timesheets](#7-attendance-and-timesheets)
8. [Requests and tickets (workflow)](#8-workflow-leave-requests-and-tickets)
9. [Payroll — the core](#9-payroll--the-core)
10. [Demo seeding](#10-demo-seeding)
11. [The browser app](#11-the-browser)
12. [What is deliberately missing](#12-what-is-deliberately-missing)
13. [Feature-by-feature: what you can actually do](#13-feature-matrix--what-you-can-actually-do)
14. [Where each function is tested](#14-test-map--where-each-claim-is-proven)

---

## 1. The system in one page

```
BROWSER (web/)                         JAVA API (src/main/java)                 FIRESTORE
──────────────────────────────         ──────────────────────────────           ─────────────
index.html  one page, 8 views          AppServer  HTTP routing + auth gate
app.js      66 functions                │
  ├─ auth screens (Firebase JS SDK)     ├─ EmployeeService   ──┐
  ├─ employees  ──────────────────────► GET/POST/PUT /api/employees        ├─► users/{uid}/demoEmployees
  ├─ attendance ──────────────────────► GET /api/attendance                 ├─► users/{uid}/demoAttendance
  │                                     GET /api/reports/attendance         ├─► users/{uid}/demoEmployeeSchedules
  ├─ requests  ───────────────────────► /api/workflows/...                  ├─► users/{uid}/demoLeaveRequests
  │                                                                          └─► users/{uid}/demoTickets
  ├─ payroll  ────────────────────────► GET  /api/payroll/rules             ├─► users/{uid}/demoPayrollRuns
  │                                     GET  /api/payroll/timesheet
  │                                     POST /api/payroll/preview
  │                                     POST /api/payroll/runs        ← written, immutable
  │                                     GET  /api/payroll/runs[/{id}]
  └─ 15 pure helper modules              └─ PayrollService + PayrollRuleset  (the maths)
```

Three sentences that explain the whole architecture:

1. **The browser never calculates money.** Every figure on screen came from the Java service. The
   `web/payroll.js` helpers only format strings and lay out tables.
2. **The browser never holds a Firestore credential.** It signs in with Firebase Auth, sends its ID
   token on every `fetch`, and the Java process forwards that token to Firestore REST. Firestore
   Security Rules — not Java code — decide whether the call is allowed.
3. **Every record is confined to one user's sandbox.** Path segment `users/{uid}` comes from the
   verified token, never from a request body, so one signed-in account cannot read another's data.

---

## 2. Startup, configuration and the three run modes

### 2.1 `AppServer.main()` — the process entry point

```
FUNCTION main():
    projectId ← env FIREBASE_PROJECT_ID or ""
    testMode  ← env MOTORPH_TEST_MODE == "true"
    port      ← env PORT or 8080
    host      ← env HOST or "0.0.0.0"
    loopback  ← host is 127.0.0.1 or localhost or ::1

    IF projectId is empty AND (NOT testMode OR NOT loopback):
        THROW "FIREBASE_PROJECT_ID is required. Local-memory runtime is disabled…"
        # This is the fail-closed guard. A production start with no Firebase config dies here,
        # audibly, instead of quietly serving fake data. ./test.sh asserts this branch.

    app ← new AppServer(projectId, WEB_ROOT or "web", null)
    app.start(host, port)
    register shutdown hook → app.stop()          # clean exit on SIGTERM
```

### 2.2 `AppServer(firebaseProjectId, webRoot, firestoreApiRoot)` — constructor

```
CONSTRUCTOR:
    store the project id
    IF projectId is empty:                      # isolated fixture mode
        localEmployeeService   ← new EmployeeService(new InMemoryEmployeeStore())     # FIXTURE
        localScheduleStore     ← new InMemoryEmployeeScheduleStore()                  # FIXTURE
        localAttendanceStore   ← new InMemoryAttendanceStore()                        # FIXTURE
        localWorkflowStore     ← new InMemoryWorkflowStore()                          # FIXTURE
    ELSE                                        # real mode: no in-memory data at all
        all four are null; every request builds Firestore-backed services instead
    webRoot ← absolute, normalised        # static file root, traversal-guarded later
    firestoreApiRoot ← override for tests, else null (real googleapis host)
```

**Why it matters:** there is no code path where the production process serves fixture data. The
in-memory stores exist only when the project id is empty, which `main()` only allows on loopback in
explicit test mode.

### 2.3 `start(host, port)` — route table

```
FUNCTION start(host, port):
    server ← HttpServer.create(host, port)
    server.context("/api/health",            handleHealth)
    server.context("/api/employees",         handleEmployees)        # list, detail, create, update
    server.context("/api/attendance",        handleAttendance)       # read only
    server.context("/api/reports/attendance", handleAttendanceReport)
    server.context("/api/workflows",         handleWorkflows)        # leave + tickets
    server.context("/api/payroll",           handlePayroll)          # 6 routes
    server.context("/api/demo/seed",         handleSeedDemo)
    server.context("/",                      handleStaticFiles)      # last = catch-all
    server.start() → server
```

### 2.4 The three run modes, and what they can do

| Mode | How it starts | Data | Who uses it |
|---|---|---|---|
| **Production** | [run.sh](run.sh) / [run-api.sh](run-api.sh), `FIREBASE_PROJECT_ID` set | Firestore, per-user | The real app |
| **Isolated fixture** | `MOTORPH_TEST_MODE=true HOST=127.0.0.1 ./test.sh` or the e2e harness | Process memory, synthetic | Automated tests, local preview |
| **Forbidden** | `FIREBASE_PROJECT_ID` empty, not test mode, or bound to `0.0.0.0` | — | Refused at startup |

---

## 3. The HTTP layer

`AppServer` is one class with one handler per route family. Every handler follows the same shape:
**check method → validate input → call a service → serialise**. It never contains business rules.

### 3.1 `servicesFor(exchange)` — who am I serving?

```
FUNCTION servicesFor(exchange):
    IF projectId is empty:
        return { employeeService: localEmployeeService,
                 scheduleStore:   localScheduleStore,
                 attendanceStore: localAttendanceStore }          # FIXTURE
    client ← firestoreClientFor(exchange)                          # may fail 401
    return { employeeService: new EmployeeService(new FirestoreEmployeeStore(client)),
             scheduleStore:   new FirestoreEmployeeScheduleStore(client),
             attendanceStore: new FirestoreAttendanceStore(client) }   # REAL
```

### 3.2 `firestoreClientFor(exchange)` — the only place a token is read

```
FUNCTION firestoreClientFor(exchange):
    auth ← header Authorization
    uid  ← header X-Firebase-UID
    IF auth does not start with "Bearer " OR uid blank:
        fail(401, "Sign in with your Firebase account to continue.")
    token ← auth after "Bearer "
    IF token longer than 8192 chars OR uid longer than 128 OR uid contains "/":
        fail(401, "Invalid Firebase session information.")          # path-injection guard
    return new FirestoreRestClient(projectId, uid, token[, apiRoot])
```

The browser sets `X-Firebase-UID` next to the Bearer token; `uid` is validated as a single path
segment so it can never be used to climb to another user's documents.

### 3.3 Route-by-route

| Method + path | Handler | Success | Failure modes |
|---|---|---|---|
| `GET /api/health` | `handleHealth` | `{status:"ok", mode:"local-synthetic-demo"\|"firebase-user-sandbox"}` | — |
| `GET /api/employees?q=` | `handleEmployees` | `{items:[…], total:n, matched:m}` | 500 |
| `GET /api/employees/{id}` | `handleEmployees` | `{item:{…}}` | 404 unknown |
| `POST /api/employees` | `handleEmployees` | 201 `{item}` | 400 validation, 415 wrong content type, 413 body > 8 KB |
| `PUT /api/employees/{id}` | `handleEmployees` | 200 `{item}` | 400 invalid id / field, 404 unknown |
| `DELETE /api/employees/{id}` | `handleEmployees` | — | **405** (deletion deliberately not exposed) |
| `GET /api/attendance?employeeId=&from=&to=` | `handleAttendance` | `{items, count, hoursCalculated:false}` | 400 missing id / bad or reversed date, 404 unknown employee |
| `POST /api/attendance` | `handleAttendance` | — | **405** (no attendance capture exists) |
| `GET /api/reports/attendance?from=&to=` | `handleAttendanceReport` | `{items:[…], count, from, to, hoursCalculated:false}` | 400 reversed range |
| `GET /api/workflows` | `handleWorkflows` | `{leaveRequests:[…], tickets:[…]}` | 401 |
| `POST /api/workflows/leave` | `handleWorkflows` | 201 `{item, storage}` | 400 bad type/date/order/note |
| `POST /api/workflows/leave/{id}/cancel` | `handleWorkflows` | 200 `{item}` | 404 unknown, **409** already cancelled |
| `POST /api/workflows/tickets` | `handleWorkflows` | 201 `{item}` | 400 bad type/summary |
| `POST /api/workflows/tickets/{id}/cancel` | `handleWorkflows` | 200 `{item}` | 404, 409 already cancelled |
| `GET /api/payroll/rules?year=` | `handlePayroll` | ruleset JSON | 400 non-numeric or uncovered year |
| `GET /api/payroll/timesheet?employeeId=&from=&to=` | `handlePayroll` | `{daysWorked, totalHours, days:[…]}` | 400 missing id / half-specified period / reversed |
| `POST /api/payroll/preview` | `handlePayroll` | `{payslip, saved:false, ruleset}` | 400 any validation, 415, 413 |
| `POST /api/payroll/runs` | `handlePayroll` | **201** `{run, saved:true}` | 400, 409 duplicate id |
| `GET /api/payroll/runs` | `handlePayroll` | `{items:[summaries], count}` | 401 |
| `GET /api/payroll/runs/{id}` | `handlePayroll` | `{run incl. payslips}` | 404 unknown / nested path |
| `DELETE /api/payroll/runs/{id}` | `handlePayroll` | — | **405** (runs are immutable) |
| `POST /api/demo/seed` | `handleSeedDemo` | `{employeesCreated, scheduleMappingsCreated, attendanceCreated}` | 401, 405 non-POST |
| `GET /*` | `handleStaticFiles` | the SPA | 404 traversal, 403 outside root |

### 3.4 Shared request helpers

```
FUNCTION readJsonBody(exchange):
    IF Content-Type is not application/json: fail(415, "Content-Type must be application/json.")
    bytes ← first 8193 bytes of body
    IF bytes > 8192:                          fail(413, "Request body is too large.")
    parse; IF not an object:                  fail(400, "JSON body must be an object.")
    IF parse error:                           fail(400, "Request body is not valid JSON.")
    → JsonObject

FUNCTION requireOnlyKeys(body, allowed):
    FOR each key in body: IF key not in allowed: fail(400, "Unexpected request field: " + key)
    # Unknown fields are rejected, never ignored — a typo in a money field cannot pass silently.

FUNCTION numberOr(body, field, fallback):     # accepts JSON number OR decimal string
    missing/null → fallback
    → BigDecimal(text of value)               # one parsing rule for the whole API

FUNCTION sendApiError(exchange, FirestoreException):
    map Firestore status → HTTP (401 unauthenticated, 403 permission, 404, 409, 429, 503…)
    body: {"error": "<firestore message>"}

FUNCTION sendJson(exchange, status, json):      # → sendText(..., "application/json; charset=utf-8")
FUNCTION sendText(exchange, status, type, body):
    Content-Type: <type>
    X-Content-Type-Options: nosniff
    Cache-Control: no-store                    # an API response is never cached
    write body

FUNCTION handleStaticFiles(exchange):
    GET and HEAD only, else 405
    "/" → /index.html
    requested ← normalise(webRoot + path)
    IF requested is outside webRoot OR not a regular file:  404     # ../ traversal blocked
    headers (documents only, not API JSON):
        X-Content-Type-Options: nosniff
        Cache-Control: no-store
        Content-Security-Policy: default-src 'self'; style-src 'self'; script-src 'self';
            img-src 'self' data:; connect-src 'self' + Firebase identity/token hosts;
            object-src 'none'; base-uri 'self'
        Referrer-Policy: no-referrer
        Permissions-Policy: camera=(), microphone=(), geolocation=()
    serve bytes with contentTypeFor(path)   # HEAD sends Content-Length and no body
```

---

## 4. Storage

Two interfaces per concern. One implementation talks to Firestore, one to an array in memory. The
services only ever see the interface, which is why the whole product is testable with no emulator.

### 4.1 The interfaces

```
INTERFACE EmployeeStore          findAll() → [Employee];  findById(id) → Optional<Employee>
INTERFACE MutableEmployeeStore   create(e) → Employee;    update(e) → Employee
INTERFACE EmployeeScheduleStore  findScheduleIdForEmployee(id) → OptionalInt
INTERFACE AttendanceStore        findByScheduleId(n) → [AttendanceRecord];  findAll() → […]
INTERFACE WorkflowStore          listLeaveRequests / createLeaveRequest / cancelLeaveRequest
                                 listTickets / createTicket / cancelTicket
INTERFACE PayrollStore           findAll() → [PayrollRun]; findById(id) → Optional<PayrollRun>
                                 create(run) → PayrollRun
```

`MutableEmployeeStore` is separate from `EmployeeStore` on purpose: a read-only store (any future
export or projection) can implement the read interface and `EmployeeService` will refuse writes with
`IllegalStateException("This employee store is read-only.")` instead of half-working.

### 4.2 `FirestoreRestClient` — the only code that speaks Firestore

```
CONSTRUCTOR(projectId, uid, idToken[, apiRoot]):
    validate projectId and uid are safe single path segments
    IF idToken blank: THROW "Firebase sign-in is required"
    normalise apiRoot to end with "/"

FUNCTION listDocuments(collectionPath):                 # paged, follows nextPageToken
    documents ← []
    LOOP:
        url ← documentsUrl(collectionPath) + "?pageSize=1000" + pageToken
        response ← send("GET", url)
        append response.documents to documents
        pageToken ← response.nextPageToken or ""
    UNTIL pageToken empty
    → documents

FUNCTION createDocument(collectionPath, documentId, fields):
    url ← documentsUrl(collectionPath) + "?documentId=" + urlencode(documentId)
    → send("POST", url, {fields}, allowExisting=true).has("name")     # false = ALREADY_EXISTS

FUNCTION updateDocument(collectionPath, documentId, fields, updateMask):
    url ← …/{documentId} + "?updateMask.fieldPaths=" + each field
    → send("PATCH", url, {fields}).has("name")

FUNCTION send(method, url, body, allowExisting):
    request with Authorization: Bearer <idToken>, Accept: application/json,
            Content-Type when there is a body, 20 s timeout
    IF response 2xx → parse JSON (or {} when empty)
    IF allowExisting AND (409 or 400 with ALREADY_EXISTS) → {}          # idempotent create
    ELSE → firestoreError(status, body)                                 # becomes HTTP status
    network error / interrupted → FirestoreException(503, "Could not reach Firestore…")

FUNCTION documentsUrl(collectionPath):
    apiRoot + urlencode(projectId) + "/databases/(default)/documents"
      + "/" + urlencode(each segment of collectionPath)

FUNCTION userCollection(name):
    → "users/" + uid + "/" + name           # ← every collection goes through this
```

**The security model in one line:** `userCollection()` is the only way a path is built, and `uid`
came from the validated token, so no handler can address another user's data even if it tried.

### 4.3 Document ↔ object mapping

Each Firestore store is *only* mapping — it holds no rules.

```
FUNCTION FirestoreEmployeeStore.findAll():
    → listDocuments("users/{uid}/demoEmployees").map(fromDocument)

FUNCTION FirestoreEmployeeStore.fromDocument(doc):
    fields ← doc.fields or {}
    id, name, department, role, location, status ← stringValue of each
    monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance ←
        amountOrNull(...)                  # absent field → null, NOT zero (see §6.4)

FUNCTION FirestoreEmployeeStore.create(e):
    createDocument(collection, e.id, employeeFields(e)); on false → 409 "ID already exists"

FUNCTION employeeFields(e):
    six string fields, plus the four pay fields as decimal STRINGS when a pay profile exists
    # Firestore has no decimal type; strings keep centavo precision exact.

FUNCTION FirestoreAttendanceStore.fromDocument(doc):
    attendanceId, scheduleId ← integerValue
    date, timeIn, breakOut, breakIn, timeOut ← stringValue (break punches optional)
    hoursWorked ← decimal string → BigDecimal
    dayType ← stringValue, default "DEMO"

FUNCTION FirestorePayrollStore.runFields(run):
    payPeriod, periodStart, periodEnd, rulesetId, taxBasis, statutoryBasis, createdAt (strings)
    payslips ← arrayValue[ {PayslipJson.toJson(slip)} … ]      # the whole computed slip

FUNCTION FirestorePayrollStore.fromDocument(doc):
    read header strings
    read payslips[].arrayValue.values[] → PayslipJson.fromJson
    IF no payslips: THROW "Stored payroll run <id> has no payslips."     # refuse a broken snapshot
    → PayrollRun
    # No recalculation anywhere in this function. A reprint is a read.

FUNCTION FirestorePayrollStore.findById(id):
    → findAll().first(run.id == id)         # small collection, one read, no index needed
```

### 4.4 The in-memory twins (test fixtures only)

```
InMemoryEmployeeStore       GENERATED from MotorPH's employee CSV: all 34 rows, 10001…10034,
                            with real names, positions, basic pay and the three allowances.
                            Do not hand-edit - run tools/generate_fixtures.py.
                            create/update mutate a LinkedHashMap; new IDs are the next
                            free five-digit number (10035, …)
InMemoryEmployeeScheduleStore   10001→101, 10002→102, 10003→103, 10015→104
                            (only these four carry seeded rows; the other 30 stay unmapped
                            because MotorPH's file has no schedule column)
InMemoryAttendanceStore     28 rows: 101=3, 102=2, 103=1, 104=22 (Dec-2023 timecard copy)
                            enforces one row per (scheduleId, date)
InMemoryWorkflowStore       two lists, generate IDs LV-/TK- + 12 hex
InMemoryPayrollStore        simple list; create appends
```

`DemoSeedService` reads these same fixtures to build its Firestore payloads, so the seeded cloud
data and the fixture data can never drift apart.

---

## 5. Primitives

### 5.1 `Money` — every centavo in the system passes through here

```
round(value)                    → value.setScale(2, HALF_UP)        # one rounding rule, one place
parse("₱ 53,500.00")            → strips ₱, commas, nbsp → BigDecimal("53500.00")
parseOrZero(null | "")          → 0.00
of("2675")                      → 2675.00
percentOf(base, "125")          → round(base × 125 / 100)
min(a,b) / max(a,b)             # BigDecimal comparison, never double
isNegative(v)                   → v < 0
format(v)                       → "26,750.00"      # display only; never parsed back
```

**Rule enforced everywhere:** floats are never used for money. Firestore stores decimal strings,
Java keeps `BigDecimal`, and the browser only formats strings it was handed.

### 5.2 `WorkedTime` — one timecard row → hours

```
FUNCTION hoursBetween(in, breakOut, breakIn, out):
    minutes ← out − in
    IF minutes < 0:                                fail "time out must not precede time in"
    IF both break punches present:
        breakMinutes ← breakIn − breakOut
        IF breakMinutes < 0:                       fail "break in must not precede break out"
        minutes ← minutes − breakMinutes
    IF minutes < 0:                                fail "the meal break is longer than the time at work"
    → round(minutes / 60, 2)

FUNCTION minutesBetween(...):  same, whole minutes, no rounding (for chained arithmetic)
```

The supplied MotorPH timecard is the reference: `08:00 / 12:00 / 13:00 / 17:00` → 9 hours elapsed
minus a 1-hour meal = **8.00**. Consequence, stated openly: a row with **no** break punches returns
**9.00**, because the app cannot invent a lunch break that was never punched. `TimesheetServiceTest`
pins both cases.

### 5.3 `PayrollRuleset` — the statutory tables, effective-dated

```
CONSTRUCTOR(year):
    id              ← "PH-STATUTORY-" + year
    effectiveFrom   ← year + "-01-01";  effectiveTo ← year + "-12-31"
    SSS             employee 5%, employer 10%, MSC 5,000–35,000 step 500,
                    EC 10.00 below MSC 15,000 else 30.00, MPF above MSC 20,000
    PhilHealth      5% of basic, floor 10,000, ceiling 100,000, split 50/50
    Pag-IBIG        fund capped at 10,000; employee 1% at ≤1,500 else 2%; employer 2%

forYear(2026) → Y2026 ; forYear(2025) → Y2025
forYear(anything else) → THROW "No statutory ruleset is loaded for <year>…"
    # Refusing is the feature: the app will not borrow a neighbouring year's table.
```

**Contribution subfunctions** — each takes the *monthly* basic:

```
monthlySalaryCredit(basic):                       # SSS salary credit
    clamped ← min(max(basic, 5,000), 35,000)
    rounded ← ceiling(clamped / 500) × 500        # round UP to the next ₱500 step
    → min(max(rounded, 5,000), 35,000)

sssEmployee(basic)  → round(MSC × 0.05)
sssEmployer(basic)  → round(MSC × 0.10)
sssEc(basic)        → MSC ≥ 15,000 ? 30.00 : 10.00

philHealthTotal(basic):
    base ← min(max(basic, 10,000), 100,000)
    → round(base × 0.05)
philHealthEmployee(basic) → round(total / 2)
philHealthEmployer(basic) → round(total − employee)      # they always sum to the total

pagibigEmployee(basic):
    fund ← min(max(basic, 0), 10,000)
    → round(fund × (fund ≤ 1,500 ? 0.01 : 0.02))
pagibigEmployer(basic):
    fund ← min(max(basic, 0), 10,000)
    → round(fund × 0.02)
```

**Withholding** — BIR Annex E, two separate tables:

```
MONTHLY table          floor      base       rate
                       20,833.33     0.00    15%
                       33,333.33  1,875.00    20%
                       66,666.67  8,541.80    25%
                      166,666.67 33,541.80    30%
                      666,666.67 183,541.80   35%

SEMI_MONTHLY table     floor      base       rate
                       10,416.67     0.00    15%
                       16,666.67   937.50    20%
                       33,333.33 4,270.70    25%
                       83,333.33 16,770.70   30%
                      333,333.33 91,770.70   35%

FUNCTION withholding(taxable, period):
    chosen ← the LAST bracket whose floor is strictly below taxable    # walk the table upward
    IF none: → 0.00
    → round(bracket.base + (taxable − bracket.floor) × bracket.rate)

FUNCTION TaxBracket.apply(taxable):
    → round(base + (taxable − floor) × rate)
```

The semi-monthly table is **not** the monthly table halved — it is its own published table with its
own bases. The floors are the annual statutory thresholds divided by 12 (or 24), carried to the
centavo: 400,000 / 12 = 33,333.33, not 33,333. Only the exact twelfths reproduce the supplied
payslip (5,250.83 rather than 5,250.90). See `docs/payroll-ruleset-and-golden-case.md` §3.

`Period.parse("semi-monthly" | "SEMIMONTHLY" | "SEMI")` → `SEMI_MONTHLY`; anything else is a `400`.

---

## 6. Employees

### 6.1 `Employee` — the record

```
FIELDS  id, name, department, role, location, status,
        monthlyBasic?, riceSubsidy?, phoneAllowance?, clothingAllowance?
CONSTRUCTOR: trims text, rejects blank, rejects negative money, status ∈ {Active, On leave}
withPay(basic, rice, phone, clothing) → copy with pay inputs
hasPayProfile() → monthlyBasic != null
```

`monthlyBasic` being **nullable** is load-bearing: `null` means "no salary on file", which is
different from `0.00` ("salary of zero"). The payroll picker only lists employees with a pay profile,
and a Firestore document missing the field reads back as `null`, not zero.

### 6.2 `EmployeeService` — the use cases

```
listEmployees() → store.findAll()

findEmployee(id) → store.findById(trimmed id)         # never throws; Optional

createEmployee(name, dept, role, location, status, [basic, rice, phone, clothing]):
    store MUST implement MutableEmployeeStore, else 409 "This employee store is read-only."
    id ← highest existing five-digit number + 1       # server-issued; client cannot choose it
         (10035 for the current 34-person directory)
    → store.create(validated(id, name, dept, role, location, status, pay fields…))

updateEmployee(id, name, dept, role, location, status, [basic, rice, phone, clothing]):
    IF id does not match [0-9]{5}:  fail 400 "Employee ID is invalid."
    existing ← findEmployee(id) or 404
    basic    ← supplied ? supplied : existing.monthlyBasic     # omitted = keep, never zero
    rice     ← supplied ? supplied : existing.riceSubsidy
    phone    ← supplied ? supplied : existing.phoneAllowance
    clothing ← supplied ? supplied : existing.clothingAllowance
    → store.update(validated(...))

searchEmployees(q):
    IF q blank → all employees
    lowercase q; keep employees whose "id name dept role location status" contains it
    # One searchable string, case-insensitive, across every displayed field.

validated(id, name, dept, role, location, status, …):
    each text field trimmed, non-blank, ≤ 100 chars
    status must be exactly "Active" or "On leave"
```

### 6.3 The employee HTTP handler

```
handleEmployees(exchange):
    path ← /api/employees or /api/employees/{id}
    services ← servicesFor(exchange)               # builds a Firestore client in real mode → 401 if unsigned

    GET collection:
        q ← query param "q"
        items ← q ? searchEmployees(q) : listEmployees()
        → { "items": [employeeToJson(e)…], "total": items.size, "matched": items.size }

    GET detail:
        reject path traversal (empty id or contains "/") → 404
        findEmployee(id) or → 404 {"error":"Employee not found"}
        → { "item": employeeToJson(e) }

    POST collection:
        body ← readJsonBody(exchange)
        requireOnlyKeys(body, name, department, role, location, status,
                              monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance)
        reject any client-supplied "id" explicitly            → 400 "client cannot choose an ID"
        e ← createEmployee(...)
        → 201 { "item": employeeToJson(e) }

    PUT detail:
        same key whitelist; updateEmployee(id, ...) → 200 {"item": …}

    DELETE → 405                     # the product has no employee deletion
    other  → 405

FUNCTION employeeToJson(e):
    { id, name, department, role, location, status,
      monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance }   # money as decimal strings
```

### 6.4 Worked example — create with pay inputs

```
POST /api/employees  {"name":"Iris Mariano","department":"Operations","role":"Coordinator",
                      "location":"Taguig","status":"Active","monthlyBasic":"22500"}
→ 201 {"item":{"id":"10035","name":"Iris Mariano", …}}             # number issued by the server
→ GET /api/attendance?employeeId=10035 → {"count":0}               # no schedule mapping yet
PUT  …/10035  {"name":"Iris Mariano-Reyes", …}                     # pay fields omitted
→ 200, and monthlyBasic is unchanged (not zeroed)
```

---

## 7. Attendance and timesheets

### 7.1 `AttendanceRecord`

```
FIELDS  attendanceId, scheduleId, date, timeIn, breakOut?, breakIn?, timeOut, dayType, hoursWorked
hasBreak() → breakOut and breakIn are both present
```

`hoursWorked` is **stored**, not computed on read — the raw timecard already contains the column and
the app does not silently redefine it. `WorkedTime` is used when a value must be derived (tests, and
any future capture path).

### 7.2 `AttendanceService` — reading raw rows

```
FUNCTION findForEmployee(employeeId, from, to):
    IF employee unknown:                    fail 404 "employee not found"
    IF from > to:                           fail 400 "from date must be on or before to date"
    scheduleId ← scheduleStore.findScheduleIdForEmployee(employeeId)
    IF no mapping:                          → []          # unmapped, not an error
    → attendanceStore.findByScheduleId(n)
        .filter(date ≥ from)                            # both bounds inclusive
        .filter(date ≤ to)

FUNCTION summarizeRawRecords(from, to):                  # used by the coverage report
    FOR each employee in listEmployees():
        schedule ← mapping or null
        IF null: summary(employee, schedule=null, count=0, "", "")     # "unmapped" is explicit
        ELSE:
            matched ← all records for that schedule within [from, to]
            summary(employee, schedule, matched.size, min(date), max(date))
    → summaries sorted by employeeId
    # Describes coverage. Never classifies absence, never derives pay hours.
```

### 7.3 `TimesheetService` — the bridge to payroll

```
FUNCTION summarise(employeeId, from, to):
    IF from or to missing:                  fail 400 "A timesheet period needs both a start and an end date."
    records ← attendanceService.findForEmployee(employeeId, from, to)

    byDate ← {} ; total ← 0
    FOR each record:
        hours ← record.hoursWorked
        IF hours ≤ 0: CONTINUE               # weekend/holiday zero rows never inflate days worked
        byDate[date] += hours                # duplicate rows on one date merge into one day
        total += hours

    days ← byDate sorted by date
    → Summary { employeeId, from, to, daysWorked = days.size, totalHours = round(total), days }
```

**The counting rule that matters:** a day counts when it has a **positive** hour total. The supplied
December timecard has 22 rows but only **19** working days — 12-08, 12-25 and 12-30 are zero-hour
rows, correctly excluded. That is exactly why the golden case can say "19 day(s), 152.00 paid hours".

### 7.4 Attendance HTTP handlers

```
handleAttendance(exchange):
    GET:
        employeeId ← query param, required             → 400 "employeeId is required"
        from, to ← optional ISO dates                  → 400 on malformed or reversed
        items ← attendanceService.findForEmployee(...) → 404 when the employee is unknown
        → { items: [attendanceRecordToJson…], count, hoursCalculated: false }
           # hoursCalculated:false is a promise: nothing here turned punches into pay.
    POST → 405      # there is no attendance capture or correction in this product
    other → 405

handleAttendanceReport(exchange):
    GET only
    from, to ← optional; reversed → 400
    summaries ← attendanceService.summarizeRawRecords(from, to)
    → { items:[{employeeId, employeeName, scheduleId, hasScheduleMapping, rawRecordCount,
                firstRecordDate, lastRecordDate}…],
        count, from, to, hoursCalculated:false }
```

`attendanceRecordToJson` emits every stored field, including `breakOut`, `breakIn` and
`hoursWorked`, so the UI can show the raw row without guessing.

---

## 8. Workflow (leave requests and tickets)

**Scope, stated plainly:** these are validated request records with a queue label and a cancel
action. There is no reviewer, no approval, no notification, no balance, and no payroll effect.

### 8.1 `WorkflowService`

```
LEAVE_TYPES  = [Vacation, Medical, Personal, Unpaid]
TICKET_QUEUES = { "Overtime request": Team Lead, "Leave request": Team Lead,
                  "Timesheet correction": Team Lead, "Payroll inquiry": Payroll Staff,
                  "HR inquiry": HR, "Technical support": IT Admin }
MAX_TEXT_LENGTH = 240

listLeaveRequests() / listTickets() → store lists (newest first)

FUNCTION createLeaveRequest(employeeId, leaveType, startDate, endDate, note):
    employee ← requireEmployee(employeeId)            # must exist in this sandbox → 404 otherwise
    IF leaveType not in LEAVE_TYPES:                  fail 400 "Choose a valid leave type."
    start ← parseDate(startDate); end ← parseDate(endDate)      # strict YYYY-MM-DD, real calendar date
    IF start > end:                                   fail 400 "start date must be on or before end date"
    note ← optionalText(note)                         # ≤ 240 chars, blank allowed
    → store.createLeaveRequest({
          id: "LV-" + 12 hex, employeeId, employeeName, leaveType, startDate, endDate, note,
          status: "Pending review", submittedAt: now, cancelledAt: "" })

FUNCTION cancelLeaveRequest(id):
    requireId(id, "LV-")                              # must look like LV-[A-F0-9]{12}
    → store.cancelLeaveRequest(id, now)                # store decides 404 vs 409

FUNCTION createTicket(employeeId, ticketType, summary):
    employee ← requireEmployee(employeeId)
    queue ← TICKET_QUEUES[ticketType] or fail 400 "Choose a valid ticket type."
    summary ← requiredText(summary)                   # non-blank, ≤ 240 chars
    → store.createTicket({ id: "TK-"+12 hex, …, queue, status: "Open", submittedAt: now })

FUNCTION cancelTicket(id): requireId(id, "TK-"); → store.cancelTicket(id, now)
```

### 8.2 `FirestoreWorkflowStore` — the state machine that guards cancels

```
cancelLeaveRequest(id, cancelledAt):
    current ← listLeaveRequests().find(id)            or 404 "not found in this user sandbox"
    IF current.status != "Pending review":            fail 409 "Only a pending leave request can be cancelled."
    updateDocument(collection, id, {status:"Cancelled", cancelledAt}, mask=[status, cancelledAt])
    → current.cancelled(cancelledAt)

cancelTicket(id, cancelledAt):
    current ← listTickets().find(id)                  or 404
    IF current.status != "Open":                      fail 409 "Only an open ticket can be cancelled."
    update the two fields only (PATCH with an update mask, so nothing else can be overwritten)
```

Note the shape: **read → check state → patch two named fields**. There is no "update whole document"
path, so a cancel can never blank a note or rewrite a submission time.

### 8.3 HTTP

```
handleWorkflows(exchange):
    services ← servicesFor(exchange)                 # 401 when unsigned in real mode
    GET  /api/workflows          → { leaveRequests:[…], tickets:[…] }
    POST /api/workflows/leave    → create → 201 { item, storage:"user-scoped-firestore"|"local-demo" }
    POST /api/workflows/leave/{id}/cancel   → 200 { item }
    POST /api/workflows/tickets  → create → 201 { item, storage }
    POST /api/workflows/tickets/{id}/cancel → 200 { item }
    anything else → 405
```

---

## 9. Payroll — the core

This is the part that was missing before 2026-11-05 and is now the most tested part of the system.

### 9.1 `PayrollService.Input` — everything a computation may depend on

```
employeeId, employeeName, position, department      # identity, copied onto the payslip
payPeriod            MONTHLY | SEMI_MONTHLY
periodStart, periodEnd                              # ISO dates, part < = end
monthlyBasic                                        # from the employee record
daysWorked                                          # operator-confirmed (see §12)
overtimeHours, overtimeRatePercent        (125)
nightDifferentialHours, nightDifferentialRatePercent   (10)
riceSubsidy, phoneAllowance, clothingAllowance      # non-taxable by default
taxableAllowance, otherDeductions
taxBasis          BASIC_ONLY | BASIC_PLUS_TAXABLE_ALLOWANCE
statutoryBasis    FULL_MONTHLY_EACH_PERIOD | SPLIT_EVENLY
```

### 9.2 `compute(input, ruleset)` — the whole calculation, in order

```
FUNCTION compute(input, ruleset):
    validate(input)                                   # §9.3 — all of it, before any arithmetic
    period ← Period.parse(input.payPeriod)
    taxBasis ← parseTaxBasis(input.taxBasis)
    statutoryBasis ← parseStatutoryBasis(input.statutoryBasis)

    ── 1. rates ─────────────────────────────────────────────────────────────
    monthlyBasic ← round(input.monthlyBasic)
    dailyRate    ← round(monthlyBasic / 20, 2)        # MotorPH's divisor, proven by the payslip
    hourlyRate   ← round(monthlyBasic / 168, 2)       # verified against every salary row supplied
    daysWorked   ← input.daysWorked

    ── 2. earnings ──────────────────────────────────────────────────────────
    basePay  ← round(dailyRate × daysWorked)
    overtimePay ← round(hourlyRate × otHours × otRate% / 100)
    nightDiffPay ← round(hourlyRate × ndHours × ndRate% / 100)
    grossIncome ← round(basePay + overtimePay + nightDiffPay)

    ── 3. benefits (what sweldongpinoy calls allowances) ─────────────────────
    nonTaxable ← round(rice + phone + clothing)
    taxableAllowance ← round(input.taxableAllowance)
    totalBenefits ← round(nonTaxable + taxableAllowance)

    ── 4. how much statutory lands in THIS period ───────────────────────────
    divisor ← (statutoryBasis == SPLIT_EVENLY AND period == SEMI_MONTHLY)
                 ? 2
                 : 1
    sss        ← perPeriod(ruleset.sssEmployee(monthlyBasic),   divisor)
    philHealth ← perPeriod(ruleset.philHealthEmployee(monthlyBasic), divisor)
    pagibig    ← perPeriod(ruleset.pagibigEmployee(monthlyBasic), divisor)

    ── 5. withholding: computed on the MONTHLY figure, then split ───────────
    monthlyStatutory ← round(sssEmployee(monthly) + philHealthEmployee(monthly) + pagibigEmployee(monthly))
    taxableMonthly   ← BASIC_PLUS_TAXABLE_ALLOWANCE
                          ? round(monthlyBasic − monthlyStatutory + taxableAllowance)
                          : round(monthlyBasic − monthlyStatutory)
    IF taxableMonthly < 0: taxableMonthly ← 0        # a tiny salary cannot go negative for tax
    monthlyWithholding ← ruleset.withholding(taxableMonthly, MONTHLY)
    withholding        ← perPeriod(monthlyWithholding, divisor)

    ── 6. deductions and net ────────────────────────────────────────────────
    otherDeductions ← round(input.otherDeductions)
    totalDeductions ← round(sss + philHealth + pagibig + withholding + otherDeductions)
    takeHomePay     ← round(grossIncome + totalBenefits − totalDeductions)
    IF takeHomePay < 0:
        fail 400 "Deductions exceed pay for <id> (take-home would be negative). Correct the inputs…"
        # Explicitly refused rather than stored as a negative payslip.

    ── 7. employer side (cost, never deducted from the employee) ────────────
    employerSss        ← perPeriod(ruleset.sssEmployer(monthlyBasic), divisor)
    employerEc         ← perPeriod(ruleset.sssEc(monthlyBasic), divisor)
    employerPhilHealth ← perPeriod(ruleset.philHealthEmployer(monthlyBasic), divisor)
    employerPagibig    ← perPeriod(ruleset.pagibigEmployer(monthlyBasic), divisor)
    employerCost ← round(grossIncome + totalBenefits + the four employer amounts above)

    → Payslip { identity, period, rulesetId, rates, days, premiums, benefits,
                employee deductions, employer shares, taxableMonthly, takeHomePay }
```

Two conventions are deliberately exposed as **assumption switches**, because the supplied payslip
cannot settle them on its own:

| Switch | Default (matches MotorPH) | Alternative | What changes |
|---|---|---|---|
| `statutoryBasis` | `FULL_MONTHLY_EACH_PERIOD` — deduct the full monthly SSS/PhilHealth/Pag-IBIG in a semi-monthly cutoff | `SPLIT_EVENLY` — half in each cutoff | SSS 1,750 vs 875; PhilHealth 1,337.50 vs 668.75; Pag-IBIG 200 vs 100 |
| `taxBasis` | `BASIC_ONLY` — tax on basic less statutory | `BASIC_PLUS_TAXABLE_ALLOWANCE` | taxable base, therefore withholding |

Both are stored on every saved run, so a reprint always says which convention produced it.

### 9.3 `validate(input)` — every rejection, in order

```
null input                      → "Payroll input is required."
employeeId blank                → "Employee ID is required."
monthlyBasic null / negative    → "Monthly basic pay is required." / "…must not be negative."
daysWorked null / negative      → "Days worked is required." / "…must not be negative."
daysWorked > 20 AND MONTHLY     → "Days worked cannot exceed 20 in one monthly period."
daysWorked > 10 AND SEMI_MONTHLY→ "Days worked cannot exceed 10 in one semi-monthly period."
overtimeHours, ndHours, rice, phone, clothing, taxableAllowance,
  otherDeductions, otRate%, ndRate%  negative → "<field> must not be negative."
payPeriod unparseable           → "Pay period must be Monthly or Semi-monthly."
periodStart / periodEnd         → must be real YYYY-MM-DD (round-tripped through LocalDate)
periodStart > periodEnd         → "Period start must be on or before period end."
```

The payload itself was already filtered upstream: unknown JSON fields are rejected by
`requireOnlyKeys`, and a non-JSON `Content-Type` never reaches the service.

### 9.4 The golden case, number by number

Input: monthly basic **53,500**, semi-monthly, 2026-01-02 → 2026-01-15, **10** days, rice 1,500,
phone 2,000, clothing 1,000, defaults for both switches.

```
dailyRate       53,500 / 20                      = 2,675.00
hourlyRate      53,500 / 168                     =   318.45
basePay         2,675.00 × 10                    = 26,750.00
grossIncome                                       = 26,750.00
benefits        1,500 + 2,000 + 1,000             =  4,500.00

MSC             ceil(53,500/500)×500 → 54,000, clamp → 35,000
SSS employee    35,000 × 5%                       =  1,750.00
PhilHealth      53,500 × 5% = 2,675 ÷ 2           =  1,337.50
Pag-IBIG        min(53,500, 10,000) × 2%          =    200.00
monthlyStatutory                                  =  3,287.50
taxableMonthly  53,500 − 3,287.50                  = 50,212.50
withholding     1,875 + (50,212.50 − 33,333.33)×20% = 5,250.83
totalDeductions  1,750 + 1,337.50 + 200 + 5,250.83 =  8,538.33
takeHomePay     26,750 + 4,500 − 8,538.33          = 22,711.67

employer        SSS 3,500.00 + EC 30.00 + PhilHealth 1,337.50 + Pag-IBIG 200.00
employerCost    31,250.00 + 5,067.50               = 36,317.50
```

Every one of those thirteen figures is asserted in `PayrollGoldenTest` (61 checks) and re-asserted
over HTTP in `AppServerIntegrationTest`, then again in a real browser by the e2e suite.

### 9.5 `remittanceRows(payslips)` — what the employer must remit

```
FOR each payslip:
    sss  += sssEmployee + sssEmployer
    ec   += sssEc
    ph   += philHealthEmployee + philHealthEmployer
    pi   += pagibigEmployee + pagibigEmployer
    tax  += withholdingTax
→ [ ("Social Security System", …), ("Employees' Compensation", …), ("PhilHealth", …),
    ("Pag-IBIG", …), ("BIR withholding", …) ]
```

### 9.6 `Payslip` and `PayslipJson`

`Payslip` is an immutable value object built through a builder with seven groups: `employee`,
`period`, `ruleset`, `salary`, `premiums`, `benefits`, `deductions`, `employer`, `result` — 33
accessors, no logic.

`PayslipJson` is the *only* serializer/deserializer for it:

```
toJson(slip):   33 fields; money → 2-decimal strings; hours/days → plain decimal strings
fromJson(node): reverse; missing numeric field → 0.00 (a truncated snapshot reads as zero,
                never as a crash) ; missing text field → ""
```

Round-trip equality is tested in `FirestorePayrollStoreTest`, which is what makes "a reprint is a
read, not a recalculation" a verified claim rather than a comment.

### 9.7 `PayrollRun` — the immutable snapshot

```
FIELDS  id ("PR-YYYYMMDD-XXXXXXXX"), payPeriod, periodStart, periodEnd,
        rulesetId, taxBasis, statutoryBasis, createdAt, payslips[]
```

The run stores the **inputs' meaning** (`rulesetId`, `taxBasis`, `statutoryBasis`) and the **outputs**
(every payslip line). Nothing about it can be recomputed differently later: the statutory tables can
change next year and last year's payslip still prints what was paid.

### 9.8 Payroll HTTP handlers, in order

```
handlePayroll(exchange):
  GET /api/payroll/rules?year=YYYY
      year ← query param; blank → 2026; non-numeric → 400
      ruleset ← PayrollRuleset.forYear(year)        # uncovered year → 400, never a guess
      → rulesetToJson(ruleset)
          { id, currency:"PHP", effectiveFrom, effectiveTo, daysPerMonth:"20", hoursPerMonth:"168",
            sss:{employeeRate, employerRate, minMsc, maxMsc, mscStep, ecLow, ecHigh,
                 ecThreshold, mpfThreshold},
            philHealth:{rate, floor, ceiling},
            pagibig:{maxFund, lowThreshold, employeeLowRate, employeeHighRate, employerRate},
            monthlyTaxBrackets:[…], semiMonthlyTaxBrackets:[…] }

  GET /api/payroll/timesheet?employeeId=&from=&to=
      employeeId required                          → 400
      from/to parsed; TimesheetService.summarise → Summary
      → { employeeId, from, to, daysWorked, totalHours, days:[{date, dayType, hours}…] }

  POST /api/payroll/preview
      input ← payrollInputFrom(readJsonBody)       # §9.9
      ruleset ← forYear(year of periodStart)
      slip ← new PayrollService().compute(input, ruleset)
      → 200 { payslip, saved:false, ruleset }      # nothing written

  POST /api/payroll/runs
      identical computation, then:
      run ← payrollStoreFor(exchange).create({
                id: "PR-" + periodStart without dashes + "-" + 8 random hex,
                payPeriod, periodStart, periodEnd, rulesetId,
                taxBasis, statutoryBasis, createdAt: now, payslips: [slip] })
      → 201 { run (with payslips), saved:true }    # 409 if that id already exists

  GET /api/payroll/runs
      runs ← store.findAll()                       # sorted newest first
      → { items:[ runToJson(run, includePayslips=false)… ], count }
      # history stays a summary: no payslip bodies in the list

  GET /api/payroll/runs/{id}
      reject empty id or an id containing "/"      → 404
      run ← store.findById(id) or 404
      → { run }                                    # with payslips

  DELETE /api/payroll/runs/{id} → 405              # immutable by design
  anything else                 → 405

FUNCTION runToJson(run, includePayslips):
    id, payPeriod, periodStart, periodEnd, rulesetId, taxBasis, statutoryBasis, createdAt
    employeeCount ← payslips.size
    totalGross    ← Σ grossIncome                   # payroll gross only
    totalBenefits ← Σ totalBenefits
    totalEarnings ← totalGross + totalBenefits      # what a payslip prints as total earnings
    totalDeductions, totalNet, totalEmployerCost  ← Σ of each
    IF includePayslips: payslips:[PayslipJson…]
```

`totalGross`, `totalBenefits` and `totalEarnings` are three separate numbers on purpose: an earlier
version folded benefits into "gross" and reported 31,250 where the payslip said 26,750.

### 9.9 `payrollInputFrom(body)` — the request contract

```
requireOnlyKeys(body, [employeeId, employeeName, position, department, payPeriod,
                       periodStart, periodEnd, monthlyBasic, daysWorked,
                       overtimeHours, overtimeRatePercent, nightDifferentialHours,
                       nightDifferentialRatePercent, riceSubsidy, phoneAllowance,
                       clothingAllowance, taxableAllowance, otherDeductions,
                       taxBasis, statutoryBasis])
    # A field like "bonus" is a 400. Typos cannot be silently ignored.

employeeId, periodStart, periodEnd   required strings
payPeriod                            default "MONTHLY"
money and hours fields               accept a JSON number or a decimal string, default 0
overtimeRatePercent                  default 125 ; nightDifferentialRatePercent default 10
taxBasis / statutoryBasis            default BASIC_ONLY / FULL_MONTHLY_EACH_PERIOD
```

### 9.10 Saved-run lifecycle

```
1. UI computes a preview      POST /preview   → figures shown, nothing stored
2. Operator reviews           payslip + remittance + rate table on screen
3. Operator saves             POST /runs      → 201, run id PR-20260102-77BC9231
4. History lists summaries    GET  /runs      → totals per run, newest first
5. Anyone reprints            GET  /runs/{id} → stored payslip lines, no recomputation
6. Nobody can alter or delete DELETE           → 405; Firestore rules allow create+read only
```

---

## 10. Demo seeding

`POST /api/demo/seed` — the only way fixture data ever reaches a real Firestore project.

```
FUNCTION seedMissingSyntheticRecords(client):
    employeesCreated ← 0
    FOR each employee in a FRESH InMemoryEmployeeStore:
        IF createDocument("users/{uid}/demoEmployees", id, employeeFields):
            employeesCreated++                      # existing documents are left untouched

    mappingsCreated ← 0
    FOR each employee with a fixture schedule mapping:
        IF createDocument("users/{uid}/demoEmployeeSchedules", id, {employeeId, scheduleId}):
            mappingsCreated++

    attendanceCreated ← 0
    FOR scheduleId in [101, 102, 103, 104]:
        FOR each fixture row on that schedule:
            IF createDocument("users/{uid}/demoAttendance", attendanceId, fields):
                attendanceCreated++
    → { employeesCreated, scheduleMappingsCreated, attendanceCreated }
```

Properties worth knowing:

- **Idempotent.** A second call reports `0, 0, 0` because `createDocument` refuses to overwrite.
- **Additive.** It never updates or deletes anything already in the sandbox.
- **Synthetic only.** The fixtures are the fictional MotorPH training set; pay profiles are marked
  as demo values. It is the only writer that can create attendance rows — there is no capture form.
- **Owner-scoped.** `client.userCollection(...)` puts everything under the caller's `uid`.

---

## 11. The browser

`web/index.html` is one page with eight views toggled by `switchView()`; `web/app.js` holds all the
wiring. Fifteen small modules hold the pure logic so it can be unit-tested in Node with no browser.

### 11.1 Module map

| Module | Functions | What they do |
|---|---|---|
| `app.js` | 66 | All DOM wiring, fetch calls, rendering, auth screens |
| `payroll.js` | 8 | Payslip layout, CSV columns, run summaries, formatting |
| `workflow-demo.js` | 10 | Client-side leave/ticket state machine (used with fixtures) |
| `attendance-calendar.js` | 3 | Month grid from raw rows |
| `leave-calendar.js` | 3 | Month grid + date validation |
| `csv-export.js` | 3 | RFC-4180-safe CSV construction |
| `attendance-coverage-*.js`, `attendance-record-sort.js` | 3 | Filter and sort raw coverage rows |
| `directory-sort.js`, `directory-summary.js` | 2 | Employee sorting, label totals |
| `workflow-filter.js`, `workflow-sort.js`, `workflow-summary.js` | 3 | Request/ticket filtering, sorting, summary |
| `employee-record-summary*.js` | 2 | One employee's cross-record summary |
| `sandbox-search.js` | 2 | Search across employees and requests |
| `workspace-export.js` | 1 | Whole-sandbox JSON export |

### 11.2 Auth (`app.js`)

```
FUNCTION apiHeaders():                                   # every fetch goes through this
    user ← firebaseAuth.currentUser
    IF user: → { "Content-Type":"application/json",
                 "Authorization":"Bearer " + await user.getIdToken(),
                 "X-Firebase-UID": user.uid }
    ELSE:    → { "Content-Type":"application/json" }     # the API answers 401

FUNCTION signIn(email, password)        → setPersistence(NONE) → signInWithEmailAndPassword
FUNCTION createDevAccount(email, pw)    → createUserWithEmailAndPassword
FUNCTION sendPasswordReset(email)       → sendPasswordResetEmail
FUNCTION showWorkspace() / showSignIn() → swap the two top-level screens
FUNCTION friendlyAuthError(code)        → plain-English message per Firebase error code
```

Persistence is `in-memory`: closing the tab signs you out. No token is written to `localStorage`.

### 11.3 Employees view

```
FUNCTION loadEmployees():
    requestNumber ← ++state.requestNumber            # guards against out-of-order responses
    url ← q ? "/api/employees?q=" + encodeURIComponent(q) : "/api/employees"
    fetch with apiHeaders()
    IF this response is not the latest request: RETURN   # a stale search cannot overwrite a new one
    state.employees ← payload.items
    IF no query: state.allEmployees ← items; repopulate the attendance, workflow and payroll pickers
    seedDemoButton.hidden ← NOT (signed in AND no query)
    renderEmployees()

FUNCTION renderEmployees():
    visible ← sortDirectoryEntries(state.employees, {field, direction})
    FOR each employee: row = [initials avatar + name + id, department, role, location, status pill]
        row.click → openEmployee(employee)

FUNCTION openEmployee(e):
    render detail panel (identity, contact-style fields, status)
    loadEmployeeSandboxRecords(e)                    # that employee's attendance + requests + tickets

FUNCTION editSyntheticEmployee(...):
    PUT /api/employees/{id} with only the editable fields → reload

FUNCTION seedSyntheticDemoData():
    POST /api/demo/seed → "N employees, M schedule links, K attendance rows added" → reload
```

### 11.4 Attendance view

```
FUNCTION applyDefaultAttendanceRange()   → current month up to today
FUNCTION loadAttendance():
    GET /api/attendance?employeeId=&from=&to=
    renders the raw table (every stored column) + the calendar + the summary line
FUNCTION renderAttendanceCalendar(rows):
    buildAttendanceCalendar(rows) → month grid; a day with rows is marked, clicking filters the table
FUNCTION loadAttendanceReport():
    GET /api/reports/attendance?from=&to=
    renders per-employee coverage (row count, first/last date, unmapped employees flagged)
FUNCTION applyAttendanceReportFilters()  → filterAttendanceCoverage + sortAttendanceCoverage → redraw
```

Both views display **stored** values. Neither derives paid hours, and the notice on the page says so.

### 11.5 Requests view

```
FUNCTION loadWorkflows()        → GET /api/workflows → state.workflow = {leaveRequests, tickets}
FUNCTION renderWorkflow()       → two tables; filterWorkflowItems + sortWorkflowItems
FUNCTION renderLeaveCalendar()  → buildLeaveCalendar; selected range highlighted on submit
FUNCTION sendWorkflowRequest(kind, payload):
    POST /api/workflows/{leave|tickets}
    IF 401 → "Sign in again" message;  IF 400 → field error under the form
FUNCTION handleWorkflowCancel(id, kind):
    POST /api/workflows/{kind}/{id}/cancel        # 409 shown as "already cancelled"
```

### 11.6 Payroll view (the one that matters)

```
FUNCTION populatePayrollEmployees():
    withPay ← state.allEmployees.filter(e => e.monthlyBasic)     # no salary → not listed
    select options = "10015 — Fredrick Romualdez" …
    keeps the previous selection if it still exists

FUNCTION applyPayrollEmployee():
    show the selected employee's stored basic/rice/phone/clothing in the status line
    → every run starts from the employee record, not from typing

FUNCTION payrollFormInput():                        # form → request body, field for field
    employeeId, payPeriod, periodStart, periodEnd, daysWorked,
    overtimeHours, overtimeRatePercent, nightDifferentialHours, nightDifferentialRatePercent,
    taxableAllowance, otherDeductions, taxBasis, statutoryBasis
    + employeeName/position/department/monthlyBasic/rice/phone/clothing from the record
    # The client sends no computed money at all.

FUNCTION computePayslip():      POST /api/payroll/preview → renderPayslip + renderRemittance
FUNCTION savePayrollRun():      POST /api/payroll/runs    → 201 → reload history, mark "saved"
FUNCTION loadPayrollRuns():     GET  /api/payroll/runs    → run-history table (summaries)
FUNCTION openPayrollRun(id):    GET  /api/payroll/runs/{id} → reprint the stored payslip
FUNCTION loadPayrollRuleset():  GET  /api/payroll/rules?year= → ruleset label + rate table
FUNCTION useStoredTimesheet():
    GET /api/payroll/timesheet?employeeId=&from=&to=
    fills DAYS WORKED from the stored timecard
    status: "Stored timecard: 19 day(s), 152.00 paid hours. Days worked filled in; hours are
             informational only."
    # It fills one field. It does not silently replace the operator's judgement.
FUNCTION exportPayslip():       payslipCsvRows([slip]) → CSV download
FUNCTION renderPayslip(slip):   sections from payslipSections() in MotorPH's print order:
    EARNINGS → BENEFITS → DEDUCTIONS → SUMMARY (Gross, Benefits, TOTAL EARNINGS, Deductions,
    Take Home) → EMPLOYER COST (not deducted from pay)
```

### 11.7 Pure helpers (unit-tested in Node)

```
payroll.js
  formatAmount("26750")            → "26,750.00"          # display only
  payslipSections(slip)            → the five sections above, raw values
  totalEarnings(slip)              → "31250.00"           # raw decimal; view formats once
  remittanceRows(slip)             → employee + employer share per agency
  payslipCsvColumns() / payslipCsvRows([slip])  → stable CSV contract
  basisSummary({taxBasis, statutoryBasis})      → human sentence for the payslip footer
  runSummary(run)                  → id, period, ruleset, employees, earnings, deductions,
                                     net, employer cost for one history row
csv-export.js
  buildCsv(columns, rows)          → escapes quotes/commas/newlines per RFC 4180
attendance-calendar.js / leave-calendar.js
  buildAttendanceCalendar(rows) / buildLeaveCalendar(range) → month matrix with marks
sorting and filtering modules      → pure, deterministic, direction-aware
workflow-demo.js                   → same state machine as §8.1, for the fixture-only demo
```

---

## 12. What is deliberately missing

Knowing what *isn't* there is as useful as knowing what is. Each item below is a real gap, not a
hidden feature — the UI says so, and the API refuses rather than guessing.

| Missing | Why it is missing | What the app does instead |
|---|---|---|
| **Attendance capture / correction** | No schedule/identity design for a durable write; a capture form without a rule would invent policy | `POST /api/attendance` → **405**; rows arrive only via the seed or Firestore |
| **Days worked derived from punches** | Absence, tardiness and undertime have no approved rule | "Use stored timesheet days" fills the field; the operator confirms it |
| **Overtime / night-differential *hours* from punches** | Requires an approval model and a premium schedule | Rates are parameterised (125% / 10%); hours are entered |
| **Leave pay, holiday and rest-day premiums, 13th month** | No rule supplied by MotorPH | Not computed; the payslip has no line for them |
| **Multi-employee run in the UI** | UI computes one employee per action | `PayrollRun` holds a list; `remittanceRows` sums many; the API is ready |
| **Approval workflow for a payroll run** | No role model | Runs are saved directly by the signed-in operator |
| **Roles, manager scope, audit trail** | Owner-only sandbox is the current security model | Firestore rules restrict every path to `users/{uid}` |
| **Statutory years before 2025 or after 2026** | No verified tables | `forYear` throws **400**; it will not borrow a neighbour's table |
| **Employee deletion** | Lifecycle policy undefined | `DELETE` → **405** |
| **Production backup / retention** | Ops concern, out of scope | Documented as a gap |

---

## 13. Feature matrix — what you can actually do

| # | Feature | Entry point | Stored where | Tested by |
|---|---|---|---|---|
| 1 | Sign in / create account / reset password | Sign-in screen | Firebase Auth | `firebase-auth-lifecycle.e2e.js` (opt-in) |
| 2 | Search employees across every displayed field | Employees view | — | `EmployeeServiceTest`, e2e |
| 3 | Sort, filter, export the directory to CSV | Employees view | — | `directory-sort`, e2e |
| 4 | Open one employee's full record | Click a row | — | e2e |
| 5 | Create a synthetic employee (server-assigned ID) | Employees view | `demoEmployees` | `EmployeeServiceTest`, `AppServerIntegrationTest` |
| 6 | Edit directory fields + pay inputs (omitted = kept) | Employees view | `demoEmployees` | `EmployeeServiceTest`, e2e |
| 7 | Seed missing synthetic fixtures (idempotent) | Employees view | 3 collections | `AppServerIntegrationTest`, e2e |
| 8 | Read raw attendance for a period | Attendance view | `demoAttendance` | `AttendanceServiceTest`, e2e |
| 9 | Attendance coverage report per employee (unmapped flagged) | Reports view | — | e2e |
| 10 | Calendar view of attendance days | Attendance view | — | e2e |
| 11 | Submit a leave request (validated, status "Pending review") | Requests view | `demoLeaveRequests` | `WorkflowServiceTest`, `FirestoreWorkflowStoreTest` |
| 12 | Cancel a leave request (409 if not pending) | Requests view | `demoLeaveRequests` | same |
| 13 | Submit a ticket (queue label per type) | Requests view | `demoTickets` | same |
| 14 | Cancel a ticket (409 if not open) | Requests view | `demoTickets` | same |
| 15 | Read the statutory ruleset for a year | Payroll view | — | `PayrollGoldenTest`, `AppServerIntegrationTest` |
| 16 | Reduce a stored timecard to days worked + paid hours | Payroll view → "Use stored timesheet days" | — | `TimesheetServiceTest` (16) |
| 17 | Compute a payslip (preview, nothing stored) | Payroll view | — | `PayrollGoldenTest` (61), HTTP, e2e |
| 18 | Save an immutable payroll run | Payroll view | `demoPayrollRuns` | `FirestorePayrollStoreTest` (16), HTTP |
| 19 | List run history with totals | Payroll view | `demoPayrollRuns` | HTTP, e2e |
| 20 | Reprint a saved payslip without recalculating | Run history → Open | `demoPayrollRuns` | `FirestorePayrollStoreTest`, HTTP |
| 21 | Export a payslip to CSV | Payslip panel | — | `payroll.test.js` |
| 22 | Remittance summary (employee + employer share) | Payroll view | — | `PayrollGoldenTest`, `payroll.test.js` |
| 23 | Statutory rate table on screen | Payroll view | — | e2e |
| 24 | Fail closed with no Firebase configuration | Startup | — | `./test.sh` guard, e2e |

---

## 14. Test map — where each claim is proven

| Suite | Checks | Covers |
|---|---|---|
| `EmployeeServiceTest` | 13 | create/update/validate/search, server-assigned IDs, read-only store refusal |
| `AttendanceServiceTest` | 23 | inclusive ranges, unmapped employees, coverage summary |
| `WorkflowServiceTest` | 32 | leave/ticket validation, queue labels, cancel state machine |
| `FirestoreRestClientTest` | 19 | URL building, token forwarding, paging, error mapping |
| `FirestoreWorkflowStoreTest` | 14 | document shapes, PATCH masks, 404/409 |
| `FirestoreEmployeeScheduleStoreTest` | 7 | mapping read, missing mapping |
| **`PayrollGoldenTest`** | **61** | every statutory function, both tables, both switches, all golden figures |
| **`FirestorePayrollStoreTest`** | **16** | create body, read-back equality, no recalculation, broken snapshot refusal |
| `TimesheetServiceTest` | 16 | punch arithmetic, zero-hour exclusion, duplicate-date merge |
| **`AppServerIntegrationTest`** | **117** | **every route over real HTTP**, incl. payroll end-to-end in both modes |
| `test.sh` startup guard | 1 | production refuses to start without Firebase |
| **Java total** | **318** | |
| `npm run test:web` | 62 | 15 pure modules, incl. payslip layout, CSV, totals |
| `npm run test:e2e` | 17 | real Chromium: full user journeys, no page errors |
| `npm run test:firebase-auth` | opt-in | account lifecycle against a live project, no writes |

---

## Appendix D — exhaustive function index

The narrative above explains every *behaviour*. This appendix names every remaining function so the
inventory is complete to the method. Grouped by file; one line each.

### D.1 `AppServer` — serialisation and plumbing helpers

```
requiredString(body, key)          → trimmed string, 400 when blank or missing
optionalString(body, key)          → "" when absent, otherwise trimmed
parseIntYear("2026" | "")          → 2026 default; non-numeric → 400 "Year must be a number."
periodYear(input)                  → the first four characters of periodStart, validated as a year
runId(input)                       → "PR-" + periodStart without dashes + "-" + 8 hex
parseOptionalDate("" | "2026-01-02") → null | LocalDate; malformed → 400
attendanceToJson(records)          → {"items":[…],"count":n,"hoursCalculated":false}
attendanceRecordToJson(r)          → one row, every stored field incl. break punches + hoursWorked
attendanceSummaryToJson(summaries, from, to)
                                   → {"items":[{employeeId,employeeName,scheduleId,
                                      hasScheduleMapping,rawRecordCount,firstRecordDate,
                                      lastRecordDate}],"count","from","to","hoursCalculated":false}
employeesToJson(list, total)       → {"items":[…],"total":n,"matched":n}
workflowsToJson(leaves, tickets, storage)
                                   → {"leaveRequests":[…],"tickets":[…],"storage"}
itemToJson(item, storage)          → {"item":…,"storage":"user-scoped-firestore"|"local-demo"}
leaveToJson(request)               → 10 fields, timestamps as ISO strings
ticketToJson(ticket)               → 10 fields, including the queue label
timesheetToJson(summary)           → {employeeId,from,to,daysWorked,totalHours,days[]}
rulesetToJson(ruleset)             → the rules payload in §9.8
runToJson(run, includePayslips)    → the run payload in §9.8
employeeToJson(employee)           → identity + status + four nullable money strings
queryParameter(rawQuery, name)     → URL-decoded value or ""
decode(value)                      → URLDecoder, "" on a malformed escape
contentTypeFor(path)               → html/css/js/svg mime, else octet-stream
sendText(exchange, status, body, type) → plain-text response with the same defensive headers
escapeJson(text)                   → minimal JSON string escaping for hand-built payloads
nullable(value)                    → "" for null, to keep the hand-built JSON valid
RequestException(status, message)  → carries an HTTP status out of a handler
RequestServices                    → the {employeeService, scheduleStore, attendanceStore} tuple
workflowsFor(exchange)             → WorkflowService bound to the caller's sandbox
```

### D.2 Value objects — accessors only, no logic

```
Employee              requireText / nonNegative (constructor guards)
                      getId getName getDepartment getRole getLocation getStatus
                      getMonthlyBasic getRiceSubsidy getPhoneAllowance getClothingAllowance
AttendanceRecord      getAttendanceId getId getScheduleId getDate getTimeIn getBreakOut getBreakIn getTimeOut
                      getDayType getHoursWorked hasBreak
DemoLeaveRequest      getId getEmployeeId getEmployeeName getLeaveType getStartDate getEndDate
                      getNote getStatus getSubmittedAt getCancelledAt  cancelled(at) → copy
DemoTicket            getId getEmployeeId getEmployeeName getTicketType getQueue getSummary
                      getStatus getSubmittedAt getCancelledAt           cancelled(at) → copy
RawAttendanceSummary  getEmployeeId getEmployeeName getScheduleId hasScheduleMapping
                      getRawRecordCount getFirstRecordDate getLastRecordDate
PayrollRun            id payPeriod periodStart periodEnd rulesetId taxBasis statutoryBasis
                      createdAt payslips
Payslip               33 accessors, incl. nightDifferentialPay and nonTaxableAllowance
                      builder(): employee period ruleset salary premiums benefits
                                 deductions employer result build
PayrollRuleset        accessors for every rate constant: sssEmployeeRate sssEmployerRate
                      sssMinMsc sssMaxMsc sssMscStep sssEcLow sssEcHigh sssEcThreshold
                      sssMpfThreshold phRate phFloor phCeiling pagibigMaxFund
                      pagibigLowThreshold pagibigEmployeeLow pagibigEmployeeHigh
                      pagibigEmployerRate   ·  Period.periodsPerMonth() → 1 | 2
FirestoreException    getStatusCode()
```

### D.3 Validation and encoding helpers

```
AttendanceService   validateRange(from,to)    → 400 when from > to (used by both read paths)
EmployeeService     mutableStore()             → cast or 409 "This employee store is read-only."
                    validatedEmployee(...)     → all fields trimmed/checked before construction
                    requiredDirectoryText(v,f) → non-blank, ≤ 100 chars
                    validStatus(v)             → "Active" | "On leave"
                    searchableText(e)          → "id name dept role location status" lowercased
WorkflowService     requireEmployee(id)        → Employee or 404
                    randomSuffix()             → 12 uppercase hex characters
PayrollService      divideMonths(monthly,d)  → 2-dp rate divisor (20 days / 168 hours)
                    requireNonNegative(v,f)  → per-field guard used by validate()
                    requireIsoDate(v,f)      → real YYYY-MM-DD, round-tripped through LocalDate
FirestoreRestClient requireSafeSegment(s,name) → rejects blank, "/", and over-long segments
                    pathEncode(s) queryEncode(s)
                    firestoreError(status, body) → FirestoreException with a mapped HTTP status
FirestoreEmployeeStore  nullToEmpty(v) stringField(v) amountOrNull(v)
FirestoreWorkflowStore  leaveFields / ticketFields (typed document bodies)
                        leaveFromDocument / ticketFromDocument (typed reads)
                        stringField timestampField dateTimeValue fields documentId
FirestoreAttendanceStore   stringValue integerValue
InMemoryAttendanceStore    enforceUniqueScheduleDate(scheduleId, date) → refuses a duplicate row
InMemoryEmployeeStore      addFixture(employee)                        → test seeding only
DemoSeedService            scheduleMappingFields / attendanceFields / string / integer
                           SeedResult.getEmployeesCreated / getScheduleMappingsCreated /
                           getAttendanceCreated                       → the three seed counters
```

### D.4 Browser plumbing in `app.js`

```
setAuthError(message)              → the red line under the sign-in card
fetchSandboxSnapshot()             → parallel load of health + employees + workflows + per-employee
                                    attendance, used by the cross-record search panel
resetEmployeeEditor()              → clear the create/edit form
resetSessionData()                 → drop every cached list when the user signs out
makeCell(content)                  → <td> wrapper used by every table
downloadCsvFile / downloadJsonFile → Blob + object URL + click
fetchSandboxSearchResults          → cross-record search panel
renderSandboxSearchResults(...)    → its results list
showTableMessage(text)             → the "no rows" / "loading" / error row
showWorkflowTableMessage(text)     → same, for the two workflow tables
showAttendanceMessage(text)        → same, for attendance
makeWorkflowStatus(item)           → the Pending review / Cancelled badge
makeWorkflowDetails(item)          → the two-line detail cell
makeCancelButton(item, kind)       → the cancel control, disabled unless cancellable
selectLeaveCalendarDate(date)      → start/end selection from the calendar
selectAttendanceCalendarDate(date) → filter the table to one day
clearAttendanceCalendarSelection() → drop that filter
applyWorkflowItem(collection,item,storage) → optimistic insert/replace after a successful write
setWorkflowStatus(id, text, isError)       → inline form status line
setWorkflowStorageMode(mode)               → the "stored in your sandbox" banner
renderWorkflowSummary()            → request/ticket counts by status
renderEmployeeRecordSummary()      → one employee's attendance + request + ticket totals
renderRawAttendanceSummary()       → "N rows, first…last"
renderAttendanceReport()           → the per-employee coverage table with unmapped rows flagged
renderDirectoryViews()             → directory summary + search + export wiring
resetPayrollState()                → clear payslip, remittance and history after a sign-out
populateAttendanceEmployees()      → the attendance employee picker
populateWorkflowEmployees()        → the request employee picker
```

### D.5 Browser pure modules in full

```
attendance-calendar.js   dateKey(date) → "YYYY-MM-DD"
                         isIsoDate(v)  → strict real-date check
                         buildAttendanceCalendar(rows) → month matrix with marked days
leave-calendar.js        dateKey / validIsoDate / buildLeaveCalendar(range)
attendance-record-sort.js          sortAttendanceRecords(rows, {field, direction})
attendance-coverage-filter.js      filterAttendanceCoverage(rows, {query, mappedOnly, withRecords})
attendance-coverage-sort.js        sortAttendanceCoverage(rows, {field, direction})
directory-sort.js                  sortDirectoryEntries(employees, {field, direction})
directory-summary.js               directoryLabelSummaryRows(employees) → status counts
employee-record-summary.js         employeeRecordSummary(employee, records, leaves, tickets)
employee-record-summary-view.js    filterAndSortEmployeeRecordSummary(...)
workflow-filter.js                 filterWorkflowItems(items, {status, type, query})
workflow-sort.js                   sortWorkflowItems(items, {field, direction})
workflow-summary.js                workflowSummaryRows(items) → counts per status
sandbox-search.js                  hasQuery(q) → boolean
                                   searchSandboxRecords(records, q) → matches across every field
csv-export.js                      validateColumns / csvCell / buildCsv (RFC 4180 escaping)
workspace-export.js                buildWorkspaceExport(state) → one whole-sandbox JSON object
workflow-demo.js                   createWorkflowState() / requireState() / findEmployee()
                                   isValidDate() / cleanOptionalText() / dateStamp()
                                   submitLeaveRequest() / cancelLeaveRequest()
                                   submitTicket() / cancelTicket()
                                   # browser-only twin of §8 for the fixture demo
```

---

## Appendix E — reading order if you are new to this

1. **§1** for the shape of the system, then **§13** to see what you can click.
2. **§9.2** and **§9.4** — the payroll calculation and the worked example. Everything else exists to
   feed or display these numbers.
3. **§9.8 / §9.10** — how a payslip becomes an unchangeable record.
4. **§4.2** — the one function that decides which data you can see.
5. **§12** — so you know what you will *not* find.

---

## Appendix A — object shapes at a glance

```
Employee        id, name, department, role, location, status,
                monthlyBasic?, riceSubsidy?, phoneAllowance?, clothingAllowance?
AttendanceRecord attendanceId, scheduleId, date, timeIn, breakOut?, breakIn?, timeOut,
                dayType, hoursWorked                      # hoursWorked is STORED
DemoLeaveRequest id(LV-), employeeId, employeeName, leaveType, startDate, endDate, note,
                status, submittedAt, cancelledAt
DemoTicket       id(TK-), employeeId, employeeName, ticketType, queue, summary,
                status, submittedAt, cancelledAt
Payslip          33 fields: identity(4), period(3), rulesetId, salary(4), premiums(5),
                benefits(6), deductions(5), employer(5), taxableIncome, takeHomePay
PayrollRun      id(PR-), payPeriod, periodStart, periodEnd, rulesetId, taxBasis,
                statutoryBasis, createdAt, payslips[]
```

## Appendix B — identifier formats

| Prefix | Example | Chosen by |
|---|---|---|
| Employee number | `10015`, and `10035` for the next one created | server: next free five-digit number after the highest in the directory |
| `LV-` leave | `LV-4C1D9A0B77E2` | server |
| `TK-` ticket | `TK-88F0C3A91DE4` | server |
| `PR-` payroll run | `PR-20260102-77BC9231` | server, period + 8 hex |

A client cannot choose or inject any of these. `AppServerIntegrationTest` asserts that a request
body carrying its own `id` is rejected with `400`.

## Appendix C — the invariants that make the product safe

1. **Fail closed.** No Firebase config → the process refuses to start (only loopback test mode may
   run fixture data).
2. **Token-scoped.** `userCollection()` is the sole path builder, and the `uid` comes from a
   validated header that the browser cannot forge into another user's space.
3. **Rules, not code, authorise.** Firestore Security Rules are the boundary; Java only forwards.
4. **Immutable money.** Runs are create + read only; `DELETE` is 405 and the rules forbid update.
5. **No silent defaults.** Unknown JSON fields are rejected; omitted pay fields keep stored values;
   an uncovered statutory year is refused instead of borrowed.
6. **One rounding rule.** `Money.round` (2 dp, half-up) is the only place a peso is rounded.
7. **The browser never computes money.** It formats strings the server produced.
8. **Refuse impossible payslips.** A negative take-home aborts the calculation with a 400.

---

*Generated 2026-11-05 from source: 36 Java classes (187 methods with a body + one-line accessors),
18 browser modules / 106 named functions. Function-name coverage was checked mechanically: every named
function in the Java and browser source appears in this document.*
*Companion documents: [api-contract.md](api-contract.md) (wire format), [payroll-ruleset-and-golden-case.md](payroll-ruleset-and-golden-case.md) (statutory evidence), [build-status-no-local-no-temp.md](build-status-no-local-no-temp.md) (what changed and when).*