//Class này tạo kết nối đến file auction.db. Bạn có thể gọi DatabaseManager.getConnection() để lấy connection.
package org.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./auction_db";  // File database sẽ tạo ở thư mục gốc dự án
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load H2 driver
                Class.forName("org.h2.Driver");
                connection = DriverManager.getConnection(DB_URL);
                System.out.println("📂 Kết nối database thành công: " + DB_URL);
            } catch (ClassNotFoundException e) {
                System.err.println("❌ Lỗi không tìm thấy driver H2: " + e.getMessage());
                throw new SQLException("Driver not found", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔒 Đã đóng kết nối database");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}