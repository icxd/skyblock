package net.icxd.dungeons.pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.ItemRegistry;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.skill.Skill;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * What a pet's held item (a Pet Item, by its id) shows and does, read from its own lines in the items data: its
 * description (what the Pets menu shows under "Held Item:", as recorded), the stats it grants ("Grants &f+10❂ True
 * Defense": the Dwarf Turtle Shelmet's +10 is in the pet's True Defense line, recorded) and the pet XP it adds
 * ("Gives &a+40% &7pet exp for &aMining&7."). What else items do (a share of the pet's stats, the Tier Boost's rarity,
 * Exp Share) isn't here. No server needed past the item registry.
 */
public final class HeldItems {
    /** "&7Grants &f+10❂ True Defense": the colour, the amount, the stat's name (till its colour or punctuation ends it). */
    private static final Pattern GRANTS = Pattern.compile("Grants (?:&[0-9a-f])?\\+([\\d,.]+)\\S* ([A-Z][A-Za-z ]*?[a-z])(?=\\s*&|[.,]|\\s+and\\b|\\s*$)");
    private static final Pattern EXP = Pattern.compile("Gives (?:&[0-9a-f])?\\+(\\d+(?:\\.\\d+)?)% (?:&[0-9a-f])?pet exp for (?:&[0-9a-f])?(all skills|[A-Z][a-z]+)");
    /** The lines every Pet Item starts with, up to "The pet must be visible to apply the item!". */
    private static final String HEADER_END = "The pet must be visible";
    private static final String FOOTER = "Right-click on your summoned pet";

    private HeldItems() {
    }

    public static SkyBlockItem item(String id) {
        return id == null ? null : ItemRegistry.get(id);
    }

    /** What it does, as its lines say: between the Pet Items' header and "Right-click on your summoned pet to give it this item!". */
    public static List<String> description(SkyBlockItem item) {
        if (item == null) return List.of();
        List<String> lore = item.lore();
        int start = 0;
        for (int i = 0; i < lore.size(); i++) {
            if (lore.get(i).contains(HEADER_END)) {
                start = i + 1;
                break;
            }
        }
        while (start < lore.size() && lore.get(start).isEmpty()) start++;
        int end = lore.size();
        for (int i = start; i < lore.size(); i++) {
            if (lore.get(i).contains(FOOTER)) {
                end = i;
                break;
            }
        }
        while (end > start && lore.get(end - 1).isEmpty()) end--;
        return new ArrayList<>(lore.subList(start, end));
    }

    private static String joined(SkyBlockItem item) {
        return String.join(" ", description(item));
    }

    /** The stats it grants the pet's owner (none for one that doesn't, or isn't an item here). */
    public static Stats stats(String id) {
        Stats stats = new Stats();
        SkyBlockItem item = item(id);
        if (item == null) return stats;
        Matcher m = GRANTS.matcher(joined(item));
        while (m.find()) {
            Stat stat = stat(m.group(2).trim());
            if (stat == null) continue;
            try {
                stats.add(stat, Double.parseDouble(m.group(1).replace(",", "")));
            } catch (NumberFormatException ignored) {
                // Not a number after all: nothing.
            }
        }
        return stats;
    }

    /** How much more pet XP it gives for a skill's XP, as a fraction ("+40%": 0.4); 0 for none. */
    public static double expBoost(String id, Skill skill) {
        SkyBlockItem item = item(id);
        if (item == null) return 0;
        Matcher m = EXP.matcher(joined(item));
        double boost = 0;
        while (m.find()) {
            String target = m.group(2);
            if (target.equals("all skills") || (skill != null && target.toUpperCase(Locale.ROOT).equals(skill.name()))) {
                boost += Double.parseDouble(m.group(1)) / 100;
            }
        }
        return boost;
    }

    private static Stat stat(String name) {
        for (Stat stat : Stat.values()) if (stat.getDisplayName().equals(name) && !stat.name().startsWith("RIFT_")) return stat;
        return null;
    }
}
