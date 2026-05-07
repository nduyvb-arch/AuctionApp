package org.example.model.user;
import java.io.Serializable;

public class Seller extends User implements Serializable {

    public Seller(final String id, final String username, final String password) {
        super(id, username, password);
    }

    @Override
    public final void displayRole() {
        System.out.println("Role: Seller - Tai khoan: " + this.getUsername());
    }

    @Override
    public String getRole() {
        return "seller";
    }

    public final void putItem(final String itemName) {
        System.out.println(this.username + " đã đăng sản phẩm: " + itemName);
    }
}