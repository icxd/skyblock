package net.icxd.dungeons.listeners;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import net.icxd.dungeons.Dungeons;

/**
 * 1.8-style combat, like Hypixel SkyBlock: no attack cooldown and no sweeping.
 */
public class CombatListener implements Listener {
    private static final NamespacedKey NO_COOLDOWN = new NamespacedKey("dungeons", "no_attack_cooldown");

    public CombatListener() {
        // Players already online when the plugin (re)loads.
        for (Player player : Bukkit.getOnlinePlayers()) removeAttackCooldown(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        removeAttackCooldown(event.getPlayer());
    }

    /** Transient modifiers don't carry over to the respawned player. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(Dungeons.getInstance(), () -> removeAttackCooldown(player));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSweep(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) event.setCancelled(true);
    }

    /**
     * Attack speed high enough that every hit is fully charged. Transient, so it isn't saved with
     * the player and goes away if the plugin is removed.
     */
    public static void removeAttackCooldown(Player player) {
        AttributeInstance attackSpeed = player.getAttribute(Attribute.ATTACK_SPEED);
        if (attackSpeed == null || attackSpeed.getModifier(NO_COOLDOWN) != null) return;
        attackSpeed.addTransientModifier(new AttributeModifier(NO_COOLDOWN, 1000, AttributeModifier.Operation.ADD_NUMBER));
    }
}
