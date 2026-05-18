package ANUSKA_2341002058_03.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private static final String DB_URL      = "jdbc:derby:libraryDB;create=true";
    private static final String SHUTDOWN_URL = "jdbc:derby:libraryDB;shutdown=true";

    public static Connection getConnection() throws SQLException {

        return DriverManager.getConnection(DB_URL);
    }

    public static void shutdown() {

        try {

            DriverManager.getConnection(SHUTDOWN_URL).close();

        } catch (SQLException e) {

            // Derby always throws SQLState 08006 on clean shutdown — this is expected
            if ("08006".equals(e.getSQLState()) || "XJ015".equals(e.getSQLState())) {

                System.out.println("Database shutdown successfully.");

            } else {

                System.out.println("Shutdown error: " + e.getMessage());
            }
        }
    }
}
