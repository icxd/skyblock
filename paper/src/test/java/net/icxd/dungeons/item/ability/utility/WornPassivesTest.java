package net.icxd.dungeons.item.ability.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Worn pieces' abilities that don't hit: their rules. */
class WornPassivesTest {
    private static final double EPS = 1e-9;

    @Test
    void jumpsGrowToTheirMost() {
        assertEquals(Movement.MOON_BASE, Movement.moonJump(0), EPS);
        assertEquals(Movement.MOON_BASE + Movement.MOON_PER_SECOND, Movement.moonJump(1), EPS);
        assertEquals(Movement.MOON_MOST, Movement.moonJump(60), EPS);
        assertEquals(1, Movement.bounce(10), EPS);
        assertEquals(Movement.BOUNCY_MOST, Movement.bounce(100), EPS);
    }
}
