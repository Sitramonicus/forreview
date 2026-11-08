import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import motorphemployee.AttendanceRepository;
import motorphemployee.oop.AttendanceLog;
import motorphemployee.oop.Employee;
import motorphemployee.oop.PayrollCalculator;
import motorphemployee.oop.PayrollRecord;
import motorphemployee.oop.RegularEmployee;

/**
 * Console evidence that payroll is driven by the company timecard, not by a hardcoded
 * working-day table (known issues list row 16 / the fake attendance block in the screens).
 *
 * <p>Reads the real {@code src/motorphemployee/attendance.csv} (5,168 rows exported from the
 * "Attendance Record" sheet of {@code Copy of MotorPH_Employee Data.xlsx}) and checks the
 * counts the screens rely on.</p>
 */
public final class AttendanceRepositoryTest {

    private static final String ATTENDANCE = "src/motorphemployee/attendance.csv";
    private static final String EMPLOYEE_DATA = "src/motorphemployee/data.csv";
    /** Employee 1 in data.csv = employee 10001 in the source sheet (Garcia, Manuel III). */
    private static final int EMPLOYEE = 1;

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        System.out.println("AttendanceRepository console evidence - real timecard feeds the payslip");
        System.out.println();

        theTimecardLoads();
        everyEmployeeHasEveryMonth();
        workedDaysMatchTheTimecard();
        payrollUsesTheTimecardDays();
        aBadTimecardRowIsRefused();
        anEmployeeWithoutRecordsIsHandled();

        System.out.println();
        System.out.println("Summary: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------- scenarios

    private static void theTimecardLoads() throws IOException {
        start("the timecard file loads at all");
        AttendanceRepository repository = AttendanceRepository.forProjectFiles();
        List<AttendanceRepository.Record> rows = repository.records();

        check("5,168 rows", 5168, rows.size());
        check("34 employees", 34, distinctEmployees(rows));
        check("every row has a clock-in and a clock-out", 0,
                rows.stream().filter(r -> r.clockIn.isEmpty() || r.clockOut.isEmpty()).count());
        check("no duplicate employee/day rows", 0, duplicates(rows));
        check("first row is employee 1 on 2025-06-03", "1|2025-06-03",
                rows.get(0).employeeId + "|" + rows.get(0).dateText());
        check("last row is employee 34 on 2025-12-31", "34|2025-12-31",
                rows.get(rows.size() - 1).employeeId + "|" + rows.get(rows.size() - 1).dateText());
    }

    private static void everyEmployeeHasEveryMonth() throws IOException {
        start("each employee gets the seven timecard months, oldest first");
        AttendanceRepository repository = AttendanceRepository.forProjectFiles();
        List<YearMonth> months = repository.monthsWithRecords(EMPLOYEE);

        check("7 months", 7, months.size());
        check("June 2025 first", "2025-06", months.get(0).toString());
        check("December 2025 last", "2025-12", months.get(months.size() - 1).toString());
        check("picker label", "June 2025", AttendanceRepository.labelOf(months.get(0)));
        check("period start", "2025-06-01", AttendanceRepository.periodStartOf(months.get(0)));
        check("period end", "2025-06-30", AttendanceRepository.periodEndOf(months.get(0)));
        check("152 timecard days in total", 152, repository.recordsFor(EMPLOYEE,
                YearMonth.of(2025, 6)).size() + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 7)).size()
                + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 8)).size()
                + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 9)).size()
                + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 10)).size()
                + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 11)).size()
                + repository.recordsFor(EMPLOYEE, YearMonth.of(2025, 12)).size());
    }

    private static void workedDaysMatchTheTimecard() throws IOException {
        start("worked days per month match the timecard (not a 2024 calendar table)");
        AttendanceRepository repository = AttendanceRepository.forProjectFiles();
        int[] expected = {20, 23, 22, 21, 23, 21, 22};
        YearMonth[] months = {
            YearMonth.of(2025, 6), YearMonth.of(2025, 7), YearMonth.of(2025, 8), YearMonth.of(2025, 9),
            YearMonth.of(2025, 10), YearMonth.of(2025, 11), YearMonth.of(2025, 12)
        };
        for (int i = 0; i < months.length; i++) {
            AttendanceLog log = repository.loadFor(EMPLOYEE, months[i]);
            int days = log.getWorkedDays(EMPLOYEE,
                    AttendanceRepository.periodStartOf(months[i]), AttendanceRepository.periodEndOf(months[i]));
            check("worked days in " + AttendanceRepository.labelOf(months[i]), expected[i], days);
        }
        AttendanceLog june = repository.loadFor(EMPLOYEE, YearMonth.of(2025, 6));
        check("2025-06-03 has the recorded hours", true, june.getDailyHours(EMPLOYEE, "2025-06-03") > 8.0);
        check("a day with no timecard row has 0 hours", 0.0, june.getDailyHours(EMPLOYEE, "2025-06-08"));
    }

    private static void payrollUsesTheTimecardDays() throws IOException {
        start("the payslip is priced from the timecard days (July 2025 = 23 days)");
        AttendanceRepository repository = AttendanceRepository.forProjectFiles();
        YearMonth july = YearMonth.of(2025, 7);
        Employee employee = employeeOne();
        AttendanceLog log = repository.loadFor(EMPLOYEE, july);

        PayrollRecord payslip = new PayrollCalculator().calculatePayroll(employee, log,
                AttendanceRepository.periodStartOf(july), AttendanceRepository.periodEndOf(july));

        double expectedGross = 23 * employee.getHourlyRate() * 8.0;
        check("gross = 23 timecard days x hourly rate x 8", String.format("%.2f", expectedGross),
                String.format("%.2f", payslip.getGrossIncome()));
        check("gross differs from a 20-day month", true,
                Math.abs(payslip.getGrossIncome() - (20 * employee.getHourlyRate() * 8.0)) > 0.01);
        check("payroll hours = 23 days x 8", String.format("%.2f", 23 * 8.0),
                String.format("%.2f", payslip.getHoursWorked()));
        check("payslip period starts on the timecard month", "2025-07-01",
                payslip.getPeriodStart().toLocalDate().toString());
        check("net pay is positive", true, payslip.getNetIncome() > 0);
    }

    private static void aBadTimecardRowIsRefused() throws IOException {
        start("a corrupt timecard row stops the payslip instead of shrinking it");
        Path temp = Files.createTempDirectory("attendance-test");
        Path broken = temp.resolve("attendance.csv");
        Files.write(broken, ("Employee #,Date,Log In,Log Out\r\n"
                + "10001,2025-06-03,08:59:00,18:31:00\r\n"
                + "10001,06/04/2025,09:00:00,18:00:00\r\n").getBytes(StandardCharsets.UTF_8));

        String message = "";
        boolean refused = false;
        try {
            new AttendanceRepository(broken.toString()).records();
        } catch (IOException expected) {
            refused = true;
            message = expected.getMessage();
        }
        check("refused", true, refused);
        check("the message names the line and the expected format", true,
                message.contains("line 3") && message.contains("yyyy-MM-dd"));

        Path duplicate = temp.resolve("duplicate.csv");
        Files.write(duplicate, ("Employee #,Date,Log In,Log Out\r\n"
                + "1,2025-06-03,08:59:00,18:31:00\r\n"
                + "1,2025-06-03,09:00:00,18:00:00\r\n").getBytes(StandardCharsets.UTF_8));
        boolean duplicateRefused = false;
        try {
            new AttendanceRepository(duplicate.toString()).loadFor(EMPLOYEE, YearMonth.of(2025, 6));
        } catch (IllegalArgumentException expected) {
            duplicateRefused = expected.getMessage().contains("duplicate clock-in");
        }
        check("a duplicate clock-in is refused by AttendanceLog", true, duplicateRefused);
    }

    private static void anEmployeeWithoutRecordsIsHandled() throws IOException {
        start("an employee with no timecard rows is reported, not priced from an assumption");
        AttendanceRepository repository = AttendanceRepository.forProjectFiles();
        check("no months offered", 0, repository.monthsWithRecords(99999).size());
        check("hasRecords is false", false, repository.hasRecords(99999));
        AttendanceLog empty = repository.loadFor(99999, YearMonth.of(2025, 7));
        check("empty log has 0 worked days", 0,
                empty.getWorkedDays(99999, "2025-07-01", "2025-07-31"));
    }

    // ------------------------------------------------------------------ helpers

    private static Employee employeeOne() throws IOException {
        for (String line : Files.readAllLines(Paths.get(EMPLOYEE_DATA), StandardCharsets.UTF_8)) {
            String[] cells = line.split(",", -1);
            if (cells.length > 18 && cells[0].trim().equals("1")) {
                return new RegularEmployee(
                        Integer.parseInt(cells[0].trim()), cells[1].trim(), cells[2].trim(),
                        cells[3].trim(), cells[4].trim(), cells[5].trim(), cells[6].trim(),
                        cells[7].trim(), cells[8].trim(), cells[9].trim(), cells[10].trim(),
                        cells[11].trim(), cells[12].trim(),
                        Double.parseDouble(cells[13].trim()), Double.parseDouble(cells[14].trim()),
                        Double.parseDouble(cells[15].trim()), Double.parseDouble(cells[16].trim()),
                        Double.parseDouble(cells[17].trim()), Double.parseDouble(cells[18].trim()), 0);
            }
        }
        throw new IOException("employee 10001 not found in " + EMPLOYEE_DATA);
    }

    private static int distinctEmployees(List<AttendanceRepository.Record> rows) {
        List<Integer> ids = new ArrayList<>();
        for (AttendanceRepository.Record row : rows) {
            if (!ids.contains(row.employeeId)) {
                ids.add(row.employeeId);
            }
        }
        return ids.size();
    }

    private static long duplicates(List<AttendanceRepository.Record> rows) {
        List<String> seen = new ArrayList<>();
        long duplicates = 0;
        for (AttendanceRepository.Record row : rows) {
            String key = row.employeeId + "|" + row.dateText();
            if (seen.contains(key)) {
                duplicates++;
            } else {
                seen.add(key);
            }
        }
        return duplicates;
    }

    private static void start(String name) {
        System.out.println("[" + name + "]");
    }

    private static void check(String label, Object expected, Object actual) {
        boolean ok = String.valueOf(expected).equals(String.valueOf(actual));
        if (ok) {
            passed++;
            System.out.println("    PASS  " + label);
        } else {
            failed++;
            System.out.println("    FAIL  " + label + " (expected <" + expected + ">, got <" + actual + ">)");
        }
    }
}
