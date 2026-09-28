package net.icxd.dungeons.leveling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.DungeonRecords;
import net.icxd.dungeons.profile.ProfileMode;
import net.icxd.dungeons.skill.Skill;

/**
 * What the leveling tests count with: a small made-up data file (its numbers aren't Hypixel's), the private data
 * where it's there (-Dleveling.dir, the folder with leveling.json; else the data checkout next to this repository),
 * and profiles with given levels and completions.
 */
final class Fixtures {
    /** Made up: two categories, a skill task (1 XP a level to 10, then 2), Catacombs, classes, two floors, collections. */
    static final String DATA = """
            {"format": 1,
             "categories": [
              {"id": "core", "name": "&3Core Tasks", "icon": {"material": "NETHER_STAR"}, "description": ["&7Core."], "max": 150,
               "tasks": [
                {"id": "skill_level_up", "name": "Skill Level Up", "icon": {"material": "DIAMOND_SWORD"}, "max": 100,
                 "description": "Level up your skills!",
                 "xp": [{"text": "Level 1-10", "xp": 1, "from": 1, "to": 10}, {"text": "Level 11-60", "xp": 2, "from": 11, "to": 60}]},
                {"id": "collections", "name": "Collections", "icon": {"material": "PAINTING"}, "max": 50,
                 "xp": [{"text": "Per Collection Milestone", "xp": 5}]}]},
              {"id": "dungeon", "name": "&3Dungeon Tasks", "icon": {"material": "PLAYER_HEAD", "texture": "abc"}, "description": ["&7Dungeons."],
               "max": 190,
               "tasks": [
                {"id": "catacombs_level_up", "name": "Catacombs Level Up", "icon": {"material": "PLAYER_HEAD"}, "max": 100,
                 "xp": [{"text": "Level 1-5", "xp": 3, "from": 1, "to": 5}, {"text": "Level 6-50", "xp": 6, "from": 6, "to": 50}]},
                {"id": "class_level_up", "name": "Class Level Up", "icon": {"material": "PLAYER_HEAD"}, "max": 50,
                 "xp": [{"text": "Level 1-50", "xp": 1, "from": 1, "to": 50}]},
                {"id": "complete_dungeons", "name": "Complete Dungeons", "icon": {"material": "PLAYER_HEAD"}, "max": 40,
                 "tasks": [{"id": "complete_dungeons.catacombs", "name": "Complete The Catacombs", "icon": {"material": "PLAYER_HEAD"}, "max": 40,
                  "tasks": [
                   {"id": "complete_dungeons.catacombs.entrance", "name": "Complete Catacombs Entrance", "icon": {"material": "PLAYER_HEAD"},
                    "max": 15, "floor": "ENTRANCE", "xp": [{"text": "", "xp": 15}]},
                   {"id": "complete_dungeons.catacombs.floor_i", "name": "Complete Catacombs Floor I", "icon": {"material": "PLAYER_HEAD"},
                    "max": 25, "floor": "FLOOR_1", "xp": [{"text": "", "xp": 25}]}]}]}]}],
             "rewards": [
              {"kind": "FEATURE", "level": 2, "name": "&bAccess to Something", "icon": {"material": "PLAYER_HEAD", "texture": "def"}},
              {"kind": "EMBLEM", "level": 3, "name": "&fDot Emblem &7•", "icon": {"material": "NAME_TAG"}, "emblem": "dot"},
              {"kind": "BONUS", "level": 3, "name": "&9Some Bonus", "icon": {"material": "BOOK"},
               "text": ["&7When hit, take &9{bonus}% &7less."]},
              {"kind": "PREFIX", "level": 40, "name": "&fWhite Level Prefix", "icon": {"material": "BONE_MEAL"}, "color": "f"}],
             "emblems": [
              {"id": "leveling", "name": "&aLeveling", "icon": {"material": "NAME_TAG"}, "description": ["&7By level."],
               "emblems": [{"id": "dot", "name": "Dot", "symbol": "&7•", "requirement": "SkyBlock Level 3", "unlock": {"level": 3}},
                           {"id": "swords", "name": "Swords", "symbol": "&7⚔", "requirement": "Catacombs X", "unlock": {"catacombs": 10}},
                           {"id": "brain", "name": "Brain", "symbol": "&7ௐ", "requirement": "A Slayer"}]}],
             "guide": [
              {"stage": "starter", "name": "&aStarter", "subtitle": "&8New Player", "lines": ["&7Start here."], "tasks": 4,
               "groups": [
                {"name": "&aSkills", "task": "skill_level_up", "lines": ["&7Level up."], "xp": 20, "icon": {"material": "DIAMOND_SWORD"},
                 "count": 2, "listed": true,
                 "items": [{"text": "Farming Skill II", "unlock": {"skill": "FARMING", "level": 2}},
                           {"text": "Mining Skill II", "unlock": {"skill": "MINING", "level": 2}}]},
                {"name": "&aMuseum", "task": "museum", "lines": ["&7Donate."], "xp": 5, "icon": {"material": "GOLD_BLOCK"}, "count": 1,
                 "progress": "Tier I", "max": 3},
                {"name": "&aEntrance", "task": "complete_dungeons", "lines": ["&7Complete it."], "xp": 5, "icon": {"material": "PLAYER_HEAD"},
                 "count": 1, "unlock": {"floor": "ENTRANCE"}}]},
              {"stage": "amateur", "name": "&bAmateur", "subtitle": "&8Early", "description": "&7Then here.", "tasks": 1,
               "groups": [{"name": "&bFloor I", "task": "complete_dungeons", "description": "Complete Floor I.", "xp": 5,
                           "icon": {"material": "PLAYER_HEAD"}, "count": 1, "unlock": {"floor": "FLOOR_1"}}]}]}
            """;

    private Fixtures() {
    }

    static LevelingData fixture() {
        LevelingData data = LevelingData.parse(JsonParser.parseString(DATA).getAsJsonObject());
        assertEquals(List.of(), data.problems());
        return data;
    }

    /** The private data's folder: -Dleveling.dir, else the data checkout's. */
    static Path privateFolder() {
        String property = System.getProperty("leveling.dir");
        if (property != null) return Path.of(property);
        Path repository = Path.of(System.getProperty("basedir", ".")).toAbsolutePath().normalize().getParent();
        return repository.resolveSibling("skyblock-dungeon-data").resolve(LevelingData.FOLDER);
    }

    /** The private data, or the test is skipped without it. */
    static LevelingData recorded() {
        Path folder = privateFolder();
        assumeTrue(Files.isRegularFile(folder.resolve(LevelingData.FILE)), "no " + folder.resolve(LevelingData.FILE));
        LevelingData data = LevelingData.load(folder.getParent());
        assertEquals(List.of(), data.problems());
        return data;
    }

    /** A profile with these skill levels, Catacombs and class levels, and one completion of each of these floors. */
    static Document profile(Map<Skill, Integer> skills, int catacombs, Map<DungeonClass, Integer> classes, DungeonFloor... floors) {
        Document profile = new Document();
        Document xp = new Document();
        for (Map.Entry<Skill, Integer> e : skills.entrySet()) xp.put(e.getKey().key(), e.getKey().cumulative(e.getValue()));
        profile.put("skills", xp);
        DungeonProfile.addCatacombsXp(profile, DungeonLevels.cumulative(catacombs));
        for (Map.Entry<DungeonClass, Integer> e : classes.entrySet()) DungeonProfile.addClassXp(profile, e.getKey(), DungeonLevels.cumulative(e.getValue()));
        for (DungeonFloor floor : floors) DungeonRecords.complete(profile, floor, 300, "S", 600_000, java.time.LocalDate.of(2026, 9, 28));
        return profile;
    }

    static final LevelingSources NONE = new LevelingSources() {
    };

    /** What the menus show of a profile with this data. */
    static LevelingView view(LevelingData data, Document profile, LevelingSources sources) {
        return new LevelingView(data, sources, profile, SkyBlockXp.of(profile, data, sources), ProfileMode.NORMAL, Rank.MVP_PLUS, "ICoding", true);
    }

    /** The menus at a given XP, whatever it came from (as the recorded profiles, whose tasks the plugin can't all count). */
    static LevelingView viewAt(LevelingData data, int xp) {
        return new LevelingView(data, NONE, new Document(), new SkyBlockXp.Breakdown(Map.of(), xp), ProfileMode.NORMAL, Rank.MVP_PLUS,
                "ICoding", true);
    }
}
