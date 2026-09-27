package net.icxd.dungeons.item.behaviour;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.stats.Stats;

import java.util.List;

/**
 * What Java code adds to an item beyond its data (see {@link ItemBehaviours}): data a new one starts with,
 * and how its text and abilities change with its data, such as a Necron's blade's scrolls. The item's own
 * text comes from items.json; a behaviour only changes it. {@link net.icxd.dungeons.item.ItemBuilder} shows
 * what these return, and the ability listener uses the blocks.
 */
public interface ItemBehaviour {
    /** Data a new item of this kind starts with, besides what every item has; null for none. */
    default NBTTagCompound nbt(SkyBlockItem item) {
        return null;
    }

    /** The item's own text, with this data: {@code lore} (the item's), or a changed copy. */
    default List<String> lore(SkyBlockItem item, NBTTagCompound tag, List<String> lore) {
        return lore;
    }

    /** Its abilities and bonuses, with this data: {@code blocks} (the item's), or a changed copy. */
    default List<ItemBlock> blocks(SkyBlockItem item, NBTTagCompound tag, List<ItemBlock> blocks) {
        return blocks;
    }

    /** What holding it does to the holder's finished stats (everything else already in them); nothing unless it says. */
    default void whileHeld(Stats stats) {
    }
}
