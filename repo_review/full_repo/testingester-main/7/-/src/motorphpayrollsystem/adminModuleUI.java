package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class adminModuleUI extends UIBase {

    public adminModuleUI(String empNo, String[] emp) {
        super("MotorPH – Admin", empNo, emp);

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
        main.add(pageHeader("Admin — Employee Management"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(LIGHT);
        body.setBorder(new EmptyBorder(20, 30, 16, 30));

        JLabel sub = new JLabel("Select an action to manage employee records.",
            SwingConstants.CENTER);
        sub.setFont(new Font(FONT, Font.PLAIN, 14));
        sub.setForeground(MUTED);
        body.add(sub, BorderLayout.NORTH);

        // 2x3 grid of cards — uses full available space
        JPanel cards = new JPanel(new GridLayout(2, 3, 16, 16));
        cards.setBackground(LIGHT);

        cards.add(adminCard("/motorphpayrollsystem/images/create.jpg",
            "Add Employee", "Register a new employee\ninto the system.", GREEN,
            e -> nav(() -> new addNewEmp(empNo, emp))));

        cards.add(adminCard("/motorphpayrollsystem/images/update.jpg",
            "Update Employee", "Modify existing employee\nprofile and details.", BLUE,
            e -> nav(() -> new adminUpdateProfile(empNo, emp))));

        cards.add(adminCard("/motorphpayrollsystem/images/delete.jpg",
            "Delete Employee", "Permanently remove an\nemployee from records.", RED,
            e -> {
                int c = JOptionPane.showConfirmDialog(this,
                    "This action is permanent. Continue?",
                    "Confirm", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
                if (c == JOptionPane.YES_OPTION)
                    nav(() -> new deleteEmp(empNo, emp));
            }));

        cards.add(adminCard("/motorphpayrollsystem/images/payroll.jpg",
            "Employee Records", "View all employees with\nsalary computation.",
            new Color(37,99,235),
            e -> nav(() -> new EmployeeRecords(empNo, emp))));

        cards.add(adminCard("/motorphpayrollsystem/images/payroll.jpg",
            "Bulk Payroll", "Compute payroll for all\nemployees at once.",
            new Color(109,40,217),
            e -> nav(() -> new BulkPayrollUI(empNo, emp))));

        body.add(cards, BorderLayout.CENTER);

        // Info bar
        JPanel info = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 8));
        info.setBackground(WHITE);
        info.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(4, 16, 4, 16)));

        JLabel countLbl = new JLabel("Total Employees: " + EmployeeData.getTotalEmployees());
        countLbl.setFont(new Font(FONT, Font.BOLD, 13));
        countLbl.setForeground(BLUE);
        info.add(countLbl);

        // Dynamically update employee count when data changes
        EmployeeData.addChangeListener(() -> {
            javax.swing.SwingUtilities.invokeLater(() ->
                countLbl.setText("Total Employees: " + EmployeeData.getTotalEmployees()));
        });
        info.add(new JLabel("|") {{ setForeground(new Color(200,210,230)); }});
        JLabel whoLbl = new JLabel("Logged in as: " + emp[2] + " " + emp[1] +
            " (" + emp[20] + ")");
        whoLbl.setFont(new Font(FONT, Font.PLAIN, 13));
        whoLbl.setForeground(MUTED);
        info.add(whoLbl);

        body.add(info, BorderLayout.SOUTH);
        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private JPanel adminCard(String imgPath, String title, String desc,
                              Color color, ActionListener action) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(16, 20, 16, 20)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel img = new JLabel(icon(imgPath, 60, 60), SwingConstants.CENTER);
        img.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(WHITE);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = GridBagConstraints.RELATIVE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.insets = new Insets(3, 0, 3, 0);
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;

        JLabel lbl = new JLabel(title, SwingConstants.CENTER);
        lbl.setFont(new Font(FONT, Font.BOLD, 15));
        lbl.setForeground(color);
        center.add(lbl, gc);

        JLabel descLbl = new JLabel(
            "<html><center>" + desc.replace("\n","<br>") + "</center></html>",
            SwingConstants.CENTER);
        descLbl.setFont(new Font(FONT, Font.PLAIN, 12));
        descLbl.setForeground(MUTED);
        center.add(descLbl, gc);

        // Button always visible at bottom
        JButton btn = actionBtn(title, color, WHITE);
        btn.addActionListener(action);
        btn.setPreferredSize(new Dimension(160, 34));
        gc.fill = GridBagConstraints.NONE;
        center.add(btn, gc);

        card.add(img,    BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        // Hover effect
        MouseAdapter hover = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                    new LineBorder(color, 2, true),
                    new EmptyBorder(15, 19, 15, 19)));
                card.setBackground(new Color(248,250,255));
                center.setBackground(new Color(248,250,255));
            }
            public void mouseExited(MouseEvent e) {
                card.setBorder(new CompoundBorder(
                    new LineBorder(new Color(220,225,240), 1, true),
                    new EmptyBorder(16, 20, 16, 20)));
                card.setBackground(WHITE);
                center.setBackground(WHITE);
            }
        };
        card.addMouseListener(hover);
        return card;
    }
}
