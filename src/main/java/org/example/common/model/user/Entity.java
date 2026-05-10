package org.example.common.model.user;

import java.io.Serializable;

public abstract class Entity implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String id;

    public final String getId() {
        return id;
    }

    public final void setId(final String id) {
        this.id = id;
    }
}
