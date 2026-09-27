package net.icxd.dungeons.dungeons.instance.puzzle;

/**
 * A puzzle's line in the tab list's Dungeon Stats column, as recorded (2026_09_26_08_23_40 and
 * 08_49_22): " ???: [✦]" until someone walks in, then its name (with a space at the end), and a
 * green tick or a red cross with who failed it. Hypixel shows each change a moment after it happens
 * ({@link #FOUND}, {@link #SOLVED}, {@link #FAILED} ticks).
 */
public final class PuzzleTab {
    /** Ticks from someone walking in to the name showing (Water Board 03:11.7 -> 03:12.4, Tic Tac Toe 03:48.9 -> 03:49.5). */
    static final long FOUND = 10;
    /** From opening the chest to the tick (Water Board 03:29.7 -> 03:30.5). */
    static final long SOLVED = 16;
    /** From the PUZZLE FAIL line to the cross (Tic Tac Toe 04:06.4 -> 04:06.8). */
    static final long FAILED = 8;

    public enum State { UNDISCOVERED, DISCOVERED, SOLVED, FAILED }

    private PuzzleTab() {
    }

    public static String row(String name, State state, String failedBy) {
        return switch (state) {
            case UNDISCOVERED -> " ???: &7[&6&l✦&7]";
            case DISCOVERED -> " " + name + ": &7[&6&l✦&7] ";
            case SOLVED -> " " + name + ": &7[&a&l✔&7] ";
            // The name in aqua as recorded; whether it's the failer's rank colour is UNKNOWN (only an MVP+ was recorded).
            case FAILED -> " " + name + ": &7[&c&l✖&7] &f(&b" + failedBy + "&f)";
        };
    }
}
