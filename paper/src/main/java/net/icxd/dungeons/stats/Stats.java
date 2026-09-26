package net.icxd.dungeons.stats;

import java.util.Arrays;
import java.util.StringJoiner;

import static net.icxd.dungeons.stats.Stat.CRIT_CHANCE;
import static net.icxd.dungeons.stats.Stat.CRIT_DAMAGE;
import static net.icxd.dungeons.stats.Stat.HEALTH;
import static net.icxd.dungeons.stats.Stat.HEALTH_REGEN;
import static net.icxd.dungeons.stats.Stat.INTELLIGENCE;
import static net.icxd.dungeons.stats.Stat.MENDING;
import static net.icxd.dungeons.stats.Stat.SEA_CREATURE_CHANCE;
import static net.icxd.dungeons.stats.Stat.SPEED;
import static net.icxd.dungeons.stats.Stat.VITALITY;

/** A value for each {@link Stat}, all 0 to start. Mutable; the changes return it, for chaining. */
public final class Stats {
    private final double[] values = new double[Stat.all().length];

    /**
     * Everyone's base: 100 health, intelligence, speed, vitality, mending and health regen, 30% crit
     * chance, 50% crit damage and 20% sea creature chance.
     */
    public static Stats base() {
        return new Stats().set(HEALTH, 100).set(INTELLIGENCE, 100).set(SPEED, 100).set(CRIT_CHANCE, 30)
                .set(CRIT_DAMAGE, 50).set(SEA_CREATURE_CHANCE, 20).set(VITALITY, 100).set(MENDING, 100)
                .set(HEALTH_REGEN, 100);
    }

    public double get(Stat stat) {
        return values[stat.ordinal()];
    }

    public boolean has(Stat stat) {
        return values[stat.ordinal()] != 0;
    }

    public Stats set(Stat stat, double value) {
        values[stat.ordinal()] = value;
        return this;
    }

    public Stats add(Stat stat, double value) {
        values[stat.ordinal()] += value;
        return this;
    }

    public Stats add(Stats other) {
        for (int i = 0; i < values.length; i++) values[i] += other.values[i];
        return this;
    }

    public Stats copy() {
        Stats copy = new Stats();
        System.arraycopy(values, 0, copy.values, 0, values.length);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stats other && Arrays.equals(values, other.values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    /** The stats that aren't 0, e.g. "Stats[DAMAGE=120, STRENGTH=100]". */
    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "Stats[", "]");
        for (Stat stat : Stat.all()) if (has(stat)) joiner.add(stat + "=" + get(stat));
        return joiner.toString();
    }
}
