package org.example.model.user;

import java.io.Serializable;

public abstract class Entity implements Serializable {

    protected String id;

    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }
}
