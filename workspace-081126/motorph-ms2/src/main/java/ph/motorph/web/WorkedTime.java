package ph.motorph.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Time-to-hours conversion for one timecard row.
 *
 * The supplied MotorPH timecard is the reference: 08:00 AM, break out 12:00 PM, break in
 * 01:00 PM, out 05:00 PM is reported as 8 hrs. That is (out - in) minus (break in - break out):
 * nine hours elapsed less one unpaid meal hour. Standard time uses the same rule, which is why
 * an 08:00-17:00 row with no break recorded is 9 hours, not 8.
 */
public final class WorkedTime {
    private WorkedTime() { }

    public static BigDecimal hoursBetween(LocalTime in, LocalTime breakOut, LocalTime breakIn, LocalTime out) {
        long minutes = Duration.between(in, out).toMinutes();
        if (minutes < 0) throw new IllegalArgumentException("time out must not precede time in");
        if (breakOut != null && breakIn != null) {
            long breakMinutes = Duration.between(breakOut, breakIn).toMinutes();
            if (breakMinutes < 0) throw new IllegalArgumentException("break in must not precede break out");
            minutes -= breakMinutes;
        }
        if (minutes < 0) throw new IllegalArgumentException("the meal break is longer than the time at work");
        return BigDecimal.valueOf(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

    /** Whole minutes, for arithmetic that must not round twice. */
    public static long minutesBetween(LocalTime in, LocalTime breakOut, LocalTime breakIn, LocalTime out) {
        long minutes = Duration.between(in, out).toMinutes();
        if (breakOut != null && breakIn != null) minutes -= Duration.between(breakOut, breakIn).toMinutes();
        return minutes;
    }
}
