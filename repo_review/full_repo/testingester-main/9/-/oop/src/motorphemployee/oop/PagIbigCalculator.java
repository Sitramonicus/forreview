package motorphemployee.oop;

/**
 * PagIbigCalculator - Pag-IBIG EMPLOYEE share, tiered, with a PHP 100 cap
 * (MS1 Week 3 CRC: "PagIbigCalculator (Concrete Subclass)").
 *
 * Official matrix (2026-10-27): only the employee share is deducted - 1% for 1,000-1,500 and 2%
 * above 1,500, capped at 100 (Home_PagIBIG_Matrix.xlsx B2/B3 employee rates vs D2/D3 totals,
 * cap note A5). CP2 added the employer share (2%) and deducted the total 3%/4% from the
 * employee; that is Issue 3 in oop/matrix-verification.md.
 *
 * Salary below 1,000 has no row in the official matrix, so it returns 0.00 and stays
 * UNVERIFIED rather than guessing a rate.
 */
public class PagIbigCalculator extends GovernmentContributionCalculator {

    private static final double LOWER_BAND_MIN = 1000.0;
    private static final double LOWER_BAND_MAX = 1500.0;
    private static final double EMPLOYEE_RATE_LOW = 0.01;
    private static final double EMPLOYEE_RATE_HIGH = 0.02;
    private static final double MAX_CONTRIBUTION = 100.0;

    public PagIbigCalculator() {
        super("Pag-IBIG Contribution", "PAGIBIG");
    }

    @Override
    public double calculate(double grossSalary) {
        double salary = requireValidSalary(grossSalary);

        double employeeRate = 0.0;

        if (salary >= LOWER_BAND_MIN && salary <= LOWER_BAND_MAX) {
            employeeRate = EMPLOYEE_RATE_LOW;
        } else if (salary > LOWER_BAND_MAX) {
            employeeRate = EMPLOYEE_RATE_HIGH;
        }

        return Math.min(salary * employeeRate, MAX_CONTRIBUTION);
    }
}
