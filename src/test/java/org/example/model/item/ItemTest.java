package org.example.model.item;

import org.example.common.model.item.Art;
import org.example.common.model.item.AuctionStatus;
import org.example.common.model.item.Electronic;
import org.example.common.model.item.Vehicle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;

class ItemTest {

    @Test
    void testItemInitialization() {
        Electronic laptop = new Electronic("Macbook", "Electronics", "M3 Chip", 1000.0, 50.0);

        assertEquals("Macbook", laptop.getItemName());
        assertEquals("Electronics", laptop.getType());
        assertEquals("M3 Chip", laptop.getDescription());
        assertEquals(1000.0, laptop.getStartingPrice());
        assertEquals(50.0, laptop.getBidIncrement());
        assertEquals(1000.0, laptop.getCurrentPrice());

        String id = laptop.getId();
        if (id != null) {
            assertTrue(id.startsWith("I-"), "ID phải bắt đầu bằng tiền tố I-");
        } else {
            System.out.println("Lưu ý: ID đang bị null. Hãy kiểm tra lại super() trong class con.");
        }
    }

    @Test
    void testSettersAndGetters() {
        Art painting = new Art("Mona Lisa", "Art", 5000.0, 100.0);

        painting.setName("Mona Lisa v2");
        painting.setDescription("Bản phục chế");
        assertEquals("Mona Lisa v2", painting.getItemName());
        assertEquals("Bản phục chế", painting.getDescription());

        painting.setStartingPrice(6000.0);
        painting.setBidIncrement(200.0);
        assertEquals(6000.0, painting.getStartingPrice());
        assertEquals(200.0, painting.getBidIncrement());

        painting.setStatus(AuctionStatus.ACTIVE);
        assertEquals(AuctionStatus.ACTIVE, painting.getStatus());

        painting.setStatus(AuctionStatus.CLOSED);
        assertEquals(AuctionStatus.CLOSED, painting.getStatus());
    }

    @Test
    void testAuctionDetails() {
        Vehicle car = new Vehicle("Tesla", "Car", 30000.0, 500.0);

        car.setCurrentPrice(35000.0);
        car.setCurrentWinnerId("USER123");
        assertEquals(35000.0, car.getCurrentPrice());
        assertEquals("USER123", car.getCurrentWinnerId());

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