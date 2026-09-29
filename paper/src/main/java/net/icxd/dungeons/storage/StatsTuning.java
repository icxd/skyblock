package net.icxd.dungeons.storage;

import java.util.List;

import org.bson.Document;
import org.bukkit.Material;

import net.icxd.dungeons.stats.Stat;
import net.icxd.dungeons.stats.Stats;

/**
 * Stats Tuning (the wiki's Maxwell: "Every 10 Accessory Power grants 1 Tuning Point. Tuning Points are used to tune a
 * specific stat to increase it"): the points a profile has put in each of the eight stats that can be tuned, kept on
 * the profile under {@link #FIELD} by stat, and the stats they give, which the Accessory Bag adds to theirs (see
 * AccessoryBag). What a point gives is the wiki's Stats Tuning menu's "Per point" (Maxwell/UI), Attack Speed's +0.3 as
 * its newer tab, NEU's and Skyblocker's profile viewers have it (its older tab says +0.2). If they have fewer points
 * than they've put in (their Accessory Power went down), only as many count, in the menu's order (UNKNOWN what Hypixel
 * does). The tuning templates aren't here yet. Main thread.
 */
public final class StatsTuning {
    /** The profile's points, by stat name. */
    public static final String FIELD = "statsTuning";

    /** A stat that can be tuned: what a point gives, and how the menu shows it (the wiki's Maxwell/UI, its newer tab). */
    enum Tuned {
        HEALTH(Stat.HEALTH, 5, 19, Material.GOLDEN_APPLE, "&7Your Health stat increases your", "&7maximum health."),
        DEFENSE(Stat.DEFENSE, 1, 20, Material.IRON_CHESTPLATE, "&7Your Defense stat reduces the", "&7damage that you take from enemies."),
        SPEED(Stat.SPEED, 1.5, 21, Material.SUGAR, "&7Your Speed stat increases how fast", "&7you can walk."),
        STRENGTH(Stat.STRENGTH, 1, 22, Material.BLAZE_POWDER, "&7Strength increases the damage you", "&7deal."),
        // A skull on Hypixel (the wiki's Crit Damage and Crit Chance: "Player Skull"); its skin is UNKNOWN, so a plain one.
        CRIT_DAMAGE(Stat.CRIT_DAMAGE, 1, 28, Material.PLAYER_HEAD, "&7Critical Damage multiplies the damage", "&7that you deal when you land a",
                "&7Critical Hit."),
        CRIT_CHANCE(Stat.CRIT_CHANCE, 0.2, 29, Material.PLAYER_HEAD, "&7Critical Chance is the percent", "&7chance that you land a Critical Hit",
                "&7when damaging an enemy."),
        ATTACK_SPEED(Stat.ATTACK_SPEED, 0.3, 30, Material.GOLDEN_AXE, "&7Attack Speed decreases the time", "&7between hits on your opponent."),
        INTELLIGENCE(Stat.INTELLIGENCE, 2, 31, Material.ENCHANTED_BOOK, "&7Intelligence increases the damage of", "&7your magical items and your mana",
                "&7pool.");

        final Stat stat;
        final double perPoint;
        final int slot;
        final Material material;
        final List<String> description;

        Tuned(Stat stat, double perPoint, int slot, Material material, String... description) {
            this.stat = stat;
            this.perPoint = perPoint;
            this.slot = slot;
            this.material = material;
            this.description = List.of(description);
        }
    }

    private StatsTuning() {
    }

    /** The points the profile has put in a stat. */
    static int points(Document profile, Tuned tuned) {
        Document tuning = profile == null ? null : profile.get(FIELD, Document.class);
        return tuning != null && tuning.get(tuned.name()) instanceof Number n ? Math.max(0, n.intValue()) : 0;
    }

    /** Puts {@code points} in a stat (none takes it out). */
    static void set(Document profile, Tuned tuned, int points) {
        Document tuning = profile.get(FIELD, Document.class);
        if (tuning == null) {
            tuning = new Document();
            profile.put(FIELD, tuning);
        }
        if (points <= 0) tuning.remove(tuned.name());
        else tuning.put(tuned.name(), points);
    }

    /** All the points the profile has put in. */
    static int assigned(Document profile) {
        int assigned = 0;
        for (Tuned tuned : Tuned.values()) assigned += points(profile, tuned);
        return assigned;
    }

    /** The stats the profile's tuning gives with this many Tuning Points: what each point gives, for as many as they have. */
    public static Stats stats(Document profile, int tuningPoints) {
        Stats stats = new Stats();
        int left = Math.max(0, tuningPoints);
        for (Tuned tuned : Tuned.values()) {
            int counted = Math.min(points(profile, tuned), left);
            left -= counted;
            if (counted > 0) stats.add(tuned.stat, counted * tuned.perPoint);
        }
        return stats;
    }

    /**
     * How many points a click puts in (more than 0) or takes out (less): {@code wanted} (1, or 10 with shift), at most
     * the points left to put in, and at most the points the stat has to take out.
     */
    static int change(int has, int left, int wanted) {
        return wanted >= 0 ? Math.min(wanted, Math.max(0, left)) : -Math.min(-wanted, has);
    }
}
