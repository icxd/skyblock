package net.icxd.dungeons.item.ability.weapons;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.icxd.dungeons.item.ability.utility.AbilityText;
import net.icxd.dungeons.mob.MobType;

/**
 * What a weapon's own text says it does beyond its stats, read from that text (items.json's, which is
 * Hypixel's): "Deals +100% damage to ༕ Undead mobs.", "Deal 1.5x damage to ♨ Infernal mobs.", "Deals +2% damage
 * to ༕ Undead Mobs for every 1% of your missing health.", "Deals +200% damage while in water.", "All damage dealt
 * with this bow is doubled.", "Gain +100❁ Strength against enemies who are in lava.", "Heal 25❤ per hit.",
 * "Regens 3 Mana on hit.", "Gain +45☯ Combat Wisdom against ༕ Undead mobs.", "Receive -20% damage from ☮ Animal
 * mobs.", "Your Critical Hits deal 100% more damage if you are behind your target.", "Reduces the defense of your
 * target by 10% of their max ❈ Defense on hit, stacking up to 5 times.", "Your arrows have a 50% chance to bounce
 * to another target after it hits something.", "Fires a volley of 5 arrows. Arrows apply venom to all enemies hit
 * dealing 2❁ Damage every second for 3 seconds.". A weapon's text is read once (see {@link WeaponPassives}), so a
 * hit reads none. Every damage one is a multiplicative multiplier: the wiki's Damage Calculation/Multiplicative
 * Sources lists them as such ("Deal +100% damage to Arthropod mobs" is x2, "Deals +2% more damage ... per 1%
 * Health the player is missing" is 1 + 2 x the share missing). No Bukkit here.
 */
final class WeaponLore {
    static final WeaponLore NONE = new WeaponLore();

    /** "Deals +50% damage to ☠ Wither mobs", "Deal +300% ❁ Damage to and gain +45☯ Combat Wisdom against Ж Arthropod mobs". */
    private static final Pattern PERCENT_TO = Pattern.compile(
            "Deals? \\+([\\d.]+)% (?:❁ )?[Dd]amage to (?:and gain \\+[\\d.]+☯ Combat Wisdom against )?\\S+ (\\p{L}+) [Mm]obs(?! for every)");
    /** "Deal 2.5x damage to ♨ Infernal mobs". */
    private static final Pattern TIMES_TO = Pattern.compile("Deals? ([\\d.]+)x damage to \\S+ (\\p{L}+) [Mm]obs");
    /** "Deals +2% damage to ༕ Undead Mobs for every 1% of your missing health". */
    private static final Pattern MISSING = Pattern.compile(
            "Deals? \\+([\\d.]+)% (?:more )?damage to \\S+ (\\p{L}+) [Mm]obs for every ([\\d.]+)% of your missing health");
    private static final Pattern IN_WATER = Pattern.compile("Deals? \\+([\\d.]+)% damage while in water");
    private static final Pattern DOUBLED = Pattern.compile("All damage dealt with this (?:bow|weapon|sword) is doubled");
    private static final Pattern LAVA = Pattern.compile("Gain \\+([\\d.]+)❁ Strength against enemies who are in lava");
    private static final Pattern HEAL = Pattern.compile("Heal ([\\d.]+)❤ per hit");
    private static final Pattern MANA = Pattern.compile("Regens ([\\d.]+) Mana(?: and [\\d.]+❤)? on hit");
    /** "Gain +45☯ Combat Wisdom against ༕ Undead mobs", "and gain +30☯ Combat Wisdom against Ж Arthropod mobs". */
    private static final Pattern WISDOM = Pattern.compile("[Gg]ain \\+([\\d.]+)☯ Combat Wisdom against \\S+ (\\p{L}+) [Mm]obs");
    /** "Receive -20% damage from ☮ Animal mobs", "Take -20% damage from ☮ Animal mobs". */
    private static final Pattern ANIMAL = Pattern.compile("(?:Receive|Take) -([\\d.]+)% damage from \\S+ Animal mobs");
    private static final Pattern BEHIND = Pattern.compile("Your Critical Hits deal ([\\d.]+)% more damage if you are behind your target");
    private static final Pattern SHRED = Pattern.compile(
            "Reduces the defense of your target by ([\\d.]+)% of their max ❈ Defense on hit, stacking up to (\\d+) times");
    private static final Pattern BOUNCE = Pattern.compile("Your arrows have a ([\\d.]+)% chance to bounce to another target");
    private static final Pattern VOLLEY = Pattern.compile("Fires a volley of (\\d+) arrows");
    private static final Pattern VENOM = Pattern.compile("apply venom to all enemies hit dealing ([\\d.]+)❁ Damage every second for (\\d+) seconds");

    /** The factor on hits on each mob type ("+100%" is 2, "1.5x" 1.5). */
    final Map<MobType, Double> types = new EnumMap<>(MobType.class);
    /** The mob type the missing-health bonus is against (null for none), and its share a whole share of health missing (2 for "+2% ... for every 1%"). */
    MobType missingType;
    double perMissing;
    /** The factor while the holder is in water. */
    double inWater = 1;
    /** The factor on every hit ("All damage dealt with this bow is doubled": 2). */
    double always = 1;
    /** Strength its hits have on a mob in lava. */
    double strengthInLava;
    /** Health a melee hit heals, mana it gives back. */
    double healPerHit;
    double manaPerHit;
    /** Combat Wisdom on a kill of each mob type. */
    final Map<MobType, Double> wisdom = new EnumMap<>(MobType.class);
    /** The factor on hits from Animal mobs while it's held ("-20%": 0.8). */
    double animalTaken = 1;
    /** The factor on a crit from behind the target ("100% more": 2). */
    double behindCrit = 1;
    /** Its arrows' Defense shred: the share of max Defense a stack, and how many stacks at most. */
    double shredShare;
    int shredStacks;
    /** Its arrows' chance to bounce (0.5 for 50%). */
    double bounceChance;
    /** How many arrows a shot is (1 for one), and the venom they put on: damage a second, for how many seconds. */
    int volley = 1;
    double venomDamage;
    int venomSeconds;
    private boolean any;

    private WeaponLore() {
    }

    /** What this text (an item's lore, its lines as they are) says. */
    static WeaponLore of(List<String> lore) {
        if (lore == null || lore.isEmpty()) return NONE;
        String plain = AbilityText.plain(lore);
        WeaponLore read = new WeaponLore();
        Matcher m = PERCENT_TO.matcher(plain);
        while (m.find()) put(read.types, m.group(2), 1 + number(m.group(1)) / 100);
        m = TIMES_TO.matcher(plain);
        while (m.find()) put(read.types, m.group(2), number(m.group(1)));
        m = MISSING.matcher(plain);
        if (m.find() && (read.missingType = type(m.group(2))) != null) read.perMissing = number(m.group(1)) / number(m.group(3));
        m = WISDOM.matcher(plain);
        while (m.find()) put(read.wisdom, m.group(2), number(m.group(1)));
        read.inWater = 1 + first(IN_WATER, plain) / 100;
        read.always = DOUBLED.matcher(plain).find() ? 2 : 1;
        read.strengthInLava = first(LAVA, plain);
        read.healPerHit = first(HEAL, plain);
        read.manaPerHit = first(MANA, plain);
        read.animalTaken = 1 - first(ANIMAL, plain) / 100;
        read.behindCrit = 1 + first(BEHIND, plain) / 100;
        m = SHRED.matcher(plain);
        if (m.find()) {
            read.shredShare = number(m.group(1)) / 100;
            read.shredStacks = Integer.parseInt(m.group(2));
        }
        read.bounceChance = first(BOUNCE, plain) / 100;
        read.volley = (int) Math.max(1, first(VOLLEY, plain));
        m = VENOM.matcher(plain);
        if (m.find()) {
            read.venomDamage = number(m.group(1));
            read.venomSeconds = Integer.parseInt(m.group(2));
        }
        read.any = !read.types.isEmpty() || read.missingType != null || read.inWater != 1 || read.always != 1 || read.strengthInLava > 0
                || read.healPerHit > 0 || read.manaPerHit > 0 || !read.wisdom.isEmpty() || read.animalTaken != 1 || read.behindCrit != 1
                || read.shredStacks > 0 || read.bounceChance > 0 || read.volley > 1 || read.venomSeconds > 0;
        return read.any ? read : NONE;
    }

    /** Whether it says anything that does something. */
    boolean any() {
        return any;
    }

    /** Whether anything of it changes a hit's damage (a factor, Strength). */
    boolean onHit() {
        return !types.isEmpty() || missingType != null || inWater != 1 || always != 1 || strengthInLava > 0 || behindCrit != 1;
    }

    /**
     * The factor on a hit on a mob of these types, by a holder who's missing {@code missing} of their health (0
     * to 1) and is ({@code inWater}) or isn't in water. A mob of several types takes the largest of the factors
     * for them (UNKNOWN whether they'd multiply: no weapon names two types one mob has).
     */
    double factor(Set<MobType> on, double missing, boolean holderInWater) {
        double best = 1;
        for (MobType type : on) best = Math.max(best, types.getOrDefault(type, 1.0));
        double factor = always * best;
        if (missingType != null && on.contains(missingType)) factor *= missingFactor(perMissing, missing);
        if (holderInWater) factor *= inWater;
        return factor;
    }

    /** 1 + {@code perMissing} x the share of health missing (0 to 1): "+2% ... for every 1% of your missing health". */
    static double missingFactor(double perMissing, double missing) {
        return 1 + perMissing * Math.max(0, Math.min(1, missing));
    }

    /**
     * What {@code extra} Strength does to a hit of someone with {@code strength}: the factor on the hit's
     * (5 + Damage) x (1 + Strength / 100).
     */
    static double strengthFactor(double strength, double extra) {
        double base = 1 + strength / 100;
        return base <= 0 ? 1 : (base + extra / 100) / base;
    }

    /** Combat Wisdom on a kill of a mob of these types: the most of those it names (0 for none). */
    double wisdomAgainst(Set<MobType> on) {
        double most = 0;
        for (MobType type : on) most = Math.max(most, wisdom.getOrDefault(type, 0.0));
        return most;
    }

    private static void put(Map<MobType, Double> map, String name, double value) {
        MobType type = type(name);
        if (type != null) map.merge(type, value, Math::max);
    }

    /** The mob type with this name ("Undead"); null for a name that isn't one ("Sea", "Endermen"). */
    static MobType type(String name) {
        try {
            return MobType.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static double first(Pattern pattern, String plain) {
        Matcher m = pattern.matcher(plain);
        return m.find() ? number(m.group(1)) : 0;
    }

    private static double number(String digits) {
        return Double.parseDouble(digits.replace(",", ""));
    }
}
