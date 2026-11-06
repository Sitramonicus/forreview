package ph.motorph.web;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Role-based access for the local demo, from the user's RBAC permissions matrix (2026-11-05).
 * Who may act is decided by the signed-in account (identity), never by a role or reviewer ID the client sends.
 *
 * Read scopes per area (ALL = everyone, TEAM = self + direct reports + teammates + own supervisor, SELF = own records):
 *   Employees   admin/HR/payroll ALL; supervisor, employee TEAM ("can view team members")
 *   Attendance  admin/HR/payroll ALL; supervisor TEAM; employee SELF
 *   Leave       admin/HR ALL; everyone else TEAM ("check team leave")
 *   Overtime    admin/HR/payroll ALL; supervisor TEAM; employee SELF
 *   Tickets     admin/HR ALL; supervisor TEAM; others SELF
 *   Payroll     admin/payroll/HR ALL; supervisor TEAM (payslip R Team); employee SELF
 *   Reports     admin/HR/payroll ALL; others none
 *   Statutory   admin/payroll ALL; others none
 * Writes: employees C/U admin+HR; attendance edits admin + supervisor for direct reports; requests are created for
 * yourself (admin any; HR any leave/ticket); leave is approved by admin, HR, or the direct supervisor; overtime by admin
 * or the direct supervisor; nobody decides their own request; payslips are finalized by admin or payroll staff, never
 * their own.
 */
public final class AccessPolicy {
    public static final String VERSION = "RBAC-2026-11-05-matrix-v1";

    public enum Role {
        EMPLOYEE("Employee"), SUPERVISOR("Supervisor"), PAYROLL("Payroll Staff"), HR("HR"), ADMIN("System Admin");
        public final String label;
        Role(String label) { this.label = label; }
    }

    public enum Scope { NONE, SELF, TEAM, ALL }

    public enum Area { EMPLOYEES, ATTENDANCE, LEAVE, OVERTIME, TICKETS, PAYROLL, REPORTS, STATUTORY, EARNINGS, REMITTANCE, PAY_CHANGES }

    /** The signed-in account. {@code explicit} is false only for unauthenticated legacy calls (see AppServer). */
    public static final class Actor {
        public final String employeeId;
        public final Role role;
        public final boolean explicit;

        public Actor(String employeeId, Role role, boolean explicit) {
            this.employeeId = employeeId;
            this.role = role;
            this.explicit = explicit;
        }
    }

    /** Thrown when the signed-in account may not do something; AppServer answers 403. */
    public static final class AccessDenied extends RuntimeException {
        public AccessDenied(String message) { super(message); }
    }

    private static final Map<String, Role> ROLES;
    private static final Map<String, Role> DEFAULT_ROLES;
    private static final Map<String, String> DEFAULT_SUPERVISORS;
    private static final Map<String, String> SUPERVISOR_OF;
    private static final Map<Role, Map<Area, Scope>> READ;
    static {
        Map<String, Role> roles = new HashMap<>();
        roles.put("DEMO-001", Role.HR);
        roles.put("DEMO-002", Role.PAYROLL);
        roles.put("DEMO-003", Role.SUPERVISOR);
        roles.put("DEMO-004", Role.EMPLOYEE);
        roles.put("DEMO-005", Role.ADMIN);
        // shortcut: process-wide so the System Admin can change roles; a real directory stores the role per account.
        DEFAULT_ROLES = Collections.unmodifiableMap(new HashMap<>(roles));
        ROLES = new java.util.concurrent.ConcurrentHashMap<>(roles);
        Map<String, String> supervisors = new HashMap<>();
        // One synthetic team: Noa Reyes (Operations Lead) supervises the other staff; the admin has no supervisor.
        supervisors.put("DEMO-001", "DEMO-003");
        supervisors.put("DEMO-002", "DEMO-003");
        supervisors.put("DEMO-004", "DEMO-003");
        // shortcut: process-wide so onboarded people join a team; a real directory stores the reporting line per employee.
        DEFAULT_SUPERVISORS = Collections.unmodifiableMap(new HashMap<>(supervisors));
        SUPERVISOR_OF = new java.util.concurrent.ConcurrentHashMap<>(supervisors);

        Map<Role, Map<Area, Scope>> read = new EnumMap<>(Role.class);
        for (Role role : Role.values()) read.put(role, new EnumMap<>(Area.class));
        for (Area area : Area.values()) read.get(Role.ADMIN).put(area, Scope.ALL);
        put(read, Role.HR, Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.LEAVE, Area.OVERTIME, Area.TICKETS, Area.PAYROLL, Area.REPORTS,
            Area.EARNINGS, Area.REMITTANCE, Area.PAY_CHANGES);
        put(read, Role.PAYROLL, Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.OVERTIME, Area.PAYROLL, Area.REPORTS, Area.STATUTORY,
            Area.EARNINGS, Area.REMITTANCE, Area.PAY_CHANGES);
        put(read, Role.PAYROLL, Scope.TEAM, Area.LEAVE);
        put(read, Role.PAYROLL, Scope.SELF, Area.TICKETS);
        put(read, Role.SUPERVISOR, Scope.TEAM, Area.EMPLOYEES, Area.ATTENDANCE, Area.LEAVE, Area.OVERTIME, Area.TICKETS, Area.PAYROLL, Area.EARNINGS, Area.PAY_CHANGES);
        put(read, Role.EMPLOYEE, Scope.TEAM, Area.EMPLOYEES, Area.LEAVE);
        put(read, Role.EMPLOYEE, Scope.SELF, Area.ATTENDANCE, Area.OVERTIME, Area.TICKETS, Area.PAYROLL);
        READ = read;
    }

    private static void put(Map<Role, Map<Area, Scope>> read, Role role, Scope scope, Area... areas) {
        for (Area area : areas) read.get(role).put(area, scope);
    }

    private AccessPolicy() { }

    /** Accounts without an assigned role (e.g. employees created in the demo) are regular employees. */
    public static Role roleOf(String employeeId) { return ROLES.getOrDefault(employeeId, Role.EMPLOYEE); }

    public static boolean isKnownAccount(String employeeId) { return ROLES.containsKey(employeeId); }

    public static Optional<String> supervisorOf(String employeeId) { return Optional.ofNullable(SUPERVISOR_OF.get(employeeId)); }

    /**
     * System Admin changes someone's role. Guard rails: never your own role; at least one System Admin stays among
     * {@code activeIds}; a supervisor with direct reports keeps the role until the team is reassigned.
     */
    public static synchronized void changeRole(Actor actor, String targetId, Role newRole, java.util.Collection<String> activeIds) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Admin changes roles.");
        if (actor.employeeId.equals(targetId)) throw new AccessDenied("Nobody changes their own role.");
        Role current = roleOf(targetId);
        if (current == Role.ADMIN && newRole != Role.ADMIN) {
            long admins = activeIds.stream().filter(id -> !id.equals(targetId) && roleOf(id) == Role.ADMIN).count();
            if (admins == 0) throw new IllegalStateException("At least one System Admin must remain.");
        }
        if (current == Role.SUPERVISOR && newRole != Role.SUPERVISOR && SUPERVISOR_OF.containsValue(targetId)) {
            throw new IllegalStateException("Reassign this supervisor's team before changing their role.");
        }
        ROLES.put(targetId, newRole);
    }

    /** System Admin sets or clears ("") someone's supervisor; no self-supervision and no loops. */
    public static synchronized void changeSupervisor(Actor actor, String targetId, String supervisorId) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Admin changes reporting lines.");
        if (supervisorId == null || supervisorId.isEmpty()) {
            SUPERVISOR_OF.remove(targetId);
            return;
        }
        if (supervisorId.equals(targetId)) throw new IllegalArgumentException("Nobody supervises themselves.");
        for (String up = supervisorId; up != null; up = SUPERVISOR_OF.get(up)) {
            if (up.equals(targetId)) throw new IllegalArgumentException("That would make a reporting loop.");
        }
        assignSupervisor(targetId, supervisorId);
    }

    /** Roles and reporting lines for the local data file. */
    public static synchronized Map<String, Role> exportRoles() { return new HashMap<>(ROLES); }
    public static synchronized Map<String, String> exportSupervisors() { return new HashMap<>(SUPERVISOR_OF); }

    /** null restores the built-in demo roles and team. */
    public static synchronized void importRoles(Map<String, Role> roles, Map<String, String> supervisors) {
        ROLES.clear();
        ROLES.putAll(roles == null ? DEFAULT_ROLES : roles);
        SUPERVISOR_OF.clear();
        SUPERVISOR_OF.putAll(supervisors == null ? DEFAULT_SUPERVISORS : supervisors);
    }

    public static void requireAdmin(Actor actor) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Admin can open system configuration.");
    }

    /** Onboarding puts a new hire on a supervisor's team; the supervisor must hold the Supervisor role. */
    public static void assignSupervisor(String employeeId, String supervisorId) {
        if (roleOf(supervisorId) != Role.SUPERVISOR) throw new IllegalArgumentException("Choose a supervisor (an account with the Supervisor role).");
        SUPERVISOR_OF.put(employeeId, supervisorId);
    }

    public static boolean isDirectReport(String supervisorId, String employeeId) {
        return supervisorId != null && supervisorId.equals(SUPERVISOR_OF.get(employeeId));
    }

    /** Self, direct reports, and (if the person has a supervisor) that supervisor and their other reports. */
    public static Set<String> teamOf(String employeeId) {
        Set<String> team = new LinkedHashSet<>();
        team.add(employeeId);
        Optional<String> lead = supervisorOf(employeeId);
        lead.ifPresent(team::add);
        for (Map.Entry<String, String> entry : SUPERVISOR_OF.entrySet()) {
            if (entry.getValue().equals(employeeId) || lead.map(entry.getValue()::equals).orElse(false)) team.add(entry.getKey());
        }
        return team;
    }

    public static Scope readScope(Role role, Area area) { return READ.get(role).getOrDefault(area, Scope.NONE); }

    public static boolean canRead(Actor actor, Area area, String targetEmployeeId) {
        switch (readScope(actor.role, area)) {
            case ALL: return true;
            case TEAM: return targetEmployeeId != null && teamOf(actor.employeeId).contains(targetEmployeeId);
            case SELF: return actor.employeeId.equals(targetEmployeeId);
            default: return false;
        }
    }

    public static void requireRead(Actor actor, Area area, String targetEmployeeId) {
        if (!canRead(actor, area, targetEmployeeId)) {
            throw new AccessDenied(actor.role.label + " accounts cannot view " + area.name().toLowerCase() + " for " + targetEmployeeId + ".");
        }
    }

    /** Areas with no per-person target (reports, statutory preview): only an ALL scope gets in. */
    public static void requireArea(Actor actor, Area area) {
        if (readScope(actor.role, area) != Scope.ALL) {
            throw new AccessDenied(actor.role.label + " accounts cannot open " + area.name().toLowerCase() + ".");
        }
    }

    public static void requireEmployeeEdit(Actor actor) {
        if (actor.role != Role.ADMIN && actor.role != Role.HR) throw new AccessDenied("Only HR or a System Admin can create or edit employee records.");
    }

    public static void requireAttendanceEdit(Actor actor, String targetEmployeeId) {
        boolean allowed = actor.role == Role.ADMIN
            || (actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, targetEmployeeId));
        if (!allowed) throw new AccessDenied("Only a System Admin, or the employee's supervisor, can edit attendance.");
    }

    /** Leave, overtime and tickets are filed for yourself; an admin may file for anyone, HR for anyone's leave or ticket. */
    public static void requireCreate(Actor actor, Area area, String targetEmployeeId) {
        boolean allowed = actor.employeeId.equals(targetEmployeeId) || actor.role == Role.ADMIN
            || (actor.role == Role.HR && (area == Area.LEAVE || area == Area.TICKETS));
        if (!allowed) throw new AccessDenied("You can only file " + area.name().toLowerCase() + " requests for yourself.");
    }

    public static void requireCancel(Actor actor, Area area, String ownerEmployeeId) {
        boolean allowed = actor.employeeId.equals(ownerEmployeeId) || actor.role == Role.ADMIN
            || (actor.role == Role.HR && (area == Area.LEAVE || area == Area.TICKETS));
        if (!allowed) throw new AccessDenied("Only the person who filed it (or HR/System Admin) can cancel this request.");
    }

    public static void requireReview(Actor actor, Area area, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can approve or reject their own request.");
        boolean direct = actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, ownerEmployeeId);
        boolean allowed = actor.role == Role.ADMIN || direct || (area == Area.LEAVE && actor.role == Role.HR);
        if (!allowed) {
            throw new AccessDenied(area == Area.LEAVE
                ? "Leave is approved by the employee's supervisor, HR, or a System Admin."
                : "Overtime is approved by the employee's supervisor or a System Admin.");
        }
    }

    /** Earnings/deductions entries and disbursement: Payroll Staff or System Admin, never for their own pay. */
    /**
     * One person's earnings and voluntary deductions (RBAC matrix: Earnings/Deductibles R,U for the supervisor's team):
     * Payroll Staff and the System Admin for anyone, a supervisor for their direct reports; never your own pay.
     */
    public static void requireEarningsEdit(Actor actor, String ownerEmployeeId) {
        boolean allowed = actor.role == Role.ADMIN || actor.role == Role.PAYROLL
            || (actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, ownerEmployeeId));
        if (!allowed) throw new AccessDenied("Only Payroll Staff, a System Admin, or the employee's supervisor can change their earnings and deductions.");
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can change their own pay.");
    }

    public static void requirePayrollProcessing(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.ADMIN && actor.role != Role.PAYROLL) throw new AccessDenied("Only Payroll Staff or a System Admin can process payroll.");
        if (ownerEmployeeId != null && actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can change their own pay.");
    }

    /** Team schedules: the person's supervisor or a System Admin. */
    public static void requireScheduleEdit(Actor actor, String targetEmployeeId) {
        boolean allowed = actor.role == Role.ADMIN || (actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, targetEmployeeId));
        if (!allowed) throw new AccessDenied("Only the employee's supervisor or a System Admin can change their schedule.");
    }

    /** Promotion, merit increase, bonus or disciplinary/unpaid leave requests: filed by the supervisor (or admin). */
    public static void requirePayChangeRequest(Actor actor, String targetEmployeeId) {
        if (actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody can request a pay change for themselves.");
        boolean allowed = actor.role == Role.ADMIN || (actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, targetEmployeeId));
        if (!allowed) throw new AccessDenied("Pay-change requests are filed by the employee's supervisor or a System Admin.");
    }

    /** HR decides promotions, merit increases and disciplinary leave; Payroll Staff decide bonuses; never the requester or the employee. */
    public static void requirePayChangeReview(Actor actor, String type, String targetEmployeeId, String requesterId) {
        if (actor.employeeId.equals(targetEmployeeId) || actor.employeeId.equals(requesterId)) {
            throw new AccessDenied("The employee and the person who filed the request cannot decide it.");
        }
        boolean allowed = actor.role == Role.ADMIN || ("BONUS".equals(type) ? actor.role == Role.PAYROLL : actor.role == Role.HR);
        if (!allowed) throw new AccessDenied("BONUS".equals(type) ? "Bonus requests are approved by Payroll Staff." : "This request is approved by HR.");
    }

    /** Onboarding, offboarding, department and position lists: HR or System Admin; nobody offboards themselves. */
    public static void requireHrManagement(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.ADMIN && actor.role != Role.HR) throw new AccessDenied("Only HR or a System Admin manages onboarding, offboarding and HR lists.");
        if (targetEmployeeId != null && actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody can offboard themselves.");
    }

    /** HR resolves tickets in the HR queue; a System Admin resolves any; never your own ticket. */
    /**
     * Who handles a ticket (design doc: tickets are routed by type to handlers): Team Lead queue = the owner's supervisor,
     * Payroll Staff queue = Payroll Staff, HR queue = HR, IT Admin queue = the System Admin. The System Admin can step in
     * on any queue. Never your own ticket.
     */
    public static boolean canHandleTicket(Actor actor, String queue, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) return false;
        if (actor.role == Role.ADMIN) return true;
        switch (queue) {
            case "Team Lead": return actor.role == Role.SUPERVISOR && isDirectReport(actor.employeeId, ownerEmployeeId);
            case "Payroll Staff": return actor.role == Role.PAYROLL;
            case "HR": return actor.role == Role.HR;
            default: return false;
        }
    }

    public static void requireTicketResolve(Actor actor, String queue, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody handles their own ticket.");
        if (!canHandleTicket(actor, queue, ownerEmployeeId)) {
            throw new AccessDenied("This ticket is in the " + queue + " queue; it is handled by " + ("Team Lead".equals(queue) ? "the employee's supervisor"
                : "Payroll Staff".equals(queue) ? "Payroll Staff" : "HR".equals(queue) ? "HR" : "the System Admin") + ".");
        }
    }

    /** Government numbers: System Admin, HR and Payroll Staff see them (masked unless allowed); nobody else. */
    public static void requireGovernmentRead(Actor actor) {
        if (actor.role != Role.ADMIN && actor.role != Role.HR && actor.role != Role.PAYROLL) {
            throw new AccessDenied("Government details are limited to HR, Payroll Staff and the System Admin.");
        }
    }

    /** HR and Payroll Staff propose changes and ask for full view (matrix "A"); never for their own record. */
    public static void requireGovernmentRequest(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.HR && actor.role != Role.PAYROLL) throw new AccessDenied("Only HR or Payroll Staff file government-details requests; the System Admin edits directly.");
        if (actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody files government-details requests about themselves.");
    }

    /** Direct edits, approvals and the audit log: System Admin only; an admin never decides a request about their own record. */
    public static void requireGovernmentAdmin(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Admin edits or approves government details.");
        if (targetEmployeeId != null && actor.employeeId.equals(targetEmployeeId) && actor.explicit) {
            throw new AccessDenied("The System Admin does not approve requests about their own record.");
        }
    }

    /** Payslip corrections: Payroll Staff file them, the System Admin approves; nobody corrects their own payslip. */
    public static void requirePayslipCorrectionRequest(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.PAYROLL) throw new AccessDenied("Payroll Staff file payslip corrections; the System Admin approves them.");
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody corrects their own payslip.");
    }

    public static void requirePayslipCorrectionReview(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Admin approves payslip corrections.");
        if (actor.explicit && actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody approves a correction to their own payslip.");
    }

    public static void requireFinalize(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.ADMIN && actor.role != Role.PAYROLL) throw new AccessDenied("Only Payroll Staff or a System Admin can finalize payslips.");
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can finalize their own payslip.");
    }
}
