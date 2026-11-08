package ph.motorph.web;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Timekeeping arithmetic: the supplied timecard row shape, breaks, and day counting. */
public final class TimesheetServiceTest {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        matchesSuppliedTimecardRow();
        excludesZeroHourRows();
        rejectsIncompletePeriod();
        overnightIsRejectedButExplained();
        timesheetFromStoredRows();
        System.out.println("TimesheetServiceTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void matchesSuppliedTimecardRow() {
        // MotorPH timecard: 08:00 AM, break out 12:00 PM, break in 01:00 PM, out 05:00 PM = 8 hrs.
        AttendanceRecord row = new AttendanceRecord(1, 101, LocalDate.parse("2026-12-01"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("12:00"),
            java.time.LocalTime.parse("13:00"), java.time.LocalTime.parse("17:00"), "WORKDAY");
        equal("supplied timecard row is 8.00 hours", "8.00", row.getHoursWorked());
        ok("row records a break", row.hasBreak());

        // No break recorded: the full elapsed span is the paid time, which is why a missing
        // meal punch overstates hours. This is a known data-quality requirement, not a rule.
        AttendanceRecord noBreak = new AttendanceRecord(2, 101, LocalDate.parse("2026-12-02"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("17:00"), "WORKDAY");
        equal("no break recorded holds elapsed time", "9.00", noBreak.getHoursWorked());
        ok("no-break row reports no break", !noBreak.hasBreak());

        AttendanceRecord half = new AttendanceRecord(3, 101, LocalDate.parse("2026-12-03"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("12:00"),
            java.time.LocalTime.parse("12:30"), java.time.LocalTime.parse("17:00"), "WORKDAY");
        equal("half-hour break leaves 8.50 hours", "8.50", half.getHoursWorked());
    }

    private static void excludesZeroHourRows() {
        AttendanceRecord weekend = new AttendanceRecord(4, 101, LocalDate.parse("2026-12-06"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("08:00"), "WEEKEND");
        equal("zero-length row is 0.00 hours", "0.00", weekend.getHoursWorked());
    }

    private static void rejectsIncompletePeriod() {
        AttendanceService attendance = new AttendanceService(new EmployeeService(new InMemoryEmployeeStore()),
            new InMemoryEmployeeScheduleStore(), new InMemoryAttendanceStore());
        TimesheetService service = new TimesheetService(attendance);
        expectFail("needs both bounds", () -> service.summarise("10001", null, LocalDate.parse("2026-10-31")));
        expectFail("unknown employee", () -> service.summarise("NOPE", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31")));
        expectFail("reversed range", () -> service.summarise("10001", LocalDate.parse("2026-10-31"), LocalDate.parse("2026-10-01")));
    }

    private static void overnightIsRejectedButExplained() {
        expectFail("out before in is rejected", () -> new AttendanceRecord(5, 101, LocalDate.parse("2026-12-07"),
            java.time.LocalTime.parse("22:00"), java.time.LocalTime.parse("06:00"), "WORKDAY"));
        expectFail("break out of order is rejected", () -> new AttendanceRecord(6, 101, LocalDate.parse("2026-12-08"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("13:00"),
            java.time.LocalTime.parse("12:00"), java.time.LocalTime.parse("17:00"), "WORKDAY"));
        expectFail("half a break is rejected", () -> new AttendanceRecord(7, 101, LocalDate.parse("2026-12-09"),
            java.time.LocalTime.parse("08:00"), java.time.LocalTime.parse("12:00"), null,
            java.time.LocalTime.parse("17:00"), "WORKDAY"));
    }

    private static void timesheetFromStoredRows() {
        AttendanceService attendance = new AttendanceService(new EmployeeService(new InMemoryEmployeeStore()),
            new InMemoryEmployeeScheduleStore(), new InMemoryAttendanceStore());
        TimesheetService.Summary summary = new TimesheetService(attendance)
            .summarise("10001", LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"));
        equalInt("three stored working days", 3, summary.daysWorked());
        ok("total hours is positive", summary.totalHours().compareTo(BigDecimal.ZERO) > 0);
        ok("days are date-ordered", summary.days().get(0).date.compareTo(summary.days().get(2).date) < 0);
        equal("first stored day", "2026-10-01", summary.days().get(0).date);
    }

    private static void equal(String label, String expected, BigDecimal actual) {
        if (expected.equals(actual.toPlainString())) { passed++; }
        else { failed++; System.out.println("  FAIL " + label + ": expected " + expected + " got " + actual); }
    }

    private static void equal(String label, String expected, String actual) {
        if (expected.equals(actual)) { passed++; }
        else { failed++; System.out.println("  FAIL " + label + ": expected " + expected + " got " + actual); }
    }

    private static void equalInt(String label, int expected, int actual) {
        if (expected == actual) { passed++; }
        else { failed++; System.out.println("  FAIL " + label + ": expected " + expected + " got " + actual); }
    }

    private static void ok(String label, boolean condition) {
        if (condition) { passed++; }
        else { failed++; System.out.println("  FAIL " + label); }
    }

    private static void expectFail(String label, Runnable action) {
        try {
            action.run();
            failed++;
            System.out.println("  FAIL " + label + ": expected rejection");
        } catch (RuntimeException expected) {
            passed++;
        }
    }
}
