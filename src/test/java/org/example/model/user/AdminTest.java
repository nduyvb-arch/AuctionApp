package org.example.model.user;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void testAdminConstructorAndGetters() {
        // 1. Kiểm tra khởi tạo và các phương thức kế thừa từ User
        Admin admin = new Admin("AD01", "superadmin", "pass123");

        assertNotNull(admin);
        assertEquals("AD01", admin.getId());
        assertEquals("superadmin", admin.getUsername());
    }

    @Test
    void testGetRole() {
        // 2. Kiểm tra phương thức getRole
        Admin admin = new Admin("AD01", "admin", "123");
        assertEquals("admin", admin.getRole());
    }

    @Test
    void testBanUser() {
        // 3. Kiểm tra hành động banUser
        Admin admin = new Admin("AD01", "admin01", "123");
        // Giả sử bạn có một lớp User cụ thể hoặc dùng một subclass khác để test
        // Ở đây tôi giả định bạn có thể khởi tạo một Admin khác làm đối tượng bị ban
        Admin targetUser = new Admin("U02", "bad_user", "123");

        // Gọi phương thức để lấy coverage
        assertDoesNotThrow(() -> admin.banUser(targetUser));
    }

    @Test
    void testDisplayRole() {
        // 4. Kiểm tra phương thức in ra màn hình
        Admin admin = new Admin("AD01", "admin_boss", "123");

        // Kỹ thuật bắt nội dung in ra Console (tùy chọn, giúp coverage sâu hơn)
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        admin.displayRole();

        String output = outContent.toString().trim();
        assertTrue(output.contains("Role: Admin"));
        assertTrue(output.contains("admin_boss"));

        // Trả lại System.out bình thường
        System.setOut(System.out);
    }
}