import benchmark.DataLoader;
import benchmark.QueryBenchmark;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Test database connections at startup
        try (Connection conn1 = DatabaseConnection.getBeforeConnection();
             Connection conn2 = DatabaseConnection.getAfterConnection()) {
            System.out.println("Successfully connected to BOTH databases.\n");
        } catch (SQLException e) {
            System.err.println("WARNING: Failed to connect to databases. They might not exist yet.");
            System.err.println("Please run Option 1 (Setup Databases) first.\n");
        }

        Scanner scanner = new Scanner(System.in);
        benchmark.DatabaseSetup dbSetup = new benchmark.DatabaseSetup();
        DataLoader dataLoader = new DataLoader();
        QueryBenchmark queryBenchmark = new QueryBenchmark();

        boolean running = true;
        while (running) {
            System.out.println("=========================================");
            System.out.println("     DATABASE OPTIMIZATION BENCHMARK     ");
            System.out.println("=========================================");
            System.out.println("1. SETUP: Create Databases and Tables");
            System.out.println("2. FULL LOAD: Populate database with CSV files");
            System.out.println("3. MIGRATE: Copy unoptimized data to Partitioned Schema");
            System.out.println("4. Compare: Data Ingestion (Single vs Batch)");
            System.out.println("5. Compare: Purchase History (Sequential vs Index Scan)");
            System.out.println("6. Compare: Revenue Report (Unoptimized vs Partitioned)");
            System.out.println("7. Compare: Deep Pagination (Offset vs Keyset)");
            System.out.println("0. Exit");
            System.out.println("=========================================");
            System.out.print("Select an option: ");

            String input = scanner.nextLine();
            System.out.println();

            switch (input) {
                case "1":
                    System.out.println("\n--- Starting Automatic Database Setup ---");
                    dbSetup.initializeDatabases();
                    break;
                case "2":
                    System.out.print("Enter absolute path to Users CSV file: ");
                    String usersPath = scanner.nextLine().replace("\"", "");
                    System.out.print("Enter absolute path to Products CSV file: ");
                    String productsPath = scanner.nextLine().replace("\"", "");
                    System.out.print("Enter absolute path to Orders CSV file: ");
                    String ordersPath = scanner.nextLine().replace("\"", "");
                    System.out.print("Enter absolute path to Order_Items CSV file: ");
                    String orderItemsPath = scanner.nextLine().replace("\"", "");
                    
                    System.out.println("\n--- Starting Full Database Load ---");
                    dataLoader.insertUsersBatch(usersPath, Integer.MAX_VALUE, 10000);
                    dataLoader.insertProductsBatch(productsPath, Integer.MAX_VALUE, 10000);
                    dataLoader.insertOrdersBatch(ordersPath, Integer.MAX_VALUE, 10000);
                    dataLoader.insertOrderItemsBatch(orderItemsPath, Integer.MAX_VALUE, 10000);
                    System.out.println("--- Full Database Load Complete ---");
                    break;
                case "3":
                    System.out.println("\n--- Starting Migration to Partitioned Schema ---");
                    dbSetup.migrateDataToPartitionedTables();
                    break;
                case "4":
                    System.out.print("Enter absolute path to Orders CSV file: ");
                    String csvPath = scanner.nextLine().replace("\"", "");
                    System.out.println("\n[1/2] Running Single Insert (50,000 rows)...");
                    long singleTime = dataLoader.insertOrdersSingle(csvPath, 50000);
                    System.out.println("\n[2/2] Running Batch Insert (50,000 rows)...");
                    long batchTime = dataLoader.insertOrdersBatch(csvPath, 50000, 10000);
                    
                    System.out.println("\n=== COMPARISON RESULT: INGESTION ===");
                    System.out.println("Single Insert: " + singleTime + " ms");
                    System.out.println("Batch Insert:  " + batchTime + " ms");
                    double impIngest = singleTime == 0 ? 0 : ((double)(singleTime - batchTime) / singleTime) * 100;
                    System.out.printf("Improvement:   %.2f%%\n", impIngest);
                    break;
                case "5":
                    System.out.print("Enter Customer Unique ID (e.g., USER_123): ");
                    String uniqueId = scanner.nextLine();
                    System.out.println("\n[1/2] Running Sequential Scan (Indexes Disabled)...");
                    long seqTime = queryBenchmark.benchmarkPurchaseHistoryQuery(uniqueId, false);
                    System.out.println("\n[2/2] Running Index Scan (Indexes Enabled)...");
                    long idxTime = queryBenchmark.benchmarkPurchaseHistoryQuery(uniqueId, true);
                    
                    System.out.println("\n=== COMPARISON RESULT: PURCHASE HISTORY ===");
                    System.out.println("Sequential Scan: " + seqTime + " ms");
                    System.out.println("Index Scan:      " + idxTime + " ms");
                    double impPurch = seqTime == 0 ? 0 : ((double)(seqTime - idxTime) / seqTime) * 100;
                    System.out.printf("Improvement:     %.2f%%\n", impPurch);
                    break;
                case "6":
                    System.out.print("Enter Year for Revenue Report (e.g., 2018): ");
                    try {
                        int year = Integer.parseInt(scanner.nextLine());
                        System.out.println("\n[1/2] Running Unoptimized Schema (Requires JOIN)...");
                        long unoptTime = queryBenchmark.benchmarkRevenueReportUnoptimized(year);
                        System.out.println("\n[2/2] Running Partitioned Schema (Direct Query)...");
                        long optTime = queryBenchmark.benchmarkRevenueReportPartitioned(year);
                        
                        System.out.println("\n=== COMPARISON RESULT: REVENUE REPORT ===");
                        System.out.println("Unoptimized (JOIN):  " + unoptTime + " ms");
                        System.out.println("Partitioned (Range): " + optTime + " ms");
                        double impRev = unoptTime == 0 ? 0 : ((double)(unoptTime - optTime) / unoptTime) * 100;
                        System.out.printf("Improvement:         %.2f%%\n", impRev);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid year format.");
                    }
                    break;
                case "7":
                    System.out.print("Enter OFFSET value (e.g., 500000): ");
                    try {
                        int offset = Integer.parseInt(scanner.nextLine());
                        System.out.print("Enter Keyset Timestamp (e.g., 2018-06-15 10:00:00): ");
                        String timestamp = scanner.nextLine();
                        
                        System.out.println("\n[1/2] Running Deep Pagination (OFFSET " + offset + ")...");
                        long offsetTime = queryBenchmark.benchmarkOffsetPagination(50, offset);
                        System.out.println("\n[2/2] Running Deep Pagination Keyset (Seek < '" + timestamp + "')...");
                        long keysetTime = queryBenchmark.benchmarkKeysetPagination(timestamp, 50);
                        
                        System.out.println("\n=== COMPARISON RESULT: DEEP PAGINATION ===");
                        System.out.println("Offset Method: " + offsetTime + " ms");
                        System.out.println("Keyset Method: " + keysetTime + " ms");
                        double impPag = offsetTime == 0 ? 0 : ((double)(offsetTime - keysetTime) / offsetTime) * 100;
                        System.out.printf("Improvement:   %.2f%%\n", impPag);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid offset format.");
                    }
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

