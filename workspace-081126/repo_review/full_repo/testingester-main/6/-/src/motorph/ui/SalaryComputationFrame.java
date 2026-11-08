package motorph.ui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import motorph.deduction.PagIbigDeduction;
import motorph.deduction.PhilHealthDeduction;
import motorph.deduction.SSSDeduction;
import motorph.deduction.WithholdingTaxDeduction;
import motorph.model.AttendanceRecord;
import motorph.model.Employee;
import motorph.model.Payslip;
import motorph.service.CSVDataHandler;
import motorph.service.PayrollProcessor;
import motorph.user.User;

/**
 * Payroll screen for one employee or all employees.
 * <p>
 * The frame creates the backend objects, passes the user's selections to
 * PayrollProcessor, and displays the Payslip objects it returns. It performs
 * no payroll computation and no file access of its own.
 * </p>
 */
public class SalaryComputationFrame extends JFrame {

    private static final Logger logger = Logger.getLogger(SalaryComputationFrame.class.getName());
    private static final long serialVersionUID = 1L;

    private static final int FRAME_WIDTH = 1200; // widened to fit extra columns

    private final transient User currentUser;
    private final transient CSVDataHandler dataHandler = new CSVDataHandler();
    private final transient PayrollProcessor payrollProcessor = new PayrollProcessor(
            List.of(new SSSDeduction(), new PhilHealthDeduction(), new PagIbigDeduction()),
            new WithholdingTaxDeduction());

    private transient List<Employee> employees = new ArrayList<>();
    private transient Map<String, List<AttendanceRecord>> attendance = new HashMap<>();
    private transient List<Payslip> payslips = new ArrayList<>();

    private final boolean singleEmployeeMode;
    private javax.swing.JTextField employeeIdField; // only shown in single-employee mode

    // UI components
    private javax.swing.JComboBox<String> monthComboBox;
    private JButton computeButton;
    private JButton summaryButton;
    private JButton saveButton;
    private JButton backButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;

    /**
     * Creates the frame in either single-employee or all-employees mode.
     *
     * @param currentUser        the logged-in user; actions are checked with hasPermission
     * @param singleEmployeeMode true to show an employee-number field and compute
     *                           payroll for just that one employee; false for the
     *                           all-employees batch behavior
     */
    @SuppressWarnings("this-escape")
    public SalaryComputationFrame(User currentUser, boolean singleEmployeeMode) {
        this.currentUser = currentUser;
        this.singleEmployeeMode = singleEmployeeMode;
        initComponents();
        setResizable(false);
        setSize(FRAME_WIDTH, 600);
        setupLayout();
        applyFrameStyling();
        setLocationRelativeTo(null);
        loadData();
    }

    // === UI SETUP ===
    private void initComponents() {
        setTitle("Salary Computation");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        monthComboBox  = new javax.swing.JComboBox<>();
        employeeIdField = new javax.swing.JTextField();

        computeButton = new JButton("Compute Salaries");
        computeButton.setMnemonic('M');
        computeButton.addActionListener(this::computeButtonActionPerformed);

        summaryButton = new JButton("Generate Summary");
        summaryButton.setMnemonic('G');
        summaryButton.addActionListener(this::summaryButtonActionPerformed);

        saveButton = new JButton("Save Results");
        saveButton.setMnemonic('R');
        saveButton.addActionListener(this::saveButtonActionPerformed);

        backButton = new JButton("Back");
        backButton.setMnemonic('B');
        backButton.addActionListener(this::backButtonActionPerformed);

        // Deduction columns come from the processor's Deduction list, so a new
        // deduction type adds its own column without changing this frame.
        List<String> cols = new ArrayList<>(List.of(
            "Employee ID", "Name", "Type", "Hourly Rate", "Hours Worked", "Gross Pay"));
        cols.addAll(payrollProcessor.getDeductionNames());
        cols.addAll(List.of("Total Deductions", "Net Pay", "Allowances"));

        tableModel = new DefaultTableModel(cols.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setRowHeight(24);
    }

    /**
     * Lays out components and sets bounds. Called after initComponents().
     */
    private void setupLayout() {
        getContentPane().setLayout(null);

        // Header panel
        javax.swing.JPanel headerPanel = new javax.swing.JPanel();
        headerPanel.setLayout(null);
        headerPanel.setBackground(new java.awt.Color(28, 57, 110));
        headerPanel.setBounds(0, 0, FRAME_WIDTH, 70);
        javax.swing.JLabel headerTitle = new javax.swing.JLabel("Salary Computation");
        headerTitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 22));
        headerTitle.setForeground(new java.awt.Color(255, 255, 255));
        headerTitle.setBounds(16, 12, 400, 30);
        headerPanel.add(headerTitle);
        javax.swing.JLabel headerSubtitle = new javax.swing.JLabel("MotorPH Payroll System");
        headerSubtitle.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
        headerSubtitle.setForeground(new java.awt.Color(210, 225, 245));
        headerSubtitle.setBounds(16, 40, 300, 16);
        headerPanel.add(headerSubtitle);
        getContentPane().add(headerPanel);

        // Separator
        javax.swing.JLabel separator = new javax.swing.JLabel();
        separator.setOpaque(true);
        separator.setBackground(new java.awt.Color(44, 90, 160));
        separator.setBounds(0, 70, FRAME_WIDTH, 2);
        getContentPane().add(separator);

        // Month / Employee controls
        java.awt.Color fg = new java.awt.Color(30, 30, 50);
        java.awt.Font labelFont = new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 13);

        JLabel monthLabel = new JLabel("Select Cutoff Period:");
        monthLabel.setBounds(20, 80, 140, 25);
        monthLabel.setFont(labelFont);
        monthLabel.setForeground(fg);
        getContentPane().add(monthLabel);

        if (singleEmployeeMode) {
            JLabel empIdLabel = new JLabel("Employee No:");
            empIdLabel.setBounds(20, 80, 100, 25);
            empIdLabel.setFont(labelFont);
            empIdLabel.setForeground(fg);
            getContentPane().remove(monthLabel);
            getContentPane().add(empIdLabel);

            employeeIdField.setBounds(120, 80, 130, 25);
            employeeIdField.setToolTipText("Enter employee number");
            employeeIdField.setVisible(true);
            getContentPane().add(employeeIdField);

            monthComboBox.setBounds(260, 80, 150, 25);
            getContentPane().add(monthComboBox);
        } else {
            employeeIdField.setVisible(false);
            monthComboBox.setBounds(170, 80, 150, 25);
            getContentPane().add(monthComboBox);
        }

        // Toolbar buttons — shifted right to match wider frame
        computeButton.setBounds(440, 80, 160, 28);
        summaryButton.setBounds(610, 80, 150, 28);
        saveButton.setBounds(770, 80, 130, 28);
        backButton.setBounds(910, 80, 100, 28);

        getContentPane().add(computeButton);
        getContentPane().add(summaryButton);
        getContentPane().add(saveButton);
        getContentPane().add(backButton);

        // Results table — uses full width of wider frame
        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBounds(20, 115, FRAME_WIDTH - 40, 440);
        getContentPane().add(scroll);
    }

    /**
     * Applies all visual styling to the frame components after initComponents().
     */
    private void applyFrameStyling() {
        getContentPane().setBackground(new java.awt.Color(245, 245, 248));

        if (monthComboBox != null) {
            monthComboBox.setBackground(new java.awt.Color(255, 255, 255));
            monthComboBox.setForeground(new java.awt.Color(30, 30, 50));
            monthComboBox.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 13));
        }

        applyFieldStyle(employeeIdField);
        applyButtonStyle(computeButton);
        applyButtonStyle(summaryButton);
        applyButtonStyle(saveButton);
        applySecondaryButtonStyle(backButton);

        resultsTable.setRowHeight(28);
        resultsTable.setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.PLAIN, 12));
        resultsTable.setGridColor(new java.awt.Color(220, 220, 230));
        resultsTable.setSelectionBackground(new java.awt.Color(44, 90, 160));
        resultsTable.setSelectionForeground(new java.awt.Color(255, 255, 255));
        resultsTable.setShowHorizontalLines(true);
        resultsTable.setShowVerticalLines(false);
        resultsTable.getTableHeader().setFont(new java.awt.Font("Helvetica Neue", java.awt.Font.BOLD, 12));
        resultsTable.getTableHeader().setBackground(new java.awt.Color(28, 57, 110));
        resultsTable.getTableHeader().setForeground(new java.awt.Color(255, 255, 255));
        resultsTable.getTableHeader().setReorderingAllowed(false);
        resultsTable.getTableHeader().setPreferredSize(new java.awt.Dimension(0, 30));
    }

    // === DATA LOADING ===

    /**
     * Loads employees and attendance once through CSVDataHandler and fills the
     * cutoff list from the attendance dates.
     */
    private void loadData() {
        try {
            employees = dataHandler.loadEmployees();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load employees", e);
            JOptionPane.showMessageDialog(this,
                    "Unable to load employee data. Please check that the file exists at: " + CSVDataHandler.DEFAULT_EMPLOYEE_CSV,
                    "File Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        try {
            attendance = dataHandler.loadAttendance();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load attendance", e);
            JOptionPane.showMessageDialog(this,
                    "Unable to load attendance data. Please check that the file exists at: " + CSVDataHandler.DEFAULT_ATTENDANCE_CSV,
                    "File Load Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        monthComboBox.removeAllItems();
        for (String cutoff : payrollProcessor.getAvailableCutoffs(attendance)) {
            monthComboBox.addItem(cutoff);
        }
    }

    /**
     * Validates that a month has been selected in the month combo box.
     *
     * @return the selected cutoff label, or null to indicate validation failure
     */
    private String validateMonthSelection() {
        Object selected = monthComboBox.getSelectedItem();
        if (selected == null || selected.toString().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please select a month before computing salaries.",
                    "Missing Selection",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return selected.toString();
    }

    /**
     * Shows an "access denied" message when the user's role lacks the action.
     *
     * @return true if the user may perform the action
     */
    private boolean checkPermission(String action) {
        if (currentUser.hasPermission(action)) {
            return true;
        }
        JOptionPane.showMessageDialog(this, "Your account is not allowed to do this.", "Access Denied", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    // === COMPUTATION (delegated to PayrollProcessor) ===

    private void computeSalaries() {
        if (!checkPermission(User.COMPUTE_PAYROLL)) return;
        String selectedMonth = validateMonthSelection();
        if (selectedMonth == null) return;

        if (singleEmployeeMode) {
            computeSingleEmployeeSalary(selectedMonth);
        } else {
            computeAllEmployeesSalary(selectedMonth);
        }
    }

    /**
     * Handles the all-employees batch computation path.
     */
    private void computeAllEmployeesSalary(String selectedMonth) {
        List<Employee> noAttendance = payrollProcessor.findEmployeesWithoutAttendance(employees, attendance, selectedMonth);
        if (!noAttendance.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Employee employee : noAttendance) {
                names.add(employee.getFullName() + " (" + employee.getEmployeeId() + ")");
            }
            JOptionPane.showMessageDialog(this,
                    "The following employees have no attendance records for " + selectedMonth
                            + " and will be computed with 0 hours:\n" + String.join("\n", names),
                    "No Attendance Records Found",
                    JOptionPane.WARNING_MESSAGE);
        }

        payslips = payrollProcessor.processPayroll(employees, attendance, selectedMonth);
        populateResultsTable(payslips);
        JOptionPane.showMessageDialog(this, "Salary computation complete.", "Computation Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Handles the single-employee computation path.
     */
    private void computeSingleEmployeeSalary(String selectedMonth) {
        String employeeId = employeeIdField.getText().trim();
        if (employeeId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an employee number.", "Missing Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Employee employee;
        try {
            employee = dataHandler.findEmployeeById(employeeId);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to load employees", e);
            JOptionPane.showMessageDialog(this, "Unable to load employee data.", "File Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (employee == null) {
            JOptionPane.showMessageDialog(this, "Employee number does not exist.", "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        payslips = new ArrayList<>();
        payslips.add(payrollProcessor.generatePayslip(employee, attendance.get(employee.getEmployeeId()), selectedMonth));
        populateResultsTable(payslips);
        JOptionPane.showMessageDialog(this, "Salary computation complete.", "Computation Complete", JOptionPane.INFORMATION_MESSAGE);
    }

    // === RESULTS DISPLAY ===

    /**
     * Clears and repopulates the results table from the payslips. Amounts are
     * rounded to 2 decimals (HALF_UP) for display only.
     */
    private void populateResultsTable(List<Payslip> results) {
        tableModel.setRowCount(0);
        for (Payslip payslip : results) {
            List<Object> row = new ArrayList<>(List.of(
                payslip.getEmployeeId(),
                payslip.getEmployeeName(),
                payslip.getEmploymentType(),
                peso(payslip.getHourlyRate()),
                String.format("%.2f hrs", payslip.getHoursWorked()),
                peso(payslip.getGrossPay())));
            for (BigDecimal amount : payslip.getDeductionBreakdown().values()) {
                row.add(peso(amount));
            }
            row.add(peso(payslip.getTotalDeductions()));
            row.add(peso(payslip.getNetPay()));
            row.add(peso(payslip.getAllowances()));
            tableModel.addRow(row.toArray());
        }
    }

    /** Formats an amount for display, e.g. ₱40,088.97. Formatter rounds BigDecimal HALF_UP. */
    private static String peso(BigDecimal amount) {
        return String.format("₱%,.2f", amount);
    }

    // === NAVIGATION ===

    /**
     * Opens the main dashboard frame and disposes this frame.
     */
    private void openDashboard() {
        new DashboardFrame(currentUser).setVisible(true);
        this.dispose();
    }

    // === EVENT HANDLERS ===

    private void computeButtonActionPerformed(ActionEvent evt) { computeSalaries(); }
    private void summaryButtonActionPerformed(ActionEvent evt) { showPayrollSummary(); }
    private void saveButtonActionPerformed(ActionEvent evt) {
        if (!checkPermission(User.SAVE_PAYROLL_RESULTS)) return;
        if (payslips.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No computed results to save. Please compute salaries first.",
                    "No Data",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            dataHandler.savePayrollResults(payslips);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save payroll results", e);
            JOptionPane.showMessageDialog(this, "Failed to save results: " + e.getMessage(), "IO Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Results saved for " + payslips.get(0).getCutoffLabel(), "Save Complete", JOptionPane.INFORMATION_MESSAGE);
    }
    private void backButtonActionPerformed(ActionEvent evt) { openDashboard(); }

    /**
     * Displays aggregate payroll totals for ALL employees in the selected cutoff period,
     * per requirement MPHCR05. Independent of which computation mode the frame is in,
     * always reflects the full employee list summary.
     */
    private void showPayrollSummary() {
        if (!checkPermission(User.VIEW_PAYROLL_SUMMARY)) return;
        String selectedMonth = validateMonthSelection();
        if (selectedMonth == null) return;

        if (employees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No employee data loaded. Please check that the CSV file exists and try again.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, BigDecimal> summary = payrollProcessor.computePayrollSummary(
                payrollProcessor.processPayroll(employees, attendance, selectedMonth));

        String msg = String.format(
                "Cutoff: %s\n\nTotal Employees: %d\nTotal Gross Pay: ₱%,.2f\nTotal Deductions: ₱%,.2f\nTotal Net Pay: ₱%,.2f\nAverage Net Pay: ₱%,.2f",
                selectedMonth,
                summary.get(PayrollProcessor.SUMMARY_TOTAL_EMPLOYEES).intValue(),
                summary.get(PayrollProcessor.SUMMARY_TOTAL_GROSS),
                summary.get(PayrollProcessor.SUMMARY_TOTAL_DEDUCTIONS),
                summary.get(PayrollProcessor.SUMMARY_TOTAL_NET),
                summary.get(PayrollProcessor.SUMMARY_AVERAGE_NET));
        JOptionPane.showMessageDialog(this, msg, "Payroll Summary (All Employees)", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Applies standard styling to a JButton.
     */
    private void applyButtonStyle(javax.swing.JButton button) {
        UIStyleHelper.applyButtonStyle(button);
    }

    /**
     * Applies secondary button styling (gray).
     */
    private void applySecondaryButtonStyle(javax.swing.JButton button) {
        UIStyleHelper.applySecondaryButtonStyle(button);
    }

    /**
     * Applies standard styling to a JTextField.
     */
    private void applyFieldStyle(javax.swing.JTextField field) {
        UIStyleHelper.applyFieldStyle(field);
    }
}
