package motorph.model;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * One day of attendance for one employee (one row of attendance.csv).
 * <p>
 * The CP2 time rules are hidden inside this class: logins from 8:00 to 8:10
 * count as 8:00 (grace period), and logouts after 5:00 PM count as 5:00 PM
 * (cap). Callers only ask for hours or minutes worked.
 * </p>
 */
public class AttendanceRecord {

    // === TIME RULES (from CP2 computeHoursWorked) ===
    private static final LocalTime WORK_START   = LocalTime.of(8, 0);
    private static final LocalTime GRACE_END    = LocalTime.of(8, 10);
    private static final LocalTime LOGOUT_CAP   = LocalTime.of(17, 0);

    // === CUTOFF RULES (days 1-15 = 1st cutoff, 16-month end = 2nd cutoff) ===
    private static final int FIRST_CUTOFF_END_DAY    = 15;
    private static final int SECOND_CUTOFF_START_DAY = 16;

    private final String employeeId;
    private final LocalDate date;
    private final LocalTime logIn;
    private LocalTime logOut;

    /**
     * Creates an attendance record. Throws IllegalArgumentException if a value
     * is missing or the logout is not after the login.
     */
    public AttendanceRecord(String employeeId, LocalDate date, LocalTime logIn, LocalTime logOut) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        if (date == null) {
            throw new IllegalArgumentException("Date is required.");
        }
        if (logIn == null) {
            throw new IllegalArgumentException("Log in time is required.");
        }
        this.employeeId = employeeId.trim();
        this.date = date;
        this.logIn = logIn;
        setLogOut(logOut);
    }

    // === GETTERS ===

    public String getEmployeeId() { return employeeId; }
    public LocalDate getDate()    { return date; }
    public LocalTime getLogIn()   { return logIn; }
    public LocalTime getLogOut()  { return logOut; }

    /** Updates the logout time. Must be after the login time. */
    public final void setLogOut(LocalTime logOut) {
        if (logOut == null) {
            throw new IllegalArgumentException("Log out time is required.");
        }
        if (!logOut.isAfter(logIn)) {
            throw new IllegalArgumentException("Log out time must be after log in time.");
        }
        this.logOut = logOut;
    }

    // === HOURS ===

    /**
     * Returns the minutes worked after applying the grace period and the
     * logout cap. Never negative. Whole minutes keep payroll math exact.
     */
    public long computeMinutesWorked() {
        long minutes = Duration.between(applyGracePeriod(logIn), applyLogoutCap(logOut)).toMinutes();
        return Math.max(0, minutes);
    }

    /** Returns the hours worked for the day after the time rules. Never negative. */
    public double computeHoursWorked() {
        return computeMinutesWorked() / 60.0;
    }

    /** Treats a login from 8:00 to 8:10 as 8:00. */
    private LocalTime applyGracePeriod(LocalTime time) {
        if (!time.isBefore(WORK_START) && !time.isAfter(GRACE_END)) {
            return WORK_START;
        }
        return time;
    }

    /** Caps a logout after 5:00 PM at 5:00 PM. */
    private LocalTime applyLogoutCap(LocalTime time) {
        return time.isAfter(LOGOUT_CAP) ? LOGOUT_CAP : time;
    }

    // === CUTOFF ===

    /**
     * Returns the payroll cutoff label for this record's date, for example
     * "June 1-15, 2024" or "June 16-30, 2024". Uses the real month length, so
     * February gives "16-29" in leap years and "16-28" otherwise.
     */
    public String getCutoffLabel() {
        String month = date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        if (date.getDayOfMonth() <= FIRST_CUTOFF_END_DAY) {
            return month + " 1-" + FIRST_CUTOFF_END_DAY + ", " + date.getYear();
        }
        return month + " " + SECOND_CUTOFF_START_DAY + "-" + date.lengthOfMonth() + ", " + date.getYear();
    }
}
