package ANUSKA_2341002058_03.connection;

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
                    "Name VARCHAR(100) NOT NULL," +
                    "ActiveLoans INT DEFAULT 0)"
            );

            stmt.executeUpdate(
                    "CREATE TABLE Books (" +
                    "BookID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                    "Title VARCHAR(100) NOT NULL," +
                    "ISBN VARCHAR(30) UNIQUE NOT NULL," +
                    "Available BOOLEAN DEFAULT true)"
            );

            stmt.executeUpdate(
                    "CREATE TABLE Loans (" +
                    "LoanID INT PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                    "BookID INT REFERENCES Books(BookID)," +
                    "MemberID INT REFERENCES Members(MemberID)," +
                    "LoanDate DATE," +
                    "ReturnDate DATE)"
            );

            stmt.executeUpdate(
                    "CREATE INDEX idx_books_isbn " +
                    "ON Books(ISBN)"
            );

            stmt.executeUpdate(
                    "CREATE INDEX idx_loans_memberid " +
                    "ON Loans(MemberID)"
            );

            stmt.executeUpdate(
                    "CREATE INDEX idx_loans_returndate " +
                    "ON Loans(ReturnDate)"
            );

            System.out.println("Tables and indexes created.");

        } catch (Exception e) {

            System.out.println("Tables already exist or error: " + e.getMessage());
        }
    }
}
