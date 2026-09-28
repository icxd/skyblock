package net.icxd.dungeons.leveling;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;
import net.icxd.dungeons.menu.Icon;
import net.icxd.dungeons.skill.Skill;

/**
 * What SkyBlock Leveling is made of: the XP tasks by category and what each is worth, every level's rewards but
 * its stats, the emblems and the SkyBlock Guide's stages. It's Hypixel's (the wiki's tasks, emblems and guide, and a
 * recording of Hypixel's own menus for the categories and rewards), so it lives in the private data as
 * {@code leveling/leveling.json}, made by tools/leveling/build_leveling.py (its README has the format); without it
 * there's no SkyBlock XP, as without items.json there are no items. No Bukkit in it but materials.
 */
public final class LevelingData {
    /** The folder under the plugin's data folder (servermgr links the private data's in). */
    public static final String FOLDER = "leveling";
    public static final String FILE = "leveling.json";
    static final int FORMAT = 1;

    /** A menu item's look: its material, and a head's skin (null for none). */
    public record Look(Material material, String texture) {
        public Icon icon(String name, List<String> lore) {
            return new Icon(material, name, lore, texture);
        }
    }

    /**
     * One of a task's XP lines: "Level 1-10" and 5 (for levels 1 to 10), "Per 5 Fairy Souls" and 10, with what the
     * wiki says under it ("(Caps at 50 runs!)"). {@code xp} is -1 for a line with no number ("See Museum/Milestones");
     * {@code from} and {@code to} are 0 for a line that isn't for levels.
     */
    public record Step(String text, int xp, int from, int to, String note) {
        boolean forLevel(int level) {
            return from > 0 && level >= from && level <= to;
        }
    }

    /**
     * An XP task (Skill Level Up) or a part of one (Complete Dungeons' Complete Catacombs Entrance): the most XP it
     * gives, its XP lines and parts, and for a dungeon completion its floor.
     */
    public record Task(String id, String name, Look look, String description, List<Step> steps, int max, DungeonFloor floor,
                       List<Task> tasks) {
        public Task {
            steps = List.copyOf(steps);
            tasks = List.copyOf(tasks);
        }

        /** XP this task gives for reaching a level (its line for that level's), 0 for none. */
        int xpFor(int level) {
            for (Step step : steps) if (step.forLevel(level) && step.xp() > 0) return step.xp();
            return 0;
        }

        /** Its first line's XP: what one more of it gives (a collection milestone, a completion); 0 for none. */
        int xpEach() {
            for (Step step : steps) if (step.xp() > 0) return step.xp();
            return 0;
        }
    }

    /** A category of Ways to Level Up as Hypixel shows it: its item, and the XP its tasks are worth together. */
    public record Category(String id, String name, Look look, List<String> description, int max, List<Task> tasks) {
        public Category {
            description = List.copyOf(description);
            tasks = List.copyOf(tasks);
        }
    }

    /** What a level's reward is: a game feature, a prefix colour, an emblem or a Book of Progression bonus. */
    public enum RewardKind { FEATURE, PREFIX, EMBLEM, BONUS }

    /**
     * A level's reward other than its stats, as the Leveling Rewards menus show it.
     *
     * @param lore  lines under "Level N" (a feature's none)
     * @param text  paragraphs the menus wrap (a bonus's, with "{bonus}" for its size at the viewer's level)
     * @param color a prefix colour's code ('e' for yellow); 0 for other kinds
     * @param emblem an emblem reward's emblem id; null for other kinds
     */
    public record Reward(RewardKind kind, int level, String name, Look look, List<String> lore, List<String> text, char color,
                         String emblem) {
        public Reward {
            lore = List.copyOf(lore);
            text = List.copyOf(text);
        }
    }

    /**
     * What completes a guide task or unlocks an emblem, of what the plugin keeps; anything else (a Slayer level, a
     * Museum donation) is null: never done here.
     */
    public sealed interface Unlock {
    }

    public record SkillLevel(Skill skill, int level) implements Unlock {
    }

    public record CatacombsLevel(int level) implements Unlock {
    }

    public record ClassLevel(DungeonClass dungeonClass, int level) implements Unlock {
    }

    /** The mean of the five classes' levels. */
    public record ClassAverage(int level) implements Unlock {
    }

    /** At least one completion of a floor. */
    public record FloorCompleted(DungeonFloor floor) implements Unlock {
    }

    public record LevelReached(int level) implements Unlock {
    }

    /** A collection's tier, by the collection's id in Hypixel's API ("CARROT_ITEM"). */
    public record CollectionTier(String collection, int tier) implements Unlock {
    }

    /** An emblem: its symbol with its colour ("&7♦"), what unlocks it as the wiki writes it, and what the plugin can tell of that. */
    public record Emblem(String id, String category, String name, String symbol, String requirement, Unlock unlock) {
    }

    /** A category of the Emblems menu, as recorded, with the wiki's emblems. */
    public record EmblemCategory(String id, String name, Look look, List<String> description, List<Emblem> emblems) {
        public EmblemCategory {
            description = List.copyOf(description);
            emblems = List.copyOf(emblems);
        }
    }

    /** One of a guide task's listed parts ("Farming Skill IV"). */
    public record GuideItem(String text, Unlock unlock) {
    }

    /**
     * One of a guide stage's tasks ("Skills": ten skill levels): its name in its colours, the Ways to Level Up task
     * it's part of, its description (as recorded, where {@code lines} has it; else the wiki's, to wrap), the XP
     * it's worth, how many parts it has, its parts where known (listed on its item or not) or what completes a
     * one-part task, and a one-part task's progress ("LVL 1", to {@code max}; null for none shown).
     */
    public record GuideTask(String name, String task, String description, List<String> lines, int xp, Look look, int count,
                            boolean listed, List<GuideItem> items, String progress, int max, Unlock unlock) {
        public GuideTask {
            lines = List.copyOf(lines);
            items = List.copyOf(items);
        }
    }

    /**
     * A stage of the SkyBlock Guide: its name in its colour ("&aStarter"), its subtitle ("&8New Player"), its
     * description (recorded lines, else the wiki's to wrap), how many tasks it has in all, and its tasks.
     */
    public record Stage(String id, String name, String subtitle, List<String> lines, String description, int count,
                        List<GuideTask> tasks) {
        public Stage {
            lines = List.copyOf(lines);
            tasks = List.copyOf(tasks);
        }
    }

    private static final LevelingData EMPTY = new LevelingData(List.of(), List.of(), List.of(), List.of(), List.of());

    private final List<Category> categories;
    private final List<Reward> rewards;
    private final List<EmblemCategory> emblemCategories;
    private final List<Stage> stages;
    private final List<String> problems;
    private final Map<String, Task> tasks = new LinkedHashMap<>();
    private final Map<String, Emblem> emblems = new LinkedHashMap<>();
    private final Map<RewardKind, List<Reward>> byKind = new EnumMap<>(RewardKind.class);
    private final int maxXp;

    LevelingData(List<Category> categories, List<Reward> rewards, List<EmblemCategory> emblemCategories, List<Stage> stages,
                 List<String> problems) {
        this.categories = List.copyOf(categories);
        List<Reward> sorted = new ArrayList<>(rewards);
        // By level, then in the Leveling Rewards menu's order of kinds (UNKNOWN on Hypixel for a level with several).
        sorted.sort((a, b) -> a.level() != b.level() ? Integer.compare(a.level(), b.level()) : a.kind().compareTo(b.kind()));
        this.rewards = List.copyOf(sorted);
        this.emblemCategories = List.copyOf(emblemCategories);
        this.stages = List.copyOf(stages);
        this.problems = List.copyOf(problems);
        int max = 0;
        for (Category category : this.categories) {
            max += category.max();
            index(category.tasks());
        }
        this.maxXp = max;
        for (EmblemCategory category : this.emblemCategories) for (Emblem emblem : category.emblems()) emblems.putIfAbsent(emblem.id(), emblem);
        for (RewardKind kind : RewardKind.values()) byKind.put(kind, new ArrayList<>());
        for (Reward reward : this.rewards) byKind.get(reward.kind()).add(reward);
        byKind.replaceAll((k, v) -> List.copyOf(v));
    }

    private void index(List<Task> list) {
        for (Task task : list) {
            tasks.putIfAbsent(task.id(), task);
            index(task.tasks());
        }
    }

    public static LevelingData empty() {
        return EMPTY;
    }

    public List<Category> categories() {
        return categories;
    }

    /** A task (or a part of one) by its id ("skill_level_up", "complete_dungeons.complete_the_catacombs"); null for none. */
    public Task task(String id) {
        return tasks.get(id);
    }

    /** The category a top-level task is in; null for none. */
    public Category categoryOf(String taskId) {
        for (Category category : categories) for (Task task : category.tasks()) if (task.id().equals(taskId)) return category;
        return null;
    }

    public Category category(String id) {
        for (Category category : categories) if (category.id().equals(id)) return category;
        return null;
    }

    /** Every level's rewards but their stats, by level. */
    public List<Reward> rewards() {
        return rewards;
    }

    public List<Reward> rewards(RewardKind kind) {
        return byKind.get(kind);
    }

    /** The rewards of one level but its stats, in the menus' order. */
    public List<Reward> rewards(int level) {
        List<Reward> out = new ArrayList<>();
        for (Reward reward : rewards) if (reward.level() == level) out.add(reward);
        return out;
    }

    public List<EmblemCategory> emblemCategories() {
        return emblemCategories;
    }

    public Emblem emblem(String id) {
        return id == null ? null : emblems.get(id);
    }

    public List<Stage> stages() {
        return stages;
    }

    /** What all the tasks are worth: the categories' XP together. */
    public int maxXp() {
        return maxXp;
    }

    /** What couldn't be read, and why. */
    public List<String> problems() {
        return problems;
    }

    // Reading it

    /** Reads {@code leveling/leveling.json} under the data folder; with problems (and nothing) when it isn't there or can't be read. */
    public static LevelingData load(Path root) {
        Path file = root.resolve(FOLDER).resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return new LevelingData(List.of(), List.of(), List.of(), List.of(), List.of("no " + file + ", so there's no SkyBlock XP"));
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new LevelingData(List.of(), List.of(), List.of(), List.of(), List.of(file + ": " + e));
        }
    }

    /** The data from its JSON (format 1); what can't be read is left out and listed in {@link #problems()}. */
    public static LevelingData parse(JsonObject json) {
        List<String> problems = new ArrayList<>();
        int format = json.has("format") ? json.get("format").getAsInt() : 0;
        if (format != FORMAT) problems.add("format " + format + ", not " + FORMAT + ": read as far as it goes");
        List<Category> categories = new ArrayList<>();
        for (JsonElement e : array(json, "categories")) {
            try {
                categories.add(category(e.getAsJsonObject(), problems));
            } catch (RuntimeException ex) {
                problems.add("a category: " + ex);
            }
        }
        List<Reward> rewards = new ArrayList<>();
        for (JsonElement e : array(json, "rewards")) {
            try {
                rewards.add(reward(e.getAsJsonObject()));
            } catch (RuntimeException ex) {
                problems.add("a reward: " + ex);
            }
        }
        List<EmblemCategory> emblems = new ArrayList<>();
        for (JsonElement e : array(json, "emblems")) {
            try {
                emblems.add(emblemCategory(e.getAsJsonObject(), problems));
            } catch (RuntimeException ex) {
                problems.add("an emblem category: " + ex);
            }
        }
        List<Stage> stages = new ArrayList<>();
        for (JsonElement e : array(json, "guide")) {
            try {
                stages.add(stage(e.getAsJsonObject(), problems));
            } catch (RuntimeException ex) {
                problems.add("a guide stage: " + ex);
            }
        }
        return new LevelingData(categories, rewards, emblems, stages, problems);
    }

    private static Category category(JsonObject o, List<String> problems) {
        List<Task> tasks = new ArrayList<>();
        for (JsonElement e : array(o, "tasks")) tasks.add(task(e.getAsJsonObject(), problems));
        return new Category(o.get("id").getAsString(), o.get("name").getAsString(), look(o.getAsJsonObject("icon")), strings(o, "description"),
                integer(o, "max", 0), tasks);
    }

    private static Task task(JsonObject o, List<String> problems) {
        List<Step> steps = new ArrayList<>();
        for (JsonElement e : array(o, "xp")) {
            JsonObject s = e.getAsJsonObject();
            steps.add(new Step(string(s, "text", ""), integer(s, "xp", -1), integer(s, "from", 0), integer(s, "to", 0), string(s, "note", null)));
        }
        List<Task> parts = new ArrayList<>();
        for (JsonElement e : array(o, "tasks")) parts.add(task(e.getAsJsonObject(), problems));
        DungeonFloor floor = null;
        if (o.has("floor")) {
            floor = parse(DungeonFloor.class, o.get("floor").getAsString());
            if (floor == null) problems.add(o.get("id").getAsString() + ": no floor " + o.get("floor").getAsString());
        }
        return new Task(o.get("id").getAsString(), o.get("name").getAsString(), look(o.getAsJsonObject("icon")), string(o, "description", null),
                steps, integer(o, "max", 0), floor, parts);
    }

    private static Reward reward(JsonObject o) {
        String color = string(o, "color", null);
        return new Reward(RewardKind.valueOf(o.get("kind").getAsString()), o.get("level").getAsInt(), o.get("name").getAsString(),
                look(o.getAsJsonObject("icon")), strings(o, "lore"), strings(o, "text"), color == null || color.isEmpty() ? 0 : color.charAt(0),
                string(o, "emblem", null));
    }

    private static EmblemCategory emblemCategory(JsonObject o, List<String> problems) {
        String id = o.get("id").getAsString();
        List<Emblem> emblems = new ArrayList<>();
        for (JsonElement e : array(o, "emblems")) {
            JsonObject m = e.getAsJsonObject();
            emblems.add(new Emblem(m.get("id").getAsString(), id, m.get("name").getAsString(), m.get("symbol").getAsString(),
                    string(m, "requirement", ""), unlock(m.getAsJsonObject("unlock"), problems)));
        }
        return new EmblemCategory(id, o.get("name").getAsString(), look(o.getAsJsonObject("icon")), strings(o, "description"), emblems);
    }

    private static Stage stage(JsonObject o, List<String> problems) {
        List<GuideTask> tasks = new ArrayList<>();
        for (JsonElement e : array(o, "groups")) {
            JsonObject g = e.getAsJsonObject();
            List<GuideItem> items = new ArrayList<>();
            for (JsonElement i : array(g, "items")) {
                JsonObject item = i.getAsJsonObject();
                items.add(new GuideItem(item.get("text").getAsString(), unlock(item.getAsJsonObject("unlock"), problems)));
            }
            tasks.add(new GuideTask(g.get("name").getAsString(), string(g, "task", ""), string(g, "description", ""), strings(g, "lines"),
                    integer(g, "xp", 0), look(g.getAsJsonObject("icon")), integer(g, "count", Math.max(1, items.size())),
                    g.has("listed") && g.get("listed").getAsBoolean(), items, string(g, "progress", null), integer(g, "max", 1),
                    unlock(g.getAsJsonObject("unlock"), problems)));
        }
        String id = o.get("stage").getAsString();
        int count = 0;
        for (GuideTask task : tasks) count += task.count();
        return new Stage(id, string(o, "name", "&a" + id), string(o, "subtitle", ""), strings(o, "lines"), string(o, "description", ""),
                integer(o, "tasks", count), tasks);
    }

    /** {"skill": "MINING", "level": 50}, {"catacombs": 40}, ...; null for none, or one the plugin has no name for. */
    static Unlock unlock(JsonObject o, List<String> problems) {
        if (o == null) return null;
        Unlock unlock = null;
        if (o.has("skill")) {
            Skill skill = parse(Skill.class, o.get("skill").getAsString());
            if (skill != null) unlock = new SkillLevel(skill, integer(o, "level", 0));
        } else if (o.has("catacombs")) {
            unlock = new CatacombsLevel(o.get("catacombs").getAsInt());
        } else if (o.has("class")) {
            DungeonClass dungeonClass = parse(DungeonClass.class, o.get("class").getAsString());
            if (dungeonClass != null) unlock = new ClassLevel(dungeonClass, integer(o, "level", 0));
        } else if (o.has("classAverage")) {
            unlock = new ClassAverage(o.get("classAverage").getAsInt());
        } else if (o.has("floor")) {
            DungeonFloor floor = parse(DungeonFloor.class, o.get("floor").getAsString());
            if (floor != null) unlock = new FloorCompleted(floor);
        } else if (o.has("level")) {
            unlock = new LevelReached(o.get("level").getAsInt());
        } else if (o.has("collection")) {
            unlock = new CollectionTier(o.get("collection").getAsString(), integer(o, "tier", 0));
        }
        if (unlock == null) problems.add("an unlock the plugin doesn't know: " + o);
        return unlock;
    }

    private static Look look(JsonObject o) {
        if (o == null) return new Look(Material.PAPER, null);
        Material material = Material.matchMaterial(string(o, "material", "PAPER"));
        return new Look(material == null ? Material.PAPER : material, string(o, "texture", null));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name) {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : array(o, key)) out.add(e.getAsString());
        return Collections.unmodifiableList(out);
    }

    private static String string(JsonObject o, String key, String fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject o, String key, int fallback) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : fallback;
    }
}
