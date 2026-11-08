package ph.motorph.web;

import java.math.BigDecimal;
import java.util.Objects;

/** A directory record: identity, placement and the pay inputs MotorPH's payslip needs. */
public final class Employee {
    private final String id;
    private final String name;
    private final String department;
    private final String role;
    private final String location;
    private final String status;
    private final BigDecimal monthlyBasic;
    private final BigDecimal riceSubsidy;
    private final BigDecimal phoneAllowance;
    private final BigDecimal clothingAllowance;
    /** Employee number from MotorPH's "Immediate Supervisor" column; null when not supplied. */
    private final String supervisorId;

    /** Directory-only record with no pay data; used where salary is irrelevant. */
    public Employee(String id, String name, String department, String role, String location, String status) {
        this(id, name, department, role, location, status, null, null, null, null);
    }

    public Employee(String id, String name, String department, String role, String location, String status,
                    BigDecimal monthlyBasic, BigDecimal riceSubsidy, BigDecimal phoneAllowance, BigDecimal clothingAllowance) {
        this(id, name, department, role, location, status, monthlyBasic, riceSubsidy, phoneAllowance,
            clothingAllowance, null);
    }

    public Employee(String id, String name, String department, String role, String location, String status,
                    BigDecimal monthlyBasic, BigDecimal riceSubsidy, BigDecimal phoneAllowance,
                    BigDecimal clothingAllowance, String supervisorId) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.department = requireText(department, "department");
        this.role = requireText(role, "role");
        this.location = requireText(location, "location");
        this.status = requireText(status, "status");
        this.monthlyBasic = nonNegative(monthlyBasic, "Monthly basic pay");
        this.riceSubsidy = nonNegative(riceSubsidy, "Rice subsidy");
        this.phoneAllowance = nonNegative(phoneAllowance, "Phone allowance");
        this.clothingAllowance = nonNegative(clothingAllowance, "Clothing allowance");
        // "N/A" in MotorPH's file means the person has no supervisor (the CEO), not a missing value.
        String supervisor = supervisorId == null ? "" : supervisorId.trim();
        this.supervisorId = supervisor.isEmpty() || "N/A".equalsIgnoreCase(supervisor) ? null : supervisor;
    }

    /** MotorPH numbers employees with five digits; anything else is not a directory ID. */
    public static boolean isValidEmployeeNumber(String id) {
        return id != null && id.matches("[0-9]{5}");
    }

    public Employee withPay(BigDecimal basic, BigDecimal rice, BigDecimal phone, BigDecimal clothing) {
        return new Employee(id, name, department, role, location, status, basic, rice, phone, clothing, supervisorId);
    }

    /** The employee number in MotorPH's "Immediate Supervisor" column; null when none. */
    public String getSupervisorId() {
        return supervisorId;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    private static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value == null ? null : Money.round(value);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public BigDecimal getMonthlyBasic() { return monthlyBasic; }
    public BigDecimal getRiceSubsidy() { return riceSubsidy; }
    public BigDecimal getPhoneAllowance() { return phoneAllowance; }
    public BigDecimal getClothingAllowance() { return clothingAllowance; }

    /** True when this record carries enough pay data to be included in a payroll run. */
    public boolean hasPayProfile() {
        return monthlyBasic != null && monthlyBasic.signum() > 0
            && riceSubsidy != null && phoneAllowance != null && clothingAllowance != null;
    }
}
