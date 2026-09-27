package net.icxd.dungeons.dungeons.instance;

import java.util.BitSet;

/**
 * A room's secrets as the team finds them ({@link RunSecrets}): each of its waypoints counts once, and the
 * room never shows more than Hypixel counts for it (Golden Oasis shows "0/1 Secrets" with two chests and a
 * Redstone Key). No Bukkit in it.
 */
final class SecretCount {
    private final int total;
    private final BitSet found = new BitSet();

    /** {@code total}: how many secrets Hypixel counts in the room (the action bar's "/5"). */
    SecretCount(int total) {
        this.total = Math.max(0, total);
    }

    /** The room's waypoint at this index was found: true the first time, false if it had been already. */
    boolean find(int waypoint) {
        if (found.get(waypoint)) return false;
        found.set(waypoint);
        return true;
    }

    int total() {
        return total;
    }

    /** Found, never more than {@link #total()}. */
    int shown() {
        return Math.min(found.cardinality(), total);
    }

    /** Whether they're all found (a room without any: yes). */
    boolean allFound() {
        return shown() >= total;
    }
}
