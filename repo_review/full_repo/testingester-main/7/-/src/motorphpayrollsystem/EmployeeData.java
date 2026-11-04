package motorphpayrollsystem;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class EmployeeData {

    // ── Data store ────────────────────────────────────────────────────────────
    public static volatile List<String[]> records;
    public static final String CSV_FILE = "MotorPH_Employee Data.csv";

    // Minimum columns expected in each record row
    private static final int MIN_COLS = 21;

    // ── Change listeners (for dynamic JTable refresh) ─────────────────────────
    private static final List<Runnable> changeListeners = new ArrayList<>();

    public static void addChangeListener(Runnable listener) {
        changeListeners.add(listener);
    }

    private static void notifyChanged() {
        for (Runnable r : changeListeners) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    // =========================================================================
    // LOAD
    // =========================================================================
    public static synchronized void loadEmployeeData() {
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE))) {
            List<String[]> raw = reader.readAll();
            // Pad short rows to MIN_COLS so we never get ArrayIndexOutOfBounds
            for (String[] row : raw) {
                if (row.length < MIN_COLS) {
                    String[] padded = new String[MIN_COLS];
                    System.arraycopy(row, 0, padded, 0, row.length);
                    for (int i = row.length; i < MIN_COLS; i++) padded[i] = "";
                    raw.set(raw.indexOf(row), padded);
                }
            }
            records = raw;
            System.out.println("Employee Data Loaded! Total: " + (records.size() - 1));
        } catch (FileNotFoundException e) {
            System.out.println("CSV file not found: " + CSV_FILE);
            records = new ArrayList<>();
        } catch (Exception e) {
            System.out.println("Error loading employee data: " + e.getMessage());
            e.printStackTrace();
            records = new ArrayList<>();
        }
    }

    // =========================================================================
    // FIELD ACCESSORS — safe, null-checked
    // =========================================================================
    public static String getEmpNo      (String[] e) { return safe(e, 0);  }
    public static String getLastName   (String[] e) { return safe(e, 1);  }
    public static String getFirstName  (String[] e) { return safe(e, 2);  }
    public static String getBirthday   (String[] e) { return safe(e, 3);  }
    public static String getAddress    (String[] e) { return safe(e, 4);  }
    public static String getPhoneNo    (String[] e) { return safe(e, 5);  }
    public static String getStatus     (String[] e) { return safe(e, 10); }
    public static String getPosition   (String[] e) { return safe(e, 11); }
    public static String getSupervisor (String[] e) { return safe(e, 12); }
    public static String getDepartment (String[] e) { return safe(e, 20); }

    private static String safe(String[] e, int idx) {
        if (e == null || idx >= e.length) return "";
        return e[idx] == null ? "" : e[idx].trim();
    }

    // =========================================================================
    // FIND
    // =========================================================================
    public static String[] findEmployee(String empNo) {
        if (records == null || empNo == null) return null;
        for (int i = 1; i < records.size(); i++) {
            String[] row = records.get(i);
            if (row != null && row.length > 0 &&
                row[0].trim().equalsIgnoreCase(empNo.trim())) {
                return row;
            }
        }
        // Only show dialog if called from a UI context
        JOptionPane.showMessageDialog(null,
            "Employee #" + empNo + " not found.",
            "Not Found", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    public static boolean employeeExisting(String empNo) {
        if (records == null || empNo == null) return false;
        for (String[] row : records) {
            if (row != null && row.length > 0 &&
                row[0].trim().equalsIgnoreCase(empNo.trim())) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // VALIDATION — used by UI before calling add/update
    // =========================================================================
    public static String validateNewEmployee(
            String empNo, String firstName, String lastName,
            String birthday, String address, String phone,
            String position, String department, String password,
            String basicSalStr, String hourRateStr,
            String sss, String philHealth, String tin, String pagibig) {

        if (isEmpty(empNo))        return "Employee No. is required.";
        if (!empNo.matches("\\d+")) return "Employee No. must be numeric.";
        if (employeeExisting(empNo)) return "Employee No. " + empNo + " already exists.";
        if (isEmpty(firstName))    return "First Name is required.";
        if (isEmpty(lastName))     return "Last Name is required.";
        if (isEmpty(birthday))     return "Birthday is required.";
        if (isEmpty(address))      return "Address is required.";
        if (isEmpty(phone))        return "Phone No. is required.";
        if (isEmpty(position))     return "Position is required.";
        if (isEmpty(department))   return "Department is required.";
        if (isEmpty(password))     return "Password is required.";
        if (password.length() < 4) return "Password must be at least 4 characters.";

        // Format validation (phone, birthday, government IDs)
        String fmtError = validateFieldFormats(birthday, phone, sss, philHealth, tin, pagibig);
        if (fmtError != null) return fmtError;

        // Numeric validation
        if (!isEmpty(basicSalStr)) {
            try {
                double v = Double.parseDouble(basicSalStr.replace(",", ""));
                if (v < 0) return "Basic Salary cannot be negative.";
            } catch (NumberFormatException e) {
                return "Basic Salary must be a valid number.";
            }
        }
        if (!isEmpty(hourRateStr)) {
            try {
                double v = Double.parseDouble(hourRateStr.replace(",", ""));
                if (v < 0) return "Hourly Rate cannot be negative.";
            } catch (NumberFormatException e) {
                return "Hourly Rate must be a valid number.";
            }
        }
        return null; // null = valid
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Validates format of optional government ID and contact fields.
     * Pass empty string for fields not being validated.
     * Returns an error message, or null if all valid.
     */
    public static String validateFieldFormats(
            String birthday, String phone,
            String sss, String philHealth, String tin, String pagibig) {

        if (!isEmpty(birthday)) {
            // Must be MM/dd/yyyy
            if (!birthday.trim().matches("\\d{2}/\\d{2}/\\d{4}"))
                return "Birthday must be in MM/dd/yyyy format (e.g. 06/15/1990).";
        }
        if (!isEmpty(phone)) {
            // Strip non-digits and require exactly 11
            String digits = phone.replaceAll("[^\\d]", "");
            if (digits.length() != 11)
                return "Phone No. must be 11 digits (e.g. 09171234567).";
        }
        if (!isEmpty(sss)) {
            // Accept XX-XXXXXXX-X or 10 plain digits
            String cleaned = sss.replaceAll("[^\\d]", "");
            if (cleaned.length() != 10)
                return "SSS No. must be 10 digits (e.g. 12-3456789-0).";
        }
        if (!isEmpty(philHealth)) {
            String cleaned = philHealth.replaceAll("[^\\d]", "");
            if (cleaned.length() != 12)
                return "PhilHealth No. must be 12 digits (e.g. 123456789012).";
        }
        if (!isEmpty(tin)) {
            // Accept 9 or 12 digits (e.g. 123-456-789 or 123-456-789-012)
            String cleaned = tin.replaceAll("[^\\d]", "");
            if (cleaned.length() != 9 && cleaned.length() != 12)
                return "TIN must be 9 or 12 digits (e.g. 123-456-789).";
        }
        if (!isEmpty(pagibig)) {
            String cleaned = pagibig.replaceAll("[^\\d]", "");
            if (cleaned.length() != 12)
                return "Pag-IBIG No. must be 12 digits (e.g. 123456789012).";
        }
        return null;
    }

    /** Validates required fields and formats for an update operation. */
    public static String validateUpdateEmployee(
            String firstName, String lastName, String birthday,
            String phone, String position,
            String sss, String philHealth, String tin, String pagibig) {

        if (isEmpty(firstName)) return "First Name is required.";
        if (isEmpty(lastName))  return "Last Name is required.";
        if (isEmpty(position))  return "Position is required.";
        return validateFieldFormats(birthday, phone, sss, philHealth, tin, pagibig);
    }

    // =========================================================================
    // ARITHMETIC: Semi-monthly and Hourly Rate relationship
    // basicSalary / 2 = semiMonthlyRate
    // semiMonthlyRate / (8 hrs * 22 working days / 2) = hourlyRate
    // =========================================================================
    public static double computeSemiMonthlyFromBasic(double basicSalary) {
        return basicSalary / 2.0;
    }

    public static double computeHourlyFromBasic(double basicSalary) {
        // Standard: basic / (22 days * 8 hours) = hourly rate
        return basicSalary / (22.0 * 8.0);
    }

    public static double computeBasicFromHourly(double hourlyRate) {
        return hourlyRate * 22.0 * 8.0;
    }

    public static double semiMonthlyToBasic(double semiMonthly) {
        return semiMonthly * 2.0;
    }

    // =========================================================================
    // ADD
    // =========================================================================
    public static synchronized void addEmployee(
            String empNo, String firstName, String lastName, String birthday,
            String address, String phone, String position, String status,
            String supervisor, String sssNo, String philHealthNo, String tin,
            String pagibigNo, double basicSal, double riceSubs, double phoneAllow,
            double clothingAllow, double grossSemiRate, double hourRate,
            String password, String department) {

        // Auto-compute semiMonthly and hourly if not provided
        if (grossSemiRate <= 0) grossSemiRate = computeSemiMonthlyFromBasic(basicSal);
        if (hourRate <= 0)      hourRate      = computeHourlyFromBasic(basicSal);

        String[] newEmp = new String[MIN_COLS];
        newEmp[0]  = empNo;
        newEmp[1]  = lastName;
        newEmp[2]  = firstName;
        newEmp[3]  = birthday;
        newEmp[4]  = address;
        newEmp[5]  = phone;
        newEmp[6]  = sssNo;
        newEmp[7]  = philHealthNo;
        newEmp[8]  = tin;
        newEmp[9]  = pagibigNo;
        newEmp[10] = status;
        newEmp[11] = position;
        newEmp[12] = supervisor;
        newEmp[13] = String.valueOf(basicSal);
        newEmp[14] = String.valueOf(riceSubs);
        newEmp[15] = String.valueOf(phoneAllow);
        newEmp[16] = String.valueOf(clothingAllow);
        newEmp[17] = String.valueOf(grossSemiRate);
        newEmp[18] = String.valueOf(hourRate);
        newEmp[19] = password;
        newEmp[20] = department;

        records.add(newEmp);
        saveToCSV("addEmployee");
        notifyChanged();
    }

    // =========================================================================
    // DELETE
    // =========================================================================
    public static synchronized void deleteEmployee(String empNo) {
        if (records == null) return;
        boolean found = false;
        for (int i = 1; i < records.size(); i++) {
            if (records.get(i)[0].equalsIgnoreCase(empNo)) {
                records.remove(i);
                found = true;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(null,
                "Employee not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveToCSV("deleteEmployee");
        JOptionPane.showMessageDialog(null,
            "Employee deleted successfully.", "Deleted",
            JOptionPane.INFORMATION_MESSAGE);
        notifyChanged();
    }

    // =========================================================================
    // UPDATE (admin full update)
    // =========================================================================
    public static synchronized void updateProfile(
            String empNo, String firstName, String lastName, String birthday,
            String address, String phone, String position, String status,
            String supervisor, String sssNo, String philHealthNo, String tin,
            String pagibigNo, double basicSal, double riceSubs, double phoneAllow,
            double clothingAllow, double grossSemiRate, double hourRate,
            String password, String department) {

        // Auto-compute semiMonthly and hourly if not provided
        if (grossSemiRate <= 0) grossSemiRate = computeSemiMonthlyFromBasic(basicSal);
        if (hourRate <= 0)      hourRate      = computeHourlyFromBasic(basicSal);

        boolean found = false;
        for (String[] row : records) {
            if (row != null && row[0].equalsIgnoreCase(empNo)) {
                found    = true;
                row[1]   = lastName;
                row[2]   = firstName;
                row[3]   = birthday;
                row[4]   = address;
                row[5]   = phone;
                row[6]   = sssNo;
                row[7]   = philHealthNo;
                row[8]   = tin;
                row[9]   = pagibigNo;
                row[10]  = status;
                row[11]  = position;
                row[12]  = supervisor;
                row[13]  = String.valueOf(basicSal);
                row[14]  = String.valueOf(riceSubs);
                row[15]  = String.valueOf(phoneAllow);
                row[16]  = String.valueOf(clothingAllow);
                row[17]  = String.valueOf(grossSemiRate);
                row[18]  = String.valueOf(hourRate);
                row[19]  = password;
                row[20]  = department;
                break;
            }
        }
        if (!found) {
            JOptionPane.showMessageDialog(null,
                "Employee not found.", "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveToCSV("updateProfile");
        JOptionPane.showMessageDialog(null,
            "Employee updated successfully.", "Updated",
            JOptionPane.INFORMATION_MESSAGE);
        notifyChanged();
    }

    // =========================================================================
    // UPDATE OWN PROFILE (employee self-service)
    // =========================================================================
    public static synchronized void updateOwnProfile(
            String[] emp, String newAddress, String newPhone) {
        if (emp == null) return;
        for (String[] row : records) {
            if (row != null && row[0].equalsIgnoreCase(emp[0])) {
                row[4] = newAddress;
                row[5] = newPhone;
                // Also update the live emp array so UI reflects change immediately
                emp[4] = newAddress;
                emp[5] = newPhone;
                break;
            }
        }
        saveToCSV("updateOwnProfile");
        JOptionPane.showMessageDialog(null,
            "Profile updated successfully.", "Updated",
            JOptionPane.INFORMATION_MESSAGE);
        notifyChanged();
    }

    // =========================================================================
    // CSV WRITE — single synchronized method, used by all operations
    // =========================================================================
    private static synchronized void saveToCSV(String caller) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE))) {
            writer.writeAll(records);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error saving data (" + caller + "): " + e.getMessage(),
                "Save Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // =========================================================================
    // ROLE CHECKS
    // =========================================================================
    public static boolean isHR(String[] emp) {
        return emp != null && emp.length > 20 &&
               emp[20].trim().equalsIgnoreCase("HR");
    }

    public static boolean isPayroll(String[] emp) {
        return emp != null && emp.length > 20 &&
               emp[20].trim().equalsIgnoreCase("Payroll");
    }

    // =========================================================================
    // STATS — for Dashboard
    // =========================================================================
    public static int getTotalEmployees() {
        return records == null ? 0 : Math.max(0, records.size() - 1);
    }

    public static int getActiveEmployees() {
        if (records == null) return 0;
        int count = 0;
        for (int i = 1; i < records.size(); i++) {
            String[] r = records.get(i);
            if (r != null && r.length > 10 &&
                "Active".equalsIgnoreCase(r[10].trim())) count++;
        }
        return count;
    }
}
