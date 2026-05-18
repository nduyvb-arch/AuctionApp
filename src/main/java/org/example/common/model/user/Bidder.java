package org.example.common.model.user;

import java.io.Serializable;

public class Bidder extends User implements Serializable {

    private static final long serialVersionUID = 1L;

    public Bidder(final String id, final String username, final String password, final double balance) {
        super(id, username, password, balance);
    }

    @Override
    public final void displayRole() {
        System.out.println("Role: Bidder - Tài khoản : " + this.getUsername());
    }

    @Override
    public String getRole() {
        return "bidder";
    }

    public final void placeBid(final double amount) {
        System.out.println(this.getUsername() + " đang đặt giá: " + amount);
    }
}
