package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ProfileUI extends UIBase {

    public ProfileUI(String empNo, String[] emp) {
        super("MotorPH – Profile", empNo, emp);
        activeScreen = "Profile";

        ActionListener[] nav = {
            e -> nav(() -> new DashboardUI(empNo, emp)),
            e -> nav(() -> new ProfileUI(empNo, emp)),
            e -> nav(() -> new PayrollUI(empNo, emp)),
            e -> nav(() -> new AttendanceUI(empNo, emp)),
            e -> {
                if (!EmployeeData.isHR(emp)) {
                    JOptionPane.showMessageDialog(this,
                        "Access Denied.", "Unauthorized", JOptionPane.ERROR_MESSAGE); return;
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
        main.add(pageHeader("Profile"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setBackground(LIGHT);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Left avatar card
        JPanel avatarCard = new JPanel(new GridBagLayout());
        avatarCard.setBackground(WHITE);
        avatarCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(20, 16, 20, 16)));
        avatarCard.setPreferredSize(new Dimension(185, 0));
        avatarCard.setMinimumSize(new Dimension(160, 0));

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(6, 0, 6, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1.0;

        // Initials avatar - auto-colored by department
        String initials = (emp[2].length() > 0 ? String.valueOf(emp[2].charAt(0)) : "") +
                          (emp[1].length() > 0 ? String.valueOf(emp[1].charAt(0)) : "");
        // Pick color based on department hash
        Color[] avatarColors = {
            new Color(0,51,153), new Color(22,163,74), new Color(109,40,217),
            new Color(217,119,6), new Color(220,38,38), new Color(14,116,144)
        };
        Color avatarColor = avatarColors[Math.abs(emp[20].hashCode()) % avatarColors.length];
        JPanel avatarCircle = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(avatarColor);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT, Font.BOLD, 32));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initials)) / 2;
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(initials, x, y);
            }
        };
        avatarCircle.setPreferredSize(new Dimension(90, 90));
        avatarCircle.setOpaque(false);
        gc.fill = GridBagConstraints.NONE; gc.weightx = 0;
        avatarCard.add(avatarCircle, gc);

        JLabel nm = new JLabel("<html><center>" + emp[2] + " " + emp[1] + "</center></html>",
            SwingConstants.CENTER);
        nm.setFont(new Font(FONT, Font.BOLD, 14));
        nm.setForeground(BLUE);
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;
        avatarCard.add(nm, gc);

        JLabel pos = new JLabel("<html><center>" + emp[11] + "</center></html>",
            SwingConstants.CENTER);
        pos.setFont(new Font(FONT, Font.PLAIN, 12));
        pos.setForeground(MUTED);
        avatarCard.add(pos, gc);

        JLabel statusLbl = new JLabel("  " + emp[10] + "  ", SwingConstants.CENTER);
        statusLbl.setFont(new Font(FONT, Font.BOLD, 11));
        statusLbl.setForeground(WHITE);
        statusLbl.setBackground(emp[10].equalsIgnoreCase("Active")
            ? GREEN : new Color(217,119,6));
        statusLbl.setOpaque(true);
        statusLbl.setBorder(new EmptyBorder(3, 8, 3, 8));
        avatarCard.add(statusLbl, gc);

        JButton editBtn = actionBtn("✏  Edit Profile", BLUE, WHITE);
        editBtn.addActionListener(e -> nav(() -> new editProfileUI(empNo, emp)));
        avatarCard.add(editBtn, gc);

        // Spacer
        GridBagConstraints gcS = new GridBagConstraints();
        gcS.gridy = 20; gcS.weighty = 1.0; gcS.fill = GridBagConstraints.VERTICAL;
        avatarCard.add(new JPanel() {{ setBackground(WHITE); }}, gcS);

        // Right info card
        JPanel infoCard = new JPanel(new BorderLayout(0, 12));
        infoCard.setBackground(WHITE);
        infoCard.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(20, 24, 20, 24)));

        JLabel infoTitle = new JLabel("Employee Information");
        infoTitle.setFont(new Font(FONT, Font.BOLD, 15));
        infoTitle.setForeground(BLUE);
        infoTitle.setBorder(new EmptyBorder(0,0,8,0));

        JPanel infoBody = new JPanel();
        infoBody.setLayout(new BoxLayout(infoBody, BoxLayout.Y_AXIS));
        infoBody.setBackground(WHITE);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 10));
        grid.setBackground(WHITE);
        addField(grid, "Employee No.",  emp[0]);
        addField(grid, "Last Name",     emp[1]);
        addField(grid, "First Name",    emp[2]);
        addField(grid, "Birthday",      emp[3]);
        addField(grid, "Phone Number",  emp[5]);
        addField(grid, "Position",      emp[11]);
        addField(grid, "Status",        emp[10]);
        addField(grid, "Supervisor",    emp[12]);
        addField(grid, "Department",    emp[20]);
        addField(grid, "Address",       emp[4]);

        JLabel salTitle = new JLabel("Compensation");
        salTitle.setFont(new Font(FONT, Font.BOLD, 14));
        salTitle.setForeground(BLUE);
        salTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        salTitle.setBorder(new EmptyBorder(10, 0, 6, 0));

        JPanel salGrid = new JPanel(new GridLayout(0, 2, 14, 8));
        salGrid.setBackground(WHITE);
        addField(salGrid, "Basic Salary",       fmtPeso(emp[13]));
        addField(salGrid, "Rice Subsidy",       fmtPeso(emp[14]));
        addField(salGrid, "Phone Allowance",    fmtPeso(emp[15]));
        addField(salGrid, "Clothing Allowance", fmtPeso(emp[16]));
        addField(salGrid, "Hourly Rate",        fmtPeso(emp[18]));

        infoBody.add(grid);
        infoBody.add(salTitle);
        infoBody.add(salGrid);

        infoCard.add(infoTitle, BorderLayout.NORTH);
        infoCard.add(new JScrollPane(infoBody) {{
            setBorder(BorderFactory.createEmptyBorder());
        }}, BorderLayout.CENTER);

        body.add(avatarCard, BorderLayout.WEST);
        body.add(infoCard,   BorderLayout.CENTER);
        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private void addField(JPanel g, String label, String value) {
        JLabel l = new JLabel(label);
        l.setFont(new Font(FONT, Font.BOLD, 12));
        l.setForeground(MUTED);
        g.add(l);
        JLabel v = new JLabel(value == null || value.isBlank() ? "—" : value);
        v.setFont(new Font(FONT, Font.PLAIN, 13));
        v.setForeground(new Color(30,30,50));
        g.add(v);
    }

    private String fmtPeso(String raw) {
        try {
            double v = Double.parseDouble(raw.trim().replace(",",""));
            return String.format("₱%,.2f", v);
        } catch (Exception e) { return raw; }
    }
}
