package net.icxd.dungeons.dungeons.chests;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.item.cost.essence.EssenceType;
import net.icxd.dungeons.utils.Utils;

/**
 * What a reward chest holds (MCW Dungeon Reward Chest, "Loot Rolling Process"), from {@code dungeons/chest_loot.json}
 * (MCW's loot tables, Floor I so far).
 *
 * <p>A chest gets a quality ({@link #quality}), which every row it rolls costs. Rows are picked by weight among those
 * still in reach that haven't come up yet (essence with a quality and a weight can come up again) until none is in
 * reach; the essence of quality 1 and weight 0 then takes what quality is left, one at a time, and last come the rows
 * every chest of the kind has. All three recorded chests (2026_10_27_06_28_42_1) come out of it exactly with a
 * Treasure Talisman's quality: Wood 112 (Feather Falling VI, Undead 3 + 2, Wither 4 + 3), Gold 164 (Combo I, Undead
 * 5 + 4, Wither 5 + 4), Diamond 204 (Combo I and Rejuvenate I, Undead 8 + 4, Wither 6).
 */
public final class ChestLoot {
    public enum Kind {
        ITEM, BOOK, ESSENCE
    }

    /** A table's row: an item (its SkyBlock id), a book (the enchantment's id, its level) or essence (type, amount). */
    public record Entry(Kind kind, String id, int count, long cost, int quality, int weight) {
        /** In every chest of the kind. */
        boolean always() {
            return quality == 0 && weight == 0;
        }

        /** Takes up what quality is left, a point at a time. */
        boolean filler() {
            return kind == Kind.ESSENCE && quality == 1 && weight == 0;
        }

        /** Can come up more than once. */
        boolean repeats() {
            return kind == Kind.ESSENCE && quality > 0 && weight > 0;
        }
    }

    /** A chest's table: what it costs with nothing extra in it, its quality before bonuses, and its rows. */
    public record Table(long cost, int quality, List<Entry> entries) {
    }

    /** What's in a chest, essence of a kind merged into one: as its menu and the chat list them. */
    public record Reward(Kind kind, String id, int count, long cost) {
        /** The essence's type; null for anything else. */
        public EssenceType essence() {
            return kind == Kind.ESSENCE ? EssenceType.valueOf(id) : null;
        }
    }

    /** Earning S+: x1.05 (MCW Quality Upgrades). */
    static final double S_PLUS = 1.05;
    /** The Treasure accessories' multipliers, the highest counting (MCW Quality Upgrades). */
    static final Map<String, Double> TREASURE = Map.of("TREASURE_ARTIFACT", 1.03, "TREASURE_RING", 1.02, "TREASURE_TALISMAN", 1.01);

    private static Map<DungeonFloor, Map<ChestType, Table>> tables;

    private ChestLoot() {
    }

    /** The table of this chest on this floor; null if there's none (no data for the floor, or no such chest there). */
    public static synchronized Table table(DungeonFloor floor, ChestType type) {
        if (tables == null) tables = read();
        Map<ChestType, Table> chests = tables.get(floor);
        return chests == null ? null : chests.get(type);
    }

    private static Map<DungeonFloor, Map<ChestType, Table>> read() {
        Map<DungeonFloor, Map<ChestType, Table>> out = new EnumMap<>(DungeonFloor.class);
        try (Reader in = new InputStreamReader(ChestLoot.class.getResourceAsStream("/dungeons/chest_loot.json"), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
            for (String floorName : root.keySet()) {
                if (floorName.startsWith("_")) continue;
                Map<ChestType, Table> chests = new EnumMap<>(ChestType.class);
                JsonObject floor = root.getAsJsonObject(floorName);
                for (String chestName : floor.keySet()) {
                    JsonObject chest = floor.getAsJsonObject(chestName);
                    List<Entry> entries = new ArrayList<>();
                    for (JsonElement e : chest.getAsJsonArray("loot")) entries.add(entry(e.getAsJsonObject()));
                    chests.put(type(chestName), new Table(chest.get("cost").getAsLong(), chest.get("quality").getAsInt(), List.copyOf(entries)));
                }
                out.put(DungeonFloor.valueOf(floorName), chests);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read dungeons/chest_loot.json", e);
        }
        return out;
    }

    private static ChestType type(String title) {
        for (ChestType type : ChestType.values()) if (type.title().equalsIgnoreCase(title)) return type;
        throw new IllegalArgumentException("no chest " + title);
    }

    private static Entry entry(JsonObject e) {
        long cost = e.get("cost").getAsLong();
        int quality = e.get("quality").getAsInt();
        int weight = e.get("weight").getAsInt();
        if (e.has("item")) return new Entry(Kind.ITEM, e.get("item").getAsString(), 1, cost, quality, weight);
        if (e.has("book")) return new Entry(Kind.BOOK, e.get("book").getAsString(), e.get("level").getAsInt(), cost, quality, weight);
        return new Entry(Kind.ESSENCE, EssenceType.valueOf(e.get("essence").getAsString()).name(), e.get("amount").getAsInt(), cost, quality, weight);
    }

    /**
     * A chest's quality (MCW Quality Formula): its base, x1.05 for S+ rounded down, then the Treasure accessory's
     * multiplier and the flat bonuses (the Boss Luck perk, the Catacombs Box attribute; neither is in the plugin yet)
     * rounded to the nearest, twice, as Hypixel's bug has it.
     */
    public static int quality(int base, boolean sPlus, double treasure, int flat) {
        int initial = (int) Math.floor(base * (sPlus ? S_PLUS : 1));
        int intended = nearest(initial * treasure + flat);
        return nearest(intended * treasure + flat);
    }

    private static int nearest(double value) {
        return new BigDecimal(value).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /** The highest Treasure accessory's multiplier among these (ids of the accessories that count); 1 for none. */
    public static double treasure(Set<String> accessories) {
        double best = 1;
        for (String id : accessories) best = Math.max(best, TREASURE.getOrDefault(id, 1.0));
        return best;
    }

    /**
     * What a chest of this quality holds: its rolled items and books in the order they came up, then Undead Essence,
     * then Wither Essence (as recorded), each essence merged into one.
     */
    public static List<Reward> roll(Table table, int quality, RandomGenerator random) {
        List<Entry> rolled = new ArrayList<>();
        Set<Entry> taken = new HashSet<>();
        int left = quality;
        while (true) {
            List<Entry> eligible = new ArrayList<>();
            int total = 0;
            for (Entry e : table.entries()) {
                if (e.weight() <= 0 || e.quality() > left || (!e.repeats() && taken.contains(e))) continue;
                eligible.add(e);
                total += e.weight();
            }
            if (eligible.isEmpty()) break;
            int pick = random.nextInt(total);
            Entry chosen = eligible.get(eligible.size() - 1);
            for (Entry e : eligible) {
                if (pick < e.weight()) {
                    chosen = e;
                    break;
                }
                pick -= e.weight();
            }
            rolled.add(chosen);
            taken.add(chosen);
            left -= chosen.quality();
        }
        for (Entry e : table.entries()) {
            if (!e.filler()) continue;
            for (; left > 0; left--) rolled.add(e);
            break;
        }
        for (Entry e : table.entries()) if (e.always()) rolled.add(e);
        return merge(rolled);
    }

    private static List<Reward> merge(List<Entry> rolled) {
        List<Reward> out = new ArrayList<>();
        Map<String, Integer> essence = new LinkedHashMap<>();
        essence.put(EssenceType.UNDEAD.name(), 0);
        essence.put(EssenceType.WITHER.name(), 0);
        for (Entry e : rolled) {
            if (e.kind() == Kind.ESSENCE) essence.merge(e.id(), e.count(), Integer::sum);
            else out.add(new Reward(e.kind(), e.id(), e.count(), e.cost()));
        }
        for (Map.Entry<String, Integer> e : essence.entrySet()) {
            if (e.getValue() > 0) out.add(new Reward(Kind.ESSENCE, e.getKey(), e.getValue(), 0));
        }
        return out;
    }

    /** What the chest costs: its base and what each of its rewards adds (Bonzo's Mask, a million). */
    public static long cost(Table table, List<Reward> rewards) {
        long cost = table.cost();
        for (Reward reward : rewards) cost += reward.cost();
        return cost;
    }

    /** "&dUndead Essence &8x12", as recorded. */
    public static String essenceLine(EssenceType type, int amount) {
        return "&d" + Utils.title(type.name()) + " Essence &8x" + amount;
    }

    /** "&fEnchanted Book (Feather Falling VI)", or an ultimate one's "&fEnchanted Book (&d&lCombo I&f)", as recorded. */
    public static String bookLine(String name, int level, boolean ultimate) {
        String enchantment = name + " " + Utils.getRomanNumeral(level);
        return "&fEnchanted Book (" + (ultimate ? "&d&l" + enchantment + "&f" : enchantment) + ")";
    }

    /** "&650,000 Coins" as the menu shows a chest's cost, or "&aFREE" (both recorded). */
    public static String costLine(long cost) {
        return cost <= 0 ? "&aFREE" : "&6" + String.format(Locale.ROOT, "%,d", cost) + " Coins";
    }
}
