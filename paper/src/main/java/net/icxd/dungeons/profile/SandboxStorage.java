package net.icxd.dungeons.profile;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Campfire;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.PlayerInventory;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.user.User;
import net.icxd.dungeons.utils.Text;

/**
 * Items don't go from a Sandbox profile to a Normal one through the world either: a player on a
 * Sandbox profile can't put anything into what keeps items there (a chest, a furnace, a jukebox, a
 * flower pot, an item frame, an armor stand, a chest minecart...), where anyone could take it out.
 * Only staff place such things (see WorldListener), so these are the map's. Taking from a frame or a
 * stand is still allowed (Normal items may go onto a Sandbox profile); a container's menu can't tell
 * a take from a put, so it doesn't open for them at all.
 *
 * <p>The vanilla ender chest is one per player (and one per server), not one per profile, so it's
 * closed to everyone but staff: what's in it may be from a Sandbox profile, or from before profiles
 * (whose items went to Sandbox ones).
 */
public class SandboxStorage implements Listener {
    private static final String NO_STORAGE = "&cItems can't leave a Sandbox profile!";
    private static final String NO_ENDER_CHEST = "&cYou can't use Ender Chests yet!";

    /** Menus whose items go back to the player when they close (see StoredInventory#rescueLooseItems). */
    @SuppressWarnings("deprecation")
    private static final Set<InventoryType> GIVES_BACK = EnumSet.of(InventoryType.WORKBENCH, InventoryType.CRAFTING,
            InventoryType.ENCHANTING, InventoryType.BEACON, InventoryType.ANVIL, InventoryType.SMITHING, InventoryType.SMITHING_NEW,
            InventoryType.GRINDSTONE, InventoryType.STONECUTTER, InventoryType.LOOM, InventoryType.CARTOGRAPHY, InventoryType.MERCHANT,
            InventoryType.PLAYER, InventoryType.CREATIVE);

    /** A right click on a block: its own menu doesn't open, and nothing goes into it. */
    @EventHandler(priority = EventPriority.LOW)
    public void onUseBlock(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || event.useInteractedBlock() == Event.Result.DENY) return;
        Player player = event.getPlayer();
        PlayerInventory hands = player.getInventory();
        // Sneaking with something in hand uses that, not the block.
        if (player.isSneaking() && !(hands.getItemInMainHand().isEmpty() && hands.getItemInOffHand().isEmpty())) return;
        String refused = null;
        if (block.getType() == Material.ENDER_CHEST) {
            if (!staff(player)) refused = NO_ENDER_CHEST;
        } else if (SandboxDrops.onSandbox(player) && keepsItems(block)) {
            refused = NO_STORAGE;
        }
        if (refused == null) return;
        event.setUseInteractedBlock(Event.Result.DENY);
        if (event.getHand() == EquipmentSlot.HAND) player.sendMessage(Text.line(refused));
    }

    /** Putting an item in a frame, or on a horse, an allay or the like. An empty hand only turns or rides it. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onUseEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.getInventory().getItem(event.getHand()).isEmpty() || !SandboxDrops.onSandbox(player)
                || !keepsItems(event.getRightClicked())) return;
        event.setCancelled(true);
        if (event.getHand() == EquipmentSlot.HAND) player.sendMessage(Text.line(NO_STORAGE));
    }

    /** Giving an armor stand something (or swapping); taking what it has is fine. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (event.getPlayerItem().isEmpty() || !SandboxDrops.onSandbox(event.getPlayer())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(Text.line(NO_STORAGE));
    }

    /** Whatever opens it: a chest minecart, a donkey, or a block the rules above missed. */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Inventory inventory = event.getInventory();
        String refused = null;
        if (inventory.getType() == InventoryType.ENDER_CHEST) {
            if (!staff(player)) refused = NO_ENDER_CHEST;
        } else if (keepsItems(inventory) && SandboxDrops.onSandbox(player)) {
            refused = NO_STORAGE;
        }
        if (refused == null) return;
        event.setCancelled(true);
        player.sendMessage(Text.line(refused));
    }

    /** A block that keeps what's put in it: one with an inventory (not a crafting table), a flower pot, a campfire, a composter. */
    private static boolean keepsItems(Block block) {
        Material type = block.getType();
        if (Tag.FLOWER_POTS.isTagged(type) || type == Material.COMPOSTER) return true;
        BlockState state = block.getState(false);
        return state instanceof BlockInventoryHolder || state instanceof Campfire;
    }

    /** An item frame, or a creature or vehicle with an inventory; not a player, nor a villager (trading takes, it doesn't keep). */
    private static boolean keepsItems(Entity entity) {
        return entity instanceof ItemFrame
                || entity instanceof InventoryHolder && !(entity instanceof HumanEntity) && !(entity instanceof AbstractVillager);
    }

    /** A block's or a creature's inventory that stays in the world when it's closed; not one of the plugin's menus (a player's). */
    private static boolean keepsItems(Inventory inventory) {
        if (GIVES_BACK.contains(inventory.getType())) return false;
        InventoryHolder holder = inventory.getHolder(false);
        return holder instanceof BlockInventoryHolder || holder instanceof DoubleChest
                || holder instanceof Entity && !(holder instanceof HumanEntity);
    }

    private static boolean staff(Player player) {
        return User.rankOf(player.getUniqueId()).isEqualOrStrongerThan(Rank.STAFF);
    }
}
