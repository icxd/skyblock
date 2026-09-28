package net.icxd.dungeons.hex.reforge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.reforge.Reforge;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.utils.Text;

/**
 * The words of "The Hex ➜ Reforges" and of the main menu's Reforge line, as data (the wiki's Weapon tab, W-WPN:
 * 167-186; SkyHanni's ReforgeHelper for the chat line): a stone's entry, the Random Basic Reforge button and the
 * summary. Plain, for tests.
 */
public final class ReforgeLore {
    /** "&9Withered &7(&6Legendary&7):", where a stone's own lore turns from what it is to what it gives. */
    private static final Pattern STATS_HEADER = Pattern.compile("&9.+ &7\\(&.[A-Za-z ]+&7\\):");

    public static final List<String> RANDOM = List.of("&7Apply a random, basic Reforge to", "&7the item, like in the Blacksmith", "&7anvil!");
    /** The last line of the random reforge (SkyHanni's ReforgeHelper wants it last), and of a stone's (UNKNOWN, U4: the same). */
    public static final String CLICK = "&eClick to reforge!";
    /** UNKNOWN: our own words, for a stone of the reforge the item has (applying it would change nothing). */
    public static final String ALREADY = "&cThis item already has this reforge!";

    private ReforgeLore() {
    }

    /** The main menu's lines: "  &7Reforge &c✖", or "  &7Reforge &a✔" and "    &9Fabled" (the official screenshot). */
    public static List<String> summary(Reforge reforge) {
        if (reforge == null) return List.of("  &7Reforge &c✖");
        return List.of("  &7Reforge &a✔", "    &9" + reforge.name());
    }

    /**
     * A stone's entry: its own lore up to what it gives (what it is, and its flavour), what its reforge gives an item
     * of this rarity (a Catacombs level's worth for the viewer's level, as the wiki's 147 Strength is Legendary's 135
     * and their 12), its bonus, and the Cost block (the fee, then the stone).
     */
    public static List<String> stone(List<String> stoneLore, Reforge reforge, Rarity rarity, int catacombsLevel, List<String> cost) {
        List<String> lore = new ArrayList<>(description(stoneLore));
        for (List<String> section : List.of(stats(reforge, rarity, catacombsLevel), reforge.bonusSection(rarity), cost)) {
            if (section.isEmpty()) continue;
            if (!lore.isEmpty()) lore.add("");
            lore.addAll(section);
        }
        return lore;
    }

    /** A stone's lore before its "&9Withered &7(&6Legendary&7):" (all of it, if it has none), without blank lines at the end. */
    static List<String> description(List<String> stoneLore) {
        List<String> lines = new ArrayList<>();
        for (String line : stoneLore) {
            if (STATS_HEADER.matcher(line).matches()) break;
            lines.add(line);
        }
        while (!lines.isEmpty() && lines.getLast().isBlank()) lines.removeLast();
        return lines;
    }

    /**
     * "&9Withered &7(&6Legendary&7):" and "&7Strength: &c+147", a line a stat in the order item lore lists them, the
     * value coloured as on items (the item data's stone lore; the wiki's older screenshot has red and green); none
     * if it gives nothing at that rarity (Divine, but on mining tools; UNKNOWN what Hypixel shows then).
     */
    static List<String> stats(Reforge reforge, Rarity rarity, int catacombsLevel) {
        List<String> lines = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            double value = reforge.stat(stat, rarity, catacombsLevel);
            if (value == 0) continue;
            lines.add("&7" + stat.getDisplayName() + ": &" + stat.getLoreColor() + Text.signed(value) + stat.getUnit());
        }
        if (lines.isEmpty()) return lines;
        lines.addFirst("&9" + reforge.name() + " &7(" + rarityName(rarity) + "&7):");
        return lines;
    }

    /** "&6Legendary", "&cVery Special": in its colour, as the stone's lore names it. */
    static String rarityName(Rarity rarity) {
        StringBuilder name = new StringBuilder();
        for (String word : rarity.name().split("_")) {
            if (!name.isEmpty()) name.append(' ');
            name.append(word.charAt(0)).append(word.substring(1).toLowerCase());
        }
        return "&" + rarity.getCode() + name;
    }

    /**
     * The Random Basic Reforge button's lore (the wiki's): what it does, "&7Cost", the price (no ✔, as the wiki shows
     * it) and "&eClick to reforge!", or "&cYou don't have enough Coins!" when they can't pay (the Bazaar-less rule).
     * On a Sandbox profile "&aFree", as every Cost block there.
     */
    public static List<String> random(long price, boolean sandbox, boolean affordable) {
        List<String> lore = new ArrayList<>(RANDOM);
        lore.add("");
        lore.add("&7Cost");
        lore.add(sandbox ? "&aFree" : "&6" + Text.number(price) + " Coins");
        lore.add("");
        lore.add(sandbox || affordable ? CLICK : "&cYou don't have enough Coins!");
        return lore;
    }

    /** A Cost block (HexCosts#lore) with its last lines, the action or what's missing, as {@code line}. */
    static List<String> withAction(List<String> cost, String line) {
        int blank = cost.lastIndexOf("");
        List<String> lore = new ArrayList<>(blank < 0 ? cost : cost.subList(0, blank + 1));
        lore.add(line);
        return lore;
    }

    /** What a Cost block says is missing (its lines after the last blank one), as one chat line. */
    static String missing(List<String> cost) {
        int blank = cost.lastIndexOf("");
        return String.join(" ", cost.subList(blank + 1, cost.size()));
    }

    /**
     * "&aYou reforged your &5Gentle Dreadlord Sword &ainto a &5Heroic Dreadlord Sword&a!" (SkyHanni's ReforgeHelper:
     * "You reforged your .+ into an? .+!"). UNKNOWN: its colours; the names' own.
     */
    public static String reforged(String oldName, String newName) {
        String plain = newName.replaceAll("[&§][0-9a-fk-orA-FK-OR]", "");
        String article = !plain.isEmpty() && "AEIOUaeiou".indexOf(plain.charAt(0)) >= 0 ? "an" : "a";
        return "&aYou reforged your " + oldName + " &ainto " + article + " " + newName + "&a!";
    }
}
