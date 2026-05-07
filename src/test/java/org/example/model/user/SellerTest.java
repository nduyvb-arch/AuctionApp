package org.example.model.user;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    @Test
    void testSellerConstructorAndGetters() {
        // 1. Kiểm tra khởi tạo và kế thừa từ User
        Seller seller = new Seller("S001", "seller_pro", "password123");

        assertNotNull(seller);
        assertEquals("S001", seller.getId());
        assertEquals("seller_pro", seller.getUsername());
    }

    @Test
    void testGetRole() {
        // 2. Kiểm tra phương thức getRole trả về đúng "seller"
        Seller seller = new Seller("S001", "name", "pass");
        assertEquals("seller", seller.getRole());
    }

    @Test
    void testPutItem() {
        // 3. Kiểm tra phương thức putItem (phương thức void)
        Seller seller = new Seller("S001", "Minh", "123");

        // Chỉ cần gọi hàm để đảm bảo code chạy qua dòng này (tăng coverage)
        assertDoesNotThrow(() -> seller.putItem("Iphone 15 Pro Max"));
    }

    @Test
    void testDisplayRole() {
        // 4. Kiểm tra displayRole in ra màn hình đúng định dạng
        Seller seller = new Seller("S001", "Hoang", "123");

        // Bắt đầu chặn luồng in console để kiểm tra nội dung
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        seller.displayRole();

        String output = outContent.toString().trim();
        assertTrue(output.contains("Role: Seller"));
        assertTrue(output.contains("Hoang"));

        // Trả lại luồng in mặc định
        System.setOut(originalOut);
    }
}
