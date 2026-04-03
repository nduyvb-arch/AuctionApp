package org.example.model;

public abstract class Item extends Entity {
    protected final String id;
    protected String itemName;
    protected String type;
    protected String describe;
    protected double startingPrice;
    protected double bidIncrement;
    private static int itemCount = 0;

    public Item(String name, String type,String describe, double startingPrice, double bidIncrement )
    {
        this.id = "I" + itemCount;
        this.itemName = name;
        this.type = type;
        this.describe = describe;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        itemCount++;
    }

    public Item(String name, String type, double startingPrice, double bidIncrement )
    {
        this(name, type, "", startingPrice, bidIncrement);
    }

    public void setDescribe(String describe)
    {
        this.describe = describe;
    }

    public void setName(String newName)
    {
        this.itemName = newName;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setStartingPrice(double startingPrice)
    {
        this.startingPrice = startingPrice;
    }

    public void setBidIncrement(double bidIncrement)
    {
        this.bidIncrement = bidIncrement;
    }

    public String getID()
    {
        return this.id;
    }

    public String getType()
    {
        return this.type;
    }

    public String getDescribe()
    {
        return this.describe;
    }

    public String getItemName()
    {
        return this.itemName;
    }

    public double getStartingPrice()
    {
        return this.startingPrice;
    }

    public double getBidIncrement() {
        return bidIncrement;
    }

}
