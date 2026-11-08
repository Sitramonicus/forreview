package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class PayrollAdminUI extends UIBase {

    // ── State ─────────────────────────────────────────────────────────────────
    private String[]  currentEmp;
    private boolean   isHR;

    // ── Local colors not in UIBase ────────────────────────────────────────────
    private static final Color TEXT    = new Color(30, 30, 30);
    private static final Color ROW_ALT = new Color(235, 240, 252);

    // ── Payslip labels ────────────────────────────────────────────────────────
    private JLabel valEmpNo, valName, valPosition;
    private JLabel valHours;
    private JLabel valSemi, valRice, valPhone, valClothing;
    private JLabel valGross;
    private JLabel valSSS, valPH, valPag, valTax, valTotDed;
    private JLabel valNet;

    // ── History table ─────────────────────────────────────────────────────────
    private DefaultTableModel histModel;

    // ── Search (HR only) ─────────────────────────────────────────────────────
    private JTextField txtEmpSearch;

    // ── Per-period payroll state ──────────────────────────────────────────────
    private JTable  histTable;
    private JLabel  valPeriod;
    private JPanel  payslipPanel;
    private java.util.List<Payroll.PayrollResult> periodResults = new java.util.ArrayList<>();
    private java.util.List<String> periodLabels = new java.util.ArrayList<>();
    private int selectedPeriod = -1;

    // =========================================================================
    public PayrollAdminUI(String empNo, String[] emp) {
        super("MotorPH – Payroll", empNo, emp);
        activeScreen = "Payroll";
        this.currentEmp = emp;
        this.isHR = EmployeeData.isHR(emp) || EmployeeData.isPayroll(emp);

        ActionListener[] nav = {
                e -> nav(() -> new DashboardUI(empNo, emp)),
                e -> nav(() -> new ProfileUI(empNo, emp)),
                e -> nav(() -> new PayrollAdminUI(empNo, emp)),
                e -> nav(() -> new AttendanceUI(empNo, emp)),
                e -> nav(() -> new adminModuleUI(empNo, emp)),
                e -> nav(() -> new loginUI()),
        };

        add(buildSidebar(empNo, emp, nav), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);

        refresh(emp);
    }

    // =========================================================================
    // MAIN CONTENT
    // =========================================================================
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(WHITE);
        main.add(buildHeader(),  BorderLayout.NORTH);
        main.add(buildBody(),    BorderLayout.CENTER);
        return main;
    }

    // ── Blue header bar ───────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLUE);
        hdr.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Payroll");
        title.setFont(new Font(FONT, Font.BOLD, 22));
        title.setForeground(WHITE);

        JPanel rightRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightRow.setBackground(BLUE);

        // Search is only available to HR / Payroll roles
        if (isHR) {
            JLabel searchLbl = new JLabel("Employee No:");
            searchLbl.setForeground(WHITE);
            searchLbl.setFont(new Font(FONT, Font.PLAIN, 13));

            txtEmpSearch = new JTextField(9);
            txtEmpSearch.setFont(new Font(FONT, Font.PLAIN, 13));
            txtEmpSearch.setText(empNo);

            JButton btnSearch = pill("Search", YELLOW, TEXT);
            JButton btnMyPay  = pill("My Pay Slip", WHITE, BLUE);
            btnSearch.addActionListener(e -> searchEmp());
            btnMyPay .addActionListener(e -> { txtEmpSearch.setText(empNo); refresh(emp); });

            rightRow.add(searchLbl);
            rightRow.add(txtEmpSearch);
            rightRow.add(btnSearch);
            rightRow.add(btnMyPay);
        }

        JButton btnSlip  = pill("Full Payslip", RED, WHITE);
        JButton btnPrint = pill("Print", GREEN, WHITE);
        btnSlip .addActionListener(e -> showFullPayslip());
        btnPrint.addActionListener(e -> doPrint());
        rightRow.add(btnSlip);
        rightRow.add(btnPrint);

        hdr.add(title,    BorderLayout.WEST);
        hdr.add(rightRow, BorderLayout.EAST);
        return hdr;
    }

    // ── Two-column body ───────────────────────────────────────────────────────
    private JSplitPane buildBody() {
        JSplitPane split = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildPayslipPanel(),
                buildHistoryPanel());
        split.setDividerLocation(460);
        split.setDividerSize(5);
        split.setBorder(null);
        split.setBackground(WHITE);
        return split;
    }

    // ── Left: payslip ─────────────────────────────────────────────────────────
    private JPanel buildPayslipPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(WHITE);

        JLabel title = new JLabel("  My Pay Slip");
        title.setFont(new Font(FONT, Font.BOLD, 20));
        title.setBackground(YELLOW);
        title.setOpaque(true);
        title.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setBackground(WHITE);
        grid.setBorder(new EmptyBorder(12, 20, 12, 20));

        GridBagConstraints gl = new GridBagConstraints();
        gl.anchor = GridBagConstraints.WEST;
        gl.insets = new Insets(4, 0, 4, 0);

        GridBagConstraints gv = new GridBagConstraints();
        gv.anchor = GridBagConstraints.WEST;
        gv.insets = new Insets(4, 20, 4, 0);
        gv.gridwidth = GridBagConstraints.REMAINDER;

        valPeriod   = addRow(grid, "Pay Period",         gl, gv, true);
        valPeriod.setForeground(BLUE);
        valEmpNo    = addRow(grid, "Employee No.",       gl, gv, false);
        valName     = addRow(grid, "Name",               gl, gv, false);
        valPosition = addRow(grid, "Position",           gl, gv, false);
        valHours    = addRow(grid, "Hours Worked",       gl, gv, false);

        addSectionHead(grid, "Earnings");
        valSemi     = addRow(grid, "Earnings (Hrs × Rate)", gl, gv, false);
        valRice     = addRow(grid, "Rice Subsidy",       gl, gv, false);
        valPhone    = addRow(grid, "Phone Allowance",    gl, gv, false);
        valClothing = addRow(grid, "Clothing Allowance", gl, gv, false);
        valGross    = addRow(grid, "Gross Pay",          gl, gv, true);

        addSectionHead(grid, "Deductions");
        valSSS    = addRow(grid, "SSS",              gl, gv, false);
        valPH     = addRow(grid, "PhilHealth",       gl, gv, false);
        valPag    = addRow(grid, "Pag-IBIG",         gl, gv, false);
        valTax    = addRow(grid, "Withholding Tax",  gl, gv, false);
        valTotDed = addRow(grid, "Total Deductions", gl, gv, true);

        addSectionHead(grid, "Summary");
        valNet = addRow(grid, "NET PAY", gl, gv, true);
        valNet.setForeground(GREEN);
        valNet.setFont(new Font(FONT, Font.BOLD, 16));

        p.add(title, BorderLayout.NORTH);
        p.add(new JScrollPane(grid), BorderLayout.CENTER);
        payslipPanel = p;
        return p;
    }

    // ── Right: history table ──────────────────────────────────────────────────
    private JPanel buildHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(WHITE);
        p.setBorder(new EmptyBorder(0, 10, 10, 10));

        JLabel title = new JLabel("  Payroll History");
        title.setFont(new Font(FONT, Font.BOLD, 18));
        title.setBackground(YELLOW);
        title.setOpaque(true);
        title.setBorder(new EmptyBorder(8, 12, 8, 12));

        histModel = new DefaultTableModel(
                new String[]{"Pay Period", "Gross Pay", "Deductions", "Net Pay"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };

        histTable = new JTable(histModel);
        JTable tbl = histTable;
        tbl.setFont(new Font(FONT, Font.PLAIN, 13));
        tbl.setRowHeight(30);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(new Color(200, 215, 255));
        tbl.setBackground(WHITE);

        JTableHeader th = tbl.getTableHeader();
        th.setFont(new Font(FONT, Font.BOLD, 13));
        th.setBackground(BLUE);
        th.setForeground(WHITE);
        th.setPreferredSize(new Dimension(0, 36));
        th.setReorderingAllowed(false);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                                                           boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                if (!sel) setBackground(row % 2 == 0 ? WHITE : ROW_ALT);
                if (col == 3 && !sel) setForeground(GREEN);
                else setForeground(TEXT);
                return this;
            }
        };
        for (int i = 0; i < 4; i++)
            tbl.getColumnModel().getColumn(i).setCellRenderer(center);

        tbl.getSelectionModel().addListSelectionListener(ev -> {
            if (!ev.getValueIsAdjusting() && tbl.getSelectedRow() >= 0)
                showPeriod(tbl.getSelectedRow());
        });

        JScrollPane sp = new JScrollPane(tbl);
        sp.setBorder(new LineBorder(new Color(210, 215, 230)));

        p.add(title, BorderLayout.NORTH);
        p.add(sp,    BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // DATA
    // =========================================================================
    private void refresh(String[] e) {
        currentEmp = e;
        valEmpNo   .setText(e[0]);
        valName    .setText(e[2] + " " + e[1]);
        valPosition.setText(e[11]);
        valPeriod  .setText("Loading…");
        histModel.setRowCount(0);
        periodResults = new java.util.ArrayList<>();
        periodLabels  = new java.util.ArrayList<>();
        selectedPeriod = -1;

        final String[] emp = e;
        new SwingWorker<Void, Void>() {
            final java.util.List<Payroll.PayrollResult> results = new java.util.ArrayList<>();
            final java.util.List<String> labels = new java.util.ArrayList<>();
            protected Void doInBackground() {
                java.util.List<java.time.YearMonth> months = Attendance.getWorkedMonths(emp[0]);
                java.time.format.DateTimeFormatter mf =
                        java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy");
                if (months.isEmpty()) {
                    results.add(new PayrollProcessor().processPayrollForGUI(emp, 0));
                    labels.add("No attendance on record");
                } else {
                    for (java.time.YearMonth ym : months) {
                        double hrs = Attendance.getHoursWorked(emp[0], ym);
                        results.add(new PayrollProcessor().processPayrollForGUI(emp, hrs));
                        labels.add(ym.format(mf));
                    }
                }
                return null;
            }
            protected void done() {
                periodResults = results;
                periodLabels  = labels;
                for (int i = 0; i < results.size(); i++) {
                    Payroll.PayrollResult r = results.get(i);
                    histModel.addRow(new Object[]{
                            labels.get(i), fmt(r.grossPay), fmt(r.totalDeductions), fmt(r.netPay)
                    });
                }
                int last = results.size() - 1;
                if (last >= 0) {
                    showPeriod(last);
                    histTable.setRowSelectionInterval(last, last);
                }
            }
        }.execute();
    }

    /** Populate the left payslip from one computed period. */
    private void showPeriod(int idx) {
        if (idx < 0 || idx >= periodResults.size()) return;
        selectedPeriod = idx;
        Payroll.PayrollResult r = periodResults.get(idx);
        valPeriod  .setText(periodLabels.get(idx));
        valHours   .setText(String.format("%.1f hrs", r.hoursWorked));
        valSemi    .setText(fmt(r.grossPay - r.totalAllowances)); // hours × hourly
        valRice    .setText(fmt(r.riceSubsidy));
        valPhone   .setText(fmt(r.phoneAllow));
        valClothing.setText(fmt(r.clothingAllow));
        valGross   .setText(fmt(r.grossPay));
        valSSS     .setText(fmt(r.sssDeduction));
        valPH      .setText(fmt(r.philHealth));
        valPag     .setText(fmt(r.pagIbig));
        valTax     .setText(fmt(r.withholdingTax));
        valTotDed  .setText(fmt(r.totalDeductions));
        valNet     .setText(fmt(r.netPay));
    }

    private void searchEmp() {
        if (txtEmpSearch == null) return;
        String id = txtEmpSearch.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter an Employee Number.",
                    "Missing", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String[] found = EmployeeData.findEmployee(id);
        if (found != null) refresh(found);
    }

    private void showFullPayslip() {
        if (selectedPeriod < 0 || selectedPeriod >= periodResults.size()) {
            JOptionPane.showMessageDialog(this,
                    "Select a pay period from the history table first.",
                    "No Period Selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] e = currentEmp;
        Payroll.PayrollResult r = periodResults.get(selectedPeriod);
        String period = periodLabels.get(selectedPeriod);

        String slip = String.format(
                "╔════════════════════════════════════════════╗%n" +
                        "║          MOTORPH PAYROLL SYSTEM            ║%n" +
                        "║                 PAYSLIP                    ║%n" +
                        "╠════════════════════════════════════════════╣%n" +
                        "  Pay Period   : %s%n" +
                        "  Employee No  : %s%n" +
                        "  Name         : %s %s%n" +
                        "  Position     : %s%n" +
                        "────────────────────────────────────────────%n" +
                        "  Hours Worked : %.1f hrs%n" +
                        "────────────────────────────────────────────%n" +
                        "  EARNINGS%n" +
                        "  Earnings (Hrs × Rate): %12s%n" +
                        "  Rice Subsidy         : %12s%n" +
                        "  Phone Allowance      : %12s%n" +
                        "  Clothing Allowance   : %12s%n" +
                        "  ─────────────────────────────────────%n" +
                        "  GROSS PAY            : %12s%n" +
                        "────────────────────────────────────────────%n" +
                        "  DEDUCTIONS%n" +
                        "  SSS                  : %12s%n" +
                        "  PhilHealth           : %12s%n" +
                        "  Pag-IBIG             : %12s%n" +
                        "  Withholding Tax      : %12s%n" +
                        "  ─────────────────────────────────────%n" +
                        "  TOTAL DEDUCTIONS     : %12s%n" +
                        "════════════════════════════════════════════%n" +
                        "  NET PAY              : %12s%n" +
                        "╚════════════════════════════════════════════╝",
                period,
                e[0], e[2], e[1], e[11],
                r.hoursWorked,
                fmt(r.grossPay - r.totalAllowances),
                fmt(r.riceSubsidy),
                fmt(r.phoneAllow),
                fmt(r.clothingAllow),
                fmt(r.grossPay),
                fmt(r.sssDeduction),
                fmt(r.philHealth),
                fmt(r.pagIbig),
                fmt(r.withholdingTax),
                fmt(r.totalDeductions),
                fmt(r.netPay)
        );

        JTextArea ta = new JTextArea(slip);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setMargin(new Insets(10, 12, 10, 12));
        ta.setBackground(new Color(252, 252, 252));

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(480, 460));

        JOptionPane.showMessageDialog(this, sp,
                "Full Payslip — " + e[2] + " " + e[1] + " — " + period,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void doPrint() {
        try {
            PrinterJob job = PrinterJob.getPrinterJob();
            final JPanel ref = (payslipPanel != null)
                    ? payslipPanel : (JPanel) getContentPane().getComponent(1);
            job.setPrintable((g, pf, pi) -> {
                if (pi > 0) return Printable.NO_SUCH_PAGE;
                Graphics2D g2 = (Graphics2D) g;
                g2.translate(pf.getImageableX(), pf.getImageableY());
                ref.printAll(g2);
                return Printable.PAGE_EXISTS;
            });
            if (job.printDialog()) {
                job.print();
                JOptionPane.showMessageDialog(this, "Printing complete.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Print error: " + e.getMessage());
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================
    private JLabel addRow(JPanel grid, String label,
                          GridBagConstraints gl, GridBagConstraints gv,
                          boolean bold) {
        GridBagConstraints lc = (GridBagConstraints) gl.clone();
        lc.gridx = 0; lc.gridy = GridBagConstraints.RELATIVE;

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(bold ? BLUE : MUTED);
        grid.add(lbl, lc);

        GridBagConstraints vc = (GridBagConstraints) gv.clone();
        vc.gridx = 1; vc.gridy = GridBagConstraints.RELATIVE;

        JLabel val = new JLabel("—");
        val.setFont(new Font(FONT, bold ? Font.BOLD : Font.PLAIN, bold ? 14 : 13));
        val.setForeground(bold ? TEXT : new Color(50, 50, 50));
        grid.add(val, vc);

        return val;
    }

    private void addSectionHead(JPanel grid, String text) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.gridwidth = 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(10, 0, 4, 0);

        JLabel head = new JLabel("  " + text);
        head.setFont(new Font(FONT, Font.BOLD, 13));
        head.setForeground(WHITE);
        head.setBackground(BLUE);
        head.setOpaque(true);
        head.setBorder(new EmptyBorder(4, 6, 4, 6));
        grid.add(head, gc);
    }

    private JButton pill(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font(FONT, Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(110, 28));
        return b;
    }

    private String fmt(double v) {
        return String.format("%,.2f", v);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmployeeData.loadEmployeeData();
            String[] e = EmployeeData.records.get(1);
            new PayrollAdminUI(e[0], e).setVisible(true);
        });
    }
}
