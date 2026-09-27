package net.icxd.dungeons.listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HealthListenerTest {
    /** "Upon respawning, 50% of max Mana is returned" (the wiki's Mana). */
    @Test
    void halfTheManaBack() {
        assertEquals(50, HealthListener.respawnMana(100));
        assertEquals(656, HealthListener.respawnMana(1312));
        assertEquals(470, HealthListener.respawnMana(941));
        assertEquals(0, HealthListener.respawnMana(-1));
    }
}
