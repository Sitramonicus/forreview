package com.motorph.view;

import com.motorph.leave.LeaveRequest;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for submitting leave requests (SL/VL/HL) and viewing them in a table.
 */
public class LeavePanel extends JPanel {

    private JTextField employeeNameField;
    private JComboBox<String> leaveTypeCombo;
    private JTextField leaveDaysField;
    private JButton submitButton;

    // Table to display leave requests
    private JTable leaveTable;
    private DefaultTableModel tableModel;
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    public LeavePanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // --- TITLE ---
        JLabel titleLabel = new JLabel("Leave Request");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(titleLabel, gbc);

        // Employee Name
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Employee Name:"), gbc);
        employeeNameField = new JTextField(20);
        gbc.gridx = 1;
        add(employeeNameField, gbc);

        // Leave Type
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Leave Type:"), gbc);
        leaveTypeCombo = new JComboBox<>(new String[]{"Sick Leave", "Vacation Leave", "Half Leave"});
        gbc.gridx = 1;
        add(leaveTypeCombo, gbc);

        // Number of Days
        gbc.gridx = 0;
        gbc.gridy = 3;
        add(new JLabel("Number of Days:"), gbc);
        leaveDaysField = new JTextField(5);
        gbc.gridx = 1;
        add(leaveDaysField, gbc);

        // Submit Button
        submitButton = new JButton("Submit Leave Request");
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        add(submitButton, gbc);

        // Button action
        submitButton.addActionListener(new SubmitListener());

        // Initialize table below the form
        String[] columns = {"Employee Name", "Leave Type", "Start Date", "End Date", "Notes", "Status"};
        tableModel = new DefaultTableModel(columns, 0);
        leaveTable = new JTable(tableModel);
        leaveTable.setFillsViewportHeight(true);

        JScrollPane tableScroll = new JScrollPane(leaveTable);
        GridBagConstraints tableGbc = new GridBagConstraints();
        tableGbc.gridx = 0;
        tableGbc.gridy = 5;
        tableGbc.gridwidth = 2;
        tableGbc.fill = GridBagConstraints.BOTH;
        tableGbc.weightx = 1;
        tableGbc.weighty = 1;
        tableGbc.insets = new Insets(20, 10, 10, 10);
        add(tableScroll, tableGbc);
    }

    // Listener for submitting leave
    private class SubmitListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String employeeName = employeeNameField.getText().trim();
            String leaveType = leaveTypeCombo.getSelectedItem().toString();
            String daysText = leaveDaysField.getText().trim();

            if (employeeName.isEmpty() || daysText.isEmpty()) {
                JOptionPane.showMessageDialog(LeavePanel.this,
                        "Please fill all fields.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int days;
            try {
                days = Integer.parseInt(daysText);
                if (days <= 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(LeavePanel.this,
                        "Number of days must be a positive integer.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusDays(days - 1); // calculate end date correctly
            String notes = ""; // optional notes
            String status = "Pending"; // default status for new leave request

            // Create LeaveRequest object
            LeaveRequest leaveRequest = new LeaveRequest(employeeName, leaveType, startDate, endDate, notes, status);

            // Add to list
            leaveRequests.add(leaveRequest);

            // Add to table
            tableModel.addRow(new Object[]{
                    leaveRequest.getEmployeeName(),
                    leaveRequest.getLeaveType(),
                    leaveRequest.getStartDate(),
                    leaveRequest.getEndDate(),
                    leaveRequest.getNotes(),
                    leaveRequest.getStatus()
            });

            // Show success message
            JOptionPane.showMessageDialog(LeavePanel.this,
                    "Leave request submitted for " + leaveRequest.getEmployeeName(),
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            // Clear form
            employeeNameField.setText("");
            leaveDaysField.setText("");
            leaveTypeCombo.setSelectedIndex(0);
        }
    }
}