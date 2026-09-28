package net.icxd.dungeons.session;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Mana spent: what a cost takes, which the spent listeners hear. */
class ManaTest {
    @Test
    void taken() {
        assertEquals(60, Mana.taken(1_000, 60));
        // Never more than they have (Jingle Bells' half of their pool, with less than that left).
        assertEquals(40, Mana.taken(40, 500));
        assertEquals(0, Mana.taken(0, 500));
        assertEquals(0, Mana.taken(-1, 500));
        assertEquals(0, Mana.taken(1_000, 0));
        assertEquals(0, Mana.taken(1_000, -5));
    }
}
