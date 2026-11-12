package ph.motorph.sp_secpol;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.OrgChart;
import ph.motorph.sp_secpol.AccessPolicy.Role;

public final class AccessMatrix {
    private AccessMatrix() {}

    public enum Scope { NONE, SELF, TEAM, COMPANY }

    public enum Permission {
        
        EMPLOYEE_VIEW, EMPLOYEE_EDIT, EMPLOYEE_CREATE, EMPLOYEE_SENSITIVE_VIEW,
        
        ATTENDANCE_VIEW, ATTENDANCE_EDIT, ATTENDANCE_APPROVE,
        
        LEAVE_REQUEST, LEAVE_APPROVE, OVERTIME_REQUEST, OVERTIME_APPROVE, TICKET_VIEW, TICKET_HANDLE,
        
        PAYSLIP_VIEW, PAYROLL_VIEW_COSTS, PAYROLL_RUN, PAYROLL_APPROVE, STATUTORY_VIEW, REMITTANCE_VIEW,
        PAY_PROFILE_EDIT, DISBURSEMENT_RECORD,
        
        REPORTS_VIEW,
        
        ACCOUNT_ADMIN, ROLE_ADMIN, AUDIT_VIEW, CONFIG_EDIT,
        
        SELF_SERVICE
    }

    private static final Map<Role, Set<Permission>> GRANTS = new EnumMap<>(Role.class);
    static {
        
        Set<Permission> base = EnumSet.of(Permission.SELF_SERVICE, Permission.PAYSLIP_VIEW, Permission.ATTENDANCE_VIEW,
            Permission.LEAVE_REQUEST, Permission.OVERTIME_REQUEST, Permission.TICKET_VIEW, Permission.EMPLOYEE_VIEW);

        
        GRANTS.put(Role.EMPLOYEE, EnumSet.copyOf(base));

        
        Set<Permission> supervisor = EnumSet.copyOf(base);
        supervisor.addAll(EnumSet.of(Permission.ATTENDANCE_EDIT, Permission.ATTENDANCE_APPROVE, Permission.LEAVE_APPROVE,
            Permission.OVERTIME_APPROVE, Permission.TICKET_HANDLE, Permission.REPORTS_VIEW));
        GRANTS.put(Role.SUPERVISOR, supervisor);

        
        
        Set<Permission> hr = EnumSet.copyOf(base);
        hr.addAll(EnumSet.of(Permission.EMPLOYEE_EDIT, Permission.EMPLOYEE_CREATE, Permission.EMPLOYEE_SENSITIVE_VIEW,
            Permission.ATTENDANCE_VIEW, Permission.ATTENDANCE_EDIT, Permission.ATTENDANCE_APPROVE,
            Permission.LEAVE_APPROVE, Permission.OVERTIME_APPROVE, Permission.TICKET_VIEW, Permission.TICKET_HANDLE,
            Permission.PAYSLIP_VIEW, Permission.REPORTS_VIEW, Permission.ACCOUNT_ADMIN, Permission.AUDIT_VIEW));
        GRANTS.put(Role.HR, hr);

        
        Set<Permission> payroll = EnumSet.copyOf(base);
        payroll.addAll(EnumSet.of(Permission.EMPLOYEE_VIEW, Permission.ATTENDANCE_VIEW, Permission.PAYSLIP_VIEW,
            Permission.PAYROLL_VIEW_COSTS, Permission.PAYROLL_RUN, Permission.STATUTORY_VIEW, Permission.REMITTANCE_VIEW,
            Permission.PAY_PROFILE_EDIT, Permission.DISBURSEMENT_RECORD, Permission.REPORTS_VIEW, Permission.AUDIT_VIEW));
        GRANTS.put(Role.PAYROLL, payroll);

        
        Set<Permission> ceo = EnumSet.copyOf(base);
        ceo.addAll(EnumSet.of(Permission.EMPLOYEE_VIEW, Permission.PAYROLL_VIEW_COSTS, Permission.REPORTS_VIEW,
            Permission.STATUTORY_VIEW, Permission.AUDIT_VIEW));
        GRANTS.put(Role.CEO, ceo);

        
        
        Set<Permission> cfo = EnumSet.copyOf(ceo);
        cfo.addAll(EnumSet.of(Permission.REMITTANCE_VIEW, Permission.EMPLOYEE_SENSITIVE_VIEW));
        GRANTS.put(Role.CFO, cfo);

        
        Set<Permission> coo = EnumSet.copyOf(base);
        coo.addAll(EnumSet.of(Permission.ATTENDANCE_VIEW, Permission.OVERTIME_APPROVE, Permission.REPORTS_VIEW,
            Permission.TICKET_VIEW));
        GRANTS.put(Role.COO, coo);

        
        GRANTS.put(Role.CMO, EnumSet.copyOf(supervisor));

        
        
        Set<Permission> admin = EnumSet.copyOf(base);
        admin.addAll(EnumSet.of(Permission.ACCOUNT_ADMIN, Permission.ROLE_ADMIN, Permission.AUDIT_VIEW,
            Permission.CONFIG_EDIT, Permission.EMPLOYEE_VIEW));
        GRANTS.put(Role.ADMIN, admin);
    }

    public static Set<Permission> grants(Role role) {
        Set<Permission> held = GRANTS.get(role);
        return held == null ? Collections.emptySet() : Collections.unmodifiableSet(held);
    }

    public static boolean holds(Role role, Permission permission) { return grants(role).contains(permission); }

    
    public static Scope scopeOf(Role role, Permission permission) {
        if (!holds(role, permission)) return Scope.NONE;
        switch (permission) {
            case SELF_SERVICE: return Scope.SELF;
            case PAYSLIP_VIEW:
            case ATTENDANCE_VIEW:
            case EMPLOYEE_VIEW:
            case LEAVE_APPROVE:
            case OVERTIME_APPROVE:
            case ATTENDANCE_APPROVE:
            case TICKET_HANDLE:
                switch (role) {
                    case EMPLOYEE: return Scope.SELF;
                    case SUPERVISOR: case CMO: return Scope.TEAM;
                    default: return Scope.COMPANY;
                }
            case ATTENDANCE_EDIT:
                return role == Role.SUPERVISOR || role == Role.CMO ? Scope.TEAM : Scope.COMPANY;
            case LEAVE_REQUEST:
            case OVERTIME_REQUEST: return Scope.SELF;
            case TICKET_VIEW: return role == Role.EMPLOYEE ? Scope.SELF : Scope.TEAM;
            case PAYROLL_VIEW_COSTS:
            case PAYROLL_RUN:
            case STATUTORY_VIEW:
            case REMITTANCE_VIEW:
            case REPORTS_VIEW:
            case EMPLOYEE_EDIT:
            case EMPLOYEE_CREATE:
            case EMPLOYEE_SENSITIVE_VIEW:
            case PAY_PROFILE_EDIT:
            case DISBURSEMENT_RECORD:
            case ACCOUNT_ADMIN:
            case ROLE_ADMIN:
            case AUDIT_VIEW:
            case CONFIG_EDIT:
                return Scope.COMPANY;
            default: return Scope.NONE;
        }
    }

    
    public static boolean can(Employee me, Permission permission, Employee about, OrgChart chart) {
        if (me == null) return false;
        Role role = chart.roleOf(me.getEid());
        Scope scope = scopeOf(role, permission);
        if (scope == Scope.NONE) return false;
        if (about == null) return scope == Scope.COMPANY || scope == Scope.SELF;   
        String target = about.getEid();
        switch (scope) {
            case SELF: return target.equals(me.getEid());
            case TEAM: return chart.scopeOf(me.getEid()).contains(target);
            case COMPANY: return true;
            default: return false;
        }
    }

    
    public static boolean allowsSelfAction(Role role, Permission permission) {
        switch (permission) {
            case PAYROLL_RUN:
            case PAYROLL_APPROVE:
                return false;
            default:
                return true;
        }
    }

    
    public static Map<String, String> describe() {
        Map<String, String> out = new LinkedHashMapHolder().map;
        for (Role role : Role.values()) {
            StringBuilder line = new StringBuilder();
            for (Permission permission : Permission.values()) {
                Scope scope = scopeOf(role, permission);
                if (scope == Scope.NONE) continue;
                if (line.length() > 0) line.append(", ");
                line.append(permission.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ')).append(" (").append(scope.name().toLowerCase(java.util.Locale.ROOT)).append(')');
            }
            out.put(role.label, line.toString());
        }
        return out;
    }

    private static final class LinkedHashMapHolder {
        final Map<String, String> map = new java.util.LinkedHashMap<>();
    }
}
