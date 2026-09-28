package net.icxd.dungeons.listeners;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.combat.VanillaDamage;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
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
 * own hits (mobs, traps, true damage) go through {@link net.icxd.dungeons.combat.PlayerDamage} and
 * never get here. Vanilla damage stays vanilla's amount (SkyBlock's own fall and fire rules are UNKNOWN,
 * see {@link VanillaDamage}), times what changes it by its cause.
 */
public class HealthListener implements Listener {
    /** More than any player's vanilla health, whatever protects them. */
    private static final double LETHAL = 1_000_000;

    /**
     * What changes vanilla damage by its cause (Feather Falling, Fire and Blast Protection: see {@link
     * VanillaDamage#addFactor}), on the event itself, before immunity and the saves from death (HIGH) and the
     * health it takes (HIGHEST) look at it, so they all see the damage as it will be.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCause(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double factor = VanillaDamage.factor(player, event.getCause());
        if (factor != 1) event.setDamage(event.getDamage() * factor);
    }

    /**
     * Last, so what's left is the damage that happens: it takes as much SkyBlock health as it would
     * have taken vanilla health (after their absorption, see {@link Absorption}). The hit itself goes
     * through with no vanilla damage, so it still flinches and knocks back; a killing one goes through in
     * full, so vanilla does the dying.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        double amount = event.getFinalDamage();
        if (amount <= 0) return;
        // Hurt players show the number too; burning is gold (as recorded on Hypixel).
        EntityDamageEvent.DamageCause cause = event.getCause();
        boolean fire = cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA;
        DamageIndicators.show(player, amount, fire ? '6' : '7');
        boolean always = cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL;
        double left = PlayerHealth.get(player) - (always ? amount : Absorption.absorb(player, amount));
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

    /**
     * Back with full health, and half their mana: "Upon respawning, 50% of max Mana is returned" (the
     * wiki's Mana, 0.11.3), so that's what they have, rounded down, whatever they had when they died. Not
     * after the End's exit portal, which is a respawn without a death (see {@link #afterDeath}).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        if (!afterDeath(event.getRespawnReason())) return;
        PlayerHealth.refill(event.getPlayer());
        Absorption.clear(event.getPlayer());
        PlayerSession session = PlayerSession.of(event.getPlayer());
        session.setMana(respawnMana(session.maxMana()));
    }

    /**
     * Whether a respawn follows a death: all but the End's exit portal's, where vanilla keeps everything.
     * A plugin's respawn is one too (Paper's {@code Player.Spigot#respawn} only brings back the dead).
     */
    static boolean afterDeath(PlayerRespawnEvent.RespawnReason reason) {
        return reason != PlayerRespawnEvent.RespawnReason.END_PORTAL;
    }

    /** Half the pool, rounded down. */
    static int respawnMana(int pool) {
        return Math.max(0, pool) / 2;
    }
}
