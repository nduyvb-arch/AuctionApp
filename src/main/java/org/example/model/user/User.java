package org.example.model.user;

public abstract class User extends Entity {
    protected String Username;
    protected String Password;

    public User(String id, String Username, String Password){
        this.id = id;
        this.Username = Username;
        this.Password = Password;
    }

    public String getUsername(){
        return Username;
    }
    public void setUsername(String username){
        this.Username = username;
    }

    public String getPassword(){
        return Password;
    }
    public void setPassword(String password){
        this.Password = password;
    }

    public abstract void displayRole();
}
