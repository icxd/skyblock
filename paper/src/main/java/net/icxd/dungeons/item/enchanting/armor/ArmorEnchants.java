package net.icxd.dungeons.item.enchanting.armor;

import net.icxd.dungeons.combat.MobHits;
import net.icxd.dungeons.item.ability.utility.Heals;
import net.icxd.dungeons.item.enums.SpecificItemType;
import net.icxd.dungeons.mob.SkyBlockMobDeathEvent;
import net.icxd.dungeons.reforge.ArmorReforgeBonuses;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Armor and equipment enchantments that do more than their text's plain stats (ENCHANTS_ARMOR.md), and the armor
 * reforges' bonuses ({@link ArmorReforgeBonuses}), on the core's hooks (EFFECTS.md): the stats that depend on
 * something ({@link StatEnchants}), hits taken ({@link HitsTaken}), vanilla damage and movement ({@link
 * Protections}), what spending Vitality and mana turns into ({@link Conversions}), Habanero Tactics, Hecatomb,
 * Stealth and Bank; and here, a tick a second (Transylvanian, the vanilla attributes, Frost Walker's melting) and the
 * events they need. What each piece has comes from {@link WornEnchants}, its numbers from its book's text ({@link
 * EnchantNumbers}). Main thread.
 */
public final class ArmorEnchants implements Listener {
    static final String TRANSYLVANIAN = "transylvanian";

    /** Their health as vanilla damage reaches them, for Last Stand (see {@link #onDamageBefore}). */
    private final Map<UUID, Double> healthBefore = new HashMap<>();
    private final Plugin plugin;

    private ArmorEnchants(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers the hooks, the listener and the second's tick, once, as the plugin starts: after the set bonuses,
     * whose stats come before theirs.
     */
    public static void enable(Plugin plugin) {
        PlayerStats.addModifier(StatEnchants::stats);
        HitsTaken.register();
        Protections.register();
        Conversions.register();
        Habanero.register();
        Hecatomb.register();
        Stealth.register();
        Bank.register();
        ArmorReforgeBonuses.register();
        plugin.getServer().getPluginManager().registerEvents(new ArmorEnchants(plugin), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, ArmorEnchants::second, 20, 20);
    }

    /** Every second: Transylvanian's heal, the vanilla attributes, Frost Walker's melting, and what's forgotten. */
    private static void second() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            List<WornEnchants.Piece> pieces = WornEnchants.of(player);
            Protections.second(player, pieces);
            if (!player.isDead()) transylvanian(player, pieces);
        }
        Protections.melt();
        HitsTaken.second();
    }

    /**
     * Transylvanian, on helmets: "Heal 4❤/s per enemy within 10 blocks, up to 40❤/s" (the text's numbers; live V
     * items and the wiki say 3 and 30, the table 4 and 40: the text wins, so lore and heal agree), a heal of their
     * own each second ({@link Heals#give}). Enemies are SkyBlock's mobs that can be hurt.
     */
    private static void transylvanian(Player player, List<WornEnchants.Piece> pieces) {
        WornEnchants.Piece helmet = WornEnchants.ofType(pieces, SpecificItemType.HELMET);
        int level = helmet == null ? 0 : helmet.level(TRANSYLVANIAN);
        if (level <= 0) return;
        double[] n = EnchantNumbers.of(TRANSYLVANIAN, level);
        if (n.length < 3) return;
        double heal = transylvanian(n[0], enemiesNear(player, n[1]), n[2]);
        if (heal > 0) Heals.give(player, player, heal);
    }

    /** Transylvanian's heal: {@code each} for every enemy, up to {@code most}. */
    static double transylvanian(double each, int enemies, double most) {
        return Math.min(most, Math.max(0, each) * Math.max(0, enemies));
    }

    private static int enemiesNear(Player player, double radius) {
        int count = 0;
        Location at = player.getLocation();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof LivingEntity && MobHits.hittable(entity)
                    && entity.getLocation().distanceSquared(at) <= radius * radius) count++;
        }
        return count;
    }

    // ---------- events ----------

    /**
     * Vanilla damage (a fall, fire) as it's about to take health (HealthListener's, at HIGHEST): their health
     * before it, for Last Stand.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageBefore(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) healthBefore.put(player.getUniqueId(), PlayerHealth.get(player));
    }

    /** Once vanilla damage has taken its health: Last Stand, if it took them below its share. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Double before = healthBefore.remove(player.getUniqueId());
        if (before == null) return;
        List<WornEnchants.Piece> pieces = WornEnchants.of(player);
        if (!pieces.isEmpty()) HitsTaken.lastStand(player, pieces, before, PlayerHealth.get(player));
    }

    /** Bank's coins for a kill. */
    @EventHandler
    public void onKill(SkyBlockMobDeathEvent event) {
        if (event.killer() != null) Bank.killed(event.killer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) ArmorReforgeBonuses.sneaked(event.getPlayer());
    }

    /** Frost Walker, as they walk onto another block. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) return;
        if (event.getPlayer().getInventory().getBoots() == null) return;
        Protections.moved(event.getPlayer(), to);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        healthBefore.remove(id);
        WornEnchants.forget(id);
        HitsTaken.forget(id);
        Conversions.forget(id);
        StatEnchants.forget(id);
    }

    /** The plugin is stopping: Frost Walker's ice goes back to water now. */
    @EventHandler
    public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) Protections.meltAll();
    }
}
