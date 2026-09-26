package net.icxd.dungeons.item;

import net.icxd.dungeons.item.ability.Ability;
import net.icxd.dungeons.item.cost.UpgradeCosts;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.enums.GenericItemType;
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
    /** The rarity line's words after the rarity ("ORE") when they aren't the type's name; null for the type's name. */
    default String typeLabel() { return null; }
    /** Its Hypixel type, for grouping and filtering ("SWORD", "SACK"); "OTHER" if it has none. */
    default String typeKey() { return specificItemType() == SpecificItemType.NONE ? "OTHER" : specificItemType().name(); }

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
    /** Abilities and bonuses from data, shown where {@link #ability()} is; Java items use ability(). */
    default List<ItemBlock> blocks() { return List.of(); }

    /** "This item can be reforged!" while it has no reforge. Weapons, armor, tools and equipment can. */
    default boolean reforgeable() {
        GenericItemType type = genericItemType();
        return type == GenericItemType.WEAPON || type == GenericItemType.ARMOR || type == GenericItemType.TOOL
                || type == GenericItemType.EQUIPMENT;
    }

    /**
     * Whether its stats count while it's held in the main hand. Armor, equipment, accessories and arrows
     * don't; the gauntlet, rods and untyped tools and staffs do, as on Hypixel.
     */
    default boolean statsWhenHeld() {
        if (genericItemType() == GenericItemType.ARMOR) return false;
        return switch (specificItemType()) {
            case CLOAK, BELT, NECKLACE, GLOVES, BRACELET, ACCESSORY, ARROW, ARROW_POISON -> false;
            // Masks are worn as helmets.
            default -> !"CARNIVAL_MASK".equals(typeKey());
        };
    }

    default Soulbound soulbound() { return Soulbound.NONE; }
    default Requirements requirements() { return null; }
    default UpgradeCosts upgradeCosts() { return null; }

    default boolean canHaveAttributes() { return false; }
    default boolean dungeonItem() { return false; }
    default boolean unstackable() { return false; }
    default double npcSellPrice() { return 0; }

    default boolean isOwnable() { return requirements() != null; }
}
