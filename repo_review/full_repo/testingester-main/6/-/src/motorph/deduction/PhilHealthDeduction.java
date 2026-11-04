package motorph.deduction;

import java.math.BigDecimal;

/**
 * PhilHealth employee share: 3% of monthly gross, split 50/50 with the
 * employer, so the employee pays 1.5%. Minimum 150, maximum 900 (CP2 rules).
 */
public class PhilHealthDeduction extends Deduction {

    private static final BigDecimal PREMIUM_RATE   = new BigDecimal("0.03");
    private static final BigDecimal EMPLOYEE_SHARE = new BigDecimal("2");
    private static final BigDecimal MINIMUM        = new BigDecimal("150");
    private static final BigDecimal MAXIMUM        = new BigDecimal("900");

    public PhilHealthDeduction() {
        super("PhilHealth");
    }

    /** @param baseAmount combined monthly gross */
    @Override
    public BigDecimal computeDeduction(BigDecimal baseAmount) {
        validateBaseAmount(baseAmount);
        BigDecimal share = baseAmount.multiply(PREMIUM_RATE).divide(EMPLOYEE_SHARE);
        return share.max(MINIMUM).min(MAXIMUM);
    }
}
