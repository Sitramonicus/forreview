package ph.motorph.web;

import java.util.OptionalInt;

/** Maps an employee to the schedule ID used by the supplied Attendance model. */
public interface EmployeeScheduleStore {
    OptionalInt findScheduleIdForEmployee(String employeeId);
}
