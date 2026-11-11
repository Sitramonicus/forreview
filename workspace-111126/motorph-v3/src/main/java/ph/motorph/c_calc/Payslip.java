package ph.motorph.c_calc;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import ph.motorph.o_oop.Employee;

public final class Payslip {
    
    public static final class Line {
        public final String label;
        public final long cents;
        public final boolean emphasis;

        public Line(String label, long cents) { this(label, cents, false); }
        public Line(String label, long cents, boolean emphasis) {
            this.label = label; this.cents = cents; this.emphasis = emphasis;
        }
    }

    public final String number;              
    public final Employee employee;
    public final YearMonth month;
    public final int cutoff;                 
    public final LocalDate periodStart, periodEnd, payDate;

    public final List<Line> earnings = new ArrayList<>();
    public final List<Line> benefits = new ArrayList<>();
    public final List<Line> deductions = new ArrayList<>();

    
    public final List<Line> info = new ArrayList<>();

    public long grossIncomeCents, benefitsCents, deductionsCents, netCents;
    public int daysWorked;
    
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

    
    public String periodHeading() {
        return periodStart.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
            + " " + periodStart.getDayOfMonth() + "–" + periodEnd.getDayOfMonth() + ", " + periodEnd.getYear();
    }

    
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

    
    public long netFromLines() { return earningsTotal() + benefitsTotal() - deductionsTotal(); }
}
