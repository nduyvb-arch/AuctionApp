package org.example.common.model.item;

import org.example.common.model.user.Entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class Item extends Entity implements Serializable {

    private static final long serialVersionUID = 1L;
    protected String itemName;
    protected String type;
    protected String description;
    protected double startingPrice;
    protected double bidIncrement;
    private double currentPrice;
    private String currentWinnerId;
    private String sellerId;
    private AuctionStatus status = AuctionStatus.PENDING; // mặc định là trạng thái chờ
    private LocalDateTime endTime;

    public Item(String name, String type, String description, double startingPrice, double bidIncrement) {

        this.id = "I-" + UUID.randomUUID().toString().substring(0, 8);
        this.itemName = name;
        this.type = type;
        this.description = description;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;

        this.currentPrice = startingPrice;
    }

    public Item(String name, String type, double startingPrice, double bidIncrement) {
        this(name, type, "", startingPrice, bidIncrement);
    }

    public void setName(String newName) {
        this.itemName = newName;
    }


    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getSellerId() {
        return this.sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
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