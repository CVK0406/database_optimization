package benchmark;

import utils.DatabaseConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DataLoader {

    /**
     * Scenario 1: Measure time for single insert (row-by-row).
     * This method represents the bottleneck caused by network round-trips.
     */
    public void insertOrdersSingle(String csvFilePath, int limit) {
        String sql = "INSERT INTO orders (order_id, customer_id, status, order_purchase_timestamp) VALUES (?, ?, ?, CAST(? AS timestamp))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip the header row
                }
                String[] data = line.split(",");
                if (data.length < 4) continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]); // Let PostgreSQL parse the string via CAST

                pstmt.executeUpdate();
                count++;
            }

        } catch (SQLException | IOException e) {
            System.err.println("Error during single insert: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Single Insert: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
    }

    /**
     * Scenario 2: Enable reWriteBatchedInserts=true, batch records per insert.
     * This method represents the optimized approach for data ingestion.
     */
    public void insertOrdersBatch(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO orders (order_id, customer_id, status, order_purchase_timestamp) VALUES (?, ?, ?, CAST(? AS timestamp))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            
            conn.setAutoCommit(false); // CRITICAL: Disables implicit commits to massively speed up batch processing

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip the header row
                }
                String[] data = line.split(",");
                if (data.length < 4) continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);

                pstmt.addBatch();
                count++;

                // Execute the batch when we hit the batchSize
                if (count % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            
            // Execute any remaining queries in the final batch
            pstmt.executeBatch();
            conn.commit(); // Commit the transaction

        } catch (SQLException | IOException e) {
            System.err.println("Error during batch insert: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Batch Insert: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
    }
}
