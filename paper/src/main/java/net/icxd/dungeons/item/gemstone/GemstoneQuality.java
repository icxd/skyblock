package net.icxd.dungeons.item.gemstone;

import lombok.Getter;

/**
 * How refined a gemstone is, from Rough to Perfect: the better, the more it gives. Its colour is the one a filled
 * slot's brackets have in the item's "Gemstones:" line ("&9[&d⚔&9]" is a Fine gem), as the wiki's Gemstone Slot
 * format has them and live items show them.
 */
@Getter
public enum GemstoneQuality {
    ROUGH("Rough", 'f'),
    FLAWED("Flawed", 'a'),
    FINE("Fine", '9'),
    FLAWLESS("Flawless", '5'),
    PERFECT("Perfect", '6');

    private final String name;
    /** Its colour code (the character after '§'). */
    private final char color;

    GemstoneQuality(String name, char color) {
        this.name = name;
        this.color = color;
    }

    /** The quality by its name ("FINE"); null for none. */
    public static GemstoneQuality of(String name) {
        for (GemstoneQuality quality : values()) if (quality.name().equals(name)) return quality;
        return null;
    }
}
