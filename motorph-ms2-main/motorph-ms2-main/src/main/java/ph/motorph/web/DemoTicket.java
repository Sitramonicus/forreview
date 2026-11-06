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
    private final String reply;
    private final String resolvedBy;
    private final String resolvedAt;

    public DemoTicket(String id, String employeeId, String employeeName, String ticketType, String queue,
            String summary, String status, String submittedAt, String cancelledAt) {
        this(id, employeeId, employeeName, ticketType, queue, summary, status, submittedAt, cancelledAt, "", "", "");
    }

    public DemoTicket(String id, String employeeId, String employeeName, String ticketType, String queue,
            String summary, String status, String submittedAt, String cancelledAt, String reply, String resolvedBy, String resolvedAt) {
        this.reply = reply == null ? "" : reply;
        this.resolvedBy = resolvedBy == null ? "" : resolvedBy;
        this.resolvedAt = resolvedAt == null ? "" : resolvedAt;
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

    /** In progress: resolvedBy holds who is handling it until it is resolved; resolvedAt stays empty. */
    public DemoTicket inProgress(String handlerId, String timestamp) {
        return new DemoTicket(id, employeeId, employeeName, ticketType, queue, summary, "In progress", submittedAt, cancelledAt, "", handlerId, "");
    }

    public DemoTicket resolved(String replyText, String by, String timestamp) {
        return new DemoTicket(id, employeeId, employeeName, ticketType, queue, summary, "Resolved", submittedAt, cancelledAt, replyText, by, timestamp);
    }

    public String getReply() { return reply; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolvedAt() { return resolvedAt; }
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
