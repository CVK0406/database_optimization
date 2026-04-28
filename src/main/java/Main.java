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
            System.out.println("2. FULL LOAD BEFORE: Data Ingestion (Single vs Batch)");
            System.out.println("3. FULL LOAD AFTER: Partitioned Data Ingestion (Single vs Batch)");
            System.out.println("4. Compare: Purchase History (Sequential vs Index Scan)");
            System.out.println("5. Compare: Revenue Report (4 Scenarios)");
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

                    // Clear ecommerce_before once at the start for a clean slate
                    dataLoader.truncateBeforeTables();

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
                    System.out.println("Database is fully loaded. Run Option 3 next to load ecommerce_after partitioned tables.");
                    break;

                case "3":
                    System.out.print("Enter path to Users CSV [default: scripts/users.csv]: ");
                    String usersPathAfter = scanner.nextLine().replace("\"", "").trim();
                    if (usersPathAfter.isEmpty()) usersPathAfter = "scripts/users.csv";

                    System.out.print("Enter path to Products CSV [default: scripts/products.csv]: ");
                    String productsPathAfter = scanner.nextLine().replace("\"", "").trim();
                    if (productsPathAfter.isEmpty()) productsPathAfter = "scripts/products.csv";

                    System.out.print("Enter path to Orders CSV [default: scripts/orders.csv]: ");
                    String ordersPathAfter = scanner.nextLine().replace("\"", "").trim();
                    if (ordersPathAfter.isEmpty()) ordersPathAfter = "scripts/orders.csv";

                    System.out.print("Enter path to Order_Items CSV [default: scripts/order_items.csv]: ");
                    String orderItemsPathAfter = scanner.nextLine().replace("\"", "").trim();
                    if (orderItemsPathAfter.isEmpty()) orderItemsPathAfter = "scripts/order_items.csv";

                    System.out.println("\n--- Starting Full Data Ingestion Benchmark on ecommerce_after (Partitioned Schema) ---");
                    System.out.println("NOTE: Single insert on large tables will take significant time. Please be patient.\n");

                    dataLoader.truncateAfterTables();

                    System.out.println("=== [1/4] USERS TABLE (100,000 rows) ===");
                    System.out.println("[1/2] Single Insert...");
                    long singleUsersAfter = dataLoader.insertUsersSingleAfter(usersPathAfter, Integer.MAX_VALUE);
                    try (java.sql.Connection c = utils.DatabaseConnection.getAfterConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE users CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchUsersAfter = dataLoader.insertUsersBatchAfter(usersPathAfter, Integer.MAX_VALUE, 10000);
                    System.out.println("Single: " + singleUsersAfter + " ms | Batch: " + batchUsersAfter + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleUsersAfter > 0 ? ((double)(singleUsersAfter - batchUsersAfter) / singleUsersAfter) * 100 : 0);

                    System.out.println("\n=== [2/4] PRODUCTS TABLE (32,000 rows) ===");
                    System.out.println("[1/2] Single Insert...");
                    long singleProductsAfter = dataLoader.insertProductsSingleAfter(productsPathAfter, Integer.MAX_VALUE);
                    try (java.sql.Connection c = utils.DatabaseConnection.getAfterConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE products CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchProductsAfter = dataLoader.insertProductsBatchAfter(productsPathAfter, Integer.MAX_VALUE, 10000);
                    System.out.println("Single: " + singleProductsAfter + " ms | Batch: " + batchProductsAfter + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleProductsAfter > 0 ? ((double)(singleProductsAfter - batchProductsAfter) / singleProductsAfter) * 100 : 0);

                    System.out.println("\n=== [3/4] ORDERS_PARTITIONED TABLE (5,000,000 rows) ===");
                    System.out.println("[1/2] Single Insert (will take several minutes)...");
                    long singleOrdersAfter = dataLoader.insertOrdersSingleAfter(ordersPathAfter, Integer.MAX_VALUE);
                    try (java.sql.Connection c = utils.DatabaseConnection.getAfterConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE orders_partitioned CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchOrdersAfter = dataLoader.insertOrdersBatchAfter(ordersPathAfter, Integer.MAX_VALUE, 10000);
                    System.out.println("Single: " + singleOrdersAfter + " ms | Batch: " + batchOrdersAfter + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleOrdersAfter > 0 ? ((double)(singleOrdersAfter - batchOrdersAfter) / singleOrdersAfter) * 100 : 0);

                    System.out.println("\n=== [4/4] ORDER_ITEMS_PARTITIONED TABLE (10,000,000+ rows) ===");
                    System.out.println("[1/2] Single Insert (will take a long time)...");
                    long singleItemsAfter = dataLoader.insertOrderItemsSingleAfter(orderItemsPathAfter, Integer.MAX_VALUE);
                    try (java.sql.Connection c = utils.DatabaseConnection.getAfterConnection();
                         java.sql.Statement s = c.createStatement()) {
                        s.executeUpdate("TRUNCATE TABLE order_items_partitioned CASCADE");
                    } catch (Exception ignored) {}
                    System.out.println("[2/2] Batch Insert...");
                    long batchItemsAfter = dataLoader.insertOrderItemsBatchAfter(orderItemsPathAfter, Integer.MAX_VALUE, 10000);
                    System.out.println("Single: " + singleItemsAfter + " ms | Batch: " + batchItemsAfter + " ms");
                    System.out.printf("Speedup: %.2f%%%n", singleItemsAfter > 0 ? ((double)(singleItemsAfter - batchItemsAfter) / singleItemsAfter) * 100 : 0);

                    System.out.println("\n==========================================");
                    System.out.println("=== COMPARISON RESULT: PARTITIONED DATA INGESTION ===");
                    System.out.println("==========================================");
                    System.out.printf("%-28s | %12s | %12s | %10s%n", "Table", "Single (ms)", "Batch (ms)", "Speedup");
                    System.out.println("----------------------------------------------------------------------");
                    System.out.printf("%-28s | %12d | %12d | %9.2f%%%n", "Users (100k)", singleUsersAfter, batchUsersAfter,
                        singleUsersAfter > 0 ? ((double)(singleUsersAfter - batchUsersAfter) / singleUsersAfter) * 100 : 0);
                    System.out.printf("%-28s | %12d | %12d | %9.2f%%%n", "Products (32k)", singleProductsAfter, batchProductsAfter,
                        singleProductsAfter > 0 ? ((double)(singleProductsAfter - batchProductsAfter) / singleProductsAfter) * 100 : 0);
                    System.out.printf("%-28s | %12d | %12d | %9.2f%%%n", "Orders_Partitioned (5M)", singleOrdersAfter, batchOrdersAfter,
                        singleOrdersAfter > 0 ? ((double)(singleOrdersAfter - batchOrdersAfter) / singleOrdersAfter) * 100 : 0);
                    System.out.printf("%-28s | %12d | %12d | %9.2f%%%n", "Order_Items_Partitioned (10M+)", singleItemsAfter, batchItemsAfter,
                        singleItemsAfter > 0 ? ((double)(singleItemsAfter - batchItemsAfter) / singleItemsAfter) * 100 : 0);
                    System.out.println("==========================================");
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

                    System.out.println("\n=== REVENUE REPORT BENCHMARK: 4 SCENARIOS ===");
                    System.out.println("[1/4] Scenario 1: Baseline (Monolithic, No Indexes)...");
                    long scenario1Time = queryBenchmark.benchmarkRevenueScenario1(year);

                    System.out.println("\n[2/4] Scenario 2: Index Only (Monolithic, With Indexes)...");
                    long scenario2Time = queryBenchmark.benchmarkRevenueScenario2(year);

                    System.out.println("\n[3/4] Scenario 3: Partition Only (Partitioned, No Indexes)...");
                    long scenario3Time = queryBenchmark.benchmarkRevenueScenario3(year);

                    System.out.println("\n[4/4] Scenario 4: Fully Optimized (Partitioned, With Indexes)...");
                    long scenario4Time = queryBenchmark.benchmarkRevenueScenario4(year);

                    System.out.println("\n==========================================");
                    System.out.println("=== COMPARISON RESULT: REVENUE REPORT ===");
                    System.out.println("==========================================");
                    System.out.printf("%-35s | %10s%n", "Scenario", "Time (ms)");
                    System.out.println("--------------------------------------------");
                    System.out.printf("%-35s | %10d%n", "1. Baseline (No optimization)", scenario1Time);
                    System.out.printf("%-35s | %10d%n", "2. Index Only (+Indexing)", scenario2Time);
                    System.out.printf("%-35s | %10d%n", "3. Partition Only (+Partitioning)", scenario3Time);
                    System.out.printf("%-35s | %10d%n", "4. Fully Optimized (+Both)", scenario4Time);
                    System.out.println("==========================================");

                    // Calculate improvements
                    double indexImprovement = scenario1Time > 0 ? ((double)(scenario1Time - scenario2Time) / scenario1Time) * 100 : 0;
                    double partitionImprovement = scenario1Time > 0 ? ((double)(scenario1Time - scenario3Time) / scenario1Time) * 100 : 0;
                    double combinedImprovement = scenario1Time > 0 ? ((double)(scenario1Time - scenario4Time) / scenario1Time) * 100 : 0;

                    System.out.printf("Indexing benefit:     %.2f%%%n", indexImprovement);
                    System.out.printf("Partitioning benefit: %.2f%%%n", partitionImprovement);
                    System.out.printf("Combined benefit:     %.2f%%%n", combinedImprovement);
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
