import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import java.awt.event.ActionEvent;

public class CustomerInfoForm extends javax.swing.JFrame {

    private int roomNumber;
    private String roomType;
    private double roomAmount;
    
    public CustomerInfoForm(int roomNumber, String roomType, double roomAmount) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.roomAmount = roomAmount;
        
        initComponents();
        setLocationRelativeTo(null);
        
        // Display room information on the form
        displayRoomInfo();
    }
    
    public CustomerInfoForm() {
        initComponents();
    }
    
    private void displayRoomInfo() {
        // Display room information on the form
        if (roomNumber > 0) {
            jLabel4.setText("Room: " + roomNumber + " | Type: " + roomType + " | Amount: $" + roomAmount);
        } else {
            jLabel4.setText("No room selected");
        }
    }
    
    // Database connection method
    private Connection getConnection() throws SQLException {
        return DBConnectionManager.getConnection();
}

    
    // Check if room exists and is available - Updated to use correct column name
   private boolean verifyRoomAvailability(Connection conn) {
    if (roomNumber <= 0) {
        JOptionPane.showMessageDialog(this, "Please select a room first from Book Rooms", 
            "Room Selection Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    String query = "SELECT availability FROM rooms WHERE room_no = ?"; // Use 'availability' column, not 'status'
    
    try (PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setInt(1, roomNumber);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            JOptionPane.showMessageDialog(this, "Room " + roomNumber + " does not exist in the system", 
                "Room Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        String availability = rs.getString("availability");
        if (!"Available".equalsIgnoreCase(availability)) {
            JOptionPane.showMessageDialog(this, "Room " + roomNumber + " is not available for booking", 
                "Room Not Available", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error verifying room availability:\n" + e.getMessage(),
            "Database Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        return false;
    } catch (Exception ex) {
        JOptionPane.showMessageDialog(this, "Unexpected error:\n" + ex.getMessage(),
            "Unexpected Error", JOptionPane.ERROR_MESSAGE);
        ex.printStackTrace();
        return false;
    }
}

    
     // Validate input fields
    private boolean validateInput() {
        if (jTextField1.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter customer name", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField1.requestFocus();
            return false;
        }
        
        if (jTextField4.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter phone number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField4.requestFocus();
            return false;
        }
        
        if (jTextField5.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email address", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField5.requestFocus();
            return false;
        }
        
        if (jTextField2.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter CNIC", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField2.requestFocus();
            return false;
        }
        
        if (jTextField3.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter address", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField3.requestFocus();
            return false;
        }
        
        // Validate email format
        String email = jTextField5.getText().trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField5.requestFocus();
            return false;
        }
        
        // Validate phone number (basic validation)
        String phone = jTextField4.getText().trim();
        if (!phone.matches("^[0-9+\\-\\s()]+$") || phone.length() < 10) {
            JOptionPane.showMessageDialog(this, "Please enter a valid phone number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField4.requestFocus();
            return false;
        }
        
        // Validate CNIC format (assuming Pakistani CNIC format: 12345-1234567-1)
        String cnic = jTextField2.getText().trim();
        if (!cnic.matches("^[0-9]{5}-[0-9]{7}-[0-9]$")) {
            JOptionPane.showMessageDialog(this, "Please enter CNIC in format: 12345-1234567-1", "Validation Error", JOptionPane.ERROR_MESSAGE);
            jTextField2.requestFocus();
            return false;
        }
        
        return true;
    }
    
    // Check if customer already exists
    private int getCustomerId(String cnic, Connection conn) throws SQLException {
        String query = "SELECT customer_id FROM customers WHERE cnic = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, cnic);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("customer_id");
            }
        }
        return -1; // Customer not found
    }
    
    // Insert new customer
    private int insertCustomer(Connection conn) throws SQLException {
        String query = "INSERT INTO customers (name, phone, email, cnic, address, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, jTextField1.getText().trim());
            pstmt.setString(2, jTextField4.getText().trim());
            pstmt.setString(3, jTextField5.getText().trim());
            pstmt.setString(4, jTextField2.getText().trim());
            pstmt.setString(5, jTextField3.getText().trim());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to insert customer");
    }
    
    // Fixed createBooking method with proper error handling and column verification
    private int createBooking(int customerId, Connection conn) throws SQLException {
        // First verify the room exists and is available
        if (!verifyRoomAvailability(conn)) {
            throw new SQLException("Room verification failed");
        }
        
        // Verify that the room exists in the rooms table with the correct room_no
        String checkRoomQuery = "SELECT room_no, room_type, price FROM rooms WHERE room_no = ? AND status = 'AVAILABLE'";
        String actualRoomType = null;
        double actualPrice = 0.0;
        
        try (PreparedStatement checkStmt = conn.prepareStatement(checkRoomQuery)) {
            
            checkStmt.setInt(1, roomNumber);
            ResultSet rs = checkStmt.executeQuery();
            
            if (!rs.next()) {
                throw new SQLException("Room " + roomNumber + " not found or not available");
            }
            
            actualRoomType = rs.getString("room_type");
            actualPrice = rs.getDouble("price");
            
            // Update the local variables with actual database values
            this.roomType = actualRoomType;
            this.roomAmount = actualPrice;
        }
        
        // Now create the booking with verified data
        String insertQuery = "INSERT INTO bookings (customer_id, room_no, room_type, total_amount, booking_status, booking_date) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, customerId);
            pstmt.setInt(2, roomNumber);
            pstmt.setString(3, actualRoomType);
            pstmt.setDouble(4, actualPrice);
            pstmt.setString(5, "CONFIRMED");
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new SQLException("Failed to create booking - no rows affected");
            }
    
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create booking");
    }
    
    private void updateCustomer(int customerId, Connection conn) throws SQLException {
        String query = "UPDATE customers SET name = ?, phone = ?, email = ?, address = ?, updated_at = ? WHERE customer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setString(1, jTextField1.getText().trim());
            pstmt.setString(2, jTextField4.getText().trim());
            pstmt.setString(3, jTextField5.getText().trim());
            pstmt.setString(4, jTextField3.getText().trim());
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(6, customerId);
            
            pstmt.executeUpdate();
        }
    }
    
    // Fixed updateRoomStatus method with correct column name
    private void updateRoomStatus(Connection conn) throws SQLException {
        String query = "UPDATE rooms SET status = 'BOOKED', availability = 'Booked', last_updated = ? WHERE room_no = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, roomNumber); // Make sure this is correct
            pstmt.executeUpdate();
        }
    }

    
    // Generate booking confirmation number
    private String generateBookingConfirmation(int bookingId) {
        return "HMS" + String.format("%06d", bookingId);
    }
    
    // Enhanced main booking process with better error handling
    private void processBooking() {
        if (!validateInput()) {
            return;
        }
        
        // Check if room is selected
        if (roomNumber <= 0) {
            JOptionPane.showMessageDialog(this, 
                "Please select a room first from Book Rooms page", 
                "Room Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try (Connection conn = getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            String cnic = jTextField2.getText().trim();
            int customerId = getCustomerId(cnic, conn);
            
            if (customerId == -1) {
                // New customer
                customerId = insertCustomer(conn);
                System.out.println("New customer registered with ID: " + customerId);
            } else {
                // Existing customer - ask for update
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Customer with this CNIC already exists. Do you want to update the information?", 
                    "Customer Exists", JOptionPane.YES_NO_OPTION);
                
                if (choice == JOptionPane.YES_OPTION) {
                    updateCustomer(customerId, conn);
                    System.out.println("Customer information updated for ID: " + customerId);
                }
            }
            
            // Create booking
            int bookingId = createBooking(customerId, conn);
            System.out.println("Booking created with ID: " + bookingId);
            
            // Update room status
            updateRoomStatus(conn);
            System.out.println("Room status updated to BOOKED");
            
            // Commit transaction
            conn.commit();
            
            // Generate confirmation
            String confirmationNumber = generateBookingConfirmation(bookingId);
            
            // Show success message
            String message = String.format(
                "Booking Confirmed!\n\n" +
                "Confirmation Number: %s\n" +
                "Customer: %s\n" +
                "Room: %d (%s)\n" +
                "Amount: $%.2f\n\n" +
                "Thank you for choosing our hotel!",
                confirmationNumber,
                jTextField1.getText().trim(),
                roomNumber,
                roomType,
                roomAmount
            );
            
            JOptionPane.showMessageDialog(this, message, "Booking Confirmed", JOptionPane.INFORMATION_MESSAGE);
            
            // Clear form
            clearForm();
            
            // Redirect back to Book Rooms page
            BookRooms bookRoomsFrame = new BookRooms();
            bookRoomsFrame.setVisible(true);
            bookRoomsFrame.pack();
            bookRoomsFrame.setLocationRelativeTo(null);
            this.dispose();
            
        } catch (SQLException e) {
            String errorMessage = "Database error: " + e.getMessage();
            if (e.getMessage().contains("foreign key constraint")) {
                errorMessage = "Room booking failed: The selected room may not exist in the system. Please contact administrator.";
            }
            
            JOptionPane.showMessageDialog(this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "An unexpected error occurred: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    // Clear form fields
    private void clearForm() {
        jTextField1.setText("");
        jTextField2.setText("");
        jTextField3.setText("");
        jTextField4.setText("");
        jTextField5.setText("");
        
        // Reset room information
        this.roomNumber = 0;
        this.roomType = "";
        this.roomAmount = 0.0;
        displayRoomInfo();
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
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jTextField3 = new javax.swing.JTextField();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();

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
        jLabel2.setText("Customer Details Form");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(0, 102, 102));

        jPanel2.setBackground(new java.awt.Color(0, 153, 153));

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Name");

        jTextField1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Phone Number");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Email Address");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("CNIC");

        jTextField2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Address");

        jTextField3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jTextField4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        jTextField5.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(37, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jTextField3)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jLabel9)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel8)
                            .addComponent(jLabel7)
                            .addComponent(jLabel6)
                            .addComponent(jLabel5)
                            .addComponent(jTextField1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE)
                            .addComponent(jTextField2)))
                    .addComponent(jTextField4)
                    .addComponent(jTextField5))
                .addGap(42, 42, 42))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel6)
                .addGap(10, 10, 10)
                .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jButton1.setBackground(new java.awt.Color(0, 0, 153));
        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jButton1.setForeground(new java.awt.Color(255, 255, 255));
        jButton1.setText("Booking Confirmation");
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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel4)
                        .addGap(695, 695, 695))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(146, 146, 146)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(88, 88, 88)
                                .addComponent(jLabel2)
                                .addGap(34, 34, 34)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(179, 179, 179)
                                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(235, 235, 235)
                                .addComponent(jButton1)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButton1)
                .addGap(30, 30, 30))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 814, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 539, Short.MAX_VALUE)
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
        processBooking();
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
            java.util.logging.Logger.getLogger(CustomerInfoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CustomerInfoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CustomerInfoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CustomerInfoForm.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CustomerInfoForm().setVisible(true);
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JButton manageBtn;
    private javax.swing.JButton servicesBtn;
    // End of variables declaration//GEN-END:variables
}
