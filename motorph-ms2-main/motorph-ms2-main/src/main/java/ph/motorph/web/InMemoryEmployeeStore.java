package ph.motorph.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/** Process-memory store containing only fictional demo employees. */
public final class InMemoryEmployeeStore implements MutableEmployeeStore {
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    public InMemoryEmployeeStore() {
        addFixture(new Employee("DEMO-001", "Mara Rivera", "People Operations", "HR Coordinator", "Makati", "Active"));
        addFixture(new Employee("DEMO-002", "Eli Santos", "Finance", "Payroll Associate", "Taguig", "Active"));
        addFixture(new Employee("DEMO-003", "Noa Reyes", "Operations", "Operations Lead", "Quezon City", "On leave"));
        addFixture(new Employee("DEMO-004", "Jae Cruz", "Operations", "Operations Associate", "Pasig", "Active"));
        addFixture(new Employee("DEMO-005", "Lia Torres", "IT", "System Administrator", "Makati", "Active"));
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

    private void addFixture(Employee employee) {
        employees.put(employee.getId(), employee);
    }
}
