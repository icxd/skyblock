package net.icxd.dungeons.reforge;

import lombok.Getter;

@Getter
public enum Reforge {
    HEROIC("Heroic", new ReforgeStats(
            new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(40, 50, 65, 80, 100, 125),
            new ReforgeStat(),
            new ReforgeStat(15, 20, 25, 32, 40, 50),
            new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(1, 2, 2, 3, 5, 7),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat()
    )),
    WITHERED("Withered", new ReforgeStats(
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(60, 75, 90, 110, 135, 170),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(), new ReforgeStat()
    )),
    ANCIENT("Ancient", new ReforgeStats(
            new ReforgeStat(7, 7, 7, 7, 7, 7),
            new ReforgeStat(7, 7, 7, 7, 7, 7),
            new ReforgeStat(6, 9, 12, 16, 20, 25),
            new ReforgeStat(),
            new ReforgeStat(4, 8, 12, 18, 25, 35),
            new ReforgeStat(3, 5, 7, 9, 12, 15),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat()
    )),
    HASTY("Hasty", new ReforgeStats(
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(3, 5, 7, 10, 15, 20),
            new ReforgeStat(20, 25, 30, 40, 50, 60),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat(), new ReforgeStat(), new ReforgeStat(), new ReforgeStat(),
            new ReforgeStat()
    ))
    ;

    private final String name;
    private final ReforgeStats stats;

    Reforge(String name, ReforgeStats stats) {
        this.name = name;
        this.stats = stats;
    }
}
