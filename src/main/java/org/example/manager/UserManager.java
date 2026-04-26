package org.example.manager;

import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final String DB_URL = "jdbc:sqlite:auction.db";
    private static volatile UserManager instance;
    private List<User> users;
    private Connection connection;

    private UserManager() {
        try {
            // Kết nối đến database
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            loadUsersFromDB();
            System.out.println("Kết nối database thành công");
        } catch (SQLException e) {
            System.err.println("Lỗi kết nối database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Tạo bảng users nếu chưa tồn tại
     */
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id TEXT PRIMARY KEY, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "role TEXT NOT NULL, " +
                "balance REAL DEFAULT 0.0, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Bảng users đã sẵn sàng");
        }
    }

    /**
     * Tải danh sách users từ database
     */
    private void loadUsersFromDB() {
        users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String role = rs.getString("role");
                double balance = rs.getDouble("balance");

                User user;
                switch (role.toLowerCase()) {
                    case "bidder":
                        user = new Bidder(id, username, password, balance);
                        break;
                    case "seller":
                        user = new Seller(id, username, password);
                        break;
                    case "admin":
                        user = new Admin(id, username, password);
                        break;
                    default:
                        continue;
                }
                users.add(user);
            }
            System.out.println("Đã tải " + users.size() + " tài khoản từ database");
        } catch (SQLException e) {
            System.err.println("Lỗi khi tải dữ liệu: " + e.getMessage());
        }
    }

    /**
     * Tạo tài khoản mới và lưu vào database
     */
    public synchronized String createAccount(String username, String password, String role) {
        // Kiểm tra username đã tồn tại
        if (isUsernameExists(username)) {
            return "Tên đăng nhập đã tồn tại";
        }

        // Validate input
        if (username.length() < 3) {
            return "Tên đăng nhập phải có ít nhất 3 ký tự";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }

        // Tạo ID duy nhất
        String id = UUID.randomUUID().toString();

        // Insert vào database
        String sql = "INSERT INTO users (id, username, password, role, balance) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role.toLowerCase());
            pstmt.setDouble(5, 0.0); // Số dư ban đầu cho bidder
            pstmt.executeUpdate();

            // Tạo User object và add vào list in-memory
            User newUser;
            switch (role.toLowerCase()) {
                case "bidder":
                    newUser = new Bidder(id, username, password, 0.0);
                    break;
                case "seller":
                    newUser = new Seller(id, username, password);
                    break;
                case "admin":
                    newUser = new Admin(id, username, password);
                    break;
                default:
                    return "Loại tài khoản không hợp lệ";
            }
            users.add(newUser);
            System.out.println("Đã tạo tài khoản: " + username + " (" + role + ") và lưu vào database");
            return "✅ Tạo tài khoản thành công!";
        } catch (SQLException e) {
            return "Lỗi khi lưu dữ liệu: " + e.getMessage();
        }
    }

    /**
     * Đăng nhập
     */
    public synchronized User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String role = rs.getString("role");
                    double balance = rs.getDouble("balance");

                    User user;
                    switch (role.toLowerCase()) {
                        case "bidder":
                            user = new Bidder(id, username, password, balance);
                            break;
                        case "seller":
                            user = new Seller(id, username, password);
                            break;
                        case "admin":
                            user = new Admin(id, username, password);
                            break;
                        default:
                            return null;
                    }
                    System.out.println("Đăng nhập thành công: " + username);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đăng nhập: " + e.getMessage());
        }
        return null;
    }

    /**
     * Kiểm tra username đã tồn tại
     */
    private boolean isUsernameExists(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi kiểm tra username: " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy tất cả users (dành cho admin)
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    /**
     * Tìm user theo username
     */
    public User findUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String password = rs.getString("password");
                    String role = rs.getString("role");
                    double balance = rs.getDouble("balance");

                    switch (role.toLowerCase()) {
                        case "bidder":
                            return new Bidder(id, username, password, balance);
                        case "seller":
                            return new Seller(id, username, password);
                        case "admin":
                            return new Admin(id, username, password);
                        default:
                            return null;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi tìm user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật thông tin user
     */
    public synchronized boolean updateUser(User updatedUser) {
        String sql = "UPDATE users SET password = ?, balance = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, updatedUser.getPassword());

            // Lưu balance nếu là Bidder
            if (updatedUser instanceof Bidder) {
                pstmt.setDouble(2, ((Bidder) updatedUser).getBalance());
            } else {
                pstmt.setDouble(2, 0.0);
            }

            pstmt.setString(3, updatedUser.getId());
            pstmt.executeUpdate();

            // Cập nhật trong list in-memory
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId().equals(updatedUser.getId())) {
                    users.set(i, updatedUser);
                    System.out.println("Cập nhật user: " + updatedUser.getUsername());
                    return true;
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật user: " + e.getMessage());
            return false;
        }
    }

    /**
     * Đóng kết nối database (gọi khi đóng ứng dụng)
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Đóng kết nối database");
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}