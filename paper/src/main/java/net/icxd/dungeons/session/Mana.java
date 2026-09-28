package net.icxd.dungeons.session;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Mana being spent: every ability's cost is taken here ({@link #spend}), so what happens when mana is
 * used (Refrigerate's "3% of Mana used becomes Defense") hears of each, whatever took it. The pool and its
 * regeneration are the session's and StatsRunnable's. Main thread.
 */
public final class Mana {
    /** Something that happens when a player spends mana (see {@link #addSpentListener}). */
    @FunctionalInterface
    public interface Spent {
        /** {@code amount} was taken from them for {@code source} (an ability's name). */
        void spent(Player player, int amount, String source);
    }

    private static final List<Spent> SPENT = new ArrayList<>();

    private Mana() {
    }

    /** Adds something that happens whenever a player spends mana (more than none). */
    public static void addSpentListener(Spent listener) {
        SPENT.add(listener);
    }

    /** Their mana now: full until their pool is known. */
    public static int get(Player player) {
        PlayerSession session = PlayerSession.of(player);
        return session.getMana() < 0 ? session.maxMana() : session.getMana();
    }

    /**
     * Takes {@code cost} of their mana for {@code source}, never below none, and tells the listeners what was
     * taken; returns it. Whether they had enough is the caller's to ask first.
     */
    public static int spend(Player player, int cost, String source) {
        if (cost <= 0) return 0;
        int mana = get(player);
        int taken = taken(mana, cost);
        PlayerSession.of(player).setMana(Math.max(0, mana) - taken);
        if (taken > 0) for (Spent listener : SPENT) listener.spent(player, taken, source);
        return taken;
    }

    /** What a cost takes of this much mana: all of it, or all they have. */
    static int taken(int mana, int cost) {
        return Math.max(0, Math.min(Math.max(0, mana), cost));
    }
}
