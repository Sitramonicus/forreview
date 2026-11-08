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
        localModeComputesSavesAndReplaysPayrollOverHttp();
        localModeEnforcesTheRbacMatrixOverHttp();
        localModeRejectsAPayrollRunThatDoesNotMatchItsPeriod();
        localModeKeepsAnEarlierPayslipAtItsOwnRate();
        firebaseModeScopesPayrollRunsAndForwardsTheIDToken();
        firebaseModeScopesWorkflowWritesAndForwardsTheIDToken();
        firebaseModeScopesAttendanceReportReads();
        firebaseSeedPersistsEmployeeScheduleMappings();
        firebaseModeScopesSyntheticDirectoryWrites();
        staticFilesExposeDefensiveHeadersAndRejectTraversal();
        firebaseModeRejectsUnauthenticatedReadsAndWritesBeforeFirestore();
        firebaseModeRejectsPartialAndMalformedSessionsBeforeFirestore();
        firebaseModeVerifiesTheTokenAndRefusesAForgedIdentity();
        System.out.println("PASS: " + checks + " AppServer integration checks");
    }

    private static void localModeServesSyntheticEmployeeAndAttendanceResponses() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            HttpResponse<String> health = get(base.resolve("/api/health"));
            check(health.statusCode() == 200 && health.body().contains("local-synthetic-demo"), "local health mode reported");

            HttpResponse<String> employees = get(base.resolve("/api/employees?q=accounting"));
            check(employees.statusCode() == 200 && employees.body().contains("Fredrick"),
                "local employee search works over HTTP");

            HttpResponse<String> noMatches = get(base.resolve("/api/employees?q=not-a-person"));
            check(noMatches.statusCode() == 200 && noMatches.body().contains("\"items\":[]"), "empty employee search is a valid empty result");

            HttpResponse<String> employee = get(base.resolve("/api/employees/10001"));
            check(employee.statusCode() == 200 && employee.body().contains("\"name\":\"Manuel III Garcia\""), "employee detail returns selected record over HTTP");
            HttpResponse<String> missingEmployee = get(base.resolve("/api/employees/UNKNOWN"));
            check(missingEmployee.statusCode() == 404, "unknown employee detail returns not found");

            HttpResponse<String> attendance = get(base.resolve("/api/attendance?employeeId=10001&from=2026-10-01&to=2026-10-15"));
            check(attendance.statusCode() == 200 && attendance.body().contains("\"hoursCalculated\":false"), "local attendance stays raw");

            HttpResponse<String> oneDay = get(base.resolve("/api/attendance?employeeId=10001&from=2026-10-02&to=2026-10-02"));
            check(oneDay.statusCode() == 200 && oneDay.body().contains("\"count\":1")
                && oneDay.body().contains("\"date\":\"2026-10-02\""), "attendance date bounds include the selected day");

            HttpResponse<String> noAttendance = get(base.resolve("/api/attendance?employeeId=10002&from=2026-10-10&to=2026-10-10"));
            check(noAttendance.statusCode() == 200 && noAttendance.body().contains("\"count\":0"), "empty selected period returns an empty attendance result");

            HttpResponse<String> missingAttendanceEmployee = get(base.resolve("/api/attendance"));
            check(missingAttendanceEmployee.statusCode() == 400, "attendance query requires an employee ID");

            HttpResponse<String> badDate = get(base.resolve("/api/attendance?employeeId=10001&from=not-a-date"));
            check(badDate.statusCode() == 400, "malformed attendance date returns client error");

            HttpResponse<String> invalidRange = get(base.resolve("/api/attendance?employeeId=10001&from=2026-10-15&to=2026-10-01"));
            check(invalidRange.statusCode() == 400, "reversed date range returns client error");

            HttpResponse<String> rawReport = get(base.resolve("/api/reports/attendance"));
            check(rawReport.statusCode() == 200 && rawReport.body().contains("\"hoursCalculated\":false")
                && rawReport.body().contains("\"count\":34"), "raw attendance coverage report lists every employee without pay calculations");
            check(rawReport.body().contains("\"employeeId\":\"10001\"")
                && rawReport.body().contains("\"rawRecordCount\":3")
                && rawReport.body().contains("\"firstRecordDate\":\"2026-10-01\"")
                && rawReport.body().contains("\"lastRecordDate\":\"2026-10-15\""),
                "report returns raw row count and date bounds");
            HttpResponse<String> filteredReport = get(base.resolve("/api/reports/attendance?from=2026-10-02&to=2026-10-02"));
            check(filteredReport.statusCode() == 200 && filteredReport.body().contains("\"from\":\"2026-10-02\"")
                && filteredReport.body().contains("\"rawRecordCount\":1"), "report supports inclusive selected-date filtering");
            HttpResponse<String> badReportRange = get(base.resolve("/api/reports/attendance?from=2026-10-15&to=2026-10-01"));
            check(badReportRange.statusCode() == 400, "report rejects reversed date ranges");

            HttpResponse<String> attendanceWrite = postJson(base.resolve("/api/attendance"), "{}" );
            check(attendanceWrite.statusCode() == 405, "attendance writes are disabled until a durable schedule/identity design exists");
        } finally {
            app.stop();
        }
    }

    private static void localModeCreatesAndUpdatesOnlySyntheticDirectoryFields() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String createBody = "{\"name\":\"Iris Mariano\",\"department\":\"Operations\",\"role\":\"Coordinator\",\"location\":\"Taguig\",\"status\":\"Active\"}";
            HttpResponse<String> created = postJson(base.resolve("/api/employees"), createBody);
            check(created.statusCode() == 201, "local sandbox can create synthetic employee");
            com.google.gson.JsonObject createdItem = com.google.gson.JsonParser.parseString(created.body()).getAsJsonObject().getAsJsonObject("item");
            String id = createdItem.get("id").getAsString();
            check(id.matches("[0-9]{5}"), "employee ID continues MotorPH's five-digit numbering");
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
                "{\"employeeId\":\"10001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-02-30\",\"endDate\":\"2026-03-01\",\"note\":\"\"}");
            check(invalid.statusCode() == 400, "invalid leave date is rejected");

            HttpResponse<String> createdLeave = postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"10001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-11\",\"note\":\"Fictional request\"}");
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
                "{\"employeeId\":\"10002\",\"ticketType\":\"Timesheet correction\",\"summary\":\"Fictional issue\"}");
            check(createdTicket.statusCode() == 201 && createdTicket.body().contains("Team Lead"), "ticket receives its documented queue label");
            com.google.gson.JsonObject ticket = com.google.gson.JsonParser.parseString(createdTicket.body()).getAsJsonObject().getAsJsonObject("item");
            String ticketId = ticket.get("id").getAsString();
            HttpResponse<String> cancelledTicket = postJson(base.resolve("/api/workflows/tickets/" + ticketId + "/cancel"), "{}");
            check(cancelledTicket.statusCode() == 200 && cancelledTicket.body().contains("Cancelled"), "owner can cancel open ticket");
        } finally {
            app.stop();
        }
    }

    /** The golden case, end to end over HTTP: compute, save, list, replay, and reject bad input. */
    private static void localModeComputesSavesAndReplaysPayrollOverHttp() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            HttpResponse<String> rules = get(base.resolve("/api/payroll/rules?year=2026"));
            check(rules.statusCode() == 200 && rules.body().contains("\"sssMinMsc\":\"5000")
                && rules.body().contains("\"effectiveFrom\""), "ruleset is readable over HTTP for a covered year");
            HttpResponse<String> uncoveredYear = get(base.resolve("/api/payroll/rules?year=2019"));
            check(uncoveredYear.statusCode() == 400, "uncovered statutory year is refused instead of guessed");
            HttpResponse<String> badYear = get(base.resolve("/api/payroll/rules?year=twenty"));
            check(badYear.statusCode() == 400, "non-numeric ruleset year is a client error");

            HttpResponse<String> timesheet = get(base.resolve(
                "/api/payroll/timesheet?employeeId=10015&from=2023-12-01&to=2023-12-31"));
            check(timesheet.statusCode() == 200 && timesheet.body().contains("\"daysWorked\":19")
                && timesheet.body().contains("\"totalHours\":\"152.00\""),
                "stored timecard reduces to 19 paid days and 152.00 hours over HTTP");
            HttpResponse<String> missingTimesheetId = get(base.resolve("/api/payroll/timesheet"));
            check(missingTimesheetId.statusCode() == 400, "timesheet requires an employee ID");
            HttpResponse<String> openTimesheet = get(base.resolve("/api/payroll/timesheet?employeeId=10015"));
            check(openTimesheet.statusCode() == 400, "timesheet refuses a half-specified period");

            HttpResponse<String> preview = postJson(base.resolve("/api/payroll/preview"), goldenPayrollBody());
            check(preview.statusCode() == 200 && preview.body().contains("\"saved\":false"),
                "preview computes a payslip without persisting a run");
            com.google.gson.JsonObject slip = com.google.gson.JsonParser.parseString(preview.body())
                .getAsJsonObject().getAsJsonObject("payslip");
            check("26750.00".equals(text(slip, "grossIncome")), "preview gross matches the MotorPH payslip over HTTP");
            check("1750.00".equals(text(slip, "sssEmployee"))
                && "1337.50".equals(text(slip, "philHealthEmployee"))
                && "200.00".equals(text(slip, "pagibigEmployee")), "preview statutory lines match the payslip");
            check("5250.83".equals(text(slip, "withholdingTax")), "preview withholding tax matches the payslip");
            check("22711.67".equals(text(slip, "takeHomePay")), "preview take-home pay matches the payslip");
            check("36317.50".equals(text(slip, "employerCost")), "preview employer cost matches the payslip");
            check("PH-STATUTORY-2026".equals(text(slip, "rulesetId")), "payslip names the ruleset version it used");

            HttpResponse<String> emptyHistory = get(base.resolve("/api/payroll/runs"));
            check(emptyHistory.statusCode() == 200 && emptyHistory.body().contains("\"count\":0"),
                "run history starts empty before a run is saved");

            HttpResponse<String> saved = postJson(base.resolve("/api/payroll/runs"), goldenPayrollBody());
            check(saved.statusCode() == 201 && saved.body().contains("\"saved\":true"),
                "saving a run returns 201 with the stored snapshot");
            com.google.gson.JsonObject run = com.google.gson.JsonParser.parseString(saved.body())
                .getAsJsonObject().getAsJsonObject("run");
            String runId = text(run, "id");
            check(runId.matches("PR-20260102-[A-F0-9]{8}"), "saved run ID encodes the period in a safe format");
            check("22711.67".equals(text(run, "totalNet")) && "26750.00".equals(text(run, "totalGross"))
                && "4500.00".equals(text(run, "totalBenefits")) && "31250.00".equals(text(run, "totalEarnings")),
                "saved run totals keep gross, benefits, and earnings as distinct sums");
            check("1".equals(text(run, "employeeCount")), "saved run counts its payslips");

            HttpResponse<String> history = get(base.resolve("/api/payroll/runs"));
            com.google.gson.JsonObject listed = com.google.gson.JsonParser.parseString(history.body()).getAsJsonObject();
            check(history.statusCode() == 200 && listed.get("count").getAsInt() == 1,
                "saved run appears exactly once in history");
            check(!listed.getAsJsonArray("items").get(0).getAsJsonObject().has("payslips"),
                "run history stays a summary without payslip bodies");

            HttpResponse<String> replay = get(base.resolve("/api/payroll/runs/" + runId));
            check(replay.statusCode() == 200 && replay.body().contains("\"takeHomePay\":\"22711.67\"")
                && replay.body().contains("\"rulesetId\":\"PH-STATUTORY-2026\""),
                "saved run replays stored payslip amounts without recomputing them");
            HttpResponse<String> missingRun = get(base.resolve("/api/payroll/runs/PR-20260102-00000000"));
            check(missingRun.statusCode() == 404, "unknown payroll run returns not found");
            HttpResponse<String> nestedRunPath = get(base.resolve("/api/payroll/runs/a/b"));
            check(nestedRunPath.statusCode() == 404, "payroll run path traversal is refused");

            HttpResponse<String> unknownField = postJson(base.resolve("/api/payroll/preview"),
                goldenPayrollBody().replace("\"daysWorked\":10", "\"daysWorked\":10,\"bonus\":100"));
            check(unknownField.statusCode() == 400, "unknown payroll field is rejected instead of silently ignored");
            HttpResponse<String> tooManyDays = postJson(base.resolve("/api/payroll/preview"),
                goldenPayrollBody().replace("\"daysWorked\":10", "\"daysWorked\":11"));
            check(tooManyDays.statusCode() == 400, "semi-monthly period rejects more than ten days worked");
            HttpResponse<String> reversed = postJson(base.resolve("/api/payroll/preview"),
                goldenPayrollBody().replace("\"periodEnd\":\"2026-01-15\"", "\"periodEnd\":\"2026-01-01\""));
            check(reversed.statusCode() == 400, "reversed payroll period is rejected");
            HttpResponse<String> uncoveredPeriod = postJson(base.resolve("/api/payroll/preview"),
                goldenPayrollBody().replace("2026-01-02", "2019-01-02"));
            check(uncoveredPeriod.statusCode() == 400, "period outside a loaded ruleset year is refused");
            HttpResponse<String> wrongContentType = HTTP.send(HttpRequest.newBuilder(base.resolve("/api/payroll/preview"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(goldenPayrollBody(), StandardCharsets.UTF_8)).build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(wrongContentType.statusCode() == 415, "payroll writes require a JSON content type");
            HttpResponse<String> delete = HTTP.send(HttpRequest.newBuilder(base.resolve("/api/payroll/runs/" + runId))
                .DELETE().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(delete.statusCode() == 405, "saved payroll runs are immutable and cannot be deleted");

            HttpResponse<String> historyAfterRejects = get(base.resolve("/api/payroll/runs"));
            check(historyAfterRejects.body().contains("\"count\":1"), "rejected payroll requests never create a run");

            // Reference comparison is optional and must degrade gracefully: with the endpoint pointing
            // at a dead port the payslip figures are still returned and nothing is saved.
            HttpResponse<String> compareDown = postJson(base.resolve("/api/payroll/compare"),
                goldenPayrollBody().replace("\"payPeriod\":\"SEMI_MONTHLY\"", "\"payPeriod\":\"MONTHLY\""));
            check(compareDown.statusCode() == 200, "comparison route answers even when the reference is unreachable");
            com.google.gson.JsonObject comparison = com.google.gson.JsonParser.parseString(compareDown.body()).getAsJsonObject();
            check(comparison.has("ours") && "22711.67".equals(text(comparison.getAsJsonObject("ours"), "netPay")),
                "comparison always returns our own payslip figures");
            check(!comparison.get("available").getAsBoolean() && comparison.has("reason"),
                "unreachable reference is reported as unavailable with a reason, not as an error");
            check(comparison.get("differences").getAsJsonArray().size() == 0,
                "no differences are invented when the reference is unavailable");

            // A part-period cannot be compared with a whole-month reference. The route must say so
            // instead of printing a delta that only reflects the different basis.
            com.google.gson.JsonObject semiMonthly = com.google.gson.JsonParser
                .parseString(postJson(base.resolve("/api/payroll/compare"), goldenPayrollBody())
                    .body()).getAsJsonObject();
            check(!semiMonthly.get("available").getAsBoolean(), "semi-monthly comparison is declined");
            check(text(semiMonthly, "reason").contains("whole month"),
                "semi-monthly refusal explains the basis difference: " + text(semiMonthly, "reason"));
            check("22711.67".equals(text(semiMonthly.getAsJsonObject("ours"), "netPay")),
                "the declined comparison still returns our own payslip figure");

            com.google.gson.JsonObject partialMonth = com.google.gson.JsonParser.parseString(postJson(
                base.resolve("/api/payroll/compare"),
                goldenPayrollBody().replace("\"payPeriod\":\"SEMI_MONTHLY\"", "\"payPeriod\":\"MONTHLY\"")
                    .replace("\"daysWorked\":10", "\"daysWorked\":10")).body()).getAsJsonObject();
            check(!partialMonth.get("available").getAsBoolean(),
                "a 10-day month is not compared against the reference's full month");
            check(text(partialMonth, "reason").contains("20-day month"),
                "partial-month refusal names the full month it would need: " + text(partialMonth, "reason"));
        } finally {
            app.stop();
        }
    }

    /** The 12x5 matrix, exercised over HTTP with real employee numbers from MotorPH's file. */
    private static void localModeEnforcesTheRbacMatrixOverHttp() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            // Identity comes from the linked employee number, and the role comes from the server.
            com.google.gson.JsonObject me = com.google.gson.JsonParser
                .parseString(getAs(base.resolve("/api/me"), "10015").body()).getAsJsonObject();
            check("SUPERVISOR".equals(me.get("role").getAsString()), "10015 resolves to Supervisor");
            check(me.getAsJsonArray("directReports").size() == 2,
                "the supervisor's reports come from the Immediate Supervisor column");
            com.google.gson.JsonObject attendanceCell = me.getAsJsonObject("permissions")
                .getAsJsonObject("ATTENDANCE");
            check(attendanceCell.get("teamScoped").getAsBoolean()
                && attendanceCell.get("update").getAsBoolean()
                && !attendanceCell.get("create").getAsBoolean(),
                "Supervisor x Attendance is read+update, team-scoped, no create");
            com.google.gson.JsonObject taxCell = me.getAsJsonObject("permissions").getAsJsonObject("TAX_RATES");
            check(!taxCell.get("read").getAsBoolean(), "Supervisor cannot reach the tax table");

            // Directory reads: self, direct reports, and nothing else.
            check(getAs(base.resolve("/api/employees/10016"), "10015").statusCode() == 200,
                "a supervisor reads a direct report's record");
            check(getAs(base.resolve("/api/employees/10001"), "10015").statusCode() == 403,
                "a supervisor cannot read the CEO's record");
            check(getAs(base.resolve("/api/employees/10018"), "10018").statusCode() == 200,
                "an employee reads their own record");
            check(getAs(base.resolve("/api/employees/10019"), "10018").statusCode() == 403,
                "an employee cannot read a colleague's record");

            // The list is filtered, so a reply can never contain a row the caller may not read.
            com.google.gson.JsonObject employeeList = com.google.gson.JsonParser
                .parseString(getAs(base.resolve("/api/employees"), "10018").body()).getAsJsonObject();
            check(employeeList.getAsJsonArray("items").size() == 1,
                "an employee's directory listing contains only themselves");
            com.google.gson.JsonObject supervisorList = com.google.gson.JsonParser
                .parseString(getAs(base.resolve("/api/employees"), "10015").body()).getAsJsonObject();
            check(supervisorList.getAsJsonArray("items").size() == 3,
                "a supervisor's listing is themselves plus two reports");
            com.google.gson.JsonObject hrList = com.google.gson.JsonParser
                .parseString(getAs(base.resolve("/api/employees"), "10006").body()).getAsJsonObject();
            check(hrList.getAsJsonArray("items").size() == 34, "HR sees the whole roster");

            // Writes: HR may create, Payroll may not, and nobody may delete.
            check(postAs(base.resolve("/api/employees"),
                "{\"name\":\"RBAC Hire\",\"department\":\"QA\",\"role\":\"Tester\","
                    + "\"location\":\"Taguig\",\"status\":\"Active\"}", "10006").statusCode() == 201,
                "HR creates an employee record");
            check(postAs(base.resolve("/api/employees"),
                "{\"name\":\"Nope\",\"department\":\"QA\",\"role\":\"Tester\","
                    + "\"location\":\"Taguig\",\"status\":\"Active\"}", "10011").statusCode() == 403,
                "Payroll Staff cannot create employees");
            check(putAs(base.resolve("/api/employees/10018"),
                "{\"name\":\"X\",\"department\":\"D\",\"role\":\"R\",\"location\":\"L\","
                    + "\"status\":\"Active\"}", "10018").statusCode() == 403,
                "an employee cannot edit their own directory record");
            check(putAs(base.resolve("/api/employees/10001"),
                "{\"name\":\"X\",\"department\":\"D\",\"role\":\"R\",\"location\":\"L\","
                    + "\"status\":\"Active\"}", "10011").statusCode() == 403,
                "Payroll Staff cannot edit an employee record");

            // Attendance: R/U team, R self, R company-wide, never cross-team.
            check(getAs(base.resolve("/api/attendance?employeeId=10016&from=2026-10-01&to=2026-10-15"),
                "10015").statusCode() == 200, "a supervisor reads a report's attendance");
            check(getAs(base.resolve("/api/attendance?employeeId=10019&from=2026-10-01&to=2026-10-15"),
                "10015").statusCode() == 403, "a supervisor cannot read a non-report's attendance");
            check(getAs(base.resolve("/api/attendance?employeeId=10019&from=2026-10-01&to=2026-10-15"),
                "10011").statusCode() == 200, "Payroll reads any employee's attendance");

            // A signed-in account HR has not linked to a person can read nothing. It is admitted so it
            // can ask for access, but every matrix cell is NONE, so its directory is empty rather than
            // refused - a 403 would tell an attacker the roster exists.
            HttpResponse<String> unlinked = getWithNoIdentity(base.resolve("/api/employees"));
            check(unlinked.statusCode() == 200 && unlinked.body().contains("\"items\":[]")
                    && unlinked.body().contains("\"total\":0"),
                "an unlinked account's directory is empty and reveals no roster size: " + unlinked.body());
            com.google.gson.JsonObject unlinkedMe = com.google.gson.JsonParser
                .parseString(getWithNoIdentity(base.resolve("/api/me")).body()).getAsJsonObject();
            check("APPLICANT".equals(text(unlinkedMe, "role")),
                "an unlinked account is reported as an unlinked account, not as an employee");
            check(!unlinkedMe.getAsJsonObject("permissions").getAsJsonObject("PAYSLIP")
                    .get("read").getAsBoolean(),
                "an unlinked account cannot read a payslip");
            check(getWithNoIdentity(base.resolve("/api/employees/10001")).statusCode() == 403,
                "an unlinked account cannot open any employee record");
            check(getWithNoIdentity(base.resolve("/api/payroll/runs")).statusCode() == 403,
                "an unlinked account cannot read payroll runs");
        } finally {
            app.stop();
        }
    }

    /** A run must belong to a payroll cycle, and a closed cycle accepts nothing. */
    private static void localModeRejectsAPayrollRunThatDoesNotMatchItsPeriod() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            HttpResponse<String> created = postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\","
                    + "\"periodEnd\":\"2026-01-15\"}", "10011");
            check(created.statusCode() == 201, "Payroll Staff opens a payroll period");
            check(created.body().contains("PP-S-2026-01-01-2026-01-15"), "the period gets a dated ID");

            // A period that does not match its cadence is refused outright.
            check(postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-02-16\","
                    + "\"periodEnd\":\"2026-02-30\"}", "10011").statusCode() == 400,
                "an impossible date is refused");
            check(postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"MONTHLY\",\"periodStart\":\"2026-01-02\","
                    + "\"periodEnd\":\"2026-01-31\"}", "10011").statusCode() == 400,
                "a monthly period must start on the 1st");

            // The same period twice is a conflict, not a duplicate.
            check(postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\","
                    + "\"periodEnd\":\"2026-01-15\"}", "10011").statusCode() == 409,
                "opening the same period twice is refused");

            // A run whose dates disagree with the period it names is refused.
            HttpResponse<String> mismatched = postAs(base.resolve("/api/payroll/runs"),
                "{\"employeeId\":\"10015\",\"payrollPeriodId\":\"PP-S-2026-01-01-2026-01-15\","
                    + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-16\","
                    + "\"periodEnd\":\"2026-01-31\",\"monthlyBasic\":\"53500\",\"daysWorked\":10,"
                    + "\"taxBasis\":\"BASIC_ONLY\",\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}",
                "10011");
            check(mismatched.statusCode() == 400 && mismatched.body().contains("does not match payroll period"),
                "a run that disagrees with its period is refused: " + mismatched.body());

            // A matching run is accepted and carries the period's dates.
            HttpResponse<String> matched = postAs(base.resolve("/api/payroll/runs"),
                "{\"employeeId\":\"10015\",\"payrollPeriodId\":\"PP-S-2026-01-01-2026-01-15\","
                    + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\","
                    + "\"periodEnd\":\"2026-01-15\",\"monthlyBasic\":\"53500\",\"daysWorked\":10,"
                    + "\"taxBasis\":\"BASIC_ONLY\",\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}",
                "10011");
            check(matched.statusCode() == 201, "a matching run is saved: " + matched.body());

            // Closing the period stops further runs for everybody.
            check(postAs(base.resolve("/api/payroll/periods/PP-S-2026-01-01-2026-01-15/close"),
                "{}", "10011").statusCode() == 200, "Payroll Staff closes the period");
            HttpResponse<String> afterClose = postAs(base.resolve("/api/payroll/runs"),
                "{\"employeeId\":\"10016\",\"payrollPeriodId\":\"PP-S-2026-01-01-2026-01-15\","
                    + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\","
                    + "\"periodEnd\":\"2026-01-15\",\"monthlyBasic\":\"22500\",\"daysWorked\":10,"
                    + "\"taxBasis\":\"BASIC_ONLY\",\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}",
                "10011");
            check(afterClose.statusCode() == 409 && afterClose.body().contains("closed"),
                "a closed period refuses new runs: " + afterClose.body());

            // Salary history answers with the opening rate from the employee file.
            com.google.gson.JsonObject history = com.google.gson.JsonParser.parseString(
                getAs(base.resolve("/api/payroll/salary-history?employeeId=10015"), "10011").body())
                .getAsJsonObject();
            check(history.get("count").getAsInt() == 1, "10015 has one dated rate from the employee file");
            check(history.getAsJsonArray("items").get(0).getAsJsonObject()
                .get("monthlyBasic").getAsString().equals("53500.00"), "the opening rate is the CSV's 53,500");
        } finally {
            app.stop();
        }
    }

    /** A raise must not rewrite an earlier payslip, and only Payroll may record one. */
    private static void localModeKeepsAnEarlierPayslipAtItsOwnRate() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

            // Everybody has an opening rate from the employee file: 34 people, 34 dated rows.
            com.google.gson.JsonObject opening = com.google.gson.JsonParser.parseString(
                getAs(base.resolve("/api/payroll/salary-history"), "10011").body()).getAsJsonObject();
            check(opening.get("count").getAsInt() == 34,
                "the salary history opens with one dated rate for every employee");
            com.google.gson.JsonObject own = com.google.gson.JsonParser.parseString(
                getAs(base.resolve("/api/payroll/salary-history"), "10018").body()).getAsJsonObject();
            check(own.get("count").getAsInt() == 1
                    && own.getAsJsonArray("items").get(0).getAsJsonObject()
                        .get("employeeId").getAsString().equals("10018"),
                "an employee sees only their own rate history");

            // Only Payroll may record a rate. HR updates the employee record, not the payroll.
            String raise = "{\"employeeId\":\"10015\",\"effectiveFrom\":\"2026-07-01\","
                + "\"monthlyBasic\":\"60000\",\"riceSubsidy\":\"1500\",\"phoneAllowance\":\"1000\","
                + "\"clothingAllowance\":\"1000\",\"reason\":\"Promotion\"}";
            check(postAs(base.resolve("/api/payroll/salary-history"), raise, "10006").statusCode() == 403,
                "HR cannot write the payroll salary history");
            check(postAs(base.resolve("/api/payroll/salary-history"), raise, "10018").statusCode() == 403,
                "an employee cannot raise their own pay");
            check(postAs(base.resolve("/api/payroll/salary-history"), raise, "10011").statusCode() == 201,
                "Payroll Staff records the raise");

            // A rate must start a cycle, and two rates cannot share a date.
            check(postAs(base.resolve("/api/payroll/salary-history"),
                raise.replace("2026-07-01", "2026-07-15"), "10011").statusCode() == 400,
                "a mid-month effective date is refused");
            check(postAs(base.resolve("/api/payroll/salary-history"),
                raise.replace("\"60000\"", "\"61000\""), "10011").statusCode() == 400,
                "a second rate for the same effective date is refused");

            // January is over: the raise cannot touch it.
            check(postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\","
                    + "\"periodEnd\":\"2026-01-15\"}", "10011").statusCode() == 201, "January cycle opens");
            check(postAs(base.resolve("/api/payroll/periods"),
                "{\"cadence\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-08-01\","
                    + "\"periodEnd\":\"2026-08-15\"}", "10011").statusCode() == 201, "August cycle opens");

            String january = "{\"employeeId\":\"10015\",\"payrollPeriodId\":\"PP-S-2026-01-01-2026-01-15\","
                + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-01\",\"periodEnd\":\"2026-01-15\","
                + "\"monthlyBasic\":\"%s\",\"daysWorked\":10,\"taxBasis\":\"BASIC_ONLY\","
                + "\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}";
            HttpResponse<String> janAtRaisedRate = postAs(base.resolve("/api/payroll/runs"),
                String.format(january, "60000"), "10011");
            check(janAtRaisedRate.statusCode() == 400
                    && janAtRaisedRate.body().contains("53500.00 was in force"),
                "January refuses the raised rate: " + janAtRaisedRate.body());
            HttpResponse<String> janAtOwnRate = postAs(base.resolve("/api/payroll/runs"),
                String.format(january, "53500"), "10011");
            check(janAtOwnRate.statusCode() == 201
                    && janAtOwnRate.body().contains("\"takeHomePay\":\"21711.67\""),
                "January still pays the rate that was in force in January: " + janAtOwnRate.body());

            // August pays the new one.
            HttpResponse<String> august = postAs(base.resolve("/api/payroll/runs"),
                "{\"employeeId\":\"10015\",\"payrollPeriodId\":\"PP-S-2026-08-01-2026-08-15\","
                    + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-08-01\","
                    + "\"periodEnd\":\"2026-08-15\",\"monthlyBasic\":\"60000\",\"daysWorked\":10,"
                    + "\"taxBasis\":\"BASIC_ONLY\",\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}",
                "10011");
            check(august.statusCode() == 201 && august.body().contains("\"monthlyBasic\":\"60000.00\""),
                "August pays the raised rate");
        } finally {
            app.stop();
        }
    }

    /** Payroll in Firebase mode must stay inside the token's user sandbox and never trust the client. */
    private static void firebaseModeScopesPayrollRunsAndForwardsTheIDToken() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

                HttpResponse<String> created = postJson(base.resolve("/api/payroll/runs"), goldenPayrollBody(),
                    "test-id-token", "user-77");
                check(created.statusCode() == 201, "authenticated payroll run is saved through Firestore");
                check(created.body().contains("\"takeHomePay\":\"22711.67\""),
                    "Firebase-mode save returns the computed payslip");

                String writePath = firestore.paths.stream()
                    .filter(path -> path.contains("/demoPayrollRuns")).findFirst().orElse("");
                check(writePath.contains("/users/user-77/demoPayrollRuns")
                    && writePath.contains("documentId=PR-20260102-"),
                    "payroll run document is written under the token UID path");
                check(firestore.paths.stream().allMatch(path -> path.contains("/users/user-77/")),
                    "every payroll Firestore call uses the supplied user sandbox");
                String body = firestore.requestBodies.get(firestore.requestBodies.size() - 1);
                check(body.contains("\"rulesetId\"") && body.contains("\"taxBasis\"")
                    && body.contains("\"statutoryBasis\""), "stored run carries its calculation assumptions");
                check(body.contains("\"arrayValue\"") && body.contains("\"takeHomePay\""),
                    "stored run carries every payslip line as an array value");
                check(body.contains("22711.67") && body.contains("5250.83"),
                    "stored run persists the MotorPH-matching amounts, not just the inputs");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "Firebase ID token forwards to each payroll Firestore call");

                HttpResponse<String> history = get(base.resolve("/api/payroll/runs"), "test-id-token", "user-77");
                check(history.statusCode() == 200, "payroll history reads from Firestore");
                String listPath = firestore.paths.get(firestore.paths.size() - 1);
                check(listPath.contains("/users/user-77/demoPayrollRuns"),
                    "payroll history is scoped to the token UID collection");

                HttpResponse<String> anonymousSave = postJson(base.resolve("/api/payroll/runs"), goldenPayrollBody());
                check(anonymousSave.statusCode() == 401, "payroll cannot be saved without a Firebase session");
                HttpResponse<String> anonymousList = get(base.resolve("/api/payroll/runs"));
                check(anonymousList.statusCode() == 401, "payroll history cannot be read without a Firebase session");
                // The statutory ruleset used to be readable with no session at all. It carries the
                // withholding tables the matrix reserves for System Admin, so it is no longer public:
                // an anonymous caller must be refused rather than served the tax tables.
                HttpResponse<String> anonymousRules = get(base.resolve("/api/payroll/rules?year=2026"));
                check(anonymousRules.statusCode() == 401,
                    "the statutory ruleset is not readable without a session (got "
                        + anonymousRules.statusCode() + ")");
            } finally {
                app.stop();
            }
        }
    }

    private static void firebaseModeScopesWorkflowWritesAndForwardsTheIDToken() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                HttpResponse<String> created = postJson(base.resolve("/api/workflows/leave"),
                    "{\"employeeId\":\"10001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"Scoped test\"}",
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
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                HttpResponse<String> report = get(base.resolve("/api/reports/attendance?from=2026-10-01&to=2026-10-15"),
                    "test-id-token", "user-77");
                check(report.statusCode() == 200 && report.body().contains("\"hoursCalculated\":false")
                    && report.body().contains("\"employeeId\":\"10001\""),
                    "authenticated raw attendance report returns user-scoped summary");
                check(report.body().contains("\"scheduleId\":101") && report.body().contains("\"hasScheduleMapping\":true"),
                    "attendance attribution uses the persisted owner-scoped schedule mapping");
                check(firestore.paths.size() == 3
                    && firestore.paths.stream().anyMatch(path -> path.contains("/users/user-77/demoEmployees"))
                    && firestore.paths.stream().anyMatch(path -> path.contains("/users/user-77/demoEmployeeSchedules"))
                    && firestore.paths.stream().anyMatch(path -> path.contains("/users/user-77/demoAttendance")),
                    "attendance report reads employees, schedule mappings, and attendance only from the caller's UID path");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "attendance report forwards the Firebase ID token on each read");
            } finally {
                app.stop();
            }
        }
    }


    private static void firebaseSeedPersistsEmployeeScheduleMappings() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI seedUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/demo/seed");
                HttpRequest seed = HttpRequest.newBuilder(seedUri).POST(HttpRequest.BodyPublishers.noBody())
                    .header("Authorization", "Bearer test-id-token").header("X-Firebase-UID", "user-77").build();
                HttpResponse<String> response = HTTP.send(seed, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                check(response.statusCode() == 200 && response.body().contains("\"employeesCreated\":34")
                    && response.body().contains("\"scheduleMappingsCreated\":4")
                    && response.body().contains("\"attendanceCreated\":28"),
                    "seed copies the whole 34-person roster into the signed-in sandbox");
                check("GET".equals(firestore.methods.get(0)) && firestore.paths.get(0).contains("/demoEmployeeSchedules"),
                    "seed preflights the new owner-scoped rules path before writing any collection");
                check(java.util.stream.IntStream.range(0, firestore.paths.size()).filter(i -> "POST".equals(firestore.methods.get(i))
                    && firestore.paths.get(i).contains("/demoEmployeeSchedules")).count() == 4,
                    "seed persists each existing synthetic mapping under the caller UID");
                check(firestore.requestBodies.stream().anyMatch(body -> body.contains("\"employeeId\":{\"stringValue\":\"10001\"}")
                    && body.contains("\"scheduleId\":{\"integerValue\":\"101\"}")),
                    "stored schedule-link document preserves the pre-existing fixture mapping as data");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "seed forwards the caller Firebase ID token to every Firestore write");
            } finally {
                app.stop();
            }
        }
    }

    private static void firebaseModeScopesSyntheticDirectoryWrites() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                String body = "{\"name\":\"Iris Mariano\",\"department\":\"Operations\",\"role\":\"Coordinator\",\"location\":\"Taguig\",\"status\":\"Active\"}";
                HttpResponse<String> created = postJson(base.resolve("/api/employees"), body, "test-id-token", "user-77");
                check(created.statusCode() == 201 && created.body().matches("(?s).*\"id\":\"[0-9]{5}\".*"),
                    "Firebase employee create returns a five-digit employee number");
                check(firestore.paths.get(0).contains("/users/user-77/demoEmployees"), "employee create uses caller user collection");
                check("Bearer test-id-token".equals(firestore.authorizations.get(0)), "employee create forwards Firebase ID token");

                HttpResponse<String> updated = putJson(base.resolve("/api/employees/10001"),
                    "{\"name\":\"Manuel III Garcia Updated\",\"department\":\"People Operations\",\"role\":\"HR Coordinator\",\"location\":\"Makati\",\"status\":\"Active\"}",
                    "test-id-token", "user-77");
                check(updated.statusCode() == 200 && updated.body().contains("Manuel III Garcia Updated"), "Firebase directory update returns the revised fictional record: " + updated.statusCode() + " " + updated.body());
                int patchIndex = firestore.methods.indexOf("PATCH");
                check(patchIndex > 0
                    && firestore.paths.get(patchIndex - 1).contains("/users/user-77/demoEmployees")
                    && firestore.paths.get(patchIndex).contains("/users/user-77/demoEmployees/10001"),
                    "employee update lookup and patch stay user-scoped: " + firestore.paths);
                check(firestore.requestBodies.get(patchIndex).contains("location")
                    && !firestore.requestBodies.get(patchIndex).contains("salary"),
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
        // No Firestore and no reachable verifier: these checks are about what happens *before* any
        // account is known, which must be a refusal rather than an attempt to read something.
        AppServer app = new AppServer("motorph-jd", Paths.get("web"), null,
            "test-api-key", "http://127.0.0.1:1/v1");
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
            HttpResponse<String> employeeUpdate = putJson(base.resolve("/api/employees/10001"),
                "{\"name\":\"Demo Person\",\"department\":\"Ops\",\"role\":\"Role\",\"location\":\"City\",\"status\":\"Active\"}");
            check(employeeUpdate.statusCode() == 401, "employee update requires Firebase sign-in");

            HttpResponse<String> attendance = get(base.resolve("/api/attendance?employeeId=10001"));
            check(attendance.statusCode() == 401, "attendance read requires Firebase sign-in");
            HttpResponse<String> attendanceWrite = postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"10001\",\"date\":\"2026-10-20\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}");
            check(attendanceWrite.statusCode() == 405,
                "raw attendance write route is not implemented in any runtime mode");
            HttpResponse<String> report = get(base.resolve("/api/reports/attendance"));
            check(report.statusCode() == 401, "attendance coverage report requires Firebase sign-in");

            HttpResponse<String> workflows = get(base.resolve("/api/workflows"));
            check(workflows.statusCode() == 401, "workflow reads require Firebase sign-in");
            HttpResponse<String> workflowWrite = postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"10001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"Test\"}");
            check(workflowWrite.statusCode() == 401, "workflow writes require Firebase sign-in");

            HttpRequest seed = HttpRequest.newBuilder(base.resolve("/api/demo/seed"))
                .POST(HttpRequest.BodyPublishers.noBody()).build();
            HttpResponse<String> seedResponse = HTTP.send(seed, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            check(seedResponse.statusCode() == 401, "demo write requires Firebase sign-in");
        } finally {
            app.stop();
        }
    }

    /**
     * Partial and malformed sessions, now that identity is actually verified.
     *
     * <p>The earlier version of this check asserted that a tokenless request was refused "locally,
     * before Firestore". It was, but for the wrong reason: the server treated any non-empty
     * {@code Authorization} value as a signed-in account, so a malformed session was refused only
     * because it lacked a UID header. With verification in place the order is: is there a token, is it
     * verifiable, and does the account match what the request claims. These checks pin all three.
     */
    private static void firebaseModeRejectsPartialAndMalformedSessionsBeforeFirestore() throws Exception {
        AppServer app = new AppServer("motorph-jd", Paths.get("web"), null,
            "test-api-key", "http://127.0.0.1:1/v1");
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI employeesUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/api/employees");

            // No token: refused before anything else is considered.
            check(HTTP.send(HttpRequest.newBuilder(employeesUri).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode() == 401,
                "no token is refused with 401");

            // A UID header is not a session. It used to be half of one.
            check(HTTP.send(HttpRequest.newBuilder(employeesUri).GET()
                    .header("X-Firebase-UID", "synthetic-test-user").build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode() == 401,
                "a UID header without a token is refused");

            // A token the verifier cannot validate is refused, whatever else the request claims.
            int unverifiable = HTTP.send(HttpRequest.newBuilder(employeesUri).GET()
                .header("Authorization", "Bearer local-test-token")
                .header("X-Firebase-UID", "user-77").build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();
            check(unverifiable != 200,
                "an unverifiable token is never accepted, even with a matching UID (got " + unverifiable + ")");
        } finally {
            app.stop();
        }
    }

    /**
     * The two claims that make the role model real, tested against a verifier that can say no:
     *
     * <ol>
     *   <li>The token is actually verified - the verifier is asked, and its answer is what counts.</li>
     *   <li>A verified account cannot borrow somebody else's identity, because the person it may act
     *       as comes from the seat registry, not from the request.</li>
     * </ol>
     */
    private static void firebaseModeVerifiesTheTokenAndRefusesAForgedIdentity() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer();
             FakeIdentityToolkit identity = new FakeIdentityToolkit("user-77", "dev@motorph.test")) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot(),
                "test-api-key", identity.apiRoot());
            app.setSeatRegistry(seatLinks());
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());

                // A real, verified session.
                HttpResponse<String> meResponse =
                    getWithToken(base.resolve("/api/me"), FakeIdentityToolkit.VALID_TOKEN, "10005");
                com.google.gson.JsonObject me = com.google.gson.JsonParser.parseString(
                    meResponse.body()).getAsJsonObject();
                check("SYSTEM_ADMIN".equals(text(me, "role")),
                    "a verified token resolves to the person it is linked to");
                check(identity.presentedTokens().contains(FakeIdentityToolkit.VALID_TOKEN),
                    "the server asked Firebase to verify the token rather than trusting the header");
                check("FIREBASE".equals(text(me, "identityMode")),
                    "the session reports that identity comes from Firebase, not a fixture");

                // A verified account may only act as the people it is linked to. "user-77" is linked to
                // 10005; naming the CEO must be refused even though the request is otherwise valid.
                int borrowed = getWithToken(base.resolve("/api/me"), FakeIdentityToolkit.VALID_TOKEN, "10001")
                    .statusCode();
                check(borrowed == 403,
                    "a verified account cannot act as an employee it is not linked to (got " + borrowed + ")");

                // A token the verifier rejects is not rescued by naming a real employee number.
                int forged = getWithToken(base.resolve("/api/me"), "forged-token", "10005").statusCode();
                check(forged == 401, "a token the verifier rejects is refused (got " + forged + ")");

                // And a verified account cannot claim a different uid than its token's.
                int wrongUid = spoofedUid(base.resolve("/api/me"), FakeIdentityToolkit.VALID_TOKEN);
                check(wrongUid == 403, "a UID that disagrees with the verified token is refused (got "
                    + wrongUid + ")");
            } finally {
                app.stop();
            }
        }
    }

    private static HttpResponse<String> getWithToken(URI uri, String token, String employeeNumber)
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Authorization", "Bearer " + token)
            // The uid is part of a complete session in Firebase mode: it decides whose Firestore
            // documents are touched, and the server refuses a uid that disagrees with the token.
            .header("X-Firebase-UID", FakeIdentityToolkit.UID)
            .header("X-MotorPH-Employee", employeeNumber).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static int spoofedUid(URI uri, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Authorization", "Bearer " + token)
            .header("X-Firebase-UID", "somebody-elses-uid")
            .header("X-MotorPH-Employee", "10005").GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode();
    }

    /** The supplied MotorPH payslip as an HTTP request body. Amounts are decimal strings. */
    private static String goldenPayrollBody() {
        return "{\"employeeId\":\"10015\",\"employeeName\":\"Fredrick Romualdez\","
            + "\"position\":\"Account Manager\",\"department\":\"Accounting\","
            + "\"payPeriod\":\"SEMI_MONTHLY\",\"periodStart\":\"2026-01-02\",\"periodEnd\":\"2026-01-15\","
            + "\"monthlyBasic\":\"53500\",\"daysWorked\":10,"
            + "\"riceSubsidy\":\"1500\",\"phoneAllowance\":\"2000\",\"clothingAllowance\":\"1000\","
            + "\"taxBasis\":\"BASIC_ONLY\",\"statutoryBasis\":\"FULL_MONTHLY_EACH_PERIOD\"}";
    }

    private static String text(com.google.gson.JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }

    /**
     * Local-mode calls are made as the System Admin (10005) so these checks exercise data shape
     * rather than permissions. The permission checks live in
     * {@link #localModeEnforcesTheRbacMatrixOverHttp()}, and they name their own identity.
     */
    private static HttpResponse<String> get(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json").header("X-Firebase-EID", "10005").build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> get(URI uri, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid).header("X-Firebase-EID", "10005").build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** No Authorization and no employee number: the unlinked-account case. */
    private static HttpResponse<String> getWithNoIdentity(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> getAs(URI uri, String employeeNumber) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("X-Firebase-EID", employeeNumber).GET().build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postAs(URI uri, String json, String employeeNumber) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json").header("X-Firebase-EID", employeeNumber)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putAs(URI uri, String json, String employeeNumber) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json").header("X-Firebase-EID", employeeNumber)
            .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(URI uri, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").header("X-Firebase-EID", "10005")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putJson(URI uri, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").header("X-Firebase-EID", "10005")
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> putJson(URI uri, String json, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid).header("X-Firebase-EID", "10005")
            .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postJson(URI uri, String json, String token, String uid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json")
            .header("Content-Type", "application/json").header("Authorization", "Bearer " + token)
            .header("X-Firebase-UID", uid).header("X-Firebase-EID", "10005")
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /**
     * A stand-in for Google's {@code accounts:lookup}. It accepts exactly one token and answers with
     * the account that token belongs to — anything else is refused.
     *
     * <p>This is what makes the Firebase-mode tests meaningful: they previously asserted "a header was
     * sent"; now they assert "a token was verified", because the server asks this endpoint and will not
     * accept a request the endpoint rejects.
     */
    /**
     * The links a Firebase-mode test account holds. `user-77` is linked to 10005, the System Admin, so
     * the existing data-shape assertions keep working while every request now needs a verified token.
     */
    private static SeatRegistry seatLinks() {
        java.util.Map<String, java.util.List<SeatRegistry.Link>> links = new java.util.LinkedHashMap<>();
        links.put(FakeIdentityToolkit.UID, java.util.List.of(new SeatRegistry.Link(
            FakeIdentityToolkit.UID, "10005",
            java.util.List.of(new RoleAssignment.Seat(Role.SYSTEM_ADMIN, "test account")))));
        return new SeatRegistry(links);
    }

    private static final class FakeIdentityToolkit implements AutoCloseable {
        static final String VALID_TOKEN = "test-id-token";
        static final String UID = "user-77";
        private final HttpServer server;
        private final ArrayList<String> presentedTokens = new ArrayList<>();
        private String accountJson;

        FakeIdentityToolkit(String uid, String email) throws Exception {
            this.accountJson = "{\"kind\":\"identitytoolkit#GetAccountInfoResponse\",\"users\":[{\"localId\":\""
                + uid + "\",\"email\":\"" + email + "\",\"validUntil\":\"9999999999\"}]}";
            server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        /** Changes which account a valid token resolves to, for the multi-seat case. */
        void setAccount(String uid, String email) {
            this.accountJson = "{\"kind\":\"identitytoolkit#GetAccountInfoResponse\",\"users\":[{\"localId\":\""
                + uid + "\",\"email\":\"" + email + "\",\"validUntil\":\"9999999999\"}]}";
        }

        String apiRoot() { return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"; }

        ArrayList<String> requests() { return presentedTokens; }

        ArrayList<String> presentedTokens() { return presentedTokens; }

        private void handle(HttpExchange exchange) throws java.io.IOException {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String token = "";
            int at = body.indexOf("\"idToken\":\"");
            if (at >= 0) {
                int start = at + "\"idToken\":\"".length();
                int end = body.indexOf('"', start);
                token = end > start ? body.substring(start, end) : "";
            }
            presentedTokens.add(token);
            String response;
            int status;
            if (VALID_TOKEN.equals(token)) {
                response = accountJson;
                status = 200;
            } else {
                response = "{\"error\":{\"code\":400,\"message\":\"INVALID_ID_TOKEN\"}}";
                status = 400;
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }

    private static final class FakeFirestoreServer implements AutoCloseable {
        private static final String SCHEDULE_MAPPING_DOCUMENT = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoEmployeeSchedules/10001\",\"fields\":{\"employeeId\":{\"stringValue\":\"10001\"},\"scheduleId\":{\"integerValue\":\"101\"}}}";
        private static final String EMPLOYEE_DOCUMENT = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoEmployees/10001\","
            + "\"fields\":{\"id\":{\"stringValue\":\"10001\"},\"name\":{\"stringValue\":\"Manuel III Garcia\"},"
            + "\"department\":{\"stringValue\":\"People Operations\"},\"role\":{\"stringValue\":\"HR Coordinator\"},"
            + "\"location\":{\"stringValue\":\"Makati\"},\"status\":{\"stringValue\":\"Active\"}}}";
        private final HttpServer server;
        private final List<String> paths = new ArrayList<>();
        private final List<String> methods = new ArrayList<>();
        private final List<String> authorizations = new ArrayList<>();
        private final List<String> requestBodies = new ArrayList<>();
        private String storedRunName;
        private String storedRunFields;

        /** Echoes what the server wrote, so a save-then-list round trip is exercised for real. */
        private void savePayrollRun(String path, String body) {
            String documentId = "";
            int query = path.indexOf('?');
            if (query >= 0) {
                for (String pair : path.substring(query + 1).split("&")) {
                    if (pair.startsWith("documentId=")) documentId = pair.substring("documentId=".length());
                }
            }
            String cleanPath = query >= 0 ? path.substring(0, query) : path;
            com.google.gson.JsonObject parsed = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
            storedRunFields = parsed.getAsJsonObject("fields").toString();
            storedRunName = "projects/demo-project/databases/(default)/documents"
                + cleanPath.substring(cleanPath.indexOf("/users/")) + "/" + documentId;
        }

        private String storedPayrollRun() {
            if (storedRunName == null) return "{}";
            return "{\"name\":\"" + storedRunName + "\",\"fields\":" + storedRunFields + "}";
        }

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
            } else if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoEmployeeSchedules")) {
                response = "{\"documents\":[" + SCHEDULE_MAPPING_DOCUMENT + "]}";
            } else if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoAttendance")) {
                response = "{\"documents\":[]}";
            } else if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoPayrollRuns")) {
                response = "{\"documents\":[" + storedPayrollRun() + "]}";
            } else if ("POST".equals(exchange.getRequestMethod()) && path.contains("/demoPayrollRuns")) {
                // Pass the full URI: the document ID lives in the query string, not the path.
                savePayrollRun(exchange.getRequestURI().toString(), body);
                response = "{\"name\":\"" + storedRunName + "\"}";
            } else if ("POST".equals(exchange.getRequestMethod()) && path.contains("/demoLeaveRequests")) {
                response = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoLeaveRequests/LV-012345ABCDEF\"}";
            } else if ("POST".equals(exchange.getRequestMethod()) && path.contains("/demoEmployees")) {
                response = "{\"name\":\"projects/demo-project/databases/(default)/documents/users/user-77/demoEmployees/DEMO-FAKE000000\"}";
            } else if ("POST".equals(exchange.getRequestMethod()) && (path.contains("/demoEmployeeSchedules") || path.contains("/demoAttendance"))) {
                response = "{\"name\":\"projects/demo-project/databases/(default)/documents" + path.substring(path.indexOf("/users/")) + "\"}";
            } else if ("PATCH".equals(exchange.getRequestMethod()) && path.endsWith("/10001")) {
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
