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

    /** One of the Watcher's undeads, which Mobs doesn't spawn: the same event, with its kind's numbers. */
    @Test
    void watcherUndead() {
        MobKind.Variant variant = MobKinds.WATCHER_UNDEAD.firstVariant();
        SkyBlockMobDeathEvent event = new SkyBlockMobDeathEvent(null, MobKinds.WATCHER_UNDEAD, variant, false, null, new Location(null, 0, 70, 0));
        assertSame(MobKinds.WATCHER_UNDEAD, event.kind());
        assertEquals(75, event.variant().combatXp());
        assertFalse(event.starred());
        assertNull(event.modifier());
        assertNull(event.mob());
    }

    /** Dungeon drops go to the inventory silently unless they're rare (the recorded 5% armor pieces had no chat line). */
    @Test
    void announced() {
        DataMob grunt = MobsTest.mob("ZOMBIE_GRUNT");
        assertTrue(grunt.dropsToInventory());
        assertFalse(Mobs.announced(grunt, MobDropType.OCCASIONAL));
        assertFalse(Mobs.announced(grunt, MobDropType.COMMON));
        assertTrue(Mobs.announced(grunt, MobDropType.VERY_RARE));
        DataMob cube = MobsTest.mob("MAGMA_CUBE");
        assertFalse(cube.dropsToInventory());
        assertTrue(Mobs.announced(cube, MobDropType.COMMON));
    }

    /** SkyHanni's recorded dungeon line: "§6§lRARE DROP! §r§9Beating Heart §r§b(...)", gold whatever the drop's tier. */
    @Test
    void dropMessage() {
        assertEquals("§6§lRARE DROP! §9Beating Heart §b(+0% ✯ Magic Find)", Mobs.dropMessage(MobDropType.VERY_RARE, "§9", "Beating Heart", 0));
        assertEquals("§6§lRARE DROP! §5Diamond Atom §b(+25% ✯ Magic Find)", Mobs.dropMessage(MobDropType.RARE, "§5", "Diamond Atom", 25));
        assertEquals("§d§lCRAZY RARE DROP! §6Test §b(+0% ✯ Magic Find)", Mobs.dropMessage(MobDropType.CRAZY_RARE, "§6", "Test", 0));
        assertEquals("§c§lINSANE DROP! §6Test §b(+0% ✯ Magic Find)", Mobs.dropMessage(MobDropType.RNGESUS_INCARNATE, "§6", "Test", 0));
    }
}
