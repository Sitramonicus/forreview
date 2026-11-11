package ph.motorph.sp_secpol;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import ph.motorph.o_oop.OrgChart;

public final class AccessPolicy {
    public static final String VERSION = "RBAC-2026-11-08-nine-roles-recursive-team-v2";

    public enum Role {
        EMPLOYEE("Employee"),
        SUPERVISOR("Team Supervisor"),
        HR("HR Staff"),
        PAYROLL("Payroll Staff"),
        CEO("Chief Executive Officer"),
        CFO("Chief Financial Officer"),
        COO("Chief Operating Officer"),
        CMO("Chief Marketing Officer"),
        ADMIN("System Administrator");

        public final String label;
        Role(String label) { this.label = label; }

        public boolean isExecutive() { return this == CEO || this == CFO || this == COO || this == CMO; }
    }

    public enum Scope { NONE, SELF, TEAM, ALL }

    public enum Area { EMPLOYEES, ATTENDANCE, LEAVE, OVERTIME, TICKETS, PAYROLL, REPORTS, STATUTORY, EARNINGS, REMITTANCE, PAY_CHANGES }

    
    public static final class Actor {
        public final String employeeId;
        public final Role role;
        public final boolean explicit;

        public Actor(String employeeId, Role role, boolean explicit) {
            this.employeeId = employeeId == null ? "" : employeeId;
            this.role = role == null ? Role.EMPLOYEE : role;
            this.explicit = explicit;
        }
    }

    
    public static final class AccessDenied extends RuntimeException {
        public AccessDenied(String message) { super(message); }
    }

    private static final Map<Role, Map<Area, Scope>> READ = new EnumMap<>(Role.class);
    private static final Map<String, Role> ROLE_OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile OrgChart chart;

    static {
        for (Role role : Role.values()) READ.put(role, new EnumMap<>(Area.class));

        
        for (Role role : Role.values()) {
            put(read(role), Scope.SELF, Area.ATTENDANCE, Area.OVERTIME, Area.TICKETS, Area.PAYROLL);
            put(read(role), Scope.TEAM, Area.LEAVE);
        }

        
        put(read(Role.EMPLOYEE), Scope.TEAM, Area.EMPLOYEES);

        
        put(read(Role.SUPERVISOR), Scope.TEAM, Area.EMPLOYEES, Area.ATTENDANCE, Area.OVERTIME,
            Area.TICKETS, Area.PAYROLL, Area.EARNINGS, Area.PAY_CHANGES);

        
        
        put(read(Role.CMO), Scope.TEAM, Area.EMPLOYEES, Area.ATTENDANCE, Area.OVERTIME,
            Area.TICKETS, Area.PAYROLL, Area.EARNINGS, Area.PAY_CHANGES);

        
        put(read(Role.HR), Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.LEAVE, Area.OVERTIME,
            Area.TICKETS, Area.PAYROLL, Area.REPORTS, Area.EARNINGS, Area.PAY_CHANGES);

        
        put(read(Role.PAYROLL), Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.OVERTIME, Area.PAYROLL,
            Area.REPORTS, Area.STATUTORY, Area.EARNINGS, Area.REMITTANCE, Area.PAY_CHANGES);

        
        put(read(Role.CEO), Scope.ALL, Area.EMPLOYEES, Area.PAYROLL, Area.REPORTS, Area.STATUTORY);

        
        put(read(Role.CFO), Scope.ALL, Area.EMPLOYEES, Area.PAYROLL, Area.REPORTS, Area.STATUTORY,
            Area.REMITTANCE, Area.EARNINGS);

        
        put(read(Role.COO), Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.OVERTIME, Area.REPORTS, Area.LEAVE);

        
        // The design document's matrix gives the System Administrator C, R, U, D on Attendance and
        // read on Payroll and Payslip, alongside full employee access.
        put(read(Role.ADMIN), Scope.ALL, Area.EMPLOYEES, Area.ATTENDANCE, Area.PAYROLL, Area.EARNINGS,
            Area.REPORTS);
    }

    private static Map<Area, Scope> read(Role role) { return READ.get(role); }

    private static void put(Map<Area, Scope> target, Scope scope, Area... areas) {
        for (Area area : areas) target.put(area, scope);
    }

    private AccessPolicy() { }

    
    public static void bind(OrgChart orgChart) { chart = orgChart; }

    
    public static Role roleOf(String employeeId) {
        if (employeeId == null || employeeId.isEmpty()) return Role.EMPLOYEE;
        Role override = ROLE_OVERRIDES.get(employeeId);
        if (override != null) return override;
        OrgChart bound = chart;
        return bound == null ? Role.EMPLOYEE : bound.roleOf(employeeId);
    }

    public static boolean isKnownAccount(String employeeId) {
        OrgChart bound = chart;
        return bound != null && bound.has(employeeId);
    }

    public static Optional<String> supervisorOf(String employeeId) {
        OrgChart bound = chart;
        return bound == null ? Optional.empty() : bound.supervisorOf(employeeId);
    }

    
    public static Set<String> teamOf(String employeeId) {
        OrgChart bound = chart;
        if (bound == null || employeeId == null) return Collections.emptySet();
        return bound.scopeOf(employeeId);
    }

    public static boolean isDirectReport(String supervisorId, String employeeId) {
        return supervisorOf(employeeId).map(supervisorId::equals).orElse(false);
    }

    

    
    public static synchronized void changeRole(Actor actor, String targetId, Role newRole, java.util.Collection<String> activeIds) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Administrator changes roles.");
        if (actor.employeeId.equals(targetId)) throw new AccessDenied("Nobody changes their own role.");
        if (!isKnownAccount(targetId)) throw new AccessDenied("That employee number is not on the roster.");
        boolean leadsPeople = !teamOf(targetId).isEmpty() && teamOf(targetId).size() > 1;
        if (leadsPeople && (newRole == Role.EMPLOYEE)) {
            throw new AccessDenied("Reassign this person's team before removing their supervisor role.");
        }
        ROLE_OVERRIDES.put(targetId, newRole);
    }

    public static synchronized void changeSupervisor(Actor actor, String targetId, String supervisorId) {
        if (actor.role != Role.ADMIN && actor.role != Role.HR) throw new AccessDenied("Only HR or the System Administrator changes reporting lines.");
        if (actor.employeeId.equals(targetId)) throw new AccessDenied("Nobody becomes their own supervisor.");
        if (supervisorId != null && !supervisorId.isEmpty() && !isKnownAccount(supervisorId)) {
            throw new AccessDenied("That supervisor is not on the roster.");
        }
        OrgChart bound = chart;
        if (bound != null) bound.reassign(targetId, supervisorId);
    }

    public static synchronized Map<String, Role> exportRoles() {
        Map<String, Role> out = new java.util.LinkedHashMap<>();
        OrgChart bound = chart;
        if (bound != null) for (var employee : bound.employees()) out.put(employee.getEid(), roleOf(employee.getEid()));
        return out;
    }

    public static synchronized Map<String, String> exportSupervisors() {
        Map<String, String> out = new java.util.LinkedHashMap<>();
        OrgChart bound = chart;
        if (bound != null) for (var employee : bound.employees()) supervisorOf(employee.getEid()).ifPresent(s -> out.put(employee.getEid(), s));
        return out;
    }

    public static synchronized void importRoles(Map<String, Role> roles, Map<String, String> supervisors) {
        ROLE_OVERRIDES.clear();
        if (roles != null) ROLE_OVERRIDES.putAll(roles);
        OrgChart bound = chart;
        if (bound != null && supervisors != null) {
            if (roles != null) {
                for (String id : roles.keySet()) if (bound.employee(id).isPresent()) bound.reassign(id, "");
            }
            for (Map.Entry<String, String> entry : supervisors.entrySet()) {
                if (bound.employee(entry.getKey()).isPresent()
                    && (entry.getValue() == null || entry.getValue().isEmpty() || bound.employee(entry.getValue()).isPresent()))
                    bound.reassign(entry.getKey(), entry.getValue());
            }
        }
    }

    public static void requireAdmin(Actor actor) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Administrator can open system configuration.");
    }

    public static void assignSupervisor(String employeeId, String supervisorId) {
        OrgChart bound = chart;
        if (bound != null) bound.reassign(employeeId, supervisorId);
    }

    

    public static Scope readScope(Role role, Area area) {
        return READ.getOrDefault(role, Collections.emptyMap()).getOrDefault(area, Scope.NONE);
    }

    public static boolean canRead(Actor actor, Area area, String targetEmployeeId) {
        if (actor == null || !actor.explicit) return false;      
        if (targetEmployeeId == null) return readScope(actor.role, area) == Scope.ALL;
        switch (readScope(actor.role, area)) {
            case ALL: return true;
            case TEAM: return teamOf(actor.employeeId).contains(targetEmployeeId);
            case SELF: return actor.employeeId.equals(targetEmployeeId);
            default: return false;
        }
    }

    public static void requireRead(Actor actor, Area area, String targetEmployeeId) {
        if (actor == null || !actor.explicit) throw new AccessDenied("Sign in to view " + area.name().toLowerCase(java.util.Locale.ROOT) + ".");
        if (!canRead(actor, area, targetEmployeeId)) {
            throw new AccessDenied(actor.role.label + " accounts cannot view " + area.name().toLowerCase(java.util.Locale.ROOT)
                + (targetEmployeeId == null ? "" : " for " + targetEmployeeId) + ".");
        }
    }

    
    public static void requireArea(Actor actor, Area area) {
        if (actor == null || !actor.explicit) throw new AccessDenied("Sign in to open " + area.name().toLowerCase(java.util.Locale.ROOT) + ".");
        if (readScope(actor.role, area) != Scope.ALL) {
            throw new AccessDenied(actor.role.label + " accounts cannot open " + area.name().toLowerCase(java.util.Locale.ROOT) + ".");
        }
    }

    

    public static void requireEmployeeEdit(Actor actor) {
        if (actor.role != Role.ADMIN && actor.role != Role.HR) {
            throw new AccessDenied("Only HR or the System Administrator can create or edit employee records.");
        }
    }

    
    public static void requireAttendanceEdit(Actor actor, String targetEmployeeId) {
        boolean allowed = actor.role == Role.ADMIN
            || ((actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, targetEmployeeId));
        if (!allowed) throw new AccessDenied("Only a supervisor (for their own team) or the System Administrator can edit attendance.");
    }

    public static void requireCreate(Actor actor, Area area, String targetEmployeeId) {
        boolean allowed = actor.employeeId.equals(targetEmployeeId) || actor.role == Role.ADMIN
            || ((actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, targetEmployeeId))
            || (actor.role == Role.HR && (area == Area.LEAVE || area == Area.TICKETS));
        if (!allowed) throw new AccessDenied("You can only file " + area.name().toLowerCase(java.util.Locale.ROOT) + " requests for yourself or your own team.");
    }

    public static void requireCancel(Actor actor, Area area, String ownerEmployeeId) {
        boolean allowed = actor.employeeId.equals(ownerEmployeeId) || actor.role == Role.ADMIN
            || (actor.role == Role.HR && (area == Area.LEAVE || area == Area.TICKETS));
        if (!allowed) throw new AccessDenied("Only the person who filed it (or HR/System Administrator) can cancel this request.");
    }

    
    public static void requireReview(Actor actor, Area area, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can approve or reject their own request.");
        boolean direct = (actor.role == Role.SUPERVISOR || actor.role == Role.CMO || actor.role == Role.COO)
            && isDirectReport(actor.employeeId, ownerEmployeeId);
        boolean allowed = actor.role == Role.ADMIN || direct || (area == Area.LEAVE && actor.role == Role.HR);
        if (!allowed) {
            throw new AccessDenied(area == Area.LEAVE
                ? "Leave is approved by the employee's supervisor, HR, or the System Administrator."
                : "Overtime is approved by the employee's supervisor or the System Administrator.");
        }
    }

    
    public static void requireEarningsEdit(Actor actor, String ownerEmployeeId) {
        boolean allowed = actor.role == Role.PAYROLL
            || ((actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, ownerEmployeeId));
        if (!allowed) throw new AccessDenied("Only Payroll Staff or the employee's own supervisor can change earnings and deductions.");
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can change their own pay.");
    }

    
    public static void requirePayrollProcessing(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.PAYROLL) throw new AccessDenied("Only Payroll Staff can process payroll.");
        if (ownerEmployeeId != null && actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can change their own pay.");
    }

    
    public static void requireScheduleEdit(Actor actor, String targetEmployeeId) {
        boolean allowed = actor.role == Role.ADMIN
            || ((actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, targetEmployeeId));
        if (!allowed) throw new AccessDenied("Only the employee's own supervisor or the System Administrator can change a schedule.");
    }

    public static void requirePayChangeRequest(Actor actor, String targetEmployeeId) {
        if (actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody can request a pay change for themselves.");
        boolean allowed = (actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, targetEmployeeId);
        if (!allowed) throw new AccessDenied("Pay-change requests are filed by the employee's own supervisor.");
    }

    public static void requirePayChangeReview(Actor actor, String type, String targetEmployeeId, String requesterId) {
        if (actor.employeeId.equals(targetEmployeeId) || actor.employeeId.equals(requesterId)) {
            throw new AccessDenied("The employee and the person who filed the request cannot decide it.");
        }
        boolean allowed = "BONUS".equals(type) ? actor.role == Role.PAYROLL : actor.role == Role.HR;
        if (!allowed) throw new AccessDenied("BONUS".equals(type) ? "Bonus requests are approved by Payroll Staff." : "This request is approved by HR.");
    }

    public static void requireHrManagement(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.HR) throw new AccessDenied("Only HR manages onboarding, offboarding and the HR lists.");
        if (targetEmployeeId != null && actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody can offboard themselves.");
    }

    
    public static boolean canHandleTicket(Actor actor, String queue, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) return false;
        if (actor.role == Role.ADMIN) return true;
        switch (queue) {
            case "Team Lead": return (actor.role == Role.SUPERVISOR || actor.role == Role.CMO) && isDirectReport(actor.employeeId, ownerEmployeeId);
            case "Payroll Staff": return actor.role == Role.PAYROLL;
            case "HR": return actor.role == Role.HR;
            default: return false;
        }
    }

    public static void requireTicketResolve(Actor actor, String queue, String ownerEmployeeId) {
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody handles their own ticket.");
        if (!canHandleTicket(actor, queue, ownerEmployeeId)) {
            throw new AccessDenied("This ticket is in the " + queue + " queue; it is handled by "
                + ("Team Lead".equals(queue) ? "the employee's supervisor"
                : "Payroll Staff".equals(queue) ? "Payroll Staff"
                : "HR".equals(queue) ? "HR" : "the System Administrator") + ".");
        }
    }

    
    public static void requireGovernmentRead(Actor actor) {
        if (actor.role != Role.HR && actor.role != Role.PAYROLL && actor.role != Role.CFO) {
            throw new AccessDenied("Government details are limited to HR, Payroll Staff and the CFO.");
        }
    }

    public static void requireGovernmentRequest(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.HR && actor.role != Role.PAYROLL) {
            throw new AccessDenied("Only HR or Payroll Staff file government-details requests; the System Administrator edits directly.");
        }
        if (actor.employeeId.equals(targetEmployeeId)) throw new AccessDenied("Nobody files government-details requests about themselves.");
    }

    public static void requireGovernmentAdmin(Actor actor, String targetEmployeeId) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Administrator edits or approves government details.");
        if (targetEmployeeId != null && actor.employeeId.equals(targetEmployeeId) && actor.explicit) {
            throw new AccessDenied("The System Administrator does not approve requests about their own record.");
        }
    }

    
    public static void requirePayslipCorrectionRequest(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.PAYROLL) throw new AccessDenied("Payroll Staff file payslip corrections; the System Administrator approves them.");
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody corrects their own payslip.");
    }

    public static void requirePayslipCorrectionReview(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.ADMIN) throw new AccessDenied("Only the System Administrator approves payslip corrections.");
        if (actor.explicit && actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody approves a correction to their own payslip.");
    }

    public static void requireFinalize(Actor actor, String ownerEmployeeId) {
        if (actor.role != Role.PAYROLL && actor.role != Role.CFO) {
            throw new AccessDenied("Only Payroll Staff prepare payslips; the CFO may release a run.");
        }
        if (actor.employeeId.equals(ownerEmployeeId)) throw new AccessDenied("Nobody can finalize their own payslip.");
    }
}
