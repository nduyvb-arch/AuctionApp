package org.example.model.item;

public class Art extends Item {
    public Art(String name, String type, String describe, double startingPrice, double bidIncrement) {
        super(name, type, describe, startingPrice, bidIncrement);
    }

    public Art(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
