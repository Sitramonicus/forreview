package ph.motorph.web;

import java.util.List;

/** Storage boundary for raw attendance entries. */
public interface AttendanceStore {
    List<AttendanceRecord> findByScheduleId(int scheduleId);
    List<AttendanceRecord> findAll();
}
