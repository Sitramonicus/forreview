package ph.motorph.web;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Round-trips a payroll run through the Firestore REST adapter against a local stub: the stored
 * document must carry the ruleset version and every payslip line, and reading it back must
 * reproduce the same amounts without recalculating them.
 */
public final class FirestorePayrollStoreTest {
    private static int passed = 0;
    private static int failed = 0;
    private static final List<String> storedBodies = new ArrayList<>();
    private static String storedResponse = "";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/projects/demo/databases/(default)/documents/", FirestorePayrollStoreTest::handle);
        server.start();
        try {
            String root = "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/projects/";
            FirestoreRestClient client = new FirestoreRestClient("demo", "user-9", "token-abc", root);
            FirestorePayrollStore store = new FirestorePayrollStore(client);

            PayrollService.Input input = new PayrollService.Input();
            input.employeeId = "10015";
            input.employeeName = "Fredrick Romualdez";
            input.position = "Account Manager";
            input.department = "Accounting";
            input.payPeriod = "SEMI_MONTHLY";
            input.periodStart = "2026-01-02";
            input.periodEnd = "2026-01-15";
            input.monthlyBasic = new BigDecimal("53500");
            input.daysWorked = new BigDecimal("10");
            input.riceSubsidy = new BigDecimal("1500");
            input.phoneAllowance = new BigDecimal("2000");
            input.clothingAllowance = new BigDecimal("1000");
            Payslip slip = new PayrollService().compute(input, PayrollRuleset.forYear(2026));
            PayrollRun run = new PayrollRun("PR-20260102-TESTCASE", "SEMI_MONTHLY", "2026-01-02", "2026-01-15",
                "PH-STATUTORY-2026", "BASIC_ONLY", "FULL_MONTHLY_EACH_PERIOD",
                "2026-11-05T00:00:00Z", Collections.singletonList(slip));

            store.create(run);
            check(!storedBodies.isEmpty(), "create issued one request");
            String body = storedBodies.get(0);
            check(body.contains("\"rulesetId\":{\"stringValue\":\"PH-STATUTORY-2026\"}"), "ruleset version is stored with the run");
            check(body.contains("\"taxBasis\":{\"stringValue\":\"BASIC_ONLY\"}"), "tax assumption is stored with the run");
            check(body.contains("\"statutoryBasis\":{\"stringValue\":\"FULL_MONTHLY_EACH_PERIOD\"}"), "statutory assumption is stored");
            check(body.contains("\"takeHomePay\":\"22711.67\""), "take-home figure is stored");
            check(body.contains("\"withholdingTax\":\"5250.83\""), "withholding figure is stored");
            check(body.contains("\"arrayValue\""), "payslips are stored as an array");

            // Read-back: a stored document with the same shape must parse into equal amounts.
            storedResponse = "{\"documents\":[{\"name\":\"projects/demo/databases/(default)/documents/users/user-9/demoPayrollRuns/PR-20260102-TESTCASE\","
                + "\"fields\":{\"payPeriod\":{\"stringValue\":\"SEMI_MONTHLY\"},"
                + "\"periodStart\":{\"stringValue\":\"2026-01-02\"},\"periodEnd\":{\"stringValue\":\"2026-01-15\"},"
                + "\"rulesetId\":{\"stringValue\":\"PH-STATUTORY-2026\"},"
                + "\"taxBasis\":{\"stringValue\":\"BASIC_ONLY\"},"
                + "\"statutoryBasis\":{\"stringValue\":\"FULL_MONTHLY_EACH_PERIOD\"},"
                + "\"createdAt\":{\"stringValue\":\"2026-11-05T00:00:00Z\"},"
                + "\"payslips\":{\"arrayValue\":{\"values\":[" + PayslipJson.toJson(slip) + "]}}}}]}";
            List<PayrollRun> runs = store.findAll();
            check(runs.size() == 1, "one stored run is read back");
            PayrollRun read = runs.get(0);
            check("PR-20260102-TESTCASE".equals(read.id()), "run id comes from the document name");
            check("PH-STATUTORY-2026".equals(read.rulesetId()), "ruleset id survives the round trip");
            check(read.payslips().size() == 1, "payslip list survives the round trip");
            Payslip reread = read.payslips().get(0);
            check("22711.67".equals(reread.takeHomePay().toPlainString()), "take-home survives unchanged");
            check("5250.83".equals(reread.withholdingTax().toPlainString()), "withholding survives unchanged");
            check("26750.00".equals(reread.grossIncome().toPlainString()), "gross survives unchanged");
            check("36317.50".equals(reread.employerCost().toPlainString()), "employer cost survives unchanged");
            check("10015".equals(reread.employeeId()), "employee id survives");
        } finally {
            server.stop(0);
        }
        System.out.println("FirestorePayrollStoreTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String response;
        int status = 200;
        if ("POST".equals(method)) {
            storedBodies.add(body);
            response = "{\"name\":\"projects/demo/databases/(default)/documents/users/user-9/demoPayrollRuns/"
                + uri.getQuery().replace("documentId=", "") + "\"}";
            status = 200;
        } else {
            response = storedResponse.isEmpty() ? "{}" : storedResponse;
        }
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) { out.write(bytes); }
    }

    private static void check(boolean condition, String label) {
        if (condition) { passed++; }
        else { failed++; System.out.println("  FAIL " + label); }
    }
}
