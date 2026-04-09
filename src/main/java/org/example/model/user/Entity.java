package org.example.model.user;

public abstract class Entity {

    protected String id;

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }
}
