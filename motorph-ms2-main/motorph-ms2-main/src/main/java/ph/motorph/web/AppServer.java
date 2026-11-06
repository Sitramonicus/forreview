package ph.motorph.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executors;

/** Java 11 HTTP backend for the browser-based MotorPH prototype. */
public final class AppServer {
    private final Path webRoot;
    private final String firebaseProjectId;
    private final String firestoreApiRoot;
    // Local demo state. Not final: the data file and "Reset demo data" replace these objects (see importState).
    private InMemoryEmployeeStore localEmployeeStore = new InMemoryEmployeeStore();
    private EmployeeService localEmployeeService = new EmployeeService(localEmployeeStore);
    private InMemoryEmployeeScheduleStore localScheduleStore = new InMemoryEmployeeScheduleStore();
    private InMemoryAttendanceStore localAttendanceStore = new InMemoryAttendanceStore();
    private InMemoryWorkflowStore localWorkflowStore = new InMemoryWorkflowStore();
    private SavedPayslipStore savedPayslips = new SavedPayslipStore();
    private PayrollAdjustments payrollAdjustments = new PayrollAdjustments();
    private DisbursementStore disbursements = new DisbursementStore();
    private WorkSchedule.Store workSchedules = new WorkSchedule.Store();
    private SalaryHistory salaryHistory = new SalaryHistory();
    private PayChangeRequests payChanges = new PayChangeRequests();
    private HrRecords hrRecords = new HrRecords();
    private GovernmentDetails governmentDetails = new GovernmentDetails();
    private StatutoryRates.Registry statutoryRates = new StatutoryRates.Registry();
    private List<JsonObject> roleChanges = new java.util.ArrayList<>();
    private List<JsonObject> timesheetCorrections = new java.util.ArrayList<>();
    private PayslipCorrections payslipCorrections = new PayslipCorrections();
    private PayslipOutbox payslipOutbox = new PayslipOutbox();
    private AccountSecurity accountSecurity = new AccountSecurity();
    private AuditLog auditLog = new AuditLog();
    private Notifications notifications = new Notifications();
    private PersonalDetails personalDetails = new PersonalDetails();
    private EmployeePhotos employeePhotos = new EmployeePhotos();
    private RateProposals rateProposals = new RateProposals();
    private AdjustmentForms adjustmentForms = new AdjustmentForms();
    private EmployeeChangelog employeeChangelog = new EmployeeChangelog();
    private final ClockService clockService = new ClockService(java.time.Clock.system(java.time.ZoneId.of("Asia/Manila")));
    private final LocalStateFile dataFile;
    private final Object stateLock = new Object();
    private final TwoFactor twoFactor = new TwoFactor(clockService::now);
    private final JsonObject seedState;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;

    public AppServer() {
        this(System.getenv().getOrDefault("FIREBASE_PROJECT_ID", ""),
            Paths.get(System.getenv().getOrDefault("WEB_ROOT", "web")), null, dataFileFromEnvironment());
    }

    /** LOCAL_DATA_FILE (default data/local-demo.json); set it to an empty value to keep the demo in memory only. */
    private static Path dataFileFromEnvironment() {
        String value = System.getenv().getOrDefault("LOCAL_DATA_FILE", "data/local-demo.json").trim();
        return value.isEmpty() ? null : Paths.get(value);
    }

    AppServer(String firebaseProjectId, Path webRoot) {
        this(firebaseProjectId, webRoot, null);
    }

    AppServer(String firebaseProjectId, Path webRoot, String firestoreApiRoot) {
        this(firebaseProjectId, webRoot, firestoreApiRoot, null);
    }

    /** dataFile null = in memory only (tests); the local demo saves every change to it and reloads it at start-up. */
    AppServer(String firebaseProjectId, Path webRoot, String firestoreApiRoot, Path dataFile) {
        this.firebaseProjectId = firebaseProjectId == null ? "" : firebaseProjectId.trim();
        if (webRoot == null) throw new IllegalArgumentException("webRoot must not be null");
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.firestoreApiRoot = firestoreApiRoot == null || firestoreApiRoot.trim().isEmpty()
            ? null : firestoreApiRoot.trim();
        this.dataFile = dataFile != null && this.firebaseProjectId.isEmpty() ? new LocalStateFile(dataFile) : null;
        AccessPolicy.importRoles(null, null);
        PayrollPolicy.importValues(null);
        this.seedState = exportState();
        if (this.dataFile != null) loadDataFile();
    }

    private void loadDataFile() {
        try {
            java.util.Optional<JsonObject> saved = dataFile.load();
            if (saved.isPresent()) {
                importState(saved.get());
                importAudit(saved.get());
                System.out.println("Loaded local demo data from " + dataFile.path());
            } else {
                System.out.println("No local demo data file yet; it will be created at " + dataFile.path());
            }
        } catch (IOException | RuntimeException ex) {
            Path aside;
            try {
                aside = dataFile.setAside();
            } catch (IOException moveFailure) {
                throw new IllegalStateException("Local demo data file is unreadable and could not be moved aside: " + dataFile.path(), moveFailure);
            }
            // Recovery: the newest backup that still loads; only if none does, the built-in demo data.
            String recovered = recoverFromNewestBackup();
            if (recovered == null) importState(seedState);
            System.out.println("Local demo data file could not be read (" + ex.getMessage() + "); moved it to " + aside + " and "
                + (recovered == null ? "started from the built-in demo data." : "recovered from backup " + recovered + "."));
        }
    }

    private String recoverFromNewestBackup() {
        try {
            for (String name : dataFile.listBackups()) {
                try {
                    JsonObject stores = dataFile.loadBackup(name);
                    importState(stores);
                    importAudit(stores);
                    return name;
                } catch (IOException | RuntimeException unreadable) {
                    // try the next older backup
                }
            }
        } catch (IOException ex) {
            // no usable backup folder
        }
        return null;
    }

    /** Every local store as JSON (the data file's "stores"). */
    JsonObject exportState() {
        com.google.gson.Gson gson = (dataFile != null ? dataFile : new LocalStateFile(Paths.get("unused.json"))).gson();
        JsonObject stores = new JsonObject();
        stores.add("employees", gson.toJsonTree(localEmployeeStore));
        stores.add("schedules", gson.toJsonTree(localScheduleStore));
        stores.add("attendance", gson.toJsonTree(localAttendanceStore));
        stores.add("workflows", gson.toJsonTree(localWorkflowStore));
        stores.add("savedPayslips", gson.toJsonTree(savedPayslips));
        stores.add("payrollAdjustments", gson.toJsonTree(payrollAdjustments));
        stores.add("disbursements", gson.toJsonTree(disbursements));
        stores.add("workSchedules", gson.toJsonTree(workSchedules));
        stores.add("salaryHistory", gson.toJsonTree(salaryHistory));
        stores.add("payChanges", gson.toJsonTree(payChanges));
        stores.add("hrRecords", gson.toJsonTree(hrRecords));
        stores.add("governmentDetails", gson.toJsonTree(governmentDetails));
        stores.add("statutoryRates", gson.toJsonTree(statutoryRates));
        stores.add("roleChanges", gson.toJsonTree(roleChanges));
        stores.add("timesheetCorrections", gson.toJsonTree(timesheetCorrections));
        stores.add("roles", gson.toJsonTree(AccessPolicy.exportRoles()));
        stores.add("supervisors", gson.toJsonTree(AccessPolicy.exportSupervisors()));
        stores.add("openShifts", gson.toJsonTree(clockService.exportOpenShifts()));
        stores.add("onsiteCheckIns", gson.toJsonTree(clockService.exportOnsiteCheckIns()));
        stores.add("payslipCorrections", gson.toJsonTree(payslipCorrections));
        stores.add("payslipOutbox", gson.toJsonTree(payslipOutbox));
        stores.add("accountSecurity", gson.toJsonTree(accountSecurity));
        stores.add("notifications", gson.toJsonTree(notifications));
        stores.add("personalDetails", gson.toJsonTree(personalDetails));
        stores.add("employeePhotos", gson.toJsonTree(employeePhotos));
        stores.add("rateProposals", gson.toJsonTree(rateProposals));
        stores.add("adjustmentForms", gson.toJsonTree(adjustmentForms));
        stores.add("employeeChangelog", gson.toJsonTree(employeeChangelog));
        stores.add("payrollPolicy", gson.toJsonTree(PayrollPolicy.values()));
        stores.add("auditLog", gson.toJsonTree(auditLog));
        return stores;
    }

    /** Replaces every local store from JSON written by exportState. All-or-nothing: parsed first, then swapped in. */
    synchronized void importState(JsonObject stores) {
        com.google.gson.Gson gson = (dataFile != null ? dataFile : new LocalStateFile(Paths.get("unused.json"))).gson();
        InMemoryEmployeeStore employees = gson.fromJson(stores.get("employees"), InMemoryEmployeeStore.class);
        InMemoryEmployeeScheduleStore schedules = gson.fromJson(stores.get("schedules"), InMemoryEmployeeScheduleStore.class);
        InMemoryAttendanceStore attendance = gson.fromJson(stores.get("attendance"), InMemoryAttendanceStore.class);
        InMemoryWorkflowStore workflows = gson.fromJson(stores.get("workflows"), InMemoryWorkflowStore.class);
        SavedPayslipStore payslips = gson.fromJson(stores.get("savedPayslips"), SavedPayslipStore.class);
        PayrollAdjustments adjustments = gson.fromJson(stores.get("payrollAdjustments"), PayrollAdjustments.class);
        DisbursementStore disbursed = gson.fromJson(stores.get("disbursements"), DisbursementStore.class);
        WorkSchedule.Store work = gson.fromJson(stores.get("workSchedules"), WorkSchedule.Store.class);
        SalaryHistory salaries = gson.fromJson(stores.get("salaryHistory"), SalaryHistory.class);
        PayChangeRequests changes = gson.fromJson(stores.get("payChanges"), PayChangeRequests.class);
        HrRecords hr = gson.fromJson(stores.get("hrRecords"), HrRecords.class);
        GovernmentDetails government = gson.fromJson(stores.get("governmentDetails"), GovernmentDetails.class);
        StatutoryRates.Registry rates = gson.fromJson(stores.get("statutoryRates"), StatutoryRates.Registry.class);
        java.lang.reflect.Type jsonList = new com.google.gson.reflect.TypeToken<java.util.ArrayList<JsonObject>>() { }.getType();
        List<JsonObject> roleLog = gson.fromJson(stores.get("roleChanges"), jsonList);
        List<JsonObject> corrections = gson.fromJson(stores.get("timesheetCorrections"), jsonList);
        Map<String, AccessPolicy.Role> roles = gson.fromJson(stores.get("roles"),
            new com.google.gson.reflect.TypeToken<java.util.HashMap<String, AccessPolicy.Role>>() { }.getType());
        Map<String, String> supervisors = gson.fromJson(stores.get("supervisors"),
            new com.google.gson.reflect.TypeToken<java.util.HashMap<String, String>>() { }.getType());
        // Sections added after the first data-file release are optional, so older files still load.
        PayslipCorrections corrected = stores.has("payslipCorrections")
            ? gson.fromJson(stores.get("payslipCorrections"), PayslipCorrections.class) : new PayslipCorrections();
        PayslipOutbox outbox = stores.has("payslipOutbox") ? gson.fromJson(stores.get("payslipOutbox"), PayslipOutbox.class) : new PayslipOutbox();
        EmployeeChangelog changelogStore = stores.has("employeeChangelog") ? gson.fromJson(stores.get("employeeChangelog"), EmployeeChangelog.class) : new EmployeeChangelog();
        AdjustmentForms formsStore = stores.has("adjustmentForms") ? gson.fromJson(stores.get("adjustmentForms"), AdjustmentForms.class) : new AdjustmentForms();
        RateProposals proposals = stores.has("rateProposals") ? gson.fromJson(stores.get("rateProposals"), RateProposals.class) : new RateProposals();
        EmployeePhotos photosStore = stores.has("employeePhotos") ? gson.fromJson(stores.get("employeePhotos"), EmployeePhotos.class) : new EmployeePhotos();
        PersonalDetails personal = stores.has("personalDetails") ? gson.fromJson(stores.get("personalDetails"), PersonalDetails.class) : new PersonalDetails();
        Notifications notified = stores.has("notifications") ? gson.fromJson(stores.get("notifications"), Notifications.class) : new Notifications();
        AccountSecurity security = stores.has("accountSecurity") ? gson.fromJson(stores.get("accountSecurity"), AccountSecurity.class) : new AccountSecurity();
        PayrollPolicy.Values policy = stores.has("payrollPolicy") ? gson.fromJson(stores.get("payrollPolicy"), PayrollPolicy.Values.class) : null;
        Map<String, String> checkIns = stores.has("onsiteCheckIns") ? gson.fromJson(stores.get("onsiteCheckIns"),
            new com.google.gson.reflect.TypeToken<java.util.HashMap<String, String>>() { }.getType()) : new java.util.HashMap<>();
        Map<String, java.time.LocalDateTime> shifts = gson.fromJson(stores.get("openShifts"),
            new com.google.gson.reflect.TypeToken<java.util.HashMap<String, java.time.LocalDateTime>>() { }.getType());
        for (Object store : new Object[] {employees, schedules, attendance, workflows, payslips, adjustments, disbursed, work, salaries,
                changes, hr, government, rates, roleLog, corrections, roles, supervisors}) {
            if (store == null) throw new IllegalArgumentException("The data file is missing a section.");
        }
        localEmployeeStore = employees;
        localEmployeeService = new EmployeeService(employees);
        localScheduleStore = schedules;
        localAttendanceStore = attendance;
        localWorkflowStore = workflows;
        savedPayslips = payslips;
        payrollAdjustments = adjustments;
        disbursements = disbursed;
        workSchedules = work;
        salaryHistory = salaries;
        payChanges = changes;
        hrRecords = hr;
        governmentDetails = government;
        statutoryRates = rates;
        roleChanges = roleLog;
        timesheetCorrections = corrections;
        payslipCorrections = corrected;
        payslipOutbox = outbox;
        accountSecurity = security;
        notifications = notified;
        personalDetails = personal;
        employeePhotos = photosStore;
        rateProposals = proposals;
        adjustmentForms = formsStore;
        employeeChangelog = changelogStore;
        PayrollPolicy.importValues(policy);
        AccessPolicy.importRoles(roles, supervisors);
        clockService.importOpenShifts(shifts);
        clockService.importOnsiteCheckIns(checkIns);
    }

    /** The audit log is loaded from the data file but kept through a demo reset or a backup restore (the restore is logged). */
    private void importAudit(JsonObject stores) {
        if (!stores.has("auditLog")) return;
        AuditLog saved = (dataFile != null ? dataFile : new LocalStateFile(Paths.get("unused.json"))).gson().fromJson(stores.get("auditLog"), AuditLog.class);
        if (saved != null) auditLog = saved;
    }

    /** One audit entry per change request: the signed-in account (from the session), method, path, target and result. */
    private void audit(HttpExchange exchange, int status) {
        String session = exchange.getRequestHeaders().getFirst("X-Demo-Session");
        String named = String.valueOf(exchange.getRequestHeaders().getFirst("X-Demo-User") == null ? "" : exchange.getRequestHeaders().getFirst("X-Demo-User")).trim();
        String actor = twoFactor.sessionOwner(session).orElse(named.isEmpty() ? "no account" : named + " (not signed in)");
        Object target = exchange.getAttribute("auditTarget");
        auditLog.record(clockService.now().toString(), actor, exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
            target == null ? "" : target.toString(), status, oldest -> {
                if (dataFile == null) return null;
                try {
                    return dataFile.writeArchive("audit", dataFile.gson().toJsonTree(oldest));
                } catch (IOException ex) {
                    System.err.println("Could not archive old audit entries: " + ex);
                    return null;
                }
            });
    }

    /** After a successful change in the local demo, write the data file. A failed save is logged, never hidden. */
    private void saveDataFile() {
        if (dataFile == null) return;
        try {
            dataFile.save(exportState());
        } catch (IOException | RuntimeException ex) {
            System.err.println("Could not save local demo data to " + dataFile.path() + ": " + ex);
        }
    }

    /** Wraps each API handler so a successful change (or a logged full view of government numbers) is saved. */
    private void context(String path, com.sun.net.httpserver.HttpHandler handler) {
        server.createContext(path, exchange -> {
            boolean change = !"GET".equals(exchange.getRequestMethod())
                || String.valueOf(exchange.getRequestURI().getRawQuery()).contains("reveal=true");
            if (!change) {
                handler.handle(exchange);
                return;
            }
            // Changes run one at a time, so the saved file is never a half-applied mix of two requests.
            synchronized (stateLock) {
                handler.handle(exchange);
                int status = exchange.getResponseCode();
                if (firebaseProjectId.isEmpty()) audit(exchange, status);
                // Saved after every change request, refused ones too, so failed sign-ins stay in the audit log.
                if (firebaseProjectId.isEmpty()) saveDataFile();
            }
        });
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String host = System.getenv().getOrDefault("HOST", "0.0.0.0");
        AppServer app = new AppServer();
        app.start(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
    }

    HttpServer start(String host, int port) throws IOException {
        if (server != null) throw new IllegalStateException("server is already started");
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        context("/api/health", this::handleHealth);
        context("/api/employees", this::handleEmployees);
        context("/api/attendance", this::handleAttendance);
        context("/api/attendance/clock", this::handleClock);
        context("/api/attendance/corrections", this::handleTimesheetCorrections);
        context("/api/schedules", this::handleSchedules);
        context("/api/pay-changes", this::handlePayChanges);
        context("/api/hr", this::handleHr);
        context("/api/government", this::handleGovernment);
        context("/api/admin", this::handleAdmin);
        context("/api/reports/attendance", this::handleAttendanceReport);
        context("/api/workflows", this::handleWorkflows);
        context("/api/payroll/cutoffs", this::handlePayrollCutoffs);
        context("/api/payroll/statutory", this::handlePayrollStatutory);
        context("/api/payroll/payslip", this::handlePayrollPayslip);
        context("/api/payroll/payslips", this::handleSavedPayslips);
        context("/api/payroll/adjustments", this::handlePayrollAdjustments);
        context("/api/payroll/disbursements", this::handleDisbursements);
        context("/api/payroll/remittance", this::handleRemittance);
        context("/api/payroll/summary", this::handlePayrollSummary);
        context("/api/payroll/outbox", this::handleOutbox);
        context("/api/payroll/corrections", this::handlePayslipCorrections);
        context("/api/demo/seed", this::handleSeedDemo);
        context("/api/demo/accounts", this::handleDemoAccounts);
        context("/api/auth", this::handleAuth);
        context("/api/branches", this::handleBranches);
        context("/api/notifications", this::handleNotifications);
        context("/api/personal", this::handlePersonal);
        context("/api/changelog", this::handleChangelog);
        context("/api/rate-proposals", this::handleRateProposals);
        context("/api/payroll/adjustment-forms", this::handleAdjustmentForms);
        context("/api/team/today", this::handleTeamToday);
        server.createContext("/", this::handleStaticFiles);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.start();
        System.out.println("MotorPH Java API listening on " + host + ":" + server.getAddress().getPort());
        System.out.println("Mode: " + (firebaseProjectId.isEmpty() ? "local synthetic demo" : "Firebase-authenticated user sandbox"));
        System.out.println("Serving browser files from " + webRoot);
        System.out.println("Local demo data: " + (dataFile == null ? "in memory only" : dataFile.path()));
        return server;
    }

    void stop() {
        // Waits for a change that is being saved, so shutting down never cuts the data file short.
        synchronized (stateLock) {
            if (server != null) {
                server.stop(0);
                server = null;
            }
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
        sendJson(exchange, 200, "{\"status\":\"ok\",\"mode\":\"" + mode + "\",\"persistence\":\"" + (dataFile == null ? "memory" : "file") + "\"}");
    }

    private void handleEmployees(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method)) {
                RequestServices services = servicesFor(exchange);
                if ("/api/employees".equals(path) || "/api/employees/".equals(path)) {
                    String query = queryParameter(exchange.getRequestURI().getRawQuery(), "q");
                    AccessPolicy.Actor actor = actorFor(exchange);
                    List<Employee> matches = new java.util.ArrayList<>();
                    for (Employee item : services.employeeService.searchEmployees(query)) {
                        if (AccessPolicy.canRead(actor, AccessPolicy.Area.EMPLOYEES, item.getId())) matches.add(item);
                    }
                    int total = (int) services.employeeService.listEmployees().stream()
                        .filter(item -> AccessPolicy.canRead(actor, AccessPolicy.Area.EMPLOYEES, item.getId())).count();
                    sendJson(exchange, 200, employeesToJson(matches, total));
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
                AccessPolicy.Actor reader = actorFor(exchange);
                guard(() -> AccessPolicy.requireRead(reader, AccessPolicy.Area.EMPLOYEES, id));
                sendJson(exchange, 200, "{\"item\":" + employeeToJson(employee.get()) + "}");
                return;
            }
            if ("POST".equals(method) && path.matches("/api/employees/[A-Za-z0-9-]+/delete")) {
                deleteEmployee(exchange, actorFor(exchange), path.split("/")[3]);
                return;
            }
            if ("POST".equals(method) && "/api/employees".equals(path)) {
                AccessPolicy.Actor editor = actorFor(exchange);
                guard(() -> AccessPolicy.requireEmployeeEdit(editor));
                RequestServices services = servicesFor(exchange);
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "name", "department", "role", "location", "status");
                Employee created = services.employeeService.createEmployee(requiredString(body, "name"),
                    requiredString(body, "department"), requiredString(body, "role"),
                    requiredString(body, "location"), requiredString(body, "status"));
                sendJson(exchange, 201, "{\"item\":" + employeeToJson(created) + "}");
                return;
            }
            if ("PUT".equals(method) && path.startsWith("/api/employees/")) {
                AccessPolicy.Actor editor = actorFor(exchange);
                guard(() -> AccessPolicy.requireEmployeeEdit(editor));
                RequestServices services = servicesFor(exchange);
                String id = path.substring("/api/employees/".length());
                if (id.isEmpty() || id.contains("/")) throw new IllegalArgumentException("Employee ID is invalid.");
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "name", "department", "role", "location", "status");
                Optional<Employee> previous = firebaseProjectId.isEmpty() ? services.employeeService.findEmployee(id) : Optional.empty();
                Employee updated = services.employeeService.updateEmployee(id, requiredString(body, "name"),
                    requiredString(body, "department"), requiredString(body, "role"),
                    requiredString(body, "location"), requiredString(body, "status"));
                if (previous.isPresent()) logEmployeeEdit(previous.get(), updated, editor.employeeId, "Directory edit");
                sendJson(exchange, 200, "{\"item\":" + employeeToJson(updated) + "}");
                return;
            }
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
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

    private void handleAttendance(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            handleAttendanceCreate(exchange);
            return;
        }
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
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.ATTENDANCE, employeeId.trim()));
            RequestServices services = servicesFor(exchange);
            LocalDate from = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "from"));
            LocalDate to = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                localScheduleStore, services.attendanceStore);
            List<AttendanceRecord> records = attendanceService.findForEmployee(employeeId, from, to);
            JsonObject response = JsonParser.parseString(attendanceToJson(records)).getAsJsonObject();
            response.add("metrics", attendanceMetricsJson(exchange, employeeId.trim(), records, from, to));
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
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

    private void handleAttendanceCreate(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendJson(exchange, 403, "{\"error\":\"Raw attendance entry is disabled in Firebase mode; this demo writer is local-memory only.\"}");
            return;
        }
        try {
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "employeeId", "date", "timeIn", "timeOut");
            LocalDate date = LocalDate.parse(requiredString(body, "date").trim());
            LocalTime timeIn = parseAttendanceTime(requiredString(body, "timeIn"));
            LocalTime timeOut = parseAttendanceTime(requiredString(body, "timeOut"));
            AccessPolicy.Actor actor = actorFor(exchange);
            String target = requiredString(body, "employeeId").trim();
            guard(() -> AccessPolicy.requireAttendanceEdit(actor, target));
            RequestServices services = servicesFor(exchange);
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                localScheduleStore, services.attendanceStore);
            AttendanceRecord created = attendanceService.createForEmployee(
                requiredString(body, "employeeId"), date, timeIn, timeOut);
            sendJson(exchange, 201, "{\"item\":" + attendanceRecordToJson(created)
                + ",\"storage\":\"local-process-memory\",\"syntheticOnly\":true,\"hoursCalculated\":false}");
        } catch (DateTimeParseException ex) {
            sendJson(exchange, 400, "{\"error\":\"Date must use YYYY-MM-DD and times must use HH:mm\"}");
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    /** Clock in/out for the signed-in demo account (GET = status, POST {"action":"in"|"out"}). Server time, Asia/Manila. */
    private void handleClock(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendJson(exchange, 403, "{\"error\":\"Clock in/out is local-demo only; Firebase attendance is read-only.\"}");
            return;
        }
        String method = exchange.getRequestMethod();
        if (!"GET".equals(method) && !"POST".equals(method)) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if (!actor.explicit) throw new IllegalArgumentException("Choose a demo account first; clock in/out is always for the signed-in account.");
            RequestServices services = servicesFor(exchange);
            AttendanceService attendance = new AttendanceService(services.employeeService, localScheduleStore, services.attendanceStore);
            JsonObject response = new JsonObject();
            if ("POST".equals(method)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "action", "code");
                String action = requiredString(body, "action");
                if ("in".equals(action)) clockService.clockIn(attendance, actor.employeeId);
                else if ("qr".equals(action)) clockService.issueQr(actor.employeeId);
                else if ("onsite".equals(action)) clockService.scanOnsite(actor.employeeId, requiredString(body, "code"));
                else if ("out".equals(action)) response.add("record", JsonParser.parseString(attendanceRecordToJson(clockService.clockOut(attendance, actor.employeeId))));
                else throw new IllegalArgumentException("action must be in, out, qr or onsite.");
            }
            java.time.LocalDateTime now = clockService.now();
            Optional<java.time.LocalDateTime> open = clockService.openShift(actor.employeeId);
            List<AttendanceRecord> today = attendance.findForEmployee(actor.employeeId, now.toLocalDate(), now.toLocalDate());
            response.addProperty("employeeId", actor.employeeId);
            response.addProperty("today", now.toLocalDate().toString());
            response.addProperty("now", now.toLocalTime().toString());
            response.addProperty("clockedIn", open.isPresent());
            response.addProperty("since", open.map(time -> time.toLocalTime().toString()).orElse(""));
            response.addProperty("shiftEnd", workSchedules.forDate(actor.employeeId, now.toLocalDate()).shiftEnd().toString());
            response.addProperty("dayType", dayTypeOf(actor.employeeId, now.toLocalDate()));
            Optional<LocalTime> timeIn = open.isPresent() ? Optional.of(open.get().toLocalTime()) : today.stream().findFirst().map(AttendanceRecord::getTimeIn);
            response.addProperty("attendanceTag", timeIn.map(time -> attendanceTag(actor.employeeId, time)).orElse(""));
            response.addProperty("workHourTag", today.isEmpty() ? "" : workHourTag(actor.employeeId, today.get(0)));
            Optional<LocalTime> onsite = clockService.onsiteAt(actor.employeeId, open.map(java.time.LocalDateTime::toLocalDate).orElse(now.toLocalDate()));
            response.addProperty("location", onsite.isPresent() ? "Onsite" : "Offsite");
            response.addProperty("onsiteSince", onsite.map(LocalTime::toString).orElse(""));
            Optional<String> qr = open.isPresent() && !onsite.isPresent() ? clockService.currentQr(actor.employeeId) : Optional.empty();
            response.addProperty("qrCode", qr.orElse(""));
            response.addProperty("qrExpiresAt", qr.isPresent() ? clockService.qrExpiry(actor.employeeId).get().toLocalTime().toString() : "");
            if (today.isEmpty()) response.add("todayRecord", com.google.gson.JsonNull.INSTANCE);
            else response.add("todayRecord", JsonParser.parseString(attendanceRecordToJson(today.get(0))));
            sendJson(exchange, "POST".equals(method) ? 201 : 200, response.toString());
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
        }
    }

    /** Requirement 3: present/absent/tardy days, overtime/undertime and leave for the attendance range (see AttendanceMetrics). */
    private JsonObject attendanceMetricsJson(HttpExchange exchange, String employeeId, List<AttendanceRecord> records, LocalDate from, LocalDate to) {
        WorkflowService workflows = workflowsFor(exchange);
        List<DemoLeaveRequest> leaves = workflows.listLeaveRequests();
        Map<LocalDate, Integer> overtime = new java.util.HashMap<>();
        for (DemoOvertimeRequest request : workflows.listOvertimeRequests()) {
            if (request.getEmployeeId().equals(employeeId) && "Approved".equals(request.getStatus())) overtime.put(LocalDate.parse(request.getDate()), request.getMinutes());
        }
        boolean local = firebaseProjectId.isEmpty();
        LocalDate today = clockService.now().toLocalDate();
        AttendanceMetrics metrics = AttendanceMetrics.of(records, from, to, today, date -> local ? workSchedules.forDate(employeeId, date) : WorkSchedule.DEFAULT, overtime,
            day -> leaves.stream().anyMatch(leave -> leave.getEmployeeId().equals(employeeId) && "Approved".equals(leave.getStatus())
                && !day.isBefore(LocalDate.parse(leave.getStartDate())) && !day.isAfter(LocalDate.parse(leave.getEndDate()))),
            day -> !local || hrRecords.find(employeeId).map(record -> record.employedOn(day)).orElse(true));
        JsonObject json = new JsonObject();
        json.addProperty("from", metrics.from == null ? "" : metrics.from.toString());
        json.addProperty("to", metrics.to == null ? "" : metrics.to.toString());
        json.addProperty("presentDays", metrics.presentDays);
        json.addProperty("absentDays", metrics.absentDays);
        json.addProperty("tardyDays", metrics.tardyDays);
        json.addProperty("tardyMinutes", metrics.tardyMinutes);
        json.addProperty("overtimeMinutes", metrics.overtimeMinutes);
        json.addProperty("unapprovedExtraMinutes", metrics.unapprovedExtraMinutes);
        json.addProperty("undertimeMinutes", metrics.undertimeMinutes);
        json.addProperty("leaveDays", metrics.leaveDays);
        json.addProperty("holidayDays", metrics.holidayDays);
        int year = (metrics.to == null ? today : metrics.to).getYear();
        JsonArray credits = new JsonArray();
        for (LeaveBalance balance : workflows.leaveBalances(employeeId, year)) {
            JsonObject item = new JsonObject();
            item.addProperty("leaveType", balance.getLeaveType());
            item.addProperty("used", balance.getUsed());
            item.addProperty("remaining", balance.getRemaining());
            credits.add(item);
        }
        json.addProperty("creditYear", year);
        json.add("leaveCredits", credits);
        return json;
    }

    private static String nullToEmpty(String value) { return value == null ? "" : value; }

    private static PersonalDetails.Names namesFrom(JsonObject body) {
        return new PersonalDetails.Names(optionalString(body, "firstName"), optionalString(body, "surname"));
    }

    private static PersonalDetails.Address addressFrom(JsonObject body) {
        return new PersonalDetails.Address(optionalString(body, "building"), optionalString(body, "street"), optionalString(body, "city"),
            optionalString(body, "region"), optionalString(body, "postalCode"));
    }

    /**
     * System Admin delete (design doc RBAC: Employee C,R,U,D for the System Admin). Only for a record with no history,
     * such as one created by mistake: anyone with attendance, payslips, pay items, requests or a team is offboarded instead,
     * so pay records are never left without their employee. Type DELETE to confirm. Local demo only.
     */
    private void deleteEmployee(HttpExchange exchange, AccessPolicy.Actor actor, String id) throws IOException {
        if (!firebaseProjectId.isEmpty()) throw new RequestException(403, "Deleting employees is local-demo only.");
        if (actor.role != AccessPolicy.Role.ADMIN) throw new RequestException(403, "Only the System Admin deletes employee records.");
        exchange.setAttribute("auditTarget", id);
        JsonObject body = readJsonBody(exchange);
        requireOnlyKeys(body, "confirm");
        if (!"DELETE".equals(optionalString(body, "confirm"))) throw new IllegalArgumentException("Type DELETE to confirm.");
        Employee employee = localEmployeeService.findEmployee(id).orElseThrow(() -> new NoSuchElementException("Employee not found."));
        if (id.equals(actor.employeeId)) throw new IllegalStateException("You cannot delete your own record.");
        List<String> reasons = new java.util.ArrayList<>();
        try {
            if (!new AttendanceService(localEmployeeService, localScheduleStore, localAttendanceStore).findForEmployee(id, null, null).isEmpty()) reasons.add("attendance");
        } catch (RuntimeException unmapped) {
            // no schedule mapping means no attendance
        }
        if (savedPayslips.list().stream().anyMatch(slip -> slip.contains("\"payslipId\":\"PS-" + id + "-"))) reasons.add("finalized payslips");
        if (payrollAdjustments.allEarnings().stream().anyMatch(item -> item.employeeId.equals(id))
            || payrollAdjustments.allDeductions().stream().anyMatch(item -> item.employeeId.equals(id))) reasons.add("pay items");
        if (localWorkflowStore.listLeaveRequests().stream().anyMatch(item -> item.getEmployeeId().equals(id))
            || localWorkflowStore.listTickets().stream().anyMatch(item -> item.getEmployeeId().equals(id))
            || localWorkflowStore.listOvertimeRequests().stream().anyMatch(item -> item.getEmployeeId().equals(id))) reasons.add("requests or tickets");
        if (localEmployeeService.listEmployees().stream().anyMatch(other -> id.equals(AccessPolicy.supervisorOf(other.getId()).orElse("")))) reasons.add("a team reporting to them");
        if (!reasons.isEmpty()) {
            throw new IllegalStateException(employee.getName() + " has " + String.join(", ", reasons) + "; offboard them instead (HR desk → Onboarding), so their records stay.");
        }
        localEmployeeStore.remove(id);
        personalDetails.remove(id);
        employeePhotos.remove(id);
        hrRecords.remove(id);
        logChange(id, "Record", employee.getName() + " (" + employee.getDepartment() + ", " + employee.getRole() + ")", "deleted", actor.employeeId, "System Admin delete");
        sendJson(exchange, 200, "{\"deleted\":\"" + id + "\"}");
    }

    /** Employee change history (local demo; Firebase mode keeps none). */
    private void logChange(String employeeId, String field, String before, String after, String by, String via) {
        if (firebaseProjectId.isEmpty()) employeeChangelog.record(clockService.now().toString(), employeeId, field, before, after, by, via);
    }

    private void logEmployeeEdit(Employee before, Employee after, String by, String via) {
        logChange(after.getId(), "Name", before.getName(), after.getName(), by, via);
        logChange(after.getId(), "Department", before.getDepartment(), after.getDepartment(), by, via);
        logChange(after.getId(), "Position", before.getRole(), after.getRole(), by, via);
        logChange(after.getId(), "Branch", before.getLocation(), after.getLocation(), by, via);
        logChange(after.getId(), "Status", before.getStatus(), after.getStatus(), by, via);
    }

    private void logBasic(String employeeId, java.time.YearMonth effective, java.math.BigDecimal newBasic, String by, String via) {
        String old = salaryHistory.monthlyBasic(employeeId, effective).map(java.math.BigDecimal::toPlainString).orElse("");
        logChange(employeeId, "Monthly basic from " + effective, old, newBasic.toPlainString(), by, via);
    }

    /** A rates version from {effectiveMonth, note, sss, philHealth, pagIbig, bir} (checked by StatutoryRates.version). */
    private StatutoryRates ratesFromBody(JsonObject body, String id, String by, String note) {
        requireOnlyKeys(body, "effectiveMonth", "note", "sss", "philHealth", "pagIbig", "bir");
        JsonObject sss, ph, hdmf;
        try {
            sss = body.getAsJsonObject("sss");
            ph = body.getAsJsonObject("philHealth");
            hdmf = body.getAsJsonObject("pagIbig");
        } catch (ClassCastException ex) {
            throw new IllegalArgumentException("sss, philHealth and pagIbig must be objects.");
        }
        if (sss == null || ph == null || hdmf == null || !body.has("bir") || !body.get("bir").isJsonArray()) {
            throw new IllegalArgumentException("Send sss, philHealth, pagIbig and bir.");
        }
        java.util.List<long[]> bir = new java.util.ArrayList<>();
        for (JsonElement row : body.getAsJsonArray("bir")) {
            if (!row.isJsonObject()) throw new IllegalArgumentException("Each tax row needs over, baseTax and ratePercent.");
            JsonObject r = row.getAsJsonObject();
            bir.add(new long[] {pesos(r, "over"), pesos(r, "baseTax"), percentBp(r, "ratePercent")});
        }
        return StatutoryRates.version(id, parseMonth(requiredString(body, "effectiveMonth")), note, by,
            percentBp(sss, "employeePercent"), percentBp(sss, "employerPercent"), pesos(sss, "minMsc"), pesos(sss, "maxMsc"),
            percentBp(ph, "premiumPercent"), pesos(ph, "floor"), pesos(ph, "ceiling"),
            pesos(hdmf, "lowBand"), percentBp(hdmf, "lowPercent"), percentBp(hdmf, "highPercent"), percentBp(hdmf, "employerPercent"), pesos(hdmf, "cap"), bir);
    }

    /**
     * Rate changes that wait for approval (design doc, "Apply Global Adjustment" -> Pending): Payroll Staff POST a rates body to
     * /api/rate-proposals; the System Admin (not the proposer) POSTs /api/rate-proposals/{RP-0001}/approve or /reject.
     * GET lists the proposals and the rates versions (Payroll Staff and System Admin).
     */
    private void handleRateProposals(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Rate changes are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            boolean admin = actor.role == AccessPolicy.Role.ADMIN, payroll = actor.role == AccessPolicy.Role.PAYROLL;
            if (!admin && !payroll) throw new RequestException(403, "Only Payroll Staff and the System Admin work with tax tables and rates.");
            if ("GET".equals(exchange.getRequestMethod()) && "/api/rate-proposals".equals(path)) {
                JsonArray versions = new JsonArray();
                for (StatutoryRates version : statutoryRates.all()) versions.add(ratesToJson(version));
                JsonObject response = new JsonObject();
                response.add("proposals", new com.google.gson.Gson().toJsonTree(rateProposals.list()));
                response.add("versions", versions);
                response.addProperty("inForce", statutoryRates.ratesFor(java.time.YearMonth.from(clockService.now())).id);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            String stamp = clockService.now().toString();
            if ("/api/rate-proposals".equals(path)) {
                if (!payroll) throw new RequestException(403, "Payroll Staff propose rate changes; the System Admin can save a version directly in System config.");
                ratesFromBody(body, "check", actor.employeeId, optionalString(body, "note")); // checked now, and again on approval
                RateProposals.Proposal proposal = rateProposals.add(parseMonth(requiredString(body, "effectiveMonth")).toString(),
                    optionalString(body, "note"), actor.employeeId, stamp, body.toString());
                exchange.setAttribute("auditTarget", proposal.id);
                sendJson(exchange, 201, "{\"item\":" + new com.google.gson.Gson().toJson(proposal) + "}");
                return;
            }
            if (path.matches("/api/rate-proposals/RP-[0-9]{4,}/(approve|reject)")) {
                String[] parts = path.split("/");
                RateProposals.Proposal proposal = rateProposals.find(parts[3]);
                exchange.setAttribute("auditTarget", proposal.id);
                if (!admin) throw new RequestException(403, "Only the System Admin approves rate changes.");
                if (proposal.proposedBy.equals(actor.employeeId)) throw new RequestException(403, "Nobody approves their own rate change.");
                requireOnlyKeys(body);
                if ("approve".equals(parts[4])) {
                    JsonObject rates = JsonParser.parseString(proposal.rates).getAsJsonObject();
                    String note = (proposal.note.isEmpty() ? "" : proposal.note + " ") + "(proposed by " + proposal.proposedBy + ", approved by " + actor.employeeId + ")";
                    StatutoryRates version = statutoryRates.add(ratesFromBody(rates, statutoryRates.nextId(), proposal.proposedBy, note));
                    rateProposals.decide(proposal, "Approved", actor.employeeId, stamp, version.id, "Saved as " + version.id + ", effective " + proposal.effectiveMonth + ".");
                } else {
                    rateProposals.decide(proposal, "Rejected", actor.employeeId, stamp, "", "No change made.");
                }
                notify(proposal.proposedBy, actor, "Rates", "Your rate change " + proposal.id + " for " + proposal.effectiveMonth + " was "
                    + proposal.status.toLowerCase() + " by " + nameOf(actor.employeeId) + ". " + proposal.outcome);
                sendJson(exchange, 200, "{\"item\":" + new com.google.gson.Gson().toJson(proposal) + "}");
                return;
            }
            sendError(exchange, 404, "Rate change endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        }
    }

    /** In-app notification for the person affected (local demo only; never for your own action). */
    private void notify(String employeeId, AccessPolicy.Actor actor, String kind, String text) {
        if (!firebaseProjectId.isEmpty()) return;
        notifications.notify(employeeId, actor.explicit ? actor.employeeId : "", clockService.now().toString(), kind, text);
    }

    private static String workdaysText(WorkSchedule schedule) {
        StringBuilder days = new StringBuilder();
        for (java.time.DayOfWeek day : java.time.DayOfWeek.values()) {
            if (!schedule.workdays.contains(day)) continue;
            if (days.length() > 0) days.append(", ");
            days.append(day.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH));
        }
        return days.toString();
    }

    /** Your own notifications: GET lists them (newest first, with the unread count); POST /read marks them all read. */
    private void handleNotifications(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Notifications are local-demo only.");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if (!actor.explicit) throw new RequestException(400, "Choose a demo account first; notifications are always your own.");
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(exchange.getRequestMethod()) && "/api/notifications".equals(path)) {
                JsonObject response = new JsonObject();
                response.add("items", new com.google.gson.Gson().toJsonTree(notifications.forEmployee(actor.employeeId)));
                response.addProperty("unread", notifications.unread(actor.employeeId));
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && "/api/notifications/read".equals(path)) {
                requireOnlyKeys(readJsonBody(exchange));
                sendJson(exchange, 200, "{\"marked\":" + notifications.markAllRead(actor.employeeId) + "}");
                return;
            }
            sendError(exchange, 404, "Notifications endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    /**
     * Address and birthdate (RBAC matrix "Address": System Admin and HR create/update; Payroll, supervisors and employees read
     * only their own). GET /api/personal/{id}; PUT /api/personal/{id} {address, birthdate}. Local demo only.
     */
    private void handlePersonal(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Personal details are local-demo only.");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            String path = exchange.getRequestURI().getPath();
            if (path.matches("/api/personal/[A-Za-z0-9-]+/photo(/remove)?")) {
                handlePhoto(exchange, actor, path);
                return;
            }
            if (!path.matches("/api/personal/[A-Za-z0-9-]+")) throw new RequestException(404, "Personal details endpoint not found");
            String id = path.substring("/api/personal/".length());
            Employee employee = localEmployeeService.findEmployee(id).orElseThrow(() -> new NoSuchElementException("Employee not found."));
            boolean office = actor.role == AccessPolicy.Role.ADMIN || actor.role == AccessPolicy.Role.HR;
            if ("GET".equals(exchange.getRequestMethod())) {
                if (!office && !actor.employeeId.equals(id)) throw new RequestException(403, "You can see only your own address and birthdate.");
                JsonObject response = new JsonObject();
                response.addProperty("employeeId", id);
                response.addProperty("name", employee.getName());
                PersonalDetails.Record record = personalDetails.find(id).orElse(null);
                response.addProperty("address", record == null ? "" : record.address);
                // Older records have no name parts: offer the directory name split at the last space.
                String name = employee.getName();
                int split = name.lastIndexOf(' ');
                response.addProperty("firstName", record != null && record.firstName != null ? record.firstName : split > 0 ? name.substring(0, split) : name);
                response.addProperty("surname", record != null && record.surname != null ? record.surname : split > 0 ? name.substring(split + 1) : "");
                response.addProperty("gender", record == null || record.gender == null ? "" : record.gender);
                response.addProperty("email", record == null || record.email == null ? "" : record.email);
                response.addProperty("hasPhoto", employeePhotos.find(id).isPresent());
                for (String[] field : new String[][] {{"building", record == null ? null : record.building}, {"street", record == null ? null : record.street},
                        {"city", record == null ? null : record.city}, {"region", record == null ? null : record.region},
                        {"postalCode", record == null ? null : record.postalCode}, {"contactNumber", record == null ? null : record.contactNumber}}) {
                    response.addProperty(field[0], field[1] == null ? "" : field[1]);
                }
                response.addProperty("birthdate", record == null ? "" : record.birthdate);
                response.addProperty("updatedBy", record == null ? "" : record.updatedBy);
                response.addProperty("updatedAt", record == null ? "" : record.updatedAt);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if (!"PUT".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            if (!office) throw new RequestException(403, "Only HR or a System Admin can change an address or birthdate.");
            exchange.setAttribute("auditTarget", id);
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "firstName", "surname", "gender", "email", "building", "street", "city", "region", "postalCode", "contactNumber", "birthdate");
            Optional<PersonalDetails.Record> was = personalDetails.find(id);
            PersonalDetails.Record record = personalDetails.set(id, namesFrom(body), optionalString(body, "gender"), optionalString(body, "email"), addressFrom(body),
                optionalString(body, "contactNumber"), optionalString(body, "birthdate"), clockService.now().toLocalDate(), actor.employeeId, clockService.now().toString());
            // The directory name follows the first name and surname.
            String fullName = record.firstName + " " + record.surname;
            if (!fullName.equals(employee.getName())) {
                localEmployeeService.updateEmployee(id, fullName, employee.getDepartment(), employee.getRole(), employee.getLocation(), employee.getStatus());
                logChange(id, "Name", employee.getName(), fullName, actor.employeeId, "Personal details");
            }
            logChange(id, "Gender", was.map(item -> item.gender == null ? "" : item.gender).orElse(""), record.gender, actor.employeeId, "Personal details");
            logChange(id, "Personal email", was.map(item -> item.email == null ? "" : item.email).orElse(""), record.email, actor.employeeId, "Personal details");
            logChange(id, "Address", was.map(item -> item.address).orElse(""), record.address, actor.employeeId, "Personal details");
            logChange(id, "Contact number", was.map(item -> item.contactNumber == null ? "" : item.contactNumber).orElse(""), record.contactNumber, actor.employeeId, "Personal details");
            logChange(id, "Birthdate", was.map(item -> item.birthdate).orElse(""), record.birthdate, actor.employeeId, "Personal details");
            notify(id, actor, "Personal details", "HR updated your personal details (" + nameOf(actor.employeeId) + ").");
            sendJson(exchange, 200, "{\"item\":" + new com.google.gson.Gson().toJson(record) + "}");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    /**
     * Employee photo: GET /api/personal/{id}/photo returns the image to anyone who may see that employee in the directory;
     * PUT {photo: data URL} and POST /photo/remove are for HR and the System Admin. PNG or JPEG, at most 150 KB.
     */
    private void handlePhoto(HttpExchange exchange, AccessPolicy.Actor actor, String path) throws IOException {
        String id = path.split("/")[3];
        localEmployeeService.findEmployee(id).orElseThrow(() -> new NoSuchElementException("Employee not found."));
        boolean office = actor.role == AccessPolicy.Role.ADMIN || actor.role == AccessPolicy.Role.HR;
        if ("GET".equals(exchange.getRequestMethod()) && !path.endsWith("/remove")) {
            guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.EMPLOYEES, id));
            EmployeePhotos.Photo photo = employeePhotos.find(id).orElseThrow(() -> new NoSuchElementException("No photo yet."));
            byte[] bytes = photo.bytes();
            exchange.getResponseHeaders().set("Content-Type", photo.type);
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
            return;
        }
        if (!office) throw new RequestException(403, "Only HR or a System Admin can change a photo.");
        exchange.setAttribute("auditTarget", id);
        if ("POST".equals(exchange.getRequestMethod()) && path.endsWith("/remove")) {
            requireOnlyKeys(readJsonBody(exchange));
            if (employeePhotos.remove(id)) logChange(id, "Photo", "set", "", actor.employeeId, "Photo removed");
            sendJson(exchange, 200, "{\"removed\":true}");
            return;
        }
        if (!"PUT".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        JsonObject body = readJsonBody(exchange, 220 * 1024); // 150 KB of image is about 205 KB of Base64
        requireOnlyKeys(body, "photo");
        boolean had = employeePhotos.find(id).isPresent();
        EmployeePhotos.Photo photo = employeePhotos.set(id, requiredString(body, "photo"), actor.employeeId, clockService.now().toString());
        logChange(id, "Photo", had ? "set" : "", "set (" + photo.type + ")", actor.employeeId, "Photo upload");
        sendJson(exchange, 200, "{\"type\":\"" + photo.type + "\",\"updatedAt\":\"" + photo.updatedAt + "\"}");
    }

    /** GET /api/changelog?employeeId= : the employee change history, newest first (HR / System Admin anyone; others themselves). */
    private void handleChangelog(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "The change history is local-demo only.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            String id = nullToEmpty(queryParameter(exchange.getRequestURI().getRawQuery(), "employeeId")).trim();
            boolean office = actor.role == AccessPolicy.Role.ADMIN || actor.role == AccessPolicy.Role.HR;
            if (!office && (id.isEmpty() || !id.equals(actor.employeeId))) throw new RequestException(403, "You can see only your own change history.");
            sendJson(exchange, 200, "{\"entries\":" + new com.google.gson.Gson().toJson(employeeChangelog.forEmployee(id, 500)) + "}");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        }
    }

    /** Approved leave of this person covering the day, if any. */
    private Optional<DemoLeaveRequest> approvedLeaveOn(String employeeId, LocalDate day) {
        return localWorkflowStore.listLeaveRequests().stream().filter(leave -> leave.getEmployeeId().equals(employeeId) && "Approved".equals(leave.getStatus())
            && !day.isBefore(LocalDate.parse(leave.getStartDate())) && !day.isAfter(LocalDate.parse(leave.getEndDate()))).findFirst();
    }

    /** "Rest day", "Regular holiday", "On approved leave (Vacation)" or "Workday" for this person on that day. */
    private String dayTypeOf(String employeeId, LocalDate day) {
        Optional<DemoLeaveRequest> leave = approvedLeaveOn(employeeId, day);
        if (leave.isPresent()) return "On approved leave (" + leave.get().getLeaveType() + ")";
        WorkSchedule schedule = workSchedules.forDate(employeeId, day);
        if (day.getYear() != 2026) return schedule.isWorkday(day) ? "Workday" : "Rest day";
        HolidayCalendar2026.DayKind kind = HolidayCalendar2026.kindOf(day, !schedule.isWorkday(day));
        return kind == HolidayCalendar2026.DayKind.ORDINARY ? "Workday" : kind.label;
    }

    /** Present or Tardy, by the time in against the shift start plus the grace period. */
    private String attendanceTag(String employeeId, LocalTime timeIn) {
        return timeIn.isAfter(workSchedules.forDate(employeeId, clockService.now().toLocalDate()).graceEnd()) ? "Tardy" : "Present";
    }

    /** Regular, Overtime (approved overtime worked) or Undertime (short of the paid hours on a workday), as payroll counts it. */
    private String workHourTag(String employeeId, AttendanceRecord record) {
        WorkSchedule schedule = workSchedules.forDate(employeeId, record.getDate());
        int approved = localWorkflowStore.listOvertimeRequests().stream().filter(item -> item.getEmployeeId().equals(employeeId)
            && "Approved".equals(item.getStatus()) && item.getDate().equals(record.getDate().toString())).mapToInt(DemoOvertimeRequest::getMinutes).sum();
        long[] split = PayrollProfile.splitDay(record.getTimeIn(), record.getTimeOut(), approved, schedule);
        if (split[PayrollProfile.OVERTIME] > 0) return "Overtime";
        if (schedule.isWorkday(record.getDate()) && split[PayrollProfile.REGULAR] < PayrollPolicy.dailyMinutes()) return "Undertime";
        return "Regular";
    }

    /**
     * Team roster "Today" (design doc: Present, On leave, Absent): for everyone whose directory record you may read.
     * Absent only once the grace period has passed with no clock-in; before that "Not in yet".
     */
    private void handleTeamToday(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Today's attendance status is local-demo only.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            java.time.LocalDateTime now = clockService.now();
            LocalDate today = now.toLocalDate();
            AttendanceService attendance = new AttendanceService(localEmployeeService, localScheduleStore, localAttendanceStore);
            JsonArray items = new JsonArray();
            for (Employee employee : localEmployeeService.listEmployees()) {
                String id = employee.getId();
                if (!AccessPolicy.canRead(actor, AccessPolicy.Area.EMPLOYEES, id) || hrRecords.isSeparated(id, today)) continue;
                String day = dayTypeOf(id, today);
                Optional<java.time.LocalDateTime> open = clockService.openShift(id);
                List<AttendanceRecord> rows;
                try {
                    rows = attendance.findForEmployee(id, today, today);
                } catch (RuntimeException unmapped) {
                    rows = java.util.Collections.emptyList();
                }
                String status;
                if (open.isPresent()) status = attendanceTag(id, open.get().toLocalTime()) + " · clocked in " + open.get().toLocalTime()
                    + (clockService.onsiteAt(id, today).isPresent() ? " (onsite)" : " (offsite)");
                else if (!rows.isEmpty()) status = attendanceTag(id, rows.get(0).getTimeIn()) + " · " + rows.get(0).getTimeIn() + "–" + rows.get(0).getTimeOut();
                else if (day.startsWith("On approved leave")) status = "On leave";
                else if (!"Workday".equals(day)) status = day;
                else if (now.toLocalTime().isAfter(workSchedules.forDate(id, today).graceEnd())) status = "Absent";
                else status = "Not in yet";
                JsonObject item = new JsonObject();
                item.addProperty("employeeId", id);
                item.addProperty("status", status);
                items.add(item);
            }
            JsonObject response = new JsonObject();
            response.addProperty("today", today.toString());
            response.addProperty("now", now.toLocalTime().toString());
            response.add("items", items);
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        }
    }

    /** A whole-number field given as a JSON number or numeric text. */
    private static int intField(JsonObject body, String key) {
        if (!body.has(key) || body.get(key).isJsonNull()) throw new IllegalArgumentException(key + " is required.");
        try {
            return Integer.parseInt(body.get(key).getAsString().trim());
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(key + " must be a whole number.");
        }
    }

    private static LocalTime parseAttendanceTime(String value) {
        String time = value == null ? "" : value.trim();
        if (!time.matches("^([01][0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException("Time values must use 24-hour HH:mm format.");
        }
        return LocalTime.parse(time);
    }

    private void handleAttendanceReport(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.REPORTS));
            RequestServices services = servicesFor(exchange);
            String query = exchange.getRequestURI().getRawQuery();
            LocalDate from = parseOptionalDate(queryParameter(query, "from"));
            LocalDate to = parseOptionalDate(queryParameter(query, "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                localScheduleStore, services.attendanceStore);
            List<RawAttendanceSummary> summaries = attendanceService.summarizeRawRecords(from, to);
            sendJson(exchange, 200, attendanceSummaryToJson(summaries, from, to));
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (DateTimeParseException ex) {
            sendJson(exchange, 400, "{\"error\":\"Dates must use YYYY-MM-DD\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    private void handlePayrollCutoffs(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            String query = exchange.getRequestURI().getRawQuery();
            String throughText = queryParameter(query, "through");
            LocalDate through;
            try {
                through = throughText == null || throughText.isEmpty() ? null : LocalDate.parse(throughText);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("through must use YYYY-MM-DD.");
            }
            MonthPayroll payroll = monthPayrollFor(exchange, queryParameter(query, "employeeId").trim(), queryParameter(query, "month"), through);
            JsonArray items = new JsonArray();
            for (int i = 0; i < 2; i++) {
                CutoffPay cutoff = payroll.getCutoffs().get(i);
                JsonObject item = new JsonObject();
                item.addProperty("label", cutoff.getLabel());
                item.addProperty("from", cutoff.getFrom().toString());
                item.addProperty("to", cutoff.getTo().toString());
                item.addProperty("daysWithRecords", cutoff.getDaysWithRecords());
                item.addProperty("paidMinutes", cutoff.getPaidMinutes());
                item.addProperty("unpaidOverageMinutes", cutoff.getUnpaidOverageMinutes());
                addEarnings(item, cutoff);
                item.addProperty("grossPay", cutoff.getGrossPay().toPlainString());
                item.addProperty("bonusCents", payroll.bonusCents(i));
                item.addProperty("allowanceCents", payroll.allowanceTotalCents(i));
                item.addProperty("statutoryCents", payroll.statutoryCents(i));
                item.addProperty("voluntaryCents", payroll.voluntaryCents(i));
                item.addProperty("deductionsCents", payroll.deductionsCents(i));
                boolean blocked = payroll.isBlocked(i);
                if (blocked) item.add("netCents", com.google.gson.JsonNull.INSTANCE);
                else item.addProperty("netCents", payroll.netCents(i));
                item.addProperty("blocked", blocked);
                items.add(item);
            }
            JsonObject response = new JsonObject();
            response.addProperty("employeeId", payroll.getEmployeeId());
            response.addProperty("month", payroll.getMonth().toString());
            response.addProperty("hourlyRate", payroll.getHourlyRate().toPlainString());
            response.addProperty("monthlyBasic", (firebaseProjectId.isEmpty() ? salaryHistory.monthlyBasic(payroll.getEmployeeId(), payroll.getMonth())
                : PayrollProfile.monthlyBasic(payroll.getEmployeeId())).get().toPlainString());
            response.add("cutoffs", items);
            response.addProperty("monthlyGross", java.math.BigDecimal.valueOf(payroll.getMonthlyGrossCents(), 2).toPlainString());
            response.addProperty("profileVersion", PayrollProfile.VERSION);
            response.addProperty("temporary", true);
            response.addProperty("deductionsApplied", true);
            response.addProperty("deductionSplit", "half-per-cutoff");
            if (payroll.getStatutory() != null) response.add("statutory", statutoryToJson(payroll.getStatutory()));
            else response.add("statutory", com.google.gson.JsonNull.INSTANCE);
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /** Read-only payslip view for one cutoff; nothing is saved and it is labelled as synthetic. */
    private void handlePayrollPayslip(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            String query = exchange.getRequestURI().getRawQuery();
            JsonObject response = new JsonObject();
            response.add("payslip", buildPayslip(exchange, queryParameter(query, "employeeId"), queryParameter(query, "month"),
                queryParameter(query, "cutoff")));
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"Employee not found\"}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    /** Builds the payslip for one cutoff from live data. Throws IllegalArgument (400), IllegalState (409) or NoSuchElement (404). */
    private JsonObject buildPayslip(HttpExchange exchange, String employeeIdText, String monthText, String cutoffText) {
        if (!"1".equals(cutoffText) && !"2".equals(cutoffText)) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
        int index = Integer.parseInt(cutoffText) - 1;
        String employeeId = employeeIdText == null ? "" : employeeIdText.trim();
        MonthPayroll payroll = monthPayrollFor(exchange, employeeId, monthText);
        CutoffPay cutoff = payroll.getCutoffs().get(index);
        if (payroll.grossCents(index) == 0) throw new IllegalStateException("No paid attendance in this cutoff, so there is no payslip.");
        if (payroll.isBlocked(index)) throw new IllegalStateException("Deductions exceed this cutoff's gross; no payslip is produced rather than a negative net.");
        Employee employee = servicesFor(exchange).employeeService.findEmployee(employeeId).orElseThrow(NoSuchElementException::new);

        JsonObject person = new JsonObject();
        person.addProperty("id", employee.getId());
        person.addProperty("name", employee.getName());
        person.addProperty("department", employee.getDepartment());
        person.addProperty("role", employee.getRole());
        person.addProperty("location", employee.getLocation());
        JsonObject period = new JsonObject();
        period.addProperty("label", cutoff.getLabel());
        period.addProperty("from", cutoff.getFrom().toString());
        period.addProperty("to", cutoff.getTo().toString());
        JsonObject deductions = new JsonObject();
        deductions.addProperty("sssCents", payroll.lineCents(index, MonthPayroll.SSS));
        deductions.addProperty("philHealthCents", payroll.lineCents(index, MonthPayroll.PHILHEALTH));
        deductions.addProperty("pagIbigCents", payroll.lineCents(index, MonthPayroll.PAGIBIG));
        deductions.addProperty("withholdingTaxCents", payroll.lineCents(index, MonthPayroll.TAX));
        deductions.addProperty("statutoryCents", payroll.statutoryCents(index));
        JsonArray voluntary = new JsonArray();
        for (MonthPayroll.Taken taken : payroll.voluntary(index)) {
            JsonObject line = new JsonObject();
            line.addProperty("id", taken.item.id);
            line.addProperty("type", taken.item.type);
            line.addProperty("description", taken.item.description);
            line.addProperty("amountCents", taken.takenCents);
            line.addProperty("shortfallCents", taken.shortfallCents);
            voluntary.add(line);
        }
        deductions.add("voluntary", voluntary);
        deductions.addProperty("voluntaryCents", payroll.voluntaryCents(index));
        deductions.addProperty("totalCents", payroll.deductionsCents(index));
        JsonArray bonuses = new JsonArray();
        for (PayrollAdjustments.Earning earning : payroll.bonuses(index)) {
            JsonObject line = new JsonObject();
            line.addProperty("id", earning.id);
            line.addProperty("type", earning.type);
            line.addProperty("description", earning.description);
            line.addProperty("amountCents", earning.amountCents);
            bonuses.add(line);
        }
        JsonObject allowances = new JsonObject();
        allowances.addProperty("riceCents", payroll.allowanceCents(index, MonthPayroll.RICE));
        allowances.addProperty("phoneCents", payroll.allowanceCents(index, MonthPayroll.PHONE));
        allowances.addProperty("clothingCents", payroll.allowanceCents(index, MonthPayroll.CLOTHING));
        JsonArray corrections = new JsonArray();
        for (PayrollAdjustments.Earning item : payroll.allowanceCorrections(index)) {
            JsonObject line = new JsonObject();
            line.addProperty("id", item.id);
            line.addProperty("type", item.type);
            line.addProperty("description", item.description);
            line.addProperty("amountCents", item.amountCents);
            corrections.add(line);
        }
        allowances.addProperty("totalCents", payroll.allowanceTotalCents(index));
        allowances.add("corrections", corrections);
        JsonObject employer = new JsonObject();
        employer.addProperty("sssCents", payroll.employerCents(index, MonthPayroll.SSS));
        employer.addProperty("philHealthCents", payroll.employerCents(index, MonthPayroll.PHILHEALTH));
        employer.addProperty("pagIbigCents", payroll.employerCents(index, MonthPayroll.PAGIBIG));
        JsonObject slip = new JsonObject();
        slip.addProperty("payslipId", "PS-" + employee.getId() + "-" + payroll.getMonth() + "-" + cutoffText);
        slip.add("employee", person);
        slip.addProperty("month", payroll.getMonth().toString());
        slip.add("period", period);
        slip.addProperty("daysWithRecords", cutoff.getDaysWithRecords());
        slip.addProperty("paidMinutes", cutoff.getPaidMinutes());
        slip.addProperty("unpaidOverageMinutes", cutoff.getUnpaidOverageMinutes());
        addEarnings(slip, cutoff);
        slip.addProperty("hourlyRate", payroll.getHourlyRate().toPlainString());
        slip.addProperty("earnedPayCents", payroll.earnedCents(index));
        slip.add("bonuses", bonuses);
        slip.addProperty("bonusCents", payroll.bonusCents(index));
        slip.addProperty("grossPayCents", payroll.grossCents(index));
        slip.add("deductions", deductions);
        slip.add("allowances", allowances);
        slip.add("employerContributions", employer);
        slip.addProperty("payMethod", hrRecords.payMethod(employee.getId()));
        slip.addProperty("netPayCents", payroll.netCents(index));
        slip.addProperty("rulesetId", payroll.getStatutory() != null ? payroll.getStatutory().rulesetId : statutoryRates.ratesFor(payroll.getMonth()).id);
        slip.addProperty("profileVersion", PayrollProfile.VERSION);
        slip.add("payrollPolicy", new com.google.gson.Gson().toJsonTree(PayrollPolicy.values()));
        slip.addProperty("generatedAt", java.time.Instant.now().toString());
        slip.addProperty("temporary", true);
        slip.addProperty("saved", false);
        return slip;
    }

    private static void sendError(HttpExchange exchange, int status, String message) throws IOException {
        sendJson(exchange, status, "{\"error\":\"" + escapeJson(message) + "\"}");
    }

    private static final String[] DAY_CODES = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    private static JsonObject scheduleToJson(WorkSchedule schedule) {
        JsonObject json = new JsonObject();
        json.addProperty("employeeId", schedule.employeeId);
        json.addProperty("shiftStart", schedule.shiftStart.toString());
        json.addProperty("shiftEnd", schedule.shiftEnd().toString());
        json.addProperty("mealStart", schedule.mealStart().toString());
        JsonArray days = new JsonArray();
        for (java.time.DayOfWeek day : schedule.workdays) days.add(DAY_CODES[day.getValue() - 1]);
        json.add("workdays", days);
        json.addProperty("updatedBy", schedule.updatedBy);
        json.addProperty("updatedAt", schedule.updatedAt);
        return json;
    }

    /** GET team schedules; PUT /api/schedules/{employeeId} {shiftStart, workdays[]} by the supervisor or admin. Local demo. */
    private void handleSchedules(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Team schedules are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod()) && "/api/schedules".equals(path)) {
                JsonArray items = new JsonArray();
                for (Employee employee : localEmployeeService.listEmployees()) {
                    if (!AccessPolicy.canRead(actor, AccessPolicy.Area.EMPLOYEES, employee.getId())) continue;
                    JsonObject item = scheduleToJson(workSchedules.get(employee.getId()));
                    item.add("dayChanges", new com.google.gson.Gson().toJsonTree(workSchedules.dayChangesFor(employee.getId())));
                    items.add(item);
                }
                sendJson(exchange, 200, "{\"schedules\":" + items + "}");
                return;
            }
            // Single-day / multi-day changes: POST /api/schedules/{id}/days {from, to, shiftStart | dayOff:true, reason}; POST /api/schedules/days/{SC-0001}/remove.
            if ("POST".equals(exchange.getRequestMethod()) && path.matches("/api/schedules/days/SC-[0-9]{4,}/remove")) {
                String changeId = path.split("/")[4];
                WorkSchedule.DayChange existing = workSchedules.dayChangesForAll().stream().filter(item -> item.id.equals(changeId)).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Day change not found."));
                guard(() -> AccessPolicy.requireScheduleEdit(actor, existing.employeeId));
                requireOnlyKeys(readJsonBody(exchange));
                workSchedules.removeDayChange(changeId);
                notify(existing.employeeId, actor, "Schedule", "Your schedule change for " + existing.from + (existing.from.equals(existing.to) ? "" : " to " + existing.to)
                    + " was removed by " + nameOf(actor.employeeId) + "; your weekly pattern applies again.");
                sendJson(exchange, 200, "{\"removed\":\"" + changeId + "\"}");
                return;
            }
            if ("POST".equals(exchange.getRequestMethod()) && path.matches("/api/schedules/[A-Za-z0-9-]+/days")) {
                String target = path.split("/")[3];
                localEmployeeService.findEmployee(target).orElseThrow(() -> new NoSuchElementException("Employee not found."));
                guard(() -> AccessPolicy.requireScheduleEdit(actor, target));
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "from", "to", "shiftStart", "dayOff", "reason");
                boolean dayOff = body.has("dayOff") && body.get("dayOff").isJsonPrimitive() && body.get("dayOff").getAsBoolean();
                LocalDate first, last;
                try {
                    first = LocalDate.parse(requiredString(body, "from").trim());
                    last = LocalDate.parse(optionalString(body, "to").trim().isEmpty() ? first.toString() : optionalString(body, "to").trim());
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Dates must use YYYY-MM-DD.");
                }
                WorkSchedule.DayChange change = workSchedules.addDayChange(target, first, last, dayOff ? null : parseAttendanceTime(requiredString(body, "shiftStart")),
                    optionalString(body, "reason"), actor.employeeId);
                String dates = change.from.equals(change.to) ? change.from : change.from + " to " + change.to;
                notify(target, actor, "Schedule", (change.dayOff() ? "You have the day off on " + dates : "Your shift on " + dates + " starts at " + change.shiftStart)
                    + " (" + change.reason + "; set by " + nameOf(actor.employeeId) + ").");
                sendJson(exchange, 201, "{\"item\":" + new com.google.gson.Gson().toJson(change) + "}");
                return;
            }
            if (!"PUT".equals(exchange.getRequestMethod()) || !path.startsWith("/api/schedules/")) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            String employeeId = path.substring("/api/schedules/".length());
            localEmployeeService.findEmployee(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
            guard(() -> AccessPolicy.requireScheduleEdit(actor, employeeId));
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "shiftStart", "workdays");
            JsonElement days = body.get("workdays");
            if (days == null || !days.isJsonArray()) throw new IllegalArgumentException("workdays must be a list such as [\"MON\",\"TUE\"].");
            java.util.Set<java.time.DayOfWeek> workdays = java.util.EnumSet.noneOf(java.time.DayOfWeek.class);
            for (JsonElement day : days.getAsJsonArray()) {
                int index = java.util.Arrays.asList(DAY_CODES).indexOf(day.isJsonPrimitive() ? day.getAsString() : "");
                if (index < 0) throw new IllegalArgumentException("Workdays use MON, TUE, WED, THU, FRI, SAT, SUN.");
                workdays.add(java.time.DayOfWeek.of(index + 1));
            }
            WorkSchedule saved = workSchedules.set(employeeId, parseAttendanceTime(requiredString(body, "shiftStart")), workdays, actor.employeeId);
            notify(employeeId, actor, "Schedule", "Your schedule changed: shift from " + saved.shiftStart + " to " + saved.shiftEnd() + ", "
                + workdaysText(saved) + " (set by " + nameOf(actor.employeeId) + ").");
            sendJson(exchange, 200, "{\"schedule\":" + scheduleToJson(saved) + "}");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        }
    }

    /** Supervisor timesheet corrections with a reason; the original times are kept in the log. Local demo. */
    private void handleTimesheetCorrections(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Timesheet corrections are local-demo only; Firebase attendance is read-only.");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod())) {
                String employeeId = queryParameter(exchange.getRequestURI().getRawQuery(), "employeeId");
                JsonArray items = new JsonArray();
                synchronized (timesheetCorrections) {
                    for (JsonObject item : timesheetCorrections) {
                        String owner = item.get("employeeId").getAsString();
                        if ((employeeId == null || employeeId.isEmpty() || employeeId.equals(owner))
                                && AccessPolicy.canRead(actor, AccessPolicy.Area.ATTENDANCE, owner)) items.add(item);
                    }
                }
                sendJson(exchange, 200, "{\"corrections\":" + items + "}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "employeeId", "attendanceId", "timeIn", "timeOut", "reason");
            String employeeId = requiredString(body, "employeeId").trim();
            guard(() -> AccessPolicy.requireAttendanceEdit(actor, employeeId));
            String reason = requiredString(body, "reason").trim();
            if (reason.isEmpty() || reason.length() > 200) throw new IllegalArgumentException("A reason (up to 200 characters) is required.");
            JsonElement idElement = body.get("attendanceId");
            if (idElement == null || !idElement.isJsonPrimitive() || !idElement.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("attendanceId must be a number.");
            RequestServices services = servicesFor(exchange);
            AttendanceService attendance = new AttendanceService(services.employeeService, localScheduleStore, services.attendanceStore);
            AttendanceRecord[] change = attendance.correctForEmployee(employeeId, idElement.getAsInt(),
                parseAttendanceTime(requiredString(body, "timeIn")), parseAttendanceTime(requiredString(body, "timeOut")));
            JsonObject item = new JsonObject();
            item.addProperty("attendanceId", change[1].getAttendanceId());
            item.addProperty("employeeId", employeeId);
            item.addProperty("date", change[1].getDate().toString());
            item.addProperty("originalTimeIn", change[0].getTimeIn().toString());
            item.addProperty("originalTimeOut", change[0].getTimeOut().toString());
            item.addProperty("timeIn", change[1].getTimeIn().toString());
            item.addProperty("timeOut", change[1].getTimeOut().toString());
            item.addProperty("reason", reason);
            item.addProperty("editedBy", actor.employeeId);
            item.addProperty("editedAt", java.time.Instant.now().toString());
            synchronized (timesheetCorrections) { timesheetCorrections.add(0, item); }
            notify(employeeId, actor, "Attendance", "Your attendance on " + change[1].getDate() + " was corrected to " + change[1].getTimeIn() + "–"
                + change[1].getTimeOut() + " by " + nameOf(actor.employeeId) + ": " + reason);
            sendJson(exchange, 201, "{\"correction\":" + item + "}");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        }
    }

    private static JsonObject payChangeToJson(PayChangeRequests.Request request, String employeeName) {
        JsonObject json = new JsonObject();
        json.addProperty("id", request.id);
        json.addProperty("employeeId", request.employeeId);
        json.addProperty("employeeName", employeeName);
        json.addProperty("type", request.type);
        json.addProperty("newPosition", request.newPosition);
        json.addProperty("newMonthlyBasic", request.newMonthlyBasic == null ? "" : request.newMonthlyBasic.toPlainString());
        json.addProperty("effectiveMonth", request.effectiveMonth == null ? "" : request.effectiveMonth.toString());
        json.addProperty("bonusCents", request.bonusCents);
        json.addProperty("bonusMonth", request.bonusMonth == null ? "" : request.bonusMonth.toString());
        json.addProperty("bonusCutoff", request.bonusCutoff);
        json.addProperty("startDate", request.startDate == null ? "" : request.startDate.toString());
        json.addProperty("endDate", request.endDate == null ? "" : request.endDate.toString());
        json.addProperty("note", request.note);
        json.addProperty("status", request.status);
        json.addProperty("requestedBy", request.requestedBy);
        json.addProperty("requestedAt", request.requestedAt);
        json.addProperty("reviewerId", request.reviewerId);
        json.addProperty("reviewedAt", request.reviewedAt);
        json.addProperty("outcome", request.outcome);
        json.addProperty("approverRole", "BONUS".equals(request.type) ? "PAYROLL" : "HR");
        return json;
    }

    private String nameOf(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) return "the system";
        return localEmployeeService.findEmployee(employeeId).map(Employee::getName).orElse(employeeId);
    }

    /** Promotion / merit / bonus / disciplinary-leave requests: filed by supervisors, decided by HR or Payroll. Local demo. */
    private void handlePayChanges(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Pay-change requests are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod()) && "/api/pay-changes".equals(path)) {
                JsonArray items = new JsonArray();
                for (PayChangeRequests.Request request : payChanges.list()) {
                    if (AccessPolicy.canRead(actor, AccessPolicy.Area.PAY_CHANGES, request.employeeId)) items.add(payChangeToJson(request, nameOf(request.employeeId)));
                }
                sendJson(exchange, 200, "{\"requests\":" + items + "}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            if ("/api/pay-changes".equals(path)) {
                requireOnlyKeys(body, "employeeId", "type", "newPosition", "newMonthlyBasic", "effectiveMonth", "amount", "month", "cutoff", "startDate", "endDate", "note");
                String employeeId = requiredString(body, "employeeId").trim();
                localEmployeeService.findEmployee(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
                guard(() -> AccessPolicy.requirePayChangeRequest(actor, employeeId));
                String type = requiredString(body, "type");
                if (!"DISCIPLINARY".equals(type) && !hasPayProfile(employeeId)) throw new IllegalArgumentException("That employee has no pay profile.");
                String basic = optionalString(body, "newMonthlyBasic").trim();
                String effective = optionalString(body, "effectiveMonth").trim();
                String amount = optionalString(body, "amount").trim();
                String month = optionalString(body, "month").trim();
                String cutoff = optionalString(body, "cutoff").trim();
                String start = optionalString(body, "startDate").trim();
                String end = optionalString(body, "endDate").trim();
                PayChangeRequests.Request created;
                try {
                    created = payChanges.create(employeeId, type, optionalString(body, "newPosition"),
                        basic.isEmpty() ? null : java.math.BigDecimal.valueOf(StatutoryCalculator2026.parsePesoCents(basic, "New monthly basic"), 2),
                        effective.isEmpty() ? null : parseMonth(effective),
                        amount.isEmpty() ? 0 : StatutoryCalculator2026.parsePesoCents(amount, "Bonus amount"),
                        month.isEmpty() ? null : parseMonth(month), cutoff.isEmpty() ? 0 : Integer.parseInt(cutoff),
                        start.isEmpty() ? null : LocalDate.parse(start), end.isEmpty() ? null : LocalDate.parse(end),
                        optionalString(body, "note"), actor.employeeId);
                } catch (DateTimeParseException | NumberFormatException ex) {
                    throw new IllegalArgumentException("Dates must use YYYY-MM-DD and cutoff must be 1 or 2.");
                }
                sendJson(exchange, 201, "{\"item\":" + payChangeToJson(created, nameOf(employeeId)) + "}");
                return;
            }
            if (path.matches("/api/pay-changes/PC-[A-F0-9]{12}/(approve|reject)")) {
                requireOnlyKeys(body);
                String[] parts = path.split("/");
                PayChangeRequests.Request current = payChanges.find(parts[3]);
                guard(() -> AccessPolicy.requirePayChangeReview(actor, current.type, current.employeeId, current.requestedBy));
                PayChangeRequests.Request decided = payChanges.decide(parts[3], parts[4], actor.employeeId, new PayChangeRequests.Effects() {
                    @Override public void promote(String employeeId, String newPosition, java.time.YearMonth effective, java.math.BigDecimal newMonthlyBasic) {
                        Employee person = localEmployeeService.findEmployee(employeeId).orElseThrow(NoSuchElementException::new);
                        // shortcut: the position title changes on approval; only the salary is effective-dated.
                        localEmployeeService.updateEmployee(employeeId, person.getName(), person.getDepartment(), newPosition, person.getLocation(), person.getStatus());
                        logChange(employeeId, "Position", person.getRole(), newPosition, actor.employeeId, "Promotion " + parts[3]);
                        logBasic(employeeId, effective, newMonthlyBasic, actor.employeeId, "Promotion " + parts[3]);
                        salaryHistory.set(employeeId, effective, newMonthlyBasic);
                    }
                    @Override public void raise(String employeeId, java.time.YearMonth effective, java.math.BigDecimal newMonthlyBasic) {
                        logBasic(employeeId, effective, newMonthlyBasic, actor.employeeId, "Merit increase " + parts[3]);
                        salaryHistory.set(employeeId, effective, newMonthlyBasic);
                    }
                    @Override public void bonus(String employeeId, java.time.YearMonth month, int cutoff, long cents, String note, String approverId) {
                        if (savedPayslips.find("PS-" + employeeId + "-" + month + "-" + cutoff).isPresent()) {
                            throw new IllegalStateException("That payslip is already finalized; ask for the bonus on a later cutoff.");
                        }
                        payrollAdjustments.addEarning(employeeId, month, cutoff, "Bonus", note, cents, approverId);
                    }
                    @Override public void unpaidLeave(String employeeId, LocalDate start, LocalDate end, String note, String approverId) {
                        WorkflowService workflows = new WorkflowService(localEmployeeService, localWorkflowStore);
                        DemoLeaveRequest leave = workflows.createLeaveRequest(employeeId, "Unpaid", start.toString(), end.toString(), "Disciplinary: " + note);
                        workflows.reviewLeaveRequest(leave.getId(), "approve", approverId);
                    }
                });
                notify(decided.employeeId, actor, "Pay change", "Your " + decided.type.toLowerCase() + " request " + decided.id + " was "
                    + decided.status.toLowerCase() + " by " + nameOf(actor.employeeId) + (decided.outcome == null || decided.outcome.isEmpty() ? "." : ": " + decided.outcome));
                sendJson(exchange, 200, "{\"item\":" + payChangeToJson(decided, nameOf(decided.employeeId)) + "}");
                return;
            }
            sendError(exchange, 404, "Pay-change endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        }
    }

    private boolean hasPayProfile(String employeeId) {
        return salaryHistory.monthlyBasic(employeeId, java.time.YearMonth.of(2026, 12)).isPresent();
    }

    private JsonObject employmentToJson(HrRecords.Employment record) {
        JsonObject json = new JsonObject();
        json.addProperty("employeeId", record.employeeId);
        json.addProperty("employeeName", nameOf(record.employeeId));
        json.addProperty("startDate", record.startDate == null ? "" : record.startDate.toString());
        json.addProperty("lastDay", record.lastDay == null ? "" : record.lastDay.toString());
        json.addProperty("separationReason", record.separationReason);
        json.addProperty("supervisorId", AccessPolicy.supervisorOf(record.employeeId).orElse(""));
        json.addProperty("payMethod", record.payMethod);
        json.addProperty("separated", hrRecords.isSeparated(record.employeeId, clockService.now().toLocalDate()));
        for (String[] list : new String[][] {{"onboarding"}, {"offboarding"}}) {
            JsonArray tasks = new JsonArray();
            for (HrRecords.Task task : "onboarding".equals(list[0]) ? record.onboarding : record.offboarding) {
                JsonObject item = new JsonObject();
                item.addProperty("task", task.name);
                item.addProperty("done", task.isDone());
                item.addProperty("doneBy", task.doneBy);
                item.addProperty("doneAt", task.doneAt);
                tasks.add(item);
            }
            json.add(list[0], tasks);
        }
        if (record.lastDay != null) {
            // The final payslip is the cutoff that contains the last day.
            json.addProperty("finalMonth", java.time.YearMonth.from(record.lastDay).toString());
            json.addProperty("finalCutoff", record.lastDay.getDayOfMonth() <= 15 ? 1 : 2);
        }
        return json;
    }

    /** HR: department/position lists, onboarding, offboarding and their checklists. Local demo only. */
    private void handleHr(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "HR onboarding is local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(method) && "/api/hr/catalog".equals(path)) {
                JsonObject response = new JsonObject();
                JsonArray departments = new JsonArray();
                hrRecords.departments().forEach(departments::add);
                JsonArray positions = new JsonArray();
                hrRecords.positions().forEach(positions::add);
                response.add("departments", departments);
                response.add("positions", positions);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && "/api/hr/employment".equals(path)) {
                guard(() -> AccessPolicy.requireHrManagement(actor, null));
                JsonArray items = new JsonArray();
                for (HrRecords.Employment record : hrRecords.all()) items.add(employmentToJson(record));
                sendJson(exchange, 200, "{\"records\":" + items + "}");
                return;
            }
            if (!"POST".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            if ("/api/hr/catalog".equals(path)) {
                guard(() -> AccessPolicy.requireHrManagement(actor, null));
                requireOnlyKeys(body, "kind", "name");
                hrRecords.addCatalogItem(requiredString(body, "kind"), requiredString(body, "name"));
                sendJson(exchange, 201, "{\"added\":\"" + escapeJson(requiredString(body, "name").trim()) + "\"}");
                return;
            }
            if ("/api/hr/catalog/rename".equals(path)) {
                guard(() -> AccessPolicy.requireHrManagement(actor, null));
                requireOnlyKeys(body, "kind", "from", "to");
                String kind = requiredString(body, "kind");
                String from = requiredString(body, "from");
                String to = requiredString(body, "to").trim();
                hrRecords.renameCatalogItem(kind, from, to);
                int updated = 0;
                for (Employee person : localEmployeeService.listEmployees()) {
                    boolean department = "department".equals(kind) && person.getDepartment().equals(from);
                    boolean position = "position".equals(kind) && person.getRole().equals(from);
                    if (!department && !position) continue;
                    localEmployeeService.updateEmployee(person.getId(), person.getName(), department ? to : person.getDepartment(),
                        position ? to : person.getRole(), person.getLocation(), person.getStatus());
                    updated++;
                }
                sendJson(exchange, 200, "{\"renamed\":\"" + escapeJson(to) + "\",\"employeesUpdated\":" + updated + "}");
                return;
            }
            if ("/api/hr/onboard".equals(path)) {
                guard(() -> AccessPolicy.requireHrManagement(actor, null));
                requireOnlyKeys(body, "name", "department", "position", "location", "startDate", "supervisorId",
                    "monthlyBasic", "riceAllowance", "phoneAllowance", "clothingAllowance", "payMethod", "firstName", "surname", "gender", "email", "building", "street", "city", "region", "postalCode", "contactNumber", "birthdate");
                boolean withPersonal = !optionalString(body, "birthdate").isEmpty() || !optionalString(body, "street").isEmpty() || !optionalString(body, "contactNumber").isEmpty();
                // Checked before anything is created, so a bad birthdate or address never leaves a half-made hire.
                if (withPersonal) new PersonalDetails().set("check", namesFrom(body), optionalString(body, "gender"), optionalString(body, "email"), addressFrom(body),
                    optionalString(body, "contactNumber"), optionalString(body, "birthdate"), clockService.now().toLocalDate(), "", "");
                String department = requiredString(body, "department");
                String position = requiredString(body, "position");
                if (!hrRecords.hasDepartment(department)) throw new IllegalArgumentException("Choose a department from the HR list.");
                if (!hrRecords.hasPosition(position)) throw new IllegalArgumentException("Choose a position from the HR list.");
                String supervisorId = requiredString(body, "supervisorId").trim();
                if (AccessPolicy.roleOf(supervisorId) != AccessPolicy.Role.SUPERVISOR) throw new IllegalArgumentException("Choose a supervisor (an account with the Supervisor role).");
                LocalDate start;
                try {
                    start = LocalDate.parse(requiredString(body, "startDate").trim());
                } catch (DateTimeParseException ex) {
                    throw new IllegalArgumentException("Start date must use YYYY-MM-DD.");
                }
                if (start.getYear() != 2026) throw new IllegalArgumentException("Start date must be in 2026 (the loaded payroll year).");
                long basic = StatutoryCalculator2026.parsePesoCents(requiredString(body, "monthlyBasic"), "Monthly basic");
                long[] allowances = {
                    StatutoryCalculator2026.parsePesoCents(optionalString(body, "riceAllowance").isEmpty() ? "0" : optionalString(body, "riceAllowance"), "Rice allowance"),
                    StatutoryCalculator2026.parsePesoCents(optionalString(body, "phoneAllowance").isEmpty() ? "0" : optionalString(body, "phoneAllowance"), "Phone allowance"),
                    StatutoryCalculator2026.parsePesoCents(optionalString(body, "clothingAllowance").isEmpty() ? "0" : optionalString(body, "clothingAllowance"), "Clothing allowance")};
                String payMethod = requiredString(body, "payMethod");
                if (!HrRecords.PAY_METHODS.contains(payMethod)) throw new IllegalArgumentException("Pay method must be Direct deposit or Check.");
                Employee hire = localEmployeeService.createEmployee(requiredString(body, "name"), department, position,
                    requiredString(body, "location"), "Active");
                AccessPolicy.assignSupervisor(hire.getId(), supervisorId);
                localScheduleStore.assign(hire.getId());
                salaryHistory.set(hire.getId(), java.time.YearMonth.from(start), java.math.BigDecimal.valueOf(basic, 2));
                HrRecords.Employment record = hrRecords.onboard(hire.getId(), start, allowances, payMethod, actor.employeeId);
                if (withPersonal) personalDetails.set(hire.getId(), namesFrom(body), optionalString(body, "gender"), optionalString(body, "email"), addressFrom(body),
                    optionalString(body, "contactNumber"), optionalString(body, "birthdate"),
                    clockService.now().toLocalDate(), actor.employeeId, clockService.now().toString());
                sendJson(exchange, 201, "{\"employee\":" + employeeToJson(hire) + ",\"record\":" + employmentToJson(record) + "}");
                return;
            }
            String[] parts = path.split("/");
            if (parts.length == 6 && "employment".equals(parts[3])) {
                String employeeId = parts[4];
                Employee person = localEmployeeService.findEmployee(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
                if ("offboard".equals(parts[5])) {
                    guard(() -> AccessPolicy.requireHrManagement(actor, employeeId));
                    requireOnlyKeys(body, "lastDay", "reason");
                    LocalDate lastDay;
                    try {
                        lastDay = LocalDate.parse(requiredString(body, "lastDay").trim());
                    } catch (DateTimeParseException ex) {
                        throw new IllegalArgumentException("Last day must use YYYY-MM-DD.");
                    }
                    if (lastDay.getYear() != 2026) throw new IllegalArgumentException("Last day must be in 2026 (the loaded payroll year).");
                    HrRecords.Employment record = hrRecords.offboard(employeeId, lastDay, requiredString(body, "reason"), actor.employeeId);
                    localEmployeeService.updateEmployee(employeeId, person.getName(), person.getDepartment(), person.getRole(), person.getLocation(), "Separated");
                    logChange(employeeId, "Status", person.getStatus(), "Separated", actor.employeeId, "Offboarding");
                    logChange(employeeId, "Last day", "", lastDay.toString(), actor.employeeId, "Offboarding");
                    sendJson(exchange, 200, "{\"record\":" + employmentToJson(record) + "}");
                    return;
                }
                if ("tasks".equals(parts[5])) {
                    guard(() -> AccessPolicy.requireHrManagement(actor, null));
                    requireOnlyKeys(body, "task");
                    sendJson(exchange, 200, "{\"record\":" + employmentToJson(hrRecords.completeTask(employeeId, requiredString(body, "task"), actor.employeeId)) + "}");
                    return;
                }
            }
            sendError(exchange, 404, "HR endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        }
    }

    private JsonObject governmentToJson(String employeeId, boolean masked) {
        GovernmentDetails.Numbers record = governmentDetails.get(employeeId);
        JsonObject json = new JsonObject();
        json.addProperty("employeeId", employeeId);
        json.addProperty("employeeName", nameOf(employeeId));
        JsonObject fields = new JsonObject();
        for (String[] spec : GovernmentDetails.FIELDS) fields.addProperty(spec[0], GovernmentDetails.display(spec[0], record.digits.get(spec[0]), masked));
        json.add("fields", fields);
        json.addProperty("masked", masked);
        json.addProperty("updatedBy", record.updatedBy);
        json.addProperty("updatedAt", record.updatedAt);
        return json;
    }

    private JsonObject governmentRequestToJson(GovernmentDetails.Request request, boolean masked) {
        JsonObject json = new JsonObject();
        json.addProperty("id", request.id);
        json.addProperty("kind", request.kind);
        json.addProperty("employeeId", request.employeeId);
        json.addProperty("employeeName", nameOf(request.employeeId));
        JsonObject proposed = new JsonObject();
        for (Map.Entry<String, String> entry : request.proposed.entrySet()) {
            proposed.addProperty(entry.getKey(), GovernmentDetails.display(entry.getKey(), entry.getValue(), masked));
        }
        json.add("proposed", proposed);
        json.addProperty("reason", request.reason);
        json.addProperty("requestedBy", request.requestedBy);
        json.addProperty("requestedAt", request.requestedAt);
        json.addProperty("status", request.status);
        json.addProperty("reviewerId", request.reviewerId);
        json.addProperty("reviewedAt", request.reviewedAt);
        json.addProperty("accessDate", request.accessDate == null ? "" : request.accessDate.toString());
        return json;
    }

    private static Map<String, String> governmentFields(JsonObject body) {
        Map<String, String> fields = new java.util.LinkedHashMap<>();
        for (String[] spec : GovernmentDetails.FIELDS) {
            if (body.has(spec[0])) fields.put(spec[0], optionalString(body, spec[0]));
        }
        return fields;
    }

    /** Government numbers (SSS, PhilHealth, Pag-IBIG, TIN): masked for HR/Payroll; changes and full view need the System Admin. */
    private void handleGovernment(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Government details are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireGovernmentRead(actor));
            LocalDate today = clockService.now().toLocalDate();
            if ("GET".equals(method) && "/api/government".equals(path)) {
                JsonArray records = new JsonArray();
                for (Employee employee : localEmployeeService.listEmployees()) records.add(governmentToJson(employee.getId(), true));
                sendJson(exchange, 200, "{\"records\":" + records + ",\"syntheticOnly\":true}");
                return;
            }
            if ("GET".equals(method) && "/api/government/requests".equals(path)) {
                JsonArray items = new JsonArray();
                for (GovernmentDetails.Request request : governmentDetails.requests()) {
                    if (actor.role == AccessPolicy.Role.ADMIN || request.requestedBy.equals(actor.employeeId)) {
                        items.add(governmentRequestToJson(request, actor.role != AccessPolicy.Role.ADMIN));
                    }
                }
                sendJson(exchange, 200, "{\"requests\":" + items + "}");
                return;
            }
            if ("GET".equals(method) && "/api/government/audit".equals(path)) {
                guard(() -> AccessPolicy.requireGovernmentAdmin(actor, null));
                JsonArray items = new JsonArray();
                for (GovernmentDetails.AuditEntry entry : governmentDetails.audit()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("at", entry.at);
                    item.addProperty("actorId", entry.actorId);
                    item.addProperty("employeeId", entry.employeeId);
                    item.addProperty("action", entry.action);
                    item.addProperty("detail", entry.detail);
                    items.add(item);
                }
                sendJson(exchange, 200, "{\"audit\":" + items + "}");
                return;
            }
            String[] parts = path.split("/");
            if (parts.length == 6 && "requests".equals(parts[3]) && "POST".equals(method)) {
                GovernmentDetails.Request current = governmentDetails.find(parts[4]);
                guard(() -> AccessPolicy.requireGovernmentAdmin(actor, current.employeeId));
                readJsonBody(exchange);
                sendJson(exchange, 200, "{\"item\":" + governmentRequestToJson(governmentDetails.decide(parts[4], parts[5], actor.employeeId, today), false) + "}");
                return;
            }
            if (parts.length < 4 || !parts[3].startsWith("DEMO-")) {
                sendError(exchange, 404, "Government details endpoint not found");
                return;
            }
            String employeeId = parts[3];
            localEmployeeService.findEmployee(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
            if (parts.length == 4 && "GET".equals(method)) {
                boolean reveal = "true".equals(queryParameter(exchange.getRequestURI().getRawQuery(), "reveal"));
                if (reveal) {
                    boolean admin = actor.role == AccessPolicy.Role.ADMIN;
                    if (!admin && !governmentDetails.hasAccess(actor.employeeId, employeeId, today)) {
                        throw new RequestException(403, "Full numbers need an approved access request for today (ask the System Admin).");
                    }
                    governmentDetails.logFullView(actor.employeeId, employeeId, admin ? "System Admin access" : "Approved access for " + today);
                }
                sendJson(exchange, 200, "{\"record\":" + governmentToJson(employeeId, !reveal) + "}");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            if (parts.length == 4 && "PUT".equals(method)) {
                guard(() -> AccessPolicy.requireGovernmentAdmin(actor, null));
                requireOnlyKeys(body, "sss", "philHealth", "pagIbig", "tin");
                governmentDetails.set(employeeId, governmentFields(body), actor.employeeId);
                sendJson(exchange, 200, "{\"record\":" + governmentToJson(employeeId, false) + "}");
                return;
            }
            if (parts.length == 5 && "change-requests".equals(parts[4]) && "POST".equals(method)) {
                guard(() -> AccessPolicy.requireGovernmentRequest(actor, employeeId));
                requireOnlyKeys(body, "sss", "philHealth", "pagIbig", "tin", "reason");
                Map<String, String> fields = governmentFields(body);
                fields.values().removeIf(String::isEmpty);
                GovernmentDetails.Request request = governmentDetails.proposeChange(employeeId, fields, optionalString(body, "reason"), actor.employeeId);
                sendJson(exchange, 201, "{\"item\":" + governmentRequestToJson(request, true) + "}");
                return;
            }
            if (parts.length == 5 && "access-requests".equals(parts[4]) && "POST".equals(method)) {
                guard(() -> AccessPolicy.requireGovernmentRequest(actor, employeeId));
                requireOnlyKeys(body, "reason");
                sendJson(exchange, 201, "{\"item\":" + governmentRequestToJson(governmentDetails.requestAccess(employeeId, optionalString(body, "reason"), actor.employeeId), true) + "}");
                return;
            }
            sendError(exchange, 404, "Government details endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        }
    }

    /** "4.5" percent -> 450 basis points (up to two decimals). */
    private static long percentBp(JsonObject body, String field) {
        String text = requiredString(body, field).trim();
        if (!text.matches("\\d{1,3}(\\.\\d{1,2})?")) throw new IllegalArgumentException(field + " must be a percent such as 5 or 4.5.");
        return StatutoryCalculator2026.parsePesoCents(text, field);
    }

    private static long pesos(JsonObject body, String field) {
        return StatutoryCalculator2026.parsePesoCents(requiredString(body, field), field);
    }

    private static String percentText(long bp) { return java.math.BigDecimal.valueOf(bp, 2).stripTrailingZeros().toPlainString(); }
    private static String pesoText(long cents) { return java.math.BigDecimal.valueOf(cents, 2).toPlainString(); }

    private static JsonObject ratesToJson(StatutoryRates rates) {
        JsonObject json = new JsonObject();
        json.addProperty("id", rates.id);
        json.addProperty("effectiveMonth", rates.effectiveMonth.toString());
        json.addProperty("note", rates.note);
        json.addProperty("createdBy", rates.createdBy);
        json.addProperty("createdAt", rates.createdAt);
        JsonObject sss = new JsonObject();
        sss.addProperty("employeePercent", percentText(rates.sssEmployeeBp));
        sss.addProperty("employerPercent", percentText(rates.sssEmployerBp));
        sss.addProperty("minMsc", pesoText(rates.sssMinMsc));
        sss.addProperty("maxMsc", pesoText(rates.sssMaxMsc));
        JsonObject philHealth = new JsonObject();
        philHealth.addProperty("premiumPercent", percentText(rates.philHealthPremiumBp));
        philHealth.addProperty("floor", pesoText(rates.philHealthFloor));
        philHealth.addProperty("ceiling", pesoText(rates.philHealthCeiling));
        JsonObject pagIbig = new JsonObject();
        pagIbig.addProperty("lowBand", pesoText(rates.pagIbigLowBand));
        pagIbig.addProperty("lowPercent", percentText(rates.pagIbigLowBp));
        pagIbig.addProperty("highPercent", percentText(rates.pagIbigHighBp));
        pagIbig.addProperty("employerPercent", percentText(rates.pagIbigEmployerBp));
        pagIbig.addProperty("cap", pesoText(rates.pagIbigCap));
        JsonArray bir = new JsonArray();
        for (long[] row : rates.birAnnual) {
            JsonObject item = new JsonObject();
            item.addProperty("over", pesoText(row[0]));
            item.addProperty("baseTax", pesoText(row[1]));
            item.addProperty("ratePercent", percentText(row[2]));
            bir.add(item);
        }
        json.add("sss", sss);
        json.add("philHealth", philHealth);
        json.add("pagIbig", pagIbig);
        json.add("bir", bir);
        return json;
    }

    private JsonObject accountRow(Employee employee) {
        JsonObject item = new JsonObject();
        item.addProperty("employeeId", employee.getId());
        item.addProperty("name", employee.getName());
        item.addProperty("position", employee.getRole());
        item.addProperty("status", employee.getStatus());
        item.addProperty("role", AccessPolicy.roleOf(employee.getId()).name());
        item.addProperty("roleLabel", AccessPolicy.roleOf(employee.getId()).label);
        item.addProperty("supervisorId", AccessPolicy.supervisorOf(employee.getId()).orElse(""));
        return item;
    }

    /** System Admin configuration: user roles and reporting lines; statutory rates and tax table versions. Local demo. */
    private void handleAdmin(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "System configuration is local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireAdmin(actor));
            if ("GET".equals(method) && "/api/admin/roles".equals(path)) {
                JsonArray items = new JsonArray();
                for (Employee employee : localEmployeeService.listEmployees()) items.add(accountRow(employee));
                JsonArray log = new JsonArray();
                synchronized (roleChanges) { roleChanges.forEach(log::add); }
                sendJson(exchange, 200, "{\"accounts\":" + items + ",\"changes\":" + log + "}");
                return;
            }
            if ("GET".equals(method) && "/api/admin/rates".equals(path)) {
                JsonArray versions = new JsonArray();
                for (StatutoryRates version : statutoryRates.all()) versions.add(ratesToJson(version));
                String month = queryParameter(exchange.getRequestURI().getRawQuery(), "month");
                JsonObject response = new JsonObject();
                response.add("versions", versions);
                response.addProperty("inForce", statutoryRates.ratesFor(month == null || month.isEmpty()
                    ? java.time.YearMonth.from(clockService.now()) : parseMonth(month)).id);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && "/api/admin/audit".equals(path)) {
                String query = exchange.getRequestURI().getRawQuery();
                String limitText = queryParameter(query, "limit");
                int limit = limitText == null || limitText.isEmpty() ? 200 : Math.max(1, Math.min(1000, Integer.parseInt(limitText)));
                JsonObject response = new JsonObject();
                response.add("entries", new com.google.gson.Gson().toJsonTree(auditLog.search(nullToEmpty(queryParameter(query, "actor")).trim(),
                    nullToEmpty(queryParameter(query, "path")).trim(), "true".equals(queryParameter(query, "failures")), limit)));
                response.addProperty("kept", auditLog.size());
                response.add("archives", new com.google.gson.Gson().toJsonTree(auditLog.archives()));
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && "/api/admin/backups".equals(path)) {
                JsonObject response = new JsonObject();
                response.addProperty("persistence", dataFile == null ? "memory" : "file");
                response.add("backups", new com.google.gson.Gson().toJsonTree(dataFile == null ? java.util.Collections.emptyList() : dataFile.listBackups()));
                response.addProperty("folder", dataFile == null ? "" : dataFile.backupDirectory().toString());
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && "/api/admin/security".equals(path)) {
                JsonObject response = new JsonObject();
                response.add("settings", new com.google.gson.Gson().toJsonTree(accountSecurity.settings()));
                JsonArray accounts = new JsonArray();
                java.time.LocalDateTime now = clockService.now();
                for (Employee employee : localEmployeeService.listEmployees()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("employeeId", employee.getId());
                    item.addProperty("name", employee.getName());
                    item.addProperty("defaultPassword", accountSecurity.usesDefaultPassword(employee.getId()));
                    item.addProperty("passwordChangedAt", accountSecurity.passwordChangedAt(employee.getId()).orElse(""));
                    item.addProperty("failedAttempts", accountSecurity.failedAttempts(employee.getId()));
                    item.addProperty("lockedUntil", accountSecurity.lockedUntil(employee.getId(), now).map(Object::toString).orElse(""));
                    accounts.add(item);
                }
                response.add("accounts", accounts);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && "/api/admin/payroll-policy".equals(path)) {
                JsonObject response = new JsonObject();
                response.add("policy", new com.google.gson.Gson().toJsonTree(PayrollPolicy.values()));
                response.addProperty("mentorDefaults", PayrollPolicy.isDefault());
                sendJson(exchange, 200, response.toString());
                return;
            }
            if (!"POST".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            String stamp = clockService.now().toString();
            if ("/api/admin/backups".equals(path)) {
                requireOnlyKeys(body);
                if (dataFile == null) throw new IllegalStateException("This server keeps data in memory only (LOCAL_DATA_FILE is empty), so there is nothing to back up.");
                saveDataFile();
                String name = dataFile.backupNow("manual");
                exchange.setAttribute("auditTarget", name);
                sendJson(exchange, 201, "{\"backup\":\"" + escapeJson(name) + "\"}");
                return;
            }
            if ("/api/admin/backups/restore".equals(path)) {
                requireOnlyKeys(body, "name", "confirm");
                if (!"RESTORE".equals(optionalString(body, "confirm"))) throw new IllegalArgumentException("Type RESTORE to confirm.");
                if (dataFile == null) throw new IllegalStateException("This server keeps data in memory only, so there are no backups.");
                String name = requiredString(body, "name").trim();
                exchange.setAttribute("auditTarget", name);
                JsonObject stores = dataFile.loadBackup(name);
                saveDataFile();
                String safety = dataFile.backupNow("before-restore");
                importState(stores);
                sendJson(exchange, 200, "{\"restored\":\"" + escapeJson(name) + "\",\"safetyBackup\":\"" + escapeJson(safety) + "\"}");
                return;
            }
            if ("/api/admin/security".equals(path)) {
                requireOnlyKeys(body, "minPasswordLength", "maxFailedAttempts", "lockoutMinutes", "codeMinutes", "sessionHours");
                accountSecurity.updateSettings(intField(body, "minPasswordLength"), intField(body, "maxFailedAttempts"), intField(body, "lockoutMinutes"),
                    intField(body, "codeMinutes"), intField(body, "sessionHours"), actor.employeeId, stamp);
                sendJson(exchange, 200, "{\"settings\":" + new com.google.gson.Gson().toJson(accountSecurity.settings()) + "}");
                return;
            }
            if (path.matches("/api/admin/security/[A-Za-z0-9-]+/(unlock|reset-password)")) {
                String targetId = path.split("/")[4];
                exchange.setAttribute("auditTarget", targetId);
                localEmployeeService.findEmployee(targetId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
                requireOnlyKeys(body);
                if (path.endsWith("/unlock")) {
                    accountSecurity.unlock(targetId);
                    sendJson(exchange, 200, "{\"unlocked\":\"" + targetId + "\"}");
                } else {
                    if (targetId.equals(actor.employeeId)) throw new IllegalArgumentException("Change your own password from your account instead.");
                    accountSecurity.resetPassword(targetId);
                    twoFactor.signOutEverywhere(targetId, null);
                    sendJson(exchange, 200, "{\"reset\":\"" + targetId + "\",\"signedOut\":true}");
                }
                return;
            }
            if ("/api/admin/payroll-policy".equals(path)) {
                requireOnlyKeys(body, "graceMinutes", "hoursPerMonth", "dailyHours", "nightDifferentialPercent");
                PayrollPolicy.update(intField(body, "graceMinutes"), intField(body, "hoursPerMonth"), intField(body, "dailyHours"),
                    intField(body, "nightDifferentialPercent"), actor.employeeId, stamp);
                JsonObject response = new JsonObject();
                response.add("policy", new com.google.gson.Gson().toJsonTree(PayrollPolicy.values()));
                response.addProperty("mentorDefaults", PayrollPolicy.isDefault());
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("/api/admin/reset".equals(path)) {
                requireOnlyKeys(body, "confirm");
                if (!"RESET".equals(optionalString(body, "confirm"))) throw new IllegalArgumentException("Type RESET to confirm.");
                importState(seedState);
                sendJson(exchange, 200, "{\"reset\":true,\"persistence\":\"" + (dataFile == null ? "memory" : "file") + "\"}");
                return;
            }
            if (path.startsWith("/api/admin/roles/")) {
                String targetId = path.substring("/api/admin/roles/".length());
                Employee target = localEmployeeService.findEmployee(targetId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
                requireOnlyKeys(body, "role", "supervisorId");
                String before = AccessPolicy.roleOf(targetId).label + " / " + AccessPolicy.supervisorOf(targetId).orElse("no supervisor");
                java.util.List<String> active = new java.util.ArrayList<>();
                for (Employee employee : localEmployeeService.listEmployees()) {
                    if (!hrRecords.isSeparated(employee.getId(), clockService.now().toLocalDate())) active.add(employee.getId());
                }
                try {
                    if (body.has("role")) {
                        AccessPolicy.Role role;
                        try {
                            role = AccessPolicy.Role.valueOf(requiredString(body, "role"));
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException("Role must be EMPLOYEE, SUPERVISOR, PAYROLL, HR or ADMIN.");
                        }
                        AccessPolicy.changeRole(actor, targetId, role, active);
                    }
                    if (body.has("supervisorId")) AccessPolicy.changeSupervisor(actor, targetId, optionalString(body, "supervisorId").trim());
                } catch (AccessPolicy.AccessDenied ex) {
                    throw new RequestException(403, ex.getMessage());
                }
                JsonObject change = new JsonObject();
                change.addProperty("at", java.time.Instant.now().toString());
                change.addProperty("by", actor.employeeId);
                change.addProperty("employeeId", targetId);
                change.addProperty("before", before);
                change.addProperty("after", AccessPolicy.roleOf(targetId).label + " / " + AccessPolicy.supervisorOf(targetId).orElse("no supervisor"));
                synchronized (roleChanges) { roleChanges.add(0, change); }
                logChange(targetId, "Role / supervisor", before, change.get("after").getAsString(), actor.employeeId, "System config");
                sendJson(exchange, 200, "{\"account\":" + accountRow(target) + ",\"change\":" + change + "}");
                return;
            }
            if ("/api/admin/rates".equals(path)) {
                StatutoryRates version = ratesFromBody(body, statutoryRates.nextId(), actor.employeeId, optionalString(body, "note"));
                sendJson(exchange, 201, "{\"version\":" + ratesToJson(statutoryRates.add(version)) + "}");
                return;
            }
            sendError(exchange, 404, "Admin endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        } catch (ClassCastException ex) {
            sendError(exchange, 400, "sss, philHealth and pagIbig must be objects.");
        } catch (IOException ex) {
            sendError(exchange, 500, "The backup could not be read or written: " + ex.getMessage());
        }
    }

    /**
     * Branch pages (requirement 1, "redirect to the branch page"): counts for the signed-in person's branch (their
     * directory location), or every branch for Payroll Staff, HR and the System Admin. Counts only, no names.
     */
    private void handleBranches(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Branch pages are local-demo only.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            String mine = actor.explicit ? localEmployeeService.findEmployee(actor.employeeId).map(Employee::getLocation).orElse("") : "";
            boolean all = actor.role == AccessPolicy.Role.ADMIN || actor.role == AccessPolicy.Role.HR || actor.role == AccessPolicy.Role.PAYROLL;
            LocalDate today = clockService.now().toLocalDate();
            List<DemoLeaveRequest> leaves = localWorkflowStore.listLeaveRequests();
            Map<String, int[]> counts = new java.util.TreeMap<>(); // headcount, active, on leave today, clocked in, onsite
            for (Employee employee : localEmployeeService.listEmployees()) {
                String id = employee.getId();
                if (hrRecords.isSeparated(id, today)) continue;
                if (!all && !employee.getLocation().equals(mine)) continue;
                int[] row = counts.computeIfAbsent(employee.getLocation(), key -> new int[5]);
                row[0]++;
                if ("Active".equals(employee.getStatus())) row[1]++;
                boolean onLeave = leaves.stream().anyMatch(leave -> leave.getEmployeeId().equals(id) && "Approved".equals(leave.getStatus())
                    && !today.isBefore(LocalDate.parse(leave.getStartDate())) && !today.isAfter(LocalDate.parse(leave.getEndDate())));
                if (onLeave) row[2]++;
                if (clockService.openShift(id).isPresent()) row[3]++;
                if (clockService.onsiteAt(id, today).isPresent()) row[4]++;
            }
            JsonArray branches = new JsonArray();
            for (Map.Entry<String, int[]> entry : counts.entrySet()) {
                JsonObject item = new JsonObject();
                item.addProperty("name", entry.getKey());
                item.addProperty("headcount", entry.getValue()[0]);
                item.addProperty("active", entry.getValue()[1]);
                item.addProperty("onLeaveToday", entry.getValue()[2]);
                item.addProperty("clockedInNow", entry.getValue()[3]);
                item.addProperty("onsiteToday", entry.getValue()[4]);
                branches.add(item);
            }
            JsonObject response = new JsonObject();
            response.addProperty("mine", mine);
            response.addProperty("today", today.toString());
            response.add("branches", branches);
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        }
    }

    private JsonObject correctionToJson(PayslipCorrections.Request request) {
        JsonObject json = new JsonObject();
        json.addProperty("id", request.id);
        json.addProperty("payslipId", request.payslipId);
        json.addProperty("employeeId", request.employeeId);
        json.addProperty("employeeName", nameOf(request.employeeId));
        json.addProperty("kind", request.kind);
        json.addProperty("targetMonth", request.targetMonth == null ? "" : request.targetMonth.toString());
        json.addProperty("targetCutoff", request.targetCutoff);
        json.addProperty("reason", request.reason);
        json.addProperty("requestedBy", request.requestedBy);
        json.addProperty("requestedAt", request.requestedAt);
        json.addProperty("status", request.status);
        json.addProperty("reviewerId", request.reviewerId);
        json.addProperty("reviewedAt", request.reviewedAt);
        json.addProperty("outcome", request.outcome);
        json.addProperty("differenceCents", request.differenceCents);
        return json;
    }

    /** Payslip corrections: VOID (unreleased) or ADJUST (released), filed by Payroll Staff, approved by the System Admin. */
    private void handlePayslipCorrections(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Payslip corrections are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(method) && "/api/payroll/corrections".equals(path)) {
                guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.PAYROLL));
                JsonArray items = new JsonArray();
                for (PayslipCorrections.Request request : payslipCorrections.list()) items.add(correctionToJson(request));
                sendJson(exchange, 200, "{\"corrections\":" + items + "}");
                return;
            }
            if (!"POST".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            if ("/api/payroll/corrections".equals(path)) {
                requireOnlyKeys(body, "payslipId", "reason", "targetMonth", "targetCutoff");
                String payslipId = requiredString(body, "payslipId").trim();
                if (!savedPayslips.isActive(payslipId)) throw new NoSuchElementException("No active finalized payslip with that number.");
                JsonObject slip = JsonParser.parseString(savedPayslips.find(payslipId).get()).getAsJsonObject();
                String owner = slip.getAsJsonObject("employee").get("id").getAsString();
                guard(() -> AccessPolicy.requirePayslipCorrectionRequest(actor, owner));
                String targetMonth = optionalString(body, "targetMonth").trim();
                String targetCutoff = optionalString(body, "targetCutoff").trim();
                PayslipCorrections.Request request = payslipCorrections.create(payslipId, owner, java.time.YearMonth.parse(slip.get("month").getAsString()),
                    "1-15".equals(slip.getAsJsonObject("period").get("label").getAsString()) ? 1 : 2, disbursements.isReleased(payslipId),
                    targetMonth.isEmpty() ? null : parseMonth(targetMonth), targetCutoff.isEmpty() ? 0 : Integer.parseInt(targetCutoff),
                    optionalString(body, "reason"), actor.employeeId);
                sendJson(exchange, 201, "{\"item\":" + correctionToJson(request) + "}");
                return;
            }
            String[] parts = path.split("/");
            if (parts.length == 6 && parts[4].startsWith("PCR-") && ("approve".equals(parts[5]) || "reject".equals(parts[5]))) {
                PayslipCorrections.Request current = payslipCorrections.find(parts[4]);
                guard(() -> AccessPolicy.requirePayslipCorrectionReview(actor, current.employeeId));
                if ("reject".equals(parts[5])) {
                    sendJson(exchange, 200, "{\"item\":" + correctionToJson(payslipCorrections.decide(current.id, "Rejected", actor.employeeId, "No change made.", 0)) + "}");
                    return;
                }
                String outcome;
                long difference = 0;
                if ("VOID".equals(current.kind)) {
                    if (disbursements.isReleased(current.payslipId)) {
                        throw new IllegalStateException("This payslip was released after the request; reject it and file an adjustment instead.");
                    }
                    JsonObject slip = JsonParser.parseString(savedPayslips.find(current.payslipId).orElseThrow(() ->
                        new NoSuchElementException("The payslip is no longer active."))).getAsJsonObject();
                    String archiveId = current.payslipId + "-void" + (savedPayslips.voidCount(current.payslipId) + 1);
                    slip.addProperty("voided", true);
                    slip.addProperty("archiveId", archiveId);
                    slip.addProperty("voidedAt", java.time.Instant.now().toString());
                    slip.addProperty("voidedBy", actor.employeeId);
                    slip.addProperty("voidReason", current.reason);
                    slip.addProperty("voidRequest", current.id);
                    savedPayslips.voidSnapshot(current.payslipId, slip.toString());
                    outcome = "Voided as " + archiveId + "; finalize the cutoff again to issue version " + (savedPayslips.voidCount(current.payslipId) + 1) + ".";
                } else {
                    String target = "PS-" + current.employeeId + "-" + current.targetMonth + "-" + current.targetCutoff;
                    if (savedPayslips.isActive(target)) throw new IllegalStateException("The target cutoff " + target + " is already finalized; choose a later one.");
                    JsonObject slip = JsonParser.parseString(savedPayslips.find(current.payslipId).get()).getAsJsonObject();
                    // Recalculate the corrected cutoff from today's data and compare gross pay with what was paid.
                    MonthPayroll recalculated = monthPayrollFor(exchange, current.employeeId, current.month.toString());
                    difference = recalculated.grossCents(current.cutoff - 1) - slip.get("grossPayCents").getAsLong();
                    String note = "Correction of " + current.payslipId;
                    if (difference > 0) {
                        payrollAdjustments.addEarning(current.employeeId, current.targetMonth, current.targetCutoff, "Adjustment", note, difference, actor.employeeId);
                        outcome = "Underpaid by ₱" + java.math.BigDecimal.valueOf(difference, 2).toPlainString() + " gross: added as a taxable Adjustment on "
                            + current.targetMonth + " cutoff " + current.targetCutoff + ".";
                    } else if (difference < 0) {
                        payrollAdjustments.addRecovery(current.employeeId, current.targetMonth, current.targetCutoff, -difference, note, actor.employeeId);
                        outcome = "Overpaid by ₱" + java.math.BigDecimal.valueOf(-difference, 2).toPlainString() + " gross: Overpayment recovery on "
                            + current.targetMonth + " cutoff " + current.targetCutoff + ".";
                    } else {
                        outcome = "No difference in gross pay; nothing to adjust.";
                    }
                }
                sendJson(exchange, 200, "{\"item\":" + correctionToJson(payslipCorrections.decide(current.id, "Approved", actor.employeeId, outcome, difference)) + "}");
                return;
            }
            sendError(exchange, 404, "Payslip correction endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (NumberFormatException | DateTimeParseException ex) {
            sendError(exchange, 400, "Target month must use YYYY-MM and cutoff must be 1 or 2.");
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        }
    }

    /** Earnings (bonuses) and voluntary deductions entered by Payroll Staff. Local demo only. */
    private void handlePayrollAdjustments(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Payroll processing is local-demo only; Firebase rules have no payroll collection.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(method) && "/api/payroll/adjustments".equals(path)) {
                String query = exchange.getRequestURI().getRawQuery();
                String employeeFilter = queryParameter(query, "employeeId");
                JsonArray earnings = new JsonArray();
                for (PayrollAdjustments.Earning item : payrollAdjustments.allEarnings()) {
                    if (!AccessPolicy.canRead(actor, AccessPolicy.Area.EARNINGS, item.employeeId)) continue;
                    if (employeeFilter != null && !employeeFilter.isEmpty() && !employeeFilter.equals(item.employeeId)) continue;
                    earnings.add(JsonParser.parseString(earningToJson(item)));
                }
                JsonArray deductions = new JsonArray();
                for (PayrollAdjustments.Deduction item : payrollAdjustments.allDeductions()) {
                    if (!AccessPolicy.canRead(actor, AccessPolicy.Area.EARNINGS, item.employeeId)) continue;
                    if (employeeFilter != null && !employeeFilter.isEmpty() && !employeeFilter.equals(item.employeeId)) continue;
                    deductions.add(JsonParser.parseString(deductionToJson(item)));
                }
                JsonArray allowances = new JsonArray();
                for (Employee employee : localEmployeeService.listEmployees()) {
                    if (!AccessPolicy.canRead(actor, AccessPolicy.Area.EARNINGS, employee.getId())) continue;
                    if (employeeFilter != null && !employeeFilter.isEmpty() && !employeeFilter.equals(employee.getId())) continue;
                    long[] monthly = hrRecords.monthlyAllowances(employee.getId());
                    JsonObject item = new JsonObject();
                    item.addProperty("employeeId", employee.getId());
                    item.addProperty("riceCents", monthly[MonthPayroll.RICE]);
                    item.addProperty("phoneCents", monthly[MonthPayroll.PHONE]);
                    item.addProperty("clothingCents", monthly[MonthPayroll.CLOTHING]);
                    item.addProperty("payMethod", hrRecords.payMethod(employee.getId()));
                    allowances.add(item);
                }
                JsonObject response = new JsonObject();
                response.add("earnings", earnings);
                response.add("deductions", deductions);
                response.add("allowances", allowances);
                response.addProperty("earningTypes", String.join("|", PayrollAdjustments.EARNING_TYPES));
                response.addProperty("allowanceTypes", String.join("|", PayrollAdjustments.ALLOWANCE_TYPES));
                response.addProperty("deductionTypes", String.join("|", PayrollAdjustments.DEDUCTION_TYPES));
                sendJson(exchange, 200, response.toString());
                return;
            }
            if (!"POST".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            if ("/api/payroll/adjustments/earnings".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "month", "cutoff", "type", "description", "amount");
                String employeeId = payrollTarget(actor, requiredString(body, "employeeId"));
                java.time.YearMonth month = parseMonth(requiredString(body, "month"));
                String cutoffText = requiredString(body, "cutoff");
                if (!"1".equals(cutoffText) && !"2".equals(cutoffText)) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
                if (savedPayslips.find("PS-" + employeeId + "-" + month + "-" + cutoffText).isPresent()) {
                    throw new IllegalStateException("That payslip is already finalized; add the earning to a later cutoff.");
                }
                PayrollAdjustments.Earning item = payrollAdjustments.addEarning(employeeId, month, Integer.parseInt(cutoffText),
                    requiredString(body, "type"), optionalString(body, "description"),
                    StatutoryCalculator2026.parsePesoCents(requiredString(body, "amount"), "Amount"), actor.employeeId);
                sendJson(exchange, 201, "{\"item\":" + earningToJson(item) + "}");
                return;
            }
            if ("/api/payroll/adjustments/deductions".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "type", "description", "amountPerCutoff", "fromMonth", "toMonth");
                String employeeId = payrollTarget(actor, requiredString(body, "employeeId"));
                String toText = optionalString(body, "toMonth").trim();
                PayrollAdjustments.Deduction item = payrollAdjustments.addDeduction(employeeId, requiredString(body, "type"),
                    optionalString(body, "description"), StatutoryCalculator2026.parsePesoCents(requiredString(body, "amountPerCutoff"), "Amount per cutoff"),
                    parseMonth(requiredString(body, "fromMonth")), toText.isEmpty() ? null : parseMonth(toText), actor.employeeId);
                sendJson(exchange, 201, "{\"item\":" + deductionToJson(item) + "}");
                return;
            }
            if ("/api/payroll/adjustments/group".equals(path)) {
                sendJson(exchange, 201, addGroupAdjustment(actor, readJsonBody(exchange)).toString());
                return;
            }
            if (path.matches("/api/payroll/adjustments/[A-Z]{2}-[A-F0-9]{12}/remove")) {
                String id = path.split("/")[4];
                String owner = payrollAdjustments.ownerOf(id);
                guard(() -> AccessPolicy.requireEarningsEdit(actor, owner));
                PayrollAdjustments.Earning earning = payrollAdjustments.earning(id);
                if (earning != null && savedPayslips.find("PS-" + owner + "-" + earning.month + "-" + earning.cutoff).isPresent()) {
                    throw new IllegalStateException("That payslip is already finalized; the earning stays on it.");
                }
                payrollAdjustments.remove(id);
                sendJson(exchange, 200, "{\"removed\":\"" + id + "\"}");
                return;
            }
            sendError(exchange, 404, "Payroll adjustment endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage() == null ? "Not found" : ex.getMessage());
        }
    }

    /** The employee a payroll entry is for: must exist, have a pay profile, and not be the person entering it. */
    /**
     * Requirement 9: one earning or deduction for a whole group: ALL, TEAM:<supervisorId>, BRANCH:<location> or DEPARTMENT:<name>.
     * It becomes one ordinary item per person (each can be removed on its own). People it cannot apply to are skipped
     * with the reason: the person filing (nobody changes their own pay), no pay profile, separated, or an already finalized payslip.
     */
    private JsonObject addGroupAdjustment(AccessPolicy.Actor actor, JsonObject body) {
        requireOnlyKeys(body, "group", "kind", "month", "cutoff", "type", "description", "amount", "amountPerCutoff", "fromMonth", "toMonth");
        guard(() -> AccessPolicy.requirePayrollProcessing(actor, null));
        String group = requiredString(body, "group").trim();
        String kind = requiredString(body, "kind").trim();
        if (!"earning".equals(kind) && !"deduction".equals(kind)) throw new IllegalArgumentException("kind must be earning or deduction.");
        List<Employee> members = groupMembers(group);
        java.time.YearMonth month = null, fromMonth = null, toMonth = null;
        int cutoff = 0;
        long cents;
        if ("earning".equals(kind)) {
            month = parseMonth(requiredString(body, "month"));
            String cutoffText = requiredString(body, "cutoff");
            if (!"1".equals(cutoffText) && !"2".equals(cutoffText)) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
            cutoff = Integer.parseInt(cutoffText);
            cents = StatutoryCalculator2026.parsePesoCents(requiredString(body, "amount"), "Amount");
        } else {
            fromMonth = parseMonth(requiredString(body, "fromMonth"));
            String toText = optionalString(body, "toMonth").trim();
            toMonth = toText.isEmpty() ? null : parseMonth(toText);
            cents = StatutoryCalculator2026.parsePesoCents(requiredString(body, "amountPerCutoff"), "Amount per cutoff");
        }
        LocalDate today = clockService.now().toLocalDate();
        JsonArray added = new JsonArray(), skipped = new JsonArray();
        for (Employee employee : members) {
            String id = employee.getId();
            String reason = id.equals(actor.employeeId) ? "you cannot change your own pay"
                : !hasPayProfile(id) ? "no pay profile"
                : hrRecords.isSeparated(id, today) ? "separated"
                : "earning".equals(kind) && savedPayslips.find("PS-" + id + "-" + month + "-" + cutoff).isPresent() ? "that payslip is already finalized"
                : null;
            if (reason != null) {
                JsonObject item = new JsonObject();
                item.addProperty("employeeId", id);
                item.addProperty("reason", reason);
                skipped.add(item);
                continue;
            }
            // Every item has the same type and amount, so a bad type fails on the first one, before anything is added.
            added.add(JsonParser.parseString("earning".equals(kind)
                ? earningToJson(payrollAdjustments.addEarning(id, month, cutoff, requiredString(body, "type"), optionalString(body, "description"), cents, actor.employeeId))
                : deductionToJson(payrollAdjustments.addDeduction(id, requiredString(body, "type"), optionalString(body, "description"), cents, fromMonth, toMonth, actor.employeeId))));
        }
        if (added.size() == 0) throw new IllegalStateException("Nobody in that group could get it (" + skipped.size() + " skipped).");
        JsonObject response = new JsonObject();
        response.addProperty("group", group);
        response.add("added", added);
        response.add("skipped", skipped);
        return response;
    }

    /** ALL, TEAM:<supervisorId>, BRANCH:<location>, DEPARTMENT:<name> or EMPLOYEE:<id>; never empty. */
    private List<Employee> groupMembers(String group) {
        int colon = group.indexOf(':');
        String scope = colon < 0 ? group : group.substring(0, colon), value = colon < 0 ? "" : group.substring(colon + 1).trim();
        List<Employee> members = new java.util.ArrayList<>();
        for (Employee employee : localEmployeeService.listEmployees()) {
            boolean match;
            if ("ALL".equals(group)) match = true;
            else if ("TEAM".equals(scope)) match = value.equals(AccessPolicy.supervisorOf(employee.getId()).orElse(""));
            else if ("BRANCH".equals(scope)) match = value.equalsIgnoreCase(employee.getLocation());
            else if ("DEPARTMENT".equals(scope)) match = value.equalsIgnoreCase(employee.getDepartment());
            else if ("EMPLOYEE".equals(scope)) match = value.equals(employee.getId());
            else throw new IllegalArgumentException("group must be ALL, TEAM:<supervisor ID>, BRANCH:<location>, DEPARTMENT:<name> or EMPLOYEE:<id>.");
            if (match) members.add(employee);
        }
        if (members.isEmpty()) throw new IllegalArgumentException("Nobody is in that group.");
        return members;
    }

    private static long[] cutoffFigures(MonthPayroll payroll, int index) {
        return new long[] {payroll.grossCents(index), payroll.allowanceTotalCents(index), payroll.deductionsCents(index), payroll.netCents(index)};
    }

    /**
     * Adjustment forms (design doc, Payroll Supervision): POST /api/payroll/adjustment-forms {scope, month, cutoff, reason,
     * lines:[{component, operation, amount}]} by Payroll Staff or the System Admin -> Pending review; POST
     * /api/payroll/adjustment-forms/{AF-0001}/approve|reject by another Payroll Staff member or the System Admin (not the
     * submitter). Approval applies the lines to each person and keeps their before/after figures. GET lists the forms.
     */
    private void handleAdjustmentForms(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Adjustment forms are local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod()) && "/api/payroll/adjustment-forms".equals(path)) {
                guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.REMITTANCE));
                sendJson(exchange, 200, "{\"forms\":" + new com.google.gson.Gson().toJson(adjustmentForms.list()) + "}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            guard(() -> AccessPolicy.requirePayrollProcessing(actor, null));
            JsonObject body = readJsonBody(exchange);
            String stamp = clockService.now().toString();
            if ("/api/payroll/adjustment-forms".equals(path)) {
                requireOnlyKeys(body, "scope", "month", "cutoff", "reason", "lines");
                String scope = requiredString(body, "scope").trim();
                groupMembers(scope); // the scope must name somebody
                java.time.YearMonth month = parseMonth(requiredString(body, "month"));
                String cutoffText = requiredString(body, "cutoff");
                if (!"1".equals(cutoffText) && !"2".equals(cutoffText)) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
                if (!body.has("lines") || !body.get("lines").isJsonArray()) throw new IllegalArgumentException("lines must be a list.");
                List<AdjustmentForms.Line> lines = new java.util.ArrayList<>();
                for (JsonElement element : body.getAsJsonArray("lines")) {
                    if (!element.isJsonObject()) throw new IllegalArgumentException("Each line needs component, operation and amount.");
                    JsonObject line = element.getAsJsonObject();
                    lines.add(new AdjustmentForms.Line(requiredString(line, "component").trim(), requiredString(line, "operation").trim(),
                        "0".equals(requiredString(line, "amount").trim()) ? 0 : StatutoryCalculator2026.parsePesoCents(requiredString(line, "amount"), "Amount")));
                }
                AdjustmentForms.Form form = adjustmentForms.add(scope, month.toString(), Integer.parseInt(cutoffText), optionalString(body, "reason"), lines, actor.employeeId, stamp);
                exchange.setAttribute("auditTarget", form.id);
                sendJson(exchange, 201, "{\"item\":" + new com.google.gson.Gson().toJson(form) + "}");
                return;
            }
            if (path.matches("/api/payroll/adjustment-forms/AF-[0-9]{4,}/(approve|reject)")) {
                String[] parts = path.split("/");
                AdjustmentForms.Form form = adjustmentForms.find(parts[4]);
                exchange.setAttribute("auditTarget", form.id);
                if (form.submittedBy.equals(actor.employeeId)) throw new RequestException(403, "Nobody approves their own adjustment form.");
                requireOnlyKeys(body);
                if ("reject".equals(parts[5])) {
                    adjustmentForms.decide(form, "Rejected", actor.employeeId, stamp, "No change made.", java.util.Collections.emptyList());
                } else {
                    if (!"Pending review".equals(form.status)) throw new IllegalStateException("This form was already " + form.status.toLowerCase() + ".");
                    List<AdjustmentForms.Result> results = new java.util.ArrayList<>();
                    int applied = 0;
                    int index = form.cutoff - 1;
                    String note = "Form " + form.id + ": " + form.reason;
                    for (Employee employee : groupMembers(form.scope)) {
                        String id = employee.getId();
                        long[] none = new long[4];
                        String skip = id.equals(actor.employeeId) || id.equals(form.submittedBy) ? "their own pay"
                            : !hasPayProfile(id) ? "no pay profile"
                            : hrRecords.isSeparated(id, clockService.now().toLocalDate()) ? "separated"
                            : savedPayslips.find("PS-" + id + "-" + form.month + "-" + form.cutoff).isPresent() ? "that payslip is already finalized" : null;
                        if (skip != null) {
                            results.add(new AdjustmentForms.Result(id, skip, none, none));
                            continue;
                        }
                        MonthPayroll before = monthPayrollFor(exchange, id, form.month, null);
                        java.time.YearMonth month = java.time.YearMonth.parse(form.month);
                        for (AdjustmentForms.Line line : form.lines) {
                            int slot = AdjustmentForms.ALLOWANCES.indexOf(line.component);
                            if (slot >= 0) {
                                String type = PayrollAdjustments.ALLOWANCE_TYPES.get(slot);
                                long current = before.allowanceWithCorrections(index, slot, type);
                                long change = "Add".equals(line.operation) ? line.amountCents
                                    : "Subtract".equals(line.operation) ? -Math.min(line.amountCents, current) : line.amountCents - current;
                                if (change != 0) payrollAdjustments.addAllowanceCorrection(id, month, form.cutoff, type, note, change, actor.employeeId);
                            } else if (AdjustmentForms.TAXABLE.equals(line.component)) {
                                if ("Add".equals(line.operation)) payrollAdjustments.addEarning(id, month, form.cutoff, "Adjustment", note, line.amountCents, actor.employeeId);
                                else payrollAdjustments.addOneCutoffDeduction(id, month, form.cutoff, line.amountCents, note + " (subtracted earning)", actor.employeeId);
                            } else {
                                payrollAdjustments.addOneCutoffDeduction(id, month, form.cutoff, line.amountCents, note, actor.employeeId);
                            }
                        }
                        MonthPayroll after = monthPayrollFor(exchange, id, form.month, null);
                        results.add(new AdjustmentForms.Result(id, null, cutoffFigures(before, index), cutoffFigures(after, index)));
                        applied++;
                    }
                    adjustmentForms.decide(form, "Approved", actor.employeeId, stamp, "Applied to " + applied + " employee(s)"
                        + (applied == results.size() ? "." : "; " + (results.size() - applied) + " skipped."), results);
                }
                notify(form.submittedBy, actor, "Payroll", "Your adjustment form " + form.id + " was " + form.status.toLowerCase() + " by " + nameOf(actor.employeeId) + ". " + form.outcome);
                sendJson(exchange, 200, "{\"item\":" + new com.google.gson.Gson().toJson(form) + "}");
                return;
            }
            sendError(exchange, 404, "Adjustment form endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        }
    }

    private String payrollTarget(AccessPolicy.Actor actor, String employeeIdText) {
        String employeeId = employeeIdText.trim();
        localEmployeeService.findEmployee(employeeId).orElseThrow(() -> new NoSuchElementException("Employee not found."));
        if (!hasPayProfile(employeeId)) throw new IllegalArgumentException("That employee has no pay profile.");
        guard(() -> AccessPolicy.requireEarningsEdit(actor, employeeId));
        return employeeId;
    }

    private static java.time.YearMonth parseMonth(String text) {
        if (text == null || !text.trim().matches("2026-(0[1-9]|1[0-2])")) throw new IllegalArgumentException("Month must be a 2026 month (YYYY-MM).");
        return java.time.YearMonth.parse(text.trim());
    }

    private static String earningToJson(PayrollAdjustments.Earning item) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item.id);
        json.addProperty("employeeId", item.employeeId);
        json.addProperty("month", item.month.toString());
        json.addProperty("cutoff", item.cutoff);
        json.addProperty("type", item.type);
        json.addProperty("description", item.description);
        json.addProperty("amountCents", item.amountCents);
        json.addProperty("taxable", true);
        json.addProperty("createdBy", item.createdBy);
        json.addProperty("createdAt", item.createdAt);
        return json.toString();
    }

    private static String deductionToJson(PayrollAdjustments.Deduction item) {
        JsonObject json = new JsonObject();
        json.addProperty("id", item.id);
        json.addProperty("employeeId", item.employeeId);
        json.addProperty("type", item.type);
        json.addProperty("description", item.description);
        json.addProperty("amountPerCutoffCents", item.perCutoffCents);
        json.addProperty("fromMonth", item.fromMonth.toString());
        json.addProperty("toMonth", item.toMonth == null ? "" : item.toMonth.toString());
        json.addProperty("onlyCutoff", item.onlyCutoff);
        json.addProperty("createdBy", item.createdBy);
        json.addProperty("createdAt", item.createdAt);
        return json.toString();
    }

    /** Release a cutoff's finalized payslips as one disbursement batch (GET lists batches). Payroll Staff / Admin. */
    private void handleDisbursements(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Disbursement is local-demo only.");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod())) {
                guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.PAYROLL));
                JsonArray items = new JsonArray();
                for (DisbursementStore.Batch batch : disbursements.list()) items.add(batchToJson(batch));
                sendJson(exchange, 200, "{\"batches\":" + items + "}");
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            guard(() -> AccessPolicy.requirePayrollProcessing(actor, null));
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "month", "cutoff");
            String month = parseMonth(requiredString(body, "month")).toString();
            String cutoffText = requiredString(body, "cutoff");
            if (!"1".equals(cutoffText) && !"2".equals(cutoffText)) throw new IllegalArgumentException("Cutoff must be 1 (1-15) or 2 (16-end).");
            String label = "1".equals(cutoffText) ? "1-15" : "16-end";
            List<DisbursementStore.Line> lines = new java.util.ArrayList<>();
            for (String snapshot : savedPayslips.list()) {
                JsonObject slip = JsonParser.parseString(snapshot).getAsJsonObject();
                if (!month.equals(slip.get("month").getAsString()) || !label.equals(slip.getAsJsonObject("period").get("label").getAsString())) continue;
                JsonObject person = slip.getAsJsonObject("employee");
                lines.add(new DisbursementStore.Line(slip.get("payslipId").getAsString(), person.get("id").getAsString(), person.get("name").getAsString(),
                    slip.has("payMethod") ? slip.get("payMethod").getAsString() : hrRecords.payMethod(person.get("id").getAsString()),
                    slip.get("netPayCents").getAsLong()));
            }
            DisbursementStore.Batch batch = disbursements.release(month, Integer.parseInt(cutoffText), lines, actor.employeeId);
            // Requirement 10: each released payslip is emailed to the employee (simulated outbox).
            List<PayslipOutbox.Message> emails = payslipOutbox.sendForBatch(batch, month + " " + ("1".equals(cutoffText) ? "1–15" : "16–end"),
                clockService.now().toString());
            for (DisbursementStore.Line line : batch.lines) {
                notify(line.employeeId, actor, "Pay", "Your payslip " + line.payslipId + " was released: net "
                    + String.format(java.util.Locale.ROOT, "₱%,.2f", java.math.BigDecimal.valueOf(line.netCents, 2)) + " by " + line.method + ". It was also emailed to you.");
            }
            payslipOutbox.sendTeamReport(batch, month + " " + ("1".equals(cutoffText) ? "1–15" : "16–end"), emails, clockService.now().toString());
            JsonObject response = new JsonObject();
            response.add("batch", batchToJson(batch));
            response.addProperty("emailsSent", emails.stream().filter(item -> "Sent".equals(item.status)).count());
            response.addProperty("emailsFailed", emails.stream().filter(item -> "Failed".equals(item.status)).count());
            sendJson(exchange, 201, response.toString());
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        } catch (IllegalStateException ex) {
            sendError(exchange, 409, ex.getMessage());
        }
    }

    /**
     * Payslip email outbox (simulated): GET lists messages (employees see their own; supervisors their team; Payroll/HR/Admin all),
     * POST /{id}/resend and /retry-failed retry delivery, POST /outage {on} simulates a mail outage (Payroll Staff / System Admin).
     */
    private void handleOutbox(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "The payslip outbox is local-demo only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("GET".equals(exchange.getRequestMethod()) && "/api/payroll/outbox".equals(path)) {
                JsonArray items = new JsonArray();
                for (PayslipOutbox.Message message : payslipOutbox.list()) {
                    if (AccessPolicy.canRead(actor, AccessPolicy.Area.PAYROLL, message.employeeId)) items.add(new com.google.gson.Gson().toJsonTree(message));
                }
                JsonObject response = new JsonObject();
                response.add("messages", items);
                response.addProperty("outage", payslipOutbox.outage());
                response.addProperty("simulated", true);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            guard(() -> AccessPolicy.requirePayrollProcessing(actor, null));
            JsonObject body = readJsonBody(exchange);
            String now = clockService.now().toString();
            if ("/api/payroll/outbox/outage".equals(path)) {
                requireOnlyKeys(body, "on");
                if (!body.has("on") || !body.get("on").isJsonPrimitive() || !body.get("on").getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("on must be true or false.");
                payslipOutbox.setOutage(body.get("on").getAsBoolean());
                sendJson(exchange, 200, "{\"outage\":" + payslipOutbox.outage() + "}");
                return;
            }
            requireOnlyKeys(body);
            if ("/api/payroll/outbox/retry-failed".equals(path)) {
                sendJson(exchange, 200, "{\"nowSent\":" + payslipOutbox.retryFailed(now) + "}");
                return;
            }
            if (path.matches("/api/payroll/outbox/EM-[0-9]{4,}/resend")) {
                sendJson(exchange, 200, "{\"item\":" + new com.google.gson.Gson().toJson(payslipOutbox.resend(path.split("/")[4], now)) + "}");
                return;
            }
            sendError(exchange, 404, "Outbox endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (NoSuchElementException ex) {
            sendError(exchange, 404, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    private JsonObject batchToJson(DisbursementStore.Batch batch) {
        JsonObject json = new JsonObject();
        json.addProperty("id", batch.id);
        json.addProperty("status", payslipOutbox.batchStatus(batch.id));
        json.addProperty("month", batch.month);
        json.addProperty("cutoff", batch.cutoff);
        json.addProperty("releasedBy", batch.releasedBy);
        json.addProperty("releasedAt", batch.releasedAt);
        json.addProperty("totalCents", batch.totalCents);
        JsonArray lines = new JsonArray();
        for (DisbursementStore.Line line : batch.lines) {
            JsonObject item = new JsonObject();
            item.addProperty("payslipId", line.payslipId);
            item.addProperty("employeeId", line.employeeId);
            item.addProperty("employeeName", line.employeeName);
            item.addProperty("method", line.method);
            item.addProperty("netCents", line.netCents);
            lines.add(item);
        }
        json.add("lines", lines);
        return json;
    }

    /** Monthly remittance: withheld employee shares plus employer shares from finalized payslips, per agency. */
    private void handleRemittance(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Remittance reporting is local-demo only.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.REMITTANCE));
            String month = parseMonth(queryParameter(exchange.getRequestURI().getRawQuery(), "month")).toString();
            java.util.Map<String, long[]> perEmployee = new java.util.LinkedHashMap<>();
            java.util.Map<String, String> names = new java.util.HashMap<>();
            java.util.Map<String, Integer> counts = new java.util.HashMap<>();
            for (String snapshot : savedPayslips.list()) {
                JsonObject slip = JsonParser.parseString(snapshot).getAsJsonObject();
                if (!month.equals(slip.get("month").getAsString())) continue;
                String id = slip.getAsJsonObject("employee").get("id").getAsString();
                names.put(id, slip.getAsJsonObject("employee").get("name").getAsString());
                counts.merge(id, 1, Integer::sum);
                JsonObject d = slip.getAsJsonObject("deductions");
                JsonObject er = slip.has("employerContributions") ? slip.getAsJsonObject("employerContributions") : new JsonObject();
                long[] sums = perEmployee.computeIfAbsent(id, key -> new long[7]);
                sums[0] += d.get("sssCents").getAsLong();
                sums[1] += er.has("sssCents") ? er.get("sssCents").getAsLong() : 0;
                sums[2] += d.get("philHealthCents").getAsLong();
                sums[3] += er.has("philHealthCents") ? er.get("philHealthCents").getAsLong() : 0;
                sums[4] += d.get("pagIbigCents").getAsLong();
                sums[5] += er.has("pagIbigCents") ? er.get("pagIbigCents").getAsLong() : 0;
                sums[6] += d.get("withholdingTaxCents").getAsLong();
            }
            long[] total = new long[7];
            JsonArray rows = new JsonArray();
            for (java.util.Map.Entry<String, long[]> entry : perEmployee.entrySet()) {
                long[] v = entry.getValue();
                for (int i = 0; i < 7; i++) total[i] += v[i];
                JsonObject row = new JsonObject();
                row.addProperty("employeeId", entry.getKey());
                row.addProperty("employeeName", names.get(entry.getKey()));
                row.addProperty("payslipCount", counts.get(entry.getKey()));
                row.addProperty("sssEmployeeCents", v[0]);
                row.addProperty("sssEmployerCents", v[1]);
                row.addProperty("philHealthEmployeeCents", v[2]);
                row.addProperty("philHealthEmployerCents", v[3]);
                row.addProperty("pagIbigEmployeeCents", v[4]);
                row.addProperty("pagIbigEmployerCents", v[5]);
                row.addProperty("withholdingTaxCents", v[6]);
                rows.add(row);
            }
            JsonArray agencies = new JsonArray();
            String[][] names2 = {{"SSS", "0", "1"}, {"PhilHealth", "2", "3"}, {"Pag-IBIG", "4", "5"}, {"BIR (withholding tax)", "6", ""}};
            for (String[] agency : names2) {
                long employee = total[Integer.parseInt(agency[1])];
                long employerShare = agency[2].isEmpty() ? 0 : total[Integer.parseInt(agency[2])];
                JsonObject item = new JsonObject();
                item.addProperty("agency", agency[0]);
                item.addProperty("employeeCents", employee);
                item.addProperty("employerCents", employerShare);
                item.addProperty("totalCents", employee + employerShare);
                agencies.add(item);
            }
            JsonObject response = new JsonObject();
            response.addProperty("month", month);
            response.add("agencies", agencies);
            response.add("employees", rows);
            response.addProperty("basis", "finalized-payslips-only");
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    /**
     * Requirement 10: consolidated payroll summary for a month from finalized payslips (voided ones excluded), grouped by
     * department or branch. Department and branch are as on the payslip; payslips saved before the branch was recorded use
     * the employee's current branch.
     */
    private void handlePayrollSummary(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 403, "Payroll reporting is local-demo only.");
            return;
        }
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.REMITTANCE));
            String query = exchange.getRequestURI().getRawQuery();
            String month = parseMonth(queryParameter(query, "month")).toString();
            String byText = queryParameter(query, "by");
            String by = byText == null || byText.trim().isEmpty() ? "department" : byText.trim();
            if (!"department".equals(by) && !"branch".equals(by)) throw new IllegalArgumentException("by must be department or branch.");
            Map<String, long[]> groups = new java.util.TreeMap<>();
            Map<String, java.util.Set<String>> people = new java.util.HashMap<>();
            long[] total = new long[5];
            java.util.Set<String> everyone = new java.util.HashSet<>();
            List<String[]> detail = new java.util.ArrayList<>(); // one line per payslip, for the CSV / PDF files
            for (String snapshot : savedPayslips.list()) {
                JsonObject slip = JsonParser.parseString(snapshot).getAsJsonObject();
                if (!month.equals(slip.get("month").getAsString())) continue;
                JsonObject person = slip.getAsJsonObject("employee");
                String id = person.get("id").getAsString();
                String name = "department".equals(by) ? person.get("department").getAsString()
                    : person.has("location") ? person.get("location").getAsString()
                    : localEmployeeService.findEmployee(id).map(Employee::getLocation).orElse("Unknown");
                long[] values = {1, slip.get("grossPayCents").getAsLong(), slip.getAsJsonObject("deductions").get("totalCents").getAsLong(),
                    slip.getAsJsonObject("allowances").get("totalCents").getAsLong(), slip.get("netPayCents").getAsLong()};
                long[] sums = groups.computeIfAbsent(name, key -> new long[5]);
                for (int i = 0; i < 5; i++) {
                    sums[i] += values[i];
                    total[i] += values[i];
                }
                people.computeIfAbsent(name, key -> new java.util.HashSet<>()).add(id);
                everyone.add(id);
                detail.add(new String[] {slip.get("payslipId").getAsString(), id, person.get("name").getAsString(), name,
                    String.valueOf(values[1]), String.valueOf(values[2]), String.valueOf(values[3]), String.valueOf(values[4])});
            }
            String format = nullToEmpty(queryParameter(query, "format")).trim();
            if ("csv".equals(format) || "pdf".equals(format)) {
                sendPayrollReportFile(exchange, format, month, by, detail, groups, people, total, everyone.size());
                return;
            }
            if (!format.isEmpty()) throw new IllegalArgumentException("format must be csv or pdf.");
            JsonArray rows = new JsonArray();
            for (Map.Entry<String, long[]> entry : groups.entrySet()) {
                JsonObject row = summaryRow(entry.getValue(), people.get(entry.getKey()).size());
                row.addProperty("name", entry.getKey());
                rows.add(row);
            }
            JsonObject response = new JsonObject();
            response.addProperty("month", month);
            response.addProperty("by", by);
            response.add("groups", rows);
            response.add("total", summaryRow(total, everyone.size()));
            response.addProperty("basis", "finalized-payslips-only");
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    /** Design doc, Payroll Distribution: the aggregate payroll report as a file (CSV or PDF) for Payroll Staff. */
    private void sendPayrollReportFile(HttpExchange exchange, String format, String month, String by, List<String[]> detail,
            Map<String, long[]> groups, Map<String, java.util.Set<String>> people, long[] total, int employees) throws IOException {
        java.util.function.LongFunction<String> pesos = cents -> java.math.BigDecimal.valueOf(cents, 2).toPlainString();
        byte[] bytes;
        String type;
        if ("csv".equals(format)) {
            StringBuilder csv = new StringBuilder("Payslip,Employee ID,Employee,").append("department".equals(by) ? "Department" : "Branch")
                .append(",Gross (PHP),Deductions (PHP),Allowances (PHP),Net (PHP)\r\n");
            for (String[] row : detail) {
                csv.append(csvCell(row[0])).append(',').append(csvCell(row[1])).append(',').append(csvCell(row[2])).append(',').append(csvCell(row[3]));
                for (int i = 4; i < 8; i++) csv.append(',').append(pesos.apply(Long.parseLong(row[i])));
                csv.append("\r\n");
            }
            for (Map.Entry<String, long[]> entry : groups.entrySet()) {
                long[] v = entry.getValue();
                csv.append("SUBTOTAL,,").append(csvCell(people.get(entry.getKey()).size() + " employee(s)")).append(',').append(csvCell(entry.getKey()));
                for (int i = 1; i < 5; i++) csv.append(',').append(pesos.apply(v[i]));
                csv.append("\r\n");
            }
            csv.append("TOTAL,,").append(csvCell(employees + " employee(s)")).append(",");
            for (int i = 1; i < 5; i++) csv.append(',').append(pesos.apply(total[i]));
            csv.append("\r\n");
            bytes = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8); // byte-order mark so Excel reads it as UTF-8
            type = "text/csv; charset=utf-8";
        } else {
            List<String> lines = new java.util.ArrayList<>();
            lines.add("MotorPH Payroll Office - Payroll summary report " + month + " by " + by + " (synthetic demo data)");
            lines.add("Finalized payslips only; amounts in PHP.");
            lines.add("");
            for (String[] row : detail) {
                lines.add(row[0] + "  " + row[2] + "  (" + row[3] + ")  gross " + pesos.apply(Long.parseLong(row[4])) + "  deductions "
                    + pesos.apply(Long.parseLong(row[5])) + "  allowances " + pesos.apply(Long.parseLong(row[6])) + "  net " + pesos.apply(Long.parseLong(row[7])));
            }
            lines.add("");
            for (Map.Entry<String, long[]> entry : groups.entrySet()) {
                long[] v = entry.getValue();
                lines.add(entry.getKey() + ": " + people.get(entry.getKey()).size() + " employee(s), " + v[0] + " payslip(s), gross " + pesos.apply(v[1])
                    + ", deductions " + pesos.apply(v[2]) + ", allowances " + pesos.apply(v[3]) + ", net " + pesos.apply(v[4]));
            }
            lines.add("TOTAL: " + employees + " employee(s), " + total[0] + " payslip(s), gross " + pesos.apply(total[1]) + ", deductions " + pesos.apply(total[2])
                + ", allowances " + pesos.apply(total[3]) + ", net " + pesos.apply(total[4]));
            bytes = PayslipPdf.renderLines(lines);
            type = "application/pdf";
        }
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"motorph-payroll-summary-" + month + "-" + by + "." + format + "\"");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    /** CSV cell: quoted when needed, and a leading = + - @ is neutralised so spreadsheets never run it as a formula. */
    private static String csvCell(String value) {
        String text = value == null ? "" : value;
        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) text = "'" + text;
        return text.matches(".*[\",\r\n].*") ? "\"" + text.replace("\"", "\"\"") + "\"" : text;
    }

    private static JsonObject summaryRow(long[] values, int employees) {
        JsonObject row = new JsonObject();
        row.addProperty("employees", employees);
        row.addProperty("payslips", values[0]);
        row.addProperty("grossCents", values[1]);
        row.addProperty("deductionsCents", values[2]);
        row.addProperty("allowancesCents", values[3]);
        row.addProperty("netCents", values[4]);
        return row;
    }

    /** Saved payroll history (local demo only): finalize a cutoff payslip once, then list and reopen the frozen snapshot. */
    private void handleSavedPayslips(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendJson(exchange, 403, "{\"error\":\"Saved payroll history is local-demo only; Firebase rules have no payroll collection.\"}");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/payroll/payslips".equals(path)) {
                JsonArray items = new JsonArray();
                AccessPolicy.Actor actor = actorFor(exchange);
                List<String> snapshots = new java.util.ArrayList<>(savedPayslips.list());
                snapshots.addAll(savedPayslips.listVoided());
                for (String snapshot : snapshots) {
                    JsonObject slip = JsonParser.parseString(snapshot).getAsJsonObject();
                    if (!AccessPolicy.canRead(actor, AccessPolicy.Area.PAYROLL, slip.getAsJsonObject("employee").get("id").getAsString())) continue;
                    JsonObject item = new JsonObject();
                    boolean isVoided = slip.has("voided") && slip.get("voided").getAsBoolean();
                    item.addProperty("payslipId", isVoided ? slip.get("archiveId").getAsString() : slip.get("payslipId").getAsString());
                    item.addProperty("voided", isVoided);
                    item.addProperty("version", slip.has("version") ? slip.get("version").getAsInt() : 1);
                    item.addProperty("employeeId", slip.getAsJsonObject("employee").get("id").getAsString());
                    item.addProperty("employeeName", slip.getAsJsonObject("employee").get("name").getAsString());
                    item.addProperty("month", slip.get("month").getAsString());
                    item.addProperty("cutoff", slip.getAsJsonObject("period").get("label").getAsString());
                    item.addProperty("grossPayCents", slip.get("grossPayCents").getAsLong());
                    item.addProperty("netPayCents", slip.get("netPayCents").getAsLong());
                    item.addProperty("finalizedAt", slip.get("finalizedAt").getAsString());
                    item.addProperty("released", disbursements.isReleased(slip.get("payslipId").getAsString()));
                    items.add(item);
                }
                JsonObject response = new JsonObject();
                response.add("payslips", items);
                response.addProperty("storage", "local-process-memory");
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("GET".equals(method) && path.matches("/api/payroll/payslips/[A-Za-z0-9-]+/pdf")) {
                String id = path.split("/")[4];
                String snapshot = savedPayslips.find(id).orElseThrow(() -> new NoSuchElementException("Saved payslip not found."));
                AccessPolicy.Actor actor = actorFor(exchange);
                JsonObject slip = JsonParser.parseString(snapshot).getAsJsonObject();
                guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.PAYROLL, slip.getAsJsonObject("employee").get("id").getAsString()));
                byte[] pdf = PayslipPdf.render(slip);
                exchange.getResponseHeaders().set("Content-Type", "application/pdf");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + id + ".pdf\"");
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.sendResponseHeaders(200, pdf.length);
                try (OutputStream out = exchange.getResponseBody()) { out.write(pdf); }
                return;
            }
            if ("GET".equals(method) && path.startsWith("/api/payroll/payslips/")) {
                String id = path.substring("/api/payroll/payslips/".length());
                String snapshot = savedPayslips.find(id).orElseThrow(() -> new NoSuchElementException("Saved payslip not found."));
                AccessPolicy.Actor actor = actorFor(exchange);
                String owner = JsonParser.parseString(snapshot).getAsJsonObject().getAsJsonObject("employee").get("id").getAsString();
                guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.PAYROLL, owner));
                sendJson(exchange, 200, "{\"payslip\":" + snapshot + "}");
                return;
            }
            if ("POST".equals(method) && "/api/payroll/payslips".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "month", "cutoff");
                AccessPolicy.Actor actor = actorFor(exchange);
                String owner = requiredString(body, "employeeId").trim();
                guard(() -> AccessPolicy.requireFinalize(actor, owner));
                JsonObject slip = buildPayslip(exchange, requiredString(body, "employeeId"), requiredString(body, "month"), requiredString(body, "cutoff"));
                slip.addProperty("saved", true);
                slip.addProperty("finalizedAt", java.time.Instant.now().toString());
                slip.addProperty("version", savedPayslips.voidCount(slip.get("payslipId").getAsString()) + 1);
                savedPayslips.save(slip.get("payslipId").getAsString(), slip.toString());
                JsonObject response = new JsonObject();
                response.add("payslip", slip);
                sendJson(exchange, 201, response.toString());
                return;
            }
            sendJson(exchange, "GET".equals(method) || "POST".equals(method) ? 404 : 405, "{\"error\":\"Payslip history endpoint not found\"}");
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalStateException ex) {
            sendJson(exchange, 409, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (NoSuchElementException ex) {
            sendJson(exchange, 404, "{\"error\":\"" + escapeJson(ex.getMessage() == null ? "Employee not found" : ex.getMessage()) + "\"}");
        }
    }

    /** Minutes/days and each earnings line (centavos) behind a cutoff's gross. */
    private static void addEarnings(JsonObject target, CutoffPay cutoff) {
        target.addProperty("overtimeMinutes", cutoff.getOvertimeMinutes());
        target.addProperty("nightDiffMinutes", cutoff.getNightDiffMinutes());
        target.addProperty("holidayDays", cutoff.getHolidayDays());
        target.addProperty("leaveDays", cutoff.getLeaveDays());
        JsonObject earnings = new JsonObject();
        earnings.addProperty("basicCents", cents(cutoff.getBasicPay()));
        earnings.addProperty("premiumCents", cents(cutoff.getPremiumPay()));
        earnings.addProperty("overtimeCents", cents(cutoff.getOvertimePay()));
        earnings.addProperty("nightDiffCents", cents(cutoff.getNightDiffPay()));
        earnings.addProperty("holidayPayCents", cents(cutoff.getHolidayPay()));
        earnings.addProperty("leavePayCents", cents(cutoff.getLeavePay()));
        target.add("earnings", earnings);
    }

    private static long cents(java.math.BigDecimal pesos) { return pesos.movePointRight(2).longValueExact(); }

    private MonthPayroll monthPayrollFor(HttpExchange exchange, String employeeId, String monthText) {
        return monthPayrollFor(exchange, employeeId, monthText, null);
    }

    private MonthPayroll monthPayrollFor(HttpExchange exchange, String employeeId, String monthText, LocalDate through) {
        if (monthText == null || !monthText.matches("20[0-9]{2}-(0[1-9]|1[0-2])")) throw new IllegalArgumentException("Month must use YYYY-MM.");
        AccessPolicy.Actor actor = actorFor(exchange);
        guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.PAYROLL, employeeId));
        RequestServices services = servicesFor(exchange);
        AttendanceService attendanceService = new AttendanceService(services.employeeService, localScheduleStore, services.attendanceStore);
        return new CutoffPayrollService(attendanceService, workflowsFor(exchange)).withAdjustments(firebaseProjectId.isEmpty() ? payrollAdjustments : null)
            .withSchedules(firebaseProjectId.isEmpty() ? workSchedules : null, firebaseProjectId.isEmpty() ? salaryHistory : null)
            .withEmployment(firebaseProjectId.isEmpty() ? hrRecords : null)
            .withRates(firebaseProjectId.isEmpty() ? statutoryRates : null)
            .payrollForMonth(employeeId, java.time.YearMonth.parse(monthText), through);
    }

    private void handlePayrollStatutory(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        try {
            AccessPolicy.Actor actor = actorFor(exchange);
            guard(() -> AccessPolicy.requireArea(actor, AccessPolicy.Area.STATUTORY));
            JsonObject body = readJsonBody(exchange);
            requireOnlyKeys(body, "period", "grossCashPay", "sssCompensation", "philHealthBasicSalary",
                "pagIbigFundSalary", "taxableCompensation");
            String periodText = requiredString(body, "period");
            if (!periodText.matches("20[0-9]{2}-(0[1-9]|1[0-2])")) throw new IllegalArgumentException("Month must use YYYY-MM.");
            StatutoryBreakdown result = StatutoryCalculator2026.calculate(java.time.YearMonth.parse(periodText),
                StatutoryCalculator2026.parsePesoCents(requiredString(body, "grossCashPay"), "Gross cash pay"),
                StatutoryCalculator2026.parsePesoCents(requiredString(body, "sssCompensation"), "SSS compensation base"),
                StatutoryCalculator2026.parsePesoCents(requiredString(body, "philHealthBasicSalary"), "PhilHealth monthly basic salary"),
                StatutoryCalculator2026.parsePesoCents(requiredString(body, "pagIbigFundSalary"), "Pag-IBIG Fund Salary"),
                StatutoryCalculator2026.parsePesoCents(requiredString(body, "taxableCompensation"), "Taxable compensation before employee contributions"),
                firebaseProjectId.isEmpty() ? statutoryRates.ratesFor(java.time.YearMonth.parse(periodText)) : StatutoryRates.DEFAULT);
            JsonObject response = new JsonObject();
            response.add("result", statutoryToJson(result));
            response.addProperty("saved", false);
            sendJson(exchange, 200, response.toString());
        } catch (RequestException ex) {
            sendJson(exchange, ex.statusCode, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        } catch (IllegalArgumentException ex) {
            sendJson(exchange, 400, "{\"error\":\"" + escapeJson(ex.getMessage()) + "\"}");
        }
    }

    /** Same shape the browser preview/CSV code already reads; amounts are integer centavos. */
    private static JsonObject statutoryToJson(StatutoryBreakdown r) {
        JsonObject sss = new JsonObject();
        sss.addProperty("mscCents", r.sssMscCents);
        sss.addProperty("employeeCents", r.sssEmployeeCents);
        sss.addProperty("employerRegularCents", r.sssEmployerRegularCents);
        sss.addProperty("ecCents", r.sssEcCents);
        sss.addProperty("employerTotalCents", r.sssEmployerTotalCents());
        JsonObject philHealth = new JsonObject();
        philHealth.addProperty("premiumBaseCents", r.philHealthPremiumBaseCents);
        philHealth.addProperty("totalPremiumCents", r.philHealthTotalPremiumCents);
        philHealth.addProperty("employeeCents", r.philHealthEmployeeCents);
        philHealth.addProperty("employerCents", r.philHealthEmployerCents);
        JsonObject pagIbig = new JsonObject();
        pagIbig.addProperty("baseCents", r.pagIbigBaseCents);
        pagIbig.addProperty("employeeCents", r.pagIbigEmployeeCents);
        pagIbig.addProperty("employerCents", r.pagIbigEmployerCents);
        JsonObject item = new JsonObject();
        item.addProperty("rulesetId", r.rulesetId);
        item.addProperty("period", r.period.toString());
        item.addProperty("grossCashPayCents", r.grossCashPayCents);
        item.addProperty("sssCompensationCents", r.sssCompensationCents);
        item.addProperty("philHealthBasicSalaryCents", r.philHealthBasicSalaryCents);
        item.addProperty("pagIbigFundSalaryCents", r.pagIbigFundSalaryCents);
        item.add("sss", sss);
        item.add("philHealth", philHealth);
        item.add("pagIbig", pagIbig);
        item.addProperty("taxableCompensationCents", r.taxableCompensationCents);
        item.addProperty("taxableAfterEmployeeContributionsCents", r.taxableAfterEmployeeContributionsCents);
        item.addProperty("withholdingTaxCents", r.withholdingTaxCents);
        item.addProperty("totalEmployeeStatutoryCents", r.totalEmployeeStatutoryCents());
        item.addProperty("totalEmployeeDeductionsCents", r.totalEmployeeDeductionsCents());
        item.addProperty("netPayCents", r.netPayCents());
        item.addProperty("totalEmployerContributionsCents", r.totalEmployerContributionsCents());
        item.addProperty("employerCashCostCents", r.employerCashCostCents());
        return item;
    }

    private void handleWorkflows(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/workflows".equals(path)) {
                WorkflowService service = workflowsFor(exchange);
                AccessPolicy.Actor actor = actorFor(exchange);
                List<DemoLeaveRequest> leaves = new java.util.ArrayList<>();
                for (DemoLeaveRequest item : service.listLeaveRequests()) {
                    if (AccessPolicy.canRead(actor, AccessPolicy.Area.LEAVE, item.getEmployeeId())) leaves.add(item);
                }
                List<DemoTicket> tickets = new java.util.ArrayList<>();
                List<DemoTicket> allTickets = service.listTickets();
                for (DemoTicket item : allTickets) {
                    if (AccessPolicy.canRead(actor, AccessPolicy.Area.TICKETS, item.getEmployeeId())
                        || (actor.explicit && AccessPolicy.canHandleTicket(actor, item.getQueue(), item.getEmployeeId()))) tickets.add(item);
                }
                List<DemoOvertimeRequest> overtime = new java.util.ArrayList<>();
                for (DemoOvertimeRequest item : service.listOvertimeRequests()) {
                    if (AccessPolicy.canRead(actor, AccessPolicy.Area.OVERTIME, item.getEmployeeId())) overtime.add(item);
                }
                sendJson(exchange, 200, workflowsToJson(leaves, tickets, overtime,
                    firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore", queuePositions(allTickets)));
                return;
            }
            if ("GET".equals(method) && "/api/workflows/leave/balances".equals(path)) {
                String query = exchange.getRequestURI().getRawQuery();
                String yearText = queryParameter(query, "year");
                int year;
                try {
                    year = yearText == null || yearText.isEmpty() ? LocalDate.now().getYear() : Integer.parseInt(yearText);
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Year must be a number.");
                }
                if (year < 2000 || year > 2100) throw new IllegalArgumentException("Year must be between 2000 and 2100.");
                String employeeId = queryParameter(query, "employeeId");
                AccessPolicy.Actor actor = actorFor(exchange);
                guard(() -> AccessPolicy.requireRead(actor, AccessPolicy.Area.LEAVE, employeeId == null ? "" : employeeId.trim()));
                sendJson(exchange, 200, balancesToJson(workflowsFor(exchange).leaveBalances(employeeId, year), year));
                return;
            }
            if (!"POST".equals(method)) {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                return;
            }

            if (path.startsWith("/api/workflows/overtime")) {
                // Local demo only, checked before auth: Firebase rules have no overtime collection or reviewer role.
                if (!firebaseProjectId.isEmpty()) {
                    sendJson(exchange, 403, "{\"error\":\"Overtime requests are local-demo only; Firebase rules have no reviewer role.\"}");
                    return;
                }
                JsonObject body = readJsonBody(exchange);
                WorkflowService service = workflowsFor(exchange);
                AccessPolicy.Actor actor = actorFor(exchange);
                DemoOvertimeRequest request;
                if ("/api/workflows/overtime".equals(path)) {
                    requireOnlyKeys(body, "employeeId", "date", "minutes", "note");
                    JsonElement minutes = body.get("minutes");
                    if (minutes == null || !minutes.isJsonPrimitive() || !minutes.getAsJsonPrimitive().isNumber()
                            || minutes.getAsDouble() != Math.rint(minutes.getAsDouble())) {
                        throw new IllegalArgumentException("minutes is required and must be a whole number.");
                    }
                    String target = requiredString(body, "employeeId").trim();
                    guard(() -> AccessPolicy.requireCreate(actor, AccessPolicy.Area.OVERTIME, target));
                    request = service.createOvertimeRequest(target, requiredString(body, "date"),
                        (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, minutes.getAsLong())), optionalString(body, "note"));
                    sendJson(exchange, 201, itemToJson(overtimeToJson(request), "local-process-memory"));
                    return;
                }
                if (path.matches("/api/workflows/overtime/[^/]+/(approve|reject)")) {
                    String[] parts = path.split("/");
                    String owner = service.listOvertimeRequests().stream().filter(item -> item.getId().equals(parts[4]))
                        .map(DemoOvertimeRequest::getEmployeeId).findFirst().orElseThrow(() -> new NoSuchElementException("Overtime request was not found."));
                    request = service.reviewOvertimeRequest(parts[4], parts[5], reviewerFor(actor, body, AccessPolicy.Area.OVERTIME, owner));
                    notify(owner, actor, "Overtime", "Your overtime request for " + request.getDate() + " (" + request.getMinutes() + " min) was "
                        + request.getStatus().toLowerCase() + " by " + nameOf(request.getReviewerId()) + ".");
                    sendJson(exchange, 200, itemToJson(overtimeToJson(request), "local-process-memory"));
                    return;
                }
                sendJson(exchange, 404, "{\"error\":\"Workflow endpoint not found\"}");
                return;
            }

            if (path.matches("/api/workflows/leave/[^/]+/(approve|reject)")) {
                // Checked before auth, like raw attendance entry: Firebase mode has no reviewer role.
                if (!firebaseProjectId.isEmpty()) {
                    sendJson(exchange, 403, "{\"error\":\"Leave approval is local-demo only; Firebase rules have no reviewer role.\"}");
                    return;
                }
                String[] parts = path.split("/");
                JsonObject body = readJsonBody(exchange);
                WorkflowService reviewService = workflowsFor(exchange);
                String owner = reviewService.listLeaveRequests().stream().filter(item -> item.getId().equals(parts[4]))
                    .map(DemoLeaveRequest::getEmployeeId).findFirst().orElseThrow(() -> new NoSuchElementException("Leave request was not found."));
                AccessPolicy.Actor leaveReviewer = actorFor(exchange);
                DemoLeaveRequest request = reviewService.reviewLeaveRequest(parts[4], parts[5],
                    reviewerFor(leaveReviewer, body, AccessPolicy.Area.LEAVE, owner));
                notify(owner, leaveReviewer, "Leave", "Your " + request.getLeaveType() + " leave " + request.getStartDate() + " to " + request.getEndDate()
                    + " was " + request.getStatus().toLowerCase() + " by " + nameOf(request.getReviewerId()) + ".");
                sendJson(exchange, 200, itemToJson(leaveToJson(request), "local-process-memory"));
                return;
            }

            WorkflowService service = workflowsFor(exchange);
            AccessPolicy.Actor actor = actorFor(exchange);
            if ("/api/workflows/leave".equals(path)) {
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "employeeId", "leaveType", "startDate", "endDate", "note");
                String target = requiredString(body, "employeeId").trim();
                guard(() -> AccessPolicy.requireCreate(actor, AccessPolicy.Area.LEAVE, target));
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
                String target = requiredString(body, "employeeId").trim();
                guard(() -> AccessPolicy.requireCreate(actor, AccessPolicy.Area.TICKETS, target));
                DemoTicket ticket = service.createTicket(requiredString(body, "employeeId"),
                    requiredString(body, "ticketType"), requiredString(body, "summary"));
                JsonObject created = ticketToJson(ticket);
                Integer position = queuePositions(service.listTickets()).get(ticket.getId());
                if (position != null) created.addProperty("queuePosition", position);
                sendJson(exchange, 201, itemToJson(created,
                    firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                return;
            }
            if (path.matches("/api/workflows/tickets/TK-[A-F0-9]{12}/start")) {
                if (!firebaseProjectId.isEmpty()) throw new RequestException(403, "Handling tickets is local-demo only.");
                String ticketId = path.split("/")[4];
                DemoTicket ticket = service.listTickets().stream().filter(item -> item.getId().equals(ticketId)).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Ticket was not found."));
                guard(() -> AccessPolicy.requireTicketResolve(actor, ticket.getQueue(), ticket.getEmployeeId()));
                requireOnlyKeys(readJsonBody(exchange));
                DemoTicket started = service.startTicket(ticketId, actor.employeeId);
                notify(started.getEmployeeId(), actor, "Ticket", "Your " + started.getTicketType().toLowerCase() + " ticket " + started.getId()
                    + " is now in progress with " + nameOf(actor.employeeId) + ".");
                sendJson(exchange, 200, itemToJson(ticketToJson(started), "local-process-memory"));
                return;
            }
            if (path.matches("/api/workflows/tickets/TK-[A-F0-9]{12}/resolve")) {
                if (!firebaseProjectId.isEmpty()) throw new RequestException(403, "Resolving tickets is local-demo only.");
                String ticketId = path.split("/")[4];
                DemoTicket ticket = service.listTickets().stream().filter(item -> item.getId().equals(ticketId)).findFirst()
                    .orElseThrow(() -> new NoSuchElementException("Ticket was not found."));
                guard(() -> AccessPolicy.requireTicketResolve(actor, ticket.getQueue(), ticket.getEmployeeId()));
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "reply");
                DemoTicket resolved = service.resolveTicket(ticketId, requiredString(body, "reply"), actor.employeeId);
                notify(resolved.getEmployeeId(), actor, "Ticket", "Your " + resolved.getTicketType().toLowerCase() + " ticket " + resolved.getId()
                    + " was resolved by " + nameOf(actor.employeeId) + ": " + resolved.getReply());
                sendJson(exchange, 200, itemToJson(ticketToJson(resolved), "local-process-memory"));
                return;
            }
            String[] segments = path.substring("/api/workflows/".length()).split("/");
            if (segments.length == 3 && "cancel".equals(segments[2])) {
                if ("leave".equals(segments[0])) {
                    service.listLeaveRequests().stream().filter(item -> item.getId().equals(segments[1])).findFirst()
                        .ifPresent(item -> guard(() -> AccessPolicy.requireCancel(actor, AccessPolicy.Area.LEAVE, item.getEmployeeId())));
                    DemoLeaveRequest request = service.cancelLeaveRequest(segments[1]);
                    sendJson(exchange, 200, itemToJson(leaveToJson(request),
                        firebaseProjectId.isEmpty() ? "local-process-memory" : "user-scoped-firestore"));
                    return;
                }
                if ("tickets".equals(segments[0])) {
                    service.listTickets().stream().filter(item -> item.getId().equals(segments[1])).findFirst()
                        .ifPresent(item -> guard(() -> AccessPolicy.requireCancel(actor, AccessPolicy.Area.TICKETS, item.getEmployeeId())));
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

    /**
     * The signed-in demo account, from the X-Demo-User header the browser sends after choosing an account.
     * Firebase mode: the signed-in Firebase user acts as one fake person from their own private sandbox (same rules).
     * shortcut: a call without the header (scripts, older tests) is unrestricted with explicit=false; replace with
     * a real login before anything leaves the demo. In Firebase mode the data is the caller's own sandbox either way.
     */
    private AccessPolicy.Actor actorFor(HttpExchange exchange) {
        String id = exchange.getRequestHeaders().getFirst("X-Demo-User");
        if (id == null || id.trim().isEmpty()) return new AccessPolicy.Actor("", AccessPolicy.Role.ADMIN, false);
        String trimmed = id.trim();
        if (!firebaseProjectId.isEmpty()) {
            if (!servicesFor(exchange).employeeService.findEmployee(trimmed).isPresent()) throw new RequestException(401, "That person is not in your sandbox.");
            return new AccessPolicy.Actor(trimmed, AccessPolicy.roleOf(trimmed), true);
        }
        if (!localEmployeeService.findEmployee(trimmed).isPresent()) throw new RequestException(401, "Unknown demo account.");
        if (hrRecords.isSeparated(trimmed, clockService.now().toLocalDate())) throw new RequestException(401, "This account was closed after the employee's last day.");
        if (!twoFactor.sessionOwner(exchange.getRequestHeaders().getFirst("X-Demo-Session")).filter(trimmed::equals).isPresent()) {
            throw new RequestException(401, "Sign in again: enter the 2FA code for this account first.");
        }
        return new AccessPolicy.Actor(trimmed, AccessPolicy.roleOf(trimmed), true);
    }

    /**
     * Simulated 2FA for the local demo accounts (see TwoFactor): POST /api/auth/2fa/start {employeeId} "texts" a code,
     * POST /api/auth/2fa/verify {challengeId, code} returns the session token, GET /api/auth/session checks it,
     * POST /api/auth/signout ends it. Firebase mode signs in with Firebase instead.
     */
    private void handleAuth(HttpExchange exchange) throws IOException {
        if (!firebaseProjectId.isEmpty()) {
            sendError(exchange, 404, "Firebase mode signs in with Firebase; demo 2FA is local only.");
            return;
        }
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method) && "/api/auth/session".equals(path)) {
                String owner = twoFactor.sessionOwner(exchange.getRequestHeaders().getFirst("X-Demo-Session"))
                    .orElseThrow(() -> new RequestException(401, "Not signed in."));
                sendJson(exchange, 200, "{\"employeeId\":\"" + escapeJson(owner) + "\"}");
                return;
            }
            if (!"POST".equals(method)) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }
            JsonObject body = readJsonBody(exchange);
            if ("/api/auth/2fa/start".equals(path)) {
                requireOnlyKeys(body, "employeeId", "password");
                String id = requiredString(body, "employeeId").trim();
                exchange.setAttribute("auditTarget", id);
                if (!localEmployeeService.findEmployee(id).isPresent()) throw new RequestException(404, "Unknown demo account.");
                if (hrRecords.isSeparated(id, clockService.now().toLocalDate())) throw new RequestException(401, "This account was closed after the employee's last day.");
                try {
                    accountSecurity.checkPassword(id, optionalString(body, "password"), clockService.now());
                } catch (IllegalArgumentException ex) {
                    throw new RequestException(401, ex.getMessage());
                } catch (AccountSecurity.TooSoon ex) {
                    throw new RequestException(429, ex.getMessage());
                } catch (IllegalStateException ex) {
                    throw new RequestException(423, ex.getMessage());
                }
                TwoFactor.Started started = twoFactor.start(id, accountSecurity.settings().codeMinutes);
                JsonObject response = new JsonObject();
                response.addProperty("challengeId", started.challengeId);
                response.addProperty("sentTo", "the phone number ending in " + id.substring(Math.max(0, id.length() - 2)));
                response.addProperty("expiresAt", started.expires.toLocalTime().toString());
                // Simulated SMS: there is no phone, so the demo shows the code. Never do this with real accounts.
                response.addProperty("demoCode", started.code);
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("/api/auth/2fa/verify".equals(path)) {
                requireOnlyKeys(body, "challengeId", "code");
                String session;
                try {
                    session = twoFactor.verify(requiredString(body, "challengeId"), requiredString(body, "code"), accountSecurity.settings().sessionHours);
                } catch (IllegalArgumentException | IllegalStateException ex) {
                    throw new RequestException(401, ex.getMessage());
                }
                // Design doc: "on success, old sessions for that ID are terminated".
                twoFactor.signOutEverywhere(twoFactor.sessionOwner(session).orElse(""), session);
                JsonObject response = new JsonObject();
                response.addProperty("session", session);
                response.addProperty("employeeId", twoFactor.sessionOwner(session).orElse(""));
                exchange.setAttribute("auditTarget", twoFactor.sessionOwner(session).orElse(""));
                response.addProperty("defaultPassword", accountSecurity.usesDefaultPassword(twoFactor.sessionOwner(session).orElse("")));
                sendJson(exchange, 200, response.toString());
                return;
            }
            if ("/api/auth/password".equals(path)) {
                requireOnlyKeys(body, "current", "next");
                String session = exchange.getRequestHeaders().getFirst("X-Demo-Session");
                String owner = twoFactor.sessionOwner(session).orElseThrow(() -> new RequestException(401, "Sign in first."));
                try {
                    accountSecurity.changePassword(owner, optionalString(body, "current"), optionalString(body, "next"), clockService.now());
                } catch (IllegalStateException ex) {
                    throw new RequestException(423, ex.getMessage());
                }
                twoFactor.signOutEverywhere(owner, session);
                sendJson(exchange, 200, "{\"changed\":true}");
                return;
            }
            if ("/api/auth/signout".equals(path)) {
                requireOnlyKeys(body);
                twoFactor.signOut(exchange.getRequestHeaders().getFirst("X-Demo-Session"));
                sendJson(exchange, 200, "{\"signedOut\":true}");
                return;
            }
            sendError(exchange, 404, "Auth endpoint not found");
        } catch (RequestException ex) {
            sendError(exchange, ex.statusCode, ex.getMessage());
        } catch (AccountSecurity.TooSoon ex) {
            sendError(exchange, 429, ex.getMessage());
        } catch (IllegalArgumentException ex) {
            sendError(exchange, 400, ex.getMessage());
        }
    }

    private static void guard(Runnable check) {
        try {
            check.run();
        } catch (AccessPolicy.AccessDenied ex) {
            throw new RequestException(403, ex.getMessage());
        }
    }

    /** A review is recorded as the signed-in account; legacy callers name a reviewer, who must still be allowed to decide. */
    private static String reviewerFor(AccessPolicy.Actor actor, JsonObject body, AccessPolicy.Area area, String ownerId) {
        requireOnlyKeys(body, "reviewerId");
        String named = optionalString(body, "reviewerId").trim();
        AccessPolicy.Actor reviewer;
        if (actor.explicit) {
            if (!named.isEmpty() && !named.equals(actor.employeeId)) {
                throw new RequestException(403, "Reviews are recorded as the signed-in account; reviewerId must be you.");
            }
            reviewer = actor;
        } else {
            String id = requiredString(body, "reviewerId").trim();
            reviewer = new AccessPolicy.Actor(id, AccessPolicy.roleOf(id), true);
        }
        guard(() -> AccessPolicy.requireReview(reviewer, area, ownerId));
        return reviewer.employeeId;
    }

    /** Local demo account chooser: every directory person with their role and supervisor. */
    private void handleDemoAccounts(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }
        List<Employee> people;
        try {
            // Firebase mode: the people in the caller's own sandbox (requires the Firebase sign-in).
            people = servicesFor(exchange).employeeService.listEmployees();
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
            return;
        }
        JsonArray items = new JsonArray();
        for (Employee employee : people) {
            if (firebaseProjectId.isEmpty() && hrRecords.isSeparated(employee.getId(), clockService.now().toLocalDate())) continue;
            AccessPolicy.Role role = AccessPolicy.roleOf(employee.getId());
            JsonObject item = new JsonObject();
            item.addProperty("employeeId", employee.getId());
            item.addProperty("name", employee.getName());
            item.addProperty("position", employee.getRole());
            item.addProperty("branch", employee.getLocation());
            item.addProperty("role", role.name());
            item.addProperty("roleLabel", role.label);
            item.addProperty("supervisorId", AccessPolicy.supervisorOf(employee.getId()).orElse(""));
            JsonArray team = new JsonArray();
            for (String member : AccessPolicy.teamOf(employee.getId())) team.add(member);
            item.add("team", team);
            items.add(item);
        }
        JsonObject response = new JsonObject();
        response.add("accounts", items);
        response.addProperty("policyVersion", AccessPolicy.VERSION);
        sendJson(exchange, 200, response.toString());
    }

    private WorkflowService workflowsFor(HttpExchange exchange) {
        if (firebaseProjectId.isEmpty()) return new WorkflowService(localEmployeeService, localWorkflowStore);
        FirestoreRestClient client = firestoreClientFor(exchange);
        EmployeeService employees = new EmployeeService(new FirestoreEmployeeStore(client));
        return new WorkflowService(employees, new FirestoreWorkflowStore(client));
    }

    private static JsonObject readJsonBody(HttpExchange exchange) throws IOException {
        return readJsonBody(exchange, 8192);
    }

    /** Same, with a larger limit for the one route that takes an upload (employee photos). */
    private static JsonObject readJsonBody(HttpExchange exchange, int maxBytes) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw new RequestException(415, "Content-Type must be application/json.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(maxBytes + 1);
        if (bytes.length > maxBytes) throw new RequestException(413, "Request body is too large.");
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

    /** Requirement 5 ("how far are you in the queue"): place of each Open ticket in its queue, oldest first, counting everyone's tickets. */
    static Map<String, Integer> queuePositions(List<DemoTicket> tickets) {
        List<DemoTicket> open = new java.util.ArrayList<>();
        for (DemoTicket ticket : tickets) if ("Open".equals(ticket.getStatus())) open.add(ticket);
        // Compare as times: ISO text with fractions of different lengths ("...00.12Z" vs "...00.123Z") does not sort as text.
        // Same time (the Windows clock can tick coarsely): the stable sort keeps the order the tickets were filed in.
        open.sort(java.util.Comparator.comparing((DemoTicket ticket) -> submittedInstant(ticket.getSubmittedAt())));
        Map<String, Integer> places = new java.util.HashMap<>(), positions = new java.util.HashMap<>();
        for (DemoTicket ticket : open) positions.put(ticket.getId(), places.merge(ticket.getQueue(), 1, Integer::sum));
        return positions;
    }

    private static java.time.Instant submittedInstant(String text) {
        try {
            return java.time.Instant.parse(text);
        } catch (RuntimeException ex) {
            return java.time.Instant.MAX; // unreadable time: back of the queue
        }
    }

    private static String workflowsToJson(List<DemoLeaveRequest> leaveRequests, List<DemoTicket> tickets,
            List<DemoOvertimeRequest> overtimeRequests, String storage, Map<String, Integer> positions) {
        JsonObject response = new JsonObject();
        JsonArray leaves = new JsonArray();
        for (DemoLeaveRequest request : leaveRequests) leaves.add(leaveToJson(request));
        JsonArray ticketItems = new JsonArray();
        for (DemoTicket ticket : tickets) {
            JsonObject item = ticketToJson(ticket);
            if (positions.containsKey(ticket.getId())) item.addProperty("queuePosition", positions.get(ticket.getId()));
            ticketItems.add(item);
        }
        response.add("leaveRequests", leaves);
        response.add("tickets", ticketItems);
        JsonArray overtimeItems = new JsonArray();
        for (DemoOvertimeRequest request : overtimeRequests) overtimeItems.add(overtimeToJson(request));
        response.add("overtimeRequests", overtimeItems);
        response.addProperty("storage", storage);
        return response.toString();
    }

    private static String balancesToJson(List<LeaveBalance> balances, int year) {
        JsonArray items = new JsonArray();
        for (LeaveBalance balance : balances) {
            JsonObject item = new JsonObject();
            item.addProperty("leaveType", balance.getLeaveType());
            item.addProperty("allowance", balance.getAllowance());
            item.addProperty("used", balance.getUsed());
            item.addProperty("pending", balance.getPending());
            item.addProperty("remaining", balance.getRemaining());
            items.add(item);
        }
        JsonObject response = new JsonObject();
        response.add("balances", items);
        response.addProperty("year", year);
        response.addProperty("unit", "weekdays");
        response.addProperty("policyVersion", LeavePolicy.VERSION);
        response.addProperty("temporary", true);
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
        item.addProperty("reviewerId", request.getReviewerId());
        item.addProperty("reviewedAt", request.getReviewedAt());
        return item;
    }

    private static JsonObject overtimeToJson(DemoOvertimeRequest request) {
        JsonObject item = new JsonObject();
        item.addProperty("id", request.getId());
        item.addProperty("employeeId", request.getEmployeeId());
        item.addProperty("employeeName", request.getEmployeeName());
        item.addProperty("date", request.getDate());
        item.addProperty("minutes", request.getMinutes());
        item.addProperty("note", request.getNote());
        item.addProperty("status", request.getStatus());
        item.addProperty("submittedAt", request.getSubmittedAt());
        item.addProperty("reviewerId", request.getReviewerId());
        item.addProperty("reviewedAt", request.getReviewedAt());
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
        item.addProperty("reply", ticket.getReply());
        item.addProperty("resolvedBy", ticket.getResolvedBy());
        item.addProperty("resolvedAt", ticket.getResolvedAt());
        return item;
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
                + ",\"attendanceCreated\":" + result.getAttendanceCreated()
                + ",\"syntheticOnly\":true}");
        } catch (FirestoreException ex) {
            sendApiError(exchange, ex);
        }
    }

    private RequestServices servicesFor(HttpExchange exchange) {
        if (firebaseProjectId.isEmpty()) {
            return new RequestServices(localEmployeeService, localAttendanceStore);
        }
        FirestoreRestClient client = firestoreClientFor(exchange);
        return new RequestServices(new EmployeeService(new FirestoreEmployeeStore(client)), new FirestoreAttendanceStore(client));
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
            + "\",\"timeOut\":\"" + record.getTimeOut()
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
        return "{"
            + "\"id\":\"" + escapeJson(employee.getId()) + "\"," 
            + "\"name\":\"" + escapeJson(employee.getName()) + "\"," 
            + "\"department\":\"" + escapeJson(employee.getDepartment()) + "\"," 
            + "\"role\":\"" + escapeJson(employee.getRole()) + "\"," 
            + "\"location\":\"" + escapeJson(employee.getLocation()) + "\"," 
            + "\"status\":\"" + escapeJson(employee.getStatus()) + "\"}"
            ;
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
        private final AttendanceStore attendanceStore;

        RequestServices(EmployeeService employeeService, AttendanceStore attendanceStore) {
            this.employeeService = employeeService;
            this.attendanceStore = attendanceStore;
        }
    }
}
