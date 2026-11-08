package ph.motorph.o_oop;

import ph.motorph.svc.AppServer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** In-memory employee store. Used by tests and as a fallback when no data file is configured. */
public final class InMemoryEmployeeStore implements MutableEmployeeStore {
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    public InMemoryEmployeeStore() {
        // Empty by design: the roster is imported from MotorPH's employee file by EmployeeImport.
    }

    @Override
    public synchronized List<Employee> findAll() {
        return new ArrayList<>(employees.values());
    }

    @Override
    public synchronized Optional<Employee> findById(String id) {
        return Optional.ofNullable(id == null ? null : employees.get(id.trim()));
    }

    @Override
    public synchronized Employee create(Employee employee) {
        if (employees.containsKey(employee.getId())) throw new IllegalStateException("Employee ID already exists.");
        employees.put(employee.getId(), employee);
        return employee;
    }

    @Override
    public synchronized Employee update(Employee employee) {
        if (!employees.containsKey(employee.getId())) throw new NoSuchElementException("Employee not found.");
        employees.put(employee.getId(), employee);
        return employee;
    }

    /** System Admin delete (only records with no history; AppServer checks). */
    public synchronized void remove(String id) {
        if (employees.remove(id) == null) throw new NoSuchElementException("Employee not found.");
    }

    /** Seeds the store from the imported roster (start-up and tests). */
    public void seed(Employee employee) {
        addFixture(employee);
    }

    void addFixture(Employee employee) {
        employees.put(employee.getId(), employee);
    }
}
