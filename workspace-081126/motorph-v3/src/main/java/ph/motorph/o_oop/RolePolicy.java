package ph.motorph.o_oop;

import ph.motorph.sp_secpol.AccessPolicy;

/**
 * Position title → access role. The one place a job title becomes a permission, so it is a table you can read and
 * argue with rather than a chain of comparisons buried in a request handler.
 *
 * Two rules keep this from over-granting:
 * <ul>
 *   <li><b>A title grants exactly one role, never a family.</b> "Accounting Head" is the CFO's deputy and gets the
 *       CFO's read scope; "Sales &amp; Marketing" is a rank-and-file job in the marketing department and is an
 *       ordinary employee, even though the CMO leads that department.</li>
 *   <li><b>An unknown title is an employee.</b> New titles must be added here deliberately; falling through to a
 *       wider role would let a typo in the employee file hand out payroll access.</li>
 * </ul>
 *
 * Titles are matched case-insensitively and with the file's stray double spaces collapsed.
 */
public final class RolePolicy {
    private RolePolicy() {}

    public static AccessPolicy.Role forPosition(String position) {
        String title = position == null ? "" : position.replaceAll("\\s+", " ").trim().toLowerCase();
        switch (title) {
            case "chief executive officer":       return AccessPolicy.Role.CEO;
            case "chief operating officer":       return AccessPolicy.Role.COO;
            case "chief finance officer":         return AccessPolicy.Role.CFO;
            case "chief financial officer":       return AccessPolicy.Role.CFO;
            case "chief marketing officer":       return AccessPolicy.Role.CMO;

            // Finance leadership below the CFO: read-heavy, no processing.
            case "accounting head":               return AccessPolicy.Role.CFO;

            // People and pay administration.
            case "hr manager":
            case "hr team leader":
            case "hr rank and file":              return AccessPolicy.Role.HR;
            case "payroll manager":
            case "payroll team leader":
            case "payroll rank and file":         return AccessPolicy.Role.PAYROLL;

            // Technical administration. Deliberately not a salary-visibility role.
            case "it operations and systems":     return AccessPolicy.Role.ADMIN;

            // Team leaders and account managers lead people: supervisor scope over their reporting tree.
            case "account team leader":
            case "account manager":               return AccessPolicy.Role.SUPERVISOR;

            // Everything else is an ordinary employee, including department staff under an executive.
            default:                              return AccessPolicy.Role.EMPLOYEE;
        }
    }

    /** Department names as the file words them, used for grouping and for the HR/payroll split. */
    public static String departmentOf(String position) {
        String title = position == null ? "" : position.replaceAll("\\s+", " ").trim().toLowerCase();
        if (title.startsWith("chief executive")) return "Executive Office";
        if (title.startsWith("chief operating")) return "Operations";
        if (title.startsWith("chief finance") || title.startsWith("chief financial")) return "Finance";
        if (title.startsWith("chief marketing")) return "Marketing";
        if (title.startsWith("it operations")) return "IT";
        if (title.startsWith("hr ")) return "Human Resources";
        if (title.startsWith("payroll") || title.startsWith("accounting")) return "Finance";
        if (title.startsWith("account ")) return "Accounts";
        if (title.startsWith("sales")) return "Marketing";
        if (title.startsWith("supply chain")) return "Logistics";
        if (title.startsWith("customer service")) return "Customer Service";
        return "Operations";
    }

    /**
     * Whether a title is a leadership title, used only for display hints (a "Team" tab makes sense for these).
     * Authority always comes from {@link #forPosition}, never from this.
     */
    public static boolean isLeadershipTitle(String position) {
        AccessPolicy.Role role = forPosition(position);
        return role == AccessPolicy.Role.SUPERVISOR || role == AccessPolicy.Role.CMO;
    }
}
