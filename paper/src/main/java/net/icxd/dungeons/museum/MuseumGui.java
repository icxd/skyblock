package net.icxd.dungeons.museum;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.gui.GUI;
import net.icxd.dungeons.gui.item.GUIClickableItem;

/**
 * A Museum menu: a click on an item in their own inventory while it's open asks to donate it ("Click on this item in
 * your inventory to add it to your Museum!"), on the next tick, and the donation comes back to this menu. Main thread.
 */
abstract class MuseumGui extends GUI {
    protected final Player viewer;

    MuseumGui(String title, int size, Player viewer) {
        super(title, size);
        this.viewer = viewer;
    }

    /** This menu again, as it is now (where a donation from it, or its Cancel, comes back to). */
    abstract void reopen(Player player);

    @Override
    public boolean onPlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
        ItemStack cursor = event.getCursor();
        if (!GUIClickableItem.pressed(event.getClick()) || (cursor != null && !cursor.isEmpty())) return true;
        int slot = event.getSlot();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (viewer.isOnline() && GUI_MAP.get(viewer.getUniqueId()) == this) MuseumDonations.clicked(viewer, slot, this::reopen);
        });
        return true;
    }
}
