package ph.motorph.o_oop;

import ph.motorph.c_calc.PayrollPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Payroll profile, revised 2026-11-05 from the mentor's answers:
 * - hourly rate = monthly basic / 168 (reproduces MotorPH's employee file, e.g. 90,000 -> 535.71)
 * - paid time = min(worked, 8 h); worked time beyond 8 h is unapproved overage, shown but not paid
 * - log-in from 08:00 to 08:10 counts as 08:00; the 12:00-13:00 meal hour is not worked time
 * Cutoffs stay 1-15 and 16-month-end. Monthly basics are placeholders for the fictional DEMO employees.
 */
public final class PayrollProfile {
    public static final String VERSION = "TEMPORARY-2026-mentor-v2";
    static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    /**
     * Pay inputs come from MotorPH's employee file, through {@link EmpRecords}. Nothing is hard-coded here any more:
     * a rate exists because the file states it, and a person with no row in the file has no rate.
     */
    private static volatile EmpRecords records;

    /** Binds the imported roster. Called once at start-up, before any payroll work. */
    public static void bind(EmpRecords roster) {
        if (roster == null) throw new IllegalArgumentException("roster must not be null");
        records = roster;
    }

    private static EmpRecords records() {
        EmpRecords current = records;
        if (current != null) return current;
        // Lazy fallback so a unit test that touches a single helper does not have to wire the whole application.
        synchronized (PayrollProfile.class) {
            if (records == null) records = EmpRecords.fromClasspath();
            return records;
        }
    }

    private PayrollProfile() { }

    /** {rice, phone, clothing} per month in centavos, from the employee file. Non-taxable, paid half per cutoff. */
    public static long[] monthlyAllowances(String employeeId) {
        Optional<Employee> employee = records().find(employeeId);
        if (!employee.isPresent()) return new long[3];
        return new long[] {
            cents(employee.get().getRiceSubsidy()),
            cents(employee.get().getPhoneAllowance()),
            cents(employee.get().getClothingAllowance())
        };
    }

    private static long cents(BigDecimal amount) {
        return amount == null ? 0 : amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    /** How net pay is released. Direct deposit for everyone; the employee file carries no payment method. */
    public static String payMethod(String employeeId) {
        return "Direct deposit";
    }

    /** The monthly basic the employee file states, or empty when the file has no row for this person. */
    public static Optional<BigDecimal> monthlyBasic(String employeeId) {
        return records().find(employeeId).map(Employee::getMonthlyBasic);
    }

    /**
     * Daily rate = monthly basic / 20, which is the divisor MotorPH's own sample payslip states: it prints
     * "Monthly Salary 53,500.00 / Daily Rate 2,675.00", and 53,500 / 20 = 2,675 exactly. Twenty is the working-day
     * divisor MotorPH's payroll office uses (the semi-monthly rate of 26,750 is ten days of 2,675, not eleven).
     */
    public static Optional<BigDecimal> dailyRate(String employeeId) {
        return monthlyBasic(employeeId).map(basic -> basic.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP));
    }

    /** Monthly basic / 168, rounded half-up to the centavo like the employee file. */
    public static Optional<BigDecimal> hourlyRate(String employeeId) {
        return monthlyBasic(employeeId).map(basic -> basic.divide(PayrollPolicy.hoursPerMonth(), 2, RoundingMode.HALF_UP));
    }

    /** Worked minutes for one raw attendance row: grace applied, meal hour removed, no clock cap. */
    public static long workedMinutes(LocalTime timeIn, LocalTime timeOut) {
        LocalTime start = !timeIn.isBefore(SHIFT_START) && !timeIn.isAfter(SHIFT_START.plusMinutes(PayrollPolicy.graceMinutes())) ? SHIFT_START : timeIn;
        if (!timeOut.isAfter(start)) return 0;
        long minutes = Duration.between(start, timeOut).toMinutes();
        LocalTime lunchFrom = start.isAfter(LUNCH_START) ? start : LUNCH_START;
        LocalTime lunchTo = timeOut.isBefore(LUNCH_END) ? timeOut : LUNCH_END;
        if (lunchTo.isAfter(lunchFrom)) minutes -= Duration.between(lunchFrom, lunchTo).toMinutes();
        return minutes;
    }

    /** Indexes into splitDay's result. */
    public static final int REGULAR = 0, REGULAR_NIGHT = 1, OVERTIME = 2, OVERTIME_NIGHT = 3, UNPAID = 4;
    static final LocalTime NIGHT_END = LocalTime.of(6, 0);
    static final LocalTime NIGHT_START = LocalTime.of(22, 0);

    /**
     * Walks one attendance row minute by minute (grace applied, meal hour skipped). The first 8 worked hours are
     * regular; later minutes are overtime up to the approved minutes, and the rest is unpaid overage. Night minutes
     * (22:00-06:00) are counted inside the regular and overtime totals.
     */
    public static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes) {
        return splitDay(timeIn, timeOut, approvedOvertimeMinutes, WorkSchedule.DEFAULT);
    }

    /** Same, with the person's schedule: grace from its shift start, meal hour 4 hours in. */
    public static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes, WorkSchedule schedule) {
        long[] result = new long[5];
        LocalTime start = !timeIn.isBefore(schedule.shiftStart) && !timeIn.isAfter(schedule.graceEnd()) ? schedule.shiftStart : timeIn;
        LocalTime mealStart = schedule.mealStart(), mealEnd = schedule.mealEnd();
        long worked = 0, daily = PayrollPolicy.dailyMinutes();
        for (LocalTime t = start; t.isBefore(timeOut); t = t.plusMinutes(1)) {
            if (!t.isBefore(mealStart) && t.isBefore(mealEnd)) continue;
            worked++;
            boolean night = t.isBefore(NIGHT_END) || !t.isBefore(NIGHT_START);
            if (worked <= daily) {
                result[REGULAR]++;
                if (night) result[REGULAR_NIGHT]++;
            } else if (worked <= daily + approvedOvertimeMinutes) {
                result[OVERTIME]++;
                if (night) result[OVERTIME_NIGHT]++;
            } else {
                result[UNPAID]++;
            }
            if (t.equals(LocalTime.MAX.withSecond(0).withNano(0))) break; // 23:59 would wrap to 00:00
        }
        return result;
    }

    /** Paid minutes = min(worked, scheduled 8 h); time beyond that is paid only through an approved overtime request. */
    public static long paidMinutes(LocalTime timeIn, LocalTime timeOut) {
        return Math.min(workedMinutes(timeIn, timeOut), PayrollPolicy.dailyMinutes());
    }
}
