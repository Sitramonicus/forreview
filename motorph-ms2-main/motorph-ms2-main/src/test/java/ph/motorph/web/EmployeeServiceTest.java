package ph.motorph.web;

import java.util.List;
import java.util.Optional;

/** Small dependency-free test runner so this project can be checked with a plain JDK. */
public final class EmployeeServiceTest {
    private int checks;

    public static void main(String[] args) {
        new EmployeeServiceTest().run();
    }

    private void run() {
        EmployeeService service = new EmployeeService(new InMemoryEmployeeStore());

        List<Employee> all = service.listEmployees();
        equal(5, all.size(), "the demo directory has five records (one per role)");
        equal("DEMO-001", all.get(0).getId(), "directory order is stable");

        List<Employee> finance = service.searchEmployees("finance");
        equal(1, finance.size(), "search matches department case-insensitively");
        equal("Eli Santos", finance.get(0).getName(), "department search returns correct person");

        List<Employee> city = service.searchEmployees("TAGUIG");
        equal(1, city.size(), "search matches location case-insensitively");
        equal("DEMO-002", city.get(0).getId(), "location search returns correct record");

        equal(1, service.searchEmployees("demo-003").size(), "search matches employee ID");
        equal(5, service.searchEmployees("  ").size(), "blank search returns all employees");
        equal(0, service.searchEmployees("not a person").size(), "no match returns an empty list");

        Optional<Employee> found = service.findEmployee(" DEMO-002 ");
        check(found.isPresent(), "employee lookup trims the supplied ID");
        equal("Finance", found.get().getDepartment(), "employee lookup returns the expected record");
        check(!service.findEmployee("MISSING").isPresent(), "unknown ID has no employee");

        expectIllegalArgument(() -> new Employee(" ", "Name", "Dept", "Role", "Place", "Active"),
            "employee ID cannot be blank");
        System.out.println("PASS: " + checks + " employee-domain checks");
    }

    private void equal(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    private void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private void expectIllegalArgument(Runnable action, String message) {
        checks++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message);
    }
}
