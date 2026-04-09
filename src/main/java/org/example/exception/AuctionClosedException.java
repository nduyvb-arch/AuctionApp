package org.example.exception;

// Ngoại lệ khi phiên đấu giá đã kết thúc nhưng vẫn có người đặt giá
public class AuctionClosedException extends Exception {
    public AuctionClosedException(String message) {
        super(message);
    }
}