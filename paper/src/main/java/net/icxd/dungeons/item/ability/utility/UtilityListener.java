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
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.ItemNBT;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.session.Absorption;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.stats.PlayerStats;

/**
 * What the utility abilities need besides their clicks: every tick their heals, veils, deployables and
 * glides; what immunity does to vanilla damage (SkyBlock hits go through {@link Protection}'s shields); hits
 * from players who can't attack now; a Spirit Leap's immunity ending with a hit; Shadowstep ready again on a
 * kill; the Creeper Veil taken down with a right click; no vanilla use of the items they're on (a thrown
 * ender pearl); Spirit Glide on sneaking; the dungeon secret items used and Training Weights shattering; and
 * what a player who leaves had going. Registered by {@link
 * Dungeons}, which is when the shields and stat hooks go in.
 */
public final class UtilityListener implements Listener {
    /** On what abilities put in the world (a veil's creepers, an orb's stand): not a mob, and never hit. */
    public static final String NOT_A_MOB = "skyblock_ability_prop";

    public UtilityListener() {
        Protection.register();
        Deployables.register();
        PlayerStats.addModifier(Masks::stats);
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), UtilityListener::tick, 1, 1);
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), SecretTracker::second, 20, 20);
        Bukkit.getScheduler().runTaskTimer(Dungeons.getInstance(), TrainingWeights::minute, 1200, 1200);
    }

    private static void tick() {
        Heals.tick();
        CreeperVeil.tick();
        Deployables.tick();
        SpiritGlide.tick();
        WornPassives.tick();
        WornPassives.landed();
        CellsAlignment.tick();
        DungeonBreaker.tick();
        SecretItems.tick();
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
        if (Movement.fell(player, event)) return;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.KILL) return;
        if (Protection.immune(player)) {
            event.setCancelled(true);
        } else if (event.getFinalDamage() >= PlayerHealth.get(player) + Absorption.get(player) && LastStand.saved(player)) {
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
     * Nor is it anything to use: no lead or name tag on a veil's creeper, and no flint and steel, which would
     * set a charged creeper off (its fuse burns without AI) among the players around it.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUseProp(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getScoreboardTags().contains(NOT_A_MOB)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPropExplode(ExplosionPrimeEvent event) {
        if (event.getEntity().getScoreboardTags().contains(NOT_A_MOB)) event.setCancelled(true);
    }

    /** "Cooldown resets on kills". */
    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        if (event.killer() != null) Shadowstep.killed(event.killer());
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

    /**
     * A right click that one of these abilities answers isn't the item's vanilla use as well: a Spirit Leap is
     * an ender pearl and a Flare a firework rocket, which would be thrown or launched and used up, and a Power
     * Orb or a Lantern a head, which would be put down. After the click's ability (see {@link
     * net.icxd.dungeons.listeners.PlayerListener#onAbilityUse}), which a denied use would stop, and whether it
     * cast or not (one on cooldown is no ender pearl either).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onItemUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Worn held = Worn.of(event.getItem());
        if (held == null) return;
        ItemBlock block = Abilities.forClick(held.blocks(), true, event.getPlayer().isSneaking(), name -> Abilities.get(name) != null);
        if (block != null && block.isAbility() && UtilityAbilities.has(block.name())) event.setUseItemInHand(Event.Result.DENY);
    }

    /**
     * A right click with a dungeon secret item that does something when used (see {@link SecretItems}): never its
     * vanilla use (a spawn egg, a pressure plate), nor the clicked block's. Not a ghost's (the run has called its
     * clicks off already).
     */
    @EventHandler
    public void onSecretItem(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        NBTTagCompound tag = ItemNBT.read(event.getItem());
        if (tag == null || !SecretItems.is(tag.getString("id"))) return;
        boolean denied = event.useItemInHand() == Event.Result.DENY;
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        if (!denied) SecretItems.used(event.getPlayer(), tag.getString("id"), event.getClickedBlock(), event.getClickedBlock() == null ? null : event.getBlockFace());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        TrainingWeights.dropped(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        TrainingWeights.clicked(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        TrainingWeights.dragged(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) SpiritGlide.sneaked(event.getPlayer());
        else Movement.released(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Protection.forget(player.getUniqueId());
        CreeperVeil.forget(player.getUniqueId());
        Deployables.forget(player.getUniqueId());
        SpiritGlide.forget(player);
        WornPassives.forget(player.getUniqueId());
        Movement.forget(player.getUniqueId());
        CellsAlignment.forget(player.getUniqueId());
        SecretTracker.forget(player.getUniqueId());
        DungeonBreaker.forget(player.getUniqueId());
    }

    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() != Dungeons.getInstance()) return;
        Deployables.removeAll();
        WornPassives.removeAll();
        DungeonBreaker.restoreAll();
        SecretItems.removeAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            CreeperVeil.forget(player.getUniqueId());
            SpiritGlide.forget(player);
        }
    }
}
