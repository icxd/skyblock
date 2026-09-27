package net.icxd.dungeons.profile;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import net.icxd.dungeons.user.User;

/**
 * Items don't go from a Sandbox profile to a Normal one on the ground: what a Sandbox player drops
 * (or leaves where they die) is marked, and only players on a Sandbox profile pick it up, as Hypixel
 * keeps normal profiles' drops from Ironman players. The same goes for the arrows they shoot, and
 * no mob picks up their drops to drop them again. Their inventories are apart anyway, one per
 * profile; for what the world keeps, see SandboxStorage.
 */
public class SandboxDrops implements Listener {
    private static final NamespacedKey SANDBOX = new NamespacedKey("skyblock", "sandbox_drop");

    static boolean onSandbox(Player player) {
        User user = User.ifLoaded(player.getUniqueId());
        return user != null && user.mode() == ProfileMode.SANDBOX;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (onSandbox(event.getPlayer())) mark(event.getItemDrop());
    }

    /** The drops are stacks until they're in the world; each carries the mark to its entity (see onSpawn). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!onSandbox(event.getPlayer())) return;
        for (ItemStack drop : event.getDrops()) {
            if (drop != null && !drop.isEmpty()) drop.editPersistentDataContainer(data -> data.set(SANDBOX, PersistentDataType.BOOLEAN, true));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(ItemSpawnEvent event) {
        ItemStack stack = event.getEntity().getItemStack();
        if (!stack.getPersistentDataContainer().has(SANDBOX)) return;
        mark(event.getEntity());
        event.getEntity().setItemStack(unmarked(stack));
    }

    /** After the freeze (InventorySyncListener), before the pickup handlers that rebuild SkyBlock items. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickup(PlayerAttemptPickupItemEvent event) {
        Item item = event.getItem();
        if (!isMarked(item)) return;
        if (!onSandbox(event.getPlayer())) {
            event.setCancelled(true);
            event.setFlyAtPlayer(false);
            return;
        }
        ItemStack stack = item.getItemStack();
        if (stack.getPersistentDataContainer().has(SANDBOX)) item.setItemStack(unmarked(stack));
    }

    /** A Sandbox drop and another one on the ground stay two, so neither takes the other's side. */
    @EventHandler(ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        if (isMarked(event.getEntity()) != isMarked(event.getTarget())) event.setCancelled(true);
    }

    /** Nor through a hopper into a chest. */
    @EventHandler(ignoreCancelled = true)
    public void onHopper(InventoryPickupItemEvent event) {
        if (isMarked(event.getItem())) event.setCancelled(true);
    }

    /** Nor by a mob, which would drop it unmarked when it dies. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onMobPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player) && isMarked(event.getItem())) event.setCancelled(true);
    }

    /** An arrow shot from a bow is the arrow item it was: picked up, it's that item again. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player player && onSandbox(player)) mark(arrow);
    }

    /** A shortbow's arrows get their shooter after they're launched, so the shooter counts too. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPickupArrow(PlayerPickupArrowEvent event) {
        AbstractArrow arrow = event.getArrow();
        boolean sandbox = arrow.getPersistentDataContainer().has(SANDBOX) || arrow.getShooter() instanceof Player shooter && onSandbox(shooter);
        if (sandbox && !onSandbox(event.getPlayer())) event.setCancelled(true);
    }

    private static void mark(PersistentDataHolder entity) {
        entity.getPersistentDataContainer().set(SANDBOX, PersistentDataType.BOOLEAN, true);
    }

    /** On the entity, or still on its stack if it came into the world some way that didn't pass onSpawn. */
    private static boolean isMarked(Item item) {
        return item.getPersistentDataContainer().has(SANDBOX) || item.getItemStack().getPersistentDataContainer().has(SANDBOX);
    }

    private static ItemStack unmarked(ItemStack stack) {
        ItemStack copy = stack.clone();
        copy.editPersistentDataContainer(data -> data.remove(SANDBOX));
        return copy;
    }
}
