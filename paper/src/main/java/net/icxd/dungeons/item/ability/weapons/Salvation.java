package net.icxd.dungeons.item.ability.weapons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;

/**
 * The Terminator's Salvation (LEFT CLICK): "Can be cast after landing 3 hits. Shoot a beam, penetrating up
 * to 5 enemies. The beam always crits." "The beam has a range of 32 blocks ... having a 0.25s cooldown"
 * (the wiki). Its arrows' hits on SkyBlock's mobs count (each of a volley's three); with fewer than 3 a
 * left click shoots as ever, and so it does in the 0.25 s after a beam. The beam hits as the bow's arrow
 * would (its enchantments, the player's Strength and Crit Damage, Snipe by how far it went), always a crit.
 * Its 1 Soulflow isn't charged: there's no Soulflow yet (UNKNOWN how it'd be spent). Blocks stop the beam
 * (UNKNOWN). The action bar's "T1", "T2", "T3!" isn't shown (LATER: the action bar has no place for it).
 */
final class Salvation implements AbilityHandler {
    static final String NAME = "Salvation";
    static final int HITS_NEEDED = 3;
    static final int MOST_HIT = 5;
    static final double RANGE = 32;
    static final long COOLDOWN_MILLIS = 250;
    /** How far to the side of the beam a mob's hitbox can be and still be hit: UNKNOWN. */
    private static final double WIDTH = 0.3;
    /** On a Terminator's arrows, so their hits count towards it. */
    static final String ARROW_TAG = "skyblock_salvation";

    /** Each player's hits towards the next beam. */
    private static final Map<UUID, Integer> LANDED = new HashMap<>();

    /** One of their arrows landed on a mob. */
    static void landed(UUID player) {
        LANDED.merge(player, 1, (a, b) -> Math.min(HITS_NEEDED, a + b));
    }

    /** Whether they've landed the 3 hits a beam takes. */
    static boolean charged(UUID player) {
        return LANDED.getOrDefault(player, 0) >= HITS_NEEDED;
    }

    /** A beam starts the count again. */
    static void spend(UUID player) {
        LANDED.remove(player);
    }

    static void forget(UUID player) {
        LANDED.remove(player);
    }

    /** A shortbow's arrow counts towards Salvation if the bow has it. */
    static void shot(AbstractArrow arrow, SkyBlockItem bow) {
        if (bow.blocks().stream().anyMatch(block -> block.isAbility() && NAME.equals(block.name()))) arrow.addScoreboardTag(ARROW_TAG);
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return charged(player.getUniqueId()) && PlayerSession.of(player).cooldownLeft("salvation") <= 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        spend(player.getUniqueId());
        PlayerSession.of(player).startCooldown("salvation", COOLDOWN_MILLIS);
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        RayTraceResult wall = player.getWorld().rayTraceBlocks(eye, direction, RANGE, FluidCollisionMode.NEVER, true);
        double length = wall == null ? RANGE : wall.getHitPosition().distance(eye.toVector());
        List<LivingEntity> struck = Hits.along(eye, direction, length, WIDTH);
        for (LivingEntity mob : struck.subList(0, Math.min(MOST_HIT, struck.size()))) {
            double travelled = mob.getBoundingBox().getCenter().distance(eye.toVector());
            Hits.weaponHit(player, tag, mob, new Hits.Strike(true, travelled, 1, true, false));
        }
        beam(eye, direction, length);
    }

    /** How the beam looks and sounds is UNKNOWN: a thin red line and a bow's shot. */
    private static void beam(Location eye, Vector direction, double length) {
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 0.8f);
        Vector step = direction.clone().normalize().multiply(0.4);
        Location at = eye.clone().add(step);
        for (double gone = 0.4; gone <= length; gone += 0.4, at.add(step)) {
            eye.getWorld().spawnParticle(Particle.DUST, at, 1, 0, 0, 0, 0, red);
        }
        eye.getWorld().playSound(eye, Sound.ENTITY_ARROW_SHOOT, 1, 1.4f);
    }
}
