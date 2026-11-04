package com.motorph.security;

public class RolePermission {

    private String role;

    public RolePermission(String role) {
        this.role = role;
    }

    public boolean canAccessEmployee() {
        return role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("HR");
    }

    public boolean canAccessPayroll() {
        return role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("FINANCE");
    }

    public boolean canAccessReports() {
        return role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("FINANCE");
    }

    public boolean canAccessLeave() {
        return true;
    }

    public boolean canAccessDashboard() {
        return true;
    }
}