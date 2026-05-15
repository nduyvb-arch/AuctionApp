package org.example.server.manager;

import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Item;
import org.example.common.model.item.Art;
import org.example.common.model.item.Electronic;
import org.example.common.model.item.Vehicle;
import org.example.common.model.user.Bidder;
import org.example.common.model.user.User;
import org.example.server.data.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.example.server.network.AuctionServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuctionManager {
    private static final Logger logger = LoggerFactory.getLogger(AuctionManager.class);
    private static volatile AuctionManager instance;
    private final List<Item> auctionItems;
    private Connection connection;

    private AuctionManager() {
        auctionItems = new ArrayList<>();
        try {
            connection = DatabaseManager.getConnection();
            loadItemsFromDB();
        } catch (Exception e) {
            logger.error("Lỗi kết nối database: {}", e.getMessage(),e);
        }
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }
    
    private void loadItemsFromDB() {
        String sql = "SELECT * FROM items";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                String description = rs.getString("description");
                double startingPrice = rs.getDouble("start_price");
                double bidIncrement = rs.getDouble("bid_increment");
                double currentPrice = rs.getDouble("current_price");
                String currentWinnerId = rs.getString("current_winner_id");
                String statusStr = rs.getString("status");
                String endTimeStr = rs.getString("end_time");


                Item item;
                switch (type.toLowerCase()) {
                    case "art":
                        item = new Art(name, type, description, startingPrice, bidIncrement);
                        break;
                    case "electronic":
                        item = new Electronic(name, type, description, startingPrice, bidIncrement);
                        break;
                    case "vehicle":
                        item = new Vehicle(name, type, description, startingPrice, bidIncrement);
                        break;
                    default:
                        item = new Art(name, type, description, startingPrice, bidIncrement); // default to Art
                }

                // Khôi phục trạng thái
                item.setId(id);
                item.setCurrentPrice(currentPrice);
                item.setCurrentWinnerId(currentWinnerId);
                item.setStatus(AuctionStatus.valueOf(statusStr));
                if (endTimeStr != null && !endTimeStr.isEmpty()) {
                    item.setEndTime(LocalDateTime.parse(endTimeStr));
                }

                auctionItems.add(item);
            }
            logger.info("Đã tải {}", auctionItems.size() + " vật phẩm từ database");
        } catch (SQLException e) {
            logger.error("Lỗi khi tải item: {}", e.getMessage(), e);
        }
    }


    public synchronized void addItem(Item item) { // Item này chưa có ID từ DB

        String sql = "INSERT INTO items (name, description, type, start_price, bid_increment, current_price, status, seller_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, item.getItemName());
            pstmt.setString(2, item.getDescription());
            pstmt.setString(3, item.getType());
            pstmt.setDouble(4, item.getStartingPrice());
            pstmt.setDouble(5, item.getBidIncrement());
            pstmt.setDouble(6, item.getCurrentPrice()); // Ban đầu giá hiện tại bằng giá khởi điểm
            pstmt.setString(7, item.getStatus().name());

            // Cần có sellerId trong model Item của bạn, ví dụ: item.getSellerId()
            // Tạm thời để là null nếu model chưa có
            // if (item.getSellerId() != null) {
            //     pstmt.setInt(8, Integer.parseInt(item.getSellerId()));
            // } else {
                 pstmt.setNull(8, java.sql.Types.INTEGER);
            // }

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(String.valueOf(generatedKeys.getLong(1))); // Cập nhật ID cho đối tượng
                        auctionItems.add(item); // Chỉ thêm vào RAM sau khi đã có ID từ DB
                        logger.info("Đã thêm sản phẩm {} vào DB với ID: {}", item.getItemName(), item.getId());
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Lỗi lưu item vào DB: {}", e.getMessage(), e);
        }
    }

    public List<Item> getAllItems() {
        return this.auctionItems;
    }

    private void updateItemDB(Item item) {
        // Sửa lại câu SQL cho khớp với bảng `items`
        String sql = "UPDATE items SET current_price = ?, current_winner_id = ?, status = ?, end_time = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, item.getCurrentPrice());

            if (item.getCurrentWinnerId() != null && !item.getCurrentWinnerId().isEmpty()) {
                pstmt.setInt(2, Integer.parseInt(item.getCurrentWinnerId()));
            } else {
                pstmt.setNull(2, java.sql.Types.INTEGER);
            }

            pstmt.setString(3, item.getStatus().name());
            pstmt.setString(4, item.getEndTime() != null ? item.getEndTime().toString() : null);
            pstmt.setInt(5, Integer.parseInt(item.getId()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật item vào DB: {}", e.getMessage(), e);
        }
    }

    public synchronized String startAuction(String itemId, int durationInMinutes) {
        Item targetItem = auctionItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst().orElse(null);

        if (targetItem == null) {
            return "Lỗi: Sản phẩm không tồn tại.";
        }
        if (targetItem.getStatus() != AuctionStatus.PENDING) {
            return "Lỗi: Sản phẩm đang mở ở phiên khác hoặc đã đóng.";
        }

        targetItem.setStatus(AuctionStatus.ACTIVE);
        targetItem.setEndTime(LocalDateTime.now().plusMinutes(durationInMinutes));

        updateItemDB(targetItem);

        return "Đã bắt đầu phiên đấu giá cho: " + targetItem.getItemName() + ". Thời gian: " + durationInMinutes + " phút.";
    }

    public synchronized String placeBid(String itemId, double bidAmount, String bidderId) {
        Item targetItem = auctionItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst().orElse(null);

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

        // TÌM NGƯỜI DÙNG & KIỂM TRA SỐ DƯ (BALANCE)
        User bidder = UserManager.getInstance().getAllUsers().stream()
                .filter(u -> u.getId().equals(bidderId))
                .findFirst().orElse(null);

        if (bidder == null || !(bidder instanceof Bidder)) {
            return "Lỗi: Không tìm thấy tài khoản người đấu giá hợp lệ.";
        }

        double userBalance = ((Bidder) bidder).getBalance();
        if (userBalance < bidAmount) {
            return "Lỗi: Số dư của bạn (" + userBalance + ") không đủ để đặt mức giá này.";
        }

        // Kiểm tra xem đã có ai đặt giá chưa
        double minRequiredBid;
        if (targetItem.getCurrentWinnerId() == null || targetItem.getCurrentWinnerId().isEmpty()) {
            minRequiredBid = targetItem.getStartingPrice();
        } else {
            minRequiredBid = targetItem.getCurrentPrice() + targetItem.getBidIncrement();
        }

        if (bidAmount < minRequiredBid) {
            return "Lỗi: Giá thấp nhất có thể đặt hiện tại là: " + minRequiredBid;
        }

        // Cập nhật phiên đấu giá nếu hợp lệ
        targetItem.setCurrentWinnerId(bidderId);
        targetItem.setCurrentPrice(bidAmount);

        // Lưu vào DB
        updateItemDB(targetItem);

        return "Đặt giá thành công! Bạn đang dẫn đầu với mức giá " + bidAmount + " cho sản phẩm " + targetItem.getItemName();
    }

    public synchronized List<String> checkAndCloseExpiredAuctions() {
        List<String> notifications = new ArrayList<>();

        for (Item item : auctionItems) {
            if (item.getStatus() == AuctionStatus.ACTIVE && item.getEndTime() != null && LocalDateTime.now().isAfter(item.getEndTime())) {
                item.setStatus(AuctionStatus.CLOSED);
                updateItemDB(item);

                String msg;

                if (item.getCurrentWinnerId() != null) {
                    msg = "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] đã thuộc về " + item.getCurrentWinnerId() + " với giá " + item.getCurrentPrice();
                } else {
                    msg = "ĐẤU GIÁ KẾT THÚC: Sản phẩm [" + item.getItemName() + "] không có ai đặt giá";
                }
                notifications.add(msg);
            }
        }
        return notifications;
    }

    public synchronized String cancelAuctionByAdmin(String itemId) {
        Item targetItem = getAllItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElse(null);

        if (targetItem == null) {
            return "Không tìm thấy sản phẩm này!";
        }

        // Xác nhận là phiên đấu giá đang diễn ra
        if (targetItem.getStatus() != AuctionStatus.ACTIVE) {
            return "Phiên đấu giá chưa bắt đầu hoặc đã kết thúc!";
        }

        // Cưỡng chế kết thúc (trừ đi 1 giây) và đổi trạng thái
        targetItem.setEndTime(LocalDateTime.now().minusSeconds(1));
        targetItem.setStatus(AuctionStatus.CLOSED);

        // Gọi hàm updateItemDB có sẵn để cập nhật xuống Database (Code cực gọn!)
        updateItemDB(targetItem);

        return "success";
    }
}