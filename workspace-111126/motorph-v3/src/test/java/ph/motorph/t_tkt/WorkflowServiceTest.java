package ph.motorph.t_tkt;

import ph.motorph.o_oop.EmployeeService;
import ph.motorph.o_oop.InMemoryEmployeeStore;
import ph.motorph.o_oop.LeaveBalance;
import ph.motorph.o_oop.LeavePolicy;
import ph.motorph.t_tkt.LeaveRequest;
import ph.motorph.t_tkt.LeaveTicket;
import ph.motorph.t_tkt.InMemoryWorkflowStore;
import ph.motorph.t_tkt.WorkflowService;
import ph.motorph.t_tkt.WorkflowStore;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public final class WorkflowServiceTest {
    private static int checks;

    public static void main(String[] args) {
        createsAndCancelsLeaveRequests();
        rejectsInvalidLeaveRequestsAndIDs();
        routesTicketsAndEnforcesCancellationLifecycle();
        listsLocalWorkflowsNewestFirst();
        approvesAndRejectsPendingLeaveRequests();
        tracksTemporaryLeaveBalances();
        System.out.println("PASS: " + checks + " workflow-domain checks");
    }

    private static void createsAndCancelsLeaveRequests() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowService service = service(store);
        LeaveRequest request = service.createLeaveRequest("10018", "Vacation",
            "2026-11-10", "2026-11-11", "Timesheet correction");
        check(request.getId().matches("LV-[A-F0-9]{12}"), "server-generated leave ID has safe format");
        check(request.getEmployeeName().equals("Allison San Jose"), "the employee name comes from the employee file");
        check("Pending review".equals(request.getStatus()), "new request starts pending review");
        check("2026-11-10".equals(request.getStartDate()) && "2026-11-11".equals(request.getEndDate()), "leave date range preserved");
        check(store.listLeaveRequests().size() == 1, "created leave request is stored");
        LeaveRequest cancelled = service.cancelLeaveRequest(request.getId());
        check("Cancelled".equals(cancelled.getStatus()) && !cancelled.getCancelledAt().isEmpty(), "cancel records status and timestamp");
        check("Cancelled".equals(store.listLeaveRequests().get(0).getStatus()), "cancelled status persists in store");
    }

    private static void rejectsInvalidLeaveRequestsAndIDs() {
        WorkflowService service = service(new InMemoryWorkflowStore());
        expectIllegal(() -> service.createLeaveRequest("10018", "Vacation", "2026-02-30", "2026-03-01", ""),
            "impossible date rejected");
        expectIllegal(() -> service.createLeaveRequest("10018", "Vacation", "2026-11-12", "2026-11-11", ""),
            "reversed range rejected");
        expectNotFound(() -> service.createLeaveRequest("DEMO-999", "Vacation", "2026-11-10", "2026-11-11", ""),
            "employee must exist in the employee directory");
        expectIllegal(() -> service.createLeaveRequest("10018", "Unapproved", "2026-11-10", "2026-11-11", ""),
            "unsupported leave type rejected");
        expectIllegal(() -> service.createLeaveRequest("10018", "Vacation", "2026-11-10", "2026-11-11", "x".repeat(241)),
            "overlong note rejected");
        expectIllegal(() -> service.cancelLeaveRequest("../outside"), "path-like request ID rejected");
        expectNotFound(() -> service.cancelLeaveRequest("LV-FFFFFFFFFFFF"), "missing request returns not found");
    }

    private static void routesTicketsAndEnforcesCancellationLifecycle() {
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
            LeaveTicket ticket = service.createTicket("10016", route.getKey(), "Test message");
            check(route.getValue().equals(ticket.getQueue()), "ticket queue label maps for " + route.getKey());
            check("Open".equals(ticket.getStatus()), "new ticket status is Open");
        }
        LeaveTicket ticket = store.listTickets().get(0);
        LeaveTicket cancelled = service.cancelTicket(ticket.getId());
        check("Cancelled".equals(cancelled.getStatus()), "open ticket can be cancelled");
        expectIllegal(() -> service.cancelTicket(ticket.getId()), "cancelled ticket cannot be cancelled again");
        expectIllegal(() -> service.createTicket("10018", "Payroll payout", "No calculation"), "unsupported ticket type rejected");
        expectIllegal(() -> service.createTicket("10018", "HR inquiry", "  "), "empty ticket message rejected");
    }

    private static void listsLocalWorkflowsNewestFirst() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        store.createLeaveRequest(new LeaveRequest("LV-OLD", "10018", "Mara Rivera", "Vacation",
            "2026-11-10", "2026-11-10", "", "Pending review", "2026-11-01T10:00:00Z", ""));
        store.createLeaveRequest(new LeaveRequest("LV-NEW", "10018", "Mara Rivera", "Personal",
            "2026-11-12", "2026-11-12", "", "Pending review", "2026-11-02T10:00:00Z", ""));
        check("LV-NEW".equals(store.listLeaveRequests().get(0).getId()), "local leave history is newest first, matching Firestore mode");

        store.createTicket(new LeaveTicket("TK-OLD", "10018", "Mara Rivera", "HR inquiry", "HR",
            "Older message", "Open", "2026-11-01T10:00:00Z", ""));
        store.createTicket(new LeaveTicket("TK-NEW", "10018", "Mara Rivera", "Technical support", "IT Admin",
            "Newer message", "Open", "2026-11-02T10:00:00Z", ""));
        check("TK-NEW".equals(store.listTickets().get(0).getId()), "local ticket history is newest first, matching Firestore mode");
    }

    private static void approvesAndRejectsPendingLeaveRequests() {
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowService service = service(store);
        LeaveRequest first = service.createLeaveRequest("10018", "Vacation", "2026-11-10", "2026-11-11", "");
        LeaveRequest approved = service.reviewLeaveRequest(first.getId(), "approve", "10016");
        check("Approved".equals(approved.getStatus()), "pending request can be approved");
        check("10016".equals(approved.getReviewerId()) && !approved.getReviewedAt().isEmpty(), "approval records reviewer and time");
        check("Approved".equals(store.listLeaveRequests().get(0).getStatus()), "approved status persists in store");
        expectIllegal(() -> service.reviewLeaveRequest(first.getId(), "reject", "10016"), "decided request cannot be reviewed again");
        expectIllegal(() -> service.cancelLeaveRequest(first.getId()), "approved request cannot be cancelled");

        LeaveRequest second = service.createLeaveRequest("10018", "Medical", "2026-11-12", "2026-11-12", "");
        expectIllegal(() -> service.reviewLeaveRequest(second.getId(), "approve", "10018"), "employee cannot review own request");
        expectIllegal(() -> service.reviewLeaveRequest(second.getId(), "maybe", "10016"), "unknown decision rejected");
        expectNotFound(() -> service.reviewLeaveRequest(second.getId(), "approve", "DEMO-999"), "reviewer must exist in directory");
        LeaveRequest rejected = service.reviewLeaveRequest(second.getId(), "reject", "10016");
        check("Rejected".equals(rejected.getStatus()) && "10016".equals(rejected.getReviewerId()), "pending request can be rejected");

        LeaveRequest third = service.createLeaveRequest("10018", "Personal", "2026-11-13", "2026-11-13", "");
        service.cancelLeaveRequest(third.getId());
        expectIllegal(() -> service.reviewLeaveRequest(third.getId(), "approve", "10016"), "cancelled request cannot be approved");
        expectNotFound(() -> service.reviewLeaveRequest("LV-FFFFFFFFFFFF", "approve", "10016"), "missing request returns not found");
    }

    private static void tracksTemporaryLeaveBalances() {
        check(LeavePolicy.creditsFor(java.time.LocalDate.parse("2026-11-13"), java.time.LocalDate.parse("2026-11-16")) == 2,
            "Fri-Mon request counts 2 weekdays");
        check(LeavePolicy.creditsFor(java.time.LocalDate.parse("2026-11-14"), java.time.LocalDate.parse("2026-11-15")) == 0,
            "weekend-only request counts 0 weekdays");
        InMemoryWorkflowStore store = new InMemoryWorkflowStore();
        WorkflowService service = service(store);
        LeaveBalance fresh = service.leaveBalances("10018", 2026).get(0);
        check("Vacation".equals(fresh.getLeaveType()) && fresh.getAllowance() == 5 && fresh.getRemaining() == 5,
            "new employee starts with 5 temporary vacation credits");

        LeaveRequest three = service.createLeaveRequest("10018", "Vacation", "2026-11-09", "2026-11-11", "");
        LeaveBalance held = service.leaveBalances("10018", 2026).get(0);
        check(held.getPending() == 3 && held.getRemaining() == 5, "pending request holds credits without using them");
        expectIllegal(() -> service.createLeaveRequest("10018", "Vacation", "2026-11-16", "2026-11-18", ""),
            "request beyond credits left after pending is rejected");
        service.reviewLeaveRequest(three.getId(), "approve", "10016");
        LeaveBalance used = service.leaveBalances("10018", 2026).get(0);
        check(used.getUsed() == 3 && used.getPending() == 0 && used.getRemaining() == 2, "approval uses credits");

        LeaveRequest two = service.createLeaveRequest("10018", "Vacation", "2026-11-16", "2026-11-17", "");
        check(two.getId().startsWith("LV-"), "request using exactly the remaining credits is accepted");
        service.cancelLeaveRequest(two.getId());
        check(service.leaveBalances("10018", 2026).get(0).getPending() == 0, "cancelled request frees held credits");
        check(service.leaveBalances("10018", 2027).get(0).getRemaining() == 5, "another year has its own credits");
        check(service.leaveBalances("10016", 2026).get(0).getRemaining() == 5, "other employees are unaffected");
        service.createLeaveRequest("10018", "Unpaid", "2026-11-02", "2026-11-27", "");
        check(service.leaveBalances("10018", 2026).stream().noneMatch(b -> "Unpaid".equals(b.getLeaveType())),
            "unpaid leave is not limited or listed");
    }

    private static WorkflowService service(WorkflowStore store) {
        return new WorkflowService(new EmployeeService(ph.motorph.o_oop.TestRoster.store()), store);
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
