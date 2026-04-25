import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Main {
    static void main() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Connected to database.");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
