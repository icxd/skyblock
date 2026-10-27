package net.icxd.dungeons.pet;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;

/** What triggers Autopet's rules that comes as events (see {@link Autopet}). Main thread. */
public final class AutopetListener implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Autopet.joined(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Autopet.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.FISHING) Autopet.fire(event.getPlayer(), Autopet.Trigger.FISHING_HOOK, null);
    }

    /** "This rule triggers upon dealing damage to any enemy, or taking any damage" (the wiki's). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player hurt) Autopet.combat(hurt);
        if (!(event instanceof EntityDamageByEntityEvent byEntity) || !(event.getEntity() instanceof LivingEntity)
                || event.getEntity() instanceof Player) return;
        Player hitter = byEntity.getDamager() instanceof Player p ? p
                : byEntity.getDamager() instanceof Projectile shot && shot.getShooter() instanceof Player p ? p : null;
        if (hitter != null) Autopet.combat(hitter);
    }

    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        if (event.killer() != null && event.kind() != null && event.kind().entityType() != null) {
            Autopet.fire(event.killer(), Autopet.Trigger.KILL_MOB, event.kind().entityType().name());
        }
    }
}
