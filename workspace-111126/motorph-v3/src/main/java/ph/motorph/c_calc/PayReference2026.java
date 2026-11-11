package ph.motorph.c_calc;

import java.time.YearMonth;

public final class PayReference2026 {
    private PayReference2026() { }

    public static StatutoryBreakdown calculate(YearMonth month, long monthlyBasicCents, long taxableBonusCents) {
        if (monthlyBasicCents <= 0 || taxableBonusCents < 0)
            throw new IllegalArgumentException("The reference needs a positive monthly basic and non-negative bonus.");
        long taxable = Math.addExact(monthlyBasicCents, taxableBonusCents);
        return StatutoryCalculator2026.calculate(month, taxable, monthlyBasicCents, monthlyBasicCents,
            monthlyBasicCents, taxable, StatutoryRates.DEFAULT);
    }
}
