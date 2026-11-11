package ph.motorph.o_oop;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class InMemoryAttendanceStore implements MutableAttendanceStore {
    private final List<AttendanceRecord> records;

    
    public InMemoryAttendanceStore() {
        records = new ArrayList<>();
    }

    
    public static InMemoryAttendanceStore withSampleRows() {
        return new InMemoryAttendanceStore(true);
    }

    private InMemoryAttendanceStore(boolean seed) {
        List<AttendanceRecord> values = new ArrayList<>();
        values.add(record(5001, 101, "2026-10-01", "08:01", "17:00"));
        values.add(record(5002, 101, "2026-10-02", "07:58", "16:59"));
        values.add(record(5003, 101, "2026-10-15", "08:03", "17:01"));
        values.add(record(5101, 102, "2026-10-01", "08:00", "17:02"));
        values.add(record(5102, 102, "2026-10-03", "08:04", "17:03"));
        values.add(record(5201, 103, "2026-10-02", "08:10", "16:55"));
        values.add(record(5301, 104, "2026-10-05", "08:00", "17:00"));
        values.add(record(5302, 104, "2026-10-06", "08:06", "17:00"));
        records = values;
        enforceUniqueScheduleDate(records);
    }

    @Override
    public synchronized List<AttendanceRecord> findAll() {
        return new ArrayList<>(records);
    }

    @Override
    public synchronized List<AttendanceRecord> findByScheduleId(int scheduleId) {
        return records.stream()
            .filter(record -> record.getScheduleId() == scheduleId)
            .collect(Collectors.toList());
    }

    @Override
    public synchronized AttendanceRecord create(int scheduleId, LocalDate date, LocalTime timeIn,
                                                 LocalTime timeOut, String dayType) {
        for (AttendanceRecord existing : records) {
            if (existing.getScheduleId() == scheduleId && existing.getDate().equals(date)) {
                throw new IllegalStateException("An attendance record already exists for this schedule and date.");
            }
        }
        int nextId = records.stream().mapToInt(AttendanceRecord::getAttendanceId).max().orElse(0) + 1;
        if (nextId <= 0) throw new IllegalStateException("No attendance IDs remain.");
        AttendanceRecord record = new AttendanceRecord(nextId, scheduleId, date, timeIn, timeOut, dayType);
        records.add(record);
        return record;
    }

    @Override
    public synchronized AttendanceRecord correctTimes(int attendanceId, LocalTime timeIn, LocalTime timeOut) {
        for (int i = 0; i < records.size(); i++) {
            AttendanceRecord existing = records.get(i);
            if (existing.getAttendanceId() != attendanceId) continue;
            AttendanceRecord corrected = new AttendanceRecord(attendanceId, existing.getScheduleId(), existing.getDate(), timeIn, timeOut, existing.getDayType());
            records.set(i, corrected);
            return corrected;
        }
        throw new java.util.NoSuchElementException("Attendance row was not found.");
    }

    private static AttendanceRecord record(int attendanceId, int scheduleId, String date, String in, String out) {
        return new AttendanceRecord(attendanceId, scheduleId, LocalDate.parse(date),
            LocalTime.parse(in), LocalTime.parse(out), "Imported");
    }

    private static void enforceUniqueScheduleDate(List<AttendanceRecord> records) {
        Set<String> keys = new HashSet<>();
        for (AttendanceRecord record : records) {
            String key = record.getScheduleId() + ":" + record.getDate();
            if (!keys.add(key)) throw new IllegalArgumentException("duplicate attendance for schedule/date: " + key);
        }
    }
}
