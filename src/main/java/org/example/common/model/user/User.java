package org.example.common.model.user;
import java.io.Serializable;

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

    public abstract void displayRole();
    public abstract String getRole();
}
