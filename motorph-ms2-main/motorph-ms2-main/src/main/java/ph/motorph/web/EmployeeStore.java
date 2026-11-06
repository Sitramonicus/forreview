package ph.motorph.web;

import java.util.List;
import java.util.Optional;

/** Storage boundary; the first implementation is in-memory, later one can use Firestore. */
public interface EmployeeStore {
    List<Employee> findAll();
    Optional<Employee> findById(String id);
}
