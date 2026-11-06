package ph.motorph.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Raw attendance data model aligned to the supplied MO-IT107 Attendance fields. */
public final class AttendanceRecord {
    private final int attendanceId;
    private final int scheduleId;
    private final LocalDate date;
    private final LocalTime timeIn;
    private final LocalTime timeOut;
    private final String dayType;

    public AttendanceRecord(int attendanceId, int scheduleId, LocalDate date,
                            LocalTime timeIn, LocalTime timeOut, String dayType) {
        if (attendanceId <= 0) throw new IllegalArgumentException("attendanceId must be positive");
        if (scheduleId <= 0) throw new IllegalArgumentException("scheduleId must be positive");
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.timeIn = Objects.requireNonNull(timeIn, "timeIn must not be null");
        this.timeOut = Objects.requireNonNull(timeOut, "timeOut must not be null");
        if (timeIn.isAfter(timeOut)) {
            throw new IllegalArgumentException("timeIn must be less than or equal to timeOut per the supplied data dictionary");
        }
        if (dayType == null || dayType.trim().isEmpty()) throw new IllegalArgumentException("dayType must not be blank");
        this.attendanceId = attendanceId;
        this.scheduleId = scheduleId;
        this.dayType = dayType.trim();
    }

    public int getAttendanceId() { return attendanceId; }
    public int getScheduleId() { return scheduleId; }
    public LocalDate getDate() { return date; }
    public LocalTime getTimeIn() { return timeIn; }
    public LocalTime getTimeOut() { return timeOut; }
    public String getDayType() { return dayType; }
}
