package motorph;

import static motorph.TestSupport.*;

import java.time.LocalDate;
import java.time.LocalTime;
import motorph.model.AttendanceRecord;

/** Tests the hidden time rules and the cutoff label. */
public class AttendanceRecordTest {

    public static void main(String[] args) {
        run();
        finish();
    }

    private static AttendanceRecord record(String date, String in, String out) {
        return new AttendanceRecord("10001", LocalDate.parse(date), LocalTime.parse(in), LocalTime.parse(out));
    }

    static void run() {
        section("AttendanceRecord: grace period and logout cap");
        checkEquals(8.0, record("2024-06-03", "08:10", "16:00").computeHoursWorked(), "8:10 login counts as 8:00 (grace)");
        checkEquals(470L, record("2024-06-03", "08:11", "16:01").computeMinutesWorked(), "8:11 login is late: 8:11 to 16:01 = 470 min");
        checkEquals(9.0, record("2024-06-03", "08:05", "18:31").computeHoursWorked(), "logout 18:31 capped at 17:00");
        checkEquals(481L, record("2024-06-03", "08:59", "18:31").computeMinutesWorked(), "8:59 to cap 17:00 = 481 min");
        checkEquals(0L, record("2024-06-03", "17:30", "18:00").computeMinutesWorked(), "login after the cap gives 0, never negative");

        section("AttendanceRecord: validation");
        checkRejects(() -> record("2024-06-03", "09:00", "08:00"), "logout before login rejected");
        checkRejects(() -> record("2024-06-03", "09:00", "09:00"), "logout equal to login rejected");
        AttendanceRecord r = record("2024-06-03", "08:00", "17:00");
        checkRejects(() -> r.setLogOut(LocalTime.of(7, 0)), "setLogOut must be after login");
        checkRejects(() -> new AttendanceRecord(" ", LocalDate.of(2024, 6, 3), LocalTime.of(8, 0), LocalTime.of(17, 0)),
                "employee ID required");

        section("AttendanceRecord: getCutoffLabel()");
        checkEquals("June 1-15, 2024", record("2024-06-01", "08:00", "17:00").getCutoffLabel(), "day 1 is 1st cutoff");
        checkEquals("June 1-15, 2024", record("2024-06-15", "08:00", "17:00").getCutoffLabel(), "day 15 is 1st cutoff");
        checkEquals("June 16-30, 2024", record("2024-06-16", "08:00", "17:00").getCutoffLabel(), "day 16 is 2nd cutoff, June has 30 days");
        checkEquals("July 16-31, 2024", record("2024-07-31", "08:00", "17:00").getCutoffLabel(), "July has 31 days");
        checkEquals("February 16-29, 2024", record("2024-02-20", "08:00", "17:00").getCutoffLabel(), "leap year February ends on 29");
        checkEquals("February 16-28, 2023", record("2023-02-20", "08:00", "17:00").getCutoffLabel(), "non-leap February ends on 28");
    }
}
