package ph.motorph.o_oop;

import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.InMemoryEmployeeStore;
import java.util.List;
import java.util.Optional;

/** Small dependency-free test runner so this project can be checked with a plain JDK. */
public final class EmployeeServiceTest {
    private int checks;

    public static void main(String[] args) {
        new EmployeeServiceTest().run();
    }

    private void run() {
        // The directory is MotorPH's own employee file, not a fixture: 34 people, IDs 10001-10034.
        InMemoryEmployeeStore store = new InMemoryEmployeeStore();
        for (Employee employee : EmployeeImport.fromClasspath().employees()) store.seed(employee);
        EmployeeService service = new EmployeeService(store);

        List<Employee> all = service.listEmployees();
        equal(34, all.size(), "the directory holds every row of the employee file");
        equal("10001", all.get(0).getId(), "directory order follows the file");

        List<Employee> executive = service.searchEmployees("executive office");
        equal(1, executive.size(), "search matches department case-insensitively");
        equal("Manuel III Garcia", executive.get(0).getName(), "department search returns the right person");

        List<Employee> marketing = service.searchEmployees("marketing");
        equal(2, marketing.size(), "the marketing department holds the CMO and the sales & marketing role");
        check(marketing.stream().anyMatch(e -> "10004".equals(e.getId())), "the CMO is found by department");

        equal(1, service.searchEmployees("10015").size(), "search matches employee ID");
        equal(34, service.searchEmployees("  ").size(), "blank search returns all employees");
        equal(0, service.searchEmployees("not a person").size(), "no match returns an empty list");

        Optional<Employee> found = service.findEmployee(" 10015 ");
        check(found.isPresent(), "employee lookup trims the supplied ID");
        equal("Romualdez", found.get().getLastName(), "employee lookup returns the expected record");
        check(!service.findEmployee("MISSING").isPresent(), "unknown ID has no employee");

        expectIllegalArgument(() -> Employee.hired(" ", "Name", "Dept", "Role", "Place", "Active"),
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
