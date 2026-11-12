package ph.motorph.o_oop;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class InMemoryEmployeeStore implements MutableEmployeeStore {
    private final Map<String, Employee> employees = new LinkedHashMap<>();

    public InMemoryEmployeeStore() {
        
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

    
    public synchronized void remove(String id) {
        if (employees.remove(id) == null) throw new NoSuchElementException("Employee not found.");
    }

    
    public void seed(Employee employee) {
        addFixture(employee);
    }

    void addFixture(Employee employee) {
        employees.put(employee.getId(), employee);
    }
}
