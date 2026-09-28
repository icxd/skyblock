package net.icxd.dungeons.combat;

import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.stats.Stat;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Players' arrows hit with what they had when the arrow left the bow: their stats (with that bow in
 * hand, Ferocity too), its enchantments and whether it crits, rolled then, not with whatever they hold
 * when it lands. Bows only crit fully drawn (the wiki's Bow history); how a bow's damage scales with a
 * partial draw is UNKNOWN, so it doesn't. Main thread.
 */
public final class Shots implements Listener {
    /**
     * An arrow in flight: who shot it, with what (and their Ferocity then), from where, whether it crits,
     * and whether that's an Overload Mega Critical Hit, and the bow it left (its SkyBlock data; null for
     * none), so a hit knows which bow's it is whatever they hold when it lands.
     */
    record Shot(Damage.Attacker launched, boolean critical, boolean megaCritical, double ferocity, Location from, NBTTagCompound bow) {
        /** The attacker, with how far the arrow has come (Snipe) by the time it hits, and a mega-crit's Overload. */
        Damage.Attacker attacker(Location at) {
            double travelled = from.getWorld().equals(at.getWorld()) ? from.distance(at) : 0;
            Damage.Attacker a = launched;
            double overload = megaCritical ? Damage.overload(a.enchantments().getOrDefault("overload", 0)) : 1;
            return new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(), a.health(),
                    a.enchantments(), true, travelled, a.multiplier() * overload);
        }

        Shot times(double factor) {
            Damage.Attacker a = launched;
            return new Shot(new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(), a.health(),
                    a.enchantments(), true, a.travelled(), a.multiplier() * factor), critical, megaCritical, ferocity, from, bow);
        }
    }

    /** Something that happens when a player draws a bow and shoots (see {@link #addShotListener}). */
    @FunctionalInterface
    public interface ShotListener {
        /** {@code bow} is the bow's SkyBlock data (null for none); the arrow's shot is recorded by now. */
        void shot(Player player, Projectile projectile, NBTTagCompound bow, boolean fullyDrawn);
    }

    private static final Map<UUID, Shot> SHOTS = new HashMap<>();
    private static final List<ShotListener> SHOT_LISTENERS = new ArrayList<>();

    /**
     * Adds something that happens when a player shoots a drawn bow (vanilla's shot, not a shortbow's, which
     * its ability shoots): after the arrow's shot is recorded, so it can change it ({@link #scale}). The ON_SHOOT
     * abilities come through here (see {@code Activations}).
     */
    public static void addShotListener(ShotListener listener) {
        SHOT_LISTENERS.add(listener);
    }

    /**
     * A player shot this projectile with this bow (its SkyBlock data; null for none); fully drawn or not. An
     * arrow can't be picked up once it lands, as a Terminator's can't on Hypixel (the owner).
     */
    public static void record(Projectile projectile, Player shooter, NBTTagCompound bow, boolean fullyDrawn) {
        record(projectile, shooter, bow, fullyDrawn, 1);
    }

    /** As {@link #record(Projectile, Player, NBTTagCompound, boolean)}, its damage times {@code factor} (70% for Rapid Fire's). */
    public static void record(Projectile projectile, Player shooter, NBTTagCompound bow, boolean fullyDrawn, double factor) {
        if (projectile instanceof AbstractArrow arrow) arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        Damage.Attacker a = Combat.attacker(shooter, bow, true, 0);
        Damage.Attacker attacker = factor == 1 ? a : new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(),
                a.combatLevel(), a.health(), a.enchantments(), true, 0, a.multiplier() * factor);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean critical = fullyDrawn && Damage.crits(attacker.critChance(), random.nextDouble());
        // Overload is a bow's enchantment, so (as Power) it's the arrow's.
        boolean megaCritical = critical && attacker.enchantments().getOrDefault("overload", 0) > 0
                && Damage.megaCrits(attacker.critChance(), random.nextDouble());
        double ferocity = PlayerSession.of(shooter).stats().get(Stat.FEROCITY);
        SHOTS.put(projectile.getUniqueId(), new Shot(attacker, critical, megaCritical, ferocity, projectile.getLocation(), bow));
    }

    /**
     * The recorded arrow's damage times {@code factor} from now on, as a multiplicative buff (Arrow Infusion's
     * "double the damage per shot": 2); false if it isn't a recorded arrow (any more).
     */
    public static boolean scale(Projectile projectile, double factor) {
        Shot shot = SHOTS.get(projectile.getUniqueId());
        if (shot == null) return false;
        SHOTS.put(projectile.getUniqueId(), shot.times(factor));
        return true;
    }

    /** The bow a recorded arrow left (its SkyBlock data); null if it isn't one, or the bow wasn't a SkyBlock item. */
    public static NBTTagCompound bow(Projectile projectile) {
        Shot shot = SHOTS.get(projectile.getUniqueId());
        return shot == null ? null : shot.bow();
    }

    /** The shot this projectile is, once (null if it isn't one). */
    static Shot take(Projectile projectile) {
        return SHOTS.remove(projectile.getUniqueId());
    }

    /**
     * Whether a player's projectile is a hit when it lands on a mob: an arrow is (one that wasn't
     * recorded, as a piercing bolt is on its second mob, hits with their stats now), a snowball, egg or
     * ender pearl isn't.
     */
    static boolean hits(Projectile projectile) {
        return projectile instanceof AbstractArrow;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Projectile projectile)) return;
        NBTTagCompound bow = Combat.skyBlockData(event.getBow());
        boolean fullyDrawn = event.getForce() >= 1;
        record(projectile, player, bow, fullyDrawn);
        for (ShotListener listener : SHOT_LISTENERS) listener.shot(player, projectile, bow, fullyDrawn);
    }

    /**
     * An arrow that lands in a block can't hit anything any more. In the Catacombs arrows don't wait
     * out a mob's invulnerability after a hit (the wiki's Catacombs rules).
     */
    @EventHandler
    public void onHit(ProjectileHitEvent event) {
        if (event.getHitBlock() != null) {
            SHOTS.remove(event.getEntity().getUniqueId());
            return;
        }
        if (!hits(event.getEntity()) || !(event.getHitEntity() instanceof LivingEntity target)
                || !(event.getEntity().getShooter() instanceof Player player)) return;
        if ((Mobs.of(target) != null || DungeonMobs.of(target) != null) && RunManager.inRun(player)) target.setNoDamageTicks(0);
    }

    @EventHandler
    public void onRemove(EntityRemoveEvent event) {
        if (event.getEntity() instanceof Projectile) SHOTS.remove(event.getEntity().getUniqueId());
    }
}
