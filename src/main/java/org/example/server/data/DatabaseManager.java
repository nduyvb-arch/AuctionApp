package org.example.server.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static String DB_URL;
    private static String USER;
    private static String PASSWORD;
    private static boolean tablesCreated = false; // Cờ để đảm bảo chỉ tạo bảng một lần

    static {
        Map<String, String> env = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(".env"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }
            DB_URL = env.get("DB_URL");
            USER = env.get("DB_USER");
            PASSWORD = env.get("DB_PASSWORD");
        } catch (Exception e) {
            logger.error("Lỗi: Không tìm thấy hoặc đọc lỗi file .env!", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
        // Chỉ tạo bảng trong lần kết nối đầu tiên để tối ưu hóa
        if (!tablesCreated) {
            autoCreateTables(conn);
            tablesCreated = true;
        }
        return conn;
    }

    private static void autoCreateTables(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Bảng Users
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "username VARCHAR(50) NOT NULL UNIQUE, "
                    + "password VARCHAR(255) NOT NULL, "
                    + "role VARCHAR(50) NOT NULL DEFAULT 'bidder', "
                    + "balance DECIMAL(15,2) DEFAULT 0.00, "
                    + "is_banned TINYINT(1) DEFAULT 0"
                    + ")";
            stmt.execute(sqlUsers);

            // 2. Bảng Items
            String sqlItems = "CREATE TABLE IF NOT EXISTS items ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(100) NOT NULL, "
                    + "description TEXT, "
                    + "type VARCHAR(50), "
                    + "start_price DECIMAL(15,2) NOT NULL, "
                    + "bid_increment DECIMAL(15,2) NOT NULL, "
                    + "current_price DECIMAL(15,2) NOT NULL, "
                    + "seller_id INT, "
                    + "current_winner_id INT, "
                    + "status VARCHAR(20) NOT NULL DEFAULT 'PENDING', "
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

            logger.info("Đã khởi tạo/kiểm tra CSDL thành công!");
        } catch (SQLException e) {
            logger.error("Lỗi khi tự động tạo bảng: {}", e.getMessage(), e);
        }
    }

    // Phương thức này không còn cần thiết vì try-with-resources sẽ tự đóng kết nối
    public static void closeConnection() {
        // No-op
    }
}
