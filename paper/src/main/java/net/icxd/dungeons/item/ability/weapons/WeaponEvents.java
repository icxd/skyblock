package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.icxd.dungeons.combat.PlayerDamage;

/**
 * What weapon abilities need to hear of: a Terminator's arrow landing (towards Salvation), and damage to
 * players (Wither Shield's 10% less while it's up). Registered once, when the plugin starts.
 */
public final class WeaponEvents implements Listener {
    public WeaponEvents() {
        PlayerDamage.addTakenMultiplier(WitherBlade::takenFactor);
    }

    /** "Can be cast after landing 3 hits": an arrow of theirs that lands on a mob an ability could hit. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onArrowLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !arrow.getScoreboardTags().contains(Salvation.ARROW_TAG)) return;
        if (arrow.getShooter() instanceof Player player && event.getHitEntity() != null && Hits.hittable(event.getHitEntity())) {
            Salvation.landed(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Salvation.forget(event.getPlayer().getUniqueId());
    }
}
