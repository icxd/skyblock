package net.icxd.dungeons.listeners;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;

import net.icxd.dungeons.common.Rank;
import net.icxd.dungeons.island.Islands;
import net.icxd.dungeons.user.User;

public class WorldListener implements Listener {
    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) event.setCancelled(true);
    }

    /**
     * Nothing is broken for real, but on a player's own island (IslandRules gives them its drops) and
     * the Hub's flower beds (FlowerGarden picks them); mining (BlockListener) works off arm swings.
     */
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (!ownIsland(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    // Nor is anything placed, except by staff (who build) and on a player's own island: anywhere else,
    // a chest, a hopper, an item frame or an armor stand would keep items outside every profile, for
    // anyone to take (see SandboxStorage). An island is its profile's.

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!builds(event.getPlayer(), event.getBlock().getWorld())) event.setCancelled(true);
    }

    /** Item frames and paintings. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHang(HangingPlaceEvent event) {
        if (!builds(event.getPlayer(), event.getEntity().getWorld())) event.setCancelled(true);
    }

    /** Armor stands, boats, minecarts and end crystals. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlaceEntity(EntityPlaceEvent event) {
        if (!builds(event.getPlayer(), event.getEntity().getWorld())) event.setCancelled(true);
    }

    /** A dispenser (no player) places what the map gave it. */
    private static boolean builds(Player player, World world) {
        return player == null || User.rankOf(player.getUniqueId()).isEqualOrStrongerThan(Rank.STAFF) || ownIsland(player, world);
    }

    /** The island of the profile they play on, while their items aren't frozen for a move (what they'd do then wouldn't be saved). */
    private static boolean ownIsland(Player player, World world) {
        return Islands.isOwnIsland(player, world) && !InventorySyncListener.frozen(player);
    }
}
