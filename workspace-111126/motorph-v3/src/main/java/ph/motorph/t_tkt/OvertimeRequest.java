package ph.motorph.t_tkt;

public final class OvertimeRequest {
    private final String id;
    private final String employeeId;
    private final String employeeName;
    private final String date;
    private final int minutes;
    private final String note;
    private final String status;
    private final String submittedAt;
    private final String reviewerId;
    private final String reviewedAt;

    public OvertimeRequest(String id, String employeeId, String employeeName, String date, int minutes, String note,
            String status, String submittedAt, String reviewerId, String reviewedAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.minutes = minutes;
        this.note = note == null ? "" : note;
        this.status = status;
        this.submittedAt = submittedAt;
        this.reviewerId = reviewerId == null ? "" : reviewerId;
        this.reviewedAt = reviewedAt == null ? "" : reviewedAt;
    }

    public OvertimeRequest reviewed(String decisionStatus, String reviewer, String timestamp) {
        return new OvertimeRequest(id, employeeId, employeeName, date, minutes, note, decisionStatus, submittedAt, reviewer, timestamp);
    }

    public String getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getDate() { return date; }
    public int getMinutes() { return minutes; }
    public String getNote() { return note; }
    public String getStatus() { return status; }
    public String getSubmittedAt() { return submittedAt; }
    public String getReviewerId() { return reviewerId; }
    public String getReviewedAt() { return reviewedAt; }
}
