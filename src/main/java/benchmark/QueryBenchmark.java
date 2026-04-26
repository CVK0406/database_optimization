package benchmark;

import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryBenchmark {

    /**
     * Problem 1: Purchase History Query (Power of Indexing)
     * Executes a 4-table join to retrieve the purchase history for a specific user.
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

        try (Connection conn = DatabaseConnection.getBeforeConnection()) {
            
            try (Statement stmt = conn.createStatement()) {
                if (!useIndex) {
                    stmt.execute("SET enable_indexscan = off; SET enable_bitmapscan = off;");
                } else {
                    stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, customerUniqueId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        rows++;
                    }
                }
            }

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET enable_indexscan = on; SET enable_bitmapscan = on;");
            }

        } catch (SQLException e) {
            System.err.println("Error executing Purchase History Query: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Purchase History (" + (useIndex ? "Indexed" : "Sequential Scan") + "): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query (Unoptimized / Index Only)
     * Executes an aggregation query over millions of rows with a JOIN.
     * This method represents Scenarios 1 & 2 from the report.
     */
    public long benchmarkRevenueReportUnoptimized(int year) {
        String sql = "SELECT SUM(oi.price) as total_revenue " +
                     "FROM order_items oi " +
                     "JOIN orders o ON oi.order_id = o.order_id " +
                     "WHERE o.order_purchase_timestamp >= CAST(? AS timestamp) " +
                     "AND o.order_purchase_timestamp < CAST(? AS timestamp)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, year + "-01-01 00:00:00");
            pstmt.setString(2, (year + 1) + "-01-01 00:00:00");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Unoptimized Revenue Report: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Unoptimized Revenue Report: Calculated total revenue (" + totalRevenue + ") for year " + year + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 2: Revenue Report Query (Partition Only / Index + Partition)
     * Executes an aggregation query on the denormalized, partitioned schema.
     * Avoids the JOIN entirely. Represents Scenarios 3 & 4.
     */
    public long benchmarkRevenueReportPartitioned(int year) {
        // Due to denormalization, we avoid the JOIN.
        String sql = "SELECT SUM(price) as total_revenue " +
                     "FROM order_items_partitioned " +
                     "WHERE order_date >= CAST(? AS date) " +
                     "AND order_date < CAST(? AS date)";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, year + "-01-01");
            pstmt.setString(2, (year + 1) + "-01-01");

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Partitioned Revenue Report: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Partitioned Revenue Report: Calculated total revenue (" + totalRevenue + ") for year " + year + " in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 3: Deep Pagination - Offset Method
     * Scans and discards rows until the offset is reached.
     */
    public long benchmarkOffsetPagination(int limit, int offset) {
        String sql = "SELECT * FROM orders ORDER BY order_purchase_timestamp DESC LIMIT ? OFFSET ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Offset Pagination: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Offset Pagination (OFFSET " + offset + "): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Problem 3: Deep Pagination - Keyset (Seek) Method
     * Uses the last value from the previous page to seek the next set directly.
     */
    public long benchmarkKeysetPagination(String lastTimestampString, int limit) {
        String sql = "SELECT * FROM orders WHERE order_purchase_timestamp < CAST(? AS timestamp) ORDER BY order_purchase_timestamp DESC LIMIT ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, lastTimestampString);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows++;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Keyset Pagination: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        long elapsed = endTime - startTime;
        System.out.println("Keyset Pagination (SEEK < '" + lastTimestampString + "'): Retrieved " + rows + " rows in " + elapsed + " ms");
        return elapsed;
    }
}
