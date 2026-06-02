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
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserManager {

    private final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private static volatile UserManager instance;
    private List<User> users;

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();

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

                User user = createUserByRole(id, username, password, role, balance);
                if (user == null) {
                    logger.warn("Bỏ qua tài khoản {} vì role không hợp lệ: {}", username, role);
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

        String normalizedRole = normalizeRole(role);
        if (normalizedRole == null) {
            return "Loại tài khoản không hợp lệ";
        }

        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String sql = "INSERT INTO users (username, password, role, balance) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, username);
            pstmt.setString(2, hashedPassword);
            pstmt.setString(3, normalizedRole);
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
            newUser = createUserByRole(newId, username, hashedPassword, normalizedRole, 0.0);
            if (newUser == null) {
                return "Loại tài khoản không hợp lệ";
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
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    logger.warn("Không tìm thấy tài khoản: {}", username);
                    return null;
                }

                boolean isBanned = false;
                try {
                    isBanned = rs.getInt("is_banned") == 1;
                } catch (SQLException ignored) {
                    // Bỏ qua nếu DB chưa có cột này
                }

                if (isBanned) {
                    logger.warn("Tài khoản {} đang bị khóa cố gắng đăng nhập.", username);
                    return null;
                }

                String id = rs.getString("id");
                String storedUsername = rs.getString("username");
                String storedPassword = rs.getString("password");
                String role = rs.getString("role");
                double balance = rs.getDouble("balance");

                boolean passwordMatched = isPasswordMatched(password, storedPassword);
                if (!passwordMatched) {
                    logger.warn("Sai mật khẩu cho tài khoản: {}", username);
                    return null;
                }

                if (!isBcryptHash(storedPassword)) {
                    storedPassword = upgradePlainPasswordToBcrypt(id, password);
                }

                User user = createUserByRole(id, storedUsername, storedPassword, role, balance);
                if (user == null) {
                    logger.warn("Tài khoản {} có role không hợp lệ: {}", username, role);
                    return null;
                }

                user.setBanned(isBanned);
                syncUserInMemory(user);

                logger.info("Đăng nhập thành công: {} với role {}", username, user.getRole());
                return user;
            }
        } catch (SQLException e) {
            logger.error("Lỗi khi đăng nhập tài khoản {}: {}", username, e.getMessage(), e);
            return null;
        }
    }

    private User createUserByRole(String id, String username, String password, String role, double balance) {
        String normalizedRole = normalizeRole(role);
        if (normalizedRole == null) {
            return null;
        }

        switch (normalizedRole) {
            case "bidder":
                return new Bidder(id, username, password, balance);
            case "seller":
                return new Seller(id, username, password, balance);
            case "admin":
                return new Admin(id, username, password);
            default:
                return null;
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }

        String normalizedRole = Normalizer.normalize(role, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase()
                .replace("đ", "d")
                .replace("_", "")
                .replace("-", "")
                .replaceAll("[\\s\\u00A0]+", "");

        if ("bidder".equals(normalizedRole)
                || "nguoidaugia".equals(normalizedRole)
                || "buyer".equals(normalizedRole)) {
            return "bidder";
        }

        if ("seller".equals(normalizedRole)
                || "nguoiban".equals(normalizedRole)) {
            return "seller";
        }

        if ("admin".equals(normalizedRole)
                || "administrator".equals(normalizedRole)
                || "superadmin".equals(normalizedRole)
                || "root".equals(normalizedRole)
                || "quantri".equals(normalizedRole)
                || "quantrivien".equals(normalizedRole)) {
            return "admin";
        }

        return null;
    }

    public String getNormalizedRoleFromDB(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String sql = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return normalizeRole(rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            logger.warn("Không đọc được role trong database của user {}: {}", username, e.getMessage());
        }
        return null;
    }

    private boolean isPasswordMatched(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        String dbPassword = storedPassword.trim();

        if (isBcryptHash(dbPassword)) {
            try {
                return BCrypt.verifyer().verify(rawPassword.toCharArray(), dbPassword).verified;
            } catch (RuntimeException e) {
                logger.warn("Mật khẩu trong database không đúng định dạng BCrypt: {}", e.getMessage());
                return false;
            }
        }

        /*
         * Hỗ trợ tài khoản được nhập trực tiếp trong database với mật khẩu dạng plain text.
         * Nếu nhập mật khẩu trong DBeaver bị dư khoảng trắng đầu/cuối thì vẫn đăng nhập được.
         */
        return rawPassword.equals(dbPassword);
    }

    private boolean isBcryptHash(String password) {
        if (password == null) {
            return false;
        }
        String value = password.trim();
        return value.startsWith("$2a$")
                || value.startsWith("$2b$")
                || value.startsWith("$2y$");
    }

    private String upgradePlainPasswordToBcrypt(String userId, String rawPassword) {
        String hashedPassword = BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray());
        String sql = "UPDATE users SET password = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, userId);
            pstmt.executeUpdate();
            logger.info("Đã tự động mã hóa BCrypt cho mật khẩu tài khoản id {}", userId);
            return hashedPassword;
        } catch (SQLException e) {
            logger.warn("Không thể tự động mã hóa mật khẩu tài khoản id {}: {}", userId, e.getMessage());
            return rawPassword;
        }
    }

    private void syncUserInMemory(User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updatedUser.getId())) {
                users.set(i, updatedUser);
                return;
            }
        }
        users.add(updatedUser);
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

            pstmt.setDouble(2, updatedUser.getBalance());

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
        String normalizedRole = normalizeRole(newRole);
        if (normalizedRole == null) {
            return "Invalid role";
        }

        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, normalizedRole);
            pstmt.setString(2, userId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                for (int i = 0; i < users.size(); i++) {
                    User user = users.get(i);
                    if (user.getId().equals(userId)) {
                        User updatedUser;
                        switch (normalizedRole) {
                            case "bidder":
                                updatedUser = new Bidder(user.getId(), user.getUsername(), user.getPassword(), user.getBalance());
                                break;
                            case "seller":
                                updatedUser = new Seller(user.getId(), user.getUsername(), user.getPassword(), user.getBalance());
                                break;
                            case "admin":
                                updatedUser = new Admin(user.getId(), user.getUsername(), user.getPassword());
                                break;
                            default:
                                return "Invalid role";
                        }
                        updatedUser.setBanned(user.isBanned());
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

    public synchronized String unbanUser(String userId) {
        User user = findUserById(userId);
        if (user == null) {
            return "Người dùng không tồn tại!";
        }

        user.setBanned(false);

        String sql = "UPDATE users SET is_banned = 0 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
            return "success";
        } catch (SQLException e) {
            logger.error("Lỗi khi mở khóa user: {}", e.getMessage(), e);
            return "Lỗi Database";
        }
    }

    public synchronized User topUpBalance(String userId, double amount, String method) {
        if (userId == null || userId.isBlank() || amount <= 0) {
            return null;
        }

        boolean success = addBalance(userId, amount);

        if (!success) {
            return null;
        }

        return findUserById(userId);
    }

    public synchronized boolean subtractBalance(String userId, double amount) {
        User user = findUserById(userId);
        if (user != null && user.getBalance() >= amount) {
            // 1. Cập nhật RAM NGAY LẬP TỨC (0 mili-giây)
            user.setBalance(user.getBalance() - amount);

            // 2. Ném việc lưu DB cho luồng ngầm làm, không bắt Client phải chờ
            dbExecutor.submit(() -> {
                String sql = "UPDATE users SET balance = ? WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, user.getBalance());
                    pstmt.setInt(2, Integer.parseInt(user.getId()));
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Lỗi cập nhật trừ tiền user: {}", e.getMessage());
                }
            });
            return true;
        }
        return false;
    }

    public synchronized boolean addBalance(String userId, double amount) {
        User user = findUserById(userId);
        if (user != null) {
            // 1. Cập nhật RAM NGAY LẬP TỨC
            user.setBalance(user.getBalance() + amount);

            // 2. Ném việc lưu DB cho luồng ngầm làm
            dbExecutor.submit(() -> {
                String sql = "UPDATE users SET balance = ? WHERE id = ?";
                try (Connection conn = DatabaseManager.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setDouble(1, user.getBalance());
                    pstmt.setInt(2, Integer.parseInt(user.getId()));
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Lỗi cập nhật cộng tiền user: {}", e.getMessage());
                }
            });
            return true;
        }
        return false;
    }
}