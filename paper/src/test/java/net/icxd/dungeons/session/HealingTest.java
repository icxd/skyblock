package net.icxd.dungeons.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Vitality as a pool (the wiki's Vitality, 0.26.1's Healing Revamp). */
class HealingTest {
    /** The changelog's examples, and the stats menu's "5.2 Vitality per second" at 104. */
    @Test
    void vitalityRegen() {
        assertEquals(5, Vitality.regenPerSecond(100), 1e-12);
        assertEquals(6, Vitality.regenPerSecond(120), 1e-12);
        assertEquals(10, Vitality.regenPerSecond(200), 1e-12);
        assertEquals(5.2, Vitality.regenPerSecond(104), 1e-12);
        assertEquals(0, Vitality.regenPerSecond(-10), 1e-12);
    }
}
