package org.example.model;

public class Vehicle extends Item {
    public Vehicle(String name, String type, String describe, double startingPrice, double bidIncrement) {
        super(name, type, describe, startingPrice, bidIncrement);
    }

    public Vehicle(String name, String type, double startingPrice, double bidIncrement) {
        super(name, type, startingPrice, bidIncrement);
    }
}
