package net.icxd.dungeons.item.ability.weapons;

import java.util.UUID;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.PlayerDamage;

/**
 * What weapon abilities need to hear of: players' arrows landing (a Terminator's count towards Salvation,
 * a Juju's hit around them, a Nasty Bite heals), and damage to players (Wither Shield's 10% less while it's
 * up, Hellstorm's double). Registered once, when the plugin starts.
 */
public final class WeaponEvents implements Listener {
    public WeaponEvents() {
        PlayerDamage.addTakenMultiplier(WitherBlade::takenFactor);
        PlayerDamage.addTakenMultiplier(Buffs.Hellstorm::takenFactor);
        Combat.addMultiplier(Buffs.SmashHead::multiplier);
    }

    /**
     * An arrow of theirs landed, before its hit is worked out: on a mob an ability could hit, that counts as
     * one of Salvation's "3 hits".
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArrowLand(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof AbstractArrow arrow) || !(arrow.getShooter() instanceof Player player)) return;
        Entity hit = event.getHitEntity();
        if (arrow.getScoreboardTags().contains(Salvation.ARROW_TAG) && hit != null && Hits.hittable(hit)) {
            Salvation.landed(player.getUniqueId());
        }
        Bows.landed(arrow, player, hit);
    }

    @EventHandler
    public void onArrowGone(EntityRemoveEvent event) {
        if (event.getEntity() instanceof AbstractArrow) Bows.gone(event.getEntity().getUniqueId());
    }

    /** What their abilities kept about them goes with them. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        Salvation.forget(player);
        WitherBlade.forget(player);
        JerryGun.forget(player);
        ThrownBlade.forget(player);
        Bonemerang.forget(player);
        ReavingStrike.forget(player);
        Buffs.forget(player);
        Strikes.SinrecallTransmission.forget(player);
    }
}
