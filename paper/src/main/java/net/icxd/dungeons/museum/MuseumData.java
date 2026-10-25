package net.icxd.dungeons.museum;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * What the Museum takes: {@code museum/museum.json} in the data checkout (format 1, made by tools/museum from a
 * recording of Hypixel's Museum menus and its items API). Its seven categories' entries in the menus' first order
 * (Ascending XP), each an item or an armor set with the SkyBlock XP it gives, what can be it (an item's starred
 * versions, each of a set's pieces) and the higher tier that counts as donating it; the Special Items' ids; the Museum
 * Milestones' rewards. Hypixel's data, so it stays in that private checkout. No server needed.
 */
public final class MuseumData {
    public static final String FOLDER = "museum";
    public static final String FILE = "museum.json";
    static final int FORMAT = 1;

    /** A category, as Your Museum lists them: its id (the API's, "COMBAT"), its name and its entries in order. */
    public record Category(String id, String name, List<Entry> entries) {
        public Category {
            entries = List.copyOf(entries);
        }
    }

    /**
     * An item or an armor set it takes.
     *
     * @param id          an item's id ("UNDEAD_SWORD"), a set's the API's ("MONSTER_RAIDER")
     * @param pieces      what it's made of, one list a piece of it, each with the item ids that can be that piece: an
     *                    item is one piece (its id, then its starred ones), a set one a piece it's worn as
     * @param parent      the entry whose donation counts as this one's (null for none)
     * @param parentLines the menu's lines saying so, wrapped as it wraps them (none without a parent)
     * @param category    its category's id
     */
    public record Entry(String id, String name, int xp, boolean set, List<List<String>> pieces, String parent,
                        List<String> parentLines, String category) {
        public Entry {
            pieces = pieces.stream().map(List::copyOf).toList();
            parentLines = List.copyOf(parentLines);
        }
    }

    /** One of the Museum Milestones: its number, the XP it takes and its reward lines ("&8+&b1% Bits Multiplier"). */
    public record Milestone(int number, int xp, List<String> rewards) {
        public Milestone {
            rewards = List.copyOf(rewards);
        }
    }

    private final List<Category> categories;
    private final List<String> special;
    private final List<Milestone> milestones;
    private final List<String> problems;
    private final Map<String, Entry> byId = new HashMap<>();
    private final Map<String, List<Entry>> byItem = new HashMap<>();
    private final int totalXp;
    private final int count;

    MuseumData(List<Category> categories, List<String> special, List<Milestone> milestones, List<String> problems) {
        this.categories = List.copyOf(categories);
        this.special = List.copyOf(special);
        this.milestones = List.copyOf(milestones);
        this.problems = List.copyOf(problems);
        int xp = 0;
        int entries = 0;
        for (Category category : this.categories) {
            for (Entry entry : category.entries()) {
                byId.put(entry.id(), entry);
                for (List<String> piece : entry.pieces()) {
                    for (String item : piece) {
                        List<Entry> of = byItem.computeIfAbsent(item, k -> new ArrayList<>());
                        if (!of.contains(entry)) of.add(entry);
                    }
                }
                xp += entry.xp();
                entries++;
            }
        }
        totalXp = xp;
        count = entries;
    }

    public static MuseumData empty() {
        return new MuseumData(List.of(), List.of(), List.of(), List.of());
    }

    /** Reads it from the plugin's data folder; what's wrong is in {@link #problems}. */
    public static MuseumData load(Path dataFolder) {
        Path file = dataFolder.resolve(FOLDER).resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return new MuseumData(List.of(), List.of(), List.of(), List.of("no " + file + ", so the Museum takes nothing"));
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new MuseumData(List.of(), List.of(), List.of(), List.of("couldn't read " + file + ": " + e));
        }
    }

    static MuseumData parse(JsonObject json) {
        List<String> problems = new ArrayList<>();
        int format = json.has("format") ? json.get("format").getAsInt() : -1;
        if (format != FORMAT) return new MuseumData(List.of(), List.of(), List.of(), List.of("format " + format + ", not " + FORMAT));
        // Read every entry first: a higher tier may be in a later category.
        Map<String, JsonObject> seen = new LinkedHashMap<>();
        List<Category> categories = new ArrayList<>();
        Map<String, List<JsonObject>> raw = new LinkedHashMap<>();
        Map<String, String> names = new LinkedHashMap<>();
        for (JsonElement element : array(json, "categories")) {
            JsonObject category = element.getAsJsonObject();
            String id = category.get("id").getAsString();
            names.put(id, category.get("name").getAsString());
            List<JsonObject> entries = new ArrayList<>();
            for (JsonElement e : array(category, "entries")) {
                JsonObject entry = e.getAsJsonObject();
                String entryId = entry.has("id") ? entry.get("id").getAsString() : null;
                if (entryId == null || seen.containsKey(entryId)) {
                    problems.add(names.get(id) + ": " + (entryId == null ? "an entry with no id" : entryId + " twice") + " (left out)");
                    continue;
                }
                seen.put(entryId, entry);
                entries.add(entry);
            }
            raw.put(id, entries);
        }
        for (Map.Entry<String, List<JsonObject>> category : raw.entrySet()) {
            List<Entry> entries = new ArrayList<>();
            for (JsonObject entry : category.getValue()) {
                try {
                    entries.add(entry(entry, category.getKey(), seen, problems));
                } catch (RuntimeException e) {
                    problems.add(names.get(category.getKey()) + ": " + entry.get("id").getAsString() + " left out (" + e.getMessage() + ")");
                }
            }
            categories.add(new Category(category.getKey(), names.get(category.getKey()), entries));
        }
        List<String> special = new ArrayList<>();
        for (JsonElement item : array(json, "special")) special.add(item.getAsString());
        List<Milestone> milestones = new ArrayList<>();
        for (JsonElement element : array(json, "milestones")) {
            JsonObject milestone = element.getAsJsonObject();
            milestones.add(new Milestone(milestone.get("milestone").getAsInt(), milestone.get("xp").getAsInt(), strings(milestone, "rewards")));
        }
        return new MuseumData(categories, special, milestones, problems);
    }

    private static Entry entry(JsonObject json, String category, Map<String, JsonObject> all, List<String> problems) {
        String id = json.get("id").getAsString();
        boolean set = json.has("set") && json.get("set").getAsBoolean();
        List<List<String>> pieces = new ArrayList<>();
        if (set) {
            for (JsonElement piece : array(json, "pieces")) {
                List<String> ids = new ArrayList<>();
                for (JsonElement item : piece.getAsJsonArray()) ids.add(item.getAsString());
                if (!ids.isEmpty()) pieces.add(ids);
            }
        } else {
            List<String> items = strings(json, "items");
            if (!items.isEmpty()) pieces.add(items);
        }
        if (pieces.isEmpty()) throw new IllegalArgumentException("nothing can be it");
        int xp = json.get("xp").getAsInt();
        if (xp < 0) throw new IllegalArgumentException("XP " + xp);
        String parent = json.has("parent") ? json.get("parent").getAsString() : null;
        if (parent != null && (!all.containsKey(parent) || parent.equals(id))) {
            problems.add(id + "'s higher tier " + parent + " isn't an entry (left off)");
            parent = null;
        }
        List<String> lines = parent == null ? List.of() : strings(json, "parent_lines");
        return new Entry(id, json.get("name").getAsString(), xp, set, pieces, parent, lines, category);
    }

    private static JsonArray array(JsonObject json, String key) {
        return json.has(key) && json.get(key).isJsonArray() ? json.getAsJsonArray(key) : new JsonArray();
    }

    private static List<String> strings(JsonObject json, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement element : array(json, key)) out.add(element.getAsString());
        return out;
    }

    public List<Category> categories() {
        return categories;
    }

    /** A category by its id; null for none. */
    public Category category(String id) {
        for (Category category : categories) if (category.id().equals(id)) return category;
        return null;
    }

    /** An entry by its id; null for none. */
    public Entry entry(String id) {
        return byId.get(id);
    }

    /** The entries an item can be donated as or towards, in the menus' order (a piece can be in two sets); none for none. */
    public List<Entry> entriesOf(String item) {
        return item == null ? List.of() : List.copyOf(byItem.getOrDefault(item, List.of()));
    }

    /** All the entries' XP: the most the Museum gives (3,646 as recorded). */
    public int totalXp() {
        return totalXp;
    }

    /** How many entries it has (636 as recorded). */
    public int count() {
        return count;
    }

    public List<String> special() {
        return special;
    }

    public List<Milestone> milestones() {
        return milestones;
    }

    public List<String> problems() {
        return problems;
    }
}
