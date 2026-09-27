package net.icxd.dungeons.mob;

import net.icxd.dungeons.common.DungeonFloor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Dungeon mobs' name tags against the recorded ones (research mobs.md 1.6, mob_spawns_recorded's first
 * tags), with the mob type glyphs as their classic symbols (Timid has none).
 */
class NameTagsTest {
    private static String tag(MobKind kind, SpawnOptions options, double health) {
        return new DataMob(kind, kind.firstVariant(), options).nameTag(health);
    }

    private static String full(MobKind kind, SpawnOptions options) {
        DataMob mob = new DataMob(kind, kind.variant(DungeonFloor.ENTRANCE, options.level()), options);
        return mob.nameTag(mob.getMaxHealth());
    }

    private static SpawnOptions room(double multiplier) {
        return SpawnOptions.NONE.roomMultiplier(multiplier);
    }

    @Test
    void plainAndStarred() {
        // &2<U+E084> &cZombie Grunt &a7,000&c❤ and, starred in a room opened at x1.1, &2<U+E084> &6✯ &cZombie Grunt &a7,700&c❤
        assertEquals("&2༕ &cZombie Grunt &a7,000&c❤", full(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE));
        assertEquals("&2༕ &6✯ &cZombie Grunt &a7,700&c❤", full(MobKinds.ZOMBIE_GRUNT, room(1.1).starred(true)));
        // Two types: &2<U+E084>&6<U+E083> &6✯ &cCrypt Lurker &a10,800&c❤
        assertEquals("&2༕&6⸕ &6✯ &cCrypt Lurker &a10,800&c❤", full(MobKinds.CRYPT_LURKER, room(1.2).starred(true)));
        // Skeletal and Timid, which has no symbol: &f<U+E081>&e<U+E088> &cScared Skeleton &a12,000&c❤
        assertEquals("&f🦴 &cScared Skeleton &a12,000&c❤", full(MobKinds.SCARED_SKELETON, SpawnOptions.NONE));
        assertEquals("&f🦴 &6✯ &cSkeleton Grunt &a14,700&c❤", full(MobKinds.SKELETON_GRUNT, room(1.05).starred(true)));
    }

    @Test
    void modifiers() {
        // &2<U+E084>&6<U+E083> &c&2Healthy Crypt Lurker &a15,120&c❤
        assertEquals("&2༕&6⸕ &c&2Healthy Crypt Lurker &a15,120&c❤", full(MobKinds.CRYPT_LURKER, room(1.05).modifier(Modifier.HEALTHY)));
        // &2<U+E084> &6✯ &c&3Speedy Tank Zombie &b1,320&c❤
        assertEquals("&2༕ &6✯ &c&3Speedy Tank Zombie &b1,320&c❤", full(MobKinds.TANK_ZOMBIE, room(1.1).starred(true).modifier(Modifier.SPEEDY)));
        // &2<U+E084>&8<U+E085>&6<U+E083> &6✯ &c&8Fortified Crypt Souleater &713,000&c❤
        assertEquals("&2༕&8☠&6⸕ &6✯ &c&8Fortified Crypt Souleater &713,000&c❤",
                full(MobKinds.CRYPT_SOULEATER, SpawnOptions.NONE.starred(true).modifier(Modifier.FORTIFIED)));
        // &2<U+E084>&8<U+E085>&6<U+E083> &c&6Flaming Crypt Dreadlord &e14,700&c❤
        assertEquals("&2༕&8☠&6⸕ &c&6Flaming Crypt Dreadlord &e14,700&c❤", full(MobKinds.CRYPT_DREADLORD, room(1.05).modifier(Modifier.FLAMING)));
        assertEquals("&2༕ &c&1Stormy Zombie Grunt &97,000&c❤", full(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE.modifier(Modifier.STORMY)));
    }

    @Test
    void health() {
        // Partly hurt stays green (as recorded: &a6,653); rounded up; yellow at 0: &2<U+E084>&6<U+E083> &cCrypt Undead &e0&c❤
        assertEquals("&2༕ &cZombie Grunt &a6,653&c❤", tag(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE, 6_652.2));
        assertEquals("&2༕ &cZombie Grunt &a1&c❤", tag(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE, 0.3));
        assertEquals("&2༕&6⸕ &cCrypt Undead &e0&c❤", tag(MobKinds.CRYPT_UNDEAD, SpawnOptions.NONE, -40));
        assertEquals("&2༕ &c&3Speedy Zombie Grunt &e0&c❤", tag(MobKinds.ZOMBIE_GRUNT, SpawnOptions.NONE.modifier(Modifier.SPEEDY), 0));
    }

    /** &8[&7Lv40&8] &2<U+E084>&f<U+E081> &cUndead Skeleton &a25,000&f/&a25,000&c❤, and &e0&f/&a25,000 dead. */
    @Test
    void leveled() {
        assertEquals("&8[&7Lv40&8] &2༕&f🦴 &cUndead Skeleton &a25,000&f/&a25,000&c❤", full(MobKinds.UNDEAD_SKELETON, SpawnOptions.NONE));
        assertEquals("&8[&7Lv40&8] &2༕&f🦴 &cUndead Skeleton &e0&f/&a25,000&c❤", tag(MobKinds.UNDEAD_SKELETON, SpawnOptions.NONE, 0));
    }

    /**
     * &e<U+E07B>&5<U+E073> &6✯ &c&d&lLost Adventurer &a130k&c❤ (Lv90; 182k once its room opened at x1.4) and
     * &e<U+E07B>&6<U+E083> &6✯ &c&d&lAngry Archaeologist &a12,750&c❤ (Lv80 at x1.5).
     */
    @Test
    void minibosses() {
        assertEquals("&e✰&5♃ &6✯ &c&d&lLost Adventurer &a130k&c❤", full(MobKinds.LOST_ADVENTURER, SpawnOptions.NONE.starred(true).level(90)));
        assertEquals("&e✰&5♃ &6✯ &c&d&lLost Adventurer &a182k&c❤", full(MobKinds.LOST_ADVENTURER, room(1.4).starred(true).level(90)));
        assertEquals("&e✰&6⸕ &6✯ &c&d&lAngry Archaeologist &a12,750&c❤", full(MobKinds.ANGRY_ARCHAEOLOGIST, room(1.5).starred(true)));
        assertEquals("&e✰&5♃ &c&d&lLost Adventurer &a40,000&c❤", full(MobKinds.LOST_ADVENTURER, SpawnOptions.NONE));
    }
}
