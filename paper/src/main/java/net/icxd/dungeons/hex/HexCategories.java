package net.icxd.dungeons.hex;

import java.util.List;

import net.icxd.dungeons.hex.category.Books;
import net.icxd.dungeons.hex.category.Enchantments;
import net.icxd.dungeons.hex.category.Gemstones;
import net.icxd.dungeons.hex.category.ItemUpgrades;
import net.icxd.dungeons.hex.category.Modifiers;
import net.icxd.dungeons.hex.category.Reforges;
import net.icxd.dungeons.hex.category.UltimateEnchantments;

/**
 * The Hex's seven categories, in their order: the order of the main menu's buttons and of the panes' summary
 * (the official screenshot's summary and the wiki's table of categories): Enchantments, Ultimate Enchantments,
 * Books, Modifiers, Reforges, Item Upgrades, Gemstones. One of each, made when first asked for.
 */
public final class HexCategories {
    public static final HexCategory ENCHANTMENTS = new Enchantments();
    public static final HexCategory ULTIMATE_ENCHANTMENTS = new UltimateEnchantments();
    public static final HexCategory BOOKS = new Books();
    public static final HexCategory MODIFIERS = new Modifiers();
    public static final HexCategory REFORGES = new Reforges();
    public static final HexCategory ITEM_UPGRADES = new ItemUpgrades();
    public static final HexCategory GEMSTONES = new Gemstones();

    private static final List<HexCategory> ALL = List.of(ENCHANTMENTS, ULTIMATE_ENCHANTMENTS, BOOKS, MODIFIERS, REFORGES, ITEM_UPGRADES, GEMSTONES);

    private HexCategories() {
    }

    public static List<HexCategory> all() {
        return ALL;
    }
}
