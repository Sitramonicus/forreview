package ph.motorph.web;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/** Employee use cases, separated from HTTP handling and the storage implementation. */
public final class EmployeeService {
    private final EmployeeStore store;

    public EmployeeService(EmployeeStore store) {
        if (store == null) throw new IllegalArgumentException("store must not be null");
        this.store = store;
    }

    public List<Employee> listEmployees() {
        return store.findAll();
    }

    public Optional<Employee> findEmployee(String id) {
        return store.findById(id == null ? null : id.trim());
    }

    /** Creates a fictional directory record inside the current owner's sandbox only. */
    public Employee createEmployee(String name, String department, String role, String location, String status) {
        MutableEmployeeStore mutable = mutableStore();
        Employee employee = validatedEmployee("DEMO-" + UUID.randomUUID().toString().replace("-", "")
            .substring(0, 12).toUpperCase(Locale.ROOT), name, department, role, location, status);
        return mutable.create(employee);
    }

    /** Updates only fields present in the synthetic directory model; no payroll/PII fields exist here. */
    public Employee updateEmployee(String id, String name, String department, String role, String location, String status) {
        String safeId = id == null ? "" : id.trim();
        if (!safeId.matches("DEMO-(?:[0-9]{3}|[A-F0-9]{12})")) throw new IllegalArgumentException("Employee ID is invalid.");
        if (!findEmployee(safeId).isPresent()) throw new NoSuchElementException("Employee not found.");
        return mutableStore().update(validatedEmployee(safeId, name, department, role, location, status));
    }

    private MutableEmployeeStore mutableStore() {
        if (!(store instanceof MutableEmployeeStore)) throw new IllegalStateException("This employee store is read-only.");
        return (MutableEmployeeStore) store;
    }

    private static Employee validatedEmployee(String id, String name, String department,
            String role, String location, String status) {
        return new Employee(id, requiredDirectoryText(name, "Name"), requiredDirectoryText(department, "Department"),
            requiredDirectoryText(role, "Role"), requiredDirectoryText(location, "Location"), validStatus(status));
    }

    private static String requiredDirectoryText(String value, String field) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " is required.");
        if (text.length() > 100) throw new IllegalArgumentException(field + " must be 100 characters or fewer.");
        return text;
    }

    private static String validStatus(String value) {
        String status = value == null ? "" : value.trim();
        if (!"Active".equals(status) && !"On leave".equals(status) && !"Separated".equals(status)) {
            throw new IllegalArgumentException("Status must be Active, On leave or Separated.");
        }
        return status;
    }

    /** Empty query returns all; search is case-insensitive across the displayed fields. */
    public List<Employee> searchEmployees(String query) {
        if (query == null || query.trim().isEmpty()) return store.findAll();
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        return store.findAll().stream()
            .filter(employee -> searchableText(employee).contains(normalized))
            .collect(Collectors.toList());
    }

    private String searchableText(Employee employee) {
        return (employee.getId() + " " + employee.getName() + " " + employee.getDepartment() + " "
            + employee.getRole() + " " + employee.getLocation() + " " + employee.getStatus())
            .toLowerCase(Locale.ROOT);
    }
}
