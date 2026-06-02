package org.example.client.controllers.auction;

public interface BidObserver {
    void onNewBidReceived(BidHistoryController.BidHistoryRecord newRecord);
}