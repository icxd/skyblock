package net.icxd.dungeons.dungeons.instance;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.icxd.dungeons.combat.LastHit;
import net.icxd.dungeons.combat.PlayerDamage;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Deaths in a run (they make ghosts, see {@link Ghosts}), what ghosts can't do, the Fairy Room's
 * fairies being hit, and Revive Stones being used. Registered by {@link RunManager}.
 */
final class GhostEvents implements Listener {
    /** A SkyBlock hit this recent took the last of their health; one this recent helped a fall. */
    private static final long HIT_NOW = 500;
    private static final long HIT_LATELY = 5_000;

    private final RunManager manager;
    private final Plugin plugin;

    GhostEvents(RunManager manager, Plugin plugin) {
        this.manager = manager;
        this.plugin = plugin;
    }

    private DungeonRun running(Player player) {
        DungeonRun run = manager.runOf(player);
        return run != null && run.phase() == DungeonRun.Phase.RUNNING ? run : null;
    }

    private boolean ghost(Entity entity) {
        if (!(entity instanceof Player player)) return false;
        DungeonRun run = manager.runOf(player);
        return run != null && run.ghosts().isGhost(player.getUniqueId());
    }

    // Dying

    /**
     * Nobody dies in a running dungeon: the death is called off (full vanilla health, SkyBlock health
     * refilled, no sound) and on the next tick they're a ghost. Meanwhile they can't be hurt.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        DungeonRun run = running(player);
        if (run == null) return;
        event.setCancelled(true);
        AttributeInstance max = player.getAttribute(Attribute.MAX_HEALTH);
        event.setReviveHealth(max == null ? 20 : Math.max(1, max.getValue()));
        event.setShouldPlayDeathSound(false);
        player.setInvulnerable(true);
        PlayerHealth.refill(player);

        LastHit.Hit now = LastHit.within(player, HIT_NOW);
        LastHit.Hit lately = LastHit.within(player, HIT_LATELY);
        String type = event.getDamageSource().getDamageType().getKey().getKey();
        boolean trap = now != null && now.by() == null && now.kind() == PlayerDamage.Kind.MAX_HEALTH;
        DeathText.Reason reason = DeathText.reason(type, now != null, trap, lately != null && lately.by() != null);
        Entity killer = now != null ? now.by() : lately != null ? lately.by() : event.getDamageSource().getCausingEntity();
        String name = killerName(killer);
        LastHit.forget(player.getUniqueId());
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && manager.runOf(player) == run && run.phase() == DungeonRun.Phase.RUNNING) {
                run.ghosts().died(player, reason, name);
            } else {
                player.setInvulnerable(false);
            }
        });
    }

    /**
     * "Zombie Grunt": a SkyBlock mob's name, the Blood Room's undead's ("Revoker") or the Watcher's,
     * else what the entity is called; null for nobody.
     */
    private static String killerName(Entity killer) {
        if (killer instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) killer = shooter;
        if (killer == null) return null;
        Mobs.Live mob = Mobs.of(killer);
        if (mob != null && !mob.type().getName().isEmpty()) return mob.type().getName().replaceAll("[&§].", "");
        // Their bodies are nameless mannequins and zombies: the name is on a stand over them.
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(killer);
        if (dungeonMob instanceof Undead undead) return undead.type.displayName;
        if (dungeonMob instanceof Watcher) return "The Watcher";
        if (killer.customName() != null) return PlainTextComponentSerializer.plainText().serialize(killer.customName());
        return killer instanceof LivingEntity && !(killer instanceof Player) ? null : killer.getName();
    }

    /**
     * Leaving while it runs makes you a ghost (MCW Ghosts). What a ghost was set to is undone for now,
     * whatever the phase (a ghost can leave an ended run too), so it isn't saved with them. Revive
     * Stones go ("When a player exits a dungeon, all Revive Stones in their inventory will be removed",
     * FW Revive Stone), before their inventory is saved.
     */
    @EventHandler(priority = EventPriority.LOW)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DungeonRun run = manager.runOf(player);
        if (run == null) return;
        ReviveStones.takeAll(player);
        if (run.phase() == DungeonRun.Phase.RUNNING) run.ghosts().disconnected(player);
        run.ghosts().leaving(player);
    }

    // What ghosts can't do

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostHit(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager() instanceof Projectile p && p.getShooter() instanceof Entity shooter ? shooter : event.getDamager();
        if (ghost(damager)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTarget(EntityTargetEvent event) {
        if (ghost(event.getTarget())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostInteract(PlayerInteractEvent event) {
        if (ghost(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostInteractEntity(PlayerInteractEntityEvent event) {
        if (ghost(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostArmorStand(PlayerArmorStandManipulateEvent event) {
        DungeonRun run = manager.runOf(event.getPlayer());
        if (ghost(event.getPlayer()) || run != null && run.isFairy(event.getRightClicked())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostPickup(EntityPickupItemEvent event) {
        if (ghost(event.getEntity())) event.setCancelled(true);
    }

    /** Arrows (and tridents) stuck in the ground have their own pickup event. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostPickupArrow(PlayerPickupArrowEvent event) {
        if (ghost(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostDrop(PlayerDropItemEvent event) {
        if (ghost(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onGhostShoot(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Entity shooter && ghost(shooter)) event.setCancelled(true);
    }

    /**
     * Nothing flying hits a ghost: the hit is called off, so the projectile goes on through (else a mob's
     * arrow would bounce off it and a Crypt Lurker's bone be used up on it, shielding whoever is behind).
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileHitGhost(ProjectileHitEvent event) {
        if (ghost(event.getHitEntity())) event.setCancelled(true);
    }

    // Fairies

    /** Any hit on a fairy kills it, a ghost's too (they're invulnerable stands, so it's caught before any damage). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onHitFairy(PrePlayerAttackEntityEvent event) {
        DungeonRun run = running(event.getPlayer());
        if (run == null || !run.isFairy(event.getAttacked())) return;
        event.setCancelled(true);
        run.fairyKilled(event.getAttacked(), event.getPlayer());
    }

    @EventHandler
    public void onShootFairy(ProjectileHitEvent event) {
        if (event.getHitEntity() == null || !(event.getEntity().getShooter() instanceof Player player)) return;
        DungeonRun run = running(player);
        if (run == null || !run.isFairy(event.getHitEntity())) return;
        event.setCancelled(true);
        event.getEntity().remove();
        run.fairyKilled(event.getHitEntity(), player);
    }

    // Revive Stones

    /** A right click with a Revive Stone (it's a head: it's never placed). */
    @EventHandler(priority = EventPriority.LOW)
    public void onReviveStone(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !ReviveStones.is(event.getItem())) return;
        event.setCancelled(true);
        boolean right = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        DungeonRun run = running(event.getPlayer());
        if (!right || run == null || run.ghosts().isGhost(event.getPlayer().getUniqueId())) return;
        ReviveStones.use(run, event.getPlayer());
    }
}
