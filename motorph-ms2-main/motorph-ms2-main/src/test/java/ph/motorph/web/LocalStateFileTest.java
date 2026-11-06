package ph.motorph.web;

import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** The local demo's data file: changes survive a restart, reset is saved too, and an unreadable file is set aside. */
public final class LocalStateFileTest {
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("motorph-data-test");
        Path file = folder.resolve("local-demo.json");

        // 1. A first server writes changes from several areas.
        AppServer first = new AppServer("", Paths.get("web"), null, file);
        HttpServer server = first.start("127.0.0.1", 0);
        URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        String hireId;
        try {
            check(get(base, "/api/health", "").contains("\"persistence\":\"file\""), "health says the demo is saved to a file");
            check(!Files.exists(file), "no file until something changes");
            check(send(base, "POST", "/api/workflows/leave", "{\"employeeId\":\"DEMO-004\",\"leaveType\":\"Personal\",\"startDate\":\"2026-12-03\",\"endDate\":\"2026-12-03\",\"note\":\"Saved leave\"}", "DEMO-004").statusCode() == 201,
                "leave request");
            HttpResponse<String> hire = send(base, "POST", "/api/hr/onboard", "{\"name\":\"Rey Tan\",\"department\":\"Finance\",\"position\":\"Payroll Associate\",\"location\":\"Pasig\",\"startDate\":\"2026-10-01\","
                + "\"supervisorId\":\"DEMO-003\",\"monthlyBasic\":\"30000\",\"riceAllowance\":\"1500\",\"phoneAllowance\":\"500\",\"clothingAllowance\":\"500\",\"payMethod\":\"Direct deposit\"}", "DEMO-001");
            check(hire.statusCode() == 201, "onboarding");
            hireId = com.google.gson.JsonParser.parseString(hire.body()).getAsJsonObject().getAsJsonObject("employee").get("id").getAsString();
            check(send(base, "POST", "/api/admin/roles/" + hireId, "{\"role\":\"PAYROLL\"}", "DEMO-005").statusCode() == 200, "role change");
            check(send(base, "PUT", "/api/government/DEMO-004", "{\"sss\":\"12-3456789-0\"}", "DEMO-005").statusCode() == 200, "government numbers (fake)");
            check(send(base, "PUT", "/api/schedules/DEMO-004", "{\"shiftStart\":\"09:00\",\"workdays\":[\"MON\",\"TUE\",\"WED\",\"THU\",\"FRI\"]}", "DEMO-003").statusCode() == 200, "schedule");
            check(send(base, "POST", "/api/payroll/payslips", "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-002").statusCode() == 201, "finalized payslip");
            check(send(base, "POST", "/api/attendance/clock", "{\"action\":\"in\"}", "DEMO-005").statusCode() == 201, "open shift");
        } finally {
            first.stop();
        }
        // stop() waits for the last save.
        check(Files.exists(file) && new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("\"format\": 1")
            || new String(Files.readAllBytes(file), StandardCharsets.UTF_8).contains("\"format\":1"), "every change is written to the file");
        check(!Files.exists(folder.resolve("local-demo.json.tmp")), "the temporary file is replaced, not left behind");

        // 2. A new server on the same file has everything back.
        AppServer second = new AppServer("", Paths.get("web"), null, file);
        AppServerIntegrationTest.SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
        server = second.start("127.0.0.1", 0);
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            check(get(base, "/api/workflows", "DEMO-004").contains("Saved leave"), "leave survives a restart");
            check(get(base, "/api/demo/accounts", "").matches("(?s).*\"employeeId\":\"" + hireId + "\",\"name\":\"Rey Tan\",\"position\":\"Payroll Associate\",[^}]*\"role\":\"PAYROLL\".*"),
                "the new hire and their changed role survive");
            check(get(base, "/api/government/DEMO-004?reveal=true", "DEMO-005").contains("\"sss\":\"12-3456789-0\""), "government numbers survive");
            check(get(base, "/api/schedules", "DEMO-003").contains("\"employeeId\":\"DEMO-004\",\"shiftStart\":\"09:00\""), "schedules survive");
            check(get(base, "/api/payroll/payslips", "DEMO-002").contains("PS-DEMO-004-2026-10-1"), "finalized payslips survive");
            check(get(base, "/api/attendance/clock", "DEMO-005").contains("\"clockedIn\":true"), "an open shift survives");
            check(get(base, "/api/payroll/cutoffs?employeeId=" + hireId + "&month=2026-10", "DEMO-002").contains("\"hourlyRate\":\"178.57\""),
                "the hire's pay setup survives (30,000 / 168 = 178.57)");
            check(get(base, "/api/government/audit", "DEMO-005").contains("\"action\":\"EDITED\""), "the audit log survives");
            check(send(base, "POST", "/api/admin/reset", "{\"confirm\":\"yes\"}", "DEMO-005").statusCode() == 400, "reset needs RESET typed");
            check(send(base, "POST", "/api/admin/reset", "{\"confirm\":\"RESET\"}", "DEMO-001").statusCode() == 403, "only the System Admin resets");
            check(send(base, "POST", "/api/admin/reset", "{\"confirm\":\"RESET\"}", "DEMO-005").statusCode() == 200, "the System Admin resets the demo");
            check(!get(base, "/api/workflows", "DEMO-004").contains("Saved leave") && !get(base, "/api/demo/accounts", "").contains(hireId), "reset restores the starting data");
        } finally {
            second.stop();
        }

        // 3. The reset itself was saved.
        AppServer third = new AppServer("", Paths.get("web"), null, file);
        AppServerIntegrationTest.SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
        server = third.start("127.0.0.1", 0);
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            check(!get(base, "/api/demo/accounts", "").contains(hireId) && get(base, "/api/demo/accounts", "").contains("DEMO-005"), "the reset survives a restart");
        } finally {
            third.stop();
        }

        // 4. An unreadable file is moved aside, never overwritten.
        Files.write(file, "{not json".getBytes(StandardCharsets.UTF_8));
        AppServer fourth = new AppServer("", Paths.get("web"), null, file);
        AppServerIntegrationTest.SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
        server = fourth.start("127.0.0.1", 0);
        base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            boolean setAside;
            try (java.util.stream.Stream<Path> files = Files.list(folder)) {
                setAside = files.anyMatch(path -> path.getFileName().toString().startsWith("local-demo.json.unreadable-"));
            }
            check(setAside && !Files.exists(file), "the unreadable file is kept aside");
            check(get(base, "/api/demo/accounts", "").contains("DEMO-005"), "and the demo starts from its built-in data");
        } finally {
            fourth.stop();
        }
        System.out.println("PASS: " + checks + " local data file checks");
    }

    private static String get(URI base, String path, String user) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(base.resolve(path)).GET().header("Accept", "application/json");
        if (!user.isEmpty()) request.header("X-Demo-User", user).header("X-Demo-Session", AppServerIntegrationTest.sessionFor(base, user));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
    }

    private static HttpResponse<String> send(URI base, String method, String path, String json, String user) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(base.resolve(path)).header("Accept", "application/json").header("Content-Type", "application/json")
            .header("X-Demo-User", user).header("X-Demo-Session", AppServerIntegrationTest.sessionFor(base, user)).method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
