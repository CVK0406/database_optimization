package benchmark;

import utils.DatabaseConnection;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;

public class DataLoader {

    /**
     * Truncate the unoptimized schema in ecommerce_before.
     */
    public void truncateBeforeTables() {
        String sql = "TRUNCATE TABLE order_items, orders, products, users CASCADE";
        try (Connection conn = DatabaseConnection.getBeforeConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println("Clearing ecommerce_before tables before ingestion...");
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error truncating before tables: " + e.getMessage());
        }
    }

    /**
     * Truncate the partitioned schema in ecommerce_after.
     */
    public void truncateAfterTables() {
        String sql = "TRUNCATE TABLE order_items_partitioned, orders_partitioned, products, users CASCADE";
        try (Connection conn = DatabaseConnection.getAfterConnection();
             Statement stmt = conn.createStatement()) {
            System.out.println("Clearing ecommerce_after tables before ingestion...");
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error truncating after tables: " + e.getMessage());
        }
    }

    public long insertOrdersSingle(String csvFilePath, int limit) {
        String sql = "INSERT INTO orders (order_id, customer_id, status, order_purchase_timestamp) VALUES (?, ?, ?, CAST(? AS timestamp))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
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
                if (data.length < 4)
                    continue;

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
        long elapsed = endTime - startTime;
        System.out.println("Single Insert: Inserted " + count + " rows in " + elapsed + " ms");
        return elapsed;
    }

    /**
     * Scenario 2: Enable reWriteBatchedInserts=true, batch records per insert.
     * This method represents the optimized approach for data ingestion.
     */
    public long insertOrdersBatch(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO orders (order_id, customer_id, status, order_purchase_timestamp) VALUES (?, ?, ?, CAST(? AS timestamp))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
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
                if (data.length < 4)
                    continue;

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
        long elapsed = endTime - startTime;
        System.out.println("Batch Insert: Inserted " + count + " rows in " + elapsed + " ms");
        return elapsed;
    }

    public long insertOrdersSingleAfter(String csvFilePath, int limit) {
        String sql = "INSERT INTO orders_partitioned (order_id, customer_id, status, order_purchase_timestamp, order_date) " +
                     "VALUES (?, ?, ?, CAST(? AS timestamp), CAST(? AS date))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 4)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[3]);

                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting orders into ecommerce_after (single): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Single Insert into orders_partitioned: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertOrdersBatchAfter(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO orders_partitioned (order_id, customer_id, status, order_purchase_timestamp, order_date) " +
                     "VALUES (?, ?, ?, CAST(? AS timestamp), CAST(? AS date))";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 4)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[3]);
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting orders into ecommerce_after (batch): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Batch Insert into orders_partitioned: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertOrderItemsSingleAfter(String csvFilePath, int limit) {
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TEMP TABLE IF NOT EXISTS order_items_staging (" +
                         "order_id VARCHAR(50), order_item_id INTEGER, product_id VARCHAR(50), price NUMERIC(10,2), freight_value NUMERIC(10,2)) ON COMMIT DROP");

                String insertStaging = "INSERT INTO order_items_staging (order_id, order_item_id, product_id, price, freight_value) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertStaging)) {
                    String line;
                    boolean firstLine = true;
                    while ((line = br.readLine()) != null && count < limit) {
                        if (firstLine) {
                            firstLine = false;
                            continue;
                        }
                        String[] data = line.split(",");
                        if (data.length < 5)
                            continue;

                        pstmt.setString(1, data[0]);
                        pstmt.setInt(2, Integer.parseInt(data[1].trim()));
                        pstmt.setString(3, data[2]);
                        pstmt.setBigDecimal(4, new java.math.BigDecimal(data[3].trim()));
                        pstmt.setBigDecimal(5, new java.math.BigDecimal(data[4].trim()));
                        pstmt.executeUpdate();
                        count++;
                    }
                }

                int inserted = stmt.executeUpdate("INSERT INTO order_items_partitioned (order_id, order_item_id, product_id, price, freight_value, order_date) " +
                                                 "SELECT oi.order_id, oi.order_item_id, oi.product_id, oi.price, oi.freight_value, o.order_date " +
                                                 "FROM order_items_staging oi JOIN orders_partitioned o ON oi.order_id = o.order_id");
                System.out.println("Flushed " + inserted + " rows from staging into order_items_partitioned");
            }

            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting order items into ecommerce_after (single): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Single Insert into order_items_partitioned: Processed " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertOrderItemsBatchAfter(String csvFilePath, int limit, int batchSize) {
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TEMP TABLE IF NOT EXISTS order_items_staging (" +
                         "order_id VARCHAR(50), order_item_id INTEGER, product_id VARCHAR(50), price NUMERIC(10,2), freight_value NUMERIC(10,2)) ON COMMIT DROP");

                String insertStaging = "INSERT INTO order_items_staging (order_id, order_item_id, product_id, price, freight_value) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insertStaging)) {
                    String line;
                    boolean firstLine = true;
                    while ((line = br.readLine()) != null && count < limit) {
                        if (firstLine) {
                            firstLine = false;
                            continue;
                        }
                        String[] data = line.split(",");
                        if (data.length < 5)
                            continue;

                        pstmt.setString(1, data[0]);
                        pstmt.setInt(2, Integer.parseInt(data[1].trim()));
                        pstmt.setString(3, data[2]);
                        pstmt.setBigDecimal(4, new java.math.BigDecimal(data[3].trim()));
                        pstmt.setBigDecimal(5, new java.math.BigDecimal(data[4].trim()));
                        pstmt.addBatch();
                        count++;
                        if (count % batchSize == 0) {
                            pstmt.executeBatch();
                        }
                    }
                    pstmt.executeBatch();
                }

                int inserted = stmt.executeUpdate("INSERT INTO order_items_partitioned (order_id, order_item_id, product_id, price, freight_value, order_date) " +
                                                 "SELECT oi.order_id, oi.order_item_id, oi.product_id, oi.price, oi.freight_value, o.order_date " +
                                                 "FROM order_items_staging oi JOIN orders_partitioned o ON oi.order_id = o.order_id");
                System.out.println("Flushed " + inserted + " rows from staging into order_items_partitioned");
            }

            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting order items into ecommerce_after (batch): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Batch Insert into order_items_partitioned: Processed " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertUsersSingle(String csvFilePath, int limit) {
        String sql = "INSERT INTO users (customer_id, customer_unique_id, zip_code, city, state) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[4]);
                
                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting users (single): " + e.getMessage());
        }
        return System.currentTimeMillis() - startTime;
    }

    public long insertUsersBatch(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO users (customer_id, customer_unique_id, zip_code, city, state) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[4]);
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0)
                    pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting users: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Inserted " + count + " Users in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertUsersSingleAfter(String csvFilePath, int limit) {
        String sql = "INSERT INTO users (customer_id, customer_unique_id, zip_code, city, state) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[4]);
                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting users into ecommerce_after (single): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Single Insert into users on ecommerce_after: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertUsersBatchAfter(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO users (customer_id, customer_unique_id, zip_code, city, state) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setString(3, data[2]);
                pstmt.setString(4, data[3]);
                pstmt.setString(5, data[4]);
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0)
                    pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting users into ecommerce_after (batch): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Batch Insert into users on ecommerce_after: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertProductsSingle(String csvFilePath, int limit) {
        String sql = "INSERT INTO products (product_id, category_name, weight_g) VALUES (?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 3)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setInt(3, Integer.parseInt(data[2].trim()));
                
                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting products (single): " + e.getMessage());
        }
        return System.currentTimeMillis() - startTime;
    }

    public long insertProductsBatch(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO products (product_id, category_name, weight_g) VALUES (?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 3)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                try {
                    pstmt.setInt(3, Integer.parseInt(data[2].trim()));
                } catch (NumberFormatException e) {
                    pstmt.setNull(3, java.sql.Types.INTEGER);
                }
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0)
                    pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting products: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Inserted " + count + " Products in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertProductsSingleAfter(String csvFilePath, int limit) {
        String sql = "INSERT INTO products (product_id, category_name, weight_g) VALUES (?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 3)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                pstmt.setInt(3, Integer.parseInt(data[2].trim()));
                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting products into ecommerce_after (single): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Single Insert into products on ecommerce_after: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertProductsBatchAfter(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO products (product_id, category_name, weight_g) VALUES (?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getAfterConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 3)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setString(2, data[1]);
                try {
                    pstmt.setInt(3, Integer.parseInt(data[2].trim()));
                } catch (NumberFormatException e) {
                    pstmt.setNull(3, java.sql.Types.INTEGER);
                }
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0)
                    pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting products into ecommerce_after (batch): " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Batch Insert into products on ecommerce_after: Inserted " + count + " rows in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }

    public long insertOrderItemsSingle(String csvFilePath, int limit) {
        String sql = "INSERT INTO order_items (order_id, order_item_id, product_id, price, freight_value) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setInt(2, Integer.parseInt(data[1].trim()));
                pstmt.setString(3, data[2]);
                pstmt.setBigDecimal(4, new java.math.BigDecimal(data[3].trim()));
                pstmt.setBigDecimal(5, new java.math.BigDecimal(data[4].trim()));
                
                pstmt.executeUpdate();
                count++;
            }
        } catch (Exception e) {
            System.err.println("Error inserting order_items (single): " + e.getMessage());
        }
        return System.currentTimeMillis() - startTime;
    }

    public long insertOrderItemsBatch(String csvFilePath, int limit, int batchSize) {
        String sql = "INSERT INTO order_items (order_id, order_item_id, product_id, price, freight_value) VALUES (?, ?, ?, ?, ?)";
        long startTime = System.currentTimeMillis();
        int count = 0;

        try (Connection conn = DatabaseConnection.getBeforeConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {

            conn.setAutoCommit(false);
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null && count < limit) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length < 5)
                    continue;

                pstmt.setString(1, data[0]);
                pstmt.setInt(2, Integer.parseInt(data[1].trim()));
                pstmt.setString(3, data[2]);
                pstmt.setBigDecimal(4, new java.math.BigDecimal(data[3].trim()));
                pstmt.setBigDecimal(5, new java.math.BigDecimal(data[4].trim()));
                pstmt.addBatch();
                count++;
                if (count % batchSize == 0)
                    pstmt.executeBatch();
            }
            pstmt.executeBatch();
            conn.commit();
        } catch (Exception e) {
            System.err.println("Error inserting order items: " + e.getMessage());
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Inserted " + count + " Order Items in " + (endTime - startTime) + " ms");
        return endTime - startTime;
    }
}
