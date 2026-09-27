package net.icxd.dungeons.item.ability.weapons;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An ability with charges ("Charges: 4 / 5s"): up to {@code max} uses, "each charge recharges every 5s" (the
 * wiki's Bone Reaver) on its own, from when it was used. Each player's are their own. No Bukkit in it.
 */
final class Charges {
    private final int max;
    private final long rechargeMillis;
    /** When each player's spent charges were used, oldest first. */
    private final Map<UUID, Deque<Long>> spent = new HashMap<>();

    Charges(int max, long rechargeMillis) {
        this.max = max;
        this.rechargeMillis = rechargeMillis;
    }

    /** How many they have at {@code now}. */
    int left(UUID player, long now) {
        Deque<Long> uses = spent.get(player);
        if (uses == null) return max;
        while (!uses.isEmpty() && now - uses.peekFirst() >= rechargeMillis) uses.pollFirst();
        if (uses.isEmpty()) spent.remove(player);
        return max - uses.size();
    }

    /** Uses one if they have one; whether they did. */
    boolean use(UUID player, long now) {
        if (left(player, now) <= 0) return false;
        spent.computeIfAbsent(player, id -> new ArrayDeque<>()).addLast(now);
        return true;
    }

    void forget(UUID player) {
        spent.remove(player);
    }
}
