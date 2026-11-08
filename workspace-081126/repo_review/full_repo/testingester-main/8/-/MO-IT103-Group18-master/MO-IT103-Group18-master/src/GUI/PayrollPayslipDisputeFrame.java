package GUI;

import Backend.PayslipDispute;
import Backend.PayslipDisputeManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Payroll staff screen. Launch only after payroll-role authorization is implemented. */
public class PayrollPayslipDisputeFrame extends JFrame {
    private final PayslipDisputeManager manager = PayslipDisputeManager.getInstance();
    private final String payrollStaffId;
    private final DefaultTableModel model = new DefaultTableModel(
        new String[]{"Dispute ID", "Employee ID", "Payslip Period", "Reason", "Description", "Status"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    private final JTable table = new JTable(model);

    public PayrollPayslipDisputeFrame(String payrollStaffId) {
        super("Payroll - Payslip Dispute Management");
        if (payrollStaffId == null || payrollStaffId.isBlank())
            throw new IllegalArgumentException("Payroll staff ID is required.");
        this.payrollStaffId = payrollStaffId;
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1050, 500);
        setLocationRelativeTo(null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton refresh = new JButton("Refresh");
        JButton review = new JButton("Mark Under Review");
        JButton resolve = new JButton("Resolve");
        JButton reject = new JButton("Reject");
        JPanel buttons = new JPanel();
        buttons.add(refresh); buttons.add(review); buttons.add(resolve); buttons.add(reject);
        refresh.addActionListener(e -> refreshTable());
        review.addActionListener(e -> updateSelected("REVIEW"));
        resolve.addActionListener(e -> updateSelected("RESOLVE"));
        reject.addActionListener(e -> updateSelected("REJECT"));
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        refreshTable();
    }

    private void refreshTable() {
        model.setRowCount(0);
        for (PayslipDispute d : manager.getAllDisputes())
            model.addRow(new Object[]{d.getDisputeId(), d.getEmployeeId(), d.getPayslipPeriod(),
                d.getReason(), d.getDescription(), d.getStatus()});
    }

    private void updateSelected(String action) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a dispute first."); return;
        }
        String id = model.getValueAt(table.convertRowIndexToModel(row), 0).toString();
        try {
            if ("REVIEW".equals(action)) {
                manager.reviewDispute(id, payrollStaffId);
            } else {
                String notes = JOptionPane.showInputDialog(this,
                    "Enter " + ("RESOLVE".equals(action) ? "resolution" : "rejection") + " notes:");
                if (notes == null) return;
                if (notes.isBlank()) {
                    JOptionPane.showMessageDialog(this, "Notes are required."); return;
                }
                if ("RESOLVE".equals(action)) manager.resolveDispute(id, payrollStaffId, notes.trim());
                else manager.rejectDispute(id, payrollStaffId, notes.trim());
            }
            refreshTable();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Unable to Update Dispute", JOptionPane.ERROR_MESSAGE);
        }
    }
}
