package net.icxd.dungeons.dungeons.instance;

import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;

import net.icxd.dungeons.listeners.InventorySyncListener;
import net.icxd.dungeons.mob.MobKinds;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;

/**
 * What players do with a run's secrets ({@link RunSecrets}): right clicks on chests, heads and levers, item
 * secrets picked up, secret bats killed, and the item chests' menus, which only give (so nothing of a
 * Sandbox profile's is left in the world through one). Registered by {@link RunManager}.
 */
final class SecretEvents implements Listener {
    private final RunManager manager;

    SecretEvents(RunManager manager) {
        this.manager = manager;
    }

    /** The secrets of the run they're in while it's on; null otherwise. */
    private RunSecrets secrets(Player player) {
        DungeonRun run = manager.runOf(player);
        return run == null || run.phase() != DungeonRun.Phase.RUNNING ? null : run.secrets();
    }

    private RunSecrets secrets(World world) {
        DungeonRun run = manager.runIn(world);
        return run == null || run.phase() != DungeonRun.Phase.RUNNING ? null : run.secrets();
    }

    /** First, so neither the block's own use (a chest's menu, a lever) nor the item in hand (an ability) happens. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        RunSecrets secrets = secrets(player);
        if (secrets == null || !secrets.isSecret(event.getClickedBlock()) || InventorySyncListener.frozen(player)) return;
        if (event.getHand() != EquipmentSlot.HAND || secrets.click(player, event.getClickedBlock())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTryPickup(PlayerAttemptPickupItemEvent event) {
        Item item = event.getItem();
        RunSecrets secrets = secrets(event.getPlayer());
        if (secrets != null && secrets.isSecret(item) && event.getRemaining() >= item.getItemStack().getAmount()) {
            secrets.noSpace(event.getPlayer());
        }
    }

    /** Before PlayerListener, which puts SkyBlock items in the inventory itself (and cancels the pickup). */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RunSecrets secrets = secrets(player);
        if (secrets != null && secrets.isSecret(event.getItem())) secrets.pickedUp(player, event.getItem());
    }

    /** An item secret stays one item on its own. */
    @EventHandler(ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        RunSecrets secrets = secrets(event.getEntity().getWorld());
        if (secrets != null && (secrets.isSecret(event.getEntity()) || secrets.isSecret(event.getTarget()))) event.setCancelled(true);
    }

    @EventHandler
    public void onBat(SkyBlockMobDeathEvent event) {
        if (event.kind() != MobKinds.SECRET_BAT) return;
        RunSecrets secrets = secrets(event.location().getWorld());
        if (secrets != null && event.mob() != null) secrets.batDied(event.mob(), event.killer(), event.location());
    }

    /** The blessing names over opened chests are invisible armor stands: nothing goes on them. */
    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        RunSecrets secrets = secrets(event.getRightClicked().getWorld());
        if (secrets != null && secrets.isName(event.getRightClicked())) event.setCancelled(true);
    }

    // An item chest's menu: taking out only.

    @EventHandler(ignoreCancelled = true)
    public void onMenuClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof RunSecrets.ChestMenu)) return;
        boolean inChest = top.equals(event.getClickedInventory());
        switch (event.getAction()) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME, DROP_ALL_SLOT, DROP_ONE_SLOT, DROP_ALL_CURSOR, DROP_ONE_CURSOR, NOTHING -> {
            }
            // Shift-clicking takes it out; from their own inventory it would put something in.
            case MOVE_TO_OTHER_INVENTORY -> event.setCancelled(!inChest);
            // Gathering onto the cursor could take from the chest, but never puts anything in it.
            case COLLECT_TO_CURSOR -> {
            }
            default -> event.setCancelled(inChest);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMenuDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder(false) instanceof RunSecrets.ChestMenu)) return;
        for (int slot : event.getRawSlots()) {
            if (slot < top.getSize()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onMenuClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder(false) instanceof RunSecrets.ChestMenu menu) menu.closed();
    }
}
