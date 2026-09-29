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

    /**
     * What changes a kill's drops (Looting) is a factor on the drop's own chance, before Magic Find, whose 5% rule
     * goes by what it makes it: the wiki's Looting, a 3% drop is 4.8% with Looting IV (Magic Find still applies) and
     * 5.25% with V (it doesn't).
     */
    @Test
    void killFactor() {
        assertEquals(0.1 * 1.15 * 2, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 0.1), 100, 0, 1.15), 1e-12);
        assertEquals(3 * 1.6 * 2, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 3), 100, 0, 1.6), 1e-12);
        assertEquals(5.25, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 3), 100, 0, 1.75), 1e-12);
        assertEquals(5 * 1.3, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 5), 300, 0, 1.3), 1e-12);
        assertEquals(0.1, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 0.1), 0, 0, 1), 1e-12);
        assertEquals(0, Mobs.chance(new MobDrop("TEST_BONE", MobDropType.RARE, 50), 0, 0, -2), 1e-12);
    }

    /** Past 100% it drops again: Looting IV on a sure drop is once, and a 60% chance of twice (the wiki's Looting). */
    @Test
    void copies() {
        assertEquals(2, Mobs.copies(160, 0.59));
        assertEquals(1, Mobs.copies(160, 0.61));
        assertEquals(1, Mobs.copies(100, 0.999));
        assertEquals(3, Mobs.copies(250, 0.4));
        assertEquals(1, Mobs.copies(50, 0.49));
        assertEquals(0, Mobs.copies(50, 0.5));
        assertEquals(0, Mobs.copies(0, 0));
        assertEquals(0, Mobs.copies(-5, 0));
    }

    /** Looting, Chance and Luck: times 1 + each one's percent; Looting V and Luck VII on an armor drop, 1.75 x 1.35. */
    @Test
    void enchantFactors() {
        assertEquals(1, DropEnchants.factor(0, 0, 0), 1e-12);
        assertEquals(1.45, DropEnchants.factor(45, 0, 0), 1e-12);
        assertEquals(1.75, DropEnchants.factor(0, 75, 0), 1e-12);
        assertEquals(1.75 * 1.35, DropEnchants.factor(75, 0, 35), 1e-12);
    }

    /** "All pets": a pet at 5% or more still gets Magic Find and Pet Luck, where another drop that likely doesn't. */
    @Test
    void likelyPet() {
        assertEquals(15, MobDrop.withMagicFind(5, 100, 100, true), 1e-12);
        assertEquals(20, MobDrop.withMagicFind(10, 100, 0, true), 1e-12);
        assertEquals(5, MobDrop.withMagicFind(5, 100, 100, false), 1e-12);
    }
}
