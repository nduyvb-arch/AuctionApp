package org.example.server.manager;

public class AntiSniper {
    private static final long DEFAULT_EXTENSION_MILLIS = 30_000; // 30 giây
    private static final long DEFAULT_SNIPE_THRESHOLD_MILLIS = 30_000; // Ngưỡng phát hiện snipe

    private final long extensionMillis;
    private final long snipeThresholdMillis;
    private long endTime;

    // Constructor mặc định: ngưỡng 30s, gia hạn 30s
    public AntiSniper(long durationMillis) {
        this(durationMillis, DEFAULT_SNIPE_THRESHOLD_MILLIS, DEFAULT_EXTENSION_MILLIS);
    }

    // Constructor tuỳ chỉnh
    public AntiSniper(long durationMillis, long snipeThresholdMillis, long extensionMillis) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("Duration phải lớn hơn 0");
        }
        if (snipeThresholdMillis <= 0) {
            throw new IllegalArgumentException("Snipe threshold phải lớn hơn 0");
        }
        if (extensionMillis <= 0) {
            throw new IllegalArgumentException("Extension phải lớn hơn 0");
        }
        this.endTime = System.currentTimeMillis() + durationMillis;
        this.snipeThresholdMillis = snipeThresholdMillis;
        this.extensionMillis = extensionMillis;
    }

    // Kiểm tra phiên đã kết thúc chưa
    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    // Thời gian còn lại (milliseconds)
    public long getRemainingMillis() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    // Gọi mỗi khi có bid mới — tự động gia hạn nếu là snipe
    // Trả về true nếu đã gia hạn
    public boolean checkAndExtend() {
        if (getRemainingMillis() < snipeThresholdMillis) {
            endTime += extensionMillis;
            System.out.println(" Phát hiện snipe! Phiên gia hạn thêm "
                    + (extensionMillis / 1000) + " giây. Còn lại: "
                    + (getRemainingMillis() / 1000) + " giây");
            return true;
        }
        return false;
    }

    // Thời gian kết thúc dạng milliseconds (để UI hiển thị)
    public long getEndTime() { return endTime; }

    // Thời gian còn lại dạng dễ đọc "mm:ss"
    public String getRemainingFormatted() {
        long remaining = getRemainingMillis();
        long minutes = remaining / 60_000;
        long seconds = (remaining % 60_000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return "AntiSniper{remaining=" + getRemainingFormatted()
                + ", threshold=" + (snipeThresholdMillis / 1000) + "s"
                + ", extension=" + (extensionMillis / 1000) + "s}";
    }
}