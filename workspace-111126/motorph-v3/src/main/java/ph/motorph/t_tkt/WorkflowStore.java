package ph.motorph.t_tkt;

import java.util.List;

public interface WorkflowStore {
    List<LeaveRequest> listLeaveRequests();
    LeaveRequest createLeaveRequest(LeaveRequest request);
    LeaveRequest cancelLeaveRequest(String id, String cancelledAt);
    LeaveRequest reviewLeaveRequest(String id, String decisionStatus, String reviewerId, String reviewedAt);

    List<OvertimeRequest> listOvertimeRequests();
    OvertimeRequest createOvertimeRequest(OvertimeRequest request);
    OvertimeRequest reviewOvertimeRequest(String id, String decisionStatus, String reviewerId, String reviewedAt);
    List<LeaveTicket> listTickets();
    LeaveTicket createTicket(LeaveTicket ticket);
    LeaveTicket cancelTicket(String id, String cancelledAt);
    LeaveTicket resolveTicket(String id, String reply, String resolvedBy, String resolvedAt);
    LeaveTicket startTicket(String id, String handlerId, String startedAt);
}
