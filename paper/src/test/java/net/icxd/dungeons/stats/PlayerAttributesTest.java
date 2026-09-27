package net.icxd.dungeons.stats;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** What stats do to a player's vanilla attributes. */
class PlayerAttributesTest {
    /** 100 Speed is vanilla's walk speed (0.2); at most 400 counts; in the Catacombs a third of what's above 100 goes. */
    @Test
    void speed() {
        assertEquals(100, PlayerAttributes.speed(100, 400, false), 1e-9);
        assertEquals(291, PlayerAttributes.speed(291, 400, false), 1e-9);
        assertEquals(400, PlayerAttributes.speed(520, 400, false), 1e-9);
        assertEquals(0, PlayerAttributes.speed(-30, 400, false), 1e-9);
        // The recorded run: 291 on the tab list ran at 12.85 blocks a second, 229% of vanilla's 5.612 sprint.
        assertEquals(227.33, PlayerAttributes.speed(291, 400, true), 0.005);
        assertEquals(300, PlayerAttributes.speed(400, 400, true), 1e-9);
        assertEquals(300, PlayerAttributes.speed(700, 400, true), 1e-9);
        assertEquals(80, PlayerAttributes.speed(80, 400, true), 1e-9);
        assertEquals(0.2f, PlayerAttributes.walkSpeed(100), 1e-6);
        assertEquals(0.8f, PlayerAttributes.walkSpeed(400), 1e-6);
        assertEquals(1f, PlayerAttributes.walkSpeed(650), 1e-6);
        assertEquals(0f, PlayerAttributes.walkSpeed(-5), 1e-6);
    }

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

    /** Each 2 Respiration is a second under water: the base 30 is vanilla's 300 ticks. */
    @Test
    void air() {
        assertEquals(300, PlayerAttributes.maxAir(30));
        assertEquals(310, PlayerAttributes.maxAir(31));
        assertEquals(305, PlayerAttributes.maxAir(30.5));
        assertEquals(0, PlayerAttributes.maxAir(-4));
    }
}
