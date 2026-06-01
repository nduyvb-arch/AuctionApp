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

    // ========== ANTI SNIPER ==========
    private final AntiSniper antiSniper;

    // Constructor không có thời gian
    public AuctionSession(String itemId, double startingPrice) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
        this.antiSniper = null; // Không dùng anti-sniping
    }

    // Constructor có thời gian, dùng anti-sniping
    public AuctionSession(String itemId, double startingPrice, long durationMillis) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
        this.antiSniper = new AntiSniper(durationMillis);
        startCountdown();
    }

    // ========== PLACE BID ==========
    public void placeBid(String bidderName, double bidAmount)
            throws InvalidBidException, AuctionClosedException {

        lock.lock();
        try {
            // Kiểm tra phiên đã đóng chưa theo isFinished hoặc antiSniper hết giờ
            if (isFinished || (antiSniper != null && antiSniper.isExpired())) {
                throw new AuctionClosedException("Phiên đấu giá cho " + itemId + " đã đóng!");
            }

            if (bidAmount <= currentPrice) {
                throw new InvalidBidException("Giá đặt " + bidAmount
                        + " phải cao hơn giá hiện tại " + currentPrice);
            }

            // Cập nhật giá và người dẫn đầu
            this.currentPrice = bidAmount;
            this.winnerName = bidderName;
            System.out.println("Bid thành công: " + bidderName + " đặt " + bidAmount);

            // Anti-sniping: gia hạn nếu bid trong thời gian cuối
            if (antiSniper != null) {
                antiSniper.checkAndExtend();
            }
        } finally {
            lock.unlock();
        }
    }

    // ========== ANTI SNIPER COUNTDOWN ==========
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

    // ========== GETTERS ==========
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

    // Thời gian còn lại, trả về -1 nếu không dùng anti-sniping
    public long getRemainingMillis() {
        if (antiSniper == null) {
            return -1;
        }
        return antiSniper.getRemainingMillis();
    }

    // Thời gian còn lại dạng "mm:ss" để hiển thị trên UI
    public String getRemainingFormatted() {
        if (antiSniper == null) {
            return "∞";
        }
        return antiSniper.getRemainingFormatted();
    }

    // ========== FINISH ==========
    public void finishAuction() {
        this.isFinished = true;
    }
}
