package net.icxd.dungeons.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobsTest {
    /** The Hub's style, as the two test mobs have always had it. */
    @Test
    void nameTags() {
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a1M&f/&a1M&c❤", Mobs.nameTag(Mobs.get("MAGMA_CUBE"), 1_000_000));
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a0&f/&a1M&c❤", Mobs.nameTag(Mobs.get("magma_cube"), -5));
        assertEquals("&e﴾ &8[&7Lv200&8] &c&8&lBladesoul &a50M&f/&a50M&c❤ &e﴿", Mobs.nameTag(Mobs.get("BLADESOUL"), 50_000_000));
    }

    @Test
    void registry() {
        assertNotNull(Mobs.get("BLADESOUL"));
        assertNotNull(Mobs.get("zombie_grunt"));
        assertNull(Mobs.get("NOT_A_MOB"));
        // The Entrance's eleven, the Watcher's undeads and the Hub's two.
        assertEquals(14, Mobs.registry().size());
        assertTrue(Mobs.registry().keySet().containsAll(java.util.List.of("MAGMA_CUBE", "BLADESOUL", "ZOMBIE_GRUNT", "ANGRY_ARCHAEOLOGIST")));
        // A dungeon kind comes at its Entrance's first level; the Hub's as they are.
        assertEquals(40, Mobs.get("ZOMBIE_GRUNT").getLevel());
        assertEquals(7_000, Mobs.get("ZOMBIE_GRUNT").getMaxHealth());
        assertEquals(1_000_000, Mobs.get("MAGMA_CUBE").getMaxHealth());
        assertEquals(4_000, Mobs.get("BLADESOUL").getDamage());
    }
}
