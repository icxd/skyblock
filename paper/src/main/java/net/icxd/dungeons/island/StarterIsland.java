package net.icxd.dungeons.island;

/**
 * The island a new profile starts with, as the 2026-10-24 recording has Hypixel's (and the capture
 * of it, {@code worlds/<n>_your_island}): a round grass island with an oak tree, and 15 blocks south
 * of it, across the void, a rock with Jerry, the Cobblestone Minion and the portal to the Hub.
 * There's no bridge: players build their own (the recording's player bridged it with oak planks).
 * Coordinates are the island world's, which are Hypixel's.
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

    private StarterIsland() {
    }
}
