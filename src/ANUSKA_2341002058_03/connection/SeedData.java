package ANUSKA_2341002058_03.connection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SeedData {

    public static void insertData() {

        try (
                Connection con =
                        ConnectionManager.getConnection();

                Statement check =
                        con.createStatement()
        ) {

            ResultSet rs =
                    check.executeQuery(
                            "SELECT COUNT(*) FROM Members");

            rs.next();

            if (rs.getInt(1) > 0) {

                System.out.println("Seed data already present.");
                return;
            }

            PreparedStatement ps1 =
                    con.prepareStatement(
                            "INSERT INTO Members(Name, ActiveLoans) VALUES(?,?)");

            String[] members = {"Rahul", "Aman", "Priya", "Vikram"};

            for (String name : members) {

                ps1.setString(1, name);
                ps1.setInt(2, 0);
                ps1.executeUpdate();
            }

            ps1.close();

            PreparedStatement ps2 =
                    con.prepareStatement(
                            "INSERT INTO Books(Title, ISBN, Available) VALUES(?,?,?)");

            String[][] books = {
                {"Java Programming",    "ISBN-1001", "true"},
                {"Database Systems",    "ISBN-1002", "true"},
                {"Data Structures",     "ISBN-1003", "true"},
                {"Operating Systems",   "ISBN-1004", "true"},
                {"Computer Networks",   "ISBN-1005", "true"}
            };

            for (String[] b : books) {

                ps2.setString(1, b[0]);
                ps2.setString(2, b[1]);
                ps2.setBoolean(3, Boolean.parseBoolean(b[2]));
                ps2.executeUpdate();
            }

            ps2.close();

            System.out.println("Seed data inserted.");

        } catch (SQLException e) {

            System.out.println("Seed data error: " + e.getMessage());
        }
    }
}
