package Backend;

import java.time.Duration;
import java.time.LocalTime;

public class AttendanceRecord {
    // 1-hour unpaid lunch break, same as CP2's calculateHoursWorked() in TACP2
    private static final double BREAK_HOURS = 1.0;

    private final String employeeId;
    private final String date;
    private final LocalTime timeIn;
    private LocalTime timeOut;   // can be logged later via recordTimeOut(), but only once

    // Use when the employee has only timed in
    public AttendanceRecord(String employeeId, String date, String timeIn) {
        this.employeeId = employeeId;
        this.date = date;
        this.timeIn = LocalTime.parse(timeIn);
    }

    // Use when both times are known (e.g., loading from the attendance CSV)
    public AttendanceRecord(String employeeId, String date, String timeIn, String timeOut) {
        this(employeeId, date, timeIn);
        recordTimeOut(LocalTime.parse(timeOut));
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getDate() {
        return date;
    }

    public LocalTime getTimeIn() {
        return timeIn;
    }

    public LocalTime getTimeOut() {
        return timeOut;
    }

    // Validation rules: timeOut must be later than timeIn, and recorded hours
    // cannot be altered once logged.
    public void recordTimeOut(LocalTime timeOut) {
        if (this.timeOut != null) {
            throw new IllegalStateException("Time out has already been recorded.");
        }
        if (!timeOut.isAfter(timeIn)) {
            throw new IllegalArgumentException("Time out must be later than time in.");
        }
        this.timeOut = timeOut;
    }

    // Derives total net worked hours; hides the timestamp subtraction and
    // automatic break-time deduction from caller classes.
    public double calculateHoursWorked() {
        if (timeOut == null) {
            return 0;   // no time out logged yet
        }
        double totalHours = Duration.between(timeIn, timeOut).toMinutes() / 60.0;
        return Math.max(totalHours - BREAK_HOURS, 0);
    }

    public static void main(String[] args) {
        AttendanceRecord record1 = new AttendanceRecord("10001", "06-03-2024", "08:59", "18:31");

        System.out.println("Employee ID: " + record1.getEmployeeId());
        System.out.println("Date: " + record1.getDate());
        System.out.println("Time In: " + record1.getTimeIn());
        System.out.println("Time Out: " + record1.getTimeOut());
        System.out.println("Hours Worked: " + record1.calculateHoursWorked());

        try {
            new AttendanceRecord("10001", "06-03-2024", "09:00", "08:00");
        } catch (IllegalArgumentException e) {
            System.out.println("Rejected invalid record: " + e.getMessage());
        }
    }
}