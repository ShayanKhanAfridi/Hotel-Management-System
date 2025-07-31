🏨 Hotel Management System (Java Swing + MySQL)

This is a Hotel Management System built using Java Swing for GUI and MySQL for the database.  
It allows admins to manage room bookings, customers, services, and check-ins/checkouts.

--------------------------
✅ Design Patterns Used
--------------------------

- Singleton Pattern — for managing a single DB connection
- Factory Pattern — for creating room types
- Observer Pattern — to notify updates in UI after room actions
- Command Pattern — for room service requests
- Null Object Pattern — to avoid null room selections

--------------------------
📸 Features
--------------------------

- Add, update, delete rooms
- Book and check-in customers
- Checkout customers with invoice
- Manage services (laundry, food, cleaning etc.)
- Admin login system
- Auto-room availability update
- GUI notifications and table refresh

--------------------------
🛠️ Technologies Used
--------------------------
Component       : Technology / Library
----------------------------------------
Language        : Java (JDK 17 or later)
GUI             : Java Swing
Database        : MySQL
Date Picker     : JCalendar (https://toedter.com/jcalendar/)
JDBC Driver     : MySQL Connector/J (https://dev.mysql.com/downloads/connector/j/)
IDE             : NetBeans (recommended)


--------------------------
🚀 How to Run the Project
--------------------------

✅ Prerequisites

- Java JDK 17 or later
- NetBeans IDE
- MySQL Server running

Required JARs:
- jcalendar-x.x.jar → https://toedter.com/jcalendar/
- mysql-connector-java-x.x.x.jar → https://dev.mysql.com/downloads/connector/j/
- (Optional) JavaFX JARs if used

📦 Steps

1. Clone the Repository:
   git clone https://github.com/your-username/Hotel-Management-System.git

2. Open in NetBeans:
   - Launch NetBeans
   - File → Open Project → Select the folder

3. Add Required Libraries:
   - Right-click project → Properties → Libraries → Add JAR/Folder
   - Add:
     - jcalendar-x.x.jar
     - mysql-connector-java-x.x.x.jar

4. Create and Set Up the Database:
   - Open MySQL (via XAMPP/phpMyAdmin or MySQL Workbench)
   - Create a database named: hotel-system
   - Import SQL file: database/hotel-system.sql

5. Run the Project:
   - Right-click HotelManagement.java or Login.java
   - Click Run File


--------------------------
📷 Screenshots
--------------------------

<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/fae9ce5f-4e25-476a-875c-0c6126a00438" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/29ff900f-713d-4e1a-bf45-e61fc37daa28" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/170734f6-4452-4e0d-8e1c-b3dcff86da55" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/154189a5-5361-4d9b-980a-b251b4927501" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/25598158-7b61-424b-a443-2f056d5de927" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/b5afc6d2-dc59-4a2d-9c5e-42ea21da629f" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/12a7012a-b37c-4528-b510-dbe53a1c5a35" />
<img width="691" height="497" alt="image" src="https://github.com/user-attachments/assets/1c24351c-c82d-4f11-b10a-1f10811e6b2c" />


--------------------------
🙌 Credits
--------------------------

Developed by: Shayan Khan Afridi

--------------------------
📃 License
--------------------------

This project is for educational purposes and can be modified freely for learning or demonstration purposes.
