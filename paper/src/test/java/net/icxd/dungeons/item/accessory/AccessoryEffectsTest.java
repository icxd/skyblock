package net.icxd.dungeons.item.accessory;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.mob.MobType;
import org.bukkit.Location;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What accessories do beyond their stats: made-up items with the ids the rules know, and their own numbers. */
class AccessoryEffectsTest {
    private static final double EPSILON = 1e-9;

    @BeforeEach
    void items(@TempDir Path folder) throws IOException {
        Path file = folder.resolve("items.json");
        Files.writeString(file, """
                {"format":1,"items":{\
                "SKELETON_TALISMAN":{"lore":["&7Reduces the damage taken from &f🦴","&fSkeletal &7mobs by &a7%&7."],"material":"PLAYER_HEAD",\
                "name":"Test","type":"ACCESSORY"},\
                "NETHER_ARTIFACT":{"lore":["&7Reduces the damage taken from &4♨","&4Infernal &7mobs by &a8%&7.","",\
                "&7While in the test isle, you will receive","&7the damage reduction from all mobs."],"material":"PLAYER_HEAD",\
                "name":"Test","type":"ACCESSORY"},\
                "BURSTSTOPPER_TALISMAN":{"lore":["&7If an incoming hit would deal at least","&c40% &7of your &c❤&7, multiply its damage",\
                "&7by &a0.8&7."],"material":"PLAYER_HEAD","name":"Test","type":"ACCESSORY"},\
                "HASTE_ARTIFACT":{"lore":["&7Grants permanent &eHaste III&7."],"material":"PLAYER_HEAD","name":"Test","type":"ACCESSORY"},\
                "INTIMIDATION_RING":{"lore":["&7Monsters at or below Level &a7 &7will no","&7longer target you."],"material":"PLAYER_HEAD",\
                "name":"Test","type":"ACCESSORY"},\
                "DAY_CRYSTAL":{"lore":["&7More &c❁ Strength &7and &a❈","&aDefense&7 by &a+3&7 during the Day."],"material":"PLAYER_HEAD",\
                "name":"Test","type":"ACCESSORY"},\
                "IQ_POINT":{"lore":["&7Increases your total &b✎ Intelligence","&7by &a3%&7."],"material":"PLAYER_HEAD","name":"Test",\
                "type":"ACCESSORY"},\
                "CATACOMBS_EXPERT_RING":{"lore":["&7Increases test experience by &a+12%&7."],"material":"PLAYER_HEAD","name":"Test",\
                "type":"ACCESSORY"}}}""");
        ItemRegistry.loadData(file);
        AccessoryText.clear();
    }

    @AfterEach
    void forget() {
        AccessoryText.clear();
    }

    /** "Reduces the damage taken from X mobs by N%": the type and the factor; the Nether Artifact's all mobs on the Crimson Isle. */
    @Test
    void reductions() {
        AccessoryEffects.Reduction skeletal = AccessoryEffects.reduction(AccessoryText.plain("SKELETON_TALISMAN"));
        assertNotNull(skeletal);
        assertEquals(MobType.SKELETAL, skeletal.type());
        assertEquals(0.93, skeletal.factor(), EPSILON);
        AccessoryEffects.Reduction nether = AccessoryEffects.reduction(AccessoryText.plain("NETHER_ARTIFACT"));
        assertTrue(nether.allOnCrimsonIsle());
        assertNull(AccessoryEffects.reduction("Take 5% less damage from Tests."));
        List<AccessoryEffects.Reduction> both = List.of(skeletal, nether);
        assertEquals(0.93, AccessoryEffects.takenFrom(both, Set.of(MobType.SKELETAL, MobType.UNDEAD), false), EPSILON);
        assertEquals(0.93 * 0.92, AccessoryEffects.takenFrom(both, Set.of(MobType.SKELETAL), true), EPSILON);
        assertEquals(1, AccessoryEffects.takenFrom(both, Set.of(), true), EPSILON);
    }

    /** What a set of counted accessories comes to. */
    @Test
    void summary() {
        AccessoryEffects.Summary s = AccessoryEffects.summarize(Set.of("SKELETON_TALISMAN", "HASTE_ARTIFACT", "INTIMIDATION_RING", "DAY_CRYSTAL",
                "IQ_POINT", "BURSTSTOPPER_TALISMAN"));
        assertEquals(1, s.reductions.size());
        assertEquals(3, s.haste);
        assertEquals(7, s.intimidation, EPSILON);
        assertEquals(List.of("DAY_CRYSTAL"), s.day);
        assertEquals(3, s.iqPercent, EPSILON);
        // The Burststopper's: a hit of at least 40% of their health now, times 0.8.
        assertEquals(300, AccessoryEffects.burststopper(s, 300, 1_000), EPSILON);
        assertEquals(320, AccessoryEffects.burststopper(s, 400, 1_000), EPSILON);
        assertEquals(-1, AccessoryEffects.summarize(Set.of()).intimidation, EPSILON);
    }

    /** The Blood God Crest: a point a digit of its counter, at most 7. */
    @Test
    void bloodGod() {
        assertEquals(0, AccessoryEffects.bloodGod(0, 1, 7), EPSILON);
        assertEquals(1, AccessoryEffects.bloodGod(9, 1, 7), EPSILON);
        assertEquals(2, AccessoryEffects.bloodGod(10, 1, 7), EPSILON);
        assertEquals(10, AccessoryEffects.bloodGod(12_345, 2, 7), EPSILON);
        assertEquals(14, AccessoryEffects.bloodGod(1e12, 2, 7), EPSILON);
        assertEquals("&7Counter: &c12,345", CrestCounter.counter(List.of("&7Test.", "&7Counter: &cNone yet!"), 12_345).get(1));
    }

    /** The Reaper Orb counts the kills in its window. */
    @Test
    void reaperOrb() {
        Deque<Long> kills = new ArrayDeque<>(List.of(0L, 3_000L, 4_000L));
        assertEquals(3, AccessoryEffects.recent(kills, 5_000, 5_000));
        assertEquals(2, AccessoryEffects.recent(kills, 6_000, 5_000));
        assertEquals(0, AccessoryEffects.recent(kills, 20_000, 5_000));
    }

    /** The Gravity Talisman: +10 at the spawn, a point less every 10 blocks, never below +1. */
    @Test
    void gravity() {
        Location spawn = new Location(null, 0, 70, 0);
        assertEquals(10, AccessoryEffects.gravity(new Location(null, 3, 70, 4), spawn), EPSILON);
        assertEquals(7, AccessoryEffects.gravity(new Location(null, 30, 90, 0), spawn), EPSILON);
        assertEquals(1, AccessoryEffects.gravity(new Location(null, 5_000, 70, 0), spawn), EPSILON);
    }

    /** The Bucket of Dye's drops: Dyes only. */
    @Test
    void dyes() {
        assertTrue(AccessoryEffects.dye(List.of("DYE_BONE")));
        assertTrue(!AccessoryEffects.dye(List.of("DYE_BONE", "BONE")) && !AccessoryEffects.dye(List.of()));
    }

    @Test
    void numerals() {
        assertEquals(2, AccessoryEffects.haste("Grants permanent Haste II."));
        assertEquals(0, AccessoryEffects.haste("Nothing."));
        assertEquals(12, AccessoryText.after("CATACOMBS_EXPERT_RING", "by", 0), EPSILON);
    }
}
