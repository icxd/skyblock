package net.icxd.dungeons.item.ability.weapons;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * Shadow Fury: "Rapidly teleports you to up to 5 enemies within 12 blocks, rooting each of them and allowing
 * you to hit them"; "teleport behind up to 5 enemies within a radius of 12 blocks in rapid succession, dealing
 * damage to them based on your currently held item" (the wiki). Nearest first; behind each (facing it) they
 * hit it with what they hold, as a melee hit of their own. How rapid is UNKNOWN (every 5 ticks), so is how
 * long the rooting lasts (until the last jump, and a second more), and they stay where the last jump took
 * them (UNKNOWN). A spot behind a mob with no room for them is its other side. Only mobs they can see are
 * jumped to (UNKNOWN: the wiki doesn't say), so it can't take them through a wall into a room they haven't
 * opened, as no other teleport here can.
 */
final class ShadowFury implements AbilityHandler {
    static final int MOST = 5;
    static final double RADIUS = 12;
    private static final int EVERY = 5;
    private static final double BEHIND = 1.2;

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        List<LivingEntity> near = Hits.near(player.getLocation(), RADIUS).stream().filter(player::hasLineOfSight).toList();
        List<LivingEntity> targets = near.subList(0, Math.min(MOST, near.size()));
        int rooted = targets.size() * EVERY + 20;
        for (LivingEntity mob : targets) Hits.root(mob, rooted);
        new BukkitRunnable() {
            private int next;

            @Override
            public void run() {
                if (next >= targets.size() || !Hits.canStillHit(player)) {
                    cancel();
                    return;
                }
                LivingEntity mob = targets.get(next++);
                if (!Hits.hittable(mob) || !mob.getWorld().equals(player.getWorld())) return;
                player.teleport(behind(player, mob));
                player.setFallDistance(0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1.3f);
                player.attack(mob);
            }
        }.runTaskTimer(Dungeons.getInstance(), 0, EVERY);
    }

    /** Behind it, facing it; its other side if there's no room behind. */
    private static Location behind(Player player, LivingEntity mob) {
        Location at = mob.getLocation();
        Vector back = at.getDirection().setY(0);
        if (back.lengthSquared() < 1e-6) back = at.toVector().subtract(player.getLocation().toVector()).setY(0);
        if (back.lengthSquared() < 1e-6) back = new Vector(1, 0, 0);
        back.normalize().multiply(-BEHIND);
        Location spot = at.clone().add(back);
        if (!room(spot)) spot = at.clone().subtract(back);
        if (!room(spot)) spot = at.clone();
        Vector look = mob.getBoundingBox().getCenter().subtract(spot.toVector().add(new Vector(0, player.getEyeHeight(), 0)));
        spot.setDirection(look);
        return spot;
    }

    private static boolean room(Location feet) {
        Block block = feet.getBlock();
        return block.isPassable() && block.getRelative(0, 1, 0).isPassable();
    }
}
