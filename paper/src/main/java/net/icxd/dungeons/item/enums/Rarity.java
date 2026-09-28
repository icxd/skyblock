package net.icxd.dungeons.item.enums;

import lombok.Getter;

public enum Rarity {
    COMMON('f'),
    UNCOMMON('a'),
    RARE('9'),
    EPIC('5'),
    LEGENDARY('6'),
    MYTHIC('d'),
    DIVINE('b'),
    SPECIAL('c'),
    VERY_SPECIAL('c'),
    UNOBTAINABLE('4');

    /** Its colour code (the character after '§'). */
    @Getter
    private final char code;

    Rarity(char code) {
        this.code = code;
    }

    /**
     * One up, as a Recombobulator 3000 makes it (the wiki's Recombobulator 3000): Mythic to Divine, Divine to
     * Special, Special to Very Special, and Very Special stays Very Special. Unobtainable (the API's, on admin
     * items) stays as it is. UNKNOWN whether it's the wiki's cosmetics' rarity that goes to Very Special.
     */
    public Rarity upgrade() {
        return this == VERY_SPECIAL || this == UNOBTAINABLE ? this : values()[this.ordinal() + 1];
    }

    public Rarity downgrade() {
        if (this.ordinal() - 1 < 0)
            return this;
        return values()[this.ordinal() - 1];
    }

    /** "§6". */
    public String getColor() {
        return "§" + code;
    }

    /** "§6§lLEGENDARY". */
    public String getDisplay() {
        return getBoldedColor() + name().replaceAll("_", " ");
    }

    /** "§6§l". */
    public String getBoldedColor() {
        return getColor() + "§l";
    }
}
