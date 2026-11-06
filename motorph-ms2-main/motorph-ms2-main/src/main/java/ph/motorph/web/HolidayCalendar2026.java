package ph.motorph.web;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 2026 Philippine national holidays: Proclamation No. 1006 (2025-09-03), plus Eid'l Fitr (Proc. 1189, 20 March)
 * and Eid'l Adha (Proc. 1264, 27 May). Local holidays are not included.
 * shortcut: rest days are Saturday and Sunday because the synthetic schedules are Monday-Friday; use per-employee
 * rest days once schedules carry them.
 */
public final class HolidayCalendar2026 {
    /** DOLE multipliers for the first 8 hours worked and for overtime (OT rates confirmed by the mentor). */
    public enum DayKind {
        ORDINARY("Ordinary day", "1.00", "1.25"),
        REST_DAY("Rest day", "1.30", "1.69"),
        SPECIAL("Special non-working day", "1.30", "1.69"),
        SPECIAL_REST_DAY("Special day on a rest day", "1.50", "1.95"),
        REGULAR_HOLIDAY("Regular holiday", "2.00", "2.60"),
        REGULAR_HOLIDAY_REST_DAY("Regular holiday on a rest day", "2.60", "3.38");

        public final String label;
        public final BigDecimal workedMultiplier;
        public final BigDecimal overtimeMultiplier;

        DayKind(String label, String worked, String overtime) {
            this.label = label;
            this.workedMultiplier = new BigDecimal(worked);
            this.overtimeMultiplier = new BigDecimal(overtime);
        }
    }

    private static final Map<LocalDate, String> HOLIDAYS;
    static {
        Map<LocalDate, String> days = new HashMap<>();
        for (String regular : new String[] {"2026-01-01", "2026-03-20", "2026-04-02", "2026-04-03", "2026-04-09", "2026-05-01",
                "2026-05-27", "2026-06-12", "2026-08-31", "2026-11-30", "2026-12-25", "2026-12-30"}) {
            days.put(LocalDate.parse(regular), "REGULAR");
        }
        for (String special : new String[] {"2026-02-17", "2026-04-04", "2026-08-21", "2026-11-01", "2026-11-02",
                "2026-12-08", "2026-12-24", "2026-12-31"}) {
            days.put(LocalDate.parse(special), "SPECIAL");
        }
        HOLIDAYS = Collections.unmodifiableMap(days);
    }

    private HolidayCalendar2026() { }

    public static boolean isRestDay(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static boolean isRegularHoliday(LocalDate date) { return "REGULAR".equals(HOLIDAYS.get(date)); }

    public static boolean isHoliday(LocalDate date) { return HOLIDAYS.containsKey(date); }

    public static DayKind kindOf(LocalDate date) {
        return kindOf(date, isRestDay(date));
    }

    /** With the person's own rest day (from their work schedule). */
    public static DayKind kindOf(LocalDate date, boolean rest) {
        if (date.getYear() != 2026) throw new IllegalArgumentException("Only the 2026 holiday calendar is loaded.");
        String holiday = HOLIDAYS.get(date);
        if ("REGULAR".equals(holiday)) return rest ? DayKind.REGULAR_HOLIDAY_REST_DAY : DayKind.REGULAR_HOLIDAY;
        if ("SPECIAL".equals(holiday)) return rest ? DayKind.SPECIAL_REST_DAY : DayKind.SPECIAL;
        return rest ? DayKind.REST_DAY : DayKind.ORDINARY;
    }
}
