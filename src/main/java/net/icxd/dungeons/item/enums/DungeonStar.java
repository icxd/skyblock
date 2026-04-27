package net.icxd.dungeons.item.enums;

import lombok.Getter;

public enum DungeonStar {
    ZERO(0),
    ONE(0.1),
    TWO(0.2),
    THREE(0.3),
    FOUR(0.4),
    FIVE(0.5),
    SIX(0.55),
    SEVEN(0.6),
    EIGHT(0.65),
    NINE(0.7),
    TEN(0.75);

    @Getter
    private final double boost;

    DungeonStar(double boost) {
        this.boost = boost;
    }
}
