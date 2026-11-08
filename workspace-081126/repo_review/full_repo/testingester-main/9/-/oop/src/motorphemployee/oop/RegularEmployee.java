package motorphemployee.oop;

/**
 * RegularEmployee - concrete subclass for permanent staff (MS1 Week 3/4/5 CRC rows).
 *
 * Only implements what is unique to this type. Everything about being a person or an
 * employee is inherited from Person/Employee.
 */
public class RegularEmployee extends Employee {

    private int yearsOfService;

    /** Same 19 fields as Employee + yearsOfService (supervisor is inherited, not re-declared). */
    public RegularEmployee(int id,
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
                           double hourlyRate,
                           int yearsOfService) {
        super(id, lastName, firstName, birthday, address, phoneNumber, sssNumber, philhealthNumber,
                tinNumber, pagIbigNumber, status, position, immediateSupervisor, basicSalary,
                riceSubsidy, phoneAllowance, clothingAllowance, grossSemiMonthlyRate, hourlyRate);
        this.yearsOfService = requireYears(yearsOfService);
    }

    public int getYearsOfService() {
        return yearsOfService;
    }

    public void setYearsOfService(int yearsOfService) {
        this.yearsOfService = requireYears(yearsOfService);
    }

    private static int requireYears(int yearsOfService) {
        if (yearsOfService < 0 || yearsOfService > 60) {
            throw new IllegalArgumentException("yearsOfService out of range: " + yearsOfService);
        }
        return yearsOfService;
    }

    /** Full rice subsidy for permanent staff. */
    @Override
    public double getRiceSubsidy() {
        return 1500.0;
    }

    /** Full phone allowance for permanent staff. */
    @Override
    public double getPhoneAllowance() {
        return 2000.0;
    }

    /** Full annual leave credits. */
    @Override
    public int getAnnualLeaveCredits() {
        return 12;
    }

    /** Promotion rule for permanent staff: at least 2 years of service. */
    @Override
    public boolean isEligibleForPromotion() {
        return yearsOfService >= 2;
    }
}
