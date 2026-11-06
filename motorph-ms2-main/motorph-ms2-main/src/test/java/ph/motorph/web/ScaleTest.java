package ph.motorph.web;

import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Requirements Analysis, non-functional: "at least 100 concurrent employee records" and "payroll computations and
 * generation of payslip reports must not exceed more than a few seconds under standard load".
 * Onboards 120 synthetic employees through the API, gives each 5 worked days, then times payroll for all of them.
 * Hand check: 5 days x 8 h x (21,000 / 168 = 125.00) = 5,000.00 gross each.
 */
public final class ScaleTest {
    private static final int PEOPLE = 120;
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static int checks;

    public static void main(String[] args) throws Exception {
        AppServer app = new AppServer("", Paths.get("web"), null, null);
        AppServerIntegrationTest.SESSIONS.clear();
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            List<String> ids = new ArrayList<>();
            for (int i = 1; i <= PEOPLE; i++) {
                String hire = "{\"name\":\"Scale Test " + String.format("%03d", i) + "\",\"department\":\"Operations\",\"position\":\"Operations Associate\","
                    + "\"location\":\"" + (i % 2 == 0 ? "Pasig" : "Makati") + "\",\"startDate\":\"2026-10-01\",\"supervisorId\":\"DEMO-003\",\"monthlyBasic\":\"21000\","
                    + "\"riceAllowance\":\"0\",\"phoneAllowance\":\"0\",\"clothingAllowance\":\"0\",\"payMethod\":\"Check\"}";
                HttpResponse<String> made = send(base, "POST", "/api/hr/onboard", hire, "DEMO-001");
                if (made.statusCode() != 201) throw new AssertionError("onboarding failed: " + made.body());
                ids.add(JsonParser.parseString(made.body()).getAsJsonObject().getAsJsonObject("employee").get("id").getAsString());
            }
            check(ids.size() == PEOPLE, PEOPLE + " employees onboarded");
            for (String id : ids) {
                for (String day : new String[] {"2026-10-01", "2026-10-02", "2026-10-05", "2026-10-06", "2026-10-07"}) {
                    HttpResponse<String> row = send(base, "POST", "/api/attendance",
                        "{\"employeeId\":\"" + id + "\",\"date\":\"" + day + "\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003");
                    if (row.statusCode() != 201) throw new AssertionError("attendance failed: " + row.body());
                }
            }

            long start = System.nanoTime();
            HttpResponse<String> directory = send(base, "GET", "/api/employees", null, "DEMO-001");
            long directoryMs = ms(start);
            check(directory.body().contains("\"total\":" + (PEOPLE + 5)) || directory.body().contains("\"count\":" + (PEOPLE + 5)), "the directory lists all " + (PEOPLE + 5) + " people");
            check(directoryMs < 1000, "the directory loads in under a second (" + directoryMs + " ms)");

            long slowest = 0;
            start = System.nanoTime();
            for (String id : ids) {
                long one = System.nanoTime();
                HttpResponse<String> cutoffs = send(base, "GET", "/api/payroll/cutoffs?employeeId=" + id + "&month=2026-10", null, "DEMO-002");
                slowest = Math.max(slowest, ms(one));
                if (!cutoffs.body().contains("\"grossPay\":\"5000.00\"")) throw new AssertionError("unexpected pay for " + id + ": " + cutoffs.body());
            }
            long allCutoffsMs = ms(start);
            check(true, "every employee's 1-15 Oct gross is 5,000.00 (hand-worked)");
            check(slowest < 2000, "one payroll computation takes under 2 seconds (slowest " + slowest + " ms)");
            check(allCutoffsMs < 30_000, "payroll for all " + PEOPLE + " people runs in " + allCutoffsMs + " ms");

            start = System.nanoTime();
            for (String id : ids) {
                HttpResponse<String> saved = send(base, "POST", "/api/payroll/payslips", "{\"employeeId\":\"" + id + "\",\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-002");
                if (saved.statusCode() != 201) throw new AssertionError("finalize failed: " + saved.body());
            }
            long finalizeMs = ms(start);
            check(finalizeMs < 30_000, "finalizing " + PEOPLE + " payslips takes " + finalizeMs + " ms");

            start = System.nanoTime();
            HttpResponse<String> released = send(base, "POST", "/api/payroll/disbursements", "{\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-002");
            long releaseMs = ms(start);
            check(released.statusCode() == 201 && released.body().contains("\"emailsSent\":" + PEOPLE), "releasing pay emails all " + PEOPLE + " payslips");
            check(releaseMs < 5000, "the release (with " + PEOPLE + " emails and the team report) takes under 5 seconds (" + releaseMs + " ms)");

            start = System.nanoTime();
            HttpResponse<String> summary = send(base, "GET", "/api/payroll/summary?month=2026-10&by=branch", null, "DEMO-002");
            long summaryMs = ms(start);
            check(summary.body().contains("\"name\":\"Makati\",\"employees\":60") || summary.body().contains("\"employees\":60"), "the payroll summary groups them by branch: " + summary.body());
            check(summaryMs < 2000, "the payroll summary report takes under 2 seconds (" + summaryMs + " ms)");
            start = System.nanoTime();
            HttpResponse<String> pdf = send(base, "GET", "/api/payroll/summary?month=2026-10&by=branch&format=pdf", null, "DEMO-002");
            long pdfMs = ms(start);
            check(pdf.body().startsWith("%PDF-1.4") && pdf.body().contains("/Count 3"), "the PDF report for " + PEOPLE + " payslips spans 3 pages");
            check(pdfMs < 2000, "the PDF report takes under 2 seconds (" + pdfMs + " ms)");

            System.out.println("PASS: " + checks + " scale checks (" + PEOPLE + " employees: directory " + directoryMs + " ms, slowest payroll " + slowest
                + " ms, all payroll " + allCutoffsMs + " ms, finalize " + finalizeMs + " ms, release " + releaseMs + " ms, summary " + summaryMs + " ms)");
        } finally {
            app.stop();
        }
    }

    private static long ms(long since) { return (System.nanoTime() - since) / 1_000_000; }

    private static HttpResponse<String> send(URI base, String method, String path, String json, String user) throws Exception {
        URI uri = base.resolve(path);
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("X-Demo-User", user).header("X-Demo-Session", AppServerIntegrationTest.sessionFor(uri, user));
        if (json == null) request.GET();
        else request.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
        System.out.println("  ok: " + message);
    }
}
