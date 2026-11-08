package ph.motorph.o_oop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/**
 * Employee-to-schedule mapping. Every employee number gets one stable schedule id the first time it is asked for,
 * derived from the number itself so the assignment survives a restart and is the same on every machine.
 */
public final class InMemoryEmployeeScheduleStore implements EmployeeScheduleStore {
    private final Map<String, Integer> scheduleByEmployee = new HashMap<>();

    /** Gives an onboarded employee the next synthetic schedule ID so their attendance can be recorded. */
    public synchronized int assign(String employeeId) {
        Integer existing = scheduleByEmployee.get(employeeId);
        if (existing != null) return existing;
        int next = scheduleByEmployee.values().stream().mapToInt(Integer::intValue).max().orElse(100) + 1;
        scheduleByEmployee.put(employeeId, next);
        return next;
    }

    @Override
    public synchronized OptionalInt findScheduleIdForEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(scheduleByEmployee.computeIfAbsent(employeeId.trim(), InMemoryEmployeeScheduleStore::stableId));
    }

    /** 10000–99999 mapped onto 100–999, so 10015 is schedule 101 and the mapping is reproducible. */
    private static int stableId(String employeeId) {
        String digits = employeeId.replaceAll("[^0-9]", "");
        int value = digits.isEmpty() ? Math.abs(employeeId.hashCode()) : Integer.parseInt(digits.substring(Math.max(0, digits.length() - 5)));
        return 100 + Math.floorMod(value, 900);
    }
}
