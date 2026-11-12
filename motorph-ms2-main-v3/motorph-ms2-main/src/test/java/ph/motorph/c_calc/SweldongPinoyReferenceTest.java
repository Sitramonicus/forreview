package ph.motorph.c_calc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Browserless form-session, validation, comparison and fallback-eligibility checks. */
public final class SweldongPinoyReferenceTest {
    private static final class FixtureSite implements AutoCloseable {
        final HttpServer server;
        String postCookie = "";
        String postBody = "";
        JsonObject result = sampleResult();
        JsonObject alternate = sampleResult();
        boolean sendAlternate = true;

        FixtureSite() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                if ("GET".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().add("Set-Cookie", "JSESSIONID=synthetic-session; Path=/; HttpOnly");
                    byte[] html = "<html>fixture homepage</html>".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, html.length);
                    exchange.getResponseBody().write(html);
                    exchange.close();
                    return;
                }
                postCookie = String.valueOf(exchange.getRequestHeaders().getFirst("Cookie"));
                postBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject response = new JsonObject();
                response.addProperty("status", "OK");
                response.add("result", result.deepCopy());
                if (sendAlternate) response.add("altresult", alternate.deepCopy());
                byte[] bytes = response.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
        }

        SweldongPinoyReference client() {
            return new SweldongPinoyReference(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"), Duration.ofSeconds(3));
        }

        public void close() { server.stop(0); }
    }

    private static JsonObject sampleResult() {
        return JsonParser.parseString("{" +
            "\"basic_pay\":\"10000.00\",\"taxable_allowance\":\"1000.00\",\"nontaxable_allowance\":\"0.00\"," +
            "\"night_differential\":\"0.00\",\"overtime_pay\":\"0.00\",\"gross_pay\":\"11000.00\"," +
            "\"sss_gsis_contribution\":\"500.00\",\"philhealth_contribution\":\"250.00\",\"pagibig_contribution\":\"200.00\"," +
            "\"withholding_tax\":\"0.00\",\"total_deductions\":\"950.00\",\"net_pay\":10050}").getAsJsonObject();
    }

    private static SweldongPinoyReference.Input validInput() {
        return SweldongPinoyReference.normalize("2026-11", "Private Employee", "Monthly", 20,
            "10,000.00", "1,000.00", "0", "", "", "", "", 5);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejects(Runnable action, String message) {
        try { action.run(); throw new AssertionError(message); }
        catch (IllegalArgumentException expected) { }
    }

    public static void main(String[] args) {
        try {
            run();
            System.out.println("SweldongPinoyReferenceTest: 31 checks passed");
        } catch (Throwable failure) {
            System.out.println("FAIL: " + failure);
            failure.printStackTrace();
            System.exit(1);
        }
    }

    private static void run() throws Exception {
        try (FixtureSite site = new FixtureSite()) {
            SweldongPinoyReference.Input input = validInput();
            JsonObject report = site.client().compare(input, StatutoryRates.DEFAULT);
            JsonObject payload = report.getAsJsonObject("requestPayload");
            JsonObject remote = report.getAsJsonObject("sweldong");
            JsonObject fallback = report.getAsJsonObject("fallback");
            check("MATCH".equals(report.get("comparisonStatus").getAsString()), "identical reference inputs compare equal");
            check(report.get("validated").getAsBoolean(), "validated output is explicitly marked");
            check(remote.get("rawResponseText").getAsString().contains("altresult"), "verbatim response text is retained");
            check(remote.get("validated").getAsBoolean(), "remote result is separately validated");
            check(remote.get("sessionCookieNames").getAsJsonArray().get(0).getAsString().equals("JSESSIONID"), "only cookie names are retained");
            check(!remote.get("rawResponseText").getAsString().contains("synthetic-session"), "session cookie value is not returned in evidence");
            check("10,000.00".equals(payload.get("basic_pay").getAsString()), "wire amount uses the captured thousands separator");
            check("Monthly".equals(payload.get("pay_period").getAsString()), "wire period is explicitly monthly");
            check(payload.size() == 11, "request preserves the captured 11-field form shape");
            check(site.postCookie.contains("JSESSIONID=synthetic-session"), "GET session cookie is reused for the browserless POST");
            Map<String, String> form = parseForm(site.postBody);
            check("10,000.00".equals(form.get("basic_pay")), "form-encoded POST carries the canonical basic pay");
            check("".equals(form.get("night_differential_hours")), "unused premiums are submitted as empty fields");
            check("5".equals(form.get("work_schedule")), "captured work schedule is included");
            check("PH-2026-MOTORPH-ESTIMATE-v5".equals(report.getAsJsonObject("motorph").get("rulesetId").getAsString()), "MotorPH ruleset provenance is attached");
            check(fallback.get("eligible").getAsBoolean(), "validated, unambiguous supported result permits explicit fallback");
            check("MOTORPH".equals(fallback.get("selectedSource").getAsString()), "MotorPH remains the default source");
            check(fallback.get("selectionRequired").getAsBoolean(), "external source is never selected automatically");
            check(!fallback.get("appliesToPayroll").getAsBoolean(), "fallback is comparison-preview only");
            check(report.getAsJsonObject("sweldong").get("providerVersion").getAsString().contains("undocumented homepage form"), "provider version is honestly marked undocumented");

            site.result.addProperty("sss_gsis_contribution", "501.00");
            site.result.addProperty("total_deductions", "951.00");
            site.result.addProperty("net_pay", "10049.00");
            site.alternate = site.result.deepCopy();
            JsonObject mismatch = site.client().compare(input, StatutoryRates.DEFAULT);
            check("MISMATCH".equals(mismatch.get("comparisonStatus").getAsString()), "component divergence is exposed as a mismatch");
            check(mismatch.getAsJsonArray("deltas").size() == 7, "all compared components remain itemized");
            check(mismatch.getAsJsonObject("fallback").get("eligible").getAsBoolean(), "mismatch does not silently pick either source");
            check("MOTORPH".equals(mismatch.getAsJsonObject("fallback").get("selectedSource").getAsString()), "mismatch still defaults to MotorPH until an explicit decision");

            site.alternate = sampleResult();
            JsonObject ambiguous = site.client().compare(input, StatutoryRates.DEFAULT);
            check(!ambiguous.getAsJsonObject("fallback").get("eligible").getAsBoolean(), "divergent altresult disables external fallback");

            site.result.addProperty("basic_pay", "9000.00");
            site.result.addProperty("gross_pay", "10000.00");
            site.result.addProperty("net_pay", "9049.00");
            site.alternate = site.result.deepCopy();
            rejectsIo(() -> site.client().compare(input, StatutoryRates.DEFAULT), "changed server input basis is rejected");
        }

        rejects(() -> SweldongPinoyReference.normalize("2026-11", "Government Employee", "Monthly", 20,
            "10000", "0", "0", "", "", "", "", 5), "government employee case is out of scope");
        rejects(() -> SweldongPinoyReference.normalize("2026-11", "Private Employee", "Semi-Monthly", 20,
            "10000", "0", "0", "", "", "", "", 5), "non-monthly period is out of scope");
        rejects(() -> SweldongPinoyReference.normalize("2026-11", "Private Employee", "Monthly", 21,
            "10000", "0", "0", "", "", "", "", 5), "unverified daily-day setting is out of scope");
        rejects(() -> SweldongPinoyReference.normalize("2026-11", "Private Employee", "Monthly", 20,
            "10000", "0", "0", "", "", "2", "30", 5), "overtime case is out of scope");
        rejects(() -> SweldongPinoyReference.normalize("2026-11", "Private Employee", "Monthly", 20,
            "10,00", "0", "0", "", "", "", "", 5), "invalid thousands grouping is rejected");
        rejects(() -> SweldongPinoyReference.normalize("2027-01", "Private Employee", "Monthly", 20,
            "10000", "0", "0", "", "", "", "", 5), "unsupported local ruleset year is rejected");
    }

    private static Map<String, String> parseForm(String encoded) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : encoded.split("&", -1)) {
            String[] parts = pair.split("=", 2);
            values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                parts.length == 1 ? "" : URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    private static void rejectsIo(ThrowingAction action, String message) {
        try { action.run(); throw new AssertionError(message); }
        catch (IOException expected) { }
        catch (Exception other) { throw new AssertionError(message, other); }
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }
}
