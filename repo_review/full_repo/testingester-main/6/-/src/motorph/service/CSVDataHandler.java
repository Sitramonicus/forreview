package motorph.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;
import motorph.model.ProbationaryEmployee;
import motorph.model.RegularEmployee;

/**
 * The only class that reads or writes the CSV files.
 * <p>
 * Converts raw CSV text into validated Employee and AttendanceRecord objects,
 * and writes objects back to CSV. This is also the only place that looks at
 * the Status column to decide between RegularEmployee and ProbationaryEmployee.
 * Switching from CSV to a database later would only change this class.
 * </p>
 */
public class CSVDataHandler {

    private static final Logger logger = Logger.getLogger(CSVDataHandler.class.getName());

    // === DEFAULT FILE PATHS ===
    public static final String DEFAULT_EMPLOYEE_CSV   = "data/employees.csv";
    public static final String DEFAULT_ATTENDANCE_CSV = "data/attendance.csv";
    public static final String DEFAULT_RESULTS_CSV    = "data/payroll_results.csv";

    /** Employee field names in employees.csv column order. Used for form labels and messages. */
    public static final List<String> EMPLOYEE_FIELD_NAMES = List.of(
        "Employee ID", "Last Name", "First Name", "Birthday", "Address", "Phone Number",
        "SSS Number", "PhilHealth Number", "TIN", "Pag-IBIG Number", "Status", "Position", "Supervisor",
        "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance", "Gross Semi-Monthly Rate", "Hourly Rate"
    );

    // === COLUMN INDEXES (employees.csv, no header row) ===
    private static final int EMPLOYEE_COLUMNS           = 19;
    private static final int COL_EMPLOYEE_ID            = 0;
    private static final int COL_LAST_NAME              = 1;
    private static final int COL_FIRST_NAME             = 2;
    private static final int COL_BIRTHDAY               = 3;
    private static final int COL_ADDRESS                = 4;
    private static final int COL_PHONE_NUMBER           = 5;
    private static final int COL_SSS_NUMBER             = 6;
    private static final int COL_PHILHEALTH_NUMBER      = 7;
    private static final int COL_TIN                    = 8;
    private static final int COL_PAGIBIG_NUMBER         = 9;
    private static final int COL_STATUS                 = 10;
    private static final int COL_POSITION               = 11;
    private static final int COL_SUPERVISOR             = 12;
    private static final int COL_BASIC_SALARY           = 13;
    private static final int COL_RICE_SUBSIDY           = 14;
    private static final int COL_PHONE_ALLOWANCE        = 15;
    private static final int COL_CLOTHING_ALLOWANCE     = 16;
    private static final int COL_GROSS_SEMI_MONTHLY_RATE = 17;
    private static final int COL_HOURLY_RATE            = 18;

    // === COLUMN INDEXES (attendance.csv, has a header row) ===
    private static final int ATTENDANCE_COLUMNS = 6;
    private static final int COL_ATT_EMPLOYEE_ID = 0;
    private static final int COL_ATT_DATE        = 3;
    private static final int COL_ATT_LOG_IN      = 4;
    private static final int COL_ATT_LOG_OUT     = 5;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT);

    private final Path employeeFile;
    private final Path attendanceFile;
    private final Path resultsFile;

    /** Uses the default files in the data/ folder. */
    public CSVDataHandler() {
        this(Path.of(DEFAULT_EMPLOYEE_CSV), Path.of(DEFAULT_ATTENDANCE_CSV), Path.of(DEFAULT_RESULTS_CSV));
    }

    /** Uses the given files. Lets tests work on copies instead of the real data. */
    public CSVDataHandler(Path employeeFile, Path attendanceFile, Path resultsFile) {
        this.employeeFile = employeeFile;
        this.attendanceFile = attendanceFile;
        this.resultsFile = resultsFile;
    }

    // === EMPLOYEES ===

    /**
     * Reads employees.csv into Employee objects. Blank or incomplete rows are
     * skipped; rows that fail validation are skipped and logged.
     *
     * @return the employees in file order
     * @throws IOException if the file cannot be read
     */
    public List<Employee> loadEmployees() throws IOException {
        List<Employee> employees = new ArrayList<>();
        for (String line : readLines(employeeFile)) {
            if (line.trim().isEmpty()) continue;
            String[] fields = parseCSVLine(line);
            if (fields.length < EMPLOYEE_COLUMNS) continue;
            try {
                employees.add(createEmployee(fields));
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Skipped invalid employee row {0}: {1}",
                        new Object[]{fields[COL_EMPLOYEE_ID], e.getMessage()});
            }
        }
        return employees;
    }

    /**
     * Builds a RegularEmployee or ProbationaryEmployee from 19 values in
     * employees.csv column order. Used for CSV rows and for the Add/Edit form.
     * This is the only place where the Status value picks the subclass.
     *
     * @param fields the 19 field values
     * @return the validated employee
     * @throws IllegalArgumentException with a user-readable message if a value is invalid
     */
    public Employee createEmployee(String[] fields) {
        if (fields == null || fields.length < EMPLOYEE_COLUMNS) {
            throw new IllegalArgumentException("All " + EMPLOYEE_COLUMNS + " employee fields are required.");
        }
        String[] f = new String[EMPLOYEE_COLUMNS];
        for (int i = 0; i < EMPLOYEE_COLUMNS; i++) {
            f[i] = fields[i] == null ? "" : fields[i].trim();
            if (f[i].isEmpty()) {
                throw new IllegalArgumentException(EMPLOYEE_FIELD_NAMES.get(i) + " is required.");
            }
        }

        LocalDate birthday          = parseDate(f[COL_BIRTHDAY], "Birthday");
        BigDecimal basicSalary      = parseAmount(f, COL_BASIC_SALARY);
        BigDecimal riceSubsidy      = parseAmount(f, COL_RICE_SUBSIDY);
        BigDecimal phoneAllowance   = parseAmount(f, COL_PHONE_ALLOWANCE);
        BigDecimal clothingAllowance = parseAmount(f, COL_CLOTHING_ALLOWANCE);
        BigDecimal grossSemiMonthly = parseAmount(f, COL_GROSS_SEMI_MONTHLY_RATE);
        BigDecimal hourlyRate       = parseAmount(f, COL_HOURLY_RATE);

        String status = f[COL_STATUS];
        if (status.equalsIgnoreCase(Employee.STATUS_REGULAR)) {
            return new RegularEmployee(f[COL_EMPLOYEE_ID], f[COL_LAST_NAME], f[COL_FIRST_NAME], birthday,
                    f[COL_ADDRESS], f[COL_PHONE_NUMBER], f[COL_SSS_NUMBER], f[COL_PHILHEALTH_NUMBER], f[COL_TIN],
                    f[COL_PAGIBIG_NUMBER], f[COL_POSITION], f[COL_SUPERVISOR], basicSalary, riceSubsidy,
                    phoneAllowance, clothingAllowance, grossSemiMonthly, hourlyRate);
        }
        if (status.equalsIgnoreCase(Employee.STATUS_PROBATIONARY)) {
            return new ProbationaryEmployee(f[COL_EMPLOYEE_ID], f[COL_LAST_NAME], f[COL_FIRST_NAME], birthday,
                    f[COL_ADDRESS], f[COL_PHONE_NUMBER], f[COL_SSS_NUMBER], f[COL_PHILHEALTH_NUMBER], f[COL_TIN],
                    f[COL_PAGIBIG_NUMBER], f[COL_POSITION], f[COL_SUPERVISOR], basicSalary, riceSubsidy,
                    phoneAllowance, clothingAllowance, grossSemiMonthly, hourlyRate);
        }
        throw new IllegalArgumentException("Status must be Regular or Probationary.");
    }

    /**
     * Returns an employee's 19 values in employees.csv column order, as text.
     * Used to write the CSV file and to fill the Edit form.
     */
    public String[] toFields(Employee employee) {
        return new String[] {
            employee.getEmployeeId(), employee.getLastName(), employee.getFirstName(),
            employee.getBirthday().format(DATE_FORMAT), employee.getAddress(), employee.getPhoneNumber(),
            employee.getSssNumber(), employee.getPhilHealthNumber(), employee.getTin(), employee.getPagIbigNumber(),
            employee.getStatus(), employee.getPosition(), employee.getImmediateSupervisor(),
            employee.getBasicSalary().toPlainString(), employee.getRiceSubsidy().toPlainString(),
            employee.getPhoneAllowance().toPlainString(), employee.getClothingAllowance().toPlainString(),
            employee.getGrossSemiMonthlyRate().toPlainString(), employee.getHourlyRate().toPlainString()
        };
    }

    /**
     * Rewrites employees.csv with the given list. Called after add, edit, or delete.
     *
     * @throws IOException if the file cannot be written
     */
    public void saveEmployees(List<Employee> employees) throws IOException {
        if (employees == null) {
            throw new IllegalArgumentException("Employee list is required.");
        }
        try (BufferedWriter writer = Files.newBufferedWriter(employeeFile, StandardCharsets.UTF_8)) {
            for (Employee employee : employees) {
                writer.write(joinCsvRow(toFields(employee)));
                writer.newLine();
            }
        }
    }

    /**
     * Finds an employee by ID in employees.csv.
     *
     * @return the employee, or null if no employee has that ID
     * @throws IOException if the file cannot be read
     */
    public Employee findEmployeeById(String employeeId) throws IOException {
        if (employeeId == null) return null;
        String id = employeeId.trim();
        for (Employee employee : loadEmployees()) {
            if (employee.getEmployeeId().equals(id)) return employee;
        }
        return null;
    }

    /**
     * Returns the next available employee ID: the highest existing ID + 1.
     *
     * @throws IOException if the file cannot be read
     */
    public String generateNextEmployeeId() throws IOException {
        long highestId = 0;
        for (Employee employee : loadEmployees()) {
            highestId = Math.max(highestId, Long.parseLong(employee.getEmployeeId()));
        }
        return String.valueOf(highestId + 1);
    }

    // === ATTENDANCE ===

    /**
     * Reads attendance.csv once and groups the records by employee ID, in file
     * order. Skips the header row and incomplete or invalid rows.
     *
     * @return employee ID mapped to that employee's attendance records
     * @throws IOException if the file cannot be read
     */
    public Map<String, List<AttendanceRecord>> loadAttendance() throws IOException {
        Map<String, List<AttendanceRecord>> attendance = new LinkedHashMap<>();
        List<String> lines = readLines(attendanceFile);
        for (int i = 1; i < lines.size(); i++) {           // line 0 is the header
            String[] fields = parseCSVLine(lines.get(i));
            if (fields.length < ATTENDANCE_COLUMNS || fields[COL_ATT_EMPLOYEE_ID].isEmpty()) continue;
            try {
                AttendanceRecord record = new AttendanceRecord(
                        fields[COL_ATT_EMPLOYEE_ID],
                        LocalDate.parse(fields[COL_ATT_DATE], DATE_FORMAT),
                        LocalTime.parse(fields[COL_ATT_LOG_IN], TIME_FORMAT),
                        LocalTime.parse(fields[COL_ATT_LOG_OUT], TIME_FORMAT));
                attendance.computeIfAbsent(record.getEmployeeId(), k -> new ArrayList<>()).add(record);
            } catch (DateTimeParseException | IllegalArgumentException e) {
                logger.log(Level.WARNING, "Skipped invalid attendance row {0}: {1}",
                        new Object[]{i + 1, e.getMessage()});
            }
        }
        return attendance;
    }

    // === PAYROLL RESULTS ===

    /**
     * Writes payslips to payroll_results.csv, replacing its contents. Columns:
     * Employee ID, Name, Hourly Rate, Hours Worked, Gross Pay, each deduction
     * (SSS, PhilHealth, Pag-IBIG, Withholding Tax), Total Deductions, Net Pay.
     *
     * @throws IllegalArgumentException if the list is empty
     * @throws IOException if the file cannot be written
     */
    public void savePayrollResults(List<Payslip> payslips) throws IOException {
        if (payslips == null || payslips.isEmpty()) {
            throw new IllegalArgumentException("There are no payroll results to save.");
        }
        List<String> header = new ArrayList<>(List.of("Employee ID", "Name", "Hourly Rate", "Hours Worked", "Gross Pay"));
        header.addAll(payslips.get(0).getDeductionBreakdown().keySet());
        header.addAll(List.of("Total Deductions", "Net Pay"));

        try (BufferedWriter writer = Files.newBufferedWriter(resultsFile, StandardCharsets.UTF_8)) {
            writer.write(joinCsvRow(header.toArray(new String[0])));
            writer.newLine();
            for (Payslip payslip : payslips) {
                writer.write(payslip.toCSVRow());
                writer.newLine();
            }
        }
    }

    // === CSV HELPERS ===

    private static List<String> readLines(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("File not found: " + file);
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return Collections.unmodifiableList(lines);
    }

    /**
     * Splits a CSV line by commas, keeping commas inside quotes (for example an
     * address) as part of one value. A doubled quote inside quotes is one quote.
     */
    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(new String[0]);
    }

    /**
     * Quotes a field that contains a comma, quote, or line break, or starts or
     * ends with a space, and doubles any quotes inside it.
     */
    private static String escapeCsvField(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.startsWith(" ") || value.endsWith(" ");
        String escaped = value.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }

    private static String joinCsvRow(String[] values) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) row.append(',');
            row.append(escapeCsvField(values[i]));
        }
        return row.toString();
    }

    private static LocalDate parseDate(String text, String fieldName) {
        try {
            return LocalDate.parse(text, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid date in MM/DD/YYYY format.");
        }
    }

    private static BigDecimal parseAmount(String[] fields, int column) {
        try {
            return new BigDecimal(fields[column]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(EMPLOYEE_FIELD_NAMES.get(column) + " must be a valid number.");
        }
    }
}
