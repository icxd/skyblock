package net.icxd.dungeons.mob;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobsTest {
    /** A mob of this kind as it first spawns (the Entrance's lowest level, for a dungeon kind); null for no such kind. */
    static DataMob mob(String id) {
        MobKind kind = Mobs.kind(id);
        return kind == null ? null : new DataMob(kind, kind.firstVariant(), SpawnOptions.NONE);
    }

    /** The Hub's style, as the two test mobs have always had it. */
    @Test
    void nameTags() {
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a1M&f/&a1M&c❤", Mobs.nameTag(mob("MAGMA_CUBE"), 1_000_000));
        assertEquals("&8[&7Lv75&8] &cMagma Cube &a0&f/&a1M&c❤", Mobs.nameTag(mob("magma_cube"), -5));
        assertEquals("&e﴾ &8[&7Lv200&8] &c&8&lBladesoul &a50M&f/&a50M&c❤ &e﴿", Mobs.nameTag(mob("BLADESOUL"), 50_000_000));
    }

    @Test
    void registry() {
        assertNotNull(mob("BLADESOUL"));
        assertNotNull(mob("zombie_grunt"));
        assertNull(Mobs.kind("NOT_A_MOB"));
        // The Entrance's eleven, the Watcher's undeads, the secret bat and the Hub's two.
        assertEquals(15, Mobs.registry().size());
        assertTrue(Mobs.registry().keySet().containsAll(List.of("MAGMA_CUBE", "BLADESOUL", "ZOMBIE_GRUNT", "ANGRY_ARCHAEOLOGIST")));
        // A dungeon kind comes at its Entrance's first level; the Hub's as they are.
        assertEquals(40, mob("ZOMBIE_GRUNT").getLevel());
        assertEquals(7_000, mob("ZOMBIE_GRUNT").getMaxHealth());
        assertEquals(1_000_000, mob("MAGMA_CUBE").getMaxHealth());
        assertEquals(4_000, mob("BLADESOUL").getDamage());
    }
}
