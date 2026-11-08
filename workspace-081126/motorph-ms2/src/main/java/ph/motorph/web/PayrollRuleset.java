package ph.motorph.web;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Effective-dated statutory ruleset. Every rate here is either published by the agency
 * or derived from the supplied MotorPH payslip, which acts as the controlling golden case.
 *
 * Verification anchor (MotorPH payslip 15-2026-01-15, employee 15, monthly basic 53,500):
 *   SSS employee 1,750.00 ; PhilHealth 1,337.50 ; Pag-IBIG 200.00 ;
 *   taxable 50,212.50 ; withholding 5,250.83 ; total deductions 8,538.33 ; net 22,711.67.
 * Those seven figures reproduce exactly under the rules below.
 */
public final class PayrollRuleset {

    public enum Period {
        MONTHLY(1),
        SEMI_MONTHLY(2);

        private final int periodsPerMonth;
        Period(int periodsPerMonth) { this.periodsPerMonth = periodsPerMonth; }
        public int periodsPerMonth() { return periodsPerMonth; }
        public static Period parse(String value) {
            if (value == null) throw new IllegalArgumentException("Pay period is required.");
            switch (value.trim().toUpperCase(java.util.Locale.ROOT).replace('-', '_')) {
                case "MONTHLY": return MONTHLY;
                case "SEMI_MONTHLY": case "SEMIMONTHLY": case "SEMI": return SEMI_MONTHLY;
                default: throw new IllegalArgumentException("Pay period must be Monthly or Semi-monthly.");
            }
        }
    }

    /** One withholding band: tax = base + (taxable - floor) * rate, for taxable above floor. */
    public static final class TaxBracket {
        private final BigDecimal floor;
        private final BigDecimal base;
        private final BigDecimal rate;

        TaxBracket(String floor, String base, String rate) {
            this.floor = new BigDecimal(floor);
            this.base = new BigDecimal(base);
            this.rate = new BigDecimal(rate);
        }

        public BigDecimal floor() { return floor; }
        public BigDecimal base() { return base; }
        public BigDecimal rate() { return rate; }

        public BigDecimal apply(BigDecimal taxable) {
            return Money.round(base.add(taxable.subtract(floor).multiply(rate)));
        }
    }

    /**
     * BIR Annex E (RR 11-2018 as amended by RA 10963).
     *
     * Both the floors AND the bases are the annual amounts divided by 12: 22,500/12 = 1,875.00,
     * 102,500/12 = 8,541.67, 402,500/12 = 33,541.67, 2,202,500/12 = 183,541.67. Carrying these to the
     * centavo matters: 8,541.80 vs 8,541.67 is a 13-centavo error on every payslip in those brackets.
     *
     * The bracket floors are the annual statutory thresholds divided by 12 (250,000/12 =
     * 20,833.33, 400,000/12 = 33,333.33, and so on). Published tables usually round these to
     * whole pesos, but only the exact twelfths reproduce the supplied MotorPH payslip:
     * 50,212.50 taxable gives 5,250.83, whereas a whole-peso 33,333 floor gives 5,250.90.
     * The two renditions differ by at most a third of a peso at a bracket edge.
     */
    private static final List<TaxBracket> MONTHLY_TAX = Collections.unmodifiableList(Arrays.asList(
        new TaxBracket("20833.33",  "0",           "0.15"),
        new TaxBracket("33333.33",  "1875.00",     "0.20"),
        new TaxBracket("66666.67",  "8541.67",     "0.25"),
        new TaxBracket("166666.67", "33541.67",    "0.30"),
        new TaxBracket("666666.67", "183541.67",   "0.35")
    ));

    /**
     * Annex E semi-monthly. This is its own published table with its own floors and bases; it
     * is not the monthly table halved. Floors are likewise the annual thresholds over 24.
     */
    private static final List<TaxBracket> SEMI_MONTHLY_TAX = Collections.unmodifiableList(Arrays.asList(
        new TaxBracket("10416.67",  "0",           "0.15"),
        new TaxBracket("16666.67",  "937.50",      "0.20"),
        new TaxBracket("33333.33",  "4270.70",     "0.25"),
        new TaxBracket("83333.33",  "16770.70",    "0.30"),
        new TaxBracket("333333.33", "91770.70",    "0.35")
    ));

    private final String id;
    private final String effectiveFrom;
    private final String effectiveTo;
    private final BigDecimal sssEmployeeRate;
    private final BigDecimal sssEmployerRate;
    private final BigDecimal sssMinMsc;
    private final BigDecimal sssMaxMsc;
    private final BigDecimal sssMscStep;
    private final BigDecimal sssEcLow;
    private final BigDecimal sssEcHigh;
    private final BigDecimal sssEcThreshold;
    private final BigDecimal sssMpfThreshold;
    private final BigDecimal phRate;
    private final BigDecimal phFloor;
    private final BigDecimal phCeiling;
    private final BigDecimal pagibigMaxFund;
    private final BigDecimal pagibigLowThreshold;
    private final BigDecimal pagibigEmployeeLow;
    private final BigDecimal pagibigEmployeeHigh;
    private final BigDecimal pagibigEmployerRate;

    private PayrollRuleset(int year) {
        this.id = "PH-STATUTORY-" + year;
        this.effectiveFrom = year + "-01-01";
        this.effectiveTo = year + "-12-31";
        // SSS Circular 2024-006: 15% total, MSC 5,000-35,000 in 500 steps. Unchanged for 2026.
        this.sssEmployeeRate = new BigDecimal("0.05");
        this.sssEmployerRate = new BigDecimal("0.10");
        this.sssMinMsc = new BigDecimal("5000");
        this.sssMaxMsc = new BigDecimal("35000");
        this.sssMscStep = new BigDecimal("500");
        this.sssEcLow = new BigDecimal("10.00");
        this.sssEcHigh = new BigDecimal("30.00");
        this.sssEcThreshold = new BigDecimal("15000");
        this.sssMpfThreshold = new BigDecimal("20000");
        // PhilHealth: 5% of monthly basic, floor 10,000, ceiling 100,000, split evenly.
        this.phRate = new BigDecimal("0.05");
        this.phFloor = new BigDecimal("10000");
        this.phCeiling = new BigDecimal("100000");
        // Pag-IBIG: max fund salary 10,000; employee 1% at or below 1,500 else 2%; employer 2%.
        this.pagibigMaxFund = new BigDecimal("10000");
        this.pagibigLowThreshold = new BigDecimal("1500");
        this.pagibigEmployeeLow = new BigDecimal("0.01");
        this.pagibigEmployeeHigh = new BigDecimal("0.02");
        this.pagibigEmployerRate = new BigDecimal("0.02");
    }

    /** Earliest date any loaded ruleset covers; salary history starts here. */
    public static final java.time.LocalDate StartOfFirstCoveredYear = java.time.LocalDate.of(2025, 1, 1);

    private static final PayrollRuleset Y2026 = new PayrollRuleset(2026);
    private static final PayrollRuleset Y2025 = new PayrollRuleset(2025);

    public static PayrollRuleset forYear(int year) {
        if (year == 2026) return Y2026;
        if (year == 2025) return Y2025;
        throw new IllegalArgumentException("No statutory ruleset is loaded for " + year
            + ". Available: 2025, 2026. Refusing to calculate outside a covered year.");
    }

    public String id() { return id; }
    public String effectiveFrom() { return effectiveFrom; }
    public String effectiveTo() { return effectiveTo; }
    public BigDecimal sssEmployeeRate() { return sssEmployeeRate; }
    public BigDecimal sssEmployerRate() { return sssEmployerRate; }
    public BigDecimal sssMinMsc() { return sssMinMsc; }
    public BigDecimal sssMaxMsc() { return sssMaxMsc; }
    public BigDecimal sssMscStep() { return sssMscStep; }
    public BigDecimal sssEcLow() { return sssEcLow; }
    public BigDecimal sssEcHigh() { return sssEcHigh; }
    public BigDecimal sssEcThreshold() { return sssEcThreshold; }
    public BigDecimal sssMpfThreshold() { return sssMpfThreshold; }
    public BigDecimal phRate() { return phRate; }
    public BigDecimal phFloor() { return phFloor; }
    public BigDecimal phCeiling() { return phCeiling; }
    public BigDecimal pagibigMaxFund() { return pagibigMaxFund; }
    public BigDecimal pagibigLowThreshold() { return pagibigLowThreshold; }
    public BigDecimal pagibigEmployeeLow() { return pagibigEmployeeLow; }
    public BigDecimal pagibigEmployeeHigh() { return pagibigEmployeeHigh; }
    public BigDecimal pagibigEmployerRate() { return pagibigEmployerRate; }

    public List<TaxBracket> taxBrackets(Period period) {
        List<TaxBracket> source = period == Period.SEMI_MONTHLY ? SEMI_MONTHLY_TAX : MONTHLY_TAX;
        return new ArrayList<>(source);
    }

    /**
     * Withholding for one period. Bands are cumulative over the period's own table, so the
     * caller passes the taxable amount for that period, not an annualised figure.
     */
    public BigDecimal withholding(BigDecimal taxablePeriod, Period period) {
        if (Money.isNegative(taxablePeriod)) throw new IllegalArgumentException("Taxable amount must not be negative.");
        TaxBracket chosen = null;
        for (TaxBracket bracket : taxBrackets(period)) {
            if (taxablePeriod.compareTo(bracket.floor()) > 0) chosen = bracket;
        }
        return chosen == null ? Money.ZERO : chosen.apply(taxablePeriod);
    }

    /** Employee share quoted for a payslip line and for the employer's remittance total. */
    public BigDecimal sssEmployee(BigDecimal monthlyBasic) {
        return Money.round(monthlySalaryCredit(monthlyBasic).multiply(sssEmployeeRate));
    }

    public BigDecimal sssEmployer(BigDecimal monthlyBasic) {
        return Money.round(monthlySalaryCredit(monthlyBasic).multiply(sssEmployerRate));
    }

    public BigDecimal sssEc(BigDecimal monthlyBasic) {
        BigDecimal msc = monthlySalaryCredit(monthlyBasic);
        return msc.compareTo(sssEcThreshold()) >= 0 ? sssEcHigh : sssEcLow;
    }

    /**
     * Salary credit for the nearest 500 bracket, then clamped to the published floor and ceiling.
     * SSS publishes each bracket as a range (e.g. 5,250.00-5,749.99 -> 5,500.00), so a salary lands
     * on the bracket it falls inside; it is never rounded up to the next whole step. Rounding up
     * would put 25,249 on 25,500 instead of the published 25,000.
     */
    public BigDecimal monthlySalaryCredit(BigDecimal monthlyBasic) {
        if (Money.isNegative(monthlyBasic)) throw new IllegalArgumentException("Monthly basic pay must not be negative.");
        BigDecimal clamped = Money.min(Money.max(monthlyBasic, sssMinMsc), sssMaxMsc);
        // Brackets split each 500 step at its midpoint: [n*500 - 250, n*500 + 250) maps to n*500.
        BigDecimal halfStep = sssMscStep.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.UNNECESSARY);
        BigDecimal shifted = clamped.add(halfStep);
        BigDecimal divided = shifted.divide(sssMscStep, 0, java.math.RoundingMode.FLOOR);
        BigDecimal rounded = divided.multiply(sssMscStep);
        return Money.min(Money.max(rounded, sssMinMsc), sssMaxMsc);
    }

    public BigDecimal philHealthTotal(BigDecimal monthlyBasic) {
        BigDecimal base = Money.min(Money.max(monthlyBasic, phFloor), phCeiling);
        return Money.round(base.multiply(phRate));
    }

    public BigDecimal philHealthEmployee(BigDecimal monthlyBasic) {
        return Money.round(philHealthTotal(monthlyBasic).divide(new BigDecimal("2"), 4, java.math.RoundingMode.HALF_UP));
    }

    public BigDecimal philHealthEmployer(BigDecimal monthlyBasic) {
        return Money.round(philHealthTotal(monthlyBasic).subtract(philHealthEmployee(monthlyBasic)));
    }

    public BigDecimal pagibigEmployee(BigDecimal monthlyBasic) {
        BigDecimal fund = Money.min(Money.max(monthlyBasic, Money.ZERO), pagibigMaxFund);
        BigDecimal rate = fund.compareTo(pagibigLowThreshold) <= 0 ? pagibigEmployeeLow : pagibigEmployeeHigh;
        return Money.round(fund.multiply(rate));
    }

    public BigDecimal pagibigEmployer(BigDecimal monthlyBasic) {
        BigDecimal fund = Money.min(Money.max(monthlyBasic, Money.ZERO), pagibigMaxFund);
        return Money.round(fund.multiply(pagibigEmployerRate));
    }
}
