package net.icxd.dungeons.stats;

import lombok.Getter;

/**
 * A SkyBlock stat: its name, symbol and colour as Hypixel shows them (the wiki's Module:Statname/Data;
 * {@link #loreColor} is the colour item lore gives its value, where that differs). In the order
 * Hypixel's item lore lists them (worked out from the live auction house's items and the NEU
 * repository's in-game dumps; stats that never show up together are placed by their group).
 * <p>
 * The name is the one item lore gives the stat's line, which for the Rift's stats is mostly the
 * ordinary stat's ("Intelligence", "Speed"): the two stay apart because the Rift keeps its own stats.
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
    BONUS_PEST_CHANCE("Bonus Pest Chance", "ൠ", '2', true),
    FARMING_FORTUNE("Farming Fortune", "☘", '6'),
    WHEAT_FORTUNE("Wheat Fortune", "☘", '6'),
    CARROT_FORTUNE("Carrot Fortune", "☘", '6'),
    POTATO_FORTUNE("Potato Fortune", "☘", '6'),
    PUMPKIN_FORTUNE("Pumpkin Fortune", "☘", '6'),
    SUGAR_CANE_FORTUNE("Sugar Cane Fortune", "☘", '6'),
    MELON_FORTUNE("Melon Slice Fortune", "☘", '6'),
    CACTUS_FORTUNE("Cactus Fortune", "☘", '6'),
    COCOA_BEANS_FORTUNE("Cocoa Beans Fortune", "☘", '6'),
    MUSHROOM_FORTUNE("Mushroom Fortune", "☘", '6'),
    NETHER_STALK_FORTUNE("Nether Wart Fortune", "☘", '6'),
    FISHING_SPEED("Fishing Speed", "☂", 'b'),
    SEA_CREATURE_CHANCE("Sea Creature Chance", "α", '3', true),
    DOUBLE_HOOK_CHANCE("Double Hook Chance", "⚓", '9', true),
    TROPHY_FISH_CHANCE("Trophy Chance", "♔", '6', true),
    TREASURE_CHANCE("Treasure Chance", "⛃", '6', true),
    SWEEP("Sweep", "∮", '2'),
    FORAGING_FORTUNE("Foraging Fortune", "☘", '6'),
    FIG_FORTUNE("Fig Fortune", "☘", '6'),
    MANGROVE_FORTUNE("Mangrove Fortune", "☘", '6'),
    HELIX_FORTUNE("Helix Fortune", "☘", '6'),
    MINING_SPEED("Mining Speed", "⸕", '6'),
    PRISTINE("Pristine", "✧", '5'),
    MINING_FORTUNE("Mining Fortune", "☘", '6'),
    ORE_FORTUNE("Ore Fortune", "☘", '6'),
    BLOCK_FORTUNE("Block Fortune", "☘", '6'),
    GEMSTONE_FORTUNE("Gemstone Fortune", "☘", '6'),
    DWARVEN_METAL_FORTUNE("Dwarven Metal Fortune", "☘", '6'),
    /** From Hunting levels ("&8+&a1 &d☘ Hunting Fortune"). */
    HUNTING_FORTUNE("Hunting Fortune", "☘", 'd'),
    SPEED("Speed", "✦", 'f'),
    MAGIC_FIND("Magic Find", "✯", 'b'),
    PET_LUCK("Pet Luck", "♣", 'd'),
    FEAR("Fear", "☠", '5'),
    HEAT_RESISTANCE("Heat Resistance", "♨", 'c'),
    COLD_RESISTANCE("Cold Resistance", "❄", 'b'),
    RESPIRATION("Respiration", "⚶", '3'),
    PRESSURE_RESISTANCE("Pressure Resistance", "❍", '9'),
    PULL("Pull", "ᛷ", 'b'),
    TRACKING("Tracking", "❃", 'd'),
    /**
     * How much more XP the skill gains: 1 + Wisdom / 100 (research skills.md 5.1), each skill's own (SkillGains.wisdom).
     * Combat's, Mining's, Enchanting's and Carpentry's skills gain XP here (STATS_EFFECTS.md).
     */
    COMBAT_WISDOM("Combat Wisdom", "☯", '3'),
    FARMING_WISDOM("Farming Wisdom", "☯", '3'),
    FISHING_WISDOM("Fishing Wisdom", "☯", '3'),
    MINING_WISDOM("Mining Wisdom", "☯", '3'),
    FORAGING_WISDOM("Foraging Wisdom", "☯", '3'),
    ENCHANTING_WISDOM("Enchanting Wisdom", "☯", '3'),
    ALCHEMY_WISDOM("Alchemy Wisdom", "☯", '3'),
    CARPENTRY_WISDOM("Carpentry Wisdom", "☯", '3'),
    RUNECRAFTING_WISDOM("Runecrafting Wisdom", "☯", '3'),
    TAMING_WISDOM("Taming Wisdom", "☯", '3'),
    SOCIAL_WISDOM("Social Wisdom", "☯", '3'),
    HUNTING_WISDOM("Hunting Wisdom", "☯", '3'),
    /** Seconds: "Rift Time: +55s". */
    RIFT_TIME("Rift Time", "ф", 'a', "s"),
    RIFT_HEALTH("Hearts", "❤", 'c'),
    RIFT_DAMAGE("Rift Damage", "❁", '5'),
    RIFT_INTELLIGENCE("Intelligence", "✎", 'b'),
    RIFT_WALK_SPEED("Speed", "✦", 'f'),
    RIFT_MANA_REGEN("Mana Regen", "⚡", 'b', true),
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
    /** What its value is followed by: "%" for a percentage ("+50%"), "s" for seconds, or nothing. */
    private final String unit;

    Stat(String displayName, String symbol, char color) {
        this(displayName, symbol, color, color, "");
    }

    Stat(String displayName, String symbol, char color, boolean percent) {
        this(displayName, symbol, color, color, percent ? "%" : "");
    }

    Stat(String displayName, String symbol, char color, String unit) {
        this(displayName, symbol, color, color, unit);
    }

    Stat(String displayName, String symbol, char color, char loreColor, String unit) {
        this.displayName = displayName;
        this.symbol = symbol;
        this.color = color;
        this.loreColor = loreColor;
        this.unit = unit;
    }

    /** Shown as a percentage ("+50%"). */
    public boolean isPercent() {
        return unit.equals("%");
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
