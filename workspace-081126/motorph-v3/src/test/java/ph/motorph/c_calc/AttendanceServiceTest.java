package ph.motorph.c_calc;

import ph.motorph.c_calc.AttendanceService;
import ph.motorph.o_oop.AttendanceRecord;
import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.InMemoryAttendanceStore;
import ph.motorph.o_oop.InMemoryEmployeeScheduleStore;
import ph.motorph.o_oop.InMemoryEmployeeStore;
import ph.motorph.o_oop.RawAttendanceSummary;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public final class AttendanceServiceTest {
    private int checks;

    public static void main(String[] args) {
        new AttendanceServiceTest().run();
    }

    private void run() {
        EmployeeService employees = new EmployeeService(new InMemoryEmployeeStore());
        InMemoryAttendanceStore attendanceStore = new InMemoryAttendanceStore();
        AttendanceService service = new AttendanceService(employees,
            new InMemoryEmployeeScheduleStore(), attendanceStore);

        List<AttendanceRecord> all = service.findForEmployee("DEMO-001", null, null);
        equal(3, all.size(), "employee gets only their synthetic schedule entries");
        equal(101, all.get(0).getScheduleId(), "attendance is linked by schedule ID");
        equal("2026-10-01", all.get(0).getDate().toString(), "date is preserved without deriving hours");

        List<RawAttendanceSummary> summaries = service.summarizeRawRecords(null, null);
        RawAttendanceSummary firstSummary = summaries.stream()
            .filter(item -> item.getEmployeeId().equals("DEMO-001")).findFirst().orElseThrow(AssertionError::new);
        equal(3, firstSummary.getRawRecordCount(), "summary counts raw rows only");
        equal("2026-10-01", firstSummary.getFirstRecordDate(), "summary exposes earliest raw date");
        equal("2026-10-15", firstSummary.getLastRecordDate(), "summary exposes latest raw date");
        check(firstSummary.hasScheduleMapping() && firstSummary.getScheduleId() == 101,
            "summary reports the configured synthetic schedule mapping");
        Employee unmapped = employees.createEmployee("Unmapped Sample", "Demo", "Tester", "Taguig", "Active");
        RawAttendanceSummary unmappedSummary = service.summarizeRawRecords(null, null).stream()
            .filter(item -> item.getEmployeeId().equals(unmapped.getId())).findFirst().orElseThrow(AssertionError::new);
        check(!unmappedSummary.hasScheduleMapping() && unmappedSummary.getRawRecordCount() == 0,
            "unmapped employee is identified without implying absence");
        List<RawAttendanceSummary> oneDaySummary = service.summarizeRawRecords(
            LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-02"));
        RawAttendanceSummary filteredSummary = oneDaySummary.stream()
            .filter(item -> item.getEmployeeId().equals("DEMO-001")).findFirst().orElseThrow(AssertionError::new);
        equal(1, filteredSummary.getRawRecordCount(), "report date range is inclusive");
        expectIllegalArgument(() -> service.summarizeRawRecords(LocalDate.parse("2026-10-15"),
            LocalDate.parse("2026-10-01")), "report rejects reversed date range");

        List<AttendanceRecord> oneDay = service.findForEmployee("DEMO-001",
            LocalDate.parse("2026-10-02"), LocalDate.parse("2026-10-02"));
        equal(1, oneDay.size(), "date filtering is inclusive at both ends");
        equal(5002, oneDay.get(0).getAttendanceId(), "date filter returns the right entry");

        equal(0, service.findForEmployee("DEMO-002", LocalDate.parse("2026-10-10"), null).size(),
            "open-ended lower date filter works");
        equal(1, service.findForEmployee("DEMO-003", null, LocalDate.parse("2026-10-02")).size(),
            "open-ended upper date filter works");
        equal(0, service.findForEmployee("DEMO-001", LocalDate.parse("2026-11-01"), null).size(),
            "no matches returns an empty list");

        Set<String> scheduleDates = new HashSet<>();
        for (AttendanceRecord record : new InMemoryAttendanceStore().findByScheduleId(101)) {
            check(scheduleDates.add(record.getScheduleId() + ":" + record.getDate()),
                "synthetic fixture respects schedule/date uniqueness");
        }

        expectIllegalArgument(() -> service.findForEmployee("DEMO-001",
            LocalDate.parse("2026-10-15"), LocalDate.parse("2026-10-01")), "from-after-to is rejected");
        expectNoSuchElement(() -> service.findForEmployee("UNKNOWN", null, null), "unknown employee is rejected");

        AttendanceRecord created = service.createForEmployee("DEMO-001", LocalDate.parse("2026-10-20"),
            LocalTime.of(8, 0), LocalTime.of(17, 0));
        equal(5303, created.getAttendanceId(), "local capture allocates the next synthetic ID");
        equal(101, created.getScheduleId(), "capture uses the existing schedule mapping, not a client-supplied ID");
        equal("DEMO", created.getDayType(), "capture assigns only the fixed synthetic day type");
        equal(4, service.findForEmployee("DEMO-001", null, null).size(), "created row appears in raw attendance only");
        expectIllegalState(() -> service.createForEmployee("DEMO-001", LocalDate.parse("2026-10-20"),
            LocalTime.of(8, 1), LocalTime.of(17, 1)), "duplicate schedule/date is rejected");
        expectIllegalArgument(() -> service.createForEmployee("DEMO-001", LocalDate.parse("2026-10-21"),
            LocalTime.of(18, 0), LocalTime.of(17, 0)), "capture enforces source timeIn <= timeOut rule");
        expectIllegalState(() -> service.createForEmployee(unmapped.getId(), LocalDate.parse("2026-10-20"),
            LocalTime.of(8, 0), LocalTime.of(17, 0)), "unmapped employee cannot receive an attendance record");
        expectIllegalArgument(() -> new AttendanceRecord(0, 101, LocalDate.parse("2026-10-01"),
            LocalTime.of(8, 0), LocalTime.of(17, 0), "DEMO"), "attendance ID must be positive");
        expectIllegalArgument(() -> new AttendanceRecord(1, 101, LocalDate.parse("2026-10-01"),
            LocalTime.of(18, 0), LocalTime.of(17, 0), "DEMO"), "source timeIn <= timeOut rule is enforced");
        expectIllegalArgument(() -> new AttendanceRecord(1, 101, LocalDate.parse("2026-10-01"),
            LocalTime.of(8, 0), LocalTime.of(17, 0), " "), "dayType cannot be blank");

        System.out.println("PASS: " + checks + " attendance-domain checks");
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void expectIllegalArgument(Runnable action, String message) {
        checks++;
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message);
    }

    private void expectNoSuchElement(Runnable action, String message) {
        checks++;
        try { action.run(); } catch (NoSuchElementException expected) { return; }
        throw new AssertionError(message);
    }

    private void expectIllegalState(Runnable action, String message) {
        checks++;
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError(message);
    }
}
