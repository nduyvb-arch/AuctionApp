package org.example.common.model.item;

import java.io.Serializable;

public class Electronic extends Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public Electronic(String name, String type, String description, double startingPrice, double bidIncrement) {
        super(name, type, description, startingPrice, bidIncrement);
    }

    public Electronic(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
