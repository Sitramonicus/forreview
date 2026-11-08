
package motorphpayrollsystem;

import java.time.Duration;
import java.time.LocalDateTime;

public class AttendanceRecord {

    private int employeeID;
    private LocalDateTime clockIn;
    private LocalDateTime clockOut;

    public AttendanceRecord(
            int employeeID,
            LocalDateTime clockIn,
            LocalDateTime clockOut) {

        setEmployeeID(employeeID);
        setClockIn(clockIn);
        setClockOut(clockOut);
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(int employeeID) {
        if (employeeID <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be greater than 0."
            );
        }

        this.employeeID = employeeID;
    }

    public LocalDateTime getClockIn() {
        return clockIn;
    }

    public void setClockIn(LocalDateTime clockIn) {
        if (clockIn == null) {
            throw new IllegalArgumentException(
                    "Clock-in time cannot be null."
            );
        }

        if (clockOut != null && !clockIn.isBefore(clockOut)) {
            throw new IllegalArgumentException(
                    "Clock-in must be before clock-out."
            );
        }

        this.clockIn = clockIn;
    }

    public LocalDateTime getClockOut() {
        return clockOut;
    }

    public void setClockOut(LocalDateTime clockOut) {
        if (clockOut == null) {
            throw new IllegalArgumentException(
                    "Clock-out time cannot be null."
            );
        }

        if (clockIn != null && !clockOut.isAfter(clockIn)) {
            throw new IllegalArgumentException(
                    "Clock-out must be after clock-in."
            );
        }

        this.clockOut = clockOut;
    }

    public double calculateHoursWorked() {
        long minutes = Duration.between(
                clockIn, clockOut
        ).toMinutes();

        return minutes / 60.0;
    }

    public double calculateOvertimeHours() {
        double hoursWorked = calculateHoursWorked();

        // Milestone 1 assumption: 8 regular hours per day.
        // Verify against the original CP2 payroll rules.
        return Math.max(0, hoursWorked - 8.0);
    }
}