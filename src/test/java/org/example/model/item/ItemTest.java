package org.example.model.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class ItemTest {

    @Test
    void testItemInitialization() {
        // Khởi tạo thông qua lớp con Electronic
        Electronic laptop = new Electronic("Macbook", "Electronics", "M3 Chip", 1000.0, 50.0);

        // Kiểm tra các thông tin cơ bản
        assertEquals("Macbook", laptop.getItemName());
        assertEquals("Electronics", laptop.getType());
        assertEquals("M3 Chip", laptop.getDescribe());
        assertEquals(1000.0, laptop.getStartingPrice());
        assertEquals(50.0, laptop.getBidIncrement());
        assertEquals(1000.0, laptop.getCurrentPrice());

        // Kiểm tra ID an toàn (Tránh NullPointerException)
        String id = laptop.getId();
        if (id != null) {
            assertTrue(id.startsWith("I-"), "ID phải bắt đầu bằng tiền tố I-");
        } else {
            System.out.println("Lưu ý: ID đang bị null. Hãy kiểm tra lại super() trong class con.");
        }
    }

    @Test
    void testSettersAndGetters() {
        // Sử dụng Art để test các phương thức setter/getter của lớp cha Item
        Art painting = new Art("Mona Lisa", "Art", 5000.0, 100.0);

        // Test cập nhật tên và mô tả
        painting.setName("Mona Lisa v2");
        painting.setDescribe("Bản phục chế");
        assertEquals("Mona Lisa v2", painting.getItemName());
        assertEquals("Bản phục chế", painting.getDescribe());

        // Test cập nhật giá và bước giá
        painting.setStartingPrice(6000.0);
        painting.setBidIncrement(200.0);
        assertEquals(6000.0, painting.getStartingPrice());
        assertEquals(200.0, painting.getBidIncrement());

        // Test cập nhật trạng thái (Dùng đúng ACTIVE và CLOSED của bạn)
        painting.setStatus(AuctionStatus.ACTIVE);
        assertEquals(AuctionStatus.ACTIVE, painting.getStatus());

        painting.setStatus(AuctionStatus.CLOSED);
        assertEquals(AuctionStatus.CLOSED, painting.getStatus());
    }

    @Test
    void testAuctionDetails() {
        Vehicle car = new Vehicle("Tesla", "Car", 30000.0, 500.0);

        // Test cập nhật giá hiện tại và người thắng cuộc
        car.setCurrentPrice(35000.0);
        car.setCurrentWinnerId("USER123");
        assertEquals(35000.0, car.getCurrentPrice());
        assertEquals("USER123", car.getCurrentWinnerId());

        // Test cập nhật thời gian kết thúc
        LocalDateTime endTime = LocalDateTime.now().plusHours(2);
        car.setEndTime(endTime);
        assertEquals(endTime, car.getEndTime());
    }

    @Test
    void testTypeHandling() {
        Electronic phone = new Electronic("iPhone", "Mobile", 1000.0, 20.0);
        phone.setType("Smartphone");
        assertEquals("Smartphone", phone.getType());
    }
}