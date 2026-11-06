package ph.motorph.web;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Clock in/out with a controllable clock. */
public final class ClockServiceTest {
    private static int checks;

    public static void main(String[] args) {
        MutableClock clock = new MutableClock("2026-11-09T08:04:31+08:00");
        ClockService service = new ClockService(clock);
        AttendanceService attendance = new AttendanceService(new EmployeeService(new InMemoryEmployeeStore()),
            new InMemoryEmployeeScheduleStore(), new InMemoryAttendanceStore());

        check(!service.openShift("DEMO-004").isPresent(), "nobody starts clocked in");
        check("08:04".equals(service.clockIn(attendance, "DEMO-004").toLocalTime().toString()), "clock in uses server time to the minute");
        expect(() -> service.clockIn(attendance, "DEMO-004"), "cannot clock in twice");
        clock.set("2026-11-09T17:02:59+08:00");
        AttendanceRecord record = service.clockOut(attendance, "DEMO-004");
        check("2026-11-09".equals(record.getDate().toString()) && "08:04".equals(record.getTimeIn().toString())
            && "17:02".equals(record.getTimeOut().toString()), "clock out writes one raw attendance row");
        check(PayrollProfile.paidMinutes(record.getTimeIn(), record.getTimeOut()) == 480, "the clocked shift is paid by the normal rules (08:04 grace, 8 h)");
        expect(() -> service.clockOut(attendance, "DEMO-004"), "cannot clock out when not clocked in");
        expect(() -> service.clockIn(attendance, "DEMO-004"), "one attendance row per day");

        clock.set("2026-11-10T22:30:00+08:00");
        service.clockIn(attendance, "DEMO-004");
        clock.set("2026-11-11T01:15:00+08:00");
        check("23:59".equals(service.clockOut(attendance, "DEMO-004").getTimeOut().toString()), "a shift past midnight closes at 23:59 of its start day");

        clock.set("2026-11-12T08:00:00+08:00");
        expect(() -> { service.clockIn(attendance, "DEMO-999"); }, "unknown employees cannot clock in");

        // QR onsite check-in (requirement 2): clocking in from the app is offsite until the shift's QR code is scanned.
        clock.set("2026-11-13T08:01:00+08:00");
        service.clockIn(attendance, "DEMO-004");
        String qr = service.currentQr("DEMO-004").orElse("");
        check(qr.matches("MPH-[A-Z2-9]{6}") && !service.onsiteAt("DEMO-004", java.time.LocalDate.parse("2026-11-13")).isPresent(),
            "clock in shows a QR code and counts as offsite");
        expect(() -> service.scanOnsite("DEMO-004", "MPH-WRONG1"), "another code does not check in");
        clock.set("2026-11-13T08:12:00+08:00");
        expect(() -> service.scanOnsite("DEMO-004", qr), "the QR code expires after 10 minutes");
        String fresh = service.issueQr("DEMO-004");
        clock.set("2026-11-13T08:14:00+08:00");
        check("08:14".equals(service.scanOnsite("DEMO-004", fresh.toLowerCase()).toString()), "a new code scanned at the branch checks in onsite at the scan time");
        check(service.onsiteAt("DEMO-004", java.time.LocalDate.parse("2026-11-13")).isPresent() && !service.currentQr("DEMO-004").isPresent(),
            "after the scan the shift is onsite and no QR is shown");
        expect(() -> service.issueQr("DEMO-004"), "no new QR once onsite");
        clock.set("2026-11-13T17:05:00+08:00");
        service.clockOut(attendance, "DEMO-004");
        check(service.onsiteAt("DEMO-004", java.time.LocalDate.parse("2026-11-13")).isPresent(), "the day stays marked onsite after clock out");
        expect(() -> service.issueQr("DEMO-004"), "no QR when not clocked in");
        System.out.println("PASS: " + checks + " clock in/out checks");
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        MutableClock(String isoOffset) { set(isoOffset); }
        void set(String isoOffset) { instant = java.time.OffsetDateTime.parse(isoOffset).toInstant(); }
        @Override public ZoneId getZone() { return ZoneId.of("Asia/Manila"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return instant; }
    }

    private static void expect(Runnable action, String message) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            check(true, message);
            return;
        }
        throw new AssertionError(message + ": nothing was thrown");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

}
