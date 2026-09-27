package net.icxd.dungeons.dungeons.instance;

/**
 * What a run's score is worked out from that the rest of the run counts: secrets, crypts, puzzles
 * and deaths. {@link DungeonRun} implements this; each count is 0 until the code that keeps it
 * overrides it there (with a public method of the same name returning an int).
 */
interface ScoreCounts {
    /** Secrets the team has found. */
    default int secretsFound() {
        return 0;
    }

    /** Secrets on the floor. */
    default int totalSecrets() {
        return 0;
    }

    /** Crypts blown up (their Crypt Undead count for the bonus, 5 at most). */
    default int cryptsBlown() {
        return 0;
    }

    /** Puzzles failed or not done yet, found or not (each costs 10 skill). */
    default int puzzlesNotDone() {
        return 0;
    }

    /** Deaths in the run (each costs 2 skill). */
    default int deaths() {
        return 0;
    }
}
