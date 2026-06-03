package org.example.server.network;

import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.server.manager.AntiSniper;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {

    private final ReentrantLock lock = new ReentrantLock();
    private final String itemId;
    private double currentPrice;
    private String winnerName;
    private boolean isFinished;

    private final AntiSniper antiSniper;

    public AuctionSession(String itemId, double startingPrice) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
        this.antiSniper = null;
    }

    public AuctionSession(String itemId, double startingPrice, long durationMillis) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
        this.antiSniper = new AntiSniper(durationMillis);
        startCountdown();
    }

    public void placeBid(String bidderName, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        lock.lock();
        try {
            if (isFinished || (antiSniper != null && antiSniper.isExpired())) {
                throw new AuctionClosedException("Phiên đấu giá cho " + itemId + " đã đóng!");
            }

            if (bidAmount <= currentPrice) {
                throw new InvalidBidException("Giá đặt " + bidAmount
                        + " phải cao hơn giá hiện tại " + currentPrice);
            }

            this.currentPrice = bidAmount;
            this.winnerName = bidderName;
            System.out.println("Bid thành công: " + bidderName + " đặt " + bidAmount);

            if (antiSniper != null) {
                antiSniper.checkAndExtend();
            }
        } finally {
            lock.unlock();
        }
    }

    private void startCountdown() {
        Thread countdown = new Thread(() -> {
            while (!isFinished) {
                if (antiSniper != null && antiSniper.isExpired()) {
                    finishAuction();
                    System.out.println("Phiên " + itemId + " đã kết thúc! Người thắng: " + winnerName);
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        countdown.setDaemon(true);
        countdown.start();
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public long getRemainingMillis() {
        if (antiSniper == null) {
            return -1;
        }
        return antiSniper.getRemainingMillis();
    }

    public String getRemainingFormatted() {
        if (antiSniper == null) {
            return "∞";
        }
        return antiSniper.getRemainingFormatted();
    }

    public void finishAuction() {
        this.isFinished = true;
    }
}
