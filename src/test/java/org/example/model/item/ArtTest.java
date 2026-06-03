package org.example.model.item;

import org.example.common.model.item.Art;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtTest {

    @Test
    void testArtFullConstructor() {
        Art art = new Art("Mona Lisa", "Art", "Classic", 1000.0, 50.0);
        assertNotNull(art);
        assertEquals("Mona Lisa", art.getItemName());
    }

    @Test
    void testArtShortConstructor() {
        Art art = new Art("Starry Night", "Art", 2000.0, 100.0);
        assertNotNull(art);
        assertEquals(2000.0, art.getStartingPrice());
    }
}