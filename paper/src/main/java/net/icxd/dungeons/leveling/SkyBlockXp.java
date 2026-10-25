package net.icxd.dungeons.leveling;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

import net.icxd.dungeons.crimsonisle.factions.FactionTitle;
import net.icxd.dungeons.crimsonisle.factions.FactionType;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.dungeons.DungeonLevels;
import net.icxd.dungeons.dungeons.DungeonProfile;
import net.icxd.dungeons.dungeons.DungeonRecords;
import net.icxd.dungeons.fairysouls.FairySouls;
import net.icxd.dungeons.leveling.LevelingData.Category;
import net.icxd.dungeons.leveling.LevelingData.Task;
import net.icxd.dungeons.leveling.LevelingData.Unlock;
import net.icxd.dungeons.quest.Quests;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.utils.Utils;

/**
 * A profile's SkyBlock XP, worked out from what it has done, as Hypixel does it (the wiki's SkyBlock Levels: tasks
 * "can only be completed once"; the XP isn't a counter). Each task's XP comes from the leveling data's XP for it and
 * what the plugin keeps of the profile: its skill levels, its Catacombs and class levels, its floors completed (a
 * floor's first completion), its collection and boss collection tiers and its Accessory Power (from {@link
 * LevelingSources}), its Fairy Soul exchanges, the minion tiers it crafted, the Kuudra tiers it beat and its
 * factions' reputation. The other tasks (Museum, Slayer, Bestiary...) have no system here yet, so they give none. A
 * level is every 100 XP. No server needed.
 */
public final class SkyBlockXp {
    /** "The player gains 1 SkyBlock Level for every 100 SkyBlock XP they have" (the wiki's SkyBlock Levels). */
    public static final int PER_LEVEL = 100;
    /**
     * The level's colour in "[88]", from 0, 40, 80 and on every 40 levels: the recorded Prefix Color rewards (and
     * NEU's sblevel_colours), whose last is Dark Red at 480.
     */
    private static final String[] COLORS = {"&7", "&f", "&e", "&a", "&2", "&b", "&3", "&9", "&d", "&5", "&6", "&c", "&4"};
    /** Levels between two colours. */
    public static final int COLOR_EVERY = 40;

    // The tasks the plugin keeps what they're counted from, by their ids in the data.
    static final String SKILLS = "skill_level_up";
    static final String CATACOMBS = "catacombs_level_up";
    static final String CLASSES = "class_level_up";
    static final String DUNGEONS = "complete_dungeons";
    static final String COLLECTIONS = "collections";
    static final String OBJECTIVES = "complete_objectives";
    static final String FAIRY_SOULS = "fairy_souls";
    static final String ACCESSORY_BAG = "accessory_bag";
    static final String BOSS_COLLECTIONS = "boss_collections";
    static final String CRAFT_MINIONS = "craft_minions";
    static final String DEFEAT_KUUDRA = "defeat_kuudra";
    static final String MAGE_REPUTATION = "mage_reputation";
    static final String BARBARIAN_REPUTATION = "barbarian_reputation";
    /** Kuudra's tiers in the order they're unlocked, as the data has them (see {@link #kuudra}). */
    private static final int KUUDRA_TIERS = 5;

    private SkyBlockXp() {
    }

    /** What a profile's SkyBlock XP comes from: each task's (by id, parts too, only those that give any) and all of it. */
    public record Breakdown(Map<String, Integer> tasks, int total) {
        public static final Breakdown NONE = new Breakdown(Map.of(), 0);

        public Breakdown {
            tasks = Collections.unmodifiableMap(new LinkedHashMap<>(tasks));
        }

        /** A task's XP (0 for none). */
        public int of(String task) {
            return tasks.getOrDefault(task, 0);
        }

        /** A category's XP: its tasks' together. */
        public int of(Category category) {
            int xp = 0;
            for (Task task : category.tasks()) xp += of(task.id());
            return xp;
        }

        public int level() {
            return SkyBlockXp.level(total);
        }
    }

    /** A profile's SkyBlock XP by task. */
    public static Breakdown of(Document profile, LevelingData data, LevelingSources sources) {
        if (profile == null) return Breakdown.NONE;
        Map<String, Integer> tasks = new LinkedHashMap<>();
        for (Category category : data.categories()) {
            for (Task task : category.tasks()) {
                int xp = switch (task.id()) {
                    case SKILLS -> skills(profile, task);
                    case CATACOMBS -> levels(task, DungeonLevels.level(DungeonProfile.catacombsXp(profile)));
                    case CLASSES -> classes(profile, task);
                    case DUNGEONS -> completions(profile, task, tasks);
                    case COLLECTIONS -> sources.collectionTiers(profile) * task.xpEach();
                    case BOSS_COLLECTIONS -> sources.bossCollectionXp(profile);
                    // "Per 5 Fairy Souls": five given to Tia, each time.
                    case FAIRY_SOULS -> FairySouls.exchanges(profile) * task.xpEach();
                    case ACCESSORY_BAG -> sources.accessoryPower(profile) * task.xpEach();
                    case CRAFT_MINIONS -> minions(profile, task);
                    case DEFEAT_KUUDRA -> kuudra(profile, task, tasks);
                    case MAGE_REPUTATION -> reputation(profile, FactionType.MAGE, task, tasks);
                    case BARBARIAN_REPUTATION -> reputation(profile, FactionType.BARBARIAN, task, tasks);
                    case OBJECTIVES -> Quests.skyBlockXp(profile);
                    default -> 0;
                };
                // No more than the task is worth (a data file with no most for it caps nothing).
                if (task.max() > 0) xp = Math.min(xp, task.max());
                if (xp > 0) tasks.put(task.id(), xp);
            }
        }
        int total = 0;
        for (Category category : data.categories()) for (Task task : category.tasks()) total += tasks.getOrDefault(task.id(), 0);
        return new Breakdown(tasks, total);
    }

    /** Every level of the skills that aren't cosmetic (Runecrafting and Social give none, as their menus show). */
    private static int skills(Document profile, Task task) {
        int xp = 0;
        for (Skill skill : Skill.values()) if (!skill.cosmetic()) xp += levels(task, Skills.level(profile, skill));
        return xp;
    }

    private static int classes(Document profile, Task task) {
        int xp = 0;
        for (DungeonClass dungeonClass : DungeonClass.values()) xp += levels(task, DungeonLevels.level(DungeonProfile.classXp(profile, dungeonClass)));
        return xp;
    }

    /**
     * Each minion tier crafted once: its tier's line ("Tier XII", +24). The profile keeps them as the API's
     * {@code crafted_generators} do ("WHEAT_12": the minion, its tier).
     */
    static int minions(Document profile, Task task) {
        int xp = 0;
        for (String crafted : craftedMinions(profile)) {
            int split = crafted.lastIndexOf('_');
            int tier;
            try {
                tier = split < 0 ? 0 : Integer.parseInt(crafted.substring(split + 1));
            } catch (NumberFormatException e) {
                continue;
            }
            if (tier >= 1) xp += task.xpFor("Tier " + Utils.getRomanNumeral(tier));
        }
        return xp;
    }

    /** The minion tiers crafted on the profile ({@code minions.craftedMinions}), each once. */
    static Set<String> craftedMinions(Document profile) {
        Set<String> crafted = new LinkedHashSet<>();
        if (profile.get("minions") instanceof Document minions && minions.get("craftedMinions") instanceof List<?> list) {
            for (Object tier : list) if (tier instanceof String s) crafted.add(s);
        }
        return crafted;
    }

    /**
     * Each Kuudra tier defeated once: its part's XP (kept in {@code tasks} too, for the menus). A tier opens once the
     * one before it is beaten, so the highest beaten ({@code crimsonIsle.kuudra.highest}, 1 for Basic to 5 for
     * Infernal) means every one up to it.
     */
    private static int kuudra(Document profile, Task task, Map<String, Integer> tasks) {
        int highest = profile.get("crimsonIsle") instanceof Document isle && isle.get("kuudra") instanceof Document kuudra
                && kuudra.get("highest") instanceof Number n ? n.intValue() : 0;
        int xp = 0;
        for (int i = 0; i < task.tasks().size() && i < Math.min(highest, KUUDRA_TIERS); i++) {
            Task part = task.tasks().get(i);
            if (part.xpEach() > 0) tasks.put(part.id(), part.xpEach());
            xp += part.xpEach();
        }
        return xp;
    }

    /**
     * Each of a faction's titles reached: its part's XP ("Friendly Status" at 1,000 reputation to "Hero Status" at
     * 12,000, as FactionTitle has them), kept in {@code tasks} too. The profile keeps both factions' reputation
     * ({@code crimsonIsle.factions.mage.reputation}), whichever one it picked.
     */
    private static int reputation(Document profile, FactionType faction, Task task, Map<String, Integer> tasks) {
        int reputation = profile.get("crimsonIsle") instanceof Document isle && isle.get("factions") instanceof Document factions
                && factions.get(faction.name().toLowerCase(Locale.ROOT)) instanceof Document of && of.get("reputation") instanceof Number n ? n.intValue() : 0;
        int xp = 0;
        for (Task part : task.tasks()) {
            for (FactionTitle title : FactionTitle.values()) {
                if (!part.name().equals(title.getName() + " Status") || reputation < title.getReputation()) continue;
                if (part.xpEach() > 0) tasks.put(part.id(), part.xpEach());
                xp += part.xpEach();
            }
        }
        return xp;
    }

    /** XP for each level up to this one (levels the task has no line for, like Catacombs' cosmetic ones past 50, give none). */
    static int levels(Task task, int level) {
        int xp = 0;
        for (int l = 1; l <= level; l++) xp += task.xpFor(l);
        return xp;
    }

    /** Each floor completed once: its part's XP (kept in {@code tasks} too, for the menus), and all of them. */
    private static int completions(Document profile, Task task, Map<String, Integer> tasks) {
        int xp = 0;
        for (Task part : task.tasks()) {
            int own = part.floor() != null ? (DungeonRecords.completions(profile, part.floor()) > 0 ? part.xpEach() : 0)
                    : completions(profile, part, tasks);
            if (own > 0) tasks.put(part.id(), own);
            xp += own;
        }
        return xp;
    }

    // Levels

    /** The level this much XP is: every 100. */
    public static int level(int xp) {
        return Math.max(0, xp) / PER_LEVEL;
    }

    /** XP into the level it's at (34 of 8,834). */
    public static int intoLevel(int xp) {
        return Math.max(0, xp) % PER_LEVEL;
    }

    /** "&e" for 80 to 119. */
    public static String color(int level) {
        return COLORS[Math.max(0, Math.min(level / COLOR_EVERY, COLORS.length - 1))];
    }

    /** "&8[&e88&8]". */
    public static String bracket(int level) {
        return "&8[" + color(level) + level + "&8]";
    }

    // What's been done

    /** The mean of the five classes' levels (for the Class Master emblems). */
    public static double classAverage(Document profile) {
        int sum = 0;
        for (DungeonClass dungeonClass : DungeonClass.values()) sum += DungeonLevels.level(DungeonProfile.classXp(profile, dungeonClass));
        return (double) sum / DungeonClass.values().length;
    }

    /** Whether the profile (at this SkyBlock level) has done what an unlock asks; never for one the plugin can't tell (null). */
    public static boolean done(Unlock unlock, Document profile, int level, LevelingSources sources) {
        if (unlock == null || profile == null) return false;
        return switch (unlock) {
            case LevelingData.SkillLevel s -> Skills.level(profile, s.skill()) >= s.level();
            case LevelingData.CatacombsLevel c -> DungeonLevels.level(DungeonProfile.catacombsXp(profile)) >= c.level();
            case LevelingData.ClassLevel c -> DungeonLevels.level(DungeonProfile.classXp(profile, c.dungeonClass())) >= c.level();
            case LevelingData.ClassAverage a -> classAverage(profile) >= a.level();
            case LevelingData.FloorCompleted f -> DungeonRecords.completions(profile, f.floor()) > 0;
            case LevelingData.LevelReached l -> level >= l.level();
            case LevelingData.CollectionTier c -> sources.collectionTier(profile, c.collection()) >= c.tier();
        };
    }
}
