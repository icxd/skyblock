package net.icxd.dungeons.mob;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Modifiers and the room multiplier against what the recordings show (research mobs.md 1.3 and 1.5). */
class ModifierTest {
    private static double health(MobKind kind, SpawnOptions options) {
        return DataMob.maxHealth(kind, kind.variant(net.icxd.dungeons.common.DungeonFloor.ENTRANCE, options.level()), options);
    }

    /** Healthy: x1.6 (Zombie Grunt 11,200, Crypt Souleater 20,800, Scared Skeleton 19,200, Tank 1,920, Dreadlord 22,400). */
    @Test
    void healthy() {
        SpawnOptions healthy = SpawnOptions.NONE.modifier(Modifier.HEALTHY);
        assertEquals(11_200, health(MobKinds.ZOMBIE_GRUNT, healthy));
        assertEquals(20_800, health(MobKinds.CRYPT_SOULEATER, healthy));
        assertEquals(19_200, health(MobKinds.SCARED_SKELETON, healthy));
        assertEquals(1_920, health(MobKinds.TANK_ZOMBIE, healthy));
        assertEquals(22_400, health(MobKinds.CRYPT_DREADLORD, healthy));
        // The others leave health alone (Fortified's recorded 9,000 Crypt Lurker, for one).
        for (Modifier modifier : Modifier.values()) {
            if (modifier != Modifier.HEALTHY) assertEquals(9_000, health(MobKinds.CRYPT_LURKER, SpawnOptions.NONE.modifier(modifier)));
        }
    }

    /** Speedy: +0.12 movement speed (Zombie Grunt 0.37 -> 0.49, Tank 0.24 -> 0.36, skeletons 0.25 -> 0.37). */
    @Test
    void speedy() {
        SpawnOptions speedy = SpawnOptions.NONE.modifier(Modifier.SPEEDY);
        assertEquals(0.49, new DataMob(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), speedy).speed(), 1e-9);
        assertEquals(0.36, new DataMob(MobKinds.TANK_ZOMBIE, MobKinds.TANK_ZOMBIE.firstVariant(), speedy).speed(), 1e-9);
        assertEquals(0.37, new DataMob(MobKinds.SKELETON_GRUNT, MobKinds.SKELETON_GRUNT.firstVariant(), speedy).speed(), 1e-9);
        assertEquals(0.37, new DataMob(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), SpawnOptions.NONE).speed(), 1e-9);
    }

    @Test
    void knockback() {
        DataMob fortified = new DataMob(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), SpawnOptions.NONE.modifier(Modifier.FORTIFIED));
        assertTrue(fortified.knockbackImmune());
        assertEquals(0, fortified.getDefense());
        DataMob tank = new DataMob(MobKinds.TANK_ZOMBIE, MobKinds.TANK_ZOMBIE.firstVariant(), SpawnOptions.NONE);
        assertTrue(tank.knockbackImmune());
        assertEquals(2, tank.getKnockback());
        assertFalse(new DataMob(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), SpawnOptions.NONE).knockbackImmune());
    }

    @Test
    void parse() {
        assertEquals(Modifier.SPEEDY, Modifier.parse("speedy"));
        assertEquals(Modifier.STORMY, Modifier.parse("Stormy"));
        assertNull(Modifier.parse("starred"));
        assertNull(Modifier.parse(null));
    }

    /** 49 of the 346 recorded tags had one, each as often as it was seen. */
    @Test
    void roll() {
        SplittableRandom random = new SplittableRandom(48);
        Map<Modifier, Integer> counts = new EnumMap<>(Modifier.class);
        int none = 0;
        int rolls = 200_000;
        for (int i = 0; i < rolls; i++) {
            Modifier modifier = Modifier.roll(random);
            if (modifier == null) none++;
            else counts.merge(modifier, 1, Integer::sum);
        }
        assertEquals(297 / 346.0, none / (double) rolls, 0.005);
        int modified = rolls - none;
        assertEquals(18 / 49.0, counts.get(Modifier.SPEEDY) / (double) modified, 0.02);
        assertEquals(1 / 49.0, counts.get(Modifier.STORMY) / (double) modified, 0.01);
    }

    /**
     * The room multiplier Phase 1 passes, on health and damage: every recorded opening (7,000 -> 7,350 at
     * x1.05, Crypt Lurker 9,900 at x1.1, Zombie Grunt 9,100 at x1.3, Tank 1,620 at x1.35, Lost Adventurer
     * 182,000 at x1.4, Crypt Lurker 13,500 and Angry Archaeologist 12,750 at x1.5).
     */
    @Test
    void roomMultiplier() {
        assertEquals(7_350, health(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE.roomMultiplier(1.05)));
        assertEquals(9_900, health(MobKinds.CRYPT_LURKER, SpawnOptions.NONE.roomMultiplier(1.1)));
        assertEquals(9_100, health(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE.roomMultiplier(1.3)));
        assertEquals(1_620, health(MobKinds.TANK_ZOMBIE, SpawnOptions.NONE.roomMultiplier(1.35)));
        assertEquals(182_000, health(MobKinds.LOST_ADVENTURER, SpawnOptions.NONE.roomMultiplier(1.4).level(90)));
        assertEquals(13_500, health(MobKinds.CRYPT_LURKER, SpawnOptions.NONE.roomMultiplier(1.5)));
        assertEquals(12_750, health(MobKinds.ANGRY_ARCHAEOLOGIST, SpawnOptions.NONE.roomMultiplier(1.5)));
        // With Healthy too: 9,000 x 1.6 x 1.05 = 15,120, as recorded.
        assertEquals(15_120, health(MobKinds.CRYPT_LURKER, SpawnOptions.NONE.roomMultiplier(1.05).modifier(Modifier.HEALTHY)));
        // Damage scales the same way.
        assertEquals(201 * 1.1, DataMob.damage(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), SpawnOptions.NONE.roomMultiplier(1.1)), 1e-9);
        assertEquals(201, DataMob.damage(MobKinds.ZOMBIE_GRUNT, MobKinds.ZOMBIE_GRUNT.firstVariant(), SpawnOptions.NONE), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> SpawnOptions.NONE.roomMultiplier(0));
        assertThrows(IllegalArgumentException.class, () -> SpawnOptions.NONE.roomMultiplier(Double.NaN));
    }

    /** Undead Skeletons (always 25,000) and Crypt Undead (22,500) aren't room-scaled, whatever the room passes (mobs.md 1.3). */
    @Test
    void notRoomScaled() {
        SpawnOptions room = SpawnOptions.NONE.roomMultiplier(1.5);
        assertEquals(25_000, health(MobKinds.UNDEAD_SKELETON, room));
        assertEquals(22_500, health(MobKinds.CRYPT_UNDEAD, room));
        assertEquals(720, DataMob.damage(MobKinds.UNDEAD_SKELETON, MobKinds.UNDEAD_SKELETON.firstVariant(), room), 1e-9);
        assertEquals(936, DataMob.damage(MobKinds.CRYPT_UNDEAD, MobKinds.CRYPT_UNDEAD.firstVariant(), room), 1e-9);
        for (MobKind kind : MobKinds.ENTRANCE_KINDS) {
            assertEquals(kind != MobKinds.UNDEAD_SKELETON && kind != MobKinds.CRYPT_UNDEAD, kind.roomScaled(), kind.id());
        }
    }
}
