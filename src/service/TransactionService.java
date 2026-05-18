package service;

import connection.ConnectionManager;

import java.sql.*;

public class TransactionService {

    public static void processLoan(
            int bookId,
            int memberId) {

        Connection con = null;

        try {

            con = ConnectionManager.getConnection();

            con.setAutoCommit(false);

            PreparedStatement checkBook =
                    con.prepareStatement(
                            "SELECT Available FROM Books WHERE BookID=?");

            checkBook.setInt(1, bookId);

            ResultSet rs =
                    checkBook.executeQuery();

            if (rs.next()) {

                boolean available =
                        rs.getBoolean("Available");

                if (!available) {

                    System.out.println(
                            "Book not available.");

                    return;
                }
            }

            PreparedStatement updateBook =
                    con.prepareStatement(
                            "UPDATE Books SET Available=false WHERE BookID=?");

            updateBook.setInt(1, bookId);

            updateBook.executeUpdate();

            Savepoint sp =
                    con.setSavepoint();

            PreparedStatement insertLoan =
                    con.prepareStatement(
                            "INSERT INTO Loans(BookID, MemberID, LoanDate) VALUES(?,?,CURRENT_DATE)");

            insertLoan.setInt(1, bookId);
            insertLoan.setInt(2, memberId);

            insertLoan.executeUpdate();

            PreparedStatement updateMember =
                    con.prepareStatement(
                            "UPDATE Members SET ActiveLoans=ActiveLoans+1 WHERE MemberID=?");

            updateMember.setInt(1, memberId);

            updateMember.executeUpdate();

            con.commit();

            System.out.println(
                    "Loan processed successfully.");

        } catch (Exception e) {

            try {

                con.rollback();

            } catch (Exception ex) {
            }

            System.out.println(
                    "Transaction rolled back.");

            System.out.println(e);

        } finally {

            try {

                con.setAutoCommit(true);

                con.close();

            } catch (Exception e) {
            }
        }
    }
}