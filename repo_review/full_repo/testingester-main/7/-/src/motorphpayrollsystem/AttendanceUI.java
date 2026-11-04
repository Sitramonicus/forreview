package motorphpayrollsystem;

import java.awt.*;
import java.awt.event.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class AttendanceUI extends UIBase {

    private JTable     tblAttendance;
    private JTextField txtSearchDate;
    private JLabel     lblStatus, lblClock;
    private Timer      clockTimer;

    public AttendanceUI(String empNo, String[] emp) {
        super("MotorPH – Attendance", empNo, emp);
        activeScreen = "Attendance";

        ActionListener[] nav = {
            e -> { stopClock(); nav(() -> new DashboardUI(empNo, emp)); },
            e -> { stopClock(); nav(() -> new ProfileUI(empNo, emp)); },
            e -> { stopClock(); nav(() -> new PayrollUI(empNo, emp)); },
            e -> { stopClock(); nav(() -> new AttendanceUI(empNo, emp)); },
            e -> {
                if (!EmployeeData.isHR(emp)) {
                    JOptionPane.showMessageDialog(this,
                        "Access Denied.", "Unauthorized", JOptionPane.ERROR_MESSAGE); return;
                }
                stopClock(); nav(() -> new adminModuleUI(empNo, emp));
            },
            e -> { stopClock(); nav(() -> new loginUI()); },
        };

        add(buildSidebar(empNo, emp, nav), BorderLayout.WEST);
        add(buildMain(), BorderLayout.CENTER);
        startClock();
        loadAttendance();

        // Stop clock when window is closed directly (not via nav)
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                stopClock();
            }
        });
    }

    private JPanel buildMain() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(WHITE);

        lblClock = new JLabel("", SwingConstants.RIGHT);
        lblClock.setFont(new Font(FONT, Font.PLAIN, 12));
        lblClock.setForeground(new Color(180,200,240));

        main.add(pageHeader("Attendance", lblClock), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setBackground(LIGHT);
        body.setBorder(new EmptyBorder(16, 20, 16, 20));

        // ── Control row ───────────────────────────────────────────────────────
        JPanel ctrl = new JPanel(new BorderLayout(10, 0));
        ctrl.setBackground(WHITE);
        ctrl.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(12, 16, 12, 16)));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnRow.setBackground(WHITE);

        JButton btnIn  = actionBtn("▶  TIME IN",  YELLOW, new Color(30,30,30));
        JButton btnOut = actionBtn("■  TIME OUT", RED,    WHITE);

        btnIn.addActionListener(e -> {
            boolean success = Attendance.recordTimeIn(emp);
            if (success) {
                lblStatus.setText("✔  Clocked in at " +
                    LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
                lblStatus.setForeground(GREEN);
                Toast.success(this, "Time In recorded successfully!");
                loadAttendance();
            }
            // If false, Attendance.recordTimeIn already showed error dialog
        });
        btnOut.addActionListener(e -> {
            boolean success = Attendance.recordTimeOut(empNo);
            if (success) {
                lblStatus.setText("✔  Clocked out at " +
                    LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a")));
                lblStatus.setForeground(BLUE);
                Toast.success(this, "Time Out recorded successfully!");
                loadAttendance();
            }
            // If false, Attendance.recordTimeOut already showed error dialog
        });
        JButton btnDel = actionBtn("🗑  DELETE ENTRY", MUTED, WHITE);
        btnDel.addActionListener(e -> deleteSelectedEntry());
        btnRow.add(btnIn); btnRow.add(btnOut); btnRow.add(btnDel);

        lblStatus = new JLabel("Ready — press Time In to start your shift.");
        lblStatus.setFont(new Font(FONT, Font.ITALIC, 13));
        lblStatus.setForeground(MUTED);
        lblStatus.setHorizontalAlignment(SwingConstants.RIGHT);

        ctrl.add(btnRow,    BorderLayout.WEST);
        ctrl.add(lblStatus, BorderLayout.CENTER);

        // ── Search row ────────────────────────────────────────────────────────
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        searchRow.setBackground(WHITE);
        searchRow.setBorder(new CompoundBorder(
            new LineBorder(new Color(220,225,240), 1, true),
            new EmptyBorder(6, 12, 6, 12)));

        JLabel sl = new JLabel("Filter by Date:");
        sl.setFont(new Font(FONT, Font.PLAIN, 13));
        txtSearchDate = new JTextField(12);
        txtSearchDate.setFont(new Font(FONT, Font.PLAIN, 13));
        txtSearchDate.setToolTipText("MM/dd/yyyy");

        // Calendar picker button
        JButton calBtn = new JButton("[cal]");
        calBtn.setFont(new Font("Dialog", Font.BOLD, 10));
        calBtn.setFocusPainted(false);
        calBtn.setBorderPainted(false);
        calBtn.setOpaque(true);
        calBtn.setBackground(BLUE);
        calBtn.setForeground(WHITE);
        calBtn.setPreferredSize(new Dimension(34, 30));
        calBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calBtn.setToolTipText("Pick a date");
        calBtn.addActionListener(e -> {
            DatePicker picker = new DatePicker(this);
            java.time.LocalDate pre = java.time.LocalDate.now();
            try {
                String existing = txtSearchDate.getText().trim();
                if (!existing.isEmpty())
                    pre = java.time.LocalDate.parse(existing,
                        java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } catch (Exception ignored) {}
            String picked = picker.showDialog(pre);
            if (picked != null) {
                txtSearchDate.setText(picked);
                Attendance.filterAttendance(tblAttendance, empNo, picked);
            }
        });

        JButton btnSearch = actionBtn("Search",   BLUE, WHITE);
        JButton btnAll    = actionBtn("Show All", MUTED, WHITE);
        btnSearch.setPreferredSize(new Dimension(100, 30));
        btnAll   .setPreferredSize(new Dimension(100, 30));

        btnSearch.addActionListener(e ->
            Attendance.filterAttendance(tblAttendance, empNo, txtSearchDate.getText().trim()));
        btnAll.addActionListener(e -> { txtSearchDate.setText(""); loadAttendance(); });
        txtSearchDate.addActionListener(e ->
            Attendance.filterAttendance(tblAttendance, empNo, txtSearchDate.getText().trim()));

        searchRow.add(sl);
        searchRow.add(txtSearchDate);
        searchRow.add(calBtn);
        searchRow.add(btnSearch);
        searchRow.add(btnAll);

        JPanel topCards = new JPanel(new GridLayout(2, 1, 0, 10));
        topCards.setBackground(LIGHT);
        topCards.add(ctrl);
        topCards.add(searchRow);

        // ── Table ─────────────────────────────────────────────────────────────
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(WHITE);
        tableCard.setBorder(new LineBorder(new Color(220,225,240), 1, true));

        JPanel tableHdr = new JPanel(new BorderLayout());
        tableHdr.setBackground(WHITE);
        tableHdr.setBorder(new EmptyBorder(10, 14, 8, 14));
        JLabel tTitle = new JLabel("Attendance Records — " + emp[2] + " " + emp[1]);
        tTitle.setFont(new Font(FONT, Font.BOLD, 14));
        tTitle.setForeground(BLUE);
        tableHdr.add(tTitle, BorderLayout.WEST);

        tblAttendance = new JTable(new DefaultTableModel(
            new String[]{"Date","Time In","Time Out","Total Hours"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        });
        styleTable(tblAttendance);

        tableCard.add(tableHdr, BorderLayout.NORTH);
        tableCard.add(new JScrollPane(tblAttendance), BorderLayout.CENTER);

        // Summary bar
        JPanel summaryBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 6));
        summaryBar.setBackground(new Color(235, 240, 252));
        summaryBar.setBorder(new MatteBorder(1,0,0,0, new Color(210,215,230)));

        JLabel lblDays  = new JLabel("Days: —");
        JLabel lblHours = new JLabel("Total Hours: —");
        JLabel lblOT    = new JLabel("Overtime: —");
        for (JLabel l : new JLabel[]{lblDays, lblHours, lblOT}) {
            l.setFont(new Font(FONT, Font.BOLD, 12));
            l.setForeground(BLUE);
            summaryBar.add(l);
        }

        // Update summary when table data changes
        tblAttendance.getModel().addTableModelListener(e2 -> {
            SwingUtilities.invokeLater(() -> {
                int rows = tblAttendance.getRowCount();
                int complete = 0;
                long totalMins = 0;
                for (int i = 0; i < rows; i++) {
                    String tout = tblAttendance.getValueAt(i, 2).toString();
                    String total = tblAttendance.getValueAt(i, 3).toString();
                    if (!tout.equals("—") && total.contains("hr")) {
                        complete++;
                        try {
                            String[] parts = total.split(" hr ");
                            int hrs = Integer.parseInt(parts[0].trim());
                            int mins = Integer.parseInt(parts[1].replace(" min","").trim());
                            totalMins += hrs * 60L + mins;
                        } catch (Exception ignored) {}
                    }
                }
                long otMins = Math.max(0, totalMins - (complete * 8L * 60));
                lblDays .setText("Days: " + rows);
                lblHours.setText("Total Hours: " + (totalMins/60) + " hr " +
                    String.format("%02d", totalMins%60) + " min");
                lblOT   .setText("Overtime: " + (otMins/60) + " hr " +
                    String.format("%02d", otMins%60) + " min");
            });
        });

        tableCard.add(summaryBar, BorderLayout.SOUTH);

        body.add(topCards,  BorderLayout.NORTH);
        body.add(tableCard, BorderLayout.CENTER);
        main.add(body, BorderLayout.CENTER);
        return main;
    }

    private void loadAttendance() {
        Attendance.filterAttendance(tblAttendance, empNo, "");
    }

    private void deleteSelectedEntry() {
        int row = tblAttendance.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this,
                "Select an attendance row to delete first.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String date    = tblAttendance.getValueAt(row, 0).toString();
        String timeIn  = tblAttendance.getValueAt(row, 1).toString();
        String timeOut = tblAttendance.getValueAt(row, 2).toString();
        int ok = JOptionPane.showConfirmDialog(this,
            "Delete this attendance entry?\n\nDate: " + date +
            "\nTime In: " + timeIn + "\nTime Out: " + timeOut,
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        if (Attendance.deleteEntry(empNo, date, timeIn, timeOut)) {
            Toast.success(this, "Attendance entry deleted.");
            loadAttendance();
        } else {
            JOptionPane.showMessageDialog(this,
                "Could not find the entry to delete.",
                "Not Found", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startClock() {
        clockTimer = new Timer(1000, e ->
            lblClock.setText(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEE, MMM dd  |  hh:mm:ss a"))));
        clockTimer.start();
    }

    private void stopClock() {
        if (clockTimer != null) clockTimer.stop();
    }

    private void styleTable(JTable t) {
        t.setFont(new Font(FONT, Font.PLAIN, 13));
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0,0));
        t.setSelectionBackground(new Color(200,215,255));
        t.getTableHeader().setFont(new Font(FONT, Font.BOLD, 13));
        t.getTableHeader().setBackground(BLUE);
        t.getTableHeader().setForeground(WHITE);
        t.getTableHeader().setPreferredSize(new Dimension(0, 34));

        // Color-coded rows:
        // Green  = complete (has both time in and out, >= 8 hrs)
        // Amber  = short shift (< 8 hrs but has both)
        // Red    = missing time out
        // Default = alternating white/light
        DefaultTableCellRenderer cr = new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setHorizontalAlignment(CENTER);
                if (!sel) {
                    String timeOut = t.getValueAt(row, 2).toString();
                    String total   = t.getValueAt(row, 3).toString();
                    if (timeOut.equals("—") || timeOut.isEmpty()) {
                        // Missing time out — red tint
                        setBackground(new Color(255, 235, 235));
                        setForeground(new Color(150, 30, 30));
                    } else if (total.contains("hr")) {
                        // Parse hours from "X hr YY min"
                        try {
                            int hrs = Integer.parseInt(total.split(" hr")[0].trim());
                            if (hrs >= 8) {
                                setBackground(new Color(235, 255, 240));
                                setForeground(new Color(20, 120, 50));
                            } else {
                                setBackground(new Color(255, 248, 230));
                                setForeground(new Color(150, 90, 0));
                            }
                        } catch (Exception e) {
                            setBackground(row % 2 == 0 ? WHITE : LIGHT);
                            setForeground(new Color(30, 30, 50));
                        }
                    } else {
                        setBackground(row % 2 == 0 ? WHITE : LIGHT);
                        setForeground(new Color(30, 30, 50));
                    }
                }
                return this;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(cr);
    }
}
