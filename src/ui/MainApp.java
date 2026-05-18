package ui;

import java.util.Scanner;

import connection.ConnectionManager;
import connection.DatabaseSetup;
import connection.SeedData;
import performance.PerformanceEvaluator;
import service.TransactionService;

public class MainApp {

    public static void main(String[] args) {

        DatabaseSetup.initializeDatabase();

        SeedData.insertData();

        Scanner sc = new Scanner(System.in);

        while(true) {

            System.out.println("\n===== MENU =====");

            System.out.println("1. Process Loan");
            System.out.println("2. Run Benchmark");
            System.out.println("3. Exit");

            System.out.print("Enter choice: ");

            int choice = sc.nextInt();

            switch(choice) {

                case 1:

                    System.out.print(
                            "Enter Book ID: ");

                    int bookId =
                            sc.nextInt();

                    System.out.print(
                            "Enter Member ID: ");

                    int memberId =
                            sc.nextInt();

                    TransactionService.processLoan(
                            bookId,
                            memberId);

                    break;

                case 2:

                    PerformanceEvaluator.benchmark();

                    break;

                case 3:

                    ConnectionManager.shutdown();

                    System.out.println(
                            "Application Closed.");

                    System.exit(0);

                default:

                    System.out.println(
                            "Invalid choice.");
            }
        }
    }
}
