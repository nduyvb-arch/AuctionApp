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

    // ========== TEST CŨ GIỮ NGUYÊN ==========

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
        assertEquals("UserB", session.getWinnerName()); // bỏ comment vì đã có getWinnerName()
    }

    // ========== TEST AUTO BID ==========

    @Nested
    @DisplayName("Auto Bid Tests")
    class AutoBidTests {

        @Test
        @DisplayName("Auto bid tự động đặt giá khi bị vượt")
        void testAutoBidTriggered() throws Exception {
            // Auto_User đăng ký max=200, mỗi lần tăng 10
            session.registerAutoBid("Auto_User", 200.0, 10.0);

            // ManualUser đặt 150 → Auto_User tự động đặt 160
            session.placeBid("ManualUser", 150.0);

            assertEquals(160.0, session.getCurrentPrice());
            assertEquals("Auto_User", session.getWinnerName());
        }

        @Test
        @DisplayName("Auto bid không kích hoạt khi chính mình đang dẫn đầu")
        void testAutoBidSkipsWhenLeading() throws Exception {
            session.registerAutoBid("Auto_User", 200.0, 10.0);

            // Auto_User tự đặt trước
            session.placeBid("Auto_User", 150.0);

            // Giá không tự tăng thêm vì Auto_User đang dẫn đầu
            assertEquals(150.0, session.getCurrentPrice());
            assertEquals("Auto_User", session.getWinnerName());
        }

        @Test
        @DisplayName("Auto bid dừng khi vượt ngưỡng maxBid")
        void testAutoBidStopsAtMax() throws Exception {
            // Auto_User chỉ chấp nhận tối đa 120
            session.registerAutoBid("Auto_User", 120.0, 10.0);

            // ManualUser đặt 115 → Auto muốn đặt 125 nhưng vượt max=120 → dừng
            session.placeBid("ManualUser", 115.0);

            assertEquals(115.0, session.getCurrentPrice());
            assertEquals("ManualUser", session.getWinnerName());
        }

        @Test
        @DisplayName("Nhiều auto bid cạnh tranh nhau — người có maxBid cao hơn thắng")
        void testMultipleAutoBids() throws Exception {
            session.registerAutoBid("AutoA", 150.0, 10.0); // maxBid thấp hơn
            session.registerAutoBid("AutoB", 200.0, 10.0); // maxBid cao hơn

            session.placeBid("ManualUser", 120.0);

            // AutoB có maxBid cao hơn nên sẽ thắng
            assertEquals("AutoB", session.getWinnerName());
        }

        @Test
        @DisplayName("Đăng ký lại auto bid ghi đè cấu hình cũ")
        void testReRegisterAutoBid() throws Exception {
            session.registerAutoBid("Auto_User", 130.0, 5.0);
            // Đăng ký lại với maxBid cao hơn
            session.registerAutoBid("Auto_User", 200.0, 10.0);

            session.placeBid("ManualUser", 150.0);

            // Dùng cấu hình mới: increment=10 → đặt 160
            assertEquals(160.0, session.getCurrentPrice());
            assertEquals("Auto_User", session.getWinnerName());
        }
    }

    // ========== TEST ANTI SNIPING ==========

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
            // Tạo phiên chỉ 5 giây (< ngưỡng 30s → luôn bị coi là snipe)
            AuctionSession shortSession = new AuctionSession("SP_C", 100.0, 5_000);
            long remainingBefore = shortSession.getRemainingMillis();

            shortSession.placeBid("Sniper", 150.0);

            // Sau khi bid → thời gian còn lại phải tăng lên
            assertTrue(shortSession.getRemainingMillis() > remainingBefore);
        }

        @Test
        @DisplayName("Phiên tự động đóng sau khi hết giờ")
        void testSessionAutoCloses() throws Exception {
            // Tạo phiên 2 giây
            AuctionSession shortSession = new AuctionSession("SP_D", 100.0, 2_000);

            // Chờ phiên hết giờ
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
            AuctionSession timedSession = new AuctionSession("SP_E", 100.0, 90_000); // 1 phút 30 giây
            String formatted = timedSession.getRemainingFormatted();

            // Phải có định dạng "mm:ss" — đúng pattern 2 số : 2 số
            assertTrue(formatted.matches("\\d{2}:\\d{2}"));
        }
    }
}