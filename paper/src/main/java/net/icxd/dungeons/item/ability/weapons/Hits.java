package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import net.icxd.dungeons.Dungeons;
import net.icxd.dungeons.combat.Combat;
import net.icxd.dungeons.combat.Damage;
import net.icxd.dungeons.combat.DamageIndicators;
import net.icxd.dungeons.dungeons.instance.DungeonMobs;
import net.icxd.dungeons.dungeons.instance.RunManager;
import net.icxd.dungeons.item.ItemBuilder;
import net.icxd.dungeons.item.SkyBlockItem;
import net.icxd.dungeons.item.ability.Abilities;
import net.icxd.dungeons.item.data.ItemBlock;
import net.icxd.dungeons.item.nbt.NBTTagCompound;
import net.icxd.dungeons.mob.Mobs;
import net.icxd.dungeons.session.PlayerHealth;
import net.icxd.dungeons.session.PlayerSession;
import net.icxd.dungeons.skill.Skills;
import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;
import net.icxd.dungeons.utils.Replacement;
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

    /**
     * What the caster's spell does to this mob (see {@link Magic#damage}): with the enchantments of the item
     * it was cast with ({@code tag}, even if a skull lands after they've switched), and their stats now
     * (UNKNOWN whether Hypixel's are the cast's). 0 for one that can't be hurt (any more: the loops that
     * call this hurt one mob after another, and a hit can end more than its own mob).
     */
    static double magic(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, LivingEntity entity) {
        if (!hittable(entity)) return 0;
        Stats stats = PlayerSession.of(caster).stats();
        Damage.Target target = target(entity);
        double additive = Magic.additive(Skills.combatLevel(caster), PlayerHealth.get(caster), Combat.enchantments(tag), target);
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
     * How an ability's hit that's worked out as a melee hit or an arrow goes: an arrow's ({@code ranged}, Snipe
     * by how far it went), times {@code factor} ("dealing 10% melee damage"), always a crit or as their Crit
     * Chance rolls, and with only the enchantments that count for abilities ({@code forAbility}: the
     * roses' "Enchantments that do not affect abilities, such as Sharpness ... do not work"), or all of them
     * (the whip's "Melee-only enchantments ... work on the beam").
     */
    record Strike(boolean ranged, double travelled, double factor, boolean alwaysCrits, boolean forAbility) {
        static Strike melee(double factor) {
            return new Strike(false, 0, factor, false, false);
        }

        static Strike arrow(double travelled, double factor) {
            return new Strike(true, travelled, factor, false, false);
        }
    }

    /**
     * What they strike with now: their stats (with what they hold) and the weapon's enchantments, only those
     * that count for abilities if the strike says so. Taken when a thrown weapon or a rose leaves them, so
     * its hits are the throw's, not whatever they hold when it lands (as an arrow's are the bow's it left:
     * see Shots).
     */
    static Damage.Attacker striker(Player player, NBTTagCompound weapon, Strike strike) {
        Damage.Attacker a = Combat.attacker(player, weapon, strike.ranged(), 0);
        return strike.forAbility() ? new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(),
                a.health(), Magic.forAbilities(a.enchantments()), a.ranged(), 0, a.multiplier()) : a;
    }

    /** {@link #weaponHit(Player, Damage.Attacker, LivingEntity, Strike)} with what they strike with now. */
    static double weaponHit(Player player, NBTTagCompound weapon, LivingEntity entity, Strike strike) {
        return weaponHit(player, striker(player, weapon, strike), entity, strike);
    }

    /**
     * An ability's hit worked out as a melee hit or an arrow with what they struck with ({@link #striker}: the
     * weapon's enchantments, their Strength and Crit Damage), see {@link Strike}. Its damage number shows a
     * crit. Ferocity doesn't strike again for it (UNKNOWN whether Hypixel's do). Returns the damage, 0 if it
     * didn't hit.
     */
    static double weaponHit(Player player, Damage.Attacker with, LivingEntity entity, Strike strike) {
        if (!hittable(entity)) return 0;
        Damage.Attacker attacker = new Damage.Attacker(with.damage(), with.strength(), with.critChance(), with.critDamage(), with.combatLevel(),
                with.health(), with.enchantments(), strike.ranged(), strike.travelled(), with.multiplier() * strike.factor());
        boolean critical = strike.alwaysCrits() || Damage.crits(attacker.critChance(), ThreadLocalRandom.current().nextDouble());
        double damage = Math.floor(Damage.exact(attacker, target(entity), critical) * takenFactor(entity));
        return hurt(player, entity, damage, DamageIndicators.Look.of(critical, false)) ? damage : 0;
    }

    /** The item's own ability damage ("Weapon Ability Damage" in its data: 2,000 on a Spirit Sceptre), else {@code otherwise}. */
    static double base(SkyBlockItem item, double otherwise) {
        double own = item.stats().get(Stat.WEAPON_ABILITY_DAMAGE);
        return own > 0 ? own : otherwise;
    }

    /** The spell with the item's own base damage, if its data has one. */
    static Magic.Spell spellOf(SkyBlockItem item, Magic.Spell spell) {
        return spell.withBase(base(item, spell.base()));
    }

    private static int mana(PlayerSession session) {
        return Math.max(0, session.getMana() < 0 ? session.maxMana() : session.getMana());
    }

    /**
     * Whether they have the mana an ability costs beyond what its block says (the Jerry-chine Gun's growing
     * cost), asked from {@link net.icxd.dungeons.item.ability.AbilityHandler#usable}; if not, what too little
     * mana always does ("NOT ENOUGH MANA"). It's taken in the use ({@link #takeMana}): PlayerListener sets
     * their mana after asking.
     */
    static boolean enoughMana(Player player, int cost) {
        if (cost <= 0 || mana(PlayerSession.of(player)) >= cost) return true;
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, -4f);
        PlayerSession.of(player).setManaReplacement(Replacement.forMillis("§c§lNOT ENOUGH MANA", 2000));
        return false;
    }

    /** {@link #enoughMana} for the extra cost on top of the block's own (which isn't taken yet when it's asked). */
    static boolean enoughMana(Player player, ItemBlock block, int extra) {
        if (extra <= 0) return true;
        return enoughMana(player, Abilities.manaCost(block, PlayerSession.of(player).maxMana()) + extra);
    }

    /** Takes that mana, shown as a block's cost is ("-60 Mana (Rapid-fire)"). */
    static void takeMana(Player player, int cost, String ability) {
        if (cost <= 0) return;
        PlayerSession session = PlayerSession.of(player);
        session.setMana(Math.max(0, mana(session) - cost));
        session.setDefenseReplacement(Replacement.forMillis("§b-" + cost + " Mana (§6" + ability + "§b)", 400));
    }

    /**
     * Whether they can pay an ability's health cost ("This ability cannot be used if the user does not have
     * enough health to be consumed, so using it repeatedly cannot cause fatal damage", the wiki's Flower of
     * Truth): more health than it costs.
     */
    static boolean canPayHealth(Player player, double cost) {
        return cost <= 0 || PlayerHealth.get(player) > cost;
    }

    static void payHealth(Player player, double cost) {
        if (cost > 0) PlayerHealth.damage(player, cost);
    }

    /**
     * Hurts one of SkyBlock's mobs for this much, with its damage number; false if it can't be hurt. A room
     * mob waiting for its room to open wakes it first, as a melee hit on one does (through a wall too).
     */
    static boolean hurt(Player by, LivingEntity entity, double damage, DamageIndicators.Look look) {
        if (!hittable(entity)) return false;
        RunManager.abilityHit(entity);
        if (DungeonMobs.of(entity) != null) {
            DungeonMobs.damage(entity, by, damage, look);
        } else {
            Mobs.damage(Mobs.of(entity), by, damage, look);
        }
        return true;
    }

    /** What their crosshair is on within {@code range}: the first block along their aim, or as far as it goes. */
    static Location aimed(Player player, double range) {
        Location eye = player.getEyeLocation();
        RayTraceResult hit = player.getWorld().rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true);
        Vector at = hit != null ? hit.getHitPosition() : eye.toVector().add(eye.getDirection().multiply(range));
        return at.toLocation(player.getWorld());
    }

    /** The first mob an ability could hurt along their crosshair within {@code range}, not through blocks; null for none. */
    static LivingEntity aimedMob(Player player, double range) {
        Location eye = player.getEyeLocation();
        double reach = aimed(player, range).distance(eye);
        RayTraceResult hit = player.getWorld().rayTraceEntities(eye, eye.getDirection(), reach, 0.3, Hits::hittable);
        return hit == null ? null : (LivingEntity) hit.getHitEntity();
    }

    /**
     * The spell's damage on each mob {@code every} ticks, {@code times} times, each hit {@code share} of it
     * ("dealing up to 42,000 damage over 10 seconds": a tenth a second): on the mobs {@code targets} finds
     * each time. Stops early when the caster can't hit any more.
     */
    static void overTime(Player caster, SkyBlockItem item, NBTTagCompound tag, Magic.Spell spell, double share, int every, int times,
                         Supplier<List<LivingEntity>> targets) {
        new BukkitRunnable() {
            private int done;

            @Override
            public void run() {
                if (done++ >= times || !canStillHit(caster)) {
                    cancel();
                    return;
                }
                for (LivingEntity mob : targets.get()) {
                    if (!hittable(mob)) continue;
                    hurt(caster, mob, magic(caster, item, tag, spell, mob) * share * takenFactor(mob), DamageIndicators.Look.NORMAL);
                }
            }
        }.runTaskTimer(Dungeons.getInstance(), every, every);
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
