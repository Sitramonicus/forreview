package ph.motorph.c_calc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.PayrollProfile;

/**
 * Turns a month of payroll maths into the slip a person is handed.
 *
 * The slip mirrors MotorPH's own sample layout — Monthly Salary, Daily Rate, Days Worked, Overtime, GROSS INCOME;
 * Rice Subsidy, Phone Allowance, Clothing Allowance, TOTAL; SSS, PhilHealth, Pag-IBIG, Withholding Tax, TOTAL
 * DEDUCTIONS; Gross, Benefits, Deductions, TAKE HOME PAY — because that is the document the payroll office already
 * issues and the one the sample payslip in the brief was written against.
 *
 * Two rules:
 * <ul>
 *   <li><b>Every figure comes from {@link MonthPayroll}.</b> Nothing is recomputed here, so the printed slip and the
 *       ledger agree by construction.</li>
 *   <li><b>The parts must sum.</b> {@link Payslip#netFromLines()} is checked against the engine's net; a mismatch
 *       throws rather than printing a slip that does not add up.</li>
 * </ul>
 */
public final class PayslipWriter {
    private PayslipWriter() {}

    /** Semicolon split of a month: cut-off 0 (1–15) is paid on the 15th, cut-off 1 (16–end) on the last day. */
    public static Payslip forCutoff(Employee employee, MonthPayroll payroll, int cutoff) {
        YearMonth month = payroll.getMonth();
        if (cutoff != 0 && cutoff != 1) throw new IllegalArgumentException("Cutoff must be 1-15 (0) or 16-end (1).");
        LocalDate start = cutoff == 0 ? month.atDay(1) : month.atDay(16);
        LocalDate end = cutoff == 0 ? month.atDay(15) : month.atEndOfMonth();
        LocalDate payDate = end;
        List<CutoffPay> cutoffs = payroll.getCutoffs();
        int daysWorked = cutoffs.size() > cutoff ? cutoffs.get(cutoff).getDaysWithRecords() : 0;
        String number = employee.getEid() + "-" + month + "-" + (cutoff == 0 ? "15" : month.atEndOfMonth().getDayOfMonth());
        Payslip slip = new Payslip(number, employee, month, cutoff, start, end, payDate,
            payroll.getStatutory() == null ? StatutoryCalculator2026.RULESET_ID : payroll.getStatutory().rulesetId);

        // ── earnings ────────────────────────────────────────────────────────────
        slip.daysWorked = daysWorked;
        // Contract figures, printed in the slip's header. They are informational, so they go in info[] and never in
        // earnings[]: counting a 53,500.00 salary as cut-off pay would break the addition below.
        slip.monthlySalaryCents = cents(employee.getMonthlyBasic());
        slip.dailyRateCents = PayrollProfile.dailyRate(employee.getEid()).map(PayslipWriter::cents).orElse(0L);
        slip.info.add(new Payslip.Line("Monthly Salary", slip.monthlySalaryCents));
        slip.info.add(new Payslip.Line("Daily Rate", slip.dailyRateCents));
        slip.info.add(new Payslip.Line("Days Worked", daysWorked));
        CutoffPay slice = cutoffs.size() > cutoff ? cutoffs.get(cutoff) : null;
        long basic = slice == null ? 0 : cents(slice.getBasicPay());
        long premium = slice == null ? 0 : cents(slice.getPremiumPay());
        long overtime = slice == null ? 0 : cents(slice.getOvertimePay());
        long nightDiff = slice == null ? 0 : cents(slice.getNightDiffPay());
        long holiday = slice == null ? 0 : cents(slice.getHolidayPay());
        long leave = slice == null ? 0 : cents(slice.getLeavePay());
        slip.earnings.add(new Payslip.Line("Basic pay for the cut-off", basic));
        if (premium != 0) slip.earnings.add(new Payslip.Line("Rest day / special day premium", premium));
        slip.earnings.add(new Payslip.Line("Overtime", overtime));
        if (nightDiff != 0) slip.earnings.add(new Payslip.Line("Night differential", nightDiff));
        if (holiday != 0) slip.earnings.add(new Payslip.Line("Holiday pay", holiday));
        if (leave != 0) slip.earnings.add(new Payslip.Line("Paid leave", leave));
        long bonuses = payroll.bonusCents(cutoff);
        if (bonuses != 0) slip.earnings.add(new Payslip.Line("Bonus", bonuses));
        slip.grossIncomeCents = payroll.grossCents(cutoff);

        // ── benefits (non-taxable allowances) ───────────────────────────────────
        slip.benefits.add(new Payslip.Line("Rice Subsidy", payroll.allowanceWithCorrections(cutoff, MonthPayroll.RICE)));
        slip.benefits.add(new Payslip.Line("Phone Allowance", payroll.allowanceWithCorrections(cutoff, MonthPayroll.PHONE)));
        slip.benefits.add(new Payslip.Line("Clothing Allowance", payroll.allowanceWithCorrections(cutoff, MonthPayroll.CLOTHING)));
        slip.benefitsCents = payroll.allowanceTotalCents(cutoff);

        // ── deductions ──────────────────────────────────────────────────────────
        slip.deductions.add(new Payslip.Line("Social Security System", payroll.lineCents(cutoff, MonthPayroll.SSS)));
        slip.deductions.add(new Payslip.Line("PhilHealth", payroll.lineCents(cutoff, MonthPayroll.PHILHEALTH)));
        slip.deductions.add(new Payslip.Line("Pag-IBIG", payroll.lineCents(cutoff, MonthPayroll.PAGIBIG)));
        slip.deductions.add(new Payslip.Line("Withholding Tax", payroll.lineCents(cutoff, MonthPayroll.TAX)));
        for (MonthPayroll.Taken taken : payroll.voluntary(cutoff)) {
            slip.deductions.add(new Payslip.Line(taken.item.description, taken.takenCents));
        }
        slip.deductionsCents = payroll.deductionsCents(cutoff);

        // ── the slip must add up ────────────────────────────────────────────────
        slip.netCents = slip.earningsTotal() + slip.benefitsTotal() - slip.deductionsTotal();
        long engineNet = payroll.netCents(cutoff);
        if (slip.netCents != engineNet) {
            throw new IllegalStateException("Payslip does not reconcile: lines give " + money(slip.netCents)
                + " but payroll computed " + money(engineNet) + " for " + employee.getEid() + " " + month
                + " cutoff " + cutoff + " (1-based " + (cutoff + 1) + "), from=" + payroll.getCutoffs().get(cutoff).getFrom()
                + " to=" + payroll.getCutoffs().get(cutoff).getTo());
        }
        return slip;
    }

    private static long cents(java.math.BigDecimal amount) {
        return amount == null ? 0 : amount.setScale(2, java.math.RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** "₱22,711.67" — the form used on the printed slip. */
    public static String money(long cents) {
        java.text.DecimalFormat format = new java.text.DecimalFormat("#,##0.00");
        String digits = format.format(Math.abs(cents) / 100.0);
        return (cents < 0 ? "-₱" : "₱") + digits;
    }

    public static String plain(long cents) {
        return java.math.BigDecimal.valueOf(cents, 2).toPlainString();
    }
}
