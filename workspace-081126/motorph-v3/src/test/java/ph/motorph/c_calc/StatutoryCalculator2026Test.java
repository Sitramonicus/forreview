package ph.motorph.c_calc;

import ph.motorph.c_calc.StatutoryBreakdown;
import ph.motorph.c_calc.StatutoryCalculator2026;
import ph.motorph.c_calc.StatutoryRates;
import ph.motorph.o_oop.Employee;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

/** Checks the Java 2026 calculator against sweldongpinoy.com results and the statutory boundaries. */
public final class StatutoryCalculator2026Test {
    private static int checks;
    private static final YearMonth OCT = YearMonth.of(2026, 10);

    public static void main(String[] args) {
        matchesSweldongPinoyResults();
        computesFullBreakdownForTwentyFiveThousand();
        respectsSssPhilHealthAndPagIbigBoundaries();
        annualisesTaxWithoutThirteenthMonth();
        parsesPesosAndRejectsBadInput();
        usesRatesVersions();
        System.out.println("PASS: " + checks + " statutory-calculator checks");
    }

    private static void matchesSweldongPinoyResults() {
        // Copied from sweldongpinoy.com (Private Employee, Monthly, 2026, basic pay only), queried 2026-11-05.
        // SSS | PhilHealth | Pag-IBIG | withholding tax | total deductions (total omitted where not recorded).
        Map<String, String> site = new LinkedHashMap<>();
        site.put("1000.00", "250.00|250.00|10.00|0.00");
        site.put("1499.99", "250.00|250.00|15.00|0.00");
        // mentor 2026-11-05: HDMF Circular 460 says 1% at or below 1,500; the site shows 30.00 here (2%), we follow the circular
        site.put("1500.00", "250.00|250.00|15.00|0.00|515.00");
        site.put("1500.01", "250.00|250.00|30.00|0.00");
        site.put("3597.50", "250.00|250.00|71.95|0.00|571.95");
        site.put("5000.00", "250.00|250.00|100.00|0.00|600.00");
        site.put("9999.99", "500.00|250.00|200.00|0.00|950.00");
        site.put("10000.00", "500.00|250.00|200.00|0.00|950.00");
        site.put("12000.00", "600.00|300.00|200.00|0.00|1100.00");
        site.put("15597.50", "775.00|389.94|200.00|0.00|1364.94");
        site.put("20833.00", "1050.00|520.83|200.00|0.00|1770.83");
        site.put("20833.33", "1050.00|520.83|200.00|0.00|1770.83");
        site.put("25000.00", "1250.00|625.00|200.00|313.75|2388.75");
        site.put("33333.00", "1675.00|833.33|200.00|1468.70|4177.03");
        site.put("34750.00", "1750.00|868.75|200.00|1664.69|4483.44");
        site.put("35000.00", "1750.00|875.00|200.00|1701.25|4526.25");
        site.put("40000.00", "1750.00|1000.00|200.00|2618.33|5568.33");
        site.put("66667.00", "1750.00|1666.68|200.00|7818.40|11435.08");
        site.put("89999.00", "1750.00|2249.98|200.00|13324.76");
        site.put("90000.00", "1750.00|2250.00|200.00|13325.00");
        // Above 90,000 the site adds a taxable 13th month; we do not (mentor 2026-11-05), so tax differs from here down.
        site.put("95000.00", "1750.00|2375.00|200.00|14543.75");
        site.put("100000.00", "1750.00|2500.00|200.00|15762.50|20212.50");
        site.put("120000.00", "1750.00|2500.00|200.00|20762.50|25212.50");
        site.put("200000.00", "1750.00|2500.00|200.00|42206.67|46656.67");
        site.put("700000.00", "1750.00|2500.00|200.00|193650.83|198100.83");
        for (Map.Entry<String, String> row : site.entrySet()) {
            long pay = StatutoryCalculator2026.parsePesoCents(row.getKey(), "pay");
            StatutoryBreakdown r = StatutoryCalculator2026.calculate(OCT, pay, pay, pay, pay, pay);
            String[] ours = {pesos(r.sssEmployeeCents), pesos(r.philHealthEmployeeCents), pesos(r.pagIbigEmployeeCents),
                pesos(r.withholdingTaxCents), pesos(r.totalEmployeeDeductionsCents())};
            String[] expected = row.getValue().split("\\|");
            for (int i = 0; i < expected.length; i++) {
                check(expected[i].equals(ours[i]), "sweldongpinoy " + row.getKey() + " column " + i + ": expected " + expected[i] + " got " + ours[i]);
            }
        }
    }

    private static void computesFullBreakdownForTwentyFiveThousand() {
        StatutoryBreakdown r = StatutoryCalculator2026.calculate(OCT, 2_500_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000);
        check(r.sssMscCents == 2_500_000 && r.sssEmployerRegularCents == 250_000 && r.sssEcCents == 3_000, "SSS employer side for 25,000");
        check(r.philHealthTotalPremiumCents == 125_000 && r.philHealthEmployerCents == 62_500, "PhilHealth employer half");
        check(r.pagIbigBaseCents == 1_000_000 && r.pagIbigEmployerCents == 20_000, "Pag-IBIG employer 2% of capped base");
        check(r.taxableAfterEmployeeContributionsCents == 2_292_500, "taxable after employee shares 22,925.00");
        check(r.netPayCents() == 2_261_125, "net 22,611.25 (sweldongpinoy.com)");
        check(r.totalEmployerContributionsCents() == 335_500 && r.employerCashCostCents() == 2_835_500, "employer totals");
    }

    private static void respectsSssPhilHealthAndPagIbigBoundaries() {
        check(msc("5249.99") == 500_000 && msc("5250.00") == 550_000, "SSS first range boundary");
        check(msc("34749.99") == 3_450_000 && msc("34750.00") == 3_500_000 && msc("90000.00") == 3_500_000, "SSS MSC cap");
        check(sss("14500.00").sssEcCents == 1_000 && sss("15000.00").sssEcCents == 3_000, "SSS EC threshold");
        StatutoryBreakdown odd = StatutoryCalculator2026.calculate(OCT, 2_500_000, 2_500_000, 1_000_010, 2_500_000, 2_500_000);
        check(odd.philHealthTotalPremiumCents == 50_001 && odd.philHealthEmployeeCents + odd.philHealthEmployerCents == 50_001,
            "PhilHealth shares always add up to the premium");
        StatutoryBreakdown cap = StatutoryCalculator2026.calculate(OCT, 20_000_000, 2_500_000, 15_000_000, 15_000_000, 2_500_000);
        check(cap.philHealthPremiumBaseCents == 10_000_000 && cap.philHealthEmployeeCents == 250_000, "PhilHealth ceiling");
        check(cap.pagIbigBaseCents == 1_000_000 && cap.pagIbigEmployeeCents == 20_000, "Pag-IBIG cap");
    }

    private static void annualisesTaxWithoutThirteenthMonth() {
        // 12 x (100,000 - 4,450) = 1,146,600 -> 102,500 + 25% x 346,600 = 189,150 / 12 = 15,762.50
        StatutoryBreakdown r = StatutoryCalculator2026.calculate(OCT, 10_000_000, 10_000_000, 10_000_000, 10_000_000, 10_000_000);
        check(r.withholdingTaxCents == 1_576_250, "annualised tax adds no 13th month");
    }

    private static void parsesPesosAndRejectsBadInput() {
        check(StatutoryCalculator2026.parsePesoCents("1,234.5", "x") == 123_450, "parses commas and one decimal");
        expectIllegal(() -> StatutoryCalculator2026.parsePesoCents("-1", "x"), "negative rejected");
        expectIllegal(() -> StatutoryCalculator2026.parsePesoCents("1.001", "x"), "three decimals rejected");
        expectIllegal(() -> StatutoryCalculator2026.parsePesoCents("10000000.01", "x"), "over ₱10M rejected");
        expectIllegal(() -> StatutoryCalculator2026.calculate(YearMonth.of(2025, 11), 1, 1, 1, 1, 1), "non-2026 period rejected");
        expectIllegal(() -> StatutoryCalculator2026.calculate(OCT, 0, 1, 1, 1, 1), "zero gross rejected");
        expectIllegal(() -> StatutoryCalculator2026.calculate(OCT, 10_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000),
            "deductions above gross blocked");
    }

    private static long msc(String compensation) { return sss(compensation).sssMscCents; }

    private static StatutoryBreakdown sss(String compensation) {
        long c = StatutoryCalculator2026.parsePesoCents(compensation, "x");
        return StatutoryCalculator2026.calculate(OCT, 10_000_000, c, 2_500_000, 2_500_000, 2_500_000);
    }

    private static String pesos(long cents) { return String.format("%d.%02d", cents / 100, cents % 100); }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            check(true, message);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static void usesRatesVersions() {
        StatutoryRates.Registry registry = new StatutoryRates.Registry();
        StatutoryRates base = StatutoryRates.DEFAULT;
        StatutoryRates pagIbig3 = StatutoryRates.version(registry.nextId(), YearMonth.of(2026, 7), "Pag-IBIG 3% test", "DEMO-005",
            base.sssEmployeeBp, base.sssEmployerBp, base.sssMinMsc, base.sssMaxMsc, base.philHealthPremiumBp, base.philHealthFloor, base.philHealthCeiling,
            base.pagIbigLowBand, base.pagIbigLowBp, 300, base.pagIbigEmployerBp, base.pagIbigCap, base.birAnnual);
        registry.add(pagIbig3);
        check(registry.ratesFor(YearMonth.of(2026, 6)) == base && registry.ratesFor(YearMonth.of(2026, 12)) == pagIbig3, "latest version on or before the month");
        // Pag-IBIG 3% of the ₱10,000 cap = ₱300 instead of ₱200.
        check(StatutoryCalculator2026.calculate(YearMonth.of(2026, 7), 2_500_000, 2_500_000, 2_500_000, 2_500_000, 2_500_000, pagIbig3).pagIbigEmployeeCents == 30_000,
            "a version changes the calculation");
        try {
            StatutoryRates.version("x", YearMonth.of(2026, 8), "", "DEMO-005", 500, 1_000, 500_000, 3_525_000, 500, 1_000_000, 10_000_000,
                150_000, 100, 200, 200, 1_000_000, base.birAnnual);
            throw new AssertionError("MSC range must be in ₱500 steps");
        } catch (IllegalArgumentException expected) {
            check(true, "SSS MSC range in ₱500 steps");
        }
    }
}
