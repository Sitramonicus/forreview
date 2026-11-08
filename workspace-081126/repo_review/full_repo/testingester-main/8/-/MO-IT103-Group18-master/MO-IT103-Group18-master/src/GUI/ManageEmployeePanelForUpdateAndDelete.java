package GUI;

import Backend.Employee;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import com.github.lgooddatepicker.components.DatePicker;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.Arrays;
import javax.swing.JComboBox;

public class ManageEmployeePanelForUpdateAndDelete {

    // This will open a new panel to manage employee records (update and delete)
    public static void displayManageEmployeesPanel(JTable employeeTable, DefaultTableModel model) {

        // Ensure the user selects an employee before managing the data
        int selectedRow = employeeTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select an employee first!", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String employeeNumber = employeeTable.getValueAt(selectedRow, 0).toString();

        // Retrieve full employee details from the CSV instead of JTable data
        String[] employeeDetails = GetEmployeeDetails.getEmployeeDetails(employeeNumber);

        if (employeeDetails == null) {
            JOptionPane.showMessageDialog(null, "Error: Employee record not found in CSV!", "Data Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        //Remove quotes before displaying in text fields
        for (int i = 0; i < employeeDetails.length; i++) {
            employeeDetails[i] = ViewEmployee.forQuotation(employeeDetails[i]); 
        }

        // Create a new frame for managing employee data
        JFrame manageFrame = new JFrame("Manage Employee");
        manageFrame.setSize(600, 700);
        manageFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        manageFrame.setLocationRelativeTo(null);
        
        //Create the main panel using GridBagLayout to make it flexible
        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        detailsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailsPanel.setAlignmentY(Component.CENTER_ALIGNMENT);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        
        //We make it scrollable because the textfields or employee details is too many
        JScrollPane scrollPane = new JScrollPane(detailsPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Smoother scroll

        // Define labels and corresponding text fields
        String[] labels = {
            "Employee Number", "Last Name", "First Name", "Birthday", "Address", "Phone Number",
            "SSS Number", "PhilHealth Number", "TIN", "Pag-IBIG Number", "Status",
            "Position", "Immediate Supervisor", "Basic Salary", "Rice Subsidy", "Phone Allowance",
            "Clothing Allowance", "Gross Semi-Monthly Rate", "Hourly Rate"
        };
        
        //Here we create an array of textfields that equals to the number of labels
        JTextField[] textFields = new JTextField[labels.length];
        String[] originalValues = new String[labels.length];
        final DatePicker[] birthdayPicker = new DatePicker[1];
        JComboBox<String> statusCombo = new JComboBox<>(new String[] {
            "Regular", "Probationary"
        });
        
        //It's a for loop that go through each label that creates and position the label field pairs
        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.anchor = GridBagConstraints.EAST;
            detailsPanel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1;
            gbc.anchor = GridBagConstraints.WEST;

            if (i == 3) { //Use DatePicker for Birthday
                birthdayPicker[0] = new DatePicker();
                String cleanedValue = cleanQuotes(employeeDetails[i]);
                if (!cleanedValue.isEmpty()) {
                    try {
                        LocalDate parsedDate = LocalDate.parse(cleanedValue, DateTimeFormatter.ofPattern("MM-dd-yyyy"));
                        birthdayPicker[0].setDate(parsedDate);
                    } catch (Exception ex) {
                        System.err.println("Invalid date format for birthday: " + cleanedValue);
                    }
                }
                originalValues[i] = cleanedValue;
                detailsPanel.add(birthdayPicker[0], gbc);
            } else if (i==10){
                String cleanedStatus = cleanQuotes(employeeDetails[1]);
                statusCombo.setSelectedItem(cleanedStatus);
                originalValues[1] = cleanedStatus;
                detailsPanel.add(statusCombo, gbc);
            }else {
                textFields[i] = new JTextField(19);
                String cleanedValue = cleanQuotes(employeeDetails[i]);
                textFields[i].setText(cleanedValue);
                originalValues[i] = cleanedValue;

                if (i == 0) {
                    textFields[i].setEditable(false);
                    textFields[i].setBackground(Color.LIGHT_GRAY);
                    textFields[i].setToolTipText("Employee ID cannot be changed.");
                }

                detailsPanel.add(textFields[i], gbc);
            }
        }
        
        

        // Define update and delete buttons
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");
        updateButton.setEnabled(false);
        //We set the position of these buttons under the form fields
        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        detailsPanel.add(updateButton, gbc);
        //It positions the delete button on the next row after the update button
        gbc.gridy = labels.length + 1;
        detailsPanel.add(deleteButton, gbc);
        //Then add the scrollable panel to the main frame and display the em,ployee window
        manageFrame.add(scrollPane, BorderLayout.CENTER);
        manageFrame.setVisible(true);
        
        statusCombo.addActionListener(e -> {
            if (!statusCombo.getSelectedItem().equals(originalValues[10])){
                updateButton.setEnabled(true);
                deleteButton.setEnabled(true);
            }
        });
        
        //Enable buttons only if any field has changed
        for (int i = 1; i < textFields.length; i++) { //Skip Employee ID at index 0
            if (i == 3|| i == 10) continue;
            textFields[i].getDocument().addDocumentListener(new DocumentListener() {
                private void checkForChanges() {
                    for (int j = 1; j < textFields.length; j++) {
                        if (j == 3|| j == 10) continue;
                        if (!textFields[j].getText().equals(originalValues[j])) {
                            updateButton.setEnabled(true);
                            deleteButton.setEnabled(true);
                            return;
                        }
                    }
                    updateButton.setEnabled(false);
                    deleteButton.setEnabled(false);
                }

                @Override
                public void insertUpdate(DocumentEvent e) { checkForChanges(); }
                @Override
                public void removeUpdate(DocumentEvent e) { checkForChanges(); }
                @Override
                public void changedUpdate(DocumentEvent e) { checkForChanges(); }
            });
        }
        
        birthdayPicker[0].addDateChangeListener(e -> {
            String selectedDate = (birthdayPicker[0].getDate() != null)
                ? birthdayPicker[0].getDate().format(DateTimeFormatter.ofPattern("MM-dd-yyyy"))
                : "";

            if (!selectedDate.equals(originalValues[3])) {
                updateButton.setEnabled(true);
            } else {
                // Optional: reset if user changes back to original date
                boolean fieldChanged = false;
                for (int i = 1; i < textFields.length; i++) {
                    if (i == 3) continue; // skip birthday, already handled
                    if (!textFields[i].getText().equals(originalValues[i])) {
                        fieldChanged = true;
                        break;
                    }
                }
                updateButton.setEnabled(fieldChanged);
            }
        });

        
        

        // Update action listener
        updateButton.addActionListener(e -> {
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(manageFrame, "Please select an employee first!", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            //It collects updated field values from textFields
            String[] updatedEmployee = new String[textFields.length];
            int[] numericFields = {5, 6, 7, 8, 9, 13, 14, 15, 16, 17, 18};

            for (int i = 0; i < textFields.length; i++) {
                final int fieldIndex = i;
                if (fieldIndex == 3) {
                    // Handle birthday using the DatePicker
                    LocalDate selected = birthdayPicker[0].getDate();
                    updatedEmployee[fieldIndex] = (selected != null)
                        ? UpdateEmployee.removeQuotation(selected.format(DateTimeFormatter.ofPattern("MM-dd-yyyy")))
                            : "";
                }else if (fieldIndex == 10) {
                    updatedEmployee[fieldIndex] = UpdateEmployee.removeQuotation((String) statusCombo.getSelectedItem());
                }else {
                    String input = textFields[fieldIndex].getText().trim();
                    boolean isChanged = !input.equals(originalValues[fieldIndex]);
                    boolean isEmpty = input.isEmpty();

                    
                    // Required field check
                    if (isChanged && isEmpty) {
                        JOptionPane.showMessageDialog(manageFrame,
                            "Field \"" + labels[fieldIndex] + "\" cannot be empty.",
                            "Missing Input", JOptionPane.WARNING_MESSAGE);
                        textFields[fieldIndex].setBackground(Color.PINK);
                        textFields[fieldIndex].requestFocus();
                        return;
                    }

                    // Numeric validation for designated fields
                    boolean requiresNumeric = Arrays.stream(numericFields).anyMatch(n -> n == fieldIndex);
                    if (isChanged && requiresNumeric && !input.matches("\\d{1,3}(,\\d{3})*(\\.\\d{1,2})?")) {
                        JOptionPane.showMessageDialog(manageFrame,
                            "Field \"" + labels[fieldIndex] + "\" must contain a valid number.",
                            "Invalid Input", JOptionPane.WARNING_MESSAGE);
                        textFields[fieldIndex].setBackground(Color.PINK);
                        textFields[fieldIndex].requestFocus();
                        return;
                    }

                    
                    updatedEmployee[fieldIndex] = UpdateEmployee.removeQuotation(textFields[fieldIndex].getText().trim());
                    
                }
            }


            // OOP validation: build an Employee object from the updated values before saving.
            // The Employee setters reject a basic salary or hourly rate of 0 or less,
            // so invalid pay values never reach the CSV (same rule as the New Employee form).
            try {
                new Employee(
                        updatedEmployee[0],                                   // Employee Number
                        updatedEmployee[1],                                   // Last Name
                        updatedEmployee[2],                                   // First Name
                        updatedEmployee[11],                                  // Position
                        updatedEmployee[10],                                  // Status
                        Double.parseDouble(updatedEmployee[13].replace(",", "")),  // Basic Salary
                        Double.parseDouble(updatedEmployee[18].replace(",", "")),  // Hourly Rate
                        Double.parseDouble(updatedEmployee[14].replace(",", "")),  // Rice Subsidy
                        Double.parseDouble(updatedEmployee[15].replace(",", "")),  // Phone Allowance
                        Double.parseDouble(updatedEmployee[16].replace(",", ""))); // Clothing Allowance
            } catch (IllegalArgumentException ex) {
                // NumberFormatException is also an IllegalArgumentException
                JOptionPane.showMessageDialog(manageFrame, "OOP Validation Error: " + ex.getMessage(),
                        "Invalid Data", JOptionPane.ERROR_MESSAGE);
                return;
            }

            //Update the CSV using values from updatedEmployee
            UpdateEmployee.updateEmployeeInCSV(updatedEmployee, model);

            // Fully reload the model to ensure consistency
            RefreshTable.refreshEmployeeTable2(model);

            JOptionPane.showMessageDialog(manageFrame, "Employee updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            manageFrame.dispose(); // Close window
        });
        
        

        // Delete action listener, it refreshes the table as well after the deletion
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(manageFrame, "Are you sure you want to delete this employee?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                DeleteEmployee.deleteEmployee(employeeNumber, model);
                RefreshTable.refreshEmployeeTable2(model);
                JOptionPane.showMessageDialog(manageFrame, "Employee deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                manageFrame.dispose();
            }
        });
    }
    private static String cleanQuotes(String input) {
        if (input == null) return "";
        return input.replaceAll("^\"|\"$", ""); // Removes starting and ending quotes only
    }
    
}