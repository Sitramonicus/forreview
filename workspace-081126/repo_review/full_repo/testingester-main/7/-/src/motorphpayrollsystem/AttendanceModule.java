package motorphpayrollsystem;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class AttendanceModule {

    private static final Color CUSTOM_BLUE = new Color(13, 71, 161);
    private static final Color CUSTOM_RED = new Color(229, 57, 53);
    private static final Color CUSTOM_YELLOW = new Color(255, 193, 7);
    private static final Color BG_LIGHT = new Color(245, 247, 250);
    private static final Color TEXT_DARK = new Color(33, 37, 41);

    public static JPanel buildAttendancePanel(final JFrame parentFrame, final String[] currentEmp) {
        JPanel attendancePanel = new JPanel();
        attendancePanel.setBackground(BG_LIGHT);
        attendancePanel.setLayout(null); 

        // Extract active logged-in employee number safely
        final String empNumber = (currentEmp != null && currentEmp.length > 0) ? currentEmp[0].trim() : "UNKNOWN";

        // Generate today's baseline date string to match your layout format
        final String todayDateStr = new SimpleDateFormat("MM/dd/yyyy").format(new Date());

        // Title Header
        JLabel lblTitle = new JLabel("ATTENDANCE");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_DARK);
        lblTitle.setBounds(30, 20, 200, 35);
        attendancePanel.add(lblTitle);

        JPanel titleLine = new JPanel();
        titleLine.setBackground(CUSTOM_YELLOW);
        titleLine.setBounds(30, 58, 45, 3);
        attendancePanel.add(titleLine);

        // TIME IN Button
        JButton btnTimeIn = new JButton("  TIME IN");
        try {
            ImageIcon iconInRaw = new ImageIcon(AttendanceModule.class.getResource("/motorphpayrollsystem/images/smallTimeIn.png"));
            Image imgInScaled = iconInRaw.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            btnTimeIn.setIcon(new ImageIcon(imgInScaled));
            btnTimeIn.setHorizontalTextPosition(SwingConstants.RIGHT);
        } catch (Exception e) {
            System.out.println("Dev Note: Place smallTimein.png in root folder.");
        }
        btnTimeIn.setBackground(CUSTOM_YELLOW);
        btnTimeIn.setForeground(TEXT_DARK);
        btnTimeIn.setFont(new Font("Arial", Font.BOLD, 14));
        btnTimeIn.setBounds(30, 85, 175, 60);
        attendancePanel.add(btnTimeIn);

        // Status Tracker Labels
        JLabel lblStatusHeader = new JLabel("STATUS", SwingConstants.CENTER);
        lblStatusHeader.setBounds(230, 85, 150, 20);
        attendancePanel.add(lblStatusHeader);

        final JLabel lblStatusValue = new JLabel("Not Timed In", SwingConstants.CENTER);
        lblStatusValue.setFont(new Font("Arial", Font.BOLD, 18));
        lblStatusValue.setForeground(CUSTOM_RED);
        lblStatusValue.setBounds(215, 110, 180, 30);
        attendancePanel.add(lblStatusValue);

        // TIME OUT Button
        JButton btnTimeOut = new JButton("  TIME OUT");
        try {
            ImageIcon iconOutRaw = new ImageIcon(AttendanceModule.class.getResource("/motorphpayrollsystem/images/smallTimeOut.png"));
            Image imgOutScaled = iconOutRaw.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            btnTimeOut.setIcon(new ImageIcon(imgOutScaled));
            btnTimeOut.setHorizontalTextPosition(SwingConstants.RIGHT);
        } catch (Exception e) {
            System.out.println("Dev Note: Place smallTimeout.png in root folder.");
        }
        btnTimeOut.setBackground(CUSTOM_RED);
        btnTimeOut.setForeground(Color.WHITE);
        btnTimeOut.setFont(new Font("Arial", Font.BOLD, 14));
        btnTimeOut.setBounds(430, 85, 175, 60);
        attendancePanel.add(btnTimeOut);

        // Title Section
        JLabel lblRecordsTitle = new JLabel("ATTENDANCE RECORDS");
        lblRecordsTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblRecordsTitle.setForeground(CUSTOM_BLUE);
        lblRecordsTitle.setBounds(30, 180, 220, 25);
        attendancePanel.add(lblRecordsTitle);

        final JComboBox<String> cmbDateFilter = new JComboBox<>();
        cmbDateFilter.setBounds(440, 180, 150, 25);
        attendancePanel.add(cmbDateFilter);

        String[] columns = {"Date", "Time In", "Time Out", "Total Hours", "Status"};
        final DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        // POPULATE UNIQUE DATES IN THE DROPDOWN
        ArrayList<String> uniqueDates = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("attendance.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] dataRow = line.split(",", -1);
                if (dataRow.length >= 6) {
                    String rowEmpNo = dataRow[0].trim();
                    String dateVal = dataRow[3].trim();
                    if (rowEmpNo.equalsIgnoreCase(empNumber) && !dateVal.isEmpty()) {
                        if (!uniqueDates.contains(dateVal)) {
                            uniqueDates.add(dateVal);
                        }
                    }
                }
            }
            
            for (String uniqueDate : uniqueDates) {
                cmbDateFilter.addItem(uniqueDate);
            }
            
            if (!uniqueDates.contains(todayDateStr)) {
                cmbDateFilter.addItem(todayDateStr);
            }
            
            cmbDateFilter.setSelectedItem(todayDateStr);

        } catch (Exception e) {
            cmbDateFilter.addItem(todayDateStr);
            cmbDateFilter.setSelectedItem(todayDateStr);
        }

        // FILTER RE-LOAD ROUTINE
        final Runnable reloadFilteredData = new Runnable() {
            @Override
            public void run() {
                tableModel.setRowCount(0);
                String selectedDate = (cmbDateFilter.getSelectedItem() != null) ? cmbDateFilter.getSelectedItem().toString() : "";
                if (selectedDate.isEmpty()) return;

                String currentLiveStatus = "Not Timed In";

                try (BufferedReader br = new BufferedReader(new FileReader("attendance.csv"))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] dataRow = line.split(",", -1);
                        if (dataRow.length >= 6) {
                            String rowEmpNo = dataRow[0].trim();
                            String dateVal  = dataRow[3].trim();

                            if (rowEmpNo.equalsIgnoreCase(empNumber) && dateVal.equals(selectedDate)) {
                                String timeInVal = dataRow[4].trim(); 
                                String timeOutVal = dataRow[5].trim(); 
                                String totalHoursVal = calculateTotalHours(timeInVal, timeOutVal);
                                String rowStatus = "Not Timed In";
                                
                                if (!timeInVal.isEmpty() && timeOutVal.isEmpty()) {
                                    rowStatus = "Timed In";
                                    currentLiveStatus = "Timed In";
                                } else if (!timeInVal.isEmpty() && !timeOutVal.isEmpty()) {
                                    rowStatus = "Completed";
                                    currentLiveStatus = "Completed";
                                }

                                tableModel.insertRow(0, new Object[]{selectedDate, timeInVal, timeOutVal, totalHoursVal, rowStatus});
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Dev Note: Finished processing reader rows.");
                }

                if (currentLiveStatus.equals("Timed In")) {
                    lblStatusValue.setText("Timed In");
                    lblStatusValue.setForeground(new Color(255, 140, 0));
                } else if (currentLiveStatus.equals("Completed")) {
                    lblStatusValue.setText("Completed");
                    lblStatusValue.setForeground(new Color(46, 125, 50));
                } else if (selectedDate.equals(todayDateStr)) {
                    lblStatusValue.setText("Not Timed In");
                    lblStatusValue.setForeground(CUSTOM_RED);
                    tableModel.addRow(new Object[]{todayDateStr, "", "", "", "Not Timed In"});
                }
            }
        };

        reloadFilteredData.run();

        cmbDateFilter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                reloadFilteredData.run();
            }
        });

        JTable recordsTable = new JTable(tableModel);
        recordsTable.setRowHeight(32);
        
        recordsTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null) {
                    String st = value.toString().trim();
                    if (st.equalsIgnoreCase("Not Timed In")) cell.setForeground(CUSTOM_RED);
                    else if (st.equalsIgnoreCase("Timed In")) cell.setForeground(new Color(255, 140, 0));
                    else if (st.equalsIgnoreCase("Completed")) cell.setForeground(new Color(46, 125, 50));
                }
                return cell;
            }
        });

        JScrollPane tableScroll = new JScrollPane(recordsTable);
        tableScroll.setBounds(30, 220, 660, 220);
        attendancePanel.add(tableScroll);

        // TIME IN CLICK ACTION
        btnTimeIn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!cmbDateFilter.getSelectedItem().toString().equals(todayDateStr)) {
                    JOptionPane.showMessageDialog(parentFrame, "You can only record attendance logs for the current date: " + todayDateStr);
                    return;
                }
                
                if (lblStatusValue.getText().equals("Timed In")) {
                    JOptionPane.showMessageDialog(parentFrame, "You are already Timed In! Please Time Out before starting a new session.");
                    return;
                }
                if (lblStatusValue.getText().equals("Completed")) {
                    JOptionPane.showMessageDialog(parentFrame, "Shift already completed for today!");
                    return;
                }

                String time = new SimpleDateFormat("HH:mm").format(new Date());

                if (tableModel.getRowCount() == 1 && tableModel.getValueAt(0, 1).toString().isEmpty()) {
                    tableModel.setRowCount(0);
                }

                lblStatusValue.setText("Timed In");
                lblStatusValue.setForeground(new Color(255, 140, 0));
                
                tableModel.insertRow(0, new Object[]{todayDateStr, time, "", "", "Timed In"});
                saveRecordToCSV(empNumber, "", "", todayDateStr, time, "");
            }
        });

        // TIME OUT CLICK ACTION (SECURED PRO PROCEDURAL BOUNDS MATCH CHECKER)
        btnTimeOut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!cmbDateFilter.getSelectedItem().toString().equals(todayDateStr)) {
                    JOptionPane.showMessageDialog(parentFrame, "You can only record attendance logs for the current date: " + todayDateStr);
                    return;
                }
                
                // 🛡️ INDEX SAFETY GUARD: Verify row indexes exist to prevent ArrayIndexOutOfBoundsException
                if (tableModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(parentFrame, "Please execute a Time-In entry first!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Object tableTimeInObj = tableModel.getValueAt(0, 1);
                String timeInStr = (tableTimeInObj != null) ? tableTimeInObj.toString().trim() : "";

                // 🛡️ LOGICAL STATE SAFETY GUARD: Verify the text contents are valid strings
                if (timeInStr.isEmpty() || timeInStr.equals("-")) {
                    JOptionPane.showMessageDialog(parentFrame, "Please execute a Time-In entry first!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (lblStatusValue.getText().equals("Completed")) {
                    JOptionPane.showMessageDialog(parentFrame, "Shift already completed for today!");
                    return;
                }

                String timeOut = new SimpleDateFormat("HH:mm").format(new Date());
                String hoursStr = calculateTotalHours(timeInStr, timeOut);

                lblStatusValue.setText("Completed");
                lblStatusValue.setForeground(new Color(46, 125, 50));
                
                tableModel.setValueAt(timeOut, 0, 2);
                tableModel.setValueAt(hoursStr, 0, 3);
                tableModel.setValueAt("Completed", 0, 4);

                saveRecordToCSV(empNumber, "", "", todayDateStr, timeInStr, timeOut);
                JOptionPane.showMessageDialog(parentFrame, "Time-Out Registered: " + timeOut + "\nDuration: " + hoursStr);
                
                reloadFilteredData.run();
            }
        });

        return attendancePanel;
    }

    private static String calculateTotalHours(String timeInStr, String timeOutStr) {
        if (timeInStr == null || timeOutStr == null || timeInStr.isEmpty() || timeOutStr.isEmpty() || timeInStr.equals("-") || timeOutStr.equals("-")) {
            return ""; 
        }
        try {
            String[] inParts = timeInStr.split(":");
            int inHours = Integer.parseInt(inParts[0].trim());
            int inMinutes = Integer.parseInt(inParts[1].trim());
            
            String[] outParts = timeOutStr.split(":");
            int outHours = Integer.parseInt(outParts[0].trim());
            int outMinutes = Integer.parseInt(outParts[1].trim());
            
            int totalInMinutes = (inHours * 60) + inMinutes;
            int totalOutMinutes = (outHours * 60) + outMinutes;
            
            int elapsedMinutes = totalOutMinutes - totalInMinutes;
            if (elapsedMinutes < 0) return ""; 
            
            int finalHours = elapsedMinutes / 60;
            int finalMinutes = elapsedMinutes % 60;
            
            return String.format("%02d:%02d", finalHours, finalMinutes);
        } catch (Exception e) {
            return ""; 
        }
    }

    public static void saveRecordToCSV(String empNo, String lastName, String firstName, String date, String timeIn, String timeOut) {
        try (java.io.FileWriter fw = new java.io.FileWriter("attendance.csv", true);
             java.io.PrintWriter out = new java.io.PrintWriter(new java.io.BufferedWriter(fw))) {
            out.println(empNo + "," + lastName + "," + firstName + "," + date + "," + timeIn + "," + timeOut);
        } catch (Exception ex) {
            System.out.println("Error saving to CSV.");
        }
    }
}