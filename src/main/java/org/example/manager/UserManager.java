package org.example.manager;

import org.example.data.StorageManager;
import org.example.model.user.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserManager {
    private static final String USERS_FILE = "users.dat";
    private static volatile UserManager instance;
    private List<User> users;

    private UserManager() {
        loadUsers();
    }

    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    /**
     * Tải danh sách users từ file
     */
    @SuppressWarnings("unchecked")
    private void loadUsers() {
        Object data = StorageManager.loadData(USERS_FILE);
        if (data instanceof List) {
            users = (List<User>) data;
            System.out.println("📂 Đã tải " + users.size() + " tài khoản từ file");
        } else {
            users = new ArrayList<>();
            System.out.println("🆕 Khởi tạo danh sách users mới");
        }
    }

    /**
     * Lưu danh sách users vào file
     */
    private void saveUsers() {
        boolean success = StorageManager.saveData(users, USERS_FILE);
        if (success) {
            System.out.println("💾 Đã lưu " + users.size() + " tài khoản");
        }
    }

    /**
     * Tạo tài khoản mới
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

        User newUser;
        switch (role.toLowerCase()) {
            case "bidder":
                newUser = new Bidder(id, username, password, 0.0); // Số dư ban đầu = 0
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

        users.add(newUser);
        saveUsers();

        System.out.println("✅ Đã tạo tài khoản: " + username + " (" + role + ")");
        return "✅ Tạo tài khoản thành công!";
    }

    /**
     * Đăng nhập
     */
    public synchronized User login(String username, String password) {
        for (User user : users) {
            if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                System.out.println("✅ Đăng nhập thành công: " + username);
                return user;
            }
        }
        return null; // Đăng nhập thất bại
    }

    /**
     * Kiểm tra username đã tồn tại
     */
    private boolean isUsernameExists(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return true;
            }
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
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    /**
     * Cập nhật thông tin user
     */
    public synchronized boolean updateUser(User updatedUser) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(updatedUser.getId())) {
                users.set(i, updatedUser);
                saveUsers();
                return true;
            }
        }
        return false;
    }
}
