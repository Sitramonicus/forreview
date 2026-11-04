package ph.motorph.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Offline HTTP integration checks for local mode and the Firebase auth gate. */
public final class AppServerIntegrationTest {
    private static int checks;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        localModeServesSyntheticEmployeeAndAttendanceResponses();
        localModeCreatesAndUpdatesOnlySyntheticDirectoryFields();
        localModeStoresScopedWorkflowRequestsWithValidatedStateChanges();
        firebaseModeScopesWorkflowWritesAndForwardsTheIDToken();
        firebaseModeScopesAttendanceReportReads();
        firebaseModeScopesSyntheticDirectoryWrites();
        staticFilesExposeDefensiveHeadersAndRejectTraversal();
        firebaseModeRejectsUnauthenticatedReadsAndWritesBeforeFirestore();
        firebaseModeRejectsPartialAndMalformedSessionsBeforeFirestore();
        System.out.println("PASS: " + checks + " AppServer integration checks");
    }

    private static void localModeServesSyntheticEmployeeAndAttendanceResponses() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> health = get(base.resolve("/api/health"));
            check(health.statusCode() == 200 && health.body().contains("local-synthetic-demo"), "local health mode reported");

            HttpResponse<String> employees = get(base.resolve("/api/employees?q=finance"));
            check(employees.statusCode() == 200 && employees.body().contains("Eli Santos"), "local employee search works over HTTP");

            HttpResponse<String> noMatches = get(base.resolve("/api/employees?q=not-a-person"));
            check(noMatches.statusCode() == 200 && noMatches.body().contains("\"items\":[]"), "empty employee search is a valid empty result");

            HttpResponse<String> employee = get(base.resolve("/api/employees/DEMO-001"));
            check(employee.statusCode() == 200 && employee.body().contains("\"name\":\"Mara Rivera\""), "employee detail returns selected record over HTTP");
            HttpResponse<String> missingEmployee = get(base.resolve("/api/employees/UNKNOWN"));
            check(missingEmployee.statusCode() == 404, "unknown employee detail returns not found");

            HttpResponse<String> attendance = get(base.resolve("/api/attendance?employeeId=DEMO-001&from=2026-10-01&to=2026-10-15"));
            check(attendance.statusCode() == 200 && attendance.body().contains("\"hoursCalculated\":false"), "local attendance stays raw");

            HttpResponse<String> oneDay = get(base.resolve("/api/attendance?employeeId=DEMO-001&from=2026-10-02&to=2026-10-02"));
            check(oneDay.statusCode() == 200 && oneDay.body().contains("\"count\":1")
                && oneDay.body().contains("\"date\":\"2026-10-02\""), "attendance date bounds include the selected day");

            HttpResponse<String> noAttendance = get(base.resolve("/api/attendance?employeeId=DEMO-002&from=2026-10-10&to=2026-10-10"));
            check(noAttendance.statusCode() == 200 && noAttendance.body().contains("\"count\":0"), "empty selected period returns an empty attendance result");

            HttpResponse<String> missingAttendanceEmployee = get(base.resolve("/api/attendance"));
            check(missingAttendanceEmployee.statusCode() == 400, "attendance query requires an employee ID");

            HttpResponse<String> badDate = get(base.resolve("/api/attendance?employeeId=DEMO-001&from=not-a-date"));
            check(badDate.statusCode() == 400, "malformed attendance date returns client error");

            HttpResponse<String> invalidRange = get(base.resolve("/api/attendance?employeeId=DEMO-001&from=2026-10-15&to=2026-10-01"));
            check(invalidRange.statusCode() == 400, "reversed date range returns client error");

            HttpResponse<String> rawReport = get(base.resolve("/api/reports/attendance"));
            check(rawReport.statusCode() == 200 && rawReport.body().contains("\"hoursCalculated\":false")
                && rawReport.body().contains("\"count\":3"), "raw attendance coverage report loads without pay calculations");
            check(rawReport.body().contains("\"employeeId\":\"DEMO-001\"")
                && rawReport.body().contains("\"rawRecordCount\":3")
                && rawReport.body().contains("\"firstRecordDate\":\"2026-10-01\"")
                && rawReport.body().contains("\"lastRecordDate\":\"2026-10-15\""),
                "report returns raw row count and date bounds");
            HttpResponse<String> filteredReport = get(base.resolve("/api/reports/attendance?from=2026-10-02&to=2026-10-02"));
            check(filteredReport.statusCode() == 200 && filteredReport.body().contains("\"from\":\"2026-10-02\"")
                && filteredReport.body().contains("\"rawRecordCount\":1"), "report supports inclusive selected-date filtering");
            HttpResponse<String> badReportRange = get(base.resolve("/api/reports/attendance?from=2026-10-15&to=2026-10-01"));
            check(badReportRange.statusCode() == 400, "report rejects reversed date ranges");

            String attendanceBody = "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-10-20\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}";
            HttpResponse<String> createdAttendance = postJson(base.resolve("/api/attendance"), attendanceBody);
            check(createdAttendance.statusCode() == 201
                && createdAttendance.body().contains("\"storage\":\"local-process-memory\"")
                && createdAttendance.body().contains("\"syntheticOnly\":true")
                && createdAttendance.body().contains("\"hoursCalculated\":false"),
                "local raw attendance creation advertises ephemeral synthetic storage without calculations");
            check(createdAttendance.body().contains("\"scheduleId\":101")
                && createdAttendance.body().contains("\"dayType\":\"DEMO\"")
                && createdAttendance.body().contains("\"date\":\"2026-10-20\""),
                "server supplies existing schedule mapping and fixed DEMO label");
            HttpResponse<String> duplicateAttendance = postJson(base.resolve("/api/attendance"), attendanceBody);
            check(duplicateAttendance.statusCode() == 409, "local attendance API rejects duplicate schedule/date");
            HttpResponse<String> reversedTimes = postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-10-21\",\"timeIn\":\"18:00\",\"timeOut\":\"17:00\"}");
            check(reversedTimes.statusCode() == 400, "local attendance API rejects reversed times");
            HttpResponse<String> clientSchedule = postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"DEMO-001\",\"scheduleId\":999,\"date\":\"2026-10-22\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}");
            check(clientSchedule.statusCode() == 400, "client cannot supply a schedule assignment");
            HttpResponse<String> unknownEmployeeAttendance = postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"UNKNOWN\",\"date\":\"2026-10-22\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}");
            check(unknownEmployeeAttendance.statusCode() == 404, "attendance creation rejects an unknown employee");
        } finally {
            app.stop();
        }
    }

    private static void localModeCreatesAndUpdatesOnlySyntheticDirectoryFields() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String createBody = "{\"name\":\"Ira Demo\",\"department\":\"Operations\",\"role\":\"Coordinator\",\"location\":\"Taguig\",\"status\":\"Active\"}";
            HttpResponse<String> created = postJson(base.resolve("/api/employees"), createBody);
            check(created.statusCode() == 201, "local sandbox can create synthetic employee");
            com.google.gson.JsonObject createdItem = com.google.gson.JsonParser.parseString(created.body()).getAsJsonObject().getAsJsonObject("item");
            String id = createdItem.get("id").getAsString();
            check(id.matches("DEMO-[A-F0-9]{12}"), "employee ID is server-generated in a safe synthetic format");
            HttpResponse<String> attendance = get(base.resolve("/api/attendance?employeeId=" + id));
            check(attendance.statusCode() == 200 && attendance.body().contains("\"count\":0"),
                "new synthetic employees have no attendance until a schedule mapping is supplied");

            HttpResponse<String> injectedId = postJson(base.resolve("/api/employees"),
                "{\"id\":\"REAL-001\",\"name\":\"Fake\",\"department\":\"Ops\",\"role\":\"Role\",\"location\":\"City\",\"status\":\"Active\"}");
            check(injectedId.statusCode() == 400, "client cannot choose or inject an employee ID");
            HttpResponse<String> invalidStatus = postJson(base.resolve("/api/employees"),
                "{\"name\":\"Bad Status\",\"department\":\"Ops\",\"role\":\"Role\",\"location\":\"City\",\"status\":\"Terminated\"}");
            check(invalidStatus.statusCode() == 400, "unsupported synthetic status is rejected");

            HttpResponse<String> updated = putJson(base.resolve("/api/employees/" + id),
                "{\"name\":\"Ira Rivera\",\"department\":\"Operations\",\"role\":\"Lead\",\"location\":\"Makati\",\"status\":\"On leave\"}");
            check(updated.statusCode() == 200 && updated.body().contains("Ira Rivera"), "owner sandbox can update synthetic directory fields");
            HttpResponse<String> search = get(base.resolve("/api/employees?q=ira+rivera"));
            check(search.statusCode() == 200 && search.body().contains(id) && search.body().contains("On leave"),
                "updated synthetic employee appears in search results");
            HttpRequest delete = HttpRequest.newBuilder(base.resolve("/api/employees/" + id)).DELETE().build();
            HttpResponse<String> deleteResponse = HTTP.send(delete, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(deleteResponse.statusCode() == 405, "employee deletion is not exposed");
        } finally {
            app.stop();
        }
    }

    private static void localModeStoresScopedWorkflowRequestsWithValidatedStateChanges() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> empty = get(base.resolve("/api/workflows"));
            check(empty.statusCode() == 200 && empty.body().contains("\"leaveRequests\":[]")
                && empty.body().contains("\"tickets\":[]"), "local workflow lists start empty");

            HttpResponse<String> invalid = postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-02-30\",\"endDate\":\"2026-03-01\",\"note\":\"\"}");
            check(invalid.statusCode() == 400, "invalid leave date is rejected");

            HttpResponse<String> createdLeave = postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-11\",\"note\":\"Fictional request\"}");
            check(createdLeave.statusCode() == 201 && createdLeave.body().contains("Pending review"), "leave request is created with pending status");
            com.google.gson.JsonObject leave = com.google.gson.JsonParser.parseString(createdLeave.body()).getAsJsonObject().getAsJsonObject("item");
            String leaveId = leave.get("id").getAsString();
            check(leaveId.matches("LV-[A-F0-9]{12}"), "leave request receives a unique server-generated ID");

            HttpResponse<String> listed = get(base.resolve("/api/workflows"));
            check(listed.statusCode() == 200 && listed.body().contains(leaveId), "created leave request appears in workflow list");
            HttpResponse<String> cancelledLeave = postJson(base.resolve("/api/workflows/leave/" + leaveId + "/cancel"), "{}");
            check(cancelledLeave.statusCode() == 200 && cancelledLeave.body().contains("Cancelled"), "owner can cancel pending leave request");
            HttpResponse<String> repeatCancel = postJson(base.resolve("/api/workflows/leave/" + leaveId + "/cancel"), "{}");
            check(repeatCancel.statusCode() == 409, "cancelled leave request cannot be cancelled again");

            HttpResponse<String> createdTicket = postJson(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-002\",\"ticketType\":\"Timesheet correction\",\"summary\":\"Fictional issue\"}");
            check(createdTicket.statusCode() == 201 && createdTicket.body().contains("Team Lead"), "ticket receives its documented queue label");
            com.google.gson.JsonObject ticket = com.google.gson.JsonParser.parseString(createdTicket.body()).getAsJsonObject().getAsJsonObject("item");
            String ticketId = ticket.get("id").getAsString();
            HttpResponse<String> cancelledTicket = postJson(base.resolve("/api/workflows/tickets/" + ticketId + "/cancel"), "{}");
            check(cancelledTicket.statusCode() == 200 && cancelledTicket.body().contains("Cancelled"), "owner can cancel open ticket");
        } finally {
            app.stop();
        }
    }

    private static void firebaseModeScopesWorkflowWritesAndForwardsTheIDToken() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer()) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                HttpResponse<String> created = postJson(base.resolve("/api/workflows/leave"),
                    "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"Scoped test\"}",
                    "test-id-token", "user-77");
                check(created.statusCode() == 201 && created.body().contains("user-scoped-firestore"),
                    "authenticated workflow write reports user-scoped Firestore storage");
                check(firestore.paths.size() == 2, "workflow write reads employee and creates one request");
                check(firestore.paths.get(0).contains("/users/user-77/demoEmployees"), "employee lookup stays under token UID path");
                check(firestore.paths.get(1).contains("/users/user-77/demoLeaveRequests"), "leave write stays under token UID path");
                check(firestore.paths.stream().allMatch(path -> path.contains("/users/user-77/")), "all Firestore paths use supplied user sandbox");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "Firebase ID token forwards to each Firestore operation");
                check(firestore.requestBodies.get(1).contains("timestampValue"), "persisted request has typed submission timestamp");
            } finally {
                app.stop();
            }
        }
    }

    private static void firebaseModeScopesAttendanceReportReads() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer()) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                HttpResponse<String> report = get(base.resolve("/api/reports/attendance?from=2026-10-01&to=2026-10-15"),
                    "test-id-token", "user-77");
                check(report.statusCode() == 200 && report.body().contains("\"hoursCalculated\":false")
                    && report.body().contains("\"employeeId\":\"DEMO-001\""),
                    "authenticated raw attendance report returns user-scoped summary");
                check(firestore.paths.size() == 2
                    && firestore.paths.stream().anyMatch(path -> path.contains("/users/user-77/demoEmployees"))
                    && firestore.paths.stream().anyMatch(path -> path.contains("/users/user-77/demoAttendance")),
                    "attendance report reads only the caller's employee and attendance collections");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "attendance report forwards the Firebase ID token on each read");
            } finally {
                app.stop();
            }
        }
    }

    private static void firebaseModeScopesSyntheticDirectoryWrites() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer()) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                String body = "{\"name\":\"Ira Demo\",\"department\":\"Operations\",\"role\":\"Coordinator\",\"location\":\"Taguig\",\"status\":\"Active\"}";
                HttpResponse<String> created = postJson(base.resolve("/api/employees"), body, "test-id-token", "user-77");
                check(created.statusCode() == 201 && created.body().contains("DEMO-"), "Firebase employee create returns generated synthetic ID");
                check(firestore.paths.get(0).contains("/users/user-77/demoEmployees"), "employee create uses caller user collection");
                check("Bearer test-id-token".equals(firestore.authorizations.get(0)), "employee create forwards Firebase ID token");

                HttpResponse<String> updated = putJson(base.resolve("/api/employees/DEMO-001"),
                    "{\"name\":\"Mara Rivera Updated\",\"department\":\"People Operations\",\"role\":\"HR Coordinator\",\"location\":\"Makati\",\"status\":\"Active\"}",
                    "test-id-token", "user-77");
                check(updated.statusCode() == 200 && updated.body().contains("Mara Rivera Updated"), "Firebase directory update returns the revised fictional record: " + updated.statusCode() + " " + updated.body());
                check(firestore.paths.get(1).contains("/users/user-77/demoEmployees")
                    && firestore.paths.get(2).contains("/users/user-77/demoEmployees/DEMO-001"), "employee update lookup and patch stay user-scoped");
                check("PATCH".equals(firestore.methods.get(2)), "Firestore employee update uses PATCH");
                check(firestore.requestBodies.get(2).contains("location") && !firestore.requestBodies.get(2).contains("salary"),
                    "employee update field mask contains only allowed directory fields");
            } finally {
                app.stop();
            }
        }
    }

    private static void staticFilesExposeDefensiveHeadersAndRejectTraversal() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> root = get(base.resolve("/"));
            check(root.statusCode() == 200, "static app root is served");
            check(root.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'self'"),
                "static app root has a restrictive content security policy");
            check("nosniff".equalsIgnoreCase(root.headers().firstValue("X-Content-Type-Options").orElse("")),
                "static app root disables MIME sniffing");

            HttpResponse<String> envFile = get(base.resolve("/.env.local"));
            check(envFile.statusCode() == 404, "environment file is not served from the web root");
            HttpResponse<String> traversal = get(base.resolve("/%2e%2e/.env.local"));
            check(traversal.statusCode() == 404, "static path traversal cannot read outside the web root");
        } finally {
            app.stop();
        }
    }

    private static void firebaseModeRejectsUnauthenticatedReadsAndWritesBeforeFirestore() throws Exception {
        AppServer app = new AppServer("motorph-jd", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> health = get(base.resolve("/api/health"));
            check(health.statusCode() == 200 && health.body().contains("firebase-user-sandbox"), "Firebase mode reported");

            HttpResponse<String> employees = get(base.resolve("/api/employees"));
            check(employees.statusCode() == 401 && employees.body().contains("Sign in"), "employee read requires Firebase sign-in");
            HttpResponse<String> employeeCreate = postJson(base.resolve("/api/employees"),
                "{\"name\":\"Demo Person\",\"department\":\"Ops\",\"role\":\"Role\",\"location\":\"City\",\"status\":\"Active\"}");
            check(employeeCreate.statusCode() == 401, "employee create requires Firebase sign-in");
            HttpResponse<String> employeeUpdate = putJson(base.resolve("/api/employees/DEMO-001"),
                "{\"name\":\"Demo Person\",\"department\":\"Ops\",\"role\":\"Role\",\"location\":\"City\",\"status\":\"Active\"}");
            check(employeeUpdate.statusCode() == 401, "employee update requires Firebase sign-in");

            HttpResponse<String> attendance = get(base.resolve("/api/attendance?employeeId=DEMO-001"));
            check(attendance.statusCode() == 401, "attendance read requires Firebase sign-in");
            HttpResponse<String> attendanceWrite = postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-10-20\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}");
            check(attendanceWrite.statusCode() == 403 && attendanceWrite.body().contains("local-memory only"),
                "raw attendance entry remains blocked in Firebase mode");
            HttpResponse<String> report = get(base.resolve("/api/reports/attendance"));
            check(report.statusCode() == 401, "attendance coverage report requires Firebase sign-in");

            HttpResponse<String> workflows = get(base.resolve("/api/workflows"));
            check(workflows.statusCode() == 401, "workflow reads require Firebase sign-in");
            HttpResponse<String> workflowWrite = postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"Test\"}");
            check(workflowWrite.statusCode() == 401, "workflow writes require Firebase sign-in");

            HttpRequest seed = HttpRequest.newBuilder(base.resolve("/api/demo/seed"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> seedResponse = HTTP.send(seed, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(seedResponse.statusCode() == 401, "demo write requires Firebase sign-in");
        } finally {
            app.stop();
        }
    }

    private static void firebaseModeRejectsPartialAndMalformedSessionsBeforeFirestore() throws Exception {
        AppServer app = new AppServer("motorph-jd", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI employeesUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/employees");
            HttpRequest tokenOnly = HttpRequest.newBuilder(employeesUri).GET()
                .header("Authorization", "Bearer local-test-token").build();
            HttpResponse<String> missingUid = HTTP.send(tokenOnly, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(missingUid.statusCode() == 401, "Firebase ID token without UID header is rejected locally");

            HttpRequest uidOnly = HttpRequest.newBuilder(employeesUri).GET()
                .header("X-Firebase-UID", "synthetic-test-user").build();
            HttpResponse<String> missingToken = HTTP.send(uidOnly, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(missingToken.statusCode() == 401, "UID header without Firebase ID token is rejected locally");

            HttpRequest invalidUid = HttpRequest.newBuilder(employeesUri).GET()
                .header("Authorization", "Bearer local-test-token")
                .header("X-Firebase-UID", "invalid/user").build();
            HttpResponse<String> invalidSession = HTTP.send(invalidUid, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(invalidSession.statusCode() == 401, "malformed UID is rejected locally");
        } finally {
            app.stop();
        }
    }

    private static HttpResponse<String> get(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json").build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> get(URI uri, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(URI uri, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putJson(URI uri, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putJson(URI uri, String json, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid)
            .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(URI uri, String json, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
            .header("X-Firebase-UID", uid)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static final class FakeFirestoreServer implements AutoCloseable {
        private static final String EMPLOYEE_DOCUMENT = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoEmployees/DEMO-001\","
            + "\"fields\":{\"id\":{\"stringValue\":\"DEMO-001\"},\"name\":{\"stringValue\":\"Mara Rivera\"},"
            + "\"department\":{\"stringValue\":\"People Operations\"},\"role\":{\"stringValue\":\"HR Coordinator\"},"
            + "\"location\":{\"stringValue\":\"Makati\"},\"status\":{\"stringValue\":\"Active\"}}}";
        private final HttpServer server;
        private final List<String> paths = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();
        private final List<String> authorizations = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();

        FakeFirestoreServer() throws Exception {
            server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String apiRoot() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/projects/"; }

        private synchronized void handle(HttpExchange exchange) throws java.io.IOException {
            paths.add(exchange.getRequestURI().toString());
            methods.add(exchange.getRequestMethod());
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requestBodies.add(body);
            String path = exchange.getRequestURI().getPath();
            String response;
            int status = 200;
            if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoEmployees")) {
                response = "{\"documents\":[" + EMPLOYEE_DOCUMENT + "]}";
            } else if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoAttendance")) {
                response = "{\"documents\":[]}";
            } else if ("POST".equals(exchange.getRequestMethod()) && path.contains("/demoLeaveRequests")) {
                response = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoLeaveRequests/LV-012345ABCDEF\"}";
            } else if ("POST".equals(exchange.getRequestMethod()) && path.contains("/demoEmployees")) {
                response = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoEmployees/DEMO-FAKE000000\"}";
            } else if ("PATCH".equals(exchange.getRequestMethod()) && path.endsWith("/DEMO-001")) {
                response = EMPLOYEE_DOCUMENT;
            } else {
                status = 404;
                response = "{\"error\":{\"status\":\"NOT_FOUND\"}}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override public void close() { server.stop(0); }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
