package ph.motorph.svc;

import ph.motorph.c_calc.PayrollPolicy;
import ph.motorph.o_oop.Employee;
import ph.motorph.sp_secpol.AccountSecurity;
import ph.motorph.svc.AppServer;
import ph.motorph.t_tkt.Notifications;
import ph.motorph.t_tkt.PayslipOutbox;
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
        firebaseModeActsAsASandboxPerson();
        securityAuditBackupsPolicyAndBranches();
        staticFilesExposeDefensiveHeadersAndRejectTraversal();
        firebaseModeRejectsUnauthenticatedReadsAndWritesBeforeFirestore();
        firebaseModeRejectsPartialAndMalformedSessionsBeforeFirestore();
        System.out.println("PASS: " + checks + " AppServer integration checks");
    }

    private static void localModeServesSyntheticEmployeeAndAttendanceResponses() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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
            // Mara (Mon-Fri 08:00) 1-15 Oct: rows on 1, 2 and 15 Oct; the other 8 workdays have none. Every row is 8 h or more
            // (07:58-16:59 and 08:03-17:01 are 8 h 1 min each after the meal hour), so no undertime and 2 extra minutes.
            check(attendance.body().contains("\"presentDays\":3,\"absentDays\":8,\"tardyDays\":0") && attendance.body().contains("\"unapprovedExtraMinutes\":2,\"undertimeMinutes\":0,"),
                "attendance metrics count present, absent, tardy and undertime: " + attendance.body());

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
                && rawReport.body().contains("\"count\":5"), "raw attendance coverage report loads without pay calculations");
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
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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

            String toReview = com.google.gson.JsonParser.parseString(postJson(base.resolve("/api/workflows/leave"),
                "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Medical\",\"startDate\":\"2026-11-12\",\"endDate\":\"2026-11-12\",\"note\":\"\"}").body())
                .getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            HttpResponse<String> selfReview = postJson(base.resolve("/api/workflows/leave/" + toReview + "/approve"), "{\"reviewerId\":\"DEMO-001\"}");
            check(selfReview.statusCode() == 403, "employee cannot approve own leave via API");
            check(postJson(base.resolve("/api/workflows/leave/" + toReview + "/approve"), "{\"reviewerId\":\"DEMO-002\"}").statusCode() == 403,
                "payroll staff cannot approve leave (RBAC matrix)");
            HttpResponse<String> approvedLeave = postJson(base.resolve("/api/workflows/leave/" + toReview + "/approve"), "{\"reviewerId\":\"DEMO-003\"}");
            check(approvedLeave.statusCode() == 200 && approvedLeave.body().contains("\"status\":\"Approved\"")
                && approvedLeave.body().contains("\"reviewerId\":\"DEMO-003\""), "the supervisor can approve a direct report's leave");
            HttpResponse<String> repeatReview = postJson(base.resolve("/api/workflows/leave/" + toReview + "/reject"), "{\"reviewerId\":\"DEMO-003\"}");
            check(repeatReview.statusCode() == 409, "decided leave cannot be reviewed again");
            HttpResponse<String> overtime = postJson(base.resolve("/api/workflows/overtime"),
                "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-11-03\",\"minutes\":90,\"note\":\"Month-end close\"}");
            check(overtime.statusCode() == 201 && overtime.body().contains("\"status\":\"Pending review\"")
                && overtime.body().contains("\"minutes\":90"), "local overtime request is created pending");
            String overtimeId = com.google.gson.JsonParser.parseString(overtime.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postJson(base.resolve("/api/workflows/overtime"),
                "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-11-04\",\"minutes\":\"90\"}").statusCode() == 400, "overtime minutes must be a number");
            check(postJson(base.resolve("/api/workflows/overtime/" + overtimeId + "/approve"), "{\"reviewerId\":\"DEMO-001\"}").statusCode() == 403,
                "employee cannot approve own overtime via API");
            HttpResponse<String> approvedOvertime = postJson(base.resolve("/api/workflows/overtime/" + overtimeId + "/approve"), "{\"reviewerId\":\"DEMO-003\"}");
            check(approvedOvertime.statusCode() == 200 && approvedOvertime.body().contains("\"reviewerId\":\"DEMO-003\""), "another reviewer approves overtime");
            check(get(base.resolve("/api/workflows")).body().contains("\"overtimeRequests\":[{\"id\":\"" + overtimeId), "workflow list includes overtime requests");
            HttpResponse<String> balances = get(base.resolve("/api/workflows/leave/balances?employeeId=DEMO-001&year=2026"));
            check(balances.statusCode() == 200 && balances.body().contains("\"temporary\":true")
                && balances.body().contains("\"leaveType\":\"Medical\",\"allowance\":5,\"used\":1"), "balances API reports approved medical credit");
            HttpResponse<String> badYear = get(base.resolve("/api/workflows/leave/balances?employeeId=DEMO-001&year=abc"));
            check(badYear.statusCode() == 400, "balances API rejects a non-numeric year");
            HttpResponse<String> unknown = get(base.resolve("/api/workflows/leave/balances?employeeId=DEMO-999"));
            check(unknown.statusCode() == 404, "balances API rejects unknown employee");
            HttpResponse<String> payroll = get(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-001&month=2026-10"));
            check(payroll.statusCode() == 200 && payroll.body().contains("\"grossPay\":\"7524.24\"")
                && payroll.body().contains("\"monthlyGross\":\"7524.24\"") && payroll.body().contains("\"temporary\":true"),
                "payroll cutoffs API returns attendance-derived gross");
            check(payroll.body().contains("\"earnings\":{\"basicCents\":752424,\"premiumCents\":0,\"overtimeCents\":0")
                && payroll.body().contains("\"leaveDays\":0"), "cutoffs API lists each earnings line");
            check(get(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-001&month=2026-13")).statusCode() == 400,
                "payroll cutoffs API rejects an invalid month");
            check(payroll.body().contains("\"totalEmployeeDeductionsCents\":77548")
                && payroll.body().contains("\"deductionsCents\":38774") && payroll.body().contains("\"unpaidOverageMinutes\":2")
                && payroll.body().contains("\"blocked\":true"), "cutoffs API splits Java deductions half per cutoff");
            HttpResponse<String> statutory = postJson(base.resolve("/api/payroll/statutory"),
                "{\"period\":\"2026-11\",\"grossCashPay\":\"25000.00\",\"sssCompensation\":\"25000.00\",\"philHealthBasicSalary\":\"25000.00\",\"pagIbigFundSalary\":\"25000.00\",\"taxableCompensation\":\"25000.00\"}");
            check(statutory.statusCode() == 200 && statutory.body().contains("\"withholdingTaxCents\":31375")
                && statutory.body().contains("\"netPayCents\":2261125"), "statutory API returns the sweldongpinoy 25,000 result");
            HttpResponse<String> negative = postJson(base.resolve("/api/payroll/statutory"),
                "{\"period\":\"2026-11\",\"grossCashPay\":\"100\",\"sssCompensation\":\"25000.00\",\"philHealthBasicSalary\":\"25000.00\",\"pagIbigFundSalary\":\"25000.00\",\"taxableCompensation\":\"25000.00\"}");
            check(negative.statusCode() == 400 && negative.body().contains("exceed gross cash pay"), "statutory API blocks negative net");
            HttpResponse<String> extra = postJson(base.resolve("/api/payroll/statutory"), "{\"period\":\"2026-11\",\"salary\":\"1\"}");
            check(extra.statusCode() == 400, "statutory API rejects unexpected fields");
            HttpResponse<String> payslip = get(base.resolve("/api/payroll/payslip?employeeId=DEMO-001&month=2026-10&cutoff=1"));
            check(payslip.statusCode() == 200 && payslip.body().contains("\"payslipId\":\"PS-DEMO-001-2026-10-1\"")
                && payslip.body().contains("\"name\":\"Mara Rivera\"") && payslip.body().contains("\"pagIbigCents\":7524")
                && payslip.body().contains("\"netPayCents\":888650") && payslip.body().contains("\"saved\":false"),
                "payslip API returns cutoff 1 lines and net");
            check(get(base.resolve("/api/payroll/payslip?employeeId=DEMO-001&month=2026-10&cutoff=2")).statusCode() == 409,
                "no payslip for a cutoff without paid attendance");
            check(get(base.resolve("/api/payroll/payslip?employeeId=DEMO-001&month=2026-10&cutoff=3")).statusCode() == 400,
                "payslip API rejects an unknown cutoff");
            String finalizeBody = "{\"employeeId\":\"DEMO-001\",\"month\":\"2026-10\",\"cutoff\":\"1\"}";
            HttpResponse<String> finalized = postJson(base.resolve("/api/payroll/payslips"), finalizeBody);
            check(finalized.statusCode() == 201 && finalized.body().contains("\"saved\":true") && finalized.body().contains("\"finalizedAt\"")
                && finalized.body().contains("\"netPayCents\":888650"), "a payable cutoff payslip can be finalized");
            check(postJson(base.resolve("/api/payroll/payslips"), finalizeBody).statusCode() == 409, "a payslip can be finalized only once");
            check(postJson(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-001\",\"month\":\"2026-10\",\"cutoff\":\"2\"}").statusCode() == 409,
                "a cutoff without paid attendance cannot be finalized");
            check(postJson(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-001\",\"month\":\"2026-10\",\"cutoff\":\"1\",\"net\":\"1\"}").statusCode() == 400,
                "finalize rejects client-supplied amounts");
            check(postJson(base.resolve("/api/attendance"),
                "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-10-05\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}").statusCode() == 201, "attendance added after finalizing");
            check(!get(base.resolve("/api/payroll/payslip?employeeId=DEMO-001&month=2026-10&cutoff=1")).body().contains("\"netPayCents\":888650"),
                "the live payslip reflects the new attendance");
            HttpResponse<String> saved = get(base.resolve("/api/payroll/payslips/PS-DEMO-001-2026-10-1"));
            check(saved.statusCode() == 200 && saved.body().contains("\"netPayCents\":888650") && saved.body().contains("\"saved\":true"),
                "the saved snapshot does not change when attendance changes");
            HttpResponse<String> history = get(base.resolve("/api/payroll/payslips"));
            check(history.statusCode() == 200 && history.body().contains("\"payslipId\":\"PS-DEMO-001-2026-10-1\"")
                && history.body().contains("\"cutoff\":\"1-15\""), "history lists finalized payslips");
            check(get(base.resolve("/api/payroll/payslips/PS-NOPE")).statusCode() == 404, "unknown saved payslip is 404");

            // --- Role-based access (X-Demo-User = the signed-in demo account) ---
            HttpResponse<String> accounts = get(base.resolve("/api/demo/accounts"));
            check(accounts.statusCode() == 200 && accounts.body().contains("\"roleLabel\":\"System Admin\"")
                && accounts.body().contains("\"employeeId\":\"DEMO-004\",\"name\":\"Jae Cruz\""), "demo accounts list each role");
            check(getAs(base.resolve("/api/employees"), "DEMO-999").statusCode() == 401, "unknown demo account is rejected");
            // --- Demo 2FA (requirement 1) ---
            check(rawAs(base.resolve("/api/employees"), "DEMO-004", "").statusCode() == 401, "an account without a 2FA session is refused");
            check(rawAs(base.resolve("/api/employees"), "DEMO-004", sessionFor(base, "DEMO-003")).statusCode() == 401, "someone else's session does not work");
            check(postJson(base.resolve("/api/auth/2fa/start"), "{\"employeeId\":\"DEMO-999\"}").statusCode() == 404, "no code for an unknown account");
            com.google.gson.JsonObject challenge = com.google.gson.JsonParser.parseString(
                postJson(base.resolve("/api/auth/2fa/start"), "{\"employeeId\":\"DEMO-004\",\"password\":\"" + AccountSecurity.DEFAULT_PASSWORD + "\"}").body()).getAsJsonObject();
            check(challenge.get("demoCode").getAsString().matches("[0-9]{6}") && challenge.get("sentTo").getAsString().endsWith("04"), "a 6-digit code is sent to the phone ending 04");
            String wrong = challenge.get("demoCode").getAsString().equals("000000") ? "111111" : "000000";
            String wrongBody = "{\"challengeId\":\"" + challenge.get("challengeId").getAsString() + "\",\"code\":\"" + wrong + "\"}";
            HttpResponse<String> firstWrong = postJson(base.resolve("/api/auth/2fa/verify"), wrongBody);
            check(firstWrong.statusCode() == 401 && firstWrong.body().contains("2 tries left"), "a wrong code says how many tries are left");
            postJson(base.resolve("/api/auth/2fa/verify"), wrongBody);
            check(postJson(base.resolve("/api/auth/2fa/verify"), wrongBody).body().contains("Wrong code 3 times"), "three wrong codes end the request");
            check(postJson(base.resolve("/api/auth/2fa/verify"), wrongBody.replace(wrong, challenge.get("demoCode").getAsString())).statusCode() == 401,
                "after three wrong codes even the right code no longer works");
            String jaeSession = sessionFor(base, "DEMO-004");
            check(rawAs(base.resolve("/api/auth/session"), "DEMO-004", jaeSession).body().contains("\"employeeId\":\"DEMO-004\""), "the session belongs to Jae");
            String spare = sessionFor(base, "DEMO-005");
            HttpRequest signOut = HttpRequest.newBuilder(base.resolve("/api/auth/signout")).header("Content-Type", "application/json").header("X-Demo-Session", spare)
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)).build();
            check(HTTP.send(signOut, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).statusCode() == 200
                && rawAs(base.resolve("/api/employees"), "DEMO-005", spare).statusCode() == 401, "after signing out the session no longer works");
            SESSIONS.remove(base.getPort() + "|DEMO-005");
            HttpResponse<String> teamDirectory = getAs(base.resolve("/api/employees"), "DEMO-004");
            check(teamDirectory.body().contains("DEMO-003") && !teamDirectory.body().contains("DEMO-005"), "employee directory shows only the team");
            check(getAs(base.resolve("/api/attendance?employeeId=DEMO-001"), "DEMO-004").statusCode() == 403, "employee cannot read a teammate's attendance");
            check(getAs(base.resolve("/api/attendance?employeeId=DEMO-004"), "DEMO-004").statusCode() == 200, "employee reads own attendance");
            check(getAs(base.resolve("/api/reports/attendance"), "DEMO-004").statusCode() == 403, "employee cannot open reports");
            check(postAs(base.resolve("/api/payroll/statutory"), "{\"period\":\"2026-11\",\"grossCashPay\":\"1\",\"sssCompensation\":\"1\",\"philHealthBasicSalary\":\"1\",\"pagIbigFundSalary\":\"1\",\"taxableCompensation\":\"1\"}", "DEMO-001").statusCode() == 403,
                "HR cannot run the statutory calculator");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-001&month=2026-10"), "DEMO-004").statusCode() == 403, "employee cannot see others' pay");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10"), "DEMO-004").statusCode() == 200, "employee sees own pay");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10&through=2026-10-05"), "DEMO-004").body().contains("\"paidMinutes\":480"),
                "through= counts only days up to that date (tracker)");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10&through=2026-02-30"), "DEMO-004").statusCode() == 400, "bad through date is a 400");
            check(postAs(base.resolve("/api/employees"), "{\"name\":\"X\",\"department\":\"Y\",\"role\":\"Z\",\"location\":\"W\",\"status\":\"Active\"}", "DEMO-004").statusCode() == 403,
                "employees cannot create employee records");
            check(postAs(base.resolve("/api/workflows/leave"), "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Personal\",\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-01\"}", "DEMO-004").statusCode() == 403,
                "employees file leave only for themselves");
            HttpResponse<String> ownLeave = postAs(base.resolve("/api/workflows/leave"), "{\"employeeId\":\"DEMO-004\",\"leaveType\":\"Personal\",\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-01\"}", "DEMO-004");
            check(ownLeave.statusCode() == 201, "employee files own leave");
            String ownLeaveId = com.google.gson.JsonParser.parseString(ownLeave.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/workflows/leave/" + ownLeaveId + "/approve"), "{}", "DEMO-004").statusCode() == 403, "no self-approval as the signed-in account");
            check(postAs(base.resolve("/api/workflows/leave/" + ownLeaveId + "/approve"), "{\"reviewerId\":\"DEMO-005\"}", "DEMO-001").statusCode() == 403,
                "a signed-in reviewer cannot record someone else as the reviewer");
            HttpResponse<String> hrApproved = postAs(base.resolve("/api/workflows/leave/" + ownLeaveId + "/approve"), "{}", "DEMO-001");
            check(hrApproved.statusCode() == 200 && hrApproved.body().contains("\"reviewerId\":\"DEMO-001\""), "HR approves leave, recorded as the signed-in account");
            HttpResponse<String> teamOvertime = postAs(base.resolve("/api/workflows/overtime"), "{\"employeeId\":\"DEMO-004\",\"date\":\"2026-10-05\",\"minutes\":30}", "DEMO-004");
            String teamOvertimeId = com.google.gson.JsonParser.parseString(teamOvertime.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/workflows/overtime/" + teamOvertimeId + "/approve"), "{}", "DEMO-001").statusCode() == 403, "HR cannot approve overtime");
            check(postAs(base.resolve("/api/workflows/overtime/" + teamOvertimeId + "/approve"), "{}", "DEMO-003").statusCode() == 200, "the supervisor approves team overtime");
            // --- Notifications: the person affected is told; never the person acting ---
            HttpResponse<String> jaeNotes = getAs(base.resolve("/api/notifications"), "DEMO-004");
            check(jaeNotes.statusCode() == 200 && jaeNotes.body().contains("Your Personal leave 2026-12-01 to 2026-12-01 was approved by Mara Rivera.")
                && jaeNotes.body().contains("Your overtime request for 2026-10-05 (30 min) was approved by Noa Reyes.") && jaeNotes.body().contains("\"unread\":2"),
                "Jae is notified of his leave and overtime decisions: " + jaeNotes.body());
            check(!getAs(base.resolve("/api/notifications"), "DEMO-001").body().contains("Personal leave"), "the reviewer is not notified of their own action");
            check(postAs(base.resolve("/api/notifications/read"), "{}", "DEMO-004").body().contains("\"marked\":2")
                && getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("\"unread\":0"), "Jae marks them read");
            check(get(base.resolve("/api/notifications")).statusCode() == 400, "notifications need a signed-in account");
            HttpResponse<String> employeeWorkflows = getAs(base.resolve("/api/workflows"), "DEMO-004");
            check(employeeWorkflows.body().contains("\"employeeId\":\"DEMO-001\",\"employeeName\":\"Mara Rivera\",\"leaveType\"")
                && !employeeWorkflows.body().contains("Fictional issue"), "employee sees team leave but only own tickets");
            check(postAs(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-002\",\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-002").statusCode() == 403,
                "payroll staff cannot finalize their own payslip");
            check(postAs(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-001").statusCode() == 403,
                "HR cannot finalize payslips");
            check(postAs(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-10\",\"cutoff\":\"1\"}", "DEMO-002").statusCode() == 201,
                "payroll staff finalize an employee's payslip");
            String mySlips = getAs(base.resolve("/api/payroll/payslips"), "DEMO-004").body();
            check(mySlips.contains("PS-DEMO-004-2026-10-1") && !mySlips.contains("PS-DEMO-001"), "employees see only their own saved payslips");
            check(getAs(base.resolve("/api/payroll/payslips/PS-DEMO-001-2026-10-1"), "DEMO-004").statusCode() == 403, "employee cannot open a teammate's saved payslip");
            check(getAs(base.resolve("/api/payroll/payslips"), "DEMO-003").body().contains("PS-DEMO-001-2026-10-1"), "supervisor sees team payslips");
            check(postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"DEMO-004\",\"date\":\"2026-10-07\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003").statusCode() == 201,
                "supervisor edits a direct report's timesheet");
            check(postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"DEMO-005\",\"date\":\"2026-10-07\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003").statusCode() == 403,
                "supervisor cannot edit attendance outside the team");
            check(get(base.resolve("/api/attendance/clock")).statusCode() == 400, "clock in/out needs a signed-in demo account");
            HttpResponse<String> clockStatus = getAs(base.resolve("/api/attendance/clock"), "DEMO-005");
            check(clockStatus.statusCode() == 200 && clockStatus.body().contains("\"clockedIn\":false"), "clock status starts clocked out");
            HttpResponse<String> clockedIn = postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"in\"}", "DEMO-005");
            check(clockedIn.statusCode() == 201 && clockedIn.body().contains("\"clockedIn\":true"), "clock in opens a shift for the signed-in account");
            check(postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"in\"}", "DEMO-005").statusCode() == 409, "cannot clock in twice");
            // QR onsite check-in: the clock-in shows a QR code and counts as offsite; scanning it makes the shift onsite.
            com.google.gson.JsonObject offsite = com.google.gson.JsonParser.parseString(getAs(base.resolve("/api/attendance/clock"), "DEMO-005").body()).getAsJsonObject();
            check("Offsite".equals(offsite.get("location").getAsString()) && offsite.get("qrCode").getAsString().startsWith("MPH-"), "clocked in from the app = offsite, with a QR code");
            check(postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"onsite\",\"code\":\"MPH-ZZZZZZ\"}", "DEMO-005").statusCode() == 400, "a wrong QR code is refused");
            HttpResponse<String> scanned = postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"onsite\",\"code\":\"" + offsite.get("qrCode").getAsString() + "\"}", "DEMO-005");
            check(scanned.statusCode() == 201 && scanned.body().contains("\"location\":\"Onsite\"") && scanned.body().contains("\"qrCode\":\"\""),
                "scanning the shift's QR code at the branch makes it onsite");
            check(postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"qr\"}", "DEMO-005").statusCode() == 409, "no new QR once onsite");
            check(postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"out\"}", "DEMO-004").statusCode() == 409, "another account is not clocked in");
            // Attendance tag on clock-in: Present until the shift start + 10 minutes (Lia: Mon-Fri 08:00), Tardy after.
            com.google.gson.JsonObject tagged = com.google.gson.JsonParser.parseString(getAs(base.resolve("/api/attendance/clock"), "DEMO-005").body()).getAsJsonObject();
            String expectedTag = java.time.LocalTime.parse(tagged.get("since").getAsString()).isAfter(java.time.LocalTime.of(8, 10)) ? "Tardy" : "Present";
            check(expectedTag.equals(tagged.get("attendanceTag").getAsString()) && !tagged.get("dayType").getAsString().isEmpty(), "clock-in tags the day Present or Tardy and says the day type");
            HttpResponse<String> clockedOut = postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"out\"}", "DEMO-005");
            check(clockedOut.body().matches("(?s).*\"workHourTag\":\"(Regular|Overtime|Undertime)\".*"), "clock-out tags the worked hours");
            HttpResponse<String> teamToday = getAs(base.resolve("/api/team/today"), "DEMO-005");
            check(teamToday.statusCode() == 200 && teamToday.body().contains("{\"employeeId\":\"DEMO-005\",\"status\":\"" + expectedTag + " · "),
                "the roster shows Lia's tag and times today: " + teamToday.body());
            check(!getAs(base.resolve("/api/team/today"), "DEMO-004").body().contains("DEMO-005"), "an employee sees only their team in the roster");
            check(clockedOut.statusCode() == 201 && clockedOut.body().contains("\"clockedIn\":false") && clockedOut.body().contains("\"record\":{"),
                "clock out records today's attendance row");
            check(postAs(base.resolve("/api/attendance/clock"), "{\"action\":\"lunch\"}", "DEMO-005").statusCode() == 400, "clock action must be in, out, qr or onsite");

            // --- Gross-to-net: Jae (DEMO-004, 148.81/h), November cutoff 1 (hand-worked) ---
            // 8 h on 3 Nov = 1,190.48 earned + 1,000 bonus = 2,190.48 gross. The month also has 8 h holiday pay for Bonifacio Day
            // (30 Nov, unworked) in cutoff 2, so month earned pay is 2,380.96: SSS 125, PhilHealth 125, Pag-IBIG 2% = 47.62 -> 23.81, tax 0;
            // loan 300; allowances (1,500 + 500 + 500) / 2 = 1,250; net 2,190.48 - 273.81 - 300 + 1,250 = 2,866.67.
            check(postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"DEMO-004\",\"date\":\"2026-11-03\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003").statusCode() == 201,
                "November attendance for Jae");
            String bonusBody = "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-11\",\"cutoff\":\"1\",\"type\":\"Bonus\",\"description\":\"Spot award\",\"amount\":\"1000.00\"}";
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody, "DEMO-004").statusCode() == 403, "employees cannot add their own bonus");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody, "DEMO-001").statusCode() == 403, "HR cannot enter payroll earnings");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody.replace("DEMO-004", "DEMO-002"), "DEMO-002").statusCode() == 403,
                "payroll staff cannot change their own pay");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody.replace("Bonus", "Tip"), "DEMO-002").statusCode() == 400, "unknown earning type is rejected");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody, "DEMO-002").statusCode() == 201, "payroll staff add a bonus");
            check(postAs(base.resolve("/api/payroll/adjustments/deductions"),
                "{\"employeeId\":\"DEMO-004\",\"type\":\"Company loan\",\"description\":\"Laptop loan\",\"amountPerCutoff\":\"300\",\"fromMonth\":\"2026-11\",\"toMonth\":\"2026-12\"}", "DEMO-002").statusCode() == 201,
                "payroll staff add a voluntary deduction");
            HttpResponse<String> novSlip = getAs(base.resolve("/api/payroll/payslip?employeeId=DEMO-004&month=2026-11&cutoff=1"), "DEMO-004");
            check(novSlip.statusCode() == 200 && novSlip.body().contains("\"grossPayCents\":219048") && novSlip.body().contains("\"bonusCents\":100000")
                && novSlip.body().contains("\"pagIbigCents\":2381") && novSlip.body().contains("\"voluntaryCents\":30000")
                && novSlip.body().contains("\"allowances\":{\"riceCents\":75000,\"phoneCents\":25000,\"clothingCents\":25000,\"totalCents\":125000,\"corrections\":[]}")
                && novSlip.body().contains("\"netPayCents\":286667"), "payslip runs gross to net with bonus, loan and allowances");
            check(getAs(base.resolve("/api/payroll/adjustments"), "DEMO-004").body().contains("\"earnings\":[]"), "employees do not browse earnings entries (matrix: -)");
            check(getAs(base.resolve("/api/payroll/adjustments?employeeId=DEMO-004"), "DEMO-003").body().contains("\"description\":\"Spot award\",\"amountCents\":100000"), "the supervisor reads team earnings (as JSON objects)");
            check(postAs(base.resolve("/api/payroll/payslips"), "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-11\",\"cutoff\":\"1\"}", "DEMO-002").statusCode() == 201,
                "payroll staff finalize the November payslip");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), bonusBody, "DEMO-002").statusCode() == 409, "no earnings on a finalized payslip");
            check(postAs(base.resolve("/api/payroll/disbursements"), "{\"month\":\"2026-11\",\"cutoff\":\"1\"}", "DEMO-001").statusCode() == 403, "HR cannot release pay");
            HttpResponse<String> batch = postAs(base.resolve("/api/payroll/disbursements"), "{\"month\":\"2026-11\",\"cutoff\":\"1\"}", "DEMO-002");
            check(batch.statusCode() == 201 && batch.body().contains("\"payslipId\":\"PS-DEMO-004-2026-11-1\"") && batch.body().contains("\"method\":\"Check\"")
                && batch.body().contains("\"totalCents\":286667"), "release creates a disbursement batch of finalized net pay");
            check(postAs(base.resolve("/api/payroll/disbursements"), "{\"month\":\"2026-11\",\"cutoff\":\"1\"}", "DEMO-002").statusCode() == 409, "a payslip is released only once");
            check(getAs(base.resolve("/api/payroll/payslips"), "DEMO-004").body().contains("\"released\":true"), "history shows the payslip as released");
            // Payslip email outbox (requirement 10, simulated): releasing pay emails each payslip; a failed send can be retried.
            check(batch.body().contains("\"emailsSent\":1,\"emailsFailed\":0"), "release emails the payslip");
            check(getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("Your payslip PS-DEMO-004-2026-11-1 was released: net ₱2,866.67 by Check."),
                "Jae is notified when his pay is released");
            HttpResponse<String> jaeInbox = getAs(base.resolve("/api/payroll/outbox"), "DEMO-004");
            check(jaeInbox.statusCode() == 200 && jaeInbox.body().contains("\"payslipId\":\"PS-DEMO-004-2026-11-1\"") && jaeInbox.body().contains("\"to\":\"jae.cruz@motorph.example\"")
                && jaeInbox.body().contains("net pay ₱2,866.67 by Check") && jaeInbox.body().contains("\"status\":\"Sent\""), "Jae sees his payslip email: " + jaeInbox.body());
            String emailId = com.google.gson.JsonParser.parseString(jaeInbox.body()).getAsJsonObject().getAsJsonArray("messages").get(0).getAsJsonObject().get("id").getAsString();
            check(postAs(base.resolve("/api/payroll/outbox/outage"), "{\"on\":true}", "DEMO-004").statusCode() == 403, "employees cannot change the mail settings");
            check(postAs(base.resolve("/api/payroll/outbox/outage"), "{\"on\":true}", "DEMO-002").body().contains("\"outage\":true"), "payroll staff simulate a mail outage");
            check(postAs(base.resolve("/api/payroll/outbox/" + emailId + "/resend"), "{}", "DEMO-002").body().contains("\"status\":\"Failed\""), "during the outage a resend fails");
            check(getAs(base.resolve("/api/payroll/disbursements"), "DEMO-002").body().contains("\"status\":\"Released, 1 email(s) failed\""), "the cycle is not Distributed while an email failed");
            check(postAs(base.resolve("/api/payroll/outbox/retry-failed"), "{}", "DEMO-002").body().contains("\"nowSent\":0"), "retrying during the outage still fails");
            postAs(base.resolve("/api/payroll/outbox/outage"), "{\"on\":false}", "DEMO-002");
            check(postAs(base.resolve("/api/payroll/outbox/retry-failed"), "{}", "DEMO-002").body().contains("\"nowSent\":1"), "after the outage the retry sends it");
            check(getAs(base.resolve("/api/payroll/outbox"), "DEMO-004").body().contains("\"status\":\"Sent\",\"lastAttemptAt\"")
                && getAs(base.resolve("/api/payroll/outbox"), "DEMO-004").body().contains("\"attempts\":4"), "the email shows 4 attempts and is sent");
            check(postAs(base.resolve("/api/payroll/outbox/EM-9999/resend"), "{}", "DEMO-002").statusCode() == 404, "unknown email is 404");
            check(getAs(base.resolve("/api/payroll/disbursements"), "DEMO-002").body().contains("\"status\":\"Distributed\""), "once every email is sent, the cycle is Distributed");
            check(jaeInbox.body().contains("\"attachment\":\"PS-DEMO-004-2026-11-1.pdf\""), "the email carries the payslip PDF");
            String teamBox = getAs(base.resolve("/api/payroll/outbox"), "DEMO-002").body();
            check(teamBox.contains("\"to\":\"payroll@motorph.example\"") && teamBox.contains("1 payslip(s), net total ₱2,866.67; 1 emailed, 0 failed."),
                "the payroll team gets a collated distribution report");
            check(!getAs(base.resolve("/api/payroll/outbox"), "DEMO-004").body().contains("payroll@motorph.example"), "employees do not see the team report");
            // The PDF: a valid one-page PDF (every cross-reference offset points at its object) with the payslip's numbers.
            HttpRequest pdfRequest = HttpRequest.newBuilder(base.resolve("/api/payroll/payslips/PS-DEMO-004-2026-11-1/pdf")).GET()
                .header("X-Demo-User", "DEMO-004").header("X-Demo-Session", sessionFor(base, "DEMO-004")).build();
            HttpResponse<byte[]> pdf = HTTP.send(pdfRequest, HttpResponse.BodyHandlers.ofByteArray());
            String pdfText = new String(pdf.body(), StandardCharsets.ISO_8859_1);
            check(pdf.statusCode() == 200 && "application/pdf".equals(pdf.headers().firstValue("Content-Type").orElse("")) && pdfText.startsWith("%PDF-1.4")
                && pdfText.endsWith("%%EOF\n") && pdfText.contains("(NET PAY ............................... PHP 2,866.67) Tj"), "Jae downloads his payslip as a PDF");
            java.util.regex.Matcher offsets = java.util.regex.Pattern.compile("(\\d{10}) 00000 n").matcher(pdfText);
            int objectNumber = 0;
            boolean offsetsOk = true;
            while (offsets.find()) {
                objectNumber++;
                offsetsOk &= pdfText.startsWith(objectNumber + " 0 obj", Integer.parseInt(offsets.group(1)));
            }
            check(offsetsOk && objectNumber == 5, "the PDF's cross-reference table points at each of its 5 objects");
            check(getAs(base.resolve("/api/payroll/payslips/PS-DEMO-004-2026-11-1/pdf"), "DEMO-001").statusCode() == 200
                && getAs(base.resolve("/api/payroll/payslips/PS-DEMO-004-2026-11-1/pdf"), "DEMO-002").statusCode() == 200, "HR and payroll staff can open it");
            check("ana.penareyes@motorph.example".equals(PayslipOutbox.workEmail("Ana Peña-Reyes")), "the email rule matches the roster (web/team-roster.js test)");
            HttpResponse<String> remittance = getAs(base.resolve("/api/payroll/remittance?month=2026-11"), "DEMO-001");
            check(remittance.statusCode() == 200 && remittance.body().contains("\"agency\":\"SSS\",\"employeeCents\":12500,\"employerCents\":25500,\"totalCents\":38000"),
                "remittance totals SSS employee 125 + employer (500 + EC 10) / 2 = 380 from finalized payslips");
            check(getAs(base.resolve("/api/payroll/remittance?month=2026-11"), "DEMO-004").statusCode() == 403, "employees cannot open remittance");
            // Payroll summary (requirement 10): Jae's finalized Nov 1-15 payslip: gross 2,190.48, allowances 1,250.00, net 2,866.67,
            // so deductions = 2,190.48 + 1,250.00 - 2,866.67 = 573.81 (statutory 273.81 + loan 300.00).
            String sameRow = "\"employees\":1,\"payslips\":1,\"grossCents\":219048,\"deductionsCents\":57381,\"allowancesCents\":125000,\"netCents\":286667";
            HttpResponse<String> byDepartment = getAs(base.resolve("/api/payroll/summary?month=2026-11"), "DEMO-002");
            check(byDepartment.statusCode() == 200 && byDepartment.body().contains("{" + sameRow + ",\"name\":\"Operations\"}"),
                "the summary totals finalized payslips by department: " + byDepartment.body());
            check(getAs(base.resolve("/api/payroll/summary?month=2026-11&by=branch"), "DEMO-002").body().contains("{" + sameRow + ",\"name\":\"Pasig\"}"), "and by branch");
            check(getAs(base.resolve("/api/payroll/summary?month=2026-11&by=floor"), "DEMO-002").statusCode() == 400, "only department or branch");
            // The same report as files (design doc: aggregate payroll report, CSV or PDF, for payroll staff).
            HttpResponse<String> csv = getAs(base.resolve("/api/payroll/summary?month=2026-11&format=csv"), "DEMO-002");
            check(csv.statusCode() == 200 && csv.headers().firstValue("Content-Type").orElse("").startsWith("text/csv")
                && csv.body().contains("PS-DEMO-004-2026-11-1,DEMO-004,Jae Cruz,Operations,2190.48,573.81,1250.00,2866.67\r\n")
                && csv.body().contains("TOTAL,,1 employee(s),,2190.48,573.81,1250.00,2866.67"), "the summary downloads as CSV: " + csv.body());
            HttpResponse<String> reportPdf = getAs(base.resolve("/api/payroll/summary?month=2026-11&format=pdf"), "DEMO-002");
            check(reportPdf.statusCode() == 200 && reportPdf.body().startsWith("%PDF-1.4") && reportPdf.body().contains("gross 2190.48, deductions 573.81, allowances 1250.00, net 2866.67)"),
                "and as PDF");
            check(getAs(base.resolve("/api/payroll/summary?month=2026-11&format=xlsx"), "DEMO-002").statusCode() == 400, "only csv or pdf");
            check(getAs(base.resolve("/api/payroll/summary?month=2026-11"), "DEMO-004").statusCode() == 403, "employees cannot open the payroll summary");

            // --- Supervisor tools ---
            String schedule = "{\"shiftStart\":\"10:00\",\"workdays\":[\"TUE\",\"WED\",\"THU\",\"FRI\",\"SAT\"]}";
            check(putAs(base.resolve("/api/schedules/DEMO-004"), schedule, "DEMO-004").statusCode() == 403, "employees cannot set their own schedule");
            check(putAs(base.resolve("/api/schedules/DEMO-005"), schedule, "DEMO-003").statusCode() == 403, "supervisors set only their team's schedules");
            check(putAs(base.resolve("/api/schedules/DEMO-004"), schedule.replace("10:00", "16:00"), "DEMO-003").statusCode() == 400, "no overnight shifts");
            HttpResponse<String> scheduled = putAs(base.resolve("/api/schedules/DEMO-004"), schedule, "DEMO-003");
            check(scheduled.statusCode() == 200 && scheduled.body().contains("\"shiftEnd\":\"19:00\"") && scheduled.body().contains("\"mealStart\":\"14:00\""),
                "the supervisor sets a 10:00-19:00 Tue-Sat schedule");
            check(getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("Your schedule changed: shift from 10:00 to 19:00, Tue, Wed, Thu, Fri, Sat (set by Noa Reyes)."),
                "Jae is notified of his new schedule");
            // Single-day / multi-day changes on top of the Tue-Sat pattern. 12-16 Oct: Mon 12 is a rest day, Tue-Fri are workdays with no rows = 4 absent.
            String octWeek = "/api/attendance?employeeId=DEMO-004&from=2026-10-12&to=2026-10-16";
            check(getAs(base.resolve(octWeek), "DEMO-003").body().contains("\"absentDays\":4"), "Tue-Fri 13-16 Oct count as absent");
            check(postAs(base.resolve("/api/schedules/DEMO-004/days"), "{\"from\":\"2026-10-14\",\"dayOff\":true,\"reason\":\"Swapped day\"}", "DEMO-004").statusCode() == 403,
                "employees cannot change their own days");
            check(postAs(base.resolve("/api/schedules/DEMO-004/days"), "{\"from\":\"2026-10-14\",\"dayOff\":true}", "DEMO-003").statusCode() == 400, "a reason is required");
            HttpResponse<String> dayOff = postAs(base.resolve("/api/schedules/DEMO-004/days"), "{\"from\":\"2026-10-14\",\"dayOff\":true,\"reason\":\"Swapped day\"}", "DEMO-003");
            check(dayOff.statusCode() == 201 && getAs(base.resolve(octWeek), "DEMO-003").body().contains("\"absentDays\":3"), "a single day off on Wed 14 Oct: 3 absent");
            HttpResponse<String> monday = postAs(base.resolve("/api/schedules/DEMO-004/days"), "{\"from\":\"2026-10-12\",\"to\":\"2026-10-12\",\"shiftStart\":\"07:00\",\"reason\":\"Inventory\"}", "DEMO-003");
            check(monday.statusCode() == 201 && getAs(base.resolve(octWeek), "DEMO-003").body().contains("\"absentDays\":4"), "an extra 07:00 shift on Mon 12 Oct makes it a workday: 4 absent");
            check(postAs(base.resolve("/api/schedules/DEMO-004/days"), "{\"from\":\"2026-10-01\",\"to\":\"2026-12-01\",\"shiftStart\":\"08:00\",\"reason\":\"Long\"}", "DEMO-003").statusCode() == 400,
                "a day change covers at most 31 days");
            check(getAs(base.resolve("/api/schedules"), "DEMO-003").body().contains("\"from\":\"2026-10-14\",\"to\":\"2026-10-14\",\"shiftStart\":\"\",\"reason\":\"Swapped day\""),
                "the team schedules list the day changes");
            check(getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("You have the day off on 2026-10-14 (Swapped day; set by Noa Reyes)."), "Jae is told about the day off");
            String mondayId = com.google.gson.JsonParser.parseString(monday.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/schedules/days/" + mondayId + "/remove"), "{}", "DEMO-003").statusCode() == 200
                && getAs(base.resolve(octWeek), "DEMO-003").body().contains("\"absentDays\":3"), "removing the Monday change: back to 3 absent");
            check(getAs(base.resolve("/api/schedules"), "DEMO-004").body().contains("\"employeeId\":\"DEMO-004\",\"shiftStart\":\"10:00\""), "employees see their schedule");
            String correction = "{\"employeeId\":\"DEMO-004\",\"attendanceId\":5302,\"timeIn\":\"10:05\",\"timeOut\":\"19:00\",\"reason\":\"Badge reader fault\"}";
            check(postAs(base.resolve("/api/attendance/corrections"), correction, "DEMO-001").statusCode() == 403, "HR cannot edit timesheets");
            check(postAs(base.resolve("/api/attendance/corrections"), correction.replace("Badge reader fault", ""), "DEMO-003").statusCode() == 400, "a correction needs a reason");
            check(postAs(base.resolve("/api/attendance/corrections"), correction.replace("\"DEMO-004\"", "\"DEMO-001\""), "DEMO-003").statusCode() == 404,
                "a correction must match the row's employee");
            HttpResponse<String> corrected = postAs(base.resolve("/api/attendance/corrections"), correction, "DEMO-003");
            check(corrected.statusCode() == 201 && corrected.body().contains("\"originalTimeIn\":\"08:06\"") && corrected.body().contains("\"editedBy\":\"DEMO-003\""),
                "the supervisor corrects a team timesheet and the original is kept");
            check(getAs(base.resolve("/api/attendance/corrections?employeeId=DEMO-004"), "DEMO-004").body().contains("Badge reader fault"), "employees see corrections to their rows");
            // Oct 1-15 for Jae now: Mon 5 (rest day, 480 at 130%), Tue 6 corrected 10:05-19:00 (480), Wed 7 08:00-17:00 (480)
            // = 1,440 min x 148.81 / 60 = 3,571.44 basic + 357.14 premium = 3,928.58.
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10"), "DEMO-003").body().contains("\"grossPay\":\"3928.58\""),
                "pay follows the schedule and the corrected timesheet");
            String merit = "{\"employeeId\":\"DEMO-004\",\"type\":\"MERIT\",\"newMonthlyBasic\":\"27000\",\"effectiveMonth\":\"2026-12\",\"note\":\"Annual review\"}";
            check(postAs(base.resolve("/api/pay-changes"), merit, "DEMO-001").statusCode() == 403, "only the supervisor (or admin) files pay changes");
            check(postAs(base.resolve("/api/pay-changes"), merit.replace("DEMO-004", "DEMO-003"), "DEMO-003").statusCode() == 403, "no pay-change requests for yourself");
            HttpResponse<String> meritRequest = postAs(base.resolve("/api/pay-changes"), merit, "DEMO-003");
            check(meritRequest.statusCode() == 201 && meritRequest.body().contains("\"approverRole\":\"HR\""), "the supervisor files a merit increase");
            String meritId = com.google.gson.JsonParser.parseString(meritRequest.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/pay-changes/" + meritId + "/approve"), "{}", "DEMO-003").statusCode() == 403, "the requester cannot approve");
            check(postAs(base.resolve("/api/pay-changes/" + meritId + "/approve"), "{}", "DEMO-002").statusCode() == 403, "payroll staff do not approve merit increases");
            check(postAs(base.resolve("/api/pay-changes/" + meritId + "/approve"), "{}", "DEMO-001").statusCode() == 200, "HR approves the merit increase");
            HttpResponse<String> december = getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-12"), "DEMO-003");
            check(december.body().contains("\"hourlyRate\":\"160.71\"") && december.body().contains("\"monthlyBasic\":\"27000.00\""), "the raise applies from December");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-11"), "DEMO-003").body().contains("\"hourlyRate\":\"148.81\""), "November keeps the old rate");
            HttpResponse<String> bonusRequest = postAs(base.resolve("/api/pay-changes"),
                "{\"employeeId\":\"DEMO-004\",\"type\":\"BONUS\",\"amount\":\"500\",\"month\":\"2026-12\",\"cutoff\":\"1\",\"note\":\"Safety award\"}", "DEMO-003");
            String bonusId = com.google.gson.JsonParser.parseString(bonusRequest.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/pay-changes/" + bonusId + "/approve"), "{}", "DEMO-001").statusCode() == 403, "HR does not approve bonuses");
            check(postAs(base.resolve("/api/pay-changes/" + bonusId + "/approve"), "{}", "DEMO-002").statusCode() == 200, "payroll staff approve the bonus");
            check(getAs(base.resolve("/api/payroll/adjustments?employeeId=DEMO-004"), "DEMO-002").body().contains("\"description\":\"Safety award\",\"amountCents\":50000"),
                "an approved bonus becomes a payroll earning");
            HttpResponse<String> promotion = postAs(base.resolve("/api/pay-changes"),
                "{\"employeeId\":\"DEMO-001\",\"type\":\"PROMOTION\",\"newPosition\":\"Senior HR Coordinator\",\"newMonthlyBasic\":\"60000\",\"effectiveMonth\":\"2026-12\"}", "DEMO-003");
            String promotionId = com.google.gson.JsonParser.parseString(promotion.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/pay-changes/" + promotionId + "/approve"), "{}", "DEMO-001").statusCode() == 403, "HR cannot approve their own promotion");
            check(postAs(base.resolve("/api/pay-changes/" + promotionId + "/approve"), "{}", "DEMO-005").statusCode() == 200, "the admin approves it instead");
            check(getAs(base.resolve("/api/employees/DEMO-001"), "DEMO-005").body().contains("\"role\":\"Senior HR Coordinator\""), "promotion updates the position");
            HttpResponse<String> discipline = postAs(base.resolve("/api/pay-changes"),
                "{\"employeeId\":\"DEMO-004\",\"type\":\"DISCIPLINARY\",\"startDate\":\"2026-12-02\",\"endDate\":\"2026-12-02\",\"note\":\"Unexcused absence\"}", "DEMO-003");
            String disciplineId = com.google.gson.JsonParser.parseString(discipline.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/pay-changes/" + disciplineId + "/approve"), "{}", "DEMO-001").statusCode() == 200, "HR approves disciplinary unpaid leave");
            check(getAs(base.resolve("/api/workflows"), "DEMO-005").body().contains("\"leaveType\":\"Unpaid\",\"startDate\":\"2026-12-02\""), "it is recorded as approved unpaid leave");
            check(getAs(base.resolve("/api/pay-changes"), "DEMO-004").body().equals("{\"requests\":[]}"), "employees do not see pay-change requests");

            // --- HR: onboarding and offboarding (hand-worked) ---
            // Ana Lim: start 2026-10-01, basic 21,000 (= 125.00/h), allowances 1,500 / 500 / 500, paid by check, reports to Noa.
            String hire = "{\"name\":\"Ana Lim\",\"department\":\"Operations\",\"position\":\"Operations Associate\",\"location\":\"Pasig\",\"startDate\":\"2026-10-01\","
                + "\"supervisorId\":\"DEMO-003\",\"monthlyBasic\":\"21000\",\"riceAllowance\":\"1500\",\"phoneAllowance\":\"500\",\"clothingAllowance\":\"500\",\"payMethod\":\"Check\"}";
            check(postAs(base.resolve("/api/hr/onboard"), hire, "DEMO-004").statusCode() == 403, "employees cannot onboard");
            check(postAs(base.resolve("/api/hr/onboard"), hire.replace("\"Operations\",", "\"Space\","), "DEMO-001").statusCode() == 400, "department must be on the HR list");
            check(postAs(base.resolve("/api/hr/onboard"), hire.replace("DEMO-003", "DEMO-001"), "DEMO-001").statusCode() == 400, "the supervisor must hold the Supervisor role");
            HttpResponse<String> onboarded = postAs(base.resolve("/api/hr/onboard"), hire, "DEMO-001");
            check(onboarded.statusCode() == 201 && onboarded.body().contains("\"task\":\"Payroll set up\",\"done\":true")
                && onboarded.body().contains("\"task\":\"Contract signed\",\"done\":false"), "HR onboards a new hire with a checklist");
            String anaId = com.google.gson.JsonParser.parseString(onboarded.body()).getAsJsonObject().getAsJsonObject("employee").get("id").getAsString();
            check(get(base.resolve("/api/demo/accounts")).body().contains("\"employeeId\":\"" + anaId + "\",\"name\":\"Ana Lim\""), "the new hire gets a demo account");
            check(postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"" + anaId + "\",\"date\":\"2026-10-20\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003").statusCode() == 201
                && postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"" + anaId + "\",\"date\":\"2026-10-30\",\"timeIn\":\"08:00\",\"timeOut\":\"17:00\"}", "DEMO-003").statusCode() == 201,
                "the new hire is on Noa's team and has a schedule for attendance");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=" + anaId + "&month=2026-10"), anaId).body().contains("\"grossPay\":\"2000.00\""),
                "the new hire sees own pay: 16 h x 125.00 = 2,000.00");
            check(postAs(base.resolve("/api/hr/employment/" + anaId + "/tasks"), "{\"task\":\"Contract signed\"}", "DEMO-002").statusCode() == 403, "payroll staff do not run HR checklists");
            check(postAs(base.resolve("/api/hr/employment/" + anaId + "/tasks"), "{\"task\":\"Contract signed\"}", "DEMO-001").statusCode() == 200, "HR ticks a checklist item");
            check(postAs(base.resolve("/api/hr/employment/" + anaId + "/tasks"), "{\"task\":\"Contract signed\"}", "DEMO-001").statusCode() == 409, "an item is ticked once");
            check(postAs(base.resolve("/api/hr/employment/" + anaId + "/tasks"), "{\"task\":\"Free lunch\"}", "DEMO-001").statusCode() == 400, "unknown checklist item");
            check(postAs(base.resolve("/api/hr/employment/DEMO-001/offboard"), "{\"lastDay\":\"2026-10-31\",\"reason\":\"Test\"}", "DEMO-001").statusCode() == 403, "nobody offboards themselves");
            HttpResponse<String> offboarded = postAs(base.resolve("/api/hr/employment/" + anaId + "/offboard"), "{\"lastDay\":\"2026-10-31\",\"reason\":\"Resigned\"}", "DEMO-001");
            check(offboarded.statusCode() == 200 && offboarded.body().contains("\"finalMonth\":\"2026-10\",\"finalCutoff\":2")
                && offboarded.body().contains("\"task\":\"Equipment returned\",\"done\":false"), "HR offboards with a last day and a clearance checklist");
            check(postAs(base.resolve("/api/hr/employment/" + anaId + "/offboard"), "{\"lastDay\":\"2026-10-31\",\"reason\":\"Again\"}", "DEMO-001").statusCode() == 409, "offboarding happens once");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=" + anaId + "&month=2026-10"), anaId).statusCode() == 401, "the account is closed after the last day");
            check(!get(base.resolve("/api/demo/accounts")).body().contains("\"employeeId\":\"" + anaId + "\""), "closed accounts leave the account chooser");
            // Final payslip, Oct 16-31: 2,000.00 - SSS 125 - PhilHealth 125 - Pag-IBIG 20 (2% of 2,000 / 2) + allowances 1,250 = 2,980.00, by check.
            HttpResponse<String> finalSlip = getAs(base.resolve("/api/payroll/payslip?employeeId=" + anaId + "&month=2026-10&cutoff=2"), "DEMO-002");
            check(finalSlip.statusCode() == 200 && finalSlip.body().contains("\"pagIbigCents\":2000") && finalSlip.body().contains("\"netPayCents\":298000")
                && finalSlip.body().contains("\"payMethod\":\"Check\""), "the final payslip covers the last cutoff");
            HttpResponse<String> afterLeaving = getAs(base.resolve("/api/payroll/cutoffs?employeeId=" + anaId + "&month=2026-11"), "DEMO-002");
            check(afterLeaving.body().contains("\"monthlyGross\":\"0.00\"") && !afterLeaving.body().contains("\"holidayDays\":1"),
                "no pay after the last day (no Bonifacio Day holiday pay)");
            check(getAs(base.resolve("/api/employees/" + anaId), "DEMO-001").body().contains("\"status\":\"Separated\""), "the directory shows Separated");
            check(getAs(base.resolve("/api/hr/employment"), "DEMO-003").statusCode() == 403, "supervisors do not see HR records");

            // --- HR tickets ---
            HttpResponse<String> hrTicket = postAs(base.resolve("/api/workflows/tickets"), "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"HR inquiry\",\"summary\":\"How many leave days do I have?\"}", "DEMO-004");
            String hrTicketId = com.google.gson.JsonParser.parseString(hrTicket.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            HttpResponse<String> leadTicket = postAs(base.resolve("/api/workflows/tickets"), "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"Timesheet correction\",\"summary\":\"Missed punch\"}", "DEMO-004");
            String leadTicketId = com.google.gson.JsonParser.parseString(leadTicket.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/workflows/tickets/" + hrTicketId + "/resolve"), "{\"reply\":\"3 Personal days left.\"}", "DEMO-002").statusCode() == 403, "payroll staff do not resolve HR tickets");
            check(postAs(base.resolve("/api/workflows/tickets/" + leadTicketId + "/resolve"), "{\"reply\":\"Done\"}", "DEMO-001").statusCode() == 403, "HR resolves only the HR queue");
            check(postAs(base.resolve("/api/workflows/tickets/" + hrTicketId + "/resolve"), "{\"reply\":\"x\"}", "DEMO-001").statusCode() == 400, "a reply is required");
            HttpResponse<String> resolved = postAs(base.resolve("/api/workflows/tickets/" + hrTicketId + "/resolve"), "{\"reply\":\"3 Personal days left.\"}", "DEMO-001");
            check(resolved.statusCode() == 200 && resolved.body().contains("\"status\":\"Resolved\"") && resolved.body().contains("\"resolvedBy\":\"DEMO-001\""), "HR resolves an HR ticket");
            check(getAs(base.resolve("/api/workflows"), "DEMO-004").body().contains("\"reply\":\"3 Personal days left.\""), "the employee sees the reply");
            check(postAs(base.resolve("/api/workflows/tickets/" + hrTicketId + "/resolve"), "{\"reply\":\"Again\"}", "DEMO-001").statusCode() == 409, "a ticket is resolved once");

            // --- Ticket handlers (design doc: routed by type; statuses Open, In progress, Resolved, Cancelled) ---
            String rescheduleTicket = com.google.gson.JsonParser.parseString(postAs(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"Reschedule request\",\"summary\":\"Swap Friday for Saturday\"}", "DEMO-004").body())
                .getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(getAs(base.resolve("/api/workflows"), "DEMO-003").body().contains("\"ticketType\":\"Reschedule request\",\"queue\":\"Team Lead\""), "a reschedule request goes to the team lead");
            check(postAs(base.resolve("/api/workflows/tickets/" + rescheduleTicket + "/start"), "{}", "DEMO-002").statusCode() == 403, "payroll staff do not handle team-lead tickets");
            HttpResponse<String> started = postAs(base.resolve("/api/workflows/tickets/" + rescheduleTicket + "/start"), "{}", "DEMO-003");
            check(started.statusCode() == 200 && started.body().contains("\"status\":\"In progress\"") && started.body().contains("\"resolvedBy\":\"DEMO-003\""),
                "Jae's supervisor takes the ticket: In progress");
            check(postAs(base.resolve("/api/workflows/tickets/" + rescheduleTicket + "/start"), "{}", "DEMO-003").statusCode() == 409, "a ticket is started once");
            check(postAs(base.resolve("/api/workflows/tickets/" + rescheduleTicket + "/cancel"), "{}", "DEMO-004").statusCode() == 409, "an in-progress ticket can no longer be cancelled");
            check(postAs(base.resolve("/api/workflows/tickets/" + rescheduleTicket + "/resolve"), "{\"reply\":\"Saturday shift set for 14 Nov.\"}", "DEMO-003").body().contains("\"status\":\"Resolved\""),
                "and resolves it");
            check(getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("is now in progress with Noa Reyes"), "Jae is told it is in progress");
            String payrollTicket = com.google.gson.JsonParser.parseString(postAs(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"Payroll inquiry\",\"summary\":\"Rice subsidy missing\"}", "DEMO-004").body())
                .getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(getAs(base.resolve("/api/workflows"), "DEMO-002").body().contains("Rice subsidy missing"), "payroll staff see the tickets in their queue");
            check(postAs(base.resolve("/api/workflows/tickets/" + payrollTicket + "/resolve"), "{\"reply\":\"Added on the next cutoff.\"}", "DEMO-002").statusCode() == 200,
                "payroll staff resolve a payroll inquiry");
            HttpResponse<String> general = postAs(base.resolve("/api/workflows/tickets"), "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"General inquiry\",\"summary\":\"Where is the handbook?\"}", "DEMO-004");
            check(general.statusCode() == 201 && general.body().contains("\"queue\":\"HR\""), "a general inquiry goes to HR");
            String techTicket = com.google.gson.JsonParser.parseString(postAs(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"Technical support\",\"summary\":\"Badge reader broken\"}", "DEMO-004").body())
                .getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/workflows/tickets/" + techTicket + "/resolve"), "{\"reply\":\"x\"}", "DEMO-001").statusCode() == 403
                && postAs(base.resolve("/api/workflows/tickets/" + techTicket + "/resolve"), "{\"reply\":\"Replaced.\"}", "DEMO-005").statusCode() == 200,
                "technical support is for the System Admin");

            // --- Queue position: oldest open ticket first, per queue, counting everyone's tickets ---
            com.google.gson.JsonObject firstInQueue = com.google.gson.JsonParser.parseString(postAs(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"Technical support\",\"summary\":\"Laptop will not start\"}", "DEMO-004").body()).getAsJsonObject().getAsJsonObject("item");
            com.google.gson.JsonObject secondInQueue = com.google.gson.JsonParser.parseString(postAs(base.resolve("/api/workflows/tickets"),
                "{\"employeeId\":\"DEMO-003\",\"ticketType\":\"Technical support\",\"summary\":\"Printer jam\"}", "DEMO-003").body()).getAsJsonObject().getAsJsonObject("item");
            int firstPlace = firstInQueue.get("queuePosition").getAsInt();
            check(secondInQueue.get("queuePosition").getAsInt() == firstPlace + 1, "a new ticket goes to the back of its queue");
            postAs(base.resolve("/api/workflows/tickets/" + firstInQueue.get("id").getAsString() + "/cancel"), "{}", "DEMO-004");
            check(getAs(base.resolve("/api/workflows"), "DEMO-003").body().contains("\"id\":\"" + secondInQueue.get("id").getAsString() + "\"")
                && getAs(base.resolve("/api/workflows"), "DEMO-003").body().contains("\"queuePosition\":" + firstPlace), "when the ticket ahead is cancelled, the next one moves up");
            check(!getAs(base.resolve("/api/workflows"), "DEMO-004").body().matches("(?s).*Laptop will not start[^}]*queuePosition.*"), "a cancelled ticket has no queue position");

            // --- Departments and positions ---
            check(postAs(base.resolve("/api/hr/catalog"), "{\"kind\":\"department\",\"name\":\"Logistics\"}", "DEMO-002").statusCode() == 403, "only HR edits the lists");
            check(postAs(base.resolve("/api/hr/catalog"), "{\"kind\":\"department\",\"name\":\"Logistics\"}", "DEMO-001").statusCode() == 201, "HR adds a department");
            check(postAs(base.resolve("/api/hr/catalog"), "{\"kind\":\"department\",\"name\":\"Logistics\"}", "DEMO-001").statusCode() == 409, "no duplicate departments");
            check(postAs(base.resolve("/api/hr/catalog"), "{\"kind\":\"position\",\"name\":\"Junior Analyst\"}", "DEMO-001").statusCode() == 201, "HR adds a position");
            HttpResponse<String> renamed = postAs(base.resolve("/api/hr/catalog/rename"), "{\"kind\":\"position\",\"from\":\"Junior Analyst\",\"to\":\"Analyst\"}", "DEMO-001");
            check(renamed.statusCode() == 200 && renamed.body().contains("\"employeesUpdated\":0"), "HR renames a position");
            check(getAs(base.resolve("/api/hr/catalog"), "DEMO-004").body().contains("\"Analyst\""), "everyone can read the lists");

            // --- Government details (fake numbers only) ---
            check(getAs(base.resolve("/api/government"), "DEMO-004").statusCode() == 403, "employees have no government-details access (matrix: -)");
            check(getAs(base.resolve("/api/government"), "DEMO-003").statusCode() == 403, "supervisors have no access either");
            check(putAs(base.resolve("/api/government/DEMO-004"), "{\"sss\":\"123\"}", "DEMO-005").statusCode() == 400, "SSS numbers need 10 digits");
            check(putAs(base.resolve("/api/government/DEMO-004"), "{\"sss\":\"12-3456789-0\"}", "DEMO-001").statusCode() == 403, "HR cannot edit directly");
            HttpResponse<String> edited = putAs(base.resolve("/api/government/DEMO-004"), "{\"sss\":\"12-3456789-0\",\"tin\":\"123456789\"}", "DEMO-005");
            check(edited.statusCode() == 200 && edited.body().contains("\"sss\":\"12-3456789-0\"") && edited.body().contains("\"tin\":\"123-456-789\""),
                "the System Admin edits immediately");
            String hrList = getAs(base.resolve("/api/government"), "DEMO-001").body();
            check(hrList.contains("\"sss\":\"••-••••789-0\"") && !hrList.contains("12-3456789-0"), "HR sees masked numbers");
            check(getAs(base.resolve("/api/government/DEMO-004?reveal=true"), "DEMO-001").statusCode() == 403, "full numbers need approval");
            check(postAs(base.resolve("/api/government/DEMO-004/access-requests"), "{\"reason\":\"\"}", "DEMO-001").statusCode() == 400, "an access request needs a reason");
            check(postAs(base.resolve("/api/government/DEMO-001/access-requests"), "{\"reason\":\"Mine\"}", "DEMO-001").statusCode() == 403, "no requests about yourself");
            HttpResponse<String> accessRequest = postAs(base.resolve("/api/government/DEMO-004/access-requests"), "{\"reason\":\"Remittance check\"}", "DEMO-001");
            String accessId = com.google.gson.JsonParser.parseString(accessRequest.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/government/requests/" + accessId + "/approve"), "{}", "DEMO-002").statusCode() == 403, "only the System Admin approves");
            check(postAs(base.resolve("/api/government/requests/" + accessId + "/approve"), "{}", "DEMO-005").statusCode() == 200, "the System Admin approves full view");
            check(getAs(base.resolve("/api/government/DEMO-004?reveal=true"), "DEMO-001").body().contains("\"sss\":\"12-3456789-0\""), "approved HR sees the full number today");
            check(getAs(base.resolve("/api/government/DEMO-004?reveal=true"), "DEMO-002").statusCode() == 403, "the grant is for that requester only");
            HttpResponse<String> change = postAs(base.resolve("/api/government/DEMO-004/change-requests"), "{\"pagIbig\":\"1234-5678-9012\",\"reason\":\"New MID card\"}", "DEMO-002");
            check(change.statusCode() == 201 && change.body().contains("\"pagIbig\":\"••••-••••-9012\""), "payroll staff propose a change (shown masked)");
            String changeId = com.google.gson.JsonParser.parseString(change.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(!getAs(base.resolve("/api/government/DEMO-004?reveal=true"), "DEMO-005").body().contains("1234-5678-9012"), "nothing changes before approval");
            check(postAs(base.resolve("/api/government/requests/" + changeId + "/approve"), "{}", "DEMO-005").statusCode() == 200, "the System Admin approves the change");
            check(getAs(base.resolve("/api/government/DEMO-004?reveal=true"), "DEMO-005").body().contains("\"pagIbig\":\"1234-5678-9012\""), "the approved change is on file");
            HttpResponse<String> adminOwn = postAs(base.resolve("/api/government/DEMO-005/change-requests"), "{\"tin\":\"987654321\"}", "DEMO-002");
            String adminOwnId = com.google.gson.JsonParser.parseString(adminOwn.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/government/requests/" + adminOwnId + "/approve"), "{}", "DEMO-005").statusCode() == 403, "the admin does not approve changes to their own record");
            check(getAs(base.resolve("/api/government/requests"), "DEMO-001").body().contains(accessId) && !getAs(base.resolve("/api/government/requests"), "DEMO-001").body().contains(changeId),
                "HR sees only their own requests");
            String audit = getAs(base.resolve("/api/government/audit"), "DEMO-005").body();
            check(audit.contains("\"actorId\":\"DEMO-001\",\"employeeId\":\"DEMO-004\",\"action\":\"VIEWED_FULL\"") && audit.contains("\"action\":\"EDITED\""), "full views and edits are audited");
            check(getAs(base.resolve("/api/government/audit"), "DEMO-001").statusCode() == 403, "only the System Admin reads the audit log");

            // --- System configuration: statutory rates versions (hand-worked) ---
            String birRows = "[{\"over\":\"0\",\"baseTax\":\"0\",\"ratePercent\":\"0\"},{\"over\":\"250000\",\"baseTax\":\"0\",\"ratePercent\":\"15\"},"
                + "{\"over\":\"400000\",\"baseTax\":\"22500\",\"ratePercent\":\"20\"},{\"over\":\"800000\",\"baseTax\":\"102500\",\"ratePercent\":\"25\"},"
                + "{\"over\":\"2000000\",\"baseTax\":\"402500\",\"ratePercent\":\"30\"},{\"over\":\"8000000\",\"baseTax\":\"2202500\",\"ratePercent\":\"35\"}]";
            String version = "{\"effectiveMonth\":\"2026-12\",\"note\":\"SSS 5.5% test\",\"sss\":{\"employeePercent\":\"5.5\",\"employerPercent\":\"10\",\"minMsc\":\"5000\",\"maxMsc\":\"35000\"},"
                + "\"philHealth\":{\"premiumPercent\":\"5\",\"floor\":\"10000\",\"ceiling\":\"100000\"},"
                + "\"pagIbig\":{\"lowBand\":\"1500\",\"lowPercent\":\"1\",\"highPercent\":\"2\",\"employerPercent\":\"2\",\"cap\":\"10000\"},\"bir\":" + birRows + "}";
            check(getAs(base.resolve("/api/admin/rates"), "DEMO-002").statusCode() == 403, "only the System Admin opens system configuration");
            check(postAs(base.resolve("/api/admin/rates"), version.replace("\"over\":\"0\"", "\"over\":\"100\""), "DEMO-005").statusCode() == 400, "the tax table must start at 0");
            HttpResponse<String> added = postAs(base.resolve("/api/admin/rates"), version, "DEMO-005");
            check(added.statusCode() == 201 && added.body().contains("\"id\":\"PH-2026-admin-v1\"") && added.body().contains("\"employeePercent\":\"5.5\""), "the admin adds a rates version");
            check(postAs(base.resolve("/api/admin/rates"), version, "DEMO-005").statusCode() == 409, "one version per effective month");
            check(getAs(base.resolve("/api/admin/rates?month=2026-11"), "DEMO-005").body().contains("\"inForce\":\"PH-2026-sweldongpinoy-v4\""), "November keeps the built-in rates");
            String preview = "{\"period\":\"2026-12\",\"grossCashPay\":\"25000\",\"sssCompensation\":\"25000\",\"philHealthBasicSalary\":\"25000\",\"pagIbigFundSalary\":\"25000\",\"taxableCompensation\":\"25000\"}";
            // December, SSS 5.5% of MSC 25,000 = 1,375; taxable 25,000 - 1,375 - 625 - 200 = 22,800 x 12 = 273,600 -> 15% of 23,600 = 3,540 / 12 = 295; net 22,505.
            HttpResponse<String> december2 = postAs(base.resolve("/api/payroll/statutory"), preview, "DEMO-002");
            check(december2.body().contains("\"employeeCents\":137500") && december2.body().contains("\"withholdingTaxCents\":29500")
                && december2.body().contains("\"netPayCents\":2250500") && december2.body().contains("\"rulesetId\":\"PH-2026-admin-v1\""), "December uses the new version");
            check(postAs(base.resolve("/api/payroll/statutory"), preview.replace("2026-12", "2026-11"), "DEMO-002").body().contains("\"netPayCents\":2261125"),
                "earlier months keep the built-in result (sweldongpinoy 25,000)");

            // --- Rate changes that wait for approval (design doc: Payroll Staff propose, Pending, the System Admin approves) ---
            String proposal = version.replace("2026-12", "2026-07").replace("SSS 5.5% test", "Proposed SSS change");
            check(postAs(base.resolve("/api/rate-proposals"), proposal, "DEMO-005").statusCode() == 403, "the admin saves versions directly instead of proposing");
            check(postAs(base.resolve("/api/rate-proposals"), proposal, "DEMO-004").statusCode() == 403, "employees cannot propose rate changes");
            check(postAs(base.resolve("/api/rate-proposals"), proposal.replace("\"over\":\"0\"", "\"over\":\"100\""), "DEMO-002").statusCode() == 400, "a proposal is checked when filed");
            HttpResponse<String> proposed = postAs(base.resolve("/api/rate-proposals"), proposal, "DEMO-002");
            check(proposed.statusCode() == 201 && proposed.body().contains("\"status\":\"Pending review\""), "payroll staff file a rate change; it waits for approval");
            String proposalId = com.google.gson.JsonParser.parseString(proposed.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/rate-proposals"), proposal, "DEMO-002").statusCode() == 409, "one pending change per month");
            check(getAs(base.resolve("/api/admin/rates?month=2026-07"), "DEMO-005").body().contains("\"inForce\":\"PH-2026-sweldongpinoy-v4\""), "July rates do not change while pending");
            check(postAs(base.resolve("/api/rate-proposals/" + proposalId + "/approve"), "{}", "DEMO-002").statusCode() == 403, "payroll staff cannot approve it");
            HttpResponse<String> approvedRates = postAs(base.resolve("/api/rate-proposals/" + proposalId + "/approve"), "{}", "DEMO-005");
            check(approvedRates.statusCode() == 200 && approvedRates.body().contains("\"status\":\"Approved\"") && approvedRates.body().contains("Saved as PH-2026-admin-v2, effective 2026-07."),
                "the admin approves: it becomes a rates version");
            check(getAs(base.resolve("/api/admin/rates?month=2026-07"), "DEMO-005").body().contains("\"inForce\":\"PH-2026-admin-v2\""), "July now uses it");
            check(postAs(base.resolve("/api/rate-proposals/" + proposalId + "/reject"), "{}", "DEMO-005").statusCode() == 409, "a request is decided once");
            check(getAs(base.resolve("/api/notifications"), "DEMO-002").body().contains("Your rate change " + proposalId + " for 2026-07 was approved by Lia Torres."), "Eli is told");
            check(getAs(base.resolve("/api/rate-proposals"), "DEMO-002").body().contains("\"versions\":[") , "payroll staff see the proposals and the versions");

            // --- System configuration: roles and reporting lines ---
            check(postAs(base.resolve("/api/admin/roles/DEMO-005"), "{\"role\":\"HR\"}", "DEMO-005").statusCode() == 403, "nobody changes their own role");
            check(postAs(base.resolve("/api/admin/roles/DEMO-003"), "{\"role\":\"EMPLOYEE\"}", "DEMO-005").statusCode() == 409, "a supervisor with a team keeps the role");
            check(postAs(base.resolve("/api/admin/roles/DEMO-002"), "{\"supervisorId\":\"DEMO-004\"}", "DEMO-005").statusCode() == 400, "the supervisor must hold the Supervisor role");
            check(postAs(base.resolve("/api/admin/roles/DEMO-004"), "{\"role\":\"BOSS\"}", "DEMO-005").statusCode() == 400, "unknown role");
            HttpResponse<String> promoted = postAs(base.resolve("/api/admin/roles/DEMO-004"), "{\"role\":\"SUPERVISOR\"}", "DEMO-005");
            check(promoted.statusCode() == 200 && promoted.body().contains("\"before\":\"Employee / DEMO-003\",\"after\":\"Supervisor / DEMO-003\""), "the admin makes Jae a supervisor");
            check(get(base.resolve("/api/demo/accounts")).body().contains("\"employeeId\":\"DEMO-004\",\"name\":\"Jae Cruz\",\"position\":\"Operations Associate\",\"branch\":\"Pasig\",\"role\":\"SUPERVISOR\""),
                "the new role shows at the next sign-in");
            check(postAs(base.resolve("/api/admin/roles/DEMO-003"), "{\"supervisorId\":\"DEMO-004\"}", "DEMO-005").statusCode() == 400, "no reporting loops (Jae reports to Noa)");
            check(postAs(base.resolve("/api/admin/roles/DEMO-004"), "{\"role\":\"EMPLOYEE\"}", "DEMO-005").statusCode() == 200, "and back to Employee");
            check(postJson(base.resolve("/api/admin/roles/DEMO-005"), "{\"role\":\"EMPLOYEE\"}").statusCode() == 409, "the last System Admin cannot be removed");
            check(getAs(base.resolve("/api/admin/roles"), "DEMO-005").body().contains("\"changes\":[{"), "role changes are logged");

            // --- Payslip corrections: VOID (not released) ---
            String noaSlip = "{\"employeeId\":\"DEMO-003\",\"month\":\"2026-10\",\"cutoff\":\"1\"}";
            check(postAs(base.resolve("/api/payroll/payslips"), noaSlip, "DEMO-002").body().contains("\"version\":1"), "payroll finalizes Noa's October 1-15 payslip (version 1)");
            String voidBody = "{\"payslipId\":\"PS-DEMO-003-2026-10-1\",\"reason\":\"Wrong hours entered\"}";
            check(postAs(base.resolve("/api/payroll/corrections"), voidBody, "DEMO-001").statusCode() == 403, "HR does not file payslip corrections");
            HttpResponse<String> voidRequest = postAs(base.resolve("/api/payroll/corrections"), voidBody, "DEMO-002");
            check(voidRequest.statusCode() == 201 && voidRequest.body().contains("\"kind\":\"VOID\""), "an unreleased payslip gets a VOID request");
            check(postAs(base.resolve("/api/payroll/corrections"), voidBody, "DEMO-002").statusCode() == 409, "one pending correction per payslip");
            String voidId = com.google.gson.JsonParser.parseString(voidRequest.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/payroll/corrections/" + voidId + "/approve"), "{}", "DEMO-002").statusCode() == 403, "payroll staff do not approve corrections");
            HttpResponse<String> voidApproved = postAs(base.resolve("/api/payroll/corrections/" + voidId + "/approve"), "{}", "DEMO-005");
            check(voidApproved.statusCode() == 200 && voidApproved.body().contains("Voided as PS-DEMO-003-2026-10-1-void1"), "the System Admin approves the void");
            check(getAs(base.resolve("/api/payroll/payslips/PS-DEMO-003-2026-10-1-void1"), "DEMO-005").body().contains("\"voidReason\":\"Wrong hours entered\""), "the voided payslip is kept");
            check(getAs(base.resolve("/api/payroll/payslips"), "DEMO-005").body().contains("\"payslipId\":\"PS-DEMO-003-2026-10-1-void1\",\"voided\":true"), "history marks it VOID");
            check(postAs(base.resolve("/api/payroll/payslips"), noaSlip, "DEMO-002").body().contains("\"version\":2"), "the cutoff is finalized again as version 2");

            // --- Payslip corrections: ADJUST (released) ---
            // Jae's released Nov 1-15 payslip paid 8 h on Tue 3 Nov (gross 1,190.48 + 1,000 bonus = 2,190.48). Noa corrects the row
            // to 08:00-12:00 (4 h on his 10:00 shift, before the 14:00 meal hour) = 595.24, so gross is now 1,595.24: overpaid by 595.24.
            String novRows = getAs(base.resolve("/api/attendance?employeeId=DEMO-004&from=2026-11-03&to=2026-11-03"), "DEMO-003").body();
            int novRowId = com.google.gson.JsonParser.parseString(novRows).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().get("attendanceId").getAsInt();
            check(postAs(base.resolve("/api/attendance/corrections"), "{\"employeeId\":\"DEMO-004\",\"attendanceId\":" + novRowId
                + ",\"timeIn\":\"08:00\",\"timeOut\":\"12:00\",\"reason\":\"Left at noon\"}", "DEMO-003").statusCode() == 201, "the supervisor corrects the November row");
            String adjustBody = "{\"payslipId\":\"PS-DEMO-004-2026-11-1\",\"reason\":\"Left at noon on 3 Nov\",\"targetMonth\":\"2026-11\",\"targetCutoff\":\"1\"}";
            check(postAs(base.resolve("/api/payroll/corrections"), adjustBody, "DEMO-002").statusCode() == 400, "the difference must go on a later cutoff");
            HttpResponse<String> adjustRequest = postAs(base.resolve("/api/payroll/corrections"), adjustBody.replace("\"targetMonth\":\"2026-11\"", "\"targetMonth\":\"2026-12\""), "DEMO-002");
            check(adjustRequest.statusCode() == 201 && adjustRequest.body().contains("\"kind\":\"ADJUST\""), "a released payslip gets an ADJUST request");
            String adjustId = com.google.gson.JsonParser.parseString(adjustRequest.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            HttpResponse<String> adjusted = postAs(base.resolve("/api/payroll/corrections/" + adjustId + "/approve"), "{}", "DEMO-005");
            check(adjusted.statusCode() == 200 && adjusted.body().contains("Overpaid by ₱595.24") && adjusted.body().contains("\"differenceCents\":-59524"),
                "approval recalculates: overpaid by 595.24");
            check(getAs(base.resolve("/api/payroll/adjustments?employeeId=DEMO-004"), "DEMO-002").body()
                .contains("\"type\":\"Overpayment recovery\",\"description\":\"Correction of PS-DEMO-004-2026-11-1\",\"amountPerCutoffCents\":59524,\"fromMonth\":\"2026-12\",\"toMonth\":\"2026-12\""),
                "the recovery is a one-cutoff deduction in December");
            check(getAs(base.resolve("/api/payroll/payslip?employeeId=DEMO-004&month=2026-12&cutoff=1"), "DEMO-002").body().contains("\"type\":\"Overpayment recovery\""),
                "December 1-15 carries the recovery");
            check(!getAs(base.resolve("/api/payroll/payslip?employeeId=DEMO-004&month=2026-12&cutoff=2"), "DEMO-002").body().contains("Overpayment recovery"),
                "December 16-31 does not");
            check(getAs(base.resolve("/api/payroll/payslips/PS-DEMO-004-2026-11-1"), "DEMO-005").body().contains("\"grossPayCents\":219048"),
                "the released payslip itself is unchanged");
            check(postAs(base.resolve("/api/payroll/corrections/" + adjustId + "/approve"), "{}", "DEMO-005").statusCode() == 409, "a correction is decided once");
            check(getAs(base.resolve("/api/payroll/corrections"), "DEMO-004").statusCode() == 403, "employees do not see payroll corrections");

            // --- Supervisors edit their direct reports' earnings (RBAC matrix: Earnings/Deductibles R,U Team) ---
            String teamEarning = "{\"employeeId\":\"DEMO-004\",\"month\":\"2026-12\",\"cutoff\":\"2\",\"type\":\"Incentive\",\"description\":\"Shift cover\",\"amount\":\"300\"}";
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), teamEarning, "DEMO-003").statusCode() == 201, "Noa adds an incentive for Jae, her direct report");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), teamEarning.replace("DEMO-004", "DEMO-005"), "DEMO-003").statusCode() == 403, "but not for someone outside her team");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), teamEarning.replace("DEMO-004", "DEMO-003"), "DEMO-003").statusCode() == 403, "and never for herself");
            check(postAs(base.resolve("/api/payroll/adjustments/group"), "{\"group\":\"ALL\",\"kind\":\"earning\",\"month\":\"2026-12\",\"cutoff\":\"2\",\"type\":\"Bonus\",\"amount\":\"1\"}", "DEMO-003")
                .statusCode() == 403, "group items stay with Payroll Staff");
            // --- Address and birthdate (RBAC: HR/Admin edit, everyone else reads only their own; synthetic values) ---
            String personal = "{\"firstName\":\"Jae\",\"surname\":\"Cruz\",\"gender\":\"Male\",\"email\":\"jae.personal@example.com\",\"building\":\"Unit 4\",\"street\":\"Sample Street\",\"city\":\"Pasig\",\"region\":\"NCR\",\"postalCode\":\"1600\",\"contactNumber\":\"09170000004\",\"birthdate\":\"1998-05-14\"}";
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal, "DEMO-003").statusCode() == 403, "supervisors cannot change an address");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("1998-05-14", "2020-01-01"), "DEMO-001").statusCode() == 400, "a 6-year-old employee is refused");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal, "DEMO-001").statusCode() == 200, "HR records Jae's address and birthdate");
            check(getAs(base.resolve("/api/personal/DEMO-004"), "DEMO-004").body().contains("\"address\":\"Unit 4, Sample Street, Pasig, NCR 1600\",\"firstName\":\"Jae\",\"surname\":\"Cruz\",\"gender\":\"Male\",\"email\":\"jae.personal@example.com\",\"hasPhoto\":false,\"building\":\"Unit 4\",\"street\":\"Sample Street\",\"city\":\"Pasig\",\"region\":\"NCR\",\"postalCode\":\"1600\",\"contactNumber\":\"09170000004\",\"birthdate\":\"1998-05-14\""),
                "Jae reads his own address (in parts), contact number and birthdate");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("1600", "16"), "DEMO-001").statusCode() == 400, "a postal code has 4 digits");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("\"Male\"", "\"Robot\""), "DEMO-001").statusCode() == 400, "gender is Female, Male or Prefer not to say");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("jae.personal@example.com", "not-an-email"), "DEMO-001").statusCode() == 400, "a personal email is checked");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("\"surname\":\"Cruz\"", "\"surname\":\"Cruz-Santos\""), "DEMO-001").statusCode() == 200
                && getAs(base.resolve("/api/employees/DEMO-004"), "DEMO-001").body().contains("\"name\":\"Jae Cruz-Santos\""), "a new surname updates the directory name");
            check(getAs(base.resolve("/api/changelog?employeeId=DEMO-004"), "DEMO-001").body().contains("\"field\":\"Name\",\"before\":\"Jae Cruz\",\"after\":\"Jae Cruz-Santos\""),
                "and the change history shows it");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal, "DEMO-001").statusCode() == 200, "back to Cruz");
            // Photo: PNG or JPEG only (checked by its bytes), at most 150 KB; HR or the System Admin change it.
            String png = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGNgYPj/HwADAQH/AoHa5wAAAABJRU5ErkJggg==";
            check(putAs(base.resolve("/api/personal/DEMO-004/photo"), "{\"photo\":\"" + png + "\"}", "DEMO-003").statusCode() == 403, "supervisors cannot change a photo");
            check(putAs(base.resolve("/api/personal/DEMO-004/photo"), "{\"photo\":\"data:image/gif;base64,R0lGODlhAQABAAAAACw=\"}", "DEMO-001").statusCode() == 400,
                "a GIF (or anything not PNG/JPEG) is refused");
            String tooBig = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(new byte[160 * 1024]);
            check(putAs(base.resolve("/api/personal/DEMO-004/photo"), "{\"photo\":\"" + tooBig + "\"}", "DEMO-001").statusCode() == 400, "a photo over 150 KB is refused");
            check(putAs(base.resolve("/api/personal/DEMO-004/photo"), "{\"photo\":\"" + png + "\"}", "DEMO-001").body().contains("\"type\":\"image/png\""), "HR uploads Jae's photo");
            HttpRequest photoRequest = HttpRequest.newBuilder(base.resolve("/api/personal/DEMO-004/photo")).GET()
                .header("X-Demo-User", "DEMO-004").header("X-Demo-Session", sessionFor(base, "DEMO-004")).build();
            HttpResponse<byte[]> photo = HTTP.send(photoRequest, HttpResponse.BodyHandlers.ofByteArray());
            check(photo.statusCode() == 200 && "image/png".equals(photo.headers().firstValue("Content-Type").orElse("")) && (photo.body()[1] == 'P'), "Jae sees his photo");
            check(getAs(base.resolve("/api/personal/DEMO-004"), "DEMO-004").body().contains("\"hasPhoto\":true"), "and his details say he has one");
            check(postAs(base.resolve("/api/personal/DEMO-004/photo/remove"), "{}", "DEMO-001").statusCode() == 200
                && getAs(base.resolve("/api/personal/DEMO-004/photo"), "DEMO-004").statusCode() == 404, "HR removes it");

            // System Admin delete: only a record with no history; anyone else is offboarded instead.
            HttpResponse<String> mistake = postAs(base.resolve("/api/employees"), "{\"name\":\"Created By Mistake\",\"department\":\"Operations\",\"role\":\"Operations Associate\","
                + "\"location\":\"Pasig\",\"status\":\"Active\"}", "DEMO-001");
            String mistakeId = com.google.gson.JsonParser.parseString(mistake.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            check(postAs(base.resolve("/api/employees/" + mistakeId + "/delete"), "{\"confirm\":\"DELETE\"}", "DEMO-001").statusCode() == 403, "HR does not delete records");
            check(postAs(base.resolve("/api/employees/" + mistakeId + "/delete"), "{\"confirm\":\"yes\"}", "DEMO-005").statusCode() == 400, "delete needs DELETE typed");
            HttpResponse<String> jaeDelete = postAs(base.resolve("/api/employees/DEMO-004/delete"), "{\"confirm\":\"DELETE\"}", "DEMO-005");
            check(jaeDelete.statusCode() == 409 && jaeDelete.body().contains("attendance") && jaeDelete.body().contains("offboard them instead"),
                "someone with attendance and pay is never deleted: " + jaeDelete.body());
            check(postAs(base.resolve("/api/employees/DEMO-005/delete"), "{\"confirm\":\"DELETE\"}", "DEMO-005").statusCode() == 409, "nobody deletes their own record");
            check(postAs(base.resolve("/api/employees/" + mistakeId + "/delete"), "{\"confirm\":\"DELETE\"}", "DEMO-005").statusCode() == 200
                && getAs(base.resolve("/api/employees/" + mistakeId), "DEMO-005").statusCode() == 404, "the System Admin deletes the record created by mistake");
            check(getAs(base.resolve("/api/changelog?employeeId=" + mistakeId), "DEMO-001").body().contains("\"field\":\"Record\",\"before\":\"Created By Mistake (Operations, Operations Associate)\",\"after\":\"deleted\""),
                "the delete stays in the change history");
            check(putAs(base.resolve("/api/personal/DEMO-004"), personal.replace("09170000004", "12345"), "DEMO-001").statusCode() == 400, "a contact number is a mobile number");
            check(getAs(base.resolve("/api/personal/DEMO-004"), "DEMO-002").statusCode() == 403, "payroll staff read only their own");
            check(getAs(base.resolve("/api/notifications"), "DEMO-004").body().contains("HR updated your personal details"), "Jae is told");
            // Employee change history (database design "Changelog"): field, before, after, who, how.
            String changes = getAs(base.resolve("/api/changelog?employeeId=DEMO-004"), "DEMO-001").body();
            check(changes.contains("\"field\":\"Address\",\"before\":\"\",\"after\":\"Unit 4, Sample Street, Pasig, NCR 1600\",\"by\":\"DEMO-001\""),
                "the change history records the new address and who made it: " + changes);
            check(changes.contains("\"field\":\"Monthly basic from 2026-12\",\"before\":\"25000.00\",\"after\":\"27000.00\""), "and the approved merit increase: " + changes);
            check(getAs(base.resolve("/api/changelog?employeeId=DEMO-004"), "DEMO-004").statusCode() == 200
                && getAs(base.resolve("/api/changelog?employeeId=DEMO-001"), "DEMO-004").statusCode() == 403, "employees read only their own history");
            check(getAs(base.resolve("/api/changelog"), "DEMO-001").body().contains("\"field\":\"Status\",\"before\":\"Active\",\"after\":\"Separated\""),
                "offboarding records the status change");
            // --- Non-taxable allowance correction (design doc: rice subsidy disputed, +200 for that employee) ---
            // Net rises by exactly 200.00; gross, contributions and tax do not change.
            String decSlip = "/api/payroll/payslip?employeeId=DEMO-002&month=2026-10&cutoff=1";
            com.google.gson.JsonObject beforeRice = com.google.gson.JsonParser.parseString(getAs(base.resolve(decSlip), "DEMO-002").body()).getAsJsonObject().getAsJsonObject("payslip");
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), "{\"employeeId\":\"DEMO-002\",\"month\":\"2026-10\",\"cutoff\":\"1\",\"type\":\"Rice subsidy adjustment\","
                + "\"description\":\"Rice subsidy dispute\",\"amount\":\"200\"}", "DEMO-005").statusCode() == 201, "the admin adds a rice subsidy correction for Eli");
            com.google.gson.JsonObject afterRice = com.google.gson.JsonParser.parseString(getAs(base.resolve(decSlip), "DEMO-002").body()).getAsJsonObject().getAsJsonObject("payslip");
            check(afterRice.get("netPayCents").getAsLong() - beforeRice.get("netPayCents").getAsLong() == 20000
                && afterRice.get("grossPayCents").getAsLong() == beforeRice.get("grossPayCents").getAsLong()
                && afterRice.getAsJsonObject("deductions").get("withholdingTaxCents").getAsLong() == beforeRice.getAsJsonObject("deductions").get("withholdingTaxCents").getAsLong(),
                "the correction adds 200.00 to net only, not to gross or tax: " + afterRice.getAsJsonObject("allowances"));
            check(afterRice.getAsJsonObject("allowances").toString().contains("\"type\":\"Rice subsidy adjustment\",\"description\":\"Rice subsidy dispute\",\"amountCents\":20000"),
                "the payslip lists it under allowances");
            // Reimbursements and cash advances (database design "Other transactions"): paid on top of net, not taxed.
            check(postAs(base.resolve("/api/payroll/adjustments/earnings"), "{\"employeeId\":\"DEMO-002\",\"month\":\"2026-10\",\"cutoff\":\"1\",\"type\":\"Reimbursement\","
                + "\"description\":\"Taxi to the branch\",\"amount\":\"300\"}", "DEMO-005").statusCode() == 201, "the admin adds a 300 reimbursement for Eli");
            com.google.gson.JsonObject afterReimbursement = com.google.gson.JsonParser.parseString(getAs(base.resolve(decSlip), "DEMO-005").body()).getAsJsonObject().getAsJsonObject("payslip");
            check(afterReimbursement.get("netPayCents").getAsLong() - afterRice.get("netPayCents").getAsLong() == 30000
                && afterReimbursement.get("grossPayCents").getAsLong() == afterRice.get("grossPayCents").getAsLong(), "net rises by exactly 300.00; gross and tax do not change");
            check(postAs(base.resolve("/api/payroll/adjustments/deductions"), "{\"employeeId\":\"DEMO-004\",\"type\":\"Cash advance repayment\",\"description\":\"Advance AD-1\","
                + "\"amountPerCutoff\":\"250\",\"fromMonth\":\"2026-12\",\"toMonth\":\"2026-12\"}", "DEMO-002").statusCode() == 201, "a cash advance is repaid through a deduction");
            // --- Adjustment forms (design doc: Add / Subtract / Replace, scoped, Pending until approved, before/after kept) ---
            // Jae works 1 Dec, so his 1-15 Dec allowances apply: rice 750, phone 250, clothing 250 per cutoff.
            // Form: replace rice with 1,000 (+250), subtract 100 from phone (-100), add a 50 deduction.
            // That cutoff already carries the 595.24 overpayment recovery, which uses all of Jae's gross (deductions = gross),
            // so the extra 50 has no room (deductions never push net below zero). Expected: allowances +150.00, deductions +0, net +150.00.
            check(postAs(base.resolve("/api/attendance"), "{\"employeeId\":\"DEMO-004\",\"date\":\"2026-12-01\",\"timeIn\":\"10:00\",\"timeOut\":\"19:00\"}", "DEMO-003").statusCode() == 201,
                "Jae has a worked day on 1 Dec");
            String form = "{\"scope\":\"EMPLOYEE:DEMO-004\",\"month\":\"2026-12\",\"cutoff\":\"1\",\"reason\":\"Allowance dispute\",\"lines\":["
                + "{\"component\":\"Rice subsidy\",\"operation\":\"Replace\",\"amount\":\"1000\"},"
                + "{\"component\":\"Phone allowance\",\"operation\":\"Subtract\",\"amount\":\"100\"},"
                + "{\"component\":\"Deduction\",\"operation\":\"Add\",\"amount\":\"50\"}]}";
            check(postAs(base.resolve("/api/payroll/adjustment-forms"), form.replace("\"operation\":\"Add\",\"amount\":\"50\"", "\"operation\":\"Replace\",\"amount\":\"50\""), "DEMO-002")
                .statusCode() == 400, "a deduction can only be added");
            check(postAs(base.resolve("/api/payroll/adjustment-forms"), form, "DEMO-003").statusCode() == 403, "supervisors do not file adjustment forms");
            HttpResponse<String> filedForm = postAs(base.resolve("/api/payroll/adjustment-forms"), form, "DEMO-002");
            check(filedForm.statusCode() == 201 && filedForm.body().contains("\"status\":\"Pending review\""), "payroll staff file the form; it waits for approval");
            String formId = com.google.gson.JsonParser.parseString(filedForm.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            String decSlip1 = "/api/payroll/payslip?employeeId=DEMO-004&month=2026-12&cutoff=1";
            String beforeForm = getAs(base.resolve(decSlip1), "DEMO-002").body();
            check(postAs(base.resolve("/api/payroll/adjustment-forms/" + formId + "/approve"), "{}", "DEMO-002").statusCode() == 403, "nobody approves their own form");
            java.util.function.Function<String, Long> netOf = body -> com.google.gson.JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("payslip").get("netPayCents").getAsLong();
            check(netOf.apply(getAs(base.resolve(decSlip1), "DEMO-002").body()).equals(netOf.apply(beforeForm)), "nothing changes while it is pending");
            HttpResponse<String> approvedForm = postAs(base.resolve("/api/payroll/adjustment-forms/" + formId + "/approve"), "{}", "DEMO-005");
            com.google.gson.JsonObject result = com.google.gson.JsonParser.parseString(approvedForm.body()).getAsJsonObject().getAsJsonObject("item")
                .getAsJsonArray("results").get(0).getAsJsonObject();
            check(approvedForm.statusCode() == 200 && approvedForm.body().contains("Applied to 1 employee(s)."), "the System Admin approves it");
            check(result.get("allowancesAfter").getAsLong() - result.get("allowancesBefore").getAsLong() == 15000
                && result.get("deductionsAfter").getAsLong() == result.get("deductionsBefore").getAsLong()
                && result.get("deductionsBefore").getAsLong() == result.get("grossBefore").getAsLong()
                && result.get("netAfter").getAsLong() - result.get("netBefore").getAsLong() == 15000
                && result.get("grossAfter").getAsLong() == result.get("grossBefore").getAsLong(), "before/after: allowances +150, no room for the deduction, net +150, gross same: " + result);
            check(postAs(base.resolve("/api/payroll/adjustment-forms/" + formId + "/reject"), "{}", "DEMO-005").statusCode() == 409, "a form is decided once");
            HttpResponse<String> teamForm = postAs(base.resolve("/api/payroll/adjustment-forms"),
                "{\"scope\":\"TEAM:DEMO-003\",\"month\":\"2026-12\",\"cutoff\":\"2\",\"reason\":\"Team rice\",\"lines\":[{\"component\":\"Rice subsidy\",\"operation\":\"Add\",\"amount\":\"100\"}]}", "DEMO-002");
            String teamFormId = com.google.gson.JsonParser.parseString(teamForm.body()).getAsJsonObject().getAsJsonObject("item").get("id").getAsString();
            HttpResponse<String> teamApproved = postAs(base.resolve("/api/payroll/adjustment-forms/" + teamFormId + "/approve"), "{}", "DEMO-005");
            check(teamApproved.body().contains("\"employeeId\":\"DEMO-002\",\"skipped\":\"their own pay\""), "a team form skips the person who filed it");
            check(getAs(base.resolve("/api/notifications"), "DEMO-002").body().contains("Your adjustment form " + formId + " was approved by Lia Torres."), "Eli is told");

            // --- Group incentives and deductions (requirement 9) ---
            String teamIncentive = "{\"group\":\"TEAM:DEMO-003\",\"kind\":\"earning\",\"month\":\"2026-12\",\"cutoff\":\"2\",\"type\":\"Incentive\",\"description\":\"Team target met\",\"amount\":\"500\"}";
            check(postAs(base.resolve("/api/payroll/adjustments/group"), teamIncentive, "DEMO-003").statusCode() == 403, "supervisors do not add pay items");
            HttpResponse<String> team = postAs(base.resolve("/api/payroll/adjustments/group"), teamIncentive, "DEMO-002");
            com.google.gson.JsonObject teamResult = com.google.gson.JsonParser.parseString(team.body()).getAsJsonObject();
            check(team.statusCode() == 201 && team.body().contains("\"employeeId\":\"DEMO-001\"") && team.body().contains("\"employeeId\":\"DEMO-004\""),
                "a team incentive is added for each member: " + team.body());
            check(teamResult.getAsJsonArray("skipped").toString().contains("{\"employeeId\":\"DEMO-002\",\"reason\":\"you cannot change your own pay\"}"),
                "the person filing it is skipped, not paid");
            check(getAs(base.resolve("/api/payroll/payslip?employeeId=DEMO-004&month=2026-12&cutoff=2"), "DEMO-002").body().contains("Team target met"),
                "Jae's December 16-31 payslip carries the team incentive");
            HttpResponse<String> branch = postAs(base.resolve("/api/payroll/adjustments/group"), "{\"group\":\"BRANCH:Makati\",\"kind\":\"deduction\",\"type\":\"Insurance\","
                + "\"description\":\"Branch HMO\",\"amountPerCutoff\":\"150\",\"fromMonth\":\"2026-12\",\"toMonth\":\"2026-12\"}", "DEMO-002");
            check(branch.statusCode() == 201 && branch.body().contains("\"employeeId\":\"DEMO-001\"") && branch.body().contains("\"employeeId\":\"DEMO-005\"")
                && !branch.body().contains("\"employeeId\":\"DEMO-004\""), "a branch deduction reaches only that branch (Makati)");
            check(postAs(base.resolve("/api/payroll/adjustments/group"), teamIncentive.replace("TEAM:DEMO-003", "BRANCH:Cebu"), "DEMO-002").statusCode() == 400, "an empty group is refused");
            check(postAs(base.resolve("/api/payroll/adjustments/group"), teamIncentive.replace("TEAM:DEMO-003", "FLOOR:2"), "DEMO-002").statusCode() == 400, "an unknown group kind is refused");
            check(postAs(base.resolve("/api/payroll/adjustments/group"), teamIncentive.replace("Incentive", "Gift"), "DEMO-002").statusCode() == 400
                && !getAs(base.resolve("/api/payroll/adjustments?employeeId=DEMO-001"), "DEMO-002").body().contains("\"type\":\"Gift\""), "a bad type adds nothing");

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
            SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                HttpResponse<String> created = postJson(base.resolve("/api/workflows/leave"),
                    "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"Scoped test\"}",
                    "test-id-token", "user-77");
                check(created.statusCode() == 201 && created.body().contains("user-scoped-firestore"),
                    "authenticated workflow write reports user-scoped Firestore storage");
                check(firestore.paths.size() == 3, "workflow write reads employee, reads leave balance, and creates one request");
                check(firestore.paths.get(0).contains("/users/user-77/demoEmployees"), "employee lookup stays under token UID path");
                check(firestore.paths.get(1).contains("/users/user-77/demoLeaveRequests") && "GET".equals(firestore.methods.get(1)),
                    "leave balance read stays under token UID path");
                check(firestore.paths.get(2).contains("/users/user-77/demoLeaveRequests"), "leave write stays under token UID path");
                check(firestore.paths.stream().allMatch(path -> path.contains("/users/user-77/")), "all Firestore paths use supplied user sandbox");
                check(firestore.authorizations.stream().allMatch(header -> "Bearer test-id-token".equals(header)),
                    "Firebase ID token forwards to each Firestore operation");
                check(firestore.requestBodies.get(2).contains("timestampValue"), "persisted request has typed submission timestamp");
            } finally {
                app.stop();
            }
        }
    }

    private static void firebaseModeScopesAttendanceReportReads() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer()) {
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot());
            SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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
            SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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

    /** A wrong password, waiting out the growing pause the server asks for after the previous one (progressive throttling). */
    private static HttpResponse<String> wrongSignIn(URI base, String employeeId) throws Exception {
        HttpResponse<String> response = startSignIn(base, employeeId, "not-the-password");
        java.util.regex.Matcher wait = java.util.regex.Pattern.compile("wait (\\d+) second").matcher(response.body());
        if (wait.find()) Thread.sleep(Long.parseLong(wait.group(1)) * 1000 + 150);
        return response;
    }

    private static HttpResponse<String> startSignIn(URI base, String employeeId, String password) throws Exception {
        return postJson(base.resolve("/api/auth/2fa/start"), "{\"employeeId\":\"" + employeeId + "\",\"password\":\"" + password + "\"}");
    }

    /** Passwords and lockout, security settings, audit log, backups and restore, payroll policy, branch pages. */
    private static void securityAuditBackupsPolicyAndBranches() throws Exception {
        java.nio.file.Path folder = java.nio.file.Files.createTempDirectory("motorph-security-test");
        AppServer app = new AppServer("", Paths.get("web"), null, folder.resolve("local-demo.json"));
        SESSIONS.clear();
        HttpServer server = app.start("127.0.0.1", 0);
        try {
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            String demo = AccountSecurity.DEFAULT_PASSWORD;

            // --- Passwords and lockout (5 wrong passwords lock the account for 15 minutes by default) ---
            check(startSignIn(base, "DEMO-004", demo).statusCode() == 200, "the demo password passes the first step");
            HttpResponse<String> wrong = wrongSignIn(base, "DEMO-004");
            check(wrong.statusCode() == 401 && wrong.body().contains("4 tries left before the account locks.") && !wrong.body().contains("demoCode"), "a wrong password gets no code");
            HttpResponse<String> second = startSignIn(base, "DEMO-004", "not-the-password");
            check(second.body().contains("wait 1 second before the next try"), "after the 2nd wrong password in a row the server asks for a 1-second wait");
            HttpResponse<String> tooSoon = startSignIn(base, "DEMO-004", demo);
            check(tooSoon.statusCode() == 429 && tooSoon.body().contains("wait 1 second"), "trying again at once is refused, even with the right password");
            Thread.sleep(1150);
            HttpResponse<String> third = startSignIn(base, "DEMO-004", "not-the-password");
            check(third.body().contains("2 tries left") && third.body().contains("wait 2 seconds"), "the wait doubles, and the refused try did not count");
            Thread.sleep(2150);
            wrongSignIn(base, "DEMO-004");
            HttpResponse<String> locked = startSignIn(base, "DEMO-004", "not-the-password");
            check(locked.statusCode() == 423 && locked.body().contains("locked for 15 minutes"), "the 5th wrong password locks the account");
            check(startSignIn(base, "DEMO-004", demo).statusCode() == 423, "even the right password waits while locked");
            check(getAs(base.resolve("/api/admin/security"), "DEMO-005").body().contains("\"employeeId\":\"DEMO-004\",\"name\":\"Jae Cruz\",\"defaultPassword\":true,\"passwordChangedAt\":\"\",\"failedAttempts\":0,\"lockedUntil\":\"20"),
                "the admin sees Jae locked");
            check(postAs(base.resolve("/api/admin/security/DEMO-004/unlock"), "{}", "DEMO-004").statusCode() == 401, "a locked account cannot unlock itself");
            check(postAs(base.resolve("/api/admin/security/DEMO-004/unlock"), "{}", "DEMO-001").statusCode() == 403, "only the System Admin unlocks");
            check(postAs(base.resolve("/api/admin/security/DEMO-004/unlock"), "{}", "DEMO-005").statusCode() == 200
                && startSignIn(base, "DEMO-004", demo).statusCode() == 200, "after the admin unlocks, Jae signs in");

            // --- Changing a password (own account) and an admin reset ---
            String jae = sessionFor(base, "DEMO-004");
            HttpRequest.Builder change = HttpRequest.newBuilder(base.resolve("/api/auth/password")).header("Content-Type", "application/json").header("X-Demo-Session", jae);
            check(HTTP.send(change.POST(HttpRequest.BodyPublishers.ofString("{\"current\":\"" + demo + "\",\"next\":\"short\"}")).build(),
                HttpResponse.BodyHandlers.ofString()).body().contains("8 to 64 characters"), "a new password needs at least 8 characters");
            check(HTTP.send(change.POST(HttpRequest.BodyPublishers.ofString("{\"current\":\"" + demo + "\",\"next\":\"jae-synthetic-pass-1\"}")).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode() == 200, "Jae changes his password");
            check(startSignIn(base, "DEMO-004", demo).statusCode() == 401 && startSignIn(base, "DEMO-004", "jae-synthetic-pass-1").statusCode() == 200,
                "the old password stops working and the new one works");
            // A new sign-in ends the older ones (design doc): sign in again as Jae and the first session stops working.
            String older = jae;
            com.google.gson.JsonObject again2 = com.google.gson.JsonParser.parseString(startSignIn(base, "DEMO-004", "jae-synthetic-pass-1").body()).getAsJsonObject();
            String newer = com.google.gson.JsonParser.parseString(postJson(base.resolve("/api/auth/2fa/verify"), "{\"challengeId\":\"" + again2.get("challengeId").getAsString()
                + "\",\"code\":\"" + again2.get("demoCode").getAsString() + "\"}").body()).getAsJsonObject().get("session").getAsString();
            check(rawAs(base.resolve("/api/employees"), "DEMO-004", older).statusCode() == 401 && rawAs(base.resolve("/api/employees"), "DEMO-004", newer).statusCode() == 200,
                "signing in again ends the older session");
            SESSIONS.put(base.getPort() + "|DEMO-004", newer);
            jae = newer;
            check(getAs(base.resolve("/api/employees"), "DEMO-004").statusCode() == 200, "the session used to change it stays signed in");
            String stored = new String(java.nio.file.Files.readAllBytes(folder.resolve("local-demo.json")), StandardCharsets.UTF_8);
            check(!stored.contains("jae-synthetic-pass-1") && stored.contains("\"hash\""), "the data file keeps only a hash of the password");
            check(postAs(base.resolve("/api/admin/security/DEMO-005/reset-password"), "{}", "DEMO-005").statusCode() == 400, "the admin does not reset their own password here");
            check(postAs(base.resolve("/api/admin/security/DEMO-004/reset-password"), "{}", "DEMO-005").body().contains("\"signedOut\":true"), "the admin resets Jae's password");
            check(rawAs(base.resolve("/api/employees"), "DEMO-004", jae).statusCode() == 401, "a reset signs Jae out everywhere");
            SESSIONS.remove(base.getPort() + "|DEMO-004");
            check(startSignIn(base, "DEMO-004", demo).statusCode() == 200, "after the reset the demo password works again");

            // --- Security settings ---
            String settings = "{\"minPasswordLength\":12,\"maxFailedAttempts\":3,\"lockoutMinutes\":30,\"codeMinutes\":2,\"sessionHours\":4}";
            check(postAs(base.resolve("/api/admin/security"), settings, "DEMO-002").statusCode() == 403, "payroll staff cannot change security settings");
            check(postAs(base.resolve("/api/admin/security"), settings.replace("\"sessionHours\":4", "\"sessionHours\":48"), "DEMO-005").statusCode() == 400, "settings are range-checked");
            check(postAs(base.resolve("/api/admin/security"), settings, "DEMO-005").body().contains("\"minPasswordLength\":12,\"maxFailedAttempts\":3"), "the admin tightens the settings");
            HttpResponse<String> shortCode = startSignIn(base, "DEMO-003", demo);
            String expires = com.google.gson.JsonParser.parseString(shortCode.body()).getAsJsonObject().get("expiresAt").getAsString();
            check(java.time.Duration.between(java.time.LocalTime.now(java.time.ZoneId.of("Asia/Manila")).withSecond(0).withNano(0), java.time.LocalTime.parse(expires)).toMinutes() <= 2,
                "2FA codes now expire after 2 minutes");
            for (int i = 0; i < 2; i++) wrongSignIn(base, "DEMO-001");
            check(startSignIn(base, "DEMO-001", "not-the-password").body().contains("locked for 30 minutes"), "now 3 wrong passwords lock for 30 minutes");
            postAs(base.resolve("/api/admin/security/DEMO-001/unlock"), "{}", "DEMO-005");

            // --- Audit log: changes and sign-in attempts, never the request body ---
            HttpResponse<String> audit = getAs(base.resolve("/api/admin/audit?actor=DEMO-004&limit=1000"), "DEMO-005");
            check(audit.statusCode() == 200 && audit.body().contains("\"path\":\"/api/auth/2fa/start\",\"target\":\"DEMO-004\",\"status\":423")
                && audit.body().contains("\"path\":\"/api/auth/2fa/start\",\"target\":\"DEMO-004\",\"status\":401"), "failed sign-ins are audited with the account");
            check(audit.body().contains("\"actor\":\"DEMO-005\",\"method\":\"POST\",\"path\":\"/api/admin/security/DEMO-004/reset-password\",\"target\":\"DEMO-004\",\"status\":200"),
                "the admin's password reset is audited");
            check(!audit.body().contains("jae-synthetic-pass-1") && !audit.body().contains(demo), "no password ever appears in the audit log");
            check(getAs(base.resolve("/api/admin/audit?failures=true"), "DEMO-005").body().contains("\"status\":403")
                && !getAs(base.resolve("/api/admin/audit?failures=true"), "DEMO-005").body().contains("\"status\":200"), "the log filters to refused requests");
            check(getAs(base.resolve("/api/admin/audit"), "DEMO-001").statusCode() == 403, "only the System Admin reads the audit log");

            // --- Payroll policy (defaults = the mentor's rules) ---
            // Jae's basic 25,000 / 168 = 148.81; with 160 hours a month: 25,000 / 160 = 156.25.
            check(getAs(base.resolve("/api/admin/payroll-policy"), "DEMO-005").body().contains("\"graceMinutes\":10,\"hoursPerMonth\":168,\"dailyHours\":8,\"nightDifferentialPercent\":10"),
                "the policy starts at the mentor's rules");
            String policy = "{\"graceMinutes\":0,\"hoursPerMonth\":160,\"dailyHours\":8,\"nightDifferentialPercent\":10}";
            check(postAs(base.resolve("/api/admin/payroll-policy"), policy, "DEMO-002").statusCode() == 403, "payroll staff cannot change the policy");
            check(postAs(base.resolve("/api/admin/payroll-policy"), policy.replace("160", "120"), "DEMO-005").statusCode() == 400, "the policy is range-checked");
            check(postAs(base.resolve("/api/admin/payroll-policy"), policy, "DEMO-005").body().contains("\"mentorDefaults\":false"), "the admin changes the policy");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10"), "DEMO-002").body().contains("\"hourlyRate\":\"156.25\""),
                "the hourly rate follows the new divisor");
            // Mara, 1-15 Oct, with no grace: 08:01 and 08:03 are now tardy (07:58 is not).
            check(getAs(base.resolve("/api/attendance?employeeId=DEMO-001&from=2026-10-01&to=2026-10-15"), "DEMO-001").body().contains("\"tardyDays\":2,\"tardyMinutes\":4"),
                "with no grace period, 08:01 and 08:03 count as tardy (1 + 3 minutes)");
            check(postAs(base.resolve("/api/admin/payroll-policy"), "{\"graceMinutes\":10,\"hoursPerMonth\":168,\"dailyHours\":8,\"nightDifferentialPercent\":10}", "DEMO-005")
                .body().contains("\"mentorDefaults\":true"), "back to the mentor's rules");
            check(getAs(base.resolve("/api/payroll/cutoffs?employeeId=DEMO-004&month=2026-10"), "DEMO-002").body().contains("\"hourlyRate\":\"148.81\""), "and 148.81 again");

            // --- Branch pages ---
            HttpResponse<String> jaeBranch = getAs(base.resolve("/api/branches"), "DEMO-004");
            check(jaeBranch.body().contains("\"mine\":\"Pasig\"") && jaeBranch.body().contains("\"name\":\"Pasig\",\"headcount\":1") && !jaeBranch.body().contains("Makati"),
                "an employee sees only their own branch");
            check(getAs(base.resolve("/api/branches"), "DEMO-001").body().contains("\"name\":\"Makati\",\"headcount\":2"), "HR sees every branch (Makati: Mara and Lia)");

            // --- Backups and restore ---
            HttpResponse<String> backup = postAs(base.resolve("/api/admin/backups"), "{}", "DEMO-005");
            check(backup.statusCode() == 201 && backup.body().contains("-manual.json"), "the admin backs up now");
            String backupName = com.google.gson.JsonParser.parseString(backup.body()).getAsJsonObject().get("backup").getAsString();
            check(getAs(base.resolve("/api/admin/backups"), "DEMO-005").body().contains(backupName) && getAs(base.resolve("/api/admin/backups"), "DEMO-005").body().contains("-auto.json"),
                "backups list the manual one and the automatic first one");
            check(postAs(base.resolve("/api/workflows/tickets"), "{\"employeeId\":\"DEMO-004\",\"ticketType\":\"HR inquiry\",\"summary\":\"After the backup\"}", "DEMO-004").statusCode() == 201,
                "a ticket is filed after the backup");
            check(postAs(base.resolve("/api/admin/backups/restore"), "{\"name\":\"" + backupName + "\",\"confirm\":\"yes\"}", "DEMO-005").statusCode() == 400, "restore needs RESTORE typed");
            check(postAs(base.resolve("/api/admin/backups/restore"), "{\"name\":\"../local-demo.json\",\"confirm\":\"RESTORE\"}", "DEMO-005").statusCode() == 404, "only backup files can be restored");
            HttpResponse<String> restored = postAs(base.resolve("/api/admin/backups/restore"), "{\"name\":\"" + backupName + "\",\"confirm\":\"RESTORE\"}", "DEMO-005");
            check(restored.statusCode() == 200 && restored.body().contains("-before-restore.json"), "restore keeps a safety backup of the current data first");
            check(!getAs(base.resolve("/api/workflows"), "DEMO-004").body().contains("After the backup"), "the ticket filed after the backup is gone");
            check(getAs(base.resolve("/api/admin/audit?path=/api/admin/backups/restore"), "DEMO-005").body().contains("\"status\":200"), "the restore itself is audited");

            // --- Recovery: a damaged data file is replaced by the newest backup at the next start ---
            app.stop();
            java.nio.file.Files.write(folder.resolve("local-demo.json"), "{not json".getBytes(StandardCharsets.UTF_8));
            AppServer recovered = new AppServer("", Paths.get("web"), null, folder.resolve("local-demo.json"));
            SESSIONS.clear();
            server = recovered.start("127.0.0.1", 0);
            app = recovered;
            URI again = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            check(getAs(again.resolve("/api/admin/security"), "DEMO-005").body().contains("\"minPasswordLength\":12"),
                "after a damaged data file, the server starts from the newest backup (the admin's settings are back)");
        } finally {
            app.stop();
            PayrollPolicy.importValues(null);
        }
    }

    private static void firebaseModeActsAsASandboxPerson() throws Exception {
        try (FakeFirestoreServer firestore = new FakeFirestoreServer()) {
            firestore.extraEmployee = true;
            AppServer app = new AppServer("demo-project", Paths.get("web"), firestore.apiRoot());
            SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
            HttpServer server = app.start("127.0.0.1", 0);
            try {
                URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
                check(get(base.resolve("/api/demo/accounts")).statusCode() == 401, "Firebase account list needs the Firebase sign-in");
                HttpResponse<String> accounts = get(base.resolve("/api/demo/accounts"), "test-id-token", "user-77");
                check(accounts.statusCode() == 200 && accounts.body().contains("\"employeeId\":\"DEMO-001\",\"name\":\"Mara Rivera\"")
                    && accounts.body().contains("\"role\":\"HR\"") && accounts.body().contains("\"employeeId\":\"DEMO-004\""),
                    "Firebase account list is the people in the caller's own sandbox, with their roles");
                check(firestore.paths.stream().allMatch(path -> path.contains("/users/user-77/")), "account list reads only the caller's sandbox");
                check(getAs(base.resolve("/api/employees"), "test-id-token", "user-77", "DEMO-009").statusCode() == 401,
                    "acting as someone outside the sandbox is refused");
                String range = "&from=2026-10-01&to=2026-10-15";
                check(getAs(base.resolve("/api/attendance?employeeId=DEMO-004" + range), "test-id-token", "user-77", "DEMO-004").statusCode() == 200,
                    "acting as an employee, their own attendance opens");
                check(getAs(base.resolve("/api/attendance?employeeId=DEMO-001" + range), "test-id-token", "user-77", "DEMO-004").statusCode() == 403,
                    "an employee cannot open a teammate's attendance");
                check(getAs(base.resolve("/api/attendance?employeeId=DEMO-004" + range), "test-id-token", "user-77", "DEMO-001").statusCode() == 200,
                    "acting as HR, anyone's attendance opens");
                int before = firestore.paths.size();
                HttpResponse<String> leaveForMara = postAs(base.resolve("/api/workflows/leave"),
                    "{\"employeeId\":\"DEMO-001\",\"leaveType\":\"Vacation\",\"startDate\":\"2026-11-10\",\"endDate\":\"2026-11-10\",\"note\":\"x\"}",
                    "test-id-token", "user-77", "DEMO-004");
                check(leaveForMara.statusCode() == 403, "an employee cannot file leave for someone else");
                check(firestore.methods.subList(before, firestore.methods.size()).stream().noneMatch("POST"::equals), "nothing is written when refused");
            } finally {
                app.stop();
            }
        }
    }

    private static HttpResponse<String> getAs(URI uri, String token, String uid, String demoUser) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid).header("X-Demo-User", demoUser).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postAs(URI uri, String json, String token, String uid, String demoUser) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + token).header("X-Firebase-UID", uid).header("X-Demo-User", demoUser)
            .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void staticFilesExposeDefensiveHeadersAndRejectTraversal() throws Exception {
        AppServer app = new AppServer("", Paths.get("web"));
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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
            HttpResponse<String> leaveApprove = postJson(base.resolve("/api/workflows/leave/LV-FFFFFFFFFFFF/approve"), "{\"reviewerId\":\"DEMO-002\"}");
            check(leaveApprove.statusCode() == 403 && leaveApprove.body().contains("local-demo only"),
                "leave approval is blocked in Firebase mode");
            check(get(base.resolve("/api/payroll/payslips")).statusCode() == 403, "saved payroll history is blocked in Firebase mode");
            check(get(base.resolve("/api/attendance/clock")).statusCode() == 403, "clock in/out is blocked in Firebase mode");
            HttpResponse<String> overtimeFirebase = postJson(base.resolve("/api/workflows/overtime"), "{\"employeeId\":\"DEMO-001\",\"date\":\"2026-11-03\",\"minutes\":30}");
            check(overtimeFirebase.statusCode() == 403 && overtimeFirebase.body().contains("local-demo only"), "overtime requests are blocked in Firebase mode");
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
        SESSIONS.clear(); // a new server has no sessions (it may reuse a port)
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


    private static HttpResponse<String> rawAs(URI uri, String demoUser, String session) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json").header("X-Demo-User", demoUser);
        if (!session.isEmpty()) request.header("X-Demo-Session", session);
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    static final java.util.Map<String, String> SESSIONS = new java.util.HashMap<>();

    /** Signs in with the demo 2FA (start, then verify with the shown code) once per server and account; "" if refused. */
    static String sessionFor(URI uri, String demoUser) throws Exception {
        String key = uri.getPort() + "|" + demoUser;
        if (SESSIONS.containsKey(key)) return SESSIONS.get(key);
        URI base = URI.create(uri.getScheme() + "://" + uri.getAuthority());
        HttpResponse<String> started = HTTP.send(HttpRequest.newBuilder(base.resolve("/api/auth/2fa/start")).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"employeeId\":\"" + demoUser + "\",\"password\":\"" + AccountSecurity.DEFAULT_PASSWORD + "\"}", StandardCharsets.UTF_8)).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (started.statusCode() != 200) return "";
        com.google.gson.JsonObject challenge = com.google.gson.JsonParser.parseString(started.body()).getAsJsonObject();
        HttpResponse<String> verified = HTTP.send(HttpRequest.newBuilder(base.resolve("/api/auth/2fa/verify")).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"challengeId\":\"" + challenge.get("challengeId").getAsString() + "\",\"code\":\""
                + challenge.get("demoCode").getAsString() + "\"}", StandardCharsets.UTF_8)).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String session = com.google.gson.JsonParser.parseString(verified.body()).getAsJsonObject().get("session").getAsString();
        SESSIONS.put(key, session);
        return session;
    }

    private static HttpResponse<String> putAs(URI uri, String json, String demoUser) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").header("Content-Type", "application/json")
            .header("X-Demo-User", demoUser).header("X-Demo-Session", sessionFor(uri, demoUser)).PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> getAs(URI uri, String demoUser) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).GET().header("Accept", "application/json").header("X-Demo-User", demoUser)
            .header("X-Demo-Session", sessionFor(uri, demoUser)).build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static HttpResponse<String> postAs(URI uri, String json, String demoUser) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri).header("Accept", "application/json").header("Content-Type", "application/json")
            .header("X-Demo-User", demoUser).header("X-Demo-Session", sessionFor(uri, demoUser)).POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8)).build();
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
        private boolean extraEmployee;

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
                response = "{\"documents\":[" + EMPLOYEE_DOCUMENT + (extraEmployee ? "," + EMPLOYEE_DOCUMENT.replace("DEMO-001", "DEMO-004")
                    .replace("Mara Rivera", "Jae Cruz") : "") + "]}";
            } else if ("GET".equals(exchange.getRequestMethod()) && path.contains("/demoLeaveRequests")) {
                response = "{\"documents\":[]}";
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
