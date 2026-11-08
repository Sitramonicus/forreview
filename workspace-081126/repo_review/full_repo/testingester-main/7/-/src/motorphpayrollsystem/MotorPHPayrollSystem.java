package motorphpayrollsystem;

import java.io.*;

public class MotorPHPayrollSystem {

    public static long timeInMs  = 0;
    public static long timeOutMs = 0;

    public static void main(String[] args) {
        // Ensure attendance.csv exists with correct header
        ensureAttendanceCSV();

        EmployeeData.loadEmployeeData();
        loginUI login = new loginUI();
        login.setVisible(true);
    }

    private static void ensureAttendanceCSV() {
        File file = new File("attendance.csv");
        if (!file.exists() || file.length() == 0) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
                pw.println("\"Employee No\",\"Last Name\",\"First Name\",\"Date\",\"Time In\",\"Time Out\"");
                System.out.println("attendance.csv created with header.");
            } catch (Exception e) {
                System.out.println("Could not create attendance.csv: " + e.getMessage());
            }
        }
    }
}
