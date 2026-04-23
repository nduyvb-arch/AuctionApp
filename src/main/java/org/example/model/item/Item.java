package org.example.model.item;

import org.example.model.user.Entity;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private static int itemCount = 0;
    protected final String id;
    protected String itemName;
    protected String type;
    protected String describe;
    protected double startingPrice;
    protected double bidIncrement;
    private double currentPrice;
    private String currentWinnerId;
    private AuctionStatus status = AuctionStatus.PENDING; // mặc định là trạng thái chờ
    private LocalDateTime endTime;

    public Item(String name, String type, String describe, double startingPrice, double bidIncrement) {
        this.id = "I" + itemCount;
        this.itemName = name;
        this.type = type;
        this.describe = describe;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
        itemCount++;
    }

    public Item(String name, String type, double startingPrice, double bidIncrement) {
        this(name, type, "", startingPrice, bidIncrement);
    }

    public void setName(String newName) {
        this.itemName = newName;
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescribe() {
        return this.describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    public String getItemName() {
        return this.itemName;
    }

    public double getStartingPrice() {
        return this.startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getBidIncrement() {
        return this.bidIncrement;
    }

    public void setBidIncrement(double bidIncrement) {
        this.bidIncrement = bidIncrement;
    }

    public double getCurrentPrice() {
        return this.currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getCurrentWinnerId() {
        return this.currentWinnerId;
    }

    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public AuctionStatus getStatus() {
        return this.status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getEndTime() {
        return this.endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

}
