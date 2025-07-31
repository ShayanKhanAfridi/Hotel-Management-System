import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.awt.print.PrinterException;
import java.text.MessageFormat;

public class CheckOut extends javax.swing.JFrame {

    private Connection connection;
    private DefaultTableModel tableModel;
    private int selectedBookingId = -1;
    private double roomRate = 0.0;
    private Date checkinDate;
    
    public CheckOut() {
        initComponents();
        connectDatabase();
        setupTable();
        loadCheckedInCustomers();
    }
    
    private void connectDatabase() {
        try {
            connection = DBConnectionManager.getConnection();
            System.out.println("Connected using Singleton!");
            
            // Test the connection
            String testQuery = "SELECT COUNT(*) FROM bookings WHERE booking_status = 'checked_in'";
            PreparedStatement testStmt = connection.prepareStatement(testQuery);
            ResultSet testRs = testStmt.executeQuery();
            if (testRs.next()) {
                System.out.println("Number of checked-in customers: " + testRs.getInt(1));
            }
            testStmt.close();
            testRs.close();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTable() {
        tableModel = (DefaultTableModel) jTable1.getModel();
        tableModel.setRowCount(0); // Clear existing rows
        
        // Add table selection listener
        jTable1.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = jTable1.getSelectedRow();
                if (selectedRow >= 0) {
                    loadSelectedBookingDetails(selectedRow);
                }
            }
        });
    }
    
    private void loadCheckedInCustomers() {
        try {
            String query = "SELECT b.booking_id, c.name, c.phone, b.room_no, b.room_type, b.booking_date, r.price AS rate " +
                          "FROM bookings b " +
                          "JOIN customers c ON b.customer_id = c.customer_id " +
                          "JOIN rooms r ON b.room_no = r.room_no " +
                          "WHERE b.booking_status = 'checked_in' " +
                          "ORDER BY b.booking_date ASC";

            PreparedStatement stmt = connection.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            tableModel.setRowCount(0); // Clear existing rows
            
            while (rs.next()) {
                // Only add 4 values to match the 4 table columns
                Object[] row = {
                    rs.getString("name"),        // Customer Name
                    rs.getString("phone"),       // Phone
                    rs.getInt("room_no"),        // Room No
                    rs.getString("room_type")    // Room Type
                };
                tableModel.addRow(row);
            }
            
            stmt.close();
            rs.close();
            
            System.out.println("Loaded " + tableModel.getRowCount() + " checked-in customers");
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading customers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadSelectedBookingDetails(int selectedRow) {
        try {
            // Get values from the correct column positions (0-based indexing)
            String customerName = (String) tableModel.getValueAt(selectedRow, 0); // name (column 0)
            int roomNo = (Integer) tableModel.getValueAt(selectedRow, 2);         // room_no (column 2)

            String query = "SELECT b.booking_id, b.booking_date, r.price AS rate " +
                           "FROM bookings b " +
                           "JOIN customers c ON b.customer_id = c.customer_id " +
                           "JOIN rooms r ON b.room_no = r.room_no " +
                           "WHERE c.name = ? AND b.room_no = ? AND b.booking_status = 'checked_in'";

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, customerName);
            stmt.setInt(2, roomNo);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                selectedBookingId = rs.getInt("booking_id");
                checkinDate = rs.getDate("booking_date");
                roomRate = rs.getDouble("rate");
                
                // Debug output
                System.out.println("Selected Booking ID: " + selectedBookingId);
                System.out.println("Check-in Date: " + checkinDate);
                System.out.println("Room Rate: " + roomRate);
            }

            stmt.close();
            rs.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading booking details: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassCastException e) {
            JOptionPane.showMessageDialog(this, "Column data type mismatch: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private double calculateTotalAmount(Date checkinDate, Date checkoutDate, double roomRate) {
        if (checkinDate == null || checkoutDate == null) {
            return 0.0;
        }
        
        long timeDiff = checkoutDate.getTime() - checkinDate.getTime();
        int days = (int) (timeDiff / (1000 * 60 * 60 * 24));
        
        // Minimum 1 day charge
        if (days < 1) {
            days = 1;
        }
        
        return days * roomRate;
    }
    
    private void checkoutCustomer() {
        if (selectedBookingId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a customer to checkout.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date checkoutDate = jDateChooser1.getDate();
        if (checkoutDate == null) {
            JOptionPane.showMessageDialog(this, "Please select a checkout date.", "No Date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double totalAmount = calculateTotalAmount(checkinDate, checkoutDate, roomRate);

        try {
            connection.setAutoCommit(false); // Start transaction

            int roomNo = -1;
            int customerId = -1;

            // Step 1: Get room number and customer_id
            String getInfoQuery = "SELECT room_no, customer_id FROM bookings WHERE booking_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(getInfoQuery)) {
                stmt.setInt(1, selectedBookingId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    roomNo = rs.getInt("room_no");
                    customerId = rs.getInt("customer_id");
                }
                rs.close();
            }

            if (roomNo == -1 || customerId == -1) {
                connection.rollback();
                JOptionPane.showMessageDialog(this, "Booking or room info not found.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Step 2: Update booking
            String updateBookingQuery = "UPDATE bookings SET booking_status = 'checked_out', checkout_date = ?, total_amount = ? WHERE booking_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateBookingQuery)) {
                stmt.setDate(1, new java.sql.Date(checkoutDate.getTime()));
                stmt.setDouble(2, totalAmount);
                stmt.setInt(3, selectedBookingId);
                if (stmt.executeUpdate() == 0) {
                    connection.rollback();
                    JOptionPane.showMessageDialog(this, "Failed to update booking.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Step 3: Update room availability
            updateRoomStatus(roomNo);

            

            // Step 6: Commit transaction
            connection.commit();

            // Step 7: Show invoice and refresh UI
            generateInvoice(checkoutDate, totalAmount);
            loadCheckedInCustomers();
            selectedBookingId = -1;
            jDateChooser1.setDate(null);
            JOptionPane.showMessageDialog(this, "Customer checked out successfully!");

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            JOptionPane.showMessageDialog(this, "Error during checkout: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void updateRoomStatus(int roomNo) {
        try {
            String updateRoomQuery = "UPDATE rooms SET availability = 'Available' WHERE room_no = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateRoomQuery)) {
                stmt.setInt(1, roomNo);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating room status: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private void generateInvoice(Date checkoutDate, double totalAmount) {
        try {
            // Get booking details for invoice
            String query = "SELECT c.name, c.phone, c.address, b.room_no, b.room_type, " +
                          "b.booking_date, r.price AS rate " +
                          "FROM bookings b " +
                          "JOIN customers c ON b.customer_id = c.customer_id " +
                          "JOIN rooms r ON b.room_no = r.room_no " +
                          "WHERE b.booking_id = ?";

            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, selectedBookingId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String customerName = rs.getString("name");
                String phone = rs.getString("phone");
                String address = rs.getString("address");
                int roomNo = rs.getInt("room_no");
                String roomType = rs.getString("room_type");
                Date checkin = rs.getDate("booking_date");
                double rate = rs.getDouble("rate");
                
                // Calculate days
                long timeDiff = checkoutDate.getTime() - checkin.getTime();
                int days = Math.max(1, (int) (timeDiff / (1000 * 60 * 60 * 24)));
                
                // Create invoice text
                StringBuilder invoice = new StringBuilder();
                invoice.append("========================================\n");
                invoice.append("         HOTEL MANAGEMENT SYSTEM        \n");
                invoice.append("              CHECKOUT INVOICE          \n");
                invoice.append("========================================\n\n");
                invoice.append("Customer Details:\n");
                invoice.append("Name: ").append(customerName).append("\n");
                invoice.append("Phone: ").append(phone).append("\n");
                invoice.append("Address: ").append(address != null ? address : "N/A").append("\n\n");
                invoice.append("Booking Details:\n");
                invoice.append("Room No: ").append(roomNo).append("\n");
                invoice.append("Room Type: ").append(roomType).append("\n");
                invoice.append("Check-in Date: ").append(new SimpleDateFormat("yyyy-MM-dd").format(checkin)).append("\n");
                invoice.append("Check-out Date: ").append(new SimpleDateFormat("yyyy-MM-dd").format(checkoutDate)).append("\n");
                invoice.append("Number of Days: ").append(days).append("\n");
                invoice.append("Rate per Day: $").append(String.format("%.2f", rate)).append("\n\n");
                invoice.append("----------------------------------------\n");
                invoice.append("Total Amount: $").append(String.format("%.2f", totalAmount)).append("\n");
                invoice.append("----------------------------------------\n\n");
                invoice.append("Thank you for staying with us!\n");
                invoice.append("========================================\n");
                
                // Show invoice in dialog
                JOptionPane.showMessageDialog(this, invoice.toString(), "Invoice", JOptionPane.INFORMATION_MESSAGE);
                
                // Optional: Print the invoice
                printInvoice(invoice.toString());
            }
            
            stmt.close();
            rs.close();
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating invoice: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void printInvoice(String invoiceText) {
        try {
            // Create a simple text area for printing
            javax.swing.JTextArea textArea = new javax.swing.JTextArea(invoiceText);
            textArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            
            // Print the text area
            MessageFormat header = new MessageFormat("Hotel Invoice");
            MessageFormat footer = new MessageFormat("Page {0}");
            
            textArea.print(header, footer);
            
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Error printing invoice: " + e.getMessage());
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
        jLabel2.setText("CheckOut Customers");

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
        jButton1.setText("CheckOut Customer");
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
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(659, 659, 659))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addGap(145, 145, 145))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jButton1)
                                .addGap(214, 214, 214)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 642, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addGap(522, 522, 522)
                                .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(241, 241, 241))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(10, 10, 10)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addGap(9, 9, 9)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
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
        checkoutCustomer();
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
            java.util.logging.Logger.getLogger(CheckOut.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CheckOut.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CheckOut.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CheckOut.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CheckOut().setVisible(true);
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
