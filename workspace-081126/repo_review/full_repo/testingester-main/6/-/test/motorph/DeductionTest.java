package motorph;

import static motorph.TestSupport.*;

import java.math.BigDecimal;
import java.util.List;
import motorph.deduction.Deduction;
import motorph.deduction.PagIbigDeduction;
import motorph.deduction.PhilHealthDeduction;
import motorph.deduction.SSSDeduction;
import motorph.deduction.WithholdingTaxDeduction;

/** Tests each Deduction subclass on its own, at the edges of each bracket. */
public class DeductionTest {

    public static void main(String[] args) {
        run();
        finish();
    }

    private static BigDecimal amt(String value) {
        return new BigDecimal(value);
    }

    static void run() {
        Deduction sss = new SSSDeduction();
        Deduction philHealth = new PhilHealthDeduction();
        Deduction pagIbig = new PagIbigDeduction();
        Deduction tax = new WithholdingTaxDeduction();

        section("SSSDeduction (range table)");
        checkAmount("135.00", sss.computeDeduction(amt("0")), "0 gross -> 135 (lowest bracket)");
        checkAmount("135.00", sss.computeDeduction(amt("3249.99")), "3,249.99 -> 135");
        checkAmount("157.50", sss.computeDeduction(amt("3250")), "3,250 -> 157.50 (threshold is exclusive)");
        checkAmount("1102.50", sss.computeDeduction(amt("24749.99")), "24,749.99 -> 1,102.50");
        checkAmount("1125.00", sss.computeDeduction(amt("24750")), "24,750 -> 1,125 (maximum)");
        checkAmount("1125.00", sss.computeDeduction(amt("100000")), "100,000 -> 1,125");

        section("PhilHealthDeduction (3% / 2, min 150, max 900)");
        checkAmount("150.00", philHealth.computeDeduction(amt("5000")), "5,000 -> 75, raised to minimum 150");
        checkAmount("300.00", philHealth.computeDeduction(amt("20000")), "20,000 -> 300");
        checkAmount("900.00", philHealth.computeDeduction(amt("60000")), "60,000 -> 900");
        checkAmount("900.00", philHealth.computeDeduction(amt("100000")), "100,000 -> 1,500, capped at 900");

        section("PagIbigDeduction (1% up to 1,500, else 2%, max 100)");
        checkAmount("15.00", pagIbig.computeDeduction(amt("1500")), "1,500 -> 1% = 15");
        checkAmount("30.00", pagIbig.computeDeduction(amt("1500.01")), "1,500.01 -> 2% = 30.00");
        checkAmount("100.00", pagIbig.computeDeduction(amt("5000")), "5,000 -> 2% = 100");
        checkAmount("100.00", pagIbig.computeDeduction(amt("10000")), "10,000 -> 200, capped at 100");

        section("WithholdingTaxDeduction (base = taxable income)");
        checkAmount("0.00", tax.computeDeduction(amt("20832")), "20,832 -> 0");
        checkAmount("833.40", tax.computeDeduction(amt("25000")), "25,000 -> (25,000 - 20,833) x 20%");
        checkAmount("2500.00", tax.computeDeduction(amt("33333")), "33,333 -> 2,500");
        checkAmount("6666.75", tax.computeDeduction(amt("50000")), "50,000 -> 2,500 + 16,667 x 25%");
        checkAmount("10833.00", tax.computeDeduction(amt("66667")), "66,667 -> 10,833");
        checkAmount("40833.33", tax.computeDeduction(amt("166667")), "166,667 -> 40,833.33");
        checkAmount("51499.89", tax.computeDeduction(amt("200000")), "200,000 -> 40,833.33 + 33,333 x 32%");
        checkAmount("212499.88", tax.computeDeduction(amt("700000")), "700,000 -> 200,833.33 + 33,333 x 35%");

        section("Deduction.validateBaseAmount (shared)");
        checkRejects(() -> sss.computeDeduction(amt("-1")), "SSS rejects a negative base");
        checkRejects(() -> philHealth.computeDeduction(null), "PhilHealth rejects a missing base");
        checkRejects(() -> tax.computeDeduction(amt("-0.01")), "tax rejects a negative base");

        section("Polymorphism + order: contributions first, then tax on taxable income");
        BigDecimal monthlyGross = amt("50000");
        BigDecimal taxable = monthlyGross;
        for (Deduction contribution : List.of(sss, philHealth, pagIbig)) {
            taxable = taxable.subtract(contribution.computeDeduction(monthlyGross));
        }
        checkAmount("48025.00", taxable, "taxable = 50,000 - 1,125 - 750 - 100");
        checkAmount("6173.00", tax.computeDeduction(taxable), "tax on taxable income = 6,173.00");
        check(tax.computeDeduction(taxable).compareTo(tax.computeDeduction(monthlyGross)) < 0,
                "tax on taxable income (6,173.00) is less than tax on gross (6,666.75)");
    }
}
