package ph.motorph.c_calc;

import java.time.YearMonth;

public final class PayReference2026Test {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        YearMonth month = YearMonth.of(2026, 1);
        StatutoryBreakdown middle = PayReference2026.calculate(month, 3_000_000, 0);
        check(middle.sssEmployeeCents == 150_000, "SSS for 30,000");
        check(middle.philHealthEmployeeCents == 75_000, "PhilHealth for 30,000");
        check(middle.pagIbigEmployeeCents == 20_000, "Pag-IBIG cap");
        check(middle.withholdingTaxCents == 100_750, "annualised withholding");
        StatutoryBreakdown bonus = PayReference2026.calculate(month, 3_000_000, 100_000);
        check(bonus.sssEmployeeCents == middle.sssEmployeeCents && bonus.withholdingTaxCents > middle.withholdingTaxCents,
            "taxable bonus raises tax without changing the contribution base");
        StatutoryBreakdown low = PayReference2026.calculate(month, 150_000, 0);
        check(low.pagIbigEmployeeCents == 1_500, "low-band boundary");
        StatutoryBreakdown high = PayReference2026.calculate(month, 15_000_000, 0);
        check(high.sssEmployeeCents == 175_000 && high.philHealthEmployeeCents == 250_000,
            "contribution ceilings");
        boolean rejected = false;
        try { PayReference2026.calculate(month, 0, 0); } catch (IllegalArgumentException ex) { rejected = true; }
        check(rejected, "zero basic is unavailable");
        rejected = false;
        try { PayReference2026.calculate(YearMonth.of(2027, 1), 3_000_000, 0); } catch (IllegalArgumentException ex) { rejected = true; }
        check(rejected, "unsupported year is unavailable");
        System.out.println("PayReference2026Test: 9 checks passed");
    }
}
