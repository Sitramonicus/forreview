package ph.motorph.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.math.BigDecimal;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executors;

/** Java 11 HTTP backend for the browser-based MotorPH prototype. */
public final class AppServer {
    private final Path webRoot;
    private final String firebaseProjectId;
    private final String firestoreApiRoot;
    /** Demonstration role assignment; see RoleAssignment for why it is a seed and not policy. */
    private RoleAssignment roleAssignment = RoleAssignment.withDemonstrationSeatHolder();

    /**
     * Verifies Firebase ID tokens. Null in the isolated fixture runtime, which has no accounts.
     * When this is null nothing that needs a verified account may run - see {@link #authenticatedAccount}.
     */
    private FirebaseTokenVerifier tokenVerifier;

    /**
     * Account -> person -> seat links. In Firebase mode this is the record HR maintains; the fixture
     * runtime installs a single synthetic account that holds all five seats.
     */
    private SeatRegistry seatRegistry = SeatRegistry.fixtureAllSeatsEverywhere();

    /** True when running the isolated fixture runtime, where identity is a local operator choice. */
    private boolean fixtureIdentityMode = true;
    /** Payroll cycles (one per period, shared by every employee) and effective-dated rates. */
    private final List<PayrollPeriod> payrollPeriods = new java.util.ArrayList<>();
    private SalaryHistory salaryHistory = new SalaryHistory();
    private final EmployeeService localEmployeeService;
    private final EmployeeScheduleStore localScheduleStore;
    private final AttendanceStore localAttendanceStore;
    private final WorkflowStore localWorkflowStore;
    private PayrollStore localPayrollStore;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public AppServer() {
        this(System.getenv().getOrDefault("FIREBASE_PROJECT_ID", ""),
            Paths.get(System.getenv().getOrDefault("WEB_ROOT", "web")), null);
    }

    AppServer(String firebaseProjectId, Path webRoot) {
        this(firebaseProjectId, webRoot, null);
    }

    AppServer(String firebaseProjectId, Path webRoot, String firestoreApiRoot) {
        this(firebaseProjectId, webRoot, firestoreApiRoot, null, null);
    }

    /**
     * @param firebaseWebApiKey the project's Web API key, used to verify ID tokens. Required in
     *                          Firebase mode: without it the server cannot tell one account from
     *                          another and would be trusting a header again.
     * @param identityApiRoot   override for the Identity Toolkit base URL (tests only)
     */
    AppServer(String firebaseProjectId, Path webRoot, String firestoreApiRoot,
              String firebaseWebApiKey, String identityApiRoot) {
        this.firebaseProjectId = firebaseProjectId == null ? "" : firebaseProjectId.trim();
        String apiKey = firebaseWebApiKey == null ? "" : firebaseWebApiKey.trim();
        this.fixtureIdentityMode = this.firebaseProjectId.isEmpty();
        if (this.firebaseProjectId.isEmpty()) {
            this.localEmployeeService = new EmployeeService(new InMemoryEmployeeStore());
            this.localScheduleStore = new InMemoryEmployeeScheduleStore();
            this.localAttendanceStore = new InMemoryAttendanceStore();
            this.localWorkflowStore = new InMemoryWorkflowStore();
            // The employee file is the opening rate for every person, effective from the ruleset
            // start, so a payslip for a period before a raise still reads the old rate.
            seedSalaryHistoryFromDirectory(this.localEmployeeService,
                PayrollRuleset.StartOfFirstCoveredYear);
        } else {
            this.localEmployeeService = null;
            this.localScheduleStore = null;
            this.localAttendanceStore = null;
            this.localWorkflowStore = null;
        }
        if (!this.firebaseProjectId.isEmpty()) {
            if (apiKey.isEmpty()) {
                throw new IllegalStateException("FIREBASE_API_KEY is required. Without it the server "
                    + "cannot verify a Firebase ID token, and an unverified token is the same as no "
                    + "authentication at all. Set FIREBASE_API_KEY to the project's Web API key.");
            }
            this.tokenVerifier = new FirebaseTokenVerifier(this.firebaseProjectId, apiKey,
                identityApiRoot, java.time.Duration.ofMinutes(2));
            // No fixture seat holder in Firebase mode: every link comes from the seat registry.
            this.seatRegistry = new SeatRegistry(java.util.Collections.emptyMap());
        }
        if (webRoot == null) throw new IllegalArgumentException("webRoot must not be null");
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.firestoreApiRoot = firestoreApiRoot == null || firestoreApiRoot.trim().isEmpty()
            ? null : firestoreApiRoot.trim();
    }

    public static void main(String[] args) throws Exception {
        String projectId = System.getenv().getOrDefault("FIREBASE_PROJECT_ID", "").trim();
        boolean isolatedTestMode = "true".equalsIgnoreCase(System.getenv().getOrDefault("MOTORPH_TEST_MODE", "false"));
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
        boolean loopbackOnly = "127.0.0.1".equals(host) || "localhost".equalsIgnoreCase(host) || "::1".equals(host);
        if (projectId.isEmpty() && (!isolatedTestMode || !loopbackOnly)) {
            throw new IllegalStateException("FIREBASE_PROJECT_ID is required. Local-memory runtime is disabled; isolated tests must bind to loopback.");
        }
        AppServer app = new AppServer(projectId,
            Paths.get(System.getenv().getOrDefault("WEB_ROOT", "web")), null,
            System.getenv("FIREBASE_API_KEY"),
            System.getenv("FIREBASE_IDENTITY_API_ROOT"));
        app.start(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    HttpServer start(String host, int port) throws IOException {
        if (server != null) throw new IllegalStateException("server is already started");
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/me", this::handleMe);
        server.createContext("/api/me/links", this::handleMe);
        server.createContext("/api/fixtures/actors", this::handleFixtureActors);
        server.createContext("/api/employees", this::handleEmployees);
        server.createContext("/api/attendance", this::handleAttendance);
        server.createContext("/api/reports/attendance", this::handleAttendanceReport);
        server.createContext("/api/workflows", this::handleWorkflows);
        server.createContext("/api/payroll", this::handlePayroll);
        server.createContext("/api/demo/seed", this::handleSeedDemo);
        server.createContext("/", this::handleStaticFiles);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.start();
        System.out.println("MotorPH Java API listening on " + host + ":" + server.getAddress().getPort());
        System.out.println("Mode: " + (firebaseProjectId.isEmpty() ? "isolated synthetic test fixture" : "Firebase-authenticated user sandbox"));
        System.out.println("Serving browser files from " + webRoot);
        return server;
    }

    void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String mode = firebaseProjectId.isEmpty() ? "local-synthetic-demo" : "firebase-user-sandbox";
        sendJson(exchange, 200, "{\"status\":\"ok\",\"mode\":\"" + mode + "\"}");
    }

    /**
     * Who the caller is and what the matrix lets them do. The UI uses this to show or hide screens;
     * the server enforces the same matrix on every route, so hiding a screen is convenience only.
     */
    private void handleMe(HttpExchange exchange) throws IOException {
        // POST is only ever the link write; GET is the identity read. Both live here because they are
        // the same resource: who this session is.
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                handleLinkWrite(exchange);
            } catch (AccessControl.Denied denied) {
                sendJson(exchange, denied.status(), "{\"error\":\"" + escapeJson(denied.getMessage()) + "\"}");
            } catch (IllegalArgumentException | NoSuchElementException ex) {
                sendJson(exchange, ex instanceof NoSuchElementException ? 404 : 400,
                    "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
            }
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            AccessControl.Actor actor = actorFor(exchange);
            EmployeeService service = servicesFor(exchange).employeeService;
            Optional<Employee> self = service.findEmployee(actor.employeeNumber());
            AccessControl control = accessControlFor(exchange);

            JsonObject o = new JsonObject();
            o.addProperty("employeeNumber", actor.employeeNumber());
            o.addProperty("role", actor.role().name());
            o.addProperty("roleLabel", actor.role().label());
            o.addProperty("name", self.map(Employee::getName).orElse(""));
            o.addProperty("position", self.map(Employee::getRole).orElse(""));
            o.addProperty("department", self.map(Employee::getDepartment).orElse(""));
            o.addProperty("supervisorId", self.map(Employee::getSupervisorId).orElse(null));
            o.addProperty("hasPayProfile", self.map(Employee::hasPayProfile).orElse(false));

            JsonArray reports = new JsonArray();
            for (Employee candidate : service.listEmployees()) {
                if (actor.employeeNumber().equals(candidate.getSupervisorId())) {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", candidate.getId());
                    row.addProperty("name", candidate.getName());
                    reports.add(row);
                }
            }
            o.add("directReports", reports);
            o.add("permissions", permissionMatrixFor(actor));
            o.addProperty("roleSeedIsDemonstration", true);

            // What this session can switch between. This is what makes one account enough: the
            // browser renders a switch for every seat instead of asking for another login.
            o.addProperty("identityMode", fixtureIdentityMode ? "FIXTURE" : "FIREBASE");
            o.add("seats", seatsToJson(actor.employeeNumber(), actor.role()));
            o.add("people", peopleToJson(exchange, service));
            sendJson(exchange, 200, o.toString());
        } catch (AccessControl.Denied denied) {
            sendJson(exchange, denied.status(), "{\"error\":\"" + escapeJson(denied.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /** The 12-row matrix as it applies to this actor, so the UI never re-implements it. */
    private static JsonObject permissionMatrixFor(AccessControl.Actor actor) {
        JsonObject matrix = new JsonObject();
        for (Policy.Entity entity : Policy.Entity.values()) {
            Policy.Scope scope = Policy.scope(entity, actor.role());
            JsonObject cell = new JsonObject();
            cell.addProperty("scope", scope.name());
            cell.addProperty("read", Policy.allows(entity, actor.role(), Policy.Operation.READ));
            cell.addProperty("create", Policy.allows(entity, actor.role(), Policy.Operation.CREATE));
            cell.addProperty("update", Policy.allows(entity, actor.role(), Policy.Operation.UPDATE));
            cell.addProperty("delete", Policy.allows(entity, actor.role(), Policy.Operation.DELETE));
            cell.addProperty("selfOnly", Policy.isSelfOnly(entity, actor.role()));
            cell.addProperty("teamScoped", Policy.isTeamScoped(entity, actor.role()));
            cell.addProperty("needsApproval", scope == Policy.Scope.APPROVAL_REQUIRED);
            matrix.add(entity.name(), cell);
        }
        return matrix;
    }

    /**
     * Payroll cycles. One period covers every employee, which is the point: a run references a
     * period, and a period's dates, cadence and open/closed state are the same for everybody.
     */
    private void handlePayrollPeriods(HttpExchange exchange, String path, String method) throws IOException {
        if ("GET".equals(method) && ("/api/payroll/periods".equals(path) || "/api/payroll/periods/".equals(path))) {
            StringBuilder items = new StringBuilder();
            int count = 0;
            synchronized (payrollPeriods) {
                for (PayrollPeriod period : payrollPeriods) {
                    if (count > 0) items.append(',');
                    items.append(periodToJson(period));
                    count++;
                }
            }
            sendJson(exchange, 200, "{\"items\":[" + items + "],\"count\":" + count + "}");
            return;
        }
        if ("GET".equals(method) && path.startsWith("/api/payroll/periods/")) {
            String id = path.substring("/api/payroll/periods/".length());
            Optional<PayrollPeriod> found = findPeriod(id);
            if (!found.isPresent()) {
                sendJson(exchange, 404, "{\"error\":\"Payroll period not found\"}");
                return;
            }
            sendJson(exchange, 200, "{\"item\":" + periodToJson(found.get()) + "}");
            return;
        }
        if ("POST".equals(method) && ("/api/payroll/periods".equals(path) || "/api/payroll/periods/".equals(path))) {
            requireAccess(exchange, Policy.Entity.PAYROLL, Policy.Operation.CREATE, null);
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "cadence", "periodStart", "periodEnd", "payPeriod");
            String cadenceText = optionalString(body, "cadence").isEmpty()
                ? optionalString(body, "payPeriod") : optionalString(body, "cadence");
            PayrollPeriod period = PayrollPeriod.open(
                PayrollPeriod.cadenceFrom(cadenceText),
                PayrollPeriod.dateFrom(optionalString(body, "periodStart"), "periodStart"),
                PayrollPeriod.dateFrom(optionalString(body, "periodEnd"), "periodEnd"),
                java.time.Instant.now().toString());
            synchronized (payrollPeriods) {
                if (findPeriod(period.id()).isPresent()) {
                    throw new IllegalStateException("Payroll period " + period.id() + " already exists.");
                }
                payrollPeriods.add(period);
            }
            sendJson(exchange, 201, "{\"item\":" + periodToJson(period) + "}");
            return;
        }
        if ("POST".equals(method) && path.endsWith("/close") && path.startsWith("/api/payroll/periods/")) {
            requireAccess(exchange, Policy.Entity.PAYROLL, Policy.Operation.UPDATE, null);
            String id = path.substring("/api/payroll/periods/".length(), path.length() - "/close".length());
            synchronized (payrollPeriods) {
                for (int i = 0; i < payrollPeriods.size(); i++) {
                    if (payrollPeriods.get(i).id().equals(id)) {
                        PayrollPeriod closed = payrollPeriods.get(i)
                            .withStatus(PayrollPeriod.Status.CLOSED, java.time.Instant.now().toString());
                        payrollPeriods.set(i, closed);
                        sendJson(exchange, 200, "{\"item\":" + periodToJson(closed) + "}");
                        return;
                    }
                }
            }
            sendJson(exchange, 404, "{\"error\":\"Payroll period not found\"}");
            return;
        }
        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
    }

    private Optional<PayrollPeriod> findPeriod(String id) {
        synchronized (payrollPeriods) {
            for (PayrollPeriod period : payrollPeriods) {
                if (period.id().equals(id)) return Optional.of(period);
            }
        }
        return Optional.empty();
    }

    private Optional<PayrollPeriod> findPeriodContaining(String periodEnd) {
        if (periodEnd == null || periodEnd.trim().isEmpty()) return Optional.empty();
        LocalDate end = LocalDate.parse(periodEnd.trim());
        synchronized (payrollPeriods) {
            for (PayrollPeriod period : payrollPeriods) {
                if (period.end().equals(end)) return Optional.of(period);
            }
        }
        return Optional.empty();
    }

    private static String salaryRateToJson(SalaryHistory.SalaryRate rate) {
        JsonObject o = new JsonObject();
        o.addProperty("employeeId", rate.employeeId());
        o.addProperty("effectiveFrom", rate.effectiveFrom().toString());
        o.addProperty("monthlyBasic", Money.round(rate.monthlyBasic()).toPlainString());
        o.addProperty("riceSubsidy", Money.round(rate.riceSubsidy()).toPlainString());
        o.addProperty("phoneAllowance", Money.round(rate.phoneAllowance()).toPlainString());
        o.addProperty("clothingAllowance", Money.round(rate.clothingAllowance()).toPlainString());
        o.addProperty("reason", rate.reason());
        return o.toString();
    }

    private static String periodToJson(PayrollPeriod period) {
        JsonObject o = new JsonObject();
        o.addProperty("id", period.id());
        o.addProperty("payPeriod", period.payPeriod());
        o.addProperty("periodStart", period.startText());
        o.addProperty("periodEnd", period.endText());
        o.addProperty("status", period.status().name());
        o.addProperty("createdAt", period.createdAt());
        if (period.closedAt() != null) o.addProperty("closedAt", period.closedAt());
        o.addProperty("acceptsRuns", period.acceptsRuns());
        return o.toString();
    }

    /** A saved run is visible only if it contains a payslip the caller may read. */
    void requireRunVisible(HttpExchange exchange, String runId) {
        AccessControl.Actor actor = actorFor(exchange);
        if (Policy.isCompanyWideRead(Policy.Entity.PAYROLL, actor.role())) return;
        AccessControl control = accessControlFor(exchange);
        PayrollRun run = payrollStoreFor(exchange).findAll().stream()
            .filter(candidate -> candidate.id().equals(runId)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Payroll run not found."));
        for (Payslip slip : run.payslips()) {
            if (controlsRecord(actor, control, slip.employeeId())) return;
        }
        throw new AccessControl.Denied("That payroll run contains no payslip you may read.", 403);
    }

    /**
     * Whether an employee row may appear in this actor's directory listing.
     *
     * <ol>
     *   <li>Company-wide read on Employee: every row.</li>
     *   <li>Otherwise their own row is always visible.</li>
     *   <li>Supervisors additionally see their direct reports, which is the team roster the design
     *       document gives them - the same people they may read attendance and payslips for.</li>
     * </ol>
     */
    static boolean canSeeEmployeeRow(AccessControl.Actor actor, AccessControl control, String rowEmployeeNumber) {
        if (Policy.isCompanyWideRead(Policy.Entity.EMPLOYEE, actor.role())) return true;
        if (rowEmployeeNumber.equals(actor.employeeNumber())) return true;
        return Policy.isTeamScoped(Policy.Entity.ATTENDANCE, actor.role())
            && control.isDirectReport(actor, rowEmployeeNumber);
    }

    /** Attendance and payslips are R, or R/U (Team), or R (Self) depending on the role. */
    void requireTeamScopedRead(HttpExchange exchange, Policy.Entity entity, String targetEmployeeNumber) {
        AccessControl.Actor actor = actorFor(exchange);
        if (Policy.isCompanyWideRead(entity, actor.role())) return;
        AccessControl control = accessControlFor(exchange);
        if (controlsRecord(actor, control, targetEmployeeNumber)) return;
        if (Policy.isSelfOnly(entity, actor.role())) {
            throw new AccessControl.Denied("You may only see your own " + entity.label().toLowerCase() + ".", 403);
        }
        throw new AccessControl.Denied("That employee is not one of your direct reports.", 403);
    }

    /**
     * Reading one employee's directory row. Company-wide roles read anything; a Supervisor or an
     * Employee reads themselves, and a Supervisor additionally reads their own direct reports
     * (which is exactly the team roster the design document gives them).
     */
    void requireDirectoryRead(HttpExchange exchange, String targetEmployeeNumber) {
        AccessControl.Actor actor = actorFor(exchange);
        AccessControl control = accessControlFor(exchange);
        if (Policy.isCompanyWideRead(Policy.Entity.EMPLOYEE, actor.role())) return;
        if (controlsRecord(actor, control, targetEmployeeNumber)) return;
        throw new AccessControl.Denied("You may only see your own employee record, "
            + "or one of your direct reports'.", 403);
    }

    /** Whether the actor may read an employee's own team-scoped records (roster, attendance, payslip). */
    static boolean controlsRecord(AccessControl.Actor actor, AccessControl control, String targetEmployeeNumber) {
        if (targetEmployeeNumber == null) return false;
        if (actor.employeeNumber().equals(targetEmployeeNumber)) return true;
        return (Policy.isTeamScoped(Policy.Entity.ATTENDANCE, actor.role())
            || Policy.isTeamScoped(Policy.Entity.PAYSLIP, actor.role()))
            && control.isDirectReport(actor, targetEmployeeNumber);
    }

    /**
     * HR links an account to a person, or changes the seats that person holds there.
     *
     * <p>This is the route that makes one login enough for somebody who does two jobs: it can be called
     * again with a second employee number for the same account, and the seat switcher will then offer
     * both people.
     *
     * <p>Only HR and System Admin may call it. Nobody may edit their own links: an account that could
     * grant itself a seat would make the whole seat model decorative.
     */
    private void handleLinkWrite(HttpExchange exchange) throws IOException {
        JsonObject body = readJsonBody(exchange);
        AccessControl.Actor actor = actorFor(exchange);

        if (Policy.scope(Policy.Entity.EMPLOYEE, actor.role()) == Policy.Scope.NONE
            || !Policy.allows(Policy.Entity.EMPLOYEE, actor.role(), Policy.Operation.UPDATE)) {
            throw new AccessControl.Denied("Only HR can link an account to an employee.", 403);
        }

        String accountId = requiredString(body, "accountId").trim();
        String employeeNumber = requiredString(body, "employeeNumber").trim();
        if (!employeeNumber.matches("[0-9]{5}")) {
            throw new IllegalArgumentException("An employee number is five digits, e.g. 10016.");
        }
        if (accountId.equals(currentAccountId(exchange))) {
            // Self-service seat granting would defeat the model, and it is also how an HR clerk would
            // quietly promote themselves. Refused with the reason, not silently ignored.
            throw new AccessControl.Denied("You cannot change your own account's links. "
                + "Ask another HR user or System Admin.", 403);
        }
        if (!servicesFor(exchange).employeeService.findEmployee(employeeNumber).isPresent()) {
            throw new NoSuchElementException("No employee " + employeeNumber + " to link to.");
        }

        List<RoleAssignment.Seat> seats = new ArrayList<>();
        for (RoleAssignment.Seat seat : roleAssignment.seatsOf(employeeNumber)) {
            seats.add(seat);
        }
        SeatRegistry.Link link = seatRegistry.link(accountId, employeeNumber, seats);
        sendJson(exchange, 201, "{\"link\":" + linkToJson(link) + "}");
    }

    private String currentAccountId(HttpExchange exchange) {
        if (fixtureIdentityMode) return "fixture-account";
        FirebaseTokenVerifier.Account account = authenticatedAccount(exchange);
        return account.uid();
    }

    private static String linkToJson(SeatRegistry.Link link) {
        JsonObject o = new JsonObject();
        o.addProperty("accountId", link.accountId());
        o.addProperty("employeeNumber", link.employeeNumber());
        StringBuilder roles = new StringBuilder();
        for (RoleAssignment.Seat seat : link.seats()) {
            if (roles.length() > 0) roles.append(',');
            roles.append(seat.role().name());
        }
        o.addProperty("seats", roles.toString());
        return o.toString();
    }

    /** Every seat the current person holds, and which one this request is acting under. */
    private JsonArray seatsToJson(String employeeNumber, Role active) {
        JsonArray seats = new JsonArray();
        for (RoleAssignment.Seat seat : seatsOf(employeeNumber)) {
            JsonObject row = new JsonObject();
            row.addProperty("role", seat.role().name());
            row.addProperty("label", seat.role().label());
            row.addProperty("basis", seat.basis());
            row.addProperty("active", seat.role() == active);
            row.addProperty("rank", seat.role().rank());
            seats.add(row);
        }
        return seats;
    }

    /** Every person this account may act as. One entry for most people. */
    private JsonArray peopleToJson(HttpExchange exchange, EmployeeService service) {
        JsonArray people = new JsonArray();
        for (String personNumber : peopleOf(exchange)) {
            JsonObject row = new JsonObject();
            row.addProperty("employeeNumber", personNumber);
            Optional<Employee> employee = service.findEmployee(personNumber);
            row.addProperty("name", employee.map(Employee::getName).orElse(""));
            row.addProperty("position", employee.map(Employee::getRole).orElse(""));
            row.addProperty("seats", RoleAssignment.describe(seatsOf(personNumber)));
            people.add(row);
        }
        return people;
    }

    /**
     * The seats held by a person.
     *
     * <p>Fixture mode answers from the in-process seed. Firebase mode answers from the seat registry,
     * keyed by the <b>verified</b> account - so the list a client sees is the list the server will
     * enforce, not a copy the client supplied.
     */
    List<RoleAssignment.Seat> seatsOf(String employeeNumber) {
        return roleAssignment.seatsOf(employeeNumber);
    }

    /**
     * The people this request's account may act as.
     *
     * <p>In fixture mode that is exactly the one person named in the request: the operator picks a
     * person from {@code /api/fixtures/actors}, and reporting the whole seated roster here would tell
     * an ordinary employee who the supervisors and payroll staff are for no reason.
     */
    List<String> peopleOf(HttpExchange exchange) {
        if (fixtureIdentityMode) {
            String current = header(exchange, "X-MotorPH-Employee");
            if (current == null) current = header(exchange, "X-Firebase-EID");
            if (current == null || current.trim().isEmpty()) return new ArrayList<>();
            return java.util.Collections.singletonList(current.trim());
        }
        String claimed = header(exchange, "X-Firebase-UID");
        List<SeatRegistry.Link> links = seatRegistry.linksOf(claimed == null ? "" : claimed);
        List<String> numbers = new ArrayList<>();
        for (SeatRegistry.Link link : links) numbers.add(link.employeeNumber());
        return numbers;
    }

    private void handleEmployees(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method)) {
                RequestServices services = servicesFor(exchange);
                if ("/api/employees".equals(path) || "/api/employees/".equals(path)) {
                    String query = queryParameter(exchange.getRequestURI().getRawQuery(), "q");
                    List<Employee> matches = services.employeeService.searchEmployees(query);
                    // RBAC §Employee: a Supervisor reaches only their direct reports, an Employee
                    // only their own row. Filtering here means the reply cannot contain a record
                    // the caller may not read, rather than relying on the browser to hide it.
                    AccessControl.Actor actor = actorFor(exchange);
                    AccessControl control = accessControlFor(exchange);
                    List<Employee> visible = new ArrayList<>();
                    for (Employee candidate : matches) {
                        // Visibility comes from the Employee cell of the matrix, not from a payslip
                        // proxy, so changing what a payslip allows cannot silently widen the roster.
                        if (canSeeEmployeeRow(actor, control, candidate.getId())) {
                            visible.add(candidate);
                        }
                    }
                    // `total` is the roster size, which is itself information. A caller who may see
                    // nobody must not learn how many people MotorPH employs, so it is reported as the
                    // number of rows they are allowed to know about.
                    int total = Policy.isCompanyWideRead(Policy.Entity.EMPLOYEE, actor.role())
                        ? services.employeeService.listEmployees().size()
                        : visible.size();
                    sendJson(exchange, 200, employeesToJson(visible, total));
                    return;
                }
                if (!path.startsWith("/api/employees/")) {
                    sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
                    return;
                }
                String id = path.substring("/api/employees/".length());
                if (id.isEmpty() || id.contains("/")) {
                    sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
                    return;
                }
                Optional<Employee> employee = services.employeeService.findEmployee(id);
                if (!employee.isPresent()) {
                    sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
                    return;
                }
                // The directory cell for a Supervisor is "R (Self)", but a supervisor legitimately
                // needs the name and placement of their direct reports — they see them in the team
                // roster. That access comes from the team-scoped entities, not from Employee.
                requireDirectoryRead(exchange, id);
                sendJson(exchange, 200, "{\"item\":" + employeeToJson(employee.get()) + "}");
                return;
            }
            if ("POST".equals(method) && "/api/employees".equals(path)) {
                RequestServices services = servicesFor(exchange);
                requireAccess(exchange, Policy.Entity.EMPLOYEE, Policy.Operation.CREATE, null);
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "name", "department", "role", "location", "status",
                    "monthlyBasic", "riceSubsidy", "phoneAllowance", "clothingAllowance");
                Employee created = services.employeeService.createEmployee(requiredString(body, "name"),
                    requiredString(body, "department"), requiredString(body, "role"),
                    requiredString(body, "location"), requiredString(body, "status"),
                    numberOr(body, "monthlyBasic", null), numberOr(body, "riceSubsidy", null),
                    numberOr(body, "phoneAllowance", null), numberOr(body, "clothingAllowance", null));
                sendJson(exchange, 201, "{\"item\":" + employeeToJson(created) + "}");
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/api/employees/")) {
                RequestServices services = servicesFor(exchange);
                String id = path.substring("/api/employees/".length());
                if (id.isEmpty() || id.contains("/")) throw new IllegalArgumentException("Employee ID is invalid.");
                requireAccess(exchange, Policy.Entity.EMPLOYEE, Policy.Operation.UPDATE, id);
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "name", "department", "role", "location", "status",
                    "monthlyBasic", "riceSubsidy", "phoneAllowance", "clothingAllowance");
                Employee updated = services.employeeService.updateEmployee(id, requiredString(body, "name"),
                    requiredString(body, "department"), requiredString(body, "role"),
                    requiredString(body, "location"), requiredString(body, "status"),
                    numberOr(body, "monthlyBasic", null), numberOr(body, "riceSubsidy", null),
                    numberOr(body, "phoneAllowance", null), numberOr(body, "clothingAllowance", null));
                sendJson(exchange, 200, "{\"item\":" + employeeToJson(updated) + "}");
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        } catch (AccessControl.Denied denied) {
            // Must come before the IllegalArgumentException catch: Denied extends it, and a refusal
            // reported as a 400 "bad request" would look like a malformed form rather than a refusal.
            sendJson(exchange, denied.status(), "{\"error\":\"" + escapeJson(denied.getMessage()) + "\"}");
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /**
     * The roster a fixture-mode operator may act as.
     *
     * <p>This exists because of an ordering problem: RBAC means an account with no employee number
     * sees no employees, so a fixture-mode screen has no way to offer a choice of employee number.
     * One endpoint answers only that question - the list of seeded employee numbers and names - and it
     * exists only in isolated fixture mode. In production the process refuses to start without
     * Firebase, so this route is unreachable there; the check below is the belt to that braces.
     *
     * <p>It deliberately returns no pay, no attendance, and no role: picking a number does not tell the
     * browser what that number may do.
     */
    private void handleFixtureActors(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        if (!firebaseProjectId.isEmpty() || localEmployeeService == null) {
            sendJson(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }
        JsonArray items = new JsonArray();
        // First, explicitly labelled: one person holding all five seats, so every role can be looked
        // at from a single session without creating any accounts.
        JsonObject demonstration = new JsonObject();
        demonstration.addProperty("id", RoleAssignment.DEMONSTRATION_EMPLOYEE);
        demonstration.addProperty("name", "Demonstration account - all five seats");
        demonstration.addProperty("position", "Every role at once, for testing");
        demonstration.addProperty("synthetic", true);
        items.add(demonstration);
        for (Employee employee : localEmployeeService.listEmployees()) {
            JsonObject row = new JsonObject();
            row.addProperty("id", employee.getId());
            row.addProperty("name", employee.getName());
            row.addProperty("position", employee.getRole());
            row.addProperty("synthetic", false);
            items.add(row);
        }
        JsonObject response = new JsonObject();
        response.add("items", items);
        response.addProperty("count", items.size());
        sendJson(exchange, 200, response.toString());
    }

    private void handleAttendance(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        String employeeId = queryParameter(exchange.getRequestURI().getRawQuery(), "employeeId");
        if (employeeId == null || employeeId.trim().isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"employeeId is required\"}");
            return;
        }
        try {
            requireTeamScopedRead(exchange, Policy.Entity.ATTENDANCE, employeeId.trim());
        } catch (AccessControl.Denied denied) {
            sendJson(exchange, denied.status(), "{\"error\":\"" + escapeJson(denied.getMessage()) + "\"}");
            return;
        }
        try {
            RequestServices services = servicesFor(exchange);
            LocalDate from = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "from"));
            LocalDate to = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                services.scheduleStore, services.attendanceStore);
            List<AttendanceRecord> records = attendanceService.findForEmployee(employeeId, from, to);
            sendJson(exchange, 200, attendanceToJson(records));
        } catch (DateTimeParseException ex) {
            sendJson(exchange, 400, "{\"error\":\"Dates must use YYYY-MM-DD\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (java.util.NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    private void handleAttendanceReport(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            RequestServices services = servicesFor(exchange);
            String query = exchange.getRequestURI().getRawQuery();
            LocalDate from = parseOptionalDate(queryParameter(query, "from"));
            LocalDate to = parseOptionalDate(queryParameter(query, "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                services.scheduleStore, services.attendanceStore);
            List<RawAttendanceSummary> summaries = attendanceService.summarizeRawRecords(from, to);
            sendJson(exchange, 200, attendanceSummaryToJson(summaries, from, to));
        } catch (DateTimeParseException ex) {
            sendJson(exchange, 400, "{\"error\":\"Dates must use YYYY-MM-DD\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    private void handleWorkflows(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/workflows".equals(path)) {
                WorkflowService service = workflowsFor(exchange);
                sendJson(exchange, 200, workflowsToJson(service.listLeaveRequests(), service.listTickets(),
                    firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                return;
            }
            if (!"POST".equals(method)) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            WorkflowService service = workflowsFor(exchange);
            if ("/api/workflows/leave".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "leaveType", "startDate", "endDate", "note");
                DemoLeaveRequest request = service.createLeaveRequest(
                    requiredString(body, "employeeId"), requiredString(body, "leaveType"),
                    requiredString(body, "startDate"), requiredString(body, "endDate"), optionalString(body, "note"));
                sendJson(exchange, 201, itemToJson(leaveToJson(request),
                    firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                return;
            }
            if ("/api/workflows/tickets".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "ticketType", "summary");
                DemoTicket ticket = service.createTicket(requiredString(body, "employeeId"),
                    requiredString(body, "ticketType"), requiredString(body, "summary"));
                sendJson(exchange, 201, itemToJson(ticketToJson(ticket),
                    firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                return;
            }
            String[] segments = path.substring("/api/workflows/".length()).split("/");
            if (segments.length == 3 && "cancel".equals(segments[2])) {
                if ("leave".equals(segments[0])) {
                    DemoLeaveRequest request = service.cancelLeaveRequest(segments[1]);
                    sendJson(exchange, 200, itemToJson(leaveToJson(request),
                        firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                    return;
                }
                if ("tickets".equals(segments[0])) {
                    DemoTicket ticket = service.cancelTicket(segments[1]);
                    sendJson(exchange, 200, itemToJson(ticketToJson(ticket),
                        firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                    return;
                }
            }
            sendJson(exchange, 404, "{\"error\":\"Workflow endpoint not found\"}");
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    private WorkflowService workflowsFor(HttpExchange exchange) {
        if (firebaseProjectId.isEmpty()) return new WorkflowService(localEmployeeService, localWorkflowStore);
        FirestoreRestClient client = firestoreClientFor(exchange);
        EmployeeService employees = new EmployeeService(new FirestoreEmployeeStore(client));
        return new WorkflowService(employees, new FirestoreWorkflowStore(client));
    }

    private static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new RequestException(415, "Content-Type must be application/json.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(8193);
        if (bytes.length > 8192) throw new RequestException(413, "Request body is too large.");
        try {
            JsonElement parsed = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new RequestException(400, "JSON body must be an object.");
            return parsed.getAsJsonObject();
        } catch (JsonParseException ex) {
            throw new RequestException(400, "Request body is not valid JSON.");
        }
    }

    private static void requireOnlyKeys(JsonObject body, String... allowed) {
        java.util.Set<String> keys = new java.util.HashSet<>(java.util.Arrays.asList(allowed));
        for (String key : body.keySet()) {
            if (!keys.contains(key)) throw new IllegalArgumentException("Unexpected request field: " + key);
        }
    }

    private static String requiredString(JsonObject body, String field) {
        JsonElement value = body.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " is required and must be text.");
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject body, String field) {
        JsonElement value = body.get(field);
        if (value == null || value.isJsonNull()) return "";
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(field + " must be text.");
        }
        return value.getAsString();
    }

    private static String itemToJson(JsonObject item, String storage) {
        JsonObject response = new JsonObject();
        response.add("item", item);
        response.addProperty("storage", storage);
        return response.toString();
    }

    private static String workflowsToJson(List<DemoLeaveRequest> leaveRequests, List<DemoTicket> tickets, String storage) {
        JsonObject response = new JsonObject();
        JsonArray leaves = new JsonArray();
        for (DemoLeaveRequest request : leaveRequests) leaves.add(leaveToJson(request));
        JsonArray ticketItems = new JsonArray();
        for (DemoTicket ticket : tickets) ticketItems.add(ticketToJson(ticket));
        response.add("leaveRequests", leaves);
        response.add("tickets", ticketItems);
        response.addProperty("storage", storage);
        return response.toString();
    }

    private static JsonObject leaveToJson(DemoLeaveRequest request) {
        JsonObject item = new JsonObject();
        item.addProperty("id", request.getId());
        item.addProperty("employeeId", request.getEmployeeId());
        item.addProperty("employeeName", request.getEmployeeName());
        item.addProperty("leaveType", request.getLeaveType());
        item.addProperty("startDate", request.getStartDate());
        item.addProperty("endDate", request.getEndDate());
        item.addProperty("note", request.getNote());
        item.addProperty("status", request.getStatus());
        item.addProperty("submittedAt", request.getSubmittedAt());
        item.addProperty("cancelledAt", request.getCancelledAt());
        return item;
    }

    private static JsonObject ticketToJson(DemoTicket ticket) {
        JsonObject item = new JsonObject();
        item.addProperty("id", ticket.getId());
        item.addProperty("employeeId", ticket.getEmployeeId());
        item.addProperty("employeeName", ticket.getEmployeeName());
        item.addProperty("ticketType", ticket.getTicketType());
        item.addProperty("queue", ticket.getQueue());
        item.addProperty("summary", ticket.getSummary());
        item.addProperty("status", ticket.getStatus());
        item.addProperty("submittedAt", ticket.getSubmittedAt());
        item.addProperty("cancelledAt", ticket.getCancelledAt());
        return item;
    }

    private void handlePayroll(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/payroll/salary-history".equals(path)) {
                String employeeId = queryParameter(exchange.getRequestURI().getRawQuery(), "employeeId");
                AccessControl.Actor actor = actorFor(exchange);
                StringBuilder items = new StringBuilder();
                int count = 0;
                for (SalaryHistory.SalaryRate rate : salaryHistory.all()) {
                    if (!employeeId.isEmpty() && !rate.employeeId().equals(employeeId)) continue;
                    // A supervisor or employee sees only rates they may read; payroll and HR see all.
                    if (!Policy.isCompanyWideRead(Policy.Entity.PAYROLL, actor.role())
                        && !rate.employeeId().equals(actor.employeeNumber())) {
                        continue;
                    }
                    if (count > 0) items.append(',');
                    items.append(salaryRateToJson(rate));
                    count++;
                }
                sendJson(exchange, 200, "{\"items\":[" + items + "],\"count\":" + count + "}");
                return;
            }
            if ("POST".equals(method) && "/api/payroll/salary-history".equals(path)) {
                submitSalaryRate(exchange);
                return;
            }
            if ("/api/payroll/periods".equals(path) || path.startsWith("/api/payroll/periods/")) {
                handlePayrollPeriods(exchange, path, method);
                return;
            }
            if ("GET".equals(method) && "/api/payroll/rules".equals(path)) {
                // Rates in force are needed by anybody who may read a payslip: a payslip that cannot
                // name the rates it used is not auditable. The Annex E bracket tables are a different
                // question - the matrix gives Tax Rates to System Admin alone - so they are withheld
                // from everybody else rather than the whole ruleset being refused.
                AccessControl.Actor actor = actorFor(exchange);
                AccessControl control = accessControlFor(exchange);
                boolean mayReadPayslip = Policy.scope(Policy.Entity.PAYSLIP, actor.role()) != Policy.Scope.NONE
                    || Policy.scope(Policy.Entity.PAYROLL, actor.role()) != Policy.Scope.NONE;
                boolean mayMaintainTables = Policy.allows(Policy.Entity.TAX_RATES, actor.role(),
                    Policy.Operation.READ);
                if (!mayReadPayslip && !mayMaintainTables) {
                    throw new AccessControl.Denied(actor.role().label()
                        + " has no access to payroll rates.", 403);
                }
                int year = parseIntYear(queryParameter(exchange.getRequestURI().getRawQuery(), "year"));
                PayrollRuleset ruleset = PayrollRuleset.forYear(year);
                JsonObject rules = JsonParser.parseString(rulesetToJson(ruleset)).getAsJsonObject();
                if (!mayMaintainTables) {
                    // Withheld, and said so - a client should not have to infer that an absent key
                    // means "not allowed" rather than "not applicable".
                    rules.remove("monthlyTaxBrackets");
                    rules.remove("semiMonthlyTaxBrackets");
                    rules.addProperty("taxTablesWithheld", "The withholding bracket tables are "
                        + "maintained by System Admin. The rates and brackets applied to a payslip are "
                        + "on the payslip itself.");
                }
                sendJson(exchange, 200, rules.toString());
                return;
            }
            if ("GET".equals(method) && "/api/payroll/timesheet".equals(path)) {
                String employeeId = queryParameter(exchange.getRequestURI().getRawQuery(), "employeeId");
                if (employeeId == null || employeeId.trim().isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"employeeId is required\"}");
                    return;
                }
                RequestServices services = servicesFor(exchange);
                AttendanceService attendance = new AttendanceService(services.employeeService,
                    services.scheduleStore, services.attendanceStore);
                LocalDate from = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "from"));
                LocalDate to = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "to"));
                TimesheetService.Summary summary = new TimesheetService(attendance).summarise(employeeId, from, to);
                sendJson(exchange, 200, timesheetToJson(summary));
                return;
            }
            if ("POST".equals(method) && "/api/payroll/compare".equals(path)) {
                // Reference-only: our payslip is already computed above by PayrollService. This route
                // adds an independent second opinion and can never change the calculation.
                PayrollService.Input input = payrollInputFrom(readJsonBody(exchange));
                Payslip slip = new PayrollService().compute(input, PayrollRuleset.forYear(periodYear(input)));
                if (!comparableToReference(input)) {
                    sendJson(exchange, 200, comparisonUnavailableToJson(slip, comparisonBasisDifference(input)));
                    return;
                }
                if ("SEMI_MONTHLY".equalsIgnoreCase(input.payPeriod)) {
                    // Their semi-monthly branch prices a whole month: it doubles both the basic pay
                    // and the allowances it is given. Comparing that against one of our cutoffs
                    // would report a difference that is only a difference of basis, so we refuse
                    // to show a comparison at all rather than print a misleading delta.
                    sendJson(exchange, 200, comparisonUnavailableToJson(slip,
                        "sweldongpinoy.com only prices a whole month. Set the pay period to Monthly "
                            + "to compare, or read its semi-monthly figure as two cutoffs combined."));
                    return;
                }
                SweldongPinoyClient.Comparison comparison = sweldongPinoyClient(input).compare(
                    input.payPeriod, referenceBasic(input),
                    Money.round(input.taxableAllowance),
                    Money.round(input.riceSubsidy.add(input.phoneAllowance).add(input.clothingAllowance)),
                    periodDays(input), input.overtimeHours, input.nightDifferentialHours);
                sendJson(exchange, 200, comparisonToJson(slip, comparison));
                return;
            }
            if ("POST".equals(method) && ("/api/payroll/preview".equals(path) || "/api/payroll/runs".equals(path))) {
                JsonObject body = readJsonBody(exchange);
                PayrollService.Input input = payrollInputFrom(body);
                // Computing a payslip is reading a payslip: the matrix says who may do it for whom.
                requireTeamScopedRead(exchange, Policy.Entity.PAYSLIP, input.employeeId);
                requireAccess(exchange, Policy.Entity.PAYROLL,
                    "/api/payroll/runs".equals(path) ? Policy.Operation.CREATE : Policy.Operation.READ,
                    input.employeeId);
                // A saved run belongs to a payroll cycle, never to dates typed per payslip. If the
                // caller names a period, the run must carry that period's dates and cadence; if it
                // names none, the dates must still match an open cycle. This is what stops two
                // employees being paid for two different ranges in the same run.
                String periodId = optionalString(body, "payrollPeriodId");
                if (!periodId.isEmpty()) {
                    PayrollPeriod period = findPeriod(periodId).orElseThrow(
                        () -> new NoSuchElementException("Payroll period not found."));
                    if (!period.acceptsRuns()) {
                        throw new IllegalStateException("Payroll period " + period.id()
                            + " is closed; no further runs may be computed for it.");
                    }
                    if (!period.payPeriod().equalsIgnoreCase(input.payPeriod)
                        || !period.startText().equals(input.periodStart)
                        || !period.endText().equals(input.periodEnd)) {
                        throw new IllegalArgumentException("This run does not match payroll period "
                            + period.id() + " (" + period.payPeriod() + " " + period.startText()
                            + " to " + period.endText() + ").");
                    }
                } else if ("/api/payroll/runs".equals(path)) {
                    Optional<PayrollPeriod> containing = findPeriodContaining(input.periodEnd);
                    if (containing.isPresent() && !containing.get().acceptsRuns()) {
                        throw new IllegalStateException("Payroll period " + containing.get().id()
                            + " is closed; no further runs may be computed for it.");
                    }
                }
                PayrollRuleset ruleset = PayrollRuleset.forYear(periodYear(input));
                // A run that declares a payroll cycle is a real payroll run: it must reconcile with the
                // salary history, so a figure that exists in no maintained record is refused. A bare
                // preview that names no cycle stays a calculator using the figures it was given - that
                // is what the reference comparison needs, and it is the ad-hoc form still to be
                // replaced by a period picker.
                if (!periodId.isEmpty()) applyRateInForce(input, body);
                Payslip slip = new PayrollService().compute(input, ruleset);
                if ("/api/payroll/preview".equals(path)) {
                    sendJson(exchange, 200, "{\"payslip\":" + PayslipJson.toJson(slip).toString()
                        + ",\"saved\":false,\"ruleset\":" + rulesetToJson(ruleset) + "}");
                    return;
                }
                PayrollRun saved = payrollStoreFor(exchange).create(new PayrollRun(runId(input), input.payPeriod,
                    input.periodStart, input.periodEnd, ruleset.id(), input.taxBasis, input.statutoryBasis,
                    java.time.Instant.now().toString(), java.util.Collections.singletonList(slip)));
                sendJson(exchange, 201, "{\"run\":" + runToJson(saved, true) + ",\"saved\":true}");
                return;
            }
            if ("GET".equals(method) && ("/api/payroll/runs".equals(path) || "/api/payroll/runs/".equals(path))) {
                AccessControl.Actor actor = actorFor(exchange);
                // Filtering rows is not enough: a role with no payslip access at all must be refused
                // outright, so "you may see none" is not confused with "there are none".
                if (Policy.scope(Policy.Entity.PAYSLIP, actor.role()) == Policy.Scope.NONE
                    && Policy.scope(Policy.Entity.PAYROLL, actor.role()) == Policy.Scope.NONE) {
                    throw new AccessControl.Denied("You do not have payroll access.", 403);
                }
                AccessControl control = accessControlFor(exchange);
                StringBuilder items = new StringBuilder();
                int count = 0;
                for (PayrollRun run : payrollStoreFor(exchange).findAll()) {
                    boolean visible = Policy.isCompanyWideRead(Policy.Entity.PAYROLL, actor.role());
                    if (!visible) {
                        for (Payslip slip : run.payslips()) {
                            if (controlsRecord(actor, control, slip.employeeId())) { visible = true; break; }
                        }
                    }
                    if (!visible) continue;
                    if (count > 0) items.append(',');
                    items.append(runToJson(run, false));
                    count++;
                }
                sendJson(exchange, 200, "{\"items\":[" + items + "],\"count\":" + count + "}");
                return;
            }
            if ("GET".equals(method) && path.startsWith("/api/payroll/runs/")) {
                String id = path.substring("/api/payroll/runs/".length());
                requireRunVisible(exchange, id);
                if (id.isEmpty() || id.contains("/")) {
                    sendJson(exchange, 404, "{\"error\":\"Payroll run not found\"}");
                    return;
                }
                PayrollRun run = payrollStoreFor(exchange).findById(id)
                    .orElseThrow(() -> new java.util.NoSuchElementException("Payroll run not found."));
                sendJson(exchange, 200, "{\"run\":" + runToJson(run, true) + "}");
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
        } catch (AccessControl.Denied denied) {
            // Before IllegalArgumentException: Denied extends it, and a refusal reported as a 400
            // would read as a malformed form instead of "you may not do this".
            sendJson(exchange, denied.status(), "{\"error\":\"" + escapeJson(denied.getMessage()) + "\"}");
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /**
     * Records a new dated rate.
     *
     * <p>This is the "monthly record that can be submitted" in the design document: a rate change is a
     * row with a date it takes effect, not an edit to the employee record. Editing the record would
     * change every past payslip that is recomputed; adding a row here changes only periods that end on
     * or after its effective date.
     *
     * <p>A row takes effect on the first day of a month because payroll is paid by the cycle - a rate
     * dated mid-month would leave the same cycle paid at two rates, which the period model does not
     * allow. That is a deliberate restriction, stated in the error rather than discovered later.
     */
    private void submitSalaryRate(HttpExchange exchange) throws IOException {
        JsonObject body = readJsonBody(exchange);
        String employeeId = requiredString(body, "employeeId");
        requireAccess(exchange, Policy.Entity.PAYROLL, Policy.Operation.UPDATE, employeeId);
        java.time.LocalDate effectiveFrom;
        try {
            effectiveFrom = java.time.LocalDate.parse(requiredString(body, "effectiveFrom"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("effectiveFrom must be a date like 2026-07-01.");
        }
        if (effectiveFrom.getDayOfMonth() != 1) {
            throw new IllegalArgumentException("A rate takes effect on the first day of a month, "
                + "because pay is computed per cycle and a cycle cannot be paid at two rates. "
                + "Give the first day of the month the change applies from.");
        }
        SalaryHistory.SalaryRate rate = new SalaryHistory.SalaryRate(
            employeeId, effectiveFrom,
            new BigDecimal(requiredString(body, "monthlyBasic")),
            new BigDecimal(requiredString(body, "riceSubsidy")),
            new BigDecimal(requiredString(body, "phoneAllowance")),
            new BigDecimal(requiredString(body, "clothingAllowance")),
            body.has("reason") ? body.get("reason").getAsString() : "",
            java.time.Instant.now().toString());
        salaryHistory.add(rate);
        sendJson(exchange, 201, "{\"rate\":" + salaryRateToJson(rate) + "}");
    }

    private PayrollStore payrollStoreFor(HttpExchange exchange) {
        if (firebaseProjectId.isEmpty()) {
            if (localPayrollStore == null) localPayrollStore = new InMemoryPayrollStore();
            return localPayrollStore;
        }
        return new FirestorePayrollStore(firestoreClientFor(exchange));
    }

    private static int parseIntYear(String value) {
        if (value == null || value.trim().isEmpty()) return 2026;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Year must be a number.");
        }
    }

    private static int periodYear(PayrollService.Input input) {
        String date = input.periodStart == null ? "" : input.periodStart.trim();
        if (date.length() < 4) throw new IllegalArgumentException("Period start must be YYYY-MM-DD.");
        return Integer.parseInt(date.substring(0, 4));
    }

    private static String runId(PayrollService.Input input) {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "")
            .substring(0, 8).toUpperCase(java.util.Locale.ROOT);
        return "PR-" + input.periodStart.replace("-", "") + "-" + suffix;
    }

    private static PayrollService.Input payrollInputFrom(JsonObject body) {
        java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(
            "employeeId", "employeeName", "position", "department", "payPeriod", "periodStart", "periodEnd",
            "monthlyBasic", "daysWorked", "overtimeHours", "overtimeRatePercent", "nightDifferentialHours",
            "nightDifferentialRatePercent", "riceSubsidy", "phoneAllowance", "clothingAllowance",
            "taxableAllowance", "otherDeductions", "taxBasis", "statutoryBasis",
            // Which payroll cycle this run belongs to. Deliberately not part of PayrollService.Input:
            // the period is a filing fact, not an input to the arithmetic.
            "payrollPeriodId"));
        for (String key : body.keySet()) {
            if (!allowed.contains(key)) throw new IllegalArgumentException("Unexpected request field: " + key);
        }
        PayrollService.Input input = new PayrollService.Input();
        input.employeeId = requiredString(body, "employeeId");
        input.employeeName = optionalString(body, "employeeName");
        input.position = optionalString(body, "position");
        input.department = optionalString(body, "department");
        input.payPeriod = optionalString(body, "payPeriod").isEmpty() ? "MONTHLY" : optionalString(body, "payPeriod");
        input.periodStart = requiredString(body, "periodStart");
        input.periodEnd = requiredString(body, "periodEnd");
        input.monthlyBasic = numberOr(body, "monthlyBasic", null);
        input.daysWorked = numberOr(body, "daysWorked", null);
        input.overtimeHours = numberOr(body, "overtimeHours", java.math.BigDecimal.ZERO);
        input.overtimeRatePercent = numberOr(body, "overtimeRatePercent", new java.math.BigDecimal("125"));
        input.nightDifferentialHours = numberOr(body, "nightDifferentialHours", java.math.BigDecimal.ZERO);
        input.nightDifferentialRatePercent = numberOr(body, "nightDifferentialRatePercent", new java.math.BigDecimal("10"));
        input.riceSubsidy = numberOr(body, "riceSubsidy", Money.ZERO);
        input.phoneAllowance = numberOr(body, "phoneAllowance", Money.ZERO);
        input.clothingAllowance = numberOr(body, "clothingAllowance", Money.ZERO);
        input.taxableAllowance = numberOr(body, "taxableAllowance", Money.ZERO);
        input.otherDeductions = numberOr(body, "otherDeductions", Money.ZERO);
        if (!optionalString(body, "taxBasis").isEmpty()) input.taxBasis = optionalString(body, "taxBasis");
        if (!optionalString(body, "statutoryBasis").isEmpty()) input.statutoryBasis = optionalString(body, "statutoryBasis");
        return input;
    }

    /** Accepts a JSON number or a decimal string so the UI never has to guess a format. */
    private static java.math.BigDecimal numberOr(JsonObject body, String field, java.math.BigDecimal fallback) {
        JsonElement value = body.get(field);
        if (value == null || value.isJsonNull()) return fallback;
        if (!value.isJsonPrimitive()) throw new IllegalArgumentException(field + " must be a number.");
        String text = value.getAsString();
        if (text.trim().isEmpty()) return fallback;
        return Money.parse(text);
    }

    private static String rulesetToJson(PayrollRuleset ruleset) {
        JsonObject o = new JsonObject();
        o.addProperty("id", ruleset.id());
        o.addProperty("currency", "PHP");
        o.addProperty("effectiveFrom", ruleset.effectiveFrom());
        o.addProperty("effectiveTo", ruleset.effectiveTo());
        o.addProperty("daysPerMonth", PayrollService.DAYS_PER_MONTH.toPlainString());
        o.addProperty("hoursPerMonth", PayrollService.HOURS_PER_MONTH.toPlainString());
        o.addProperty("sssEmployeeRate", ruleset.sssEmployeeRate().toPlainString());
        o.addProperty("sssEmployerRate", ruleset.sssEmployerRate().toPlainString());
        o.addProperty("sssMinMsc", ruleset.sssMinMsc().toPlainString());
        o.addProperty("sssMaxMsc", ruleset.sssMaxMsc().toPlainString());
        o.addProperty("sssMscStep", ruleset.sssMscStep().toPlainString());
        o.addProperty("sssEcThreshold", ruleset.sssEcThreshold().toPlainString());
        o.addProperty("sssEcLow", ruleset.sssEcLow().toPlainString());
        o.addProperty("sssEcHigh", ruleset.sssEcHigh().toPlainString());
        o.addProperty("philHealthRate", ruleset.phRate().toPlainString());
        o.addProperty("philHealthFloor", ruleset.phFloor().toPlainString());
        o.addProperty("philHealthCeiling", ruleset.phCeiling().toPlainString());
        o.addProperty("pagibigMaxFund", ruleset.pagibigMaxFund().toPlainString());
        o.addProperty("pagibigEmployeeLow", ruleset.pagibigEmployeeLow().toPlainString());
        o.addProperty("pagibigEmployeeHigh", ruleset.pagibigEmployeeHigh().toPlainString());
        o.addProperty("pagibigEmployerRate", ruleset.pagibigEmployerRate().toPlainString());
        JsonArray monthly = new JsonArray();
        for (PayrollRuleset.TaxBracket b : ruleset.taxBrackets(PayrollRuleset.Period.MONTHLY)) {
            JsonObject row = new JsonObject();
            row.addProperty("over", b.floor().toPlainString());
            row.addProperty("base", b.base().toPlainString());
            row.addProperty("rate", b.rate().toPlainString());
            monthly.add(row);
        }
        o.add("monthlyTaxBrackets", monthly);
        JsonArray semi = new JsonArray();
        for (PayrollRuleset.TaxBracket b : ruleset.taxBrackets(PayrollRuleset.Period.SEMI_MONTHLY)) {
            JsonObject row = new JsonObject();
            row.addProperty("over", b.floor().toPlainString());
            row.addProperty("base", b.base().toPlainString());
            row.addProperty("rate", b.rate().toPlainString());
            semi.add(row);
        }
        o.add("semiMonthlyTaxBrackets", semi);
        // The golden case is reproduced by the engine test with the payslip's own benefit figures.
        // The master employee file differs by P1,000 on this person's phone allowance, so the
        // claim is worded for the payslip and the conflict is surfaced rather than hidden.
        o.addProperty("goldenCase", "Payslip 15-2026-01-15: engine reproduces 22,711.67 "
            + "(phone allowance 2,000 on the payslip, 1,000 in the employee file - see data notes)");
        // Supplied files disagree for employee 10015; the UI shows this instead of reconciling it.
        JsonObject conflicts = new JsonObject();
        conflicts.addProperty("10015", "Phone allowance is 2,000.00 on MotorPH's sample payslip "
            + "15-2026-01-15 but 1,000.00 in the employee master file. Salary history pays 2,000.00 "
            + "(the paid figure); the employee record keeps 1,000.00. Unresolved - confirm with MotorPH.");
        o.add("dataConflicts", conflicts);
        return o.toString();
    }

    private static String timesheetToJson(TimesheetService.Summary summary) {
        JsonObject o = new JsonObject();
        o.addProperty("employeeId", summary.employeeId());
        o.addProperty("from", summary.from());
        o.addProperty("to", summary.to());
        o.addProperty("daysWorked", summary.daysWorked());
        o.addProperty("totalHours", summary.totalHours().toPlainString());
        JsonArray days = new JsonArray();
        for (TimesheetService.Day day : summary.days()) {
            JsonObject row = new JsonObject();
            row.addProperty("date", day.date);
            row.addProperty("dayType", day.dayType);
            row.addProperty("hours", day.hours);
            days.add(row);
        }
        o.add("days", days);
        o.addProperty("note", "Counts stored rows with a positive worked total; it does not classify absence.");
        return o.toString();
    }

    /** How the reference calculator is reached. Overridable for tests via MOTORPH_SWP_ENDPOINT. */
    private static SweldongPinoyClient sweldongPinoyClient(PayrollService.Input input) {
        String override = System.getenv().getOrDefault("MOTORPH_SWP_ENDPOINT", "");
        return override.trim().isEmpty() ? new SweldongPinoyClient() : new SweldongPinoyClient(override);
    }

    /**
     * True only when our payslip and the reference calculator are pricing the same thing: one
     * whole month, with every working day paid. Anything else is a part-period on our side and a
     * full period on theirs, and a difference then measures the basis, not the arithmetic.
     */
    private static boolean comparableToReference(PayrollService.Input input) {
        if ("SEMI_MONTHLY".equalsIgnoreCase(input.payPeriod)) return false;
        return periodDays(input).compareTo(PayrollService.DAYS_PER_MONTH) == 0;
    }

    /** Says, in one sentence, why the two figures are not like for like. */
    private static String comparisonBasisDifference(PayrollService.Input input) {
        if ("SEMI_MONTHLY".equalsIgnoreCase(input.payPeriod)) {
            return "sweldongpinoy.com only prices a whole month. Set the pay period to Monthly "
                + "and days worked to the full month to compare like for like.";
        }
        String full = PayrollService.DAYS_PER_MONTH.stripTrailingZeros().toPlainString();
        return "Our figure pays " + periodDays(input).stripTrailingZeros().toPlainString()
            + " day(s) of a " + full
            + "-day month; the reference always pays a full month. Set days worked to "
            + full + " to compare like for like.";
    }

    /**
     * Binds a run to the rate that was in force at the end of its period.
     *
     * <p>Two things this deliberately does <em>not</em> do:
     *
     * <ul>
     *   <li>It does not overwrite the caller's figures. If the request names an amount different from
     *       the dated rate, that is a refusal, because otherwise a payslip could be computed from
     *       numbers that exist nowhere in the employee's history.</li>
     *   <li>It does not invent a rate. An employee with no dated row is paid from the directory
     *       record, exactly as submitted.</li>
     * </ul>
     *
     * <p>The effect is that salary edits cannot silently rewrite an earlier payslip: the rate in force
     * at that period end is what pays, or the run is refused.
     */
    private void applyRateInForce(PayrollService.Input input, JsonObject submitted) {
        if (input.employeeId == null || input.periodEnd == null) return;
        LocalDate asOf;
        try {
            asOf = LocalDate.parse(input.periodEnd);
        } catch (DateTimeParseException ex) {
            return;
        }
        Optional<SalaryHistory.SalaryRate> found = salaryHistory.rateOn(input.employeeId, asOf);
        if (!found.isPresent()) return;
        SalaryHistory.SalaryRate inForce = found.get();

        // Only components the caller actually sent are compared. A missing key means "use the rate",
        // not "pay zero", so an absent allowance is filled in below rather than treated as a conflict.
        List<String> mismatches = new ArrayList<>();
        checkRateComponent(mismatches, "monthly basic", submitted, "monthlyBasic",
            input.monthlyBasic, inForce.monthlyBasic(), asOf);
        checkRateComponent(mismatches, "rice subsidy", submitted, "riceSubsidy",
            input.riceSubsidy, inForce.riceSubsidy(), asOf);
        checkRateComponent(mismatches, "phone allowance", submitted, "phoneAllowance",
            input.phoneAllowance, inForce.phoneAllowance(), asOf);
        checkRateComponent(mismatches, "clothing allowance", submitted, "clothingAllowance",
            input.clothingAllowance, inForce.clothingAllowance(), asOf);
        if (!mismatches.isEmpty()) {
            throw new IllegalArgumentException("This run disagrees with the salary rate in force on "
                + asOf + " for employee " + input.employeeId + ": " + String.join("; ", mismatches)
                + ". Use the dated rate, or record a new rate in the salary history first.");
        }
        if (!submitted.has("monthlyBasic")) input.monthlyBasic = inForce.monthlyBasic();
        if (!submitted.has("riceSubsidy")) input.riceSubsidy = inForce.riceSubsidy();
        if (!submitted.has("phoneAllowance")) input.phoneAllowance = inForce.phoneAllowance();
        if (!submitted.has("clothingAllowance")) input.clothingAllowance = inForce.clothingAllowance();
    }

    private static void checkRateComponent(List<String> mismatches, String label, JsonObject body,
                                           String key, BigDecimal submitted, BigDecimal inForce, LocalDate asOf) {
        if (submitted == null || !body.has(key)) return;
        if (Money.round(submitted).compareTo(Money.round(inForce)) != 0) {
            mismatches.add(label + " submitted " + Money.round(submitted).toPlainString()
                + " but " + Money.round(inForce).toPlainString() + " was in force");
        }
    }

    /** The reference calculator always quotes a whole month, so it is sent the monthly basic. */
    private static String comparisonUnavailableToJson(Payslip slip, String reason) {
        JsonObject o = new JsonObject();
        o.addProperty("reference", "sweldongpinoy.com");
        o.addProperty("referenceNote", "Undocumented third-party calculator, used for comparison only. "
            + "Never used to compute or save a payslip.");
        o.add("ours", payslipFigures(slip));
        o.addProperty("available", false);
        o.addProperty("reason", reason);
        o.addProperty("matches", false);
        o.add("differences", new JsonArray());
        return o.toString();
    }

    private static java.math.BigDecimal referenceBasic(PayrollService.Input input) {
        return Money.round(input.monthlyBasic);
    }

    private static java.math.BigDecimal periodDays(PayrollService.Input input) {
        return input.daysWorked == null ? java.math.BigDecimal.valueOf(20) : input.daysWorked;
    }

    /**
     * Side-by-side figures. "ours" is what the payslip already says; "theirs" is the reference
     * calculator's own answer, restated in our field names so a client never has to know both.
     */
    /** Our side of a comparison, in the same field names the reference figures use. */
    private static JsonObject payslipFigures(Payslip slip) {
        JsonObject ours = new JsonObject();
        ours.addProperty("grossPay", Money.round(slip.grossIncome().add(slip.totalBenefits())).toPlainString());
        ours.addProperty("sss", slip.sssEmployee().toPlainString());
        ours.addProperty("philHealth", slip.philHealthEmployee().toPlainString());
        ours.addProperty("pagibig", slip.pagibigEmployee().toPlainString());
        ours.addProperty("withholdingTax", slip.withholdingTax().toPlainString());
        ours.addProperty("totalDeductions", slip.totalDeductions().toPlainString());
        ours.addProperty("netPay", slip.takeHomePay().toPlainString());
        return ours;
    }

    private static String comparisonToJson(Payslip slip, SweldongPinoyClient.Comparison comparison) {
        JsonObject o = new JsonObject();
        o.addProperty("reference", "sweldongpinoy.com");
        o.addProperty("referenceNote",
            "Undocumented third-party calculator, used for comparison only. Never used to compute or save a payslip.");
        o.add("ours", payslipFigures(slip));

        o.addProperty("available", comparison.available);
        if (!comparison.available) {
            o.addProperty("reason", comparison.reason);
            o.add("theirs", com.google.gson.JsonNull.INSTANCE);
            o.add("differences", new JsonArray());
            return o.toString();
        }
        JsonObject theirs = new JsonObject();
        theirs.addProperty("grossPay", amount(comparison.theirs, "gross_pay"));
        theirs.addProperty("sss", amount(comparison.theirs, "sss_gsis_contribution"));
        theirs.addProperty("philHealth", amount(comparison.theirs, "philhealth_contribution"));
        theirs.addProperty("pagibig", amount(comparison.theirs, "pagibig_contribution"));
        theirs.addProperty("withholdingTax", amount(comparison.theirs, "withholding_tax"));
        theirs.addProperty("totalDeductions", amount(comparison.theirs, "total_deductions"));
        theirs.addProperty("netPay", amount(comparison.theirs, "net_pay"));
        o.add("theirs", theirs);

        JsonObject mineFigures = o.getAsJsonObject("ours");
        JsonArray differences = new JsonArray();
        for (String field : new String[]{"sss", "philHealth", "pagibig", "withholdingTax", "totalDeductions", "netPay"}) {
            String mine = mineFigures.get(field).getAsString();
            String other = theirs.get(field).getAsString();
            if (money(mine).compareTo(money(other)) != 0) {
                JsonObject diff = new JsonObject();
                diff.addProperty("field", field);
                diff.addProperty("ours", mine);
                diff.addProperty("theirs", other);
                diff.addProperty("delta", Money.round(money(other).subtract(money(mine))).toPlainString());
                differences.add(diff);
            }
        }
        o.add("differences", differences);
        o.addProperty("matches", differences.size() == 0);
        return o.toString();
    }

    private static String amount(JsonObject source, String field) {
        if (source == null || !source.has(field) || source.get(field).isJsonNull()) return "0.00";
        String raw = source.get(field).getAsString().replace(",", "").trim();
        try {
            return Money.round(new java.math.BigDecimal(raw)).toPlainString();
        } catch (NumberFormatException ex) {
            return "0.00";
        }
    }

    private static java.math.BigDecimal money(String value) {
        try {
            return Money.round(new java.math.BigDecimal(value.replace(",", "").trim()));
        } catch (RuntimeException ex) {
            return Money.ZERO;
        }
    }

    private static String runToJson(PayrollRun run, boolean includePayslips) {
        JsonObject o = new JsonObject();
        o.addProperty("id", run.id());
        o.addProperty("payPeriod", run.payPeriod());
        o.addProperty("periodStart", run.periodStart());
        o.addProperty("periodEnd", run.periodEnd());
        o.addProperty("rulesetId", run.rulesetId());
        o.addProperty("taxBasis", run.taxBasis());
        o.addProperty("statutoryBasis", run.statutoryBasis());
        o.addProperty("createdAt", run.createdAt());
        java.math.BigDecimal gross = Money.ZERO, benefits = Money.ZERO;
        java.math.BigDecimal deductions = Money.ZERO, net = Money.ZERO, cost = Money.ZERO;
        for (Payslip slip : run.payslips()) {
            gross = gross.add(slip.grossIncome());
            benefits = benefits.add(slip.totalBenefits());
            deductions = deductions.add(slip.totalDeductions());
            net = net.add(slip.takeHomePay());
            cost = cost.add(slip.employerCost());
        }
        o.addProperty("employeeCount", run.payslips().size());
        // Three distinct sums: payroll "gross" excludes benefits, so earnings = gross + benefits.
        o.addProperty("totalGross", Money.round(gross).toPlainString());
        o.addProperty("totalBenefits", Money.round(benefits).toPlainString());
        o.addProperty("totalEarnings", Money.round(gross.add(benefits)).toPlainString());
        o.addProperty("totalDeductions", Money.round(deductions).toPlainString());
        o.addProperty("totalNet", Money.round(net).toPlainString());
        o.addProperty("totalEmployerCost", Money.round(cost).toPlainString());
        if (includePayslips) {
            JsonArray slips = new JsonArray();
            for (Payslip slip : run.payslips()) slips.add(PayslipJson.toJson(slip));
            o.add("payslips", slips);
        }
        return o.toString();
    }

    private void handleSeedDemo(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        if (firebaseProjectId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"Firebase is not configured for this server.\"}");
            return;
        }
        try {
            FirestoreRestClient client = firestoreClientFor(exchange);
            DemoSeedService.SeedResult result = new DemoSeedService(client).seedMissingSyntheticRecords();
            sendJson(exchange, 200, "{\"employeesCreated\":" + result.getEmployeesCreated()
                + ",\"scheduleMappingsCreated\":" + result.getScheduleMappingsCreated()
                + ",\"attendanceCreated\":" + result.getAttendanceCreated()
                + ",\"syntheticOnly\":true}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /**
     * The caller: account, then person, then seat.
     *
     * <p>In Firebase mode the account comes from a <b>verified</b> ID token, never from a header the
     * client chose. Two client-supplied values then select <em>within</em> that account:
     *
     * <ul>
     *   <li>{@code X-MotorPH-Employee} — which linked person to act as. An account may be linked to
     *       more than one, which is how one login covers someone who does two jobs.</li>
     *   <li>{@code X-MotorPH-Seat} — which seat of that person to act under. One person may hold more
     *       than one, which is how 10006 switches between HR and Supervisor without a second login.</li>
     * </ul>
     *
     * <p>Both are refusals waiting to happen: the server checks that the account really is linked to
     * that person and that the person really holds that seat, and answers 403 with the alternatives
     * when they are not. Those are the two things that had been assumed rather than checked.
     */
    AccessControl.Actor actorFor(HttpExchange exchange) {
        String requestedPerson = header(exchange, "X-MotorPH-Employee");
        if (requestedPerson == null) requestedPerson = header(exchange, "X-Firebase-EID");
        String requestedSeat = header(exchange, "X-MotorPH-Seat");

        if (fixtureIdentityMode) {
            // Isolated fixture runtime: no accounts exist, so the operator names the person directly.
            // The seat is still validated against that person's seats - the matrix is real here too.
            if (requestedPerson == null || requestedPerson.trim().isEmpty()) {
                return new AccessControl.Actor("", Role.APPLICANT);
            }
            String person = requestedPerson.trim();
            if (!person.matches("[0-9]{5}")) {
                throw new AccessControl.Denied("That is not a MotorPH employee number.", 403);
            }
            RoleAssignment.Seat seat = roleAssignment.resolve(person, requestedSeat);
            return new AccessControl.Actor(person, seat.role());
        }

        FirebaseTokenVerifier.Account account = authenticatedAccount(exchange);
        return seatRegistry.actorFor(account.uid(), requestedPerson, requestedSeat, roleAssignment);
    }

    /**
     * The verified account behind this request.
     *
     * <p>This is the line that used to be ``Authorization header != null''. An unverified token is not
     * authentication: it let any caller name any employee and be given that person's role.
     */
    FirebaseTokenVerifier.Account authenticatedAccount(HttpExchange exchange) {
        if (tokenVerifier == null) {
            throw new AccessControl.Denied("This server cannot verify sessions right now.", 503);
        }
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessControl.Denied("Sign in with your MotorPH account to continue.", 401);
        }
        FirebaseTokenVerifier.Account account =
            tokenVerifier.verify(authorization.substring("Bearer ".length()));
        String claimedUid = header(exchange, "X-Firebase-UID");
        if (claimedUid != null && !claimedUid.equals(account.uid())) {
            // The UID header is retained for the Firestore path, where it decides which user's
            // documents are touched. If it disagrees with the verified token, that is an attempt to
            // read somebody else's sandbox and is refused rather than quietly corrected.
            throw new AccessControl.Denied("This session does not match the account it claims to be.", 403);
        }
        return account;
    }

    private static String header(HttpExchange exchange, String name) {
        String value = exchange.getRequestHeaders().getFirst(name);
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /** The access-control engine, bound to whichever employee directory this request uses. */
    AccessControl accessControlFor(HttpExchange exchange) {
        EmployeeService service = servicesFor(exchange).employeeService;
        return new AccessControl(service::findEmployee);
    }

    /** Fails the request unless the matrix allows this operation on this employee. */
    void requireAccess(HttpExchange exchange, Policy.Entity entity, Policy.Operation operation,
                       String targetEmployeeNumber) {
        accessControlFor(exchange).require(actorFor(exchange), entity, operation, targetEmployeeNumber);
    }

    /**
     * Seeds one dated rate row per employee from the employee file, effective from {@code since}.
     * This is the first entry in each person's salary history, not a synthetic invention: the
     * amounts are exactly the ones in MotorPH's CSV.
     */
    void seedSalaryHistoryFromDirectory(EmployeeService employees, LocalDate since) {
        for (Employee employee : employees.listEmployees()) {
            if (!employee.hasPayProfile()) continue;
            salaryHistory.add(new SalaryHistory.SalaryRate(employee.getId(), since,
                employee.getMonthlyBasic(), employee.getRiceSubsidy(), employee.getPhoneAllowance(),
                employee.getClothingAllowance(), "Opening rate from MotorPH employee file",
                java.time.Instant.now().toString()));
        }
    }

    /**
     * Why the salary history does not carry the sample payslip's 2,000.00 phone allowance for 10015.
     *
     * <p>MotorPH's two files disagree: payslip 15-2026-01-15 pays 2,000.00, the employee master file
     * says 1,000.00. The salary history has to pick one authority or every run becomes ambiguous, so it
     * follows the employee master file - the record that HR maintains and that the onboarding and
     * rate-change workflows write to. The 2,000.00 is not discarded: it stays on the payroll screen,
     * in {@code /api/payroll/rules} under {@code dataConflicts}, and in the payslip test that reproduces
     * MotorPH's document from its own figures.
     *
     * <p>Consequently a run that submits 2,000.00 for 10015 is <em>refused</em> rather than quietly
     * computed: that amount exists in no maintained record. The conflict is visible, and the resolution
     * is a decision for MotorPH to make, not one to be buried in a lookup table.
     */
    static final String CONFLICT_10015_PHONE_ALLOWANCE = "10015";

    SalaryHistory salaryHistory() {
        return salaryHistory;
    }

    /** Test seam: install the account -> person -> seat links for a Firebase-mode server. */
    void setSeatRegistry(SeatRegistry registry) {
        this.seatRegistry = registry == null ? new SeatRegistry(java.util.Collections.emptyMap()) : registry;
    }

    /** Test seam: whether this server is the isolated fixture runtime. */
    boolean fixtureIdentityMode() {
        return fixtureIdentityMode;
    }

    void setRoleAssignment(RoleAssignment assignment) {
        if (assignment == null) throw new IllegalArgumentException("role assignment must not be null");
        this.roleAssignment = assignment;
    }

    RoleAssignment roleAssignment() {
        return roleAssignment;
    }

    private RequestServices servicesFor(HttpExchange exchange) {
        if (firebaseProjectId.isEmpty()) {
            return new RequestServices(localEmployeeService, localScheduleStore, localAttendanceStore);
        }
        FirestoreRestClient client = firestoreClientFor(exchange);
        return new RequestServices(new EmployeeService(new FirestoreEmployeeStore(client)),
            new FirestoreEmployeeScheduleStore(client), new FirestoreAttendanceStore(client));
    }

    private FirestoreRestClient firestoreClientFor(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        String uid = exchange.getRequestHeaders().getFirst("X-Firebase-UID");
        if (authorization == null || !authorization.startsWith("Bearer ") || uid == null || uid.trim().isEmpty()) {
            throw new FirestoreException("Sign in with your Firebase account to continue.", 401);
        }
        String token = authorization.substring("Bearer ".length()).trim();
        if (token.length() > 8192 || uid.length() > 128 || uid.contains("/")) {
            throw new FirestoreException("Invalid Firebase session information.", 401);
        }
        return firestoreApiRoot == null
            ? new FirestoreRestClient(firebaseProjectId, uid, token)
            : new FirestoreRestClient(firebaseProjectId, uid, token, firestoreApiRoot);
    }

    private static LocalDate parseOptionalDate(String value) {
        return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value.trim());
    }

    private static String attendanceToJson(List<AttendanceRecord> records) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < records.size(); i++) {
            AttendanceRecord record = records.get(i);
            if (i > 0) json.append(',');
            json.append(attendanceRecordToJson(record));
        }
        return json.append("],\"count\":").append(records.size())
            .append(",\"hoursCalculated\":false}").toString();
    }

    private static String attendanceRecordToJson(AttendanceRecord record) {
        return "{\"attendanceId\":" + record.getAttendanceId()
            + ",\"scheduleId\":" + record.getScheduleId()
            + ",\"date\":\"" + record.getDate()
            + "\",\"timeIn\":\"" + record.getTimeIn()
            + "\",\"breakOut\":\"" + (record.getBreakOut() == null ? "" : record.getBreakOut())
            + "\",\"breakIn\":\"" + (record.getBreakIn() == null ? "" : record.getBreakIn())
            + "\",\"timeOut\":\"" + record.getTimeOut()
            + "\",\"hoursWorked\":\"" + record.getHoursWorked().toPlainString()
            + "\",\"dayType\":\"" + escapeJson(record.getDayType()) + "\"}";
    }

    private static String attendanceSummaryToJson(List<RawAttendanceSummary> summaries, LocalDate from, LocalDate to) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < summaries.size(); i++) {
            RawAttendanceSummary summary = summaries.get(i);
            if (i > 0) json.append(',');
            json.append("{\"employeeId\":\"").append(escapeJson(summary.getEmployeeId()))
                .append("\",\"employeeName\":\"").append(escapeJson(summary.getEmployeeName()))
                .append("\",\"scheduleId\":").append(summary.getScheduleId() == null ? "null" : summary.getScheduleId())
                .append(",\"hasScheduleMapping\":").append(summary.hasScheduleMapping())
                .append(",\"rawRecordCount\":").append(summary.getRawRecordCount())
                .append(",\"firstRecordDate\":\"").append(escapeJson(summary.getFirstRecordDate()))
                .append("\",\"lastRecordDate\":\"").append(escapeJson(summary.getLastRecordDate())).append("\"}");
        }
        json.append("],\"count\":").append(summaries.size())
            .append(",\"from\":").append(from == null ? "null" : "\"" + from + "\"")
            .append(",\"to\":").append(to == null ? "null" : "\"" + to + "\"")
            .append(",\"hoursCalculated\":false}");
        return json.toString();
    }

    private void handleStaticFiles(HttpExchange exchange) throws IOException {
        boolean headOnly = "HEAD".equals(exchange.getRequestMethod());
        if (!"GET".equals(exchange.getRequestMethod()) && !headOnly) {
            sendText(exchange, 405, "text/plain; charset=utf-8", "Method not allowed");
            return;
        }
        String requestPath = exchange.getRequestURI().getPath();
        if (requestPath == null || "/".equals(requestPath)) requestPath = "/index.html";
        Path requested = webRoot.resolve(requestPath.substring(1)).normalize();
        if (!requested.startsWith(webRoot) || !Files.isRegularFile(requested)) {
            sendText(exchange, 404, "text/plain; charset=utf-8", "Not found");
            return;
        }
        String contentType = contentTypeFor(requested);
        byte[] bytes = Files.readAllBytes(requested);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("Content-Security-Policy",
            "default-src 'self'; style-src 'self'; script-src 'self'; img-src 'self' data:; "
                + "connect-src 'self' https://identitytoolkit.googleapis.com https://securetoken.googleapis.com; "
                + "object-src 'none'; base-uri 'self'");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (headOnly) {
            exchange.getResponseHeaders().set("Content-Length", Integer.toString(bytes.length));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static String employeesToJson(List<Employee> employees, int total) {
        StringBuilder json = new StringBuilder("{\"items\":[");
        for (int i = 0; i < employees.size(); i++) {
            if (i > 0) json.append(',');
            json.append(employeeToJson(employees.get(i)));
        }
        return json.append("],\"total\":").append(total)
            .append(",\"matched\":").append(employees.size()).append('}').toString();
    }

    private static String employeeToJson(Employee employee) {
        StringBuilder json = new StringBuilder("{")
            .append("\"id\":\"").append(escapeJson(employee.getId())).append("\",")
            .append("\"name\":\"").append(escapeJson(employee.getName())).append("\",")
            .append("\"department\":\"").append(escapeJson(employee.getDepartment())).append("\",")
            .append("\"role\":\"").append(escapeJson(employee.getRole())).append("\",")
            .append("\"location\":\"").append(escapeJson(employee.getLocation())).append("\",")
            .append("\"status\":\"").append(escapeJson(employee.getStatus())).append("\"");
        if (employee.getMonthlyBasic() != null) {
            json.append(",\"monthlyBasic\":\"").append(employee.getMonthlyBasic().toPlainString()).append("\"")
                .append(",\"riceSubsidy\":\"").append(nullable(employee.getRiceSubsidy())).append("\"")
                .append(",\"phoneAllowance\":\"").append(nullable(employee.getPhoneAllowance())).append("\"")
                .append(",\"clothingAllowance\":\"").append(nullable(employee.getClothingAllowance())).append("\"");
        }
        return json.append("}").toString();
    }

    private static String nullable(java.math.BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private static String queryParameter(String rawQuery, String target) {
        if (rawQuery == null || rawQuery.isEmpty()) return "";
        for (String part : rawQuery.split("&")) {
            int separator = part.indexOf('=');
            String key = separator < 0 ? part : part.substring(0, separator);
            if (target.equals(decode(key))) return separator < 0 ? "" : decode(part.substring(separator + 1));
        }
        return "";
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (IllegalArgumentException | java.io.UnsupportedEncodingException ex) {
            return "";
        }
    }

    private static String contentTypeFor(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".html")) return "text/html; charset=utf-8";
        if (name.endsWith(".css")) return "text/css; charset=utf-8";
        if (name.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (name.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static void sendApiError(HttpExchange exchange, FirestoreException exception) throws IOException {
        int status = exception.getStatusCode();
        if (status < 400 || status > 599) status = 500;
        sendJson(exchange, status, "{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}");
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        sendText(exchange, status, "application/json; charset=utf-8", body);
    }

    private static void sendText(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static final class RequestException extends RuntimeException {
        private final int statusCode;
        RequestException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

    private static final class RequestServices {
        private final EmployeeService employeeService;
        private final EmployeeScheduleStore scheduleStore;
        private final AttendanceStore attendanceStore;

        RequestServices(EmployeeService employeeService, EmployeeScheduleStore scheduleStore, AttendanceStore attendanceStore) {
            this.employeeService = employeeService;
            this.scheduleStore = scheduleStore;
            this.attendanceStore = attendanceStore;
        }
    }
}
