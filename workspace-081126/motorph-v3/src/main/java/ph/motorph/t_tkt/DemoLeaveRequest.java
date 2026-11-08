package ph.motorph.t_tkt;

/** Synthetic leave request stored only in a user's development sandbox. */
public final class DemoLeaveRequest {
    private final String id;
    private final String employeeId;
    private final String employeeName;
    private final String leaveType;
    private final String startDate;
    private final String endDate;
    private final String note;
    private final String status;
    private final String submittedAt;
    private final String cancelledAt;
    private final String reviewerId;
    private final String reviewedAt;

    public DemoLeaveRequest(String id, String employeeId, String employeeName, String leaveType,
            String startDate, String endDate, String note, String status, String submittedAt, String cancelledAt) {
        this(id, employeeId, employeeName, leaveType, startDate, endDate, note, status, submittedAt, cancelledAt, "", "");
    }

    /** reviewerId/reviewedAt follow the course Leave table's approverID and dateApproved. */
    public DemoLeaveRequest(String id, String employeeId, String employeeName, String leaveType,
            String startDate, String endDate, String note, String status, String submittedAt, String cancelledAt,
            String reviewerId, String reviewedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.note = note;
        this.status = status;
        this.submittedAt = submittedAt;
        this.cancelledAt = cancelledAt == null ? "" : cancelledAt;
        this.reviewerId = reviewerId == null ? "" : reviewerId;
        this.reviewedAt = reviewedAt == null ? "" : reviewedAt;
    }

    public DemoLeaveRequest cancelled(String timestamp) {
        return new DemoLeaveRequest(id, employeeId, employeeName, leaveType, startDate, endDate,
            note, "Cancelled", submittedAt, timestamp);
    }

    public DemoLeaveRequest reviewed(String decisionStatus, String reviewer, String timestamp) {
        return new DemoLeaveRequest(id, employeeId, employeeName, leaveType, startDate, endDate,
            note, decisionStatus, submittedAt, cancelledAt, reviewer, timestamp);
    }

    public String getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getLeaveType() { return leaveType; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public String getSubmittedAt() { return submittedAt; }
    public String getCancelledAt() { return cancelledAt; }
    public String getReviewerId() { return reviewerId; }
    public String getReviewedAt() { return reviewedAt; }
}
