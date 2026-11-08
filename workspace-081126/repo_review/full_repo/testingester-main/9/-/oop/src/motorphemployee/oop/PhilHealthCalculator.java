package motorphemployee.oop;

/**
 * PhilHealthCalculator - PhilHealth employee share: 50% of the premium (3% rate with a PHP 300
 * floor and a PHP 1,800 ceiling), so the deduction runs from PHP 150 to PHP 900
 * (MS1 Week 3 CRC: "PhilHealthCalculator (Concrete Subclass)").
 *
 * Official matrix (2026-10-27): the premium formula is carried over from CP2 unchanged, but the
 * EMPLOYEE SHARE is applied. The official matrix states the premium is equally shared between
 * employee and employer (Home_Philhealth_Matrix.xlsx note A6; SAMPLE B10 = 375 on a 25,000
 * basic salary). CP2 returned the full premium, which over-deducted every payslip by 2x -
 * logged as Issue 2 in oop/matrix-verification.md.
 */
public class PhilHealthCalculator extends GovernmentContributionCalculator {

    private static final double EMPLOYEE_SHARE_RATE = 0.5;
    private static final double PREMIUM_RATE = 0.03;
    private static final double MIN_PREMIUM = 300.0;
    private static final double MAX_PREMIUM = 1800.0;
    private static final double LOWER_CAP_SALARY = 10000.0;
    private static final double UPPER_CAP_SALARY = 59999.99;

    public PhilHealthCalculator() {
        super("PhilHealth Contribution", "PH");
    }

    @Override
    public double calculate(double grossSalary) {
        double salary = requireValidSalary(grossSalary);

        // Official monthly premium: 3% of salary, floor 300, ceiling 1,800 (matrix A2:C4).
        double monthlyPremium;
        if (salary <= LOWER_CAP_SALARY) {
            monthlyPremium = MIN_PREMIUM;
        } else if (salary <= UPPER_CAP_SALARY) {
            monthlyPremium = Math.min(MIN_PREMIUM + (salary - LOWER_CAP_SALARY) * PREMIUM_RATE, MAX_PREMIUM);
        } else {
            monthlyPremium = MAX_PREMIUM;
        }

        // The employee pays HALF (matrix note A6: "equally shared"; SAMPLE B10 = 375 of 750).
        return monthlyPremium * EMPLOYEE_SHARE_RATE;
    }
}
