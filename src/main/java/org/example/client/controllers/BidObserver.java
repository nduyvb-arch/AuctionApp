package org.example.client.controllers;

public interface BidObserver {
    void onNewBidReceived(BidHistoryController.BidHistoryRecord newRecord);
}