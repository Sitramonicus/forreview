package ph.motorph.web;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
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
        return createEmployee(name, department, role, location, status, null, null, null, null);
    }

    /** Creates a directory record with the pay inputs the payslip layout requires. */
    public Employee createEmployee(String name, String department, String role, String location, String status,
                                   java.math.BigDecimal monthlyBasic, java.math.BigDecimal riceSubsidy,
                                   java.math.BigDecimal phoneAllowance, java.math.BigDecimal clothingAllowance) {
        MutableEmployeeStore mutable = mutableStore();
        Employee employee = validatedEmployee(nextEmployeeNumber(), name, department, role, location, status,
            monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance);
        return mutable.create(employee);
    }

    /** Updates the directory fields and, when supplied, the pay inputs used by payroll. */
    public Employee updateEmployee(String id, String name, String department, String role, String location, String status) {
        return updateEmployee(id, name, department, role, location, status, null, null, null, null);
    }

    public Employee updateEmployee(String id, String name, String department, String role, String location, String status,
                                   java.math.BigDecimal monthlyBasic, java.math.BigDecimal riceSubsidy,
                                   java.math.BigDecimal phoneAllowance, java.math.BigDecimal clothingAllowance) {
        String safeId = id == null ? "" : id.trim();
        if (!Employee.isValidEmployeeNumber(safeId)) throw new IllegalArgumentException("Employee ID is invalid.");
        Employee existing = findEmployee(safeId)
            .orElseThrow(() -> new NoSuchElementException("Employee not found."));
        // Omitted pay fields keep their stored values instead of being silently zeroed.
        java.math.BigDecimal basic = monthlyBasic == null ? existing.getMonthlyBasic() : monthlyBasic;
        java.math.BigDecimal rice = riceSubsidy == null ? existing.getRiceSubsidy() : riceSubsidy;
        java.math.BigDecimal phone = phoneAllowance == null ? existing.getPhoneAllowance() : phoneAllowance;
        java.math.BigDecimal clothing = clothingAllowance == null ? existing.getClothingAllowance() : clothingAllowance;
        return mutableStore().update(validatedEmployee(safeId, name, department, role, location, status,
            basic, rice, phone, clothing));
    }

    private MutableEmployeeStore mutableStore() {
        if (!(store instanceof MutableEmployeeStore)) throw new IllegalStateException("This employee store is read-only.");
        return (MutableEmployeeStore) store;
    }

    private static Employee validatedEmployee(String id, String name, String department,
            String role, String location, String status, java.math.BigDecimal monthlyBasic,
            java.math.BigDecimal riceSubsidy, java.math.BigDecimal phoneAllowance,
            java.math.BigDecimal clothingAllowance) {
        return new Employee(id, requiredDirectoryText(name, "Name"), requiredDirectoryText(department, "Department"),
            requiredDirectoryText(role, "Role"), requiredDirectoryText(location, "Location"), validStatus(status),
            monthlyBasic, riceSubsidy, phoneAllowance, clothingAllowance);
    }

    private static String requiredDirectoryText(String value, String field) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " is required.");
        if (text.length() > 100) throw new IllegalArgumentException(field + " must be 100 characters or fewer.");
        return text;
    }

    private static String validStatus(String value) {
        String status = value == null ? "" : value.trim();
        if (!"Active".equals(status) && !"On leave".equals(status)) {
            throw new IllegalArgumentException("Status must be Active or On leave.");
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

    /** Next free number after the highest employee number in this directory, so new records sit
     *  in MotorPH's own 5-digit series instead of a separate "demo" series. */
    private String nextEmployeeNumber() {
        int highest = 0;
        for (Employee existing : store.findAll()) {
            try {
                highest = Math.max(highest, Integer.parseInt(existing.getId()));
            } catch (NumberFormatException ignored) {
                // Non-numeric legacy IDs never set the ceiling.
            }
        }
        return String.valueOf(Math.max(10001, highest + 1));
    }
}
