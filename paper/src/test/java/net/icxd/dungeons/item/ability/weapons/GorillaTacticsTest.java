package net.icxd.dungeons.item.ability.weapons;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Tactical Insertion's burn. */
class GorillaTacticsTest {
    private static final double EPS = 1e-9;

    /** "The burn deals 10% of ALL damage you dealt within the 3s, spread over 6s." */
    @Test
    void aTenthOfWhatTheyDealtOverSixSeconds() {
        assertEquals(1_000, GorillaTactics.burnEachSecond(60_000, 0.1, 6), EPS);
        assertEquals(0, GorillaTactics.burnEachSecond(0, 0.1, 6), EPS);
        assertEquals(0, GorillaTactics.burnEachSecond(60_000, 0.1, 0), EPS);
    }
}
