package org.example.model.item;

import org.example.model.user.Entity;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected final String id;
    protected String itemName;
    protected String type;
    protected String describe;
    protected double startingPrice;
    protected double bidIncrement;
    private static int itemCount = 0;
    private double currentPrice;
    private String currentWinnerId;
    private AuctionStatus status = AuctionStatus.PENDING; // mặc định là trạng thái chờ
    private LocalDateTime endTime;

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

    public void setCurrentPrice(double currentPrice){ this.currentPrice = currentPrice;}

    public void setCurrentWinnerId(String currentWinnerId){
        this.currentWinnerId = currentWinnerId;
    }

    public void setStatus(AuctionStatus status)
    {
        this.status = status;
    }

    public void setEndTime(LocalDateTime endTime)
    {
        this.endTime = endTime;
    }

    public String getId()
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
        return this.bidIncrement;
    }

    public double getCurrentPrice()
    {
        return this.currentPrice;
    }

    public String getCurrentWinnerId()
    {
        return this.currentWinnerId;
    }

    public AuctionStatus getStatus()
    {
        return this.status;
    }

    public LocalDateTime getEndTime()
    {
        return this.endTime;
    }

}
