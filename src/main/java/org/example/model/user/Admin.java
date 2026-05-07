package org.example.model.user;
import java.io.Serializable;

public class Admin extends User implements Serializable {

    public Admin(final String id, final String username, final String password) {
        super(id, username, password);
    }

    @Override
    public final void displayRole() {
        System.out.println("Role: Admin - Tài khoản quản trị: " + this.username);
    }

    @Override
    public String getRole() {
        return "admin";
    }

    public final void banUser(final User user) {
        System.out.println("Admin " + this.username + " đã khóa tài khoản: " + user.getUsername());
    }
}