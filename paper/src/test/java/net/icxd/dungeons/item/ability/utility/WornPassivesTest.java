package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Worn pieces' abilities that don't hit: their rules. */
class WornPassivesTest {
    private static final double EPS = 1e-9;

    /** A hit spread over 3 seconds: a third now, then what waits an even share each second. */
    @Test
    void alignedSpreadsAHitOverItsSeconds() {
        assertEquals(300, CellsAlignment.now(900, 3), EPS);
        assertEquals(300, CellsAlignment.due(600, 2), EPS);
        assertEquals(300, CellsAlignment.due(300, 1), EPS);
        assertEquals(50, CellsAlignment.due(50, 0), EPS);
    }

    @Test
    void jumpsGrowToTheirMost() {
        assertEquals(Movement.MOON_BASE, Movement.moonJump(0), EPS);
        assertEquals(Movement.MOON_BASE + Movement.MOON_PER_SECOND, Movement.moonJump(1), EPS);
        assertEquals(Movement.MOON_MOST, Movement.moonJump(60), EPS);
        assertEquals(1, Movement.bounce(10), EPS);
        assertEquals(Movement.BOUNCY_MOST, Movement.bounce(100), EPS);
    }
}
