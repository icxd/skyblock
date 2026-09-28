package net.icxd.dungeons.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** In combat: damage dealt or taken within the window, whichever was later. */
class CombatStateTest {
    @Test
    void window() {
        long window = CombatState.WINDOW_MILLIS;
        assertTrue(CombatState.inCombat(10_000, 0, 10_000 + window, window));
        assertFalse(CombatState.inCombat(10_000, 0, 10_001 + window, window));
        // The later of the two counts.
        assertTrue(CombatState.inCombat(1_000, 12_000, 12_000 + window, window));
        assertTrue(CombatState.inCombat(12_000, 1_000, 13_000, window));
        // Never in combat yet.
        assertFalse(CombatState.inCombat(0, 0, 1_000_000, window));
        assertEquals(Long.MAX_VALUE, CombatState.since(0, 0, 5));
        assertEquals(3_000, CombatState.since(2_000, 1_000, 5_000));
        // A longer window of an effect's own.
        assertTrue(CombatState.inCombat(10_000, 0, 17_000, 8_000));
    }
}
