package ph.motorph.svc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import ph.motorph.c_calc.SweldongPinoyReference;
import ph.motorph.sp_secpol.AuthService;
import ph.motorph.sp_secpol.SessionCookie;
import ph.motorph.sp_secpol.SessionStore;

/** Admin API integration test using a local simulated provider; no live payroll data is sent. */
public final class SweldongComparisonApiTest {
    private static final String VALID_RESULT = "{\"basic_pay\":\"10000.00\",\"taxable_allowance\":\"1000.00\",\"nontaxable_allowance\":\"0.00\","
        + "\"night_differential\":\"0.00\",\"overtime_pay\":\"0.00\",\"gross_pay\":\"11000.00\","
        + "\"sss_gsis_contribution\":\"500.00\",\"philhealth_contribution\":\"250.00\",\"pagibig_contribution\":\"200.00\","
        + "\"withholding_tax\":\"0.00\",\"total_deductions\":\"950.00\",\"net_pay\":10050}";

    private static final class AppFixture implements AutoCloseable {
        final HttpServer provider;
        final AtomicInteger posts = new AtomicInteger();
        volatile String form = "";
        final AppServer app;
        final URI appBase;
        final HttpClient http = HttpClient.newHttpClient();
        final String adminSession, adminCsrf, employeeSession, employeeCsrf;

        AppFixture() throws Exception {
            provider = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            provider.createContext("/", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=fixture-session; Path=/; HttpOnly");
                    byte[] body = "<html>local test fixture</html>".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } else {
                    posts.incrementAndGet();
                    form = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String body = "{\"status\":\"OK\",\"result\":" + VALID_RESULT + ",\"altresult\":" + VALID_RESULT + "}";
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
                exchange.close();
            });
            provider.start();
            app = new AppServer("", Path.of("web"), null, null);
            Field client = AppServer.class.getDeclaredField("sweldongReference");
            client.setAccessible(true);
            client.set(app, new SweldongPinoyReference(URI.create("http://127.0.0.1:" + provider.getAddress().getPort() + "/"), Duration.ofSeconds(3)));
            int appPort = app.start("127.0.0.1", 0).getAddress().getPort();
            appBase = URI.create("http://127.0.0.1:" + appPort);
            adminSession = rotateFirstPassword("10005", "Compare!Admin-2026");
            adminCsrf = SessionCookie.newCsrfToken();
            employeeSession = rotateFirstPassword("10018", "Compare!Employee-2026");
            employeeCsrf = SessionCookie.newCsrfToken();
        }

        private String rotateFirstPassword(String id, String next) throws Exception {
            Field sessionsField = AppServer.class.getDeclaredField("sessionStore");
            sessionsField.setAccessible(true);
            String initial = ((SessionStore) sessionsField.get(app)).issue(id);
            Field authField = AppServer.class.getDeclaredField("authService");
            authField.setAccessible(true);
            String rotated = ((AuthService) authField.get(app)).changePassword(initial, "MotorPH-demo-2026", next);
            if (rotated == null) throw new AssertionError("Could not prepare test account " + id);
            return rotated;
        }

        HttpResponse<String> get(String path, String session, String csrf) throws Exception {
            return http.send(HttpRequest.newBuilder(appBase.resolve(path)).header("Cookie", cookie(session, csrf)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        }

        HttpResponse<String> post(String path, JsonObject body, String session, String csrf, boolean includeCsrf) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(appBase.resolve(path)).header("Cookie", cookie(session, csrf))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString()));
            if (includeCsrf) request.header("X-CSRF-Token", csrf);
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }

        private static String cookie(String session, String csrf) {
            return "motorph_session=" + session + "; motorph_csrf=" + csrf;
        }

        public void close() {
            app.stop();
            provider.stop(0);
        }
    }

    private static JsonObject compareBody(String confirmation, String employmentType) {
        JsonObject body = new JsonObject();
        body.addProperty("confirm", confirmation);
        body.addProperty("period", "2026-11");
        body.addProperty("employmentType", employmentType);
        body.addProperty("payPeriod", "Monthly");
        body.addProperty("dailyDays", 20);
        body.addProperty("basicPay", "10,000.00");
        body.addProperty("taxableAllowance", "1,000.00");
        body.addProperty("nontaxableAllowance", "0.00");
        body.addProperty("nightDifferentialHours", "");
        body.addProperty("nightDifferentialRate", "");
        body.addProperty("overtimeHours", "");
        body.addProperty("overtimeRate", "");
        body.addProperty("workSchedule", 5);
        return body;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        try {
            run();
            System.out.println("SweldongComparisonApiTest: 18 checks passed");
        } catch (Throwable failure) {
            System.out.println("FAIL: " + failure);
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        try (AppFixture fixture = new AppFixture()) {
            HttpResponse<String> refusedEmployee = fixture.post("/api/admin/sweldong/compare",
                compareBody("SEND_TO_SWELDONG", "Private Employee"), fixture.employeeSession, fixture.employeeCsrf, true);
            check(refusedEmployee.statusCode() == 403, "comparison endpoint is admin-only");
            check(fixture.posts.get() == 0, "unauthorized request never reaches the external provider");

            HttpResponse<String> noCsrf = fixture.post("/api/admin/sweldong/compare",
                compareBody("SEND_TO_SWELDONG", "Private Employee"), fixture.adminSession, fixture.adminCsrf, false);
            check(noCsrf.statusCode() == 403, "comparison endpoint requires CSRF");
            HttpResponse<String> noConsent = fixture.post("/api/admin/sweldong/compare",
                compareBody("YES", "Private Employee"), fixture.adminSession, fixture.adminCsrf, true);
            check(noConsent.statusCode() == 400, "explicit external-transfer confirmation is mandatory");
            HttpResponse<String> unsupported = fixture.post("/api/admin/sweldong/compare",
                compareBody("SEND_TO_SWELDONG", "Government Employee"), fixture.adminSession, fixture.adminCsrf, true);
            check(unsupported.statusCode() == 400, "unsupported government/GSIS case is rejected before the request");
            check(fixture.posts.get() == 0, "ineligible inputs are never transmitted");

            HttpResponse<String> response = fixture.post("/api/admin/sweldong/compare",
                compareBody("SEND_TO_SWELDONG", "Private Employee"), fixture.adminSession, fixture.adminCsrf, true);
            check(response.statusCode() == 200, "explicit supported comparison succeeds with local provider fixture");
            JsonObject report = JsonParser.parseString(response.body()).getAsJsonObject();
            check("MATCH".equals(report.get("comparisonStatus").getAsString()), "comparison returns component match status");
            check(report.getAsJsonObject("fallback").get("eligible").getAsBoolean(), "validated result is eligible for explicit fallback");
            check("MOTORPH".equals(report.getAsJsonObject("fallback").get("selectedSource").getAsString()), "external value is not selected automatically");
            check(!report.getAsJsonObject("fallback").get("appliesToPayroll").getAsBoolean(), "reference selection cannot affect payroll");
            check(report.getAsJsonObject("sweldong").get("rawResponseText").getAsString().contains("altresult"), "raw provider JSON is returned for local evidence export");
            check(fixture.form.contains("basic_pay=10%2C000.00") && fixture.form.contains("pay_period=Monthly"), "normalized form input is encoded and preserves selected period");

            JsonObject decision = new JsonObject();
            decision.addProperty("comparisonId", report.get("comparisonId").getAsString());
            decision.addProperty("selectedSource", "SWELDONG");
            HttpResponse<String> selected = fixture.post("/api/admin/sweldong/decision", decision,
                fixture.adminSession, fixture.adminCsrf, true);
            check(selected.statusCode() == 200, "explicit fallback selection is accepted after the comparison");
            JsonObject selectedBody = JsonParser.parseString(selected.body()).getAsJsonObject();
            check("SWELDONG".equals(selectedBody.get("selectedSource").getAsString()), "decision reports the selected source");
            check(!selectedBody.get("appliesToPayroll").getAsBoolean(), "decision is a non-persisted preview selection only");
            check(fixture.get("/api/admin/audit?path=sweldong&failures=false", fixture.adminSession, fixture.adminCsrf)
                .body().contains("decision SWELDONG"), "source decision is included in the admin audit log");

            JsonObject invalidDecision = new JsonObject();
            invalidDecision.addProperty("comparisonId", "not-a-live-comparison");
            invalidDecision.addProperty("selectedSource", "SWELDONG");
            check(fixture.post("/api/admin/sweldong/decision", invalidDecision,
                fixture.adminSession, fixture.adminCsrf, true).statusCode() == 409, "unknown or expired comparison cannot be selected");
        }
    }
}
