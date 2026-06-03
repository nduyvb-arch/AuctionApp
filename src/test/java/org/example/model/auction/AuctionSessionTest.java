package org.example.model.auction;

import org.example.server.network.AuctionSession;
import org.example.exception.AuctionClosedException;
import org.example.exception.InvalidBidException;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSessionTest {

    private AuctionSession session;

    @BeforeEach
    void setUp() {
        session = new AuctionSession("Sản phẩm A", 100.0);
    }

    @Test
    void testValidBid() throws Exception {
        session.placeBid("Lương", 150.0);
        assertEquals(150.0, session.getCurrentPrice());
    }

    @Test
    void testInvalidBidException() {
        assertThrows(InvalidBidException.class, () -> {
            session.placeBid("User2", 50.0);
        });
    }

    @Test
    void testAuctionClosedException() {
        session.finishAuction();
        assertThrows(AuctionClosedException.class, () -> {
            session.placeBid("User3", 200.0);
        });
    }

    @Test
    void testInitialState() {
        assertEquals(100.0, session.getCurrentPrice());
    }

    @Test
    void testBidEqualToCurrentPrice() {
        assertThrows(InvalidBidException.class, () -> {
            session.placeBid("User4", 100.0);
        });
    }

    @Test
    void testMultipleValidBids() throws Exception {
        session.placeBid("UserA", 120.0);
        session.placeBid("UserB", 130.0);

        assertEquals(130.0, session.getCurrentPrice());
        assertEquals("UserB", session.getWinnerName());
    }

    @Nested
    @DisplayName("Anti Sniping Tests")
    class AntiSnipingTests {

        @Test
        @DisplayName("Phiên không có thời gian — getRemainingMillis trả về -1")
        void testNoTimerReturnsMinusOne() {
            assertEquals(-1, session.getRemainingMillis());
        }

        @Test
        @DisplayName("Phiên có thời gian — còn thời gian sau khi tạo")
        void testSessionHasRemainingTime() {
            AuctionSession timedSession = new AuctionSession("SP_B", 100.0, 60_000);
            assertTrue(timedSession.getRemainingMillis() > 0);
        }

        @Test
        @DisplayName("Phiên được gia hạn khi bid trong 30 giây cuối")
        void testExtensionOnLateBid() throws Exception {
            AuctionSession shortSession = new AuctionSession("SP_C", 100.0, 5_000);
            long remainingBefore = shortSession.getRemainingMillis();

            shortSession.placeBid("Sniper", 150.0);

            assertTrue(shortSession.getRemainingMillis() > remainingBefore);
        }

        @Test
        @DisplayName("Phiên tự động đóng sau khi hết giờ")
        void testSessionAutoCloses() throws Exception {
            AuctionSession shortSession = new AuctionSession("SP_D", 100.0, 2_000);

            Thread.sleep(3_000);

            assertThrows(AuctionClosedException.class, () -> {
                shortSession.placeBid("LateUser", 200.0);
            });
        }

        @Test
        @DisplayName("getRemainingFormatted trả về ∞ khi không có timer")
        void testFormattedRemainingNoTimer() {
            assertEquals("∞", session.getRemainingFormatted());
        }

        @Test
        @DisplayName("getRemainingFormatted trả về định dạng mm:ss")
        void testFormattedRemainingWithTimer() {
            AuctionSession timedSession = new AuctionSession("SP_E", 100.0, 90_000);
            String formatted = timedSession.getRemainingFormatted();

            assertTrue(formatted.matches("\\d{2}:\\d{2}"));
        }
    }
}