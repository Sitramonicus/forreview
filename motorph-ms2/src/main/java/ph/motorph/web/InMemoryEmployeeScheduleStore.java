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
        scheduleByEmployee = Collections.unmodifiableMap(values);
    }

    @Override
    public OptionalInt findScheduleIdForEmployee(String employeeId) {
        Integer scheduleId = employeeId == null ? null : scheduleByEmployee.get(employeeId);
        return scheduleId == null ? OptionalInt.empty() : OptionalInt.of(scheduleId);
    }
}
