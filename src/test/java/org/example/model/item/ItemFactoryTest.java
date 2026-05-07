package org.example.model.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ItemFactoryTest {

    @Test
    void testCreateArt() {
        Item item = ItemFactory.createItem("art", "Bức tranh", "Tranh dầu", "Mô tả", 100.0, 10.0);
        assertNotNull(item);
        assertTrue(item instanceof Art);
        assertEquals("Bức tranh", item.getItemName());
    }

    @Test
    void testCreateElectronic() {
        Item item = ItemFactory.createItem("electronic", "Laptop", "Điện tử", "Mới", 500.0, 50.0);
        assertNotNull(item);
        assertTrue(item instanceof Electronic);
        assertEquals("điện tử", item.getType().toLowerCase());
    }

    @Test
    void testCreateVehicle() {
        Item item = ItemFactory.createItem("vehicle", "Xe máy", "Phương tiện", "Cũ", 1000.0, 100.0);
        assertNotNull(item);
        assertTrue(item instanceof Vehicle);
    }

    @Test
    void testCreateWithTrimAndUpperCase() {
        // Kiểm tra xem Factory có xử lý được khoảng trắng và chữ hoa không
        Item item = ItemFactory.createItem("  ART  ", "Tượng", "Art", "Đẹp", 200.0, 20.0);
        assertTrue(item instanceof Art);
    }

    @Test
    void testCreateItemNullCategory() {
        // Kiểm tra trường hợp category bị null
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem(null, "Name", "Type", "Desc", 0, 0);
        });
        assertEquals("Category can't be null", exception.getMessage());
    }

    @Test
    void testCreateItemInvalidCategory() {
        // Kiểm tra trường hợp loại sản phẩm không tồn tại
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            ItemFactory.createItem("unknown", "Name", "Type", "Desc", 0, 0);
        });
        assertTrue(exception.getMessage().contains("Loại sản phẩm không hợp lệ"));
    }
}