package net.icxd.dungeons.item.ability.abilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Where a shortbow's arrows go. */
class InstantlyShootsTest {
    @Test
    void terminatorSideArrowsAreFiveDegreesOut() {
        assertEquals(-5f, InstantlyShoots.sideAngle(0, 3));
        assertEquals(0f, InstantlyShoots.sideAngle(1, 3));
        assertEquals(5f, InstantlyShoots.sideAngle(2, 3));
    }

    @Test
    void oneArrowGoesWhereYouLook() {
        assertEquals(0f, InstantlyShoots.sideAngle(0, 1));
    }
}
