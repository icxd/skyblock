package net.icxd.dungeons.stats;

import lombok.Getter;

/**
 * A SkyBlock stat: its name, symbol and colour as Hypixel shows them (the wiki's Module:Statname/Data;
 * {@link #loreColor} is the colour item lore gives its value, where that differs). In the order
 * Hypixel's item lore lists them (worked out from the live auction house's items; stats that never
 * show up together are placed by their group).
 */
@Getter
public enum Stat {
    HEALTH("Health", "❤", 'c'),
    DEFENSE("Defense", "❈", 'a'),
    TRUE_DEFENSE("True Defense", "❂", 'f'),
    DAMAGE("Damage", "❁", 'c'),
    STRENGTH("Strength", "❁", 'c'),
    CRIT_CHANCE("Crit Chance", "☣", '9', true),
    CRIT_DAMAGE("Crit Damage", "☠", '9', true),
    ATTACK_SPEED("Attack Speed", "⚔", 'e', true),
    FEROCITY("Ferocity", "⫽", 'c'),
    SWING_RANGE("Swing Range", "Ⓢ", 'e'),
    INTELLIGENCE("Intelligence", "✎", 'b'),
    ABILITY_DAMAGE("Ability Damage", "๑", 'c', true),
    HEALTH_REGEN("Health Regen", "❣", 'c'),
    VITALITY("Vitality", "♨", '4'),
    MENDING("Mending", "☄", 'a'),
    FARMING_FORTUNE("Farming Fortune", "☘", '6'),
    FISHING_SPEED("Fishing Speed", "☂", 'b'),
    SEA_CREATURE_CHANCE("Sea Creature Chance", "α", '3', true),
    FORAGING_FORTUNE("Foraging Fortune", "☘", '6'),
    MINING_SPEED("Mining Speed", "⸕", '6'),
    MINING_FORTUNE("Mining Fortune", "☘", '6'),
    SPEED("Speed", "✦", 'f'),
    MAGIC_FIND("Magic Find", "✯", 'b'),
    PET_LUCK("Pet Luck", "♣", 'd'),
    COMBAT_WISDOM("Combat Wisdom", "☯", '3'),
    FARMING_WISDOM("Farming Wisdom", "☯", '3'),
    FISHING_WISDOM("Fishing Wisdom", "☯", '3'),
    FORAGING_WISDOM("Foraging Wisdom", "☯", '3'),
    RIFT_TIME("Rift Time", "ф", 'a'),
    RIFT_DAMAGE("Rift Damage", "❁", '5'),
    RIFT_INTELLIGENCE("Rift Intelligence", "✎", 'b'),
    BREAKING_POWER("Breaking Power", "Ⓟ", '2'),
    /** A weapon's own ability damage (e.g. what Giant's Slam hits for), not a player stat. */
    WEAPON_ABILITY_DAMAGE("Weapon Ability Damage", "๑", 'c');

    private static final Stat[] VALUES = values();

    private final String displayName;
    private final String symbol;
    /** The stat's colour code (the character after '&'). */
    private final char color;
    /** The colour of its value in item lore. */
    private final char loreColor;
    /** Shown as a percentage ("+50%"). */
    private final boolean percent;

    Stat(String displayName, String symbol, char color) {
        this(displayName, symbol, color, color, false);
    }

    Stat(String displayName, String symbol, char color, boolean percent) {
        this(displayName, symbol, color, color, percent);
    }

    Stat(String displayName, String symbol, char color, char loreColor, boolean percent) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
        this.loreColor = loreColor;
        this.percent = percent;
    }

    /** {@link #values()} without the copy. */
    static Stat[] all() {
        return VALUES;
    }

    /** "&c❁ Strength": as the stat is named in text. */
    public String label() {
        return "&" + color + symbol + " " + displayName;
    }
}
