//Class này tạo kết nối đến file auction.db. Bạn có thể gọi DatabaseManager.getConnection() để lấy connection.
package org.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:auction.db";  // File database sẽ tạo ở thư mục gốc dự án
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection(DB_URL);
                System.out.println("Kết nối database thành công: " + DB_URL);
            } catch (SQLException e) {
                System.err.println("Lỗi kết nối database: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Đã đóng kết nối database");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}