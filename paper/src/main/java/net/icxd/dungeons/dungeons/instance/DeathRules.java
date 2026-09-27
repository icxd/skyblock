package net.icxd.dungeons.dungeons.instance;

import net.icxd.dungeons.common.DungeonFloor;

/**
 * When ghosts come back by themselves, and when a run fails (research critic.md 3.1: MCW Ghosts,
 * MCW Catacombs "Finishing a Run", FW Dungeon Score).
 */
final class DeathRules {
    /** "after 60 minutes without killing the boss". */
    static final long TIME_LIMIT_MILLIS = 60 * 60 * 1000L;
    /** "The reviving process itself takes 5 seconds" (MCW Revive Stone). */
    static final long REVIVE_STONE_TICKS = 5 * 20;

    private DeathRules() {
    }

    /**
     * Seconds until a ghost comes back by itself: 15 on the Entrance, 45 on Floor I, 100 on Floor II
     * (MCW Ghosts, a stub page; FW has no timer); -1 elsewhere, where only a revive brings them back.
     */
    static int autoReviveSeconds(DungeonFloor floor) {
        if (floor.isMasterMode()) return -1;
        return switch (floor.getNumber()) {
            case 0 -> 15;
            case 1 -> 45;
            case 2 -> 100;
            default -> -1;
        };
    }

    /**
     * Whether the run has failed: everyone still in it is a ghost ("All players in the run have either
     * died and become Ghosts or left the run") with none coming back by itself, or it has gone on for 60
     * minutes. {@code ghostsComeBack} is for floors whose ghosts are revived after a while ({@link
     * #autoReviveSeconds}): there a wipe waits for that, else a solo Entrance would end at its first
     * death and its 15 second revive could never happen. Whether Hypixel waits is UNKNOWN.
     */
    static boolean failed(int here, int ghostsHere, boolean ghostsComeBack, long elapsedMillis) {
        return (here > 0 && ghostsHere >= here && !ghostsComeBack) || elapsedMillis >= TIME_LIMIT_MILLIS;
    }

    /**
     * A failed run's score, UNVERIFIED (no failed run was recorded; FW Dungeon Score): its Speed isn't
     * the time's but "the percentage of the dungeon rooms that have been cleared" (see {@link
     * #failedSpeed}), and then "If the boss is not killed, Dungeon Score will be reduced by 30%": each
     * part is cut by 30% and rounded, as the Entrance's own 70% is.
     */
    static Score failedScore(Score score, DungeonFloor floor, int completedRooms, int totalRooms) {
        return new Score(cut(score.skill()), cut(score.explore()), cut(failedSpeed(floor, completedRooms, totalRooms)), cut(score.bonus()));
    }

    /**
     * A failed run's Speed before the cut: the share of the rooms cleared, in whole percent (rounded
     * down, as the sidebar's "Cleared: 46%"), and 70% of that on the Entrance, as every part is there.
     */
    static int failedSpeed(DungeonFloor floor, int completedRooms, int totalRooms) {
        int percent = totalRooms <= 0 ? 0 : Math.clamp(completedRooms * 100L / totalRooms, 0, 100);
        return floor.getNumber() == 0 ? (int) (percent * 0.7) : percent;
    }

    private static int cut(int points) {
        return (int) Math.round(points * 0.7);
    }
}
