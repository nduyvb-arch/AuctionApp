package org.example.model;

import org.example.server.network.AuctionSession;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuctionSessionTest {
    private AuctionSession session;

    @BeforeEach
    void setUp() {
        // Khởi tạo phiên đấu giá mới trước mỗi bài test
        session = new AuctionSession("Sản phẩm A", 100.0);
    }

    @Test
    void testValidBid() throws Exception {
        // Kiểm tra đặt giá hợp lệ
        session.placeBid("Lương", 150.0);
        assertEquals(150.0, session.getCurrentPrice());
    }

    @Test
    void testInvalidBidException() {
        // Kiểm tra xem có ném ra InvalidBidException khi giá thấp hơn không
        assertThrows(InvalidBidException.class, () -> {
            session.placeBid("User2", 50.0);
        });
    }

    @Test
    void testAuctionClosedException() {
        // Kết thúc phiên và kiểm tra lỗi khi cố đặt giá
        session.finishAuction();
        assertThrows(AuctionClosedException.class, () -> {
            session.placeBid("User3", 200.0);
        });
    }
    @Test
    void testInitialState() {
        // Đảm bảo thông tin ban đầu chính xác
        assertEquals(100.0, session.getCurrentPrice());
    }

    @Test
    void testBidEqualToCurrentPrice() {
        // Kiểm tra đặt giá bằng đúng giá hiện tại (phải ném ngoại lệ)
        assertThrows(InvalidBidException.class, () -> {
            session.placeBid("User4", 100.0);
        });
    }

    @Test
    void testMultipleValidBids() throws Exception {
        session.placeBid("UserA", 120.0);
        session.placeBid("UserB", 130.0);

        assertEquals(130.0, session.getCurrentPrice());
        // Giả sử bạn có method getWinner() để kiểm tra người thắng
        // assertEquals("UserB", session.getWinner());
    }
}