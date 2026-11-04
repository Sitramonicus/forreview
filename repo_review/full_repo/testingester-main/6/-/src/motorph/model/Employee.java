package motorph.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;

/**
 * Parent class for every MotorPH employee.
 * <p>
 * Holds the 19 fields of employees.csv as private fields. Values can only be
 * read through getters and changed through validated setters, so an Employee
 * object is never in an invalid state. The shared pay rule lives here
 * (computeGrossPay); allowance eligibility and the employment type label are
 * left to each subclass.
 * </p>
 */
public abstract class Employee {

    // === STATUS VALUES (the only two allowed in employees.csv) ===
    public static final String STATUS_REGULAR      = "Regular";
    public static final String STATUS_PROBATIONARY = "Probationary";

    // === VALIDATION PATTERNS ===
    private static final String ID_PATTERN         = "\\d+";
    private static final String PHONE_PATTERN      = "\\d{3}-\\d{3}-\\d{3}";
    private static final String SSS_PATTERN        = "\\d{2}-\\d{7}-\\d";
    private static final String PHILHEALTH_PATTERN = "\\d{12}";
    private static final String TIN_PATTERN        = "\\d{3}-\\d{3}-\\d{3}-\\d{3}";
    private static final String PAGIBIG_PATTERN    = "\\d{12}";
    private static final String HAS_DIGIT_PATTERN  = ".*\\d.*";

    private static final BigDecimal MINUTES_PER_HOUR = new BigDecimal("60");

    // === IDENTITY ===
    private final String employeeId;
    private String lastName;
    private String firstName;
    private LocalDate birthday;
    private String address;
    private String phoneNumber;

    // === GOVERNMENT IDS ===
    private String sssNumber;
    private String philHealthNumber;
    private String tin;
    private String pagIbigNumber;

    // === EMPLOYMENT ===
    private String status;
    private String position;
    private String immediateSupervisor;

    // === COMPENSATION ===
    private BigDecimal basicSalary;
    private BigDecimal riceSubsidy;
    private BigDecimal phoneAllowance;
    private BigDecimal clothingAllowance;
    private BigDecimal grossSemiMonthlyRate;
    private BigDecimal hourlyRate;

    /**
     * Creates an employee with all 19 fields. Every value goes through its
     * validated setter, so invalid input throws IllegalArgumentException.
     * The employee ID has no setter: it is fixed here and never changes.
     */
    protected Employee(String employeeId, String lastName, String firstName, LocalDate birthday,
                       String address, String phoneNumber, String sssNumber, String philHealthNumber,
                       String tin, String pagIbigNumber, String status, String position,
                       String immediateSupervisor, BigDecimal basicSalary, BigDecimal riceSubsidy,
                       BigDecimal phoneAllowance, BigDecimal clothingAllowance,
                       BigDecimal grossSemiMonthlyRate, BigDecimal hourlyRate) {
        this.employeeId = validateEmployeeId(employeeId);
        setLastName(lastName);
        setFirstName(firstName);
        setBirthday(birthday);
        setAddress(address);
        setPhoneNumber(phoneNumber);
        setSssNumber(sssNumber);
        setPhilHealthNumber(philHealthNumber);
        setTin(tin);
        setPagIbigNumber(pagIbigNumber);
        setStatus(status);
        setPosition(position);
        setImmediateSupervisor(immediateSupervisor);
        setBasicSalary(basicSalary);
        setRiceSubsidy(riceSubsidy);
        setPhoneAllowance(phoneAllowance);
        setClothingAllowance(clothingAllowance);
        setGrossSemiMonthlyRate(grossSemiMonthlyRate);
        setHourlyRate(hourlyRate);
    }

    // === SHARED BEHAVIOR ===

    /**
     * Computes gross pay for the given hours: hoursWorked x hourlyRate.
     * <p>
     * Attendance is recorded to the minute, so the hours are turned back into
     * whole minutes and divided by 60 last. Multiplying a cut-off decimal such as
     * 74.8333 hours would lose the exact half-centavo (535.71 x 4,490 min / 60 =
     * 40,088.965). The result keeps full precision; it is rounded to 2 decimals
     * (HALF_UP) only when displayed or saved.
     * </p>
     *
     * @param hoursWorked hours worked in the period; must not be negative
     * @return gross pay at full precision
     */
    public final BigDecimal computeGrossPay(double hoursWorked) {
        if (Double.isNaN(hoursWorked) || Double.isInfinite(hoursWorked) || hoursWorked < 0) {
            throw new IllegalArgumentException("Hours worked must not be negative.");
        }
        long minutesWorked = Math.round(hoursWorked * 60);
        return hourlyRate.multiply(BigDecimal.valueOf(minutesWorked))
                .divide(MINUTES_PER_HOUR, MathContext.DECIMAL128);
    }

    /**
     * Returns the employee's name as "First Last", the format CP2 showed in
     * the payroll table.
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    // === SUBCLASS CONTRACT ===

    /**
     * Returns the monthly allowances this employee is entitled to. Shown on the
     * payslip; not added to net pay (matches CP2).
     */
    public abstract BigDecimal computeAllowances();

    /** Returns the employment type label, "Regular" or "Probationary". */
    public abstract String getEmploymentType();

    // === GETTERS ===

    public String getEmployeeId()           { return employeeId; }
    public String getLastName()             { return lastName; }
    public String getFirstName()            { return firstName; }
    public LocalDate getBirthday()          { return birthday; }
    public String getAddress()              { return address; }
    public String getPhoneNumber()          { return phoneNumber; }
    public String getSssNumber()            { return sssNumber; }
    public String getPhilHealthNumber()     { return philHealthNumber; }
    public String getTin()                  { return tin; }
    public String getPagIbigNumber()        { return pagIbigNumber; }
    public String getStatus()               { return status; }
    public String getPosition()             { return position; }
    public String getImmediateSupervisor()  { return immediateSupervisor; }
    public BigDecimal getBasicSalary()      { return basicSalary; }
    public BigDecimal getRiceSubsidy()      { return riceSubsidy; }
    public BigDecimal getPhoneAllowance()   { return phoneAllowance; }
    public BigDecimal getClothingAllowance(){ return clothingAllowance; }
    public BigDecimal getGrossSemiMonthlyRate() { return grossSemiMonthlyRate; }
    public BigDecimal getHourlyRate()       { return hourlyRate; }

    // === VALIDATED SETTERS ===
    // Setters are final so subclasses cannot weaken the validation rules.

    public final void setLastName(String lastName) {
        this.lastName = requireNameText(lastName, "Last Name");
    }

    public final void setFirstName(String firstName) {
        this.firstName = requireNameText(firstName, "First Name");
    }

    public final void setBirthday(LocalDate birthday) {
        if (birthday == null) {
            throw new IllegalArgumentException("Birthday is required.");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday cannot be in the future.");
        }
        this.birthday = birthday;
    }

    public final void setAddress(String address) {
        this.address = requireText(address, "Address");
    }

    public final void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = requireFormat(phoneNumber, PHONE_PATTERN, "Phone Number", "###-###-###");
    }

    public final void setSssNumber(String sssNumber) {
        this.sssNumber = requireFormat(sssNumber, SSS_PATTERN, "SSS Number", "##-#######-#");
    }

    public final void setPhilHealthNumber(String philHealthNumber) {
        this.philHealthNumber = requireFormat(philHealthNumber, PHILHEALTH_PATTERN, "PhilHealth Number", "12 digits");
    }

    public final void setTin(String tin) {
        this.tin = requireFormat(tin, TIN_PATTERN, "TIN", "###-###-###-###");
    }

    public final void setPagIbigNumber(String pagIbigNumber) {
        this.pagIbigNumber = requireFormat(pagIbigNumber, PAGIBIG_PATTERN, "Pag-IBIG Number", "12 digits");
    }

    /**
     * Updates the status value. Only "Regular" or "Probationary" is accepted.
     * A Java object cannot change its class, so a status change on the Edit
     * screen is saved by building a new object through
     * CSVDataHandler.createEmployee(), which picks the matching subclass.
     */
    public final void setStatus(String status) {
        String value = requireText(status, "Status");
        if (!value.equals(STATUS_REGULAR) && !value.equals(STATUS_PROBATIONARY)) {
            throw new IllegalArgumentException("Status must be Regular or Probationary.");
        }
        this.status = value;
    }

    public final void setPosition(String position) {
        this.position = requireNameText(position, "Position");
    }

    public final void setImmediateSupervisor(String immediateSupervisor) {
        this.immediateSupervisor = requireNameText(immediateSupervisor, "Supervisor");
    }

    public final void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = requirePositive(basicSalary, "Basic Salary");
    }

    public final void setRiceSubsidy(BigDecimal riceSubsidy) {
        this.riceSubsidy = requirePositive(riceSubsidy, "Rice Subsidy");
    }

    public final void setPhoneAllowance(BigDecimal phoneAllowance) {
        this.phoneAllowance = requirePositive(phoneAllowance, "Phone Allowance");
    }

    public final void setClothingAllowance(BigDecimal clothingAllowance) {
        this.clothingAllowance = requirePositive(clothingAllowance, "Clothing Allowance");
    }

    public final void setGrossSemiMonthlyRate(BigDecimal grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = requirePositive(grossSemiMonthlyRate, "Gross Semi-Monthly Rate");
    }

    public final void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = requirePositive(hourlyRate, "Hourly Rate");
    }

    // === VALIDATION HELPERS ===

    private static String validateEmployeeId(String employeeId) {
        String value = requireText(employeeId, "Employee ID");
        if (!value.matches(ID_PATTERN) || new BigDecimal(value).signum() <= 0) {
            throw new IllegalArgumentException("Employee ID must be a positive whole number.");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static String requireNameText(String value, String fieldName) {
        String text = requireText(value, fieldName);
        if (text.matches(HAS_DIGIT_PATTERN)) {
            throw new IllegalArgumentException(fieldName + " must not contain numbers.");
        }
        return text;
    }

    private static String requireFormat(String value, String pattern, String fieldName, String formatHint) {
        String text = requireText(value, fieldName);
        if (!text.matches(pattern)) {
            throw new IllegalArgumentException(fieldName + " must use the format " + formatHint + ".");
        }
        return text;
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0.");
        }
        return value;
    }
}
