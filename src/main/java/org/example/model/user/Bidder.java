package org.example.model.user;
import java.io.Serializable;
/**
 * Lớp đại diện cho người dùng tham gia đấu giá (Bidder).
 */
public class Bidder extends User implements Serializable {
    /**
     * Số dư tài khoản của người tham gia đấu giá.
     */
    private double balance;

    /**
     * Khởi tạo một đối tượng Bidder mới.
     *
     * @param id       Mã định danh của người đấu giá.
     * @param username Tên đăng nhập của người đấu giá.
     * @param password Mật khẩu của người đấu giá.
     * @param balance  Số dư ban đầu trong tài khoản.
     */
    public Bidder(final String id, final String username, final String password, final double balance) {
        super(id, username, password);
        this.balance = balance;
    }

    /**
     * Hiển thị vai trò và tên đăng nhập của người đấu giá.
     */
    @Override
    public final void displayRole() {
        System.out.println("Role: Bidder - Tài khoản : " + this.getUsername());
    }

    /**
     * Lấy vai trò của người dùng.
     */
    @Override
    public String getRole() {
        return "bidder";
    }

    /**
     * Thực hiện hành động đặt giá cho một sản phẩm.
     *
     * @param amount Số tiền đặt giá.
     */
    public final void placeBid(final double amount) {
        System.out.println(this.getUsername() + " đang đặt giá: " + amount);
    }

    /**
     * Cập nhật số dư mới cho tài khoản.
     *
     * @param newBalance Số dư mới.
     */
    public void setBalance(final double newBalance) {
        this.balance = newBalance;
    }

    /**
     * Lấy số dư hiện tại của tài khoản.
     *
     * @return Số dư hiện tại.
     */
    public double getBalance() {
        return this.balance;
    }
}