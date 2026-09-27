package net.icxd.dungeons.skill;

import org.bukkit.Material;

import java.util.List;
import java.util.Locale;

/**
 * Hypixel SkyBlock's skills (research skills.md 1.2, in the "Your Skills" menu's order), each with its
 * XP table (3.3 for most, 3.4 for Runecrafting and Social), its level cap (3.2), its menu icon and the
 * words the menus describe it with. XP is kept per profile as a double under {@code skills.<key>}
 * (see {@link Skills}); a level is how many of the table's cumulative amounts the XP reaches, at most
 * the cap. XP past the cap is kept (Hypixel keeps counting it, and caps that go up later use it).
 */
public enum Skill {
    COMBAT("Combat", Table.STANDARD, 60, Material.STONE_SWORD,
            "&7Fight mobs and special bosses to", "&7earn Combat EXP!"),
    /** 50, and up to 10 more bought from Anita (not in this plugin: 50). */
    FARMING("Farming", Table.STANDARD, 50, Material.GOLDEN_HOE,
            "&7Harvest crops and shear sheep to", "&7earn Farming EXP!"),
    FISHING("Fishing", Table.STANDARD, 50, Material.FISHING_ROD,
            "&7Visit your local pond to fish and", "&7earn Fishing EXP!"),
    MINING("Mining", Table.STANDARD, 60, Material.STONE_PICKAXE,
            "&7Dive into deep caves and find rare", "&7ores and valuable materials to earn", "&7Mining EXP!"),
    /** 50, and more from collections and Agatha (not in this plugin: 50). */
    FORAGING("Foraging", Table.STANDARD, 50, Material.JUNGLE_SAPLING,
            "&7Cut trees and forage for other", "&7plants to earn Foraging EXP!"),
    ENCHANTING("Enchanting", Table.STANDARD, 60, Material.ENCHANTING_TABLE,
            "&7Enchant items to earn Enchanting EXP!"),
    ALCHEMY("Alchemy", Table.STANDARD, 50, Material.BREWING_STAND,
            "&7Brew potions to earn Alchemy EXP!"),
    CARPENTRY("Carpentry", Table.STANDARD, 50, Material.CRAFTING_TABLE,
            "&7Craft items to earn Carpentry EXP!"),
    RUNECRAFTING("Runecrafting", Table.RUNECRAFTING, 25, Material.MAGMA_CREAM,
            "&7Slay bosses and runic mobs, and", "&7fuse runes to earn Runecrafting EXP!"),
    /** 50, and one more per pet type sacrificed (not in this plugin: 50). */
    TAMING("Taming", Table.STANDARD, 50, Material.POLAR_BEAR_SPAWN_EGG,
            "&7Level up pets to earn Taming EXP!"),
    SOCIAL("Social", Table.SOCIAL, 25, Material.EMERALD,
            "&7Gain Social EXP for every new unique", "&7guest, hosting guests, and visiting", "&7islands!"),
    /** On the standard table: NEU has no other, and the one recorded bar (VIII to IX, 2k) fits it. */
    HUNTING("Hunting", Table.STANDARD, 50, Material.LEAD,
            "&7Hunt various monsters to earn", "&7Hunting EXP!");

    /** The XP each level takes from the one before, level 1 first. */
    enum Table {
        STANDARD(50, 125, 200, 300, 500, 750, 1_000, 1_500, 2_000, 3_500,
                5_000, 7_500, 10_000, 15_000, 20_000, 30_000, 50_000, 75_000, 100_000, 200_000,
                300_000, 400_000, 500_000, 600_000, 700_000, 800_000, 900_000, 1_000_000, 1_100_000, 1_200_000,
                1_300_000, 1_400_000, 1_500_000, 1_600_000, 1_700_000, 1_800_000, 1_900_000, 2_000_000, 2_100_000, 2_200_000,
                2_300_000, 2_400_000, 2_500_000, 2_600_000, 2_750_000, 2_900_000, 3_100_000, 3_400_000, 3_700_000, 4_000_000,
                4_300_000, 4_600_000, 4_900_000, 5_200_000, 5_500_000, 5_800_000, 6_100_000, 6_400_000, 6_700_000, 7_000_000),
        RUNECRAFTING(50, 100, 125, 160, 200, 250, 315, 400, 500, 625,
                785, 1_000, 1_250, 1_600, 2_000, 2_465, 3_125, 4_000, 5_000, 6_200,
                7_800, 9_800, 12_200, 15_300, 19_050),
        SOCIAL(50, 100, 150, 250, 500, 750, 1_000, 1_250, 1_500, 2_000,
                2_500, 3_000, 3_750, 4_500, 6_000, 8_000, 10_000, 12_500, 15_000, 20_000,
                25_000, 30_000, 35_000, 40_000, 50_000);

        private final long[] perLevel;
        /** cumulative[n] is the XP level n takes from nothing; cumulative[0] is 0. */
        private final long[] cumulative;

        Table(long... perLevel) {
            this.perLevel = perLevel;
            this.cumulative = new long[perLevel.length + 1];
            for (int i = 0; i < perLevel.length; i++) cumulative[i + 1] = cumulative[i] + perLevel[i];
        }
    }

    private final String name;
    private final Table table;
    private final int cap;
    private final Material icon;
    private final List<String> description;

    Skill(String name, Table table, int cap, Material icon, String... description) {
        this.name = name;
        this.table = table;
        this.cap = cap;
        this.icon = icon;
        this.description = List.of(description);
    }

    /** "Combat". */
    public String getName() {
        return name;
    }

    /** "combat": its key under {@code skills} in a profile. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The highest level its table has (60, or 25 for Runecrafting and Social). */
    public int maxLevel() {
        return table.perLevel.length;
    }

    /** The highest level it can reach here: 60 for Combat, Mining and Enchanting, 25 for Runecrafting and Social, else 50. */
    public int cap() {
        return cap;
    }

    /** Runecrafting and Social: they don't count towards the skill average. */
    public boolean cosmetic() {
        return table != Table.STANDARD;
    }

    /** What the menus show it as. */
    public Material icon() {
        return icon;
    }

    /** The menus' lines about how it's earned ("&7Fight mobs and special bosses to", ...). */
    public List<String> description() {
        return description;
    }

    /** XP that level {@code level} takes from the one before (0 past the table). */
    public long xpFor(int level) {
        return level < 1 || level > maxLevel() ? 0 : table.perLevel[level - 1];
    }

    /** XP to reach level {@code level} from nothing (the table's total past it). */
    public long cumulative(int level) {
        return table.cumulative[Math.max(0, Math.min(level, maxLevel()))];
    }

    /** The level this much XP reaches, at most the cap. */
    public int level(double xp) {
        int level = 0;
        while (level < cap && xp >= table.cumulative[level + 1]) level++;
        return level;
    }

    /** Whether this much XP has reached the cap. */
    public boolean maxed(double xp) {
        return level(xp) >= cap;
    }

    /** How far this much XP is into the next level, from 0 to 1 (1 once it's at the cap). */
    public double progress(double xp) {
        int level = level(xp);
        if (level >= cap) return 1;
        double into = xp - table.cumulative[level];
        return Math.max(0, Math.min(1, into / table.perLevel[level]));
    }

    /** XP into the level after this much XP's; past the cap, XP beyond it. */
    public double xpIntoLevel(double xp) {
        return Math.max(0, xp - table.cumulative[level(xp)]);
    }

    /**
     * Coins for reaching the level (research skills.md 3.3): one table, as every skill's recorded next
     * level is. Social's is only recorded at II (250, the table's); its other levels are UNKNOWN, so
     * they're the table's too. Runecrafting gives none (its menu shows none).
     */
    public int coins(int level) {
        if (this == RUNECRAFTING || level < 1 || level > maxLevel()) return 0;
        return COINS[level - 1];
    }

    /**
     * SkyBlock XP for reaching the level: 5 for levels 1-10, 10 for 11-25, 20 for 26-50 and 30 for
     * 51-60 (3.3). Runecrafting and Social give none (their menus show none).
     */
    public int skyBlockXp(int level) {
        if (cosmetic() || level < 1 || level > maxLevel()) return 0;
        if (level <= 10) return 5;
        if (level <= 25) return 10;
        if (level <= 50) return 20;
        return 30;
    }

    private static final int[] COINS = {
            100, 250, 500, 750, 1_000, 2_000, 3_000, 4_000, 5_000, 7_500,
            10_000, 15_000, 20_000, 25_000, 30_000, 40_000, 50_000, 65_000, 80_000, 100_000,
            125_000, 150_000, 175_000, 200_000, 225_000, 250_000, 275_000, 300_000, 325_000, 350_000,
            375_000, 400_000, 425_000, 450_000, 475_000, 500_000, 550_000, 600_000, 650_000, 700_000,
            750_000, 800_000, 850_000, 900_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000,
            1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000, 1_000_000};

    /** "COMBAT" or "combat": the skill; null for none. */
    public static Skill parse(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
