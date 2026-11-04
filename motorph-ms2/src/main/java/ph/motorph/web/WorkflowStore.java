package ph.motorph.web;

import java.util.List;

/** Storage boundary for user-scoped fictional leave requests and help-desk tickets. */
public interface WorkflowStore {
    List<DemoLeaveRequest> listLeaveRequests();
    DemoLeaveRequest createLeaveRequest(DemoLeaveRequest request);
    DemoLeaveRequest cancelLeaveRequest(String id, String cancelledAt);

    List<DemoTicket> listTickets();
    DemoTicket createTicket(DemoTicket ticket);
    DemoTicket cancelTicket(String id, String cancelledAt);
}
