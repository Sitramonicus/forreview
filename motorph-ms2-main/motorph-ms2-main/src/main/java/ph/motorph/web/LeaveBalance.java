package ph.motorph.web;

/** One employee's temporary yearly credits for one leave type, in weekdays. */
public final class LeaveBalance {
    private final String leaveType;
    private final int allowance;
    private final int used;
    private final int pending;

    public LeaveBalance(String leaveType, int allowance, int used, int pending) {
        this.leaveType = leaveType;
        this.allowance = allowance;
        this.used = used;
        this.pending = pending;
    }

    public String getLeaveType() { return leaveType; }
    public int getAllowance() { return allowance; }
    /** Credits taken by approved requests. */
    public int getUsed() { return used; }
    /** Credits held by requests still waiting for review. */
    public int getPending() { return pending; }
    public int getRemaining() { return allowance - used; }
}
