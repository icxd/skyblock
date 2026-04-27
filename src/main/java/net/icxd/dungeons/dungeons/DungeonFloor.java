package net.icxd.dungeons.dungeons;

import lombok.Getter;

public enum DungeonFloor {
    ENTRANCE("Entrance", false, 4, 4),
    FLOOR_1("Floor 1", false, 4, 4),
    FLOOR_2("Floor 2", false, 4, 4),
    FLOOR_3("Floor 3", false, 4, 4),
    FLOOR_4("Floor 4", false, 5, 5),
    FLOOR_5("Floor 5", false, 5, 5),
    FLOOR_6("Floor 6", false, 7, 7),
    FLOOR_7("Floor 7", false, 7, 7),
    MASTER_FLOOR_1("Master Floor 1", true, 4, 4),
    MASTER_FLOOR_2("Master Floor 2", true, 4, 4),
    MASTER_FLOOR_3("Master Floor 3", true, 4, 4),
    MASTER_FLOOR_4("Master Floor 4", true, 5, 5),
    MASTER_FLOOR_5("Master Floor 5", true, 5, 5),
    MASTER_FLOOR_6("Master Floor 6", true, 7, 7),
    MASTER_FLOOR_7("Master Floor 7", true, 7, 7);
    ;

    @Getter
    private final String name;
    @Getter
    private final boolean masterMode;
    @Getter
    private final int maxXSize, maxZSize;
    DungeonFloor(String name, boolean masterMode, int maxXSize, int maxZSize) {
        this.name = name;
        this.masterMode = masterMode;
        this.maxXSize = maxXSize;
        this.maxZSize = maxZSize;
    }
}
