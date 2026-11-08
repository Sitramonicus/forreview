# Function-by-function pseudocode, current-behavior inventory, and handoff synthesis

**Status date:** 2026-11-05
**Scope:** every named function in the shipped Java backend and in the shipped browser JavaScript, with observable pseudocode, status, and the exact gaps/formulas/policy decisions still outstanding.
**Superseded for navigation:** this 1,652-line document predates the payroll engine and the payroll
HTTP tests. For a current, complete walkthrough of every feature, function and subfunction use
[`docs/full-pseudocode-inventory.md`](full-pseudocode-inventory.md). This document is still the record of
the open policy/formula decisions (D-01 … D-32).

**Companion documents:** `docs/build-status-no-local-no-temp.md` (current boundaries), `docs/api-contract.md` (route contract), `docs/source-grounding-and-open-rules.md` and `docs/candidate-policy-review.md` (unresolved rules, research only).

> **What this document is not.** It is not a design for a payroll engine, and it does not authorise any calculation. Every payroll-shaped function below is either absent or explicitly a count/label/read. The "formulas you must supply" section lists decisions only you can make; nothing in it is switched on.

---

## 0. How to read this

Legend used on every entry:

| Tag | Meaning |
|---|---|
| **[LIVE]** | Runs in the Firebase-signed-in product path. |
| **[READ-ONLY]** | Returns stored data; derives no business meaning (no status, hours, or pay). |
| **[VALIDATION]** | Pure input/format check. Rejects bad input; produces no business result. |
| **[FIXTURE]** | Only reachable in the isolated loopback test mode or as seed material. Not a product feature. |
| **[PROPOSED]** | Exists in the GPT handoff package only. Not in this repo. Do not assume it runs. |
| **[GAP]** | Behaviour that does not exist yet and needs your policy decision before it can. |

Method convention: `Name(args) -> result` followed by numbered steps. Steps describe what the code actually does, not what it was meant to do. Where the code and the doc comment disagree, the code wins and the discrepancy is called out.

**Source footprint measured from this workspace:** 25 Java files / 1,926 lines / 166 named Java methods; `web/app.js` 1,878 lines with 53 named functions plus 73 `addEventListener` registrations; 16 further browser modules / 537 lines.

---

## 1. Synthesising the GPT handoff against what actually exists

### 1.1 The short version

`/home/user/uploads/MOTORPH_AGENT_HANDOFF.md` describes **a different build of the same product idea**. Its own provenance note says so: it is "a locally reconstructed product", "not a byte-for-byte clone, checkout, or verified export". It chose Java 11 + a local JSON file + a loopback-only server, and it deliberately dropped Firebase:

> "The original LMArena workspace was described as a Java backend and browser UI with Firebase Email/Password Authentication plus user-scoped Firestore REST storage. That mode is **not in this reconstructed package**. The local build is intentionally self-contained and uses JSON. Do not say Firebase is connected in this ZIP."

The workspace you are actually holding is the Firebase-backed application you asked for: real sign-in, per-user Firestore sandboxes, Firestore Rules as the authorization boundary, and no local or temporary product mode. So the handoff is **not a patch to apply and not a newer version of this repo**. It is a parallel implementation with useful ideas, a useful checklist, and a payroll proposal that directly contradicts your standing "no locals, no temps" instruction.

### 1.2 Side-by-side

| Dimension | GPT handoff package | This workspace | Standing decision |
|---|---|---|---|
| Persistence | `data/demo-data.json` on disk | Firestore REST under `users/{uid}/demo*` | Keep Firestore. Its JSON file and `POST /api/demo/reset` are **[PROPOSED]** only. |
| Auth | None in the package ("Do not say Firebase is connected") | Firebase Email/Password; server requires `Authorization: Bearer` + `X-Firebase-UID`; Rules authorise | Keep Firebase. |
| Default bind | `127.0.0.1:8080` | `0.0.0.0` with `FIREBASE_PROJECT_ID` required; loopback local only under `MOTORPH_TEST_MODE=true` | Keep fail-closed startup. |
| Payroll | **Active** non-persisted monthly estimate, profile `PH-STATUTORY-2026-MONTHLY-PROVISIONAL` | Payroll view renders a blocking gate; **zero** calculation code | Your instruction wins: no temporary calculator. Its formula is **candidate research**, not shipped. |
| Attendance writes | `POST /api/attendance` creates a local synthetic row | Route returns **405**; writer, mutable store, and form deleted | Keep removed until a durable, authorised capture design exists. |
| Schedule link | Implicit in the JSON fixtures | Owner-scoped `demoEmployeeSchedules` docs, read-only, seeded once | Keep; assignment lifecycle is still a **[GAP]**. |
| Leave / tickets | `/api/leave-requests`, `/api/tickets` + cancel | `/api/workflows/leave`, `/api/workflows/tickets` + `/{id}/cancel` | Keep current routes (already documented and tested). |
| Extra read routes | `/api/team`, `/api/hr/employees`, `/api/payroll/rules` | None — Team and HR views are client-side filters over `/api/employees` | No new route needed yet; RBAC is the real blocker. |
| Health payload | mode + storage + payroll profile + production marker | `{status, mode}` only | Adding a `payroll` block would require a payroll ruleset to describe; defer. |
| Tests | `SmokeTest.java` + python html-binding check | 182 Java checks, 51 web tests, Playwright E2E, startup guard | Keep current suite. |
| Self-description | "Treat this package's source as the current runnable baseline" | This repo is the baseline | **Reject.** Its baseline claim cannot override your workspace. |

### 1.3 The one instruction in the handoff you should explicitly overrule

Handoff §0.3 says:

> "The user has explicitly overridden an earlier overly cautious pause on payroll calculation. Do not disable or freeze the payroll module."

That sentence was written against a different conversation state and a package that shipped a calculator. It is not compatible with your current instruction set, which keeps payroll gated and bans temporary features. **Treat it as stale context, not permission.** Nothing in the current repo was changed on the strength of it.

### 1.4 What is genuinely worth taking from it

These are the parts I would carry forward, and how each maps onto this repo:

| Take | Why it is good | Where it would land here |
|---|---|---|
| Five independent statutory **input bases** (`cashPay`, `sssCompensation`, `philHealthMbs`, `pagibigFundSalary`, `taxableCompensation`) instead of one salary number | Stops the silent assumption that every agency uses the same base — the single most common payroll-modelling error | Future `PayrollRunService` input contract. Answers "what do I need to supply" cleanly. |
| Versioned ruleset with an explicit profile ID and **fail-closed outside covered periods** | A calculation is only meaningful if you can state which table produced it | `PayrollRuleset` with `effectiveFrom`/`effectiveTo`, `rulesetVersion` stored on every run snapshot. |
| `BigDecimal`, `HALF_UP`, 2 dp, reject negative bases and negative net | Removes floating-point centavo drift before it exists | Mandatory for the future engine; already the discipline in `docs/candidate-policy-review.md`. |
| Bracket comparison against the **next whole-peso lower bound** (e.g. `< ₱33,333`, so ₱33,332.50 stays in the lower band) | Precise, testable, and a real source of off-by-one-centavo bugs | Bracket table plus boundary golden cases. |
| Golden worked examples stated to the centavo | Turns "does it work?" into a checkable assertion | `docs/acceptance-baseline.md` style table, added when a ruleset is approved. |
| Its §12 dependency ordering (schedule → hours → leave → tickets → HR → payroll → security → QA) | Correct dependency chain; matches what this repo's build-status already says | Already the shape of `docs/build-status-no-local-no-temp.md` "next slices". |
| Its feature inventory matrix (§10) | A ready-made completeness checklist to audit against | Use as the gap checklist in §7 below. |
| CSV formula-injection prefixing (`'` before `= + - @`, including whitespace-led) | Small, real, easy to miss | **Already implemented** in `web/csv-export.js:csvCell`. |

### 1.5 What must not be merged

- Its local JSON store, `POST /api/demo/reset`, and "local-only raw record creation".
- Its active payroll preview and any of its computed sample figures (₱22,611.20 net, ₱28,355.00 employer cost, etc.). Those numbers are artefacts of *its* assumptions: monthly period, contributions deducted **before** withholding, all five bases equal.
- Its class names and routes as statements about this repo. Nothing named `ph.motorph.ms2.*` exists here.
- Its `61–62%` peer-parity figure — it labels it a subjective inventory estimate, not coverage.

---

## 2. The system as built

### 2.1 Modes

```
MODE = firebase-user-sandbox   when FIREBASE_PROJECT_ID is set
MODE = local-synthetic-demo    when it is blank AND MOTORPH_TEST_MODE=true AND host is loopback
otherwise                      startup throws; the browser shows a setup error instead of the workspace
```

`firebase-user-sandbox` is the only supported deployment. `local-synthetic-demo` exists so tests can exercise the HTTP and UI contract without writing to a live project; it is not a product mode.

### 2.2 Route surface actually mounted

| Method | Path | Behaviour |
|---|---|---|
| GET | `/api/health` | `{status, mode}`; 405 otherwise |
| GET | `/api/employees?q=` | search + `{items,total,matched}` |
| GET | `/api/employees/{id}` | `{item}` or 404 |
| POST | `/api/employees` | create allowlisted synthetic record; 201 |
| PUT | `/api/employees/{id}` | update the same six fields; 200 |
| GET | `/api/attendance?employeeId=&from=&to=` | raw rows, `hoursCalculated:false` |
| POST | `/api/attendance` | **405** (deliberately disabled) |
| GET | `/api/reports/attendance?from=&to=` | per-employee raw counts + date coverage |
| GET | `/api/workflows` | `{leaveRequests,tickets,storage}` |
| POST | `/api/workflows/leave` | create; 201 |
| POST | `/api/workflows/tickets` | create; 201 |
| POST | `/api/workflows/leave/{id}/cancel` | cancel pending; 200 |
| POST | `/api/workflows/tickets/{id}/cancel` | cancel open; 200 |
| POST | `/api/demo/seed` | seeds fictional fixtures into the caller's sandbox |
| GET/HEAD | `/` and static assets | CSP + `nosniff` + `no-store` |

Absent by design: payroll, team, HR, delete, import, attendance write, schedule write.

### 2.3 Firestore layout (per signed-in user)

```
users/{uid}/demoEmployees/{employeeId}          id,name,department,role,location,status
users/{uid}/demoEmployeeSchedules/{employeeId}  employeeId,scheduleId
users/{uid}/demoAttendance/{attendanceId}       attendanceId,scheduleId,date,timeIn,timeOut,dayType
users/{uid}/demoLeaveRequests/{LV-…}            …status,submittedAt,cancelledAt
users/{uid}/demoTickets/{TK-…}                  …status,submittedAt,cancelledAt
```

`demoEmployeeSchedules` is the only link between a person and attendance rows. There is no schedule definition document — `scheduleId` is an opaque integer copied from the fixture (101/102/103). **That is the root of most of the missing behaviour below.**

---

## 3. Startup, mode selection, and request plumbing (Java)

### 3.1 `AppServer`

**`AppServer()`** — reads `FIREBASE_PROJECT_ID` from the environment and `WEB_ROOT` (default `web`), delegates to the 3-arg constructor. **[LIVE]**

**`AppServer(String firebaseProjectId, Path webRoot)`** — delegates with `firestoreApiRoot = null` (i.e. real Google endpoint). **[LIVE]**

**`AppServer(String firebaseProjectId, Path webRoot, String firestoreApiRoot)`** **[LIVE]**
```
1. normalise projectId (trim; null -> "")
2. if projectId is blank:
       build the four in-memory fixtures (employee, schedule, attendance, workflow)
   else:
       null all four out — there is no process-memory fallback in cloud mode
3. reject a null webRoot
4. store webRoot as absolute+normalised
5. store firestoreApiRoot (blank -> null) — package-private override used only by HTTP contract tests
```
Note the constructor does **not** enforce the Firebase requirement. Only `main` does. A test or future embedding could still construct a local server; that is intentional and documented in `build-status-no-local-no-temp.md`.

**`main(String[])`** **[LIVE]**
```
1. projectId  <- env FIREBASE_PROJECT_ID (default "")
2. testMode   <- env MOTORPH_TEST_MODE == "true"
3. port       <- env PORT (default 8080); host <- env HOST (default 0.0.0.0)
4. loopbackOnly <- host in {127.0.0.1, localhost, ::1}
5. if projectId is blank AND (not testMode OR not loopbackOnly):
       throw "FIREBASE_PROJECT_ID is required. Local-memory runtime is disabled; isolated tests must bind to loopback."
6. construct AppServer, start(host, port), register shutdown hook -> stop()
```
This is the fail-closed gate proven by the test suite's startup assertion.

**`start(String host, int port) -> HttpServer`** **[LIVE]**
```
1. refuse if already started
2. HttpServer.create(InetSocketAddress(host,port), backlog 0)
3. mount contexts: /api/health, /api/employees, /api/attendance, /api/reports/attendance,
                   /api/workflows, /api/demo/seed, then "/" for static files
4. executor = fixed pool of 8; server.start()
5. print listening address, mode sentence, and served directory
6. return server
```
Contexts are prefix matches, so `/api/workflows/leave` is handled by `handleWorkflows`, which splits the tail itself.

**`stop()`** — `server.stop(0)` and `executor.shutdownNow()`; nulls both fields. **[LIVE]**

**`handleHealth(exchange)`** **[LIVE]**
```
1. if method != GET -> 405 {"error":"Method not allowed"}
2. mode <- projectId blank ? "local-synthetic-demo" : "firebase-user-sandbox"
3. 200 {"status":"ok","mode":mode}
```
No storage, ruleset, or version fields. If you later want the UI to display "which payroll ruleset is active", this is the natural place — but only once one exists.

**`handleEmployees(exchange)`** **[LIVE]** — one method serving five route shapes.
```
try:
  if GET and path is /api/employees or /api/employees/:
      q <- queryParameter("q")
      matches <- employeeService.searchEmployees(q)
      total   <- employeeService.listEmployees().size()      // full directory size, not match count
      200 employeesToJson(matches, total)                   // {items[], total, matched}
  if GET and path starts with /api/employees/:
      id <- substring after prefix
      if id blank or contains "/" -> 404
      employee <- employeeService.findEmployee(id) or 404
      200 {"item": …}
  if POST and path == /api/employees:
      body <- readJsonBody()                                // requires JSON content type, ≤8 KiB
      requireOnlyKeys(name, department, role, location, status)
      created <- employeeService.createEmployee(all five requiredString fields)
      201 {"item": …}
  if PUT and path starts with /api/employees/:
      id <- substring after prefix; blank or "/" -> IllegalArgumentException -> 400
      body <- readJsonBody(); requireOnlyKeys(same five)
      updated <- employeeService.updateEmployee(id, five requiredString fields)
      200 {"item": …}
  else 405
catch RequestException      -> its status code + {"error": message}
catch IllegalArgumentException -> 400
catch NoSuchElementException   -> 404
catch IllegalStateException    -> 409
catch FirestoreException       -> sendApiError (mapped status, safe message)
```
Gaps: no DELETE, no pagination, no `If-Match`/optimistic concurrency, no audit of who changed a record. `total` is computed by a second full directory read.

**`handleAttendance(exchange)`** **[LIVE][READ-ONLY]**
```
1. method != GET -> 405          // this is what makes POST /api/attendance 405
2. employeeId <- queryParameter("employeeId"); blank -> 400 "employeeId is required"
3. from, to <- parseOptionalDate(...)   // null when blank; LocalDate.parse otherwise
4. services <- servicesFor(exchange)
5. attendanceService <- new AttendanceService(employeeService, scheduleStore, attendanceStore)
6. records <- attendanceService.findForEmployee(employeeId, from, to)
7. 200 attendanceToJson(records)        // {items[], count, hoursCalculated:false}
catch DateTimeParseException -> 400 "Dates must use YYYY-MM-DD"
catch IllegalArgumentException -> 400 ; NoSuchElementException -> 404 ; FirestoreException -> mapped
```

**`handleAttendanceReport(exchange)`** **[LIVE][READ-ONLY]**
```
1. method != GET -> 405
2. from, to <- parseOptionalDate(query)
3. summaries <- AttendanceService.summarizeRawRecords(from, to)
4. 200 attendanceSummaryToJson(summaries, from, to)
        {items[{employeeId,employeeName,scheduleId,hasScheduleMapping,rawRecordCount,firstRecordDate,lastRecordDate}],
         count, from, to, hoursCalculated:false}
```

**`handleWorkflows(exchange)`** **[LIVE]**
```
try:
  if GET and path == /api/workflows:
      service <- workflowsFor(exchange)
      200 {leaveRequests[], tickets[], storage: "user-scoped-firestore" | "local-process-memory"}
  if method != POST -> 405
  service <- workflowsFor(exchange)
  if path == /api/workflows/leave:
      body <- readJsonBody(); requireOnlyKeys(employeeId,leaveType,startDate,endDate,note)
      201 {"item": leaveToJson(service.createLeaveRequest(...)), "storage": …}
  if path == /api/workflows/tickets:
      body <- readJsonBody(); requireOnlyKeys(employeeId,ticketType,summary)
      201 {"item": ticketToJson(service.createTicket(...)), "storage": …}
  segments <- tail.split("/")
  if segments.length == 3 and segments[2] == "cancel":
      if segments[0] == "leave"   -> 200 cancelLeaveRequest(segments[1])
      if segments[0] == "tickets" -> 200 cancelTicket(segments[1])
  404 "Workflow endpoint not found"
same catch ladder as employees
```
Gaps: no approve/reject/assign/comment route, no list-by-employee server-side query (the client filters the full list), no idempotency key.

**`workflowsFor(exchange) -> WorkflowService`**
```
if projectId blank: return new WorkflowService(localEmployeeService, localWorkflowStore)
client   <- firestoreClientFor(exchange)                 // throws 401 if headers missing
employees<- new EmployeeService(new FirestoreEmployeeStore(client))
service  <- new WorkflowService(employees, new FirestoreWorkflowStore(client))
```
A fresh client and stores are constructed per request; any per-store cache dies with the request.

**`readJsonBody(exchange) -> JsonObject`** **[VALIDATION]**
```
1. Content-Type must exist and start with application/json  -> else 415
2. read at most 8,193 bytes; if > 8,192 -> 413
3. parse; must be a JSON object -> else 400 (non-object) or 400 (invalid JSON)
```
No schema/type validation here; that is per-field, later, via `requiredString`/`optionalString`.

**`requireOnlyKeys(body, allowed...)`** **[VALIDATION]** — throws 400-class `IllegalArgumentException` naming the first unexpected key. This is the allowlist that stops a client inventing fields such as `salary`.

**`requiredString(body, field) -> String`** **[VALIDATION]** — must be a JSON string primitive; otherwise `"<field> is required and must be text."` Returns raw (untrimmed) text; trimming happens downstream.

**`optionalString(body, field) -> String`** **[VALIDATION]** — missing or JSON null → `""`; must otherwise be a string primitive.

**`itemToJson(item, storage) -> String`** — `{"item": <object>, "storage": <label>}`.

**`workflowsToJson(leaveRequests, tickets, storage) -> String`** — arrays plus the storage label. No sorting here (ordering comes from the store).

**`leaveToJson(request) -> JsonObject`** — ten properties: `id, employeeId, employeeName, leaveType, startDate, endDate, note, status, submittedAt, cancelledAt`.

**`ticketToJson(ticket) -> JsonObject`** — nine properties: `id, employeeId, employeeName, ticketType, queue, summary, status, submittedAt, cancelledAt`.

**`handleSeedDemo(exchange)`** **[LIVE]**
```
1. method != POST -> 405
2. projectId blank -> 400 "Firebase is not configured for this server."
3. result <- DemoSeedService(client).seedMissingSyntheticRecords()
4. 200 {employeesCreated, scheduleMappingsCreated, attendanceCreated, syntheticOnly:true}
```
Writes only fictional fixture values. It cannot be re-run destructively: existing document IDs return `ALREADY_EXISTS` and are counted as not created.

**`servicesFor(exchange) -> RequestServices`**
```
if projectId blank: (localEmployeeService, localScheduleStore, localAttendanceStore)
else: client <- firestoreClientFor(exchange)
      (new EmployeeService(new FirestoreEmployeeStore(client)),
       new FirestoreEmployeeScheduleStore(client),
       new FirestoreAttendanceStore(client))
```

**`firestoreClientFor(exchange) -> FirestoreRestClient`** **[LIVE] — the security boundary, read carefully**
```
1. authorization <- header "Authorization"; must start with "Bearer "
2. uid           <- header "X-Firebase-UID"; must be non-blank
3. else FirestoreException("Sign in with your Firebase account to continue.", 401)
4. token <- authorization minus "Bearer ", trimmed
5. reject if token > 8,192 chars, uid > 128 chars, or uid contains "/"   -> 401
6. new FirestoreRestClient(projectId, uid, token, optional override)
```
**The Java server never verifies the token itself.** It cannot mint data access either: the token is forwarded to Firestore, which validates it, and the path is derived from `uid`. A forged `X-Firebase-UID` therefore produces a request that Firestore Rules reject (the path would not match `request.auth.uid`). The design fails closed, but it does mean every authorization decision lives in Rules, not in Java. If you ever add a privileged read (payroll registers, other employees' records), this pattern is no longer sufficient — you would need server-side token verification and a role model. Flagged as **[GAP] OPS-3**.

**`parseOptionalDate(String) -> LocalDate|null`** **[VALIDATION]** — blank/null → null, else `LocalDate.parse` (throws `DateTimeParseException` → 400).

**`attendanceToJson(records) -> String`** — hand-built JSON: `{items:[{attendanceId,scheduleId,date,timeIn,timeOut,dayType}], count, hoursCalculated:false}`.

**`attendanceRecordToJson(record) -> String`** — one row; `dayType` is escaped, the numeric/date/time fields are trusted shapes.

**`attendanceSummaryToJson(summaries, from, to) -> String`** — rows plus `count`, echoed `from`/`to` (or null), and `hoursCalculated:false`.

**`handleStaticFiles(exchange)`** **[LIVE]**
```
1. HEAD allowed; anything else -> 405 text
2. path "/" -> "/index.html"
3. requested <- webRoot.resolve(path minus leading "/").normalize()
4. if not under webRoot OR not a regular file -> 404        // traversal guard
5. headers: X-Content-Type-Options nosniff, Cache-Control no-store,
            CSP default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:;
                connect-src 'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com;
                object-src 'none'; base-uri 'self',
            Referrer-Policy no-referrer, Permissions-Policy camera/mic/geo off
6. HEAD -> Content-Length only; GET -> write bytes
```
The CSP `connect-src` allowlist is why Firebase Auth works from the app and why an arbitrary external API would not.

**`employeesToJson(employees, total) -> String`** — `{items[], total, matched}`.

**`employeeToJson(employee) -> String`** — the six display fields, escaped.

**`queryParameter(rawQuery, target) -> String`** **[VALIDATION]** — manual split on `&` and first `=`; URL-decodes key and value; returns `""` for missing. Note `""` is indistinguishable from an absent parameter, which is why `handleAttendance` tests `trim().isEmpty()`.

**`decode(value) -> String`** — `URLDecoder.decode(...,"UTF-8")`, returning `""` on failure rather than throwing.

**`contentTypeFor(path) -> String`** — html/css/js/svg mapped to explicit types with UTF-8; everything else `application/octet-stream`.

**`escapeJson(value) -> String`** — escapes `\ " \n \r \t` only. Control characters below 0x20 other than those are not escaped; acceptable for the current field set, would need hardening before free-text payroll remarks.

**`sendApiError(exchange, exception)`** — clamps status into 400–599 (default 500) and emits `{"error": …}`.

**`sendJson(exchange, status, body)`** — delegates to `sendText` with `application/json; charset=utf-8`.

**`sendText(exchange, status, type, body)`** — sets content type, `nosniff`, `no-store`, then writes bytes.

**`RequestException(int statusCode, String message)`** — checked-at-catch-time carrier for 400/413/415-class errors.

**`RequestServices(employeeService, scheduleStore, attendanceStore)`** — value holder for the three services a read path needs.

### 3.2 `FirestoreException`

**`FirestoreException(String message, int statusCode)`** — carries a message and an HTTP-ish status for the handler ladder. **`getStatusCode()`** returns it.

### 3.3 `FirestoreRestClient` — the only network egress

**`FirestoreRestClient(projectId, userId, idToken)`** — delegates to the 4-arg form with the real API root.

**`FirestoreRestClient(projectId, userId, idToken, apiRoot)`** **[LIVE]**
```
1. projectId, userId must be non-blank, single path segments (no "/")
2. idToken must be non-blank
3. apiRoot must be non-blank; force a trailing "/"
```
The override exists so contract tests can point at a local stub.

**`listDocuments(collectionPath) -> List<JsonObject>`** **[LIVE]**
```
1. pageToken <- ""
2. do:
     url <- documentsUrl(collectionPath) + "?pageSize=1000" + pageToken
     response <- send(GET, url, null, allowExisting=false)
     append every element of response.documents
     pageToken <- response.nextPageToken ? "&pageToken=" + encoded : ""
3. while pageToken non-empty
```
Every read is unbounded: there is no per-user cap or index assumption, and the whole collection is materialised in memory each call.

**`createDocument(collectionPath, documentId, fields) -> boolean`** **[LIVE]**
```
url  <- documentsUrl + "?documentId=" + encode(documentId)
body <- {"fields": fields}
return send(POST, url, body, allowExisting=true).has("name")
```
Returns `false` on `ALREADY_EXISTS` (409, or 400 whose body mentions it) so callers can treat re-seeding as a no-op. No pre-existence read; it relies on the create conflict.

**`updateDocument(collectionPath, documentId, fields, updateMask) -> boolean`** **[LIVE]**
```
require fields non-null; require updateMask non-empty
url <- documentsUrl + "/" + encode(documentId)
for each field in updateMask: url += "?/&" + "updateMask.fieldPaths=" + encode(field)
return send(PATCH, url, {"fields": fields}, false).has("name")
```
Field-masked PATCH, so unspecified fields are preserved. This is what makes cancel safe: only `status` and `cancelledAt` are touched.

**`userCollection(collectionName) -> String`** — `"users/" + userId + "/" + collectionName` with a safe-segment check. Every store goes through this; there is no way to address another user's tree from this class.

**`send(method, url, body, allowExisting) -> JsonObject`** (private) **[LIVE]**
```
1. build request: 20 s timeout, Authorization: Bearer <idToken>, Accept: application/json
2. attach JSON body + Content-Type if body present
3. execute
4. if allowExisting and status in {409,400} and body contains "ALREADY_EXISTS" -> return {}
5. if status outside 200–299 -> firestoreError(status, body)
6. empty body -> {}  else parse as object
catch InterruptedException -> re-interrupt, FirestoreException 503
catch IOException | IllegalArgumentException -> FirestoreException("Could not reach Firestore…", 503)
```

**`documentsUrl(collectionPath) -> String`** — `apiRoot + project/databases/(default)/documents` then each path segment encoded individually, so a collection name cannot smuggle a slash.

**`requireSafeSegment(value,label)`** **[VALIDATION]** — non-blank, no `/`, trimmed.

**`pathEncode(value)`** — `URLEncoder` with `+` → `%20`.

**`queryEncode(value)`** — plain `URLEncoder` (the `+` form is legal in a query string).

**`firestoreError(status, body) -> FirestoreException`** — maps to human text: 401 "Firebase sign-in expired…", 403 "Firestore denied this request. Publish the scoped development rules in docs/firestore.rules, then retry.", 404 "database or requested resource was not found", 400/409 "A demo record could not be created…". Anything else keeps the generic message.

---

## 4. Domain services (Java)

### 4.1 `EmployeeService`

**`EmployeeService(EmployeeStore store)`** — rejects null. Holds only a `store`; all persistence polymorphism lives below this line.

**`listEmployees() -> List<Employee>`** — `store.findAll()`; no sort, no filter. Order is whatever the store returns (Firestore document order for cloud, insertion order in the fixture).

**`findEmployee(String id) -> Optional<Employee>`** **[READ-ONLY]**
```
return store.findById(trim(id))       // null id passes through as null
```

**`createEmployee(name, department, role, location, status) -> Employee`** **[LIVE]**
```
1. mutable <- mutableStore()                      // throws IllegalStateException 409 if read-only
2. id      <- highest five-digit employee number in the directory + 1 (10035, 10036, …)
3. employee<- validatedEmployee(id, name, department, role, location, status)
4. return mutable.create(employee)
```
The server owns the ID; a client cannot choose one. There is no uniqueness pre-check in the cloud path — `FirestoreEmployeeStore.create` relies on the document-ID conflict → 409.

**`updateEmployee(id, name, department, role, location, status) -> Employee`** **[LIVE]**
```
1. safeId <- trim(id)
2. safeId must match  [0-9]{5}                            -> else 400 "Employee ID is invalid."
3. findEmployee(safeId) must exist                        -> else 404
4. mutableStore().update(validatedEmployee(safeId, five fields))
```
Employee numbers follow MotorPH's own five-digit series (10001–10034 from their file, then 10035 onward for new records). Nothing else about the number is editable, and there is no delete.

**`mutableStore() -> MutableEmployeeStore`** (private) — casts or throws `IllegalStateException("This employee store is read-only.")` → HTTP 409.

**`validatedEmployee(id, name, department, role, location, status) -> Employee`** (private) — applies text + status validation below.

**`requiredDirectoryText(value, field) -> String`** **[VALIDATION]** — trim; blank → 400 "@field is required."; > 100 chars → 400.

**`validStatus(value) -> String`** **[VALIDATION]** — exactly `Active` or `On leave`; anything else → 400 `"Status must be Active or On leave."`. **This is a stored label only** — it drives no access decision and no pay treatment.

**`searchEmployees(query) -> List<Employee>`** **[READ-ONLY]**
```
1. query blank -> store.findAll()
2. needle <- lowercase(trim(query))
3. return employees where searchableText(employee).contains(needle)
```
Case-insensitive substring over all six fields. No tokenisation, no ranking, no fuzzy match; it also cannot search attendance, leave, or tickets (the client does that separately in `searchSandboxRecords`).

**`searchableText(employee) -> String`** (private) — concatenates id, name, department, role, location, status separated by spaces, lowercased. Because fields are concatenated, a query can match across a boundary (e.g. "active" plus a trailing space).

### 4.2 `AttendanceService`

Constructor rejects any null collaborator.

**`findForEmployee(employeeId, from, to) -> List<AttendanceRecord>`** **[LIVE][READ-ONLY]**
```
1. employee must exist                      -> else NoSuchElementException -> 404
2. validateRange(from, to)                  -> from > to is 400
3. scheduleId <- scheduleStore.findScheduleIdForEmployee(trim(employeeId))
4. no mapping -> return emptyList()          // deliberately not "absent", not an error
5. rows <- attendanceStore.findByScheduleId(scheduleId)
6. keep rows where (from is null or date >= from) and (to is null or date <= to)
7. return — no sort is applied here
```
Three deliberate silences: no mapping yields `[]`, a mapping with no rows yields `[]`, and neither is distinguished from "employee was absent". The UI says so explicitly; keep it that way until a schedule model exists.

**`summarizeRawRecords(from, to) -> List<RawAttendanceSummary>`** **[LIVE][READ-ONLY]**
```
1. validateRange
2. allRows <- attendanceStore.findAll()
3. for each employee in employeeService.listEmployees():
       schedule <- mapping for employee
       if none: summary(employeeId, name, null, 0, "", "")
       else:
           matched <- allRows where scheduleId == mapping and date within [from,to]
           first   <- min(date) as string, or ""
           last    <- max(date) as string, or ""
           summary(employeeId, name, scheduleId, matched.size, first, last)
4. sort summaries by employeeId (string order)
```
Cost: `findAll()` once, then an O(employees × rows) scan in memory. Fine at fixture scale; needs a query or an index if this ever holds a real directory.

**`validateRange(from, to)`** (private) **[VALIDATION]** — `from > to` → 400 `"from date must be on or before to date"`.

### 4.3 `WorkflowService`

Constants: `MAX_TEXT_LENGTH = 240`; `LEAVE_TYPES = [Vacation, Medical, Personal, Unpaid]`; `TICKET_QUEUES` = Overtime request→Team Lead, Leave request→Team Lead, Timesheet correction→Team Lead, Payroll inquiry→Payroll Staff, HR inquiry→HR, Technical support→IT Admin.

**`WorkflowService(employeeService, store)`** — rejects null dependencies.

**`listLeaveRequests() / listTickets()`** — pass straight through to the store (which sorts newest-first).

**`createLeaveRequest(employeeId, leaveType, startDate, endDate, note) -> DemoLeaveRequest`** **[LIVE]**
```
1. employee <- requireEmployee(employeeId)
2. leaveType must be in LEAVE_TYPES                        -> else 400 "Choose a valid leave type."
3. start <- parseDate(startDate,"start date"); end <- parseDate(endDate,"end date")
4. start > end -> 400 "The start date must be on or before the end date."
5. note <- optionalText(note, "Note")                      // ≤240, may be blank
6. id   <- "LV-" + 12 uppercase hex
7. store.createLeaveRequest(new DemoLeaveRequest(id, employeeId, employeeName, leaveType,
       start, end, note, "Pending review", Instant.now(), ""))
```
Status is hard-coded `Pending review` — there is no approval state machine. `employeeName` is denormalised into the record at submission time, so a later directory rename does not update history.

**`cancelLeaveRequest(id) -> DemoLeaveRequest`**
```
1. requireId(id, "LV-")            // must match LV-[A-F0-9]{12}
2. store.cancelLeaveRequest(id, Instant.now())
```
Ownership is not checked in Java; it is enforced by the Firestore path being under `users/{uid}`. The client also renders Cancel only for `Pending review`.

**`createTicket(employeeId, ticketType, summary) -> DemoTicket`** **[LIVE]**
```
1. employee <- requireEmployee(employeeId)
2. queue <- TICKET_QUEUES[ticketType]; null -> 400 "Choose a valid ticket type."
3. summary <- requiredText(summary, "Message")     // required, ≤240
4. id <- "TK-" + 12 hex; status "Open"; submittedAt now
5. store.createTicket(...)
```
The queue is a **label only**. Nothing is delivered, notified, or assigned.

**`cancelTicket(id)`** — `requireId(id,"TK-")` then `store.cancelTicket(id, now)`.

**`requireEmployee(employeeId) -> Employee`** (private) — required text then `employeeService.findEmployee(...)`; missing → `NoSuchElementException("Choose an employee from your synthetic demo directory.")` → 404.

**`parseDate(value, field) -> LocalDate`** (private) **[VALIDATION]** — required text; `LocalDate.parse`; **round-trip check** (`date.toString().equals(text)`) so `2026-1-5` is rejected; parse failure → 400 `"Enter a valid YYYY-MM-DD <field>."`

**`requiredText / optionalText(value, field)`** (private) **[VALIDATION]** — trim; required rejects blank; both reject > 240.

**`randomSuffix() -> String`** (private) — first 12 chars of a hyphen-stripped UUID, uppercased. Collision probability is negligible but not zero; a collision surfaces as 409 from the create conflict.

**`requireId(id, prefix)`** (private) **[VALIDATION]** — regex `prefix + "[A-F0-9]{12}"`; else 400 "Workflow record ID is invalid." This is also a path-injection guard: an ID like `../x` cannot reach the store.

### 4.4 `DemoSeedService`

**`DemoSeedService(FirestoreRestClient client)`** — rejects null.

**`seedMissingSyntheticRecords() -> SeedResult`** **[LIVE]**
```
1. PREFLIGHT: list users/{uid}/demoEmployeeSchedules
      // done first so an unpublished ruleset fails before any write, avoiding a half-seeded sandbox
2. for each fixture employee:  createDocument(demoEmployees, employeeId, fields) -> count if created
3. for each fixture schedule mapping: createDocument(demoEmployeeSchedules, employeeId, {employeeId,scheduleId})
      -> count if created
4. for scheduleId in [101,102,103]:
       for each fixture attendance row of that schedule:
           createDocument(demoAttendance, str(attendanceId), fields incl. dayType "DEMO") -> count if created
5. return SeedResult(employeesCreated, mappingsCreated, attendanceCreated)
```
Semantics: **create-if-absent**, never update, never delete. Re-running the seed is safe; existing values are never overwritten, so a locally mutated record stays mutated. If step 3 or 4 fails mid-way (e.g. rules deny one collection), earlier writes are already committed — the preflight only covers the *ruleset* class of failure, not partial-write atomicity.

**`employeeFields(employee)`** (private) — six string fields.
**`scheduleMappingFields(employeeId, scheduleId)`** (private) — `employeeId` string, `scheduleId` integer.
**`attendanceFields(record)`** (private) — integers plus ISO date/time strings, `dayType` forced to `"DEMO"` regardless of the fixture value.
**`string(value)` / `integer(value)`** (private) — wrap into Firestore typed-value objects.
**`SeedResult(employeesCreated, scheduleMappingsCreated, attendanceCreated)`** — immutable counts; **`getEmployeesCreated()`**, **`getScheduleMappingsCreated()`**, **`getAttendanceCreated()`**.

---

## 5. Models and value objects

**`Employee(id, name, department, role, location, status)`** — every field passes `requireText` (non-null, non-blank, trimmed); no format rules. Getters: `getId`, `getName`, `getDepartment`, `getRole`, `getLocation`, `getStatus`. `requireText` is private.

**`Employee.requireText(value, field)`** (private) — `Objects.requireNonNull`, blank check, trim.

**`AttendanceRecord(attendanceId, scheduleId, date, timeIn, timeOut, dayType)`**
```
1. attendanceId > 0 ; scheduleId > 0                     -> else IllegalArgumentException
2. date, timeIn, timeOut non-null
3. timeIn must not be after timeOut                      // "per the supplied data dictionary"
4. dayType non-blank, trimmed
```
Consequence worth knowing: **overnight shifts are unrepresentable.** A 22:00–06:00 shift throws at construction. There is no break field, no timezone, no source system, no "as-of" version. Getters `getAttendanceId` … `getDayType`.

**`DemoLeaveRequest(id, employeeId, employeeName, leaveType, startDate, endDate, note, status, submittedAt, cancelledAt)`** — all strings; `cancelledAt` null → `""`. **`cancelled(timestamp)`** returns a copy with `status="Cancelled"` and `cancelledAt=timestamp` (immutable update). Getters for all ten fields.

**`DemoTicket(id, employeeId, employeeName, ticketType, queue, summary, status, submittedAt, cancelledAt)`** — same shape minus `note`/dates. **`cancelled(timestamp)`** returns a copy with `status="Cancelled"`. Nine getters.

**`RawAttendanceSummary(employeeId, employeeName, scheduleId|null, rawRecordCount, firstRecordDate, lastRecordDate)`** — null dates normalised to `""`. `hasScheduleMapping()` is `scheduleId != null` — the single predicate the whole coverage report and UI filter on. Accessors: `getEmployeeId()`, `getEmployeeName()`, `getScheduleId()` (nullable `Integer`), `getRawRecordCount()`, `getFirstRecordDate()`, `getLastRecordDate()`, `hasScheduleMapping()`.

---

## 6. Storage adapters

### 6.1 Interfaces

**`EmployeeStore`** — `findAll()`, `findById(id)`.
**`MutableEmployeeStore extends EmployeeStore`** — adds `create(employee)`, `update(employee)`. The read-only/full split is what lets `EmployeeService` refuse writes without knowing which adapter it has.
**`AttendanceStore`** — `findByScheduleId(int)`, `findAll()`. Read-only by contract; there is no `create`, which is how the attendance writer was removed rather than merely disabled.
**`EmployeeScheduleStore`** — `findScheduleIdForEmployee(employeeId) -> OptionalInt`.
**`WorkflowStore`** — `listLeaveRequests`, `createLeaveRequest`, `cancelLeaveRequest(id, cancelledAt)`, `listTickets`, `createTicket`, `cancelTicket(id, cancelledAt)`.

### 6.2 Firestore adapters

**`FirestoreEmployeeStore(FirestoreRestClient)`** — **[LIVE]**
- **`findAll()`** → reads `demoEmployees`, maps each document.
- **`findById(id)`** → `findAll().stream().filter(...).findFirst()` — a full collection read per lookup. Called once per attendance request and once per workflow creation; acceptable now, an N+1 pattern if the directory grows.
- **`create(employee)`** → `createDocument(demoEmployees, employee.getId(), fields)`; `false` → `IllegalStateException("Employee ID already exists…")` → 409.
- **`update(employee)`** → field-masked PATCH over all six fields; `false` → `NoSuchElementException` → 404.
- **`employeeFields / stringField / fromDocument`** (private/static) — typed-value mapping; `fromDocument` tolerates a missing `fields` object by using an empty one, so a malformed document becomes a blank `Employee` and would throw on construction.
- **`stringValue(fields, name)`** (static, package-visible) — shared typed-value reader used by the attendance and schedule stores.

**`FirestoreAttendanceStore(FirestoreRestClient)`** — **[LIVE]**
- **`findAll()`** → reads all of `demoAttendance` and parses each row.
- **`findByScheduleId(int)`** → filters `findAll()` in memory.
- **`fromDocument`** (private) — `LocalDate.parse`/`LocalTime.parse` on stored strings. A malformed date string throws `DateTimeParseException` inside a read and surfaces as a 500-ish failure, not a skipped row.
- **`integerValue / stringValue`** (private) — typed-value readers.

**`FirestoreEmployeeScheduleStore(FirestoreRestClient)`** — **[LIVE]**
- **`findScheduleIdForEmployee(employeeId)`** → `OptionalInt` from the cached map; blank id → empty.
- **`mappings()`** (private) — lazy, double-checked, cached for the lifetime of the store instance (one request). Reads `demoEmployeeSchedules`; keeps entries where `employeeId` is non-blank **and** `scheduleId > 0`; caches an unmodifiable map.
- **`stringValue / integerValue`** (private) — typed-value readers.

This class is the reason the app no longer guesses: there is no hard-coded `10001 → 101` fallback in the cloud path. An unmapped employee simply has no attendance — which is the normal state for 30 of the 34 people in MotorPH's file, because it has no schedule column.

**`FirestoreWorkflowStore(FirestoreRestClient)`** — **[LIVE]**
- **`listLeaveRequests()`** → read `demoLeaveRequests`, map, sort by `submittedAt` descending.
- **`createLeaveRequest(request)`** → create by generated ID; conflict → 409.
- **`cancelLeaveRequest(id, cancelledAt)`**
  ```
  1. current <- listLeaveRequests().firstWhere(id)      -> else 404 "not found in this user sandbox"
  2. current.status must be "Pending review"            -> else 409 "Only a pending leave request can be cancelled."
  3. PATCH {status:"Cancelled", cancelledAt} masking exactly those two fields
  4. return current.cancelled(cancelledAt)
  ```
- **`listTickets()`**, **`createTicket(ticket)`**, **`cancelTicket(id, cancelledAt)`** — same shape, status must be `Open`.
- **`leaveFields / ticketFields`** (private) — typed values; **note**: create does not write `cancelledAt`, so the field is absent until first cancel.
- **`leaveFromDocument / ticketFromDocument`** (private) — the **document ID from the Firestore `name` path** becomes the record id; that is why `documentId(document)` exists and throws if the name has no tail segment.
- **`fields / documentId / value / dateTimeValue / stringField / timestampField`** (private) — typed-value helpers; `value()` reads `stringValue` or `timestampValue`, returning `""` for anything else.
- Read-modify-write on cancel is not transactional: two concurrent cancels both read `Pending review`, both PATCH, both return success. Harmless for a demo (idempotent end state), but it is **[GAP] CONC-1** for anything that must be auditable.

### 6.3 In-memory fixtures **[FIXTURE]**

Reachable only when `AppServer` is constructed without a project ID: the isolated test mode, or direct unit construction.

**`InMemoryEmployeeStore()`** — **generated** from `uploads/Copy of MotorPH_Employee Data - Employee Details.csv` by `tools/generate_fixtures.py`: all 34 rows, 10001–10034, with the supplied names, positions, basic pay and three allowances. Department is derived from position (the CSV has no department column); status is "Active" for everyone because the CSV's `Status` column is employment type (Regular/Probationary). Do not hand-edit this file.
**`findAll / findById / create / update`** — synchronized map operations; create rejects duplicates, update rejects unknown IDs. **`addFixture(employee)`** (private) — unconditional put used only by the constructor.

**`InMemoryEmployeeScheduleStore()`** — fixed map `10001→101, 10002→102, 10003→103, 10015→104` for the four employees who carry seeded attendance rows, unmodifiable. **`findScheduleIdForEmployee`** returns the entry or empty; the other 30 employees are deliberately unmapped.

**`InMemoryAttendanceStore()`** — six rows: schedule 101 on 2026-10-01, -02, -15; 102 on 2026-10-01, -03; 103 on 2026-10-02; all ~08:00–17:00 with `dayType "DEMO"`. **`findAll`**, **`findByScheduleId`** (synchronized), **`record(...)`** (private row builder), **`enforceUniqueScheduleDate`** (private) — throws if two rows share `scheduleId:date`, preserving the data dictionary's key.

**`InMemoryWorkflowStore()`** — empty maps. **`listLeaveRequests / listTickets`** sorted newest-first; **`createLeaveRequest / createTicket`** reject duplicate IDs; **`cancelLeaveRequest`** requires `Pending review`; **`cancelTicket`** requires `Open`. Both cancel operations are read-modify-write on a single thread-safe method, so they are atomic here (unlike the Firestore path).

---

## 7. Browser: `web/app.js` controller

53 named functions plus 73 event registrations. Module-level state first, because nearly every function reads it.

### 7.1 Module state and configuration

```
firebaseConfig        six VITE_FIREBASE_* values, empty strings when unset
isolatedTestMode      VITE_E2E_TEST_MODE === 'true'
firebaseConfigured    apiKey && projectId && appId, and apiKey does not contain "replace" or "paste_"
firebaseApp/auth      initialised only when configured, otherwise null
state = { employees, allEmployees, totalEmployees, rawAttendance, attendanceReport,
          reportRequestNumber, selected, searchTimer, requestNumber, attendanceRequestNumber,
          user, workflow, visibleLeaveRequests, visibleTickets,
          employeeRecordSummary, visibleEmployeeRecordSummary }
leaveCalendarMonth, leaveDateSelectionStart/End
attendanceCalendarMonth, attendanceCalendarEmployee, attendanceCalendarSelectionStart/End
~60 cached DOM element references (authGate, rows, search, dialog, …)
```
The four `…RequestNumber` counters are monotonic tokens. Every async loader captures the counter *before* awaiting and discards its own result if the counter moved — that is the whole of the stale-response protection. There is no `AbortController`.

### 7.2 Authentication and session

**`setAuthError(message)`** — writes text into `#auth-error` and hides it when the message is empty.

**`friendlyAuthError(error) -> String`** — maps Firebase codes to plain sentences: `operation-not-allowed` (Email/Password disabled in console), `unauthorized-domain` (preview host not allow-listed), `invalid-api-key`, `email-already-in-use`, `weak-password`, `invalid-email`, `too-many-requests`, `invalid-credential`/`wrong-password`/`user-not-found` → "Email or password is incorrect." Falls back to `error.message`, then a generic sentence. Deliberately does **not** distinguish wrong password from unknown user.

**`signIn()`** **[LIVE]**
```
1. clear auth error and reset-status text
2. await signInWithEmailAndPassword(auth, trim(email), password)
3. on failure -> setAuthError(friendlyAuthError(error))
```
Success is not handled here at all — `onAuthStateChanged` does it.

**`createDevAccount()`** **[LIVE]** — same shape with `createUserWithEmailAndPassword`. It creates a real Firebase Auth account with whatever email/password is typed; there is no invitation, tenant, or role assignment.

**`sendPasswordReset()`** **[LIVE]**
```
1. clear error and status
2. if email blank or fails native validity -> reportValidity() and return (no request)
3. disable button
4. await sendPasswordResetEmail(auth, trim(email))
5. status: "If an account exists for that email, Firebase will send a reset link."
6. user-not-found / user-disabled are caught and shown as the SAME neutral sentence (no account enumeration)
7. other errors -> friendlyAuthError
8. re-enable button in finally
```

**`apiHeaders(extra = {}) -> Promise<Object>`** **[LIVE]**
```
headers <- { Accept: application/json, ...extra }
if state.user:
    token <- await state.user.getIdToken()        // refreshed by the SDK when near expiry
    headers.Authorization <- "Bearer " + token
    headers['X-Firebase-UID'] <- state.user.uid
return headers
```
Every API call goes through this; there is no unauthenticated fetch in the product path.

**`resetSessionData()`** — the sign-out / sign-in hygiene sweep. **[LIVE]**
```
1. bump all three request counters (invalidates every in-flight load)
2. clear the debounce timer
3. blank employees/allEmployees/totalEmployees/rawAttendance/attendanceReport
4. reset every report filter, count label, and export button to its empty state
5. reset raw-attendance summary, selection, workflow state, visible lists, employee record summary
6. reset both calendars to today and clear both draft date ranges
7. clear the directory search, all sort/order selects, both attendance date inputs
8. reset both request forms, all leave/ticket filters and sorts
9. reset HR/team filters, sorts, export buttons
10. resetEmployeeEditor()
11. hide the seed button
12. repopulate the employee pickers, then render employees/workflow/directories
13. show the "choose an employee" attendance message and disable exports
```
It is long because it is the only thing preventing one user's rows from remaining on screen after another signs in. Anything you add with per-user data must be cleared here too — that is the main upkeep hazard in this file.

**`showWorkspace(user)`** **[LIVE]**
```
state.user <- user
resetSessionData()
authGate.hidden <- true ; appShell.hidden <- false
sign-out button hidden only when there is no user
operator label  <- user ? "USER SANDBOX" : "TEST FIXTURE"
storage label   <- user ? "User-scoped Firestore" : "Isolated test fixture"
avatar letter   <- first letter of email, or "D"
loadEmployees() ; loadWorkflows()          // not awaited
```

**`showSignIn(message = '')`** — nulls `state.user`, calls `resetSessionData()`, hides the shell, shows the gate, sets the message.

**`friendlyAuthError`, `signIn`, `createDevAccount`, `sendPasswordReset`** are only bound when Firebase is configured (see the bootstrap at the bottom).

### 7.3 Small UI/DOM helpers

**`initials(name) -> String`** — first two whitespace-separated words, first character each, uppercased. `""` for a blank name (no crash).

**`makeCell(content) -> HTMLTableCellElement`** — if `content instanceof Node` it is appended, else set as `textContent`. Everything user-supplied goes through the `textContent` branch, which is why there is no HTML-injection surface in table rendering.

**`downloadCsvFile(filename, columns, records)`**
```
1. csv  <- buildCsv(columns, records)          // throws on malformed columns/rows
2. blob <- Blob(["\uFEFF" + csv], text/csv;charset=utf-8)     // BOM so Excel reads UTF-8
3. object URL, hidden <a download>, click, remove, revoke after 1 s
```

**`downloadJsonFile(filename, data)`** — same pattern with `JSON.stringify(data, null, 2)` and `application/json`.

**`showTableMessage(message)`** — replaces the employee table body with one `colSpan=6` `.table-message` row.

**`showWorkflowTableMessage(tbody, columnCount, message)`** — same for an arbitrary table and column count.

**`showAttendanceMessage(message)`** — same for the attendance table (6 columns).

**`makeWorkflowStatus(status) -> HTMLElement`**
```
badge.className <- "status-pill" + " workflow-cancelled" when status == "Cancelled"
                                  + " leave"  when status is neither Cancelled nor Active
badge.textContent <- status
```
Purely decorative mapping of the stored label. It is also reused for `Active`/`On leave` in the Team and HR tables.

**`makeWorkflowDetails(item, kind) -> HTMLDetailsElement`**
```
1. <details class="workflow-detail"> with a <summary> "Details" and an aria-label naming the record id
2. <dl> fields:
     leave  -> NOTE (or "No note provided"), SUBMITTED (UTC)
     ticket -> MESSAGE (or "No message"), QUEUE LABEL, SUBMITTED (UTC)
3. append CANCELLED (UTC) when cancelledAt is set
```
Read-only expansion of stored values; no approval history exists to show.

**`makeCancelButton(action, item, employeeId, label) -> HTMLButtonElement`**
```
button.type=button ; class "button button-quiet workflow-cancel"
dataset: action ("cancel-leave"/"cancel-ticket"), id (record id), employeeId
textContent <- label ; aria-label "<label> <id> for <employeeName>"
```
Data attributes only — the single delegated listener on each table body reads them in `handleWorkflowCancel`.

**`setWorkflowStatus(id, message, error = false)`** — writes text to a status element and toggles `workflow-error` / `workflow-success` classes.

**`setWorkflowStorageMode(value)`** — writes a label into `#workflow-storage-mode` ("User-scoped Firestore" / "Isolated test fixture memory" / "Rules access required" / "Local API unavailable").

**`renderSandboxSearchResults(results, employees) -> number`**
```
1. clear container
2. nameById <- map of employeeId -> name
3. groups: Employees, Raw attendance, Leave requests, Tickets, each with a formatter
4. total <- sum of group lengths
5. total == 0 -> single sentence: no matches, and a blank result implies no attendance status or eligibility
6. otherwise one <section><h3>Title (n)</h3><ul><li>…</li></ul></section> per non-empty group
7. return total
```
All values formatted with `textContent`. The attendance formatter prints `timeIn–timeOut` and `dayType` verbatim — no duration.

### 7.4 Employee directory

**`renderEmployees()`** **[LIVE]**
```
1. clear tbody
2. visible <- sortDirectoryEntries(state.employees, {field: #employee-sort, direction: #employee-order})
3. empty -> message: "No employee records match that search." when a query is present, else "No demo records are available."
4. per row: initials avatar + name + id | department | role | location | status pill | open button
   - the row itself is clickable -> openEmployee(employee)
   - the open button stops propagation and then opens (so keyboard users have a real button)
   - status pill gets class `leave` unless the stored status lowercases to "active"
5. count <- state.totalEmployees zero-padded to 2; overview count mirrors it
6. result count <- "<n> record(s) shown"
7. export button disabled when nothing is shown
```
`state.totalEmployees` comes from the server's `total` (full directory), while the rows are the filtered `items` — that is why the header can read "03" with one row shown.

**`openEmployee(employee)`** **[LIVE]**
```
1. state.selected <- employee
2. dialog title <- employee.name
3. render six always-present fields: employee id, status, department, role, location, "DATA SOURCE: Synthetic demo record"
4. append a "Linked sandbox records" section with a status line ("Loading linked records…")
5. dialog.showModal()
6. loadEmployeeSandboxRecords(employee.id, recordsSection)     // not awaited
```
No salary, government ID, bank, schedule, or leave-balance field exists to show.

**`loadEmployeeSandboxRecords(employeeId, container)`** **[LIVE][READ-ONLY]**
```
try:
  1. headers <- await apiHeaders()
  2. parallel GET /api/attendance?employeeId=... and GET /api/workflows
  3. parse both; throw on either non-OK
  4. STALE GUARD: if state.selected?.id !== employeeId or container detached -> return silently
  5. attendance <- items (or [])
     leave      <- workflowPayload.leaveRequests filtered by employeeId (client-side)
     tickets    <- workflowPayload.tickets filtered by employeeId
  6. remove the status line; append three groups (heading with count, list, or "No stored rows …")
catch:
  if still current -> write the error into the status line
```
Note it fetches the **entire** workflow collection to filter one employee, and it ignores the date range chosen elsewhere.

**`editSyntheticEmployee(employee)`** **[LIVE]**
```
1. copy the six fields into the editor inputs
2. title "Edit <id>", save button "Save synthetic record", show cancel button
3. scroll the form into view, focus the name input
```
No optimistic-concurrency token: two admins editing the same record silently last-write-wins.

**`populateAttendanceEmployees()`** — rebuilds the attendance employee `<select>` from `state.allEmployees` as `"<id> — <name>"`, preserving the current selection when it still exists. No placeholder, so a selection is always possible once employees load.

**`populateWorkflowEmployees()`** — rebuilds `#leave-employee` and `#ticket-employee` with a leading placeholder (`"Choose synthetic employee"` / `"Load directory first"`) plus the same options, preserving selection.

**`populateWorkflowEmployees` and `populateAttendanceEmployees` are the only bridge between identity and an employee record — and it is a free-choice dropdown.** Nothing binds the signed-in Firebase account to an employee. Selecting "Eli Santos" asserts nothing. This is stated in the UI notice and is **[GAP] ID-1**.

**`renderDirectoryViews()`** **[LIVE][READ-ONLY]** — renders Team and HR views from the same `state.allEmployees`.
```
1. disable summary export when no employees
2. filteredTeam      <- sortDirectoryEntries(filterWorkflowItems(allEmployees, {query:#team-directory-search,
                          status:#team-directory-status, searchFields:[id,name,department,role,location,status]}),
                          {field:#team-directory-sort, direction:#team-directory-order})
   filteredEmployees <- same with the HR filter controls
3. directory-report-total / -active / -onleave  <- counts by exact stored status strings
4. department counts -> sorted <li> list ("<Department>: <count>"), or "No directory labels loaded."
5. no employees -> both tables get the empty message
6. Team rows: person button (opens dialog) | department | role | location | status pill
7. HR rows:   name+id | department | role | location | status pill | [View] [Edit]
8. counts "<shown> of <total> record(s)"; export buttons disabled when their filtered view is empty
9. renderEmployeeRecordSummary()
```
Both views iterate the *entire* directory — there is no supervisor scoping, no team membership, and no permission boundary. "Team" is a filtered copy of "HR".

**`renderRawAttendanceSummary(records)`** — zero-padded total count, earliest and latest valid `YYYY-MM-DD` date (or `—`), and mirrors the count into the overview card. No hours, no days, no rate.

**`applyDefaultAttendanceRange(records)`** — if records exist and **both** date inputs are empty, set `from`=earliest and `to`=latest. Comment cites the legacy MO-IT108 "default to full available span" requirement; it is not a pay period.

**`clearAttendanceCalendarSelection()`** — clears the two calendar selection variables, restores the instruction text, re-renders the calendar. Bound to typing in either date field so a manual edit invalidates the calendar-selected range.

**`selectAttendanceCalendarDate(date)`** **[LIVE]**
```
1. no start yet, or a complete range exists:
       start <- date; end <- ""; status "Start date … choose an end date; no status or hours are inferred"
       re-render; return
2. otherwise: order the two dates, set both inputs, status "Applied inclusive raw-record range … no days or hours were calculated."
3. loadAttendance()            // refetch with the new range
```
Clicking a second earlier date normalises rather than erroring.

**`renderAttendanceCalendar(records)`** **[LIVE][READ-ONLY]**
```
1. employeeId <- current select value
2. if the employee changed: reset month to the earliest record's month, else today
3. calendar <- buildAttendanceCalendar(month, records)
4. for each cell: aria-label "<date>, n raw records"; a date button (selected when it equals either bound);
   when rows exist, a <ul> of `<timeIn>–<timeOut>` events whose title/aria-label include the raw attendanceId and dayType
```
A day with no row renders an empty cell and **no** absence meaning.

**`renderAttendance(records)`** **[LIVE]**
```
1. state.rawAttendance <- sortAttendanceRecords(records, {#attendance-sort, #attendance-order})
2. export enabled when non-empty; applyDefaultAttendanceRange; renderRawAttendanceSummary
3. table rows: date | attendanceId | scheduleId | timeIn | timeOut | dayType   (all as text)
4. empty -> "No attendance entries match this employee and date range."
5. "<n> raw entries loaded"; renderAttendanceCalendar(ordered)
```
Sorting re-sorts the already-loaded rows without refetching.

**`loadAttendance()`** **[LIVE]**
```
1. employeeId <- select value; blank -> clear table/summary/calendar, message "Load the employee directory first."
2. token <- ++state.attendanceRequestNumber; blank the current rows and enable nothing
3. message "Loading raw attendance records…"
4. params: employeeId, plus from/to only when set
5. fetch /api/attendance; throw on non-OK
6. discard result if the token moved
7. renderAttendance(items or [])
catch (token still current): clear state, message the error, count "Could not load records"
```
Nothing is cached: switching employee or range always refetches.

**`renderAttendanceReport(items)`** — stores `state.attendanceReport` (the unfiltered server payload), then `applyAttendanceReportFilters()` and `renderEmployeeRecordSummary()`.

**`applyAttendanceReportFilters()`** **[READ-ONLY]**
```
1. visible <- sortAttendanceCoverage(filterAttendanceCoverage(state.attendanceReport,
                 {query:#attendance-report-search, mapping:#attendance-report-mapping}),
                 {field:#attendance-report-sort, direction:#attendance-report-order})
2. per row: name·id | "Schedule <id>" or "No mapping" | raw count | first date | last date | "View rows" (only when mapped)
3. count label "<shown> of <total> employee rows visible · <sum> raw records in range"
4. export disabled when nothing visible
```
"View rows" carries only the employee ID; the click handler (registered once at load) copies the report's `from`/`to` into the attendance filters, switches view, and lets `switchView` refetch.

**`loadAttendanceReport()`** **[LIVE]**
```
1. token <- ++state.reportRequestNumber; hide status; count "Loading report"; disable export
2. params from/to when set
3. fetch /api/reports/attendance
4. VALIDATE SHAPE: items must be an array AND payload.hoursCalculated must be exactly false
      -> otherwise "Unexpected attendance report response."
5. discard if token moved
6. renderAttendanceReport(items); status "Loaded raw record counts and date coverage only; no hours or payroll values were calculated."
catch (token current): clear, message, count "Could not load report", show error status
```
The `hoursCalculated !== false` check is a contract assertion, not a calculation.

**`loadEmployees()`** **[LIVE]**
```
1. token <- ++state.requestNumber; query <- trimmed search value
2. "Loading employee records…" / count "Loading"
3. url = /api/employees[?q=…]
4. fetch, parse, validate `items` is an array
5. discard if token moved
6. state.employees <- items; state.totalEmployees <- payload.total (integer) else items.length
7. if there is no query: allEmployees <- items; repopulate both pickers; renderDirectoryViews()
8. seed button visible only when signed in and not searching
9. renderEmployees()
catch (token current): clear employees and pickers, hide seed, re-render, show the message,
      count "Could not load records"
```
A search therefore narrows the employee table but leaves `allEmployees`, the pickers, and the Team/HR/directory summaries at the last full load — intentional, but worth knowing when reading the counts.

**`seedSyntheticDemoData()`** **[LIVE]**
```
1. disable the button, label "Adding missing fixtures…"
2. POST /api/demo/seed
3. throw on non-OK
4. result count "<e> employees, <m> schedule links, and <a> attendance rows added"
5. await loadEmployees()
finally: re-enable, restore label
catch: table message + "Demo setup failed"
```
Safe to repeat; already-existing documents are reported as not created.

### 7.5 Workflow (leave and tickets)

**`renderWorkflow()`** **[LIVE]** — the largest renderer.
```
1. bail out if the leave table is absent
2. leaves <- sortWorkflowItems(filterWorkflowItems(state.workflow.leaveRequests, {
       query, status, type=leaveType, typeField:'leaveType',
       searchFields:[id,employeeId,employeeName,leaveType,startDate,endDate],
       dateRange:{from,to,startField:'startDate',endField:'endDate'}}), {leave-sort, leave-sort-order})
   - remember the filtered list in state.visibleLeaveRequests
3. table: id | name·id | leaveType | startDate — endDate | status pill | [Details] + [Cancel] (Cancel only when Pending review)
4. empty states distinguish "no session requests yet" from "no requests match these filters"
5. "<shown> of <total> request(s) in this sandbox"
6. same for tickets: id | name·id | ticketType | queue | status | [Details] + [Cancel] (only when Open)
7. enable/disable the four export buttons by full vs filtered emptiness
8. overview workflow count <- leaves + tickets, zero-padded
9. renderWorkflowSummary(); renderEmployeeRecordSummary(); renderLeaveCalendar()
```
The date filter is an **overlap** test computed in `filterWorkflowItems`: `endDate >= from` and `startDate <= to`.

**`renderWorkflowSummary()`** — groups stored labels into rows (`Leave request status/type`, `Ticket status/type/queue`) with counts, writes `"<rows> stored-label rows · <n> workflow records"`, disables export when empty. No approval, SLA, or resolution meaning.

**`renderEmployeeRecordSummary()`** **[READ-ONLY]**
```
1. summary <- employeeRecordSummary(state.allEmployees, state.attendanceReport, leaves, tickets)
2. visible <- filterAndSortEmployeeRecordSummary(summary, {search, mapping, sort, order})
3. rows: name·id | Mapped / No mapping | raw record count | first date | last date | leave count | ticket count
4. "<shown> of <total> employee rows · stored counts only"
5. enable all/visible exports
```
Depends on `state.attendanceReport`, which is only populated after the Reports view has been opened — before that, every employee shows 0 raw rows even if rows exist. That is a real display defect (**[GAP] UX-1**), not a calculation issue.

**`applyWorkflowItem(collection, item, storage)`**
```
1. find the existing item by id in state.workflow[collection]
2. absent -> prepend; present -> replace in place
3. refresh the storage label from the server's label
4. renderWorkflow()
```
No refetch — the server response is the source of truth for the single item.

**`sendWorkflowRequest(url, body) -> Promise<Payload>`**
```
1. headers <- apiHeaders(body === undefined ? {} : {Content-Type: application/json})
2. POST (no body when undefined)
3. parse JSON; throw on non-OK with the server message
4. require payload.item, else "Unexpected workflow response."
return payload
```

**`handleWorkflowCancel(event)`** **[LIVE]**
```
1. button <- event.target.closest('button[data-action]'); none -> return
2. isLeave <- action == 'cancel-leave'; pick the matching status element and collection
3. disable the button
4. POST /api/workflows/(leave|tickets)/<id>/cancel     (no body)
5. applyWorkflowItem(...); status "<id> cancelled in this user sandbox. No reviewer was notified."
catch: error text; re-enable the button
```
There is no confirmation dialog. Cancel is terminal in this model — there is no reopen or appeal state.

### 7.6 Calendars

**`selectLeaveCalendarDate(date)`** **[LIVE]**
```
1. if no start or a complete range exists:
       start <- date; end <- ""; write start into #leave-start, clear #leave-end
       status "Draft start date <date> selected; choose an end date. Nothing has been submitted."
2. else if date < start:
       restart the draft at the earlier date
3. else:
       end <- date; write into #leave-end
       status "Draft range <start> through <date> selected… no days or balances are calculated."
4. renderLeaveCalendar()
```
It **only drafts form values**. No submission, no day count, no balance check.

**`renderLeaveCalendar()`** **[READ-ONLY]**
```
1. calendar <- buildLeaveCalendar(leaveCalendarMonth, state.workflow.leaveRequests)   // today defaults to now
2. title; rebuild the 6×7 grid
3. out-of-month cells: aria-hidden, empty
4. in-month: date button (class `selected` when it equals either draft bound, `today` when today)
5. per-day request markers: "<leaveType> · <status>", cancelled styled, title "<name> · <id> · request date only"
```
The calendar is a *display* of submitted ranges, not an approved-leave calendar.

### 7.7 Search, exports, and view switching

**`fetchSandboxSnapshot() -> Promise<Object>`** **[LIVE][READ-ONLY]**
```
1. headers <- apiHeaders()
2. parallel GET /api/health, /api/employees, /api/workflows
3. parse all three; throw the first non-OK with its message
4. require employees.items to be an array
5. parallel GET /api/attendance?employeeId=… for EVERY employee
6. return {mode, employees, leaveRequests, tickets, attendanceByEmployee}
```
This is the only fan-out read: N+1 requests, no concurrency cap. Acceptable for three fixtures, not for a real roster.

**`renderSandboxSearchResults`** — described in §7.3.

**`switchView(view)`** **[LIVE]**
```
1. labels map (overview, employees, attendance, requests, team, hr, reports, payroll); unknown -> return
2. hide every .page-view except <view>-view
3. nav items: toggle .active and aria-current="page"
4. breadcrumb text; document.title "<label> — MotorPH Payroll Office"
5. view == attendance and an employee is selected -> loadAttendance()
6. view == reports -> loadAttendanceReport()
```
Entering Attendance or Reports refetches every time; there is no cache.

### 7.8 Event wiring (73 registrations)

Grouped by what they do — each is a one-line handler, so they are listed rather than pseudocoded individually:

| Handler(s) | Behaviour |
|---|---|
| `#employee-form` submit | POST (new) or PUT (edit) with the five editable fields; disable save; on success reset the editor, clear search, reload, and report whether a schedule mapping was added (it never is) |
| `#employee-cancel-edit-button` click | `resetEmployeeEditor()` |
| `#leave-request-form` submit | local `submitLeaveRequest()` shape check → POST → set the calendar month to the request's month → clear draft → `applyWorkflowItem` → reset form, restore employee → status "saved… No reviewer was notified." |
| `#ticket-form` submit | same pattern with `submitTicket()`; status includes the queue label "…, not delivered." |
| leave/ticket row containers click | `handleWorkflowCancel` |
| leave/ticket filter inputs, selects, sorts | `renderWorkflow` (client-side only) |
| employee record summary search/mapping/sort/order | `renderEmployeeRecordSummary` |
| HR and Team search/status/sort/order | `renderDirectoryViews` |
| `#leave-start`, `#leave-end` input | clear the draft range and re-render the calendar (manual typing overrides calendar selection) |
| leave calendar prev/next | shift `leaveCalendarMonth` by one month, re-render |
| `[data-open-requests]` buttons | `switchView('requests')` |
| `#seed-demo-button` | `seedSyntheticDemoData` |
| `#employee-search` input | 180 ms debounce → `loadEmployees` |
| employee sort/order change | `renderEmployees` (no refetch) |
| `#refresh-button` | `loadEmployees` |
| employee export | `downloadCsvFile('motorph-synthetic-employees.csv', six columns, sorted current rows)` |
| team export / HR export | fetch the same filtered+sorted view the table shows, six columns, status column labelled "Stored Demo Status" |
| employee-record-summary export (all / visible) | nine columns of stored counts, mapping rendered as "Mapped"/"No mapping" |
| directory summary export | `directoryLabelSummaryRows` → three columns of label counts |
| workflow summary export | `workflowSummaryRows` → three columns |
| attendance export | six raw columns from `state.rawAttendance`; employee ID sanitised for the filename |
| attendance report export | re-applies the current filter+sort, six columns, no hours |
| leave export (all / visible) | nine columns |
| ticket export (all / visible) | eight columns |
| `#sandbox-export-button` | `fetchSandboxSnapshot` → `buildWorkspaceExport` → download JSON, then a sentence counting each collection |
| `#sandbox-search-form` submit | blank query is refused with a message; otherwise snapshot → `searchSandboxRecords` → render → "n matching stored records… No attendance or payroll values were derived." |
| attendance from/to input | `clearAttendanceCalendarSelection` |
| attendance filter submit | `loadAttendance` |
| attendance sort/order change | `renderAttendance(state.rawAttendance)` (re-sort only) |
| attendance calendar prev/next | shift the month, re-render |
| attendance report filter submit | `loadAttendanceReport` |
| attendance report search/mapping/sort/order | `applyAttendanceReportFilters` |
| attendance report rows click (`button[data-employee-id]`) | copy the report range into the attendance filters, switch to the attendance view |
| attendance employee change | clear both dates and the calendar selection, `loadAttendance` |
| dialog close/done buttons and backdrop click | `dialog.close()` |
| document keydown `/` | focus the employee search when not typing in a field and no dialog is open |
| nav items | `switchView(dataset.view)` |
| `#auth-form` submit / create / reset / sign-out | only bound when Firebase is configured: `signIn`, `createDevAccount`, `sendPasswordReset`, `signOut(auth)` |

### 7.9 Bootstrap

```
if not firebaseConfigured and isolatedTestMode:
    showWorkspace(null)                       // test-only unauthenticated workspace
else if not firebaseConfigured:
    hide the auth form, create-account, and reset buttons
    showSignIn("Firebase configuration is required. Local-memory access is disabled. …")
else:
    bind auth form / create / reset / sign-out
    setPersistence(auth, inMemoryPersistence)  // session not persisted across reloads
      .then(onAuthStateChanged(user => user ? showWorkspace(user) : showSignIn()))
      .catch(error => showSignIn(friendlyAuthError(error)))
```
`inMemoryPersistence` is a deliberate choice: closing the tab ends the session. `isolatedTestMode` requires both `VITE_E2E_TEST_MODE=true` **and** no Firebase config; it cannot leak into a configured build.

---

## 8. Browser: support modules (16 files, 537 lines)

Each is a pure function; all follow the same defensive contract — non-array input returns `[]`, never throws, and where they do validate, they throw `TypeError` on unusable *arguments* rather than on data.

### 8.1 `attendance-calendar.js`
**`dateKey(year, monthIndex, day)`** (private) — zero-padded `YYYY-MM-DD`.
**`isIsoDate(value)`** (private) — shape check **plus** round-trip through `Date` so `2026-02-30` is rejected.
**`buildAttendanceCalendar(monthDate, records = [])`** **[READ-ONLY]**
```
1. monthDate must be a valid Date -> TypeError ; records must be an array -> TypeError
2. year, monthIndex, daysInMonth; mondayOffset = (first weekday + 6) % 7   // Monday-first grid
3. bucket records by date, keeping only valid ISO dates inside this month
4. 42 cells: in-month cells carry the date string and a shallow copy of that day's rows;
   out-of-month cells carry date "", day null, records []
5. return {title: "Month YYYY", weekdays: Mon…Sun, weeks: 6 × 7}
```
Always six weeks, so the grid height is stable. No cell ever implies absence.

### 8.2 `leave-calendar.js`
**`dateKey` / `validIsoDate`** (private) — same as above.
**`buildLeaveCalendar(monthDate, requests = [], today = new Date())`** **[READ-ONLY]**
```
1. validate arguments
2. todayKey from a valid `today`, else ""
3. keep requests with valid start AND end and start <= end
4. 42 cells; for in-month dates, requests where startDate <= date <= endDate
5. mark isToday
6. return the same grid shape plus per-cell `requests`
```
Range overlap is evaluated per day, which is why a multi-day leave shows a marker on every covered day.

### 8.3 `workflow-demo.js` — **this is a shape mirror, not the writer**
**Constants** — `LEAVE_TYPES`, `TICKET_QUEUES`, `MAX_NOTE_LENGTH = 240`, exported.
**`createWorkflowState()`** — `{leaveRequests: [], tickets: [], nextLeaveNumber: 1, nextTicketNumber: 1}`.
**`requireState(state)`** (private) — both arrays present, else `TypeError`.
**`findEmployee(employeeId, employees)`** (private) — must exist in the directory, else `"Choose an employee from the synthetic demo directory."`
**`isValidDate(value)`** (private) — regex + UTC round-trip.
**`cleanOptionalText(value, field)`** (private) — trim, ≤240.
**`dateStamp(value)`** (private) — `YYYY-MM-DD` from a Date or date string.
**`submitLeaveRequest(state, input, employees, submittedAt = new Date())`** **[VALIDATION]**
```
1. validate type, employee, both dates, ordering, note length
2. build id "LV-0001" from state.nextLeaveNumber
3. return {state: new state with the request appended and the counter incremented, request}
```
**`cancelLeaveRequest(state, requestId, employeeId)`** — finds it, requires same employee, requires `Pending review`, returns a new state with `status: "Cancelled"`.
**`submitTicket(state, input, employees, submittedAt)`** — validates type against `TICKET_QUEUES`, employee, required message ≤240; id `TK-0001`; `queue` from the map; `status: "Open"`.
**`cancelTicket(state, ticketId, employeeId)`** — same shape, requires `Open`.

**Why this file matters:** `app.js` calls `submitLeaveRequest(createWorkflowState(), …)` and `submitTicket(createWorkflowState(), …)` — each time with a **fresh throwaway state** — purely to reuse the validation messages. The returned state is discarded; the persisted record comes from the server. So:
- This module is a **client-side pre-check**, and the only place a "you can cancel only your own item" rule exists on the client.
- Its `LV-0001` / `TK-0001` IDs and its `submittedAt` date-only stamp are **not** what gets stored (the server issues `LV-<12 hex>` and an ISO instant).
- `cancelLeaveRequest` / `cancelTicket` here are **dead code in the product path** — nothing calls them. They only exist for the module's own tests.
- The duplicated validity rules (Java vs this file) can drift. If you change `MAX_NOTE_LENGTH` or the leave types in one place, change both (**[GAP] DUP-1**).

### 8.4 `csv-export.js`
**`validateColumns(columns)`** (private) — non-empty array; each entry needs a non-blank string `key` and `label`, else `TypeError`.
**`csvCell(value)`** (private) — `String(value ?? '')`; prefixes `'` when the text starts with optional whitespace then one of `= + - @` (formula-injection guard); wraps in double quotes and doubles internal quotes.
**`buildCsv(columns, rows)`** — validates, requires each row to be a non-array object, emits quoted header then quoted body joined by `\r\n`. All cells are quoted, so commas, quotes, and newlines are safe.

### 8.5 `attendance-coverage-filter.js`
**`filterAttendanceCoverage(items, {query = '', mapping = 'all'})`** **[READ-ONLY]**
```
1. non-array -> []
2. drop items that are not objects or have neither employeeId nor employeeName
3. mapping 'mapped' requires hasScheduleMapping === true; 'unmapped' requires it false;
   any mapping value outside all/mapped/unmapped -> the whole result is empty (fail closed)
4. empty query -> keep; else search the joined text of id, name, and "schedule <id>"/"no mapping"
```
Note step 3: an unrecognised `mapping` value returns **nothing** rather than everything — a deliberate fail-closed choice.

### 8.6 `attendance-coverage-sort.js`
**`sortAttendanceCoverage(items, {field = 'employeeId', direction = 'asc'})`** — allowlist of six fields (unknown → `employeeId`); numeric comparison for `scheduleId`/`rawRecordCount` with non-numeric treated as `-Infinity`; otherwise `localeCompare` with numeric collation; ties broken by `employeeId`.

### 8.7 `attendance-record-sort.js`
**`sortAttendanceRecords(items, {field = 'date', direction = 'asc'})`** — allowlist of six fields; numeric for `attendanceId`/`scheduleId`; `localeCompare` numeric for date/time strings (so `08:05` < `8:05`-style inconsistencies do not reorder badly); final tie-break by `attendanceId` ascending.

### 8.8 `directory-sort.js`
**`sortDirectoryEntries(items, {field = 'name', direction = 'asc'})`** — allowlist of the six employee fields; everything compared as strings with numeric collation; ties by `id`.

### 8.9 `directory-summary.js`
**`directoryLabelSummaryRows(employees)`** **[READ-ONLY]** — counts employees per stored `status` label ("Status label") and per `department` ("Department label"), skipping blank or non-string labels, emitting `{category, label, employeeRecords}` sorted by category then label. Keyed on `category\u0000label` so identical strings in different categories never merge.

### 8.10 `workflow-summary.js`
**`workflowSummaryRows(leaveRequests, tickets)`** **[READ-ONLY]** — five label groups (leave status, leave type, ticket status, ticket type, ticket queue); counts trimmed non-empty string labels; returns `{category, label, recordCount}` sorted. Counts are of stored labels, nothing else.

### 8.11 `workflow-filter.js`
**`filterWorkflowItems(items, {query, status, type, typeField, searchFields, dateRange})`** **[READ-ONLY]**
```
1. non-array -> []
2. an inverted date range (from > to) -> []
3. per item: exact match on status when given; exact match on typeField when given and typeField non-blank;
   overlap test — item.endField >= from and item.startField <= to;
   query substring over the enumerated searchFields only
```
Used for leave, tickets, **and** the Team/HR directories (with `status` bound to the directory-status control and the six directory fields as `searchFields`), which is why an "employee filter" function lives in a workflow module.

### 8.12 `workflow-sort.js`
**`sortWorkflowItems(items, {field = 'submittedAt', direction = 'desc'})`** — nine-field allowlist; string comparison with numeric collation; ties by `id`. Works for both collections because it only reads shared field names.

### 8.13 `employee-record-summary.js`
**`employeeRecordSummary(employees, attendanceSummaries, leaveRequests, tickets)`** **[READ-ONLY]**
```
1. non-array employees -> []
2. index attendance summaries by employeeId
3. count leave and ticket rows per employeeId
4. per employee (must have a string id):
     employeeId, employeeName (name or id), department,
     hasScheduleMapping (=== true only),
     rawRecordCount (integer else 0), firstRecordDate, lastRecordDate (string else ""),
     leaveRequestCount, ticketCount
```
Left join only: an employee with no attendance summary gets zeros, and an attendance summary with no employee is dropped.

### 8.14 `employee-record-summary-view.js`
**`filterAndSortEmployeeRecordSummary(rows, {query, mapping, sortBy, direction})`** **[READ-ONLY]** — mapping filter, substring search over id/name/department, sort over six allowed keys with numeric comparison for numeric fields, ties by `employeeId`. Note the tie-break is applied **before** the direction multiplier, so ties stay ascending even when the primary sort is descending.

### 8.15 `sandbox-search.js`
**`hasQuery(record, fields, query)`** (private) — substring over explicit fields only.
**`searchSandboxRecords(query, {employees, attendanceByEmployee, leaveRequests, tickets})`** **[READ-ONLY]**
```
1. blank query -> four empty arrays (never "everything")
2. attendance: flatten the per-employee groups, stamp employeeId, match over
   employeeId, attendanceId, scheduleId, date, timeIn, timeOut, dayType
3. employees: id, name, department, role, location, status
4. leaveRequests: id, employeeId, employeeName, leaveType, startDate, endDate, status, note
5. tickets: id, employeeId, employeeName, ticketType, queue, summary, status
```
Every field is enumerated, so a future field (a salary, a bank account) cannot be searched unless someone adds it here.

### 8.16 `workspace-export.js`
**`buildWorkspaceExport({mode, employees, leaveRequests, tickets, attendanceByEmployee}, exportedAt = new Date())`** **[READ-ONLY]**
```
1. validate: three arrays, an object map, a valid Date
2. flatten attendanceByEmployee into rows stamped with employeeId, validating each row is an object
3. return {
     formatVersion: 1, exportedAt: ISO, storageMode: mode,
     syntheticOnly: true, hoursCalculated: false, payrollIncluded: false,
     employees, attendance, leaveRequests, tickets          // shallow copies
   }
```
The three boolean/marker fields make the file self-describing: it cannot be mistaken for a payroll extract. **There is no import counterpart** — a restore path would need schema versioning, per-collection validation, duplicate handling, and an owner check.

---

## 9. What is missing, by area

Status key: **Done** = implemented and reachable · **Partial** = works but with a named limit · **Absent** = no code · **Blocked** = cannot be written until you decide something.

| # | Area | Status | What exists now | What is missing | Blocker |
|---|---|---|---|---|---|
| A1 | Employee directory read/search | Done | list, `q` search, detail, Team/HR filtered views | pagination, server-side sort/filter, delete | — |
| A2 | Employee create/update | Partial | six allowlisted fields, server-generated ID, field-masked update | concurrency control, audit trail, PII fields, lifecycle states | D-30, D-31 |
| A3 | Employee ↔ user identity | **Absent** | a free-choice dropdown | binding an account to an employee, or a documented "no identity" decision | D-32 |
| A4 | Org structure | **Absent** | `department`/`role`/`location` strings | department entity, supervisor chain, team membership, effective dates | D-33 |
| B1 | Schedule definition | **Absent** | opaque `scheduleId` 101–103 copied from fixtures | shift start/end, break rules, rest days, holidays, effective dating | D-01…D-05 |
| B2 | Schedule assignment | **Absent** | immutable seeded mapping docs | a create/change/end-date lifecycle and who may do it | D-06, D-23 |
| B3 | Attendance capture | **Absent (removed)** | read-only rows via seed | durable writer, source, correction flow, `POST` returns 405 | D-07…D-09, D-24 |
| B4 | Attendance classification | **Absent** | `dayType` is a stored string (`DEMO`) | present/absent/tardy/undertime/overtime/leave/holiday semantics | D-10…D-13 |
| B5 | Worked time | **Absent** | `timeIn`/`timeOut` printed verbatim | break deduction, overnight handling, rounding, grace, caps, timezone | D-02, D-14…D-17 |
| B6 | Coverage report | Done (as a count) | rows per employee + first/last date | anything that implies absence, days worked, or hours | by design |
| C1 | Leave submission | Done | create pending, list, cancel pending, filters, calendar draft | approval, rejection, balances, accrual, credit deduction, reversal | D-18…D-21 |
| C2 | Ticket workflow | Done (as a log) | create with queue label, list, cancel open | assignment, resolution, reopen, SLA, notification, event history | D-22 |
| D1 | Payroll | **Absent (gated)** | a fail-closed configuration page | everything: bases, tables, period method, rounding, snapshots, payslip, remittance | D-03, D-04, D-10, D-34…D-45 |
| E1 | Reporting | Partial | label counts, raw coverage, record counts, CSV/JSON export | payroll register, remittance, statutory reports, period reports | D-34, D-41 |
| F1 | Concurrency | Partial | per-request token guards in the UI; `ALREADY_EXISTS` on create | optimistic concurrency for updates, transactional cancel | D-31 |
| F2 | Audit | **Absent** | `submittedAt`/`cancelledAt` only | who changed what, before/after, immutable log | D-30 |
| F3 | Roles / RBAC | **Absent** | owner-only Firestore paths | roles, per-action permissions, supervisor scope, HR scope | D-23, D-32 |
| F4 | Tenant model | **Absent** | one Firestore tree per Firebase user | company/tenant, shared employee directory, invitations | D-32 |
| G1 | Ops | Partial | fail-closed startup, CSP, loopback test mode, rules file | emulator rules tests, indexes, backups/retention, error reporting, deploy smoke | D-43 |
| G2 | Accessibility / responsiveness | Partial | semantic tables, aria labels, keyboard `/` search | formal audit across 320 px→desktop, focus management in the dialog | — |

Two "absences" deserve emphasis because they are frequently mistaken for features that exist:

- **There is no attendance writer.** Not disabled behind a flag — the route, the service method, the mutable store interface, and the form are deleted. `POST /api/attendance` → 405 is asserted by tests.
- **There is no payroll code path.** Not a stub, not a disabled form. The Payroll view is static HTML describing its own gates. Nothing computes.

---

## 10. The formulas and policy inputs you need to supply

This is the answer to "what formulas I need to provide". Each row is a decision the code cannot make for you. **The examples are illustrative only — they are not recommendations, and none of them is switched on.** Where the GPT handoff or a peer repo offers a value, it is labelled as *candidate*, never *current*.

### 10.1 Timekeeping and schedule (needed before any hour can be computed)

| ID | Decision | Why the code cannot guess | Plugs into |
|---|---|---|---|
| **D-01** | Do schedules exist as first-class records (shift start, end, unpaid break minutes, rest days, holiday calendar, effective dates), or is `scheduleId` kept as an opaque label? | Today `scheduleId` is an integer with no definition anywhere. Hours cannot be computed against a schedule that does not exist. | `EmployeeScheduleStore`, new `ScheduleStore` |
| **D-02** | Break policy: fixed unpaid minutes per shift, a punch-based break, or none? | A single 08:00–17:00 pair yields 9 elapsed hours; the peer candidate calls that "9 hours worked" with no lunch deduction, which contradicts an 8-hour normal workday with a ≥60-minute meal period. | `computeWorkedMinutes` |
| **D-03** | Timezone and daylight assumptions for stored `timeIn`/`timeOut` (strings, no offset today). | PHT has no DST, but an import from another source may carry offsets. | parser + `computeWorkedMinutes` |
| **D-04** | Overnight shifts: representable or prohibited? | `AttendanceRecord` **throws** when `timeIn > timeOut`, so a 22:00–06:00 shift cannot be stored at all. | `AttendanceRecord` constructor |
| **D-05** | Holiday and rest-day calendar source, and whether `dayType` becomes an enum (`WORKDAY`, `RESTDAY`, `SPECIAL`, `REGULAR_HOLIDAY`, …) or stays a free string. | `dayType` is currently whatever the seed wrote (`DEMO`), and nothing interprets it. | `dayType` enum + premium rules |
| **D-06** | Who may create/change a schedule assignment, and are changes effective-dated? | Attendance attribution today is "whatever mapping document exists now" — a change retroactively reattributes history. | assignment lifecycle |
| **D-07** | Approved capture source: manual entry, biometric import, CSV import, API? | There is no writer; the answer determines validation, duplicates, and trust level. | new attendance writer |
| **D-08** | Unique key for an attendance row: `(employee, date)`, `(schedule, date)`, or `(employee, date, sequence)` for split shifts? | The fixture enforces `scheduleId:date`; Firestore does not enforce it at all. | document ID scheme + rules |
| **D-09** | Correction policy: who may amend a punch, is a reason required, is the original retained? | An amendable record with no history is unusable for payroll evidence. | correction flow + audit |
| **D-10** | Rounding for punches: exact minutes, nearest 5/15 minutes, or grace-then-exact? Is rounding applied to a punch time or to the computed total? | `08:01` vs `08:00` changes pay at any hourly rate. Rounding to totals vs per-punch gives different answers on the same data. | `computeWorkedMinutes` |
| **D-11** | Lateness: grace period length, and does it subtract pay or only flag? | Never derivable from stored data alone. | classification |
| **D-12** | Undertime and early-out treatment. | Same. | classification |
| **D-13** | What an employee with **no** row means: nothing, absent, rest day, holiday, no schedule? | Today the UI says explicitly "not absent". Any report that changes that must be your decision, not an inference. | classification + reports |
| **D-14** | Overtime: threshold (daily 8 h? weekly 40 h? both?), pre-approval requirement, and whether it is computed from punches or a separate approval record. | Statutory and company policy both matter; punches alone cannot tell approved from unapproved OT. | `computeOvertime` |
| **D-15** | Split shifts / multiple punch pairs per day. | The current model holds exactly one in and one out per row. | schema |
| **D-16** | Missing-punch handling: reject, hold for correction, or pay at a policy default. | A payroll engine must never silently pay a half-punch. | validation + exception queue |
| **D-17** | Whether leave days are excluded from expected workdays before absence/undertime is assessed. | Required to avoid double-penalising an approved leave day. | classifier + leave |

### 10.2 Leave and workflow

| ID | Decision | Why | Plugs into |
|---|---|---|---|
| **D-18** | Leave types, and which are paid, part-paid, or unpaid. | `LEAVE_TYPES` is `[Vacation, Medical, Personal, Unpaid]` in **two** places (Java + JS) with no pay semantics attached. | leave ruleset |
| **D-19** | Accrual: per cutoff, per month, per year; carryover cap; probation rules. | Nothing exists. | accrual job |
| **D-20** | Balance check at submission (block, warn, or allow negative) and unit (calendar days vs working days, half-days?). | Today a request for any range is accepted. | validation |
| **D-21** | Approval authority, stages, delegation, and what happens to a request already reflected in a paid period (reversal/recovery). | Cancel is the only transition; there is no approver. | state machine |
| **D-22** | Ticket lifecycle: assignment, resolution, reopen, SLA, notification, and whether a queue label has any operational meaning. | The current queue is a string. | workflow state machine |

### 10.3 Payroll structure and policy

| ID | Decision | Why | Plugs into |
|---|---|---|---|
| **D-34** | **Pay period model**: monthly, semi-monthly (1–15 / 16–end), weekly, or daily. If semi-monthly, how the second cutoff is computed (full-month contributions taken from cutoff 2? split evenly?). | Every downstream number changes. The peer candidate uses 1–15 / 16–end and deducts the whole monthly statutory amount from cutoff 2 — *candidate, not policy*. | `PayPeriod` + engine |
| **D-35** | **Rate basis**: monthly salary ÷ 22 days ÷ 8 h? ÷ actual working days? ÷ 261? And is the divisor fixed or period-dependent? | Determines the hourly rate used for OT and undertime. | `hourlyRate` |
| **D-36** | **Earnings inputs**: basic pay, allowances (taxable vs non-taxable), 13th month, incentives, commissions. Which are in the contribution bases vs the tax base? | The GPT five-base split exists precisely because these differ per agency. | `PayrollInputs` |
| **D-37** | **Overtime, night differential, rest-day and holiday premium rates** (e.g. 125 %, 130 %, 200 %, 30 % ND), and whether they are multipliers on hourly rate or fixed amounts. | Statutory floors, company generosity, and category eligibility all vary. | premium functions |
| **D-38** | **Absence and undertime deduction** basis and whether leave without pay uses the same divisor as OT. | Inconsistent divisors are a classic payroll defect. | deduction |
| **D-39** | **Rounding of money**: 2 dp `HALF_UP`, `HALF_EVEN`, or truncation; and whether rounding happens per component or once at the end. | The peer evidence shows 0.01 differences purely from this. | every money function |
| **D-40** | **Rounding of time**: see D-10; state whether it is minutes-exact to the centavo. | — | time functions |
| **D-41** | **Deduction allocation across cutoffs** and the order of deduction application (statutory first? loans? cash advance?). | Order changes the net, and it must match your payslip layout. | allocation |
| **D-42** | **Negative-net and zero-floor policy**: floor at zero and carry forward, or reject the run. | The candidate floors at zero; that silently forgives an over-deduction. | net calculation |
| **D-43** | **Retention and immutability of a payroll run**: which inputs and which ruleset version are snapshotted, and can a run be amended after payment? | Needed before a run can be an auditable record. | run snapshot schema |
| **D-44** | **Payslip content and authorization**: which fields appear, who may view another person's payslip, and how it is delivered. | Requires the role model in D-23. | payslip view |
| **D-45** | **Remittance and statutory reporting**: which agency reports are produced, on what schedule, in what format. | Not derivable from contribution amounts alone. | reports |

### 10.4 Statutory tables — required, and required *versioned*

Every one of these must be supplied as an effective-dated table with a source citation and a ruleset version. The values below are what the research in `docs/candidate-policy-review.md` located; they are **candidates for citation, not approved inputs**, and the handoff's 2026 monthly profile is a third-party reconstruction.

| ID | Component | Must state | Candidate research says | Still unresolved |
|---|---|---|---|---|
| **D-46** | SSS | Total rate and split; MSC floor/ceiling; MSC step; EC bracket; MPF boundary; rounding of the MSC | 15 % total, 5 % employee / 10 % employer; MSC ₱5,000–₱35,000 in ₱500 steps; EC ₱10 ≤ ₱14,500 and ₱30 ≥ ₱15,000; MPF above ₱20,000 | the exact 2026 circular; MSC rounding rule; whether EC is employer-only on your payslip |
| **D-47** | PhilHealth | Rate; floor/ceiling; split; **which base** (basic vs gross vs explicit) | 5 %, ₱10,000–₱100,000, equal split; base is monthly basic salary excluding OT/allowances/13th month/bonuses | the exact period method and centavo rounding per share |
| **D-48** | Pag-IBIG | Max fund salary; employee rate tiers; employer rate; whether it is monthly regardless of period | max ₱10,000; employee 1 % ≤ ₱1,500 else 2 %; employer 2 % | cutoff allocation; voluntary/other member categories |
| **D-49** | BIR withholding | Which table (monthly / semi-monthly / weekly / daily Annex E); tax base; whether contributions are deducted **before** withholding; annual reconciliation | Monthly Annex E bands: 0 % ≤ ₱20,833; 15 % over ₱20,833; ₱1,875 + 20 % over ₱33,333; ₱8,541.80 + 25 % over ₱66,667; ₱33,541.80 + 30 % over ₱166,667; ₱183,541.80 + 35 % over ₱666,667 | base definition; **the pre-tax contribution deduction assumption is explicitly documented as an implementation assumption in the handoff, not a rule**; year-end adjustment |
| **D-50** | Bracket boundary convention | Whether comparison is against the next whole-peso bound so ₱33,332.50 stays in the lower band | handoff uses `< next bound` | confirm this is what you want, and encode it as tests |
| **D-51** | Effective dates | `effectiveFrom` / `effectiveTo` per table, and what happens outside the covered window | `ph-payroll` reference fails closed outside covered periods | fail-closed vs nearest-table behaviour |

### 10.5 Identity, roles, and operations

| ID | Decision | Why | Plugs into |
|---|---|---|---|
| **D-23** | Role model: which roles exist (employee, team lead, payroll staff, HR, IT admin, admin), and the exact action each may take. | The queue labels imply roles that do not exist. Owner-only Firestore paths can express "mine only", nothing more. | Rules + service authz |
| **D-24** | Employee self-service vs HR-operated: who may submit attendance, leave, and tickets on whose behalf. | Today the dropdown allows submitting as anyone. | validation + rules |
| **D-30** | Audit requirements: which entity changes must be logged, with what fields and retention. | Nothing today. | audit log |
| **D-31** | Concurrency policy for directory edits and cancellations: last-write-wins acceptable, or version check required? | Two concurrent cancels both succeed today. | `If-Match`/version field |
| **D-32** | Tenant model: is one Firebase account one company, one employee, or one sandbox? Do employees share a directory? | Owner-scoped paths mean every signed-in user has an isolated fake company. That is fine for a demo and wrong for the product. | schema + rules |
| **D-33** | Organization model: departments, positions, supervisors, cost centres, effective dates. | Current fields are free strings. | new entities |
| **D-52** | Currency precision and display: PHP only, centavos stored as integers or as decimal strings in Firestore? | Firestore has no native decimal; `stringValue` vs `integerValue` centavos is a schema decision that is expensive to change later. | money schema |
| **D-53** | Data classification and retention: what is PII, how long is it kept, what is deletable on request. | Blocks any real-data use. | policy + schema |

### 10.6 The minimum set to unblock the next slice

If you want one short list to answer first, this is it — in dependency order, because each one gates the next:

```
D-32  tenant model            → decides the whole schema
D-23  roles + permissions     → decides what Rules and services may allow
D-01  schedule definition     → the missing entity
D-02  break policy            → the first number that makes hours meaningful
D-04  overnight shifts        → a model change, not a formula
D-10/D-39/D-40  rounding      → must be decided before any golden example
D-34  pay period model        → decides semi-monthly vs monthly everything
D-18/D-19/D-20  leave policy  → gates approval + pay interaction
D-46…D-51 statutory tables    → with sources, effective dates, and golden examples
```

---

## 11. Pseudocode for the missing functions (design only — do not implement yet)

These are the seams. Each is written against the *current* model so you can see exactly where it attaches, and each is annotated with the decisions that must land first. **None of this is authorised work**; it exists so you can judge the shape of the remaining build.

### 11.1 Timekeeping

```
computeWorkedMinutes(record, schedule, calendar, policy) -> Result<minutes, exception>
  // requires D-01 (schedule), D-02 (break), D-03 (tz), D-04 (overnight), D-10 (rounding)
  1. if schedule is missing            -> return Unmapped(record)        // never Absent
  2. start <- combine(record.date, record.timeIn)
     end   <- combine(record.date, record.timeOut); if end <= start and policy.allowsOvernight -> end += 1 day
  3. rounded <- roundPunches(record.timeIn, record.timeOut, policy.punchRounding)
  4. gross   <- minutesBetween(rounded.in, rounded.out)
  5. breakM <- policy.breakFor(schedule, gross)          // fixed, punch-based, or zero
  6. paid    <- max(0, gross - breakM)
  7. cap     <- policy.capPerDay ?? infinity
  8. return Worked(min(paid, cap), overtimeCandidate: max(0, paid - schedule.standardMinutes))
```

```
classifyAttendance(worked, schedule, calendar, policy) -> Classification
  // requires D-05 (dayType enum), D-11 (grace), D-12 (undertime), D-13 (no row), D-14 (OT)
  Present | Late(minutes) | Undertime(minutes) | Absent | RestDay | Holiday(type) | OnLeave(type) | NoSchedule
  // rule: a day with no row is only Absent when the schedule says it is a workday AND no approved leave covers it
  // rule: an unapproved overtime minute is reported, never paid, until D-14 says otherwise
```

### 11.2 Leave

```
computeLeaveDays(startDate, endDate, leaveType, calendar, policy) -> days
  // requires D-20 (calendar vs working days, half days), D-05 (holiday calendar)
  filter the inclusive date range by policy.countsWeekends / policy.countsHolidays for the type
  subtract half-day markers if the type supports them
```

```
applyLeaveRequest(current, request, balances, approver, policy) -> newState | Rejection
  // requires D-19, D-21, D-23, D-24
  guard: requester may act for this employee (D-23/D-24)
  guard: balance sufficient, or policy allows negative (D-20)
  guard: no overlapping approved leave or approved OT on the same dates (D-17)
  transition Pending review -> Approved | Rejected, recording approver, timestamp, reason
  on Approval: deduct from the balance atomically (transaction) and emit an audit event
```

### 11.3 Payroll (the gated engine)

```
buildPayrollRun(period, employees, attendance, leave, ruleset) -> Run | Blocked(reasons)
  // requires D-34, D-36, D-43, D-46…D-51
  1. assert ruleset covers period (D-51); if not -> Blocked("no ruleset for period")   // fail closed
  2. for each employee with an approved payroll profile:
       inputs <- PayrollInputs{
                   cashPay, sssCompensation, philHealthMbs, pagibigFundSalary, taxableCompensation
                 }                                    // the five INDEPENDENT bases
       if any base missing -> Blocked(employee, "missing base")   // never infer one from another
  3. compute components per employee, each a pure function of (input base, ruleset)
  4. persist an immutable snapshot: inputs, ruleset version, computed components, timestamps
  return Run
```

```
computeSss(base, ruleset) -> {employee, employer, ec}
  msc     <- clamp(base, floor, ceiling); msc <- roundToStep(msc, step)          // D-46
  employee<- msc * employeeRate
  employer<- msc * employerRate
  ec      <- bracketLookup(msc, ecTable)                                         // employer-only
```

```
computePhilHealth(base, ruleset) -> {employee, employer}
  b <- clamp(base, floor, ceiling)                                               // D-47
  total <- b * rate
  return { employee: total / 2, employer: total / 2 }                            // each share rounded per D-39
```

```
computePagIbig(base, ruleset) -> {employee, employer}
  fund <- min(base, maxFundSalary)                                               // D-48
  employeeRate <- fund <= lowThreshold ? lowRate : highRate
  return { employee: fund * employeeRate, employer: fund * employerRate }
```

```
computeWithholding(taxableBase, ruleset) -> amount
  // requires D-49 (which table + base definition) and D-50 (boundary convention)
  band <- last band whose lowerBound < taxableBase            // strict, whole-peso bound
  return band.baseTax + (taxableBase - band.floor) * band.rate
```

```
computeNet(run, policy) -> {net, employerCost}
  // requires D-41 (order/allocation), D-42 (floor)
  employeeStatutory <- sss.employee + philHealth.employee + pagibig.employee
  taxableBase       <- policy.taxBaseOf(cashPay, allowances, employeeStatutory)  // <-- THE open assumption
  withholding       <- computeWithholding(taxableBase, ruleset)
  net               <- cashPay - employeeStatutory - withholding - otherDeductions
  if net < 0 and policy.floorAtZero -> net <- 0 and record a carry-forward
  employerCost      <- cashPay + sss.employer + sss.ec + philHealth.employer + pagibig.employer
```

The single most important line above is `policy.taxBaseOf(...)`. The handoff's own document says its choice — subtract employee statutory shares before withholding — is "a documented implementation assumption". **That is exactly the line you have to decide (D-49), and it is why no calculator should exist yet.**

### 11.4 Golden examples you will need to supply

Before any engine is switched on, you need a signed-off table like this — filled by you or by payroll, not by me:

| Case | Period | Inputs (five bases) | Expected SSS ee/er/ec | PhilHealth ee/er | Pag-IBIG ee/er | Taxable base | Withholding | Net | Employer cost |
|---|---|---|---|---|---|---|---|---|---|
| Monthly minimum wage | monthly | … | … | … | … | … | … | … | … |
| Below SSS floor | monthly | … | … | … | … | … | … | … | … |
| At each bracket boundary ±₱0.50 | monthly | … | … | … | … | … | … | … | … |
| Above all ceilings | monthly | … | … | … | … | … | … | … | … |
| Semi-monthly cutoff 1 / cutoff 2 pair | semi-monthly | … | … | … | … | … | … | … | … |
| Zero-pay period with active benefits | any | … | … | … | … | … | … | … | … |

Boundary cases matter more than mid-band cases: every historical 0.01 discrepancy in the peer evidence came from a boundary or a rounding choice.

---

## 12. Route → function → test map

Useful when you change something and want to know what else moves.

| Route | Handler | Service | Store | Client caller | Covered by |
|---|---|---|---|---|---|
| `GET /api/health` | `handleHealth` | — | — | `fetchSandboxSnapshot` | AppServer integration |
| `GET /api/employees` | `handleEmployees` | `searchEmployees`, `listEmployees` | `FirestoreEmployeeStore.findAll` | `loadEmployees` | employee + Firestore REST suites |
| `GET /api/employees/{id}` | `handleEmployees` | `findEmployee` | `findById` | *(none — the dialog uses the in-memory row)* | employee suite |
| `POST /api/employees` | `handleEmployees` | `createEmployee` | `create` | employee form submit | employee + REST suites |
| `PUT /api/employees/{id}` | `handleEmployees` | `updateEmployee` | `update` | employee form submit | employee + REST suites |
| `GET /api/attendance` | `handleAttendance` | `findForEmployee` | `FirestoreEmployeeScheduleStore` + `FirestoreAttendanceStore` | `loadAttendance`, `loadEmployeeSandboxRecords`, `fetchSandboxSnapshot` | attendance + schedule-mapping suites |
| `POST /api/attendance` | `handleAttendance` | — | — | — | asserted 405 |
| `GET /api/reports/attendance` | `handleAttendanceReport` | `summarizeRawRecords` | same two | `loadAttendanceReport` | attendance + REST suites |
| `GET /api/workflows` | `handleWorkflows` | `listLeaveRequests`, `listTickets` | `FirestoreWorkflowStore` | `loadWorkflows`, `loadEmployeeSandboxRecords`, `fetchSandboxSnapshot` | workflow + Firestore workflow-store suites |
| `POST /api/workflows/leave` | `handleWorkflows` | `createLeaveRequest` | `createLeaveRequest` | leave form | workflow suite |
| `POST /api/workflows/tickets` | `handleWorkflows` | `createTicket` | `createTicket` | ticket form | workflow suite |
| `POST /api/workflows/leave/{id}/cancel` | `handleWorkflows` | `cancelLeaveRequest` | `cancelLeaveRequest` | `handleWorkflowCancel` | workflow suite |
| `POST /api/workflows/tickets/{id}/cancel` | `handleWorkflows` | `cancelTicket` | `cancelTicket` | `handleWorkflowCancel` | workflow suite |
| `POST /api/demo/seed` | `handleSeedDemo` | `DemoSeedService` | `createDocument` ×3 | `seedSyntheticDemoData` | Firestore workflow-store + integration |
| `GET /` + assets | `handleStaticFiles` | — | — | browser | E2E |

---

## 13. Practical next steps

Ordered so each step is verifiable on its own, and so none of them commits you to a payroll decision:

1. **Answer D-32 and D-23 first.** Tenant and role shape decide the Firestore schema, the Rules, and whether "Team"/"HR" views are a filter or a permission boundary. Everything else is cheaper to build after that.
2. **Decide D-01…D-04, then add a real `Schedule` entity** with effective dates, and an assignment lifecycle that ends a mapping instead of overwriting it. This is the missing keystone; attendance attribution currently rewrites history.
3. **Publish the rules update** for `demoEmployeeSchedules` in your own project when you are ready. The seed preflight will fail closed until you do, which is the intended behaviour.
4. **Then, and only then, a durable attendance writer** (D-07…D-09, D-16) with duplicate-key enforcement and a correction flow. Do not reintroduce a process-memory version as a product feature; the `POST /api/attendance` → 405 assertion exists to prevent that drift.
5. **Decide the rounding set (D-10, D-39, D-40) before writing the first money or time function.** Retrofitting rounding is where centavo defects come from.
6. **Fill the golden example table in §11.4** with independently checked numbers. The engine is only as trustworthy as that table.
7. **Only then** scaffold the versioned payroll ruleset and the immutable run snapshot — with `rulesetVersion` and effective dates on every stored result, and fail-closed behaviour outside covered periods.

Anything you want carried over from the handoff (five independent bases, versioned profile, `BigDecimal`/`HALF_UP`, whole-peso bracket bounds, boundary golden cases) belongs in steps 5–7. Nothing from its local JSON mode or its active preview belongs anywhere in the current product.

---

## Appendix A — counts and method inventory used for this document

| Artifact | Count | Note |
|---|---|---|
| Java files under `src/main/java/ph/motorph/web/` | 25 | 1,926 lines |
| Named Java methods | 166 | includes getters and constructors |
| `web/app.js` | 1,878 lines | 53 named functions + 73 event registrations |
| Other browser modules | 16 files | 537 lines |
| Browser/domain unit tests | 51 | `npm run test:web` |
| Java checks | 182 | `./test.sh`, plus startup guard |
| Playwright E2E | passing | `npm run test:e2e` |

Every named function in both codebases is catalogued above: Java in §3–§6, `app.js` in §7, and the 16 support modules in §8. Anonymous inline callbacks are enumerated as behaviour in §7.8 rather than as separate functions, because each is a one- or two-line handler for a specific control.
