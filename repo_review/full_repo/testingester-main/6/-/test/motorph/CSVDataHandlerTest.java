package motorph;

import static motorph.TestSupport.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.ProbationaryEmployee;
import motorph.model.RegularEmployee;
import motorph.service.CSVDataHandler;

/**
 * Tests CSV parsing, subclass creation, and saving. Reads the real data files;
 * all writes go to temporary copies so data/ is never changed.
 */
public class CSVDataHandlerTest {

    public static void main(String[] args) throws IOException {
        run();
        finish();
    }

    static void run() throws IOException {
        CSVDataHandler csv = new CSVDataHandler();

        section("CSVDataHandler: loadEmployees()");
        List<Employee> employees = csv.loadEmployees();
        checkEquals(34, employees.size(), "all 34 rows of employees.csv load");
        long regular = employees.stream().filter(e -> e instanceof RegularEmployee).count();
        long probationary = employees.stream().filter(e -> e instanceof ProbationaryEmployee).count();
        checkEquals(23L, regular, "23 rows become RegularEmployee");
        checkEquals(11L, probationary, "11 rows become ProbationaryEmployee (10021 to 10031)");
        checkEquals("Probationary", csv.findEmployeeById("10021").getEmploymentType(), "10021 is Probationary");
        checkEquals("Regular", csv.findEmployeeById("10020").getEmploymentType(), "10020 is Regular");
        checkEquals(null, csv.findEmployeeById("99999"), "unknown ID returns null");
        checkEquals("10035", csv.generateNextEmployeeId(), "next ID = highest (10034) + 1");

        section("CSVDataHandler: loadAttendance()");
        Map<String, List<AttendanceRecord>> attendance = csv.loadAttendance();
        int total = attendance.values().stream().mapToInt(List::size).sum();
        checkEquals(5168, total, "all 5,168 attendance records load (header skipped)");
        checkEquals(34, attendance.size(), "records grouped under 34 employee IDs");

        section("CSVDataHandler: createEmployee() from form values");
        String[] fields = csv.toFields(csv.findEmployeeById("10021"));
        fields[10] = "Regular";
        check(csv.createEmployee(fields) instanceof RegularEmployee, "changing Status to Regular builds a RegularEmployee");
        String[] bad = fields.clone();
        bad[13] = "abc";
        checkRejects(() -> csv.createEmployee(bad), "non-numeric Basic Salary rejected");
        String[] blank = fields.clone();
        blank[4] = " ";
        checkRejects(() -> csv.createEmployee(blank), "blank Address rejected");
        String[] badDate = fields.clone();
        badDate[3] = "02/30/1990";
        checkRejects(() -> csv.createEmployee(badDate), "impossible birthday rejected");
        String[] badStatus = fields.clone();
        badStatus[10] = "Contractual";
        checkRejects(() -> csv.createEmployee(badStatus), "unknown status rejected");

        section("CSVDataHandler: saveEmployees() round trip (temporary copy)");
        Path tempDir = Files.createTempDirectory("motorph-test");
        Path employeeCopy = tempDir.resolve("employees.csv");
        Files.copy(Path.of(CSVDataHandler.DEFAULT_EMPLOYEE_CSV), employeeCopy, StandardCopyOption.REPLACE_EXISTING);
        CSVDataHandler copy = new CSVDataHandler(employeeCopy, Path.of(CSVDataHandler.DEFAULT_ATTENDANCE_CSV),
                tempDir.resolve("payroll_results.csv"));
        List<Employee> loaded = copy.loadEmployees();
        copy.saveEmployees(loaded);
        checkEquals(Files.readAllLines(Path.of(CSVDataHandler.DEFAULT_EMPLOYEE_CSV)), Files.readAllLines(employeeCopy),
                "saving unchanged employees reproduces employees.csv line for line");

        String[] withComma = copy.toFields(loaded.get(0));
        withComma[4] = "Unit 5, \"Tower\" A, Makati";
        loaded.set(0, copy.createEmployee(withComma));
        copy.saveEmployees(loaded);
        checkEquals("Unit 5, \"Tower\" A, Makati", copy.findEmployeeById("10001").getAddress(),
                "address with commas and quotes survives save and reload");
        checkEquals(34, copy.loadEmployees().size(), "still 34 employees after save");
    }
}
