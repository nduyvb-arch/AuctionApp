package org.example.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "duyananhluong";

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Tải driver của MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Mở kết nối với đầy đủ User/Password
                connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                System.out.println("Kết nối MySQL thành công: " + DB_URL);
                autoCreateTables();

            } catch (ClassNotFoundException e) {
                System.err.println("Lỗi: Không tìm thấy thư viện MySQL JDBC!");
                e.printStackTrace();
            } catch (SQLException e) {
                System.err.println("Lỗi kết nối MySQL: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }

    // Hàm tự động dựng bộ khung Database Đấu Giá
    private static void autoCreateTables() {
        try (Statement stmt = connection.createStatement()) {

            // 1. Bảng Users (Đã nâng cấp thêm Ví tiền và Role)
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(50) NOT NULL UNIQUE, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(50) NOT NULL DEFAULT 'bidder', " // Thêm cột role với giá trị mặc định
                    + "balance DECIMAL(15,2) DEFAULT 0.00" // Tiền nạp vào ví
                    + ")";
            stmt.execute(sqlUsers);

            // 2. Bảng Items (Phiên đấu giá)
            String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "description TEXT, "
                    + "start_price DECIMAL(15,2) NOT NULL, "
                    + "current_price DECIMAL(15,2) NOT NULL, "
                    + "seller_id INT, "
                    + "end_time DATETIME, "
                    + "FOREIGN KEY (seller_id) REFERENCES users(id)" // Trỏ về người bán
                    + ")";
            stmt.execute(sqlItems);

            // 3. Bảng Bids (Lịch sử đặt giá)
            String sqlBids = "CREATE TABLE IF NOT EXISTS bids ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "item_id INT, "
                    + "user_id INT, "
                    + "bid_amount DECIMAL(15,2) NOT NULL, "
                    + "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (item_id) REFERENCES items(id), " // Trỏ về món đồ
                    + "FOREIGN KEY (user_id) REFERENCES users(id)"   // Trỏ về người mua
                    + ")";
            stmt.execute(sqlBids);

            System.out.println("Đã khởi tạo toàn bộ CSDL cho Hệ Thống Đấu Giá thành công!");
        } catch (SQLException e) {
            System.err.println("Lỗi khi tự động tạo bảng: " + e.getMessage());
        }
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