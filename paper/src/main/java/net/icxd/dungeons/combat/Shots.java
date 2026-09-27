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

import java.util.HashMap;
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
     * An arrow in flight: who shot it, with what (and their Ferocity then), from where, and whether it crits,
     * and whether that's an Overload Mega Critical Hit.
     */
    record Shot(Damage.Attacker launched, boolean critical, boolean megaCritical, double ferocity, Location from) {
        /** The attacker, with how far the arrow has come (Snipe) by the time it hits, and a mega-crit's Overload. */
        Damage.Attacker attacker(Location at) {
            double travelled = from.getWorld().equals(at.getWorld()) ? from.distance(at) : 0;
            Damage.Attacker a = launched;
            double overload = megaCritical ? Damage.overload(a.enchantments().getOrDefault("overload", 0)) : 1;
            return new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(), a.health(),
                    a.enchantments(), true, travelled, a.multiplier() * overload);
        }
    }

    private static final Map<UUID, Shot> SHOTS = new HashMap<>();

    /**
     * A player shot this projectile with this bow (its SkyBlock data; null for none); fully drawn or not. An
     * arrow can't be picked up once it lands, as a Terminator's can't on Hypixel (the owner).
     */
    public static void record(Projectile projectile, Player shooter, NBTTagCompound bow, boolean fullyDrawn) {
        if (projectile instanceof AbstractArrow arrow) arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        Damage.Attacker attacker = Combat.attacker(shooter, bow, true, 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        boolean critical = fullyDrawn && Damage.crits(attacker.critChance(), random.nextDouble());
        // Overload is a bow's enchantment, so (as Power) it's the arrow's.
        boolean megaCritical = critical && attacker.enchantments().getOrDefault("overload", 0) > 0
                && Damage.megaCrits(attacker.critChance(), random.nextDouble());
        double ferocity = PlayerSession.of(shooter).stats().get(Stat.FEROCITY);
        SHOTS.put(projectile.getUniqueId(), new Shot(attacker, critical, megaCritical, ferocity, projectile.getLocation()));
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
        record(projectile, player, Combat.skyBlockData(event.getBow()), event.getForce() >= 1);
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
