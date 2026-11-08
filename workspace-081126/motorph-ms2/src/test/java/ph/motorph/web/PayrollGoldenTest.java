package ph.motorph.web;

import java.math.BigDecimal;

/**
 * The controlling test: the engine must reproduce the MotorPH sample payslip exactly.
 * Source: "Copy of MotorPH Payslip - MotorPH Payslip.csv", payslip 15-2026-01-15.
 * Any change to rates, rounding, or bases that breaks one of these assertions is a regression
 * against a document MotorPH itself produced.
 */
public final class PayrollGoldenTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        reproducesSuppliedPayslip();
        bracketsBehaveAtBoundaries();
        salaryCreditRoundsUpToBracket();
        splitsAndScalesSemiMonthly();
        rejectsUncoveredYearAndBadInput();
        remittanceTotalsBothShares();
        System.out.println("PayrollGoldenTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static PayrollService.Input payslipInput() {
        PayrollService.Input in = new PayrollService.Input();
        in.employeeId = "15";
        in.employeeName = "Romualdez, Fredrick";
        in.position = "Account Manager";
        in.department = "Accounting";
        in.payPeriod = "SEMI_MONTHLY";
        in.periodStart = "2026-01-02";
        in.periodEnd = "2026-01-15";
        in.monthlyBasic = new BigDecimal("53500");
        in.daysWorked = new BigDecimal("10");
        in.riceSubsidy = new BigDecimal("1500");
        in.phoneAllowance = new BigDecimal("2000");
        in.clothingAllowance = new BigDecimal("1000");
        return in;
    }

    private static void reproducesSuppliedPayslip() {
        Payslip slip = new PayrollService().compute(payslipInput(), PayrollRuleset.forYear(2026));
        check("daily rate 2,675.00", "2675.00", slip.dailyRate());
        check("hourly rate 318.45", "318.45", slip.hourlyRate());
        check("gross income 26,750.00", "26750.00", slip.grossIncome());
        check("rice 1,500.00", "1500.00", slip.riceSubsidy());
        check("phone 2,000.00", "2000.00", slip.phoneAllowance());
        check("clothing 1,000.00", "1000.00", slip.clothingAllowance());
        check("benefits total 4,500.00", "4500.00", slip.totalBenefits());
        check("SSS 1,750.00", "1750.00", slip.sssEmployee());
        check("PhilHealth 1,337.50", "1337.50", slip.philHealthEmployee());
        check("Pag-IBIG 200.00", "200.00", slip.pagibigEmployee());
        check("taxable base 50,212.50", "50212.50", slip.taxableIncome());
        check("withholding 5,250.83", "5250.83", slip.withholdingTax());
        check("total deductions 8,538.33", "8538.33", slip.totalDeductions());
        check("take home 22,711.67", "22711.67", slip.takeHomePay());
        require("ruleset id", "PH-STATUTORY-2026", slip.rulesetId());
    }

    private static void bracketsBehaveAtBoundaries() {
        PayrollRuleset r = PayrollRuleset.forYear(2026);
        PayrollRuleset.Period m = PayrollRuleset.Period.MONTHLY;
        check("20,833 exempt", "0.00", r.withholding(new BigDecimal("20833"), m));
        check("20,833.01 taxed at 15%", "0.00", r.withholding(new BigDecimal("20833.01"), m));
        check("20,834 -> 0.10", "0.10", r.withholding(new BigDecimal("20834"), m));
        // Just below the 33,333.33 edge the 15% band still applies.
        check("33,332.50 stays in 15% band", "1874.88", r.withholding(new BigDecimal("33332.50"), m));
        check("33,334.00 starts base 1,875.00", "1875.13", r.withholding(new BigDecimal("33334.00"), m));
        check("66,666.67 enters 25% band", "8541.67", r.withholding(new BigDecimal("66666.68"), m));
        check("666,666.67 enters 35% band", "183541.67", r.withholding(new BigDecimal("666666.68"), m));
        // Base amounts are the annual BIR figures over 12: 102,500 / 402,500 / 2,202,500.
        check("25% base is 102,500/12", "8541.67", r.taxBrackets(m).get(2).base());
        check("30% base is 402,500/12", "33541.67", r.taxBrackets(m).get(3).base());
        check("35% base is 2,202,500/12", "183541.67", r.taxBrackets(m).get(4).base());
        // Cross-checked against sweldongpinoy.com: 90,000 basic -> tax 13,325.00, not 13,325.13.
        check("90,000 basic taxes to 13,325.00", "13325.00", r.withholding(new BigDecimal("85800.00"), m));
        // Semi-monthly is its own published table, not the monthly table divided by two.
        check("semi 10,416 exempt", "0.00", r.withholding(new BigDecimal("10416"), PayrollRuleset.Period.SEMI_MONTHLY));
        check("semi 10,418 -> 0.15", "0.20", r.withholding(new BigDecimal("10418"), PayrollRuleset.Period.SEMI_MONTHLY));
        check("semi 16,668 -> 937.50 base", "937.77", r.withholding(new BigDecimal("16668.00"), PayrollRuleset.Period.SEMI_MONTHLY));
    }

    private static void salaryCreditRoundsUpToBracket() {
        PayrollRuleset r = PayrollRuleset.forYear(2026);
        check("MSC floor 5,000", "5000.00", r.monthlySalaryCredit(new BigDecimal("1200")));
        // SSS publishes brackets as ranges; a salary lands on the bracket it falls inside.
        check("MSC 20,001 -> 20,000", "20000.00", r.monthlySalaryCredit(new BigDecimal("20001")));
        check("MSC 10,001 -> 10,000", "10000.00", r.monthlySalaryCredit(new BigDecimal("10001")));
        check("MSC 25,249 -> 25,000", "25000.00", r.monthlySalaryCredit(new BigDecimal("25249")));
        check("MSC 25,251 -> 25,500", "25500.00", r.monthlySalaryCredit(new BigDecimal("25251")));
        // Published bracket 5,250.00-5,749.99 -> 5,500 and 5,000.00-5,249.99 -> 5,000.
        check("MSC 5,250 -> 5,500 (bracket starts here)", "5500.00", r.monthlySalaryCredit(new BigDecimal("5250")));
        check("MSC 5,249 -> 5,000 (previous bracket)", "5000.00", r.monthlySalaryCredit(new BigDecimal("5249")));
        check("MSC 53,500 caps at 35,000", "35000.00", r.monthlySalaryCredit(new BigDecimal("53500")));
        check("MSC ceiling 35,000", "35000.00", r.monthlySalaryCredit(new BigDecimal("90000")));
        check("EC low at 14,499", "10.00", r.sssEc(new BigDecimal("14499")));
        check("EC high at 15,000", "30.00", r.sssEc(new BigDecimal("15000")));
        check("PhilHealth floor premium/2", "250.00", r.philHealthEmployee(new BigDecimal("9000")));
        check("PhilHealth ceiling premium/2", "2500.00", r.philHealthEmployee(new BigDecimal("150000")));
        check("Pag-IBIG 1% at 1,500", "15.00", r.pagibigEmployee(new BigDecimal("1500")));
        check("Pag-IBIG 2% at 1,501", "30.02", r.pagibigEmployee(new BigDecimal("1501")));
        check("Pag-IBIG capped at 10,000", "200.00", r.pagibigEmployee(new BigDecimal("40000")));
        check("SSS employer 10% of 53,500 MSC", "3500.00", r.sssEmployer(new BigDecimal("53500")));
    }

    private static void splitsAndScalesSemiMonthly() {
        PayrollService.Input in = payslipInput();
        in.statutoryBasis = "SPLIT_EVENLY";
        Payslip slip = new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        check("split SSS half", "875.00", slip.sssEmployee());
        check("split PhilHealth half", "668.75", slip.philHealthEmployee());
        check("split Pag-IBIG half", "100.00", slip.pagibigEmployee());
        check("split withholding half", "2625.42", slip.withholdingTax());

        PayrollService.Input overtime = payslipInput();
        overtime.payPeriod = "MONTHLY";
        overtime.daysWorked = new BigDecimal("20");
        overtime.overtimeHours = new BigDecimal("2");
        PayrollService service = new PayrollService();
        Payslip withOt = service.compute(overtime, PayrollRuleset.forYear(2026));
        check("overtime 2h at 125%", "796.13", withOt.overtimePay());
        check("gross includes overtime", "54296.13", withOt.grossIncome());

        PayrollService.Input nd = payslipInput();
        nd.nightDifferentialHours = new BigDecimal("3");
        Payslip withNd = service.compute(nd, PayrollRuleset.forYear(2026));
        check("night differential 3h at 10%", "95.54", withNd.nightDifferentialPay());

        PayrollService.Input taxable = payslipInput();
        taxable.taxBasis = "BASIC_PLUS_TAXABLE_ALLOWANCE";
        taxable.taxableAllowance = new BigDecimal("1000");
        Payslip withAllowance = service.compute(taxable, PayrollRuleset.forYear(2026));
        check("taxable allowance raises tax base", "51212.50", withAllowance.taxableIncome());
        check("taxable allowance raises withholding", "5450.83", withAllowance.withholdingTax());
    }

    private static void rejectsUncoveredYearAndBadInput() {
        expectFail("no ruleset for 2024", () -> PayrollRuleset.forYear(2024));
        expectFail("negative basic", () -> {
            PayrollService.Input in = payslipInput();
            in.monthlyBasic = new BigDecimal("-1");
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("too many days for monthly", () -> {
            PayrollService.Input in = payslipInput();
            in.payPeriod = "MONTHLY";
            in.daysWorked = new BigDecimal("21");
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("too many days for semi-monthly", () -> {
            PayrollService.Input in = payslipInput();
            in.daysWorked = new BigDecimal("11");
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("negative take home", () -> {
            PayrollService.Input in = payslipInput();
            in.daysWorked = BigDecimal.ZERO;
            in.otherDeductions = new BigDecimal("9000");
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("bad period order", () -> {
            PayrollService.Input in = payslipInput();
            in.periodStart = "2026-01-15";
            in.periodEnd = "2026-01-02";
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("bad date format", () -> {
            PayrollService.Input in = payslipInput();
            in.periodStart = "1/2/2026";
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
        expectFail("unknown pay period", () -> {
            PayrollService.Input in = payslipInput();
            in.payPeriod = "WEEKLY";
            new PayrollService().compute(in, PayrollRuleset.forYear(2026));
        });
    }

    private static void remittanceTotalsBothShares() {
        Payslip slip = new PayrollService().compute(payslipInput(), PayrollRuleset.forYear(2026));
        java.util.List<String[]> rows = new PayrollService().remittanceRows(java.util.Arrays.asList(slip));
        check("remittance SSS total = 1,750 + 3,500", "5250.00", new BigDecimal(rows.get(0)[1].replace(",", "")));
        check("remittance PhilHealth total = 2,675.00", "2675.00", new BigDecimal(rows.get(2)[1].replace(",", "")));
        check("remittance Pag-IBIG total = 400.00", "400.00", new BigDecimal(rows.get(3)[1].replace(",", "")));
        check("remittance tax = withholding", "5250.83", new BigDecimal(rows.get(4)[1].replace(",", "")));
        check("employer cost includes both shares", "36317.50", slip.employerCost());
        check("employer SSS 10%", "3500.00", slip.sssEmployer());
        check("employer EC", "30.00", slip.sssEc());
    }

    private static void require(String label, String expected, String actual) {
        if (expected.equals(actual)) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL " + label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void check(String label, String expected, BigDecimal actual) {
        String got = actual.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        if (expected.equals(got)) {
            passed++;
        } else {
            failed++;
            System.out.println("  FAIL " + label + ": expected " + expected + " but got " + got);
        }
    }

    private static void expectFail(String label, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL " + label + ": expected rejection but the call succeeded");
        } catch (RuntimeException expected) {
            passed++;
        }
    }
}
