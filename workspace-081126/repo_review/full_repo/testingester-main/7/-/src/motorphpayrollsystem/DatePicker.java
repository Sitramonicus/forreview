package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class DatePicker {

    private final Component parent;

    // Colors
    private static final Color BLUE     = new Color(0, 51, 153);
    private static final Color LT_BLUE  = new Color(37, 99, 235);
    private static final Color WHITE    = Color.WHITE;
    private static final Color LIGHT    = new Color(245, 247, 252);
    private static final Color TODAY_BG = new Color(219, 234, 254);
    private static final Color GRAY     = new Color(150, 160, 175);
    private static final Color WEEKEND  = new Color(200, 40, 40);
    private static final Color OFF_BG   = new Color(248, 249, 252);
    private static final Color YELLOW   = new Color(255, 220, 60);
    private static final Color NAV_BG   = new Color(20, 60, 140);
    private static final String F       = "Dialog";

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("MM/dd/yyyy");

    private static final String[] MONTH_FULL = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };
    private static final String[] MONTH_SHORT = {
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    };
    private static final String[] DOW = {
        "Sun","Mon","Tue","Wed","Thu","Fri","Sat"
    };

    // Mutable state
    private LocalDate viewMonth;   // always .withDayOfMonth(1)
    private LocalDate selected;
    private JButton[] cells = new JButton[42];
    private JLabel lblMonthBtn;
    private JLabel lblYearBtn;
    private JLabel lblSelected;
    private JDialog mainDlg;

    public DatePicker(Component parent) {
        this.parent = parent;
    }

    /** Show calendar starting at today. Returns MM/dd/yyyy or null. */
    public String showDialog() {
        return showDialog(LocalDate.now());
    }

    /** Show calendar pre-selected at given date. Returns MM/dd/yyyy or null. */
    public String showDialog(LocalDate initial) {
        this.viewMonth = initial.withDayOfMonth(1);
        this.selected  = initial;
        final String[] result = {null};

        mainDlg = new JDialog(
            SwingUtilities.getWindowAncestor(parent),
            "Select Date", Dialog.ModalityType.APPLICATION_MODAL);
        mainDlg.setSize(420, 390);
        mainDlg.setMinimumSize(new Dimension(400, 370));
        mainDlg.setResizable(false);
        mainDlg.setLocationRelativeTo(parent);
        mainDlg.setLayout(new BorderLayout(0, 0));
        mainDlg.add(makeHeader(),        BorderLayout.NORTH);
        mainDlg.add(makeCalendarGrid(),  BorderLayout.CENTER);
        mainDlg.add(makeFooter(result),  BorderLayout.SOUTH);

        repaintCalendar();
        mainDlg.setVisible(true);
        return result[0];
    }

    // =========================================================================
    // HEADER
    // =========================================================================
    private JPanel makeHeader() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(BLUE);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Prev year / prev month
        JButton bPY = makeNavBtn("<<");
        JButton bPM = makeNavBtn("< ");
        bPY.addActionListener(e -> { viewMonth = viewMonth.minusYears(1);  repaintCalendar(); });
        bPM.addActionListener(e -> { viewMonth = viewMonth.minusMonths(1); repaintCalendar(); });

        // Next month / next year
        JButton bNM = makeNavBtn(" >");
        JButton bNY = makeNavBtn(">>");
        bNM.addActionListener(e -> { viewMonth = viewMonth.plusMonths(1);  repaintCalendar(); });
        bNY.addActionListener(e -> { viewMonth = viewMonth.plusYears(1);   repaintCalendar(); });

        JPanel lp = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        lp.setBackground(BLUE);
        lp.add(bPY); lp.add(bPM);

        JPanel rp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        rp.setBackground(BLUE);
        rp.add(bNM); rp.add(bNY);

        // Month label — click to pick month
        lblMonthBtn = new JLabel("", SwingConstants.CENTER);
        lblMonthBtn.setFont(new Font(F, Font.BOLD, 15));
        lblMonthBtn.setForeground(WHITE);
        lblMonthBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblMonthBtn.setToolTipText("Click to choose month");
        lblMonthBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { openMonthPicker(); }
            public void mouseEntered(MouseEvent e) { lblMonthBtn.setForeground(YELLOW); }
            public void mouseExited (MouseEvent e) { lblMonthBtn.setForeground(WHITE);  }
        });

        // Year label — click to pick year (shown in yellow = hint clickable)
        lblYearBtn = new JLabel("", SwingConstants.CENTER);
        lblYearBtn.setFont(new Font(F, Font.BOLD, 15));
        lblYearBtn.setForeground(YELLOW);
        lblYearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lblYearBtn.setToolTipText("Click to choose year");
        lblYearBtn.setBorder(new EmptyBorder(2, 8, 2, 8));
        lblYearBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { openYearPicker(); }
            public void mouseEntered(MouseEvent e) {
                lblYearBtn.setOpaque(true);
                lblYearBtn.setBackground(NAV_BG);
                lblYearBtn.setForeground(WHITE);
            }
            public void mouseExited(MouseEvent e) {
                lblYearBtn.setOpaque(false);
                lblYearBtn.setForeground(YELLOW);
            }
        });

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        center.setBackground(BLUE);
        center.add(lblMonthBtn);
        center.add(lblYearBtn);

        p.add(lp,     BorderLayout.WEST);
        p.add(center, BorderLayout.CENTER);
        p.add(rp,     BorderLayout.EAST);
        return p;
    }

    // =========================================================================
    // CALENDAR GRID
    // =========================================================================
    private JPanel makeCalendarGrid() {
        // Day-of-week row
        JPanel dowRow = new JPanel(new GridLayout(1, 7, 0, 0));
        dowRow.setBackground(new Color(225, 230, 245));
        dowRow.setBorder(new EmptyBorder(5, 10, 5, 10));
        for (int i = 0; i < 7; i++) {
            JLabel l = new JLabel(DOW[i], SwingConstants.CENTER);
            l.setFont(new Font(F, Font.BOLD, 12));
            l.setForeground(i == 0 || i == 6 ? WEEKEND : new Color(50, 60, 100));
            dowRow.add(l);
        }

        // 6 × 7 date buttons
        JPanel grid = new JPanel(new GridLayout(6, 7, 3, 3));
        grid.setBackground(WHITE);
        grid.setBorder(new EmptyBorder(4, 10, 4, 10));

        for (int i = 0; i < 42; i++) {
            final int idx = i;
            JButton b = new JButton(" ");
            b.setFont(new Font(F, Font.PLAIN, 13));
            b.setMargin(new Insets(0, 0, 0, 0));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setBackground(WHITE);
            b.setPreferredSize(new Dimension(46, 36));
            b.setMinimumSize(new Dimension(40, 32));
            b.addActionListener(e -> {
                int sd  = viewMonth.getDayOfWeek().getValue() % 7; // Sun=0
                int day = idx - sd + 1;
                if (day >= 1 && day <= viewMonth.lengthOfMonth()) {
                    selected = viewMonth.withDayOfMonth(day);
                    repaintCalendar();
                }
            });
            cells[i] = b;
            grid.add(b);
        }

        JPanel wrap = new JPanel(new BorderLayout(0, 0));
        wrap.setBackground(WHITE);
        wrap.add(dowRow, BorderLayout.NORTH);
        wrap.add(grid,   BorderLayout.CENTER);
        return wrap;
    }

    // =========================================================================
    // FOOTER
    // =========================================================================
    private JPanel makeFooter(String[] result) {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(LIGHT);
        p.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 230)));

        lblSelected = new JLabel(" ", SwingConstants.LEFT);
        lblSelected.setFont(new Font(F, Font.BOLD, 12));
        lblSelected.setForeground(BLUE);
        lblSelected.setBorder(new EmptyBorder(5, 12, 2, 12));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        btns.setBackground(LIGHT);

        JButton bToday  = makeFootBtn("Today",  LT_BLUE, WHITE);
        JButton bClear  = makeFootBtn("Clear",  GRAY,    WHITE);
        JButton bCancel = makeFootBtn("Cancel", new Color(175, 177, 183), new Color(40,40,50));
        JButton bOK     = makeFootBtn("OK",     BLUE,    WHITE);

        bToday.addActionListener(e -> {
            selected  = LocalDate.now();
            viewMonth = selected.withDayOfMonth(1);
            repaintCalendar();
        });
        bClear .addActionListener(e -> { result[0] = ""; mainDlg.dispose(); });
        bCancel.addActionListener(e -> mainDlg.dispose());
        bOK    .addActionListener(e -> {
            result[0] = selected.format(FMT);
            mainDlg.dispose();
        });

        btns.add(bToday); btns.add(bClear); btns.add(bCancel); btns.add(bOK);
        p.add(lblSelected, BorderLayout.NORTH);
        p.add(btns,        BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    // REPAINT CALENDAR
    // =========================================================================
    private void repaintCalendar() {
        LocalDate now = LocalDate.now();
        int sd  = viewMonth.getDayOfWeek().getValue() % 7; // Sun=0
        int dim = viewMonth.lengthOfMonth();

        lblMonthBtn.setText(MONTH_FULL[viewMonth.getMonthValue() - 1]);
        lblYearBtn .setText(String.valueOf(viewMonth.getYear()));
        if (lblSelected != null)
            lblSelected.setText("Selected:  " + selected.format(FMT));

        for (int i = 0; i < 42; i++) {
            int day = i - sd + 1;
            JButton b = cells[i];

            if (day < 1 || day > dim) {
                b.setText(" ");
                b.setEnabled(false);
                b.setBackground(OFF_BG);
                b.setForeground(GRAY);
            } else {
                LocalDate d     = viewMonth.withDayOfMonth(day);
                boolean isSel   = d.equals(selected);
                boolean isToday = d.equals(now);
                boolean isWkend = (i % 7 == 0 || i % 7 == 6);

                b.setText(String.valueOf(day));
                b.setEnabled(true);

                if (isSel) {
                    b.setBackground(BLUE);
                    b.setForeground(WHITE);
                    b.setFont(new Font(F, Font.BOLD, 13));
                } else if (isToday) {
                    b.setBackground(TODAY_BG);
                    b.setForeground(BLUE);
                    b.setFont(new Font(F, Font.BOLD, 13));
                } else {
                    b.setBackground(WHITE);
                    b.setForeground(isWkend ? WEEKEND : new Color(30, 30, 50));
                    b.setFont(new Font(F, Font.PLAIN, 13));
                }
            }
        }
        if (mainDlg != null) { mainDlg.revalidate(); mainDlg.repaint(); }
    }

    // =========================================================================
    // MONTH PICKER
    // =========================================================================
    private void openMonthPicker() {
        JDialog md = new JDialog(mainDlg, "Select Month",
            Dialog.ModalityType.APPLICATION_MODAL);
        md.setSize(280, 200);
        md.setResizable(false);
        md.setLocationRelativeTo(mainDlg);
        md.setLayout(new BorderLayout(0, 0));

        JLabel hdr = new JLabel("Select Month", SwingConstants.CENTER);
        hdr.setFont(new Font(F, Font.BOLD, 14));
        hdr.setForeground(WHITE);
        hdr.setBackground(BLUE);
        hdr.setOpaque(true);
        hdr.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel grid = new JPanel(new GridLayout(3, 4, 6, 6));
        grid.setBackground(WHITE);
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));

        for (int m = 0; m < 12; m++) {
            final int mo = m + 1;
            boolean isCurrent = (mo == viewMonth.getMonthValue());
            JButton b = new JButton(MONTH_SHORT[m]);
            b.setFont(new Font(F, Font.BOLD, 12));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setPreferredSize(new Dimension(56, 34));
            b.setBackground(isCurrent ? BLUE : new Color(235, 238, 248));
            b.setForeground(isCurrent ? WHITE : new Color(30, 30, 50));
            b.addActionListener(e -> {
                viewMonth = viewMonth.withMonth(mo);
                repaintCalendar();
                md.dispose();
            });
            grid.add(b);
        }

        md.add(hdr,  BorderLayout.NORTH);
        md.add(grid, BorderLayout.CENTER);
        md.setVisible(true);
    }

    // =========================================================================
    // YEAR PICKER
    // =========================================================================
    private void openYearPicker() {
        JDialog yd = new JDialog(mainDlg, "Select Year",
            Dialog.ModalityType.APPLICATION_MODAL);
        yd.setSize(360, 400);
        yd.setResizable(false);
        yd.setLocationRelativeTo(mainDlg);
        yd.setLayout(new BorderLayout(0, 0));

        int cur = viewMonth.getYear();
        final int[] rs = {(cur / 24) * 24 - 12};  // center current year in range

        // Header
        JPanel hdr = new JPanel(new BorderLayout(0, 6));
        hdr.setBackground(BLUE);
        hdr.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel title = new JLabel("Select Year", SwingConstants.CENTER);
        title.setFont(new Font(F, Font.BOLD, 14));
        title.setForeground(WHITE);

        JButton bPrev = makeNavBtn("<<");
        JButton bNext = makeNavBtn(">>");
        JLabel  rLbl  = new JLabel("", SwingConstants.CENTER);
        rLbl.setFont(new Font(F, Font.PLAIN, 11));
        rLbl.setForeground(new Color(180, 200, 230));

        JPanel navRow = new JPanel(new BorderLayout(6, 0));
        navRow.setBackground(BLUE);
        navRow.add(bPrev, BorderLayout.WEST);
        navRow.add(rLbl,  BorderLayout.CENTER);
        navRow.add(bNext, BorderLayout.EAST);

        hdr.add(title,  BorderLayout.NORTH);
        hdr.add(navRow, BorderLayout.SOUTH);

        // Year grid — 6 rows × 4 cols = 24 years
        JPanel grid = new JPanel(new GridLayout(6, 4, 6, 6));
        grid.setBackground(WHITE);
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton[] yb = new JButton[24];
        for (int i = 0; i < 24; i++) {
            JButton b = new JButton();
            b.setFont(new Font(F, Font.BOLD, 13));
            b.setFocusPainted(false);
            b.setBorderPainted(false);
            b.setOpaque(true);
            b.setPreferredSize(new Dimension(70, 38));
            yb[i] = b;
            grid.add(b);
        }

        // Jump-to-year footer
        JPanel foot = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        foot.setBackground(LIGHT);
        foot.setBorder(new MatteBorder(1, 0, 0, 0, new Color(210, 215, 230)));

        JLabel jLbl = new JLabel("Type year:");
        jLbl.setFont(new Font(F, Font.PLAIN, 12));

        JTextField jTxt = new JTextField(6);
        jTxt.setFont(new Font(F, Font.PLAIN, 13));
        jTxt.setBorder(new CompoundBorder(
            new LineBorder(BLUE, 1, true), new EmptyBorder(3, 6, 3, 6)));

        JButton jBtn = makeFootBtn("Go", BLUE, WHITE);
        jBtn.setPreferredSize(new Dimension(50, 28));

        foot.add(jLbl); foot.add(jTxt); foot.add(jBtn);

        yd.add(hdr,  BorderLayout.NORTH);
        yd.add(grid, BorderLayout.CENTER);
        yd.add(foot, BorderLayout.SOUTH);

        // ── Render year grid ────────────────────────────────────────────────
        Runnable renderYears = () -> {
            rLbl.setText(rs[0] + "  –  " + (rs[0] + 23));
            for (int i = 0; i < 24; i++) {
                final int yr = rs[0] + i;
                JButton b = yb[i];
                b.setText(String.valueOf(yr));
                if (yr == viewMonth.getYear()) {
                    b.setBackground(BLUE);    b.setForeground(WHITE);
                } else if (yr == LocalDate.now().getYear()) {
                    b.setBackground(TODAY_BG); b.setForeground(BLUE);
                } else {
                    b.setBackground(new Color(235,238,248));
                    b.setForeground(new Color(30,30,50));
                }
                for (ActionListener al : b.getActionListeners())
                    b.removeActionListener(al);
                b.addActionListener(e -> {
                    viewMonth = viewMonth.withYear(yr);
                    try { selected = selected.withYear(yr); }
                    catch (Exception ex) {
                        selected = selected.withDayOfMonth(28).withYear(yr);
                    }
                    repaintCalendar();
                    yd.dispose();
                });
            }
            grid.revalidate(); grid.repaint();
        };

        bPrev.addActionListener(e -> { rs[0] -= 24; renderYears.run(); });
        bNext.addActionListener(e -> { rs[0] += 24; renderYears.run(); });

        Runnable doJump = () -> {
            try {
                int yr = Integer.parseInt(jTxt.getText().trim());
                if (yr < 1900 || yr > 2200) {
                    JOptionPane.showMessageDialog(yd,
                        "Enter a year between 1900 and 2200.",
                        "Invalid Year", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                rs[0] = (yr / 24) * 24 - 12;
                viewMonth = viewMonth.withYear(yr);
                try { selected = selected.withYear(yr); }
                catch (Exception ex) {
                    selected = selected.withDayOfMonth(28).withYear(yr);
                }
                renderYears.run();
                repaintCalendar();
                yd.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(yd,
                    "Enter a valid year (e.g. 1985).",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            }
        };
        jBtn.addActionListener(e -> doJump.run());
        jTxt.addActionListener(e -> doJump.run());

        renderYears.run();
        yd.setVisible(true);
    }

    // =========================================================================
    // STATIC FACTORY — field + Pick button
    // =========================================================================
    public static JPanel createDateField(Component parent,
                                          JTextField field,
                                          String initialValue) {
        if (initialValue != null && !initialValue.isEmpty())
            field.setText(initialValue);

        JButton btn = new JButton("Pick");
        btn.setFont(new Font("Dialog", Font.BOLD, 11));
        btn.setBackground(new Color(0, 51, 153));
        btn.setForeground(Color.WHITE);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(50, 26));
        btn.setMargin(new Insets(2, 4, 2, 4));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setToolTipText("Click to pick a date");
        btn.addActionListener(e -> {
            DatePicker dp = new DatePicker(parent);
            LocalDate pre = LocalDate.now();
            try {
                String v = field.getText().trim();
                if (!v.isEmpty())
                    pre = LocalDate.parse(v, DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } catch (Exception ignored) {}
            String picked = dp.showDialog(pre);
            if (picked != null) field.setText(picked);
        });

        JPanel wrap = new JPanel(new BorderLayout(2, 0));
        wrap.setOpaque(false);
        wrap.add(field, BorderLayout.CENTER);
        wrap.add(btn,   BorderLayout.EAST);
        return wrap;
    }

    // =========================================================================
    // BUTTON FACTORIES
    // =========================================================================
    private JButton makeNavBtn(String txt) {
        JButton b = new JButton(txt);
        b.setFont(new Font(F, Font.BOLD, 13));
        b.setForeground(WHITE);
        b.setBackground(NAV_BG);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(54, 26));
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton makeFootBtn(String txt, Color bg, Color fg) {
        JButton b = new JButton(txt);
        b.setFont(new Font(F, Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setPreferredSize(new Dimension(80, 30));
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
