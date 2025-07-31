import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectionManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/hotel-system"; // Change if needed
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    private static Connection connection;

    // Private constructor to prevent instantiation
    private DBConnectionManager() {}

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                System.out.println("✅ Singleton DB connection established!");
            } catch (ClassNotFoundException e) {
                System.err.println("❌ JDBC Driver not found: " + e.getMessage());
            }
        }
        return connection;
    }
}
