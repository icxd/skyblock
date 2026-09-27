package net.icxd.dungeons.mob;

import lombok.Getter;

/** How rare a mob drop is, and its colour (a CRAZY RARE or INSANE drop's line is in it; every RARE DROP! line is gold). */
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
