package org.example.model.user;
import java.io.Serializable;
/**
 * Lớp đại diện cho người bán hàng trong hệ thống đấu giá (Seller).
 */
public class Seller extends User implements Serializable {

    /**
     * Khởi tạo một đối tượng Seller mới.
     *
     * @param id       Mã định danh của người bán.
     * @param username Tên đăng nhập của người bán.
     * @param password Mật khẩu của người bán.
     */
    public Seller(final String id, final String username, final String password) {
        super(id, username, password);
    }

    /**
     * Hiển thị vai trò và tên đăng nhập của người bán.
     */
    @Override
    public final void displayRole() {
        System.out.println("Role: Seller - Tai khoan: " + this.getUsername());
    }

    /**
     * Lấy vai trò của người dùng.
     */
    @Override
    public String getRole() {
        return "seller";
    }

    /**
     * Thực hiện hành động đăng một sản phẩm mới lên sàn đấu giá.
     *
     * @param itemName Tên của sản phẩm cần đăng.
     */
    public final void putItem(final String itemName) {
        System.out.println(this.username + " đã đăng sản phẩm: " + itemName);
    }
}