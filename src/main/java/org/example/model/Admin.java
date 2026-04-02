package org.example.model;

public class Admin extends User {

    public Admin(String id, String Username, String Password) {
        super(id, Username, Password);
    }

    @Override
    public void displayRole() {
        System.out.println("Role: Admin - Tài khoản quản trị: " + this.Username);
    }

    public void banUser(User user) {
        System.out.println("Admin " + this.Username + " đã khóa tài khoản: " + user.getUsername());
    }
}