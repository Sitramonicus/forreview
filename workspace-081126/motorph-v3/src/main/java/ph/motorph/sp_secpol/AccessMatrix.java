package ph.motorph.sp_secpol;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.OrgChart;
import ph.motorph.sp_secpol.AccessPolicy.Role;

/**
 * What each role may do, and whose records it may do it to.
 *
 * Two separate questions, deliberately answered in two different places:
 * <ul>
 *   <li><b>Capability</b> — a role either holds a permission or does not. This is the table below.</li>
 *   <li><b>Scope</b> — which employees a granted permission reaches: only yourself, your reporting tree, or the
 *       company. This comes from the {@link OrgChart}, never from a branch or a department.</li>
 * </ul>
 *
 * The brief's principle that "hiding a button is not access control" is implemented literally:
 * {@link #can(Employee, Permission, Employee, OrgChart)} is called by the server for every protected read and write,
 * and the browser's hidden tabs are a convenience on top of it, not a substitute.
 */
public final class AccessMatrix {
    private AccessMatrix() {}

    public enum Scope { NONE, SELF, TEAM, COMPANY }

    public enum Permission {
        // directory
        EMPLOYEE_VIEW, EMPLOYEE_EDIT, EMPLOYEE_CREATE, EMPLOYEE_SENSITIVE_VIEW,
        // attendance
        ATTENDANCE_VIEW, ATTENDANCE_EDIT, ATTENDANCE_APPROVE,
        // requests and tickets
        LEAVE_REQUEST, LEAVE_APPROVE, OVERTIME_REQUEST, OVERTIME_APPROVE, TICKET_VIEW, TICKET_HANDLE,
        // money
        PAYSLIP_VIEW, PAYROLL_VIEW_COSTS, PAYROLL_RUN, PAYROLL_APPROVE, STATUTORY_VIEW, REMITTANCE_VIEW,
        PAY_PROFILE_EDIT, DISBURSEMENT_RECORD,
        // reports
        REPORTS_VIEW,
        // administration
        ACCOUNT_ADMIN, ROLE_ADMIN, AUDIT_VIEW, CONFIG_EDIT,
        // everyone
        SELF_SERVICE
    }

    private static final Map<Role, Set<Permission>> GRANTS = new EnumMap<>(Role.class);
    static {
        // Everyone can see their own record, their own payslips, and submit their own requests.
        Set<Permission> base = EnumSet.of(Permission.SELF_SERVICE, Permission.PAYSLIP_VIEW, Permission.ATTENDANCE_VIEW,
            Permission.LEAVE_REQUEST, Permission.OVERTIME_REQUEST, Permission.TICKET_VIEW, Permission.EMPLOYEE_VIEW);

        // Employee: own records only.
        GRANTS.put(Role.EMPLOYEE, EnumSet.copyOf(base));

        // Team supervisor: the reporting tree, plus the approvals that belong to a team lead.
        Set<Permission> supervisor = EnumSet.copyOf(base);
        supervisor.addAll(EnumSet.of(Permission.ATTENDANCE_EDIT, Permission.ATTENDANCE_APPROVE, Permission.LEAVE_APPROVE,
            Permission.OVERTIME_APPROVE, Permission.TICKET_HANDLE, Permission.REPORTS_VIEW));
        GRANTS.put(Role.SUPERVISOR, supervisor);

        // HR: employee administration and HR workflows. Payroll amounts are visible only through PAYSLIP rows for
        // people they administer, never as company totals, and processing is not theirs.
        Set<Permission> hr = EnumSet.copyOf(base);
        hr.addAll(EnumSet.of(Permission.EMPLOYEE_EDIT, Permission.EMPLOYEE_CREATE, Permission.EMPLOYEE_SENSITIVE_VIEW,
            Permission.ATTENDANCE_VIEW, Permission.ATTENDANCE_EDIT, Permission.ATTENDANCE_APPROVE,
            Permission.LEAVE_APPROVE, Permission.OVERTIME_APPROVE, Permission.TICKET_VIEW, Permission.TICKET_HANDLE,
            Permission.PAYSLIP_VIEW, Permission.REPORTS_VIEW, Permission.ACCOUNT_ADMIN, Permission.AUDIT_VIEW));
        GRANTS.put(Role.HR, hr);

        // Payroll staff: prepares and calculates. Does not approve, and does not administer accounts.
        Set<Permission> payroll = EnumSet.copyOf(base);
        payroll.addAll(EnumSet.of(Permission.EMPLOYEE_VIEW, Permission.ATTENDANCE_VIEW, Permission.PAYSLIP_VIEW,
            Permission.PAYROLL_VIEW_COSTS, Permission.PAYROLL_RUN, Permission.STATUTORY_VIEW, Permission.REMITTANCE_VIEW,
            Permission.PAY_PROFILE_EDIT, Permission.DISBURSEMENT_RECORD, Permission.REPORTS_VIEW, Permission.AUDIT_VIEW));
        GRANTS.put(Role.PAYROLL, payroll);

        // CEO: company-wide oversight, read-only. No processing, no editing, no approvals.
        Set<Permission> ceo = EnumSet.copyOf(base);
        ceo.addAll(EnumSet.of(Permission.EMPLOYEE_VIEW, Permission.PAYROLL_VIEW_COSTS, Permission.REPORTS_VIEW,
            Permission.STATUTORY_VIEW, Permission.AUDIT_VIEW));
        GRANTS.put(Role.CEO, ceo);

        // CFO: adds financial detail and remittances. Still no processing: authority to look at the money is not
        // authority to move it.
        Set<Permission> cfo = EnumSet.copyOf(ceo);
        cfo.addAll(EnumSet.of(Permission.REMITTANCE_VIEW, Permission.EMPLOYEE_SENSITIVE_VIEW));
        GRANTS.put(Role.CFO, cfo);

        // COO: operational visibility — attendance, overtime, holiday work — without individual salary records.
        Set<Permission> coo = EnumSet.copyOf(base);
        coo.addAll(EnumSet.of(Permission.ATTENDANCE_VIEW, Permission.OVERTIME_APPROVE, Permission.REPORTS_VIEW,
            Permission.TICKET_VIEW));
        GRANTS.put(Role.COO, coo);

        // CMO: a department head with a three-person team. Same permissions as a supervisor, nothing financial.
        GRANTS.put(Role.CMO, EnumSet.copyOf(supervisor));

        // System administrator: technical administration, accounts and configuration. Explicitly not salary
        // visibility — a technical admin who also needs costs gets a second, separate grant.
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

    /**
     * The scope a permission reaches for this role. Approvals reach the team even for roles whose read scope is
     * wider or narrower; payroll processing reaches the company but never one's own payslip run.
     */
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

    /**
     * The decision the server makes on every request: does {@code me} hold {@code permission} at a scope that covers
     * {@code about}?
     */
    public static boolean can(Employee me, Permission permission, Employee about, OrgChart chart) {
        if (me == null) return false;
        Role role = chart.roleOf(me.getEid());
        Scope scope = scopeOf(role, permission);
        if (scope == Scope.NONE) return false;
        if (about == null) return scope == Scope.COMPANY || scope == Scope.SELF;   // "my own" operations
        String target = about.getEid();
        switch (scope) {
            case SELF: return target.equals(me.getEid());
            case TEAM: return chart.scopeOf(me.getEid()).contains(target);
            case COMPANY: return true;
            default: return false;
        }
    }

    /** Whether the role may act on its own record for this permission (payroll staff must not finalise their own slip). */
    public static boolean allowsSelfAction(Role role, Permission permission) {
        switch (permission) {
            case PAYROLL_RUN:
            case PAYROLL_APPROVE:
                return false;
            default:
                return true;
        }
    }

    /** A readable summary for the role matrix page and for the docs. */
    public static Map<String, String> describe() {
        Map<String, String> out = new LinkedHashMapHolder().map;
        for (Role role : Role.values()) {
            StringBuilder line = new StringBuilder();
            for (Permission permission : Permission.values()) {
                Scope scope = scopeOf(role, permission);
                if (scope == Scope.NONE) continue;
                if (line.length() > 0) line.append(", ");
                line.append(permission.name().toLowerCase().replace('_', ' ')).append(" (").append(scope.name().toLowerCase()).append(')');
            }
            out.put(role.label, line.toString());
        }
        return out;
    }

    private static final class LinkedHashMapHolder {
        final Map<String, String> map = new java.util.LinkedHashMap<>();
    }
}
