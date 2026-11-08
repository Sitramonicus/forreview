package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * One raw timecard row, shaped by the supplied MotorPH timecard: time in, break out,
 * break in, time out, and the employer's own "Total Hours Worked".
 *
 * Break times are optional so a row without a recorded meal break is still representable,
 * but the worked total is always derived from the punches. Overnight rows are supported by
 * passing an explicit dayOffset for the out-punches (see {@link WorkedTime}).
 */
public final class AttendanceRecord {
    private final int attendanceId;
    private final int scheduleId;
    private final LocalDate date;
    private final LocalTime timeIn;
    private final LocalTime breakOut;
    private final LocalTime breakIn;
    private final LocalTime timeOut;
    private final String dayType;
    private final BigDecimal hoursWorked;

    public AttendanceRecord(int attendanceId, int scheduleId, LocalDate date, LocalTime timeIn,
                            LocalTime breakOut, LocalTime breakIn, LocalTime timeOut, String dayType) {
        if (attendanceId <= 0) throw new IllegalArgumentException("attendanceId must be positive");
        if (scheduleId <= 0) throw new IllegalArgumentException("scheduleId must be positive");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.timeIn = Objects.requireNonNull(timeIn, "timeIn must not be null");
        this.timeOut = Objects.requireNonNull(timeOut, "timeOut must not be null");
        if (timeIn.isAfter(timeOut)) {
            throw new IllegalArgumentException("timeIn must be less than or equal to timeOut; "
                + "record overnight shifts with a day offset on the out-punches");
        }
        if (breakOut != null && breakIn != null) {
            if (breakOut.isBefore(timeIn) || breakIn.isBefore(breakOut) || breakIn.isAfter(timeOut)) {
                throw new IllegalArgumentException("break times must fall between time in and time out, in order");
            }
        } else if (breakOut != null || breakIn != null) {
            throw new IllegalArgumentException("record both break out and break in, or neither");
        }
        if (dayType == null || dayType.trim().isEmpty()) throw new IllegalArgumentException("dayType must not be blank");
        this.attendanceId = attendanceId;
        this.scheduleId = scheduleId;
        this.breakOut = breakOut;
        this.breakIn = breakIn;
        this.dayType = dayType.trim();
        this.hoursWorked = WorkedTime.hoursBetween(timeIn, breakOut, breakIn, timeOut);
    }

    /** Convenience for a row with no recorded meal break. */
    public AttendanceRecord(int attendanceId, int scheduleId, LocalDate date,
                            LocalTime timeIn, LocalTime timeOut, String dayType) {
        this(attendanceId, scheduleId, date, timeIn, null, null, timeOut, dayType);
    }

    public int getAttendanceId() { return attendanceId; }
    public int getScheduleId() { return scheduleId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTimeIn() { return timeIn; }
    public LocalTime getBreakOut() { return breakOut; }
    public LocalTime getBreakIn() { return breakIn; }
    public LocalTime getTimeOut() { return timeOut; }
    public String getDayType() { return dayType; }
    /** Paid hours for the row: elapsed time between punches, never including the meal break. */
    public BigDecimal getHoursWorked() { return hoursWorked; }
    public boolean hasBreak() { return breakOut != null && breakIn != null; }
}
