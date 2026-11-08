package ph.motorph.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Validates and coordinates safe, non-payroll workflow demonstrations. */
public final class WorkflowService {
    private static final int MAX_TEXT_LENGTH = 240;
    private static final List<String> LEAVE_TYPES = Collections.unmodifiableList(
        Arrays.asList("Vacation", "Medical", "Personal", "Unpaid"));
    private static final Map<String, String> TICKET_QUEUES;
    static {
        Map<String, String> queues = new LinkedHashMap<>();
        queues.put("Overtime request", "Team Lead");
        queues.put("Leave request", "Team Lead");
        queues.put("Timesheet correction", "Team Lead");
        queues.put("Payroll inquiry", "Payroll Staff");
        queues.put("HR inquiry", "HR");
        queues.put("Technical support", "IT Admin");
        TICKET_QUEUES = Collections.unmodifiableMap(queues);
    }

    private final EmployeeService employeeService;
    private final WorkflowStore store;

    public WorkflowService(EmployeeService employeeService, WorkflowStore store) {
        if (employeeService == null || store == null) throw new IllegalArgumentException("workflow dependencies must not be null");
        this.employeeService = employeeService;
        this.store = store;
    }

    public List<DemoLeaveRequest> listLeaveRequests() { return store.listLeaveRequests(); }
    public List<DemoTicket> listTickets() { return store.listTickets(); }

    public DemoLeaveRequest createLeaveRequest(String employeeId, String leaveType,
            String startDate, String endDate, String note) {
        Employee employee = requireEmployee(employeeId);
        if (!LEAVE_TYPES.contains(leaveType)) throw new IllegalArgumentException("Choose a valid leave type.");
        LocalDate start = parseDate(startDate, "start date");
        LocalDate end = parseDate(endDate, "end date");
        if (start.isAfter(end)) throw new IllegalArgumentException("The start date must be on or before the end date.");
        String safeNote = optionalText(note, "Note");
        String id = "LV-" + randomSuffix();
        return store.createLeaveRequest(new DemoLeaveRequest(id, employee.getId(), employee.getName(), leaveType,
            start.toString(), end.toString(), safeNote, "Pending review", Instant.now().toString(), ""));
    }

    public DemoLeaveRequest cancelLeaveRequest(String id) {
        requireId(id, "LV-");
        return store.cancelLeaveRequest(id, Instant.now().toString());
    }

    public DemoTicket createTicket(String employeeId, String ticketType, String summary) {
        Employee employee = requireEmployee(employeeId);
        String queue = TICKET_QUEUES.get(ticketType);
        if (queue == null) throw new IllegalArgumentException("Choose a valid ticket type.");
        String safeSummary = requiredText(summary, "Message");
        String id = "TK-" + randomSuffix();
        return store.createTicket(new DemoTicket(id, employee.getId(), employee.getName(), ticketType, queue,
            safeSummary, "Open", Instant.now().toString(), ""));
    }

    public DemoTicket cancelTicket(String id) {
        requireId(id, "TK-");
        return store.cancelTicket(id, Instant.now().toString());
    }

    private Employee requireEmployee(String employeeId) {
        String id = requiredText(employeeId, "Employee ID");
        return employeeService.findEmployee(id).orElseThrow(() ->
            new NoSuchElementException("Choose an employee from your synthetic demo directory."));
    }

    private static LocalDate parseDate(String value, String field) {
        String text = requiredText(value, field);
        try {
            LocalDate date = LocalDate.parse(text);
            if (!date.toString().equals(text)) throw new IllegalArgumentException("Enter a valid YYYY-MM-DD " + field + ".");
            return date;
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter a valid YYYY-MM-DD " + field + ".");
        }
    }

    private static String requiredText(String value, String field) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) throw new IllegalArgumentException(field + " is required.");
        if (text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException(field + " must be " + MAX_TEXT_LENGTH + " characters or fewer.");
        return text;
    }

    private static String optionalText(String value, String field) {
        String text = value == null ? "" : value.trim();
        if (text.length() > MAX_TEXT_LENGTH) throw new IllegalArgumentException(field + " must be " + MAX_TEXT_LENGTH + " characters or fewer.");
        return text;
    }

    private static String randomSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static void requireId(String id, String prefix) {
        if (id == null || !id.matches(prefix + "[A-F0-9]{12}")) throw new IllegalArgumentException("Workflow record ID is invalid.");
    }
}
