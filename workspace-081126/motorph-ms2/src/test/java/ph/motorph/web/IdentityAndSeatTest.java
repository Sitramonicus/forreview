package ph.motorph.web;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * Identity: one account, several people, several seats — and the fact that neither is chosen by the
 * browser.
 *
 * <p>Two things are being pinned here, and the second is the one that was broken:
 *
 * <ol>
 *   <li><b>Seats.</b> A person can hold more than one, and a request names which one it acts under.
 *       Asking for a seat you do not hold is a refusal, not a default.</li>
 *   <li><b>Accounts.</b> In Firebase mode the account comes from a verified token. A request with a
 *       plausible-looking header and no verifiable token must be refused, because before this the
 *       server accepted any non-empty {@code Authorization} value as proof of sign-in.</li>
 * </ol>
 */
public final class IdentityAndSeatTest {

    private int checks;
    private int failures;
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {
        new IdentityAndSeatTest().run();
    }

    private void run() throws Exception {
        onePersonCanExerciseEveryRoleFromOneSession();
        fixtureModeSwitchesSeatWithoutSwitchingLogin();
        fixtureModeRefusesASeatNotHeld();
        hrLinksAnAccountToOneOrMorePeople();
        productionModeRequiresAVerifiableToken();
        verifierRefusesGarbageWithoutContactingAnything();
        System.out.println("PASS: " + checks + " identity and seat checks"
            + (failures == 0 ? "" : " (" + failures + " FAILED)"));
        if (failures > 0) System.exit(1);
    }

    /**
     * The answer to "I can't have five Firebase accounts just to test five roles".
     *
     * <p>One identity, all five seats, no second login and no second account.
     */
    private void onePersonCanExerciseEveryRoleFromOneSession() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String all = RoleAssignment.DEMONSTRATION_EMPLOYEE;

            JsonObject me = json(get(base, all, ""));
            equal("SYSTEM_ADMIN", me.get("role").getAsString(),
                "the demonstration person defaults to their most senior seat");
            equal(5, me.getAsJsonArray("seats").size(), "all five seats are offered by one identity");

            // Every seat resolves, and the ones that differ do differ.
            equal("HR", json(get(base, all, "HR")).get("role").getAsString(), "can act as HR");
            equal("PAYROLL", json(get(base, all, "PAYROLL")).get("role").getAsString(),
                "can act as Payroll Staff");
            equal("SUPERVISOR", json(get(base, all, "SUPERVISOR")).get("role").getAsString(),
                "can act as Supervisor");
            equal("EMPLOYEE", json(get(base, all, "EMPLOYEE")).get("role").getAsString(),
                "can act as Employee");

            // What each seat may do is not the same, which is the reason to have seats at all.
            check(get(base, all, "HR", "/api/employees").statusCode() == 200,
                "the HR seat can read the roster");
            check(get(base, all, "SYSTEM_ADMIN", "/api/payroll/rules?year=2026").statusCode() == 200,
                "the System Admin seat can read the tax tables");
            // An employee may read the rates in force (their payslip names them) but not the Annex E
            // bracket tables, which the matrix gives to System Admin alone. That is a redaction, not a
            // refusal, and the response says so rather than leaving the client to infer it.
            HttpResponse<String> employeeRules = get(base, all, "EMPLOYEE", "/api/payroll/rules?year=2026");
            check(employeeRules.statusCode() == 200,
                "an employee may read the rates in force for an auditable payslip");
            check(!employeeRules.body().contains("monthlyTaxBrackets")
                    && employeeRules.body().contains("taxTablesWithheld"),
                "the withholding bracket tables are withheld from an employee, and said to be");
            HttpResponse<String> adminRules = get(base, all, "SYSTEM_ADMIN", "/api/payroll/rules?year=2026");
            check(adminRules.body().contains("monthlyTaxBrackets"),
                "System Admin sees the bracket tables");
            check(get(base, all, "PAYROLL", "/api/payroll/runs").statusCode() == 200,
                "the Payroll seat can read payroll runs");

            // And the list the client is offered contains all five, so a UI can render a switcher.
            JsonArray(me, "seats");
            java.util.Set<String> offered = new java.util.HashSet<>();
            for (com.google.gson.JsonElement seat : me.getAsJsonArray("seats")) {
                offered.add(seat.getAsJsonObject().get("role").getAsString());
            }
            for (String role : new String[] {"SYSTEM_ADMIN", "HR", "PAYROLL", "SUPERVISOR", "EMPLOYEE"}) {
                check(offered.contains(role), "/api/me offers the " + role + " seat");
            }
        } finally {
            app.stop();
        }
    }

    /** One person, two seats, two different answers from the same login. */
    private void fixtureModeSwitchesSeatWithoutSwitchingLogin() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            // 10006 is the HR Manager and also supervises 10007.
            JsonObject me = json(get(base, "10006", ""));
            equal("HR", me.get("role").getAsString(), "with no seat named, 10006 acts as her senior seat");
            JsonArray(me, "seats");
            equal(2, me.getAsJsonArray("seats").size(), "10006 holds two seats");

            int asHr = countRows(get(base, "10006", "HR", "/api/employees"));
            int asSupervisor = countRows(get(base, "10006", "SUPERVISOR", "/api/employees"));
            equal(34, asHr, "acting as HR, the directory is the whole roster");
            equal(2, asSupervisor, "acting as Supervisor, the directory is herself plus her one report");

            // The same login, a different seat, a different answer - that is the whole point.
            check(asHr != asSupervisor, "the same person sees different data under different seats");

            // Seats survive across endpoints, not just the directory listing.
            check(get(base, "10006", "SUPERVISOR",
                "/api/attendance?employeeId=10007&from=2026-10-01&to=2026-10-15").statusCode() == 200,
                "as Supervisor, her report's attendance is readable");
            check(get(base, "10018", "EMPLOYEE",
                "/api/attendance?employeeId=10019&from=2026-10-01&to=2026-10-15").statusCode() == 403,
                "an ordinary employee still cannot read a colleague's attendance");
        } finally {
            app.stop();
        }
    }

    /** A seat is a grant, not a preference. */
    private void fixtureModeRefusesASeatNotHeld() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            HttpResponse<String> payrollSeat = get(base, "10018", "PAYROLL");
            check(payrollSeat.statusCode() == 403, "an employee cannot act as Payroll Staff");
            check(payrollSeat.body().contains("do not hold"), "the refusal says the seat is not held: "
                + payrollSeat.body());

            check(get(base, "10018", "SYSTEM_ADMIN").statusCode() == 403,
                "an employee cannot act as System Admin");
            check(get(base, "10015", "KING").statusCode() == 403,
                "an unknown seat is refused rather than ignored");

            // The employee floor is not a grant: it is what everybody is.
            check(get(base, "10018", "EMPLOYEE").statusCode() == 200,
                "naming the Employee seat always works, because everybody is one");
        } finally {
            app.stop();
        }
    }

    /**
     * How one login gets a person, and then a second one. Without this route every real account would
     * stay an unlinked applicant forever, which is why it mattered enough to build.
     */
    private void hrLinksAnAccountToOneOrMorePeople() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            URI links = base.resolve("/api/me/links");

            HttpResponse<String> first = postAs(links,
                "{\"accountId\":\"uid-hire\",\"employeeNumber\":\"10016\"}", "10006", "HR");
            equal(201, first.statusCode(), "HR links an account to an employee");
            check(first.body().contains("SUPERVISOR"),
                "the link carries the seats that employee actually holds");

            // A second person on the same account: somebody covering two jobs.
            HttpResponse<String> second = postAs(links,
                "{\"accountId\":\"uid-hire\",\"employeeNumber\":\"10017\"}", "10006", "HR");
            equal(201, second.statusCode(), "the same account can be linked to a second person");

            // Nobody may grant themselves a seat.
            HttpResponse<String> selfLink = postAs(links,
                "{\"accountId\":\"fixture-account\",\"employeeNumber\":\"10005\"}", "10006", "HR");
            equal(403, selfLink.statusCode(), "an account cannot add a link to itself");

            // Only HR may write links.
            equal(403, postAs(links,
                "{\"accountId\":\"x\",\"employeeNumber\":\"10016\"}", "10018", "EMPLOYEE").statusCode(),
                "an employee cannot link accounts");
            equal(403, postAs(links,
                "{\"accountId\":\"x\",\"employeeNumber\":\"10016\"}", "10011", "PAYROLL").statusCode(),
                "Payroll Staff cannot link accounts");

            // Bad input is a client error, not a silent no-op.
            equal(404, postAs(links,
                "{\"accountId\":\"y\",\"employeeNumber\":\"99999\"}", "10006", "HR").statusCode(),
                "linking a non-existent employee is refused");
            equal(400, postAs(links,
                "{\"accountId\":\"z\",\"employeeNumber\":\"abc\"}", "10006", "HR").statusCode(),
                "a malformed employee number is a client error");
        } finally {
            app.stop();
        }
    }

    /**
     * The regression that matters: in Firebase mode an unverified session must not resolve to anybody.
     */
    private void productionModeRequiresAVerifiableToken() throws Exception {
        // A Firebase-mode server needs an API key to construct; no network is contacted here because
        // none of the calls below present a token worth verifying.
        AppServer app = new AppServer("demo-project", Paths.get("web"), null,
            "test-api-key", "http://127.0.0.1:1/v1");
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            check(getWith(base, null, "10005", "").statusCode() == 401,
                "no token at all is 401, even with a System Admin employee number in the header");
            // A junk token against a verifier that cannot be reached must fail closed. 401 and 503 are
            // both correct answers here; 200 is not, and that is the regression this pins: the old
            // code returned 200 for any non-empty Authorization header.
            int junkToken = getWith(base, "anything", "10005", "").statusCode();
            check(junkToken != 200,
                "a token Firebase cannot verify is never accepted (got " + junkToken + ")");
            check(junkToken == 401 || junkToken == 503,
                "an unverifiable token is refused as unauthorised or unavailable, not as success");
            // And an unverifiable token must not be rescued by a plausible employee number.
            check(getWith(base, "anything", "10005", "SYSTEM_ADMIN").statusCode() != 200,
                "naming a System Admin employee number does not substitute for a token");
        } finally {
            app.stop();
        }
    }

    /** A verifier built with no API key must refuse to exist rather than verify nothing. */
    private void verifierRefusesGarbageWithoutContactingAnything() {
        expectThrows(() -> new FirebaseTokenVerifier("demo-project", "", null, null),
            "a verifier cannot be built without an API key");
        expectThrows(() -> new FirebaseTokenVerifier("", "key", null, null),
            "a verifier cannot be built without a project");
    }

    // ---------------------------------------------------------------- helpers

    private HttpResponse<String> get(URI base, String employee, String seat) throws Exception {
        return get(base, employee, seat, "/api/me");
    }

    private HttpResponse<String> get(URI base, String employee, String seat, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve(path))
            .header("Accept", "application/json").GET();
        if (employee != null) builder.header("X-MotorPH-Employee", employee);
        if (seat != null && !seat.isEmpty()) builder.header("X-MotorPH-Seat", seat);
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> postAs(URI uri, String json, String employee, String seat)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("X-MotorPH-Employee", employee)
            .header("X-MotorPH-Seat", seat)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private HttpResponse<String> getWith(URI base, String token, String employee, String seat)
            throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(base.resolve("/api/me"))
            .header("Accept", "application/json").GET();
        if (token != null) builder.header("Authorization", "Bearer " + token);
        if (employee != null) builder.header("X-MotorPH-Employee", employee);
        if (seat != null && !seat.isEmpty()) builder.header("X-MotorPH-Seat", seat);
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private int countRows(HttpResponse<String> response) {
        if (response.statusCode() != 200) return -1;
        return json(response).getAsJsonArray("items").size();
    }

    private JsonObject json(HttpResponse<String> response) {
        return com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void JsonArray(JsonObject o, String key) {
        check(o.has(key) && o.get(key).isJsonArray(), "/api/me reports " + key);
    }

    private void expectThrows(Runnable action, String message) {
        checks++;
        try {
            action.run();
            System.out.println("  FAIL " + message + ": nothing was thrown");
            failures++;
        } catch (IllegalArgumentException expected) {
            // Correct.
        }
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            System.out.println("  FAIL " + message);
            failures++;
        }
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            System.out.println("  FAIL " + message + ": expected " + expected + " but got " + actual);
            failures++;
        }
    }
}
