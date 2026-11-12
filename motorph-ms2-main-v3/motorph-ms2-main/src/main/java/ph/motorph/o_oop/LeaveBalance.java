package ph.motorph.o_oop;

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
    
    public int getUsed() { return used; }
    
    public int getPending() { return pending; }
    public int getRemaining() { return allowance - used; }
}
