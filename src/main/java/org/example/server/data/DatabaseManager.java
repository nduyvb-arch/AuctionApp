package org.example.server.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {


    private static String DB_URL;
    private static String USER;
    private static String PASSWORD;

    private static Connection connection;

    // Khối code này tự động lấy dữ liệu từ file .env khi Server khởi động
    static {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Bỏ qua dòng trống hoặc comment
                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }

            // Lấy dữ liệu gán vào biến
            DB_URL = env.get("DB_URL");
            USER = env.get("DB_USER");
            PASSWORD = env.get("DB_PASSWORD");

        } catch (Exception e) {
            System.err.println("Lỗi: Không tìm thấy hoặc đọc lỗi file .env! Đảm bảo bạn đã tạo file .env ở thư mục gốc.");
            e.printStackTrace();
        }
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                // Tải driver của MySQL
                Class.forName("com.mysql.cj.jdbc.Driver");

                System.out.println("=== KIỂM TRA ĐỌC FILE .ENV ===");
                System.out.println("DB_URL hiện tại là: " + DB_URL);
                System.out.println("USER hiện tại là: " + USER);
                System.out.println("==============================");

                // Mở kết nối với dữ liệu đã đọc từ file .env
                connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
                System.out.println("Kết nối MySQL thành công: " + DB_URL);

                // Tự động dựng bảng nếu chưa có
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


    private static void autoCreateTables() {
        try (Statement stmt = connection.createStatement()) {

            // 1. Bảng Users
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(50) NOT NULL UNIQUE, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(50) NOT NULL DEFAULT 'bidder', "
                    + "balance DECIMAL(15,2) DEFAULT 0.00"
                    + ")";
            stmt.execute(sqlUsers);

            // 2. Bảng Items
            String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "description TEXT, "
                    + "type VARCHAR(50), " // Thêm cột loại sản phẩm
                    + "start_price DECIMAL(15,2) NOT NULL, "
                    + "bid_increment DECIMAL(15,2) NOT NULL, " // Thêm cột bước giá
                    + "current_price DECIMAL(15,2) NOT NULL, "
                    + "seller_id INT, "
                    + "current_winner_id INT, " // Thêm cột người thắng hiện tại
                    + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', " // Thêm cột trạng thái
                    + "end_time DATETIME, "
                    + "FOREIGN KEY (seller_id) REFERENCES users(id), "
                    + "FOREIGN KEY (current_winner_id) REFERENCES users(id)"
                    + ")";
            stmt.execute(sqlItems);

            // 3. Bảng Bids
            String sqlBids = "CREATE TABLE IF NOT EXISTS bids ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "item_id INT, "
                    + "user_id INT, "
                    + "bid_amount DECIMAL(15,2) NOT NULL, "
                    + "bid_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (item_id) REFERENCES items(id), "
                    + "FOREIGN KEY (user_id) REFERENCES users(id)"
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
<<<<<<< HEAD:src/main/java/org/example/data/DatabaseManager.java

}
=======
}
>>>>>>> 2a560cbf6ff0cc80e03f038778e60284404e1e36:src/main/java/org/example/server/data/DatabaseManager.java
