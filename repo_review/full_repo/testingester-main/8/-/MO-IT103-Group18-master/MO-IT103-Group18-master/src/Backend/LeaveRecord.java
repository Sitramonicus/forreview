package Backend;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class LeaveRecord {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final String employeeId;
    private final String leaveType;     // e.g., Sick, Vacation, Emergency (values, not subclasses)
    private final LocalDate startDate;
    private final LocalDate endDate;
    private String approvalStatus;      // Pending, Approved, or Declined
    private final String paidStatus;    // Paid or Unpaid, checked by PayrollProcessor

    public LeaveRecord(String employeeId, String leaveType, String startDate, String endDate,
                       String approvalStatus, String paidStatus) {
        LocalDate start = LocalDate.parse(startDate, DATE_FORMAT);
        LocalDate end = LocalDate.parse(endDate, DATE_FORMAT);

        // Validation rule: enforce a valid date range before duration is calculated
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }

        this.employeeId = employeeId;
        this.leaveType = leaveType;
        this.startDate = start;
        this.endDate = end;
        this.approvalStatus = approvalStatus;
        this.paidStatus = paidStatus;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getLeaveType() {
        return leaveType;
    }

    public String getStartDate() {
        return startDate.format(DATE_FORMAT);
    }

    public String getEndDate() {
        return endDate.format(DATE_FORMAT);
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public String getPaidStatus() {
        return paidStatus;
    }

    // Retrieves the total APPROVED leave duration in days (start and end date included).
    // Read-only; returns 0 while the leave is Pending or Declined.
    public int getDuration() {
        if (!"Approved".equalsIgnoreCase(approvalStatus)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public static void main(String[] args) {
        LeaveRecord leave1 = new LeaveRecord("10001", "Vacation", "10-26-2026", "10-28-2026", "Approved", "Paid");
        LeaveRecord leave2 = new LeaveRecord("10001", "Sick", "10-30-2026", "11-02-2026", "Approved", "Paid");
        LeaveRecord leave3 = new LeaveRecord("10001", "Emergency", "11-05-2026", "11-06-2026", "Pending", "Unpaid");

        System.out.println("Employee ID: " + leave1.getEmployeeId());
        System.out.println("Leave Type: " + leave1.getLeaveType());
        System.out.println("Start Date: " + leave1.getStartDate());
        System.out.println("End Date: " + leave1.getEndDate());
        System.out.println("Approval Status: " + leave1.getApprovalStatus());
        System.out.println("Paid Status: " + leave1.getPaidStatus());
        System.out.println("Duration: " + leave1.getDuration() + " day(s)");
        System.out.println("Cross-month leave (Oct 30 - Nov 2): " + leave2.getDuration() + " day(s)");
        System.out.println("Pending leave duration: " + leave3.getDuration() + " day(s)");
    }
}