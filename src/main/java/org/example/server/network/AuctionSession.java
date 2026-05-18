package org.example.server.network;

import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.example.server.manager.AutoBid;
import org.example.server.manager.AntiSniper;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

public class AuctionSession {

    private final ReentrantLock lock = new ReentrantLock();
    private final String itemId;
    private double currentPrice;
    private String winnerName;
    private boolean isFinished;

    // ========== AUTO BID ==========
    private final PriorityQueue<AutoBid> autoBidQueue = new PriorityQueue<>(
            Comparator.comparingDouble(AutoBid::getMaxBid).reversed()
    );

    // ========== ANTI SNIPER ==========
    private final AntiSniper antiSniper;

    // ── Constructor không có thời gian (giữ nguyên như cũ) ──
    public AuctionSession(String itemId, double startingPrice) {
        this.itemId = itemId;
        this.currentPrice = startingPrice;
        this.isFinished = false;
        this.antiSniper = null; // Không dùng anti-sniping
    }

    // ── Constructor có thời gian (dùng anti-sniping) ──
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
            // Kiểm tra phiên đã đóng chưa (theo isFinished hoặc antiSniper hết giờ)
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

            // Kích hoạt auto bid của người khác
            triggerAutoBids();

        } finally {
            lock.unlock();
        }
    }

    // ========== AUTO BID ==========
    public void registerAutoBid(String username, double maxBid, double increment) {
        lock.lock();
        try {
            // Xóa đăng ký cũ nếu đã có
            autoBidQueue.removeIf(a -> a.getUsername().equals(username));
            autoBidQueue.add(new AutoBid(username, maxBid, increment));
            System.out.println("Đăng ký auto bid: " + username
                    + " | max=" + maxBid + " | increment=" + increment);
        } finally {
            lock.unlock();
        }
    }

    private void triggerAutoBids() {
        // Lấy người có maxBid cao nhất trong hàng đợi
        while (!autoBidQueue.isEmpty()) {
            AutoBid top = autoBidQueue.peek();

            // Bỏ qua nếu người đang dẫn đầu chính là người có auto bid cao nhất
            if (top.getUsername().equals(winnerName)) break;

            if (top.canBid(currentPrice)) {
                double nextBid = top.nextBidAmount(currentPrice);
                this.currentPrice = nextBid;
                this.winnerName = top.getUsername();
                System.out.println("Auto bid: " + top.getUsername() + " tự động đặt " + nextBid);
                break;
            } else {
                // Vượt ngưỡng maxBid → loại khỏi hàng đợi
                autoBidQueue.poll();
                System.out.println("Auto bid hết ngân sách: " + top.getUsername() + " bị loại");
            }
        }
    }

    // ========== ANTI SNIPER COUNTDOWN ==========
    private void startCountdown() {
        Thread countdown = new Thread(() -> {
            while (!isFinished) {
                if (antiSniper != null && antiSniper.isExpired()) {
                    finishAuction();
                    System.out.println(" Phiên " + itemId + " đã kết thúc! Người thắng: " + winnerName);
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

    // Thời gian còn lại (trả về -1 nếu không dùng anti-sniping)
    public long getRemainingMillis() {
        if (antiSniper == null) return -1;
        return antiSniper.getRemainingMillis();
    }

    // Thời gian còn lại dạng "mm:ss" để hiển thị trên UI
    public String getRemainingFormatted() {
        if (antiSniper == null) return "∞";
        return antiSniper.getRemainingFormatted();
    }

    // ========== FINISH ==========
    public void finishAuction() {
        this.isFinished = true;
    }
}