package net.icxd.dungeons.session;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Vitality as a pool and Mending on outgoing heals (the wiki's Vitality and Mending, 0.26.1's Healing Revamp). */
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

    /** Healing others is times Mending / 100: the base 100 changes nothing. */
    @Test
    void mending() {
        assertEquals(320, PlayerHealth.outgoing(320, 100), 1e-12);
        assertEquals(480, PlayerHealth.outgoing(320, 150), 1e-12);
        assertEquals(64, PlayerHealth.outgoing(320, 20), 1e-12);
        assertEquals(0, PlayerHealth.outgoing(-5, 150), 1e-12);
        assertEquals(0, PlayerHealth.outgoing(320, -40), 1e-12);
    }

    /** The action bar shows Vitality from the first use of an item that costs it on, and the profile keeps that. */
    @Test
    void shownAfterFirstUse() {
        Document profile = new Document();
        assertFalse(Vitality.shown(profile));
        Vitality.markShown(profile);
        assertTrue(Vitality.shown(profile));
        assertEquals(true, profile.getBoolean(Vitality.SHOWN));
        Vitality.markShown(profile);
        assertTrue(Vitality.shown(profile));
        assertFalse(Vitality.shown((Document) null));
    }
}
