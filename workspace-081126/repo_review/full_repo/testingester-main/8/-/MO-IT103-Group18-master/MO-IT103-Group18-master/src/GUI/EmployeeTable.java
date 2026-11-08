/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

/**
 *
 * @author MO-IT110 Group 8
 */

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class EmployeeTable {
    //This will display the employee table in a new window
    public static void displayEmployeeTable() {
        JFrame employeeFrame = new JFrame("Employee Records");
        employeeFrame.setSize(900, 500);
        employeeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        employeeFrame.setLocationRelativeTo(null);
        
        //This is the navigation buttons and their layout and style
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new GridLayout(1, 4, 10, 10));
        JButton viewEmployeeButton = ButtonsStyle.ButtonStyle2("View Employee");
        JButton newEmployeeButton = ButtonsStyle.ButtonStyle2("New Employee");
        JButton manageEmployeeButton = ButtonsStyle.ButtonStyle2("Manage Employees");
        
        //It will add the navigation buttons to the window
        menuPanel.add(viewEmployeeButton);
        menuPanel.add(newEmployeeButton);
        menuPanel.add(manageEmployeeButton);
        
        //This will define the table model with headers
        String[] columnNames = {"Employee Number", "Last Name", "First Name", "SSS Number", "PhilHealth Number", "TIN", "Pag-IBIG Number"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0){
            @Override
            public boolean isCellEditable(int row, int column){
            return false;}
        };
        JTable employeeTable = new JTable(model);
        
        //It will now try to read the records from the csv file and show the table
        try (BufferedReader br = new BufferedReader(new FileReader(ViewEmployee.EMPLOYEE_DETAILS_CSV))) {
            br.readLine(); //skipping the header row (br)
            String line;
            while ((line = br.readLine()) != null) {
                String[] record = ViewEmployee.parseCSVLine(line);
                if (record.length >= 19) {  //this ensure that the index is >=19, a right format
                    model.addRow(new Object[]{record[0], record[1], record[2], record[6], record[7], record[8], record[9]});
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(employeeFrame, "Error loading employee records: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        //We use a scrollbar in the table, it also set the layout
        JScrollPane scrollPane = new JScrollPane(employeeTable);
        employeeFrame.setLayout(new BorderLayout());
        employeeFrame.add(menuPanel, BorderLayout.NORTH);
        employeeFrame.add(scrollPane, BorderLayout.CENTER);
        employeeFrame.setVisible(true);
        
        //Here we define the actions listeners for our buttons
        viewEmployeeButton.addActionListener(e -> ViewEmployee.viewEmployeeDetails(employeeTable));
        
        manageEmployeeButton.addActionListener(e -> ManageEmployeePanelForUpdateAndDelete.displayManageEmployeesPanel(employeeTable, model));
        
        newEmployeeButton.addActionListener(e -> NewEmployee.displayNewEmployeeForm(employeeFrame, model));
        
    }

}