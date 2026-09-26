package net.icxd.dungeons.listeners;

import net.icxd.dungeons.session.PlayerHealth;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Vanilla damage and healing on players, as SkyBlock health (see {@link PlayerHealth}). SkyBlock's
 * own hits (mobs, explosions) take health directly and never get here.
 */
public class HealthListener implements Listener {
    /** More than any player's vanilla health, whatever protects them. */
    private static final double LETHAL = 1_000_000;

    /**
     * Last, so what's left is the damage that happens: it takes as much SkyBlock health as it would
     * have taken vanilla health. The hit itself goes through with no vanilla damage, so it still
     * flinches and knocks back; a killing one goes through in full, so vanilla does the dying.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double amount = event.getFinalDamage();
        if (amount <= 0) return;
        double left = PlayerHealth.get(player) - amount;
        if (left > 0) {
            event.setDamage(0);
            PlayerHealth.set(player, left);
        } else {
            PlayerHealth.drain(player);
            event.setDamage(Math.max(LETHAL, event.getDamage()));
        }
    }

    /** SkyBlock heals by its own rules (see StatsRunnable): a vanilla heart would be a tenth of their health. */
    @EventHandler(ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        PlayerHealth.sync(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerHealth.refill(event.getPlayer());
    }
}
