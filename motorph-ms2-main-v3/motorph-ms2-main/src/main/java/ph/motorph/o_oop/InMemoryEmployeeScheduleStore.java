package ph.motorph.o_oop;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class InMemoryEmployeeScheduleStore implements EmployeeScheduleStore {
    private final Map<String, Integer> scheduleByEmployee = new HashMap<>();

    
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

    
    private static int stableId(String employeeId) {
        String digits = employeeId.replaceAll("[^0-9]", "");
        int value = digits.isEmpty() ? Math.floorMod(employeeId.hashCode(), 900) : Integer.parseInt(digits.substring(Math.max(0, digits.length() - 5)));
        return 100 + Math.floorMod(value, 900);
    }
}
