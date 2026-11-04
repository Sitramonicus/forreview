package motorphemployee.oop;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AttendanceLog - the input side of gross pay (MS1 Week 2 CRC: "AttendanceLog").
 *
 * Enforces the integrity rules from the approved design: one clock-in per day, clock-in
 * must come before clock-out, hours never exceed 24 per day, and no future-dated records.
 * Callers only get read-only summaries - there is no way to edit a saved record.
 *
 * Storage note (open question in the Refactoring Plan): CP2 hardcoded a
 * workingDays[] array (MainMenu.java:1066) and no timecard CSV has been imported yet, so
 * this class keeps the records in memory. The public API is CSV-ready: an adapter that
 * feeds rows from attendance.csv needs no change to PayrollCalculator. No attendance data
 * was invented to make this test pass.
 */
public class AttendanceLog {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    /** employeeId -> (date -> {clockIn, clockOut}) */
    private final Map<Integer, Map<String, String[]>> records = new LinkedHashMap<>();

    public void recordClockIn(int employeeId) {
        recordClockIn(employeeId, LocalDate.now().format(DATE), LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    public void recordClockOut(int employeeId) {
        recordClockOut(employeeId, LocalDate.now().format(DATE), LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    /** Testable overload: records a clock-in for an explicit date and time. */
    public void recordClockIn(int employeeId, String date, String time) {
        requireEmployee(employeeId);
        LocalDate day = parseDate(date);
        if (day.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("cannot clock in on a future date: " + date);
        }
        parseTime(time);

        Map<String, String[]> perDay = records.computeIfAbsent(employeeId, key -> new LinkedHashMap<>());
        String[] entry = perDay.computeIfAbsent(date, key -> new String[]{null, null});
        if (entry[0] != null) {
            throw new IllegalArgumentException("duplicate clock-in for employee " + employeeId + " on " + date);
        }
        entry[0] = time;
    }

    /** Testable overload: records a clock-out for an explicit date and time. */
    public void recordClockOut(int employeeId, String date, String time) {
        requireEmployee(employeeId);
        parseDate(date);
        LocalTime clockOut = parseTime(time);

        Map<String, String[]> perDay = records.get(employeeId);
        if (perDay == null || perDay.get(date) == null || perDay.get(date)[0] == null) {
            throw new IllegalArgumentException("cannot clock out without a clock-in: employee "
                    + employeeId + " on " + date);
        }
        String[] entry = perDay.get(date);
        if (entry[1] != null) {
            throw new IllegalArgumentException("duplicate clock-out for employee " + employeeId + " on " + date);
        }
        LocalTime clockIn = parseTime(entry[0]);
        if (!clockOut.isAfter(clockIn)) {
            throw new IllegalArgumentException("clock-out must be after clock-in: " + entry[0] + " -> " + time);
        }
        double hours = Duration.between(clockIn, clockOut).toMinutes() / 60.0;
        if (hours > 24) {
            throw new IllegalArgumentException("hours worked cannot exceed 24 in a day: " + hours);
        }
        entry[1] = time;
    }

    /** Validated hours for one employee on one date; 0 when there is no complete record. */
    public double getDailyHours(int employeeId, String date) {
        requireEmployee(employeeId);
        Map<String, String[]> perDay = records.get(employeeId);
        if (perDay == null) {
            return 0.0;
        }
        String[] entry = perDay.get(date);
        if (entry == null || entry[0] == null || entry[1] == null) {
            return 0.0;
        }
        return Duration.between(parseTime(entry[0]), parseTime(entry[1])).toMinutes() / 60.0;
    }

    /** Days with a complete clock-in/clock-out pair inside the period (inclusive). */
    public int getWorkedDays(int employeeId, String periodStart, String periodEnd) {
        requireEmployee(employeeId);
        LocalDate start = parseDate(periodStart);
        LocalDate end = parseDate(periodEnd);
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("periodEnd cannot be before periodStart");
        }
        Map<String, String[]> perDay = records.get(employeeId);
        if (perDay == null) {
            return 0;
        }
        int days = 0;
        for (Map.Entry<String, String[]> entry : perDay.entrySet()) {
            String[] times = entry.getValue();
            if (times[0] == null || times[1] == null) {
                continue;
            }
            LocalDate day = parseDate(entry.getKey());
            if (!day.isBefore(start) && !day.isAfter(end)) {
                days++;
            }
        }
        return days;
    }

    private static void requireEmployee(int employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("employeeId must be positive: " + employeeId);
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date, DATE);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("date must be yyyy-MM-dd: " + date);
        }
    }

    private static LocalTime parseTime(String time) {
        try {
            return LocalTime.parse(time, TIME);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("time must be HH:mm or HH:mm:ss: " + time);
        }
    }
}
