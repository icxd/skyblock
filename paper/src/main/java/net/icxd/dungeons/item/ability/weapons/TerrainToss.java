package net.icxd.dungeons.item.ability.weapons;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.AbilityHandler;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;

/**
 * The Yeti Sword's Terrain Toss: "Throws a chunk of terrain in the direction you are facing! Deals up to
 * 15,000 damage. Maximum range of 32 blocks." "A small piece of terrain is tossed upward towards the
 * player's cursor, exploding on impact. The damage dealt by the explosion scales not only with the
 * player's Intelligence, but also with distance between the mob and the explosion center"; "If the player
 * is midair ... the game throws a single Packed Ice block" (the wiki); base 15,000 (17,000 fragged) with
 * Intelligence scaling 0.3. The blast's size and how its damage falls off are UNKNOWN: 5 blocks, all of
 * it at the middle down to none at the edge. Its chat line is UNKNOWN (Terrain Toss's, in the others' form).
 */
final class TerrainToss implements AbilityHandler {
    static final Magic.Spell BOULDER = new Magic.Spell(15_000, 0.3);
    static final double RANGE = 32;
    static final double RADIUS = 5;
    private static final double SPEED = 1.2;
    private static final double GRAVITY = 0.08;

    /** What share of its damage a mob this far from the middle takes. */
    static double falloff(double distance, double radius) {
        return Math.max(0, Math.min(1, 1 - distance / radius));
    }

    /**
     * The first velocity (blocks a tick) that lands a throw falling {@code gravity} a tick at {@code to} after
     * {@code ticks} steps of the missile (each tick it falls, then moves).
     */
    static Vector lob(Vector from, Vector to, int ticks, double gravity) {
        Vector velocity = to.clone().subtract(from).multiply(1.0 / ticks);
        return velocity.setY(velocity.getY() + gravity * (ticks + 1) / 2);
    }

    @Override
    public void use(Player player, SkyBlockItem item, NBTTagCompound tag, ItemBlock block) {
        Magic.Spell spell = Hits.spellOf(item, BOULDER);
        Location eye = player.getEyeLocation();
        RayTraceResult aim = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), RANGE, FluidCollisionMode.NEVER, true);
        Vector to = aim != null ? aim.getHitPosition() : eye.toVector().add(eye.getDirection().multiply(RANGE));
        int ticks = Math.max(1, (int) Math.ceil(to.distance(eye.toVector()) / SPEED));
        new Missile(player, eye, lob(eye.toVector(), to, ticks, GRAVITY))
                .gravity(GRAVITY)
                .range(RANGE * 3)
                .width(0.5)
                .look(Missile.display(eye, new ItemStack(chunk(player)), 1, 0))
                .onEnd((missile, at, impact) -> explode(missile.caster(), item, tag, spell, at))
                .launch();
        player.getWorld().playSound(eye, Sound.BLOCK_STONE_BREAK, 1, 0.8f);
    }

    /** What's under their feet, or Packed Ice in the air (or for anything that isn't an item). */
    private static Material chunk(Player player) {
        Material under = player.getLocation().subtract(0, 0.5, 0).getBlock().getType();
        return under.isSolid() && under.isItem() ? under : Material.PACKED_ICE;
    }

    private static void explode(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, Location at) {
        at.getWorld().spawnParticle(Particle.EXPLOSION, at, 2, 1, 0.5, 1, 0);
        at.getWorld().playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.9f);
        Tally tally = new Tally();
        for (LivingEntity mob : Hits.near(at, RADIUS)) {
            double share = falloff(mob.getBoundingBox().getCenter().distance(at.toVector()), RADIUS);
            double damage = Hits.magic(caster, item, tag, spell, mob) * share * Hits.takenFactor(mob);
            if (damage > 0 && Hits.hurt(caster, mob, damage, DamageIndicators.Look.NORMAL, tag)) tally.add(damage);
        }
        Hits.report(caster, "Terrain Toss", tally);
    }
}
