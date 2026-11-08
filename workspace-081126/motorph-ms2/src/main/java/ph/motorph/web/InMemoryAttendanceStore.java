package ph.motorph.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Temporary raw attendance fixture. All entries and classifications are synthetic. */
public final class InMemoryAttendanceStore implements AttendanceStore {
    private final List<AttendanceRecord> records;

    public InMemoryAttendanceStore() {
        List<AttendanceRecord> values = new ArrayList<>();
        values.add(record(5001, 101, "2026-10-01", "08:01", "12:00", "13:00", "17:00"));
        values.add(record(5002, 101, "2026-10-02", "07:58", "12:00", "13:00", "16:59"));
        values.add(record(5003, 101, "2026-10-15", "08:03", "12:00", "13:00", "17:01"));
        values.add(record(5101, 102, "2026-10-01", "08:00", "12:00", "13:00", "17:02"));
        values.add(record(5102, 102, "2026-10-03", "08:04", "12:00", "13:00", "17:03"));
        values.add(record(5201, 103, "2026-10-02", "08:10", "12:00", "13:00", "16:55"));
        // December 2023 mirrors the supplied MotorPH timecard row-for-row: eleven working days
        // of 08:00-12:00 / 13:00-17:00, with weekends and holidays recorded as zero-hour rows.
        // This schedule belongs to employee 10015 (Romualdez, Fredrick) in MotorPH's own file.
        String[] workingDays = {"01", "04", "05", "06", "07", "11", "12", "13", "14", "15",
            "18", "19", "20", "21", "22", "26", "27", "28", "29"};
        int id = 6001;
        for (String day : workingDays) {
            values.add(record(id++, 104, "2023-12-" + day, "08:00", "12:00", "13:00", "17:00"));
        }
        values.add(record(id++, 104, "2023-12-08", "00:00", "00:00", "00:00", "00:00"));
        values.add(record(id++, 104, "2023-12-25", "00:00", "00:00", "00:00", "00:00"));
        values.add(record(id, 104, "2023-12-30", "00:00", "00:00", "00:00", "00:00"));
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

    private static AttendanceRecord record(int attendanceId, int scheduleId, String date,
                                           String in, String breakOut, String breakIn, String out) {
        return new AttendanceRecord(attendanceId, scheduleId, LocalDate.parse(date),
            LocalTime.parse(in), LocalTime.parse(breakOut), LocalTime.parse(breakIn),
            LocalTime.parse(out), "DEMO");
    }

    private static void enforceUniqueScheduleDate(List<AttendanceRecord> records) {
        Set<String> keys = new HashSet<>();
        for (AttendanceRecord record : records) {
            String key = record.getScheduleId() + ":" + record.getDate();
            if (!keys.add(key)) throw new IllegalArgumentException("duplicate attendance for schedule/date: " + key);
        }
    }
}
