package GUI;

/**
 *
 * @author MO-IT110 Group 8
 */

import Backend.Employee;
import Backend.PayrollDataLoader;
import javax.swing.*;
import java.awt.*;
import java.time.Month;
import java.util.Arrays;
import java.util.Locale;


public class ViewEmployee extends JFrame {
    //File paths for our CSV files (now defined once in PayrollDataLoader, which finds the
    //databases folder whether the app runs from NetBeans or from the repository root)
    public static final String EMPLOYEE_DETAILS_CSV = PayrollDataLoader.EMPLOYEE_DETAILS_CSV;
    public static final String ATTENDANCE_RECORD_CSV = PayrollDataLoader.ATTENDANCE_RECORD_CSV;
   
    //It display an employee data when a row is selected in the table
    public static void viewEmployeeDetails(JTable table) {
        int selectedRow = table.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(null, "Please select an employee first!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String employeeID = table.getValueAt(selectedRow, 0).toString(); //Get ID from selected table row

        // Use existing method to retrieve full employee record from CSV
        String[] fullDetails = GetEmployeeDetails.getEmployeeDetails(employeeID);//Be sure this method is complete
        
        if (fullDetails == null) {
            JOptionPane.showMessageDialog(null, "Could not find complete employee data.", "Data Missing", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        for (int i = 0; i < fullDetails.length; i++) {
            fullDetails[i] = forQuotation(fullDetails[i]);
        }
        
        String[] headers = {
            "Employee ID", "Last Name", "First Name", "Birthday",
            "Address", "Phone Number", "SSS Number", "PhilHealth Number", "TIN",
            "Pag-IBIG Number", "Status", "Position", "Immediate Supervisor",
            "Basic Salary", "Rice Subsidy", "Phone Allowance", "Clothing Allowance",
            "Gross Semi-monthly Rate", "Hourly Rate"
        };

        //Building the employee data for display
        //We divided it to 4 section for readability.
        StringBuilder detailsBuilder = new StringBuilder("<html><h2>Employee Details</h2><p>");
        
        //This is the section 1, showing index 0 to 5
        detailsBuilder.append("***************** Employee Basic Details *****************<br>");
        for (int i = 0; i <= 5; i++) {
            detailsBuilder.append("<b>").append(headers[i]).append(":</b> ").append(fullDetails[i]).append("<br>");
        }
        detailsBuilder.append("<br>----------------------------------------------------------<br>");

        //This is the section 2, showing index 6 to 9
        detailsBuilder.append("******************* Government IDs ***********************<br>");
        for (int i = 6; i <= 9; i++) {
            detailsBuilder.append("<b>").append(headers[i]).append(":</b> ").append(fullDetails[i]).append("<br>");
        }
        detailsBuilder.append("<br>----------------------------------------------------------<br>");

        //This is the section 3, showing index 10 to 12
        detailsBuilder.append("******************* Work Information *********************<br>");
        for (int i = 10; i <= 12; i++) {
            detailsBuilder.append("<b>").append(headers[i]).append(":</b> ").append(fullDetails[i]).append("<br>");
        }
        detailsBuilder.append("<br>----------------------------------------------------------<br>");

        //This is the last section, showing the index 13 until the end of the line
        detailsBuilder.append("******************* Salary Breakdown *********************<br>");
        for (int i = 13; i < headers.length; i++) {
            detailsBuilder.append("<b>").append(headers[i]).append(":</b> ").append(fullDetails[i]).append("<br>");
        }

        detailsBuilder.append("</p></html>");

        JLabel label = new JLabel(detailsBuilder.toString());
        label.setHorizontalAlignment(SwingConstants.CENTER);
        
        JScrollPane scrollPane = new JScrollPane(label);
        scrollPane.setPreferredSize(new Dimension(600, 500));
        
        //We make a drop down menu wherein the user can choose a month then compute the salary of the selected employee
        // Month names
        String[] months = months = Arrays.stream(Month.values())
            .map(m -> m.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH))
            .toArray(String[]::new);
        JComboBox<String> monthSelector = new JComboBox<>(months);

        // Year options come from the years that actually have attendance data.
        // (Previously current year ± 1, so 2024 data could never be selected and salary always showed 0.)
        String[] years = PayrollDataLoader.getAttendanceYears();
        if (years.length == 0) {
            years = new String[]{String.valueOf(java.time.Year.now().getValue())};
        }
        JComboBox<String> yearSelector = new JComboBox<>(years);
        yearSelector.setSelectedItem(years[years.length - 1]); // Default to the latest year with data
        JButton computeSalaryButton = ButtonsStyle.ButtonStyle2("Compute Salary");
        
         //It adds the components to the panel
        JPanel selectionPanel = new JPanel();
        selectionPanel.add(new JLabel("Select Month:"));
        selectionPanel.add(monthSelector);
        selectionPanel.add(new JLabel("Year:"));
        selectionPanel.add(yearSelector);
        selectionPanel.add(computeSalaryButton);


        JFrame detailsFrame = new JFrame("Employee Information");
        detailsFrame.setSize(700, 650);
        detailsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        detailsFrame.setLayout(new BorderLayout());
        detailsFrame.add(scrollPane, BorderLayout.CENTER); // Employee details in center
        
        //This is the action listener for Salary computation and we call the methods to get the total hours worked, hourly rate and salary.
        computeSalaryButton.addActionListener(e -> {
            String selectedMonth = (String) monthSelector.getSelectedItem();
            String selectedYear = (String) yearSelector.getSelectedItem();
            // Hours and salary now come from the backend (AttendanceRecord + Employee)
            double totalHoursWorked = PayrollDataLoader.getTotalHoursWorked(employeeID,
                    Month.valueOf(selectedMonth.toUpperCase()), Integer.parseInt(selectedYear));
            Employee employee = PayrollDataLoader.loadEmployee(employeeID);
            double salary = (employee != null) ? employee.calculateSalary(totalHoursWorked) : 0;
            String employeeDetails = detailsBuilder.toString();
            
           displayComputedSalary(detailsFrame, employeeDetails, employeeID, selectedMonth, selectedYear, totalHoursWorked, salary);
    });
       
        detailsFrame.add(selectionPanel, BorderLayout.SOUTH); // Dropdown & button at bottom
        detailsFrame.setLocationRelativeTo(null);
        detailsFrame.setVisible(true);

    }
    
    //It will now display the computed salary
    public static void displayComputedSalary(JFrame frame, String employeeDetails, String employeeID, String month, String year, double totalHoursWorked, double salary) {

    //Payslip now comes from the backend (RegularPayrollProcessor -> Payslip)
    String payslipInfo = (totalHoursWorked > 0)
            ? MainMenu.showPayslip(employeeID, month, year)
            : "No attendance records for the selected month.";
    
    //Since we use the method from show payslip from CP1
    //And since we can add a new employee without attendance record
    //We make an if statement if the records is null or empty.
    if (payslipInfo == null || payslipInfo.isEmpty()) {
        payslipInfo = "<html><b>Error:</b> Payslip could not be generated.</html>"; // Fallback in case of null
    }
    
    //This shows the employee full details
    JLabel detailsLabel = new JLabel("<html><pre>" + employeeDetails.replace("\n", "<br>") + "</pre></html>");
    detailsLabel.setHorizontalAlignment(SwingConstants.CENTER);

    //This is the payslip details, it will show below the employee details
    JLabel payslipLabel = new JLabel("<html><pre>" + payslipInfo.replace("\n", "<br>") + "</pre></html>");
    payslipLabel.setHorizontalAlignment(SwingConstants.CENTER);

    //We want to show the employee details and the payslip in the same panel
    JPanel panel = new JPanel();
    panel.setLayout(new GridLayout(2, 1)); // Divides the panel into sections
    panel.add(detailsLabel);  // Employee details remain visible
    panel.add(payslipLabel);  // Payslip is displayed below details

    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setPreferredSize(new Dimension(600, 400));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    
    //Refresh Frame to Show Updated Payslip
    frame.getContentPane().removeAll();
    frame.add(scrollPane);
    frame.revalidate();
    frame.repaint();
}

    
    //We make this to read the data in the csv file correctly
    public static String[] parseCSVLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); //Preserves fields correctly
    }  
    public static String forQuotation(String input) {
        return input.replaceAll("^\"|\"$", ""); //Removes surrounding quotes
    }
}