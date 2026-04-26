package benchmark;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseSetup {

    public void initializeDatabases() {
        Properties props = new Properties();
        try (InputStream in = DatabaseSetup.class.getClassLoader().getResourceAsStream("database.properties")) {
            props.load(in);
            String user = props.getProperty("username");
            String pass = props.getProperty("password");
            
            // Connect to default 'postgres' database to issue CREATE DATABASE
            String defaultUrl = "jdbc:postgresql://localhost:5432/postgres";
            try (Connection conn = DriverManager.getConnection(defaultUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("Attempting to create database 'ecommerce_before'...");
                try {
                    stmt.executeUpdate("CREATE DATABASE ecommerce_before");
                    System.out.println("  -> Created ecommerce_before successfully.");
                } catch (Exception e) {
                    System.out.println("  -> ecommerce_before might already exist or skipped.");
                }
                
                System.out.println("Attempting to create database 'ecommerce_after'...");
                try {
                    stmt.executeUpdate("CREATE DATABASE ecommerce_after");
                    System.out.println("  -> Created ecommerce_after successfully.");
                } catch (Exception e) {
                    System.out.println("  -> ecommerce_after might already exist or skipped.");
                }
            } catch (Exception e) {
                System.err.println("Could not connect to default 'postgres' database. Check credentials: " + e.getMessage());
                return;
            }

            System.out.println("\nReading ecommerce_db.sql script...");
            String sqlScript = new String(Files.readAllBytes(Paths.get("src/main/resources/ecommerce_db.sql")));

            // Connect to ecommerce_before and run script
            System.out.println("Executing schema script on ecommerce_before...");
            try (Connection conn = DriverManager.getConnection(props.getProperty("url-ecommerce-before-db"), user, pass);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlScript);
                System.out.println("  -> Schema created successfully on ecommerce_before.");
            } catch (Exception e) {
                System.out.println("  -> Schema execution skipped (tables might already exist).");
            }

            // Connect to ecommerce_after and run script
            System.out.println("Executing schema script on ecommerce_after...");
            try (Connection conn = DriverManager.getConnection(props.getProperty("url-ecommerce-after-db"), user, pass);
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlScript);
                System.out.println("  -> Schema created successfully on ecommerce_after.");
            } catch (Exception e) {
                System.out.println("  -> Schema execution skipped (tables might already exist).");
            }
            
            System.out.println("\n*** Database Setup Complete! ***");
            System.out.println("Please restart the application to establish permanent connection pools to the new databases.\n");
            System.exit(0);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void migrateDataToPartitionedTables() {
        System.out.println("Migrating data from unoptimized tables to partitioned schema...");
        String sqlOrders = "INSERT INTO orders_partitioned (order_id, customer_id, status, order_purchase_timestamp, order_date) " +
                           "SELECT order_id, customer_id, status, order_purchase_timestamp, DATE(order_purchase_timestamp) FROM orders " +
                           "ON CONFLICT DO NOTHING";
        
        String sqlOrderItems = "INSERT INTO order_items_partitioned (order_id, order_item_id, product_id, price, freight_value, order_date) " +
                               "SELECT oi.order_id, oi.order_item_id, oi.product_id, oi.price, oi.freight_value, DATE(o.order_purchase_timestamp) " +
                               "FROM order_items oi JOIN orders o ON oi.order_id = o.order_id " +
                               "ON CONFLICT DO NOTHING";
                               
        try (Connection conn = utils.DatabaseConnection.getAfterConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Executing Orders Migration... (This may take a minute)");
            long start1 = System.currentTimeMillis();
            int ordersMoved = stmt.executeUpdate(sqlOrders);
            System.out.println("-> Moved " + ordersMoved + " orders in " + (System.currentTimeMillis() - start1) + " ms.");
            
            System.out.println("Executing Order Items Migration... (This involves a massive JOIN, may take several minutes)");
            long start2 = System.currentTimeMillis();
            int itemsMoved = stmt.executeUpdate(sqlOrderItems);
            System.out.println("-> Moved " + itemsMoved + " order items in " + (System.currentTimeMillis() - start2) + " ms.");
            
            System.out.println("Migration successfully completed! Both ecommerce_before and ecommerce_after states are ready for comparison.");
        } catch (Exception e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
