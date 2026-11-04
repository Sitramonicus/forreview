package Backend;

import java.time.LocalDateTime;

public class PayslipDispute {

    // Encapsulation: private attributes
    public enum Status {
        OPEN,
        UNDER_REVIEW,
        RESOLVED,
        REJECTED
    }

    private final String disputeId;
    private final String employeeId;
    private final String payslipPeriod;
    private final String reason;
    private final String description;

    private final LocalDateTime dateSubmitted;

    private Status status;
    private String reviewedBy;
    private String resolutionNotes;
    private LocalDateTime dateResolved;

    // Constructor
    public PayslipDispute(
            String disputeId,
            String employeeId,
            String payslipPeriod,
            String reason,
            String description) {

        this.disputeId = requireText(disputeId, "Dispute ID");
        this.employeeId = requireText(employeeId, "Employee ID");
        this.payslipPeriod = requireText(payslipPeriod, "Payslip Period");
        this.reason = requireText(reason, "Reason");
        this.description = requireText(description, "Description");

        this.status = Status.OPEN;
        this.dateSubmitted = LocalDateTime.now();
    }

    // Getter methods
    public String getDisputeId() {
        return disputeId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getPayslipPeriod() {
        return payslipPeriod;
    }

    public String getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getDateSubmitted() {
        return dateSubmitted;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public LocalDateTime getDateResolved() {
        return dateResolved;
    }

    // Payroll reviews the dispute
    public void markUnderReview(String payrollStaffId) {

        if (status != Status.OPEN) {
            throw new IllegalStateException(
                "Only OPEN disputes can be reviewed."
            );
        }

        reviewedBy = requireText(
            payrollStaffId, "Payroll Staff ID"
        );

        status = Status.UNDER_REVIEW;
    }

    // Resolve dispute
    public void resolve(String payrollStaffId, String notes) {
        closeDispute(Status.RESOLVED, payrollStaffId, notes);
    }

    // Reject dispute
    public void reject(String payrollStaffId, String notes) {
        closeDispute(Status.REJECTED, payrollStaffId, notes);
    }

    // Common method for closing disputes
    private void closeDispute(
            Status finalStatus,
            String payrollStaffId,
            String notes) {

        if (status != Status.UNDER_REVIEW) {
            throw new IllegalStateException(
                "Dispute must be UNDER_REVIEW before closing."
            );
        }

        reviewedBy = requireText(
            payrollStaffId, "Payroll Staff ID"
        );

        resolutionNotes = requireText(
            notes, "Resolution Notes"
        );

        status = finalStatus;
        dateResolved = LocalDateTime.now();
    }

    // Input validation
    private static String requireText(
            String value, String fieldName) {

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(
                fieldName + " cannot be empty."
            );
        }

        return value.trim();
    }

    // Display dispute information
    @Override
    public String toString() {

        return "PayslipDispute{" +
                "disputeId='" + disputeId + '\'' +
                ", employeeId='" + employeeId + '\'' +
                ", payslipPeriod='" + payslipPeriod + '\'' +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", dateSubmitted=" + dateSubmitted +
                '}';
    }
}


