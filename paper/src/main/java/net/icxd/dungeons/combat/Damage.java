package net.icxd.dungeons.combat;

import net.icxd.dungeons.mob.MobType;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Hypixel SkyBlock's damage formulas, as the wiki's Damage Calculation page has them, with no Bukkit
 * in them, so that every hit (a sword, a fist, an arrow, and abilities once they deal damage) is
 * worked out the same way:
 * <pre>
 * initial = (5 + Damage) x (1 + Strength / 100), x (1 + Crit Damage / 100) on a critical hit
 * dealt   = initial x (1 + sum of additive buffs / 100) x product of multiplicative buffs
 *           -> damage caps -> x 100 / (100 + target's Defense) -> rounded down
 * </pre>
 * Additive buffs are the Combat skill's Warrior bonus and the weapon's enchantments (their values are
 * what the enchanted books say in game, enchantments.json); multiplicative ones (dungeon classes and
 * the like) come in as one product, {@link Attacker#multiplier}. Abilities ({@link #initialAbility},
 * {@link #exactMagic}) never crit and go through the target's magic resistance before its caps.
 */
public final class Damage {
    private static final double[] SHARPNESS = {5, 10, 15, 20, 30, 45, 65};
    /** Smite, Bane of Arthropods and Ender Slayer. */
    private static final double[] SLAYER = {5, 10, 15, 20, 30, 40, 50};
    private static final double[] CUBISM = {5, 10, 15, 20, 30, 40};
    /** Smoldering and Gravity (dragon_hunter). */
    private static final double[] SMOLDERING = {5, 10, 15, 20, 30};
    private static final double[] GIANT_KILLER = {0.1, 0.2, 0.3, 0.4, 0.6, 0.9, 1.2};
    private static final double[] GIANT_KILLER_CAP = {5, 10, 15, 20, 30, 45, 65};
    private static final double[] PROSECUTE = {0.1, 0.2, 0.3, 0.4, 0.7, 1};
    private static final double[] EXECUTE = {0.2, 0.4, 0.6, 0.8, 1, 1.25};
    private static final double[] TITAN_KILLER = {2, 4, 6, 8, 12, 16, 20};
    private static final double[] TITAN_KILLER_CAP = {6, 12, 18, 24, 40, 60, 80};
    private static final double[] FIRST_STRIKE = {25, 50, 75, 100, 125};
    private static final double[] TRIPLE_STRIKE = {10, 20, 30, 40, 50};
    private static final double[] POWER = {8, 16, 24, 32, 40, 50, 65};
    private static final double[] SNIPE = {1, 2, 3, 4};
    /** "Removes all other enchants but increases your weapon damage by 500%": additive, not x5. */
    private static final double ONE_FOR_ALL = 500;
    /** Attack Speed's cap (the wiki's Attack Speed, "max_value=100"), before anything raises it. */
    public static final double ATTACK_SPEED_CAP = 100;

    private Damage() {
    }

    /**
     * Who hits, with what: their Damage, Strength, Crit Chance and Crit Damage stats, Combat level and
     * health now, the weapon's enchantments (id to level), whether it's an arrow, how far the arrow
     * flew (Snipe), and the product of the multiplicative buffs (1 for none).
     */
    public record Attacker(double damage, double strength, double critChance, double critDamage, int combatLevel,
                           double health, Map<String, Integer> enchantments, boolean ranged, double travelled,
                           double multiplier) {
        public Attacker {
            enchantments = enchantments == null ? Map.of() : Map.copyOf(enchantments);
        }
    }

    /**
     * What's hit: its health now and at most, its Defense, the share of magic damage it resists (0.1 for
     * 10%), its mob types, how many hits it had taken before this one (First Strike, Triple-Strike), and
     * its damage caps (bosses; none for most).
     */
    public record Target(double health, double maxHealth, double defense, double magicResistance, Set<MobType> types, int hitsTaken,
                         double... caps) {
        public Target {
            types = types == null ? Set.of() : Set.copyOf(types);
            caps = caps == null ? new double[0] : caps.clone();
        }
    }

    /** Crit Chance is a percentage; above 100 every hit crits. */
    public static boolean crits(double critChance, double random) {
        return random * 100 < critChance;
    }

    /**
     * With Overload on the bow, the chance of a crit being a Mega Critical Hit: "Having a Crit Chance above 100%
     * grants a chance to perform a Mega Critical Hit", (Crit Chance - 100) / 100 up to 100% at 200 (the wiki's
     * Damage Calculation, Multiplicative Sources). Crit Chance above 100 does nothing else.
     */
    public static double megaCritChance(double critChance) {
        return Math.max(0, Math.min((critChance - 100) / 100, 1));
    }

    /** Whether a crit is a mega one, for this {@code random} (0 inclusive to 1 exclusive). */
    public static boolean megaCrits(double critChance, double random) {
        return random < megaCritChance(critChance);
    }

    /** A Mega Critical Hit's multiplicative multiplier with Overload at this level: 1 + 10% a level (10 to 50%). */
    public static double overload(int level) {
        return 1 + 0.1 * Math.max(0, Math.min(level, 5));
    }

    /** (5 + Damage) x (1 + Strength / 100). */
    public static double initial(double damage, double strength) {
        return (5 + damage) * (1 + strength / 100);
    }

    public static double critMultiplier(double critDamage) {
        return 1 + critDamage / 100;
    }

    /** The Combat skill's Warrior bonus, in percent: 4 a level to 50, then 1 a level to 60. */
    public static double warrior(int combatLevel) {
        int level = Math.max(0, Math.min(combatLevel, 60));
        return 4 * Math.min(level, 50) + Math.max(0, level - 50);
    }

    /** Damage taken through Defense (or True Defense, for true damage): 100 / (100 + Defense). */
    public static double defenseMultiplier(double defense) {
        return 100 / (100 + Math.max(0, defense));
    }

    /**
     * Past each threshold only a tenth of the rest gets through, lowest threshold first (bosses and
     * minibosses have them).
     */
    public static double cap(double damage, double... thresholds) {
        double[] sorted = thresholds.clone();
        Arrays.sort(sorted);
        for (double threshold : sorted) {
            if (damage > threshold) damage = 0.1 * (damage - threshold) + threshold;
        }
        return damage;
    }

    /** The sum of the additive buffs on this hit, in percent. */
    public static double additive(Attacker attacker, Target target) {
        double sum = warrior(attacker.combatLevel());
        for (Map.Entry<String, Integer> enchantment : attacker.enchantments().entrySet()) {
            sum += enchantment(enchantment.getKey(), enchantment.getValue(), attacker, target);
        }
        return sum;
    }

    /** What one enchantment adds to this hit, in percent (0 for enchantments that add nothing to damage). */
    static double enchantment(String id, int level, Attacker attacker, Target target) {
        boolean melee = !attacker.ranged();
        return switch (id) {
            case "sharpness" -> melee ? at(SHARPNESS, level) : 0;
            case "smite" -> target.types().contains(MobType.UNDEAD) || target.types().contains(MobType.SKELETAL)
                    || target.types().contains(MobType.WITHER) ? at(SLAYER, level) : 0;
            case "bane_of_arthropods" -> target.types().contains(MobType.ARTHROPOD) ? at(SLAYER, level) : 0;
            case "ender_slayer" -> target.types().contains(MobType.ENDER) ? at(SLAYER, level) : 0;
            case "cubism" -> target.types().contains(MobType.CUBIC) ? at(CUBISM, level) : 0;
            case "smoldering" -> target.types().contains(MobType.INFERNAL) ? at(SMOLDERING, level) : 0;
            case "dragon_hunter" -> target.types().contains(MobType.AIRBORNE) ? at(SMOLDERING, level) : 0;
            case "giant_killer" -> giantKiller(level, attacker.health(), target.health());
            case "prosecute" -> target.maxHealth() <= 0 ? 0 : at(PROSECUTE, level) * Math.max(0, target.health()) / target.maxHealth() * 100;
            case "execute" -> target.maxHealth() <= 0 ? 0
                    : at(EXECUTE, level) * (target.maxHealth() - Math.max(0, target.health())) / target.maxHealth() * 100;
            case "titan_killer" -> Math.min(at(TITAN_KILLER, level) * Math.floor(Math.max(0, target.defense()) / 100), at(TITAN_KILLER_CAP, level));
            case "first_strike" -> melee && target.hitsTaken() == 0 ? at(FIRST_STRIKE, level) : 0;
            case "triple_strike" -> melee && target.hitsTaken() < 3 ? at(TRIPLE_STRIKE, level) : 0;
            case "power" -> melee ? 0 : at(POWER, level);
            case "snipe" -> melee ? 0 : attacker.travelled() * at(SNIPE, level) / 10;
            case "one_for_all" -> ONE_FOR_ALL;
            default -> 0;
        };
    }

    /** 0 if the target has less health than you; else k per percent it has more, up to the cap. */
    private static double giantKiller(int level, double yours, double theirs) {
        if (yours <= 0 || theirs <= yours) return 0;
        return Math.min(at(GIANT_KILLER, level) * (theirs - yours) / (0.01 * yours), at(GIANT_KILLER_CAP, level));
    }

    /** A level's value (the highest level's past the table, 0 below level 1). */
    private static double at(double[] values, int level) {
        return level < 1 ? 0 : values[Math.min(level, values.length) - 1];
    }

    /**
     * The attacker with more buffs on a hit on this target: {@code additive} more percent in the additive
     * sum (the Tuxedo's "Deal +50% damage!") and {@code multiplier} more in the product (1 for none). The
     * additive part goes in as the factor it makes on that sum, (100 + sum + additive) / (100 + sum), so
     * the formula above stays as it is; the same attacker if there's nothing more.
     */
    public static Attacker buffed(Attacker a, Target target, double additive, double multiplier) {
        if (additive == 0 && multiplier == 1) return a;
        double sum = 100 + additive(a, target);
        double factor = multiplier * (sum > 0 ? (sum + additive) / sum : 1);
        return new Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(), a.health(), a.enchantments(),
                a.ranged(), a.travelled(), a.multiplier() * factor);
    }

    /** What the hit does to the target, before rounding. */
    public static double exact(Attacker attacker, Target target, boolean critical) {
        double damage = initial(attacker.damage(), attacker.strength());
        if (critical) damage *= critMultiplier(attacker.critDamage());
        damage *= 1 + additive(attacker, target) / 100;
        damage *= attacker.multiplier();
        damage = cap(damage, target.caps());
        return damage * defenseMultiplier(target.defense());
    }

    /** What the hit does to the target: rounded down, as Hypixel's damage is. */
    public static double hit(Attacker attacker, Target target, boolean critical) {
        return Math.floor(exact(attacker, target, critical));
    }

    /**
     * An ability's damage before any buff: its base damage x (1 + Intelligence / 100 x its scaling) x
     * (1 + Ability Damage / 100) (the wiki's Damage Calculation, "Ability Damage"; the Giant's Slam is base
     * 100,000 with scaling 0.05, for one). Abilities don't crit.
     */
    public static double initialAbility(double baseDamage, double scaling, double intelligence, double abilityDamage) {
        return baseDamage * (1 + intelligence / 100 * scaling) * (1 + abilityDamage / 100);
    }

    /**
     * What magic damage (an ability's) does to the target, before rounding: times the additive and
     * multiplicative buffs (a few abilities skip the additive ones: pass 0), less the target's magic
     * resistance, then its caps and its Defense, in that order (the wiki's Scarf example).
     */
    public static double exactMagic(double initial, double additive, double multiplier, Target target) {
        double damage = initial * (1 + additive / 100) * multiplier;
        damage *= 1 - Math.max(0, Math.min(target.magicResistance(), 1));
        damage = cap(damage, target.caps());
        return damage * defenseMultiplier(target.defense());
    }

    /** A hit on a player, less their Defense (True Defense for true damage). */
    public static double taken(double damage, double defense) {
        return Math.max(0, damage) * defenseMultiplier(defense);
    }

    /**
     * Health regenerated each second: 1% of max health and 1.5, times Health Regen / 100 (the stats
     * menu: "Base regen ticks: 1% of Max ❤ + 1.5❤", "Regen interval: Every 1 seconds").
     */
    public static double healthRegen(double maxHealth, double healthRegen) {
        return (maxHealth / 100 + 1.5) * healthRegen / 100;
    }

    /**
     * Mana regenerated each second: 2% of the pool, rounded up (+26 at 1,264 in the recordings; whether
     * it's rounded up or rounded down plus one is UNKNOWN: no recorded pool made a whole 2%).
     */
    public static int manaRegen(int pool) {
        return manaRegen(pool, 0);
    }

    /**
     * With {@code bonus} more of the base, as a share: a Power Orb's "Grants +50% base mana regen" is 0.5 (3% of
     * the pool a second, rounded up as the base is).
     */
    public static int manaRegen(int pool, double bonus) {
        return (int) Math.ceil(pool * 0.02 * (1 + Math.max(0, bonus)) - 1e-9);
    }

    /** In dungeons each melee or arrow hit restores 5 mana and 1% of the pool. */
    public static double manaOnHit(int pool) {
        return 5 + pool * 0.01;
    }

    /**
     * How many ticks a mob can't be hurt again after a hit, with this much Attack Speed (at most 100):
     * 10 / (1 + Attack Speed / 100), rounded.
     */
    public static int invulnerabilityTicks(double attackSpeed) {
        return invulnerabilityTicks(attackSpeed, ATTACK_SPEED_CAP);
    }

    /** The same with the cap raised to {@code cap} (Newton's Demise: 150; see {@code Combat#attackSpeedCap}). */
    public static int invulnerabilityTicks(double attackSpeed, double cap) {
        return (int) Math.round(10 / (1 + attackSpeedShare(attackSpeed, cap)));
    }

    /**
     * How many ticks a shortbow with this shot cooldown (in seconds) waits between shots, with this much
     * Attack Speed (at most 100): its ticks / (1 + Attack Speed / 100), rounded up, so a 0.5 s one fires
     * every 10 ticks to 11 Attack Speed, 9 from 12 and 5 only at 100 (the wiki's Attack Speed). The wiki
     * gives it for 10 ticks; that other cooldowns shorten the same way is an approximation.
     */
    public static int shotCooldownTicks(double seconds, double attackSpeed) {
        return shotCooldownTicks(seconds, attackSpeed, ATTACK_SPEED_CAP);
    }

    /** The same with the cap raised to {@code cap}. */
    public static int shotCooldownTicks(double seconds, double attackSpeed, double cap) {
        // Less a hair, so a whole number of ticks that doubles don't quite hit exactly isn't rounded up past it.
        return (int) Math.ceil(Math.max(0, seconds) * 20 / (1 + attackSpeedShare(attackSpeed, cap)) - 1e-9);
    }

    /** Attack Speed as a share (0.25 for 25), between 0 and its cap (100, unless something raises it). */
    static double attackSpeedShare(double attackSpeed, double cap) {
        return Math.max(0, Math.min(attackSpeed, Math.max(ATTACK_SPEED_CAP, cap))) / 100;
    }
}
