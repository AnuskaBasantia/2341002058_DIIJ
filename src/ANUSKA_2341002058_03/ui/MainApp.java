package ANUSKA_2341002058_03.ui;

import java.util.Scanner;

import ANUSKA_2341002058_03.connection.ConnectionManager;
import ANUSKA_2341002058_03.connection.DatabaseSetup;
import ANUSKA_2341002058_03.connection.SeedData;
import ANUSKA_2341002058_03.performance.PerformanceEvaluator;
import ANUSKA_2341002058_03.service.TransactionService;

public class MainApp {

    public static void main(String[] args) {

        DatabaseSetup.initializeDatabase();
        SeedData.insertData();

        Scanner sc = new Scanner(System.in);

        while (true) {

            System.out.println("\n========================================");
            System.out.println("   LIBRARY LOAN MANAGEMENT SYSTEM");
            System.out.println("========================================");
            System.out.println(" 1. Register Member");
            System.out.println(" 2. Add Book");
            System.out.println(" 3. Process Loan");
            System.out.println(" 4. Process Return");
            System.out.println(" 5. View Active Loans by Member");
            System.out.println(" 6. View Overdue Books");
            System.out.println(" 7. Demonstrate Constraint Violation");
            System.out.println(" 8. Run Performance Benchmark");
            System.out.println(" 9. Exit");
            System.out.println("========================================");
            System.out.print("Enter choice: ");

            int choice;

            try {

                choice = Integer.parseInt(sc.nextLine().trim());

            } catch (NumberFormatException e) {

                System.out.println("Invalid input. Enter a number.");
                continue;
            }

            switch (choice) {

                case 1:
                    System.out.print("Enter member name: ");
                    String name = sc.nextLine().trim();
                    TransactionService.registerMember(name);
                    break;

                case 2:
                    System.out.print("Enter book title: ");
                    String title = sc.nextLine().trim();
                    System.out.print("Enter ISBN: ");
                    String isbn = sc.nextLine().trim();
                    TransactionService.addBook(title, isbn);
                    break;

                case 3:
                    System.out.print("Enter Book ID: ");
                    int bookId = parseIntSafe(sc.nextLine());
                    System.out.print("Enter Member ID: ");
                    int memberId = parseIntSafe(sc.nextLine());
                    if (bookId > 0 && memberId > 0)
                        TransactionService.processLoan(bookId, memberId);
                    break;

                case 4:
                    System.out.print("Enter Loan ID: ");
                    int loanId = parseIntSafe(sc.nextLine());
                    if (loanId > 0)
                        TransactionService.processReturn(loanId);
                    break;

                case 5:
                    System.out.print("Enter Member ID: ");
                    int mid = parseIntSafe(sc.nextLine());
                    if (mid > 0)
                        TransactionService.queryLoansByMember(mid);
                    break;

                case 6:
                    TransactionService.queryOverdueBooks();
                    break;

                case 7:
                    TransactionService.demonstrateConstraintViolation();
                    break;

                case 8:
                    PerformanceEvaluator.benchmark();
                    break;

                case 9:
                    sc.close();
                    ConnectionManager.shutdown();
                    System.out.println("Application closed.");
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice. Try again.");
                    break;
            }
        }
    }

    private static int parseIntSafe(String input) {

        try {

            return Integer.parseInt(input.trim());

        } catch (NumberFormatException e) {

            System.out.println("Invalid number entered.");
            return -1;
        }
    }
}
