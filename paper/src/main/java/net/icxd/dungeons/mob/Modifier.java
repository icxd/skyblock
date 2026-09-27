package net.icxd.dungeons.mob;

import java.util.Locale;
import java.util.random.RandomGenerator;

/**
 * The modifiers a dungeon mob can spawn with (the wiki's Mob Modifiers; research mobs.md 1.5): the
 * name goes before its name in its colour, its health in the second colour. What each does beyond
 * health and speed is in {@link DataMob} (Flaming, Healing, Stormy); minibosses and bosses get none.
 */
public enum Modifier {
    /** Runs faster: +0.12 movement speed on every recorded one. */
    SPEEDY("Speedy", '3', 'b', 18),
    /** More health: x1.6 on every recorded one. */
    HEALTHY("Healthy", '2', 'a', 11),
    /** Its melee hits set you on fire. */
    FLAMING("Flaming", '6', 'e', 9),
    /** "Mob will slowly regenerate health at all times." */
    HEALING("Healing", '2', 'a', 5),
    /**
     * "Higher Defense and knockback resistance": neither number is published. The one recorded hit on a
     * Fortified mob (✧21,222,242✧ on a Fortified Crypt Lurker) was as big as those on plain mobs just
     * before (~20M), so no Defense is added; it isn't knocked back at all (UNKNOWN how much less it is).
     */
    FORTIFIED("Fortified", '8', '7', 5),
    /** "Strikes lightning on nearby players, dealing True Damage." */
    STORMY("Stormy", '1', '9', 1);

    /** Of the 346 name tags recorded, 49 had a modifier. */
    private static final double CHANCE = 49 / 346.0;

    private final String displayName;
    private final char nameColor;
    private final char healthColor;
    /** How many of the 346 recorded name tags had it. */
    private final int seen;

    Modifier(String displayName, char nameColor, char healthColor, int seen) {
        this.displayName = displayName;
        this.nameColor = nameColor;
        this.healthColor = healthColor;
        this.seen = seen;
    }

    public String displayName() {
        return displayName;
    }

    public char nameColor() {
        return nameColor;
    }

    public char healthColor() {
        return healthColor;
    }

    /** What it multiplies health by. */
    public double healthMultiplier() {
        return this == HEALTHY ? 1.6 : 1;
    }

    /** What it adds to the movement speed attribute. */
    public double speedBonus() {
        return this == SPEEDY ? 0.12 : 0;
    }

    /** Players' hits don't knock it back. */
    public boolean knockbackImmune() {
        return this == FORTIFIED;
    }

    /** "speedy", "Speedy": the one named so; null for none. */
    public static Modifier parse(String name) {
        try {
            return name == null ? null : valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * A modifier for a new (non-miniboss) mob, or null for none: Hypixel's chances aren't published (the
     * wiki says "a small chance"), so these are how often each showed up in the two recorded runs.
     */
    public static Modifier roll(RandomGenerator random) {
        if (random.nextDouble() >= CHANCE) return null;
        int total = 0;
        for (Modifier modifier : values()) total += modifier.seen;
        int pick = random.nextInt(total);
        for (Modifier modifier : values()) {
            pick -= modifier.seen;
            if (pick < 0) return modifier;
        }
        return null;
    }
}
