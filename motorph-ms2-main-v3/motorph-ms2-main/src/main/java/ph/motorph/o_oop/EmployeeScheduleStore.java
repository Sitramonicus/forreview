package ph.motorph.o_oop;

import java.util.OptionalInt;

public interface EmployeeScheduleStore {
    OptionalInt findScheduleIdForEmployee(String employeeId);
}
