/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package motorphpayrollsystem;

import javax.swing.JOptionPane;

/**
 *
 * @author Ally
 */
public class editProfileUI extends javax.swing.JFrame {

    /**
     * Creates new form editProfileUI
     */
    
    private String empNo;
    private String[] emp;
    
    public editProfileUI(String empNo,
        String[] emp) {
        initComponents();
        
        this.empNo = empNo;
        this.emp = emp;
        
        // Initializing data fields into components
        lblEmployeeNumber.setText(EmployeeData.getEmpNo(emp));
        lblLastName.setText(EmployeeData.getLastName(emp));
        lblFirstName.setText(EmployeeData.getFirstName(emp));
        lblBirthday.setText(EmployeeData.getBirthday(emp));
        txtAddress.setText(EmployeeData.getAddress(emp));
        txtPhone.setText(EmployeeData.getPhoneNo(emp));
        lblPosition.setText(EmployeeData.getPosition(emp));
        lblStatus.setText(EmployeeData.getStatus(emp));
        lblSupervisor.setText(EmployeeData.getSupervisor(emp));
        
       // =====================================================================
        // MASTER UNIFORM SIDEBAR LAYOUT MAP: Edit Profile Screen
        // =====================================================================
        this.setSize(900, 600);
        this.setLocationRelativeTo(null); 
        getContentPane().setLayout(null);
        
        // Size base containers perfectly
        jPanel2.setBounds(0, 0, 150, 600);    // Blue Sidebar panel map
        jPanel1.setBounds(150, 0, 750, 600);   // White Workspace content panel map
        
        jPanel2.setLayout(null); 
        jPanel1.setLayout(null); // Kill workspace layout engine for absolute positioning
        
        // =====================================================================
        // 2. CENTRALIZING EDIT PROFILE CONTENT BLOCK
        // =====================================================================
        
        // --- Header Title ---
        jLabel12.setBounds(30, 20, 150, 50);  // "Edit Profile" Header label

        // --- Profile Icon Avatar Alignment ---
        jLabel2.setBounds(80, 150, 150, 150); 

        // --- Core Text Fields & Data Grid Mapping Matrix ---
        int labelX = 260; 
        int inputX = 380;
        int startY = 130;
        int rowGap = 25;

        // Static Text Labels vs Dynamic Value Labels/Inputs
        jLabel4.setBounds(labelX, startY, 110, 20);      lblEmployeeNumber.setBounds(inputX, startY, 300, 20);
        jLabel14.setBounds(labelX, startY + rowGap, 110, 20);   lblLastName.setBounds(inputX, startY + rowGap, 300, 20);
        jLabel15.setBounds(labelX, startY + rowGap*2, 110, 20); lblFirstName.setBounds(inputX, startY + rowGap*2, 300, 20);
        jLabel19.setBounds(labelX, startY + rowGap*3, 110, 20); lblBirthday.setBounds(inputX, startY + rowGap*3, 300, 20);
        
        // Editable Form Elements
        jLabel21.setBounds(labelX, startY + rowGap*4, 110, 20); txtAddress.setBounds(inputX, startY + rowGap*4, 320, 22);
        jLabel11.setBounds(labelX, startY + rowGap*5, 110, 20); txtPhone.setBounds(inputX, startY + rowGap*5, 200, 22);
        
        // Remaining Corporate Metadata Labels
        jLabel23.setBounds(labelX, startY + rowGap*6, 110, 20); lblPosition.setBounds(inputX, startY + rowGap*6, 300, 20);
        jLabel22.setBounds(labelX, startY + rowGap*7, 110, 20); lblStatus.setBounds(inputX, startY + rowGap*7, 300, 20);
        jLabel26.setBounds(labelX, startY + rowGap*8, 110, 20); lblSupervisor.setBounds(inputX, startY + rowGap*8, 300, 20);

        // Center bottom action save button alignment positioning
        btnEditProfile.setBounds(500, 480, 150, 40);

        // =====================================================================
        // 3. MASTER UNIFORM SIDEBAR LOCK (Matches Attendance Modules Perfectly)
        // =====================================================================
        jLabel3.setBounds(15, 20, 120, 64);   // Corporate Brand Logo Asset 
        
        btnMainMenu.setBounds(15, 120, 33, 32);   jLabel5.setBounds(55, 120, 90, 32);   
        btnProfile.setBounds(15, 170, 33, 32);    jLabel10.setBounds(55, 170, 90, 32);  
        btnPayroll.setBounds(15, 220, 33, 32);    jLabel6.setBounds(55, 220, 90, 32);   
        btnAttendance.setBounds(15, 270, 33, 32); jLabel7.setBounds(55, 270, 90, 32);    
        btnLogout.setBounds(15, 370, 33, 32);     jLabel9.setBounds(55, 370, 90, 32);   

        // Admin tab is not shown on the self-service Edit Profile screen.
        // Hide it and shift Logout up into the freed slot so there's no gap.
        btnAdmin.setVisible(false);
        jLabel8.setVisible(false);
        btnLogout.setBounds(15, 320, 33, 32);
        jLabel9.setBounds(55, 320, 90, 32);

        // =====================================================================
        // 4. RENDERING INTERFACE UPDATE FLUSH
        // =====================================================================
        jPanel1.revalidate();
        jPanel1.repaint();
        jPanel2.revalidate();
        jPanel2.repaint();
        
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        lblEmployeeNumber = new javax.swing.JLabel();
        lblFirstName = new javax.swing.JLabel();
        lblLastName = new javax.swing.JLabel();
        jLabel19 = new javax.swing.JLabel();
        lblBirthday = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        lblPosition = new javax.swing.JLabel();
        lblStatus = new javax.swing.JLabel();
        jLabel26 = new javax.swing.JLabel();
        lblSupervisor = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        btnEditProfile = new javax.swing.JButton();
        txtAddress = new javax.swing.JTextField();
        txtPhone = new javax.swing.JTextField();
        jPanel2 = new javax.swing.JPanel();
        btnLogout = new javax.swing.JButton();
        btnAdmin = new javax.swing.JButton();
        btnProfile = new javax.swing.JButton();
        btnPayroll = new javax.swing.JButton();
        btnAttendance = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        btnMainMenu = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(1366, 800));

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/profileicon_dash.jpg"))); // NOI18N

        jLabel4.setText("Employee No.");

        jLabel11.setText("Phone No:");

        jLabel14.setText("Last Name:");

        jLabel15.setText("First Name:");

        lblEmployeeNumber.setText("jLabel4");

        lblFirstName.setText("jLabel4");

        lblLastName.setText("jLabel4");

        jLabel19.setText("Birthday:");

        lblBirthday.setText("jLabel4");

        jLabel21.setText("Address:");

        jLabel22.setText("Status:");

        jLabel23.setText("Position:");

        lblPosition.setText("jLabel4");

        lblStatus.setText("jLabel4");

        jLabel26.setText("Supervisor:");

        lblSupervisor.setText("jLabel4");

        jLabel12.setBackground(new java.awt.Color(255, 153, 0));
        jLabel12.setFont(new java.awt.Font("Dialog",1, 24)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Edit Profile");
        jLabel12.setOpaque(true);

        btnEditProfile.setBackground(new java.awt.Color(0, 51, 153));
        btnEditProfile.setFont(new java.awt.Font("Dialog",1, 18)); // NOI18N
        btnEditProfile.setForeground(new java.awt.Color(255, 255, 255));
        btnEditProfile.setText("Save");
        btnEditProfile.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        btnEditProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditProfileActionPerformed(evt);
            }
        });

        txtAddress.setText("jTextField1");

        txtPhone.setText("jTextField2");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 138, Short.MAX_VALUE)
                        .addGap(22, 22, 22))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel14, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(jLabel19, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel15, javax.swing.GroupLayout.Alignment.LEADING))
                            .addComponent(jLabel11)
                            .addComponent(jLabel21, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel22, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(27, 27, 27))
                    .addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblEmployeeNumber, javax.swing.GroupLayout.DEFAULT_SIZE, 380, Short.MAX_VALUE)
                    .addComponent(lblLastName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblFirstName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblBirthday, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblPosition, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSupervisor, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnEditProfile, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(102, 102, 102))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel14)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel15)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel19)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel21)
                                    .addComponent(txtAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(txtPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(lblEmployeeNumber)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblLastName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblFirstName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblBirthday)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel23)
                    .addComponent(lblPosition))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel22)
                    .addComponent(lblStatus))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel26)
                    .addComponent(lblSupervisor))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEditProfile)
                .addGap(33, 33, 33))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(160, -20, 650, 420));

        jPanel2.setBackground(new java.awt.Color(0, 51, 153));

        btnLogout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/logout.jpg"))); // NOI18N
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        btnAdmin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/admin.jpg"))); // NOI18N
        btnAdmin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAdminActionPerformed(evt);
            }
        });

        btnProfile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/userprofile.jpg"))); // NOI18N
        btnProfile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProfileActionPerformed(evt);
            }
        });

        btnPayroll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/payroll.jpg"))); // NOI18N
        btnPayroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPayrollActionPerformed(evt);
            }
        });

        btnAttendance.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/attendance.jpg"))); // NOI18N
        btnAttendance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAttendanceActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/logo.jpg"))); // NOI18N

        jLabel5.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Main Menu");

        jLabel6.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Payroll");

        jLabel7.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Attendance");

        jLabel8.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Admin");

        jLabel9.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Logout");

        btnMainMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/motorphpayrollsystem/images/mainmenu.jpg"))); // NOI18N
        btnMainMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnMainMenuActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Dialog",1, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Profile");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(117, 117, 117)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addComponent(btnMainMenu, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(btnProfile, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(btnPayroll, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(btnAttendance, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addComponent(btnLogout, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(btnAdmin, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel3)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(btnMainMenu, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                            .addComponent(jLabel10)
                                            .addComponent(btnProfile, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(btnPayroll, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING))
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(btnAttendance, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnAdmin, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel8, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel9, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap(79, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 150, 400));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /** Navigate preserving window state and monitor position (flash-free). */
    private void nav(java.util.function.Supplier<javax.swing.JFrame> factory) {
        final int                            state = getExtendedState();
        final java.awt.Dimension             size  = getSize();
        final java.awt.Point                 loc   = getLocation();
        final java.awt.GraphicsConfiguration gc    = getGraphicsConfiguration();

        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.JFrame next = factory.get();

                if ((state & javax.swing.JFrame.MAXIMIZED_BOTH) != 0) {
                    java.awt.Rectangle screen = gc.getBounds();
                    next.setLocation(screen.x + 10, screen.y + 10);
                    next.setVisible(true);
                    next.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
                } else {
                    next.setSize(size);
                    next.setLocation(loc);
                    next.setVisible(true);
                }

                dispose();   // dispose old AFTER new is visible -> no flicker
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void btnEditProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditProfileActionPerformed

        String address = txtAddress.getText().trim();
        String phoneNumber = txtPhone.getText().trim();

        if (address.isEmpty() || phoneNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Address and Phone Number cannot be blank.",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() < 7 || digits.length() > 12) {
            JOptionPane.showMessageDialog(this,
                "Please enter a valid Phone Number (7-12 digits).",
                "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
            "Save changes to your profile?",
            "Confirm Update", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        emp[4] = address;
        emp[5] = phoneNumber;
        EmployeeData.updateOwnProfile(emp, address, phoneNumber);
        Toast.success(this, "Profile updated successfully.");

        nav(() -> new ProfileUI(empNo, emp));
    }//GEN-LAST:event_btnEditProfileActionPerformed

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed

        nav(() -> new loginUI());
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnAdminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAdminActionPerformed

       if(!EmployeeData.isHR(emp)){

        JOptionPane.showMessageDialog(null,

            "Access Denied.\n"
            + "Only HR employees can access "
            + "the Admin Module.",

            "Unauthorized Access",

            JOptionPane.ERROR_MESSAGE);

         return;
        }
        nav(() -> new adminModuleUI(empNo, emp));
    }//GEN-LAST:event_btnAdminActionPerformed

    private void btnProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProfileActionPerformed

        nav(() -> new ProfileUI(empNo, emp));
    }//GEN-LAST:event_btnProfileActionPerformed

    private void btnPayrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPayrollActionPerformed

        nav(() -> new PayrollAdminUI(empNo, emp));
    }//GEN-LAST:event_btnPayrollActionPerformed

    private void btnAttendanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAttendanceActionPerformed

        nav(() -> new AttendanceUI(empNo, emp));
    }//GEN-LAST:event_btnAttendanceActionPerformed

    private void btnMainMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnMainMenuActionPerformed

        nav(() -> new DashboardUI(empNo, emp));
    }//GEN-LAST:event_btnMainMenuActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(editProfileUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(editProfileUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(editProfileUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(editProfileUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
   //             new editProfileUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAdmin;
    private javax.swing.JButton btnAttendance;
    private javax.swing.JButton btnEditProfile;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnMainMenu;
    private javax.swing.JButton btnPayroll;
    private javax.swing.JButton btnProfile;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel26;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel lblBirthday;
    private javax.swing.JLabel lblEmployeeNumber;
    private javax.swing.JLabel lblFirstName;
    private javax.swing.JLabel lblLastName;
    private javax.swing.JLabel lblPosition;
    private javax.swing.JLabel lblStatus;
    private javax.swing.JLabel lblSupervisor;
    private javax.swing.JTextField txtAddress;
    private javax.swing.JTextField txtPhone;
    // End of variables declaration//GEN-END:variables
}
