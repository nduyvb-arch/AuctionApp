package org.example.server.manager;

import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Electronic;
import org.example.common.model.item.Item;
import org.example.common.model.user.User;
import org.example.server.data.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuctionManagerTest {

    private MockedStatic<DatabaseManager> mockedDbManager;
    private MockedStatic<UserManager> mockedUserManager;
    private MockedStatic<AntiSniper> mockedAntiSniper;

    private UserManager mockUserMgrInstance;
    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Giả lập kết nối Database ban đầu khi khởi tạo Singleton
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // DB trống lúc khởi động

        mockedDbManager = mockStatic(DatabaseManager.class);
        mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);

        // 2. Giả lập UserManager Singleton
        mockUserMgrInstance = mock(UserManager.class);
        mockedUserManager = mockStatic(UserManager.class);
        mockedUserManager.when(UserManager::getInstance).thenReturn(mockUserMgrInstance);

        // 3. Giả lập AntiSniper mặc định không kích hoạt
        mockedAntiSniper = mockStatic(AntiSniper.class);
        mockedAntiSniper.when(() -> AntiSniper.applyAntiSniper(any(Item.class))).thenReturn(false);

        // 4. Làm sạch thực thể cũ của AuctionManager trước mỗi test case (Cô lập môi trường test)
        resetAuctionManagerSingleton();
        auctionManager = AuctionManager.getInstance();

        // 5. Ép dbExecutor chạy đồng bộ trên luồng chính để tránh chạy ngầm gây lỗi Mockito khi đóng kết nối
        injectDirectExecutor();
    }

    @AfterEach
    void tearDown() {
        // Giải phóng các hàm tĩnh (Static mock)
        mockedDbManager.close();
        mockedUserManager.close();
        mockedAntiSniper.close();
    }

    // Hàm phụ trợ xóa instance Singleton bằng kỹ thuật Reflection
    private void resetAuctionManagerSingleton() throws Exception {
        Field instanceField = AuctionManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    // Hàm phụ trợ đưa sản phẩm vượt qua cơ chế đóng gói (Bypass Sao chép ArrayList) vào thẳng Manager
    private void injectItemToManager(Item item) throws Exception {
        Field itemsField = AuctionManager.class.getDeclaredField("auctionItems");
        itemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Item> internalList = (List<Item>) itemsField.get(auctionManager);
        internalList.add(item);
    }

    // Biến đổi ExecutorService ngầm chạy đồng bộ (Synchronous) phục vụ Unit Test
    private void injectDirectExecutor() throws Exception {
        Field executorField = AuctionManager.class.getDeclaredField("dbExecutor");
        executorField.setAccessible(true);
        java.util.concurrent.ExecutorService directExecutor = new java.util.concurrent.AbstractExecutorService() {
            private boolean isShutdown = false;
            @Override public void shutdown() { isShutdown = true; }
            @Override public List<Runnable> shutdownNow() { isShutdown = true; return Collections.emptyList(); }
            @Override public boolean isShutdown() { return isShutdown; }
            @Override public boolean isTerminated() { return isShutdown; }
            @Override public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
            @Override public void execute(Runnable command) { command.run(); }
        };
        executorField.set(auctionManager, directExecutor);
    }

    // Tạo sản phẩm mẫu (Bắt buộc dùng ID số: "1", "10", "20" khớp với Integer.parseInt trong code của bạn)
    private Item createDummyItem(String id, String sellerId, AuctionStatus status) {
        Item item = new Electronic("Laptop Gaming", "electronic", "Core i7 RTX 4060", 1000.0, 100.0);
        item.setId(id);
        item.setSellerId(sellerId);
        item.setStatus(status);
        item.setCurrentPrice(1000.0);
        return item;
    }

    @Test
    @DisplayName("Kiểm tra thiết kế Singleton: getInstance() luôn trả về cùng một đối tượng")
    void testSingletonInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2, "Lỗi: Singleton phải trả về cùng một tham chiếu bộ nhớ");
    }

    @Test
    @DisplayName("Bắt đầu phiên đấu giá thành công với sản phẩm hợp lệ")
    void testStartAuction_Success() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.PENDING);
        injectItemToManager(item);

        String result = auctionManager.startAuction("1", 30);

        assertTrue(result.contains("Đã bắt đầu phiên đấu giá cho"));
        assertEquals(AuctionStatus.ACTIVE, item.getStatus());
        assertNotNull(item.getEndTime());
    }

    @Test
    @DisplayName("Bắt lỗi: Đặt giá cho sản phẩm chưa được kích hoạt")
    void testPlaceBid_ItemNotActive() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.PENDING);
        injectItemToManager(item);

        String result = auctionManager.placeBid("1", 1200.0, "20");

        assertTrue(result.contains("Phiên đấu giá chưa bắt đầu hoặc đã kết thúc"));
    }

    @Test
    @DisplayName("Bắt lỗi: Người bán tự đặt giá cho sản phẩm của chính mình")
    void testPlaceBid_SellerBidsOwnItem() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        String result = auctionManager.placeBid("1", 1500.0, "10");

        assertTrue(result.contains("Người bán không được đặt giá sản phẩm của chính mình"));
    }

    @Test
    @DisplayName("Bắt lỗi: Người mua không đủ số dư trong tài khoản khả dụng")
    void testPlaceBid_InsufficientBalance() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        User mockBidder = mock(User.class);
        when(mockBidder.getRole()).thenReturn("bidder");
        when(mockBidder.getBalance()).thenReturn(500.0); // Ví chỉ có 500 VNĐ nhưng định đấu giá tận 1200 VNĐ

        when(mockUserMgrInstance.findUserById("20")).thenReturn(mockBidder);

        String result = auctionManager.placeBid("1", 1200.0, "20");

        assertTrue(result.contains("không đủ để đặt mức giá này"));
    }

    @Test
    @DisplayName("Luồng chuẩn: Đặt giá thành công và kích hoạt tính năng Anti-Sniper nâng cao")
    void testPlaceBid_Success_WithAntiSniper() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        User mockBidder = mock(User.class);
        when(mockBidder.getRole()).thenReturn("bidder");
        when(mockBidder.getBalance()).thenReturn(5000.0);

        when(mockUserMgrInstance.findUserById("20")).thenReturn(mockBidder);
        when(mockUserMgrInstance.subtractBalance("20", 1200.0)).thenReturn(true);

        // Giả lập kích hoạt thành công tính năng nâng cao Anti-Sniper (+0.5 điểm BTL)
        mockedAntiSniper.when(() -> AntiSniper.applyAntiSniper(item)).thenReturn(true);

        String result = auctionManager.placeBid("1", 1200.0, "20");

        assertTrue(result.contains("Đã kích hoạt Anti-Sniper"));
        assertEquals("20", item.getCurrentWinnerId());
        assertEquals(1200.0, item.getCurrentPrice());
    }

    @Test
    @DisplayName("Kiểm tra tự động đóng phiên đấu giá khi hết hạn và kết chuyển tiền cho Seller")
    void testCheckAndCloseExpiredAuctions() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().minusMinutes(1)); // Đặt mốc thời gian hết hạn ở quá khứ
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(1500.0);
        injectItemToManager(item);

        var notifications = auctionManager.checkAndCloseExpiredAuctions();

        assertEquals(AuctionStatus.CLOSED, item.getStatus()); // Đảm bảo trạng thái chuyển sang CLOSED chuẩn xác
        assertFalse(notifications.isEmpty());
        assertTrue(notifications.get(0).contains("ĐẤU GIÁ KẾT THÚC"));

        // Kiểm tra xem hệ thống có tự động gọi hàm kết chuyển tiền thắng cuộc cho Người bán không
        verify(mockUserMgrInstance, times(1)).addBalance("10", 1500.0);
    }
}