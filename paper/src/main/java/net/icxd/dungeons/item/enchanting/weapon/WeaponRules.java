package net.icxd.dungeons.item.enchanting.weapon;

import net.icxd.dungeons.combat.Damage;

import java.util.function.IntToDoubleFunction;

/**
 * The weapon enchantments' rules as sums, with no Bukkit in them (WeaponEnchants does what they say): damages over
 * time as the wiki's Damage Calculation works them out, the stacks that grow with hits and kills, tiers that grow
 * with use, and the like. The numbers come in from the enchantments' texts (see {@link EnchantText}).
 */
public final class WeaponRules {
    private static final double PERCENT = 100;

    private WeaponRules() {
    }

    /**
     * A hit's damage once its additive buffs are in, before its multiplicative ones, its caps and the target's
     * Defense: "Damage_post-additive" = initial x crit x Additive Multiplier, which Fire Aspect's and Venomous's
     * damage over time take their share of (the wiki's Damage Calculation, example 4).
     */
    public static double postAdditive(Damage.Attacker attacker, Damage.Target target, boolean critical) {
        double damage = Damage.initial(attacker.damage(), attacker.strength());
        if (critical) damage *= Damage.critMultiplier(attacker.critDamage());
        return damage * (1 + Damage.additive(attacker, target) / PERCENT);
    }

    /**
     * One second of fire (Fire Aspect's "dealing 3% of your damage per second", Flame's): {@code percent} of the hit's
     * damage after additive buffs, "with no second additive multiplier" (the wiki), times what the target takes from
     * fire (Duplex's mark, 1 without), then through its Defense (UNKNOWN: the wiki's examples are on a Training Dummy;
     * its Toxic Arrow Poison example takes Defense off a damage over time).
     */
    public static double fireTick(double postAdditive, double percent, double fireTaken, double defense) {
        return Math.max(0, postAdditive) * percent / PERCENT * Math.max(0, fireTaken) * Damage.defenseMultiplier(defense);
    }

    /**
     * One second of Venomous's poison: what its stacks saved (each hit's damage after additive buffs times its
     * share), times the Additive Multiplier worked out again without melee-only buffs ("Venomous damage is not melee
     * damage so Sharpness is not added"), then through the target's Defense (UNKNOWN, as for fire).
     */
    public static double venomTick(double saved, double secondAdditive, double defense) {
        return Math.max(0, saved) * (1 + secondAdditive / PERCENT) * Damage.defenseMultiplier(defense);
    }

    /**
     * The attacker for Venomous's second Additive Multiplier: as {@code a}, but not a melee hit, so Sharpness, First
     * Strike and Triple-Strike (the melee-only ones, see Damage#enchantment) add nothing; a sword's enchantments have
     * no bow-only one to count instead.
     */
    public static Damage.Attacker notMelee(Damage.Attacker a) {
        return new Damage.Attacker(a.damage(), a.strength(), a.critChance(), a.critDamage(), a.combatLevel(), a.health(), a.enchantments(),
                true, 0, a.multiplier());
    }

    /**
     * Venomous on a mob: its stacks from every player's hits ("stacking globally up to 40 hits") and what they saved
     * together, for as long as the last hit's "Lasts 5s". A hit past the most stacks refreshes it and saves no more
     * ("The total saved Venomous damage cannot be increased further once 41 stacks have been saved": the text's 40).
     */
    public static final class Venom {
        int stacks;
        double saved;
        long until;

        /** A hit with this much damage after additive buffs and this share per stack (0.002 for Venomous I), at {@code now}. */
        public void hit(double postAdditive, double share, int maxStacks, long lastsMillis, long now) {
            if (now >= until) {
                stacks = 0;
                saved = 0;
            }
            if (stacks < maxStacks) {
                stacks++;
                saved += Math.max(0, postAdditive) * share;
            }
            until = now + lastsMillis;
        }

        public int stacks() {
            return stacks;
        }

        public double saved() {
            return saved;
        }
    }

    /**
     * Fatal Tempo's boost after a hit: {@code perHit} percent more Ferocity for each hit, up to {@code cap}, counted
     * again from nothing once {@code windowMillis} have gone by since the one before ("for 3 seconds after your last
     * attack").
     */
    public static double tempo(double percent, long lastMillis, long now, double perHit, double cap, long windowMillis) {
        double before = now - lastMillis <= windowMillis ? percent : 0;
        return Math.min(cap, before + perHit);
    }

    /** Ferocity with Fatal Tempo's boost ({@code percent} more). */
    public static double withTempo(double ferocity, double percent) {
        return ferocity * (1 + Math.max(0, percent) / PERCENT);
    }

    /**
     * Combo's additive buff: {@code perKill} percent for each of their kills within the last {@code windowMillis}, up
     * to {@code most} kills ("+1% per kill up to 2 kills within 2s"; the wiki's k x min(kills, n)). Each kill counts
     * for the window on its own (UNKNOWN whether a kill starts the window again for the ones before). {@code kills} are
     * the kills' times, any order; 0 for no time.
     */
    public static double combo(long[] kills, long now, double perKill, int most, long windowMillis) {
        int within = 0;
        for (long kill : kills) if (kill > 0 && now - kill <= windowMillis) within++;
        return perKill * Math.min(within, Math.max(0, most));
    }

    /** Swarm's additive buff: {@code perEnemy} percent for each enemy near, up to {@code most} ("Maximum of 10 enemies"). */
    public static double swarm(int enemies, double perEnemy, int most) {
        return perEnemy * Math.min(Math.max(0, enemies), Math.max(0, most));
    }

    /**
     * What Soul Eater stores from a kill: its multiplier times the mob's Damage ("gains 2x the Damage of the latest
     * monster killed"), at most {@code cap} outside a dungeon ("Max 1M outside Dungeons"; no cap in one).
     */
    public static double soul(double multiplier, double mobDamage, double cap, boolean inDungeon) {
        double soul = Math.max(0, multiplier) * Math.max(0, mobDamage);
        return inDungeon || cap <= 0 ? soul : Math.min(soul, cap);
    }

    /**
     * Whether a hit that carries {@code carried} soul (its weapon as the hit knows it: the bow as the arrow left) may add
     * it: only while what they hold has Soul Eater and that same soul, which the hit then takes. So one soul is added
     * once, not by each of a shortbow's arrows (or every arrow still in flight), and a soul a kill stored since the
     * arrow left is the next hit's.
     */
    public static boolean soulToAdd(double carried, boolean heldHasSoulEater, double heldSoul) {
        return carried > 0 && heldHasSoulEater && heldSoul == carried;
    }

    /**
     * A tiered enchantment's level once its count has grown (Champion's Combat XP, Toxophilite's): up one for each
     * tier whose "&850k Combat XP to tier up!" the count has reached, several at once if it has ("Fixed Champion not
     * leveling multiple times when gaining enough Combat XP for multiple levels", the wiki). {@code thresholds} gives a
     * level's count to reach to go up (0 at the last).
     */
    public static int tier(int level, double count, IntToDoubleFunction thresholds) {
        int at = Math.max(1, level);
        for (double next = thresholds.applyAsDouble(at); next > 0 && count >= next; next = thresholds.applyAsDouble(at)) at++;
        return at;
    }

    /** Whether this is the {@code every}th hit ({@code hits} counts it: 3, 6, ... for every 3rd); never for 0 or less. */
    public static boolean every(int hits, int every) {
        return every > 0 && hits > 0 && hits % every == 0;
    }

    /**
     * Knockback's and Punch's extra push, as vanilla's Knockback and Punch give it a level (the melee one's 0.5 and the
     * arrow's 0.6 more strength: vanilla's own enchantments, whose "about 3 blocks" a level the books' "Increases
     * knockback by 3 blocks" reads as); {@code blocks} is the text's (3 a level). UNKNOWN how Hypixel turns blocks
     * into a push.
     */
    public static double knockback(double blocks, boolean arrow) {
        return Math.max(0, blocks) / 3 * (arrow ? 0.6 : 0.5);
    }

    /**
     * Whether an arrow at height {@code y} hit a mob's head (Precise's and Headstrong's "when arrows hit the head of a
     * mob"): at or above its eyes less as far again as its head goes above them (a zombie's top 0.42 of its 1.95).
     * UNKNOWN where Hypixel puts a mob's head.
     */
    public static boolean headshot(double y, double feetY, double height, double eyeHeight) {
        double above = Math.max(0.1, height - eyeHeight);
        return y >= feetY + eyeHeight - above;
    }
}
