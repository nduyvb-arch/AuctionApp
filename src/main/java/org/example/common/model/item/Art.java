package org.example.common.model.item;

import java.io.Serializable;

public class Art extends Item implements Serializable {
    private static final long serialVersionUID = 1L;
    public Art(String name, String type, String description, double startingPrice, double bidIncrement) {
        super(name, type, description, startingPrice, bidIncrement);
    }

    public Art(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
