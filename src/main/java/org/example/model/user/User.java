package org.example.model.user;

/**
 * Lớp trừu tượng đại diện cho một người dùng cơ bản trong hệ thống.
 * Cung cấp các thông tin nền tảng như tên đăng nhập và mật khẩu.
 */
public abstract class User extends Entity {
    /**
     * Tên đăng nhập của người dùng.
     */
    protected String username;

    /**
     * Mật khẩu của người dùng.
     */
    protected String password;

    /**
     * Khởi tạo một đối tượng User mới.
     *
     * @param id       Mã định danh duy nhất.
     * @param username Tên đăng nhập.
     * @param password Mật khẩu.
     */
    public User(final String id, final String username, final String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    /**
     * Lấy tên đăng nhập của người dùng.
     *
     * @return Tên đăng nhập hiện tại.
     */
    public final String getUsername() {
        return username;
    }

    /**
     * Cập nhật tên đăng nhập cho người dùng.
     *
     * @param username Tên đăng nhập mới.
     */
    public final void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Lấy mật khẩu của người dùng.
     *
     * @return Mật khẩu hiện tại.
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Cập nhật mật khẩu cho người dùng.
     *
     * @param password Mật khẩu mới.
     */
    public final void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Phương thức trừu tượng để hiển thị vai trò cụ thể của từng loại người dùng.
     */
    public abstract void displayRole();

    /**
     * Lấy vai trò của người dùng.
     *
     * @return vai trò (bidder, seller, admin)
     */
    public abstract String getRole();
}
