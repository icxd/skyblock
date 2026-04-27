package net.icxd.dungeons.gui.item;

import org.bukkit.inventory.ItemStack;

public interface GUIItem {
    int slot();
    ItemStack stack();
    default boolean pickup() { return false; }
}
