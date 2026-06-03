package org.example.model.user;

import org.example.common.model.user.Admin;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

class AdminTest {

    @Test
    void testAdminConstructorAndGetters() {
        Admin admin = new Admin("AD01", "superadmin", "pass123");

        assertNotNull(admin);
        assertEquals("AD01", admin.getId());
        assertEquals("superadmin", admin.getUsername());
    }

    @Test
    void testGetRole() {
        Admin admin = new Admin("AD01", "admin", "123");
        assertEquals("admin", admin.getRole());
    }

    @Test
    void testBanUser() {
        Admin admin = new Admin("AD01", "admin01", "123");
        Admin targetUser = new Admin("U02", "bad_user", "123");

        assertDoesNotThrow(() -> admin.banUser(targetUser));
    }

    @Test
    void testDisplayRole() {
        Admin admin = new Admin("AD01", "admin_boss", "123");

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        admin.displayRole();

        String output = outContent.toString().trim();
        assertTrue(output.contains("Role: Admin"));
        assertTrue(output.contains("admin_boss"));

        System.setOut(System.out);
    }
}