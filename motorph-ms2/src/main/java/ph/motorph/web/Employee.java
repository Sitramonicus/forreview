package ph.motorph.web;

import java.util.Objects;

/** A small employee view model for the first synthetic-data prototype. */
public final class Employee {
    private final String id;
    private final String name;
    private final String department;
    private final String role;
    private final String location;
    private final String status;

    public Employee(String id, String name, String department, String role, String location, String status) {
        this.id = requireText(id, "id");
        this.name = requireText(name, "name");
        this.department = requireText(department, "department");
        this.role = requireText(role, "role");
        this.location = requireText(location, "location");
        this.status = requireText(status, "status");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        if (value.trim().isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return value.trim();
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
}
