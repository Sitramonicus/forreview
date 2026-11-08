package GUI;

import Backend.PayslipDispute;
import Backend.PayslipDisputeManager;
import Backend.PayrollDataLoader;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.Month;
import java.util.UUID;

/** Employee screen for submitting and tracking the logged-in employee's disputes. */
public class EmployeePayslipDisputeFrame extends JFrame {
    private final String employeeId;
    private final PayslipDisputeManager manager = PayslipDisputeManager.getInstance();
    private final JComboBox<String> period = new JComboBox<>(new String[]{
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    });
    private final JComboBox<String> year = new JComboBox<>(new String[]{"2024", "2025", "2026", "2027"});
    private final JComboBox<String> reason = new JComboBox<>(new String[]{
        "Incorrect Basic Salary", "Incorrect Overtime Pay", "Incorrect Deduction",
        "Missing Allowance", "Incorrect Tax", "Other"
    });
    private final JTextArea description = new JTextArea(4, 30);
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Dispute ID", "Payslip Period", "Reason", "Status", "Submitted"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

    public EmployeePayslipDisputeFrame(String employeeId) {
        super("My Payslip Disputes");
        if (employeeId == null || employeeId.isBlank())
            throw new IllegalArgumentException("Employee ID is required.");
        this.employeeId = employeeId;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Payslip month:"), c);
        c.gridx = 1; form.add(period, c);
        c.gridx = 2; form.add(new JLabel("Year:"), c);
        c.gridx = 3; form.add(year, c);
        c.gridx = 0; c.gridy = 1; form.add(new JLabel("Reason:"), c);
        c.gridx = 1; c.gridwidth = 3; form.add(reason, c);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 1; c.anchor = GridBagConstraints.NORTH;
        form.add(new JLabel("Description:"), c);
        c.gridx = 1; c.gridwidth = 3; form.add(new JScrollPane(description), c);

        JButton submit = new JButton("Submit Dispute");
        JButton refresh = new JButton("Refresh My Disputes");
        JPanel actions = new JPanel(); actions.add(submit); actions.add(refresh);
        submit.addActionListener(e -> submitDispute());
        refresh.addActionListener(e -> refreshTable());

        JPanel north = new JPanel(new BorderLayout());
        north.add(form, BorderLayout.CENTER); north.add(actions, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        refreshTable();
    }

    private void submitDispute() {
        String selectedMonthStr = (String) period.getSelectedItem();
        int selectedYear = Integer.parseInt((String) year.getSelectedItem());
        Month month = Month.valueOf(selectedMonthStr.toUpperCase());

        // Validate if attendance records exist for the selected month before submitting dispute
        if (PayrollDataLoader.getTotalHoursWorked(employeeId, month, selectedYear) == 0) {
            JOptionPane.showMessageDialog(this, "No attendance records for " + employeeId + " in the selected month.",
                    "No Data", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String details = description.getText().trim();
        if (details.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please describe the payslip issue.",
                "Missing Description", JOptionPane.WARNING_MESSAGE);
            description.requestFocus(); return;
        }
        
        String payPeriod = selectedMonthStr + " " + selectedYear;
        String id = "DSP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        try {
            manager.submitDispute(new PayslipDispute(id, employeeId, payPeriod,
                (String) reason.getSelectedItem(), details));
            JOptionPane.showMessageDialog(this, "Dispute submitted. Reference: " + id);
            description.setText("");
            refreshTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Submission Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (PayslipDispute d : manager.getEmployeeDisputes(employeeId)) {
            model.addRow(new Object[]{d.getDisputeId(), d.getPayslipPeriod(), d.getReason(),
                d.getStatus(), d.getDateSubmitted()});
        }
    }
}
