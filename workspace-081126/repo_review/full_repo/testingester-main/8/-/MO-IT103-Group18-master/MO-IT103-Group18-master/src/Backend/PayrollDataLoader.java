package Backend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

// Helper class that loads the CSV databases into backend objects (Employee, AttendanceRecord),
// so the GUI never parses payroll data or does hours/salary math itself.
// It also keeps every CSV path in ONE place, and finds the databases folder whether the
// app is run from NetBeans (project folder) or from the repository root.
public class PayrollDataLoader {

    private static final String DATABASES_FOLDER = resolveDatabasesFolder();

    public static final String EMPLOYEE_DETAILS_CSV =
            DATABASES_FOLDER + "MotorPH Employee Data - Employee Details.csv";
    public static final String ATTENDANCE_RECORD_CSV =
            DATABASES_FOLDER + "Copy of MotorPH Employee Data - Attendance Record.csv";
    public static final String CREDENTIALS_CSV =
            DATABASES_FOLDER + "MotorPH Employee Data - Credentials.csv";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private PayrollDataLoader() {
        // utility class: not meant to be instantiated
    }

    // Tries the possible locations of the databases folder and uses the first one that exists
    private static String resolveDatabasesFolder() {
        String[] candidates = {
            "src/payroll/hub/databases/",                                        // run from NetBeans project folder
            "MO-IT103-Group18-master/src/payroll/hub/databases/",                // run from one level up
            "MO-IT103-Group18-master/MO-IT103-Group18-master/src/payroll/hub/databases/" // run from repo root
        };
        for (String path : candidates) {
            if (new File(path).isDirectory()) {
                return path;
            }
        }
        return candidates[0];
    }

    // Splits a CSV line on commas that are not inside quotes
    public static String[] parseCSVLine(String line) {
        String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        for (int i = 0; i < fields.length; i++) {
            fields[i] = fields[i].trim().replaceAll("^\"|\"$", "");
        }
        return fields;
    }

    // Converts values like "90,000" or "1,500" to a number
    private static double parseNumber(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return Double.parseDouble(value.replace(",", "").trim());
    }

    // Returns every employee ID in the Employee Details CSV
    public static List<String> loadEmployeeIds() {
        List<String> ids = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_DETAILS_CSV))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseCSVLine(line);
                if (f.length > 0 && !f[0].isBlank()) {
                    ids.add(f[0]);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read employee data: " + e.getMessage());
        }
        return ids;
    }

    // Builds an Employee object from the CSV row with the given ID, or returns null if not found
    public static Employee loadEmployee(String employeeId) {
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_DETAILS_CSV))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseCSVLine(line);
                if (f.length >= 19 && f[0].equals(employeeId)) {
                    return new Employee(
                            f[0],                 // Employee #
                            f[1],                 // Last Name
                            f[2],                 // First Name
                            f[11],                // Position
                            f[10],                // Status (Regular / Probationary)
                            parseNumber(f[13]),   // Basic Salary
                            parseNumber(f[18]),   // Hourly Rate
                            parseNumber(f[14]),   // Rice Subsidy
                            parseNumber(f[15]),   // Phone Allowance
                            parseNumber(f[16]));  // Clothing Allowance
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read employee data: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid employee data for " + employeeId + ": " + e.getMessage());
        }
        return null;
    }

    // Returns all attendance records of one employee
    public static List<AttendanceRecord> loadAttendance(String employeeId) {
        List<AttendanceRecord> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_RECORD_CSV))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseCSVLine(line);
                if (f.length >= 6 && f[0].equals(employeeId)) {
                    try {
                        records.add(new AttendanceRecord(f[0], f[3], f[4], f[5]));
                    } catch (RuntimeException e) {
                        // AttendanceRecord rejects invalid rows (e.g., time out before time in)
                        System.err.println("Skipping invalid attendance row: " + line + " (" + e.getMessage() + ")");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read attendance data: " + e.getMessage());
        }
        return records;
    }

    // Returns the attendance records of one employee for a given month and year
    public static List<AttendanceRecord> loadAttendance(String employeeId, Month month, int year) {
        List<AttendanceRecord> result = new ArrayList<>();
        for (AttendanceRecord record : loadAttendance(employeeId)) {
            LocalDate date = LocalDate.parse(record.getDate(), DATE_FORMAT);
            if (date.getMonth() == month && date.getYear() == year) {
                result.add(record);
            }
        }
        return result;
    }

    // Total net hours worked in a month, using AttendanceRecord.calculateHoursWorked()
    // (1-hour break deducted per day, as specified in the MS1 Method Dictionary)
    public static double getTotalHoursWorked(String employeeId, Month month, int year) {
        double total = 0;
        for (AttendanceRecord record : loadAttendance(employeeId, month, year)) {
            total += record.calculateHoursWorked();
        }
        return total;
    }

    // Years that actually have attendance data, for the GUI's year dropdowns
    public static String[] getAttendanceYears() {
        TreeSet<String> years = new TreeSet<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_RECORD_CSV))) {
            br.readLine(); // skip header
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = parseCSVLine(line);
                if (f.length >= 4 && f[3].length() == 10) {
                    years.add(f[3].substring(6));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read attendance data: " + e.getMessage());
        }
        return years.toArray(new String[0]);
    }

    public static void main(String[] args) {
        System.out.println("Databases folder: " + DATABASES_FOLDER);
        System.out.println("Employees loaded: " + loadEmployeeIds().size());

        Employee emp = loadEmployee("10001");
        System.out.println("Employee 10001: " + emp.getName() + ", hourly rate " + emp.getHourlyRate()
                + ", allowances " + emp.getAllowances());

        double hours = getTotalHoursWorked("10001", Month.JUNE, 2024);
        System.out.println("June 2024 hours (break deducted): " + hours);
        System.out.println("June 2024 salary: " + emp.calculateSalary(hours));
        System.out.println("Years with attendance data: " + String.join(", ", getAttendanceYears()));
    }
}