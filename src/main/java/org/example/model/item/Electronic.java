package org.example.model.item;

import java.io.Serializable;

public class Electronic extends Item implements Serializable {
    public Electronic(String name, String type, String describe, double startingPrice, double bidIncrement) {
        super(name, type, describe, startingPrice, bidIncrement);
    }

    public Electronic(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
