package com.motorph.leave;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing leave requests (SL/VL/HL)
 * Handles adding, retrieving, and persisting leave requests to CSV
 */
public class LeaveRequestService {

    private final List<LeaveRequest> leaveRequests;
    private final String csvFilePath;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public LeaveRequestService(String csvFilePath) {
        this.csvFilePath = csvFilePath;
        this.leaveRequests = new ArrayList<>();
        loadLeaveRequestsFromCSV();
    }

    /**
     * Add a new leave request and save to CSV
     */
    public void addLeaveRequest(LeaveRequest leaveRequest) {
        leaveRequests.add(leaveRequest);
        saveAllLeaveRequestsToCSV();
    }

    /**
     * Get all leave requests
     */
    public List<LeaveRequest> getAllLeaveRequests() {
        return new ArrayList<>(leaveRequests);
    }

    /**
     * Save all leave requests to CSV
     */
    private void saveAllLeaveRequestsToCSV() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(csvFilePath))) {
            // Header
            String[] header = {"Employee Name", "Leave Type", "Start Date", "End Date", "Notes", "Status"};
            writer.writeNext(header);

            // Data
            for (LeaveRequest request : leaveRequests) {
                String[] data = {
                        request.getEmployeeName(),
                        request.getLeaveType(),
                        request.getStartDate() != null ? request.getStartDate().format(dateFormatter) : "",
                        request.getEndDate() != null ? request.getEndDate().format(dateFormatter) : "",
                        request.getNotes() != null ? request.getNotes() : "",
                        request.getStatus() != null ? request.getStatus() : "Pending"
                };
                writer.writeNext(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save leave requests to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Load leave requests from CSV file
     */
    private void loadLeaveRequestsFromCSV() {
        File file = new File(csvFilePath);
        if (!file.exists()) return; // no file yet

        try (CSVReader reader = new CSVReader(new FileReader(file))) {
            String[] line;
            boolean isHeader = true;
            while ((line = reader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false; // skip header
                    continue;
                }

                String employeeName = line[0];
                String leaveType = line[1];
                LocalDate startDate = !line[2].isEmpty() ? LocalDate.parse(line[2], dateFormatter) : null;
                LocalDate endDate = !line[3].isEmpty() ? LocalDate.parse(line[3], dateFormatter) : null;
                String notes = line[4];
                String status = line[5];

                LeaveRequest request = new LeaveRequest(employeeName, leaveType, startDate, endDate, notes, status);
                leaveRequests.add(request);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load leave requests from CSV: " + e.getMessage(), e);
        }
    }
}