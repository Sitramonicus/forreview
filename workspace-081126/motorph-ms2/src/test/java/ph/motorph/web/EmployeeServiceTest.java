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
        equal(34, all.size(), "the directory holds all 34 rows of MotorPH's employee file");
        equal("10001", all.get(0).getId(), "directory order follows the source file");

        List<Employee> finance = service.searchEmployees("finance");
        equal(1, finance.size(), "search matches position text case-insensitively");
        equal("Bianca Sofia Aquino", finance.get(0).getName(), "department search returns correct person");

        List<Employee> accounting = service.searchEmployees("ACCOUNTING");
        equal(18, accounting.size(), "all 18 Accounting positions are found by department");

        List<Employee> city = service.searchEmployees("TAGUIG");
        equal(1, city.size(), "search matches location case-insensitively");
        equal("10012", city.get(0).getId(), "location search returns correct record");

        equal(1, service.searchEmployees("10003").size(), "search matches employee number");
        equal(34, service.searchEmployees("  ").size(), "blank search returns all employees");
        equal(0, service.searchEmployees("not a person").size(), "no match returns an empty list");

        Optional<Employee> found = service.findEmployee(" 10002 ");
        check(found.isPresent(), "employee lookup trims the supplied ID");
        equal("Executive", found.get().getDepartment(), "employee lookup returns the expected record");
        // The golden-case employee must exist with the supplied payslip's pay profile.
        Optional<Employee> golden = service.findEmployee("10015");
        check(golden.isPresent(), "employee 10015 from the supplied payslip exists");
        equal("53500.00", golden.get().getMonthlyBasic().toPlainString(), "10015 has the payslip's basic pay");
        check(golden.get().hasPayProfile(), "10015 carries a complete pay profile");
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
