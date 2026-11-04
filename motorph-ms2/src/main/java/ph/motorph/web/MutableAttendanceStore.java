package ph.motorph.web;

import java.time.LocalDate;
import java.time.LocalTime;

/** Write boundary for local synthetic attendance; Firebase stores remain read-only. */
public interface MutableAttendanceStore extends AttendanceStore {
    AttendanceRecord create(int scheduleId, LocalDate date, LocalTime timeIn, LocalTime timeOut, String dayType);
}
