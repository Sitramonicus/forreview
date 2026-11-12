package ph.motorph.o_oop;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LeavePolicy {
    public static final String VERSION = "TEMPORARY-2026-placeholder";
    private static final Map<String, Integer> YEARLY_CREDITS;
    static {
        Map<String, Integer> credits = new LinkedHashMap<>();
        credits.put("Vacation", 5);
        credits.put("Medical", 5);
        credits.put("Personal", 3);
        YEARLY_CREDITS = Collections.unmodifiableMap(credits);
    }

    private LeavePolicy() { }

    
    public static boolean isLimited(String leaveType) { return YEARLY_CREDITS.containsKey(leaveType); }

    
    public static boolean isPaid(String leaveType) { return YEARLY_CREDITS.containsKey(leaveType); }

    public static int yearlyCredits(String leaveType) { return YEARLY_CREDITS.getOrDefault(leaveType, 0); }

    public static List<String> limitedTypes() { return List.copyOf(YEARLY_CREDITS.keySet()); }

    
    public static int creditsFor(LocalDate start, LocalDate end) {
        int days = 0;
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) days++;
        }
        return days;
    }
}
