package net.icxd.dungeons.item.gemstone;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.stats.Stat;

/**
 * Hypixel's gemstone table, kept out of this repository (the private data's {@code hex/gemstones.json}, made by
 * tools/hex/build_gems.py from NEU's gemstones.json and the items API; the Hex's Gemstones category reads it at
 * startup, see HexData): what each gem gives by its quality and the item's rarity, what taking one off costs, what
 * one adds to a chisel's perk, and the armour sets the Gemstone Guide shows once. Until it's read (or without the
 * file) there's none, and gems give nothing ({@link #get} is null). Unlock costs aren't here: they're the item
 * data's (items.json gemstone_slots).
 *
 * @param gems              each gem's stat and values
 * @param removalCosts      coins by quality (Rough 1 to Perfect 500,000)
 * @param chiselPercentages a chisel perk's percentage by quality (LATER: the Fossil Excavator)
 * @param chiselPerks       the perks' words by gem, "{}" for the percentage (LATER)
 * @param armorSets         sets by id (the Museum's, else pieces' ids without the piece), each with its name and pieces
 */
public record GemstoneTable(Map<GemstoneType, Stone> gems, Map<GemstoneQuality, Double> removalCosts,
                            Map<GemstoneQuality, Integer> chiselPercentages, Map<GemstoneType, String> chiselPerks,
                            Map<String, ArmorSet> armorSets) {
    public static final String FILE = "gemstones.json";

    /** A gem's stat, and what one gives by quality and rarity (Common to Mythic; Divine for Amber, Topaz and Jade). */
    public record Stone(Stat stat, Map<GemstoneQuality, Map<Rarity, Double>> values) {
    }

    /** An armour set: its name ("Divan Armor") and its pieces by id, helmet first. */
    public record ArmorSet(String name, List<String> pieces) {
    }

    private static volatile GemstoneTable loaded;

    /** The table as read; null until it is (or without the file). */
    public static GemstoneTable get() {
        return loaded;
    }

    /** The table read at startup (null takes it away, for tests). */
    public static void use(GemstoneTable table) {
        loaded = table;
    }

    /**
     * What one gem gives on an item of this rarity (the item's rarity now, after recombobulating: the recorded
     * recombobulated Shadow Assassin pieces have Mythic's). Divine has its own values only for Amber, Topaz and
     * Jade; the rest take Mythic's there, and every gem takes Mythic's at Special and Very Special (UNKNOWN: no
     * source has those). 0 for a gem the table doesn't have.
     */
    public double value(Gem gem, Rarity rarity) {
        Stone stone = gems.get(gem.type());
        Map<Rarity, Double> values = stone == null ? null : stone.values().get(gem.quality());
        if (values == null) return 0;
        Rarity at = rarity.ordinal() > Rarity.DIVINE.ordinal() ? Rarity.MYTHIC : rarity;
        if (at == Rarity.DIVINE && !values.containsKey(Rarity.DIVINE)) at = Rarity.MYTHIC;
        for (int r = at.ordinal(); r >= 0; r--) {
            Double value = values.get(Rarity.values()[r]);
            if (value != null) return value;
        }
        return 0;
    }

    /** The stat a gem gives; null for one the table doesn't have. */
    public Stat stat(GemstoneType gem) {
        Stone stone = gems.get(gem);
        return stone == null ? null : stone.stat();
    }

    /** The coins taking a gem of this quality off costs. */
    public double removalCost(GemstoneQuality quality) {
        return removalCosts.getOrDefault(quality, 0.0);
    }

    // Reading

    /** The table in the file's JSON; null, with problems, if it isn't one. What's unknown in it is a problem, and left out. */
    public static GemstoneTable read(JsonElement json, List<String> problems) {
        if (json == null) return null;
        if (!json.isJsonObject() || !json.getAsJsonObject().has("format") || json.getAsJsonObject().get("format").getAsInt() != 1) {
            problems.add(FILE + ": not format 1");
            return null;
        }
        JsonObject root = json.getAsJsonObject();
        Map<GemstoneType, Stone> gems = new EnumMap<>(GemstoneType.class);
        for (Map.Entry<String, JsonElement> e : object(root, "gems").entrySet()) {
            GemstoneType type = GemstoneType.of(e.getKey());
            JsonObject gem = e.getValue().getAsJsonObject();
            Stat stat = stat(gem.has("stat") ? gem.get("stat").getAsString() : "");
            if (type == null || !type.gem() || stat == null) {
                problems.add(FILE + ": gem " + e.getKey() + " (stat " + gem.get("stat") + ") isn't one this plugin has");
                continue;
            }
            Map<GemstoneQuality, Map<Rarity, Double>> values = new EnumMap<>(GemstoneQuality.class);
            for (Map.Entry<String, JsonElement> q : object(gem, "values").entrySet()) {
                GemstoneQuality quality = GemstoneQuality.of(q.getKey());
                if (quality == null) {
                    problems.add(FILE + ": " + e.getKey() + " quality " + q.getKey());
                    continue;
                }
                Map<Rarity, Double> byRarity = new EnumMap<>(Rarity.class);
                for (Map.Entry<String, JsonElement> r : q.getValue().getAsJsonObject().entrySet()) {
                    Rarity rarity = rarity(r.getKey());
                    if (rarity == null) problems.add(FILE + ": " + e.getKey() + " " + q.getKey() + " rarity " + r.getKey());
                    else byRarity.put(rarity, r.getValue().getAsDouble());
                }
                values.put(quality, byRarity);
            }
            gems.put(type, new Stone(stat, values));
        }
        Map<GemstoneQuality, Double> removal = new EnumMap<>(GemstoneQuality.class);
        Map<GemstoneQuality, Integer> chisel = new EnumMap<>(GemstoneQuality.class);
        for (GemstoneQuality quality : GemstoneQuality.values()) {
            JsonElement fee = object(root, "removal_costs").get(quality.name());
            if (fee == null) problems.add(FILE + ": no removal cost for " + quality);
            else removal.put(quality, fee.getAsDouble());
            JsonElement percent = object(root, "chisel_percentages").get(quality.name());
            if (percent != null) chisel.put(quality, percent.getAsInt());
        }
        Map<GemstoneType, String> perks = new EnumMap<>(GemstoneType.class);
        for (Map.Entry<String, JsonElement> e : object(root, "chisel_perks").entrySet()) {
            GemstoneType type = GemstoneType.of(e.getKey());
            if (type != null) perks.put(type, e.getValue().getAsString());
        }
        Map<String, ArmorSet> sets = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : object(root, "armor_sets").entrySet()) {
            JsonObject set = e.getValue().getAsJsonObject();
            List<String> pieces = new ArrayList<>();
            for (JsonElement piece : set.getAsJsonArray("pieces")) pieces.add(piece.getAsString());
            sets.put(e.getKey(), new ArmorSet(set.get("name").getAsString(), List.copyOf(pieces)));
        }
        return new GemstoneTable(gems, removal, chisel, perks, sets);
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement e = parent.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
    }

    private static Stat stat(String name) {
        for (Stat stat : Stat.values()) if (stat.name().equals(name)) return stat;
        return null;
    }

    private static Rarity rarity(String name) {
        for (Rarity rarity : Rarity.values()) if (rarity.name().equals(name)) return rarity;
        return null;
    }
}
