package ph.motorph.o_oop;

import java.time.LocalDate;
import java.time.LocalTime;

public interface MutableAttendanceStore extends AttendanceStore {
    AttendanceRecord create(int scheduleId, LocalDate date, LocalTime timeIn, LocalTime timeOut, String dayType);

    
    AttendanceRecord correctTimes(int attendanceId, LocalTime timeIn, LocalTime timeOut);
}
