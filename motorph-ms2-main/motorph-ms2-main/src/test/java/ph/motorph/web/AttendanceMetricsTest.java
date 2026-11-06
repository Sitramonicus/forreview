package ph.motorph.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;

/** Requirement 3 counts, hand-worked on the default Mon-Fri 08:00 schedule (grace to 08:10, meal 12:00-13:00). */
public final class AttendanceMetricsTest {
    private static int checks;

    public static void main(String[] args) {
        // Week of Mon 2 Nov 2026, "today" Thu 5 Nov.
        // Mon 08:15-17:00: tardy 15 min; paid 08:15-12:00 + 13:00-17:00 = 465, so 15 min undertime.
        // Tue 08:00-19:00 with 60 approved OT: 600 worked = 480 regular + 60 OT + 60 not approved.
        // Wed: no row -> absent. Thu: approved leave. Fri: after today, so not absent yet. Sat/Sun: rest days.
        AttendanceMetrics m = AttendanceMetrics.of(Arrays.asList(row("2026-11-02", "08:15", "17:00"), row("2026-11-03", "08:00", "19:00")),
            LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-08"), LocalDate.parse("2026-11-05"), WorkSchedule.DEFAULT,
            Collections.singletonMap(LocalDate.parse("2026-11-03"), 60), day -> day.equals(LocalDate.parse("2026-11-05")), day -> true);
        check(m.presentDays == 2, "two days present");
        check(m.tardyDays == 1 && m.tardyMinutes == 15, "one tardy day, 15 minutes late");
        check(m.undertimeMinutes == 15, "15 minutes undertime on Monday");
        check(m.overtimeMinutes == 60 && m.unapprovedExtraMinutes == 60, "60 approved overtime, 60 extra not approved");
        check(m.absentDays == 1, "Wednesday absent; Friday is still to come");
        check(m.leaveDays == 1, "Thursday on leave");

        // 08:10 is still inside the grace period; no range given = earliest to latest record.
        AttendanceMetrics graced = AttendanceMetrics.of(Arrays.asList(row("2026-10-01", "08:10", "17:00"), row("2026-10-05", "08:00", "17:00")),
            null, null, LocalDate.parse("2026-11-05"), WorkSchedule.DEFAULT, Collections.emptyMap(), day -> false, day -> true);
        check(graced.tardyDays == 0 && graced.undertimeMinutes == 0, "08:10 is not tardy and is paid in full");
        check("2026-10-01".equals(graced.from.toString()) && "2026-10-05".equals(graced.to.toString()), "default range is earliest to latest record");
        check(graced.absentDays == 1, "Fri 2 Oct absent between the two records");
        check(AttendanceMetrics.of(Collections.emptyList(), null, null, LocalDate.parse("2026-11-05"), WorkSchedule.DEFAULT,
            Collections.emptyMap(), day -> false, day -> true).absentDays == 0, "no records and no range: nothing counted");
        System.out.println("PASS: " + checks + " attendance-metrics checks");
    }

    private static AttendanceRecord row(String date, String in, String out) {
        return new AttendanceRecord(1, 101, LocalDate.parse(date), LocalTime.parse(in), LocalTime.parse(out), "DEMO");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
