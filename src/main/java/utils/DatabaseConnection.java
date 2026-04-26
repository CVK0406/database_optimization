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
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);

    static {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in == null) {
                logger.error("Sorry, unable to find database.properties");
                throw new RuntimeException("database.properties not found");
            }
            props.load(in);

            config.setJdbcUrl(props.getProperty("url-ecommerce-before-db"));
            config.setUsername(props.getProperty("username"));
            config.setPassword(props.getProperty("password"));

            config.setMaximumPoolSize(10); // Maximum connection
            config.setMinimumIdle(5);      // Minimum connection waiting
            config.setIdleTimeout(300000); // Time waiting to get connection (ms)
            config.setConnectionTimeout(20000); // Time to get connection (ms)

            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("reWriteBatchedInserts", "true"); // Added for Batch Insert Optimization
            
            logger.info("Initial Hikari Connection Pool for database...");
            ds = new HikariDataSource(config);

        } catch (IOException e) {
            logger.error("Failed to load database.properties", e);
            throw new RuntimeException(e);
        }
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
