package org.example.model;

import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {
    private String itemId;
    private double currentPrice;
    private String winnerName;
    private boolean isFinished;

    // Khóa ReentrantLock để đảm bảo tính đồng bộ khi nhiều người cùng bid [cite: 75]
    private final ReentrantLock lock = new ReentrantLock();

    public AuctionSession(String itemId, double startingPrice) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
    }

    public void placeBid(String bidderName, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        lock.lock(); // Bắt đầu bảo vệ dữ liệu [cite: 75]
        try {
            if (isFinished) {
                throw new AuctionClosedException("Phiên đấu giá cho " + itemId + " đã đóng!");
            }

            if (bidAmount <= currentPrice) {
                throw new InvalidBidException("Giá đặt " + bidAmount + " phải cao hơn giá hiện tại " + currentPrice);
            }

            // Cập nhật giá và người dẫn đầu
            this.currentPrice = bidAmount;
            this.winnerName = bidderName;
            System.out.println("Bid thành công: " + bidderName + " đặt " + bidAmount);

        } finally {
            lock.unlock(); // Giải phóng khóa trong khối finally để tránh deadlock [cite: 75]
        }
    }
    // Hàm này để testValidBid lấy giá về so sánh
    public double getCurrentPrice() {
        return currentPrice;
    }

    // Hàm này để testAuctionClosedException đóng phiên đấu giá
    public void finishAuction() {
        this.isFinished = true;
    }

    // Các Getter và Setter cần thiết khác...
}