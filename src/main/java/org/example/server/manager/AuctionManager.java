package org.example.server.manager;

import org.example.common.model.item.Art;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Electronic;
import org.example.common.model.item.Item;
import org.example.common.model.item.Vehicle;
import org.example.common.model.user.Bidder;
import org.example.common.model.user.User;
import org.example.server.data.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Bản sửa:
 * - getAllItems() tự kiểm tra sản phẩm hết hạn và đổi sang CLOSED.
 * - startAuction() đổi status sang ACTIVE, set endTime, cập nhật DB.
 * - placeBid() vẫn kiểm tra hết hạn trước khi cho đặt giá.
 */
public class AuctionManager {

    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;

    private final List<Item> auctionItems;
    private static final DateTimeFormatter DB_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private AuctionManager() {
        auctionItems = new ArrayList<>();
        loadItemsFromDB();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }

        return instance;
    }

    private void loadItemsFromDB() {
        String sql = "SELECT * FROM items";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql);
                ResultSet rs = pstmt.executeQuery()
        ) {
            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("start_price");
                double bidIncrement = rs.getDouble("bid_increment");
                double currentPrice = rs.getDouble("current_price");
                String currentWinnerId = rs.getString("current_winner_id");
                String sellerId = rs.getString("seller_id");
                String statusStr = rs.getString("status");
                String endTimeStr = rs.getString("end_time");

                Item item = createItemByType(type, name, description, startingPrice, bidIncrement);

                item.setId(id);
                item.setCurrentPrice(currentPrice);
                item.setCurrentWinnerId(currentWinnerId);
                item.setSellerId(sellerId);
                item.setStatus(AuctionStatus.valueOf(statusStr));

                if (endTimeStr != null && !endTimeStr.isEmpty()) {
                    item.setEndTime(LocalDateTime.parse(endTimeStr, DB_TIME_FORMAT));
                }

                auctionItems.add(item);
            }

            checkAndCloseExpiredAuctions();

            logger.info("Đã tải {} vật phẩm từ database", auctionItems.size());

        } catch (SQLException e) {
            logger.error("Lỗi khi tải item: {}", e.getMessage(), e);
        }
    }

    public synchronized void addItem(Item item) {
        String sql = "INSERT INTO items (name, description, type, start_price, bid_increment, current_price, status, seller_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            pstmt.setString(1, item.getItemName());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getType());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getBidIncrement());
            pstmt.setDouble(6, item.getCurrentPrice());
            pstmt.setString(7, item.getStatus().name());

            if (item.getSellerId() != null && !item.getSellerId().isEmpty()) {
                pstmt.setInt(8, Integer.parseInt(item.getSellerId()));
            } else {
                pstmt.setNull(8, java.sql.Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(String.valueOf(generatedKeys.getLong(1)));
                        auctionItems.add(item);
                        logger.info("Đã thêm sản phẩm {} vào DB với ID: {}", item.getItemName(), item.getId());
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Lỗi lưu item vào DB: {}", e.getMessage(), e);
        }
    }

    public synchronized List<Item> getAllItems() {
        checkAndCloseExpiredAuctions();
        return new ArrayList<>(auctionItems);
    }

    private void updateItemDB(Item item) {
        String sql = "UPDATE items SET current_price = ?, current_winner_id = ?, status = ?, end_time = ? WHERE id = ?";

        try (
                Connection conn = DatabaseManager.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {
            pstmt.setDouble(1, item.getCurrentPrice());

            if (item.getCurrentWinnerId() != null && !item.getCurrentWinnerId().isEmpty()) {
                pstmt.setInt(2, Integer.parseInt(item.getCurrentWinnerId()));
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }

            pstmt.setString(3, item.getStatus().name());
            pstmt.setString(4, item.getEndTime() != null ? item.getEndTime().format(DB_TIME_FORMAT) : null);
            pstmt.setInt(5, Integer.parseInt(item.getId()));
            pstmt.executeUpdate();

        } catch (SQLException e) {
            logger.error("Lỗi cập nhật item vào DB: {}", e.getMessage(), e);
        }
    }

    public synchronized String startAuction(String itemId, int durationInMinutes) {
        Item targetItem = findItemById(itemId);

        if (targetItem == null) {
            return "Lỗi: Sản phẩm không tồn tại.";
        }

        if (targetItem.getStatus() != AuctionStatus.PENDING) {
            return "Lỗi: Sản phẩm đang mở ở phiên khác hoặc đã đóng.";
        }

        targetItem.setStatus(AuctionStatus.ACTIVE);
        targetItem.setEndTime(LocalDateTime.now().plusMinutes(durationInMinutes));
        updateItemDB(targetItem);

        return "Đã bắt đầu phiên đấu giá cho: " + targetItem.getItemName() + ".\nThời gian: " + durationInMinutes + " phút.";
    }

    public synchronized String placeBid(String itemId, double bidAmount, String bidderId) {
        Item targetItem = findItemById(itemId);

        if (targetItem == null) {
            return "Lỗi: Sản phẩm cần tìm không tồn tại.";
        }

        if (targetItem.getStatus() != AuctionStatus.ACTIVE) {
            return "Lỗi: Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.";
        }

        if (targetItem.getEndTime() != null && LocalDateTime.now().isAfter(targetItem.getEndTime())) {
            targetItem.setStatus(AuctionStatus.CLOSED);
            updateItemDB(targetItem);
            return "Lỗi: Phiên đấu giá đã kết thúc.";
        }

        User bidder = UserManager.getInstance().findUserById(bidderId);

        if (bidder == null || !(bidder instanceof Bidder)) {
            return "Lỗi: Không tìm thấy tài khoản người đấu giá hợp lệ.";
        }

        double userBalance = ((Bidder) bidder).getBalance();

        if (userBalance < bidAmount) {
            return "Lỗi: Số dư của bạn (" + userBalance + ") không đủ để đặt mức giá này.";
        }

        double minRequiredBid = targetItem.getCurrentWinnerId() == null || targetItem.getCurrentWinnerId().isEmpty()
                ? targetItem.getStartingPrice()
                : targetItem.getCurrentPrice() + targetItem.getBidIncrement();

        if (bidAmount < minRequiredBid) {
            return "Lỗi: Giá thấp nhất có thể đặt hiện tại là: " + minRequiredBid;
        }

        targetItem.setCurrentWinnerId(bidderId);
        targetItem.setCurrentPrice(bidAmount);
        updateItemDB(targetItem);

        return "Đặt giá thành công! Bạn đang dẫn đầu với mức giá " + bidAmount + " cho sản phẩm " + targetItem.getItemName();
    }

    public synchronized List<String> checkAndCloseExpiredAuctions() {
        List<String> notifications = new ArrayList<>();

        for (Item item : auctionItems) {
            if (item.getStatus() == AuctionStatus.ACTIVE
                    && item.getEndTime() != null
                    && LocalDateTime.now().isAfter(item.getEndTime())) {

                item.setStatus(AuctionStatus.CLOSED);
                updateItemDB(item);

                String msg = item.getCurrentWinnerId() != null
                        ? "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] đã thuộc về " + item.getCurrentWinnerId() + " với giá " + item.getCurrentPrice()
                        : "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] không có ai đặt giá";

                notifications.add(msg);
            }
        }

        return notifications;
    }

    public synchronized String cancelAuctionByAdmin(String itemId) {
        Item targetItem = findItemById(itemId);

        if (targetItem == null) {
            return "Không tìm thấy sản phẩm này!";
        }

        if (targetItem.getStatus() != AuctionStatus.ACTIVE) {
            return "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!";
        }

        targetItem.setEndTime(LocalDateTime.now().minusSeconds(1));
        targetItem.setStatus(AuctionStatus.CLOSED);
        updateItemDB(targetItem);

        return "success";
    }

    private Item findItemById(String itemId) {
        return auctionItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }

    private Item createItemByType(String type, String name, String description, double startingPrice, double bidIncrement) {
        if (type == null) {
            return new Electronic(name, "Electronic", description, startingPrice, bidIncrement);
        }

        switch (type.toLowerCase()) {
            case "art":
                return new Art(name, type, description, startingPrice, bidIncrement);

            case "vehicle":
                return new Vehicle(name, type, description, startingPrice, bidIncrement);

            case "electronic":
            default:
                return new Electronic(name, type, description, startingPrice, bidIncrement);
        }
    }
}
