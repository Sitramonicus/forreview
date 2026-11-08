package ph.motorph.web;

/** Synthetic help-desk ticket stored only in a user's development sandbox. */
public final class DemoTicket {
    private final String id;
    private final String employeeId;
    private final String employeeName;
    private final String ticketType;
    private final String queue;
    private final String summary;
    private final String status;
    private final String submittedAt;
    private final String cancelledAt;

    public DemoTicket(String id, String employeeId, String employeeName, String ticketType, String queue,
            String summary, String status, String submittedAt, String cancelledAt) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.ticketType = ticketType;
        this.queue = queue;
        this.summary = summary;
        this.status = status;
        this.submittedAt = submittedAt;
        this.cancelledAt = cancelledAt == null ? "" : cancelledAt;
    }

    public DemoTicket cancelled(String timestamp) {
        return new DemoTicket(id, employeeId, employeeName, ticketType, queue, summary,
            "Cancelled", submittedAt, timestamp);
    }

    public String getId() { return id; }
    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getTicketType() { return ticketType; }
    public String getQueue() { return queue; }
    public String getSummary() { return summary; }
    public String getStatus() { return status; }
    public String getSubmittedAt() { return submittedAt; }
    public String getCancelledAt() { return cancelledAt; }
}
