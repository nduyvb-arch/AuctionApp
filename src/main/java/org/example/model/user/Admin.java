package org.example.model.user;
import java.io.Serializable;
/**
 * Lớp đại diện cho người dùng có quyền quản trị (Admin).
 */
public class Admin extends User implements Serializable {

    /**
     * Khởi tạo một đối tượng Admin mới.
     *
     * @param id       Mã định danh của quản trị viên.
     * @param username Tên đăng nhập của quản trị viên.
     * @param password Mật khẩu của quản trị viên.
     */
    public Admin(final String id, final String username, final String password) {
        super(id, username, password);
    }

    /**
     * Hiển thị vai trò và tên đăng nhập của quản trị viên.
     */
    @Override
    public final void displayRole() {
        System.out.println("Role: Admin - Tài khoản quản trị: " + this.username);
    }

    /**
     * Lấy vai trò của người dùng.
     */
    @Override
    public String getRole() {
        return "admin";
    }

    /**
     * Thực hiện hành động khóa tài khoản người dùng khác.
     *
     * @param user Đối tượng người dùng cần bị khóa.
     */
    public final void banUser(final User user) {
        System.out.println("Admin " + this.username + " đã khóa tài khoản: " + user.getUsername());
    }
}