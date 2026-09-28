package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;

/** A profile's SkyBlock XP from what it has done, its level and the level's colour. */
class SkyBlockXpTest {
    @Test
    void freshProfileHasNone() {
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(new Document(), Fixtures.fixture(), Fixtures.NONE);
        assertEquals(0, xp.total());
        assertEquals(0, xp.level());
        assertEquals(Map.of(), xp.tasks());
        assertEquals(SkyBlockXp.Breakdown.NONE, SkyBlockXp.of(null, Fixtures.fixture(), Fixtures.NONE));
        // Without the data, there's nothing to count.
        Document profile = Fixtures.profile(Map.of(Skill.COMBAT, 30), 20, Map.of(), DungeonFloor.ENTRANCE);
        assertEquals(0, SkyBlockXp.of(profile, LevelingData.empty(), Fixtures.NONE).total());
    }

    /** The made-up data's numbers: every task counted from the profile, and capped at what it's worth. */
    @Test
    void fromWhatTheProfileHasDone() {
        LevelingData data = Fixtures.fixture();
        Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        skills.put(Skill.COMBAT, 12);
        skills.put(Skill.FARMING, 3);
        // Cosmetic skills give none.
        skills.put(Skill.RUNECRAFTING, 20);
        skills.put(Skill.SOCIAL, 10);
        Document profile = Fixtures.profile(skills, 7, Map.of(DungeonClass.MAGE, 4, DungeonClass.TANK, 2), DungeonFloor.ENTRANCE);
        LevelingSources sources = new LevelingSources() {
            @Override
            public int collectionTiers(Document p) {
                return 3;
            }
        };
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, sources);
        // Combat: 10 x 1 + 2 x 2; Farming 3.
        assertEquals(17, xp.of("skill_level_up"));
        // 5 x 3 + 2 x 6.
        assertEquals(27, xp.of("catacombs_level_up"));
        assertEquals(6, xp.of("class_level_up"));
        // The Entrance's first completion, and its part.
        assertEquals(15, xp.of("complete_dungeons"));
        assertEquals(15, xp.of("complete_dungeons.catacombs.entrance"));
        assertEquals(0, xp.of("complete_dungeons.catacombs.floor_i"));
        assertEquals(15, xp.of("collections"));
        assertEquals(17 + 27 + 6 + 15 + 15, xp.total());
        assertEquals(0, xp.level());
        Category core = data.category("core");
        assertEquals(32, xp.of(core));

        // A second completion gives nothing more; another floor does.
        Document more = Fixtures.profile(skills, 7, Map.of(DungeonClass.MAGE, 4, DungeonClass.TANK, 2), DungeonFloor.ENTRANCE,
                DungeonFloor.ENTRANCE, DungeonFloor.FLOOR_1);
        assertEquals(40, SkyBlockXp.of(more, data, sources).of("complete_dungeons"));

        // No more than a task is worth: 60 levels of every skill would be far more than 100.
        Map<Skill, Integer> maxed = new EnumMap<>(Skill.class);
        for (Skill skill : Skill.values()) maxed.put(skill, skill.maxLevel());
        assertEquals(100, SkyBlockXp.of(Fixtures.profile(maxed, 0, Map.of()), data, Fixtures.NONE).of("skill_level_up"));
    }

    /** The recorded data against NEU's sblevels.json: its dungeon tasks, and a known profile's XP. */
    @Test
    void recordedDungeonTasks() {
        LevelingData data = Fixtures.recorded();
        assertEquals(1220, data.task("catacombs_level_up").max());
        assertEquals(1000, data.task("class_level_up").max());
        assertEquals(540, data.task("complete_dungeons").max());
        assertEquals(2760, data.category("dungeon").max());
        // Catacombs 24 (20 a level to 39), Mage 20 and Tank 10 (4 a level), and the Entrance to Floor V completed
        // (20 each, 30 from Floor V: sblevels.json's complete_catacombs), and Master Floor I (50).
        Document profile = Fixtures.profile(Map.of(), 24, Map.of(DungeonClass.MAGE, 20, DungeonClass.TANK, 10), DungeonFloor.ENTRANCE,
                DungeonFloor.FLOOR_1, DungeonFloor.FLOOR_2, DungeonFloor.FLOOR_3, DungeonFloor.FLOOR_4, DungeonFloor.FLOOR_5,
                DungeonFloor.MASTER_FLOOR_1);
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, Fixtures.NONE);
        assertEquals(480, xp.of("catacombs_level_up"));
        assertEquals(120, xp.of("class_level_up"));
        assertEquals(5 * 20 + 30 + 50, xp.of("complete_dungeons"));
        assertEquals(480 + 120 + 180, xp.of(data.category("dungeon")));
        // Past 39, 40 a level; past 50, none (cosmetic levels).
        assertEquals(39 * 20 + 11 * 40, SkyBlockXp.of(Fixtures.profile(Map.of(), 55, Map.of()), data, Fixtures.NONE).of("catacombs_level_up"));
    }

    @Test
    void recordedSkillLevels() {
        LevelingData data = Fixtures.recorded();
        // Combat 24: 10 levels of 5, 14 of 10 (the wiki's Skill Level Up); Runecrafting none.
        Document profile = Fixtures.profile(Map.of(Skill.COMBAT, 24, Skill.RUNECRAFTING, 10), 0, Map.of());
        assertEquals(190, SkyBlockXp.of(profile, data, Fixtures.NONE).total());
        // Level 51 to 60: 30 each.
        assertEquals(50 + 150 + 500 + 300, SkyBlockXp.of(Fixtures.profile(Map.of(Skill.MINING, 60), 0, Map.of()), data, Fixtures.NONE).total());
    }

    /**
     * The old SkyBlock XP ({@link Skills#skyBlockXp}, all User#getSkyBlockXp was) is the Skill Level Up task's part of
     * the new: nothing a profile had is lost.
     */
    @Test
    void oldSkyBlockXpIsTheSkillTask() {
        LevelingData data = Fixtures.recorded();
        Map<Skill, Integer> skills = new EnumMap<>(Skill.class);
        int level = 7;
        for (Skill skill : Skill.values()) skills.put(skill, Math.min(skill.maxLevel(), level += 4));
        Document profile = Fixtures.profile(skills, 12, Map.of(DungeonClass.BERSERK, 5));
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, Fixtures.NONE);
        assertEquals(Skills.skyBlockXp(profile), xp.of(SkyBlockXp.SKILLS));
        assertTrue(xp.total() > Skills.skyBlockXp(profile));
    }

    @Test
    void levels() {
        assertEquals(0, SkyBlockXp.level(0));
        assertEquals(0, SkyBlockXp.level(99));
        assertEquals(1, SkyBlockXp.level(100));
        assertEquals(88, SkyBlockXp.level(8834));
        assertEquals(34, SkyBlockXp.intoLevel(8834));
        assertEquals(0, SkyBlockXp.level(-5));
    }

    /** Every 40 levels a colour: the recorded Prefix Color rewards, the tab's "[88]" in yellow. */
    @Test
    void colours() {
        assertEquals("&7", SkyBlockXp.color(0));
        assertEquals("&7", SkyBlockXp.color(39));
        assertEquals("&f", SkyBlockXp.color(40));
        assertEquals("&e", SkyBlockXp.color(88));
        assertEquals("&a", SkyBlockXp.color(120));
        assertEquals("&4", SkyBlockXp.color(480));
        assertEquals("&4", SkyBlockXp.color(600));
        assertEquals("&8[&e88&8]", SkyBlockXp.bracket(88));
        assertEquals("&8[&70&8]", SkyBlockXp.bracket(0));
    }

    /** The recorded colours are the prefix rewards' own. */
    @Test
    void coloursAreThePrefixRewards() {
        LevelingData data = Fixtures.recorded();
        List<LevelingData.Reward> prefixes = data.rewards(LevelingData.RewardKind.PREFIX);
        assertEquals(12, prefixes.size());
        for (LevelingData.Reward prefix : prefixes) assertEquals("&" + prefix.color(), SkyBlockXp.color(prefix.level()), prefix.name());
    }

    /** What the plugin can tell of an unlock. */
    @Test
    void unlocks() {
        Document profile = Fixtures.profile(Map.of(Skill.MINING, 50), 40, Map.of(DungeonClass.ARCHER, 40), DungeonFloor.FLOOR_7);
        assertTrue(SkyBlockXp.done(new LevelingData.SkillLevel(Skill.MINING, 50), profile, 0, Fixtures.NONE));
        assertFalse(SkyBlockXp.done(new LevelingData.SkillLevel(Skill.MINING, 51), profile, 0, Fixtures.NONE));
        assertTrue(SkyBlockXp.done(new LevelingData.CatacombsLevel(40), profile, 0, Fixtures.NONE));
        assertTrue(SkyBlockXp.done(new LevelingData.ClassLevel(DungeonClass.ARCHER, 40), profile, 0, Fixtures.NONE));
        assertFalse(SkyBlockXp.done(new LevelingData.ClassAverage(30), profile, 0, Fixtures.NONE));
        assertTrue(SkyBlockXp.done(new LevelingData.FloorCompleted(DungeonFloor.FLOOR_7), profile, 0, Fixtures.NONE));
        assertFalse(SkyBlockXp.done(new LevelingData.FloorCompleted(DungeonFloor.MASTER_FLOOR_7), profile, 0, Fixtures.NONE));
        assertTrue(SkyBlockXp.done(new LevelingData.LevelReached(10), profile, 10, Fixtures.NONE));
        assertFalse(SkyBlockXp.done(new LevelingData.LevelReached(10), profile, 9, Fixtures.NONE));
        // Collections are none until their code is wired in.
        assertFalse(SkyBlockXp.done(new LevelingData.CollectionTier("CARROT_ITEM", 1), profile, 0, Fixtures.NONE));
        // What it can't tell is never done.
        assertFalse(SkyBlockXp.done(null, profile, 500, Fixtures.NONE));
        assertEquals(8.0, SkyBlockXp.classAverage(profile), 1e-9);
    }

    @Test
    void recordedData() {
        LevelingData data = Fixtures.recorded();
        // The recorded Ways to Level Up: nine categories, 117 tasks, 57,874 XP.
        assertEquals(9, data.categories().size());
        assertEquals(117, data.categories().stream().mapToInt(c -> c.tasks().size()).sum());
        assertEquals(57_874, data.maxXp());
        assertEquals(19_810, data.category("core").max());
        // 54 rewards, as the recorded Leveling Rewards counts them.
        assertEquals(54, data.rewards().size());
        Task floor = data.task("complete_dungeons.complete_the_catacombs.complete_catacombs_floor_v");
        assertEquals(DungeonFloor.FLOOR_5, floor.floor());
        assertEquals(7, data.stages().size());
        assertEquals(128, data.stages().getFirst().count());
    }
}
