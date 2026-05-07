package org.example.common.model.item;

import java.io.Serializable;

public class Vehicle extends Item implements Serializable {
    public Vehicle(String name, String type, String describe, double startingPrice, double bidIncrement) {
        super(name, type, describe, startingPrice, bidIncrement);
    }

    public Vehicle(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
