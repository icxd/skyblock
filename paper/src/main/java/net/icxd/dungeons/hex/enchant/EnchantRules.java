package net.icxd.dungeons.hex.enchant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enchanting.EnchantmentData;
import net.icxd.dungeons.item.enchanting.EnchantmentData.Entry;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.nbt.NBTTagList;
import net.icxd.dungeons.utils.Utils;

/**
 * The Hex's enchanting rules, as plain functions of the table and an item's data: which enchantments it offers
 * for an item, how many can be on it at once, and what choosing a level does (the wiki's Enchantments and The Hex:
 * clicking the level it has removes it, a higher one upgrades it, a conflicting enchantment is replaced, only one
 * ultimate goes on an item, and One For All "removes all other enchants").
 */
public final class EnchantRules {
    private EnchantRules() {
    }

    /** What choosing a level does. */
    public enum Action {
        /** It isn't on the item: it goes on. */
        APPLY,
        /** A lower level is on it: this one replaces it. */
        UPGRADE,
        /** That very level is on it: it comes off (for the same Exp, as the wiki's Enchantments says). */
        REMOVE,
        /**
         * A higher level is on it. UNKNOWN (what Hypixel does; NEU's Hex hides these books as a "Bad Level"): nothing,
         * and it says so; remove the higher one first.
         */
        LOWER
    }

    /** A level chosen: what it does, the enchantments after it (id to level, in the item's order), and what it takes off besides. */
    public record Change(Action action, Map<String, Integer> after, List<String> replaced) {
    }

    /** The item's enchantments, id to level, in its order (each by the plugin's id, also one stored under Hypixel's). */
    public static Map<String, Integer> on(NBTTagCompound tag) {
        Map<String, Integer> on = new LinkedHashMap<>();
        if (tag == null) return on;
        NBTTagList list = tag.getList("enchantments", 10);
        for (int i = 0; i < list.size(); i++) on.put(EnchantmentData.id(list.get(i).getString("name")), list.get(i).getInt("lvl"));
        return on;
    }

    /**
     * The enchantments the Hex lists for an item, the ultimate ones or the rest, in the Default order: those whose
     * books name the item's kind, but not those Hypixel took out of the game.
     */
    public static List<Entry> offered(EnchantmentData data, SkyBlockItem item, boolean ultimate) {
        List<Entry> offered = new ArrayList<>();
        for (String id : data.order()) {
            Entry entry = data.get(id);
            if (entry.ultimate() == ultimate && !entry.removed() && entry.appliesTo(item)) offered.add(entry);
        }
        return offered;
    }

    /**
     * How many of these can be on one item at once: the groups they make when every two that conflict are in one
     * group. The Hex's "Enchantments 0/26" for a sword (the wiki's Weapon tab): its 34 enchantments then, less the
     * 8 that conflict with another (Sharpness, Smite and Bane of Arthropods; Life Steal, Drain and Mana Steal; Giant
     * and Titan Killer; Execute and Prosecute; First Strike and Triple-Strike; Thunderlord and Thunderbolt).
     */
    public static int groups(List<String> ids, BiPredicate<String, String> conflict) {
        Map<String, String> parent = new HashMap<>();
        for (String id : ids) parent.put(id, id);
        for (int i = 0; i < ids.size(); i++) {
            for (int j = i + 1; j < ids.size(); j++) {
                if (conflict.test(ids.get(i), ids.get(j))) parent.put(root(parent, ids.get(i)), root(parent, ids.get(j)));
            }
        }
        int groups = 0;
        for (String id : ids) if (root(parent, id).equals(id)) groups++;
        return groups;
    }

    private static String root(Map<String, String> parent, String id) {
        String root = id;
        while (!parent.get(root).equals(root)) root = parent.get(root);
        return root;
    }

    /**
     * The main menu's line for the normal enchantments: "  &7Enchantments &e<on it>&7/&a<at most>", how many of those
     * the Hex lists are on it, of how many can be at once ({@link #groups}); the count green once it's all of them
     * (the official screenshot's "10/10"; UNKNOWN: its colour before that, the wiki's &e for none).
     */
    public static String summary(EnchantmentData data, SkyBlockItem item, Map<String, Integer> on) {
        List<Entry> offered = offered(data, item, false);
        List<String> ids = offered.stream().map(Entry::id).toList();
        int have = (int) ids.stream().filter(on::containsKey).count();
        int max = groups(ids, data::conflict);
        return "  &7Enchantments " + count(have, max);
    }

    /** "  &7Ultimate Enchantments &e0&7/&a1": only one ultimate goes on an item. */
    public static String ultimateSummary(EnchantmentData data, Map<String, Integer> on) {
        int have = (int) on.keySet().stream().map(data::get).filter(e -> e != null && e.ultimate()).count();
        return "  &7Ultimate Enchantments " + count(have, 1);
    }

    private static String count(int have, int max) {
        return (have >= max ? "&a" : "&e") + have + "&7/&a" + max;
    }

    /**
     * What choosing {@code level} of {@code entry} does to an item with these enchantments. Going on (or up): what it
     * conflicts with comes off (the wiki's Enchantments: "the old enchantment" is "replaced"), and an ultimate takes
     * the other ultimate's place. One For All takes every other enchantment off (its text: "Removes all other
     * enchants"; UNKNOWN whether Hypixel keeps any, NEU's pools spare Champion, Tabasco and Divine Gift); another
     * enchantment only takes One For All off if they conflict.
     */
    public static Change choose(EnchantmentData data, Map<String, Integer> on, Entry entry, int level) {
        Integer current = on.get(entry.id());
        Map<String, Integer> after = new LinkedHashMap<>(on);
        if (current != null && current == level) {
            after.remove(entry.id());
            return new Change(Action.REMOVE, after, List.of());
        }
        if (current != null && current > level) return new Change(Action.LOWER, on, List.of());
        List<String> replaced = new ArrayList<>();
        for (String id : on.keySet()) {
            if (id.equals(entry.id())) continue;
            Entry other = data.get(id);
            boolean off = "one_for_all".equals(entry.id()) || data.conflict(entry.id(), id)
                    || entry.ultimate() && other != null && other.ultimate();
            if (off) replaced.add(id);
        }
        replaced.forEach(after::remove);
        after.put(entry.id(), level);
        return new Change(current == null ? Action.APPLY : Action.UPGRADE, after, replaced);
    }

    /**
     * The item's data with these enchantments: those it keeps stay as they're stored (name and place), a changed
     * level is changed in place, and a new one goes last. A level is stored as an int, as Hypixel's are.
     */
    public static NBTTagCompound with(NBTTagCompound tag, Map<String, Integer> enchantments) {
        NBTTagCompound out = tag.copy();
        NBTTagList old = tag.getList("enchantments", 10);
        NBTTagList list = new NBTTagList();
        List<String> kept = new ArrayList<>();
        for (int i = 0; i < old.size(); i++) {
            NBTTagCompound e = old.get(i);
            String id = EnchantmentData.id(e.getString("name"));
            Integer level = enchantments.get(id);
            if (level == null || kept.contains(id)) continue;
            NBTTagCompound copy = e.copy();
            if (copy.getInt("lvl") != level) copy.setInt("lvl", level);
            list.add(copy);
            kept.add(id);
        }
        for (Map.Entry<String, Integer> e : enchantments.entrySet()) {
            if (kept.contains(e.getKey())) continue;
            NBTTagCompound enchantment = new NBTTagCompound();
            enchantment.setString("name", e.getKey());
            enchantment.setInt("lvl", e.getValue());
            list.add(enchantment);
        }
        out.set("enchantments", list);
        return out;
    }

    /** "&aSharpness VI", an ultimate "&d&lOne For All I": a level's book (UNKNOWN: the colour of a normal one; the Enchantments Guide's green). */
    public static String bookName(Entry entry, int level) {
        return (entry.ultimate() ? "&d&l" : "&a") + entry.name() + " " + Utils.getRomanNumeral(level);
    }

    /** "&9Sharpness VI" or "&d&lOne For All I", as items show them (see Enchantment#getDisplayName). */
    public static String displayName(Entry entry, int level) {
        return (entry.ultimate() ? "&d&l" : "&9") + entry.name() + " " + Utils.getRomanNumeral(level);
    }

    /** The Enchanting XP for spending this many Exp levels: 3.5 X^1.5 (the wiki's Enchanting). */
    public static double enchantingXp(int levels) {
        return levels <= 0 ? 0 : 3.5 * Math.pow(levels, 1.5);
    }
}
