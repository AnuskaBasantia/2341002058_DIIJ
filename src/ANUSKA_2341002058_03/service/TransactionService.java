package ANUSKA_2341002058_03.service;

import ANUSKA_2341002058_03.connection.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;

public class TransactionService {

    // ----------------------------------------------------------------
    // Process Loan — multi-step transaction with savepoint
    // ----------------------------------------------------------------
    public static void processLoan(int bookId, int memberId) {

        Connection con = null;

        try {

            con = ConnectionManager.getConnection();
            con.setAutoCommit(false);

            // Step 1: Verify book availability
            PreparedStatement checkBook =
                    con.prepareStatement(
                            "SELECT Available FROM Books WHERE BookID=?");

            checkBook.setInt(1, bookId);
            ResultSet rs = checkBook.executeQuery();

            if (!rs.next()) {

                System.out.println("Book ID " + bookId + " does not exist.");
                con.rollback();
                return;
            }

            if (!rs.getBoolean("Available")) {

                System.out.println("Book not available for loan.");
                con.rollback();
                return;
            }

            rs.close();
            checkBook.close();

            // Step 2: Verify member exists
            PreparedStatement checkMember =
                    con.prepareStatement(
                            "SELECT MemberID FROM Members WHERE MemberID=?");

            checkMember.setInt(1, memberId);
            ResultSet rs2 = checkMember.executeQuery();

            if (!rs2.next()) {

                System.out.println("Member ID " + memberId + " does not exist.");
                con.rollback();
                return;
            }

            rs2.close();
            checkMember.close();

            // Step 3: Update book availability
            PreparedStatement updateBook =
                    con.prepareStatement(
                            "UPDATE Books SET Available=false WHERE BookID=?");

            updateBook.setInt(1, bookId);
            updateBook.executeUpdate();
            updateBook.close();

            // Savepoint after book update — rollback to here if loan insert fails
            Savepoint sp = con.setSavepoint("AfterBookUpdate");

            try {

                // Step 4: Insert loan record
                PreparedStatement insertLoan =
                        con.prepareStatement(
                                "INSERT INTO Loans(BookID, MemberID, LoanDate)" +
                                " VALUES(?,?,CURRENT_DATE)");

                insertLoan.setInt(1, bookId);
                insertLoan.setInt(2, memberId);
                insertLoan.executeUpdate();
                insertLoan.close();

                // Step 5: Update member active loan count
                PreparedStatement updateMember =
                        con.prepareStatement(
                                "UPDATE Members SET ActiveLoans=ActiveLoans+1" +
                                " WHERE MemberID=?");

                updateMember.setInt(1, memberId);
                updateMember.executeUpdate();
                updateMember.close();

                con.commit();
                System.out.println("Loan processed successfully.");

            } catch (SQLException inner) {

                // Partial rollback — undo loan insert but keep book status change
                con.rollback(sp);
                con.commit();
                System.out.println("Loan insert failed; rolled back to savepoint.");
                System.out.println("Reason: " + inner.getMessage());
            }

        } catch (SQLException e) {

            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback error: " + ex.getMessage());
            }

            System.out.println("Transaction rolled back: " + e.getMessage());

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Connection close error: " + e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------
    // Process Return
    // ----------------------------------------------------------------
    public static void processReturn(int loanId) {

        Connection con = null;

        try {

            con = ConnectionManager.getConnection();
            con.setAutoCommit(false);

            // Get bookId and memberId from loan
            PreparedStatement getLoan =
                    con.prepareStatement(
                            "SELECT BookID, MemberID, ReturnDate FROM Loans WHERE LoanID=?");

            getLoan.setInt(1, loanId);
            ResultSet rs = getLoan.executeQuery();

            if (!rs.next()) {

                System.out.println("Loan ID " + loanId + " not found.");
                con.rollback();
                return;
            }

            if (rs.getDate("ReturnDate") != null) {

                System.out.println("Loan ID " + loanId + " already returned.");
                con.rollback();
                return;
            }

            int bookId   = rs.getInt("BookID");
            int memberId = rs.getInt("MemberID");
            rs.close();
            getLoan.close();

            // Update return date
            PreparedStatement setReturn =
                    con.prepareStatement(
                            "UPDATE Loans SET ReturnDate=CURRENT_DATE WHERE LoanID=?");

            setReturn.setInt(1, loanId);
            setReturn.executeUpdate();
            setReturn.close();

            // Mark book available again
            PreparedStatement markBook =
                    con.prepareStatement(
                            "UPDATE Books SET Available=true WHERE BookID=?");

            markBook.setInt(1, bookId);
            markBook.executeUpdate();
            markBook.close();

            // Decrement member active loans
            PreparedStatement decMember =
                    con.prepareStatement(
                            "UPDATE Members SET ActiveLoans=ActiveLoans-1" +
                            " WHERE MemberID=? AND ActiveLoans > 0");

            decMember.setInt(1, memberId);
            decMember.executeUpdate();
            decMember.close();

            con.commit();
            System.out.println("Return processed successfully for Loan ID " + loanId + ".");

        } catch (SQLException e) {

            try {
                if (con != null) con.rollback();
            } catch (SQLException ex) {
                System.out.println("Rollback error: " + ex.getMessage());
            }

            System.out.println("Return transaction rolled back: " + e.getMessage());

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Connection close error: " + e.getMessage());
            }
        }
    }

    // ----------------------------------------------------------------
    // Register Member
    // ----------------------------------------------------------------
    public static void registerMember(String name) {

        try (
                Connection con = ConnectionManager.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Members(Name, ActiveLoans) VALUES(?,0)")
        ) {

            ps.setString(1, name);
            ps.executeUpdate();
            System.out.println("Member '" + name + "' registered successfully.");

        } catch (SQLException e) {

            System.out.println("Register member error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Add Book
    // ----------------------------------------------------------------
    public static void addBook(String title, String isbn) {

        try (
                Connection con = ConnectionManager.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                "INSERT INTO Books(Title, ISBN, Available) VALUES(?,?,true)")
        ) {

            ps.setString(1, title);
            ps.setString(2, isbn);
            ps.executeUpdate();
            System.out.println("Book '" + title + "' added successfully.");

        } catch (SQLException e) {

            System.out.println("Add book error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Query Active Loans by Member
    // ----------------------------------------------------------------
    public static void queryLoansByMember(int memberId) {

        try (
                Connection con = ConnectionManager.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                "SELECT L.LoanID, B.Title, B.ISBN, L.LoanDate" +
                                " FROM Loans L" +
                                " JOIN Books B ON L.BookID = B.BookID" +
                                " WHERE L.MemberID=? AND L.ReturnDate IS NULL")
        ) {

            ps.setInt(1, memberId);
            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- Active Loans for Member ID " + memberId + " ---");
            System.out.printf("%-10s %-30s %-15s %-12s%n",
                    "LoanID", "Title", "ISBN", "LoanDate");
            System.out.println("-".repeat(70));

            boolean found = false;

            while (rs.next()) {

                found = true;
                System.out.printf("%-10d %-30s %-15s %-12s%n",
                        rs.getInt("LoanID"),
                        rs.getString("Title"),
                        rs.getString("ISBN"),
                        rs.getDate("LoanDate"));
            }

            if (!found) System.out.println("No active loans found.");

            rs.close();

        } catch (SQLException e) {

            System.out.println("Query error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Query Overdue Books (ReturnDate IS NULL and LoanDate < today)
    // ----------------------------------------------------------------
    public static void queryOverdueBooks() {

        try (
                Connection con = ConnectionManager.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(
                                "SELECT L.LoanID, M.Name, B.Title, L.LoanDate," +
                                " {fn TIMESTAMPDIFF(SQL_TSI_DAY, L.LoanDate, CURRENT_DATE)} AS DaysOverdue" +
                                " FROM Loans L" +
                                " JOIN Books B ON L.BookID = B.BookID" +
                                " JOIN Members M ON L.MemberID = M.MemberID" +
                                " WHERE L.ReturnDate IS NULL" +
                                " AND L.LoanDate < CURRENT_DATE")
        ) {

            ResultSet rs = ps.executeQuery();

            System.out.println("\n--- Overdue Books ---");
            System.out.printf("%-10s %-20s %-30s %-12s %-10s%n",
                    "LoanID", "Member", "Title", "LoanDate", "DaysOverdue");
            System.out.println("-".repeat(85));

            boolean found = false;

            while (rs.next()) {

                found = true;
                System.out.printf("%-10d %-20s %-30s %-12s %-10d%n",
                        rs.getInt("LoanID"),
                        rs.getString("Name"),
                        rs.getString("Title"),
                        rs.getDate("LoanDate"),
                        rs.getInt("DaysOverdue"));
            }

            if (!found) System.out.println("No overdue books found.");

            rs.close();

        } catch (SQLException e) {

            System.out.println("Query error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------
    // Demonstrate Constraint Violation & Isolation
    // ----------------------------------------------------------------
    public static void demonstrateConstraintViolation() {

        System.out.println("\n--- Demonstrating Constraint Violation & Rollback ---");

        Connection con = null;

        try {

            con = ConnectionManager.getConnection();
            con.setAutoCommit(false);

            // Attempt to insert a book with a duplicate ISBN (constraint violation)
            PreparedStatement ps =
                    con.prepareStatement(
                            "INSERT INTO Books(Title, ISBN, Available) VALUES(?,?,true)");

            ps.setString(1, "Duplicate Book");
            ps.setString(2, "ISBN-1001"); // already exists — UNIQUE constraint
            ps.executeUpdate();
            ps.close();

            con.commit();
            System.out.println("Insert succeeded (unexpected).");

        } catch (SQLException e) {

            System.out.println("Constraint violation caught: " + e.getMessage());

            try {
                con.rollback();
                System.out.println("Transaction rolled back. Data integrity preserved.");
            } catch (SQLException ex) {
                System.out.println("Rollback error: " + ex.getMessage());
            }

        } finally {

            try {
                if (con != null) {
                    con.setAutoCommit(true);
                    con.close();
                }
            } catch (SQLException e) {
                System.out.println("Connection close error: " + e.getMessage());
            }
        }

        // Verify data consistency post-rollback
        System.out.println("\nVerifying data consistency post-rollback:");

        try (
                Connection con2 = ConnectionManager.getConnection();

                PreparedStatement verify =
                        con2.prepareStatement(
                                "SELECT COUNT(*) FROM Books WHERE ISBN=?")
        ) {

            verify.setString(1, "ISBN-1001");
            ResultSet rs = verify.executeQuery();
            rs.next();
            System.out.println("Books with ISBN-1001: " + rs.getInt(1) +
                    " (should be 1 — no duplicate inserted)");
            rs.close();

        } catch (SQLException e) {

            System.out.println("Verification error: " + e.getMessage());
        }
    }
}
