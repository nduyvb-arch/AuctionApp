package org.example.manager;

import org.example.model.user.*;
import org.example.data.DatabaseManager;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {

    private static volatile UserManager instance;
    private List<User> users;

    private UserManager() {
        users = new ArrayList<>();
        loadUsersFromDB();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Tải danh sách users từ database
     */
    private void loadUsersFromDB() {
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
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
     * Tạo tài khoản mới an toàn với Bcrypt
     */
    public synchronized String createAccount(String username, String password, String role) {
        if (isUsernameExists(username)) return "Tên đăng nhập đã tồn tại";
        if (username.length() < 3) return "Tên đăng nhập phải có ít nhất 3 ký tự";
        if (password.length() < 6) return "Mật khẩu phải có ít nhất 6 ký tự";

        String id = UUID.randomUUID().toString();

        // Băm mật khẩu
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());

        String sql = "INSERT INTO users (id, username, password, role, balance) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, username);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, role.toLowerCase());
            pstmt.setDouble(5, 0.0);
            pstmt.executeUpdate();

            // Cập nhật RAM (In-memory)
            User newUser;
            switch (role.toLowerCase()) {
                case "bidder": newUser = new Bidder(id, username, hashedPassword, 0.0); break;
                case "seller": newUser = new Seller(id, username, hashedPassword); break;
                case "admin":  newUser = new Admin(id, username, hashedPassword); break;
                default: return "Loại tài khoản không hợp lệ";
            }
            users.add(newUser);
            return "Tạo tài khoản thành công!";
        } catch (SQLException e) {
            return "Lỗi khi lưu dữ liệu: " + e.getMessage();
        }
    }

    /**
     * Đăng nhập kiểu mới: So khớp mã băm
     */
    public synchronized User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), storedHash);

                    if (result.verified) {
                        String id = rs.getString("id");
                        String role = rs.getString("role");
                        double balance = rs.getDouble("balance");

                        User user;
                        switch (role.toLowerCase()) {
                            case "bidder": user = new Bidder(id, username, storedHash, balance); break;
                            case "seller": user = new Seller(id, username, storedHash); break;
                            case "admin":  user = new Admin(id, username, storedHash); break;
                            default: return null;
                        }
                        System.out.println("Đăng nhập thành công: " + username);
                        return user;
                    } else {
                        System.out.println("Sai mật khẩu cho tài khoản: " + username);
                        return null;
                    }
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
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        // Đã bọc lại bằng Try-with-resources
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String id = rs.getString("id");
                    String password = rs.getString("password");
                    String role = rs.getString("role");
                    double balance = rs.getDouble("balance");

                    switch (role.toLowerCase()) {
                        case "bidder": return new Bidder(id, username, password, balance);
                        case "seller": return new Seller(id, username, password);
                        case "admin":  return new Admin(id, username, password);
                        default: return null;
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

        // Đã bọc lại bằng Try-with-resources
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, updatedUser.getPassword());

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
}