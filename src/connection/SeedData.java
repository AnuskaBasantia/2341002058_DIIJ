package connection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class SeedData {

    public static void insertData() {

        try (
                Connection con =
                        ConnectionManager.getConnection()
        ) {

            PreparedStatement ps1 =
                    con.prepareStatement(
                            "INSERT INTO Members(Name, ActiveLoans) VALUES(?,?)");

            ps1.setString(1, "Rahul");
            ps1.setInt(2, 0);
            ps1.executeUpdate();

            ps1.setString(1, "Aman");
            ps1.setInt(2, 0);
            ps1.executeUpdate();

            PreparedStatement ps2 =
                    con.prepareStatement(
                            "INSERT INTO Books(Title, ISBN, Available) VALUES(?,?,?)");

            ps2.setString(1, "Java Programming");
            ps2.setString(2, "1111");
            ps2.setBoolean(3, true);
            ps2.executeUpdate();

            ps2.setString(1, "Database Systems");
            ps2.setString(2, "2222");
            ps2.setBoolean(3, true);
            ps2.executeUpdate();

            System.out.println(
                    "Seed data inserted.");

        } catch (Exception e) {

            System.out.println(e);
        }
    }
}