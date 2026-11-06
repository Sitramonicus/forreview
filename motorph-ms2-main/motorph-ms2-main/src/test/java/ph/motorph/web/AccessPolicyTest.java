package ph.motorph.web;

import ph.motorph.web.AccessPolicy.Actor;
import ph.motorph.web.AccessPolicy.Area;
import ph.motorph.web.AccessPolicy.Role;

/** Checks the RBAC rules against the user's permissions matrix and role descriptions. */
public final class AccessPolicyTest {
    private static int checks;
    private static final Actor HR = new Actor("DEMO-001", Role.HR, true);
    private static final Actor PAYROLL = new Actor("DEMO-002", Role.PAYROLL, true);
    private static final Actor SUPERVISOR = new Actor("DEMO-003", Role.SUPERVISOR, true);
    private static final Actor EMPLOYEE = new Actor("DEMO-004", Role.EMPLOYEE, true);
    private static final Actor ADMIN = new Actor("DEMO-005", Role.ADMIN, true);

    public static void main(String[] args) {
        check(AccessPolicy.roleOf("DEMO-001") == Role.HR && AccessPolicy.roleOf("DEMO-005") == Role.ADMIN, "demo roles");
        check(AccessPolicy.roleOf("DEMO-ABCDEF123456") == Role.EMPLOYEE, "new employees default to the Employee role");
        check(AccessPolicy.teamOf("DEMO-003").containsAll(java.util.Arrays.asList("DEMO-001", "DEMO-002", "DEMO-004")), "supervisor's team is the direct reports");
        check(AccessPolicy.teamOf("DEMO-004").contains("DEMO-003") && AccessPolicy.teamOf("DEMO-004").contains("DEMO-001")
            && !AccessPolicy.teamOf("DEMO-004").contains("DEMO-005"), "an employee's team is their supervisor and teammates");

        // Reads (matrix: Employee R Self; Supervisor R,U Team; Payroll R all attendance; HR R).
        check(AccessPolicy.canRead(EMPLOYEE, Area.ATTENDANCE, "DEMO-004") && !AccessPolicy.canRead(EMPLOYEE, Area.ATTENDANCE, "DEMO-001"), "employee sees only own attendance");
        check(AccessPolicy.canRead(EMPLOYEE, Area.LEAVE, "DEMO-001"), "employee can check team leave");
        check(AccessPolicy.canRead(EMPLOYEE, Area.EMPLOYEES, "DEMO-002") && !AccessPolicy.canRead(EMPLOYEE, Area.EMPLOYEES, "DEMO-005"), "employee sees team members only");
        check(!AccessPolicy.canRead(EMPLOYEE, Area.PAYROLL, "DEMO-001") && AccessPolicy.canRead(EMPLOYEE, Area.PAYROLL, "DEMO-004"), "employee sees only own pay");
        check(AccessPolicy.canRead(SUPERVISOR, Area.PAYROLL, "DEMO-004") && !AccessPolicy.canRead(SUPERVISOR, Area.PAYROLL, "DEMO-005"), "supervisor reads team payslips only");
        check(AccessPolicy.canRead(PAYROLL, Area.ATTENDANCE, "DEMO-005") && AccessPolicy.canRead(HR, Area.PAYROLL, "DEMO-005"), "payroll and HR read everyone's attendance and payroll");
        check(!AccessPolicy.canRead(PAYROLL, Area.TICKETS, "DEMO-004"), "payroll staff see only their own tickets");
        deny(() -> AccessPolicy.requireArea(EMPLOYEE, Area.REPORTS), "employees cannot open reports");
        deny(() -> AccessPolicy.requireArea(HR, Area.STATUTORY), "HR cannot run the statutory calculator");
        AccessPolicy.requireArea(PAYROLL, Area.STATUTORY);
        check(true, "payroll staff can run the statutory calculator");

        // Writes.
        deny(() -> AccessPolicy.requireEmployeeEdit(SUPERVISOR), "only HR/admin edit employee records");
        AccessPolicy.requireEmployeeEdit(HR);
        AccessPolicy.requireAttendanceEdit(SUPERVISOR, "DEMO-004");
        check(true, "supervisor edits a direct report's attendance");
        deny(() -> AccessPolicy.requireAttendanceEdit(SUPERVISOR, "DEMO-005"), "supervisor cannot edit someone outside the team");
        deny(() -> AccessPolicy.requireAttendanceEdit(HR, "DEMO-004"), "HR only reads attendance");
        deny(() -> AccessPolicy.requireCreate(EMPLOYEE, Area.LEAVE, "DEMO-001"), "employees file leave only for themselves");
        AccessPolicy.requireCreate(HR, Area.LEAVE, "DEMO-004");
        deny(() -> AccessPolicy.requireCreate(HR, Area.OVERTIME, "DEMO-004"), "HR cannot file overtime for others");

        // Reviews: never your own; leave by supervisor/HR/admin; overtime by supervisor/admin.
        deny(() -> AccessPolicy.requireReview(SUPERVISOR, Area.LEAVE, "DEMO-003"), "no self-approval");
        AccessPolicy.requireReview(SUPERVISOR, Area.LEAVE, "DEMO-004");
        AccessPolicy.requireReview(HR, Area.LEAVE, "DEMO-004");
        check(true, "supervisor and HR approve leave");
        deny(() -> AccessPolicy.requireReview(PAYROLL, Area.LEAVE, "DEMO-004"), "payroll staff cannot approve leave");
        deny(() -> AccessPolicy.requireReview(HR, Area.OVERTIME, "DEMO-004"), "HR cannot approve overtime");
        deny(() -> AccessPolicy.requireReview(EMPLOYEE, Area.OVERTIME, "DEMO-001"), "employees cannot approve overtime");
        AccessPolicy.requireReview(ADMIN, Area.OVERTIME, "DEMO-004");
        check(true, "admin approves overtime");

        deny(() -> AccessPolicy.requireFinalize(PAYROLL, "DEMO-002"), "payroll staff cannot finalize their own payslip");
        deny(() -> AccessPolicy.requireFinalize(HR, "DEMO-004"), "HR cannot finalize payslips");
        AccessPolicy.requireFinalize(PAYROLL, "DEMO-004");
        check(true, "payroll staff finalize others' payslips");
        AccessPolicy.requireScheduleEdit(SUPERVISOR, "DEMO-004");
        deny(() -> AccessPolicy.requireScheduleEdit(HR, "DEMO-004"), "HR does not manage team schedules");
        deny(() -> AccessPolicy.requirePayChangeRequest(EMPLOYEE, "DEMO-001"), "employees cannot file pay changes");
        AccessPolicy.requirePayChangeReview(PAYROLL, "BONUS", "DEMO-004", "DEMO-003");
        AccessPolicy.requirePayChangeReview(HR, "MERIT", "DEMO-004", "DEMO-003");
        deny(() -> AccessPolicy.requirePayChangeReview(HR, "BONUS", "DEMO-004", "DEMO-003"), "bonuses are approved by payroll staff");
        deny(() -> AccessPolicy.requirePayChangeReview(PAYROLL, "PROMOTION", "DEMO-004", "DEMO-003"), "promotions are approved by HR");
        deny(() -> AccessPolicy.requirePayChangeReview(ADMIN, "MERIT", "DEMO-004", "DEMO-005"), "the requester never decides");
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
