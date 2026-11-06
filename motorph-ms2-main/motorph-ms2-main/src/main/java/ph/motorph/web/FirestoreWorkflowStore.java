package ph.motorph.web;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

/** Firestore user-subcollection adapter. Firestore Rules enforce owner-only access. */
public final class FirestoreWorkflowStore implements WorkflowStore {
    private static final String LEAVE_COLLECTION = "demoLeaveRequests";
    private static final String TICKET_COLLECTION = "demoTickets";
    private final FirestoreRestClient client;

    public FirestoreWorkflowStore(FirestoreRestClient client) {
        if (client == null) throw new IllegalArgumentException("client must not be null");
        this.client = client;
    }

    @Override
    public List<DemoLeaveRequest> listLeaveRequests() {
        List<DemoLeaveRequest> requests = new ArrayList<>();
        for (JsonObject document : client.listDocuments(client.userCollection(LEAVE_COLLECTION))) {
            requests.add(leaveFromDocument(document));
        }
        requests.sort(Comparator.comparing(DemoLeaveRequest::getSubmittedAt).reversed());
        return requests;
    }

    @Override
    public DemoLeaveRequest createLeaveRequest(DemoLeaveRequest request) {
        if (!client.createDocument(client.userCollection(LEAVE_COLLECTION), request.getId(), leaveFields(request))) {
            throw new IllegalStateException("Request ID already exists; try submitting again.");
        }
        return request;
    }

    @Override
    public DemoLeaveRequest cancelLeaveRequest(String id, String cancelledAt) {
        DemoLeaveRequest current = listLeaveRequests().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Leave request was not found in this user sandbox."));
        if (!"Pending review".equals(current.getStatus())) throw new IllegalStateException("Only a pending leave request can be cancelled.");
        JsonObject fields = new JsonObject();
        fields.add("status", stringField("Cancelled"));
        fields.add("cancelledAt", timestampField(cancelledAt));
        if (!client.updateDocument(client.userCollection(LEAVE_COLLECTION), id, fields,
            java.util.Arrays.asList("status", "cancelledAt"))) {
            throw new NoSuchElementException("Leave request was not found in this user sandbox.");
        }
        return current.cancelled(cancelledAt);
    }

    @Override
    public List<DemoTicket> listTickets() {
        List<DemoTicket> tickets = new ArrayList<>();
        for (JsonObject document : client.listDocuments(client.userCollection(TICKET_COLLECTION))) {
            tickets.add(ticketFromDocument(document));
        }
        tickets.sort(Comparator.comparing(DemoTicket::getSubmittedAt).reversed());
        return tickets;
    }

    @Override
    public DemoTicket createTicket(DemoTicket ticket) {
        if (!client.createDocument(client.userCollection(TICKET_COLLECTION), ticket.getId(), ticketFields(ticket))) {
            throw new IllegalStateException("Ticket ID already exists; try submitting again.");
        }
        return ticket;
    }

    @Override
    public DemoTicket cancelTicket(String id, String cancelledAt) {
        DemoTicket current = listTickets().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Ticket was not found in this user sandbox."));
        if (!"Open".equals(current.getStatus())) throw new IllegalStateException("Only an open ticket can be cancelled.");
        JsonObject fields = new JsonObject();
        fields.add("status", stringField("Cancelled"));
        fields.add("cancelledAt", timestampField(cancelledAt));
        if (!client.updateDocument(client.userCollection(TICKET_COLLECTION), id, fields,
            java.util.Arrays.asList("status", "cancelledAt"))) {
            throw new NoSuchElementException("Ticket was not found in this user sandbox.");
        }
        return current.cancelled(cancelledAt);
    }

    @Override
    public DemoLeaveRequest reviewLeaveRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        // shortcut: published Firestore rules have no reviewer role; enable once rules add one.
        throw new UnsupportedOperationException("Leave approval is local-demo only; Firebase rules have no reviewer role.");
    }

    private static JsonObject leaveFields(DemoLeaveRequest request) {
        JsonObject fields = new JsonObject();
        fields.add("id", stringField(request.getId()));
        fields.add("employeeId", stringField(request.getEmployeeId()));
        fields.add("employeeName", stringField(request.getEmployeeName()));
        fields.add("leaveType", stringField(request.getLeaveType()));
        fields.add("startDate", stringField(request.getStartDate()));
        fields.add("endDate", stringField(request.getEndDate()));
        fields.add("note", stringField(request.getNote()));
        fields.add("status", stringField(request.getStatus()));
        fields.add("submittedAt", timestampField(request.getSubmittedAt()));
        return fields;
    }

    private static JsonObject ticketFields(DemoTicket ticket) {
        JsonObject fields = new JsonObject();
        fields.add("id", stringField(ticket.getId()));
        fields.add("employeeId", stringField(ticket.getEmployeeId()));
        fields.add("employeeName", stringField(ticket.getEmployeeName()));
        fields.add("ticketType", stringField(ticket.getTicketType()));
        fields.add("queue", stringField(ticket.getQueue()));
        fields.add("summary", stringField(ticket.getSummary()));
        fields.add("status", stringField(ticket.getStatus()));
        fields.add("submittedAt", timestampField(ticket.getSubmittedAt()));
        return fields;
    }

    private static DemoLeaveRequest leaveFromDocument(JsonObject document) {
        JsonObject fields = fields(document);
        return new DemoLeaveRequest(documentId(document), value(fields, "employeeId"), value(fields, "employeeName"),
            value(fields, "leaveType"), value(fields, "startDate"), value(fields, "endDate"), value(fields, "note"),
            value(fields, "status"), dateTimeValue(fields, "submittedAt"), dateTimeValue(fields, "cancelledAt"));
    }

    private static DemoTicket ticketFromDocument(JsonObject document) {
        JsonObject fields = fields(document);
        return new DemoTicket(documentId(document), value(fields, "employeeId"), value(fields, "employeeName"),
            value(fields, "ticketType"), value(fields, "queue"), value(fields, "summary"), value(fields, "status"),
            dateTimeValue(fields, "submittedAt"), dateTimeValue(fields, "cancelledAt"));
    }

    private static JsonObject fields(JsonObject document) {
        return document.has("fields") && document.get("fields").isJsonObject()
            ? document.getAsJsonObject("fields") : new JsonObject();
    }

    private static String documentId(JsonObject document) {
        String name = document.has("name") ? document.get("name").getAsString() : "";
        int slash = name.lastIndexOf('/');
        if (slash < 0 || slash == name.length() - 1) throw new IllegalStateException("Firestore workflow document has no ID.");
        return name.substring(slash + 1);
    }

    private static String value(JsonObject fields, String name) {
        JsonElement element = fields.get(name);
        if (element == null || !element.isJsonObject()) return "";
        JsonObject typed = element.getAsJsonObject();
        if (typed.has("stringValue")) return typed.get("stringValue").getAsString();
        if (typed.has("timestampValue")) return typed.get("timestampValue").getAsString();
        return "";
    }

    private static String dateTimeValue(JsonObject fields, String name) {
        return fields.has(name) ? value(fields, name) : "";
    }

    private static JsonObject stringField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("stringValue", value);
        return field;
    }

    private static JsonObject timestampField(String value) {
        JsonObject field = new JsonObject();
        field.addProperty("timestampValue", value);
        return field;
    }

    /** Overtime requests are local-demo only (no Firestore rules for them), so Firebase payroll sees none. */
    @Override
    public List<DemoOvertimeRequest> listOvertimeRequests() { return java.util.Collections.emptyList(); }

    @Override
    public DemoOvertimeRequest createOvertimeRequest(DemoOvertimeRequest request) {
        throw new UnsupportedOperationException("Overtime requests are local-demo only.");
    }

    @Override
    public DemoOvertimeRequest reviewOvertimeRequest(String id, String decisionStatus, String reviewerId, String reviewedAt) {
        throw new UnsupportedOperationException("Overtime approval is local-demo only; Firebase rules have no reviewer role.");
    }

    @Override
    public DemoTicket resolveTicket(String id, String reply, String resolvedBy, String resolvedAt) {
        throw new UnsupportedOperationException("Resolving tickets is local-demo only; Firebase rules have no HR role.");
    }

    @Override
    public DemoTicket startTicket(String id, String handlerId, String startedAt) {
        throw new UnsupportedOperationException("Handling tickets is local-demo only; Firebase rules have no handler roles.");
    }
}
