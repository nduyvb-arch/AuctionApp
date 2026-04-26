package org.example.manager;

import org.example.model.item.AuctionStatus;
import org.example.model.item.Item;
import org.example.model.item.Art;
import org.example.model.item.Electronic;
import org.example.model.item.Vehicle;
import org.example.model.user.Bidder;
import org.example.model.user.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionManager {
    private static final String DB_URL = "jdbc:sqlite:auction.db";
    private static volatile AuctionManager instance;
    private final List<Item> auctionItems;
    private Connection connection;

    private AuctionManager() {
        auctionItems = new ArrayList<>();
        try {
            // Load H2 driver
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            loadItemsFromDB();
        } catch (SQLException e) {
            System.err.println("❌ Lỗi kết nối database trong AuctionManager: " + e.getMessage());
        }
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    /**
     * 1. TẠO BẢNG TRONG SQLITE
     */
    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS auction_items (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "type TEXT, " +
                "describe TEXT, " +
                "starting_price REAL, " +
                "bid_increment REAL, " +
                "current_price REAL, " +
                "current_winner_id TEXT, " +
                "status TEXT, " +
                "end_time TEXT)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 2. TẢI DỮ LIỆU TỪ DATABASE LÊN RAM
     */
    private void loadItemsFromDB() {
        String sql = "SELECT * FROM auction_items";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String id = rs.getString("id");
                String name = rs.getString("name");
                String type = rs.getString("type");
                String describe = rs.getString("describe");
                double startingPrice = rs.getDouble("starting_price");
                double bidIncrement = rs.getDouble("bid_increment");
                double currentPrice = rs.getDouble("current_price");
                String currentWinnerId = rs.getString("current_winner_id");
                String statusStr = rs.getString("status");
                String endTimeStr = rs.getString("end_time");

                // LƯU Ý: Vì Item là Abstract Class, bạn cần khởi tạo các lớp con cụ thể ở đây
                Item item;
                switch (type.toLowerCase()) {
                    case "art":
                        item = new Art(name, type, describe, startingPrice, bidIncrement);
                        break;
                    case "electronic":
                        item = new Electronic(name, type, describe, startingPrice, bidIncrement);
                        break;
                    case "vehicle":
                        item = new Vehicle(name, type, describe, startingPrice, bidIncrement);
                        break;
                    default:
                        item = new Art(name, type, describe, startingPrice, bidIncrement); // default to Art
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
            System.out.println("📂 Đã tải " + auctionItems.size() + " vật phẩm từ database");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi tải item: " + e.getMessage());
        }
    }

    /**
     * 3. THÊM SẢN PHẨM MỚI VÀO CẢ RAM VÀ DB
     */
    public synchronized void addItem(Item item) {
        auctionItems.add(item);

        String sql = "INSERT INTO auction_items (id, name, type, describe, starting_price, bid_increment, current_price, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            // Lưu ý: Nếu đổi tên biến bên Item, hàm lấy ID phải là getItemId()
            // Mình dùng getId() ở đây để giữ nguyên cấu trúc gốc của bạn, hãy đảm bảo hàm getter bên Item gọi đúng.
            pstmt.setString(1, item.getId());
            pstmt.setString(2, item.getItemName());
            pstmt.setString(3, item.getType());
            pstmt.setString(4, item.getDescribe());
            pstmt.setDouble(5, item.getStartingPrice());
            pstmt.setDouble(6, item.getBidIncrement());
            pstmt.setDouble(7, item.getCurrentPrice());
            pstmt.setString(8, item.getStatus().name());
            pstmt.executeUpdate();
            System.out.println("✅ Đã thêm sản phẩm " + item.getItemName() + " vào DB.");
        } catch (SQLException e) {
            System.err.println("❌ Lỗi lưu item vào DB: " + e.getMessage());
        }
    }

    public List<Item> getAllItems() {
        return this.auctionItems;
    }

    /**
     * 4. CẬP NHẬT DATABASE KHI CÓ THAY ĐỔI TRẠNG THÁI HOẶC GIÁ
     */
    private void updateItemDB(Item item) {
        String sql = "UPDATE auction_items SET current_price = ?, current_winner_id = ?, status = ?, end_time = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, item.getCurrentPrice());
            pstmt.setString(2, item.getCurrentWinnerId());
            pstmt.setString(3, item.getStatus().name());
            pstmt.setString(4, item.getEndTime() != null ? item.getEndTime().toString() : null);
            pstmt.setString(5, item.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Lỗi cập nhật item vào DB: " + e.getMessage());
        }
    }

    /**
     * 5. MỞ PHIÊN ĐẤU GIÁ
     */
    public synchronized String startAuction(String itemId, int durationInMinutes) {
        Item targetItem = auctionItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst().orElse(null);

        if (targetItem == null) {
            return "❌ Lỗi: Sản phẩm không tồn tại.";
        }
        if (targetItem.getStatus() != AuctionStatus.PENDING) {
            return "❌ Lỗi: Sản phẩm đang mở ở phiên khác hoặc đã đóng.";
        }

        targetItem.setStatus(AuctionStatus.ACTIVE);
        targetItem.setEndTime(LocalDateTime.now().plusMinutes(durationInMinutes));

        // Cập nhật lên Database
        updateItemDB(targetItem);

        return "✅ Đã bắt đầu phiên đấu giá cho: " + targetItem.getItemName() + ". Thời gian: " + durationInMinutes + " phút.";
    }

    /**
     * 6. ĐẶT GIÁ
     */
    public synchronized String placeBid(String itemId, double bidAmount, String bidderId) {
        Item targetItem = auctionItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst().orElse(null);

        if (targetItem == null) {
            return "❌ Lỗi: Sản phẩm cần tìm không tồn tại.";
        }
        if (targetItem.getStatus() != AuctionStatus.ACTIVE) {
            return "❌ Lỗi: Phiên đấu giá chưa bắt đầu hoặc đã kết thúc.";
        }

        if (targetItem.getEndTime() != null && LocalDateTime.now().isAfter(targetItem.getEndTime())) {
            // SỬA Ở ĐÂY: Đổi COMPLETED thành ENDED (hoặc trạng thái tương ứng có sẵn trong file AuctionStatus của bạn).
            targetItem.setStatus(AuctionStatus.CLOSED);
            updateItemDB(targetItem);
            return "❌ Lỗi: Phiên đấu giá đã kết thúc.";
        }

        // TÌM NGƯỜI DÙNG & KIỂM TRA SỐ DƯ (BALANCE)
        User bidder = UserManager.getInstance().getAllUsers().stream()
                .filter(u -> u.getId().equals(bidderId))
                .findFirst().orElse(null);

        if (bidder == null || !(bidder instanceof Bidder)) {
            return "❌ Lỗi: Không tìm thấy tài khoản người đấu giá hợp lệ.";
        }

        double userBalance = ((Bidder) bidder).getBalance();
        if (userBalance < bidAmount) {
            return "❌ Lỗi: Số dư của bạn (" + userBalance + ") không đủ để đặt mức giá này.";
        }

        // Kiểm tra xem đã có ai đặt giá chưa
        double minRequiredBid;
        if (targetItem.getCurrentWinnerId() == null || targetItem.getCurrentWinnerId().isEmpty()) {
            minRequiredBid = targetItem.getStartingPrice(); // Lần đầu tiên, được đặt bằng giá khởi điểm
        } else {
            minRequiredBid = targetItem.getCurrentPrice() + targetItem.getBidIncrement(); // Các lần sau phải cộng bước giá
        }

        if (bidAmount < minRequiredBid) {
            return "❌ Lỗi: Giá thấp nhất có thể đặt hiện tại là: " + minRequiredBid;
        }

        // Cập nhật phiên đấu giá nếu hợp lệ
        targetItem.setCurrentWinnerId(bidderId);
        targetItem.setCurrentPrice(bidAmount);

        // Lưu vào DB
        updateItemDB(targetItem);

        return "✅ Đặt giá thành công! Bạn đang dẫn đầu với mức giá " + bidAmount + " cho sản phẩm " + targetItem.getItemName();
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
}