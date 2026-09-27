package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.session.PlayerSession;

/**
 * The Soul Whip's (and Flaming Flay's) Flay: "Flay your whip in an arc, dealing your melee damage to all
 * enemies in its path." "This beam has a parabolic trajectory, traveling forward 13 blocks and upwards 2
 * blocks. The beam travels through enemies, damaging all in its path; for the first 3 enemies hit, it deals
 * its melee damage, then it is halved for every subsequent enemy hit. The ability does not consume mana, but
 * has a hidden cooldown of 0.5s"; "Melee-only enchantments ... work on the beam" (the wiki). The curve is the
 * wiki's own, y = -8/169 (x - 6.5)² + 2, along their aim. Flaming Flay's healing from Sea Creatures waits for
 * sea creatures (LATER).
 */
final class Flay implements AbilityHandler {
    static final double LENGTH = 13;
    static final int FULL_HITS = 3;
    static final long COOLDOWN_MILLIS = 500;
    /** How close to the curve a hitbox has to be (UNKNOWN). */
    private static final double WIDTH = 0.5;
    private static final double STEP = 0.5;

    /** The share of their melee damage its {@code hit}th enemy (from 0) takes: 1, 1, 1, then 1/2, 1/4 ... */
    static double share(int hit) {
        return hit < FULL_HITS ? 1 : Math.pow(0.5, hit - FULL_HITS + 1);
    }

    /** How far above their aim the curve is, {@code x} blocks along it. */
    static double height(double x) {
        return -8.0 / 169 * (x - 6.5) * (x - 6.5) + 2;
    }

    @Override
    public boolean casts(Player player, SkyBlockItem item, NBTTagCompound tag) {
        return PlayerSession.of(player).cooldownLeft("flay") <= 0;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        PlayerSession.of(player).startCooldown("flay", COOLDOWN_MILLIS);
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        List<Vector> curve = new ArrayList<>();
        for (double x = STEP; x <= LENGTH; x += STEP) {
            curve.add(eye.toVector().add(direction.clone().multiply(x)).add(new Vector(0, height(x), 0)));
        }
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(70, 40, 20), 1);
        List<LivingEntity> struck = new ArrayList<>();
        BoundingBox around = BoundingBox.of(eye.toVector(), curve.getLast()).expand(3);
        List<Entity> candidates = new ArrayList<>(player.getWorld().getNearbyEntities(around, Hits::hittable));
        // Along the curve, so the first it reaches is the first hit.
        for (Vector point : curve) {
            player.getWorld().spawnParticle(Particle.DUST, point.getX(), point.getY(), point.getZ(), 1, 0, 0, 0, 0, dust);
            for (Entity entity : candidates) {
                LivingEntity mob = (LivingEntity) entity;
                if (!struck.contains(mob) && Shapes.inBall(point, WIDTH, mob.getBoundingBox())) struck.add(mob);
            }
        }
        for (int i = 0; i < struck.size(); i++) Hits.weaponHit(player, tag, struck.get(i), Hits.Strike.melee(share(i)));
        player.getWorld().playSound(eye, Sound.ENTITY_FISHING_BOBBER_THROW, 1, 0.6f);
    }
}
