package ph.motorph.web;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/** Domain checks for owner-sandbox workflow validation and state transitions. */
public final class WorkflowServiceTest {
    private static int checks;

    public static void main(String[] args) {
        createsAndCancelsFictionalLeaveRequests();
        rejectsInvalidLeaveRequestsAndIDs();
        routesSyntheticTicketsAndEnforcesCancellationLifecycle();
        listsLocalWorkflowsNewestFirst();
        System.out.println("PASS: " + checks + " workflow-domain checks");
    }

    private static void createsAndCancelsFictionalLeaveRequests() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowService service = service(store);
        DemoLeaveRequest request = service.createLeaveRequest("10001", "Vacation",
            "2026-11-10", "2026-11-11", "Fictional request");
        check(request.getId().matches("LV-[A-F0-9]{12}"), "server-generated leave ID has safe format");
        check("Manuel III Garcia".equals(request.getEmployeeName()), "employee name comes from the synthetic directory");
        check("Pending review".equals(request.getStatus()), "new request starts pending review");
        check("2026-11-10".equals(request.getStartDate()) && "2026-11-11".equals(request.getEndDate()), "leave date range preserved");
        check(store.listLeaveRequests().size() == 1, "created leave request is stored");
        DemoLeaveRequest cancelled = service.cancelLeaveRequest(request.getId());
        check("Cancelled".equals(cancelled.getStatus()) && !cancelled.getCancelledAt().isEmpty(), "cancel records status and timestamp");
        check("Cancelled".equals(store.listLeaveRequests().get(0).getStatus()), "cancelled status persists in store");
    }

    private static void rejectsInvalidLeaveRequestsAndIDs() {
        WorkflowService service = service(new InMemoryWorkflowStore());
        expectIllegal(() -> service.createLeaveRequest("10001", "Vacation", "2026-02-30", "2026-03-01", ""),
            "impossible date rejected");
        expectIllegal(() -> service.createLeaveRequest("10001", "Vacation", "2026-11-12", "2026-11-11", ""),
            "reversed range rejected");
        expectNotFound(() -> service.createLeaveRequest("DEMO-999", "Vacation", "2026-11-10", "2026-11-11", ""),
            "employee must exist in the synthetic directory");
        expectIllegal(() -> service.createLeaveRequest("10001", "Unapproved", "2026-11-10", "2026-11-11", ""),
            "unsupported leave type rejected");
        expectIllegal(() -> service.createLeaveRequest("10001", "Vacation", "2026-11-10", "2026-11-11", "x".repeat(241)),
            "overlong note rejected");
        expectIllegal(() -> service.cancelLeaveRequest("../outside"), "path-like request ID rejected");
        expectNotFound(() -> service.cancelLeaveRequest("LV-FFFFFFFFFFFF"), "missing request returns not found");
    }

    private static void routesSyntheticTicketsAndEnforcesCancellationLifecycle() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowService service = service(store);
        Map<String, String> routes = new HashMap<>();
        routes.put("Overtime request", "Team Lead");
        routes.put("Leave request", "Team Lead");
        routes.put("Timesheet correction", "Team Lead");
        routes.put("Payroll inquiry", "Payroll Staff");
        routes.put("HR inquiry", "HR");
        routes.put("Technical support", "IT Admin");
        for (Map.Entry<String, String> route : routes.entrySet()) {
            DemoTicket ticket = service.createTicket("10002", route.getKey(), "Synthetic test message");
            check(route.getValue().equals(ticket.getQueue()), "ticket queue label maps for " + route.getKey());
            check("Open".equals(ticket.getStatus()), "new ticket status is Open");
        }
        DemoTicket ticket = store.listTickets().get(0);
        DemoTicket cancelled = service.cancelTicket(ticket.getId());
        check("Cancelled".equals(cancelled.getStatus()), "open ticket can be cancelled");
        expectIllegal(() -> service.cancelTicket(ticket.getId()), "cancelled ticket cannot be cancelled again");
        expectIllegal(() -> service.createTicket("10001", "Payroll payout", "No calculation"), "unsupported ticket type rejected");
        expectIllegal(() -> service.createTicket("10001", "HR inquiry", "  "), "empty ticket message rejected");
    }

    private static void listsLocalWorkflowsNewestFirst() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        store.createLeaveRequest(new DemoLeaveRequest("LV-OLD", "10001", "Manuel III Garcia", "Vacation",
            "2026-11-10", "2026-11-10", "", "Pending review", "2026-11-01T10:00:00Z", ""));
        store.createLeaveRequest(new DemoLeaveRequest("LV-NEW", "10001", "Manuel III Garcia", "Personal",
            "2026-11-12", "2026-11-12", "", "Pending review", "2026-11-02T10:00:00Z", ""));
        check("LV-NEW".equals(store.listLeaveRequests().get(0).getId()), "local leave history is newest first, matching Firestore mode");

        store.createTicket(new DemoTicket("TK-OLD", "10001", "Manuel III Garcia", "HR inquiry", "HR",
            "Older synthetic message", "Open", "2026-11-01T10:00:00Z", ""));
        store.createTicket(new DemoTicket("TK-NEW", "10001", "Manuel III Garcia", "Technical support", "IT Admin",
            "Newer synthetic message", "Open", "2026-11-02T10:00:00Z", ""));
        check("TK-NEW".equals(store.listTickets().get(0).getId()), "local ticket history is newest first, matching Firestore mode");
    }

    private static WorkflowService service(WorkflowStore store) {
        return new WorkflowService(new EmployeeService(new InMemoryEmployeeStore()), store);
    }

    private static void expectIllegal(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException | IllegalStateException expected) {
            check(true, message);
        }
    }

    private static void expectNotFound(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (NoSuchElementException expected) {
            check(true, message);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
