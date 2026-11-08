package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class DashboardUI extends UIBase {

    public DashboardUI(String empNo, String[] emp) {
        super("MotorPH – Dashboard", empNo, emp);
        activeScreen = "Main Menu";

        ActionListener[] nav = {
            e -> nav(() -> new DashboardUI(empNo, emp)),
            e -> nav(() -> new ProfileUI(empNo, emp)),
            e -> nav(() -> new PayrollUI(empNo, emp)),
            e -> nav(() -> new AttendanceUI(empNo, emp)),
            e -> {
                if (!EmployeeData.isHR(emp)) {
                    JOptionPane.showMessageDialog(this,
                        "Access Denied.\nOnly HR can access Admin.",
                        "Unauthorized", JOptionPane.ERROR_MESSAGE); return;
                }
                nav(() -> new adminModuleUI(empNo, emp));
            },
            e -> nav(() -> new loginUI()),
        };

        add(buildSidebar(empNo, emp, nav), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);
        main.add(pageHeader("MOTORPH PAYROLL SYSTEM"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(LIGHT);
        body.setBorder(new EmptyBorder(24, 32, 24, 32));

        // ── Welcome card ──────────────────────────────────────────────────────
        JPanel welcomeCard = new JPanel(new GridBagLayout());
        welcomeCard.setBackground(WHITE);
        welcomeCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(20, 28, 20, 28)));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(4, 0, 4, 0);
        gc.fill = GridBagConstraints.NONE;

        JLabel avatar = new JLabel(icon(
            "/motorphpayrollsystem/images/profileicon_dash.jpg", 72, 72));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        welcomeCard.add(avatar, gc);

        // Time-based greeting
        int hour = java.time.LocalTime.now().getHour();
        String greeting = hour < 12 ? "Good morning," :
                          hour < 17 ? "Good afternoon," : "Good evening,";
        JLabel welcome = new JLabel(greeting);
        welcome.setFont(new Font(FONT, Font.PLAIN, 15));
        welcome.setForeground(new Color(100, 110, 130));
        welcomeCard.add(welcome, gc);

        JLabel name = new JLabel(emp[2] + " " + emp[1] + "!");
        name.setFont(new Font(FONT, Font.BOLD, 26));
        name.setForeground(BLUE);
        welcomeCard.add(name, gc);

        JLabel posLbl = new JLabel(emp[11] + "   ·   " + emp[20]);
        posLbl.setFont(new Font(FONT, Font.PLAIN, 13));
        posLbl.setForeground(MUTED);
        welcomeCard.add(posLbl, gc);

        // ── Stats row (only for HR) ───────────────────────────────────────────
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 14, 0));
        statsRow.setBackground(LIGHT);

        if (EmployeeData.isHR(emp)) {
            int total  = EmployeeData.getTotalEmployees();
            int active = EmployeeData.getActiveEmployees();
            int depts  = countDepartments();

            statsRow.add(statCard("Total Employees", String.valueOf(total),
                BLUE, "/motorphpayrollsystem/images/admin.jpg"));
            statsRow.add(statCard("Active Employees", String.valueOf(active),
                GREEN, "/motorphpayrollsystem/images/userprofile.jpg"));
            statsRow.add(statCard("Departments", String.valueOf(depts),
                PURPLE, "/motorphpayrollsystem/images/mainmenu.jpg"));
        } else {
            // Non-HR: load attendance off the EDT so the dashboard appears instantly
            JLabel daysVal = new JLabel("…");
            daysVal.setFont(new Font(FONT, Font.BOLD, 28));
            daysVal.setForeground(BLUE);

            JLabel otVal = new JLabel("…");
            otVal.setFont(new Font(FONT, Font.BOLD, 28));
            otVal.setForeground(GREEN);

            statsRow.add(statCard("Days Present",   daysVal, BLUE,
                "/motorphpayrollsystem/images/attendance.jpg"));
            statsRow.add(statCard("Overtime Hours", otVal,   GREEN,
                "/motorphpayrollsystem/images/payroll.jpg"));
            statsRow.add(statCard("Position", emp[11], PURPLE,
                "/motorphpayrollsystem/images/userprofile.jpg"));

            new javax.swing.SwingWorker<double[], Void>() {
                protected double[] doInBackground() {
                    return Attendance.getAttendance(empNo);
                }
                protected void done() {
                    try {
                        double[] att = get();
                        daysVal.setText(String.valueOf((int) att[0]));
                        otVal.setText(String.format("%.1f", att[1]));
                    } catch (Exception ignored) {}
                }
            }.execute();
        }

        // ── Quick access buttons ──────────────────────────────────────────────
        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        quickRow.setBackground(LIGHT);

        quickRow.add(quickBtn("My Profile",  new Color(37,99,235),
            e -> nav(() -> new ProfileUI(empNo, emp))));
        quickRow.add(quickBtn("Payroll",     GREEN,
            e -> nav(() -> new PayrollUI(empNo, emp))));
        quickRow.add(quickBtn("Attendance",  new Color(217,119,6),
            e -> nav(() -> new AttendanceUI(empNo, emp))));
        if (EmployeeData.isHR(emp))
            quickRow.add(quickBtn("Admin", PURPLE,
                e -> nav(() -> new adminModuleUI(empNo, emp))));

        body.add(welcomeCard, BorderLayout.NORTH);
        body.add(statsRow,    BorderLayout.CENTER);
        body.add(quickRow,    BorderLayout.SOUTH);

        main.add(body, BorderLayout.CENTER);
        return main;
    }

    // ── Stats card (async variant — caller owns the value JLabel) ────────────
    private JPanel statCard(String label, JLabel val, Color color, String imgPath) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(16, 20, 16, 20)));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(WHITE);
        JLabel ico = new JLabel(icon(imgPath, 32, 32));
        top.add(ico, BorderLayout.WEST);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(MUTED);
        top.add(lbl, BorderLayout.CENTER);

        JPanel accent = new JPanel();
        accent.setBackground(color);
        accent.setPreferredSize(new Dimension(0, 4));

        card.add(top,    BorderLayout.NORTH);
        card.add(val,    BorderLayout.CENTER);
        card.add(accent, BorderLayout.SOUTH);
        return card;
    }

    // ── Stats card ────────────────────────────────────────────────────────────
    private JPanel statCard(String label, String value, Color color, String imgPath) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 240), 1, true),
            new EmptyBorder(16, 20, 16, 20)));

        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setBackground(WHITE);

        JLabel ico = new JLabel(icon(imgPath, 32, 32));
        top.add(ico, BorderLayout.WEST);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font(FONT, Font.PLAIN, 12));
        lbl.setForeground(MUTED);
        top.add(lbl, BorderLayout.CENTER);

        JLabel val = new JLabel(value);
        val.setFont(new Font(FONT, Font.BOLD, 28));
        val.setForeground(color);

        // Colored bottom accent bar
        JPanel accent = new JPanel();
        accent.setBackground(color);
        accent.setPreferredSize(new Dimension(0, 4));

        card.add(top,    BorderLayout.NORTH);
        card.add(val,    BorderLayout.CENTER);
        card.add(accent, BorderLayout.SOUTH);
        return card;
    }

    // ── Quick action button ───────────────────────────────────────────────────
    private JButton quickBtn(String label, Color color, ActionListener action) {
        JButton b = new JButton(label);
        b.setFont(new Font(FONT, Font.BOLD, 13));
        b.setBackground(color);
        b.setForeground(WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(140, 40));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // Hover darken
        b.addMouseListener(new MouseAdapter() {
            Color orig = color;
            public void mouseEntered(MouseEvent e) {
                b.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(orig);
            }
        });
        b.addActionListener(action);
        return b;
    }

    private int countDepartments() {
        java.util.Set<String> depts = new java.util.HashSet<>();
        if (EmployeeData.records == null) return 0;
        for (int i = 1; i < EmployeeData.records.size(); i++) {
            String[] r = EmployeeData.records.get(i);
            if (r.length > 20 && !r[20].trim().isEmpty())
                depts.add(r[20].trim());
        }
        return depts.size();
    }
}
