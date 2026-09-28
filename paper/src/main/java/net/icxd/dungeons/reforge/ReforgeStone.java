package net.icxd.dungeons.reforge;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.enums.Rarity;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * A Reforge Stone: the item, the reforge it applies, what it goes on and its coin fee at each rarity it goes on
 * (the Blacksmith's and the Hex's "Cost", the fee first, then the stone).
 *
 * @param item  the stone's SkyBlock id ("WITHER_BLOOD")
 * @param type  the kind of item it goes on, as the data names it ("SWORD", "ARMOR"; see {@link #TYPES}); null when
 *              {@code items} says
 * @param items the items it goes on, by id, for a stone of only those (the Warped Stone's Aspects); empty when
 *              {@code type} says
 */
public record ReforgeStone(String item, Reforge reforge, String type, Set<String> items, Map<Rarity, Long> costs) {
    /**
     * The kinds of item the data's types are, by the items' Hypixel type (SkyHanni's ReforgeApi, which reads the
     * same NEU names): a pool's or a stone's "SWORD/ROD" is a sword, longsword, gauntlet or fishing rod.
     * UNKNOWN: a Hoe, taken as a Farming Tool (no item is one yet).
     */
    public static final Map<String, Set<String>> TYPES = Map.ofEntries(
            Map.entry("SWORD", Set.of("SWORD", "LONGSWORD", "GAUNTLET")),
            Map.entry("SWORD/ROD", Set.of("SWORD", "LONGSWORD", "GAUNTLET", "FISHING_ROD")),
            Map.entry("BOW", Set.of("BOW")),
            Map.entry("ROD", Set.of("FISHING_ROD")),
            Map.entry("ARMOR", Set.of("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "CARNIVAL_MASK")),
            Map.entry("HELMET", Set.of("HELMET")),
            Map.entry("CHESTPLATE", Set.of("CHESTPLATE")),
            Map.entry("EQUIPMENT", Set.of("NECKLACE", "CLOAK", "BELT", "GLOVES", "BRACELET")),
            Map.entry("CLOAK", Set.of("CLOAK")),
            Map.entry("BELT", Set.of("BELT")),
            Map.entry("PICKAXE", Set.of("PICKAXE", "DRILL", "GAUNTLET")),
            Map.entry("AXE", Set.of("AXE")),
            Map.entry("FARMING_TOOL", Set.of("FARMING_TOOL", "HOE")),
            Map.entry("VACUUM", Set.of("VACUUM")),
            Map.entry("FISHING_NET", Set.of("FISHING_NET")));
    /**
     * Types whose items never say "This item can be reforged!" (so the item data has them as not reforgeable),
     * though their stones go on them: live vacuums have Beady and Buzzing, and nets Sticky.
     */
    private static final Set<String> UNLABELLED = Set.of("VACUUM", "FISHING_NET");

    public ReforgeStone {
        items = Set.copyOf(items);
        Map<Rarity, Long> copy = new EnumMap<>(Rarity.class);
        copy.putAll(costs);
        costs = Collections.unmodifiableMap(copy);
    }

    /** Whether it goes on this kind of item (at some rarity). */
    public boolean fits(SkyBlockItem item) {
        if (!items.isEmpty()) return items.contains(item.id());
        return isOfType(item, type) && (item.reforgeable() || UNLABELLED.contains(type));
    }

    /** Whether it goes on this item at this rarity (its current one): only at the rarities it has a fee for. */
    public boolean fits(SkyBlockItem item, Rarity rarity) {
        return costs.containsKey(rarity) && fits(item);
    }

    /** The fee at that rarity; null if it doesn't go on an item of it. */
    public Long cost(Rarity rarity) {
        return costs.get(rarity);
    }

    /** Whether the item is of the data's type ("SWORD/ROD"), by its Hypixel type. */
    static boolean isOfType(SkyBlockItem item, String type) {
        Set<String> kinds = TYPES.get(type);
        return kinds != null && kinds.contains(item.typeKey());
    }
}
