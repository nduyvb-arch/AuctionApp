package org.example.model.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BidderTest {

    @Test
    void testBidderCreation() {
        // Test constructor và các getter kế thừa từ User
        Bidder bidder = new Bidder("U1", "luong_bidder", "password123", 1000.0);

        assertEquals("U1", bidder.getId());
        assertEquals("luong_bidder", bidder.getUsername());
        assertEquals(1000.0, bidder.getBalance());
        assertEquals("bidder", bidder.getRole());
    }

    @Test
    void testSetBalance() {
        Bidder bidder = new Bidder("U1", "user", "pass", 500.0);
        bidder.setBalance(750.0);
        assertEquals(750.0, bidder.getBalance());
    }

    @Test
    void testDisplayRoleAndPlaceBid() {
        // Test các phương thức void để đảm bảo dòng code được thực thi (tăng coverage)
        Bidder bidder = new Bidder("U1", "luong_test", "pass", 1000.0);

        assertDoesNotThrow(() -> {
            bidder.displayRole();
            bidder.placeBid(200.0);
        });
    }
}