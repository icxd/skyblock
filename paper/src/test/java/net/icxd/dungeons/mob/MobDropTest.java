package net.icxd.dungeons.mob;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Magic Find and Pet Luck on drop chances (the wiki's Magic Find and Pet Luck). */
class MobDropTest {
    @Test
    void magicFind() {
        // Chance x (1 + Magic Find / 100) on a rare drop.
        assertEquals(0.2, MobDrop.withMagicFind(0.1, 100, 0, false), 1e-12);
        assertEquals(0.115, MobDrop.withMagicFind(0.1, 15, 0, false), 1e-12);
        assertEquals(4.99, MobDrop.withMagicFind(4.99, 0, 0, false), 1e-12);
        // Not on drops of 5% and more.
        assertEquals(5, MobDrop.withMagicFind(5, 300, 0, false), 1e-12);
        assertEquals(100, MobDrop.withMagicFind(100, 300, 0, false), 1e-12);
        // At most 900 counts; less than none is none.
        assertEquals(1, MobDrop.withMagicFind(0.1, 1500, 0, false), 1e-12);
        assertEquals(0.1, MobDrop.withMagicFind(0.1, -20, 0, false), 1e-12);
        assertEquals(900, MobDrop.magicFind(1234), 1e-12);
    }

    /** A pet's chance adds Pet Luck to Magic Find; other drops ignore it. */
    @Test
    void petLuck() {
        assertEquals(0.3, MobDrop.withMagicFind(0.1, 100, 100, true), 1e-12);
        assertEquals(0.2, MobDrop.withMagicFind(0.1, 100, 100, false), 1e-12);
        assertEquals(0.15, MobDrop.withMagicFind(0.1, 0, 50, true), 1e-12);
    }

    /** What changes a kill's drops (Looting-like) is a factor after Magic Find, on drops of 5% and more too. */
    @Test
    void killFactor() {
        assertEquals(0.2 * 1.15, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 0.1), 100, 0, 1.15), 1e-12);
        assertEquals(5 * 1.3, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 5), 300, 0, 1.3), 1e-12);
        assertEquals(0.1, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 0.1), 0, 0, 1), 1e-12);
        assertEquals(0, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 50), 0, 0, -2), 1e-12);
    }

    /** "All pets": a pet at 5% or more still gets Magic Find and Pet Luck, where another drop that likely doesn't. */
    @Test
    void likelyPet() {
        assertEquals(15, MobDrop.withMagicFind(5, 100, 100, true), 1e-12);
        assertEquals(20, MobDrop.withMagicFind(10, 100, 0, true), 1e-12);
        assertEquals(5, MobDrop.withMagicFind(5, 100, 100, false), 1e-12);
    }
}
