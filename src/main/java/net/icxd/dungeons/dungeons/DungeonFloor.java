package net.icxd.dungeons.dungeons;

import lombok.Getter;

/**
 * Map sizes are what the map mods hard-code (Odin, Skytils): 4x4 entrance, 4x5 F1, 5x5 F2-F3,
 * 6x5 F4, 6x6 F5-F7, same for master mode. The wiki lists F5 as 6x5 and M7 as 6x7, which the mods
 * contradict.
 *
 * <p>Special rooms: one miniboss (yellow) room per floor, one trap room from F3 up. Puzzles are
 * from play experience: usually 2 on the small maps and 4-5 on the big ones; 5x5 sits in between.
 *
 * <p>{@code specialColumn}: on F4-F6 Odin's map assumes the last column is filled with puzzles,
 * trap and miniboss ({@code SpecialColumn.kt}), i.e. those floors are a 5-wide dungeon with a
 * column of special rooms bolted on. Specials that don't fit in the column go elsewhere.
 */
@Getter
public enum DungeonFloor {
    ENTRANCE("Entrance", false, 0, 4, 4, 2, 2, 0, false),
    FLOOR_1("Floor 1", false, 1, 4, 5, 2, 2, 0, false),
    FLOOR_2("Floor 2", false, 2, 5, 5, 2, 3, 0, false),
    FLOOR_3("Floor 3", false, 3, 5, 5, 2, 3, 1, false),
    FLOOR_4("Floor 4", false, 4, 6, 5, 4, 5, 1, true),
    FLOOR_5("Floor 5", false, 5, 6, 6, 4, 5, 1, true),
    FLOOR_6("Floor 6", false, 6, 6, 6, 4, 5, 1, true),
    FLOOR_7("Floor 7", false, 7, 6, 6, 4, 5, 1, false),
    MASTER_FLOOR_1("Master Floor 1", true, 1, 4, 5, 2, 2, 0, false),
    MASTER_FLOOR_2("Master Floor 2", true, 2, 5, 5, 2, 3, 0, false),
    MASTER_FLOOR_3("Master Floor 3", true, 3, 5, 5, 2, 3, 1, false),
    MASTER_FLOOR_4("Master Floor 4", true, 4, 6, 5, 4, 5, 1, true),
    MASTER_FLOOR_5("Master Floor 5", true, 5, 6, 6, 4, 5, 1, true),
    MASTER_FLOOR_6("Master Floor 6", true, 6, 6, 6, 4, 5, 1, true),
    MASTER_FLOOR_7("Master Floor 7", true, 7, 6, 6, 4, 5, 1, false);

    private final String name;
    private final boolean masterMode;
    /** 0 for the entrance, otherwise the floor number (same for normal and master mode). */
    private final int number;
    /** Map size in rooms. */
    private final int maxXSize, maxZSize;
    private final int minPuzzles, maxPuzzles;
    private final int traps;
    private final int minibosses = 1;
    private final boolean specialColumn;

    DungeonFloor(String name, boolean masterMode, int number, int maxXSize, int maxZSize,
                 int minPuzzles, int maxPuzzles, int traps, boolean specialColumn) {
        this.name = name;
        this.masterMode = masterMode;
        this.number = number;
        this.maxXSize = maxXSize;
        this.maxZSize = maxZSize;
        this.minPuzzles = minPuzzles;
        this.maxPuzzles = maxPuzzles;
        this.traps = traps;
        this.specialColumn = specialColumn;
    }
}
