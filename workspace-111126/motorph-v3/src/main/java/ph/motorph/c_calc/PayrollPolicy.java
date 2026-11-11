package ph.motorph.c_calc;

import java.math.BigDecimal;

public final class PayrollPolicy {
    public static final int DEFAULT_GRACE_MINUTES = 10, DEFAULT_HOURS_PER_MONTH = 168, DEFAULT_DAILY_HOURS = 8, DEFAULT_NIGHT_PERCENT = 10;

    
    public static final class Values {
        public int graceMinutes = DEFAULT_GRACE_MINUTES, hoursPerMonth = DEFAULT_HOURS_PER_MONTH, dailyHours = DEFAULT_DAILY_HOURS,
            nightDifferentialPercent = DEFAULT_NIGHT_PERCENT;
        public String updatedBy = "", updatedAt = "";
    }

    private static volatile Values current = new Values();

    private PayrollPolicy() { }

    public static int graceMinutes() { return current.graceMinutes; }

    public static BigDecimal hoursPerMonth() { return BigDecimal.valueOf(current.hoursPerMonth); }

    
    public static long dailyMinutes() { return current.dailyHours * 60L; }

    public static BigDecimal nightDifferential() { return BigDecimal.valueOf(current.nightDifferentialPercent).movePointLeft(2); }

    public static Values values() {
        Values copy = new Values();
        Values now = current;
        copy.graceMinutes = now.graceMinutes;
        copy.hoursPerMonth = now.hoursPerMonth;
        copy.dailyHours = now.dailyHours;
        copy.nightDifferentialPercent = now.nightDifferentialPercent;
        copy.updatedBy = now.updatedBy;
        copy.updatedAt = now.updatedAt;
        return copy;
    }

    public static boolean isDefault() {
        Values now = current;
        return now.graceMinutes == DEFAULT_GRACE_MINUTES && now.hoursPerMonth == DEFAULT_HOURS_PER_MONTH
            && now.dailyHours == DEFAULT_DAILY_HOURS && now.nightDifferentialPercent == DEFAULT_NIGHT_PERCENT;
    }

    public static void update(int graceMinutes, int hoursPerMonth, int dailyHours, int nightPercent, String by, String at) {
        range(graceMinutes, 0, 30, "Grace minutes");
        range(hoursPerMonth, 160, 208, "Hours per month");
        range(dailyHours, 4, 10, "Paid hours a day");
        range(nightPercent, 10, 30, "Night differential %");
        Values next = new Values();
        next.graceMinutes = graceMinutes;
        next.hoursPerMonth = hoursPerMonth;
        next.dailyHours = dailyHours;
        next.nightDifferentialPercent = nightPercent;
        next.updatedBy = by;
        next.updatedAt = at;
        current = next;
    }

    
    public static void importValues(Values saved) {
        if (saved == null) {
            current = new Values();
            return;
        }
        update(saved.graceMinutes, saved.hoursPerMonth, saved.dailyHours, saved.nightDifferentialPercent, saved.updatedBy, saved.updatedAt);
    }

    private static void range(int value, int min, int max, String label) {
        if (value < min || value > max) throw new IllegalArgumentException(label + " must be " + min + " to " + max + ".");
    }
}
