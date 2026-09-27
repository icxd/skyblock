package net.icxd.dungeons.mob;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A room's mobs spawn at base health and get the room's multiplier when it opens (research mobs.md 1.3). */
class RoomMultiplierTest {
    private static DataMob mob(MobKind kind, Modifier modifier) {
        return new DataMob(kind, kind.firstVariant(), SpawnOptions.NONE.modifier(modifier));
    }

    @Test
    void openingScalesHealthAndDamage() {
        DataMob grunt = mob(MobKinds.ZOMBIE_GRUNT, null);
        assertEquals(7_000, grunt.getMaxHealth());
        assertEquals(1, grunt.roomMultiplier());
        grunt.roomMultiplier(1.05);
        assertEquals(7_350, grunt.getMaxHealth());
        assertEquals(201 * 1.05, grunt.getDamage(), 1e-9);
        assertEquals(1.05, grunt.roomMultiplier());
        // Healthy Crypt Lurker in the first room: 9,000 x 1.6 x 1.05.
        DataMob lurker = mob(MobKinds.CRYPT_LURKER, Modifier.HEALTHY);
        lurker.roomMultiplier(1.05);
        assertEquals(15_120, lurker.getMaxHealth());
        // The Lost Adventurer in the Dragon room, x1.4: 130k to 182k.
        DataMob adventurer = new DataMob(MobKinds.LOST_ADVENTURER, MobKinds.LOST_ADVENTURER.variant(net.icxd.dungeons.common.DungeonFloor.ENTRANCE, 90),
                SpawnOptions.NONE.starred(true));
        adventurer.roomMultiplier(1.4);
        assertEquals(182_000, adventurer.getMaxHealth());
    }

    /** Undead Skeletons and Crypt Undead are never scaled. */
    @Test
    void notEveryKind() {
        DataMob skeleton = mob(MobKinds.UNDEAD_SKELETON, null);
        skeleton.roomMultiplier(1.5);
        assertEquals(25_000, skeleton.getMaxHealth());
        DataMob undead = mob(MobKinds.CRYPT_UNDEAD, null);
        undead.roomMultiplier(1.5);
        assertEquals(22_500, undead.getMaxHealth());
    }
}
