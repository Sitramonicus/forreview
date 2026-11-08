package ph.motorph.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Payroll calculation, grounded in the MotorPH sample payslip supplied on 2026-11-05 and the
 * rate structure of sweldongpinoy.com (Basic Pay + taxable/non-taxable allowance + night
 * differential + overtime -> statutory contributions -> withholding -> net).
 *
 * The supplied payslip (employee 15, 2026-01-02..2026-01-15) is reproduced to the centavo:
 *   daily rate 2,675.00 = monthly basic 53,500 / 20
 *   10 days worked     = 26,750.00 gross
 *   SSS 1,750.00, PhilHealth 1,337.50, Pag-IBIG 200.00, withholding 5,250.83
 *   total deductions 8,538.33, take home 22,711.67
 *
 * Two MotorPH conventions are implemented exactly as evidenced and are flagged in the API
 * response as assumptions rather than re-derived:
 *   1. Statutory contributions are computed on the MONTHLY basic salary, and the full monthly
 *      amount is deducted inside a semi-monthly cutoff (the supplied payslip does this).
 *   2. Withholding is computed on monthly basic minus employee statutory shares, on the
 *      monthly Annex E table.
 */
public final class PayrollService {

    /** MotorPH's daily-rate divisor, proven by the supplied payslip (53,500 / 20 = 2,675). */
    public static final BigDecimal DAYS_PER_MONTH = new BigDecimal("20");
    /** MotorPH's hourly-rate divisor, verified against every row of the supplied employee file. */
    public static final BigDecimal HOURS_PER_MONTH = new BigDecimal("168");
    public static final BigDecimal HOURS_PER_DAY = new BigDecimal("8");

    /** What the withholding is computed on. BASIC_ONLY reproduces the supplied payslip. */
    public enum TaxBasis { BASIC_ONLY, BASIC_PLUS_TAXABLE_ALLOWANCE }
    /** How a monthly statutory amount is applied to a pay period. */
    public enum StatutoryBasis { FULL_MONTHLY_EACH_PERIOD, SPLIT_EVENLY }

    public static final class Input {
        public String employeeId;
        public String employeeName = "";
        public String position = "";
        public String department = "";
        public String payPeriod = "MONTHLY";
        public String periodStart = "";
        public String periodEnd = "";
        public BigDecimal monthlyBasic;
        public BigDecimal daysWorked;
        public BigDecimal overtimeHours = BigDecimal.ZERO;
        public BigDecimal overtimeRatePercent = new BigDecimal("125");
        public BigDecimal nightDifferentialHours = BigDecimal.ZERO;
        public BigDecimal nightDifferentialRatePercent = new BigDecimal("10");
        public BigDecimal riceSubsidy = Money.ZERO;
        public BigDecimal phoneAllowance = Money.ZERO;
        public BigDecimal clothingAllowance = Money.ZERO;
        public BigDecimal taxableAllowance = Money.ZERO;
        public BigDecimal otherDeductions = Money.ZERO;
        public String taxBasis = TaxBasis.BASIC_ONLY.name();
        public String statutoryBasis = StatutoryBasis.FULL_MONTHLY_EACH_PERIOD.name();
    }

    public Payslip compute(Input input, PayrollRuleset ruleset) {
        validate(input);
        PayrollRuleset.Period period = PayrollRuleset.Period.parse(input.payPeriod);
        TaxBasis taxBasis = parseTaxBasis(input.taxBasis);
        StatutoryBasis statutoryBasis = parseStatutoryBasis(input.statutoryBasis);

        BigDecimal monthlyBasic = Money.round(input.monthlyBasic);
        BigDecimal dailyRate = divideMonths(monthlyBasic, DAYS_PER_MONTH);
        BigDecimal hourlyRate = divideMonths(monthlyBasic, HOURS_PER_MONTH);
        BigDecimal daysWorked = input.daysWorked;

        BigDecimal basePay = Money.round(dailyRate.multiply(daysWorked));
        BigDecimal overtimePay = Money.round(hourlyRate.multiply(input.overtimeHours)
            .multiply(input.overtimeRatePercent).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal nightDifferentialPay = Money.round(hourlyRate.multiply(input.nightDifferentialHours)
            .multiply(input.nightDifferentialRatePercent).divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        BigDecimal grossIncome = Money.round(basePay.add(overtimePay).add(nightDifferentialPay));

        BigDecimal nonTaxable = Money.round(input.riceSubsidy.add(input.phoneAllowance).add(input.clothingAllowance));
        BigDecimal taxableAllowance = Money.round(input.taxableAllowance);
        BigDecimal totalBenefits = Money.round(nonTaxable.add(taxableAllowance));

        // Statutory amounts are monthly figures; the split decides how much lands in this period.
        BigDecimal divisor = statutoryBasis == StatutoryBasis.SPLIT_EVENLY && period == PayrollRuleset.Period.SEMI_MONTHLY
            ? new BigDecimal(period.periodsPerMonth()) : BigDecimal.ONE;
        BigDecimal sss = perPeriod(ruleset.sssEmployee(monthlyBasic), divisor);
        BigDecimal philHealth = perPeriod(ruleset.philHealthEmployee(monthlyBasic), divisor);
        BigDecimal pagibig = perPeriod(ruleset.pagibigEmployee(monthlyBasic), divisor);

        BigDecimal monthlyStatutory = Money.round(ruleset.sssEmployee(monthlyBasic)
            .add(ruleset.philHealthEmployee(monthlyBasic)).add(ruleset.pagibigEmployee(monthlyBasic)));
        BigDecimal taxableMonthly = taxBasis == TaxBasis.BASIC_PLUS_TAXABLE_ALLOWANCE
            ? Money.round(monthlyBasic.subtract(monthlyStatutory).add(taxableAllowance))
            : Money.round(monthlyBasic.subtract(monthlyStatutory));
        if (Money.isNegative(taxableMonthly)) taxableMonthly = Money.ZERO;

        BigDecimal monthlyWithholding = ruleset.withholding(taxableMonthly, PayrollRuleset.Period.MONTHLY);
        BigDecimal otherDeductions = Money.round(input.otherDeductions);
        BigDecimal withholding = perPeriod(monthlyWithholding, divisor);
        BigDecimal totalDeductions = Money.round(sss.add(philHealth).add(pagibig).add(withholding).add(otherDeductions));

        BigDecimal takeHome = Money.round(grossIncome.add(totalBenefits).subtract(totalDeductions));
        if (Money.isNegative(takeHome)) {
            throw new IllegalArgumentException("Deductions exceed pay for " + input.employeeId
                + " (take-home would be negative). Correct the inputs before saving this run.");
        }

        BigDecimal employerSss = perPeriod(ruleset.sssEmployer(monthlyBasic), divisor);
        BigDecimal employerEc = perPeriod(ruleset.sssEc(monthlyBasic), divisor);
        BigDecimal employerPhilHealth = perPeriod(ruleset.philHealthEmployer(monthlyBasic), divisor);
        BigDecimal employerPagibig = perPeriod(ruleset.pagibigEmployer(monthlyBasic), divisor);
        BigDecimal employerCost = Money.round(grossIncome.add(totalBenefits)
            .add(employerSss).add(employerEc).add(employerPhilHealth).add(employerPagibig));

        return Payslip.builder()
            .employee(input.employeeId, input.employeeName, input.position, input.department)
            .period(period.name(), input.periodStart, input.periodEnd)
            .ruleset(ruleset.id())
            .salary(monthlyBasic, dailyRate, daysWorked, hourlyRate)
            .premiums(input.overtimeHours, overtimePay, input.nightDifferentialHours, nightDifferentialPay, grossIncome)
            .benefits(input.riceSubsidy, input.phoneAllowance, input.clothingAllowance,
                taxableAllowance, nonTaxable, totalBenefits)
            .deductions(sss, philHealth, pagibig, withholding, totalDeductions)
            .employer(employerSss, employerEc, employerPhilHealth, employerPagibig, employerCost)
            .result(taxableMonthly, takeHome)
            .build();
    }

    /** Employer-side totals for remittance reporting; monthly figures regardless of period split. */
    public List<String[]> remittanceRows(List<Payslip> payslips) {
        BigDecimal sss = Money.ZERO, ec = Money.ZERO, ph = Money.ZERO, pi = Money.ZERO, tax = Money.ZERO;
        for (Payslip slip : payslips) {
            sss = sss.add(slip.sssEmployee()).add(slip.sssEmployer());
            ec = ec.add(slip.sssEc());
            ph = ph.add(slip.philHealthEmployee()).add(slip.philHealthEmployer());
            pi = pi.add(slip.pagibigEmployee()).add(slip.pagibigEmployer());
            tax = tax.add(slip.withholdingTax());
        }
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Social Security System", Money.format(sss)});
        rows.add(new String[]{"Employees' Compensation", Money.format(ec)});
        rows.add(new String[]{"PhilHealth", Money.format(ph)});
        rows.add(new String[]{"Pag-IBIG", Money.format(pi)});
        rows.add(new String[]{"BIR withholding", Money.format(tax)});
        return rows;
    }

    private static BigDecimal divideMonths(BigDecimal monthly, BigDecimal divisor) {
        return monthly.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal perPeriod(BigDecimal monthlyAmount, BigDecimal divisor) {
        if (divisor.compareTo(BigDecimal.ONE) == 0) return monthlyAmount;
        return Money.round(monthlyAmount.divide(divisor, 4, RoundingMode.HALF_UP));
    }

    private static TaxBasis parseTaxBasis(String value) {
        if (value == null || value.trim().isEmpty()) return TaxBasis.BASIC_ONLY;
        try {
            return TaxBasis.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tax basis must be BASIC_ONLY or BASIC_PLUS_TAXABLE_ALLOWANCE.");
        }
    }

    private static StatutoryBasis parseStatutoryBasis(String value) {
        if (value == null || value.trim().isEmpty()) return StatutoryBasis.FULL_MONTHLY_EACH_PERIOD;
        try {
            return StatutoryBasis.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Statutory basis must be FULL_MONTHLY_EACH_PERIOD or SPLIT_EVENLY.");
        }
    }

    private static void validate(Input input) {
        if (input == null) throw new IllegalArgumentException("Payroll input is required.");
        if (input.employeeId == null || input.employeeId.trim().isEmpty()) throw new IllegalArgumentException("Employee ID is required.");
        if (input.monthlyBasic == null) throw new IllegalArgumentException("Monthly basic pay is required.");
        if (Money.isNegative(input.monthlyBasic)) throw new IllegalArgumentException("Monthly basic pay must not be negative.");
        if (input.daysWorked == null) throw new IllegalArgumentException("Days worked is required.");
        if (Money.isNegative(input.daysWorked)) throw new IllegalArgumentException("Days worked must not be negative.");
        if (input.daysWorked.compareTo(DAYS_PER_MONTH) > 0 && "MONTHLY".equalsIgnoreCase(input.payPeriod)) {
            throw new IllegalArgumentException("Days worked cannot exceed " + DAYS_PER_MONTH + " in one monthly period.");
        }
        if (input.daysWorked.compareTo(DAYS_PER_MONTH.divide(new BigDecimal("2"))) > 0
            && "SEMI_MONTHLY".equalsIgnoreCase(input.payPeriod)) {
            throw new IllegalArgumentException("Days worked cannot exceed 10 in one semi-monthly period.");
        }
        requireNonNegative(input.overtimeHours, "Overtime hours");
        requireNonNegative(input.nightDifferentialHours, "Night differential hours");
        requireNonNegative(input.riceSubsidy, "Rice subsidy");
        requireNonNegative(input.phoneAllowance, "Phone allowance");
        requireNonNegative(input.clothingAllowance, "Clothing allowance");
        requireNonNegative(input.taxableAllowance, "Taxable allowance");
        requireNonNegative(input.otherDeductions, "Other deductions");
        requireNonNegative(input.overtimeRatePercent, "Overtime rate");
        requireNonNegative(input.nightDifferentialRatePercent, "Night differential rate");
        PayrollRuleset.Period period = PayrollRuleset.Period.parse(input.payPeriod);
        if (period == PayrollRuleset.Period.SEMI_MONTHLY) {
            // 10 days is the semi-monthly ceiling at a 20-day month.
            if (input.daysWorked.compareTo(new BigDecimal("10")) > 0) {
                throw new IllegalArgumentException("Days worked cannot exceed 10 in a semi-monthly period.");
            }
        }
        requireIsoDate(input.periodStart, "Period start");
        requireIsoDate(input.periodEnd, "Period end");
        if (input.periodStart.compareTo(input.periodEnd) > 0) {
            throw new IllegalArgumentException("Period start must be on or before period end.");
        }
    }

    private static void requireNonNegative(BigDecimal value, String field) {
        if (value != null && Money.isNegative(value)) throw new IllegalArgumentException(field + " must not be negative.");
    }

    private static void requireIsoDate(String value, String field) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(field + " is required.");
        try {
            LocalDate parsed = LocalDate.parse(value.trim());
            if (!parsed.toString().equals(value.trim())) throw new IllegalArgumentException(field + " must be YYYY-MM-DD.");
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException(field + " must be YYYY-MM-DD.");
        }
    }
}
