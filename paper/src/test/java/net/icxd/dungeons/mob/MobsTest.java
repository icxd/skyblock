package net.icxd.dungeons.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MobsTest {
    @Test
    void nameTags() {
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a1M&f/&a1M&c❤", Mobs.nameTag(Mobs.get("MAGMA_CUBE"), 1_000_000));
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a0&f/&a1M&c❤", Mobs.nameTag(Mobs.get("magma_cube"), -5));
        assertEquals("&e﴾ &8[&7Lv200&8] &c&8&lBladesoul &a50M&f/&a50M&c❤ &e﴿", Mobs.nameTag(Mobs.get("BLADESOUL"), 50_000_000));
    }

    @Test
    void registry() {
        assertNotNull(Mobs.get("BLADESOUL"));
        assertEquals(2, Mobs.registry().size());
    }
}
