package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class EmployeeRecords extends UIBase {

    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;

    private static final String[] COLUMNS = {
        "Emp No","Last Name","First Name","Position",
        "Basic Salary","Hourly Rate","Gross Pay","Deductions","Net Pay"
    };

    public EmployeeRecords(String empNo, String[] emp) {
        super("MotorPH – Employee Records", empNo, emp);
        activeScreen = "Admin";
        setSize(1200, 640);

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
        loadAll();

        // Auto-refresh table when any employee data changes
        EmployeeData.addChangeListener(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (tableModel != null) loadAll();
            });
        });
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("Employee Records"), BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        toolbar.setBackground(LIGHT);
        toolbar.setBorder(new MatteBorder(0,0,1,0,new Color(220,225,240)));

        JLabel sl = new JLabel("Search:");
        sl.setFont(new Font(FONT, Font.PLAIN, 13));
        txtSearch = new JTextField(18);
        txtSearch.setFont(new Font(FONT, Font.PLAIN, 13));

        JButton bS      = actionBtn("Search",       BLUE,                WHITE);
        JButton bA      = actionBtn("Show All",     MUTED,               WHITE);
        JButton bBack   = actionBtn("← Admin",      PURPLE,              WHITE);
        JButton bView   = actionBtn("View Details", new Color(37,99,235), WHITE);
        JButton bExport = actionBtn("Export CSV",   new Color(22,163,74), WHITE);
        bS     .setPreferredSize(new Dimension(90,  30));
        bA     .setPreferredSize(new Dimension(90,  30));
        bBack  .setPreferredSize(new Dimension(100, 30));
        bView  .setPreferredSize(new Dimension(120, 30));
        bExport.setPreferredSize(new Dimension(110, 30));
        bView.setEnabled(false); // enabled when a row is selected

        bS     .addActionListener(e -> search());
        bA     .addActionListener(e -> loadAll());
        bBack  .addActionListener(e -> nav(() -> new adminModuleUI(empNo, emp)));
        bView  .addActionListener(e -> viewSelectedEmployee());
        bExport.addActionListener(e -> exportToCSV());
        txtSearch.addActionListener(e -> search());

        toolbar.add(sl); toolbar.add(txtSearch);
        toolbar.add(bS); toolbar.add(bA);
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(bView);
        toolbar.add(bExport);
        toolbar.add(Box.createHorizontalStrut(16));
        toolbar.add(bBack);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(
            e -> bView.setEnabled(table.getSelectedRow() >= 0));
        table.setAutoCreateRowSorter(true); // click header to sort
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0,0));
        table.setSelectionBackground(new Color(200,215,255));

        JTableHeader th = table.getTableHeader();
        th.setFont(new Font(FONT, Font.BOLD, 13));
        th.setBackground(BLUE); th.setForeground(WHITE);
        th.setPreferredSize(new Dimension(0, 34));
        th.setReorderingAllowed(false);

        int[] widths = {70,110,110,170,110,90,110,110,110};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t,v,sel,foc,row,col);
                setHorizontalAlignment(col < 3 ? LEFT : CENTER);
                if (!sel) setBackground(row%2==0 ? WHITE : LIGHT);
                if (col == 8 && !sel) setForeground(GREEN);
                else setForeground(new Color(30,30,50));
                return this;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);

        // Summary bar
        JPanel sumBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        sumBar.setBackground(new Color(235,240,252));
        sumBar.setBorder(new MatteBorder(1,0,0,0,new Color(210,215,230)));
        JLabel sl2 = new JLabel("Total Employees: " + (EmployeeData.records.size()-1));
        sl2.setFont(new Font(FONT, Font.BOLD, 13));
        sl2.setForeground(BLUE);
        sumBar.add(sl2);

        JPanel body = new JPanel(new BorderLayout());
        body.add(toolbar, BorderLayout.NORTH);
        body.add(new JScrollPane(table), BorderLayout.CENTER);
        body.add(sumBar, BorderLayout.SOUTH);

        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private void loadAll() {
        tableModel.setRowCount(0);
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] e = EmployeeData.records.get(i);
            if (e[0]==null||e[0].trim().isEmpty()) continue;
            try {
                double gross = Payroll.grossPay(e);
                double basic = Payroll.parseAmount(e[13]);
                double totD  = Payroll.totalDeductions(gross, basic);
                double tax   = Payroll.taxDeduction(Payroll.taxableIncome(gross, totD));
                double net   = Payroll.NetPay(gross, totD, tax);
                tableModel.addRow(new Object[]{e[0],e[1],e[2],e[11],
                    fmt(basic), fmt(Payroll.parseAmount(e[18])),
                    fmt(gross), fmt(totD+tax), fmt(net)});
            } catch (Exception ex) {
                tableModel.addRow(new Object[]{e[0],e[1],e[2],e[11],"—","—","—","—","—"});
            }
        }
    }

    private void search() {
        String q = txtSearch.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] e = EmployeeData.records.get(i);
            String name = (e[2]+" "+e[1]).toLowerCase();
            if (e[0].contains(q)||name.contains(q)||e[11].toLowerCase().contains(q)) {
                try {
                    double gross = Payroll.grossPay(e);
                    double basic = Payroll.parseAmount(e[13]);
                    double totD  = Payroll.totalDeductions(gross, basic);
                    double tax   = Payroll.taxDeduction(Payroll.taxableIncome(gross, totD));
                    double net   = Payroll.NetPay(gross, totD, tax);
                    tableModel.addRow(new Object[]{e[0],e[1],e[2],e[11],
                        fmt(basic),fmt(Payroll.parseAmount(e[18])),
                        fmt(gross),fmt(totD+tax),fmt(net)});
                } catch (Exception ex) {
                    tableModel.addRow(new Object[]{e[0],e[1],e[2],e[11],"—","—","—","—","—"});
                }
            }
        }
    }

    private void exportToCSV() {
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("EmployeeRecords_Export.csv"));
        fc.setDialogTitle("Save Employee Records as CSV");
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;

        java.io.File file = fc.getSelectedFile();
        if (!file.getName().endsWith(".csv"))
            file = new java.io.File(file.getAbsolutePath() + ".csv");

        try (java.io.PrintWriter pw = new java.io.PrintWriter(
                new java.io.FileWriter(file))) {
            // Header
            pw.println(String.join(",", COLUMNS));
            // Rows from table model
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < tableModel.getColumnCount(); j++) {
                    if (j > 0) row.append(",");
                    String val = tableModel.getValueAt(i, j).toString();
                    // Wrap in quotes if contains comma
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

    private void viewSelectedEmployee() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        String id = tableModel.getValueAt(modelRow, 0).toString();

        String[] e = EmployeeData.findEmployee(id);
        if (e == null) return;

        double gross  = Payroll.grossPay(e);
        double basic  = Payroll.parseAmount(e[13]);
        double totDed = Payroll.totalDeductions(gross, basic);
        double tax    = Payroll.taxDeduction(Payroll.taxableIncome(gross, totDed));
        double net    = Payroll.NetPay(gross, totDed, tax);

        String details = String.format(
            "╔═══════════════════════════════════════╗%n" +
            "║      EMPLOYEE DETAILS                 ║%n" +
            "╠═══════════════════════════════════════╣%n" +
            "  Employee No.   : %s%n" +
            "  Name           : %s, %s%n" +
            "  Birthday       : %s%n" +
            "  Address        : %s%n" +
            "  Phone No.      : %s%n" +
            "  Position       : %s%n" +
            "  Department     : %s%n" +
            "  Supervisor     : %s%n" +
            "  Status         : %s%n" +
            "───────────────────────────────────────%n" +
            "  SSS No.        : %s%n" +
            "  PhilHealth No. : %s%n" +
            "  TIN            : %s%n" +
            "  Pag-IBIG No.   : %s%n" +
            "───────────────────────────────────────%n" +
            "  Basic Salary   : %s%n" +
            "  Semi-Monthly   : %s%n" +
            "  Hourly Rate    : %s%n" +
            "  Rice Subsidy   : %s%n" +
            "  Phone Allow.   : %s%n" +
            "  Clothing Allow.: %s%n" +
            "───────────────────────────────────────%n" +
            "  GROSS PAY      : %s%n" +
            "  TOTAL DEDUCT.  : %s%n" +
            "  NET PAY        : %s%n" +
            "╚═══════════════════════════════════════╝",
            e[0], e[1], e[2], e[3], e[4], e[5],
            e[11], e.length > 20 ? e[20] : "", e[12], e[10],
            e[6], e[7], e[8], e[9],
            fmt(Payroll.parseAmount(e[13])),
            fmt(Payroll.parseAmount(e[17])),
            fmt(Payroll.parseAmount(e[18])),
            fmt(Payroll.parseAmount(e[14])),
            fmt(Payroll.parseAmount(e[15])),
            fmt(Payroll.parseAmount(e[16])),
            fmt(gross), fmt(totDed + tax), fmt(net));

        JTextArea ta = new JTextArea(details);
        ta.setEditable(false);
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setMargin(new java.awt.Insets(8, 10, 8, 10));
        ta.setBackground(new Color(252, 252, 252));

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(420, 500));

        JOptionPane.showMessageDialog(this, sp,
            "Details — " + e[2] + " " + e[1],
            JOptionPane.INFORMATION_MESSAGE);
    }

    private String fmt(double v) { return String.format("₱%,.2f", v); }
}
