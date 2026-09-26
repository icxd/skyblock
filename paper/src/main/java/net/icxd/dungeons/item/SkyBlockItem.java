package net.icxd.dungeons.item;

import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.enchanting.Enchantment;
import net.icxd.dungeons.item.enums.GenericItemType;
import net.icxd.dungeons.item.enums.Rarity;
import net.icxd.dungeons.item.enums.Soulbound;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.item.gemstone.GemstoneSlots;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.item.requirement.Requirements;
import net.icxd.dungeons.stats.Stats;
import org.bukkit.Color;
import org.bukkit.Material;

import java.util.List;

/**
 * A kind of SkyBlock item: what it is and what it says. {@link ItemBuilder} turns one into an item
 * stack, laid out the way Hypixel lays out item tooltips. Text is written with {@code &} colour codes;
 * lines Hypixel writes by hand are given line by line, as Hypixel has them.
 */
public interface SkyBlockItem {
    /** What the item is stored as ("HYPERION"). */
    String id();
    String name();
    Material material();

    default Rarity rarity() { return Rarity.COMMON; }
    /** A player head's texture (the base64 "textures" property value). */
    default String skin() { return null; }
    /** Leather armor's colour. */
    default Color color() { return null; }
    /** Enchanted-looking even without enchantments. */
    default boolean glowing() { return false; }

    /** What the rarity line calls it ("SWORD", "GEMSTONE"); NONE for just the rarity. */
    default SpecificItemType specificItemType() { return SpecificItemType.NONE; }
    default GenericItemType genericItemType() { return GenericItemType.get(specificItemType()); }

    /** Dark gray lines right under the name, such as "Collection Item" or "Drill Part". */
    default List<String> categories() { return List.of(); }
    /** 0 for none. Hypixel doesn't publish how it's worked out, so items give theirs. */
    default int gearScore() { return 0; }
    default Stats stats() { return new Stats(); }
    /** Seconds between shots for a shortbow ("Shot Cooldown: 0.5s"); 0 if it isn't one. */
    default double shotCooldown() { return 0; }
    default GemstoneSlots gemstoneSlots() { return null; }

    /** The item's own text, between its enchantments and abilities: a line each, "" for a blank line. */
    default List<String> lore() { return List.of(); }
    default Ability ability() { return null; }
    /** More text that depends on the item's data (drill parts, scroll abilities); null for none. */
    default List<String> nbtLore(NBTTagCompound tag) { return null; }

    /** "This item can be reforged!" while it has no reforge. Weapons, armor, tools and equipment can. */
    default boolean reforgeable() {
        GenericItemType type = genericItemType();
        return type == GenericItemType.WEAPON || type == GenericItemType.ARMOR || type == GenericItemType.TOOL
                || type == GenericItemType.EQUIPMENT;
    }

    default Soulbound soulbound() { return Soulbound.NONE; }
    default Requirements requirements() { return null; }
    default UpgradeCosts upgradeCosts() { return null; }

    default boolean canHaveAttributes() { return false; }
    default boolean dungeonItem() { return false; }
    default boolean unstackable() { return false; }
    default double npcSellPrice() { return 0; }

    /** Data a new item of this kind starts with, besides what every item has. */
    default NBTTagCompound nbt() { return null; }

    default List<Enchantment> enchantments() { return null; }

    default boolean isOwnable() { return requirements() != null; }
}
