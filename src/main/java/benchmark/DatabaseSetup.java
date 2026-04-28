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
            
            // Connect to ecommerce_before and run unoptimized schema
            System.out.println("\nReading ecommerce_before.sql script...");
            String sqlScriptBefore = new String(Files.readAllBytes(Paths.get("src/main/resources/ecommerce_before.sql")));
            System.out.println("Executing schema script on ecommerce_before...");
            try (Connection conn = DriverManager.getConnection(props.getProperty("url-ecommerce-before-db"), user, pass);
                  Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlScriptBefore);
                System.out.println("  -> Schema created successfully on ecommerce_before.");
            } catch (Exception e) {
                System.out.println("  -> Schema execution skipped (tables might already exist).");
            }
            
            // Connect to ecommerce_after and run the ecommerce_after schema
            System.out.println("\nReading ecommerce_after.sql script...");
            String sqlScriptAfter = new String(Files.readAllBytes(Paths.get("src/main/resources/ecommerce_after.sql")));
            System.out.println("Executing optimized schema script on ecommerce_after...");
            try (Connection conn = DriverManager.getConnection(props.getProperty("url-ecommerce-after-db"), user, pass);
                  Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sqlScriptAfter);
                System.out.println("  -> Partitioned schema created successfully on ecommerce_after.");
            } catch (Exception e) {
                System.out.println("  -> Partitioned schema execution skipped (tables might already exist).");
            }

            System.out.println("\n*** Database Setup Complete! ***");
            System.out.println("Please restart the application to establish permanent connection pools to the new databases.\n");
            System.exit(0);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
