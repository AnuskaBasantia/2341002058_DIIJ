package connection;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseSetup {

    public static void initializeDatabase() {

        try (
                Connection con =
                        ConnectionManager.getConnection();

                Statement stmt =
                        con.createStatement()
        ) {

            stmt.executeUpdate(
                    "CREATE TABLE Members (" +
                    "MemberID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                    "Name VARCHAR(100)," +
                    "ActiveLoans INT DEFAULT 0)"
            );

            stmt.executeUpdate(
                    "CREATE TABLE Books (" +
                    "BookID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                    "Title VARCHAR(100)," +
                    "ISBN VARCHAR(30)," +
                    "Available BOOLEAN)"
            );

            stmt.executeUpdate(
                    "CREATE TABLE Loans (" +
                    "LoanID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                    "BookID INT REFERENCES Books(BookID)," +
                    "MemberID INT REFERENCES Members(MemberID)," +
                    "LoanDate DATE," +
                    "ReturnDate DATE)"
            );

            System.out.println(
                    "Tables created.");

        } catch (Exception e) {

            System.out.println(
                    "Tables already exist.");
        }
    }
}