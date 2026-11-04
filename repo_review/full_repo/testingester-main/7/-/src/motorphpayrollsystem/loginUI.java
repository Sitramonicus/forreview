package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class loginUI extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JButton        btnLogin;
    private JLabel         lblError;
    private JCheckBox      chkShow;
    private int            failedAttempts = 0;
    private static final int MAX_ATTEMPTS = 5;

    private static final Color BLUE      = new Color(0, 51, 153);
    private static final Color BLUE_DARK = new Color(0, 35, 110);
    private static final Color YELLOW    = new Color(255, 204, 0);
    private static final Color WHITE     = Color.WHITE;
    private static final Color TEXT_GRAY = new Color(120, 120, 130);
    private static final Color RED_ERR   = new Color(200, 30, 30);
    private static final String F        = "Dialog";

    public loginUI() {
        setTitle("MotorPH – Login");
        setSize(860, 520);
        setMinimumSize(new Dimension(700, 440));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        add(buildLeftPanel(),  BorderLayout.WEST);
        add(buildRightPanel(), BorderLayout.CENTER);

        // Auto-focus Employee No field on open
        SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
    }

    private JPanel buildLeftPanel() {
        JPanel left = new JPanel(new GridBagLayout());
        left.setBackground(BLUE);
        left.setPreferredSize(new Dimension(340, 0));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(10, 20, 10, 20);
        gc.fill = GridBagConstraints.NONE;

        JLabel logo = new JLabel(loadIcon("/motorphpayrollsystem/images/logo.jpg", 110, 110));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(logo, gc);

        JLabel company = new JLabel("MotorPH");
        company.setFont(new Font(F, Font.BOLD, 30));
        company.setForeground(YELLOW);
        company.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(company, gc);

        JLabel tagline = new JLabel("HR Management System");
        tagline.setFont(new Font(F, Font.PLAIN, 14));
        tagline.setForeground(new Color(180, 200, 240));
        tagline.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(tagline, gc);

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(60, 90, 180));
        sep.setPreferredSize(new Dimension(220, 1));
        left.add(sep, gc);

        JLabel sub = new JLabel("<html><center>Empowering your workforce<br>with smarter tools.</center></html>");
        sub.setFont(new Font(F, Font.ITALIC, 12));
        sub.setForeground(new Color(160, 185, 230));
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        left.add(sub, gc);

        // Version tag
        JLabel ver = new JLabel("v2.0  ·  © 2025 MotorPH");
        ver.setFont(new Font(F, Font.PLAIN, 11));
        ver.setForeground(new Color(100, 130, 200));
        ver.setHorizontalAlignment(SwingConstants.CENTER);
        gc.insets = new Insets(30, 20, 10, 20);
        left.add(ver, gc);

        return left;
    }

    private JPanel buildRightPanel() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(WHITE);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(36, 40, 36, 40)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(6, 0, 6, 0);
        gc.weightx = 1.0;

        // Heading
        JLabel welcome = new JLabel("Welcome back!");
        welcome.setFont(new Font(F, Font.BOLD, 24));
        welcome.setForeground(BLUE);
        card.add(welcome, gc);

        JLabel sub = new JLabel("Sign in to your account");
        sub.setFont(new Font(F, Font.PLAIN, 13));
        sub.setForeground(TEXT_GRAY);
        gc.insets = new Insets(0, 0, 20, 0);
        card.add(sub, gc);

        // Employee No
        gc.insets = new Insets(6, 0, 2, 0);
        JLabel empLbl = new JLabel("Employee Number");
        empLbl.setFont(new Font(F, Font.BOLD, 13));
        empLbl.setForeground(new Color(60, 60, 80));
        card.add(empLbl, gc);

        gc.insets = new Insets(2, 0, 6, 0);
        txtUsername = new JTextField();
        txtUsername.setFont(new Font(F, Font.PLAIN, 14));
        txtUsername.setPreferredSize(new Dimension(280, 40));
        txtUsername.setBorder(fieldBorder(false));
        txtUsername.setToolTipText("Digits only");
        txtUsername.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if (!Character.isDigit(e.getKeyChar())) e.consume();
            }
        });
        txtUsername.addFocusListener(focusHighlight(txtUsername));
        card.add(txtUsername, gc);

        // Password label + caps lock hint
        gc.insets = new Insets(12, 0, 2, 0);
        JPanel passLabelRow = new JPanel(new BorderLayout());
        passLabelRow.setBackground(WHITE);
        JLabel passLbl = new JLabel("Password");
        passLbl.setFont(new Font(F, Font.BOLD, 13));
        passLbl.setForeground(new Color(60, 60, 80));
        JLabel capsLbl = new JLabel("");
        capsLbl.setFont(new Font(F, Font.PLAIN, 11));
        capsLbl.setForeground(new Color(200, 100, 0));
        passLabelRow.add(passLbl,  BorderLayout.WEST);
        passLabelRow.add(capsLbl,  BorderLayout.EAST);
        card.add(passLabelRow, gc);

        // Password field + show/hide toggle
        gc.insets = new Insets(2, 0, 2, 0);
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font(F, Font.PLAIN, 14));
        txtPassword.setPreferredSize(new Dimension(280, 40));
        txtPassword.setBorder(fieldBorder(false));
        txtPassword.addFocusListener(focusHighlight(txtPassword));
        txtPassword.addActionListener(e -> doLogin());

        // Caps Lock detection
        txtPassword.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                boolean caps = Toolkit.getDefaultToolkit()
                    .getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
                capsLbl.setText(caps ? "Caps Lock is ON" : "");
            }
        });

        // Show/hide password toggle
        chkShow = new JCheckBox("Show");
        chkShow.setFont(new Font(F, Font.PLAIN, 12));
        chkShow.setBackground(WHITE);
        chkShow.setFocusPainted(false);
        chkShow.addActionListener(e -> {
            if (chkShow.isSelected()) {
                txtPassword.setEchoChar((char) 0);
            } else {
                txtPassword.setEchoChar('\u2022');
            }
        });

        JPanel passRow = new JPanel(new BorderLayout(6, 0));
        passRow.setBackground(WHITE);
        passRow.add(txtPassword, BorderLayout.CENTER);
        passRow.add(chkShow,     BorderLayout.EAST);
        card.add(passRow, gc);

        // Error label
        gc.insets = new Insets(2, 0, 2, 0);
        lblError = new JLabel(" ");
        lblError.setFont(new Font(F, Font.PLAIN, 12));
        lblError.setForeground(RED_ERR);
        card.add(lblError, gc);

        // Login button with rounded corners
        gc.insets = new Insets(12, 0, 6, 0);
        btnLogin = new JButton("LOGIN") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font(F, Font.BOLD, 15));
        btnLogin.setBackground(BLUE);
        btnLogin.setForeground(WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setOpaque(false);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setPreferredSize(new Dimension(280, 44));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btnLogin.setBackground(BLUE_DARK); }
            public void mouseExited (MouseEvent e) { btnLogin.setBackground(BLUE); }
        });
        btnLogin.addActionListener(e -> doLogin());
        card.add(btnLogin, gc);

        gc.insets = new Insets(10, 0, 0, 0);
        JLabel footer = new JLabel("© 2025 MotorPH. All rights reserved.");
        footer.setFont(new Font(F, Font.PLAIN, 11));
        footer.setForeground(new Color(180, 185, 200));
        footer.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(footer, gc);

        right.add(card);
        return right;
    }

    private void doLogin() {
        // Lockout check
        if (failedAttempts >= MAX_ATTEMPTS) {
            showError("Account locked after " + MAX_ATTEMPTS +
                " failed attempts. Please restart the app.");
            btnLogin.setEnabled(false);
            return;
        }

        String empNo   = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (empNo.isEmpty()) {
            showError("Please enter your Employee Number.");
            txtUsername.requestFocus(); return;
        }
        if (password.isEmpty()) {
            showError("Please enter your Password.");
            txtPassword.requestFocus(); return;
        }

        // Show loading state
        btnLogin.setText("Signing in...");
        btnLogin.setEnabled(false);

        new SwingWorker<String[], Void>() {
            protected String[] doInBackground() {
                if (EmployeeData.records == null) return null;
                for (int i = 1; i < EmployeeData.records.size(); i++) {
                    String[] row = EmployeeData.records.get(i);
                    if (row != null && row.length > 0 &&
                        row[0].trim().equalsIgnoreCase(empNo)) return row;
                }
                return null;
            }
            protected void done() {
                btnLogin.setText("LOGIN");
                btnLogin.setEnabled(true);
                try {
                    String[] emp = get();
                    if (emp == null) {
                        failedAttempts++;
                        showError("Employee not found. (" + failedAttempts +
                            "/" + MAX_ATTEMPTS + " attempts)");
                        txtUsername.requestFocus();
                    } else if (emp.length <= 19 || !emp[19].equals(password)) {
                        failedAttempts++;
                        showError("Incorrect password. (" + failedAttempts +
                            "/" + MAX_ATTEMPTS + " attempts)");
                        txtPassword.setText("");
                        txtPassword.requestFocus();
                    } else {
                        failedAttempts = 0;
                        lblError.setText(" ");
                        new DashboardUI(empNo, emp).setVisible(true);
                        dispose();
                    }
                } catch (Exception ex) {
                    showError("Login error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        final int[] count = {0};
        final int origX = getLocation().x;
        Timer shake = new Timer(30, null);
        shake.addActionListener(e -> {
            setLocation(origX + (count[0]++ % 2 == 0 ? 6 : -6), getLocation().y);
            if (count[0] > 8) { setLocation(origX, getLocation().y); shake.stop(); }
        });
        shake.start();
    }

    private Border fieldBorder(boolean error) {
        return new CompoundBorder(
            new LineBorder(error ? new Color(200, 50, 50) :
                new Color(200, 210, 230), 1, true),
            new EmptyBorder(6, 12, 6, 12));
    }

    private FocusListener focusHighlight(JComponent comp) {
        return new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                comp.setBorder(new CompoundBorder(
                    new LineBorder(BLUE, 2, true),
                    new EmptyBorder(5, 11, 5, 11)));
            }
            public void focusLost(FocusEvent e) {
                comp.setBorder(fieldBorder(false));
            }
        };
    }

    private ImageIcon loadIcon(String path, int w, int h) {
        try {
            java.net.URL u = getClass().getResource(path);
            if (u == null) return new ImageIcon();
            return new ImageIcon(new ImageIcon(u).getImage()
                .getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) { return new ImageIcon(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmployeeData.loadEmployeeData();
            new loginUI().setVisible(true);
        });
    }
}
