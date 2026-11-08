package ph.motorph.web;

import java.util.Optional;

/**
 * Decides, for one request, whether the caller may act on one employee's record.
 *
 * <p>Everything is derived from the caller's verified token and from MotorPH's own
 * {@code Immediate Supervisor} column — never from a query parameter. A Supervisor reaches an
 * employee because {@code employees/{id}.supervisorId == caller's employee number}, not because the
 * browser said so.
 */
public final class AccessControl {

    /** Who is asking: their employee number, their role, and their team. */
    public static final class Actor {
        private final String employeeNumber;
        private final Role role;

        public Actor(String employeeNumber, Role role) {
            // An unlinked account is a real, supported state: the person has signed in but HR has not
            // given them an employee number. It is allowed here only with the role that holds no
            // permissions, and it is never allowed to hold any other role.
            boolean unlinked = employeeNumber != null && employeeNumber.isEmpty() && role == Role.APPLICANT;
            if (!unlinked && (employeeNumber == null || !employeeNumber.matches("[0-9]{5}"))) {
                throw new IllegalArgumentException("An actor needs a five-digit employee number, "
                    + "or an empty one with the unlinked role.");
            }
            if (role == null) throw new IllegalArgumentException("An actor needs a role.");
            this.employeeNumber = employeeNumber == null ? "" : employeeNumber;
            this.role = role;
        }

        public String employeeNumber() {
            return employeeNumber;
        }

        public Role role() {
            return role;
        }

        public boolean isSystemAdmin() {
            return role == Role.SYSTEM_ADMIN;
        }

        public boolean isHr() {
            return role == Role.HR;
        }

        public boolean isPayroll() {
            return role == Role.PAYROLL;
        }

        public boolean isSupervisor() {
            return role == Role.SUPERVISOR;
        }
    }

    /** Raised when the matrix or the supervisor link does not allow the action. */
    public static final class Denied extends RuntimeException {
        private final int status;

        public Denied(String message, int status) {
            super(message);
            this.status = status;
        }

        public int status() {
            return status;
        }
    }

    private final EmployeeDirectory directory;

    public AccessControl(EmployeeDirectory directory) {
        if (directory == null) throw new IllegalArgumentException("directory must not be null");
        this.directory = directory;
    }

    /** The employee record lookup used to resolve team membership. */
    public interface EmployeeDirectory {
        Optional<Employee> find(String employeeNumber);
    }

    /**
     * Checks one operation against one employee.
     *
     * @param targetEmployeeNumber the record being touched (for a list call, call this per row)
     * @throws Denied with 403 when the matrix forbids it
     */
    public void require(Actor actor, Policy.Entity entity, Policy.Operation operation, String targetEmployeeNumber) {
        if (actor == null) throw new Denied("Sign in with your MotorPH account to continue.", 401);
        Policy.Scope scope = Policy.scope(entity, actor.role());

        if (scope == Policy.Scope.NONE) {
            throw new Denied(actor.role().label() + " has no access to " + entity.label() + ".", 403);
        }
        if (scope == Policy.Scope.APPROVAL_REQUIRED) {
            throw new Denied(entity.label() + " changes are approved, not applied directly.", 403);
        }
        if (operation != Policy.Operation.READ && !Policy.allows(entity, actor.role(), operation)) {
            throw new Denied(actor.role().label() + " may not " + operation.name().toLowerCase()
                + " " + entity.label() + ".", 403);
        }

        boolean own = targetEmployeeNumber != null && targetEmployeeNumber.equals(actor.employeeNumber());
        switch (scope) {
            case READ_SELF:
                if (!own) throw new Denied("You may only see your own " + entity.label().toLowerCase() + ".", 403);
                return;
            case READ_TEAM:
                if (!own && !isDirectReport(actor, targetEmployeeNumber)) {
                    throw new Denied("That employee does not report to you.", 403);
                }
                return;
            case READ_UPDATE_TEAM:
                if (!own && !isDirectReport(actor, targetEmployeeNumber)) {
                    throw new Denied("That employee does not report to you.", 403);
                }
                return;
            default:
                // Company-wide read or write: nothing further to check.
        }
    }

    /** True when the target lists the actor as their Immediate Supervisor. */
    public boolean isDirectReport(Actor actor, String targetEmployeeNumber) {
        if (targetEmployeeNumber == null) return false;
        if (targetEmployeeNumber.equals(actor.employeeNumber())) return true;
        return directory.find(targetEmployeeNumber)
            .map(employee -> actor.employeeNumber().equals(employee.getSupervisorId()))
            .orElse(false);
    }

    /**
     * Whether {@code subject} may see {@code target}'s payslips, given the Payroll row of the
     * matrix (Payroll: C,R,U; Supervisor: R (Team); Employee: R (Self)).
     */
    public boolean canReadPayslipOf(Actor subject, String targetEmployeeNumber) {
        try {
            require(subject, Policy.Entity.PAYSLIP, Policy.Operation.READ, targetEmployeeNumber);
            return true;
        } catch (Denied denied) {
            return false;
        }
    }
}
