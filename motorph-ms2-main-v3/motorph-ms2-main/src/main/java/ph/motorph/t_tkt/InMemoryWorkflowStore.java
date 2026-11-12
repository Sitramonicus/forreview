package ph.motorph.t_tkt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public final class InMemoryWorkflowStore implements WorkflowStore {
    private final Map<String, LeaveRequest> leaveRequests = new LinkedHashMap<>();
    private final Map<String, LeaveTicket> tickets = new LinkedHashMap<>();
    private final Map<String, OvertimeRequest> overtimeRequests = new LinkedHashMap<>();

    @Override
    public synchronized List<OvertimeRequest> listOvertimeRequests() {
        List<OvertimeRequest> result = new ArrayList<>(overtimeRequests.values());
        result.sort(Comparator.comparing(OvertimeRequest::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized OvertimeRequest createOvertimeRequest(OvertimeRequest request) {
        if (overtimeRequests.containsKey(request.getId())) throw new IllegalStateException("Request ID already exists.");
        overtimeRequests.put(request.getId(), request);
        return request;
    }

    @Override
    public synchronized OvertimeRequest reviewOvertimeRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        OvertimeRequest existing = overtimeRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Overtime request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending overtime request can be approved or rejected.");
        OvertimeRequest reviewed = existing.reviewed(decisionStatus, reviewerId, reviewedAt);
        overtimeRequests.put(id, reviewed);
        return reviewed;
    }

    @Override
    public synchronized List<LeaveRequest> listLeaveRequests() {
        List<LeaveRequest> result = new ArrayList<>(leaveRequests.values());
        result.sort(Comparator.comparing(LeaveRequest::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized LeaveRequest createLeaveRequest(LeaveRequest request) {
        if (leaveRequests.containsKey(request.getId())) throw new IllegalStateException("Request ID already exists.");
        leaveRequests.put(request.getId(), request);
        return request;
    }

    @Override
    public synchronized LeaveRequest cancelLeaveRequest(String id, String cancelledAt) {
        LeaveRequest existing = leaveRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Leave request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending leave request can be cancelled.");
        LeaveRequest cancelled = existing.cancelled(cancelledAt);
        leaveRequests.put(id, cancelled);
        return cancelled;
    }

    @Override
    public synchronized LeaveRequest reviewLeaveRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        LeaveRequest existing = leaveRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Leave request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending leave request can be approved or rejected.");
        LeaveRequest reviewed = existing.reviewed(decisionStatus, reviewerId, reviewedAt);
        leaveRequests.put(id, reviewed);
        return reviewed;
    }

    @Override
    public synchronized List<LeaveTicket> listTickets() {
        List<LeaveTicket> result = new ArrayList<>(tickets.values());
        result.sort(Comparator.comparing(LeaveTicket::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized LeaveTicket createTicket(LeaveTicket ticket) {
        if (tickets.containsKey(ticket.getId())) throw new IllegalStateException("Ticket ID already exists.");
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public synchronized LeaveTicket resolveTicket(String id, String reply, String resolvedBy, String resolvedAt) {
        LeaveTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus()) && !"In progress".equals(existing.getStatus())) throw new IllegalStateException("Only an open or in-progress ticket can be resolved.");
        LeaveTicket resolved = existing.resolved(reply, resolvedBy, resolvedAt);
        tickets.put(id, resolved);
        return resolved;
    }

    @Override
    public synchronized LeaveTicket startTicket(String id, String handlerId, String startedAt) {
        LeaveTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus())) throw new IllegalStateException("Only an open ticket can be started.");
        LeaveTicket started = existing.inProgress(handlerId, startedAt);
        tickets.put(id, started);
        return started;
    }

    @Override
    public synchronized LeaveTicket cancelTicket(String id, String cancelledAt) {
        LeaveTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus())) throw new IllegalStateException("Only an open ticket can be cancelled.");
        LeaveTicket cancelled = existing.cancelled(cancelledAt);
        tickets.put(id, cancelled);
        return cancelled;
    }
}
