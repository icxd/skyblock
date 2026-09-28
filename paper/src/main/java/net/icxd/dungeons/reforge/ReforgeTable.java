package net.icxd.dungeons.reforge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Hypixel's reforges, kept out of this repository in the private data's {@code hex/reforges.json} (made by
 * tools/hex/build_reforges.py; see REFORGES.md): every reforge by its modifier id, the Blacksmith's basic pools
 * (which a random reforge is rolled from), every Reforge Stone, and what a random reforge costs. Read with the Hex's
 * tables (HexData, off the main thread) and {@link #set} on the main thread; until then, or without the file, it's
 * empty: reforges keep their names and give nothing, and there's nothing to apply. No Bukkit in it.
 */
public final class ReforgeTable {
    public static final String FILE = "reforges.json";
    static final ReforgeTable EMPTY = new ReforgeTable(Map.of(), List.of(), List.of(), Map.of());

    private static volatile ReforgeTable current = EMPTY;

    /** A basic pool: the kind of item it's for ("SWORD/ROD", see {@link ReforgeStone#TYPES}) and its reforges. */
    public record Pool(String type, List<Reforge> reforges) {
        public Pool {
            reforges = List.copyOf(reforges);
        }
    }

    private final Map<String, Reforge> reforges;
    /** By id and by name, lower case. */
    private final Map<String, Reforge> lookup = new HashMap<>();
    private final List<Pool> pools;
    private final List<ReforgeStone> stones;
    private final Map<Rarity, Long> randomPrice;

    ReforgeTable(Map<String, Reforge> reforges, List<Pool> pools, List<ReforgeStone> stones, Map<Rarity, Long> randomPrice) {
        this.reforges = Collections.unmodifiableMap(new LinkedHashMap<>(reforges));
        for (Reforge reforge : reforges.values()) lookup.putIfAbsent(reforge.name().toLowerCase(Locale.ROOT), reforge);
        for (Reforge reforge : reforges.values()) lookup.put(reforge.id(), reforge);
        this.pools = List.copyOf(pools);
        this.stones = List.copyOf(stones);
        Map<Rarity, Long> prices = new EnumMap<>(Rarity.class);
        prices.putAll(randomPrice);
        this.randomPrice = Collections.unmodifiableMap(prices);
    }

    /** What's been read (empty until the Hex's tables are). */
    public static ReforgeTable get() {
        return current;
    }

    /** Main thread, once read. */
    public static void set(ReforgeTable table) {
        current = table == null ? EMPTY : table;
    }

    // Reading

    /** The table in reforges.json ({@code json}; null if it couldn't be read); what's wrong with it goes in {@code problems}. */
    public static ReforgeTable read(JsonElement json, List<String> problems) {
        if (json == null) return null;
        JsonObject root = json.getAsJsonObject();
        Map<String, Map<String, String>> prefixes = new HashMap<>();
        for (JsonElement e : array(root, "prefixes")) {
            JsonObject p = e.getAsJsonObject();
            prefixes.computeIfAbsent(p.get("reforge").getAsString(), k -> new LinkedHashMap<>())
                    .put(p.get("name").getAsString(), p.get("prefix").getAsString());
        }
        Map<String, Reforge> reforges = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object(root, "reforges").entrySet()) {
            String id = e.getKey();
            JsonObject r = e.getValue().getAsJsonObject();
            Map<Rarity, Map<Stat, Double>> stats = new EnumMap<>(Rarity.class);
            for (Map.Entry<String, JsonElement> row : object(r, "stats").entrySet()) {
                Rarity rarity = rarity(row.getKey(), id, problems);
                if (rarity != null) stats.put(rarity, stats(row.getValue().getAsJsonObject(), id, problems));
            }
            Map<Rarity, List<String>> bonus = new EnumMap<>(Rarity.class);
            for (Map.Entry<String, JsonElement> row : object(r, "bonus").entrySet()) {
                Rarity rarity = rarity(row.getKey(), id, problems);
                if (rarity == null) continue;
                List<String> lines = new ArrayList<>();
                for (JsonElement line : row.getValue().getAsJsonArray()) lines.add(line.getAsString());
                bonus.put(rarity, lines);
            }
            String title = r.has("bonus_title") ? r.get("bonus_title").getAsString() : null;
            reforges.put(id, new Reforge(id, r.get("name").getAsString(), new ReforgeStats(stats), title, bonus,
                    stats(object(r, "per_catacombs_level"), id, problems), prefixes.getOrDefault(id, Map.of())));
        }
        prefixes.keySet().stream().filter(id -> !reforges.containsKey(id)).forEach(id -> problems.add("a prefix for no reforge: " + id));
        List<Pool> pools = new ArrayList<>();
        for (JsonElement e : array(root, "pools")) {
            JsonObject p = e.getAsJsonObject();
            String type = p.get("type").getAsString();
            if (!ReforgeStone.TYPES.containsKey(type)) problems.add("pool of an unknown type: " + type);
            List<Reforge> members = new ArrayList<>();
            for (JsonElement id : p.getAsJsonArray("reforges")) {
                Reforge reforge = reforges.get(id.getAsString());
                if (reforge == null) problems.add("pool " + type + ": no reforge " + id.getAsString());
                else members.add(reforge);
            }
            pools.add(new Pool(type, members));
        }
        List<ReforgeStone> stones = new ArrayList<>();
        for (JsonElement e : array(root, "stones")) {
            JsonObject s = e.getAsJsonObject();
            String item = s.get("item").getAsString();
            Reforge reforge = reforges.get(s.get("reforge").getAsString());
            String type = s.has("type") ? s.get("type").getAsString() : null;
            Set<String> items = new LinkedHashSet<>();
            for (JsonElement id : array(s, "items")) items.add(id.getAsString());
            if (reforge == null) {
                problems.add(item + ": no reforge " + s.get("reforge").getAsString());
                continue;
            }
            if (items.isEmpty() && (type == null || !ReforgeStone.TYPES.containsKey(type))) {
                problems.add(item + ": goes on an unknown type " + type);
                continue;
            }
            stones.add(new ReforgeStone(item, reforge, type, items, prices(object(s, "costs"), item, problems)));
        }
        return new ReforgeTable(reforges, pools, stones, prices(object(root, "random_price"), "random_price", problems));
    }

    private static JsonObject object(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonObject() ? o.getAsJsonObject(key) : new JsonObject();
    }

    private static JsonArray array(JsonObject o, String key) {
        return o.has(key) && o.get(key).isJsonArray() ? o.getAsJsonArray(key) : new JsonArray();
    }

    private static Rarity rarity(String name, String where, List<String> problems) {
        try {
            return Rarity.valueOf(name);
        } catch (IllegalArgumentException e) {
            problems.add(where + ": no rarity " + name);
            return null;
        }
    }

    private static Map<Stat, Double> stats(JsonObject o, String where, List<String> problems) {
        Map<Stat, Double> out = new EnumMap<>(Stat.class);
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            try {
                out.put(Stat.valueOf(e.getKey()), e.getValue().getAsDouble());
            } catch (IllegalArgumentException x) {
                problems.add(where + ": no stat " + e.getKey());
            }
        }
        return out;
    }

    private static Map<Rarity, Long> prices(JsonObject o, String where, List<String> problems) {
        Map<Rarity, Long> out = new EnumMap<>(Rarity.class);
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            Rarity rarity = rarity(e.getKey(), where, problems);
            if (rarity != null) out.put(rarity, e.getValue().getAsLong());
        }
        return out;
    }

    // Asking

    /** The reforge an item's data names (its id or its name, any case); null if the table doesn't have it. */
    public Reforge reforge(String data) {
        return data == null ? null : lookup.get(data.trim().toLowerCase(Locale.ROOT));
    }

    /** Every reforge, by id. */
    public Map<String, Reforge> reforges() {
        return reforges;
    }

    public List<Pool> pools() {
        return pools;
    }

    public List<ReforgeStone> stones() {
        return stones;
    }

    /**
     * The basic pool a random reforge on this item comes from: the first for its type, if it's reforgeable. UNKNOWN:
     * a gauntlet is of both the sword and rod pool's types and the pickaxe pool's (SkyHanni's); the sword and rod
     * pool, the first, is taken.
     */
    public Pool pool(SkyBlockItem item) {
        if (!item.reforgeable()) return null;
        for (Pool pool : pools) if (ReforgeStone.isOfType(item, pool.type())) return pool;
        return null;
    }

    /** The stones that go on this item at this rarity (its current one), in the table's order. */
    public List<ReforgeStone> stones(SkyBlockItem item, Rarity rarity) {
        List<ReforgeStone> out = new ArrayList<>();
        for (ReforgeStone stone : stones) if (stone.fits(item, rarity)) out.add(stone);
        return out;
    }

    /**
     * Whether the Hex can reforge it at all: a basic pool or a stone is for it. Without the table, whether the item
     * says it can be reforged.
     */
    public boolean reforgeable(SkyBlockItem item) {
        if (this == EMPTY) return item.reforgeable();
        if (pool(item) != null) return true;
        for (ReforgeStone stone : stones) if (stone.fits(item)) return true;
        return false;
    }

    /** What a random basic reforge costs on an item of that rarity; null if the table has no price (UNKNOWN: Unobtainable). */
    public Long randomPrice(Rarity rarity) {
        return randomPrice.get(rarity);
    }

    /**
     * A random reforge from the pool, each as likely (the wiki's Reforging), but never the one it has now: UNKNOWN
     * whether Hypixel rolls it again. Null if the pool has no other.
     */
    public static Reforge roll(Pool pool, Reforge current, RandomGenerator random) {
        List<Reforge> choices = new ArrayList<>();
        for (Reforge reforge : pool.reforges()) if (current == null || !reforge.id().equals(current.id())) choices.add(reforge);
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }
}
