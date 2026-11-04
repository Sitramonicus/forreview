package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public abstract class UIBase extends JFrame {

    // ── Brand colors ──────────────────────────────────────────────────────────
    protected static final Color BLUE   = new Color(0, 51, 153);
    protected static final Color YELLOW = new Color(255, 204, 0);
    protected static final Color WHITE  = Color.WHITE;
    protected static final Color LIGHT  = new Color(245, 247, 252);
    protected static final Color GREEN  = new Color(22, 163, 74);
    protected static final Color RED    = new Color(220, 38, 38);
    protected static final Color MUTED  = new Color(100, 116, 139);
    protected static final Color PURPLE = new Color(109, 40, 217);

    protected String   empNo;
    protected String[] emp;
    protected String   activeScreen = "";  // set by subclass to highlight sidebar item

    private static final Color ACTIVE_BG = new Color(10, 40, 100);

    // ── Cross-platform font ───────────────────────────────────────────────────
    // "Segoe UI" doesn't exist on Mac/Linux — use Dialog which maps to the
    // system font on every OS (Helvetica on Mac, DejaVu on Linux, Segoe on Win)
    protected static final String FONT = "Dialog";

    public UIBase(String title, String empNo, String[] emp) {
        this.empNo = empNo;
        this.emp   = emp;
        setTitle(title);
        setSize(1060, 640);
        setMinimumSize(new Dimension(820, 520));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Fix Mac menu bar behavior
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MotorPH");
    }

    // =========================================================================
    // SIDEBAR — fully responsive, works on all platforms and window sizes
    // =========================================================================
    protected JPanel buildSidebar(String adminEmpNo, String[] adminEmp,
                                   ActionListener[] navActions) {
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setBackground(BLUE);
        sidebar.setPreferredSize(new Dimension(165, 0));
        sidebar.setMinimumSize(new Dimension(130, 0));

        // ── Logo ──────────────────────────────────────────────────────────────
        GridBagConstraints gcLogo = new GridBagConstraints();
        gcLogo.gridx = 0; gcLogo.gridy = 0;
        gcLogo.gridwidth = 2;
        gcLogo.fill = GridBagConstraints.HORIZONTAL;
        gcLogo.weightx = 1.0;
        gcLogo.insets = new Insets(14, 0, 6, 0);

        JLabel logo = new JLabel(icon("/motorphpayrollsystem/images/logo.jpg", 100, 60));
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(165, 70));
        sidebar.add(logo, gcLogo);

        // ── Thin divider ──────────────────────────────────────────────────────
        GridBagConstraints gcDiv = new GridBagConstraints();
        gcDiv.gridx = 0; gcDiv.gridy = 1;
        gcDiv.gridwidth = 2;
        gcDiv.fill = GridBagConstraints.HORIZONTAL;
        gcDiv.weightx = 1.0;
        gcDiv.insets = new Insets(0, 10, 4, 10);

        JPanel div = new JPanel();
        div.setBackground(new Color(50, 80, 140));
        div.setPreferredSize(new Dimension(0, 1));
        sidebar.add(div, gcDiv);

        // ── Nav items ─────────────────────────────────────────────────────────
        String[][] navDefs = {
            {"Main Menu",  "/motorphpayrollsystem/images/mainmenu.jpg"},
            {"Profile",    "/motorphpayrollsystem/images/userprofile.jpg"},
            {"Payroll",    "/motorphpayrollsystem/images/payroll.jpg"},
            {"Attendance", "/motorphpayrollsystem/images/attendance.jpg"},
            {"Admin",      "/motorphpayrollsystem/images/admin.jpg"},
            {"Logout",     "/motorphpayrollsystem/images/logout.jpg"},
        };

        for (int i = 0; i < navDefs.length; i++) {
            // Hide Admin tab entirely for non-HR users
            if (i == 4 && !EmployeeData.isHR(adminEmp)) continue;
            final Color rowHover = new Color(20, 50, 110);

            // Icon button
            JButton iconBtn = new JButton(icon(navDefs[i][1], 24, 24));
            iconBtn.setContentAreaFilled(false);
            iconBtn.setOpaque(false);
            iconBtn.setBorderPainted(false);
            iconBtn.setFocusPainted(false);
            iconBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            iconBtn.setPreferredSize(new Dimension(36, 36));
            iconBtn.addActionListener(navActions[i]);

            // Label button
            JButton lblBtn = new JButton(navDefs[i][0]);
            lblBtn.setFont(new Font(FONT, Font.BOLD, 13));
            lblBtn.setForeground(
                navDefs[i][0].equals("Logout") ? new Color(255, 120, 120) : WHITE);
            lblBtn.setBackground(BLUE);
            lblBtn.setOpaque(false);
            lblBtn.setContentAreaFilled(false);
            lblBtn.setBorderPainted(false);
            lblBtn.setFocusPainted(false);
            lblBtn.setHorizontalAlignment(SwingConstants.LEFT);
            lblBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lblBtn.addActionListener(navActions[i]);

            // Active screen highlight
            boolean isActive = navDefs[i][0].equalsIgnoreCase(activeScreen);
            if (isActive) {
                iconBtn.setOpaque(true); iconBtn.setBackground(ACTIVE_BG);
                lblBtn .setOpaque(true); lblBtn .setBackground(ACTIVE_BG);
                lblBtn .setForeground(!navDefs[i][0].equals("Logout") ? 
                    new Color(255,220,60) : new Color(255,120,120));
            }

            // Hover effect — both components respond together
            MouseAdapter hover = new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    iconBtn.setOpaque(true); iconBtn.setBackground(rowHover);
                    lblBtn .setOpaque(true); lblBtn .setBackground(rowHover);
                    sidebar.repaint();
                }
                public void mouseExited(MouseEvent e) {
                    if (true) {
                        // restore active or default
                    }
                    iconBtn.setOpaque(isActive); 
                    iconBtn.setBackground(isActive ? ACTIVE_BG : BLUE);
                    lblBtn .setOpaque(isActive);  
                    lblBtn .setBackground(isActive ? ACTIVE_BG : BLUE);
                    sidebar.repaint();
                }
            };
            iconBtn.addMouseListener(hover);
            lblBtn .addMouseListener(hover);

            GridBagConstraints gc1 = new GridBagConstraints();
            gc1.gridx = 0; gc1.gridy = 2 + i;
            gc1.weightx = 0; gc1.fill = GridBagConstraints.NONE;
            gc1.anchor = GridBagConstraints.CENTER;
            gc1.insets = new Insets(4, 8, 4, 2);
            sidebar.add(iconBtn, gc1);

            GridBagConstraints gc2 = new GridBagConstraints();
            gc2.gridx = 1; gc2.gridy = 2 + i;
            gc2.weightx = 1.0; gc2.fill = GridBagConstraints.HORIZONTAL;
            gc2.insets = new Insets(4, 0, 4, 6);
            sidebar.add(lblBtn, gc2);
        }

        // ── Vertical spacer pushes nav items to top ───────────────────────────
        GridBagConstraints gcSpace = new GridBagConstraints();
        gcSpace.gridx = 0; gcSpace.gridy = 20;
        gcSpace.gridwidth = 2; gcSpace.weighty = 1.0;
        gcSpace.fill = GridBagConstraints.VERTICAL;
        JPanel spacer = new JPanel();
        spacer.setBackground(BLUE);
        sidebar.add(spacer, gcSpace);

        return sidebar;
    }

    // =========================================================================
    // SHARED HELPERS
    // =========================================================================

    /** Blue header bar with title. */
    protected JPanel pageHeader(String title) {
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(BLUE);
        hdr.setBorder(new EmptyBorder(14, 24, 14, 24));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font(FONT, Font.BOLD, 22));
        lbl.setForeground(WHITE);
        hdr.add(lbl, BorderLayout.WEST);
        return hdr;
    }

    /** Blue header bar with title + right-side component (e.g. clock). */
    protected JPanel pageHeader(String title, JComponent right) {
        JPanel hdr = pageHeader(title);
        hdr.add(right, BorderLayout.EAST);
        return hdr;
    }

    /**
     * Navigate to another screen.
     * Always runs on the Event Dispatch Thread to prevent latency/freezes.
     * - Preserves maximized/restored state
     * - Keeps the window on the same monitor (multi-display aware)
     * - Flash-free: the old window is disposed only AFTER the new one is shown
     *
     * (No opacity fade — Swing forbids setOpacity() on decorated frames, which
     *  all our windows are, so a fade would throw IllegalComponentStateException.)
     */
    protected void nav(java.util.function.Supplier<JFrame> factory) {
        final int                    windowState = getExtendedState();
        final Dimension              size        = getSize();
        final Point                  loc         = getLocation();
        final GraphicsConfiguration  gc          = getGraphicsConfiguration();

        SwingUtilities.invokeLater(() -> {
            try {
                JFrame next = factory.get();

                if ((windowState & JFrame.MAXIMIZED_BOTH) != 0) {
                    // Anchor to the same monitor, then maximize on it
                    Rectangle screen = gc.getBounds();
                    next.setLocation(screen.x + 10, screen.y + 10);
                    next.setVisible(true);
                    next.setExtendedState(JFrame.MAXIMIZED_BOTH);
                } else {
                    next.setSize(size);
                    next.setLocation(loc);
                    next.setVisible(true);
                }

                dispose();   // dispose old AFTER new is visible -> no flicker
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Navigation error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /** Standard styled action button — works on Mac and Windows. */
    protected JButton actionBtn(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(new Font(FONT, Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        // setOpaque(true) is required on Mac for background color to show
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(150, 34));
        return b;
    }

    /**
     * Loads an image from the classpath.
     * Returns empty ImageIcon (no crash) if image not found —
     * prevents NullPointerException on Mac when path differs.
     */
    protected ImageIcon icon(String path, int w, int h) {
        try {
            java.net.URL u = getClass().getResource(path);
            if (u == null) {
                // Fallback: try without leading slash (Mac classpath difference)
                u = getClass().getResource(path.replaceFirst("^/", ""));
            }
            if (u == null) return new ImageIcon();
            Image img = new ImageIcon(u).getImage()
                .getScaledInstance(w, h, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        } catch (Exception e) {
            return new ImageIcon(); // never crash on missing image
        }
    }
}
