package org.example.server.manager;

import org.example.common.model.user.Admin;
import org.example.common.model.user.Bidder;
import org.example.common.model.user.Seller;
import org.example.common.model.user.User;
import org.example.common.model.user.*;
import org.example.server.data.DatabaseManager;
import at.favre.lib.crypto.bcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    private void loadUsersFromDB() {
        String sql = "SELECT * FROM users";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Lấy ID dưới dạng String để tương thích chung
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
            e.printStackTrace();
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
            return "Tạo tài khoản thành công!";
        } catch (SQLException e) {
            e.printStackTrace();
            return "Lỗi khi lưu dữ liệu: " + e.getMessage();
        }
    }

    public synchronized User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                // So khớp mật khẩu nhập vào với mật khẩu đã băm trong bộ nhớ
                BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPassword());
                if (result.verified) {
                    System.out.println("Đăng nhập thành công: " + username);
                    return user;
                } else {
                    System.out.println("Sai mật khẩu cho tài khoản: " + username);
                    return null;
                }
            }
        }
        System.out.println("Không tìm thấy tài khoản: " + username);
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
                        System.out.println("Cập nhật user: " + updatedUser.getUsername());
                        break;
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật user: " + e.getMessage());
            return false;
        }
    }
    
    public void closeConnection() {
        DatabaseManager.closeConnection();
    }
}