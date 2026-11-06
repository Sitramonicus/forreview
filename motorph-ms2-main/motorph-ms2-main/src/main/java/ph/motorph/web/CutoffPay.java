package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Attendance-, leave- and overtime-derived earnings for one cutoff. Deductions are applied separately (MonthPayroll). */
public final class CutoffPay {
    private final String label;
    private final LocalDate from;
    private final LocalDate to;
    private final int daysWithRecords;
    private final long paidMinutes;
    private final long unpaidOverageMinutes;
    private final long overtimeMinutes;
    private final long nightDiffMinutes;
    private final int holidayDays;
    private final int leaveDays;
    private final BigDecimal basicPay;
    private final BigDecimal premiumPay;
    private final BigDecimal overtimePay;
    private final BigDecimal nightDiffPay;
    private final BigDecimal holidayPay;
    private final BigDecimal leavePay;

    /** Gross only (all of it basic pay); used by tests of the deduction split. */
    public CutoffPay(String label, LocalDate from, LocalDate to, int daysWithRecords, long paidMinutes, BigDecimal grossPay) {
        this(label, from, to, daysWithRecords, paidMinutes, 0, 0, 0, 0, 0,
            grossPay, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public CutoffPay(String label, LocalDate from, LocalDate to, int daysWithRecords, long paidMinutes, long unpaidOverageMinutes,
            long overtimeMinutes, long nightDiffMinutes, int holidayDays, int leaveDays, BigDecimal basicPay, BigDecimal premiumPay,
            BigDecimal overtimePay, BigDecimal nightDiffPay, BigDecimal holidayPay, BigDecimal leavePay) {
        this.label = label;
        this.from = from;
        this.to = to;
        this.daysWithRecords = daysWithRecords;
        this.paidMinutes = paidMinutes;
        this.unpaidOverageMinutes = unpaidOverageMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.nightDiffMinutes = nightDiffMinutes;
        this.holidayDays = holidayDays;
        this.leaveDays = leaveDays;
        this.basicPay = basicPay;
        this.premiumPay = premiumPay;
        this.overtimePay = overtimePay;
        this.nightDiffPay = nightDiffPay;
        this.holidayPay = holidayPay;
        this.leavePay = leavePay;
    }

    public String getLabel() { return label; }
    public LocalDate getFrom() { return from; }
    public LocalDate getTo() { return to; }
    public int getDaysWithRecords() { return daysWithRecords; }
    /** Regular worked minutes (at most 8 h a day); leave and unworked holidays are never counted here. */
    public long getPaidMinutes() { return paidMinutes; }
    /** Worked minutes beyond 8 h a day with no approved overtime: shown, not paid. */
    public long getUnpaidOverageMinutes() { return unpaidOverageMinutes; }
    public long getOvertimeMinutes() { return overtimeMinutes; }
    public long getNightDiffMinutes() { return nightDiffMinutes; }
    public int getHolidayDays() { return holidayDays; }
    public int getLeaveDays() { return leaveDays; }
    public BigDecimal getBasicPay() { return basicPay; }
    public BigDecimal getPremiumPay() { return premiumPay; }
    public BigDecimal getOvertimePay() { return overtimePay; }
    public BigDecimal getNightDiffPay() { return nightDiffPay; }
    public BigDecimal getHolidayPay() { return holidayPay; }
    public BigDecimal getLeavePay() { return leavePay; }

    public BigDecimal getGrossPay() {
        return basicPay.add(premiumPay).add(overtimePay).add(nightDiffPay).add(holidayPay).add(leavePay);
    }
}
