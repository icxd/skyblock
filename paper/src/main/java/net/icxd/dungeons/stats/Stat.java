package net.icxd.dungeons.stats;

import lombok.Getter;

/**
 * A SkyBlock stat: its name, symbol and colour as Hypixel shows them (the symbols and colours are the
 * wiki's, Module:Statname/Data; where item lore colours a stat's value differently, as with vitality
 * and mending, {@link #loreColor} is the lore's). In the order item lore lists them.
 */
@Getter
public enum Stat {
    DAMAGE("Damage", "❁", 'c'),
    HEALTH("Health", "❤", 'c'),
    DEFENSE("Defense", "❈", 'a'),
    STRENGTH("Strength", "❁", 'c'),
    INTELLIGENCE("Intelligence", "✎", 'b'),
    CRIT_CHANCE("Crit Chance", "☣", '9', true),
    CRIT_DAMAGE("Crit Damage", "☠", '9', true),
    ATTACK_SPEED("Attack Speed", "⚔", 'e', true),
    ABILITY_DAMAGE("Ability Damage", "๑", 'c', true),
    FEROCITY("Ferocity", "⫽", 'c'),
    TRUE_DEFENSE("True Defense", "❂", 'f'),
    HEALTH_REGEN("Health Regen", "❣", 'c'),
    VITALITY("Vitality", "♨", '4', 'a', false),
    MENDING("Mending", "☄", 'a'),
    SPEED("Speed", "✦", 'f'),
    SWING_RANGE("Swing Range", "Ⓢ", 'e'),
    MAGIC_FIND("Magic Find", "✯", 'b'),
    PET_LUCK("Pet Luck", "♣", 'd'),
    SEA_CREATURE_CHANCE("Sea Creature Chance", "α", '3', true),
    FISHING_SPEED("Fishing Speed", "☂", 'b'),
    MINING_SPEED("Mining Speed", "⸕", '6'),
    MINING_FORTUNE("Mining Fortune", "☘", '6'),
    BREAKING_POWER("Breaking Power", "Ⓟ", '2'),
    FARMING_FORTUNE("Farming Fortune", "☘", '6'),
    FORAGING_FORTUNE("Foraging Fortune", "☘", '6'),
    COMBAT_WISDOM("Combat Wisdom", "☯", '3'),
    FARMING_WISDOM("Farming Wisdom", "☯", '3'),
    FORAGING_WISDOM("Foraging Wisdom", "☯", '3'),
    FISHING_WISDOM("Fishing Wisdom", "☯", '3'),
    RIFT_TIME("Rift Time", "ф", 'a'),
    RIFT_DAMAGE("Rift Damage", "❁", '5'),
    RIFT_INTELLIGENCE("Rift Intelligence", "✎", 'b'),
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
