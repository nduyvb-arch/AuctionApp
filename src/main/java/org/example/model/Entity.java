package org.example.model;

public abstract class Entity {

    protected String id;

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }
}
