package ph.motorph.t_tkt;

import java.util.List;

/** Storage boundary for user-scoped fictional leave requests and help-desk tickets. */
public interface WorkflowStore {
    List<DemoLeaveRequest> listLeaveRequests();
    DemoLeaveRequest createLeaveRequest(DemoLeaveRequest request);
    DemoLeaveRequest cancelLeaveRequest(String id, String cancelledAt);
    DemoLeaveRequest reviewLeaveRequest(String id, String decisionStatus, String reviewerId, String reviewedAt);

    List<DemoOvertimeRequest> listOvertimeRequests();
    DemoOvertimeRequest createOvertimeRequest(DemoOvertimeRequest request);
    DemoOvertimeRequest reviewOvertimeRequest(String id, String decisionStatus, String reviewerId, String reviewedAt);
    List<DemoTicket> listTickets();
    DemoTicket createTicket(DemoTicket ticket);
    DemoTicket cancelTicket(String id, String cancelledAt);
    DemoTicket resolveTicket(String id, String reply, String resolvedBy, String resolvedAt);
    DemoTicket startTicket(String id, String handlerId, String startedAt);
}
