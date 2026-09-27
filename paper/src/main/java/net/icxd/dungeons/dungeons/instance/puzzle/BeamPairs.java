package net.icxd.dungeons.dungeons.instance.puzzle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creeper Beams' picks and beams, without the world ({@link CreeperBeamsPuzzle} shows them). Each
 * player picks a lantern, then another: the two make a beam if the line between them goes through
 * the creeper ({@link BeamGeometry}), else the first pick is dropped. A lantern goes into one beam
 * at most (the wiki: "four different beams"; Skyblocker never reuses one), so a pick of a lantern
 * someone else has put in a beam since counts for nothing.
 */
final class BeamPairs {
    /** What a pick did. */
    enum Pick {
        /** Nothing: that lantern is in a beam already, or the beams are all made. */
        NONE,
        /** The player's first lantern (or the same one again). */
        FIRST,
        /** Their second, but the line misses the creeper: the first is dropped. */
        MISSED,
        /** Their second, and the two make a beam ({@link #last()}). */
        BEAM
    }

    private final double[] feet;
    private final int needed;
    private final List<int[]> used = new ArrayList<>();
    private final List<int[][]> beams = new ArrayList<>();
    private final Map<UUID, int[]> picked = new HashMap<>();

    /**
     * @param feet   where the creeper stands (the middle of its feet)
     * @param needed how many beams it takes
     */
    BeamPairs(double[] feet, int needed) {
        this.feet = feet.clone();
        this.needed = needed;
    }

    Pick pick(UUID player, int[] lantern) {
        if (isDone() || isUsed(lantern)) return Pick.NONE;
        int[] first = picked.remove(player);
        // Their first pick may have gone into someone else's beam since.
        if (first != null && isUsed(first)) first = null;
        if (first == null || Arrays.equals(first, lantern)) {
            picked.put(player, lantern);
            return Pick.FIRST;
        }
        if (!BeamGeometry.throughCreeper(first, lantern, feet)) return Pick.MISSED;
        used.add(first);
        used.add(lantern);
        beams.add(new int[][]{first, lantern});
        return Pick.BEAM;
    }

    /** The beam made last: its two lanterns, the first pick first. */
    int[][] last() {
        return beams.isEmpty() ? null : beams.get(beams.size() - 1);
    }

    List<int[][]> beams() {
        return beams;
    }

    boolean isDone() {
        return beams.size() >= needed;
    }

    private boolean isUsed(int[] lantern) {
        for (int[] u : used) if (Arrays.equals(u, lantern)) return true;
        return false;
    }
}
