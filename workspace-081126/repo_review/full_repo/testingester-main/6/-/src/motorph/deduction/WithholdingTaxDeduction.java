package motorph.deduction;

import java.math.BigDecimal;

/**
 * Withholding tax from the BIR monthly brackets used in CP2.
 * <p>
 * Unlike the other deductions, the base amount is <b>taxable income</b>:
 * monthly gross minus SSS, PhilHealth, and Pag-IBIG. PayrollProcessor computes
 * those contributions first, then passes the taxable income here.
 * </p>
 */
public class WithholdingTaxDeduction extends Deduction {

    // Upper limit of each bracket (inclusive)
    private static final BigDecimal BRACKET_1_MAX = new BigDecimal("20832");
    private static final BigDecimal BRACKET_2_MAX = new BigDecimal("33332");
    private static final BigDecimal BRACKET_3_MAX = new BigDecimal("66666");
    private static final BigDecimal BRACKET_4_MAX = new BigDecimal("166666");
    private static final BigDecimal BRACKET_5_MAX = new BigDecimal("666666");

    // Amount over which the bracket rate applies
    private static final BigDecimal BRACKET_2_FLOOR = new BigDecimal("20833");
    private static final BigDecimal BRACKET_3_FLOOR = new BigDecimal("33333");
    private static final BigDecimal BRACKET_4_FLOOR = new BigDecimal("66667");
    private static final BigDecimal BRACKET_5_FLOOR = new BigDecimal("166667");
    private static final BigDecimal BRACKET_6_FLOOR = new BigDecimal("666667");

    // Fixed tax at the start of each bracket
    private static final BigDecimal BRACKET_3_BASE_TAX = new BigDecimal("2500");
    private static final BigDecimal BRACKET_4_BASE_TAX = new BigDecimal("10833");
    private static final BigDecimal BRACKET_5_BASE_TAX = new BigDecimal("40833.33");
    private static final BigDecimal BRACKET_6_BASE_TAX = new BigDecimal("200833.33");

    // Rate on the amount over the floor
    private static final BigDecimal BRACKET_2_RATE = new BigDecimal("0.20");
    private static final BigDecimal BRACKET_3_RATE = new BigDecimal("0.25");
    private static final BigDecimal BRACKET_4_RATE = new BigDecimal("0.30");
    private static final BigDecimal BRACKET_5_RATE = new BigDecimal("0.32");
    private static final BigDecimal BRACKET_6_RATE = new BigDecimal("0.35");

    public WithholdingTaxDeduction() {
        super("Withholding Tax");
    }

    /** @param baseAmount taxable income (monthly gross minus SSS, PhilHealth, Pag-IBIG) */
    @Override
    public BigDecimal computeDeduction(BigDecimal baseAmount) {
        validateBaseAmount(baseAmount);
        if (baseAmount.compareTo(BRACKET_1_MAX) <= 0) {
            return BigDecimal.ZERO;
        }
        if (baseAmount.compareTo(BRACKET_2_MAX) <= 0) {
            return baseAmount.subtract(BRACKET_2_FLOOR).multiply(BRACKET_2_RATE);
        }
        if (baseAmount.compareTo(BRACKET_3_MAX) <= 0) {
            return BRACKET_3_BASE_TAX.add(baseAmount.subtract(BRACKET_3_FLOOR).multiply(BRACKET_3_RATE));
        }
        if (baseAmount.compareTo(BRACKET_4_MAX) <= 0) {
            return BRACKET_4_BASE_TAX.add(baseAmount.subtract(BRACKET_4_FLOOR).multiply(BRACKET_4_RATE));
        }
        if (baseAmount.compareTo(BRACKET_5_MAX) <= 0) {
            return BRACKET_5_BASE_TAX.add(baseAmount.subtract(BRACKET_5_FLOOR).multiply(BRACKET_5_RATE));
        }
        return BRACKET_6_BASE_TAX.add(baseAmount.subtract(BRACKET_6_FLOOR).multiply(BRACKET_6_RATE));
    }
}
