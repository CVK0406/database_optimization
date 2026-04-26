package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static HikariDataSource dsBefore;
    private static HikariDataSource dsAfter;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    static {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in == null) {
                logger.error("Sorry, unable to find database.properties");
                throw new RuntimeException("database.properties not found");
            }
            props.load(in);

            // Configure Before DB (ecommerce_before)
            HikariConfig configBefore = new HikariConfig();
            configBefore.setJdbcUrl(props.getProperty("url-ecommerce-before-db"));
            configBefore.setUsername(props.getProperty("username"));
            configBefore.setPassword(props.getProperty("password"));
            configBefore.setInitializationFailTimeout(-1); // Don't crash if DB is missing at startup
            applyCommonSettings(configBefore);
            
            logger.info("Initializing Hikari Connection Pool for ecommerce_before...");
            dsBefore = new HikariDataSource(configBefore);

            // Configure After DB (ecommerce_after)
            HikariConfig configAfter = new HikariConfig();
            configAfter.setJdbcUrl(props.getProperty("url-ecommerce-after-db"));
            configAfter.setUsername(props.getProperty("username"));
            configAfter.setPassword(props.getProperty("password"));
            configAfter.setInitializationFailTimeout(-1); // Don't crash if DB is missing at startup
            applyCommonSettings(configAfter);

            logger.info("Initializing Hikari Connection Pool for ecommerce_after...");
            dsAfter = new HikariDataSource(configAfter);

        } catch (IOException e) {
            logger.error("Failed to load database.properties", e);
            throw new RuntimeException(e);
        }
    }

    private static void applyCommonSettings(HikariConfig config) {
        config.setMaximumPoolSize(10); // Maximum connection
        config.setMinimumIdle(5);      // Minimum connection waiting
        config.setIdleTimeout(300000); // Time waiting to get connection (ms)
        config.setConnectionTimeout(20000); // Time to get connection (ms)
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("reWriteBatchedInserts", "true"); // Added for Batch Insert Optimization
    }

    private DatabaseConnection() {}

    public static Connection getBeforeConnection() throws SQLException {
        return dsBefore.getConnection();
    }

    public static Connection getAfterConnection() throws SQLException {
        return dsAfter.getConnection();
    }
}
