package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.fairysouls.FairySoulTable;
import net.icxd.dungeons.fairysouls.FairySouls;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.storage.BagUpgrades;

/** A profile's SkyBlock XP from what it has done, its level and the level's colour. */
class SkyBlockXpTest {
    /** Complete Objectives: the SkyBlock XP the quests' objectives gave (Saving Up's Banker, +5), named as the action bar had it. */
    @Test
    void objectivesCount() {
        LevelingData data = LevelingData.parse(JsonParser.parseString("""
                {"format": 1, "categories": [{"id": "core", "name": "&3Core Tasks", "icon": {"material": "NETHER_STAR"}, "description": [],
                 "max": 300, "tasks": [{"id": "complete_objectives", "name": "Complete Objectives", "icon": {"material": "WRITABLE_BOOK"},
                  "max": 212, "xp": [{"text": "Per Objective Completed", "xp": 5}]}]}]}
                """).getAsJsonObject());
        assertEquals(List.of(), data.problems());
        Document profile = new Document();
        assertEquals(0, SkyBlockXp.of(profile, data, Fixtures.NONE).of("complete_objectives"));
        Quests.start(profile, Quests.SAVING_UP, 1000);
        Quests.complete(profile, Quests.SAVING_UP, Quests.TALK_TO_THE_BANKER, 2000);
        assertEquals(5, SkyBlockXp.of(profile, data, Fixtures.NONE).of("complete_objectives"));
        Quests.complete(profile, Quests.SAVING_UP, Quests.DEPOSIT_COINS, 3000);
        assertEquals(5, SkyBlockXp.of(profile, data, Fixtures.NONE).total());
        Task task = data.categories().getFirst().tasks().getFirst();
        assertEquals("Complete Objective", LevelingText.gainName(task));
        assertEquals("&b+5 SkyBlock XP &7(Complete Objective&7)&b (5/100)", LevelingText.actionBar(5, LevelingText.gainName(task), 5));
    }

    /**
     * The Accessory Bag (its most Accessory Power), Boss Collections (their tiers' own SkyBlock XP), Craft Minions
     * (each tier once, by its line) and Defeat Kuudra (each tier up to the highest beaten), on the recorded shapes.
     */
    @Test
    void bagBossesMinionsAndKuudra() {
        LevelingData data = LevelingData.parse(JsonParser.parseString("""
                {"format": 1, "categories": [{"id": "core", "name": "&3Core Tasks", "icon": {"material": "NETHER_STAR"}, "description": [],
                 "max": 9000, "tasks": [
                  {"id": "accessory_bag", "name": "Accessory Bag", "icon": {"material": "NETHER_STAR"}, "max": 2121,
                   "xp": [{"text": "Per &6Accessory Power&7", "xp": 1}]},
                  {"id": "craft_minions", "name": "Craft Minions", "icon": {"material": "NETHER_STAR"}, "max": 3164, "xp": [
                     {"text": "Tier I", "xp": 1},
                     {"text": "Tier II", "xp": 1},
                     {"text": "Tier III", "xp": 1},
                     {"text": "Tier IV", "xp": 1},
                     {"text": "Tier V", "xp": 1},
                     {"text": "Tier VI", "xp": 1},
                     {"text": "Tier VII", "xp": 2},
                     {"text": "Tier VIII", "xp": 3},
                     {"text": "Tier IX", "xp": 4},
                     {"text": "Tier X", "xp": 6},
                     {"text": "Tier XI", "xp": 12},
                     {"text": "Tier XII", "xp": 24}]}]},
                 {"id": "slaying", "name": "&3Slaying Tasks", "icon": {"material": "GOLDEN_SWORD"}, "description": [], "max": 2000, "tasks": [
                  {"id": "boss_collections", "name": "Boss Collections", "icon": {"material": "GOLDEN_SWORD"}, "max": 1015, "xp": [{"text": "Varies"}]},
                  {"id": "defeat_kuudra", "name": "Defeat Kuudra", "icon": {"material": "GOLDEN_SWORD"}, "max": 300, "tasks": [
                   {"id": "defeat_kuudra.basic_tier", "name": "Basic Tier", "icon": {"material": "GOLDEN_SWORD"}, "max": 20, "xp": [{"text": "", "xp": 20}]},
                   {"id": "defeat_kuudra.hot_tier", "name": "Hot Tier", "icon": {"material": "GOLDEN_SWORD"}, "max": 40, "xp": [{"text": "", "xp": 40}]},
                   {"id": "defeat_kuudra.burning_tier", "name": "Burning Tier", "icon": {"material": "GOLDEN_SWORD"}, "max": 60, "xp": [{"text": "", "xp": 60}]},
                   {"id": "defeat_kuudra.fiery_tier", "name": "Fiery Tier", "icon": {"material": "GOLDEN_SWORD"}, "max": 80, "xp": [{"text": "", "xp": 80}]},
                   {"id": "defeat_kuudra.infernal_tier", "name": "Infernal Tier", "icon": {"material": "GOLDEN_SWORD"}, "max": 100, "xp": [{"text": "", "xp": 100}]}]}]}]}
                """).getAsJsonObject());
        assertEquals(List.of(), data.problems());
        Document profile = new Document();
        profile.put("minions", new Document("craftedMinions", List.of("WHEAT_1", "WHEAT_2", "WHEAT_12", "COBBLESTONE_11", "WHEAT_1", "NOT_A_TIER", "WHEAT_0")));
        profile.put("crimsonIsle", new Document("kuudra", new Document("highest", 3)));
        LevelingSources sources = new LevelingSources() {
            @Override
            public int bossCollectionXp(Document p) {
                return 145;
            }

            @Override
            public int accessoryPower(Document p) {
                return 250;
            }
        };
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, sources);
        assertEquals(250, xp.of("accessory_bag"));
        // Tier I twice is one: 1 + 1 + 24 (XII) + 12 (XI).
        assertEquals(38, xp.of("craft_minions"));
        assertEquals(145, xp.of("boss_collections"));
        // Basic, Hot and Burning.
        assertEquals(120, xp.of("defeat_kuudra"));
        assertEquals(60, xp.of("defeat_kuudra.burning_tier"));
        assertEquals(0, xp.of("defeat_kuudra.fiery_tier"));
        assertEquals(250 + 38 + 145 + 120, xp.total());
        // Capped at what it's worth; nothing kept, nothing given.
        assertEquals(2121, SkyBlockXp.of(profile, data, new LevelingSources() {
            @Override
            public int accessoryPower(Document p) {
                return 5000;
            }
        }).of("accessory_bag"));
        assertEquals(0, SkyBlockXp.of(new Document(), data, Fixtures.NONE).total());
    }

    /** The recorded data's Accessory Bag, Craft Minions, Boss Collections and Defeat Kuudra. */
    @Test
    void recordedBagBossesMinionsKuudraAndReputation() {
        LevelingData data = Fixtures.recorded();
        assertEquals(2121, data.task("accessory_bag").max());
        assertEquals(3164, data.task("craft_minions").max());
        assertEquals(1015, data.task("boss_collections").max());
        assertEquals(300, data.task("defeat_kuudra").max());
        // One minion's twelve tiers: 1 x 6 + 2 + 3 + 4 + 6 + 12 + 24.
        List<String> wheat = new ArrayList<>();
        for (int tier = 1; tier <= 12; tier++) wheat.add("WHEAT_" + tier);
        Document profile = new Document("minions", new Document("craftedMinions", wheat));
        assertEquals(57, SkyBlockXp.of(profile, data, Fixtures.NONE).of("craft_minions"));
        profile.put("crimsonIsle", new Document("kuudra", new Document("highest", 5)));
        assertEquals(300, SkyBlockXp.of(profile, data, Fixtures.NONE).of("defeat_kuudra"));

        // Either faction's titles: Mage Friendly and Trusted (5 + 10) at 3,500; Barbarian Hero, every title.
        assertEquals(75, data.task("mage_reputation").max());
        profile.put("crimsonIsle", new Document("factions", new Document("mage", new Document("reputation", 3500))
                .append("barbarian", new Document("reputation", 12000))));
        SkyBlockXp.Breakdown xp = SkyBlockXp.of(profile, data, Fixtures.NONE);
        assertEquals(15, xp.of("mage_reputation"));
        assertEquals(10, xp.of("mage_reputation.trusted_status"));
        assertEquals(0, xp.of("mage_reputation.honored_status"));
        assertEquals(75, xp.of("barbarian_reputation"));
        // Below Friendly (or hostile) is none.
        profile.put("crimsonIsle", new Document("factions", new Document("mage", new Document("reputation", 999))
                .append("barbarian", new Document("reputation", -3000))));
        xp = SkyBlockXp.of(profile, data, Fixtures.NONE);
        assertEquals(0, xp.of("mage_reputation") + xp.of("barbarian_reputation"));
    }

    /** "Per 5 Fairy Souls: +10", for each five given to Tia; found ones not given count for nothing yet. */
    @Test
    void fairySoulExchanges() {
        LevelingData data = Fixtures.recorded();
        assertEquals(570, data.task("fairy_souls").max());
        Document profile = new Document();
        for (int i = 0; i < 12; i++) FairySouls.find(profile, new FairySoulTable.Soul(FairySoulTable.HUB, i, 70, 0));
        assertEquals(0, SkyBlockXp.of(profile, data, Fixtures.NONE).of("fairy_souls"));
        FairySouls.exchange(profile);
        FairySouls.exchange(profile);
        assertEquals(20, SkyBlockXp.of(profile, data, Fixtures.NONE).of("fairy_souls"));
        FairySouls.yoinked(profile, 289, 57);
        assertEquals(570, SkyBlockXp.of(profile, data, Fixtures.NONE).of("fairy_souls"));
    }

    /** "Per Upgrade: +2", each of Jacobus's. */
    @Test
    void accessoryBagUpgrades() {
        LevelingData data = Fixtures.recorded();
        assertEquals(198, data.task("accessory_bag_upgrades").max());
        Document profile = new Document();
        BagUpgrades.bought(profile, 7);
        assertEquals(14, SkyBlockXp.of(profile, data, Fixtures.NONE).of("accessory_bag_upgrades"));
    }

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
