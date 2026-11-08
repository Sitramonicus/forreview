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
        // Only the employees with seeded attendance rows are mapped. Employee 10015 is the person
        // on MotorPH's supplied December 2023 timecard; 10001-10003 carry the small synthetic
        // October sample used by the attendance screens. The real file has no schedule column,
        // so every other employee is deliberately unmapped until MotorPH supplies assignments.
        values.put("10015", 104);   // Romualdez, Fredrick - supplied December 2023 timecard
        values.put("10001", 101);
        values.put("10002", 102);
        values.put("10003", 103);
        scheduleByEmployee = Collections.unmodifiableMap(values);
    }

    @Override
    public OptionalInt findScheduleIdForEmployee(String employeeId) {
        Integer scheduleId = employeeId == null ? null : scheduleByEmployee.get(employeeId);
        return scheduleId == null ? OptionalInt.empty() : OptionalInt.of(scheduleId);
    }
}
