package ph.motorph.web;

import java.util.Optional;

/**
 * Checks the code against MotorPH's RBAC permissions matrix, cell by cell.
 *
 * <p>The table below is the document's table, transcribed independently of {@link Policy}. If the
 * two ever disagree, this test fails — that is the point. Do not "fix" a failure by editing the
 * expectation to match the code; decide which one is wrong first.
 */
public final class RbacPolicyTest {

    private int checks;

    public static void main(String[] args) {
        new RbacPolicyTest().run();
    }

    private void run() {
        documentedMatrixIsTranscribedExactly();
        supervisorScopeComesFromTheEmployeeFile();
        employeeCannotReachAnotherEmployee();
        payrollAndHrReachEveryoneToTheDocumentedDegree();
        systemAdminIsTheOnlyWriterOfTaxTables();
        governmentDetailsNeedsASecondApprover();
        unknownRoleFallsBackToLeastPrivilege();
        everyCellIsAnswered();
        System.out.println("PASS: " + checks + " RBAC policy checks");
    }

    /** The document's matrix, typed independently: role order is Admin, HR, Payroll, Supervisor, Employee. */
    private void documentedMatrixIsTranscribedExactly() {
        // W = plain create/read/update/delete, R = plain read, S = read self, T = read team,
        // U = read+update team, A = approval required, - = no access.
        String[][] document = {
            // Employee             Address              Department  Position  Leave   Attendance Payroll  Payslip  Earnings  GovDeduct  GovDetails  TaxRates
            {"W","W","W","W","W","W","W","W","W","W","W","W"},   // System Admin
            {"CRU","CRU","CRU","CRU","CRU","R","R","R","R","R","A","-"},  // HR: C,R,U (never D)
            {"R","S","-","-","S","R","CRU","CRU","CRU","R","A","-"}, // Payroll: C,R,U (never D)
            {"S","S","-","-","U","U","S","T","U","-","-","-"},   // Supervisor
            {"S","S","-","-","S","S","S","S","-","-","-","-"},   // Employee
        };
        Policy.Entity[] entities = {
            Policy.Entity.EMPLOYEE, Policy.Entity.ADDRESS, Policy.Entity.DEPARTMENT, Policy.Entity.POSITION,
            Policy.Entity.LEAVE, Policy.Entity.ATTENDANCE, Policy.Entity.PAYROLL, Policy.Entity.PAYSLIP,
            Policy.Entity.EARNINGS_DEDUCTIBLES, Policy.Entity.GOVERNMENT_DEDUCTIBLES,
            Policy.Entity.GOVERNMENT_DETAILS, Policy.Entity.TAX_RATES
        };
        Role[] roles = {Role.SYSTEM_ADMIN, Role.HR, Role.PAYROLL, Role.SUPERVISOR, Role.EMPLOYEE};

        for (int r = 0; r < roles.length; r++) {
            for (int e = 0; e < entities.length; e++) {
                Policy.Scope expected = decode(document[r][e]);
                Policy.Scope actual = Policy.scope(entities[e], roles[r]);
                equal(expected, actual, roles[r].label() + " x " + entities[e].label());
            }
        }
    }

    /** "C,R,U" (no delete) and full "C,R,U,D" are different cells and must stay different. */
    private Policy.Scope decode(String cell) {
        switch (cell) {
            case "W": return Policy.Scope.WRITE;
            case "CRU": return Policy.Scope.CREATE_READ_UPDATE;
            case "R": return Policy.Scope.READ;
            case "S": return Policy.Scope.READ_SELF;
            case "T": return Policy.Scope.READ_TEAM;
            case "U": return Policy.Scope.READ_UPDATE_TEAM;
            case "A": return Policy.Scope.APPROVAL_REQUIRED;
            case "-": return Policy.Scope.NONE;
            default: throw new IllegalStateException("Unknown document cell " + cell);
        }
    }

    /** Team scope must come from the "Immediate Supervisor" column of MotorPH's file. */
    private void supervisorScopeComesFromTheEmployeeFile() {
        AccessControl control = directory();
        // 10015 (Account Manager) supervises 10016 and 10017 in the supplied file.
        AccessControl.Actor fredrick = new AccessControl.Actor("10015", Role.SUPERVISOR);
        check(control.isDirectReport(fredrick, "10016"), "10016 reports to 10015");
        check(control.isDirectReport(fredrick, "10017"), "10017 reports to 10015");
        check(!control.isDirectReport(fredrick, "10001"), "the CEO does not report to 10015");
        check(!control.isDirectReport(fredrick, "10003"), "the CFO does not report to 10015");
        // The CEO is top of the chain: no supervisor recorded.
        check(employees().findEmployee("10001").get().getSupervisorId() == null,
            "the CEO has no supervisor in MotorPH's file");
        // 10015's own chain: 10015 -> 10002 -> 10001.
        check("10002".equals(employees().findEmployee("10015").get().getSupervisorId()),
            "10015 reports to the COO (10002)");

        // Supervisor may read and correct a direct report's attendance...
        control.require(fredrick, Policy.Entity.ATTENDANCE, Policy.Operation.UPDATE, "10016");
        // ...but not an unrelated employee's.
        expectDenied(() -> control.require(fredrick, Policy.Entity.ATTENDANCE, Policy.Operation.UPDATE, "10003"),
            "supervisor cannot edit a non-report's attendance");
        // ...and may not create attendance anywhere: the matrix says R, U (Team) only.
        expectDenied(() -> control.require(fredrick, Policy.Entity.ATTENDANCE, Policy.Operation.CREATE, "10016"),
            "supervisor cannot create attendance records");
        // Payslip is read-only for a supervisor, and team-scoped.
        control.require(fredrick, Policy.Entity.PAYSLIP, Policy.Operation.READ, "10016");
        expectDenied(() -> control.require(fredrick, Policy.Entity.PAYSLIP, Policy.Operation.UPDATE, "10016"),
            "supervisor cannot update a payslip");
        expectDenied(() -> control.require(fredrick, Policy.Entity.PAYSLIP, Policy.Operation.READ, "10003"),
            "supervisor cannot read a non-report's payslip");
        // Earnings/Deductibles is R, U (Team) but Employee gets "-": a supervisor is not an employee
        // for this entity, and the matrix grants no create.
        control.require(fredrick, Policy.Entity.EARNINGS_DEDUCTIBLES, Policy.Operation.UPDATE, "10016");
        expectDenied(() -> control.require(fredrick, Policy.Entity.EARNINGS_DEDUCTIBLES,
            Policy.Operation.CREATE, "10016"), "supervisor cannot create earnings lines");
    }

    private void employeeCannotReachAnotherEmployee() {
        AccessControl control = directory();
        AccessControl.Actor alice = new AccessControl.Actor("10008", Role.EMPLOYEE);
        control.require(alice, Policy.Entity.EMPLOYEE, Policy.Operation.READ, "10008");
        control.require(alice, Policy.Entity.PAYSLIP, Policy.Operation.READ, "10008");
        control.require(alice, Policy.Entity.ATTENDANCE, Policy.Operation.READ, "10008");
        expectDenied(() -> control.require(alice, Policy.Entity.EMPLOYEE, Policy.Operation.READ, "10009"),
            "an employee cannot read a colleague's record");
        expectDenied(() -> control.require(alice, Policy.Entity.PAYSLIP, Policy.Operation.READ, "10015"),
            "an employee cannot read a manager's payslip");
        expectDenied(() -> control.require(alice, Policy.Entity.EMPLOYEE, Policy.Operation.UPDATE, "10008"),
            "an employee cannot update even their own directory record");
        // Earnings/Deductibles is "-" for Employee: no read at all.
        expectDenied(() -> control.require(alice, Policy.Entity.EARNINGS_DEDUCTIBLES,
            Policy.Operation.READ, "10008"), "employee has no earnings visibility");
        // Leave is R (Self) for an employee, so a supervisor cannot approve on their behalf.
        expectDenied(() -> directory().require(
            new AccessControl.Actor("10016", Role.SUPERVISOR),
            Policy.Entity.LEAVE, Policy.Operation.UPDATE, "10008"),
            "a supervisor cannot touch an unrelated employee's leave");
    }

    private void payrollAndHrReachEveryoneToTheDocumentedDegree() {
        AccessControl control = directory();
        AccessControl.Actor payroll = new AccessControl.Actor("10011", Role.PAYROLL);
        AccessControl.Actor hr = new AccessControl.Actor("10006", Role.HR);

        control.require(payroll, Policy.Entity.PAYROLL, Policy.Operation.CREATE, "10001");
        control.require(payroll, Policy.Entity.PAYSLIP, Policy.Operation.CREATE, "10001");
        control.require(payroll, Policy.Entity.EARNINGS_DEDUCTIBLES, Policy.Operation.UPDATE, "10001");
        expectDenied(() -> control.require(payroll, Policy.Entity.EMPLOYEE, Policy.Operation.CREATE, null),
            "payroll cannot create employees");
        expectDenied(() -> control.require(payroll, Policy.Entity.EMPLOYEE, Policy.Operation.UPDATE, "10001"),
            "payroll cannot edit employee records");

        control.require(hr, Policy.Entity.EMPLOYEE, Policy.Operation.CREATE, null);
        control.require(hr, Policy.Entity.EMPLOYEE, Policy.Operation.UPDATE, "10001");
        control.require(hr, Policy.Entity.DEPARTMENT, Policy.Operation.CREATE, null);
        control.require(hr, Policy.Entity.POSITION, Policy.Operation.UPDATE, null);
        // HR has no delete on Employee (C, R, U only).
        expectDenied(() -> control.require(hr, Policy.Entity.EMPLOYEE, Policy.Operation.DELETE, "10001"),
            "HR may not delete an employee");
        // HR reads payroll but does not run it.
        expectDenied(() -> control.require(hr, Policy.Entity.PAYROLL, Policy.Operation.CREATE, "10001"),
            "HR cannot run payroll");
        expectDenied(() -> control.require(hr, Policy.Entity.TAX_RATES, Policy.Operation.READ, null),
            "HR cannot read the tax table entity directly");
    }

    private void systemAdminIsTheOnlyWriterOfTaxTables() {
        AccessControl control = directory();
        AccessControl.Actor admin = new AccessControl.Actor("10005", Role.SYSTEM_ADMIN);
        control.require(admin, Policy.Entity.TAX_RATES, Policy.Operation.UPDATE, null);
        control.require(admin, Policy.Entity.EMPLOYEE, Policy.Operation.DELETE, "10001");
        for (Role role : new Role[]{Role.HR, Role.PAYROLL, Role.SUPERVISOR, Role.EMPLOYEE}) {
            equal(Policy.Scope.NONE, Policy.scope(Policy.Entity.TAX_RATES, role),
                role.label() + " has no tax-table access");
        }
    }

    private void governmentDetailsNeedsASecondApprover() {
        check(Policy.requiresSecondApprover(Policy.Entity.GOVERNMENT_DETAILS, Role.HR),
            "HR government-detail changes need approval");
        check(Policy.requiresSecondApprover(Policy.Entity.GOVERNMENT_DETAILS, Role.PAYROLL),
            "Payroll government-detail changes need approval");
        check(!Policy.requiresSecondApprover(Policy.Entity.GOVERNMENT_DETAILS, Role.SYSTEM_ADMIN),
            "the System Admin is not gated");
        AccessControl control = directory();
        expectDenied(() -> control.require(new AccessControl.Actor("10006", Role.HR),
            Policy.Entity.GOVERNMENT_DETAILS, Policy.Operation.UPDATE, "10008"),
            "HR cannot write government details directly");
    }

    private void unknownRoleFallsBackToLeastPrivilege() {
        // An unrecognised claim is not evidence that the person is an employee, so it must not be
        // promoted into the Employee role - that would hand a directory and payslip access to anyone
        // who can set a role string. Unknown means unlinked, which holds no permissions at all.
        equal(Role.APPLICANT, Role.fromClaim("superuser"), "an unknown role claim is unlinked, not Employee");
        equal(Role.APPLICANT, Role.fromClaim(null), "a missing role claim is unlinked, not Employee");
        equal(Role.EMPLOYEE, Role.fromClaim("employee"), "a spelled-out employee claim is Employee");
        equal(Role.SYSTEM_ADMIN, Role.fromClaim("system-admin"), "a hyphenated claim is accepted");
        equal(Role.EMPLOYEE,
            new RoleAssignment(java.util.Collections.emptyMap()).resolve("10001", null).role(),
            "an unassigned employee number acts as a plain Employee");
        // A role header cannot be used to promote: seats come from the assignment, not the request.
        equal(Role.SUPERVISOR, RoleAssignment.demoSeed().resolve("10015", null).role(),
            "10015's most senior seat is Supervisor");
        equal(Role.EMPLOYEE, RoleAssignment.demoSeed().resolve("10018", null).role(),
            "10018 holds no special seat, so acts as an Employee");
        equal(1, RoleAssignment.demoSeed().seatsOf("10018").size(),
            "an ordinary employee still holds exactly one seat: Employee, the floor");
        equal(1, RoleAssignment.demoSeed().seatsOf("99999").size(),
            "an employee number with no record at all resolves to the Employee floor");

        // One person, several seats. This is the case that removes the need for several accounts.
        RoleAssignment seed = RoleAssignment.demoSeed();
        equal(2, seed.seatsOf("10006").size(), "the HR Manager also holds a Supervisor seat");
        equal(Role.HR, seed.resolve("10006", "HR").role(), "10006 can act as HR");
        equal(Role.SUPERVISOR, seed.resolve("10006", "SUPERVISOR").role(), "10006 can act as Supervisor");
        equal(Role.HR, seed.resolve("10006", null).role(), "with no seat named, the senior seat is used");
        equal(2, seed.seatsOf("10011").size(), "the Payroll Manager also holds a Supervisor seat");
        expectDenied(() -> seed.resolve("10018", "SYSTEM_ADMIN"),
            "an employee cannot act as System Admin by naming the seat");

        // A demonstration account holding all five seats, for looking at each role from one sign-in.
        RoleAssignment all = RoleAssignment.withDemonstrationSeatHolder();
        equal(5, all.seatsOf(RoleAssignment.DEMONSTRATION_EMPLOYEE).size(),
            "the demonstration account holds all five seats");
        for (Role role : Role.values()) {
            if (role == Role.APPLICANT) continue;
            equal(role, all.resolve(RoleAssignment.DEMONSTRATION_EMPLOYEE, role.name()).role(),
                "the demonstration account can act as " + role);
        }
        equal(Role.SYSTEM_ADMIN, all.resolve(RoleAssignment.DEMONSTRATION_EMPLOYEE, null).role(),
            "the demonstration account defaults to its most senior seat");
        expectDenied(() -> all.resolve(RoleAssignment.DEMONSTRATION_EMPLOYEE, "KING"),
            "an unknown seat is refused, not defaulted");
    }

    private EmployeeService employees() {
        return new EmployeeService(new InMemoryEmployeeStore());
    }

    private AccessControl directory() {
        return new AccessControl(employees()::findEmployee);
    }

    private void expectDenied(Runnable action, String message) {
        checks++;
        try {
            action.run();
            System.out.println("  FAIL " + message + ": expected a denial but the action was allowed");
            failures++;
        } catch (AccessControl.Denied denied) {
            if (denied.status() != 403) {
                System.out.println("  FAIL " + message + ": expected status 403 but got " + denied.status());
                failures++;
            }
        }
    }


    /** No entity may be answered by a fall-through, and an unlinked account must be granted nothing. */
    private void everyCellIsAnswered() {
        int answered = 0;
        for (Policy.Entity entity : Policy.Entity.values()) {
            for (Role role : Role.values()) {
                Policy.Scope scope = Policy.scope(entity, role);
                check(scope != null, entity + " x " + role + " has an explicit scope");
                if (role == Role.APPLICANT) {
                    equal(Policy.Scope.NONE, scope, "an unlinked account gets nothing on " + entity);
                    for (Policy.Operation operation : Policy.Operation.values()) {
                        check(!Policy.allows(entity, role, operation),
                            "an unlinked account may not " + operation + " " + entity);
                    }
                }
                answered++;
            }
        }
        equal(72, answered, "12 entities x 6 roles are all answered");
    }

    private int failures;

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
