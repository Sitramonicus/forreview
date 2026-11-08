
package motorphpayrollsystem;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LeaveRequest {

    private int employeeID;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String approvalStatus;
    private String approver;

    public LeaveRequest(
            int employeeID,
            String leaveType,
            LocalDate startDate,
            LocalDate endDate) {

        setEmployeeID(employeeID);
        setLeaveType(leaveType);
        setStartDate(startDate);
        setEndDate(endDate);

        this.description = "";
        this.approvalStatus = "Pending";
        this.approver = "";
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

    public String getLeaveType() {
        return leaveType;
    }

    public void setLeaveType(String leaveType) {
        if (leaveType == null || leaveType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Leave type cannot be empty."
            );
        }

        this.leaveType = leaveType.trim();
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) {
            throw new IllegalArgumentException(
                    "Start date cannot be null."
            );
        }

        if (endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date."
            );
        }

        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        if (endDate == null) {
            throw new IllegalArgumentException(
                    "End date cannot be null."
            );
        }

        if (startDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date."
            );
        }

        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Description cannot be empty."
            );
        }

        this.description = description.trim();
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        if (approvalStatus == null ||
                (!approvalStatus.equalsIgnoreCase("Pending") &&
                        !approvalStatus.equalsIgnoreCase("Approved") &&
                        !approvalStatus.equalsIgnoreCase("Rejected"))) {

            throw new IllegalArgumentException(
                    "Approval status must be Pending, Approved, or Rejected."
            );
        }

        if (approvalStatus.equalsIgnoreCase("Approved")) {
            this.approvalStatus = "Approved";
        } else if (approvalStatus.equalsIgnoreCase("Rejected")) {
            this.approvalStatus = "Rejected";
        } else {
            this.approvalStatus = "Pending";
        }
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        if (approver == null || approver.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Approver cannot be empty."
            );
        }

        this.approver = approver.trim();
    }

    public boolean isApproved() {
        return "Approved".equals(approvalStatus);
    }

    public int getLeaveDays() {
        long days = ChronoUnit.DAYS.between(
                startDate, endDate
        ) + 1;

        if (days > Integer.MAX_VALUE) {
            throw new IllegalStateException(
                    "Leave period is too long."
            );
        }

        return (int) days;
    }
}