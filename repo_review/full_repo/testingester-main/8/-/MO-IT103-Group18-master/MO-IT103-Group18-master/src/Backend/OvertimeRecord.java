package Backend;

// "Is-a" relationship (MS1 Week 4 / MS2 Refactoring Plan): an OvertimeRecord IS a
// specialized AttendanceRecord. It stores the same daily time-in/time-out data and reuses
// the inherited calculateHoursWorked(), but adds approval and a different pay rule,
// because overtime hours are paid at a premium rate instead of the regular hourly rate.
public abstract class OvertimeRecord extends AttendanceRecord {
    // Hours beyond a regular 8-hour workday (after the 1-hour break) count as overtime
    private static final double REGULAR_HOURS = 8.0;

    private final double hourlyRate;
    private String approvalStatus = "Pending";

    public OvertimeRecord(String employeeId, String date, String timeIn, String timeOut, double hourlyRate) {
        super(employeeId, date, timeIn, timeOut);
        if (hourlyRate <= 0) {
            throw new IllegalArgumentException("Hourly rate must be greater than 0.");
        }
        this.hourlyRate = hourlyRate;
    }

    // Overtime hours derived from the inherited hours-worked calculation
    public double getOvertimeHours() {
        return Math.max(calculateHoursWorked() - REGULAR_HOURS, 0);
    }

    public double getHourlyRate() {
        return hourlyRate;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public boolean isApproved() {
        return "Approved".equals(approvalStatus);
    }

    // Validation rule: only allows Pending -> Approved/Denied; cannot move backward once set
    public void setApprovalStatus(String status) {
        if (!"Pending".equals(approvalStatus)) {
            throw new IllegalStateException("Approval status is already final: " + approvalStatus);
        }
        if (!"Approved".equals(status) && !"Denied".equals(status)) {
            throw new IllegalArgumentException("Status must be Approved or Denied.");
        }
        this.approvalStatus = status;
    }

    // Abstract contract: each overtime type applies its own rate multiplier.
    // Only approved (authorized) overtime is paid.
    public abstract double computeOvertimePay();
}