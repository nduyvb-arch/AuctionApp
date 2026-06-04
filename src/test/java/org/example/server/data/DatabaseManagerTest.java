package org.example.server.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DatabaseManagerTest {

    private MockedStatic<DriverManager> mockedDriverManager;
    private Connection mockConnection;
    private Statement mockStatement;

    private static File actualEnv = new File(".env");
    private static File backupEnvFile = new File(".env_backup");
    private boolean envBackedUp = false;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Sao lưu file .env gốc nếu có và tạo file giả lập môi trường sạch để class đọc dữ liệu thật
        if (actualEnv.exists() && !envBackedUp) {
            actualEnv.renameTo(backupEnvFile);
            envBackedUp = true;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(".env"))) {
            writer.write("# Hệ thống kiểm thử tự động\n");
            writer.write("\n"); // Kiểm tra dòng trống
            writer.write("DB_URL=jdbc:mysql://localhost:3306/auction\n");
            writer.write("DB_USER=root\n");
            writer.write("DB_PASSWORD=secret\n");
            writer.write("INVALID_LINE_TEST\n"); // Kiểm tra dòng thiếu dấu gạch ngang '='
        }

        // 2. Khởi tạo Mock JDBC độc lập câu lệnh
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        when(mockConnection.createStatement()).thenReturn(mockStatement);

        // Đóng mock static cũ trước khi mở luồng kiểm thử mới
        if (mockedDriverManager != null) {
            mockedDriverManager.close();
        }

        // Mock Static DriverManager toàn cục
        mockedDriverManager = mockStatic(DriverManager.class);
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString(), any(), any()))
                .thenReturn(mockConnection);
        mockedDriverManager.when(() -> DriverManager.getConnection(any(), any(), any()))
                .thenReturn(mockConnection);
        mockedDriverManager.when(() -> DriverManager.getConnection(anyString()))
                .thenReturn(mockConnection);

        // Đặt lại cờ tạo bảng về false để ép chạy qua các hàm tuần tự bên trong
        setTablesCreatedField(false);
    }

    @AfterEach
    void tearDown() {
        // Giải phóng tài nguyên mock static an toàn
        if (mockedDriverManager != null) {
            mockedDriverManager.close();
            mockedDriverManager = null;
        }

        // Dọn dẹp file .env tạm và trả lại file .env gốc ban đầu cho dự án
        if (actualEnv.exists()) {
            actualEnv.delete();
        }
        if (envBackedUp && backupEnvFile.exists()) {
            backupEnvFile.renameTo(actualEnv);
            envBackedUp = false;
        }
    }

    private void setTablesCreatedField(boolean value) throws Exception {
        Field field = DatabaseManager.class.getDeclaredField("tablesCreated");
        field.setAccessible(true);
        field.set(null, value);
    }

    @Test
    @DisplayName("Lấy Connection thành công và chạy tự động tạo bảng lần đầu")
    void testGetConnection_Success_FirstTime() throws Exception {
        Connection conn = DatabaseManager.getConnection();
        assertNotNull(conn);
        verify(mockConnection, times(1)).createStatement();
        verify(mockStatement, atLeast(3)).execute(anyString());
    }

    @Test
    @DisplayName("Lấy Connection các lần tiếp theo không chạy lại tạo bảng")
    void testGetConnection_SecondTime() throws Exception {
        DatabaseManager.getConnection();
        Connection conn = DatabaseManager.getConnection();
        assertNotNull(conn);
        verify(mockConnection, times(1)).createStatement();
    }

    @Test
    @DisplayName("Xử lý ngoại lệ SQLException khi tạo bảng bị lỗi")
    void testAutoCreateTables_HandlesSQLException() throws Exception {
        doThrow(new SQLException("Mock DB Error")).when(mockStatement).execute(anyString());
        assertNotNull(DatabaseManager.getConnection());
        verify(mockConnection, times(1)).createStatement();
    }

    @Test
    @DisplayName("Thêm cột bị trùng - Bỏ qua mã lỗi MySQL 1060 hoặc SQLState 42S21")
    void testAddColumnIfMissing_ColumnAlreadyExists() throws Exception {
        SQLException sqlException1 ;}
}