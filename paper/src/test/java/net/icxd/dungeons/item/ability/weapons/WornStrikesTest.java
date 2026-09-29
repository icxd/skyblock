package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Worn pieces' abilities that hit: their rules. */
class WornStrikesTest {
    private static final double EPS = 1e-9;

    /** "10 damage plus 20% of your Strength". */
    @Test
    void spikyDealsTenAndAFifthOfStrength() {
        assertEquals(10, WornStrikes.spiky(10, 20, 0), EPS);
        assertEquals(110, WornStrikes.spiky(10, 20, 500), EPS);
    }

    /** "when dropping below 20% HP": only the hit that takes them from above it to below it, not one while they're already below. */
    @Test
    void detonateOnlyOnTheWayDown() {
        assertTrue(WornStrikes.droppedBelow(300, 150, 1_000, 0.2));
        assertFalse(WornStrikes.droppedBelow(190, 100, 1_000, 0.2));
        assertFalse(WornStrikes.droppedBelow(900, 300, 1_000, 0.2));
        assertFalse(WornStrikes.droppedBelow(300, 0, 1_000, 0.2), "not once they're dead");
    }

    /** The wiki's Additive Sources: 20 x Speed / 25 + 1, a whole 25 at a time (after halving). */
    @Test
    void bruteForceByWholeTwentyFives() {
        assertEquals(1, WornStrikes.bruteForce(24, 20, 25), EPS);
        assertEquals(81, WornStrikes.bruteForce(100, 20, 25), EPS);
        // The 400 Speed cap halved: 200, +161% (the wiki).
        assertEquals(161, WornStrikes.bruteForce(200, 20, 25), EPS);
    }
}
