package ph.motorph.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** User-scoped demo employee records read through the Firestore REST API. */
public final class FirestoreEmployeeStore implements MutableEmployeeStore {
    private final FirestoreRestClient client;

    public FirestoreEmployeeStore(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    @Override
    public List<Employee> findAll() {
        List<Employee> employees = new ArrayList<>();
        for (JsonObject document : client.listDocuments(client.userCollection("demoEmployees"))) {
            employees.add(fromDocument(document));
        }
        return employees;
    }

    @Override
    public Optional<Employee> findById(String id) {
        if (id == null) return Optional.empty();
        return findAll().stream().filter(employee -> employee.getId().equals(id)).findFirst();
    }

    @Override
    public Employee create(Employee employee) {
        if (!client.createDocument(client.userCollection("demoEmployees"), employee.getId(), employeeFields(employee))) {
            throw new IllegalStateException("Employee ID already exists; refresh the directory and retry.");
        }
        return employee;
    }

    @Override
    public Employee update(Employee employee) {
        if (!client.updateDocument(client.userCollection("demoEmployees"), employee.getId(), employeeFields(employee),
            java.util.Arrays.asList("id", "name", "department", "role", "location", "status"))) {
            throw new java.util.NoSuchElementException("Employee not found.");
        }
        return employee;
    }

    private static JsonObject employeeFields(Employee employee) {
        JsonObject fields = new JsonObject();
        fields.add("id", stringField(employee.getId()));
        fields.add("name", stringField(employee.getName()));
        fields.add("department", stringField(employee.getDepartment()));
        fields.add("role", stringField(employee.getRole()));
        fields.add("location", stringField(employee.getLocation()));
        fields.add("status", stringField(employee.getStatus()));
        // Pay inputs are written only when present so directory-only records stay salary-free.
        if (employee.getMonthlyBasic() != null) {
            fields.add("monthlyBasic", stringField(employee.getMonthlyBasic().toPlainString()));
            fields.add("riceSubsidy", stringField(nullToEmpty(employee.getRiceSubsidy())));
            fields.add("phoneAllowance", stringField(nullToEmpty(employee.getPhoneAllowance())));
            fields.add("clothingAllowance", stringField(nullToEmpty(employee.getClothingAllowance())));
        }
        return fields;
    }

    private static String nullToEmpty(java.math.BigDecimal value) {
        return value == null ? "0.00" : value.toPlainString();
    }

    private static java.math.BigDecimal amountOrNull(JsonObject fields, String name) {
        String text = stringValue(fields, name);
        return text.isEmpty() ? null : Money.parse(text);
    }

    private static JsonObject stringField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("stringValue", value);
        return field;
    }

    private static Employee fromDocument(JsonObject document) {
        JsonObject fields = document.has("fields") ? document.getAsJsonObject("fields") : new JsonObject();
        return new Employee(
            stringValue(fields, "id"), stringValue(fields, "name"), stringValue(fields, "department"),
            stringValue(fields, "role"), stringValue(fields, "location"), stringValue(fields, "status"),
            amountOrNull(fields, "monthlyBasic"), amountOrNull(fields, "riceSubsidy"),
            amountOrNull(fields, "phoneAllowance"), amountOrNull(fields, "clothingAllowance"));
    }

    static String stringValue(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return "";
        JsonElement value = element.getAsJsonObject().get("stringValue");
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }
}
