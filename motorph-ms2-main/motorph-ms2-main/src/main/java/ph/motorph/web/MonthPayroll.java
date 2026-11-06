package ph.motorph.web;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One employee's month, gross to net, per cutoff:
 *   earned pay (attendance, premiums, OT, night diff, holiday and leave pay) + taxable bonuses = gross
 *   − statutory (SSS, PhilHealth, Pag-IBIG on earned pay; tax on earned pay + bonuses), each line half per cutoff
 *   − voluntary deductions (loans, insurance...), taken in order, never below a zero net
 *   + allowances (rice, phone, clothing; non-taxable, half per cutoff)
 *   = net.
 * Statutory amounts follow sweldongpinoy.com on the monthly totals; the odd centavo of any split goes to the 2nd cutoff.
 */
public final class MonthPayroll {
    public static final int SSS = 0, PHILHEALTH = 1, PAGIBIG = 2, TAX = 3;
    public static final int RICE = 0, PHONE = 1, CLOTHING = 2;

    /** A voluntary deduction as applied to one cutoff. */
    public static final class Taken {
        public final PayrollAdjustments.Deduction item;
        public final long takenCents;
        public final long shortfallCents;

        Taken(PayrollAdjustments.Deduction item, long takenCents) {
            this.item = item;
            this.takenCents = takenCents;
            this.shortfallCents = item.perCutoffCents - takenCents;
        }
    }

    private final String employeeId;
    private final YearMonth month;
    private final BigDecimal hourlyRate;
    private final List<CutoffPay> cutoffs;
    private final long monthlyGrossCents;
    private final StatutoryBreakdown statutory;
    private final long[][] lines = new long[2][4];
    private final long[][] employerLines = new long[2][3];
    private final long[][] allowances = new long[2][3];
    private final long[] bonusCents = new long[2];
    private final List<List<PayrollAdjustments.Earning>> bonuses = new ArrayList<>();
    private final List<List<PayrollAdjustments.Earning>> allowanceCorrections = new ArrayList<>();
    private final long[] allowanceCorrectionCents = new long[2];
    private final List<List<Taken>> voluntary = new ArrayList<>();

    MonthPayroll(String employeeId, YearMonth month, BigDecimal hourlyRate, List<CutoffPay> cutoffs) {
        this(employeeId, month, hourlyRate, cutoffs, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), new long[3]);
    }

    MonthPayroll(String employeeId, YearMonth month, BigDecimal hourlyRate, List<CutoffPay> cutoffs,
            List<PayrollAdjustments.Earning> firstBonuses, List<PayrollAdjustments.Earning> secondBonuses,
            List<PayrollAdjustments.Deduction> voluntaryDeductions, long[] monthlyAllowanceCents) {
        this(employeeId, month, hourlyRate, cutoffs, firstBonuses, secondBonuses, voluntaryDeductions, monthlyAllowanceCents, StatutoryRates.DEFAULT);
    }

    MonthPayroll(String employeeId, YearMonth month, BigDecimal hourlyRate, List<CutoffPay> cutoffs,
            List<PayrollAdjustments.Earning> firstBonuses, List<PayrollAdjustments.Earning> secondBonuses,
            List<PayrollAdjustments.Deduction> voluntaryDeductions, long[] monthlyAllowanceCents, StatutoryRates rates) {
        if (cutoffs.size() != 2) throw new IllegalArgumentException("A month has exactly two cutoffs.");
        this.employeeId = employeeId;
        this.month = month;
        this.hourlyRate = hourlyRate;
        this.cutoffs = Collections.unmodifiableList(cutoffs);
        // Taxable earnings are bonuses (in gross); allowance corrections are non-taxable and paid on top of net.
        for (List<PayrollAdjustments.Earning> items : java.util.Arrays.asList(firstBonuses, secondBonuses)) {
            List<PayrollAdjustments.Earning> taxable = new ArrayList<>(), allowance = new ArrayList<>();
            for (PayrollAdjustments.Earning item : items) (PayrollAdjustments.isTaxable(item.type) ? taxable : allowance).add(item);
            bonuses.add(Collections.unmodifiableList(taxable));
            allowanceCorrections.add(Collections.unmodifiableList(allowance));
        }
        for (int c = 0; c < 2; c++) {
            for (PayrollAdjustments.Earning item : bonuses.get(c)) bonusCents[c] += item.amountCents;
            for (PayrollAdjustments.Earning item : allowanceCorrections.get(c)) allowanceCorrectionCents[c] += item.amountCents;
        }
        long earned = earnedCents(0) + earnedCents(1);
        long bonus = bonusCents[0] + bonusCents[1];
        this.monthlyGrossCents = earned + bonus;
        // shortcut: a month with bonuses but no earned pay has no contribution base, so nothing is withheld; rare in the demo.
        this.statutory = earned > 0
            ? StatutoryCalculator2026.calculate(month, earned + bonus, earned, earned, earned, earned + bonus, rates)
            : null;
        if (statutory != null) {
            split(lines, SSS, statutory.sssEmployeeCents);
            split(lines, PHILHEALTH, statutory.philHealthEmployeeCents);
            split(lines, PAGIBIG, statutory.pagIbigEmployeeCents);
            split(lines, TAX, statutory.withholdingTaxCents);
            split(employerLines, SSS, statutory.sssEmployerTotalCents());
            split(employerLines, PHILHEALTH, statutory.philHealthEmployerCents);
            split(employerLines, PAGIBIG, statutory.pagIbigEmployerCents);
        }
        for (int c = 0; c < 2; c++) {
            // shortcut: allowances are paid only in a cutoff with earned pay (no pay for a cutoff fully absent); confirm with the mentor.
            if (earnedCents(c) > 0) {
                for (int a = 0; a < 3; a++) allowances[c][a] = c == 0 ? monthlyAllowanceCents[a] / 2 : monthlyAllowanceCents[a] - monthlyAllowanceCents[a] / 2;
            }
            List<Taken> taken = new ArrayList<>();
            long room = Math.max(0, grossCents(c) - statutoryCents(c));
            for (PayrollAdjustments.Deduction item : voluntaryDeductions) {
                if (item.onlyCutoff != 0 && item.onlyCutoff != c + 1) continue;
                long amount = Math.min(item.perCutoffCents, room);
                taken.add(new Taken(item, amount));
                room -= amount;
            }
            voluntary.add(Collections.unmodifiableList(taken));
        }
    }

    private static void split(long[][] target, int line, long monthlyCents) {
        target[0][line] = monthlyCents / 2;
        target[1][line] = monthlyCents - target[0][line];
    }

    private static long cents(BigDecimal pesos) { return pesos.movePointRight(2).longValueExact(); }

    public String getEmployeeId() { return employeeId; }
    public YearMonth getMonth() { return month; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public List<CutoffPay> getCutoffs() { return cutoffs; }
    /** Earned pay + bonuses for the month (allowances excluded: they are not part of gross). */
    public long getMonthlyGrossCents() { return monthlyGrossCents; }
    /** Null when the month has no earned pay. */
    public StatutoryBreakdown getStatutory() { return statutory; }

    /** cutoff is 0 (1-15) or 1 (16-end); line is SSS, PHILHEALTH, PAGIBIG or TAX. */
    public long lineCents(int cutoff, int line) { return lines[cutoff][line]; }
    /** Employer share for SSS (incl. EC), PHILHEALTH or PAGIBIG, half per cutoff. */
    public long employerCents(int cutoff, int line) { return employerLines[cutoff][line]; }
    public long allowanceCents(int cutoff, int allowance) { return allowances[cutoff][allowance]; }
    /** RICE, PHONE or CLOTHING for this cutoff including its corrections (never below 0). */
    public long allowanceWithCorrections(int cutoff, int allowance, String correctionType) {
        long total = allowances[cutoff][allowance];
        for (PayrollAdjustments.Earning item : allowanceCorrections.get(cutoff)) if (item.type.equals(correctionType)) total += item.amountCents;
        return Math.max(0, total);
    }
    /** Monthly allowances (half) plus any non-taxable allowance corrections for this cutoff. */
    public long allowanceTotalCents(int cutoff) {
        // Subtracting corrections can lower the allowances, never below nothing.
        return Math.max(0, allowances[cutoff][0] + allowances[cutoff][1] + allowances[cutoff][2] + allowanceCorrectionCents[cutoff]);
    }
    public List<PayrollAdjustments.Earning> allowanceCorrections(int cutoff) { return allowanceCorrections.get(cutoff); }
    public long allowanceCorrectionCents(int cutoff) { return allowanceCorrectionCents[cutoff]; }
    public List<PayrollAdjustments.Earning> bonuses(int cutoff) { return bonuses.get(cutoff); }
    public long bonusCents(int cutoff) { return bonusCents[cutoff]; }
    public List<Taken> voluntary(int cutoff) { return voluntary.get(cutoff); }

    /** Attendance-derived pay (basic, premiums, overtime, night diff, holiday and leave pay). */
    public long earnedCents(int cutoff) { return cents(cutoffs.get(cutoff).getGrossPay()); }
    /** Gross = earned pay + taxable bonuses. */
    public long grossCents(int cutoff) { return earnedCents(cutoff) + bonusCents[cutoff]; }
    public long statutoryCents(int cutoff) {
        long total = 0;
        for (long line : lines[cutoff]) total += line;
        return total;
    }
    public long voluntaryCents(int cutoff) {
        long total = 0;
        for (Taken item : voluntary.get(cutoff)) total += item.takenCents;
        return total;
    }
    /** Statutory + voluntary deductions actually taken. */
    public long deductionsCents(int cutoff) { return statutoryCents(cutoff) + voluntaryCents(cutoff); }
    /** True when this cutoff's statutory share is larger than its gross (net would be negative). */
    public boolean isBlocked(int cutoff) { return statutoryCents(cutoff) > grossCents(cutoff); }
    public long netCents(int cutoff) {
        if (isBlocked(cutoff)) throw new IllegalStateException("Deductions exceed this cutoff's gross.");
        return grossCents(cutoff) - deductionsCents(cutoff) + allowanceTotalCents(cutoff);
    }
}
