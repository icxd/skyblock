package net.icxd.dungeons.gui.item;

import net.icxd.dungeons.utils.StackBuilder;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;


public interface GUIClickableItem extends GUIItem {
    void run(InventoryClickEvent event);

    static GUIClickableItem close(int slot) {
        return new GUIClickableItem() {
            @Override public void run(InventoryClickEvent event) { event.getWhoClicked().closeInventory(); }
            @Override public int slot() { return slot; }
            @Override public ItemStack stack() {
                return new StackBuilder(Material.BARRIER)
                        .setDisplayName("&cClose")
                        .build();
            }
        };
    }
}
