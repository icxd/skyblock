package net.icxd.dungeons.storage;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import net.icxd.dungeons.Dungeons;

/**
 * Storage's pages in the server's events (see {@link ItemPage}): what a page doesn't take can't be put in
 * it, by a click with it on the cursor or a number key from the hotbar (a shift-click is the page's own,
 * see ItemPage#onPlayerInventoryClick), nor anything from or into a bundle; the page is written back on
 * the tick after any click while it's open. And what's kept of a player goes when they leave.
 */
public final class StorageListener implements Listener {
    /** After GUIListener, which has cancelled a click on a button. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemPage page = ItemPage.current(player);
        if (page == null || event.getClickedInventory() != event.getView().getTopInventory() || !page.isItemSlot(event.getSlot())) return;
        ItemStack coming = switch (event.getAction()) {
            case PLACE_ALL, PLACE_SOME, PLACE_ONE, SWAP_WITH_CURSOR -> event.getCursor();
            // 0 to 8, or 40 for the off-hand.
            case HOTBAR_SWAP -> player.getInventory().getItem(event.getHotbarButton());
            case PICKUP_FROM_BUNDLE, PICKUP_ALL_INTO_BUNDLE, PICKUP_SOME_INTO_BUNDLE, PLACE_FROM_BUNDLE, PLACE_ALL_INTO_BUNDLE,
                 PLACE_SOME_INTO_BUNDLE, UNKNOWN -> {
                event.setCancelled(true);
                yield null;
            }
            default -> null;
        };
        if (coming != null && !coming.isEmpty() && !page.accepts(event.getSlot(), coming)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !(ItemPage.current(player) instanceof ItemPage page)) return;
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            if (ItemPage.current(player) == page) page.sync();
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Storage.forget(event.getPlayer().getUniqueId());
    }
}
