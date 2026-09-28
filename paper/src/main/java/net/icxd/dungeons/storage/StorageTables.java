package net.icxd.dungeons.storage;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;

/**
 * Hypixel's numbers the storage menus and bags need, kept out of this repository in the private data
 * folder's {@code storage/} (servermgr links it in as the plugin's {@code storage}); see STORAGE.md:
 * <ul>
 *   <li>{@code powers.json}: each Accessory Power's stats, and the Accessory Power each rarity gives</li>
 *   <li>{@code accessories.json}: which accessories upgrade into which (only the best of a line counts)</li>
 *   <li>{@code bags.json}: how big each bag is at each tier of its collection</li>
 * </ul>
 * Read once, off the main thread (see {@link #load}); until then, or without the files, there are no
 * powers, every accessory is a line of its own and the bags are as small as they start. No Bukkit in it.
 */
public final class StorageTables {
    public static final String FOLDER = "storage";

    /**
     * A power: its tier's words ("Intermediate Stone Power"), the Power Stone that teaches it (null for one
     * everyone has) or else the material it shows as, the Combat level it needs, its stats before
     * Accessory Power (percentage points of the power, see {@link Powers#stats}) and its Unique Power
     * Bonus. Stats iterate in {@link Stat}'s order, which is the order Hypixel lists them in.
     */
    public record Power(String name, String tier, String stone, String icon, int combat, Map<Stat, Double> stats, Map<Stat, Double> bonus) {
        public Power {
            stats = ordered(stats);
            bonus = ordered(bonus);
        }

        private static Map<Stat, Double> ordered(Map<Stat, Double> stats) {
            Map<Stat, Double> out = new EnumMap<>(Stat.class);
            out.putAll(stats);
            return Collections.unmodifiableMap(out);
        }

        /** Learned from Power Stones at Maxwell, not there from the start. */
        public boolean stonePower() {
            return stone != null;
        }
    }

    /**
     * A bag's size by its collection: {@code base} slots once {@code collection} reaches tier {@code unlock}
     * (0: from the start), and each later tier's added slots.
     */
    public record BagSize(String collection, int unlock, int base, Map<Integer, Integer> slots) {
        public BagSize {
            slots = Collections.unmodifiableMap(new TreeMap<>(slots));
        }

        /** Slots at this tier of the collection; 0 while the bag is locked. */
        public int at(int tier) {
            if (tier < unlock) return 0;
            int size = base;
            for (Map.Entry<Integer, Integer> e : slots.entrySet()) {
                if (e.getKey() <= tier) size += e.getValue();
            }
            return size;
        }
    }

    private static final StorageTables EMPTY = new StorageTables(Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    private static volatile StorageTables current = EMPTY;

    private final Map<String, Power> powers;
    private final Map<Rarity, Integer> accessoryPower;
    private final Map<Stat, Double> multipliers;
    private final Map<String, List<String>> upgrades;
    private final Map<String, BagSize> bags;
    private final List<String> problems;

    StorageTables(Map<String, Power> powers, Map<Rarity, Integer> accessoryPower, Map<Stat, Double> multipliers,
                  Map<String, List<String>> upgrades, Map<String, BagSize> bags, List<String> problems) {
        this.powers = powers;
        this.accessoryPower = accessoryPower;
        this.multipliers = multipliers;
        this.upgrades = upgrades;
        this.bags = bags;
        this.problems = problems;
    }

    /** What's been read (nothing until {@link #load} has run). */
    public static StorageTables get() {
        return current;
    }

    static void set(StorageTables tables) {
        current = tables;
    }

    /** Reads the three files in {@code folder}; what's missing or broken is in {@link #problems}. */
    public static StorageTables load(Path folder) {
        List<String> problems = new ArrayList<>();
        Map<String, Power> powers = new LinkedHashMap<>();
        Map<Rarity, Integer> accessoryPower = new EnumMap<>(Rarity.class);
        Map<Stat, Double> multipliers = new EnumMap<>(Stat.class);
        Map<String, List<String>> upgrades = new HashMap<>();
        Map<String, BagSize> bags = new HashMap<>();
        if (!Files.isDirectory(folder)) {
            problems.add("no " + folder + ", so there are no Accessory Powers and the bags are as small as they start");
            return new StorageTables(powers, accessoryPower, multipliers, upgrades, bags, problems);
        }
        read(folder.resolve("powers.json"), problems, json -> {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("accessory_power").entrySet()) {
                accessoryPower.put(Rarity.valueOf(e.getKey()), e.getValue().getAsInt());
            }
            multipliers.putAll(stats(json.getAsJsonObject("multipliers")));
            for (JsonElement e : json.getAsJsonArray("powers")) {
                JsonObject p = e.getAsJsonObject();
                Power power = new Power(p.get("name").getAsString(), p.get("tier").getAsString(), text(p, "stone"), text(p, "icon"),
                        p.get("combat").getAsInt(), stats(p.getAsJsonObject("stats")), stats(p.getAsJsonObject("bonus")));
                powers.put(power.name(), power);
            }
        });
        read(folder.resolve("accessories.json"), problems, json -> {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("upgrades").entrySet()) {
                List<String> higher = new ArrayList<>();
                for (JsonElement id : e.getValue().getAsJsonArray()) higher.add(id.getAsString());
                upgrades.put(e.getKey(), List.copyOf(higher));
            }
        });
        read(folder.resolve("bags.json"), problems, json -> {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("bags").entrySet()) {
                JsonObject b = e.getValue().getAsJsonObject();
                Map<Integer, Integer> slots = new TreeMap<>();
                for (Map.Entry<String, JsonElement> s : b.getAsJsonObject("slots").entrySet()) {
                    slots.put(Integer.parseInt(s.getKey()), s.getValue().getAsInt());
                }
                bags.put(e.getKey(), new BagSize(b.get("collection").getAsString(), b.get("unlock").getAsInt(), b.get("base").getAsInt(), slots));
            }
        });
        return new StorageTables(Collections.unmodifiableMap(powers), Collections.unmodifiableMap(accessoryPower),
                Collections.unmodifiableMap(multipliers), Collections.unmodifiableMap(upgrades), Collections.unmodifiableMap(bags),
                List.copyOf(problems));
    }

    /** Reads a file into {@code into}; one that's missing or isn't what it should be is a problem (what was read of it stays). */
    private static void read(Path file, List<String> problems, Consumer<JsonObject> into) {
        if (!Files.exists(file)) {
            problems.add("no " + file);
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            into.accept(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            problems.add(file.getFileName() + ": " + e);
        }
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static Map<Stat, Double> stats(JsonObject o) {
        Map<Stat, Double> out = new LinkedHashMap<>();
        if (o == null) return out;
        for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(Stat.valueOf(e.getKey()), e.getValue().getAsDouble());
        return out;
    }

    /** Every power, by name. */
    public Map<String, Power> powers() {
        return powers;
    }

    /** Null if there's no such power. */
    public Power power(String name) {
        return name == null ? null : powers.get(name);
    }

    /** The Accessory Power an accessory of this rarity gives; 0 without the table. */
    public int accessoryPower(Rarity rarity) {
        return accessoryPower.getOrDefault(rarity, 0);
    }

    /** How much of a stat a power's base is worth, the stat's own multiplier; 1 for a stat the table doesn't have. */
    public double multiplier(Stat stat) {
        return multipliers.getOrDefault(stat, 1.0);
    }

    /** The accessories this one upgrades into; none if it's the top of its line or in no line. */
    public List<String> upgrades(String id) {
        return upgrades.getOrDefault(id, List.of());
    }

    /** Every accessory with upgrades. */
    public Set<String> upgradable() {
        return upgrades.keySet();
    }

    /** Lines of accessories (Talisman, Ring, Artifact, ...), by one accessory of each: worked out when first asked. */
    private volatile Map<String, String> lines;

    /**
     * The line an accessory is in, as one of the line's ids: every accessory that upgrades into another
     * is in its line, and so on (NEU puts all the Abicases in one, each "upgrading" into the others).
     * An accessory in no line is its own.
     */
    public String line(String id) {
        Map<String, String> lines = this.lines;
        if (lines == null) {
            Map<String, String> parent = new HashMap<>();
            for (Map.Entry<String, List<String>> e : upgrades.entrySet()) {
                for (String higher : e.getValue()) parent.put(root(parent, higher), root(parent, e.getKey()));
            }
            lines = new HashMap<>();
            for (String member : new ArrayList<>(parent.keySet())) lines.put(member, root(parent, member));
            this.lines = lines = Collections.unmodifiableMap(lines);
        }
        return lines.getOrDefault(id, id);
    }

    private static String root(Map<String, String> parent, String id) {
        String root = id;
        while (parent.containsKey(root) && !parent.get(root).equals(root)) root = parent.get(root);
        parent.putIfAbsent(id, root);
        return root;
    }

    /** A bag's sizes; null if bags.json doesn't have it. */
    public BagSize bag(String bag) {
        return bags.get(bag);
    }

    /** What couldn't be read, and why. */
    public List<String> problems() {
        return problems;
    }
}
