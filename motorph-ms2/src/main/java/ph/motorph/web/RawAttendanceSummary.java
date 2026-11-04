package ph.motorph.web;

/** Read-only raw-record coverage for one synthetic directory employee. */
public final class RawAttendanceSummary {
    private final String employeeId;
    private final String employeeName;
    private final Integer scheduleId;
    private final int rawRecordCount;
    private final String firstRecordDate;
    private final String lastRecordDate;

    public RawAttendanceSummary(String employeeId, String employeeName, Integer scheduleId,
            int rawRecordCount, String firstRecordDate, String lastRecordDate) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.scheduleId = scheduleId;
        this.rawRecordCount = rawRecordCount;
        this.firstRecordDate = firstRecordDate == null ? "" : firstRecordDate;
        this.lastRecordDate = lastRecordDate == null ? "" : lastRecordDate;
    }

    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Integer getScheduleId() { return scheduleId; }
    public boolean hasScheduleMapping() { return scheduleId != null; }
    public int getRawRecordCount() { return rawRecordCount; }
    public String getFirstRecordDate() { return firstRecordDate; }
    public String getLastRecordDate() { return lastRecordDate; }
}
