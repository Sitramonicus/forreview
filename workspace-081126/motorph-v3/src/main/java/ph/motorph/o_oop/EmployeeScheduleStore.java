package ph.motorph.o_oop;

import java.util.OptionalInt;

/** Maps an employee to the schedule ID used by the supplied Attendance model. */
public interface EmployeeScheduleStore {
    OptionalInt findScheduleIdForEmployee(String employeeId);
}
