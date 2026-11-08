package ph.motorph.o_oop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * One row of MotorPH's employee file ({@code Copy of MotorPH_Employee Data - Employee Details.csv}).
 *
 * Read construction, no mutation: the file is the single source of truth for identity, pay inputs and the
 * reporting line, so an {@code Employee} is never edited field-by-field. Corrections go through
 * {@link ph.motorph.o_oop.HrRecords} or a fresh import, which keeps every edit auditable.
 *
 * Pay fields are kept exactly as the file states them (a monthly basic of 53,500.00, not a derived daily rate).
 * Derivation belongs to the calculation layer ({@code c_calc}), never to the record.
 */
public final class Employee {
    // ── identity ────────────────────────────────────────────────────────────────
    private final String eid,         // employee number as the file writes it, e.g. "10015"
                         firstName,
                         lastName,
                         employeeStatus,   // Regular | Probationary
                         position,
                         department;
    private final LocalDate birthday;

    // ── personal details held for statutory filings, not for display ─────────────
    private final String phone, street, city, region, postalCode,
                         sssNumber, philhealthNumber, tinNumber, pagibigNumber;

    // ── pay inputs ───────────────────────────────────────────────────────────────
    private final BigDecimal monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance,
                             semiMonthlyRate, hourlyRate;

    // ── reporting line ───────────────────────────────────────────────────────────
    private final String supervisorEid;   // "" for the Chief Executive Officer, who reports to the board
    private final String branchName;

    private Employee(Builder b) {
        this.eid = require(b.eid, "employee number");
        this.firstName = require(b.firstName, "first name");
        this.lastName = require(b.lastName, "last name");
        this.birthday = b.birthday == null ? LocalDate.of(1970, 1, 1) : b.birthday;
        this.employeeStatus = require(b.employeeStatus, "employment status");
        this.position = require(b.position, "position");
        this.department = require(b.department, "department");
        this.phone = blankToEmpty(b.phone);
        this.street = blankToEmpty(b.street);
        this.city = blankToEmpty(b.city);
        this.region = blankToEmpty(b.region);
        this.postalCode = blankToEmpty(b.postalCode);
        this.sssNumber = blankToEmpty(b.sssNumber);
        this.philhealthNumber = blankToEmpty(b.philhealthNumber);
        this.tinNumber = blankToEmpty(b.tinNumber);
        this.pagibigNumber = blankToEmpty(b.pagibigNumber);
        this.monthlyBasic = money(b.monthlyBasic, "monthly basic rate");
        this.riceSubsidy = money(b.riceSubsidy, "rice subsidy");
        this.phoneAllowance = money(b.phoneAllowance, "phone allowance");
        this.clothingAllowance = money(b.clothingAllowance, "clothing allowance");
        this.semiMonthlyRate = money(b.semiMonthlyRate, "semi-monthly rate");
        this.hourlyRate = money(b.hourlyRate, "hourly rate");
        this.supervisorEid = blankToEmpty(b.supervisorEid);
        this.branchName = require(b.branchName, "branch");
    }

    private static String require(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String text = value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return text;
    }
    private static String blankToEmpty(String value) { return value == null ? "" : value.trim(); }
    private static BigDecimal money(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.signum() < 0) throw new IllegalArgumentException(field + " must not be negative");
        return value;
    }

    // ── accessors (read-only by design) ─────────────────────────────────────────
    public String getEid() { return eid; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    /** "Garcia, Manuel III" — the form the file uses for supervisors and for payslips. */
    public String getFullName() { return lastName + ", " + firstName; }
    /** "Manuel III Garcia" — the natural-order form used in headings and pickers. */
    public String getDisplayName() { return firstName + " " + lastName; }
    public LocalDate getBirthday() { return birthday; }
    public String getEmployeeStatus() { return employeeStatus; }
    public String getPosition() { return position; }
    public String getDepartment() { return department; }
    public String getPhone() { return phone; }
    public String getStreet() { return street; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getPostalCode() { return postalCode; }
    public String getSssNumber() { return sssNumber; }
    public String getPhilhealthNumber() { return philhealthNumber; }
    public String getTinNumber() { return tinNumber; }
    public String getPagibigNumber() { return pagibigNumber; }
    public BigDecimal getMonthlyBasic() { return monthlyBasic; }
    public BigDecimal getRiceSubsidy() { return riceSubsidy; }
    public BigDecimal getPhoneAllowance() { return phoneAllowance; }
    public BigDecimal getClothingAllowance() { return clothingAllowance; }
    public BigDecimal getSemiMonthlyRate() { return semiMonthlyRate; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public String getSupervisorEid() { return supervisorEid; }
    public boolean hasSupervisor() { return !supervisorEid.isEmpty(); }
    public String getBranchName() { return branchName; }

    /** Allowances the file lists for everyone; kept separate from basic so payslips can itemise them. */
    public BigDecimal getTotalAllowances() { return riceSubsidy.add(phoneAllowance).add(clothingAllowance); }

    // ── the names the rest of the application already speaks ────────────────────
    // The directory tables, workflow documents and payslip writer all use these five; keeping them as the single
    // spelling avoids churning forty call sites while the record itself stays read-only and file-shaped.

    /** Internal identifier: for imported staff this is the employee number from the file, e.g. "10015". */
    public String getId() { return eid; }
    /** "Manuel III Garcia" — natural order, as the roster and pickers show it. */
    public String getName() { return getDisplayName(); }
    public String getRole() { return position; }
    /** Branch name. Replaces the old free-text location; the value is one of {@link Branch}. */
    public String getLocation() { return branchName; }
    /** "Active" unless HR has recorded a separation date; probationary staff are active too. */
    public String getStatus() { return employeeStatus; }

    /**
     * Convenience constructor for a new hire recorded through HR rather than imported from the file.
     * Pay defaults to zero because a hire with no rate yet must not silently inherit one.
     */
    public static Employee hired(String id, String name, String department, String role, String location, String status) {
        String[] parts = name.trim().isEmpty() ? new String[]{""} : name.trim().split(" +");
        String lastName = parts.length > 1 ? parts[parts.length - 1] : "";
        String firstName = parts.length > 1 ? name.substring(0, name.lastIndexOf(lastName)).trim() : name;
        return new Builder().eid(id).firstName(firstName == null || firstName.isEmpty() ? name : firstName)
            .lastName(lastName.isEmpty() ? "-" : lastName).employeeStatus(status).position(role)
            .department(department).branchName(location)
            .monthlyBasic(BigDecimal.ZERO).riceSubsidy(BigDecimal.ZERO).phoneAllowance(BigDecimal.ZERO)
            .clothingAllowance(BigDecimal.ZERO).semiMonthlyRate(BigDecimal.ZERO).hourlyRate(BigDecimal.ZERO)
            .build();
    }

    @Override public String toString() { return eid + " " + getFullName(); }

    /** Tolerant of the file's whitespace quirks, strict about what actually matters. */
    public static final class Builder {
        private String eid, firstName, lastName, employeeStatus, position, department;
        private LocalDate birthday;
        private String phone, street, city, region, postalCode;
        private String sssNumber, philhealthNumber, tinNumber, pagibigNumber;
        private BigDecimal monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance, semiMonthlyRate, hourlyRate;
        private String supervisorEid, branchName;

        public Builder eid(String v) { eid = v; return this; }
        public Builder firstName(String v) { firstName = v; return this; }
        public Builder lastName(String v) { lastName = v; return this; }
        public Builder birthday(LocalDate v) { birthday = v; return this; }
        public Builder employeeStatus(String v) { employeeStatus = v; return this; }
        public Builder position(String v) { position = v; return this; }
        public Builder department(String v) { department = v; return this; }
        public Builder phone(String v) { phone = v; return this; }
        public Builder street(String v) { street = v; return this; }
        public Builder city(String v) { city = v; return this; }
        public Builder region(String v) { region = v; return this; }
        public Builder postalCode(String v) { postalCode = v; return this; }
        public Builder sssNumber(String v) { sssNumber = v; return this; }
        public Builder philhealthNumber(String v) { philhealthNumber = v; return this; }
        public Builder tinNumber(String v) { tinNumber = v; return this; }
        public Builder pagibigNumber(String v) { pagibigNumber = v; return this; }
        public Builder monthlyBasic(BigDecimal v) { monthlyBasic = v; return this; }
        public Builder riceSubsidy(BigDecimal v) { riceSubsidy = v; return this; }
        public Builder phoneAllowance(BigDecimal v) { phoneAllowance = v; return this; }
        public Builder clothingAllowance(BigDecimal v) { clothingAllowance = v; return this; }
        public Builder semiMonthlyRate(BigDecimal v) { semiMonthlyRate = v; return this; }
        public Builder hourlyRate(BigDecimal v) { hourlyRate = v; return this; }
        public Builder supervisorEid(String v) { supervisorEid = v; return this; }
        public Builder branchName(String v) { branchName = v; return this; }
        public Employee build() { return new Employee(this); }
    }

    public Builder toBuilder() {
        return new Builder().eid(eid).firstName(firstName).lastName(lastName).birthday(birthday)
            .employeeStatus(employeeStatus).position(position).department(department)
            .phone(phone).street(street).city(city).region(region).postalCode(postalCode)
            .sssNumber(sssNumber).philhealthNumber(philhealthNumber).tinNumber(tinNumber).pagibigNumber(pagibigNumber)
            .monthlyBasic(monthlyBasic).riceSubsidy(riceSubsidy).phoneAllowance(phoneAllowance)
            .clothingAllowance(clothingAllowance).semiMonthlyRate(semiMonthlyRate).hourlyRate(hourlyRate)
            .supervisorEid(supervisorEid).branchName(branchName);
    }
}
