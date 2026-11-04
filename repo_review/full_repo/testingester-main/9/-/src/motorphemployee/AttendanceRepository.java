package motorphemployee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import motorphemployee.oop.AttendanceLog;

/**
 * The real MotorPH timecard, read from {@code attendance.csv}.
 *
 * <p>Until this class existed the two payroll screens invented attendance: for every
 * working day of a hardcoded month table they recorded 08:00-16:00 on a 2024 calendar,
 * so a payslip never reflected an actual timecard. The data now comes from the company
 * timecard export — {@code Employee #, Date, Log In, Log Out} — a straight copy of the
 * "Attendance Record" sheet in {@code Copy of MotorPH_Employee Data.xlsx}
 * (34 employees, 2025-06-03 to 2025-12-31, 5,168 rows, one row per employee per working
 * day, no duplicate or missing clock-in/clock-out pairs).</p>
 *
 * <p>Employee numbers: the sheet uses 10001-10034 while {@code data.csv} uses 1-34 for the
 * same 34 people (same names, same order, difference 10000). The export was therefore keyed
 * to the numbers {@code data.csv} uses, so the payslip looks up the same employee id it
 * already holds.</p>
 *
 * <p>Only months that have rows are offered to the user, so a payroll period can never be
 * priced from an empty month. Worked days and gross pay stay a function of the log
 * ({@link AttendanceLog#getWorkedDays}) — nothing here adds or assumes a day.</p>
 */
public class AttendanceRepository {

    /** Columns of attendance.csv, in file order. */
    public static final String[] COLUMNS = {"Employee #", "Date", "Log In", "Log Out"};

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Path file;
    private List<Record> cache;

    public AttendanceRepository(String path) {
        this.file = Paths.get(path);
    }

    /** The file the screens use, relative to the project directory. */
    public static AttendanceRepository forProjectFiles() {
        return new AttendanceRepository("src/motorphemployee/attendance.csv");
    }

    /** One timecard row: who, which day, and the clock-in/clock-out pair. */
    public static final class Record {
        public final int employeeId;
        public final LocalDate date;
        public final String clockIn;
        public final String clockOut;

        Record(int employeeId, LocalDate date, String clockIn, String clockOut) {
            this.employeeId = employeeId;
            this.date = date;
            this.clockIn = clockIn;
            this.clockOut = clockOut;
        }

        public String dateText() {
            return date.format(DATE);
        }
    }

    /** Every row, in file order. Loaded once per repository instance. */
    public List<Record> records() throws IOException {
        if (cache == null) {
            cache = read();
        }
        return cache;
    }

    /** Months that actually have timecard rows for this employee, oldest first. */
    public List<YearMonth> monthsWithRecords(int employeeId) throws IOException {
        TreeSet<YearMonth> months = new TreeSet<>();
        for (Record record : records()) {
            if (record.employeeId == employeeId) {
                months.add(YearMonth.from(record.date));
            }
        }
        return new ArrayList<>(months);
    }

    /** True when the timecard has at least one row for this employee. */
    public boolean hasRecords(int employeeId) throws IOException {
        for (Record record : records()) {
            if (record.employeeId == employeeId) {
                return true;
            }
        }
        return false;
    }

    /** Rows of one employee inside one month, oldest first. */
    public List<Record> recordsFor(int employeeId, YearMonth month) throws IOException {
        List<Record> rows = new ArrayList<>();
        for (Record record : records()) {
            if (record.employeeId == employeeId && YearMonth.from(record.date).equals(month)) {
                rows.add(record);
            }
        }
        return rows;
    }

    /**
     * The month as an {@link AttendanceLog} the payroll calculator accepts. Every row is
     * recorded as a clock-in/clock-out pair; the log itself rejects duplicates, a clock-out
     * before a clock-in, and future dates, so a corrupt row stops the payslip instead of
     * silently shrinking it.
     */
    public AttendanceLog loadFor(int employeeId, YearMonth month) throws IOException {
        AttendanceLog log = new AttendanceLog();
        for (Record record : recordsFor(employeeId, month)) {
            log.recordClockIn(employeeId, record.dateText(), record.clockIn);
            log.recordClockOut(employeeId, record.dateText(), record.clockOut);
        }
        return log;
    }

    /** "June 2025" — the label the period picker shows. */
    public static String labelOf(YearMonth month) {
        return month.atDay(1).format(LABEL);
    }

    /** The first and last day of the month as yyyy-MM-dd, the period the calculator takes. */
    public static String periodStartOf(YearMonth month) {
        return month.atDay(1).format(DATE);
    }

    public static String periodEndOf(YearMonth month) {
        return month.atEndOfMonth().format(DATE);
    }

    // ------------------------------------------------------------------ parsing

    private List<Record> read() throws IOException {
        List<Record> rows = new ArrayList<>();
        if (!Files.exists(file)) {
            throw new IOException("attendance file not found: " + file);
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int lineNumber = 0; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] cells = line.split(",", -1);
            if (lineNumber == 0 && looksLikeHeader(cells)) {
                continue;
            }
            if (cells.length < 4) {
                throw new IOException("attendance.csv line " + (lineNumber + 1) + " has " + cells.length
                        + " column(s), expected " + COLUMNS.length);
            }
            int employeeId;
            try {
                employeeId = Integer.parseInt(cells[0].trim());
            } catch (NumberFormatException notAnId) {
                throw new IOException("attendance.csv line " + (lineNumber + 1)
                        + " has an invalid employee number: '" + cells[0] + "'");
            }
            LocalDate date = parseDate(cells[1].trim(), lineNumber + 1);
            String clockIn = parseTime(cells[2].trim(), lineNumber + 1);
            String clockOut = parseTime(cells[3].trim(), lineNumber + 1);
            rows.add(new Record(employeeId, date, clockIn, clockOut));
        }
        return rows;
    }

    private static boolean looksLikeHeader(String[] cells) {
        return cells[0].trim().toLowerCase(Locale.ROOT).startsWith("employee");
    }

    private static LocalDate parseDate(String raw, int lineNumber) throws IOException {
        try {
            return LocalDate.parse(raw, DATE);
        } catch (DateTimeParseException badDate) {
            throw new IOException("attendance.csv line " + lineNumber + " has an invalid date: '" + raw
                    + "' (expected yyyy-MM-dd)");
        }
    }

    private static String parseTime(String raw, int lineNumber) throws IOException {
        try {
            return LocalTime.parse(raw, TIME).format(TIME);
        } catch (DateTimeParseException badTime) {
            throw new IOException("attendance.csv line " + lineNumber + " has an invalid time: '" + raw
                    + "' (expected HH:mm:ss)");
        }
    }
}
