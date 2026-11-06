package ph.motorph.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

/** Synthetic employee-to-schedule mapping; replace with the approved source later. */
public final class InMemoryEmployeeScheduleStore implements EmployeeScheduleStore {
    private final Map<String, Integer> scheduleByEmployee;

    public InMemoryEmployeeScheduleStore() {
        Map<String, Integer> values = new HashMap<>();
        values.put("DEMO-001", 101);
        values.put("DEMO-002", 102);
        values.put("DEMO-003", 103);
        values.put("DEMO-004", 104);
        values.put("DEMO-005", 105);
        scheduleByEmployee = values;
    }

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
        Integer scheduleId = employeeId == null ? null : scheduleByEmployee.get(employeeId);
        return scheduleId == null ? OptionalInt.empty() : OptionalInt.of(scheduleId);
    }
}
