package net.icxd.dungeons.mob;

import lombok.Getter;

/** How rare a mob drop is, and the colour its "RARE DROP!" message is in. */
@Getter
public enum MobDropType {
    GUARANTEED('a'),
    COMMON('a'),
    OCCASIONAL('9'),
    RARE('9'),
    VERY_RARE('b'),
    EXTRAORDINARILY_RARE('5'),
    CRAZY_RARE('d'),
    RNGESUS_INCARNATE('c');

    /** Its colour code (the character after '§'). */
    private final char color;

    MobDropType(char color) {
        this.color = color;
    }

    public String getDisplay() {
        return "§" + color + "§l" + name().replaceAll("_", " ");
    }
}
