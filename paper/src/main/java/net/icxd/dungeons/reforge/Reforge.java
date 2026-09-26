package net.icxd.dungeons.reforge;

import lombok.Getter;

import static net.icxd.dungeons.stats.Stat.*;

@Getter
public enum Reforge {
    HEROIC("Heroic", new ReforgeStats()
            .with(INTELLIGENCE, 40, 50, 65, 80, 100, 125)
            .with(STRENGTH, 15, 20, 25, 32, 40, 50)
            .with(ATTACK_SPEED, 1, 2, 2, 3, 5, 7)),
    WITHERED("Withered", new ReforgeStats()
            .with(STRENGTH, 60, 75, 90, 110, 135, 170)),
    ANCIENT("Ancient", new ReforgeStats()
            .with(HEALTH, 7, 7, 7, 7, 7, 7)
            .with(DEFENSE, 7, 7, 7, 7, 7, 7)
            .with(INTELLIGENCE, 6, 9, 12, 16, 20, 25)
            .with(STRENGTH, 4, 8, 12, 18, 25, 35)
            .with(CRIT_CHANCE, 3, 5, 7, 9, 12, 15)),
    HASTY("Hasty", new ReforgeStats()
            .with(STRENGTH, 3, 5, 7, 10, 15, 20)
            .with(CRIT_CHANCE, 20, 25, 30, 40, 50, 60))
    ;

    private final String name;
    private final ReforgeStats stats;

    Reforge(String name, ReforgeStats stats) {
        this.name = name;
        this.stats = stats;
    }
}
