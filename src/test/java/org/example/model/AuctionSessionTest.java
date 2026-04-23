package org.example.model;

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
}