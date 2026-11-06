package ph.motorph.web;

import java.time.LocalDate;
import java.time.LocalTime;

/** Write boundary for local synthetic attendance; Firebase stores remain read-only. */
public interface MutableAttendanceStore extends AttendanceStore {
    AttendanceRecord create(int scheduleId, LocalDate date, LocalTime timeIn, LocalTime timeOut, String dayType);

    /** Corrects a row's times (same ID, schedule and date); returns the corrected row. */
    AttendanceRecord correctTimes(int attendanceId, LocalTime timeIn, LocalTime timeOut);
}
