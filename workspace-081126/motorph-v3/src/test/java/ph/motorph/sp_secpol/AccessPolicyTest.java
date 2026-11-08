package ph.motorph.sp_secpol;

import ph.motorph.o_oop.Employee;
import ph.motorph.sp_secpol.AccessPolicy;
import ph.motorph.sp_secpol.AccessPolicy.Actor;
import ph.motorph.sp_secpol.AccessPolicy.Area;
import ph.motorph.sp_secpol.AccessPolicy.Role;

/** Checks the RBAC rules against the user's permissions matrix and role descriptions. */
public final class AccessPolicyTest {
    private static int checks;
    private static final Actor HR = new Actor("10006", Role.HR, true);
    private static final Actor PAYROLL = new Actor("10011", Role.PAYROLL, true);
    private static final Actor SUPERVISOR = new Actor("10015", Role.SUPERVISOR, true);
    private static final Actor EMPLOYEE = new Actor("10018", Role.EMPLOYEE, true);
    private static final Actor ADMIN = new Actor("10005", Role.ADMIN, true);

    public static void main(String[] args) {
        // Roles and the reporting tree come from MotorPH's employee file, exactly as the application binds them.
        AccessPolicy.bind(new ph.motorph.o_oop.OrgChart(ph.motorph.o_oop.EmployeeImport.fromClasspath().employees()));
        check(AccessPolicy.roleOf("10006") == Role.HR && AccessPolicy.roleOf("10005") == Role.ADMIN, "roles come from the file");
        check(AccessPolicy.roleOf("99999") == Role.EMPLOYEE, "new employees default to the Employee role");
        // 10015 is the account manager: self plus both team leaders plus everyone beneath them.
        check(AccessPolicy.teamOf("10015").containsAll(java.util.Arrays.asList("10015", "10016", "10017", "10018")),
            "a supervisor's team is themselves and every descendant, recursively");
        check(AccessPolicy.teamOf("10015").size() == 17, "the account manager reaches 17 people in total");
        // 10018 is rank and file: their team is themselves and their own reports (none), never a sibling's chain.
        check(AccessPolicy.teamOf("10018").size() == 1 && AccessPolicy.teamOf("10018").contains("10018"),
            "a rank-and-file employee's team is themselves alone");
        check(AccessPolicy.teamOf("10001").size() == 34, "the CEO's team is the whole company");
        check(!AccessPolicy.teamOf("10004").contains("10018"), "the CMO's scope stays inside the marketing chain");

        // Reads (matrix: Employee R Self; Supervisor R,U Team; Payroll R all attendance; HR R).
        check(AccessPolicy.canRead(EMPLOYEE, Area.ATTENDANCE, "10018") && !AccessPolicy.canRead(EMPLOYEE, Area.ATTENDANCE, "10006"), "employee sees only own attendance");
        // TEAM means self plus descendants. A rank-and-file employee has nobody beneath them, so their own leave is
        // theirs to read and a colleague's is not: least privilege, not a shared department inbox.
        check(AccessPolicy.canRead(EMPLOYEE, Area.LEAVE, "10018"), "employee can read their own leave");
        check(!AccessPolicy.canRead(EMPLOYEE, Area.LEAVE, "10019") && !AccessPolicy.canRead(EMPLOYEE, Area.LEAVE, "10006"),
            "employee cannot read a colleague's or the HR manager's leave");
        check(AccessPolicy.canRead(EMPLOYEE, Area.EMPLOYEES, "10018") && !AccessPolicy.canRead(EMPLOYEE, Area.EMPLOYEES, "10005"), "employee sees their own record only");
        // A supervisor, by contrast, reaches their whole chain.
        check(AccessPolicy.canRead(SUPERVISOR, Area.LEAVE, "10018") && AccessPolicy.canRead(SUPERVISOR, Area.LEAVE, "10016"),
            "a supervisor reads the leave of everyone beneath them");
        check(!AccessPolicy.canRead(EMPLOYEE, Area.PAYROLL, "10006") && AccessPolicy.canRead(EMPLOYEE, Area.PAYROLL, "10018"), "employee sees only own pay");
        check(AccessPolicy.canRead(SUPERVISOR, Area.PAYROLL, "10018") && !AccessPolicy.canRead(SUPERVISOR, Area.PAYROLL, "10005"), "supervisor reads team payslips only");
        check(AccessPolicy.canRead(PAYROLL, Area.ATTENDANCE, "10005") && AccessPolicy.canRead(HR, Area.PAYROLL, "10005"), "payroll and HR read everyone's attendance and payroll");
        check(!AccessPolicy.canRead(PAYROLL, Area.TICKETS, "10018"), "payroll staff see only their own tickets");
        deny(() -> AccessPolicy.requireArea(EMPLOYEE, Area.REPORTS), "employees cannot open reports");
        deny(() -> AccessPolicy.requireArea(HR, Area.STATUTORY), "HR cannot run the statutory calculator");
        AccessPolicy.requireArea(PAYROLL, Area.STATUTORY);
        check(true, "payroll staff can run the statutory calculator");

        // Writes.
        deny(() -> AccessPolicy.requireEmployeeEdit(SUPERVISOR), "only HR/admin edit employee records");
        AccessPolicy.requireEmployeeEdit(HR);
        // Editing attendance follows the direct line: 10015 leads 10016 and 10017, and 10018 sits one level lower.
        AccessPolicy.requireAttendanceEdit(SUPERVISOR, "10016");
        check(true, "supervisor edits a direct report's attendance");
        deny(() -> AccessPolicy.requireAttendanceEdit(SUPERVISOR, "10005"), "supervisor cannot edit someone outside the team");
        deny(() -> AccessPolicy.requireAttendanceEdit(SUPERVISOR, "10018"),
            "a supervisor does not rewrite the attendance of a report's report");
        deny(() -> AccessPolicy.requireAttendanceEdit(HR, "10018"), "HR only reads attendance");
        deny(() -> AccessPolicy.requireCreate(EMPLOYEE, Area.LEAVE, "10006"), "employees file leave only for themselves");
        AccessPolicy.requireCreate(HR, Area.LEAVE, "10018");
        deny(() -> AccessPolicy.requireCreate(HR, Area.OVERTIME, "10018"), "HR cannot file overtime for others");

        // Reviews: never your own; leave by supervisor/HR/admin; overtime by supervisor/admin.
        deny(() -> AccessPolicy.requireReview(SUPERVISOR, Area.LEAVE, "10015"), "no self-approval");
        AccessPolicy.requireReview(SUPERVISOR, Area.LEAVE, "10016");
        check(true, "a supervisor approves a direct report's leave");
        deny(() -> AccessPolicy.requireReview(SUPERVISOR, Area.LEAVE, "10018"),
            "a supervisor does not approve leave one level further down");
        AccessPolicy.requireReview(HR, Area.LEAVE, "10018");
        check(true, "HR approves leave company-wide");
        deny(() -> AccessPolicy.requireReview(PAYROLL, Area.LEAVE, "10018"), "payroll staff cannot approve leave");
        deny(() -> AccessPolicy.requireReview(HR, Area.OVERTIME, "10018"), "HR cannot approve overtime");
        deny(() -> AccessPolicy.requireReview(EMPLOYEE, Area.OVERTIME, "10006"), "employees cannot approve overtime");
        AccessPolicy.requireReview(ADMIN, Area.OVERTIME, "10018");
        check(true, "admin approves overtime");

        deny(() -> AccessPolicy.requireFinalize(PAYROLL, "10011"), "payroll staff cannot finalize their own payslip");
        deny(() -> AccessPolicy.requireFinalize(HR, "10018"), "HR cannot finalize payslips");
        AccessPolicy.requireFinalize(PAYROLL, "10018");
        check(true, "payroll staff finalize others' payslips");
        AccessPolicy.requireScheduleEdit(SUPERVISOR, "10016");
        deny(() -> AccessPolicy.requireScheduleEdit(HR, "10018"), "HR does not manage team schedules");
        deny(() -> AccessPolicy.requirePayChangeRequest(EMPLOYEE, "10006"), "employees cannot file pay changes");
        AccessPolicy.requirePayChangeReview(PAYROLL, "BONUS", "10018", "10015");
        AccessPolicy.requirePayChangeReview(HR, "MERIT", "10018", "10015");
        deny(() -> AccessPolicy.requirePayChangeReview(HR, "BONUS", "10018", "10015"), "bonuses are approved by payroll staff");
        deny(() -> AccessPolicy.requirePayChangeReview(PAYROLL, "PROMOTION", "10018", "10015"), "promotions are approved by HR");
        deny(() -> AccessPolicy.requirePayChangeReview(ADMIN, "MERIT", "10018", "10005"), "the requester never decides");
        check(true, "pay-change approvals split between HR and payroll");
        System.out.println("PASS: " + checks + " access-policy checks");
    }

    private static void deny(Runnable action, String message) {
        try {
            action.run();
        } catch (AccessPolicy.AccessDenied expected) {
            check(true, message);
            return;
        }
        throw new AssertionError(message + ": was allowed");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
