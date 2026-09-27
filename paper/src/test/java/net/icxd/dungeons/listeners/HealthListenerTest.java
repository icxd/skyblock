package net.icxd.dungeons.listeners;

import org.bukkit.event.player.PlayerRespawnEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthListenerTest {
    /** "Upon respawning, 50% of max Mana is returned" (the wiki's Mana). */
    @Test
    void halfTheManaBack() {
        assertEquals(50, HealthListener.respawnMana(100));
        assertEquals(656, HealthListener.respawnMana(1312));
        assertEquals(470, HealthListener.respawnMana(941));
        assertEquals(0, HealthListener.respawnMana(-1));
    }

    /** A death's respawn (or a plugin's, which only brings back the dead), not the End's exit portal. */
    @Test
    void onlyAfterDeath() {
        assertTrue(HealthListener.afterDeath(PlayerRespawnEvent.RespawnReason.DEATH));
        assertTrue(HealthListener.afterDeath(PlayerRespawnEvent.RespawnReason.PLUGIN));
        assertFalse(HealthListener.afterDeath(PlayerRespawnEvent.RespawnReason.END_PORTAL));
    }
}
