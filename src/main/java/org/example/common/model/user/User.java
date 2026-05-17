package org.example.common.model.user;

import java.io.Serializable;

public abstract class User extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String username;
    protected String password;
    protected double balance;
    private boolean isBanned = false;

    public User(final String id, final String username, final String password) {
        this(id, username, password, 0.0);
    }

    public User(final String id, final String username, final String password, final double balance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.balance = balance;
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

    public double getBalance() {
        return balance;
    }

    public void setBalance(final double balance) {
        this.balance = balance;
    }

    public abstract void displayRole();

    public abstract String getRole();

    public void setRole(String role) {
        // Thực hiện logic nếu cần, hiện tại là rỗng đối với abstract class
    }

    public void setBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    public boolean isBanned() {
        return this.isBanned;
    }
}
