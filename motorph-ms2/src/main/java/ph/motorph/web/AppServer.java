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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executors;

/** Java 11 HTTP backend for the browser-based MotorPH prototype. */
public final class AppServer {
    private final Path webRoot;
    private final String firebaseProjectId;
    private final String firestoreApiRoot;
    private final EmployeeService localEmployeeService = new EmployeeService(new InMemoryEmployeeStore());
    private final EmployeeScheduleStore localScheduleStore = new InMemoryEmployeeScheduleStore();
    private final AttendanceStore localAttendanceStore = new InMemoryAttendanceStore();
    private final WorkflowStore localWorkflowStore = new InMemoryWorkflowStore();
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
        this.firebaseProjectId = firebaseProjectId == null ? "" : firebaseProjectId.trim();
        if (webRoot == null) throw new IllegalArgumentException("webRoot must not be null");
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.firestoreApiRoot = firestoreApiRoot == null || firestoreApiRoot.trim().isEmpty()
            ? null : firestoreApiRoot.trim();
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
        server.createContext("/api/health", this::handleHealth);
        server.createContext("/api/employees", this::handleEmployees);
        server.createContext("/api/attendance", this::handleAttendance);
        server.createContext("/api/reports/attendance", this::handleAttendanceReport);
        server.createContext("/api/workflows", this::handleWorkflows);
        server.createContext("/api/demo/seed", this::handleSeedDemo);
        server.createContext("/", this::handleStaticFiles);
        executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.start();
        System.out.println("MotorPH Java API listening on " + host + ":" + server.getAddress().getPort());
        System.out.println("Mode: " + (firebaseProjectId.isEmpty() ? "local synthetic demo" : "Firebase-authenticated user sandbox"));
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

    private void handleEmployees(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        try {
            if ("GET".equals(method)) {
                RequestServices services = servicesFor(exchange);
                if ("/api/employees".equals(path) || "/api/employees/".equals(path)) {
                    String query = queryParameter(exchange.getRequestURI().getRawQuery(), "q");
                    List<Employee> matches = services.employeeService.searchEmployees(query);
                    int total = services.employeeService.listEmployees().size();
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
                sendJson(exchange, 200, "{\"item\":" + employeeToJson(employee.get()) + "}");
                return;
            }
            if ("POST".equals(method) && "/api/employees".equals(path)) {
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
                RequestServices services = servicesFor(exchange);
                String id = path.substring("/api/employees/".length());
                if (id.isEmpty() || id.contains("/")) throw new IllegalArgumentException("Employee ID is invalid.");
                JsonObject body = readJsonBody(exchange);
                requireOnlyKeys(body, "name", "department", "role", "location", "status");
                Employee updated = services.employeeService.updateEmployee(id, requiredString(body, "name"),
                    requiredString(body, "department"), requiredString(body, "role"),
                    requiredString(body, "location"), requiredString(body, "status"));
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
            RequestServices services = servicesFor(exchange);
            LocalDate from = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "from"));
            LocalDate to = parseOptionalDate(queryParameter(exchange.getRequestURI().getRawQuery(), "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                localScheduleStore, services.attendanceStore);
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
            RequestServices services = servicesFor(exchange);
            String query = exchange.getRequestURI().getRawQuery();
            LocalDate from = parseOptionalDate(queryParameter(query, "from"));
            LocalDate to = parseOptionalDate(queryParameter(query, "to"));
            AttendanceService attendanceService = new AttendanceService(services.employeeService,
                localScheduleStore, services.attendanceStore);
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
