package org.example.server.manager;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.example.common.model.user.Admin;
import org.example.common.model.user.Bidder;
import org.example.common.model.user.Seller;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserManagerTest {

    private MockedStatic<DatabaseManager> mockedDbManager;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private ResultSet mockGeneratedKeys;

    private UserManager userManager;

    @BeforeEach
    void setUp() throws Exception {
        // Reset Singleton Instance
        resetUserManagerSingleton();

        // Mocks for DB
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
        mockGeneratedKeys = mock(ResultSet.class);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockPreparedStatement);

        // Mặc định DB trống khi khởi tạo Manager
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        // Mock Execute Update thành công
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        mockedDbManager = mockStatic(DatabaseManager.class);
        mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);

        // Khởi tạo instance
        userManager = UserManager.getInstance();

        // Inject luồng đồng bộ để test các thao tác cộng/trừ tiền chạy ngầm
        injectDirectExecutor();
    }

    @AfterEach
    void tearDown() {
        mockedDbManager.close();
    }

    private void resetUserManagerSingleton() throws Exception {
        Field instanceField = UserManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    private void injectDirectExecutor() throws Exception {
        Field executorField = UserManager.class.getDeclaredField("dbExecutor");
        executorField.setAccessible(true);
        ExecutorService directExecutor = new java.util.concurrent.AbstractExecutorService() {
            private boolean isShutdown = false;
            @Override public void shutdown() { isShutdown = true; }
            @Override public List<Runnable> shutdownNow() { isShutdown = true; return Collections.emptyList(); }
            @Override public boolean isShutdown() { return isShutdown; }
            @Override public boolean isTerminated() { return isShutdown; }
            @Override public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return true; }
            @Override public void execute(Runnable command) { command.run(); }
        };
        executorField.set(userManager, directExecutor);
    }

    private void injectUserToRAM(User user) throws Exception {
        Field usersField = UserManager.class.getDeclaredField("users");
        usersField.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<User> list = (List<User>) usersField.get(userManager);
        list.add(user);
    }

    @Test
    @DisplayName("Kiểm tra thiết kế Singleton")
    void testSingleton() {
        UserManager instance1 = UserManager.getInstance();
        assertSame(userManager, instance1);
    }

    @Test
    @DisplayName("Tải dữ liệu từ DB (Bao phủ cả role hợp lệ và không hợp lệ)")
    void testLoadUsersFromDB() throws Exception {
        resetUserManagerSingleton(); // Reset để load lại từ đầu

        // Cấu hình mockResultSet trả về 2 user (1 hợp lệ, 1 không hợp lệ)
        when(mockResultSet.next()).thenReturn(true, true, false);

        when(mockResultSet.getString("id")).thenReturn("1", "2");
        when(mockResultSet.getString("username")).thenReturn("validUser", "invalidRoleUser");
        when(mockResultSet.getString("password")).thenReturn("pass1", "pass2");
        when(mockResultSet.getString("role")).thenReturn("bidder", "alien"); // alien là role lỗi
        when(mockResultSet.getDouble("balance")).thenReturn(100.0, 0.0);
        when(mockResultSet.getInt("is_banned")).thenReturn(0, 0);

        UserManager newManager = UserManager.getInstance();

        assertEquals(1, newManager.getAllUsers().size()); // Chỉ load thành công 1 user hợp lệ
        assertEquals("validUser", newManager.getAllUsers().get(0).getUsername());
    }

    @Test
    @DisplayName("Tạo tài khoản: Lỗi do dữ liệu đầu vào")
    void testCreateAccount_Validations() throws Exception {
        injectUserToRAM(new Bidder("1", "existingUser", "pass123", 0));

        assertEquals("Tên đăng nhập đã tồn tại", userManager.createAccount("existingUser", "123456", "bidder"));
        assertEquals("Tên đăng nhập phải có ít nhất 3 ký tự", userManager.createAccount("ab", "123456", "bidder"));
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự", userManager.createAccount("newUser", "123", "bidder"));
        assertEquals("Loại tài khoản không hợp lệ", userManager.createAccount("newUser", "123456", "superman"));
    }

    @Test
    @DisplayName("Tạo tài khoản: Thành công (Mock sinh khóa tự động)")
    void testCreateAccount_Success() throws Exception {
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockGeneratedKeys);
        when(mockGeneratedKeys.next()).thenReturn(true);
        when(mockGeneratedKeys.getLong(1)).thenReturn(99L); // ID tự sinh ra

        String result = userManager.createAccount("nguyen_a", "securePass123", "nguoiban"); // test normalizeRole

        assertEquals("Tạo tài khoản thành công!", result);
        User savedUser = userManager.findUserById("99");
        assertNotNull(savedUser);
        assertTrue(savedUser instanceof Seller); // "nguoiban" -> "seller"
    }

    @Test
    @DisplayName("Đăng nhập: Sai thông tin / Bị Ban")
    void testLogin_Fails() throws Exception {
        // User không tồn tại
        when(mockResultSet.next()).thenReturn(false);
        assertNull(userManager.login("ghost", "pass"));

        // User bị khóa
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("is_banned")).thenReturn(1);
        assertNull(userManager.login("bannedUser", "pass"));
    }

    @Test
    @DisplayName("Đăng nhập: Pass Bcrypt và Pass trơn tự nâng cấp")
    void testLogin_Success_And_UpgradeBcrypt() throws Exception {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("is_banned")).thenReturn(0);
        when(mockResultSet.getString("id")).thenReturn("1");
        when(mockResultSet.getString("username")).thenReturn("testUser");
        when(mockResultSet.getString("role")).thenReturn("quantrivien"); // "quantrivien" -> admin
        when(mockResultSet.getDouble("balance")).thenReturn(0.0);

        // Trường hợp 1: Pass trơn -> Tự động mã hoá Bcrypt lưu ngược vào DB
        when(mockResultSet.getString("password")).thenReturn("plainPass123");
        User user1 = userManager.login("testUser", "plainPass123");
        assertNotNull(user1);
        assertTrue(user1 instanceof Admin);
        verify(mockPreparedStatement, atLeastOnce()).executeUpdate(); // Xác minh hàm nâng cấp Bcrypt đã chạy lệnh UPDATE

        // Trường hợp 2: Pass đã là Bcrypt
        String hashed = BCrypt.withDefaults().hashToString(12, "hashedPass".toCharArray());
        when(mockResultSet.getString("password")).thenReturn(hashed);
        User user2 = userManager.login("testUser", "hashedPass");
        assertNotNull(user2);
    }

    @Test
    @DisplayName("Lấy Role chuần từ DB")
    void testGetNormalizedRoleFromDB() throws Exception {
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("role")).thenReturn("  Nguoi Dau Gia  ");

        String role = userManager.getNormalizedRoleFromDB("testUser");
        assertEquals("bidder", role);

        assertNull(userManager.getNormalizedRoleFromDB(""));
    }

    @Test
    @DisplayName("Bắt lỗi DB chung")
    void testSQLExceptions() throws Exception {
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Mock DB Error"));

        assertNull(userManager.getNormalizedRoleFromDB("user"));
        assertFalse(userManager.updateUser(new Bidder("1", "a", "b", 0)));

        // ID "1" chưa được nạp vào RAM nên hàm banUser sẽ trả về chuỗi thông báo không tồn tại
        assertEquals("Người dùng không tồn tại!", userManager.banUser("1"));

        // Nạp ID "99" vào RAM để vượt qua bước validate null, ép hàm chạy xuống gọi Database để tạo lỗi SQLException
        injectUserToRAM(new Bidder("99", "u", "p", 0));
        assertEquals("Lỗi Database", userManager.banUser("99"));
    }

    @Test
    @DisplayName("Cập nhật quyền User (updateUserRole)")
    void testUpdateUserRole() throws Exception {
        injectUserToRAM(new Bidder("1", "userA", "pass", 100));

        // Test update thành công
        String res1 = userManager.updateUserRole("1", "seller");
        assertEquals("Cập nhật quyền thành công!", res1);
        assertTrue(userManager.findUserById("1") instanceof Seller);

        // Test sai role
        assertEquals("Invalid role", userManager.updateUserRole("1", "superman"));

        // Test user không tồn tại trong DB (executeUpdate = 0)
        when(mockPreparedStatement.executeUpdate()).thenReturn(0);
        assertEquals("Không tìm thấy user.", userManager.updateUserRole("999", "admin"));
    }

    @Test
    @DisplayName("Khóa và mở khóa User (Ban / Unban)")
    void testBanAndUnbanUser() throws Exception {
        User user = new Bidder("1", "userA", "pass", 100);
        injectUserToRAM(user);

        assertEquals("success", userManager.banUser("1"));
        assertTrue(user.isBanned());

        assertEquals("success", userManager.unbanUser("1"));
        assertFalse(user.isBanned());

        assertEquals("Người dùng không tồn tại!", userManager.banUser("999"));
    }

    @Test
    @DisplayName("Nạp tiền, trừ tiền, cộng tiền (Có luồng đồng bộ)")
    void testBalanceOperations() throws Exception {
        User user = new Bidder("1", "userA", "pass", 500);
        injectUserToRAM(user);

        // Nạp tiền (TopUp)
        User toppedUser = userManager.topUpBalance("1", 200, "bank");
        assertNotNull(toppedUser);
        assertEquals(700.0, toppedUser.getBalance());
        verify(mockPreparedStatement, times(1)).executeUpdate(); // Đảm bảo luồng ngầm đã lưu xuống DB

        // Trừ tiền thành công
        assertTrue(userManager.subtractBalance("1", 100));
        assertEquals(600.0, userManager.findUserById("1").getBalance());

        // Trừ tiền thất bại (không đủ số dư)
        assertFalse(userManager.subtractBalance("1", 9999));
        assertEquals(600.0, userManager.findUserById("1").getBalance());

        // Add tiền cơ bản
        assertTrue(userManager.addBalance("1", 50));
        assertEquals(650.0, userManager.findUserById("1").getBalance());

        // Thao tác với user ảo
        assertNull(userManager.topUpBalance("999", 100, "bank"));
        assertFalse(userManager.subtractBalance("999", 10));
    }
}