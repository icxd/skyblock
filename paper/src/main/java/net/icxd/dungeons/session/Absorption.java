package net.icxd.dungeons.session;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A player's absorption: health on top of their health, "an extension of the player's health ... affected by
 * Defense. When damage is inflicted to Absorption, it is removed. Unlike health, Absorption will not
 * regenerate over time after taking damage" (the wiki's Absorption). Each source gives its own for a while
 * (Wither Shield's, Golem Armor's "+60 for 20 seconds after killing a mob", an Absorption Potion's "for 3
 * minutes"); giving it again replaces what that source gave (UNKNOWN whether Hypixel's stack: different
 * sources add up here). A hit takes it before their health, after their Defense and shields (see
 * PlayerDamage), the source that runs out soonest first (UNKNOWN); vanilla damage too (HealthListener). The
 * action bar shows it as Hypixel's does: their health and absorption together over their max, in gold
 * ("§66,171/4,422❤", as the SkyHanni mod's action bar patterns have it). Kept on their session (one per
 * player); a respawn clears it. The pool's rules take the time in milliseconds, so they're tested alone.
 */
public final class Absorption {
    private static final class Pool {
        double amount;
        long until;
    }

    private final Map<String, Pool> pools = new HashMap<>();

    /** {@code amount} from {@code source} until {@code untilMillis}, in place of what it gave before. */
    public void give(String source, double amount, long untilMillis) {
        if (amount <= 0) {
            pools.remove(source);
            return;
        }
        Pool pool = new Pool();
        pool.amount = amount;
        pool.until = untilMillis;
        pools.put(source, pool);
    }

    /** All of it at {@code now}. */
    public double total(long now) {
        double total = 0;
        for (Pool pool : pools.values()) if (pool.until > now) total += pool.amount;
        return total;
    }

    /** What's left of this source's at {@code now}; 0 for none. */
    public double of(String source, long now) {
        Pool pool = pools.get(source);
        return pool == null || pool.until <= now ? 0 : pool.amount;
    }

    /** What's left of a hit that would take {@code taken} once absorption has taken what it can, at {@code now}. */
    public double absorb(double taken, long now) {
        if (taken <= 0 || pools.isEmpty()) return Math.max(0, taken);
        while (taken > 0) {
            Pool soonest = null;
            for (Iterator<Pool> it = pools.values().iterator(); it.hasNext(); ) {
                Pool pool = it.next();
                if (pool.until <= now || pool.amount <= 0) {
                    it.remove();
                } else if (soonest == null || pool.until < soonest.until) {
                    soonest = pool;
                }
            }
            if (soonest == null) break;
            double used = Math.min(soonest.amount, taken);
            soonest.amount -= used;
            taken -= used;
        }
        pools.values().removeIf(pool -> pool.amount <= 0);
        return taken;
    }

    /** None of it any more. */
    public void clear() {
        pools.clear();
    }

    // What the rest of the plugin asks, by player.

    /** Gives them {@code amount} absorption from {@code source} for {@code millis}, in place of what it gave before. */
    public static void give(Player player, String source, double amount, long millis) {
        PlayerSession.of(player).getAbsorption().give(source, amount, System.currentTimeMillis() + millis);
    }

    /** Their absorption now. */
    public static double get(Player player) {
        return PlayerSession.of(player).getAbsorption().total(System.currentTimeMillis());
    }

    /** What's left of what this source gave them (Wither Shield's refund goes by it). */
    public static double left(Player player, String source) {
        return PlayerSession.of(player).getAbsorption().of(source, System.currentTimeMillis());
    }

    /** Takes a hit of {@code taken} from their absorption first: what's left for their health. */
    public static double absorb(Player player, double taken) {
        return PlayerSession.of(player).getAbsorption().absorb(taken, System.currentTimeMillis());
    }

    /** What this source gave them is gone. */
    public static void remove(Player player, String source) {
        PlayerSession.of(player).getAbsorption().give(source, 0, 0);
    }

    public static void clear(Player player) {
        PlayerSession.of(player).getAbsorption().clear();
    }
}
