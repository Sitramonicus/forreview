package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class addNewEmp extends UIBase {

    private JTextField txtEmpNo, txtFirstName, txtLastName, txtBirthday;
    private JTextField txtAddress, txtPhoneNo, txtPosition, txtSupervisor;
    private JTextField txtSSSNo, txtPhilHealthNo, txtTIN, txtPagibigNo;
    private JTextField txtBasicSal, txtRiceSubs, txtPhoneAllow, txtClothingAllow;
    private JTextField txtSemiMonthly, txtHourlyRate, txtDepartment;
    private JPasswordField txtPassword;
    private JComboBox<String> cmbStatus;

    public addNewEmp(String empNo, String[] emp) {
        super("MotorPH – Add New Employee", empNo, emp);
        activeScreen = "Admin";

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

        // Auto-generate next Employee No after UI is built
        SwingUtilities.invokeLater(() -> {
            if (txtEmpNo != null && txtEmpNo.getText().trim().isEmpty()) {
                int maxNo = 10000;
                if (EmployeeData.records != null) {
                    for (int i = 1; i < EmployeeData.records.size(); i++) {
                        try {
                            int no = Integer.parseInt(EmployeeData.records.get(i)[0].trim());
                            if (no > maxNo) maxNo = no;
                        } catch (Exception ignored) {}
                    }
                }
                txtEmpNo.setText(String.valueOf(maxNo + 1));
            }
        });
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("Add New Employee"), BorderLayout.NORTH);

        // Two-column form
        JPanel form = new JPanel(new GridLayout(1, 2, 14, 0));
        form.setBackground(LIGHT);

        form.add(buildSection("Personal Information", new String[][]{
            {"Employee No. *", "txtEmpNo"}, {"First Name *","txtFirstName"},
            {"Last Name *","txtLastName"},  {"Birthday *","txtBirthday"},
            {"Address *","txtAddress"},     {"Phone No. *","txtPhoneNo"},
            {"Position *","txtPosition"},   {"Supervisor","txtSupervisor"},
            {"Department *","txtDepartment"}, {"Password *","txtPassword"},
        }));
        form.add(buildSection("Compensation & Government IDs", new String[][]{
            {"SSS No.","txtSSSNo"},               {"PhilHealth No.","txtPhilHealthNo"},
            {"TIN","txtTIN"},                     {"Pag-IBIG No.","txtPagibigNo"},
            {"Basic Salary *","txtBasicSal"},     {"Rice Subsidy","txtRiceSubs"},
            {"Phone Allowance","txtPhoneAllow"},  {"Clothing Allow.","txtClothingAllow"},
            {"Semi-Monthly Rate","txtSemiMonthly"}, {"Hourly Rate *","txtHourlyRate"},
        }));

        // Bottom bar — always visible
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        bottom.setBackground(LIGHT);
        bottom.setBorder(new MatteBorder(1,0,0,0, new Color(220,225,240)));

        JLabel sl = new JLabel("Status *:");
        sl.setFont(new Font(FONT, Font.BOLD, 13));
        cmbStatus = new JComboBox<>(new String[]{"Active","On Leave","Resigned","Terminated"});
        cmbStatus.setFont(new Font(FONT, Font.PLAIN, 13));

        JButton btnSave  = actionBtn("💾  Save Employee", GREEN, WHITE);
        JButton btnClear = actionBtn("🗑  Clear Fields",  MUTED, WHITE);
        JButton btnBack  = actionBtn("← Back to Admin",  BLUE,  WHITE);

        btnSave .addActionListener(e -> saveEmployee());
        btnClear.addActionListener(e -> clearFields());
        btnBack .addActionListener(e -> nav(() -> new adminModuleUI(empNo, emp)));

        bottom.add(sl); bottom.add(cmbStatus);
        bottom.add(Box.createHorizontalStrut(16));
        bottom.add(btnSave); bottom.add(btnClear); bottom.add(btnBack);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setBackground(LIGHT);
        body.setBorder(new EmptyBorder(14, 20, 0, 20));
        body.add(form,   BorderLayout.CENTER);
        body.add(bottom, BorderLayout.SOUTH);

        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildSection(String title, String[][] fields) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(14, 16, 14, 16)));

        JLabel hdr = new JLabel(title);
        hdr.setFont(new Font(FONT, Font.BOLD, 13));
        hdr.setForeground(BLUE);
        hdr.setBorder(new EmptyBorder(0,0,6,0));

        JPanel grid = new JPanel(new GridLayout(fields.length, 2, 6, 6));
        grid.setBackground(WHITE);

        for (String[] f : fields) {
            JLabel l = new JLabel(f[0]);
            l.setFont(new Font(FONT, Font.PLAIN, 12));
            l.setForeground(MUTED);
            grid.add(l);

            JTextField tf = new JTextField();
            tf.setFont(new Font(FONT, Font.PLAIN, 12));
            tf.setBorder(new CompoundBorder(
                new LineBorder(new Color(210,215,230),1,true),
                new EmptyBorder(3,7,3,7)));
            grid.add(tf);

            switch(f[1]) {
                case "txtEmpNo":         txtEmpNo         = tf; break;
                case "txtFirstName":     txtFirstName     = tf; break;
                case "txtLastName":      txtLastName      = tf; break;
                case "txtBirthday":
                    txtBirthday = tf;
                    // Replace plain field with calendar picker in the grid
                    // Remove the plain field we just added and put a date panel instead
                    grid.remove(tf);
                    grid.add(DatePicker.createDateField(this, txtBirthday, ""));
                    break;
                case "txtAddress":       txtAddress       = tf; break;
                case "txtPhoneNo":       txtPhoneNo       = tf; break;
                case "txtPosition":      txtPosition      = tf; break;
                case "txtSupervisor":    txtSupervisor    = tf; break;
                case "txtDepartment":    txtDepartment    = tf; break;
                case "txtPassword":
                    grid.remove(tf);
                    JPasswordField pf = new JPasswordField();
                    pf.setFont(new Font(FONT, Font.PLAIN, 12));
                    pf.setBorder(new CompoundBorder(
                        new LineBorder(new Color(210,215,230),1,true),
                        new EmptyBorder(3,7,3,7)));
                    txtPassword = pf;
                    JButton togglePw = new JButton("Show");
                    togglePw.setFont(new Font(FONT, Font.PLAIN, 10));
                    togglePw.setFocusPainted(false);
                    togglePw.setBorderPainted(false);
                    togglePw.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    togglePw.addActionListener(ev -> {
                        if (pf.getEchoChar() == 0) {
                            pf.setEchoChar('•'); togglePw.setText("Show");
                        } else {
                            pf.setEchoChar((char)0); togglePw.setText("Hide");
                        }
                    });
                    JPanel pwWrap = new JPanel(new BorderLayout(2, 0));
                    pwWrap.setBackground(WHITE);
                    pwWrap.add(pf, BorderLayout.CENTER);
                    pwWrap.add(togglePw, BorderLayout.EAST);
                    grid.add(pwWrap);
                    break;
                case "txtSSSNo":         txtSSSNo         = tf; break;
                case "txtPhilHealthNo":  txtPhilHealthNo  = tf; break;
                case "txtTIN":           txtTIN           = tf; break;
                case "txtPagibigNo":     txtPagibigNo     = tf; break;
                case "txtBasicSal":
                    txtBasicSal = tf;
                    tf.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusLost(java.awt.event.FocusEvent e) { onBasicChanged(); }
                    });
                    break;
                case "txtSemiMonthly":
                    txtSemiMonthly = tf;
                    tf.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusLost(java.awt.event.FocusEvent e) { onSemiMonthlyChanged(); }
                    });
                    break;
                case "txtHourlyRate":
                    txtHourlyRate = tf;
                    tf.addFocusListener(new java.awt.event.FocusAdapter() {
                        public void focusLost(java.awt.event.FocusEvent e) { onHourlyChanged(); }
                    });
                    break;
                case "txtRiceSubs":      txtRiceSubs      = tf; break;
                case "txtPhoneAllow":    txtPhoneAllow    = tf; break;
                case "txtClothingAllow": txtClothingAllow = tf; break;
            }
        }
        card.add(hdr, BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private boolean computing = false;

    private void onBasicChanged() {
        if (computing) return; computing = true;
        try {
            double basic = pd2(txtBasicSal);
            if (basic > 0) {
                txtSemiMonthly.setText(String.format("%.2f",
                    EmployeeData.computeSemiMonthlyFromBasic(basic)));
                txtHourlyRate .setText(String.format("%.4f",
                    EmployeeData.computeHourlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private void onSemiMonthlyChanged() {
        if (computing) return; computing = true;
        try {
            double semi = pd2(txtSemiMonthly);
            if (semi > 0) {
                double basic = EmployeeData.semiMonthlyToBasic(semi);
                txtBasicSal  .setText(String.format("%.2f", basic));
                txtHourlyRate.setText(String.format("%.4f",
                    EmployeeData.computeHourlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private void onHourlyChanged() {
        if (computing) return; computing = true;
        try {
            double hourly = pd2(txtHourlyRate);
            if (hourly > 0) {
                double basic = EmployeeData.computeBasicFromHourly(hourly);
                txtBasicSal   .setText(String.format("%.2f", basic));
                txtSemiMonthly.setText(String.format("%.2f",
                    EmployeeData.computeSemiMonthlyFromBasic(basic)));
            }
        } catch (Exception ignored) {}
        computing = false;
    }

    private double pd2(JTextField f) {
        if (f == null) return 0;
        String v = f.getText().trim().replace(",","");
        return v.isEmpty() ? 0 : Double.parseDouble(v);
    }


    private void saveEmployee() {
        String id = txtEmpNo.getText().trim();
        String fn = txtFirstName.getText().trim();
        String ln = txtLastName.getText().trim();
        String bd = txtBirthday.getText().trim();
        String ad = txtAddress.getText().trim();
        String ph = txtPhoneNo.getText().trim();
        String ps = txtPosition.getText().trim();
        String dp = txtDepartment.getText().trim();
        String pw = new String(txtPassword.getPassword()).trim();

        // Highlight empty required fields in red
        boolean hasError = false;
        for (JTextField[] pair : new JTextField[][]{{txtEmpNo},{txtFirstName},{txtLastName},
                {txtBirthday},{txtAddress},{txtPhoneNo},{txtPosition},{txtDepartment},{txtPassword}}) {
            if (pair[0] != null && pair[0].getText().trim().isEmpty()) {
                pair[0].setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(200,38,38),2,true),
                    new javax.swing.border.EmptyBorder(3,7,3,7)));
                hasError = true;
            } else if (pair[0] != null) {
                pair[0].setBorder(new javax.swing.border.CompoundBorder(
                    new javax.swing.border.LineBorder(new Color(210,215,230),1,true),
                    new javax.swing.border.EmptyBorder(3,7,3,7)));
            }
        }

        // Centralized validation from EmployeeData
        String validationError = EmployeeData.validateNewEmployee(
            id, fn, ln, bd, ad, ph, ps, dp, pw,
            txtBasicSal.getText(), txtHourlyRate.getText(),
            txtSSSNo.getText().trim(), txtPhilHealthNo.getText().trim(),
            txtTIN.getText().trim(), txtPagibigNo.getText().trim());
        if (validationError != null || hasError) {
            if (validationError != null)
                JOptionPane.showMessageDialog(this, validationError,
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double bs, rs, pa, ca, sm, hr;
        try {
            bs = pd(txtBasicSal.getText(),    "Basic Salary");
            rs = pd(txtRiceSubs.getText(),    "Rice Subsidy");
            pa = pd(txtPhoneAllow.getText(),  "Phone Allowance");
            ca = pd(txtClothingAllow.getText(),"Clothing Allowance");
            sm = pd(txtSemiMonthly.getText(), "Semi-Monthly Rate");
            hr = pd(txtHourlyRate.getText(),  "Hourly Rate");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                "Invalid Number", JOptionPane.WARNING_MESSAGE); return;
        }
        // Auto-compute rates from basic if still blank
        if (bs > 0) {
            if (sm <= 0) sm = EmployeeData.computeSemiMonthlyFromBasic(bs);
            if (hr <= 0) hr = EmployeeData.computeHourlyFromBasic(bs);
        }
        EmployeeData.addEmployee(id, fn, ln, bd, ad, ph, ps,
            cmbStatus.getSelectedItem().toString(),
            txtSupervisor.getText().trim(),
            txtSSSNo.getText().trim(), txtPhilHealthNo.getText().trim(),
            txtTIN.getText().trim(), txtPagibigNo.getText().trim(),
            bs, rs, pa, ca, sm, hr, pw, dp);
        Toast.success(this, "Employee " + fn + " " + ln + " added successfully!");
        clearFields();
    }

    private double pd(String v, String n) {
        try { return v.trim().isEmpty() ? 0 :
            Double.parseDouble(v.trim().replace(",","")); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(n + " must be a valid number."); }
    }

    private void clearFields() {
        for (JTextField f : new JTextField[]{
            txtEmpNo,txtFirstName,txtLastName,txtBirthday,txtAddress,
            txtPhoneNo,txtPosition,txtSupervisor,txtDepartment,
            txtSSSNo,txtPhilHealthNo,txtTIN,txtPagibigNo,
            txtBasicSal,txtRiceSubs,txtPhoneAllow,txtClothingAllow,
            txtSemiMonthly,txtHourlyRate})
            if (f != null) f.setText("");
        if (txtPassword != null) txtPassword.setText("");
        cmbStatus.setSelectedIndex(0);
    }
}
