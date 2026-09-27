package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;

/**
 * The Archer's, Tank's and Healer's Dungeon Orb abilities, from their recorded Class Details lore (no
 * recording has them used; where the lore leaves something out it says UNKNOWN). {@link RunClasses}
 * decides when they may be used and says "Used ...!". Not built: the Healer's Healing Circle (its
 * radius and duration are UNKNOWN), the Mage's Guided Sheep and Thunderstorm (their damage "based on
 * your Mage level" is UNKNOWN), and "Acts as Superboom TNT!" (crypts and weak walls are the rooms').
 */
final class ClassAbilities {
    /** Explosive Shot: "a volley of 3 Arrows"; their spread is UNKNOWN (5 degrees apart). */
    private static final int VOLLEY = 3;
    private static final double VOLLEY_SPREAD = 5;
    /** "in a 4 block radius". */
    private static final double BLAST = 4;
    /** Rapid Fire: "5 Arrows per second". */
    private static final int RAPID_EVERY = 4;
    private static final double ARROW_SPEED = 3;
    /** Seismic Wave: a block a tick, for a length that's UNKNOWN (12 blocks), hitting what's within this of its path. */
    private static final int WAVE_LENGTH = 12;
    private static final double WAVE_REACH = 1.5;
    /** Castle of Stone: "aggros all enemies in a 10 block radius". */
    static final double AGGRO = 10;
    /**
     * Castle of Stone: "reducing the damage you take by 70%", on every SkyBlock hit (whether traps and
     * true damage count too is UNKNOWN: they do here; vanilla damage, a fall say, doesn't).
     */
    static final double CASTLE_OF_STONE_TAKEN = 0.3;
    /** Wish: 10 seconds off its cooldown "for every player below 25% health". */
    private static final double WISH_LOW = 0.25;

    /** An arrow of theirs in flight: what it does when it lands. */
    private record Arrowhead(Player shooter, double damage, boolean explodes) {
    }

    private final DungeonRun run;
    private final Map<UUID, Arrowhead> arrows = new HashMap<>();
    private final List<Projectile> flying = new ArrayList<>();

    ClassAbilities(DungeonRun run) {
        this.run = run;
    }

    // What the lore gives

    /** "&8Cooldown: &a34s" at 16: 40 seconds, 2 less every 5 levels (MCW, and the recording). */
    static long explosiveShotCooldown(int level) {
        return (40 - 2L * (Math.clamp(level, 0, 50) / 5)) * 1000;
    }

    /** Rapid Fire's seconds: 4, and one more every 10 levels (5 at 16, as recorded). */
    static int rapidFireSeconds(int level) {
        return 4 + Math.clamp(level, 0, 50) / 10;
    }

    /** "Deals 20,000 +10% every +50 Defense in damage". */
    static double seismicWaveDamage(double defense) {
        return 20_000 * (1 + 0.1 * Math.floor(Math.max(0, defense) / 50));
    }

    /** Wish's cooldown, 120 seconds, 10 less for each teammate below 25% health. */
    static long wishCooldown(int low) {
        return Math.max(0, 120 - 10L * low) * 1000;
    }

    // The abilities

    /** Archer, right click: three exploding arrows, each for the highest hit (see {@link RunClasses}). */
    void explosiveShot(Player player, double damage) {
        for (int i = 0; i < VOLLEY; i++) {
            Location eye = player.getEyeLocation();
            eye.setYaw((float) (eye.getYaw() + (i - (VOLLEY - 1) / 2.0) * VOLLEY_SPREAD));
            shoot(player, eye.getDirection(), damage, true);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1);
    }

    /** Archer, left click: 5 arrows a second, each for 75% of the highest hit. */
    void rapidFire(Player player, double damage, int seconds) {
        for (int t = 0; t < seconds * 20; t += RAPID_EVERY) {
            run.later(t, () -> {
                if (!canStillHit(player)) return;
                shoot(player, player.getEyeLocation().getDirection(), damage, false);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1.2f);
            });
        }
    }

    private void shoot(Player player, Vector direction, double damage, boolean explodes) {
        Arrow arrow = player.launchProjectile(Arrow.class, direction.normalize().multiply(ARROW_SPEED));
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setPersistent(false);
        arrows.put(arrow.getUniqueId(), new Arrowhead(player, damage, explodes));
        flying.add(arrow);
    }

    /**
     * One of theirs landed (on {@code hit}, or in a block when that's null): an Explosive Shot arrow blows
     * up, a Rapid Fire one hurts what it hit. False if it isn't one of theirs.
     */
    boolean landed(Projectile projectile, Entity hit) {
        Arrowhead head = arrows.remove(projectile.getUniqueId());
        if (head == null) return false;
        flying.remove(projectile);
        Location at = projectile.getLocation();
        projectile.remove();
        if (!canStillHit(head.shooter())) return true;
        if (head.explodes()) {
            at.getWorld().spawnParticle(Particle.EXPLOSION, at, 1);
            at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);
            for (Entity entity : at.getWorld().getNearbyEntities(at, BLAST, BLAST, BLAST)) {
                if (entity.getLocation().distanceSquared(at) <= BLAST * BLAST) hurt(entity, head.shooter(), head.damage());
            }
        } else if (hit != null) {
            hurt(hit, head.shooter(), head.damage());
        }
        return true;
    }

    /** Tank, right click: a wave along the ground ahead, hitting each mob in its path once. */
    void seismicWave(Player player) {
        double damage = seismicWaveDamage(PlayerSession.of(player).stats().get(Stat.DEFENSE));
        Vector step = player.getLocation().getDirection().setY(0);
        if (step.lengthSquared() == 0) step = new Vector(1, 0, 0);
        step.normalize();
        Location start = player.getLocation();
        List<UUID> struck = new ArrayList<>();
        for (int i = 1; i <= WAVE_LENGTH; i++) {
            Location at = start.clone().add(step.clone().multiply(i));
            run.later(i, () -> {
                Block ground = at.clone().subtract(0, 1, 0).getBlock();
                if (ground.getType().isSolid()) {
                    at.getWorld().spawnParticle(Particle.BLOCK, at, 20, 0.4, 0.1, 0.4, ground.getBlockData());
                }
                if (!canStillHit(player)) return;
                for (Entity entity : at.getWorld().getNearbyEntities(at, WAVE_REACH, WAVE_REACH, WAVE_REACH)) {
                    if (!struck.contains(entity.getUniqueId()) && hurt(entity, player, damage)) struck.add(entity.getUniqueId());
                }
            });
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1, 0.6f);
    }

    /**
     * Tank, left click (the damage cut is {@link RunClasses#damageTaken}): every mob around goes for them. The
     * Blood Room's undead aren't vanilla mobs: they choose for themselves ({@link Watcher#targetFor}).
     */
    void castleOfStone(Player player) {
        for (Entity entity : player.getNearbyEntities(AGGRO, AGGRO, AGGRO)) {
            if (entity instanceof Mob mob && (Mobs.of(mob) != null || DungeonMobs.of(mob) != null)) mob.setTarget(player);
        }
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.5f);
    }

    /**
     * Healer, left click: "Heals everyone in your group to full health". The shield "for 20% of their
     * maximum health" isn't built (players have no absorption yet).
     *
     * @return how many were below 25% health before (each takes 10 seconds off its cooldown)
     */
    int wish(Player player) {
        int low = 0;
        for (Player member : run.players()) {
            if (run.ghosts().isGhost(member.getUniqueId())) continue;
            double max = PlayerHealth.max(member);
            if (PlayerHealth.get(member) < WISH_LOW * max) low++;
            PlayerHealth.set(member, max);
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
        return low;
    }

    /**
     * Whether an ability of theirs still going (an arrow in flight, a wave, a thrown axe) may hit: they're
     * still here and alive, and the run still on. A ghost's don't (it can't touch the world, and its
     * kills' drops would go into its inventory).
     */
    boolean canStillHit(Player caster) {
        return caster.isOnline() && caster.getWorld().equals(run.world) && run.phase() == DungeonRun.Phase.RUNNING
                && !run.ghosts().isGhost(caster.getUniqueId());
    }

    /** Hurts one of our mobs that can be hurt (not a crit: abilities' numbers are grey); false for anything else. */
    static boolean hurt(Entity entity, Player by, double damage) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player) return false;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) {
            if (dungeonMob.invulnerable()) return false;
            if (damage > 0) DungeonMobs.damage(entity, by, damage, DamageIndicators.Look.NORMAL);
            return true;
        }
        Mobs.Live mob = Mobs.of(entity);
        if (mob == null || mob.type().isInvulnerable() || mob.health() <= 0) return false;
        if (damage > 0) Mobs.damage(mob, by, damage, DamageIndicators.Look.NORMAL);
        return true;
    }

    void dispose() {
        for (Projectile projectile : flying) projectile.remove();
        flying.clear();
        arrows.clear();
    }
}
