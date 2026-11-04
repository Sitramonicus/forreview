package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class adminUpdateProfile extends UIBase {

    private String[] selectedEmp;
    private JTextField txtSearch;
    private JTable tblResults;
    private DefaultTableModel tblModel;

    // All form fields
    private JTextField txtEmpNo, txtFirstName, txtLastName, txtBirthday;
    private JTextField txtAddress, txtPhoneNo, txtPosition, txtSupervisor;
    private JTextField txtSSSNo, txtPhilHealthNo, txtTIN, txtPagibigNo;
    private JTextField txtBasicSal, txtRiceSubs, txtPhoneAllow, txtClothingAllow;
    private JTextField txtSemiMonthly, txtHourlyRate, txtDepartment;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbStatus;

    // Prevent recursive salary computation
    private boolean computing = false;

    public adminUpdateProfile(String empNo, String[] emp) {
        super("MotorPH – Update Employee", empNo, emp);

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

        // Dynamic table refresh when data changes
        EmployeeData.addChangeListener(() ->
            SwingUtilities.invokeLater(() -> { if (tblModel != null) loadAll(); }));
    }

    // =========================================================================
    // MAIN LAYOUT
    // =========================================================================
    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("Update Employee"), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildSearchPanel(), buildFormPanel());
        split.setDividerLocation(300);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setResizeWeight(0.25);

        main.add(split, BorderLayout.CENTER);
        return main;
    }

    // =========================================================================
    // LEFT: SEARCH PANEL
    // =========================================================================
    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(14, 14, 14, 8));

        JLabel lbl = new JLabel("Search Employee");
        lbl.setFont(new Font(FONT, Font.BOLD, 14));
        lbl.setForeground(BLUE);

        // Search row
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setBackground(LIGHT);
        txtSearch = new JTextField();
        txtSearch.setFont(new Font(FONT, Font.PLAIN, 13));
        txtSearch.setToolTipText("Search by name or employee number");

        JPanel btns = new JPanel(new GridLayout(1, 2, 4, 0));
        btns.setBackground(LIGHT);
        JButton bS = actionBtn("Search", BLUE, WHITE);
        JButton bA = actionBtn("All",    MUTED, WHITE);
        bS.setPreferredSize(new Dimension(80, 30));
        bA.setPreferredSize(new Dimension(50, 30));
        bS.addActionListener(e -> searchEmps());
        bA.addActionListener(e -> loadAll());
        txtSearch.addActionListener(e -> searchEmps());
        btns.add(bS); btns.add(bA);

        row.add(txtSearch, BorderLayout.CENTER);
        row.add(btns,      BorderLayout.EAST);

        // Results table
        tblModel = new DefaultTableModel(
            new String[]{"Emp No", "Name", "Position"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblResults = new JTable(tblModel);
        tblResults.setFont(new Font(FONT, Font.PLAIN, 12));
        tblResults.setRowHeight(26);
        tblResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblResults.setShowGrid(false);
        tblResults.setIntercellSpacing(new Dimension(0, 0));
        tblResults.getTableHeader().setBackground(BLUE);
        tblResults.getTableHeader().setForeground(WHITE);
        tblResults.getTableHeader().setFont(new Font(FONT, Font.BOLD, 12));
        tblResults.getTableHeader().setPreferredSize(new Dimension(0, 30));

        // Alternating rows
        DefaultTableCellRenderer rdr = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                if (!sel) setBackground(row % 2 == 0 ? WHITE : LIGHT);
                return this;
            }
        };
        for (int i = 0; i < 3; i++)
            tblResults.getColumnModel().getColumn(i).setCellRenderer(rdr);

        tblResults.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });

        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setBackground(LIGHT);
        top.add(lbl, BorderLayout.NORTH);
        top.add(row, BorderLayout.CENTER);

        panel.add(top,                       BorderLayout.NORTH);
        panel.add(new JScrollPane(tblResults), BorderLayout.CENTER);

        loadAll();
        return panel;
    }

    private void loadAll() {
        tblModel.setRowCount(0);
        if (EmployeeData.records == null) return;
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] r = EmployeeData.records.get(i);
            if (r != null && r.length > 11)
                tblModel.addRow(new Object[]{r[0], r[2] + " " + r[1], r[11]});
        }
    }

    private void searchEmps() {
        String q = txtSearch.getText().trim().toLowerCase();
        tblModel.setRowCount(0);
        if (EmployeeData.records == null) return;
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] r = EmployeeData.records.get(i);
            if (r == null || r.length < 3) continue;
            String name = (r[2] + " " + r[1]).toLowerCase();
            if (r[0].contains(q) || name.contains(q))
                tblModel.addRow(new Object[]{r[0], r[2] + " " + r[1], r[11]});
        }
    }

    private void loadSelected() {
        int row = tblResults.getSelectedRow();
        if (row < 0) return;
        String id = tblModel.getValueAt(row, 0).toString();
        // Silent lookup — no "not found" popup
        String[] found = null;
        if (EmployeeData.records != null) {
            for (int i = 1; i < EmployeeData.records.size(); i++) {
                String[] r = EmployeeData.records.get(i);
                if (r != null && r[0].trim().equalsIgnoreCase(id)) {
                    found = r; break;
                }
            }
        }
        if (found != null) {
            selectedEmp = found;
            populate(found);
        }
    }

    // =========================================================================
    // RIGHT: FORM PANEL
    // =========================================================================
    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(LIGHT);
        panel.setBorder(new EmptyBorder(14, 8, 0, 14));

        // Two-column form
        JPanel form = new JPanel(new GridLayout(1, 2, 12, 0));
        form.setBackground(LIGHT);
        form.add(buildPersonalSection());
        form.add(buildCompensationSection());

        // Always-visible bottom bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottom.setBackground(LIGHT);
        bottom.setBorder(new MatteBorder(1, 0, 0, 0, new Color(220, 225, 240)));

        JLabel sl = new JLabel("Status:");
        sl.setFont(new Font(FONT, Font.BOLD, 13));
        cmbStatus = new JComboBox<>(
            new String[]{"Active", "On Leave", "Resigned", "Terminated"});
        cmbStatus.setFont(new Font(FONT, Font.PLAIN, 13));

        JButton btnSave = actionBtn("Save Changes", GREEN, WHITE);
        JButton btnBack = actionBtn("Back",         BLUE,  WHITE);
        btnSave.addActionListener(e -> updateEmp());
        btnBack.addActionListener(e -> nav(() -> new adminModuleUI(empNo, emp)));

        bottom.add(sl); bottom.add(cmbStatus);
        bottom.add(Box.createHorizontalStrut(16));
        bottom.add(btnSave); bottom.add(btnBack);

        panel.add(form,   BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ── Personal section ──────────────────────────────────────────────────────
    private JPanel buildPersonalSection() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(14, 16, 14, 16)));

        JLabel hdr = new JLabel("Personal Details");
        hdr.setFont(new Font(FONT, Font.BOLD, 13));
        hdr.setForeground(BLUE);
        hdr.setBorder(new EmptyBorder(0, 0, 6, 0));

        JPanel grid = new JPanel(new GridLayout(10, 2, 6, 6));
        grid.setBackground(WHITE);

        txtEmpNo      = addRow(grid, "Employee No.",  false); txtEmpNo.setEditable(false);
        txtFirstName  = addRow(grid, "First Name *",  false);
        txtLastName   = addRow(grid, "Last Name *",   false);

        // Birthday with date picker
        JLabel bdLbl = fieldLabel("Birthday");
        grid.add(bdLbl);
        txtBirthday = new JTextField();
        styleField(txtBirthday);
        grid.add(DatePicker.createDateField(card, txtBirthday, ""));

        txtAddress    = addRow(grid, "Address",       false);
        txtPhoneNo    = addRow(grid, "Phone No.",     false);
        txtPosition   = addRow(grid, "Position *",   false);
        txtSupervisor = addRow(grid, "Supervisor",    false);
        txtDepartment = addRow(grid, "Department",    false);
        // Password with show/hide toggle
        grid.add(fieldLabel("Password"));
        JPasswordField pwField = new JPasswordField();
        styleField(pwField);
        txtPassword = pwField;
        JButton togglePw = new JButton("Show");
        togglePw.setFont(new Font(FONT, Font.PLAIN, 10));
        togglePw.setFocusPainted(false);
        togglePw.setBorderPainted(false);
        togglePw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePw.addActionListener(e -> {
            if (pwField.getEchoChar() == 0) {
                pwField.setEchoChar('•'); togglePw.setText("Show");
            } else {
                pwField.setEchoChar((char)0); togglePw.setText("Hide");
            }
        });
        JPanel pwWrap = new JPanel(new BorderLayout(2, 0));
        pwWrap.setBackground(WHITE);
        pwWrap.add(pwField, BorderLayout.CENTER);
        pwWrap.add(togglePw, BorderLayout.EAST);
        grid.add(pwWrap);

        card.add(hdr,  BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    // ── Compensation section ──────────────────────────────────────────────────
    private JPanel buildCompensationSection() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(14, 16, 14, 16)));

        JLabel hdr = new JLabel("Government IDs & Compensation");
        hdr.setFont(new Font(FONT, Font.BOLD, 13));
        hdr.setForeground(BLUE);
        hdr.setBorder(new EmptyBorder(0, 0, 6, 0));

        JPanel grid = new JPanel(new GridLayout(10, 2, 6, 6));
        grid.setBackground(WHITE);

        txtSSSNo        = addRow(grid, "SSS No.",           false);
        txtPhilHealthNo = addRow(grid, "PhilHealth No.",    false);
        txtTIN          = addRow(grid, "TIN",               false);
        txtPagibigNo    = addRow(grid, "Pag-IBIG No.",      false);

        // Basic Salary — triggers auto-compute
        txtBasicSal = addRow(grid, "Basic Salary *", false);
        txtBasicSal.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { computeFromBasic(); }
        });

        txtRiceSubs      = addRow(grid, "Rice Subsidy",      false);
        txtPhoneAllow    = addRow(grid, "Phone Allowance",   false);
        txtClothingAllow = addRow(grid, "Clothing Allow.",   false);

        // Semi-Monthly — triggers auto-compute
        txtSemiMonthly = addRow(grid, "Semi-Monthly Rate", false);
        txtSemiMonthly.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { computeFromSemi(); }
        });

        // Hourly Rate — triggers auto-compute
        txtHourlyRate = addRow(grid, "Hourly Rate *", false);
        txtHourlyRate.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) { computeFromHourly(); }
        });

        card.add(hdr,  BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    // =========================================================================
    // BIDIRECTIONAL SALARY COMPUTATION
    // =========================================================================
    private void computeFromBasic() {
        if (computing) return;
        computing = true;
        try {
            double basic = parseField(txtBasicSal);
            if (basic > 0) {
                txtSemiMonthly.setText(fmt2(EmployeeData.computeSemiMonthlyFromBasic(basic)));
                txtHourlyRate .setText(fmt4(EmployeeData.computeHourlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private void computeFromSemi() {
        if (computing) return;
        computing = true;
        try {
            double semi = parseField(txtSemiMonthly);
            if (semi > 0) {
                double basic = EmployeeData.semiMonthlyToBasic(semi);
                txtBasicSal  .setText(fmt2(basic));
                txtHourlyRate.setText(fmt4(EmployeeData.computeHourlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private void computeFromHourly() {
        if (computing) return;
        computing = true;
        try {
            double hourly = parseField(txtHourlyRate);
            if (hourly > 0) {
                double basic = EmployeeData.computeBasicFromHourly(hourly);
                txtBasicSal   .setText(fmt2(basic));
                txtSemiMonthly.setText(fmt2(EmployeeData.computeSemiMonthlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private double parseField(JTextField f) {
        if (f == null) return 0;
        String v = f.getText().trim().replace(",", "");
        if (v.isEmpty()) return 0;
        return Double.parseDouble(v);
    }

    private String fmt2(double v) { return String.format("%.2f", v); }
    private String fmt4(double v) { return String.format("%.4f", v); }

    // =========================================================================
    // POPULATE FIELDS
    // =========================================================================
    private void populate(String[] e) {
        set(txtEmpNo,         e[0]);
        set(txtLastName,      e[1]);
        set(txtFirstName,     e[2]);
        set(txtBirthday,      e[3]);
        set(txtAddress,       e[4]);
        set(txtPhoneNo,       e[5]);
        set(txtSSSNo,         e[6]);
        set(txtPhilHealthNo,  e[7]);
        set(txtTIN,           e[8]);
        set(txtPagibigNo,     e[9]);
        set(txtPosition,      e[11]);
        set(txtSupervisor,    e[12]);
        set(txtBasicSal,      e[13]);
        set(txtRiceSubs,      e[14]);
        set(txtPhoneAllow,    e[15]);
        set(txtClothingAllow, e[16]);
        set(txtSemiMonthly,   e[17]);
        set(txtHourlyRate,    e[18]);
        if (txtPassword != null) txtPassword.setText(e[19]);
        set(txtDepartment,    e.length > 20 ? e[20] : "");
        if (cmbStatus != null) cmbStatus.setSelectedItem(e[10]);
    }

    private void set(JTextField f, String v) {
        if (f != null) f.setText(v == null ? "" : v);
    }

    // =========================================================================
    // SAVE
    // =========================================================================
    private void updateEmp() {
        if (selectedEmp == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an employee from the list first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fn = get(txtFirstName);
        String ln = get(txtLastName);
        String ps = get(txtPosition);

        String valError = EmployeeData.validateUpdateEmployee(
            fn, ln, get(txtBirthday), get(txtPhoneNo), ps,
            get(txtSSSNo), get(txtPhilHealthNo), get(txtTIN), get(txtPagibigNo));
        if (valError != null) {
            JOptionPane.showMessageDialog(this, valError,
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double bs, rs, pa, ca, sm, hr;
        try {
            bs = parseFieldSafe(txtBasicSal,      "Basic Salary");
            rs = parseFieldSafe(txtRiceSubs,      "Rice Subsidy");
            pa = parseFieldSafe(txtPhoneAllow,    "Phone Allowance");
            ca = parseFieldSafe(txtClothingAllow, "Clothing Allowance");
            sm = parseFieldSafe(txtSemiMonthly,   "Semi-Monthly Rate");
            hr = parseFieldSafe(txtHourlyRate,    "Hourly Rate");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Invalid Number", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Auto-compute missing rates from basic
        if (bs > 0) {
            if (sm <= 0) sm = EmployeeData.computeSemiMonthlyFromBasic(bs);
            if (hr <= 0) hr = EmployeeData.computeHourlyFromBasic(bs);
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Save changes for " + fn + " " + ln + "?",
            "Confirm Update", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        EmployeeData.updateProfile(
            get(txtEmpNo), fn, ln,
            get(txtBirthday), get(txtAddress), get(txtPhoneNo),
            ps, cmbStatus.getSelectedItem().toString(),
            get(txtSupervisor),
            get(txtSSSNo), get(txtPhilHealthNo),
            get(txtTIN), get(txtPagibigNo),
            bs, rs, pa, ca, sm, hr,
            txtPassword != null ? new String(txtPassword.getPassword()) : "",
            get(txtDepartment));
    }

    private String get(JTextField f) {
        return f == null ? "" : f.getText().trim();
    }

    private double parseFieldSafe(JTextField f, String name) {
        if (f == null) return 0;
        String v = f.getText().trim().replace(",", "");
        if (v.isEmpty()) return 0;
        try { return Double.parseDouble(v); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a valid number.");
        }
    }

    // =========================================================================
    // FIELD BUILDER HELPERS
    // =========================================================================
    private JTextField addRow(JPanel grid, String label, boolean bold) {
        grid.add(fieldLabel(label));
        JTextField tf = new JTextField();
        styleField(tf);
        grid.add(tf);
        return tf;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT, Font.PLAIN, 11));
        l.setForeground(MUTED);
        return l;
    }

    private void styleField(JTextField tf) {
        tf.setFont(new Font(FONT, Font.PLAIN, 12));
        tf.setBorder(new CompoundBorder(
            new LineBorder(new Color(210, 215, 230), 1, true),
            new EmptyBorder(3, 7, 3, 7)));
    }
}
