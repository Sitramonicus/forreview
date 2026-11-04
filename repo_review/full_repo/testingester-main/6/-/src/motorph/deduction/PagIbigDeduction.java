package motorph.deduction;

import java.math.BigDecimal;

/**
 * Pag-IBIG contribution: 1% if monthly gross is 1,500 or less, otherwise 2%,
 * capped at 100 (CP2 rules).
 */
public class PagIbigDeduction extends Deduction {

    private static final BigDecimal LOW_RATE      = new BigDecimal("0.01");
    private static final BigDecimal HIGH_RATE     = new BigDecimal("0.02");
    private static final BigDecimal LOW_THRESHOLD = new BigDecimal("1500");
    private static final BigDecimal MAXIMUM       = new BigDecimal("100");

    public PagIbigDeduction() {
        super("Pag-IBIG");
    }

    /** @param baseAmount combined monthly gross */
    @Override
    public BigDecimal computeDeduction(BigDecimal baseAmount) {
        validateBaseAmount(baseAmount);
        BigDecimal rate = baseAmount.compareTo(LOW_THRESHOLD) <= 0 ? LOW_RATE : HIGH_RATE;
        return baseAmount.multiply(rate).min(MAXIMUM);
    }
}
