package net.icxd.dungeons.item.ability.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.session.PlayerHealth;

/**
 * What the utility abilities need besides their clicks: every tick their heals and glides; what immunity
 * does to vanilla damage (SkyBlock hits go through {@link Protection}'s shields); hits from players who
 * can't attack now; Spirit Glide on sneaking; and what a player who leaves had going. Registered by
 * {@link Dungeons}, which is when the shields and stat hooks go in.
 */
public final class UtilityListener implements Listener {
    public UtilityListener() {
        Protection.register();
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), UtilityListener::tick, 1, 1);
    }

    private static void tick() {
        Heals.tick();
        SpiritGlide.tick();
    }

    /**
     * Immunity takes vanilla damage too (a fall, fire), and a helmet saves them from vanilla damage that would
     * kill them; not the void's, nor /kill's. Before {@link net.icxd.dungeons.listeners.HealthListener} turns it
     * into SkyBlock health.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;
        if (Protection.immune(player)) {
            event.setCancelled(true);
        } else if (event.getFinalDamage() >= PlayerHealth.get(player) && LastStand.saved(player)) {
            event.setCancelled(true);
        }
    }

    /** Players who can't attack now (Soulward) don't: their hits and arrows do nothing. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = Combat.playerBehind(event.getDamager());
        if (attacker != null && Protection.cantAttack(attacker)) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) SpiritGlide.sneaked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Protection.forget(player.getUniqueId());
        SpiritGlide.forget(player);
    }
}
