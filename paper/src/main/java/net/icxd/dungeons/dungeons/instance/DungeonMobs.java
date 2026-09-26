package net.icxd.dungeons.dungeons.instance;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;
import net.icxd.dungeons.utils.Text;

/**
 * Dungeon mobs (the Watcher and his undeads) and how they fight. Their health is SkyBlock health,
 * kept here rather than in the entity; hits on them are routed here by the combat listeners, and
 * their hits on players go through {@link #hit}.
 */
public final class DungeonMobs {
    /** One of our mobs. */
    public interface Mob {
        /** A player hit it for this much damage. */
        void hurt(Player by, double damage);

        /** Takes no hits at all (the Watcher): the hit is cancelled rather than shown. */
        default boolean invulnerable() {
            return false;
        }

        /** What its own (vanilla) attacks do to a player, in SkyBlock damage; 0 if it has none. */
        default double attackDamage() {
            return 0;
        }
    }

    /** On every entity that's one of ours, dead or alive: their deaths drop nothing. */
    public static final String TAG = "skyblock_dungeon_mob";

    private static final Map<UUID, Mob> MOBS = new HashMap<>();
    /** The last hit {@link #playerHit} took care of, so the catch-all listener leaves it be. */
    private static EntityDamageEvent handled;

    private DungeonMobs() {
    }

    /** The dungeon mob this entity is, or null. */
    public static Mob of(Entity entity) {
        return entity == null ? null : MOBS.get(entity.getUniqueId());
    }

    static void add(Entity entity, Mob mob) {
        entity.addScoreboardTag(TAG);
        MOBS.put(entity.getUniqueId(), mob);
    }

    static void remove(Entity entity) {
        MOBS.remove(entity.getUniqueId());
    }

    /**
     * A mob hits a player for SkyBlock damage, less their defense (SkyBlock's {@code defense /
     * (defense + 100)}). Vanilla armor doesn't count again, so their health is set directly.
     */
    public static void hit(Player player, double damage, Entity by) {
        if (player.isDead() || player.getGameMode() == org.bukkit.GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) return;
        Stats stats = PlayerSession.of(player).stats();
        double defense = stats == null ? 0 : stats.get(Stat.DEFENSE);
        double taken = damage * 100 / (defense + 100);
        player.setHealth(Math.max(0, player.getHealth() - taken));
        if (player.isDead()) return;
        Vector away = player.getLocation().toVector().subtract(by.getLocation().toVector()).setY(0);
        if (away.lengthSquared() > 0) away.normalize().multiply(0.4);
        player.setVelocity(away.setY(0.36));
        player.playHurtAnimation(0);
        player.getWorld().playSound(player, Sound.ENTITY_PLAYER_HURT, 1, 1);
    }

    /**
     * A player hit one of our mobs for this much SkyBlock damage. The hit itself goes through with
     * no vanilla damage (so it still flinches and takes knockback), or not at all if it's invulnerable.
     */
    public static void playerHit(EntityDamageByEntityEvent event, Player player, Mob mob, double damage, boolean critical) {
        handled = event;
        if (mob.invulnerable()) {
            event.setCancelled(true);
            mob.hurt(player, damage);
            return;
        }
        event.setDamage(0);
        mob.hurt(player, damage);
        showDamage(event.getEntity(), damage, critical);
    }

    /** Whether {@link #playerHit} already dealt with this hit. */
    public static boolean isHandled(EntityDamageEvent event) {
        return event == handled;
    }

    /** The number that pops up where a mob was hit, as for SkyBlock's other mobs. */
    public static void showDamage(Entity at, double damage, boolean critical) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location spot = at.getLocation().add(random.nextDouble(-0.5, 0.5), 2, random.nextDouble(-0.5, 0.5));
        ArmorStand stand = at.getWorld().spawn(spot, ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setMarker(true);
            s.setPersistent(false);
            s.customName(Text.line(critical ? Utils.rainbowize("✧" + (int) damage + "✧") : "&7" + (int) damage));
            s.setCustomNameVisible(true);
        });
        Bukkit.getScheduler().runTaskLater(Dungeons.getInstance(), stand::remove, 30);
    }

    /** What a player's fist (or a non-SkyBlock item) does: 5 base damage with their damage and strength. */
    public static double fistDamage(Player player) {
        Stats stats = PlayerSession.of(player).stats();
        if (stats == null) return 5;
        return (5 + stats.get(Stat.DAMAGE)) * (1 + stats.get(Stat.STRENGTH) / 100);
    }
}
