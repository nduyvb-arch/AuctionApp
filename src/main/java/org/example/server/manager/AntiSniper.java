package org.example.server.manager;

import org.example.common.model.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class AntiSniper {
    private static final Logger logger = LoggerFactory.getLogger(AntiSniper.class);

    // =========================================================================
    // PHẦN 1: TÍNH NĂNG MỚI (DÀNH CHO AUCTION MANAGER & DATABASE)
    // =========================================================================
    private static final int TRIGGER_THRESHOLD_MILLIS = 30_000;
    private static final int EXTENSION_SECONDS = 30;

    public static boolean applyAntiSniper(Item item) {
        if (item.getEndTime() == null) return false;

        LocalDateTime now = LocalDateTime.now();
        long milisLeft = ChronoUnit.MILLIS.between(now, item.getEndTime());

        if (milisLeft > 0 && milisLeft <= TRIGGER_THRESHOLD_MILLIS) {
            LocalDateTime newEndTime = item.getEndTime().plusSeconds(EXTENSION_SECONDS);
            item.setEndTime(newEndTime);
            logger.info("[ANTI-SNIPER] Phát hiện 'bắn tỉa' sản phẩm '{}'! Gia hạn thêm {} giây.",
                    item.getItemName(), EXTENSION_SECONDS);
            return true;
        }
        return false;
    }

    // =========================================================================
    // PHẦN 2: TÍNH NĂNG CŨ (GIỮ LẠI ĐỂ AUCTION SESSION KHÔNG BỊ LỖI)
    // =========================================================================
    private static final long DEFAULT_EXTENSION_MILLIS = 30_000;
    private static final long DEFAULT_SNIPE_THRESHOLD_MILLIS = 30_000;

    private final long extensionMillis;
    private final long snipeThresholdMillis;
    private long endTime;

    public AntiSniper(long durationMillis) {
        this(durationMillis, DEFAULT_SNIPE_THRESHOLD_MILLIS, DEFAULT_EXTENSION_MILLIS);
    }

    public AntiSniper(long durationMillis, long snipeThresholdMillis, long extensionMillis) {
        this.endTime = System.currentTimeMillis() + durationMillis;
        this.snipeThresholdMillis = snipeThresholdMillis;
        this.extensionMillis = extensionMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= endTime;
    }

    public long getRemainingMillis() {
        return Math.max(0, endTime - System.currentTimeMillis());
    }

    public boolean checkAndExtend() {
        if (getRemainingMillis() < snipeThresholdMillis) {
            endTime += extensionMillis;
            System.out.println("Phát hiện snipe! Phiên gia hạn thêm "
                    + (extensionMillis / 1000) + " giây.");
            return true;
        }
        return false;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getRemainingFormatted() {
        long remaining = getRemainingMillis();
        long minutes = remaining / 60_000;
        long seconds = (remaining % 60_000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }
}