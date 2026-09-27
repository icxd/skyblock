package net.icxd.dungeons.listeners;

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
import net.icxd.dungeons.user.User;

public class WorldListener implements Listener {
    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) event.setCancelled(true);
    }

    /** Nothing is broken for real anywhere; mining (BlockListener) works off arm swings. */
    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }

    // Nor is anything placed, except by staff (who build): a chest, a hopper, an item frame or an
    // armor stand would keep items outside every profile, for anyone to take (see SandboxStorage).

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    /** Item frames and paintings. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHang(HangingPlaceEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    /** Armor stands, boats, minecarts and end crystals. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlaceEntity(EntityPlaceEvent event) {
        if (!builds(event.getPlayer())) event.setCancelled(true);
    }

    /** A dispenser (no player) places what the map gave it. */
    private static boolean builds(Player player) {
        return player == null || User.rankOf(player.getUniqueId()).isEqualOrStrongerThan(Rank.STAFF);
    }
}
