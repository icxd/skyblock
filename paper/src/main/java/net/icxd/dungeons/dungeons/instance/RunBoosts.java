package net.icxd.dungeons.dungeons.instance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

import net.icxd.dungeons.common.DungeonFloor;
import net.icxd.dungeons.dungeons.DungeonClass;

/**
 * What a member's own gear does to the end of a run, from outside the run (EFFECTS.md): more of its experience
 * (Hecatomb's "Gain +1% Catacombs XP & +2% Class XP, doubled on S+ runs"), and what hears that a member got theirs
 * (Hecatomb's tiers, which go up with S runs). Main thread.
 */
public final class RunBoosts {
    /** Percent more Catacombs experience, and of every class's experience (their own and their team bonus). */
    public record Boost(double catacombs, double classes) {
        public static final Boost NONE = new Boost(0, 0);
    }

    /** A member's boost from a run with this chat score; null for none. */
    @FunctionalInterface
    public interface Source {
        Boost boost(Player player, Score score);
    }

    /** A member got a run's experience: a completed run's, or a failed one's. */
    @FunctionalInterface
    public interface Rewarded {
        void rewarded(Player player, DungeonFloor floor, Score score, boolean failed);
    }

    private static final List<Source> SOURCES = new ArrayList<>();
    private static final List<Rewarded> REWARDED = new ArrayList<>();

    private RunBoosts() {
    }

    /** Adds what boosts members' experience; the boosts add up. */
    public static void addBoost(Source source) {
        SOURCES.add(source);
    }

    /** Adds what hears of each member getting a run's experience. */
    public static void addRewardedListener(Rewarded listener) {
        REWARDED.add(listener);
    }

    /** A member's boosts from this run, added up. */
    static Boost of(Player player, Score score) {
        double catacombs = 0;
        double classes = 0;
        for (Source source : SOURCES) {
            Boost boost = source.boost(player, score);
            if (boost == null) continue;
            catacombs += boost.catacombs();
            classes += boost.classes();
        }
        return catacombs == 0 && classes == 0 ? Boost.NONE : new Boost(catacombs, classes);
    }

    static void rewarded(Player player, DungeonFloor floor, Score score, boolean failed) {
        for (Rewarded listener : REWARDED) listener.rewarded(player, floor, score, failed);
    }

    /**
     * The reward with a boost: its experience that many percent more, to a tenth as the chat shows it (UNKNOWN
     * whether Hypixel's boosts multiply the rest or add to the day's bonus: here they multiply). Bits don't change.
     */
    static RunRewards.Reward boosted(RunRewards.Reward reward, Boost boost) {
        if (boost == null || (boost.catacombs() == 0 && boost.classes() == 0)) return reward;
        Map<DungeonClass, Double> classes = new LinkedHashMap<>();
        for (Map.Entry<DungeonClass, Double> e : reward.classes().entrySet()) {
            classes.put(e.getKey(), RunRewards.round(e.getValue() * (1 + boost.classes() / 100)));
        }
        return new RunRewards.Reward(RunRewards.round(reward.catacombs() * (1 + boost.catacombs() / 100)), classes, reward.bits());
    }
}
