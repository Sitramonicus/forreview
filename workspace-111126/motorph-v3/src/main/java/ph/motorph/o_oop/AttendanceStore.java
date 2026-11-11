package ph.motorph.o_oop;

import java.util.List;

public interface AttendanceStore {
    List<AttendanceRecord> findByScheduleId(int scheduleId);
    List<AttendanceRecord> findAll();
}
