package net.icxd.dungeons.mob;

/**
 * SkyBlock's mob types (the wiki's Module:MobType/Data): what Smite, Bane of Arthropods and the
 * like count, and what a dungeon mob's name tag starts with. Hypixel draws them with glyphs from its
 * resource pack ({@link #glyph}), which this server doesn't send, so name tags show the classic
 * symbol instead (tools/items/data/glyphs.tsv); Critter and Timid have none, and show nothing.
 */
public enum MobType {
    AIRBORNE('', "✈", '7'),
    ANIMAL('', "☮", 'a'),
    AQUATIC('', "⚓", '9'),
    ARCANE('', "♃", '5'),
    ARTHROPOD('', "Ж", '4'),
    CONSTRUCT('', "⚙", '7'),
    CUBIC('', "⚂", 'a'),
    ELUSIVE('', "❃", 'd'),
    ENDER('', "⊙", '5'),
    FROZEN('', "❆", 'f'),
    GLACIAL('', "❆", 'b'),
    HUMANOID('', "✰", 'e'),
    INFERNAL('', "♨", '4'),
    MAGMATIC('', "♆", 'c'),
    MYTHOLOGICAL('', "✿", '2'),
    PEST('', "ൠ", '2'),
    SHIELDED('', "⛨", 'e'),
    SKELETAL('', "🦴", 'f'),
    SPOOKY('', "☽", '6'),
    SUBTERRANEAN('', "⸕", '6'),
    UNDEAD('', "༕", '2'),
    WITHER('', "☠", '8'),
    WOODLAND('', "⸙", '2'),
    CRITTER('', "", 'a'),
    TIMID('', "", 'e');

    /** Hypixel's resource-pack character for it. */
    private final char glyph;
    /** What's shown instead; empty if there's no classic symbol. */
    private final String symbol;
    /** Its colour code (the character after '&'). */
    private final char color;

    MobType(char glyph, String symbol, char color) {
        this.glyph = glyph;
        this.symbol = symbol;
        this.color = color;
    }

    public char glyph() {
        return glyph;
    }

    public String symbol() {
        return symbol;
    }

    public char color() {
        return color;
    }
}
