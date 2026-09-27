package net.icxd.dungeons.item.ability.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.session.PlayerHealth;

/**
 * What the utility abilities need besides their clicks: every tick their heals, veils and glides; what
 * immunity does to vanilla damage (SkyBlock hits go through {@link Protection}'s shields); hits from players
 * who can't attack now; a Spirit Leap's immunity ending with a hit; the Creeper Veil taken down with a right
 * click; Spirit Glide on sneaking; and what a player who leaves had going. Registered by {@link Dungeons},
 * which is when the shields and stat hooks go in.
 */
public final class UtilityListener implements Listener {
    /** On what abilities put in the world (a veil's creepers): not a mob, and never hit. */
    static final String NOT_A_MOB = "skyblock_ability_prop";

    public UtilityListener() {
        Protection.register();
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), UtilityListener::tick, 1, 1);
    }

    private static void tick() {
        Heals.tick();
        CreeperVeil.tick();
        SpiritGlide.tick();
    }

    /**
     * Immunity takes vanilla damage too (a fall, fire), and a helmet saves them from vanilla damage that would
     * kill them; not the void's, nor /kill's. Before {@link net.icxd.dungeons.listeners.HealthListener} turns it
     * into SkyBlock health.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity().getScoreboardTags().contains(NOT_A_MOB)) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player player)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;
        if (Protection.immune(player)) {
            event.setCancelled(true);
        } else if (event.getFinalDamage() >= PlayerHealth.get(player) && LastStand.saved(player)) {
            event.setCancelled(true);
        }
    }

    /** Players who can't attack now (a veil up, Soulward) don't: their hits and arrows do nothing. */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        Player attacker = Combat.playerBehind(event.getDamager());
        if (attacker != null && Protection.cantAttack(attacker)) event.setCancelled(true);
    }

    /**
     * "immunity is cancelled upon dealing damage": a hit of theirs ends a Spirit Leap's, a killing one too
     * (SkyBlock's mobs call those off: see {@link net.icxd.dungeons.mob.Mobs#playerHit}).
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(EntityDamageByEntityEvent event) {
        Player attacker = Combat.playerBehind(event.getDamager());
        if (attacker == null || event.getEntity().equals(attacker) || event.getEntity().getScoreboardTags().contains(NOT_A_MOB)) return;
        if (!Protection.cantAttack(attacker)) Protection.endImmunity(attacker, SpiritLeap.NAME);
    }

    /** What abilities put in the world isn't in the way of arrows. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHit(ProjectileHitEvent event) {
        Entity hit = event.getHitEntity();
        if (hit != null && hit.getScoreboardTags().contains(NOT_A_MOB)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (event.getTarget() != null && event.getTarget().getScoreboardTags().contains(NOT_A_MOB)) event.setCancelled(true);
    }

    /**
     * A right click with the Wither Cloak Sword while its veil is up takes it down, before the click can be a
     * cast (see {@link net.icxd.dungeons.listeners.PlayerListener#onAbilityUse}).
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onVeilClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!CreeperVeil.isUp(player) || Worn.ability(player.getInventory().getItemInMainHand(), CreeperVeil.NAME) == null) return;
        if (CreeperVeil.deactivate(player)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) SpiritGlide.sneaked(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Protection.forget(player.getUniqueId());
        CreeperVeil.forget(player.getUniqueId());
        SpiritGlide.forget(player);
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != Dungeons.getInstance()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            CreeperVeil.forget(player.getUniqueId());
            SpiritGlide.forget(player);
        }
    }
}
