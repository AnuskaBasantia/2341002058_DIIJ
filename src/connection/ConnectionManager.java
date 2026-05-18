package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionManager {

    private static final String URL =
            "jdbc:derby:libraryDB;create=true";

    public static Connection getConnection()
            throws SQLException {

        return DriverManager.getConnection(URL);
    }

    public static void shutdown() {

        try {

            DriverManager.getConnection(
                    "jdbc:derby:libraryDB;shutdown=true");

        } catch (SQLException e) {

            System.out.println(
                    "Database shutdown successfully.");
        }
    }
}