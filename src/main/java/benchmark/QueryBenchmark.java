package benchmark;

import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class QueryBenchmark {

    /**
     * Problem 1: Purchase History Query (Power of Indexing)
     * Executes a 4-table join to retrieve the purchase history for a specific user.
     */
    public void benchmarkPurchaseHistoryQuery(String customerUniqueId) {
        String sql = "SELECT o.order_id, o.order_purchase_timestamp, p.category_name, oi.price " +
                     "FROM orders o " +
                     "JOIN users u ON o.customer_id = u.customer_id " +
                     "JOIN order_items oi ON o.order_id = oi.order_id " +
                     "JOIN products p ON oi.product_id = p.product_id " +
                     "WHERE u.customer_unique_id = ? " +
                     "ORDER BY o.order_purchase_timestamp DESC";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, customerUniqueId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rows++;
                    // Optionally process results:
                    // String orderId = rs.getString("order_id");
                    // System.out.println("Found order: " + orderId);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Purchase History Query: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Purchase History Query: Retrieved " + rows + " rows in " + (endTime - startTime) + " ms");
    }

    /**
     * Problem 2: Revenue Report Query (Power of Table Partitioning)
     * Executes an aggregation query over millions of rows without optimization.
     */
    public void benchmarkRevenueReportQuery(int year) {
        // Without denormalization and partitioning, we join orders and order_items
        String sql = "SELECT SUM(oi.price) as total_revenue " +
                     "FROM order_items oi " +
                     "JOIN orders o ON oi.order_id = o.order_id " +
                     "WHERE EXTRACT(YEAR FROM o.order_purchase_timestamp) = ?";

        long startTime = System.currentTimeMillis();
        double totalRevenue = 0.0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, year);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenue = rs.getDouble("total_revenue");
                }
            }

        } catch (SQLException e) {
            System.err.println("Error executing Revenue Report Query: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Revenue Report Query: Calculated total revenue (" + totalRevenue + ") for year " + year + " in " + (endTime - startTime) + " ms");
    }

    /**
     * Problem 3: Deep Pagination - Offset Method
     * Scans and discards rows until the offset is reached.
     */
    public void benchmarkOffsetPagination(int limit, int offset) {
        String sql = "SELECT * FROM orders ORDER BY order_purchase_timestamp DESC LIMIT ? OFFSET ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getConnection();
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
        System.out.println("Offset Pagination (OFFSET " + offset + "): Retrieved " + rows + " rows in " + (endTime - startTime) + " ms");
    }

    /**
     * Problem 3: Deep Pagination - Keyset (Seek) Method
     * Uses the last value from the previous page to seek the next set directly.
     */
    public void benchmarkKeysetPagination(String lastTimestampString, int limit) {
        String sql = "SELECT * FROM orders WHERE order_purchase_timestamp < CAST(? AS timestamp) ORDER BY order_purchase_timestamp DESC LIMIT ?";

        long startTime = System.currentTimeMillis();
        int rows = 0;

        try (Connection conn = DatabaseConnection.getConnection();
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
        System.out.println("Keyset Pagination (SEEK < '" + lastTimestampString + "'): Retrieved " + rows + " rows in " + (endTime - startTime) + " ms");
    }
}
