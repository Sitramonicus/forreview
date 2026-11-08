package ph.motorph.c_calc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import ph.motorph.o_oop.Employee;

/**
 * One employee's pay for one cut-off, ready to print: earnings, benefits, deductions and the take-home figure.
 *
 * A payslip is assembled only from {@link MonthPayroll}, the same engine that produces the company's payroll run, so
 * the figure on the paper and the figure in the ledger cannot drift. Every amount is in centavos until the moment it
 * is formatted, which is what keeps the totals adding up to the last centavo of MotorPH's sample slip.
 */
public final class Payslip {
    /** A labelled money line on the slip. */
    public static final class Line {
        public final String label;
        public final long cents;
        public final boolean emphasis;

        public Line(String label, long cents) { this(label, cents, false); }
        public Line(String label, long cents, boolean emphasis) {
            this.label = label; this.cents = cents; this.emphasis = emphasis;
        }
    }

    public final String number;              // "15-2026-01-15", the form MotorPH's own slip uses
    public final Employee employee;
    public final YearMonth month;
    public final int cutoff;                 // 0 = 1–15, 1 = 16–end
    public final LocalDate periodStart, periodEnd, payDate;

    public final List<Line> earnings = new ArrayList<>();
    public final List<Line> benefits = new ArrayList<>();
    public final List<Line> deductions = new ArrayList<>();

    /**
     * Header facts the paper prints but the arithmetic must ignore: the monthly salary, the daily rate and the days
     * worked are a description of the contract, not pay for this cut-off. They live here, outside every total, so that
     * {@link #earningsTotal()} can never be inflated by them.
     */
    public final List<Line> info = new ArrayList<>();

    public long grossIncomeCents, benefitsCents, deductionsCents, netCents;
    public int daysWorked;
    /** Contract figures, mirrored as cents for callers that want to print them without scanning {@link #info}. */
    public long monthlySalaryCents, dailyRateCents;
    public final String rulesetId;

    Payslip(String number, Employee employee, YearMonth month, int cutoff, LocalDate periodStart, LocalDate periodEnd,
            LocalDate payDate, String rulesetId) {
        this.number = number;
        this.employee = employee;
        this.month = month;
        this.cutoff = cutoff;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.payDate = payDate;
        this.rulesetId = rulesetId;
    }

    public String periodLabel() {
        return periodStart + " to " + periodEnd;
    }

    /** "Jan 1–15, 2026" — the heading form. */
    public String periodHeading() {
        return periodStart.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
            + " " + periodStart.getDayOfMonth() + "–" + periodEnd.getDayOfMonth() + ", " + periodEnd.getYear();
    }

    /** Total of the earnings block, recomputed so a caller can never ship a slip whose parts do not sum. */
    public long earningsTotal() {
        long total = 0;
        for (Line line : earnings) total += line.cents;
        return total;
    }
    public long benefitsTotal() {
        long total = 0;
        for (Line line : benefits) total += line.cents;
        return total;
    }
    public long deductionsTotal() {
        long total = 0;
        for (Line line : deductions) total += line.cents;
        return total;
    }

    /** Net, derived rather than stored, so it always equals earnings + benefits − deductions. */
    public long netFromLines() { return earningsTotal() + benefitsTotal() - deductionsTotal(); }
}
