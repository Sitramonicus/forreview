package ph.motorph.o_oop;

import java.util.List;
import java.util.Optional;

public interface EmployeeStore {
    List<Employee> findAll();
    Optional<Employee> findById(String id);
}
