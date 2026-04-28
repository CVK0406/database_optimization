package benchmark;

import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryBenchmark {

    /**
     * Helper: Runs EXPLAIN ANALYZE on a query and prints the execution plan.
     */
    private void printExplainPlan(Connection conn, String label, String sql, String... params) {
        String explainSql = "EXPLAIN ANALYZE " + sql;
        try (PreparedStatement pstmt = conn.prepareStatement(explainSql)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setString(i + 1, params[i]);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\n--- EXPLAIN ANALYZE: " + label + " ---");
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1));
                }
                System.out.println("--- END EXPLAIN ---\n");
            }
        } catch (SQLException e) {
            System.err.println("Could not get EXPLAIN plan for " + label + ": " + e.getMessage());
        }
    }

    /**
     * Problem 1: Purchase History Query (Power of Indexing)
     * Executes a 4-table join to retrieve the purchase history for a specific user.
     * Compares:
     * - ecommerce_before (baseline, no indexes): Sequential Scan
     * - ecommerce_after (optimized, with indexes): Index Scan
     */
    public long benchmarkPurchaseHistoryQuery(String customerUniqueId, boolean useIndex) {
        String sql = "SELECT o.order_id, o.order_purchase_timestamp, p.category_name, oi.price " +
                     "FROM orders o " +
                     "JOIN users u ON o.customer_id = u.customer_id " +
                     "JOIN order_items oi ON o.order_id = oi.order_id " +
                     "JOIN products p ON oi.product_id = p.product_id " +
                     "WHERE u.customer_unique_id = ? " +
                     "ORDER BY o.order_purchase_timestamp DESC";

        long startTime = System.currentTimeMillis();
        int rows = 0;
        String dbName = useIndex ? "ecommerce_after (With Indexes)" : "ecommerce_before (No Indexes)";

        try (Connection conn = useIndex ? DatabaseConnection.getAfterConnection() : DatabaseConnection.getBeforeConnection()) {

            // Print EXPLAIN ANALYZE plan
            printExplainPlan(conn, useIndex ? "Index Scan (ecommerce_after)" : "Sequential Scan (ecommerce_before)", sql, customerUniqueId);

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, customerUniqueId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rows++;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Purchase History Query on " + dbName + ": " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Purchase History (" + dbName + "): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query - Scenario 1 (Baseline)
     * Monolithic tables, no indexes (indexes disabled)
     * This represents the worst-case scenario.
     */
    public long benchmarkRevenueScenario1(int year) {
        String sql = "SELECT SUM(oi.price) as total_revenue " +
                     "FROM order_items oi " +
                     "JOIN orders o ON oi.order_id = o.order_id " +
                     "WHERE o.order_purchase_timestamp >= CAST(? AS timestamp) " +
                     "AND o.order_purchase_timestamp < CAST(? AS timestamp)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Force sequential scan (disable indexes)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = off; SET enable_bitmapscan = off;");
            }

            String startDate = year + "-01-01 00:00:00";
            String endDate = (year + 1) + "-01-01 00:00:00";

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);

            // Print EXPLAIN ANALYZE plan
            printExplainPlan(conn, "Scenario 1: Baseline (No Indexes)", sql, startDate, endDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

            // Reset index settings
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
            }

        } catch (SQLException e) {
            System.err.println("Error executing Revenue Report Scenario 1: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Revenue Scenario 1 (Baseline - No Indexes): $" + totalRevenue + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query - Scenario 2 (Index Only)
     * Monolithic tables, with indexes enabled
     * Shows the benefit of indexing alone.
     */
    public long benchmarkRevenueScenario2(int year) {
        String sql = "SELECT SUM(oi.price) as total_revenue " +
                     "FROM order_items oi " +
                     "JOIN orders o ON oi.order_id = o.order_id " +
                     "WHERE o.order_purchase_timestamp >= CAST(? AS timestamp) " +
                     "AND o.order_purchase_timestamp < CAST(? AS timestamp)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Ensure indexes are enabled
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
            }

            String startDate = year + "-01-01 00:00:00";
            String endDate = (year + 1) + "-01-01 00:00:00";

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);

            // Print EXPLAIN ANALYZE plan
            printExplainPlan(conn, "Scenario 2: Index Only", sql, startDate, endDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Revenue Report Scenario 2: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Revenue Scenario 2 (Index Only): $" + totalRevenue + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query - Scenario 3 (Partition Only)
     * Partitioned tables, but indexes disabled
     * Shows the benefit of partitioning alone.
     */
    public long benchmarkRevenueScenario3(int year) {
        // Due to denormalization, we avoid the JOIN.
        String sql = "SELECT SUM(price) as total_revenue " +
                     "FROM order_items_partitioned " +
                     "WHERE order_date >= CAST(? AS date) " +
                     "AND order_date < CAST(? AS date)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Force sequential scan within partitions (disable indexes)
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = off; SET enable_bitmapscan = off;");
            }

            String startDate = year + "-01-01";
            String endDate = (year + 1) + "-01-01";

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);

            // Print EXPLAIN ANALYZE plan
            printExplainPlan(conn, "Scenario 3: Partition Only (No Indexes)", sql, startDate, endDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

            // Reset index settings
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
            }

        } catch (SQLException e) {
            System.err.println("Error executing Revenue Report Scenario 3: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Revenue Scenario 3 (Partition Only): $" + totalRevenue + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query - Scenario 4 (Fully Optimized)
     * Partitioned tables with indexes enabled
     * Shows the combined benefit of partitioning + indexing.
     */
    public long benchmarkRevenueScenario4(int year) {
        // Due to denormalization, we avoid the JOIN.
        String sql = "SELECT SUM(price) as total_revenue " +
                     "FROM order_items_partitioned " +
                     "WHERE order_date >= CAST(? AS date) " +
                     "AND order_date < CAST(? AS date)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Ensure indexes are enabled
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
            }

            String startDate = year + "-01-01";
            String endDate = (year + 1) + "-01-01";

            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);

            // Print EXPLAIN ANALYZE plan
            printExplainPlan(conn, "Scenario 4: Fully Optimized (Partition + Index)", sql, startDate, endDate);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Revenue Report Scenario 4: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Revenue Scenario 4 (Fully Optimized): $" + totalRevenue + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 3: Deep Pagination - Offset Method (on ecommerce_before baseline)
     * Scans and discards rows until the offset is reached.
     */
    public long benchmarkOffsetPagination(int offset, int limit) {
        String sql = "SELECT * FROM orders ORDER BY order_purchase_timestamp DESC LIMIT ? OFFSET ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            // Print EXPLAIN ANALYZE plan (use string params for the helper)
            String explainSql = "EXPLAIN ANALYZE SELECT * FROM orders ORDER BY order_purchase_timestamp DESC LIMIT " + limit + " OFFSET " + offset;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(explainSql)) {
                System.out.println("\n--- EXPLAIN ANALYZE: Offset Pagination (ecommerce_before) ---");
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1));
                }
                System.out.println("--- END EXPLAIN ---\n");
            } catch (SQLException e) {
                System.err.println("Could not get EXPLAIN plan for Offset: " + e.getMessage());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Offset Pagination on ecommerce_before: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Offset Pagination (ecommerce_before - OFFSET " + offset + "): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 3: Deep Pagination - Keyset (Seek) Method
     * Uses the last value from the previous page to seek the next set directly.
     * Runs on ecommerce_after which has indexes for optimal performance.
     */
    public long benchmarkKeysetPagination(String lastTimestampString, int limit) {
        String sql = "SELECT * FROM orders_partitioned WHERE order_purchase_timestamp < CAST(? AS timestamp) ORDER BY order_purchase_timestamp DESC LIMIT ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, lastTimestampString);
            pstmt.setInt(2, limit);

            // Print EXPLAIN ANALYZE plan
            String explainSql = "EXPLAIN ANALYZE SELECT * FROM orders_partitioned WHERE order_purchase_timestamp < '" + lastTimestampString + "' ORDER BY order_purchase_timestamp DESC LIMIT " + limit;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(explainSql)) {
                System.out.println("\n--- EXPLAIN ANALYZE: Keyset Pagination (ecommerce_after) ---");
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1));
                }
                System.out.println("--- END EXPLAIN ---\n");
            } catch (SQLException e) {
                System.err.println("Could not get EXPLAIN plan for Keyset: " + e.getMessage());
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Keyset Pagination on ecommerce_after: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Keyset Pagination (ecommerce_after - SEEK < '" + lastTimestampString + "'): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }
}
