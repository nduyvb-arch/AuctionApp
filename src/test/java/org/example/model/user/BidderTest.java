package org.example.model.user;

import org.example.common.model.user.Bidder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BidderTest {

    @Test
    void testBidderCreation() {
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
        Bidder bidder = new Bidder("U1", "luong_test", "pass", 1000.0);

        assertDoesNotThrow(() -> {
            bidder.displayRole();
            bidder.placeBid(200.0);
        });
    }
}