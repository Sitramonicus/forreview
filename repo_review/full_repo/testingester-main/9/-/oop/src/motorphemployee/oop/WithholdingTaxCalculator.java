package motorphemployee.oop;

/**
 * WithholdingTaxCalculator - BIR progressive withholding tax, 6 brackets up to 35%
 * (MS1 Week 3 CRC: "WithholdingTaxCalculator (Concrete Subclass)").
 *
 * Applied AFTER SSS/PhilHealth/Pag-IBIG, so the input is the salary already net of those
 * three contributions - same as CP2 MainMenu.java:1418 / Dashboard.java:1334.
 *
 * Official matrix (2026-10-27): bracket 1 uses the official excess base of 20,833, not CP2's
 * 20,832 (Home_WithholdingTax_Matrix.xlsx bracket text B3 and SAMPLE B20: taxable 23,400 ->
 * 513.40). CP2 over-withheld PHP 0.20 for every taxable income in [20,833, 33,333] - Issue 4.
 * Brackets 2-6 were already exact and are unchanged.
 */
public class WithholdingTaxCalculator extends GovernmentContributionCalculator {

    /** BIR brackets: upper bound of the bracket, base tax, excess over the previous bound, marginal rate. */
    private static final double[] UPPER_BOUNDS = {20832.0, 33333.0, 66667.0, 166667.0, 666667.0};
    private static final double[] BASE_TAX = {0.0, 0.0, 2500.0, 10833.0, 40833.33};
    private static final double[] LOWER_BOUNDS = {0.0, 20833.0, 33333.0, 66667.0, 166667.0};
    private static final double[] MARGINAL_RATES = {0.0, 0.20, 0.25, 0.30, 0.32};
    private static final double TOP_LOWER_BOUND = 666667.0;
    private static final double TOP_BASE_TAX = 200833.33;
    private static final double TOP_MARGINAL_RATE = 0.35;

    public WithholdingTaxCalculator() {
        super("Withholding Tax", "TAX");
    }

    @Override
    public double calculate(double taxableIncome) {
        double income = requireValidSalary(taxableIncome);

        for (int i = 0; i < UPPER_BOUNDS.length; i++) {
            if (income <= UPPER_BOUNDS[i]) {
                // max(0, ...) guards the 20,832.01-20,832.99 gap between the exempt ceiling and
                // the official 20,833 excess base, so the tax can never come out negative.
                return Math.max(0.0, BASE_TAX[i] + (income - LOWER_BOUNDS[i]) * MARGINAL_RATES[i]);
            }
        }
        return TOP_BASE_TAX + (income - TOP_LOWER_BOUND) * TOP_MARGINAL_RATE;
    }
}
