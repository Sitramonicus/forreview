package ph.motorph.o_oop;

import ph.motorph.sp_secpol.AccessPolicy;

public final class Policies {
    private Policies() {}

    public static AccessPolicy.Role forPosition(String position) {
        String title = position == null ? "" : position.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
        switch (title) {
            case "chief executive officer":       return AccessPolicy.Role.CEO;
            case "chief operating officer":       return AccessPolicy.Role.COO;
            case "chief finance officer":         return AccessPolicy.Role.CFO;
            case "chief financial officer":       return AccessPolicy.Role.CFO;
            case "chief marketing officer":       return AccessPolicy.Role.CMO;

            
            case "accounting head":               return AccessPolicy.Role.CFO;

            
            case "hr manager":
            case "hr team leader":
            case "hr rank and file":              return AccessPolicy.Role.HR;
            case "payroll manager":
            case "payroll team leader":
            case "payroll rank and file":         return AccessPolicy.Role.PAYROLL;

            
            case "it operations and systems":     return AccessPolicy.Role.ADMIN;

            
            case "account team leader":
            case "account manager":               return AccessPolicy.Role.SUPERVISOR;

            
            default:                              return AccessPolicy.Role.EMPLOYEE;
        }
    }

    
    public static String departmentOf(String position) {
        String title = position == null ? "" : position.replaceAll("\\s+", " ").trim().toLowerCase(java.util.Locale.ROOT);
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

    
    public static boolean isLeadershipTitle(String position) {
        AccessPolicy.Role role = forPosition(position);
        return role == AccessPolicy.Role.SUPERVISOR || role == AccessPolicy.Role.CMO;
    }
}
