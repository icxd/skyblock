package net.icxd.dungeons.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.command.commands.admin.PlayerDataCommand;
import net.icxd.dungeons.user.User;

/**
 * Keeps the inventory saved in Mongo (see StoredInventory) in step with the player's.
 */
public class InventorySyncListener implements Listener {

    /**
     * A container open when the player disconnects: vanilla drops the cursor item and the
     * container's inputs right after this, before the player quits. They go into the inventory.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisconnectClose(InventoryCloseEvent event) {
        if (event.getReason() != InventoryCloseEvent.Reason.DISCONNECT || !(event.getPlayer() instanceof Player player)) return;
        User user = User.cached(player.getUniqueId());
        if (user != null) Dungeons.getUserStore().rescueLooseItems(player, user);
    }

    /** The drops are in the world now; save the emptied inventory so a crash can't bring them back too. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> {
            User user = User.cached(player.getUniqueId());
            if (player.isOnline() && user != null && user.isLoaded()) user.save();
        });
    }

    // A player whose data has been handed off is on their way to another server: what happens to
    // their items here isn't saved, so nothing may leave or enter their inventory.

    private static boolean frozen(HumanEntity player) {
        User user = User.cached(player.getUniqueId());
        return user != null && user.isReleased();
    }

    private static void freeze(HumanEntity player, Cancellable event) {
        if (frozen(player)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent event) {
        freeze(event.getPlayer(), event);
    }

    /** Before the pickup events other listeners act on. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        freeze(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PlayerDataCommand.ItemsView) {
            event.setCancelled(true);
            return;
        }
        freeze(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PlayerDataCommand.ItemsView) {
            event.setCancelled(true);
            return;
        }
        freeze(event.getWhoClicked(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        freeze(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        freeze(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        freeze(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        freeze(event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsume(PlayerItemConsumeEvent event) {
        freeze(event.getPlayer(), event);
    }
}
