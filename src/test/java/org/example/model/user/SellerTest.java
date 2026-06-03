package org.example.model.user;

import org.example.common.model.user.Seller;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class SellerTest {

    @Test
    void testSellerConstructorAndGetters() {
        Seller seller = new Seller("S001", "seller_pro", "password123");

        assertNotNull(seller);
        assertEquals("S001", seller.getId());
        assertEquals("seller_pro", seller.getUsername());
    }

    @Test
    void testGetRole() {
        Seller seller = new Seller("S001", "name", "pass");
        assertEquals("seller", seller.getRole());
    }

    @Test
    void testPutItem() {
        Seller seller = new Seller("S001", "Minh", "123");

        assertDoesNotThrow(() -> seller.putItem("Iphone 15 Pro Max"));
    }

    @Test
    void testDisplayRole() {
        Seller seller = new Seller("S001", "Hoang", "123");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        seller.displayRole();

        String output = outContent.toString().trim();
        assertTrue(output.contains("Role: Seller"));
        assertTrue(output.contains("Hoang"));

        System.setOut(originalOut);
    }
}
