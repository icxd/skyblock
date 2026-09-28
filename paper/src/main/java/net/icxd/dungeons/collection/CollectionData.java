package net.icxd.dungeons.collection;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Every collection there is, as {@code collections/collections.json} in the private data folder has them
 * (made by tools/collections/build_collections.py from Hypixel's collections API, NEU's recipes and the
 * wiki): the menu's categories in their order, each collection's tiers and what each tier gives, the boss
 * collections, and which items count toward which collection and by how much. Hypixel's data, so it stays
 * out of this repository. No Bukkit in it.
 */
public final class CollectionData {
    public static final String FOLDER = "collections";
    public static final String FILE = "collections.json";
    /** The boss collections' category (the Collections menu's Boss Collections). */
    public static final String BOSS = "BOSS";

    /** A category of the Collections menu ("Combat"), its collections in the menu's order. */
    public record Category(String id, String name, List<String> collections) {
        public Category {
            collections = List.copyOf(collections);
        }
    }

    /**
     * One collection, or a boss's. {@code item} is the item the menus show (the collection's own, or for the
     * Gemstone and Mushroom collections one of theirs); a boss has a head's {@code texture} instead (null for
     * none) and the dungeon {@code floor} whose completions count (0 for none).
     */
    public record Collection(String id, String name, String category, String item, String texture, int floor, List<Tier> tiers) {
        public Collection {
            tiers = List.copyOf(tiers);
        }

        public boolean boss() {
            return BOSS.equals(category);
        }

        public int maxTier() {
            return tiers.size();
        }

        /** Tier {@code n}, from 1. */
        public Tier tier(int n) {
            return tiers.get(n - 1);
        }
    }

    /** A tier: how many it takes (in all), and what reaching it gives, in the order the menus list it. */
    public record Tier(long amount, List<Reward> rewards) {
        public Tier {
            rewards = List.copyOf(rewards);
        }
    }

    /**
     * Something a tier gives. Which fields it has depends on its type: an item's id ({@code item}, null when
     * the plugin has no such item), a name, a skill, an amount, a percent, an essence, a line of text or a
     * head's texture.
     */
    public record Reward(Type type, String item, String name, String skill, long amount, int percent, String essence, String text,
                         String texture) {
        public enum Type {
            /** SkyBlock XP: the SkyBlock Leveling part counts it; nothing to give here. */
            SKYBLOCK_XP,
            /** Skill XP, given when the tier is reached. */
            SKILL_XP,
            /** A cheaper enchantment (enchanting isn't in the plugin yet): shown only. */
            EXP_DISCOUNT,
            /** More slots in a bag (the Quiver's, the Potion Bag's): shown only. */
            SLOTS,
            /** A minion's recipes (minions aren't in the plugin yet): shown, and never unlocked. */
            MINION_RECIPES,
            /** A pet's recipe (pets aren't in the plugin yet): shown, and never unlocked. */
            PET_RECIPE,
            /** A Dwarven Forge recipe (no forge yet): shown only. */
            FORGE_RECIPE,
            /** A crafting recipe: the Recipe Book and the crafting table have it from this tier on. */
            RECIPE,
            /** A trade (no Trades menu yet): shown only. */
            TRADE,
            /** A stat for good ({@code name} the plugin's Stat, "MINING_FORTUNE"), and its {@code text}: PlayerStats adds it. */
            STAT,
            /** Anything else (a bag, a level cap): its text, shown only. */
            UNLOCK,
            /** A boss collection's item, claimed from its Rewards menu. */
            ITEM,
            /** A boss collection's essence ({@code essence} "GOLD", {@code amount}, its head's {@code texture}): claimed with the tier's items. */
            ESSENCE
        }

        /** Whether its Rewards menu shows an item for it (the wiki's Collection UI: recipes, trades, items, essence). */
        public boolean shownInRewards() {
            return switch (type) {
                case MINION_RECIPES, PET_RECIPE, FORGE_RECIPE, RECIPE, TRADE, ITEM, ESSENCE -> true;
                default -> false;
            };
        }
    }

    /** An item that counts toward a collection: which one, and how many of its items one of it is (160 for an enchanted one). */
    public record Counted(String collection, long amount) {
    }

    private final List<Category> categories;
    private final Map<String, Collection> collections;
    private final List<Collection> bosses;
    private final Map<String, Counted> counted;
    private final List<Collection> withStats;
    private final List<String> problems;

    CollectionData(List<Category> categories, Map<String, Collection> collections, List<Collection> bosses, Map<String, Counted> counted,
                   List<String> problems) {
        this.categories = List.copyOf(categories);
        this.collections = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(collections));
        this.bosses = List.copyOf(bosses);
        this.counted = java.util.Collections.unmodifiableMap(new LinkedHashMap<>(counted));
        this.withStats = this.collections.values().stream()
                .filter(c -> c.tiers().stream().anyMatch(t -> t.rewards().stream().anyMatch(r -> r.type() == Reward.Type.STAT))).toList();
        this.problems = List.copyOf(problems);
    }

    public static CollectionData empty() {
        return new CollectionData(List.of(), Map.of(), List.of(), Map.of(), List.of());
    }

    public List<Category> categories() {
        return categories;
    }

    public Category category(String id) {
        for (Category category : categories) if (category.id().equals(id)) return category;
        return null;
    }

    /** An item collection, or a boss's ("CATACOMBS_1"); null for none. */
    public Collection collection(String id) {
        if (id == null) return null;
        Collection collection = collections.get(id);
        if (collection != null) return collection;
        for (Collection boss : bosses) if (boss.id().equals(id)) return boss;
        return null;
    }

    /** The item collections, in the menu's order. */
    public Map<String, Collection> collections() {
        return collections;
    }

    public List<Collection> bosses() {
        return bosses;
    }

    /** The collections some tier of which gives a stat (Obsidian's Mining Fortune). */
    public List<Collection> withStats() {
        return withStats;
    }

    /** What an item adds to a collection when a player gets it; null if it counts toward none. */
    public Counted counted(String itemId) {
        return itemId == null ? null : counted.get(itemId);
    }

    public int size() {
        return collections.size();
    }

    /** What was wrong with the file (it's missing, or parts of it couldn't be read). */
    public List<String> problems() {
        return problems;
    }

    /** Reads {@code collections.json} in this folder (collections/ of the data folder); what's missing is in {@link #problems}. */
    public static CollectionData load(Path folder) {
        Path file = folder.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return new CollectionData(List.of(), Map.of(), List.of(), Map.of(), List.of("no " + file + ", so there are no collections"));
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return new CollectionData(List.of(), Map.of(), List.of(), Map.of(), List.of("couldn't read " + file + ": " + e));
        }
    }

    static CollectionData parse(JsonObject o) {
        List<String> problems = new ArrayList<>();
        List<Category> categories = new ArrayList<>();
        Map<String, Collection> collections = new LinkedHashMap<>();
        JsonObject all = o.getAsJsonObject("collections");
        for (JsonElement e : o.getAsJsonArray("categories")) {
            JsonObject c = e.getAsJsonObject();
            List<String> ids = new ArrayList<>();
            for (JsonElement id : c.getAsJsonArray("collections")) {
                String cid = id.getAsString();
                JsonObject entry = all.has(cid) ? all.getAsJsonObject(cid) : null;
                if (entry == null) {
                    problems.add(cid + ": listed in " + c.get("id").getAsString() + " but not described");
                    continue;
                }
                try {
                    collections.put(cid, new Collection(cid, entry.get("name").getAsString(), c.get("id").getAsString(),
                            string(entry, "item"), null, 0, tiers(entry.getAsJsonArray("tiers"))));
                    ids.add(cid);
                } catch (RuntimeException ex) {
                    problems.add(cid + ": " + ex);
                }
            }
            categories.add(new Category(c.get("id").getAsString(), c.get("name").getAsString(), ids));
        }
        List<Collection> bosses = new ArrayList<>();
        if (o.has("bosses")) {
            for (JsonElement e : o.getAsJsonArray("bosses")) {
                JsonObject b = e.getAsJsonObject();
                try {
                    bosses.add(new Collection(b.get("id").getAsString(), b.get("name").getAsString(), BOSS, null,
                            string(b, "texture"), b.has("floor") ? b.get("floor").getAsInt() : 0, tiers(b.getAsJsonArray("tiers"))));
                } catch (RuntimeException ex) {
                    problems.add("boss " + b.get("id") + ": " + ex);
                }
            }
        }
        Map<String, Counted> counted = new LinkedHashMap<>();
        if (o.has("items")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("items").entrySet()) {
                JsonArray a = e.getValue().getAsJsonArray();
                if (!collections.containsKey(a.get(0).getAsString())) continue;
                counted.put(e.getKey(), new Counted(a.get(0).getAsString(), a.get(1).getAsLong()));
            }
        }
        return new CollectionData(categories, collections, bosses, counted, problems);
    }

    private static List<Tier> tiers(JsonArray array) {
        List<Tier> tiers = new ArrayList<>();
        for (JsonElement e : array) {
            JsonObject t = e.getAsJsonObject();
            List<Reward> rewards = new ArrayList<>();
            for (JsonElement r : t.getAsJsonArray("rewards")) rewards.add(reward(r.getAsJsonObject()));
            tiers.add(new Tier(t.get("amount").getAsLong(), rewards));
        }
        return tiers;
    }

    static Reward reward(JsonObject r) {
        return new Reward(Reward.Type.valueOf(r.get("type").getAsString()), string(r, "item"), string(r, "name"), string(r, "skill"),
                r.has("amount") ? r.get("amount").getAsLong() : 0, r.has("percent") ? r.get("percent").getAsInt() : 0,
                string(r, "essence"), string(r, "text"), string(r, "texture"));
    }

    private static String string(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
