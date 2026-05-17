package org.example.server.manager;

public class AutoBid {
    private final String username;
    private final double maxBid;      // Giá tối đa người dùng chấp nhận
    private final double increment;   // Mỗi lần tăng bao nhiêu

    public AutoBid(String username, double maxBid, double increment) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username không được để trống");
        }
        if (maxBid <= 0) {
            throw new IllegalArgumentException("MaxBid phải lớn hơn 0");
        }
        if (increment <= 0) {
            throw new IllegalArgumentException("Increment phải lớn hơn 0");
        }
        this.username = username;
        this.maxBid = maxBid;
        this.increment = increment;
    }

    public String getUsername() { return username; }
    public double getMaxBid()   { return maxBid; }
    public double getIncrement(){ return increment; }

    // Tính giá đặt tiếp theo dựa trên giá hiện tại
    public double nextBidAmount(double currentPrice) {
        return currentPrice + increment;
    }

    // Kiểm tra còn đủ ngân sách để đặt giá tiếp không
    public boolean canBid(double currentPrice) {
        return nextBidAmount(currentPrice) <= maxBid;
    }

    @Override
    public String toString() {
        return "AutoBid{username='" + username + "', maxBid=" + maxBid + ", increment=" + increment + "}";
    }
}
