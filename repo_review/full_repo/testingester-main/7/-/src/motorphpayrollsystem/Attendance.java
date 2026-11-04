package motorphpayrollsystem;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class Attendance {

    public static final String CSV_FILE = "attendance.csv";
    private static final DateTimeFormatter FMT_DATE = 
        DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final DateTimeFormatter FMT_TIME = 
        DateTimeFormatter.ofPattern("H:mm");

    public static synchronized void ensureCSVExists() {
        try {
            CSVUtil.ensureHeader(CSV_FILE,
                "\"Emp No\",\"Last Name\",\"First Name\","
                + "\"Date\",\"Time In\",\"Time Out\"");
        } catch (Exception e) {
            System.out.println("Could not create attendance.csv: " + e.getMessage());
        }
    }

    public static synchronized boolean recordTimeIn(String[] emp) {
        ensureCSVExists();
        String today = LocalDate.now().format(FMT_DATE);

        // Check for duplicate open time-in today
        if (hasOpenRecord(emp[0], today)) {
            JOptionPane.showMessageDialog(null,
                "You already have an active Time In today.\n"
              + "Please Time Out first before clocking in again.",
                "Duplicate Time In", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            String timeIn = LocalTime.now().format(FMT_TIME);
            CSVUtil.append(CSV_FILE, new String[]{
                emp[0], emp[1], emp[2], today, timeIn, ""
            });
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error recording Time In: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
    }

    /** Returns true if the employee has a time-in record today with no time-out. */
    private static boolean hasOpenRecord(String empNo, String today) {
        try {
            List<String[]> rows = CSVUtil.read(CSV_FILE);
            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);
                if (row.length >= 6 &&
                    row[0].trim().equals(empNo) &&
                    row[3].trim().equals(today) &&
                    !row[4].trim().isEmpty() &&
                    row[5].trim().isEmpty()) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    // =========================================================================
    // TIME OUT
    // Returns false if no open record found
    // =========================================================================
    public static synchronized boolean recordTimeOut(String empNo) {
        ensureCSVExists();
        String today = LocalDate.now().format(FMT_DATE);
        List<String[]> records;
        boolean updated = false;

        try {
            records = CSVUtil.read(CSV_FILE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error reading attendance file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        for (String[] row : records) {
            if (!updated &&
                row.length >= 6 &&
                row[0].trim().equals(empNo) &&
                row[3].trim().equals(today) &&
                !row[4].trim().isEmpty() &&
                row[5].trim().isEmpty()) {
                row[5] = LocalTime.now().format(FMT_TIME);
                updated = true;
            }
        }

        if (!updated) {
            JOptionPane.showMessageDialog(null,
                "No active Time In found for today.\n"
              + "Please Time In first.",
                "No Record Found", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            CSVUtil.write(CSV_FILE, records);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error saving Time Out: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    // =========================================================================
    // FILTER / DISPLAY — used by AttendanceUI JTable
    // Single-pass, efficient, guards against all malformed rows
    // =========================================================================
    public static void filterAttendance(JTable table, String empNo,
                                         String selectedDate) {
        if (table == null) return;
        DefaultTableModel model = (DefaultTableModel) table.getModel();

        SwingUtilities.invokeLater(() -> {
            model.setRowCount(0);
            ensureCSVExists();

            try {
                List<String[]> rows = CSVUtil.read(CSV_FILE);
                for (int ri = 1; ri < rows.size(); ri++) {
                    String[] row = rows.get(ri);
                    if (row.length < 6) continue;

                    boolean matchEmp  = row[0].trim().equals(empNo);
                    boolean matchDate = (selectedDate == null || selectedDate.isEmpty())
                                     || row[3].contains(selectedDate);

                    if (!matchEmp || !matchDate) continue;

                    String timeIn  = row[4].trim().isEmpty() ? "—" : row[4].trim();
                    String timeOut = row[5].trim().isEmpty() ? "—" : row[5].trim();
                    String total   = computeTotalHours(row[4].trim(), row[5].trim());

                    model.addRow(new Object[]{row[3], timeIn, timeOut, total});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // =========================================================================
    // TOTAL HOURS — handles missing/malformed times gracefully
    // =========================================================================
    public static String computeTotalHours(String timeIn, String timeOut) {
        if (timeIn == null || timeIn.isEmpty() ||
            timeOut == null || timeOut.isEmpty()) return "—";
        try {
            LocalTime in  = LocalTime.parse(timeIn.trim(),  FMT_TIME);
            LocalTime out = LocalTime.parse(timeOut.trim(), FMT_TIME);
            long mins = ChronoUnit.MINUTES.between(in, out);
            // Handle overnight shift
            if (mins < 0) mins += 24 * 60;
            return String.format("%d hr %02d min", mins / 60, mins % 60);
        } catch (Exception e) {
            return "—";
        }
    }

    // =========================================================================
    // STATS — used by Dashboard / PayrollModule
    // =========================================================================
    public static double[] getAttendance(String empNo) {
        int daysPresent = 0;
        double totalOvertime = 0.0;
        ensureCSVExists();

        try {
            List<String[]> rows = CSVUtil.read(CSV_FILE);
            for (int i = 1; i < rows.size(); i++) {
                String[] line = rows.get(i);
                if (line.length < 6) continue;
                if (!line[0].trim().equals(empNo)) continue;
                String login = line[4].trim(), logout = line[5].trim();
                if (login.isEmpty() || logout.isEmpty()) continue;
                daysPresent++;
                try {
                    LocalTime in  = LocalTime.parse(login,  FMT_TIME);
                    LocalTime out = LocalTime.parse(logout, FMT_TIME);
                    long mins = ChronoUnit.MINUTES.between(in, out);
                    if (mins < 0) mins += 24 * 60;
                    double hrs = mins / 60.0;
                    if (hrs > 9) totalOvertime += hrs - 9;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new double[]{daysPresent, totalOvertime};
    }

    /** Total hours worked in a month (for payroll computation). */
    public static double getTotalHoursWorked(String empNo) {
        double[] stats = getAttendance(empNo);
        return stats[0] * Payroll.WORKING_HOURS_PER_DAY;
    }

    // =========================================================================
    // DELETE — remove one erroneous/duplicate attendance row
    // =========================================================================
    /** Delete one attendance row matching empNo+date+timeIn+timeOut. */
    public static synchronized boolean deleteEntry(String empNo, String date,
                                                   String timeIn, String timeOut) {
        ensureCSVExists();
        String tIn  = "—".equals(timeIn)  ? "" : timeIn.trim();
        String tOut = "—".equals(timeOut) ? "" : timeOut.trim();
        try {
            List<String[]> rows = CSVUtil.read(CSV_FILE);
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 6) continue;
                if (r[0].trim().equals(empNo) &&
                    r[3].trim().equals(date.trim()) &&
                    r[4].trim().equals(tIn) &&
                    r[5].trim().equals(tOut)) {
                    rows.remove(i);
                    CSVUtil.write(CSV_FILE, rows);
                    return true;
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                "Error deleting entry: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    // =========================================================================
    // PER-MONTH HOURS — used by per-period payroll
    // =========================================================================
    /** Distinct months with attendance for this employee, oldest -> newest. */
    public static java.util.List<java.time.YearMonth> getWorkedMonths(String empNo) {
        java.util.TreeSet<java.time.YearMonth> months = new java.util.TreeSet<>();
        ensureCSVExists();
        try {
            List<String[]> rows = CSVUtil.read(CSV_FILE);
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 6 || !r[0].trim().equals(empNo)) continue;
                try {
                    LocalDate d = LocalDate.parse(r[3].trim(), FMT_DATE);
                    months.add(java.time.YearMonth.from(d));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return new java.util.ArrayList<>(months);
    }

    /** Total hours worked by this employee within a given month. */
    public static double getHoursWorked(String empNo, java.time.YearMonth ym) {
        double total = 0.0;
        ensureCSVExists();
        try {
            List<String[]> rows = CSVUtil.read(CSV_FILE);
            for (int i = 1; i < rows.size(); i++) {
                String[] r = rows.get(i);
                if (r.length < 6 || !r[0].trim().equals(empNo)) continue;
                String in = r[4].trim(), out = r[5].trim();
                if (in.isEmpty() || out.isEmpty()) continue;
                try {
                    LocalDate d = LocalDate.parse(r[3].trim(), FMT_DATE);
                    if (!java.time.YearMonth.from(d).equals(ym)) continue;
                    LocalTime ti = LocalTime.parse(in, FMT_TIME);
                    LocalTime to = LocalTime.parse(out, FMT_TIME);
                    long mins = ChronoUnit.MINUTES.between(ti, to);
                    if (mins < 0) mins += 24 * 60;
                    total += mins / 60.0;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return total;
    }
}