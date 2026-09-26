package net.icxd.dungeons.item.data;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.List;

/**
 * A SkyBlock item from items.json (see {@link ItemData}): everything it says is data. It has no
 * {@link #ability()}: its abilities are {@link #blocks()}, text only.
 *
 * @param specificItemType NONE when its type isn't one of those (see {@link #typeKey} for what it is)
 * @param typeKey its Hypixel type ("SWORD", "SACK"), "OTHER" if it has none
 * @param typeLabel what its rarity line says after the rarity; null for its type's name
 * @param reforgeableData whether "This item can be reforged!" shows; null for the usual rule
 */
public record DataItem(String id, String name, Material material, Rarity rarity, SpecificItemType specificItemType,
                       String typeKey, String typeLabel, List<String> categories, String skin, Color color, boolean glowing,
                       boolean unstackable, boolean dungeonItem, boolean canHaveAttributes, Boolean reforgeableData,
                       Soulbound soulbound, int gearScore, double npcSellPrice, Stats stats, double shotCooldown,
                       GemstoneSlots gemstoneSlots, UpgradeCosts upgradeCosts, Requirements requirements, List<String> lore,
                       List<ItemBlock> blocks) implements SkyBlockItem {
    public DataItem {
        categories = List.copyOf(categories);
        stats = stats.copy();
        lore = List.copyOf(lore);
        blocks = List.copyOf(blocks);
    }

    /** A copy: Stats is mutable, and what callers get is theirs to add to. */
    @Override
    public Stats stats() {
        return stats.copy();
    }

    @Override
    public boolean reforgeable() {
        return reforgeableData != null ? reforgeableData : SkyBlockItem.super.reforgeable();
    }
}
