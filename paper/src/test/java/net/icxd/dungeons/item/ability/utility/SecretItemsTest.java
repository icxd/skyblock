package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;

/** The dungeon secret items' rules. */
class SecretItemsTest {
    private static final double EPS = 1e-9;

    /** "dealing 20,000-250,000 damage based on the dungeon floor you are on": the Entrance's least, Floor VII's most. */
    @Test
    void trapDamageByFloor() {
        assertEquals(20_000, SecretItems.trapDamage(DungeonFloor.ENTRANCE), EPS);
        assertEquals(250_000, SecretItems.trapDamage(DungeonFloor.FLOOR_7), EPS);
        assertEquals(250_000, SecretItems.trapDamage(DungeonFloor.MASTER_FLOOR_7), EPS);
        assertEquals(20_000 + 230_000 * 3 / 7.0, SecretItems.trapDamage(DungeonFloor.FLOOR_3), EPS);
        assertEquals(20_000, SecretItems.trapDamage(null), EPS);
    }

    @Test
    void theItemsUsedByARightClick() {
        assertTrue(SecretItems.is("DUNGEON_DECOY"));
        assertTrue(SecretItems.is("ARCHITECT_FIRST_DRAFT"));
        assertFalse(SecretItems.is("TRAINING_WEIGHTS"));
        assertFalse(SecretItems.is("SPIRIT_LEAP"));
    }
}
