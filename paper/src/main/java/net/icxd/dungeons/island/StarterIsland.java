package net.icxd.dungeons.island;

import java.util.ArrayList;
import java.util.List;

/**
 * The island a new profile starts with, as the 2026-10-24 recording has Hypixel's (and the capture
 * of it, {@code worlds/<n>_your_island}): a round grass island with an oak tree, and 15 blocks south
 * of it a rock with Jerry, the Cobblestone Minion and the portal to the Hub. Coordinates are the
 * island world's, which are Hypixel's.
 */
public final class StarterIsland {
    /** Where a player arrives, facing south towards Jerry and the portal (the arrival teleport: 7.5 100 7.5, yaw 0). */
    public static final double[] SPAWN = {7.5, 100, 7.5};
    public static final float SPAWN_YAW = 0;
    /** Jerry's feet and facing, as he was until moved (9.5 100 33.5, northwest). */
    public static final double[] JERRY = {9.5, 100, 33.5};
    public static final float JERRY_YAW = 139.22f;
    /** The bottom line of "Travel to: / Hub Island" over the portal; the line above is 0.375 higher. */
    public static final double[] PORTAL_SIGN = {6.5, 100.5625, 41.5};

    /**
     * The bridge to the rock: planks along x 7 at y 99, from z 16 (the island's edge is z 15) to z 30
     * (the rock starts at 31). None of it is there at first; Hypixel lays the planks under a player
     * walking out along it, the one they're over and the next, and they stay (recorded: (7,99,16) to
     * (7,99,26) appeared as the player walked south from z 16 to 26, the rest on the next walk).
     */
    static final int BRIDGE_X = 7;
    static final int BRIDGE_Y = 99;
    static final int BRIDGE_FROM_Z = 16;
    static final int BRIDGE_TO_Z = 30;

    private StarterIsland() {
    }

    /**
     * The bridge blocks to lay for a player standing at this block (their feet), walking with z
     * changing by {@code dz}: the one under them and the next one in the direction they're going.
     * Only for a player over the bridge's line (a block either side of it, as a player that wide still
     * stands on it) at its height. Each as {x, y, z}.
     */
    static List<int[]> bridgeUnder(int x, int y, int z, int dz) {
        List<int[]> blocks = new ArrayList<>();
        if (Math.abs(x - BRIDGE_X) > 1 || y < BRIDGE_Y + 1 || y > BRIDGE_Y + 2) return blocks;
        int ahead = dz < 0 ? -1 : 1;
        for (int bz : new int[]{z, z + ahead}) {
            if (bz >= BRIDGE_FROM_Z && bz <= BRIDGE_TO_Z) blocks.add(new int[]{BRIDGE_X, BRIDGE_Y, bz});
        }
        return blocks;
    }
}
