package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class deleteEmp extends UIBase {

    private String[] selectedEmp;
    private JTextField txtSearch;
    private JTable tblResults;
    private DefaultTableModel tblModel;
    private JLabel valEmpNo, valName, valPosition, valDept, valStatus, valPhone;

    public deleteEmp(String empNo, String[] emp) {
        super("MotorPH – Delete Employee", empNo, emp);

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

        // Refresh table when data changes
        EmployeeData.addChangeListener(() -> {
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (tblModel != null) loadAll();
            });
        });
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("Delete Employee"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildSearchPanel(), buildDetailPanel());
        split.setDividerLocation(340);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setResizeWeight(0.3);

        main.add(split, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(14, 14, 14, 8));

        JLabel lbl = new JLabel("Search Employee to Delete");
        lbl.setFont(new Font(FONT, Font.BOLD, 14));
        lbl.setForeground(BLUE);

        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(LIGHT);
        txtSearch = new JTextField();
        txtSearch.setFont(new Font(FONT, Font.PLAIN, 13));

        JPanel btns = new JPanel(new GridLayout(1, 2, 4, 0));
        btns.setBackground(LIGHT);
        JButton bS = actionBtn("Search", BLUE, WHITE);
        JButton bA = actionBtn("All",    MUTED, WHITE);
        bS.setPreferredSize(new Dimension(70, 30));
        bA.setPreferredSize(new Dimension(50, 30));
        bS.addActionListener(e -> searchEmps());
        bA.addActionListener(e -> loadAll());
        txtSearch.addActionListener(e -> searchEmps());
        btns.add(bS); btns.add(bA);

        row.add(txtSearch, BorderLayout.CENTER);
        row.add(btns,      BorderLayout.EAST);

        tblModel = new DefaultTableModel(
            new String[]{"Emp No","Name","Department"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblResults = new JTable(tblModel);
        tblResults.setFont(new Font(FONT, Font.PLAIN, 12));
        tblResults.setRowHeight(26);
        tblResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblResults.getTableHeader().setBackground(BLUE);
        tblResults.getTableHeader().setForeground(WHITE);
        tblResults.getTableHeader().setFont(new Font(FONT, Font.BOLD, 12));
        tblResults.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBackground(LIGHT);
        top.add(lbl, BorderLayout.NORTH);
        top.add(row, BorderLayout.CENTER);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(tblResults), BorderLayout.CENTER);
        loadAll();
        return panel;
    }

    private void loadAll() {
        tblModel.setRowCount(0);
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] r = EmployeeData.records.get(i);
            tblModel.addRow(new Object[]{r[0], r[2]+" "+r[1],
                r.length > 20 ? r[20] : ""});
        }
    }

    private void searchEmps() {
        String q = txtSearch.getText().trim().toLowerCase();
        tblModel.setRowCount(0);
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] r = EmployeeData.records.get(i);
            if (r[0].contains(q) || (r[2]+" "+r[1]).toLowerCase().contains(q))
                tblModel.addRow(new Object[]{r[0], r[2]+" "+r[1],
                    r.length > 20 ? r[20] : ""});
        }
    }

    private void loadSelected() {
        int row = tblResults.getSelectedRow();
        if (row < 0) return;
        selectedEmp = EmployeeData.findEmployee(
            tblModel.getValueAt(row, 0).toString());
        if (selectedEmp != null) populateDetail(selectedEmp);
    }

    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(14, 8, 14, 14));

        // Warning banner
        JLabel warning = new JLabel(
            "<html><center>⚠ This action is permanent and cannot be undone.</center></html>",
            SwingConstants.CENTER);
        warning.setFont(new Font(FONT, Font.ITALIC, 12));
        warning.setForeground(new Color(180,100,0));
        warning.setBackground(new Color(255,248,230));
        warning.setOpaque(true);
        warning.setBorder(new CompoundBorder(
            new LineBorder(new Color(255,200,100),1,true),
            new EmptyBorder(8,12,8,12)));

        // Info card
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240),1,true),
            new EmptyBorder(18,22,18,22)));

        JLabel ct = new JLabel("Employee Details");
        ct.setFont(new Font(FONT, Font.BOLD, 15));
        ct.setForeground(BLUE);
        ct.setBorder(new EmptyBorder(0,0,8,0));

        JPanel grid = new JPanel(new GridLayout(6, 2, 10, 10));
        grid.setBackground(WHITE);
        valEmpNo    = addRow(grid, "Employee No.");
        valName     = addRow(grid, "Full Name");
        valPosition = addRow(grid, "Position");
        valDept     = addRow(grid, "Department");
        valStatus   = addRow(grid, "Status");
        valPhone    = addRow(grid, "Phone");

        card.add(ct,   BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);

        // Buttons — always at bottom
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        btnRow.setBackground(LIGHT);
        JButton btnDel  = actionBtn("🗑  Delete Employee", RED,  WHITE);
        JButton btnBack = actionBtn("← Back to Admin",    BLUE, WHITE);
        btnDel .addActionListener(e -> doDelete());
        btnBack.addActionListener(e -> nav(() -> new adminModuleUI(empNo, emp)));
        btnRow.add(btnDel); btnRow.add(btnBack);

        panel.add(warning, BorderLayout.NORTH);
        panel.add(card,    BorderLayout.CENTER);
        panel.add(btnRow,  BorderLayout.SOUTH);
        return panel;
    }

    private JLabel addRow(JPanel g, String label) {
        JLabel l = new JLabel(label + ":");
        l.setFont(new Font(FONT, Font.BOLD, 13));
        l.setForeground(MUTED);
        g.add(l);
        JLabel v = new JLabel("—");
        v.setFont(new Font(FONT, Font.PLAIN, 13));
        v.setForeground(new Color(30,30,50));
        g.add(v);
        return v;
    }

    private void populateDetail(String[] e) {
        valEmpNo   .setText(e[0]);
        valName    .setText(e[2] + " " + e[1]);
        valPosition.setText(e[11]);
        valDept    .setText(e.length > 20 ? e[20] : "—");
        valStatus  .setText(e[10]);
        valPhone   .setText(e[5]);
    }

    private void doDelete() {
        if (selectedEmp == null) {
            JOptionPane.showMessageDialog(this,
                "Select an employee from the list first.",
                "No Selection", JOptionPane.WARNING_MESSAGE); return;
        }
        if (selectedEmp[0].equals(empNo)) {
            JOptionPane.showMessageDialog(this,
                "You cannot delete your own account.",
                "Action Denied", JOptionPane.ERROR_MESSAGE); return;
        }
        int c = JOptionPane.showConfirmDialog(this,
            "Permanently delete " + selectedEmp[2] + " " + selectedEmp[1] +
            " (" + selectedEmp[0] + ")?\nThis cannot be undone.",
            "Confirm Delete", JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        if (c != JOptionPane.YES_OPTION) return;
        EmployeeData.deleteEmployee(selectedEmp[0]);
        selectedEmp = null;
        for (JLabel l : new JLabel[]{valEmpNo,valName,valPosition,valDept,valStatus,valPhone})
            l.setText("—");
        loadAll();
    }
}
