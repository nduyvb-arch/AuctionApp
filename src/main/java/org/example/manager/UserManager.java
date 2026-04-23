package org.example.manager;

import org.example.model.user.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
<<<<<<< HEAD
    private static final String DB_URL = "jdbc:h2:./auction_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
=======
    private static final String DB_URL = "jdbc:sqlite:auction.db";
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
    private static volatile UserManager instance;
    private List<User> users;
    private Connection connection;

    private UserManager() {
<<<<<<< HEAD
        initializeDatabase();
        loadUsers();
=======
        try {
            // Kết nối đến database
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            loadUsersFromDB();
            System.out.println("✅ Kết nối database thành công");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối database: " + e.getMessage());
            e.printStackTrace();
        }
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

<<<<<<< HEAD
    private void initializeDatabase() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("H2 Driver not found: " + e.getMessage());
            return;
        }
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(255) UNIQUE NOT NULL," +
                    "password VARCHAR(255) NOT NULL," +
                    "role VARCHAR(50) NOT NULL," +
                    "balance DOUBLE DEFAULT 0.0" +
                    ")";
            stmt.execute(sql);
            System.out.println("Database initialized.");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
=======
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
            System.out.println("📊 Bảng users đã sẵn sàng");
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
        }
    }

    /**
     * Tải danh sách users từ database
     */
<<<<<<< HEAD
    private void loadUsers() {
        users = new ArrayList<>();
        String sql = "SELECT * FROM users";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
=======
    private void loadUsersFromDB() {
        users = new ArrayList<>();
        String sql = "SELECT * FROM users";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
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
            System.out.println("📂 Đã tải " + users.size() + " tài khoản từ database");
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
=======
            System.err.println("❌ Lỗi khi tải dữ liệu: " + e.getMessage());
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
        }
    }

    /**
     * Tạo tài khoản mới và lưu vào database
     */
    public synchronized String createAccount(String username, String password, String role) {
        // Kiểm tra username đã tồn tại
        if (isUsernameExists(username)) {
            return "❌ Tên đăng nhập đã tồn tại";
        }

        // Validate input
        if (username.length() < 3) {
            return "❌ Tên đăng nhập phải có ít nhất 3 ký tự";
        }
        if (password.length() < 6) {
            return "❌ Mật khẩu phải có ít nhất 6 ký tự";
        }

        // Tạo ID duy nhất
        String id = UUID.randomUUID().toString();

<<<<<<< HEAD
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
                return "❌ Loại tài khoản không hợp lệ";
        }

        // Lưu vào database
        String sql = "INSERT INTO users (id, username, password, role, balance) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.setDouble(5, role.equalsIgnoreCase("bidder") ? 0.0 : 0.0); // Balance only for bidder
            pstmt.executeUpdate();
=======
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
                    return "❌ Loại tài khoản không hợp lệ";
            }
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
            users.add(newUser);
            System.out.println("✅ Đã tạo tài khoản: " + username + " (" + role + ") và lưu vào database");
            return "✅ Tạo tài khoản thành công!";
        } catch (SQLException e) {
<<<<<<< HEAD
            System.err.println("Error creating account: " + e.getMessage());
            e.printStackTrace();
            return "❌ Lỗi khi lưu dữ liệu";
=======
            return "❌ Lỗi khi lưu dữ liệu: " + e.getMessage();
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
        }
    }

    /**
     * Đăng nhập
     */
    public synchronized User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
<<<<<<< HEAD
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
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
                System.out.println("✅ Đăng nhập thành công: " + username);
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error logging in: " + e.getMessage());
            e.printStackTrace();
=======

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
                    System.out.println("✅ Đăng nhập thành công: " + username);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi đăng nhập: " + e.getMessage());
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
        }
        return null;
    }

    /**
     * Kiểm tra username đã tồn tại
     */
    private boolean isUsernameExists(String username) {
<<<<<<< HEAD
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking username: " + e.getMessage());
            e.printStackTrace();
=======
        String sql = "SELECT 1 FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi kiểm tra username: " + e.getMessage());
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
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
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi tìm user: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật thông tin user
     */
    public synchronized boolean updateUser(User updatedUser) {
<<<<<<< HEAD
        String sql = "UPDATE users SET username = ?, password = ?, balance = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, updatedUser.getUsername());
            pstmt.setString(2, updatedUser.getPassword());
            if (updatedUser instanceof Bidder) {
                pstmt.setDouble(3, ((Bidder) updatedUser).getBalance());
            } else {
                pstmt.setDouble(3, 0.0);
            }
            pstmt.setString(4, updatedUser.getId());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                // Update in memory
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).getId().equals(updatedUser.getId())) {
                        users.set(i, updatedUser);
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
=======
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
                    System.out.println("✅ Cập nhật user: " + updatedUser.getUsername());
                    return true;
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi cập nhật user: " + e.getMessage());
            return false;
>>>>>>> d688cdcd330146c6d9feaa4526710d6fced9a9d3
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
            System.err.println("❌ Lỗi khi đóng kết nối: " + e.getMessage());
        }
    }
}