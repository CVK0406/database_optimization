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
            System.out.println("2. FULL LOAD & Compare: Data Ingestion (Single vs Batch)");
            System.out.println("3. MIGRATE: Copy unoptimized data to Partitioned Schema");
            System.out.println("4. Compare: Purchase History (Sequential vs Index Scan)");
            System.out.println("5. Compare: Revenue Report (Unoptimized vs Partitioned)");
            System.out.println("6. Compare: Deep Pagination (Offset vs Keyset)");
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
                    System.out.print("Enter path to Users CSV [default: scripts/users.csv]: ");
                    String usersPath = scanner.nextLine().replace("\"", "").trim();
                    if (usersPath.isEmpty()) usersPath = "scripts/users.csv";

                    System.out.print("Enter path to Products CSV [default: scripts/products.csv]: ");
                    String productsPath = scanner.nextLine().replace("\"", "").trim();
                    if (productsPath.isEmpty()) productsPath = "scripts/products.csv";

                    System.out.print("Enter path to Orders CSV [default: scripts/orders.csv]: ");
                    String ordersPath = scanner.nextLine().replace("\"", "").trim();
                    if (ordersPath.isEmpty()) ordersPath = "scripts/orders.csv";

                    System.out.print("Enter path to Order_Items CSV [default: scripts/order_items.csv]: ");
                    String orderItemsPath = scanner.nextLine().replace("\"", "").trim();
                    if (orderItemsPath.isEmpty()) orderItemsPath = "scripts/order_items.csv";

                    System.out.println("\n--- Starting Full Data Ingestion Benchmark (Single vs Batch) ---");
                    System.out.println("NOTE: Single insert on large tables will take significant time. Please be patient.\n");

                    // Clear everything once at the start for a clean slate
                    dataLoader.truncateAllTables();

                    // =========================================================
                    // TABLE 1: USERS (100,000 rows) - no FK dependencies
                    // =========================================================
                    System.out.println("=== [1/4] USERS TABLE (100,000 rows) ===");
                    System.out.println("[1/2] Single Insert...");
                    long singleUsers = dataLoader.insertUsersSingle(usersPath, Integer.MAX_VALUE);
                    // Only truncate users between the two tests
                    try (java.sql.Connection c = utils.DatabaseConnection.getBeforeConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE users CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchUsers = dataLoader.insertUsersBatch(usersPath, Integer.MAX_VALUE, 10000);
                    // Users remain loaded for Orders FK dependency
                    System.out.println("Single: " + singleUsers + " ms | Batch: " + batchUsers + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleUsers > 0 ? ((double)(singleUsers - batchUsers) / singleUsers) * 100 : 0);

                    // =========================================================
                    // TABLE 2: PRODUCTS (32,000 rows) - no FK dependencies
                    // =========================================================
                    System.out.println("\n=== [2/4] PRODUCTS TABLE (32,000 rows) ===");
                    System.out.println("[1/2] Single Insert...");
                    long singleProducts = dataLoader.insertProductsSingle(productsPath, Integer.MAX_VALUE);
                    // Only truncate products between the two tests
                    try (java.sql.Connection c = utils.DatabaseConnection.getBeforeConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE products CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchProducts = dataLoader.insertProductsBatch(productsPath, Integer.MAX_VALUE, 10000);
                    // Products remain loaded for Order Items FK dependency
                    System.out.println("Single: " + singleProducts + " ms | Batch: " + batchProducts + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleProducts > 0 ? ((double)(singleProducts - batchProducts) / singleProducts) * 100 : 0);

                    // =========================================================
                    // TABLE 3: ORDERS (5,000,000 rows)
                    // Users already loaded above - no pre-loading needed!
                    // =========================================================
                    System.out.println("\n=== [3/4] ORDERS TABLE (5,000,000 rows) ===");
                    System.out.println("[1/2] Single Insert (will take several minutes)...");
                    long singleOrders = dataLoader.insertOrdersSingle(ordersPath, Integer.MAX_VALUE);
                    // Only truncate orders between the two tests
                    try (java.sql.Connection c = utils.DatabaseConnection.getBeforeConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE orders CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchOrders = dataLoader.insertOrdersBatch(ordersPath, Integer.MAX_VALUE, 10000);
                    // Orders remain loaded for Order Items FK dependency
                    System.out.println("Single: " + singleOrders + " ms | Batch: " + batchOrders + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleOrders > 0 ? ((double)(singleOrders - batchOrders) / singleOrders) * 100 : 0);

                    // =========================================================
                    // TABLE 4: ORDER ITEMS (10M+ rows)
                    // Orders + Products already loaded above - no pre-loading needed!
                    // =========================================================
                    System.out.println("\n=== [4/4] ORDER ITEMS TABLE (10,000,000+ rows) ===");
                    System.out.println("[1/2] Single Insert (will take a long time)...");
                    long singleItems = dataLoader.insertOrderItemsSingle(orderItemsPath, Integer.MAX_VALUE);
                    // Only truncate order_items between the two tests
                    try (java.sql.Connection c = utils.DatabaseConnection.getBeforeConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE order_items CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchItems = dataLoader.insertOrderItemsBatch(orderItemsPath, Integer.MAX_VALUE, 10000);
                    System.out.println("Single: " + singleItems + " ms | Batch: " + batchItems + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleItems > 0 ? ((double)(singleItems - batchItems) / singleItems) * 100 : 0);

                    // =========================================================
                    // FINAL SUMMARY
                    // =========================================================
                    System.out.println("\n==========================================");
                    System.out.println("=== COMPARISON RESULT: DATA INGESTION ===");
                    System.out.println("==========================================");
                    System.out.printf("%-22s | %12s | %12s | %10s%n", "Table", "Single (ms)", "Batch (ms)", "Speedup");
                    System.out.println("--------------------------------------------------------------------");
                    System.out.printf("%-22s | %12d | %12d | %9.2f%%%n", "Users (100k)", singleUsers, batchUsers,
                        singleUsers > 0 ? ((double)(singleUsers - batchUsers) / singleUsers) * 100 : 0);
                    System.out.printf("%-22s | %12d | %12d | %9.2f%%%n", "Products (32k)", singleProducts, batchProducts,
                        singleProducts > 0 ? ((double)(singleProducts - batchProducts) / singleProducts) * 100 : 0);
                    System.out.printf("%-22s | %12d | %12d | %9.2f%%%n", "Orders (5M)", singleOrders, batchOrders,
                        singleOrders > 0 ? ((double)(singleOrders - batchOrders) / singleOrders) * 100 : 0);
                    System.out.printf("%-22s | %12d | %12d | %9.2f%%%n", "Order Items (10M+)", singleItems, batchItems,
                        singleItems > 0 ? ((double)(singleItems - batchItems) / singleItems) * 100 : 0);
                    System.out.println("==========================================");
                    System.out.println("Database is fully loaded. Run Option 3 (MIGRATE) next.");
                    break;

                case "3":
                    System.out.println("\n--- Starting Migration to Partitioned Schema ---");
                    dbSetup.migrateDataToPartitionedTables();
                    break;

                case "4":
                    System.out.print("Enter a Customer Unique ID (e.g., USER_12345): ");
                    String customerId = scanner.nextLine();

                    System.out.println("\n[1/2] Running Sequential Scan (Indexes Disabled)...");
                    long seqTime = queryBenchmark.benchmarkPurchaseHistoryQuery(customerId, false);
                    System.out.println("\n[2/2] Running Index Scan (Indexes Enabled)...");
                    long idxTime = queryBenchmark.benchmarkPurchaseHistoryQuery(customerId, true);

                    System.out.println("\n=== COMPARISON RESULT: PURCHASE HISTORY ===");
                    System.out.println("Sequential Scan: " + seqTime + " ms");
                    System.out.println("Index Scan:      " + idxTime + " ms");
                    double queryImprovement = seqTime > 0 ? ((double)(seqTime - idxTime) / seqTime) * 100 : 0;
                    System.out.printf("Improvement:     %.2f%%%n", queryImprovement);
                    break;

                case "5":
                    System.out.print("Enter Year for Revenue Report (e.g., 2017): ");
                    int year = 2017;
                    try { year = Integer.parseInt(scanner.nextLine().trim()); } catch (Exception e) {}

                    System.out.println("\n[1/2] Running Unoptimized Schema (Requires JOIN)...");
                    long unoptTime = queryBenchmark.benchmarkRevenueReportUnoptimized(year);
                    System.out.println("\n[2/2] Running Partitioned Schema (Direct Query)...");
                    long partTime = queryBenchmark.benchmarkRevenueReportPartitioned(year);

                    System.out.println("\n=== COMPARISON RESULT: REVENUE REPORT ===");
                    System.out.println("Unoptimized (JOIN):  " + unoptTime + " ms");
                    System.out.println("Partitioned (Range): " + partTime + " ms");
                    double partImprovement = unoptTime > 0 ? ((double)(unoptTime - partTime) / unoptTime) * 100 : 0;
                    System.out.printf("Improvement:         %.2f%%%n", partImprovement);
                    break;

                case "6":
                    System.out.println("\n[1/2] Running Deep Pagination via OFFSET (Page 10,000)...");
                    long offsetTime = queryBenchmark.benchmarkOffsetPagination(500000, 50);

                    System.out.println("\n[2/2] Running Deep Pagination via KEYSET (Seek Method)...");
                    long keysetTime = queryBenchmark.benchmarkKeysetPagination("2017-06-01 12:00:00", 50);

                    System.out.println("\n=== COMPARISON RESULT: DEEP PAGINATION ===");
                    System.out.println("OFFSET Method: " + offsetTime + " ms");
                    System.out.println("KEYSET Method: " + keysetTime + " ms");
                    double pagImprovement = offsetTime > 0 ? ((double)(offsetTime - keysetTime) / offsetTime) * 100 : 0;
                    System.out.printf("Improvement:   %.2f%%%n", pagImprovement);
                    break;

                case "0":
                    System.out.println("Exiting...");
                    running = false;
                    break;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
            if (running) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        scanner.close();
    }
}
