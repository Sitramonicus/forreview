package ph.motorph.o_oop;

import ph.motorph.c_calc.PayrollPolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

public final class PayrollProfile {
    public static final String VERSION = "TEMPORARY-2026-mentor-v2";
    static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    
    private static volatile EmpRecords records;

    
    public static void bind(EmpRecords roster) {
        if (roster == null) throw new IllegalArgumentException("roster must not be null");
        records = roster;
    }

    private static EmpRecords records() {
        EmpRecords current = records;
        if (current != null) return current;
        
        synchronized (PayrollProfile.class) {
            if (records == null) records = EmpRecords.fromClasspath();
            return records;
        }
    }

    private PayrollProfile() { }

    
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

    
    public static String payMethod(String employeeId) {
        return "Direct deposit";
    }

    
    public static Optional<BigDecimal> monthlyBasic(String employeeId) {
        return records().find(employeeId).map(Employee::getMonthlyBasic);
    }

    
    public static Optional<BigDecimal> dailyRate(String employeeId) {
        return monthlyBasic(employeeId).map(basic -> basic.divide(BigDecimal.valueOf(20), 2, RoundingMode.HALF_UP));
    }

    
    public static Optional<BigDecimal> hourlyRate(String employeeId) {
        return monthlyBasic(employeeId).map(basic -> basic.divide(PayrollPolicy.hoursPerMonth(), 2, RoundingMode.HALF_UP));
    }

    
    public static long workedMinutes(LocalTime timeIn, LocalTime timeOut) {
        LocalTime start = !timeIn.isBefore(SHIFT_START) && !timeIn.isAfter(SHIFT_START.plusMinutes(PayrollPolicy.graceMinutes())) ? SHIFT_START : timeIn;
        if (!timeOut.isAfter(start)) return 0;
        long minutes = Duration.between(start, timeOut).toMinutes();
        LocalTime lunchFrom = start.isAfter(LUNCH_START) ? start : LUNCH_START;
        LocalTime lunchTo = timeOut.isBefore(LUNCH_END) ? timeOut : LUNCH_END;
        if (lunchTo.isAfter(lunchFrom)) minutes -= Duration.between(lunchFrom, lunchTo).toMinutes();
        return minutes;
    }

    
    public static final int REGULAR = 0, REGULAR_NIGHT = 1, OVERTIME = 2, OVERTIME_NIGHT = 3, UNPAID = 4;
    static final LocalTime NIGHT_END = LocalTime.of(6, 0);
    static final LocalTime NIGHT_START = LocalTime.of(22, 0);

    
    public static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes) {
        return splitDay(timeIn, timeOut, approvedOvertimeMinutes, WorkSchedule.DEFAULT);
    }

    
    public static long[] splitDay(LocalTime timeIn, LocalTime timeOut, int approvedOvertimeMinutes, WorkSchedule schedule) {
        long[] result = new long[5];
        LocalTime start = !timeIn.isBefore(schedule.ws_tShiftStart) && !timeIn.isAfter(schedule.graceEnd()) ? schedule.ws_tShiftStart : timeIn;
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
            if (t.equals(LocalTime.MAX.withSecond(0).withNano(0))) break; 
        }
        return result;
    }

    
    public static long paidMinutes(LocalTime timeIn, LocalTime timeOut) {
        return Math.min(workedMinutes(timeIn, timeOut), PayrollPolicy.dailyMinutes());
    }
}
