package org.example.data;

import org.example.model.item.Item;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemDAO {

    /**
     * Hàm cập nhật Giá và Người chiến thắng vào Database
     */
    public static void updateBid(Item item) {
        String sql = "UPDATE items SET current_price = ?, current_winner_username = ? WHERE id = ?";

        try {
            // 1. Gọi DatabaseManager "chính chủ" của nhóm em
            Connection conn = DatabaseManager.getConnection();

            // 2. Chỉ dùng try-with-resources cho PreparedStatement để tránh đóng nhầm Connection
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setDouble(1, item.getCurrentPrice());
                pstmt.setString(2, item.getCurrentWinnerId());
                pstmt.setString(3, item.getId());

                pstmt.executeUpdate();
                System.out.println("💾 [Database] Đã cập nhật giá mới cho sản phẩm: " + item.getId());
            }
        } catch (SQLException e) {
            System.out.println("[Database] Lỗi khi cập nhật giá: " + e.getMessage());
        }
    }

    /**
     * Hàm cập nhật trạng thái khi phiên đấu giá kết thúc
     */
    public static void updateStatus(Item item) {
        String sql = "UPDATE items SET status = ? WHERE id = ?";

        try {
            Connection conn = DatabaseManager.getConnection();

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, item.getStatus().name());
                pstmt.setString(2, item.getId());

                pstmt.executeUpdate();
                System.out.println("[Database] Đã cập nhật trạng thái " + item.getStatus() + " cho: " + item.getId());
            }
        } catch (SQLException e) {
            System.out.println("[Database] Lỗi khi cập nhật trạng thái: " + e.getMessage());
        }
    }
}