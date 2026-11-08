package motorph.deduction;

import java.math.BigDecimal;

/**
 * Parent class for every government deduction.
 * <p>
 * PayrollProcessor only needs to "compute a deduction", not know each formula.
 * Each subclass keeps its own rates, caps, and tables as private constants and
 * overrides computeDeduction(). Amounts are returned at full precision; they
 * are rounded to 2 decimals (HALF_UP) only when displayed or saved.
 * </p>
 */
public abstract class Deduction {

    private final String name;

    protected Deduction(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Deduction name is required.");
        }
        this.name = name.trim();
    }

    /** Returns the deduction name shown on the payslip, for example "SSS". */
    public String getName() {
        return name;
    }

    /** Rejects a missing or negative base amount. Shared by all subclasses. */
    protected void validateBaseAmount(BigDecimal baseAmount) {
        if (baseAmount == null) {
            throw new IllegalArgumentException(name + " base amount is required.");
        }
        if (baseAmount.signum() < 0) {
            throw new IllegalArgumentException(name + " base amount must not be negative.");
        }
    }

    /**
     * Computes this deduction for the given base amount.
     *
     * @param baseAmount monthly gross for contributions; taxable income for withholding tax
     * @return the deduction amount
     */
    public abstract BigDecimal computeDeduction(BigDecimal baseAmount);
}
