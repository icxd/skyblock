package net.icxd.dungeons.item;

import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Which items are dungeon items: the kinds the item data says are, and any item made one since ("Convert to
 * Dungeon Item" at Malik or in the Hex, which only some kinds can be: the API's conversion costs). A made one
 * keeps the flag in its data, as Hypixel's {@code dungeon_item} does, and is then a dungeon item in every way:
 * its stars' dungeon bonus, the gray bracket, "DUNGEON" on its rarity line, the enchantments three a line.
 */
public final class DungeonItems {
    /** The flag every item's data has (see ItemBuilder#newData): the kind's, or true once it's been made one. */
    public static final String FLAG = "dungeon_item";

    private DungeonItems() {
    }

    /** Whether this item is a dungeon item: its kind is, or it was made one ({@code tag} null: only its kind). */
    public static boolean is(SkyBlockItem item, NBTTagCompound tag) {
        return item.dungeonItem() || tag != null && tag.getBoolean(FLAG);
    }

    /** Whether it can be made one now: its kind can be, and it isn't one yet. */
    public static boolean convertible(SkyBlockItem item, NBTTagCompound tag) {
        return item.dungeonConversionCost() != null && !is(item, tag);
    }

    /** Makes it one: its data says so from now on. */
    public static void convert(NBTTagCompound tag) {
        tag.setBoolean(FLAG, true);
    }
}
