package ph.motorph.web;

import java.util.Locale;

/**
 * The five account types named in MotorPH's System Design Document (MO-IT108 2nd Milestone,
 * §2.2.1 "RBAC Roles"). The order here is only display order; authority comes from {@link Policy}.
 *
 * <p>{@link #APPLICANT} is not one of the five. It exists for a signed-in account that HR has not
 * linked to an employee number yet, and every matrix cell for it is NONE. Without it a brand-new
 * account could not even sign in to ask for access — and the alternative, silently granting
 * {@code EMPLOYEE}, would hand payroll data to anyone with an email address.
 */
public enum Role {
    // Declaration order is the seniority order used to pick a default seat, so a screen that does not
    // name a seat degrades to the most the person is, never to the least.
    SYSTEM_ADMIN("System Admin", 50),
    HR("HR", 40),
    PAYROLL("Payroll Staff", 40),
    SUPERVISOR("Supervisor", 30),
    EMPLOYEE("Employee", 20),
    APPLICANT("Unlinked account", 10);

    private final String label;
    private final int rank;

    Role(String label, int rank) {
        this.label = label;
        this.rank = rank;
    }

    /** Seniority, used only to choose which seat to act under by default. Never used to grant access. */
    public int rank() {
        return rank;
    }

    public String label() {
        return label;
    }

    /**
     * Reads a role from an authority string. Anything unrecognised returns {@link #EMPLOYEE}, the
     * least-privileged role: an unknown or misspelled role can never grant access.
     */
    public static Role fromClaim(String value) {
        // No claim at all is treated the same as an unrecognised one: unlinked, not Employee.
        if (value == null || value.trim().isEmpty()) return APPLICANT;
        String key = value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        switch (key) {
            case "SYSTEM_ADMIN":
            case "SYSTEMADMIN":
            case "ADMIN":
                return SYSTEM_ADMIN;
            case "HR":
            case "HR_MANAGEMENT":
                return HR;
            case "PAYROLL":
            case "PAYROLL_STAFF":
            case "PAYROLL_SUPERVISION":
                return PAYROLL;
            case "SUPERVISOR":
            case "SUPERVISOR_TEAM_LEAD":
            case "TEAM_LEAD":
                return SUPERVISOR;
            case "EMPLOYEE":
                return EMPLOYEE;
            case "APPLICANT":
            case "UNLINKED":
                return APPLICANT;
            default:
                // A misspelled or unknown role must never grant anything, and an unknown claim is not
                // evidence of employment. Unknown means unlinked, not Employee.
                return APPLICANT;
        }
    }
}
