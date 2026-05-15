package org.example.server.manager;

import org.example.common.model.user.Admin;
import org.example.common.model.user.Bidder;
import org.example.common.model.user.Seller;
import org.example.common.model.user.User;
import org.example.server.data.DatabaseManager;
import at.favre.lib.crypto.bcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    private final Logger logger = LoggerFactory.getLogger(UserManager.class);

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

                boolean isBanned = false;
                try {
                    isBanned = rs.getInt("is_banned") == 1;
                } catch (SQLException ignored) {
                    // Bỏ qua nếu DB chưa có cột này
                }

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

                // Set trạng thái ban
                user.setBanned(isBanned);
                users.add(user);
            }
            logger.info("Đã tải {} tài khoản từ database", users.size());
        } catch (SQLException e) {
            logger.error("Lỗi khi tải dữ liệu: {}", e.getMessage(), e);
        }
    }

    public synchronized String createAccount(String username, String password, String role) {
        if (isUsernameExists(username)) {
            return "Tên đăng nhập đã tồn tại";
        }
        if (username.length() < 3) {
            return "Tên đăng nhập phải có ít nhất 3 ký tự";
        }
        if (password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự";
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "INSERT INTO users (username, password, role, balance) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, role.toLowerCase());
            pstmt.setDouble(4, 0.0);
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                return "Lỗi khi tạo tài khoản, không có hàng nào được thêm.";
            }

            String newId;
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    newId = String.valueOf(generatedKeys.getLong(1));
                } else {
                    throw new SQLException("Tạo tài khoản thất bại, không lấy được ID.");
                }
            }

            User newUser;
            switch (role.toLowerCase()) {
                case "bidder": newUser = new Bidder(newId, username, hashedPassword, 0.0); break;
                case "seller": newUser = new Seller(newId, username, hashedPassword); break;
                case "admin":  newUser = new Admin(newId, username, hashedPassword); break;
                default: return "Loại tài khoản không hợp lệ";
            }
            users.add(newUser);
            logger.info("Tạo tài khoản thành công: {}", username);
            return "Tạo tài khoản thành công!";
        } catch (SQLException e) {
            logger.error("Lỗi khi lưu dữ liệu: {}", e.getMessage(), e);
            return "Lỗi khi lưu dữ liệu: " + e.getMessage();
        }
    }

    public synchronized User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {

                // 4. CHỐT CHẶN: Kiểm tra xem tài khoản có bị khóa không
                if (user.isBanned()) {
                    logger.warn("Tài khoản {} đang bị khóa cố gắng đăng nhập.", username);
                    return null; // Có thể throw Exception để ClientHandler báo lỗi chi tiết hơn
                }

                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
                if (result.verified) {
                    logger.info("Đăng nhập thành công: {}", username);
                    return user;
                } else {
                    logger.warn("Sai mật khẩu cho tài khoản: {}", username);
                    return null;
                }
            }
        }
        logger.warn("Không tìm thấy tài khoản: {}", username);
        return null;
    }

    private boolean isUsernameExists(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users);
    }

    public User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    public User findUserById(String id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    public synchronized boolean updateUser(User updatedUser) {
        String sql = "UPDATE users SET password = ?, balance = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, updatedUser.getPassword());

            if (updatedUser instanceof Bidder) {
                pstmt.setDouble(2, ((Bidder) updatedUser).getBalance());
            } else {
                pstmt.setDouble(2, 0.0);
            }

            pstmt.setString(3, updatedUser.getId());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                for (int i = 0; i < users.size(); i++) {
                    if (users.get(i).getId().equals(updatedUser.getId())) {
                        users.set(i, updatedUser);
                        logger.info("Cập nhật user: {}", updatedUser.getUsername());
                        break;
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            logger.error("Lỗi khi cập nhật user: {}", e.getMessage(), e);
            return false;
        }
    }

    public synchronized String updateUserRole(String userId, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole.toLowerCase());
            pstmt.setString(2, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                for (int i = 0; i < users.size(); i++) {
                    User user = users.get(i);
                    if (user.getId().equals(userId)) {
                        User updatedUser;
                        switch (newRole.toLowerCase()) {
                            case "bidder":
                                updatedUser = new Bidder(user.getId(), user.getUsername(), user.getPassword(), 0.0);
                                break;
                            case "seller":
                                updatedUser = new Seller(user.getId(), user.getUsername(), user.getPassword());
                                break;
                            case "admin":
                                updatedUser = new Admin(user.getId(), user.getUsername(), user.getPassword());
                                break;
                            default:
                                return "Invalid role";
                        }
                        users.set(i, updatedUser);
                        break;
                    }
                }
                return "Cập nhật quyền thành công!";
            }
            return "Không tìm thấy user.";
        } catch (SQLException e) {
            return "Lỗi cơ sở dữ liệu: " + e.getMessage();
        }
    }

    public String banUser(String userId) {
        User user = findUserById(userId);
        if (user == null) return "Người dùng không tồn tại!";

        user.setBanned(true);

        String sql = "UPDATE users SET is_banned = 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
            return "success";
        } catch (SQLException e) {
            logger.error("Lỗi khi ban user: {}", e.getMessage(), e);
            return "Lỗi Database";
        }
    }

    public void closeConnection() {
        DatabaseManager.closeConnection();
    }
}