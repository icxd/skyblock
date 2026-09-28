package net.icxd.dungeons.item.enchanting;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.SpecificItemType;

/**
 * Every enchantment there is, as Hypixel has it: the private data folder's {@code hex/enchantments.json}, made by
 * tools/hex/build_enchants.py from the game's enchanted books and the wiki (see ENCHANTMENTS.md). For each: its
 * name, whether it's ultimate, the levels the Hex offers and those the Enchantment Table sells, the Exp levels
 * each costs and the Enchanting level it needs, what it goes on (the books' "Applied To"), what it can't be on
 * with, and what each level says. Item lore reads the text through {@link EnchantmentType}, the Hex the rest.
 *
 * <p>It's read off the main thread when the plugin starts, with the Hex's tables (the Enchantments category adds
 * it to HexData); until then, and without the file, there are no enchantments, and items show none of theirs.
 * What code does for an enchantment (damage, coins, set bonuses) goes by its id and works either way. No Bukkit.
 */
public final class EnchantmentData {
    public static final String FILE = "enchantments.json";

    /** A level: what it says (one paragraph, as item lore wraps it, and the book's own lines), its book's rarity, and what the next tier takes. */
    public record Level(String text, List<String> lines, Rarity rarity, String tierUp) {
        public Level {
            lines = lines == null ? null : List.copyOf(lines);
        }
    }

    /**
     * An enchantment. {@code id} is what items store it as (Hypixel's id, an ultimate's without "ultimate_"),
     * {@code hypixelId} Hypixel's. The Hex offers levels {@code min} to {@code max}; the Enchantment Table sells up
     * to {@code table} (0 for none). {@code xp} is the Exp levels by level from 1 (null where no source says).
     * {@code removed}: Hypixel took it out of the game (it stays for the items that have it).
     */
    public record Entry(String id, String hypixelId, String name, boolean ultimate, boolean removed, int min, int max, int table,
                        List<Integer> xp, int enchanting, List<String> applies, List<String> conflicts, Map<Integer, Level> levels) {
        public Entry {
            xp = Collections.unmodifiableList(new ArrayList<>(xp));
            applies = List.copyOf(applies);
            conflicts = List.copyOf(conflicts);
            levels = Collections.unmodifiableMap(new TreeMap<>(levels));
        }

        /** Null if it has no text at that level. */
        public Level level(int level) {
            return levels.get(level);
        }

        /** The Exp levels a level costs; null where no source says (UNKNOWN). */
        public Integer xp(int level) {
            return level >= 1 && level <= xp.size() ? xp.get(level - 1) : null;
        }

        /** Whether the Enchantment Table sells this level; above that, the Hex wants the book too. */
        public boolean fromTable(int level) {
            return level <= table;
        }

        /** Whether it goes on this item: the item is one of the kinds its books name. */
        public boolean appliesTo(SkyBlockItem item) {
            for (String label : applies) {
                if (is(item, label)) return true;
            }
            return false;
        }
    }

    public static final EnchantmentData EMPTY = new EnchantmentData(Map.of(), List.of());
    private static volatile EnchantmentData current = EMPTY;

    /** By id, in the file's order. */
    private final Map<String, Entry> entries;
    /** Every id, and Hypixel's, to the id. */
    private final Map<String, String> names = new HashMap<>();
    private final List<String> order;

    EnchantmentData(Map<String, Entry> entries, List<String> order) {
        this.entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
        for (Entry e : entries.values()) {
            names.put(e.hypixelId(), e.id());
            names.put(e.id(), e.id());
        }
        List<String> ordered = new ArrayList<>();
        for (String id : order) if (entries.containsKey(id) && !ordered.contains(id)) ordered.add(id);
        for (String id : entries.keySet()) if (!ordered.contains(id)) ordered.add(id);
        this.order = List.copyOf(ordered);
    }

    /** The table in use (empty until it's read). */
    public static EnchantmentData current() {
        return current;
    }

    public static void use(EnchantmentData data) {
        current = data == null ? EMPTY : data;
    }

    /**
     * What items store an enchantment as, from any name it goes by: lower case, and an ultimate without Hypixel's
     * "ultimate_" ("ultimate_one_for_all" is "one_for_all", as the plugin has always stored it).
     */
    public static String id(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        return id.startsWith("ultimate_") ? id.substring("ultimate_".length()) : id;
    }

    /** The enchantment by its id or Hypixel's, in any case; null if there's no such one. */
    public Entry get(String name) {
        if (name == null) return null;
        String id = names.get(name.toLowerCase(Locale.ROOT));
        return entries.get(id != null ? id : id(name));
    }

    public Collection<Entry> all() {
        return entries.values();
    }

    public int size() {
        return entries.size();
    }

    /** Every id, in the Hex's Default order: the order the wiki's Hex screens show, then the rest by name (see the generator). */
    public List<String> order() {
        return order;
    }

    /** Whether the two can't be on one item together (either's book says so; NEU's pools). */
    public boolean conflict(String a, String b) {
        Entry x = get(a), y = get(b);
        if (x == null || y == null || x.id().equals(y.id())) return false;
        return x.conflicts().contains(y.id()) || y.conflicts().contains(x.id());
    }

    // What they go on

    /**
     * Whether an item is one of the kinds an "Applied To" line names. The books' words mapped onto the plugin's item
     * types: "Armor" is the four pieces (and a Carnival Mask, which NEU gives a chestplate's enchantments), "Tools"
     * pickaxes, drills, axes, shovels and shears (the wiki's Enchantments) and also gauntlets and farming tools (live
     * items: Efficiency, "Tools" only, is on 66 Gemstone Gauntlets and 59 farming tools, Silk Touch on 35 farming
     * tools), "Hoe" and "Farming Tool" both the farming tools (no item is typed HOE; NEU puts Harvesting, the books'
     * only "Hoe" one, on farming tools); a name that isn't a kind is an item's own ("Precursor Eye"). UNKNOWN: which
     * items are "Fishing Weapon"s (none has that type here).
     */
    public static boolean is(SkyBlockItem item, String label) {
        SpecificItemType type = item.specificItemType();
        String key = item.typeKey();
        boolean mask = "CARNIVAL_MASK".equals(key);
        return switch (label) {
            case "Sword" -> type == SpecificItemType.SWORD;
            case "Longsword" -> type == SpecificItemType.LONGSWORD;
            case "Fishing Weapon" -> type == SpecificItemType.FISHING_WEAPON;
            case "Gauntlet" -> type == SpecificItemType.GAUNTLET;
            case "Bow" -> type == SpecificItemType.BOW;
            case "Fishing Rod" -> type == SpecificItemType.FISHING_ROD;
            case "Wand" -> type == SpecificItemType.WAND;
            case "Armor" -> mask || type == SpecificItemType.HELMET || type == SpecificItemType.CHESTPLATE
                    || type == SpecificItemType.LEGGINGS || type == SpecificItemType.BOOTS;
            case "Helmet" -> type == SpecificItemType.HELMET;
            case "Chestplate" -> mask || type == SpecificItemType.CHESTPLATE;
            case "Leggings" -> type == SpecificItemType.LEGGINGS;
            case "Boots" -> type == SpecificItemType.BOOTS;
            case "Necklace" -> type == SpecificItemType.NECKLACE;
            case "Belt" -> type == SpecificItemType.BELT;
            case "Bracelet" -> type == SpecificItemType.BRACELET;
            case "Cloak" -> type == SpecificItemType.CLOAK;
            case "Gloves" -> type == SpecificItemType.GLOVES;
            case "Pickaxe" -> type == SpecificItemType.PICKAXE;
            case "Drill" -> type == SpecificItemType.DRILL;
            case "Axe" -> type == SpecificItemType.AXE;
            case "Shovel" -> type == SpecificItemType.SPADE;
            case "Shears" -> type == SpecificItemType.SHEARS;
            case "Hoe", "Farming Tool" -> type == SpecificItemType.HOE || "FARMING_TOOL".equals(key);
            case "Tools" -> type == SpecificItemType.PICKAXE || type == SpecificItemType.DRILL || type == SpecificItemType.AXE
                    || type == SpecificItemType.SPADE || type == SpecificItemType.SHEARS || type == SpecificItemType.GAUNTLET
                    || type == SpecificItemType.HOE || "FARMING_TOOL".equals(key);
            case "Vacuum" -> "VACUUM".equals(key);
            default -> label.equals(item.name());
        };
    }

    // Reading

    /** The file in {@code folder}; null, with what's wrong in {@code problems}, if it isn't there or can't be read. */
    public static EnchantmentData read(Path folder, List<String> problems) {
        Path file = folder.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            problems.add("no " + file + ", so there are no enchantments");
            return null;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(JsonParser.parseReader(reader).getAsJsonObject(), problems);
        } catch (IOException | RuntimeException e) {
            problems.add(FILE + ": " + e);
            return null;
        }
    }

    /** Format 1 (see tools/hex/README.md); an enchantment that can't be read is a problem, and left out. */
    public static EnchantmentData parse(JsonObject root, List<String> problems) {
        if (!root.has("format") || root.get("format").getAsInt() != 1) {
            problems.add(FILE + " isn't format 1");
            return null;
        }
        Map<String, Entry> entries = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("enchantments").entrySet()) {
            try {
                entries.put(e.getKey(), entry(e.getKey(), e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                problems.add(e.getKey() + ": " + ex);
            }
        }
        List<String> order = new ArrayList<>();
        if (root.has("order")) for (JsonElement id : root.getAsJsonArray("order")) order.add(id.getAsString());
        return new EnchantmentData(entries, order);
    }

    private static Entry entry(String id, JsonObject o) {
        List<Integer> xp = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("xp")) xp.add(e.isJsonNull() ? null : e.getAsInt());
        Map<Integer, Level> levels = new TreeMap<>();
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("levels").entrySet()) {
            JsonObject l = e.getValue().getAsJsonObject();
            List<String> lines = l.has("lines") ? strings(l, "lines") : null;
            String rarity = text(l, "rarity");
            levels.put(Integer.parseInt(e.getKey()), new Level(text(l, "text"), lines,
                    rarity == null ? null : Rarity.valueOf(rarity.replace(' ', '_')), text(l, "tier_up")));
        }
        JsonElement table = o.get("table");
        return new Entry(id, o.get("hypixel").getAsString(), o.get("name").getAsString(), o.get("ultimate").getAsBoolean(),
                o.has("removed") && o.get("removed").getAsBoolean(), o.get("min").getAsInt(), o.get("max").getAsInt(),
                table == null || table.isJsonNull() ? 0 : table.getAsInt(), xp, o.get("enchanting").getAsInt(), strings(o, "applies"),
                strings(o, "conflicts"), levels);
    }

    private static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
        return out;
    }

    private static String text(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }
}
