package ph.motorph.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** Process-memory store used only by the unauthenticated local synthetic preview. */
public final class InMemoryWorkflowStore implements WorkflowStore {
    private final Map<String, DemoLeaveRequest> leaveRequests = new LinkedHashMap<>();
    private final Map<String, DemoTicket> tickets = new LinkedHashMap<>();
    private final Map<String, DemoOvertimeRequest> overtimeRequests = new LinkedHashMap<>();

    @Override
    public synchronized List<DemoOvertimeRequest> listOvertimeRequests() {
        List<DemoOvertimeRequest> result = new ArrayList<>(overtimeRequests.values());
        result.sort(Comparator.comparing(DemoOvertimeRequest::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized DemoOvertimeRequest createOvertimeRequest(DemoOvertimeRequest request) {
        if (overtimeRequests.containsKey(request.getId())) throw new IllegalStateException("Request ID already exists.");
        overtimeRequests.put(request.getId(), request);
        return request;
    }

    @Override
    public synchronized DemoOvertimeRequest reviewOvertimeRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        DemoOvertimeRequest existing = overtimeRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Overtime request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending overtime request can be approved or rejected.");
        DemoOvertimeRequest reviewed = existing.reviewed(decisionStatus, reviewerId, reviewedAt);
        overtimeRequests.put(id, reviewed);
        return reviewed;
    }

    @Override
    public synchronized List<DemoLeaveRequest> listLeaveRequests() {
        List<DemoLeaveRequest> result = new ArrayList<>(leaveRequests.values());
        result.sort(Comparator.comparing(DemoLeaveRequest::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized DemoLeaveRequest createLeaveRequest(DemoLeaveRequest request) {
        if (leaveRequests.containsKey(request.getId())) throw new IllegalStateException("Request ID already exists.");
        leaveRequests.put(request.getId(), request);
        return request;
    }

    @Override
    public synchronized DemoLeaveRequest cancelLeaveRequest(String id, String cancelledAt) {
        DemoLeaveRequest existing = leaveRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Leave request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending leave request can be cancelled.");
        DemoLeaveRequest cancelled = existing.cancelled(cancelledAt);
        leaveRequests.put(id, cancelled);
        return cancelled;
    }

    @Override
    public synchronized DemoLeaveRequest reviewLeaveRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        DemoLeaveRequest existing = leaveRequests.get(id);
        if (existing == null) throw new NoSuchElementException("Leave request was not found.");
        if (!"Pending review".equals(existing.getStatus())) throw new IllegalStateException("Only a pending leave request can be approved or rejected.");
        DemoLeaveRequest reviewed = existing.reviewed(decisionStatus, reviewerId, reviewedAt);
        leaveRequests.put(id, reviewed);
        return reviewed;
    }

    @Override
    public synchronized List<DemoTicket> listTickets() {
        List<DemoTicket> result = new ArrayList<>(tickets.values());
        result.sort(Comparator.comparing(DemoTicket::getSubmittedAt).reversed());
        return result;
    }

    @Override
    public synchronized DemoTicket createTicket(DemoTicket ticket) {
        if (tickets.containsKey(ticket.getId())) throw new IllegalStateException("Ticket ID already exists.");
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public synchronized DemoTicket resolveTicket(String id, String reply, String resolvedBy, String resolvedAt) {
        DemoTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus()) && !"In progress".equals(existing.getStatus())) throw new IllegalStateException("Only an open or in-progress ticket can be resolved.");
        DemoTicket resolved = existing.resolved(reply, resolvedBy, resolvedAt);
        tickets.put(id, resolved);
        return resolved;
    }

    @Override
    public synchronized DemoTicket startTicket(String id, String handlerId, String startedAt) {
        DemoTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus())) throw new IllegalStateException("Only an open ticket can be started.");
        DemoTicket started = existing.inProgress(handlerId, startedAt);
        tickets.put(id, started);
        return started;
    }

    @Override
    public synchronized DemoTicket cancelTicket(String id, String cancelledAt) {
        DemoTicket existing = tickets.get(id);
        if (existing == null) throw new NoSuchElementException("Ticket was not found.");
        if (!"Open".equals(existing.getStatus())) throw new IllegalStateException("Only an open ticket can be cancelled.");
        DemoTicket cancelled = existing.cancelled(cancelledAt);
        tickets.put(id, cancelled);
        return cancelled;
    }
}
