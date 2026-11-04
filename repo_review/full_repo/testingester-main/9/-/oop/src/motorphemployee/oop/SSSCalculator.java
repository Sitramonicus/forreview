package motorphemployee.oop;

/**
 * SSSCalculator - SSS contribution using the official 44-bracket table, capped at PHP 1,125
 * (MS1 Week 3 CRC: "SSSCalculator (Concrete Subclass)").
 *
 * The bracket table is private: outside code only sees calculate(). Brackets come from
 * Home_SSS_Matrix.xlsx (sheet "SSS Contribution Schedule"): lower bound 3,250 stepping by 500,
 * contribution 157.50 stepping by 22.50, "below 3,250" = 135.00, "over 24,750" = 1,125.00.
 *
 * Official matrix (2026-10-27): the official ranges are LOWER-bound inclusive ("3,250 - 3,749.99"),
 * so bracket i covers [lower, lower + 499.99]. CP2 used an upper-bound inclusive `<=` chain,
 * which under-deducted PHP 22.50 whenever the salary landed exactly on a bracket boundary -
 * Issue 5 in oop/matrix-verification.md.
 */
public class SSSCalculator extends GovernmentContributionCalculator {

    private static final double FIRST_LOWER_BOUND = 3250.0;
    private static final double LOWER_BOUND_STEP = 500.0;
    private static final double BELOW_TABLE_AMOUNT = 135.0;
    private static final double FIRST_BRACKET_AMOUNT = 157.5;
    private static final double AMOUNT_STEP = 22.5;
    private static final int BRACKETS = 43;              // 3,250 ... 24,250 + the "over 24,750" row
    private static final double MAX_CONTRIBUTION = 1125.0;

    private static final double[] LOWER_BOUNDS = new double[BRACKETS];
    private static final double[] AMOUNTS = new double[BRACKETS];

    static {
        for (int i = 0; i < BRACKETS; i++) {
            LOWER_BOUNDS[i] = FIRST_LOWER_BOUND + LOWER_BOUND_STEP * i;
            AMOUNTS[i] = FIRST_BRACKET_AMOUNT + AMOUNT_STEP * i;
        }
    }

    public SSSCalculator() {
        super("SSS Contribution", "SSS");
    }

    @Override
    public double calculate(double grossSalary) {
        double salary = requireValidSalary(grossSalary);
        if (salary < FIRST_LOWER_BOUND) {
            return BELOW_TABLE_AMOUNT;
        }
        for (int i = 0; i < BRACKETS; i++) {
            if (salary < LOWER_BOUNDS[i] + LOWER_BOUND_STEP) {
                return AMOUNTS[i];
            }
        }
        return MAX_CONTRIBUTION;
    }

    /** Read-only inspection of the table (used by the console test, never by payroll logic). */
    public int getBracketCount() {
        return BRACKETS + 2;                             // "below 3,250" + 43 rows + "over 24,750"
    }
}
