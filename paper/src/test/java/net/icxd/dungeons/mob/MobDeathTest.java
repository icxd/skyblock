package net.icxd.dungeons.mob;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What a death tells the rest of the plugin, and which drops are announced. */
class MobDeathTest {
    @Test
    void event() {
        SpawnOptions options = SpawnOptions.NONE.starred(true).modifier(Modifier.SPEEDY).roomMultiplier(1.05);
        DataMob mob = new DataMob(MobKinds.CRYPT_DREADLORD, MobKinds.CRYPT_DREADLORD.firstVariant(), options);
        Location at = new Location(null, 1.5, 69, -3.5);
        SkyBlockMobDeathEvent event = new SkyBlockMobDeathEvent(null, mob, at);
        at.setX(100);
        assertNull(event.killer());
        assertSame(MobKinds.CRYPT_DREADLORD, event.kind());
        assertEquals(61, event.variant().combatXp());
        assertEquals(1, event.variant().coins());
        assertTrue(event.starred());
        assertEquals(Modifier.SPEEDY, event.modifier());
        assertEquals(1.5, event.location().getX());
        assertEquals(14_700, event.mob().getMaxHealth());
    }

    /** Dungeon drops go to the inventory silently unless they're rare (the recorded 5% armor pieces had no chat line). */
    @Test
    void announced() {
        DataMob grunt = Mobs.get("ZOMBIE_GRUNT");
        assertTrue(grunt.dropsToInventory());
        assertFalse(Mobs.announced(grunt, MobDropType.OCCASIONAL));
        assertFalse(Mobs.announced(grunt, MobDropType.COMMON));
        assertTrue(Mobs.announced(grunt, MobDropType.VERY_RARE));
        DataMob cube = Mobs.get("MAGMA_CUBE");
        assertFalse(cube.dropsToInventory());
        assertTrue(Mobs.announced(cube, MobDropType.COMMON));
    }
}
