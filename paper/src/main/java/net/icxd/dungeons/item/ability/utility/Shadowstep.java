package net.icxd.dungeons.item.ability.utility;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerSession;

/**
 * The Silent Death's Shadowstep: "Teleport behind the enemy you are looking at, gaining +25❁ Strength for 10
 * seconds. Max range of 20 blocks. Cooldown resets on kills." (the numbers are its text's; "reset upon
 * killing an enemy with any weapon", the wiki: see {@link UtilityListener}). The enemy is the first SkyBlock
 * mob the line from their eyes meets before a block, and behind it is its far side from them, a block past
 * its middle, facing it (UNKNOWN: where Hypixel puts them isn't written down); where that's in a wall they
 * land where the mob stands. With no enemy there, nothing happens and nothing is spent (UNKNOWN).
 */
final class Shadowstep implements AbilityHandler {
    static final String NAME = "Shadowstep";
    private static final double RANGE = 20;
    /** Past the mob's middle, in blocks. */
    private static final double BEHIND = 1;
    private static final double RAY_SIZE = 0.3;

    @Override
    public boolean usable(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        return target(player, range(block)) != null;
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        LivingEntity target = target(player, range(block));
        if (target == null) return;
        player.teleport(behind(player, target));
        player.setFallDistance(0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.8f);
        String plain = AbilityText.plain(block.text());
        long millis = (long) AbilityText.millis(plain).orElse(0);
        if (millis > 0) Buffs.give(player, NAME, AbilityText.stats(plain), millis);
    }

    private static double range(ItemBlock block) {
        return AbilityText.after(AbilityText.plain(block.text()), "Max range of").orElse(RANGE);
    }

    /** The SkyBlock mob they're looking at within {@code range}, not behind a block; null if none. */
    static LivingEntity target(Player player, double range) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTrace(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true, RAY_SIZE,
                entity -> entity instanceof LivingEntity && !(entity instanceof Player) && enemy(entity));
        return hit != null && hit.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean enemy(Entity entity) {
        if (entity.isDead()) return false;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) return !dungeonMob.invulnerable();
        Mobs.Live mob = Mobs.of(entity);
        return mob != null && !mob.type().isInvulnerable();
    }

    /** Its far side from them, facing it; where the mob stands if that's in a wall. */
    static Location behind(Player player, LivingEntity target) {
        Location at = target.getLocation();
        Vector away = at.toVector().subtract(player.getLocation().toVector()).setY(0);
        if (away.lengthSquared() == 0) away = player.getLocation().getDirection().setY(0);
        if (away.lengthSquared() == 0) away = new Vector(1, 0, 0);
        away.normalize();
        Location spot = at.clone().add(away.clone().multiply(BEHIND));
        if (!roomFor(spot)) spot = at.clone();
        spot.setDirection(away.multiply(-1));
        spot.setPitch(0);
        return spot;
    }

    private static boolean roomFor(Location feet) {
        World world = feet.getWorld();
        return world.getBlockAt(feet).isPassable() && world.getBlockAt(feet.clone().add(0, 1, 0)).isPassable();
    }

    /** A kill: their Shadowstep is ready again. */
    static void killed(Player killer) {
        PlayerSession.of(killer).startCooldown("ability:" + NAME, 0);
    }
}
