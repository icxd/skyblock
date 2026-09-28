package net.icxd.dungeons.reforge;

import net.icxd.dungeons.mob.MobType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The armor reforges' cuts on hits by Undead and Nether mobs (the rest is in ArmorEnchantsTest, with worn pieces). */
class ArmorReforgeBonusesTest {
    /** Undead's and Cubic's cuts add up; never below none. */
    @Test
    void cuts() {
        assertEquals(0.94, ArmorReforgeBonuses.taken(4, 2), 1e-9);
        assertEquals(1, ArmorReforgeBonuses.taken(0, 0), 1e-9);
        assertEquals(0, ArmorReforgeBonuses.taken(80, 40), 1e-9);
    }

    /** "Nether mobs" are the Crimson Isle's types: Infernal, Magmatic and Arcane. */
    @Test
    void netherMobs() {
        assertTrue(ArmorReforgeBonuses.nether(Set.of(MobType.CUBIC, MobType.INFERNAL)));
        assertTrue(ArmorReforgeBonuses.nether(Set.of(MobType.MAGMATIC)));
        assertFalse(ArmorReforgeBonuses.nether(Set.of(MobType.UNDEAD)));
        assertFalse(ArmorReforgeBonuses.nether(Set.of()));
    }
}
