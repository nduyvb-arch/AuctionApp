package org.example.model.user;
import java.io.Serializable;
/**
 * Lớp trừu tượng đại diện cho một người dùng cơ bản trong hệ thống.
 * Cung cấp các thông tin nền tảng như tên đăng nhập và mật khẩu.
 */
public abstract class User extends Entity implements Serializable {

    protected String username;
    protected String password;

    public User(final String id, final String username, final String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public final String getUsername() {
        return username;
    }
    public final void setUsername(final String username) {
        this.username = username;
    }

    public final String getPassword() {
        return password;
    }
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
