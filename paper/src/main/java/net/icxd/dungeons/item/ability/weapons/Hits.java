package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Utils;

/**
 * SkyBlock's mobs as weapon abilities find and hurt them: the ones that can be hurt (a dungeon's too,
 * not the Watcher), through the same path as a melee hit's damage (their health, damage number, kill
 * and drops, and the run's damage dealt: see {@link Mobs#damage} and {@link DungeonMobs#damage}), and
 * the magic damage an ability does to each (see {@link Magic}). Main thread.
 */
final class Hits {
    /** Mobs frozen by an ability (Ice Spray) until when, by entity: "Frozen mobs take 10% increased damage!". */
    private static final Map<UUID, Long> FROZEN = new HashMap<>();
    static final double FROZEN_TAKEN = 1.1;
    /** Slowness this strong stops a mob walking (each level takes 15% of its speed). */
    private static final int ROOTED = 6;

    private Hits() {
    }

    /** Whether an ability can hurt it: one of SkyBlock's mobs, alive and not invulnerable. Never a player. */
    static boolean hittable(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity instanceof Player || entity.isDead()) return false;
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) return !dungeonMob.invulnerable();
        Mobs.Live mob = Mobs.of(entity);
        return mob != null && !mob.type().isInvulnerable() && mob.health() > 0;
    }

    /** What a hit on it is worked out against (it must be {@link #hittable}). */
    static Damage.Target target(LivingEntity entity) {
        DungeonMobs.Mob dungeonMob = DungeonMobs.of(entity);
        if (dungeonMob != null) {
            return new Damage.Target(dungeonMob.health(), dungeonMob.maxHealth(), dungeonMob.defense(), dungeonMob.magicResistance(),
                    dungeonMob.types(), DungeonMobs.hitsTaken(entity));
        }
        return Mobs.of(entity).target();
    }

    /** The mobs it can hurt whose hitbox passes {@code test}, of those in {@code around}, nearest {@code from} first. */
    static List<LivingEntity> find(World world, BoundingBox around, Vector from, Predicate<BoundingBox> test) {
        List<LivingEntity> found = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(around, Hits::hittable)) {
            if (test.test(entity.getBoundingBox())) found.add((LivingEntity) entity);
        }
        found.sort(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceSquared(from)));
        return found;
    }

    /** The mobs it can hurt with any part within {@code radius} of {@code center}, nearest first. */
    static List<LivingEntity> near(Location center, double radius) {
        Vector c = center.toVector();
        return find(center.getWorld(), BoundingBox.of(c, radius, radius, radius), c, box -> Shapes.inBall(c, radius, box));
    }

    /** In the cone from their eyes the way they look ({@code length} long, {@code degrees} wide), nearest first. */
    static List<LivingEntity> inCone(Player player, double length, double degrees) {
        Location eye = player.getEyeLocation();
        Vector apex = eye.toVector();
        Vector direction = eye.getDirection();
        return find(player.getWorld(), BoundingBox.of(apex, length, length, length), apex,
                box -> Shapes.inCone(apex, direction, length, degrees, box));
    }

    /** Along the line from {@code start} ({@code length} long, reaching {@code width} to the sides), nearest first. */
    static List<LivingEntity> along(Location start, Vector direction, double length, double width) {
        Vector from = start.toVector();
        Vector to = from.clone().add(direction.clone().normalize().multiply(length));
        return find(start.getWorld(), BoundingBox.of(from, to).expand(width), from,
                box -> Shapes.along(from, direction, length, width, box) >= 0);
    }

    /**
     * What a dungeon item's ability base damage is multiplied by in a run, as its own stats are (see
     * {@link ItemBuilder#dungeonFactor}); 1 anywhere else, or for any other item.
     */
    static double dungeonFactor(Player caster, SkyBlockItem item, NBTTagCompound tag) {
        if (!item.dungeonItem() || !RunManager.inRun(caster)) return 1;
        return ItemBuilder.dungeonFactor(Stat.DAMAGE, Math.min(ItemBuilder.starCount(tag), 5), ItemBuilder.catacombsBoost(caster));
    }

    /** What the caster's spell does to this mob (see {@link Magic#damage}). */
    static double magic(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, LivingEntity entity) {
        Stats stats = PlayerSession.of(caster).stats();
        Damage.Target target = target(entity);
        double additive = Magic.additive(Skills.combatLevel(caster), PlayerHealth.get(caster), Combat.heldEnchantments(caster), target);
        Magic.Caster by = new Magic.Caster(stats.get(Stat.INTELLIGENCE), stats.get(Stat.ABILITY_DAMAGE), additive);
        return Magic.damage(spell, dungeonFactor(caster, item, tag), by, target);
    }

    /**
     * Casts the spell on each of them (their damage numbers gray: abilities don't crit), and returns
     * the hits for the chat line.
     */
    static Tally spell(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, List<LivingEntity> targets) {
        Tally tally = new Tally();
        for (LivingEntity entity : targets) {
            if (!hittable(entity)) continue;
            double damage = magic(caster, item, tag, spell, entity) * takenFactor(entity);
            if (hurt(caster, entity, damage, DamageIndicators.Look.NORMAL)) tally.add(damage);
        }
        return tally;
    }

    /** Says what it hit ("Your Implosion hit 3 enemies for ..."), if it hit anything. */
    static void report(Player caster, String name, Tally tally) {
        String message = tally.message(name);
        if (message != null) caster.sendMessage(Utils.color(message));
    }

    /**
     * An ability's hit that's worked out as a melee hit (or an arrow, {@code ranged}) with what they hold,
     * times {@code factor} ("dealing 10% melee damage"): its enchantments, their Strength and Crit Damage,
     * a crit as their Crit Chance rolls it unless {@code alwaysCrits}. Its damage number shows the crit.
     * Ferocity doesn't strike again for it (UNKNOWN whether Hypixel's do). Returns the damage, 0 if it
     * didn't hit.
     */
    static double weaponHit(Player player, NBTTagCompound weapon, LivingEntity entity, boolean ranged, double travelled,
                            double factor, boolean alwaysCrits) {
        if (!hittable(entity)) return 0;
        Damage.Attacker a = Combat.attacker(player, weapon, ranged, travelled);
        Damage.Attacker attacker = new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(),
                a.health(), a.enchantments(), a.ranged(), a.travelled(), a.multiplier() * factor);
        boolean critical = alwaysCrits || Damage.crits(attacker.critChance(), ThreadLocalRandom.current().nextDouble());
        double damage = Math.floor(Damage.exact(attacker, target(entity), critical) * takenFactor(entity));
        return hurt(player, entity, damage, DamageIndicators.Look.of(critical, false)) ? damage : 0;
    }

    /** Hurts one of SkyBlock's mobs for this much, with its damage number; false if it can't be hurt. */
    static boolean hurt(Player by, LivingEntity entity, double damage, DamageIndicators.Look look) {
        if (!hittable(entity)) return false;
        if (DungeonMobs.of(entity) != null) {
            DungeonMobs.damage(entity, by, damage, look);
        } else {
            Mobs.damage(Mobs.of(entity), by, damage, look);
        }
        return true;
    }

    /**
     * Whether something of theirs still going (a missile in flight, a wave) may hit: they're still on and
     * alive, and not a dungeon ghost (ghosts are invulnerable, and their abilities in flight don't hit).
     */
    static boolean canStillHit(Player caster) {
        return caster.isOnline() && !caster.isDead() && !caster.isInvulnerable();
    }

    /**
     * Frozen for this long: it can't walk (its attacks and abilities go on: "Frozen mobs can still take
     * knockback and use abilities", the wiki's Ice Spray Wand), and takes 10% more from abilities' hits
     * (from melee and arrows too on Hypixel: not yet, Combat has no factor for what a mob takes).
     */
    static void freeze(LivingEntity entity, int ticks) {
        root(entity, ticks);
        long now = System.currentTimeMillis();
        // The ones that thawed (or died frozen) are forgotten.
        FROZEN.values().removeIf(until -> until <= now);
        FROZEN.put(entity.getUniqueId(), now + ticks * 50L);
    }

    /** Can't walk for this long ("rooting" them, as Shadow Fury does); nothing else. */
    static void root(LivingEntity entity, int ticks) {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, ROOTED, false, false, false));
    }

    /** What an ability's hit on it is multiplied by: 1.1 while it's frozen, else 1. */
    static double takenFactor(Entity entity) {
        Long until = FROZEN.get(entity.getUniqueId());
        if (until == null) return 1;
        if (until > System.currentTimeMillis()) return FROZEN_TAKEN;
        FROZEN.remove(entity.getUniqueId());
        return 1;
    }
}
