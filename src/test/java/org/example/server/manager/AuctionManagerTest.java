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
        // 1. Giả lập kết nối Database mạnh mẽ hơn để bao phủ UPDATE/DELETE/INSERT
        Connection mockConnection = mock(Connection.class);
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);
        ResultSet mockResultSet = mock(ResultSet.class);
        ResultSet mockGeneratedKeys = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);

        // Giả lập Query trả về rỗng (lúc khởi động Manager)
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Giả lập ExecuteUpdate thành công (cho UPDATE, DELETE)
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        // Giả lập tạo khóa tự động (cho INSERT)
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeys);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(99L); // ID ảo sinh ra

        mockedDbManager = mockStatic(DatabaseManager.class);
        mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);

        // 2. Giả lập UserManager Singleton
        mockUserMgrInstance = mock(UserManager.class);
        mockedUserManager = mockStatic(UserManager.class);
        mockedUserManager.when(UserManager::getInstance).thenReturn(mockUserMgrInstance);

        // 3. Giả lập AntiSniper mặc định không kích hoạt
        mockedAntiSniper = mockStatic(AntiSniper.class);
        mockedAntiSniper.when(() -> AntiSniper.applyAntiSniper(any(Item.class))).thenReturn(false);

        // 4. Làm sạch thực thể cũ của AuctionManager
        resetAuctionManagerSingleton();
        auctionManager = AuctionManager.getInstance();

        // 5. Ép dbExecutor chạy đồng bộ
        injectDirectExecutor();
    }

    @AfterEach
    void tearDown() {
        mockedDbManager.close();
        mockedUserManager.close();
        mockedAntiSniper.close();
    }

    private void resetAuctionManagerSingleton() throws Exception {
        Field instanceField = AuctionManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void injectItemToManager(Item item) throws Exception {
        Field itemsField = AuctionManager.class.getDeclaredField("auctionItems");
        itemsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Item> internalList = (List<Item>) itemsField.get(auctionManager);
        internalList.add(item);
    }

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

    private Item createDummyItem(String id, String sellerId, AuctionStatus status) {
        Item item = new Electronic("Laptop Gaming", "electronic", "Core i7", 1000.0, 100.0);
        item.setId(id);
        item.setSellerId(sellerId);
        item.setStatus(status);
        item.setCurrentPrice(1000.0);
        return item;
    }

    // ================== CÁC TEST CASE CŨ ==================

    @Test
    @DisplayName("Kiểm tra thiết kế Singleton")
    void testSingletonInstance() {
        AuctionManager instance1 = AuctionManager.getInstance();
        AuctionManager instance2 = AuctionManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    @DisplayName("Bắt đầu phiên đấu giá thành công")
    void testStartAuction_Success() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.PENDING);
        injectItemToManager(item);

        String result = auctionManager.startAuction("1", 30);
        assertTrue(result.contains("Đã bắt đầu phiên đấu giá"));
        assertEquals(AuctionStatus.ACTIVE, item.getStatus());
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
        assertTrue(result.contains("Người bán không được đặt giá"));
    }

    @Test
    @DisplayName("Bắt lỗi: Người mua không đủ số dư")
    void testPlaceBid_InsufficientBalance() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        User mockBidder = mock(User.class);
        when(mockBidder.getRole()).thenReturn("bidder");
        when(mockBidder.getBalance()).thenReturn(500.0);
        when(mockUserMgrInstance.findUserById("20")).thenReturn(mockBidder);

        String result = auctionManager.placeBid("1", 1200.0, "20");
        assertTrue(result.contains("không đủ để đặt mức giá này"));
    }

    @Test
    @DisplayName("Luồng chuẩn: Đặt giá thành công và kích hoạt Anti-Sniper")
    void testPlaceBid_Success_WithAntiSniper() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        User mockBidder = mock(User.class);
        when(mockBidder.getRole()).thenReturn("bidder");
        when(mockBidder.getBalance()).thenReturn(5000.0);

        when(mockUserMgrInstance.findUserById("20")).thenReturn(mockBidder);
        when(mockUserMgrInstance.subtractBalance("20", 1200.0)).thenReturn(true);
        mockedAntiSniper.when(() -> AntiSniper.applyAntiSniper(item)).thenReturn(true);

        String result = auctionManager.placeBid("1", 1200.0, "20");

        assertTrue(result.contains("Đã kích hoạt Anti-Sniper"));
        assertEquals("20", item.getCurrentWinnerId());
    }

    @Test
    @DisplayName("Tự động đóng phiên và kết chuyển tiền cho Seller")
    void testCheckAndCloseExpiredAuctions() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().minusMinutes(1));
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(1500.0);
        injectItemToManager(item);

        var notifications = auctionManager.checkAndCloseExpiredAuctions();
        assertEquals(AuctionStatus.CLOSED, item.getStatus());
        verify(mockUserMgrInstance, times(1)).addBalance("10", 1500.0);
    }

    // ================== CÁC TEST CASE MỚI (TĂNG COVERAGE) ==================

    @Test
    @DisplayName("Lấy toàn bộ sản phẩm")
    void testGetAllItems() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.PENDING);
        injectItemToManager(item);

        List<Item> items = auctionManager.getAllItems();
        assertEquals(1, items.size());
        assertEquals("1", items.get(0).getId());
    }

    @Test
    @DisplayName("Bắt lỗi Start Auction: Sản phẩm không tồn tại")
    void testStartAuction_ItemNotFound() {
        String result = auctionManager.startAuction("999", 30);
        assertTrue(result.contains("Sản phẩm không tồn tại"));
    }

    @Test
    @DisplayName("Bắt lỗi Start Auction: Sản phẩm đã kích hoạt hoặc đóng")
    void testStartAuction_ItemNotPending() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE); // Không phải PENDING
        injectItemToManager(item);

        String result = auctionManager.startAuction("1", 30);
        assertTrue(result.contains("đang mở ở phiên khác hoặc đã đóng"));
    }

    @Test
    @DisplayName("Bắt lỗi Place Bid: Người dùng không tồn tại")
    void testPlaceBid_BidderNotFound() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        when(mockUserMgrInstance.findUserById("999")).thenReturn(null);

        String result = auctionManager.placeBid("1", 1200.0, "999");
        assertTrue(result.contains("Không tìm thấy tài khoản người đấu giá hợp lệ"));
    }

    @Test
    @DisplayName("Bắt lỗi Place Bid: Người đấu giá không có role bidder")
    void testPlaceBid_InvalidRole() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        injectItemToManager(item);

        User mockAdmin = mock(User.class);
        when(mockAdmin.getRole()).thenReturn("admin");
        when(mockUserMgrInstance.findUserById("50")).thenReturn(mockAdmin);

        String result = auctionManager.placeBid("1", 1200.0, "50");
        assertTrue(result.contains("Chỉ người đấu giá mới được đặt giá"));
    }

    @Test
    @DisplayName("Bắt lỗi Place Bid: Giá đặt thấp hơn bước giá yêu cầu")
    void testPlaceBid_BidAmountTooLow() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setEndTime(LocalDateTime.now().plusMinutes(10));
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(1000.0);
        item.setBidIncrement(100.0); // Yêu cầu thấp nhất là 1100.0
        injectItemToManager(item);

        User mockBidder = mock(User.class);
        when(mockBidder.getRole()).thenReturn("bidder");
        when(mockUserMgrInstance.findUserById("30")).thenReturn(mockBidder);

        String result = auctionManager.placeBid("1", 1050.0, "30"); // Đặt 1050 là quá thấp
        assertTrue(result.contains("Giá thấp nhất có thể đặt hiện tại là: 1100.0"));
    }

    @Test
    @DisplayName("Admin Hủy phiên đấu giá thành công (Có hoàn tiền cho người đang dẫn đầu)")
    void testCancelAuctionByAdmin_Success() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(1500.0);
        injectItemToManager(item);

        String result = auctionManager.cancelAuctionByAdmin("1");

        assertEquals("success", result);
        assertEquals(AuctionStatus.CANCELED, item.getStatus());
        assertNull(item.getCurrentWinnerId());
        verify(mockUserMgrInstance, times(1)).addBalance("20", 1500.0); // Đảm bảo đã hoàn tiền
    }

    @Test
    @DisplayName("Admin Kết thúc sớm phiên đấu giá thành công (Chuyển tiền cho người bán)")
    void testEndAuctionByAdmin_Success() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(3000.0);
        injectItemToManager(item);

        String result = auctionManager.endAuctionByAdmin("1");

        assertTrue(result.contains("Đã kết thúc phiên đấu giá. Người thắng là user #20"));
        assertEquals(AuctionStatus.CLOSED, item.getStatus());
        verify(mockUserMgrInstance, times(1)).addBalance("10", 3000.0); // Chuyển tiền cho Seller
    }

    @Test
    @DisplayName("Admin Xóa Item thành công")
    void testDeleteItemByAdmin_Success() throws Exception {
        Item item = createDummyItem("1", "10", AuctionStatus.ACTIVE);
        item.setCurrentWinnerId("20");
        item.setCurrentPrice(2000.0);
        injectItemToManager(item);

        String result = auctionManager.deleteItemByAdmin("1");

        assertEquals("Đã xóa sản phẩm thành công.", result);
        verify(mockUserMgrInstance, times(1)).addBalance("20", 2000.0); // Đảm bảo hoàn tiền cho Winner trước khi xóa
        assertTrue(auctionManager.getAllItems().isEmpty()); // Item đã bay khỏi RAM
    }
}