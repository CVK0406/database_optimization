import benchmark.DataLoader;
import benchmark.QueryBenchmark;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Test database connection at startup
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Successfully connected to the database.\n");
        } catch (SQLException e) {
            System.err.println("Failed to connect to the database.");
            e.printStackTrace();
            return;
        }

        Scanner scanner = new Scanner(System.in);
        DataLoader dataLoader = new DataLoader();
        QueryBenchmark queryBenchmark = new QueryBenchmark();

        boolean running = true;
        while (running) {
            System.out.println("=========================================");
            System.out.println("     DATABASE OPTIMIZATION BENCHMARK     ");
            System.out.println("=========================================");
            System.out.println("1. Data Ingestion: Single Insert (50,000 rows)");
            System.out.println("2. Data Ingestion: JDBC Batch Insert (50,000 rows, batch size 10,000)");
            System.out.println("3. Query: Purchase History (Problem 1)");
            System.out.println("4. Query: Revenue Report 2023 (Problem 2)");
            System.out.println("5. Query: Deep Pagination - Offset Method (Page 10,000)");
            System.out.println("6. Query: Deep Pagination - Keyset Method (Page 10,000)");
            System.out.println("0. Exit");
            System.out.println("=========================================");
            System.out.print("Select an option: ");

            String input = scanner.nextLine();
            System.out.println();

            switch (input) {
                case "1":
                    System.out.print("Enter absolute path to orders CSV file: ");
                    String csvPathSingle = scanner.nextLine().replace("\"", "");
                    dataLoader.insertOrdersSingle(csvPathSingle, 50000);
                    break;
                case "2":
                    System.out.print("Enter absolute path to orders CSV file: ");
                    String csvPathBatch = scanner.nextLine().replace("\"", "");
                    dataLoader.insertOrdersBatch(csvPathBatch, 50000, 10000);
                    break;
                case "3":
                    // Sample ID - In reality you'd get an existing customer_unique_id from the dataset
                    String dummyUniqueId = "USER_123"; 
                    System.out.println("Running Purchase History Query for: " + dummyUniqueId);
                    queryBenchmark.benchmarkPurchaseHistoryQuery(dummyUniqueId);
                    break;
                case "4":
                    System.out.println("Running Revenue Report for Year: 2023");
                    queryBenchmark.benchmarkRevenueReportQuery(2023);
                    break;
                case "5":
                    System.out.println("Running Deep Pagination (OFFSET 500,000 LIMIT 50)...");
                    queryBenchmark.benchmarkOffsetPagination(50, 500000);
                    break;
                case "6":
                    // A dummy timestamp simulating the last record on page 9,999
                    String lastTimestamp = "2023-06-15 10:00:00";
                    System.out.println("Running Deep Pagination Keyset (Seek < '" + lastTimestamp + "' LIMIT 50)...");
                    queryBenchmark.benchmarkKeysetPagination(lastTimestamp, 50);
                    break;
                case "0":
                    System.out.println("Exiting...");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
        scanner.close();
    }
}

