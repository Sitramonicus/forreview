package ph.motorph.t_tkt;

import ph.motorph.o_oop.Employee;
import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.LeaveBalance;
import ph.motorph.o_oop.LeavePolicy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

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
        queues.put("Reschedule request", "Team Lead");
        queues.put("Payroll inquiry", "Payroll Staff");
        queues.put("HR inquiry", "HR");
        queues.put("General inquiry", "HR");
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

    public List<LeaveRequest> listLeaveRequests() { return store.listLeaveRequests(); }
    public List<LeaveTicket> listTickets() { return store.listTickets(); }
    public List<OvertimeRequest> listOvertimeRequests() { return store.listOvertimeRequests(); }

    
    public OvertimeRequest createOvertimeRequest(String employeeId, String date, int minutes, String note) {
        Employee employee = requireEmployee(employeeId);
        LocalDate day = parseDate(date, "overtime date");
        if (minutes < 1 || minutes > 720) throw new IllegalArgumentException("Overtime must be 1 to 720 minutes.");
        String safeNote = optionalText(note, "Note");
        boolean duplicate = store.listOvertimeRequests().stream().anyMatch(item -> item.getEmployeeId().equals(employee.getId())
            && item.getDate().equals(day.toString()) && !"Rejected".equals(item.getStatus()));
        if (duplicate) throw new IllegalStateException("There is already a pending or approved overtime request for that date.");
        return store.createOvertimeRequest(new OvertimeRequest("OT-" + randomSuffix(), employee.getId(), employee.getName(),
            day.toString(), minutes, safeNote, "Pending review", Instant.now().toString(), "", ""));
    }

    
    public OvertimeRequest reviewOvertimeRequest(String id, String decision, String reviewerId) {
        requireId(id, "OT-");
        String status = decisionStatus(decision);
        Employee reviewer = requireEmployee(reviewerId);
        OvertimeRequest current = store.listOvertimeRequests().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Overtime request was not found."));
        if (current.getEmployeeId().equals(reviewer.getId())) {
            throw new IllegalArgumentException("A reviewer cannot approve or reject their own overtime request.");
        }
        return store.reviewOvertimeRequest(id, status, reviewer.getId(), Instant.now().toString());
    }

    private static String decisionStatus(String decision) {
        if ("approve".equals(decision)) return "Approved";
        if ("reject".equals(decision)) return "Rejected";
        throw new IllegalArgumentException("Decision must be approve or reject.");
    }

    public LeaveRequest createLeaveRequest(String employeeId, String leaveType,
            String startDate, String endDate, String note) {
        Employee employee = requireEmployee(employeeId);
        if (!LEAVE_TYPES.contains(leaveType)) throw new IllegalArgumentException("Choose a valid leave type.");
        LocalDate start = parseDate(startDate, "start date");
        LocalDate end = parseDate(endDate, "end date");
        if (start.isAfter(end)) throw new IllegalArgumentException("The start date must be on or before the end date.");
        String safeNote = optionalText(note, "Note");
        if (LeavePolicy.isLimited(leaveType)) {
            LeaveBalance balance = balanceFor(employee.getId(), leaveType, start.getYear());
            int requested = LeavePolicy.creditsFor(start, end);
            int available = balance.getRemaining() - balance.getPending();
            if (requested > available) {
                throw new IllegalArgumentException("Not enough " + leaveType + " credits: requested " + requested
                    + " weekday(s), " + available + " available after pending requests.");
            }
        }
        String id = "LV-" + randomSuffix();
        return store.createLeaveRequest(new LeaveRequest(id, employee.getId(), employee.getName(), leaveType,
            start.toString(), end.toString(), safeNote, "Pending review", Instant.now().toString(), ""));
    }

    public LeaveRequest cancelLeaveRequest(String id) {
        requireId(id, "LV-");
        return store.cancelLeaveRequest(id, Instant.now().toString());
    }

    
    public LeaveRequest reviewLeaveRequest(String id, String decision, String reviewerId) {
        requireId(id, "LV-");
        String status = decisionStatus(decision);
        Employee reviewer = requireEmployee(reviewerId);
        LeaveRequest current = store.listLeaveRequests().stream().filter(item -> item.getId().equals(id)).findFirst()
            .orElseThrow(() -> new NoSuchElementException("Leave request was not found."));
        if (current.getEmployeeId().equals(reviewer.getId())) {
            throw new IllegalArgumentException("A reviewer cannot approve or reject their own leave request.");
        }
        if ("Approved".equals(status) && LeavePolicy.isLimited(current.getLeaveType())) {
            LocalDate start = LocalDate.parse(current.getStartDate());
            LeaveBalance balance = balanceFor(current.getEmployeeId(), current.getLeaveType(), start.getYear());
            if (creditsOf(current) > balance.getRemaining()) {
                throw new IllegalStateException("Not enough " + current.getLeaveType() + " credits left to approve this request.");
            }
        }
        return store.reviewLeaveRequest(id, status, reviewer.getId(), Instant.now().toString());
    }

    
    public List<LeaveBalance> leaveBalances(String employeeId, int year) {
        Employee employee = requireEmployee(employeeId);
        List<LeaveBalance> balances = new ArrayList<>();
        for (String type : LeavePolicy.limitedTypes()) balances.add(balanceFor(employee.getId(), type, year));
        return balances;
    }

    private LeaveBalance balanceFor(String employeeId, String leaveType, int year) {
        int used = 0;
        int pending = 0;
        for (LeaveRequest request : store.listLeaveRequests()) {
            if (!request.getEmployeeId().equals(employeeId) || !request.getLeaveType().equals(leaveType)) continue;
            
            if (LocalDate.parse(request.getStartDate()).getYear() != year) continue;
            if ("Approved".equals(request.getStatus())) used += creditsOf(request);
            else if ("Pending review".equals(request.getStatus())) pending += creditsOf(request);
        }
        return new LeaveBalance(leaveType, LeavePolicy.yearlyCredits(leaveType), used, pending);
    }

    private static int creditsOf(LeaveRequest request) {
        return LeavePolicy.creditsFor(LocalDate.parse(request.getStartDate()), LocalDate.parse(request.getEndDate()));
    }

    public LeaveTicket createTicket(String employeeId, String ticketType, String summary) {
        Employee employee = requireEmployee(employeeId);
        String queue = TICKET_QUEUES.get(ticketType);
        if (queue == null) throw new IllegalArgumentException("Choose a valid ticket type.");
        String safeSummary = requiredText(summary, "Message");
        String id = "TK-" + randomSuffix();
        return store.createTicket(new LeaveTicket(id, employee.getId(), employee.getName(), ticketType, queue,
            safeSummary, "Open", Instant.now().toString(), ""));
    }

    
    public LeaveTicket resolveTicket(String id, String reply, String resolvedBy) {
        requireId(id, "TK-");
        String text = reply == null ? "" : reply.trim();
        if (text.length() < 2 || text.length() > 400) throw new IllegalArgumentException("Write a reply (2 to 400 characters).");
        return store.resolveTicket(id, text, resolvedBy, Instant.now().toString());
    }

    
    public LeaveTicket startTicket(String id, String handlerId) {
        return store.startTicket(id, handlerId, Instant.now().toString());
    }

    public LeaveTicket cancelTicket(String id) {
        requireId(id, "TK-");
        return store.cancelTicket(id, Instant.now().toString());
    }

    private Employee requireEmployee(String employeeId) {
        String id = requiredText(employeeId, "Employee ID");
        return employeeService.findEmployee(id).orElseThrow(() ->
            new NoSuchElementException("No employee " + employeeId + " is on the roster, so no request can be filed for them."));
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
