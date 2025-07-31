import java.sql.*;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class CheckIn extends javax.swing.JFrame {

    private Connection connection;
    private DefaultTableModel tableModel;
    
    public CheckIn() {
        initComponents();
        initializeDatabase();
        setupTableModel();
        loadPendingBookings();
    }
    
    // Initialize database connection
    private void initializeDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/hotel-system";
            String username = "root"; // Change to your database username
            String password = ""; // Change to your database password
            connection = DriverManager.getConnection(url, username, password);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage(), 
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Setup table model with proper column names
    private void setupTableModel() {
        String[] columnNames = {"Booking ID", "Customer Name", "Phone", "Room No", "Room Type", "Booking Date"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        jTable1.setModel(tableModel);
        
        // Set column widths
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(80);  // Booking ID
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(150); // Customer Name
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(120); // Phone
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(80);  // Room No
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(100); // Room Type
        jTable1.getColumnModel().getColumn(5).setPreferredWidth(120); // Booking Date
    }
    
    // Load pending bookings from database
    private void loadPendingBookings() {
        if (connection == null) {
            JOptionPane.showMessageDialog(this, "No database connection available", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            // Query to get bookings that haven't been checked in yet
            String query = """
    SELECT b.booking_id, c.name AS customer_name, c.phone, b.room_no, 
           b.room_type, b.booking_date
    FROM bookings b
    JOIN customers c ON b.customer_id = c.customer_id
    WHERE b.booking_status = 'confirmed' OR b.booking_status = 'booked'
    ORDER BY b.booking_date ASC
""";

            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            // Clear existing table data
            tableModel.setRowCount(0);
            
            // Add data to table
            while (rs.next()) {
                Object[] row = {
                    rs.getInt("booking_id"),
                    rs.getString("customer_name"),
                    rs.getString("phone"),
                    rs.getString("room_no"),
                    rs.getString("room_type"),
                    rs.getDate("booking_date")
                };
                tableModel.addRow(row);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading bookings: " + e.getMessage(), 
                                        "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Check in selected customer
private void checkInCustomer() {
    int selectedRow = jTable1.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select a booking to check in.",
                "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
    }

    if (jDateChooser1.getDate() == null) {
        JOptionPane.showMessageDialog(this, "Please select a check-in date.",
                "No Date Selected", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        int bookingId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String customerName = (String) tableModel.getValueAt(selectedRow, 1);
        int roomNo = Integer.parseInt(tableModel.getValueAt(selectedRow, 3).toString());
        String roomType = (String) tableModel.getValueAt(selectedRow, 4);

        // ✅ Create Room object using Factory Pattern
        Room room = RoomFactory.createRoom(roomType);
        System.out.println("Room booked of type: " + room.getRoomType());

        // Convert selected date to SQL date
        java.util.Date selectedDate = jDateChooser1.getDate();
        java.sql.Date checkInDate = new java.sql.Date(selectedDate.getTime());

        // Update booking status and add check-in date
        String updateQuery = """
            UPDATE bookings 
            SET booking_status = 'checked_in', checkin_date = ? 
            WHERE booking_id = ?
        """;

        PreparedStatement stmt = connection.prepareStatement(updateQuery);
        stmt.setDate(1, checkInDate);
        stmt.setInt(2, bookingId);

        int rowsUpdated = stmt.executeUpdate();
        stmt.close();

        if (rowsUpdated > 0) {
            // ✅ Update room availability to 'Booked'
            String roomUpdate = "UPDATE rooms SET availability = 'Booked' WHERE room_no = ?";
            PreparedStatement roomStmt = connection.prepareStatement(roomUpdate);
            roomStmt.setInt(1, roomNo);
            roomStmt.executeUpdate();
            roomStmt.close();

            JOptionPane.showMessageDialog(this,
                    String.format("Successfully checked in %s to room %d (%s)", customerName, roomNo, room.getRoomType()),
                    "Check-in Successful", JOptionPane.INFORMATION_MESSAGE);

            // Refresh the table
            loadPendingBookings();

            // Clear the date chooser
            jDateChooser1.setDate(null);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to update booking status.",
                    "Update Failed", JOptionPane.ERROR_MESSAGE);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error during check-in: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    } catch (IllegalArgumentException e) {
        JOptionPane.showMessageDialog(this, "Invalid room type: " + e.getMessage(),
                "Invalid Room Type", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}



    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        manageBtn = new javax.swing.JButton();
        checkinBtn = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        bookBtn = new javax.swing.JButton();
        checkoutBtn = new javax.swing.JButton();
        custBtn = new javax.swing.JButton();
        servicesBtn = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setForeground(new java.awt.Color(255, 255, 255));

        jPanel3.setBackground(new java.awt.Color(0, 102, 102));

        manageBtn.setBackground(new java.awt.Color(0, 102, 102));
        manageBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        manageBtn.setForeground(new java.awt.Color(255, 255, 255));
        manageBtn.setText("Manage Rooms");
        manageBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                manageBtnActionPerformed(evt);
            }
        });

        checkinBtn.setBackground(new java.awt.Color(0, 102, 102));
        checkinBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        checkinBtn.setForeground(new java.awt.Color(255, 255, 255));
        checkinBtn.setText("CheckIn");
        checkinBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkinBtnActionPerformed(evt);
            }
        });

        jButton2.setBackground(new java.awt.Color(51, 255, 51));
        jButton2.setFont(new java.awt.Font("Segoe UI", 3, 18)); // NOI18N
        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setText("Logout");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));

        bookBtn.setBackground(new java.awt.Color(0, 102, 102));
        bookBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        bookBtn.setForeground(new java.awt.Color(255, 255, 255));
        bookBtn.setText("Book Rooms");
        bookBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bookBtnActionPerformed(evt);
            }
        });

        checkoutBtn.setBackground(new java.awt.Color(0, 102, 102));
        checkoutBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        checkoutBtn.setForeground(new java.awt.Color(255, 255, 255));
        checkoutBtn.setText("CheckOut");
        checkoutBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkoutBtnActionPerformed(evt);
            }
        });

        custBtn.setBackground(new java.awt.Color(0, 102, 102));
        custBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        custBtn.setForeground(new java.awt.Color(255, 255, 255));
        custBtn.setText("Customers");
        custBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                custBtnActionPerformed(evt);
            }
        });

        servicesBtn.setBackground(new java.awt.Color(0, 102, 102));
        servicesBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        servicesBtn.setForeground(new java.awt.Color(255, 255, 255));
        servicesBtn.setText("Services");
        servicesBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                servicesBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(258, 258, 258))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 117, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(manageBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(bookBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(checkinBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(checkoutBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(custBtn, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(servicesBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 115, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap(105, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(25, 25, 25)
                .addComponent(manageBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(bookBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkinBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(checkoutBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(custBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(servicesBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(85, 85, 85)
                .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setText("HOTEL MANAGEMENT SYSTEM");

        jLabel2.setBackground(new java.awt.Color(0, 0, 102));
        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("CheckIn Rooms");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 102, 102));

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Cust. Name", "Phone", "Room No", "Room Type"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable1.setSelectionBackground(new java.awt.Color(0, 102, 102));
        jTable1.setSelectionForeground(new java.awt.Color(255, 255, 255));
        jScrollPane1.setViewportView(jTable1);
        if (jTable1.getColumnModel().getColumnCount() > 0) {
            jTable1.getColumnModel().getColumn(0).setResizable(false);
            jTable1.getColumnModel().getColumn(1).setResizable(false);
            jTable1.getColumnModel().getColumn(2).setResizable(false);
            jTable1.getColumnModel().getColumn(3).setResizable(false);
        }

        jButton1.setBackground(new java.awt.Color(0, 102, 102));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("CheckIn Customer");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 161, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel4)
                            .addGap(659, 659, 659))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel3)
                            .addGap(145, 145, 145))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                            .addComponent(jButton1)
                            .addGap(66, 66, 66)
                            .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(39, 39, 39))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 644, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addContainerGap()))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(262, 262, 262))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(456, 456, 456))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 812, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 540, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void manageBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_manageBtnActionPerformed
        ManageRooms ManageRoomsFrame = new ManageRooms();
        ManageRoomsFrame.setVisible(true);
        ManageRoomsFrame.pack();
        ManageRoomsFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_manageBtnActionPerformed

    private void checkinBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkinBtnActionPerformed
        CheckIn CheckInFrame = new CheckIn();
        CheckInFrame.setVisible(true);
        CheckInFrame.pack();
        CheckInFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_checkinBtnActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        Login LoginFrame = new Login();
        LoginFrame.setVisible(true);
        LoginFrame.pack();
        LoginFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_jButton2ActionPerformed

    private void bookBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookBtnActionPerformed
        BookRooms BookRoomsFrame = new BookRooms();
        BookRoomsFrame.setVisible(true);
        BookRoomsFrame.pack();
        BookRoomsFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_bookBtnActionPerformed

    private void checkoutBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkoutBtnActionPerformed
        CheckOut CheckOutFrame = new CheckOut();
        CheckOutFrame.setVisible(true);
        CheckOutFrame.pack();
        CheckOutFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_checkoutBtnActionPerformed

    private void custBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_custBtnActionPerformed
        ManageCustomers ManageCustomersFrame = new ManageCustomers();
        ManageCustomersFrame.setVisible(true);
        ManageCustomersFrame.pack();
        ManageCustomersFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_custBtnActionPerformed

    private void servicesBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_servicesBtnActionPerformed
        Services ServicesFrame = new Services();
        ServicesFrame.setVisible(true);
        ServicesFrame.pack();
        ServicesFrame.setLocationRelativeTo(null);
        this.dispose();
    }//GEN-LAST:event_servicesBtnActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        checkInCustomer();
    }//GEN-LAST:event_jButton1ActionPerformed

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
            java.util.logging.Logger.getLogger(CheckIn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CheckIn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CheckIn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CheckIn.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CheckIn().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton bookBtn;
    private javax.swing.JButton checkinBtn;
    private javax.swing.JButton checkoutBtn;
    private javax.swing.JButton custBtn;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton manageBtn;
    private javax.swing.JButton servicesBtn;
    // End of variables declaration//GEN-END:variables
}
