package net.icxd.dungeons.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What stats do to a player's vanilla attributes. */
class PlayerAttributesTest {
    /** Swing Range is the reach in blocks: the base 3 is vanilla's, a Giant's Sword's +1 makes 4, and 15 is the most. */
    @Test
    void reach() {
        assertEquals(0, PlayerAttributes.reachBonus(3), 1e-9);
        assertEquals(1, PlayerAttributes.reachBonus(4), 1e-9);
        assertEquals(0.03, PlayerAttributes.reachBonus(3.03), 1e-9);
        assertEquals(12, PlayerAttributes.reachBonus(15), 1e-9);
        assertEquals(12, PlayerAttributes.reachBonus(19.25), 1e-9);
        assertEquals(-3, PlayerAttributes.reachBonus(-1), 1e-9);
    }
}
