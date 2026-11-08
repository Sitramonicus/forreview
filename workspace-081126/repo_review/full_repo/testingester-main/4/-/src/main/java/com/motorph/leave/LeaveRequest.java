package com.motorph.leave;

import java.time.LocalDate;

/**
 * Represents a leave request made by an employee.
 */
public class LeaveRequest {
    private String employeeName;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private String status; // NEW: Pending / Approved / Rejected

    // Updated constructor including status
    public LeaveRequest(String employeeName, String leaveType, LocalDate startDate,
                        LocalDate endDate, String notes, String status) {
        this.employeeName = employeeName;
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.status = status;
    }

    // Getters and setters
    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getLeaveType() { return leaveType; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return employeeName + " - " + leaveType + " (" + startDate + " to " + endDate + ") Status: " + status;
    }
}