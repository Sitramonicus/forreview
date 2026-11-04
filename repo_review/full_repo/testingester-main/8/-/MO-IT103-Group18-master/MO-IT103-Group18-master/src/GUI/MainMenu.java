/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package GUI;

import Backend.AttendanceRecord;
import Backend.Employee;
import Backend.PayrollDataLoader;
import Backend.FinalPayPayrollProcessor;
import Backend.LeaveRecord;
import Backend.PayrollProcessor;
import Backend.Payslip;
import Backend.RegularPayrollProcessor;
import Backend.ThirteenthMonthPayrollProcessor;
import com.github.lgooddatepicker.components.DatePicker;
import static GUI.ViewEmployee.ATTENDANCE_RECORD_CSV;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;


public class MainMenu extends JFrame {
    private static String selectedEmployeeID = ""; // Stores selected Employee ID
    private static final String EMPLOYEE_CSV = PayrollDataLoader.EMPLOYEE_DETAILS_CSV; // Path is now defined in one place (PayrollDataLoader)

    //Here it will just launch the login screen.
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> LogIn.showLoginScreen());
    }

    public MainMenu(String username) {
        setTitle("MotorPH Employee App");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(8, 1, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(50, 200, 50, 200));

        //We added a selection drop down for the selected employee ID (modification)
        JComboBox<String> employeeSelector = new JComboBox<>(getEmployeeIDs());
        employeeSelector.addActionListener(e -> selectedEmployeeID = (String) employeeSelector.getSelectedItem());
        
        //Buttons for the menu option, we added the view employee table button and removed admin access.
        JButton profileButton = new JButton("View Profile");
        JButton attendanceButton = new JButton("View Attendance");
        JButton payrollButton = new JButton("View Payroll");
        JButton disputeButton = new JButton("My Payslip Disputes");
        JButton viewEmployeeTableButton = new JButton("View Employee Table");
        JButton logoutButton = new JButton("Log Out");
        
        //Action listener for each button
        viewEmployeeTableButton.addActionListener(e -> EmployeeTable.displayEmployeeTable());
        profileButton.addActionListener(e -> showProfile());
        attendanceButton.addActionListener(e -> showAttendance());
        payrollButton.addActionListener(e -> showPayrollMenu());
        disputeButton.addActionListener(e -> showEmployeeDisputes());
        logoutButton.addActionListener(e -> logOut());

        panel.add(new JLabel("Select Employee:")); //We added a label for Employee Selector
        
        //Adding the buttons to the UI
        panel.add(employeeSelector); //And add a dropdown to UI
        panel.add(profileButton);
        panel.add(attendanceButton);
        panel.add(payrollButton);
        panel.add(disputeButton);
        panel.add(viewEmployeeTableButton);
        panel.add(logoutButton);

        add(panel);
    }

    // Opens the logged-in/selected employee's payslip dispute screen.
    private void showEmployeeDisputes() {
        if (selectedEmployeeID == null || selectedEmployeeID.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please select an employee first!",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new EmployeePayslipDisputeFrame(selectedEmployeeID).setVisible(true);
    }

    //This method is used to get the Id of the Employee for the drop down menu
    private String[] getEmployeeIDs() {
        return PayrollDataLoader.loadEmployeeIds().toArray(String[]::new);
    }

    public static String[] parseCSVLine(String line) {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1); //Smart comma split
    }
    
    //Retrieves the profile of the selected employee
    private static String getUserProfile(String employeeID) {
        String line;
        
        //It will now try to read the csv file and find the user's info.
        try (BufferedReader br = new BufferedReader(new FileReader(EMPLOYEE_CSV))) {
            while ((line = br.readLine()) != null) {
                String[] details = parseCSVLine(line);
                
                //now it checks if the data exists in the csv file and show it.
                if (details.length >= 5 && details[0].equals(employeeID)) {
                    return "Name: " + details[1] + ", " + details[2] + // Changed to Last Name, First Name
                           "\nEmployee Number: " + details[0] +
                           "\nBirthday: " + details[3] +
                           "\nAddress: " + details[4] +
                           "\nPhone Number: " + details[5] +
                           "\nStatus: " + details[10];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Profile not found.";
    }
    
    // This will read attendance CSV file and get the records of the current user.
    private static String getDailyAttendance(String employeeID) {
        String csvFile = ATTENDANCE_RECORD_CSV;
        String line;
        StringBuilder attendanceDetails = new StringBuilder();
        
        //It will try to read the csv file and show the records.
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                String[] record = parseCSVLine(line);
                if (record.length >= 4 && record[0].equals(employeeID)) {
                    attendanceDetails.append("First name: ").append(record[2])
                        .append("   |   Employee #: ").append(record[0])
                        .append("   |   Date: ").append(record[3])
                        .append("   |   LogIn: ").append(record[4])
                        .append("   |   LogOut: ").append(record[5]).append("\n");
                }
            }
        } catch (IOException e) {
            
        }
        return attendanceDetails.length() > 0 ? attendanceDetails.toString() : "No attendance records found.";
    }

    
    //It calculates the weekly hours worked of the user from the Attendance csv file.
    // OOP: hours now come from AttendanceRecord.calculateHoursWorked(); the GUI only groups them by week.
    private static String getWeeklyHoursWorked(String employeeID) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
        Map<Integer, Double> weeklyHoursMap = new LinkedHashMap<>();

        for (AttendanceRecord record : PayrollDataLoader.loadAttendance(employeeID)) {
            LocalDate date = LocalDate.parse(record.getDate(), dateFormatter);
            int weekNumber = (date.getDayOfYear() - 1) / 7 + 1; // This determine which week number it is in a year.
            weeklyHoursMap.merge(weekNumber, record.calculateHoursWorked(), Double::sum);
        }
        
        //This make the summary of weekly hours.
        StringBuilder weeklyHoursInfo = new StringBuilder("Weekly Hours Worked:\n");
        for (Map.Entry<Integer, Double> entry : weeklyHoursMap.entrySet()) {
            weeklyHoursInfo.append("Week ").append(entry.getKey()).append(": ").append(String.format("%.2f", entry.getValue())).append(" hours\n");
        }
        
        return weeklyHoursInfo.toString();
    }


    //Here, it retrieves attendance history of the selected employee
    private static List<String> getAttendanceHistory(String employeeID) {
        List<String> records = new ArrayList<>();
        for (AttendanceRecord record : PayrollDataLoader.loadAttendance(employeeID)) {
            records.add("Date: " + record.getDate() + " | Hours Worked: "
                    + String.format("%.2f", record.calculateHoursWorked()));
        }
        return records;
    }

    // Displays the profile of the selected employee
    private void showProfile() {
        if (selectedEmployeeID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an employee!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String profileInfo = getUserProfile(selectedEmployeeID);
        JOptionPane.showMessageDialog(this, profileInfo, "Employee Profile", JOptionPane.INFORMATION_MESSAGE);
    }
    
    //This will display the options of attendance menu
    public void showAttendance() {
        String[] options = {"View Daily Attendance Record", "Calculate Hours Worked Per Week","View Attendance History", "View Leave Records"};
        int choice = JOptionPane.showOptionDialog(
            this, 
            "Select an option:", 
            "Attendance Menu", 
            JOptionPane.DEFAULT_OPTION, 
            JOptionPane.INFORMATION_MESSAGE, 
            null, 
            options, 
            options[0]
        );

        switch (choice) {
            case 0 -> showDailyAttendance();
            case 1 -> showWeeklyHoursWorked();
            case 2 -> showAttendanceHistory();
            case 3 -> showLeaveRecords();
        }
    }

    // Sample leave records for the selected employee. MotorPH has no leave data file,
    // so these show how LeaveRecord works: getDuration() counts calendar days across
    // months and returns 0 unless the leave is Approved.
    static List<LeaveRecord> sampleLeaveRecords(String employeeID) {
        List<LeaveRecord> leaves = new ArrayList<>();
        leaves.add(new LeaveRecord(employeeID, "Vacation", "06-10-2024", "06-12-2024", "Approved", "Paid"));
        leaves.add(new LeaveRecord(employeeID, "Sick", "07-30-2024", "08-02-2024", "Approved", "Paid"));
        leaves.add(new LeaveRecord(employeeID, "Emergency", "09-05-2024", "09-06-2024", "Pending", "Unpaid"));
        return leaves;
    }

    // Builds the leave summary text; all day counts come from LeaveRecord.getDuration()
    static String formatLeaveRecords(String employeeID, List<LeaveRecord> leaves) {
        final int leaveCreditsPerYear = 15; // same MotorPH policy used by FinalPayPayrollProcessor
        int paidDaysUsed = 0;
        StringBuilder info = new StringBuilder();
        info.append("Leave Records for Employee ").append(employeeID).append("\n");
        info.append("(Sample records: MotorPH has no leave data file yet)\n\n");
        for (LeaveRecord leave : leaves) {
            info.append(leave.getLeaveType())
                .append(" | ").append(leave.getStartDate()).append(" to ").append(leave.getEndDate())
                .append(" | ").append(leave.getApprovalStatus())
                .append(" | ").append(leave.getPaidStatus())
                .append(" | ").append(leave.getDuration()).append(" day(s)\n");
            if ("Paid".equalsIgnoreCase(leave.getPaidStatus())) {
                paidDaysUsed += leave.getDuration(); // 0 unless Approved
            }
        }
        info.append("\nApproved paid leave used: ").append(paidDaysUsed).append(" day(s)");
        info.append("\nRemaining leave credits: ").append(Math.max(leaveCreditsPerYear - paidDaysUsed, 0))
            .append(" of ").append(leaveCreditsPerYear);
        return info.toString();
    }

    // Displays the leave records in a scrollable dialog, like the other attendance screens
    private void showLeaveRecords() {
        if (selectedEmployeeID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an employee!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JTextArea textArea = new JTextArea(formatLeaveRecords(selectedEmployeeID, sampleLeaveRecords(selectedEmployeeID)));
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 250));

        JOptionPane.showMessageDialog(this, scrollPane, "Leave Records", JOptionPane.INFORMATION_MESSAGE);
    }
        //This will display the daily attendance of the user. We also make it scrollable. 
    private void showDailyAttendance() {
        String attendanceInfo = getDailyAttendance(selectedEmployeeID);

        JTextArea textArea = new JTextArea(attendanceInfo);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Daily Attendance", JOptionPane.INFORMATION_MESSAGE);
    }
    
    //Here, it will display the total weekly hours worked by the user.
    private void showWeeklyHoursWorked() {
        String weeklyHoursInfo = getWeeklyHoursWorked(selectedEmployeeID);

        JTextArea textArea = new JTextArea(weeklyHoursInfo);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        //We also make it scrollable like what we did in viewing the attendance.
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(200, 300));

        //It displays the weekly hours worked in a message dialog
        JOptionPane.showMessageDialog(this, scrollPane, "Weekly Hours Worked", JOptionPane.INFORMATION_MESSAGE);
    }

    // Displays attendance history for the selected employee
    public void showAttendanceHistory() {
        if (selectedEmployeeID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an employee!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> attendanceHistory = getAttendanceHistory(selectedEmployeeID);

        StringBuilder historyInfo = new StringBuilder("Attendance History:\n");
        for (String record : attendanceHistory) {
            historyInfo.append(record).append("\n");
        }

        JTextArea textArea = new JTextArea(historyInfo.toString());
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(300, 300));

        JOptionPane.showMessageDialog(this, scrollPane, "Full Attendance History", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // Compute total monthly hours worked for selected employee
    // Total monthly hours now come from the backend (AttendanceRecord via PayrollDataLoader)
    private double computeTotalMonthlyHours(String employeeID, String selectedMonth, String selectedYear) {
        Month month = Month.valueOf(selectedMonth.toUpperCase());
        int year = Integer.parseInt(selectedYear);
        return PayrollDataLoader.getTotalHoursWorked(employeeID, month, year);
    }

    // Payroll types the user can choose. Each one creates a different PayrollProcessor subclass.
    private static final String REGULAR = "Regular";
    private static final String THIRTEENTH_MONTH = "13th Month";
    private static final String FINAL_PAY = "Final Pay";

    // Creates the right PayrollProcessor subclass for the chosen payroll type.
    // The variable type is the parent class, so the GUI never needs to know which
    // subclass it is: processPayroll() runs the subclass's own computePay() (polymorphism).
    private static PayrollProcessor createProcessor(String payrollType, Employee employee,
                                                    Month month, int year, LocalDate separationDate) {
        switch (payrollType) {
            case THIRTEENTH_MONTH:
                return new ThirteenthMonthPayrollProcessor(employee);
            case FINAL_PAY:
                return new FinalPayPayrollProcessor(employee, separationDate);
            default:
                return new RegularPayrollProcessor(employee, month, year);
        }
    }

    private static String payPeriodLabel(String payrollType, Month month, int year, LocalDate separationDate) {
        switch (payrollType) {
            case THIRTEENTH_MONTH:
                return "13th Month Pay " + year;
            case FINAL_PAY:
                return "Final Pay (separated " + separationDate.format(DateTimeFormatter.ofPattern("MM-dd-yyyy")) + ")";
            default:
                return month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH) + " " + year;
        }
    }

    private static NumberFormat currencyFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format;
    }

    // View Salary: gross pay only (plus the breakdown for Final Pay)
    private void showSalary(Payslip payslip, PayrollProcessor processor, Employee employee) {
        NumberFormat money = currencyFormat();
        String breakdown = processor.getGrossPayBreakdown();
        String info = "Employee Name: " + employee.getName()
                + "\nEmployee ID: " + employee.getEmployeeId()
                + "\nPay Period: " + payslip.getPayPeriod()
                + (breakdown.isEmpty() ? "" : "\n\n" + breakdown + "\n")
                + "\nGross Pay: ₱" + money.format(payslip.getGrossPay());
        JOptionPane.showMessageDialog(this, info, "Salary Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // Gross salary from Employee.calculateSalary() (kept for screens that only need gross pay)
    public static String computeSalary(String employeeID, double totalHours) {
        Employee employee = PayrollDataLoader.loadEmployee(employeeID);
        if (employee == null) {
            return "Employee Name: \nEmployee ID: " + employeeID + "\nGross Salary: 0.00";
        }
        return "Employee Name: " + employee.getName()
                + "\nEmployee ID: " + employee.getEmployeeId()
                + "\nGross Salary: ₱" + currencyFormat().format(employee.calculateSalary(totalHours));
    }

    // View Deductions: every value comes from the read-only Payslip returned by the backend
    private void showDeductions(Payslip payslip) {
        NumberFormat money = currencyFormat();
        double totalDeductions = payslip.getSssDeduction() + payslip.getPhilHealthDeduction()
                + payslip.getPagIbigDeduction() + payslip.getWithholdingTax();

        String deductionsInfo = "Pay Period: " + payslip.getPayPeriod()
                + "\nGross Pay: ₱" + money.format(payslip.getGrossPay())
                + "\nSSS Deduction: ₱" + money.format(payslip.getSssDeduction())
                + "\nPhilHealth Deduction: ₱" + money.format(payslip.getPhilHealthDeduction())
                + "\nPag-IBIG Deduction: ₱" + money.format(payslip.getPagIbigDeduction())
                + "\nWithholding Tax: ₱" + money.format(payslip.getWithholdingTax())
                + "\nTotal Deductions: ₱" + money.format(totalDeductions)
                + "\nNet Pay: ₱" + money.format(payslip.getNetPay());

        JOptionPane.showMessageDialog(this, deductionsInfo, "Salary Deductions Breakdown", JOptionPane.INFORMATION_MESSAGE);
    }

    // Builds the payslip text from the read-only Payslip (no computation in the GUI)
    public static String formatPayslip(Payslip payslip, PayrollProcessor processor) {
        NumberFormat money = currencyFormat();
        double totalDeductions = payslip.getSssDeduction() + payslip.getPhilHealthDeduction()
                + payslip.getPagIbigDeduction() + payslip.getWithholdingTax();
        double totalAllowances = payslip.getRiceSubsidy() + payslip.getPhoneAllowance() + payslip.getClothingAllowance();
        String breakdown = processor.getGrossPayBreakdown();

        return "**PAYSLIP**"
                + "\nEmployee: " + payslip.getEmployeeId()
                + "\nPay Period: " + payslip.getPayPeriod()
                + (breakdown.isEmpty() ? "" : "\n\n" + breakdown)
                + "\n\nGross Pay: ₱" + money.format(payslip.getGrossPay())
                + "\n\n**Deductions**"
                + "\nSSS Deduction: ₱" + money.format(payslip.getSssDeduction())
                + "\nPhilHealth Deduction: ₱" + money.format(payslip.getPhilHealthDeduction())
                + "\nPag-IBIG Deduction: ₱" + money.format(payslip.getPagIbigDeduction())
                + "\nWithholding Tax: ₱" + money.format(payslip.getWithholdingTax())
                + "\nTotal Deductions: ₱" + money.format(totalDeductions)
                + "\n\n**Net Pay**: ₱" + money.format(payslip.getNetPay())
                + "\n\n**Allowances (non-taxable)**"
                + "\nRice Subsidy: ₱" + money.format(payslip.getRiceSubsidy())
                + "\nPhone Allowance: ₱" + money.format(payslip.getPhoneAllowance())
                + "\nClothing Allowance: ₱" + money.format(payslip.getClothingAllowance())
                + "\nTotal Allowances: ₱" + money.format(totalAllowances)
                + "\n\n**Take-Home Pay**: ₱" + money.format(payslip.getTakeHomePay());
    }

    // Regular payslip for one month (used by the View Employee screen)
    public static String showPayslip(String employeeID, String selectedMonth, String selectedYear) {
        Employee employee = PayrollDataLoader.loadEmployee(employeeID);
        if (employee == null) {
            return "Employee " + employeeID + " not found.";
        }
        Month month = Month.valueOf(selectedMonth.toUpperCase());
        int year = Integer.parseInt(selectedYear);
        PayrollProcessor processor = new RegularPayrollProcessor(employee, month, year);
        Payslip payslip = processor.processPayroll(employeeID, payPeriodLabel(REGULAR, month, year, null));
        return formatPayslip(payslip, processor);
    }

    // Show payroll menu for the selected employee
    private void showPayrollMenu() {
        if (selectedEmployeeID.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an employee!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] options = {"View Salary", "View Deductions", "View Payslip"};

        int choice = JOptionPane.showOptionDialog(
            this,
            "Select an option:",
            "Payroll Menu",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );

        //We want to close the selection if user close the dialog box
        if (choice == JOptionPane.CLOSED_OPTION) {
            return;
        }

        // Payroll type decides which PayrollProcessor subclass is created
        JComboBox<String> typeSelector = new JComboBox<>(new String[]{REGULAR, THIRTEENTH_MONTH, FINAL_PAY});

        JComboBox<String> monthSelector = new JComboBox<>(new String[]{
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        });

        // Only years that actually have attendance data
        String[] years = PayrollDataLoader.getAttendanceYears();
        if (years.length == 0) {
            years = new String[]{String.valueOf(java.time.Year.now().getValue())};
        }
        JComboBox<String> yearSelector = new JComboBox<>(years);
        yearSelector.setSelectedItem(years[years.length - 1]);

        // Separation date is only used for Final Pay
        DatePicker separationPicker = new DatePicker();
        separationPicker.setDate(LocalDate.of(Integer.parseInt(years[years.length - 1]), 6, 30));
        separationPicker.setEnabled(false);

        // Enable the fields that matter for the chosen type
        typeSelector.addActionListener(e -> {
            String type = (String) typeSelector.getSelectedItem();
            monthSelector.setEnabled(REGULAR.equals(type));
            yearSelector.setEnabled(!FINAL_PAY.equals(type));
            separationPicker.setEnabled(FINAL_PAY.equals(type));
        });

        JPanel selectionPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        selectionPanel.add(new JLabel("Payroll Type:"));
        selectionPanel.add(typeSelector);
        selectionPanel.add(new JLabel("Month (Regular):"));
        selectionPanel.add(monthSelector);
        selectionPanel.add(new JLabel("Year (Regular / 13th Month):"));
        selectionPanel.add(yearSelector);
        selectionPanel.add(new JLabel("Separation Date (Final Pay):"));
        selectionPanel.add(separationPicker);

        int selection = JOptionPane.showConfirmDialog(
            null,
            selectionPanel,
            "Select Payroll Coverage",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (selection != JOptionPane.OK_OPTION) {
            return;
        }

        String payrollType = (String) typeSelector.getSelectedItem();
        Month month = Month.valueOf(((String) monthSelector.getSelectedItem()).toUpperCase());
        int year = Integer.parseInt((String) yearSelector.getSelectedItem());
        LocalDate separationDate = separationPicker.getDate();

        if (FINAL_PAY.equals(payrollType) && separationDate == null) {
            JOptionPane.showMessageDialog(this, "Please pick a separation date for Final Pay.", "Missing Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee employee = PayrollDataLoader.loadEmployee(selectedEmployeeID);
        if (employee == null) {
            JOptionPane.showMessageDialog(this, "Employee not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (REGULAR.equals(payrollType) && PayrollDataLoader.getTotalHoursWorked(selectedEmployeeID, month, year) == 0) {
            JOptionPane.showMessageDialog(this, "No attendance records for " + selectedEmployeeID + " in the selected month.",
                    "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Parent-typed variable: the correct subclass's computePay() runs automatically
        PayrollProcessor processor = createProcessor(payrollType, employee, month, year, separationDate);
        Payslip payslip = processor.processPayroll(selectedEmployeeID,
                payPeriodLabel(payrollType, month, year, separationDate));

        switch (choice) {
            case 0 -> showSalary(payslip, processor, employee);
            case 1 -> showDeductions(payslip);
            case 2 -> JOptionPane.showMessageDialog(this, formatPayslip(payslip, processor),
                        "Generated Payslip", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Logs the user out and returns to the login screen
    private void logOut() {
        dispose(); // Close the main window
        SwingUtilities.invokeLater(() -> LogIn.showLoginScreen()); // Return to login screen
    }
    
}