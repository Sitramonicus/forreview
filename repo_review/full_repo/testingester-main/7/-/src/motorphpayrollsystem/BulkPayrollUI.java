package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class BulkPayrollUI extends UIBase {

    private static final String[] COLUMNS = {
        "Emp No", "Name", "Position", "Hours Worked",
        "Gross Pay", "Deductions", "Net Pay"
    };

    private DefaultTableModel tableModel;
    private JLabel statusLbl;
    private JButton btnCompute, btnExport;

    public BulkPayrollUI(String empNo, String[] emp) {
        super("MotorPH – Bulk Payroll", empNo, emp);
        activeScreen = "Admin";
        setSize(1200, 660);

        ActionListener[] nav = {
            e -> nav(() -> new DashboardUI(empNo, emp)),
            e -> nav(() -> new ProfileUI(empNo, emp)),
            e -> nav(() -> new PayrollUI(empNo, emp)),
            e -> nav(() -> new AttendanceUI(empNo, emp)),
            e -> nav(() -> new adminModuleUI(empNo, emp)),
            e -> nav(() -> new loginUI()),
        };

        add(buildSidebar(empNo, emp, nav), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("Bulk Payroll Computation"), BorderLayout.NORTH);

        // ── Toolbar ───────────────────────────────────────────────────────────
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(LIGHT);
        toolbar.setBorder(new MatteBorder(0, 0, 1, 0, new Color(220, 225, 240)));

        btnCompute = actionBtn("Compute All", BLUE, WHITE);
        btnExport  = actionBtn("Export CSV",  GREEN, WHITE);
        JButton btnBack = actionBtn("← Admin", PURPLE, WHITE);

        btnCompute.setPreferredSize(new Dimension(130, 30));
        btnExport .setPreferredSize(new Dimension(110, 30));
        btnBack   .setPreferredSize(new Dimension(100, 30));
        btnExport.setEnabled(false);

        btnCompute.addActionListener(e -> runBulkCompute());
        btnExport .addActionListener(e -> exportToCSV());
        btnBack   .addActionListener(e -> nav(() -> new adminModuleUI(empNo, emp)));

        statusLbl = new JLabel("Click 'Compute All' to process payroll for all employees.");
        statusLbl.setFont(new Font(FONT, Font.ITALIC, 12));
        statusLbl.setForeground(MUTED);

        toolbar.add(btnCompute);
        toolbar.add(btnExport);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(btnBack);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(statusLbl);

        // ── Table ─────────────────────────────────────────────────────────────
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(200, 215, 255));

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font(FONT, Font.BOLD, 13));
        th.setBackground(BLUE);
        th.setForeground(WHITE);
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);

        int[] widths = {70, 160, 160, 100, 130, 120, 120};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer rdr = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(col < 3 ? LEFT : CENTER);
                if (!sel) setBackground(row % 2 == 0 ? WHITE : LIGHT);
                if (col == 6 && !sel) setForeground(GREEN);
                else setForeground(new Color(30, 30, 50));
                return this;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(rdr);

        // ── Summary bar ───────────────────────────────────────────────────────
        JLabel sumLbl = new JLabel("  Total: 0 employees processed.");
        sumLbl.setFont(new Font(FONT, Font.BOLD, 13));
        sumLbl.setForeground(BLUE);

        JPanel sumBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        sumBar.setBackground(new Color(235, 240, 252));
        sumBar.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 230)));
        sumBar.add(sumLbl);

        // Update summary after each compute
        tableModel.addTableModelListener(e2 ->
            sumLbl.setText("  Total: " + tableModel.getRowCount() + " employees processed."));

        JPanel body = new JPanel(new BorderLayout());
        body.add(toolbar,              BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(sumBar,               BorderLayout.SOUTH);

        main.add(body, BorderLayout.CENTER);
        return main;
    }

    // =========================================================================
    // COMPUTE
    // =========================================================================
    private void runBulkCompute() {
        btnCompute.setEnabled(false);
        btnExport.setEnabled(false);
        statusLbl.setText("Computing payroll for all employees…");
        tableModel.setRowCount(0);

        new SwingWorker<List<Payroll.PayrollResult>, Void>() {
            protected List<Payroll.PayrollResult> doInBackground() {
                List<Payroll.PayrollResult> results = new ArrayList<>();
                if (EmployeeData.records == null) return results;
                for (int i = 1; i < EmployeeData.records.size(); i++) {
                    String[] e = EmployeeData.records.get(i);
                    if (e == null || e[0] == null || e[0].trim().isEmpty()) continue;
                    try {
                        double hours = Attendance.getTotalHoursWorked(e[0]);
                        results.add(Payroll.compute(e, hours));
                    } catch (Exception ignored) {}
                }
                return results;
            }

            protected void done() {
                try {
                    List<Payroll.PayrollResult> results = get();
                    for (Payroll.PayrollResult r : results) {
                        tableModel.addRow(new Object[]{
                            r.empNo,
                            r.lastName + ", " + r.firstName,
                            r.position,
                            String.format("%.1f", r.hoursWorked),
                            String.format("₱%,.2f", r.grossPay),
                            String.format("₱%,.2f", r.totalDeductions),
                            String.format("₱%,.2f", r.netPay)
                        });
                    }
                    statusLbl.setText("Done — " + results.size() + " employees processed.");
                    btnExport.setEnabled(true);
                } catch (Exception ex) {
                    statusLbl.setText("Error: " + ex.getMessage());
                } finally {
                    btnCompute.setEnabled(true);
                }
            }
        }.execute();
    }

    // =========================================================================
    // EXPORT
    // =========================================================================
    private void exportToCSV() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("BulkPayroll_Export.csv"));
        fc.setDialogTitle("Save Bulk Payroll as CSV");
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".csv"))
            file = new java.io.File(file.getAbsolutePath() + ".csv");

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(file))) {
            pw.println(String.join(",", COLUMNS));
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    if (j > 0) row.append(",");
                    String val = tableModel.getValueAt(i, j).toString();
                    if (val.contains(",")) val = "\"" + val + "\"";
                    row.append(val);
                }
                pw.println(row);
            }
            Toast.success(this, "Exported " + tableModel.getRowCount() +
                " records to " + file.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
