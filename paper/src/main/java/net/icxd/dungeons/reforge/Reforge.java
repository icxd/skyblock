package net.icxd.dungeons.reforge;

import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A reforge: what an item's {@code reforge} data names, its name before the item's, the stats it adds at the item's
 * rarity (the lore's {@code &9(+N)}) and its bonus's text ("&9Withered Bonus"). The reforges are Hypixel's, read
 * from the private data (see {@link ReforgeTable}); an item keeps the one it has when the table doesn't know it (or
 * isn't there): its name is made from the data's word, and it gives nothing.
 *
 * @param id         Hypixel's {@code modifier} ("withered", "double_bit"), what the item's data holds
 * @param bonusTitle the bonus's heading, "&9Withered Bonus" (Gilded's is its own)
 * @param bonus      the bonus's lines at each rarity that has them
 * @param perLevel   what it adds a Catacombs level (Withered's +1 Strength, Ancient's +1 Crit Damage)
 * @param prefixes   the word it shows as on items whose name starts with a key ("Very" Wise Dragon armor)
 */
public record Reforge(String id, String name, ReforgeStats stats, String bonusTitle, Map<Rarity, List<String>> bonus,
                      Map<Stat, Double> perLevel, Map<String, String> prefixes) {
    public Reforge {
        Map<Rarity, List<String>> lines = new EnumMap<>(Rarity.class);
        bonus.forEach((rarity, text) -> lines.put(rarity, List.copyOf(text)));
        bonus = Collections.unmodifiableMap(lines);
        Map<Stat, Double> levels = new EnumMap<>(Stat.class);
        levels.putAll(perLevel);
        perLevel = Collections.unmodifiableMap(levels);
        prefixes = Collections.unmodifiableMap(new LinkedHashMap<>(prefixes));
    }

    /** The reforge an item's data names; null for none. Never throws: see {@link #of(String)}. */
    public static Reforge of(NBTTagCompound tag) {
        return tag == null ? null : of(tag.getString("reforge"));
    }

    /**
     * The reforge by what an item's data holds: Hypixel's modifier id ("withered") or, as older items have it, the
     * name in capitals ("WITHERED"), any case. Null for none; one the table doesn't have is {@link #unknown}.
     */
    public static Reforge of(String data) {
        if (data == null || data.isBlank()) return null;
        Reforge known = ReforgeTable.get().reforge(data);
        return known != null ? known : unknown(data);
    }

    /** A reforge the table doesn't have: named from its data ("double_bit" is "Double Bit"), giving nothing. */
    public static Reforge unknown(String data) {
        StringBuilder name = new StringBuilder();
        for (String word : data.trim().toLowerCase(Locale.ROOT).split("[_\\s]+")) {
            if (word.isEmpty()) continue;
            if (!name.isEmpty()) name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return new Reforge(data.trim().toLowerCase(Locale.ROOT), name.toString(), ReforgeStats.NONE, null, Map.of(), Map.of(), Map.of());
    }

    /** "Withered": the word before an item's name, or the one Hypixel uses instead on an item named like it ("Very Wise Dragon Helmet"). */
    public String prefix(String itemName) {
        for (Map.Entry<String, String> e : prefixes.entrySet()) {
            if (itemName.equals(e.getKey()) || itemName.startsWith(e.getKey() + " ")) return e.getValue();
        }
        return name;
    }

    /** One stat it adds to an item of that rarity, for an owner of this Catacombs level (0 for nobody). */
    public double stat(Stat stat, Rarity rarity, int catacombsLevel) {
        return stats.get(stat, rarity) + perLevel.getOrDefault(stat, 0.0) * catacombsLevel;
    }

    /** Everything it adds to an item of that rarity, for an owner of this Catacombs level (0 for nobody). */
    public Stats statsAt(Rarity rarity, int catacombsLevel) {
        Stats out = stats.at(rarity);
        perLevel.forEach((stat, perLevel) -> out.add(stat, perLevel * catacombsLevel));
        return out;
    }

    /**
     * The bonus's lines on an item of that rarity: that rarity's, else the nearest lower one's (an item above the
     * data's rarities keeps the bonus: "only the reforge abilities" on Divine); none if it has no bonus. Special and
     * Very Special have Mythic's, as their stats are, not Divine's (Scraped's +50 Mining Fortune there is +35).
     */
    public List<String> bonusLines(Rarity rarity) {
        for (int i = ReforgeStats.numbersFor(rarity).ordinal(); i >= 0; i--) {
            List<String> lines = bonus.get(Rarity.values()[i]);
            if (lines != null) return lines;
        }
        return List.of();
    }

    /** The lore's bonus section, heading and lines ("&9Withered Bonus", ...); empty if it has none. */
    public List<String> bonusSection(Rarity rarity) {
        List<String> lines = bonusLines(rarity);
        if (lines.isEmpty()) return List.of();
        List<String> section = new ArrayList<>(lines.size() + 1);
        section.add(bonusTitle != null ? bonusTitle : "&9" + name + " Bonus");
        section.addAll(lines);
        return section;
    }
}
