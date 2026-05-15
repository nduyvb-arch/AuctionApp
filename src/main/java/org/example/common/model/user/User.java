package org.example.common.model.user;
import java.io.Serializable;

public abstract class User extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String username;
    protected String password;
    private boolean isBanned = false;

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
    
    // Thêm hàm setRole vì có thể User cần set lại role ở Controller
    public void setRole(String role) {
        // Thực hiện logic nếu cần, hiện tại là rỗng đối với abstract class
    }

    public void setBanned(boolean isBanned) {
        this.isBanned = isBanned;
    }

    public boolean isBanned()
    {
        return this.isBanned;
    }
}
