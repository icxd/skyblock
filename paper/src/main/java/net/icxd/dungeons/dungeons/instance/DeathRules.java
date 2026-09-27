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
     * died and become Ghosts or left the run"), or it has gone on for 60 minutes. Taken as written, the
     * first also ends a solo Entrance at the first death, before its 15 second revive: whether Hypixel
     * waits for that is UNKNOWN.
     */
    static boolean failed(int here, int ghostsHere, long elapsedMillis) {
        return (here > 0 && ghostsHere >= here) || elapsedMillis >= TIME_LIMIT_MILLIS;
    }

    /**
     * A failed run's score: "If the boss is not killed, Dungeon Score will be reduced by 30%" (FW Dungeon
     * Score trivia; no failed run was recorded). Each part is cut by 30% and rounded, as the Entrance's
     * own 70% is.
     */
    static Score failedScore(Score score) {
        return new Score(cut(score.skill()), cut(score.explore()), cut(score.speed()), cut(score.bonus()));
    }

    private static int cut(int points) {
        return (int) Math.round(points * 0.7);
    }
}
