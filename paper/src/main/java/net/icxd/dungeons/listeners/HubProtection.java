package net.icxd.dungeons.listeners;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.OnlyOn;
import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.common.ServerType;
import net.icxd.dungeons.user.User;

/**
 * The hubs and islands are the map as it's built: players look around, they don't use or change it,
 * as on Hypixel. No doors, trapdoors, levers, buttons, containers, beds or pressure plates, no item
 * frames or armor stands, no buckets, bone meal, fire, or a tool's work on a block (an axe stripping
 * a log, a hoe tilling). Placing and breaking are refused everywhere (see WorldListener).
 *
 * <p>Only the block's own use is refused, never the item's: a right click on a block still casts
 * the held item's ability. NPCs and SkyBlock mobs are the plugin's, and handle their own clicks.
 * Staff are left alone, since they build. Dungeon runs have worlds of their own, where secrets,
 * levers and doors are the game, so only the main world is covered.
 */
@OnlyOn({ServerType.LOBBY, ServerType.DUNGEON_HUB, ServerType.CRIMSON_ISLE, ServerType.DWARVEN_MINES})
public class HubProtection implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (!covers(event.getPlayer(), event.getPlayer().getWorld())) return;
        if (event.getAction() == Action.PHYSICAL) {
            // Pressure plates, tripwires, farmland, turtle eggs.
            event.setCancelled(true);
        } else if (event.getClickedBlock() != null) {
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    /** An axe stripping a log, a shovel making a path, a hoe tilling, wax on copper. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player player && covers(player, event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFertilize(BlockFertilizeEvent event) {
        if (covers(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onIgnite(BlockIgniteEvent event) {
        if (covers(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCauldron(CauldronLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player && covers(player, event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSign(SignChangeEvent event) {
        if (covers(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLectern(PlayerTakeLecternBookEvent event) {
        if (covers(event.getPlayer(), event.getLectern().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (covers(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (covers(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEntity(PlayerBucketEntityEvent event) {
        if (covers(event.getPlayer(), event.getEntity().getWorld())) event.setCancelled(true);
    }

    /** Turning a frame's item or taking it out; getting in a boat or opening a chest minecart. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if ((clicked instanceof Hanging || clicked instanceof Vehicle) && covers(event.getPlayer(), clicked.getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        if (covers(event.getPlayer(), event.getRightClicked().getWorld())) event.setCancelled(true);
    }

    /** Boats and minecarts the map has. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnterVehicle(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player && covers(player, event.getVehicle().getWorld())) event.setCancelled(true);
    }

    /** Knocking an item out of a frame, or an armor stand over; paintings and frames break in onBreakHanging. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if (!(target instanceof Hanging || target instanceof ArmorStand || target instanceof Vehicle)) return;
        Player player = byPlayer(event.getDamager());
        if (player != null && covers(player, target.getWorld())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreakHanging(HangingBreakByEntityEvent event) {
        Player player = byPlayer(event.getRemover());
        if (player != null && covers(player, event.getEntity().getWorld())) event.setCancelled(true);
    }

    /** An arrow on a button, a pressure plate or a tripwire, or a mob on farmland. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityInteract(EntityInteractEvent event) {
        if (event.getBlock().getWorld().equals(mainWorld())) event.setCancelled(true);
    }

    /** The player behind a hit: themselves, or who shot the projectile. */
    private static Player byPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) return player;
        return null;
    }

    /** Whether a player's use of the world is refused there: a non-staff player in the main world. */
    private static boolean covers(Player player, World world) {
        if (player == null || !world.equals(mainWorld())) return false;
        return !User.rankOf(player.getUniqueId()).isEqualOrStrongerThan(Rank.STAFF);
    }

    private static World mainWorld() {
        return Dungeons.getSkyBlockServer().getMainWorld();
    }
}
