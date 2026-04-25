package utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;

    static {
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/ecommerce_before");
        config.setUsername("postgres");
        config.setPassword("root");

        config.setMaximumPoolSize(10); // Maximum connection
        config.setMinimumIdle(5);      // Minimum connection waiting
        config.setIdleTimeout(300000); // Time waiting to get connection (ms)
        config.setConnectionTimeout(20000); // Time to get connection (ms)

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        ds = new HikariDataSource(config);
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }
}
