package net.icxd.dungeons.combat;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * One mob's debuffs and damages over time, as rules with no Bukkit in them ({@link MobDebuffs} keeps one of
 * these for each mob that has any). A debuff is its source's (one of each source on a mob: "lethality",
 * "frozen"); putting it on again adds a stack up to its most and starts its time again, and the stronger
 * value stays: "A debuff keeps the level it was applied with, and refreshing it never lowers it" (the
 * wiki's Damage Calculation, as Fire Aspect's example has it). A damage over time is its source's too, ticks
 * every so many ticks for so many times, and the same rule holds for it: putting it on again starts its
 * count again ("will refresh the timer of the 9,000 damage DoT, without decreasing it") and keeps its beat.
 * Times are the caller's milliseconds; ticks are counted by {@link #tick}.
 */
public final class Debuffs {
    /** What a debuff does. */
    public enum Kind {
        /** A share of its Defense taken off (Lethality: 0.012 a stack); they add up, at most all of it. */
        DEFENSE,
        /**
         * It takes more damage from players: a share more ("Frozen mobs take 10% increased damage": 0.1), times
         * its stacks; each source's is a factor and they multiply (UNKNOWN whether Hypixel adds them).
         */
        TAKEN,
        /** A share of its speed taken off (Venomous); they add up, at most all of it. */
        SLOW
    }

    /**
     * A debuff: its source (one of each on a mob), its kind, what one stack of it does (a share: 0.1 for 10%),
     * how many stacks it goes up to (1 for one that doesn't stack), and how long it lasts after it's last put on.
     */
    public record Spec(String source, Kind kind, double perStack, int maxStacks, long millis) {
        public Spec {
            maxStacks = Math.max(1, maxStacks);
        }
    }

    /** Where a damage over time's ticks go: {@code by} dealt {@code damage} from {@code source}. */
    @FunctionalInterface
    public interface DotSink {
        void deal(String source, UUID by, double damage, DamageIndicators.Look look);
    }

    private static final class Debuff {
        final Kind kind;
        double perStack;
        int stacks;
        long until;
        UUID by;

        Debuff(Kind kind) {
            this.kind = kind;
        }
    }

    private static final class Dot {
        UUID by;
        double damage;
        int every;
        int left;
        int untilNext;
        DamageIndicators.Look look;
    }

    private final Map<String, Debuff> debuffs = new HashMap<>();
    private final Map<String, Dot> dots = new HashMap<>();

    /** Puts it on (again), by {@code by} (null for nobody's), at {@code now}. */
    public void add(Spec spec, UUID by, long now) {
        Debuff debuff = debuffs.get(spec.source());
        if (debuff == null || debuff.until <= now || debuff.kind != spec.kind()) {
            debuff = new Debuff(spec.kind());
            debuffs.put(spec.source(), debuff);
        }
        debuff.stacks = Math.min(spec.maxStacks(), debuff.stacks + 1);
        if (Math.abs(spec.perStack()) >= Math.abs(debuff.perStack)) {
            debuff.perStack = spec.perStack();
            debuff.by = by;
        }
        debuff.until = now + Math.max(0, spec.millis());
    }

    /** Its stacks of this source's debuff at {@code now}; 0 for none. */
    public int stacks(String source, long now) {
        Debuff debuff = debuffs.get(source);
        return debuff == null || debuff.until <= now ? 0 : debuff.stacks;
    }

    /** Who put this source's debuff on last with the value it has (null for nobody, or none on). */
    public UUID by(String source, long now) {
        Debuff debuff = debuffs.get(source);
        return debuff == null || debuff.until <= now ? null : debuff.by;
    }

    /** Takes this source's debuff off, and its damage over time. */
    public void remove(String source) {
        debuffs.remove(source);
        dots.remove(source);
    }

    /** What its debuffs of this kind come to at {@code now}: the share they take off (DEFENSE, SLOW), or the factor on what it takes (TAKEN). */
    public double of(Kind kind, long now) {
        if (kind == Kind.TAKEN) {
            double factor = 1;
            for (Debuff debuff : debuffs.values()) {
                if (debuff.kind == Kind.TAKEN && debuff.until > now) factor *= Math.max(0, 1 + debuff.perStack * debuff.stacks);
            }
            return factor;
        }
        double share = 0;
        for (Debuff debuff : debuffs.values()) if (debuff.kind == kind && debuff.until > now) share += debuff.perStack * debuff.stacks;
        return Math.max(0, Math.min(1, share));
    }

    /** Its Defense with its debuffs: {@code defense} less the share they take off. */
    public double defense(double defense, long now) {
        return defense * (1 - of(Kind.DEFENSE, now));
    }

    /** The factor on the damage it takes from players (1 for none). */
    public double takenFactor(long now) {
        return of(Kind.TAKEN, now);
    }

    /**
     * Starts (or renews) a damage over time: {@code damage} from {@code by} every {@code every} ticks,
     * {@code times} times, the first {@code every} ticks from now, its number looking as {@code look}. Put on
     * again, it counts its times from now and keeps its beat and the greater damage (and whose that is).
     */
    public void dot(String source, UUID by, double damage, int every, int times, DamageIndicators.Look look) {
        if (times <= 0 || damage <= 0) return;
        Dot dot = dots.get(source);
        if (dot == null) {
            dot = new Dot();
            dot.untilNext = Math.max(1, every);
            dots.put(source, dot);
        }
        if (damage >= dot.damage) {
            dot.damage = damage;
            dot.by = by;
        }
        dot.every = Math.max(1, every);
        dot.left = times;
        dot.look = look;
    }

    /** Whether this source's damage over time is still going. */
    public boolean hasDot(String source) {
        return dots.containsKey(source);
    }

    /** A tick: each damage over time that's due deals its damage to {@code sink}, and those that are done end. */
    public void tick(DotSink sink) {
        for (Iterator<Map.Entry<String, Dot>> it = dots.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, Dot> entry = it.next();
            Dot dot = entry.getValue();
            if (--dot.untilNext > 0) continue;
            dot.untilNext = dot.every;
            sink.deal(entry.getKey(), dot.by, dot.damage, dot.look);
            if (--dot.left <= 0) it.remove();
        }
    }

    /** Takes off what has run out by {@code now}; whether nothing is left (no debuff, no damage over time). */
    public boolean expire(long now) {
        for (Iterator<Debuff> it = debuffs.values().iterator(); it.hasNext(); ) if (it.next().until <= now) it.remove();
        return debuffs.isEmpty() && dots.isEmpty();
    }
}
