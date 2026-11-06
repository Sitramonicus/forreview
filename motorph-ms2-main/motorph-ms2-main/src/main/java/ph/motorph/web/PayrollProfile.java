package ph.motorph.web;

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
    private static final Map<String, BigDecimal> MONTHLY_BASIC;
    static {
        Map<String, BigDecimal> basics = new HashMap<>();
        basics.put("DEMO-001", new BigDecimal("52670.00"));
        basics.put("DEMO-002", new BigDecimal("60000.00"));
        basics.put("DEMO-003", new BigDecimal("90000.00"));
        basics.put("DEMO-004", new BigDecimal("25000.00"));
        basics.put("DEMO-005", new BigDecimal("75000.00"));
        MONTHLY_BASIC = Collections.unmodifiableMap(basics);
    }

    /** Monthly rice / phone / clothing allowances in centavos (placeholders shaped like the MotorPH employee file). */
    private static final Map<String, long[]> ALLOWANCES;
    static {
        Map<String, long[]> allowances = new HashMap<>();
        allowances.put("DEMO-001", new long[] {150_000, 100_000, 100_000});
        allowances.put("DEMO-002", new long[] {150_000, 100_000, 100_000});
        allowances.put("DEMO-003", new long[] {150_000, 200_000, 100_000});
        allowances.put("DEMO-004", new long[] {150_000, 50_000, 50_000});
        allowances.put("DEMO-005", new long[] {150_000, 200_000, 100_000});
        ALLOWANCES = Collections.unmodifiableMap(allowances);
    }

    private PayrollProfile() { }

    /** {rice, phone, clothing} per month; zeros for people without a profile. Non-taxable, paid half per cutoff. */
    public static long[] monthlyAllowances(String employeeId) {
        long[] values = ALLOWANCES.get(employeeId);
        return values == null ? new long[3] : values.clone();
    }

    /** How net pay is released (synthetic): Jae is paid by check, everyone else by direct deposit. */
    public static String payMethod(String employeeId) {
        return "DEMO-004".equals(employeeId) ? "Check" : "Direct deposit";
    }

    public static Optional<BigDecimal> monthlyBasic(String employeeId) {
        return Optional.ofNullable(MONTHLY_BASIC.get(employeeId));
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
    static final int REGULAR = 0, REGULAR_NIGHT = 1, OVERTIME = 2, OVERTIME_NIGHT = 3, UNPAID = 4;
    static final LocalTime NIGHT_END = LocalTime.of(6, 0);
    static final LocalTime NIGHT_START = LocalTime.of(22, 0);

    /**
     * Walks one attendance row minute by minute (grace applied, meal hour skipped). The first 8 worked hours are
     * regular; later minutes are overtime up to the approved minutes, and the rest is unpaid overage. Night minutes
     * (22:00-06:00) are counted inside the regular and overtime totals.
     */
    static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes) {
        return splitDay(timeIn, timeOut, approvedOvertimeMinutes, WorkSchedule.DEFAULT);
    }

    /** Same, with the person's schedule: grace from its shift start, meal hour 4 hours in. */
    static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes, WorkSchedule schedule) {
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
