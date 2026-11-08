package ph.motorph.web;

/**
 * MotorPH's RBAC permissions matrix, transcribed cell for cell from the System Design Document
 * (MO-IT108 2nd Milestone, "System Users → RBAC Permissions Matrix").
 *
 * <p>This class is the single source of truth for who may do what. It deliberately contains no
 * opinion: every row below is one row of the document, and {@code RbacPolicyTest} fails if the
 * code and the documented matrix ever disagree.
 *
 * <pre>
 * Entity                 System Admin   HR            Payroll       Supervisor     Employee
 * Employee               C R U D        C R U         R             R (Self)       R (Self)
 * Address                C R U D        C R U         R (Self)      R (Self)       R (Self)
 * Department             C R U D        C R U         -             -              -
 * Position               C R U D        C R U         -             -              -
 * Leave                  C R U D        C R U         R (Self)      R U (Team)     R (Self)
 * Attendance             C R U D        R             R             R U (Team)     R (Self)
 * Payroll                C R U D        R             C R U         R (Self)       R (Self)
 * Payslip                C R U D        R             C R U         R (Team)       R (Self)
 * Earnings/Deductibles   C R U D        R             C R U         R U (Team)     -
 * Government Deductibles C R U D        R             R             -              -
 * Government Details     C R U D        A (approve)   A (approve)   -              -
 * Tax Table/Rates        C R U D        -             -             -              -
 * </pre>
 *
 * <p>Two notes on interpretation, both explicit rather than silent:
 *
 * <ol>
 *   <li><b>(Self)</b> means the actor may act only on their own employee number. <b>(Team)</b>
 *       means the actor may act only on the set of employees who report to them. Both are checked
 *       against {@link Employee#getSupervisorId()}, never against what the client claims.</li>
 *   <li>"A" for Government Details is an <i>approval requirement</i>, not an approval right: HR and
 *       Payroll may propose a change, and neither may self-approve it. See
 *       {@code requiresSecondApprover}.</li>
 * </ol>
 */
public final class Policy {

    /** The entities named in the document's matrix. */
    public enum Entity {
        EMPLOYEE("Employee"),
        ADDRESS("Address"),
        DEPARTMENT("Department"),
        POSITION("Position"),
        LEAVE("Leave"),
        ATTENDANCE("Attendance"),
        PAYROLL("Payroll"),
        PAYSLIP("Payslip"),
        EARNINGS_DEDUCTIBLES("Earnings/Deductibles"),
        GOVERNMENT_DEDUCTIBLES("Government Deductibles"),
        GOVERNMENT_DETAILS("Government Details"),
        TAX_RATES("Tax Table/Rates");

        private final String label;

        Entity(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** One cell of the matrix. */
    public enum Scope {
        /** The document has "-" in this cell: no access at all. */
        NONE,
        /** Plain read, any employee. */
        READ,
        /** Plain create, read, update, no delete. */
        CREATE_READ_UPDATE,
        /** Plain read and write, but delete is not granted. */
        READ_WRITE,
        /** Full create, read, update and delete. */
        WRITE,
        /** Read limited to the actor's own employee number. */
        READ_SELF,
        /** Read limited to the actor's direct reports. */
        READ_TEAM,
        /** Read and update limited to the actor's direct reports. */
        READ_UPDATE_TEAM,
        /** Approval required before the change takes effect; not a right to approve. */
        APPROVAL_REQUIRED
    }

    private static final Scope CRUD = Scope.WRITE;

    private Policy() {
    }

    /**
     * The matrix. Index by entity, then by role. This is a direct transcription; do not "tidy" it.
     */
    public static Scope scope(Entity entity, Role role) {
        switch (entity) {
            case EMPLOYEE:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case PAYROLL: return Scope.READ;
                    case SUPERVISOR: return Scope.READ_SELF;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case ADDRESS:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case PAYROLL: return Scope.READ_SELF;
                    case SUPERVISOR: return Scope.READ_SELF;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case DEPARTMENT:
            case POSITION:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case PAYROLL: return Scope.NONE;
                    case SUPERVISOR: return Scope.NONE;
                    case EMPLOYEE: return Scope.NONE;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case LEAVE:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case PAYROLL: return Scope.READ_SELF;
                    case SUPERVISOR: return Scope.READ_UPDATE_TEAM;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case ATTENDANCE:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.READ;
                    case PAYROLL: return Scope.READ;
                    case SUPERVISOR: return Scope.READ_UPDATE_TEAM;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case PAYROLL:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.READ;
                    case PAYROLL: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case SUPERVISOR: return Scope.READ_SELF;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case PAYSLIP:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.READ;
                    case PAYROLL: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case SUPERVISOR: return Scope.READ_TEAM;
                    case EMPLOYEE: return Scope.READ_SELF;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case EARNINGS_DEDUCTIBLES:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.READ;
                    case PAYROLL: return Scope.CREATE_READ_UPDATE;   // C, R, U (no delete)
                    case SUPERVISOR: return Scope.READ_UPDATE_TEAM;
                    case EMPLOYEE: return Scope.NONE;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case GOVERNMENT_DEDUCTIBLES:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.READ;
                    case PAYROLL: return Scope.READ;
                    case SUPERVISOR: return Scope.NONE;
                    case EMPLOYEE: return Scope.NONE;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case GOVERNMENT_DETAILS:
                switch (role) {
                    case SYSTEM_ADMIN: return CRUD;
                    case HR: return Scope.APPROVAL_REQUIRED;
                    case PAYROLL: return Scope.APPROVAL_REQUIRED;
                    case SUPERVISOR: return Scope.NONE;
                    case EMPLOYEE: return Scope.NONE;
                    case APPLICANT: return Scope.NONE;
                }
                break;
            case TAX_RATES:
                // The document gives every other role "-" here. System Admin alone maintains the
                // statutory tables; this is why a tax-table change is a separate, audited action.
                return role == Role.SYSTEM_ADMIN ? CRUD : Scope.NONE;
        }
        throw new IllegalStateException("Matrix has no cell for " + entity + " / " + role);
    }

    /** The operations a cell allows. WRITE implies create, read, update and delete. */
    public static boolean allows(Entity entity, Role role, Operation operation) {
        Scope scope = scope(entity, role);
        switch (operation) {
            case READ:
                return scope != Scope.NONE && scope != Scope.APPROVAL_REQUIRED;
            case CREATE:
            case UPDATE:
                return scope == Scope.WRITE
                    || scope == Scope.CREATE_READ_UPDATE
                    || scope == Scope.READ_WRITE
                    || (scope == Scope.READ_UPDATE_TEAM && operation == Operation.UPDATE);
            case DELETE:
                // Only a full CRUD cell may delete. HR's "C, R, U" on Employee is not a delete.
                return scope == Scope.WRITE;
            default:
                return false;
        }
    }

    /**
     * Whether the matrix restricts the actor to their own records for this entity. Used to decide
     * if a request must be expanded to a team or narrowed to self before it is answered.
     */
    public static boolean isSelfOnly(Entity entity, Role role) {
        return scope(entity, role) == Scope.READ_SELF;
    }

    /** Whether the actor may see every employee's rows for this entity. */
    public static boolean isCompanyWideRead(Entity entity, Role role) {
        Scope scope = scope(entity, role);
        return scope == Scope.READ || scope == Scope.WRITE
            || scope == Scope.CREATE_READ_UPDATE || scope == Scope.READ_WRITE;
    }

    /** Whether the actor is limited to their direct reports for this entity. */
    public static boolean isTeamScoped(Entity entity, Role role) {
        Scope scope = scope(entity, role);
        return scope == Scope.READ_TEAM || scope == Scope.READ_UPDATE_TEAM;
    }

    /**
     * Government Details needs a second person: HR and Payroll may propose, but a proposal cannot
     * be approved by the same employee number that raised it.
     */
    public static boolean requiresSecondApprover(Entity entity, Role actorRole) {
        return entity == Entity.GOVERNMENT_DETAILS && scope(entity, actorRole) == Scope.APPROVAL_REQUIRED;
    }

    public enum Operation {
        READ, CREATE, UPDATE, DELETE
    }
}
