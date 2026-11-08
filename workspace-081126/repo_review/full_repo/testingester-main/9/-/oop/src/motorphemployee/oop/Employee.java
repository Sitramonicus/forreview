package motorphemployee.oop;

/**
 * Employee - abstract base class for all employee types (MS1 Week 3/4 CRC rows).
 *
 * Inherits the personal fields from Person. Adds employment data. Every salary and
 * allowance field is private and can only be changed through a validated setter,
 * which is the encapsulation contract from MS1 Week 2 (the CP2 class had 19 setters
 * and zero validation - see Employee.java:186 in the CP2 baseline).
 *
 * The four benefit methods are abstract: only the concrete employee type knows the
 * amount, so callers never need an if/else on employee type.
 */
public abstract class Employee extends Person {

    private int id;
    private final String sssNumber;
    private final String philhealthNumber;
    private final String tinNumber;
    private final String pagIbigNumber;

    private String status;
    private String position;
    private String immediateSupervisor;

    private double basicSalary;
    private double grossSemiMonthlyRate;
    private double hourlyRate;
    private double clothingAllowance;

    /**
     * MS1 Method Dictionary signature kept verbatim: 19 fields from data.csv.
     *
     * Design note: riceSubsidy and phoneAllowance are still accepted here because the
     * approved Method Dictionary declares this exact parameter list, but the values are
     * superseded by the polymorphic getRiceSubsidy()/getPhoneAllowance() overrides that
     * the Week 3 abstraction design introduced (MS1 Week 3 CRC: Employee Abstract Base
     * Class). They are validated and then deliberately not used as the source of truth.
     */
    public Employee(int id,
                    String lastName,
                    String firstName,
                    String birthday,
                    String address,
                    String phoneNumber,
                    String sssNumber,
                    String philhealthNumber,
                    String tinNumber,
                    String pagIbigNumber,
                    String status,
                    String position,
                    String immediateSupervisor,
                    double basicSalary,
                    double riceSubsidy,
                    double phoneAllowance,
                    double clothingAllowance,
                    double grossSemiMonthlyRate,
                    double hourlyRate) {
        super(firstName, lastName, birthday, address, phoneNumber);

        this.id = requirePositiveId(id);
        this.sssNumber = requireIdentifier(sssNumber, "sssNumber");
        this.philhealthNumber = requireIdentifier(philhealthNumber, "philhealthNumber");
        this.tinNumber = requireIdentifier(tinNumber, "tinNumber");
        this.pagIbigNumber = requireIdentifier(pagIbigNumber, "pagIbigNumber");

        this.status = requireText(status, "status");
        this.position = requireText(position, "position");
        this.immediateSupervisor = requireText(immediateSupervisor, "immediateSupervisor");

        this.basicSalary = requirePositive(basicSalary, "basicSalary");
        this.clothingAllowance = requireNonNegative(clothingAllowance, "clothingAllowance");
        this.grossSemiMonthlyRate = requirePositive(grossSemiMonthlyRate, "grossSemiMonthlyRate");
        this.hourlyRate = requirePositive(hourlyRate, "hourlyRate");

        // validated, then not used - see the design note above
        requireNonNegative(riceSubsidy, "riceSubsidy");
        requireNonNegative(phoneAllowance, "phoneAllowance");
    }

    // ---- abstract contract: the specialization point of every subclass ----

    public abstract double getRiceSubsidy();

    public abstract double getPhoneAllowance();

    public abstract int getAnnualLeaveCredits();

    public abstract boolean isEligibleForPromotion();

    // ---- shared accessors (validated where the data can be corrupted) ----

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = requirePositiveId(id);
    }

    // Government IDs are read-only after creation: getters only, no setters (MS1 Week 2 CRC).
    public String getSssNumber() {
        return sssNumber;
    }

    public String getPhilhealthNumber() {
        return philhealthNumber;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public String getPagIbigNumber() {
        return pagIbigNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = requireText(status, "status");
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = requireText(position, "position");
    }

    public String getImmediateSupervisor() {
        return immediateSupervisor;
    }

    public void setImmediateSupervisor(String immediateSupervisor) {
        this.immediateSupervisor = requireText(immediateSupervisor, "immediateSupervisor");
    }

    public double getBasicSalary() {
        return basicSalary;
    }

    /** Rejects zero or negative salary - CP2 accepted setBasicSalary(-5000) (Employee.java:186). */
    public void setBasicSalary(double basicSalary) {
        this.basicSalary = requirePositive(basicSalary, "basicSalary");
    }

    public double getClothingAllowance() {
        return clothingAllowance;
    }

    public void setClothingAllowance(double clothingAllowance) {
        this.clothingAllowance = requireNonNegative(clothingAllowance, "clothingAllowance");
    }

    public double getGrossSemiMonthlyRate() {
        return grossSemiMonthlyRate;
    }

    public void setGrossSemiMonthlyRate(double grossSemiMonthlyRate) {
        this.grossSemiMonthlyRate = requirePositive(grossSemiMonthlyRate, "grossSemiMonthlyRate");
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    /** Rejects zero or negative rate - CP2 accepted setHourlyRate(0) (Employee.java:206). */
    public void setHourlyRate(double hourlyRate) {
        this.hourlyRate = requirePositive(hourlyRate, "hourlyRate");
    }

    @Override
    public String toString() {
        return "[" + getClass().getSimpleName() + " #" + id + "] " + getFullName()
                + " - " + position + " (" + status + ")";
    }

    // ---- validation helpers ----

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be null or empty");
        }
        return value.trim();
    }

    private static String requireIdentifier(String value, String field) {
        String text = requireText(value, field);
        if (text.length() < 3) {
            throw new IllegalArgumentException(field + " looks invalid: " + value);
        }
        return text;
    }

    private static double requireNonNegative(double value, String field) {
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException(field + " cannot be negative: " + value);
        }
        return value;
    }

    private static double requirePositive(double value, String field) {
        if (Double.isNaN(value) || !(value > 0)) {
            throw new IllegalArgumentException(field + " must be greater than zero: " + value);
        }
        return value;
    }

    private static int requirePositiveId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("employee id must be positive: " + id);
        }
        return id;
    }
}
